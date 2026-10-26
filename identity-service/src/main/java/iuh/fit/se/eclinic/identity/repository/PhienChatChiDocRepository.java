package iuh.fit.se.eclinic.identity.repository;

import org.springframework.data.repository.Repository;

import iuh.fit.se.eclinic.common.entity.chatbot.PhienChat;

/**
 * CHỈ ĐỌC phiên chat (bảng của chatbot-service) để kiểm tra trước khi xoá tài khoản. Kế thừa {@link Repository}
 * chứ không phải JpaRepository nên không có save / delete: identity-service không được ghi bảng này.
 */
public interface PhienChatChiDocRepository extends Repository<PhienChat, Long> {

    boolean existsByTaiKhoanId(Long taiKhoanId);

}
