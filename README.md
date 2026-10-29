# eClinic backend

Backend của đồ án tốt nghiệp **eClinic** (Nhóm 35): hệ thống đặt lịch khám bệnh trực tuyến cho bệnh viện đa khoa.
Bệnh nhân / khách đặt và quản lý lịch khám, bác sĩ quản lý lịch làm việc và ghi bệnh án, quản trị viên quản lý danh mục,
tài khoản và xem báo cáo. Kiến trúc **service-based**: nhiều service Spring Boot chạy riêng, dùng chung một MySQL và một
Redis. Frontend chỉ gọi qua **api-gateway**.

```
React (5173)  ──►  api-gateway :8080  ──►  identity :8081   catalog :8082   booking :8084   medical :8085
                                           notification :8086   chatbot :8087   report :8088
                                                          │
                                              MySQL 8.4 (1 database) + Redis
```

Công nghệ: Java 17, Spring Boot 4.1, Spring Cloud Gateway (WebMVC), Spring Security (JWT), Spring Data JPA, Flyway,
MySQL 8.4, Redis, springdoc (Swagger), Maven, Docker Compose, Caddy (HTTPS khi triển khai).

## Các service

| Module | Cổng | Chức năng | Swagger |
|---|---|---|---|
| `api-gateway` | 8080 | Cổng vào duy nhất: định tuyến, CORS, giới hạn tần suất | – |
| `identity-service` | 8081 | Đăng ký, đăng nhập (cả Google), tài khoản, đổi mật khẩu / email, ảnh đại diện, quản trị tài khoản, dữ liệu mẫu | http://localhost:8081/swagger-ui.html |
| `catalog-service` | 8082 | Chuyên khoa, phòng khám, hồ sơ bác sĩ | http://localhost:8082/swagger-ui.html |
| `booking-service` | 8084 | Đặt / đổi / huỷ lịch hẹn, ca làm việc, hàng đợi, hồ sơ bệnh nhân, đánh giá, quy tắc đặt lịch, nhắc lịch | http://localhost:8084/swagger-ui.html |
| `medical-service` | 8085 | Khám bệnh, hồ sơ bệnh án, danh mục thuốc | http://localhost:8085/swagger-ui.html |
| `notification-service` | 8086 | Thông báo trong ứng dụng, gửi email | http://localhost:8086/swagger-ui.html |
| `chatbot-service` | 8087 | Trợ lý AI (API kiểu OpenAI, mặc định Gemini) | http://localhost:8087/swagger-ui.html |
| `report-service` | 8088 | Báo cáo thống kê theo kỳ, xuất Excel | http://localhost:8088/swagger-ui.html |
| `common` | – | Thư viện dùng chung: entity, bảo mật, Flyway migration | – |

Qua gateway, API có dạng `http://localhost:8080/api/<service>/...` (vd `/api/catalog/chuyen-khoa`). Riêng `/api/auth/...`
và `/api/users/...` thuộc `identity-service`. phpMyAdmin: http://localhost:8090.

## Cách dùng

Cần cài: **JDK 17**, **IntelliJ IDEA**, **Docker Desktop**. Copy `.env.example` thành `.env` (giữ nguyên là chạy được).

**Cách 1 — chạy service trong IntelliJ (khi code)**

```bash
docker compose up -d          # chỉ MySQL + Redis + phpMyAdmin
```

Mở `pom.xml` ở thư mục gốc bằng IntelliJ, chọn run config **`Tất cả service + gateway`** (có sẵn trong `.run/`) rồi Run.
Service đọc `.env` ở thư mục gốc; biến đặt trong run config được ưu tiên hơn.

**Cách 2 — chạy toàn bộ trong Docker (demo, test frontend)**

```bash
docker compose --profile app up -d --build     # build và chạy 8 ứng dụng
docker compose --profile app ps                 # chờ tất cả "healthy"
docker compose --profile app down               # dừng
```

Không chạy cùng lúc với Cách 1 (trùng cổng 8080–8088).

