package iuh.fit.se.eclinic.notification.dto.request;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 1 email do service khác gửi sang (API nội bộ). */
public record TaoEmailRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[a-z-]+:[0-9]+", message = "{kiemTra.maNguon.dinhDang}")
        String maNguon,
        @NotBlank @Email @Size(max = 255) String nguoiNhan,
        @NotNull NgonNgu ngonNgu,
        @NotBlank @Size(max = 80) String khoaMau,
        @Size(max = 20000) String thamSo,
        Long idLichHen,
        @Size(max = 20000) String tepLich) {
}
