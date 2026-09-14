package iuh.fit.fe.be_websatlichkham.common.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * Entity có cả created_at và updated_at (tự điền nhờ JPA auditing).
 */
@Getter
@MappedSuperclass
public abstract class AuditableEntity extends CreatableEntity {

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
