-- =====================================================================
-- Ảnh giới thiệu của bác sĩ vào bac_si (DOANTOTNGH-19, phase 5)
--  * Bảng anh_bac_si (V6, V17) thành 1 danh sách JSON trên bac_si.anh_gioi_thieu, mỗi phần tử 1 ảnh:
--    {id, loai, url, maLuuTru, chuThich, chuThichEn, thuTu, ngayTao}, xếp theo (thuTu, id). NULL = chưa có ảnh.
--  * id ảnh duy nhất trong từng bác sĩ (mọi route ảnh đều theo bác sĩ). Ảnh cũ giữ id cũ; ảnh mới lấy anh_id_cuoi + 1,
--    nên id của ảnh đã xoá không bao giờ được dùng lại cho ảnh khác của cùng bác sĩ.
--  * Chỉ catalog-service (và dữ liệu mẫu) đọc / ghi 2 cột này, bằng câu SQL riêng có khoá dòng bac_si: entity BacSi
--    không ánh xạ chúng, nên lần lưu hồ sơ bác sĩ không ghi đè danh sách ảnh.
-- =====================================================================
ALTER TABLE bac_si
    ADD COLUMN anh_gioi_thieu JSON   NULL COMMENT 'ảnh làm việc / chứng chỉ, xem V45',
    ADD COLUMN anh_id_cuoi    BIGINT NOT NULL DEFAULT 0 COMMENT 'id ảnh lớn nhất đã cấp cho bác sĩ này';

SET SESSION group_concat_max_len = 1048576;

UPDATE bac_si b
    JOIN (SELECT id_bac_si,
                 MAX(id_anh_bac_si) AS id_cuoi,
                 CAST(CONCAT('[', GROUP_CONCAT(JSON_OBJECT(
                         'id', id_anh_bac_si,
                         'loai', loai,
                         'url', url,
                         'maLuuTru', ma_luu_tru,
                         'chuThich', chu_thich,
                         'chuThichEn', chu_thich_en,
                         'thuTu', thu_tu,
                         'ngayTao', DATE_FORMAT(ngay_tao, '%Y-%m-%dT%H:%i:%s.%f'))
                         ORDER BY thu_tu, id_anh_bac_si SEPARATOR ','), ']') AS JSON) AS anh
          FROM anh_bac_si
          GROUP BY id_bac_si) a ON a.id_bac_si = b.id_bac_si
SET b.anh_gioi_thieu = a.anh,
    b.anh_id_cuoi    = a.id_cuoi;

DROP TABLE anh_bac_si;
