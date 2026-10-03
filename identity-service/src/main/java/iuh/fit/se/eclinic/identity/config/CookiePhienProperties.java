package iuh.fit.se.eclinic.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cookie chứa refresh token, prefix {@code app.cookie-phien} trong application.yml. Tên và đường dẫn cookie cố định
 * (xem CookiePhien).
 *
 * @param secure   chỉ gửi qua HTTPS; trình duyệt và curl coi http://localhost là an toàn nên dev vẫn để true.
 *                 Chỉ tắt (COOKIE_SECURE=false) khi thử qua IP mạng LAN bằng http
 * @param sameSite Strict: trình duyệt không gửi cookie trong request từ trang web khác (chống CSRF)
 */
@ConfigurationProperties("app.cookie-phien")
public record CookiePhienProperties(
        @DefaultValue("true") boolean secure,
        @DefaultValue("Strict") String sameSite) {
}
