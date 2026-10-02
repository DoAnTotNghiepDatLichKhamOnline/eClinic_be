# Bảo mật xác thực và tài khoản — eClinic backend

Tài liệu rà soát bảo mật cho phần xác thực (DOANTOTNGH-2) và phần tài khoản (DOANTOTNGH-3: hồ sơ cá nhân, đổi mật khẩu,
thiết bị đăng nhập, ảnh đại diện, đổi email, quản trị tài khoản) của identity-service: danh sách API, đối chiếu OWASP,
rủi ro đang chấp nhận, việc phải làm trước khi triển khai thật và hướng dẫn cho frontend.
Đối chiếu với OWASP Cheat Sheet: Authentication, Forgot Password, Session Management, JSON Web Token, Logging,
File Upload. Mục 7 ghi phần bảo mật của các API đặt lịch khám công khai (DOANTOTNGH-4, booking-service).

## 1. Các API

Tất cả qua gateway `http://localhost:8080`. Body/response theo `PhanHoiApi`.

### 1.1 API công khai (không cần access token)

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
| `POST /api/auth/confirm-email-change` | `token` (trong liên kết gửi tới email mới) | 200, email đăng nhập được đổi, đăng xuất mọi thiết bị, báo cho email cũ | 410 `LIEN_KET_KHONG_HOP_LE`; 403 `TAI_KHOAN_BI_VO_HIEU_HOA`; 409 `EMAIL_DA_TON_TAI` (email mới vừa bị tài khoản khác đăng ký) |

**Cookie refresh token:** `eclinic_rt`, `HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800` (7 ngày,
mỗi lần làm mới cấp cookie mới). JavaScript không đọc được; trình duyệt tự gửi kèm các API `/api/auth/*`.

### 1.2 API của người đang đăng nhập (cần access token, mọi vai trò)

Lỗi chung của cả nhóm: 401 `CHUA_DANG_NHAP` (không có / hết hạn token, hoặc tài khoản đã bị xoá); 403
`TAI_KHOAN_BI_VO_HIEU_HOA` (tài khoản bị vô hiệu hoá sau khi token được cấp); 400 `DU_LIEU_KHONG_HOP_LE` (body sai quy tắc).

| API | Body | Kết quả chính | Lỗi hay gặp (`maLoi`) |
|-----|------|---------------|-----------------------|
| `GET /api/users/me` | — | Hồ sơ: `id, hoTen, email, soDienThoai, anhDaiDien, vaiTro, trangThai, coMatKhau, lienKetGoogle, ngayTao`; bác sĩ có thêm `bacSi`, bệnh nhân có thêm `hoSoBenhNhan` (chỉ xem) | — |
| `PUT /api/users/me` | `hoTen, soDienThoai` (bỏ trống `soDienThoai` thì giữ số cũ) | 200, hồ sơ sau khi sửa | 403 `KHONG_CO_QUYEN` (bác sĩ đổi họ tên); 409 `SO_DIEN_THOAI_DA_TON_TAI` |
| `PUT /api/users/me/change-password` | `matKhauCu, matKhauMoi` | 200, đăng xuất các thiết bị khác (thiết bị này giữ nguyên), gửi email thông báo | 400 `MAT_KHAU_CU_KHONG_DUNG`, `MAT_KHAU_MOI_TRUNG_MAT_KHAU_CU`; 409 `TAI_KHOAN_CHUA_CO_MAT_KHAU` (tài khoản chỉ đăng nhập Google: dùng Quên mật khẩu); 429 `SAI_MAT_KHAU_QUA_NHIEU` |
| `GET /api/users/me/sessions` | — | Danh sách thiết bị: `id, thietBi, dangNhapLuc, hoatDongLuc, hetHanLuc, hienTai`; thiết bị đang dùng đứng đầu | — |
| `DELETE /api/users/me/sessions/{id}` | — | 200, thiết bị đó bị đăng xuất | 400 `DU_LIEU_KHONG_HOP_LE` (là thiết bị đang dùng: gọi `/api/auth/logout`); 404 `KHONG_TIM_THAY` |
| `DELETE /api/users/me/sessions` | — | 200, `duLieu` = số thiết bị khác đã đăng xuất | — |
| `POST /api/users/me/avatar` | `multipart/form-data`, phần tệp `anh` (JPEG / PNG / WebP, tối đa 2 MB) | 200, hồ sơ với `anhDaiDien` mới | 400 `ANH_KHONG_HOP_LE`, `DU_LIEU_KHONG_HOP_LE` (thiếu phần `anh`); 413 `TEP_QUA_LON` (quá 2 MB), `YEU_CAU_QUA_LON` (quá 5 MB, do gateway trả); 429 `GUI_LAI_QUA_NHANH` (quá 10 lần / giờ); 503 `LUU_TRU_ANH_KHONG_KHA_DUNG` |
| `DELETE /api/users/me/avatar` | — | 200, hồ sơ không còn ảnh (kể cả ảnh Google); chưa có ảnh cũng 200 | — |
| `POST /api/users/me/change-email` | `emailMoi, matKhauHienTai` | 200, gửi liên kết xác nhận (hạn 1 giờ) tới email mới, báo cho email đang dùng | 400 `EMAIL_MOI_TRUNG_EMAIL_CU`, `MAT_KHAU_CU_KHONG_DUNG`; 409 `EMAIL_DA_TON_TAI`, `TAI_KHOAN_CHUA_CO_MAT_KHAU`; 429 `GUI_LAI_QUA_NHANH` (chờ 60 giây), `SAI_MAT_KHAU_QUA_NHIEU` |
| `GET /api/users/me/change-email` | — | 200, `duLieu.emailMoi` của yêu cầu đang chờ; không có thì `duLieu` vắng mặt | — |
| `DELETE /api/users/me/change-email` | — | 200 (luôn thành công), liên kết đã gửi hết hiệu lực | — |

