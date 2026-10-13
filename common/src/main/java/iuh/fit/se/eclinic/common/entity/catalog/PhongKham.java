package iuh.fit.se.eclinic.common.entity.catalog;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.BaseEntity;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phòng khám (ERD: PhongKham): thuộc 1 chuyên khoa. Bỏ {@code idCoSo} vì chỉ có 1 bệnh viện (Q4).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "phong_kham")
@AttributeOverride(name = "id", column = @Column(name = "id_phong_kham"))
public class PhongKham extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_chuyen_khoa", nullable = false)
    private ChuyenKhoa chuyenKhoa;

    /** vd: Phòng 203 */
    @Column(name = "ten_phong", nullable = false, unique = true, length = 100)
    private String tenPhong;

    @Column(name = "ten_phong_en", unique = true, length = 100)
    private String tenPhongEn;

    @Column(name = "tang", length = 20)
    private String tang;

    @Column(name = "tang_en", length = 20)
    private String tangEn;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private TrangThaiPhongKham trangThai = TrangThaiPhongKham.HOAT_DONG;

    // Chữ theo ngôn ngữ: bản tiếng Anh nếu được yêu cầu và đã nhập, không thì bản tiếng Việt (NgonNgu.chon).
    public String ten(NgonNgu ngonNgu) {
        return ngonNgu.chon(tenPhong, tenPhongEn);
    }

    public String tang(NgonNgu ngonNgu) {
        return ngonNgu.chon(tang, tangEn);
    }
}
