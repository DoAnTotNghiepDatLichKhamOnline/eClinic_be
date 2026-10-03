# Quy ước code — eClinic backend

Tài liệu cho cả nhóm khi viết tính năng mới. Mẫu để copy: **quản lý chuyên khoa (ADM-01) trong `catalog-service`**.
Cách chạy project: xem [README](../README.md).

## 1. Đặt tên

| Loại | Quy ước | Ví dụ |
|---|---|---|
| Class | Tiếng Việt **không dấu**, PascalCase + hậu tố vai trò Spring bằng tiếng Anh | `LichHenController`, `LichHenService`, `LichHenServiceImpl`, `LichHenMapper` |
| DTO | `<Ten>Request` / `<Ten>Response` | `ChuyenKhoaRequest`, `ChuyenKhoaResponse` |
| Class dùng chung | Tiếng Việt | `PhanHoiApi`, `MaLoi`, `LoiNghiepVu`, `TrangDuLieu` |
| Field, biến, tham số, key JSON | camelCase tiếng Việt không dấu | `tenChuyenKhoa`, `idTaiKhoan`, `thanhCong` |
| Method controller/service | Tiếng Việt theo bảng dưới | `layTheoId`, `timKiem`, `tao` |
| Method repository (Spring Data) | **Giữ tiếng Anh** (Spring đọc tên method để sinh query) | `existsByTenChuyenKhoa`, `countByChuyenKhoaId` |
| Hằng số enum, mã lỗi | UPPER_SNAKE_CASE tiếng Việt | `QUAN_TRI_VIEN`, `TEN_CHUYEN_KHOA_DA_TON_TAI` |
| Bảng, cột DB | snake_case (theo migration) | `chuyen_khoa`, `id_tai_khoan` |
| Package | chữ thường | `iuh.fit.se.eclinic.catalog.controller` |

Một số class kỹ thuật giữ tên tiếng Anh: `BaseEntity`, `AuditableEntity`, `AppTimeZone`, `JpaAuditingConfig`,
các class `Jwt*`, `*Properties`, `*Config` và class hỗ trợ test.

Tên method thường dùng:

| Method | Ý nghĩa |
|---|---|
| `layTheoId(id)` | Lấy 1 bản ghi, không có thì ném `LoiKhongTimThay` |
| `layTatCa()` | Lấy tất cả (danh sách ngắn, không phân trang) |
| `timTheoX(...)` | Tìm theo một điều kiện cụ thể |
| `timKiem(tuKhoa, trang, kichThuoc)` | Tìm kiếm có phân trang |
| `tao(request)` / `capNhat(id, request)` / `xoa(id)` | Thêm / sửa / xoá |
| `kiemTra...` / `dem...` / `tonTai...` | Kiểm tra điều kiện / đếm / có tồn tại hay không |

## 2. Cấu trúc package của một service

```
<service>/src/main/java/iuh/fit/se/eclinic/
├── <Ten>ServiceApplication.java        # class main, nằm ở package gốc iuh.fit.se.eclinic
└── <service>/                          # ví dụ: catalog, booking
    ├── controller/                     # REST controller
    ├── service/                        # interface service
    │   └── impl/                       # class cài đặt service
    ├── mapper/                         # chuyển entity <-> DTO (viết tay, @Component)
    ├── dto/request/  dto/response/     # DTO (record)
    ├── repository/                     # Spring Data JPA repository
    ├── client/                         # gọi REST sang service khác (RestClient)
    └── config/                         # cấu hình riêng của service
```

**Entity và enum chỉ nằm trong `common`** (`common/src/main/java/iuh/fit/se/eclinic/common/entity/<miền>/`,
`.../common/enums/`), vì mọi service dùng chung một database.

## 3. Thêm một API mới (copy theo mẫu chuyên khoa)

File mẫu trong `catalog-service/src/main/java/iuh/fit/se/eclinic/catalog/`:
`controller/ChuyenKhoaController.java`, `service/ChuyenKhoaService.java`,
`service/impl/ChuyenKhoaServiceImpl.java`, `mapper/ChuyenKhoaMapper.java`,
`dto/request/ChuyenKhoaRequest.java`, `dto/response/ChuyenKhoaResponse.java`,
`repository/ChuyenKhoaRepository.java`.

1. **Repository**: interface `extends JpaRepository<Entity, Long>` trong `repository/`. Tên method bằng tiếng Anh.
2. **DTO**: `record` trong `dto/request` (kèm `@NotBlank`, `@Size`... để kiểm tra dữ liệu vào) và `dto/response`.
   Không bao giờ trả entity ra ngoài controller.
