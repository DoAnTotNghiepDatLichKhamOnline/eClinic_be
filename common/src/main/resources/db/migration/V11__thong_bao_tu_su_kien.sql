-- =====================================================================
-- Thông báo trong ứng dụng từ sự kiện của lịch hẹn (DOANTOTNGH-7, phase 4)
--  * su_kien_thong_bao (bảng của booking-service, "outbox"): booking-service ghi 1 dòng cho mỗi người nhận TRONG CÙNG
--    transaction với việc đổi trạng thái lịch hẹn; job GuiThongBaoJob gửi các dòng chưa gửi sang notification-service và
--    chỉ đặt ngay_gui khi notification-service đã nhận. notification-service ngừng chạy thì dòng nằm chờ, không mất.
--    Không có khoá ngoại tới tai_khoan / lich_hen: bảng chờ gửi không được chặn việc xoá dữ liệu của miền khác.
--  * so_lan_loi: số lần notification-service từ chối RIÊNG dòng này (dữ liệu sai). Đủ 3 lần thì job bỏ qua dòng đó để
--    1 dòng hỏng không chặn các dòng sau; mất kết nối / lỗi 5xx không tăng số này.
--  * thong_bao.ma_nguon: mã của sự kiện đã sinh ra thông báo ("booking:<id_su_kien>"), UNIQUE: cùng 1 sự kiện gửi lại
--    nhiều lần vẫn chỉ tạo 1 thông báo. NULL với thông báo không sinh từ sự kiện.
-- =====================================================================
CREATE TABLE su_kien_thong_bao
(
    id_su_kien   BIGINT      NOT NULL AUTO_INCREMENT,
    loai         VARCHAR(30) NOT NULL COMMENT 'xem enum LoaiThongBao',
    id_tai_khoan BIGINT      NOT NULL COMMENT 'tài khoản nhận thông báo',
    id_lich_hen  BIGINT      NULL COMMENT 'lịch hẹn liên quan',
    noi_dung     TEXT        NOT NULL,
    so_lan_loi   INT         NOT NULL DEFAULT 0 COMMENT 'số lần notification-service từ chối dòng này',
    ngay_tao     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ngay_gui     DATETIME(6) NULL COMMENT 'NULL = chưa gửi được',
    CONSTRAINT pk_su_kien_thong_bao PRIMARY KEY (id_su_kien)
);
CREATE INDEX idx_su_kien_thong_bao_cho_gui ON su_kien_thong_bao (ngay_gui, id_su_kien);

ALTER TABLE thong_bao
    ADD COLUMN ma_nguon VARCHAR(64) NULL COMMENT 'Mã sự kiện đã sinh ra thông báo, vd booking:15' AFTER id_yeu_cau,
    ADD CONSTRAINT uk_thong_bao_ma_nguon UNIQUE (ma_nguon);
