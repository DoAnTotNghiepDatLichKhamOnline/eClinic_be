package iuh.fit.se.eclinic.booking.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Giới hạn khách tự xin cấp lại liên kết phiếu khám, prefix {@code app.cap-lai-lien-ket}. */
@ConfigurationProperties("app.cap-lai-lien-ket")
public record CapLaiLienKetProperties(
        @DefaultValue("3") int soLanToiDaMoiMa,
        @DefaultValue("10") int soLanToiDaMoiIp,
        @DefaultValue("1h") Duration cuaSo) {
}
