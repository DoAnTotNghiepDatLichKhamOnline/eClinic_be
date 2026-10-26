package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.identity.dto.request.DangNhapRequest;
import iuh.fit.se.eclinic.identity.dto.request.DatMatKhauLanDauRequest;
import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;

/** Đăng nhập, làm mới phiên (xoay vòng refresh token), đăng xuất. */
public interface DangNhapService {

    /** Sai email hoặc mật khẩu đều trả SAI_THONG_TIN_DANG_NHAP; trạng thái chỉ báo khi mật khẩu đúng. */
    DangNhapResponse dangNhap(DangNhapRequest request, String thongTinThietBi);

    /** Lần đăng nhập đầu với mật khẩu mặc định: kiểm tra mật khẩu hiện tại, đặt mật khẩu mới và gỡ cờ. */
    void datMatKhauLanDau(DatMatKhauLanDauRequest request);

    /** Đổi refresh token lấy cặp token mới của cùng phiên đăng nhập; token cũ hết hiệu lực. */
    DangNhapResponse lamMoi(String refreshToken);

    /** Thu hồi phiên của refresh token. Token sai / đã thu hồi thì bỏ qua (không báo lỗi). */
    void dangXuat(String refreshToken);

    /** Mở phiên đăng nhập mới và cấp cặp access token + refresh token cho tài khoản. */
    DangNhapResponse capPhien(TaiKhoan taiKhoan, String thongTinThietBi);

}
