package iuh.fit.se.eclinic.catalog.service;

import iuh.fit.se.eclinic.catalog.dto.request.SuaBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.request.ThemBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhHuongNgungCongTacResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.HoSoBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.dto.response.KetQuaNgungCongTacResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;

/** Danh bạ bác sĩ của quản trị viên; tài khoản và ca làm việc do service khác ghi qua API nội bộ. */
public interface QuanLyBacSiService {

    TrangDuLieu<BacSiQuanTriResponse> danhSach(String tuKhoa, Long idChuyenKhoa, TrangThaiBacSi trangThai, int trang,
            int kichThuoc);

    /** Tạo tài khoản (mật khẩu mặc định, phải đặt mật khẩu ở lần đăng nhập đầu) rồi tạo hồ sơ bác sĩ. */
    HoSoBacSiQuanTriResponse them(ThemBacSiRequest request);

    /** Sửa họ tên, số điện thoại, chuyên khoa, số giấy phép. */
    HoSoBacSiQuanTriResponse sua(Long idBacSi, SuaBacSiRequest request);

    AnhHuongNgungCongTacResponse anhHuongNgungCongTac(Long idBacSi);

    /** 3 bước gọi lại được: ngừng công tác, booking-service hủy ca, identity-service khoá tài khoản. */
    KetQuaNgungCongTacResponse ngungCongTac(Long idTaiKhoanQuanTri, Long idBacSi, String lyDo);

    /** Kích hoạt lại tài khoản rồi đưa bác sĩ về DANG_CONG_TAC. Các ca đã hủy không được khôi phục. */
    HoSoBacSiQuanTriResponse congTacLai(Long idBacSi);

}
