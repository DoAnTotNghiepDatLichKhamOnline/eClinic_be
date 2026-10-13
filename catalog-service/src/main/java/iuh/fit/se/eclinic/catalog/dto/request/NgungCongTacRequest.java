package iuh.fit.se.eclinic.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lý do được gửi cho bác sĩ (email) và cho bệnh nhân có lịch hẹn trên các ca bị hủy (thông báo). */
public record NgungCongTacRequest(

        @NotBlank(message = "{kiemTra.lyDo.batBuoc}")
        @Size(max = 500, message = "{kiemTra.lyDo.toiDa}")
        String lyDo) {
}
