package iuh.fit.se.eclinic.identity.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Đăng nhập bằng Google, prefix {@code app.google} trong application.yml. */
@ConfigurationProperties("app.google")
public record GoogleProperties(
        @DefaultValue("") String clientId,
        @DefaultValue("https://www.googleapis.com/oauth2/v3/certs") String jwkSetUri,
        @DefaultValue("5s") Duration thoiGianCho) {

    public boolean daCauHinh() {
        return clientId != null && !clientId.isBlank();
    }

}
