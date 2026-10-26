package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Xếp 1 ca làm việc, có thể lặp lại theo tuần. */
public record TaoCaRequest(
        @NotNull Long idBacSi,
        @NotNull Long idPhongKham,
        @NotNull LocalDate ngay,
        @NotNull LocalTime gioBatDau,
        @NotNull LocalTime gioKetThuc,
        @NotNull @Min(1) @Max(60) Integer soLuotToiDaMoiGio,
        @NotNull @Min(1) @Max(60) Integer thoiLuongLuotPhut,
        @Valid LapLai lapLai) {

    /** Tạo thêm ca cùng giờ vào các ngày sau {@code ngay}. */
    public record LapLai(
            @NotEmpty List<@NotNull @Min(1) @Max(7) Integer> cacThu,
            @NotNull LocalDate denNgay) {
    }
}
