package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Sao chép các ca của khoảng [tuNgay, denNgay] sang từ {@code ngayDich}, giữ nguyên thứ trong tuần. */
public record SaoChepCaRequest(
        @NotNull LocalDate tuNgay,
        @NotNull LocalDate denNgay,
        @NotNull LocalDate ngayDich,
        @NotNull @Min(1) @Max(12) Integer soLan,
        Long idChuyenKhoa,
        List<@NotNull Long> cacIdBacSi) {
}
