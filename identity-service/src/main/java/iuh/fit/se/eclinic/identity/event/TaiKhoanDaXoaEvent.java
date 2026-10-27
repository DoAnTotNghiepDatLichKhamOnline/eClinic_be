package iuh.fit.se.eclinic.identity.event;

/** Phát ra khi quản trị viên xoá 1 tài khoản, để dọn ảnh đại diện sau khi transaction commit. */
public record TaiKhoanDaXoaEvent(Long idTaiKhoan, String anhDaiDien) {
}
