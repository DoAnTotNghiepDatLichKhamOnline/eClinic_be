package iuh.fit.se.eclinic.identity.service.impl;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

import iuh.fit.se.eclinic.identity.config.EmailProperties;
import iuh.fit.se.eclinic.identity.config.EmailProperties.CheDoEmail;
import iuh.fit.se.eclinic.identity.config.LienKetProperties;
import iuh.fit.se.eclinic.identity.event.EmailDatLaiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailDoiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailXacThucEvent;
import iuh.fit.se.eclinic.identity.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private static final String TIEU_DE_XAC_THUC = "[eClinic] Kích hoạt tài khoản";
    private static final String TIEU_DE_DAT_LAI = "[eClinic] Đặt lại mật khẩu";
    private static final String TIEU_DE_DOI_MAT_KHAU = "[eClinic] Mật khẩu đã được thay đổi";

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;
    private final LienKetProperties lienKetProperties;
    private final String nguoiGui;

    /**
     * Kiểm tra cấu hình ngay khi khởi động: chế độ SMTP mà thiếu tài khoản Gmail thì không cho chạy
     * (tránh chạy "bình thường" nhưng không email nào đi).
     */
    public EmailServiceImpl(JavaMailSender mailSender, EmailProperties emailProperties, LienKetProperties lienKetProperties,
            @Value("${spring.mail.username:}") String nguoiGui, @Value("${spring.mail.password:}") String matKhau) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
        this.lienKetProperties = lienKetProperties;
        this.nguoiGui = nguoiGui;
        if (emailProperties.cheDo() == CheDoEmail.SMTP) {
            if (nguoiGui == null || nguoiGui.isBlank() || matKhau == null || matKhau.isBlank()) {
                throw new IllegalStateException(
                        "MAIL_MODE=smtp nhưng thiếu MAIL_USERNAME hoặc MAIL_APP_PASSWORD (App Password của Gmail)");
            }
        } else {
            log.warn("Email đang ở chế độ CONSOLE: KHÔNG gửi email thật, liên kết được ghi ra log (chỉ dùng khi dev)."
                    + " Đặt MAIL_MODE=smtp để gửi qua Gmail.");
        }
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiEmailXacThuc(EmailXacThucEvent event) {
        String lienKet = emailProperties.frontendUrl() + emailProperties.duongDanXacThuc() + "?token=" + event.token();
        gui(event.email(), TIEU_DE_XAC_THUC, noiDungXacThuc(event.hoTen(), lienKet, lienKetProperties.thoiHanXacThucEmail().toHours()), lienKet);
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiEmailDatLaiMatKhau(EmailDatLaiMatKhauEvent event) {
        String lienKet = emailProperties.frontendUrl() + emailProperties.duongDanDatLai() + "?token=" + event.token();
        gui(event.email(), TIEU_DE_DAT_LAI,
                noiDungDatLai(event.hoTen(), lienKet, lienKetProperties.thoiHanDatLaiMatKhau().toMinutes()), lienKet);
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiThongBaoDoiMatKhau(EmailDoiMatKhauEvent event) {
        gui(event.email(), TIEU_DE_DOI_MAT_KHAU, noiDungDoiMatKhau(event.hoTen()), null);
    }

    /** @param lienKet chỉ để ghi log ở chế độ console; null nếu email không có liên kết */
    private void gui(String nguoiNhan, String tieuDe, String html, String lienKet) {
        if (emailProperties.cheDo() == CheDoEmail.CONSOLE) {
            log.warn("[DEV - không gửi email] Tới: {} | Tiêu đề: {} | Liên kết: {}", nguoiNhan, tieuDe,
                    lienKet == null ? "-" : lienKet);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(nguoiGui, "eClinic");
            helper.setTo(nguoiNhan);
            helper.setSubject(tieuDe);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            // Ghi cả stack trace để dò lỗi SMTP; exception không chứa liên kết/token
            log.error("Gửi email '{}' thất bại", tieuDe, e);
        }
    }

    private static String noiDungXacThuc(String hoTen, String lienKet, long soGio) {
        String url = escape(lienKet);
        return """
                <div style="font-family:Arial,sans-serif;font-size:15px;color:#222;max-width:560px">
                  <p>Xin chào %s,</p>
                  <p>Cảm ơn bạn đã đăng ký tài khoản eClinic. Bấm nút bên dưới để kích hoạt tài khoản:</p>
                  <p><a href="%s" style="display:inline-block;padding:10px 20px;background:#1a73e8;color:#fff;\
                text-decoration:none;border-radius:4px">Kích hoạt tài khoản</a></p>
                  <p>Nếu nút không hoạt động, hãy mở liên kết sau:<br><a href="%s">%s</a></p>
                  <p>Liên kết có hiệu lực %d giờ. Nếu bạn không đăng ký, hãy bỏ qua email này.</p>
                </div>
                """.formatted(escape(hoTen), url, url, url, soGio);
    }

    private static String noiDungDatLai(String hoTen, String lienKet, long soPhut) {
        String url = escape(lienKet);
        return """
                <div style="font-family:Arial,sans-serif;font-size:15px;color:#222;max-width:560px">
                  <p>Xin chào %s,</p>
                  <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản eClinic của bạn. Bấm nút bên dưới để \
                đặt mật khẩu mới:</p>
                  <p><a href="%s" style="display:inline-block;padding:10px 20px;background:#1a73e8;color:#fff;\
                text-decoration:none;border-radius:4px">Đặt lại mật khẩu</a></p>
                  <p>Nếu nút không hoạt động, hãy mở liên kết sau:<br><a href="%s">%s</a></p>
                  <p>Liên kết có hiệu lực %d phút và chỉ dùng được 1 lần. Nếu bạn không yêu cầu, hãy bỏ qua email này, \
                mật khẩu của bạn không thay đổi.</p>
                </div>
                """.formatted(escape(hoTen), url, url, url, soPhut);
    }

    private static String noiDungDoiMatKhau(String hoTen) {
        return """
                <div style="font-family:Arial,sans-serif;font-size:15px;color:#222;max-width:560px">
                  <p>Xin chào %s,</p>
                  <p>Mật khẩu tài khoản eClinic của bạn vừa được đặt lại. Tất cả thiết bị đã bị đăng xuất.</p>
                  <p>Nếu không phải bạn thực hiện, hãy dùng chức năng <b>Quên mật khẩu</b> ngay để lấy lại tài khoản \
                và liên hệ phòng khám.</p>
                </div>
                """.formatted(escape(hoTen));
    }

    private static String escape(String giaTri) {
        return HtmlUtils.htmlEscape(giaTri == null ? "" : giaTri, StandardCharsets.UTF_8.name());
    }

}
