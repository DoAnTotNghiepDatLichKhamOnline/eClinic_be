package iuh.fit.se.eclinic.common.entity.catalog;

import iuh.fit.se.eclinic.common.entity.BaseEntity;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Chuyên khoa. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chuyen_khoa")
@AttributeOverride(name = "id", column = @Column(name = "id_chuyen_khoa"))
public class ChuyenKhoa extends BaseEntity {

    @Column(name = "ten_chuyen_khoa", nullable = false, unique = true, length = 150)
    private String tenChuyenKhoa;

    @Column(name = "ten_chuyen_khoa_en", unique = true, length = 150)
    private String tenChuyenKhoaEn;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "mo_ta_en", columnDefinition = "TEXT")
    private String moTaEn;

    /** Triệu chứng thường gặp của chuyên khoa: chỉ để chatbot chọn chuyên khoa, API công khai không trả. */
    @Column(name = "trieu_chung", columnDefinition = "TEXT")
    private String trieuChung;

    @Column(name = "trieu_chung_en", columnDefinition = "TEXT")
    private String trieuChungEn;

    // Chữ theo ngôn ngữ: bản tiếng Anh nếu có, không thì tiếng Việt
    public String ten(NgonNgu ngonNgu) {
        return ngonNgu.chon(tenChuyenKhoa, tenChuyenKhoaEn);
    }

    public String moTa(NgonNgu ngonNgu) {
        return ngonNgu.chon(moTa, moTaEn);
    }

    public String trieuChung(NgonNgu ngonNgu) {
        return ngonNgu.chon(trieuChung, trieuChungEn);
    }
}
