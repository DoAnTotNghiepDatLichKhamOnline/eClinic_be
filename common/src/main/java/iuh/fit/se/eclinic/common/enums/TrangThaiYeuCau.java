package iuh.fit.se.eclinic.common.enums;

import java.util.List;

/** Trạng thái yêu cầu đổi lịch làm việc. */
public enum TrangThaiYeuCau {
    /** NHUONG_CA vừa gửi: chờ bác sĩ nhận đồng ý; quản trị viên chưa duyệt được. */
    CHO_DONG_Y,
    CHO_DUYET,
    DA_DUYET,
    TU_CHOI,
    /** Bác sĩ tự rút yêu cầu khi còn chờ (CHO_DONG_Y / CHO_DUYET). */
    DA_RUT,
    /** NHUONG_CA: bác sĩ nhận từ chối nhận ca. */
    DONG_NGHIEP_TU_CHOI;

    /** Yêu cầu còn chờ: mỗi ca tối đa 1 yêu cầu ở các trạng thái này (UNIQUE trong DB). */
    public static final List<TrangThaiYeuCau> DANG_CHO = List.of(CHO_DONG_Y, CHO_DUYET);
}
