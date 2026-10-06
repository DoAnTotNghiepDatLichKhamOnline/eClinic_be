package iuh.fit.se.eclinic.booking.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;

/**
 * Khoảng ngày [tuNgay, denNgay] của các API xem theo lịch (lịch làm việc, lịch hẹn theo khoảng ngày).
 */
public final class KhoangNgay {

    /** Số ngày tối đa của 1 lần xem (lưới tháng 6 tuần). */
    public static final int SO_NGAY_TOI_DA = 42;

    private KhoangNgay() {
    }

    /** Ném DU_LIEU_KHONG_HOP_LE nếu denNgay trước tuNgay hoặc khoảng dài hơn {@link #SO_NGAY_TOI_DA} ngày. */
    public static void kiemTra(LocalDate tuNgay, LocalDate denNgay) {
        if (denNgay.isBefore(tuNgay)) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "denNgay phải từ tuNgay trở đi");
        }
        if (ChronoUnit.DAYS.between(tuNgay, denNgay) >= SO_NGAY_TOI_DA) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE,
                    "Mỗi lần chỉ xem được tối đa " + SO_NGAY_TOI_DA + " ngày");
        }
    }

}
