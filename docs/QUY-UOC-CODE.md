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
     `common/src/main/java/iuh/fit/se/eclinic/common/exception/MaLoi.java` (mã HTTP) và 1 dòng `loi.<MA_LOI>=...` vào
     mỗi file `thong-diep/chung.properties`, `chung_en.properties` (xem mục 4, "Thông điệp 2 ngôn ngữ").
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

- Thành công: `PhanHoiApi.ok(duLieu)` hoặc `PhanHoiApi.ok(duLieu, "thanhCong.themChuyenKhoa")` (khoá thông điệp) →
  `{"thanhCong": true, "thongDiep": "...", "duLieu": ...}`.
- Có phân trang: service trả `TrangDuLieu.tu(page)` →
  `{"noiDung": [...], "trang", "kichThuoc", "tongSoPhanTu", "tongSoTrang"}`.
  `@Query` phân trang có `join fetch` phải khai báo thêm `countQuery` (không có `fetch`) và viết `order by` ngay trong JPQL,
  truyền `PageRequest.of(trang, kichThuoc)` không kèm `Sort` (xem `LichHenRepository.timCuaTaiKhoan` của booking-service).
- Lỗi: chỉ cần `throw`; `XuLyLoiHandler` trong `common` chuyển thành
  `{"thanhCong": false, "maLoi": "...", "thongDiep": "...", "chiTiet": [{"truong", "thongDiep"}]}`
  với mã HTTP lấy từ `MaLoi`. Lỗi validation, 404, trùng khoá DB... đều đã được xử lý sẵn.
- Không viết `try/catch` để tự trả lỗi trong controller.

### Thông điệp 2 ngôn ngữ (tiếng Việt / tiếng Anh)

Request chọn ngôn ngữ bằng header `Accept-Language`: `en...` là tiếng Anh; không gửi, hoặc ngôn ngữ khác, là tiếng Việt.
`maLoi` không đổi theo ngôn ngữ, chỉ `thongDiep` đổi. **Không viết câu chữ cho người dùng thẳng trong mã Java**: viết
khoá, câu nằm trong `common/src/main/resources/thong-diep/`:

| File | Chứa |
|---|---|
| `chung.properties` / `chung_en.properties` | thông điệp của `MaLoi`, lỗi request sai dạng, câu dùng ở 2 service trở lên |
| `identity`, `catalog`, `booking`, `medical`, `notification` (+ `_en`) | câu riêng của từng service |

File không có đuôi là tiếng Việt, `_en` là tiếng Anh; mọi khoá phải có ở cả 2 file. api-gateway có file riêng
(`api-gateway/src/main/resources/thong-diep/gateway*.properties`) vì không dùng module `common`.

| Loại | Viết trong mã | Khoá |
|---|---|---|
| Lỗi theo mã | `throw new LoiNghiepVu(MaLoi.X)` | `loi.X` |
| Lỗi có câu riêng / có số liệu | `throw new LoiNghiepVu(MaLoi.X, "loi.X.bienThe", String.valueOf(soLuong))` | `loi.X.bienThe=... {0} ...` |
| Kiểm tra dữ liệu vào | `@NotBlank(message = "{kiemTra.hoTen.batBuoc}")` | `kiemTra.<truong>.<luat>` |
| Thành công | `PhanHoiApi.ok(duLieu, "thanhCong.datLich")` | `thanhCong.<viec>` |

- Ràng buộc không ghi `message` (`@Min`, `@NotNull`...) dùng câu chung ở `chung*.properties`
  (`jakarta.validation.constraints.Min.message`...), không cần thêm gì.
- Câu có `{0}`, `{1}`: **viết dấu nháy đơn thành 2 dấu** (`''`), và đổi số / ngày giờ sang chuỗi trước khi truyền
  (`String.valueOf`), để 2 ngôn ngữ hiện cùng 1 dạng số. Câu `kiemTra.*` chỉ dùng tham số có tên của Bean Validation
  (`{min}`, `{max}`, `{value}`) và viết dấu nháy bình thường.
