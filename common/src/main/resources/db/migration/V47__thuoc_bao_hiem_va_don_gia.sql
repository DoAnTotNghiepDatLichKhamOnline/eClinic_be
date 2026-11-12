-- =====================================================================
-- Thuốc có bảo hiểm y tế và đơn giá (DOANTOTNGH-26, phase 1)
--  * thuoc.co_bao_hiem: thuốc có trong danh mục thanh toán BHYT (Thông tư 20/2022/TT-BYT); vitamin / thực phẩm bổ sung = FALSE.
--  * thuoc.don_gia: giá 1 don_vi (viên, gói, chai...), VND nên không có phần thập phân.
--  * Gán cho mọi thuốc đã có: thuốc mẫu (DuLieuMau, DuLieuMauLon, khớp tên + hàm lượng chuẩn hoá) nhận giá và BHYT như
--    dữ liệu mẫu trong mã nguồn; thuốc khác (quản trị viên / bác sĩ thêm) nhận co_bao_hiem = FALSE và giá tạm 5000 để
--    quản trị viên sửa sau. DB mới: bảng còn rỗng lúc migration chạy, dữ liệu mẫu tự gán cùng giá khi thêm thuốc.
--  * Sau khi gán, don_gia bỏ DEFAULT: thêm thuốc phải có giá.
-- =====================================================================
ALTER TABLE thuoc
    ADD COLUMN co_bao_hiem BOOLEAN       NOT NULL DEFAULT FALSE COMMENT 'có trong danh mục thanh toán BHYT' AFTER trang_thai,
    ADD COLUMN don_gia     DECIMAL(12, 0) NOT NULL DEFAULT 5000 COMMENT 'giá 1 don_vi (VND)' AFTER co_bao_hiem;

CREATE TEMPORARY TABLE gia_thuoc_mau
(
    ten_chuan_hoa       VARCHAR(255)   NOT NULL,
    ham_luong_chuan_hoa VARCHAR(100)   NOT NULL,
    co_bao_hiem         BOOLEAN        NOT NULL,
    don_gia             DECIMAL(12, 0) NOT NULL,
    PRIMARY KEY (ten_chuan_hoa, ham_luong_chuan_hoa)
);

