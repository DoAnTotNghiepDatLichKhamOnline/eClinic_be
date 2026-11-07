-- =====================================================================
-- eClinic - Thêm ca làm cuối tuần cho 1 bác sĩ (mặc định bacsi01) + 10 lịch hẹn mỗi ca (dữ liệu TEST)
--
-- Làm gì:
--   1. Tạo 4 ca cho bacsi01: Thứ 7 và Chủ nhật, mỗi ngày 1 ca sáng (08:00-11:30) + 1 ca chiều (13:30-17:00).
--      Ca nào đã có sẵn (vd ca sáng thứ 7 do dữ liệu mẫu tạo) thì GIỮ NGUYÊN, không tạo trùng.
--   2. Chia mỗi ca thành các khung giờ khám (khung_gio_kham) y như app: 6 lượt/giờ, mỗi lượt 10 phút = 21 lượt/ca.
--   3. Tạo 10 hồ sơ bệnh nhân test (CCCD 07999000xxxx) và đặt đủ 10 lịch hẹn cho MỖI ca (tính cả lịch đã có).
--
-- Chạy lại nhiều lần vẫn an toàn: ca nào đã đủ 10 lịch thì không thêm nữa.
-- Chạy trên MySQL 8, đúng database của eClinic (chạy cả file trong 1 phiên: mysql CLI hoặc phpMyAdmin tab SQL).
-- Cần đã chạy xong Flyway (V1..V46) và có sẵn tài khoản bacsi01@eclinic.local (SEED_DATA=true).
--
-- KHÔNG phải migration Flyway: đừng đặt vào common/src/main/resources/db/migration. Chạy tay khi cần (xem README.md cùng thư mục):
--   docker exec -i eclinic-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD exec mysql -uroot --default-character-set=utf8mb4 -t "$MYSQL_DATABASE"' < scripts/du-lieu-test/them_ca_bac_si_cuoi_tuan.sql
-- =====================================================================
SET NAMES utf8mb4;
-- Dùng đúng collation của database để biến chuỗi (@email_bac_si) so sánh được với cột (tránh lỗi 1267 Illegal mix of collations)
SET collation_connection = @@collation_database;

-- ---------- THAM SỐ: sửa ở đây ----------
SET @email_bac_si    = 'bacsi01@eclinic.local';
SET @ngay_thu_7      = '2026-11-07';   -- Thứ 7  (muốn cuối tuần sau: '2026-11-14')
SET @ngay_chu_nhat   = '2026-11-08';   -- Chủ nhật (muốn cuối tuần sau: '2026-11-15')
SET @so_lich_moi_ca  = 10;             -- tổng số lịch hẹn mỗi ca
SET @so_luot_moi_gio = 6;              -- N, giống dữ liệu mẫu (DuLieuMau.SO_LUOT_MOI_GIO)
SET @phut_moi_luot   = 10;             -- t, giống dữ liệu mẫu (DuLieuMau.SO_PHUT_MOI_LUOT)

-- ---------- Tra id bác sĩ, phòng khám, quản trị viên ----------
SELECT b.id_bac_si, b.id_chuyen_khoa
  INTO @id_bac_si, @id_chuyen_khoa
  FROM bac_si b
  JOIN tai_khoan t ON t.id_tai_khoan = b.id_tai_khoan
 WHERE t.email = @email_bac_si;

-- Phòng: ưu tiên phòng bác sĩ đang dùng ở ca gần nhất, không có thì phòng đầu tiên của chuyên khoa
SELECT COALESCE(
           (SELECT l.id_phong_kham FROM lich_lam_viec l
             WHERE l.id_bac_si = @id_bac_si
             ORDER BY l.ngay_lam_viec DESC, l.id_lich_lam_viec DESC LIMIT 1),
           (SELECT p.id_phong_kham FROM phong_kham p
             WHERE p.id_chuyen_khoa = @id_chuyen_khoa AND p.trang_thai = 'HOAT_DONG'
             ORDER BY p.id_phong_kham LIMIT 1))
  INTO @id_phong_kham;

SELECT COALESCE(
           (SELECT q.id_quan_tri_vien FROM quan_tri_vien q
              JOIN tai_khoan t ON t.id_tai_khoan = q.id_tai_khoan
             WHERE t.email = 'admin@eclinic.local'),
           (SELECT MIN(id_quan_tri_vien) FROM quan_tri_vien))
  INTO @id_admin;

