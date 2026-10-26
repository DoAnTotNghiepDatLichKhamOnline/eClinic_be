package iuh.fit.se.eclinic.booking.util;

/** Che CCCD, SĐT, họ tên và email trước khi đưa lên trang công khai hoặc phản hồi cho nhân viên. */
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

    /** Cho danh sách hồ sơ bệnh nhân của quản trị viên, giữ 4 số cuối: {@code 079080001123} -> {@code ********1123}. */
    public static String cccdGiuBonSoCuoi(String cccd) {
        if (cccd == null) {
            return null;
        }
        int soKyTuChe = Math.max(cccd.length() - 4, 0);
        return "*".repeat(soKyTuChe) + cccd.substring(soKyTuChe);
    }

    /** Họ tên cho sổ khám công khai: {@code Nguyễn Văn An} -> {@code N. V. An}. */
    public static String hoTen(String hoTen) {
        if (hoTen == null) {
            return null;
        }
        String[] chu = hoTen.trim().split("\\s+");
        StringBuilder ketQua = new StringBuilder();
        for (int i = 0; i < chu.length - 1; i++) {
            ketQua.appendCodePoint(chu[i].codePointAt(0)).append(". ");
        }
        return ketQua.append(chu[chu.length - 1]).toString();
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

    /** Email cho phản hồi cấp lại liên kết: {@code buihoang@gmail.com} -> {@code b***@gmail.com}. */
    public static String email(String email) {
        if (email == null) {
            return null;
        }
        int viTriA = email.indexOf('@');
        if (viTriA <= 0) {
            return "***";
        }
        return email.substring(0, 1) + "***" + email.substring(viTriA);
    }

}
