package iuh.fit.se.eclinic.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình gửi email, prefix {@code app.email} trong application.yml.
 *
 * @param cheDo           CONSOLE: chỉ ghi ra log (máy dev); SMTP: gửi thật qua spring.mail.*
 * @param frontendUrl     địa chỉ frontend, liên kết trong email trỏ về đây
 * @param duongDanXacThuc trang của frontend nhận token xác thực email
 * @param duongDanDatLai  trang của frontend nhận token đặt lại mật khẩu
 */
@ConfigurationProperties("app.email")
public record EmailProperties(
        @DefaultValue("CONSOLE") CheDoEmail cheDo,
        @DefaultValue("http://localhost:5173") String frontendUrl,
        @DefaultValue("/verify-email") String duongDanXacThuc,
        @DefaultValue("/reset-password") String duongDanDatLai) {

    public enum CheDoEmail {
        CONSOLE,
        SMTP
    }

}
