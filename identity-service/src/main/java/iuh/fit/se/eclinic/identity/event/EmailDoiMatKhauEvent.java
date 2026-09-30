package iuh.fit.se.eclinic.identity.event;

/**
 * Phát ra sau khi mật khẩu được đặt lại, để báo cho chủ tài khoản (nếu không phải họ thực hiện thì biết ngay).
 */
public record EmailDoiMatKhauEvent(String email, String hoTen) {
}
