package iuh.fit.se.eclinic.common.enums;

/**
 * Phần của hồ sơ bệnh án mà 1 lần sửa đã đổi (lịch sử sửa hồ sơ bệnh án). Nhãn hiển thị do frontend đặt.
 */
public enum TruongBenhAn {
    LY_DO_KHAM,
    /** mạch, nhiệt độ, huyết áp, nhịp thở, cân nặng, chiều cao */
    SINH_HIEU,
    KHAM_LAM_SANG,
    /** chẩn đoán chính: chữ hoặc mã ICD-10 */
    CHAN_DOAN,
    CHAN_DOAN_KEM_THEO,
    /** lời dặn của bác sĩ */
    GHI_CHU,
    NGAY_TAI_KHAM,
    /** thêm / bỏ thuốc hoặc đổi liều dùng, số lần, số ngày, ghi chú của 1 dòng */
    DON_THUOC
}
