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
- **Node.js** (không bắt buộc): chỉ cần cho trang demo `scripts/demo-xac-thuc/server.js` (xác thực, tài khoản và đặt lịch khám).

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
Cùng server này còn có trang demo đặt lịch khám ở http://localhost:5173/dat-lich và trang demo lịch hẹn (bệnh nhân, bác
sĩ) ở http://localhost:5173/lich-hen (xem mục "Đặt lịch khám").

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
| `GET /api/booking/phieu-kham/{maPhieuKham}` | Công khai (ai có mã cũng xem được) | Phiếu khám; CCCD và số điện thoại đã che; trẻ dưới 18 tuổi có người giám hộ và `canNguoiGiamHoDiCung` |
| `GET /api/booking/phieu-kham/{maPhieuKham}/qr?kichThuoc=&taiVe=` | Công khai | Ảnh PNG mã QR của link phiếu khám (100–1000 px, mặc định 300; `taiVe=true` để tải về) |
| `GET /api/booking/lich-hen/cua-toi?loc=TAT_CA\|SAP_TOI\|LICH_SU&cuaAi=TAT_CA\|BAN_THAN\|NGUOI_KHAC&trang=&kichThuoc=` | `BENH_NHAN` | Lịch tài khoản đã đặt (cho bản thân hoặc người thân), lịch của hồ sơ bệnh nhân đã liên kết (kể cả lịch đặt như khách) và lịch của người khám có người giám hộ khai số CCCD của hồ sơ đó; kèm `maTraCuu`, link phiếu khám. `cuaAi`: `BAN_THAN` = người khám là chủ tài khoản (bất kể ai đặt), `NGUOI_KHAC` = còn lại. Mỗi dòng có `laBanThan`, `nguoiDat` (`TOI` / `KHACH` / `TAI_KHOAN_KHAC`) và `thongTinKhacHoSo` (lịch của tôi mà người đặt nhập họ tên / ngày sinh khác hồ sơ của tôi) |
| `GET /api/booking/lich-hen/cua-toi/lich?tuNgay=&denNgay=&cuaAi=` | `BENH_NHAN` | Lịch hẹn của tài khoản theo khoảng ngày cho màn hình lịch: mọi trạng thái, theo giờ khám, không phân trang, tối đa 42 ngày (lưới tháng 6 tuần); dòng như "lịch hẹn của tôi" |
| `GET /api/booking/bac-si/toi/lich-hen/lich?tuNgay=&denNgay=` | `BAC_SI` | Lịch hẹn của chính bác sĩ theo khoảng ngày cho màn hình lịch (tối đa 42 ngày), dòng như danh sách trong ngày |
| `GET /api/booking/lich-hen/cua-toi/{maPhieuKham}` | `BENH_NHAN` | Chi tiết 1 lịch hẹn tài khoản được xem: `lichHen` (như dòng danh sách), `ngaySinhBenhNhan`, `gioiTinhBenhNhan`, `soDienThoaiLienHe`, `emailLienHe` (2 trường liên hệ chỉ có khi chính tài khoản này đặt). Lịch không thuộc tài khoản -> 404 |
| `GET /api/booking/trang-ca-nhan/cua-toi` | `BENH_NHAN` | Trang cá nhân trong 1 lần gọi: `hoSo`, `emailTaiKhoan`, `nguoiThan[]`, `lichSapToi` {`cuaToi[]`, `cuaNguoiKhac[]`} (5 lịch gần nhất mỗi bên), `lanKhamGanDay[]` (5 lượt đã khám gần nhất kèm kết quả), `soLich` {`sapToiCuaToi`, `sapToiCuaNguoiKhac`, `lichSu`, `daKham`} |
| `GET /api/booking/lich-su-kham/cua-toi?cuaAi=TAT_CA\|BAN_THAN\|NGUOI_KHAC&trang=&kichThuoc=` | `BENH_NHAN` | Lịch sử khám: các lượt đã khám xong mà tài khoản được xem kết quả, mới nhất trước. Mỗi dòng: `lichHen` (như dòng "lịch hẹn của tôi") và `ketQua` {`chanDoan`, `ghiChu`, `ngayTaiKhamDeXuat`, `donThuoc[]`}. Được xem kết quả khi người khám là chủ tài khoản (bất kể ai đặt) hoặc chính tài khoản đã đặt lịch; lịch chỉ thấy vì là người giám hộ theo CCCD thì không có kết quả. `GET .../lich-hen/cua-toi/{maPhieuKham}` cũng trả `ketQua` theo cùng quy tắc |
| `GET /api/booking/ho-so-benh-nhan/cua-toi`, `PUT /api/booking/ho-so-benh-nhan/cua-toi` | `BENH_NHAN` | Xem, tạo, sửa hồ sơ bệnh nhân của tài khoản; số CCCD không đổi được sau khi đã có hồ sơ. Số CCCD đã có hồ sơ chưa gắn tài khoản: khớp thì liên kết, không khớp trả `trangThaiLienKet: CHO_XAC_MINH` |
| `GET /api/booking/thong-tin-dat-lich/cua-toi`, `PUT .../cua-toi/nguoi-than/{id}`, `DELETE .../cua-toi/nguoi-than/{id}` | `BENH_NHAN` | Điền sẵn form: `banThan`, `emailTaiKhoan`, `nguoiThan[]` đã lưu, `lanDatGanNhat`; sửa bản lưu của 1 người thân (các trường của `benhNhan` trừ `cccd`, kèm `nguoiGiamHo`?; không sửa hồ sơ bệnh nhân); bỏ 1 người thân đã lưu |
| `GET /api/booking/bac-si/toi/lich-lam-viec?tuNgay=&denNgay=`, `GET /api/booking/bac-si/toi/lich-hen?ngay=`, `GET /api/booking/bac-si/toi/lich-hen/tra-cuu?ma=` | `BAC_SI` | Ca làm việc của chính bác sĩ (tối đa 42 ngày, kèm số lượt đã đặt), bệnh nhân trong ngày, tra 1 lịch hẹn theo mã tra cứu, mã phiếu khám hoặc cả link phiếu khám (quét QR). Danh sách trong ngày: `ngay` bỏ trống = hôm nay; lọc `idLichLamViec`, `soThuTu`, `tuKhoa` (họ tên, không xét dấu / hoa thường) |
| `GET /api/booking/bac-si/toi/lich-hen/{id}/ho-so-kham`, `GET /api/booking/bac-si/toi/lich-hen/tra-cuu/ho-so-kham?ma=` | `BAC_SI` | Hồ sơ khám của bệnh nhân từ 1 lịch hẹn của chính bác sĩ (theo id, hoặc theo mã / link phiếu khám quét từ QR): `lichHen`, `hoSoBenhNhan` (số CCCD không che, `tienSuBenhLy`), `lanKhamTruoc[]` (các lần khám trước với mọi bác sĩ, kèm `ketQua` nếu đã có bệnh án). Lịch hẹn của bác sĩ khác -> 404 |
| `POST` / `PUT` / `GET /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an`, `GET /api/medical/bac-si/toi/thuoc?tuKhoa=` | `BAC_SI` | Khám bệnh (medical-service): ghi kết quả khám `{ chanDoan, ghiChu?, ngayTaiKhamDeXuat?, donThuoc[]? }` cho lịch hẹn của chính bác sĩ, từ ngày khám trở đi; lịch hẹn chuyển sang `DA_HOAN_THANH` trong cùng transaction. `PUT` sửa kết quả đã ghi (thay toàn bộ đơn thuốc), `GET` xem lại; gợi ý tên thuốc trong danh mục. Hồ sơ khám ở booking-service hiện `ketQua` và `lanKhamTruoc[].ketQua` kèm `donThuoc[]` |
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
| 409 | `CHUA_DEN_NGAY_KHAM`, `LICH_HEN_KHONG_KHAM_DUOC` | (Khám bệnh) Bác sĩ ghi kết quả khám trước ngày khám; lịch hẹn đã khám xong, đã huỷ hoặc bị từ chối |
| 404 | `KHONG_TIM_THAY` | Ca khám không tồn tại; mã phiếu khám sai |

