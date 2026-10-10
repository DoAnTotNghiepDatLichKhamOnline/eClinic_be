# eClinic backend

Backend của đồ án tốt nghiệp **eClinic** (Nhóm 35): hệ thống đặt lịch khám bệnh trực tuyến cho bệnh viện đa khoa.
Kiến trúc **service-based**: nhiều service Spring Boot chạy riêng, dùng chung **một** MySQL và một Redis,
frontend React chỉ gọi qua **api-gateway** (cổng 8080).

```
React (5173)  ──►  api-gateway :8080  ──►  identity :8081   catalog :8082   booking :8084   medical :8085
                   (định tuyến, CORS)      notification :8086   chatbot :8087   report :8088
                                                          │
                                              MySQL 8.4 (1 database) + Redis
```

- Mỗi service tự kiểm tra JWT; gateway chỉ chuyển tiếp `/api/<service>/**` nguyên vẹn và trả 503/504 khi service chưa chạy / quá chậm.
  Riêng `/api/auth/**` và `/api/users/**` (theo tài liệu API, không có tên service) cũng về `identity-service`.
- Toàn bộ entity, enum, exception, bảo mật dùng chung và Flyway migration nằm trong module `common`.
- Cách viết code (đặt tên, thêm API mới, migration...): xem [docs/QUY-UOC-CODE.md](docs/QUY-UOC-CODE.md).

Công nghệ: Java 17, Spring Boot 4.1, Spring Cloud Gateway (WebMVC), Spring Security (JWT HS256), Spring Data JPA,
Flyway, MySQL 8.4, Redis, springdoc (Swagger), Maven, Docker Compose.

## Các module

| Module | Cổng | Chức năng (mã yêu cầu) | Swagger |
|---|---|---|---|
| `api-gateway` | 8080 | Cổng vào duy nhất cho frontend: định tuyến + CORS | (không có) |
| `identity-service` | 8081 | AUTH-01..04; hồ sơ cá nhân, đổi mật khẩu, thiết bị đăng nhập, ảnh đại diện, đổi email; quản trị tài khoản (UC-USER-01); dữ liệu mẫu | http://localhost:8081/swagger-ui.html |
| `catalog-service` | 8082 | ADM-01, ADM-02, AUTH-04 (hồ sơ bác sĩ), DOC-01, DOC-03 | http://localhost:8082/swagger-ui.html |
| `booking-service` | 8084 | BOOK-01..12, SCHED-01..05, PAT-01..03, ADM-03, ADM-04 | http://localhost:8084/swagger-ui.html |
| `medical-service` | 8085 | EXAM-01..05, DOC-02 | http://localhost:8085/swagger-ui.html |
| `notification-service` | 8086 | NOTI-01: thông báo trong ứng dụng (chuông) | http://localhost:8086/swagger-ui.html |
| `chatbot-service` | 8087 | UC-CHAT-01 (ngoài MVP) | http://localhost:8087/swagger-ui.html |
| `report-service` | 8088 | DASH-01..04, ADM-05 | http://localhost:8088/swagger-ui.html |
| `common` | – | Thư viện dùng chung (không chạy riêng) | – |

Cổng 8083 bỏ trống (lịch làm việc đã gộp vào `booking-service`). phpMyAdmin: http://localhost:8090.

Qua gateway, mọi API có dạng `http://localhost:8080/api/<service>/...`, ví dụ
`http://localhost:8080/api/catalog/chuyen-khoa`. Ngoại lệ: API xác thực là `/api/auth/...`, API tài khoản là `/api/users/...`
(cả hai do `identity-service` xử lý).

## Cần cài

- **JDK 17** (IntelliJ tự tải được: File → Project Structure → SDK → Download JDK, chọn bản 17).
- **IntelliJ IDEA** (bản miễn phí là đủ).
- **Docker Desktop** (MySQL, Redis; và để chạy test).
- **Node.js** (không bắt buộc): chỉ cần cho trang demo `scripts/demo/server.js` (thay frontend khi dev).

## Cách 1 — Chạy service trong IntelliJ (khi đang code)

1. Copy `.env.example` thành `.env` (giữ nguyên giá trị mặc định là chạy được).
2. Bật MySQL + Redis + phpMyAdmin:
   ```bash
   docker compose up -d
   ```
3. IntelliJ: **File → Open** → chọn file `pom.xml` ở thư mục gốc → **Open as Project**. Chờ Maven tải xong thư viện.
4. Trên thanh Run chọn **`Tất cả service + gateway`** rồi bấm Run (hoặc chỉ chạy service mình đang làm,
   ví dụ `catalog-service`). Các cấu hình chạy nằm sẵn trong thư mục `.run/`.
5. Mở Swagger của service, ví dụ http://localhost:8082/swagger-ui.html.

Dừng hạ tầng: `docker compose down` (thêm `-v` để xoá luôn dữ liệu).

Service chạy trong IntelliJ không đọc file `.env`; nó dùng giá trị mặc định trong
`common/src/main/resources/application-common.yml`, vốn khớp với MySQL/Redis của `docker compose up -d`.
Nếu đổi cổng/mật khẩu trong `.env` thì phải đặt biến môi trường tương ứng (`DB_PORT`, `DB_PASSWORD`...) trong run config.

## Cách 2 — Chạy toàn bộ trong Docker (demo, test frontend)

```bash
docker compose --profile app up -d --build     # build 8 image + chạy tất cả
docker compose --profile app ps                 # xem trạng thái (chờ tất cả "healthy")
docker compose --profile app down               # dừng; phải có --profile app
```

- Lần build đầu tải toàn bộ thư viện Maven (vài phút, mạng chậm có thể tới ~10 phút); các lần sau nhanh hơn.
- **Không** chạy cùng lúc với Cách 1: cả hai dùng cổng 8080–8088.
- Mỗi service giới hạn 512 MB RAM. Xem log một service: `docker compose --profile app logs -f catalog-service`.

## Tài khoản và token

- Lần đầu `identity-service` khởi động (chưa có quản trị viên nào), nó tạo tài khoản
  **`admin@eclinic.local` / `Admin@123`** (đổi bằng `ADMIN_EMAIL`, `ADMIN_PASSWORD` trong `.env`).
