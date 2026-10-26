package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.identity.dto.response.HoSoCaNhanResponse;
import iuh.fit.se.eclinic.identity.event.TaiKhoanDaXoaEvent;

/** Ảnh đại diện của người đang đăng nhập (/api/users/me/avatar). */
public interface AnhDaiDienService {

    /** Đổi ảnh đại diện: kiểm tra JPEG / PNG / WebP, tải lên kho ảnh (ghi đè ảnh cũ) rồi lưu URL mới. */
    HoSoCaNhanResponse taiLen(Long idTaiKhoan, byte[] noiDung);

    /** Bỏ ảnh đại diện. */
    HoSoCaNhanResponse xoa(Long idTaiKhoan);

    /** Xoá ảnh trong kho của tài khoản vừa bị quản trị viên xoá, nếu ảnh nằm trong kho. */
    void donAnhCuaTaiKhoanDaXoa(TaiKhoanDaXoaEvent event);

}