- Thông điệp thành công có số liệu: controller tự soạn bằng `MessageSource` (xem `PhienDangNhapController`), vì
  `DichThongDiepAdvice` chỉ đổi khoá không có tham số.
- Khoá không có trong file thì chính chuỗi đó được trả về (nhờ vậy thông điệp chuyển nguyên từ service khác qua
  `GoiNoiBo` vẫn hiện đúng). Vì thế viết sai tên khoá sẽ không báo lỗi khi chạy: **chạy
  `node scripts/kiem-tra-thong-diep.js` sau khi thêm / sửa thông điệp**.
- Cần biết ngôn ngữ trong mã: `NgonNgu.hienTai()` (trong request), `NgonNgu.cuaYeuCau(request)` (ở filter). Ngoài request
  (job, `@Async`) không có ngôn ngữ của request: dùng `tai_khoan.ngon_ngu` (ngôn ngữ ưa dùng đã lưu, đổi bằng
  `PUT /api/users/me/language`).
- Client nội bộ gọi trong request của người dùng phải gửi kèm `Accept-Language` (xem `GoiNoiBo.taoRestClient`).
- **Thông báo (chuông)** không lưu câu soạn sẵn: lưu **khoá mẫu + tham số**, notification-service soạn câu lúc đọc theo
  ngôn ngữ của request đang đọc chuông.
  - Mẫu nằm trong `thong-diep/notification*.properties`, khoá `thongBao.<mau>[.<bienThe>]`, tham số `{0}`, `{1}`...
    Phần có / không (lý do, tầng) là 1 khoá biến thể riêng, không ghép chuỗi trong Java.
  - Tham số dựng bằng `ThamSoThongBao` theo đúng thứ tự của mẫu: `chu(...)` chép nguyên văn (họ tên, mã tra cứu, lý do
    người dùng gõ); `gioNgay / ngay / gio` viết theo ngôn ngữ; `haiNgonNgu(vi, en)` cho tên phòng, tầng;
    `khoa("heThong....")` cho câu do hệ thống viết. Chữ "Bác sĩ" / "Dr." nằm trong mẫu, tham số chỉ là họ tên.
  - Service nguồn ghi `su_kien_thong_bao.khoa_mau` + `tham_so` (không ghi `noi_dung`). `thong_bao.noi_dung` do
    notification-service ghi bản tiếng Việt (dự phòng, và là chữ của thông báo tạo trước V18).
  - Soạn câu ở nơi khác (email nhắc lịch...): `SoanThongBao.soan(khoaMau, thamSoJson, ngonNgu)`.
- Câu do hệ thống viết vào 1 cột chữ tự do (vd `yeu_cau_doi_lich.ghi_chu_xu_ly` khi ca bị hủy làm đóng yêu cầu): lưu bằng
  `VanBanHeThong.ghi("heThong.<khoa>", phanNguoiDungGo)`, lúc trả ra gọi `VanBanHeThong.doc(...)`.
- **Email**: câu chữ nằm trong `thong-diep/identity*.properties` (email tài khoản) và `notification*.properties` (email lịch
  hẹn, mục 7), khoá `email.<ten>.<phan>`; khung HTML dùng chung là `common/thongbao/KhungEmail`. Giá trị chèn vào câu được escape trước, câu trong file được phép có `<b>`. Email gửi sau khi request kết
  thúc nên event phải mang `NgonNgu ngonNgu` (lấy `taiKhoan.getNgonNgu()` lúc phát event). Tài khoản bác sĩ do quản
  trị viên tạo mặc định tiếng Việt.

### Nội dung danh mục 2 ngôn ngữ (chuyên khoa, phòng khám, hồ sơ bác sĩ, thuốc)

