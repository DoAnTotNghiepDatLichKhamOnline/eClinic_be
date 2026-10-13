package iuh.fit.se.eclinic.chatbot.service;

/**
 * Người đang gửi tin nhắn, nhìn từ các hàm tra cứu dữ liệu CÁ NHÂN ({@link TraCuuPhongKham}). Dựng 1 lần trong
 * {@code ChatbotServiceImpl.guiTinNhan} từ JWT của request và chủ của phiên chat; không trường nào lấy từ lời người dùng
 * gõ hay từ tham số mô hình viết.
 *
 * @param token            JWT của request đang chat, chỉ để gọi API "của tôi" của booking-service thay mặt chính người
 *                         đó; null khi {@code duocTraCuuCaNhan} false
 * @param duocTraCuuCaNhan true khi đủ cả: tính năng đang bật, request có JWT vai trò bệnh nhân, và phiên chat thuộc
 *                         chính tài khoản đó (phiên của khách không bao giờ được, kể cả khi người cầm mã bí mật đã đăng
 *                         nhập: ai có mã cũng đọc lại được phiên)
 * @param daDongY          bệnh nhân đã đồng ý trong phiên này cho chatbot đọc lịch hẹn của mình và gửi phần tối thiểu
 *                         tới nhà cung cấp mô hình
 */
public record NguoiHoi(String token, boolean duocTraCuuCaNhan, boolean daDongY) {

    public static final NguoiHoi KHONG_CO_DU_LIEU_CA_NHAN = new NguoiHoi(null, false, false);

    /** Được đọc dữ liệu cá nhân ngay bây giờ. */
    public boolean duocDoc() {
        return duocTraCuuCaNhan && daDongY;
    }

    /** Không in token ra log dù vô tình. */
    @Override
    public String toString() {
        return "NguoiHoi[duocTraCuuCaNhan=" + duocTraCuuCaNhan + ", daDongY=" + daDongY + "]";
    }

}
