package iuh.fit.se.eclinic.common.enums;

/** Trạng thái liên kết hồ sơ bệnh nhân (CCCD) với tài khoản. */
public enum TrangThaiLienKet {
    /** Hồ sơ chưa gắn tài khoản (Khách đặt lịch). */
    CHUA_LIEN_KET,
    /** Đã gắn tài khoản nhưng SĐT không khớp hồ sơ: chờ quản trị viên xác minh. */
    CHO_XAC_MINH,
    DA_LIEN_KET
}
