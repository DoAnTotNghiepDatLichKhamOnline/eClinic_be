package iuh.fit.se.eclinic.booking.service;

/** Báo và nhắc ngày tái khám bác sĩ đề xuất trong hồ sơ bệnh án (xem {@code NhacTaiKhamJob}). */
public interface NhacTaiKhamService {

    /** Báo và nhắc các ngày tái khám đến hạn. */
    int xuLyDenHan();

}
