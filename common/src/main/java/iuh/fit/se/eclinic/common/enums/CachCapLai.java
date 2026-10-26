package iuh.fit.se.eclinic.common.enums;

/**
 * Nhân viên cấp lại liên kết phiếu khám / sổ khám bệnh cho người đã mất link (PAT-03) bằng cách nào.
 */
public enum CachCapLai {
    /** Gửi email tới địa chỉ của lịch hẹn; phản hồi chỉ có địa chỉ đã che. */
    EMAIL,
    /**
     * Trả mã và link trong phản hồi để nhân viên mở mã QR trên màn hình cho người đang đứng ở quầy: cách duy nhất với
     * lịch hẹn không có email (đặt qua tổng đài không nhập email).
     */
    MAN_HINH
}
