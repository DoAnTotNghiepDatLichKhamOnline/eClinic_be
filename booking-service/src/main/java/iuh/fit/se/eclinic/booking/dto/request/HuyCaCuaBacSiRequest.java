package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** API nội bộ: catalog-service nhờ hủy mọi ca sắp tới của bác sĩ ngừng công tác. */
public record HuyCaCuaBacSiRequest(

        @NotNull(message = "Thiếu tài khoản quản trị viên thực hiện")
        Long idTaiKhoanQuanTri,

        @NotBlank(message = "Phải nhập lý do")
        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        String lyDo) {
}
