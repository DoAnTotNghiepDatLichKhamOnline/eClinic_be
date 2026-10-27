package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Xem đầy đủ sổ khám bệnh: gửi trong body (không đặt trên URL) để 4 số không nằm trong log truy cập. */
public record XemSoKhamRequest(
        @NotBlank(message = "{kiemTra.bonSoCuoi.batBuoc}")
        @Pattern(regexp = "\\d{4}", message = "{kiemTra.bonSoCuoi.dinhDang}")
        String bonSoCuoi) {
}
