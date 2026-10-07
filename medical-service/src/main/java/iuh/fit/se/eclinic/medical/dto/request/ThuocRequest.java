package iuh.fit.se.eclinic.medical.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu thêm / sửa 1 thuốc trong danh mục (quản trị viên).
 *
 * @param donVi viên, ml, gói...
 */
public record ThuocRequest(
        @NotBlank(message = "Tên thuốc không được để trống")
        @Size(max = 255, message = "Tên thuốc tối đa 255 ký tự")
        String tenThuoc,
        @Size(max = 30, message = "Đơn vị tối đa 30 ký tự")
        String donVi,
        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
        String moTa) {
}
