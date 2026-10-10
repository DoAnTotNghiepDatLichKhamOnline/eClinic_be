package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 1 đánh giá bác sĩ nhận được. Ẩn danh: không có tên bệnh nhân, không có mã lịch hẹn.
 *
 * @param ngayKham ngày của lượt khám được đánh giá
 * @param ngayTao  lúc bệnh nhân gửi đánh giá
 */
public record DanhGiaCuaBacSiResponse(int soSao, String nhanXet, LocalDate ngayKham, LocalDateTime ngayTao) {
}
