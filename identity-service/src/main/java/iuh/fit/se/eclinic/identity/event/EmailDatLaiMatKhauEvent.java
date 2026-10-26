package iuh.fit.se.eclinic.identity.event;

import iuh.fit.se.eclinic.common.enums.NgonNgu;

/** Phát ra khi cần gửi liên kết đặt lại mật khẩu. */
public record EmailDatLaiMatKhauEvent(String email, String hoTen, String token, NgonNgu ngonNgu) {

    /** Không in token (tránh lộ khi event bị log). */
    @Override
    public String toString() {
        return "EmailDatLaiMatKhauEvent[email=" + email + "]";
    }

}
