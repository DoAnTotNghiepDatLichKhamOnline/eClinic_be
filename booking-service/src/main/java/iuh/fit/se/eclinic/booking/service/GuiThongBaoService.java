package iuh.fit.se.eclinic.booking.service;

/**
 * Gửi các thông báo đang chờ trong bảng su_kien_thong_bao sang notification-service (xem {@code GuiThongBaoJob}).
 */
public interface GuiThongBaoService {

    /**
     * Gửi tối đa 100 sự kiện chưa gửi, cũ nhất trước. notification-service không nhận được thì các dòng giữ nguyên, lần
     * sau gửi lại.
     *
     * @return số sự kiện vừa gửi xong
     */
    int guiDangCho();

    /** Xoá các sự kiện đã gửi quá 7 ngày. */
    int donDaGui();

}
