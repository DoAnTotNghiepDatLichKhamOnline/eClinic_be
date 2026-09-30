package iuh.fit.se.eclinic.identity.dto.request;

import iuh.fit.se.eclinic.identity.validation.MatKhauHopLe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Đặt mật khẩu mới bằng token trong liên kết email (AUTH-03). Mật khẩu được kiểm tra trước khi dùng token,
 * nên mật khẩu sai quy tắc không làm mất liên kết.
 */
public record DatLaiMatKhauRequest(

        @NotBlank(message = "Token không được để trống")
        @Size(max = 100, message = "Token không hợp lệ")
        String token,

        @MatKhauHopLe
        String matKhauMoi) {

    /** Không in token và mật khẩu (tránh lộ khi request bị log). */
    @Override
    public String toString() {
        return "DatLaiMatKhauRequest[***]";
    }

}
