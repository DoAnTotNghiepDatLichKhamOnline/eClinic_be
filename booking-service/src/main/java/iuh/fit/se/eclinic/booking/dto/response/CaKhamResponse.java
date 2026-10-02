package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 1 ca làm việc đặt lịch được trong ngày, kèm các khung 1 giờ còn kịp đặt.
 * Khi đặt lịch, khung giờ được xác định bằng {@code idLichLamViec} + {@code khungGio[].gioBatDau}.
 *
 * @param soLuotToiDaMoiGio N: số lượt tối đa của 1 khung 1 giờ
 * @param thoiLuongLuotPhut t: số phút của 1 lượt khám
 */
public record CaKhamResponse(
        Long idLichLamViec,
        LocalDate ngay,
        LocalTime gioBatDau,
        LocalTime gioKetThuc,
        Integer soLuotToiDaMoiGio,
        Integer thoiLuongLuotPhut,
        BacSiTomTatResponse bacSi,
        PhongKhamTomTatResponse phongKham,
        List<KhungGioResponse> khungGio) {
}
