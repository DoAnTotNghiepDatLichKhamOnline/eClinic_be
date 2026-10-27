package iuh.fit.se.eclinic.catalog.dto.response;

/** Chuyên khoa cho quản trị viên: chữ như đã lưu ở cả 2 ngôn ngữ, để đổ vào form sửa. */
public record ChuyenKhoaQuanTriResponse(Long id, String tenChuyenKhoa, String tenChuyenKhoaEn, String moTa,
        String moTaEn, String trieuChung, String trieuChungEn) {
}
