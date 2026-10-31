-- =====================================================================
-- Gộp bảng nhac_tai_kham (V21) vào ho_so_benh_an (DOANTOTNGH-19, phase 1)
--  * Mỗi hồ sơ bệnh án 1 dòng nhắc nên thành 2 cột của chính hồ sơ. "Chưa có dòng" và "ngay_da_bao NULL" vốn cùng nghĩa
--    (chưa báo ngày nào), nên không mất thông tin.
--  * Chỉ booking-service ghi 2 cột này, bằng câu UPDATE riêng sau khi khoá dòng hồ sơ bệnh án; medical-service không
--    đọc / ghi chúng (entity map chỉ đọc), nên lần lưu hồ sơ của bác sĩ không ghi đè.
-- =====================================================================
ALTER TABLE ho_so_benh_an
    ADD COLUMN tai_kham_ngay_da_bao   DATE    NULL COMMENT 'ngày tái khám đã báo cho bệnh nhân; NULL = chưa báo / bác sĩ đã xoá ngày',
    ADD COLUMN tai_kham_da_nhac_truoc BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'lần nhắc trước tai_kham_ngay_da_bao đã xử lý';

UPDATE ho_so_benh_an b
    JOIN nhac_tai_kham n ON n.id_ho_so_benh_an = b.id_ho_so_benh_an
SET b.tai_kham_ngay_da_bao   = n.ngay_da_bao,
    b.tai_kham_da_nhac_truoc = n.da_nhac_truoc;

DROP TABLE nhac_tai_kham;