Khác với thông điệp (nằm trong file `thong-diep/`), nội dung danh mục do quản trị viên nhập nên nằm trong DB: mỗi cột chữ
dịch được có thêm 1 cột `<ten>_en` cho phép NULL ngay sau nó (`ten_chuyen_khoa` + `ten_chuyen_khoa_en`). Cột tiếng Việt
vẫn là cột bắt buộc; bản tiếng Anh trống thì người xem tiếng Anh thấy bản tiếng Việt. **Không dịch**: họ tên, học vị (chữ
viết tắt), tên thuốc. Chữ người dùng tự gõ (lý do khám, chẩn đoán, đánh giá...) giữ nguyên như đã gõ, không có cột `_en`.

- Entity có 1 hàm cho mỗi trường dịch được, nhận ngôn ngữ làm tham số: `chuyenKhoa.ten(ngonNgu)`,
  `phongKham.tang(ngonNgu)`, `bacSi.tieuSu(ngonNgu)`... (bên trong gọi `NgonNgu.chon(tiengViet, tiengAnh)`). Trong
  request truyền `NgonNgu.hienTai()`; ngoài request (job, email) truyền ngôn ngữ của người nhận.
- **API không nằm dưới `/quan-tri/`**: giữ nguyên dạng response, trường sẵn có mang chữ của ngôn ngữ được yêu cầu. Viết
  `chuyenKhoa.ten(NgonNgu.hienTai())`, không viết `getTenChuyenKhoa()`.
- **API quản lý danh mục dưới `/quan-tri/`** (và request thêm / sửa): chữ của chính dòng đó trả đúng như đã lưu, trường
  sẵn có là bản tiếng Việt, trường `...En` là bản tiếng Anh (null khi chưa nhập), để form sửa đọc ra và ghi lại được cả 2
  bản. Tên chỉ để tham chiếu sang dòng khác (`tenChuyenKhoa` của phòng khám, của bác sĩ) vẫn theo ngôn ngữ của request.
- Tên tiếng Anh của chuyên khoa, phòng khám không trùng nhau (UNIQUE trên cột `_en`, cho phép nhiều dòng NULL); trùng thì
  ném đúng mã lỗi của tên tiếng Việt kèm khoá `.tiengAnh`. Từ khoá tìm kiếm so với tên của cả 2 ngôn ngữ.
- Chuỗi rỗng ở trường `...En` phải lưu thành NULL (để UNIQUE và việc lấy bản tiếng Việt thay thế chạy đúng).

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
- Đường dẫn theo người gọi: API chỉ quản trị viên dùng nằm dưới `/api/<service>/quan-tri/...` (vd
  `/api/catalog/quan-tri/bac-si/{id}`, `/api/booking/quan-tri/lich-lam-viec`); API của bác sĩ về dữ liệu của chính mình nằm
  dưới `/api/booking/bac-si/toi/...` và **không nhận id bác sĩ**: lấy bác sĩ từ token bằng
  `TaiKhoanService.layBacSiDangHoatDong(idTaiKhoan)`. Đường dẫn công khai của booking-service: `GET /api/booking/khung-gio/**`,
  `POST /api/booking/lich-hen`, `GET /api/booking/phieu-kham/**`.
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
  `node scripts/demo/server.js`, xem README). Có sẵn tài khoản mẫu cho cả 3 vai trò: `admin@eclinic.local`,
  `bacsi01@eclinic.local`, `benhnhan01@eclinic.local` (mật khẩu xem README).
- Refresh token nằm trong cookie HttpOnly `eclinic_rt` (`Path=/api/auth`), không có trong body. Không ghi mật khẩu,
  token, email ra log; sự kiện bảo mật chỉ ghi id (xem [BAO-MAT-XAC-THUC.md](BAO-MAT-XAC-THUC.md)).
- CORS chỉ cấu hình ở api-gateway, không thêm `@CrossOrigin` trong service.

## 6. Database và migration

