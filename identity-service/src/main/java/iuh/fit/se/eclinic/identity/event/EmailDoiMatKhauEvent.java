package iuh.fit.se.eclinic.identity.event;

import iuh.fit.se.eclinic.common.enums.NgonNgu;

/**
 * Phát ra sau khi mật khẩu được đặt lại (qua liên kết email) hoặc được đổi khi đang đăng nhập, để báo cho chủ tài khoản
 * (nếu không phải họ thực hiện thì biết ngay).
 * <p>
 * {@code ngonNgu}: ngôn ngữ của tài khoản (tai_khoan.ngon_ngu), email được viết bằng ngôn ngữ này.
 */
public record EmailDoiMatKhauEvent(String email, String hoTen, NgonNgu ngonNgu) {
}