### 1.3 API quản trị tài khoản (chỉ `QUAN_TRI_VIEN`)

Lỗi chung: 401 `CHUA_DANG_NHAP`; 403 `KHONG_CO_QUYEN` (token không phải quản trị viên, hoặc tài khoản đang gọi không còn là
quản trị viên trong DB); 404 `KHONG_TIM_THAY` (không có tài khoản với id đó).

| API | Tham số / body | Kết quả chính | Lỗi hay gặp (`maLoi`) |
|-----|----------------|---------------|-----------------------|
| `GET /api/users` | query `tuKhoa` (họ tên, email, số điện thoại), `vaiTro`, `trangThai`, `trang` (từ 0), `kichThuoc` (1–100, mặc định 20) | `TrangDuLieu`, mới tạo đứng trước; mỗi dòng: `id, hoTen, email, soDienThoai, anhDaiDien, vaiTro, trangThai, lyDoVoHieuHoa, ngaySinh, ngayTao` | 400 `DU_LIEU_KHONG_HOP_LE` (tham số sai) |
| `GET /api/users/{id}` | — | Chi tiết: các trường trên + `coMatKhau, lienKetGoogle, ngayCapNhat, trangThaiLienKetHoSo, bacSi`; không có dữ liệu y tế | — |
| `PUT /api/users/{id}/status` | `trangThai` (`VO_HIEU_HOA` hoặc `DA_KICH_HOAT`), `lyDo` (bắt buộc khi vô hiệu hoá, tối đa 500 ký tự) | 200, chi tiết tài khoản; vô hiệu hoá: đăng xuất mọi thiết bị, huỷ các liên kết đang chờ; cả hai chiều đều gửi email cho chủ tài khoản | 400 `DU_LIEU_KHONG_HOP_LE` (thiếu lý do, trạng thái `CHO_XAC_NHAN`); 403 `TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE`; 409 `TRANG_THAI_TAI_KHOAN_KHONG_HOP_LE` (tài khoản chưa xác thực email, hoặc đã ở trạng thái đó), `BAC_SI_CON_LICH_HEN` |
| `DELETE /api/users/{id}` | — | 200, tài khoản bị xoá hẳn (chỉ khi chưa có dữ liệu nào tham chiếu), ảnh đại diện trong kho ảnh được xoá theo | 403 `TAI_KHOAN_QUAN_TRI_DUOC_BAO_VE`; 409 `TAI_KHOAN_DANG_DUOC_SU_DUNG` (thông điệp nêu rõ còn gì đang dùng: hãy vô hiệu hoá) |

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
| Phiên đăng nhập | Access token JWT 30 phút; refresh token 7 ngày, ngẫu nhiên, DB chỉ lưu SHA-256; mỗi lần làm mới đổi token (rotation) nhưng giữ mã phiên `ma_phien` (access token mang mã này trong claim `phien`); token cũ bị dùng lại sau 10 giây -> đăng xuất phiên của token đó, các thiết bị khác giữ nguyên (reuse detection theo từng phiên) |
| Lưu token ở trình duyệt | Refresh token trong cookie HttpOnly/Secure/SameSite=Strict; access token chỉ trong bộ nhớ |
| CSRF | Cookie SameSite=Strict (trình duyệt không gửi từ trang khác) + CORS chỉ cho origin cụ thể + body JSON, vì vậy không dùng CSRF token của Spring |
| Header phản hồi | `Cache-Control: no-cache, no-store`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` (mặc định Spring Security), `Referrer-Policy: no-referrer` |
| Đăng nhập Google | Kiểm tra chữ ký (khoá công khai của Google), `iss`, `aud` = Client ID, hạn dùng, `email_verified`; định danh bằng `sub`; chỉ liên kết theo email khi Google đảm bảo chủ email (@gmail.com hoặc Google Workspace); chỉ bệnh nhân |
| Chiếm tài khoản chờ kích hoạt | Đăng ký lại email đang chờ thì ghi đè, liên kết cũ hết hiệu lực; đăng nhập Google vào tài khoản chờ kích hoạt thì bỏ mật khẩu, số điện thoại, liên kết kích hoạt của người đăng ký trước |
| Không ghi bí mật ra log | Không ghi mật khẩu, token, ID token, App Password; request/response/event che token trong `toString`. Riêng chế độ `MAIL_MODE=console` ghi liên kết ra log (chỉ dùng khi dev, có cảnh báo) |
| Đổi mật khẩu khi đang đăng nhập | Phải nhập lại mật khẩu hiện tại; sai 5 lần -> khoá 15 phút (429), bộ đếm theo tài khoản và dùng chung với đổi email; mật khẩu mới phải khác mật khẩu cũ; thành công thì thu hồi phiên của các thiết bị khác (giữ thiết bị đang dùng), huỷ yêu cầu đổi email đang chờ, gửi email "mật khẩu đã thay đổi" |
| Đổi email đăng nhập | Phải nhập mật khẩu hiện tại (tài khoản chỉ đăng nhập Google phải đặt mật khẩu trước); liên kết xác nhận gửi tới email MỚI (token như các liên kết khác, hạn 1 giờ, chờ 60 giây giữa 2 lần gửi); email chỉ đổi khi liên kết được xác nhận bằng POST; email cũ được báo lúc yêu cầu và sau khi đổi; kiểm tra trùng email lúc yêu cầu và lần nữa lúc xác nhận; xác nhận xong thu hồi mọi phiên và liên kết đặt lại mật khẩu đang chờ |
| Người dùng quản lý phiên của mình | Xem các thiết bị đang đăng nhập, đăng xuất từ xa 1 thiết bị hoặc mọi thiết bị khác; chỉ thao tác được phiên của chính mình (mã phiên của người khác chỉ nhận 404); danh sách không chứa token |
| Tải tệp lên (ảnh đại diện) | Chỉ JPEG / PNG / WebP, nhận ra bằng các byte đầu của nội dung, không tin tên tệp hay `Content-Type`; giới hạn 2 MB ở identity-service và 5 MB mỗi request ở gateway; mỗi tài khoản 10 lần / giờ; ảnh lưu ở Cloudinary (không nằm trên máy chủ), tên do server đặt `avatar/<id tài khoản>`, không dùng tên tệp của người dùng; lời gọi Cloudinary được ký bằng API Secret chỉ có ở server |
| Thao tác của quản trị viên | Kiểm tra vai trò trên token (`@PreAuthorize`) và kiểm tra lại trong DB ở mỗi lần gọi (tài khoản còn hoạt động, còn là quản trị viên); vô hiệu hoá bắt buộc có lý do, thu hồi mọi phiên, huỷ mọi liên kết đang chờ, gửi email cho chủ tài khoản; không vô hiệu hoá / xoá được tài khoản quản trị viên; không vô hiệu hoá bác sĩ còn lịch hẹn sắp tới; màn hình quản trị không có dữ liệu y tế; chỉ xoá tài khoản chưa có dữ liệu tham chiếu |
| Ghi nhật ký sự kiện bảo mật | Mục 5 |

## 3. Rủi ro đang chấp nhận

| Rủi ro | Ghi chú |
|--------|---------|
| Mật khẩu chỉ cần 6 ký tự | Đang thử nghiệm. Triển khai thật: tối thiểu 8 (tốt hơn 12), kiểm tra mật khẩu đã bị lộ |
| Đăng ký báo 409 khi email đã có | Theo đặc tả đồ án; cho biết email nào đã đăng ký (quên mật khẩu thì không) |
| Khoá đăng nhập theo email | Người khác biết email có thể cố tình sai 5 lần để khoá 15 phút |
| Chưa có CAPTCHA, giới hạn theo IP | Đăng ký, đăng nhập, quên mật khẩu chỉ giới hạn theo email / tài khoản |
| Quên mật khẩu chênh thời gian rất nhỏ | Tài khoản có thật tốn thêm vài lệnh Redis |
| Access token không thu hồi được | Đăng xuất, đăng xuất từ xa, đổi / đặt lại mật khẩu, đổi email, vô hiệu hoá chỉ thu hồi refresh token; access token còn sống tối đa 30 phút. Các API `/api/users/...` của identity-service kiểm tra lại tài khoản trong DB nên chặn ngay tài khoản bị vô hiệu hoá / đã xoá; các service khác vẫn nhận token đó tới khi hết hạn |
| Đăng nhập trùng lúc bị vô hiệu hoá | Một lần đăng nhập chạy đồng thời với thao tác vô hiệu hoá có thể để lại 1 phiên; phiên đó bị từ chối ở lần làm mới kế tiếp (tối đa 30 phút) |
| Không giới hạn tổng thời gian phiên | Refresh token trượt 7 ngày: dùng ít nhất 1 lần/tuần thì phiên không bao giờ hết. Đã có mốc đăng nhập ban đầu của phiên (`refresh_token.ngay_dang_nhap`, migration V3) nhưng chưa dùng để giới hạn |
| Token đã xoay vòng bị dùng lại | Sau 10 giây bị coi là lộ -> chỉ đăng xuất phiên (thiết bị) của token đó; các thiết bị khác của tài khoản không bị đăng xuất và chủ tài khoản không được báo. Token của phiên đã đăng xuất (tự đăng xuất, bị đăng xuất từ xa, đổi mật khẩu) bị dùng lại thì chỉ nhận 401 |
| Token liên kết bị dùng trước khi commit | Commit lỗi thì liên kết mất, người dùng yêu cầu lại |
| Không kiểm tra header Origin ở làm mới phiên / đăng xuất | SameSite=Strict + CORS đã chặn request từ trang khác |
| Access token trong bộ nhớ trang | Nếu frontend có lỗi XSS, script vẫn dùng được token khi trang đang mở (không lấy được refresh token) |
| Google: email không phải Gmail / Workspace | Tài khoản Google đó vẫn tạo được tài khoản mới với email ấy (Google đã xác minh lúc tạo); chủ email thật đăng ký sau sẽ bị 409 |
| Google: email không đồng bộ | Đổi email trên Google không đổi email trong eClinic |
| Chưa có xác thực 2 lớp (MFA) | Ngoài phạm vi |
| Đổi email: email cũ chỉ được báo, không phải xác nhận | OWASP khuyên khi không có MFA thì xác nhận ở cả 2 địa chỉ. Ở đây: mật khẩu hiện tại + liên kết ở email mới; email cũ nhận thông báo lúc yêu cầu và sau khi đổi (người dùng đổi mật khẩu là huỷ được yêu cầu) |
| Đổi email: email mới đang có đăng ký chờ kích hoạt | Lúc xác nhận nhận 409 `EMAIL_DA_TON_TAI` và liên kết đã bị dùng; người dùng chọn email khác, hoặc nhờ quản trị viên xoá tài khoản chờ kích hoạt đó rồi yêu cầu lại |
| Đổi email: gửi thư tới địa chỉ bất kỳ | Tài khoản đã đăng nhập (và biết mật khẩu) gửi được 1 thư xác nhận / phút tới một địa chỉ tuỳ ý |
| Ảnh đại diện: request chunked không qua giới hạn của gateway | Gateway chỉ xét `Content-Length`; request không có header này vẫn tới identity-service và bị chặn ở giới hạn 2 MB / 3 MB tại đó |
| Ảnh đại diện: body rất lớn | Body trên 20 MB có thể bị ngắt kết nối thay vì nhận 413 |
| Ảnh Google dùng trực tiếp URL của Google | Tài khoản tạo bằng Google giữ URL ảnh của Google (không chép vào kho ảnh): trình duyệt người xem tải ảnh từ máy chủ Google |
| Xoá tài khoản: ảnh còn sót trong kho ảnh | Xoá ảnh chạy sau khi commit; Cloudinary lỗi thì ảnh còn lại trong kho (có log WARN kèm mã ảnh) |
| Tài khoản quản trị viên không vô hiệu hoá / xoá được qua API | Chặn hẳn để không thể tự khoá mình hoặc khoá quản trị viên cuối cùng; cần thì sửa trực tiếp trong DB |
| Chưa có bảng nhật ký thao tác quản trị | Vô hiệu hoá / kích hoạt lại / xoá chỉ ghi log (id quản trị viên, id tài khoản); lý do vô hiệu hoá lưu ở `tai_khoan.ly_do_vo_hieu_hoa` |
| Bác sĩ bị vô hiệu hoá vẫn còn trong danh mục | identity-service không sửa `bac_si.trang_thai` và không huỷ ca làm việc; chức năng đặt lịch và danh sách bác sĩ phải tự kiểm tra `tai_khoan.trang_thai` |

## 4. Trước khi triển khai thật

- [ ] Đặt `JWT_SECRET` (ngẫu nhiên, ≥ 32 ký tự, giống nhau ở mọi service). Khởi động mà còn khoá dev sẽ có cảnh báo WARN.
- [ ] Đặt `ADMIN_PASSWORD` mạnh trước lần khởi động đầu (hoặc đổi ngay sau đó). Còn `Admin@123` sẽ có cảnh báo WARN.
- [ ] `MAIL_MODE=smtp` với `MAIL_USERNAME`, `MAIL_APP_PASSWORD` (App Password của Gmail).
- [ ] Chạy HTTPS; `FRONTEND_URL` là `https://…`; `CORS_ALLOWED_ORIGINS` chỉ gồm frontend thật; giữ `COOKIE_SECURE=true`.
- [ ] Không mở cổng các service (8081…8088), MySQL, Redis, phpMyAdmin ra ngoài; chỉ mở gateway. Cân nhắc tắt Swagger.
- [ ] Nâng quy tắc mật khẩu (mục 3).
- [ ] Google Cloud: OAuth client "Web application" với Authorized JavaScript origins là domain frontend thật; đặt `GOOGLE_CLIENT_ID`.
- [ ] `SEED_DATA=false`: tài khoản mẫu dùng chung mật khẩu đã công khai. DB từng chạy với dữ liệu mẫu thì vô hiệu hoá / xoá
  các tài khoản `bacsiNN@eclinic.local`, `benhnhanNN@eclinic.local`. Còn bật sẽ có cảnh báo WARN khi khởi động.
