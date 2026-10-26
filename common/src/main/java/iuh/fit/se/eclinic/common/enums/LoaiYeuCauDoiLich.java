package iuh.fit.se.eclinic.common.enums;

/**
 * Loại yêu cầu đổi lịch làm việc của bác sĩ (SCHED-02).
 */
public enum LoaiYeuCauDoiLich {
    DOI_CA,
    XIN_NGHI,
    /** Nhường 1 ca cho 1 bác sĩ cùng chuyên khoa: bác sĩ nhận đồng ý trước, rồi quản trị viên duyệt (SCHED-03). */
    NHUONG_CA
}
