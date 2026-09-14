package iuh.fit.fe.be_websatlichkham.modules.chat.service;

import java.util.List;

import iuh.fit.fe.be_websatlichkham.modules.chat.entity.ChatMessage;
import iuh.fit.fe.be_websatlichkham.modules.chat.entity.ChatSession;

public interface ChatService {

    ChatSession getSessionById(Long sessionId);

    List<ChatSession> findSessionsByUser(Long userId);

    List<ChatMessage> findMessages(Long sessionId);

}
