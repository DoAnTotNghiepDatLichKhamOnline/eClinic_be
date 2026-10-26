package iuh.fit.se.eclinic.catalog.dto.response;

import java.time.LocalDate;
import java.util.List;

/** Chi tiết công khai của 1 bác sĩ: như {@link BacSiResponse}, thêm giới thiệu và ảnh giới thiệu. */
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
