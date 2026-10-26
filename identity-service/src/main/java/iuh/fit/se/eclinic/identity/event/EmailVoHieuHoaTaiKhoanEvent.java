package iuh.fit.se.eclinic.identity.event;

import iuh.fit.se.eclinic.common.enums.NgonNgu;

/**
 * Phát ra khi quản trị viên vô hiệu hoá 1 tài khoản, để báo cho chủ tài khoản kèm lý do (UC-USER-01).
 * <p>
 * {@code ngonNgu}: ngôn ngữ của tài khoản (tai_khoan.ngon_ngu), email được viết bằng ngôn ngữ này.
 */
public record EmailVoHieuHoaTaiKhoanEvent(String email, String hoTen, String lyDo, NgonNgu ngonNgu) {
}
