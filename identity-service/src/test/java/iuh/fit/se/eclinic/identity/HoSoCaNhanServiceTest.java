package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.RedisTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.TestFixtures;
import iuh.fit.se.eclinic.identity.dto.request.CapNhatHoSoRequest;
import iuh.fit.se.eclinic.identity.dto.response.HoSoCaNhanResponse;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.HoSoCaNhanService;
import jakarta.persistence.EntityManager;

/**
 * Hồ sơ cá nhân với MySQL thật: đọc hồ sơ bác sĩ / hồ sơ bệnh nhân của miền khác, quy tắc sửa họ tên và số điện thoại.
 * Dữ liệu mỗi test có hậu tố ngẫu nhiên để không đụng nhau trong container dùng chung.
 */
@SpringBootTest
@Import({ MySqlTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
class HoSoCaNhanServiceTest {

    @Autowired HoSoCaNhanService hoSoCaNhanService;
    @Autowired TaiKhoanRepository taiKhoanRepository;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;

    private TestFixtures fixtures;

    @BeforeEach
    void khoiTao() {
        fixtures = new TestFixtures(entityManager, transactionManager);
    }

    @Test
    void benhNhanDaLienKetXemDuocHoSoBenhNhan() {
        TaiKhoan taiKhoan = taoTaiKhoan(VaiTro.BENH_NHAN);
        HoSoBenhNhan hoSo = taoHoSoBenhNhan(taiKhoan, TrangThaiLienKet.DA_LIEN_KET);

        HoSoCaNhanResponse ketQua = hoSoCaNhanService.layHoSo(taiKhoan.getId());

        assertThat(ketQua.id()).isEqualTo(taiKhoan.getId());
        assertThat(ketQua.email()).isEqualTo(taiKhoan.getEmail());
        assertThat(ketQua.vaiTro()).isEqualTo(VaiTro.BENH_NHAN);
        assertThat(ketQua.coMatKhau()).isTrue();
        assertThat(ketQua.lienKetGoogle()).isFalse();
        assertThat(ketQua.ngayTao()).isNotNull();
        assertThat(ketQua.bacSi()).isNull();
        assertThat(ketQua.hoSoBenhNhan().id()).isEqualTo(hoSo.getId());
        assertThat(ketQua.hoSoBenhNhan().trangThaiLienKet()).isEqualTo(TrangThaiLienKet.DA_LIEN_KET);
        assertThat(ketQua.hoSoBenhNhan().cccd()).isEqualTo(hoSo.getCccd());
        assertThat(ketQua.hoSoBenhNhan().ngaySinh()).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(ketQua.hoSoBenhNhan().gioiTinh()).isEqualTo(GioiTinh.NAM);
        assertThat(ketQua.hoSoBenhNhan().diaChi()).isEqualTo("1 Lê Lợi");
    }

    @Test
    void hoSoChoXacMinhChiTraTrangThai() {
        TaiKhoan taiKhoan = taoTaiKhoan(VaiTro.BENH_NHAN);
        taoHoSoBenhNhan(taiKhoan, TrangThaiLienKet.CHO_XAC_MINH);

        HoSoCaNhanResponse ketQua = hoSoCaNhanService.layHoSo(taiKhoan.getId());

        assertThat(ketQua.hoSoBenhNhan().trangThaiLienKet()).isEqualTo(TrangThaiLienKet.CHO_XAC_MINH);
        assertThat(ketQua.hoSoBenhNhan().id()).isNull();
        assertThat(ketQua.hoSoBenhNhan().cccd()).isNull();
        assertThat(ketQua.hoSoBenhNhan().hoTen()).isNull();
        assertThat(ketQua.hoSoBenhNhan().diaChi()).isNull();
        assertThat(ketQua.hoSoBenhNhan().soDienThoai()).isNull();
    }

    @Test
    void benhNhanChuaCoHoSoVaQuanTriVienKhongCoKhoiVaiTro() {
        HoSoCaNhanResponse benhNhan = hoSoCaNhanService.layHoSo(taoTaiKhoan(VaiTro.BENH_NHAN).getId());
        // Dùng admin mặc định (KhoiTaoAdminRunner): tạo thêm admin sẽ làm hỏng KhoiTaoAdminTest chạy chung DB
        HoSoCaNhanResponse quanTriVien = hoSoCaNhanService
                .layHoSo(taiKhoanRepository.findByEmail("admin@eclinic.local").orElseThrow().getId());

        assertThat(benhNhan.hoSoBenhNhan()).isNull();
        assertThat(benhNhan.bacSi()).isNull();
        assertThat(quanTriVien.vaiTro()).isEqualTo(VaiTro.QUAN_TRI_VIEN);
        assertThat(quanTriVien.hoSoBenhNhan()).isNull();
        assertThat(quanTriVien.bacSi()).isNull();
    }

    @Test
    void bacSiXemDuocChuyenKhoaVaTrinhDo() {
        TaiKhoan taiKhoan = taoTaiKhoan(VaiTro.BAC_SI);
        ChuyenKhoa chuyenKhoa = new ChuyenKhoa();
        chuyenKhoa.setTenChuyenKhoa("Nội tổng quát " + hauTo());
        fixtures.persist(chuyenKhoa);
        BacSi bacSi = new BacSi();
        bacSi.setTaiKhoan(taiKhoan);
        bacSi.setChuyenKhoa(chuyenKhoa);
        bacSi.setHocVi("ThS.BS");
        bacSi.setSoNamKinhNghiem(9);
        bacSi.setTieuSu("Tiểu sử");
        fixtures.persist(bacSi);

        HoSoCaNhanResponse ketQua = hoSoCaNhanService.layHoSo(taiKhoan.getId());

        assertThat(ketQua.hoSoBenhNhan()).isNull();
        assertThat(ketQua.bacSi().id()).isEqualTo(bacSi.getId());
        assertThat(ketQua.bacSi().idChuyenKhoa()).isEqualTo(chuyenKhoa.getId());
        assertThat(ketQua.bacSi().tenChuyenKhoa()).isEqualTo(chuyenKhoa.getTenChuyenKhoa());
        assertThat(ketQua.bacSi().hocVi()).isEqualTo("ThS.BS");
        assertThat(ketQua.bacSi().soNamKinhNghiem()).isEqualTo(9);
    }

    @Test
    void taiKhoanChiDangNhapGoogle() {
        TaiKhoan taiKhoan = taoTaiKhoan(VaiTro.BENH_NHAN);
        taiKhoan.setMatKhauHash(null);
        taiKhoan.setGoogleId("google-" + hauTo());
        taiKhoan.setSoDienThoai(null);
        taiKhoanRepository.save(taiKhoan);

        HoSoCaNhanResponse ketQua = hoSoCaNhanService.layHoSo(taiKhoan.getId());

        assertThat(ketQua.coMatKhau()).isFalse();
        assertThat(ketQua.lienKetGoogle()).isTrue();
        assertThat(ketQua.soDienThoai()).isNull();
    }

    @Test
    void benhNhanSuaHoTenVaSoDienThoai() {
        TaiKhoan taiKhoan = taoTaiKhoan(VaiTro.BENH_NHAN);
        HoSoBenhNhan hoSo = taoHoSoBenhNhan(taiKhoan, TrangThaiLienKet.DA_LIEN_KET);
        String soMoi = soDienThoaiMoi();

        HoSoCaNhanResponse ketQua = hoSoCaNhanService.capNhat(taiKhoan.getId(),
                new CapNhatHoSoRequest("  Trần Văn Mới  ", soMoi));

        assertThat(ketQua.hoTen()).isEqualTo("Trần Văn Mới");
        assertThat(ketQua.soDienThoai()).isEqualTo(soMoi);
        TaiKhoan daLuu = taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow();
        assertThat(daLuu.getHoTen()).isEqualTo("Trần Văn Mới");
        assertThat(daLuu.getSoDienThoai()).isEqualTo(soMoi);
        // Hồ sơ bệnh nhân có họ tên / SĐT riêng, không bị sửa theo
        assertThat(ketQua.hoSoBenhNhan().hoTen()).isEqualTo(hoSo.getHoTen());
        assertThat(ketQua.hoSoBenhNhan().soDienThoai()).isEqualTo(hoSo.getSoDienThoai());
    }

    @Test
    void khongGuiSoDienThoaiThiGiuSoCuVaGuiLaiSoCuaMinhVanDuoc() {
        TaiKhoan taiKhoan = taoTaiKhoan(VaiTro.BENH_NHAN);
        String soCu = taiKhoan.getSoDienThoai();

        hoSoCaNhanService.capNhat(taiKhoan.getId(), new CapNhatHoSoRequest("Tên Một", null));
        assertThat(taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow().getSoDienThoai()).isEqualTo(soCu);

        HoSoCaNhanResponse ketQua = hoSoCaNhanService.capNhat(taiKhoan.getId(), new CapNhatHoSoRequest("Tên Hai", soCu));
        assertThat(ketQua.hoTen()).isEqualTo("Tên Hai");
        assertThat(ketQua.soDienThoai()).isEqualTo(soCu);
    }

    @Test
    void soDienThoaiCuaTaiKhoanKhacTraVe409VaKhongSuaGi() {
        TaiKhoan taiKhoan = taoTaiKhoan(VaiTro.BENH_NHAN);
        TaiKhoan nguoiKhac = taoTaiKhoan(VaiTro.BENH_NHAN);

        assertMaLoi(() -> hoSoCaNhanService.capNhat(taiKhoan.getId(),
                new CapNhatHoSoRequest("Tên Mới", nguoiKhac.getSoDienThoai())), MaLoi.SO_DIEN_THOAI_DA_TON_TAI);

        TaiKhoan daLuu = taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow();
        assertThat(daLuu.getHoTen()).isEqualTo(taiKhoan.getHoTen());
        assertThat(daLuu.getSoDienThoai()).isEqualTo(taiKhoan.getSoDienThoai());
    }

    @Test
    void bacSiChiDoiDuocSoDienThoai() {
        TaiKhoan taiKhoan = taoTaiKhoan(VaiTro.BAC_SI);
        String soMoi = soDienThoaiMoi();

        // Giữ nguyên họ tên (kể cả thừa khoảng trắng 2 đầu) + số mới: được
        HoSoCaNhanResponse ketQua = hoSoCaNhanService.capNhat(taiKhoan.getId(),
                new CapNhatHoSoRequest(" " + taiKhoan.getHoTen() + " ", soMoi));
        assertThat(ketQua.soDienThoai()).isEqualTo(soMoi);

        // Đổi họ tên: 403, không lưu gì (kể cả số điện thoại gửi kèm)
        assertMaLoi(() -> hoSoCaNhanService.capNhat(taiKhoan.getId(),
                new CapNhatHoSoRequest("Tên Tự Đổi", soDienThoaiMoi())), MaLoi.KHONG_CO_QUYEN);
        TaiKhoan daLuu = taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow();
        assertThat(daLuu.getHoTen()).isEqualTo(taiKhoan.getHoTen());
        assertThat(daLuu.getSoDienThoai()).isEqualTo(soMoi);
    }

    @Test
    void taiKhoanBiVoHieuHoaHoacKhongTonTaiBiTuChoi() {
        TaiKhoan taiKhoan = taoTaiKhoan(VaiTro.BENH_NHAN);
        taiKhoan.setTrangThai(TrangThaiTaiKhoan.VO_HIEU_HOA);
        taiKhoanRepository.save(taiKhoan);
        CapNhatHoSoRequest request = new CapNhatHoSoRequest("Tên Mới", null);

        assertMaLoi(() -> hoSoCaNhanService.layHoSo(taiKhoan.getId()), MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertMaLoi(() -> hoSoCaNhanService.capNhat(taiKhoan.getId(), request), MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertThat(taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow().getHoTen()).isEqualTo(taiKhoan.getHoTen());

        assertMaLoi(() -> hoSoCaNhanService.layHoSo(Long.MAX_VALUE), MaLoi.CHUA_DANG_NHAP);
        assertMaLoi(() -> hoSoCaNhanService.capNhat(Long.MAX_VALUE, request), MaLoi.CHUA_DANG_NHAP);
    }

    private TaiKhoan taoTaiKhoan(VaiTro vaiTro) {
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setHoTen("Người dùng " + hauTo());
        taiKhoan.setEmail("hs-" + hauTo() + "@example.com");
        taiKhoan.setMatKhauHash("x");
        taiKhoan.setSoDienThoai(soDienThoaiMoi());
        taiKhoan.setVaiTro(vaiTro);
        taiKhoan.setTrangThai(TrangThaiTaiKhoan.DA_KICH_HOAT);
        return taiKhoanRepository.save(taiKhoan);
    }

    private HoSoBenhNhan taoHoSoBenhNhan(TaiKhoan taiKhoan, TrangThaiLienKet trangThaiLienKet) {
        HoSoBenhNhan hoSo = new HoSoBenhNhan();
        hoSo.setTaiKhoan(taiKhoan);
        hoSo.setCccd(TestFixtures.cccdNgauNhien());
        hoSo.setHoTen("Hồ sơ " + hauTo());
        hoSo.setNgaySinh(LocalDate.of(1990, 5, 20));
        hoSo.setGioiTinh(GioiTinh.NAM);
        hoSo.setSoDienThoai("0900000000");
        hoSo.setDiaChi("1 Lê Lợi");
        hoSo.setTrangThaiLienKet(trangThaiLienKet);
        return fixtures.persist(hoSo);
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

    /** Số điện thoại hợp lệ ngẫu nhiên (cột so_dien_thoai là UNIQUE). */
    private static String soDienThoaiMoi() {
        return "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

}
