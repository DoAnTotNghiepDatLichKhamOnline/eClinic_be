package iuh.fit.se.eclinic.gateway.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cấu hình CORS của gateway (app.cors trong application.yml). */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> nguonDuocPhep) {
}
