package iuh.fit.se.eclinic.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Kết quả đăng nhập / làm mới phiên. */
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
