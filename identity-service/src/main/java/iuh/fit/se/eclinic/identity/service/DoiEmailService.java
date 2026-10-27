package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.identity.dto.request.DoiEmailRequest;
import iuh.fit.se.eclinic.identity.dto.response.YeuCauDoiEmailResponse;

/** Đổi email đăng nhập của người đang đăng nhập (mọi vai trò). */
public interface DoiEmailService {

    /** Gửi liên kết xác nhận tới email mới và thông báo tới email đang dùng. */
    YeuCauDoiEmailResponse yeuCau(Long idTaiKhoan, DoiEmailRequest request);

    /** Yêu cầu đang chờ xác nhận; null nếu không có. */
    YeuCauDoiEmailResponse layYeuCauDangCho(Long idTaiKhoan);

    /** Huỷ yêu cầu đang chờ (liên kết đã gửi hết hiệu lực). Không có yêu cầu nào cũng không báo lỗi. */
    void huy(Long idTaiKhoan);

    /** Dùng token trong liên kết để đổi email. */
    void xacNhan(String token);

}
