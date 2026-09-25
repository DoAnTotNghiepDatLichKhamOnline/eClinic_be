package iuh.fit.fe.be_websatlichkham.common.enums;

/**
 * Trạng thái tài khoản (AUTH-01, AUTH-02, ADM-03).
 */
public enum TrangThaiTaiKhoan {
    /** Đã đăng ký, chưa bấm link kích hoạt trong email. */
    CHO_XAC_NHAN,
    DA_KICH_HOAT,
    /** Admin vô hiệu hóa (ADM-03) hoặc bác sĩ ngừng công tác (ADM-02): không đăng nhập được. */
    VO_HIEU_HOA
}
