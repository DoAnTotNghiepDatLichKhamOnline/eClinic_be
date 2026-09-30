package iuh.fit.se.eclinic.identity.service;

/**
 * AUTH-03: quên mật khẩu / đặt lại mật khẩu qua liên kết email.
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

}
