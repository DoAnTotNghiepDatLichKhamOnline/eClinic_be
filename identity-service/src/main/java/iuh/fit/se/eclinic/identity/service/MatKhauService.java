package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.identity.dto.request.DoiMatKhauRequest;

/** Quên / đặt lại mật khẩu qua liên kết email và đổi mật khẩu. */
public interface MatKhauService {

    /** Gửi liên kết đặt lại mật khẩu nếu email thuộc tài khoản đã kích hoạt và đã hết thời gian chờ. */
    void quenMatKhau(String email);

    /** Đặt mật khẩu mới, đăng xuất mọi thiết bị, xoá khoá đăng nhập sai và gửi email thông báo. */
    void datLaiMatKhau(String token, String matKhauMoi);

    /** Đổi mật khẩu của người đang đăng nhập: phải nhập đúng mật khẩu hiện tại. */
    void doiMatKhau(Long idTaiKhoan, String maPhienHienTai, DoiMatKhauRequest request);

    /** Hỏi lại mật khẩu hiện tại trước thao tác nhạy cảm (đổi mật khẩu, đổi email). */
    void xacNhanMatKhauHienTai(TaiKhoan taiKhoan, String matKhau);

}
