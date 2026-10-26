package iuh.fit.se.eclinic.gateway.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình CORS của gateway (app.cors trong application.yml).
 *
 * @param nguonDuocPhep các origin của frontend được phép gọi API, vd {@code http://localhost:5173}
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> nguonDuocPhep) {
}
