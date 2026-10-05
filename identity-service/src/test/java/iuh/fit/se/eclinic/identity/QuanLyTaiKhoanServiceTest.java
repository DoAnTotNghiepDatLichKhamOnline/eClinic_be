package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
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
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.RedisTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.TestFixtures;
import iuh.fit.se.eclinic.identity.dto.request.CapNhatTrangThaiTaiKhoanRequest;
import iuh.fit.se.eclinic.identity.dto.request.DangKyRequest;
import iuh.fit.se.eclinic.identity.dto.request.DangNhapRequest;
import iuh.fit.se.eclinic.identity.dto.request.DoiEmailRequest;
import iuh.fit.se.eclinic.identity.dto.response.ChiTietTaiKhoanResponse;
import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanQuanTriResponse;
import iuh.fit.se.eclinic.identity.event.EmailDatLaiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailKichHoatLaiTaiKhoanEvent;
import iuh.fit.se.eclinic.identity.event.EmailVoHieuHoaTaiKhoanEvent;
import iuh.fit.se.eclinic.identity.event.EmailXacNhanDoiEmailEvent;
import iuh.fit.se.eclinic.identity.event.EmailXacThucEvent;
import iuh.fit.se.eclinic.identity.event.TaiKhoanDaXoaEvent;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.DangNhapService;
import iuh.fit.se.eclinic.identity.service.DoiEmailService;
import iuh.fit.se.eclinic.identity.service.MatKhauService;
import iuh.fit.se.eclinic.identity.service.QuanLyTaiKhoanService;
import iuh.fit.se.eclinic.identity.service.RefreshTokenService;
import iuh.fit.se.eclinic.identity.service.XacThucService;
import jakarta.persistence.EntityManager;

/**
 * Quản trị viên quản lý tài khoản với MySQL + Redis thật. Người thực hiện là quản trị viên mặc định do KhoiTaoAdminRunner
 * tạo (KHÔNG tạo thêm quản trị viên trong test: KhoiTaoAdminTest đếm số quản trị viên). Dữ liệu mỗi test có hậu tố
 * ngẫu nhiên để không đụng nhau trong container dùng chung. Không gửi email: kiểm tra qua event đã phát.
 */
