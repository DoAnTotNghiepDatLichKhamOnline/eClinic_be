package iuh.fit.se.eclinic.chatbot.service;

import java.util.List;

import iuh.fit.se.eclinic.common.entity.chatbot.PhienChat;
import iuh.fit.se.eclinic.common.entity.chatbot.TinNhanChat;

public interface ChatbotService {

    PhienChat layPhienChatTheoId(Long phienChatId);

    List<PhienChat> timPhienChatTheoTaiKhoan(Long taiKhoanId);

    List<TinNhanChat> timTinNhan(Long phienChatId);

}
