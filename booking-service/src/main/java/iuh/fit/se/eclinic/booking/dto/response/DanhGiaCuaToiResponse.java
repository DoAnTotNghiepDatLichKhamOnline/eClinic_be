package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDateTime;

/**
 * Đánh giá bệnh nhân đã gửi cho 1 lượt khám.
 *
 * @param ngayTao    lúc gửi lần đầu
 * @param duocSuaDen sửa được (PUT) tới thời điểm này
 */
public record DanhGiaCuaToiResponse(int soSao, String nhanXet, LocalDateTime ngayTao, LocalDateTime duocSuaDen) {
}
