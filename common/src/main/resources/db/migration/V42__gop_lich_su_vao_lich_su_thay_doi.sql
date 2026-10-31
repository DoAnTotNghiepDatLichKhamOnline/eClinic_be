-- =====================================================================
-- Gộp 4 bảng lịch sử vào lich_su_thay_doi (DOANTOTNGH-19, phase 3)
--  * lich_su_lich_hen (V35), lich_su_ca_lam_viec (V31), lich_su_cau_hinh (V33), lich_su_sua_benh_an (V26) thành 1 bảng,
--    cột loai cho biết dòng thuộc loại nào (JPA SINGLE_TABLE, 4 entity cũ là lớp con).
--  * Đối tượng được ghi lịch sử giữ khoá ngoại riêng (id_lich_hen, id_lich_lam_viec, id_ho_so_benh_an), không dùng 1 cột
--    id chung. Cột dùng chung chỉ khi cùng nghĩa: ly_do, hanh_dong, id_bac_si, id_quan_tri_vien, gia_tri_truoc / sau.
--  * NOT NULL riêng của từng loại thành CHECK theo loai.
--  * Lịch sử ca chép trước, giữ nguyên id (id có trong response); 3 loại còn lại nhận id mới, chép theo thứ tự id cũ nên
--    thứ tự (ngay_tao, id) / phien_ban trong từng loại không đổi.
-- =====================================================================
CREATE TABLE lich_su_thay_doi
(
    id_lich_su             BIGINT      NOT NULL AUTO_INCREMENT,
    loai                   VARCHAR(20) NOT NULL COMMENT 'LICH_HEN | CA_LAM_VIEC | CAU_HINH | BENH_AN',
    hanh_dong              VARCHAR(20) NULL COMMENT 'LICH_HEN: DAT_LICH | DOI_LICH | HUY | CA_BI_HUY | DOI_BAC_SI | CA_THAY_DOI; CA_LAM_VIEC: TAO | SUA | HUY',
    ly_do                  TEXT        NULL COMMENT 'câu người dùng gõ, hoặc {heThong.<khoa>} (VanBanHeThong); BENH_AN: lý do sửa, bắt buộc',
    -- LICH_HEN
    id_lich_hen            BIGINT      NULL COMMENT 'LICH_HEN: lịch hẹn của thao tác',
    loai_nguoi_thuc_hien   VARCHAR(20) NULL COMMENT 'LICH_HEN: KHACH | TAI_KHOAN | BAC_SI | QUAN_TRI_VIEN',
    id_tai_khoan_thuc_hien BIGINT      NULL COMMENT 'LICH_HEN: tài khoản đã làm; NULL khi khách (cầm link phiếu khám)',
    id_lich_hen_lien_quan  BIGINT      NULL COMMENT 'LICH_HEN, DOI_LICH: lịch hẹn mới tạo ra',
    -- CA_LAM_VIEC
    id_lich_lam_viec       BIGINT      NULL COMMENT 'CA_LAM_VIEC: ca được thay đổi',
    nguon                  VARCHAR(20) NULL COMMENT 'CA_LAM_VIEC: QUAN_TRI | DUYET_YEU_CAU | NGUNG_CONG_TAC | SAO_CHEP | NHAP_EXCEL | NGAY_NGHI | DU_LIEU_MAU | DU_LIEU_CU',
    id_quan_tri_vien       BIGINT      NULL COMMENT 'CA_LAM_VIEC: quản trị viên thực hiện (hoặc duyệt yêu cầu); CAU_HINH: quản trị viên đã lưu',
    id_yeu_cau             BIGINT      NULL COMMENT 'CA_LAM_VIEC: yêu cầu đổi ca / xin nghỉ được duyệt; chỉ khi nguon = DUYET_YEU_CAU',
    id_bac_si              BIGINT      NULL COMMENT 'CA_LAM_VIEC: ảnh ca sau thay đổi: bác sĩ; BENH_AN: bác sĩ đã sửa',
    id_phong_kham          BIGINT      NULL COMMENT 'CA_LAM_VIEC: ảnh ca sau thay đổi: phòng khám',
    ngay_lam_viec          DATE        NULL COMMENT 'CA_LAM_VIEC: ảnh ca sau thay đổi: ngày',
    gio_bat_dau            TIME        NULL COMMENT 'CA_LAM_VIEC: ảnh ca sau thay đổi: giờ bắt đầu',
    gio_ket_thuc           TIME        NULL COMMENT 'CA_LAM_VIEC: ảnh ca sau thay đổi: giờ kết thúc',
    so_luot_toi_da_moi_gio INT         NULL COMMENT 'CA_LAM_VIEC: ảnh ca sau thay đổi: N',
    thoi_luong_luot_phut   INT         NULL COMMENT 'CA_LAM_VIEC: ảnh ca sau thay đổi: t (phút)',
    -- CAU_HINH
    ten_truong             VARCHAR(60) NULL COMMENT 'CAU_HINH: tên trường trong API, vd soGioToiDaMoiTuan',
    -- CAU_HINH, BENH_AN
    gia_tri_truoc          TEXT        NULL COMMENT 'CAU_HINH: giá trị cũ; BENH_AN: JSON nội dung hồ sơ trước khi sửa',
    gia_tri_sau            TEXT        NULL COMMENT 'CAU_HINH: giá trị mới; BENH_AN: JSON nội dung hồ sơ sau khi sửa',
    -- BENH_AN
    id_ho_so_benh_an       BIGINT      NULL COMMENT 'BENH_AN: hồ sơ bệnh án được sửa',
    phien_ban              INT         NULL COMMENT 'BENH_AN: phiên bản hồ sơ sau lần sửa này (2, 3, ...)',
    ngay_tao               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'thời điểm thay đổi',
    CONSTRAINT pk_lich_su_thay_doi PRIMARY KEY (id_lich_su),
    CONSTRAINT uk_lich_su_thay_doi_phien_ban UNIQUE (id_ho_so_benh_an, phien_ban),
    CONSTRAINT ck_lich_su_thay_doi_loai CHECK (loai IN ('LICH_HEN', 'CA_LAM_VIEC', 'CAU_HINH', 'BENH_AN')),
    CONSTRAINT ck_lich_su_thay_doi_lich_hen CHECK (loai <> 'LICH_HEN'
        OR (id_lich_hen IS NOT NULL AND hanh_dong IS NOT NULL AND loai_nguoi_thuc_hien IS NOT NULL)),
    CONSTRAINT ck_lich_su_thay_doi_ca CHECK (loai <> 'CA_LAM_VIEC'
        OR (id_lich_lam_viec IS NOT NULL AND hanh_dong IS NOT NULL AND nguon IS NOT NULL
            AND id_quan_tri_vien IS NOT NULL AND id_bac_si IS NOT NULL AND id_phong_kham IS NOT NULL
            AND ngay_lam_viec IS NOT NULL AND gio_bat_dau IS NOT NULL AND gio_ket_thuc IS NOT NULL
            AND so_luot_toi_da_moi_gio IS NOT NULL AND thoi_luong_luot_phut IS NOT NULL)),
    CONSTRAINT ck_lich_su_thay_doi_cau_hinh CHECK (loai <> 'CAU_HINH'
        OR (ten_truong IS NOT NULL AND gia_tri_truoc IS NOT NULL AND gia_tri_sau IS NOT NULL
            AND id_quan_tri_vien IS NOT NULL)),
    CONSTRAINT ck_lich_su_thay_doi_benh_an CHECK (loai <> 'BENH_AN'
        OR (id_ho_so_benh_an IS NOT NULL AND phien_ban IS NOT NULL AND id_bac_si IS NOT NULL AND ly_do IS NOT NULL
            AND gia_tri_truoc IS NOT NULL AND gia_tri_sau IS NOT NULL)),
    CONSTRAINT fk_lich_su_thay_doi_lich_hen FOREIGN KEY (id_lich_hen) REFERENCES lich_hen (id_lich_hen),
    CONSTRAINT fk_lich_su_thay_doi_tai_khoan FOREIGN KEY (id_tai_khoan_thuc_hien) REFERENCES tai_khoan (id_tai_khoan),
    CONSTRAINT fk_lich_su_thay_doi_lien_quan FOREIGN KEY (id_lich_hen_lien_quan) REFERENCES lich_hen (id_lich_hen),
    CONSTRAINT fk_lich_su_thay_doi_ca FOREIGN KEY (id_lich_lam_viec) REFERENCES lich_lam_viec (id_lich_lam_viec),
    CONSTRAINT fk_lich_su_thay_doi_quan_tri FOREIGN KEY (id_quan_tri_vien) REFERENCES quan_tri_vien (id_quan_tri_vien),
    CONSTRAINT fk_lich_su_thay_doi_yeu_cau FOREIGN KEY (id_yeu_cau) REFERENCES yeu_cau_doi_lich (id_yeu_cau),
    CONSTRAINT fk_lich_su_thay_doi_bac_si FOREIGN KEY (id_bac_si) REFERENCES bac_si (id_bac_si),
    CONSTRAINT fk_lich_su_thay_doi_phong_kham FOREIGN KEY (id_phong_kham) REFERENCES phong_kham (id_phong_kham),
    CONSTRAINT fk_lich_su_thay_doi_ho_so_benh_an FOREIGN KEY (id_ho_so_benh_an) REFERENCES ho_so_benh_an (id_ho_so_benh_an)
);
CREATE INDEX idx_lich_su_thay_doi_lich_hen ON lich_su_thay_doi (id_lich_hen, ngay_tao);
CREATE INDEX idx_lich_su_thay_doi_ca ON lich_su_thay_doi (id_lich_lam_viec, ngay_tao);
CREATE INDEX idx_lich_su_thay_doi_loai ON lich_su_thay_doi (loai, ngay_tao);

