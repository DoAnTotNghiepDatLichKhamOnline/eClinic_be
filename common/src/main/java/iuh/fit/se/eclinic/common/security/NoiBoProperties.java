package iuh.fit.se.eclinic.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Lời gọi giữa các service, prefix {@code app.noi-bo} (application-common.yml). */
@ConfigurationProperties("app.noi-bo")
public record NoiBoProperties(String khoa) {
}
