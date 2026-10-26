package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

import iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec;

/**
 * 1 ca làm việc trên lịch của bác sĩ / lịch toàn viện của quản trị viên. Có cả ca đã huỷ ({@code trangThai}).
 *
 * @param tongSoLuot     số lượt khám của ca, không tính lượt đã huỷ
 * @param soLuotDaDat    số lượt đã có người đặt
 * @param soLuotConTrong số lượt còn trống (kể cả lượt đã qua giờ)
 */
public record CaLamViecResponse(
        Long idLichLamViec,
        LocalDate ngay,
        LocalTime gioBatDau,
        LocalTime gioKetThuc,
        Integer soLuotToiDaMoiGio,
        Integer thoiLuongLuotPhut,
        TrangThaiLichLamViec trangThai,
        BacSiTomTatResponse bacSi,
        String tenChuyenKhoa,
        PhongKhamTomTatResponse phongKham,
        long tongSoLuot,
        long soLuotDaDat,
        long soLuotConTrong) {
}
