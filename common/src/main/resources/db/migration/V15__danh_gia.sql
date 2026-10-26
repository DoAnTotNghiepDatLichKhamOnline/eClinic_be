-- =====================================================================
-- Bệnh nhân đánh giá lượt khám đã hoàn thành (DOANTOTNGH-7, phase 8)
--  * danh_gia: mỗi lịch hẹn tối đa 1 đánh giá (UNIQUE id_lich_hen). id_bac_si lặp lại từ lịch hẹn để tính điểm trung
--    bình của bác sĩ không phải join. Nhận xét chỉ bác sĩ và quản trị viên đọc; công khai chỉ có điểm trung bình và số
--    lượt đánh giá. Do booking-service ghi; catalog-service chỉ đọc.
-- =====================================================================
CREATE TABLE danh_gia
(
    id_danh_gia   BIGINT      NOT NULL AUTO_INCREMENT,
    id_lich_hen   BIGINT      NOT NULL COMMENT 'lượt khám được đánh giá',
    id_bac_si     BIGINT      NOT NULL COMMENT 'bác sĩ của lượt khám',
    id_tai_khoan  BIGINT      NOT NULL COMMENT 'tài khoản bệnh nhân đã gửi đánh giá',
    so_sao        INT         NOT NULL COMMENT '1..5',
    nhan_xet      VARCHAR(1000),
    ngay_tao      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ngay_cap_nhat DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_danh_gia PRIMARY KEY (id_danh_gia),
    CONSTRAINT uk_danh_gia_lich_hen UNIQUE (id_lich_hen),
    CONSTRAINT fk_danh_gia_lich_hen FOREIGN KEY (id_lich_hen) REFERENCES lich_hen (id_lich_hen),
    CONSTRAINT fk_danh_gia_bac_si FOREIGN KEY (id_bac_si) REFERENCES bac_si (id_bac_si),
    CONSTRAINT fk_danh_gia_tai_khoan FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan (id_tai_khoan),
    CONSTRAINT ck_danh_gia_so_sao CHECK (so_sao BETWEEN 1 AND 5)
);

CREATE INDEX idx_danh_gia_bac_si ON danh_gia (id_bac_si, ngay_tao);
