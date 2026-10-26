package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;

/**
 * 1 dòng trong danh sách hồ sơ bệnh nhân của quản trị viên. Số CCCD đã che, số đầy đủ chỉ có khi mở hồ sơ
 * ({@link HoSoBenhNhanQuanTriResponse}).
 *
 * @param cccdChe    chỉ còn 4 số cuối; null nếu hồ sơ chưa có số CCCD
 * @param coTaiKhoan hồ sơ đang gắn với 1 tài khoản (đã liên kết hoặc đang chờ xác minh, xem {@code trangThaiLienKet})
 * @param soLichHen  số lịch hẹn của hồ sơ, mọi trạng thái
 */
public record HoSoBenhNhanDongResponse(
        Long id,
        String hoTen,
        LocalDate ngaySinh,
        GioiTinh gioiTinh,
        String soDienThoai,
        String cccdChe,
        boolean coTaiKhoan,
        TrangThaiLienKet trangThaiLienKet,
        long soLichHen,
        LocalDateTime ngayTao) {
}
