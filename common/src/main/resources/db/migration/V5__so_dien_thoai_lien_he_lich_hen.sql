-- =====================================================================
-- Số điện thoại liên hệ của từng lịch hẹn (DOANTOTNGH-4, phase 3)
--  * lich_hen.so_dien_thoai_lien_he: SĐT người đặt nhập trên form đặt lịch, dùng để liên hệ cho lượt khám đó.
--    Không ghi đè ho_so_benh_nhan.so_dien_thoai: Khách chỉ cần biết CCCD + họ tên + ngày sinh là đặt được lịch vào
--    hồ sơ có sẵn, còn SĐT của hồ sơ là thứ được đối chiếu khi liên kết hồ sơ vào tài khoản (quy tắc #3).
--    NULL với lịch hẹn tạo trước V5.
--  * Index (so_dien_thoai_lien_he, trang_thai): đếm số lịch còn hiệu lực theo SĐT để giới hạn đặt lịch.
-- =====================================================================
ALTER TABLE lich_hen
    ADD COLUMN so_dien_thoai_lien_he VARCHAR(20) NULL AFTER ghi_chu;

CREATE INDEX idx_lich_hen_sdt_lien_he_trang_thai ON lich_hen (so_dien_thoai_lien_he, trang_thai);
