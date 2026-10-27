package iuh.fit.se.eclinic.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Cấu hình gửi email, prefix {@code app.email} trong application.yml. */
@ConfigurationProperties("app.email")
public record EmailProperties(
        @DefaultValue("CONSOLE") CheDoEmail cheDo,
        @DefaultValue("http://localhost:5173") String frontendUrl,
        @DefaultValue("/verify-email") String duongDanXacThuc,
        @DefaultValue("/reset-password") String duongDanDatLai,
        @DefaultValue("/confirm-email-change") String duongDanDoiEmail) {

    public enum CheDoEmail {
        CONSOLE,
        SMTP
    }

}
