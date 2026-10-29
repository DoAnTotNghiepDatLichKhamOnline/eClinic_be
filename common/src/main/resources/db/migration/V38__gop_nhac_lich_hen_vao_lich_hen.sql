-- =====================================================================
-- Gộp bảng nhac_lich_hen (V20) vào lich_hen (DOANTOTNGH-19, phase 1)
--  * Xử lý 1 mốc nhắc thì các mốc xa giờ khám hơn cũng coi như đã xử lý, nên các mốc đã xử lý của 1 lịch hẹn luôn là
--    "mọi mốc >= mốc nhỏ nhất đã xử lý". Thay các dòng (lịch hẹn, mốc) bằng 2 cột:
--      - nhac_moc_da_xu_ly: mốc nhỏ nhất (số phút trước giờ khám) đã xử lý; NULL = chưa xử lý mốc nào
--      - nhac_moc_da_gui:   mốc nhỏ nhất đã thật sự gửi nhắc; NULL = chưa nhắc lần nào
--  * Job vẫn khoá dòng lịch hẹn như trước và chỉ ghi 2 cột này bằng câu UPDATE riêng (không tăng version, không đổi
--    ngay_cap_nhat), trong cùng transaction với các dòng chờ gửi.
-- =====================================================================
ALTER TABLE lich_hen
    ADD COLUMN nhac_moc_da_xu_ly INT NULL COMMENT 'mốc nhắc nhỏ nhất đã xử lý (phút trước giờ khám); mọi mốc >= nó đã xử lý',
    ADD COLUMN nhac_moc_da_gui   INT NULL COMMENT 'mốc nhắc nhỏ nhất đã thật sự gửi; NULL = chưa nhắc';

UPDATE lich_hen l
    JOIN (SELECT id_lich_hen,
                 MIN(so_phut_truoc)                                  AS da_xu_ly,
                 MIN(CASE WHEN da_gui THEN so_phut_truoc END)        AS da_gui
          FROM nhac_lich_hen
          GROUP BY id_lich_hen) n ON n.id_lich_hen = l.id_lich_hen
SET l.nhac_moc_da_xu_ly = n.da_xu_ly,
    l.nhac_moc_da_gui   = n.da_gui;

DROP TABLE nhac_lich_hen;
