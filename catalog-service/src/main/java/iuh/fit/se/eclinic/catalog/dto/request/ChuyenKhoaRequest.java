package iuh.fit.se.eclinic.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu thêm / sửa chuyên khoa (ADM-01).
 */
public record ChuyenKhoaRequest(

        @NotBlank(message = "Tên chuyên khoa không được để trống")
        @Size(max = 150, message = "Tên chuyên khoa tối đa 150 ký tự")
        String tenChuyenKhoa,

        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
        String moTa) {
}
