package iuh.fit.se.eclinic.notification.service;

import java.util.List;

import iuh.fit.se.eclinic.notification.dto.request.TaoEmailRequest;
import iuh.fit.se.eclinic.notification.dto.response.KetQuaNhanThongBaoResponse;

/** Hàng chờ gửi email (hàng EMAIL_GUI của bảng hang_cho_gui): nhận email từ service khác, rồi job gửi dần (xem {@code GuiEmailJob}). */
public interface EmailService {

    /** Đưa email vào hàng chờ; {@code maNguon} đã có thì bỏ qua. */
    KetQuaNhanThongBaoResponse nhan(List<TaoEmailRequest> danhSach);

    /** Gửi các email đến hạn, cũ nhất trước. */
    int guiDangCho();

    /** Xoá email đã gửi / đã bỏ quá {@code app.email.so-ngay-giu} ngày. */
    int donDaXong();

}
