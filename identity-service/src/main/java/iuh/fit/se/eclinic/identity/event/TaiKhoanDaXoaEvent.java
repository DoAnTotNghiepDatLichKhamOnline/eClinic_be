package iuh.fit.se.eclinic.identity.event;

/**
 * Phát ra khi quản trị viên xoá 1 tài khoản, để dọn ảnh đại diện của nó trong kho ảnh sau khi transaction commit.
 *
 * @param anhDaiDien URL ảnh đại diện lúc bị xoá; null nếu tài khoản không có ảnh
 */
public record TaiKhoanDaXoaEvent(Long idTaiKhoan, String anhDaiDien) {
}
