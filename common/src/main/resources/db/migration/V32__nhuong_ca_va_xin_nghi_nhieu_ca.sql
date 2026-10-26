-- =====================================================================
-- Nhường ca cho đồng nghiệp và xin nghỉ nhiều ca (DOANTOTNGH-13, phase 5)
--  * yeu_cau_doi_lich.loai_yeu_cau có thêm NHUONG_CA: bác sĩ nhường 1 ca của mình cho 1 bác sĩ cùng chuyên khoa
--    (id_bac_si_nhan). Yêu cầu bắt đầu ở CHO_DONG_Y; bác sĩ nhận đồng ý thì thành CHO_DUYET (quản trị viên mới thấy và
--    duyệt / từ chối), từ chối thì thành DONG_NGHIEP_TU_CHOI (ghi_chu_bac_si_nhan, ngay_bac_si_nhan_phan_hoi). Duyệt thì
--    ca (cùng các lượt khám, lịch hẹn) chuyển sang bác sĩ nhận.
--  * Mỗi ca tối đa 1 yêu cầu đang chờ: cột generated id_lich_lam_viec_cho_duyet nay xét cả CHO_DONG_Y. Bỏ index UNIQUE +
--    cột trước khi sửa cột trang_thai mà nó phụ thuộc, rồi tạo lại (cùng tên).
--  * ma_nhom: các yêu cầu xin nghỉ gửi cùng 1 lần (xin nghỉ nhiều ca) mang chung 1 UUID; quản trị viên duyệt / từ chối,
--    bác sĩ rút cả nhóm. Mỗi dòng vẫn là 1 yêu cầu bình thường.
--  * lich_hen.doi_bac_si: lịch hẹn của ca vừa được nhường, bác sĩ khám đã đổi. Bệnh nhân được hủy / đổi lịch tới đúng giờ
--    khám và lần đổi đó không tính vào giới hạn số lần đổi. Khác can_doi_lich (ca bị hủy, lịch hẹn không khám được).
--  * lich_su_ca_lam_viec.nguon: chú thích liệt kê đủ các nguồn (SAO_CHEP, NHAP_EXCEL thêm ở phase 2, 3).
-- =====================================================================
ALTER TABLE yeu_cau_doi_lich
    DROP INDEX uk_yeu_cau_doi_lich_cho_duyet,
    DROP COLUMN id_lich_lam_viec_cho_duyet;

ALTER TABLE yeu_cau_doi_lich
    ADD COLUMN id_bac_si_nhan BIGINT NULL COMMENT 'NHUONG_CA: bác sĩ nhận ca' AFTER id_lich_lam_viec,
    ADD COLUMN ghi_chu_bac_si_nhan TEXT NULL COMMENT 'NHUONG_CA: ghi chú của bác sĩ nhận khi từ chối' AFTER ghi_chu_xu_ly,
    ADD COLUMN ngay_bac_si_nhan_phan_hoi DATETIME(6) NULL COMMENT 'NHUONG_CA: lúc bác sĩ nhận đồng ý / từ chối'
        AFTER ngay_gui,
    ADD COLUMN ma_nhom VARCHAR(36) NULL COMMENT 'các yêu cầu xin nghỉ gửi cùng 1 lần mang chung mã' AFTER id_phong_kham_mong_muon,
    MODIFY COLUMN loai_yeu_cau VARCHAR(20) NOT NULL COMMENT 'DOI_CA | XIN_NGHI | NHUONG_CA',
    MODIFY COLUMN trang_thai VARCHAR(20) NOT NULL DEFAULT 'CHO_DUYET'
        COMMENT 'CHO_DONG_Y | CHO_DUYET | DA_DUYET | TU_CHOI | DA_RUT | DONG_NGHIEP_TU_CHOI',
    ADD CONSTRAINT fk_yeu_cau_doi_lich_bac_si_nhan FOREIGN KEY (id_bac_si_nhan) REFERENCES bac_si (id_bac_si),
    ADD CONSTRAINT ck_yeu_cau_doi_lich_nhuong_ca CHECK (loai_yeu_cau <> 'NHUONG_CA' OR id_bac_si_nhan IS NOT NULL);

CREATE INDEX idx_yeu_cau_doi_lich_ma_nhom ON yeu_cau_doi_lich (ma_nhom);

ALTER TABLE yeu_cau_doi_lich
    ADD COLUMN id_lich_lam_viec_cho_duyet BIGINT GENERATED ALWAYS AS (
        CASE WHEN trang_thai IN ('CHO_DONG_Y', 'CHO_DUYET') THEN id_lich_lam_viec END
        ) STORED COMMENT 'chỉ có giá trị khi yêu cầu còn chờ (đồng nghiệp hoặc quản trị viên)',
    ADD CONSTRAINT uk_yeu_cau_doi_lich_cho_duyet UNIQUE (id_lich_lam_viec_cho_duyet);

ALTER TABLE lich_hen
    ADD COLUMN doi_bac_si BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'ca được nhường cho bác sĩ khác -> bệnh nhân hủy / đổi tự do tới giờ khám' AFTER can_doi_lich;

ALTER TABLE lich_su_ca_lam_viec
    MODIFY COLUMN nguon VARCHAR(20) NOT NULL
        COMMENT 'QUAN_TRI | DUYET_YEU_CAU | NGUNG_CONG_TAC | SAO_CHEP | NHAP_EXCEL | DU_LIEU_MAU | DU_LIEU_CU';
