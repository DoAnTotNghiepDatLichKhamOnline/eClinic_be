package iuh.fit.se.eclinic.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Đánh giá lượt khám, prefix {@code app.danh-gia} trong application.yml.
 *
 * @param soNgayDuocSua bệnh nhân sửa được đánh giá trong số ngày này kể từ lúc gửi
 */
@ConfigurationProperties("app.danh-gia")
public record DanhGiaProperties(int soNgayDuocSua) {
}
