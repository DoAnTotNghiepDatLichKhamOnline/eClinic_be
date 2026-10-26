package iuh.fit.se.eclinic.catalog.dto.response;

import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;

/**
 * 1 dòng của danh bạ bác sĩ cho quản trị viên (kể cả bác sĩ ngừng công tác).
 *
 * @param maBacSi        mã hiển thị, suy ra từ id ("BS" + id đủ 4 chữ số); không lưu trong DB
 * @param soLuotDaKham   số lịch hẹn đã khám xong của bác sĩ
 * @param phaiDoiMatKhau true: bác sĩ chưa đăng nhập lần nào (còn mang mật khẩu mặc định)
 */
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
