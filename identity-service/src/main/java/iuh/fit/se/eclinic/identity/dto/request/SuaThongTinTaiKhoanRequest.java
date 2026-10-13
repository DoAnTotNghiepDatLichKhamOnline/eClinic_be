package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** API nội bộ: catalog-service nhờ sửa họ tên, số điện thoại của tài khoản bác sĩ. Số điện thoại trống = bỏ số. */
public record SuaThongTinTaiKhoanRequest(

        @NotBlank(message = "{kiemTra.hoTen.batBuoc}")
        @Size(max = 150, message = "{kiemTra.hoTen.toiDa}")
        String hoTen,

        @Pattern(regexp = "^(0\\d{9})?$", message = "{kiemTra.soDienThoai.dinhDang}")
        String soDienThoai) {
}
