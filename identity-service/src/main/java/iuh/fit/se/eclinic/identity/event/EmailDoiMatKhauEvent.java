package iuh.fit.se.eclinic.identity.event;

import iuh.fit.se.eclinic.common.enums.NgonNgu;

/** Phát ra sau khi mật khẩu được đặt lại hoặc đổi, để báo cho chủ tài khoản. */
public record EmailDoiMatKhauEvent(String email, String hoTen, NgonNgu ngonNgu) {
}
