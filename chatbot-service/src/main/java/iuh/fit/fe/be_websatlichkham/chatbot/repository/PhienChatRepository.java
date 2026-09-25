package iuh.fit.fe.be_websatlichkham.chatbot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.fe.be_websatlichkham.common.entity.chatbot.PhienChat;

public interface PhienChatRepository extends JpaRepository<PhienChat, Long> {

    List<PhienChat> findByTaiKhoanIdOrderByThoiGianBatDauDesc(Long taiKhoanId);

    Optional<PhienChat> findFirstByMaDinhDanhKhachOrderByThoiGianBatDauDesc(String maDinhDanhKhach);

}
