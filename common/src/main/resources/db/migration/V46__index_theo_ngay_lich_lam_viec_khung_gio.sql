-- =====================================================================
-- Index theo ngày cho lịch làm việc và khung giờ (DOANTOTNGH-20, phase 3)
--  * 2 bảng này lớn thêm mỗi ngày (khoảng 33 ca, 700 khung giờ với 100 bác sĩ) mà các truy vấn theo khoảng ngày chưa có
--    index phù hợp: lịch toàn viện, tệp PDF / Excel, báo cáo thống kê. Index cũ đều bắt đầu bằng bác sĩ, phòng khám, trạng thái.
--  * Đã đo trên dữ liệu mẫu lớn: thời gian chưa đổi, vì 14 ngày tới chiếm khoảng 70% số ca nên planner vẫn quét bảng. Index
--    có tác dụng khi khoảng ngày chọn lọc (1 ngày: 41 dòng thay vì 656) và rõ dần khi lịch sử dày lên.
-- =====================================================================
CREATE INDEX idx_lich_lam_viec_ngay ON lich_lam_viec (ngay_lam_viec);
CREATE INDEX idx_khung_gio_kham_gio_bat_dau ON khung_gio_kham (gio_bat_dau);
