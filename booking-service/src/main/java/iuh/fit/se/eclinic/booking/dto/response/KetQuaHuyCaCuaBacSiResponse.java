package iuh.fit.se.eclinic.booking.dto.response;

/**
 * @param soCaDaHuy       số ca sắp tới vừa bị hủy (0 nếu gọi lại sau khi đã hủy hết)
 * @param soLichHenCanDoi số lịch hẹn còn hiệu lực vừa được đánh dấu cần đổi lịch
 */
public record KetQuaHuyCaCuaBacSiResponse(int soCaDaHuy, int soLichHenCanDoi) {
}
