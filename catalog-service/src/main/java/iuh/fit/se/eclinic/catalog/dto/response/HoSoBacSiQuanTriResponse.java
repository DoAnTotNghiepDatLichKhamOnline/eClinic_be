package iuh.fit.se.eclinic.catalog.dto.response;

import java.time.LocalDate;
import java.util.List;

import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;

/** Hồ sơ giới thiệu của bác sĩ cho quản trị viên: như {@link BacSiChiTietResponse}, thêm số giấy phép và trạng thái. */
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
