# Bảo mật xác thực — eClinic backend

Tài liệu rà soát bảo mật cho phần xác thực (identity-service, DOANTOTNGH-2): đối chiếu OWASP, rủi ro đang chấp nhận,
việc phải làm trước khi triển khai thật và hướng dẫn cho frontend.
Đối chiếu với OWASP Cheat Sheet: Authentication, Forgot Password, Session Management, JSON Web Token, Logging.

## 1. Các API

Tất cả qua gateway `http://localhost:8080`, đều công khai (không cần access token). Body/response theo `PhanHoiApi`.

| API | Body | Kết quả chính | Lỗi hay gặp (`maLoi`) |
|-----|------|---------------|-----------------------|
| `POST /api/auth/register` | `hoTen, email, matKhau, soDienThoai` | 201, gửi email kích hoạt | 409 `EMAIL_DA_TON_TAI`, `SO_DIEN_THOAI_DA_TON_TAI`; 429 `GUI_LAI_QUA_NHANH` |
| `POST /api/auth/verify-email` | `token` | 200, tài khoản được kích hoạt | 410 `LIEN_KET_KHONG_HOP_LE`; 403 `TAI_KHOAN_BI_VO_HIEU_HOA` |
| `POST /api/auth/resend-verification` | `email` | 200 (luôn cùng 1 thông báo) | — |
| `POST /api/auth/login` | `email, matKhau` | 200: `accessToken` trong body, refresh token trong cookie | 401 `SAI_THONG_TIN_DANG_NHAP`; 403 `TAI_KHOAN_CHUA_XAC_THUC`, `TAI_KHOAN_BI_VO_HIEU_HOA`; 429 `DANG_NHAP_SAI_QUA_NHIEU` |
| `POST /api/auth/google` | `idToken` (credential của Google Identity Services) | như login | 401 `GOOGLE_TOKEN_KHONG_HOP_LE`; 403 `DANG_NHAP_GOOGLE_KHONG_HO_TRO`, `TAI_KHOAN_BI_VO_HIEU_HOA`; 409 `EMAIL_DA_DANG_KY_MAT_KHAU`, `XUNG_DOT_DU_LIEU`; 503 `DANG_NHAP_GOOGLE_KHONG_KHA_DUNG` |
| `POST /api/auth/refresh-token` | không cần (cookie); hoặc `refreshToken` khi thử bằng Swagger/curl | 200: access token mới + cookie mới | 401 `PHIEN_DANG_NHAP_KHONG_HOP_LE`; 403 `TAI_KHOAN_BI_VO_HIEU_HOA` |
| `POST /api/auth/logout` | không cần (cookie); hoặc `refreshToken` | 200 (luôn thành công), xoá cookie | — |
| `POST /api/auth/forgot-password` | `email` | 200 (luôn cùng 1 thông báo) | — |
| `POST /api/auth/reset-password` | `token, matKhauMoi` | 200, đăng xuất mọi thiết bị, gửi email thông báo | 410 `LIEN_KET_KHONG_HOP_LE`; 400 mật khẩu sai quy tắc (liên kết vẫn dùng được) |

**Cookie refresh token:** `eclinic_rt`, `HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800` (7 ngày,
mỗi lần làm mới cấp cookie mới). JavaScript không đọc được; trình duyệt tự gửi kèm các API `/api/auth/*`.

## 2. Đối chiếu OWASP

