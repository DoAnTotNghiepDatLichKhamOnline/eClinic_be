-- =====================================================================
-- Quản lý ca làm việc và yêu cầu đổi ca / xin nghỉ (DOANTOTNGH-7, phase 5)
--  * su_kien_thong_bao.id_yeu_cau: thông báo về 1 yêu cầu đổi lịch (admin: có yêu cầu mới; bác sĩ: kết quả duyệt) mang
--    theo id yêu cầu, notification-service ghi vào thong_bao.id_yeu_cau. Không có khoá ngoại, như các cột khác của bảng.
--  * yeu_cau_doi_lich.trang_thai có thêm giá trị DA_RUT: bác sĩ tự rút yêu cầu khi còn CHO_DUYET. Cột là VARCHAR nên
--    không đổi kiểu, chỉ sửa chú thích. Cột generated id_lich_lam_viec_cho_duyet chỉ xét CHO_DUYET nên rút xong là ca
--    nhận được yêu cầu mới.
-- =====================================================================
ALTER TABLE su_kien_thong_bao
    ADD COLUMN id_yeu_cau BIGINT NULL COMMENT 'yêu cầu đổi lịch liên quan' AFTER id_lich_hen;

ALTER TABLE yeu_cau_doi_lich
    MODIFY COLUMN trang_thai VARCHAR(20) NOT NULL DEFAULT 'CHO_DUYET' COMMENT 'CHO_DUYET | DA_DUYET | TU_CHOI | DA_RUT';
