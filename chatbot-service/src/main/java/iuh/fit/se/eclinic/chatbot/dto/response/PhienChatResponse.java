package iuh.fit.se.eclinic.chatbot.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Phiên chat vừa mở.
 *
 * @param maKhach chỉ có khi người mở chưa đăng nhập, và chỉ trả 1 lần này: frontend giữ lại, gửi ở header X-Chat-Khach
 *                trong các request sau của phiên. Server chỉ lưu bản băm nên không cấp lại được.
 * @param tinNhan câu chào của chatbot
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PhienChatResponse(Long idPhien, String maKhach, LocalDateTime thoiGianBatDau,
        List<TinNhanChatResponse> tinNhan) {
}
