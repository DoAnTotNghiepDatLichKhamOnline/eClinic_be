package iuh.fit.fe.be_websatlichkham.common.entity.identity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.fe.be_websatlichkham.common.entity.BaseEntity;
import iuh.fit.fe.be_websatlichkham.common.enums.CapDoQuyen;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Quản trị viên (ERD: QuanTriVien). Bỏ {@code idCoSoQuanLy} vì hệ thống chỉ có 1 bệnh viện (Q4).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "quan_tri_vien")
@AttributeOverride(name = "id", column = @Column(name = "id_quan_tri_vien"))
public class QuanTriVien extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tai_khoan", nullable = false, unique = true)
    private TaiKhoan taiKhoan;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "cap_do_quyen", nullable = false, length = 20)
    private CapDoQuyen capDoQuyen = CapDoQuyen.NHAN_VIEN;

}
