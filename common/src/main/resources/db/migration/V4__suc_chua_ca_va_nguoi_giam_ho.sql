-- =====================================================================
-- Sức chứa ca làm việc + người giám hộ + người đặt lịch (DOANTOTNGH-4, phase 1)
--  * lich_lam_viec: N = so_luot_toi_da_moi_gio, t = thoi_luong_luot_phut, N x t <= 60 (BOOK-12, quy tắc #11).
--    Khung 1 giờ không lưu thành dòng: mỗi dòng khung_gio_kham là 1 lượt khám dài t phút, khung 1 giờ thứ k của ca
--    gồm các lượt có giờ bắt đầu trong [giờ bắt đầu ca + k x 60, + 60). UNIQUE uk_lich_hen_khung_gio_hieu_luc (V1)
--    vẫn là chốt chặn đặt vượt sức chứa ở mức DB.
--  * Ca có sẵn: t = độ dài khung giờ ngắn nhất của ca (ca chưa có khung giờ: số phút của ca / so_benh_nhan_toi_da),
--    ép vào 1..60; N = floor(60 / t). Dữ liệu mẫu cũ (khung 30 phút) thành N = 2, t = 30, đúng với các dòng đang có.
--  * nguoi_giam_ho: người giám hộ khai khi đặt lịch cho bệnh nhân dưới 18 tuổi (BOOK-11, quy tắc #9).
--  * lich_hen: id_nguoi_giam_ho = người giám hộ của lượt khám đó; id_tai_khoan_dat = tài khoản đã đặt (NULL = Khách).
--  MySQL tự commit từng câu DDL nên phần phụ thuộc dữ liệu (lich_lam_viec) đặt trước.
-- =====================================================================
ALTER TABLE lich_lam_viec
    ADD COLUMN so_luot_toi_da_moi_gio INT NULL AFTER so_benh_nhan_toi_da,
    ADD COLUMN thoi_luong_luot_phut INT NULL AFTER so_luot_toi_da_moi_gio;

UPDATE lich_lam_viec l
    LEFT JOIN (SELECT id_lich_lam_viec, MIN(TIMESTAMPDIFF(MINUTE, gio_bat_dau, gio_ket_thuc)) AS so_phut
               FROM khung_gio_kham
               GROUP BY id_lich_lam_viec) k ON k.id_lich_lam_viec = l.id_lich_lam_viec
SET l.thoi_luong_luot_phut = LEAST(60, GREATEST(1, COALESCE(k.so_phut,
        FLOOR((TIME_TO_SEC(l.gio_ket_thuc) - TIME_TO_SEC(l.gio_bat_dau)) / 60 / l.so_benh_nhan_toi_da))));

UPDATE lich_lam_viec
SET so_luot_toi_da_moi_gio = FLOOR(60 / thoi_luong_luot_phut);

ALTER TABLE lich_lam_viec
    MODIFY COLUMN so_benh_nhan_toi_da INT NOT NULL COMMENT 'tổng số lượt khám của cả ca = số dòng khung_gio_kham của ca',
    MODIFY COLUMN so_luot_toi_da_moi_gio INT NOT NULL COMMENT 'N: số lượt khám tối đa trong 1 khung 1 giờ (BOOK-12)',
    MODIFY COLUMN thoi_luong_luot_phut INT NOT NULL COMMENT 't: số phút của 1 lượt khám, N x t <= 60',
    ADD CONSTRAINT ck_lich_lam_viec_so_luot CHECK (so_luot_toi_da_moi_gio > 0),
    ADD CONSTRAINT ck_lich_lam_viec_thoi_luong CHECK (thoi_luong_luot_phut > 0),
    ADD CONSTRAINT ck_lich_lam_viec_suc_chua_gio CHECK (so_luot_toi_da_moi_gio * thoi_luong_luot_phut <= 60);

CREATE TABLE nguoi_giam_ho
(
    id_nguoi_giam_ho   BIGINT       NOT NULL AUTO_INCREMENT,
    id_ho_so_benh_nhan BIGINT       NOT NULL COMMENT 'bệnh nhân được giám hộ',
    ho_ten             VARCHAR(150) NOT NULL,
    quan_he            VARCHAR(30)  NOT NULL COMMENT 'CHA | ME | NGUOI_GIAM_HO_HOP_PHAP | KHAC',
    so_dien_thoai      VARCHAR(20)  NOT NULL,
    cccd               VARCHAR(12)  NOT NULL,
    ngay_sinh          DATE,
    ngay_tao           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_nguoi_giam_ho PRIMARY KEY (id_nguoi_giam_ho),
    -- 1 người giám hộ chỉ khai 1 lần cho 1 hồ sơ, các lần đặt sau dùng lại
    CONSTRAINT uk_nguoi_giam_ho_ho_so_cccd UNIQUE (id_ho_so_benh_nhan, cccd),
    CONSTRAINT fk_nguoi_giam_ho_ho_so_benh_nhan FOREIGN KEY (id_ho_so_benh_nhan) REFERENCES ho_so_benh_nhan (id_ho_so_benh_nhan)
);

ALTER TABLE lich_hen
    ADD COLUMN id_nguoi_giam_ho BIGINT NULL COMMENT 'người giám hộ của lượt khám (bệnh nhân dưới 18 tuổi), NULL = không cần' AFTER id_ho_so_benh_nhan,
    ADD COLUMN id_tai_khoan_dat BIGINT NULL COMMENT 'tài khoản đã đặt lịch, NULL = Khách đặt' AFTER id_nguoi_giam_ho,
    ADD CONSTRAINT fk_lich_hen_nguoi_giam_ho FOREIGN KEY (id_nguoi_giam_ho) REFERENCES nguoi_giam_ho (id_nguoi_giam_ho),
    ADD CONSTRAINT fk_lich_hen_tai_khoan_dat FOREIGN KEY (id_tai_khoan_dat) REFERENCES tai_khoan (id_tai_khoan);
