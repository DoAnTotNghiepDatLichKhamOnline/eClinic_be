package iuh.fit.se.eclinic.identity.dto.request;

import iuh.fit.se.eclinic.identity.validation.MatKhauHopLe;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Đặt mật khẩu ở lần đăng nhập đầu của tài khoản đang mang mật khẩu mặc định. */
public record DatMatKhauLanDauRequest(

        @NotBlank(message = "{kiemTra.email.batBuoc}")
        @Email(message = "{kiemTra.email.dinhDang}")
        @Size(max = 255, message = "{kiemTra.email.toiDa}")
        String email,

        @NotBlank(message = "{kiemTra.matKhauHienTai.batBuoc}")
        @Size(max = 200, message = "{kiemTra.matKhauHienTai.khongHopLe}")
        String matKhauHienTai,

        @MatKhauHopLe
        String matKhauMoi) {

    /** Không in mật khẩu (tránh lộ khi request bị log). */
    @Override
    public String toString() {
        return "DatMatKhauLanDauRequest[email=" + email + "]";
    }

}
