package iuh.fit.fe.be_websatlichkham.modules.chat.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.fe.be_websatlichkham.common.entity.BaseEntity;
import iuh.fit.fe.be_websatlichkham.modules.chat.enums.ChatSessionStatus;
import iuh.fit.fe.be_websatlichkham.modules.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phiên chat với chatbot. Khách chưa đăng nhập được nhận diện bằng {@code anonymousKey}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chat_sessions")
public class ChatSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "anonymous_key", length = 64)
    private String anonymousKey;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private ChatSessionStatus status = ChatSessionStatus.OPEN;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @PrePersist
    void onPrePersist() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

}
