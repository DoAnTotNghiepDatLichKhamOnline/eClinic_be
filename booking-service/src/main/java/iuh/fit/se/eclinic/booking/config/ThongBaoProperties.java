package iuh.fit.se.eclinic.booking.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gửi thông báo sang notification-service, prefix {@code app.thong-bao} trong application.yml.
 *
 * @param url         địa chỉ notification-service (gọi thẳng, không qua gateway)
 * @param chuKyGui    khoảng nghỉ giữa 2 lần job gửi các sự kiện chưa gửi
 * @param thoiGianCho thời gian chờ kết nối / chờ phản hồi
 */
@ConfigurationProperties("app.thong-bao")
public record ThongBaoProperties(String url, Duration chuKyGui, Duration thoiGianCho) {
}
