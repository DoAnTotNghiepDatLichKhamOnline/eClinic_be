-- =====================================================================
-- Đặt lịch cho trẻ chưa có CCCD + mã tra cứu ngắn + email liên hệ (DOANTOTNGH-5, phase 3)
--  * lich_hen.ma_tra_cuu: mã ngắn để đọc / gõ, dạng ECL-<ngày khám yyyyMMdd>-<4 chữ số>, in trên phiếu khám; bác sĩ và
--    quản trị viên tra lịch hẹn theo mã này. KHÔNG mở được phiếu khám công khai (đoán được): phiếu khám công khai chỉ
--    tra bằng ma_token_phieu_kham. Lịch hẹn có sẵn: phần cuối là id lịch hẹn (duy nhất sẵn).
--  * lich_hen.email_lien_he: email người đặt nhập trên form (không bắt buộc), để sau này gửi phiếu khám / nhắc lịch.
--  * ho_so_benh_nhan.cccd cho NULL: bệnh nhân dưới 18 tuổi chưa có CCCD (quy tắc #10). Hồ sơ không có CCCD được nhận
--    diện bằng khoa_nhan_dien = SHA-256 của (họ tên đã chuẩn hoá | ngày sinh | CCCD người giám hộ); UNIQUE để 2 lần đặt
--    cùng lúc cho cùng 1 trẻ không tạo 2 hồ sơ. MySQL cho nhiều dòng NULL trong cột UNIQUE.
--  MySQL tự commit từng câu DDL nên phần phụ thuộc dữ liệu (lich_hen) đặt trước.
-- =====================================================================
ALTER TABLE lich_hen
    ADD COLUMN ma_tra_cuu VARCHAR(20) NULL AFTER ma_token_phieu_kham,
    ADD COLUMN email_lien_he VARCHAR(255) NULL COMMENT 'email người đặt nhập trên form, NULL = không nhập' AFTER so_dien_thoai_lien_he;

UPDATE lich_hen l
    JOIN khung_gio_kham k ON k.id_khung_gio = l.id_khung_gio
SET l.ma_tra_cuu = CONCAT('ECL-', DATE_FORMAT(k.gio_bat_dau, '%Y%m%d'), '-', LPAD(l.id_lich_hen, 4, '0'));

ALTER TABLE lich_hen
    MODIFY COLUMN ma_tra_cuu VARCHAR(20) NOT NULL COMMENT 'mã tra cứu ngắn, vd ECL-20261005-4198',
    ADD CONSTRAINT uk_lich_hen_ma_tra_cuu UNIQUE (ma_tra_cuu);

ALTER TABLE ho_so_benh_nhan
    MODIFY COLUMN cccd VARCHAR(12) NULL COMMENT 'NULL = bệnh nhân dưới 18 tuổi chưa có CCCD (quy tắc #10)',
    ADD COLUMN khoa_nhan_dien VARCHAR(64) NULL COMMENT 'SHA-256(họ tên chuẩn hoá | ngày sinh | CCCD người giám hộ) của hồ sơ tạo khi chưa có CCCD' AFTER cccd,
    ADD CONSTRAINT uk_ho_so_benh_nhan_khoa_nhan_dien UNIQUE (khoa_nhan_dien),
    ADD CONSTRAINT ck_ho_so_benh_nhan_dinh_danh CHECK (cccd IS NOT NULL OR khoa_nhan_dien IS NOT NULL);
