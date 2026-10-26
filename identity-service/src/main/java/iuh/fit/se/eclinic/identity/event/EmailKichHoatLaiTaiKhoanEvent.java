package iuh.fit.se.eclinic.identity.event;

import iuh.fit.se.eclinic.common.enums.NgonNgu;

/**
 * Phát ra khi quản trị viên kích hoạt lại 1 tài khoản đã bị vô hiệu hoá, để báo cho chủ tài khoản (UC-USER-01).
 * <p>
 * {@code ngonNgu}: ngôn ngữ của tài khoản (tai_khoan.ngon_ngu), email được viết bằng ngôn ngữ này.
 */
public record EmailKichHoatLaiTaiKhoanEvent(String email, String hoTen, NgonNgu ngonNgu) {
}
