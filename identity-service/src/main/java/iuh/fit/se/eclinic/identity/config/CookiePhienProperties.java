package iuh.fit.se.eclinic.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Cookie chứa refresh token, prefix {@code app.cookie-phien} trong application.yml. */
@ConfigurationProperties("app.cookie-phien")
public record CookiePhienProperties(
        @DefaultValue("true") boolean secure,
        @DefaultValue("Strict") String sameSite) {
}
