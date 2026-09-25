package iuh.fit.fe.be_websatlichkham.common.enums;

/**
 * Trạng thái khung giờ khám (Q3: mỗi khung giờ nhận 1 bệnh nhân).
 */
public enum TrangThaiKhungGio {
    CON_TRONG,
    DA_DAT,
    /** Khung giờ thuộc ca đã bị hủy/thay đổi -> đặt lịch trả lỗi SLOT_UNAVAILABLE (BOOK-04). */
    DA_HUY
}
