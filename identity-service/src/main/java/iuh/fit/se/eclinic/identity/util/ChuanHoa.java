package iuh.fit.se.eclinic.identity.util;

import java.util.Locale;

/**
 * Chuẩn hoá dữ liệu trước khi lưu / so sánh. Mọi chỗ ghi hoặc tìm theo email phải đi qua đây.
 */
public final class ChuanHoa {

    private ChuanHoa() {
    }

    /** Bỏ khoảng trắng 2 đầu, chữ thường. */
    public static String email(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

}
