package iuh.fit.se.eclinic.common.entity.medical;

import iuh.fit.se.eclinic.common.entity.BaseEntity;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.enums.DuongDungThuoc;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Danh mục thuốc. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "thuoc", uniqueConstraints = @UniqueConstraint(name = "uk_thuoc_ten_ham_luong",
        columnNames = { "ten_chuan_hoa", "ham_luong_chuan_hoa" }))
@AttributeOverride(name = "id", column = @Column(name = "id_thuoc"))
public class Thuoc extends BaseEntity {

    /** Tên hiển thị như người nhập. */
    @Column(name = "ten_thuoc", nullable = false)
    private String tenThuoc;

    /** lowercase + trim + gộp khoảng trắng. */
    @Column(name = "ten_chuan_hoa", nullable = false)
    private String tenChuanHoa;

    /** Hoạt chất / tên chung quốc tế; thuốc phối hợp nối bằng " + ". Null ở thuốc cũ chưa bổ sung. */
    @Column(name = "hoat_chat")
    private String hoatChat;

    /** vd 500mg, 250mg/5ml. Null ở thuốc cũ chưa bổ sung. */
    @Column(name = "ham_luong", length = 100)
    private String hamLuong;

    /** Hàm lượng chuẩn hoá như tên; chuỗi rỗng khi chưa có hàm lượng. */
    @Column(name = "ham_luong_chuan_hoa", nullable = false, length = 100)
    private String hamLuongChuanHoa = "";

    /** viên nén, siro... */
    @Column(name = "dang_bao_che", length = 50)
    private String dangBaoChe;

    @Column(name = "dang_bao_che_en", length = 50)
    private String dangBaoCheEn;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "duong_dung", length = 20)
    private DuongDungThuoc duongDung;

    /** viên, ml, gói... */
    @Column(name = "don_vi", length = 30)
    private String donVi;

    @Column(name = "don_vi_en", length = 30)
    private String donViEn;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "mo_ta_en", columnDefinition = "TEXT")
    private String moTaEn;

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

    // Chữ theo ngôn ngữ: bản tiếng Anh nếu có, không thì tiếng Việt
    public String donVi(NgonNgu ngonNgu) {
        return ngonNgu.chon(donVi, donViEn);
    }

    public String moTa(NgonNgu ngonNgu) {
        return ngonNgu.chon(moTa, moTaEn);
    }

    public String dangBaoChe(NgonNgu ngonNgu) {
        return ngonNgu.chon(dangBaoChe, dangBaoCheEn);
    }

    /** Kê được cho dòng thuốc mới: đang dùng và đã xác minh. */
    public boolean keDuoc() {
        return trangThai == TrangThaiThuoc.DANG_DUNG && daXacMinh;
    }
}
