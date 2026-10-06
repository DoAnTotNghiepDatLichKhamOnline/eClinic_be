package iuh.fit.se.eclinic.booking.dto.response;

/**
 * 1 lượt đã khám trong lịch sử khám của bệnh nhân.
 *
 * @param lichHen như 1 dòng "lịch hẹn của tôi"
 * @param ketQua  chẩn đoán, lời dặn, ngày tái khám, đơn thuốc
 */
public record LanKhamCuaToiResponse(LichHenCuaToiResponse lichHen, KetQuaKhamResponse ketQua) {
}
