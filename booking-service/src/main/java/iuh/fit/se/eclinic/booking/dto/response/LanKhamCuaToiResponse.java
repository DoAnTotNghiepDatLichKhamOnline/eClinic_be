package iuh.fit.se.eclinic.booking.dto.response;

/** 1 lượt đã khám trong lịch sử khám của bệnh nhân. */
public record LanKhamCuaToiResponse(LichHenCuaToiResponse lichHen, KetQuaKhamResponse ketQua) {
}