3. **Mapper**: class `@Component` viết tay với `toEntity`, `toResponse`, `capNhat(entity, request)`.
4. **Service**: interface trong `service/`, cài đặt trong `service/impl/` với `@Transactional(readOnly = true)` ở class
   và `@Transactional` ở các method ghi. Service trả về DTO, không trả entity.
   - Không tìm thấy: `throw new LoiKhongTimThay("ChuyenKhoa", id)`.
   - Vi phạm nghiệp vụ: `throw new LoiNghiepVu(MaLoi.TEN_CHUYEN_KHOA_DA_TON_TAI)`; mỗi lỗi mới thêm một mã vào
     `common/src/main/java/iuh/fit/se/eclinic/common/exception/MaLoi.java` (mã HTTP + thông điệp tiếng Việt).
   - **Kiểm tra trùng TRƯỚC khi sửa entity** (xem `capNhat` trong mẫu): entity đang được Hibernate quản lý, chạy
     query sau khi đã sửa nó sẽ làm Hibernate ghi xuống DB trước, và lỗi unique key bật ra trước khi kịp kiểm tra.
   - Xoá bản ghi còn đang được dùng: trả 409 kèm thông điệp nói rõ còn gì đang dùng (xem `xoa` trong mẫu).
5. **Controller**: mỏng — chỉ nhận dữ liệu (`@Valid`), gọi service, bọc kết quả `PhanHoiApi.ok(...)`.
   - `@RequestMapping` phải là **đường dẫn đầy đủ** `/api/<service>/...` (ví dụ `/api/catalog/chuyen-khoa`),
     vì gateway chuyển tiếp nguyên đường dẫn. Chỉ có 2 ngoại lệ, theo tài liệu API của đồ án: `/api/auth/...` và
     `/api/users/...` (đều của identity-service, đã khai báo trong route của gateway). Không thêm ngoại lệ mới.
   - Thêm `@Tag` / `@Operation` để Swagger có mô tả tiếng Việt.
6. **Phân quyền**: xem mục 5.
7. **Test**: copy 2 file test mẫu (mục 8) và sửa theo API mới.
8. Chạy test module: `./mvnw -pl <service> -am verify`, rồi thử trên Swagger của service.

## 4. Phản hồi và lỗi

- Thành công: `PhanHoiApi.ok(duLieu)` hoặc `PhanHoiApi.ok(duLieu, "Đã thêm ...")` →
  `{"thanhCong": true, "thongDiep": "...", "duLieu": ...}`.
- Có phân trang: service trả `TrangDuLieu.tu(page)` →
  `{"noiDung": [...], "trang", "kichThuoc", "tongSoPhanTu", "tongSoTrang"}`.
  `@Query` phân trang có `join fetch` phải khai báo thêm `countQuery` (không có `fetch`) và viết `order by` ngay trong JPQL,
  truyền `PageRequest.of(trang, kichThuoc)` không kèm `Sort` (xem `LichHenRepository.timCuaTaiKhoan` của booking-service).
- Lỗi: chỉ cần `throw`; `XuLyLoiHandler` trong `common` chuyển thành
  `{"thanhCong": false, "maLoi": "...", "thongDiep": "...", "chiTiet": [{"truong", "thongDiep"}]}`
  với mã HTTP lấy từ `MaLoi`. Lỗi validation, 404, trùng khoá DB... đều đã được xử lý sẵn.
- Không viết `try/catch` để tự trả lỗi trong controller.

## 5. Bảo mật

- Mặc định **mọi API đều cần token** (JWT HS256, cùng `JWT_SECRET` ở mọi service).
- API không cần đăng nhập: khai báo trong `application.yml` của service:
  ```yaml
  app:
    bao-mat:
      duong-dan-cong-khai:
        - GET /api/catalog/chuyen-khoa/**
  ```
- Giới hạn vai trò: `@PreAuthorize("hasRole('QUAN_TRI_VIEN')")` (vai trò: `BENH_NHAN`, `BAC_SI`, `QUAN_TRI_VIEN`).
- Lấy người đang gọi API: `NguoiDungHienTai.layIdTaiKhoan()`, `NguoiDungHienTai.layVaiTro()`,
  `NguoiDungHienTai.layMaPhien()` (mã phiên đăng nhập = thiết bị đang dùng; `null` nếu token không có claim `phien`).
