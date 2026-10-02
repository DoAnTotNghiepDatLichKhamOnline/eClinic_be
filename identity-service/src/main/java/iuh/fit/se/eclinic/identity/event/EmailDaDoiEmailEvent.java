package iuh.fit.se.eclinic.identity.event;

/**
 * Phát ra sau khi email đăng nhập đã được đổi (liên kết xác nhận đã dùng), để báo cho địa chỉ CŨ.
 */
public record EmailDaDoiEmailEvent(String emailCu, String hoTen, String emailMoi) {
}
