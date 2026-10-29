package iuh.fit.se.eclinic.common.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * 1 dòng chờ gửi. 2 hàng dùng chung bảng hang_cho_gui, cột {@code hang} cho biết hàng nào: sự kiện của booking-service
 * chờ chuyển sang notification-service, và email của notification-service chờ gửi tới máy chủ mail. Mỗi hàng là 1 lớp
 * con giữ các cột riêng của nó; mỗi service chỉ đọc / ghi hàng của mình.
 */
@Entity
@Table(name = "hang_cho_gui")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "hang", discriminatorType = DiscriminatorType.STRING, length = 20)
@AttributeOverride(name = "id", column = @Column(name = "id_hang_cho"))
public abstract class HangChoGui extends CreatableEntity {
}
