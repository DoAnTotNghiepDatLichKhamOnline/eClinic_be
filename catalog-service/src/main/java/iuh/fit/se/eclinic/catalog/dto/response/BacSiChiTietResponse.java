package iuh.fit.se.eclinic.catalog.dto.response;

/**
 * Chi tiết công khai của 1 bác sĩ: như {@link BacSiResponse} và thêm tiểu sử.
 */
public record BacSiChiTietResponse(
        Long id,
        String hoTen,
        String anhDaiDien,
        String hocVi,
        Integer soNamKinhNghiem,
        Long idChuyenKhoa,
        String tenChuyenKhoa,
        String tieuSu) {
}
