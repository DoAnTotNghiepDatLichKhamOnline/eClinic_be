package iuh.fit.se.eclinic.common.enums;

import java.time.LocalTime;

/** Buổi của 1 ca làm việc, theo giờ bắt đầu ca: trước 12:00 sáng, trước 17:00 chiều, còn lại tối. */
public enum BuoiKham {
    SANG,
    CHIEU,
    TOI;

    private static final LocalTime TRUA = LocalTime.NOON;
    private static final LocalTime CHIEU_TOI = LocalTime.of(17, 0);

    public static BuoiKham cua(LocalTime gioBatDau) {
        if (gioBatDau.isBefore(TRUA)) {
            return SANG;
        }
        return gioBatDau.isBefore(CHIEU_TOI) ? CHIEU : TOI;
    }

    /** Buổi gồm các giờ bắt đầu có phần giờ (0-23) trong [gioTu, gioDen), khớp {@link #cua}. */
    public int gioTu() {
        return switch (this) {
            case SANG -> 0;
            case CHIEU -> TRUA.getHour();
            case TOI -> CHIEU_TOI.getHour();
        };
    }

    /** Giờ kết thúc (không gồm) của buổi; buổi tối kéo tới hết ngày (24). */
    public int gioDen() {
        return switch (this) {
            case SANG -> TRUA.getHour();
            case CHIEU -> CHIEU_TOI.getHour();
            case TOI -> 24;
        };
    }
}
