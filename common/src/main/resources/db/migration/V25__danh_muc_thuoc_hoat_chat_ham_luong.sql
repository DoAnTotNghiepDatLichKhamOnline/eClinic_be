-- =====================================================================
-- Kê đơn chỉ từ danh mục thuốc (DOANTOTNGH-10, phase 1)
--  * Bác sĩ không còn tự thêm thuốc khi kê đơn: dòng đơn thuốc chọn thuốc theo id trong danh mục do quản trị viên quản lý.
--  * Thêm hoạt chất, hàm lượng, dạng bào chế, đường dùng (Thông tư 26/2025/TT-BYT: ghi tên chung quốc tế + hàm lượng).
--    Các cột đều cho NULL: thuốc bác sĩ đã thêm trước đây chưa có; quản trị viên bổ sung trước khi xác minh.
--  * Thuốc phân biệt theo tên + hàm lượng ("Paracetamol 500mg" và "Paracetamol 250mg" là 2 thuốc):
--    khoá UNIQUE chuyển từ ten_chuan_hoa sang (ten_chuan_hoa, ham_luong_chuan_hoa).
-- =====================================================================
ALTER TABLE thuoc
    ADD COLUMN hoat_chat           VARCHAR(255) NULL COMMENT 'hoạt chất / tên chung quốc tế; thuốc phối hợp nối bằng " + "' AFTER ten_chuan_hoa,
    ADD COLUMN ham_luong           VARCHAR(100) NULL COMMENT 'vd 500mg, 250mg/5ml' AFTER hoat_chat,
    ADD COLUMN ham_luong_chuan_hoa VARCHAR(100) NOT NULL DEFAULT '' COMMENT 'lowercase + trim + gộp khoảng trắng; rỗng khi chưa có hàm lượng' AFTER ham_luong,
    ADD COLUMN dang_bao_che        VARCHAR(50)  NULL COMMENT 'viên nén, siro...' AFTER ham_luong_chuan_hoa,
    ADD COLUMN dang_bao_che_en     VARCHAR(50)  NULL COMMENT 'tablet, syrup...' AFTER dang_bao_che,
    ADD COLUMN duong_dung          VARCHAR(20)  NULL COMMENT 'UONG | NGAM_DUOI_LUOI | TIEM | BOI_NGOAI_DA | NHO_MAT | NHO_TAI | NHO_MUI | XIT_HIT | DAT | KHAC' AFTER dang_bao_che_en;

ALTER TABLE thuoc
    ADD CONSTRAINT uk_thuoc_ten_ham_luong UNIQUE (ten_chuan_hoa, ham_luong_chuan_hoa),
    DROP INDEX uk_thuoc_ten_chuan_hoa;
