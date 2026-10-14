package iuh.fit.se.eclinic.common.enums;

/**
 * Phần của hồ sơ bệnh án mà 1 lần sửa đã đổi (lịch sử sửa hồ sơ bệnh án). Nhãn hiển thị do frontend đặt.
 */
public enum TruongBenhAn {
    CHAN_DOAN,
    /** lời dặn của bác sĩ */
    GHI_CHU,
    NGAY_TAI_KHAM,
    /** thêm / bỏ thuốc hoặc đổi liều dùng, số lần, số ngày, ghi chú của 1 dòng */
    DON_THUOC
}
