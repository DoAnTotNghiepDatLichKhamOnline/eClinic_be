package iuh.fit.se.eclinic.booking.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Gửi thông báo sang notification-service, prefix {@code app.thong-bao} trong application.yml. */
@ConfigurationProperties("app.thong-bao")
public record ThongBaoProperties(String url, Duration chuKyGui, Duration thoiGianCho) {
}
