package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/**
 * Đổi 1 lịch hẹn sang khung giờ khác (đổi lịch = hủy lịch cũ + tạo lịch mới, quy tắc #10). Chọn khung giờ như lúc đặt
 * lịch: gửi {@code idLichLamViec} (chọn bác sĩ) hoặc {@code idChuyenKhoa} (bác sĩ bất kỳ), không gửi cả hai. Thông tin
 * người khám lấy từ lịch hẹn cũ, không gửi lại.
 *
 * @param gioBatDauKhung giờ bắt đầu khung 1 giờ muốn đổi sang
 * @param soDienThoai    chỉ dùng (và bắt buộc) khi đổi bằng link phiếu khám: đúng SĐT liên hệ đã nhập lúc đặt lịch
 */
public record DoiLichRequest(
        Long idLichLamViec,

        Long idChuyenKhoa,

        @NotNull(message = "Chưa chọn khung giờ khám")
        LocalDateTime gioBatDauKhung,

        String soDienThoai) {
}
