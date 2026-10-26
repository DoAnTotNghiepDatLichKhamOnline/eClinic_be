-- =====================================================================
-- Phiên đăng nhập có mã ổn định (DOANTOTNGH-3, phase 3)
--  * ma_phien: mã của 1 lần đăng nhập (1 thiết bị). Mỗi lần làm mới, dòng refresh_token mới giữ nguyên mã này,
--    nên danh sách thiết bị và access token (claim "phien") chỉ tới cùng 1 phiên dù token đã xoay vòng.
--  * ngay_dang_nhap: thời điểm đăng nhập ban đầu của phiên (ngay_tao là thời điểm làm mới gần nhất).
--  * Dòng có sẵn: mỗi dòng thành 1 phiên riêng với mã 'cu-<id>', ngay_dang_nhap = ngay_tao.
-- =====================================================================
ALTER TABLE refresh_token
    ADD COLUMN ma_phien VARCHAR(36) NULL AFTER id_tai_khoan,
    ADD COLUMN ngay_dang_nhap DATETIME(6) NULL AFTER ngay_thu_hoi;

UPDATE refresh_token
SET ma_phien       = CONCAT('cu-', id_refresh_token),
    ngay_dang_nhap = ngay_tao;

ALTER TABLE refresh_token
    MODIFY COLUMN ma_phien VARCHAR(36) NOT NULL COMMENT 'Mã phiên đăng nhập, giữ nguyên qua các lần làm mới',
    MODIFY COLUMN ngay_dang_nhap DATETIME(6) NOT NULL COMMENT 'Thời điểm đăng nhập ban đầu của phiên';

CREATE INDEX idx_refresh_token_ma_phien ON refresh_token (ma_phien);
