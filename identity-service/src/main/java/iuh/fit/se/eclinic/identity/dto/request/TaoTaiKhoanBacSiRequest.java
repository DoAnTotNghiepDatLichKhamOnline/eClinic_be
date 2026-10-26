package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** API nội bộ: catalog-service nhờ tạo tài khoản cho bác sĩ mới. */
public record TaoTaiKhoanBacSiRequest(

        @NotBlank(message = "{kiemTra.hoTen.batBuoc}")
        @Size(max = 150, message = "{kiemTra.hoTen.toiDa}")
        String hoTen,

        @NotBlank(message = "{kiemTra.email.batBuoc}")
        @Email(message = "{kiemTra.email.dinhDang}")
        @Size(max = 255, message = "{kiemTra.email.toiDa}")
        String email,

        @Pattern(regexp = "^(0\\d{9})?$", message = "{kiemTra.soDienThoai.dinhDang}")
        String soDienThoai) {
}
