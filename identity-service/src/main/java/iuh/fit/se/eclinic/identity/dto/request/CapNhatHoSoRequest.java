package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cập nhật hồ sơ cá nhân (PUT /api/users/me): chỉ họ tên và số điện thoại của tài khoản.
 *
 * @param soDienThoai không gửi (null) thì giữ nguyên số hiện tại; không xoá được số điện thoại đã có
 */
public record CapNhatHoSoRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
        String hoTen,
        @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0")
        String soDienThoai) {
}
