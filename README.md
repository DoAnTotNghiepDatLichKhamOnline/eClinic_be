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
| `notification-service` | 8086 | NOTI-01, gửi email | http://localhost:8086/swagger-ui.html |
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
- **Node.js** (không bắt buộc): chỉ cần cho trang demo `scripts/demo-xac-thuc/server.js`.

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
  Mọi tài khoản mẫu dùng mật khẩu **`Demo@123`** (đổi bằng `SEED_PASSWORD`):

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
  (chưa có API cấp tài khoản bác sĩ, AUTH-04). Token này dùng được trên Swagger của mọi service.
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

### Ảnh đại diện (Cloudinary)

Ảnh lưu ở [Cloudinary](https://cloudinary.com/users/register_free), gói Free, không cần thẻ:
1. Đăng ký tài khoản, vào Console > Settings > API Keys: lấy **Cloud name** (đầu trang), **API Key**, **API Secret**.
2. Trong `.env`: `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` (API Secret là bí mật, chỉ để trong `.env`).
   Chạy bằng IntelliJ thì đặt 3 biến này trong run config của identity-service.
3. Khởi động lại identity-service.

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

**Trang demo** (thay frontend khi dev: đăng ký, đăng nhập, Google, làm mới phiên, đăng xuất, quên mật khẩu, đổi email,
hồ sơ của tôi, ảnh đại diện, đổi mật khẩu, thiết bị đăng nhập, quản trị tài khoản;
liên kết kích hoạt / đặt lại mật khẩu / xác nhận đổi email trong email mở đúng trang này, ở `/verify-email`,
`/reset-password`, `/confirm-email-change`):
```bash
node scripts/demo-xac-thuc/server.js      # rồi mở http://localhost:5173 (Chrome/Edge/Firefox)
```
Cần gateway + identity-service đang chạy. Trang đọc `GOOGLE_CLIENT_ID` từ biến môi trường hoặc từ `.env`.
Các ô "cần đăng nhập" dùng access token của lần đăng nhập gần nhất trên trang (đăng nhập bằng tài khoản mẫu ở mục
"Tài khoản và token"); ô "Quản trị tài khoản" cần đăng nhập `admin@eclinic.local`, vai trò khác sẽ thấy 403.
Kết quả của mỗi lần gọi API hiện ở ô "Kết quả gọi API gần nhất" cuối trang.

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
   node scripts/demo-xac-thuc/server.js              # để cửa sổ này chạy, mở http://localhost:5173
   ```
   - Máy đã có MySQL khác chiếm cổng 3306: đặt `DB_PORT=3307` trong `.env` (chỉ đổi cổng mở ra máy host).
   - Kiểm tra đúng chế độ: `docker compose --profile app logs identity-service | grep -E "CONSOLE|GOOGLE_CLIENT_ID"`
     KHÔNG được ra dòng nào (có dòng = vẫn ở chế độ console hoặc thiếu Client ID).
   - Dùng Chrome/Edge/Firefox (Safari không gửi cookie `Secure` qua http://localhost).
2. **Email:**
   - [ ] Đăng ký (email nhận được thư, mật khẩu ≥ 6 ký tự, số điện thoại 10 số bắt đầu bằng 0) -> nhận thư
     "[eClinic] Kích hoạt tài khoản" (xem cả thư rác).
   - [ ] Bấm liên kết trong thư -> trang báo "Kích hoạt thành công".
   - [ ] Về trang chính: Đăng nhập -> Làm mới phiên -> Đăng xuất, mỗi bước 200 ở ô "Kết quả gọi API gần nhất".
   - [ ] Quên mật khẩu -> nhận thư "[eClinic] Đặt lại mật khẩu" -> bấm liên kết -> nhập mật khẩu mới 2 lần -> 200.
   - [ ] Nhận thư "[eClinic] Mật khẩu đã được thay đổi".
   - [ ] Đăng nhập bằng mật khẩu mới -> 200; mật khẩu cũ -> 401.
3. **Google:**
   - [ ] Bấm nút Google, chọn tài khoản -> 200, hiện đúng tên.
     (Cùng Gmail với bước 2 thì được liên kết vào tài khoản đó, cùng id — đúng như thiết kế.)
   - [ ] F12 > Application > Cookies > `http://localhost:8080`: có `eclinic_rt` (HttpOnly, Secure).
   - [ ] Làm mới phiên -> 200; Đăng xuất -> 200 và cookie `eclinic_rt` biến mất.
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
(Đổi mật khẩu, Đổi email, Ảnh đại diện, Quản trị tài khoản), dùng cách nào cũng được.

1. **Đổi mật khẩu** (Swagger, `PUT /api/users/me/change-password`):
   - [ ] 200, nhận thư "[eClinic] Mật khẩu đã được thay đổi"; đăng nhập bằng mật khẩu mới -> 200.
2. **Đổi email** (trang demo, ô "Đổi email", cần một hộp thư thật thứ hai):
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
