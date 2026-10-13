package iuh.fit.se.eclinic.chatbot.service;

/**
 * Giới hạn dùng chatbot, đếm trên Redis (cửa sổ cố định). {@code chuThe} là tài khoản đang đăng nhập hoặc địa chỉ IP
 * của khách: mã bí mật của phiên khách tạo mới miễn phí nên không đếm theo nó được. Vượt giới hạn: CHAT_QUA_NHIEU (429).
 * Redis không dùng được thì bỏ qua giới hạn, như API đặt lịch.
 */
public interface GioiHanChatService {

    /** {@code app.chatbot.so-phien-moi-gio}. */
    void ghiNhanMoPhien(String chuThe);

    /** {@code app.chatbot.so-tin-nhan-moi-cua-so} trong {@code cua-so} và {@code so-tin-nhan-moi-ngay} trong 24 giờ. */
    void ghiNhanTinNhan(String chuThe);

    /** {@code app.chatbot.so-tin-nhan-toan-he-thong-moi-ngay}: gọi ngay trước mỗi lần gọi mô hình. */
    void ghiNhanGoiMoHinh();

}
