package iuh.fit.se.eclinic.catalog.dto.response;

/** Bác sĩ trong danh sách công khai, đủ để vẽ thẻ bác sĩ. */
public record BacSiResponse(
        Long id,
        String hoTen,
        String anhDaiDien,
        String hocVi,
        String chucVu,
        Integer soNamKinhNghiem,
        String gioiThieuNgan,
        Long idChuyenKhoa,
        String tenChuyenKhoa,
        Double diemDanhGia,
        long soDanhGia) {
}
