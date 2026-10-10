package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** API nội bộ: catalog-service nhờ tạo tài khoản cho bác sĩ mới. */
public record TaoTaiKhoanBacSiRequest(

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
        String hoTen,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        @Pattern(regexp = "^(0\\d{9})?$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0")
        String soDienThoai) {
}
