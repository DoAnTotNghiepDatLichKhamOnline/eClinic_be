package iuh.fit.fe.be_websatlichkham.modules.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.fe.be_websatlichkham.modules.chat.entity.ChatSession;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByUserIdOrderByStartedAtDesc(Long userId);

    Optional<ChatSession> findFirstByAnonymousKeyOrderByStartedAtDesc(String anonymousKey);

}
