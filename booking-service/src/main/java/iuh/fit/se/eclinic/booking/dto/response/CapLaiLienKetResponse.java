package iuh.fit.se.eclinic.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import iuh.fit.se.eclinic.common.enums.CachCapLai;

/** Kết quả cấp lại liên kết, luôn của lịch hẹn hiện hành trong chuỗi đổi lịch. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CapLaiLienKetResponse(
        Long idLichHen,
        String maTraCuu,
        CachCapLai cachGui,
        String emailDaGui,
        String maPhieuKham,
        String lienKetPhieuKham,
        String maSoKham,
        String lienKetSoKham) {
}
