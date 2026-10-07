package iuh.fit.se.eclinic.catalog.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Thêm bác sĩ: tạo tài khoản đăng nhập (identity-service) và hồ sơ bác sĩ trong 1 bước. Các mục giới thiệu dài (tiểu sử,
 * quá trình đào tạo...) và ảnh sửa sau bằng API hồ sơ.
 */
public record ThemBacSiRequest(

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
        String hoTen,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        @Pattern(regexp = "^(0\\d{9})?$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0")
        String soDienThoai,

        @NotNull(message = "Phải chọn chuyên khoa")
        Long idChuyenKhoa,

        @Size(max = 50, message = "Số giấy phép tối đa 50 ký tự")
        String soGiayPhep,

        @Size(max = 100, message = "Học vị tối đa 100 ký tự")
        String hocVi,

        @Size(max = 150, message = "Chức vụ tối đa 150 ký tự")
        String chucVu,

        @Min(value = 0, message = "Số năm kinh nghiệm không được âm")
        @Max(value = 80, message = "Số năm kinh nghiệm tối đa 80")
        Integer soNamKinhNghiem) {
}
