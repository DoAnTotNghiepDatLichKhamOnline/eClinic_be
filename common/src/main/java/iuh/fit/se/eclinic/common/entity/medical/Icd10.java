package iuh.fit.se.eclinic.common.entity.medical;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.util.DanhMucIcd10;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 1 mã trong danh mục ICD-10 ({@link DanhMucIcd10}, tệp danh-muc/icd10.tsv; không phải bảng). */
@Getter
@AllArgsConstructor
public class Icd10 {

    private final String ma;

    private final String ten;

    private final String tenEn;

    /** Thuộc Phụ lục VII Thông tư 26/2025/TT-BYT: chẩn đoán chính có mã này thì mỗi thuốc kê được tối đa 90 ngày. */
    private final boolean manTinh;

    public String ten(NgonNgu ngonNgu) {
        return ngonNgu.chon(ten, tenEn);
    }

}