-- Kiểm tra nhanh: cả 3 giá trị phải khác NULL
SELECT @id_bac_si AS id_bac_si, @id_phong_kham AS id_phong_kham, @id_admin AS id_admin;

-- ---------- Bảng tạm ----------
DROP TEMPORARY TABLE IF EXISTS tmp_ca_muc_tieu;
CREATE TEMPORARY TABLE tmp_ca_muc_tieu (
    ngay         DATE NOT NULL,
    gio_bat_dau  TIME NOT NULL,
    gio_ket_thuc TIME NOT NULL,
    PRIMARY KEY (ngay, gio_bat_dau)
);
INSERT INTO tmp_ca_muc_tieu (ngay, gio_bat_dau, gio_ket_thuc) VALUES
    (@ngay_thu_7,    '08:00:00', '11:30:00'),
    (@ngay_thu_7,    '13:30:00', '17:00:00'),
    (@ngay_chu_nhat, '08:00:00', '11:30:00'),
    (@ngay_chu_nhat, '13:30:00', '17:00:00');

DROP TEMPORARY TABLE IF EXISTS tmp_khung;
CREATE TEMPORARY TABLE tmp_khung (f INT NOT NULL PRIMARY KEY);   -- thứ tự khung 1 giờ trong ca
INSERT INTO tmp_khung (f) VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9),(10),(11);

DROP TEMPORARY TABLE IF EXISTS tmp_luot;
CREATE TEMPORARY TABLE tmp_luot (p INT NOT NULL PRIMARY KEY);    -- vị trí lượt khám trong khung giờ
INSERT INTO tmp_luot (p) VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9),(10),(11),(12),(13),(14),(15),(16),(17),(18),(19);

-- ---------- 1. Tạo ca làm việc (bỏ qua ca đã có hoặc bị chồng giờ với ca khác của bác sĩ / phòng) ----------
INSERT INTO lich_lam_viec (id_bac_si, id_phong_kham, id_admin_tao, ngay_lam_viec, gio_bat_dau, gio_ket_thuc,
                           so_benh_nhan_toi_da, so_luot_toi_da_moi_gio, thoi_luong_luot_phut, trang_thai)
SELECT @id_bac_si, @id_phong_kham, @id_admin, m.ngay, m.gio_bat_dau, m.gio_ket_thuc,
       (SELECT COUNT(*)
          FROM tmp_khung k
          JOIN tmp_luot v ON v.p < @so_luot_moi_gio
         WHERE k.f * 60 + v.p * @phut_moi_luot + @phut_moi_luot
               <= (TIME_TO_SEC(m.gio_ket_thuc) - TIME_TO_SEC(m.gio_bat_dau)) / 60),
       @so_luot_moi_gio, @phut_moi_luot, 'HOAT_DONG'
  FROM tmp_ca_muc_tieu m
 WHERE NOT EXISTS (SELECT 1 FROM lich_lam_viec x
                    WHERE x.trang_thai = 'HOAT_DONG'
                      AND x.ngay_lam_viec = m.ngay
                      AND x.gio_bat_dau < m.gio_ket_thuc
                      AND x.gio_ket_thuc > m.gio_bat_dau
                      AND (x.id_bac_si = @id_bac_si OR x.id_phong_kham = @id_phong_kham));

-- ---------- 2. Chia ca thành các khung giờ khám (đã có khung thì bỏ qua nhờ UNIQUE) ----------
INSERT INTO khung_gio_kham (id_lich_lam_viec, gio_bat_dau, gio_ket_thuc, trang_thai)
SELECT l.id_lich_lam_viec,
       TIMESTAMP(l.ngay_lam_viec, SEC_TO_TIME(TIME_TO_SEC(l.gio_bat_dau)
                                              + (k.f * 60 + v.p * l.thoi_luong_luot_phut) * 60)),
       TIMESTAMP(l.ngay_lam_viec, SEC_TO_TIME(TIME_TO_SEC(l.gio_bat_dau)
                                              + (k.f * 60 + v.p * l.thoi_luong_luot_phut
                                                 + l.thoi_luong_luot_phut) * 60)),
       'CON_TRONG'
  FROM lich_lam_viec l
  JOIN tmp_ca_muc_tieu m ON m.ngay = l.ngay_lam_viec
                        AND m.gio_bat_dau = l.gio_bat_dau
                        AND m.gio_ket_thuc = l.gio_ket_thuc
  JOIN tmp_khung k
  JOIN tmp_luot v ON v.p < l.so_luot_toi_da_moi_gio
 WHERE l.id_bac_si = @id_bac_si
   AND l.trang_thai = 'HOAT_DONG'
   AND k.f * 60 + v.p * l.thoi_luong_luot_phut + l.thoi_luong_luot_phut
       <= (TIME_TO_SEC(l.gio_ket_thuc) - TIME_TO_SEC(l.gio_bat_dau)) / 60
