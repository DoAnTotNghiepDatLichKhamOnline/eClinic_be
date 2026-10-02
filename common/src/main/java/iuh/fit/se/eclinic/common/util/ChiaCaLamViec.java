package iuh.fit.se.eclinic.common.util;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Chia 1 ca làm việc thành các lượt khám (BOOK-12): ca gồm các khung 1 giờ tính từ giờ bắt đầu ca,
 * mỗi khung có tối đa N lượt, mỗi lượt t phút. Khung cuối có thể ngắn hơn 1 giờ nên ít lượt hơn.
 */
public final class ChiaCaLamViec {

    public static final int SO_PHUT_MOI_KHUNG = 60;

    private ChiaCaLamViec() {
    }

    /**
     * Giờ bắt đầu của từng lượt khám trong ca, tăng dần. Lượt kết thúc sau giờ kết thúc ca thì bỏ.
     *
     * @param soLuotMoiGio  N, số lượt tối đa trong 1 khung 1 giờ
     * @param soPhutMoiLuot t, số phút của 1 lượt (N x t &lt;= 60)
     */
    public static List<LocalTime> gioBatDauCacLuot(LocalTime gioBatDauCa, LocalTime gioKetThucCa, int soLuotMoiGio,
            int soPhutMoiLuot) {
        if (soLuotMoiGio <= 0 || soPhutMoiLuot <= 0 || soLuotMoiGio * soPhutMoiLuot > SO_PHUT_MOI_KHUNG) {
            throw new IllegalArgumentException("Cần N > 0, t > 0 và N x t <= 60");
        }
        // Tính theo phút trong ngày để không bị vòng qua nửa đêm
        int phutKetThucCa = gioKetThucCa.toSecondOfDay() / 60;
        List<LocalTime> ketQua = new ArrayList<>();
        for (int phutKhung = gioBatDauCa.toSecondOfDay() / 60; phutKhung < phutKetThucCa;
                phutKhung += SO_PHUT_MOI_KHUNG) {
            for (int viTri = 0; viTri < soLuotMoiGio; viTri++) {
                int phutLuot = phutKhung + viTri * soPhutMoiLuot;
                if (phutLuot + soPhutMoiLuot <= phutKetThucCa) {
                    ketQua.add(LocalTime.ofSecondOfDay(phutLuot * 60L));
                }
            }
        }
        return ketQua;
    }

    /** Giờ bắt đầu của khung 1 giờ chứa lượt khám. Khung tính từ giờ bắt đầu ca, không theo giờ tròn. */
    public static LocalTime gioBatDauKhung(LocalTime gioBatDauCa, LocalTime gioBatDauLuot) {
        int phutCa = gioBatDauCa.toSecondOfDay() / 60;
        int phutLuot = gioBatDauLuot.toSecondOfDay() / 60;
        if (phutLuot < phutCa) {
            throw new IllegalArgumentException("Lượt khám bắt đầu trước giờ bắt đầu ca");
        }
        int chiSoKhung = (phutLuot - phutCa) / SO_PHUT_MOI_KHUNG;
        return LocalTime.ofSecondOfDay((phutCa + chiSoKhung * SO_PHUT_MOI_KHUNG) * 60L);
    }

    /** Giờ kết thúc của khung: sau giờ bắt đầu khung 1 giờ, nhưng không quá giờ kết thúc ca (khung cuối có thể ngắn hơn). */
    public static LocalTime gioKetThucKhung(LocalTime gioBatDauKhung, LocalTime gioKetThucCa) {
        int phutKetThuc = gioBatDauKhung.toSecondOfDay() / 60 + SO_PHUT_MOI_KHUNG;
        int phutKetThucCa = gioKetThucCa.toSecondOfDay() / 60;
        return phutKetThuc >= phutKetThucCa ? gioKetThucCa : LocalTime.ofSecondOfDay(phutKetThuc * 60L);
    }

}
