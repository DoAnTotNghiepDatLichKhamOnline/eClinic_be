package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.security.JwtConfig;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.RedisTestcontainersConfiguration;
import iuh.fit.se.eclinic.identity.dto.request.DangNhapRequest;
import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;
import iuh.fit.se.eclinic.identity.dto.response.PhienDangNhapResponse;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.DangNhapService;
import iuh.fit.se.eclinic.identity.service.PhienDangNhapService;

/**
 * Danh sách thiết bị đang đăng nhập và đăng xuất từ xa, với MySQL + Redis thật. Mỗi "thiết bị" là 1 lần đăng nhập;
 * phiên hiện tại được truyền vào bằng mã phiên trong access token của thiết bị đó (claim "phien").
 */
@SpringBootTest
@Import({ MySqlTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
class PhienDangNhapServiceTest {

    private static final String MAT_KHAU = "123456";

    @Autowired PhienDangNhapService phienDangNhapService;
    @Autowired DangNhapService dangNhapService;
    @Autowired TaiKhoanRepository taiKhoanRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtDecoder jwtDecoder;

    @Test
    void danhSachCoPhienHienTaiDungDauVaDuocDanhDau() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        DangNhapResponse mayA = dangNhap(taiKhoan, "May A");
        DangNhapResponse mayB = dangNhap(taiKhoan, "May B");

        // Máy B đăng nhập sau nên hoạt động gần hơn, nhưng phiên hiện tại (máy A) vẫn đứng đầu
        List<PhienDangNhapResponse> cuaMayA = phienDangNhapService.layDanhSach(taiKhoan.getId(), maPhien(mayA));

        assertThat(cuaMayA).extracting(PhienDangNhapResponse::id).containsExactly(maPhien(mayA), maPhien(mayB));
        assertThat(cuaMayA).extracting(PhienDangNhapResponse::thietBi).containsExactly("May A", "May B");
        assertThat(cuaMayA).extracting(PhienDangNhapResponse::hienTai).containsExactly(true, false);
        PhienDangNhapResponse phienA = cuaMayA.get(0);
        assertThat(phienA.dangNhapLuc()).isNotNull();
        assertThat(phienA.hoatDongLuc()).isNotNull();
        assertThat(phienA.hetHanLuc()).isAfter(phienA.hoatDongLuc().plusDays(6));

        assertThat(phienDangNhapService.layDanhSach(taiKhoan.getId(), maPhien(mayB)))
                .extracting(PhienDangNhapResponse::id).containsExactly(maPhien(mayB), maPhien(mayA));
    }

    @Test
    void tokenKhongCoMaPhienThiKhongPhienNaoLaHienTai() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        DangNhapResponse mayA = dangNhap(taiKhoan, "May A");
        DangNhapResponse mayB = dangNhap(taiKhoan, "May B");

        List<PhienDangNhapResponse> danhSach = phienDangNhapService.layDanhSach(taiKhoan.getId(), null);

        // Theo lần hoạt động gần nhất
        assertThat(danhSach).extracting(PhienDangNhapResponse::id).containsExactly(maPhien(mayB), maPhien(mayA));
        assertThat(danhSach).extracting(PhienDangNhapResponse::hienTai).containsOnly(false);
    }

    @Test
    void lamMoiGiuNguyenIdVaThoiDiemDangNhapChiDoiLanHoatDong() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        DangNhapResponse lanDau = dangNhap(taiKhoan, "May A");
        PhienDangNhapResponse truoc = phienDangNhapService.layDanhSach(taiKhoan.getId(), maPhien(lanDau)).get(0);

        DangNhapResponse lanHai = dangNhapService.lamMoi(lanDau.refreshToken());

        List<PhienDangNhapResponse> danhSach = phienDangNhapService.layDanhSach(taiKhoan.getId(), maPhien(lanHai));
        assertThat(danhSach).hasSize(1);
        PhienDangNhapResponse sau = danhSach.get(0);
        assertThat(sau.id()).isEqualTo(truoc.id());
        assertThat(sau.dangNhapLuc()).isEqualTo(truoc.dangNhapLuc());
        assertThat(sau.hoatDongLuc()).isAfter(truoc.hoatDongLuc());
        assertThat(sau.thietBi()).isEqualTo("May A");
        assertThat(sau.hienTai()).isTrue();
    }

    @Test
    void dangXuatPhienKhacThiPhienDoKhongLamMoiDuocPhienMinhVanCon() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        DangNhapResponse mayA = dangNhap(taiKhoan, "May A");
        DangNhapResponse mayB = dangNhap(taiKhoan, "May B");

        phienDangNhapService.dangXuat(taiKhoan.getId(), maPhien(mayA), maPhien(mayB));

        assertMaLoi(() -> dangNhapService.lamMoi(mayB.refreshToken()), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        assertThat(phienDangNhapService.layDanhSach(taiKhoan.getId(), maPhien(mayA)))
                .extracting(PhienDangNhapResponse::id).containsExactly(maPhien(mayA));
        assertThat(dangNhapService.lamMoi(mayA.refreshToken()).accessToken()).isNotBlank();
        // Phiên đã đăng xuất thì lần sau là "không tìm thấy"
        assertMaLoi(() -> phienDangNhapService.dangXuat(taiKhoan.getId(), maPhien(mayA), maPhien(mayB)),
                MaLoi.KHONG_TIM_THAY);
    }

    @Test
    void maPhienSaiHoacCuaTaiKhoanKhacBao404() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        DangNhapResponse cuaToi = dangNhap(taiKhoan, "May A");
        TaiKhoan nguoiKhac = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        DangNhapResponse cuaNguoiKhac = dangNhap(nguoiKhac, "May X");

        assertMaLoi(() -> phienDangNhapService.dangXuat(taiKhoan.getId(), maPhien(cuaToi),
                UUID.randomUUID().toString()), MaLoi.KHONG_TIM_THAY);
        assertMaLoi(() -> phienDangNhapService.dangXuat(taiKhoan.getId(), maPhien(cuaToi), maPhien(cuaNguoiKhac)),
                MaLoi.KHONG_TIM_THAY);

        // Phiên của người khác không bị đụng tới
        assertThat(dangNhapService.lamMoi(cuaNguoiKhac.refreshToken()).accessToken()).isNotBlank();
    }

    @Test
    void dangXuatPhienHienTaiBao400VaPhienVanCon() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        DangNhapResponse mayA = dangNhap(taiKhoan, "May A");

        assertMaLoi(() -> phienDangNhapService.dangXuat(taiKhoan.getId(), maPhien(mayA), maPhien(mayA)),
                MaLoi.DU_LIEU_KHONG_HOP_LE);

        assertThat(dangNhapService.lamMoi(mayA.refreshToken()).accessToken()).isNotBlank();
    }

    @Test
    void dangXuatCacPhienKhacTraVeSoPhienVaGiuPhienHienTai() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        DangNhapResponse mayA = dangNhap(taiKhoan, "May A");
        DangNhapResponse mayB = dangNhap(taiKhoan, "May B");
        DangNhapResponse mayC = dangNhap(taiKhoan, "May C");

        assertThat(phienDangNhapService.dangXuatCacPhienKhac(taiKhoan.getId(), maPhien(mayA))).isEqualTo(2);

        assertMaLoi(() -> dangNhapService.lamMoi(mayB.refreshToken()), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        assertMaLoi(() -> dangNhapService.lamMoi(mayC.refreshToken()), MaLoi.PHIEN_DANG_NHAP_KHONG_HOP_LE);
        assertThat(dangNhapService.lamMoi(mayA.refreshToken()).accessToken()).isNotBlank();
        // Không còn phiên nào khác
        assertThat(phienDangNhapService.dangXuatCacPhienKhac(taiKhoan.getId(), maPhien(mayA))).isZero();
    }

    @Test
    void taiKhoanBiVoHieuHoaBao403TaiKhoanKhongConBao401() {
        TaiKhoan taiKhoan = taoTaiKhoan(TrangThaiTaiKhoan.DA_KICH_HOAT);
        DangNhapResponse mayA = dangNhap(taiKhoan, "May A");
        TaiKhoan moiNhat = taiKhoanRepository.findById(taiKhoan.getId()).orElseThrow();
        moiNhat.setTrangThai(TrangThaiTaiKhoan.VO_HIEU_HOA);
        taiKhoanRepository.save(moiNhat);
        Long id = taiKhoan.getId();
        String maPhien = maPhien(mayA);

        assertMaLoi(() -> phienDangNhapService.layDanhSach(id, maPhien), MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertMaLoi(() -> phienDangNhapService.dangXuat(id, maPhien, "bat-ky"), MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertMaLoi(() -> phienDangNhapService.dangXuatCacPhienKhac(id, maPhien), MaLoi.TAI_KHOAN_BI_VO_HIEU_HOA);
        assertMaLoi(() -> phienDangNhapService.layDanhSach(Long.MAX_VALUE, null), MaLoi.CHUA_DANG_NHAP);
    }

    private TaiKhoan taoTaiKhoan(TrangThaiTaiKhoan trangThai) {
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setHoTen("Bệnh nhân test");
        taiKhoan.setEmail("phien-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        taiKhoan.setMatKhauHash(passwordEncoder.encode(MAT_KHAU));
        taiKhoan.setVaiTro(VaiTro.BENH_NHAN);
        taiKhoan.setTrangThai(trangThai);
        return taiKhoanRepository.save(taiKhoan);
    }

    private DangNhapResponse dangNhap(TaiKhoan taiKhoan, String thietBi) {
        return dangNhapService.dangNhap(new DangNhapRequest(taiKhoan.getEmail(), MAT_KHAU), thietBi);
    }

    /** Mã phiên trong access token (claim "phien"). */
    private String maPhien(DangNhapResponse ketQua) {
        return jwtDecoder.decode(ketQua.accessToken()).getClaimAsString(JwtConfig.CLAIM_PHIEN);
    }

    private static void assertMaLoi(Runnable hanhDong, MaLoi maLoi) {
        assertThatThrownBy(hanhDong::run)
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(maLoi);
    }

}
