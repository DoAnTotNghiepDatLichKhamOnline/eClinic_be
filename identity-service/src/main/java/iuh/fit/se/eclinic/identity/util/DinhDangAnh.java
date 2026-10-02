package iuh.fit.se.eclinic.identity.util;

import java.util.Optional;

/**
 * Định dạng ảnh được chấp nhận, nhận ra bằng các byte đầu của tệp. Không tin tên tệp hay Content-Type do client gửi:
 * cả hai đều tự khai được.
 */
public enum DinhDangAnh {

    JPEG,
    PNG,
    WEBP;

    private static final int[] DAU_JPEG = { 0xFF, 0xD8, 0xFF };
    private static final int[] DAU_PNG = { 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
    private static final int[] DAU_RIFF = { 'R', 'I', 'F', 'F' };
    private static final int[] DAU_WEBP = { 'W', 'E', 'B', 'P' };
    /** WebP: "RIFF" + 4 byte độ dài + "WEBP". */
    private static final int VI_TRI_WEBP = 8;

    public static Optional<DinhDangAnh> nhanDien(byte[] noiDung) {
        if (noiDung == null) {
            return Optional.empty();
        }
        if (batDauBang(noiDung, 0, DAU_JPEG)) {
            return Optional.of(JPEG);
        }
        if (batDauBang(noiDung, 0, DAU_PNG)) {
            return Optional.of(PNG);
        }
        if (batDauBang(noiDung, 0, DAU_RIFF) && batDauBang(noiDung, VI_TRI_WEBP, DAU_WEBP)) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean batDauBang(byte[] noiDung, int viTri, int[] mau) {
        if (noiDung.length < viTri + mau.length) {
            return false;
        }
        for (int i = 0; i < mau.length; i++) {
            if ((noiDung[viTri + i] & 0xFF) != mau[i]) {
                return false;
            }
        }
        return true;
    }

}
