package iuh.fit.se.eclinic.chatbot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.eclinic.common.entity.chatbot.PhienChat;

public interface PhienChatRepository extends JpaRepository<PhienChat, Long> {

    List<PhienChat> findByTaiKhoanIdOrderByThoiGianBatDauDesc(Long taiKhoanId);

    Optional<PhienChat> findFirstByMaDinhDanhKhachOrderByThoiGianBatDauDesc(String maDinhDanhKhach);

}