ON DUPLICATE KEY UPDATE id_khung_gio = khung_gio_kham.id_khung_gio;

-- Lịch sử ca (dòng TAO) cho ca chưa có lịch sử, giống dữ liệu mẫu
INSERT INTO lich_su_thay_doi (loai, hanh_dong, id_lich_lam_viec, nguon, id_quan_tri_vien, id_bac_si, id_phong_kham,
                              ngay_lam_viec, gio_bat_dau, gio_ket_thuc, so_luot_toi_da_moi_gio, thoi_luong_luot_phut)
SELECT 'CA_LAM_VIEC', 'TAO', l.id_lich_lam_viec, 'DU_LIEU_MAU', l.id_admin_tao, l.id_bac_si, l.id_phong_kham,
       l.ngay_lam_viec, l.gio_bat_dau, l.gio_ket_thuc, l.so_luot_toi_da_moi_gio, l.thoi_luong_luot_phut
  FROM lich_lam_viec l
  JOIN tmp_ca_muc_tieu m ON m.ngay = l.ngay_lam_viec
                        AND m.gio_bat_dau = l.gio_bat_dau
                        AND m.gio_ket_thuc = l.gio_ket_thuc
 WHERE l.id_bac_si = @id_bac_si
   AND l.trang_thai = 'HOAT_DONG'
   AND NOT EXISTS (SELECT 1 FROM lich_su_thay_doi h
                    WHERE h.loai = 'CA_LAM_VIEC' AND h.id_lich_lam_viec = l.id_lich_lam_viec);

-- ---------- 3. Hồ sơ bệnh nhân test (khách, chưa gắn tài khoản) ----------
INSERT INTO ho_so_benh_nhan (cccd, ho_ten, ngay_sinh, gioi_tinh, so_dien_thoai, dia_chi, trang_thai_lien_ket)
SELECT CONCAT('07999000', LPAD(n.i, 4, '0')),
       ELT(n.i, 'Nguyễn Văn Bình', 'Trần Thị Cúc', 'Lê Minh Dũng', 'Phạm Thị Em', 'Hoàng Văn Phúc',
                'Vũ Thị Gấm', 'Đặng Quốc Huy', 'Bùi Thị Lan', 'Ngô Văn Khải', 'Đỗ Thị Mai'),
       DATE_SUB('1990-01-15', INTERVAL n.i * 400 DAY),
       IF(n.i % 2 = 1, 'NAM', 'NU'),
       CONCAT('09099000', LPAD(n.i, 2, '0')),
       'Quận 10, TP. Hồ Chí Minh',
       'CHUA_LIEN_KET'
  FROM (SELECT 1 AS i UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
        UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10) n
 WHERE NOT EXISTS (SELECT 1 FROM ho_so_benh_nhan h WHERE h.cccd = CONCAT('07999000', LPAD(n.i, 4, '0')));

-- ---------- 4. Đặt lịch hẹn: bù cho đủ @so_lich_moi_ca lịch / ca, lấy các khung giờ trống sớm nhất ----------
INSERT INTO lich_hen (id_ho_so_benh_nhan, id_bac_si, id_khung_gio, id_phong_kham, so_thu_tu, trang_thai,
                      ly_do_kham, so_dien_thoai_lien_he, ngon_ngu, ho_ten_da_nhap, ngay_sinh_da_nhap,
                      gioi_tinh_da_nhap, ma_token_phieu_kham, ma_tra_cuu, kenh_dat)
