package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.Size;

/** Refresh token gửi trong body khi làm mới phiên / đăng xuất. */
public record PhienDangNhapRequest(

        @Size(max = 100, message = "{kiemTra.refreshToken.khongHopLe}")
        String refreshToken) {

    /** Không in token (tránh lộ khi request bị log). */
    @Override
    public String toString() {
        return "PhienDangNhapRequest[***]";
    }

}
