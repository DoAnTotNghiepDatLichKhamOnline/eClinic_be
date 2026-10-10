package iuh.fit.se.eclinic.common.enums;

/**
 * Loại thông báo — các sự kiện phát thông báo theo NOTI-01.
 */
public enum LoaiThongBao {
    // Bác sĩ
    LICH_HEN_MOI,
    LICH_HEN_DA_DOI,
    LICH_HEN_DA_HUY,
    KET_QUA_DUYET_DOI_LICH,
    CA_LAM_VIEC_THAY_DOI,
    // Bệnh nhân
    LICH_HEN_DA_XAC_NHAN,
    LICH_HEN_BI_TU_CHOI,
    LICH_HEN_CAN_DOI,
    /** Ca khám đổi phòng: lịch hẹn giữ nguyên giờ, chỉ đổi phòng khám. */
    LICH_HEN_DOI_PHONG,
    NHAC_LICH_KHAM,
    // Admin
    YEU_CAU_DOI_LICH_MOI,
    // Chung
    HE_THONG
}