- Access token còn hạn tối đa 30 phút sau khi tài khoản bị vô hiệu hoá / xoá. API nào cần chắc tài khoản còn hoạt động
  thì kiểm tra lại trong DB; trong identity-service mọi API `/api/users/me/...` gọi `TaiKhoanService.layDangHoatDong(id)`
  (tài khoản không còn -> 401 `CHUA_DANG_NHAP`, bị vô hiệu hoá -> 403 `TAI_KHOAN_BI_VO_HIEU_HOA`).
  booking-service có `TaiKhoanService.layBenhNhanDangHoatDong(id)` cho việc đặt lịch khi đã đăng nhập (thêm 403
  `TAI_KHOAN_CHUA_XAC_THUC`, và `KHONG_CO_QUYEN` khi tài khoản không còn là bệnh nhân).
- API công khai nhưng **đăng nhập thì làm thêm việc** (mẫu: `POST /api/booking/lich-hen`, khách đặt được, bệnh nhân đăng
  nhập thì lịch lưu vào tài khoản): vẫn khai báo trong `duong-dan-cong-khai`, trong controller hỏi
  `NguoiDungHienTai.daDangNhap()` rồi mới gọi `layIdTaiKhoan()` / `layVaiTro()` (không có token thì 2 method này ném 401 `CHUA_DANG_NHAP`). Token hợp lệ
  trên đường dẫn công khai vẫn được đọc; token hết hạn / sai thì request nhận 401 dù API công khai, nên client chỉ gửi
  `Authorization` khi đang có phiên. `@PreAuthorize` không dùng được cho trường hợp này: tự kiểm tra vai trò trong controller.
- Thử API cần đăng nhập: lấy `accessToken` từ `POST /api/auth/login` (Swagger, hoặc trang demo
  `node scripts/demo-xac-thuc/server.js`, xem README). Có sẵn tài khoản mẫu cho cả 3 vai trò: `admin@eclinic.local`,
  `bacsi01@eclinic.local`, `benhnhan01@eclinic.local` (mật khẩu xem README).
- Refresh token nằm trong cookie HttpOnly `eclinic_rt` (`Path=/api/auth`), không có trong body. Không ghi mật khẩu,
  token, email ra log; sự kiện bảo mật chỉ ghi id (xem [BAO-MAT-XAC-THUC.md](BAO-MAT-XAC-THUC.md)).
- CORS chỉ cấu hình ở api-gateway, không thêm `@CrossOrigin` trong service.

## 6. Database và migration

- Schema do **Flyway** quản lý, file nằm trong `common/src/main/resources/db/migration`.
- Muốn đổi schema: tạo file **mới** `V<số tiếp theo>__mo_ta_ngan.sql` (hiện đã có V1–V5, file tiếp theo là `V6__...sql`).
  **Không bao giờ sửa file đã chạy** (kể cả `V1__init_schema.sql`): Flyway so checksum và service sẽ không khởi động.
- Sửa entity trong `common` cho khớp: Hibernate chạy `ddl-auto: validate`, entity lệch schema thì service báo lỗi.
- DB dùng collation `utf8mb4_unicode_ci`: so sánh `=` và `LIKE` không phân biệt hoa thường và dấu
  (tìm "noi" ra "Nội"). Đừng dựa vào khác biệt `đ`/`d`.

## 7. Dữ liệu của ai, gọi service khác khi nào

- Mọi service **được đọc** mọi bảng qua entity dùng chung trong `common`. Repository đọc bảng của miền khác đặt tên
  `<Ten>ChiDocRepository` và `extends Repository<Entity, Long>` (không phải `JpaRepository`), nên không có `save` / `delete`
  (xem `identity-service/src/main/java/iuh/fit/se/eclinic/identity/repository/BacSiChiDocRepository.java`).
- Mỗi service **chỉ ghi** vào bảng thuộc miền của mình.
  Ngoại lệ duy nhất: dữ liệu mẫu (package `dulieumau` của identity-service, chỉ chạy khi `SEED_DATA=true`) ghi cả bảng của
  catalog và booking, vì ca làm việc cần admin, bác sĩ, phòng khám có trước mà các service khởi động song song.
- Cần ghi vào bảng của miền khác → gọi REST sang service sở hữu, bằng `RestClient` đặt trong package `client/`.
  Địa chỉ lấy từ cấu hình (ví dụ `${CATALOG_URL:http://localhost:8082}`); trong Docker là `http://catalog-service:8082`.
  Ví dụ đầu tiên: tạo tài khoản bác sĩ (AUTH-04/ADM-02) — tài khoản do identity ghi, hồ sơ bác sĩ do catalog ghi.
