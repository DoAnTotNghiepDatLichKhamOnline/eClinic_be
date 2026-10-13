-- =====================================================================
-- Nhắc tái khám (DOANTOTNGH-9, phase 6)
--  * medical-service chỉ ghi ho_so_benh_an.ngay_tai_kham_de_xuat như cũ. booking-service có 1 job so ngày tái khám hiện
--    tại của từng hồ sơ bệnh án với ngày ĐÃ BÁO cho bệnh nhân (bảng này, mỗi hồ sơ bệnh án 1 dòng):
--      - ngày mới / ngày khác ngay_da_bao  -> báo "bác sĩ hẹn tái khám" / "đổi ngày tái khám", ghi lại ngay_da_bao
--      - còn N ngày tới ngay_da_bao và da_nhac_truoc = 0 -> nhắc 1 lần, đặt da_nhac_truoc = 1
--      - bác sĩ xoá ngày -> ngay_da_bao = NULL, không gửi gì
--    Dòng này được ghi TRONG CÙNG transaction với thông báo / email chờ gửi (su_kien_thong_bao).
--  * Không bao giờ tạo lịch hẹn: chỉ báo và nhắc.
--  * Hồ sơ bệnh án đã có ngày tái khám từ trước: ghi sẵn là đã báo, để lúc triển khai không gửi lại các lời hẹn cũ;
--    ngày còn ở tương lai vẫn được nhắc trước như thường.
-- =====================================================================
CREATE TABLE nhac_tai_kham
(
    id_nhac          BIGINT      NOT NULL AUTO_INCREMENT,
    id_ho_so_benh_an BIGINT      NOT NULL,
    ngay_da_bao      DATE COMMENT 'ngày tái khám đã báo cho bệnh nhân; NULL = bác sĩ đã xoá ngày',
    da_nhac_truoc    BOOLEAN     NOT NULL DEFAULT FALSE COMMENT 'lần nhắc trước ngay_da_bao đã được xử lý (đã nhắc hoặc bỏ qua)',
    ngay_tao         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_nhac_tai_kham PRIMARY KEY (id_nhac),
    CONSTRAINT uk_nhac_tai_kham_ho_so_benh_an UNIQUE (id_ho_so_benh_an),
    CONSTRAINT fk_nhac_tai_kham_ho_so_benh_an FOREIGN KEY (id_ho_so_benh_an) REFERENCES ho_so_benh_an (id_ho_so_benh_an) ON DELETE CASCADE
);

INSERT INTO nhac_tai_kham (id_ho_so_benh_an, ngay_da_bao, da_nhac_truoc)
SELECT id_ho_so_benh_an, ngay_tai_kham_de_xuat, FALSE
FROM ho_so_benh_an
WHERE ngay_tai_kham_de_xuat IS NOT NULL;
