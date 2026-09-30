package iuh.fit.se.eclinic.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Kết quả đăng nhập / làm mới phiên.
 *
 * @param accessToken        JWT gửi kèm header {@code Authorization: Bearer ...}
 * @param refreshToken       dùng 1 lần để lấy cặp token mới; mỗi lần làm mới token cũ hết hiệu lực.
 *                           KHÔNG có trong JSON: controller đặt vào cookie HttpOnly (xem CookiePhien)
 * @param loaiToken          luôn là "Bearer"
 * @param thoiHanAccessToken số giây access token còn hiệu lực
 * @param taiKhoan           thông tin tài khoản đang đăng nhập
 */
public record DangNhapResponse(
        String accessToken,
        @JsonIgnore String refreshToken,
        String loaiToken,
        long thoiHanAccessToken,
        TaiKhoanResponse taiKhoan) {

    /** Không in token (tránh lộ khi response bị log). */
    @Override
    public String toString() {
        return "DangNhapResponse[taiKhoan=" + taiKhoan + "]";
    }

}
