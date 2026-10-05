# Bàn giao cho frontend — đối chiếu với "Danh sách UI hiện đang có 05_10_2026"

Tài liệu này đi theo từng màn hình trong file UI ngày 05/10: màn hình lấy dữ liệu từ API nào, frontend cần đổi gì cho khớp
backend, và màn hình nào backend chưa có. Mọi API gọi qua gateway `http://localhost:8080`; phản hồi luôn bọc trong
`{ thanhCong, thongDiep, duLieu }`, lỗi có `maLoi` và `chiTiet[]` (xem `docs/QUY-UOC-CODE.md` mục 4). Chi tiết từng trường
xem Swagger của từng service. Trang mẫu chạy được: `node scripts/demo-xac-thuc/server.js` rồi mở
`http://localhost:5173/dat-lich` (mã nguồn `scripts/demo-dat-lich/index.html`, một file, không framework).

## 1. Những chỗ frontend phải đổi

| Trên UI 05/10 | Backend | Frontend cần làm |
|---|---|---|
| Lưới giờ 30 phút, mỗi ô "Available / Booked" | Khung **1 tiếng**, mỗi khung có `tongSoCho`, `soChoConLai`, `hetCho` (BOOK-12). Khung cuối ca có thể ngắn hơn 1 tiếng | Hiện mỗi khung 1 nút kèm "còn x/y", khoá nút khi `hetCho`. Giờ khám chính xác (`gioKhamDuKien`) và số thứ tự do server xếp, có trong phản hồi đặt lịch |
| "Any available doctor — clinic assigns automatically" | Server chọn bác sĩ còn nhiều chỗ nhất trong giờ đó | Gửi `idChuyenKhoa` + `gioBatDauKhung` (lấy từ `khung-gio/gop`), **không** gửi `idLichLamViec`. Phản hồi có `bacSi` đã xếp và `bacSiDoPhongKhamXep: true` |
| Form đăng ký có ô CCCD (không bắt buộc) | `POST /api/auth/register` nhận `cccd` (12 số hoặc bỏ trống) | Gửi thêm `cccd`. Không API nào trả lại số này |
| Form trẻ em không có ô CCCD của trẻ | Người khám dưới 18 tuổi (tính theo ngày khám) được bỏ trống `benhNhan.cccd`; từ đủ 18 tuổi bắt buộc 12 số | Không bắt nhập CCCD của trẻ. Người giám hộ vẫn bắt buộc `hoTen`, `quanHe`, `soDienThoai`, `cccd` |
| Form trẻ em không có ngày sinh người giám hộ | `nguoiGiamHo.ngaySinh` không bắt buộc (có gửi thì phải từ đủ 18 tuổi) | Không cần thêm ô |
| Giới tính, lý do khám có dấu * | `benhNhan.gioiTinh` (`NAM` / `NU` / `KHAC`) và `lyDoKham` bắt buộc | Thiếu thì server trả 400 kèm tên trường trong `chiTiet` |
| Phiếu khám có mã `ECL-20261005-4198` | Mỗi lịch hẹn có `maTraCuu` dạng `ECL-<ngày khám yyyyMMdd>-<4 số>` | Hiện `maTraCuu` trên phiếu. Mã này **không** mở được phiếu khám công khai: link và QR vẫn dùng `maPhieuKham` (43 ký tự) |
| Phiếu khám hiện đủ CCCD người giám hộ | Phiếu khám công khai (`GET /api/booking/phieu-kham/{ma}`) che CCCD và số điện thoại | Ngay sau khi đặt, muốn hiện đủ thì dùng giá trị người dùng vừa nhập trong form. Mở lại bằng link thì chỉ có bản đã che |
| Bác sĩ nhập "mã lượt khám" để tìm bệnh nhân | Tra theo `maTraCuu` hoặc `maPhieuKham` (nội dung QR) | `GET /api/booking/bac-si/toi/lich-hen/tra-cuu?ma=` |

## 2. Bệnh nhân

### Appointment (đặt lịch, một trang)

