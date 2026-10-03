package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Yêu cầu đổi email đăng nhập của người đang đăng nhập. Mật khẩu hiện tại không dùng @MatKhauHopLe (như
 * DoiMatKhauRequest): sai quy tắc cũng chỉ là "mật khẩu hiện tại không đúng".
 */
public record DoiEmailRequest(

        @NotBlank(message = "Email mới không được để trống")
        @Email(message = "Email mới không đúng định dạng")
        @Size(max = 255, message = "Email mới quá dài")
        String emailMoi,

        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        @Size(max = 1000, message = "Mật khẩu quá dài")
        String matKhauHienTai) {

    /** Không in mật khẩu (tránh lộ khi request bị log). */
    @Override
    public String toString() {
        return "DoiEmailRequest[***]";
    }

}