- [ ] Cloudinary: đặt `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` qua biến môi trường (không commit);
  mỗi môi trường một `CLOUDINARY_FOLDER` riêng để không ghi đè ảnh của nhau.
- [ ] Đặt lịch khám: xử lý địa chỉ client sau reverse proxy và xem lại các giới hạn `BOOKING_*` (mục 7).

## 5. Nhật ký bảo mật (log của identity-service)

Chỉ ghi id tài khoản / id phiên, không ghi email, mật khẩu, token, lý do vô hiệu hoá.

| Sự kiện | Mức | Nội dung |
|---------|-----|----------|
| Đăng nhập sai | INFO | `Đăng nhập sai id=<id hoặc -> (lần sai thứ n)` |
| Bị khoá đăng nhập | WARN | `Khoá đăng nhập id=<id hoặc -> sau n lần sai mật khẩu` |
| Mật khẩu đúng nhưng tài khoản chưa kích hoạt / bị vô hiệu hoá | INFO | `Từ chối đăng nhập id=…` |
| Đăng nhập, làm mới phiên, đăng xuất | INFO | `Đăng nhập thành công id=…`, `Làm mới phiên id=… của tài khoản id=…`, `Đăng xuất phiên id=…` |
| Refresh token cũ bị dùng lại | WARN | `Refresh token đã thu hồi bị dùng lại: đăng xuất phiên <mã phiên> của tài khoản id=…` (phiên đã đăng xuất từ trước: INFO `Refresh token của phiên đã đăng xuất bị dùng lại, tài khoản id=…`) |
| Gửi liên kết / đặt lại mật khẩu | INFO | `Gửi liên kết đặt lại mật khẩu id=…`, `Đặt lại mật khẩu id=…, thu hồi n phiên` |
| Google | INFO | `Từ chối ID token Google: <lý do>`, `Đăng nhập Google id=…`, `Liên kết Google…`, `Tạo tài khoản từ Google…`, `Kích hoạt tài khoản id=… bằng Google…`, `Từ chối liên kết Google…`, `Từ chối đăng nhập Google id=…` |
| Cập nhật hồ sơ cá nhân | INFO | `Cập nhật hồ sơ cá nhân tài khoản id=…` |
| Đổi mật khẩu | INFO | `Đổi mật khẩu id=…, thu hồi n phiên khác` |
| Sai mật khẩu hiện tại (đổi mật khẩu, đổi email) | INFO / WARN | `Sai mật khẩu hiện tại id=… (lần sai thứ n)`; WARN `Khoá đổi mật khẩu id=… sau n lần sai mật khẩu hiện tại (khoá cả đổi email)` |
| Đăng xuất từ xa | INFO | `Đăng xuất từ xa phiên <mã phiên> của tài khoản id=…`, `Đăng xuất n phiên khác của tài khoản id=…` |
| Ảnh đại diện | INFO | `Đổi ảnh đại diện tài khoản id=… (n byte)`, `Bỏ ảnh đại diện tài khoản id=…`, `Tài khoản id=… tải ảnh đại diện quá n lần` |
| Kho ảnh lỗi | INFO / WARN | INFO `Cloudinary từ chối ảnh: …`; WARN `Cloudinary <thao tác> lỗi HTTP <mã>: …`, `Không gọi được Cloudinary <thao tác>: …`, `Không xoá được ảnh đại diện cũ của tài khoản id=… trong kho ảnh…`, `Không xoá được ảnh đại diện của tài khoản đã xoá id=… trong kho ảnh (ảnh còn sót lại: …)` |
| Đổi email | INFO | `Gửi liên kết xác nhận đổi email id=…`, `Huỷ yêu cầu đổi email id=…`, `Đổi email đăng nhập id=…, thu hồi n phiên` |
| Quản trị viên vô hiệu hoá / kích hoạt lại / xoá | INFO | `Quản trị viên id=… vô hiệu hoá tài khoản id=…, thu hồi n phiên`, `Quản trị viên id=… kích hoạt lại tài khoản id=…`, `Quản trị viên id=… xoá tài khoản id=…` |
| Cấu hình chỉ dành cho dev (khi khởi động) | WARN | `Dữ liệu mẫu đang BẬT (SEED_DATA=true): …`; `Chưa đặt đủ CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET: …` |

