package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Bệnh nhân gửi / sửa đánh giá 1 lượt khám đã hoàn thành. */
public record DanhGiaRequest(
        @NotNull(message = "Chưa chọn số sao")
        @Min(value = 1, message = "Số sao từ 1 đến 5")
        @Max(value = 5, message = "Số sao từ 1 đến 5")
        Integer soSao,
        @Size(max = 1000, message = "Nhận xét tối đa 1000 ký tự")
        String nhanXet) {
}
