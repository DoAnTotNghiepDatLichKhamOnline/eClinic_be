package iuh.fit.se.eclinic.common.entity.catalog;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import iuh.fit.se.eclinic.common.entity.BaseEntity;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;
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

    /** vd: Trưởng khoa Nhi */
    @Column(name = "chuc_vu", length = 150)
    private String chucVu;

    @Column(name = "chuc_vu_en", length = 150)
    private String chucVuEn;

    /** 1-2 câu hiện trên thẻ bác sĩ. */
    @Column(name = "gioi_thieu_ngan", length = 300)
    private String gioiThieuNgan;

    @Column(name = "gioi_thieu_ngan_en", length = 300)
    private String gioiThieuNganEn;

    /** Đoạn giới thiệu tự do. */
    @Column(name = "tieu_su", columnDefinition = "TEXT")
    private String tieuSu;

    @Column(name = "tieu_su_en", columnDefinition = "TEXT")
    private String tieuSuEn;

    /** 3 mục dưới đây lưu văn bản thuần, mỗi dòng 1 ý; API trả về / nhận vào dạng mảng chuỗi. */
    @Column(name = "qua_trinh_dao_tao", columnDefinition = "TEXT")
    private String quaTrinhDaoTao;

    @Column(name = "qua_trinh_dao_tao_en", columnDefinition = "TEXT")
    private String quaTrinhDaoTaoEn;

    @Column(name = "qua_trinh_cong_tac", columnDefinition = "TEXT")
    private String quaTrinhCongTac;

    @Column(name = "qua_trinh_cong_tac_en", columnDefinition = "TEXT")
    private String quaTrinhCongTacEn;

    /** Khám và điều trị. */
    @Column(name = "linh_vuc_kham_chua", columnDefinition = "TEXT")
    private String linhVucKhamChua;

    @Column(name = "linh_vuc_kham_chua_en", columnDefinition = "TEXT")
    private String linhVucKhamChuaEn;

    @Column(name = "so_nam_kinh_nghiem")
    private Integer soNamKinhNghiem;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private TrangThaiBacSi trangThai = TrangThaiBacSi.DANG_CONG_TAC;

    // Chữ theo ngôn ngữ: bản tiếng Anh nếu được yêu cầu và đã nhập, không thì bản tiếng Việt (NgonNgu.chon).
    public String chucVu(NgonNgu ngonNgu) {
        return ngonNgu.chon(chucVu, chucVuEn);
    }

    public String gioiThieuNgan(NgonNgu ngonNgu) {
        return ngonNgu.chon(gioiThieuNgan, gioiThieuNganEn);
    }

    public String tieuSu(NgonNgu ngonNgu) {
        return ngonNgu.chon(tieuSu, tieuSuEn);
    }

    public String quaTrinhDaoTao(NgonNgu ngonNgu) {
        return ngonNgu.chon(quaTrinhDaoTao, quaTrinhDaoTaoEn);
    }

    public String quaTrinhCongTac(NgonNgu ngonNgu) {
        return ngonNgu.chon(quaTrinhCongTac, quaTrinhCongTacEn);
    }

    public String linhVucKhamChua(NgonNgu ngonNgu) {
        return ngonNgu.chon(linhVucKhamChua, linhVucKhamChuaEn);
    }
}
