package iuh.fit.se.eclinic.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu thêm / sửa phòng khám.
 *
 * @param tang vd "Tầng 2"; có thể để trống
 */
public record PhongKhamRequest(
        @NotBlank(message = "Tên phòng không được để trống")
        @Size(max = 100, message = "Tên phòng tối đa 100 ký tự")
        String tenPhong,
        @Size(max = 20, message = "Tầng tối đa 20 ký tự")
        String tang,
        @NotNull(message = "Chưa chọn chuyên khoa")
        Long idChuyenKhoa) {
}