INSERT INTO lich_su_thay_doi (id_lich_su, loai, hanh_dong, ly_do, id_lich_lam_viec, nguon, id_quan_tri_vien, id_yeu_cau,
                              id_bac_si, id_phong_kham, ngay_lam_viec, gio_bat_dau, gio_ket_thuc,
                              so_luot_toi_da_moi_gio, thoi_luong_luot_phut, ngay_tao)
SELECT id_lich_su_ca, 'CA_LAM_VIEC', hanh_dong, ly_do, id_lich_lam_viec, nguon, id_quan_tri_vien, id_yeu_cau, id_bac_si,
       id_phong_kham, ngay_lam_viec, gio_bat_dau, gio_ket_thuc, so_luot_toi_da_moi_gio, thoi_luong_luot_phut, ngay_tao
FROM lich_su_ca_lam_viec
ORDER BY id_lich_su_ca;

INSERT INTO lich_su_thay_doi (loai, ten_truong, gia_tri_truoc, gia_tri_sau, id_quan_tri_vien, ngay_tao)
SELECT 'CAU_HINH', ten_truong, gia_tri_cu, gia_tri_moi, id_quan_tri_vien, ngay_tao
FROM lich_su_cau_hinh
ORDER BY id_lich_su_cau_hinh;

INSERT INTO lich_su_thay_doi (loai, hanh_dong, ly_do, id_lich_hen, loai_nguoi_thuc_hien, id_tai_khoan_thuc_hien,
                              id_lich_hen_lien_quan, ngay_tao)
SELECT 'LICH_HEN', su_kien, ly_do, id_lich_hen, loai_nguoi_thuc_hien, id_tai_khoan_thuc_hien, id_lich_hen_lien_quan,
       ngay_tao
FROM lich_su_lich_hen
ORDER BY id_lich_su_lich_hen;

INSERT INTO lich_su_thay_doi (loai, ly_do, id_ho_so_benh_an, phien_ban, id_bac_si, gia_tri_truoc, gia_tri_sau, ngay_tao)
SELECT 'BENH_AN', ly_do, id_ho_so_benh_an, phien_ban, id_bac_si, noi_dung_truoc, noi_dung_sau, ngay_tao
FROM lich_su_sua_benh_an
ORDER BY id_lich_su_sua;

DROP TABLE lich_su_ca_lam_viec;
DROP TABLE lich_su_cau_hinh;
DROP TABLE lich_su_lich_hen;
DROP TABLE lich_su_sua_benh_an;
