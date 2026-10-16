package iuh.fit.se.eclinic.catalog.dto.response;

import java.time.LocalDate;
import java.util.List;

import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;

/**
 * Hồ sơ giới thiệu của bác sĩ cho quản trị viên: như {@link BacSiChiTietResponse}, thêm số giấy phép và trạng thái.
 * Trả về cả bác sĩ không hiển thị công khai (ngừng công tác, tài khoản bị vô hiệu hoá).
 * <p>
 * Các mục giới thiệu là chữ đã lưu của cả 2 ngôn ngữ (trường {@code ...En} là bản tiếng Anh, null / mảng rỗng khi chưa
 * nhập), không đổi theo ngôn ngữ của request; riêng tên chuyên khoa theo ngôn ngữ của request.
 * <p>
 * Bác sĩ xem hồ sơ của chính mình cũng nhận bản này (GET /api/catalog/bac-si/toi/ho-so).
 *
 * @param phienBanHoSo    gửi lại khi lưu hồ sơ ({@code phienBan}); tăng 1 mỗi lần lưu
 * @param soNamKinhNghiem suy ra từ {@code namBatDauHanhNghe}; null khi chưa có năm
 * @param ngonNguKham     mã ngôn ngữ ISO 639 viết thường (vd "vi", "en"), mảng rỗng khi chưa có
 */
public record HoSoBacSiQuanTriResponse(
        Long id,
        String maBacSi,
        String hoTen,
        String email,
        String soDienThoai,
        String anhDaiDien,
        String soGiayPhep,
        LocalDate ngayCapGiayPhep,
        String noiCapGiayPhep,
        String noiCapGiayPhepEn,
        String phamViHanhNghe,
        String phamViHanhNgheEn,
        TrangThaiBacSi trangThai,
        TrangThaiTaiKhoan trangThaiTaiKhoan,
        boolean phaiDoiMatKhau,
        String hocVi,
        String chucVu,
        String chucVuEn,
        Integer namBatDauHanhNghe,
        Integer soNamKinhNghiem,
        List<String> ngonNguKham,
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
        List<String> giaiThuong,
        List<String> giaiThuongEn,
        List<String> congTrinhNghienCuu,
        List<String> congTrinhNghienCuuEn,
        int phienBanHoSo,
        List<AnhBacSiQuanTriResponse> anh) {
}
