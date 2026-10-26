-- =====================================================================
-- Lịch sử thay đổi ca làm việc (DOANTOTNGH-13, phase 1)
--  * Mỗi lần xếp / sửa / hủy 1 ca, hệ thống thêm 1 dòng lich_su_ca_lam_viec giữ ẢNH CA SAU THAY ĐỔI (bác sĩ, phòng,
--    ngày, giờ, N, t), ai làm, lúc nào, từ đâu (quản trị viên trực tiếp, duyệt yêu cầu, ngừng công tác...) và lý do.
--    Giá trị TRƯỚC thay đổi là ảnh của dòng liền trước của cùng ca, nên dòng đầu tiên của mọi ca là TAO.
--    Dòng lịch sử chỉ được thêm, không sửa, không xoá.
--  * Ca đã có trước migration này: thêm 1 dòng TAO (và 1 dòng HUY nếu ca đã hủy) với nguon = DU_LIEU_CU, ảnh ca là giá
--    trị hiện tại, thời điểm là lúc xếp ca (không có dữ liệu cũ hơn để biết lần sửa / hủy diễn ra khi nào, do ai).
--  * Giới hạn giờ làm mỗi tuần (SCHED-05) không cần cột mới: tính từ các ca còn hoạt động, ngưỡng là cấu hình
--    app.lich-lam-viec.so-gio-toi-da-moi-tuan của booking-service.
-- =====================================================================
CREATE TABLE lich_su_ca_lam_viec
(
    id_lich_su_ca          BIGINT      NOT NULL AUTO_INCREMENT,
    id_lich_lam_viec       BIGINT      NOT NULL,
    hanh_dong              VARCHAR(20) NOT NULL COMMENT 'TAO | SUA | HUY',
    nguon                  VARCHAR(20) NOT NULL COMMENT 'QUAN_TRI | DUYET_YEU_CAU | NGUNG_CONG_TAC | DU_LIEU_MAU | DU_LIEU_CU',
    id_quan_tri_vien       BIGINT      NOT NULL COMMENT 'quản trị viên thực hiện (hoặc duyệt yêu cầu)',
    id_yeu_cau             BIGINT      NULL COMMENT 'yêu cầu đổi ca / xin nghỉ được duyệt; chỉ khi nguon = DUYET_YEU_CAU',
    id_bac_si              BIGINT      NOT NULL COMMENT 'ảnh ca sau thay đổi: bác sĩ',
    id_phong_kham          BIGINT      NOT NULL COMMENT 'ảnh ca sau thay đổi: phòng khám',
    ngay_lam_viec          DATE        NOT NULL COMMENT 'ảnh ca sau thay đổi: ngày',
    gio_bat_dau            TIME        NOT NULL COMMENT 'ảnh ca sau thay đổi: giờ bắt đầu',
    gio_ket_thuc           TIME        NOT NULL COMMENT 'ảnh ca sau thay đổi: giờ kết thúc',
    so_luot_toi_da_moi_gio INT         NOT NULL COMMENT 'ảnh ca sau thay đổi: N',
    thoi_luong_luot_phut   INT         NOT NULL COMMENT 'ảnh ca sau thay đổi: t (phút)',
    ly_do                  TEXT        NULL COMMENT 'lý do hủy: câu quản trị viên gõ, hoặc {heThong.<khoa>} (VanBanHeThong)',
    ngay_tao               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'thời điểm thay đổi',
    CONSTRAINT pk_lich_su_ca_lam_viec PRIMARY KEY (id_lich_su_ca),
    CONSTRAINT fk_lich_su_ca_lam_viec_ca FOREIGN KEY (id_lich_lam_viec) REFERENCES lich_lam_viec (id_lich_lam_viec),
    CONSTRAINT fk_lich_su_ca_lam_viec_quan_tri FOREIGN KEY (id_quan_tri_vien) REFERENCES quan_tri_vien (id_quan_tri_vien),
    CONSTRAINT fk_lich_su_ca_lam_viec_yeu_cau FOREIGN KEY (id_yeu_cau) REFERENCES yeu_cau_doi_lich (id_yeu_cau),
    CONSTRAINT fk_lich_su_ca_lam_viec_bac_si FOREIGN KEY (id_bac_si) REFERENCES bac_si (id_bac_si),
    CONSTRAINT fk_lich_su_ca_lam_viec_phong_kham FOREIGN KEY (id_phong_kham) REFERENCES phong_kham (id_phong_kham)
);
CREATE INDEX idx_lich_su_ca_lam_viec_ca ON lich_su_ca_lam_viec (id_lich_lam_viec, ngay_tao);

INSERT INTO lich_su_ca_lam_viec (id_lich_lam_viec, hanh_dong, nguon, id_quan_tri_vien, id_bac_si, id_phong_kham,
                                 ngay_lam_viec, gio_bat_dau, gio_ket_thuc, so_luot_toi_da_moi_gio,
                                 thoi_luong_luot_phut, ngay_tao)
SELECT id_lich_lam_viec, 'TAO', 'DU_LIEU_CU', id_admin_tao, id_bac_si, id_phong_kham, ngay_lam_viec, gio_bat_dau,
       gio_ket_thuc, so_luot_toi_da_moi_gio, thoi_luong_luot_phut, ngay_tao
FROM lich_lam_viec
ORDER BY id_lich_lam_viec;

INSERT INTO lich_su_ca_lam_viec (id_lich_lam_viec, hanh_dong, nguon, id_quan_tri_vien, id_bac_si, id_phong_kham,
                                 ngay_lam_viec, gio_bat_dau, gio_ket_thuc, so_luot_toi_da_moi_gio,
                                 thoi_luong_luot_phut, ngay_tao)
SELECT id_lich_lam_viec, 'HUY', 'DU_LIEU_CU', id_admin_tao, id_bac_si, id_phong_kham, ngay_lam_viec, gio_bat_dau,
       gio_ket_thuc, so_luot_toi_da_moi_gio, thoi_luong_luot_phut, ngay_tao
FROM lich_lam_viec
WHERE trang_thai = 'DA_HUY'
ORDER BY id_lich_lam_viec;
