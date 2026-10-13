package iuh.fit.se.eclinic.booking.service;

/**
 * Nhắc lịch khám trước giờ khám ở các mốc cấu hình {@code app.nhac-lich.cac-moc} (xem {@code NhacLichJob}).
 * <p>
 * Lịch hẹn được nhắc ở 1 mốc khi: còn hiệu lực (chờ xác nhận / đã xác nhận), không đang chờ đổi vì ca bị hủy, lượt khám
 * đã vào trong khoảng của mốc nhưng còn cách ít nhất {@code con-toi-thieu}, và mốc đó chưa được ghi nhận. Lịch đặt khi
 * đã ở trong khoảng của mốc thì bỏ mốc đó (người đặt vừa nhận email xác nhận). Nhiều mốc cùng đến hạn (sau ban đêm, sau
 * khi service ngừng) thì chỉ nhắc mốc gần giờ khám nhất. Mỗi mốc của 1 lịch hẹn được nhắc nhiều nhất 1 lần.
 */
public interface NhacLichService {

    /**
     * Nhắc các lịch hẹn đến hạn. Ngoài giờ gửi ({@code gui-tu} - {@code gui-den}) không làm gì.
     *
     * @return số lịch hẹn vừa được nhắc
     */
    int nhacDenHan();

}
