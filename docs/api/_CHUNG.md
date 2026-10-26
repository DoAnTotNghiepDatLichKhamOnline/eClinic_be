<!-- Nguồn của phần chung trong 3 tài liệu API (API-BENH-NHAN-KHACH.md, API-BAC-SI.md, API-QUAN-TRI.md). Không đọc riêng
file này: sửa ở đây rồi chạy `node scripts/kiem-tra-tai-lieu-api.js` để chép vào 3 tài liệu. Mọi thứ sau dòng HET-GHI-CHU
được chép nguyên văn. -->
<!-- HET-GHI-CHU -->
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
