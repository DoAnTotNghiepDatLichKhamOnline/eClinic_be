package iuh.fit.se.eclinic.identity.event;

import iuh.fit.se.eclinic.common.enums.NgonNgu;

/**
 * Phát ra khi cần gửi liên kết kích hoạt tài khoản. EmailService gửi sau khi transaction commit.
 *
 * @param token token gốc, chỉ dùng để dựng liên kết; không ghi ra log
 * @param ngonNgu ngôn ngữ của tài khoản (tai_khoan.ngon_ngu): email được viết bằng ngôn ngữ này
 */
public record EmailXacThucEvent(String email, String hoTen, String token, NgonNgu ngonNgu) {

    /** Không in token (tránh lộ khi event bị log). */
    @Override
    public String toString() {
        return "EmailXacThucEvent[email=" + email + "]";
    }

}
