package iuh.fit.se.eclinic.common.entity;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 1 dòng lịch sử thay đổi, chỉ thêm và đọc. Các loại (lịch hẹn, ca làm việc, cấu hình, sửa bệnh án) dùng chung bảng
 * lich_su_thay_doi, cột {@code loai} cho biết loại nào; mỗi loại là 1 lớp con giữ các cột riêng của nó.
 */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "lich_su_thay_doi")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "loai", discriminatorType = DiscriminatorType.STRING, length = 20)
@AttributeOverride(name = "id", column = @Column(name = "id_lich_su"))
public abstract class LichSuThayDoi extends CreatableEntity {

    /** Câu người dùng gõ, hoặc câu của hệ thống dạng {@code {heThong.<khoa>}} (VanBanHeThong). */
    @Column(name = "ly_do", updatable = false, columnDefinition = "TEXT")
    private String lyDo;

}
