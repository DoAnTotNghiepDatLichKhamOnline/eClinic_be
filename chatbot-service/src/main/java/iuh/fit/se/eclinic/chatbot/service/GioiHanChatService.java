package iuh.fit.se.eclinic.chatbot.service;

/** Giới hạn dùng chatbot, đếm trên Redis (cửa sổ cố định). */
public interface GioiHanChatService {

    /** {@code app.chatbot.so-phien-moi-gio}. */
    void ghiNhanMoPhien(String chuThe);

    /** {@code app.chatbot.so-tin-nhan-moi-cua-so} trong {@code cua-so} và {@code so-tin-nhan-moi-ngay} trong 24 giờ. */
    void ghiNhanTinNhan(String chuThe);

    /** {@code app.chatbot.so-tin-nhan-toan-he-thong-moi-ngay}: gọi ngay trước mỗi lần gọi mô hình. */
    void ghiNhanGoiMoHinh();

}
