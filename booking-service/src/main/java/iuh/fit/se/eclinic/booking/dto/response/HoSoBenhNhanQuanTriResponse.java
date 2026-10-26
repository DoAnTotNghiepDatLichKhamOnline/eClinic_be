package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;

/**
 * Hồ sơ bệnh nhân cho quản trị viên đối chiếu / sửa: số CCCD KHÔNG che. Không có tiền sử bệnh lý.
 *
 * @param cccd       null với trẻ chưa có CCCD
 * @param idTaiKhoan tài khoản đang gắn với hồ sơ; null nếu hồ sơ chưa liên kết
 */
public record HoSoBenhNhanQuanTriResponse(
        Long id,
        String cccd,
        String hoTen,
        LocalDate ngaySinh,
        GioiTinh gioiTinh,
        String soDienThoai,
        String diaChi,
        String soBaoHiemYTe,
        TrangThaiLienKet trangThaiLienKet,
        Long idTaiKhoan,
        LocalDateTime ngayTao) {
}
