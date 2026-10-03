package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Refresh token gửi trong body khi làm mới phiên / đăng xuất. Không bắt buộc: trình duyệt gửi bằng cookie
 * (xem CookiePhien), body chỉ dùng khi thử API bằng Swagger, curl.
 */
public record PhienDangNhapRequest(

        @Size(max = 100, message = "Refresh token không hợp lệ")
        String refreshToken) {

    /** Không in token (tránh lộ khi request bị log). */
    @Override
    public String toString() {
        return "PhienDangNhapRequest[***]";
    }

}
