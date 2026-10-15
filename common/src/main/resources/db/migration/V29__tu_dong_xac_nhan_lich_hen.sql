-- =====================================================================
-- Lịch hẹn được xác nhận ngay khi đặt (DOANTOTNGH-11, phase 1)
--  * Bỏ trạng thái CHO_XAC_NHAN: đặt lịch / đổi lịch tạo thẳng DA_XAC_NHAN, bác sĩ không còn duyệt từng lịch hẹn.
--    Lịch đang chờ xác nhận chuyển sang đã xác nhận.
--  * BI_TU_CHOI giữ nguyên giá trị, nay nghĩa là "bác sĩ hủy lịch đã xác nhận, kèm lý do" (trước giờ khám).
--  * Cột sinh lich_hen.id_khung_gio_hieu_luc (V1) vẫn liệt kê 'CHO_XAC_NHAN': vô hại vì không dòng nào còn giá trị đó,
--    nên không dựng lại cột (UNIQUE) chỉ để bỏ 1 giá trị.
-- =====================================================================
UPDATE lich_hen
SET trang_thai = 'DA_XAC_NHAN'
WHERE trang_thai = 'CHO_XAC_NHAN';

ALTER TABLE lich_hen
    ALTER COLUMN trang_thai SET DEFAULT 'DA_XAC_NHAN';
