package iuh.fit.se.eclinic.catalog.dto.response;

import java.util.List;

import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;

/**
 * Hồ sơ giới thiệu của bác sĩ cho quản trị viên: như {@link BacSiChiTietResponse}, thêm số giấy phép và trạng thái.
 * Trả về cả bác sĩ không hiển thị công khai (ngừng công tác, tài khoản bị vô hiệu hoá).
 */
public record HoSoBacSiQuanTriResponse(
        Long id,
        String hoTen,
        String anhDaiDien,
        String soGiayPhep,
        TrangThaiBacSi trangThai,
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
