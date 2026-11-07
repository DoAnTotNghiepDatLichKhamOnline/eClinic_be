package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.chatbot.PhienChat;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.entity.notification.ThongBao;
import iuh.fit.se.eclinic.common.entity.scheduling.KhungGioKham;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.enums.LoaiThongBao;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.testsupport.TestFixtures;
import iuh.fit.se.eclinic.identity.dto.request.CapNhatTrangThaiTaiKhoanRequest;
import iuh.fit.se.eclinic.identity.dto.request.DangNhapRequest;
import iuh.fit.se.eclinic.identity.dto.response.ChiTietTaiKhoanResponse;
import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;
import iuh.fit.se.eclinic.identity.event.EmailKichHoatLaiTaiKhoanEvent;
import iuh.fit.se.eclinic.identity.event.EmailVoHieuHoaTaiKhoanEvent;
import iuh.fit.se.eclinic.identity.event.TaiKhoanDaXoaEvent;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.DangNhapService;
import iuh.fit.se.eclinic.identity.service.QuanLyTaiKhoanService;
import iuh.fit.se.eclinic.identity.service.RefreshTokenService;
import jakarta.persistence.EntityManager;

/** Quản trị viên quản lý tài khoản với MySQL + Redis thật. */
class QuanLyTaiKhoanServiceTest extends KiemThuTichHop {

    private static final String MAT_KHAU = "matkhau-cu";

    @Autowired QuanLyTaiKhoanService quanLyTaiKhoanService;
    @Autowired DangNhapService dangNhapService;
    @Autowired RefreshTokenService refreshTokenService;
    @Autowired TaiKhoanRepository taiKhoanRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ApplicationEvents applicationEvents;

    private TestFixtures fixtures;
    private TransactionTemplate transactionTemplate;
    private Long idAdmin;

    @BeforeEach
    void khoiTao() {
        fixtures = new TestFixtures(entityManager, transactionManager);
        transactionTemplate = new TransactionTemplate(transactionManager);
        idAdmin = taiKhoanRepository.findByEmail("admin@eclinic.local").orElseThrow().getId();
    }

    // ---------- Danh sách, chi tiết ----------

    @Test
    void nguoiThucHienBiVoHieuHoaKhongConHoacKhongPhaiQuanTriVienBiTuChoi() {
        TaiKhoan mucTieu = taoTaiKhoan();
        Long biKhoa = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.VO_HIEU_HOA, "Bị khoá").getId();
        Long benhNhan = taoTaiKhoan().getId();

        for (Object[] truongHop : new Object[][] { { biKhoa, MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA },
                { Long.MAX_VALUE, MaLoi.CHUA_DANG_NHAP }, { benhNhan, MaLoi.KHONG_CO_QUYEN } }) {
            Long nguoiThucHien = (Long) truongHop[0];
            MaLoi maLoi = (MaLoi) truongHop[1];
            assertMaLoi(() -> quanLyTaiKhoanService.timKiem(nguoiThucHien, null, null, null, 0, 20), maLoi);
            assertMaLoi(() -> quanLyTaiKhoanService.layChiTiet(nguoiThucHien, mucTieu.getId()), maLoi);
            assertMaLoi(() -> quanLyTaiKhoanService.capNhatTrangThai(nguoiThucHien, mucTieu.getId(),
                    voHieuHoa("Vi phạm")), maLoi);
            assertMaLoi(() -> quanLyTaiKhoanService.xoa(nguoiThucHien, mucTieu.getId()), maLoi);
        }

