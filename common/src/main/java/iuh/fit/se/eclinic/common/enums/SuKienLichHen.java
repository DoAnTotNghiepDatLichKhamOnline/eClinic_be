package iuh.fit.se.eclinic.common.enums;

/**
 * 1 thao tác trên lịch hẹn, ghi ở lich_su_lich_hen (V35). Không có "đã khám" / "không đến": trạng thái lịch hẹn đã cho
 * biết.
 */
public enum SuKienLichHen {
    /** Lịch hẹn được đặt. */
    DAT_LICH,
    /** Lịch hẹn được đổi sang lịch mới (dòng nằm ở lịch cũ, trỏ tới lịch mới). */
    DOI_LICH,
    /** Lịch hẹn bị hủy; ai hủy xem trạng thái (DA_HUY, BI_TU_CHOI, QUAN_TRI_HUY) và người thực hiện. */
    HUY,
    /** Ca khám bị hủy: lịch hẹn giữ nguyên, được đánh dấu cần đổi lịch. */
    CA_BI_HUY,
    /** Ca khám được nhường cho bác sĩ khác: lịch hẹn mang bác sĩ mới. */
    DOI_BAC_SI,
    /** Ca khám bị sửa phòng hoặc giờ. */
    CA_THAY_DOI,
    /**
     * Liên kết phiếu khám (và sổ khám bệnh nếu có) được cấp lại cho người đã mất link (PAT-03): cùng mã cũ, gửi email
     * hoặc hiện trên màn hình của nhân viên; lý do ghi kênh gửi.
     */
    CAP_LAI_LIEN_KET
}
