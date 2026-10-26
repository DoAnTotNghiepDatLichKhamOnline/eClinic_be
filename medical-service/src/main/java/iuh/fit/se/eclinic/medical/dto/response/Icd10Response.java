package iuh.fit.se.eclinic.medical.dto.response;

/** 1 mã trong danh mục ICD-10: gợi ý khi bác sĩ gõ chẩn đoán, và mã của chẩn đoán chính trong hồ sơ bệnh án. */
public record Icd10Response(String ma, String ten, boolean manTinh) {
}
