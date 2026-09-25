package iuh.fit.fe.be_websatlichkham.common.entity.medical;

import iuh.fit.fe.be_websatlichkham.common.entity.BaseEntity;
import iuh.fit.fe.be_websatlichkham.common.entity.catalog.BacSi;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Danh mục thuốc (ERD: Thuoc).
 * <p>
 * Khác ERD: thêm {@code tenChuanHoa} (UNIQUE, dùng cho ThuocService.getOrCreate khi bác sĩ tự thêm thuốc)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bac_si_tao")
    private BacSi bacSiTao;

}
