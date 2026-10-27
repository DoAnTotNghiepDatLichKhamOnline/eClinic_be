package iuh.fit.se.eclinic.booking.service;

/** Giới hạn số lần gọi API đặt lịch công khai theo IP, đếm trên Redis. */
public interface GioiHanDatLichService {

    /** Ghi nhận 1 lần gọi đặt lịch từ địa chỉ IP (kể cả lần đặt không thành công). */
    void ghiNhan(String diaChiIp);

}
