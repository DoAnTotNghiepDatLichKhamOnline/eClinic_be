package iuh.fit.se.eclinic.common.config;

import java.util.TimeZone;

/** Múi giờ chung của hệ thống. */
public final class AppTimeZone {

    public static final String ID = "Asia/Ho_Chi_Minh";

    private AppTimeZone() {
    }

    /** Toàn hệ thống dùng giờ Việt Nam (lịch khám là giờ địa phương). */
    public static void apply() {
        TimeZone.setDefault(TimeZone.getTimeZone(ID));
    }

}
