package iuh.fit.se.eclinic.common.enums;

/**
 * Trạng thái khung giờ khám. Mỗi dòng khung giờ là 1 lượt khám, nhận 1 bệnh nhân.
 */
public enum TrangThaiKhungGio {
    CON_TRONG,
    DA_DAT,
    /** Khung giờ thuộc ca đã bị hủy/thay đổi -> đặt lịch trả lỗi SLOT_UNAVAILABLE (BOOK-04). */
    DA_HUY
}
