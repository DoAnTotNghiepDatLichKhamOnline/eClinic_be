package iuh.fit.se.eclinic.medical.dto.response;

/**
 * 1 mã trong danh mục ICD-10: gợi ý khi bác sĩ gõ chẩn đoán, và mã của chẩn đoán chính trong hồ sơ bệnh án.
 *
 * @param ma      vd J06.9; gửi lại ở {@code maIcdChinh}
 * @param ten     theo ngôn ngữ người xem
 * @param manTinh thuộc Phụ lục VII Thông tư 26/2025: chẩn đoán chính có mã này thì mỗi thuốc kê được tối đa 90 ngày
 *                (không thì 30)
 */
public record Icd10Response(String ma, String ten, boolean manTinh) {
}
