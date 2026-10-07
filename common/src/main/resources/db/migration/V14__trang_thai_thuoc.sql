-- =====================================================================
-- Danh mục thuốc do quản trị viên quản lý (DOANTOTNGH-7, phase 7)
--  * thuoc.trang_thai: thuốc NGUNG_DUNG không còn được gợi ý và không kê mới được; đơn thuốc cũ vẫn hiển thị thuốc đó.
--    Thuốc không bị xoá vì chi_tiet_don_thuoc tham chiếu tới.
-- =====================================================================
ALTER TABLE thuoc
    ADD COLUMN trang_thai VARCHAR(20) NOT NULL DEFAULT 'DANG_DUNG' COMMENT 'DANG_DUNG | NGUNG_DUNG' AFTER da_xac_minh;
