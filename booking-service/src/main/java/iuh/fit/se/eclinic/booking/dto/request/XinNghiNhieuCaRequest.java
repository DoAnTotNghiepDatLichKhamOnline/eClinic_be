package iuh.fit.se.eclinic.booking.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Bác sĩ xin nghỉ nhiều ca của mình trong 1 lần (vd nghỉ 1 tuần): mỗi ca thành 1 yêu cầu XIN_NGHI, cùng lý do, chung 1
 * mã nhóm.
 *
 * @param cacIdLichLamViec các ca của tôi; id trùng chỉ tính 1 lần
 */
public record XinNghiNhieuCaRequest(
        @NotEmpty @Size(max = 50) List<@NotNull Long> cacIdLichLamViec,
        @NotBlank @Size(max = 1000) String lyDo) {
}