SELECT hs.id_ho_so_benh_nhan, @id_bac_si, s.id_khung_gio, @id_phong_kham,
       -- Số thứ tự = thứ hạng của lượt trong các lượt của phòng trong ngày (giống DatLichServiceImpl.soThuTu)
       (SELECT COUNT(*)
          FROM khung_gio_kham k2
          JOIN lich_lam_viec l2 ON l2.id_lich_lam_viec = k2.id_lich_lam_viec
         WHERE l2.id_phong_kham = @id_phong_kham
           AND l2.ngay_lam_viec = s.ngay
           AND (k2.gio_bat_dau < s.gio_bat_dau
                OR (k2.gio_bat_dau = s.gio_bat_dau AND k2.id_khung_gio < s.id_khung_gio))) + 1,
       'DA_XAC_NHAN',
       ELT(MOD(s.stt_chung - 1, 5) + 1, 'Khám tổng quát', 'Đau đầu, chóng mặt kéo dài', 'Ho, sốt nhẹ 3 ngày',
           'Đau dạ dày', 'Tái khám huyết áp'),
       hs.so_dien_thoai, 'VI', hs.ho_ten, hs.ngay_sinh, hs.gioi_tinh,
       SHA2(CONCAT(UUID(), s.id_khung_gio), 256),
       CONCAT('ECL-', DATE_FORMAT(s.ngay, '%Y%m%d'), '-', LPAD(9000 + s.stt_trong_ngay, 4, '0')),
       'KHACH'
  FROM (
        SELECT t.*,
               ROW_NUMBER() OVER (ORDER BY t.ngay, t.gio_bat_dau)                    AS stt_chung,
               ROW_NUMBER() OVER (PARTITION BY t.ngay ORDER BY t.gio_bat_dau)        AS stt_trong_ngay
          FROM (
                SELECT g.id_khung_gio, g.gio_bat_dau, l.ngay_lam_viec AS ngay,
                       ROW_NUMBER() OVER (PARTITION BY g.id_lich_lam_viec ORDER BY g.gio_bat_dau) AS thu_tu_trong_ca,
                       (SELECT COUNT(*)
                          FROM lich_hen h
                          JOIN khung_gio_kham g3 ON g3.id_khung_gio = h.id_khung_gio
                         WHERE g3.id_lich_lam_viec = l.id_lich_lam_viec
                           AND h.trang_thai IN ('CHO_XAC_NHAN', 'DA_XAC_NHAN', 'DA_HOAN_THANH')) AS da_co
                  FROM khung_gio_kham g
                  JOIN lich_lam_viec l ON l.id_lich_lam_viec = g.id_lich_lam_viec
                  JOIN tmp_ca_muc_tieu m ON m.ngay = l.ngay_lam_viec
                                        AND m.gio_bat_dau = l.gio_bat_dau
                                        AND m.gio_ket_thuc = l.gio_ket_thuc
                 WHERE l.id_bac_si = @id_bac_si
                   AND l.trang_thai = 'HOAT_DONG'
                   AND g.trang_thai = 'CON_TRONG'
                   AND NOT EXISTS (SELECT 1 FROM lich_hen h2
                                    WHERE h2.id_khung_gio = g.id_khung_gio
                                      AND h2.trang_thai IN ('CHO_XAC_NHAN', 'DA_XAC_NHAN', 'DA_HOAN_THANH'))
               ) t
         WHERE t.thu_tu_trong_ca <= @so_lich_moi_ca - t.da_co
       ) s
  JOIN ho_so_benh_nhan hs
    ON hs.cccd = CONCAT('07999000', LPAD(MOD(s.stt_chung - 1, 10) + 1, 4, '0'));

-- Khung giờ đã có lịch hẹn còn hiệu lực -> DA_DAT (giống lúc đặt lịch thật)
UPDATE khung_gio_kham g
  JOIN lich_hen h ON h.id_khung_gio = g.id_khung_gio
                 AND h.trang_thai IN ('CHO_XAC_NHAN', 'DA_XAC_NHAN', 'DA_HOAN_THANH')
  JOIN lich_lam_viec l ON l.id_lich_lam_viec = g.id_lich_lam_viec
  JOIN tmp_ca_muc_tieu m ON m.ngay = l.ngay_lam_viec
                        AND m.gio_bat_dau = l.gio_bat_dau
                        AND m.gio_ket_thuc = l.gio_ket_thuc
   SET g.trang_thai = 'DA_DAT'
 WHERE l.id_bac_si = @id_bac_si
   AND g.trang_thai = 'CON_TRONG';

