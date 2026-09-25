package iuh.fit.fe.be_websatlichkham.common.enums;

/**
 * Trạng thái liên kết hồ sơ bệnh nhân (CCCD) với tài khoản — quy tắc #3 (Q2).
 */
public enum TrangThaiLienKet {
    /** Hồ sơ chưa gắn tài khoản (Khách đặt lịch). */
    CHUA_LIEN_KET,
    /** Đã gắn tài khoản nhưng SĐT không khớp hồ sơ -> chờ Admin xác minh; chưa hiển thị các lượt khám cũ. */
    CHO_XAC_MINH,
    DA_LIEN_KET
}
