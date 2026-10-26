package iuh.fit.se.eclinic.common.luutru;

/** Kho lưu ảnh bên ngoài. */
public interface LuuTruAnh {

    /** false: chưa có thông tin kết nối, {@link #taiLen} sẽ báo LUU_TRU_ANH_KHONG_KHA_DUNG. */
    boolean daCauHinh();

    /** Cạnh dài nhất (pixel) của ảnh đại diện sau khi lưu. */
    int CANH_ANH_DAI_DIEN = 512;

    /** Như {@link #taiLen(String, byte[], int)} với kích thước của ảnh đại diện. */
    default String taiLen(String ma, byte[] noiDung) {
        return taiLen(ma, noiDung, CANH_ANH_DAI_DIEN);
    }

    /** Tải ảnh lên, ghi đè ảnh đang có cùng mã. */
    String taiLen(String ma, byte[] noiDung, int canhToiDa);

    /** Xoá ảnh theo mã; ảnh không tồn tại không phải là lỗi. */
    void xoa(String ma);

    /** URL này có phải ảnh nằm trong kho của mình không (ảnh Google của tài khoản đăng nhập Google thì không). */
    boolean laAnhCuaKho(String url);

}
