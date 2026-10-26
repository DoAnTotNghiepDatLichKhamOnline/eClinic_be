-- =====================================================================
-- Tệp lịch .ics đính kèm email lịch hẹn (DOANTOTNGH-14, phase 7)
--  * su_kien_thong_bao.tep_lich: nội dung tệp .ics (RFC 5545, METHOD:PUBLISH) booking-service soạn lúc xếp email đặt /
--    đổi / hủy lịch... theo ngôn ngữ của email; notification-service chép sang email_gui.tep_lich.
--  * email_gui.tep_lich: có thì email gửi kèm tệp lich-kham.ics. NULL (dòng cũ, email nhắc lịch, tái khám...) = không
--    đính kèm.
-- =====================================================================
ALTER TABLE su_kien_thong_bao
    ADD COLUMN tep_lich TEXT NULL COMMENT 'nội dung tệp .ics đính kèm email; NULL = không đính kèm';

ALTER TABLE email_gui
    ADD COLUMN tep_lich TEXT NULL COMMENT 'nội dung tệp .ics đính kèm (lich-kham.ics); NULL = không đính kèm';
