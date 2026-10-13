package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** API nội bộ: catalog-service nhờ hủy mọi ca sắp tới của bác sĩ ngừng công tác. */
public record HuyCaCuaBacSiRequest(

        @NotNull(message = "{kiemTra.taiKhoanQuanTri.batBuoc}")
        Long idTaiKhoanQuanTri,

        @NotBlank(message = "{kiemTra.lyDo.batBuoc}")
        @Size(max = 500, message = "{kiemTra.lyDo.toiDa}")
        String lyDo) {
}
