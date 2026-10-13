package iuh.fit.se.eclinic.chatbot.dto.response;

/**
 * Kết quả gửi 1 tin nhắn: tin của người dùng đã lưu và câu trả lời của chatbot.
 */
public record TraLoiResponse(TinNhanChatResponse tinNhan, TinNhanChatResponse traLoi) {
}