Xem nhanh: `docker logs eclinic-identity-service 2>&1 | grep -E "Đăng nhập sai|Khoá đăng nhập|Từ chối|dùng lại|Quản trị viên id="`.

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
- **Gửi `Authorization` chỉ cho API cần đăng nhập** (`/api/users/...` và API của các service khác), KHÔNG gửi cho
  `/api/auth/...`: access token hết hạn gửi kèm sẽ làm cả API công khai (làm mới phiên, xác nhận đổi email...) trả 401.
- **401 và 403 ở API cần đăng nhập:** 401 `CHUA_DANG_NHAP` -> làm mới phiên rồi gọi lại; 403 `TAI_KHOAN_BI_VO_HIEU_HOA` ->
  tài khoản đã bị khoá: xoá access token, về trang đăng nhập và hiện thông điệp, không làm mới phiên; 403 `KHONG_CO_QUYEN`
  -> sai vai trò, không đăng xuất.
- **Đổi mật khẩu** (`PUT /api/users/me/change-password`): thành công thì thiết bị này vẫn đăng nhập, không cần đăng nhập lại.
  400 `MAT_KHAU_CU_KHONG_DUNG` là sai mật khẩu hiện tại (không phải hết phiên, đừng làm mới token). 409
  `TAI_KHOAN_CHUA_CO_MAT_KHAU` (hồ sơ có `coMatKhau = false`) -> hướng dẫn dùng Quên mật khẩu để đặt mật khẩu.
