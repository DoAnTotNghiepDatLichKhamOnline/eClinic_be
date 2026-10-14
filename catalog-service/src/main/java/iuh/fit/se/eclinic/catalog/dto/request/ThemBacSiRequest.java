package iuh.fit.se.eclinic.catalog.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Thêm bác sĩ: tạo tài khoản đăng nhập (identity-service) và hồ sơ bác sĩ trong 1 bước. Các mục giới thiệu dài (tiểu sử,
 * quá trình đào tạo...) và ảnh sửa sau bằng API hồ sơ.
 * <p>
 * {@code namBatDauHanhNghe}: số năm kinh nghiệm suy ra từ đây; không được sau năm nay (kiểm tra ở service).
 */
public record ThemBacSiRequest(

        @NotBlank(message = "{kiemTra.hoTen.batBuoc}")
        @Size(max = 150, message = "{kiemTra.hoTen.toiDa}")
        String hoTen,

        @NotBlank(message = "{kiemTra.email.batBuoc}")
        @Email(message = "{kiemTra.email.dinhDang}")
        @Size(max = 255, message = "{kiemTra.email.toiDa}")
        String email,

        @Pattern(regexp = "^(0\\d{9})?$", message = "{kiemTra.soDienThoai.dinhDang}")
        String soDienThoai,

        @NotNull(message = "{kiemTra.chuyenKhoa.batBuoc}")
        Long idChuyenKhoa,

        @Size(max = 50, message = "{kiemTra.soGiayPhep.toiDa}")
        String soGiayPhep,

        @Size(max = 100, message = "{kiemTra.hocVi.toiDa}")
        String hocVi,

        @Size(max = 150, message = "{kiemTra.chucVu.toiDa}")
        String chucVu,

        @Min(value = 1950, message = "{kiemTra.namBatDauHanhNghe.toiThieu}")
        Integer namBatDauHanhNghe) {
}
