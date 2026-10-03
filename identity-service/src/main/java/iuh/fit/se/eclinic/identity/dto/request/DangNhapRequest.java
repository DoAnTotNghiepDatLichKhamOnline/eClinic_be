package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Đăng nhập bằng email + mật khẩu (AUTH-02). Không dùng @MatKhauHopLe: mật khẩu sai quy tắc cũng chỉ là
 * "sai thông tin đăng nhập" (401), không phải lỗi dữ liệu (400).
 */
public record DangNhapRequest(

        @NotBlank(message = "Email không được để trống")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(max = 1000, message = "Mật khẩu quá dài")
        String matKhau) {

    /** Không in mật khẩu (tránh lộ khi request bị log). */
    @Override
    public String toString() {
        return "DangNhapRequest[email=" + email + "]";
    }

}