-- Lịch sử lịch hẹn (dòng DAT_LICH) cho lịch hẹn test chưa có lịch sử
INSERT INTO lich_su_thay_doi (loai, hanh_dong, id_lich_hen, loai_nguoi_thuc_hien)
SELECT 'LICH_HEN', 'DAT_LICH', h.id_lich_hen, 'KHACH'
  FROM lich_hen h
  JOIN ho_so_benh_nhan hs ON hs.id_ho_so_benh_nhan = h.id_ho_so_benh_nhan
 WHERE hs.cccd LIKE '07999000%'
   AND h.id_bac_si = @id_bac_si
   AND NOT EXISTS (SELECT 1 FROM lich_su_thay_doi x WHERE x.loai = 'LICH_HEN' AND x.id_lich_hen = h.id_lich_hen);

-- ---------- KẾT QUẢ ----------
-- (a) Ca nào không tạo được (do chồng giờ với ca khác) sẽ KHÔNG xuất hiện ở bảng này
SELECT l.id_lich_lam_viec, l.ngay_lam_viec, DAYNAME(l.ngay_lam_viec) AS thu, l.gio_bat_dau, l.gio_ket_thuc,
       l.so_benh_nhan_toi_da AS so_khung_gio,
       (SELECT COUNT(*) FROM khung_gio_kham g WHERE g.id_lich_lam_viec = l.id_lich_lam_viec) AS khung_gio_da_tao,
       (SELECT COUNT(*) FROM lich_hen h JOIN khung_gio_kham g ON g.id_khung_gio = h.id_khung_gio
         WHERE g.id_lich_lam_viec = l.id_lich_lam_viec
           AND h.trang_thai IN ('CHO_XAC_NHAN', 'DA_XAC_NHAN', 'DA_HOAN_THANH')) AS so_lich_hen
  FROM lich_lam_viec l
  JOIN tmp_ca_muc_tieu m ON m.ngay = l.ngay_lam_viec AND m.gio_bat_dau = l.gio_bat_dau
 WHERE l.id_bac_si = @id_bac_si AND l.trang_thai = 'HOAT_DONG'
 ORDER BY l.ngay_lam_viec, l.gio_bat_dau;

-- (b) Danh sách lịch hẹn của bacsi01 trong 2 ngày
SELECT l.ngay_lam_viec, TIME(g.gio_bat_dau) AS gio_kham, h.so_thu_tu, h.ma_tra_cuu, hs.ho_ten, h.trang_thai
  FROM lich_hen h
  JOIN khung_gio_kham g ON g.id_khung_gio = h.id_khung_gio
  JOIN lich_lam_viec l ON l.id_lich_lam_viec = g.id_lich_lam_viec
  JOIN ho_so_benh_nhan hs ON hs.id_ho_so_benh_nhan = h.id_ho_so_benh_nhan
 WHERE h.id_bac_si = @id_bac_si
   AND l.ngay_lam_viec IN (@ngay_thu_7, @ngay_chu_nhat)
   AND h.trang_thai IN ('CHO_XAC_NHAN', 'DA_XAC_NHAN', 'DA_HOAN_THANH')
 ORDER BY g.gio_bat_dau;

DROP TEMPORARY TABLE IF EXISTS tmp_ca_muc_tieu;
DROP TEMPORARY TABLE IF EXISTS tmp_khung;
DROP TEMPORARY TABLE IF EXISTS tmp_luot;

-- =====================================================================
-- HOÀN TÁC (bỏ comment rồi chạy riêng nếu muốn xoá dữ liệu test): xoá lịch hẹn + hồ sơ test, trả khung giờ về trống.
-- Ca làm việc giữ nguyên; muốn bỏ ca thì huỷ trong giao diện quản trị.
-- =====================================================================
-- UPDATE khung_gio_kham g JOIN lich_hen h ON h.id_khung_gio = g.id_khung_gio
--   JOIN ho_so_benh_nhan hs ON hs.id_ho_so_benh_nhan = h.id_ho_so_benh_nhan
--    SET g.trang_thai = 'CON_TRONG' WHERE hs.cccd LIKE '07999000%';
-- DELETE x FROM lich_su_thay_doi x JOIN lich_hen h ON h.id_lich_hen = x.id_lich_hen
--   JOIN ho_so_benh_nhan hs ON hs.id_ho_so_benh_nhan = h.id_ho_so_benh_nhan WHERE hs.cccd LIKE '07999000%';
-- DELETE h FROM lich_hen h JOIN ho_so_benh_nhan hs ON hs.id_ho_so_benh_nhan = h.id_ho_so_benh_nhan
--  WHERE hs.cccd LIKE '07999000%';
-- DELETE FROM ho_so_benh_nhan WHERE cccd LIKE '07999000%';
