package iuh.fit.se.eclinic.identity.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Thời hạn các liên kết gửi qua email, prefix {@code app.lien-ket} trong application.yml. */
@ConfigurationProperties("app.lien-ket")
public record LienKetProperties(
        @DefaultValue("24h") Duration thoiHanXacThucEmail,
        @DefaultValue("60s") Duration thoiGianChoGuiLai,
        @DefaultValue("15m") Duration thoiHanDatLaiMatKhau,
        @DefaultValue("1h") Duration thoiHanDoiEmail) {
}
