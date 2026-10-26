-- =====================================================================
-- Liên kết hồ sơ bệnh nhân của Khách vào tài khoản theo CCCD (DOANTOTNGH-5, phase 5; quy tắc #3)
--  * tai_khoan.cccd_dang_ky: số CCCD người dùng nhập khi đăng ký (không bắt buộc). Chỉ là lời khai để booking-service
--    tìm hồ sơ bệnh nhân đã có của Khách và liên kết vào tài khoản; KHÔNG UNIQUE: 2 tài khoản có thể khai cùng 1 số,
--    tài khoản nào khớp thông tin trước thì được liên kết (ho_so_benh_nhan.id_tai_khoan mới là UNIQUE).
--  * lien_ket_ho_so_bi_tu_choi: quản trị viên đã từ chối gắn hồ sơ này vào tài khoản này. Có dòng ở đây thì hệ thống
--    không tự đưa cặp (tài khoản, hồ sơ) đó vào hàng chờ xác minh nữa. Xoá tài khoản thì xoá theo.
-- =====================================================================
ALTER TABLE tai_khoan
    ADD COLUMN cccd_dang_ky VARCHAR(12) NULL COMMENT 'CCCD khai khi đăng ký, dùng để liên kết hồ sơ bệnh nhân đã có' AFTER so_dien_thoai;

CREATE TABLE lien_ket_ho_so_bi_tu_choi
(
    id_lien_ket_bi_tu_choi BIGINT      NOT NULL AUTO_INCREMENT,
    id_tai_khoan           BIGINT      NOT NULL,
    id_ho_so_benh_nhan     BIGINT      NOT NULL,
    ly_do                  VARCHAR(500),
    ngay_tao               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_lien_ket_ho_so_bi_tu_choi PRIMARY KEY (id_lien_ket_bi_tu_choi),
    CONSTRAINT uk_lien_ket_bi_tu_choi_tai_khoan_ho_so UNIQUE (id_tai_khoan, id_ho_so_benh_nhan),
    CONSTRAINT fk_lien_ket_bi_tu_choi_tai_khoan FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan (id_tai_khoan) ON DELETE CASCADE,
    CONSTRAINT fk_lien_ket_bi_tu_choi_ho_so FOREIGN KEY (id_ho_so_benh_nhan) REFERENCES ho_so_benh_nhan (id_ho_so_benh_nhan)
);
