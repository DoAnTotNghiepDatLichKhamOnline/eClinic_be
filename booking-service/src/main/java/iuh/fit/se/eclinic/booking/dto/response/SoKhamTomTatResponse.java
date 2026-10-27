package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

/** Phần đầu của sổ khám bệnh ai có link cũng xem được, không có kết quả khám. */
public record SoKhamTomTatResponse(String hoTenChe, Integer namSinh, LocalDate ngayKham, LocalTime gioKham,
        BacSiTomTatResponse bacSi, String tenChuyenKhoa, PhongKhamTomTatResponse phongKham) {
}
