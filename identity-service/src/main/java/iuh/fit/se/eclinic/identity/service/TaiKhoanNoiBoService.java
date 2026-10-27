package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.identity.dto.request.SuaThongTinTaiKhoanRequest;
import iuh.fit.se.eclinic.identity.dto.request.TaoTaiKhoanBacSiRequest;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanNoiBoResponse;

/** Việc catalog-service nhờ làm trên tài khoản BAC_SI (API nội bộ /noi-bo/tai-khoan). */
public interface TaiKhoanNoiBoService {

    /** Tạo tài khoản BAC_SI đã kích hoạt, mật khẩu mặc định phải đổi ở lần đăng nhập đầu. */
    TaiKhoanNoiBoResponse taoTaiKhoanBacSi(TaoTaiKhoanBacSiRequest request);

    /** Xoá tài khoản vừa tạo khi catalog-service không tạo được hồ sơ bác sĩ. */
    void xoaTaiKhoanBacSi(Long id);

    /** Sửa họ tên, số điện thoại. Ném SO_DIEN_THOAI_DA_TON_TAI. */
    TaiKhoanNoiBoResponse suaThongTin(Long id, SuaThongTinTaiKhoanRequest request);

    /** Vô hiệu hoá tài khoản của bác sĩ ngừng công tác; đã vô hiệu hoá thì không làm gì. */
    TaiKhoanNoiBoResponse voHieuHoa(Long id, String lyDo);

    /** Kích hoạt lại tài khoản của bác sĩ công tác lại; đang hoạt động thì không làm gì. */
    TaiKhoanNoiBoResponse kichHoatLai(Long id);

}
