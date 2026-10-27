package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Bác sĩ hủy 1 lịch hẹn đã xác nhận của mình. */
public record BacSiHuyLichHenRequest(
        @NotBlank(message = "{kiemTra.lyDoHuy.batBuoc}")
        @Size(max = 500, message = "{kiemTra.lyDoHuy.toiDa}")
        String lyDo) {
}
