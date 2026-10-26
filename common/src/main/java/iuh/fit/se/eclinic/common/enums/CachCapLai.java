package iuh.fit.se.eclinic.common.enums;

/** Cách nhân viên cấp lại liên kết phiếu khám / sổ khám bệnh cho người đã mất link. */
public enum CachCapLai {
    /** Gửi email tới địa chỉ của lịch hẹn; phản hồi chỉ có địa chỉ đã che. */
    EMAIL,
    /** Trả mã và link để nhân viên mở mã QR tại quầy; cách duy nhất khi lịch hẹn không có email. */
    MAN_HINH
}
