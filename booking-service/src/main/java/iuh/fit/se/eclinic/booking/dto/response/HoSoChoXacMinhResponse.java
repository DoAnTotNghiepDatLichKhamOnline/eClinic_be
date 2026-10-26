package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;

/**
 * 1 hồ sơ bệnh nhân đang chờ quản trị viên xác minh trước khi gắn vào tài khoản (quy tắc #3).
 *
 * @param khopHoTen       họ tên tài khoản giống họ tên hồ sơ (không xét dấu, hoa/thường, khoảng trắng thừa)
 * @param khopSoDienThoai SĐT tài khoản trùng SĐT hồ sơ
 */
public record HoSoChoXacMinhResponse(HoSo hoSo, TaiKhoan taiKhoan, boolean khopHoTen, boolean khopSoDienThoai) {

    /** @param soLichHen số lịch hẹn (mọi trạng thái) của hồ sơ: tài khoản sẽ xem được nếu được duyệt */
    public record HoSo(Long id, String hoTen, LocalDate ngaySinh, String cccd, String soDienThoai, long soLichHen) {
    }

    public record TaiKhoan(Long id, String hoTen, String email, String soDienThoai) {
    }

}
