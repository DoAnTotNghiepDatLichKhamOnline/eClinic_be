package iuh.fit.se.eclinic.identity.dto.response;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;

/**
 * Hồ sơ bệnh nhân (theo CCCD) gắn với tài khoản, chỉ xem. Khi {@code trangThaiLienKet} là CHO_XAC_MINH
 * (chờ quản trị viên xác minh) thì mọi trường khác đều null: chưa xác minh thì chưa được xem thông tin hồ sơ.
 */
public record HoSoBenhNhanResponse(
        Long id,
        TrangThaiLienKet trangThaiLienKet,
        String cccd,
        String hoTen,
        LocalDate ngaySinh,
        GioiTinh gioiTinh,
        String soDienThoai,
        String diaChi,
        String soBaoHiemYTe,
        String tienSuBenhLy) {
}
