package iuh.fit.se.eclinic.booking.util;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.util.TokenNgauNhien;

/**
 * Khoá nhận diện hồ sơ của bệnh nhân dưới 18 tuổi chưa có CCCD (quy tắc #10): cùng họ tên (không xét dấu, hoa/thường,
 * khoảng trắng thừa) + ngày sinh + CCCD người giám hộ thì cùng 1 khoá, tức cùng 1 hồ sơ. Lưu bản băm để cột có độ dài
 * cố định và đặt UNIQUE được.
 */
public final class KhoaNhanDien {

    private KhoaNhanDien() {
    }

    /** SHA-256 hex (64 ký tự). */
    public static String tao(String hoTen, LocalDate ngaySinh, String cccdNguoiGiamHo) {
        return TokenNgauNhien.bam(ChuanHoaTen.khongDau(hoTen) + "|" + ngaySinh + "|" + cccdNguoiGiamHo);
    }

}
