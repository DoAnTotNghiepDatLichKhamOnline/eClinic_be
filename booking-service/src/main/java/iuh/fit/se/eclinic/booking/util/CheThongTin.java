package iuh.fit.se.eclinic.booking.util;

/**
 * Che CCCD và số điện thoại trước khi đưa lên phiếu khám công khai (quy tắc #8).
 */
public final class CheThongTin {

    private static final int SO_KY_TU_GIU_LAI = 3;

    private CheThongTin() {
    }

    /** Chỉ giữ 3 số cuối: {@code 079080001123} -> {@code *********123}. */
    public static String cccd(String cccd) {
        if (cccd == null) {
            return null;
        }
        int soKyTuChe = Math.max(cccd.length() - SO_KY_TU_GIU_LAI, 0);
        return "*".repeat(soKyTuChe) + cccd.substring(soKyTuChe);
    }

    /** Giữ 3 số đầu và 3 số cuối: {@code 0901234567} -> {@code 090****567}. Chuỗi quá ngắn thì che hết. */
    public static String soDienThoai(String soDienThoai) {
        if (soDienThoai == null) {
            return null;
        }
        int doDai = soDienThoai.length();
        if (doDai <= 2 * SO_KY_TU_GIU_LAI) {
            return "*".repeat(doDai);
        }
        return soDienThoai.substring(0, SO_KY_TU_GIU_LAI) + "*".repeat(doDai - 2 * SO_KY_TU_GIU_LAI)
                + soDienThoai.substring(doDai - SO_KY_TU_GIU_LAI);
    }

}
