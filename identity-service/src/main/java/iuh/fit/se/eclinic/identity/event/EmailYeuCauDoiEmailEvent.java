package iuh.fit.se.eclinic.identity.event;

import iuh.fit.se.eclinic.common.enums.NgonNgu;

/**
 * Phát ra khi có yêu cầu đổi email, để báo cho địa chỉ ĐANG DÙNG (email chưa đổi; nếu không phải chủ tài khoản yêu cầu
 * thì họ đổi mật khẩu để huỷ).
 * <p>
 * {@code ngonNgu}: ngôn ngữ của tài khoản (tai_khoan.ngon_ngu), email được viết bằng ngôn ngữ này.
 */
public record EmailYeuCauDoiEmailEvent(String emailCu, String hoTen, String emailMoi, NgonNgu ngonNgu) {
}
