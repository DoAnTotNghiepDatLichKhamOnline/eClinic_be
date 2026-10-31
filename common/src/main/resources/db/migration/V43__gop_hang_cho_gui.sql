-- =====================================================================
-- Gộp 2 bảng chờ gửi vào hang_cho_gui (DOANTOTNGH-19, phase 4)
--  * su_kien_thong_bao (V11, outbox của booking-service -> notification-service) và email_gui (V19, hàng chờ gửi email
--    của notification-service -> máy chủ mail) thành 1 bảng, cột hang cho biết dòng thuộc hàng nào (JPA SINGLE_TABLE,
--    2 entity cũ là lớp con). Mỗi service vẫn chỉ đọc / ghi / dọn hàng của mình.
--  * Cột dùng chung khi cùng nghĩa: email_nhan (địa chỉ nhận), so_lan_loi (số lần chuyển đi bị lỗi), ngon_ngu, khoa_mau,
--    tham_so, id_lich_hen, tep_lich, ngay_gui. NOT NULL riêng của từng hàng thành CHECK theo hang.
--  * id của dòng SU_KIEN là mã chống trùng ở bên nhận (thong_bao.ma_nguon / email_gui.ma_nguon = 'booking:<id>'): dòng
--    SU_KIEN chép giữ nguyên id, rồi bộ đếm được nâng qua mọi 'booking:<n>' đã ghi (dòng đã gửi bị xoá sau 7 ngày nên
--    MAX(id) chưa đủ), để 1 id không bao giờ được dùng lại và thông báo mới bị bên nhận bỏ qua. Dòng EMAIL_GUI chép sau,
--    nhận id mới (id này chỉ có trong log).
-- =====================================================================
CREATE TABLE hang_cho_gui
(
    id_hang_cho     BIGINT       NOT NULL AUTO_INCREMENT,
    hang            VARCHAR(20)  NOT NULL COMMENT 'SU_KIEN: booking-service -> notification-service; EMAIL_GUI: notification-service -> máy chủ mail',
    -- SU_KIEN
    kenh            VARCHAR(10)  NULL COMMENT 'SU_KIEN: CHUONG (thông báo trong ứng dụng) | EMAIL',
    loai            VARCHAR(30)  NULL COMMENT 'SU_KIEN: xem enum LoaiThongBao; NULL với dòng kênh EMAIL',
    id_tai_khoan    BIGINT       NULL COMMENT 'SU_KIEN: tài khoản nhận thông báo; NULL với dòng kênh EMAIL',
    id_yeu_cau      BIGINT       NULL COMMENT 'SU_KIEN: yêu cầu đổi lịch liên quan',
    noi_dung        TEXT         NULL COMMENT 'SU_KIEN: câu soạn sẵn của sự kiện cũ; sự kiện mới dùng khoa_mau + tham_so',
    -- EMAIL_GUI
    ma_nguon        VARCHAR(64)  NULL COMMENT 'EMAIL_GUI: mã sự kiện ở service nguồn, vd booking:15',
    trang_thai      VARCHAR(10)  NULL COMMENT 'EMAIL_GUI: CHO_GUI / DA_GUI / THAT_BAI',
    lan_thu_ke_tiep DATETIME(6)  NULL COMMENT 'EMAIL_GUI: không gửi trước thời điểm này',
    loi_cuoi        VARCHAR(100) NULL COMMENT 'EMAIL_GUI: tên loại lỗi của lần gửi gần nhất (không chứa địa chỉ, nội dung)',
    -- dùng chung
    email_nhan      VARCHAR(255) NULL COMMENT 'địa chỉ nhận: SU_KIEN kênh EMAIL, EMAIL_GUI',
    ngon_ngu        VARCHAR(5)   NULL COMMENT 'VI / EN: ngôn ngữ của email (SU_KIEN kênh EMAIL, EMAIL_GUI)',
    id_lich_hen     BIGINT       NULL COMMENT 'lịch hẹn liên quan (không có khoá ngoại)',
    khoa_mau        VARCHAR(80)  NULL COMMENT 'khoá mẫu câu / mẫu email trong thong-diep/',
    tham_so         TEXT         NULL COMMENT 'mảng JSON các tham số của mẫu',
    tep_lich        TEXT         NULL COMMENT 'nội dung tệp .ics đính kèm email (lich-kham.ics); NULL = không đính kèm',
    so_lan_loi      INT          NOT NULL DEFAULT 0 COMMENT 'SU_KIEN: số lần notification-service từ chối dòng này; EMAIL_GUI: số lần gửi đã lỗi',
    ngay_tao        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ngay_gui        DATETIME(6)  NULL COMMENT 'SU_KIEN: notification-service đã nhận; EMAIL_GUI: máy chủ mail đã nhận; NULL = chưa',
    CONSTRAINT pk_hang_cho_gui PRIMARY KEY (id_hang_cho),
    CONSTRAINT uk_hang_cho_gui_ma_nguon UNIQUE (ma_nguon),
    CONSTRAINT ck_hang_cho_gui_hang CHECK (hang IN ('SU_KIEN', 'EMAIL_GUI')),
    CONSTRAINT ck_hang_cho_gui_su_kien CHECK (hang <> 'SU_KIEN' OR kenh IS NOT NULL),
    CONSTRAINT ck_hang_cho_gui_email CHECK (hang <> 'EMAIL_GUI'
        OR (ma_nguon IS NOT NULL AND email_nhan IS NOT NULL AND ngon_ngu IS NOT NULL AND khoa_mau IS NOT NULL
            AND trang_thai IS NOT NULL AND lan_thu_ke_tiep IS NOT NULL))
);
CREATE INDEX idx_hang_cho_gui_su_kien ON hang_cho_gui (hang, ngay_gui, id_hang_cho);
CREATE INDEX idx_hang_cho_gui_email ON hang_cho_gui (trang_thai, lan_thu_ke_tiep);

