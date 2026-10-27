package iuh.fit.se.eclinic.booking.service;

/** Nhắc lịch khám trước giờ khám ở các mốc cấu hình {@code app.nhac-lich.cac-moc} (xem {@code NhacLichJob}). */
public interface NhacLichService {

    /** Nhắc các lịch hẹn đến hạn. */
    int nhacDenHan();

}
