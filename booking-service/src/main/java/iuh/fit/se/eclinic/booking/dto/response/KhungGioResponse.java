package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDateTime;

/** 1 khung 1 giờ của ca làm việc. */
public record KhungGioResponse(
        LocalDateTime gioBatDau,
        LocalDateTime gioKetThuc,
        int tongSoCho,
        int soChoConLai,
        boolean hetCho) {
}
