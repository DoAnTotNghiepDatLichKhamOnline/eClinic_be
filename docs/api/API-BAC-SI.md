# Tài liệu API eClinic — Bác sĩ

Dành cho: frontend các màn hình của bác sĩ (vai trò `BAC_SI`).

Mọi route ở mục 3 cần token của tài khoản bác sĩ. Phần quy ước chung và các route xác thực, tài khoản, thông báo giống hệt ở cả 3 tài liệu.

**Lịch sử phiên bản**

| Phiên bản | Nội dung |
|---|---|
| 1.0 — 21/10/2026 | Bản đầu: 27 route dùng chung (mục 2) và 39 route của bác sĩ (mục 3). |

## Mục lục

- [1. Quy ước chung](#1-quy-ước-chung)
- [2. Xác thực, tài khoản và thông báo (mọi vai trò)](#2-xác-thực-tài-khoản-và-thông-báo-mọi-vai-trò)
- [3. API của bác sĩ](#3-api-của-bác-sĩ)
  - [3.1 Tổng quan](#31-tổng-quan)
  - [3.2 Mô hình và enum của mục 3](#32-mô-hình-và-enum-của-mục-3)
  - [3.3 Tổng quan, lịch làm việc, phòng khám](#33-tổng-quan-lịch-làm-việc-phòng-khám)
  - [3.4 Lịch hẹn của tôi](#34-lịch-hẹn-của-tôi)
  - [3.5 Hàng đợi khám](#35-hàng-đợi-khám)
  - [3.6 Hồ sơ khám của bệnh nhân](#36-hồ-sơ-khám-của-bệnh-nhân)
  - [3.7 Bệnh án, gợi ý thuốc và ICD-10](#37-bệnh-án-gợi-ý-thuốc-và-icd-10)
  - [3.8 Yêu cầu đổi ca, xin nghỉ, nhường ca](#38-yêu-cầu-đổi-ca-xin-nghỉ-nhường-ca)
  - [3.9 Hồ sơ giới thiệu và ảnh](#39-hồ-sơ-giới-thiệu-và-ảnh)
  - [3.10 Đánh giá về tôi](#310-đánh-giá-về-tôi)

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

## 3. API của bác sĩ

### 3.1 Tổng quan

Mọi route của mục 3 cần token bác sĩ (`BAC_SI`) và chỉ làm việc trên dữ liệu **của chính bác sĩ đang đăng nhập**: không
nhận id bác sĩ. Lịch hẹn, ca, yêu cầu của bác sĩ khác coi như không có (404 `KHONG_TIM_THAY`, không phải 403). Riêng
`GET /api/catalog/phong-kham` quản trị viên cũng gọi được.

**Hai luồng chính**

1. **Ngày khám:**
   - mở trang chủ: số liệu `GET /api/booking/bac-si/toi/tong-quan`, hàng đợi `GET /api/booking/bac-si/toi/hang-doi`;
   - gọi bệnh nhân: `POST …/hang-doi/goi-tiep` (người kế tiếp) hoặc `POST …/lich-hen/{id}/goi-kham` (gọi 1 người),
     vắng thì `POST …/lich-hen/{id}/vang-mat`;
   - mở hồ sơ khám `GET …/lich-hen/{id}/ho-so-kham` (hoặc quét QR phiếu khám: `GET …/lich-hen/tra-cuu/ho-so-kham?ma=`):
     thông tin bệnh nhân không che, các lần khám trước;
   - đối chiếu giấy tờ: sửa thông tin `PUT …/lich-hen/{id}/benh-nhan`, xong thì `POST …/lich-hen/{id}/da-doi-chieu`;
   - ghi bệnh án `POST /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an` (gợi ý thuốc `GET …/thuoc`, ICD-10
     `GET …/icd10`). Lịch hẹn thành `DA_HOAN_THANH`, phản hồi có link sổ khám để đưa cho bệnh nhân. Sửa trong 7 ngày
     bằng `PUT` cùng đường dẫn.
2. **Lịch làm việc và yêu cầu:**
   - xem ca `GET /api/booking/bac-si/toi/lich-lam-viec`, in `…/lich-lam-viec/pdf`, xem lịch hẹn theo lịch
     `GET …/lich-hen/lich`;
   - xin đổi ca / xin nghỉ / nhường ca: `POST …/yeu-cau-doi-lich` (nghỉ nhiều ca: `…/xin-nghi-nhieu-ca`), theo dõi và rút
     ở `GET …/yeu-cau-doi-lich`;
   - đồng nghiệp nhường ca cho mình: `GET …/yeu-cau-nhuong-ca`, đồng ý / từ chối. Kết quả duyệt đến qua thông báo
     (mục 2.3, `idYeuCau`).

**Id và mã.** Bác sĩ làm việc bằng `id` của lịch hẹn (số). `maTraCuu` (`ECL-yyyyMMdd-nnnn`) in trên phiếu; các route
`tra-cuu` nhận `maTraCuu`, mã phiếu khám hoặc cả link phiếu khám (nội dung mã QR). Backend không trả mã phiếu khám cho bác
sĩ, trừ khi cấp lại liên kết bằng `MAN_HINH`.

**Tất cả route của mục 3**

| Phương thức | Đường dẫn | Quyền | Mục đích |
|---|---|---|---|
| GET | `/api/booking/bac-si/toi/tong-quan` | BAC_SI | Số liệu Dashboard |
| GET | `/api/booking/bac-si/toi/lich-lam-viec` | BAC_SI | Ca làm việc của tôi theo khoảng ngày |
| GET | `/api/booking/bac-si/toi/lich-lam-viec/pdf` | BAC_SI | Tải PDF lịch làm việc |
| GET | `/api/catalog/phong-kham` | BAC_SI, QUAN_TRI_VIEN | Phòng khám đang hoạt động |
| GET | `/api/booking/bac-si/toi/lich-hen` | BAC_SI | Lịch hẹn của tôi trong 1 ngày |
| GET | `/api/booking/bac-si/toi/lich-hen/lich` | BAC_SI | Lịch hẹn của tôi theo khoảng ngày |
| GET | `/api/booking/bac-si/toi/lich-hen/tra-cuu` | BAC_SI | Tìm 1 lịch hẹn theo mã / QR |
| GET | `/api/booking/bac-si/toi/lich-hen/theo-cccd` | BAC_SI | Lịch hẹn của tôi theo CCCD |
| POST | `/api/booking/bac-si/toi/lich-hen/{id}/huy` | BAC_SI | Hủy 1 lịch hẹn của tôi |
| POST | `/api/booking/bac-si/toi/lich-hen/{id}/cap-lai-lien-ket` | BAC_SI | Cấp lại link phiếu khám |
| GET | `/api/booking/bac-si/toi/hang-doi` | BAC_SI | Hàng đợi khám 1 ngày |
| POST | `/api/booking/bac-si/toi/hang-doi/goi-tiep` | BAC_SI | Gọi bệnh nhân kế tiếp của 1 ca |
| POST | `/api/booking/bac-si/toi/lich-hen/{id}/goi-kham` | BAC_SI | Gọi 1 bệnh nhân vào khám |
| POST | `/api/booking/bac-si/toi/lich-hen/{id}/vang-mat` | BAC_SI | Đánh dấu vắng mặt |
| GET | `/api/booking/bac-si/toi/lich-hen/{id}/ho-so-kham` | BAC_SI | Hồ sơ khám từ 1 lịch hẹn |
| GET | `/api/booking/bac-si/toi/lich-hen/tra-cuu/ho-so-kham` | BAC_SI | Hồ sơ khám theo mã / QR |
| PUT | `/api/booking/bac-si/toi/lich-hen/{id}/benh-nhan` | BAC_SI | Sửa hồ sơ bệnh nhân sau đối chiếu |
| POST | `/api/booking/bac-si/toi/lich-hen/{id}/da-doi-chieu` | BAC_SI | Bỏ đánh dấu cần đối chiếu |
| POST | `/api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an` | BAC_SI | Ghi bệnh án (xong lượt khám) |
| PUT | `/api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an` | BAC_SI | Sửa bệnh án |
| GET | `/api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an` | BAC_SI | Xem bệnh án |
| GET | `/api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an/lich-su` | BAC_SI | Lịch sử sửa bệnh án |
| GET | `/api/medical/bac-si/toi/thuoc` | BAC_SI | Gợi ý thuốc |
| GET | `/api/medical/bac-si/toi/icd10` | BAC_SI | Gợi ý mã ICD-10 |
| POST | `/api/booking/bac-si/toi/yeu-cau-doi-lich` | BAC_SI | Gửi yêu cầu đổi ca / xin nghỉ / nhường ca |
| POST | `/api/booking/bac-si/toi/yeu-cau-doi-lich/xin-nghi-nhieu-ca` | BAC_SI | Xin nghỉ nhiều ca 1 lần |
| GET | `/api/booking/bac-si/toi/yeu-cau-doi-lich` | BAC_SI | Yêu cầu tôi đã gửi |
| POST | `/api/booking/bac-si/toi/yeu-cau-doi-lich/nhom/{maNhom}/rut` | BAC_SI | Rút 1 nhóm xin nghỉ |
| POST | `/api/booking/bac-si/toi/yeu-cau-doi-lich/{id}/rut` | BAC_SI | Rút 1 yêu cầu |
| GET | `/api/booking/bac-si/toi/yeu-cau-nhuong-ca` | BAC_SI | Yêu cầu nhường ca gửi cho tôi |
| POST | `/api/booking/bac-si/toi/yeu-cau-nhuong-ca/{id}/dong-y` | BAC_SI | Đồng ý nhận ca |
| POST | `/api/booking/bac-si/toi/yeu-cau-nhuong-ca/{id}/tu-choi` | BAC_SI | Từ chối nhận ca |
| GET | `/api/catalog/bac-si/toi/ho-so` | BAC_SI | Hồ sơ giới thiệu của tôi |
| PUT | `/api/catalog/bac-si/toi/ho-so` | BAC_SI | Sửa phần hồ sơ được phép |
| POST | `/api/catalog/bac-si/toi/anh` | BAC_SI | Thêm ảnh làm việc |
| PUT | `/api/catalog/bac-si/toi/anh/{idAnh}` | BAC_SI | Sửa chú thích ảnh |
| DELETE | `/api/catalog/bac-si/toi/anh/{idAnh}` | BAC_SI | Xoá ảnh làm việc |
| GET | `/api/booking/bac-si/toi/danh-gia/tong-quan` | BAC_SI | Điểm đánh giá của tôi |
| GET | `/api/booking/bac-si/toi/danh-gia` | BAC_SI | Các đánh giá tôi nhận được |

**Lỗi chung của mục 3** (ngoài mục 1.4, không nhắc lại ở từng route): 403 `TAI_KHOAN_BI_VO_HIEU_HOA` khi tài khoản bác
sĩ bị vô hiệu hoá sau khi token được cấp; 403 `KHONG_CO_QUYEN` khi token không phải của bác sĩ.

### 3.2 Mô hình và enum của mục 3

`GioiTinh`, `TrangThaiBacSi`, `TrangThaiTaiKhoan`, `TrangThaiLichHen`: xem [mục 1.7](#17-enum-dùng-chung).

| Enum | Giá trị |
|---|---|
| `BuoiKham` | `SANG` (ca bắt đầu trước 12:00), `CHIEU` (12:00–17:00), `TOI` (từ 17:00) |
| `TrangThaiLichLamViec` | `HOAT_DONG`, `DA_HUY` (ca bị hủy, vd do duyệt xin nghỉ) |
| `KenhDatLich` | `KHACH` (đặt không đăng nhập), `TAI_KHOAN` (bệnh nhân đã đăng nhập), `HOTLINE` (quản trị viên đặt thay qua tổng đài / quầy) |
| `QuanHeGiamHo` | `CHA`, `ME`, `NGUOI_GIAM_HO_HOP_PHAP`, `KHAC` |
| `TrangThaiHangDoi` | `CHO_KHAM` (chưa gọi), `DANG_KHAM` (đã gọi vào; mỗi bác sĩ tối đa 1 người), `VANG_MAT` (gọi không có mặt, gọi lại được; cũng là cách hiện lịch `KHONG_DEN`), `DA_KHAM` (đã ghi bệnh án) |
| `CachCapLai` | `EMAIL` (gửi tới email của lịch hẹn), `MAN_HINH` (trả mã + link để mở mã QR cho người ở quầy; cách duy nhất khi lịch hẹn không có email) |
| `LoaiYeuCauDoiLich` | `DOI_CA`, `XIN_NGHI`, `NHUONG_CA` (nhường cho bác sĩ cùng chuyên khoa) |
| `TrangThaiYeuCau` | `CHO_DONG_Y` (nhường ca: chờ bác sĩ nhận), `CHO_DUYET` (chờ quản trị viên), `DA_DUYET`, `TU_CHOI` (quản trị viên từ chối), `DA_RUT` (tôi đã rút), `DONG_NGHIEP_TU_CHOI` (bác sĩ nhận từ chối) |
| `DuongDungThuoc` | `UONG`, `NGAM_DUOI_LUOI`, `TIEM`, `BOI_NGOAI_DA`, `NHO_MAT`, `NHO_TAI`, `NHO_MUI`, `XIT_HIT`, `DAT`, `KHAC` (nhãn hiển thị do frontend đặt) |
| `TruongBenhAn` | `LY_DO_KHAM`, `SINH_HIEU`, `KHAM_LAM_SANG`, `CHAN_DOAN` (chữ hoặc mã ICD-10), `CHAN_DOAN_KEM_THEO`, `GHI_CHU`, `NGAY_TAI_KHAM`, `DON_THUOC` (nhãn do frontend đặt) |
| `LoaiAnhBacSi` | `ANH_CONG_VIEC` (bác sĩ tự thêm / sửa / xoá), `CHUNG_CHI` (chỉ quản trị viên sửa / xoá) |

#### `BacSiTomTatResponse`

`{ id, hoTen, hocVi, anhDaiDien }`. `hocVi`, `anhDaiDien` (URL) có thể null.

#### `PhongKhamTomTatResponse`

`{ id, tenPhong, tang }`, theo ngôn ngữ của request. `tang` có thể null.

#### `CaLamViecResponse`

1 ca làm việc, có cả ca đã hủy.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `idLichLamViec` | số | id ca |
| `ngay` | ngày | |
| `gioBatDau`, `gioKetThuc` | giờ | |
| `soLuotToiDaMoiGio`, `thoiLuongLuotPhut` | số | số lượt mỗi giờ, độ dài 1 lượt (phút) |
| `trangThai` | `TrangThaiLichLamViec` | |
| `bacSi` | [`BacSiTomTatResponse`](#bacsitomtatresponse) | |
| `tenChuyenKhoa` | chuỗi | |
| `phongKham` | [`PhongKhamTomTatResponse`](#phongkhamtomtatresponse) | |
| `tongSoLuot` | số | số lượt của ca, không tính lượt đã hủy |
| `soLuotDaDat`, `soLuotConTrong` | số | đã có người đặt; còn trống (kể cả lượt đã qua giờ) |

#### `LichHenTrongCaResponse`

1 lịch hẹn trong danh sách của bác sĩ. **Số điện thoại không che**; không có mã phiếu khám, không có CCCD.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | số | id lịch hẹn, dùng cho mọi route `lich-hen/{id}` |
| `maTraCuu` | chuỗi | mã in trên phiếu, vd `ECL-20261025-4198` |
| `idLichLamViec` | số | ca của lịch hẹn |
| `trangThai` | `TrangThaiLichHen` | |
| `soThuTu` | số | số thứ tự trong ca |
| `gioKhamDuKien` | ngày giờ | giờ bắt đầu lượt khám |
| `gioBatDauKhung`, `gioKetThucKhung` | ngày giờ | khung giờ bệnh nhân đã chọn |
| `bacSi` | object \| null | luôn `null` ở các route của bác sĩ |
| `phongKham` | [`PhongKhamTomTatResponse`](#phongkhamtomtatresponse) | |
| `lyDoKham` | chuỗi \| null | người đặt ghi khi đặt |
| `ngayDat` | ngày giờ | |
| `idHoSoBenhNhan` | số | |
| `benhNhan` | object | `{ hoTen, ngaySinh, tuoi, gioiTinh, soDienThoai }` theo hồ sơ; `tuoi` tròn vào ngày khám (null nếu không có ngày sinh); `soDienThoai` null khi có người giám hộ |
| `nguoiGiamHo` | object \| null | `{ hoTen, quanHe, soDienThoai }` (`quanHe`: `QuanHeGiamHo`) |
| `doiChieu` | object | `{ canDoiChieu, hoTenDaNhap, ngaySinhDaNhap, gioiTinhDaNhap, hoTenGiamHoDaNhap }`: những gì người đặt nhập. `canDoiChieu = true`: họ tên / ngày sinh nhập khác hồ sơ và chưa đối chiếu → hiện cảnh báo, xử lý ở mục 3.6 |
| `lyDoHuy` | chuỗi \| null | có ở `BI_TU_CHOI` và các trạng thái hủy |
| `canDoiLich` | boolean | `true`: ca đã bị hủy, lịch còn hiệu lực và đang chờ bệnh nhân đổi khung giờ (không khám được) |
| `kenhDat` | `KenhDatLich` | |
| `chuyenKhoa` | object \| null | luôn `null` ở các route của bác sĩ |

```json
{
  "id": 812, "maTraCuu": "ECL-20261025-4198", "idLichLamViec": 55, "trangThai": "DA_XAC_NHAN", "soThuTu": 3,
  "gioKhamDuKien": "2026-10-25T08:30:00", "gioBatDauKhung": "2026-10-25T08:00:00", "gioKetThucKhung": "2026-10-25T09:00:00",
  "bacSi": null, "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" },
  "lyDoKham": "Ho kéo dài", "ngayDat": "2026-10-20T21:14:05", "idHoSoBenhNhan": 301,
  "benhNhan": { "hoTen": "Nguyễn Văn An", "ngaySinh": "1990-05-12", "tuoi": 36, "gioiTinh": "NAM", "soDienThoai": "0901234001" },
  "nguoiGiamHo": null,
  "doiChieu": { "canDoiChieu": false, "hoTenDaNhap": "Nguyễn Văn An", "ngaySinhDaNhap": "1990-05-12", "gioiTinhDaNhap": "NAM", "hoTenGiamHoDaNhap": null },
  "lyDoHuy": null, "canDoiLich": false, "kenhDat": "KHACH", "chuyenKhoa": null
}
```

#### `KetQuaKhamResponse`

Kết quả khám nằm **trong hồ sơ khám** (mục 3.6). Khác [`BenhAnResponse`](#benhanresponse): không có `id`, `maSoKham`,
`linkSoKham`.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `lyDoKham`, `khamLamSang`, `chanDoan`, `chanDoanKemTheo`, `ghiChu` | chuỗi \| null | `ghiChu`: lời dặn; `chanDoan` luôn có |
| `sinhHieu` | object \| null | `{ mach, nhietDo, huyetApTamThu, huyetApTamTruong, nhipTho, canNang, chieuCao }` (lần/phút, °C, mmHg, kg, cm); null nếu không đo chỉ số nào, chỉ số không đo là null |
| `icdChinh` | object \| null | `{ ma, ten, manTinh }` |
| `ngayTaiKhamDeXuat` | ngày \| null | chỉ là gợi ý, không phải lịch hẹn |
| `donThuoc` | mảng | `{ idThuoc, tenThuoc, hoatChat, hamLuong, duongDung, donVi, lieuDung, soLanMoiNgay, soNgayDung, soLuong, ghiChuSuDung }`; rỗng nếu không kê |
| `ngaySuaCuoi` | ngày giờ \| null | null = chưa sửa |
| `phienBan` | số | gửi lại khi sửa bệnh án |
| `hanSua` | ngày giờ | sau lúc này bệnh án chỉ xem được |

#### `HoSoKhamResponse`

Hồ sơ khám mở từ 1 lịch hẹn của tôi. **CCCD và số điện thoại không che** (bác sĩ đối chiếu giấy tờ).

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `lichHen` | [`LichHenTrongCaResponse`](#lichhentrongcaresponse) | lịch hẹn đang mở |
| `hoSoBenhNhan` | object | `{ id, cccd, hoTen, ngaySinh, tuoi, gioiTinh, soDienThoai, diaChi, soBaoHiemYTe, tienSuBenhLy, daLienKetTaiKhoan }`. `cccd` null với trẻ chưa có CCCD; `daLienKetTaiKhoan = true`: chủ tài khoản tự sửa được hồ sơ |
| `ketQua` | [`KetQuaKhamResponse`](#ketquakhamresponse) \| null | null khi chưa ghi bệnh án |
| `lanKhamTruoc` | mảng | các lịch hẹn khác của bệnh nhân (với mọi bác sĩ) có giờ khám trước lịch này, mới nhất trước: `{ idLichHen, maTraCuu, ngay, gioKhamDuKien, bacSi, tenChuyenKhoa, trangThai, lyDoKham, ketQua }` (`bacSi`: [`BacSiTomTatResponse`](#bacsitomtatresponse), `ketQua`: [`KetQuaKhamResponse`](#ketquakhamresponse) \| null) |

#### `BenhAnResponse`

Bệnh án do các route `/api/medical/bac-si/toi/…/benh-an` trả.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id`, `idLichHen` | số | |
| `maTraCuu` | chuỗi | |
| `trangThaiLichHen` | `TrangThaiLichHen` | `DA_HOAN_THANH` sau khi ghi |
| `lyDoKham`, `khamLamSang`, `chanDoan`, `chanDoanKemTheo`, `ghiChu` | chuỗi \| null | như [`KetQuaKhamResponse`](#ketquakhamresponse) |
| `sinhHieu` | object \| null | như `KetQuaKhamResponse` |
| `icdChinh` | object \| null | `{ ma, ten, manTinh }` |
| `ngayTaiKhamDeXuat` | ngày \| null | |
| `donThuoc` | mảng | như `KetQuaKhamResponse` |
| `ngayTao` | ngày giờ | lúc ghi nhận |
| `phienBan` | số | 1 khi ghi, +1 mỗi lần sửa; gửi lại khi sửa |
| `ngaySuaCuoi` | ngày giờ \| null | |
| `hanSua` | ngày giờ | `ngayTao` + 7 ngày (mặc định, biến `RECORD_EDIT_DAYS`) |
| `maSoKham` | chuỗi | mã sổ khám bệnh của lượt khám (khác mã phiếu khám) |
| `linkSoKham` | chuỗi | link trang sổ khám công khai, cũng là nội dung QR (`GET /api/booking/so-kham/{maSoKham}/qr`). Bệnh nhân mở bằng 4 số cuối SĐT |

#### `YeuCauDoiLichResponse`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | số | |
| `loaiYeuCau` | `LoaiYeuCauDoiLich` | |
| `trangThai` | `TrangThaiYeuCau` | |
| `lyDo` | chuỗi | |
| `bacSi` | [`BacSiTomTatResponse`](#bacsitomtatresponse) | bác sĩ đã gửi |
| `ca` | [`CaLamViecResponse`](#calamviecresponse) | ca của yêu cầu, theo trạng thái hiện tại; `soLuotDaDat` > 0 nghĩa là duyệt thì các lịch hẹn đó phải đổi lịch (hoặc chuyển sang bác sĩ nhận). Sau khi duyệt nhường ca, `ca.bacSi` là bác sĩ nhận |
| `ngayMongMuon`, `gioBatDauMongMuon`, `gioKetThucMongMuon` | ngày, giờ \| null | chỉ có với `DOI_CA` |
| `phongKhamMongMuon` | [`PhongKhamTomTatResponse`](#phongkhamtomtatresponse) \| null | `DOI_CA`; null = giữ phòng hiện tại |
| `bacSiNhan` | [`BacSiTomTatResponse`](#bacsitomtatresponse) \| null | chỉ có với `NHUONG_CA` |
| `ghiChuBacSiNhan` | chuỗi \| null | `NHUONG_CA`: ghi chú khi bác sĩ nhận từ chối |
| `ngayBacSiNhanPhanHoi` | ngày giờ \| null | `NHUONG_CA`: lúc bác sĩ nhận đồng ý / từ chối |
| `maNhom` | chuỗi \| null | chung cho các yêu cầu xin nghỉ gửi 1 lần (UUID); null với yêu cầu lẻ |
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

#### `HoSoBacSiQuanTriResponse`

Hồ sơ giới thiệu đầy đủ (cùng bản quản trị viên xem). Các mục giới thiệu trả **chữ đã lưu của cả 2 ngôn ngữ** (trường
`…En` là bản tiếng Anh, null / mảng rỗng khi chưa nhập), không đổi theo `Accept-Language`; riêng `tenChuyenKhoa` theo
ngôn ngữ của request.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id`, `maBacSi` | số, chuỗi | |
| `hoTen`, `email`, `soDienThoai`, `anhDaiDien` | chuỗi \| null | từ tài khoản; sửa ở `/api/users/me` (mục 2.2) |
| `soGiayPhep`, `ngayCapGiayPhep`, `noiCapGiayPhep`, `noiCapGiayPhepEn`, `phamViHanhNghe`, `phamViHanhNgheEn` | chuỗi / ngày \| null | quản trị viên sửa |
| `trangThai` | `TrangThaiBacSi` | |
| `trangThaiTaiKhoan` | `TrangThaiTaiKhoan` | |
| `phaiDoiMatKhau` | boolean | |
| `hocVi`, `chucVu`, `chucVuEn` | chuỗi \| null | quản trị viên sửa |
| `namBatDauHanhNghe`, `soNamKinhNghiem` | số \| null | `soNamKinhNghiem` suy ra từ năm bắt đầu |
| `ngonNguKham` | mảng chuỗi | mã ISO 639 viết thường (`"vi"`, `"en"`) — **bác sĩ tự sửa** |
| `gioiThieuNgan`, `gioiThieuNganEn`, `tieuSu`, `tieuSuEn` | chuỗi \| null | **bác sĩ tự sửa** |
| `idChuyenKhoa`, `tenChuyenKhoa` | số, chuỗi | |
| `quaTrinhDaoTao(En)`, `quaTrinhCongTac(En)`, `linhVucKhamChua(En)` | mảng chuỗi | quản trị viên sửa |
| `giaiThuong(En)`, `congTrinhNghienCuu(En)` | mảng chuỗi | **bác sĩ tự sửa** |
| `phienBanHoSo` | số | gửi lại ở `phienBan` khi lưu; +1 mỗi lần lưu |
| `anh` | mảng [`AnhBacSiQuanTriResponse`](#anhbacsiquantriresponse) | theo thứ tự hiển thị |

#### `AnhBacSiQuanTriResponse`

`{ id, loai, url, chuThich, chuThichEn }`: `loai` là `LoaiAnhBacSi`; chú thích của cả 2 ngôn ngữ, có thể null.

### 3.3 Tổng quan, lịch làm việc, phòng khám

#### GET /api/booking/bac-si/toi/tong-quan

**Quyền:** BAC_SI. Các ô số liệu của Dashboard, 1 lần gọi.

**Response:**

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `lichHenHomNay` | số | lịch hẹn của tôi có giờ khám hôm nay, không tính lịch đã hủy / bị hủy |
| `dangCho` | số | trong số đó: `DA_XAC_NHAN`, không chờ đổi lịch (chưa khám) |
| `daKham` | số | trong số đó: `DA_HOAN_THANH` |
| `diemDanhGia` | số \| null | điểm trung bình mọi thời gian, 1 chữ số thập phân; null khi chưa có đánh giá |
| `soDanhGia` | số | |
| `lichHenSapToi` | số | lịch `DA_XAC_NHAN` có lượt khám bắt đầu sau thời điểm tính |
| `tinhLuc` | ngày giờ | |

```json
{ "lichHenHomNay": 14, "dangCho": 9, "daKham": 5, "diemDanhGia": 4.7, "soDanhGia": 36, "lichHenSapToi": 41, "tinhLuc": "2026-10-21T10:15:02" }
```

#### GET /api/booking/bac-si/toi/lich-lam-viec

**Quyền:** BAC_SI. Các ca của tôi trong khoảng ngày, **kể cả ca đã hủy** (`trangThai: "DA_HUY"`), kèm số lượt đã đặt.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuNgay`, `denNgay` | ngày | có | khoảng tối đa 42 ngày (lưới tháng 6 tuần) |

**Response:** `duLieu` = mảng [`CaLamViecResponse`](#calamviecresponse), theo ngày và giờ bắt đầu.

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (`denNgay` trước `tuNgay`, khoảng quá 42 ngày).

#### GET /api/booking/bac-si/toi/lich-lam-viec/pdf

**Quyền:** BAC_SI. Tải bản in PDF lịch làm việc của tôi, chỉ ca còn hoạt động, chữ theo `Accept-Language`.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuNgay`, `denNgay` | ngày | có | khoảng tối đa 42 ngày |

**Response:** tệp `application/pdf`, không bọc khung, tên tệp ở `Content-Disposition`
(`lich-lam-viec_<maBacSi>_<tuNgay>_<denNgay>.pdf`). Gọi bằng `fetch` kèm token rồi tải `blob` (thẻ `<a href>` không gửi
được header `Authorization`).

**Lỗi:** trả JSON như các route khác: 400 `DU_LIEU_KHONG_HOP_LE` (khoảng ngày sai).

#### GET /api/catalog/phong-kham

**Quyền:** BAC_SI, QUAN_TRI_VIEN. Phòng khám đang hoạt động, theo tên phòng: bác sĩ chọn phòng mong muốn khi xin đổi ca.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idChuyenKhoa` | số | không | chỉ lấy phòng của 1 chuyên khoa (khi xin đổi ca: chuyên khoa của tôi) |

**Response:** `duLieu` = mảng `{ id, tenPhong, tang, idChuyenKhoa, tenChuyenKhoa }` (`tang` có thể null).

```json
[ { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2", "idChuyenKhoa": 1, "tenChuyenKhoa": "Nội tổng quát" } ]
```

### 3.4 Lịch hẹn của tôi

#### GET /api/booking/bac-si/toi/lich-hen

**Quyền:** BAC_SI. Lịch hẹn của tôi trong 1 ngày, theo giờ khám, mọi trạng thái, kèm thông tin bệnh nhân.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `ngay` | ngày | không | mặc định hôm nay |
| `idLichLamViec` | số | không | chỉ 1 ca |
| `soThuTu` | số | không | đúng số thứ tự |
| `tuKhoa` | chuỗi | không | 1 phần họ tên bệnh nhân (trong hồ sơ hoặc người đặt đã nhập), không phân biệt dấu, hoa / thường |

**Response:** `duLieu` = mảng [`LichHenTrongCaResponse`](#lichhentrongcaresponse).

#### GET /api/booking/bac-si/toi/lich-hen/lich

**Quyền:** BAC_SI. Lịch hẹn của tôi theo khoảng ngày cho màn hình lịch: mọi trạng thái, theo giờ khám, không phân trang.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuNgay`, `denNgay` | ngày | có | khoảng tối đa 42 ngày |

**Response:** `duLieu` = mảng [`LichHenTrongCaResponse`](#lichhentrongcaresponse).

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (`denNgay` trước `tuNgay`, khoảng quá 42 ngày).

#### GET /api/booking/bac-si/toi/lich-hen/tra-cuu

**Quyền:** BAC_SI. Tìm 1 lịch hẹn của tôi theo mã.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `ma` | chuỗi | có | `maTraCuu` (không phân biệt hoa thường), mã phiếu khám, hoặc cả link phiếu khám (nội dung QR, phần `?…` / `#…` bị bỏ) |

**Response:** `duLieu` = [`LichHenTrongCaResponse`](#lichhentrongcaresponse).

**Lỗi:** 404 `KHONG_TIM_THAY` (mã sai dạng, không có, hoặc lịch hẹn của bác sĩ khác).

#### GET /api/booking/bac-si/toi/lich-hen/theo-cccd

**Quyền:** BAC_SI. Lịch hẹn của tôi (mọi trạng thái, tối đa 50, giờ khám muộn nhất trước) mà người khám **hoặc người giám
hộ** có số CCCD này: tìm lịch hẹn của người đến quầy xin cấp lại link.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `cccd` | chuỗi | có | đúng 12 chữ số |

**Response:** `duLieu` = mảng [`LichHenTrongCaResponse`](#lichhentrongcaresponse) (rỗng nếu không có).

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (CCCD sai dạng).

#### POST /api/booking/bac-si/toi/lich-hen/{id}/huy

**Quyền:** BAC_SI. Hủy 1 lịch hẹn đã xác nhận của tôi, trước giờ khám. Lịch thành `BI_TU_CHOI`, lượt khám được mở lại cho
người khác đặt; bệnh nhân được báo và đọc lý do trên phiếu khám.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `lyDo` | chuỗi | có | tối đa 500; bệnh nhân đọc được |

**Response:** `duLieu` = [`LichHenTrongCaResponse`](#lichhentrongcaresponse) (`trangThai: "BI_TU_CHOI"`, `lyDoHuy`).

**Lỗi:** 404 `KHONG_TIM_THAY`; 409 `LICH_HEN_KHONG_HUY_DOI_DUOC` (không còn `DA_XAC_NHAN`), `LICH_HEN_CAN_DOI_LICH`
(đang chờ bệnh nhân đổi lịch vì ca bị hủy), `LICH_HEN_DA_QUA_GIO` (đã tới giờ khám).

#### POST /api/booking/bac-si/toi/lich-hen/{id}/cap-lai-lien-ket

**Quyền:** BAC_SI. Cấp lại link phiếu khám cho người đến quầy đã mất link (như quản trị viên). Luôn dùng **lịch hẹn hiện
hành** của chuỗi đổi lịch (có thể khác `{id}` đã chọn) và giữ mã cũ.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `cccd` | chuỗi | có | 12 chữ số, trên giấy tờ người đó xuất trình: của bệnh nhân hoặc của người giám hộ ghi trên lịch hẹn |
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
{ "idLichHen": 812, "maTraCuu": "ECL-20261025-4198", "cachGui": "MAN_HINH",
  "maPhieuKham": "Qm3v8xYb2LwTn0pRk7sJdA4fHc9eUgZi1oVqXyWtE5M",
  "lienKetPhieuKham": "http://localhost:5173/phieu-kham/Qm3v8xYb2LwTn0pRk7sJdA4fHc9eUgZi1oVqXyWtE5M" }
```

**Lỗi:** 404 `KHONG_TIM_THAY`; 403 `CCCD_KHONG_KHOP`; 409 `KHONG_CO_EMAIL_NHAN` (`EMAIL` mà lịch hẹn không có email: dùng
`MAN_HINH`).

### 3.5 Hàng đợi khám

Hàng đợi theo từng ca còn hoạt động trong ngày. Mỗi thao tác (`goi-tiep`, `goi-kham`, `vang-mat`) trả lại **hàng đợi hôm
nay**, frontend thay cả màn hình bằng phản hồi. Chỉ thao tác được với lịch hẹn của hôm nay; ghi bệnh án (mục 3.7) là xong
lượt (`DA_KHAM`). Lịch chờ đổi lịch (`canDoiLich`) không có trong hàng đợi.

#### GET /api/booking/bac-si/toi/hang-doi

**Quyền:** BAC_SI. Hàng đợi 1 ngày của tôi.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `ngay` | ngày | không | mặc định hôm nay |

**Response:**

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `ngay` | ngày | |
| `idCaHienTai` | số \| null | ca nên mở sẵn: hôm nay là ca đang diễn ra, không có thì ca kế tiếp, không có nữa thì ca cuối; ngày khác là ca đầu tiên; null khi không có ca |
| `tinhLuc` | ngày giờ | lúc máy chủ tính |
| `cacCa` | mảng | theo giờ bắt đầu: `{ idLichLamViec, buoi, gioBatDau, gioKetThuc, phongKham, soLuong, idDangKham, idTiepTheo, cacLuot }` |
| `cacCa[].soLuong` | object | `{ tong, choKham, dangKham, vangMat, daKham }` |
| `cacCa[].idDangKham` | số \| null | id lịch hẹn đang khám |
| `cacCa[].idTiepTheo` | số \| null | id lịch hẹn sẽ được `goi-tiep` gọi (`CHO_KHAM` có số thứ tự nhỏ nhất); chỉ có khi là hôm nay |
| `cacCa[].cacLuot` | mảng | theo số thứ tự: `{ trangThaiHangDoi, goiLuc, lichHen }` (`trangThaiHangDoi`: `TrangThaiHangDoi`; `goiLuc`: lần gọi gần nhất, null nếu chưa gọi; `lichHen`: [`LichHenTrongCaResponse`](#lichhentrongcaresponse)) |

```json
{
  "ngay": "2026-10-21", "idCaHienTai": 55, "tinhLuc": "2026-10-21T08:40:11",
  "cacCa": [ {
    "idLichLamViec": 55, "buoi": "SANG", "gioBatDau": "08:00:00", "gioKetThuc": "11:00:00",
    "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" },
    "soLuong": { "tong": 2, "choKham": 1, "dangKham": 1, "vangMat": 0, "daKham": 0 },
    "idDangKham": 812, "idTiepTheo": 815,
    "cacLuot": [
      { "trangThaiHangDoi": "DANG_KHAM", "goiLuc": "2026-10-21T08:31:02", "lichHen": { "id": 812, "soThuTu": 1, "trangThai": "DA_XAC_NHAN" } },
      { "trangThaiHangDoi": "CHO_KHAM", "goiLuc": null, "lichHen": { "id": 815, "soThuTu": 2, "trangThai": "DA_XAC_NHAN" } }
    ]
  } ]
}
```

(`lichHen` rút gọn trong ví dụ; thực tế là đủ trường của `LichHenTrongCaResponse`.)

#### POST /api/booking/bac-si/toi/hang-doi/goi-tiep

**Quyền:** BAC_SI. Gọi bệnh nhân `CHO_KHAM` có số thứ tự nhỏ nhất của 1 ca hôm nay của tôi (thành `DANG_KHAM`).

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idLichLamViec` | số | có | ca hôm nay, còn hoạt động |

**Response:** `duLieu` = hàng đợi hôm nay (như `GET …/hang-doi`).

**Lỗi:** 404 `KHONG_TIM_THAY` (ca không có, đã hủy, của bác sĩ khác); 409 `KHONG_PHAI_NGAY_KHAM` (ca không phải hôm nay),
`HANG_DOI_TRONG` (không còn ai chờ), `DANG_KHAM_BENH_NHAN_KHAC` (đang khám 1 người khác: ghi bệnh án hoặc đánh vắng mặt
người đó trước; `thongDiep` nêu tên và số thứ tự).

#### POST /api/booking/bac-si/toi/lich-hen/{id}/goi-kham

**Quyền:** BAC_SI. Gọi 1 bệnh nhân vào khám: người đang chờ, hoặc gọi lại người vắng mặt. Gọi lại người đang khám vẫn 200
(không đổi `goiLuc`).

**Request:** path `id` (lịch hẹn).

**Response:** `duLieu` = hàng đợi hôm nay.

**Lỗi:** 404 `KHONG_TIM_THAY`; 409 `KHONG_PHAI_NGAY_KHAM`, `LICH_HEN_KHONG_KHAM_DUOC` (không còn `DA_XAC_NHAN` hoặc đang chờ
đổi lịch), `DANG_KHAM_BENH_NHAN_KHAC`.

#### POST /api/booking/bac-si/toi/lich-hen/{id}/vang-mat

**Quyền:** BAC_SI. Đánh dấu vắng mặt (gọi lại được sau bằng `goi-kham`). Đã vắng mặt vẫn 200.

**Request:** path `id` (lịch hẹn).

**Response:** `duLieu` = hàng đợi hôm nay.

**Lỗi:** 404 `KHONG_TIM_THAY`; 409 `KHONG_PHAI_NGAY_KHAM`, `LICH_HEN_KHONG_KHAM_DUOC`.

### 3.6 Hồ sơ khám của bệnh nhân

Mở được từ **lịch hẹn của tôi** (mọi trạng thái). Lịch hẹn không có và lịch hẹn của bác sĩ khác trả cùng 404
`KHONG_TIM_THAY`.

#### GET /api/booking/bac-si/toi/lich-hen/{id}/ho-so-kham

**Quyền:** BAC_SI. Hồ sơ khám của bệnh nhân từ 1 lịch hẹn: lịch hẹn, hồ sơ bệnh nhân đầy đủ, kết quả khám (nếu đã ghi) và
các lần khám trước.

**Request:** path `id` (lịch hẹn).

**Response:** `duLieu` = [`HoSoKhamResponse`](#hosokhamresponse).

```json
{
  "lichHen": { "id": 812, "maTraCuu": "ECL-20261025-4198", "trangThai": "DA_XAC_NHAN" },
  "hoSoBenhNhan": { "id": 301, "cccd": "079090001001", "hoTen": "Nguyễn Văn An", "ngaySinh": "1990-05-12", "tuoi": 36,
    "gioiTinh": "NAM", "soDienThoai": "0901234001", "diaChi": "12 Lê Lợi, Q.1", "soBaoHiemYTe": null,
    "tienSuBenhLy": "Tăng huyết áp", "daLienKetTaiKhoan": true },
  "ketQua": null,
  "lanKhamTruoc": [ { "idLichHen": 640, "maTraCuu": "ECL-20260912-0071", "ngay": "2026-09-12",
    "gioKhamDuKien": "2026-09-12T09:15:00", "bacSi": { "id": 9, "hoTen": "Lê Minh Châu", "hocVi": "BS.CKI", "anhDaiDien": null },
    "tenChuyenKhoa": "Tim mạch", "trangThai": "DA_HOAN_THANH", "lyDoKham": "Đau ngực", "ketQua": null } ]
}
```

(`lichHen` rút gọn trong ví dụ.)

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### GET /api/booking/bac-si/toi/lich-hen/tra-cuu/ho-so-kham

**Quyền:** BAC_SI. Như route trên nhưng tìm theo mã: quét QR phiếu khám của bệnh nhân là mở thẳng hồ sơ.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `ma` | chuỗi | có | `maTraCuu`, mã phiếu khám hoặc link phiếu khám (như `GET …/lich-hen/tra-cuu`) |

**Response:** `duLieu` = [`HoSoKhamResponse`](#hosokhamresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### PUT /api/booking/bac-si/toi/lich-hen/{id}/benh-nhan

**Quyền:** BAC_SI. Sửa hồ sơ bệnh nhân của 1 lịch hẹn của tôi sau khi đối chiếu giấy tờ. Gửi đủ các trường: trường không
bắt buộc gửi trống thì **bị xoá trắng**. Sửa xong vẫn phải gọi `da-doi-chieu` nếu `canDoiChieu` đang `true`.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `hoTen` | chuỗi | có | tối đa 150 |
| `ngaySinh` | ngày | có | không ở tương lai |
| `gioiTinh` | `GioiTinh` | không | |
| `soDienThoai` | chuỗi | có | `^0\d{9}$` |
| `cccd` | chuỗi | không | 12 chữ số. Chỉ điền được khi hồ sơ **chưa có** CCCD; hồ sơ đã có thì bỏ trống hoặc gửi đúng số đang lưu |
| `diaChi` | chuỗi | không | tối đa 500 |
| `soBaoHiemYTe` | chuỗi | không | tối đa 50 |

**Response:** `duLieu` = [`HoSoKhamResponse`](#hosokhamresponse) sau khi sửa.

**Lỗi:** 404 `KHONG_TIM_THAY`; 400 `DU_LIEU_KHONG_HOP_LE` (cả khi đổi CCCD đã có); 409 `XUNG_DOT_DU_LIEU` (CCCD trùng hồ sơ
khác).

#### POST /api/booking/bac-si/toi/lich-hen/{id}/da-doi-chieu

**Quyền:** BAC_SI. Đã đối chiếu giấy tờ: bỏ đánh dấu `doiChieu.canDoiChieu` của lịch hẹn. Gọi lại vẫn 200.

**Request:** path `id` (lịch hẹn), không body.

**Response:** không có `duLieu`, chỉ `thongDiep`.

**Lỗi:** 404 `KHONG_TIM_THAY`.

### 3.7 Bệnh án, gợi ý thuốc và ICD-10

Bệnh án thuộc medical-service; `{idLichHen}` là `id` của lịch hẹn ở mục 3.4–3.6. Chỉ bác sĩ của lịch hẹn ghi / sửa / xem
được (lịch hẹn của bác sĩ khác: 404).

**Body chung của `POST` và `PUT`** (`BenhAnRequest`). Trường không bắt buộc: bỏ trống hoặc gửi `""` đều được (lưu thành
null).

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `lyDoKham` | chuỗi | không | tối đa 2000; điền sẵn `lichHen.lyDoKham` |
| `sinhHieu` | object | không | xem bảng dưới; bỏ trống = không đo |
| `khamLamSang` | chuỗi | không | tối đa 5000 |
| `chanDoan` | chuỗi | có | tối đa 5000; chẩn đoán chính, chữ tự do |
| `maIcdChinh` | chuỗi | không | tối đa 10; mã trong danh mục (`GET …/icd10`), không phân biệt hoa thường. Mã `manTinh` cho kê mỗi thuốc tới 90 ngày thay vì 30 |
| `chanDoanKemTheo` | chuỗi | không | tối đa 2000 |
| `ghiChu` | chuỗi | không | tối đa 5000; lời dặn của bác sĩ |
| `ngayTaiKhamDeXuat` | ngày | không | phải sau hôm nay (khi sửa mà giữ nguyên ngày cũ thì được); chỉ là gợi ý, không tạo lịch hẹn |
| `donThuoc` | mảng | không | tối đa 30 dòng, mỗi thuốc 1 dòng; xem bảng dưới. Khi sửa, mảng này **thay toàn bộ** đơn cũ |
| `lyDoSua` | chuỗi | khi sửa (PUT) | tối đa 500; lưu vào lịch sử sửa |
| `phienBan` | số | khi sửa (PUT) | `phienBan` của bệnh án đang xem |

`sinhHieu` (mọi trường không bắt buộc; huyết áp gửi cả 2 số hoặc không số nào, tâm trương nhỏ hơn tâm thu):

| Trường | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `mach` | số | không | 20–250 lần/phút |
| `nhietDo` | số | không | 30.0–45.0 °C, 1 chữ số thập phân |
| `huyetApTamThu` | số | không | 50–300 mmHg |
| `huyetApTamTruong` | số | không | 20–200 mmHg |
| `nhipTho` | số | không | 5–80 lần/phút |
| `canNang` | số | không | 0.3–400 kg, 2 chữ số thập phân. **Bắt buộc** khi trẻ dưới 72 tháng tuổi (tính theo ngày khám) có kê thuốc |
| `chieuCao` | số | không | 20–250 cm, 1 chữ số thập phân |

`donThuoc[]` (thuốc chọn trong danh mục, bác sĩ không tự thêm thuốc):

| Trường | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idThuoc` | số | có | `id` từ `GET …/thuoc`. Khi sửa, gửi lại `idThuoc` của dòng cũ để giữ thuốc đó dù đã ngừng dùng / chưa xác minh |
| `lieuDung` | chuỗi | không | tối đa 100, vd `500mg/lần` |
| `soLanMoiNgay` | số | không | từ 1 |
| `soNgayDung` | số | có | 1–30, hoặc tới 90 khi `maIcdChinh` là mã mạn tính |
| `soLuong` | số | có | 1–9999, tổng số lượng cấp theo `donVi` của thuốc |
| `ghiChuSuDung` | chuỗi | không | tối đa 255, vd `uống sau ăn` |

#### POST /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an

**Quyền:** BAC_SI. Ghi nhận kết quả khám: lưu bệnh án kèm đơn thuốc và chuyển lịch hẹn sang `DA_HOAN_THANH` (xong lượt
trong hàng đợi). Chỉ khi lịch hẹn còn `DA_XAC_NHAN` và ngày khám không ở tương lai (ghi được sau ngày khám).

**Request:** path `idLichHen`, body như bảng trên (không gửi `lyDoSua`, `phienBan`).

```json
{
  "lyDoKham": "Ho kéo dài 2 tuần",
  "sinhHieu": { "mach": 82, "nhietDo": 37.6, "huyetApTamThu": 125, "huyetApTamTruong": 80, "canNang": 64.5 },
  "khamLamSang": "Họng đỏ, phổi không ran",
  "chanDoan": "Viêm họng cấp",
  "maIcdChinh": "J02.9",
  "ghiChu": "Uống nhiều nước, tái khám nếu sốt cao",
  "ngayTaiKhamDeXuat": "2026-11-04",
  "donThuoc": [ { "idThuoc": 15, "lieuDung": "500mg/lần", "soLanMoiNgay": 2, "soNgayDung": 5, "soLuong": 10, "ghiChuSuDung": "uống sau ăn" } ]
}
```

**Response:** HTTP 201, `duLieu` = [`BenhAnResponse`](#benhanresponse). Đưa `linkSoKham` (hoặc ảnh QR của `maSoKham`) cho
bệnh nhân.

```json
{
  "id": 590, "idLichHen": 812, "maTraCuu": "ECL-20261025-4198", "trangThaiLichHen": "DA_HOAN_THANH",
  "lyDoKham": "Ho kéo dài 2 tuần",
  "sinhHieu": { "mach": 82, "nhietDo": 37.6, "huyetApTamThu": 125, "huyetApTamTruong": 80, "nhipTho": null, "canNang": 64.5, "chieuCao": null },
  "khamLamSang": "Họng đỏ, phổi không ran", "chanDoan": "Viêm họng cấp",
  "icdChinh": { "ma": "J02.9", "ten": "Viêm họng cấp, không đặc hiệu", "manTinh": false },
  "chanDoanKemTheo": null, "ghiChu": "Uống nhiều nước, tái khám nếu sốt cao", "ngayTaiKhamDeXuat": "2026-11-04",
  "donThuoc": [ { "idThuoc": 15, "tenThuoc": "Paracetamol", "hoatChat": "Paracetamol", "hamLuong": "500mg", "duongDung": "UONG",
    "donVi": "viên", "lieuDung": "500mg/lần", "soLanMoiNgay": 2, "soNgayDung": 5, "soLuong": 10, "ghiChuSuDung": "uống sau ăn" } ],
  "ngayTao": "2026-10-25T09:02:44", "phienBan": 1, "ngaySuaCuoi": null, "hanSua": "2026-11-01T09:02:44",
  "maSoKham": "s7Hq2LmX9vBn4KcT0wYpR8dJfA3eGu6iZo1VxQyWtE5",
  "linkSoKham": "http://localhost:5173/so-kham/s7Hq2LmX9vBn4KcT0wYpR8dJfA3eGu6iZo1VxQyWtE5"
}
```

**Lỗi:**
- 404 `KHONG_TIM_THAY` (lịch hẹn, hoặc `idThuoc` không có trong danh mục);
- 409 `LICH_HEN_KHONG_KHAM_DUOC` (đã khám xong, đã hủy, không đến), `CHUA_DEN_NGAY_KHAM`;
- 409 `THUOC_NGUNG_DUNG`, `THUOC_CHUA_XAC_MINH` (thuốc thêm mới không kê được; `thongDiep` nêu tên thuốc);
- 400 `DU_LIEU_KHONG_HOP_LE`: 1 thuốc 2 dòng, mã ICD-10 không có trong danh mục, huyết áp thiếu 1 số hoặc tâm trương ≥ tâm
  thu, 1 thuốc quá 30 ngày (90 với mã mạn tính), trẻ dưới 72 tháng có kê thuốc mà thiếu cân nặng, ngày tái khám không sau
  hôm nay.

#### PUT /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an

**Quyền:** BAC_SI. Sửa bệnh án đã ghi, trong `hanSua` (mặc định 7 ngày từ lúc ghi). Gửi lại **toàn bộ nội dung** (như
`POST`) cộng `lyDoSua` và `phienBan` đang xem; mỗi lần sửa thêm 1 dòng lịch sử.

```json
{ "chanDoan": "Viêm họng cấp do liên cầu", "maIcdChinh": "J02.0", "donThuoc": [ { "idThuoc": 15, "soNgayDung": 5, "soLuong": 10 } ],
  "lyDoSua": "Có kết quả test nhanh liên cầu", "phienBan": 1 }
```

(Ví dụ chỉ nêu vài trường; trường không gửi bị xoá trắng như khi ghi.)

**Response:** `duLieu` = [`BenhAnResponse`](#benhanresponse) (`phienBan` tăng 1, `ngaySuaCuoi` có giá trị).

**Lỗi:** như `POST` (trừ 2 lỗi trạng thái lịch hẹn), thêm:
- 404 `KHONG_TIM_THAY` (chưa có bệnh án);
- 409 `HET_HAN_SUA_BENH_AN` (quá `hanSua`: chỉ xem);
- 409 `DU_LIEU_DA_THAY_DOI` (`phienBan` cũ: có người vừa sửa, tải lại bệnh án rồi sửa lại);
- 400 `DU_LIEU_KHONG_HOP_LE` (thiếu `lyDoSua` / `phienBan`, hoặc nội dung không đổi gì).

#### GET /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an

**Quyền:** BAC_SI. Bệnh án của 1 lịch hẹn của tôi, kèm `maSoKham` và `linkSoKham`.

**Request:** path `idLichHen`.

**Response:** `duLieu` = [`BenhAnResponse`](#benhanresponse).

**Lỗi:** 404 `KHONG_TIM_THAY` (lịch hẹn không có / của bác sĩ khác, hoặc chưa ghi bệnh án).

#### GET /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an/lich-su

**Quyền:** BAC_SI. Lịch sử sửa bệnh án của 1 lượt khám, mới nhất trước. Xem được lượt khám **của mình** và lượt khám
trước đó của bệnh nhân mà mình có lịch hẹn từ giờ khám đó trở đi (như `lanKhamTruoc` trong hồ sơ khám).

**Request:** path `idLichHen`.

**Response:**

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `idLichHen`, `maTraCuu` | số, chuỗi | |
| `phienBanHienTai` | số | 1 = chưa sửa lần nào |
| `ngayGhiNhan` | ngày giờ | lúc ghi (phiên bản 1) |
| `cacLanSua` | mảng | mới nhất trước, rỗng nếu chưa sửa: `{ phienBan, ngaySua, bacSi, lyDo, cacTruongDoi, truoc, sau }` |
| `cacLanSua[].bacSi` | object | `{ id, hoTen }` |
| `cacLanSua[].cacTruongDoi` | mảng `TruongBenhAn` | phần đã đổi, theo thứ tự trên hồ sơ |
| `cacLanSua[].truoc`, `sau` | object | toàn bộ nội dung để so: `{ lyDoKham, sinhHieu, khamLamSang, chanDoan, maIcdChinh, chanDoanKemTheo, ghiChu, ngayTaiKhamDeXuat, donThuoc }`; `donThuoc[]` = `{ idThuoc, tenThuoc, hamLuong, lieuDung, soLanMoiNgay, soNgayDung, soLuong, ghiChuSuDung }`. Lịch sử cũ có thể thiếu một số trường (null) |

```json
{ "idLichHen": 812, "maTraCuu": "ECL-20261025-4198", "phienBanHienTai": 2, "ngayGhiNhan": "2026-10-25T09:02:44",
  "cacLanSua": [ { "phienBan": 2, "ngaySua": "2026-10-26T14:10:00", "bacSi": { "id": 7, "hoTen": "Trần Thị Bình" },
    "lyDo": "Có kết quả test nhanh liên cầu", "cacTruongDoi": [ "CHAN_DOAN" ],
    "truoc": { "chanDoan": "Viêm họng cấp", "maIcdChinh": "J02.9" }, "sau": { "chanDoan": "Viêm họng cấp do liên cầu", "maIcdChinh": "J02.0" } } ] }
```

(`truoc` / `sau` rút gọn trong ví dụ.)

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### GET /api/medical/bac-si/toi/thuoc

**Quyền:** BAC_SI. Gợi ý thuốc kê được (đang dùng, đã xác minh) theo 1 phần tên hoặc hoạt chất, tối đa 20.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuKhoa` | chuỗi | không | trống → mảng rỗng |

**Response:** `duLieu` = mảng `{ id, tenThuoc, hoatChat, hamLuong, dangBaoChe, duongDung, donVi }`. `dangBaoChe` theo
ngôn ngữ (vd `viên nén`); `dangBaoChe`, `duongDung` có thể null. Gửi `id` vào `donThuoc[].idThuoc`.

```json
[ { "id": 15, "tenThuoc": "Paracetamol", "hoatChat": "Paracetamol", "hamLuong": "500mg", "dangBaoChe": "viên nén", "duongDung": "UONG", "donVi": "viên" } ]
```

#### GET /api/medical/bac-si/toi/icd10

**Quyền:** BAC_SI. Gợi ý mã ICD-10 cho chẩn đoán chính theo đầu mã hoặc 1 phần tên tiếng Việt / tiếng Anh, tối đa 20,
theo mã.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuKhoa` | chuỗi | không | trống → mảng rỗng |

**Response:** `duLieu` = mảng `{ ma, ten, manTinh }`. `ten` theo ngôn ngữ; `manTinh = true` (Phụ lục VII Thông tư
26/2025): mỗi thuốc kê được tới 90 ngày. Gửi `ma` vào `maIcdChinh`.

```json
[ { "ma": "J02.9", "ten": "Viêm họng cấp, không đặc hiệu", "manTinh": false } ]
```

### 3.8 Yêu cầu đổi ca, xin nghỉ, nhường ca

- Chỉ gửi được cho ca **của tôi**, còn hoạt động, bắt đầu sau ít nhất 24 giờ (mặc định; quản trị viên đổi trong cấu
  hình). Mỗi ca chỉ có 1 yêu cầu còn chờ (`CHO_DONG_Y` / `CHO_DUYET`).
- `XIN_NGHI` và `DOI_CA` đi thẳng tới quản trị viên (`CHO_DUYET`). `NHUONG_CA` chờ bác sĩ nhận đồng ý (`CHO_DONG_Y`), rồi
  mới tới quản trị viên.
- Duyệt / từ chối do quản trị viên làm; kết quả đến qua thông báo (mục 2.3).

#### POST /api/booking/bac-si/toi/yeu-cau-doi-lich

**Quyền:** BAC_SI. Gửi 1 yêu cầu cho 1 ca của tôi.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idLichLamViec` | số | có | ca của tôi |
| `loaiYeuCau` | `LoaiYeuCauDoiLich` | có | |
| `lyDo` | chuỗi | có | tối đa 1000 |
| `ngayMongMuon` | ngày | có điều kiện | bắt buộc với `DOI_CA`; không quá 90 ngày tới (mặc định) và không phải ngày bệnh viện nghỉ |
| `gioBatDauMongMuon`, `gioKetThucMongMuon` | giờ | có điều kiện | bắt buộc với `DOI_CA`; kết thúc sau bắt đầu; ca mới cũng phải cách hiện tại ít nhất 24 giờ |
| `idPhongKhamMongMuon` | số | không | chỉ `DOI_CA`; null = giữ phòng hiện tại; phòng đang hoạt động cùng chuyên khoa (`GET /api/catalog/phong-kham`) |
| `idBacSiNhan` | số | có điều kiện | bắt buộc với `NHUONG_CA`: bác sĩ khác, đang công tác, cùng chuyên khoa |

Với `XIN_NGHI` chỉ cần 3 trường đầu. `DOI_CA` phải đổi ít nhất 1 trong ngày / giờ / phòng.

```json
{ "idLichLamViec": 55, "loaiYeuCau": "XIN_NGHI", "lyDo": "Đi hội thảo" }
```

```json
{ "idLichLamViec": 55, "loaiYeuCau": "DOI_CA", "lyDo": "Trùng lịch hội chẩn",
  "ngayMongMuon": "2026-10-29", "gioBatDauMongMuon": "13:00:00", "gioKetThucMongMuon": "16:00:00", "idPhongKhamMongMuon": 3 }
```

```json
{ "idLichLamViec": 55, "loaiYeuCau": "NHUONG_CA", "lyDo": "Việc gia đình", "idBacSiNhan": 9 }
```

**Response:** HTTP 201, `duLieu` = [`YeuCauDoiLichResponse`](#yeucaudoilichresponse).

**Lỗi:**
- 404 `KHONG_TIM_THAY` (ca không có / của bác sĩ khác);
- 409 `CA_KHONG_SUA_DUOC` (ca đã hủy), `QUA_HAN_GUI_YEU_CAU` (còn chưa tới 24 giờ), `CA_DA_CO_YEU_CAU_CHO_DUYET`;
- `DOI_CA`: 400 `DU_LIEU_KHONG_HOP_LE` (thiếu ngày / giờ, giờ sai, ca mới quá gần / quá xa, phòng không hợp lệ, không đổi
  gì), 409 `NGAY_BENH_VIEN_NGHI`;
- `NHUONG_CA`: 400 `DU_LIEU_KHONG_HOP_LE` (`idBacSiNhan` thiếu / là chính mình / không hợp lệ), 409 `TRUNG_LICH_LAM_VIEC`,
  `VUOT_GIO_LAM_TUAN` (bác sĩ nhận trùng giờ hoặc vượt giờ làm trong tuần).

#### POST /api/booking/bac-si/toi/yeu-cau-doi-lich/xin-nghi-nhieu-ca

**Quyền:** BAC_SI. Xin nghỉ nhiều ca 1 lần (vd nghỉ 1 tuần): mỗi ca thành 1 yêu cầu `XIN_NGHI` cùng lý do, chung `maNhom`.
1 ca không gửi được thì **không lưu gì**, lỗi nêu ca đó (giữ mã lỗi của ca).

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `cacIdLichLamViec` | mảng số | có | 1–50 ca của tôi, tối đa 50; id trùng tính 1 lần |
| `lyDo` | chuỗi | có | tối đa 1000 |

**Response:** HTTP 201, `duLieu` = mảng [`YeuCauDoiLichResponse`](#yeucaudoilichresponse), theo ngày giờ ca.

**Lỗi:** như `POST …/yeu-cau-doi-lich` với từng ca (`thongDiep` nêu ngày giờ ca, hoặc `#id` khi không tìm thấy).

#### GET /api/booking/bac-si/toi/yeu-cau-doi-lich

**Quyền:** BAC_SI. Các yêu cầu tôi đã gửi, mới nhất trước.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `trangThai` | `TrangThaiYeuCau` | không | lọc theo trạng thái |
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của [`YeuCauDoiLichResponse`](#yeucaudoilichresponse).

#### POST /api/booking/bac-si/toi/yeu-cau-doi-lich/nhom/{maNhom}/rut

**Quyền:** BAC_SI. Rút mọi yêu cầu còn chờ của 1 nhóm xin nghỉ nhiều ca (yêu cầu đã xử lý thì giữ nguyên).

**Request:** path `maNhom` (từ `YeuCauDoiLichResponse.maNhom`).

**Response:** `duLieu` = mảng [`YeuCauDoiLichResponse`](#yeucaudoilichresponse) vừa rút (`DA_RUT`).

**Lỗi:** 404 `KHONG_TIM_THAY` (không có nhóm của tôi); 409 `YEU_CAU_DA_XU_LY` (không còn yêu cầu nào đang chờ).

#### POST /api/booking/bac-si/toi/yeu-cau-doi-lich/{id}/rut

**Quyền:** BAC_SI. Rút 1 yêu cầu còn chờ (`CHO_DONG_Y` hoặc `CHO_DUYET`) của tôi, thành `DA_RUT`. Sau đó ca nhận được yêu
cầu mới. Rút nhường ca thì bác sĩ nhận được báo.

**Request:** path `id`.

**Response:** `duLieu` = [`YeuCauDoiLichResponse`](#yeucaudoilichresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`; 409 `YEU_CAU_DA_XU_LY`.

#### GET /api/booking/bac-si/toi/yeu-cau-nhuong-ca

**Quyền:** BAC_SI. Yêu cầu nhường ca đồng nghiệp gửi cho tôi, mới nhất trước.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `trangThai` | `TrangThaiYeuCau` | không | `CHO_DONG_Y` = đang chờ tôi trả lời |
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của [`YeuCauDoiLichResponse`](#yeucaudoilichresponse) (`bacSi` là người nhường,
`bacSiNhan` là tôi).

#### POST /api/booking/bac-si/toi/yeu-cau-nhuong-ca/{id}/dong-y

**Quyền:** BAC_SI. Đồng ý nhận ca: yêu cầu thành `CHO_DUYET`, chờ quản trị viên duyệt. Ca chỉ chuyển sang tôi khi được
duyệt.

**Request:** path `id`, không body.

**Response:** `duLieu` = [`YeuCauDoiLichResponse`](#yeucaudoilichresponse).

**Lỗi:** 404 `KHONG_TIM_THAY` (không gửi cho tôi); 409 `YEU_CAU_DA_XU_LY` (không còn `CHO_DONG_Y`), `CA_KHONG_SUA_DUOC`
(ca đã hủy hoặc đã bắt đầu), `TRUNG_LICH_LAM_VIEC` (tôi có ca trùng giờ), `VUOT_GIO_LAM_TUAN` (nhận thêm thì vượt giờ làm
tối đa trong tuần, mặc định 48 giờ).

#### POST /api/booking/bac-si/toi/yeu-cau-nhuong-ca/{id}/tu-choi

**Quyền:** BAC_SI. Từ chối nhận ca: yêu cầu thành `DONG_NGHIEP_TU_CHOI`, người nhường được báo.

| Trường (body, có thể bỏ cả body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `ghiChu` | chuỗi | không | tối đa 1000; thành `ghiChuBacSiNhan` |

**Response:** `duLieu` = [`YeuCauDoiLichResponse`](#yeucaudoilichresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`; 409 `YEU_CAU_DA_XU_LY`.

### 3.9 Hồ sơ giới thiệu và ảnh

Bác sĩ tự sửa: giới thiệu ngắn, tiểu sử, ngôn ngữ khám, giải thưởng, công trình nghiên cứu (cả bản tiếng Anh) và ảnh làm
việc; có hiệu lực ngay trên trang công khai. Các mục khác (học vị, chức vụ, giấy phép, đào tạo, công tác, lĩnh vực, năm
hành nghề, ảnh chứng chỉ) do quản trị viên sửa. Ảnh đại diện, số điện thoại: `/api/users/me` (mục 2.2).

#### GET /api/catalog/bac-si/toi/ho-so

**Quyền:** BAC_SI. Hồ sơ giới thiệu của tôi: mọi mục, cả 2 ngôn ngữ, kèm `phienBanHoSo`.

**Response:** `duLieu` = [`HoSoBacSiQuanTriResponse`](#hosobacsiquantriresponse).

#### PUT /api/catalog/bac-si/toi/ho-so

**Quyền:** BAC_SI. Ghi đè các mục bác sĩ được sửa. **Gửi đủ mọi trường**: trường bỏ trống / null bị xoá; mục khác của hồ sơ
giữ nguyên (gửi kèm cũng bị bỏ qua).

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `phienBan` | số | có | `phienBanHoSo` đang xem |
| `gioiThieuNgan`, `gioiThieuNganEn` | chuỗi | không | tối đa 300 |
| `tieuSu`, `tieuSuEn` | chuỗi | không | tối đa 5000 |
| `ngonNguKham` | mảng chuỗi | không | tối đa 10 mã, mỗi mã `[A-Za-z]{2,3}` (ISO 639, vd `vi`, `en`) |
| `giaiThuong`, `giaiThuongEn` | mảng chuỗi | không | tối đa 20 mục; mỗi mục không trống, tối đa 300, 1 dòng |
| `congTrinhNghienCuu`, `congTrinhNghienCuuEn` | mảng chuỗi | không | tối đa 20 mục; mỗi mục như trên |

```json
{ "phienBan": 4, "gioiThieuNgan": "15 năm kinh nghiệm nội tổng quát", "gioiThieuNganEn": "15 years in internal medicine",
  "tieuSu": null, "tieuSuEn": null, "ngonNguKham": [ "vi", "en" ],
  "giaiThuong": [ "Thầy thuốc trẻ tiêu biểu 2020" ], "giaiThuongEn": [ "Outstanding Young Physician 2020" ],
  "congTrinhNghienCuu": [], "congTrinhNghienCuuEn": [] }
```

**Response:** `duLieu` = [`HoSoBacSiQuanTriResponse`](#hosobacsiquantriresponse) (`phienBanHoSo` tăng 1).

**Lỗi:** 409 `DU_LIEU_DA_THAY_DOI` (`phienBan` cũ: tải lại hồ sơ).

#### POST /api/catalog/bac-si/toi/anh

**Quyền:** BAC_SI. Thêm 1 ảnh làm việc (luôn `ANH_CONG_VIEC`). Body `multipart/form-data`:

| Trường (multipart) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `anh` | tệp | có | JPEG / PNG / WebP (nhận theo nội dung tệp), tối đa 4 MB; lưu thu về cạnh dài 1600 px |
| `chuThich`, `chuThichEn` | chuỗi | không | tối đa 200 |

Mỗi bác sĩ tối đa 12 ảnh (tính cả ảnh chứng chỉ).

**Response:** HTTP 201, `duLieu` = [`AnhBacSiQuanTriResponse`](#anhbacsiquantriresponse).

```json
{ "id": 33, "loai": "ANH_CONG_VIEC", "url": "https://res.cloudinary.com/demo/image/upload/bac-si/7/abc.jpg", "chuThich": "Phòng khám", "chuThichEn": null }
```

**Lỗi:** 400 `ANH_KHONG_HOP_LE` (không phải ảnh JPEG / PNG / WebP); 409 `VUOT_SO_ANH_BAC_SI` (đủ 12 ảnh); 413
`TEP_QUA_LON` (quá 4 MB); 503 `LUU_TRU_ANH_KHONG_KHA_DUNG` (kho ảnh chưa cấu hình).

#### PUT /api/catalog/bac-si/toi/anh/{idAnh}

**Quyền:** BAC_SI. Sửa chú thích 1 ảnh làm việc của tôi (không đổi tệp). Trường bỏ trống là xoá chú thích.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `chuThich`, `chuThichEn` | chuỗi | không | tối đa 200 |

**Response:** `duLieu` = [`AnhBacSiQuanTriResponse`](#anhbacsiquantriresponse).

**Lỗi:** 404 `KHONG_TIM_THAY` (ảnh không có / của bác sĩ khác); 403 `KHONG_CO_QUYEN` (ảnh chứng chỉ).

#### DELETE /api/catalog/bac-si/toi/anh/{idAnh}

**Quyền:** BAC_SI. Xoá 1 ảnh làm việc của tôi.

**Request:** path `idAnh`.

**Response:** không có `duLieu`, chỉ `thongDiep`.

**Lỗi:** 404 `KHONG_TIM_THAY`; 403 `KHONG_CO_QUYEN` (ảnh chứng chỉ).

### 3.10 Đánh giá về tôi

Ẩn danh: không có tên bệnh nhân, không có mã lịch hẹn.

#### GET /api/booking/bac-si/toi/danh-gia/tong-quan

**Quyền:** BAC_SI. Điểm trung bình, số đánh giá và phân bố theo sao của tôi.

**Response:** `{ diemTrungBinh, soDanhGia, phanBo }`. `diemTrungBinh` 1 chữ số thập phân, null khi chưa có đánh giá;
`phanBo` luôn đủ 5 khoá `"1"`–`"5"`.

```json
{ "diemTrungBinh": 4.7, "soDanhGia": 36, "phanBo": { "1": 0, "2": 1, "3": 2, "4": 4, "5": 29 } }
```

#### GET /api/booking/bac-si/toi/danh-gia

**Quyền:** BAC_SI. Các đánh giá tôi nhận được, mới nhất trước.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của `{ soSao, nhanXet, ngayKham, ngayTao }` (`nhanXet` có thể null; `ngayKham`: ngày
của lượt khám; `ngayTao`: lúc gửi đánh giá).

```json
{ "noiDung": [ { "soSao": 5, "nhanXet": "Bác sĩ tận tình", "ngayKham": "2026-10-18", "ngayTao": "2026-10-18T15:20:00" } ],
  "trang": 0, "kichThuoc": 20, "tongSoPhanTu": 36, "tongSoTrang": 2 }
```
