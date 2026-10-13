-- =====================================================================
-- Ngôn ngữ ưa dùng của tài khoản (DOANTOTNGH-9, phase 1)
--  * tai_khoan.ngon_ngu: 'VI' hoặc 'EN'. Chỉ dùng khi soạn nội dung không gắn với request nào (email, nhắc lịch);
--    thông điệp của API theo header Accept-Language của từng request. Tài khoản đã có: tiếng Việt.
-- =====================================================================
ALTER TABLE tai_khoan
    ADD COLUMN ngon_ngu VARCHAR(5) NOT NULL DEFAULT 'VI'
        COMMENT 'Ngôn ngữ ưa dùng: VI hoặc EN (email, nhắc lịch)' AFTER trang_thai;
