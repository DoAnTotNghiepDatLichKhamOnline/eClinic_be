package iuh.fit.se.eclinic.identity.event;

import iuh.fit.se.eclinic.common.enums.NgonNgu;

/** Phát ra khi quản trị viên tạo tài khoản cho 1 bác sĩ mới, để báo cho bác sĩ email đăng nhập. */
public record EmailChaoBacSiEvent(String email, String hoTen, NgonNgu ngonNgu) {
}
