package iuh.fit.se.eclinic.identity.event;

import iuh.fit.se.eclinic.common.enums.NgonNgu;

/** Phát ra khi quản trị viên kích hoạt lại 1 tài khoản đã bị vô hiệu hoá, để báo cho chủ tài khoản. */
public record EmailKichHoatLaiTaiKhoanEvent(String email, String hoTen, NgonNgu ngonNgu) {
}
