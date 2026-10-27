package iuh.fit.se.eclinic.identity.dto.response;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;

/** Hồ sơ bệnh nhân (theo CCCD) gắn với tài khoản, chỉ xem. */
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
