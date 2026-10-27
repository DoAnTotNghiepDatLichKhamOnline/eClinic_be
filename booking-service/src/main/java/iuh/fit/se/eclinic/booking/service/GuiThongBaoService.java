package iuh.fit.se.eclinic.booking.service;

/** Gửi các thông báo và email đang chờ trong bảng su_kien_thong_bao sang notification-service. */
public interface GuiThongBaoService {

    /** Gửi tối đa 100 sự kiện cũ nhất; gửi lỗi thì giữ nguyên để lần sau gửi lại. */
    int guiDangCho();

    /** Xoá các sự kiện đã gửi quá 7 ngày. */
    int donDaGui();

}
