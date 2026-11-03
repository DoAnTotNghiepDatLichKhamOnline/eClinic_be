package iuh.fit.se.eclinic.common.enums;

/** Vòng đời lịch hẹn. */
public enum TrangThaiLichHen {
    DA_XAC_NHAN {
        @Override
        public boolean choPhepChuyenSang(TrangThaiLichHen moi) {
            return moi != DA_XAC_NHAN;
        }
    },
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

    /**
     * Quy tắc chuyển trạng thái duy nhất: chỉ DA_XAC_NHAN chuyển đi được, mọi trạng thái khác là trạng thái cuối. Job đóng
     * ngày khám là lệnh UPDATE hàng loạt, không đi qua đây; điều kiện WHERE của lệnh đó giữ đúng quy tắc này.
     */
    public boolean choPhepChuyenSang(TrangThaiLichHen moi) {
        return false;
    }
}
