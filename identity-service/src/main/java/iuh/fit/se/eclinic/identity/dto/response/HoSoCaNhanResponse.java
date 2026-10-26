package iuh.fit.se.eclinic.identity.dto.response;

import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;

/**
 * Hồ sơ cá nhân của người đang đăng nhập (GET /api/users/me).
 *
 * @param ngonNgu       ngôn ngữ ưa dùng đã lưu của tài khoản (VI / EN), đổi bằng PUT /api/users/me/language
 * @param coMatKhau     false: tài khoản chỉ đăng nhập bằng Google, chưa đặt mật khẩu
 * @param lienKetGoogle true: tài khoản đã liên kết với Google
 * @param bacSi         chỉ có với vai trò BAC_SI (null nếu chưa có hồ sơ bác sĩ)
 * @param hoSoBenhNhan  chỉ có với vai trò BENH_NHAN (null nếu tài khoản chưa gắn hồ sơ bệnh nhân)
 */
public record HoSoCaNhanResponse(
        Long id,
        String hoTen,
        String email,
        String soDienThoai,
        String anhDaiDien,
        VaiTro vaiTro,
        TrangThaiTaiKhoan trangThai,
        NgonNgu ngonNgu,
        boolean coMatKhau,
        boolean lienKetGoogle,
        LocalDateTime ngayTao,
        HoSoBacSiResponse bacSi,
        HoSoBenhNhanResponse hoSoBenhNhan) {
}
