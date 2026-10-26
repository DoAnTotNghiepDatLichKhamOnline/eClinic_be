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
- **Tài liệu API cho frontend** (route, request, response, lỗi), mỗi vai trò 1 tài liệu đọc độc lập:
  [bệnh nhân / khách](docs/api/API-BENH-NHAN-KHACH.md), [bác sĩ](docs/api/API-BAC-SI.md),
  [quản trị viên](docs/api/API-QUAN-TRI.md). Mục 1–2 (quy ước, đăng nhập, tài khoản, thông báo) giống nhau ở cả 3 tài liệu:
  chỉ sửa ở `docs/api/_CHUNG.md` rồi chạy `node scripts/kiem-tra-tai-lieu-api.js` để chép sang. Thêm / sửa route thì chạy
  `node scripts/kiem-tra-tai-lieu-api.js --kiem-tra`: báo route có trong code mà chưa có trong tài liệu và ngược lại.
  - Bản Word của 3 tài liệu (để gửi / nộp): `eClinic_API_BenhNhan_Khach.docx`, `eClinic_API_BacSi.docx`,
    `eClinic_API_QuanTri.docx` ở thư mục gốc, dựng từ `docs/api/` bằng `node scripts/xuat-docx-api.js` (cần Docker), không
    đưa vào git. Sửa Markdown xong thì chạy lại lệnh này; không sửa tay tệp .docx.
- Tài liệu cũ, **chỉ để tra lịch sử thay đổi**, không còn cập nhật: bàn giao theo màn hình
  [docs/BAN-GIAO-FRONTEND-05-10.md](docs/BAN-GIAO-FRONTEND-05-10.md),
  [docs/BAN-GIAO-FRONTEND-NGON-NGU-EMAIL-CHATBOT.md](docs/BAN-GIAO-FRONTEND-NGON-NGU-EMAIL-CHATBOT.md), và 2 tệp Word
  `eClinic_API_XacThuc_TaiKhoan.docx`, `eClinic_API_LichHen_KhamBenh_BacSi_QuanTri.docx` (dựng bằng `scripts/api-doc/`).

Công nghệ: Java 17, Spring Boot 4.1, Spring Cloud Gateway (WebMVC), Spring Security (JWT HS256), Spring Data JPA,
Flyway, MySQL 8.4, Redis, springdoc (Swagger), Maven, Docker Compose.

## Các module

| Module | Cổng | Chức năng (mã yêu cầu) | Swagger |
|---|---|---|---|
| `api-gateway` | 8080 | Cổng vào duy nhất cho frontend: định tuyến + CORS | (không có) |
| `identity-service` | 8081 | AUTH-01..04; hồ sơ cá nhân, đổi mật khẩu, thiết bị đăng nhập, ảnh đại diện, đổi email; quản trị tài khoản (UC-USER-01); dữ liệu mẫu | http://localhost:8081/swagger-ui.html |
| `catalog-service` | 8082 | ADM-01, ADM-02, AUTH-04 (hồ sơ bác sĩ), DOC-01, DOC-03 | http://localhost:8082/swagger-ui.html |
| `booking-service` | 8084 | BOOK-01..12, SCHED-01..05, PAT-01..03, ADM-03, ADM-04, EXAM-03 / EXAM-04 (trang sổ khám bệnh); quy tắc đặt lịch, ngày bệnh viện nghỉ, đặt lịch qua tổng đài | http://localhost:8084/swagger-ui.html |
| `medical-service` | 8085 | EXAM-01..05, DOC-02 | http://localhost:8085/swagger-ui.html |
| `notification-service` | 8086 | NOTI-01: thông báo trong ứng dụng (chuông) | http://localhost:8086/swagger-ui.html |
| `chatbot-service` | 8087 | UC-CHAT-01: chatbot (trợ lý AI) | http://localhost:8087/swagger-ui.html |
| `report-service` | 8088 | DASH-01..04, ADM-05: báo cáo thống kê theo kỳ cho quản trị viên, xuất Excel (Apache POI); chỉ đọc DB chung | http://localhost:8088/swagger-ui.html |
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

Service chạy trong IntelliJ đọc file `.env` ở thư mục gốc (run config có sẵn đặt thư mục làm việc là thư mục gốc dự án),
nên nhận cùng giá trị như cách 2: email, đăng nhập Google, Cloudinary, khoá chatbot, cổng MySQL... Không có `.env` thì dùng
giá trị mặc định trong `application.yml` / `common/src/main/resources/application-common.yml`, vốn khớp với MySQL/Redis của
`docker compose up -d`. Biến môi trường đặt trong run config được ưu tiên hơn `.env` (ví dụ `SEED_DATA=true` của
identity-service). Tự tạo run config mới thì đặt *Working directory* là thư mục gốc dự án (`$PROJECT_DIR$`).

- Ghi giá trị trong `.env` trơn, không có dấu nháy (`MAIL_USERNAME=ten-ban@gmail.com`).
- **`MAIL_MODE=smtp` trong `.env` thì service chạy trong IntelliJ cũng gửi email thật.**
- Máy đã có MySQL / MariaDB khác chiếm cổng 3306: dừng nó, hoặc đặt `DB_PORT=3307` trong `.env` (IntelliJ và Docker cùng theo).

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
| Ngôn ngữ ưa dùng | `PUT /api/users/me/language` `{ ngonNgu: "VI" \| "EN" }` | Ngôn ngữ của email và lời nhắc gửi cho tài khoản; trả về ở `taiKhoan.ngonNgu` khi đăng nhập. Xem mục "Ngôn ngữ (EN / VI)" |
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
   Chạy bằng IntelliJ cũng đọc 3 biến này từ `.env` (identity-service và catalog-service), không cần đặt trong run config.
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
   Chạy bằng IntelliJ cũng đọc 3 biến này từ `.env`, không cần đặt trong run config.
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

- **Nút EN | VI** (thanh trên, mọi màn hình): đổi cả chữ của chính trang demo (nhãn, nút, thanh điều hướng, câu báo lỗi phía
  trình duyệt) lẫn dữ liệu từ API (mọi request gửi `Accept-Language`; màn hình đang mở được tải lại). Họ tên, mã và chữ
  người dùng gõ giữ nguyên. Chữ trong mã nguồn viết thành cặp `t('Tiếng Việt', 'English')`; sau khi sửa màn hình chạy
  `node scripts/kiem-tra-demo-ngon-ngu.js` để tìm chuỗi tiếng Việt chưa có bản tiếng Anh.
- **Nút "Hỏi trợ lý"** (góc dưới phải, mọi màn hình, mọi vai trò): khung chat với chatbot. Vẽ thẻ chuyên khoa / bác sĩ /
  khung giờ từ `goiY` của câu trả lời; bấm thẻ mở `/appointment` đã chọn sẵn. Bệnh nhân đã đăng nhập hỏi về lịch hẹn của
  mình sẽ thấy nút "Đồng ý" trước khi chatbot đọc dữ liệu. Đăng nhập / đăng xuất thì khung chat mở phiên mới. Cần
  chatbot-service đang chạy và có `CHATBOT_API_KEY` (xem mục "Chatbot"); không có khoá thì gửi tin báo lỗi 503.

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
| `/doctor/upcoming-appointments` | bác sĩ | Doctor: Upcoming Appointments (thay Appointment Requests từ 15/10) | `bs-lich-hen-sap-toi.js` |
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
kiến, link, mã QR). Lịch mới ở trạng thái `DA_XAC_NHAN` ngay, không chờ bác sĩ duyệt (từ 15/10/2026). Bác sĩ hủy được lịch
trước giờ khám, kèm lý do (`BI_TU_CHOI`); bác sĩ ghi kết quả khám thì lịch chuyển sang `DA_HOAN_THANH`.

- **Không đến (`KHONG_DEN`):** job đóng ngày khám của booking-service chạy lúc 00:01 mỗi ngày và mỗi lần service khởi động:
  lịch `DA_XAC_NHAN` của ngày đã qua mà chưa khám thành `KHONG_DEN`, trừ người đã được gọi vào khám (`DANG_KHAM`) và lịch
  đang chờ đổi (`canDoiLich`). Lịch `KHONG_DEN` không hủy, đổi hay đánh giá được; hàng đợi của ngày đó hiện là `VANG_MAT`.
