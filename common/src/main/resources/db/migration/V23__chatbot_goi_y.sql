-- Chatbot gợi ý từ dữ liệu phòng khám (UC-CHAT-01).
-- 1) chuyen_khoa.trieu_chung / trieu_chung_en: các triệu chứng, vấn đề thường gặp mà chuyên khoa tiếp nhận, do quản trị
--    viên nhập. Chỉ đưa cho chatbot để chọn chuyên khoa theo lời người dùng kể; API công khai không trả 2 cột này.
-- 2) tin_nhan_chat.goi_y: gợi ý đi kèm câu trả lời của chatbot (chuyên khoa, bác sĩ, lịch khám còn chỗ), JSON do server
--    dựng từ kết quả tra cứu lúc trả lời. Là ảnh chụp tại thời điểm đó: số chỗ còn lại có thể đã đổi khi đọc lại.
ALTER TABLE chuyen_khoa
    ADD COLUMN trieu_chung TEXT NULL COMMENT 'triệu chứng thường gặp, cho chatbot chọn chuyên khoa' AFTER mo_ta_en,
    ADD COLUMN trieu_chung_en TEXT NULL COMMENT 'bản tiếng Anh của trieu_chung' AFTER trieu_chung;

ALTER TABLE tin_nhan_chat
    ADD COLUMN goi_y TEXT NULL COMMENT 'JSON gợi ý kèm câu trả lời của chatbot; NULL = không có' AFTER noi_dung;
