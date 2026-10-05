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

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
        String hoTen,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        @MatKhauHopLe
        String matKhau,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0")
        String soDienThoai,

        /**
         * Không bắt buộc. Có thì hồ sơ bệnh nhân đã tạo khi đặt lịch như khách bằng số CCCD này được liên kết vào tài
         * khoản sau khi đăng nhập (quy tắc #3).
         */
        @Pattern(regexp = "^(\\d{12})?$", message = "Số CCCD gồm đúng 12 chữ số")
        String cccd) {

    /** Không in mật khẩu, số CCCD (tránh lộ khi request bị log). */
    @Override
    public String toString() {
        return "DangKyRequest[email=" + email + "]";
    }

}
