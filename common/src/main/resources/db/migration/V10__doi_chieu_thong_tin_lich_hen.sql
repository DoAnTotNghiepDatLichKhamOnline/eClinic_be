-- =====================================================================
-- CCCD là khoá nhận diện duy nhất khi đặt lịch (DOANTOTNGH-6, phase 1)
--  * Số CCCD đã có hồ sơ thì lần đặt sau không còn bị từ chối vì họ tên / ngày sinh nhập khác hồ sơ. Hồ sơ không bị
--    ghi đè; lịch hẹn giữ bản sao những gì người đặt nhập (*_da_nhap) để phiếu khám và "lịch hẹn của tôi" hiện đúng
--    thứ người đặt đã nhập, không lộ dữ liệu đang lưu trong hồ sơ.
--  * can_doi_chieu: thông tin nhập khác hồ sơ (hoặc khác người giám hộ đã khai), phòng khám cần đối chiếu giấy tờ khi
--    bệnh nhân đến khám.
--  * Lịch hẹn tạo trước V10: các cột *_da_nhap để NULL, nơi hiển thị dùng lại dữ liệu của hồ sơ.
-- =====================================================================
ALTER TABLE lich_hen
    ADD COLUMN ho_ten_da_nhap         VARCHAR(150) NULL COMMENT 'Họ tên người khám do người đặt nhập' AFTER email_lien_he,
    ADD COLUMN ngay_sinh_da_nhap      DATE         NULL COMMENT 'Ngày sinh người khám do người đặt nhập' AFTER ho_ten_da_nhap,
    ADD COLUMN gioi_tinh_da_nhap      VARCHAR(10)  NULL COMMENT 'Giới tính người khám do người đặt nhập' AFTER ngay_sinh_da_nhap,
    ADD COLUMN ho_ten_giam_ho_da_nhap VARCHAR(150) NULL COMMENT 'Họ tên người giám hộ do người đặt nhập' AFTER gioi_tinh_da_nhap,
    ADD COLUMN can_doi_chieu          BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'Thông tin nhập khác hồ sơ, cần đối chiếu giấy tờ' AFTER ho_ten_giam_ho_da_nhap;
