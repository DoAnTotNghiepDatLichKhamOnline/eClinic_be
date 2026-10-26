package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.identity.dto.request.SuaThongTinTaiKhoanRequest;
import iuh.fit.se.eclinic.identity.dto.request.TaoTaiKhoanBacSiRequest;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanNoiBoResponse;

/**
 * Việc catalog-service nhờ làm trên tài khoản của bác sĩ (API nội bộ /noi-bo/tai-khoan, phase 6). catalog-service đã
 * kiểm tra người gọi là quản trị viên; ở đây chỉ nhận tài khoản vai trò BAC_SI (tài khoản khác: KHONG_TIM_THAY).
 */
public interface TaiKhoanNoiBoService {

    /**
     * Tạo tài khoản BAC_SI đã kích hoạt, mật khẩu mặc định, phải đặt mật khẩu mới ở lần đăng nhập đầu. Gửi email chào.
     * Ném EMAIL_DA_TON_TAI, SO_DIEN_THOAI_DA_TON_TAI.
     */
    TaiKhoanNoiBoResponse taoTaiKhoanBacSi(TaoTaiKhoanBacSiRequest request);

    /**
     * Xoá tài khoản vừa tạo khi catalog-service không tạo được hồ sơ bác sĩ. Chỉ xoá tài khoản chưa có hồ sơ bác sĩ và
     * chưa từng đặt mật khẩu; không còn tài khoản thì coi như đã xoá. Ném TAI_KHOAN_DANG_DUOC_SU_DUNG.
     */
    void xoaTaiKhoanBacSi(Long id);

    /** Sửa họ tên, số điện thoại. Ném SO_DIEN_THOAI_DA_TON_TAI. */
    TaiKhoanNoiBoResponse suaThongTin(Long id, SuaThongTinTaiKhoanRequest request);

    /**
     * Vô hiệu hoá tài khoản của bác sĩ ngừng công tác; đã vô hiệu hoá thì không làm gì. Không kiểm tra lịch hẹn sắp tới
     * như API của quản trị viên: catalog-service đã hủy các ca sắp tới của bác sĩ trước khi gọi.
     */
    TaiKhoanNoiBoResponse voHieuHoa(Long id, String lyDo);

    /** Kích hoạt lại tài khoản của bác sĩ công tác lại; đang hoạt động thì không làm gì. */
    TaiKhoanNoiBoResponse kichHoatLai(Long id);

}
