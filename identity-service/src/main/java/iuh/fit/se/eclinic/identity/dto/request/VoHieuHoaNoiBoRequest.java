package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** API nội bộ: catalog-service nhờ vô hiệu hoá tài khoản của bác sĩ ngừng công tác. */
public record VoHieuHoaNoiBoRequest(

        @NotBlank(message = "{kiemTra.lyDo.batBuoc}")
        @Size(max = 500, message = "{kiemTra.lyDo.toiDa}")
        String lyDo) {
}
