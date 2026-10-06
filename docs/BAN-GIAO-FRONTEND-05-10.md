# Bàn giao cho frontend — đối chiếu với "Danh sách UI hiện đang có 05_10_2026"

Tài liệu này đi theo từng màn hình trong file UI ngày 05/10: màn hình lấy dữ liệu từ API nào, frontend cần đổi gì cho khớp
backend, và màn hình nào backend chưa có. Mọi API gọi qua gateway `http://localhost:8080`; phản hồi luôn bọc trong
`{ thanhCong, thongDiep, duLieu }`, lỗi có `maLoi` và `chiTiet[]` (xem `docs/QUY-UOC-CODE.md` mục 4). Chi tiết từng trường
xem Swagger của từng service. Trang mẫu chạy được: `node scripts/demo-xac-thuc/server.js` rồi mở
`http://localhost:5173/dat-lich` (mã nguồn `scripts/demo-dat-lich/index.html`, một file, không framework) và
`http://localhost:5173/lich-hen` (`scripts/demo-lich-hen/index.html`: lịch hẹn dạng lịch và trang cá nhân của bệnh nhân, màn hình
của bác sĩ; dùng FullCalendar 6 tải từ CDN).

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
  `THIEU_NGUOI_GIAM_HO` / `NGUOI_GIAM_HO_KHONG_HOP_LE` (400), `THONG_TIN_BENH_NHAN_KHONG_KHOP` (409: đặt cho bản thân với
  số CCCD khác hồ sơ của tài khoản), `HO_SO_CHO_XAC_MINH` (409), 429 khi 1 địa chỉ IP đặt quá nhiều lần.
- Số CCCD đã có hồ sơ mà họ tên / ngày sinh nhập khác hồ sơ: **vẫn đặt được** (201). `hoTenBenhNhan`, `hoTenNguoiGiamHo`
  trong kết quả, trên phiếu khám và ở "lịch hẹn của tôi" là đúng những gì người đặt vừa nhập; hồ sơ đã lưu không bị sửa.

Điền sẵn cho bệnh nhân đã đăng nhập (vai trò `BENH_NHAN`):

- `GET /api/booking/thong-tin-dat-lich/cua-toi` → `banThan` (hồ sơ của tài khoản hoặc null), `emailTaiKhoan`, `nguoiThan[]`
  (những người tài khoản này đã đặt hộ, đúng như đã nhập, kèm `nguoiGiamHo`), `lanDatGanNhat`
  `{ idChuyenKhoa, tenChuyenKhoa, idBacSi, hoTenBacSi }`. Tên trường của `nguoiThan[]` trùng với body đặt lịch nên chép thẳng
  vào form được. Tối đa 10 người, người đặt gần nhất đứng đầu.
- `PUT /api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id}` → sửa bản lưu của 1 người: body như `benhNhan` của đặt lịch
  nhưng **không có `cccd`**, kèm `nguoiGiamHo` (bỏ trống = bản lưu không còn người giám hộ). Trả về dòng `nguoiThan` đã
  sửa. Chỉ đổi bản lưu của tài khoản, không đổi hồ sơ bệnh nhân hay lịch đã đặt.
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
- Màn hình lịch (tháng / tuần / ngày): `GET /api/booking/lich-hen/cua-toi/lich?tuNgay=&denNgay=&cuaAi=` → **mảng** các
  dòng như trên (không phân trang), mọi trạng thái, theo giờ khám. Gửi đúng khoảng ngày lịch đang hiện, tối đa 42 ngày (vượt
  → 400). Gợi ý vẽ: 1 lịch hẹn = 1 mục bắt đầu ở `gioKhamDuKien` (API không có giờ kết thúc của lượt khám: vẽ dài bằng
  thời lượng lượt, hoặc 15 phút); màu theo `laBanThan` và `trangThai`; bấm vào thì gọi chi tiết bằng `maPhieuKham`.
- Tách "Lịch của tôi" / "Lịch của người khác": thêm `cuaAi=BAN_THAN|NGUOI_KHAC` (mặc định `TAT_CA`), kết hợp được với
  `loc`. `BAN_THAN` = người khám là chủ tài khoản, **bất kể ai đặt**; `NGUOI_KHAC` = lịch đặt cho người thân hoặc lịch mà
  chủ tài khoản là người giám hộ. Tài khoản chưa có hồ sơ bệnh nhân thì `BAN_THAN` rỗng.
