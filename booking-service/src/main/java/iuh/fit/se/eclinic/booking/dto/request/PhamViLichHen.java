package iuh.fit.se.eclinic.booking.dto.request;

/**
 * Bộ lọc "lịch hẹn của tôi" theo người khám.
 */
public enum PhamViLichHen {
    TAT_CA,
    /** Người khám là chủ tài khoản (hồ sơ bệnh nhân đã liên kết của tài khoản), bất kể ai đặt. */
    BAN_THAN,
    /** Người khám là người khác: lịch tài khoản đặt cho người thân, hoặc lịch mà chủ tài khoản là người giám hộ. */
    NGUOI_KHAC
}
