-- =====================================================================
-- Nhắc lịch khám (DOANTOTNGH-9, phase 5)
--  * booking-service có 1 job tìm lịch hẹn đến hạn nhắc ở từng mốc "trước giờ khám bao lâu" (cấu hình app.nhac-lich,
--    mặc định 3 ngày và 1 ngày). Mỗi (lịch hẹn, mốc) được ghi 1 dòng ở đây TRONG CÙNG transaction với các dòng thông báo
--    / email chờ gửi (su_kien_thong_bao), nên 1 mốc không bao giờ được nhắc 2 lần.
--  * da_gui = 0: mốc được ghi nhận nhưng không nhắc (lịch đặt khi đã ở trong khoảng của mốc đó, hoặc 2 mốc cùng đến hạn
--    thì chỉ nhắc mốc gần giờ khám nhất).
--  * Không ghi vào lich_hen: mốc là cấu hình (không cố định số cột), và job không được làm đổi version của lịch hẹn khi
--    người dùng đang hủy / đổi.
-- =====================================================================
CREATE TABLE nhac_lich_hen
(
    id_nhac       BIGINT      NOT NULL AUTO_INCREMENT,
    id_lich_hen   BIGINT      NOT NULL,
    so_phut_truoc INT         NOT NULL COMMENT 'mốc nhắc: số phút trước giờ khám',
    da_gui        BOOLEAN     NOT NULL COMMENT '0 = ghi nhận mốc nhưng không nhắc',
    ngay_tao      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_nhac_lich_hen PRIMARY KEY (id_nhac),
    CONSTRAINT uk_nhac_lich_hen_moc UNIQUE (id_lich_hen, so_phut_truoc),
    CONSTRAINT fk_nhac_lich_hen_lich_hen FOREIGN KEY (id_lich_hen) REFERENCES lich_hen (id_lich_hen) ON DELETE CASCADE
);
