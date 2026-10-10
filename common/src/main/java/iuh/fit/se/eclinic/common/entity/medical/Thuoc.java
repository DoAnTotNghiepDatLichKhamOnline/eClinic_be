package iuh.fit.se.eclinic.common.entity.medical;

import iuh.fit.se.eclinic.common.entity.BaseEntity;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiThuoc;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Danh mục thuốc (ERD: Thuoc).
 * <p>
 * Khác ERD: thêm {@code tenChuanHoa} (UNIQUE, dùng cho ThuocService.layHoacTao khi bác sĩ tự thêm thuốc)
 * và {@code bacSiTao}. Ai quản lý danh mục thuốc còn chờ chốt (EXAM-05) — {@code daXacMinh} = Admin đã duyệt.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "thuoc")
@AttributeOverride(name = "id", column = @Column(name = "id_thuoc"))
public class Thuoc extends BaseEntity {

    /** Tên hiển thị như người nhập. */
    @Column(name = "ten_thuoc", nullable = false)
    private String tenThuoc;

    /** lowercase + trim + gộp khoảng trắng. */
    @Column(name = "ten_chuan_hoa", nullable = false, unique = true)
    private String tenChuanHoa;

    /** viên, ml, gói... */
    @Column(name = "don_vi", length = 30)
    private String donVi;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "da_xac_minh", nullable = false)
    private boolean daXacMinh;

    /** NGUNG_DUNG: không được gợi ý, không kê mới được; đơn thuốc cũ vẫn hiển thị. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private TrangThaiThuoc trangThai = TrangThaiThuoc.DANG_DUNG;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bac_si_tao")
    private BacSi bacSiTao;

}
