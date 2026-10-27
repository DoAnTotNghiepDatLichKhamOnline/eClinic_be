package iuh.fit.se.eclinic.booking.util;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.util.TokenNgauNhien;

/** Khoá nhận diện hồ sơ của trẻ chưa có CCCD: họ tên chuẩn hoá, ngày sinh và CCCD người giám hộ. */
public final class KhoaNhanDien {

    private KhoaNhanDien() {
    }

    /** SHA-256 hex (64 ký tự). */
    public static String tao(String hoTen, LocalDate ngaySinh, String cccdNguoiGiamHo) {
        return TokenNgauNhien.bam(ChuanHoaTen.khongDau(hoTen) + "|" + ngaySinh + "|" + cccdNguoiGiamHo);
    }

}
