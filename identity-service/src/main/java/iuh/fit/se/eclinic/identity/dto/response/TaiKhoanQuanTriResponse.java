package iuh.fit.se.eclinic.identity.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;

/** 1 dòng trong danh sách tài khoản của quản trị viên (GET /api/users). */
public record TaiKhoanQuanTriResponse(
        Long id,
        String hoTen,
        String email,
        String soDienThoai,
        String anhDaiDien,
        VaiTro vaiTro,
        TrangThaiTaiKhoan trangThai,
        String lyDoVoHieuHoa,
        LocalDate ngaySinh,
        LocalDateTime ngayTao) {
}
