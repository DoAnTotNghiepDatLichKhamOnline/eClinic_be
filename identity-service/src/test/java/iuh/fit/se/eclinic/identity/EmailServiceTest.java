package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import iuh.fit.se.eclinic.identity.config.EmailProperties;
import iuh.fit.se.eclinic.identity.config.EmailProperties.CheDoEmail;
import iuh.fit.se.eclinic.identity.config.LienKetProperties;
import iuh.fit.se.eclinic.identity.event.EmailDatLaiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailDoiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailXacThucEvent;
import iuh.fit.se.eclinic.identity.service.impl.EmailServiceImpl;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * EmailService không cần Spring: JavaMailSender là mock, không bao giờ gọi tới Gmail.
 */
class EmailServiceTest {

    private static final LienKetProperties LIEN_KET = new LienKetProperties(Duration.ofHours(24), Duration.ofSeconds(60),
            Duration.ofMinutes(15));
    private static final EmailXacThucEvent EVENT =
            new EmailXacThucEvent("bn@example.com", "<b>An</b>", "token-abc_123");

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    void cheDoSmtpGuiDungNguoiNhanVaLienKet() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        emailService(CheDoEmail.SMTP).guiEmailXacThuc(EVENT);

        ArgumentCaptor<MimeMessage> daGui = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(daGui.capture());
        MimeMessage message = daGui.getValue();
        message.saveChanges();
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("bn@example.com");
        assertThat(message.getFrom()[0].toString()).contains("eclinic.test@gmail.com");
        String noiDung = (String) message.getContent();
        assertThat(noiDung).contains("http://localhost:5173/verify-email?token=token-abc_123")
                .contains("&lt;b&gt;An&lt;/b&gt;")
                .doesNotContain("<b>An</b>")
                .contains("24 giờ");
    }

    @Test
    void emailDatLaiMatKhauCoLienKetVaThoiHanPhut() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        emailService(CheDoEmail.SMTP).guiEmailDatLaiMatKhau(
                new EmailDatLaiMatKhauEvent("bn@example.com", "<b>An</b>", "token-dat-lai"));

        String noiDung = noiDungDaGui();
        assertThat(noiDung).contains("http://localhost:5173/reset-password?token=token-dat-lai")
                .contains("15 phút")
                .contains("&lt;b&gt;An&lt;/b&gt;")
                .doesNotContain("<b>An</b>");
    }

    @Test
    void thongBaoDoiMatKhauKhongCoLienKet() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        emailService(CheDoEmail.SMTP).guiThongBaoDoiMatKhau(new EmailDoiMatKhauEvent("bn@example.com", "<b>An</b>"));

        String noiDung = noiDungDaGui();
        assertThat(noiDung).contains("Quên mật khẩu")
                .contains("&lt;b&gt;An&lt;/b&gt;")
                .doesNotContain("token=")
                .doesNotContain("href");
    }

    @Test
    void cheDoConsoleKhongGoiMailSender() {
        EmailServiceImpl console = emailService(CheDoEmail.CONSOLE);
        console.guiEmailXacThuc(EVENT);
        console.guiEmailDatLaiMatKhau(new EmailDatLaiMatKhauEvent("bn@example.com", "An", "token"));
        console.guiThongBaoDoiMatKhau(new EmailDoiMatKhauEvent("bn@example.com", "An"));

        verifyNoInteractions(mailSender);
    }

    @Test
    void loiGuiMailChiGhiLogKhongNemRa() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> emailService(CheDoEmail.SMTP).guiEmailXacThuc(EVENT)).doesNotThrowAnyException();
    }

    @Test
    void cheDoSmtpThieuTaiKhoanThiKhongKhoiDong() {
        EmailProperties smtp = new EmailProperties(CheDoEmail.SMTP, "http://localhost:5173", "/verify-email",
                "/reset-password");

        assertThatThrownBy(() -> new EmailServiceImpl(mailSender, smtp, LIEN_KET, "", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_APP_PASSWORD");
        assertThatThrownBy(() -> new EmailServiceImpl(mailSender, smtp, LIEN_KET, "eclinic.test@gmail.com", " "))
                .isInstanceOf(IllegalStateException.class);
    }

    private String noiDungDaGui() throws Exception {
        ArgumentCaptor<MimeMessage> daGui = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(daGui.capture());
        MimeMessage message = daGui.getValue();
        message.saveChanges();
        return (String) message.getContent();
    }

    private EmailServiceImpl emailService(CheDoEmail cheDo) {
        EmailProperties properties = new EmailProperties(cheDo, "http://localhost:5173", "/verify-email",
                "/reset-password");
        return new EmailServiceImpl(mailSender, properties, LIEN_KET, "eclinic.test@gmail.com", "app-password");
    }

}
