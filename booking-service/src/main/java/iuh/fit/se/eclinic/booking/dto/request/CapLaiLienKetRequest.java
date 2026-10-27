package iuh.fit.se.eclinic.booking.dto.request;

import iuh.fit.se.eclinic.common.enums.CachCapLai;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Nhân viên cấp lại liên kết phiếu khám cho người đã mất link. */
public record CapLaiLienKetRequest(
        @NotBlank(message = "{kiemTra.cccd.batBuoc}")
        @Pattern(regexp = "\\d{12}", message = "{kiemTra.cccd.dinhDang}")
        String cccd,

        @NotNull(message = "{kiemTra.cachGui.batBuoc}")
        CachCapLai cachGui) {
}
