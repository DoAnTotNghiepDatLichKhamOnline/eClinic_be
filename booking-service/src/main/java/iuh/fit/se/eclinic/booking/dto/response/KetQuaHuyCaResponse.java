package iuh.fit.se.eclinic.booking.dto.response;

/**
 * Kết quả hủy ca.
 *
 * @param ca              ca sau khi hủy
 * @param soLichHenCanDoi số lịch hẹn còn hiệu lực của ca vừa được đánh dấu "cần đổi lịch" (bệnh nhân đã được báo)
 */
public record KetQuaHuyCaResponse(CaLamViecResponse ca, int soLichHenCanDoi) {
}
