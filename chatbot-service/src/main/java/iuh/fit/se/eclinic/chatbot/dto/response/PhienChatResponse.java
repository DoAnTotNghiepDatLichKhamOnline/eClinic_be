package iuh.fit.se.eclinic.chatbot.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Phiên chat vừa mở. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PhienChatResponse(Long idPhien, String maKhach, LocalDateTime thoiGianBatDau,
        List<TinNhanChatResponse> tinNhan) {
}
