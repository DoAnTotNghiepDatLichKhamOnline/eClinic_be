package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Hủy 1 ca làm việc (SCHED-06). Lý do được ghi vào thông báo gửi bác sĩ và các bệnh nhân đã đặt lịch trong ca. */
public record HuyCaRequest(@NotBlank @Size(max = 500) String lyDo) {
}
