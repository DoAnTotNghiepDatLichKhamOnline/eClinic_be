package iuh.fit.se.eclinic.notification.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Gửi email từ hàng chờ email_gui, prefix {@code app.email} trong application.yml.
 *
 * @param cheDo       CONSOLE: chỉ ghi ra log (máy dev); SMTP: gửi thật qua spring.mail.*
 * @param smtpXacThuc máy chủ mail có đòi đăng nhập không (Gmail: có; máy chủ nhận thử khi dev: không)
 * @param chuKyGui    khoảng nghỉ giữa 2 lần job gửi các email đến hạn
 * @param cacLanCho   khoảng chờ trước mỗi lần thử lại, theo thứ tự; thử hết danh sách mà vẫn lỗi thì bỏ email đó
 * @param kichThuocLo số email tối đa gửi trong 1 lần chạy
 * @param soNgayGiu   email đã gửi / đã bỏ được giữ lại bao nhiêu ngày rồi xoá
 */
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
