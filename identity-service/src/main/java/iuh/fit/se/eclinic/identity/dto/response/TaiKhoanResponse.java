package iuh.fit.se.eclinic.identity.dto.response;

import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;

/**
 * Thông tin tài khoản trả về cho frontend (không có mật khẩu).
 */
public record TaiKhoanResponse(
        Long id,
        String hoTen,
        String email,
        String soDienThoai,
        VaiTro vaiTro,
        TrangThaiTaiKhoan trangThai) {
}
