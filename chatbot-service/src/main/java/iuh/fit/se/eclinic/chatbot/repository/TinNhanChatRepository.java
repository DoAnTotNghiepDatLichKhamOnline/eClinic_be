package iuh.fit.se.eclinic.chatbot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.eclinic.common.entity.chatbot.TinNhanChat;

public interface TinNhanChatRepository extends JpaRepository<TinNhanChat, Long> {

    List<TinNhanChat> findByPhienChatIdOrderByNgayTaoAsc(Long phienChatId);

}
