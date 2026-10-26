package iuh.fit.se.eclinic.identity.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;

/**
 * 1 dòng trong danh sách tài khoản của quản trị viên (GET /api/users, UC-USER-01). Chỉ có thông tin định danh cơ bản,
 * không có dữ liệu y tế.
 *
 * @param lyDoVoHieuHoa chỉ có khi tài khoản đang bị vô hiệu hoá
 * @param ngaySinh      lấy từ hồ sơ bệnh nhân đã liên kết (DA_LIEN_KET) của tài khoản; null nếu không có
 */
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
