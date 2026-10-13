package iuh.fit.se.eclinic.catalog.dto.response;

import java.util.List;

import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;

/**
 * Hồ sơ giới thiệu của bác sĩ cho quản trị viên: như {@link BacSiChiTietResponse}, thêm số giấy phép và trạng thái.
 * Trả về cả bác sĩ không hiển thị công khai (ngừng công tác, tài khoản bị vô hiệu hoá).
 * <p>
 * Các mục giới thiệu là chữ đã lưu của cả 2 ngôn ngữ (trường {@code ...En} là bản tiếng Anh, null / mảng rỗng khi chưa
 * nhập), không đổi theo ngôn ngữ của request; riêng tên chuyên khoa theo ngôn ngữ của request.
 */
public record HoSoBacSiQuanTriResponse(
        Long id,
        String maBacSi,
        String hoTen,
        String email,
        String soDienThoai,
        String anhDaiDien,
        String soGiayPhep,
        TrangThaiBacSi trangThai,
        TrangThaiTaiKhoan trangThaiTaiKhoan,
        boolean phaiDoiMatKhau,
        String hocVi,
        String chucVu,
        String chucVuEn,
        Integer soNamKinhNghiem,
        String gioiThieuNgan,
        String gioiThieuNganEn,
        Long idChuyenKhoa,
        String tenChuyenKhoa,
        String tieuSu,
        String tieuSuEn,
        List<String> quaTrinhDaoTao,
        List<String> quaTrinhDaoTaoEn,
        List<String> quaTrinhCongTac,
        List<String> quaTrinhCongTacEn,
        List<String> linhVucKhamChua,
        List<String> linhVucKhamChuaEn,
        List<AnhBacSiQuanTriResponse> anh) {
}
