package iuh.fit.se.eclinic.identity.dto.request;

import iuh.fit.se.eclinic.identity.validation.MatKhauHopLe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Đổi mật khẩu khi đang đăng nhập. Mật khẩu hiện tại không dùng @MatKhauHopLe (như DangNhapRequest): sai quy tắc
 * cũng chỉ là "mật khẩu hiện tại không đúng".
 */
public record DoiMatKhauRequest(

        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        @Size(max = 1000, message = "Mật khẩu quá dài")
        String matKhauCu,

        @MatKhauHopLe
        String matKhauMoi) {

    /** Không in mật khẩu (tránh lộ khi request bị log). */
    @Override
    public String toString() {
        return "DoiMatKhauRequest[***]";
    }

}
