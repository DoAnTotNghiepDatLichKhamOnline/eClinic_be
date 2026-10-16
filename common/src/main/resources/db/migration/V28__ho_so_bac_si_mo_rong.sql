-- =====================================================================
-- Hồ sơ bác sĩ mở rộng + bác sĩ tự sửa 1 phần hồ sơ (DOANTOTNGH-10, phase 4)
--  * Giấy phép hành nghề: ngày cấp, nơi cấp, phạm vi hành nghề (cạnh so_giay_phep). Chỉ quản trị viên sửa, hiện công khai.
--  * nam_bat_dau_hanh_nghe thay so_nam_kinh_nghiem: số năm kinh nghiệm suy ra từ năm bắt đầu nên không bị cũ theo thời
--    gian. Dữ liệu cũ đổi thành năm hiện tại - số năm. Năm không được ở tương lai: kiểm tra trong service (CHECK không
--    gọi được CURDATE()).
--  * ngon_ngu_kham: mã ngôn ngữ ISO 639 viết thường, cách nhau dấu phẩy (vd "vi,en"); API trả về dạng mảng.
--  * giai_thuong, cong_trinh_nghien_cuu: mỗi dòng 1 ý như qua_trinh_dao_tao. Bác sĩ tự sửa cùng giới thiệu ngắn, tiểu sử,
--    ngôn ngữ; quản trị viên sửa được mọi mục.
--  * phien_ban_ho_so: tăng 1 mỗi lần lưu hồ sơ giới thiệu (quản trị viên hoặc bác sĩ); lần lưu gửi phiên bản đã cũ bị từ
--    chối để không đè lên lần lưu của người kia. Đổi trạng thái / chuyên khoa không tăng.
-- =====================================================================
ALTER TABLE bac_si
    ADD COLUMN ngay_cap_giay_phep       DATE         NULL AFTER so_giay_phep,
    ADD COLUMN noi_cap_giay_phep        VARCHAR(200) NULL AFTER ngay_cap_giay_phep,
    ADD COLUMN noi_cap_giay_phep_en     VARCHAR(200) NULL AFTER noi_cap_giay_phep,
    ADD COLUMN pham_vi_hanh_nghe        VARCHAR(500) NULL AFTER noi_cap_giay_phep_en,
    ADD COLUMN pham_vi_hanh_nghe_en     VARCHAR(500) NULL AFTER pham_vi_hanh_nghe,
    ADD COLUMN nam_bat_dau_hanh_nghe    INT          NULL AFTER linh_vuc_kham_chua_en,
    ADD COLUMN ngon_ngu_kham            VARCHAR(50)  NULL COMMENT 'mã ISO 639, cách nhau dấu phẩy' AFTER nam_bat_dau_hanh_nghe,
    ADD COLUMN giai_thuong              TEXT         NULL COMMENT 'mỗi dòng 1 ý' AFTER ngon_ngu_kham,
    ADD COLUMN giai_thuong_en           TEXT         NULL COMMENT 'mỗi dòng 1 ý' AFTER giai_thuong,
    ADD COLUMN cong_trinh_nghien_cuu    TEXT         NULL COMMENT 'mỗi dòng 1 ý' AFTER giai_thuong_en,
    ADD COLUMN cong_trinh_nghien_cuu_en TEXT         NULL COMMENT 'mỗi dòng 1 ý' AFTER cong_trinh_nghien_cuu,
    ADD COLUMN phien_ban_ho_so          INT          NOT NULL DEFAULT 1 AFTER cong_trinh_nghien_cuu_en;

UPDATE bac_si
SET nam_bat_dau_hanh_nghe = YEAR(CURDATE()) - so_nam_kinh_nghiem
WHERE so_nam_kinh_nghiem IS NOT NULL;

ALTER TABLE bac_si DROP CHECK ck_bac_si_kinh_nghiem;
ALTER TABLE bac_si DROP COLUMN so_nam_kinh_nghiem;
ALTER TABLE bac_si
    ADD CONSTRAINT ck_bac_si_nam_hanh_nghe CHECK (nam_bat_dau_hanh_nghe IS NULL OR nam_bat_dau_hanh_nghe >= 1950);
