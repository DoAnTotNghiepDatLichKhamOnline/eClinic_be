package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Token lấy từ liên kết kích hoạt trong email (frontend đọc trên URL rồi gửi lên).
 */
public record XacThucEmailRequest(

        @NotBlank(message = "Token không được để trống")
        @Size(max = 100, message = "Token không hợp lệ")
        String token) {
}
