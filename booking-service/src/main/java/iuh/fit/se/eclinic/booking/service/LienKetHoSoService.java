package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.HoSoChoXacMinhResponse;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;

/** Gắn hồ sơ bệnh nhân tạo khi đặt lịch như khách vào tài khoản khai đúng CCCD của hồ sơ. */
public interface LienKetHoSoService {

    /** Thử liên kết hồ sơ theo số CCCD tài khoản khai khi đăng ký. */
    void thuLienKet(Long idTaiKhoan);

    /** Gắn hồ sơ (chưa thuộc tài khoản nào) vào tài khoản trong transaction đang chạy của bên gọi. */
    void lienKet(TaiKhoan taiKhoan, HoSoBenhNhan hoSo, boolean khop);

    /** Quản trị viên đã từ chối gắn hồ sơ này vào tài khoản này? */
    boolean daBiTuChoi(Long idTaiKhoan, Long idHoSoBenhNhan);

    TrangDuLieu<HoSoChoXacMinhResponse> choXacMinh(int trang, int kichThuoc);

    void duyet(Long idHoSoBenhNhan);

    void tuChoi(Long idHoSoBenhNhan, String lyDo);

}