| Bước | API | Ghi chú |
|---|---|---|
| Chuyên khoa | `GET /api/catalog/chuyen-khoa?kichThuoc=100` | Công khai |
| Thẻ bác sĩ | `GET /api/catalog/bac-si?idChuyenKhoa=` | `id`, `hoTen`, `anhDaiDien`, `hocVi`, `chucVu`, `soNamKinhNghiem`, `gioiThieuNgan` |
| "Ngày sớm nhất còn chỗ" trên thẻ | `GET /api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa=` | `[{ idBacSi, ngay, soChoConLai }]`; ghép với thẻ theo `idBacSi`. Bác sĩ không còn chỗ thì không có dòng |
| Chi tiết bác sĩ (popup) | `GET /api/catalog/bac-si/{id}` | Thêm `tieuSu` (giới thiệu), `quaTrinhDaoTao[]`, `quaTrinhCongTac[]`, `linhVucKhamChua[]`, `anh[]` gồm `{ id, loai: ANH_CONG_VIEC \| CHUNG_CHI, url, chuThich }` theo đúng thứ tự hiển thị |
| Ngày + giờ của 1 bác sĩ | `GET /api/booking/khung-gio/nhieu-ngay?idBacSi=&tuNgay=&denNgay=` | Tối đa 7 ngày (mặc định hôm nay + 6). Mỗi ca có `idLichLamViec`, `bacSi`, `phongKham`, `khungGio[]` |
| Ngày còn chỗ (bác sĩ bất kỳ) | `GET /api/booking/khung-gio/ngay-con-cho?tuNgay=&denNgay=&idChuyenKhoa=` | |
| Giờ gộp (bác sĩ bất kỳ) | `GET /api/booking/khung-gio/gop?idChuyenKhoa=&ngay=` | `[{ gioBatDau, gioKetThuc, tongSoCho, soChoConLai, hetCho, soBacSi }]` |
| Đặt lịch | `POST /api/booking/lich-hen` | Xem body bên dưới. Khách không cần token; có token `BENH_NHAN` thì lịch lưu vào tài khoản |

Body đặt lịch:

```json
{
  "idLichLamViec": 27,
  "gioBatDauKhung": "2026-10-07T08:00:00",
  "benhNhan": { "hoTen": "", "ngaySinh": "2019-05-05", "gioiTinh": "NU", "soDienThoai": "", "cccd": null,
                "email": null, "soBaoHiemYTe": null, "diaChi": null },
  "nguoiGiamHo": { "hoTen": "", "quanHe": "ME", "soDienThoai": "", "cccd": "", "ngaySinh": null },
  "lyDoKham": "",
  "datChoBanThan": false,
  "luuNguoiThan": true
}
```

- Gửi **đúng một** trong `idLichLamViec` (bác sĩ cụ thể) hoặc `idChuyenKhoa` (bác sĩ bất kỳ); gửi cả hai hoặc không gửi → 400.
- `nguoiGiamHo` chỉ gửi khi người khám dưới 18 tuổi; `quanHe`: `CHA`, `ME`, `NGUOI_GIAM_HO_HOP_PHAP`, `KHAC`.
- `datChoBanThan` và `luuNguoiThan` chỉ có nghĩa khi đã đăng nhập. `luuNguoiThan: false` = không ghi nhớ người thân này.
- 201 trả `maPhieuKham`, `maTraCuu`, `linkPhieuKham`, `soThuTu`, `ngay`, `gioKhamDuKien`, `gioBatDauKhung`, `gioKetThucKhung`,
  `trangThai`, `bacSi`, `phongKham`, `hoTenBenhNhan`, `hoTenNguoiGiamHo`, `luuVaoTaiKhoan`, `bacSiDoPhongKhamXep`.
- Lỗi cần xử lý trên giao diện: `KHUNG_GIO_KHONG_CON_TRONG` (409, hết chỗ: tải lại số chỗ), `KHUNG_GIO_KHONG_KHA_DUNG` (409),
  `LICH_HEN_TRUNG_GIO` (409), `VUOT_GIOI_HAN_DAT_LICH` (409: 1 hồ sơ tối đa 3 lịch sắp tới, 1 số điện thoại tối đa 5),
  `THIEU_NGUOI_GIAM_HO` / `NGUOI_GIAM_HO_KHONG_HOP_LE` (400), `THONG_TIN_BENH_NHAN_KHONG_KHOP` (409: CCCD đã có hồ sơ với họ
  tên hoặc ngày sinh khác), `HO_SO_CHO_XAC_MINH` (409), 429 khi 1 địa chỉ IP đặt quá nhiều lần.

Điền sẵn cho bệnh nhân đã đăng nhập (vai trò `BENH_NHAN`):

- `GET /api/booking/thong-tin-dat-lich/cua-toi` → `banThan` (hồ sơ của tài khoản hoặc null), `emailTaiKhoan`, `nguoiThan[]`
  (những người tài khoản này đã đặt hộ, đúng như đã nhập, kèm `nguoiGiamHo`), `lanDatGanNhat`
  `{ idChuyenKhoa, tenChuyenKhoa, idBacSi, hoTenBacSi }`. Tên trường của `nguoiThan[]` trùng với body đặt lịch nên chép thẳng
  vào form được. Tối đa 10 người, người đặt gần nhất đứng đầu.
