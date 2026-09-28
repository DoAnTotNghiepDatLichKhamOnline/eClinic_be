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
- **Node.js**: chỉ cần cho script tạo token `scripts/tao-token-dev.js`.

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
- **Chưa có API đăng nhập** (AUTH-02). Để thử API cần đăng nhập trên Swagger, tạo token bằng:
  ```bash
  node scripts/tao-token-dev.js                   # quản trị viên, idTaiKhoan = 1
  node scripts/tao-token-dev.js BAC_SI 5          # bác sĩ có idTaiKhoan = 5
  node scripts/tao-token-dev.js BENH_NHAN 12
  ```
  Dán token vào nút **Authorize** của Swagger (không cần gõ `Bearer `). Token hết hạn sau 8 giờ.
- Để trống `JWT_SECRET` trong `.env` (mặc định đang comment): khi đó IntelliJ, Docker và script dùng chung một khoá.

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
