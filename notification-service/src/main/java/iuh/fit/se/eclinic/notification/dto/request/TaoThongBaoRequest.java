package iuh.fit.se.eclinic.notification.dto.request;

import iuh.fit.se.eclinic.common.enums.LoaiThongBao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 1 thông báo do service khác gửi sang (API nội bộ).
 *
 * @param maNguon    mã duy nhất của sự kiện ở service nguồn, dạng {@code <service>:<id>} (vd {@code booking:15}); gửi lại
 *                   cùng mã thì không tạo thông báo thứ hai
 * @param idTaiKhoan tài khoản nhận
 * @param idLichHen  lịch hẹn liên quan, có thể null
 * @param idYeuCau   yêu cầu đổi ca / xin nghỉ liên quan, có thể null
 */
public record TaoThongBaoRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[a-z-]+:[0-9]+", message = "phải có dạng <service>:<id>")
        String maNguon,
        @NotNull LoaiThongBao loai,
        @NotNull Long idTaiKhoan,
        Long idLichHen,
        Long idYeuCau,
        @NotBlank @Size(max = 2000) String noiDung) {
}
