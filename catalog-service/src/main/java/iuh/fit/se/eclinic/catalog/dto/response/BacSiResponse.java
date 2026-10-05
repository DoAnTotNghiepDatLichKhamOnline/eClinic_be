package iuh.fit.se.eclinic.catalog.dto.response;

/**
 * Bác sĩ trong danh sách công khai (DOC-03), đủ để vẽ thẻ bác sĩ. Không có email, số điện thoại, số giấy phép.
 * Ngày còn chỗ sớm nhất của từng bác sĩ lấy ở booking-service: GET /api/booking/khung-gio/ngay-som-nhat.
 */
public record BacSiResponse(
        Long id,
        String hoTen,
        String anhDaiDien,
        String hocVi,
        String chucVu,
        Integer soNamKinhNghiem,
        String gioiThieuNgan,
        Long idChuyenKhoa,
        String tenChuyenKhoa) {
}
