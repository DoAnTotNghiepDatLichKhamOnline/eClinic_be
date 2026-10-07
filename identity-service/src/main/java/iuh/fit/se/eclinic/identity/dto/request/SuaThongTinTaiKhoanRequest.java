package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** API nội bộ: catalog-service nhờ sửa họ tên, số điện thoại của tài khoản bác sĩ. Số điện thoại trống = bỏ số. */
public record SuaThongTinTaiKhoanRequest(

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
        String hoTen,

        @Pattern(regexp = "^(0\\d{9})?$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0")
        String soDienThoai) {
}