Cấu hình (đều có giá trị mặc định, xem `.env.example`): `BOOKING_MIN_LEAD` (30m), `BOOKING_MAX_DAYS` (30),
`BOOKING_MAX_ACTIVE_PER_PATIENT` (3), `BOOKING_MAX_ACTIVE_PER_PHONE` (5), `BOOKING_IP_LIMIT` (20) trong `BOOKING_IP_WINDOW` (10m),
`GATEWAY_TRUSTED_PROXIES`; link phiếu khám dựng từ `FRONTEND_URL`. Rủi ro và việc cần làm trước khi triển khai thật:
[docs/BAO-MAT-XAC-THUC.md](docs/BAO-MAT-XAC-THUC.md) mục 7.

**Trang demo đặt lịch** (cần gateway, identity-service, catalog-service, booking-service đang chạy và dữ liệu mẫu):
```bash
node scripts/demo-xac-thuc/server.js      # rồi mở http://localhost:5173/dat-lich (Chrome/Edge/Firefox)
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
4. **Bệnh nhân:** đăng nhập `benhnhan01@eclinic.local` / `Demo@123` (hoặc đăng nhập ở trang demo xác thực rồi mở trang này)
   -> "Đặt cho bản thân" điền sẵn từ hồ sơ; đặt xong lịch hiện ở "Lịch hẹn của tôi" (lọc Tất cả / Sắp tới / Lịch sử).
   "Đặt cho người thân": nhập người mới (mặc định được lưu) hoặc chọn **người thân đã lưu** để điền sẵn cả người khám lẫn
   người giám hộ. Lần mở trang sau, chuyên khoa và bác sĩ của lần đặt gần nhất được chọn sẵn.
5. **Liên kết theo CCCD:** đặt 1 lịch như khách; ở trang demo xác thực đăng ký tài khoản với cùng họ tên, số điện thoại và
   số CCCD đó, kích hoạt, đăng nhập -> lịch đã đặt như khách nằm trong "Lịch hẹn của tôi". Đăng ký với số điện thoại khác
   -> ô hồ sơ ghi "đang chờ phòng khám xác minh"; quản trị viên duyệt bằng `POST /api/booking/quan-tri/ho-so-benh-nhan/{id}/duyet`.
6. Khung giờ hết chỗ hiện "Hết chỗ" và không bấm được. Mọi lần gọi API hiện ở ô "Kết quả gọi API gần nhất" cuối trang.

**Trang demo lịch hẹn** (cùng server, mở http://localhost:5173/lich-hen; cần thêm medical-service đang chạy và **internet**: thư
viện lịch FullCalendar 6 và, ở trình duyệt chưa tự đọc được QR, thư viện jsQR được tải từ CDN jsdelivr). Trang hiện theo vai trò
của tài khoản đang đăng nhập (dùng chung phiên với 2 trang demo kia):
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

**Danh sách kiểm tra trên màn hình cho trang `/lich-hen`** (DOANTOTNGH-6; phần API đã kiểm tra tự động, phần dưới đây phải
nhìn trên trình duyệt). Chuẩn bị: chạy gateway, identity-service, booking-service, medical-service với `SEED_DATA=true`, chạy
`node scripts/demo-xac-thuc/server.js`, máy có internet. Đặt sẵn ở `/dat-lich` bằng `benhnhan01@eclinic.local`: 1 lịch cho bản thân,
1 lịch cho người thân, và 1 lịch cho ca **hôm nay** (để bác sĩ ghi kết quả khám); ghi lại bác sĩ của các ca đó.

- [ ] **1. Lịch của bệnh nhân.** Đăng nhập `benhnhan01` ở `/lich-hen`. Lịch hẹn hiện ở cả 4 chế độ Tháng / Tuần / Ngày / Lịch
      biểu; lịch của tôi và của người khác khác màu, lịch đã khám màu xanh lá. Bấm 1 lịch hẹn -> hiện chi tiết; lịch đã khám hiện
      kết quả khám. Đổi bộ lọc Tất cả / Lịch của tôi / Lịch của người khác -> lịch đổi theo.
- [ ] **2. Trang cá nhân của bệnh nhân.** Hiện hồ sơ bệnh nhân, 4 số đếm lịch hẹn, người thân đã lưu, các lần khám gần đây. Bấm
      **Sửa** 1 người thân, đổi họ tên, **Lưu** -> bảng hiện tên mới. Bấm **Bỏ** -> người đó biến mất khỏi bảng.
- [ ] **3. Lịch và danh sách của bác sĩ.** Đăng xuất, đăng nhập bằng bác sĩ của ca đã đặt (`bacsiNN@eclinic.local` / `Demo@123`).
      Lịch hiện ca làm việc (kèm số lượt đã đặt / tổng) và lịch hẹn. Bấm 1 ngày hoặc 1 ca -> danh sách bệnh nhân của ngày đó. Lọc
      theo **số thứ tự**, theo **họ tên gõ không dấu** -> chỉ còn dòng khớp. Bấm 1 dòng -> mở hồ sơ khám, có mục "Các lần khám trước".
- [ ] **4. Thao tác của bác sĩ trong hồ sơ khám.** Sửa họ tên trong "Sửa hồ sơ bệnh nhân", **Lưu** -> hồ sơ hiện tên mới. Với lịch
      có cảnh báo "khác hồ sơ" (đặt bằng số CCCD đã có nhưng gõ tên khác): bấm **Đã đối chiếu giấy tờ** -> cảnh báo mất. Với lịch
      của **hôm nay**: nhập chẩn đoán, thêm 1 dòng thuốc (gõ 2 ký tự trở lên của tên thuốc đã từng kê -> có gợi ý), **Lưu kết quả
      khám** -> lịch hẹn thành "Đã hoàn thành", form chuyển sang chế độ sửa. Lịch của ngày sau -> báo `CHUA_DEN_NGAY_KHAM`.
- [ ] **5. Mã QR.** Mở phiếu khám của 1 lịch hẹn thuộc bác sĩ đang đăng nhập (link "Mở phiếu khám" ở chi tiết lịch hẹn của bệnh
      nhân) để có mã QR. Ở màn hình bác sĩ: **Quét QR bằng camera** rồi đưa mã vào -> mở đúng hồ sơ khám; chụp / lưu ảnh mã QR rồi
      **chọn ảnh** -> mở đúng hồ sơ khám; dán mã `ECL-…` hoặc cả link phiếu khám vào ô mã -> mở đúng hồ sơ khám. Mã của bác sĩ
      khác -> báo không tìm thấy.

Đối chiếu từng màn hình của frontend với API: [docs/BAN-GIAO-FRONTEND-05-10.md](docs/BAN-GIAO-FRONTEND-05-10.md).

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