- **Thiết bị đăng nhập:** dòng có `hienTai = true` là thiết bị này: ẩn nút đăng xuất từ xa, dùng nút Đăng xuất thường.
- **Ảnh đại diện:** gửi `FormData` với phần tệp tên `anh`; KHÔNG tự đặt header `Content-Type` (trình duyệt tự thêm
  boundary). Kiểm tra trước ở client: JPEG / PNG / WebP, tối đa 2 MB. Hiển thị bằng URL trong `anhDaiDien` (URL đổi sau
  mỗi lần tải lên); `anhDaiDien` là `null` thì dùng ảnh mặc định.
- **Đổi email:** form nhập email mới + mật khẩu hiện tại; sau khi gửi, hiện "đang chờ xác nhận <email mới>" lấy từ
  `GET /api/users/me/change-email`, kèm nút huỷ. **Trang `/confirm-email-change?token=…`:** POST token khi người dùng
  bấm nút (không POST lúc tải trang: trình quét email mở trước liên kết sẽ tiêu mất token), xoá token khỏi thanh địa chỉ;
  thành công -> xoá access token, chuyển tới trang đăng nhập (mọi thiết bị đã bị đăng xuất); 410 -> yêu cầu đổi email lại.
- **Quản trị tài khoản:** `GET /api/users?tuKhoa=&vaiTro=&trangThai=&trang=0&kichThuoc=20`; số trang bắt đầu từ 0, tổng
  số trang ở `duLieu.tongSoTrang`. Vô hiệu hoá phải có `lyDo`. Tài khoản `CHO_XAC_NHAN` chỉ có nút xoá, tài khoản
  `QUAN_TRI_VIEN` không có nút vô hiệu hoá / xoá.
