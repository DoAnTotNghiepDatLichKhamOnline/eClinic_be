package iuh.fit.fe.be_websatlichkham.common.config;

import java.util.TimeZone;

/**
 * Múi giờ chung của hệ thống. Gọi {@link #apply()} ở đầu hàm main của mỗi service.
 */
public final class AppTimeZone {

    public static final String ID = "Asia/Ho_Chi_Minh";

    private AppTimeZone() {
    }

    /**
     * Toàn hệ thống dùng giờ Việt Nam (lịch khám là giờ địa phương). DATETIME lưu đúng giờ "tường" (LocalDateTime).
     */
    public static void apply() {
        TimeZone.setDefault(TimeZone.getTimeZone(ID));
    }

}
