package iuh.fit.se.eclinic.identity.dto.response;

import java.time.LocalDateTime;

/** Một phiên đăng nhập (1 thiết bị) đang còn hiệu lực của người dùng. */
public record PhienDangNhapResponse(
        String id,
        String thietBi,
        LocalDateTime dangNhapLuc,
        LocalDateTime hoatDongLuc,
        LocalDateTime hetHanLuc,
        boolean hienTai) {
}