- Schema do **Flyway** quản lý, file nằm trong `common/src/main/resources/db/migration`.
- Muốn đổi schema: tạo file **mới** `V<số tiếp theo>__mo_ta_ngan.sql` (hiện đã có V1–V28, file tiếp theo là `V29__...sql`).
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
  Ngoại lệ thứ hai, có chủ đích: khi bác sĩ ghi nhận kết quả khám, medical-service đổi `lich_hen.trang_thai` sang
  `DA_HOAN_THANH` trong cùng transaction với việc lưu hồ sơ bệnh án (`LichHenKhamRepository`, `KhamBenhServiceImpl`), để
  bệnh án và trạng thái lịch hẹn không bao giờ lệch nhau. Qua repository đó chỉ được đổi `trangThai`.
- Cần ghi vào bảng của miền khác → gọi REST sang service sở hữu, bằng `RestClient` đặt trong package `client/`.
  Địa chỉ lấy từ cấu hình (ví dụ `${CATALOG_URL:http://localhost:8082}`); trong Docker là `http://catalog-service:8082`.
  Mẫu: quản lý bác sĩ — tài khoản do identity ghi, hồ sơ bác sĩ do catalog ghi, ca làm việc do booking ghi
  (`catalog/client/TaiKhoanClient`, `LichLamViecClient`, `QuanLyBacSiServiceImpl`).
- Một thao tác phải ghi ở nhiều service (thêm bác sĩ, cho bác sĩ ngừng công tác): **không** giữ transaction của mình trong
  lúc gọi service khác. Chia thành các bước, mỗi bước 1 transaction ngắn (`TransactionTemplate`) hoặc 1 lời gọi, và làm
  cho từng bước **gọi lại được** (đã ở trạng thái đích thì không làm gì). Bước sau lỗi thì trả 503
  `DICH_VU_NOI_BO_LOI` và người dùng gửi lại request; chỉ viết bước hoàn tác khi bước đầu tạo ra thứ không được để lại
  (tài khoản bác sĩ không có hồ sơ → `DELETE /noi-bo/tai-khoan/bac-si/{id}`).
- Client gọi API nội bộ đổi lỗi của bên kia qua `catalog/client/GoiNoiBo`: 4xx có thân `PhanHoiApi` (trừ 401) được ném lại
  nguyên mã lỗi cho người dùng; mất kết nối, quá thời gian chờ, 5xx, 401 (sai khoá nội bộ) thành `DICH_VU_NOI_BO_LOI`.
- API để service khác gọi đặt dưới `/noi-bo/**`, không dưới `/api/**`: gateway chỉ chuyển tiếp `/api/<service>/**` nên
  đường dẫn này không ra được bên ngoài. Bên nhận khai đường dẫn trong `app.bao-mat.duong-dan-cong-khai` (không có JWT của
  người dùng) và gọi `KhoaNoiBo.kiemTra(...)` ở đầu controller; bên gọi gửi `KhoaNoiBo.giaTri()` trong header
  `X-Khoa-Noi-Bo` (khoá chung `INTERNAL_API_KEY`). Mẫu: `booking/client/ThongBaoClient` gọi
  `notification/controller/NoiBoThongBaoController`. Các đường dẫn nội bộ đang có: `POST /noi-bo/thong-bao`,
  `POST /noi-bo/email` (notification), `/noi-bo/tai-khoan/**` (identity: tạo / xoá tài khoản bác sĩ, sửa họ tên và số điện thoại, vô hiệu hoá,
  kích hoạt lại), `POST /noi-bo/bac-si/{id}/huy-ca-sap-toi` (booking).
