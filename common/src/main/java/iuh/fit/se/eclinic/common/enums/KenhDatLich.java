package iuh.fit.se.eclinic.common.enums;

/**
 * Lịch hẹn được đặt qua kênh nào (V35). Đổi lịch giữ kênh của lịch cũ.
 */
public enum KenhDatLich {
    /** Khách đặt không đăng nhập. */
    KHACH,
    /** Tài khoản bệnh nhân đặt khi đã đăng nhập (cho bản thân hoặc người thân). */
    TAI_KHOAN,
    /**
     * Quản trị viên đặt thay người gọi tổng đài / người đến quầy: đặt như khách (không gắn tài khoản), người đặt ghi ở
     * nhật ký lịch hẹn.
     */
    HOTLINE
}
