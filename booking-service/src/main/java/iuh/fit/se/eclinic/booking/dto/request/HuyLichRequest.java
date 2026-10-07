package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Hủy 1 lịch hẹn. Dùng cho cả bệnh nhân đã đăng nhập và người cầm link phiếu khám.
 *
 * @param lyDo        không bắt buộc; bác sĩ và quản trị viên đọc được
 * @param soDienThoai chỉ dùng (và bắt buộc) khi hủy bằng link phiếu khám: đúng SĐT liên hệ đã nhập lúc đặt lịch
 */
public record HuyLichRequest(
        @Size(max = 500, message = "Lý do hủy tối đa 500 ký tự")
        String lyDo,

        String soDienThoai) {
}
