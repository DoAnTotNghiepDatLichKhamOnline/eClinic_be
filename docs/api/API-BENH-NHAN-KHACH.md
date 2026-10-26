# Tài liệu API eClinic — Bệnh nhân và khách

Dành cho: frontend các màn hình của bệnh nhân (đã đăng nhập) và khách (chưa đăng nhập).

Khách không cần token: các route ghi **Quyền: Công khai** gọi được khi chưa đăng nhập; khi đã đăng nhập vẫn gửi token để backend nhận ra bệnh nhân. Phần quy ước chung và các route xác thực, tài khoản, thông báo giống hệt ở cả 3 tài liệu.

**Lịch sử phiên bản**

| Phiên bản | Nội dung |
|---|---|
| 1.0 — 21/10/2026 | Bản đầu: 27 route dùng chung (mục 2) và 41 route của bệnh nhân và khách (mục 3). |

## Mục lục

- [1. Quy ước chung](#1-quy-ước-chung)
- [2. Xác thực, tài khoản và thông báo (mọi vai trò)](#2-xác-thực-tài-khoản-và-thông-báo-mọi-vai-trò)
- [3. API của bệnh nhân và khách](#3-api-của-bệnh-nhân-và-khách)
  - [3.1 Tổng quan](#31-tổng-quan)
  - [3.2 Mô hình và enum của mục 3](#32-mô-hình-và-enum-của-mục-3)
  - [3.3 Danh mục bác sĩ, chuyên khoa](#33-danh-mục-bác-sĩ-chuyên-khoa)
  - [3.4 Lịch trống và quy tắc đặt lịch](#34-lịch-trống-và-quy-tắc-đặt-lịch)
  - [3.5 Đặt lịch](#35-đặt-lịch)
  - [3.6 Phiếu khám theo link, cấp lại liên kết](#36-phiếu-khám-theo-link-cấp-lại-liên-kết)
  - [3.7 Sổ khám bệnh](#37-sổ-khám-bệnh)
  - [3.8 Lịch hẹn của tôi và đánh giá](#38-lịch-hẹn-của-tôi-và-đánh-giá)
  - [3.9 Hồ sơ, người thân, lịch sử khám, trang cá nhân](#39-hồ-sơ-người-thân-lịch-sử-khám-trang-cá-nhân)
  - [3.10 Chatbot](#310-chatbot)

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

## 3. API của bệnh nhân và khách

### 3.1 Tổng quan

**Ba luồng chính**

1. **Khách đặt lịch** (không đăng nhập):
   - chọn chuyên khoa (`GET /api/catalog/chuyen-khoa`);
   - chọn bác sĩ (`GET /api/catalog/bac-si`, ngày còn chỗ sớm nhất ở `GET /api/booking/khung-gio/ngay-som-nhat`);
   - chọn ngày (`GET /api/booking/khung-gio/ngay-con-cho`, ngày nghỉ ở `GET /api/booking/ngay-nghi`);
   - chọn khung giờ (`GET /api/booking/khung-gio`, hoặc `/gop` cho "bác sĩ bất kỳ");
   - đặt bằng `POST /api/booking/lich-hen`, rồi mở trang phiếu khám theo `linkPhieuKham`.

   Khách xem lại, hủy, đổi lịch **chỉ bằng link phiếu khám** (`/api/booking/phieu-kham/{maPhieuKham}`, hủy / đổi kèm SĐT
   liên hệ).
2. **Bệnh nhân đã đăng nhập** đi cùng các bước trên nhưng gửi kèm token:
   - form điền sẵn từ `GET /api/booking/thong-tin-dat-lich/cua-toi`;
   - lịch đặt xong nằm trong "lịch hẹn của tôi" (`/api/booking/lich-hen/cua-toi…`), hủy / đổi / đánh giá ở đó;
   - kết quả khám có trong chi tiết lịch hẹn và lịch sử khám.
3. **Mất link phiếu khám**: khách gửi mã tra cứu + SĐT tới `POST /api/booking/cap-lai-lien-ket`, link được gửi lại qua
   email của lịch hẹn. Sau khi khám, kết quả cũng xem được ở **sổ khám bệnh** (`/api/booking/so-kham/{maSoKham}`, link có
   trong email và trong kết quả khám).

**Các mã của 1 lịch hẹn.** Backend không trả id lịch hẹn cho bệnh nhân, chỉ trả các mã:

| Mã | Dạng | Dùng để |
|---|---|---|
| `maPhieuKham` | 43 ký tự ngẫu nhiên `[A-Za-z0-9_-]` | Mở phiếu khám, hủy / đổi, chi tiết trong "lịch hẹn của tôi". Ai có mã là xem được phiếu: giữ kín như mật khẩu, không ghi log |
| `maTraCuu` | `ECL-yyyyMMdd-nnnn`, vd `ECL-20261025-4198` | In trên phiếu để đọc / gõ, xin cấp lại link. Không mở được phiếu khám |
| `maSoKham` | 43 ký tự ngẫu nhiên, khác `maPhieuKham` | Mở sổ khám bệnh (có kết quả khám) |

**Tất cả route của mục 3**

| Phương thức | Đường dẫn | Quyền | Mục đích |
|---|---|---|---|
| GET | `/api/catalog/chuyen-khoa` | Công khai | Danh sách chuyên khoa |
| GET | `/api/catalog/chuyen-khoa/{id}` | Công khai | Chi tiết chuyên khoa |
| GET | `/api/catalog/bac-si` | Công khai | Tìm bác sĩ |
| GET | `/api/catalog/bac-si/{id}` | Công khai | Chi tiết bác sĩ |
| GET | `/api/booking/khung-gio` | Công khai | Ca và khung giờ của 1 ngày |
| GET | `/api/booking/khung-gio/ngay-con-cho` | Công khai | Ngày có ca và số chỗ còn lại |
| GET | `/api/booking/khung-gio/nhieu-ngay` | Công khai | Khung giờ của 1 bác sĩ trong tối đa 7 ngày |
| GET | `/api/booking/khung-gio/gop` | Công khai | Khung giờ gộp của chuyên khoa ("bác sĩ bất kỳ") |
| GET | `/api/booking/khung-gio/ngay-som-nhat` | Công khai | Ngày còn chỗ sớm nhất của từng bác sĩ |
| GET | `/api/booking/ngay-nghi` | Công khai | Ngày bệnh viện nghỉ |
| GET | `/api/booking/quy-tac-dat-lich` | Công khai | Các giới hạn đặt / hủy / đổi lịch |
| POST | `/api/booking/lich-hen` | Công khai (gửi token nếu đã đăng nhập) | Đặt lịch khám |
| GET | `/api/booking/phieu-kham/{maPhieuKham}` | Công khai | Xem phiếu khám |
| POST | `/api/booking/phieu-kham/{maPhieuKham}/huy` | Công khai | Hủy lịch bằng link (kèm SĐT) |
| POST | `/api/booking/phieu-kham/{maPhieuKham}/doi-lich` | Công khai | Đổi lịch bằng link (kèm SĐT) |
| GET | `/api/booking/phieu-kham/{maPhieuKham}/lich.ics` | Công khai | Tệp lịch `.ics` |
| GET | `/api/booking/phieu-kham/{maPhieuKham}/qr` | Công khai | Ảnh QR của phiếu khám |
| POST | `/api/booking/cap-lai-lien-ket` | Công khai | Xin gửi lại link phiếu khám qua email |
| GET | `/api/booking/so-kham/{maSoKham}` | Công khai | Tóm tắt sổ khám (đã che) |
| POST | `/api/booking/so-kham/{maSoKham}/xem` | Công khai | Sổ khám đầy đủ (kèm 4 số cuối SĐT) |
| GET | `/api/booking/so-kham/{maSoKham}/qr` | Công khai | Ảnh QR của sổ khám |
| GET | `/api/booking/lich-hen/cua-toi` | BENH_NHAN | Lịch hẹn của tôi (phân trang) |
| GET | `/api/booking/lich-hen/cua-toi/lich` | BENH_NHAN | Lịch hẹn của tôi theo khoảng ngày |
| GET | `/api/booking/lich-hen/cua-toi/{maPhieuKham}` | BENH_NHAN | Chi tiết 1 lịch hẹn, kèm kết quả khám |
| POST | `/api/booking/lich-hen/cua-toi/{maPhieuKham}/huy` | BENH_NHAN | Hủy lịch hẹn của tôi |
| POST | `/api/booking/lich-hen/cua-toi/{maPhieuKham}/doi-lich` | BENH_NHAN | Đổi lịch hẹn của tôi |
| POST | `/api/booking/lich-hen/cua-toi/{maPhieuKham}/danh-gia` | BENH_NHAN | Gửi đánh giá lượt khám |
| PUT | `/api/booking/lich-hen/cua-toi/{maPhieuKham}/danh-gia` | BENH_NHAN | Sửa đánh giá |
| GET | `/api/booking/ho-so-benh-nhan/cua-toi` | BENH_NHAN | Xem hồ sơ bệnh nhân của tôi |
| PUT | `/api/booking/ho-so-benh-nhan/cua-toi` | BENH_NHAN | Tạo / sửa hồ sơ bệnh nhân của tôi |
| GET | `/api/booking/thong-tin-dat-lich/cua-toi` | BENH_NHAN | Dữ liệu điền sẵn form đặt lịch |
| PUT | `/api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id}` | BENH_NHAN | Sửa người thân đã lưu |
| DELETE | `/api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id}` | BENH_NHAN | Bỏ người thân đã lưu |
| GET | `/api/booking/lich-su-kham/cua-toi` | BENH_NHAN | Lịch sử khám (kèm kết quả) |
| GET | `/api/booking/trang-ca-nhan/cua-toi` | BENH_NHAN | Dữ liệu trang cá nhân |
| POST | `/api/chatbot/phien` | Công khai | Mở phiên chat |
| GET | `/api/chatbot/phien` | Đăng nhập (mọi vai trò) | Các phiên chat của tôi |
| POST | `/api/chatbot/phien/{idPhien}/tin-nhan` | Công khai (khách gửi `X-Chat-Khach`) | Gửi tin nhắn |
| GET | `/api/chatbot/phien/{idPhien}/tin-nhan` | Công khai (khách gửi `X-Chat-Khach`) | Tin nhắn của 1 phiên |
| POST | `/api/chatbot/phien/{idPhien}/dong-y` | BENH_NHAN | Đồng ý cho chatbot đọc lịch hẹn của tôi |
| DELETE | `/api/chatbot/phien/{idPhien}/dong-y` | BENH_NHAN | Rút lại đồng ý |

### 3.2 Mô hình và enum của mục 3

`GioiTinh`, `TrangThaiLienKet`, `TrangThaiLichHen`: xem [mục 1.7](#17-enum-dùng-chung).

| Enum | Giá trị |
|---|---|
| `LocLichHen` | `TAT_CA`; `SAP_TOI` (lịch `DA_XAC_NHAN` chưa khám xong); `LICH_SU` (còn lại: đã khám, đã hủy, bị hủy, không đến, đã qua giờ khám) |
| `PhamViLichHen` (query `cuaAi`) | `TAT_CA`; `BAN_THAN` (người khám là chủ tài khoản, ai đặt cũng được); `NGUOI_KHAC` (tài khoản đặt cho người thân, hoặc chủ tài khoản là người giám hộ) |
| `BuoiKham` | `SANG` (giờ bắt đầu trước 12:00), `CHIEU` (12:00–17:00), `TOI` (từ 17:00) |
| `QuanHeGiamHo` | `CHA`, `ME`, `NGUOI_GIAM_HO_HOP_PHAP`, `KHAC` |
| `NguoiDatLich` | `TOI` (chính tài khoản đang xem), `KHACH` (đặt khi không đăng nhập), `TAI_KHOAN_KHAC` |
| `LoaiAnhBacSi` | `ANH_CONG_VIEC`, `CHUNG_CHI` |
| `DuongDungThuoc` | `UONG`, `NGAM_DUOI_LUOI`, `TIEM`, `BOI_NGOAI_DA`, `NHO_MAT`, `NHO_TAI`, `NHO_MUI`, `XIT_HIT`, `DAT`, `KHAC` (nhãn hiển thị do frontend đặt) |

#### `BacSiTomTatResponse`

`{ id, hoTen, hocVi, anhDaiDien }`: bác sĩ cạnh khung giờ / trên phiếu. `hocVi`, `anhDaiDien` (URL) có thể null.

#### `PhongKhamTomTatResponse`

`{ id, tenPhong, tang }`, theo ngôn ngữ của request. `tang` có thể null.

#### `BenhNhanRequest`

Người khám trên form đặt lịch. Trường không bắt buộc: bỏ trống hoặc gửi `""` đều được.

| Trường | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `hoTen` | chuỗi | có | tối đa 150 |
| `ngaySinh` | ngày | có | không ở tương lai; dùng để tính tuổi theo **ngày khám** |
| `gioiTinh` | `GioiTinh` | có | |
| `soDienThoai` | chuỗi | có | `^0\d{9}$`; là SĐT liên hệ của lượt khám khi không có người giám hộ |
| `cccd` | chuỗi | có điều kiện | 12 chữ số. Bắt buộc với người từ đủ 18 tuổi (theo ngày khám); dưới 18 tuổi chưa có CCCD thì bỏ trống |
| `email` | chuỗi | không | email liên hệ của lượt khám (nhận phiếu khám, nhắc lịch), tối đa 255 |
| `soBaoHiemYTe` | chuỗi | không | tối đa 50; chỉ ghi vào hồ sơ khi hồ sơ chưa có |
| `diaChi` | chuỗi | không | tối đa 500; chỉ ghi vào hồ sơ khi hồ sơ chưa có |

#### `NguoiGiamHoRequest`

Bắt buộc khi người khám dưới 18 tuổi tính theo ngày khám; từ đủ 18 tuổi thì bị bỏ qua.

| Trường | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `hoTen` | chuỗi | có | tối đa 150 |
| `quanHe` | `QuanHeGiamHo` | có | |
| `soDienThoai` | chuỗi | có | `^0\d{9}$`; thành SĐT liên hệ của lượt khám |
| `cccd` | chuỗi | có | 12 chữ số, phải khác CCCD của người khám |
| `ngaySinh` | ngày | không | ở quá khứ; có gửi thì người giám hộ phải từ đủ 18 tuổi tính theo ngày khám |

#### `CaKhamResponse`

1 ca làm việc còn đặt được, kèm các khung 1 giờ còn kịp đặt. Đặt lịch bằng `idLichLamViec` + `khungGio[].gioBatDau`.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `idLichLamViec` | số | id ca làm việc |
| `ngay` | ngày | |
| `gioBatDau`, `gioKetThuc` | giờ | giờ của ca |
| `soLuotToiDaMoiGio` | số | số lượt tối đa của 1 khung 1 giờ |
| `thoiLuongLuotPhut` | số | số phút của 1 lượt khám |
| `bacSi` | [`BacSiTomTatResponse`](#bacsitomtatresponse) | |
| `phongKham` | [`PhongKhamTomTatResponse`](#phongkhamtomtatresponse) | |
| `khungGio[]` | mảng | `{ gioBatDau, gioKetThuc (ngày giờ), tongSoCho, soChoConLai, hetCho }`. `hetCho = true`: hiện "Hết chỗ", không cho chọn. Khung cuối của ca có thể ngắn hơn 1 giờ |

```json
{ "idLichLamViec": 120, "ngay": "2026-10-25", "gioBatDau": "08:00:00", "gioKetThuc": "11:00:00",
  "soLuotToiDaMoiGio": 4, "thoiLuongLuotPhut": 15,
  "bacSi": { "id": 3, "hoTen": "Trần Minh", "hocVi": "ThS.BS", "anhDaiDien": null },
  "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" },
  "khungGio": [
    { "gioBatDau": "2026-10-25T08:00:00", "gioKetThuc": "2026-10-25T09:00:00", "tongSoCho": 4, "soChoConLai": 1,
      "hetCho": false },
    { "gioBatDau": "2026-10-25T09:00:00", "gioKetThuc": "2026-10-25T10:00:00", "tongSoCho": 4, "soChoConLai": 0,
      "hetCho": true } ] }
```

#### `DatLichResponse`

Kết quả đặt lịch và đổi lịch (đổi lịch trả lịch **mới**, với `maPhieuKham` mới).

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `maPhieuKham` | chuỗi | mở phiếu khám |
| `maTraCuu` | chuỗi | mã ngắn in trên phiếu |
| `linkPhieuKham` | chuỗi | link trang phiếu khám của frontend (`<FRONTEND_URL>/phieu-kham/<maPhieuKham>`), cũng là nội dung QR |
| `soThuTu` | số | số thứ tự theo phòng khám + ngày |
| `ngay` | ngày | ngày khám |
| `gioKhamDuKien` | ngày giờ | giờ dự kiến vào khám (giờ đầu khung + (vị trí − 1) × thời lượng lượt) |
| `gioBatDauKhung`, `gioKetThucKhung` | ngày giờ | khung 1 giờ đã chọn |
| `trangThai` | `TrangThaiLichHen` | luôn `DA_XAC_NHAN` |
| `bacSi` | [`BacSiTomTatResponse`](#bacsitomtatresponse) | bác sĩ khám (với "bác sĩ bất kỳ" là bác sĩ hệ thống xếp) |
| `phongKham` | [`PhongKhamTomTatResponse`](#phongkhamtomtatresponse) | |
| `hoTenBenhNhan` | chuỗi | |
| `hoTenNguoiGiamHo` | chuỗi \| null | null nếu không có người giám hộ |
| `luuVaoTaiKhoan` | boolean | `true`: đặt khi đã đăng nhập, lịch có trong "lịch hẹn của tôi"; `false`: khách đặt, chỉ xem lại được bằng link |
| `bacSiDoPhongKhamXep` | boolean | `true`: người đặt chọn "bác sĩ bất kỳ" |

```json
{ "maPhieuKham": "Xk3v9QpL2mR7tY0wZb5nC8dF1gH4jK6sA2eU9iO3pQx", "maTraCuu": "ECL-20261025-4198",
  "linkPhieuKham": "http://localhost:5173/phieu-kham/Xk3v9QpL2mR7tY0wZb5nC8dF1gH4jK6sA2eU9iO3pQx",
  "soThuTu": 3, "ngay": "2026-10-25", "gioKhamDuKien": "2026-10-25T08:30:00",
  "gioBatDauKhung": "2026-10-25T08:00:00", "gioKetThucKhung": "2026-10-25T09:00:00", "trangThai": "DA_XAC_NHAN",
  "bacSi": { "id": 3, "hoTen": "Trần Minh", "hocVi": "ThS.BS", "anhDaiDien": null },
  "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" },
  "hoTenBenhNhan": "Nguyễn Văn An", "hoTenNguoiGiamHo": null, "luuVaoTaiKhoan": false, "bacSiDoPhongKhamXep": false }
```

#### `PhieuKhamResponse`

Phiếu khám công khai: ai có link cũng xem được nên CCCD, SĐT đã che; không có id lịch hẹn, id hồ sơ.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `maPhieuKham`, `maTraCuu`, `linkPhieuKham`, `soThuTu`, `ngay`, `gioKhamDuKien`, `gioBatDauKhung`, `gioKetThucKhung`, `bacSi`, `phongKham` | | như [`DatLichResponse`](#datlichresponse) |
| `trangThai` | `TrangThaiLichHen` | |
| `tenChuyenKhoa` | chuỗi | theo ngôn ngữ của request |
| `lyDoKham` | chuỗi | |
| `ngayDat` | ngày giờ | thời điểm đặt |
| `benhNhan` | object | `{ hoTen, namSinh, gioiTinh, cccd, soDienThoai }`: `cccd` đã che (chỉ còn 3 số cuối); `soDienThoai` đã che, null khi có người giám hộ |
| `nguoiGiamHo` | object \| null | `{ hoTen, quanHe, soDienThoai }` (SĐT đã che, không có CCCD) |
| `canNguoiGiamHoDiCung` | boolean | `true`: hiện ghi chú "người giám hộ phải đi cùng" |
| `lyDoHuy` | chuỗi \| null | lý do khi `BI_TU_CHOI` / `DA_HUY` / `QUAN_TRI_HUY` |
| `duocHuyDoi` | boolean | còn hủy / đổi được không (`DA_XAC_NHAN` và chưa quá `hanHuyDoi`). Dùng để bật / tắt nút |
| `hanHuyDoi` | ngày giờ | hạn chót hủy / đổi (giờ khám trừ khoảng tối thiểu) |
| `canDoiLich` | boolean | `true`: ca khám đã bị hủy, lịch còn hiệu lực và đang chờ đổi sang giờ khác (hoặc hủy). Khi đó hủy / đổi được tới giờ khám cũ, không tính vào số lần đổi. Nên hiện cảnh báo nổi bật |
| `daDoiBacSi` | boolean | `true`: ca được nhường cho bác sĩ khác (`bacSi` là bác sĩ mới), lịch vẫn khám bình thường; hủy / đổi được tới giờ khám, không tính vào số lần đổi |

```json
{ "maPhieuKham": "Xk3v9QpL2mR7tY0wZb5nC8dF1gH4jK6sA2eU9iO3pQx", "maTraCuu": "ECL-20261025-4198",
  "linkPhieuKham": "http://localhost:5173/phieu-kham/Xk3v9QpL2mR7tY0wZb5nC8dF1gH4jK6sA2eU9iO3pQx",
  "trangThai": "DA_XAC_NHAN", "soThuTu": 3, "ngay": "2026-10-25", "gioKhamDuKien": "2026-10-25T08:30:00",
  "gioBatDauKhung": "2026-10-25T08:00:00", "gioKetThucKhung": "2026-10-25T09:00:00",
  "bacSi": { "id": 3, "hoTen": "Trần Minh", "hocVi": "ThS.BS", "anhDaiDien": null }, "tenChuyenKhoa": "Nội tổng quát",
  "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" }, "lyDoKham": "Ho kéo dài 2 tuần",
  "ngayDat": "2026-10-21T10:15:00",
  "benhNhan": { "hoTen": "Nguyễn Văn An", "namSinh": 1995, "gioiTinh": "NAM", "cccd": "*********001",
    "soDienThoai": "090****001" },
  "nguoiGiamHo": null, "canNguoiGiamHoDiCung": false, "lyDoHuy": null, "duocHuyDoi": true,
  "hanHuyDoi": "2026-10-25T06:30:00", "canDoiLich": false, "daDoiBacSi": false }
```

#### `LichHenCuaToiResponse`

1 dòng trong "lịch hẹn của tôi". Không có id lịch hẹn: mở chi tiết bằng `maPhieuKham`.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `maPhieuKham`, `maTraCuu`, `linkPhieuKham`, `trangThai`, `soThuTu`, `ngay`, `gioKhamDuKien`, `gioBatDauKhung`, `gioKetThucKhung`, `bacSi`, `tenChuyenKhoa`, `phongKham`, `lyDoKham`, `ngayDat`, `lyDoHuy`, `hanHuyDoi`, `canDoiLich`, `daDoiBacSi` | | như [`PhieuKhamResponse`](#phieukhamresponse) |
| `hoTenBenhNhan` | chuỗi | `laBanThan`: họ tên trong hồ sơ của chủ tài khoản; còn lại: họ tên người đặt đã nhập |
| `laBanThan` | boolean | `true`: người khám là chủ tài khoản (ai đặt cũng được); `false`: lịch của người khác |
| `hoTenNguoiGiamHo` | chuỗi \| null | |
| `nguoiDat` | `NguoiDatLich` | ai đã đặt, nhìn từ tài khoản đang xem |
| `thongTinKhacHoSo` | boolean | chỉ có nghĩa khi `laBanThan`: người đặt nhập họ tên / ngày sinh khác hồ sơ của chủ tài khoản và phòng khám chưa đối chiếu (có thể không phải lịch của mình). Nên hiện cảnh báo |
| `duocHuyDoi` | boolean | tài khoản đang xem còn hủy / đổi được lịch này: `DA_XAC_NHAN`, chưa quá `hanHuyDoi`, và tài khoản là người đặt hoặc người khám |

```json
{ "maPhieuKham": "Xk3v9QpL2mR7tY0wZb5nC8dF1gH4jK6sA2eU9iO3pQx", "maTraCuu": "ECL-20261025-4198",
  "linkPhieuKham": "http://localhost:5173/phieu-kham/Xk3v9QpL2mR7tY0wZb5nC8dF1gH4jK6sA2eU9iO3pQx",
  "trangThai": "DA_XAC_NHAN", "soThuTu": 3, "ngay": "2026-10-25", "gioKhamDuKien": "2026-10-25T08:30:00",
  "gioBatDauKhung": "2026-10-25T08:00:00", "gioKetThucKhung": "2026-10-25T09:00:00",
  "bacSi": { "id": 3, "hoTen": "Trần Minh", "hocVi": "ThS.BS", "anhDaiDien": null }, "tenChuyenKhoa": "Nội tổng quát",
  "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" }, "hoTenBenhNhan": "Nguyễn Văn An",
  "laBanThan": true, "hoTenNguoiGiamHo": null, "lyDoKham": "Ho kéo dài 2 tuần", "ngayDat": "2026-10-21T10:15:00",
  "nguoiDat": "TOI", "thongTinKhacHoSo": false, "lyDoHuy": null, "duocHuyDoi": true,
  "hanHuyDoi": "2026-10-25T06:30:00", "canDoiLich": false, "daDoiBacSi": false }
```

#### `KetQuaKhamResponse`

Kết quả 1 lượt khám (bản cho bệnh nhân), dùng ở chi tiết lịch hẹn, lịch sử khám, trang cá nhân và sổ khám.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `lyDoKham` | chuỗi | lý do khám, triệu chứng bác sĩ ghi |
| `sinhHieu` | object \| null | `{ mach, nhietDo, huyetApTamThu, huyetApTamTruong, nhipTho, canNang, chieuCao }` (lần/phút, °C, mmHg, kg, cm); chỉ số không đo là null; null cả object nếu chưa đo gì |
| `khamLamSang` | chuỗi \| null | |
| `chanDoan` | chuỗi | chẩn đoán chính |
| `icdChinh` | object \| null | `{ ma, ten, manTinh }`: mã ICD-10, tên theo ngôn ngữ, `manTinh` = bệnh mạn tính |
| `chanDoanKemTheo` | chuỗi \| null | |
| `ghiChu` | chuỗi \| null | lời dặn của bác sĩ |
| `ngayTaiKhamDeXuat` | ngày \| null | gợi ý ngày tái khám, **không phải lịch hẹn** |
| `donThuoc[]` | mảng | rỗng nếu không kê đơn. Mỗi dòng: `{ idThuoc, tenThuoc, hoatChat, hamLuong, duongDung: DuongDungThuoc, donVi, lieuDung, soLanMoiNgay, soNgayDung, soLuong, ghiChuSuDung }`; `hoatChat`, `hamLuong`, `duongDung`, `soLuong` có thể null ở dữ liệu cũ |
| `ngaySuaCuoi` | ngày giờ \| null | bác sĩ sửa hồ sơ lần gần nhất; null = chưa sửa |
| `maSoKham`, `linkSoKham` | chuỗi | mã và link sổ khám bệnh của lượt khám |

```json
{ "lyDoKham": "Ho kéo dài 2 tuần",
  "sinhHieu": { "mach": 82, "nhietDo": 37.2, "huyetApTamThu": 120, "huyetApTamTruong": 80, "nhipTho": 18,
    "canNang": 62.5, "chieuCao": 168 },
  "khamLamSang": "Họng đỏ, phổi không ran", "chanDoan": "Viêm họng cấp",
  "icdChinh": { "ma": "J02.9", "ten": "Viêm họng cấp, không đặc hiệu", "manTinh": false },
  "chanDoanKemTheo": null, "ghiChu": "Uống nhiều nước, tái khám nếu sốt cao", "ngayTaiKhamDeXuat": "2026-11-01",
  "donThuoc": [ { "idThuoc": 15, "tenThuoc": "Paracetamol", "hoatChat": "Paracetamol", "hamLuong": "500mg",
    "duongDung": "UONG", "donVi": "viên", "lieuDung": "1 viên/lần", "soLanMoiNgay": 3, "soNgayDung": 5,
    "soLuong": 15, "ghiChuSuDung": "Sau ăn" } ],
  "ngaySuaCuoi": null, "maSoKham": "Qm7Lp2Zx9Vb4Nc8Rt1Yw6Ks3Hd5Gf0Ja2Ue7Io4Pq9",
  "linkSoKham": "http://localhost:5173/so-kham/Qm7Lp2Zx9Vb4Nc8Rt1Yw6Ks3Hd5Gf0Ja2Ue7Io4Pq9" }
```

#### `HoSoCuaToiResponse`

Hồ sơ bệnh nhân của tài khoản đang đăng nhập; không che vì người xem là chủ hồ sơ.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `trangThaiLienKet` | `TrangThaiLienKet` | `CHO_XAC_MINH`: chờ phòng khám xác minh, **mọi trường khác là null** |
| `cccd`, `hoTen`, `ngaySinh`, `gioiTinh`, `soDienThoai` | | |
| `diaChi`, `soBaoHiemYTe`, `tienSuBenhLy` | chuỗi \| null | |

#### `NguoiThanDaLuuResponse`

1 người thân đã lưu (những gì tài khoản nhập ở lần đặt gần nhất cho người này). Tên trường trùng với `benhNhan` /
`nguoiGiamHo` của request đặt lịch để điền thẳng vào form.

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | số | dùng để sửa / bỏ |
| `hoTen`, `ngaySinh`, `gioiTinh`, `soDienThoai` | | |
| `cccd` | chuỗi \| null | null nếu người dưới 18 tuổi đặt khi chưa có CCCD |
| `email`, `diaChi`, `soBaoHiemYTe` | chuỗi \| null | |
| `nguoiGiamHo` | object \| null | `{ hoTen, quanHe, soDienThoai, cccd, ngaySinh }` |
| `lanDungCuoi` | ngày giờ | lần đặt gần nhất cho người này |

### 3.3 Danh mục bác sĩ, chuyên khoa

Tên, mô tả, hồ sơ bác sĩ trả theo `Accept-Language` ([mục 1.6](#16-ngôn-ngữ)); bản tiếng Anh trống thì là bản tiếng Việt.

#### GET /api/catalog/chuyen-khoa

**Quyền:** Công khai. Danh sách chuyên khoa, sắp xếp theo tên.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuKhoa` | chuỗi | không | tìm theo tên tiếng Việt hoặc tiếng Anh |
| `trang` | số | không | từ 0, mặc định 0 |
| `kichThuoc` | số | không | 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của `{ id, tenChuyenKhoa, moTa }` (`moTa` có thể null).

```json
{ "noiDung": [ { "id": 1, "tenChuyenKhoa": "Nội tổng quát", "moTa": "Khám và điều trị bệnh nội khoa thường gặp" } ],
  "trang": 0, "kichThuoc": 20, "tongSoPhanTu": 1, "tongSoTrang": 1 }
```

#### GET /api/catalog/chuyen-khoa/{id}

**Quyền:** Công khai. 1 chuyên khoa.

**Request:** path `id`.

**Response:** `duLieu` = `{ id, tenChuyenKhoa, moTa }`.

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### GET /api/catalog/bac-si

**Quyền:** Công khai. Tìm bác sĩ đang công tác, sắp xếp theo tên, để vẽ thẻ bác sĩ.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idChuyenKhoa` | số | không | |
| `tuKhoa` | chuỗi | không | tìm theo tên bác sĩ |
| `ngay` | ngày | không | chỉ bác sĩ còn ít nhất 1 lượt đặt được trong ngày này; ngày ngoài khoảng đặt được thì trang rỗng |
| `buoi` | `BuoiKham` | không | như `ngay`, theo buổi của giờ bắt đầu lượt khám; dùng riêng hoặc cùng `ngay` |
| `trang`, `kichThuoc` | số | không | như trên |

**Response:** `duLieu` = `TrangDuLieu` của bác sĩ:

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id`, `hoTen` | | |
| `anhDaiDien` | chuỗi \| null | URL |
| `hocVi`, `chucVu`, `gioiThieuNgan` | chuỗi \| null | |
| `soNamKinhNghiem` | số \| null | |
| `idChuyenKhoa`, `tenChuyenKhoa` | | |
| `diemDanhGia` | số \| null | trung bình sao, 1 chữ số thập phân; null khi chưa có đánh giá |
| `soDanhGia` | số | luôn hiện kèm điểm |

Ngày còn chỗ sớm nhất của từng bác sĩ không có ở đây: gọi `GET /api/booking/khung-gio/ngay-som-nhat`.

```json
{ "noiDung": [ { "id": 3, "hoTen": "Trần Minh", "anhDaiDien": null, "hocVi": "ThS.BS", "chucVu": "Trưởng khoa",
    "soNamKinhNghiem": 12, "gioiThieuNgan": "Chuyên điều trị bệnh hô hấp", "idChuyenKhoa": 1,
    "tenChuyenKhoa": "Nội tổng quát", "diemDanhGia": 4.6, "soDanhGia": 25 } ],
  "trang": 0, "kichThuoc": 20, "tongSoPhanTu": 1, "tongSoTrang": 1 }
```

#### GET /api/catalog/bac-si/{id}

**Quyền:** Công khai. Trang chi tiết bác sĩ.

**Request:** path `id`.

**Response:** `duLieu` = các trường của `GET /api/catalog/bac-si`, thêm:

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `tieuSu` | chuỗi \| null | đoạn giới thiệu dài |
| `quaTrinhDaoTao[]`, `quaTrinhCongTac[]`, `linhVucKhamChua[]`, `giaiThuong[]`, `congTrinhNghienCuu[]` | mảng chuỗi | rỗng khi chưa có |
| `ngonNguKham[]` | mảng chuỗi | mã ISO 639 viết thường (`"vi"`, `"en"`) |
| `soGiayPhep`, `ngayCapGiayPhep`, `noiCapGiayPhep`, `phamViHanhNghe` | | giấy phép hành nghề, công khai để tra cứu; có thể null |
| `anh[]` | mảng | ảnh giới thiệu theo thứ tự hiển thị: `{ id, loai: LoaiAnhBacSi, url, chuThich }` |

Nhận xét của bệnh nhân không công khai, chỉ có điểm và số lượt.

**Lỗi:** 404 `KHONG_TIM_THAY` (không có, hoặc bác sĩ không còn hiển thị công khai).

### 3.4 Lịch trống và quy tắc đặt lịch

Các route này chỉ trả lượt **còn kịp đặt**: trong khoảng từ hôm nay tới `soNgayDatTruocToiDa` ngày tới, và bắt đầu sau giờ
hiện tại cộng `datTruocToiThieuPhut` (`GET /api/booking/quy-tac-dat-lich`). Số chỗ là ảnh chụp lúc gọi: 2 người có thể cùng
chọn chỗ cuối, người sau nhận 409 `KHUNG_GIO_KHONG_CON_TRONG` khi đặt.

#### GET /api/booking/khung-gio

**Quyền:** Công khai. Các ca và khung giờ của 1 ngày, theo bác sĩ hoặc chuyên khoa. Bước chọn giờ khi đã chọn bác sĩ.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `ngay` | ngày | có | ngày ngoài khoảng đặt được thì trả `[]` |
| `idBacSi` | số | có điều kiện | phải gửi `idBacSi` hoặc `idChuyenKhoa` (hoặc cả hai) |
| `idChuyenKhoa` | số | có điều kiện | |

**Response:** `duLieu` = mảng [`CaKhamResponse`](#cakhamresponse).

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (thiếu cả `idBacSi` lẫn `idChuyenKhoa`).

#### GET /api/booking/khung-gio/ngay-con-cho

**Quyền:** Công khai. Các ngày có ca kèm số chỗ còn lại, để tô lịch chọn ngày.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuNgay` | ngày | không | mặc định (và sớm nhất) hôm nay |
| `denNgay` | ngày | không | mặc định (và muộn nhất) ngày xa nhất đặt được |
| `idBacSi`, `idChuyenKhoa` | số | có điều kiện | gửi ít nhất 1 |

**Response:** `duLieu` = `[{ ngay, soChoConLai }]`. `soChoConLai = 0`: ngày có ca nhưng đã hết chỗ. Ngày không có trong mảng
là ngày không có ca.

```json
[ { "ngay": "2026-10-24", "soChoConLai": 0 }, { "ngay": "2026-10-25", "soChoConLai": 7 } ]
```

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (`denNgay` trước `tuNgay`; thiếu cả 2 bộ lọc).

#### GET /api/booking/khung-gio/nhieu-ngay

**Quyền:** Công khai. Các ca và khung giờ của 1 bác sĩ trong tối đa 7 ngày liền, cho lịch tuần trên trang bác sĩ.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idBacSi` | số | có | |
| `tuNgay` | ngày | không | mặc định hôm nay |
| `denNgay` | ngày | không | mặc định `tuNgay` + 6; khoảng tối đa 7 ngày |

**Response:** `duLieu` = mảng [`CaKhamResponse`](#cakhamresponse) của mọi ngày trong khoảng.

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (`denNgay` trước `tuNgay`, khoảng quá 7 ngày).

#### GET /api/booking/khung-gio/gop

**Quyền:** Công khai. Khung giờ của chuyên khoa trong 1 ngày, gộp mọi bác sĩ theo giờ bắt đầu: dùng khi người đặt chọn
**"bác sĩ bất kỳ"**. Khung của 2 ca bắt đầu lệch nhau (08:00 và 08:30) là 2 dòng riêng.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idChuyenKhoa` | số | có | |
| `ngay` | ngày | có | |

**Response:** `duLieu` = mảng `{ gioBatDau, gioKetThuc, tongSoCho, soChoConLai, hetCho, soBacSi }`. `gioKetThuc` là giờ kết
thúc muộn nhất trong các khung được gộp; `soBacSi` là số bác sĩ có khung bắt đầu giờ này. Đặt lịch bằng `idChuyenKhoa` +
`gioBatDau` ([mục 3.5](#35-đặt-lịch)).

```json
[ { "gioBatDau": "2026-10-25T08:00:00", "gioKetThuc": "2026-10-25T09:00:00", "tongSoCho": 8, "soChoConLai": 3,
    "hetCho": false, "soBacSi": 2 } ]
```

#### GET /api/booking/khung-gio/ngay-som-nhat

**Quyền:** Công khai. Ngày còn chỗ sớm nhất của từng bác sĩ thuộc chuyên khoa, để hiện trên thẻ bác sĩ.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idChuyenKhoa` | số | có | |

**Response:** `duLieu` = `[{ idBacSi, ngay, soChoConLai }]`. Bác sĩ hết chỗ trong khoảng đặt được thì không có trong mảng.

```json
[ { "idBacSi": 3, "ngay": "2026-10-25", "soChoConLai": 7 } ]
```

#### GET /api/booking/ngay-nghi

**Quyền:** Công khai. Ngày bệnh viện nghỉ (không có lượt khám nào), để làm mờ ngày trên lịch và hiện lý do.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuNgay` | ngày | không | mặc định hôm nay |
| `denNgay` | ngày | không | mặc định 1 năm sau `tuNgay`; khoảng tối đa 366 ngày |

**Response:** `duLieu` = `[{ ngay, lyDo }]`, `lyDo` theo ngôn ngữ của request.

```json
[ { "ngay": "2027-01-01", "lyDo": "Nghỉ Tết Dương lịch" } ]
```

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (`denNgay` trước `tuNgay`, khoảng quá 366 ngày).

#### GET /api/booking/quy-tac-dat-lich

**Quyền:** Công khai. Các giới hạn do quản trị viên cấu hình. Frontend đọc số ở đây để hiện "đặt trước tối đa N ngày",
"hủy / đổi trước giờ khám X phút"; không viết cứng.

**Request:** Không có.

**Response:**

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `datTruocToiThieuPhut` | số | lượt khám chỉ đặt được khi còn cách giờ bắt đầu ít nhất chừng này phút |
| `soNgayDatTruocToiDa` | số | chỉ đặt được trong số ngày tới này, tính từ hôm nay |
| `huyDoiTruocToiThieuPhut` | số | hủy / đổi được khi còn cách giờ khám ít nhất chừng này phút |
| `soLanDoiLichToiDa` | số | số lần đổi tối đa của 1 lần đặt (0 = không đổi được) |
| `soLichHieuLucToiDaMoiHoSo` | số | 1 người khám giữ tối đa chừng này lịch sắp tới còn hiệu lực |

```json
{ "datTruocToiThieuPhut": 30, "soNgayDatTruocToiDa": 30, "huyDoiTruocToiThieuPhut": 120, "soLanDoiLichToiDa": 2,
  "soLichHieuLucToiDaMoiHoSo": 3 }
```

### 3.5 Đặt lịch

#### POST /api/booking/lich-hen

**Quyền:** Công khai. Khách không cần token. **Bệnh nhân đã đăng nhập phải gửi token**: khi đó lịch được lưu vào tài khoản
("lịch hẹn của tôi"). Token của bác sĩ / quản trị viên bị từ chối.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idLichLamViec` | số | có điều kiện | cách 1, **chọn bác sĩ**: `idLichLamViec` của [`CaKhamResponse`](#cakhamresponse) |
| `idChuyenKhoa` | số | có điều kiện | cách 2, **"bác sĩ bất kỳ"**: hệ thống xếp vào bác sĩ còn nhiều chỗ nhất của khung giờ. Gửi đúng 1 trong `idLichLamViec` / `idChuyenKhoa` |
| `gioBatDauKhung` | ngày giờ | có | `khungGio[].gioBatDau` (cách 1) hoặc `gioBatDau` của `khung-gio/gop` (cách 2) |
| `benhNhan` | [`BenhNhanRequest`](#benhnhanrequest) | có | người khám |
| `nguoiGiamHo` | [`NguoiGiamHoRequest`](#nguoigiamhorequest) | có điều kiện | bắt buộc khi người khám dưới 18 tuổi tính theo ngày khám |
| `lyDoKham` | chuỗi | có | tối đa 500 |
| `datChoBanThan` | boolean | không | chỉ khi đã đăng nhập. `true`: người khám là chủ tài khoản, lịch dùng hồ sơ bệnh nhân của tài khoản (chưa có thì tạo bằng `benhNhan.cccd` và gắn vào tài khoản). Bỏ trống / `false`: đặt cho người thân |
| `luuNguoiThan` | boolean | không | chỉ khi đã đăng nhập và đặt cho người thân. Bỏ trống / `true`: lưu vào "người thân đã lưu" để điền sẵn lần sau; `false`: không lưu |

Quy tắc cần biết:
- 1 người khám (theo hồ sơ CCCD) giữ tối đa `soLichHieuLucToiDaMoiHoSo` lịch sắp tới; 1 SĐT liên hệ giữ tối đa 5 (mặc định).
  Người khám không có 2 lịch còn hiệu lực trùng khung giờ.
- Mỗi IP gọi tối đa 20 lần / 10 phút (mặc định), tính chung cho đặt lịch, hủy / đổi bằng link phiếu khám và đổi lịch của tôi.
- SĐT liên hệ của lượt khám là SĐT người giám hộ nếu có, không thì `benhNhan.soDienThoai`. SĐT này dùng để hủy / đổi bằng
  link, xin cấp lại link và mở sổ khám, nên nhắc người dùng nhớ nó.
- Sau khi đặt: chuyển tới trang phiếu khám (`maPhieuKham`). Với khách, đây là cách duy nhất xem lại lịch: nhắc lưu link /
  QR; email (nếu có nhập) cũng gửi link.

```json
{ "idLichLamViec": 120, "gioBatDauKhung": "2026-10-25T08:00:00",
  "benhNhan": { "hoTen": "Nguyễn Văn An", "ngaySinh": "1995-04-12", "gioiTinh": "NAM", "soDienThoai": "0901000001",
    "cccd": "079095000001", "email": "an@example.com" },
  "lyDoKham": "Ho kéo dài 2 tuần" }
```

Đặt cho trẻ dưới 18 tuổi: thêm `"nguoiGiamHo": { "hoTen": "...", "quanHe": "ME", "soDienThoai": "...", "cccd": "..." }`;
`benhNhan.cccd` được bỏ trống. "Bác sĩ bất kỳ": thay `idLichLamViec` bằng `"idChuyenKhoa": 1`.

**Response:** HTTP 201, `duLieu` = [`DatLichResponse`](#datlichresponse).

**Lỗi:**
- 400:
  - `DU_LIEU_KHONG_HOP_LE`: gửi cả hai / không gửi `idLichLamViec` và `idChuyenKhoa`; `gioBatDauKhung` không phải giờ
    bắt đầu khung của ca; người từ đủ 18 tuổi thiếu `cccd`; `datChoBanThan` mà thiếu `cccd`;
  - `THIEU_NGUOI_GIAM_HO`: người khám dưới 18 tuổi mà thiếu `nguoiGiamHo`;
  - `NGUOI_GIAM_HO_KHONG_HOP_LE`: người giám hộ trùng CCCD với người khám, hoặc chưa đủ 18 tuổi.
- 403 `KHONG_CO_QUYEN`: token của bác sĩ / quản trị viên (đăng xuất để đặt như khách).
- 409:
  - `KHUNG_GIO_KHONG_CON_TRONG`: khung vừa hết chỗ, cho chọn lại;
  - `KHUNG_GIO_KHONG_KHA_DUNG`: ca không còn nhận đặt, quá giờ đặt hoặc ngoài khoảng ngày;
  - `LICH_HEN_TRUNG_GIO`: người khám đã có lịch trong khung này;
  - `VUOT_GIOI_HAN_DAT_LICH`: đủ số lịch sắp tới theo người khám / SĐT;
  - `CCCD_DA_CO_HO_SO`: đặt cho bản thân bằng CCCD đã có hồ sơ (phải qua xác minh, xem `PUT /api/booking/ho-so-benh-nhan/cua-toi`);
  - `HO_SO_CHO_XAC_MINH`: đặt cho bản thân khi hồ sơ đang chờ xác minh;
  - `THONG_TIN_BENH_NHAN_KHONG_KHOP`: đặt cho bản thân nhưng CCCD khác hồ sơ của tài khoản.
- 429 `GUI_LAI_QUA_NHANH`: IP gọi quá số lần cho phép.

### 3.6 Phiếu khám theo link, cấp lại liên kết

Trang phiếu khám của frontend là `<FRONTEND_URL>/phieu-kham/<maPhieuKham>`. Các route `/api/booking/phieu-kham/**` công khai, không
cần token. Bệnh nhân đã đăng nhập nên dùng các route "của tôi" ([mục 3.8](#38-lịch-hẹn-của-tôi-và-đánh-giá)), không cần SĐT.
Mã sai dạng và mã không tồn tại trả cùng 1 lỗi 404.

#### GET /api/booking/phieu-kham/{maPhieuKham}

**Quyền:** Công khai. Xem phiếu khám.

**Request:** path `maPhieuKham`.

**Response:** `duLieu` = [`PhieuKhamResponse`](#phieukhamresponse). Lịch đã đổi thì phiếu cũ ở `DA_HUY_DO_DOI_LICH`; lịch mới
có `maPhieuKham` riêng.

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### POST /api/booking/phieu-kham/{maPhieuKham}/huy

**Quyền:** Công khai. Hủy lịch bằng link, phải nhập đúng SĐT liên hệ. Lượt khám được mở lại cho người khác.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `soDienThoai` | chuỗi | có | đúng SĐT liên hệ lúc đặt (có người giám hộ: SĐT người giám hộ), `0` + 9 số |
| `lyDo` | chuỗi | không | tối đa 500; bác sĩ, quản trị viên đọc được |

**Response:** `duLieu` = [`PhieuKhamResponse`](#phieukhamresponse) (`trangThai: "DA_HUY"`).

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (SĐT sai dạng); 403 `SO_DIEN_THOAI_KHONG_KHOP`; 404 `KHONG_TIM_THAY`; 409
`LICH_HEN_KHONG_HUY_DOI_DUOC` (không còn ở `DA_XAC_NHAN`), `QUA_HAN_HUY_DOI_LICH` (quá `hanHuyDoi`: liên hệ phòng khám); 429
`GUI_LAI_QUA_NHANH` (giới hạn IP như khi đặt lịch).

#### POST /api/booking/phieu-kham/{maPhieuKham}/doi-lich

**Quyền:** Công khai. Đổi sang khung giờ khác bằng link. Đổi lịch = hủy lịch cũ (`DA_HUY_DO_DOI_LICH`) + tạo lịch mới, thông
tin người khám lấy từ lịch cũ.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idLichLamViec` / `idChuyenKhoa` | số | có điều kiện | chọn khung giờ như khi đặt lịch, gửi đúng 1 trong 2 |
| `gioBatDauKhung` | ngày giờ | có | phải khác khung đang giữ |
| `soDienThoai` | chuỗi | có | như khi hủy |

**Response:** HTTP 201, `duLieu` = [`DatLichResponse`](#datlichresponse) của **lịch mới**: chuyển sang phiếu khám mới
(`maPhieuKham` mới).

**Lỗi:** như hủy bằng link; thêm 409 `VUOT_SO_LAN_DOI_LICH` (đủ `soLanDoiLichToiDa` lần; vẫn hủy được), 400
`DU_LIEU_KHONG_HOP_LE` (chọn lại đúng khung đang giữ), và các lỗi chọn khung giờ của `POST /api/booking/lich-hen`
(`KHUNG_GIO_KHONG_CON_TRONG`, `KHUNG_GIO_KHONG_KHA_DUNG`, `LICH_HEN_TRUNG_GIO`).

#### GET /api/booking/phieu-kham/{maPhieuKham}/lich.ics

**Quyền:** Công khai. Tệp lịch để thêm vào Google Calendar / Outlook / điện thoại: giờ khám, bác sĩ, phòng, mã tra cứu, link
phiếu khám (không có họ tên, SĐT, CCCD). Lịch đã hủy / đổi đi thì sự kiện ở trạng thái `CANCELLED`.

**Request:** path `maPhieuKham`.

**Response:** tệp `text/calendar` (không bọc khung), tên `lich-kham-<maTraCuu>.ics`. Dùng thẳng làm `href` của nút "Thêm
vào lịch".

**Lỗi:** 404 `KHONG_TIM_THAY` (JSON).

#### GET /api/booking/phieu-kham/{maPhieuKham}/qr

**Quyền:** Công khai. Ảnh QR chứa `linkPhieuKham`.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `kichThuoc` | số | không | cạnh ảnh (pixel), 100–1000, mặc định 300 |
| `taiVe` | boolean | không | `true`: trả kèm `Content-Disposition: attachment` (`phieu-kham-qr.png`) để trình duyệt lưu tệp |

**Response:** ảnh `image/png`. Dùng thẳng làm `<img src>`; nút "Tải QR" trỏ tới URL kèm `taiVe=true`.

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (`kichThuoc` ngoài khoảng); 404 `KHONG_TIM_THAY`. Lỗi vẫn trả JSON, không phải ảnh.

#### POST /api/booking/cap-lai-lien-ket

**Quyền:** Công khai. Khách mất link phiếu khám xin gửi lại qua **email của lịch hẹn**. Link không bao giờ có trong phản hồi.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `maTraCuu` | chuỗi | có | `ECL-yyyyMMdd-nnnn`, không phân biệt hoa thường, cho phép khoảng trắng 2 đầu |
| `soDienThoai` | chuỗi | có | đầy đủ SĐT liên hệ lúc đặt (có người giám hộ: SĐT người giám hộ) |

**Response:** không có `duLieu`. **Luôn trả 200 cùng 1 `thongDiep`**, dù mã có thật hay không, SĐT đúng hay sai, lịch có
email hay không. Frontend hiện câu đó và hướng dẫn kiểm tra email; không nói "đã gửi thành công". Lịch đã đổi thì gửi phiếu
của lịch mới nhất; đã khám thì kèm link sổ khám.

**Lỗi:** 429 `GUI_LAI_QUA_NHANH`: mỗi mã tối đa 3 lần, mỗi IP tối đa 10 lần trong 1 giờ (mặc định; đúng hay sai đều tính).
`thongDiep` có số phút còn phải chờ.

### 3.7 Sổ khám bệnh

Sổ khám là trang công khai của 1 lượt khám đã có kết quả, mở theo `linkSoKham` (`<FRONTEND_URL>/so-kham/<maSoKham>`, có trong
email và trong [`KetQuaKhamResponse`](#ketquakhamresponse)). Xem theo 2 bước:
1. `GET` lấy phần tóm tắt đã che.
2. Người xem nhập 4 số cuối SĐT liên hệ, `POST …/xem` trả kết quả khám đầy đủ.

#### GET /api/booking/so-kham/{maSoKham}

**Quyền:** Công khai. Phần đầu sổ khám, đủ để nhận ra lượt khám, không có kết quả.

**Request:** path `maSoKham`.

**Response:**

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `hoTenChe` | chuỗi | chỉ giữ tên, còn lại viết tắt (`"N. V. An"`) |
| `namSinh` | số \| null | |
| `ngayKham` | ngày | |
| `gioKham` | giờ | giờ khám dự kiến |
| `bacSi` | [`BacSiTomTatResponse`](#bacsitomtatresponse) | |
| `tenChuyenKhoa` | chuỗi | |
| `phongKham` | [`PhongKhamTomTatResponse`](#phongkhamtomtatresponse) | |

```json
{ "hoTenChe": "N. V. An", "namSinh": 1995, "ngayKham": "2026-10-25", "gioKham": "08:30:00",
  "bacSi": { "id": 3, "hoTen": "Trần Minh", "hocVi": "ThS.BS", "anhDaiDien": null }, "tenChuyenKhoa": "Nội tổng quát",
  "phongKham": { "id": 2, "tenPhong": "Phòng khám Nội 1", "tang": "Tầng 2" } }
```

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### POST /api/booking/so-kham/{maSoKham}/xem

**Quyền:** Công khai. Sổ khám đầy đủ khi 4 số cuối khớp SĐT liên hệ lúc đặt (có người giám hộ: SĐT người giám hộ). Gửi trong
body để 4 số không nằm trên URL.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `bonSoCuoi` | chuỗi | có | đúng 4 chữ số |

**Response:** `duLieu` = `{ tomTat, benhNhan, hoTenNguoiGiamHo, ketQua }`:
- `tomTat`: như `GET /api/booking/so-kham/{maSoKham}`;
- `benhNhan`: `{ hoTen, ngaySinh, gioiTinh, cccdChe }` (CCCD chỉ giữ 3 số cuối);
- `hoTenNguoiGiamHo`: chuỗi hoặc null;
- `ketQua`: [`KetQuaKhamResponse`](#ketquakhamresponse).

**Lỗi:** 403 `SO_DIEN_THOAI_KHONG_KHOP`; 429 `GUI_LAI_QUA_NHANH`: sai 5 lần trong 15 phút thì sổ bị khoá tới hết 15 phút,
kể cả khi nhập đúng; `thongDiep` có số phút còn lại; 404 `KHONG_TIM_THAY`.

#### GET /api/booking/so-kham/{maSoKham}/qr

**Quyền:** Công khai. Ảnh QR chứa `linkSoKham`. Query `kichThuoc`, `taiVe` và cách dùng như
`GET /api/booking/phieu-kham/{maPhieuKham}/qr` (tệp tải về `so-kham-qr.png`).

**Response:** ảnh `image/png`.

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE`; 404 `KHONG_TIM_THAY` (JSON).

### 3.8 Lịch hẹn của tôi và đánh giá

"Lịch hẹn của tôi" gồm:
- lịch tài khoản đã đặt (cho bản thân hoặc người thân);
- lịch của hồ sơ bệnh nhân đã liên kết, kể cả lịch đặt như khách bằng CCCD đó;
- lịch mà chủ tài khoản là người giám hộ.

Chủ tài khoản chỉ hủy / đổi / đánh giá được lịch mình đặt hoặc mình là người khám. Dùng `duocHuyDoi`, `duocDanhGia` để
hiện nút.

#### GET /api/booking/lich-hen/cua-toi

**Quyền:** BENH_NHAN. Danh sách lịch hẹn, phân trang.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `loc` | `LocLichHen` | không | mặc định `TAT_CA` |
| `cuaAi` | `PhamViLichHen` | không | mặc định `TAT_CA` |
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của [`LichHenCuaToiResponse`](#lichhencuatoiresponse).

#### GET /api/booking/lich-hen/cua-toi/lich

**Quyền:** BENH_NHAN. Lịch hẹn theo khoảng ngày cho màn hình lịch: mọi trạng thái, theo giờ khám, không phân trang.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `tuNgay`, `denNgay` | ngày | có | khoảng tối đa 42 ngày (lưới tháng 6 tuần) |
| `cuaAi` | `PhamViLichHen` | không | mặc định `TAT_CA` |

**Response:** `duLieu` = mảng [`LichHenCuaToiResponse`](#lichhencuatoiresponse).

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (`denNgay` trước `tuNgay`, khoảng quá 42 ngày).

#### GET /api/booking/lich-hen/cua-toi/{maPhieuKham}

**Quyền:** BENH_NHAN. Chi tiết 1 lịch hẹn, kèm kết quả khám và đánh giá.

**Request:** path `maPhieuKham`.

**Response:**

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `lichHen` | [`LichHenCuaToiResponse`](#lichhencuatoiresponse) | |
| `ngaySinhBenhNhan` | ngày | `laBanThan`: theo hồ sơ của chủ tài khoản; còn lại: như người đặt đã nhập |
| `gioiTinhBenhNhan` | `GioiTinh` | |
| `soDienThoaiLienHe`, `emailLienHe` | chuỗi \| null | null nếu lịch không do tài khoản này đặt (email còn null khi không nhập) |
| `ketQua` | [`KetQuaKhamResponse`](#ketquakhamresponse) \| null | null khi chưa khám xong, hoặc tài khoản không được xem kết quả (chỉ người khám `laBanThan` hoặc người đặt `nguoiDat = TOI` được xem) |
| `duocDanhGia` | boolean | `true`: đã khám xong, được xem kết quả và chưa đánh giá: hiện form đánh giá |
| `danhGia` | object \| null | đánh giá đã gửi `{ soSao, nhanXet, ngayTao, duocSuaDen }` |

**Lỗi:** 404 `KHONG_TIM_THAY` (không có hoặc không thuộc tài khoản).

#### POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/huy

**Quyền:** BENH_NHAN. Hủy lịch của tôi trước `hanHuyDoi`. Lượt khám được mở lại cho người khác.

| Trường (body, có thể bỏ cả body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `lyDo` | chuỗi | không | tối đa 500 |

**Response:** `duLieu` = [`PhieuKhamResponse`](#phieukhamresponse) (`trangThai: "DA_HUY"`).

**Lỗi:** 403 `KHONG_CO_QUYEN` (chủ tài khoản chỉ là người giám hộ, không phải người đặt / người khám); 404
`KHONG_TIM_THAY`; 409 `LICH_HEN_KHONG_HUY_DOI_DUOC`, `QUA_HAN_HUY_DOI_LICH`.

#### POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/doi-lich

**Quyền:** BENH_NHAN. Đổi lịch của tôi sang khung giờ khác.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idLichLamViec` / `idChuyenKhoa` | số | có điều kiện | như khi đặt lịch, gửi đúng 1 trong 2 |
| `gioBatDauKhung` | ngày giờ | có | phải khác khung đang giữ |

**Response:** HTTP 201, `duLieu` = [`DatLichResponse`](#datlichresponse) của **lịch mới** (lịch cũ thành
`DA_HUY_DO_DOI_LICH`).

**Lỗi:** như hủy; thêm 409 `VUOT_SO_LAN_DOI_LICH`, các lỗi chọn khung giờ của `POST /api/booking/lich-hen`, và 429
`GUI_LAI_QUA_NHANH` (giới hạn IP như khi đặt lịch).

#### POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/danh-gia

**Quyền:** BENH_NHAN. Đánh giá lượt khám đã hoàn thành, mỗi lịch hẹn 1 lần. Chỉ người khám hoặc người đặt.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `soSao` | số | có | 1–5 |
| `nhanXet` | chuỗi | không | tối đa 1000; chỉ bác sĩ và quản trị viên đọc, không công khai |

**Response:** HTTP 201, `duLieu` = `{ soSao, nhanXet, ngayTao, duocSuaDen }` (`duocSuaDen`: sửa được tới lúc này).

```json
{ "soSao": 5, "nhanXet": "Bác sĩ tận tình", "ngayTao": "2026-10-25T10:00:00", "duocSuaDen": "2026-11-01T10:00:00" }
```

**Lỗi:** 403 `KHONG_CO_QUYEN`; 404 `KHONG_TIM_THAY`; 409 `LICH_HEN_CHUA_KHAM_XONG`, `DA_DANH_GIA`.

#### PUT /api/booking/lich-hen/cua-toi/{maPhieuKham}/danh-gia

**Quyền:** BENH_NHAN. Sửa đánh giá đã gửi, trong thời hạn sửa (mặc định 7 ngày từ lúc gửi).

**Request:** body như `POST`.

**Response:** `duLieu` = `{ soSao, nhanXet, ngayTao, duocSuaDen }`.

**Lỗi:** 403 `KHONG_CO_QUYEN`; 404 `KHONG_TIM_THAY` (chưa có đánh giá); 409 `HET_HAN_SUA_DANH_GIA` (quá `duocSuaDen`).

### 3.9 Hồ sơ, người thân, lịch sử khám, trang cá nhân

#### GET /api/booking/ho-so-benh-nhan/cua-toi

**Quyền:** BENH_NHAN. Hồ sơ bệnh nhân của tài khoản.

**Request:** Không có.

**Response:** `duLieu` = [`HoSoCuaToiResponse`](#hosocuatoiresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`: tài khoản chưa có hồ sơ (hiện form tạo hồ sơ).

#### PUT /api/booking/ho-so-benh-nhan/cua-toi

**Quyền:** BENH_NHAN. Tạo hồ sơ (khi chưa có) hoặc sửa. Trường tuỳ chọn gửi trống thì bị xoá trắng.

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `hoTen` | chuỗi | có | tối đa 150 |
| `ngaySinh` | ngày | có | không ở tương lai |
| `gioiTinh` | `GioiTinh` | không | |
| `soDienThoai` | chuỗi | có | `^0\d{9}$` |
| `cccd` | chuỗi | có điều kiện | 12 chữ số. Bắt buộc khi tạo; khi sửa thì bỏ trống hoặc gửi đúng số đang lưu (không đổi được) |
| `diaChi` | chuỗi | không | tối đa 500 |
| `soBaoHiemYTe` | chuỗi | không | tối đa 50 |
| `tienSuBenhLy` | chuỗi | không | tối đa 5000 |

Khi tạo bằng CCCD **đã có hồ sơ** (từng đặt lịch như khách):
- họ tên khớp, kèm ngày sinh hoặc SĐT khớp: liên kết ngay (`DA_LIEN_KET`), các lượt khám cũ hiện ra;
- không khớp: trả `trangThaiLienKet: "CHO_XAC_MINH"`, thông tin trong form **không được lưu**. Chờ quản trị viên xác minh;
  trong lúc chờ không sửa được hồ sơ và không đặt "cho bản thân" được.

**Response:** `duLieu` = [`HoSoCuaToiResponse`](#hosocuatoiresponse).

**Lỗi:** 400 `DU_LIEU_KHONG_HOP_LE` (thiếu CCCD khi tạo, đổi CCCD); 409 `CCCD_DA_CO_HO_SO` (CCCD đã thuộc tài khoản khác,
hoặc lần xác minh trước bị từ chối: liên hệ phòng khám), `HO_SO_CHO_XAC_MINH`.

#### GET /api/booking/thong-tin-dat-lich/cua-toi

**Quyền:** BENH_NHAN. Mọi thứ để điền sẵn form đặt lịch, trong 1 lần gọi.

**Request:** Không có.

**Response:**

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `banThan` | [`HoSoCuaToiResponse`](#hosocuatoiresponse) \| null | cho "đặt cho bản thân"; null nếu chưa có hồ sơ (đặt cho bản thân lần đầu thì nhập CCCD, hồ sơ được tạo) |
| `emailTaiKhoan` | chuỗi | gợi ý cho ô email |
| `nguoiThan[]` | mảng [`NguoiThanDaLuuResponse`](#nguoithandaluuresponse) | người vừa đặt gần nhất đứng trước |
| `lanDatGanNhat` | object \| null | `{ idChuyenKhoa, tenChuyenKhoa, idBacSi, hoTenBacSi }` của lần đặt gần nhất; null nếu chưa đặt |

```json
{ "banThan": { "trangThaiLienKet": "DA_LIEN_KET", "cccd": "079095000001", "hoTen": "Nguyễn Văn An",
    "ngaySinh": "1995-04-12", "gioiTinh": "NAM", "soDienThoai": "0901000001", "diaChi": null, "soBaoHiemYTe": null,
    "tienSuBenhLy": null },
  "emailTaiKhoan": "benhnhan01@eclinic.local",
  "nguoiThan": [ { "id": 7, "hoTen": "Nguyễn Bảo Ngọc", "ngaySinh": "2016-03-02", "gioiTinh": "NU", "cccd": null,
    "soDienThoai": "0901000001", "email": null, "diaChi": null, "soBaoHiemYTe": null,
    "nguoiGiamHo": { "hoTen": "Nguyễn Văn An", "quanHe": "CHA", "soDienThoai": "0901000001", "cccd": "079095000001",
      "ngaySinh": null },
    "lanDungCuoi": "2026-10-20T09:00:00" } ],
  "lanDatGanNhat": { "idChuyenKhoa": 1, "tenChuyenKhoa": "Nội tổng quát", "idBacSi": 3, "hoTenBacSi": "Trần Minh" } }
```

#### PUT /api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id}

**Quyền:** BENH_NHAN. Sửa bản lưu của 1 người thân. Chỉ sửa bản lưu: hồ sơ bệnh nhân và lịch đã đặt không đổi. Không có
`cccd` (không đổi được; nhập sai thì bỏ người này rồi đặt lại).

| Trường (body) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `hoTen`, `ngaySinh`, `gioiTinh`, `soDienThoai` | | có | như [`BenhNhanRequest`](#benhnhanrequest) |
| `email`, `soBaoHiemYTe`, `diaChi` | chuỗi | không | như `BenhNhanRequest`; gửi trống thì bị xoá trắng |
| `nguoiGiamHo` | [`NguoiGiamHoRequest`](#nguoigiamhorequest) | không | bỏ trống thì bản lưu không còn người giám hộ |

**Response:** `duLieu` = [`NguoiThanDaLuuResponse`](#nguoithandaluuresponse).

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### DELETE /api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id}

**Quyền:** BENH_NHAN. Bỏ 1 người khỏi danh sách đã lưu; lịch đã đặt không bị ảnh hưởng.

**Response:** không có `duLieu`.

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### GET /api/booking/lich-su-kham/cua-toi

**Quyền:** BENH_NHAN. Các lượt đã khám mà tài khoản được xem kết quả (tôi là người khám, hoặc tôi đã đặt), mới nhất trước.

| Trường (query) | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `cuaAi` | `PhamViLichHen` | không | mặc định `TAT_CA` |
| `trang`, `kichThuoc` | số | không | từ 0; 1–100, mặc định 20 |

**Response:** `duLieu` = `TrangDuLieu` của `{ lichHen: LichHenCuaToiResponse, ketQua: KetQuaKhamResponse }`
([`LichHenCuaToiResponse`](#lichhencuatoiresponse), [`KetQuaKhamResponse`](#ketquakhamresponse)).

#### GET /api/booking/trang-ca-nhan/cua-toi

**Quyền:** BENH_NHAN. Mọi thứ trang cá nhân hiển thị, trong 1 lần gọi.

**Request:** Không có.

**Response:**

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `hoSo` | [`HoSoCuaToiResponse`](#hosocuatoiresponse) \| null | null nếu chưa có hồ sơ |
| `emailTaiKhoan` | chuỗi | |
| `nguoiThan[]` | mảng [`NguoiThanDaLuuResponse`](#nguoithandaluuresponse) | |
| `lichSapToi` | object | `{ cuaToi[], cuaNguoiKhac[] }`: các lịch sắp tới gần nhất của mỗi bên, phần tử là [`LichHenCuaToiResponse`](#lichhencuatoiresponse) |
| `lanKhamGanDay[]` | mảng | các lượt đã khám gần nhất được xem kết quả: `{ lichHen, ketQua }` như lịch sử khám |
| `soLich` | object | `{ sapToiCuaToi, sapToiCuaNguoiKhac, lichSu, daKham }`: tổng số để hiện số đếm và biết còn lịch ngoài các dòng trả kèm |

### 3.10 Chatbot

Trợ lý AI trả lời câu hỏi về phòng khám, gợi ý chuyên khoa / bác sĩ / lịch còn chỗ. Bệnh nhân đồng ý thì nó đọc được lịch hẹn
của chính họ.
- **Phiên của khách:** `POST /api/chatbot/phien` không token trả `maKhach` (chỉ 1 lần, server không cấp lại). Gửi
  `X-Chat-Khach: <maKhach>` ở mọi request sau của phiên. Mất mã thì mở phiên mới.
- **Phiên của tài khoản:** mở khi có token; các request sau phải gửi token của đúng tài khoản đó.
- Sai chủ phiên, sai `maKhach` hay phiên không tồn tại đều trả 404 `KHONG_TIM_THAY`.
- Giới hạn:
  - gateway: 30 `POST` / phút / IP ([mục 1.4](#14-lỗi-dùng-chung));
  - service, theo tài khoản (khách theo IP): 10 phiên mới / giờ, 20 tin / 10 phút, 100 tin / ngày, cộng hạn mức toàn hệ thống
    mỗi ngày;
  - vượt giới hạn service trả 429 `CHAT_QUA_NHIEU`, `thongDiep` nói rõ loại giới hạn.
- Server chưa cấu hình mô hình: mở phiên vẫn được, gửi tin trả 503 `CHATBOT_KHONG_KHA_DUNG`.

Mô hình tin nhắn `TinNhanChatResponse`:

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | số | |
| `nguoiGui` | chuỗi | `NGUOI_DUNG`, `CHATBOT` |
| `loai` | chuỗi | tin người dùng: `CAU_HOI`. Tin chatbot: `LOI_CHAO`, `TRA_LOI`, `NGOAI_PHAM_VI` (từ chối, câu cố định), `KHAN_CAP` (dấu hiệu cấp cứu: hiện nổi bật "gọi 115"), `CAN_DONG_Y` (hiện nút "Đồng ý", xem `POST …/dong-y`) |
| `noiDung` | chuỗi | như lúc lưu, không đổi theo ngôn ngữ khi đọc lại |
| `ngayTao` | ngày giờ | |
| `goiY` | object \| null | chỉ ở `TRA_LOI` có tra cứu dữ liệu. Là ảnh chụp lúc trả lời: form đặt lịch phải đọc lại khung giờ |

`goiY` = `{ chuyenKhoa, bacSi[], lichKham[], lichHenCuaToi, lanKham }`:
- `chuyenKhoa`: `{ id, ten }` hoặc null;
- `bacSi[]`: `{ id, hoTen, hocVi, chucVu, anhDaiDien, ngaySomNhat, soChoConLai }`, còn chỗ sớm nhất đứng trước;
- `lichKham[]`: các ca còn chỗ của 1 bác sĩ trong tối đa 7 ngày, mỗi ca
  `{ idBacSi, hoTenBacSi, idLichLamViec, ngay, tenPhong, tang, khungGio[]: { gioBatDau, gioKetThuc, soChoConLai } }`. Mở form
  đặt lịch bằng `idLichLamViec` + `gioBatDau`;
- `lichHenCuaToi`: null nếu không tra cứu, `[]` nếu không có. Mỗi lịch
  `{ maPhieuKham, maTraCuu, linkPhieuKham, trangThai, ngay, gioBatDauKhung, gioKetThucKhung, hoTenBacSi, tenChuyenKhoa, tenPhong, tang, hoTenBenhNhan, laBanThan, canDoiLich }`;
- `lanKham`: null / `[]` như trên. Mỗi lượt
  `{ maPhieuKham, maTraCuu, ngay, hoTenBacSi, tenChuyenKhoa, hoTenBenhNhan, laBanThan, ngayTaiKhamDeXuat }`.

Ngày giờ trong `goiY` là chuỗi ISO (`"2026-10-25"`, `"2026-10-25T08:00:00"`).

#### POST /api/chatbot/phien

**Quyền:** Công khai. Mở phiên mới, trả câu chào.

**Request:** Không có body.

**Response:** HTTP 201, `duLieu` = `{ idPhien, maKhach, thoiGianBatDau, tinNhan[] }`. `maKhach` **chỉ có khi chưa đăng
nhập** (khi có token, trường này vắng mặt); `tinNhan` chứa câu chào `LOI_CHAO`.

```json
{ "idPhien": 41, "maKhach": "fP0aJ3kL8qW2xZ5cV7bN1mR4tY6uI9oE3sD2gH5jK0l", "thoiGianBatDau": "2026-10-21T14:00:00",
  "tinNhan": [ { "id": 301, "nguoiGui": "CHATBOT", "loai": "LOI_CHAO",
    "noiDung": "Xin chào, tôi là trợ lý AI của eClinic...", "ngayTao": "2026-10-21T14:00:00", "goiY": null } ] }
```

**Lỗi:** 429 `CHAT_QUA_NHIEU` (mở quá nhiều phiên).

#### GET /api/chatbot/phien

**Quyền:** Đăng nhập (mọi vai trò). Các phiên của tài khoản, mới nhất trước, tối đa 50.

**Request:** Không có.

**Response:** `duLieu` = `[{ idPhien, thoiGianBatDau, daDongYDuLieu }]` (`daDongYDuLieu`: đã đồng ý cho đọc lịch hẹn trong
phiên này).

#### POST /api/chatbot/phien/{idPhien}/tin-nhan

**Quyền:** Công khai; khách gửi header `X-Chat-Khach`, tài khoản gửi token. Gửi 1 tin nhắn, nhận câu trả lời.

| Trường | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `idPhien` (path) | số | có | |
| `X-Chat-Khach` (header) | chuỗi | có điều kiện | bắt buộc với phiên của khách |
| `noiDung` (body) | chuỗi | có | tối đa 1000 ký tự |

**Response:** `duLieu` = `{ tinNhan, traLoi }`: tin vừa lưu và câu trả lời, đều là `TinNhanChatResponse`. Gặp `CAN_DONG_Y`:
hiện nút "Đồng ý", gọi `POST …/dong-y`, rồi gửi lại câu hỏi. Chatbot chỉ đọc lịch hẹn khi người gửi là bệnh nhân đăng nhập,
phiên thuộc chính tài khoản đó và đã đồng ý.

```json
{ "tinNhan": { "id": 302, "nguoiGui": "NGUOI_DUNG", "loai": "CAU_HOI", "noiDung": "Tôi bị ho kéo dài nên khám khoa nào?",
    "ngayTao": "2026-10-21T14:01:00", "goiY": null },
  "traLoi": { "id": 303, "nguoiGui": "CHATBOT", "loai": "TRA_LOI", "noiDung": "Bạn nên khám Nội tổng quát...",
    "ngayTao": "2026-10-21T14:01:04",
    "goiY": { "chuyenKhoa": { "id": 1, "ten": "Nội tổng quát" },
      "bacSi": [ { "id": 3, "hoTen": "Trần Minh", "hocVi": "ThS.BS", "chucVu": null, "anhDaiDien": null,
        "ngaySomNhat": "2026-10-25", "soChoConLai": 7 } ],
      "lichKham": [], "lichHenCuaToi": null, "lanKham": null } } }
```

**Lỗi:** 404 `KHONG_TIM_THAY`; 429 `CHAT_QUA_NHIEU`; 503 `CHATBOT_KHONG_KHA_DUNG` (không lưu gì, gửi lại được).

#### GET /api/chatbot/phien/{idPhien}/tin-nhan

**Quyền:** Công khai; khách gửi header `X-Chat-Khach`, tài khoản gửi token. Toàn bộ tin nhắn của phiên, cũ nhất trước.

**Request:** path `idPhien`; header `X-Chat-Khach` với phiên của khách.

**Response:** `duLieu` = mảng `TinNhanChatResponse`.

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### POST /api/chatbot/phien/{idPhien}/dong-y

**Quyền:** BENH_NHAN. Đồng ý cho chatbot đọc lịch hẹn, lượt khám của chính mình trong phiên này. Mô hình chỉ nhận ngày giờ,
bác sĩ, chuyên khoa, phòng, trạng thái, ngày tái khám (không nhận họ tên, mã). Chỉ phiên của chính tài khoản: phiên mở khi
chưa đăng nhập trả 404.

**Request:** path `idPhien`.

**Response:** không có `duLieu`.

**Lỗi:** 404 `KHONG_TIM_THAY`.

#### DELETE /api/chatbot/phien/{idPhien}/dong-y

**Quyền:** BENH_NHAN. Rút lại đồng ý: chatbot thôi đọc lịch hẹn và thôi gửi lại các câu trả lời cũ về lịch hẹn cho mô hình.

**Request:** path `idPhien`.

**Response:** không có `duLieu`.

**Lỗi:** 404 `KHONG_TIM_THAY`.
