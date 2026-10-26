package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Quản trị viên đổi 1 lịch hẹn sang khung giờ khác thay bệnh nhân. Chọn khung giờ như {@link DoiLichRequest}: gửi
 * {@code idLichLamViec} (chọn bác sĩ) hoặc {@code idChuyenKhoa} (bác sĩ bất kỳ), không gửi cả hai.
 *
 * @param gioBatDauKhung giờ bắt đầu khung 1 giờ muốn đổi sang
 * @param lyDo           bắt buộc; ghi vào nhật ký lịch hẹn (bệnh nhân không đọc được)
 */
public record QuanTriDoiLichRequest(
        Long idLichLamViec,

        Long idChuyenKhoa,

        @NotNull(message = "{kiemTra.khungGio.batBuoc}")
        LocalDateTime gioBatDauKhung,

        @NotBlank(message = "{kiemTra.lyDoDoiLich.batBuoc}")
        @Size(max = 500, message = "{kiemTra.lyDoDoiLich.toiDa}")
        String lyDo) {
}
