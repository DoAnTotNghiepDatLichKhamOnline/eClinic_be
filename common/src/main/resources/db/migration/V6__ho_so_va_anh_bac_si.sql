-- =====================================================================
-- Hồ sơ giới thiệu bác sĩ + ảnh giới thiệu (DOANTOTNGH-5, phase 1)
--  * bac_si: thêm chức vụ, câu giới thiệu ngắn (hiện trên thẻ bác sĩ) và 3 mục dạng danh sách. Mỗi mục lưu văn bản
--    thuần, MỖI DÒNG 1 Ý; API trả về / nhận vào dạng mảng chuỗi. tieu_su (V1) vẫn là đoạn giới thiệu tự do.
--    Các cột đều cho NULL: bác sĩ có sẵn chưa có hồ sơ giới thiệu.
--  * anh_bac_si: ảnh giới thiệu của bác sĩ (ảnh làm việc, chứng chỉ), tối đa 12 ảnh / bác sĩ (giới hạn ở service).
--    Ảnh đại diện vẫn là tai_khoan.anh_dai_dien.
-- =====================================================================
ALTER TABLE bac_si
    ADD COLUMN chuc_vu VARCHAR(150) NULL COMMENT 'vd: Trưởng khoa Nhi' AFTER hoc_vi,
    ADD COLUMN gioi_thieu_ngan VARCHAR(300) NULL COMMENT '1-2 câu hiện trên thẻ bác sĩ' AFTER chuc_vu,
    ADD COLUMN qua_trinh_dao_tao TEXT NULL COMMENT 'mỗi dòng 1 ý' AFTER tieu_su,
    ADD COLUMN qua_trinh_cong_tac TEXT NULL COMMENT 'mỗi dòng 1 ý' AFTER qua_trinh_dao_tao,
    ADD COLUMN linh_vuc_kham_chua TEXT NULL COMMENT 'khám và điều trị, mỗi dòng 1 ý' AFTER qua_trinh_cong_tac;

CREATE TABLE anh_bac_si
(
    id_anh_bac_si BIGINT       NOT NULL AUTO_INCREMENT,
    id_bac_si     BIGINT       NOT NULL,
    loai          VARCHAR(20)  NOT NULL COMMENT 'ANH_CONG_VIEC | CHUNG_CHI',
    url           VARCHAR(500) NOT NULL,
    ma_luu_tru    VARCHAR(150) COMMENT 'mã ảnh trong kho ảnh (để xoá); NULL = ảnh nằm ngoài kho (dữ liệu mẫu)',
    chu_thich     VARCHAR(200),
    thu_tu        INT          NOT NULL COMMENT 'thứ tự hiển thị, nhỏ đứng trước',
    ngay_tao      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_anh_bac_si PRIMARY KEY (id_anh_bac_si),
    CONSTRAINT fk_anh_bac_si_bac_si FOREIGN KEY (id_bac_si) REFERENCES bac_si (id_bac_si),
    CONSTRAINT ck_anh_bac_si_loai CHECK (loai IN ('ANH_CONG_VIEC', 'CHUNG_CHI'))
);
CREATE INDEX idx_anh_bac_si_bac_si_thu_tu ON anh_bac_si (id_bac_si, thu_tu);
