package iuh.fit.se.eclinic.identity.event;

/**
 * Phát ra khi cần gửi liên kết xác nhận đổi email tới địa chỉ MỚI. Không mang họ tên hay email cũ của chủ tài khoản:
 * địa chỉ mới có thể bị gõ nhầm và thuộc về người lạ.
 *
 * @param token token gốc, chỉ dùng để dựng liên kết; không ghi ra log
 */
public record EmailXacNhanDoiEmailEvent(String emailMoi, String token) {

    /** Không in token (tránh lộ khi event bị log). */
    @Override
    public String toString() {
        return "EmailXacNhanDoiEmailEvent[emailMoi=" + emailMoi + "]";
    }

}
