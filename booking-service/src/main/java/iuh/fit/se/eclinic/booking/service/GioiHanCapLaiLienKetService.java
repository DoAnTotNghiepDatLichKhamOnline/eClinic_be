package iuh.fit.se.eclinic.booking.service;

/** Giới hạn số lần khách tự xin cấp lại liên kết phiếu khám, đếm trên Redis theo IP và mã tra cứu. */
public interface GioiHanCapLaiLienKetService {

    /** Ghi nhận 1 lần gửi (đúng hay sai SĐT đều tính). */
    void ghiNhan(String maTraCuu, String diaChiIp);

}
