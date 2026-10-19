package iuh.fit.se.eclinic.common.util;

import java.util.regex.Pattern;

/**
 * Mã hiển thị của bác sĩ: "BS" + id đủ 4 chữ số (BS0007). Suy ra từ id, không lưu trong DB. Danh bạ bác sĩ của quản
 * trị viên (catalog-service) hiện mã này; tệp Excel lịch làm việc (booking-service) dùng nó để chỉ bác sĩ.
 */
public final class MaBacSi {

    private static final Pattern MAU = Pattern.compile("(?i)^BS(\\d{1,9})$");

    private MaBacSi() {
    }

    public static String tu(Long id) {
        return String.format("BS%04d", id);
    }

    /** Id bác sĩ của mã (không phân biệt hoa / thường, bỏ khoảng trắng 2 đầu, BS12 = BS0012); null nếu sai dạng. */
    public static Long docId(String ma) {
        if (ma == null) {
            return null;
        }
        var khop = MAU.matcher(ma.strip());
        return khop.matches() ? Long.valueOf(khop.group(1)) : null;
    }

}
