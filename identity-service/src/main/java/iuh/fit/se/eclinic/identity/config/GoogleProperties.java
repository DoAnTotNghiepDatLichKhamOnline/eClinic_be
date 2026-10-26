package iuh.fit.se.eclinic.identity.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Đăng nhập bằng Google, prefix {@code app.google} trong application.yml.
 *
 * @param clientId   OAuth Client ID của frontend (GOOGLE_CLIENT_ID); để trống thì đăng nhập Google trả 503
 * @param jwkSetUri  nơi lấy khoá công khai của Google để kiểm tra chữ ký ID token
 * @param thoiGianCho thời gian chờ tối đa khi kết nối / đọc khoá công khai của Google
 */
@ConfigurationProperties("app.google")
public record GoogleProperties(
        @DefaultValue("") String clientId,
        @DefaultValue("https://www.googleapis.com/oauth2/v3/certs") String jwkSetUri,
        @DefaultValue("5s") Duration thoiGianCho) {

    public boolean daCauHinh() {
        return clientId != null && !clientId.isBlank();
    }

}
