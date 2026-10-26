package iuh.fit.se.eclinic.chatbot.dto.response;

import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.LoaiTinNhanChat;
import iuh.fit.se.eclinic.common.enums.NguoiGui;

/** 1 tin nhắn trong phiên chat. */
public record TinNhanChatResponse(Long id, NguoiGui nguoiGui, LoaiTinNhanChat loai, String noiDung,
        LocalDateTime ngayTao, GoiYResponse goiY) {
}
