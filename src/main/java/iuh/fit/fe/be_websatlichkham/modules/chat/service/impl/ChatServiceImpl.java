package iuh.fit.fe.be_websatlichkham.modules.chat.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.modules.chat.entity.ChatMessage;
import iuh.fit.fe.be_websatlichkham.modules.chat.entity.ChatSession;
import iuh.fit.fe.be_websatlichkham.modules.chat.repository.ChatMessageRepository;
import iuh.fit.fe.be_websatlichkham.modules.chat.repository.ChatSessionRepository;
import iuh.fit.fe.be_websatlichkham.modules.chat.service.ChatService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatServiceImpl implements ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Override
    public ChatSession getSessionById(Long sessionId) {
        return chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", sessionId));
    }

    @Override
    public List<ChatSession> findSessionsByUser(Long userId) {
        return chatSessionRepository.findByUserIdOrderByStartedAtDesc(userId);
    }

    @Override
    public List<ChatMessage> findMessages(Long sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

}
