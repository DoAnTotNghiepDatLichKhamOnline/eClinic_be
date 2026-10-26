package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Kết quả sao chép ca (xem trước hoặc đã lưu).
 *
 * @param daLuu     false = chỉ xem trước, chưa ca nào được lưu
 * @param soCaNguon số ca còn hoạt động trong khoảng nguồn khớp bộ lọc
 * @param taoDuoc   các bản sao tạo được (đã tạo khi {@code daLuu}), theo ngày, giờ bắt đầu, tên bác sĩ
 * @param boQua     các bản sao không tạo được, kèm lý do, cùng thứ tự
 */
public record KetQuaSaoChepResponse(boolean daLuu, int soCaNguon, List<CaSaoChep> taoDuoc, List<CaSaoChep> boQua) {

    /**
     * 1 bản sao của 1 ca nguồn.
     *
     * @param idCaMoi id ca vừa tạo; null khi xem trước và trong {@code boQua}
     * @param lyDo    lý do bỏ qua; null trong {@code taoDuoc}
     */
    public record CaSaoChep(Long idCaNguon, Long idCaMoi, BacSiTomTatResponse bacSi, PhongKhamTomTatResponse phongKham,
            LocalDate ngay, LocalTime gioBatDau, LocalTime gioKetThuc, String lyDo) {
    }
}
