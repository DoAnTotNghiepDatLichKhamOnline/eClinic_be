package iuh.fit.se.eclinic.identity.dto.request;

import iuh.fit.se.eclinic.identity.validation.MatKhauHopLe;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Đặt mật khẩu ở lần đăng nhập đầu của tài khoản đang mang mật khẩu mặc định (đăng nhập trả PHAI_DOI_MAT_KHAU). */
public record DatMatKhauLanDauRequest(

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        @Size(max = 200, message = "Mật khẩu hiện tại không hợp lệ")
        String matKhauHienTai,

        @MatKhauHopLe
        String matKhauMoi) {

    /** Không in mật khẩu (tránh lộ khi request bị log). */
    @Override
    public String toString() {
        return "DatMatKhauLanDauRequest[email=" + email + "]";
    }

}
