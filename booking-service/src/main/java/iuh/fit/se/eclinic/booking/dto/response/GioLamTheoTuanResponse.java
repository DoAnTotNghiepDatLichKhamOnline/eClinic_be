package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.util.List;

/** Giờ làm của các bác sĩ theo tuần (thứ Hai - Chủ nhật), chỉ cộng ca còn hoạt động. */
public record GioLamTheoTuanResponse(long gioiHanPhut, List<GioLamCuaBacSi> bacSi) {

    /** @param cacTuan mọi tuần phủ khoảng ngày đã hỏi, theo thứ tự thời gian */
    public record GioLamCuaBacSi(BacSiTomTatResponse bacSi, String tenChuyenKhoa, List<Tuan> cacTuan) {
    }

    public record Tuan(LocalDate tuNgay, LocalDate denNgay, int soCa, long soPhut, long soPhutConLai) {
    }
}