| Yêu cầu | Cách đáp ứng |
|---------|--------------|
| Thông báo lỗi chung, không lộ email nào đã đăng ký | Sai email và sai mật khẩu cùng 401; trạng thái tài khoản chỉ báo khi mật khẩu đúng; quên mật khẩu / gửi lại kích hoạt luôn cùng 1 câu |
| Thời gian phản hồi không lộ email | Luôn chạy BCrypt đúng 1 lần (hash giả khi không có tài khoản); email gửi bất đồng bộ sau commit |
| Lưu mật khẩu | BCrypt; tối đa 72 byte UTF-8 (BCrypt bỏ phần sau byte 72) kiểm tra khi đăng ký và khi đăng nhập |
| Chống dò mật khẩu | 5 lần sai trong 15 phút -> khoá email 15 phút (429), đăng nhập đúng thì xoá bộ đếm |
| Token trong liên kết email | 256 bit ngẫu nhiên, Redis chỉ lưu SHA-256, dùng 1 lần, gắn mục đích, hết hạn (kích hoạt 24 giờ, đặt lại 15 phút), cấp mới thì liên kết cũ hết hiệu lực, chờ 60 giây giữa 2 lần gửi |
| Liên kết không dựa vào header Host | Dựng từ `FRONTEND_URL`; liên kết trỏ về frontend, frontend POST token (không GET đổi trạng thái — trình quét email hay mở trước liên kết) |
| Sau đặt lại mật khẩu | Không tự đăng nhập; thu hồi mọi phiên; xoá khoá đăng nhập; gửi email "mật khẩu đã thay đổi" |
| Phiên đăng nhập | Access token JWT 30 phút; refresh token 7 ngày, ngẫu nhiên, DB chỉ lưu SHA-256; mỗi lần làm mới đổi token (rotation); token cũ bị dùng lại sau 10 giây -> thu hồi mọi phiên (reuse detection) |
| Lưu token ở trình duyệt | Refresh token trong cookie HttpOnly/Secure/SameSite=Strict; access token chỉ trong bộ nhớ |
| CSRF | Cookie SameSite=Strict (trình duyệt không gửi từ trang khác) + CORS chỉ cho origin cụ thể + body JSON, vì vậy không dùng CSRF token của Spring |
| Header phản hồi | `Cache-Control: no-cache, no-store`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` (mặc định Spring Security), `Referrer-Policy: no-referrer` |
| Đăng nhập Google | Kiểm tra chữ ký (khoá công khai của Google), `iss`, `aud` = Client ID, hạn dùng, `email_verified`; định danh bằng `sub`; chỉ liên kết theo email khi Google đảm bảo chủ email (@gmail.com hoặc Google Workspace); chỉ bệnh nhân |
| Chiếm tài khoản chờ kích hoạt | Đăng ký lại email đang chờ thì ghi đè, liên kết cũ hết hiệu lực; đăng nhập Google vào tài khoản chờ kích hoạt thì bỏ mật khẩu, số điện thoại, liên kết kích hoạt của người đăng ký trước |
| Không ghi bí mật ra log | Không ghi mật khẩu, token, ID token, App Password; request/response/event che token trong `toString`. Riêng chế độ `MAIL_MODE=console` ghi liên kết ra log (chỉ dùng khi dev, có cảnh báo) |
| Ghi nhật ký sự kiện bảo mật | Mục 5 |

## 3. Rủi ro đang chấp nhận

| Rủi ro | Ghi chú |
|--------|---------|
| Mật khẩu chỉ cần 6 ký tự | Đang thử nghiệm. Triển khai thật: tối thiểu 8 (tốt hơn 12), kiểm tra mật khẩu đã bị lộ |
| Đăng ký báo 409 khi email đã có | Theo đặc tả đồ án; cho biết email nào đã đăng ký (quên mật khẩu thì không) |
| Khoá đăng nhập theo email | Người khác biết email có thể cố tình sai 5 lần để khoá 15 phút |
| Chưa có CAPTCHA, giới hạn theo IP | Đăng ký, đăng nhập, quên mật khẩu chỉ giới hạn theo email / tài khoản |
| Quên mật khẩu chênh thời gian rất nhỏ | Tài khoản có thật tốn thêm vài lệnh Redis |
| Access token không thu hồi được | Đăng xuất / đặt lại mật khẩu chỉ thu hồi refresh token; access token còn sống tối đa 30 phút |
| Không giới hạn tổng thời gian phiên | Refresh token trượt 7 ngày: dùng ít nhất 1 lần/tuần thì phiên không bao giờ hết. Muốn giới hạn cần thêm cột mốc phiên (migration V3) |
| Token đã đăng xuất / đã xoay vòng bị dùng lại | Sau 10 giây bị coi là lộ -> thu hồi mọi phiên của tài khoản (không phân biệt được lý do thu hồi; cần cột `ly_do_thu_hoi`) |
| Token liên kết bị dùng trước khi commit | Commit lỗi thì liên kết mất, người dùng yêu cầu lại |
| Không kiểm tra header Origin ở làm mới phiên / đăng xuất | SameSite=Strict + CORS đã chặn request từ trang khác |
| Access token trong bộ nhớ trang | Nếu frontend có lỗi XSS, script vẫn dùng được token khi trang đang mở (không lấy được refresh token) |
| Google: email không phải Gmail / Workspace | Tài khoản Google đó vẫn tạo được tài khoản mới với email ấy (Google đã xác minh lúc tạo); chủ email thật đăng ký sau sẽ bị 409 |
| Google: email không đồng bộ | Đổi email trên Google không đổi email trong eClinic |
| Chưa có xác thực 2 lớp (MFA) | Ngoài phạm vi |

## 4. Trước khi triển khai thật

- [ ] Đặt `JWT_SECRET` (ngẫu nhiên, ≥ 32 ký tự, giống nhau ở mọi service). Khởi động mà còn khoá dev sẽ có cảnh báo WARN.
- [ ] Đặt `ADMIN_PASSWORD` mạnh trước lần khởi động đầu (hoặc đổi ngay sau đó). Còn `Admin@123` sẽ có cảnh báo WARN.
- [ ] `MAIL_MODE=smtp` với `MAIL_USERNAME`, `MAIL_APP_PASSWORD` (App Password của Gmail).
- [ ] Chạy HTTPS; `FRONTEND_URL` là `https://…`; `CORS_ALLOWED_ORIGINS` chỉ gồm frontend thật; giữ `COOKIE_SECURE=true`.
- [ ] Không mở cổng các service (8081…8088), MySQL, Redis, phpMyAdmin ra ngoài; chỉ mở gateway. Cân nhắc tắt Swagger.
- [ ] Nâng quy tắc mật khẩu (mục 3).
- [ ] Google Cloud: OAuth client "Web application" với Authorized JavaScript origins là domain frontend thật; đặt `GOOGLE_CLIENT_ID`.

