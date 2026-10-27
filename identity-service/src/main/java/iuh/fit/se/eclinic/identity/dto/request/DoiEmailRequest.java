package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Yêu cầu đổi email đăng nhập của người đang đăng nhập. */
public record DoiEmailRequest(

        @NotBlank(message = "{kiemTra.emailMoi.batBuoc}")
        @Email(message = "{kiemTra.emailMoi.dinhDang}")
        @Size(max = 255, message = "{kiemTra.emailMoi.quaDai}")
        String emailMoi,

        @NotBlank(message = "{kiemTra.matKhauHienTai.batBuoc}")
        @Size(max = 1000, message = "{kiemTra.matKhau.quaDai}")
        String matKhauHienTai) {

    /** Không in mật khẩu (tránh lộ khi request bị log). */
    @Override
    public String toString() {
        return "DoiEmailRequest[***]";
    }

}
