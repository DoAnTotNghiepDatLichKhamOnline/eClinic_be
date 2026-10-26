package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Quản trị viên hủy 1 lịch hẹn thay bệnh nhân / bệnh viện.
 *
 * @param lyDo bắt buộc; bệnh nhân đọc được trên phiếu khám và trong thông báo
 */
public record QuanTriHuyLichHenRequest(
        @NotBlank(message = "{kiemTra.lyDoHuy.batBuoc}")
        @Size(max = 500, message = "{kiemTra.lyDoHuy.toiDa}")
        String lyDo) {
}
