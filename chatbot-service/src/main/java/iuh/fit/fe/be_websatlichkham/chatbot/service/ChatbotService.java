package iuh.fit.fe.be_websatlichkham.chatbot.service;

import java.util.List;

import iuh.fit.fe.be_websatlichkham.common.entity.chatbot.PhienChat;
import iuh.fit.fe.be_websatlichkham.common.entity.chatbot.TinNhanChat;

public interface ChatbotService {

    PhienChat getPhienChatById(Long phienChatId);

    List<PhienChat> findPhienChatByTaiKhoan(Long taiKhoanId);

    List<TinNhanChat> findTinNhan(Long phienChatId);

}
