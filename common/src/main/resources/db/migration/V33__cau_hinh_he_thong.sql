-- =====================================================================
-- Quy tắc đặt lịch / xếp ca do quản trị viên sửa (DOANTOTNGH-14, phase 1; SCHED-05, quy tắc #13)
--  * cau_hinh_he_thong có ĐÚNG 1 dòng (id_cau_hinh = 1), mỗi quy tắc 1 cột. Trước đây là biến môi trường của
--    booking-service (app.dat-lich, app.lich-lam-viec, app.danh-gia); giờ biến môi trường chỉ dùng để ghi dòng đầu tiên.
--  * Migration KHÔNG ghi sẵn dòng: Flyway chạy ở service nào khởi động trước, không đọc được biến môi trường của
--    booking-service. booking-service thêm dòng khi khởi động (hoặc ở lần đọc đầu) từ cấu hình đang có, sau đó DB là
--    nguồn duy nhất; biến môi trường khác giá trị đang lưu thì chỉ ghi cảnh báo vào log.
--  * Giới hạn theo IP, nhắc lịch, đường dẫn frontend và RECORD_EDIT_DAYS vẫn là biến môi trường.
--  * lich_su_cau_hinh: mỗi lần lưu, mỗi trường đổi giá trị thêm 1 dòng (giá trị cũ, mới, ai, lúc nào). Chỉ thêm.
-- =====================================================================
CREATE TABLE cau_hinh_he_thong
(
    id_cau_hinh                       INT         NOT NULL,
    dat_truoc_toi_thieu_phut          INT         NOT NULL COMMENT 'lượt khám chỉ đặt được khi còn cách giờ bắt đầu ít nhất chừng này phút',
    so_ngay_dat_truoc_toi_da          INT         NOT NULL COMMENT 'chỉ đặt được lịch trong số ngày tới này',
    so_lich_hieu_luc_toi_da_moi_ho_so INT         NOT NULL COMMENT 'lịch hẹn sắp tới còn hiệu lực tối đa của 1 hồ sơ bệnh nhân',
    so_lich_hieu_luc_toi_da_moi_sdt   INT         NOT NULL COMMENT 'lịch hẹn sắp tới còn hiệu lực tối đa của 1 SĐT liên hệ',
    huy_doi_truoc_toi_thieu_phut      INT         NOT NULL COMMENT 'bệnh nhân hủy / đổi lịch khi còn cách giờ khám ít nhất chừng này phút',
    so_lan_doi_lich_toi_da            INT         NOT NULL COMMENT 'số lần đổi lịch tối đa của 1 lần đặt (0 = không cho đổi)',
    gui_yeu_cau_truoc_toi_thieu_gio   INT         NOT NULL COMMENT 'bác sĩ gửi yêu cầu đổi ca / xin nghỉ khi ca còn cách ít nhất chừng này giờ',
    so_ngay_xep_truoc_toi_da          INT         NOT NULL COMMENT 'quản trị viên chỉ xếp được ca trong số ngày tới này',
    so_gio_toi_da_moi_tuan            INT         NOT NULL COMMENT 'tổng giờ ca của 1 bác sĩ trong 1 tuần thứ Hai - Chủ nhật (SCHED-05)',
    so_ngay_duoc_sua_danh_gia         INT         NOT NULL COMMENT 'bệnh nhân sửa được đánh giá trong số ngày này kể từ lúc gửi',
    phien_ban                         INT         NOT NULL DEFAULT 1 COMMENT 'tăng 1 mỗi lần lưu có thay đổi; lần lưu gửi phiên bản cũ bị từ chối',
    ngay_sua                          DATETIME(6) NULL COMMENT 'lần lưu gần nhất; NULL = chưa ai sửa (giá trị lấy từ biến môi trường)',
    id_quan_tri_vien_sua              BIGINT      NULL COMMENT 'quản trị viên lưu gần nhất',
    CONSTRAINT pk_cau_hinh_he_thong PRIMARY KEY (id_cau_hinh),
    CONSTRAINT ck_cau_hinh_he_thong_mot_dong CHECK (id_cau_hinh = 1),
    CONSTRAINT fk_cau_hinh_he_thong_quan_tri FOREIGN KEY (id_quan_tri_vien_sua) REFERENCES quan_tri_vien (id_quan_tri_vien)
);

CREATE TABLE lich_su_cau_hinh
(
    id_lich_su_cau_hinh BIGINT      NOT NULL AUTO_INCREMENT,
    ten_truong          VARCHAR(60) NOT NULL COMMENT 'tên trường trong API, vd soGioToiDaMoiTuan',
    gia_tri_cu          VARCHAR(30) NOT NULL,
    gia_tri_moi         VARCHAR(30) NOT NULL,
    id_quan_tri_vien    BIGINT      NOT NULL COMMENT 'quản trị viên đã lưu',
    ngay_tao            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'thời điểm lưu',
    CONSTRAINT pk_lich_su_cau_hinh PRIMARY KEY (id_lich_su_cau_hinh),
    CONSTRAINT fk_lich_su_cau_hinh_quan_tri FOREIGN KEY (id_quan_tri_vien) REFERENCES quan_tri_vien (id_quan_tri_vien)
);
CREATE INDEX idx_lich_su_cau_hinh_ngay_tao ON lich_su_cau_hinh (ngay_tao);
