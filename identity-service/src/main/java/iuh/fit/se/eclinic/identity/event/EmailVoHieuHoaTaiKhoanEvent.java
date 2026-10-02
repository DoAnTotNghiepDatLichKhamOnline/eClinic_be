package iuh.fit.se.eclinic.identity.event;

/**
 * Phát ra khi quản trị viên vô hiệu hoá 1 tài khoản, để báo cho chủ tài khoản kèm lý do (UC-USER-01).
 */
public record EmailVoHieuHoaTaiKhoanEvent(String email, String hoTen, String lyDo) {
}
