package iuh.fit.se.eclinic.identity.event;

import iuh.fit.se.eclinic.common.enums.NgonNgu;

/**
 * Phát ra khi quản trị viên tạo tài khoản cho 1 bác sĩ mới, để báo cho bác sĩ email đăng nhập. Email không chứa mật khẩu
 * mặc định: phòng khám tự báo cho bác sĩ.
 * <p>
 * {@code ngonNgu}: ngôn ngữ của tài khoản (tai_khoan.ngon_ngu), email được viết bằng ngôn ngữ này.
 */
public record EmailChaoBacSiEvent(String email, String hoTen, NgonNgu ngonNgu) {
}
