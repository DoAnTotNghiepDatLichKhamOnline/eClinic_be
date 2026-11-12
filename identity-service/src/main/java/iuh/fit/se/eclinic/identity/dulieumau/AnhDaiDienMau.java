package iuh.fit.se.eclinic.identity.dulieumau;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import iuh.fit.se.eclinic.common.dulieumau.AnhMau;
import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.identity.util.ChuanHoa;

/**
 * Ảnh đại diện mẫu của tài khoản mẫu, suy ra từ email ({@code bacsiNN}, {@code benhnhanNN}, {@code quantriNNN}
 * {@code @eclinic.local}). Dùng chung cho lúc tạo dữ liệu mẫu và lúc thay ảnh giữ chỗ trong DB cũ, để hai nơi luôn khớp.
 */
final class AnhDaiDienMau {

    private static final Pattern BAC_SI = Pattern.compile("^bacsi(\\d{2,})@eclinic\\.local$");
    private static final Pattern BENH_NHAN = Pattern.compile("^benhnhan(\\d{2,})@eclinic\\.local$");
    private static final Pattern QUAN_TRI = Pattern.compile("^quantri(\\d{3})@eclinic\\.local$");

    private AnhDaiDienMau() {
    }

    /** URL ảnh mẫu của tài khoản có email này; null nếu không phải tài khoản mẫu (vd. admin mặc định). */
    static String theoEmail(String email) {
        if (email == null) {
            return null;
        }
        String chuan = ChuanHoa.email(email);
        Integer so = so(BAC_SI, chuan);
        if (so != null) {
            return so <= DuLieuMau.BAC_SI.size() ? AnhMau.bacSi(so)
                    : AnhMau.bacSiTheoGioiTinh(DuLieuMauLon.gioiTinh(so), so);
        }
        so = so(BENH_NHAN, chuan);
        if (so != null) {
            // Bệnh nhân mẫu chính: số lẻ là nữ, số chẵn là nam (theo họ tên; 5 và 6 không có hồ sơ để đọc giới tính)
            return so <= DuLieuMau.BENH_NHAN.size()
                    ? AnhMau.nguoiDung(so % 2 == 1 ? GioiTinh.NU : GioiTinh.NAM, (so - 1) / 2)
                    : AnhMau.nguoiDung(DuLieuMauLon.gioiTinh(so), so);
        }
        so = so(QUAN_TRI, chuan);
        return so != null ? AnhMau.nguoiDung(DuLieuMauLon.gioiTinh(so), so) : null;
    }

    /** Số của bác sĩ mẫu ({@code bacsiNN@eclinic.local} -> NN); null nếu không phải email bác sĩ mẫu. */
    static Integer soBacSi(String email) {
        return email == null ? null : so(BAC_SI, ChuanHoa.email(email));
    }

    private static Integer so(Pattern mau, String email) {
        Matcher m = mau.matcher(email);
        if (!m.matches()) {
            return null;
        }
        try {
            int so = Integer.parseInt(m.group(1));
            return so > 0 ? so : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
