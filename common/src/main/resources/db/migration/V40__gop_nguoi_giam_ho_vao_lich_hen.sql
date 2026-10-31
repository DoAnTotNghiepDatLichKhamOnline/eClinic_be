-- =====================================================================
-- Gộp bảng nguoi_giam_ho (V4) vào lich_hen (DOANTOTNGH-19, phase 2)
--  * Người giám hộ của lượt khám là các cột giam_ho_* của chính lịch hẹn (cùng tên cột với nguoi_than_da_luu, V8); cả 4
--    cột bắt buộc NULL = lượt khám không có người giám hộ.
--  * Dùng lại người giám hộ: lần đặt sau cho cùng hồ sơ + CCCD người giám hộ chép thông tin của lịch hẹn sớm nhất trong
--    nhóm đó (thay cho dòng nguoi_giam_ho dùng chung). Mọi lần đặt cho 1 hồ sơ đều chạy dưới khoá dòng hồ sơ nên không
--    cần UNIQUE (hồ sơ, CCCD) nữa.
--  * CCCD người giám hộ trong khoá nhận diện của trẻ chưa có CCCD = giam_ho_cccd của lịch hẹn sớm nhất có người giám hộ
--    của hồ sơ (lịch hẹn không đổi hồ sơ, không bị xoá).
--  * Lúc đặt lịch các cột này được ghi cùng dòng lịch hẹn; sau đó chỉ câu UPDATE điền ngày sinh còn trống được ghi:
--    không tăng version, không đổi ngay_cap_nhat.
-- =====================================================================
ALTER TABLE lich_hen
    ADD COLUMN giam_ho_ho_ten        VARCHAR(150) NULL COMMENT 'các cột giam_ho_*: người giám hộ của lượt khám, NULL = không có' AFTER id_ho_so_benh_nhan,
    ADD COLUMN giam_ho_quan_he       VARCHAR(30)  NULL COMMENT 'CHA | ME | NGUOI_GIAM_HO_HOP_PHAP | KHAC' AFTER giam_ho_ho_ten,
    ADD COLUMN giam_ho_so_dien_thoai VARCHAR(20)  NULL AFTER giam_ho_quan_he,
    ADD COLUMN giam_ho_cccd          VARCHAR(12)  NULL AFTER giam_ho_so_dien_thoai,
    ADD COLUMN giam_ho_ngay_sinh     DATE         NULL AFTER giam_ho_cccd,
    ADD CONSTRAINT ck_lich_hen_giam_ho CHECK (
        (giam_ho_ho_ten IS NULL AND giam_ho_quan_he IS NULL AND giam_ho_so_dien_thoai IS NULL AND giam_ho_cccd IS NULL)
        OR (giam_ho_ho_ten IS NOT NULL AND giam_ho_quan_he IS NOT NULL AND giam_ho_so_dien_thoai IS NOT NULL
            AND giam_ho_cccd IS NOT NULL));

CREATE INDEX idx_lich_hen_giam_ho_cccd ON lich_hen (giam_ho_cccd, id_ho_so_benh_nhan);

UPDATE lich_hen l
    JOIN nguoi_giam_ho g ON g.id_nguoi_giam_ho = l.id_nguoi_giam_ho
SET l.giam_ho_ho_ten        = g.ho_ten,
    l.giam_ho_quan_he       = g.quan_he,
    l.giam_ho_so_dien_thoai = g.so_dien_thoai,
    l.giam_ho_cccd          = g.cccd,
    l.giam_ho_ngay_sinh     = g.ngay_sinh;

ALTER TABLE lich_hen DROP FOREIGN KEY fk_lich_hen_nguoi_giam_ho;
ALTER TABLE lich_hen DROP COLUMN id_nguoi_giam_ho;

DROP TABLE nguoi_giam_ho;
