package iuh.fit.se.eclinic.booking.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Chuẩn hoá họ tên người khám / người giám hộ trước khi lưu và khi đối chiếu với hồ sơ đã có.
 */
public final class ChuanHoaTen {

    private static final Pattern KHOANG_TRANG = Pattern.compile("\\s+");
    private static final Pattern DAU = Pattern.compile("\\p{M}+");

    private ChuanHoaTen() {
    }

    /** Dạng để lưu: bỏ khoảng trắng 2 đầu, giữa các từ chỉ còn 1 dấu cách. */
    public static String gon(String hoTen) {
        return KHOANG_TRANG.matcher(hoTen.trim()).replaceAll(" ");
    }

    /** true nếu 2 họ tên là một khi không xét dấu tiếng Việt, hoa/thường và khoảng trắng thừa. */
    public static boolean giongNhau(String hoTen1, String hoTen2) {
        return khongDau(hoTen1).equals(khongDau(hoTen2));
    }

    private static String khongDau(String hoTen) {
        // NFD tách dấu khỏi chữ cái; đ/Đ không phải chữ có dấu tổ hợp nên đổi riêng
        String tachDau = Normalizer.normalize(gon(hoTen), Normalizer.Form.NFD);
        return DAU.matcher(tachDau).replaceAll("").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }

}
