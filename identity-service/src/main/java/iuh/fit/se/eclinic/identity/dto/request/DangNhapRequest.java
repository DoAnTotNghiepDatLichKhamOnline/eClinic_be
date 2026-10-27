package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Đăng nhập bằng email + mật khẩu. */
public record DangNhapRequest(

        @NotBlank(message = "{kiemTra.email.batBuoc}")
        @Size(max = 255, message = "{kiemTra.email.toiDa}")
        String email,

        @NotBlank(message = "{kiemTra.matKhau.batBuoc}")
        @Size(max = 1000, message = "{kiemTra.matKhau.quaDai}")
        String matKhau) {

    /** Không in mật khẩu (tránh lộ khi request bị log). */
    @Override
    public String toString() {
        return "DangNhapRequest[email=" + email + "]";
    }

}
