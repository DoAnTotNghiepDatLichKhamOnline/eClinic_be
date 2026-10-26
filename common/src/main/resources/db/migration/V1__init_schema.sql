-- =====================================================================
-- eClinic - Hệ thống đặt lịch khám bệnh
-- Flyway migration V1: tạo toàn bộ bảng, khoá ngoại, index, ràng buộc.
-- Tên bảng/cột theo ERD trong báo cáo (Hình 3.3), viết snake_case; entity Java ở module common.
--
-- QUY ƯỚC
--  * InnoDB + utf8mb4, khoá chính BIGINT AUTO_INCREMENT, tên cột khoá chính theo ERD (id_tai_khoan, id_lich_hen...).
--  * Enum lưu dạng VARCHAR (Java: @Enumerated(STRING)); giá trị hợp lệ ghi ở COMMENT.
--  * Khoá ngoại mặc định RESTRICT (không cho xoá cha khi còn con).
--    Chỉ CASCADE với bảng con thuần tuý: refresh_token, chi_tiet_don_thuoc, tin_nhan_chat.
--  * Tất cả service dùng chung DB này; Flyway chạy ở mọi service (có khoá, an toàn khi khởi động song song).
--  * KHÔNG sửa file này sau khi đã chạy trên máy ai đó -> tạo V2__..., V3__... mới.
--
-- KHÁC VỚI ERD (Hình 3.3) - xem yeu-cau-toan-dien-dat-lich-kham-benh.md mục 15.4
--  * Bỏ co_so_y_te, phong_kham.id_co_so, quan_tri_vien.id_co_so_quan_ly (Q4 - chỉ 1 bệnh viện).
--  * Bỏ thanh_toan, bac_si.phi_kham (Q1 - thanh toán không nằm trong MVP).
--  * ho_so_benh_nhan: + tien_su_benh_ly, + trang_thai_lien_ket (Q2 - liên kết hồ sơ CCCD phải xác minh).
--  * yeu_cau_doi_lich: + loai_yeu_cau, + id_phong_kham_mong_muon, + ghi_chu_xu_ly;
--    thoi_gian_mong_muon tách thành ngay_mong_muon + gio_bat_dau_mong_muon + gio_ket_thuc_mong_muon.
--  * lich_hen: + ly_do_huy, + can_doi_lich, + id_lich_hen_cu (đổi lịch = hủy + tạo mới), + version.
--  * bac_si: + hoc_vi, + trang_thai; lich_lam_viec: + trang_thai; khung_gio_kham: + trang thái DA_HUY, + version.
--  * tai_khoan: + ly_do_vo_hieu_hoa, so_dien_thoai UNIQUE.
--  * refresh_token: lưu token_hash (SHA-256) thay vì token gốc, + ngay_thu_hoi.
--  * thuoc: + ten_chuan_hoa (UNIQUE), + id_bac_si_tao. thong_bao: + id_yeu_cau. phien_chat: + ma_dinh_danh_khach.
--  * lich_hen.id_khung_gio_hieu_luc (generated + UNIQUE): DB chặn 2 lịch hẹn còn hiệu lực trên cùng 1 khung giờ.
--  * yeu_cau_doi_lich.id_lich_lam_viec_cho_duyet (generated + UNIQUE): mỗi ca tối đa 1 yêu cầu CHO_DUYET.
-- =====================================================================

-- ---------- identity-service ----------
CREATE TABLE tai_khoan
(
    id_tai_khoan      BIGINT       NOT NULL AUTO_INCREMENT,
    ho_ten            VARCHAR(150),
    email             VARCHAR(255) NOT NULL,
    mat_khau_hash     VARCHAR(255) NOT NULL,
    so_dien_thoai     VARCHAR(20),
    anh_dai_dien      VARCHAR(500),
    vai_tro           VARCHAR(20)  NOT NULL COMMENT 'BENH_NHAN | BAC_SI | QUAN_TRI_VIEN',
    trang_thai        VARCHAR(20)  NOT NULL DEFAULT 'CHO_XAC_NHAN' COMMENT 'CHO_XAC_NHAN | DA_KICH_HOAT | VO_HIEU_HOA',
    ly_do_vo_hieu_hoa VARCHAR(500),
    ngay_tao          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ngay_cap_nhat     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_tai_khoan PRIMARY KEY (id_tai_khoan),
    CONSTRAINT uk_tai_khoan_email UNIQUE (email),
    CONSTRAINT uk_tai_khoan_so_dien_thoai UNIQUE (so_dien_thoai)
);

