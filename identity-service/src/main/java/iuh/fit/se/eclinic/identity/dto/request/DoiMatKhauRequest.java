package iuh.fit.se.eclinic.identity.dto.request;

import iuh.fit.se.eclinic.identity.validation.MatKhauHopLe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Đổi mật khẩu khi đang đăng nhập. */
public record DoiMatKhauRequest(

        @NotBlank(message = "{kiemTra.matKhauHienTai.batBuoc}")
        @Size(max = 1000, message = "{kiemTra.matKhau.quaDai}")
        String matKhauCu,

        @MatKhauHopLe
        String matKhauMoi) {

    /** Không in mật khẩu (tránh lộ khi request bị log). */
    @Override
    public String toString() {
        return "DoiMatKhauRequest[***]";
    }

}