- Việc cần cập nhật nhiều bảng trong **một transaction** phải nằm trong cùng một service
  (vì vậy lịch làm việc và lịch hẹn đều ở `booking-service`).
- Gọi dịch vụ bên ngoài (Cloudinary, Google...) **không nằm trong transaction DB**: gọi xong mới mở transaction ngắn để lưu
  (xem `AnhDaiDienServiceImpl`, class này không có `@Transactional`). Việc phải làm **sau khi commit** (gửi email, xoá ảnh
  của tài khoản vừa xoá): phát event rồi xử lý bằng `@Async @TransactionalEventListener(phase = AFTER_COMMIT)`
  (xem `EmailServiceImpl`).

## 8. Test

| Loại | Dùng khi | Mẫu |
|---|---|---|
| Controller | Kiểm tra URL, validation, phân quyền, JSON trả về; service được mock | `catalog-service/src/test/java/iuh/fit/se/eclinic/catalog/ChuyenKhoaControllerTest.java` |
| Service | Kiểm tra logic + truy vấn với MySQL thật (Testcontainers) | `catalog-service/src/test/java/iuh/fit/se/eclinic/catalog/ChuyenKhoaServiceTest.java` |

- Test controller: `@WebMvcTest(XController.class)` + **`@AutoConfigureJson`** (thiếu là lỗi, Spring Boot 4.1
  không có JSON mapper trong slice này) + `@Import(BaoMatConfig.class)` + `@MockitoBean` cho service.
- Giả lập người dùng: `.with(NguoiDungGiaLap.quanTriVien())`, `.with(NguoiDungGiaLap.bacSi(5L))`,
  `.with(NguoiDungGiaLap.benhNhan(12L))` (có sẵn trong test-jar của `common`).
- Test service: `@SpringBootTest` + `@Import(MySqlTestcontainersConfiguration.class)`; dữ liệu test nên có hậu tố
  ngẫu nhiên (UUID) để các test không đụng nhau. Cần Docker đang chạy. Service dùng Redis thì import thêm
  `RedisTestcontainersConfiguration.class`.
- Các `@SpringBootTest` của một module dùng chung 1 context và 1 DB. Trong identity-service: **không tạo tài khoản
  `QUAN_TRI_VIEN` trong test** (làm hỏng `KhoiTaoAdminTest`), cần admin thì dùng `admin@eclinic.local`; số điện thoại là
  duy nhất nên dùng số ngẫu nhiên (`09` + 8 chữ số).
- Client gọi dịch vụ bên ngoài được mock bằng `@MockitoBean` (ví dụ `LuuTruAnh` trong `AnhDaiDienServiceTest`), không gọi
  dịch vụ thật. Việc chạy bất đồng bộ sau commit kiểm tra bằng `verify(mock, timeout(5000))` / `after(300).never()`.
- Test cần module khai báo dependency `common` loại `test-jar` (xem `catalog-service/pom.xml`).

## 9. Lưu ý khác

- **Java 17**: không dùng tính năng Java 21 (ví dụ pattern matching trong `switch`).
- Giờ hệ thống: mọi service chạy múi giờ `Asia/Ho_Chi_Minh` (`AppTimeZone`), dùng `LocalDateTime`.
- **Tải tệp lên** (mẫu: ảnh đại diện, `AnhDaiDienController`): `@PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)` +
  `@RequestPart("<tên phần>") MultipartFile`.
  - Giới hạn dung lượng đặt ở `spring.servlet.multipart.max-file-size` / `max-request-size` của service (vượt -> 413
    `TEP_QUA_LON`, đã xử lý sẵn trong `XuLyLoiHandler`), kèm `server.tomcat.max-swallow-size` lớn hơn giới hạn của gateway.
  - Gateway từ chối mọi request quá 5 MB (`GATEWAY_MAX_REQUEST_SIZE`, 413 `YEU_CAU_QUA_LON`); tệp lớn hơn thì phải nâng cả hai.
  - Loại tệp nhận ra bằng **các byte đầu của nội dung** (`DinhDangAnh`), không tin tên tệp hay `Content-Type` client gửi;
    tên lưu trữ do server đặt, không lấy từ tên tệp.
- Git trên Windows để `core.autocrlf=true`; file trong repo lưu xuống dòng LF, máy Windows thấy CRLF — bình thường.
- Không commit file `.env` (chứa mật khẩu, khoá) — đã có trong `.gitignore`.
