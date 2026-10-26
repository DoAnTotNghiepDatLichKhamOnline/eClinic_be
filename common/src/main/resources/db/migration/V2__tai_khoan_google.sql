-- =====================================================================
-- Đăng nhập bằng Google (DOANTOTNGH-2, phase 4)
--  * mat_khau_hash cho phép NULL: tài khoản tạo từ Google chưa có mật khẩu (có thể đặt qua quên mật khẩu).
--  * google_id: "sub" trong ID token Google, định danh bất biến của tài khoản Google (không dùng email).
-- =====================================================================
ALTER TABLE tai_khoan
    MODIFY COLUMN mat_khau_hash VARCHAR(255) NULL COMMENT 'NULL: tài khoản chỉ đăng nhập bằng Google',
    ADD COLUMN google_id VARCHAR(255) NULL COMMENT 'sub trong ID token Google' AFTER mat_khau_hash,
    ADD CONSTRAINT uk_tai_khoan_google_id UNIQUE (google_id);