- **Dữ liệu mẫu** (`SEED_DATA=true`, bật sẵn ở docker compose và run config IntelliJ của identity-service): khi DB chưa có
  bác sĩ nào, identity-service tạo 8 chuyên khoa + phòng khám, 12 bác sĩ, 6 bệnh nhân và ca làm việc + khung giờ trống cho
  14 ngày tới; mỗi ngày (và mỗi lần khởi động) tự bổ sung ca cho đủ 14 ngày. Đã có bác sĩ thì không tạo lại, không ghi đè.
  Mọi tài khoản mẫu dùng mật khẩu **`Demo@123`** (đổi bằng `SEED_PASSWORD`). Bác sĩ do quản trị viên thêm sau này dùng
  mật khẩu mặc định `Doctor@123` (`DOCTOR_DEFAULT_PASSWORD`) và phải đặt mật khẩu mới ở lần đăng nhập đầu, xem
  [Quản lý bác sĩ](#quản-lý-bác-sĩ):

  | Email | Vai trò | Ghi chú |
  |---|---|---|
  | `bacsi01@eclinic.local` … `bacsi12@eclinic.local` | `BAC_SI` | có hồ sơ bác sĩ, chuyên khoa, lịch làm việc |
  | `benhnhan01@eclinic.local` … `benhnhan04@eclinic.local` | `BENH_NHAN` | đã kích hoạt, có hồ sơ bệnh nhân đã liên kết |
  | `benhnhan05@eclinic.local` | `BENH_NHAN` | chưa xác thực email (đăng nhập -> 403) |
  | `benhnhan06@eclinic.local` | `BENH_NHAN` | bị vô hiệu hoá, có lý do (đăng nhập -> 403) |

  Tắt bằng `SEED_DATA=false` (bắt buộc khi triển khai thật). Bác sĩ mẫu được nhận ra theo email `bacsiNN@eclinic.local`,
  nên bác sĩ mẫu đã đổi email sẽ không được bổ sung ca mới.
- **Lấy token để thử API** cần đăng nhập trên Swagger: đăng nhập bằng `POST /api/auth/login`
  (Swagger của identity-service, hoặc qua gateway `http://localhost:8080/api/auth/login`):
  ```json
  {"email": "admin@eclinic.local", "matKhau": "Admin@123"}
  ```
  Copy `duLieu.accessToken` rồi dán vào nút **Authorize** của Swagger (không cần gõ `Bearer `).
  Access token hết hạn sau 30 phút; lấy token mới bằng `POST /api/auth/refresh-token` hoặc đăng nhập lại.
  Refresh token **không có trong body**: nó nằm trong cookie HttpOnly `eclinic_rt` (trình duyệt tự gửi, nên trên
  Swagger chỉ cần gọi `refresh-token` với body trống). Với curl: `-c cookie.txt` khi đăng nhập, `-b cookie.txt` khi làm mới.
  Bệnh nhân: đăng ký `POST /api/auth/register`, lấy liên kết kích hoạt trong log identity-service
  (chế độ `MAIL_MODE=console`), gọi `POST /api/auth/verify-email`, rồi đăng nhập. Hoặc dùng trang demo bên dưới.
- **Token bác sĩ / bệnh nhân**: đăng nhập bằng tài khoản mẫu, ví dụ `bacsi01@eclinic.local` / `Demo@123`
  (bác sĩ mới do quản trị viên thêm: xem [Quản lý bác sĩ](#quản-lý-bác-sĩ)). Token này dùng được trên Swagger của mọi service.
- Để trống `JWT_SECRET` trong `.env` (mặc định đang comment): khi đó IntelliJ và Docker dùng chung một khoá.
- Rà soát bảo mật, rủi ro đang chấp nhận, việc phải làm trước khi triển khai thật và hướng dẫn cho frontend:
  [docs/BAO-MAT-XAC-THUC.md](docs/BAO-MAT-XAC-THUC.md).

## Tài khoản của tôi và quản trị tài khoản

Các API dưới đây đều cần access token và gọi qua gateway `http://localhost:8080`. Body, kết quả và mã lỗi của từng API:
[docs/BAO-MAT-XAC-THUC.md](docs/BAO-MAT-XAC-THUC.md) mục 1; thử nhanh trên Swagger của identity-service.

| Nhóm | API | Ghi chú |
|---|---|---|
| Hồ sơ cá nhân | `GET /api/users/me`, `PUT /api/users/me` | Sửa họ tên, số điện thoại; bác sĩ chỉ sửa được số điện thoại. Bác sĩ thấy hồ sơ bác sĩ, bệnh nhân thấy hồ sơ bệnh nhân đã liên kết (chỉ xem) |
| Đổi mật khẩu | `PUT /api/users/me/change-password` | Phải nhập mật khẩu hiện tại; các thiết bị khác bị đăng xuất |
| Thiết bị đăng nhập | `GET /api/users/me/sessions`, `DELETE /api/users/me/sessions/{id}`, `DELETE /api/users/me/sessions` | Xem, đăng xuất 1 thiết bị khác, đăng xuất mọi thiết bị khác |
| Ảnh đại diện | `POST /api/users/me/avatar`, `DELETE /api/users/me/avatar` | `multipart/form-data`, phần tệp `anh`: JPEG / PNG / WebP, tối đa 2 MB, 10 lần / giờ |
| Đổi email | `POST`, `GET`, `DELETE /api/users/me/change-email`; xác nhận bằng `POST /api/auth/confirm-email-change` | Liên kết xác nhận gửi tới email mới (hạn 1 giờ); xác nhận xong mọi thiết bị bị đăng xuất |
| Quản trị tài khoản | `GET /api/users`, `GET /api/users/{id}`, `PUT /api/users/{id}/status`, `DELETE /api/users/{id}` | Chỉ `QUAN_TRI_VIEN`: tìm kiếm, xem, vô hiệu hoá (kèm lý do) / kích hoạt lại, xoá tài khoản chưa có dữ liệu |

Giới hạn dung lượng: ảnh tối đa 2 MB (identity-service); mỗi request qua gateway tối đa 5 MB (`GATEWAY_MAX_REQUEST_SIZE`).

### Ảnh đại diện và ảnh giới thiệu bác sĩ (Cloudinary)

Ảnh lưu ở [Cloudinary](https://cloudinary.com/users/register_free), gói Free, không cần thẻ:
1. Đăng ký tài khoản, vào Console > Settings > API Keys: lấy **Cloud name** (đầu trang), **API Key**, **API Secret**.
2. Trong `.env`: `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` (API Secret là bí mật, chỉ để trong `.env`).
   Chạy bằng IntelliJ thì đặt 3 biến này trong run config của identity-service **và catalog-service**.
3. Khởi động lại identity-service và catalog-service.

Mã nối kho ảnh nằm ở `common` (`common/luutru`), cấu hình kết nối ở `application-common.yml`. Chỉ service đặt
`app.cloudinary.bat: true` trong `application.yml` của mình mới tạo kho ảnh: identity-service (ảnh đại diện) và
catalog-service (ảnh giới thiệu bác sĩ, lưu tại `<CLOUDINARY_FOLDER>/bac-si/<id bác sĩ>/<mã ngẫu nhiên>`).

- Thiếu 1 trong 3 giá trị: `POST /api/users/me/avatar` trả 503 `LUU_TRU_ANH_KHONG_KHA_DUNG`, log khởi động có cảnh báo WARN;
  bỏ ảnh (`DELETE`) và mọi chức năng khác vẫn chạy.
- Mỗi tài khoản 1 ảnh, lưu tại `<CLOUDINARY_FOLDER>/avatar/<id tài khoản>` (mặc định thư mục `eclinic`). Nhiều người dùng chung
  1 tài khoản Cloudinary với DB riêng thì mỗi người đặt `CLOUDINARY_FOLDER` khác nhau, nếu không sẽ ghi đè ảnh của nhau.
- Tài khoản tạo bằng Google lấy ảnh Google làm ảnh đầu tiên (giữ nguyên URL của Google, không chép vào Cloudinary).

## Email, đăng nhập Google và trang demo

**Gửi email thật qua Gmail** (mặc định `MAIL_MODE=console`: không gửi, liên kết chỉ ghi ra log identity-service):
1. Tài khoản Google gửi mail phải bật **Xác minh 2 bước**, rồi tạo **App Password** ở
   https://myaccount.google.com/apppasswords (16 ký tự, không phải mật khẩu Gmail).
2. Trong `.env`: `MAIL_MODE=smtp`, `MAIL_USERNAME=ten-ban@gmail.com`, `MAIL_APP_PASSWORD=<16 ký tự, bỏ dấu cách>`.
   Chạy bằng IntelliJ thì đặt 3 biến này trong run config của identity-service.
3. Khởi động lại identity-service. Thiếu tài khoản khi `MAIL_MODE=smtp` thì service không khởi động.

**Đăng nhập Google:**
1. [Google Cloud Console](https://console.cloud.google.com/apis/credentials) > Create credentials > OAuth client ID >
   loại **Web application**; *Authorized JavaScript origins* thêm `http://localhost:5173` (chính xác, không có `/` cuối).
   Lần đầu có thể phải cấu hình OAuth consent screen (External, thêm email của mình vào Test users).
2. Trong `.env`: `GOOGLE_CLIENT_ID=<client id>.apps.googleusercontent.com`, rồi khởi động lại identity-service.
   Chưa đặt thì `POST /api/auth/google` trả 503.

**Trang demo** (thay frontend khi dev). Một server, đi theo đúng danh sách màn hình của frontend: cùng đường dẫn, cùng
thanh bên của bác sĩ và quản trị viên; giao diện cố ý để đơn giản.
```bash
node scripts/demo/server.js      # rồi mở http://localhost:5173 (Chrome/Edge/Firefox)
```
Cần gateway và các service đang chạy (màn hình xác thực chỉ cần identity-service). Trang đọc `GOOGLE_CLIENT_ID` từ biến
môi trường hoặc từ `.env`. Đăng nhập ở `/login` bằng tài khoản mẫu (mục "Tài khoản và token"): bệnh nhân về
`/appointment`, bác sĩ về `/doctor/dashboard`, quản trị viên về `/admin/dashboard`; mở màn hình của vai trò khác sẽ thấy
báo "Sai vai trò". Liên kết kích hoạt / đặt lại mật khẩu / xác nhận đổi email trong email và link phiếu khám trong mã QR mở
đúng trang này. Kết quả của mỗi lần gọi API hiện ở ô "Kết quả gọi API gần nhất" cuối trang.

| Đường dẫn | Vai trò | Màn hình UI | File trong `scripts/demo/man-hinh/` |
|---|---|---|---|
| `/login`, `/register`, `/forgot-password`, `/reset-password` | công khai | Authentication | `dang-nhap.js`, `dang-ky.js`, `quen-mat-khau.js`, `dat-lai-mat-khau.js` |
| `/verify-email`, `/confirm-email-change` | công khai | (liên kết trong email, ngoài UI) | `lien-ket-email.js` |
| `/appointment` | công khai | Patient: Appointment + Appointment Ticket | `dat-lich.js` |
| `/phieu-kham/<mã>` | công khai | Appointment Ticket (link trong mã QR) | `phieu-kham.js` |
| `/patient/appointments`, `/patient/profile` | bệnh nhân | (ngoài UI) lịch hẹn, kết quả khám, trang cá nhân | `bn-lich-hen.js`, `bn-ho-so.js` |
| `/account` | đã đăng nhập | menu tài khoản: Change password (+ hồ sơ, ảnh, đổi email, thiết bị) | `tai-khoan.js` |
| `/doctor/dashboard` | bác sĩ | Doctor: Dashboard | `bs-tong-quan.js` |
| `/doctor/work-schedule` | bác sĩ | Doctor: Work Schedule | `bs-lich-lam-viec.js` |
| `/doctor/appointment-requests` | bác sĩ | Doctor: Appointment Requests | `bs-yeu-cau.js` |
| `/doctor/consultation` | bác sĩ | Doctor: Consultation / EMR | `bs-kham-benh.js` |
| `/doctor/profile` | bác sĩ | Doctor: Doctor Profile | `bs-ho-so.js` |
| `/admin/dashboard` | quản trị viên | Admin: Dashboard | `qt-tong-quan.js` |
| `/admin/schedule-management` | quản trị viên | Admin: Schedule Management | `qt-lich-lam-viec.js` |
| `/admin/medical-catalog` | quản trị viên | Admin: Medical Catalog | `qt-danh-muc.js` |
| `/admin/doctors` | quản trị viên | Admin: Doctor Management | `qt-bac-si.js` |
| `/admin/patients` | quản trị viên | Admin: Patient Management | `qt-benh-nhan.js` |
| `/admin/accounts` | quản trị viên | (ngoài UI) quản trị tài khoản | `qt-tai-khoan.js` |

Mỗi file màn hình mở đầu bằng khối chú thích ghi màn hình UI, đường dẫn, vai trò và các API nó gọi; phần dùng chung (gọi
API, phiên đăng nhập, bộ định tuyến) nằm ở `scripts/demo/chung.js`. Đường dẫn cũ `/dat-lich` và `/lich-hen` chuyển hướng sang `/appointment` và
`/patient/appointments`.

### Thử toàn bộ luồng xác thực với email thật và Google thật

Làm khi đã có 4 biến sau trong `.env` (xem 2 mục trên):
```
MAIL_MODE=smtp
MAIL_USERNAME=ten-ban@gmail.com
MAIL_APP_PASSWORD=<16 ký tự, không có dấu cách>
GOOGLE_CLIENT_ID=<client id>.apps.googleusercontent.com
```
OAuth client Google: *Authorized JavaScript origins* gồm `http://localhost:5173` và `http://localhost`, *Authorized redirect
URIs* để trống; không cần client secret. Consent screen đang ở chế độ Testing thì thêm tài khoản Google sẽ dùng vào
*Test users*. Cấu hình mới có thể mất vài phút mới có hiệu lực.

1. Chạy hệ thống và trang demo:
   ```bash
   docker compose --profile app up -d --build        # chờ tất cả "healthy": docker compose --profile app ps
   node scripts/demo/server.js                       # để cửa sổ này chạy, mở http://localhost:5173
   ```
   - Máy đã có MySQL khác chiếm cổng 3306: đặt `DB_PORT=3307` trong `.env` (chỉ đổi cổng mở ra máy host).
   - Kiểm tra đúng chế độ: `docker compose --profile app logs identity-service | grep -E "CONSOLE|GOOGLE_CLIENT_ID"`
     KHÔNG được ra dòng nào (có dòng = vẫn ở chế độ console hoặc thiếu Client ID).
   - Dùng Chrome/Edge/Firefox (Safari không gửi cookie `Secure` qua http://localhost).
2. **Email:**
   - [ ] Đăng ký (email nhận được thư, mật khẩu ≥ 6 ký tự, số điện thoại 10 số bắt đầu bằng 0) -> nhận thư
     "[eClinic] Kích hoạt tài khoản" (xem cả thư rác).
   - [ ] Bấm liên kết trong thư -> trang báo "Kích hoạt thành công".
   - [ ] Mở `/login`: Đăng nhập -> 200 ở ô "Kết quả gọi API gần nhất"; tải lại trang (F5) vẫn còn đăng nhập (phiên lấy
     lại bằng cookie); menu tài khoản > Log out -> 200.
   - [ ] Quên mật khẩu -> nhận thư "[eClinic] Đặt lại mật khẩu" -> bấm liên kết -> nhập mật khẩu mới 2 lần -> 200.
   - [ ] Nhận thư "[eClinic] Mật khẩu đã được thay đổi".
   - [ ] Đăng nhập bằng mật khẩu mới -> 200; mật khẩu cũ -> 401.
3. **Google:**
   - [ ] Bấm nút Google, chọn tài khoản -> 200, hiện đúng tên.
     (Cùng Gmail với bước 2 thì được liên kết vào tài khoản đó, cùng id — đúng như thiết kế.)
   - [ ] F12 > Application > Cookies > `http://localhost:8080`: có `eclinic_rt` (HttpOnly, Secure).
   - [ ] Tải lại trang (F5) vẫn còn đăng nhập; Log out -> 200 và cookie `eclinic_rt` biến mất.
4. Xem nhật ký bảo mật (chỉ có id, không có mật khẩu / token):
   ```bash
   docker compose --profile app logs identity-service | grep -E "Đăng nhập|Đăng ký|Kích hoạt|Đặt lại|Google|Từ chối"
   ```
5. Xong thì dừng trang demo (Ctrl+C) và `docker compose --profile app down`. Đặt lại `MAIL_MODE=console` nếu không muốn
   gửi email thật khi dev.

Lỗi hay gặp: nút Google báo origin không hợp lệ -> kiểm tra *Authorized JavaScript origins* hoặc chờ vài phút;
identity-service không khởi động với `MAIL_MODE=smtp` -> thiếu `MAIL_USERNAME` / `MAIL_APP_PASSWORD`; không nhận được thư ->
xem `docker compose --profile app logs identity-service | grep "thất bại"` (sai App Password, chưa bật Xác minh 2 bước).

### Thử chức năng tài khoản với email thật và Cloudinary thật

Làm sau mục trên (đã có `MAIL_MODE=smtp`), thêm 3 biến `CLOUDINARY_*` trong `.env`, với tài khoản bệnh nhân đã đăng ký bằng
hộp thư thật. Trang demo mở ở http://localhost:5173, Swagger của identity-service ở http://localhost:8081/swagger-ui.html
(đăng nhập rồi dán `accessToken` vào **Authorize**). Các bước dưới ghi tên API; trên trang demo có ô tương ứng cho từng bước
(Đổi mật khẩu, Đổi email, Ảnh đại diện ở `/account`; quản trị tài khoản ở `/admin/accounts`), dùng cách nào cũng được.

1. **Đổi mật khẩu** (Swagger, `PUT /api/users/me/change-password`):
   - [ ] 200, nhận thư "[eClinic] Mật khẩu đã được thay đổi"; đăng nhập bằng mật khẩu mới -> 200.
2. **Đổi email** (trang demo `/account`, ô "Đổi email đăng nhập", cần một hộp thư thật thứ hai):
   - [ ] Email mới nhận thư "[eClinic] Xác nhận đổi email đăng nhập", email cũ nhận thư "[eClinic] Có yêu cầu đổi email đăng nhập".
   - [ ] Bấm liên kết trong thư -> trang demo -> bấm "Xác nhận đổi email" -> 200.
   - [ ] Email cũ nhận thư "[eClinic] Email đăng nhập đã được thay đổi"; đăng nhập bằng email mới -> 200, email cũ -> 401.
3. **Ảnh đại diện** (Swagger, `POST /api/users/me/avatar`, chọn 1 ảnh JPEG / PNG / WebP dưới 2 MB):
   - [ ] 200, `duLieu.anhDaiDien` là URL `https://res.cloudinary.com/...`; mở URL thấy ảnh (đã thu về tối đa 512 x 512).
   - [ ] `DELETE /api/users/me/avatar` -> 200; Cloudinary Console > Media Library không còn ảnh trong `eclinic/avatar`.
4. **Vô hiệu hoá / kích hoạt lại** (Swagger, đăng nhập `admin@eclinic.local`; lấy id tài khoản ở `GET /api/users?tuKhoa=<email>`):
   - [ ] `PUT /api/users/{id}/status` với `{"trangThai": "VO_HIEU_HOA", "lyDo": "Thử nghiệm"}` -> 200; chủ tài khoản nhận thư
     "[eClinic] Tài khoản của bạn đã bị vô hiệu hoá" có ghi lý do; đăng nhập -> 403.
   - [ ] `{"trangThai": "DA_KICH_HOAT"}` -> 200; nhận thư "[eClinic] Tài khoản của bạn đã được kích hoạt lại"; đăng nhập -> 200.

## Đặt lịch khám

UC-APPT-01 (BOOK-01..07, BOOK-11, BOOK-12): **khách không cần đăng nhập** hoặc **bệnh nhân đã đăng nhập** chọn chuyên khoa,
bác sĩ (hoặc "bác sĩ bất kỳ"), ngày, khung giờ 1 tiếng, điền thông tin người khám và nhận phiếu khám (số thứ tự, giờ khám dự
kiến, link, mã QR). Lịch mới ở trạng thái `CHO_XAC_NHAN`. Huỷ / đổi lịch và bác sĩ xác nhận chưa có (task khác); bác sĩ ghi kết quả khám thì lịch chuyển sang `DA_HOAN_THANH`.

- Mỗi ca làm việc có N lượt khám mỗi giờ, mỗi lượt t phút (dữ liệu mẫu: N = 6, t = 10). Khung 1 tiếng hết N lượt thì hiện
  "Hết chỗ"; người đặt được xếp vào lượt trống sớm nhất của khung. Khung cuối của ca có thể ngắn hơn 1 tiếng.
- Người khám **dưới 18 tuổi tính theo ngày khám** phải kèm thông tin người giám hộ (từ đủ 18 tuổi, CCCD khác người khám).
- Hồ sơ bệnh nhân tìm theo số CCCD, chưa có thì tạo mới. **Số CCCD là khoá nhận diện duy nhất:** số đã có hồ sơ thì lần
  đặt sau luôn dùng lại hồ sơ đó, họ tên / ngày sinh nhập khác hồ sơ không bị từ chối. Hồ sơ không bị sửa; lịch hẹn giữ
  bản sao thông tin đã nhập (phiếu khám và "lịch hẹn của tôi" hiện bản sao này, không hiện dữ liệu của hồ sơ) và được đánh
  dấu `canDoiChieu` khi có khác biệt, kể cả khác biệt ở người giám hộ, để phòng khám đối chiếu giấy tờ. Thông tin nhập
  khớp hồ sơ thì form chỉ điền thêm ngày sinh, giới tính, địa chỉ, số bảo hiểm y tế còn trống.
- Sửa thông tin đã lưu: chủ tài khoản sửa hồ sơ của mình (`PUT .../ho-so-benh-nhan/cua-toi`) và người thân đã lưu
  (`PUT .../nguoi-than/{id}`); hồ sơ chưa gắn tài khoản do quản trị viên sửa sau khi đối chiếu giấy tờ. Số CCCD không đổi được.
- **Người khám dưới 18 tuổi được bỏ trống CCCD**: hồ sơ nhận diện bằng họ tên + ngày sinh + CCCD người giám hộ; lần đặt sau
  có CCCD thì hồ sơ đó được điền số. Từ đủ 18 tuổi bắt buộc CCCD 12 số. Giới tính và lý do khám bắt buộc.
- **"Bác sĩ bất kỳ"**: gửi `idChuyenKhoa` thay cho `idLichLamViec`, server xếp bác sĩ còn nhiều chỗ nhất trong giờ đó.
- Mỗi lịch hẹn có **mã tra cứu ngắn** `ECL-<ngày khám>-<4 số>` in trên phiếu; bác sĩ và quản trị viên tra theo mã này. Mã
  ngắn không mở được phiếu khám công khai (link và QR vẫn dùng mã phiếu khám 43 ký tự).
- Khách đặt: chỉ xem lại bằng link phiếu khám. Bệnh nhân đăng nhập: lịch được lưu vào tài khoản ("Lịch hẹn của tôi"), đặt cho
  bản thân (`datChoBanThan: true`, dùng hồ sơ của tài khoản, chưa có thì tạo và gắn vào tài khoản) hoặc cho người thân.
  Người thân đã đặt hộ được ghi nhớ đúng như đã nhập (tối đa 10 người) để lần sau điền sẵn; gửi `luuNguoiThan: false` để
  không ghi nhớ.
- **Liên kết lịch đã đặt như khách (quy tắc #3):** đăng ký kèm `cccd` (không bắt buộc). Lần đầu bệnh nhân mở "lịch hẹn của
  tôi", "thông tin điền sẵn" hoặc "hồ sơ của tôi" sau khi đăng nhập, booking-service tìm hồ sơ có số CCCD đó: họ tên và số
  điện thoại khớp thì gắn ngay vào tài khoản (`DA_LIEN_KET`) và lịch cũ hiện trong tài khoản; không khớp thì hồ sơ
  `CHO_XAC_MINH` chờ quản trị viên duyệt / từ chối. Tài khoản chưa khai CCCD thì nhập ở form hồ sơ của tôi (khớp họ tên
  và ngày sinh hoặc số điện thoại). Đăng nhập không gọi booking-service.

Tất cả gọi qua gateway `http://localhost:8080`; thử nhanh trên Swagger của catalog-service và booking-service.

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/catalog/chuyen-khoa`, `GET /api/catalog/chuyen-khoa/{id}` | Công khai | Danh sách chuyên khoa (phân trang) |
| `GET /api/catalog/bac-si?idChuyenKhoa=&tuKhoa=&trang=&kichThuoc=`, `GET /api/catalog/bac-si/{id}` | Công khai | Bác sĩ đang công tác, tài khoản còn hoạt động; không có email, số điện thoại, số giấy phép. Danh sách: `chucVu`, `gioiThieuNgan`; chi tiết thêm `tieuSu`, `quaTrinhDaoTao[]`, `quaTrinhCongTac[]`, `linhVucKhamChua[]`, `anh[]` |
| `GET /api/catalog/quan-tri/bac-si/{id}`, `PUT .../{id}/ho-so`, `POST .../{id}/anh`, `PUT .../{id}/anh/{idAnh}`, `PUT .../{id}/anh/thu-tu`, `DELETE .../{id}/anh/{idAnh}` | `QUAN_TRI_VIEN` | Sửa hồ sơ giới thiệu của bác sĩ; tải lên (multipart `anh`, `loai`, `chuThich`), sửa, sắp xếp, xoá ảnh giới thiệu. Tối đa 12 ảnh / bác sĩ, 4 MB / ảnh |
| `GET /api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa=` | Công khai | Ngày sớm nhất còn chỗ của từng bác sĩ trong chuyên khoa (cho thẻ bác sĩ) |
| `GET /api/booking/khung-gio/nhieu-ngay?idBacSi=&tuNgay=&denNgay=` | Công khai | Các ca và khung giờ của 1 bác sĩ, tối đa 7 ngày (mặc định từ hôm nay) |
| `GET /api/booking/khung-gio/gop?idChuyenKhoa=&ngay=` | Công khai | Khung giờ gộp các bác sĩ của chuyên khoa (`tongSoCho`, `soChoConLai`, `soBacSi`), dùng cho "bác sĩ bất kỳ" |
| `GET /api/booking/khung-gio/ngay-con-cho?tuNgay=&denNgay=&idBacSi=&idChuyenKhoa=` | Công khai | Các ngày có ca đặt được và số chỗ còn lại |
| `GET /api/booking/khung-gio?ngay=&idBacSi=&idChuyenKhoa=` | Công khai | Các ca trong ngày, mỗi ca kèm bác sĩ, phòng và các khung 1 tiếng (`tongSoCho`, `soChoConLai`, `hetCho`) |
| `POST /api/booking/lich-hen` | Công khai; có token `BENH_NHAN` thì lưu vào tài khoản | Body: `idLichLamViec` **hoặc** `idChuyenKhoa` (đúng 1 trong 2), `gioBatDauKhung`, `benhNhan` {`hoTen`, `ngaySinh`, `gioiTinh`, `soDienThoai`, `cccd` (bỏ trống được khi dưới 18 tuổi), `email`?, `soBaoHiemYTe`?, `diaChi`?}, `nguoiGiamHo` {`hoTen`, `quanHe`, `soDienThoai`, `cccd`, `ngaySinh`?}, `lyDoKham`, `datChoBanThan`?, `luuNguoiThan`?. 201: `maPhieuKham`, `maTraCuu`, `linkPhieuKham`, `soThuTu`, `gioKhamDuKien`, `bacSi`, `bacSiDoPhongKhamXep`, `luuVaoTaiKhoan`... |
| `GET /api/booking/phieu-kham/{maPhieuKham}` | Công khai (ai có mã cũng xem được) | Phiếu khám; CCCD và số điện thoại đã che; trẻ dưới 18 tuổi có người giám hộ và `canNguoiGiamHoDiCung`. `lyDoHuy` = lý do bác sĩ từ chối khi `trangThai` là `BI_TU_CHOI` (null ở trạng thái khác); phiếu của lịch bị từ chối vẫn mở được |
| `GET /api/booking/phieu-kham/{maPhieuKham}/qr?kichThuoc=&taiVe=` | Công khai | Ảnh PNG mã QR của link phiếu khám (100–1000 px, mặc định 300; `taiVe=true` để tải về) |
| `GET /api/booking/lich-hen/cua-toi?loc=TAT_CA\|SAP_TOI\|LICH_SU&cuaAi=TAT_CA\|BAN_THAN\|NGUOI_KHAC&trang=&kichThuoc=` | `BENH_NHAN` | Lịch tài khoản đã đặt (cho bản thân hoặc người thân), lịch của hồ sơ bệnh nhân đã liên kết (kể cả lịch đặt như khách) và lịch của người khám có người giám hộ khai số CCCD của hồ sơ đó; kèm `maTraCuu`, link phiếu khám. `cuaAi`: `BAN_THAN` = người khám là chủ tài khoản (bất kể ai đặt), `NGUOI_KHAC` = còn lại. Mỗi dòng có `laBanThan`, `nguoiDat` (`TOI` / `KHACH` / `TAI_KHOAN_KHAC`) và `thongTinKhacHoSo` (lịch của tôi mà người đặt nhập họ tên / ngày sinh khác hồ sơ của tôi) |
| `GET /api/booking/lich-hen/cua-toi/lich?tuNgay=&denNgay=&cuaAi=` | `BENH_NHAN` | Lịch hẹn của tài khoản theo khoảng ngày cho màn hình lịch: mọi trạng thái, theo giờ khám, không phân trang, tối đa 42 ngày (lưới tháng 6 tuần); dòng như "lịch hẹn của tôi" |
| `GET /api/booking/bac-si/toi/lich-hen/lich?tuNgay=&denNgay=` | `BAC_SI` | Lịch hẹn của chính bác sĩ theo khoảng ngày cho màn hình lịch (tối đa 42 ngày), dòng như danh sách trong ngày |
| `GET /api/booking/lich-hen/cua-toi/{maPhieuKham}` | `BENH_NHAN` | Chi tiết 1 lịch hẹn tài khoản được xem: `lichHen` (như dòng danh sách), `ngaySinhBenhNhan`, `gioiTinhBenhNhan`, `soDienThoaiLienHe`, `emailLienHe` (2 trường liên hệ chỉ có khi chính tài khoản này đặt). `lichHen.lyDoHuy` = lý do bác sĩ từ chối (có ở cả dòng danh sách). Lịch không thuộc tài khoản -> 404 |
| `GET /api/booking/trang-ca-nhan/cua-toi` | `BENH_NHAN` | Trang cá nhân trong 1 lần gọi: `hoSo`, `emailTaiKhoan`, `nguoiThan[]`, `lichSapToi` {`cuaToi[]`, `cuaNguoiKhac[]`} (5 lịch gần nhất mỗi bên), `lanKhamGanDay[]` (5 lượt đã khám gần nhất kèm kết quả), `soLich` {`sapToiCuaToi`, `sapToiCuaNguoiKhac`, `lichSu`, `daKham`} |
| `POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/huy`, `POST .../cua-toi/{maPhieuKham}/doi-lich` | `BENH_NHAN` | Hủy `{ lyDo? }` (trả phiếu khám `DA_HUY`) hoặc đổi lịch `{ idLichLamViec | idChuyenKhoa, gioBatDauKhung }` (201, trả lịch mới `CHO_XAC_NHAN` với mã phiếu khám mới; lịch cũ thành `DA_HUY_DO_DOI_LICH`, thông tin người khám lấy từ lịch cũ). Chỉ tài khoản đã đặt lịch hoặc người khám (`laBanThan`); tài khoản chỉ là người giám hộ theo CCCD -> 403. Chỉ lịch `CHO_XAC_NHAN` / `DA_XAC_NHAN`, trước giờ khám ít nhất `BOOKING_CHANGE_LEAD` (2 giờ), đổi tối đa `BOOKING_MAX_RESCHEDULES` (2) lần cho 1 lần đặt. Lượt khám cũ được mở lại. Dòng "lịch hẹn của tôi" và phiếu khám có `duocHuyDoi`, `hanHuyDoi` |
| `POST /api/booking/phieu-kham/{maPhieuKham}/huy`, `POST .../phieu-kham/{maPhieuKham}/doi-lich` | Công khai (cần đúng SĐT) | Hủy / đổi lịch bằng link phiếu khám cho khách không có tài khoản: body như trên kèm `soDienThoai` = SĐT liên hệ đã nhập lúc đặt lịch (thiếu / sai dạng -> 400, không khớp -> 403 `SO_DIEN_THOAI_KHONG_KHOP`). Số lần gọi từ 1 địa chỉ IP bị giới hạn như đặt lịch |
| `GET /api/booking/lich-su-kham/cua-toi?cuaAi=TAT_CA\|BAN_THAN\|NGUOI_KHAC&trang=&kichThuoc=` | `BENH_NHAN` | Lịch sử khám: các lượt đã khám xong mà tài khoản được xem kết quả, mới nhất trước. Mỗi dòng: `lichHen` (như dòng "lịch hẹn của tôi") và `ketQua` {`chanDoan`, `ghiChu`, `ngayTaiKhamDeXuat`, `donThuoc[]`}. Được xem kết quả khi người khám là chủ tài khoản (bất kể ai đặt) hoặc chính tài khoản đã đặt lịch; lịch chỉ thấy vì là người giám hộ theo CCCD thì không có kết quả. `GET .../lich-hen/cua-toi/{maPhieuKham}` cũng trả `ketQua` theo cùng quy tắc |
| `GET /api/booking/ho-so-benh-nhan/cua-toi`, `PUT /api/booking/ho-so-benh-nhan/cua-toi` | `BENH_NHAN` | Xem, tạo, sửa hồ sơ bệnh nhân của tài khoản; số CCCD không đổi được sau khi đã có hồ sơ. Số CCCD đã có hồ sơ chưa gắn tài khoản: khớp thì liên kết, không khớp trả `trangThaiLienKet: CHO_XAC_MINH` |
| `GET /api/booking/thong-tin-dat-lich/cua-toi`, `PUT .../cua-toi/nguoi-than/{id}`, `DELETE .../cua-toi/nguoi-than/{id}` | `BENH_NHAN` | Điền sẵn form: `banThan`, `emailTaiKhoan`, `nguoiThan[]` đã lưu, `lanDatGanNhat`; sửa bản lưu của 1 người thân (các trường của `benhNhan` trừ `cccd`, kèm `nguoiGiamHo`?; không sửa hồ sơ bệnh nhân); bỏ 1 người thân đã lưu |
| `GET /api/booking/bac-si/toi/lich-lam-viec?tuNgay=&denNgay=`, `GET /api/booking/bac-si/toi/lich-hen?ngay=`, `GET /api/booking/bac-si/toi/lich-hen/tra-cuu?ma=` | `BAC_SI` | Ca làm việc của chính bác sĩ (tối đa 42 ngày, kèm số lượt đã đặt), bệnh nhân trong ngày, tra 1 lịch hẹn theo mã tra cứu, mã phiếu khám hoặc cả link phiếu khám (quét QR). Danh sách trong ngày: `ngay` bỏ trống = hôm nay; lọc `idLichLamViec`, `soThuTu`, `tuKhoa` (họ tên, không xét dấu / hoa thường) |
| `GET /api/booking/bac-si/toi/lich-hen/{id}/ho-so-kham`, `GET /api/booking/bac-si/toi/lich-hen/tra-cuu/ho-so-kham?ma=` | `BAC_SI` | Hồ sơ khám của bệnh nhân từ 1 lịch hẹn của chính bác sĩ (theo id, hoặc theo mã / link phiếu khám quét từ QR): `lichHen`, `hoSoBenhNhan` (số CCCD không che, `tienSuBenhLy`), `lanKhamTruoc[]` (các lần khám trước với mọi bác sĩ, kèm `ketQua` nếu đã có bệnh án). Lịch hẹn của bác sĩ khác -> 404 |
| `POST` / `PUT` / `GET /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an`, `GET /api/medical/bac-si/toi/thuoc?tuKhoa=` | `BAC_SI` | Khám bệnh (medical-service): ghi kết quả khám `{ chanDoan, ghiChu?, ngayTaiKhamDeXuat?, donThuoc[]? }` cho lịch hẹn của chính bác sĩ, từ ngày khám trở đi; lịch hẹn chuyển sang `DA_HOAN_THANH` trong cùng transaction. `PUT` sửa kết quả đã ghi (thay toàn bộ đơn thuốc), `GET` xem lại; gợi ý tên thuốc trong danh mục. Hồ sơ khám ở booking-service hiện `ketQua` và `lanKhamTruoc[].ketQua` kèm `donThuoc[]` |
| `GET /api/booking/bac-si/toi/lich-hen/yeu-cau?trang=&kichThuoc=`, `POST .../lich-hen/{id}/xac-nhan`, `POST .../lich-hen/{id}/tu-choi` | `BAC_SI` | Yêu cầu đặt lịch: lịch `CHO_XAC_NHAN` của chính bác sĩ mà lượt khám chưa bắt đầu (phân trang, giờ khám sớm nhất trước; `tongSoPhanTu` = số yêu cầu đang chờ). Xác nhận -> `DA_XAC_NHAN`; từ chối `{ lyDo }` (bắt buộc, tối đa 500 ký tự) -> `BI_TU_CHOI` và lượt khám được mở lại cho người khác đặt. Cả hai chỉ làm được trước giờ khám và trả lại dòng lịch hẹn. Lịch chưa kịp xác nhận vẫn khám được vào ngày khám |
| `PUT /api/booking/bac-si/toi/lich-hen/{id}/benh-nhan`, `POST .../lich-hen/{id}/da-doi-chieu` | `BAC_SI` | Sửa hồ sơ bệnh nhân của 1 lịch hẹn của mình sau khi đối chiếu giấy tờ (body như của quản trị viên); bỏ đánh dấu `canDoiChieu` |
| `GET /api/booking/quan-tri/lich-lam-viec?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=&idPhongKham=`, `GET .../lich-lam-viec/{id}/lich-hen`, `GET /api/booking/quan-tri/lich-hen/tra-cuu?ma=` | `QUAN_TRI_VIEN` | Lịch làm việc toàn viện (chỉ xem, tối đa 42 ngày), lịch hẹn của 1 ca, tra lịch hẹn theo mã. Mỗi lịch hẹn (cả ở danh sách của bác sĩ) có `idHoSoBenhNhan`, `benhNhan` (dữ liệu của hồ sơ) và `doiChieu` {`canDoiChieu`, `hoTenDaNhap`, `ngaySinhDaNhap`, `gioiTinhDaNhap`, `hoTenGiamHoDaNhap`} |
| `POST /api/booking/quan-tri/lich-hen/{id}/da-doi-chieu` | `QUAN_TRI_VIEN` | Đã đối chiếu giấy tờ: bỏ đánh dấu `canDoiChieu` của lịch hẹn |
| `GET /api/booking/quan-tri/ho-so-benh-nhan/{id}`, `PUT .../ho-so-benh-nhan/{id}` | `QUAN_TRI_VIEN` | Xem (số CCCD không che) và sửa hồ sơ bệnh nhân bất kỳ: `hoTen`, `ngaySinh`, `gioiTinh`?, `soDienThoai`, `diaChi`?, `soBaoHiemYTe`?, `cccd`? (chỉ điền được khi hồ sơ chưa có số; đổi số đã có -> 400, số thuộc hồ sơ khác -> 409) |
| `GET /api/booking/quan-tri/ho-so-benh-nhan/cho-xac-minh`, `POST .../ho-so-benh-nhan/{id}/duyet`, `POST .../{id}/tu-choi` | `QUAN_TRI_VIEN` | Hồ sơ bệnh nhân chờ xác minh liên kết với tài khoản: duyệt hoặc từ chối (cặp đã từ chối không tự vào hàng chờ lại) |

Mã lỗi (`maLoi`) khi đặt lịch:

| Mã HTTP | `maLoi` | Khi nào |
|---|---|---|
| 400 | `DU_LIEU_KHONG_HOP_LE` | Form sai quy tắc; `chiTiet` nêu từng trường (vd `benhNhan.gioiTinh`). Gửi cả hai hoặc không gửi `idLichLamViec` / `idChuyenKhoa`; người từ đủ 18 tuổi không có CCCD |
| 400 | `THIEU_NGUOI_GIAM_HO`, `NGUOI_GIAM_HO_KHONG_HOP_LE` | Người khám dưới 18 tuổi mà thiếu người giám hộ; người giám hộ chưa đủ 18 tuổi hoặc trùng CCCD với người khám |
| 409 | `KHUNG_GIO_KHONG_CON_TRONG` | Khung giờ đã hết chỗ |
| 409 | `KHUNG_GIO_KHONG_KHA_DUNG` | Khung giờ không còn đặt được (đã qua, quá gần giờ khám, quá xa, ca bị huỷ, bác sĩ ngừng công tác) |
| 409 | `LICH_HEN_TRUNG_GIO` | Người khám đã có lịch còn hiệu lực trong cùng khung giờ đó |
| 409 | `THONG_TIN_BENH_NHAN_KHONG_KHOP` | Đặt cho bản thân với CCCD khác hồ sơ của tài khoản (họ tên / ngày sinh khác hồ sơ của 1 số CCCD không còn bị từ chối) |
| 409 | `VUOT_GIOI_HAN_DAT_LICH` | Hồ sơ / số điện thoại đã có quá nhiều lịch sắp tới |
| 409 | `CCCD_DA_CO_HO_SO`, `HO_SO_CHO_XAC_MINH` | Đặt cho bản thân bằng số CCCD đã có hồ sơ chưa gắn với tài khoản; hồ sơ của tài khoản đang chờ xác minh; tạo hồ sơ bằng số CCCD thuộc tài khoản khác hoặc đã bị quản trị viên từ chối liên kết |
| 403 | `KHONG_CO_QUYEN`, `TAI_KHOAN_BI_VO_HIEU_HOA`, `TAI_KHOAN_CHUA_XAC_THUC` | Token bác sĩ / quản trị viên; tài khoản bị vô hiệu hoá hoặc chưa xác thực email |
| 429 | `GUI_LAI_QUA_NHANH` | Quá nhiều lần đặt từ một địa chỉ IP |
| 409 | `LICH_HEN_KHONG_CHO_XAC_NHAN`, `LICH_HEN_DA_QUA_GIO` | (Bác sĩ xác nhận / từ chối) Lịch hẹn đã được xác nhận, bị từ chối, đã khám hoặc đã huỷ (kể cả khi bấm lần thứ hai); lượt khám đã bắt đầu |
| 409 | `LICH_HEN_KHONG_HUY_DOI_DUOC`, `QUA_HAN_HUY_DOI_LICH`, `VUOT_SO_LAN_DOI_LICH` | (Bệnh nhân hủy / đổi lịch) Lịch hẹn đã khám, đã hủy, đã đổi hoặc bị từ chối; còn cách giờ khám ít hơn hạn hủy / đổi; lần đặt này đã đổi đủ số lần |
| 403 | `SO_DIEN_THOAI_KHONG_KHOP` | (Hủy / đổi bằng link phiếu khám) SĐT nhập vào không phải SĐT liên hệ của lượt khám |
| 409 | `CHUA_DEN_NGAY_KHAM`, `LICH_HEN_KHONG_KHAM_DUOC` | (Khám bệnh) Bác sĩ ghi kết quả khám trước ngày khám; lịch hẹn đã khám xong, đã huỷ hoặc bị từ chối |
| 404 | `KHONG_TIM_THAY` | Ca khám không tồn tại; mã phiếu khám sai |

Cấu hình (đều có giá trị mặc định, xem `.env.example`): `BOOKING_MIN_LEAD` (30m), `BOOKING_MAX_DAYS` (30),
`BOOKING_MAX_ACTIVE_PER_PATIENT` (3), `BOOKING_MAX_ACTIVE_PER_PHONE` (5), `BOOKING_IP_LIMIT` (20) trong `BOOKING_IP_WINDOW` (10m),
`GATEWAY_TRUSTED_PROXIES`; link phiếu khám dựng từ `FRONTEND_URL`. Rủi ro và việc cần làm trước khi triển khai thật:
[docs/BAO-MAT-XAC-THUC.md](docs/BAO-MAT-XAC-THUC.md) mục 7.

**Màn hình đặt lịch của trang demo** (cần gateway, identity-service, catalog-service, booking-service đang chạy và dữ liệu mẫu):
```bash
node scripts/demo/server.js      # rồi mở http://localhost:5173/appointment (Chrome/Edge/Firefox)
```
Cả luồng nằm trên một trang: chuyên khoa -> thẻ bác sĩ -> ngày -> giờ -> thông tin người khám -> xác nhận -> phiếu khám.
1. **Khách:** chọn chuyên khoa (vd Nhi khoa) -> hiện thẻ từng bác sĩ (ảnh, chức vụ, số năm kinh nghiệm, giới thiệu ngắn, ngày
   sớm nhất còn chỗ) và thẻ "Bác sĩ bất kỳ". **Xem chi tiết** mở hộp giới thiệu (quá trình đào tạo, công tác, lĩnh vực khám
   chữa, ảnh công việc và chứng chỉ; ảnh mẫu lấy từ placehold.co nên cần internet). **Chọn** 1 thẻ, bấm 1 ngày, 1 khung giờ,
   điền form, bấm **Xem lại và đặt lịch** -> hộp tóm tắt -> **Xác nhận đặt lịch** -> phiếu khám kèm mã tra cứu `ECL-...`
   và mã QR. "Mở trang phiếu khám" mở `/phieu-kham/<mã>` (cũng là link trong mã QR), CCCD và số điện thoại đã che.
2. **Bác sĩ bất kỳ:** giờ hiện là giờ gộp của cả chuyên khoa; đặt xong trang ghi bác sĩ được phòng khám xếp.
3. **Trẻ dưới 18 tuổi:** nhập ngày sinh của trẻ -> ô "Người giám hộ" hiện ra; ô CCCD của trẻ để trống được. Để trống người
   giám hộ rồi đặt -> 400 `THIEU_NGUOI_GIAM_HO`; điền đủ -> phiếu khám có người giám hộ và dòng "người giám hộ phải đi cùng".
4. **Bệnh nhân:** đăng nhập `benhnhan01@eclinic.local` / `Demo@123` ở `/login` (đăng nhập xong về `/appointment`)
   -> "Đặt cho bản thân" điền sẵn từ hồ sơ; đặt xong lịch hiện ở `/patient/appointments` (danh sách lọc Tất cả / Sắp tới / Lịch sử).
   "Đặt cho người thân": nhập người mới (mặc định được lưu) hoặc chọn **người thân đã lưu** để điền sẵn cả người khám lẫn
   người giám hộ. Lần mở trang sau, chuyên khoa và bác sĩ của lần đặt gần nhất được chọn sẵn.
5. **Liên kết theo CCCD:** đặt 1 lịch như khách; ở `/register` đăng ký tài khoản với cùng họ tên, số điện thoại và
   số CCCD đó, kích hoạt, đăng nhập -> lịch đã đặt như khách nằm trong `/patient/appointments`. Đăng ký với số điện thoại khác
   -> `/patient/profile` ghi "đang chờ phòng khám xác minh"; quản trị viên duyệt ở `/admin/patients`
   (`POST /api/booking/quan-tri/ho-so-benh-nhan/{id}/duyet`).
6. Khung giờ hết chỗ hiện "Hết chỗ" và không bấm được. Mọi lần gọi API hiện ở ô "Kết quả gọi API gần nhất" cuối trang.

**Màn hình lịch hẹn của trang demo** (cùng server; cần thêm medical-service đang chạy và **internet**: thư viện lịch
FullCalendar 6 và, ở trình duyệt chưa tự đọc được QR, thư viện jsQR được tải từ CDN jsdelivr). Bệnh nhân dùng
`/patient/appointments` và `/patient/profile`; bác sĩ dùng `/doctor/work-schedule` và `/doctor/consultation`:
1. **Bệnh nhân** (`benhnhan01@eclinic.local` / `Demo@123`): lịch hẹn dạng lịch (tháng / tuần / ngày / danh sách), màu khác nhau
   cho lịch của tôi, lịch của người khác, đã khám, đã hủy; lọc Tất cả / Của tôi / Của người khác. Bấm 1 lịch hẹn -> chi tiết, kết
   quả khám (nếu được xem), cảnh báo khi người đặt nhập thông tin khác hồ sơ của mình. Trang cá nhân: hồ sơ, số lịch hẹn, người
   thân đã lưu (sửa, bỏ), các lần khám gần đây kèm chẩn đoán và đơn thuốc.
2. **Bác sĩ** (`bacsi01@eclinic.local` / `Demo@123`): lịch gồm ca làm việc (số lượt đã đặt / tổng) và lịch hẹn. Bấm 1 ngày hoặc 1
   ca -> danh sách bệnh nhân trong ngày, lọc theo số thứ tự, họ tên (không cần dấu), ca. Bấm 1 lịch hẹn, gõ mã, hoặc **quét mã QR**
   trên phiếu khám (camera hoặc chọn ảnh) -> hồ sơ khám: hồ sơ đang lưu, thông tin người đặt đã nhập, các lần khám trước. Tại đó
   sửa hồ sơ bệnh nhân, bấm "Đã đối chiếu giấy tờ", ghi / sửa kết quả khám kèm đơn thuốc (gõ tên thuốc có gợi ý).
3. Ghi kết quả khám chỉ được từ **ngày khám** trở đi: muốn thử ngay thì đặt 1 lịch cho ca **hôm nay** (đặt trước ít nhất 30 phút)
   rồi đăng nhập bằng bác sĩ của ca đó; lịch của ngày sau trả 409 `CHUA_DEN_NGAY_KHAM`.

**Danh sách kiểm tra trên màn hình cho các màn hình lịch hẹn** (DOANTOTNGH-6, đường dẫn theo trang demo mới; phần API đã kiểm tra tự động, phần dưới đây phải
nhìn trên trình duyệt). Chuẩn bị: chạy gateway, identity-service, booking-service, medical-service với `SEED_DATA=true`, chạy
`node scripts/demo/server.js`, máy có internet. Đặt sẵn ở `/appointment` bằng `benhnhan01@eclinic.local`: 1 lịch cho bản thân,
1 lịch cho người thân, và 1 lịch cho ca **hôm nay** (để bác sĩ ghi kết quả khám); ghi lại bác sĩ của các ca đó.

- [ ] **1. Lịch của bệnh nhân.** Đăng nhập `benhnhan01`, mở `/patient/appointments`. Lịch hẹn hiện ở cả 4 chế độ Tháng / Tuần / Ngày / Lịch
      biểu; lịch của tôi và của người khác khác màu, lịch đã khám màu xanh lá. Bấm 1 lịch hẹn -> hiện chi tiết; lịch đã khám hiện
      kết quả khám. Đổi bộ lọc Tất cả / Lịch của tôi / Lịch của người khác -> lịch đổi theo.
- [ ] **2. Trang cá nhân của bệnh nhân.** Mở `/patient/profile`. Hiện hồ sơ bệnh nhân, 4 số đếm lịch hẹn, người thân đã lưu, các lần khám gần đây. Bấm
      **Sửa** 1 người thân, đổi họ tên, **Lưu** -> bảng hiện tên mới. Bấm **Bỏ** -> người đó biến mất khỏi bảng.
- [ ] **3. Lịch và danh sách của bác sĩ.** Đăng xuất, đăng nhập bằng bác sĩ của ca đã đặt (`bacsiNN@eclinic.local` / `Demo@123`).
      Mở `/doctor/work-schedule`: lịch hiện ca làm việc (kèm số lượt đã đặt / tổng) và lịch hẹn. Bấm 1 ngày hoặc 1 ca -> sang `/doctor/consultation` với danh sách bệnh nhân của ngày đó. Lọc
      theo **số thứ tự**, theo **họ tên gõ không dấu** -> chỉ còn dòng khớp. Bấm 1 dòng -> mở hồ sơ khám, có mục "Các lần khám trước".
- [ ] **4. Thao tác của bác sĩ trong hồ sơ khám.** Sửa họ tên trong "Sửa hồ sơ bệnh nhân", **Lưu** -> hồ sơ hiện tên mới. Với lịch
      có cảnh báo "khác hồ sơ" (đặt bằng số CCCD đã có nhưng gõ tên khác): bấm **Đã đối chiếu giấy tờ** -> cảnh báo mất. Với lịch
      của **hôm nay**: nhập chẩn đoán, thêm 1 dòng thuốc (gõ 2 ký tự trở lên của tên thuốc đã từng kê -> có gợi ý), **Lưu kết quả
      khám** -> lịch hẹn thành "Đã hoàn thành", form chuyển sang chế độ sửa. Lịch của ngày sau -> báo `CHUA_DEN_NGAY_KHAM`.
- [ ] **5. Mã QR.** Mở phiếu khám của 1 lịch hẹn thuộc bác sĩ đang đăng nhập (link "Mở phiếu khám" ở chi tiết lịch hẹn của bệnh
      nhân) để có mã QR. Ở `/doctor/consultation`: **Quét QR bằng camera** rồi đưa mã vào -> mở đúng hồ sơ khám; chụp / lưu ảnh mã QR rồi
      **chọn ảnh** -> mở đúng hồ sơ khám; dán mã `ECL-…` hoặc cả link phiếu khám vào ô mã -> mở đúng hồ sơ khám. Mã của bác sĩ
      khác -> báo không tìm thấy.

Đối chiếu từng màn hình của frontend với API: [docs/BAN-GIAO-FRONTEND-05-10.md](docs/BAN-GIAO-FRONTEND-05-10.md).

## Ca làm việc và yêu cầu đổi ca / xin nghỉ

| API | Ai gọi | Ghi chú |
|---|---|---|
| `POST /api/booking/quan-tri/lich-lam-viec` | `QUAN_TRI_VIEN` | Xếp ca: `{ idBacSi, idPhongKham, ngay, gioBatDau, gioKetThuc, soLuotToiDaMoiGio (N), thoiLuongLuotPhut (t), lapLai?: { cacThu: [1..7], denNgay } }` (`cacThu`: 1 = thứ Hai ... 7 = Chủ nhật). 201 `{ daTao: [ca], boQua: [{ ngay, lyDo }] }`. Các lượt khám được sinh theo N, t (`N x t <= 60`). Phòng phải đang hoạt động và thuộc chuyên khoa của bác sĩ; ca phải bắt đầu sau hiện tại và trong `SHIFT_MAX_DAYS` (90) ngày tới. Trùng giờ với ca khác của bác sĩ hoặc của phòng → 409 `TRUNG_LICH_LAM_VIEC`; có `lapLai` thì ngày trùng bị bỏ qua và nằm trong `boQua` |
| `PUT /api/booking/quan-tri/lich-lam-viec/{id}` | `QUAN_TRI_VIEN` | Sửa `{ idPhongKham, gioBatDau, gioKetThuc, soLuotToiDaMoiGio, thoiLuongLuotPhut }` (gửi đủ 5 giá trị; bác sĩ và ngày không sửa được). Phòng đổi được bất cứ lúc nào: lịch hẹn còn hiệu lực mang phòng mới, bệnh nhân được báo. Giờ / sức chứa chỉ đổi được khi mọi lượt đã có người đặt còn nguyên giờ; không thì 409 `CA_CON_LICH_HEN` và không có gì thay đổi. Ca đã bắt đầu hoặc đã hủy → 409 `CA_KHONG_SUA_DUOC` |
| `POST /api/booking/quan-tri/lich-lam-viec/{id}/huy` | `QUAN_TRI_VIEN` | `{ lyDo }` bắt buộc. Trả `{ ca, soLichHenCanDoi }`. Ca và mọi lượt khám thành `DA_HUY`; lịch hẹn còn hiệu lực của ca **giữ nguyên trạng thái** và có `canDoiLich = true`; bệnh nhân và bác sĩ được báo. Yêu cầu đang chờ duyệt của ca bị đóng (`TU_CHOI` kèm ghi chú) |
| `GET /api/booking/quan-tri/lich-lam-viec/can-doi-lich?trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Lịch hẹn còn hiệu lực đang chờ bệnh nhân đổi lịch, giờ khám cũ sớm nhất trước (để phòng khám gọi điện cho khách đặt không đăng nhập) |
| `POST /api/booking/bac-si/toi/yeu-cau-doi-lich` | `BAC_SI` | `{ idLichLamViec, loaiYeuCau: XIN_NGHI \| DOI_CA, lyDo, ngayMongMuon?, gioBatDauMongMuon?, gioKetThucMongMuon?, idPhongKhamMongMuon? }` → 201. 3 trường ngày / giờ bắt buộc với `DOI_CA`. Chỉ ca còn hoạt động của chính bác sĩ (ca của người khác → 404), còn cách giờ bắt đầu ít nhất `SHIFT_REQUEST_LEAD` (24 giờ) → không thì 409 `QUA_HAN_GUI_YEU_CAU`. Mỗi ca 1 yêu cầu chờ duyệt → 409 `CA_DA_CO_YEU_CAU_CHO_DUYET` |
| `GET /api/booking/bac-si/toi/yeu-cau-doi-lich?trangThai=&trang=&kichThuoc=`, `POST .../yeu-cau-doi-lich/{id}/rut` | `BAC_SI` | Yêu cầu tôi đã gửi (mới nhất trước) và rút yêu cầu còn chờ duyệt (`DA_RUT`; sau đó ca nhận được yêu cầu mới). Mỗi dòng: `{ id, loaiYeuCau, trangThai, lyDo, ca, ngayMongMuon, gioBatDauMongMuon, gioKetThucMongMuon, phongKhamMongMuon, ghiChuXuLy, ngayGui, ngayXuLy }`; `ca` như 1 dòng lịch làm việc (có `soLuotDaDat`) |
| `GET /api/booking/quan-tri/yeu-cau-doi-lich?trangThai=&tatCa=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Yêu cầu của mọi bác sĩ, gửi sớm nhất trước; mặc định `CHO_DUYET`, `tatCa=true` lấy mọi trạng thái |
| `POST /api/booking/quan-tri/yeu-cau-doi-lich/{id}/duyet`, `POST .../{id}/tu-choi` | `QUAN_TRI_VIEN` | Body `{ ghiChu }`, bắt buộc khi từ chối. Duyệt `XIN_NGHI` = hủy ca. Duyệt `DOI_CA`: chỉ khác phòng thì đổi phòng tại chỗ; còn lại thì hủy ca cũ và xếp ca mới theo ngày / giờ / phòng mong muốn (giữ N, t). Ca mới trùng giờ → 409 `TRUNG_LICH_LAM_VIEC`, yêu cầu vẫn chờ duyệt. Đã xử lý / đã rút → 409 `YEU_CAU_DA_XU_LY` |
| `GET /api/catalog/phong-kham?idChuyenKhoa=` | `QUAN_TRI_VIEN`, `BAC_SI` | Phòng khám đang hoạt động: `{ id, tenPhong, tang, idChuyenKhoa, tenChuyenKhoa }`. Chỉ đọc |

**Lịch hẹn của ca bị hủy (`canDoiLich`).** Phiếu khám, dòng "lịch hẹn của tôi" và dòng lịch hẹn của bác sĩ / quản trị viên có
`canDoiLich`. Khi `true`: lịch hẹn vẫn `CHO_XAC_NHAN` / `DA_XAC_NHAN` nhưng ca đã hủy, bệnh nhân phải đổi sang khung giờ khác
hoặc hủy. Với lịch này `duocHuyDoi` đúng tới giờ khám cũ (`hanHuyDoi` = giờ khám, không trừ 2 giờ) và lần đổi không tính vào
giới hạn 2 lần; lịch mới sau khi đổi không còn cờ. Bác sĩ không thấy lịch này trong Appointment Requests và không xác nhận /
từ chối được (409 `LICH_HEN_CAN_DOI_LICH`).

**Số thứ tự khám** là thứ hạng của lượt khám trong các lượt của phòng trong ngày, nên khi phòng có thêm ca đứng trước, ca được
kéo dài hoặc ca đổi phòng thì số thứ tự của các lịch hẹn còn hiệu lực của phòng trong ngày đó được tính lại. Luôn hiển thị
`soThuTu` mới nhất từ API, không lưu lại ở frontend.

Lỗi mới (đều 409): `CA_KHONG_SUA_DUOC`, `CA_CON_LICH_HEN`, `CA_DA_CO_YEU_CAU_CHO_DUYET`, `QUA_HAN_GUI_YEU_CAU`,
`YEU_CAU_DA_XU_LY`, `LICH_HEN_CAN_DOI_LICH`. Cấu hình: `SHIFT_REQUEST_LEAD` (24h), `SHIFT_MAX_DAYS` (90).

## Quản lý bác sĩ

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/catalog/quan-tri/bac-si?tuKhoa=&idChuyenKhoa=&trangThai=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Danh bạ bác sĩ, bác sĩ mới thêm đứng trước, **cả bác sĩ ngừng công tác**. `tuKhoa` so với họ tên, email, số điện thoại, số giấy phép. Mỗi dòng: `id`, `maBacSi`, `hoTen`, `email`, `soDienThoai`, `anhDaiDien`, `hocVi`, `soGiayPhep`, `idChuyenKhoa`, `tenChuyenKhoa`, `soLuotDaKham`, `trangThai` (`DANG_CONG_TAC` / `NGUNG_CONG_TAC`), `trangThaiTaiKhoan`, `phaiDoiMatKhau` |
| `POST /api/catalog/quan-tri/bac-si` | `QUAN_TRI_VIEN` | "Add New Doctor": `{ hoTen, email, soDienThoai?, idChuyenKhoa, soGiayPhep?, hocVi?, chucVu?, soNamKinhNghiem? }` → 201, trả hồ sơ quản trị của bác sĩ. Tạo tài khoản đăng nhập và hồ sơ bác sĩ trong 1 bước |
| `PUT /api/catalog/quan-tri/bac-si/{id}` | `QUAN_TRI_VIEN` | Thông tin cơ bản, ghi đè cả 4 trường: `{ hoTen, soDienThoai?, idChuyenKhoa, soGiayPhep? }`. Email không sửa ở đây. Hồ sơ giới thiệu vẫn sửa bằng `PUT .../{id}/ho-so` |
| `GET /api/catalog/quan-tri/bac-si/{id}/anh-huong-ngung-cong-tac` | `QUAN_TRI_VIEN` | `{ soCaSapToi, soLichHenBiAnhHuong }`: hiện cho quản trị viên trước khi xác nhận |
| `POST /api/catalog/quan-tri/bac-si/{id}/ngung-cong-tac` | `QUAN_TRI_VIEN` | `{ lyDo }` (bắt buộc) → `{ bacSi, soCaDaHuy, soLichHenCanDoi }` |
| `POST /api/catalog/quan-tri/bac-si/{id}/cong-tac-lai` | `QUAN_TRI_VIEN` | Bác sĩ về `DANG_CONG_TAC`, tài khoản về `DA_KICH_HOAT`. Các ca đã hủy không được khôi phục |
| `POST /api/auth/first-password` | Công khai | `{ email, matKhauHienTai, matKhauMoi }`: đặt mật khẩu ở lần đăng nhập đầu của tài khoản bác sĩ mới |

- **Mã bác sĩ** (`maBacSi`, ví dụ `BS0007`) suy ra từ `id`, chỉ để hiển thị; mọi API vẫn dùng `id`.
- **Thêm bác sĩ.** Tài khoản được tạo ở trạng thái đã kích hoạt, mật khẩu là mật khẩu mặc định của phòng khám
  (`DOCTOR_DEFAULT_PASSWORD`, mặc định `Doctor@123`), `phaiDoiMatKhau = true`. Bác sĩ nhận email báo email đăng nhập
  (email không chứa mật khẩu). Email / số điện thoại đã có: 409 `EMAIL_DA_TON_TAI` / `SO_DIEN_THOAI_DA_TON_TAI`; số giấy
  phép đã có: 409 `SO_GIAY_PHEP_DA_TON_TAI`.
- **Lần đăng nhập đầu.** `POST /api/auth/login` với mật khẩu mặc định trả **403 `PHAI_DOI_MAT_KHAU`** và không cấp phiên.
  Frontend hiện form "đặt mật khẩu mới", gọi `POST /api/auth/first-password` với email + mật khẩu vừa nhập + mật khẩu mới
  (tối thiểu 6 ký tự, khác mật khẩu hiện tại), rồi gọi lại `login` bằng mật khẩu mới. Sai mật khẩu hiện tại: 401, đếm chung
  với đăng nhập sai (5 lần thì khoá 15 phút). Tài khoản không cần đặt mật khẩu lần đầu: 409. "Quên mật khẩu" qua email cũng
  gỡ được trạng thái này.
- **Ngừng công tác** chạy 3 bước: (1) bác sĩ thành `NGUNG_CONG_TAC`: ẩn khỏi danh sách công khai, không nhận ca và lịch hẹn
  mới; (2) mọi ca chưa bắt đầu bị hủy như khi quản trị viên hủy từng ca: lịch hẹn được giữ, đánh dấu `canDoiLich`, bệnh
  nhân nhận thông báo `LICH_HEN_CAN_DOI`, lịch hẹn vào danh sách `GET /api/booking/quan-tri/lich-lam-viec/can-doi-lich`;
  (3) tài khoản bị vô hiệu hoá, mọi phiên bị đăng xuất, bác sĩ nhận email kèm lý do. Ca đang diễn ra được giữ nguyên.
  Nếu trả **503 `DICH_VU_NOI_BO_LOI`** thì bác sĩ đã ở `NGUNG_CONG_TAC` nhưng chưa xong: gửi lại đúng request đó để chạy
  nốt (gọi lại nhiều lần không sao).
- **Đổi chuyên khoa** khi bác sĩ còn ca sắp tới: 409 `BAC_SI_CON_CA_LAM_VIEC` (phòng khám của ca thuộc chuyên khoa cũ); hủy
  các ca đó trước.
- `PUT /api/users/{id}/status` (quản lý tài khoản) không đổi: vẫn từ chối vô hiệu hoá tài khoản bác sĩ còn lịch hẹn sắp
  tới và không đụng tới trạng thái bác sĩ. Với bác sĩ hãy dùng 2 API ngừng công tác / công tác lại ở trên.
- catalog-service là nơi nhận mọi request của màn hình này. Tài khoản do identity-service ghi, ca làm việc do booking-service
  ghi: catalog-service gọi API nội bộ `/noi-bo/tai-khoan/**` và `/noi-bo/bac-si/{id}/huy-ca-sap-toi` của 2 service đó
  (không qua gateway, header `X-Khoa-Noi-Bo`). Chạy IntelliJ: đặt `IDENTITY_URL`, `BOOKING_URL` nếu không dùng cổng mặc định.

## Quản lý bệnh nhân và danh mục phòng khám, thuốc

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/booking/quan-tri/ho-so-benh-nhan?tuKhoa=&trangThaiLienKet=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Patient Management: danh sách hồ sơ, mới tạo trước, **cả hồ sơ của khách không có tài khoản**. Mỗi dòng: `id`, `hoTen`, `ngaySinh`, `gioiTinh`, `soDienThoai`, `cccdChe` (chỉ còn 4 số cuối), `coTaiKhoan`, `trangThaiLienKet`, `soLichHen`, `ngayTao` |
| `GET /api/booking/quan-tri/ho-so-benh-nhan/{id}/lich-hen?trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Lịch hẹn của 1 hồ sơ, mọi trạng thái, giờ khám muộn nhất trước. Cùng cấu trúc dòng với lịch hẹn của 1 ca (có `bacSi`). Không có chẩn đoán / đơn thuốc |
| `GET /api/catalog/quan-tri/phong-kham?tuKhoa=&idChuyenKhoa=&trangThai=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Medical Catalog: mọi phòng khám theo tên, **cả phòng ngừng hoạt động**: `id`, `tenPhong`, `tang`, `idChuyenKhoa`, `tenChuyenKhoa`, `trangThai` (`HOAT_DONG` / `NGUNG_HOAT_DONG`), `soCaSapToi` |
| `POST /api/catalog/quan-tri/phong-kham`, `PUT .../{id}` | `QUAN_TRI_VIEN` | `{ tenPhong, tang?, idChuyenKhoa }`; thêm → 201. Trùng tên: 409 `TEN_PHONG_DA_TON_TAI` |
| `POST /api/catalog/quan-tri/phong-kham/{id}/ngung-hoat-dong`, `POST .../{id}/hoat-dong-lai` | `QUAN_TRI_VIEN` | Phòng ngừng hoạt động không còn trong `GET /api/catalog/phong-kham` và không xếp ca mới vào được |
| `GET /api/medical/quan-tri/thuoc?tuKhoa=&daXacMinh=&trangThai=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Medical Catalog: danh mục thuốc, chưa xác minh trước rồi theo tên: `id`, `tenThuoc`, `donVi`, `moTa`, `daXacMinh`, `trangThai` (`DANG_DUNG` / `NGUNG_DUNG`), `tenBacSiTao`, `soLanKe` |
| `POST /api/medical/quan-tri/thuoc`, `PUT .../{id}` | `QUAN_TRI_VIEN` | `{ tenThuoc, donVi?, moTa? }`; thêm → 201, thuốc đã xác minh. Trùng tên (không phân biệt hoa thường / khoảng trắng): 409 `TEN_THUOC_DA_TON_TAI` |
| `POST /api/medical/quan-tri/thuoc/{id}/xac-minh`, `POST .../{id}/ngung-dung`, `POST .../{id}/dung-lai` | `QUAN_TRI_VIEN` | Xác minh thuốc bác sĩ tự thêm khi kê đơn; cho ngừng dùng / dùng lại. Gọi lại nhiều lần không sao |

- **Tìm hồ sơ bệnh nhân.** `tuKhoa` so với họ tên, số điện thoại, số bảo hiểm y tế (chứa từ khoá). Số CCCD chỉ tra được khi
  gõ **đủ 12 số** (khớp hoàn toàn); gõ 1 phần số CCCD không ra kết quả. Danh sách chỉ trả 4 số cuối của CCCD, số đầy đủ có
  ở `GET /api/booking/quan-tri/ho-so-benh-nhan/{id}`.
- **Phòng khám không xoá được**, chỉ ngừng hoạt động. Phòng còn ca sắp tới (`soCaSapToi > 0`) thì ngừng hoạt động và đổi
  chuyên khoa đều trả 409 `PHONG_CON_CA_LAM_VIEC` (thông điệp có số ca): chuyển các ca đó sang phòng khác bằng
  `PUT /api/booking/quan-tri/lich-lam-viec/{id}` trước. Tên phòng và tầng sửa được bất cứ lúc nào.
- **Thuốc không xoá và không gộp được.** Thuốc đã có trong đơn thuốc (`soLanKe > 0`) chỉ sửa được cách viết hoa / khoảng
  trắng của tên; đổi sang tên khác trả 409 `THUOC_DA_DUOC_KE` (dòng đơn thuốc chỉ lưu id thuốc, đổi tên là sửa luôn đơn
  cũ). Đơn vị và mô tả sửa được bất cứ lúc nào.
- **Thuốc ngừng dùng** không còn trong gợi ý `GET /api/medical/bac-si/toi/thuoc`, và bác sĩ kê thuốc đó ở đơn mới nhận 409
  `THUOC_NGUNG_DUNG` (thông điệp có tên thuốc). Hồ sơ bệnh án đã có thuốc đó vẫn sửa / lưu lại được với dòng thuốc cũ, và
  bệnh nhân vẫn thấy thuốc trong kết quả khám cũ.

## Đánh giá lượt khám

| API | Ai gọi | Ghi chú |
|---|---|---|
| `POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/danh-gia` | `BENH_NHAN` | `{ soSao: 1..5, nhanXet? (tối đa 1000 ký tự) }` → 201 `{ soSao, nhanXet, ngayTao, duocSuaDen }`. Mỗi lịch hẹn 1 đánh giá |
| `PUT /api/booking/lich-hen/cua-toi/{maPhieuKham}/danh-gia` | `BENH_NHAN` | Cùng thân, thay số sao và nhận xét, trong 7 ngày kể từ lúc gửi (`duocSuaDen`) |
| `GET /api/booking/lich-hen/cua-toi/{maPhieuKham}` (có từ trước) | `BENH_NHAN` | Thêm `duocDanhGia` (true: hiện form gửi đánh giá) và `danhGia` (đánh giá đã gửi, hoặc null) |
| `GET /api/booking/bac-si/toi/danh-gia/tong-quan` | `BAC_SI` | `{ diemTrungBinh, soDanhGia, phanBo: { "1": n, ..., "5": n } }`; `diemTrungBinh` null khi chưa có đánh giá. Ô "Average Rating" của Dashboard |
| `GET /api/booking/bac-si/toi/danh-gia?trang=&kichThuoc=` | `BAC_SI` | Đánh giá tôi nhận được, mới nhất trước: `soSao`, `nhanXet`, `ngayKham`, `ngayTao`. **Ẩn danh**: không có tên bệnh nhân, không có mã lịch hẹn |
| `GET /api/booking/quan-tri/danh-gia?idBacSi=&soSaoToiDa=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Mới nhất trước: `id`, `soSao`, `nhanXet`, `ngayTao`, `ngayCapNhat`, `maTraCuu`, `ngayKham`, `idHoSoBenhNhan`, `tenBenhNhan`, `idBacSi`, `tenBacSi`. `soSaoToiDa=2`: chỉ đánh giá 1-2 sao |
| `GET /api/catalog/bac-si`, `GET /api/catalog/bac-si/{id}` (có từ trước) | Công khai | Thêm `diemDanhGia` (1 chữ số thập phân, null khi chưa có) và `soDanhGia`; luôn hiển thị điểm kèm số lượt, vd `★ 4.5 (12)` |

- **Ai được đánh giá:** tài khoản bệnh nhân được xem kết quả của lượt khám đó (chủ tài khoản là người khám, hoặc tài khoản đã
  đặt lịch), và chỉ khi lịch hẹn `DA_HOAN_THANH`. Khách không có tài khoản không đánh giá được. Tài khoản chỉ thấy lịch
  vì là người giám hộ theo CCCD: 403.
- **Mã lỗi:** 409 `LICH_HEN_CHUA_KHAM_XONG` (lịch chưa khám xong / đã hủy), 409 `DA_DANH_GIA` (gửi lần 2, dùng `PUT` để
  sửa), 409 `HET_HAN_SUA_DANH_GIA` (quá 7 ngày), 404 (không thấy lịch hẹn, hoặc `PUT` khi chưa có đánh giá), 400 (số sao
  ngoài 1..5, nhận xét quá dài). Không có xoá đánh giá.
- **Nhận xét không công khai:** chỉ bác sĩ (ẩn danh) và quản trị viên (có tên bệnh nhân) đọc. Trang công khai chỉ có điểm
  trung bình và số lượt, hiện ngay từ đánh giá đầu tiên. Điểm = trung bình cộng mọi đánh giá của bác sĩ, tính khi đọc.
- Thời hạn sửa đổi bằng `RATING_EDIT_DAYS` (mặc định 7) của booking-service.

## Dashboard

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/booking/bac-si/toi/tong-quan` | `BAC_SI` | Dashboard của bác sĩ, 1 lần gọi cho cả 4 ô: `{ lichHenHomNay, dangCho, daKham, diemDanhGia, soDanhGia, yeuCauChoXacNhan, tinhLuc }` |
| `GET /api/booking/quan-tri/tong-quan` | `QUAN_TRI_VIEN` | Dashboard của quản trị viên, 1 lần gọi cho cả 3 ô: `{ bacSiDangCongTac, benhNhanDaDangKy, luotDatTrongThang, tinhLuc }` |

Định nghĩa từng số (frontend không tự đếm lại từ các danh sách: danh sách có phân trang và bộ lọc riêng):

| Ô trên UI | Trường | Cách tính |
|---|---|---|
| Today's Appointments | `lichHenHomNay` | Lịch hẹn của bác sĩ có giờ khám hôm nay, **không tính** `DA_HUY`, `DA_HUY_DO_DOI_LICH`, `BI_TU_CHOI` |
| Waiting | `dangCho` | Trong số đó: `CHO_XAC_NHAN` hoặc `DA_XAC_NHAN` và không có `canDoiLich` (chưa khám) |
| Completed | `daKham` | Trong số đó: `DA_HOAN_THANH` |
| Average Rating | `diemDanhGia`, `soDanhGia` | Trung bình mọi đánh giá của bác sĩ, 1 chữ số thập phân; `null` và `0` khi chưa có. Hiển thị kèm số lượt |
| (nhãn Appointment Requests) | `yeuCauChoXacNhan` | Số lịch `CHO_XAC_NHAN` từ bây giờ trở đi của bác sĩ = `tongSoPhanTu` của danh sách yêu cầu |
| Active Doctors | `bacSiDangCongTac` | Bác sĩ `DANG_CONG_TAC` có tài khoản `DA_KICH_HOAT`: đúng tập bác sĩ của danh sách công khai |
| Registered Patients | `benhNhanDaDangKy` | Tài khoản vai trò `BENH_NHAN` đã kích hoạt. Khách chỉ đặt lịch (không có tài khoản) không tính |
| Bookings This Month | `luotDatTrongThang` | Lịch hẹn **được tạo** từ 00:00 ngày 1 tháng này, mọi trạng thái trừ `DA_HUY_DO_DOI_LICH` (1 lần đổi lịch chỉ tính 1) |

- `tinhLuc` là thời điểm backend tính các số; số được đếm trực tiếp mỗi lần gọi, không lưu sẵn.
- "System Uptime" trên Dashboard của quản trị viên **không có API** (đã thống nhất bỏ): frontend ẩn ô này.
- Bảng "Màn hình UI → API" (mỗi màn hình của danh sách UI gọi những API nào) nằm ở đầu
  [docs/BAN-GIAO-FRONTEND-05-10.md](docs/BAN-GIAO-FRONTEND-05-10.md).

## Thông báo trong ứng dụng

Chuông thông báo: lưu, liệt kê, đếm chưa đọc, đánh dấu đã đọc. Không gửi email, không đẩy realtime: frontend hỏi lại
`so-chua-doc` định kỳ (trang mẫu: 30 giây, bỏ qua khi tab đang ẩn, hỏi ngay khi tab hiện lại).

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/notification/thong-bao?chuaDoc=&trang=&kichThuoc=` | Mọi tài khoản đã đăng nhập | Thông báo của tôi, mới nhất trước, dạng `TrangDuLieu`. `chuaDoc=true` chỉ lấy thông báo chưa đọc; `kichThuoc` mặc định 20, tối đa 100. Mỗi dòng: `{ id, loai, noiDung, daDoc, ngayTao, idLichHen, maPhieuKham, idYeuCau }`. `noiDung` là câu tiếng Việt đã soạn sẵn, hiển thị nguyên văn. `maPhieuKham` chỉ có khi người đọc là bệnh nhân (dùng để mở chi tiết lịch hẹn); bác sĩ dùng `idLichHen` |
| `GET /api/notification/thong-bao/so-chua-doc` | Mọi tài khoản đã đăng nhập | `{ soChuaDoc }`: số trên biểu tượng chuông |
| `POST /api/notification/thong-bao/{id}/da-doc` | Mọi tài khoản đã đăng nhập | Đánh dấu 1 thông báo của tôi đã đọc, gọi lại nhiều lần vẫn được. Thông báo của tài khoản khác hoặc không có -> 404 `KHONG_TIM_THAY_THONG_BAO` |
| `POST /api/notification/thong-bao/da-doc-tat-ca` | Mọi tài khoản đã đăng nhập | `{ soDaDanhDau }` |

| Sự kiện | `loai` | Người nhận |
|---|---|---|
| Có lịch hẹn mới (kể cả khách đặt) | `LICH_HEN_MOI` | Bác sĩ của lịch hẹn |
| Bác sĩ xác nhận | `LICH_HEN_DA_XAC_NHAN` | Tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân (mỗi tài khoản 1 thông báo) |
| Bác sĩ từ chối (câu thông báo có lý do) | `LICH_HEN_BI_TU_CHOI` | Như trên |
| Bệnh nhân hủy | `LICH_HEN_DA_HUY` | Bác sĩ của lịch hẹn |
| Bệnh nhân đổi lịch, cùng bác sĩ | `LICH_HEN_DA_DOI` (gắn với lịch mới) | Bác sĩ đó, 1 thông báo |
| Bệnh nhân đổi lịch sang bác sĩ khác | `LICH_HEN_DA_HUY` cho bác sĩ cũ, `LICH_HEN_MOI` cho bác sĩ mới | Cả 2 bác sĩ |
| Ca khám bị hủy (quản trị viên hủy, hoặc duyệt xin nghỉ / đổi ca) | `LICH_HEN_CAN_DOI` | Tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân, cho từng lịch hẹn còn hiệu lực của ca |
| Ca khám đổi phòng | `LICH_HEN_DOI_PHONG` | Như trên |
| Quản trị viên xếp ca mới (1 thông báo cho 1 lần xếp), sửa hoặc hủy ca | `CA_LAM_VIEC_THAY_DOI` | Bác sĩ của ca |
| Bác sĩ gửi yêu cầu đổi ca / xin nghỉ | `YEU_CAU_DOI_LICH_MOI` (có `idYeuCau`) | Mọi quản trị viên đang hoạt động |
| Quản trị viên duyệt / từ chối yêu cầu | `KET_QUA_DUYET_DOI_LICH` (có `idYeuCau`, câu thông báo có ghi chú) | Bác sĩ đã gửi |

Người làm thao tác không nhận thông báo về thao tác của chính mình. Khách đặt không đăng nhập và người giám hộ chỉ khớp theo
CCCD không nhận thông báo. Chưa có nhắc lịch khám.

Cách thông báo đi từ booking-service sang notification-service (không mất khi notification-service ngừng chạy):

1. Trong **cùng transaction** đổi trạng thái lịch hẹn, booking-service ghi 1 dòng cho mỗi người nhận vào bảng của mình
   `su_kien_thong_bao` (`ThongBaoLichHenService`).
2. `GuiThongBaoJob` (mỗi `BOOKING_NOTIFY_INTERVAL`, mặc định 5 giây) gửi các dòng chưa gửi sang
   `POST /noi-bo/thong-bao` của notification-service, và chỉ đặt `ngay_gui` khi được trả 2xx. Không gọi được thì dòng nằm
   chờ, lần sau gửi lại.
3. notification-service lưu `thong_bao.ma_nguon = booking:<id sự kiện>` (UNIQUE): cùng 1 sự kiện gửi lại không tạo thông
   báo thứ hai.

`/noi-bo/**` là API giữa các service: gateway không chuyển tiếp (chỉ chuyển `/api/**`), người gọi gửi khoá chung
`INTERNAL_API_KEY` trong header `X-Khoa-Noi-Bo`. Cấu hình: `INTERNAL_API_KEY`, `NOTIFICATION_URL`,
`BOOKING_NOTIFY_INTERVAL` (xem `.env.example`). Chạy trong IntelliJ: phải chạy cả `notification-service` thì chuông mới
có dữ liệu; không chạy thì đặt lịch vẫn bình thường, thông báo nằm chờ.

## Chạy test

Cần Docker đang chạy (test dùng Testcontainers để bật MySQL/Redis tạm).

```bash
mvnw.cmd clean verify          # Windows (cần JAVA_HOME trỏ tới JDK 17)
./mvnw clean verify            # macOS / Linux / Git Bash
./mvnw -pl catalog-service -am verify   # chỉ một service (kèm common)
```

Trong IntelliJ: chuột phải thư mục `src/test/java` của module → **Run 'All Tests'**.

Máy không cài JDK: chạy Maven trong container (Git Bash, ở thư mục gốc):

```bash
MSYS_NO_PATHCONV=1 docker run --rm -v "$PWD":/workspace -w /workspace \
  -v /var/run/docker.sock:/var/run/docker.sock -v eclinic-m2:/root/.m2 \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  maven:3.9-eclipse-temurin-17 mvn -B -ntp clean verify
```

## Lỗi thường gặp

| Hiện tượng | Nguyên nhân / cách xử lý |
|---|---|
| `Port 808x was already in use` | Service đó đang chạy ở chỗ khác (IntelliJ và Docker cùng lúc?). Dừng một bên. |
| Gọi qua gateway nhận 503 `DICH_VU_KHONG_KHA_DUNG` | Service đích chưa chạy hoặc chưa khởi động xong. |
| 401 `CHUA_DANG_NHAP` dù đã dán token | Token hết hạn, hoặc `JWT_SECRET` của service khác khoá tạo token. Tạo token mới. |
| 403 `KHONG_CO_QUYEN` | Token đúng nhưng sai vai trò (ví dụ API chỉ dành cho `QUAN_TRI_VIEN`). |
| Không đăng nhập được `bacsi01@eclinic.local` | Dữ liệu mẫu chưa được tạo: `SEED_DATA` đang tắt (chạy ngoài docker compose / run config có sẵn), hoặc DB đã có bác sĩ từ trước khi bật. |
| Tải ảnh đại diện nhận 503 `LUU_TRU_ANH_KHONG_KHA_DUNG` | Chưa đặt đủ 3 biến `CLOUDINARY_*` (xem mục Ảnh đại diện), hoặc Cloudinary không phản hồi. |
| Tải ảnh nhận 413 `TEP_QUA_LON` / `YEU_CAU_QUA_LON` | Ảnh quá 2 MB / request quá 5 MB tại gateway (`GATEWAY_MAX_REQUEST_SIZE`). |
| Flyway báo `checksum mismatch` | Đã sửa file migration đã chạy. Không sửa file cũ; tạo file `V<n>__...sql` mới. Máy local có thể `docker compose down -v` để làm lại DB. |
| Service báo lỗi kết nối MySQL | Chưa `docker compose up -d`, hoặc MySQL chưa "healthy" (xem `docker compose ps`). |
| `Command line is too long` (Windows) | Run config đã đặt *Shorten command line: @argfiles*; nếu tự tạo config mới thì chọn như vậy. |
