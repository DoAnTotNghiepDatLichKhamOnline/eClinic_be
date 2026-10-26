package iuh.fit.se.eclinic.report.dto.response;

/**
 * Mỗi điểm của chuỗi theo thời gian gom bao lâu: kỳ tới 31 ngày theo ngày, tới 184 ngày theo tuần (thứ Hai - Chủ
 * nhật), dài hơn theo tháng.
 */
public enum DonViThoiGian {
    NGAY,
    TUAN,
    THANG
}
