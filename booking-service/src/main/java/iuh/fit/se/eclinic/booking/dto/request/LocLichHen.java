package iuh.fit.se.eclinic.booking.dto.request;

/**
 * Bộ lọc "lịch hẹn của tôi" (UC-APPT-05: Tất cả / Sắp tới / Lịch sử).
 */
public enum LocLichHen {
    TAT_CA,
    /** Lịch DA_XAC_NHAN mà lượt khám chưa kết thúc. */
    SAP_TOI,
    /** Mọi lịch còn lại: đã khám, đã hủy, bác sĩ đã hủy, không đến, hoặc đã qua giờ khám. */
    LICH_SU
}
