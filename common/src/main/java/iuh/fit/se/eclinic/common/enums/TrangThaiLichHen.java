package iuh.fit.se.eclinic.common.enums;

/**
 * Vòng đời lịch hẹn — quy tắc #9. Chỉ lịch DA_XAC_NHAN mới được đổi/hủy.
 * <p>
 * Đặt lịch tạo thẳng DA_XAC_NHAN (không cần bác sĩ duyệt; số thứ tự cấp ngay khi đặt). Trước giờ khám, bác sĩ của lịch
 * hẹn hủy được kèm lý do (BI_TU_CHOI), ở booking-service. Ghi kết quả khám (medical-service) chuyển DA_XAC_NHAN sang
 * DA_HOAN_THANH. Bệnh nhân (đã đăng nhập, hoặc cầm link phiếu khám kèm SĐT liên hệ) hủy (DA_HUY) hoặc đổi lịch
 * (DA_HUY_DO_DOI_LICH + 1 lịch mới DA_XAC_NHAN) trước hạn hủy / đổi, ở booking-service. Sang ngày hôm sau, lịch
 * DA_XAC_NHAN chưa khám mà bệnh nhân chưa được gọi vào khám thành KHONG_DEN (job đóng ngày khám, booking-service).
 */
public enum TrangThaiLichHen {
    DA_XAC_NHAN,
    /** Bác sĩ hủy lịch đã xác nhận, kèm lý do bệnh nhân đọc được (tên giữ từ khi còn bước bác sĩ duyệt / từ chối). */
    BI_TU_CHOI,
    DA_HOAN_THANH,
    DA_HUY,
    /** Lịch cũ khi đổi lịch (đổi lịch = hủy lịch cũ + tạo lịch mới, quy tắc #10). */
    DA_HUY_DO_DOI_LICH,
    /**
     * Bệnh nhân không đến: chỉ job đóng ngày khám gán, cho lịch DA_XAC_NHAN của các ngày đã qua mà chưa ghi nhận kết quả
     * khám, bệnh nhân chưa được gọi vào khám (khác DANG_KHAM) và ca khám không bị hủy (canDoiLich = false). Trạng thái
     * cuối: không khám, không hủy / đổi được nữa.
     */
    KHONG_DEN;

    /**
     * Lịch hẹn ở trạng thái này đang "chiếm" khung giờ. Phải khớp với biểu thức của cột
     * {@code lich_hen.id_khung_gio_hieu_luc} trong V1__init_schema.sql (giá trị 'CHO_XAC_NHAN' còn trong biểu thức đó
     * đã bỏ ở V29, không dòng nào còn mang). KHONG_DEN không chiếm: lượt khám đã qua, không ai đặt lại được, nên không
     * phải sửa cột sinh ra đó.
     */
    public boolean chiemKhungGio() {
        return this == DA_XAC_NHAN || this == DA_HOAN_THANH;
    }
}
