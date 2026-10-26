package iuh.fit.se.eclinic.chatbot.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import iuh.fit.se.eclinic.common.entity.chatbot.PhienChat;

public interface PhienChatRepository extends JpaRepository<PhienChat, Long> {

    /** Chủ của 1 phiên chat, để kiểm tra người gọi có phải chủ phiên không. */
    record ChuPhien(Long idTaiKhoan, String maDinhDanhKhach, LocalDateTime dongYDuLieuLuc) {
    }

    @Query("""
            select new iuh.fit.se.eclinic.chatbot.repository.PhienChatRepository$ChuPhien(tk.id, p.maDinhDanhKhach,
                p.dongYDuLieuLuc)
            from PhienChat p left join p.taiKhoan tk
            where p.id = :idPhien
            """)
    Optional<ChuPhien> timChuPhien(@Param("idPhien") Long idPhien);

    List<PhienChat> findByTaiKhoanIdOrderByIdDesc(Long taiKhoanId, Limit gioiHan);

}