@SpringBootTest
@Import({ MySqlTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
@RecordApplicationEvents
class QuanLyTaiKhoanServiceTest {

    private static final String MAT_KHAU = "matkhau-cu";

    @Autowired QuanLyTaiKhoanService quanLyTaiKhoanService;
    @Autowired DangNhapService dangNhapService;
    @Autowired DoiEmailService doiEmailService;
    @Autowired MatKhauService matKhauService;
    @Autowired XacThucService xacThucService;
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
    void timTheoTuKhoaTrenHoTenEmailSoDienThoaiKhongPhanBietHoaThuongVaDau() {
        String hauTo = hauTo();
        TaiKhoan theoTen = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, "Nguyễn Văn " + hauTo,
                "ql-" + hauTo() + "@example.com");
        TaiKhoan theoEmail = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, "Người khác",
                "ql-" + hauTo + "@example.com");
        TaiKhoan theoSoDienThoai = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, "Người thứ ba",
                "ql-" + hauTo() + "@example.com");

        assertThat(idTimDuoc(hauTo, null, null)).containsExactlyInAnyOrder(theoTen.getId(), theoEmail.getId());
        assertThat(idTimDuoc("  " + hauTo.toUpperCase() + " ", null, null))
                .containsExactlyInAnyOrder(theoTen.getId(), theoEmail.getId());
        // Collation của cột: gõ không dấu vẫn ra tên có dấu
        assertThat(idTimDuoc("nguyen van " + hauTo, null, null)).containsExactly(theoTen.getId());
        assertThat(idTimDuoc(theoSoDienThoai.getSoDienThoai(), null, null)).containsExactly(theoSoDienThoai.getId());
        // Từ khoá rỗng = không lọc
        assertThat(quanLyTaiKhoanService.timKiem(idAdmin, "   ", null, null, 0, 1).tongSoPhanTu())
                .isEqualTo(quanLyTaiKhoanService.timKiem(idAdmin, null, null, null, 0, 1).tongSoPhanTu())
                .isGreaterThanOrEqualTo(4);
    }

    @Test
    void locTheoVaiTroVaTrangThaiKetHopTuKhoa() {
        String hauTo = hauTo();
        TaiKhoan benhNhan = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, "Lọc " + hauTo);
        TaiKhoan benhNhanBiKhoa = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.VO_HIEU_HOA, "Lọc " + hauTo);
        TaiKhoan chuaXacThuc = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.CHO_XAC_NHAN, "Lọc " + hauTo);
        TaiKhoan bacSi = taoTaiKhoan(VaiTro.BAC_SI, TrangThaiTaiKhoan.DA_KICH_HOAT, "Lọc " + hauTo);

        assertThat(idTimDuoc(hauTo, null, null)).containsExactlyInAnyOrder(benhNhan.getId(), benhNhanBiKhoa.getId(),
                chuaXacThuc.getId(), bacSi.getId());
        assertThat(idTimDuoc(hauTo, VaiTro.BENH_NHAN, null)).containsExactlyInAnyOrder(benhNhan.getId(),
                benhNhanBiKhoa.getId(), chuaXacThuc.getId());
        assertThat(idTimDuoc(hauTo, VaiTro.BAC_SI, null)).containsExactly(bacSi.getId());
        assertThat(idTimDuoc(hauTo, null, TrangThaiTaiKhoan.DA_KICH_HOAT)).containsExactlyInAnyOrder(benhNhan.getId(),
                bacSi.getId());
        assertThat(idTimDuoc(hauTo, VaiTro.BENH_NHAN, TrangThaiTaiKhoan.VO_HIEU_HOA))
                .containsExactly(benhNhanBiKhoa.getId());
        assertThat(idTimDuoc(hauTo, VaiTro.BENH_NHAN, TrangThaiTaiKhoan.CHO_XAC_NHAN))
                .containsExactly(chuaXacThuc.getId());
        assertThat(idTimDuoc(hauTo, VaiTro.QUAN_TRI_VIEN, null)).isEmpty();
        // Không có từ khoá: quản trị viên mặc định nằm trong danh sách quản trị viên
        assertThat(idTimDuoc(null, VaiTro.QUAN_TRI_VIEN, TrangThaiTaiKhoan.DA_KICH_HOAT)).contains(idAdmin);
    }

    @Test
    void kyTuPhanTramGachDuoiVaChamThanTrongTuKhoaLaKyTuThuong() {
        String hauTo = hauTo();
        TaiKhoan phanTram = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, hauTo + " 50%off");
        TaiKhoan khongPhanTram = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, hauTo + " 50xoff");
        TaiKhoan gachDuoi = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, hauTo + " a_b");
        TaiKhoan khongGachDuoi = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, hauTo + " axb");
        TaiKhoan chamThan = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, hauTo + " c!d");

        // Nếu % và _ còn là ký tự đại diện thì 2 truy vấn đầu ra cả tài khoản "x"
        assertThat(idTimDuoc(hauTo + " 50%off", null, null)).containsExactly(phanTram.getId());
        assertThat(idTimDuoc(hauTo + " a_b", null, null)).containsExactly(gachDuoi.getId());
        assertThat(idTimDuoc(hauTo + " c!d", null, null)).containsExactly(chamThan.getId());
        assertThat(idTimDuoc(hauTo + " c!", null, null)).containsExactly(chamThan.getId());
        // Gõ đúng 1 ký tự "%" hay "_": chỉ ra tài khoản thật sự chứa ký tự đó, không phải mọi tài khoản
        assertThat(idTimDuoc("%", null, null)).contains(phanTram.getId())
                .doesNotContain(khongPhanTram.getId(), gachDuoi.getId(), khongGachDuoi.getId(), chamThan.getId());
        assertThat(idTimDuoc("_", null, null)).contains(gachDuoi.getId())
                .doesNotContain(phanTram.getId(), khongPhanTram.getId(), khongGachDuoi.getId(), chamThan.getId());
        assertThat(idTimDuoc("!", null, null)).contains(chamThan.getId())
                .doesNotContain(phanTram.getId(), khongPhanTram.getId(), gachDuoi.getId(), khongGachDuoi.getId());
    }

    @Test
    void moiTaoDungTruocVaPhanTrangDungTongSo() {
        String hauTo = hauTo();
        TaiKhoan thuNhat = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, "Trang " + hauTo);
        TaiKhoan thuHai = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, "Trang " + hauTo);
        TaiKhoan thuBa = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, "Trang " + hauTo);

        TrangDuLieu<TaiKhoanQuanTriResponse> trang0 = quanLyTaiKhoanService.timKiem(idAdmin, hauTo, null, null, 0, 2);
        assertThat(trang0.noiDung()).extracting(TaiKhoanQuanTriResponse::id)
                .containsExactly(thuBa.getId(), thuHai.getId());
        assertThat(trang0.trang()).isZero();
        assertThat(trang0.kichThuoc()).isEqualTo(2);
        assertThat(trang0.tongSoPhanTu()).isEqualTo(3);
        assertThat(trang0.tongSoTrang()).isEqualTo(2);

        TrangDuLieu<TaiKhoanQuanTriResponse> trang1 = quanLyTaiKhoanService.timKiem(idAdmin, hauTo, null, null, 1, 2);
        assertThat(trang1.noiDung()).extracting(TaiKhoanQuanTriResponse::id).containsExactly(thuNhat.getId());
        assertThat(trang1.trang()).isEqualTo(1);
        assertThat(trang1.tongSoPhanTu()).isEqualTo(3);

        assertThat(quanLyTaiKhoanService.timKiem(idAdmin, hauTo, null, null, 2, 2).noiDung()).isEmpty();
    }

    @Test
    void ngaySinhChiCoKhiHoSoBenhNhanDaLienKet() {
        String hauTo = hauTo();
        TaiKhoan daLienKet = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, "NS " + hauTo);
        taoHoSoBenhNhan(daLienKet, TrangThaiLienKet.DA_LIEN_KET, LocalDate.of(1990, 5, 20));
        TaiKhoan choXacMinh = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, "NS " + hauTo);
        taoHoSoBenhNhan(choXacMinh, TrangThaiLienKet.CHO_XAC_MINH, LocalDate.of(1985, 1, 2));
        TaiKhoan hoSoThieuNgaySinh = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.DA_KICH_HOAT, "NS " + hauTo);
        taoHoSoBenhNhan(hoSoThieuNgaySinh, TrangThaiLienKet.DA_LIEN_KET, null);
        TaiKhoan khongHoSo = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.VO_HIEU_HOA, "NS " + hauTo);
        khongHoSo.setLyDoVoHieuHoa("Vi phạm");
        taiKhoanRepository.save(khongHoSo);

        List<TaiKhoanQuanTriResponse> danhSach = quanLyTaiKhoanService.timKiem(idAdmin, hauTo, null, null, 0, 20)
                .noiDung();
        assertThat(danhSach).hasSize(4);
        TaiKhoanQuanTriResponse dong = dong(danhSach, daLienKet);
        assertThat(dong.ngaySinh()).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(dong.email()).isEqualTo(daLienKet.getEmail());
        assertThat(dong.hoTen()).isEqualTo(daLienKet.getHoTen());
        assertThat(dong.soDienThoai()).isEqualTo(daLienKet.getSoDienThoai());
        assertThat(dong.vaiTro()).isEqualTo(VaiTro.BENH_NHAN);
        assertThat(dong.trangThai()).isEqualTo(TrangThaiTaiKhoan.DA_KICH_HOAT);
        assertThat(dong.lyDoVoHieuHoa()).isNull();
        assertThat(dong.ngayTao()).isNotNull();
        assertThat(dong(danhSach, choXacMinh).ngaySinh()).isNull();
        assertThat(dong(danhSach, hoSoThieuNgaySinh).ngaySinh()).isNull();
        assertThat(dong(danhSach, khongHoSo).ngaySinh()).isNull();
        assertThat(dong(danhSach, khongHoSo).lyDoVoHieuHoa()).isEqualTo("Vi phạm");

        ChiTietTaiKhoanResponse chiTiet = quanLyTaiKhoanService.layChiTiet(idAdmin, daLienKet.getId());
        assertThat(chiTiet.ngaySinh()).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(chiTiet.trangThaiLienKetHoSo()).isEqualTo(TrangThaiLienKet.DA_LIEN_KET);
        assertThat(chiTiet.bacSi()).isNull();
        ChiTietTaiKhoanResponse chiTietChoXacMinh = quanLyTaiKhoanService.layChiTiet(idAdmin, choXacMinh.getId());
        assertThat(chiTietChoXacMinh.ngaySinh()).isNull();
        assertThat(chiTietChoXacMinh.trangThaiLienKetHoSo()).isEqualTo(TrangThaiLienKet.CHO_XAC_MINH);
        ChiTietTaiKhoanResponse chiTietKhongHoSo = quanLyTaiKhoanService.layChiTiet(idAdmin, khongHoSo.getId());
        assertThat(chiTietKhongHoSo.ngaySinh()).isNull();
        assertThat(chiTietKhongHoSo.trangThaiLienKetHoSo()).isNull();
        assertThat(chiTietKhongHoSo.lyDoVoHieuHoa()).isEqualTo("Vi phạm");
    }

    @Test
    void chiTietBacSiCoHoSoBacSiVaChuyenKhoa() {
        TaiKhoan taiKhoan = taoTaiKhoan(VaiTro.BAC_SI, TrangThaiTaiKhoan.DA_KICH_HOAT, "BS " + hauTo());
        BacSi bacSi = taoBacSi(taiKhoan);

        ChiTietTaiKhoanResponse chiTiet = quanLyTaiKhoanService.layChiTiet(idAdmin, taiKhoan.getId());

        assertThat(chiTiet.id()).isEqualTo(taiKhoan.getId());
        assertThat(chiTiet.vaiTro()).isEqualTo(VaiTro.BAC_SI);
        assertThat(chiTiet.coMatKhau()).isTrue();
        assertThat(chiTiet.lienKetGoogle()).isFalse();
        assertThat(chiTiet.ngayTao()).isNotNull();
        assertThat(chiTiet.ngayCapNhat()).isNotNull();
        assertThat(chiTiet.ngaySinh()).isNull();
        assertThat(chiTiet.trangThaiLienKetHoSo()).isNull();
        assertThat(chiTiet.bacSi().id()).isEqualTo(bacSi.getId());
        assertThat(chiTiet.bacSi().tenChuyenKhoa()).isEqualTo(bacSi.getChuyenKhoa().getTenChuyenKhoa());
        assertThat(chiTiet.bacSi().hocVi()).isEqualTo("ThS.BS");
        // Quản trị viên mặc định: không có hồ sơ nào kèm theo
        ChiTietTaiKhoanResponse admin = quanLyTaiKhoanService.layChiTiet(idAdmin, idAdmin);
        assertThat(admin.vaiTro()).isEqualTo(VaiTro.QUAN_TRI_VIEN);
        assertThat(admin.bacSi()).isNull();
    }

    @Test
    void taiKhoanKhongTonTaiBaoKhongTimThay() {
        assertMaLoi(() -> quanLyTaiKhoanService.layChiTiet(idAdmin, Long.MAX_VALUE), MaLoi.KHONG_TIM_THAY);
        assertMaLoi(() -> quanLyTaiKhoanService.capNhatTrangThai(idAdmin, Long.MAX_VALUE, voHieuHoa("Vi phạm")),
                MaLoi.KHONG_TIM_THAY);
        assertMaLoi(() -> quanLyTaiKhoanService.xoa(idAdmin, Long.MAX_VALUE), MaLoi.KHONG_TIM_THAY);
    }

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
                new EmailVoHieuHoaTaiKhoanEvent(taiKhoan.getEmail(), taiKhoan.getHoTen(), "Vi phạm quy định"));
        assertThat(applicationEvents.stream(EmailKichHoatLaiTaiKhoanEvent.class).toList()).isEmpty();
    }

    @Test
    void kichHoatLaiXoaLyDoPhatEmailDangNhapLaiDuocNhungKhongKhoiPhucPhienCu() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        DangNhapResponse phienCu = dangNhap(taiKhoan);
        quanLyTaiKhoanService.capNhatTrangThai(idAdmin, taiKhoan.getId(), voHieuHoa("Vi phạm"));

        // Lý do gửi kèm khi kích hoạt lại bị bỏ qua
        ChiTietTaiKhoanResponse ketQua = quanLyTaiKhoanService.capNhatTrangThai(idAdmin, taiKhoan.getId(),
                new CapNhatTrangThaiTaiKhoanRequest(TrangThaiTaiKhoan.DA_KICH_HOAT, "không dùng"));

        assertThat(ketQua.trangThai()).isEqualTo(TrangThaiTaiKhoan.DA_KICH_HOAT);
        assertThat(ketQua.lyDoVoHieuHoa()).isNull();
        TaiKhoan sau = trongDb(taiKhoan);
        assertThat(sau.getTrangThai()).isEqualTo(TrangThaiTaiKhoan.DA_KICH_HOAT);
        assertThat(sau.getLyDoVoHieuHoa()).isNull();
        assertThat(applicationEvents.stream(EmailKichHoatLaiTaiKhoanEvent.class).toList())
                .containsExactly(new EmailKichHoatLaiTaiKhoanEvent(taiKhoan.getEmail(), taiKhoan.getHoTen()));
        assertThat(dangNhap(taiKhoan).accessToken()).isNotBlank();
        assertMaLoi(() -> dangNhapService.lamMoi(phienCu.refreshToken()), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
    }

    @Test
    void lienKetGuiTruocKhiVoHieuHoaKhongDungDuocSauKhiKichHoatLai() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        String emailMoi = "ql-moi-" + hauTo() + "@example.com";
        doiEmailService.yeuCau(taiKhoan.getId(), new DoiEmailRequest(emailMoi, MAT_KHAU));
        String tokenDoiEmail = applicationEvents.stream(EmailXacNhanDoiEmailEvent.class).toList().get(0).token();
        matKhauService.quenMatKhau(taiKhoan.getEmail());
        String tokenDatLai = applicationEvents.stream(EmailDatLaiMatKhauEvent.class).toList().get(0).token();

        quanLyTaiKhoanService.capNhatTrangThai(idAdmin, taiKhoan.getId(), voHieuHoa("Vi phạm"));
        quanLyTaiKhoanService.capNhatTrangThai(idAdmin, taiKhoan.getId(), kichHoatLai());

        // Tài khoản đã hoạt động lại nên lỗi 410 là do liên kết đã bị huỷ, không phải do trạng thái tài khoản
        assertThat(doiEmailService.layYeuCauDangCho(taiKhoan.getId())).isNull();
        assertMaLoi(() -> doiEmailService.xacNhan(tokenDoiEmail), MaLoi.LIEN_KET_KHONG_HOP_LE);
        assertMaLoi(() -> matKhauService.datLaiMatKhau(tokenDatLai, "matkhau-moi"), MaLoi.LIEN_KET_KHONG_HOP_LE);
        assertThat(trongDb(taiKhoan).getEmail()).isEqualTo(taiKhoan.getEmail());
        assertThat(dangNhap(taiKhoan).accessToken()).isNotBlank();
    }

    @Test
    void voHieuHoaThieuLyDoBao400VaKhongDoiGi() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        DangNhapResponse phien = dangNhap(taiKhoan);

        for (String lyDo : new String[] { null, "", "   " }) {
            assertThatThrownBy(() -> quanLyTaiKhoanService.capNhatTrangThai(idAdmin, taiKhoan.getId(), voHieuHoa(lyDo)))
                    .isInstanceOf(LoiNghiepVu.class)
                    .hasMessage("Phải nhập lý do khi vô hiệu hoá tài khoản")
                    .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                    .isEqualTo(MaLoi.DU_LIEU_KHONG_HOP_LE);
        }

        assertThat(trongDb(taiKhoan).getTrangThai()).isEqualTo(TrangThaiTaiKhoan.DA_KICH_HOAT);
        assertThat(dangNhapService.lamMoi(phien.refreshToken()).accessToken()).isNotBlank();
        assertThat(applicationEvents.stream(EmailVoHieuHoaTaiKhoanEvent.class).toList()).isEmpty();
    }

    @Test
    void chuyenSangTrangThaiDangCoBao409() {
        TaiKhoan dangHoatDong = taoTaiKhoan();
        TaiKhoan biKhoa = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.VO_HIEU_HOA, "Bị khoá");
        biKhoa.setLyDoVoHieuHoa("Lý do cũ");
        taiKhoanRepository.save(biKhoa);

        assertMaLoi(() -> quanLyTaiKhoanService.capNhatTrangThai(idAdmin, dangHoatDong.getId(), kichHoatLai()),
                MaLoi.TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE);
        assertMaLoi(() -> quanLyTaiKhoanService.capNhatTrangThai(idAdmin, biKhoa.getId(), voHieuHoa("Lý do mới")),
                MaLoi.TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE);

        // Vô hiệu hoá lần 2 không ghi đè lý do và không gửi email
        assertThat(trongDb(biKhoa).getLyDoVoHieuHoa()).isEqualTo("Lý do cũ");
        assertThat(applicationEvents.stream(EmailVoHieuHoaTaiKhoanEvent.class).toList()).isEmpty();
        assertThat(applicationEvents.stream(EmailKichHoatLaiTaiKhoanEvent.class).toList()).isEmpty();
    }

    @Test
    void taiKhoanChuaXacThucKhongDoiTrangThaiDuocVaKhongAiChuyenVeChoXacNhanDuoc() {
        TaiKhoan chuaXacThuc = taoTaiKhoan(VaiTro.BENH_NHAN, TrangThaiTaiKhoan.CHO_XAC_NHAN, "Chưa xác thực");
        TaiKhoan dangHoatDong = taoTaiKhoan();
        CapNhatTrangThaiTaiKhoanRequest veChoXacNhan = new CapNhatTrangThaiTaiKhoanRequest(
                TrangThaiTaiKhoan.CHO_XAC_NHAN, "Lý do");

        assertMaLoi(() -> quanLyTaiKhoanService.capNhatTrangThai(idAdmin, chuaXacThuc.getId(), voHieuHoa("Vi phạm")),
                MaLoi.TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE);
        // Kích hoạt hộ = bỏ qua bước xác thực email
        assertMaLoi(() -> quanLyTaiKhoanService.capNhatTrangThai(idAdmin, chuaXacThuc.getId(), kichHoatLai()),
                MaLoi.TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE);
        assertMaLoi(() -> quanLyTaiKhoanService.capNhatTrangThai(idAdmin, dangHoatDong.getId(), veChoXacNhan),
                MaLoi.DU_LIEU_KHONG_HOP_LE);
        assertMaLoi(() -> quanLyTaiKhoanService.capNhatTrangThai(idAdmin, chuaXacThuc.getId(), veChoXacNhan),
                MaLoi.DU_LIEU_KHONG_HOP_LE);

        assertThat(trongDb(chuaXacThuc).getTrangThai()).isEqualTo(TrangThaiTaiKhoan.CHO_XAC_NHAN);
        assertThat(trongDb(dangHoatDong).getTrangThai()).isEqualTo(TrangThaiTaiKhoan.DA_KICH_HOAT);
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

    // ---------- Bác sĩ còn lịch hẹn (UC-DOCT-03) ----------

    @Test
    void bacSiConLichHenSapToiKhongVoHieuHoaDuocChoToiKhiLichHenBiHuy() {
        TaiKhoan taiKhoan = taoTaiKhoan(VaiTro.BAC_SI, TrangThaiTaiKhoan.DA_KICH_HOAT, "BS " + hauTo());
        Long idLichHen = taoLichHen(taoBacSi(taiKhoan), LocalDate.now().plusDays(1), TrangThaiLichHen.CHO_XAC_NHAN);

        assertThatThrownBy(() -> quanLyTaiKhoanService.capNhatTrangThai(idAdmin, taiKhoan.getId(), voHieuHoa("Nghỉ việc")))
                .isInstanceOf(LoiNghiepVu.class)
                .hasMessageContaining("còn 1 lịch hẹn sắp tới")
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(MaLoi.BAC_SI_CON_LICH_HEN);
        doiTrangThaiLichHen(idLichHen, TrangThaiLichHen.DA_XAC_NHAN);
        assertMaLoi(() -> quanLyTaiKhoanService.capNhatTrangThai(idAdmin, taiKhoan.getId(), voHieuHoa("Nghỉ việc")),
                MaLoi.BAC_SI_CON_LICH_HEN);
        assertThat(trongDb(taiKhoan).getTrangThai()).isEqualTo(TrangThaiTaiKhoan.DA_KICH_HOAT);
        assertThat(applicationEvents.stream(EmailVoHieuHoaTaiKhoanEvent.class).toList()).isEmpty();

        doiTrangThaiLichHen(idLichHen, TrangThaiLichHen.DA_HUY);
        assertThat(quanLyTaiKhoanService.capNhatTrangThai(idAdmin, taiKhoan.getId(), voHieuHoa("Nghỉ việc")).trangThai())
                .isEqualTo(TrangThaiTaiKhoan.VO_HIEU_HOA);
    }

    @Test
    void bacSiChiConLichHenDaQuaHoacKhongCoLichHenVanVoHieuHoaDuoc() {
        TaiKhoan coLichDaQua = taoTaiKhoan(VaiTro.BAC_SI, TrangThaiTaiKhoan.DA_KICH_HOAT, "BS " + hauTo());
        taoLichHen(taoBacSi(coLichDaQua), LocalDate.now().minusDays(1), TrangThaiLichHen.DA_XAC_NHAN);
        TaiKhoan khongLichHen = taoTaiKhoan(VaiTro.BAC_SI, TrangThaiTaiKhoan.DA_KICH_HOAT, "BS " + hauTo());
        taoBacSi(khongLichHen);

        ChiTietTaiKhoanResponse ketQua = quanLyTaiKhoanService.capNhatTrangThai(idAdmin, coLichDaQua.getId(),
                voHieuHoa("Nghỉ việc"));
        assertThat(ketQua.trangThai()).isEqualTo(TrangThaiTaiKhoan.VO_HIEU_HOA);
        // identity-service không ghi bảng bac_si: hồ sơ bác sĩ vẫn còn nguyên trong chi tiết
        assertThat(ketQua.bacSi()).isNotNull();
        assertThat(quanLyTaiKhoanService.capNhatTrangThai(idAdmin, khongLichHen.getId(), voHieuHoa("Nghỉ việc"))
                .trangThai()).isEqualTo(TrangThaiTaiKhoan.VO_HIEU_HOA);
    }

    // ---------- Xoá ----------

    @Test
    void xoaTaiKhoanChuaDuocThamChieuXoaLuonPhienVaLienKetRoiEmailDangKyLaiDuoc() {
        String email = "ql-xoa-" + hauTo() + "@example.com";
        String soDienThoai = soDienThoaiMoi();
        Long id = xacThucService.dangKy(new DangKyRequest("Người đăng ký", email, "Matkhau@123", soDienThoai, null)).id();
        String tokenXacThuc = applicationEvents.stream(EmailXacThucEvent.class).toList().get(0).token();
        TaiKhoan taiKhoan = taiKhoanRepository.findById(id).orElseThrow();
        String tokenA = refreshTokenService.tao(taiKhoan, "JUnit A", Duration.ofDays(7)).refreshToken();
        String tokenB = refreshTokenService.tao(taiKhoan, "JUnit B", Duration.ofDays(7)).refreshToken();
        assertThat(refreshTokenService.layPhienDangHoatDong(id)).hasSize(2);

        quanLyTaiKhoanService.xoa(idAdmin, id);

        assertThat(taiKhoanRepository.existsById(id)).isFalse();
        assertThat(refreshTokenService.timTheoToken(tokenA)).isEmpty();
        assertThat(refreshTokenService.timTheoToken(tokenB)).isEmpty();
        assertMaLoi(() -> xacThucService.xacThucEmail(tokenXacThuc), MaLoi.LIEN_KET_KHONG_HOP_LE);
        assertThat(applicationEvents.stream(TaiKhoanDaXoaEvent.class).toList())
                .containsExactly(new TaiKhoanDaXoaEvent(id, null));
        assertMaLoi(() -> quanLyTaiKhoanService.layChiTiet(idAdmin, id), MaLoi.KHONG_TIM_THAY);
        // Email và số điện thoại được giải phóng
        Long idMoi = xacThucService.dangKy(new DangKyRequest("Người đăng ký lại", email, "Matkhau@123", soDienThoai, null))
                .id();
        assertThat(idMoi).isNotEqualTo(id);
    }

    @Test
    void xoaTaiKhoanDangHoatDongKhongCoDuLieuCungDuoc() {
        TaiKhoan taiKhoan = taoTaiKhoan();
        taiKhoan.setAnhDaiDien("https://lh3.googleusercontent.com/a/anh");
        taiKhoanRepository.save(taiKhoan);
        dangNhap(taiKhoan);

        quanLyTaiKhoanService.xoa(idAdmin, taiKhoan.getId());

        assertThat(taiKhoanRepository.existsById(taiKhoan.getId())).isFalse();
        assertThat(applicationEvents.stream(TaiKhoanDaXoaEvent.class).toList())
                .containsExactly(new TaiKhoanDaXoaEvent(taiKhoan.getId(), "https://lh3.googleusercontent.com/a/anh"));
    }

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

        assertKhongXoaDuoc(bacSi, "Tài khoản đang có hồ sơ bác sĩ nên không thể xoá");
        assertKhongXoaDuoc(coHoSo, "Tài khoản đang có hồ sơ bệnh nhân nên không thể xoá");
        assertKhongXoaDuoc(coThongBao, "Tài khoản đang có thông báo nên không thể xoá");
        assertKhongXoaDuoc(coPhienChat, "Tài khoản đang có phiên chat nên không thể xoá");
        assertKhongXoaDuoc(coCaHai, "Tài khoản đang có hồ sơ bệnh nhân, thông báo nên không thể xoá");
        assertThat(applicationEvents.stream(TaiKhoanDaXoaEvent.class).toList()).isEmpty();
    }

    // ---------- Hỗ trợ ----------

    private void assertKhongXoaDuoc(TaiKhoan taiKhoan, String thongDiep) {
        assertThatThrownBy(() -> quanLyTaiKhoanService.xoa(idAdmin, taiKhoan.getId()))
                .isInstanceOf(LoiNghiepVu.class)
                .hasMessageStartingWith(thongDiep)
                .hasMessageContaining("hãy vô hiệu hoá tài khoản")
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(MaLoi.TAI_KHOAN_DANG_DUOC_SU_DUNG);
        assertThat(taiKhoanRepository.existsById(taiKhoan.getId())).isTrue();
    }

    private List<Long> idTimDuoc(String tuKhoa, VaiTro vaiTro, TrangThaiTaiKhoan trangThai) {
        return quanLyTaiKhoanService.timKiem(idAdmin, tuKhoa, vaiTro, trangThai, 0, 100).noiDung().stream()
                .map(TaiKhoanQuanTriResponse::id)
                .toList();
    }

    private static TaiKhoanQuanTriResponse dong(List<TaiKhoanQuanTriResponse> danhSach, TaiKhoan taiKhoan) {
        return danhSach.stream().filter(d -> d.id().equals(taiKhoan.getId())).findFirst().orElseThrow();
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

    /**
     * 1 lịch hẹn của Khách với bác sĩ vào ngày {@code ngay} (ca 08:00–11:00, khung giờ 08:00–08:30). Dựng tay thay cho
     * TestFixtures.taoCaLamViec (cái đó tạo thêm 1 tài khoản quản trị viên): ca do quản trị viên mặc định xếp.
     *
     * @return id lịch hẹn
     */
    private Long taoLichHen(BacSi bacSi, LocalDate ngay, TrangThaiLichHen trangThai) {
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
            lichHen.setTrangThai(trangThai);
            lichHen.setMaTokenPhieuKham(UUID.randomUUID().toString().replace("-", ""));
            lichHen.setMaTraCuu(UUID.randomUUID().toString().substring(0, 20));
            entityManager.persist(lichHen);
            entityManager.flush();
            return lichHen.getId();
        });
    }

    private void doiTrangThaiLichHen(Long idLichHen, TrangThaiLichHen trangThai) {
        transactionTemplate.executeWithoutResult(status -> entityManager.find(LichHen.class, idLichHen)
                .setTrangThai(trangThai));
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
