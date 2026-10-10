package iuh.fit.se.eclinic.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Sửa thông tin cơ bản của bác sĩ (ghi đè cả 4 trường; số điện thoại / số giấy phép bỏ trống là xoá). Email đăng nhập
 * không sửa ở đây: chủ tài khoản tự đổi bằng luồng đổi email.
 */
public record SuaBacSiRequest(

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
        String hoTen,

        @Pattern(regexp = "^(0\\d{9})?$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0")
        String soDienThoai,

        @NotNull(message = "Phải chọn chuyên khoa")
        Long idChuyenKhoa,

        @Size(max = 50, message = "Số giấy phép tối đa 50 ký tự")
        String soGiayPhep) {
}
