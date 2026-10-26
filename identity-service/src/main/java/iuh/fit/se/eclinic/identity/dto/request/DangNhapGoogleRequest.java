package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Đăng nhập bằng Google: ID token (credential) mà Google Identity Services trả cho frontend.
 */
public record DangNhapGoogleRequest(

        @NotBlank(message = "{kiemTra.idTokenGoogle.batBuoc}")
        @Size(max = 4096, message = "{kiemTra.idTokenGoogle.khongHopLe}")
        String idToken) {

    /** Không in token (tránh lộ khi request bị log). */
    @Override
    public String toString() {
        return "DangNhapGoogleRequest[***]";
    }

}