-- ---------- catalog-service (tạo trước vì nhiều bảng tham chiếu) ----------
CREATE TABLE chuyen_khoa
(
    id_chuyen_khoa  BIGINT       NOT NULL AUTO_INCREMENT,
    ten_chuyen_khoa VARCHAR(150) NOT NULL,
    mo_ta           TEXT,
    CONSTRAINT pk_chuyen_khoa PRIMARY KEY (id_chuyen_khoa),
    CONSTRAINT uk_chuyen_khoa_ten UNIQUE (ten_chuyen_khoa)
);

CREATE TABLE quan_tri_vien
(
    id_quan_tri_vien BIGINT      NOT NULL AUTO_INCREMENT,
    id_tai_khoan     BIGINT      NOT NULL,
    cap_do_quyen     VARCHAR(20) NOT NULL DEFAULT 'NHAN_VIEN' COMMENT 'TOAN_QUYEN | NHAN_VIEN',
    CONSTRAINT pk_quan_tri_vien PRIMARY KEY (id_quan_tri_vien),
    CONSTRAINT uk_quan_tri_vien_tai_khoan UNIQUE (id_tai_khoan),
    CONSTRAINT fk_quan_tri_vien_tai_khoan FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan (id_tai_khoan)
);

CREATE TABLE bac_si
(
    id_bac_si          BIGINT      NOT NULL AUTO_INCREMENT,
    id_tai_khoan       BIGINT      NOT NULL,
    id_chuyen_khoa     BIGINT      NOT NULL,
    so_giay_phep       VARCHAR(50),
    hoc_vi             VARCHAR(100) COMMENT 'vd: ThS.BS, TS.BS, BSCKII',
    tieu_su            TEXT,
    so_nam_kinh_nghiem INT,
    trang_thai         VARCHAR(20) NOT NULL DEFAULT 'DANG_CONG_TAC' COMMENT 'DANG_CONG_TAC | NGUNG_CONG_TAC',
    CONSTRAINT pk_bac_si PRIMARY KEY (id_bac_si),
    CONSTRAINT uk_bac_si_tai_khoan UNIQUE (id_tai_khoan),
    CONSTRAINT uk_bac_si_so_giay_phep UNIQUE (so_giay_phep),
    CONSTRAINT fk_bac_si_tai_khoan FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan (id_tai_khoan),
    CONSTRAINT fk_bac_si_chuyen_khoa FOREIGN KEY (id_chuyen_khoa) REFERENCES chuyen_khoa (id_chuyen_khoa),
    CONSTRAINT ck_bac_si_kinh_nghiem CHECK (so_nam_kinh_nghiem IS NULL OR so_nam_kinh_nghiem >= 0)
);
CREATE INDEX idx_bac_si_chuyen_khoa ON bac_si (id_chuyen_khoa);

CREATE TABLE phong_kham
(
    id_phong_kham  BIGINT       NOT NULL AUTO_INCREMENT,
    id_chuyen_khoa BIGINT       NOT NULL,
    ten_phong      VARCHAR(100) NOT NULL COMMENT 'vd: Phòng 203',
    tang           VARCHAR(20),
    trang_thai     VARCHAR(20)  NOT NULL DEFAULT 'HOAT_DONG' COMMENT 'HOAT_DONG | NGUNG_HOAT_DONG',
    CONSTRAINT pk_phong_kham PRIMARY KEY (id_phong_kham),
    CONSTRAINT uk_phong_kham_ten UNIQUE (ten_phong),
    CONSTRAINT fk_phong_kham_chuyen_khoa FOREIGN KEY (id_chuyen_khoa) REFERENCES chuyen_khoa (id_chuyen_khoa)
);