- **Khi dev:** frontend chạy `http://localhost:5173` (trùng `FRONTEND_URL`, nằm trong CORS). Cookie `Secure` chạy được
  trên `http://localhost` với Chrome/Edge/Firefox (Safari thì không); curl cần bản ≥ 7.84 (dùng `-c`/`-b` để giữ cookie).
  Trang mẫu: `scripts/demo-xac-thuc` (xem README).

## 7. API đặt lịch khám công khai (booking-service)

Xem khung giờ, đặt lịch và xem phiếu khám không cần đăng nhập (BOOK-01, BOOK-06), nên các API này tự bảo vệ. Danh sách API
và mã lỗi: README mục "Đặt lịch khám".

| Vấn đề | Cách xử lý |
|--------|------------|
| Gọi đặt lịch hàng loạt | Mỗi địa chỉ IP tối đa 20 lần / 10 phút (đếm trên Redis, trước mọi thao tác DB) -> 429 `GUI_LAI_QUA_NHANH`; mỗi hồ sơ bệnh nhân tối đa 3 lịch sắp tới còn hiệu lực, mỗi số điện thoại liên hệ tối đa 5, mỗi hồ sơ 1 lịch trong cùng 1 giờ -> 409. Các con số là cấu hình `BOOKING_*` |
| Hai người giành lượt khám cuối | Đặt lịch chạy trong 1 transaction, khoá các lượt trống của khung giờ rồi mới chọn; UNIQUE `lich_hen.id_khung_gio_hieu_luc` là chốt cuối trong DB -> chỉ 1 người được, người kia 409 |
| Dùng số CCCD của người khác để xem / sửa hồ sơ | Số CCCD đã có hồ sơ thì họ tên và ngày sinh phải khớp, sai -> 409 với thông điệp không chứa dữ liệu đang lưu; form đặt lịch không sửa hồ sơ hay người giám hộ đã lưu, chỉ điền ngày sinh / giới tính còn trống (số điện thoại vừa nhập chỉ lưu trên lịch hẹn) |
| Gắn hồ sơ bệnh nhân vào tài khoản | Chỉ khi đặt cho bản thân (`datChoBanThan`) hoặc tự tạo hồ sơ, và chỉ với số CCCD **chưa có** hồ sơ; số CCCD đã có hồ sơ (do khách đặt trước đó) -> 409 `CCCD_DA_CO_HO_SO`, phải xác minh tại phòng khám |
| Mã phiếu khám | 32 byte ngẫu nhiên (`SecureRandom`, 43 ký tự base64url); API phiếu khám không nhận id số; mã sai -> 404 |
| Dữ liệu trên phiếu khám công khai | Chỉ năm sinh; CCCD còn 3 số cuối; số điện thoại che phần giữa; người giám hộ không có CCCD; không có id lịch hẹn, id hồ sơ |
| Token trên API đặt lịch | Không có token: khách. Token `BENH_NHAN`: lịch lưu vào tài khoản, và booking-service kiểm tra lại trong DB rằng tài khoản còn hoạt động (cả ở "lịch hẹn của tôi" và hồ sơ bệnh nhân). Token bác sĩ / quản trị viên -> 403 |
| Danh sách bác sĩ, khung giờ | Không có email, số điện thoại, số giấy phép của bác sĩ; không có thông tin người đã đặt, chỉ số chỗ còn lại |

