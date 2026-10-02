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
import iuh.fit.se.eclinic.identity.event.EmailDaDoiEmailEvent;
import iuh.fit.se.eclinic.identity.event.EmailDatLaiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailDoiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailKichHoatLaiTaiKhoanEvent;
import iuh.fit.se.eclinic.identity.event.EmailVoHieuHoaTaiKhoanEvent;
import iuh.fit.se.eclinic.identity.event.EmailXacNhanDoiEmailEvent;
import iuh.fit.se.eclinic.identity.event.EmailXacThucEvent;
import iuh.fit.se.eclinic.identity.event.EmailYeuCauDoiEmailEvent;
import iuh.fit.se.eclinic.identity.service.impl.EmailServiceImpl;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * EmailService không cần Spring: JavaMailSender là mock, không bao giờ gọi tới Gmail.
 */
class EmailServiceTest {

    private static final LienKetProperties LIEN_KET = new LienKetProperties(Duration.ofHours(24), Duration.ofSeconds(60),
            Duration.ofMinutes(15), Duration.ofHours(1));
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
    void emailXacNhanDoiEmailGuiToiDiaChiMoiCoLienKetKhongCoThongTinChuTaiKhoan() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        emailService(CheDoEmail.SMTP).guiEmailXacNhanDoiEmail(
                new EmailXacNhanDoiEmailEvent("moi@example.com", "token-doi-email"));

        MimeMessage message = thuDaGui();
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("moi@example.com");
        // Chào không kèm tên: địa chỉ mới có thể là người lạ
        assertThat((String) message.getContent())
                .contains("http://localhost:5173/confirm-email-change?token=token-doi-email")
                .contains("60 phút")
                .contains("Xin chào,");
    }

    @Test
    void thongBaoYeuCauDoiEmailGuiToiDiaChiCuKhongCoLienKet() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        emailService(CheDoEmail.SMTP).guiThongBaoYeuCauDoiEmail(
                new EmailYeuCauDoiEmailEvent("bn@example.com", "<b>An</b>", "moi<x>@example.com"));

        MimeMessage message = thuDaGui();
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("bn@example.com");
        assertThat((String) message.getContent())
                .contains("&lt;b&gt;An&lt;/b&gt;")
                .contains("moi&lt;x&gt;@example.com")
                .doesNotContain("<b>An</b>")
                .doesNotContain("moi<x>")
                .contains("đổi mật khẩu")
                .doesNotContain("token=")
                .doesNotContain("href");
    }

    @Test
    void thongBaoDaDoiEmailGuiToiDiaChiCuKhongCoLienKet() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        emailService(CheDoEmail.SMTP).guiThongBaoDaDoiEmail(
                new EmailDaDoiEmailEvent("bn@example.com", "<b>An</b>", "moi<x>@example.com"));

        MimeMessage message = thuDaGui();
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("bn@example.com");
        assertThat((String) message.getContent())
                .contains("&lt;b&gt;An&lt;/b&gt;")
                .contains("moi&lt;x&gt;@example.com")
                .doesNotContain("<b>An</b>")
                .doesNotContain("moi<x>")
                .doesNotContain("token=")
                .doesNotContain("href");
    }

    @Test
    void thongBaoVoHieuHoaCoLyDoDaEscapeKhongCoLienKet() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        emailService(CheDoEmail.SMTP).guiThongBaoVoHieuHoa(
                new EmailVoHieuHoaTaiKhoanEvent("bn@example.com", "<b>An</b>", "Vi phạm <b>quy định</b> & \"nội quy\""));

        MimeMessage message = thuDaGui();
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("bn@example.com");
        assertThat(message.getSubject()).isEqualTo("[eClinic] Tài khoản của bạn đã bị vô hiệu hoá");
        assertThat((String) message.getContent())
                .contains("&lt;b&gt;An&lt;/b&gt;")
                .contains("Vi phạm &lt;b&gt;quy định&lt;/b&gt; &amp; &quot;nội quy&quot;")
                .doesNotContain("<b>An</b>")
                .doesNotContain("<b>quy định</b>")
                .doesNotContain("token=")
                .doesNotContain("href");
    }

    @Test
    void thongBaoKichHoatLaiKhongCoLienKet() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        emailService(CheDoEmail.SMTP).guiThongBaoKichHoatLai(
                new EmailKichHoatLaiTaiKhoanEvent("bn@example.com", "<b>An</b>"));

        MimeMessage message = thuDaGui();
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("bn@example.com");
        assertThat(message.getSubject()).isEqualTo("[eClinic] Tài khoản của bạn đã được kích hoạt lại");
        assertThat((String) message.getContent())
                .contains("&lt;b&gt;An&lt;/b&gt;")
                .contains("kích hoạt lại")
                .doesNotContain("<b>An</b>")
                .doesNotContain("token=")
                .doesNotContain("href");
    }

    @Test
    void cheDoConsoleKhongGoiMailSender() {
        EmailServiceImpl console = emailService(CheDoEmail.CONSOLE);
        console.guiEmailXacThuc(EVENT);
        console.guiEmailDatLaiMatKhau(new EmailDatLaiMatKhauEvent("bn@example.com", "An", "token"));
        console.guiThongBaoDoiMatKhau(new EmailDoiMatKhauEvent("bn@example.com", "An"));
        console.guiEmailXacNhanDoiEmail(new EmailXacNhanDoiEmailEvent("moi@example.com", "token"));
        console.guiThongBaoYeuCauDoiEmail(new EmailYeuCauDoiEmailEvent("bn@example.com", "An", "moi@example.com"));
        console.guiThongBaoDaDoiEmail(new EmailDaDoiEmailEvent("bn@example.com", "An", "moi@example.com"));
        console.guiThongBaoVoHieuHoa(new EmailVoHieuHoaTaiKhoanEvent("bn@example.com", "An", "Vi phạm"));
        console.guiThongBaoKichHoatLai(new EmailKichHoatLaiTaiKhoanEvent("bn@example.com", "An"));

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
                "/reset-password", "/confirm-email-change");

        assertThatThrownBy(() -> new EmailServiceImpl(mailSender, smtp, LIEN_KET, "", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_APP_PASSWORD");
        assertThatThrownBy(() -> new EmailServiceImpl(mailSender, smtp, LIEN_KET, "eclinic.test@gmail.com", " "))
                .isInstanceOf(IllegalStateException.class);
    }

    private String noiDungDaGui() throws Exception {
        return (String) thuDaGui().getContent();
    }

    private MimeMessage thuDaGui() throws Exception {
        ArgumentCaptor<MimeMessage> daGui = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(daGui.capture());
        MimeMessage message = daGui.getValue();
        message.saveChanges();
        return message;
    }

    private EmailServiceImpl emailService(CheDoEmail cheDo) {
        EmailProperties properties = new EmailProperties(cheDo, "http://localhost:5173", "/verify-email",
                "/reset-password", "/confirm-email-change");
        return new EmailServiceImpl(mailSender, properties, LIEN_KET, "eclinic.test@gmail.com", "app-password");
    }

}
