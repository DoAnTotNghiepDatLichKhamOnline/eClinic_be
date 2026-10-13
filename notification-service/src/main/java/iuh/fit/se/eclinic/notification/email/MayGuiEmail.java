package iuh.fit.se.eclinic.notification.email;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.notification.config.EmailProperties;
import iuh.fit.se.eclinic.notification.config.EmailProperties.CheDoEmail;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Đưa 1 email đã soạn tới máy chủ mail (chế độ SMTP), hoặc chỉ ghi ra log (chế độ CONSOLE, máy dev). Cùng 2 chế độ và
 * cùng biến môi trường với email tài khoản của identity-service.
 */
@Slf4j
@Component
public class MayGuiEmail {

    private final JavaMailSender mailSender;
    private final CheDoEmail cheDo;
    private final String nguoiGui;

    /**
     * Kiểm tra cấu hình ngay khi khởi động: chế độ SMTP mà thiếu địa chỉ gửi (hoặc thiếu mật khẩu khi máy chủ đòi đăng
     * nhập) thì không cho chạy, tránh chạy "bình thường" nhưng không email nào đi.
     */
    public MayGuiEmail(JavaMailSender mailSender, EmailProperties emailProperties,
            @Value("${spring.mail.username:}") String nguoiGui, @Value("${spring.mail.password:}") String matKhau) {
        this.mailSender = mailSender;
        this.cheDo = emailProperties.cheDo();
        this.nguoiGui = nguoiGui;
        if (cheDo == CheDoEmail.SMTP) {
            boolean thieuMatKhau = emailProperties.smtpXacThuc() && (matKhau == null || matKhau.isBlank());
            if (nguoiGui == null || nguoiGui.isBlank() || thieuMatKhau) {
                throw new IllegalStateException(
                        "Email ở chế độ smtp nhưng thiếu MAIL_USERNAME hoặc MAIL_APP_PASSWORD (App Password của Gmail)");
            }
        } else {
            log.warn("Email đang ở chế độ CONSOLE: KHÔNG gửi email thật, liên kết được ghi ra log (chỉ dùng khi dev)."
                    + " Đặt MAIL_MODE=smtp (hoặc NOTIFICATION_MAIL_MODE=smtp) để gửi.");
        }
    }

    /**
     * Trả về bình thường = máy chủ mail đã nhận (chế độ CONSOLE: đã ghi log).
     *
     * @throws MailException không gửi được; bên gọi quyết định thử lại
     */
    public void gui(String nguoiNhan, NgonNgu ngonNgu, MauEmail.Thu thu) {
        if (cheDo == CheDoEmail.CONSOLE) {
            log.warn("[DEV - không gửi email] Tới: {} | Ngôn ngữ: {} | Tiêu đề: {} | Liên kết: {}", nguoiNhan, ngonNgu,
                    thu.tieuDe(), thu.lienKet());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(nguoiGui, "eClinic");
            helper.setTo(nguoiNhan);
            helper.setSubject(thu.tieuDe());
            helper.setText(thu.html(), true);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException(e);
        }
    }
}
