package iuh.fit.se.eclinic.chatbot.dto.response;

import java.time.LocalDateTime;

/**
 * 1 dòng trong danh sách phiên chat của tài khoản.
 *
 * @param daDongYDuLieu true: trong phiên này bệnh nhân đã đồng ý cho chatbot đọc lịch hẹn, lượt khám của mình
 */
public record PhienChatTomTatResponse(Long idPhien, LocalDateTime thoiGianBatDau, boolean daDongYDuLieu) {
}