- Mỗi dòng có thêm `nguoiDat` (`TOI` / `KHACH` / `TAI_KHOAN_KHAC`: hiện nhãn "Bạn đặt" / "Đặt không đăng nhập" / "Người
  khác đặt") và `thongTinKhacHoSo`. `thongTinKhacHoSo: true` chỉ có ở lịch của tôi: người đặt đã nhập họ tên / ngày sinh
  khác hồ sơ của tôi → hiện cảnh báo "Lịch này được đặt với thông tin khác hồ sơ của bạn; nếu không phải lịch của bạn,
  hãy liên hệ phòng khám". `hoTenBenhNhan` ở lịch của tôi là họ tên trong hồ sơ của tôi; ở lịch của người khác là họ tên
  tôi đã nhập.
- `GET /api/booking/lich-hen/cua-toi/{maPhieuKham}` → chi tiết 1 lịch: `lichHen` (đúng như dòng danh sách),
  `ngaySinhBenhNhan`, `gioiTinhBenhNhan`, `soDienThoaiLienHe`, `emailLienHe` (2 trường liên hệ null nếu không phải tôi
  đặt). Lịch không thuộc tài khoản → 404. Không có id số: luôn dùng `maPhieuKham`.
- Trang cá nhân: `GET /api/booking/trang-ca-nhan/cua-toi` → `hoSo` (như `GET .../ho-so-benh-nhan/cua-toi`, null khi chưa
  có), `emailTaiKhoan`, `nguoiThan[]` (sửa / xoá bằng `PUT` / `DELETE .../nguoi-than/{id}`), `lichSapToi { cuaToi[],
  cuaNguoiKhac[] }` (5 lịch gần nhất mỗi bên, cùng dạng dòng danh sách), `soLich { sapToiCuaToi, sapToiCuaNguoiKhac,
  lichSu, daKham }`, `lanKhamGanDay[]` (5 lượt đã khám gần nhất, cùng dạng với lịch sử khám). Xem thêm thì gọi danh sách
  với `loc` + `cuaAi`, hoặc lịch sử khám bên dưới.
- Lịch sử khám: `GET /api/booking/lich-su-kham/cua-toi?cuaAi=TAT_CA|BAN_THAN|NGUOI_KHAC&trang=&kichThuoc=` → các lượt đã
  khám xong, mới nhất trước. Mỗi dòng `{ lichHen, ketQua }`: `lichHen` như dòng "lịch hẹn của tôi", `ketQua { chanDoan,
  ghiChu, ngayTaiKhamDeXuat, donThuoc[] { tenThuoc, donVi, lieuDung, soLanMoiNgay, soNgayDung, ghiChuSuDung } }` (trường
  không có giá trị là `null`, không kê đơn thì `donThuoc: []`). `ngayTaiKhamDeXuat` chỉ là gợi ý, không phải lịch hẹn.
- Khi nào hiện nút "Xem kết quả" ở danh sách lịch hẹn: `trangThai = DA_HOAN_THANH` **và** (`laBanThan` hoặc
  `nguoiDat = TOI`). Chi tiết `GET .../lich-hen/cua-toi/{maPhieuKham}` trả thêm `ketQua` theo đúng quy tắc đó; lịch chỉ
  thấy vì tôi là người giám hộ (người khác đặt) thì `ketQua` không có.
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
| Schedule: lịch hẹn theo khoảng ngày | `GET /api/booking/bac-si/toi/lich-hen/lich?tuNgay=&denNgay=` | Mảng các dòng như danh sách trong ngày, tối đa 42 ngày. Vẽ cùng lịch làm việc: ca là khối từ `gioBatDau` đến `gioKetThuc` kèm `soLuotDaDat`/`tongSoLuot`, lịch hẹn là mục ở `gioKhamDuKien`; `doiChieu.canDoiChieu` nên có màu cảnh báo |
| Tìm trong danh sách trong ngày | `GET /api/booking/bac-si/toi/lich-hen?ngay=&idLichLamViec=&soThuTu=&tuKhoa=` | Mọi tham số đều không bắt buộc; `ngay` bỏ trống = hôm nay. `soThuTu` khớp đúng số (số thứ tự tính theo phòng + ngày, thêm `idLichLamViec` nếu bác sĩ có 2 ca khác phòng). `tuKhoa` = một phần họ tên, không xét dấu / hoa thường, khớp cả tên trong hồ sơ lẫn tên người đặt nhập. Mỗi dòng có `id`, `idHoSoBenhNhan`, `doiChieu` |
| Consultation: nhập mã / quét QR | `GET /api/booking/bac-si/toi/lich-hen/tra-cuu?ma=` | `ma` = `maTraCuu`, `maPhieuKham`, hoặc **cả chuỗi link** máy quét QR trả về (`.../phieu-kham/<mã>`). Lịch của bác sĩ khác → 404 |
| Hồ sơ khám của bệnh nhân | `GET /api/booking/bac-si/toi/lich-hen/{id}/ho-so-kham` (bấm vào 1 dòng / số thứ tự), `GET /api/booking/bac-si/toi/lich-hen/tra-cuu/ho-so-kham?ma=` (gõ mã / quét QR, `ma` như trên) | Cùng 1 kết quả: `lichHen` (như dòng danh sách), `hoSoBenhNhan { id, cccd, hoTen, ngaySinh, tuoi, gioiTinh, soDienThoai, diaChi, soBaoHiemYTe, tienSuBenhLy, daLienKetTaiKhoan }` (CCCD không che), `lanKhamTruoc[] { idLichHen, maTraCuu, ngay, gioKhamDuKien, bacSi, tenChuyenKhoa, trangThai, lyDoKham, ketQua { chanDoan, ghiChu, ngayTaiKhamDeXuat } }` mới nhất trước, gồm cả lần khám với bác sĩ khác; `ketQua` null khi lần đó chưa có bệnh án. Lịch của bác sĩ khác → 404 |
| Đối chiếu giấy tờ | `PUT /api/booking/bac-si/toi/lich-hen/{id}/benh-nhan` `{ hoTen, ngaySinh, gioiTinh?, soDienThoai, diaChi?, soBaoHiemYTe?, cccd? }`, `POST .../lich-hen/{id}/da-doi-chieu` | Khi `lichHen.doiChieu.canDoiChieu`: bác sĩ sửa hồ sơ cho đúng giấy tờ (trả lại hồ sơ khám mới; `cccd` chỉ điền được khi hồ sơ chưa có số) rồi bấm "đã đối chiếu". Hai thao tác độc lập |
| Doctor Profile (chỉ xem) | `GET /api/users/me` | Hồ sơ giới thiệu công khai: `GET /api/catalog/bac-si/{id}` |
| Consultation: ghi kết quả khám | `POST /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an` `{ chanDoan, ghiChu?, ngayTaiKhamDeXuat?, donThuoc?: [{ tenThuoc, donVi?, lieuDung?, soLanMoiNgay?, soNgayDung?, ghiChuSuDung? }] }` | 201; lịch hẹn chuyển sang `DA_HOAN_THANH` cùng lúc. Chỉ với lịch hẹn của chính bác sĩ (khác → 404), còn `CHO_XAC_NHAN` / `DA_XAC_NHAN` (khác → 409 `LICH_HEN_KHONG_KHAM_DUOC`), và từ ngày khám trở đi (sớm hơn → 409 `CHUA_DEN_NGAY_KHAM`). `chanDoan` bắt buộc; `ngayTaiKhamDeXuat` phải sau hôm nay; đơn thuốc tối đa 30 dòng. Tên thuốc chưa có trong danh mục được tự thêm (chưa xác minh) |
| Consultation: xem / sửa kết quả đã ghi | `GET` / `PUT /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an` | `PUT` cùng body như `POST`, thay toàn bộ nội dung và đơn thuốc; lịch hẹn giữ `DA_HOAN_THANH`. Chưa có kết quả → 404. Kết quả cũng hiện ở hồ sơ khám (`ketQua`, và `lanKhamTruoc[].ketQua` kèm `donThuoc[]`) |
| Consultation: gợi ý tên thuốc | `GET /api/medical/bac-si/toi/thuoc?tuKhoa=` | Tối đa 20 dòng `{ id, tenThuoc, donVi, daXacMinh }`; từ khoá trống → `[]` |
| Dashboard, Appointment Request (nhận / từ chối), form xin đổi ca | **Chưa có backend** | BOOK-10, SCHED-02 thuộc task khác. Lịch mới luôn ở `CHO_XAC_NHAN` cho tới khi bác sĩ ghi kết quả khám |

## 4. Quản trị viên (vai trò `QUAN_TRI_VIEN`)

| Màn hình | API | Ghi chú |
|---|---|---|
| Schedule Management (chỉ xem) | `GET /api/booking/quan-tri/lich-lam-viec?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=&idPhongKham=` | Tối đa 42 ngày, cùng dạng dòng ca như của bác sĩ, kèm `bacSi`, `tenChuyenKhoa` |
| Bệnh nhân của 1 ca | `GET /api/booking/quan-tri/lich-lam-viec/{id}/lich-hen` | |
| Tra lịch hẹn theo mã | `GET /api/booking/quan-tri/lich-hen/tra-cuu?ma=` | Mỗi lịch hẹn (cả ở danh sách của bác sĩ và của 1 ca) có `idHoSoBenhNhan`, `benhNhan` (dữ liệu của hồ sơ) và `doiChieu` `{ canDoiChieu, hoTenDaNhap, ngaySinhDaNhap, gioiTinhDaNhap, hoTenGiamHoDaNhap }` (những gì người đặt nhập). `canDoiChieu: true` → hiện cảnh báo "thông tin nhập khác hồ sơ, cần đối chiếu giấy tờ" |
| Đã đối chiếu giấy tờ | `POST /api/booking/quan-tri/lich-hen/{id}/da-doi-chieu` | Bỏ `canDoiChieu` của lịch hẹn; gọi lại vẫn 200 |
| Patient Management: xem / sửa hồ sơ | `GET /api/booking/quan-tri/ho-so-benh-nhan/{id}`, `PUT .../{id}` `{ hoTen, ngaySinh, gioiTinh?, soDienThoai, diaChi?, soBaoHiemYTe?, cccd? }` | `{id}` là `idHoSoBenhNhan` của lịch hẹn. Số CCCD không che. `cccd` chỉ điền được khi hồ sơ chưa có số: đổi số đã có → 400, số thuộc hồ sơ khác → 409 `XUNG_DOT_DU_LIEU`. Sửa hồ sơ không tự bỏ `canDoiChieu` của lịch hẹn |
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
