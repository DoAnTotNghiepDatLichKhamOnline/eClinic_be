package iuh.fit.se.eclinic.identity.event;

import iuh.fit.se.eclinic.common.enums.NgonNgu;

/**
 * Phát ra sau khi email đăng nhập đã được đổi (liên kết xác nhận đã dùng), để báo cho địa chỉ CŨ.
 * <p>
 * {@code ngonNgu}: ngôn ngữ của tài khoản (tai_khoan.ngon_ngu), email được viết bằng ngôn ngữ này.
 */
public record EmailDaDoiEmailEvent(String emailCu, String hoTen, String emailMoi, NgonNgu ngonNgu) {
}
