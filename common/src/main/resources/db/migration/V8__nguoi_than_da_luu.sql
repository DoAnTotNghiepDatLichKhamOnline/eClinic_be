-- =====================================================================
-- Người thân đã lưu của tài khoản, để điền sẵn form đặt lịch lần sau (DOANTOTNGH-5, phase 4)
--  * Mỗi dòng = 1 người (hồ sơ bệnh nhân) mà tài khoản đã đặt lịch cho, lưu ĐÚNG những gì tài khoản đó nhập trên form
--    lần đặt gần nhất (người khám + người giám hộ nếu có). Không đọc lại từ ho_so_benh_nhan: hồ sơ gom dữ liệu của
--    nhiều người đặt khác nhau (địa chỉ, số BHYT, CCCD điền sau) nên trả hồ sơ cho tài khoản sẽ lộ dữ liệu người khác nhập.
--  * Xoá tài khoản thì xoá theo (ON DELETE CASCADE, như refresh_token): đây là ghi chú riêng của tài khoản.
--  * Tối đa 10 người / tài khoản (giới hạn ở service: người dùng lâu nhất bị bỏ).
-- =====================================================================
CREATE TABLE nguoi_than_da_luu
(
    id_nguoi_than_da_luu  BIGINT       NOT NULL AUTO_INCREMENT,
    id_tai_khoan          BIGINT       NOT NULL COMMENT 'tài khoản đã đặt lịch cho người này',
    id_ho_so_benh_nhan    BIGINT       NOT NULL COMMENT 'hồ sơ bệnh nhân của người này (khoá để cập nhật, không đọc dữ liệu từ đây)',
    ho_ten                VARCHAR(150) NOT NULL,
    ngay_sinh             DATE         NOT NULL,
    gioi_tinh             VARCHAR(10) COMMENT 'NAM | NU | KHAC',
    cccd                  VARCHAR(12) COMMENT 'NULL = người dưới 18 tuổi chưa có CCCD',
    so_dien_thoai         VARCHAR(20)  NOT NULL,
    email                 VARCHAR(255),
    dia_chi               VARCHAR(500),
    so_bao_hiem_y_te      VARCHAR(50),
    giam_ho_ho_ten        VARCHAR(150) COMMENT 'các cột giam_ho_*: NULL khi lần đặt gần nhất không có người giám hộ',
    giam_ho_quan_he       VARCHAR(30) COMMENT 'CHA | ME | NGUOI_GIAM_HO_HOP_PHAP | KHAC',
    giam_ho_so_dien_thoai VARCHAR(20),
    giam_ho_cccd          VARCHAR(12),
    giam_ho_ngay_sinh     DATE,
    lan_dung_cuoi         DATETIME(6)  NOT NULL COMMENT 'lần đặt lịch gần nhất cho người này',
    ngay_tao              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_nguoi_than_da_luu PRIMARY KEY (id_nguoi_than_da_luu),
    CONSTRAINT uk_nguoi_than_da_luu_tai_khoan_ho_so UNIQUE (id_tai_khoan, id_ho_so_benh_nhan),
    CONSTRAINT fk_nguoi_than_da_luu_tai_khoan FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan (id_tai_khoan) ON DELETE CASCADE,
    CONSTRAINT fk_nguoi_than_da_luu_ho_so FOREIGN KEY (id_ho_so_benh_nhan) REFERENCES ho_so_benh_nhan (id_ho_so_benh_nhan)
);
CREATE INDEX idx_nguoi_than_da_luu_tai_khoan_lan_dung ON nguoi_than_da_luu (id_tai_khoan, lan_dung_cuoi);