- `DELETE /api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id}` → bỏ 1 người khỏi danh sách (lịch đã đặt không đổi).
- `banThan.trangThaiLienKet = CHO_XAC_MINH` thì các trường khác đều null: hiện thông báo "hồ sơ đang chờ phòng khám xác
  minh", chưa cho đặt "cho bản thân".

### Appointment Ticket (phiếu khám)

`GET /api/booking/phieu-kham/{maPhieuKham}` (công khai) và `GET /api/booking/phieu-kham/{maPhieuKham}/qr` (ảnh PNG; thêm
`?taiVe=true` để tải về). Có `maTraCuu`, số thứ tự, giờ khám dự kiến, khung giờ, bác sĩ, phòng, lý do khám, bệnh nhân và
người giám hộ đã che thông tin, `canNguoiGiamHoDiCung`.

### Lịch hẹn của tôi, hồ sơ của tôi, tài khoản

- `GET /api/booking/lich-hen/cua-toi?loc=TAT_CA|SAP_TOI|LICH_SU&trang=&kichThuoc=`: gồm lịch tài khoản đã đặt (cho bản thân
  hoặc người thân), **và** mọi lịch của hồ sơ bệnh nhân đã liên kết với tài khoản (kể cả lịch đặt như khách trước khi có
  tài khoản), **và** lịch của người khám có người giám hộ khai đúng số CCCD của hồ sơ đó. Mỗi dòng có `maTraCuu`,
  `maPhieuKham`, `linkPhieuKham`, `laBanThan`.
- `GET` / `PUT /api/booking/ho-so-benh-nhan/cua-toi`: hồ sơ bệnh nhân của tài khoản (404 khi chưa có).
- Màn hình "User account": các API `/api/users/me...` của identity-service (xem README mục "Tài khoản của tôi").

### Liên kết lịch đã đặt như khách vào tài khoản (quy tắc #3)

Frontend không phải gọi API riêng. Sau khi đăng nhập, lần đầu trang gọi "lịch hẹn của tôi", "thông tin điền sẵn" hoặc "hồ
sơ của tôi", server tự tìm hồ sơ bệnh nhân có số CCCD đã khai lúc đăng ký:

- Họ tên và số điện thoại của tài khoản khớp hồ sơ → liên kết ngay (`DA_LIEN_KET`), lịch cũ hiện trong danh sách.
- Không khớp → hồ sơ `CHO_XAC_MINH`: tài khoản chưa thấy gì của hồ sơ cho tới khi quản trị viên duyệt.
- Tài khoản chưa khai CCCD (hoặc đăng nhập Google): nhập CCCD ở form "hồ sơ của tôi" (`PUT .../cua-toi`). Số đã có hồ sơ:
  khớp họ tên **và** (ngày sinh hoặc số điện thoại) → 200 `DA_LIEN_KET`; không khớp → 200 chỉ có
  `trangThaiLienKet: CHO_XAC_MINH`; hồ sơ đang thuộc tài khoản khác hoặc đã bị quản trị viên từ chối → 409 `CCCD_DA_CO_HO_SO`.

Cần biết khi thiết kế màn hình và khi viết báo cáo:

- Số điện thoại và họ tên của tài khoản là do người dùng tự khai, chỉ email được xác thực. Ai biết CCCD, họ tên và số điện
  thoại trên hồ sơ của một người và đăng ký trước thì xem được lịch sử lịch hẹn của người đó. Muốn chặn phải xác thực số
  điện thoại bằng SMS (chưa làm).
- Khai sai CCCD (trùng số của người khác) làm hồ sơ của người đó bị "giữ" ở trạng thái chờ xác minh dưới tài khoản khai
  sai, cho tới khi quản trị viên từ chối. Trong lúc đó khách vẫn đặt lịch bằng hồ sơ ấy được.
- Khách đặt cho trẻ có thể khai CCCD người giám hộ bất kỳ, nên lịch của trẻ có thể xuất hiện trong tài khoản của người bị
  khai là giám hộ (họ thấy tên trẻ, bác sĩ, giờ khám).

## 3. Bác sĩ (vai trò `BAC_SI`)

