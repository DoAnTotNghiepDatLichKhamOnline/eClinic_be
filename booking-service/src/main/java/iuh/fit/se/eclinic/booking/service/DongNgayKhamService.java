package iuh.fit.se.eclinic.booking.service;

/**
 * Đóng các ngày khám đã qua (xem {@code DongNgayKhamJob}): bệnh nhân chưa được khám thì lịch hẹn thành KHONG_DEN.
 */
public interface DongNgayKhamService {

    /**
     * Lịch DA_XAC_NHAN có lượt khám trước 00:00 hôm nay, ca khám không bị hủy (canDoiLich = false) và bệnh nhân không ở
     * DANG_KHAM chuyển sang KHONG_DEN. Lịch bệnh nhân đã được gọi vào khám (DANG_KHAM) để mở cho bác sĩ ghi nhận kết quả
     * khám. Chạy lại bao nhiêu lần cũng vậy; lần chạy bị lỡ thì lần sau đóng bù luôn.
     *
     * @return số lịch hẹn vừa chuyển sang KHONG_DEN
     */
    int dongNgayDaQua();

}
