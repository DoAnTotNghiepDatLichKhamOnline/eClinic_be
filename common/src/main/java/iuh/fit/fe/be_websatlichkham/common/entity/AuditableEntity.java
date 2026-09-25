package iuh.fit.fe.be_websatlichkham.common.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * Entity có cả ngay_tao và ngay_cap_nhat (tự điền nhờ JPA auditing). Theo ERD: TaiKhoan, LichHen.
 */
@Getter
@MappedSuperclass
public abstract class AuditableEntity extends CreatableEntity {

    @LastModifiedDate
    @Column(name = "ngay_cap_nhat", nullable = false)
    private LocalDateTime ngayCapNhat;

}