-- ---------- identity-service: phiên đăng nhập ----------
CREATE TABLE refresh_token
(
    id_refresh_token   BIGINT       NOT NULL AUTO_INCREMENT,
    id_tai_khoan       BIGINT       NOT NULL,
    token_hash         VARCHAR(64)  NOT NULL COMMENT 'SHA-256 hex của refresh token, KHÔNG lưu token gốc',
    thong_tin_thiet_bi VARCHAR(255),
    ngay_het_han       DATETIME(6)  NOT NULL,
    ngay_thu_hoi       DATETIME(6) COMMENT 'NULL = còn hiệu lực',
    ngay_tao           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_refresh_token PRIMARY KEY (id_refresh_token),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_tai_khoan FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan (id_tai_khoan) ON DELETE CASCADE
);
CREATE INDEX idx_refresh_token_tai_khoan ON refresh_token (id_tai_khoan);

-- ---------- booking-service: hồ sơ bệnh nhân (định danh theo CCCD) ----------
CREATE TABLE ho_so_benh_nhan
(
    id_ho_so_benh_nhan  BIGINT       NOT NULL AUTO_INCREMENT,
    id_tai_khoan        BIGINT COMMENT 'NULL = chưa liên kết tài khoản (Khách đặt lịch)',
    cccd                VARCHAR(12)  NOT NULL,
    ho_ten              VARCHAR(150) NOT NULL,
    ngay_sinh           DATE,
    gioi_tinh           VARCHAR(10) COMMENT 'NAM | NU | KHAC',
    so_dien_thoai       VARCHAR(20)  NOT NULL,
    dia_chi             VARCHAR(500),
    so_bao_hiem_y_te    VARCHAR(50),
    tien_su_benh_ly     TEXT,
    trang_thai_lien_ket VARCHAR(20)  NOT NULL DEFAULT 'CHUA_LIEN_KET' COMMENT 'CHUA_LIEN_KET | CHO_XAC_MINH | DA_LIEN_KET',
    ngay_tao            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_ho_so_benh_nhan PRIMARY KEY (id_ho_so_benh_nhan),
    CONSTRAINT uk_ho_so_benh_nhan_cccd UNIQUE (cccd),
    CONSTRAINT uk_ho_so_benh_nhan_tai_khoan UNIQUE (id_tai_khoan),
    CONSTRAINT fk_ho_so_benh_nhan_tai_khoan FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan (id_tai_khoan),
    -- Chưa gắn tài khoản <=> CHUA_LIEN_KET
    CONSTRAINT ck_ho_so_benh_nhan_lien_ket CHECK (
        (id_tai_khoan IS NULL AND trang_thai_lien_ket = 'CHUA_LIEN_KET')
            OR (id_tai_khoan IS NOT NULL AND trang_thai_lien_ket IN ('CHO_XAC_MINH', 'DA_LIEN_KET')))
);
CREATE INDEX idx_ho_so_benh_nhan_so_dien_thoai ON ho_so_benh_nhan (so_dien_thoai);

-- ---------- scheduling-service ----------
CREATE TABLE lich_lam_viec
(
    id_lich_lam_viec    BIGINT      NOT NULL AUTO_INCREMENT,
    id_bac_si           BIGINT      NOT NULL,
    id_phong_kham       BIGINT      NOT NULL,
    id_admin_tao        BIGINT      NOT NULL COMMENT 'quản trị viên xếp ca (SCHED-01)',
    ngay_lam_viec       DATE        NOT NULL,
    gio_bat_dau         TIME        NOT NULL,
    gio_ket_thuc        TIME        NOT NULL,
    so_benh_nhan_toi_da INT         NOT NULL COMMENT 'ca được chia thành đúng số khung giờ này, mỗi khung 1 BN (Q3)',
    trang_thai          VARCHAR(20) NOT NULL DEFAULT 'HOAT_DONG' COMMENT 'HOAT_DONG | DA_HUY',
    ngay_tao            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_lich_lam_viec PRIMARY KEY (id_lich_lam_viec),
    CONSTRAINT fk_lich_lam_viec_bac_si FOREIGN KEY (id_bac_si) REFERENCES bac_si (id_bac_si),
    CONSTRAINT fk_lich_lam_viec_phong_kham FOREIGN KEY (id_phong_kham) REFERENCES phong_kham (id_phong_kham),
    CONSTRAINT fk_lich_lam_viec_admin_tao FOREIGN KEY (id_admin_tao) REFERENCES quan_tri_vien (id_quan_tri_vien),
    CONSTRAINT ck_lich_lam_viec_gio CHECK (gio_ket_thuc > gio_bat_dau),
    CONSTRAINT ck_lich_lam_viec_so_benh_nhan CHECK (so_benh_nhan_toi_da > 0)
);
-- Chồng giờ (overlap) không kiểm được bằng UNIQUE -> kiểm ở LichLamViecService.assertNoOverlap
CREATE INDEX idx_lich_lam_viec_bac_si_ngay ON lich_lam_viec (id_bac_si, ngay_lam_viec);
CREATE INDEX idx_lich_lam_viec_phong_kham_ngay ON lich_lam_viec (id_phong_kham, ngay_lam_viec);

