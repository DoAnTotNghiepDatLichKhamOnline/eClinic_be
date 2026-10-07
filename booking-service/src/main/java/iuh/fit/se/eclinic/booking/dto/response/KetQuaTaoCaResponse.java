package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Kết quả xếp ca.
 *
 * @param daTao các ca vừa tạo, theo ngày
 * @param boQua các ngày của phần lặp lại không tạo được ca, kèm lý do (trùng giờ với ca khác của bác sĩ / phòng khám)
 */
public record KetQuaTaoCaResponse(List<CaLamViecResponse> daTao, List<NgayBoQua> boQua) {

    public record NgayBoQua(LocalDate ngay, String lyDo) {
    }
}
