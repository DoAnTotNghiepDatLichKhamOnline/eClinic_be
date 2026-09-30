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
- Toàn bộ entity, enum, exception, bảo mật dùng chung và Flyway migration nằm trong module `common`.
- Cách viết code (đặt tên, thêm API mới, migration...): xem [docs/QUY-UOC-CODE.md](docs/QUY-UOC-CODE.md).

Công nghệ: Java 17, Spring Boot 4.1, Spring Cloud Gateway (WebMVC), Spring Security (JWT HS256), Spring Data JPA,
Flyway, MySQL 8.4, Redis, springdoc (Swagger), Maven, Docker Compose.

## Các module

| Module | Cổng | Chức năng (mã yêu cầu) | Swagger |
|---|---|---|---|
| `api-gateway` | 8080 | Cổng vào duy nhất cho frontend: định tuyến + CORS | (không có) |
| `identity-service` | 8081 | AUTH-01..04 | http://localhost:8081/swagger-ui.html |
| `catalog-service` | 8082 | ADM-01, ADM-02, AUTH-04 (hồ sơ bác sĩ), DOC-01, DOC-03 | http://localhost:8082/swagger-ui.html |
| `booking-service` | 8084 | BOOK-01..12, SCHED-01..05, PAT-01..03, ADM-03, ADM-04 | http://localhost:8084/swagger-ui.html |
| `medical-service` | 8085 | EXAM-01..05, DOC-02 | http://localhost:8085/swagger-ui.html |
| `notification-service` | 8086 | NOTI-01, gửi email | http://localhost:8086/swagger-ui.html |
| `chatbot-service` | 8087 | UC-CHAT-01 (ngoài MVP) | http://localhost:8087/swagger-ui.html |
| `report-service` | 8088 | DASH-01..04, ADM-05 | http://localhost:8088/swagger-ui.html |
| `common` | – | Thư viện dùng chung (không chạy riêng) | – |

Cổng 8083 bỏ trống (lịch làm việc đã gộp vào `booking-service`). phpMyAdmin: http://localhost:8090.

Qua gateway, mọi API có dạng `http://localhost:8080/api/<service>/...`, ví dụ
`http://localhost:8080/api/catalog/chuyen-khoa`.

## Cần cài

- **JDK 17** (IntelliJ tự tải được: File → Project Structure → SDK → Download JDK, chọn bản 17).
- **IntelliJ IDEA** (bản miễn phí là đủ).
- **Docker Desktop** (MySQL, Redis; và để chạy test).
- **Node.js** (không bắt buộc): chỉ cần cho script tạo token bác sĩ `scripts/tao-token-dev.js`.

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
- **Token bác sĩ**: chưa có API cấp tài khoản bác sĩ (AUTH-04), tạm tạo token bằng script:
  ```bash
  node scripts/tao-token-dev.js BAC_SI 5          # bác sĩ có idTaiKhoan = 5
  ```
  Token của script hết hạn sau 8 giờ.
- Để trống `JWT_SECRET` trong `.env` (mặc định đang comment): khi đó IntelliJ, Docker và script dùng chung một khoá.
- Rà soát bảo mật, rủi ro đang chấp nhận, việc phải làm trước khi triển khai thật và hướng dẫn cho frontend:
  [docs/BAO-MAT-XAC-THUC.md](docs/BAO-MAT-XAC-THUC.md).

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

**Trang demo** (thay frontend khi dev: đăng ký, đăng nhập, Google, làm mới phiên, đăng xuất, quên mật khẩu;
liên kết kích hoạt / đặt lại mật khẩu trong email mở đúng trang này):
```bash
node scripts/demo-xac-thuc/server.js      # rồi mở http://localhost:5173 (Chrome/Edge/Firefox)
```
Cần gateway + identity-service đang chạy. Trang đọc `GOOGLE_CLIENT_ID` từ biến môi trường hoặc từ `.env`.

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
| Flyway báo `checksum mismatch` | Đã sửa file migration đã chạy. Không sửa file cũ; tạo file `V<n>__...sql` mới. Máy local có thể `docker compose down -v` để làm lại DB. |
| Service báo lỗi kết nối MySQL | Chưa `docker compose up -d`, hoặc MySQL chưa "healthy" (xem `docker compose ps`). |
| `Command line is too long` (Windows) | Run config đã đặt *Shorten command line: @argfiles*; nếu tự tạo config mới thì chọn như vậy. |
