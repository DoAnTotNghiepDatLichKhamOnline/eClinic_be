package iuh.fit.se.eclinic.common.entity.medical;

import org.hibernate.annotations.Immutable;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 1 mã trong danh mục ICD-10 (DOANTOTNGH-11). Danh mục chỉ đọc, nạp sẵn bằng migration V30; bác sĩ chọn 1 mã cho chẩn
 * đoán chính. Khác các entity khác: khoá chính là chính mã ICD (vd {@code J06.9}), nên không kế thừa {@code BaseEntity}.
 */
@Getter
@NoArgsConstructor
@Immutable
@Entity
@Table(name = "icd10")
public class Icd10 {

    @Id
    @Column(name = "ma", length = 10)
    private String ma;

    @Column(name = "ten", nullable = false)
    private String ten;

    @Column(name = "ten_en", nullable = false)
    private String tenEn;

    /** Thuộc Phụ lục VII Thông tư 26/2025/TT-BYT: chẩn đoán chính có mã này thì mỗi thuốc kê được tối đa 90 ngày. */
    @Column(name = "man_tinh", nullable = false)
    private boolean manTinh;

    public String ten(NgonNgu ngonNgu) {
        return ngonNgu.chon(ten, tenEn);
    }

}
