package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 1 đánh giá trong danh sách của quản trị viên, kèm bệnh nhân và bác sĩ của lượt khám.
 *
 * @param maTraCuu    mã tra cứu ngắn của lịch hẹn
 * @param ngayCapNhat lần sửa gần nhất của bệnh nhân (bằng {@code ngayTao} nếu chưa sửa)
 */
public record DanhGiaQuanTriResponse(
        Long id,
        int soSao,
        String nhanXet,
        LocalDateTime ngayTao,
        LocalDateTime ngayCapNhat,
        String maTraCuu,
        LocalDate ngayKham,
        Long idHoSoBenhNhan,
        String tenBenhNhan,
        Long idBacSi,
        String tenBacSi) {
}
