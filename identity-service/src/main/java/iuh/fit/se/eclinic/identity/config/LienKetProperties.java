package iuh.fit.se.eclinic.identity.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Thời hạn các liên kết gửi qua email, prefix {@code app.lien-ket} trong application.yml.
 *
 * @param thoiHanXacThucEmail thời gian sống của liên kết kích hoạt tài khoản
 * @param thoiGianChoGuiLai   khoảng chờ tối thiểu giữa 2 lần gửi liên kết cùng mục đích cho cùng 1 tài khoản
 * @param thoiHanDatLaiMatKhau thời gian sống của liên kết đặt lại mật khẩu
 * @param thoiHanDoiEmail     thời gian sống của liên kết xác nhận đổi email
 */
@ConfigurationProperties("app.lien-ket")
public record LienKetProperties(
        @DefaultValue("24h") Duration thoiHanXacThucEmail,
        @DefaultValue("60s") Duration thoiGianChoGuiLai,
        @DefaultValue("15m") Duration thoiHanDatLaiMatKhau,
        @DefaultValue("1h") Duration thoiHanDoiEmail) {
}
