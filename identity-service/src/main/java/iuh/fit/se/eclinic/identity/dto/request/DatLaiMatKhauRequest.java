package iuh.fit.se.eclinic.identity.dto.request;

import iuh.fit.se.eclinic.identity.validation.MatKhauHopLe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Đặt mật khẩu mới bằng token trong liên kết email. */
public record DatLaiMatKhauRequest(

        @NotBlank(message = "{kiemTra.token.batBuoc}")
        @Size(max = 100, message = "{kiemTra.token.khongHopLe}")
        String token,

        @MatKhauHopLe
        String matKhauMoi) {

    /** Không in token và mật khẩu (tránh lộ khi request bị log). */
    @Override
    public String toString() {
        return "DatLaiMatKhauRequest[***]";
    }

}
