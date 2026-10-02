package iuh.fit.se.eclinic.identity.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.VaiTro;

/**
 * Chi tiết 1 tài khoản cho quản trị viên (GET /api/users/{id}). Không có dữ liệu y tế: từ hồ sơ bệnh nhân chỉ lấy ngày
 * sinh và trạng thái liên kết.
 *
 * @param lyDoVoHieuHoa        chỉ có khi tài khoản đang bị vô hiệu hoá
 * @param ngaySinh             lấy từ hồ sơ bệnh nhân đã liên kết (DA_LIEN_KET); null nếu không có
 * @param coMatKhau            false: tài khoản chỉ đăng nhập bằng Google, chưa đặt mật khẩu
 * @param lienKetGoogle        true: tài khoản đã liên kết với Google
 * @param trangThaiLienKetHoSo trạng thái hồ sơ bệnh nhân gắn với tài khoản; null nếu tài khoản chưa gắn hồ sơ nào
 * @param bacSi                chỉ có với vai trò BAC_SI (null nếu chưa có hồ sơ bác sĩ)
 */
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
