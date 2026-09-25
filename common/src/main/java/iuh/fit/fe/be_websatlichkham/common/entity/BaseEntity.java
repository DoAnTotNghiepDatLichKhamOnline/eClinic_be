package iuh.fit.fe.be_websatlichkham.common.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * Lớp cha của mọi entity: khoá chính BIGINT AUTO_INCREMENT.
 * <p>
 * Tên cột khoá chính theo ERD (vd {@code id_tai_khoan}) được khai báo ở từng entity bằng
 * {@code @AttributeOverride(name = "id", column = @Column(name = "..."))}.
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

}
