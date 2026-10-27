package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Kết quả nhập lịch từ Excel (xem trước hoặc đã lưu). */
public record KetQuaNhapExcelResponse(boolean daLuu, int soDong, List<DongHopLe> hopLe, List<LoiDong> loi) {

    /** @param idCaMoi id ca vừa tạo; null khi chưa lưu */
    public record DongHopLe(int dong, Long idCaMoi, BacSiTomTatResponse bacSi, PhongKhamTomTatResponse phongKham,
            LocalDate ngay, LocalTime gioBatDau, LocalTime gioKetThuc) {
    }

    public record LoiDong(int dong, String cot, String lyDo) {
    }
}
