package iuh.fit.se.eclinic.identity.service.impl;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.thongbao.KhungEmail;
import iuh.fit.se.eclinic.identity.config.EmailProperties;
import iuh.fit.se.eclinic.identity.config.EmailProperties.CheDoEmail;
import iuh.fit.se.eclinic.identity.config.LienKetProperties;
import iuh.fit.se.eclinic.identity.event.EmailChaoBacSiEvent;
import iuh.fit.se.eclinic.identity.event.EmailDaDoiEmailEvent;
import iuh.fit.se.eclinic.identity.event.EmailDatLaiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailDoiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailKichHoatLaiTaiKhoanEvent;
import iuh.fit.se.eclinic.identity.event.EmailVoHieuHoaTaiKhoanEvent;
import iuh.fit.se.eclinic.identity.event.EmailXacNhanDoiEmailEvent;
import iuh.fit.se.eclinic.identity.event.EmailXacThucEvent;
import iuh.fit.se.eclinic.identity.event.EmailYeuCauDoiEmailEvent;
import iuh.fit.se.eclinic.identity.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Email gửi theo ngôn ngữ của tài khoản (event mang {@code ngonNgu}; lúc gửi không còn request nào). Câu chữ nằm trong
 * thong-diep/identity*.properties, khoá {@code email.<ten>.<phan>}; khung HTML là {@link KhungEmail}. Giá trị chèn vào câu
 * (họ tên, email, lý do quản trị viên gõ) được escape trước; câu trong file thông điệp được phép chứa thẻ {@code <b>}.
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private static final String DUONG_DAN_DANG_NHAP = "/login";

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;
    private final LienKetProperties lienKetProperties;
    private final MessageSource messageSource;
    private final String nguoiGui;

    /**
     * Kiểm tra cấu hình ngay khi khởi động: chế độ SMTP mà thiếu tài khoản Gmail thì không cho chạy
     * (tránh chạy "bình thường" nhưng không email nào đi).
     */
    public EmailServiceImpl(JavaMailSender mailSender, EmailProperties emailProperties, LienKetProperties lienKetProperties,
            MessageSource messageSource, @Value("${spring.mail.username:}") String nguoiGui,
            @Value("${spring.mail.password:}") String matKhau) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
        this.lienKetProperties = lienKetProperties;
        this.messageSource = messageSource;
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
        Thu thu = new Thu(event.ngonNgu(), event.hoTen())
                .doan("email.xacThuc.moDau")
                .nut("email.xacThuc.nut", lienKet)
                .doan("email.xacThuc.ketThuc", lienKetProperties.thoiHanXacThucEmail().toHours());
        gui(event.email(), "email.xacThuc.tieuDe", thu, lienKet);
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiEmailDatLaiMatKhau(EmailDatLaiMatKhauEvent event) {
        String lienKet = emailProperties.frontendUrl() + emailProperties.duongDanDatLai() + "?token=" + event.token();
        Thu thu = new Thu(event.ngonNgu(), event.hoTen())
                .doan("email.datLai.moDau")
                .nut("email.datLai.nut", lienKet)
                .doan("email.datLai.ketThuc", lienKetProperties.thoiHanDatLaiMatKhau().toMinutes());
        gui(event.email(), "email.datLai.tieuDe", thu, lienKet);
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiThongBaoDoiMatKhau(EmailDoiMatKhauEvent event) {
        Thu thu = new Thu(event.ngonNgu(), event.hoTen())
                .doan("email.doiMatKhau.doan1")
                .doan("email.doiMatKhau.doan2");
        gui(event.email(), "email.doiMatKhau.tieuDe", thu, null);
    }

    /** Gửi tới địa chỉ mới, có thể là người lạ (gõ nhầm): không nêu họ tên hay email đang dùng của chủ tài khoản. */
    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiEmailXacNhanDoiEmail(EmailXacNhanDoiEmailEvent event) {
        String lienKet = emailProperties.frontendUrl() + emailProperties.duongDanDoiEmail() + "?token=" + event.token();
        Thu thu = new Thu(event.ngonNgu(), null)
                .doan("email.xacNhanDoiEmail.moDau")
                .nut("email.xacNhanDoiEmail.nut", lienKet)
                .doan("email.xacNhanDoiEmail.ketThuc", lienKetProperties.thoiHanDoiEmail().toMinutes());
        gui(event.emailMoi(), "email.xacNhanDoiEmail.tieuDe", thu, lienKet);
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiThongBaoYeuCauDoiEmail(EmailYeuCauDoiEmailEvent event) {
        Thu thu = new Thu(event.ngonNgu(), event.hoTen())
                .doan("email.yeuCauDoiEmail.doan1", event.emailMoi())
                .doan("email.yeuCauDoiEmail.doan2");
        gui(event.emailCu(), "email.yeuCauDoiEmail.tieuDe", thu, null);
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiThongBaoDaDoiEmail(EmailDaDoiEmailEvent event) {
        Thu thu = new Thu(event.ngonNgu(), event.hoTen())
                .doan("email.daDoiEmail.doan1", event.emailMoi())
                .doan("email.daDoiEmail.doan2");
        gui(event.emailCu(), "email.daDoiEmail.tieuDe", thu, null);
    }

    /** Lý do do quản trị viên nhập tự do: chép nguyên văn ở mọi ngôn ngữ, escape như mọi giá trị khác. */
    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiThongBaoVoHieuHoa(EmailVoHieuHoaTaiKhoanEvent event) {
        Thu thu = new Thu(event.ngonNgu(), event.hoTen())
                .doan("email.voHieuHoa.doan1")
                .doan("email.voHieuHoa.lyDo", event.lyDo())
                .doan("email.voHieuHoa.doan2");
        gui(event.email(), "email.voHieuHoa.tieuDe", thu, null);
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiThongBaoKichHoatLai(EmailKichHoatLaiTaiKhoanEvent event) {
        Thu thu = new Thu(event.ngonNgu(), event.hoTen()).doan("email.kichHoatLai.doan1");
        gui(event.email(), "email.kichHoatLai.tieuDe", thu, null);
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiEmailChaoBacSi(EmailChaoBacSiEvent event) {
        String lienKet = emailProperties.frontendUrl() + DUONG_DAN_DANG_NHAP;
        Thu thu = new Thu(event.ngonNgu(), event.hoTen())
                .doan("email.chaoBacSi.doan1", event.email())
                .doan("email.chaoBacSi.doan2", lienKet)
                .doan("email.chaoBacSi.doan3");
        gui(event.email(), "email.chaoBacSi.tieuDe", thu, lienKet);
    }

    /** @param lienKet chỉ để ghi log ở chế độ console; null nếu email không có liên kết */
    private void gui(String nguoiNhan, String khoaTieuDe, Thu thu, String lienKet) {
        String tieuDe = messageSource.getMessage(khoaTieuDe, null, khoaTieuDe, thu.ngonNgu.locale());
        // Dựng nội dung cả ở chế độ console để câu sai mẫu lộ ra ngay khi dev
        String html = thu.xong();
        if (emailProperties.cheDo() == CheDoEmail.CONSOLE) {
            log.warn("[DEV - không gửi email] Tới: {} | Ngôn ngữ: {} | Tiêu đề: {} | Liên kết: {}", nguoiNhan,
                    thu.ngonNgu, tieuDe, lienKet == null ? "-" : lienKet);
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

    /** 1 email theo 1 ngôn ngữ: câu lấy từ thong-diep/, khung HTML là {@link KhungEmail}. */
    private final class Thu {

        private final NgonNgu ngonNgu;
        private final KhungEmail khung;

        /** @param hoTen null = lời chào không nêu tên */
        Thu(NgonNgu ngonNguCuaThu, String hoTen) {
            this.ngonNgu = ngonNguCuaThu == null ? NgonNgu.VI : ngonNguCuaThu;
            this.khung = new KhungEmail(
                    (khoa, thamSo) -> messageSource.getMessage(khoa, thamSo, khoa, ngonNgu.locale()), hoTen);
        }

        Thu doan(String khoa, Object... giaTri) {
            khung.doan(khoa, giaTri);
            return this;
        }

        Thu nut(String khoaNhan, String lienKet) {
            khung.nut(khoaNhan, lienKet);
            return this;
        }

        String xong() {
            return khung.xong();
        }
    }
}
