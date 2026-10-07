package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Bác sĩ từ chối 1 lịch hẹn đang chờ xác nhận.
 *
 * @param lyDo lý do từ chối, bệnh nhân đọc được trên phiếu khám và trong "lịch hẹn của tôi"
 */
public record TuChoiLichHenRequest(
        @NotBlank(message = "Lý do từ chối không được để trống")
        @Size(max = 500, message = "Lý do từ chối tối đa 500 ký tự")
        String lyDo) {
}
