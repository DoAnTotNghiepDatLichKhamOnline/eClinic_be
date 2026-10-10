package iuh.fit.se.eclinic.catalog.dto.response;

/**
 * @param soCaDaHuy       số ca sắp tới vừa bị hủy trong lần gọi này
 * @param soLichHenCanDoi số lịch hẹn vừa được đánh dấu cần đổi lịch (bệnh nhân đã được báo)
 */
public record KetQuaNgungCongTacResponse(HoSoBacSiQuanTriResponse bacSi, int soCaDaHuy, int soLichHenCanDoi) {
}
