package iuh.fit.se.eclinic.medical.service;

import java.util.List;

import iuh.fit.se.eclinic.common.entity.medical.Icd10;

public interface Icd10Service {

    /** Mã ICD-10 cho chẩn đoán chính. */
    Icd10 layDeGhi(String ma);

    /** Gợi ý theo đầu mã hoặc một phần tên tiếng Việt / tiếng Anh, tối đa 20; từ khoá trống trả danh sách rỗng. */
    List<Icd10> timKiem(String tuKhoa);

}
