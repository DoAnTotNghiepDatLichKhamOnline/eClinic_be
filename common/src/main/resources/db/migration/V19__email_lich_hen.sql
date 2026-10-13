-- =====================================================================
-- Email xác nhận đặt lịch (DOANTOTNGH-9, phase 4)
--  * Email đi 2 chặng, mỗi chặng tự nhận ra dòng đã có:
--      1) booking-service -> notification-service: dùng lại bảng chờ gửi su_kien_thong_bao, thêm kênh EMAIL. Dòng email
--         ghi địa chỉ nhận + ngôn ngữ thay cho tài khoản nhận (khách đặt lịch không có tài khoản).
--      2) notification-service -> máy chủ mail: bảng email_gui là hàng chờ gửi của notification-service; gửi lỗi thì thử
--         lại sau khoảng chờ tăng dần, hết số lần thì THAT_BAI; đã DA_GUI thì không gửi lại.
--  * lich_hen.ngon_ngu: ngôn ngữ của request đã tạo lịch hẹn. Dùng cho email gửi tới khách (không có tài khoản để lấy
--    ngôn ngữ đã lưu); lịch hẹn do tài khoản đặt thì email theo tai_khoan.ngon_ngu. NULL = lịch hẹn tạo trước V19.
--  * email_gui không có khoá ngoại tới lich_hen: hàng chờ gửi không được chặn việc xoá dữ liệu của miền khác.
-- =====================================================================
ALTER TABLE lich_hen
    ADD COLUMN ngon_ngu VARCHAR(5) NULL COMMENT 'VI / EN: ngôn ngữ của request tạo lịch hẹn; NULL = tạo trước V19' AFTER email_lien_he;

ALTER TABLE su_kien_thong_bao
    MODIFY COLUMN loai VARCHAR(30) NULL COMMENT 'xem enum LoaiThongBao; NULL với dòng kênh EMAIL',
    MODIFY COLUMN id_tai_khoan BIGINT NULL COMMENT 'tài khoản nhận thông báo; NULL với dòng kênh EMAIL',
    ADD COLUMN kenh VARCHAR(10) NOT NULL DEFAULT 'CHUONG' COMMENT 'CHUONG: thông báo trong ứng dụng; EMAIL' AFTER id_su_kien,
    ADD COLUMN email_nhan VARCHAR(255) NULL COMMENT 'địa chỉ nhận, chỉ có ở dòng kênh EMAIL' AFTER id_tai_khoan,
    ADD COLUMN ngon_ngu VARCHAR(5) NULL COMMENT 'VI / EN: ngôn ngữ của email, chỉ có ở dòng kênh EMAIL' AFTER email_nhan;

CREATE TABLE email_gui
(
    id_email         BIGINT       NOT NULL AUTO_INCREMENT,
    ma_nguon         VARCHAR(64)  NOT NULL COMMENT 'mã sự kiện ở service nguồn, vd booking:15',
    nguoi_nhan       VARCHAR(255) NOT NULL,
    ngon_ngu         VARCHAR(5)   NOT NULL COMMENT 'VI / EN',
    khoa_mau         VARCHAR(80)  NOT NULL COMMENT 'mẫu email, vd email.datLich',
    tham_so          TEXT         NULL COMMENT 'mảng JSON các tham số của mẫu',
    id_lich_hen      BIGINT       NULL COMMENT 'lịch hẹn liên quan (không có khoá ngoại)',
    trang_thai       VARCHAR(10)  NOT NULL DEFAULT 'CHO_GUI' COMMENT 'CHO_GUI / DA_GUI / THAT_BAI',
    so_lan_thu       INT          NOT NULL DEFAULT 0 COMMENT 'số lần gửi đã lỗi',
    lan_thu_ke_tiep  DATETIME(6)  NOT NULL COMMENT 'không gửi trước thời điểm này',
    loi_cuoi         VARCHAR(100) NULL COMMENT 'tên loại lỗi của lần gửi gần nhất (không chứa địa chỉ, nội dung)',
    ngay_tao         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ngay_gui         DATETIME(6)  NULL,
    CONSTRAINT pk_email_gui PRIMARY KEY (id_email),
    CONSTRAINT uk_email_gui_ma_nguon UNIQUE (ma_nguon)
);
CREATE INDEX idx_email_gui_cho_gui ON email_gui (trang_thai, lan_thu_ke_tiep);
