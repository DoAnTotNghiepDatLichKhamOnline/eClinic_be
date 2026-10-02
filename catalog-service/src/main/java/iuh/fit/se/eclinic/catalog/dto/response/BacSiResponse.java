package iuh.fit.se.eclinic.catalog.dto.response;

/**
 * Bác sĩ trong danh sách công khai (DOC-03). Không có email, số điện thoại, số giấy phép.
 */
public record BacSiResponse(
        Long id,
        String hoTen,
        String anhDaiDien,
        String hocVi,
        Integer soNamKinhNghiem,
        Long idChuyenKhoa,
        String tenChuyenKhoa) {
}
