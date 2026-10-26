package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Kết quả sao chép ca (xem trước hoặc đã lưu). */
public record KetQuaSaoChepResponse(boolean daLuu, int soCaNguon, List<CaSaoChep> taoDuoc, List<CaSaoChep> boQua) {

    /** 1 bản sao của 1 ca nguồn. */
    public record CaSaoChep(Long idCaNguon, Long idCaMoi, BacSiTomTatResponse bacSi, PhongKhamTomTatResponse phongKham,
            LocalDate ngay, LocalTime gioBatDau, LocalTime gioKetThuc, String lyDo) {
    }
}
