package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.identity.dto.request.DoiMatKhauRequest;

/**
 * AUTH-03: quên mật khẩu / đặt lại mật khẩu qua liên kết email. Đổi mật khẩu khi đang đăng nhập.
 * Đặt lại hoặc đổi mật khẩu thành công đều huỷ yêu cầu đổi email đang chờ (xem DoiEmailService).
 */
public interface MatKhauService {

    /**
     * Gửi liên kết đặt lại mật khẩu nếu email thuộc tài khoản đã kích hoạt và đã hết thời gian chờ.
     * Không bao giờ báo lỗi: mọi email đều nhận cùng 1 câu trả lời (không lộ email nào đã đăng ký).
     */
    void quenMatKhau(String email);

    /**
     * Đặt mật khẩu mới, đăng xuất mọi thiết bị, xoá khoá đăng nhập sai và gửi email thông báo.
     * Ném LIEN_KET_KHONG_HOP_LE, TAI_KHOAN_BI_VO_HIEU_HOA.
     */
    void datLaiMatKhau(String token, String matKhauMoi);

    /**
     * Đổi mật khẩu của người đang đăng nhập: phải nhập đúng mật khẩu hiện tại. Thành công thì đăng xuất mọi thiết bị
     * khác (giữ phiên {@code maPhienHienTai}; null thì đăng xuất tất cả), xoá các bộ đếm sai mật khẩu và gửi email
     * thông báo. Sai mật khẩu hiện tại quá số lần cho phép thì khoá chức năng này (không khoá đăng nhập).
     * Ném MAT_KHAU_CU_KHONG_DUNG, MAT_KHAU_MOI_TRUNG_MAT_KHAU_CU, TAI_KHOAN_CHUA_CO_MAT_KHAU (tài khoản chỉ có
     * Google), SAI_MAT_KHAU_QUA_NHIEU và các lỗi của {@link TaiKhoanService#layDangHoatDong}.
     */
    void doiMatKhau(Long idTaiKhoan, String maPhienHienTai, DoiMatKhauRequest request);

    /**
     * Hỏi lại mật khẩu hiện tại trước thao tác nhạy cảm của người đang đăng nhập (đổi mật khẩu, đổi email). Mọi thao tác
     * dùng chung 1 bộ đếm sai theo tài khoản: sai quá số lần cho phép thì khoá cả các thao tác này (không khoá đăng
     * nhập); nhập đúng thì xoá bộ đếm. Không ghi DB.
     * Ném TAI_KHOAN_CHUA_CO_MAT_KHAU (tài khoản chỉ có Google), SAI_MAT_KHAU_QUA_NHIEU, MAT_KHAU_CU_KHONG_DUNG.
     */
    void xacNhanMatKhauHienTai(TaiKhoan taiKhoan, String matKhau);

}
