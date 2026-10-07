package iuh.fit.se.eclinic.common.enums;

/**
 * Vòng đời lịch hẹn — quy tắc #9. Chỉ lịch CHO_XAC_NHAN / DA_XAC_NHAN mới được đổi/hủy.
 * <p>
 * Đặt lịch tạo CHO_XAC_NHAN. Bác sĩ của lịch hẹn xác nhận (DA_XAC_NHAN) hoặc từ chối kèm lý do (BI_TU_CHOI) trước
 * giờ khám, ở booking-service. Ghi kết quả khám (medical-service) chuyển CHO_XAC_NHAN / DA_XAC_NHAN sang
 * DA_HOAN_THANH. Bệnh nhân (đã đăng nhập, hoặc cầm link phiếu khám kèm SĐT liên hệ) hủy (DA_HUY) hoặc đổi lịch
 * (DA_HUY_DO_DOI_LICH + 1 lịch mới CHO_XAC_NHAN) trước hạn hủy / đổi, ở booking-service.
 */
public enum TrangThaiLichHen {
    CHO_XAC_NHAN,
    DA_XAC_NHAN,
    BI_TU_CHOI,
    DA_HOAN_THANH,
    DA_HUY,
    /** Lịch cũ khi đổi lịch (đổi lịch = hủy lịch cũ + tạo lịch mới, quy tắc #10). */
    DA_HUY_DO_DOI_LICH;

    /**
     * Lịch hẹn ở trạng thái này đang "chiếm" khung giờ. Phải khớp với biểu thức của cột
     * {@code lich_hen.id_khung_gio_hieu_luc} trong V1__init_schema.sql.
     */
    public boolean chiemKhungGio() {
        return this == CHO_XAC_NHAN || this == DA_XAC_NHAN || this == DA_HOAN_THANH;
    }
}
