package iuh.fit.se.eclinic.identity.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;

/** Chi tiết 1 tài khoản cho quản trị viên (GET /api/users/{id}). */
public record ChiTietTaiKhoanResponse(
        Long id,
        String hoTen,
        String email,
        String soDienThoai,
        String anhDaiDien,
        VaiTro vaiTro,
        TrangThaiTaiKhoan trangThai,
        String lyDoVoHieuHoa,
        LocalDate ngaySinh,
        LocalDateTime ngayTao,
        boolean coMatKhau,
        boolean lienKetGoogle,
        LocalDateTime ngayCapNhat,
        TrangThaiLienKet trangThaiLienKetHoSo,
        HoSoBacSiResponse bacSi) {
}
