package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Khách tự xin cấp lại liên kết: link chỉ gửi tới email của lịch hẹn, không trả trong phản hồi. */
public record KhachCapLaiLienKetRequest(
        @NotBlank(message = "{kiemTra.maTraCuu.batBuoc}")
        @Pattern(regexp = "(?i)\\s*ECL-\\d{8}-\\d{4}\\s*", message = "{kiemTra.maTraCuu.dinhDang}")
        String maTraCuu,

        @NotBlank(message = "{kiemTra.soDienThoai.batBuoc}")
        @Pattern(regexp = "\\s*0\\d{9}\\s*", message = "{kiemTra.soDienThoai.dinhDang}")
        String soDienThoai) {
}
