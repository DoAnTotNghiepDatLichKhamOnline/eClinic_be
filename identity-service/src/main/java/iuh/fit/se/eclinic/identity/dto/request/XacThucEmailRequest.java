package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Token lấy từ liên kết trong email (frontend đọc trên URL rồi gửi lên): liên kết kích hoạt tài khoản, liên kết xác nhận
 * đổi email.
 */
public record XacThucEmailRequest(

        @NotBlank(message = "{kiemTra.token.batBuoc}")
        @Size(max = 100, message = "{kiemTra.token.khongHopLe}")
        String token) {
}
