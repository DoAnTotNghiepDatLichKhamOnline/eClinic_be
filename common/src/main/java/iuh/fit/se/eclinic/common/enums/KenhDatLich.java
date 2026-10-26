package iuh.fit.se.eclinic.common.enums;

/** Lịch hẹn được đặt qua kênh nào. */
public enum KenhDatLich {
    /** Khách đặt không đăng nhập. */
    KHACH,
    /** Tài khoản bệnh nhân đặt khi đã đăng nhập (cho bản thân hoặc người thân). */
    TAI_KHOAN,
    /** Quản trị viên đặt thay người gọi tổng đài / đến quầy, như khách (không gắn tài khoản). */
    HOTLINE
}
