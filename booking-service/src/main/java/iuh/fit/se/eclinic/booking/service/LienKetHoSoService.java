package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.HoSoChoXacMinhResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;

/**
 * Quy tắc #3: gắn hồ sơ bệnh nhân đã có (tạo khi đặt lịch như khách) vào tài khoản khai đúng số CCCD của hồ sơ. Thông
 * tin khớp thì liên kết ngay, không khớp thì hồ sơ chờ quản trị viên xác minh.
 */
public interface LienKetHoSoService {

    /**
     * Thử liên kết hồ sơ theo số CCCD tài khoản khai khi đăng ký. Gọi trước các lần đọc đầu tiên của bệnh nhân sau khi
     * đăng nhập (lịch hẹn của tôi, thông tin điền sẵn, hồ sơ của tôi). Chạy trong transaction riêng và không bao giờ ném
     * lỗi: liên kết không được thì lần đọc vẫn phải chạy.
     */
    void thuLienKet(Long idTaiKhoan);

    /**
     * Gắn hồ sơ (chưa thuộc tài khoản nào) vào tài khoản trong transaction đang chạy của bên gọi.
     *
     * @param khop true: DA_LIEN_KET; false: CHO_XAC_MINH
     */
    void lienKet(TaiKhoan taiKhoan, HoSoBenhNhan hoSo, boolean khop);

    /** Quản trị viên đã từ chối gắn hồ sơ này vào tài khoản này? */
    boolean daBiTuChoi(Long idTaiKhoan, Long idHoSoBenhNhan);

    TrangDuLieu<HoSoChoXacMinhResponse> choXacMinh(int trang, int kichThuoc);

    void duyet(Long idHoSoBenhNhan);

    void tuChoi(Long idHoSoBenhNhan, String lyDo);

}
