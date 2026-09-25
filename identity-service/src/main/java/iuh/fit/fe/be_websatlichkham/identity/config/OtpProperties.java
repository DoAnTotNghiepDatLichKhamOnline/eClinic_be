package iuh.fit.fe.be_websatlichkham.identity.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình OTP (lưu ở Redis), prefix {@code app.otp} trong application.yml.
 *
 * @param ttl         thời gian sống của mã OTP
 * @param maxAttempts số lần nhập sai tối đa trước khi mã bị huỷ
 * @param length      số chữ số của mã
 */
@ConfigurationProperties("app.otp")
public record OtpProperties(
        @DefaultValue("300s") Duration ttl,
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("6") int length) {
}
