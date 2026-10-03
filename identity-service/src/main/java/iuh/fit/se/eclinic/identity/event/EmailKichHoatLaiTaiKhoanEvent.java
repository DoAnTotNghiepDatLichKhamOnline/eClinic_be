package iuh.fit.se.eclinic.identity.event;

/**
 * Phát ra khi quản trị viên kích hoạt lại 1 tài khoản đã bị vô hiệu hoá, để báo cho chủ tài khoản (UC-USER-01).
 */
public record EmailKichHoatLaiTaiKhoanEvent(String email, String hoTen) {
}
