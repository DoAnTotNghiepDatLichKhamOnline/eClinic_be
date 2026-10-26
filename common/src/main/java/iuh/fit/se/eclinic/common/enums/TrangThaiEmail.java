package iuh.fit.se.eclinic.common.enums;

/**
 * Trạng thái của 1 email trong hàng chờ gửi email_gui (V19).
 */
public enum TrangThaiEmail {

    /** Chưa gửi được, còn được thử (lần thử kế tiếp ở {@code lan_thu_ke_tiep}). */
    CHO_GUI,

    /** Máy chủ mail đã nhận; không bao giờ gửi lại. */
    DA_GUI,

    /** Đã thử hết số lần, hoặc mẫu không dựng được; không thử nữa. */
    THAT_BAI

}