CREATE TABLE khung_gio_kham
(
    id_khung_gio     BIGINT      NOT NULL AUTO_INCREMENT,
    id_lich_lam_viec BIGINT      NOT NULL,
    gio_bat_dau      DATETIME(6) NOT NULL,
    gio_ket_thuc     DATETIME(6) NOT NULL,
    trang_thai       VARCHAR(20) NOT NULL DEFAULT 'CON_TRONG' COMMENT 'CON_TRONG | DA_DAT | DA_HUY',
    version          BIGINT      NOT NULL DEFAULT 0 COMMENT 'optimistic locking',
    CONSTRAINT pk_khung_gio_kham PRIMARY KEY (id_khung_gio),
    CONSTRAINT uk_khung_gio_kham_ca_gio_bat_dau UNIQUE (id_lich_lam_viec, gio_bat_dau),
    CONSTRAINT fk_khung_gio_kham_lich_lam_viec FOREIGN KEY (id_lich_lam_viec) REFERENCES lich_lam_viec (id_lich_lam_viec),
    CONSTRAINT ck_khung_gio_kham_gio CHECK (gio_ket_thuc > gio_bat_dau)
);
CREATE INDEX idx_khung_gio_kham_trang_thai_gio ON khung_gio_kham (trang_thai, gio_bat_dau);

