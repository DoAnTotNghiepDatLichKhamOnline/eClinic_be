package iuh.fit.fe.be_websatlichkham.common.entity.catalog;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.fe.be_websatlichkham.common.entity.BaseEntity;
import iuh.fit.fe.be_websatlichkham.common.entity.identity.TaiKhoan;
import iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiBacSi;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Hồ sơ bác sĩ (ERD: BacSi). Chỉ Admin được sửa (DOC-01, ADM-02).
 * <p>
 * Khác ERD: bỏ {@code phiKham} (Q1 — không thanh toán ở MVP); thêm {@code hocVi} (DOC-01/DOC-03)
 * và {@code trangThai} "Đang/Ngừng công tác" (ADM-02).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bac_si")
@AttributeOverride(name = "id", column = @Column(name = "id_bac_si"))
public class BacSi extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tai_khoan", nullable = false, unique = true)
    private TaiKhoan taiKhoan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_chuyen_khoa", nullable = false)
    private ChuyenKhoa chuyenKhoa;

    @Column(name = "so_giay_phep", length = 50, unique = true)
    private String soGiayPhep;

    /** vd: ThS.BS, TS.BS, BSCKII */
    @Column(name = "hoc_vi", length = 100)
    private String hocVi;

    @Column(name = "tieu_su", columnDefinition = "TEXT")
    private String tieuSu;

    @Column(name = "so_nam_kinh_nghiem")
    private Integer soNamKinhNghiem;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private TrangThaiBacSi trangThai = TrangThaiBacSi.DANG_CONG_TAC;

}