## 5. Nhật ký bảo mật (log của identity-service)

Chỉ ghi id tài khoản / id phiên, không ghi email, mật khẩu, token.

| Sự kiện | Mức | Nội dung |
|---------|-----|----------|
| Đăng nhập sai | INFO | `Đăng nhập sai id=<id hoặc -> (lần sai thứ n)` |
| Bị khoá đăng nhập | WARN | `Khoá đăng nhập id=<id hoặc -> sau n lần sai mật khẩu` |
| Mật khẩu đúng nhưng tài khoản chưa kích hoạt / bị vô hiệu hoá | INFO | `Từ chối đăng nhập id=…` |
| Đăng nhập, làm mới phiên, đăng xuất | INFO | `Đăng nhập thành công id=…`, `Làm mới phiên id=… của tài khoản id=…`, `Đăng xuất phiên id=…` |
| Refresh token cũ bị dùng lại | WARN | `Refresh token đã thu hồi bị dùng lại: thu hồi n phiên của tài khoản id=…` |
| Gửi liên kết / đặt lại mật khẩu | INFO | `Gửi liên kết đặt lại mật khẩu id=…`, `Đặt lại mật khẩu id=…, thu hồi n phiên` |
| Google | INFO | `Từ chối ID token Google: <lý do>`, `Đăng nhập Google id=…`, `Liên kết Google…`, `Tạo tài khoản từ Google…`, `Kích hoạt tài khoản id=… bằng Google…`, `Từ chối liên kết Google…`, `Từ chối đăng nhập Google id=…` |

Xem nhanh: `docker logs eclinic-identity-service 2>&1 | grep -E "Đăng nhập sai|Khoá đăng nhập|Từ chối|dùng lại"`.

## 6. Hướng dẫn cho frontend

- **Gọi API xác thực với `credentials: 'include'`** (fetch) / `withCredentials: true` (axios) để trình duyệt nhận và gửi
  cookie refresh token. Không đọc, không lưu refresh token (JavaScript cũng không đọc được).
- **Access token chỉ giữ trong bộ nhớ** (state), KHÔNG lưu `localStorage` / `sessionStorage`. Gửi kèm
  `Authorization: Bearer <accessToken>` khi gọi API cần đăng nhập.
- **Tải lại trang / access token hết hạn (401):** gọi `POST /api/auth/refresh-token` (không body) để lấy access token mới;
  401 tiếp thì chuyển tới trang đăng nhập. Nhiều request cùng gặp 401 thì chỉ gọi làm mới 1 lần rồi dùng chung kết quả
  (làm mới 2 lần cùng lúc thì 1 lần bị 401).
- **Đăng xuất:** `POST /api/auth/logout` (không body) rồi xoá access token trong bộ nhớ.
- **Trang `/verify-email?token=…`:** lấy token trên URL, POST `verify-email`, xoá token khỏi thanh địa chỉ
  (`history.replaceState`). 410 -> cho nút "Gửi lại liên kết kích hoạt".
- **Trang `/reset-password?token=…`:** form nhập mật khẩu mới 2 lần; trang phải có `Referrer-Policy: no-referrer`
  (hoặc thẻ `<meta name="referrer" content="no-referrer">`), không nhúng script/ảnh bên thứ ba; 410 -> cho nút "Gửi lại
  liên kết"; thành công -> chuyển tới trang đăng nhập (không tự đăng nhập).
- **Đăng nhập Google:** dùng Google Identity Services (`accounts.google.com/gsi/client`) với cùng Client ID như
  `GOOGLE_CLIENT_ID`; callback nhận `credential` rồi POST `{ idToken: credential }` tới `/api/auth/google`.
  409 `EMAIL_DA_DANG_KY_MAT_KHAU` -> gợi ý đăng nhập bằng mật khẩu.
- **Khi dev:** frontend chạy `http://localhost:5173` (trùng `FRONTEND_URL`, nằm trong CORS). Cookie `Secure` chạy được
  trên `http://localhost` với Chrome/Edge/Firefox (Safari thì không); curl cần bản ≥ 7.84 (dùng `-c`/`-b` để giữ cookie).
  Trang mẫu: `scripts/demo-xac-thuc` (xem README).
