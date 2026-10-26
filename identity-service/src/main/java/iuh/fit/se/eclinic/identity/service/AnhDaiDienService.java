package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.identity.dto.response.HoSoCaNhanResponse;
import iuh.fit.se.eclinic.identity.event.TaiKhoanDaXoaEvent;

/**
 * Ảnh đại diện của người đang đăng nhập (/api/users/me/avatar). {@code idTaiKhoan} luôn lấy từ JWT ở controller.
 * Mỗi tài khoản có nhiều nhất 1 ảnh trong kho ảnh; ảnh lấy từ Google (đăng nhập Google) chỉ là URL của Google,
 * không nằm trong kho.
 */
public interface AnhDaiDienService {

    /**
     * Đổi ảnh đại diện: kiểm tra nội dung là JPEG / PNG / WebP (ANH_KHONG_HOP_LE), tải lên kho ảnh (ghi đè ảnh cũ của
     * tài khoản) rồi lưu URL mới. Kho ảnh chưa cấu hình hoặc lỗi: LUU_TRU_ANH_KHONG_KHA_DUNG, ảnh hiện tại giữ nguyên.
     * Tải lên quá số lần cho phép trong 1 giờ: GUI_LAI_QUA_NHANH.
     *
     * @return hồ sơ cá nhân với ảnh mới
     */
    HoSoCaNhanResponse taiLen(Long idTaiKhoan, byte[] noiDung);

    /**
     * Bỏ ảnh đại diện. Luôn thành công kể cả khi kho ảnh chưa cấu hình hoặc đang lỗi (ảnh còn sót trong kho sẽ bị
     * ghi đè ở lần tải lên sau); tài khoản chưa có ảnh thì không làm gì.
     *
     * @return hồ sơ cá nhân không còn ảnh
     */
    HoSoCaNhanResponse xoa(Long idTaiKhoan);

    /**
     * Tài khoản vừa bị quản trị viên xoá (QuanLyTaiKhoanService#xoa): xoá ảnh của nó trong kho ảnh, nếu ảnh nằm trong
     * kho. Chạy nền sau khi transaction xoá tài khoản commit; lỗi kho chỉ ghi log, không ảnh hưởng việc xoá tài khoản.
     */
    void donAnhCuaTaiKhoanDaXoa(TaiKhoanDaXoaEvent event);

}
