package iuh.fit.se.eclinic.catalog.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Chi tiết công khai của 1 bác sĩ: như {@link BacSiResponse}, thêm đoạn giới thiệu, 3 mục dạng danh sách (mảng rỗng
 * khi chưa có) và ảnh giới thiệu theo thứ tự hiển thị. Giấy phép hành nghề (số, ngày cấp, nơi cấp, phạm vi) công khai
 * để người bệnh tra cứu.
 *
 * @param ngonNguKham mã ngôn ngữ ISO 639 viết thường (vd "vi", "en"), mảng rỗng khi chưa có
 * @param diemDanhGia điểm đánh giá trung bình của bệnh nhân, 1 chữ số thập phân; null khi chưa có đánh giá nào
 * @param soDanhGia   số lượt đánh giá. Nhận xét của bệnh nhân không công khai
 */
public record BacSiChiTietResponse(
        Long id,
        String hoTen,
        String anhDaiDien,
        String hocVi,
        String chucVu,
        Integer soNamKinhNghiem,
        String gioiThieuNgan,
        Long idChuyenKhoa,
        String tenChuyenKhoa,
        String tieuSu,
        List<String> quaTrinhDaoTao,
        List<String> quaTrinhCongTac,
        List<String> linhVucKhamChua,
        List<String> giaiThuong,
        List<String> congTrinhNghienCuu,
        List<String> ngonNguKham,
        String soGiayPhep,
        LocalDate ngayCapGiayPhep,
        String noiCapGiayPhep,
        String phamViHanhNghe,
        List<AnhBacSiResponse> anh,
        Double diemDanhGia,
        long soDanhGia) {
}
