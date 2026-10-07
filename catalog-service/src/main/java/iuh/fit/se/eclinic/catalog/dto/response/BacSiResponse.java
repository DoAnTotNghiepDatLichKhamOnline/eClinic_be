package iuh.fit.se.eclinic.catalog.dto.response;

/**
 * Bác sĩ trong danh sách công khai (DOC-03), đủ để vẽ thẻ bác sĩ. Không có email, số điện thoại, số giấy phép.
 * Ngày còn chỗ sớm nhất của từng bác sĩ lấy ở booking-service: GET /api/booking/khung-gio/ngay-som-nhat.
 *
 * @param diemDanhGia điểm đánh giá trung bình của bệnh nhân, 1 chữ số thập phân; null khi chưa có đánh giá nào
 * @param soDanhGia   số lượt đánh giá; luôn hiển thị kèm điểm
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
        String tenChuyenKhoa,
        Double diemDanhGia,
        long soDanhGia) {
}
