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

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private static final String TIEU_DE_XAC_THUC = "[eClinic] Kích hoạt tài khoản";
    private static final String TIEU_DE_DAT_LAI = "[eClinic] Đặt lại mật khẩu";
    private static final String TIEU_DE_DOI_MAT_KHAU = "[eClinic] Mật khẩu đã được thay đổi";
    private static final String TIEU_DE_XAC_NHAN_DOI_EMAIL = "[eClinic] Xác nhận đổi email đăng nhập";
    private static final String TIEU_DE_YEU_CAU_DOI_EMAIL = "[eClinic] Có yêu cầu đổi email đăng nhập";
    private static final String TIEU_DE_DA_DOI_EMAIL = "[eClinic] Email đăng nhập đã được thay đổi";
    private static final String TIEU_DE_VO_HIEU_HOA = "[eClinic] Tài khoản của bạn đã bị vô hiệu hoá";
    private static final String TIEU_DE_KICH_HOAT_LAI = "[eClinic] Tài khoản của bạn đã được kích hoạt lại";
    private static final String TIEU_DE_CHAO_BAC_SI = "[eClinic] Tài khoản bác sĩ của bạn đã được tạo";
    private static final String DUONG_DAN_DANG_NHAP = "/login";

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

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiEmailXacNhanDoiEmail(EmailXacNhanDoiEmailEvent event) {
        String lienKet = emailProperties.frontendUrl() + emailProperties.duongDanDoiEmail() + "?token=" + event.token();
        gui(event.emailMoi(), TIEU_DE_XAC_NHAN_DOI_EMAIL,
                noiDungXacNhanDoiEmail(lienKet, lienKetProperties.thoiHanDoiEmail().toMinutes()), lienKet);
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiThongBaoYeuCauDoiEmail(EmailYeuCauDoiEmailEvent event) {
        gui(event.emailCu(), TIEU_DE_YEU_CAU_DOI_EMAIL, noiDungYeuCauDoiEmail(event.hoTen(), event.emailMoi()), null);
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiThongBaoDaDoiEmail(EmailDaDoiEmailEvent event) {
        gui(event.emailCu(), TIEU_DE_DA_DOI_EMAIL, noiDungDaDoiEmail(event.hoTen(), event.emailMoi()), null);
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiThongBaoVoHieuHoa(EmailVoHieuHoaTaiKhoanEvent event) {
        gui(event.email(), TIEU_DE_VO_HIEU_HOA, noiDungVoHieuHoa(event.hoTen(), event.lyDo()), null);
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiThongBaoKichHoatLai(EmailKichHoatLaiTaiKhoanEvent event) {
        gui(event.email(), TIEU_DE_KICH_HOAT_LAI, noiDungKichHoatLai(event.hoTen()), null);
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guiEmailChaoBacSi(EmailChaoBacSiEvent event) {
        String lienKet = emailProperties.frontendUrl() + DUONG_DAN_DANG_NHAP;
        gui(event.email(), TIEU_DE_CHAO_BAC_SI, noiDungChaoBacSi(event.hoTen(), event.email(), lienKet), lienKet);
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
                  <p>Mật khẩu tài khoản eClinic của bạn vừa được thay đổi. Các thiết bị khác đang đăng nhập tài khoản \
                này đã bị đăng xuất.</p>
                  <p>Nếu không phải bạn thực hiện, hãy dùng chức năng <b>Quên mật khẩu</b> ngay để lấy lại tài khoản \
                và liên hệ phòng khám.</p>
                </div>
                """.formatted(escape(hoTen));
    }

    /** Gửi tới địa chỉ mới, có thể là người lạ (gõ nhầm): không nêu họ tên hay email đang dùng của chủ tài khoản. */
    private static String noiDungXacNhanDoiEmail(String lienKet, long soPhut) {
        String url = escape(lienKet);
        return """
                <div style="font-family:Arial,sans-serif;font-size:15px;color:#222;max-width:560px">
                  <p>Xin chào,</p>
                  <p>Một tài khoản eClinic vừa yêu cầu dùng địa chỉ email này làm email đăng nhập. Bấm nút bên dưới để \
                xác nhận:</p>
                  <p><a href="%s" style="display:inline-block;padding:10px 20px;background:#1a73e8;color:#fff;\
                text-decoration:none;border-radius:4px">Xác nhận đổi email</a></p>
                  <p>Nếu nút không hoạt động, hãy mở liên kết sau:<br><a href="%s">%s</a></p>
                  <p>Liên kết có hiệu lực %d phút và chỉ dùng được 1 lần. Sau khi xác nhận, mọi thiết bị của tài khoản \
                sẽ bị đăng xuất và phải đăng nhập lại bằng email này. Nếu bạn không yêu cầu, hãy bỏ qua email này.</p>
                </div>
                """.formatted(url, url, url, soPhut);
    }

    private static String noiDungYeuCauDoiEmail(String hoTen, String emailMoi) {
        return """
                <div style="font-family:Arial,sans-serif;font-size:15px;color:#222;max-width:560px">
                  <p>Xin chào %s,</p>
                  <p>Tài khoản eClinic của bạn vừa có yêu cầu đổi email đăng nhập sang <b>%s</b>. Email đăng nhập chưa \
                thay đổi cho tới khi yêu cầu được xác nhận từ địa chỉ mới.</p>
                  <p>Nếu không phải bạn thực hiện, hãy <b>đổi mật khẩu</b> ngay: việc đổi mật khẩu sẽ huỷ yêu cầu này.</p>
                </div>
                """.formatted(escape(hoTen), escape(emailMoi));
    }

    private static String noiDungDaDoiEmail(String hoTen, String emailMoi) {
        return """
                <div style="font-family:Arial,sans-serif;font-size:15px;color:#222;max-width:560px">
                  <p>Xin chào %s,</p>
                  <p>Email đăng nhập tài khoản eClinic của bạn đã được đổi sang <b>%s</b>. Mọi thiết bị đang đăng nhập \
                tài khoản này đã bị đăng xuất; từ nay hãy đăng nhập bằng email mới.</p>
                  <p>Địa chỉ email này không còn gắn với tài khoản. Nếu không phải bạn thực hiện, hãy liên hệ phòng \
                khám ngay để được hỗ trợ.</p>
                </div>
                """.formatted(escape(hoTen), escape(emailMoi));
    }

    /** Lý do do quản trị viên nhập tự do nên phải escape như mọi giá trị khác. */
    private static String noiDungVoHieuHoa(String hoTen, String lyDo) {
        return """
                <div style="font-family:Arial,sans-serif;font-size:15px;color:#222;max-width:560px">
                  <p>Xin chào %s,</p>
                  <p>Tài khoản eClinic của bạn đã bị quản trị viên vô hiệu hoá. Mọi thiết bị đang đăng nhập tài khoản \
                này đã bị đăng xuất và bạn không thể đăng nhập cho tới khi tài khoản được kích hoạt lại.</p>
                  <p>Lý do: <b>%s</b></p>
                  <p>Nếu bạn cho rằng đây là nhầm lẫn, hãy liên hệ phòng khám để được hỗ trợ.</p>
                </div>
                """.formatted(escape(hoTen), escape(lyDo));
    }

    private static String noiDungKichHoatLai(String hoTen) {
        return """
                <div style="font-family:Arial,sans-serif;font-size:15px;color:#222;max-width:560px">
                  <p>Xin chào %s,</p>
                  <p>Tài khoản eClinic của bạn đã được quản trị viên kích hoạt lại. Bạn có thể đăng nhập và sử dụng \
                hệ thống như bình thường.</p>
                </div>
                """.formatted(escape(hoTen));
    }

    private static String noiDungChaoBacSi(String hoTen, String email, String lienKet) {
        String url = escape(lienKet);
        return """
                <div style="font-family:Arial,sans-serif;font-size:15px;color:#222;max-width:560px">
                  <p>Xin chào %s,</p>
                  <p>Phòng khám đã tạo tài khoản bác sĩ eClinic cho bạn với email đăng nhập <b>%s</b>.</p>
                  <p>Hãy đăng nhập tại <a href="%s">%s</a> bằng mật khẩu ban đầu do phòng khám cung cấp. Ở lần đăng \
                nhập đầu tiên, hệ thống sẽ yêu cầu bạn đặt mật khẩu của riêng mình trước khi sử dụng.</p>
                  <p>Nếu bạn không làm việc tại phòng khám, hãy bỏ qua email này.</p>
                </div>
                """.formatted(escape(hoTen), escape(email), url, url);
    }

    private static String escape(String giaTri) {
        return HtmlUtils.htmlEscape(giaTri == null ? "" : giaTri, StandardCharsets.UTF_8.name());
    }

}
