package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Xếp 1 ca làm việc (SCHED-01), có thể lặp lại theo tuần.
 *
 * @param ngay              ngày của ca (ca đầu tiên khi có {@code lapLai})
 * @param soLuotToiDaMoiGio N: số lượt khám tối đa trong 1 khung 1 giờ
 * @param thoiLuongLuotPhut t: số phút của 1 lượt khám; N x t &lt;= 60
 * @param lapLai            null = chỉ tạo 1 ca
 */
public record TaoCaRequest(
        @NotNull Long idBacSi,
        @NotNull Long idPhongKham,
        @NotNull LocalDate ngay,
        @NotNull LocalTime gioBatDau,
        @NotNull LocalTime gioKetThuc,
        @NotNull @Min(1) @Max(60) Integer soLuotToiDaMoiGio,
        @NotNull @Min(1) @Max(60) Integer thoiLuongLuotPhut,
        @Valid LapLai lapLai) {

    /**
     * Tạo thêm ca cùng giờ vào các ngày sau {@code ngay}.
     *
     * @param cacThu  các thứ trong tuần có ca, theo ISO: 1 = thứ Hai ... 7 = Chủ nhật
     * @param denNgay ngày cuối cùng được tạo ca (tính cả ngày này)
     */
    public record LapLai(
            @NotEmpty List<@NotNull @Min(1) @Max(7) Integer> cacThu,
            @NotNull LocalDate denNgay) {
    }
}
