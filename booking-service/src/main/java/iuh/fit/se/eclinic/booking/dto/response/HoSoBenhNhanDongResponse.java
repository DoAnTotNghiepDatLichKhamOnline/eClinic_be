package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;

/** 1 dòng trong danh sách hồ sơ bệnh nhân của quản trị viên. */
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
