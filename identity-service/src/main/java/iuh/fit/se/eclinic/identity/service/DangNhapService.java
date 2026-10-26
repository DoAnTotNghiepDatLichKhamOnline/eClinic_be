package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.identity.dto.request.DangNhapRequest;
import iuh.fit.se.eclinic.identity.dto.request.DatMatKhauLanDauRequest;
import iuh.fit.se.eclinic.identity.dto.response.DangNhapResponse;

/**
 * AUTH-02: đăng nhập, làm mới phiên (xoay vòng refresh token), đăng xuất.
 */
public interface DangNhapService {

    /**
     * Sai email hoặc mật khẩu đều trả SAI_THONG_TIN_DANG_NHAP; trạng thái tài khoản chỉ được báo khi mật khẩu đúng.
     *
     * @param thongTinThietBi User-Agent của client, lưu cùng phiên (có thể null)
     */
    DangNhapResponse dangNhap(DangNhapRequest request, String thongTinThietBi);

    /**
     * Lần đăng nhập đầu của tài khoản đang mang mật khẩu mặc định ({@code phaiDoiMatKhau}): kiểm tra mật khẩu hiện tại
     * như {@link #dangNhap} (cùng bộ đếm sai mật khẩu), đặt mật khẩu mới và gỡ cờ. Không mở phiên: người dùng đăng nhập
     * lại bằng mật khẩu mới. Ném SAI_THONG_TIN_DANG_NHAP, DANG_NHAP_SAI_QUA_NHIEU, TAI_KHOAN_BI_VO_HIEU_HOA,
     * TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE (tài khoản không cần đặt mật khẩu lần đầu), MAT_KHAU_MOI_TRUNG_MAT_KHAU_CU.
     */
    void datMatKhauLanDau(DatMatKhauLanDauRequest request);

    /**
     * Đổi refresh token lấy cặp token mới của cùng phiên đăng nhập; token cũ hết hiệu lực.
     * Token đã thu hồi bị dùng lại sau thời gian ân hạn thì phiên của token đó bị đăng xuất (các phiên khác giữ nguyên).
     */
    DangNhapResponse lamMoi(String refreshToken);

    /** Thu hồi phiên của refresh token. Token sai / đã thu hồi thì bỏ qua (không báo lỗi). */
    void dangXuat(String refreshToken);

    /**
     * Mở phiên đăng nhập mới và cấp cặp access token + refresh token cho tài khoản. Người gọi PHẢI đã xác thực tài khoản (mật khẩu, Google...)
     * và kiểm tra trạng thái của nó.
     */
    DangNhapResponse capPhien(TaiKhoan taiKhoan, String thongTinThietBi);

}
