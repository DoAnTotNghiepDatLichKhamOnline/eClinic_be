package iuh.fit.se.eclinic.common.util;

/**
 * Đổi từ khoá người dùng gõ thành mẫu LIKE "chứa từ khoá" / "bắt đầu bằng từ khoá", đã escape {@code %} và {@code _}.
 * Câu truy vấn phải viết {@code like :mau escape '!'}.
 */
public final class MauTimKiem {

    public static final char KY_TU_ESCAPE = '!';

    private MauTimKiem() {
    }

    /** @return null nếu từ khoá rỗng (= không lọc) */
    public static String chua(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) {
            return null;
        }
        return "%" + escape(tuKhoa.trim()) + "%";
    }

    /** @return null nếu từ khoá rỗng (= không lọc) */
    public static String batDau(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) {
            return null;
        }
        return escape(tuKhoa.trim()) + "%";
    }

    private static String escape(String tuKhoa) {
        StringBuilder mau = new StringBuilder();
        for (char kyTu : tuKhoa.toCharArray()) {
            if (kyTu == '%' || kyTu == '_' || kyTu == KY_TU_ESCAPE) {
                mau.append(KY_TU_ESCAPE);
            }
            mau.append(kyTu);
        }
        return mau.toString();
    }
}
