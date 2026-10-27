package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Sửa 1 ca làm việc: gửi đủ cả 5 giá trị, giá trị không đổi thì gửi lại như cũ. */
public record SuaCaRequest(
        @NotNull Long idPhongKham,
        @NotNull LocalTime gioBatDau,
        @NotNull LocalTime gioKetThuc,
        @NotNull @Min(1) @Max(60) Integer soLuotToiDaMoiGio,
        @NotNull @Min(1) @Max(60) Integer thoiLuongLuotPhut) {
}
