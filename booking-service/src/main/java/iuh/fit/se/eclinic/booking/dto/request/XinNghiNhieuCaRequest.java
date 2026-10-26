package iuh.fit.se.eclinic.booking.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Bác sĩ xin nghỉ nhiều ca 1 lần: mỗi ca 1 yêu cầu XIN_NGHI, cùng lý do và mã nhóm. */
public record XinNghiNhieuCaRequest(
        @NotEmpty @Size(max = 50) List<@NotNull Long> cacIdLichLamViec,
        @NotBlank @Size(max = 1000) String lyDo) {
}
