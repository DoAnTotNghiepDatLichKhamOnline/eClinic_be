-- =====================================================================
-- Hàng đợi khám trong ngày theo ca (DOANTOTNGH-10, phase 3)
--  * lich_hen.trang_thai_hang_doi: CHO_KHAM (mặc định) / DANG_KHAM (bác sĩ đã gọi vào) / VANG_MAT (gọi mà không có mặt,
--    gọi lại được). "Đã khám" không lưu ở đây: suy ra từ trang_thai = DA_HOAN_THANH (medical-service ghi nhận kết quả
--    khám chỉ đổi trang_thai). Chỉ booking-service ghi cột này.
--  * lich_hen.goi_luc: lần gần nhất bác sĩ gọi bệnh nhân vào khám.
--  * Mỗi bác sĩ chỉ 1 bệnh nhân DANG_KHAM chưa khám xong trong ngày: kiểm tra trong service khi đã khoá dòng bác sĩ
--    (không làm UNIQUE được vì khoá phải gồm cả ngày khám).
-- =====================================================================
ALTER TABLE lich_hen
    ADD COLUMN trang_thai_hang_doi VARCHAR(20) NOT NULL DEFAULT 'CHO_KHAM'
        COMMENT 'CHO_KHAM / DANG_KHAM / VANG_MAT; đã khám suy ra từ trang_thai = DA_HOAN_THANH' AFTER can_doi_lich,
    ADD COLUMN goi_luc             DATETIME(6) NULL COMMENT 'lần gần nhất bác sĩ gọi vào khám' AFTER trang_thai_hang_doi,
    ADD INDEX idx_lich_hen_bac_si_hang_doi (id_bac_si, trang_thai_hang_doi);
