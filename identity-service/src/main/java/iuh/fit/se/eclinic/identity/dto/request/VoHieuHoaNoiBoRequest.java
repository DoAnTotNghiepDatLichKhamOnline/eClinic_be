package iuh.fit.se.eclinic.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** API nội bộ: catalog-service nhờ vô hiệu hoá tài khoản của bác sĩ ngừng công tác. */
public record VoHieuHoaNoiBoRequest(

        @NotBlank(message = "Phải nhập lý do")
        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        String lyDo) {
}
