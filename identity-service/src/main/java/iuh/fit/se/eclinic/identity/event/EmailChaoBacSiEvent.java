package iuh.fit.se.eclinic.identity.event;

/**
 * Phát ra khi quản trị viên tạo tài khoản cho 1 bác sĩ mới, để báo cho bác sĩ email đăng nhập. Email không chứa mật khẩu
 * mặc định: phòng khám tự báo cho bác sĩ.
 */
public record EmailChaoBacSiEvent(String email, String hoTen) {
}