**Tài khoản và token**

| Email | Mật khẩu | Vai trò |
|---|---|---|
| `admin@eclinic.local` | `Admin@123` (`ADMIN_PASSWORD`) | Quản trị viên |
| `bacsi01@eclinic.local` … `bacsi12@eclinic.local` | `Demo@123` | Bác sĩ |
| `benhnhan01@eclinic.local` … `benhnhan04@eclinic.local` | `Demo@123` | Bệnh nhân (đã kích hoạt) |
| `benhnhan05@eclinic.local` / `benhnhan06@eclinic.local` | `Demo@123` | Chưa xác thực email / bị vô hiệu hoá |

Tài khoản mẫu chỉ có khi `SEED_DATA=true` (bật sẵn ở docker compose và run config IntelliJ). Bác sĩ do quản trị viên
thêm dùng mật khẩu ban đầu `Doctor@123` và phải đổi ở lần đăng nhập đầu.

Lấy token: `POST /api/auth/login` với `{"email": "admin@eclinic.local", "matKhau": "Admin@123"}`, copy
`duLieu.accessToken` vào nút **Authorize** của Swagger. Access token hết hạn sau 30 phút; refresh token nằm trong cookie
HttpOnly `eclinic_rt`, làm mới bằng `POST /api/auth/refresh-token`.

**Chạy test** (cần Docker, test dùng Testcontainers)

```bash
./mvnw clean verify                      # mvnw.cmd trên Windows
./mvnw -pl catalog-service -am verify    # một service
```

Máy không cài JDK (Git Bash):

```bash
MSYS_NO_PATHCONV=1 docker run --rm -v "$PWD":/workspace -w /workspace \
  -v /var/run/docker.sock:/var/run/docker.sock -v eclinic-m2:/root/.m2 \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  maven:3.9-eclipse-temurin-17 mvn -B -ntp clean verify
```

Lần chạy đủ gần nhất (29/10/2026): BUILD SUCCESS, 292 test, 0 lỗi.

## Triển khai

- **Tự động triển khai**: mỗi lần push vào `main`, GitHub Actions ([.github/workflows/deploy.yml](.github/workflows/deploy.yml))
  chạy toàn bộ test rồi cập nhật máy chủ đúng commit đó. Test lỗi thì không triển khai.
- **Migration xoá bảng (V37–V45, gộp 33 → 22 bảng)**: lần đầu `main` có các migration này, service khởi động sẽ chép dữ
  liệu sang chỗ mới rồi xoá 11 bảng cũ trên DB máy chủ. Sau đó triển khai lại commit cũ sẽ không chạy được (Hibernate
  `validate` báo thiếu bảng). Trước khi merge: lấy bản dump của VM bằng `deploy\backup-restore.ps1 -Pull`, nên chạy thử
  bản dump đó với commit mới (`deploy\backup-up.ps1 -Restore`, mở `/actuator/health`). Cần quay lại: nạp lại bản dump đó
  rồi triển khai commit cũ.
- **Dự phòng trên laptop**: máy chủ gặp sự cố thì chạy bản sao trên laptop qua Cloudflare Tunnel, xem
  [deploy/BACKUP.md](deploy/BACKUP.md).

## Lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| `Port 808x was already in use` | Service đang chạy ở chỗ khác (IntelliJ và Docker cùng lúc?). Dừng một bên. |
| Gateway trả 503 `DICH_VU_KHONG_KHA_DUNG` | Service đích chưa chạy hoặc chưa khởi động xong. |
| 401 `CHUA_DANG_NHAP` dù đã dán token | Token hết hạn hoặc khác `JWT_SECRET`. Đăng nhập lại. |
| Không đăng nhập được `bacsi01@eclinic.local` | `SEED_DATA` đang tắt, hoặc DB đã có bác sĩ trước khi bật. |
| Flyway báo `checksum mismatch` | Đã sửa migration đã chạy. Tạo file `V<n>__...sql` mới; máy local có thể `docker compose down -v`. |
