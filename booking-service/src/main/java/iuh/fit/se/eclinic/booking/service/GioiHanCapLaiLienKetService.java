package iuh.fit.se.eclinic.booking.service;

/**
 * Giới hạn số lần khách tự xin cấp lại liên kết phiếu khám (PAT-03, API công khai), đếm trên Redis theo cửa sổ cố định
 * với 2 khoá: {@code cap-lai:ip:<ip>} và {@code cap-lai:ma:<MÃ TRA CỨU viết hoa>}. Khoá theo chuỗi mã khách gửi, không
 * theo id lịch hẹn: mã không tồn tại cũng bị đếm y như mã có thật, nên không dò được mã nào có thật. Mức giới hạn:
 * {@code app.cap-lai-lien-ket}.
 */
public interface GioiHanCapLaiLienKetService {

    /**
     * Ghi nhận 1 lần gửi (đúng hay sai SĐT đều tính). Ném GUI_LAI_QUA_NHANH kèm số phút còn lại khi IP hoặc mã vượt giới
     * hạn. Redis không dùng được thì bỏ qua giới hạn (ghi WARN).
     */
    void ghiNhan(String maTraCuu, String diaChiIp);

}