**Rủi ro đang chấp nhận:**
- Chưa có CAPTCHA hay xác minh số điện thoại (OTP): ai biết họ tên, ngày sinh và số CCCD của một người thì đặt được lịch
  đứng tên người đó, trong các giới hạn ở trên.
- 409 `THONG_TIN_BENH_NHAN_KHONG_KHOP` / `CCCD_DA_CO_HO_SO` cho biết một số CCCD đã có hồ sơ ở phòng khám (không cho biết nội dung).
- Redis lỗi thì bỏ qua giới hạn theo IP (đặt lịch vẫn chạy, mỗi lần chờ tối đa 2 giây, có log WARN); 2 giới hạn theo hồ sơ
  và số điện thoại vẫn còn vì nằm trong DB.
- Link phiếu khám là bí mật duy nhất của phiếu: ai có link / ảnh QR đều xem được phiếu (đã che), và link không hết hạn.

**Trước khi triển khai thật:**
- [ ] **Địa chỉ client sau reverse proxy.** booking-service lấy phần tử CUỐI của `X-Forwarded-For` (do gateway ghi, cần
  `GATEWAY_TRUSTED_PROXIES`). Client gọi thẳng gateway thì đó là địa chỉ client. Đặt gateway sau nginx / load balancer thì
  phần tử cuối là địa chỉ của proxy đó, mọi người dùng bị đếm chung 1 IP: phải giới hạn theo IP ngay tại proxy, hoặc sửa
  `DiaChiIp` cho đúng số lớp proxy, rồi đặt `GATEWAY_TRUSTED_PROXIES` thành địa chỉ proxy thay cho `.*`.