- Sự kiện của miền này phải tới miền khác và **không được mất** (thông báo cho lịch hẹn): không gọi REST trong transaction,
  cũng không gọi 1 lần sau khi commit. Ghi sự kiện vào bảng chờ gửi của chính service trong cùng transaction
  (`su_kien_thong_bao`), một job `@Scheduled` gửi các dòng chưa gửi và chỉ đánh dấu đã gửi khi bên nhận trả 2xx; bên nhận
  nhận ra sự kiện gửi lại bằng 1 mã duy nhất (`thong_bao.ma_nguon`). Mẫu: `ThongBaoLichHenServiceImpl`,
  `GuiThongBaoServiceImpl`, `GuiThongBaoJob`.
- **Việc theo giờ** (nhắc lịch khám): job `@Scheduled` chạy lặp và **hỏi DB "cái gì đến hạn"**, không hẹn giờ riêng cho
  từng dòng lúc tạo (hủy / đổi lịch khỏi phải hẹn lại). Mỗi việc đã làm ghi 1 dòng có UNIQUE (`nhac_lich_hen`: lịch hẹn +
  mốc) **trong cùng transaction** với các dòng chờ gửi, nên không làm 2 lần. Mỗi lịch hẹn 1 transaction riêng
  (`TransactionTemplate`), khoá dòng rồi kiểm tra lại trạng thái; chỉ khoá, không sửa `lich_hen`. Câu chữ ghi ngày giờ
  thật, không ghi "còn N ngày", để gửi trễ (ban đêm, service ngừng) vẫn đúng. Mẫu: `NhacLichServiceImpl`, `NhacLichJob`.
- **Theo dõi 1 giá trị service khác ghi** (nhắc tái khám): service ghi giá trị **không phát sự kiện**. medical-service chỉ
  ghi `ho_so_benh_an.ngay_tai_kham_de_xuat`; job của booking-service so giá trị hiện tại với **giá trị đã báo** (bảng
  `nhac_tai_kham`, mỗi hồ sơ bệnh án 1 dòng UNIQUE, ghi cùng transaction với các dòng chờ gửi). Hẹn mới, đổi ngày, xoá
  ngày và nhắc trước đều ra từ phép so đó, không phải hẹn lại gì khi bác sĩ sửa. Dùng dòng trạng thái (không phải mỗi
  lần gửi 1 dòng) vì giá trị có thể đổi rồi quay về như cũ. Migration thêm bảng trạng thái phải ghi sẵn dữ liệu đang có
  là "đã báo", nếu không lần chạy đầu sẽ gửi lại mọi thứ cũ. Mẫu: `NhacTaiKhamServiceImpl`, `NhacTaiKhamJob`, `V21`.
- **Email lịch hẹn** đi cùng bảng chờ gửi đó, kênh `EMAIL` (`su_kien_thong_bao.kenh`): dòng email ghi **địa chỉ nhận +
  ngôn ngữ** thay cho tài khoản nhận, vì người đặt có thể là khách. Email đi 2 chặng, mỗi chặng tự nhận ra dòng đã có:
  booking-service → notification-service (`POST /noi-bo/email`, trùng `maNguon` thì bỏ qua), rồi notification-service →
  máy chủ mail (bảng `email_gui`: gửi lỗi thì thử lại theo `app.email.cac-lan-cho`, hết lần thì `THAT_BAI`, đã `DA_GUI`
  thì không gửi lại). Mẫu: `ThongBaoLichHenServiceImpl.ghiEmail`, `notification/.../EmailServiceImpl`, `GuiEmailJob`.
  - 1 email = **mẫu + tham số** như thông báo chuông; notification-service soạn lúc gửi (`MauEmail`). Thêm 1 mẫu: thêm khoá
    vào `MauEmail.CAC_MAU`, thêm `email.<mau>` (tiêu đề, `{0}` = mã tra cứu) và `email.<mau>.moDau` vào
    `notification*.properties`; tham số theo đúng thứ tự ghi ở `MauEmail`.
  - Gửi tới: email liên hệ của lịch hẹn, để trống thì email của tài khoản đã đặt; khách không nhập email thì không gửi.
    Ngôn ngữ: của tài khoản đã đặt (`tai_khoan.ngon_ngu`); khách thì của request đã tạo lịch hẹn (`lich_hen.ngon_ngu`).
  - Email chỉ chứa những gì phiếu khám đã hiện cho người có link (họ tên bệnh nhân, giờ, bác sĩ, phòng, mã tra cứu, link
    phiếu khám). **Không** đưa lý do khám, chuyên khoa, số điện thoại, CCCD, chẩn đoán vào email.
  - Log của việc gửi email chỉ ghi id dòng `email_gui` và tên loại lỗi, không ghi địa chỉ nhận hay nội dung.
