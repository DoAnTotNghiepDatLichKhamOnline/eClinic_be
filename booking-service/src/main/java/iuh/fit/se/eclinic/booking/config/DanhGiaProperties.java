package iuh.fit.se.eclinic.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Đánh giá lượt khám, prefix {@code app.danh-gia} trong application.yml. */
@ConfigurationProperties("app.danh-gia")
public record DanhGiaProperties(int soNgayDuocSua) {
}
