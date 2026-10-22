package iuh.fit.se.eclinic.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import iuh.fit.se.eclinic.common.enums.CachCapLai;

/**
 * Kết quả nhân viên cấp lại liên kết (PAT-03), luôn của lịch hẹn hiện hành trong chuỗi đổi lịch (có thể khác lịch hẹn đã
 * chọn). EMAIL chỉ có {@code emailDaGui} (đã che); MAN_HINH có mã và link để mở mã QR qua
 * {@code /api/booking/phieu-kham/{maPhieuKham}/qr} và {@code /api/booking/so-kham/{maSoKham}/qr}. Trường không có giá trị
 * thì không xuất hiện.
 *
 * @param maSoKham      chỉ khi lịch hẹn đã khám (có sổ khám bệnh)
 */
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