CREATE TABLE yeu_cau_doi_lich
(
    id_yeu_cau                 BIGINT      NOT NULL AUTO_INCREMENT,
    id_bac_si                  BIGINT      NOT NULL,
    id_lich_lam_viec           BIGINT      NOT NULL,
    id_admin_xu_ly             BIGINT COMMENT 'NULL khi còn CHO_DUYET',
    loai_yeu_cau               VARCHAR(20) NOT NULL COMMENT 'DOI_CA | XIN_NGHI',
    ly_do                      TEXT        NOT NULL,
    ngay_mong_muon             DATE COMMENT 'bắt buộc với DOI_CA',
    gio_bat_dau_mong_muon      TIME,
    gio_ket_thuc_mong_muon     TIME,
    id_phong_kham_mong_muon    BIGINT COMMENT 'NULL = giữ phòng hiện tại',
    trang_thai                 VARCHAR(20) NOT NULL DEFAULT 'CHO_DUYET' COMMENT 'CHO_DUYET | DA_DUYET | TU_CHOI',
    ghi_chu_xu_ly              TEXT COMMENT 'bắt buộc khi từ chối (SCHED-03)',
    ngay_gui                   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ngay_xu_ly                 DATETIME(6),
    -- Chỉ có giá trị khi yêu cầu còn CHO_DUYET -> UNIQUE bên dưới: mỗi ca tối đa 1 yêu cầu chờ duyệt (quy tắc #15).
    id_lich_lam_viec_cho_duyet BIGINT GENERATED ALWAYS AS (
        CASE WHEN trang_thai = 'CHO_DUYET' THEN id_lich_lam_viec END
        ) STORED,
    CONSTRAINT pk_yeu_cau_doi_lich PRIMARY KEY (id_yeu_cau),
    CONSTRAINT uk_yeu_cau_doi_lich_cho_duyet UNIQUE (id_lich_lam_viec_cho_duyet),
    CONSTRAINT fk_yeu_cau_doi_lich_bac_si FOREIGN KEY (id_bac_si) REFERENCES bac_si (id_bac_si),
    CONSTRAINT fk_yeu_cau_doi_lich_lich_lam_viec FOREIGN KEY (id_lich_lam_viec) REFERENCES lich_lam_viec (id_lich_lam_viec),
    CONSTRAINT fk_yeu_cau_doi_lich_admin FOREIGN KEY (id_admin_xu_ly) REFERENCES quan_tri_vien (id_quan_tri_vien),
    CONSTRAINT fk_yeu_cau_doi_lich_phong_kham FOREIGN KEY (id_phong_kham_mong_muon) REFERENCES phong_kham (id_phong_kham),
    CONSTRAINT ck_yeu_cau_doi_lich_doi_ca CHECK (loai_yeu_cau <> 'DOI_CA' OR (ngay_mong_muon IS NOT NULL
        AND gio_bat_dau_mong_muon IS NOT NULL AND gio_ket_thuc_mong_muon IS NOT NULL)),
    CONSTRAINT ck_yeu_cau_doi_lich_gio CHECK (gio_ket_thuc_mong_muon IS NULL OR gio_bat_dau_mong_muon IS NULL
        OR gio_ket_thuc_mong_muon > gio_bat_dau_mong_muon)
);
CREATE INDEX idx_yeu_cau_doi_lich_trang_thai ON yeu_cau_doi_lich (trang_thai, ngay_gui);

-- ---------- booking-service: lịch hẹn ----------
CREATE TABLE lich_hen
(
    id_lich_hen           BIGINT       NOT NULL AUTO_INCREMENT,
    id_ho_so_benh_nhan    BIGINT       NOT NULL,
    id_bac_si             BIGINT       NOT NULL,
    id_khung_gio          BIGINT       NOT NULL,
    id_phong_kham         BIGINT       NOT NULL COMMENT 'denormalized từ khung_gio -> lich_lam_viec -> phong_kham',
    so_thu_tu             INT          NOT NULL COMMENT 'theo phòng khám + ngày (quy tắc #4)',
    trang_thai            VARCHAR(30)  NOT NULL DEFAULT 'CHO_XAC_NHAN'
        COMMENT 'CHO_XAC_NHAN | DA_XAC_NHAN | BI_TU_CHOI | DA_HOAN_THANH | DA_HUY | DA_HUY_DO_DOI_LICH',
    ly_do_kham            VARCHAR(500),
    ghi_chu               TEXT,
    ma_token_phieu_kham   VARCHAR(64)  NOT NULL COMMENT 'token ngẫu nhiên cho link/QR phiếu khám',
    ly_do_huy             VARCHAR(500) COMMENT 'lý do hủy / từ chối',
    can_doi_lich          BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'ca làm việc bị đổi/hủy -> cần dời/hủy lịch hẹn',
    id_lich_hen_cu        BIGINT COMMENT 'lịch hẹn cũ nếu lịch này được tạo do đổi lịch',
    version               BIGINT       NOT NULL DEFAULT 0 COMMENT 'optimistic locking',
    ngay_tao              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ngay_cap_nhat         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    -- Chỉ có giá trị khi lịch hẹn còn "chiếm" khung giờ -> UNIQUE bên dưới chống đặt trùng ở mức DB.
    -- Lịch đã hủy/từ chối/đổi lịch có giá trị NULL nên khung giờ được đặt lại bình thường.
    -- Phải khớp với TrangThaiLichHen.chiemKhungGio().
    id_khung_gio_hieu_luc BIGINT GENERATED ALWAYS AS (
        CASE WHEN trang_thai IN ('CHO_XAC_NHAN', 'DA_XAC_NHAN', 'DA_HOAN_THANH') THEN id_khung_gio END
        ) STORED,
    CONSTRAINT pk_lich_hen PRIMARY KEY (id_lich_hen),
    CONSTRAINT uk_lich_hen_token_phieu_kham UNIQUE (ma_token_phieu_kham),
    CONSTRAINT uk_lich_hen_khung_gio_hieu_luc UNIQUE (id_khung_gio_hieu_luc),
    CONSTRAINT fk_lich_hen_ho_so_benh_nhan FOREIGN KEY (id_ho_so_benh_nhan) REFERENCES ho_so_benh_nhan (id_ho_so_benh_nhan),
    CONSTRAINT fk_lich_hen_bac_si FOREIGN KEY (id_bac_si) REFERENCES bac_si (id_bac_si),
    CONSTRAINT fk_lich_hen_khung_gio FOREIGN KEY (id_khung_gio) REFERENCES khung_gio_kham (id_khung_gio),
    CONSTRAINT fk_lich_hen_phong_kham FOREIGN KEY (id_phong_kham) REFERENCES phong_kham (id_phong_kham),
    CONSTRAINT fk_lich_hen_lich_hen_cu FOREIGN KEY (id_lich_hen_cu) REFERENCES lich_hen (id_lich_hen),
    CONSTRAINT ck_lich_hen_so_thu_tu CHECK (so_thu_tu > 0)
);
CREATE INDEX idx_lich_hen_ho_so_trang_thai ON lich_hen (id_ho_so_benh_nhan, trang_thai);
CREATE INDEX idx_lich_hen_bac_si_trang_thai ON lich_hen (id_bac_si, trang_thai);
CREATE INDEX idx_lich_hen_khung_gio ON lich_hen (id_khung_gio);

-- ---------- medical-service ----------
CREATE TABLE ho_so_benh_an
(
    id_ho_so_benh_an      BIGINT      NOT NULL AUTO_INCREMENT,
    id_lich_hen           BIGINT      NOT NULL,
    id_ho_so_benh_nhan    BIGINT      NOT NULL,
    id_bac_si             BIGINT      NOT NULL,
    chan_doan             TEXT,
    ghi_chu               TEXT,
    ngay_tai_kham_de_xuat DATE COMMENT 'gợi ý tái khám, KHÔNG tự tạo lịch hẹn',
    ma_token_so_kham      VARCHAR(64) NOT NULL COMMENT 'token ngẫu nhiên cho link/QR sổ khám bệnh',
    ngay_tao              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_ho_so_benh_an PRIMARY KEY (id_ho_so_benh_an),
    CONSTRAINT uk_ho_so_benh_an_lich_hen UNIQUE (id_lich_hen),
    CONSTRAINT uk_ho_so_benh_an_token_so_kham UNIQUE (ma_token_so_kham),
    CONSTRAINT fk_ho_so_benh_an_lich_hen FOREIGN KEY (id_lich_hen) REFERENCES lich_hen (id_lich_hen),
    CONSTRAINT fk_ho_so_benh_an_ho_so_benh_nhan FOREIGN KEY (id_ho_so_benh_nhan) REFERENCES ho_so_benh_nhan (id_ho_so_benh_nhan),
    CONSTRAINT fk_ho_so_benh_an_bac_si FOREIGN KEY (id_bac_si) REFERENCES bac_si (id_bac_si)
);
CREATE INDEX idx_ho_so_benh_an_ho_so_benh_nhan ON ho_so_benh_an (id_ho_so_benh_nhan);

CREATE TABLE thuoc
(
    id_thuoc      BIGINT       NOT NULL AUTO_INCREMENT,
    ten_thuoc     VARCHAR(255) NOT NULL COMMENT 'tên hiển thị như người nhập',
    ten_chuan_hoa VARCHAR(255) NOT NULL COMMENT 'lowercase + trim + gộp khoảng trắng, dùng cho get-or-create',
    don_vi        VARCHAR(30) COMMENT 'viên, ml, gói...',
    mo_ta         TEXT,
    da_xac_minh   BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'false = bác sĩ tự thêm; true = admin đã duyệt',
    id_bac_si_tao BIGINT,
    CONSTRAINT pk_thuoc PRIMARY KEY (id_thuoc),
    CONSTRAINT uk_thuoc_ten_chuan_hoa UNIQUE (ten_chuan_hoa),
    CONSTRAINT fk_thuoc_bac_si_tao FOREIGN KEY (id_bac_si_tao) REFERENCES bac_si (id_bac_si)
);

CREATE TABLE chi_tiet_don_thuoc
(
    id_chi_tiet_don  BIGINT NOT NULL AUTO_INCREMENT,
    id_ho_so_benh_an BIGINT NOT NULL,
    id_thuoc         BIGINT NOT NULL,
    lieu_dung        VARCHAR(100) COMMENT 'vd: 500mg/lần',
    so_lan_moi_ngay  INT,
    so_ngay_dung     INT,
    ghi_chu_su_dung  VARCHAR(255) COMMENT 'vd: uống sau ăn',
    CONSTRAINT pk_chi_tiet_don_thuoc PRIMARY KEY (id_chi_tiet_don),
    CONSTRAINT fk_chi_tiet_don_thuoc_ho_so_benh_an FOREIGN KEY (id_ho_so_benh_an) REFERENCES ho_so_benh_an (id_ho_so_benh_an) ON DELETE CASCADE,
    CONSTRAINT fk_chi_tiet_don_thuoc_thuoc FOREIGN KEY (id_thuoc) REFERENCES thuoc (id_thuoc),
    CONSTRAINT ck_chi_tiet_don_thuoc_so_lan CHECK (so_lan_moi_ngay IS NULL OR so_lan_moi_ngay > 0),
    CONSTRAINT ck_chi_tiet_don_thuoc_so_ngay CHECK (so_ngay_dung IS NULL OR so_ngay_dung > 0)
);
CREATE INDEX idx_chi_tiet_don_thuoc_ho_so_benh_an ON chi_tiet_don_thuoc (id_ho_so_benh_an);

-- ---------- notification-service ----------
CREATE TABLE thong_bao
(
    id_thong_bao BIGINT      NOT NULL AUTO_INCREMENT,
    id_tai_khoan BIGINT      NOT NULL,
    id_lich_hen  BIGINT,
    id_yeu_cau   BIGINT COMMENT 'yêu cầu đổi lịch liên quan (thông báo cho Admin/Bác sĩ)',
    noi_dung     TEXT        NOT NULL,
    loai         VARCHAR(30) NOT NULL COMMENT 'xem enum LoaiThongBao (các sự kiện NOTI-01)',
    da_doc       BOOLEAN     NOT NULL DEFAULT FALSE,
    ngay_tao     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_thong_bao PRIMARY KEY (id_thong_bao),
    CONSTRAINT fk_thong_bao_tai_khoan FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan (id_tai_khoan),
    CONSTRAINT fk_thong_bao_lich_hen FOREIGN KEY (id_lich_hen) REFERENCES lich_hen (id_lich_hen),
    CONSTRAINT fk_thong_bao_yeu_cau FOREIGN KEY (id_yeu_cau) REFERENCES yeu_cau_doi_lich (id_yeu_cau)
);
CREATE INDEX idx_thong_bao_tai_khoan_da_doc ON thong_bao (id_tai_khoan, da_doc, ngay_tao);

-- ---------- chatbot-service ----------
CREATE TABLE phien_chat
(
    id_phien_chat      BIGINT      NOT NULL AUTO_INCREMENT,
    id_tai_khoan       BIGINT COMMENT 'NULL nếu là Khách (không đăng nhập)',
    ma_dinh_danh_khach VARCHAR(64) COMMENT 'định danh phía client cho Khách',
    thoi_gian_bat_dau  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    thoi_gian_ket_thuc DATETIME(6),
    trang_thai         VARCHAR(20) NOT NULL DEFAULT 'DANG_MO' COMMENT 'DANG_MO | DA_DONG',
    CONSTRAINT pk_phien_chat PRIMARY KEY (id_phien_chat),
    CONSTRAINT fk_phien_chat_tai_khoan FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan (id_tai_khoan)
);
CREATE INDEX idx_phien_chat_tai_khoan ON phien_chat (id_tai_khoan);
CREATE INDEX idx_phien_chat_ma_dinh_danh_khach ON phien_chat (ma_dinh_danh_khach);

CREATE TABLE tin_nhan_chat
(
    id_tin_nhan   BIGINT      NOT NULL AUTO_INCREMENT,
    id_phien_chat BIGINT      NOT NULL,
    nguoi_gui     VARCHAR(20) NOT NULL COMMENT 'NGUOI_DUNG | CHATBOT',
    noi_dung      TEXT        NOT NULL,
    ngay_tao      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_tin_nhan_chat PRIMARY KEY (id_tin_nhan),
    CONSTRAINT fk_tin_nhan_chat_phien_chat FOREIGN KEY (id_phien_chat) REFERENCES phien_chat (id_phien_chat) ON DELETE CASCADE
);
CREATE INDEX idx_tin_nhan_chat_phien_ngay_tao ON tin_nhan_chat (id_phien_chat, ngay_tao);
