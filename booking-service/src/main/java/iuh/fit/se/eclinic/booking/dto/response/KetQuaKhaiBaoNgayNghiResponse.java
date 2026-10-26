package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Kết quả (hoặc xem trước) khai báo ngày bệnh viện nghỉ. */
public record KetQuaKhaiBaoNgayNghiResponse(
        boolean daLuu,
        List<NgayNghiQuanTriResponse> ngayMoi,
        List<LocalDate> ngayDaNghi,
        List<CaBiHuy> caBiHuy,
        int soLichHenCanDoi) {

    /** @param soLichHenCanDoi số lịch hẹn còn hiệu lực của ca này */
    public record CaBiHuy(
            Long idLichLamViec,
            LocalDate ngay,
            LocalTime gioBatDau,
            LocalTime gioKetThuc,
            BacSiTomTatResponse bacSi,
            PhongKhamTomTatResponse phongKham,
            int soLichHenCanDoi) {
    }
}
