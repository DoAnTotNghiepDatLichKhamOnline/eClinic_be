package iuh.fit.se.eclinic.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;

/**
 * Lấy thông tin người đang gọi API từ JWT. Dùng trong controller/service: {@code NguoiDungHienTai.layIdTaiKhoan()}.
 */
public final class NguoiDungHienTai {

    private NguoiDungHienTai() {
    }

    /**
     * Người gọi có gửi JWT hợp lệ không. Dùng ở đường dẫn công khai nhận cả khách lẫn người đã đăng nhập (vd đặt lịch
     * khám); các hàm lấy thông tin bên dưới ném CHUA_DANG_NHAP khi không có JWT.
     */
    public static boolean daDangNhap() {
        return SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken;
    }

    public static Long layIdTaiKhoan() {
        return Long.valueOf(layToken().getToken().getSubject());
    }

    public static VaiTro layVaiTro() {
        return VaiTro.valueOf(layToken().getToken().getClaimAsString(JwtConfig.CLAIM_VAI_TRO));
    }

    /** Mã phiên đăng nhập của token đang dùng (claim "phien"); null nếu token được cấp không kèm phiên. */
    public static String layMaPhien() {
        return layToken().getToken().getClaimAsString(JwtConfig.CLAIM_PHIEN);
    }

    private static JwtAuthenticationToken layToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return token;
        }
        throw new LoiNghiepVu(MaLoi.CHUA_DANG_NHAP);
    }

}
