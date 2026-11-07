package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.javamail.JavaMailSender;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.identity.config.EmailProperties;
import iuh.fit.se.eclinic.identity.config.EmailProperties.CheDoEmail;
import iuh.fit.se.eclinic.identity.config.LienKetProperties;
import iuh.fit.se.eclinic.identity.event.EmailVoHieuHoaTaiKhoanEvent;
import iuh.fit.se.eclinic.identity.event.EmailXacThucEvent;
import iuh.fit.se.eclinic.identity.service.impl.EmailServiceImpl;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/** EmailService không cần Spring: JavaMailSender là mock, không bao giờ gọi tới Gmail. */
class EmailServiceTest {

    private static final LienKetProperties LIEN_KET = new LienKetProperties(Duration.ofHours(24), Duration.ofSeconds(60),
            Duration.ofMinutes(15), Duration.ofHours(1));
    private static final EmailXacThucEvent EVENT =
            new EmailXacThucEvent("bn@example.com", "<b>An</b>", "token-abc_123", NgonNgu.VI);

    /** Đọc đúng file câu chữ của email như khi chạy thật (application-common.yml). */
    private static final ResourceBundleMessageSource THONG_DIEP = thongDiep();

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    void thongBaoVoHieuHoaCoLyDoDaEscapeKhongCoLienKet() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        emailService(CheDoEmail.SMTP).guiThongBaoVoHieuHoa(
                new EmailVoHieuHoaTaiKhoanEvent("bn@example.com", "<b>An</b>", "Vi phạm <b>quy định</b> & \"nội quy\"", NgonNgu.VI));

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

    private MimeMessage thuDaGui() throws Exception {
        ArgumentCaptor<MimeMessage> daGui = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(daGui.capture());
        MimeMessage message = daGui.getValue();
        message.saveChanges();
        return message;
    }

    private static ResourceBundleMessageSource thongDiep() {
        ResourceBundleMessageSource nguon = new ResourceBundleMessageSource();
        nguon.setBasenames("thong-diep/chung", "thong-diep/identity");
        nguon.setDefaultEncoding("UTF-8");
        nguon.setFallbackToSystemLocale(false);
        return nguon;
    }

    private EmailServiceImpl emailService(CheDoEmail cheDo) {
        EmailProperties properties = new EmailProperties(cheDo, "http://localhost:5173", "/verify-email",
                "/reset-password", "/confirm-email-change");
        return new EmailServiceImpl(mailSender, properties, LIEN_KET, THONG_DIEP, "eclinic.test@gmail.com", "app-password");
    }

}
