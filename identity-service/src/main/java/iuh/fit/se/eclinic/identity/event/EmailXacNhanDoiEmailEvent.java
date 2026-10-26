package iuh.fit.se.eclinic.identity.event;

import iuh.fit.se.eclinic.common.enums.NgonNgu;

/** Phát ra khi cần gửi liên kết xác nhận đổi email tới địa chỉ MỚI. */
public record EmailXacNhanDoiEmailEvent(String emailMoi, String token, NgonNgu ngonNgu) {

    /** Không in token (tránh lộ khi event bị log). */
    @Override
    public String toString() {
        return "EmailXacNhanDoiEmailEvent[emailMoi=" + emailMoi + "]";
    }

}
