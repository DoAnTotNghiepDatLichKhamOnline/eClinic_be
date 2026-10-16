-- =====================================================================
-- Lịch sử sửa hồ sơ bệnh án (DOANTOTNGH-10, phase 2)
--  * Mỗi lần bác sĩ sửa hồ sơ bệnh án phải ghi lý do; hệ thống thêm 1 dòng lich_su_sua_benh_an giữ nội dung trước và sau
--    khi sửa (chẩn đoán, lời dặn, ngày tái khám, đơn thuốc). Dòng lịch sử chỉ được thêm, không sửa, không xoá.
--  * ho_so_benh_an.phien_ban: 1 khi ghi nhận, tăng 1 mỗi lần sửa; client gửi lại phiên bản đang xem, lệch thì bị từ chối
--    (2 người sửa cùng lúc không đè lên nhau).
-- =====================================================================
ALTER TABLE ho_so_benh_an
    ADD COLUMN phien_ban     INT         NOT NULL DEFAULT 1 COMMENT '1 khi ghi nhận, tăng 1 mỗi lần sửa' AFTER ma_token_so_kham,
    ADD COLUMN ngay_sua_cuoi DATETIME(6) NULL COMMENT 'lần sửa gần nhất; NULL = chưa sửa lần nào' AFTER phien_ban;

CREATE TABLE lich_su_sua_benh_an
(
    id_lich_su_sua   BIGINT       NOT NULL AUTO_INCREMENT,
    id_ho_so_benh_an BIGINT       NOT NULL,
    phien_ban        INT          NOT NULL COMMENT 'phiên bản hồ sơ sau lần sửa này (2, 3, ...)',
    id_bac_si        BIGINT       NOT NULL COMMENT 'bác sĩ đã sửa',
    ly_do            VARCHAR(500) NOT NULL COMMENT 'lý do sửa, bắt buộc',
    noi_dung_truoc   TEXT         NOT NULL COMMENT 'JSON nội dung hồ sơ trước khi sửa',
    noi_dung_sau     TEXT         NOT NULL COMMENT 'JSON nội dung hồ sơ sau khi sửa',
    ngay_tao         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'thời điểm sửa',
    CONSTRAINT pk_lich_su_sua_benh_an PRIMARY KEY (id_lich_su_sua),
    CONSTRAINT uk_lich_su_sua_benh_an_phien_ban UNIQUE (id_ho_so_benh_an, phien_ban),
    CONSTRAINT fk_lich_su_sua_benh_an_ho_so_benh_an FOREIGN KEY (id_ho_so_benh_an) REFERENCES ho_so_benh_an (id_ho_so_benh_an),
    CONSTRAINT fk_lich_su_sua_benh_an_bac_si FOREIGN KEY (id_bac_si) REFERENCES bac_si (id_bac_si)
);
