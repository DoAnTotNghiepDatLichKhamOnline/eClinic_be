package iuh.fit.se.eclinic.notification.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Gửi email từ hàng chờ email_gui, prefix {@code app.email} trong application.yml. */
@ConfigurationProperties("app.email")
public record EmailProperties(
        @DefaultValue("CONSOLE") CheDoEmail cheDo,
        @DefaultValue("true") boolean smtpXacThuc,
        @DefaultValue("5s") Duration chuKyGui,
        @DefaultValue({"1m", "5m", "30m", "2h"}) List<Duration> cacLanCho,
        @DefaultValue("20") int kichThuocLo,
        @DefaultValue("30") int soNgayGiu) {

    public enum CheDoEmail {
        CONSOLE,
        SMTP
    }

}