INSERT INTO gia_thuoc_mau (ten_chuan_hoa, ham_luong_chuan_hoa, co_bao_hiem, don_gia)
VALUES
    ('paracetamol', '500mg', TRUE, 500),
    ('hapacol 250', '250mg', TRUE, 2500),
    ('amoxicillin', '500mg', TRUE, 1200),
    ('cefuroxim', '500mg', TRUE, 6000),
    ('ibuprofen', '400mg', TRUE, 900),
    ('loratadin', '10mg', TRUE, 600),
    ('cetirizin', '10mg', TRUE, 700),
    ('omeprazol', '20mg', TRUE, 800),
    ('domperidon', '10mg', TRUE, 500),
    ('salbutamol', '100mcg/liều', TRUE, 75000),
    ('amlodipin', '5mg', TRUE, 600),
    ('metformin', '500mg', TRUE, 700),
    ('vitamin c', '500mg', FALSE, 400),
    ('oresol', '4,22g', TRUE, 2000),
    ('povidon iod', '10%', TRUE, 25000),
    ('cloramphenicol', '0,4%', TRUE, 4000),
    ('azithromycin', '100mg', TRUE, 3000),
    ('azithromycin', '250mg', TRUE, 5000),
    ('azithromycin', '500mg', TRUE, 9000),
    ('clarithromycin', '125mg', TRUE, 3000),
    ('clarithromycin', '250mg', TRUE, 5000),
    ('clarithromycin', '500mg', TRUE, 8500),
    ('ciprofloxacin', '250mg', TRUE, 1000),
    ('ciprofloxacin', '500mg', TRUE, 1500),
    ('ciprofloxacin', '750mg', TRUE, 2500),
    ('levofloxacin', '250mg', TRUE, 2500),
    ('levofloxacin', '500mg', TRUE, 4000),
    ('levofloxacin', '750mg', TRUE, 6500),
    ('cefixim', '100mg', TRUE, 2500),
    ('cefixim', '200mg', TRUE, 4000),
    ('cefixim', '400mg', TRUE, 7000),
    ('cefpodoxim', '50mg', TRUE, 2500),
    ('cefpodoxim', '100mg', TRUE, 4000),
    ('cefpodoxim', '200mg', TRUE, 7500),
    ('doxycyclin', '50mg', TRUE, 500),
    ('doxycyclin', '100mg', TRUE, 800),
    ('doxycyclin', '200mg', TRUE, 1500),
    ('metronidazol', '200mg', TRUE, 300),
    ('metronidazol', '250mg', TRUE, 400),
    ('metronidazol', '500mg', TRUE, 700),
    ('fluconazol', '50mg', TRUE, 3000),
    ('fluconazol', '100mg', TRUE, 5000),
    ('fluconazol', '150mg', TRUE, 7000),
    ('acyclovir', '200mg', TRUE, 800),
    ('acyclovir', '400mg', TRUE, 1400),
    ('acyclovir', '800mg', TRUE, 2500),
    ('naproxen', '250mg', TRUE, 1200),
    ('naproxen', '275mg', TRUE, 1300),
    ('naproxen', '500mg', TRUE, 2200),
    ('diclofenac', '25mg', TRUE, 300),
    ('diclofenac', '50mg', TRUE, 500),
    ('diclofenac', '75mg', TRUE, 800),
    ('celecoxib', '100mg', TRUE, 1500),
    ('celecoxib', '200mg', TRUE, 2500),
    ('celecoxib', '400mg', TRUE, 4500),
    ('esomeprazol', '10mg', TRUE, 2500),
    ('esomeprazol', '20mg', TRUE, 4000),
    ('esomeprazol', '40mg', TRUE, 6000),
    ('pantoprazol', '20mg', TRUE, 2000),
    ('pantoprazol', '40mg', TRUE, 3500),
    ('pantoprazol', '80mg', TRUE, 6000),
    ('famotidin', '10mg', TRUE, 400),
    ('famotidin', '20mg', TRUE, 600),
    ('famotidin', '40mg', TRUE, 1000),
    ('losartan', '25mg', TRUE, 1000),
    ('losartan', '50mg', TRUE, 1600),
    ('losartan', '100mg', TRUE, 2800),
    ('enalapril', '5mg', TRUE, 400),
    ('enalapril', '10mg', TRUE, 600),
    ('enalapril', '20mg', TRUE, 1000),
    ('bisoprolol', '2,5mg', TRUE, 1000),
    ('bisoprolol', '5mg', TRUE, 1500),
    ('bisoprolol', '10mg', TRUE, 2500),
    ('atorvastatin', '10mg', TRUE, 1000),
    ('atorvastatin', '20mg', TRUE, 1800),
    ('atorvastatin', '40mg', TRUE, 3000),
    ('rosuvastatin', '5mg', TRUE, 1500),
    ('rosuvastatin', '10mg', TRUE, 2500),
    ('rosuvastatin', '20mg', TRUE, 4000),
    ('simvastatin', '10mg', TRUE, 600),
    ('simvastatin', '20mg', TRUE, 1000),
    ('simvastatin', '40mg', TRUE, 1800),
    ('gliclazid', '30mg', TRUE, 1500),
    ('gliclazid', '60mg', TRUE, 2800),
    ('gliclazid', '80mg', TRUE, 900),
    ('levothyroxin', '25mcg', TRUE, 800),
    ('levothyroxin', '50mcg', TRUE, 1000),
    ('levothyroxin', '100mcg', TRUE, 1500),
    ('prednisolon', '5mg', TRUE, 200),
    ('prednisolon', '10mg', TRUE, 400),
    ('prednisolon', '20mg', TRUE, 700),
    ('fexofenadin', '60mg', TRUE, 1500),
    ('fexofenadin', '120mg', TRUE, 2500),
    ('fexofenadin', '180mg', TRUE, 3500),
    ('montelukast', '4mg', TRUE, 3000),
    ('montelukast', '5mg', TRUE, 3500),
    ('montelukast', '10mg', TRUE, 5000),
    ('vitamin d3', '400iu', FALSE, 1000),
    ('vitamin d3', '1000iu', FALSE, 1500),
    ('vitamin d3', '2000iu', FALSE, 2500);

UPDATE thuoc t
    JOIN gia_thuoc_mau g ON g.ten_chuan_hoa = t.ten_chuan_hoa AND g.ham_luong_chuan_hoa = t.ham_luong_chuan_hoa
SET t.co_bao_hiem = g.co_bao_hiem,
    t.don_gia     = g.don_gia;

DROP TEMPORARY TABLE gia_thuoc_mau;

ALTER TABLE thuoc
    ALTER COLUMN don_gia DROP DEFAULT;
