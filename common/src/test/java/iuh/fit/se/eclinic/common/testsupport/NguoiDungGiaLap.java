package iuh.fit.se.eclinic.common.testsupport;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import iuh.fit.se.eclinic.common.enums.VaiTro;
import iuh.fit.se.eclinic.common.security.JwtConfig;

/**
 * Giả lập người dùng đã đăng nhập trong test MockMvc (@WebMvcTest hoặc @SpringBootTest + @AutoConfigureMockMvc):
 * {@code mockMvc.perform(get("/api/...").with(NguoiDungGiaLap.bacSi(5L)))}.
 * <p>
 * jwt() bỏ qua bộ giải mã và JwtAuthenticationConverter, nên quyền ROLE_... được gán trực tiếp ở đây.
 */
public final class NguoiDungGiaLap {

    private NguoiDungGiaLap() {
    }

    public static RequestPostProcessor quanTriVien() {
        return giaLap(1L, VaiTro.QUAN_TRI_VIEN);
    }

    public static RequestPostProcessor bacSi(Long idTaiKhoan) {
        return giaLap(idTaiKhoan, VaiTro.BAC_SI);
    }

    public static RequestPostProcessor benhNhan(Long idTaiKhoan) {
        return giaLap(idTaiKhoan, VaiTro.BENH_NHAN);
    }

    private static RequestPostProcessor giaLap(Long idTaiKhoan, VaiTro vaiTro) {
        return jwt()
                .jwt(token -> token
                        .subject(String.valueOf(idTaiKhoan))
                        .claim(JwtConfig.CLAIM_VAI_TRO, vaiTro.name()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + vaiTro.name()));
    }

}