- Những gì frontend phải đổi từ 15/10/2026 (xác nhận ngay, bác sĩ hủy lịch, không đến, bệnh án đầy đủ): khối "Thay đổi ngày
  15/10/2026" trong `docs/BAN-GIAO-FRONTEND-05-10.md`; route phía bệnh nhân ở
  [docs/api/API-BENH-NHAN-KHACH.md](docs/api/API-BENH-NHAN-KHACH.md).

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
| `GET /api/catalog/bac-si?idChuyenKhoa=&tuKhoa=&trang=&kichThuoc=`, `GET /api/catalog/bac-si/{id}` | Công khai | Bác sĩ đang công tác, tài khoản còn hoạt động; không có email, số điện thoại. Danh sách: `chucVu`, `gioiThieuNgan`, `soNamKinhNghiem` (suy ra từ năm bắt đầu hành nghề); chi tiết thêm `tieuSu`, `quaTrinhDaoTao[]`, `quaTrinhCongTac[]`, `linhVucKhamChua[]`, `giaiThuong[]`, `congTrinhNghienCuu[]`, `ngonNguKham[]` (mã ISO 639), giấy phép hành nghề (`soGiayPhep`, `ngayCapGiayPhep`, `noiCapGiayPhep`, `phamViHanhNghe`), `anh[]` |
| `GET /api/catalog/quan-tri/bac-si/{id}`, `PUT .../{id}/ho-so`, `POST .../{id}/anh`, `PUT .../{id}/anh/{idAnh}`, `PUT .../{id}/anh/thu-tu`, `DELETE .../{id}/anh/{idAnh}` | `QUAN_TRI_VIEN` | Sửa hồ sơ giới thiệu của bác sĩ (`PUT .../ho-so` bắt buộc `phienBan` = `phienBanHoSo` đã tải; bản cũ -> 409 `DU_LIEU_DA_THAY_DOI`); tải lên (multipart `anh`, `loai`, `chuThich`), sửa, sắp xếp, xoá ảnh giới thiệu. Tối đa 12 ảnh / bác sĩ, 4 MB / ảnh |
| `GET` / `PUT /api/catalog/bac-si/toi/ho-so`, `POST /api/catalog/bac-si/toi/anh`, `PUT` / `DELETE .../toi/anh/{idAnh}` | `BAC_SI` | Bác sĩ tự sửa 1 phần hồ sơ giới thiệu: giới thiệu ngắn, tiểu sử, ngôn ngữ khám, giải thưởng, nghiên cứu (VI + EN, kèm `phienBan`) và ảnh làm việc (ảnh chứng chỉ -> 403). Phải gửi token dù `GET /api/catalog/bac-si/**` là công khai. Chi tiết: `docs/BAN-GIAO-FRONTEND-05-10.md` mục "Hồ sơ bác sĩ: bác sĩ tự sửa" |
| `GET /api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa=` | Công khai | Ngày sớm nhất còn chỗ của từng bác sĩ trong chuyên khoa (cho thẻ bác sĩ) |
| `GET /api/booking/khung-gio/nhieu-ngay?idBacSi=&tuNgay=&denNgay=` | Công khai | Các ca và khung giờ của 1 bác sĩ, tối đa 7 ngày (mặc định từ hôm nay) |
| `GET /api/booking/khung-gio/gop?idChuyenKhoa=&ngay=` | Công khai | Khung giờ gộp các bác sĩ của chuyên khoa (`tongSoCho`, `soChoConLai`, `soBacSi`), dùng cho "bác sĩ bất kỳ" |
| `GET /api/booking/khung-gio/ngay-con-cho?tuNgay=&denNgay=&idBacSi=&idChuyenKhoa=` | Công khai | Các ngày có ca đặt được và số chỗ còn lại |
| `GET /api/booking/khung-gio?ngay=&idBacSi=&idChuyenKhoa=` | Công khai | Các ca trong ngày, mỗi ca kèm bác sĩ, phòng và các khung 1 tiếng (`tongSoCho`, `soChoConLai`, `hetCho`) |
| `POST /api/booking/lich-hen` | Công khai; có token `BENH_NHAN` thì lưu vào tài khoản | Body: `idLichLamViec` **hoặc** `idChuyenKhoa` (đúng 1 trong 2), `gioBatDauKhung`, `benhNhan` {`hoTen`, `ngaySinh`, `gioiTinh`, `soDienThoai`, `cccd` (bỏ trống được khi dưới 18 tuổi), `email`?, `soBaoHiemYTe`?, `diaChi`?}, `nguoiGiamHo` {`hoTen`, `quanHe`, `soDienThoai`, `cccd`, `ngaySinh`?}, `lyDoKham`, `datChoBanThan`?, `luuNguoiThan`?. 201: `maPhieuKham`, `maTraCuu`, `linkPhieuKham`, `soThuTu`, `gioKhamDuKien`, `bacSi`, `bacSiDoPhongKhamXep`, `luuVaoTaiKhoan`... |
| `GET /api/booking/phieu-kham/{maPhieuKham}` | Công khai (ai có mã cũng xem được) | Phiếu khám; CCCD và số điện thoại đã che; trẻ dưới 18 tuổi có người giám hộ và `canNguoiGiamHoDiCung`. `lyDoHuy` = lý do bác sĩ hủy khi `trangThai` là `BI_TU_CHOI`, hoặc lý do bệnh nhân hủy (null ở trạng thái khác); phiếu của lịch đã hủy vẫn mở được |
| `GET /api/booking/phieu-kham/{maPhieuKham}/qr?kichThuoc=&taiVe=` | Công khai | Ảnh PNG mã QR của link phiếu khám (100–1000 px, mặc định 300; `taiVe=true` để tải về) |
| `GET /api/booking/lich-hen/cua-toi?loc=TAT_CA\|SAP_TOI\|LICH_SU&cuaAi=TAT_CA\|BAN_THAN\|NGUOI_KHAC&trang=&kichThuoc=` | `BENH_NHAN` | Lịch tài khoản đã đặt (cho bản thân hoặc người thân), lịch của hồ sơ bệnh nhân đã liên kết (kể cả lịch đặt như khách) và lịch của người khám có người giám hộ khai số CCCD của hồ sơ đó; kèm `maTraCuu`, link phiếu khám. `cuaAi`: `BAN_THAN` = người khám là chủ tài khoản (bất kể ai đặt), `NGUOI_KHAC` = còn lại. Mỗi dòng có `laBanThan`, `nguoiDat` (`TOI` / `KHACH` / `TAI_KHOAN_KHAC`) và `thongTinKhacHoSo` (lịch của tôi mà người đặt nhập họ tên / ngày sinh khác hồ sơ của tôi) |
| `GET /api/booking/lich-hen/cua-toi/lich?tuNgay=&denNgay=&cuaAi=` | `BENH_NHAN` | Lịch hẹn của tài khoản theo khoảng ngày cho màn hình lịch: mọi trạng thái, theo giờ khám, không phân trang, tối đa 42 ngày (lưới tháng 6 tuần); dòng như "lịch hẹn của tôi" |
| `GET /api/booking/bac-si/toi/lich-hen/lich?tuNgay=&denNgay=` | `BAC_SI` | Lịch hẹn của chính bác sĩ theo khoảng ngày cho màn hình lịch (tối đa 42 ngày), dòng như danh sách trong ngày |
| `GET /api/booking/lich-hen/cua-toi/{maPhieuKham}` | `BENH_NHAN` | Chi tiết 1 lịch hẹn tài khoản được xem: `lichHen` (như dòng danh sách), `ngaySinhBenhNhan`, `gioiTinhBenhNhan`, `soDienThoaiLienHe`, `emailLienHe` (2 trường liên hệ chỉ có khi chính tài khoản này đặt). `lichHen.lyDoHuy` = lý do bác sĩ hủy hoặc lý do bệnh nhân hủy (có ở cả dòng danh sách). Lịch không thuộc tài khoản -> 404 |
| `GET /api/booking/trang-ca-nhan/cua-toi` | `BENH_NHAN` | Trang cá nhân trong 1 lần gọi: `hoSo`, `emailTaiKhoan`, `nguoiThan[]`, `lichSapToi` {`cuaToi[]`, `cuaNguoiKhac[]`} (5 lịch gần nhất mỗi bên), `lanKhamGanDay[]` (5 lượt đã khám gần nhất kèm kết quả), `soLich` {`sapToiCuaToi`, `sapToiCuaNguoiKhac`, `lichSu`, `daKham`} |
| `POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/huy`, `POST .../cua-toi/{maPhieuKham}/doi-lich` | `BENH_NHAN` | Hủy `{ lyDo? }` (trả phiếu khám `DA_HUY`) hoặc đổi lịch `{ idLichLamViec | idChuyenKhoa, gioBatDauKhung }` (201, trả lịch mới `DA_XAC_NHAN` với mã phiếu khám mới; lịch cũ thành `DA_HUY_DO_DOI_LICH`, thông tin người khám lấy từ lịch cũ). Chỉ tài khoản đã đặt lịch hoặc người khám (`laBanThan`); tài khoản chỉ là người giám hộ theo CCCD -> 403. Chỉ lịch `DA_XAC_NHAN`, trước giờ khám ít nhất `huyDoiTruocToiThieuPhut` (mặc định 120 phút), đổi tối đa `soLanDoiLichToiDa` (mặc định 2) lần cho 1 lần đặt (quy tắc ở `/api/booking/quan-tri/cau-hinh`). Lượt khám cũ được mở lại. Dòng "lịch hẹn của tôi" và phiếu khám có `duocHuyDoi`, `hanHuyDoi` |
| `POST /api/booking/phieu-kham/{maPhieuKham}/huy`, `POST .../phieu-kham/{maPhieuKham}/doi-lich` | Công khai (cần đúng SĐT) | Hủy / đổi lịch bằng link phiếu khám cho khách không có tài khoản: body như trên kèm `soDienThoai` = SĐT liên hệ đã nhập lúc đặt lịch (thiếu / sai dạng -> 400, không khớp -> 403 `SO_DIEN_THOAI_KHONG_KHOP`). Số lần gọi từ 1 địa chỉ IP bị giới hạn như đặt lịch |
| `GET /api/booking/lich-su-kham/cua-toi?cuaAi=TAT_CA\|BAN_THAN\|NGUOI_KHAC&trang=&kichThuoc=` | `BENH_NHAN` | Lịch sử khám: các lượt đã khám xong mà tài khoản được xem kết quả, mới nhất trước. Mỗi dòng: `lichHen` (như dòng "lịch hẹn của tôi") và `ketQua` {`lyDoKham`, `sinhHieu` (null khi không đo), `khamLamSang`, `chanDoan`, `icdChinh` {`ma`, `ten`, `manTinh`} (null khi không chọn mã), `chanDoanKemTheo`, `ghiChu`, `ngayTaiKhamDeXuat`, `donThuoc[]` (kèm `idThuoc`, `hoatChat`, `hamLuong`, `duongDung`, `soLuong`), `ngaySuaCuoi` (khác null: bác sĩ đã sửa kết quả, hiện "Đã chỉnh sửa lúc …")}. Được xem kết quả khi người khám là chủ tài khoản (bất kể ai đặt) hoặc chính tài khoản đã đặt lịch; lịch chỉ thấy vì là người giám hộ theo CCCD thì không có kết quả. `GET .../lich-hen/cua-toi/{maPhieuKham}` cũng trả `ketQua` theo cùng quy tắc |
| `GET /api/booking/ho-so-benh-nhan/cua-toi`, `PUT /api/booking/ho-so-benh-nhan/cua-toi` | `BENH_NHAN` | Xem, tạo, sửa hồ sơ bệnh nhân của tài khoản; số CCCD không đổi được sau khi đã có hồ sơ. Số CCCD đã có hồ sơ chưa gắn tài khoản: khớp thì liên kết, không khớp trả `trangThaiLienKet: CHO_XAC_MINH` |
| `GET /api/booking/thong-tin-dat-lich/cua-toi`, `PUT .../cua-toi/nguoi-than/{id}`, `DELETE .../cua-toi/nguoi-than/{id}` | `BENH_NHAN` | Điền sẵn form: `banThan`, `emailTaiKhoan`, `nguoiThan[]` đã lưu, `lanDatGanNhat`; sửa bản lưu của 1 người thân (các trường của `benhNhan` trừ `cccd`, kèm `nguoiGiamHo`?; không sửa hồ sơ bệnh nhân); bỏ 1 người thân đã lưu |
| `GET /api/booking/bac-si/toi/lich-lam-viec?tuNgay=&denNgay=`, `GET /api/booking/bac-si/toi/lich-hen?ngay=`, `GET /api/booking/bac-si/toi/lich-hen/tra-cuu?ma=` | `BAC_SI` | Ca làm việc của chính bác sĩ (tối đa 42 ngày, kèm số lượt đã đặt), bệnh nhân trong ngày, tra 1 lịch hẹn theo mã tra cứu, mã phiếu khám hoặc cả link phiếu khám (quét QR). Danh sách trong ngày: `ngay` bỏ trống = hôm nay; lọc `idLichLamViec`, `soThuTu`, `tuKhoa` (họ tên, không xét dấu / hoa thường) |
| `GET /api/booking/bac-si/toi/lich-hen/{id}/ho-so-kham`, `GET /api/booking/bac-si/toi/lich-hen/tra-cuu/ho-so-kham?ma=` | `BAC_SI` | Hồ sơ khám của bệnh nhân từ 1 lịch hẹn của chính bác sĩ (theo id, hoặc theo mã / link phiếu khám quét từ QR): `lichHen`, `hoSoBenhNhan` (số CCCD không che, `tienSuBenhLy`), `lanKhamTruoc[]` (các lần khám trước với mọi bác sĩ, kèm `ketQua` nếu đã có bệnh án). Lịch hẹn của bác sĩ khác -> 404 |
| `POST` / `PUT` / `GET /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an`, `GET /api/medical/bac-si/toi/thuoc?tuKhoa=` | `BAC_SI` | Khám bệnh (medical-service): ghi kết quả khám `{ lyDoKham?, sinhHieu?, khamLamSang?, chanDoan, maIcdChinh?, chanDoanKemTheo?, ghiChu?, ngayTaiKhamDeXuat?, donThuoc[]? }` cho lịch hẹn `DA_XAC_NHAN` của chính bác sĩ, từ ngày khám trở đi; lịch hẹn chuyển sang `DA_HOAN_THANH` trong cùng transaction. `sinhHieu` `{ mach?, nhietDo?, huyetApTamThu?, huyetApTamTruong?, nhipTho?, canNang?, chieuCao? }` (huyết áp gửi cả 2 số; trẻ dưới 72 tháng có kê thuốc thì bắt buộc cân nặng); `maIcdChinh` lấy từ gợi ý ICD-10. Dòng thuốc `{ idThuoc, lieuDung?, soLanMoiNgay?, soNgayDung, soLuong, ghiChuSuDung? }`: `soNgayDung` tối đa 30 ngày, 90 khi mã ICD-10 chính là bệnh mạn tính (Thông tư 26/2025), `soLuong` = tổng số lượng cấp; chỉ thuốc trong danh mục, đang dùng và đã xác minh (bác sĩ không thêm thuốc mới). Khoảng giá trị và lỗi: `docs/BAN-GIAO-FRONTEND-05-10.md` mục 3. `PUT` sửa kết quả đã ghi (thay toàn bộ đơn thuốc), bắt buộc thêm `lyDoSua` và `phienBan`, chỉ trong `RECORD_EDIT_DAYS` ngày kể từ lần lưu đầu; `GET` xem lại; gợi ý thuốc theo tên hoặc hoạt chất. Hồ sơ khám ở booking-service hiện `ketQua` và `lanKhamTruoc[].ketQua` kèm `donThuoc[]` |
| `GET /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an/lich-su`, `GET /api/medical/quan-tri/lich-hen/{idLichHen}/benh-an/lich-su` | `BAC_SI`, `QUAN_TRI_VIEN` | Lịch sử sửa hồ sơ bệnh án (ai sửa, lúc nào, lý do, phần đã đổi, nội dung trước / sau). Bác sĩ: lượt khám của mình hoặc của bệnh nhân mà mình có lịch hẹn từ giờ khám đó trở đi; quản trị viên: mọi lượt khám. Bệnh nhân chỉ thấy `ngaySuaCuoi` |
| `GET /api/booking/bac-si/toi/hang-doi?ngay=`, `POST .../hang-doi/goi-tiep?idLichLamViec=`, `POST .../lich-hen/{id}/goi-kham`, `POST .../lich-hen/{id}/vang-mat` | `BAC_SI` | Hàng đợi khám trong ngày theo ca (sáng / chiều / tối): gọi bệnh nhân tiếp theo hoặc 1 người, đánh dấu vắng mặt (gọi lại được); ghi kết quả khám là xong lượt. Chỉ 1 người đang khám mỗi lúc (409 `DANG_KHAM_BENH_NHAN_KHAC`); chỉ thao tác trong ngày khám. Mỗi thao tác trả lại hàng đợi hôm nay |
| `GET /api/medical/bac-si/toi/icd10?tuKhoa=` | `BAC_SI` | Gợi ý mã ICD-10 cho chẩn đoán chính: tối đa 20 `{ ma, ten, manTinh }` theo mã, khớp đầu mã hoặc một phần tên tiếng Việt (không xét dấu) / tiếng Anh; từ khoá trống -> `[]`. `manTinh` = bệnh thuộc Phụ lục VII Thông tư 26/2025 (thuốc kê tới 90 ngày). Danh mục 191 mã do migration V30 tạo, chỉ đọc |
| `POST /api/booking/bac-si/toi/lich-hen/{id}/huy` | `BAC_SI` | Bác sĩ hủy 1 lịch `DA_XAC_NHAN` của mình trước giờ khám: `{ lyDo }` bắt buộc (tối đa 500 ký tự) -> `BI_TU_CHOI`, lượt khám được mở lại cho người khác đặt, bệnh nhân nhận thông báo + email và đọc được lý do trên phiếu khám. Trả lại dòng lịch hẹn. Màn hình Upcoming Appointments liệt kê lịch sắp tới bằng `GET .../lich-hen/lich` (thay 3 route xác nhận / từ chối đã bỏ từ 15/10/2026) |
| `PUT /api/booking/bac-si/toi/lich-hen/{id}/benh-nhan`, `POST .../lich-hen/{id}/da-doi-chieu` | `BAC_SI` | Sửa hồ sơ bệnh nhân của 1 lịch hẹn của mình sau khi đối chiếu giấy tờ (body như của quản trị viên); bỏ đánh dấu `canDoiChieu` |
| `GET /api/booking/quan-tri/lich-lam-viec?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=&idPhongKham=`, `GET .../lich-lam-viec/{id}/lich-hen`, `GET /api/booking/quan-tri/lich-hen/tra-cuu?ma=` | `QUAN_TRI_VIEN` | Lịch làm việc toàn viện (chỉ xem, tối đa 42 ngày), lịch hẹn của 1 ca, tra lịch hẹn theo mã. Mỗi lịch hẹn (cả ở danh sách của bác sĩ) có `idHoSoBenhNhan`, `benhNhan` (dữ liệu của hồ sơ) và `doiChieu` {`canDoiChieu`, `hoTenDaNhap`, `ngaySinhDaNhap`, `gioiTinhDaNhap`, `hoTenGiamHoDaNhap`} |
| `POST /api/booking/quan-tri/lich-hen/{id}/da-doi-chieu` | `QUAN_TRI_VIEN` | Đã đối chiếu giấy tờ: bỏ đánh dấu `canDoiChieu` của lịch hẹn |
| `GET /api/booking/quan-tri/ho-so-benh-nhan/{id}`, `PUT .../ho-so-benh-nhan/{id}` | `QUAN_TRI_VIEN` | Xem (số CCCD không che) và sửa hồ sơ bệnh nhân bất kỳ: `hoTen`, `ngaySinh`, `gioiTinh`?, `soDienThoai`, `diaChi`?, `soBaoHiemYTe`?, `cccd`? (chỉ điền được khi hồ sơ chưa có số; đổi số đã có -> 400, số thuộc hồ sơ khác -> 409) |
| `GET /api/booking/quan-tri/ho-so-benh-nhan/cho-xac-minh`, `POST .../ho-so-benh-nhan/{id}/duyet`, `POST .../{id}/tu-choi` | `QUAN_TRI_VIEN` | Hồ sơ bệnh nhân chờ xác minh liên kết với tài khoản: duyệt hoặc từ chối (cặp đã từ chối không tự vào hàng chờ lại) |
| `GET /api/booking/quy-tac-dat-lich` | Công khai | 5 quy tắc bệnh nhân cần biết (đặt trước tối thiểu / tối đa, hạn hủy / đổi, số lần đổi, số lịch còn hiệu lực của 1 người khám) theo cấu hình hiện hành |
| `GET /api/booking/ngay-nghi?tuNgay=&denNgay=` | Công khai | Ngày bệnh viện nghỉ `{ ngay, lyDo }` (lý do theo ngôn ngữ), mặc định từ hôm nay 1 năm, tối đa 366 ngày |
| `GET /api/booking/phieu-kham/{maPhieuKham}/lich.ics` | Công khai | Tệp lịch `lich-kham-<ECL>.ics` (`text/calendar`) của trạng thái hiện tại, nhắc trước 1 giờ, không có họ tên / SĐT / CCCD. Cả chuỗi đổi lịch là 1 sự kiện (UID = mã tra cứu đầu chuỗi, SEQUENCE tăng theo lần đổi / trạng thái); phiếu cũ chỉ cho tệp "đã hủy". Mã sai -> 404 |
| `GET /api/catalog/bac-si?...&ngay=&buoi=SANG\|CHIEU\|TOI` | Công khai | (DOC-03) Chỉ bác sĩ còn ít nhất 1 lượt đặt được vào ngày / buổi đó (buổi theo giờ bắt đầu lượt: trước 12h, 12h-17h, từ 17h), theo khoảng đặt trước của cấu hình hiện hành; catalog-service đọc ca và cấu hình thẳng từ DB chung. Ngày ngoài khoảng -> trang rỗng; `buoi` sai -> 400 |
| `GET /api/booking/so-kham/{maSoKham}`, `POST .../so-kham/{maSoKham}/xem` `{ bonSoCuoi }`, `GET .../so-kham/{maSoKham}/qr` | Công khai | Sổ khám bệnh (EXAM-03) bằng mã riêng của hồ sơ bệnh án (khác mã phiếu khám, quy tắc #8): chỉ có link thì xem được tóm tắt đã che (họ tên "N. V. An", năm sinh, ngày giờ khám, bác sĩ, chuyên khoa, phòng); kết quả khám đầy đủ khi `bonSoCuoi` khớp 4 số cuối SĐT liên hệ (người giám hộ nếu có). Sai -> 403 `SO_DIEN_THOAI_KHONG_KHOP`; sai `app.so-kham.so-lan-sai-toi-da` (5) lần -> 429 trong 15 phút (Redis `so-kham:sai:{id}`, Redis lỗi thì bỏ qua). Link = `FRONTEND_URL` + `app.so-kham.duong-dan` (`/so-kham/`) + mã; bệnh nhân thấy `maSoKham` / `linkSoKham` ở `ketQua` (lịch sử khám, chi tiết lịch hẹn), bác sĩ ở `BenhAnResponse` |
| `POST /api/booking/cap-lai-lien-ket` | Công khai | (PAT-03) Khách mất link: `{ maTraCuu, soDienThoai }` -> luôn 200 cùng 1 câu; khớp thì link phiếu khám (và sổ khám) được gửi tới email của lịch hẹn, không bao giờ trả trên màn hình. Mỗi mã `RELINK_CODE_LIMIT` (3), mỗi IP `RELINK_IP_LIMIT` (10) lần trong `RELINK_WINDOW` (1h) -> 429 |
| `GET /api/booking/bac-si/toi/lich-hen/theo-cccd?cccd=`, `POST /api/booking/bac-si/toi/lich-hen/{id}/cap-lai-lien-ket` | `BAC_SI` | Tìm lịch của mình theo CCCD người đến xuất trình (bệnh nhân hoặc người giám hộ, tối đa 50); cấp lại link `{ cccd, cachGui: EMAIL \| MAN_HINH }` như quản trị viên (bên dưới). Lịch của bác sĩ khác -> 404 |
| `GET /api/booking/quan-tri/lich-hen?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=&idPhongKham=&trangThai=&kenhDat=&canDoiLich=&canDoiChieu=&tuKhoa=&trang=&kichThuoc=`, `GET .../quan-tri/lich-hen/{id}` | `QUAN_TRI_VIEN` | (ADM-04) Lịch hẹn toàn viện theo giờ khám (mặc định 31 ngày từ hôm nay, tối đa 366), `tuKhoa` = mã ECL, CCCD 12 số hoặc 1 phần họ tên / SĐT / email. Chi tiết: email liên hệ, tài khoản đặt, ngôn ngữ, `quanTriHuyDoiDuoc`, chuỗi đổi lịch và nhật ký `lich_su_lich_hen` (đặt, đổi, hủy, ca bị hủy, đổi bác sĩ, ca được sửa, cấp lại link: ai, lúc nào, lý do; có từ 20/10/2026, không có dữ liệu cũ) |
| `POST /api/booking/quan-tri/lich-hen/{id}/huy` `{ lyDo }`, `POST .../{id}/doi-lich` `{ idLichLamViec \| idChuyenKhoa, gioBatDauKhung, lyDo }` | `QUAN_TRI_VIEN` | Hủy (`QUAN_TRI_HUY`) / đổi lịch (201, lịch mới) thay bệnh nhân, lý do bắt buộc; không theo hạn hủy / đổi và số lần đổi của bệnh nhân, khung giờ mới theo quy tắc đặt lịch. Đã quá giờ khám -> 409 `LICH_HEN_DA_QUA_GIO` (trừ lịch `canDoiLich`). Bệnh nhân nhận thông báo + email theo ngôn ngữ của lịch hẹn, bác sĩ được báo |
| `POST /api/booking/quan-tri/lich-hen` | `QUAN_TRI_VIEN` | Đặt lịch qua tổng đài / tại quầy: body như khách đặt + `ngonNgu` (`VI` / `EN`, của người gọi); cùng quy tắc (CCCD, người giám hộ, trùng giờ, giới hạn theo hồ sơ / SĐT), không giới hạn theo IP. 201 `{ idLichHen, phieuKham }`: phản hồi duy nhất cho nhân viên thấy mã phiếu khám (in phiếu / QR). `kenhDat = HOTLINE`, không gắn tài khoản |
| `POST /api/booking/quan-tri/lich-hen/{id}/cap-lai-lien-ket` | `QUAN_TRI_VIEN` | (PAT-03) `{ cccd, cachGui }`: `cccd` của bệnh nhân hoặc người giám hộ trên lịch hẹn (sai -> 403 `CCCD_KHONG_KHOP`). `EMAIL`: gửi tới email của lịch hẹn, trả địa chỉ đã che (không có email -> 409 `KHONG_CO_EMAIL_NHAN`); `MAN_HINH`: trả mã + link phiếu khám (và sổ khám) để mở QR. Dùng lại mã cũ, luôn cho lịch hiện hành của chuỗi đổi lịch; ghi nhật ký `CAP_LAI_LIEN_KET` |
| `GET` / `PUT /api/booking/quan-tri/cau-hinh`, `GET .../cau-hinh/lich-su` | `QUAN_TRI_VIEN` | (SCHED-05, quy tắc #13) 10 quy tắc đặt lịch / xếp ca, `PUT` gửi đủ 10 giá trị + `phienBan` (cũ -> 409 `DU_LIEU_DA_THAY_DOI`; ngoài khoảng -> 400); áp dụng ngay cho thao tác mới. Lịch sử: mỗi trường đổi 1 dòng (cũ, mới, ai, lúc nào) |
| `GET /api/booking/quan-tri/ngay-nghi`, `POST .../ngay-nghi/xem-truoc`, `POST .../ngay-nghi`, `DELETE .../ngay-nghi/{ngay}` | `QUAN_TRI_VIEN` | Ngày bệnh viện nghỉ: `{ tuNgay, denNgay, lyDo, lyDoEn? }` từ ngày mai, tối đa 42 ngày; xem trước rồi lưu (201) hủy các ca còn hoạt động của những ngày đó theo quy trình "cần đổi lịch" (bệnh nhân được báo + email, mỗi bác sĩ 1 thông báo; lịch sử ca nguồn `NGAY_NGHI`); bỏ được ngày nghỉ chưa tới (ca đã hủy không khôi phục) |

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
| 409 | `LICH_HEN_KHONG_HUY_DOI_DUOC`, `LICH_HEN_CAN_DOI_LICH`, `LICH_HEN_DA_QUA_GIO` | (Bác sĩ hủy lịch) Lịch hẹn đã khám hoặc đã hủy (kể cả khi bấm lần thứ hai); ca đã bị hủy, lịch chờ bệnh nhân đổi; lượt khám đã bắt đầu. `LICH_HEN_KHONG_CHO_XAC_NHAN` không còn được trả từ 15/10/2026 |
| 409 | `LICH_HEN_KHONG_HUY_DOI_DUOC`, `QUA_HAN_HUY_DOI_LICH`, `VUOT_SO_LAN_DOI_LICH` | (Bệnh nhân hủy / đổi lịch) Lịch hẹn đã khám, đã hủy, đã đổi, bác sĩ đã hủy hoặc không đến (`KHONG_DEN`); còn cách giờ khám ít hơn hạn hủy / đổi; lần đặt này đã đổi đủ số lần |
| 400 | `DU_LIEU_KHONG_HOP_LE` | (Khám bệnh, `thongDiep` không có `chiTiet`) Mã ICD-10 không có trong danh mục; huyết áp thiếu 1 số hoặc tâm trương không thấp hơn tâm thu; thuốc quá 30 ngày (90 với mã mạn tính); trẻ dưới 72 tháng có kê thuốc mà thiếu cân nặng |
| 403 | `SO_DIEN_THOAI_KHONG_KHOP` | (Hủy / đổi bằng link phiếu khám) SĐT nhập vào không phải SĐT liên hệ của lượt khám |
| 409 | `CHUA_DEN_NGAY_KHAM`, `LICH_HEN_KHONG_KHAM_DUOC` | (Khám bệnh) Bác sĩ ghi kết quả khám trước ngày khám; lịch hẹn đã khám xong, đã huỷ, bác sĩ đã hủy hoặc không đến |
| 404 | `KHONG_TIM_THAY` | Ca khám không tồn tại; mã phiếu khám sai |
| 409 | `LICH_HEN_KHONG_HUY_DOI_DUOC`, `LICH_HEN_DA_QUA_GIO` | (Quản trị viên hủy / đổi lịch) Lịch không còn `DA_XAC_NHAN`; lượt khám đã bắt đầu (trừ lịch `canDoiLich`) |
| 409 | `NGAY_BENH_VIEN_NGHI` | Xếp ca / duyệt đổi ca / bác sĩ xin đổi ca vào ngày bệnh viện nghỉ (thông điệp nêu ngày và lý do); khai báo nghỉ mà mọi ngày đều đã nghỉ |
| 403 | `CCCD_KHONG_KHOP` | (Cấp lại link) CCCD xuất trình không phải của bệnh nhân hay người giám hộ trên lịch hẹn |
| 409 | `KHONG_CO_EMAIL_NHAN` | (Cấp lại link qua email) Lịch hẹn không có email nhận |
| 409 | `DU_LIEU_DA_THAY_DOI` | (Sửa quy tắc đặt lịch) `phienBan` đã cũ: người khác vừa lưu |
| 403 / 429 | `SO_DIEN_THOAI_KHONG_KHOP` / `GUI_LAI_QUA_NHANH` | (Sổ khám bệnh) Sai 4 số cuối SĐT / sai quá số lần; (khách xin cấp lại link) quá số lần theo mã hoặc theo IP |

Cấu hình: **quy tắc đặt lịch / xếp ca nằm ở bảng `cau_hinh_he_thong`** (migration V33), quản trị viên sửa ở
`/api/booking/quan-tri/cau-hinh` và có hiệu lực ngay cho thao tác mới. `BOOKING_MIN_LEAD` (30m), `BOOKING_MAX_DAYS` (30),
`BOOKING_MAX_ACTIVE_PER_PATIENT` (3), `BOOKING_MAX_ACTIVE_PER_PHONE` (5), `BOOKING_CHANGE_LEAD` (2h), `BOOKING_MAX_RESCHEDULES`
(2), `SHIFT_REQUEST_LEAD`, `SHIFT_MAX_DAYS`, `SHIFT_MAX_WEEKLY_HOURS`, `RATING_EDIT_DAYS` **chỉ ghi giá trị ban đầu** của bảng
lần đầu booking-service chạy; sau đó giá trị trong bảng thắng (log WARN khi biến môi trường khác bảng). Vẫn là biến môi
trường: `BOOKING_IP_LIMIT` (20) trong `BOOKING_IP_WINDOW` (10m), `RELINK_CODE_LIMIT` (3) / `RELINK_IP_LIMIT` (10) trong
`RELINK_WINDOW` (1h), `GATEWAY_TRUSTED_PROXIES`; link phiếu khám dựng từ `FRONTEND_URL`, link sổ khám từ `FRONTEND_URL` +
`app.so-kham.duong-dan` (`/so-kham/`, `application-common.yml`; khoá xem sau `so-lan-sai-toi-da` 5 lần sai trong `khoa-sau`
15 phút). Rủi ro và việc cần làm trước khi triển khai thật:
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

Tài liệu API cho frontend: [docs/api/](docs/api/) (mỗi vai trò 1 tài liệu). Ghi chú cũ theo từng màn hình:
[docs/BAN-GIAO-FRONTEND-05-10.md](docs/BAN-GIAO-FRONTEND-05-10.md).

## Ca làm việc và yêu cầu đổi ca / xin nghỉ

| API | Ai gọi | Ghi chú |
|---|---|---|
| `POST /api/booking/quan-tri/lich-lam-viec` | `QUAN_TRI_VIEN` | Xếp ca: `{ idBacSi, idPhongKham, ngay, gioBatDau, gioKetThuc, soLuotToiDaMoiGio (N), thoiLuongLuotPhut (t), lapLai?: { cacThu: [1..7], denNgay } }` (`cacThu`: 1 = thứ Hai ... 7 = Chủ nhật). 201 `{ daTao: [ca], boQua: [{ ngay, lyDo }] }`. Các lượt khám được sinh theo N, t (`N x t <= 60`). Phòng phải đang hoạt động và thuộc chuyên khoa của bác sĩ; ca phải bắt đầu sau hiện tại và trong `SHIFT_MAX_DAYS` (90) ngày tới. Trùng giờ với ca khác của bác sĩ hoặc của phòng → 409 `TRUNG_LICH_LAM_VIEC`; có `lapLai` thì ngày trùng bị bỏ qua và nằm trong `boQua` |
| `PUT /api/booking/quan-tri/lich-lam-viec/{id}` | `QUAN_TRI_VIEN` | Sửa `{ idPhongKham, gioBatDau, gioKetThuc, soLuotToiDaMoiGio, thoiLuongLuotPhut }` (gửi đủ 5 giá trị; bác sĩ và ngày không sửa được). Phòng đổi được bất cứ lúc nào: lịch hẹn còn hiệu lực mang phòng mới, bệnh nhân được báo. Giờ / sức chứa chỉ đổi được khi mọi lượt đã có người đặt còn nguyên giờ; không thì 409 `CA_CON_LICH_HEN` và không có gì thay đổi. Ca đã bắt đầu hoặc đã hủy → 409 `CA_KHONG_SUA_DUOC` |
| `POST /api/booking/quan-tri/lich-lam-viec/{id}/huy` | `QUAN_TRI_VIEN` | `{ lyDo }` bắt buộc. Trả `{ ca, soLichHenCanDoi }`. Ca và mọi lượt khám thành `DA_HUY`; lịch hẹn còn hiệu lực của ca **giữ nguyên trạng thái** và có `canDoiLich = true`; bệnh nhân và bác sĩ được báo. Yêu cầu đang chờ của ca (`CHO_DUYET`, cả nhường ca còn `CHO_DONG_Y`) bị đóng (`TU_CHOI` kèm ghi chú); bác sĩ gửi và bác sĩ được nhường đều được báo |
| `GET /api/booking/quan-tri/lich-lam-viec/can-doi-lich?trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Lịch hẹn còn hiệu lực đang chờ bệnh nhân đổi lịch, giờ khám cũ sớm nhất trước (để phòng khám gọi điện cho khách đặt không đăng nhập) |
| `POST /api/booking/bac-si/toi/yeu-cau-doi-lich` | `BAC_SI` | `{ idLichLamViec, loaiYeuCau: XIN_NGHI \| DOI_CA \| NHUONG_CA, lyDo, ngayMongMuon?, gioBatDauMongMuon?, gioKetThucMongMuon?, idPhongKhamMongMuon?, idBacSiNhan? }` → 201. 3 trường ngày / giờ bắt buộc với `DOI_CA`. Chỉ ca còn hoạt động của chính bác sĩ (ca của người khác → 404), còn cách giờ bắt đầu ít nhất `SHIFT_REQUEST_LEAD` (24 giờ) → không thì 409 `QUA_HAN_GUI_YEU_CAU`. Mỗi ca 1 yêu cầu còn chờ (`CHO_DONG_Y` hoặc `CHO_DUYET`) → 409 `CA_DA_CO_YEU_CAU_CHO_DUYET`. **Nhường ca** (`NHUONG_CA`): `idBacSiNhan` là 1 bác sĩ khác, cùng chuyên khoa, đang công tác (không thì 400, thông điệp nêu điều này); bác sĩ nhận trùng giờ hoặc nhận thêm ca thì vượt giờ tuần → 409 `TRUNG_LICH_LAM_VIEC` / `VUOT_GIO_LAM_TUAN`. Yêu cầu thành `CHO_DONG_Y`, bác sĩ nhận được báo (`YEU_CAU_NHUONG_CA`); quản trị viên chỉ thấy và xử lý sau khi bác sĩ nhận đồng ý. Danh sách đồng nghiệp: `GET /api/catalog/bac-si?idChuyenKhoa=` (chuyên khoa lấy từ `GET /api/catalog/bac-si/toi/ho-so`) |
| `GET /api/booking/bac-si/toi/yeu-cau-doi-lich?trangThai=&trang=&kichThuoc=`, `POST .../yeu-cau-doi-lich/{id}/rut` | `BAC_SI` | Yêu cầu tôi đã gửi (mới nhất trước) và rút yêu cầu còn chờ, `CHO_DONG_Y` hoặc `CHO_DUYET` (`DA_RUT`; sau đó ca nhận được yêu cầu mới; rút nhường ca thì bác sĩ nhận được báo). Mỗi dòng: `{ id, loaiYeuCau, trangThai, lyDo, bacSi, ca, ngayMongMuon, gioBatDauMongMuon, gioKetThucMongMuon, phongKhamMongMuon, bacSiNhan, ghiChuBacSiNhan, ngayBacSiNhanPhanHoi, maNhom, ghiChuXuLy, ngayGui, ngayXuLy }`; `bacSi` là bác sĩ gửi, `ca` như 1 dòng lịch làm việc (có `soLuotDaDat`); `bacSiNhan`, `ghiChuBacSiNhan` (lý do đồng nghiệp không nhận), `ngayBacSiNhanPhanHoi` chỉ có ở nhường ca; `maNhom` chỉ có ở xin nghỉ nhiều ca |
| `GET /api/booking/quan-tri/yeu-cau-doi-lich?trangThai=&tatCa=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Yêu cầu của mọi bác sĩ, gửi sớm nhất trước; mặc định `CHO_DUYET` (nhường ca chỉ vào đây sau khi bác sĩ nhận đồng ý), `tatCa=true` lấy mọi trạng thái. Cùng dạng dòng như trên |
| `POST /api/booking/quan-tri/yeu-cau-doi-lich/{id}/duyet`, `POST .../{id}/tu-choi` | `QUAN_TRI_VIEN` | Body `{ ghiChu }`, bắt buộc khi từ chối. Duyệt `XIN_NGHI` = hủy ca. Duyệt `DOI_CA`: chỉ khác phòng thì đổi phòng tại chỗ; còn lại thì hủy ca cũ và xếp ca mới theo ngày / giờ / phòng mong muốn (giữ N, t). Duyệt `NHUONG_CA`: ca và mọi lịch hẹn còn hiệu lực của ca chuyển sang bác sĩ nhận (giờ, phòng, số thứ tự không đổi; lịch hẹn có `daDoiBacSi`, bệnh nhân nhận thông báo `LICH_HEN_DOI_BAC_SI` và email); lịch sử ca ghi `SUA` với `cacTruongDoi: ["bacSi"]`; cả 2 bác sĩ được báo. Ca mới / bác sĩ nhận trùng giờ hoặc vượt giờ tuần → 409 `TRUNG_LICH_LAM_VIEC` / `VUOT_GIO_LAM_TUAN`, yêu cầu vẫn chờ duyệt. Nhường ca còn `CHO_DONG_Y` → 409 `YEU_CAU_CHO_DONG_NGHIEP`. Đã xử lý / đã rút → 409 `YEU_CAU_DA_XU_LY` |
| `GET /api/booking/quan-tri/lich-lam-viec/gio-lam-theo-tuan?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=` | `QUAN_TRI_VIEN` | Giờ làm của từng bác sĩ theo tuần thứ Hai - Chủ nhật (khoảng tối đa 42 ngày, nới ra đủ tuần): `{ gioiHanPhut, bacSi: [{ bacSi, tenChuyenKhoa, cacTuan: [{ tuNgay, denNgay, soCa, soPhut, soPhutConLai }] }] }`, chỉ ca còn hoạt động. `soPhutConLai` âm = đã vượt. Form xếp ca gọi với `idBacSi` và tuần của ngày đã chọn để hiện giờ đã xếp |
| `GET /api/booking/quan-tri/lich-lam-viec/{id}/lich-su` | `QUAN_TRI_VIEN` | Lịch sử của 1 ca, cũ nhất trước: `[{ id, hanhDong, nguon, thoiDiem, quanTri { id, hoTen }, idYeuCau, lyDo, truoc, sau, cacTruongDoi }]`. `hanhDong`: `TAO`, `SUA`, `HUY`. `nguon`: `QUAN_TRI`, `DUYET_YEU_CAU` (có `idYeuCau`), `NGUNG_CONG_TAC`, `SAO_CHEP`, `NHAP_EXCEL`, `DU_LIEU_MAU`, `DU_LIEU_CU` (ca có từ trước khi có lịch sử: 1 dòng `TAO` tạo lúc nâng cấp). `truoc` / `sau` = `{ bacSi, phongKham, ngay, gioBatDau, gioKetThuc, soLuotToiDaMoiGio, thoiLuongLuotPhut }` (`truoc` null ở `TAO`); `cacTruongDoi` liệt kê tên trường đổi ở `SUA` |
| `POST /api/booking/quan-tri/lich-lam-viec/sao-chep/xem-truoc`, `POST /api/booking/quan-tri/lich-lam-viec/sao-chep` | `QUAN_TRI_VIEN` | Sao chép các ca còn hoạt động của khoảng nguồn sang khoảng khác: `{ tuNgay, denNgay, ngayDich, soLan, idChuyenKhoa?, cacIdBacSi? }`. Nguồn tối đa 42 ngày; `ngayDich` sau `denNgay` và cùng thứ với `tuNgay`; `soLan` 1-12, mỗi lần dời thêm số tuần đủ phủ khoảng nguồn; tối đa 1000 bản sao. Trả `{ daLuu, soCaNguon, taoDuoc: [ca], boQua: [ca] }`, mỗi `ca` = `{ idCaNguon, idCaMoi, bacSi, phongKham, ngay, gioBatDau, gioKetThuc, lyDo }`. Bản sao trùng giờ, vượt giờ tuần, ngày đã qua / quá `SHIFT_MAX_DAYS`, bác sĩ / phòng ngừng hoạt động thì vào `boQua` kèm `lyDo`. `xem-truoc` (200) chạy y hệt rồi hoàn tác (`daLuu: false`); `sao-chep` (201) lưu, mỗi bác sĩ 1 thông báo `CA_LAM_VIEC_THAY_DOI`, lịch sử `SAO_CHEP`. 400 khi: khác thứ, `ngayDich` không sau nguồn, nguồn không có ca, quá 1000 bản sao, hoặc lưu mà không bản sao nào tạo được |
| `GET /api/booking/quan-tri/lich-lam-viec/xuat-excel?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=&idPhongKham=`, `GET /api/booking/quan-tri/lich-lam-viec/nhap-excel/mau` | `QUAN_TRI_VIEN` | Tải `.xlsx`: ca còn hoạt động của khoảng (tối đa 42 ngày, cùng bộ lọc như lịch toàn viện), tên tệp `lich-lam-viec_<tuNgay>_<denNgay>.xlsx`; và tệp mẫu `mau-nhap-lich-lam-viec.xlsx` (trang ca, hướng dẫn, danh sách bác sĩ đang công tác và phòng đang hoạt động; cột mã bác sĩ / mã phòng có danh sách chọn). Cột A-G của cả hai: mã bác sĩ (`BS0001`), ngày, giờ bắt đầu, giờ kết thúc, mã phòng (id), N, t; cột sau G của tệp xuất chỉ để xem. Chữ theo `Accept-Language` |
| `POST /api/booking/quan-tri/lich-lam-viec/nhap-excel/xem-truoc`, `POST /api/booking/quan-tri/lich-lam-viec/nhap-excel` | `QUAN_TRI_VIEN` | Multipart, phần tệp tên `tep` (`.xlsx`, tối đa 2 MB → 413 `TEP_QUA_LON`; tối đa 1000 dòng ca). Trả `{ daLuu, soDong, hopLe: [{ dong, idCaMoi, bacSi, phongKham, ngay, gioBatDau, gioKetThuc }], loi: [{ dong, cot, lyDo }] }`. Ngày / giờ là ô ngày giờ của Excel hoặc chữ `yyyy-MM-dd` / `dd/MM/yyyy`, `HH:mm`. Lỗi theo dòng / cột: ô sai, mã bác sĩ / phòng không có hoặc ngừng hoạt động, phòng khác chuyên khoa, N x t, trùng giờ trong tệp hoặc với ca đã có, vượt giờ tuần, ngày đã qua / quá xa. `xem-truoc` (200) không lưu; `nhap-excel` (201) lưu tất cả hoặc không: còn lỗi → 400 (thông điệp nêu số lỗi), tệp hỏng / rỗng / quá 1000 dòng → 400. Mỗi bác sĩ 1 thông báo, lịch sử `NHAP_EXCEL`. Tệp xuất ra sửa rồi nhập lại được |
| `GET /api/booking/quan-tri/lich-lam-viec/pdf/bac-si/{idBacSi}?tuNgay=&denNgay=`, `GET /api/booking/quan-tri/lich-lam-viec/pdf/toan-vien?tuNgay=&denNgay=&idChuyenKhoa=` | `QUAN_TRI_VIEN` | Bản in PDF (khoảng tối đa 42 ngày, chỉ ca còn hoạt động, chữ theo `Accept-Language`): lịch 1 bác sĩ (A4 dọc, cùng tệp bác sĩ tự tải, tên `lich-lam-viec_<mã bác sĩ>_<tuNgay>_<denNgay>.pdf`); lưới toàn viện (A4 ngang, mỗi tuần thứ Hai - Chủ nhật 1 trang, bác sĩ theo chuyên khoa, cột cuối giờ làm trong tuần, tên `lich-lam-viec-toan-vien_<tuNgay>_<denNgay>.pdf`). Bác sĩ không có → 404 |
| `POST /api/booking/quan-tri/lich-lam-viec/gui-lich` | `QUAN_TRI_VIEN` | `{ tuNgay, denNgay, idChuyenKhoa?, cacIdBacSi? }` (khoảng tối đa 42 ngày, tối đa 200 bác sĩ). Mỗi bác sĩ có ca còn hoạt động từ hôm nay trong khoảng nhận 1 email liệt kê ca (tối đa 100 dòng, theo `ngonNgu` của tài khoản, nút mở `FRONTEND_URL` + `DOCTOR_SCHEDULE_PATH?tuNgay=&denNgay=`) và 1 thông báo `CA_LAM_VIEC_THAY_DOI`. Trả `{ soBacSi, soCa, cacBacSi: [{ bacSi, soCa, soPhut }] }`. Không có ca nào → 400. Gửi lại thì gửi thêm lần nữa |
| `GET /api/booking/bac-si/toi/lich-lam-viec/pdf?tuNgay=&denNgay=` | `BAC_SI` | Bản in PDF lịch của chính bác sĩ, như bản quản trị viên tải cho 1 bác sĩ (tài liệu API thứ 2, mục 5.5). Màn hình nhận `tuNgay` / `denNgay` từ đường dẫn của email gửi lịch thì điền sẵn 2 ngày này |
| `POST /api/booking/bac-si/toi/yeu-cau-doi-lich/xin-nghi-nhieu-ca` | `BAC_SI` | `{ cacIdLichLamViec: [id] (1-50), lyDo }` → 201, danh sách yêu cầu `XIN_NGHI` vừa tạo (mỗi ca 1 yêu cầu `CHO_DUYET`, chung `maNhom`), theo ngày giờ ca. Mỗi ca theo đúng luật của yêu cầu lẻ; 1 ca không gửi được thì không lưu gì và lỗi mang mã của ca đó, thông điệp "Ca <ngày giờ>: <lý do>". Quản trị viên nhận 1 thông báo cho cả nhóm |
| `POST /api/booking/bac-si/toi/yeu-cau-doi-lich/nhom/{maNhom}/rut` | `BAC_SI` | Rút mọi yêu cầu còn chờ của 1 nhóm của tôi; trả các yêu cầu vừa rút. Nhóm không có / của bác sĩ khác → 404; không còn yêu cầu chờ → 409 `YEU_CAU_DA_XU_LY` |
| `GET /api/booking/bac-si/toi/yeu-cau-nhuong-ca?trangThai=&trang=&kichThuoc=` | `BAC_SI` | Yêu cầu nhường ca gửi cho tôi, mới nhất trước (`trangThai=CHO_DONG_Y`: đang chờ tôi). Cùng dạng dòng như trên |
| `POST /api/booking/bac-si/toi/yeu-cau-nhuong-ca/{id}/dong-y`, `POST .../{id}/tu-choi` `{ ghiChu? }` | `BAC_SI` | Bác sĩ nhận đồng ý: yêu cầu thành `CHO_DUYET`, quản trị viên nhận `YEU_CAU_DOI_LICH_MOI`, bác sĩ gửi được báo; ca đã bắt đầu / đã hủy → 409 `CA_KHONG_SUA_DUOC`; tôi trùng giờ / vượt giờ tuần → 409 `TRUNG_LICH_LAM_VIEC` / `VUOT_GIO_LAM_TUAN`. Từ chối: `DONG_NGHIEP_TU_CHOI`, ghi chú (không bắt buộc) lưu ở `ghiChuBacSiNhan`, bác sĩ gửi được báo. Không phải yêu cầu gửi tôi → 404; không còn `CHO_DONG_Y` → 409 `YEU_CAU_DA_XU_LY` |
| `POST /api/booking/quan-tri/yeu-cau-doi-lich/nhom/{maNhom}/duyet`, `POST .../nhom/{maNhom}/tu-choi` | `QUAN_TRI_VIEN` | Body `{ ghiChu }`, bắt buộc khi từ chối. Xử lý mọi yêu cầu `CHO_DUYET` của nhóm, trả `{ daXuLy: [yêu cầu], boQua: [{ idYeuCau, lyDo }] }`; bác sĩ nhận 1 thông báo cho cả nhóm. Duyệt: ca đã bắt đầu thì bỏ qua (vào `boQua`); mọi ca đều bỏ qua → 409 `CA_KHONG_SUA_DUOC`. Nhóm không có → 404; không còn yêu cầu chờ → 409 `YEU_CAU_DA_XU_LY` |
| `GET /api/catalog/phong-kham?idChuyenKhoa=` | `QUAN_TRI_VIEN`, `BAC_SI` | Phòng khám đang hoạt động: `{ id, tenPhong, tang, idChuyenKhoa, tenChuyenKhoa }`. Chỉ đọc |

**Lịch hẹn của ca bị hủy (`canDoiLich`).** Phiếu khám, dòng "lịch hẹn của tôi" và dòng lịch hẹn của bác sĩ / quản trị viên có
`canDoiLich`. Khi `true`: lịch hẹn vẫn `DA_XAC_NHAN` nhưng ca đã hủy, bệnh nhân phải đổi sang khung giờ khác
hoặc hủy. Với lịch này `duocHuyDoi` đúng tới giờ khám cũ (`hanHuyDoi` = giờ khám, không trừ 2 giờ) và lần đổi không tính vào
giới hạn 2 lần; lịch mới sau khi đổi không còn cờ. Bác sĩ không thấy lịch này trong Upcoming Appointments và không hủy được
(409 `LICH_HEN_CAN_DOI_LICH`); lịch này cũng không bị chuyển thành `KHONG_DEN`.

**Lịch hẹn của ca được nhường (`daDoiBacSi`).** Phiếu khám và dòng "lịch hẹn của tôi" có `daDoiBacSi`. Khi `true`: quản trị
viên đã duyệt cho bác sĩ cũ nhường ca, `bacSi` là bác sĩ mới; giờ, phòng, số thứ tự và mã phiếu không đổi, lịch vẫn khám bình
thường. Bệnh nhân hủy / đổi được tới giờ khám (`hanHuyDoi` = giờ khám) và lần đổi không tính vào giới hạn 2 lần (cột
`lich_hen.doi_bac_si`, migration V32).

**Giới hạn giờ làm trong tuần (SCHED-05).** Tổng giờ các ca còn hoạt động của 1 bác sĩ trong 1 tuần thứ Hai - Chủ nhật không
vượt `SHIFT_MAX_WEEKLY_HOURS` (48; đúng 48 giờ vẫn được), kiểm tra ở mọi cách tạo / đổi ca: xếp, sửa, duyệt đổi ca, đồng ý
và duyệt nhường ca (tính cho bác sĩ nhận), sao chép, nhập Excel.

**Lịch sử ca** (bảng `lich_su_ca_lam_viec`, migration V31): mỗi lần tạo / sửa / hủy ca ghi 1 dòng với người làm, thời điểm,
nguồn, lý do, ảnh ca trước và sau. `hanhDong`: `TAO`, `SUA`, `HUY` (nhường ca là `SUA` với `cacTruongDoi: ["bacSi"]`).
`nguon`: `QUAN_TRI`, `DUYET_YEU_CAU`, `NGUNG_CONG_TAC`, `SAO_CHEP`, `NHAP_EXCEL`, `DU_LIEU_MAU`, `DU_LIEU_CU` (ca có từ trước
V31). Hai cột là VARCHAR: thêm giá trị enum mới không cần migration.

**Thư viện của booking-service:** Apache POI `poi-ooxml` 5.5.1 (Excel, `poi.version` ở pom gốc), OpenPDF 2.0.5 (PDF,
`com.lowagie`, dòng cuối còn chạy Java 17), phông Noto Sans (giấy phép OFL, nhúng vào PDF để có dấu tiếng Việt) ở
`booking-service/src/main/resources/fonts`. Mã bác sĩ `BS0001` (tệp Excel, PDF) = "BS" + id đủ 4 chữ số, chỉ có 1 định
nghĩa ở `common/util/MaBacSi`. Tải lên của booking-service: tệp tối đa 2 MB, request tối đa 3 MB.

**Số thứ tự khám** là thứ hạng của lượt khám trong các lượt của phòng trong ngày, nên khi phòng có thêm ca đứng trước, ca được
kéo dài hoặc ca đổi phòng thì số thứ tự của các lịch hẹn còn hiệu lực của phòng trong ngày đó được tính lại. Luôn hiển thị
`soThuTu` mới nhất từ API, không lưu lại ở frontend.

Lỗi mới (đều 409): `CA_KHONG_SUA_DUOC`, `CA_CON_LICH_HEN`, `CA_DA_CO_YEU_CAU_CHO_DUYET`, `QUA_HAN_GUI_YEU_CAU`,
`YEU_CAU_DA_XU_LY`, `LICH_HEN_CAN_DOI_LICH`, `VUOT_GIO_LAM_TUAN` (từ 19/10/2026), `YEU_CAU_CHO_DONG_NGHIEP` (từ 19/10/2026),
`NGAY_BENH_VIEN_NGHI` (từ 20/10/2026: ngày bệnh viện nghỉ; xếp lặp lại, sao chép, nhập Excel thì ngày đó vào `boQua` / lỗi
theo dòng); tệp nhập quá 2 MB → 413 `TEP_QUA_LON`. Cấu hình: gửi yêu cầu trước ca 24 giờ, xếp ca trước tối đa 90 ngày,
48 giờ / tuần là giá trị ban đầu từ `SHIFT_REQUEST_LEAD`, `SHIFT_MAX_DAYS`, `SHIFT_MAX_WEEKLY_HOURS`; từ 20/10/2026 quản trị
viên sửa ở `/api/booking/quan-tri/cau-hinh` (mục "Đặt lịch khám"). `DOCTOR_SCHEDULE_PATH` (`/doctor/work-schedule`: nút "Xem
lịch làm việc" trong email gửi lịch mở `FRONTEND_URL` + đường dẫn này, kèm `?tuNgay=&denNgay=`). Ngày bệnh viện nghỉ
(bảng `ngay_nghi_benh_vien`, migration V34): khai báo hủy ca qua cùng quy trình "cần đổi lịch", lịch sử ca nguồn `NGAY_NGHI`;
tạo ca giữ khoá dùng chung trên dòng `cau_hinh_he_thong`, khai báo nghỉ giữ khoá riêng, nên ca mới không lọt vào ngày vừa
khai báo.

## Quản lý bác sĩ

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/catalog/quan-tri/bac-si?tuKhoa=&idChuyenKhoa=&trangThai=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Danh bạ bác sĩ, bác sĩ mới thêm đứng trước, **cả bác sĩ ngừng công tác**. `tuKhoa` so với họ tên, email, số điện thoại, số giấy phép. Mỗi dòng: `id`, `maBacSi`, `hoTen`, `email`, `soDienThoai`, `anhDaiDien`, `hocVi`, `soGiayPhep`, `idChuyenKhoa`, `tenChuyenKhoa`, `soLuotDaKham`, `trangThai` (`DANG_CONG_TAC` / `NGUNG_CONG_TAC`), `trangThaiTaiKhoan`, `phaiDoiMatKhau` |
| `POST /api/catalog/quan-tri/bac-si` | `QUAN_TRI_VIEN` | "Add New Doctor": `{ hoTen, email, soDienThoai?, idChuyenKhoa, soGiayPhep?, hocVi?, chucVu?, namBatDauHanhNghe? }` → 201, trả hồ sơ quản trị của bác sĩ. Tạo tài khoản đăng nhập và hồ sơ bác sĩ trong 1 bước. `namBatDauHanhNghe` từ 1950, không sau năm nay; số năm kinh nghiệm suy ra từ đây |
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
  (không qua gateway, header `X-Khoa-Noi-Bo`). Chạy IntelliJ: đặt `IDENTITY_URL`, `BOOKING_URL` trong `.env` nếu không dùng cổng mặc định.

## Quản lý bệnh nhân và danh mục phòng khám, thuốc

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/booking/quan-tri/ho-so-benh-nhan?tuKhoa=&trangThaiLienKet=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Patient Management: danh sách hồ sơ, mới tạo trước, **cả hồ sơ của khách không có tài khoản**. Mỗi dòng: `id`, `hoTen`, `ngaySinh`, `gioiTinh`, `soDienThoai`, `cccdChe` (chỉ còn 4 số cuối), `coTaiKhoan`, `trangThaiLienKet`, `soLichHen`, `ngayTao` |
| `GET /api/booking/quan-tri/ho-so-benh-nhan/{id}/lich-hen?trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Lịch hẹn của 1 hồ sơ, mọi trạng thái, giờ khám muộn nhất trước. Cùng cấu trúc dòng với lịch hẹn của 1 ca (có `bacSi`). Không có chẩn đoán / đơn thuốc |
| `GET /api/catalog/quan-tri/phong-kham?tuKhoa=&idChuyenKhoa=&trangThai=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Medical Catalog: mọi phòng khám theo tên, **cả phòng ngừng hoạt động**: `id`, `tenPhong`, `tang`, `idChuyenKhoa`, `tenChuyenKhoa`, `trangThai` (`HOAT_DONG` / `NGUNG_HOAT_DONG`), `soCaSapToi` |
| `POST /api/catalog/quan-tri/phong-kham`, `PUT .../{id}` | `QUAN_TRI_VIEN` | `{ tenPhong, tang?, idChuyenKhoa }`; thêm → 201. Trùng tên: 409 `TEN_PHONG_DA_TON_TAI` |
| `POST /api/catalog/quan-tri/phong-kham/{id}/ngung-hoat-dong`, `POST .../{id}/hoat-dong-lai` | `QUAN_TRI_VIEN` | Phòng ngừng hoạt động không còn trong `GET /api/catalog/phong-kham` và không xếp ca mới vào được |
| `GET /api/medical/quan-tri/thuoc?tuKhoa=&daXacMinh=&trangThai=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Medical Catalog: danh mục thuốc, chưa xác minh trước rồi theo tên: `id`, `tenThuoc`, `hoatChat`, `hamLuong`, `dangBaoChe(En)`, `duongDung`, `donVi(En)`, `moTa(En)`, `daXacMinh`, `trangThai` (`DANG_DUNG` / `NGUNG_DUNG`), `tenBacSiTao`, `soLanKe` |
| `POST /api/medical/quan-tri/thuoc`, `PUT .../{id}` | `QUAN_TRI_VIEN` | `{ tenThuoc, hoatChat, hamLuong, dangBaoChe?, dangBaoCheEn?, duongDung?, donVi?, donViEn?, moTa?, moTaEn? }` (hoạt chất, hàm lượng bắt buộc; `duongDung` là mã `UONG`, `TIEM`, `BOI_NGOAI_DA`...); thêm → 201, thuốc đã xác minh. Trùng **tên + hàm lượng** (không phân biệt hoa thường / khoảng trắng): 409 `TEN_THUOC_DA_TON_TAI` |
| `POST /api/medical/quan-tri/thuoc/{id}/xac-minh`, `POST .../{id}/ngung-dung`, `POST .../{id}/dung-lai` | `QUAN_TRI_VIEN` | Xác minh thuốc bác sĩ đã tự thêm trước đây (thiếu hoạt chất / hàm lượng → 409 `THUOC_THIEU_THONG_TIN`); cho ngừng dùng / dùng lại. Gọi lại nhiều lần không sao |

- **Tìm hồ sơ bệnh nhân.** `tuKhoa` so với họ tên, số điện thoại, số bảo hiểm y tế (chứa từ khoá). Số CCCD chỉ tra được khi
  gõ **đủ 12 số** (khớp hoàn toàn); gõ 1 phần số CCCD không ra kết quả. Danh sách chỉ trả 4 số cuối của CCCD, số đầy đủ có
  ở `GET /api/booking/quan-tri/ho-so-benh-nhan/{id}`.
- **Phòng khám không xoá được**, chỉ ngừng hoạt động. Phòng còn ca sắp tới (`soCaSapToi > 0`) thì ngừng hoạt động và đổi
  chuyên khoa đều trả 409 `PHONG_CON_CA_LAM_VIEC` (thông điệp có số ca): chuyển các ca đó sang phòng khác bằng
  `PUT /api/booking/quan-tri/lich-lam-viec/{id}` trước. Tên phòng và tầng sửa được bất cứ lúc nào.
- **Bác sĩ chỉ kê thuốc trong danh mục này** (đang dùng, đã xác minh), chọn theo `idThuoc`; bác sĩ không thêm được thuốc
  mới. Dữ liệu mẫu có 16 thuốc thường dùng.
- **Thuốc không xoá và không gộp được.** Thuốc đã có trong đơn thuốc (`soLanKe > 0`) không đổi được tên, hoạt chất, hàm
  lượng, dạng bào chế, đường dùng (chỉ sửa được cách viết hoa / khoảng trắng của tên); đổi → 409 `THUOC_DA_DUOC_KE` (dòng
  đơn thuốc chỉ lưu id thuốc, đổi là sửa luôn đơn cũ). Đơn vị và mô tả sửa được bất cứ lúc nào.
- **Thuốc ngừng dùng** không còn trong gợi ý `GET /api/medical/bac-si/toi/thuoc`, và bác sĩ kê thuốc đó ở đơn mới nhận 409
  `THUOC_NGUNG_DUNG` (thông điệp có tên thuốc). Hồ sơ bệnh án đã có thuốc đó vẫn sửa / lưu lại được với dòng thuốc cũ, và
  bệnh nhân vẫn thấy thuốc trong kết quả khám cũ.
- **Hồ sơ bệnh án** bác sĩ sửa được trong `RECORD_EDIT_DAYS` ngày (mặc định 7) kể từ lần lưu đầu, mỗi lần sửa cần lý do và
  thêm 1 dòng lịch sử không sửa / xoá được; sau đó chỉ xem (409 `HET_HAN_SUA_BENH_AN`). Biến này đọc ở medical-service (chặn
  sửa) và booking-service (trả `hanSua` cho màn hình bác sĩ): đặt trong `.env` thì cả 2 cùng nhận, chạy IntelliJ lẫn docker
  compose. Quản trị viên xem lịch sử sửa bằng `GET /api/medical/quan-tri/lich-hen/{id}/benh-an/lich-su`. Việc mở
  xem hồ sơ bệnh án không được ghi lại.

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
- Thời hạn sửa đổi: quy tắc `soNgayDuocSuaDanhGia` (mặc định 7, giá trị ban đầu từ `RATING_EDIT_DAYS`), quản trị viên sửa
  ở `/api/booking/quan-tri/cau-hinh`.

## Dashboard

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/booking/bac-si/toi/tong-quan` | `BAC_SI` | Dashboard của bác sĩ, 1 lần gọi cho cả 4 ô: `{ lichHenHomNay, dangCho, daKham, diemDanhGia, soDanhGia, lichHenSapToi, tinhLuc }` |
| `GET /api/booking/quan-tri/tong-quan` | `QUAN_TRI_VIEN` | Dashboard của quản trị viên, 1 lần gọi cho cả 3 ô: `{ bacSiDangCongTac, benhNhanDaDangKy, luotDatTrongThang, tinhLuc }` |
| `GET /api/booking/bac-si/toi/hang-doi?ngay=` (+ `goi-tiep`, `goi-kham`, `vang-mat`) | `BAC_SI` | Hàng đợi khám hôm nay theo ca, đặt ở đầu Dashboard của bác sĩ (bảng route "Đặt lịch khám"; chi tiết ở `docs/BAN-GIAO-FRONTEND-05-10.md` mục "Hàng đợi khám"). Không có đẩy realtime: gọi lại khoảng 15 giây 1 lần |

Định nghĩa từng số (frontend không tự đếm lại từ các danh sách: danh sách có phân trang và bộ lọc riêng):

| Ô trên UI | Trường | Cách tính |
|---|---|---|
| Today's Appointments | `lichHenHomNay` | Lịch hẹn của bác sĩ có giờ khám hôm nay, **không tính** `DA_HUY`, `DA_HUY_DO_DOI_LICH`, `BI_TU_CHOI` |
| Waiting | `dangCho` | Trong số đó: `DA_XAC_NHAN` và không có `canDoiLich` (chưa khám) |
| Completed | `daKham` | Trong số đó: `DA_HOAN_THANH` |
| Average Rating | `diemDanhGia`, `soDanhGia` | Trung bình mọi đánh giá của bác sĩ, 1 chữ số thập phân; `null` và `0` khi chưa có. Hiển thị kèm số lượt |
| (nhãn Upcoming Appointments) | `lichHenSapToi` | Số lịch `DA_XAC_NHAN` của bác sĩ, không có `canDoiLich`, mà lượt khám bắt đầu sau `tinhLuc` = số dòng của màn hình Upcoming Appointments |
| Active Doctors | `bacSiDangCongTac` | Bác sĩ `DANG_CONG_TAC` có tài khoản `DA_KICH_HOAT`: đúng tập bác sĩ của danh sách công khai |
| Registered Patients | `benhNhanDaDangKy` | Tài khoản vai trò `BENH_NHAN` đã kích hoạt. Khách chỉ đặt lịch (không có tài khoản) không tính |
| Bookings This Month | `luotDatTrongThang` | Lịch hẹn **được tạo** từ 00:00 ngày 1 tháng này, mọi trạng thái trừ `DA_HUY_DO_DOI_LICH` (1 lần đổi lịch chỉ tính 1) |

- `tinhLuc` là thời điểm backend tính các số; số được đếm trực tiếp mỗi lần gọi, không lưu sẵn.

**Báo cáo thống kê (report-service, DASH-01..04, ADM-05).** Chỉ đọc DB chung (repository chỉ đọc, không có bảng / migration
riêng), chỉ `QUAN_TRI_VIEN`:

| API | Ghi chú |
|---|---|
| `GET /api/report/quan-tri/thong-ke?ky=NGAY\|TUAN\|THANG\|NAM\|TUY_CHON&ngay=&tuNgay=&denNgay=&idChuyenKhoa=` | Kỳ quanh `ngay` (mặc định `THANG`, hôm nay; tuần thứ Hai - Chủ nhật), hoặc `TUY_CHON` từ `tuNgay` đến `denNgay` tối đa 366 ngày (sai → 400 `DU_LIEU_KHONG_HOP_LE`); `idChuyenKhoa` không có → 404. Trả `tongQuan`, `trangThai`, `kenhDat`, `thoiGianDatTruoc`, `theoChuyenKhoa[]`, `theoBacSi[]`, `theoThoiGian[]` (theo ngày khi kỳ ≤ 31 ngày, theo tuần khi ≤ 184, theo tháng), `tinhLuc` |
| `GET /api/report/quan-tri/thong-ke/xuat-excel` | Cùng tham số, tệp `bao-cao-thong-ke_<tuNgay>_<denNgay>.xlsx` 5 trang (tổng quan, theo chuyên khoa, theo bác sĩ, theo thời gian, ghi chú cách tính), chữ theo `Accept-Language` |

Cách tính: lịch hẹn theo giờ khám trong kỳ, không tính lịch cũ của 1 lần đổi lịch; lượt khám = `DA_HOAN_THANH`, bệnh nhân
đã khám = số hồ sơ bệnh nhân khác nhau (duy nhất theo CCCD); tỷ lệ hủy = (bệnh nhân + bác sĩ + quản trị viên hủy) / lịch
hẹn; tỷ lệ không đến = không đến / (đã khám + không đến); tỷ lệ lấp đầy = lượt đã đặt / tổng lượt của các ca còn hoạt động
(ca bị hủy, kể cả do ngày nghỉ, không tính); kênh đặt từ `lich_hen.kenh_dat`; thời gian đặt trước = giờ bắt đầu lượt − lúc
tạo lịch. `luotDatMoi` (lịch **tạo** trong kỳ) cùng quy tắc với `luotDatTrongThang` của Dashboard, nên báo cáo tháng này và
Dashboard cho cùng số. Tỷ lệ là %, 1 chữ số thập phân, `null` khi mẫu số 0. Tên chuyên khoa / bác sĩ sắp theo bảng chữ của
ngôn ngữ request.
- "System Uptime" trên Dashboard của quản trị viên **không có API** (đã thống nhất bỏ): frontend ẩn ô này.
- Bảng "Màn hình UI → API" (mỗi màn hình của danh sách UI gọi những API nào) nằm ở đầu
  [docs/BAN-GIAO-FRONTEND-05-10.md](docs/BAN-GIAO-FRONTEND-05-10.md).

## Thông báo trong ứng dụng

Chuông thông báo: lưu, liệt kê, đếm chưa đọc, đánh dấu đã đọc. Không đẩy realtime: frontend hỏi lại
`so-chua-doc` định kỳ (trang mẫu: 30 giây, bỏ qua khi tab đang ẩn, hỏi ngay khi tab hiện lại). notification-service cũng gửi
email lịch hẹn (mục "Email lịch hẹn" bên dưới).

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/notification/thong-bao?chuaDoc=&trang=&kichThuoc=` | Mọi tài khoản đã đăng nhập | Thông báo của tôi, mới nhất trước, dạng `TrangDuLieu`. `chuaDoc=true` chỉ lấy thông báo chưa đọc; `kichThuoc` mặc định 20, tối đa 100. Mỗi dòng: `{ id, loai, noiDung, daDoc, ngayTao, idLichHen, maPhieuKham, idYeuCau }`. `noiDung` là câu đã soạn sẵn theo `Accept-Language` của request đọc (đổi ngôn ngữ rồi tải lại là câu đổi theo; tên người, mã và lý do người dùng gõ giữ nguyên văn; thông báo tạo trước khi có 2 ngôn ngữ chỉ có câu tiếng Việt), hiển thị nguyên văn. `maPhieuKham` chỉ có khi người đọc là bệnh nhân (dùng để mở chi tiết lịch hẹn); bác sĩ dùng `idLichHen` |
| `GET /api/notification/thong-bao/so-chua-doc` | Mọi tài khoản đã đăng nhập | `{ soChuaDoc }`: số trên biểu tượng chuông |
| `POST /api/notification/thong-bao/{id}/da-doc` | Mọi tài khoản đã đăng nhập | Đánh dấu 1 thông báo của tôi đã đọc, gọi lại nhiều lần vẫn được. Thông báo của tài khoản khác hoặc không có -> 404 `KHONG_TIM_THAY_THONG_BAO` |
| `POST /api/notification/thong-bao/da-doc-tat-ca` | Mọi tài khoản đã đăng nhập | `{ soDaDanhDau }` |

| Sự kiện | `loai` | Người nhận |
|---|---|---|
| Có lịch hẹn mới (kể cả khách đặt) | `LICH_HEN_MOI` | Bác sĩ của lịch hẹn |
| Bác sĩ hủy lịch đã xác nhận (câu thông báo có lý do) | `LICH_HEN_BI_TU_CHOI` | Tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân (mỗi tài khoản 1 thông báo); kèm email tới địa chỉ liên hệ |
| (đã bỏ từ 15/10/2026) Bác sĩ xác nhận | `LICH_HEN_DA_XAC_NHAN` | Không còn được tạo; chỉ có ở thông báo cũ |
| Bệnh nhân hủy | `LICH_HEN_DA_HUY` | Bác sĩ của lịch hẹn |
| Bệnh nhân đổi lịch, cùng bác sĩ | `LICH_HEN_DA_DOI` (gắn với lịch mới) | Bác sĩ đó, 1 thông báo |
| Bệnh nhân đổi lịch sang bác sĩ khác | `LICH_HEN_DA_HUY` cho bác sĩ cũ, `LICH_HEN_MOI` cho bác sĩ mới | Cả 2 bác sĩ |
| Ca khám bị hủy (quản trị viên hủy, hoặc duyệt xin nghỉ / đổi ca) | `LICH_HEN_CAN_DOI` | Tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân, cho từng lịch hẹn còn hiệu lực của ca |
| Ca khám đổi phòng | `LICH_HEN_DOI_PHONG` | Như trên |
| Ca khám được nhường cho bác sĩ khác (quản trị viên duyệt nhường ca) | `LICH_HEN_DOI_BAC_SI` | Như trên; kèm email "đổi bác sĩ khám" |
| Quản trị viên xếp ca mới (1 thông báo cho 1 lần xếp), sửa hoặc hủy ca; sao chép, nhập Excel (1 thông báo cho mỗi bác sĩ); gửi lịch làm việc (kèm email) | `CA_LAM_VIEC_THAY_DOI` | Bác sĩ của ca |
| Bác sĩ gửi yêu cầu đổi ca / xin nghỉ (xin nghỉ nhiều ca: 1 thông báo cho cả nhóm); bác sĩ nhận đồng ý nhận ca được nhường | `YEU_CAU_DOI_LICH_MOI` (có `idYeuCau`) | Mọi quản trị viên đang hoạt động |
| Quản trị viên duyệt / từ chối yêu cầu (cả nhóm: 1 thông báo); bác sĩ nhận đồng ý / từ chối nhận ca; ca của yêu cầu bị quản trị viên hủy | `KET_QUA_DUYET_DOI_LICH` (có `idYeuCau`, câu thông báo có ghi chú) | Bác sĩ đã gửi |
| Bác sĩ khác nhường ca cho tôi; bác sĩ đó rút yêu cầu; quản trị viên duyệt / từ chối; ca bị hủy | `YEU_CAU_NHUONG_CA` (có `idYeuCau`) | Bác sĩ nhận ca |
| Quản trị viên hủy lịch hẹn (có lý do) | `LICH_HEN_QUAN_TRI_HUY` (bệnh nhân), `LICH_HEN_DA_HUY` (bác sĩ) | Tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân; bác sĩ của lịch hẹn; kèm email `email.quanTriHuy` |
| Quản trị viên đổi lịch thay bệnh nhân | `LICH_HEN_QUAN_TRI_DOI` (bệnh nhân, gắn với lịch mới); bác sĩ: `LICH_HEN_DA_DOI` hoặc `LICH_HEN_DA_HUY` + `LICH_HEN_MOI` khi đổi bác sĩ | Như trên; kèm email đổi lịch |
| Khai báo ngày bệnh viện nghỉ hủy ca | `LICH_HEN_CAN_DOI` (bệnh nhân), 1 `CA_LAM_VIEC_THAY_DOI` cho mỗi bác sĩ | Như "Ca khám bị hủy" |

Người làm thao tác không nhận thông báo về thao tác của chính mình. Khách đặt không đăng nhập và người giám hộ chỉ khớp theo
CCCD không nhận thông báo. Nhắc lịch khám (`NHAC_LICH_KHAM`, bệnh nhân): trước giờ khám 3 ngày và 1 ngày (cấu hình
`BOOKING_REMINDER_*`), kèm 1 email cho người đặt; lịch đã hủy / bác sĩ đã hủy / đang chờ đổi không được nhắc. Nhắc tái khám
(`NHAC_TAI_KHAM`, bệnh nhân): bác sĩ ghi hoặc đổi `ngayTaiKhamDeXuat` thì sau nhiều nhất 1 chu kỳ nhắc (5 phút) bệnh nhân
nhận thông báo + email nêu bác sĩ và ngày tái khám, rồi được nhắc lại 1 lần trước ngày đó 7 ngày
(`FOLLOWUP_REMINDER_DAYS`). Không có lịch hẹn nào được tạo; xoá ngày tái khám thì không nhắc; bệnh nhân đã có lịch hẹn
sắp tới với bác sĩ cùng chuyên khoa thì bỏ lần nhắc trước. medical-service không gửi gì: booking-service tự so ngày tái
khám với ngày đã báo (bảng `nhac_tai_kham`).

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

## Email lịch hẹn

notification-service gửi email cho người đặt lịch; booking-service ghi sự kiện vào `su_kien_thong_bao` trong cùng
transaction rồi `GuiThongBaoJob` chuyển sang `POST /noi-bo/email` (cùng đường đi với chuông: service ngừng chạy thì
email nằm chờ, không mất và không gửi 2 lần).

| Email | Khi nào |
|---|---|
| Đặt lịch khám thành công (lịch đã được xác nhận) | Ngay sau khi đặt lịch (khách hoặc tài khoản); sau khi đổi lịch: 1 email cho lịch mới |
| Bác sĩ hủy lịch | Ngay khi bác sĩ hủy 1 lịch đã xác nhận, kèm lý do; gửi tới cùng địa chỉ liên hệ như email đặt lịch, nút mở phiếu khám |
| Nhắc lịch khám | Trước giờ khám 3 ngày và 1 ngày (`BOOKING_REMINDER_BEFORE`), chỉ trong `BOOKING_REMINDER_FROM` đến `BOOKING_REMINDER_TO` (07:00 - 21:00); kèm thông báo `NHAC_LICH_KHAM` trên chuông |
| Bác sĩ hẹn / đổi ngày tái khám, nhắc tái khám | Sau khi bác sĩ ghi `ngayTaiKhamDeXuat` (chậm nhất `BOOKING_REMINDER_INTERVAL`), và trước ngày đó `FOLLOWUP_REMINDER_DAYS` ngày; kèm thông báo `NHAC_TAI_KHAM`. Không tạo lịch hẹn |
| Đổi bác sĩ khám (`email.doiBacSi`) | Ngay khi quản trị viên duyệt cho bác sĩ nhường ca: 1 email cho mỗi lịch hẹn còn hiệu lực của ca, nêu bác sĩ mới, giờ và phòng không đổi, hủy / đổi được tới giờ khám; nút mở phiếu khám; kèm thông báo `LICH_HEN_DOI_BAC_SI` |
| Lịch làm việc của bác sĩ (`email.lichLamViec`) | Khi quản trị viên bấm "Gửi lịch cho bác sĩ": tới email tài khoản của bác sĩ, theo `ngonNgu` của tài khoản; bảng các ca (ngày kèm thứ, giờ, phòng · tầng; tối đa 100 dòng, còn lại ghi số ca chưa liệt kê) và nút mở `FRONTEND_URL` + `DOCTOR_SCHEDULE_PATH?tuNgay=&denNgay=`; kèm thông báo `CA_LAM_VIEC_THAY_DOI` |

| Ca khám bị hủy, cần đổi lịch (`email.canDoiLich`) | Mỗi lần ca bị hủy mà vẫn giữ lịch hẹn (quản trị viên hủy ca, duyệt xin nghỉ / đổi ca, khai báo ngày nghỉ): 1 email cho mỗi lịch hẹn có địa chỉ nhận (kể cả khách đặt), nút mở phiếu khám để đổi / hủy |
| Phòng khám hủy lịch (`email.quanTriHuy`) | Quản trị viên hủy 1 lịch hẹn, kèm lý do; theo ngôn ngữ của lịch hẹn |
| Cấp lại link (`email.capLaiLienKet`) | Quản trị viên / bác sĩ cấp lại qua email, hoặc khách tự xin (mã tra cứu + SĐT khớp): link phiếu khám của lịch hiện hành, và nút thứ 2 mở sổ khám bệnh khi đã khám |

- Tệp lịch `lich-kham.ics` (như `GET /api/booking/phieu-kham/{ma}/lich.ics`, cột `tep_lich` của `su_kien_thong_bao` và
  `email_gui`, migration V36) đi kèm email đặt lịch, đổi lịch, đổi bác sĩ, cần đổi lịch, bác sĩ hủy, phòng khám hủy, cấp lại
  link; email nhắc lịch, tái khám, lịch làm việc không kèm. Bệnh nhân tự hủy thì không có email.
- Người nhận: email liên hệ nhập lúc đặt lịch, không có thì email của tài khoản đã đặt; khách không nhập email thì không
  nhận gì. Ngôn ngữ: `ngonNgu` của tài khoản, với khách là ngôn ngữ của request đặt lịch, với lịch đặt qua tổng đài là
  `ngonNgu` quản trị viên chọn cho người gọi.
- Máy chủ nhận thử khi dev: Mailpit (`docker run -d -p 1025:1025 -p 8025:8025 axllent/mailpit`, xem thư ở
  http://localhost:8025), rồi chạy notification-service với `NOTIFICATION_MAIL_MODE=smtp MAIL_HOST=localhost` (trong compose:
  `mailpit`) `MAIL_PORT=1025 MAIL_SMTP_AUTH=false MAIL_SMTP_STARTTLS=false`.
- Nội dung: bệnh nhân, ngày giờ, bác sĩ, phòng, mã tra cứu, nút mở phiếu khám (email tái khám: nút mở
  `FRONTEND_URL` + `FOLLOWUP_BOOKING_PATH`). Không có lý do khám, chẩn đoán, số điện thoại, CCCD.
- Chế độ gửi theo `MAIL_MODE` như identity-service (`console`: chỉ ghi log notification-service; `smtp`: gửi thật). Muốn
  notification-service gửi tới máy chủ nhận thử (vd Mailpit) trong khi identity-service vẫn ở chế độ khác: đặt
  `NOTIFICATION_MAIL_MODE`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`. Gửi lỗi thì thử lại theo
  `NOTIFICATION_MAIL_RETRY_WAITS` (1 phút, 5 phút, 30 phút, 2 giờ) rồi bỏ; chu kỳ gửi `NOTIFICATION_MAIL_INTERVAL`. Lượt khám
  còn cách ít hơn `BOOKING_REMINDER_MIN_LEAD` thì không nhắc. Giải thích từng biến: `.env.example`.

## Ngôn ngữ (EN / VI)

Request gửi header `Accept-Language: en` thì nhận tiếng Anh; không gửi (hoặc ngôn ngữ khác) thì nhận tiếng Việt như
trước. Đổi theo header: `thongDiep` (lỗi, kiểm tra dữ liệu, thành công, 401 / 403, lỗi của gateway), nội dung danh mục
(chuyên khoa, phòng khám, hồ sơ bác sĩ, chú thích ảnh, đơn vị và mô tả thuốc; chưa có bản tiếng Anh thì trả bản tiếng
Việt), câu thông báo trên chuông, câu cố định của chatbot. Không đổi: `maLoi`, tên trường, giá trị enum. Không bao giờ
dịch: họ tên, học vị, tên thuốc và mọi chữ người dùng tự gõ.

- Tài khoản lưu ngôn ngữ ưa dùng `ngonNgu` (`VI` / `EN`): lấy từ `Accept-Language` lúc đăng ký, đổi bằng
  `PUT /api/users/me/language`; dùng cho email và lời nhắc (những thứ gửi khi không có request của người dùng).
- API quản lý danh mục (`/quan-tri/`, và `POST` / `PUT` chuyên khoa) nhận và trả cả 2 bản: trường cũ là tiếng Việt, trường
  `…En` là tiếng Anh.
- Thêm câu chữ mới ở backend: `docs/QUY-UOC-CODE.md` mục 4, kiểm tra bằng `node scripts/kiem-tra-thong-diep.js`.
- Hợp đồng đầy đủ cho frontend: [docs/BAN-GIAO-FRONTEND-NGON-NGU-EMAIL-CHATBOT.md](docs/BAN-GIAO-FRONTEND-NGON-NGU-EMAIL-CHATBOT.md).

## Chatbot (trợ lý AI)

Hội thoại với trợ lý AI: hướng dẫn dùng trang (đặt lịch, phiếu khám, đổi / hủy lịch, nhắc lịch) và trả lời câu hỏi sức khỏe
thông thường. Từ triệu chứng người dùng kể, chatbot gợi ý chuyên khoa rồi bác sĩ, phòng và khung giờ còn chỗ, đọc từ dữ
liệu của phòng khám. Khách và mọi tài khoản đã đăng nhập đều dùng được. Bệnh nhân đã đăng nhập còn hỏi được về lịch hẹn
sắp tới, các lượt đã khám và ngày tái khám của **chính mình**, sau khi đồng ý trong phiên chat.

| API | Ai gọi | Ghi chú |
|---|---|---|
| `POST /api/chatbot/phien` | Khách, tài khoản đã đăng nhập | Mở phiên mới -> 201 `{ idPhien, maKhach, thoiGianBatDau, tinNhan[] }`; `tinNhan` có sẵn câu chào (nói rõ là AI, không thay bác sĩ). `maKhach` chỉ có khi **chưa đăng nhập** và chỉ trả lần này: frontend giữ lại (vd `sessionStorage`) và gửi ở header `X-Chat-Khach` trong các request sau của phiên |
| `POST /api/chatbot/phien/{idPhien}/tin-nhan` | Chủ phiên | Body `{ noiDung }` (1 đến 1000 ký tự) -> `{ tinNhan, traLoi }`, mỗi tin `{ id, nguoiGui, loai, noiDung, ngayTao, goiY }`. Trả lời sau vài giây, tới khoảng 25 giây khi có tra cứu (không stream) |
| `GET /api/chatbot/phien/{idPhien}/tin-nhan` | Chủ phiên | Toàn bộ tin nhắn của phiên, cũ nhất trước |
| `GET /api/chatbot/phien` | Tài khoản đã đăng nhập | Các phiên gần nhất của tôi `{ idPhien, thoiGianBatDau, daDongYDuLieu }`, mới nhất trước, tối đa 50 |
| `POST /api/chatbot/phien/{idPhien}/dong-y` | Bệnh nhân, phiên của chính tài khoản | Đồng ý cho chatbot đọc lịch hẹn, lượt khám của mình trong phiên này. Phiên của người khác và phiên của khách: 404 |
| `DELETE /api/chatbot/phien/{idPhien}/dong-y` | Bệnh nhân, phiên của chính tài khoản | Rút lại đồng ý của phiên |

- **Chủ phiên**: tài khoản đã mở phiên (JWT), hoặc người cầm `maKhach` của phiên khách. Phiên không tồn tại và phiên của
  người khác cùng trả 404 `KHONG_TIM_THAY`; chỉ biết `idPhien` thì không đọc, không gửi được gì. Phiên khách không chuyển
  sang tài khoản khi đăng nhập (người đã đăng nhập vẫn dùng tiếp được phiên khách nếu gửi kèm `X-Chat-Khach`).
- `nguoiGui`: `NGUOI_DUNG` / `CHATBOT`. `loai`: `CAU_HOI` (tin của người dùng); của chatbot: `LOI_CHAO`, `TRA_LOI` (mô hình trả
  lời), `NGOAI_PHAM_VI` (câu từ chối cố định khi hỏi việc không liên quan, vd mua sắm), `KHAN_CAP` (tin có dấu hiệu cấp cứu:
  câu "gọi 115" cố định, không qua mô hình), `CAN_DONG_Y` (câu cố định xin bệnh nhân đồng ý, xem "Lịch hẹn của chính
  người hỏi" bên dưới). `noiDung` giữ nguyên như lúc gửi / trả lời, không đổi theo `Accept-Language`
  của request đọc lại.
- **`goiY`** (chỉ ở `TRA_LOI` mà chatbot đã tra cứu dữ liệu phòng khám, còn lại là `null`):
  `{ chuyenKhoa: { id, ten }, bacSi: [{ id, hoTen, hocVi, chucVu, anhDaiDien, ngaySomNhat, soChoConLai }],
  lichKham: [{ idBacSi, hoTenBacSi, idLichLamViec, ngay, tenPhong, tang, khungGio: [{ gioBatDau, gioKetThuc, soChoConLai }] }],
  lichHenCuaToi, lanKham }` (2 trường cuối: xem "Lịch hẹn của chính người hỏi").
  - `bacSi`: tối đa 6 bác sĩ của chuyên khoa, người còn chỗ sớm nhất đứng trước; `ngaySomNhat` là `null` khi bác sĩ hết
    chỗ. `lichKham`: các ca còn chỗ của 1 bác sĩ trong 7 ngày, chỉ gồm khung giờ còn chỗ. Mảng rỗng = chưa tra cứu phần đó.
  - **Do server dựng từ dữ liệu, không lấy từ câu chữ của mô hình**: frontend vẽ thẻ bác sĩ / khung giờ từ `goiY` và mở
    form đặt lịch bằng `chuyenKhoa.id`, `bacSi[].id`, `idLichLamViec` + `khungGio[].gioBatDau`. Câu trong `noiDung` do
    mô hình viết nên chỉ để đọc, không lấy bác sĩ hay giờ khám từ đó.
  - Là ảnh chụp lúc trả lời (đọc lại phiên vẫn trả đúng như vậy): số chỗ có thể đã đổi, form đặt lịch phải đọc lại khung
    giờ. Chữ trong `goiY` theo `Accept-Language` của request đã gửi tin nhắn.
- Chatbot chọn chuyên khoa dựa trên tên, mô tả và **triệu chứng thường gặp** của từng chuyên khoa. Quản trị viên nhập
  triệu chứng ở 2 trường tuỳ chọn `trieuChung`, `trieuChungEn` (tối đa 2000 ký tự) của `POST` / `PUT /api/catalog/chuyen-khoa`;
  `GET /api/catalog/quan-tri/chuyen-khoa` trả lại 2 trường này, API chuyên khoa công khai thì không. Dữ liệu mẫu có sẵn
  triệu chứng cho 8 chuyên khoa.
- Với khách và mọi tài khoản, chatbot có 2 hàm tra cứu cố định, chỉ đọc dữ liệu công khai: bác sĩ của 1 chuyên khoa và
  lịch còn chỗ của 1 bác sĩ (đúng những gì trang đặt lịch hiển thị cho khách). catalog-service hoặc booking-service không chạy thì chatbot vẫn trả
  lời, chỉ không có `goiY`.
- **Lịch hẹn của chính người hỏi.** Chỉ khi request có JWT vai trò `BENH_NHAN` **và** phiên chat thuộc chính tài khoản đó
  (phiên của khách không bao giờ được, kể cả khi người dùng đã đăng nhập), chatbot có thêm 2 hàm không tham số: lịch hẹn
  sắp tới và các lượt đã khám của người đang chat. "Của ai" là JWT của request, chatbot-service gửi tiếp JWT đó tới API
  "của tôi" của booking-service; không có cách nào hỏi về người khác, dù gõ tên, số điện thoại hay mã nào.
  - **Đồng ý theo từng phiên**: lần đầu bệnh nhân hỏi về lịch hẹn của mình, câu trả lời có `loai: CAN_DONG_Y` (câu cố
    định nói rõ cái gì sẽ được gửi cho dịch vụ AI bên ngoài), chưa có gì được đọc. Frontend hiện nút "Đồng ý", gọi
    `POST .../dong-y` rồi **gửi lại câu hỏi**. `DELETE .../dong-y` rút lại; `daDongYDuLieu` ở danh sách phiên cho biết
    trạng thái.
  - **Gửi cho mô hình**: chỉ ngày giờ khám, bác sĩ, chuyên khoa, phòng, tầng, trạng thái, lịch của mình hay đặt cho người
    khác, ngày tái khám đề xuất. **Không gửi**: họ tên bệnh nhân, mã tra cứu, mã phiếu khám, lý do khám, lý do hủy, chẩn
    đoán, lời dặn, đơn thuốc.
  - **Thẻ cho người dùng** (do server dựng, có họ tên và mã): `goiY.lichHenCuaToi: [{ maPhieuKham, maTraCuu,
    linkPhieuKham, trangThai, ngay, gioBatDauKhung, gioKetThucKhung, hoTenBacSi, tenChuyenKhoa, tenPhong, tang,
    hoTenBenhNhan, laBanThan, canDoiLich }]` (tối đa 10, gần nhất trước) và `goiY.lanKham: [{ maPhieuKham, maTraCuu, ngay,
    hoTenBacSi, tenChuyenKhoa, hoTenBenhNhan, laBanThan, ngayTaiKhamDeXuat }]` (tối đa 10, mới nhất trước). `null` = câu
    trả lời không tra cứu phần đó, mảng rỗng = đã tra cứu mà không có. Mở chi tiết bằng `maPhieuKham`
    (`GET /api/booking/lich-hen/cua-toi/{maPhieuKham}`); chẩn đoán và đơn thuốc chỉ có ở màn hình lịch sử khám.
  - Phạm vi dữ liệu đúng như màn hình "Lịch hẹn của tôi" và "Lịch sử khám" (gồm lịch tài khoản đặt cho người thân).
  - `CHATBOT_PERSONAL_DATA=false` tắt hẳn tính năng này.
- Ngôn ngữ: câu chào, câu từ chối, câu cấp cứu, câu xin đồng ý theo `Accept-Language`; mô hình trả lời theo `Accept-Language`, trừ khi người
  dùng viết rõ bằng ngôn ngữ còn lại.
- Lỗi: 400 `DU_LIEU_KHONG_HOP_LE` (tin rỗng / quá 1000 ký tự); 429 `CHAT_QUA_NHIEU` (mặc định 20 tin / 10 phút và 100 tin /
  ngày cho mỗi tài khoản hoặc mỗi địa chỉ IP của khách, 10 phiên / giờ, 1000 lần gọi mô hình / ngày cho cả hệ thống);
  503 `CHATBOT_KHONG_KHA_DUNG` (chưa đặt khoá API hoặc mô hình không trả lời: **không lưu gì**, cho người dùng gửi lại).
- Gửi cho nhà cung cấp mô hình: chỉ dẫn hệ thống (kèm danh sách chuyên khoa), tối đa 10 tin gần nhất của chính phiên đó,
  câu hỏi mới và kết quả tra cứu (tên bác sĩ, phòng, khung giờ: dữ liệu công khai; với bệnh nhân đã đồng ý thì thêm phần
  tối thiểu về lịch hẹn của họ như trên). Họ tên người dùng không bao giờ được gửi. Rút lại đồng ý thì các câu trả lời cũ
  về lịch hẹn cũng thôi được gửi lại. Câu trả lời chép lại chỉ dẫn hệ thống bị thay bằng câu từ chối. Log không ghi nội
  dung tin nhắn, không ghi JWT.

Mô hình được gọi qua API kiểu OpenAI nên đổi nhà cung cấp bằng cấu hình: `CHATBOT_BASE_URL`, `CHATBOT_API_KEY`,
`CHATBOT_MODEL` (mặc định Gemini; khoá miễn phí ở https://aistudio.google.com/apikey). **Chưa đặt `CHATBOT_API_KEY` thì
chatbot tắt**: mở phiên vẫn được, gửi tin nhắn trả 503. 1 tin nhắn có tra cứu gọi mô hình 2 - 3 lần (tối đa
`CHATBOT_MAX_MODEL_CALLS`, trong `CHATBOT_TOTAL_TIMEOUT`), mỗi lần đều tính vào `CHATBOT_LIMIT_TOTAL_DAILY`. chatbot-service
đọc bác sĩ và lịch khám ở `CATALOG_URL`, `BOOKING_URL`. Các giới hạn và biến khác: xem `.env.example`. Chỉ dẫn hệ thống và
danh sách dấu hiệu cấp cứu là các file văn bản trong `chatbot-service/src/main/resources/chatbot/`.

## Hiệu năng: giới hạn tần suất, cache, kiểm tra tải

Chi tiết, số liệu đo và các phương án đã loại: [docs/KIEN-TRUC-HIEU-NANG.md](docs/KIEN-TRUC-HIEU-NANG.md).

- **Giới hạn tần suất** ở gateway, theo IP, mỗi phút: `GATEWAY_AUTH_RATE_LIMIT` (20, POST đăng nhập / đăng ký / quên mật
  khẩu / gửi lại email), `GATEWAY_CHATBOT_RATE_LIMIT` (30, POST chatbot), `GATEWAY_RATE_LIMIT` (300, còn lại). Vượt → 429
  `QUA_NHIEU_YEU_CAU` kèm `Retry-After`. Đếm trên Redis; Redis lỗi thì bỏ qua giới hạn.
- **Cache Redis** (10 phút, theo ngôn ngữ): danh sách chuyên khoa không có từ khoá, chi tiết chuyên khoa (catalog), ngày
  bệnh viện nghỉ (booking). Quản trị viên sửa thì cache bị xoá sau khi lưu. Khung giờ còn trống và bác sĩ không cache.
  Redis lỗi thì đọc thẳng DB.
- **Nén gzip** response JSON từ 2 KB ở gateway.
- **Kiểm tra tải** bằng k6 (chạy trong Docker, không cần cài): `scripts/load-test/kiem-tra-tai.js`, lệnh ở đầu file. Chạy
  trên stack riêng `-p eclinic-smoke` (không đụng dữ liệu của bạn) và đặt 3 biến `GATEWAY_*RATE_LIMIT` cùng
  `BOOKING_IP_LIMIT` rất lớn (mọi request của k6 đến từ 1 IP). Chế độ thực tế (mặc định, 1.000 người dùng, khoảng 9 phút):
  0% lỗi, p95 13 ms. Chế độ dồn tải `-e VUS=1000 -e HOLD=2m -e NGHI=0.5`: khoảng 950 – 1.100 request/giây trên 1 laptop.
  Kết quả ghi ở `scripts/load-test/ket-qua/` (không commit).

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

Lần chạy đủ gần nhất (21/10/2026, cách trên, khoảng 11 phút): BUILD SUCCESS, 292 test, 0 lỗi — api-gateway 15,
identity-service 247, catalog-service 21, booking-service 6, medical-service 3 (common, notification, chatbot, report chưa có
test).

## Lỗi thường gặp

| Hiện tượng | Nguyên nhân / cách xử lý |
|---|---|
| `Port 808x was already in use` | Service đó đang chạy ở chỗ khác (IntelliJ và Docker cùng lúc?). Dừng một bên. |
| Gọi qua gateway nhận 503 `DICH_VU_KHONG_KHA_DUNG` | Service đích chưa chạy hoặc chưa khởi động xong. |
| 401 `CHUA_DANG_NHAP` dù đã dán token | Token hết hạn, hoặc `JWT_SECRET` của service khác khoá tạo token. Tạo token mới. |
| 403 `KHONG_CO_QUYEN` | Token đúng nhưng sai vai trò (ví dụ API chỉ dành cho `QUAN_TRI_VIEN`). |
| Không đăng nhập được `bacsi01@eclinic.local` | Dữ liệu mẫu chưa được tạo: `SEED_DATA` đang tắt (chạy ngoài docker compose / run config có sẵn), hoặc DB đã có bác sĩ từ trước khi bật. |
| Tải ảnh đại diện nhận 503 `LUU_TRU_ANH_KHONG_KHA_DUNG` | Chưa đặt đủ 3 biến `CLOUDINARY_*` (xem mục Ảnh đại diện), hoặc Cloudinary không phản hồi. |
| Mọi API nhận 429 `QUA_NHIEU_YEU_CAU` | Quá giới hạn số request của 1 IP trong 1 phút tại gateway (mục Hiệu năng). Chờ `Retry-After` giây. Kiểm tra tải hoặc script gửi nhiều request: nâng `GATEWAY_RATE_LIMIT`, `GATEWAY_AUTH_RATE_LIMIT`, `GATEWAY_CHATBOT_RATE_LIMIT`. |
| Tải ảnh nhận 413 `TEP_QUA_LON` / `YEU_CAU_QUA_LON` | Ảnh quá 2 MB / request quá 5 MB tại gateway (`GATEWAY_MAX_REQUEST_SIZE`). |
| Flyway báo `checksum mismatch` | Đã sửa file migration đã chạy. Không sửa file cũ; tạo file `V<n>__...sql` mới. Máy local có thể `docker compose down -v` để làm lại DB. |
| Service báo lỗi kết nối MySQL | Chưa `docker compose up -d`, hoặc MySQL chưa "healthy" (xem `docker compose ps`). |
| `Command line is too long` (Windows) | Run config đã đặt *Shorten command line: @argfiles*; nếu tự tạo config mới thì chọn như vậy. |
