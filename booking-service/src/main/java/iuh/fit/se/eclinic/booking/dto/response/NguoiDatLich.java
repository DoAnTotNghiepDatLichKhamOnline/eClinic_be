package iuh.fit.se.eclinic.booking.dto.response;

/**
 * Ai đã đặt 1 lịch hẹn, nhìn từ tài khoản đang xem.
 */
public enum NguoiDatLich {
    /** Chính tài khoản đang xem. */
    TOI,
    /** Đặt khi không đăng nhập. */
    KHACH,
    TAI_KHOAN_KHAC
}
