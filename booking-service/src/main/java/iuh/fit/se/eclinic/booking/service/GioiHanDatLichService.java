package iuh.fit.se.eclinic.booking.service;

/**
 * Giới hạn số lần gọi API đặt lịch (công khai, không cần đăng nhập) theo địa chỉ IP, đếm trên Redis với khoá
 * {@code dat-lich:ip:<ip>}. Mức giới hạn: {@code app.dat-lich.so-lan-dat-toi-da-moi-ip} lần trong
 * {@code cua-so-gioi-han-ip}.
 */
public interface GioiHanDatLichService {

    /**
     * Ghi nhận 1 lần gọi đặt lịch từ địa chỉ IP (kể cả lần đặt không thành công). Ném GUI_LAI_QUA_NHANH khi vượt giới
     * hạn. Redis không dùng được thì bỏ qua giới hạn này để việc đặt lịch không bị chặn theo.
     */
    void ghiNhan(String diaChiIp);

}
