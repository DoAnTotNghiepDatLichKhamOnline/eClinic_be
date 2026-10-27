package iuh.fit.se.eclinic.identity.event;

import iuh.fit.se.eclinic.common.enums.NgonNgu;

/** Phát ra khi có yêu cầu đổi email, để báo cho địa chỉ ĐANG DÙNG. */
public record EmailYeuCauDoiEmailEvent(String emailCu, String hoTen, String emailMoi, NgonNgu ngonNgu) {
}
