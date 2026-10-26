-- =====================================================================
-- Thông báo lưu theo mẫu + tham số (DOANTOTNGH-9, phase 3)
--  * Từ nay 1 thông báo đi và được lưu dưới dạng khoá mẫu (khoá trong thong-diep/, vd thongBao.lichHenMoi) + tham số
--    (mảng JSON), câu chữ được soạn theo ngôn ngữ của người đọc lúc đọc.
--  * su_kien_thong_bao.noi_dung cho phép NULL: sự kiện mới chỉ có khoa_mau + tham_so; dòng cũ chưa gửi vẫn gửi được.
--  * thong_bao.noi_dung giữ NOT NULL: notification-service ghi câu tiếng Việt vào đó khi nhận (chữ của dòng cũ, và là
--    bản dự phòng). Dòng có khoa_mau thì soạn theo mẫu, dòng không có thì trả nguyên noi_dung.
-- =====================================================================
ALTER TABLE su_kien_thong_bao
    MODIFY COLUMN noi_dung TEXT NULL,
    ADD COLUMN khoa_mau VARCHAR(80) NULL COMMENT 'khoá mẫu câu trong thong-diep/' AFTER id_yeu_cau,
    ADD COLUMN tham_so TEXT NULL COMMENT 'mảng JSON các tham số của mẫu' AFTER khoa_mau;

ALTER TABLE thong_bao
    ADD COLUMN khoa_mau VARCHAR(80) NULL COMMENT 'khoá mẫu câu trong thong-diep/; NULL = thông báo cũ, đọc noi_dung' AFTER ma_nguon,
    ADD COLUMN tham_so TEXT NULL COMMENT 'mảng JSON các tham số của mẫu' AFTER khoa_mau;
