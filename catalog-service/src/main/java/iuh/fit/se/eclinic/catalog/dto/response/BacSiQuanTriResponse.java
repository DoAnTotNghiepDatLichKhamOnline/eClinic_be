package iuh.fit.se.eclinic.catalog.dto.response;

import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;

/** 1 dòng của danh bạ bác sĩ cho quản trị viên (kể cả bác sĩ ngừng công tác). */
public record BacSiQuanTriResponse(
        Long id,
        String maBacSi,
        String hoTen,
        String email,
        String soDienThoai,
        String anhDaiDien,
        String hocVi,
        String soGiayPhep,
        Long idChuyenKhoa,
        String tenChuyenKhoa,
        long soLuotDaKham,
        TrangThaiBacSi trangThai,
        TrangThaiTaiKhoan trangThaiTaiKhoan,
        boolean phaiDoiMatKhau) {
}
