package iuh.fit.se.eclinic.identity.event;

/**
 * Phát ra khi cần gửi liên kết đặt lại mật khẩu. EmailService gửi sau khi transaction commit.
 *
 * @param token token gốc, chỉ dùng để dựng liên kết; không ghi ra log
 */
public record EmailDatLaiMatKhauEvent(String email, String hoTen, String token) {

    /** Không in token (tránh lộ khi event bị log). */
    @Override
    public String toString() {
        return "EmailDatLaiMatKhauEvent[email=" + email + "]";
    }

}
