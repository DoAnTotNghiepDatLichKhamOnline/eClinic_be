package iuh.fit.se.eclinic.common.thongbao;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.MessageSource;

import iuh.fit.se.eclinic.common.enums.NgonNgu;

/** Câu do hệ thống viết vào 1 cột chữ tự do (vd ghi chú xử lý khi ca bị hủy làm đóng yêu cầu). */
public final class VanBanHeThong {

    private static final Pattern DANG_LUU = Pattern.compile("^\\{(heThong\\.[A-Za-z0-9.]+)\\}(.*)$", Pattern.DOTALL);

    private VanBanHeThong() {
    }

    /** Chữ để lưu vào cột. */
    public static String ghi(String khoa, String phanNguoiDungGo) {
        return "{" + khoa + "}" + (phanNguoiDungGo == null ? "" : phanNguoiDungGo);
    }

    /** Chữ để hiển thị theo ngôn ngữ; {@code daLuu} null thì trả null. */
    public static String doc(String daLuu, MessageSource messageSource, NgonNgu ngonNgu) {
        if (daLuu == null) {
            return null;
        }
        Matcher khop = DANG_LUU.matcher(daLuu);
        if (!khop.matches()) {
            return daLuu;
        }
        String cau = messageSource.getMessage(khop.group(1), new Object[] {khop.group(2)}, null, ngonNgu.locale());
        return cau == null ? daLuu : cau;
    }
}
