package iuh.fit.se.eclinic.common.enums;

/** Trạng thái của 1 lịch hẹn trong hàng đợi khám trong ngày của bác sĩ. */
public enum TrangThaiHangDoi {
    /** Chưa được gọi (mặc định khi đặt lịch). */
    CHO_KHAM,
    /** Bác sĩ đã gọi vào khám; mỗi bác sĩ chỉ 1 lượt như vậy chưa khám xong trong ngày. */
    DANG_KHAM,
    /** Đã gọi mà bệnh nhân không có mặt; bác sĩ gọi lại được trong ngày. */
    VANG_MAT,
    /** Đã ghi nhận kết quả khám: chỉ để hiển thị, không bao giờ lưu vào cột. */
    DA_KHAM
}
