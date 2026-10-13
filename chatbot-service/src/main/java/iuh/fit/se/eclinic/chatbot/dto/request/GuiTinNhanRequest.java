package iuh.fit.se.eclinic.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 1 tin nhắn người dùng gửi cho chatbot.
 */
public record GuiTinNhanRequest(
        @NotBlank(message = "{kiemTra.tinNhanChat.batBuoc}")
        @Size(max = 1000, message = "{kiemTra.tinNhanChat.toiDa}")
        String noiDung) {
}