- [ ] `FRONTEND_URL` là địa chỉ `https://…` thật: link phiếu khám trong mã QR dựng từ biến này.
- [ ] Xem lại `BOOKING_IP_LIMIT`, `BOOKING_MAX_ACTIVE_PER_PATIENT`, `BOOKING_MAX_ACTIVE_PER_PHONE` theo lượng khách thực tế.

**Hướng dẫn cho frontend:**
- `POST /api/booking/lich-hen`: chỉ gửi `Authorization` khi đang có phiên bệnh nhân. Token hết hạn làm request nhận 401 dù
  API công khai: làm mới phiên rồi gọi lại, không được thì đặt như khách. Các API công khai còn lại không gửi token.
- Khung giờ gửi lên bằng đúng `idLichLamViec` + `khungGio[].gioBatDau` lấy từ `GET /api/booking/khung-gio`; khung có
  `hetCho = true` không cho chọn. Nhận 409 khi đặt thì tải lại khung giờ.
- Ô người giám hộ hiện khi người khám dưới 18 tuổi **tính theo ngày khám**; server vẫn là nơi quyết định (400
  `THIEU_NGUOI_GIAM_HO`).
- Trang `/phieu-kham/<mã>`: đọc mã trên URL, gọi `GET /api/booking/phieu-kham/<mã>`; mã QR hiển thị bằng
  `<img src=".../api/booking/phieu-kham/<mã>/qr">`. Trang phải có `Referrer-Policy: no-referrer`, không nhúng script / ảnh
  bên thứ ba (mã nằm trên URL). Khách đặt lịch chỉ xem lại được bằng link này: nhắc lưu link hoặc tải mã QR.
- Trang mẫu: `scripts/demo-dat-lich` (xem README).
