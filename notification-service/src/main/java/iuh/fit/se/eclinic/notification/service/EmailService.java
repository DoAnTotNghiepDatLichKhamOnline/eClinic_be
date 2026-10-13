package iuh.fit.se.eclinic.notification.service;

import java.util.List;

import iuh.fit.se.eclinic.notification.dto.request.TaoEmailRequest;
import iuh.fit.se.eclinic.notification.dto.response.KetQuaNhanThongBaoResponse;

/**
 * Hàng chờ gửi email (bảng email_gui): nhận email từ service khác, rồi job gửi dần (xem {@code GuiEmailJob}).
 */
public interface EmailService {

    /**
     * Đưa email vào hàng chờ; {@code maNguon} đã có thì bỏ qua. Lỗi: DU_LIEU_KHONG_HOP_LE khi mẫu không có hoặc tham số
     * không dựng được email (cả lô không được nhận, service nguồn gửi lại từng dòng).
     */
    KetQuaNhanThongBaoResponse nhan(List<TaoEmailRequest> danhSach);

    /**
     * Gửi các email đến hạn, cũ nhất trước. Gửi lỗi thì hẹn lần thử sau theo {@code app.email.cac-lan-cho}; hết số lần
     * thì đánh dấu THAT_BAI.
     *
     * @return số email vừa gửi xong
     */
    int guiDangCho();

    /** Xoá email đã gửi / đã bỏ quá {@code app.email.so-ngay-giu} ngày. */
    int donDaXong();

}
