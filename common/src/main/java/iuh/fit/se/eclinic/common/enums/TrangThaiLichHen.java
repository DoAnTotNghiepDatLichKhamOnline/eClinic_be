package iuh.fit.se.eclinic.common.enums;

/** Vòng đời lịch hẹn. */
public enum TrangThaiLichHen {
    DA_XAC_NHAN,
    /** Bác sĩ hủy lịch đã xác nhận, kèm lý do bệnh nhân đọc được. */
    BI_TU_CHOI,
    DA_HOAN_THANH,
    DA_HUY,
    /** Lịch cũ khi đổi lịch (đổi lịch = hủy lịch cũ + tạo lịch mới). */
    DA_HUY_DO_DOI_LICH,
    /** Bệnh nhân không đến: job đóng ngày khám gán cho lịch đã qua mà chưa khám. */
    KHONG_DEN,
    /** Quản trị viên hủy lịch đã xác nhận, kèm lý do bệnh nhân đọc được. */
    QUAN_TRI_HUY;

    /** Lịch hẹn ở trạng thái này đang "chiếm" khung giờ. */
    public boolean chiemKhungGio() {
        return this == DA_XAC_NHAN || this == DA_HOAN_THANH;
    }
}
