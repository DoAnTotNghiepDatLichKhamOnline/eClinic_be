package iuh.fit.se.eclinic.identity.controller;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.identity.config.CookiePhienProperties;
import iuh.fit.se.eclinic.identity.config.DangNhapProperties;
import lombok.RequiredArgsConstructor;

/**
 * Tạo header Set-Cookie cho refresh token. Cookie HttpOnly (JavaScript không đọc được, XSS không lấy được),
 * chỉ gửi kèm các API /api/auth/*; access token vẫn trả trong body và frontend chỉ giữ trong bộ nhớ.
 */
@Component
@RequiredArgsConstructor
public class CookiePhien {

    public static final String TEN = "eclinic_rt";
    /** Chỉ các API xác thực cần refresh token (làm mới phiên, đăng xuất). */
    public static final String DUONG_DAN = "/api/auth";

    private final CookiePhienProperties cookiePhienProperties;
    private final DangNhapProperties dangNhapProperties;

    /** Cookie sống bằng refresh token (mỗi lần làm mới cấp token + cookie mới). */
    public String tao(String refreshToken) {
        return taoCookie(refreshToken, dangNhapProperties.thoiHanRefreshToken().toSeconds());
    }

    /** Xoá cookie phía trình duyệt (Max-Age=0). */
    public String xoa() {
        return taoCookie("", 0);
    }

    private String taoCookie(String giaTri, long soGiay) {
        return ResponseCookie.from(TEN, giaTri)
                .httpOnly(true)
                .secure(cookiePhienProperties.secure())
                .sameSite(cookiePhienProperties.sameSite())
                .path(DUONG_DAN)
                .maxAge(soGiay)
                .build()
                .toString();
    }

}
