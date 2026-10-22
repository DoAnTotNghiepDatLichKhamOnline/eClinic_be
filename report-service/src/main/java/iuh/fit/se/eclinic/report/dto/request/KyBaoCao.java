package iuh.fit.se.eclinic.report.dto.request;

/**
 * Kỳ của báo cáo thống kê (DASH-02): ngày, tuần (thứ Hai - Chủ nhật), tháng, năm chứa 1 ngày mốc, hoặc khoảng tuỳ chọn.
 */
public enum KyBaoCao {
    NGAY,
    TUAN,
    THANG,
    NAM,
    /** Từ ngày - đến ngày (tính cả 2 đầu), tối đa {@code KhoangBaoCao.SO_NGAY_TOI_DA} ngày. */
    TUY_CHON
}
