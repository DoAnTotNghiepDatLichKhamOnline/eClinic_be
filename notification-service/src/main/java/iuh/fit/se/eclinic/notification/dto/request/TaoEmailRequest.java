package iuh.fit.se.eclinic.notification.dto.request;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 1 email do service khác gửi sang (API nội bộ). Không nhận câu soạn sẵn: chỉ nhận mẫu có trong {@code MauEmail}.
 *
 * @param maNguon   mã duy nhất của sự kiện ở service nguồn, dạng {@code <service>:<id>}; gửi lại cùng mã thì không có
 *                  email thứ hai
 * @param nguoiNhan địa chỉ nhận (người nhận có thể không có tài khoản)
 * @param ngonNgu   ngôn ngữ của email
 * @param khoaMau   mẫu email, vd {@code email.datLich}
 * @param thamSo    tham số của mẫu, mảng JSON (xem {@code ThamSoThongBao}), theo thứ tự mẫu quy định. Tối đa 20000 ký
 *                  tự: email lịch làm việc liệt kê tới 100 ca (~100 ký tự / ca); cột tham_so là TEXT (65535 byte)
 * @param idLichHen lịch hẹn liên quan, có thể null
 */
public record TaoEmailRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[a-z-]+:[0-9]+", message = "{kiemTra.maNguon.dinhDang}")
        String maNguon,
        @NotBlank @Email @Size(max = 255) String nguoiNhan,
        @NotNull NgonNgu ngonNgu,
        @NotBlank @Size(max = 80) String khoaMau,
        @Size(max = 20000) String thamSo,
        Long idLichHen) {
}
