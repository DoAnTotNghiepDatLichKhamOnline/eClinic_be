package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDateTime;

/**
 * 1 khung 1 giờ của ca làm việc (BOOK-12, SCHED-04). Khung cuối của ca có thể ngắn hơn 1 giờ.
 *
 * @param tongSoCho   số lượt khám của khung (không tính lượt đã huỷ theo ca)
 * @param soChoConLai số lượt còn trống và còn kịp đặt
 * @param hetCho      true khi {@code soChoConLai == 0}: hiển thị "Hết chỗ", không chọn được
 */
public record KhungGioResponse(
        LocalDateTime gioBatDau,
        LocalDateTime gioKetThuc,
        int tongSoCho,
        int soChoConLai,
        boolean hetCho) {
}
