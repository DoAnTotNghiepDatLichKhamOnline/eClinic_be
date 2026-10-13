package iuh.fit.se.eclinic.identity.dto.request;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import jakarta.validation.constraints.NotNull;

/**
 * Đổi ngôn ngữ ưa dùng của tài khoản (PUT /api/users/me/language).
 *
 * @param ngonNgu VI hoặc EN
 */
public record DoiNgonNguRequest(
        @NotNull(message = "{kiemTra.ngonNgu.batBuoc}")
        NgonNgu ngonNgu) {
}
