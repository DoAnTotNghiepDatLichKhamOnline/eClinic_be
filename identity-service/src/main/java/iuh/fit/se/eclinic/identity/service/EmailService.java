package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.identity.event.EmailDatLaiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailDoiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailXacThucEvent;

/**
 * Gửi email của identity-service. Chạy nền sau khi transaction commit; lỗi gửi chỉ ghi log, không trả về cho người dùng
 * (người dùng bấm "gửi lại" nếu không nhận được).
 */
public interface EmailService {

    void guiEmailXacThuc(EmailXacThucEvent event);

    void guiEmailDatLaiMatKhau(EmailDatLaiMatKhauEvent event);

    /** Báo mật khẩu vừa được đổi. Không chứa liên kết hay token. */
    void guiThongBaoDoiMatKhau(EmailDoiMatKhauEvent event);

}
