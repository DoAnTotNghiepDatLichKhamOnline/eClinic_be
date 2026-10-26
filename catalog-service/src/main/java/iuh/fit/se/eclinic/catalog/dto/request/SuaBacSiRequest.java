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

        @NotBlank(message = "{kiemTra.hoTen.batBuoc}")
        @Size(max = 150, message = "{kiemTra.hoTen.toiDa}")
        String hoTen,

        @Pattern(regexp = "^(0\\d{9})?$", message = "{kiemTra.soDienThoai.dinhDang}")
        String soDienThoai,

        @NotNull(message = "{kiemTra.chuyenKhoa.batBuoc}")
        Long idChuyenKhoa,

        @Size(max = 50, message = "{kiemTra.soGiayPhep.toiDa}")
        String soGiayPhep) {
}
