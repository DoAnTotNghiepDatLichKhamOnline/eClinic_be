package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDateTime;

/** 1 khung giờ của chuyên khoa, gộp các khung cùng giờ bắt đầu của mọi bác sĩ ("bác sĩ bất kỳ"). */
public record KhungGioGopResponse(
        LocalDateTime gioBatDau,
        LocalDateTime gioKetThuc,
        int tongSoCho,
        int soChoConLai,
        boolean hetCho,
        int soBacSi) {
}
