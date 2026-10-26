-- =====================================================================
-- Ngày bệnh viện nghỉ (DOANTOTNGH-14, phase 2): Tết, lễ, ngày nghỉ đột xuất do quản trị viên khai báo
--  * Mỗi ngày nghỉ 1 dòng (ngay UNIQUE). Khai báo 1 khoảng ngày thì thêm 1 dòng cho mỗi ngày chưa nghỉ.
--  * Khai báo hủy mọi ca còn hoạt động của ngày đó trong cùng transaction (lịch hẹn giữ nguyên, được đánh dấu cần đổi
--    lịch như khi quản trị viên hủy ca; lich_su_ca_lam_viec.nguon = NGAY_NGHI). Sau đó không xếp / sao chép / nhập /
--    duyệt đổi ca vào ngày nghỉ được. Trang đặt lịch không cần lọc thêm: lượt khám của ca đã hủy không còn đặt được.
--  * Xoá 1 ngày nghỉ (chưa tới) chỉ xoá dòng này; các ca đã hủy giữ nguyên.
--  * ly_do bắt buộc (tiếng Việt), ly_do_en tuỳ chọn: tiếng Anh để trống thì hiển thị câu tiếng Việt.
-- =====================================================================
CREATE TABLE ngay_nghi_benh_vien
(
    id_ngay_nghi         BIGINT       NOT NULL AUTO_INCREMENT,
    ngay                 DATE         NOT NULL COMMENT 'ngày bệnh viện nghỉ, không có ca làm việc',
    ly_do                VARCHAR(200) NOT NULL COMMENT 'lý do (tiếng Việt), hiện cho bệnh nhân và trong thông báo',
    ly_do_en             VARCHAR(200) NULL COMMENT 'lý do tiếng Anh; NULL = dùng câu tiếng Việt',
    id_quan_tri_vien_tao BIGINT       NOT NULL COMMENT 'quản trị viên đã khai báo',
    ngay_tao             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_ngay_nghi_benh_vien PRIMARY KEY (id_ngay_nghi),
    CONSTRAINT uk_ngay_nghi_benh_vien_ngay UNIQUE (ngay),
    CONSTRAINT fk_ngay_nghi_benh_vien_quan_tri FOREIGN KEY (id_quan_tri_vien_tao) REFERENCES quan_tri_vien (id_quan_tri_vien)
);
