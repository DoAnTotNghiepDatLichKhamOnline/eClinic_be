package iuh.fit.se.eclinic.identity.event;

/**
 * Phát ra khi có yêu cầu đổi email, để báo cho địa chỉ ĐANG DÙNG (email chưa đổi; nếu không phải chủ tài khoản yêu cầu
 * thì họ đổi mật khẩu để huỷ).
 */
public record EmailYeuCauDoiEmailEvent(String emailCu, String hoTen, String emailMoi) {
}
