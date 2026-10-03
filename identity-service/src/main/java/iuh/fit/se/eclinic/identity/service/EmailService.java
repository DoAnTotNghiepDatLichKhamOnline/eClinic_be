package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.identity.event.EmailDaDoiEmailEvent;
import iuh.fit.se.eclinic.identity.event.EmailDatLaiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailDoiMatKhauEvent;
import iuh.fit.se.eclinic.identity.event.EmailKichHoatLaiTaiKhoanEvent;
import iuh.fit.se.eclinic.identity.event.EmailVoHieuHoaTaiKhoanEvent;
import iuh.fit.se.eclinic.identity.event.EmailXacNhanDoiEmailEvent;
import iuh.fit.se.eclinic.identity.event.EmailXacThucEvent;
import iuh.fit.se.eclinic.identity.event.EmailYeuCauDoiEmailEvent;

/**
 * Gửi email của identity-service. Chạy nền sau khi transaction commit; lỗi gửi chỉ ghi log, không trả về cho người dùng
 * (người dùng bấm "gửi lại" nếu không nhận được).
 */
public interface EmailService {

    void guiEmailXacThuc(EmailXacThucEvent event);

    void guiEmailDatLaiMatKhau(EmailDatLaiMatKhauEvent event);

    /** Báo mật khẩu vừa được đổi. Không chứa liên kết hay token. */
    void guiThongBaoDoiMatKhau(EmailDoiMatKhauEvent event);

    /** Liên kết xác nhận đổi email, gửi tới địa chỉ mới. Không nêu họ tên hay email cũ của chủ tài khoản. */
    void guiEmailXacNhanDoiEmail(EmailXacNhanDoiEmailEvent event);

    /** Báo cho địa chỉ đang dùng rằng có yêu cầu đổi email. Không chứa liên kết hay token. */
    void guiThongBaoYeuCauDoiEmail(EmailYeuCauDoiEmailEvent event);

    /** Báo cho địa chỉ cũ rằng email đăng nhập đã đổi. Không chứa liên kết hay token. */
    void guiThongBaoDaDoiEmail(EmailDaDoiEmailEvent event);

    /** Báo tài khoản vừa bị quản trị viên vô hiệu hoá, kèm lý do. Không chứa liên kết hay token. */
    void guiThongBaoVoHieuHoa(EmailVoHieuHoaTaiKhoanEvent event);

    /** Báo tài khoản vừa được quản trị viên kích hoạt lại. Không chứa liên kết hay token. */
    void guiThongBaoKichHoatLai(EmailKichHoatLaiTaiKhoanEvent event);

}
