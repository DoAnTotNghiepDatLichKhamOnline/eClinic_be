package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Bệnh nhân gửi / sửa đánh giá 1 lượt khám đã hoàn thành. */
public record DanhGiaRequest(
        @NotNull(message = "{kiemTra.soSao.batBuoc}")
        @Min(value = 1, message = "{kiemTra.soSao.khoang}")
        @Max(value = 5, message = "{kiemTra.soSao.khoang}")
        Integer soSao,
        @Size(max = 1000, message = "{kiemTra.nhanXet.toiDa}")
        String nhanXet) {
}
