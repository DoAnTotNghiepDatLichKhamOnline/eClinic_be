package iuh.fit.se.eclinic.catalog.dto.response;

/**
 * Chuyên khoa cho quản trị viên: chữ đúng như đã lưu ở cả 2 ngôn ngữ, không đổi theo ngôn ngữ của request (để đổ vào
 * form sửa). {@link ChuyenKhoaResponse} là bản công khai, chỉ có chữ của ngôn ngữ được yêu cầu.
 *
 * @param tenChuyenKhoaEn tên tiếng Anh; null = chưa nhập (API công khai trả tên tiếng Việt)
 * @param trieuChung      triệu chứng thường gặp, cho chatbot chọn chuyên khoa; chỉ có ở bản quản trị
 */
public record ChuyenKhoaQuanTriResponse(Long id, String tenChuyenKhoa, String tenChuyenKhoaEn, String moTa,
        String moTaEn, String trieuChung, String trieuChungEn) {
}
