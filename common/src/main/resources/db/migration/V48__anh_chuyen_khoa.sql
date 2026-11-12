-- =====================================================================
-- Ảnh minh hoạ của chuyên khoa (DOANTOTNGH-27, phase 4)
--  * chuyen_khoa.anh_minh_hoa: URL ảnh hiển thị ở thẻ / trang chuyên khoa; NULL = chưa có ảnh (frontend dùng biểu tượng).
--  * Gán cho 30 chuyên khoa mẫu đã có (DuLieuMau, DuLieuMauLon, khớp tên) ảnh mẫu dùng chung trên Cloudinary
--    (eclinic/mau/chuyen-khoa/, danh sách ở scripts/anh-mau/anh-mau.json). Chuyên khoa khác giữ NULL. DB mới: bảng còn
--    rỗng lúc migration chạy, dữ liệu mẫu tự gán ảnh khi tạo chuyên khoa.
-- =====================================================================
ALTER TABLE chuyen_khoa
    ADD COLUMN anh_minh_hoa VARCHAR(500) NULL COMMENT 'URL ảnh minh hoạ' AFTER mo_ta_en;

UPDATE chuyen_khoa
SET anh_minh_hoa = CASE ten_chuyen_khoa
    WHEN 'Nội tổng quát'          THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322245/eclinic/mau/chuyen-khoa/noi-tong-quat.jpg'
    WHEN 'Nhi khoa'               THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322248/eclinic/mau/chuyen-khoa/nhi-khoa.jpg'
    WHEN 'Da liễu'                THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322250/eclinic/mau/chuyen-khoa/da-lieu.jpg'
    WHEN 'Tai Mũi Họng'           THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322252/eclinic/mau/chuyen-khoa/tai-mui-hong.jpg'
    WHEN 'Tim mạch'               THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322255/eclinic/mau/chuyen-khoa/tim-mach.jpg'
    WHEN 'Sản phụ khoa'           THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322256/eclinic/mau/chuyen-khoa/san-phu-khoa.jpg'
    WHEN 'Mắt'                    THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322259/eclinic/mau/chuyen-khoa/mat.jpg'
    WHEN 'Răng Hàm Mặt'           THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322260/eclinic/mau/chuyen-khoa/rang-ham-mat.jpg'
    WHEN 'Thần kinh'              THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322262/eclinic/mau/chuyen-khoa/than-kinh.jpg'
    WHEN 'Tiêu hoá'               THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322265/eclinic/mau/chuyen-khoa/tieu-hoa.jpg'
    WHEN 'Hô hấp'                 THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322268/eclinic/mau/chuyen-khoa/ho-hap.jpg'
    WHEN 'Nội tiết'               THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322271/eclinic/mau/chuyen-khoa/noi-tiet.jpg'
    WHEN 'Cơ xương khớp'          THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322273/eclinic/mau/chuyen-khoa/co-xuong-khop.jpg'
    WHEN 'Thận - Tiết niệu'       THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322276/eclinic/mau/chuyen-khoa/than-tiet-nieu.jpg'
    WHEN 'Ung bướu'               THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322280/eclinic/mau/chuyen-khoa/ung-buou.jpg'
    WHEN 'Huyết học'              THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322284/eclinic/mau/chuyen-khoa/huyet-hoc.jpg'
    WHEN 'Truyền nhiễm'           THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322285/eclinic/mau/chuyen-khoa/truyen-nhiem.jpg'
    WHEN 'Tâm thần'               THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322286/eclinic/mau/chuyen-khoa/tam-than.jpg'
    WHEN 'Y học cổ truyền'        THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322287/eclinic/mau/chuyen-khoa/y-hoc-co-truyen.jpg'
    WHEN 'Phục hồi chức năng'     THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322289/eclinic/mau/chuyen-khoa/phuc-hoi-chuc-nang.jpg'
    WHEN 'Dinh dưỡng'             THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322292/eclinic/mau/chuyen-khoa/dinh-duong.jpg'
    WHEN 'Chấn thương chỉnh hình' THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322293/eclinic/mau/chuyen-khoa/chan-thuong-chinh-hinh.jpg'
    WHEN 'Ngoại tổng quát'        THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322295/eclinic/mau/chuyen-khoa/ngoai-tong-quat.jpg'
    WHEN 'Ngoại thần kinh'        THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322297/eclinic/mau/chuyen-khoa/ngoai-than-kinh.jpg'
    WHEN 'Lão khoa'               THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322300/eclinic/mau/chuyen-khoa/lao-khoa.jpg'
    WHEN 'Dị ứng - Miễn dịch'     THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322303/eclinic/mau/chuyen-khoa/di-ung-mien-dich.jpg'
    WHEN 'Nam khoa'               THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322304/eclinic/mau/chuyen-khoa/nam-khoa.jpg'
    WHEN 'Sơ sinh'                THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322308/eclinic/mau/chuyen-khoa/so-sinh.jpg'
    WHEN 'Phẫu thuật tạo hình'    THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322310/eclinic/mau/chuyen-khoa/phau-thuat-tao-hinh.jpg'
    WHEN 'Y học thể thao'         THEN 'https://res.cloudinary.com/deied72qz/image/upload/v1794322311/eclinic/mau/chuyen-khoa/y-hoc-the-thao.jpg'
    END
WHERE anh_minh_hoa IS NULL
  AND ten_chuyen_khoa IN (
      'Nội tổng quát',
      'Nhi khoa',
      'Da liễu',
      'Tai Mũi Họng',
      'Tim mạch',
      'Sản phụ khoa',
      'Mắt',
      'Răng Hàm Mặt',
      'Thần kinh',
      'Tiêu hoá',
      'Hô hấp',
      'Nội tiết',
      'Cơ xương khớp',
      'Thận - Tiết niệu',
      'Ung bướu',
      'Huyết học',
      'Truyền nhiễm',
      'Tâm thần',
      'Y học cổ truyền',
      'Phục hồi chức năng',
      'Dinh dưỡng',
      'Chấn thương chỉnh hình',
      'Ngoại tổng quát',
      'Ngoại thần kinh',
      'Lão khoa',
      'Dị ứng - Miễn dịch',
      'Nam khoa',
      'Sơ sinh',
      'Phẫu thuật tạo hình',
      'Y học thể thao');
