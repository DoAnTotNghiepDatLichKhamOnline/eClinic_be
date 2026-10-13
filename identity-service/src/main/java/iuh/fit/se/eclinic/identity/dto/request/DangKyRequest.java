package iuh.fit.se.eclinic.identity.dto.request;

import iuh.fit.se.eclinic.identity.validation.MatKhauHopLe;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Bệnh nhân tự đăng ký tài khoản (AUTH-01).
 */
public record DangKyRequest(

        @NotBlank(message = "{kiemTra.hoTen.batBuoc}")
        @Size(max = 150, message = "{kiemTra.hoTen.toiDa}")
        String hoTen,

        @NotBlank(message = "{kiemTra.email.batBuoc}")
        @Email(message = "{kiemTra.email.dinhDang}")
        @Size(max = 255, message = "{kiemTra.email.toiDa}")
        String email,

        @MatKhauHopLe
        String matKhau,

        @NotBlank(message = "{kiemTra.soDienThoai.batBuoc}")
        @Pattern(regexp = "^0\\d{9}$", message = "{kiemTra.soDienThoai.dinhDang}")
        String soDienThoai,

        /**
         * Không bắt buộc. Có thì hồ sơ bệnh nhân đã tạo khi đặt lịch như khách bằng số CCCD này được liên kết vào tài
         * khoản sau khi đăng nhập (quy tắc #3).
         */
        @Pattern(regexp = "^(\\d{12})?$", message = "{kiemTra.cccd.dinhDang}")
        String cccd) {

    /** Không in mật khẩu, số CCCD (tránh lộ khi request bị log). */
    @Override
    public String toString() {
        return "DangKyRequest[email=" + email + "]";
    }

}
