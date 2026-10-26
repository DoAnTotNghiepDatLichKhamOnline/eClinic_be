# Tài liệu API eClinic — Quản trị viên

Dành cho: frontend các màn hình của quản trị viên (vai trò `QUAN_TRI_VIEN`).

Mọi route ở mục 3–4 cần token của tài khoản quản trị viên. Phần quy ước chung và các route xác thực, tài khoản, thông báo giống hệt ở cả 3 tài liệu.

**Lịch sử phiên bản**

| Phiên bản | Nội dung |
|---|---|
| 1.0 — 21/10/2026 | Bản đầu: 27 route dùng chung (mục 2) và 81 route của quản trị viên (mục 3–4). |

## Mục lục

- [1. Quy ước chung](#1-quy-ước-chung)
- [2. Xác thực, tài khoản và thông báo (mọi vai trò)](#2-xác-thực-tài-khoản-và-thông-báo-mọi-vai-trò)
- [3. Quản trị đặt lịch và lịch làm việc](#3-quản-trị-đặt-lịch-và-lịch-làm-việc)
  - [3.1 Tổng quan](#31-tổng-quan)
  - [3.2 Mô hình và enum của mục 3](#32-mô-hình-và-enum-của-mục-3)
  - [3.3 Tổng quan và quy tắc đặt lịch](#33-tổng-quan-và-quy-tắc-đặt-lịch)
  - [3.4 Ngày nghỉ của bệnh viện](#34-ngày-nghỉ-của-bệnh-viện)
  - [3.5 Ca làm việc: xem, xếp, sửa, hủy](#35-ca-làm-việc-xem-xếp-sửa-hủy)
  - [3.6 Sao chép, Excel, PDF, gửi lịch](#36-sao-chép-excel-pdf-gửi-lịch)
  - [3.7 Yêu cầu đổi ca / xin nghỉ / nhường ca](#37-yêu-cầu-đổi-ca--xin-nghỉ--nhường-ca)
  - [3.8 Lịch hẹn toàn viện và đặt qua tổng đài](#38-lịch-hẹn-toàn-viện-và-đặt-qua-tổng-đài)
  - [3.9 Hồ sơ bệnh nhân và xác minh liên kết](#39-hồ-sơ-bệnh-nhân-và-xác-minh-liên-kết)
  - [3.10 Đánh giá của bệnh nhân](#310-đánh-giá-của-bệnh-nhân)
- [4. Quản trị danh mục, tài khoản, thuốc và thống kê](#4-quản-trị-danh-mục-tài-khoản-thuốc-và-thống-kê)
  - [4.1 Tổng quan](#41-tổng-quan)
  - [4.2 Mô hình và enum của mục 4](#42-mô-hình-và-enum-của-mục-4)
  - [4.3 Chuyên khoa](#43-chuyên-khoa)
  - [4.4 Phòng khám](#44-phòng-khám)
  - [4.5 Bác sĩ](#45-bác-sĩ)
  - [4.6 Ảnh giới thiệu của bác sĩ](#46-ảnh-giới-thiệu-của-bác-sĩ)
  - [4.7 Tài khoản](#47-tài-khoản)
  - [4.8 Danh mục thuốc](#48-danh-mục-thuốc)
  - [4.9 Lịch sử sửa hồ sơ bệnh án](#49-lịch-sử-sửa-hồ-sơ-bệnh-án)
  - [4.10 Báo cáo thống kê](#410-báo-cáo-thống-kê)

<!-- CHUNG:BAT-DAU -->
## 1. Quy ước chung

### 1.1 Địa chỉ và service

Mọi request đi qua gateway: **`http://localhost:8080`**. Gateway chuyển theo tiền tố đường dẫn, giữ nguyên đường dẫn:

| Tiền tố | Service | Swagger (chỉ để thử, tài liệu này là chuẩn) |
|---|---|---|
| `/api/auth/**`, `/api/users/**` | identity-service | http://localhost:8081/swagger-ui.html |
| `/api/catalog/**` | catalog-service | http://localhost:8082/swagger-ui.html |
| `/api/booking/**` | booking-service | http://localhost:8084/swagger-ui.html |
| `/api/medical/**` | medical-service | http://localhost:8085/swagger-ui.html |
| `/api/notification/**` | notification-service | http://localhost:8086/swagger-ui.html |
| `/api/chatbot/**` | chatbot-service | http://localhost:8087/swagger-ui.html |
| `/api/report/**` | report-service | http://localhost:8088/swagger-ui.html |

**Chạy thử và tài khoản mẫu**

Luôn cần: MySQL, Redis, api-gateway, identity-service (đăng nhập), cộng với service của tiền tố đang gọi. Chạy cả hệ thống:
`docker compose --profile app up -d --build`. Tài khoản quản trị được tạo khi identity-service khởi động lần đầu (đổi bằng
`ADMIN_EMAIL`, `ADMIN_PASSWORD`). Bác sĩ và bệnh nhân mẫu chỉ có khi `SEED_DATA=true` (mặc định của docker compose) và
CSDL chưa có bác sĩ nào; mật khẩu chung đổi bằng `SEED_PASSWORD`.

| Tài khoản | Mật khẩu | Vai trò / ghi chú |
|---|---|---|
| `admin@eclinic.local` | `Admin@123` | `QUAN_TRI_VIEN` |
| `bacsi01@eclinic.local` … `bacsi12@eclinic.local` | `Demo@123` | `BAC_SI`, đã kích hoạt, mỗi người 1 chuyên khoa |
| `benhnhan01@eclinic.local` … `benhnhan04@eclinic.local` | `Demo@123` | `BENH_NHAN`, đã kích hoạt, có hồ sơ bệnh nhân `DA_LIEN_KET` |
| `benhnhan05@eclinic.local` | `Demo@123` | `BENH_NHAN` chưa kích hoạt: đăng nhập trả 403 `TAI_KHOAN_CHUA_XAC_THUC` |
| `benhnhan06@eclinic.local` | `Demo@123` | `BENH_NHAN` bị vô hiệu hoá: đăng nhập trả 403 `TAI_KHOAN_BI_VO_HIEU_HOA` |

### 1.2 Cách đọc mỗi route

Mỗi route ghi phương thức và đường dẫn (biến đường dẫn viết đúng tên trong code, vd `{maPhieuKham}`), rồi:

- **Quyền:** `Công khai` (không cần token), `Đăng nhập` (mọi vai trò), hoặc vai trò `BENH_NHAN` / `BAC_SI` / `QUAN_TRI_VIEN`.
- **Request:** tham số đường dẫn (path), query, body JSON. Cột *Bắt buộc*: `có` / `không`. Giới hạn ghi ở *Ghi chú*.
- **Response:** nội dung của `duLieu` (không lặp lại phần bọc ngoài ở mục 1.3), bảng trường và 1 ví dụ JSON.
- **Lỗi:** chỉ các lỗi riêng của route. Các lỗi có thể đến từ mọi route nằm ở mục 1.4, không ghi lại.

### 1.3 Phần bọc phản hồi, phân trang, ngày giờ

Mọi phản hồi JSON đều bọc trong cùng 1 khung:

```json
{ "thanhCong": true, "thongDiep": "Đăng nhập thành công", "duLieu": { } }
```

```json
{ "thanhCong": false, "maLoi": "DU_LIEU_KHONG_HOP_LE", "thongDiep": "Dữ liệu không hợp lệ",
  "chiTiet": [ { "truong": "email", "thongDiep": "Email không đúng định dạng" } ] }
```

- Khung bọc bỏ các trường null: thành công thường không có `thongDiep` (chỉ thao tác ghi mới có), `duLieu` vắng mặt khi
  route không trả dữ liệu; lỗi chỉ có `chiTiet` khi lỗi kiểm tra theo trường (thiếu trường, sai định dạng).
- **Bên trong `duLieu`, trường null vẫn được ghi ra** (`"anhDaiDien": null`). Frontend không nên dựa vào việc trường vắng
  mặt.
- Hiện lỗi cho người dùng: dùng `thongDiep` (đã theo ngôn ngữ, mục 1.6); lỗi theo trường thì gắn `chiTiet[].thongDiep`
  vào ô `chiTiet[].truong`. Rẽ nhánh logic theo `maLoi`, không theo câu chữ.
- **Phân trang** (`TrangDuLieu`): query `trang` (từ 0) và `kichThuoc`; `duLieu` có dạng
  `{ "noiDung": [ ... ], "trang": 0, "kichThuoc": 20, "tongSoPhanTu": 57, "tongSoTrang": 3 }`.
- **Ngày giờ** theo giờ Việt Nam, không có múi giờ: ngày `"2026-10-21"`, giờ `"08:30:00"`, ngày giờ
  `"2026-10-21T08:30:00"` (có thể kèm phần lẻ giây, vd `"2026-10-21T08:30:00.123456"`). Gửi lên cùng định dạng.
- Mã định danh (`id`) là số, trừ khi ghi khác. Enum là chuỗi viết hoa (`"DA_XAC_NHAN"`).
- Tải tệp (Excel, PDF, `.ics`, ảnh QR) không bọc khung: trả thẳng tệp, tên tệp ở header `Content-Disposition` (gateway
  cho trình duyệt đọc header này).

### 1.4 Lỗi dùng chung

| HTTP | `maLoi` | Khi nào | Frontend nên |
|---|---|---|---|
| 400 | `DU_LIEU_KHONG_HOP_LE` | Body / query sai quy tắc, sai kiểu, thiếu tham số, JSON hỏng | Hiện lỗi theo trường (`chiTiet`) hoặc `thongDiep` |
| 401 | `CHUA_DANG_NHAP` | Không gửi token, token sai / hết hạn, tài khoản đã bị xoá | Làm mới phiên 1 lần (mục 1.5), lỗi tiếp thì về trang đăng nhập |
| 403 | `KHONG_CO_QUYEN` | Đã đăng nhập nhưng vai trò không được gọi route này, hoặc không phải dữ liệu của mình | Hiện thông báo, không thử lại |
| 403 | `TAI_KHOAN_BI_VO_HIEU_HOA` | Tài khoản bị vô hiệu hoá sau khi token được cấp | Đăng xuất, hiện thông báo |
| 404 | `KHONG_TIM_THAY` | Không có bản ghi với id / mã đó; đường dẫn không tồn tại | Hiện "không tìm thấy" |
| 405 | `PHUONG_THUC_KHONG_HO_TRO` | Sai phương thức HTTP | Lỗi lập trình |
| 409 | `XUNG_DOT_DU_LIEU` | Trùng dữ liệu duy nhất, hoặc ràng buộc dữ liệu bị vi phạm | Hiện `thongDiep` |
| 409 | `DU_LIEU_DA_THAY_DOI` | Người khác vừa sửa cùng bản ghi (`phienBan` cũ), hoặc hệ thống bận | Tải lại dữ liệu rồi thao tác lại |
| 413 | `TEP_QUA_LON` | Tệp tải lên vượt giới hạn của route | Hiện giới hạn trong `thongDiep` |
| 413 | `YEU_CAU_QUA_LON` | Body quá 5 MB (gateway chặn) | Như trên |
| 429 | `QUA_NHIEU_YEU_CAU` | 1 địa chỉ IP gửi quá nhiều request (gateway chặn), có header `Retry-After` (giây) | Chờ `Retry-After` giây rồi thử lại |
| 500 | `LOI_HE_THONG` | Lỗi không lường trước | Hiện thông báo chung |
| 502 | `LOI_KET_NOI_DICH_VU` | Gateway không nói chuyện được với service | Thử lại sau |
| 503 | `DICH_VU_KHONG_KHA_DUNG` | Service chưa chạy (gateway trả) | Thử lại sau |
| 503 | `DICH_VU_NOI_BO_LOI` | Service gọi service khác bị lỗi | Thử lại sau |
| 504 | `DICH_VU_PHAN_HOI_QUA_LAU` | Service trả lời quá 30 giây | Thử lại sau |

Giới hạn tần suất của gateway (theo IP, mỗi phút): 300 request nói chung; 20 lần `POST` tới `/api/auth/login`,
`/api/auth/google`, `/api/auth/register`, `/api/auth/forgot-password`, `/api/auth/resend-verification`; 30 lần `POST`
tới `/api/chatbot/**`. Một số route có giới hạn riêng (ghi ở route đó).

### 1.5 Đăng nhập, token và vai trò

- Đăng nhập (`POST /api/auth/login` hoặc `/api/auth/google`) trả `accessToken` trong body và đặt **refresh token** trong
  cookie `eclinic_rt` (HttpOnly, Secure, SameSite=Strict, Path=`/api/auth`, 7 ngày). JavaScript không đọc được cookie này.
- Gửi access token ở mọi route cần đăng nhập: header `Authorization: Bearer <accessToken>`. Token sống 30 phút
  (`thoiHanAccessToken`, giây). Chỉ giữ token trong bộ nhớ (biến, store), không lưu `localStorage`.
- Các request tới `/api/auth/*` phải gửi kèm cookie: `fetch(..., { credentials: 'include' })` /
  `axios` `withCredentials: true`. Frontend phải chạy ở 1 origin được phép (mặc định `http://localhost:5173`,
  `http://localhost:3000`; biến `CORS_ALLOWED_ORIGINS`).
- Nhận 401 `CHUA_DANG_NHAP` → gọi `POST /api/auth/refresh-token` 1 lần (không body), lấy `accessToken` mới rồi gửi lại
  request. Làm mới cũng lỗi 401 → về trang đăng nhập. Mở lại trang (F5): gọi refresh-token trước để khôi phục phiên.
  Nhiều request cùng gặp 401 thì chỉ làm mới 1 lần rồi dùng chung kết quả (refresh token đổi mỗi lần làm mới).
- Vai trò lấy ở `duLieu.taiKhoan.vaiTro` khi đăng nhập (hoặc `GET /api/users/me`): `BENH_NHAN`, `BAC_SI`,
  `QUAN_TRI_VIEN`. Chọn giao diện theo vai trò; quyền thật vẫn do backend kiểm tra (403 `KHONG_CO_QUYEN`).
- Tài khoản bác sĩ mới do quản trị viên tạo mang mật khẩu mặc định: lần đăng nhập đầu nhận 403 `PHAI_DOI_MAT_KHAU` → mở
  form đặt mật khẩu, gọi `POST /api/auth/first-password`, rồi đăng nhập lại bằng mật khẩu mới.
- Đăng nhập Google chỉ dành cho bệnh nhân.
- Liên kết trong email (kích hoạt, đặt lại mật khẩu, xác nhận đổi email) trỏ về trang của frontend kèm `token`; trang đó
  gọi route `POST` tương ứng với `token` (không tự đổi trạng thái khi chỉ mở trang).

### 1.6 Ngôn ngữ

- Header `Accept-Language`: giá trị bắt đầu bằng `en` → tiếng Anh; không gửi hoặc ngôn ngữ khác → tiếng Việt. Gửi ở mọi
  request (cả request của khách).
- Đổi theo ngôn ngữ: `thongDiep`, câu thông báo, và nội dung danh mục có 2 bản (tên chuyên khoa, phòng, hồ sơ bác sĩ, thuốc,
  tên mã ICD-10). `maLoi`, giá trị enum, tên trường không đổi.
- Các route quản trị danh mục trả cả bản tiếng Việt và trường `…En` (vd `ten`, `tenEn`) để sửa; bản tiếng Anh trống thì
  hiển thị bản tiếng Việt.
- Ngôn ngữ ưa dùng của tài khoản (`ngonNgu`: `VI` / `EN`, đổi bằng `PUT /api/users/me/language`) chỉ dùng cho email và
  nhắc lịch gửi sau này; không thay cho `Accept-Language`.

### 1.7 Enum dùng chung

| Enum | Giá trị |
|---|---|
| `VaiTro` | `BENH_NHAN`, `BAC_SI`, `QUAN_TRI_VIEN` |
| `TrangThaiTaiKhoan` | `CHO_XAC_NHAN` (đã đăng ký, chưa kích hoạt qua email), `DA_KICH_HOAT`, `VO_HIEU_HOA` (không đăng nhập được) |
| `NgonNgu` | `VI`, `EN` |
| `GioiTinh` | `NAM`, `NU`, `KHAC` |
| `TrangThaiLienKet` (hồ sơ bệnh nhân ↔ tài khoản) | `CHUA_LIEN_KET` (hồ sơ của khách), `CHO_XAC_MINH` (chờ quản trị viên xác minh, chưa thấy lượt khám cũ), `DA_LIEN_KET` |
| `TrangThaiBacSi` | `DANG_CONG_TAC`, `NGUNG_CONG_TAC` |
| `TrangThaiLichHen` | `DA_XAC_NHAN` (đã đặt, chờ khám), `DA_HOAN_THANH` (đã khám), `DA_HUY` (bệnh nhân / khách hủy), `DA_HUY_DO_DOI_LICH` (lịch cũ sau khi đổi sang lịch mới), `BI_TU_CHOI` (bác sĩ hủy, lý do ở `lyDoHuy`), `QUAN_TRI_HUY` (quản trị viên hủy), `KHONG_DEN` (ngày khám đã qua mà không khám) |

### 1.8 Mô hình dùng chung

**`TaiKhoanResponse`**

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | số | |
| `hoTen` | chuỗi | |
| `email` | chuỗi | |
| `soDienThoai` | chuỗi \| null | |
| `vaiTro` | `VaiTro` | |
| `trangThai` | `TrangThaiTaiKhoan` | |
| `ngonNgu` | `NgonNgu` | ngôn ngữ ưa dùng |

**`DangNhapResponse`**

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `accessToken` | chuỗi | JWT |
| `loaiToken` | chuỗi | luôn `"Bearer"` |
| `thoiHanAccessToken` | số | số giây token còn hiệu lực (1800) |
| `taiKhoan` | `TaiKhoanResponse` | |

**`HoSoCaNhanResponse`** (hồ sơ của người đang đăng nhập)

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id`, `hoTen`, `email`, `soDienThoai` | | như `TaiKhoanResponse` |
| `anhDaiDien` | chuỗi \| null | URL ảnh |
| `vaiTro`, `trangThai`, `ngonNgu` | enum | |
| `coMatKhau` | boolean | `false`: tài khoản chỉ đăng nhập Google (ẩn form đổi mật khẩu / đổi email, gợi ý "Quên mật khẩu" để đặt) |
| `lienKetGoogle` | boolean | |
| `ngayTao` | ngày giờ | |
| `bacSi` | object \| null | chỉ có ở bác sĩ: `{ id, idChuyenKhoa, tenChuyenKhoa, hocVi, soGiayPhep, soNamKinhNghiem, tieuSu, trangThai: TrangThaiBacSi }` |
| `hoSoBenhNhan` | object \| null | chỉ có ở bệnh nhân đã có hồ sơ: `{ id, trangThaiLienKet, cccd, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChi, soBaoHiemYTe, tienSuBenhLy }` (chỉ xem, sửa ở route hồ sơ bệnh nhân của booking) |

```json
{
  "id": 15, "hoTen": "Nguyễn Văn An", "email": "benhnhan01@eclinic.local", "soDienThoai": "0901000001",
  "anhDaiDien": null, "vaiTro": "BENH_NHAN", "trangThai": "DA_KICH_HOAT", "ngonNgu": "VI",
  "coMatKhau": true, "lienKetGoogle": false, "ngayTao": "2026-10-01T09:00:00",
  "bacSi": null,
  "hoSoBenhNhan": { "id": 3, "trangThaiLienKet": "DA_LIEN_KET", "cccd": "079200000001", "hoTen": "Nguyễn Văn An",
    "ngaySinh": "1995-04-12", "gioiTinh": "NAM", "soDienThoai": "0901000001", "diaChi": "Q.1, TP.HCM",
    "soBaoHiemYTe": null, "tienSuBenhLy": null }
}
```

**`ThongBaoResponse`**

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | số | |
| `loai` | `LoaiThongBao` | xem mục 2.3 |
| `noiDung` | chuỗi | câu đã soạn theo `Accept-Language` |
| `daDoc` | boolean | |
| `ngayTao` | ngày giờ | |
| `idLichHen` | số \| null | lịch hẹn liên quan (bác sĩ / quản trị viên mở lịch hẹn bằng id này) |
| `maPhieuKham` | chuỗi \| null | **chỉ trả cho bệnh nhân**: mở phiếu khám `GET /api/booking/phieu-kham/{maPhieuKham}` |
| `idYeuCau` | số \| null | yêu cầu đổi ca / xin nghỉ / nhường ca liên quan |

## 2. Xác thực, tài khoản và thông báo (mọi vai trò)

**Tất cả route của mục 2**

| Phương thức | Đường dẫn | Quyền | Mục đích |
|---|---|---|---|
| POST | `/api/auth/register` | Công khai | Đăng ký tài khoản bệnh nhân |
| POST | `/api/auth/verify-email` | Công khai | Kích hoạt tài khoản bằng token |
| POST | `/api/auth/resend-verification` | Công khai | Gửi lại liên kết kích hoạt |
| POST | `/api/auth/login` | Công khai | Đăng nhập bằng email và mật khẩu |
| POST | `/api/auth/google` | Công khai | Đăng nhập bằng Google |
| POST | `/api/auth/first-password` | Công khai | Đặt mật khẩu lần đầu (bác sĩ mới) |
| POST | `/api/auth/refresh-token` | Công khai | Làm mới phiên đăng nhập |
| POST | `/api/auth/logout` | Công khai | Đăng xuất thiết bị đang dùng |
| POST | `/api/auth/forgot-password` | Công khai | Quên mật khẩu: gửi liên kết đặt lại |
| POST | `/api/auth/reset-password` | Công khai | Đặt mật khẩu mới bằng token |
| POST | `/api/auth/confirm-email-change` | Công khai | Xác nhận đổi email bằng token |
| GET | `/api/users/me` | Đăng nhập | Xem hồ sơ của tôi |
| PUT | `/api/users/me` | Đăng nhập | Sửa họ tên, số điện thoại |
| PUT | `/api/users/me/language` | Đăng nhập | Lưu ngôn ngữ của tài khoản |
| PUT | `/api/users/me/change-password` | Đăng nhập | Đổi mật khẩu |
| POST | `/api/users/me/avatar` | Đăng nhập | Đổi ảnh đại diện |
| DELETE | `/api/users/me/avatar` | Đăng nhập | Bỏ ảnh đại diện |
| POST | `/api/users/me/change-email` | Đăng nhập | Yêu cầu đổi email |
| GET | `/api/users/me/change-email` | Đăng nhập | Xem yêu cầu đổi email đang chờ |
| DELETE | `/api/users/me/change-email` | Đăng nhập | Huỷ yêu cầu đổi email |
| GET | `/api/users/me/sessions` | Đăng nhập | Thiết bị đang đăng nhập |
| DELETE | `/api/users/me/sessions/{id}` | Đăng nhập | Đăng xuất 1 thiết bị khác |
| DELETE | `/api/users/me/sessions` | Đăng nhập | Đăng xuất mọi thiết bị khác |
| GET | `/api/notification/thong-bao` | Đăng nhập | Thông báo của tôi |
| GET | `/api/notification/thong-bao/so-chua-doc` | Đăng nhập | Số thông báo chưa đọc |
| POST | `/api/notification/thong-bao/{id}/da-doc` | Đăng nhập | Đánh dấu 1 thông báo đã đọc |
| POST | `/api/notification/thong-bao/da-doc-tat-ca` | Đăng nhập | Đánh dấu mọi thông báo đã đọc |

### 2.1 Xác thực

Mọi route ở đây là **công khai**. Mật khẩu (`matKhau`, `matKhauMoi`): từ 6 ký tự, tối đa 72 byte UTF-8.
Số điện thoại: `^0\d{9}$`. Email: tối đa 255 ký tự.

#### POST /api/auth/register

**Quyền:** Công khai. Đăng ký tài khoản bệnh nhân, gửi liên kết kích hoạt (hạn 24 giờ) tới email.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `hoTen` | chuỗi | có | tối đa 150 |
| `email` | chuỗi | có | |
| `matKhau` | chuỗi | có | |
| `soDienThoai` | chuỗi | có | |
| `cccd` | chuỗi | không | 12 chữ số. Có thì hồ sơ đã tạo khi đặt lịch như khách bằng CCCD này được liên kết vào tài khoản |

**Response:** HTTP 201, `duLieu` = `TaiKhoanResponse` (`trangThai: "CHO_XAC_NHAN"`).

**Lỗi:** 409 `EMAIL_DA_TON_TAI`, `SO_DIEN_THOAI_DA_TON_TAI`; 429 `GUI_LAI_QUA_NHANH` (vừa gửi email cho địa chỉ này,
chờ 60 giây).

#### POST /api/auth/verify-email

**Quyền:** Công khai. Kích hoạt tài khoản bằng `token` trong liên kết email.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `token` | chuỗi | có | tối đa 100 |

**Response:** không có `duLieu`.

**Lỗi:** 410 `LIEN_KET_KHONG_HOP_LE` (sai, đã dùng hoặc hết hạn: cho người dùng gửi lại); 403 `TAI_KHOAN_BI_VO_HIEU_HOA`.

#### POST /api/auth/resend-verification

**Quyền:** Công khai. Gửi lại liên kết kích hoạt. Luôn trả cùng 1 thông báo (không cho biết email có tồn tại hay không).

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `email` | chuỗi | có | |

**Response:** không có `duLieu`.

#### POST /api/auth/login

**Quyền:** Công khai. Đăng nhập bằng email và mật khẩu (mọi vai trò).

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `email` | chuỗi | có | |
| `matKhau` | chuỗi | có | |

**Response:** `duLieu` = `DangNhapResponse`, kèm header `Set-Cookie: eclinic_rt=...`.

```json
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9...", "loaiToken": "Bearer", "thoiHanAccessToken": 1800,
  "taiKhoan": { "id": 2, "hoTen": "BS. Trần Minh", "email": "bacsi01@eclinic.local", "soDienThoai": "0912000001",
    "vaiTro": "BAC_SI", "trangThai": "DA_KICH_HOAT", "ngonNgu": "VI" } }
```

**Lỗi:** 401 `SAI_THONG_TIN_DANG_NHAP` (sai email hoặc mật khẩu, không phân biệt); 403 `TAI_KHOAN_CHUA_XAC_THUC` (chưa
kích hoạt: gợi ý gửi lại liên kết), `TAI_KHOAN_BI_VO_HIEU_HOA`, `PHAI_DOI_MAT_KHAU` (bác sĩ mới, mục 1.5); 429
`DANG_NHAP_SAI_QUA_NHIEU` (sai 5 lần trong 15 phút, khoá 15 phút).

#### POST /api/auth/google

**Quyền:** Công khai. Đăng nhập bằng Google (chỉ bệnh nhân); chưa có tài khoản thì tạo mới.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idToken` | chuỗi | có | `credential` do Google Identity Services trả về, tối đa 4096 |

**Response:** như `/api/auth/login`.

**Lỗi:** 401 `GOOGLE_TOKEN_KHONG_HOP_LE`; 403 `DANG_NHAP_GOOGLE_KHONG_HO_TRO` (email thuộc tài khoản bác sĩ / quản trị
viên), `TAI_KHOAN_BI_VO_HIEU_HOA`; 409 `EMAIL_DA_DANG_KY_MAT_KHAU` (email đã đăng ký bằng
mật khẩu, không liên kết tự động được: đăng nhập bằng mật khẩu); 503 `DANG_NHAP_GOOGLE_KHONG_KHA_DUNG` (server chưa cấu
hình Google).

#### POST /api/auth/first-password

**Quyền:** Công khai. Bác sĩ mới đặt mật khẩu ở lần đăng nhập đầu (sau 403 `PHAI_DOI_MAT_KHAU`). Không đăng nhập luôn: gọi
`/api/auth/login` lại bằng mật khẩu mới.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `email` | chuỗi | có | |
| `matKhauHienTai` | chuỗi | có | mật khẩu mặc định được cấp |
| `matKhauMoi` | chuỗi | có | khác mật khẩu hiện tại |

**Response:** không có `duLieu`.

**Lỗi:** 401 `SAI_THONG_TIN_DANG_NHAP`; 409 `TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE` (tài khoản không cần đặt mật khẩu lần
đầu); 400 `MAT_KHAU_MOI_TRUNG_MAT_KHAU_CU`; 403 `TAI_KHOAN_BI_VO_HIEU_HOA`; 429 `DANG_NHAP_SAI_QUA_NHIEU`.

#### POST /api/auth/refresh-token

**Quyền:** Công khai (dùng cookie `eclinic_rt`, gửi với `credentials: 'include'`). Đổi refresh token lấy access token mới;
cookie được thay bằng token mới.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `refreshToken` | chuỗi | không | chỉ để thử bằng Swagger / curl; trình duyệt không gửi body |

**Response:** như `/api/auth/login`.

**Lỗi:** 401 `PHIEN_DANG_NHAP_KHONG_HOP_LE` (không có cookie, hết hạn, đã đăng xuất: về trang đăng nhập); 403
`TAI_KHOAN_BI_VO_HIEU_HOA`.

#### POST /api/auth/logout

**Quyền:** Công khai (dùng cookie). Đăng xuất thiết bị này và xoá cookie. Luôn thành công. Frontend xoá access token
trong bộ nhớ.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `refreshToken` | chuỗi | không | như refresh-token |

**Response:** không có `duLieu`.

#### POST /api/auth/forgot-password

**Quyền:** Công khai. Gửi liên kết đặt lại mật khẩu (hạn 15 phút). Luôn trả cùng 1 thông báo.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `email` | chuỗi | có | |

**Response:** không có `duLieu`.

#### POST /api/auth/reset-password

**Quyền:** Công khai. Đặt mật khẩu mới bằng `token` trong liên kết; mọi thiết bị bị đăng xuất. Không tự đăng nhập.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `token` | chuỗi | có | tối đa 100 |
| `matKhauMoi` | chuỗi | có | |

**Response:** không có `duLieu`.

**Lỗi:** 410 `LIEN_KET_KHONG_HOP_LE`; 403 `TAI_KHOAN_BI_VO_HIEU_HOA`. Mật khẩu sai quy tắc → 400 (liên kết vẫn dùng được).

#### POST /api/auth/confirm-email-change

**Quyền:** Công khai. Xác nhận đổi email bằng `token` trong liên kết gửi tới email mới; mọi thiết bị bị đăng xuất (cả thiết
bị đang mở: cho người dùng đăng nhập lại bằng email mới).

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `token` | chuỗi | có | tối đa 100 |

**Response:** không có `duLieu`.

**Lỗi:** 410 `LIEN_KET_KHONG_HOP_LE`; 403 `TAI_KHOAN_BI_VO_HIEU_HOA`; 409 `EMAIL_DA_TON_TAI` (email mới vừa bị tài
khoản khác dùng).

### 2.2 Tài khoản của tôi

**Quyền:** Đăng nhập (mọi vai trò), luôn thao tác trên tài khoản của chính người gọi.

#### GET /api/users/me

**Quyền:** Đăng nhập. Hồ sơ của tôi.

**Request:** Không có.

**Response:** `duLieu` = `HoSoCaNhanResponse` (mục 1.8).

#### PUT /api/users/me

**Quyền:** Đăng nhập. Sửa họ tên, số điện thoại.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `hoTen` | chuỗi | có | tối đa 150. **Bác sĩ** phải gửi đúng họ tên hiện tại (chỉ quản trị viên đổi được) |
| `soDienThoai` | chuỗi | không | bỏ trống / null thì giữ số cũ |

**Response:** `HoSoCaNhanResponse` sau khi sửa.

**Lỗi:** 403 `KHONG_CO_QUYEN` (bác sĩ đổi họ tên); 409 `SO_DIEN_THOAI_DA_TON_TAI`.

#### PUT /api/users/me/language

**Quyền:** Đăng nhập. Lưu ngôn ngữ ưa dùng cho email và nhắc lịch (mục 1.6). Gọi khi người dùng đổi ngôn ngữ trên giao diện.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `ngonNgu` | `NgonNgu` | có | `VI` / `EN` |

**Response:** `HoSoCaNhanResponse`.

#### PUT /api/users/me/change-password

**Quyền:** Đăng nhập. Đổi mật khẩu; các thiết bị khác bị đăng xuất, thiết bị này giữ nguyên.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `matKhauCu` | chuỗi | có | |
| `matKhauMoi` | chuỗi | có | khác mật khẩu cũ |

**Response:** không có `duLieu`.

**Lỗi:** 400 `MAT_KHAU_CU_KHONG_DUNG`, `MAT_KHAU_MOI_TRUNG_MAT_KHAU_CU`; 409 `TAI_KHOAN_CHUA_CO_MAT_KHAU` (tài khoản chỉ
đăng nhập Google: dùng Quên mật khẩu); 429 `SAI_MAT_KHAU_QUA_NHIEU` (sai 5 lần, khoá 15 phút).

#### POST /api/users/me/avatar

**Quyền:** Đăng nhập. Đổi ảnh đại diện. Body `multipart/form-data`.

| Trường (form) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `anh` | tệp | có | JPEG / PNG / WebP (nhận ra theo nội dung, không theo tên tệp), tối đa 2 MB |

**Response:** `HoSoCaNhanResponse` với `anhDaiDien` mới.

**Lỗi:** 400 `ANH_KHONG_HOP_LE`, `DU_LIEU_KHONG_HOP_LE` (thiếu phần `anh`); 413 `TEP_QUA_LON`; 429 `GUI_LAI_QUA_NHANH`
(quá 10 lần / giờ); 503 `LUU_TRU_ANH_KHONG_KHA_DUNG`.

#### DELETE /api/users/me/avatar

**Quyền:** Đăng nhập. Bỏ ảnh đại diện (kể cả ảnh lấy từ Google). Chưa có ảnh cũng thành công.

**Request:** Không có.

**Response:** `HoSoCaNhanResponse` với `anhDaiDien: null`.

#### POST /api/users/me/change-email

**Quyền:** Đăng nhập. Yêu cầu đổi email: gửi liên kết xác nhận (hạn 1 giờ) tới email mới. Email chỉ đổi sau khi xác nhận
(`POST /api/auth/confirm-email-change`).

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `emailMoi` | chuỗi | có | |
| `matKhauHienTai` | chuỗi | có | |

**Response:** `duLieu` = `{ "emailMoi": "moi@example.com" }`.

**Lỗi:** 400 `EMAIL_MOI_TRUNG_EMAIL_CU`, `MAT_KHAU_CU_KHONG_DUNG`; 409 `EMAIL_DA_TON_TAI`, `TAI_KHOAN_CHUA_CO_MAT_KHAU`;
429 `GUI_LAI_QUA_NHANH` (chờ 60 giây), `SAI_MAT_KHAU_QUA_NHIEU`.

#### GET /api/users/me/change-email

**Quyền:** Đăng nhập. Yêu cầu đổi email đang chờ xác nhận.

**Request:** Không có.

**Response:** `duLieu` = `{ "emailMoi": "..." }`; không có yêu cầu nào thì `duLieu` vắng mặt.

#### DELETE /api/users/me/change-email

**Quyền:** Đăng nhập. Huỷ yêu cầu đổi email đang chờ (liên kết đã gửi hết hiệu lực). Luôn thành công.

**Request:** Không có.

**Response:** không có `duLieu`.

#### GET /api/users/me/sessions

**Quyền:** Đăng nhập. Các thiết bị đang đăng nhập; thiết bị đang dùng đứng đầu.

**Request:** Không có.

**Response:** `duLieu` = mảng:

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | chuỗi | mã phiên, dùng để đăng xuất từ xa |
| `thietBi` | chuỗi \| null | User-Agent lúc đăng nhập |
| `dangNhapLuc`, `hoatDongLuc`, `hetHanLuc` | ngày giờ | |
| `hienTai` | boolean | thiết bị đang dùng |

```json
[ { "id": "7f3c2a9e-...", "thietBi": "Mozilla/5.0 (Windows NT 10.0; ...)", "dangNhapLuc": "2026-10-20T08:00:00",
    "hoatDongLuc": "2026-10-21T09:15:00", "hetHanLuc": "2026-10-28T09:15:00", "hienTai": true } ]
```

#### DELETE /api/users/me/sessions/{id}

**Quyền:** Đăng nhập. Đăng xuất 1 thiết bị khác. Thiết bị đó còn dùng access token đang giữ tối đa 30 phút.

| Trường (path) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `id` | chuỗi | có | `id` trong danh sách thiết bị |

**Response:** không có `duLieu`.

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (là thiết bị đang dùng: gọi `/api/auth/logout`); 404 `KHONG_TIM_THAY`.

#### DELETE /api/users/me/sessions

**Quyền:** Đăng nhập. Đăng xuất mọi thiết bị khác, giữ thiết bị đang dùng.

**Request:** Không có.

**Response:** `duLieu` = số thiết bị đã đăng xuất (số nguyên).

### 2.3 Thông báo (chuông)

**Quyền:** Đăng nhập (mọi vai trò), chỉ thông báo của chính người gọi. Không có đẩy realtime: gọi lại
`GET /api/notification/thong-bao/so-chua-doc` định kỳ (vd 30 giây) và khi mở trang.

`LoaiThongBao` theo người nhận:

| Người nhận | Giá trị |
|---|---|
| Bác sĩ | `LICH_HEN_MOI`, `LICH_HEN_DA_DOI`, `LICH_HEN_DA_HUY`, `KET_QUA_DUYET_DOI_LICH`, `CA_LAM_VIEC_THAY_DOI`, `YEU_CAU_NHUONG_CA` |
| Bệnh nhân | `LICH_HEN_DA_XAC_NHAN` (chỉ còn ở thông báo cũ), `LICH_HEN_BI_TU_CHOI` (bác sĩ hủy), `LICH_HEN_CAN_DOI`, `LICH_HEN_DOI_PHONG`, `LICH_HEN_DOI_BAC_SI`, `NHAC_LICH_KHAM`, `NHAC_TAI_KHAM`, `LICH_HEN_QUAN_TRI_HUY`, `LICH_HEN_QUAN_TRI_DOI` |
| Quản trị viên | `YEU_CAU_DOI_LICH_MOI` |
| Mọi vai trò | `HE_THONG` |

Bấm 1 thông báo: đánh dấu đã đọc, rồi mở lịch hẹn (`maPhieuKham` với bệnh nhân, `idLichHen` với bác sĩ / quản trị viên)
hoặc yêu cầu (`idYeuCau`) nếu có.

#### GET /api/notification/thong-bao

**Quyền:** Đăng nhập. Thông báo của tôi, mới nhất trước.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `chuaDoc` | boolean | không | `true`: chỉ thông báo chưa đọc. Mặc định `false` |
| `trang` | số | không | từ 0, mặc định 0 |
| `kichThuoc` | số | không | 1–100, mặc định 20 |

**Response:** `TrangDuLieu<ThongBaoResponse>` (mục 1.3, 1.8).

```json
{ "noiDung": [ { "id": 41, "loai": "NHAC_LICH_KHAM", "noiDung": "Nhắc lịch khám lúc 08:30 ngày 22/10/2026 ...",
    "daDoc": false, "ngayTao": "2026-10-21T08:00:00", "idLichHen": 12, "maPhieuKham": "Xk3...", "idYeuCau": null } ],
  "trang": 0, "kichThuoc": 20, "tongSoPhanTu": 1, "tongSoTrang": 1 }
```

#### GET /api/notification/thong-bao/so-chua-doc

**Quyền:** Đăng nhập. Số thông báo chưa đọc (số trên chuông).

**Request:** Không có.

**Response:** `duLieu` = `{ "soChuaDoc": 3 }`.

#### POST /api/notification/thong-bao/{id}/da-doc

**Quyền:** Đăng nhập. Đánh dấu 1 thông báo đã đọc (gọi lại nhiều lần vẫn được).

| Trường (path) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `id` | số | có | id thông báo |

**Response:** `ThongBaoResponse` sau khi đánh dấu.

**Lỗi:** 404 `KHONG_TIM_THAY_THONG_BAO` (không có, hoặc không phải của tôi).

#### POST /api/notification/thong-bao/da-doc-tat-ca

**Quyền:** Đăng nhập. Đánh dấu mọi thông báo của tôi đã đọc.

**Request:** Không có.

**Response:** `duLieu` = `{ "soDaDanhDau": 3 }`.
<!-- CHUNG:KET-THUC -->

## 3. Quản trị đặt lịch và lịch làm việc

### 3.1 Tổng quan

Mọi route của mục 3 cần token quản trị viên (`QUAN_TRI_VIEN`) và làm việc trên dữ liệu **toàn viện**: lọc theo chuyên
khoa / bác sĩ / phòng bằng query. Id không có trả 404 `KHONG_TIM_THAY`. Thời điểm và ngày theo giờ Việt Nam, không có
múi giờ.

**Ba luồng chính**

1. **Lên lịch làm việc:**
   - khai báo ngày bệnh viện nghỉ trước: `POST /api/booking/quan-tri/ngay-nghi/xem-truoc` rồi `POST …/ngay-nghi`;
   - xếp ca: từng ca (có lặp lại theo tuần) `POST /api/booking/quan-tri/lich-lam-viec`, sao chép tuần / tháng trước
     `POST …/lich-lam-viec/sao-chep/xem-truoc` rồi `…/sao-chep`, hoặc nhập Excel (tải mẫu `GET …/nhap-excel/mau`, xem
     trước `POST …/nhap-excel/xem-truoc`, nhập `POST …/nhap-excel`);
   - kiểm tra lưới ca `GET …/lich-lam-viec` và giờ làm mỗi tuần `GET …/gio-lam-theo-tuan`, in PDF;
   - báo cho bác sĩ: `POST …/lich-lam-viec/gui-lich`.
2. **Ca bị hủy / bác sĩ xin nghỉ:**
   - duyệt yêu cầu của bác sĩ ở `GET /api/booking/quan-tri/yeu-cau-doi-lich`, hoặc tự hủy ca `POST …/lich-lam-viec/{id}/huy`;
   - lịch hẹn của ca bị hủy **không bị hủy**: giữ nguyên, được đánh dấu `canDoiLich` và bệnh nhân được báo;
   - danh sách cần xử lý `GET …/lich-lam-viec/can-doi-lich`: gọi bệnh nhân rồi đổi lịch thay
     `POST /api/booking/quan-tri/lich-hen/{id}/doi-lich`, hoặc hủy `POST …/lich-hen/{id}/huy`.
3. **Tổng đài / quầy:**
   - tìm lịch hẹn: `GET /api/booking/quan-tri/lich-hen/tra-cuu?ma=` (mã trên phiếu / quét QR) hoặc
     `GET /api/booking/quan-tri/lich-hen?tuKhoa=`;
   - đặt lịch thay người gọi: chọn ca / khung giờ bằng các route công khai của trang đặt lịch (`GET /api/booking/khung-gio`…,
     xem tài liệu bệnh nhân / khách), rồi `POST /api/booking/quan-tri/lich-hen`. Phản hồi có phiếu khám để in;
   - hủy, đổi lịch, cấp lại link phiếu khám (`…/cap-lai-lien-ket`) cho người đã mất link.

**Id và mã.** Quản trị viên làm việc bằng `id` của lịch hẹn. `maTraCuu` (`ECL-yyyyMMdd-nnnn`) in trên phiếu. Mã phiếu
khám (mở trang phiếu khám) **không** được trả cho quản trị viên, trừ lúc vừa đặt qua tổng đài và khi cấp lại liên kết
bằng `MAN_HINH`.

**Quy tắc đặt lịch / xếp ca** (số lịch tối đa, hạn hủy / đổi, số ngày xếp trước, giờ làm tối đa mỗi tuần…) không cố định
trong code: quản trị viên sửa ở `PUT /api/booking/quan-tri/cau-hinh` ([mục 3.3](#33-tổng-quan-và-quy-tắc-đặt-lịch)).
Các số "mặc định" trong tài liệu này là giá trị ban đầu.

**Tất cả route của mục 3**

| Phương thức | Đường dẫn | Quyền | Mục đích |
|---|---|---|---|
| GET | `/api/booking/quan-tri/tong-quan` | QUAN_TRI_VIEN | Số liệu Dashboard |
| GET | `/api/booking/quan-tri/cau-hinh` | QUAN_TRI_VIEN | Quy tắc đặt lịch / xếp ca đang dùng |
| PUT | `/api/booking/quan-tri/cau-hinh` | QUAN_TRI_VIEN | Lưu quy tắc |
| GET | `/api/booking/quan-tri/cau-hinh/lich-su` | QUAN_TRI_VIEN | Lịch sử sửa quy tắc |
| GET | `/api/booking/quan-tri/ngay-nghi` | QUAN_TRI_VIEN | Ngày bệnh viện nghỉ |
| POST | `/api/booking/quan-tri/ngay-nghi/xem-truoc` | QUAN_TRI_VIEN | Xem trước khai báo ngày nghỉ |
| POST | `/api/booking/quan-tri/ngay-nghi` | QUAN_TRI_VIEN | Khai báo ngày nghỉ |
| DELETE | `/api/booking/quan-tri/ngay-nghi/{ngay}` | QUAN_TRI_VIEN | Bỏ 1 ngày nghỉ |
| GET | `/api/booking/quan-tri/lich-lam-viec` | QUAN_TRI_VIEN | Lưới ca toàn viện |
| POST | `/api/booking/quan-tri/lich-lam-viec` | QUAN_TRI_VIEN | Xếp ca (có lặp lại) |
| PUT | `/api/booking/quan-tri/lich-lam-viec/{id}` | QUAN_TRI_VIEN | Sửa ca |
| POST | `/api/booking/quan-tri/lich-lam-viec/{id}/huy` | QUAN_TRI_VIEN | Hủy ca |
| GET | `/api/booking/quan-tri/lich-lam-viec/{id}/lich-su` | QUAN_TRI_VIEN | Lịch sử xếp / sửa / hủy của 1 ca |
| GET | `/api/booking/quan-tri/lich-lam-viec/{id}/lich-hen` | QUAN_TRI_VIEN | Lịch hẹn của 1 ca |
| GET | `/api/booking/quan-tri/lich-lam-viec/gio-lam-theo-tuan` | QUAN_TRI_VIEN | Giờ làm mỗi tuần của bác sĩ |
| GET | `/api/booking/quan-tri/lich-lam-viec/can-doi-lich` | QUAN_TRI_VIEN | Lịch hẹn chờ đổi vì ca bị hủy |
| POST | `/api/booking/quan-tri/lich-lam-viec/sao-chep/xem-truoc` | QUAN_TRI_VIEN | Xem trước sao chép ca |
| POST | `/api/booking/quan-tri/lich-lam-viec/sao-chep` | QUAN_TRI_VIEN | Sao chép ca |
| GET | `/api/booking/quan-tri/lich-lam-viec/xuat-excel` | QUAN_TRI_VIEN | Tải Excel các ca |
| GET | `/api/booking/quan-tri/lich-lam-viec/nhap-excel/mau` | QUAN_TRI_VIEN | Tải Excel mẫu để nhập |
| POST | `/api/booking/quan-tri/lich-lam-viec/nhap-excel/xem-truoc` | QUAN_TRI_VIEN | Xem trước nhập Excel |
| POST | `/api/booking/quan-tri/lich-lam-viec/nhap-excel` | QUAN_TRI_VIEN | Nhập ca từ Excel |
| GET | `/api/booking/quan-tri/lich-lam-viec/pdf/bac-si/{idBacSi}` | QUAN_TRI_VIEN | PDF lịch của 1 bác sĩ |
| GET | `/api/booking/quan-tri/lich-lam-viec/pdf/toan-vien` | QUAN_TRI_VIEN | PDF lưới tuần toàn viện |
| POST | `/api/booking/quan-tri/lich-lam-viec/gui-lich` | QUAN_TRI_VIEN | Gửi lịch làm việc cho bác sĩ |
| GET | `/api/booking/quan-tri/yeu-cau-doi-lich` | QUAN_TRI_VIEN | Yêu cầu đổi ca / xin nghỉ / nhường ca |
| POST | `/api/booking/quan-tri/yeu-cau-doi-lich/{id}/duyet` | QUAN_TRI_VIEN | Duyệt 1 yêu cầu |
| POST | `/api/booking/quan-tri/yeu-cau-doi-lich/{id}/tu-choi` | QUAN_TRI_VIEN | Từ chối 1 yêu cầu |
| POST | `/api/booking/quan-tri/yeu-cau-doi-lich/nhom/{maNhom}/duyet` | QUAN_TRI_VIEN | Duyệt cả nhóm xin nghỉ |
| POST | `/api/booking/quan-tri/yeu-cau-doi-lich/nhom/{maNhom}/tu-choi` | QUAN_TRI_VIEN | Từ chối cả nhóm xin nghỉ |
| GET | `/api/booking/quan-tri/lich-hen` | QUAN_TRI_VIEN | Lịch hẹn toàn viện |
| GET | `/api/booking/quan-tri/lich-hen/tra-cuu` | QUAN_TRI_VIEN | Tìm 1 lịch hẹn theo mã / QR |
| GET | `/api/booking/quan-tri/lich-hen/{id}` | QUAN_TRI_VIEN | Chi tiết + nhật ký 1 lịch hẹn |
| POST | `/api/booking/quan-tri/lich-hen` | QUAN_TRI_VIEN | Đặt lịch qua tổng đài / quầy |
| POST | `/api/booking/quan-tri/lich-hen/{id}/huy` | QUAN_TRI_VIEN | Hủy lịch hẹn |
| POST | `/api/booking/quan-tri/lich-hen/{id}/doi-lich` | QUAN_TRI_VIEN | Đổi lịch thay bệnh nhân |
| POST | `/api/booking/quan-tri/lich-hen/{id}/da-doi-chieu` | QUAN_TRI_VIEN | Bỏ đánh dấu cần đối chiếu |
| POST | `/api/booking/quan-tri/lich-hen/{id}/cap-lai-lien-ket` | QUAN_TRI_VIEN | Cấp lại link phiếu khám |
| GET | `/api/booking/quan-tri/ho-so-benh-nhan` | QUAN_TRI_VIEN | Danh sách hồ sơ bệnh nhân |
| GET | `/api/booking/quan-tri/ho-so-benh-nhan/cho-xac-minh` | QUAN_TRI_VIEN | Hồ sơ chờ xác minh liên kết |
| GET | `/api/booking/quan-tri/ho-so-benh-nhan/{id}` | QUAN_TRI_VIEN | Xem 1 hồ sơ (CCCD không che) |
| GET | `/api/booking/quan-tri/ho-so-benh-nhan/{id}/lich-hen` | QUAN_TRI_VIEN | Lịch hẹn của 1 hồ sơ |
| PUT | `/api/booking/quan-tri/ho-so-benh-nhan/{id}` | QUAN_TRI_VIEN | Sửa hồ sơ sau đối chiếu |
| POST | `/api/booking/quan-tri/ho-so-benh-nhan/{id}/duyet` | QUAN_TRI_VIEN | Duyệt liên kết hồ sơ ↔ tài khoản |
| POST | `/api/booking/quan-tri/ho-so-benh-nhan/{id}/tu-choi` | QUAN_TRI_VIEN | Từ chối liên kết |
| GET | `/api/booking/quan-tri/danh-gia` | QUAN_TRI_VIEN | Đánh giá của bệnh nhân |

**Lỗi chung của mục 3** (ngoài mục 1.4, không nhắc lại ở từng route): 403 `KHONG_CO_QUYEN` khi token không phải của
quản trị viên; 403 `TAI_KHOAN_BI_VO_HIEU_HOA` khi tài khoản bị vô hiệu hoá sau khi token được cấp. Khoảng ngày sai
(`denNgay` trước `tuNgay`, dài hơn mức tối đa) trả 400 `DU_LIEU_KHONG_HOP_LE`.

### 3.2 Mô hình và enum của mục 3

`GioiTinh`, `TrangThaiLienKet`, `TrangThaiLichHen`, `NgonNgu`: xem [mục 1.7](#17-enum-dùng-chung).

| Enum | Giá trị |
|---|---|
| `BuoiKham` | `SANG` (ca bắt đầu trước 12:00), `CHIEU` (12:00–17:00), `TOI` (từ 17:00) |
| `TrangThaiLichLamViec` | `HOAT_DONG`, `DA_HUY` |
| `KenhDatLich` | `KHACH` (đặt không đăng nhập), `TAI_KHOAN` (bệnh nhân đã đăng nhập), `HOTLINE` (quản trị viên đặt thay qua tổng đài / quầy) |
| `QuanHeGiamHo` | `CHA`, `ME`, `NGUOI_GIAM_HO_HOP_PHAP`, `KHAC` |
| `CachCapLai` | `EMAIL` (gửi tới email của lịch hẹn), `MAN_HINH` (trả mã + link để mở mã QR cho người ở quầy; cách duy nhất khi lịch hẹn không có email) |
| `LoaiYeuCauDoiLich` | `DOI_CA`, `XIN_NGHI`, `NHUONG_CA` (nhường cho bác sĩ cùng chuyên khoa) |
| `TrangThaiYeuCau` | `CHO_DONG_Y` (nhường ca: chờ bác sĩ nhận), `CHO_DUYET` (chờ quản trị viên), `DA_DUYET`, `TU_CHOI` (quản trị viên từ chối), `DA_RUT` (bác sĩ đã rút), `DONG_NGHIEP_TU_CHOI` (bác sĩ nhận từ chối) |
| `SuKienLichHen` | `DAT_LICH`, `DOI_LICH` (dòng nằm ở lịch cũ, `maTraCuuLienQuan` là lịch mới), `HUY` (ai hủy: xem trạng thái lịch hẹn và người thực hiện), `CA_BI_HUY` (lịch giữ nguyên, cần đổi lịch), `DOI_BAC_SI` (ca được nhường cho bác sĩ khác), `CA_THAY_DOI` (ca đổi phòng / giờ), `CAP_LAI_LIEN_KET` (lý do ghi kênh gửi) |
| `LoaiNguoiThucHien` | `KHACH` (người cầm link phiếu khám hoặc khách đặt không đăng nhập), `TAI_KHOAN` (bệnh nhân đã đăng nhập), `BAC_SI`, `QUAN_TRI_VIEN` |
| `HanhDongCa` | `TAO`, `SUA` (nhường ca cũng là `SUA` với `cacTruongDoi = ["bacSi"]`), `HUY` |
| `NguonThayDoiCa` | `QUAN_TRI` (sửa trực tiếp), `DUYET_YEU_CAU` (duyệt yêu cầu của bác sĩ, có `idYeuCau`), `NGUNG_CONG_TAC` (hủy hàng loạt khi bác sĩ ngừng công tác), `SAO_CHEP`, `NHAP_EXCEL`, `NGAY_NGHI` (hủy khi khai báo ngày nghỉ), `DU_LIEU_MAU`, `DU_LIEU_CU` |

#### `BacSiTomTatResponse`

`{ id, hoTen, hocVi, anhDaiDien }`. `hocVi`, `anhDaiDien` (URL) có thể null.

#### `PhongKhamTomTatResponse`

`{ id, tenPhong, tang }`, theo ngôn ngữ của request. `tang` có thể null.

#### `CaLamViecResponse`

1 ca làm việc, có cả ca đã hủy.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `idLichLamViec` | số | id ca, dùng cho mọi route `lich-lam-viec/{id}` |
| `ngay` | ngày | |
| `gioBatDau`, `gioKetThuc` | giờ | |
| `soLuotToiDaMoiGio`, `thoiLuongLuotPhut` | số | số lượt mỗi giờ (N), độ dài 1 lượt (t, phút) |
| `trangThai` | `TrangThaiLichLamViec` | |
| `bacSi` | [`BacSiTomTatResponse`](#bacsitomtatresponse) | |
| `tenChuyenKhoa` | chuỗi | |
| `phongKham` | [`PhongKhamTomTatResponse`](#phongkhamtomtatresponse) | |
| `tongSoLuot` | số | số lượt của ca, không tính lượt đã hủy |
| `soLuotDaDat`, `soLuotConTrong` | số | đã có người đặt; còn trống (kể cả lượt đã qua giờ) |

```json
{ "idLichLamViec": 55, "ngay": "2026-10-28", "gioBatDau": "08:00:00", "gioKetThuc": "11:00:00", "soLuotToiDaMoiGio": 4,
  "thoiLuongLuotPhut": 15, "trangThai": "HOAT_DONG",
  "bacSi": { "id": 7, "hoTen": "Trần Thị Bình", "hocVi": "ThS.BS", "anhDaiDien": null }, "tenChuyenKhoa": "Nội tổng quát",
  "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" },
  "tongSoLuot": 12, "soLuotDaDat": 3, "soLuotConTrong": 9 }
```

#### `LichHenTrongCaResponse`

1 dòng lịch hẹn trong các danh sách của quản trị viên. **Số điện thoại không che**; không có mã phiếu khám, không có CCCD.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | số | id lịch hẹn, dùng cho mọi route `lich-hen/{id}` |
| `maTraCuu` | chuỗi | mã in trên phiếu, vd `ECL-20261025-4198` |
| `idLichLamViec` | số | ca của lịch hẹn |
| `trangThai` | `TrangThaiLichHen` | |
| `soThuTu` | số | số thứ tự theo phòng khám + ngày |
| `gioKhamDuKien` | ngày giờ | giờ bắt đầu lượt khám |
| `gioBatDauKhung`, `gioKetThucKhung` | ngày giờ | khung 1 giờ người đặt đã chọn |
| `bacSi` | [`BacSiTomTatResponse`](#bacsitomtatresponse) | bác sĩ khám |
| `phongKham` | [`PhongKhamTomTatResponse`](#phongkhamtomtatresponse) | |
| `lyDoKham` | chuỗi \| null | người đặt ghi khi đặt |
| `ngayDat` | ngày giờ | |
| `idHoSoBenhNhan` | số | mở hồ sơ ở `GET /api/booking/quan-tri/ho-so-benh-nhan/{id}` |
| `benhNhan` | object | `{ hoTen, ngaySinh, tuoi, gioiTinh, soDienThoai }` theo hồ sơ; `tuoi` tròn vào ngày khám (null nếu không có ngày sinh); `soDienThoai` null khi có người giám hộ |
| `nguoiGiamHo` | object \| null | `{ hoTen, quanHe, soDienThoai }` (`quanHe`: `QuanHeGiamHo`) |
| `doiChieu` | object | `{ canDoiChieu, hoTenDaNhap, ngaySinhDaNhap, gioiTinhDaNhap, hoTenGiamHoDaNhap }`: những gì người đặt nhập. `canDoiChieu = true`: họ tên / ngày sinh nhập khác hồ sơ và chưa đối chiếu → hiện cảnh báo; sửa hồ sơ ở mục 3.9, xong gọi `POST …/lich-hen/{id}/da-doi-chieu` |
| `lyDoHuy` | chuỗi \| null | có ở `BI_TU_CHOI` và các trạng thái hủy |
| `canDoiLich` | boolean | `true`: ca đã bị hủy, lịch còn hiệu lực và đang chờ đổi sang khung giờ khác (không khám được) |
| `kenhDat` | `KenhDatLich` | |
| `chuyenKhoa` | object | `{ id, ten }` của bác sĩ khám |

```json
{
  "id": 812, "maTraCuu": "ECL-20261025-4198", "idLichLamViec": 55, "trangThai": "DA_XAC_NHAN", "soThuTu": 3,
  "gioKhamDuKien": "2026-10-25T08:30:00", "gioBatDauKhung": "2026-10-25T08:00:00", "gioKetThucKhung": "2026-10-25T09:00:00",
  "bacSi": { "id": 7, "hoTen": "Trần Thị Bình", "hocVi": "ThS.BS", "anhDaiDien": null },
  "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" },
  "lyDoKham": "Ho kéo dài", "ngayDat": "2026-10-20T21:14:05", "idHoSoBenhNhan": 301,
  "benhNhan": { "hoTen": "Nguyễn Văn An", "ngaySinh": "1990-05-12", "tuoi": 36, "gioiTinh": "NAM", "soDienThoai": "0901234001" },
  "nguoiGiamHo": null,
  "doiChieu": { "canDoiChieu": false, "hoTenDaNhap": "Nguyễn Văn An", "ngaySinhDaNhap": "1990-05-12", "gioiTinhDaNhap": "NAM", "hoTenGiamHoDaNhap": null },
  "lyDoHuy": null, "canDoiLich": false, "kenhDat": "HOTLINE", "chuyenKhoa": { "id": 1, "ten": "Nội tổng quát" }
}
```

#### `LichHenQuanTriChiTietResponse`

Chi tiết 1 lịch hẹn: dòng lịch hẹn, liên hệ, chuỗi đổi lịch và nhật ký thao tác. Không có mã phiếu khám.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `lichHen` | [`LichHenTrongCaResponse`](#lichhentrongcaresponse) | |
| `emailLienHe` | chuỗi \| null | email người đặt nhập |
| `taiKhoanDat` | object \| null | `{ id, hoTen, email }`; null = khách đặt / đặt qua tổng đài |
| `ngonNgu` | `NgonNgu` \| null | ngôn ngữ email gửi người đặt; null với lịch hẹn rất cũ |
| `quanTriHuyDoiDuoc` | boolean | `true`: hiện nút hủy / đổi lịch (lịch `DA_XAC_NHAN` chưa tới giờ khám, hoặc đang `canDoiLich`) |
| `chuoiDoiLich` | mảng | các lịch của cùng 1 lần đặt (gồm lịch này), cũ nhất trước: `{ id, maTraCuu, trangThai, gioKhamDuKien, laLichNay }`; 1 phần tử nếu chưa đổi |
| `lichSu` | mảng | nhật ký của cả chuỗi, cũ nhất trước: `{ thoiDiem, maTraCuu, suKien, nguoiThucHien, lyDo, maTraCuuLienQuan }`. `suKien`: `SuKienLichHen`; `nguoiThucHien`: `{ loai, idTaiKhoan, hoTen }` (`loai`: `LoaiNguoiThucHien`, `idTaiKhoan` null khi `KHACH`); `lyDo` là câu người dùng gõ hoặc câu hệ thống theo ngôn ngữ của request; `maTraCuuLienQuan` chỉ có ở `DOI_LICH` (lịch mới) |

```json
{
  "lichHen": { "id": 845, "maTraCuu": "ECL-20261027-0712", "idLichLamViec": 61, "trangThai": "DA_XAC_NHAN", "soThuTu": 2,
    "gioKhamDuKien": "2026-10-27T13:15:00", "gioBatDauKhung": "2026-10-27T13:00:00", "gioKetThucKhung": "2026-10-27T14:00:00",
    "bacSi": { "id": 7, "hoTen": "Trần Thị Bình", "hocVi": "ThS.BS", "anhDaiDien": null },
    "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" },
    "lyDoKham": "Ho kéo dài", "ngayDat": "2026-10-22T10:02:11", "idHoSoBenhNhan": 301,
    "benhNhan": { "hoTen": "Nguyễn Văn An", "ngaySinh": "1990-05-12", "tuoi": 36, "gioiTinh": "NAM", "soDienThoai": "0901234001" },
    "nguoiGiamHo": null,
    "doiChieu": { "canDoiChieu": false, "hoTenDaNhap": "Nguyễn Văn An", "ngaySinhDaNhap": "1990-05-12", "gioiTinhDaNhap": "NAM", "hoTenGiamHoDaNhap": null },
    "lyDoHuy": null, "canDoiLich": false, "kenhDat": "KHACH", "chuyenKhoa": { "id": 1, "ten": "Nội tổng quát" } },
  "emailLienHe": "an@example.com", "taiKhoanDat": null, "ngonNgu": "VI", "quanTriHuyDoiDuoc": true,
  "chuoiDoiLich": [
    { "id": 812, "maTraCuu": "ECL-20261025-4198", "trangThai": "DA_HUY_DO_DOI_LICH", "gioKhamDuKien": "2026-10-25T08:30:00", "laLichNay": false },
    { "id": 845, "maTraCuu": "ECL-20261027-0712", "trangThai": "DA_XAC_NHAN", "gioKhamDuKien": "2026-10-27T13:15:00", "laLichNay": true } ],
  "lichSu": [
    { "thoiDiem": "2026-10-20T21:14:05", "maTraCuu": "ECL-20261025-4198", "suKien": "DAT_LICH",
      "nguoiThucHien": { "loai": "KHACH", "idTaiKhoan": null, "hoTen": null }, "lyDo": null, "maTraCuuLienQuan": null },
    { "thoiDiem": "2026-10-22T09:40:00", "maTraCuu": "ECL-20261025-4198", "suKien": "CA_BI_HUY",
      "nguoiThucHien": { "loai": "QUAN_TRI_VIEN", "idTaiKhoan": 1, "hoTen": "Quản trị viên" }, "lyDo": "Bác sĩ đi hội chẩn", "maTraCuuLienQuan": null },
    { "thoiDiem": "2026-10-22T10:02:11", "maTraCuu": "ECL-20261025-4198", "suKien": "DOI_LICH",
      "nguoiThucHien": { "loai": "QUAN_TRI_VIEN", "idTaiKhoan": 1, "hoTen": "Quản trị viên" }, "lyDo": "Bệnh nhân chọn chiều 27/10 qua điện thoại", "maTraCuuLienQuan": "ECL-20261027-0712" } ]
}
```

#### `YeuCauDoiLichResponse`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | số | |
| `loaiYeuCau` | `LoaiYeuCauDoiLich` | |
| `trangThai` | `TrangThaiYeuCau` | |
| `lyDo` | chuỗi | lý do bác sĩ ghi |
| `bacSi` | [`BacSiTomTatResponse`](#bacsitomtatresponse) | bác sĩ đã gửi |
| `ca` | [`CaLamViecResponse`](#calamviecresponse) | ca của yêu cầu, theo trạng thái hiện tại; `soLuotDaDat` > 0 nghĩa là duyệt thì các lịch hẹn đó phải đổi lịch (hoặc chuyển sang bác sĩ nhận). Sau khi duyệt nhường ca, `ca.bacSi` là bác sĩ nhận |
| `ngayMongMuon`, `gioBatDauMongMuon`, `gioKetThucMongMuon` | ngày, giờ \| null | chỉ có với `DOI_CA` |
| `phongKhamMongMuon` | [`PhongKhamTomTatResponse`](#phongkhamtomtatresponse) \| null | `DOI_CA`; null = giữ phòng hiện tại |
| `bacSiNhan` | [`BacSiTomTatResponse`](#bacsitomtatresponse) \| null | chỉ có với `NHUONG_CA` |
| `ghiChuBacSiNhan` | chuỗi \| null | `NHUONG_CA`: ghi chú khi bác sĩ nhận từ chối |
| `ngayBacSiNhanPhanHoi` | ngày giờ \| null | `NHUONG_CA`: lúc bác sĩ nhận đồng ý / từ chối |
| `maNhom` | chuỗi \| null | chung cho các yêu cầu xin nghỉ gửi 1 lần (UUID); null với yêu cầu lẻ. Dùng cho route `nhom/{maNhom}` |
| `ghiChuXuLy` | chuỗi \| null | ghi chú của quản trị viên khi duyệt / từ chối, hoặc câu của hệ thống khi ca bị hủy làm đóng yêu cầu |
| `ngayGui` | ngày giờ | |
| `ngayXuLy` | ngày giờ \| null | lúc duyệt / từ chối / rút |

```json
{
  "id": 41, "loaiYeuCau": "DOI_CA", "trangThai": "CHO_DUYET", "lyDo": "Trùng lịch hội chẩn",
  "bacSi": { "id": 7, "hoTen": "Trần Thị Bình", "hocVi": "ThS.BS", "anhDaiDien": null },
  "ca": { "idLichLamViec": 55, "ngay": "2026-10-28", "gioBatDau": "08:00:00", "gioKetThuc": "11:00:00", "soLuotToiDaMoiGio": 4,
    "thoiLuongLuotPhut": 15, "trangThai": "HOAT_DONG", "bacSi": { "id": 7, "hoTen": "Trần Thị Bình", "hocVi": "ThS.BS", "anhDaiDien": null },
    "tenChuyenKhoa": "Nội tổng quát", "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" },
    "tongSoLuot": 12, "soLuotDaDat": 3, "soLuotConTrong": 9 },
  "ngayMongMuon": "2026-10-29", "gioBatDauMongMuon": "13:00:00", "gioKetThucMongMuon": "16:00:00",
  "phongKhamMongMuon": null, "bacSiNhan": null, "ghiChuBacSiNhan": null, "ngayBacSiNhanPhanHoi": null,
  "maNhom": null, "ghiChuXuLy": null, "ngayGui": "2026-10-21T09:00:00", "ngayXuLy": null
}
```

#### `CauHinhQuanTriResponse`

10 quy tắc đang dùng (ý nghĩa và giới hạn: bảng của `PUT /api/booking/quan-tri/cau-hinh`), cộng:

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `phienBan` | số | gửi lại khi lưu; +1 mỗi lần lưu có thay đổi |
| `ngaySua` | ngày giờ \| null | lần lưu gần nhất; null = chưa ai sửa (đang dùng giá trị từ biến môi trường lúc khởi tạo) |
| `quanTriSua` | object \| null | `{ id, hoTen }` của quản trị viên lưu gần nhất |

```json
{ "datTruocToiThieuPhut": 30, "soNgayDatTruocToiDa": 30, "soLichHieuLucToiDaMoiHoSo": 3,
  "soLichHieuLucToiDaMoiSoDienThoai": 5, "huyDoiTruocToiThieuPhut": 120, "soLanDoiLichToiDa": 2,
  "guiYeuCauTruocToiThieuGio": 24, "soNgayXepTruocToiDa": 90, "soGioToiDaMoiTuan": 48, "soNgayDuocSuaDanhGia": 7,
  "phienBan": 4, "ngaySua": "2026-10-18T15:20:00", "quanTriSua": { "id": 1, "hoTen": "Quản trị viên" } }
```

### 3.3 Tổng quan và quy tắc đặt lịch

#### GET /api/booking/quan-tri/tong-quan

**Quyền:** QUAN_TRI_VIEN. Các ô số liệu của Dashboard quản trị viên.

**Request:** Không có.

**Response:**

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `bacSiDangCongTac` | số | bác sĩ đang công tác có tài khoản đã kích hoạt (đúng tập bác sĩ của danh sách công khai) |
| `benhNhanDaDangKy` | số | tài khoản bệnh nhân đã kích hoạt; khách chỉ đặt lịch không tính |
| `luotDatTrongThang` | số | lịch hẹn tạo từ 00:00 ngày 1 tháng này, mọi trạng thái trừ `DA_HUY_DO_DOI_LICH` (1 lần đổi lịch không đếm 2 lần) |
| `tinhLuc` | ngày giờ | thời điểm tính |

```json
{ "bacSiDangCongTac": 12, "benhNhanDaDangKy": 148, "luotDatTrongThang": 326, "tinhLuc": "2026-10-21T09:05:12" }
```

#### GET /api/booking/quan-tri/cau-hinh

**Quyền:** QUAN_TRI_VIEN. Quy tắc đặt lịch / xếp ca đang dùng, để điền form.

**Request:** Không có.

**Response:** `duLieu` = [`CauHinhQuanTriResponse`](#cauhinhquantriresponse).

#### PUT /api/booking/quan-tri/cau-hinh

**Quyền:** QUAN_TRI_VIEN. Lưu **cả 10** quy tắc (đọc `GET` rồi gửi lại cả form). Giá trị mới chỉ áp dụng cho thao tác
mới: ca, lịch hẹn, đánh giá đã có không đổi. Mỗi trường đổi giá trị ghi 1 dòng lịch sử; không trường nào đổi thì không ghi
gì và `phienBan` giữ nguyên. Các service đọc lại trong tối đa 60 giây.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `datTruocToiThieuPhut` | số | có | 0–1440. Lượt khám chỉ đặt được khi còn cách giờ bắt đầu ít nhất chừng này phút (mặc định 30) |
| `soNgayDatTruocToiDa` | số | có | 1–180. Chỉ đặt được lịch trong số ngày tới này, tính từ hôm nay (mặc định 30) |
| `soLichHieuLucToiDaMoiHoSo` | số | có | 1–20. Số lịch sắp tới còn hiệu lực tối đa của 1 người khám (theo hồ sơ CCCD) (mặc định 3) |
| `soLichHieuLucToiDaMoiSoDienThoai` | số | có | 1–50, **không nhỏ hơn** `soLichHieuLucToiDaMoiHoSo`. Số lịch sắp tới tối đa đặt bằng cùng 1 SĐT liên hệ (mặc định 5) |
| `huyDoiTruocToiThieuPhut` | số | có | 0–10080. Bệnh nhân chỉ hủy / đổi được khi còn cách giờ khám ít nhất chừng này phút (mặc định 120); quản trị viên không bị giới hạn |
| `soLanDoiLichToiDa` | số | có | 0–10. Số lần bệnh nhân đổi lịch tối đa của 1 lần đặt (mặc định 2); quản trị viên không bị giới hạn |
| `guiYeuCauTruocToiThieuGio` | số | có | 0–168. Bác sĩ chỉ gửi được yêu cầu đổi ca / xin nghỉ / nhường ca khi ca còn cách giờ bắt đầu ít nhất chừng này giờ; giờ mong muốn của đổi ca cũng vậy (mặc định 24) |
| `soNgayXepTruocToiDa` | số | có | 7–365. Quản trị viên chỉ xếp được ca trong số ngày tới này (mặc định 90) |
| `soGioToiDaMoiTuan` | số | có | 1–168. Tổng giờ các ca còn hoạt động của 1 bác sĩ trong 1 tuần thứ Hai – Chủ nhật (mặc định 48) |
| `soNgayDuocSuaDanhGia` | số | có | 0–60. Bệnh nhân sửa được đánh giá trong số ngày này kể từ lúc gửi (mặc định 7) |
| `phienBan` | số | có | `phienBan` của lần `GET` |

**Response:** `duLieu` = [`CauHinhQuanTriResponse`](#cauhinhquantriresponse) sau khi lưu.

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (ngoài khoảng, số lịch mỗi SĐT nhỏ hơn số lịch mỗi hồ sơ); 409 `DU_LIEU_DA_THAY_DOI`
(quản trị viên khác đã lưu trước: tải lại form).

#### GET /api/booking/quan-tri/cau-hinh/lich-su

**Quyền:** QUAN_TRI_VIEN. Lịch sử sửa quy tắc, mới nhất trước. 1 lần lưu đổi 3 trường cho 3 dòng cùng `thoiDiem`.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của `{ id, truong, giaTriCu, giaTriMoi, thoiDiem, quanTri }`. `truong` là tên
trường như trong `CauHinhQuanTriResponse` (vd `soGioToiDaMoiTuan`); `giaTriCu`, `giaTriMoi` là chuỗi; `quanTri`:
`{ id, hoTen }`.

```json
{ "id": 17, "truong": "soGioToiDaMoiTuan", "giaTriCu": "40", "giaTriMoi": "48", "thoiDiem": "2026-10-18T15:20:00",
  "quanTri": { "id": 1, "hoTen": "Quản trị viên" } }
```

### 3.4 Ngày nghỉ của bệnh viện

Ngày nghỉ (Tết, lễ, nghỉ đột xuất) **hủy mọi ca còn hoạt động** của ngày đó theo quy trình "cần đổi lịch" (như
[`POST …/lich-lam-viec/{id}/huy`](#post-apibookingquan-trilich-lam-viecidhuy)): lịch hẹn giữ nguyên, được đánh dấu
`canDoiLich`, bệnh nhân được báo và nhận email; mỗi bác sĩ bị hủy ca được báo 1 lần. Sau đó không xếp được ca vào ngày
nghỉ (`NGAY_BENH_VIEN_NGHI`). Trang đặt lịch hiện ngày nghỉ qua route công khai `GET /api/booking/ngay-nghi`.

#### GET /api/booking/quan-tri/ngay-nghi

**Quyền:** QUAN_TRI_VIEN. Ngày nghỉ trong khoảng, theo ngày, kèm cả 2 bản lý do.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuNgay` | ngày | không | mặc định hôm nay |
| `denNgay` | ngày | không | mặc định `tuNgay` + 365 ngày; khoảng tối đa 366 ngày |

**Response:** `duLieu` = mảng `{ id, ngay, lyDo, lyDoEn, quanTriTao, ngayTao }` (không phân trang). `lyDoEn` null = chưa
nhập bản tiếng Anh; `quanTriTao`: `{ id, hoTen }`.

#### POST /api/booking/quan-tri/ngay-nghi/xem-truoc

**Quyền:** QUAN_TRI_VIEN. Xem trước khai báo: chạy **y như khi lưu rồi hoàn tác**, không lưu gì, không báo ai. Dùng để
hiện "sẽ hủy N ca, M lịch hẹn cần đổi" trước khi xác nhận.

**Request:** như [`POST /api/booking/quan-tri/ngay-nghi`](#post-apibookingquan-tringay-nghi).

**Response:** `duLieu` = kết quả như `POST`, với `daLuu = false` và `ngayMoi[].id = null`.

**Lỗi:** như `POST`.

#### POST /api/booking/quan-tri/ngay-nghi

**Quyền:** QUAN_TRI_VIEN. Khai báo bệnh viện nghỉ [`tuNgay`, `denNgay`]. Ngày đã nghỉ từ trước được bỏ qua.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuNgay` | ngày | có | từ **ngày mai** trở đi |
| `denNgay` | ngày | có | không trước `tuNgay`; khoảng tối đa 42 ngày |
| `lyDo` | chuỗi | có | tối đa 200; hiện cho bệnh nhân trên trang đặt lịch và trong thông báo |
| `lyDoEn` | chuỗi | không | tối đa 200; bỏ trống thì người đọc tiếng Anh thấy câu tiếng Việt |

```json
{ "tuNgay": "2027-02-05", "denNgay": "2027-02-11", "lyDo": "Nghỉ Tết Nguyên đán", "lyDoEn": "Lunar New Year holiday" }
```

**Response:** HTTP 201.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `daLuu` | boolean | `false` = xem trước |
| `ngayMoi` | mảng | các ngày vừa thành ngày nghỉ: `{ id, ngay, lyDo, lyDoEn, quanTriTao, ngayTao }` (`id` null khi xem trước) |
| `ngayDaNghi` | mảng ngày | các ngày đã nghỉ từ trước: bỏ qua, không đổi gì |
| `caBiHuy` | mảng | ca còn hoạt động của các ngày mới, bị hủy: `{ idLichLamViec, ngay, gioBatDau, gioKetThuc, bacSi, phongKham, soLichHenCanDoi }` |
| `soLichHenCanDoi` | số | tổng số lịch hẹn của các ca đó: được đánh dấu `canDoiLich` (xử lý ở `GET …/lich-lam-viec/can-doi-lich`) |

```json
{
  "daLuu": true,
  "ngayMoi": [
    { "id": 9, "ngay": "2027-02-06", "lyDo": "Nghỉ Tết Nguyên đán", "lyDoEn": "Lunar New Year holiday",
      "quanTriTao": { "id": 1, "hoTen": "Quản trị viên" }, "ngayTao": "2026-10-21T10:00:00" } ],
  "ngayDaNghi": [ "2027-02-05" ],
  "caBiHuy": [
    { "idLichLamViec": 301, "ngay": "2027-02-06", "gioBatDau": "08:00:00", "gioKetThuc": "11:00:00",
      "bacSi": { "id": 7, "hoTen": "Trần Thị Bình", "hocVi": "ThS.BS", "anhDaiDien": null },
      "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" }, "soLichHenCanDoi": 2 } ],
  "soLichHenCanDoi": 2
}
```

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (`tuNgay` từ hôm nay trở về trước, khoảng sai hoặc quá 42 ngày); 409
`NGAY_BENH_VIEN_NGHI` (mọi ngày trong khoảng đã là ngày nghỉ).

#### DELETE /api/booking/quan-tri/ngay-nghi/{ngay}

**Quyền:** QUAN_TRI_VIEN. Bỏ 1 ngày nghỉ chưa tới. Các ca đã bị hủy khi khai báo **vẫn hủy**; ngày đó xếp ca lại được.

**Request:** path `ngay` dạng `yyyy-MM-dd`.

**Response:** `duLieu` = `null`.

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (hôm nay hoặc đã qua); 404 `KHONG_TIM_THAY` (ngày đó không phải ngày nghỉ).

### 3.5 Ca làm việc: xem, xếp, sửa, hủy

Quy tắc chung khi tạo ca (xếp, lặp lại, sao chép, nhập Excel, duyệt đổi ca):
- ca bắt đầu sau thời điểm hiện tại và trong `soNgayXepTruocToiDa` ngày tới (mặc định 90);
- `gioKetThuc` sau `gioBatDau`, đủ dài cho ít nhất 1 lượt; N × t ≤ 60 (N = `soLuotToiDaMoiGio`, t = `thoiLuongLuotPhut`);
- bác sĩ đang công tác, phòng đang hoạt động và **cùng chuyên khoa** với bác sĩ;
- không trùng giờ với ca còn hoạt động khác của bác sĩ hoặc của phòng (409 `TRUNG_LICH_LAM_VIEC`);
- không làm tuần thứ Hai – Chủ nhật của bác sĩ vượt `soGioToiDaMoiTuan` (mặc định 48 giờ, 409 `VUOT_GIO_LAM_TUAN`, thông
  điệp nêu tuần và số giờ);
- không vào ngày bệnh viện nghỉ (409 `NGAY_BENH_VIEN_NGHI`, thông điệp nêu ngày và lý do).

Ca đã bắt đầu hoặc đã hủy thì không sửa / hủy được (409 `CA_KHONG_SUA_DUOC`).

#### GET /api/booking/quan-tri/lich-lam-viec

**Quyền:** QUAN_TRI_VIEN. Lưới ca toàn viện trong khoảng ngày, **kể cả ca đã hủy**, theo ngày rồi giờ bắt đầu.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuNgay`, `denNgay` | ngày | có | khoảng tối đa 42 ngày (lưới tháng 6 tuần) |
| `idChuyenKhoa`, `idBacSi`, `idPhongKham` | số | không | bỏ trống = không lọc |

**Response:** `duLieu` = mảng [`CaLamViecResponse`](#calamviecresponse).

#### POST /api/booking/quan-tri/lich-lam-viec

**Quyền:** QUAN_TRI_VIEN. Xếp 1 ca (sinh các lượt khám), hoặc nhiều ca cùng giờ theo thứ trong tuần khi có `lapLai`. Bác
sĩ được báo.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idBacSi` | số | có | |
| `idPhongKham` | số | có | phòng đang hoạt động, cùng chuyên khoa với bác sĩ |
| `ngay` | ngày | có | ngày của ca (ca đầu tiên khi có `lapLai`) |
| `gioBatDau`, `gioKetThuc` | giờ | có | `HH:mm` hoặc `HH:mm:ss` |
| `soLuotToiDaMoiGio` | số | có | 1–60, N: số lượt tối đa trong 1 khung 1 giờ |
| `thoiLuongLuotPhut` | số | có | 1–60, t: số phút của 1 lượt; N × t ≤ 60 |
| `lapLai` | object | không | bỏ trống = chỉ tạo 1 ca |

`lapLai`:

| Trường | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `cacThu` | mảng số | có | không rỗng; thứ trong tuần theo ISO, mỗi phần tử 1–7 (1 = thứ Hai … 7 = Chủ nhật) |
| `denNgay` | ngày | có | ngày cuối được tạo ca (tính cả ngày này), từ `ngay` trở đi |

```json
{ "idBacSi": 7, "idPhongKham": 2, "ngay": "2026-11-02", "gioBatDau": "08:00", "gioKetThuc": "11:00",
  "soLuotToiDaMoiGio": 4, "thoiLuongLuotPhut": 15, "lapLai": { "cacThu": [1, 3, 5], "denNgay": "2026-11-27" } }
```

**Response:** HTTP 201, `duLieu` = `{ daTao, boQua }`:
- `daTao`: mảng [`CaLamViecResponse`](#calamviecresponse) vừa tạo, theo ngày;
- `boQua`: mảng `{ ngay, lyDo }`: các ngày của phần lặp lại không tạo được (ngày nghỉ, trùng giờ, vượt giờ làm tuần),
  `lyDo` theo ngôn ngữ của request.

**Lỗi:**
- 400 `DU_LIEU_KHONG_HOP_LE`: ca đã qua / quá `soNgayXepTruocToiDa` ngày, giờ kết thúc không sau giờ bắt đầu, ca quá ngắn,
  N × t > 60, `lapLai.denNgay` trước `ngay`, bác sĩ ngừng công tác, phòng ngừng hoạt động / khác chuyên khoa;
- 404 `KHONG_TIM_THAY`: không có bác sĩ / phòng;
- 409 không lặp lại: `TRUNG_LICH_LAM_VIEC`, `VUOT_GIO_LAM_TUAN`, `NGAY_BENH_VIEN_NGHI`. Có lặp lại thì các ngày đó vào
  `boQua`; không tạo được ngày nào thì 409 `TRUNG_LICH_LAM_VIEC`.

#### PUT /api/booking/quan-tri/lich-lam-viec/{id}

**Quyền:** QUAN_TRI_VIEN. Sửa 1 ca chưa bắt đầu. Gửi **đủ 5 giá trị**, giá trị không đổi gửi lại như cũ. Bác sĩ và ngày
không sửa được (hủy ca rồi xếp ca mới).

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idPhongKham` | số | có | đổi được bất cứ lúc nào: lịch hẹn còn hiệu lực mang phòng mới, bệnh nhân được báo |
| `gioBatDau`, `gioKetThuc` | giờ | có | |
| `soLuotToiDaMoiGio` | số | có | 1–60 |
| `thoiLuongLuotPhut` | số | có | 1–60; N × t ≤ 60 |

Giờ và sức chứa chỉ đổi được khi **mọi lượt đã có người đặt vẫn giữ nguyên giờ** bắt đầu / kết thúc. Ca dài thêm mà tuần
vượt giới hạn giờ làm thì bị chặn.

**Response:** `duLieu` = [`CaLamViecResponse`](#calamviecresponse) sau khi sửa.

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (như khi xếp ca); 404 `KHONG_TIM_THAY`; 409 `CA_KHONG_SUA_DUOC`, `CA_CON_LICH_HEN`
(thay đổi làm mất N lượt đã có người đặt), `TRUNG_LICH_LAM_VIEC`, `VUOT_GIO_LAM_TUAN`.

#### POST /api/booking/quan-tri/lich-lam-viec/{id}/huy

**Quyền:** QUAN_TRI_VIEN. Hủy 1 ca chưa bắt đầu. Lịch hẹn còn hiệu lực của ca **giữ nguyên**, được đánh dấu `canDoiLich`,
bệnh nhân được báo để đổi khung giờ hoặc hủy; yêu cầu đang chờ duyệt của ca bị đóng; bác sĩ được báo.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `lyDo` | chuỗi | có | tối đa 500; ghi vào thông báo gửi bác sĩ và bệnh nhân |

**Response:** `duLieu` = `{ ca, soLichHenCanDoi }`: `ca` là [`CaLamViecResponse`](#calamviecresponse) sau khi hủy
(`DA_HUY`), `soLichHenCanDoi` là số lịch hẹn vừa được đánh dấu.

**Lỗi:** 404 `KHONG_TIM_THAY`; 409 `CA_KHONG_SUA_DUOC`.

#### GET /api/booking/quan-tri/lich-lam-viec/{id}/lich-su

**Quyền:** QUAN_TRI_VIEN. Lịch sử xếp / sửa / hủy của 1 ca, cũ nhất trước.

**Request:** path `id` (`idLichLamViec`).

**Response:** `duLieu` = mảng:

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | số | |
| `hanhDong` | `HanhDongCa` | |
| `nguon` | `NguonThayDoiCa` | |
| `thoiDiem` | ngày giờ | |
| `quanTri` | object \| null | `{ id, hoTen }` người làm |
| `idYeuCau` | số \| null | yêu cầu được duyệt, khi `nguon = DUYET_YEU_CAU` |
| `lyDo` | chuỗi \| null | lý do hủy (câu quản trị viên gõ giữ nguyên văn) |
| `truoc`, `sau` | object \| null | ảnh ca `{ bacSi, phongKham, ngay, gioBatDau, gioKetThuc, soLuotToiDaMoiGio, thoiLuongLuotPhut }`; `truoc` null ở dòng `TAO`; `sau` ở dòng `HUY` là ca lúc bị hủy |
| `cacTruongDoi` | mảng chuỗi | tên các trường của ảnh ca khác giữa `truoc` và `sau` (vd `["phongKham"]`); rỗng ở dòng `TAO` / `HUY` |

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### GET /api/booking/quan-tri/lich-lam-viec/{id}/lich-hen

**Quyền:** QUAN_TRI_VIEN. Lịch hẹn của 1 ca (mọi trạng thái) theo giờ khám, kèm thông tin bệnh nhân.

**Request:** path `id` (`idLichLamViec`).

**Response:** `duLieu` = mảng [`LichHenTrongCaResponse`](#lichhentrongcaresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### GET /api/booking/quan-tri/lich-lam-viec/gio-lam-theo-tuan

**Quyền:** QUAN_TRI_VIEN. Giờ làm của từng bác sĩ theo tuần thứ Hai – Chủ nhật: chỉ cộng ca còn hoạt động, kể cả ca đã
qua. Khoảng ngày được nới ra đủ các tuần chứa `tuNgay` và `denNgay`.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuNgay`, `denNgay` | ngày | có | khoảng tối đa 42 ngày |
| `idChuyenKhoa`, `idBacSi` | số | không | bỏ trống = không lọc |

**Response:**

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `gioiHanPhut` | số | giới hạn mỗi tuần (phút) theo `soGioToiDaMoiTuan` |
| `bacSi` | mảng | bác sĩ đang công tác khớp bộ lọc (kể cả chưa có ca) và bác sĩ khác còn ca trong khoảng: `{ bacSi, tenChuyenKhoa, cacTuan }` |
| `bacSi[].cacTuan` | mảng | mọi tuần của khoảng, theo thời gian: `{ tuNgay (thứ Hai), denNgay (Chủ nhật), soCa, soPhut, soPhutConLai }`; `soPhutConLai` không âm (tuần đã vượt trả 0) |

```json
{ "gioiHanPhut": 2880,
  "bacSi": [ { "bacSi": { "id": 7, "hoTen": "Trần Thị Bình", "hocVi": "ThS.BS", "anhDaiDien": null },
    "tenChuyenKhoa": "Nội tổng quát",
    "cacTuan": [ { "tuNgay": "2026-11-02", "denNgay": "2026-11-08", "soCa": 6, "soPhut": 1080, "soPhutConLai": 1800 } ] } ] }
```

#### GET /api/booking/quan-tri/lich-lam-viec/can-doi-lich

**Quyền:** QUAN_TRI_VIEN. Lịch hẹn còn hiệu lực đang chờ đổi vì ca khám bị hủy (`canDoiLich = true`), giờ khám cũ sớm
nhất trước. Xử lý từng lịch bằng `POST …/lich-hen/{id}/doi-lich` hoặc `…/huy` ([mục 3.8](#38-lịch-hẹn-toàn-viện-và-đặt-qua-tổng-đài)).

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của [`LichHenTrongCaResponse`](#lichhentrongcaresponse).

### 3.6 Sao chép, Excel, PDF, gửi lịch

Các route tải tệp (Excel, PDF) trả tệp **không bọc khung**, tên tệp ở `Content-Disposition`. Gọi bằng `fetch` kèm token
rồi tải `blob` (thẻ `<a href>` không gửi được header `Authorization`). Lỗi vẫn trả JSON như các route khác. Chỉ có ca
**còn hoạt động** (ca đã hủy không xuất / không in).

#### POST /api/booking/quan-tri/lich-lam-viec/sao-chep/xem-truoc

**Quyền:** QUAN_TRI_VIEN. Xem trước sao chép: chạy **y như khi sao chép rồi hoàn tác**, trả các ca sẽ tạo và các ca bị
bỏ qua kèm lý do. Không lưu gì, không báo ai.

**Request:** như [`POST /api/booking/quan-tri/lich-lam-viec/sao-chep`](#post-apibookingquan-trilich-lam-viecsao-chep).

**Response:** `duLieu` = kết quả như `sao-chep`, với `daLuu = false` và `taoDuoc[].idCaMoi = null`.

**Lỗi:** như `sao-chep`.

#### POST /api/booking/quan-tri/lich-lam-viec/sao-chep

**Quyền:** QUAN_TRI_VIEN. Sao chép các ca còn hoạt động của khoảng nguồn sang khoảng bắt đầu từ `ngayDich`, giữ nguyên thứ
trong tuần, phòng, giờ, N, t. Dán `soLan` lần; lần sau cách lần trước số tuần đủ phủ khoảng nguồn. Mỗi bác sĩ được báo 1
lần.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuNgay` | ngày | có | ngày đầu của khoảng nguồn |
| `denNgay` | ngày | có | ngày cuối của khoảng nguồn (tính cả ngày này); khoảng tối đa 42 ngày |
| `ngayDich` | ngày | có | ngày nhận bản sao của `tuNgay`: **sau** `denNgay` và **cùng thứ** với `tuNgay` |
| `soLan` | số | có | 1–12, số lần dán khoảng nguồn |
| `idChuyenKhoa` | số | không | bỏ trống = mọi chuyên khoa |
| `cacIdBacSi` | mảng số | không | bỏ trống / rỗng = mọi bác sĩ |

```json
{ "tuNgay": "2026-11-02", "denNgay": "2026-11-08", "ngayDich": "2026-11-09", "soLan": 3, "idChuyenKhoa": 1 }
```

Bản sao bị **bỏ qua kèm lý do** (không làm hỏng cả lần sao chép) khi: ngày đã qua hoặc quá `soNgayXepTruocToiDa`, ngày
bệnh viện nghỉ, bác sĩ ngừng công tác, phòng ngừng hoạt động / khác chuyên khoa, trùng giờ, vượt giờ làm tuần.

**Response:** HTTP 201.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `daLuu` | boolean | `false` = xem trước |
| `soCaNguon` | số | số ca còn hoạt động trong khoảng nguồn khớp bộ lọc |
| `taoDuoc` | mảng | bản sao tạo được, theo ngày, giờ, tên bác sĩ |
| `boQua` | mảng | bản sao không tạo được, có `lyDo` |

Phần tử của `taoDuoc` / `boQua`: `{ idCaNguon, idCaMoi, bacSi, phongKham, ngay, gioBatDau, gioKetThuc, lyDo }`.
`idCaMoi` null khi xem trước và trong `boQua`; `lyDo` null trong `taoDuoc`, theo ngôn ngữ của request trong `boQua`.

```json
{ "daLuu": true, "soCaNguon": 2,
  "taoDuoc": [ { "idCaNguon": 55, "idCaMoi": 402, "bacSi": { "id": 7, "hoTen": "Trần Thị Bình", "hocVi": "ThS.BS", "anhDaiDien": null },
    "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" }, "ngay": "2026-11-09",
    "gioBatDau": "08:00:00", "gioKetThuc": "11:00:00", "lyDo": null } ],
  "boQua": [ { "idCaNguon": 56, "idCaMoi": null, "bacSi": { "id": 7, "hoTen": "Trần Thị Bình", "hocVi": "ThS.BS", "anhDaiDien": null },
    "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" }, "ngay": "2026-11-11",
    "gioBatDau": "13:00:00", "gioKetThuc": "16:00:00", "lyDo": "Bác sĩ đã có ca làm việc trùng giờ" } ] }
```

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE`: khoảng nguồn sai / quá 42 ngày, `ngayDich` không sau `denNgay` hoặc khác thứ với
`tuNgay`, khoảng nguồn không có ca nào khớp bộ lọc, số ca nguồn × `soLan` quá 1000, hoặc (khi lưu) không bản sao nào tạo
được.

#### GET /api/booking/quan-tri/lich-lam-viec/xuat-excel

**Quyền:** QUAN_TRI_VIEN. Tải tệp Excel các ca còn hoạt động trong khoảng ngày. Cột A–G cùng bố cục với tệp nhập (sửa rồi
nhập lại được); các cột sau G (họ tên, chuyên khoa, phòng, tầng, số lượt đã đặt / tổng) chỉ để xem.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuNgay`, `denNgay` | ngày | có | khoảng tối đa 42 ngày |
| `idChuyenKhoa`, `idBacSi`, `idPhongKham` | số | không | như `GET …/lich-lam-viec` |

**Response:** tệp `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`,
`lich-lam-viec_<tuNgay>_<denNgay>.xlsx`. Tiêu đề cột, tên trang theo `Accept-Language`.

**Lỗi:** JSON: 400 `DU_LIEU_KHONG_HOP_LE` (khoảng ngày sai).

#### GET /api/booking/quan-tri/lich-lam-viec/nhap-excel/mau

**Quyền:** QUAN_TRI_VIEN. Tải tệp Excel mẫu để nhập lịch: trang ca (chỉ có tiêu đề), trang hướng dẫn, danh sách bác sĩ
đang công tác và phòng đang hoạt động; cột mã bác sĩ / mã phòng có danh sách chọn. Quy tắc từng cột nằm ở trang hướng dẫn,
frontend không cần biết.

**Request:** Không có.

**Response:** tệp `.xlsx` (cùng content type như trên), `mau-nhap-lich-lam-viec.xlsx`.

#### POST /api/booking/quan-tri/lich-lam-viec/nhap-excel/xem-truoc

**Quyền:** QUAN_TRI_VIEN. Kiểm tra tệp như khi nhập, trả các dòng hợp lệ và lỗi theo dòng / cột. Không lưu gì.

| Trường (multipart) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tep` | tệp | có | `.xlsx` (không nhận `.xls`), tối đa 2 MB, tối đa 1000 dòng ca; đọc theo vị trí cột ở trang đầu tiên, dòng 1 là tiêu đề |

Gửi `multipart/form-data`, không tự đặt header `Content-Type` (trình duyệt tự thêm `boundary`).

**Response:**

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `daLuu` | boolean | `false` = xem trước (hoặc tệp còn lỗi) |
| `soDong` | số | số dòng ca trong tệp (không tính tiêu đề, dòng trống) |
| `hopLe` | mảng | dòng không lỗi, theo thứ tự dòng: `{ dong, idCaMoi, bacSi, phongKham, ngay, gioBatDau, gioKetThuc }`; `idCaMoi` null khi chưa lưu |
| `loi` | mảng | lỗi theo dòng rồi theo cột: `{ dong, cot, lyDo }`. `dong` là số dòng trong Excel; `cot` là chữ cái A–G, null khi lỗi cả dòng (trùng giờ, vượt giờ làm tuần…); `lyDo` theo ngôn ngữ của request. 1 dòng có thể có nhiều lỗi ô |

```json
{ "daLuu": false, "soDong": 2,
  "hopLe": [ { "dong": 2, "idCaMoi": null, "bacSi": { "id": 7, "hoTen": "Trần Thị Bình", "hocVi": "ThS.BS", "anhDaiDien": null },
    "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" }, "ngay": "2026-11-16",
    "gioBatDau": "08:00:00", "gioKetThuc": "11:00:00" } ],
  "loi": [ { "dong": 3, "cot": "A", "lyDo": "Không có bác sĩ mã BS0099" } ] }
```

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (tệp rỗng, hỏng hoặc không phải `.xlsx`, không có dòng ca nào, quá 1000 dòng); 413
`TEP_QUA_LON` (quá 2 MB).

#### POST /api/booking/quan-tri/lich-lam-viec/nhap-excel

**Quyền:** QUAN_TRI_VIEN. Nhập ca từ tệp, **tất cả hoặc không**: chỉ lưu khi không còn lỗi nào. Nên gọi `xem-truoc` trước.
Mỗi bác sĩ được báo 1 lần.

**Request:** như `nhap-excel/xem-truoc` (phần `tep`).

**Response:** HTTP 201, `duLieu` = kết quả như `xem-truoc`, `daLuu = true`, `hopLe[].idCaMoi` có giá trị, `loi` rỗng.

**Lỗi:** như `xem-truoc`; tệp còn lỗi thì 400 `DU_LIEU_KHONG_HOP_LE` ("Tệp còn N lỗi…"), không ca nào được lưu (lấy chi
tiết bằng `xem-truoc`).

#### GET /api/booking/quan-tri/lich-lam-viec/pdf/bac-si/{idBacSi}

**Quyền:** QUAN_TRI_VIEN. Bản in PDF lịch làm việc của 1 bác sĩ (A4 dọc), cùng tệp bác sĩ tự tải được. Không có ca thì vẫn
có tệp, ghi rõ không có ca.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuNgay`, `denNgay` | ngày | có | khoảng tối đa 42 ngày |

**Response:** tệp `application/pdf`, `lich-lam-viec_<maBacSi>_<tuNgay>_<denNgay>.pdf`; chữ theo `Accept-Language`.

**Lỗi:** JSON: 400 `DU_LIEU_KHONG_HOP_LE` (khoảng ngày sai); 404 `KHONG_TIM_THAY` (không có bác sĩ).

#### GET /api/booking/quan-tri/lich-lam-viec/pdf/toan-vien

**Quyền:** QUAN_TRI_VIEN. Bản in lưới tuần toàn viện (A4 ngang): mỗi tuần thứ Hai – Chủ nhật chạm khoảng ngày là 1 trang,
mỗi dòng 1 bác sĩ có ca (theo chuyên khoa), cột cuối là giờ làm trong tuần.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuNgay`, `denNgay` | ngày | có | khoảng tối đa 42 ngày |
| `idChuyenKhoa` | số | không | bỏ trống = mọi chuyên khoa |

**Response:** tệp `application/pdf`, `lich-lam-viec-toan-vien_<tuNgay>_<denNgay>.pdf`.

**Lỗi:** JSON: 400 `DU_LIEU_KHONG_HOP_LE` (khoảng ngày sai).

#### POST /api/booking/quan-tri/lich-lam-viec/gui-lich

**Quyền:** QUAN_TRI_VIEN. Gửi lịch làm việc: mỗi bác sĩ có ca còn hoạt động **từ hôm nay** trong khoảng nhận 1 email (bảng
các ca) và 1 thông báo trong ứng dụng. Gửi lại cùng khoảng thì gửi thêm lần nữa.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuNgay`, `denNgay` | ngày | có | khoảng tối đa 42 ngày; ca trước hôm nay không gửi |
| `idChuyenKhoa` | số | không | bỏ trống = mọi chuyên khoa |
| `cacIdBacSi` | mảng số | không | tối đa 200 id; bỏ trống / rỗng = mọi bác sĩ |

**Response:** `duLieu` = `{ soBacSi, soCa, cacBacSi }`: số bác sĩ được gửi, tổng số ca trong các email, và từng bác sĩ
`{ bacSi, soCa, soPhut }` ([`BacSiTomTatResponse`](#bacsitomtatresponse), tổng phút các ca đã gửi).

```json
{ "soBacSi": 1, "soCa": 6,
  "cacBacSi": [ { "bacSi": { "id": 7, "hoTen": "Trần Thị Bình", "hocVi": "ThS.BS", "anhDaiDien": null }, "soCa": 6, "soPhut": 1080 } ] }
```

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (khoảng ngày sai / quá 42 ngày, không có ca nào để gửi).

### 3.7 Yêu cầu đổi ca / xin nghỉ / nhường ca

Bác sĩ gửi yêu cầu cho 1 ca của mình. Vòng đời: `CHO_DUYET` → `DA_DUYET` / `TU_CHOI` (quản trị viên) hoặc `DA_RUT` (bác sĩ
rút). Nhường ca bắt đầu ở `CHO_DONG_Y`: bác sĩ nhận đồng ý thì thành `CHO_DUYET`, từ chối thì `DONG_NGHIEP_TU_CHOI`. Mỗi ca
có tối đa 1 yêu cầu đang chờ. Xin nghỉ nhiều ca gửi 1 lần thành nhiều yêu cầu `XIN_NGHI` chung `maNhom`: xử lý từng yêu cầu
hoặc cả nhóm. Bác sĩ được báo kết quả.

#### GET /api/booking/quan-tri/yeu-cau-doi-lich

**Quyền:** QUAN_TRI_VIEN. Yêu cầu của mọi bác sĩ, **gửi sớm nhất trước**.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `trangThai` | `TrangThaiYeuCau` | không | mặc định `CHO_DUYET` |
| `tatCa` | boolean | không | `true` = mọi trạng thái (bỏ qua `trangThai`); mặc định `false` |
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của [`YeuCauDoiLichResponse`](#yeucaudoilichresponse).

#### POST /api/booking/quan-tri/yeu-cau-doi-lich/{id}/duyet

**Quyền:** QUAN_TRI_VIEN. Duyệt 1 yêu cầu `CHO_DUYET`:
- `XIN_NGHI`: hủy ca;
- `DOI_CA`: chỉ khác phòng thì đổi phòng của ca tại chỗ; còn lại hủy ca cũ và xếp ca mới theo ngày / giờ / phòng mong muốn
  (giữ sức chứa của ca cũ);
- `NHUONG_CA`: chuyển ca (giữ id, phòng, giờ và các lịch hẹn) sang bác sĩ nhận; bệnh nhân được báo bác sĩ mới.

Lịch hẹn của ca bị hủy được đánh dấu `canDoiLich` như khi hủy ca. Ca mới / bác sĩ nhận trùng giờ hoặc vượt giờ làm tuần
thì báo lỗi và **không có gì thay đổi**.

| Trường (body, không bắt buộc gửi body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `ghiChu` | chuỗi | không | tối đa 1000; bác sĩ đọc được (`ghiChuXuLy`) |

**Response:** `duLieu` = [`YeuCauDoiLichResponse`](#yeucaudoilichresponse) sau khi duyệt (`DA_DUYET`).

**Lỗi:** 404 `KHONG_TIM_THAY`; 409 `YEU_CAU_DA_XU_LY` (không còn chờ duyệt), `YEU_CAU_CHO_DONG_NGHIEP` (nhường ca chưa được
bác sĩ nhận đồng ý), `CA_KHONG_SUA_DUOC` (ca đã bắt đầu / đã hủy), `TRUNG_LICH_LAM_VIEC`, `VUOT_GIO_LAM_TUAN`,
`NGAY_BENH_VIEN_NGHI` (đổi ca sang ngày đã thành ngày nghỉ); 400 `DU_LIEU_KHONG_HOP_LE` (bác sĩ nhận không còn hợp lệ, vd
ngừng công tác).

#### POST /api/booking/quan-tri/yeu-cau-doi-lich/{id}/tu-choi

**Quyền:** QUAN_TRI_VIEN. Từ chối 1 yêu cầu (`TU_CHOI`); ca giữ nguyên.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `ghiChu` | chuỗi | có điều kiện | **bắt buộc** khi từ chối (thiếu → 400); tối đa 1000 |

**Response:** `duLieu` = [`YeuCauDoiLichResponse`](#yeucaudoilichresponse) sau khi từ chối.

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (thiếu `ghiChu`); 404 `KHONG_TIM_THAY`; 409 `YEU_CAU_DA_XU_LY`, `YEU_CAU_CHO_DONG_NGHIEP`.

#### POST /api/booking/quan-tri/yeu-cau-doi-lich/nhom/{maNhom}/duyet

**Quyền:** QUAN_TRI_VIEN. Duyệt mọi yêu cầu còn `CHO_DUYET` của 1 nhóm xin nghỉ nhiều ca (hủy từng ca). Ca đã bắt đầu thì
bỏ qua (yêu cầu vẫn `CHO_DUYET`) và liệt kê; bác sĩ được báo 1 lần.

**Request:** path `maNhom` (từ `YeuCauDoiLichResponse.maNhom`); body `{ "ghiChu": "..." }` không bắt buộc (tối đa 1000).

**Response:** `duLieu` = `{ daXuLy, boQua }`: `daXuLy` là mảng [`YeuCauDoiLichResponse`](#yeucaudoilichresponse) vừa
duyệt theo ngày của ca; `boQua` là mảng `{ idYeuCau, lyDo }`.

```json
{ "daXuLy": [ { "id": 52, "loaiYeuCau": "XIN_NGHI", "trangThai": "DA_DUYET", "lyDo": "Nghỉ phép",
    "bacSi": { "id": 7, "hoTen": "Trần Thị Bình", "hocVi": "ThS.BS", "anhDaiDien": null },
    "ca": { "idLichLamViec": 70, "ngay": "2026-11-03", "gioBatDau": "08:00:00", "gioKetThuc": "11:00:00", "soLuotToiDaMoiGio": 4,
      "thoiLuongLuotPhut": 15, "trangThai": "DA_HUY", "bacSi": { "id": 7, "hoTen": "Trần Thị Bình", "hocVi": "ThS.BS", "anhDaiDien": null },
      "tenChuyenKhoa": "Nội tổng quát", "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" },
      "tongSoLuot": 0, "soLuotDaDat": 0, "soLuotConTrong": 0 },
    "ngayMongMuon": null, "gioBatDauMongMuon": null, "gioKetThucMongMuon": null, "phongKhamMongMuon": null,
    "bacSiNhan": null, "ghiChuBacSiNhan": null, "ngayBacSiNhanPhanHoi": null,
    "maNhom": "3f1c2a9e-8d7b-4c61-9a0e-5b2f6d4e8c11", "ghiChuXuLy": null,
    "ngayGui": "2026-10-20T08:00:00", "ngayXuLy": "2026-10-21T10:30:00" } ],
  "boQua": [ { "idYeuCau": 51, "lyDo": "Ca làm việc đã bắt đầu hoặc đã hủy, không sửa / hủy / gửi yêu cầu được" } ] }
```

**Lỗi:** 404 `KHONG_TIM_THAY` (không có nhóm); 409 `YEU_CAU_DA_XU_LY` (không còn yêu cầu nào chờ duyệt),
`CA_KHONG_SUA_DUOC` (mọi ca còn chờ đều đã bắt đầu).

#### POST /api/booking/quan-tri/yeu-cau-doi-lich/nhom/{maNhom}/tu-choi

**Quyền:** QUAN_TRI_VIEN. Từ chối mọi yêu cầu còn `CHO_DUYET` của 1 nhóm; bác sĩ được báo 1 lần.

**Request:** path `maNhom`; body `{ "ghiChu": "..." }`, `ghiChu` **bắt buộc** (tối đa 1000).

**Response:** `duLieu` = `{ daXuLy, boQua }` như duyệt nhóm (`boQua` luôn rỗng).

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (thiếu `ghiChu`); 404 `KHONG_TIM_THAY`; 409 `YEU_CAU_DA_XU_LY`.

### 3.8 Lịch hẹn toàn viện và đặt qua tổng đài

Quản trị viên hủy / đổi lịch thay bệnh nhân **không theo** hạn hủy / đổi (`huyDoiTruocToiThieuPhut`) và số lần đổi
(`soLanDoiLichToiDa`) của bệnh nhân. Chỉ hủy / đổi được lịch `DA_XAC_NHAN` chưa tới giờ khám; lịch đang `canDoiLich` thì
lúc nào cũng được. Dùng `quanTriHuyDoiDuoc` của chi tiết để hiện / ẩn nút.

#### GET /api/booking/quan-tri/lich-hen

**Quyền:** QUAN_TRI_VIEN. Lịch hẹn toàn viện có giờ khám trong [`tuNgay`, `denNgay`], sớm nhất trước.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuNgay` | ngày | không | mặc định hôm nay |
| `denNgay` | ngày | không | mặc định 31 ngày từ `tuNgay`; khoảng tối đa 366 ngày |
| `idChuyenKhoa`, `idBacSi`, `idPhongKham` | số | không | |
| `trangThai` | `TrangThaiLichHen` | không | |
| `kenhDat` | `KenhDatLich` | không | vd `HOTLINE`: lịch đặt qua tổng đài |
| `canDoiLich` | boolean | không | `true`: chỉ lịch chờ đổi vì ca bị hủy |
| `canDoiChieu` | boolean | không | `true`: chỉ lịch có thông tin nhập khác hồ sơ, chưa đối chiếu |
| `tuKhoa` | chuỗi | không | mã `ECL-…` đầy đủ, số CCCD 12 chữ số (người khám hoặc người giám hộ), hoặc 1 phần họ tên / SĐT / email liên hệ |
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

Bộ lọc bỏ trống = không lọc.

**Response:** `duLieu` = `TrangDuLieu` của [`LichHenTrongCaResponse`](#lichhentrongcaresponse).

#### GET /api/booking/quan-tri/lich-hen/tra-cuu

**Quyền:** QUAN_TRI_VIEN. Tìm 1 lịch hẹn bất kỳ theo mã.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `ma` | chuỗi | có | `maTraCuu` (không phân biệt hoa thường), mã phiếu khám, hoặc cả link phiếu khám (nội dung QR, phần `?…` / `#…` bị bỏ) |

**Response:** `duLieu` = [`LichHenTrongCaResponse`](#lichhentrongcaresponse). Mở chi tiết bằng `id`.

**Lỗi:** 404 `KHONG_TIM_THAY` (mã sai dạng hoặc không có).

#### GET /api/booking/quan-tri/lich-hen/{id}

**Quyền:** QUAN_TRI_VIEN. Chi tiết 1 lịch hẹn: email liên hệ, tài khoản đặt, chuỗi đổi lịch, nhật ký thao tác.

**Request:** path `id`.

**Response:** `duLieu` = [`LichHenQuanTriChiTietResponse`](#lichhenquantrichitietresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### POST /api/booking/quan-tri/lich-hen

**Quyền:** QUAN_TRI_VIEN. Đặt lịch thay người gọi tổng đài / người đến quầy: cùng form và quy tắc với khách đặt (khung giờ,
hồ sơ theo CCCD, số lịch tối đa theo hồ sơ và theo SĐT), **không giới hạn theo IP**. Lịch hẹn có `kenhDat = HOTLINE`, nhật
ký `DAT_LICH` ghi quản trị viên. Chọn ca / khung giờ bằng các route công khai của trang đặt lịch.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idLichLamViec` | số | có điều kiện | cách 1, chọn bác sĩ: ca đã chọn |
| `idChuyenKhoa` | số | có điều kiện | cách 2, "bác sĩ bất kỳ": hệ thống xếp vào bác sĩ còn nhiều chỗ nhất. Gửi đúng 1 trong `idLichLamViec` / `idChuyenKhoa` |
| `gioBatDauKhung` | ngày giờ | có | giờ bắt đầu khung 1 giờ đã chọn |
| `benhNhan` | object | có | người khám, bảng dưới |
| `nguoiGiamHo` | object | có điều kiện | bắt buộc khi người khám dưới 18 tuổi tính theo ngày khám; bảng dưới |
| `lyDoKham` | chuỗi | có | tối đa 500 |
| `ngonNgu` | `NgonNgu` | không | ngôn ngữ email phiếu khám gửi người gọi; bỏ trống = `VI` |

`benhNhan` (trường không bắt buộc: bỏ trống hoặc gửi `""`):

| Trường | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `hoTen` | chuỗi | có | tối đa 150 |
| `ngaySinh` | ngày | có | không ở tương lai |
| `gioiTinh` | `GioiTinh` | có | |
| `soDienThoai` | chuỗi | có | `^0\d{9}$`; SĐT liên hệ của lượt khám khi không có người giám hộ |
| `cccd` | chuỗi | có điều kiện | 12 chữ số; bắt buộc với người từ đủ 18 tuổi (theo ngày khám) |
| `email` | chuỗi | không | tối đa 255; nhận phiếu khám, nhắc lịch |
| `soBaoHiemYTe` | chuỗi | không | tối đa 50; chỉ ghi vào hồ sơ khi hồ sơ chưa có |
| `diaChi` | chuỗi | không | tối đa 500; chỉ ghi vào hồ sơ khi hồ sơ chưa có |

`nguoiGiamHo` (bị bỏ qua khi người khám từ đủ 18 tuổi):

| Trường | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `hoTen` | chuỗi | có | tối đa 150 |
| `quanHe` | `QuanHeGiamHo` | có | |
| `soDienThoai` | chuỗi | có | `^0\d{9}$`; thành SĐT liên hệ của lượt khám |
| `cccd` | chuỗi | có | 12 chữ số, khác CCCD người khám |
| `ngaySinh` | ngày | không | ở quá khứ; có gửi thì người giám hộ phải từ đủ 18 tuổi |

```json
{ "idChuyenKhoa": 1, "gioBatDauKhung": "2026-10-25T08:00:00",
  "benhNhan": { "hoTen": "Lê Thị Hoa", "ngaySinh": "1978-09-03", "gioiTinh": "NU", "soDienThoai": "0902000002",
    "cccd": "079178000002" },
  "lyDoKham": "Đau đầu", "ngonNgu": "VI" }
```

**Response:** HTTP 201, `duLieu` = `{ idLichHen, phieuKham }`. Đây là phản hồi **duy nhất** có mã phiếu khám cho quản trị
viên: in phiếu hoặc mở mã QR ngay; sau đó chỉ cấp lại được bằng `cap-lai-lien-ket`.

| Trường của `phieuKham` | Kiểu | Ghi chú |
|---|---|---|
| `maPhieuKham` | chuỗi | mở trang phiếu khám |
| `maTraCuu` | chuỗi | mã ngắn in trên phiếu |
| `linkPhieuKham` | chuỗi | link trang phiếu khám của frontend, cũng là nội dung QR (ảnh QR: `GET /api/booking/phieu-kham/{maPhieuKham}/qr`) |
| `soThuTu` | số | |
| `ngay` | ngày | |
| `gioKhamDuKien`, `gioBatDauKhung`, `gioKetThucKhung` | ngày giờ | |
| `trangThai` | `TrangThaiLichHen` | `DA_XAC_NHAN` |
| `bacSi` | [`BacSiTomTatResponse`](#bacsitomtatresponse) | với "bác sĩ bất kỳ" là bác sĩ hệ thống xếp |
| `phongKham` | [`PhongKhamTomTatResponse`](#phongkhamtomtatresponse) | |
| `hoTenBenhNhan`, `hoTenNguoiGiamHo` | chuỗi, chuỗi \| null | |
| `luuVaoTaiKhoan` | boolean | luôn `false` |
| `bacSiDoPhongKhamXep` | boolean | `true` khi chọn "bác sĩ bất kỳ" |

```json
{ "idLichHen": 913,
  "phieuKham": { "maPhieuKham": "Xk3v9QpL2mR7tY0wZb5nC8dF1gH4jK6sA2eU9iO3pQx", "maTraCuu": "ECL-20261025-5521",
    "linkPhieuKham": "http://localhost:5173/phieu-kham/Xk3v9QpL2mR7tY0wZb5nC8dF1gH4jK6sA2eU9iO3pQx",
    "soThuTu": 4, "ngay": "2026-10-25", "gioKhamDuKien": "2026-10-25T08:45:00",
    "gioBatDauKhung": "2026-10-25T08:00:00", "gioKetThucKhung": "2026-10-25T09:00:00", "trangThai": "DA_XAC_NHAN",
    "bacSi": { "id": 7, "hoTen": "Trần Thị Bình", "hocVi": "ThS.BS", "anhDaiDien": null },
    "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" },
    "hoTenBenhNhan": "Lê Thị Hoa", "hoTenNguoiGiamHo": null, "luuVaoTaiKhoan": false, "bacSiDoPhongKhamXep": true } }
```

**Lỗi:**
- 400 `DU_LIEU_KHONG_HOP_LE` (gửi cả hai / không gửi `idLichLamViec` và `idChuyenKhoa`, khung giờ không thuộc ca, người từ
  đủ 18 tuổi thiếu `cccd`), `THIEU_NGUOI_GIAM_HO`, `NGUOI_GIAM_HO_KHONG_HOP_LE`;
- 404 `KHONG_TIM_THAY` (không có ca);
- 409 `KHUNG_GIO_KHONG_CON_TRONG`, `KHUNG_GIO_KHONG_KHA_DUNG`, `LICH_HEN_TRUNG_GIO`, `VUOT_GIOI_HAN_DAT_LICH`.

#### POST /api/booking/quan-tri/lich-hen/{id}/huy

**Quyền:** QUAN_TRI_VIEN. Hủy lịch hẹn: `DA_XAC_NHAN` → `QUAN_TRI_HUY`, lượt khám được mở lại, bệnh nhân (thông báo +
email) và bác sĩ được báo.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `lyDo` | chuỗi | có | tối đa 500; bệnh nhân đọc được trên phiếu khám và trong thông báo |

**Response:** `duLieu` = [`LichHenQuanTriChiTietResponse`](#lichhenquantrichitietresponse) sau khi hủy.

**Lỗi:** 404 `KHONG_TIM_THAY`; 409 `LICH_HEN_KHONG_HUY_DOI_DUOC` (không phải `DA_XAC_NHAN`), `LICH_HEN_DA_QUA_GIO`.

#### POST /api/booking/quan-tri/lich-hen/{id}/doi-lich

**Quyền:** QUAN_TRI_VIEN. Đổi lịch hẹn sang khung giờ khác thay bệnh nhân: lịch cũ thành `DA_HUY_DO_DOI_LICH`, lịch mới
`DA_XAC_NHAN` với phiếu khám mới, giữ ngôn ngữ và kênh đặt của lịch cũ. Khung giờ mới theo đúng quy tắc đặt lịch. Người đặt
nhận email phiếu mới.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idLichLamViec` | số | có điều kiện | chọn bác sĩ |
| `idChuyenKhoa` | số | có điều kiện | "bác sĩ bất kỳ"; gửi đúng 1 trong 2 |
| `gioBatDauKhung` | ngày giờ | có | giờ bắt đầu khung 1 giờ muốn đổi sang |
| `lyDo` | chuỗi | có | tối đa 500; chỉ ghi vào nhật ký (bệnh nhân không đọc được) |

**Response:** HTTP 201, `duLieu` = [`LichHenQuanTriChiTietResponse`](#lichhenquantrichitietresponse) của **lịch mới**
(`id` mới).

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (thiếu / thừa lựa chọn ca, khung giờ không thuộc ca hoặc trùng khung đang giữ); 404
`KHONG_TIM_THAY`; 409 `LICH_HEN_KHONG_HUY_DOI_DUOC`, `LICH_HEN_DA_QUA_GIO`, `KHUNG_GIO_KHONG_KHA_DUNG`,
`KHUNG_GIO_KHONG_CON_TRONG`, `LICH_HEN_TRUNG_GIO`, `VUOT_GIOI_HAN_DAT_LICH`.

#### POST /api/booking/quan-tri/lich-hen/{id}/da-doi-chieu

**Quyền:** QUAN_TRI_VIEN. Đã đối chiếu giấy tờ: bỏ đánh dấu `doiChieu.canDoiChieu` của lịch hẹn. Gọi lại nhiều lần vẫn 200.
Sửa hồ sơ (nếu cần) ở [`PUT …/ho-so-benh-nhan/{id}`](#put-apibookingquan-triho-so-benh-nhanid).

**Request:** path `id`.

**Response:** `duLieu` = `null`.

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### POST /api/booking/quan-tri/lich-hen/{id}/cap-lai-lien-ket

**Quyền:** QUAN_TRI_VIEN. Cấp lại link phiếu khám (và sổ khám bệnh nếu đã khám) cho người đã mất link, sau khi xem giấy tờ.
Luôn dùng **lịch hẹn hiện hành** của chuỗi đổi lịch (có thể khác `{id}` đã chọn) và giữ mã cũ (link người bệnh đang giữ vẫn
dùng được). Ghi nhật ký `CAP_LAI_LIEN_KET`.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `cccd` | chuỗi | có | 12 chữ số, trên giấy tờ người đến xuất trình: của bệnh nhân hoặc của người giám hộ ghi trên lịch hẹn |
| `cachGui` | `CachCapLai` | có | `EMAIL` hoặc `MAN_HINH` |

**Response:** các trường không có giá trị thì **không xuất hiện**.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `idLichHen`, `maTraCuu` | số, chuỗi | của lịch hẹn hiện hành |
| `cachGui` | `CachCapLai` | |
| `emailDaGui` | chuỗi | chỉ `EMAIL`: địa chỉ đã che, vd `b***@gmail.com` |
| `maPhieuKham`, `lienKetPhieuKham` | chuỗi | chỉ `MAN_HINH`: ảnh QR ở `GET /api/booking/phieu-kham/{maPhieuKham}/qr` |
| `maSoKham`, `lienKetSoKham` | chuỗi | chỉ `MAN_HINH` và lịch hẹn đã khám: ảnh QR ở `GET /api/booking/so-kham/{maSoKham}/qr` |

```json
{ "idLichHen": 845, "maTraCuu": "ECL-20261027-0712", "cachGui": "EMAIL", "emailDaGui": "a***@example.com" }
```

**Lỗi:** 404 `KHONG_TIM_THAY`; 403 `CCCD_KHONG_KHOP`; 409 `KHONG_CO_EMAIL_NHAN` (`EMAIL` mà lịch hẹn không có email: dùng
`MAN_HINH`).

### 3.9 Hồ sơ bệnh nhân và xác minh liên kết

Hồ sơ bệnh nhân được tạo khi đặt lịch, theo CCCD (cả khách không có tài khoản). Khi 1 tài khoản bệnh nhân khai CCCD trùng
hồ sơ có sẵn mà họ tên / SĐT không khớp, hồ sơ chờ quản trị viên xác minh (`CHO_XAC_MINH`) trước khi tài khoản xem được các
lượt khám cũ.

#### GET /api/booking/quan-tri/ho-so-benh-nhan

**Quyền:** QUAN_TRI_VIEN. Danh sách hồ sơ, hồ sơ mới tạo trước. **CCCD chỉ còn 4 số cuối** (số đầy đủ ở `GET …/{id}`).

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuKhoa` | chuỗi | không | chứa trong họ tên, SĐT, số bảo hiểm y tế; đúng 12 chữ số thì còn so khớp hoàn toàn với CCCD |
| `trangThaiLienKet` | `TrangThaiLienKet` | không | |
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của:

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | số | |
| `hoTen`, `ngaySinh`, `gioiTinh`, `soDienThoai` | | `gioiTinh`: `GioiTinh`; SĐT không che |
| `cccdChe` | chuỗi \| null | vd `********4198`; null nếu hồ sơ chưa có CCCD |
| `coTaiKhoan` | boolean | đang gắn với 1 tài khoản (đã liên kết hoặc chờ xác minh) |
| `trangThaiLienKet` | `TrangThaiLienKet` | |
| `soLichHen` | số | mọi trạng thái |
| `ngayTao` | ngày giờ | |

#### GET /api/booking/quan-tri/ho-so-benh-nhan/cho-xac-minh

**Quyền:** QUAN_TRI_VIEN. Hồ sơ đang chờ xác minh liên kết, cũ nhất trước.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của `{ hoSo, taiKhoan, khopHoTen, khopSoDienThoai }`:
- `hoSo`: `{ id, hoTen, ngaySinh, cccd, soDienThoai, soLichHen }` (CCCD không che; `soLichHen`: tài khoản sẽ xem được nếu
  được duyệt);
- `taiKhoan`: `{ id, hoTen, email, soDienThoai }`;
- `khopHoTen`: họ tên giống nhau (không xét dấu, hoa / thường, khoảng trắng thừa); `khopSoDienThoai`: SĐT trùng.

```json
{ "hoSo": { "id": 301, "hoTen": "Nguyễn Văn An", "ngaySinh": "1990-05-12", "cccd": "079090004198", "soDienThoai": "0901234001", "soLichHen": 3 },
  "taiKhoan": { "id": 58, "hoTen": "Nguyen Van An", "email": "an@example.com", "soDienThoai": "0907654321" },
  "khopHoTen": true, "khopSoDienThoai": false }
```

#### GET /api/booking/quan-tri/ho-so-benh-nhan/{id}

**Quyền:** QUAN_TRI_VIEN. Xem 1 hồ sơ để đối chiếu / sửa. **CCCD không che**; không có tiền sử bệnh lý. `id` lấy từ
`idHoSoBenhNhan` của lịch hẹn.

**Request:** path `id`.

**Response:** `duLieu` = `{ id, cccd, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChi, soBaoHiemYTe, trangThaiLienKet,
idTaiKhoan, ngayTao }`. `cccd` null với trẻ chưa có CCCD; `idTaiKhoan` null nếu hồ sơ chưa liên kết.

```json
{ "id": 301, "cccd": "079090004198", "hoTen": "Nguyễn Văn An", "ngaySinh": "1990-05-12", "gioiTinh": "NAM",
  "soDienThoai": "0901234001", "diaChi": "12 Nguyễn Trãi, Q.1, TP.HCM", "soBaoHiemYTe": null,
  "trangThaiLienKet": "DA_LIEN_KET", "idTaiKhoan": 58, "ngayTao": "2026-09-30T14:02:00" }
```

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### GET /api/booking/quan-tri/ho-so-benh-nhan/{id}/lich-hen

**Quyền:** QUAN_TRI_VIEN. Lịch hẹn của 1 hồ sơ, mọi trạng thái, giờ khám muộn nhất trước. Không có chẩn đoán / đơn thuốc.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của [`LichHenTrongCaResponse`](#lichhentrongcaresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### PUT /api/booking/quan-tri/ho-so-benh-nhan/{id}

**Quyền:** QUAN_TRI_VIEN. Sửa hồ sơ sau khi đối chiếu giấy tờ (hồ sơ chưa liên kết tài khoản chỉ sửa được ở đây hoặc bởi
bác sĩ khám). Gửi đủ các trường: trường không bắt buộc gửi trống thì **bị xoá trắng**.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `hoTen` | chuỗi | có | tối đa 150 |
| `ngaySinh` | ngày | có | không ở tương lai |
| `gioiTinh` | `GioiTinh` | không | |
| `soDienThoai` | chuỗi | có | `^0\d{9}$` |
| `cccd` | chuỗi | không | 12 chữ số. Chỉ điền được khi hồ sơ **chưa có** CCCD; hồ sơ đã có thì bỏ trống hoặc gửi đúng số đang lưu |
| `diaChi` | chuỗi | không | tối đa 500 |
| `soBaoHiemYTe` | chuỗi | không | tối đa 50 |

**Response:** `duLieu` = hồ sơ sau khi sửa, như `GET …/ho-so-benh-nhan/{id}`.

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (cả khi đổi CCCD đã có); 404 `KHONG_TIM_THAY`; 409 `XUNG_DOT_DU_LIEU` (CCCD trùng hồ sơ
khác).

#### POST /api/booking/quan-tri/ho-so-benh-nhan/{id}/duyet

**Quyền:** QUAN_TRI_VIEN. Duyệt: hồ sơ thuộc về tài khoản đang chờ (`DA_LIEN_KET`); tài khoản xem được các lượt khám của hồ
sơ.

**Request:** path `id` (`hoSo.id` của danh sách chờ xác minh).

**Response:** `duLieu` = `null`.

**Lỗi:** 404 `KHONG_TIM_THAY`; 409 `XUNG_DOT_DU_LIEU` (hồ sơ không ở trạng thái `CHO_XAC_MINH`).

#### POST /api/booking/quan-tri/ho-so-benh-nhan/{id}/tu-choi

**Quyền:** QUAN_TRI_VIEN. Từ chối: hồ sơ trở lại `CHUA_LIEN_KET`; tài khoản này **không tự liên kết lại** hồ sơ đó được.

| Trường (body, không bắt buộc gửi body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `lyDo` | chuỗi | không | tối đa 500 |

**Response:** `duLieu` = `null`.

**Lỗi:** 404 `KHONG_TIM_THAY`; 409 `XUNG_DOT_DU_LIEU` (hồ sơ không ở trạng thái `CHO_XAC_MINH`).

### 3.10 Đánh giá của bệnh nhân

#### GET /api/booking/quan-tri/danh-gia

**Quyền:** QUAN_TRI_VIEN. Đánh giá của bệnh nhân, mới nhất trước, kèm tên bệnh nhân và bác sĩ.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idBacSi` | số | không | chỉ đánh giá của bác sĩ này |
| `soSaoToiDa` | số | không | 1–5; chỉ đánh giá có số sao ≤ giá trị này (vd 2 để xem đánh giá thấp) |
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của `{ id, soSao, nhanXet, ngayTao, ngayCapNhat, maTraCuu, ngayKham, idHoSoBenhNhan,
tenBenhNhan, idBacSi, tenBacSi }`. `nhanXet` có thể null; `ngayCapNhat` là lần bệnh nhân sửa gần nhất (bằng `ngayTao` nếu
chưa sửa); `maTraCuu` của lịch hẹn.

```json
{ "id": 77, "soSao": 2, "nhanXet": "Chờ lâu", "ngayTao": "2026-10-19T17:20:00", "ngayCapNhat": "2026-10-19T17:20:00",
  "maTraCuu": "ECL-20261019-2210", "ngayKham": "2026-10-19", "idHoSoBenhNhan": 301, "tenBenhNhan": "Nguyễn Văn An",
  "idBacSi": 7, "tenBacSi": "Trần Thị Bình" }
```

## 4. Quản trị danh mục, tài khoản, thuốc và thống kê

### 4.1 Tổng quan

Mọi route của mục 4 cần token quản trị viên (`QUAN_TRI_VIEN`); riêng `GET /api/catalog/phong-kham` bác sĩ cũng gọi được.
Id không có trả 404 `KHONG_TIM_THAY`. Danh sách có phân trang dùng `trang` (từ 0) và `kichThuoc` (1–100, mặc định 20),
trả `TrangDuLieu` (mục 1.3).

**Ba luồng chính**

1. **Thêm bác sĩ mới:**
   - chuyên khoa và phòng khám phải có trước: `POST /api/catalog/chuyen-khoa`, `POST /api/catalog/quan-tri/phong-kham`;
   - `POST /api/catalog/quan-tri/bac-si` tạo tài khoản đăng nhập và hồ sơ bác sĩ trong 1 bước. Bác sĩ đăng nhập bằng
     mật khẩu mặc định rồi phải đặt mật khẩu mới ([`POST /api/auth/first-password`](#post-apiauthfirst-password));
   - điền hồ sơ giới thiệu `PUT …/bac-si/{id}/ho-so`, thêm ảnh `POST …/bac-si/{id}/anh`;
   - xếp ca cho bác sĩ ở [mục 3.5](#35-ca-làm-việc-xem-xếp-sửa-hủy).
2. **Bác sĩ ngừng công tác:**
   - xem trước `GET /api/catalog/quan-tri/bac-si/{id}/anh-huong-ngung-cong-tac` (số ca bị hủy, số lịch hẹn phải đổi);
   - `POST …/bac-si/{id}/ngung-cong-tac`: hủy mọi ca sắp tới, vô hiệu hoá tài khoản, ẩn khỏi trang công khai;
   - lịch hẹn của các ca đó **không bị hủy**: xử lý ở `GET /api/booking/quan-tri/lich-lam-viec/can-doi-lich`
     ([mục 3.5](#35-ca-làm-việc-xem-xếp-sửa-hủy)).
3. **Danh mục thuốc:** bác sĩ chỉ kê được thuốc **đang dùng và đã xác minh**, không tự thêm thuốc mới. Quản trị viên
   thêm thuốc (`POST /api/medical/quan-tri/thuoc`, đã xác minh ngay), xác minh thuốc cũ do bác sĩ thêm trước đây
   (`GET …/thuoc?daXacMinh=false` rồi `POST …/thuoc/{id}/xac-minh`), và ngừng dùng thuốc không còn kê.

**Ngôn ngữ.** Route quản trị trả chữ đã lưu của **cả 2 ngôn ngữ** (trường `…En`, null khi chưa nhập) để sửa; riêng
`tenChuyenKhoa` đi kèm bác sĩ / phòng khám và các tên trong báo cáo theo `Accept-Language`.

**Tất cả route của mục 4**

| Phương thức | Đường dẫn | Quyền | Mục đích |
|---|---|---|---|
| GET | `/api/catalog/quan-tri/chuyen-khoa` | QUAN_TRI_VIEN | Danh mục chuyên khoa (2 ngôn ngữ) |
| POST | `/api/catalog/chuyen-khoa` | QUAN_TRI_VIEN | Thêm chuyên khoa |
| PUT | `/api/catalog/chuyen-khoa/{id}` | QUAN_TRI_VIEN | Sửa chuyên khoa |
| DELETE | `/api/catalog/chuyen-khoa/{id}` | QUAN_TRI_VIEN | Xoá chuyên khoa chưa dùng |
| GET | `/api/catalog/quan-tri/phong-kham` | QUAN_TRI_VIEN | Mọi phòng khám kèm số ca sắp tới |
| POST | `/api/catalog/quan-tri/phong-kham` | QUAN_TRI_VIEN | Thêm phòng khám |
| PUT | `/api/catalog/quan-tri/phong-kham/{id}` | QUAN_TRI_VIEN | Sửa phòng khám |
| POST | `/api/catalog/quan-tri/phong-kham/{id}/ngung-hoat-dong` | QUAN_TRI_VIEN | Cho phòng ngừng hoạt động |
| POST | `/api/catalog/quan-tri/phong-kham/{id}/hoat-dong-lai` | QUAN_TRI_VIEN | Cho phòng hoạt động lại |
| GET | `/api/catalog/phong-kham` | QUAN_TRI_VIEN, BAC_SI | Phòng đang hoạt động (ô chọn phòng) |
| GET | `/api/catalog/quan-tri/bac-si` | QUAN_TRI_VIEN | Danh bạ bác sĩ |
| POST | `/api/catalog/quan-tri/bac-si` | QUAN_TRI_VIEN | Thêm bác sĩ (tài khoản + hồ sơ) |
| GET | `/api/catalog/quan-tri/bac-si/{id}` | QUAN_TRI_VIEN | Hồ sơ đầy đủ của 1 bác sĩ |
| PUT | `/api/catalog/quan-tri/bac-si/{id}` | QUAN_TRI_VIEN | Sửa thông tin cơ bản |
| PUT | `/api/catalog/quan-tri/bac-si/{id}/ho-so` | QUAN_TRI_VIEN | Sửa hồ sơ giới thiệu |
| GET | `/api/catalog/quan-tri/bac-si/{id}/anh-huong-ngung-cong-tac` | QUAN_TRI_VIEN | Xem trước ngừng công tác |
| POST | `/api/catalog/quan-tri/bac-si/{id}/ngung-cong-tac` | QUAN_TRI_VIEN | Cho bác sĩ ngừng công tác |
| POST | `/api/catalog/quan-tri/bac-si/{id}/cong-tac-lai` | QUAN_TRI_VIEN | Cho bác sĩ công tác lại |
| POST | `/api/catalog/quan-tri/bac-si/{id}/anh` | QUAN_TRI_VIEN | Thêm ảnh giới thiệu |
| PUT | `/api/catalog/quan-tri/bac-si/{id}/anh/{idAnh}` | QUAN_TRI_VIEN | Sửa loại / chú thích ảnh |
| PUT | `/api/catalog/quan-tri/bac-si/{id}/anh/thu-tu` | QUAN_TRI_VIEN | Sắp xếp lại ảnh |
| DELETE | `/api/catalog/quan-tri/bac-si/{id}/anh/{idAnh}` | QUAN_TRI_VIEN | Xoá ảnh |
| GET | `/api/users` | QUAN_TRI_VIEN | Danh sách tài khoản |
| GET | `/api/users/{id}` | QUAN_TRI_VIEN | Chi tiết 1 tài khoản |
| PUT | `/api/users/{id}/status` | QUAN_TRI_VIEN | Vô hiệu hoá / kích hoạt lại tài khoản |
| DELETE | `/api/users/{id}` | QUAN_TRI_VIEN | Xoá tài khoản chưa có dữ liệu |
| GET | `/api/medical/quan-tri/thuoc` | QUAN_TRI_VIEN | Danh mục thuốc |
| POST | `/api/medical/quan-tri/thuoc` | QUAN_TRI_VIEN | Thêm thuốc |
| PUT | `/api/medical/quan-tri/thuoc/{id}` | QUAN_TRI_VIEN | Sửa thuốc |
| POST | `/api/medical/quan-tri/thuoc/{id}/xac-minh` | QUAN_TRI_VIEN | Xác minh thuốc |
| POST | `/api/medical/quan-tri/thuoc/{id}/ngung-dung` | QUAN_TRI_VIEN | Ngừng dùng thuốc |
| POST | `/api/medical/quan-tri/thuoc/{id}/dung-lai` | QUAN_TRI_VIEN | Dùng lại thuốc |
| GET | `/api/medical/quan-tri/lich-hen/{idLichHen}/benh-an/lich-su` | QUAN_TRI_VIEN | Lịch sử sửa bệnh án của 1 lượt khám |
| GET | `/api/report/quan-tri/thong-ke` | QUAN_TRI_VIEN | Báo cáo thống kê |
| GET | `/api/report/quan-tri/thong-ke/xuat-excel` | QUAN_TRI_VIEN | Tải báo cáo thống kê dạng Excel |

**Lỗi chung của mục 4** (ngoài mục 1.4): 403 `KHONG_CO_QUYEN` khi token không phải quản trị viên. Route của catalog gọi
sang identity-service / booking-service (thêm bác sĩ, sửa thông tin, ngừng / công tác lại) trả 503 `DICH_VU_NOI_BO_LOI`
khi service kia không trả lời: gửi lại sau.

### 4.2 Mô hình và enum của mục 4

`TrangThaiBacSi`, `TrangThaiTaiKhoan`, `VaiTro`, `TrangThaiLienKet`: xem [mục 1.7](#17-enum-dùng-chung).

| Enum | Giá trị |
|---|---|
| `TrangThaiPhongKham` | `HOAT_DONG`, `NGUNG_HOAT_DONG` (không xếp ca mới vào được, không có trong `GET /api/catalog/phong-kham`) |
| `LoaiAnhBacSi` | `ANH_CONG_VIEC` (bác sĩ cũng tự thêm / sửa / xoá được), `CHUNG_CHI` (chỉ quản trị viên sửa / xoá) |
| `TrangThaiThuoc` | `DANG_DUNG`, `NGUNG_DUNG` (không gợi ý, không kê mới; đơn thuốc cũ vẫn hiện) |
| `DuongDungThuoc` | `UONG`, `NGAM_DUOI_LUOI`, `TIEM`, `BOI_NGOAI_DA`, `NHO_MAT`, `NHO_TAI`, `NHO_MUI`, `XIT_HIT`, `DAT`, `KHAC` (nhãn hiển thị do frontend đặt) |
| `TruongBenhAn` | `LY_DO_KHAM`, `SINH_HIEU`, `KHAM_LAM_SANG`, `CHAN_DOAN` (chữ hoặc mã ICD-10), `CHAN_DOAN_KEM_THEO`, `GHI_CHU`, `NGAY_TAI_KHAM`, `DON_THUOC` (nhãn do frontend đặt) |
| `KyBaoCao` | `NGAY`, `TUAN` (thứ 2 – chủ nhật), `THANG`, `NAM` (kỳ chứa ngày mốc), `TUY_CHON` (từ ngày – đến ngày) |
| `DonViThoiGian` | `NGAY`, `TUAN`, `THANG`: mỗi điểm của `theoThoiGian` dài bao nhiêu |

#### `HoSoBacSiQuanTriResponse`

Hồ sơ đầy đủ của 1 bác sĩ. Các mục giới thiệu trả **chữ đã lưu của cả 2 ngôn ngữ** (trường `…En` là bản tiếng Anh, null /
mảng rỗng khi chưa nhập), không đổi theo `Accept-Language`; riêng `tenChuyenKhoa` theo ngôn ngữ của request.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id`, `maBacSi` | số, chuỗi | `maBacSi` dạng `BS0007` |
| `hoTen`, `email`, `soDienThoai`, `anhDaiDien` | chuỗi \| null | từ tài khoản; họ tên / SĐT sửa ở `PUT …/bac-si/{id}`, email không đổi được ở đây |
| `soGiayPhep` | chuỗi \| null | sửa ở `PUT …/bac-si/{id}` |
| `ngayCapGiayPhep`, `noiCapGiayPhep`, `noiCapGiayPhepEn`, `phamViHanhNghe`, `phamViHanhNgheEn` | ngày / chuỗi \| null | sửa ở `PUT …/ho-so` |
| `trangThai` | `TrangThaiBacSi` | |
| `trangThaiTaiKhoan` | `TrangThaiTaiKhoan` | |
| `phaiDoiMatKhau` | boolean | `true` khi bác sĩ chưa đăng nhập lần đầu để đặt mật khẩu |
| `hocVi`, `chucVu`, `chucVuEn` | chuỗi \| null | |
| `namBatDauHanhNghe`, `soNamKinhNghiem` | số \| null | `soNamKinhNghiem` suy ra từ năm bắt đầu |
| `ngonNguKham` | mảng chuỗi | mã ISO 639 viết thường (`"vi"`, `"en"`); bác sĩ cũng tự sửa |
| `gioiThieuNgan`, `gioiThieuNganEn`, `tieuSu`, `tieuSuEn` | chuỗi \| null | bác sĩ cũng tự sửa |
| `idChuyenKhoa`, `tenChuyenKhoa` | số, chuỗi | |
| `quaTrinhDaoTao(En)`, `quaTrinhCongTac(En)`, `linhVucKhamChua(En)` | mảng chuỗi | chỉ quản trị viên sửa |
| `giaiThuong(En)`, `congTrinhNghienCuu(En)` | mảng chuỗi | bác sĩ cũng tự sửa |
| `phienBanHoSo` | số | gửi lại ở `phienBan` khi lưu `PUT …/ho-so`; +1 mỗi lần lưu (của quản trị viên hoặc bác sĩ) |
| `anh` | mảng [`AnhBacSiQuanTriResponse`](#anhbacsiquantriresponse) | theo thứ tự hiển thị |

#### `AnhBacSiQuanTriResponse`

`{ id, loai, url, chuThich, chuThichEn }`: `loai` là `LoaiAnhBacSi`; chú thích của cả 2 ngôn ngữ, có thể null. Quản trị viên
sửa / xoá được cả ảnh `CHUNG_CHI`.

#### `ChuyenKhoaQuanTriResponse`

`{ id, tenChuyenKhoa, tenChuyenKhoaEn, moTa, moTaEn, trieuChung, trieuChungEn }`: chữ đã lưu của cả 2 ngôn ngữ; mọi trường
trừ `id`, `tenChuyenKhoa` có thể null.

#### `PhongKhamQuanTriResponse`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | số | |
| `tenPhong`, `tenPhongEn`, `tang`, `tangEn` | chuỗi \| null | chữ đã lưu của cả 2 ngôn ngữ; `tenPhong` luôn có |
| `idChuyenKhoa`, `tenChuyenKhoa` | số, chuỗi | `tenChuyenKhoa` theo ngôn ngữ của request |
| `trangThai` | `TrangThaiPhongKham` | |
| `soCaSapToi` | số | ca chưa hủy, chưa bắt đầu của phòng; > 0 thì không đổi chuyên khoa / ngừng hoạt động được |

#### `ThuocQuanTriResponse`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | số | |
| `tenThuoc`, `hoatChat`, `hamLuong` | chuỗi | `hoatChat`, `hamLuong` null ở thuốc cũ chưa xác minh |
| `dangBaoChe`, `dangBaoCheEn`, `donVi`, `donViEn` | chuỗi \| null | chữ đã lưu của cả 2 ngôn ngữ (vd `viên nén`, `viên`) |
| `duongDung` | `DuongDungThuoc` \| null | |
| `moTa`, `moTaEn` | chuỗi \| null | |
| `daXacMinh` | boolean | `false`: bác sĩ thêm khi kê đơn trước đây, chưa kê được nữa |
| `trangThai` | `TrangThaiThuoc` | |
| `tenBacSiTao` | chuỗi \| null | bác sĩ đã thêm thuốc; null nếu quản trị viên thêm |
| `soLanKe` | số | số dòng đơn thuốc đã dùng thuốc này; > 0 thì không đổi tên / hoạt chất / hàm lượng / dạng / đường dùng được |

```json
{ "id": 15, "tenThuoc": "Paracetamol", "hoatChat": "Paracetamol", "hamLuong": "500mg", "dangBaoChe": "viên nén",
  "dangBaoCheEn": "tablet", "duongDung": "UONG", "donVi": "viên", "donViEn": "tablet", "moTa": null, "moTaEn": null,
  "daXacMinh": true, "trangThai": "DANG_DUNG", "tenBacSiTao": null, "soLanKe": 42 }
```

#### `ChiTietTaiKhoanResponse`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id`, `hoTen`, `email` | số, chuỗi, chuỗi | |
| `soDienThoai`, `anhDaiDien` | chuỗi \| null | |
| `vaiTro` | `VaiTro` | |
| `trangThai` | `TrangThaiTaiKhoan` | |
| `lyDoVoHieuHoa` | chuỗi \| null | lý do của lần vô hiệu hoá gần nhất |
| `ngaySinh` | ngày \| null | chỉ có ở bệnh nhân đã liên kết hồ sơ (`DA_LIEN_KET`) |
| `ngayTao`, `ngayCapNhat` | ngày giờ | |
| `coMatKhau` | boolean | `false`: tài khoản chỉ đăng nhập Google |
| `lienKetGoogle` | boolean | |
| `trangThaiLienKetHoSo` | `TrangThaiLienKet` \| null | chỉ bệnh nhân có hồ sơ; null nếu chưa có |
| `bacSi` | object \| null | chỉ bác sĩ: `{ id, idChuyenKhoa, tenChuyenKhoa, hocVi, soGiayPhep, soNamKinhNghiem, tieuSu, trangThai: TrangThaiBacSi }` |

Không có dữ liệu y tế (lịch hẹn, bệnh án).

```json
{ "id": 21, "hoTen": "Trần Thị Bình", "email": "binh.tran@eclinic.vn", "soDienThoai": "0901234567", "anhDaiDien": null,
  "vaiTro": "BAC_SI", "trangThai": "DA_KICH_HOAT", "lyDoVoHieuHoa": null, "ngaySinh": null,
  "ngayTao": "2026-09-01T08:00:00", "coMatKhau": true, "lienKetGoogle": false, "ngayCapNhat": "2026-10-20T10:15:00",
  "trangThaiLienKetHoSo": null,
  "bacSi": { "id": 7, "idChuyenKhoa": 1, "tenChuyenKhoa": "Nội tổng quát", "hocVi": "ThS.BS", "soGiayPhep": "001234/HCM-CCHN",
    "soNamKinhNghiem": 12, "tieuSu": null, "trangThai": "DANG_CONG_TAC" } }
```

### 4.3 Chuyên khoa

Danh sách công khai `GET /api/catalog/chuyen-khoa` (1 ngôn ngữ, theo `Accept-Language`) dùng cho trang đặt lịch, xem tài liệu
bệnh nhân / khách. Màn quản trị dùng route dưới để có cả 2 ngôn ngữ.

#### GET /api/catalog/quan-tri/chuyen-khoa

**Quyền:** QUAN_TRI_VIEN. Danh mục chuyên khoa theo tên tiếng Việt.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuKhoa` | chuỗi | không | so với tên của cả 2 ngôn ngữ |
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của [`ChuyenKhoaQuanTriResponse`](#chuyenkhoaquantriresponse).

```json
{ "id": 1, "tenChuyenKhoa": "Nội tổng quát", "tenChuyenKhoaEn": "General Internal Medicine",
  "moTa": "Khám và điều trị bệnh nội khoa thường gặp", "moTaEn": null, "trieuChung": "Sốt, ho, đau đầu", "trieuChungEn": null }
```

#### POST /api/catalog/chuyen-khoa

**Quyền:** QUAN_TRI_VIEN. Thêm chuyên khoa. Body chung của `POST` và `PUT` (`ChuyenKhoaRequest`):

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tenChuyenKhoa` | chuỗi | có | tối đa 150; không trùng |
| `tenChuyenKhoaEn` | chuỗi | không | tối đa 150; không trùng |
| `moTa`, `moTaEn` | chuỗi | không | tối đa 2000 |
| `trieuChung`, `trieuChungEn` | chuỗi | không | tối đa 2000; triệu chứng thường gặp; chatbot dùng để gợi ý chuyên khoa |

**Response:** HTTP 201, `duLieu` = [`ChuyenKhoaQuanTriResponse`](#chuyenkhoaquantriresponse).

**Lỗi:** 409 `TEN_CHUYEN_KHOA_DA_TON_TAI` (trùng tên tiếng Việt hoặc tiếng Anh; thông điệp nói rõ tên nào).

#### PUT /api/catalog/chuyen-khoa/{id}

**Quyền:** QUAN_TRI_VIEN. Sửa chuyên khoa: gửi đủ các trường như `POST`, trường bỏ trống bị xoá trắng.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tenChuyenKhoa` | chuỗi | có | tối đa 150; không trùng chuyên khoa khác |
| `tenChuyenKhoaEn` | chuỗi | không | tối đa 150; không trùng chuyên khoa khác |
| `moTa`, `moTaEn` | chuỗi | không | tối đa 2000 |
| `trieuChung`, `trieuChungEn` | chuỗi | không | tối đa 2000 |

**Response:** `duLieu` = [`ChuyenKhoaQuanTriResponse`](#chuyenkhoaquantriresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`; 409 `TEN_CHUYEN_KHOA_DA_TON_TAI`.

#### DELETE /api/catalog/chuyen-khoa/{id}

**Quyền:** QUAN_TRI_VIEN. Xoá chuyên khoa chưa có phòng khám và bác sĩ nào.

**Request:** path `id`.

**Response:** không có `duLieu`, chỉ `thongDiep`.

**Lỗi:** 404 `KHONG_TIM_THAY`; 409 `CHUYEN_KHOA_DANG_DUOC_SU_DUNG` (thông điệp có số phòng khám và số bác sĩ đang thuộc chuyên
khoa: chuyển họ sang chuyên khoa khác trước).

### 4.4 Phòng khám

#### GET /api/catalog/quan-tri/phong-kham

**Quyền:** QUAN_TRI_VIEN. Mọi phòng khám theo tên phòng, kể cả phòng ngừng hoạt động, kèm số ca sắp tới.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuKhoa` | chuỗi | không | tên phòng hoặc tầng |
| `idChuyenKhoa` | số | không | |
| `trangThai` | `TrangThaiPhongKham` | không | |
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của [`PhongKhamQuanTriResponse`](#phongkhamquantriresponse).

```json
{ "id": 2, "tenPhong": "Phòng khám Nội 1", "tenPhongEn": "Internal Medicine Room 1", "tang": "Tầng 2", "tangEn": "2nd floor",
  "idChuyenKhoa": 1, "tenChuyenKhoa": "Nội tổng quát", "trangThai": "HOAT_DONG", "soCaSapToi": 18 }
```

#### POST /api/catalog/quan-tri/phong-kham

**Quyền:** QUAN_TRI_VIEN. Thêm phòng khám (trạng thái `HOAT_DONG`). Body chung của `POST` và `PUT` (`PhongKhamRequest`):

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tenPhong` | chuỗi | có | tối đa 100; không trùng |
| `tenPhongEn` | chuỗi | không | tối đa 100; không trùng |
| `tang`, `tangEn` | chuỗi | không | tối đa 20 |
| `idChuyenKhoa` | số | có | |

**Response:** HTTP 201, `duLieu` = [`PhongKhamQuanTriResponse`](#phongkhamquantriresponse) (`soCaSapToi` = 0).

**Lỗi:** 404 `KHONG_TIM_THAY` (chuyên khoa); 409 `TEN_PHONG_DA_TON_TAI` (trùng tên tiếng Việt hoặc tiếng Anh).

#### PUT /api/catalog/quan-tri/phong-kham/{id}

**Quyền:** QUAN_TRI_VIEN. Sửa tên phòng, tầng, chuyên khoa: gửi đủ các trường, trường bỏ trống bị xoá trắng. Đổi chuyên
khoa chỉ được khi phòng không còn ca sắp tới (`soCaSapToi` = 0).

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tenPhong` | chuỗi | có | tối đa 100; không trùng phòng khác |
| `tenPhongEn` | chuỗi | không | tối đa 100; không trùng phòng khác |
| `tang`, `tangEn` | chuỗi | không | tối đa 20 |
| `idChuyenKhoa` | số | có | |

**Response:** `duLieu` = [`PhongKhamQuanTriResponse`](#phongkhamquantriresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`; 409 `TEN_PHONG_DA_TON_TAI`; 409 `PHONG_CON_CA_LAM_VIEC` (đổi chuyên khoa khi còn ca sắp tới;
thông điệp có số ca).

#### POST /api/catalog/quan-tri/phong-kham/{id}/ngung-hoat-dong

**Quyền:** QUAN_TRI_VIEN. Cho phòng ngừng hoạt động: không xếp ca mới vào được nữa. Phòng còn ca sắp tới thì phải chuyển các
ca đó sang phòng khác trước (`PUT /api/booking/quan-tri/lich-lam-viec/{id}`, [mục 3.5](#35-ca-làm-việc-xem-xếp-sửa-hủy)).
Gọi lại khi phòng đã ngừng thì không đổi gì.

**Request:** path `id`.

**Response:** `duLieu` = [`PhongKhamQuanTriResponse`](#phongkhamquantriresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`; 409 `PHONG_CON_CA_LAM_VIEC` (thông điệp có số ca sắp tới).

#### POST /api/catalog/quan-tri/phong-kham/{id}/hoat-dong-lai

**Quyền:** QUAN_TRI_VIEN. Cho phòng hoạt động lại. Gọi lại khi phòng đang hoạt động thì không đổi gì.

**Request:** path `id`.

**Response:** `duLieu` = [`PhongKhamQuanTriResponse`](#phongkhamquantriresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### GET /api/catalog/phong-kham

**Quyền:** QUAN_TRI_VIEN, BAC_SI. Phòng khám **đang hoạt động**, theo tên phòng: ô chọn phòng khi xếp / sửa ca (mục 3.5).

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idChuyenKhoa` | số | không | chỉ lấy phòng của 1 chuyên khoa (ca phải ở phòng cùng chuyên khoa với bác sĩ) |

**Response:** `duLieu` = mảng `{ id, tenPhong, tang, idChuyenKhoa, tenChuyenKhoa }` (theo ngôn ngữ của request; `tang` có thể
null), không phân trang.

```json
[ { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2", "idChuyenKhoa": 1, "tenChuyenKhoa": "Nội tổng quát" } ]
```

### 4.5 Bác sĩ

#### GET /api/catalog/quan-tri/bac-si

**Quyền:** QUAN_TRI_VIEN. Danh bạ bác sĩ, bác sĩ mới thêm đứng trước, kể cả bác sĩ đã ngừng công tác.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuKhoa` | chuỗi | không | họ tên, email, số điện thoại hoặc số giấy phép |
| `idChuyenKhoa` | số | không | |
| `trangThai` | `TrangThaiBacSi` | không | |
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của `{ id, maBacSi, hoTen, email, soDienThoai, anhDaiDien, hocVi, soGiayPhep,
idChuyenKhoa, tenChuyenKhoa, soLuotDaKham, trangThai, trangThaiTaiKhoan, phaiDoiMatKhau }`. `soLuotDaKham` = số lịch hẹn đã
khám xong; `soDienThoai`, `anhDaiDien`, `hocVi`, `soGiayPhep` có thể null; `trangThai` là `TrangThaiBacSi`,
`trangThaiTaiKhoan` là `TrangThaiTaiKhoan`.

```json
{ "id": 7, "maBacSi": "BS0007", "hoTen": "Trần Thị Bình", "email": "binh.tran@eclinic.vn", "soDienThoai": "0901234567",
  "anhDaiDien": null, "hocVi": "ThS.BS", "soGiayPhep": "001234/HCM-CCHN", "idChuyenKhoa": 1, "tenChuyenKhoa": "Nội tổng quát",
  "soLuotDaKham": 356, "trangThai": "DANG_CONG_TAC", "trangThaiTaiKhoan": "DA_KICH_HOAT", "phaiDoiMatKhau": false }
```

#### POST /api/catalog/quan-tri/bac-si

**Quyền:** QUAN_TRI_VIEN. Thêm bác sĩ: tạo tài khoản `BAC_SI` (đã kích hoạt, không qua xác thực email, mật khẩu mặc định của
hệ thống, `phaiDoiMatKhau = true`) và hồ sơ bác sĩ trong 1 bước; bác sĩ nhận email chào mừng. Lần đăng nhập đầu, bác sĩ
phải đặt mật khẩu mới ([`POST /api/auth/first-password`](#post-apiauthfirst-password)). Nếu không lưu được hồ sơ, tài khoản
vừa tạo bị xoá.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `hoTen` | chuỗi | có | tối đa 150 |
| `email` | chuỗi | có | tối đa 255; email hợp lệ, chưa có tài khoản nào dùng (kể cả tài khoản chờ kích hoạt); dùng để đăng nhập |
| `soDienThoai` | chuỗi | không | `^0\d{9}$` hoặc để trống; chưa có tài khoản nào dùng |
| `idChuyenKhoa` | số | có | |
| `soGiayPhep` | chuỗi | không | tối đa 50; không trùng bác sĩ khác |
| `hocVi` | chuỗi | không | tối đa 100 (vd `ThS.BS`) |
| `chucVu` | chuỗi | không | tối đa 150 |
| `namBatDauHanhNghe` | số | không | từ 1950 tới năm nay |

Các mục giới thiệu khác điền sau ở `PUT …/bac-si/{id}/ho-so`.

```json
{ "hoTen": "Lê Minh Châu", "email": "chau.le@eclinic.vn", "soDienThoai": "0912345678", "idChuyenKhoa": 3,
  "soGiayPhep": "004567/HCM-CCHN", "hocVi": "BS.CKI", "chucVu": "Bác sĩ điều trị", "namBatDauHanhNghe": 2015 }
```

**Response:** HTTP 201, `duLieu` = [`HoSoBacSiQuanTriResponse`](#hosobacsiquantriresponse) (`phaiDoiMatKhau = true`,
`phienBanHoSo` = 1, `anh` rỗng).

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (`namBatDauHanhNghe` sau năm nay); 404 `KHONG_TIM_THAY` (chuyên khoa); 409
`EMAIL_DA_TON_TAI`; 409 `SO_DIEN_THOAI_DA_TON_TAI`; 409 `SO_GIAY_PHEP_DA_TON_TAI`; 409 `XUNG_DOT_DU_LIEU` (chuyên khoa vừa bị
xoá hoặc số giấy phép vừa bị dùng cùng lúc); 503 `DICH_VU_NOI_BO_LOI`.

#### GET /api/catalog/quan-tri/bac-si/{id}

**Quyền:** QUAN_TRI_VIEN. Hồ sơ đầy đủ của 1 bác sĩ, kể cả bác sĩ không hiện ở trang công khai.

**Request:** path `id` (id bác sĩ, không phải id tài khoản).

**Response:** `duLieu` = [`HoSoBacSiQuanTriResponse`](#hosobacsiquantriresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### PUT /api/catalog/quan-tri/bac-si/{id}

**Quyền:** QUAN_TRI_VIEN. Sửa thông tin cơ bản: họ tên và số điện thoại (ghi vào tài khoản), chuyên khoa, số giấy phép.
Đổi chuyên khoa chỉ được khi bác sĩ không còn ca sắp tới.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `hoTen` | chuỗi | có | tối đa 150 |
| `soDienThoai` | chuỗi | không | `^0\d{9}$` hoặc để trống (trống = bỏ số điện thoại); chưa có tài khoản khác dùng |
| `idChuyenKhoa` | số | có | |
| `soGiayPhep` | chuỗi | không | tối đa 50; không trùng bác sĩ khác; trống = bỏ |

**Response:** `duLieu` = [`HoSoBacSiQuanTriResponse`](#hosobacsiquantriresponse).

**Lỗi:** 404 `KHONG_TIM_THAY` (bác sĩ / chuyên khoa); 409 `BAC_SI_CON_CA_LAM_VIEC` (đổi chuyên khoa khi còn ca sắp tới: hủy
hoặc chuyển ca trước); 409 `SO_DIEN_THOAI_DA_TON_TAI`; 409 `SO_GIAY_PHEP_DA_TON_TAI`; 409 `XUNG_DOT_DU_LIEU`; 503
`DICH_VU_NOI_BO_LOI`.

#### PUT /api/catalog/quan-tri/bac-si/{id}/ho-so

**Quyền:** QUAN_TRI_VIEN. Sửa hồ sơ giới thiệu. **Ghi đè mọi trường**: gửi đủ (lấy từ `GET …/bac-si/{id}`), trường bỏ trống
bị xoá trắng, mảng bỏ trống thành rỗng. Bác sĩ cũng tự sửa được một phần hồ sơ: dùng `phienBan` để không đè lên nhau.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `phienBan` | số | có | `phienBanHoSo` lúc mở form |
| `hocVi` | chuỗi | không | tối đa 100 |
| `chucVu`, `chucVuEn` | chuỗi | không | tối đa 150 |
| `namBatDauHanhNghe` | số | không | từ 1950 tới năm nay |
| `ngayCapGiayPhep` | ngày | không | không sau hôm nay |
| `noiCapGiayPhep`, `noiCapGiayPhepEn` | chuỗi | không | tối đa 200 |
| `phamViHanhNghe`, `phamViHanhNgheEn` | chuỗi | không | tối đa 500 |
| `gioiThieuNgan`, `gioiThieuNganEn` | chuỗi | không | tối đa 300 |
| `tieuSu`, `tieuSuEn` | chuỗi | không | tối đa 5000 |
| `ngonNguKham` | mảng chuỗi | không | tối đa 10 mã, mỗi mã 2–3 chữ cái (`vi`, `en`) |
| `quaTrinhDaoTao`, `quaTrinhDaoTaoEn`, `quaTrinhCongTac`, `quaTrinhCongTacEn`, `linhVucKhamChua`, `linhVucKhamChuaEn`, `giaiThuong`, `giaiThuongEn`, `congTrinhNghienCuu`, `congTrinhNghienCuuEn` | mảng chuỗi | không | tối đa 20 ý mỗi mảng; mỗi ý không trống, 1 dòng, tối đa 300 ký tự |

**Response:** `duLieu` = [`HoSoBacSiQuanTriResponse`](#hosobacsiquantriresponse) (`phienBanHoSo` tăng 1).

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (`namBatDauHanhNghe` sau năm nay, `ngayCapGiayPhep` trong tương lai, ý / mã ngôn ngữ sai);
404 `KHONG_TIM_THAY`; 409 `DU_LIEU_DA_THAY_DOI` (bác sĩ hoặc quản trị viên khác vừa lưu: tải lại rồi sửa lại).

#### GET /api/catalog/quan-tri/bac-si/{id}/anh-huong-ngung-cong-tac

**Quyền:** QUAN_TRI_VIEN. Xem trước khi cho ngừng công tác: hiện trong hộp xác nhận.

**Request:** path `id`.

**Response:** `duLieu` = `{ soCaSapToi, soLichHenBiAnhHuong }`: số ca chưa bắt đầu sẽ bị hủy, số lịch hẹn còn hiệu lực của
các ca đó sẽ phải đổi lịch.

```json
{ "soCaSapToi": 9, "soLichHenBiAnhHuong": 23 }
```

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### POST /api/catalog/quan-tri/bac-si/{id}/ngung-cong-tac

**Quyền:** QUAN_TRI_VIEN. Cho bác sĩ ngừng công tác, theo thứ tự:
- bác sĩ chuyển sang `NGUNG_CONG_TAC`: ẩn khỏi trang công khai, không xếp ca / nhận lịch hẹn mới;
- mọi ca sắp tới bị hủy (nguồn `NGUNG_CONG_TAC` trong lịch sử ca); lịch hẹn của các ca đó **được giữ**, đánh dấu `canDoiLich`
  và bệnh nhân được báo. Xử lý tiếp ở `GET /api/booking/quan-tri/lich-lam-viec/can-doi-lich`;
- tài khoản bị vô hiệu hoá với lý do này: bác sĩ bị đăng xuất mọi thiết bị.

Gọi lại được (vd sau 503): bước đã xong thì không làm lại, lần sau trả `soCaDaHuy` / `soLichHenCanDoi` là số của lần đó
(thường 0).

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `lyDo` | chuỗi | có | tối đa 500; ghi vào lịch sử ca và lý do vô hiệu hoá tài khoản |

**Response:** `duLieu` = `{ bacSi, soCaDaHuy, soLichHenCanDoi }`, `bacSi` là
[`HoSoBacSiQuanTriResponse`](#hosobacsiquantriresponse).

```json
{ "bacSi": { "id": 7, "maBacSi": "BS0007", "hoTen": "Trần Thị Bình", "trangThai": "NGUNG_CONG_TAC",
    "trangThaiTaiKhoan": "VO_HIEU_HOA", "phienBanHoSo": 4, "anh": [] },
  "soCaDaHuy": 9, "soLichHenCanDoi": 23 }
```

(`bacSi` rút gọn trong ví dụ.)

**Lỗi:** 404 `KHONG_TIM_THAY`; 503 `DICH_VU_NOI_BO_LOI` (booking-service hoặc identity-service không trả lời; bác sĩ đã ở
`NGUNG_CONG_TAC`: gửi lại để làm nốt).

#### POST /api/catalog/quan-tri/bac-si/{id}/cong-tac-lai

**Quyền:** QUAN_TRI_VIEN. Cho bác sĩ công tác lại: tài khoản được kích hoạt lại, bác sĩ hiện lại ở trang công khai. Các ca đã
hủy **không** được khôi phục: xếp ca mới ở mục 3.5. Gọi lại khi bác sĩ đang công tác thì không đổi gì.

**Request:** path `id`.

**Response:** `duLieu` = [`HoSoBacSiQuanTriResponse`](#hosobacsiquantriresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`; 503 `DICH_VU_NOI_BO_LOI`.

### 4.6 Ảnh giới thiệu của bác sĩ

Mỗi bác sĩ tối đa 12 ảnh (ảnh làm việc + ảnh chứng chỉ). Ảnh hiện ở trang công khai của bác sĩ theo thứ tự trong
`anh` của [`HoSoBacSiQuanTriResponse`](#hosobacsiquantriresponse). `{id}` là id bác sĩ.

#### POST /api/catalog/quan-tri/bac-si/{id}/anh

**Quyền:** QUAN_TRI_VIEN. Thêm 1 ảnh, xếp cuối danh sách. Body `multipart/form-data`:

| Trường (multipart) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `anh` | tệp | có | JPEG / PNG / WebP (nhận theo nội dung tệp), tối đa 4 MB; lưu thu về cạnh dài 1600 px |
| `loai` | `LoaiAnhBacSi` | có | `ANH_CONG_VIEC` hoặc `CHUNG_CHI` |
| `chuThich`, `chuThichEn` | chuỗi | không | tối đa 200 |

**Response:** HTTP 201, `duLieu` = [`AnhBacSiQuanTriResponse`](#anhbacsiquantriresponse).

```json
{ "id": 34, "loai": "CHUNG_CHI", "url": "https://res.cloudinary.com/demo/image/upload/bac-si/7/def.jpg",
  "chuThich": "Chứng chỉ hành nghề", "chuThichEn": "Practising certificate" }
```

**Lỗi:** 400 `ANH_KHONG_HOP_LE` (không phải ảnh JPEG / PNG / WebP); 400 `DU_LIEU_KHONG_HOP_LE` (thiếu `anh` / `loai`, chú
thích quá dài); 404 `KHONG_TIM_THAY`; 409 `VUOT_SO_ANH_BAC_SI` (đủ 12 ảnh); 413 `TEP_QUA_LON` (quá 4 MB); 503
`LUU_TRU_ANH_KHONG_KHA_DUNG` (kho ảnh chưa cấu hình).

#### PUT /api/catalog/quan-tri/bac-si/{id}/anh/{idAnh}

**Quyền:** QUAN_TRI_VIEN. Sửa loại và chú thích 1 ảnh (không đổi tệp; muốn đổi tệp thì xoá rồi thêm lại). Chú thích bỏ trống
là xoá chú thích.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `loai` | `LoaiAnhBacSi` | có | |
| `chuThich`, `chuThichEn` | chuỗi | không | tối đa 200 |

**Response:** `duLieu` = [`AnhBacSiQuanTriResponse`](#anhbacsiquantriresponse).

**Lỗi:** 404 `KHONG_TIM_THAY` (ảnh không có hoặc không phải của bác sĩ này).

#### PUT /api/catalog/quan-tri/bac-si/{id}/anh/thu-tu

**Quyền:** QUAN_TRI_VIEN. Sắp xếp lại ảnh (kéo thả): gửi id của **mọi** ảnh của bác sĩ theo thứ tự mới.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idAnh` | mảng số | có | đúng tập id ảnh hiện có, mỗi id 1 lần |

```json
{ "idAnh": [ 34, 31, 33 ] }
```

**Response:** `duLieu` = mảng [`AnhBacSiQuanTriResponse`](#anhbacsiquantriresponse) theo thứ tự mới.

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (thiếu / thừa / trùng id: thường do ảnh vừa được thêm hoặc xoá ở nơi khác, tải lại rồi
sắp lại); 404 `KHONG_TIM_THAY`.

#### DELETE /api/catalog/quan-tri/bac-si/{id}/anh/{idAnh}

**Quyền:** QUAN_TRI_VIEN. Xoá 1 ảnh, kể cả ảnh chứng chỉ.

**Request:** path `id`, `idAnh`.

**Response:** không có `duLieu`, chỉ `thongDiep`.

**Lỗi:** 404 `KHONG_TIM_THAY`.

### 4.7 Tài khoản

Tài khoản quản trị viên chỉ xem được: mọi thao tác đổi trạng thái / xoá trên tài khoản `QUAN_TRI_VIEN` trả 403
`TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE`. Bác sĩ ngừng làm việc thì dùng `ngung-cong-tac` ([mục 4.5](#45-bác-sĩ)) thay vì vô hiệu hoá ở
đây: route đó hủy ca và xử lý lịch hẹn.

#### GET /api/users

**Quyền:** QUAN_TRI_VIEN. Danh sách tài khoản của mọi vai trò (kể cả quản trị viên đang đăng nhập), mới tạo đứng trước.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuKhoa` | chuỗi | không | 1 phần họ tên, email hoặc số điện thoại |
| `vaiTro` | `VaiTro` | không | |
| `trangThai` | `TrangThaiTaiKhoan` | không | |
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của `{ id, hoTen, email, soDienThoai, anhDaiDien, vaiTro, trangThai, lyDoVoHieuHoa,
ngaySinh, ngayTao }`. `soDienThoai`, `anhDaiDien`, `lyDoVoHieuHoa` có thể null; `ngaySinh` chỉ có ở bệnh nhân đã liên kết hồ
sơ.

```json
{ "id": 105, "hoTen": "Nguyễn Văn An", "email": "an.nguyen@gmail.com", "soDienThoai": "0987654321", "anhDaiDien": null,
  "vaiTro": "BENH_NHAN", "trangThai": "DA_KICH_HOAT", "lyDoVoHieuHoa": null, "ngaySinh": "1990-05-12",
  "ngayTao": "2026-10-02T19:45:10" }
```

#### GET /api/users/{id}

**Quyền:** QUAN_TRI_VIEN. Chi tiết 1 tài khoản (kèm tóm tắt hồ sơ bác sĩ nếu là bác sĩ).

**Request:** path `id` (id tài khoản).

**Response:** `duLieu` = [`ChiTietTaiKhoanResponse`](#chitiettaikhoanresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### PUT /api/users/{id}/status

**Quyền:** QUAN_TRI_VIEN. Vô hiệu hoá hoặc kích hoạt lại 1 tài khoản; chủ tài khoản nhận email báo.
- `VO_HIEU_HOA`: cần `lyDo`; tài khoản bị đăng xuất mọi thiết bị và không đăng nhập được (lỗi `TAI_KHOAN_BI_VO_HIEU_HOA`, mục
  1.4). Bác sĩ còn lịch hẹn sắp tới chưa khám thì không vô hiệu hoá được.
- `DA_KICH_HOAT`: đăng nhập lại được; không khôi phục phiên đăng nhập cũ.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `trangThai` | `TrangThaiTaiKhoan` | có | chỉ `VO_HIEU_HOA` hoặc `DA_KICH_HOAT` |
| `lyDo` | chuỗi | có điều kiện | tối đa 500; bắt buộc khi `VO_HIEU_HOA` (gửi trong email và hiện ở `lyDoVoHieuHoa`) |

```json
{ "trangThai": "VO_HIEU_HOA", "lyDo": "Tài khoản đăng ký thông tin giả" }
```

**Response:** `duLieu` = [`ChiTietTaiKhoanResponse`](#chitiettaikhoanresponse) sau khi đổi.

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (`trangThai = CHO_XAC_NHAN`, thiếu `lyDo` khi vô hiệu hoá); 403
`TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE`; 404 `KHONG_TIM_THAY`; 409 `TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE` (tài khoản chưa xác thực
email, hoặc đã ở đúng trạng thái này); 409 `BAC_SI_CON_LICH_HEN` (thông điệp có số lịch hẹn).

#### DELETE /api/users/{id}

**Quyền:** QUAN_TRI_VIEN. Xoá hẳn 1 tài khoản, chỉ khi chưa có dữ liệu nào gắn với nó (hồ sơ bác sĩ, hồ sơ bệnh nhân, lịch hẹn
đã đặt, thông báo, phiên chat). Thường chỉ xoá được tài khoản đăng ký nhầm chưa dùng; còn lại thì vô hiệu hoá.

**Request:** path `id`.

**Response:** không có `duLieu`, chỉ `thongDiep`.

**Lỗi:** 403 `TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE`; 404 `KHONG_TIM_THAY`; 409 `TAI_KHOAN_DANG_DUOC_SU_DUNG` (thông điệp kể tên dữ
liệu đang gắn với tài khoản).

### 4.8 Danh mục thuốc

#### GET /api/medical/quan-tri/thuoc

**Quyền:** QUAN_TRI_VIEN. Danh mục thuốc: thuốc chưa xác minh đứng trước, rồi theo tên và hàm lượng; kèm số lần đã kê.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuKhoa` | chuỗi | không | 1 phần tên hoặc hoạt chất, không phân biệt hoa thường / khoảng trắng |
| `daXacMinh` | boolean | không | `false` = danh sách cần xác minh |
| `trangThai` | `TrangThaiThuoc` | không | |
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của [`ThuocQuanTriResponse`](#thuocquantriresponse).

#### POST /api/medical/quan-tri/thuoc

**Quyền:** QUAN_TRI_VIEN. Thêm thuốc vào danh mục: đã xác minh và đang dùng ngay, bác sĩ kê được luôn. Body chung của `POST`
và `PUT` (`ThuocRequest`):

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tenThuoc` | chuỗi | có | tối đa 255 |
| `hoatChat` | chuỗi | có | tối đa 255 |
| `hamLuong` | chuỗi | có | tối đa 100 (vd `500mg`); tên + hàm lượng không trùng thuốc khác |
| `dangBaoChe`, `dangBaoCheEn` | chuỗi | không | tối đa 50 (vd `viên nén` / `tablet`) |
| `duongDung` | `DuongDungThuoc` | không | |
| `donVi`, `donViEn` | chuỗi | không | tối đa 30 (vd `viên` / `tablet`) |
| `moTa`, `moTaEn` | chuỗi | không | tối đa 2000 |

```json
{ "tenThuoc": "Amoxicillin", "hoatChat": "Amoxicillin", "hamLuong": "500mg", "dangBaoChe": "viên nang",
  "dangBaoCheEn": "capsule", "duongDung": "UONG", "donVi": "viên", "donViEn": "capsule" }
```

**Response:** HTTP 201, `duLieu` = [`ThuocQuanTriResponse`](#thuocquantriresponse) (`soLanKe` = 0).

**Lỗi:** 409 `TEN_THUOC_DA_TON_TAI` (trùng tên + hàm lượng, không phân biệt hoa thường / khoảng trắng); 409
`XUNG_DOT_DU_LIEU` (quản trị viên khác vừa thêm cùng thuốc).

#### PUT /api/medical/quan-tri/thuoc/{id}

**Quyền:** QUAN_TRI_VIEN. Sửa thuốc: gửi đủ các trường như `POST`, trường không bắt buộc bỏ trống bị xoá trắng. Thuốc **đã
được kê** (`soLanKe` > 0) không đổi được tên, hoạt chất, hàm lượng, dạng bào chế, đường dùng (trường đang trống thì vẫn điền
được); đơn vị, bản tiếng Anh, mô tả luôn sửa được.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tenThuoc` | chuỗi | có | tối đa 255 |
| `hoatChat` | chuỗi | có | tối đa 255 |
| `hamLuong` | chuỗi | có | tối đa 100 |
| `dangBaoChe`, `dangBaoCheEn` | chuỗi | không | tối đa 50 |
| `duongDung` | `DuongDungThuoc` | không | |
| `donVi`, `donViEn` | chuỗi | không | tối đa 30 |
| `moTa`, `moTaEn` | chuỗi | không | tối đa 2000 |

**Response:** `duLieu` = [`ThuocQuanTriResponse`](#thuocquantriresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`; 409 `THUOC_DA_DUOC_KE`; 409 `TEN_THUOC_DA_TON_TAI`.

#### POST /api/medical/quan-tri/thuoc/{id}/xac-minh

**Quyền:** QUAN_TRI_VIEN. Xác minh thuốc do bác sĩ thêm khi kê đơn trước đây (`daXacMinh = false`): từ đó bác sĩ kê được.
Thuốc phải có hoạt chất và hàm lượng: thiếu thì sửa bằng `PUT` trước. Gọi lại khi đã xác minh thì không đổi gì.

**Request:** path `id`.

**Response:** `duLieu` = [`ThuocQuanTriResponse`](#thuocquantriresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`; 409 `THUOC_THIEU_THONG_TIN`.

#### POST /api/medical/quan-tri/thuoc/{id}/ngung-dung

**Quyền:** QUAN_TRI_VIEN. Ngừng dùng: thuốc không còn được gợi ý cho bác sĩ và không kê mới được; đơn thuốc cũ vẫn hiện thuốc
này. Gọi lại khi đã ngừng thì không đổi gì.

**Request:** path `id`.

**Response:** `duLieu` = [`ThuocQuanTriResponse`](#thuocquantriresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### POST /api/medical/quan-tri/thuoc/{id}/dung-lai

**Quyền:** QUAN_TRI_VIEN. Dùng lại thuốc đã ngừng dùng. Gọi lại khi đang dùng thì không đổi gì.

**Request:** path `id`.

**Response:** `duLieu` = [`ThuocQuanTriResponse`](#thuocquantriresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`.

### 4.9 Lịch sử sửa hồ sơ bệnh án

#### GET /api/medical/quan-tri/lich-hen/{idLichHen}/benh-an/lich-su

**Quyền:** QUAN_TRI_VIEN. Lịch sử sửa bệnh án của 1 lượt khám bất kỳ, mới nhất trước: mỗi lần sửa có bác sĩ, thời điểm, lý do,
phần đã đổi và nội dung trước / sau. Bác sĩ chỉ sửa được trong một số ngày sau lần lưu đầu, luôn kèm lý do.

**Request:** path `idLichHen` (`id` của lịch hẹn ở mục 3.8).

**Response:**

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `idLichHen`, `maTraCuu` | số, chuỗi | |
| `phienBanHienTai` | số | 1 = chưa sửa lần nào |
| `ngayGhiNhan` | ngày giờ | lúc ghi (phiên bản 1) |
| `cacLanSua` | mảng | mới nhất trước, rỗng nếu chưa sửa: `{ phienBan, ngaySua, bacSi, lyDo, cacTruongDoi, truoc, sau }`; `phienBan` là phiên bản sau lần sửa đó (2, 3…) |
| `cacLanSua[].bacSi` | object | `{ id, hoTen }` |
| `cacLanSua[].cacTruongDoi` | mảng `TruongBenhAn` | phần đã đổi, theo thứ tự trên hồ sơ |
| `cacLanSua[].truoc`, `sau` | object | toàn bộ nội dung để so: `{ lyDoKham, sinhHieu, khamLamSang, chanDoan, maIcdChinh, chanDoanKemTheo, ghiChu, ngayTaiKhamDeXuat, donThuoc }`; `sinhHieu` = `{ mach, nhietDo, huyetApTamThu, huyetApTamTruong, nhipTho, canNang, chieuCao }`; `donThuoc[]` = `{ idThuoc, tenThuoc, hamLuong, lieuDung, soLanMoiNgay, soNgayDung, soLuong, ghiChuSuDung }`. Lịch sử cũ có thể thiếu một số trường (null) |

```json
{ "idLichHen": 812, "maTraCuu": "ECL-20261025-4198", "phienBanHienTai": 2, "ngayGhiNhan": "2026-10-25T09:02:44",
  "cacLanSua": [ { "phienBan": 2, "ngaySua": "2026-10-26T14:10:00", "bacSi": { "id": 7, "hoTen": "Trần Thị Bình" },
    "lyDo": "Có kết quả test nhanh liên cầu", "cacTruongDoi": [ "CHAN_DOAN" ],
    "truoc": { "chanDoan": "Viêm họng cấp", "maIcdChinh": "J02.9" }, "sau": { "chanDoan": "Viêm họng cấp do liên cầu", "maIcdChinh": "J02.0" } } ] }
```

(`truoc` / `sau` rút gọn trong ví dụ.)

**Lỗi:** 404 `KHONG_TIM_THAY` (lịch hẹn không có, hoặc lượt khám chưa có bệnh án).

### 4.10 Báo cáo thống kê

**Kỳ báo cáo** (query chung của 2 route):

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `ky` | `KyBaoCao` | không | mặc định `THANG` |
| `ngay` | ngày | không | ngày mốc, mặc định hôm nay; kỳ là ngày / tuần (thứ 2 – chủ nhật) / tháng / năm chứa ngày này. Bỏ qua khi `TUY_CHON` |
| `tuNgay`, `denNgay` | ngày | có điều kiện | bắt buộc khi `ky = TUY_CHON`, tính cả 2 đầu, tối đa 366 ngày; bỏ qua ở kỳ khác |
| `idChuyenKhoa` | số | không | chỉ tính 1 chuyên khoa (theo chuyên khoa hiện tại của bác sĩ) |

`donViThoiGian` của chuỗi `theoThoiGian` theo độ dài kỳ: tới 31 ngày → `NGAY`, tới 184 ngày → `TUAN`, dài hơn → `THANG`.

#### GET /api/report/quan-tri/thong-ke

**Quyền:** QUAN_TRI_VIEN. Toàn bộ số liệu của 1 kỳ trong 1 lần gọi: tổng quan, trạng thái, kênh đặt, thời gian đặt trước,
theo chuyên khoa, theo bác sĩ, chuỗi theo thời gian.

**Response:** `duLieu` = `BaoCaoThongKeResponse`:

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `ky`, `tuNgay`, `denNgay` | `KyBaoCao`, ngày, ngày | khoảng thật đã tính (vd kỳ `THANG` → ngày 1 tới cuối tháng) |
| `idChuyenKhoa` | số \| null | như query |
| `donViThoiGian` | `DonViThoiGian` | |
| `tongQuan` | object | bảng dưới |
| `trangThai` | object | lịch hẹn trong kỳ theo trạng thái: `{ chuaKham, daKham, khongDen, benhNhanHuy, bacSiHuy, quanTriHuy, canDoiLich }`. `chuaKham` = `DA_XAC_NHAN`; `benhNhanHuy` = `DA_HUY`, `bacSiHuy` = `BI_TU_CHOI`, `quanTriHuy` = `QUAN_TRI_HUY`; `canDoiLich` = phần của `chuaKham` có ca bị hủy, chờ đổi lịch |
| `kenhDat` | object | `{ khach, taiKhoan, hotline }`: số lịch hẹn theo `KenhDatLich` |
| `thoiGianDatTruoc` | object | `{ trungBinhGio, duoi1Ngay, tu1Den3Ngay, tu4Den7Ngay, tu8Den14Ngay, tren14Ngay }`: giờ khám − lúc đặt (lịch đổi tính từ lúc đổi); `trungBinhGio` 1 chữ số thập phân, null khi không có lịch hẹn |
| `theoChuyenKhoa` | mảng | mọi chuyên khoa (cả chuyên khoa không có số liệu), theo tên: `{ idChuyenKhoa, tenChuyenKhoa, chiSo }` |
| `theoBacSi` | mảng | bác sĩ có lịch hẹn hoặc ca trong kỳ, nhiều lịch hẹn trước: `{ idBacSi, maBacSi, hoTen, idChuyenKhoa, tenChuyenKhoa, chiSo }` |
| `chiSo` | object | `{ lichHen, luotKham, benhNhanDaKham, daHuy, khongDen, tyLeHuy, tyLeKhongDen, luotDaDat, tongLuot, tyLeLapDay }`: cùng định nghĩa với `tongQuan`, trong phạm vi chuyên khoa / bác sĩ đó; `daHuy` = bệnh nhân + bác sĩ + quản trị viên hủy |
| `theoThoiGian` | mảng | đủ mọi điểm từ đầu tới cuối kỳ: `{ tuNgay, denNgay, lichHen, luotKham, daHuy, khongDen }`; điểm đầu / cuối cắt theo kỳ |
| `tinhLuc` | ngày giờ | lúc tính |

`tongQuan`:

| Trường | Ghi chú |
|---|---|
| `bacSiDangCongTac` | bác sĩ đang công tác có tài khoản đã kích hoạt, **hiện tại** (không theo kỳ) |
| `bacSiCoCa` | bác sĩ có ít nhất 1 ca hoạt động trong kỳ |
| `taiKhoanBenhNhanMoi` | tài khoản bệnh nhân tạo trong kỳ, đã xác thực email; **không** lọc theo `idChuyenKhoa` |
| `benhNhanDaKham` | bệnh nhân khác nhau (theo hồ sơ, duy nhất theo CCCD) đã khám trong kỳ |
| `luotKham` | lịch hẹn đã khám xong (`DA_HOAN_THANH`) |
| `lichHen` | lịch hẹn có giờ khám trong kỳ, trừ lịch cũ của 1 lần đổi lịch |
| `luotDatMoi` | lịch hẹn được **tạo** trong kỳ, trừ lịch cũ của 1 lần đổi lịch |
| `tyLeHuy` | % đã hủy / `lichHen` |
| `tyLeKhongDen` | % không đến / (đã khám + không đến) |
| `luotDaDat`, `tongLuot` | lượt đã đặt / tổng lượt (còn trống + đã đặt) của các ca hoạt động trong kỳ |
| `tyLeLapDay` | % `luotDaDat` / `tongLuot` |

Các tỷ lệ là **phần trăm** (0–100, 1 chữ số thập phân, vd `12.5`), null khi mẫu số bằng 0.

```json
{ "ky": "THANG", "tuNgay": "2026-10-01", "denNgay": "2026-10-31", "idChuyenKhoa": null, "donViThoiGian": "NGAY",
  "tongQuan": { "bacSiDangCongTac": 24, "bacSiCoCa": 21, "taiKhoanBenhNhanMoi": 138, "benhNhanDaKham": 702,
    "luotKham": 845, "lichHen": 1012, "luotDatMoi": 1090, "tyLeHuy": 9.8, "tyLeKhongDen": 4.2, "luotDaDat": 1012,
    "tongLuot": 1560, "tyLeLapDay": 64.9 },
  "trangThai": { "chuaKham": 58, "daKham": 845, "khongDen": 10, "benhNhanHuy": 70, "bacSiHuy": 12, "quanTriHuy": 17, "canDoiLich": 3 },
  "kenhDat": { "khach": 402, "taiKhoan": 571, "hotline": 39 },
  "thoiGianDatTruoc": { "trungBinhGio": 61.5, "duoi1Ngay": 210, "tu1Den3Ngay": 455, "tu4Den7Ngay": 240, "tu8Den14Ngay": 80, "tren14Ngay": 27 },
  "theoChuyenKhoa": [ { "idChuyenKhoa": 1, "tenChuyenKhoa": "Nội tổng quát", "chiSo": { "lichHen": 320, "luotKham": 270,
    "benhNhanDaKham": 231, "daHuy": 31, "khongDen": 4, "tyLeHuy": 9.7, "tyLeKhongDen": 1.5, "luotDaDat": 320, "tongLuot": 480, "tyLeLapDay": 66.7 } } ],
  "theoBacSi": [ { "idBacSi": 7, "maBacSi": "BS0007", "hoTen": "Trần Thị Bình", "idChuyenKhoa": 1, "tenChuyenKhoa": "Nội tổng quát",
    "chiSo": { "lichHen": 96, "luotKham": 84, "benhNhanDaKham": 80, "daHuy": 9, "khongDen": 1, "tyLeHuy": 9.4, "tyLeKhongDen": 1.2,
      "luotDaDat": 96, "tongLuot": 120, "tyLeLapDay": 80.0 } } ],
  "theoThoiGian": [ { "tuNgay": "2026-10-01", "denNgay": "2026-10-01", "lichHen": 35, "luotKham": 30, "daHuy": 3, "khongDen": 1 } ],
  "tinhLuc": "2026-10-21T10:30:00" }
```

(Mỗi mảng chỉ giữ 1 phần tử trong ví dụ.)

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (`TUY_CHON` thiếu `tuNgay` / `denNgay`, `denNgay` trước `tuNgay`, quá 366 ngày, `ky` sai);
404 `KHONG_TIM_THAY` (chuyên khoa).

#### GET /api/report/quan-tri/thong-ke/xuat-excel

**Quyền:** QUAN_TRI_VIEN. Tải báo cáo của cùng kỳ dạng Excel: trang tổng quan, theo chuyên khoa, theo bác sĩ, theo thời gian
và trang ghi chú cách tính.

**Request:** query như `GET …/thong-ke` (bảng "Kỳ báo cáo").

**Response:** tệp `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`,
`bao-cao-thong-ke_<tuNgay>_<denNgay>.xlsx`. Tên trang, tiêu đề theo `Accept-Language`. Gọi bằng `fetch` kèm token rồi tải
`blob`.

**Lỗi:** JSON, như `GET …/thong-ke`.
