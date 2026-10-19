package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Giờ làm của các bác sĩ theo từng tuần thứ Hai - Chủ nhật (SCHED-05): chỉ cộng ca còn hoạt động, kể cả ca đã qua.
 *
 * @param gioiHanPhut giới hạn mỗi tuần (phút), theo cấu hình app.lich-lam-viec.so-gio-toi-da-moi-tuan
 * @param bacSi       bác sĩ đang công tác khớp bộ lọc (kể cả chưa có ca nào), cộng bác sĩ khác còn ca trong khoảng
 */
public record GioLamTheoTuanResponse(long gioiHanPhut, List<GioLamCuaBacSi> bacSi) {

    /** @param cacTuan mọi tuần phủ khoảng ngày đã hỏi, theo thứ tự thời gian */
    public record GioLamCuaBacSi(BacSiTomTatResponse bacSi, String tenChuyenKhoa, List<Tuan> cacTuan) {
    }

    /**
     * @param tuNgay       thứ Hai
     * @param denNgay      Chủ nhật
     * @param soPhutConLai giới hạn trừ số phút đã xếp, không âm (tuần đã vượt giới hạn trả 0)
     */
    public record Tuan(LocalDate tuNgay, LocalDate denNgay, int soCa, long soPhut, long soPhutConLai) {
    }
}
