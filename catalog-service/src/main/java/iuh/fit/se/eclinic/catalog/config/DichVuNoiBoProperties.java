package iuh.fit.se.eclinic.catalog.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Địa chỉ gọi thẳng (không qua gateway) tới API nội bộ /noi-bo/** của các service khác khi quản lý bác sĩ.
 *
 * @param thoiGianChoPhanHoi dài vì hủy mọi ca sắp tới của 1 bác sĩ chạy trong 1 lần gọi
 */
@ConfigurationProperties("app.dich-vu-noi-bo")
public record DichVuNoiBoProperties(
        @DefaultValue("http://localhost:8081") String identityUrl,
        @DefaultValue("http://localhost:8084") String bookingUrl,
        @DefaultValue("5s") Duration thoiGianChoKetNoi,
        @DefaultValue("60s") Duration thoiGianChoPhanHoi) {
}
