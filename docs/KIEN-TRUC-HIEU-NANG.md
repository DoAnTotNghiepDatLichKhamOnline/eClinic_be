# Kiến trúc và hiệu năng — 1.000 người dùng đồng thời

Tài liệu này ghi lại vì sao backend giữ kiến trúc hiện tại, những gì đã thêm để chịu khoảng 1.000 người dùng đồng thời
(giới hạn tần suất, cache Redis, nén gzip), số liệu đo trước / sau và các phương án đã cân nhắc nhưng không làm.
Công việc: DOANTOTNGH-15 (tháng 10/2026). Nguyên tắc: chỉ thêm thứ đã có số liệu cho thấy cần, không làm hệ thống phức
tạp hơn mức cần.

## 1. Mục tiêu

- Khoảng 1.000 người dùng đồng thời (bệnh nhân xem bác sĩ, xem khung giờ, đặt lịch; bác sĩ, quản trị viên) không lỗi và
  thời gian phản hồi vẫn nhanh.
- Chặn một client gửi quá nhiều request (dò mật khẩu, spam email, gọi chatbot tốn tiền) ngay tại gateway.
- Không thêm hạ tầng mới ngoài Redis (đã có sẵn trong `docker-compose.yml`).

## 2. Các hệ thống tương tự

| Hệ thống | Thông tin công khai | Rút ra cho eClinic |
|---|---|---|
| Doctolib (Pháp, Đức, Ý) | Bắt đầu là 1 ứng dụng Ruby on Rails (monolith), hiện đang tách dần thành nhiều service, có dùng Java / Spring Boot ([Devoxx](https://devoxx.be/talk?id=20068)). | Một hệ thống phục vụ hàng chục triệu bệnh nhân vẫn đi lên từ 1 ứng dụng + 1 CSDL quan hệ; chỉ tách khi đội ngũ và tải lớn hơn nhiều. |
| Zocdoc (Mỹ) | Từng là 1 ứng dụng web monolith, tách dần thành microservice trên AWS trong nhiều năm, chủ yếu để nhiều nhóm phát triển độc lập ([AWS Startups blog](https://aws.amazon.com/blogs/startups/from-monoliths-to-microservices-at-zocdoc), [AWS case study](https://aws.amazon.com/fr/solutions/case-studies/zocdoc)). | Lý do tách service là tổ chức đội ngũ, không phải để chịu vài nghìn người dùng. |
| BookingCare, YouMed (Việt Nam) | Không có tài liệu kỹ thuật công khai; trang web công khai cho thấy cùng dạng: trang đặt lịch trước một CSDL quan hệ. | Không có số liệu để so; chỉ dùng làm tham chiếu về chức năng. |

Kết luận: 1.000 người dùng đồng thời là tải nhỏ so với các hệ thống trên. Kiến trúc hiện có (1 gateway, vài service Spring
Boot, 1 MySQL, 1 Redis) đủ chịu tải đó; số liệu ở mục 7 xác nhận điều này trên 1 máy laptop.

## 3. Kiến trúc đang dùng

```
Trình duyệt ──► api-gateway :8080 ──► identity / catalog / booking / medical / notification / chatbot / report
                 │ CORS                      │  mỗi service tự kiểm tra JWT (HS256, không gọi DB)
                 │ giới hạn tần suất ─┐      │
                 │ nén gzip           │      ▼
                 │                    └──► Redis ◄── cache (catalog, booking), giới hạn riêng từng chức năng
                 │                                 MySQL (1 CSDL dùng chung, Flyway theo từng service)
```

- JWT được kiểm tra không trạng thái ở từng service, nên không có lượt gọi DB nào chỉ để xác thực.
- Đặt lịch khoá dòng (`SELECT ... FOR UPDATE`) trên khung giờ, lịch hẹn và hồ sơ bệnh nhân, nên nhiều người đặt cùng lúc
  không thể đặt trùng chỗ.
- Mọi request từ trình duyệt đi qua gateway; các service không mở CORS.

## 4. Giới hạn tần suất tại gateway

| Nhóm | Áp dụng cho | Mặc định / phút / IP | Biến môi trường |
|---|---|---|---|
| `xac-thuc` | POST `/api/auth/login`, `/google`, `/register`, `/forgot-password`, `/resend-verification` | 20 | `GATEWAY_AUTH_RATE_LIMIT` |
| `chatbot` | POST `/api/chatbot/**` | 30 | `GATEWAY_CHATBOT_RATE_LIMIT` |
| `chung` | Mọi request `/api/**` còn lại | 300 | `GATEWAY_RATE_LIMIT` |

- Cửa sổ cố định 1 phút, đếm trên Redis bằng 1 script Lua (tăng bộ đếm và đặt hạn trong 1 lần gọi). Khoá
  `gioi-han:<nhom>:<ip>`, dùng chung cho mọi instance gateway.
- IP là địa chỉ kết nối trực tiếp tới gateway, không phải `X-Forwarded-For` (client tự ghi được).
- Vượt giới hạn: 429 `QUA_NHIEU_YEU_CAU`, câu thông báo VI / EN và header `Retry-After` (số giây tới hết cửa sổ). Header này
  được mở qua CORS nên JavaScript đọc được. Cách frontend xử lý: [BAN-GIAO-FRONTEND-05-10.md](BAN-GIAO-FRONTEND-05-10.md).
- Redis lỗi: request vẫn đi tiếp (fail-open), gateway bỏ qua Redis 30 giây rồi thử lại, để mỗi request không phải chờ hết
  thời gian chờ của Redis.
- Mức mặc định rộng vì nhiều bệnh nhân ở cùng 1 mạng (bệnh viện, công ty) dùng chung 1 IP.
- Các giới hạn riêng của từng chức năng vẫn giữ: đặt lịch theo IP, đăng nhập sai, chatbot theo người dùng, xin cấp lại
  liên kết phiếu khám, sổ khám bệnh.

## 5. Cache Redis

| Cache | Dữ liệu | Khoá | Xoá khi |
|---|---|---|---|
| `catalog:chuyen-khoa` | Danh sách chuyên khoa công khai, **không** có từ khoá tìm kiếm | `catalog:chuyen-khoa::<VI\|EN>:<trang>:<kichThuoc>` | Thêm / sửa / xoá chuyên khoa |
| `catalog:chuyen-khoa-chi-tiet` | Chi tiết 1 chuyên khoa | `catalog:chuyen-khoa-chi-tiet::<VI\|EN>:<id>` | Thêm / sửa / xoá chuyên khoa |
| `booking:ngay-nghi` | Lịch ngày nghỉ của bệnh viện (công khai) | `booking:ngay-nghi::<VI\|EN>:<tuNgay>:<denNgay>` | Khai báo / xoá ngày nghỉ |

- Khoá có ngôn ngữ của request vì tên, mô tả, lý do nghỉ trả theo ngôn ngữ.
- Hết hạn sau 10 phút chỉ để phòng hờ: mọi lần ghi xoá cache **sau khi commit**, nên lần xem kế tiếp đã thấy dữ liệu mới.
- Giá trị là JSON thường (không kèm tên class), mỗi cache 1 kiểu.
- **Không cache**:
  - Khung giờ còn trống: đổi sau mỗi lượt đặt.
  - Bác sĩ: identity, catalog và booking đều ghi dữ liệu bác sĩ, xoá cache đúng lúc sẽ phức tạp.
  - Quy tắc đặt lịch: đã có bản chụp trong bộ nhớ 60 giây.
  - Danh sách phòng khám: chỉ quản trị viên xem.
- Redis không bắt buộc với catalog và booking. Thời gian chờ Redis là 500 ms (`application-common.yml`). Mất kết nối thì
  lệnh bị từ chối ngay, service ghi 1 dòng cảnh báo và đọc thẳng DB. Cache chạy lại trong khoảng 30 giây sau khi Redis lên
  lại.

## 6. Cấu hình chạy

| Mục | Giá trị | Lý do |
|---|---|---|
| GC | `-XX:+UseSerialGC`, heap `MaxRAMPercentage=70` của 512 MB | Tốn ít bộ nhớ nhất với heap nhỏ; G1 / ZGC tốn thêm bộ nhớ mà không lợi gì ở cỡ heap này. |
| Trình biên dịch JIT | Giữ `-XX:TieredStopAtLevel=1` (chỉ C1) | Đã đo: bật C2 làm booking-service vượt 512 MB và phải dùng swap, chậm hơn (bảng dưới). |
| Nén gzip | `server.compression.enabled` ở gateway, JSON từ 2 KB | Danh sách bác sĩ 4.593 B → 1.276 B. Chỉ tốn khoảng 4% thông lượng khi CPU đã đầy. |
| Pool kết nối DB, luồng Tomcat | Mặc định (Hikari 10 / service, Tomcat 200) | Ở mức tải tối đa: không có lỗi chờ kết nối, không 503; MySQL dùng tối đa 41 / 151 kết nối. |

Thử C2 (1.000 người dùng ảo, nghỉ 0,5 giây, mỗi lần chạy trên stack mới):

| Cấu hình | request/giây | p95 | Bộ nhớ booking-service |
|---|---|---|---|
| C1 (đang dùng), không gzip | 960 – 1.101 | 719 – 956 ms | 443 – 465 MiB, không swap |
| C2 | 1.187 | 880 ms | 512 MiB + 124 MiB swap |
| C2 + `ReservedCodeCacheSize=64m` | 962 | 1.177 ms | 510 MiB + 101 MiB swap |
| C2 + `CICompilerCount=2` | 1.048 | 908 ms | 511 MiB + 57 MiB swap |

Nguyên nhân: container thấy cả 12 CPU của máy nên JVM mở khoảng 10 luồng biên dịch C2, bộ nhớ làm việc của chúng đẩy
service quá 512 MB. Chỉ bật C2 khi đã nâng `mem_limit` (mục 9).

## 7. Kiểm tra tải

Script k6: `scripts/load-test/kiem-tra-tai.js`; lệnh chạy và biến môi trường ở đầu file. Hai chế độ:

- **Thực tế** (mặc định): tới 1.000 người dùng ảo, nghỉ 3–8 giây giữa các bước như người thật, khoảng 9 phút.
- **Dồn tải** (`-e VUS=1000 -e HOLD=2m -e NGHI=0.5`): cùng 1.000 người dùng nhưng nghỉ trung bình 0,5 giây, để tìm giới
  hạn của hệ thống. Mỗi lần đo nên chạy trên stack mới (`down -v`) sau 1 lượt khởi động ngắn (`-e VUS=40 -e HOLD=60s`), vì
  lịch đã đặt làm đầy khung giờ và JVM vừa khởi động chạy chậm hơn.

Mọi request của k6 đến từ 1 IP nên khi chạy phải đặt 3 biến `GATEWAY_*RATE_LIMIT` (và `BOOKING_IP_LIMIT`) rất lớn, vd
1000000; giới hạn vẫn đếm trên Redis như khi chạy thật.

**Chế độ thực tế, 1.000 người dùng** (máy 12 CPU, 7,4 GiB cho Docker):

| | Trước (chưa có giới hạn, cache) | Sau (đủ các thay đổi) |
|---|---|---|
| Request | 76.327 (135/giây) | 76.180 (134/giây) |
| Lỗi | 0% | 0% |
| p50 / p95 / p99 / max | 6 / 10 / 69 / 286 ms | 7 / 13 / 71 / 131 ms |
| CPU gateway | 30–32% | 39% |
| Bộ nhớ booking-service | 397 MiB | 410 MiB |

p95 tăng 3 ms là chi phí của 1 lần gọi Redis cho giới hạn tần suất ở mỗi request cộng với nén gzip. Chậm nhất là đăng
nhập (p95 khoảng 80 ms, do BCrypt cố ý chậm).

**Chế độ dồn tải** (giới hạn của hệ thống trên 1 laptop): khoảng 950 – 1.100 request/giây, 0% lỗi, p95 0,7 – 1,1 giây,
tức gấp 7 – 8 lần tải của 1.000 người dùng thật. Nút thắt là CPU của máy (các service và MySQL dùng khoảng 10 / 12 nhân,
k6 chạy cùng máy), không phải pool kết nối, luồng hay CSDL. Kết quả giữa các lần chạy dao động khoảng ±15%, nên chỉ so 2
lần chạy liền nhau.

**Không có N+1**: số câu `SELECT` của mỗi API hay gọi (đếm bằng `Com_select` của MySQL) là 1 – 5 và không tăng theo số
dòng trả về (danh sách bác sĩ 5 dòng: 4 câu, 12 dòng: 3 câu). Danh sách lịch hẹn của tôi đã `join fetch` các bảng liên quan.

## 8. Phương án đã loại

| Phương án | Lý do không làm |
|---|---|
| Kafka / hàng đợi tin nhắn | Không có luồng nào cần xử lý bất đồng bộ ở mức tải này; thêm 1 hệ thống phải vận hành. |
| Mỗi service 1 CSDL | Không cải thiện hiệu năng ở tải này; cần đồng bộ dữ liệu giữa các service. |
| CQRS | Không có màn hình nào cần mô hình đọc riêng; chỉ thêm độ phức tạp. |
| Kubernetes | docker compose đủ cho đồ án và demo; không có yêu cầu tự mở rộng. |
| Bản sao đọc MySQL | MySQL chỉ dùng khoảng 1,5 nhân khi dồn tải; không phải nút thắt. |
| Cache bác sĩ, khung giờ | Xem mục 5: nhiều service cùng ghi / đổi sau mỗi lượt đặt. |
| Danh sách IP được miễn giới hạn | Kiểm tra tải nâng giới hạn bằng biến môi trường, vẫn đo được chi phí của Redis. |
| Đổi GC, bật C2 | Xem mục 6. |
| Tăng pool kết nối / luồng Tomcat | Không có số liệu nào cho thấy thiếu (mục 6). |

## 9. Khi cần mở rộng

Theo thứ tự, mỗi bước chỉ làm khi có số liệu tương ứng:

1. **Nâng `mem_limit` của booking-service** (vd 768m) khi bộ nhớ của nó thường xuyên trên 90% (dồn tải hiện tới khoảng
   463 / 512 MiB). Khi đó có thể bỏ `TieredStopAtLevel=1` để bật C2 và đo lại.
2. **Chạy 2 bản của 1 service sau gateway** khi CPU của service đó luôn đầy mà máy còn nhân trống. Giới hạn tần suất đã
   đếm trên Redis nên vẫn đúng khi có nhiều gateway.
3. **Tinh chỉnh MySQL hoặc thêm bản sao đọc** khi MySQL trở thành nút thắt (CPU cao, truy vấn chậm trong log).
