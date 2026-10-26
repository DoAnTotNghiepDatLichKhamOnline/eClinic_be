package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.identity.dto.request.DangKyRequest;
import iuh.fit.se.eclinic.identity.dto.response.TaiKhoanResponse;

/**
 * Đăng ký và kích hoạt tài khoản bệnh nhân qua liên kết email (AUTH-01).
 */
public interface XacThucService {

    /**
     * Tạo tài khoản CHO_XAC_NHAN và gửi liên kết kích hoạt. Email đang chờ kích hoạt thì ghi đè họ tên, số điện thoại,
     * mật khẩu và gửi liên kết mới (liên kết cũ hết hiệu lực).
     * Ném EMAIL_DA_TON_TAI, SO_DIEN_THOAI_DA_TON_TAI, GUI_LAI_QUA_NHANH (còn trong thời gian chờ).
     */
    TaiKhoanResponse dangKy(DangKyRequest request);

    /** Kích hoạt tài khoản. Ném LIEN_KET_KHONG_HOP_LE, TAI_KHOAN_BI_VO_HIEU_HOA. Đã kích hoạt rồi thì coi như thành công. */
    void xacThucEmail(String token);

    /** Gửi lại liên kết nếu email tồn tại, đang chờ kích hoạt và đã hết thời gian chờ. Không bao giờ báo lỗi. */
    void guiLaiXacThuc(String email);

}
