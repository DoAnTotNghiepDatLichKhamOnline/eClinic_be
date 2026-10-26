package iuh.fit.se.eclinic.booking.dto.response;

/**
 * Kết quả quản trị viên đặt lịch qua tổng đài. Phản hồi duy nhất cho nhân viên có mã phiếu khám, và chỉ lúc vừa đặt:
 * người đặt luôn nhận phiếu (để in hoặc mở mã QR cho người bệnh), như khách tự đặt. Danh sách, chi tiết, tra cứu, hủy,
 * đổi lịch của quản trị viên sau đó không trả mã này.
 *
 * @param idLichHen để mở chi tiết lịch hẹn của quản trị viên
 */
public record DatLichHotlineResponse(Long idLichHen, DatLichResponse phieuKham) {
}
