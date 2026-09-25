package iuh.fit.fe.be_websatlichkham.common.entity.catalog;

import iuh.fit.fe.be_websatlichkham.common.entity.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Chuyên khoa (ERD: ChuyenKhoa). Phân cấp: Bệnh viện -> Chuyên khoa -> Phòng khám -> Bác sĩ.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chuyen_khoa")
@AttributeOverride(name = "id", column = @Column(name = "id_chuyen_khoa"))
public class ChuyenKhoa extends BaseEntity {

    @Column(name = "ten_chuyen_khoa", nullable = false, unique = true, length = 150)
    private String tenChuyenKhoa;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

}
