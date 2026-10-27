package iuh.fit.se.eclinic.identity.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Giới hạn đổi ảnh đại diện, prefix {@code app.anh-dai-dien} trong application.yml. */
@ConfigurationProperties("app.anh-dai-dien")
public record AnhDaiDienProperties(
        @DefaultValue("10") int soLanTaiLenToiDa,
        @DefaultValue("1h") Duration khoangDem) {
}
