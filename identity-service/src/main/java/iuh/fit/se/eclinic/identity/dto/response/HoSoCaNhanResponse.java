package iuh.fit.se.eclinic.identity.dto.response;

import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;

/** Hồ sơ cá nhân của người đang đăng nhập (GET /api/users/me). */
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
