package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Bác sĩ hủy 1 lịch hẹn đã xác nhận của mình.
 *
 * @param lyDo lý do hủy, bệnh nhân đọc được trên phiếu khám và trong "lịch hẹn của tôi"
 */
public record BacSiHuyLichHenRequest(
        @NotBlank(message = "{kiemTra.lyDoHuy.batBuoc}")
        @Size(max = 500, message = "{kiemTra.lyDoHuy.toiDa}")
        String lyDo) {
}