        assertThat(trongDb(mucTieu).getTrangThai()).isEqualTo(TrangThaiTaiKhoan.DA_KICH_HOAT);
    }

    // ---------- Vô hiệu hoá, kích hoạt lại ----------

    @Test
    void voHieuHoaLuuLyDoDaCatKhoangTrangDangXuatMoiThietBiVaPhatEmail() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        DangNhapResponse mayA = dangNhap(taiKhoan);
        DangNhapResponse mayB = dangNhap(taiKhoan);
        assertThat(refreshTokenService.layPhienDangHoatDong(taiKhoan.getId())).hasSize(2);

        ChiTietTaiKhoanResponse ketQua = quanLyTaiKhoanService.capNhatTrangThai(idAdmin, taiKhoan.getId(),
                voHieuHoa("  Vi phạm quy định  "));

        assertThat(ketQua.id()).isEqualTo(taiKhoan.getId());
        assertThat(ketQua.trangThai()).isEqualTo(TrangThaiTaiKhoan.VO_HIEU_HOA);
        assertThat(ketQua.lyDoVoHieuHoa()).isEqualTo("Vi phạm quy định");
        TaiKhoan sau = trongDb(taiKhoan);
        assertThat(sau.getTrangThai()).isEqualTo(TrangThaiTaiKhoan.VO_HIEU_HOA);
        assertThat(sau.getLyDoVoHieuHoa()).isEqualTo("Vi phạm quy định");
        // Chỉ đổi trạng thái: email, mật khẩu giữ nguyên
        assertThat(sau.getEmail()).isEqualTo(taiKhoan.getEmail());
        assertThat(sau.getMatKhauHash()).isEqualTo(taiKhoan.getMatKhauHash());
        assertThat(refreshTokenService.layPhienDangHoatDong(taiKhoan.getId())).isEmpty();
        assertMaLoi(() -> dangNhapService.lamMoi(mayA.refreshToken()), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        assertMaLoi(() -> dangNhapService.lamMoi(mayB.refreshToken()), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        assertMaLoi(() -> dangNhap(taiKhoan), MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertThat(applicationEvents.stream(EmailVoHieuHoaTaiKhoanEvent.class).toList()).containsExactly(
                new EmailVoHieuHoaTaiKhoanEvent(taiKhoan.getEmail(), taiKhoan.getHoTen(), "Vi phạm quy định", NgonNgu.VI));
        assertThat(applicationEvents.stream(EmailKichHoatLaiTaiKhoanEvent.class).toList()).isEmpty();
    }

    @Test
    void taiKhoanQuanTriVienKhongVoHieuHoaVaKhongXoaDuoc() {
        assertMaLoi(() -> quanLyTaiKhoanService.capNhatTrangThai(idAdmin, idAdmin, voHieuHoa("Tự khoá")),
                MaLoi.TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE);
        assertMaLoi(() -> quanLyTaiKhoanService.capNhatTrangThai(idAdmin, idAdmin, kichHoatLai()),
                MaLoi.TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE);
        assertMaLoi(() -> quanLyTaiKhoanService.xoa(idAdmin, idAdmin), MaLoi.TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE);

        TaiKhoan admin = taiKhoanRepository.findById(idAdmin).orElseThrow();
        assertThat(admin.getTrangThai()).isEqualTo(TrangThaiTaiKhoan.DA_KICH_HOAT);
        assertThat(admin.getLyDoVoHieuHoa()).isNull();
        // Vẫn dùng được quyền quản trị
        assertThat(quanLyTaiKhoanService.layChiTiet(idAdmin, idAdmin).id()).isEqualTo(idAdmin);
    }

    // ---------- Bác sĩ còn lịch hẹn ----------

    @Test
    void bacSiConLichHenSapToiKhongVoHieuHoaDuocChoToiKhiLichHenBiHuy() {
        TaiKhoan taiKhoan = taoTaiKhoan(VaiTro.BAC_SI, TrangThaiTaiKhoan.DA_KICH_HOAT, "BS " + hauTo());
        Long idLichHen = taoLichHen(taoBacSi(taiKhoan), LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> quanLyTaiKhoanService.capNhatTrangThai(idAdmin, taiKhoan.getId(), voHieuHoa("Nghỉ việc")))
                .isInstanceOf(LoiNghiepVu.class)
                .hasMessage("loi.BAC_SI_CON_LICH_HEN.soLuong")
                .satisfies(e -> assertThat(((LoiNghiepVu) e).getThamSo()).containsExactly("1"))
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(MaLoi.BAC_SI_CON_LICH_HEN);
        assertMaLoi(() -> quanLyTaiKhoanService.capNhatTrangThai(idAdmin, taiKhoan.getId(), voHieuHoa("Nghỉ việc")),
                MaLoi.BAC_SI_CON_LICH_HEN);
        assertThat(trongDb(taiKhoan).getTrangThai()).isEqualTo(TrangThaiTaiKhoan.DA_KICH_HOAT);
        assertThat(applicationEvents.stream(EmailVoHieuHoaTaiKhoanEvent.class).toList()).isEmpty();

        doiTrangThaiLichHen(idLichHen, TrangThaiLichHen.DA_HUY);
        assertThat(quanLyTaiKhoanService.capNhatTrangThai(idAdmin, taiKhoan.getId(), voHieuHoa("Nghỉ việc")).trangThai())
                .isEqualTo(TrangThaiTaiKhoan.VO_HIEU_HOA);
    }

    // ---------- Xoá ----------

    @Test
    void xoaBiTuChoiKhiConDuLieuThamChieuVaThongDiepNeuRoDuLieuNao() {
        TaiKhoan bacSi = taoTaiKhoan(VaiTro.BAC_SI, TrangThaiTaiKhoan.DA_KICH_HOAT, "BS " + hauTo());
        taoBacSi(bacSi);
        TaiKhoan coHoSo = taoTaiKhoan();
        taoHoSoBenhNhan(coHoSo, TrangThaiLienKet.CHO_XAC_MINH, null);
        TaiKhoan coThongBao = taoTaiKhoan();
        ThongBao thongBao = new ThongBao();
        thongBao.setTaiKhoan(coThongBao);
        thongBao.setNoiDung("Thông báo test");
        thongBao.setLoai(LoaiThongBao.HE_THONG);
        fixtures.persist(thongBao);
        TaiKhoan coPhienChat = taoTaiKhoan();
        PhienChat phienChat = new PhienChat();
        phienChat.setTaiKhoan(coPhienChat);
        fixtures.persist(phienChat);
        TaiKhoan coCaHai = taoTaiKhoan();
        ThongBao thongBaoKhac = new ThongBao();
        thongBaoKhac.setTaiKhoan(coCaHai);
        thongBaoKhac.setNoiDung("Thông báo test");
        thongBaoKhac.setLoai(LoaiThongBao.HE_THONG);
        fixtures.persist(thongBaoKhac);
        taoHoSoBenhNhan(coCaHai, TrangThaiLienKet.DA_LIEN_KET, null);

        assertKhongXoaDuoc(bacSi, "hồ sơ bác sĩ");
        assertKhongXoaDuoc(coHoSo, "hồ sơ bệnh nhân");
        assertKhongXoaDuoc(coThongBao, "thông báo");
        assertKhongXoaDuoc(coPhienChat, "phiên chat");
        assertKhongXoaDuoc(coCaHai, "hồ sơ bệnh nhân, thông báo");
        assertThat(applicationEvents.stream(TaiKhoanDaXoaEvent.class).toList()).isEmpty();
    }

    // ---------- Hỗ trợ ----------

    /** Xoá phải bị chặn, thông điệp nêu {@code duLieuDangGiu} (tiếng Việt vì test không có request). */
    private void assertKhongXoaDuoc(TaiKhoan taiKhoan, String duLieuDangGiu) {
        assertThatThrownBy(() -> quanLyTaiKhoanService.xoa(idAdmin, taiKhoan.getId()))
                .isInstanceOf(LoiNghiepVu.class)
                .hasMessage("loi.TAI_KHOAN_DANG_DUOC_SU_DUNG.chiTiet")
                .satisfies(e -> assertThat(((LoiNghiepVu) e).getThamSo()).containsExactly(duLieuDangGiu))
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(MaLoi.TAI_KHOAN_DANG_DUOC_SU_DUNG);
        assertThat(taiKhoanRepository.existsById(taiKhoan.getId())).isTrue();
    }

    private static CapNhatTrangThaiTaiKhoanRequest voHieuHoa(String lyDo) {
        return new CapNhatTrangThaiTaiKhoanRequest(TrangThaiTaiKhoan.VO_HIEU_HOA, lyDo);
    }

    private static CapNhatTrangThaiTaiKhoanRequest kichHoatLai() {
        return new CapNhatTrangThaiTaiKhoanRequest(TrangThaiTaiKhoan.DA_KICH_HOAT, null);
    }

    private TaiKhoan trongDb(TaiKhoan taiKhoan) {
        return taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow();
    }

    /** Bệnh nhân đang hoạt động. */
    private TaiKhoan taoTaiKhoan() {
        return taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, "Bệnh nhân test");
    }

    private TaiKhoan taoTaiKhoan(VaiTro vaiTro, TrangThaiTaiKhoan trangThai, String hoTen) {
        return taoTaiKhoan(vaiTro, trangThai, hoTen, "ql-" + hauTo() + "@example.com");
    }

    private TaiKhoan taoTaiKhoan(VaiTro vaiTro, TrangThaiTaiKhoan trangThai, String hoTen, String email) {
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setHoTen(hoTen);
        taiKhoan.setEmail(email);
        taiKhoan.setMatKhauHash(passwordEncoder.encode(MAT_KHAU));
        taiKhoan.setSoDienThoai(soDienThoaiMoi());
        taiKhoan.setVaiTro(vaiTro);
        taiKhoan.setTrangThai(trangThai);
        return taiKhoanRepository.save(taiKhoan);
    }

    private DangNhapResponse dangNhap(TaiKhoan taiKhoan) {
        return dangNhapService.dangNhap(new DangNhapRequest(taiKhoan.getEmail(), MAT_KHAU), "JUnit");
    }

    private HoSoBenhNhan taoHoSoBenhNhan(TaiKhoan taiKhoan, TrangThaiLienKet trangThaiLienKet, LocalDate ngaySinh) {
        HoSoBenhNhan hoSo = new HoSoBenhNhan();
        hoSo.setTaiKhoan(taiKhoan);
        hoSo.setCccd(TestFixtures.cccdNgauNhien());
        hoSo.setHoTen("Hồ sơ " + hauTo());
        hoSo.setNgaySinh(ngaySinh);
        hoSo.setSoDienThoai("0900000000");
        hoSo.setTrangThaiLienKet(trangThaiLienKet);
        return fixtures.persist(hoSo);
    }

    /** Hồ sơ bác sĩ + chuyên khoa riêng cho tài khoản BAC_SI. */
    private BacSi taoBacSi(TaiKhoan taiKhoan) {
        ChuyenKhoa chuyenKhoa = new ChuyenKhoa();
        chuyenKhoa.setTenChuyenKhoa("Chuyên khoa " + hauTo());
        fixtures.persist(chuyenKhoa);
        BacSi bacSi = new BacSi();
        bacSi.setTaiKhoan(taiKhoan);
        bacSi.setChuyenKhoa(chuyenKhoa);
        bacSi.setHocVi("ThS.BS");
        return fixtures.persist(bacSi);
    }

    /** 1 lịch hẹn của Khách với bác sĩ vào ngày {@code ngay} (ca 08:00–11:00, khung 08:00–08:30). */
    private Long taoLichHen(BacSi bacSi, LocalDate ngay) {
        return transactionTemplate.execute(status -> {
            QuanTriVien quanTriVien = entityManager
                    .createQuery("select q from QuanTriVien q where q.taiKhoan.id = :id", QuanTriVien.class)
                    .setParameter("id", idAdmin)
                    .getSingleResult();
            PhongKham phongKham = new PhongKham();
            phongKham.setChuyenKhoa(bacSi.getChuyenKhoa());
            phongKham.setTenPhong("Phòng " + hauTo());
            entityManager.persist(phongKham);

            LichLamViec lichLamViec = new LichLamViec();
            lichLamViec.setBacSi(bacSi);
            lichLamViec.setPhongKham(phongKham);
            lichLamViec.setAdminTao(quanTriVien);
            lichLamViec.setNgayLamViec(ngay);
            lichLamViec.setGioBatDau(LocalTime.of(8, 0));
            lichLamViec.setGioKetThuc(LocalTime.of(11, 0));
            lichLamViec.setSoBenhNhanToiDa(6);
            lichLamViec.setSoLuotToiDaMoiGio(2);
            lichLamViec.setThoiLuongLuotPhut(30);
            entityManager.persist(lichLamViec);

            KhungGioKham khungGio = new KhungGioKham();
            khungGio.setLichLamViec(lichLamViec);
            khungGio.setGioBatDau(ngay.atTime(8, 0));
            khungGio.setGioKetThuc(ngay.atTime(8, 30));
            entityManager.persist(khungGio);

            HoSoBenhNhan hoSo = new HoSoBenhNhan();
            hoSo.setCccd(TestFixtures.cccdNgauNhien());
            hoSo.setHoTen("Khách " + hauTo());
            hoSo.setSoDienThoai("0900000000");
            entityManager.persist(hoSo);

            LichHen lichHen = new LichHen();
            lichHen.setHoSoBenhNhan(hoSo);
            lichHen.setBacSi(bacSi);
            lichHen.setKhungGio(khungGio);
            lichHen.setPhongKham(phongKham);
            lichHen.setSoThuTu(1);
            lichHen.setMaTokenPhieuKham(UUID.randomUUID().toString().replace("-", ""));
            lichHen.setMaTraCuu(UUID.randomUUID().toString().substring(0, 20));
            entityManager.persist(lichHen);
            entityManager.flush();
            return lichHen.getId();
        });
    }

    private void doiTrangThaiLichHen(Long idLichHen, TrangThaiLichHen trangThai) {
        transactionTemplate.executeWithoutResult(status -> entityManager.find(LichHen.class, idLichHen)
                .chuyenTrangThai(trangThai));
    }

    private static void assertMaLoi(Runnable hanhDong, MaLoi maLoi) {
        assertThatThrownBy(hanhDong::run)
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(maLoi);
    }

    private static String hauTo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String soDienThoaiMoi() {
        return "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

}
