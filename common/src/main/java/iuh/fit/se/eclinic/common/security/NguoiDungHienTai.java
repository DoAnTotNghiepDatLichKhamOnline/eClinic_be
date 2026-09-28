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

    public static Long layIdTaiKhoan() {
        return Long.valueOf(layToken().getToken().getSubject());
    }

    public static VaiTro layVaiTro() {
        return VaiTro.valueOf(layToken().getToken().getClaimAsString(JwtConfig.CLAIM_VAI_TRO));
    }

    private static JwtAuthenticationToken layToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return token;
        }
        throw new LoiNghiepVu(MaLoi.CHUA_DANG_NHAP);
    }

}
