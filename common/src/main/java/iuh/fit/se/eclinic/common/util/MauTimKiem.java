package iuh.fit.se.eclinic.common.util;

/** Đổi từ khoá thành mẫu LIKE "chứa" / "bắt đầu bằng", đã escape {@code %} và {@code _}. */
public final class MauTimKiem {

    public static final char KY_TU_ESCAPE = '!';

    private MauTimKiem() {
    }

    /** Mẫu "chứa từ khoá"; null nếu từ khoá rỗng (= không lọc). */
    public static String chua(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) {
            return null;
        }
        return "%" + escape(tuKhoa.trim()) + "%";
    }

    /** Mẫu "bắt đầu bằng từ khoá"; null nếu từ khoá rỗng (= không lọc). */
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
