-- =====================================================================
-- Nội dung danh mục bằng tiếng Anh (DOANTOTNGH-9, phase 2)
--  * Mỗi cột chữ dịch được có thêm 1 cột <ten>_en ngay sau nó, cùng kiểu, luôn cho phép NULL. Cột tiếng Việt vẫn là
--    cột bắt buộc; cột _en trống thì API trả tiếng Việt.
--  * Không dịch: họ tên, bac_si.hoc_vi (chữ viết tắt), thuoc.ten_thuoc.
--  * Tên tiếng Anh của chuyên khoa / phòng khám không trùng nhau (UNIQUE cho phép nhiều dòng NULL).
-- =====================================================================
ALTER TABLE chuyen_khoa
    ADD COLUMN ten_chuyen_khoa_en VARCHAR(150) NULL AFTER ten_chuyen_khoa,
    ADD COLUMN mo_ta_en TEXT NULL AFTER mo_ta,
    ADD CONSTRAINT uk_chuyen_khoa_ten_en UNIQUE (ten_chuyen_khoa_en);

ALTER TABLE phong_kham
    ADD COLUMN ten_phong_en VARCHAR(100) NULL COMMENT 'vd: Room 203' AFTER ten_phong,
    ADD COLUMN tang_en VARCHAR(20) NULL AFTER tang,
    ADD CONSTRAINT uk_phong_kham_ten_en UNIQUE (ten_phong_en);

ALTER TABLE bac_si
    ADD COLUMN chuc_vu_en VARCHAR(150) NULL AFTER chuc_vu,
    ADD COLUMN gioi_thieu_ngan_en VARCHAR(300) NULL AFTER gioi_thieu_ngan,
    ADD COLUMN tieu_su_en TEXT NULL AFTER tieu_su,
    ADD COLUMN qua_trinh_dao_tao_en TEXT NULL COMMENT 'mỗi dòng 1 ý' AFTER qua_trinh_dao_tao,
    ADD COLUMN qua_trinh_cong_tac_en TEXT NULL COMMENT 'mỗi dòng 1 ý' AFTER qua_trinh_cong_tac,
    ADD COLUMN linh_vuc_kham_chua_en TEXT NULL COMMENT 'mỗi dòng 1 ý' AFTER linh_vuc_kham_chua;

ALTER TABLE anh_bac_si
    ADD COLUMN chu_thich_en VARCHAR(200) NULL AFTER chu_thich;

ALTER TABLE thuoc
    ADD COLUMN don_vi_en VARCHAR(30) NULL COMMENT 'tablet, ml, sachet...' AFTER don_vi,
    ADD COLUMN mo_ta_en TEXT NULL AFTER mo_ta;
