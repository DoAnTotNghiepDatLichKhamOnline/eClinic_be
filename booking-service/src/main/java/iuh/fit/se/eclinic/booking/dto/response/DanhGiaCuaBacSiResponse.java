package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 1 đánh giá bác sĩ nhận được. */
public record DanhGiaCuaBacSiResponse(int soSao, String nhanXet, LocalDate ngayKham, LocalDateTime ngayTao) {
}
