package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.util.List;

/** Kết quả xếp ca. */
public record KetQuaTaoCaResponse(List<CaLamViecResponse> daTao, List<NgayBoQua> boQua) {

    public record NgayBoQua(LocalDate ngay, String lyDo) {
    }
}
