package iuh.fit.se.eclinic.identity.event;

import iuh.fit.se.eclinic.common.enums.NgonNgu;

/**
 * Phát ra khi cần gửi liên kết xác nhận đổi email tới địa chỉ MỚI. Không mang họ tên hay email cũ của chủ tài khoản:
 * địa chỉ mới có thể bị gõ nhầm và thuộc về người lạ.
 *
 * @param token token gốc, chỉ dùng để dựng liên kết; không ghi ra log
 * @param ngonNgu ngôn ngữ của tài khoản (tai_khoan.ngon_ngu): email được viết bằng ngôn ngữ này
 */
public record EmailXacNhanDoiEmailEvent(String emailMoi, String token, NgonNgu ngonNgu) {

    /** Không in token (tránh lộ khi event bị log). */
    @Override
    public String toString() {
        return "EmailXacNhanDoiEmailEvent[emailMoi=" + emailMoi + "]";
    }

}