- Kho ảnh (Cloudinary) dùng chung nằm ở `common/luutru` (`LuuTruAnh`); service nào cần tải ảnh lên thì đặt
  `app.cloudinary.bat: true` trong `application.yml` của mình (identity-service, catalog-service).
  `application-common.yml` được **import** nên giá trị ở đó **đè** lên `application.yml` của service: khoá nào mỗi service
  cần đặt khác nhau thì không được khai trong file chung.
- Việc của miền này phải xảy ra "khi" miền khác có sự kiện mà chưa có lời gọi REST giữa 2 service: làm ở lần đọc kế tiếp
  của miền sở hữu dữ liệu. Mẫu: identity chỉ lưu số CCCD khai khi đăng ký (`tai_khoan.cccd_dang_ky`); booking-service liên
  kết hồ sơ bệnh nhân ở lần đọc đầu tiên của bệnh nhân (`LienKetHoSoService.thuLienKet`, transaction riêng, không bao giờ
  làm hỏng lần đọc). Nhờ vậy đăng nhập không phụ thuộc booking-service.
- Việc cần cập nhật nhiều bảng trong **một transaction** phải nằm trong cùng một service
  (vì vậy lịch làm việc và lịch hẹn đều ở `booking-service`).
- Gọi dịch vụ bên ngoài (Cloudinary, Google...) **không nằm trong transaction DB**: gọi xong mới mở transaction ngắn để lưu
  (xem `AnhDaiDienServiceImpl`, class này không có `@Transactional`). Việc phải làm **sau khi commit** (gửi email, xoá ảnh
  của tài khoản vừa xoá): phát event rồi xử lý bằng `@Async @TransactionalEventListener(phase = AFTER_COMMIT)`
  (xem `EmailServiceImpl`).
