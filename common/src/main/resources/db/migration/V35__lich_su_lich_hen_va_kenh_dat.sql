-- =====================================================================
-- Quản trị lịch hẹn toàn viện (DOANTOTNGH-14, phase 3, ADM-04)
--  * lich_hen.kenh_dat: lịch hẹn được đặt qua kênh nào. Dòng cũ: có tài khoản đặt -> TAI_KHOAN, còn lại KHACH.
--    Đổi lịch giữ kênh của lịch cũ. Báo cáo thống kê nhóm theo cột này.
--  * lich_su_lich_hen: nhật ký CHỈ THÊM các thao tác trên lịch hẹn (đặt, đổi, hủy, ca bị hủy, ca đổi bác sĩ, ca bị sửa),
--    ai làm, lúc nào, lý do; ghi cùng transaction với thao tác. Đổi lịch ghi 1 dòng ở lịch cũ, trỏ tới lịch mới. Không
--    ghi "đã khám" / "không đến" (trạng thái lịch hẹn đã cho biết). Không có dữ liệu cho các thao tác trước phase này.
--  * Trạng thái mới QUAN_TRI_HUY (quản trị viên hủy, kèm lý do): cột trang_thai là VARCHAR, không cần đổi.
-- =====================================================================
ALTER TABLE lich_hen
    ADD COLUMN kenh_dat VARCHAR(20) NOT NULL DEFAULT 'KHACH' COMMENT 'KHACH | TAI_KHOAN (xem enum KenhDatLich)';

UPDATE lich_hen
SET kenh_dat = 'TAI_KHOAN'
WHERE id_tai_khoan_dat IS NOT NULL;

CREATE TABLE lich_su_lich_hen
(
    id_lich_su_lich_hen    BIGINT      NOT NULL AUTO_INCREMENT,
    id_lich_hen            BIGINT      NOT NULL COMMENT 'lịch hẹn của thao tác',
    su_kien                VARCHAR(20) NOT NULL COMMENT 'DAT_LICH | DOI_LICH | HUY | CA_BI_HUY | DOI_BAC_SI | CA_THAY_DOI',
    loai_nguoi_thuc_hien   VARCHAR(20) NOT NULL COMMENT 'KHACH | TAI_KHOAN | BAC_SI | QUAN_TRI_VIEN',
    id_tai_khoan_thuc_hien BIGINT      NULL COMMENT 'tài khoản đã làm; NULL khi khách (cầm link phiếu khám)',
    id_lich_hen_lien_quan  BIGINT      NULL COMMENT 'DOI_LICH: lịch hẹn mới tạo ra',
    ly_do                  TEXT        NULL COMMENT 'câu người dùng gõ, hoặc {heThong.<khoa>} (VanBanHeThong)',
    ngay_tao               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'thời điểm thao tác',
    CONSTRAINT pk_lich_su_lich_hen PRIMARY KEY (id_lich_su_lich_hen),
    CONSTRAINT fk_lich_su_lich_hen_lich_hen FOREIGN KEY (id_lich_hen) REFERENCES lich_hen (id_lich_hen),
    CONSTRAINT fk_lich_su_lich_hen_tai_khoan FOREIGN KEY (id_tai_khoan_thuc_hien) REFERENCES tai_khoan (id_tai_khoan),
    CONSTRAINT fk_lich_su_lich_hen_lien_quan FOREIGN KEY (id_lich_hen_lien_quan) REFERENCES lich_hen (id_lich_hen)
);
CREATE INDEX idx_lich_su_lich_hen_lich_hen ON lich_su_lich_hen (id_lich_hen, ngay_tao);
