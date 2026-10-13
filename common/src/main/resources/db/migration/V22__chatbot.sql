-- Chatbot (UC-CHAT-01): mỗi tin nhắn có loại để frontend hiển thị đúng kiểu và lịch sử đọc lại vẫn như lúc trả lời.
--   CAU_HOI: tin của người dùng. Tin của chatbot: LOI_CHAO (mở phiên), TRA_LOI (mô hình trả lời),
--   NGOAI_PHAM_VI (câu từ chối cố định), KHAN_CAP (câu "gọi 115" cố định, không qua mô hình).
-- phien_chat.ma_dinh_danh_khach từ nay chứa SHA-256 (hex, 64 ký tự) của mã bí mật server cấp cho khách khi mở phiên;
-- độ dài cột không đổi.
ALTER TABLE tin_nhan_chat
    ADD COLUMN loai VARCHAR(20) NOT NULL DEFAULT 'TRA_LOI'
        COMMENT 'CAU_HOI | LOI_CHAO | TRA_LOI | NGOAI_PHAM_VI | KHAN_CAP' AFTER nguoi_gui;

UPDATE tin_nhan_chat SET loai = 'CAU_HOI' WHERE nguoi_gui = 'NGUOI_DUNG';
