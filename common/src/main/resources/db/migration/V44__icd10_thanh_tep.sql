-- =====================================================================
-- Danh mục ICD-10 ra khỏi DB (DOANTOTNGH-19, phase 5)
--  * Bảng icd10 (V30) chỉ đọc, chỉ V30 ghi. Danh mục nay nằm trong tệp common/src/main/resources/danh-muc/icd10.tsv
--    (cùng các cột ma, ten, ten_en, man_tinh, theo thứ tự mã), nạp 1 lần bởi DanhMucIcd10. Muốn thêm / sửa mã thì sửa tệp.
--  * ho_so_benh_an.ma_icd_chinh giữ nguyên, thôi là khoá ngoại: service kiểm tra mã có trong danh mục trước khi ghi.
-- =====================================================================
ALTER TABLE ho_so_benh_an DROP FOREIGN KEY fk_ho_so_benh_an_icd10;
ALTER TABLE ho_so_benh_an DROP INDEX fk_ho_so_benh_an_icd10;

DROP TABLE icd10;
