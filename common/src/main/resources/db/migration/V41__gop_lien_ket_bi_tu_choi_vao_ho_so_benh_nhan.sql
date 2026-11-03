-- =====================================================================
-- Gộp bảng lien_ket_ho_so_bi_tu_choi (V9) vào ho_so_benh_nhan (DOANTOTNGH-19, phase 2)
--  * tu_choi_lien_ket: mảng JSON các lần quản trị viên từ chối gắn hồ sơ này vào 1 tài khoản,
--    [{"idTaiKhoan": .., "lyDo": .., "ngayTao": ..}]; NULL = chưa từ chối lần nào. Tài khoản có trong mảng thì hệ thống
--    không tự đưa cặp (tài khoản, hồ sơ) đó vào hàng chờ xác minh nữa.
--  * 1 tài khoản có thể bị từ chối với nhiều hồ sơ (form hồ sơ của tôi tìm theo CCCD nhập trong form, không theo
--    cccd_dang_ky) nên giữ đủ mọi lần, không chỉ lần gần nhất.
--  * Chỉ đọc / ghi bằng câu SQL riêng (JSON_CONTAINS / JSON_ARRAY_APPEND), không qua entity HoSoBenhNhan.
-- =====================================================================
ALTER TABLE ho_so_benh_nhan
    ADD COLUMN tu_choi_lien_ket JSON NULL COMMENT 'mảng các lần từ chối liên kết: idTaiKhoan, lyDo, ngayTao' AFTER trang_thai_lien_ket;

UPDATE ho_so_benh_nhan h
    JOIN (SELECT id_ho_so_benh_nhan,
                 JSON_ARRAYAGG(JSON_OBJECT('idTaiKhoan', id_tai_khoan, 'lyDo', ly_do, 'ngayTao', ngay_tao)) AS ds
          FROM lien_ket_ho_so_bi_tu_choi
          GROUP BY id_ho_so_benh_nhan) t ON t.id_ho_so_benh_nhan = h.id_ho_so_benh_nhan
SET h.tu_choi_lien_ket = t.ds;

DROP TABLE lien_ket_ho_so_bi_tu_choi;
