package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.TrangThaiLienKet;

/** Hồ sơ bệnh nhân của tài khoản đang đăng nhập, không che vì người xem là chủ hồ sơ. */
public record HoSoCuaToiResponse(TrangThaiLienKet trangThaiLienKet, String cccd, String hoTen, LocalDate ngaySinh,
        GioiTinh gioiTinh, String soDienThoai, String diaChi, String soBaoHiemYTe, String tienSuBenhLy) {
}
