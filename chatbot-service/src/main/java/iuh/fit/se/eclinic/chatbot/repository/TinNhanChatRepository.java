package iuh.fit.se.eclinic.chatbot.repository;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.eclinic.common.entity.chatbot.TinNhanChat;

/**
 * Thứ tự tin nhắn theo id, không theo ngay_tao: câu hỏi và câu trả lời được lưu trong cùng 1 transaction nên có thể
 * trùng ngay_tao.
 */
public interface TinNhanChatRepository extends JpaRepository<TinNhanChat, Long> {

    List<TinNhanChat> findByPhienChatIdOrderByIdAsc(Long phienChatId);

    /** Các tin nhắn mới nhất của phiên, mới nhất trước. */
    List<TinNhanChat> findByPhienChatIdOrderByIdDesc(Long phienChatId, Limit gioiHan);

}
