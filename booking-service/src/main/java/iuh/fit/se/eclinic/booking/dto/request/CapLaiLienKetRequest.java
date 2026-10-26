package iuh.fit.se.eclinic.booking.dto.request;

import iuh.fit.se.eclinic.common.enums.CachCapLai;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Nhân viên (quản trị viên, bác sĩ) cấp lại liên kết phiếu khám cho người đến quầy / gọi điện đã mất link (PAT-03).
 *
 * @param cccd    số CCCD trên giấy tờ người đó xuất trình: của bệnh nhân, hoặc của người giám hộ ghi trên lịch hẹn
 * @param cachGui EMAIL: gửi tới email của lịch hẹn; MAN_HINH: trả mã và link để mở mã QR trên màn hình
 */
public record CapLaiLienKetRequest(
        @NotBlank(message = "{kiemTra.cccd.batBuoc}")
        @Pattern(regexp = "\\d{12}", message = "{kiemTra.cccd.dinhDang}")
        String cccd,

        @NotNull(message = "{kiemTra.cachGui.batBuoc}")
        CachCapLai cachGui) {
}