INSERT INTO hang_cho_gui (id_hang_cho, hang, kenh, loai, id_tai_khoan, id_yeu_cau, noi_dung, email_nhan, ngon_ngu,
                          id_lich_hen, khoa_mau, tham_so, tep_lich, so_lan_loi, ngay_tao, ngay_gui)
SELECT id_su_kien, 'SU_KIEN', kenh, loai, id_tai_khoan, id_yeu_cau, noi_dung, email_nhan, ngon_ngu,
       id_lich_hen, khoa_mau, tham_so, tep_lich, so_lan_loi, ngay_tao, ngay_gui
FROM su_kien_thong_bao
ORDER BY id_su_kien;

SET @san = 1 + GREATEST(
        (SELECT COALESCE(MAX(id_su_kien), 0) FROM su_kien_thong_bao),
        (SELECT COALESCE(MAX(CAST(SUBSTRING(ma_nguon, 9) AS UNSIGNED)), 0) FROM thong_bao
         WHERE ma_nguon LIKE 'booking:%'),
        (SELECT COALESCE(MAX(CAST(SUBSTRING(ma_nguon, 9) AS UNSIGNED)), 0) FROM email_gui
         WHERE ma_nguon LIKE 'booking:%'));
SET @nang_bo_dem = CONCAT('ALTER TABLE hang_cho_gui AUTO_INCREMENT = ', @san);
PREPARE nang_bo_dem FROM @nang_bo_dem;
EXECUTE nang_bo_dem;
DEALLOCATE PREPARE nang_bo_dem;

INSERT INTO hang_cho_gui (hang, ma_nguon, email_nhan, ngon_ngu, khoa_mau, tham_so, id_lich_hen, trang_thai,
                          so_lan_loi, lan_thu_ke_tiep, loi_cuoi, ngay_tao, ngay_gui, tep_lich)
SELECT 'EMAIL_GUI', ma_nguon, nguoi_nhan, ngon_ngu, khoa_mau, tham_so, id_lich_hen, trang_thai,
       so_lan_thu, lan_thu_ke_tiep, loi_cuoi, ngay_tao, ngay_gui, tep_lich
FROM email_gui
ORDER BY id_email;

DROP TABLE su_kien_thong_bao;
DROP TABLE email_gui;