| Màn hình | API | Ghi chú |
|---|---|---|
| Schedule (ngày / tuần) | `GET /api/booking/bac-si/toi/lich-lam-viec?tuNgay=&denNgay=` | Tối đa 42 ngày. Mỗi ca: `idLichLamViec`, `ngay`, `gioBatDau`, `gioKetThuc`, `trangThai` (`HOAT_DONG` / `DA_HUY`), `phongKham`, `tongSoLuot`, `soLuotDaDat`, `soLuotConTrong` |
| Danh sách bệnh nhân trong ngày | `GET /api/booking/bac-si/toi/lich-hen?ngay=` | Theo giờ khám: `maTraCuu`, `trangThai`, `soThuTu`, `gioKhamDuKien`, `lyDoKham`, `benhNhan { hoTen, ngaySinh, tuoi, gioiTinh, soDienThoai }`, `nguoiGiamHo` |
| Consultation: nhập mã / quét QR | `GET /api/booking/bac-si/toi/lich-hen/tra-cuu?ma=` | `ma` = `maTraCuu` hoặc `maPhieuKham`. Lịch của bác sĩ khác → 404 |
| Doctor Profile (chỉ xem) | `GET /api/users/me` | Hồ sơ giới thiệu công khai: `GET /api/catalog/bac-si/{id}` |
| Dashboard, Appointment Request (nhận / từ chối), form xin đổi ca, khám bệnh và ghi bệnh án | **Chưa có backend** | BOOK-10, SCHED-02, EXAM-* thuộc task khác. Lịch mới luôn ở `CHO_XAC_NHAN` |

## 4. Quản trị viên (vai trò `QUAN_TRI_VIEN`)

| Màn hình | API | Ghi chú |
|---|---|---|
| Schedule Management (chỉ xem) | `GET /api/booking/quan-tri/lich-lam-viec?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=&idPhongKham=` | Tối đa 42 ngày, cùng dạng dòng ca như của bác sĩ, kèm `bacSi`, `tenChuyenKhoa` |
| Bệnh nhân của 1 ca | `GET /api/booking/quan-tri/lich-lam-viec/{id}/lich-hen` | |
| Tra lịch hẹn theo mã | `GET /api/booking/quan-tri/lich-hen/tra-cuu?ma=` | |
| Doctor Management: hồ sơ giới thiệu | `GET /api/catalog/quan-tri/bac-si/{id}`, `PUT .../{id}/ho-so` | PUT thay toàn bộ: `hocVi`, `chucVu`, `soNamKinhNghiem`, `gioiThieuNgan`, `tieuSu`, `quaTrinhDaoTao[]`, `quaTrinhCongTac[]`, `linhVucKhamChua[]` |
| Doctor Management: ảnh giới thiệu | `POST .../{id}/anh` (multipart: `anh`, `loai`, `chuThich`), `PUT .../{id}/anh/{idAnh}`, `PUT .../{id}/anh/thu-tu` `{ idAnh: [] }`, `DELETE .../{id}/anh/{idAnh}` | Tối đa 12 ảnh / bác sĩ (409 `VUOT_SO_ANH_BAC_SI`), mỗi ảnh tối đa 4 MB (413), JPEG / PNG / WebP |
| Patient Management: hồ sơ chờ xác minh | `GET /api/booking/quan-tri/ho-so-benh-nhan/cho-xac-minh?trang=&kichThuoc=`, `POST .../{id}/duyet`, `POST .../{id}/tu-choi` `{ lyDo }` | `{id}` là id hồ sơ. Mỗi dòng có `hoSo`, `taiKhoan`, `khopHoTen`, `khopSoDienThoai` để quản trị viên đối chiếu. Tài khoản đang có hồ sơ chờ xác minh thì chưa xoá được: từ chối trước |
| Medical Catalog: chuyên khoa | `/api/catalog/chuyen-khoa` (CRUD) | Có từ trước |
| User account, quản lý tài khoản | `/api/users...` | Có từ trước (README) |
| Dashboard, tạo / sửa ca làm việc, duyệt xin đổi ca, CRUD bác sĩ và phòng khám, danh sách bệnh nhân, đánh giá | **Chưa có backend** | DASH-*, SCHED-01/03/05. Ca làm việc hiện do dữ liệu mẫu tạo |

## 5. Dữ liệu mẫu để thử

`SEED_DATA=true`: `admin@eclinic.local` / `Admin@123`; `bacsi01..12@eclinic.local` và `benhnhan01..04@eclinic.local` /
`Demo@123`. 12 bác sĩ mẫu có đủ hồ sơ giới thiệu và 3 ảnh mẫu (ảnh lấy từ `placehold.co`, cần internet). Trong dữ liệu mẫu,
2 bác sĩ cùng chuyên khoa làm lệch buổi nhau nên giờ gộp của "bác sĩ bất kỳ" luôn có `soBacSi: 1`.
