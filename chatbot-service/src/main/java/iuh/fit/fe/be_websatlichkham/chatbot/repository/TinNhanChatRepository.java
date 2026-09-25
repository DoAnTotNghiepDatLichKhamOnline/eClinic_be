package iuh.fit.fe.be_websatlichkham.chatbot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.fe.be_websatlichkham.common.entity.chatbot.TinNhanChat;

public interface TinNhanChatRepository extends JpaRepository<TinNhanChat, Long> {

    List<TinNhanChat> findByPhienChatIdOrderByNgayTaoAsc(Long phienChatId);

}
