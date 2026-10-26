package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Sửa 1 ca làm việc (SCHED-05): gửi đủ cả 5 giá trị, giá trị không đổi thì gửi lại như cũ. Bác sĩ và ngày của ca không
 * sửa được (hủy ca rồi xếp ca mới).
 */
public record SuaCaRequest(
        @NotNull Long idPhongKham,
        @NotNull LocalTime gioBatDau,
        @NotNull LocalTime gioKetThuc,
        @NotNull @Min(1) @Max(60) Integer soLuotToiDaMoiGio,
        @NotNull @Min(1) @Max(60) Integer thoiLuongLuotPhut) {
}
