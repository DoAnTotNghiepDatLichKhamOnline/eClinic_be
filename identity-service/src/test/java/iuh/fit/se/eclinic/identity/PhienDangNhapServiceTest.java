package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.security.JwtConfig;
import iuh.fit.se.eclinic.identity.dto.request.DangNhapRequest;
import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.DangNhapService;
import iuh.fit.se.eclinic.identity.service.PhienDangNhapService;

/** Danh sách thiết bị đang đăng nhập và đăng xuất từ xa, với MySQL + Redis thật. */
class PhienDangNhapServiceTest extends KiemThuTichHop {

    private static final String MAT_KHAU = "123456";

    @Autowired PhienDangNhapService phienDangNhapService;
    @Autowired DangNhapService dangNhapService;
    @Autowired TaiKhoanRepository taiKhoanRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtDecoder jwtDecoder;

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
