-- =====================================================================
-- Gộp bảng danh_gia (V15) vào lich_hen (DOANTOTNGH-19, phase 1)
--  * Mỗi lịch hẹn tối đa 1 đánh giá nên đánh giá là các cột danh_gia_* của chính lịch hẹn; danh_gia_so_sao NULL = chưa
--    đánh giá. Bác sĩ được đánh giá là lich_hen.id_bac_si (đổi bác sĩ của ca chỉ áp cho lịch hẹn còn hiệu lực, lịch đã
--    khám xong không đổi bác sĩ nữa).
--  * Chỉ ghi bằng câu UPDATE riêng các cột danh_gia_*: không tăng version, không đổi ngay_cap_nhat của lịch hẹn.
--  * Mã đánh giá trả cho quản trị viên từ nay là id_lich_hen.
-- =====================================================================
ALTER TABLE lich_hen
    ADD COLUMN danh_gia_so_sao        INT           NULL COMMENT 'đánh giá của bệnh nhân: 1..5; NULL = chưa đánh giá',
    ADD COLUMN danh_gia_nhan_xet      VARCHAR(1000) NULL COMMENT 'nhận xét kèm đánh giá; chỉ bác sĩ và quản trị viên đọc',
    ADD COLUMN danh_gia_id_tai_khoan  BIGINT        NULL COMMENT 'tài khoản đã gửi đánh giá (bệnh nhân hoặc người đặt)',
    ADD COLUMN danh_gia_ngay_tao      DATETIME(6)   NULL COMMENT 'lúc gửi đánh giá; hạn sửa tính từ đây',
    ADD COLUMN danh_gia_ngay_cap_nhat DATETIME(6)   NULL COMMENT 'lần sửa đánh giá gần nhất',
    ADD CONSTRAINT fk_lich_hen_danh_gia_tai_khoan FOREIGN KEY (danh_gia_id_tai_khoan) REFERENCES tai_khoan (id_tai_khoan),
    ADD CONSTRAINT ck_lich_hen_danh_gia_so_sao CHECK (danh_gia_so_sao BETWEEN 1 AND 5);

CREATE INDEX idx_lich_hen_danh_gia ON lich_hen (id_bac_si, danh_gia_ngay_tao);

UPDATE lich_hen l
    JOIN danh_gia d ON d.id_lich_hen = l.id_lich_hen
SET l.danh_gia_so_sao        = d.so_sao,
    l.danh_gia_nhan_xet      = d.nhan_xet,
    l.danh_gia_id_tai_khoan  = d.id_tai_khoan,
    l.danh_gia_ngay_tao      = d.ngay_tao,
    l.danh_gia_ngay_cap_nhat = d.ngay_cap_nhat;

DROP TABLE danh_gia;
