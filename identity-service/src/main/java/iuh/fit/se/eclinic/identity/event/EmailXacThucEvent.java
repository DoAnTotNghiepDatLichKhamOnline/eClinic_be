package iuh.fit.se.eclinic.identity.event;

/**
 * Phát ra khi cần gửi liên kết kích hoạt tài khoản. EmailService gửi sau khi transaction commit.
 *
 * @param token token gốc, chỉ dùng để dựng liên kết; không ghi ra log
 */
public record EmailXacThucEvent(String email, String hoTen, String token) {

    /** Không in token (tránh lộ khi event bị log). */
    @Override
    public String toString() {
        return "EmailXacThucEvent[email=" + email + "]";
    }

}
