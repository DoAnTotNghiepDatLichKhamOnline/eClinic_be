package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.identity.dto.request.DoiEmailRequest;
import iuh.fit.se.eclinic.identity.dto.response.YeuCauDoiEmailResponse;

/**
 * Đổi email đăng nhập của người đang đăng nhập (mọi vai trò). Email chỉ đổi khi liên kết gửi tới địa chỉ MỚI được xác
 * nhận; địa chỉ đang dùng chỉ nhận thông báo. Email mới nằm trong chính token của liên kết (xem
 * {@link TokenLienKetService}), nên liên kết chỉ áp dụng được đúng địa chỉ mà nó được gửi tới.
 * <p>
 * Mỗi tài khoản có nhiều nhất 1 yêu cầu đang chờ. Yêu cầu bị huỷ khi: có yêu cầu mới, người dùng tự huỷ, mật khẩu được
 * đổi / đặt lại (xem {@link MatKhauService}), hoặc liên kết hết hạn.
 * <p>
 * 3 method "của tôi" ném lỗi của {@link TaiKhoanService#layDangHoatDong} khi tài khoản không còn / bị vô hiệu hoá.
 */
public interface DoiEmailService {

    /**
     * Gửi liên kết xác nhận tới email mới và thông báo tới email đang dùng; liên kết của yêu cầu trước (nếu có) hết hiệu
     * lực. Chưa đổi gì trong DB. Thứ tự kiểm tra: mật khẩu hiện tại ({@link MatKhauService#xacNhanMatKhauHienTai}) rồi
     * mới tới email mới, để không lộ email nào đã đăng ký cho người chưa nhập đúng mật khẩu.
     * Ném EMAIL_MOI_TRUNG_EMAIL_CU, EMAIL_DA_TON_TAI, GUI_LAI_QUA_NHANH (yêu cầu trước chưa hết thời gian chờ).
     *
     * @return email mới sau khi chuẩn hoá
     */
    YeuCauDoiEmailResponse yeuCau(Long idTaiKhoan, DoiEmailRequest request);

    /** Yêu cầu đang chờ xác nhận; null nếu không có. */
    YeuCauDoiEmailResponse layYeuCauDangCho(Long idTaiKhoan);

    /** Huỷ yêu cầu đang chờ (liên kết đã gửi hết hiệu lực). Không có yêu cầu nào cũng không báo lỗi. */
    void huy(Long idTaiKhoan);

    /**
     * Dùng token trong liên kết để đổi email. Không cần đăng nhập: token là bằng chứng. Thành công thì đăng xuất mọi
     * thiết bị, huỷ liên kết đặt lại mật khẩu đang chờ (đã gửi tới email cũ) và gửi thông báo tới email cũ.
     * Ném LIEN_KET_KHONG_HOP_LE, TAI_KHOAN_BI_VO_HIEU_HOA, EMAIL_DA_TON_TAI (email mới vừa bị tài khoản khác dùng;
     * token đã bị tiêu, phải yêu cầu lại).
     */
    void xacNhan(String token);

}
