package iuh.fit.se.eclinic.chatbot.dto.response;

import java.time.LocalDateTime;

/** 1 dòng trong danh sách phiên chat của tài khoản. */
public record PhienChatTomTatResponse(Long idPhien, LocalDateTime thoiGianBatDau, boolean daDongYDuLieu) {
}
