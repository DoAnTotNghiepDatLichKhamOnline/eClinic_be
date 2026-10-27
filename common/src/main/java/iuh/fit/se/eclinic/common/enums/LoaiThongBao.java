package iuh.fit.se.eclinic.common.enums;

/** Loại thông báo: các sự kiện phát thông báo. */
public enum LoaiThongBao {
    // Bác sĩ
    LICH_HEN_MOI,
    LICH_HEN_DA_DOI,
    LICH_HEN_DA_HUY,
    KET_QUA_DUYET_DOI_LICH,
    CA_LAM_VIEC_THAY_DOI,
    /** Bác sĩ được đồng nghiệp nhường ca, hoặc yêu cầu nhường ca gửi mình vừa đổi trạng thái. */
    YEU_CAU_NHUONG_CA,
    // Bệnh nhân
    LICH_HEN_DA_XAC_NHAN,
    LICH_HEN_BI_TU_CHOI,
    LICH_HEN_CAN_DOI,
    /** Ca khám đổi phòng: lịch hẹn giữ nguyên giờ, chỉ đổi phòng khám. */
    LICH_HEN_DOI_PHONG,
    /** Ca khám được nhường cho bác sĩ khác: lịch hẹn giữ nguyên giờ, phòng; bác sĩ khám đổi. */
    LICH_HEN_DOI_BAC_SI,
    NHAC_LICH_KHAM,
    /** Bác sĩ hẹn / đổi ngày tái khám, và lời nhắc trước ngày đó. Chỉ là lời nhắc, không có lịch hẹn mới. */
    NHAC_TAI_KHAM,
    /** Quản trị viên hủy lịch hẹn, kèm lý do. */
    LICH_HEN_QUAN_TRI_HUY,
    /** Quản trị viên đổi lịch hẹn sang khung giờ khác thay bệnh nhân. */
    LICH_HEN_QUAN_TRI_DOI,
    // Admin
    YEU_CAU_DOI_LICH_MOI,
    // Chung
    HE_THONG
}
