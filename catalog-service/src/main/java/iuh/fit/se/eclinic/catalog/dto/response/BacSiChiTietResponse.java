package iuh.fit.se.eclinic.catalog.dto.response;

import java.util.List;

/**
 * Chi tiết công khai của 1 bác sĩ: như {@link BacSiResponse}, thêm đoạn giới thiệu, 3 mục dạng danh sách (mảng rỗng
 * khi chưa có) và ảnh giới thiệu theo thứ tự hiển thị.
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
        List<AnhBacSiResponse> anh) {
}
