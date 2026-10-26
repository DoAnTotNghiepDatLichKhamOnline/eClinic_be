package iuh.fit.se.eclinic.common.luutru;

import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;

/**
 * Kho lưu ảnh bên ngoài. Phần còn lại của service chỉ biết interface này: đổi nhà cung cấp = viết 1 class mới
 * (hiện tại: {@link LuuTruAnhCloudinary}).
 * <p>
 * {@code ma} là mã ảnh do bên gọi đặt (vd {@code avatar/12}); tải lên cùng mã thì ảnh cũ bị ghi đè.
 * <p>
 * Nằm trong common vì identity-service (ảnh đại diện) và catalog-service (ảnh giới thiệu bác sĩ) cùng dùng; bean chỉ
 * được tạo ở service đặt {@code app.cloudinary.bat=true} (xem {@link LuuTruAnhConfig}).
 */
public interface LuuTruAnh {

    /** false: chưa có thông tin kết nối, {@link #taiLen} sẽ báo LUU_TRU_ANH_KHONG_KHA_DUNG. */
    boolean daCauHinh();

    /** Cạnh dài nhất (pixel) của ảnh đại diện sau khi lưu. */
    int CANH_ANH_DAI_DIEN = 512;

    /** Như {@link #taiLen(String, byte[], int)} với kích thước của ảnh đại diện. */
    default String taiLen(String ma, byte[] noiDung) {
        return taiLen(ma, noiDung, CANH_ANH_DAI_DIEN);
    }

    /**
     * Tải ảnh lên, ghi đè ảnh đang có cùng mã.
     *
     * @param canhToiDa ảnh lớn hơn được thu nhỏ để cả chiều rộng lẫn chiều cao không quá số pixel này
     * @return URL công khai của ảnh (khác nhau sau mỗi lần tải lên để trình duyệt không dùng ảnh cũ trong cache)
     * @throws LoiNghiepVu ANH_KHONG_HOP_LE (400) khi kho từ chối nội dung ảnh;
     *                     LUU_TRU_ANH_KHONG_KHA_DUNG (503) khi chưa cấu hình hoặc kho không trả lời / báo lỗi
     */
    String taiLen(String ma, byte[] noiDung, int canhToiDa);

    /**
     * Xoá ảnh theo mã; ảnh không tồn tại không phải là lỗi.
     *
     * @throws LoiNghiepVu LUU_TRU_ANH_KHONG_KHA_DUNG (503) khi chưa cấu hình hoặc kho không trả lời / báo lỗi
     */
    void xoa(String ma);

    /** URL này có phải ảnh nằm trong kho của mình không (ảnh Google của tài khoản đăng nhập Google thì không). */
    boolean laAnhCuaKho(String url);

}
