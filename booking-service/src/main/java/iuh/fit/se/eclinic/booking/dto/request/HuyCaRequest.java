package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Hủy 1 ca làm việc; lý do được ghi vào thông báo gửi bác sĩ và bệnh nhân. */
public record HuyCaRequest(@NotBlank @Size(max = 500) String lyDo) {
}