- Chatbot (`chatbot-service`): mô hình ngôn ngữ chỉ là **một bước** trong chuỗi xử lý cố định của `ChatbotServiceImpl`:
  chủ phiên → giới hạn của người gửi → dấu hiệu cấp cứu (trúng thì trả câu cố định, không gọi mô hình) → giới hạn toàn hệ
  thống → mô hình → dấu hiệu ngoài phạm vi (server thay bằng câu từ chối của mình) → lưu. Thêm bước mới thì đặt đúng chỗ
  trong chuỗi này, không nhờ mô hình làm thay.
  - **Chỉ dẫn hệ thống (prompt) không phải ranh giới bảo mật.** Ai được đọc gì do code quyết định trước khi gọi mô hình;
    mô hình chỉ nhận dữ liệu mà chính người hỏi được phép xem, và không bao giờ nhận quyền truy vấn tự do (không SQL do
    mô hình viết, chỉ các hàm tra cứu cố định, tham số "của ai" lấy từ JWT chứ không lấy từ lời mô hình).
  - Bước "mô hình" là 1 vòng có chặn (`hoiMoHinh`): mô hình xin tra cứu, code chạy hàm tra cứu rồi gọi lại. Chặn bằng số
    lần gọi và tổng thời gian (nhỏ hơn read-timeout của gateway); mỗi lần gọi đều tính vào giới hạn toàn hệ thống.
  - Hàm tra cứu cho mô hình nằm ở `TraCuuPhongKham`. Thêm 1 hàm: khai báo tên + tham số trong `CONG_CU`, thêm nhánh trong
    `thucHien`. Luật của mọi hàm: chỉ đọc; tham số do mô hình viết là dữ liệu chưa đáng tin (kiểm tra kiểu, kiểm tra id có
    thật); lỗi nào cũng thành kết quả báo lỗi cho mô hình chứ không ném exception; dữ liệu của service khác đọc qua API
    của service đó (không chép luật "còn chỗ" sang đây).
  - Hàm đọc dữ liệu CÁ NHÂN (`lich_hen_cua_toi`, `lan_kham_cua_toi`) theo thêm 6 luật: (1) không có tham số nào, tham số
    mô hình viết không được đọc; (2) "của ai" là `NguoiHoi`, dựng 1 lần trong `guiTinNhan` từ JWT của request, và dữ liệu
    đọc qua API "của tôi" của service chủ dữ liệu kèm chính JWT đó (`NguoiDungHienTai.layGiaTriToken()`), không đọc thẳng
    DB, không tạo API nội bộ nhận id tài khoản; (3) chỉ đưa cho mô hình khi người chat là bệnh nhân trong phiên của chính
    tài khoản đó, phiên của khách không bao giờ được; (4) chưa có đồng ý của phiên (`phien_chat.dong_y_du_lieu_luc`) thì
    không đọc gì, trả câu `CAN_DONG_Y`; (5) record của client chỉ khai báo trường cần dùng, mô hình chỉ nhận phần tối
    thiểu (không họ tên, không mã, không chữ do người dùng gõ), họ tên và mã chỉ vào thẻ `goiY`; (6) dòng ghi chú lịch
    sử không chứa dữ liệu cá nhân, và câu trả lời cũ có thẻ cá nhân chỉ gửi lại khi request hiện tại còn được đọc.
  - Thứ người dùng bấm vào được (bác sĩ, khung giờ, id để đặt lịch) trả ở `goiY`, do server dựng từ kết quả tra cứu.
    Không bao giờ tách id hay giờ khám ra từ câu chữ của mô hình.
  - Lời gọi mô hình nằm ngoài transaction DB. Mô hình lỗi, hết thời gian hay xin tra cứu mãi: 503, không lưu gì.
  - Không ghi nội dung tin nhắn, body lỗi của nhà cung cấp, khoá API hay JWT của người dùng vào log.
  - Câu do server viết (chào, từ chối, cấp cứu) nằm ở `thong-diep/chatbot*.properties`; chỉ dẫn hệ thống và danh sách dấu
    hiệu cấp cứu nằm ở `chatbot-service/src/main/resources/chatbot/`, không viết trong Java.

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
- **Trang demo** (`scripts/demo/`): chữ người dùng nhìn thấy viết thành cặp `t('Tiếng Việt', 'English')` ngay tại chỗ dùng,
  kể cả trong chuỗi HTML của `khuon()` (`${t(...)}`), `confirm` / `alert` và câu báo lỗi phía trình duyệt. Chữ khai báo 1
  lần lúc tải file (hằng số, `tieuDe` của `dangKy`) dùng `cap(vi, en)` hoặc bảng `nhan({...})`, hoặc viết thành hàm, vì
  lúc đó chưa biết ngôn ngữ. Dữ liệu từ API không đưa qua `t()`. Không đặt biến cục bộ tên `t`. Sửa màn hình xong chạy
  `node scripts/kiem-tra-demo-ngon-ngu.js` (in file:dòng của chuỗi tiếng Việt chưa có bản tiếng Anh; không nhận ra từ
  không dấu, phải tự đọc lại).
- Git trên Windows để `core.autocrlf=true`; file trong repo lưu xuống dòng LF, máy Windows thấy CRLF — bình thường.
- Không commit file `.env` (chứa mật khẩu, khoá) — đã có trong `.gitignore`.
