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

Lần chạy đủ gần nhất (04/11/2026): BUILD SUCCESS, 75 test, 0 lỗi, khoảng 8 phút.

## Triển khai

- **CI**: mỗi PR vào `main` và mỗi lần push lên nhánh khác, GitHub Actions ([.github/workflows/ci.yml](.github/workflows/ci.yml))
  chạy toàn bộ test (job **Test**). Test lỗi thì tải báo cáo surefire ở mục *Artifacts* của lần chạy đó (giữ 7 ngày).
- **Bắt buộc CI xanh trước khi merge** (làm tay một lần, cần quyền admin repo): *Settings → Rules → Rulesets → New branch
  ruleset* → *Target branches*: thêm `main` → bật *Require a pull request before merging* và *Require status checks to
  pass*, thêm check **Test** → *Enforcement status*: Active → *Create*. Repo chưa dùng Rulesets thì làm tương tự ở
  *Settings → Branches → Add branch protection rule*.
- **Cập nhật thư viện (Dependabot)**: mỗi thứ Hai, [.github/dependabot.yml](.github/dependabot.yml) mở PR cập nhật
  Maven (minor/patch gộp một PR, mỗi bản major một PR riêng), GitHub Actions (gộp một PR), image trong `Dockerfile` và
  các file compose (không đề xuất bản major, ví dụ giữ JDK 17, MySQL 8.4). CI chạy test trên từng PR. Không tự merge:
  merge vào `main` là triển khai lên máy chủ, nên xem PR rồi merge tay. Nên bật thêm *Settings → Code security →
  Dependabot alerts* và *Dependabot security updates*.
- **Môi trường `production`** (làm tay một lần, cần quyền admin repo): job triển khai và rollback chạy trong environment
  `production` (GitHub tự tạo ở lần chạy đầu). Vào *Settings → Environments → production*:
  1. *Deployment branches and tags* → *Selected branches and tags* → thêm `main`.
  2. (Tuỳ chọn) *Required reviewers*: mỗi lần triển khai phải có người bấm duyệt.
  3. Chuyển secret: thêm `VM_HOST`, `VM_SSH_KEY`, `VM_SSH_FINGERPRINT` vào *Environment secrets* của `production`
     **trước**, rồi mới xoá 3 secret đó ở *Settings → Secrets and variables → Actions → Repository secrets*. Làm ngược thứ
     tự thì lần triển khai ở giữa sẽ thiếu secret.

  Workflow cũng tự chặn: Deploy chạy tay từ nhánh khác `main` chỉ chạy test, Rollback từ nhánh khác `main` báo lỗi.
- **Tự động triển khai**: mỗi lần push vào `main`, GitHub Actions ([.github/workflows/deploy.yml](.github/workflows/deploy.yml))
  chạy toàn bộ test (gọi lại `ci.yml`) rồi cập nhật máy chủ đúng commit đó. Test lỗi thì không triển khai.
- **Image build trên CI, VM chỉ pull**: song song với test, job **Build images** build 8 image theo
  [docker-bake.hcl](docker-bake.hcl) (Maven chạy một lần cho cả 8) và đẩy lên GHCR:
  `ghcr.io/doantotnghiepdatlichkhamonline/eclinic-<service>:<commit SHA đầy đủ>`. VM chỉ pull 8 image ứng dụng (không pull
  lại mysql/redis/caddy/phpmyadmin), chạy `up --no-build`, rồi xoá image của commit cũ. Tag đang chạy nằm ở dòng
  `IMAGE_TAG=` trong `.env` trên VM (deploy tự ghi), nên lệnh tay trên VM vẫn chạy như cũ:
  `docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile app ps`. Báo lỗi
  `IMAGE_TAG is not set` nghĩa là `.env` thiếu dòng đó: thêm `IMAGE_TAG=$(git rev-parse HEAD)`.
  Dọn một lần trên VM sau lần triển khai đầu theo cách mới (image build tại chỗ và cache build cũ):
  `docker image rm $(docker images --format '{{.Repository}}' | grep -xE 'eclinic-([a-z]+-service|api-gateway)' | sort -u)` rồi
  `docker builder prune -af`.
- **Tự động sao lưu DB trước mỗi lần triển khai**: trước khi khởi động lại stack (Flyway chạy lúc khởi động),
  [deploy/vm-backup.sh](deploy/vm-backup.sh) dump MySQL trên VM vào `~/eclinic-backups/eclinic-<giờ UTC>-<commit đang
  chạy>-to-<commit mới>.sql.gz`, giữ 10 bản mới nhất. Dump lỗi thì VM quay về commit cũ và dừng triển khai. Không cần tự
  dump trước khi merge nữa. Lấy một bản về laptop:
  `scp -i $HOME\.ssh\eclinic_gcp huythanhyasuo@<IP_VM>:~/eclinic-backups/<file> backups\` rồi
  `deploy\backup-restore.ps1 -Dump backups\<file>`.
- **Quay lại bản cũ (Rollback)**: *Actions → Rollback → Run workflow* ([.github/workflows/rollback.yml](.github/workflows/rollback.yml)),
  nhập `sha` là một commit trên `main` (SHA ngắn hoặc đầy đủ). Workflow kiểm tra commit nằm trên `main` và đủ 8 image trên
  GHCR (chỉ các commit triển khai sau khi image chuyển sang build trên CI mới có), dump DB hiện tại, rồi chạy lại image của
  commit đó. Nếu giữa hai commit có migration đổi schema thì phải bật `restore_db` và gõ `restore-db` vào `confirm`:
  workflow dừng các service, xoá và tạo lại DB, nạp bản dump `…-<commit đó>-to-…` (chụp lúc commit đó đang chạy), rồi xoá
  Redis (cache, giới hạn tần suất, token liên kết). Dữ liệu phát sinh sau thời điểm dump sẽ mất, nhưng vẫn còn trong bản
  dump vừa chụp trước khi rollback. Không bật `restore_db` mà schema khác thì service không khởi động được (Hibernate
  `validate`). Lần push vào `main` tiếp theo sẽ triển khai lại `main`: muốn giữ bản cũ thì `git revert` commit lỗi trên
  `main`.
- **Migration xoá bảng (V37–V45, gộp 33 → 22 bảng)**: lần đầu `main` có các migration này, service khởi động sẽ chép dữ
  liệu sang chỗ mới rồi xoá 11 bảng cũ trên DB máy chủ. Sau đó triển khai lại commit cũ sẽ không chạy được (Hibernate
  `validate` báo thiếu bảng). Với migration rủi ro như vậy, nên chạy thử trước: lấy dump mới của VM
  (`deploy\backup-restore.ps1 -Pull`), chạy với commit mới (`deploy\backup-up.ps1 -Restore`, mở `/actuator/health`).
  Cần quay lại: chạy workflow Rollback với `restore_db` (xem mục trên).
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
