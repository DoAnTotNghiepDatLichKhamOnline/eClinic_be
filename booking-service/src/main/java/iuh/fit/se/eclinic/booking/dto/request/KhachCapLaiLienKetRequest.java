package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Khách đã mất link tự xin cấp lại (PAT-03): link chỉ gửi tới email của lịch hẹn, không bao giờ trả trong phản hồi.
 *
 * @param maTraCuu    mã tra cứu ngắn in trên phiếu khám / trong email (ECL-yyyyMMdd-nnnn), không phân biệt hoa thường
 * @param soDienThoai đầy đủ SĐT liên hệ đã nhập lúc đặt lịch (lượt khám có người giám hộ: SĐT người giám hộ)
 */
public record KhachCapLaiLienKetRequest(
        @NotBlank(message = "{kiemTra.maTraCuu.batBuoc}")
        @Pattern(regexp = "(?i)\\s*ECL-\\d{8}-\\d{4}\\s*", message = "{kiemTra.maTraCuu.dinhDang}")
        String maTraCuu,

        @NotBlank(message = "{kiemTra.soDienThoai.batBuoc}")
        @Pattern(regexp = "\\s*0\\d{9}\\s*", message = "{kiemTra.soDienThoai.dinhDang}")
        String soDienThoai) {
}
