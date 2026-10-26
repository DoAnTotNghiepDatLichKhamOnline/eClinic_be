package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.identity.dto.request.DangKyRequest;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanResponse;

/** Đăng ký và kích hoạt tài khoản bệnh nhân qua liên kết email. */
public interface XacThucService {

    /** Tạo tài khoản CHO_XAC_NHAN và gửi liên kết kích hoạt. */
    TaiKhoanResponse dangKy(DangKyRequest request);

    /** Kích hoạt tài khoản; đã kích hoạt rồi thì coi như thành công. */
    void xacThucEmail(String token);

    /** Gửi lại liên kết nếu email đang chờ kích hoạt và đã hết thời gian chờ; không bao giờ báo lỗi. */
    void guiLaiXacThuc(String email);

}
