-- =====================================================================
-- Quản lý bác sĩ (DOANTOTNGH-7, phase 6)
--  * tai_khoan.phai_doi_mat_khau: tài khoản bác sĩ do quản trị viên tạo mang mật khẩu mặc định (cấu hình
--    DOCTOR_DEFAULT_PASSWORD). Khi cột này = TRUE, đăng nhập đúng mật khẩu KHÔNG được cấp phiên (403
--    PHAI_DOI_MAT_KHAU); chủ tài khoản phải đặt mật khẩu của mình qua POST /api/auth/first-password trước.
--    Đặt lại mật khẩu bằng liên kết email cũng gỡ cờ này.
-- =====================================================================
ALTER TABLE tai_khoan
    ADD COLUMN phai_doi_mat_khau BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'TRUE: đang dùng mật khẩu mặc định, phải đặt mật khẩu mới trước khi được cấp phiên' AFTER mat_khau_hash;
