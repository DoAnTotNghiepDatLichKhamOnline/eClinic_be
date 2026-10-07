# Bàn giao cho frontend — đối chiếu với "Danh sách UI hiện đang có 05_10_2026"

Tài liệu này đi theo từng màn hình trong file UI ngày 05/10: màn hình lấy dữ liệu từ API nào, frontend cần đổi gì cho khớp
backend. Mọi API gọi qua gateway `http://localhost:8080`; phản hồi luôn bọc trong
`{ thanhCong, thongDiep, duLieu }`, lỗi có `maLoi` và `chiTiet[]` (xem `docs/QUY-UOC-CODE.md` mục 4). Chi tiết từng trường
xem Swagger của từng service. Trang mẫu chạy được: `node scripts/demo/server.js` rồi mở
`http://localhost:5173` (không framework, không bước build). Trang mẫu đi theo đúng danh sách màn hình này: cùng đường dẫn,
cùng thanh bên của bác sĩ và quản trị viên. Mỗi màn hình là một file trong `scripts/demo/man-hinh/`, mở đầu bằng khối chú
thích ghi các API màn hình đó gọi. Lịch dùng FullCalendar 6 tải từ CDN.

| Đường dẫn | Vai trò | Màn hình UI | File trong `scripts/demo/man-hinh/` |
|---|---|---|---|
| `/login`, `/register`, `/forgot-password`, `/reset-password` | công khai | Authentication | `dang-nhap.js`, `dang-ky.js`, `quen-mat-khau.js`, `dat-lai-mat-khau.js` |
| `/verify-email`, `/confirm-email-change` | công khai | (liên kết trong email, ngoài UI) | `lien-ket-email.js` |
| `/appointment` | công khai | Patient: Appointment + Appointment Ticket | `dat-lich.js` |
| `/phieu-kham/<mã>` | công khai | Appointment Ticket (link trong mã QR) | `phieu-kham.js` |
| `/patient/appointments`, `/patient/profile` | bệnh nhân | (ngoài UI) lịch hẹn, kết quả khám, trang cá nhân | `bn-lich-hen.js`, `bn-ho-so.js` |
| `/account` | đã đăng nhập | menu tài khoản: Change password (+ hồ sơ, ảnh, đổi email, thiết bị) | `tai-khoan.js` |
| `/doctor/dashboard` | bác sĩ | Doctor: Dashboard | `bs-tong-quan.js` |
| `/doctor/work-schedule` | bác sĩ | Doctor: Work Schedule | `bs-lich-lam-viec.js` |
| `/doctor/appointment-requests` | bác sĩ | Doctor: Appointment Requests | `bs-yeu-cau.js` |
| `/doctor/consultation` | bác sĩ | Doctor: Consultation / EMR | `bs-kham-benh.js` |
| `/doctor/profile` | bác sĩ | Doctor: Doctor Profile | `bs-ho-so.js` |
| `/admin/dashboard` | quản trị viên | Admin: Dashboard | `qt-tong-quan.js` |
| `/admin/schedule-management` | quản trị viên | Admin: Schedule Management | `qt-lich-lam-viec.js` |
| `/admin/medical-catalog` | quản trị viên | Admin: Medical Catalog | `qt-danh-muc.js` |
| `/admin/doctors` | quản trị viên | Admin: Doctor Management | `qt-bac-si.js` |
| `/admin/patients` | quản trị viên | Admin: Patient Management | `qt-benh-nhan.js` |
| `/admin/accounts` | quản trị viên | (ngoài UI) quản trị tài khoản | `qt-tai-khoan.js` |

**Màn hình UI → API** (theo thứ tự của danh sách UI; cột cuối chỉ tới mục có chi tiết trong tài liệu này):

| Màn hình UI | API chính | Chi tiết |
|---|---|---|
| Login | `POST /api/auth/login`, `POST /api/auth/google`, `POST /api/auth/first-password` (bác sĩ mới) | README "Tài khoản và token"; mục "Doctor Management" (lần đăng nhập đầu) |
| Register, Forgot / Reset password | `POST /api/auth/register`, `/verify-email`, `/forgot-password`, `/reset-password` | README "Tài khoản và token" |
| Patient: Appointment | `GET /api/catalog/chuyen-khoa`, `GET /api/catalog/bac-si`, `GET /api/booking/khung-gio...`, `POST /api/booking/lich-hen` | Mục 2 "Appointment" |
| Patient: Appointment Ticket | `GET /api/booking/phieu-kham/{ma}`, `POST .../huy`, `POST .../doi-lich` | Mục 2 "Appointment Ticket"; bảng "Bệnh nhân hủy / đổi lịch" |
| (ngoài UI) lịch hẹn của tôi, kết quả khám, đánh giá | `GET /api/booking/lich-hen/cua-toi...`, `GET /api/booking/lich-su-kham/cua-toi`, `POST / PUT .../danh-gia` | Mục 2 "Lịch hẹn của tôi"; mục "Đánh giá lượt khám" |
| Doctor: Dashboard | `GET /api/booking/bac-si/toi/tong-quan`, `GET /api/booking/bac-si/toi/lich-hen?ngay=`, `GET /api/booking/bac-si/toi/danh-gia` | Mục "Dashboard"; mục "Đánh giá lượt khám" |
| Doctor: Work Schedule | `GET /api/booking/bac-si/toi/lich-lam-viec`, `GET .../lich-hen/lich`, `POST /api/booking/bac-si/toi/yeu-cau-doi-lich` | Mục 3; bảng "Ca làm việc và yêu cầu đổi ca / xin nghỉ" |
| Doctor: Appointment Requests | `GET /api/booking/bac-si/toi/lich-hen/yeu-cau`, `POST .../{id}/xac-nhan`, `POST .../{id}/tu-choi` | Mục 3 |
| Doctor: Consultation / EMR | `GET .../lich-hen/tra-cuu`, `GET .../{id}/ho-so-kham`, `POST / PUT /api/medical/bac-si/toi/lich-hen/{id}/benh-an`, `GET /api/medical/bac-si/toi/thuoc` | Mục 3 |
| Doctor: Doctor Profile, menu tài khoản | `GET /api/users/me`, `GET /api/catalog/bac-si/{id}`, `/api/users/me/...` | Mục 3; README "Tài khoản của tôi" |
| Chuông thông báo (Doctor, Patient, Admin) | `GET /api/notification/thong-bao`, `GET .../so-chua-doc`, `POST .../{id}/da-doc` | README "Thông báo trong ứng dụng" |
| Admin: Dashboard | `GET /api/booking/quan-tri/tong-quan` | Mục "Dashboard" |
| Admin: Schedule Management | `GET / POST / PUT /api/booking/quan-tri/lich-lam-viec...`, `/api/booking/quan-tri/yeu-cau-doi-lich...` | Mục 4; bảng "Ca làm việc và yêu cầu đổi ca / xin nghỉ" |
| Admin: Medical Catalog | `/api/catalog/chuyen-khoa`, `/api/catalog/quan-tri/phong-kham...`, `/api/medical/quan-tri/thuoc...` | Mục "Patient Management và Medical Catalog" |
| Admin: Doctor Management | `/api/catalog/quan-tri/bac-si...`, `GET /api/booking/quan-tri/danh-gia?idBacSi=` | Mục "Doctor Management"; mục 4 (hồ sơ, ảnh giới thiệu) |
| Admin: Patient Management | `/api/booking/quan-tri/ho-so-benh-nhan...` | Mục "Patient Management và Medical Catalog"; mục 4 |
| (ngoài UI) quản trị tài khoản | `/api/users...` | README "Tài khoản của tôi và quản trị tài khoản" |

Mọi màn hình trong danh sách UI đều đã có API; riêng ô "System Uptime" của Admin Dashboard không được cung cấp.

**Vòng đời lịch hẹn:** đặt lịch tạo `CHO_XAC_NHAN` (phiếu khám dùng được ngay). Bác sĩ của lịch hẹn xác nhận (`DA_XAC_NHAN`) hoặc
từ chối kèm lý do (`BI_TU_CHOI`) trước giờ khám; lịch chưa kịp xác nhận vẫn khám được vào ngày khám. Ghi kết quả khám chuyển
lịch sang `DA_HOAN_THANH`. Phiếu khám (`GET /api/booking/phieu-kham/{ma}`), dòng "lịch hẹn của tôi" và dòng lịch hẹn của bác sĩ đều có
`lyDoHuy`: lý do từ chối hoặc lý do hủy, null ở các trạng thái khác — hiện cho bệnh nhân cạnh trạng thái.

**Bệnh nhân hủy / đổi lịch** (danh sách UI chưa có màn hình cho việc này; trang mẫu đặt nút ở chi tiết lịch hẹn và ở trang phiếu khám):

| Việc | API | Ghi chú |
|---|---|---|
| Hủy (đã đăng nhập) | `POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/huy` `{ lyDo? }` | 200, trả phiếu khám với `trangThai: DA_HUY`. Chỉ tài khoản đã đặt hoặc người khám; chỉ là người giám hộ theo CCCD → 403; lịch không thấy được → 404 |
| Đổi lịch (đã đăng nhập) | `POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/doi-lich` `{ idLichLamViec | idChuyenKhoa, gioBatDauKhung }` | 201, trả như đặt lịch: lịch **mới** `CHO_XAC_NHAN` với `maPhieuKham` mới (bác sĩ phải xác nhận lại). Lịch cũ thành `DA_HUY_DO_DOI_LICH`. Không gửi lại thông tin người khám. Phải khác khung giờ đang giữ (→ 400) |
| Hủy / đổi (khách, không đăng nhập) | `POST /api/booking/phieu-kham/{maPhieuKham}/huy` `{ soDienThoai, lyDo? }`, `POST .../phieu-kham/{maPhieuKham}/doi-lich` `{ soDienThoai, ... }` | `soDienThoai` = đủ 10 số SĐT liên hệ đã nhập lúc đặt (phiếu chỉ hiện bản đã che). Sai → 403 `SO_DIEN_THOAI_KHONG_KHOP`; giới hạn số lần gọi theo IP |
| Ẩn / hiện nút | `duocHuyDoi`, `hanHuyDoi` trên phiếu khám và dòng "lịch hẹn của tôi" | `duocHuyDoi` = lịch `CHO_XAC_NHAN` / `DA_XAC_NHAN`, chưa quá `hanHuyDoi` (giờ khám trừ 2 giờ) và, ở dòng "của tôi", tài khoản là người đặt / người khám. Quá hạn → 409 `QUA_HAN_HUY_DOI_LICH` (hiện "vui lòng liên hệ phòng khám") |
| Giới hạn | | Đổi tối đa 2 lần cho 1 lần đặt (→ 409 `VUOT_SO_LAN_DOI_LICH`), hủy thì không giới hạn; lịch đã khám / đã hủy / bị từ chối → 409 `LICH_HEN_KHONG_HUY_DOI_DUOC`; khung mới hết chỗ → 409 `KHUNG_GIO_KHONG_CON_TRONG` và lịch cũ giữ nguyên |

**Ca làm việc và yêu cầu đổi ca / xin nghỉ (Schedule Management của Admin, Work Schedule của Doctor).**

| API | Ai gọi | Ghi chú |
|---|---|---|
| `POST /api/booking/quan-tri/lich-lam-viec` | `QUAN_TRI_VIEN` | Xếp ca: `{ idBacSi, idPhongKham, ngay, gioBatDau, gioKetThuc, soLuotToiDaMoiGio (N), thoiLuongLuotPhut (t), lapLai?: { cacThu: [1..7], denNgay } }` (`cacThu`: 1 = thứ Hai ... 7 = Chủ nhật). 201 `{ daTao: [ca], boQua: [{ ngay, lyDo }] }`. Các lượt khám được sinh theo N, t (`N x t <= 60`). Phòng phải đang hoạt động và thuộc chuyên khoa của bác sĩ; ca phải bắt đầu sau hiện tại và trong `SHIFT_MAX_DAYS` (90) ngày tới. Trùng giờ với ca khác của bác sĩ hoặc của phòng → 409 `TRUNG_LICH_LAM_VIEC`; có `lapLai` thì ngày trùng bị bỏ qua và nằm trong `boQua` |
| `PUT /api/booking/quan-tri/lich-lam-viec/{id}` | `QUAN_TRI_VIEN` | Sửa `{ idPhongKham, gioBatDau, gioKetThuc, soLuotToiDaMoiGio, thoiLuongLuotPhut }` (gửi đủ 5 giá trị; bác sĩ và ngày không sửa được). Phòng đổi được bất cứ lúc nào: lịch hẹn còn hiệu lực mang phòng mới, bệnh nhân được báo. Giờ / sức chứa chỉ đổi được khi mọi lượt đã có người đặt còn nguyên giờ; không thì 409 `CA_CON_LICH_HEN` và không có gì thay đổi. Ca đã bắt đầu hoặc đã hủy → 409 `CA_KHONG_SUA_DUOC` |
| `POST /api/booking/quan-tri/lich-lam-viec/{id}/huy` | `QUAN_TRI_VIEN` | `{ lyDo }` bắt buộc. Trả `{ ca, soLichHenCanDoi }`. Ca và mọi lượt khám thành `DA_HUY`; lịch hẹn còn hiệu lực của ca **giữ nguyên trạng thái** và có `canDoiLich = true`; bệnh nhân và bác sĩ được báo. Yêu cầu đang chờ duyệt của ca bị đóng (`TU_CHOI` kèm ghi chú) |
| `GET /api/booking/quan-tri/lich-lam-viec/can-doi-lich?trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Lịch hẹn còn hiệu lực đang chờ bệnh nhân đổi lịch, giờ khám cũ sớm nhất trước (để phòng khám gọi điện cho khách đặt không đăng nhập) |
| `POST /api/booking/bac-si/toi/yeu-cau-doi-lich` | `BAC_SI` | `{ idLichLamViec, loaiYeuCau: XIN_NGHI \| DOI_CA, lyDo, ngayMongMuon?, gioBatDauMongMuon?, gioKetThucMongMuon?, idPhongKhamMongMuon? }` → 201. 3 trường ngày / giờ bắt buộc với `DOI_CA`. Chỉ ca còn hoạt động của chính bác sĩ (ca của người khác → 404), còn cách giờ bắt đầu ít nhất `SHIFT_REQUEST_LEAD` (24 giờ) → không thì 409 `QUA_HAN_GUI_YEU_CAU`. Mỗi ca 1 yêu cầu chờ duyệt → 409 `CA_DA_CO_YEU_CAU_CHO_DUYET` |
| `GET /api/booking/bac-si/toi/yeu-cau-doi-lich?trangThai=&trang=&kichThuoc=`, `POST .../yeu-cau-doi-lich/{id}/rut` | `BAC_SI` | Yêu cầu tôi đã gửi (mới nhất trước) và rút yêu cầu còn chờ duyệt (`DA_RUT`; sau đó ca nhận được yêu cầu mới). Mỗi dòng: `{ id, loaiYeuCau, trangThai, lyDo, ca, ngayMongMuon, gioBatDauMongMuon, gioKetThucMongMuon, phongKhamMongMuon, ghiChuXuLy, ngayGui, ngayXuLy }`; `ca` như 1 dòng lịch làm việc (có `soLuotDaDat`) |
| `GET /api/booking/quan-tri/yeu-cau-doi-lich?trangThai=&tatCa=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Yêu cầu của mọi bác sĩ, gửi sớm nhất trước; mặc định `CHO_DUYET`, `tatCa=true` lấy mọi trạng thái |
| `POST /api/booking/quan-tri/yeu-cau-doi-lich/{id}/duyet`, `POST .../{id}/tu-choi` | `QUAN_TRI_VIEN` | Body `{ ghiChu }`, bắt buộc khi từ chối. Duyệt `XIN_NGHI` = hủy ca. Duyệt `DOI_CA`: chỉ khác phòng thì đổi phòng tại chỗ; còn lại thì hủy ca cũ và xếp ca mới theo ngày / giờ / phòng mong muốn (giữ N, t). Ca mới trùng giờ → 409 `TRUNG_LICH_LAM_VIEC`, yêu cầu vẫn chờ duyệt. Đã xử lý / đã rút → 409 `YEU_CAU_DA_XU_LY` |
| `GET /api/catalog/phong-kham?idChuyenKhoa=` | `QUAN_TRI_VIEN`, `BAC_SI` | Phòng khám đang hoạt động: `{ id, tenPhong, tang, idChuyenKhoa, tenChuyenKhoa }`. Chỉ đọc |

**Lịch hẹn của ca bị hủy (`canDoiLich`).** Phiếu khám, dòng "lịch hẹn của tôi" và dòng lịch hẹn của bác sĩ / quản trị viên có
`canDoiLich`. Khi `true`: lịch hẹn vẫn `CHO_XAC_NHAN` / `DA_XAC_NHAN` nhưng ca đã hủy, bệnh nhân phải đổi sang khung giờ khác
hoặc hủy. Với lịch này `duocHuyDoi` đúng tới giờ khám cũ (`hanHuyDoi` = giờ khám, không trừ 2 giờ) và lần đổi không tính vào
giới hạn 2 lần; lịch mới sau khi đổi không còn cờ. Bác sĩ không thấy lịch này trong Appointment Requests và không xác nhận /
từ chối được (409 `LICH_HEN_CAN_DOI_LICH`).

**Số thứ tự khám** là thứ hạng của lượt khám trong các lượt của phòng trong ngày, nên khi phòng có thêm ca đứng trước, ca được
kéo dài hoặc ca đổi phòng thì số thứ tự của các lịch hẹn còn hiệu lực của phòng trong ngày đó được tính lại. Luôn hiển thị
`soThuTu` mới nhất từ API, không lưu lại ở frontend.

**Chuông thông báo (mọi vai trò đã đăng nhập).** UI mới vẽ chuông ở khu bác sĩ; bệnh nhân cũng có thông báo (bác sĩ xác nhận /
từ chối), nên trang mẫu hiện chuông cho mọi tài khoản.

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/notification/thong-bao?chuaDoc=&trang=&kichThuoc=` | Mọi tài khoản đã đăng nhập | Thông báo của tôi, mới nhất trước, dạng `TrangDuLieu`. `chuaDoc=true` chỉ lấy thông báo chưa đọc; `kichThuoc` mặc định 20, tối đa 100. Mỗi dòng: `{ id, loai, noiDung, daDoc, ngayTao, idLichHen, maPhieuKham, idYeuCau }`. `noiDung` là câu tiếng Việt đã soạn sẵn, hiển thị nguyên văn. `maPhieuKham` chỉ có khi người đọc là bệnh nhân (dùng để mở chi tiết lịch hẹn); bác sĩ dùng `idLichHen` |
| `GET /api/notification/thong-bao/so-chua-doc` | Mọi tài khoản đã đăng nhập | `{ soChuaDoc }`: số trên biểu tượng chuông |
| `POST /api/notification/thong-bao/{id}/da-doc` | Mọi tài khoản đã đăng nhập | Đánh dấu 1 thông báo của tôi đã đọc, gọi lại nhiều lần vẫn được. Thông báo của tài khoản khác hoặc không có -> 404 `KHONG_TIM_THAY_THONG_BAO` |
| `POST /api/notification/thong-bao/da-doc-tat-ca` | Mọi tài khoản đã đăng nhập | `{ soDaDanhDau }` |

| Sự kiện | `loai` | Người nhận |
|---|---|---|
| Có lịch hẹn mới (kể cả khách đặt) | `LICH_HEN_MOI` | Bác sĩ của lịch hẹn |
| Bác sĩ xác nhận | `LICH_HEN_DA_XAC_NHAN` | Tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân (mỗi tài khoản 1 thông báo) |
| Bác sĩ từ chối (câu thông báo có lý do) | `LICH_HEN_BI_TU_CHOI` | Như trên |
| Bệnh nhân hủy | `LICH_HEN_DA_HUY` | Bác sĩ của lịch hẹn |
| Bệnh nhân đổi lịch, cùng bác sĩ | `LICH_HEN_DA_DOI` (gắn với lịch mới) | Bác sĩ đó, 1 thông báo |
| Bệnh nhân đổi lịch sang bác sĩ khác | `LICH_HEN_DA_HUY` cho bác sĩ cũ, `LICH_HEN_MOI` cho bác sĩ mới | Cả 2 bác sĩ |
| Ca khám bị hủy (quản trị viên hủy, hoặc duyệt xin nghỉ / đổi ca) | `LICH_HEN_CAN_DOI` | Tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân, cho từng lịch hẹn còn hiệu lực của ca |
| Ca khám đổi phòng | `LICH_HEN_DOI_PHONG` | Như trên |
| Quản trị viên xếp ca mới (1 thông báo cho 1 lần xếp), sửa hoặc hủy ca | `CA_LAM_VIEC_THAY_DOI` | Bác sĩ của ca |
| Bác sĩ gửi yêu cầu đổi ca / xin nghỉ | `YEU_CAU_DOI_LICH_MOI` (có `idYeuCau`) | Mọi quản trị viên đang hoạt động |
| Quản trị viên duyệt / từ chối yêu cầu | `KET_QUA_DUYET_DOI_LICH` (có `idYeuCau`, câu thông báo có ghi chú) | Bác sĩ đã gửi |

- Không có đẩy realtime: gọi `so-chua-doc` khi vẽ trang và lặp lại khoảng 30 giây 1 lần; dừng khi tab bị ẩn
  (`document.hidden`), gọi ngay khi tab hiện lại. Thông báo xuất hiện chậm nhất khoảng 5 giây sau sự kiện, cộng chu kỳ hỏi.
- Bấm 1 thông báo: gọi `da-doc` rồi chuyển màn hình. Trang mẫu: bác sĩ sang Appointment Requests (`LICH_HEN_MOI`,
  `LICH_HEN_DA_DOI`) hoặc Work Schedule; bệnh nhân sang chi tiết lịch hẹn theo `maPhieuKham`.
- Người làm thao tác không nhận thông báo về thao tác của chính mình; khách đặt không đăng nhập không có thông báo. Chưa có
  nhắc lịch khám, chưa có xoá thông báo.

`/doctor/appointment-requests` là đường dẫn backend tự chọn (ảnh chụp màn hình Appointment Requests không có thanh địa chỉ):
frontend dùng đường dẫn khác thì báo lại để trang mẫu đổi theo.

### Doctor Management (quản trị viên) và lần đăng nhập đầu của bác sĩ mới

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/catalog/quan-tri/bac-si?tuKhoa=&idChuyenKhoa=&trangThai=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Danh bạ bác sĩ, bác sĩ mới thêm đứng trước, **cả bác sĩ ngừng công tác**. `tuKhoa` so với họ tên, email, số điện thoại, số giấy phép. Mỗi dòng: `id`, `maBacSi`, `hoTen`, `email`, `soDienThoai`, `anhDaiDien`, `hocVi`, `soGiayPhep`, `idChuyenKhoa`, `tenChuyenKhoa`, `soLuotDaKham`, `trangThai` (`DANG_CONG_TAC` / `NGUNG_CONG_TAC`), `trangThaiTaiKhoan`, `phaiDoiMatKhau` |
| `POST /api/catalog/quan-tri/bac-si` | `QUAN_TRI_VIEN` | "Add New Doctor": `{ hoTen, email, soDienThoai?, idChuyenKhoa, soGiayPhep?, hocVi?, chucVu?, soNamKinhNghiem? }` → 201, trả hồ sơ quản trị của bác sĩ. Tạo tài khoản đăng nhập và hồ sơ bác sĩ trong 1 bước |
| `PUT /api/catalog/quan-tri/bac-si/{id}` | `QUAN_TRI_VIEN` | Thông tin cơ bản, ghi đè cả 4 trường: `{ hoTen, soDienThoai?, idChuyenKhoa, soGiayPhep? }`. Email không sửa ở đây. Hồ sơ giới thiệu vẫn sửa bằng `PUT .../{id}/ho-so` |
| `GET /api/catalog/quan-tri/bac-si/{id}/anh-huong-ngung-cong-tac` | `QUAN_TRI_VIEN` | `{ soCaSapToi, soLichHenBiAnhHuong }`: hiện cho quản trị viên trước khi xác nhận |
| `POST /api/catalog/quan-tri/bac-si/{id}/ngung-cong-tac` | `QUAN_TRI_VIEN` | `{ lyDo }` (bắt buộc) → `{ bacSi, soCaDaHuy, soLichHenCanDoi }` |
| `POST /api/catalog/quan-tri/bac-si/{id}/cong-tac-lai` | `QUAN_TRI_VIEN` | Bác sĩ về `DANG_CONG_TAC`, tài khoản về `DA_KICH_HOAT`. Các ca đã hủy không được khôi phục |
| `POST /api/auth/first-password` | Công khai | `{ email, matKhauHienTai, matKhauMoi }`: đặt mật khẩu ở lần đăng nhập đầu của tài khoản bác sĩ mới |

- **Mã bác sĩ** (`maBacSi`, ví dụ `BS0007`) suy ra từ `id`, chỉ để hiển thị; mọi API vẫn dùng `id`.
- **Thêm bác sĩ.** Tài khoản được tạo ở trạng thái đã kích hoạt, mật khẩu là mật khẩu mặc định của phòng khám
  (`DOCTOR_DEFAULT_PASSWORD`, mặc định `Doctor@123`), `phaiDoiMatKhau = true`. Bác sĩ nhận email báo email đăng nhập
  (email không chứa mật khẩu). Email / số điện thoại đã có: 409 `EMAIL_DA_TON_TAI` / `SO_DIEN_THOAI_DA_TON_TAI`; số giấy
  phép đã có: 409 `SO_GIAY_PHEP_DA_TON_TAI`.
- **Lần đăng nhập đầu.** `POST /api/auth/login` với mật khẩu mặc định trả **403 `PHAI_DOI_MAT_KHAU`** và không cấp phiên.
  Frontend hiện form "đặt mật khẩu mới", gọi `POST /api/auth/first-password` với email + mật khẩu vừa nhập + mật khẩu mới
  (tối thiểu 6 ký tự, khác mật khẩu hiện tại), rồi gọi lại `login` bằng mật khẩu mới. Sai mật khẩu hiện tại: 401, đếm chung
  với đăng nhập sai (5 lần thì khoá 15 phút). Tài khoản không cần đặt mật khẩu lần đầu: 409. "Quên mật khẩu" qua email cũng
  gỡ được trạng thái này.
- **Ngừng công tác** chạy 3 bước: (1) bác sĩ thành `NGUNG_CONG_TAC`: ẩn khỏi danh sách công khai, không nhận ca và lịch hẹn
  mới; (2) mọi ca chưa bắt đầu bị hủy như khi quản trị viên hủy từng ca: lịch hẹn được giữ, đánh dấu `canDoiLich`, bệnh
  nhân nhận thông báo `LICH_HEN_CAN_DOI`, lịch hẹn vào danh sách `GET /api/booking/quan-tri/lich-lam-viec/can-doi-lich`;
  (3) tài khoản bị vô hiệu hoá, mọi phiên bị đăng xuất, bác sĩ nhận email kèm lý do. Ca đang diễn ra được giữ nguyên.
  Nếu trả **503 `DICH_VU_NOI_BO_LOI`** thì bác sĩ đã ở `NGUNG_CONG_TAC` nhưng chưa xong: gửi lại đúng request đó để chạy
  nốt (gọi lại nhiều lần không sao).
- **Đổi chuyên khoa** khi bác sĩ còn ca sắp tới: 409 `BAC_SI_CON_CA_LAM_VIEC` (phòng khám của ca thuộc chuyên khoa cũ); hủy
  các ca đó trước.
- `PUT /api/users/{id}/status` (quản lý tài khoản) không đổi: vẫn từ chối vô hiệu hoá tài khoản bác sĩ còn lịch hẹn sắp
  tới và không đụng tới trạng thái bác sĩ. Với bác sĩ hãy dùng 2 API ngừng công tác / công tác lại ở trên.

### Patient Management và Medical Catalog (quản trị viên)

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/booking/quan-tri/ho-so-benh-nhan?tuKhoa=&trangThaiLienKet=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Patient Management: danh sách hồ sơ, mới tạo trước, **cả hồ sơ của khách không có tài khoản**. Mỗi dòng: `id`, `hoTen`, `ngaySinh`, `gioiTinh`, `soDienThoai`, `cccdChe` (chỉ còn 4 số cuối), `coTaiKhoan`, `trangThaiLienKet`, `soLichHen`, `ngayTao` |
| `GET /api/booking/quan-tri/ho-so-benh-nhan/{id}/lich-hen?trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Lịch hẹn của 1 hồ sơ, mọi trạng thái, giờ khám muộn nhất trước. Cùng cấu trúc dòng với lịch hẹn của 1 ca (có `bacSi`). Không có chẩn đoán / đơn thuốc |
| `GET /api/catalog/quan-tri/phong-kham?tuKhoa=&idChuyenKhoa=&trangThai=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Medical Catalog: mọi phòng khám theo tên, **cả phòng ngừng hoạt động**: `id`, `tenPhong`, `tang`, `idChuyenKhoa`, `tenChuyenKhoa`, `trangThai` (`HOAT_DONG` / `NGUNG_HOAT_DONG`), `soCaSapToi` |
| `POST /api/catalog/quan-tri/phong-kham`, `PUT .../{id}` | `QUAN_TRI_VIEN` | `{ tenPhong, tang?, idChuyenKhoa }`; thêm → 201. Trùng tên: 409 `TEN_PHONG_DA_TON_TAI` |
| `POST /api/catalog/quan-tri/phong-kham/{id}/ngung-hoat-dong`, `POST .../{id}/hoat-dong-lai` | `QUAN_TRI_VIEN` | Phòng ngừng hoạt động không còn trong `GET /api/catalog/phong-kham` và không xếp ca mới vào được |
| `GET /api/medical/quan-tri/thuoc?tuKhoa=&daXacMinh=&trangThai=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Medical Catalog: danh mục thuốc, chưa xác minh trước rồi theo tên: `id`, `tenThuoc`, `donVi`, `moTa`, `daXacMinh`, `trangThai` (`DANG_DUNG` / `NGUNG_DUNG`), `tenBacSiTao`, `soLanKe` |
| `POST /api/medical/quan-tri/thuoc`, `PUT .../{id}` | `QUAN_TRI_VIEN` | `{ tenThuoc, donVi?, moTa? }`; thêm → 201, thuốc đã xác minh. Trùng tên (không phân biệt hoa thường / khoảng trắng): 409 `TEN_THUOC_DA_TON_TAI` |
| `POST /api/medical/quan-tri/thuoc/{id}/xac-minh`, `POST .../{id}/ngung-dung`, `POST .../{id}/dung-lai` | `QUAN_TRI_VIEN` | Xác minh thuốc bác sĩ tự thêm khi kê đơn; cho ngừng dùng / dùng lại. Gọi lại nhiều lần không sao |

- **Tìm hồ sơ bệnh nhân.** `tuKhoa` so với họ tên, số điện thoại, số bảo hiểm y tế (chứa từ khoá). Số CCCD chỉ tra được khi
  gõ **đủ 12 số** (khớp hoàn toàn); gõ 1 phần số CCCD không ra kết quả. Danh sách chỉ trả 4 số cuối của CCCD, số đầy đủ có
  ở `GET /api/booking/quan-tri/ho-so-benh-nhan/{id}`.
- **Phòng khám không xoá được**, chỉ ngừng hoạt động. Phòng còn ca sắp tới (`soCaSapToi > 0`) thì ngừng hoạt động và đổi
  chuyên khoa đều trả 409 `PHONG_CON_CA_LAM_VIEC` (thông điệp có số ca): chuyển các ca đó sang phòng khác bằng
  `PUT /api/booking/quan-tri/lich-lam-viec/{id}` trước. Tên phòng và tầng sửa được bất cứ lúc nào.
- **Thuốc không xoá và không gộp được.** Thuốc đã có trong đơn thuốc (`soLanKe > 0`) chỉ sửa được cách viết hoa / khoảng
  trắng của tên; đổi sang tên khác trả 409 `THUOC_DA_DUOC_KE` (dòng đơn thuốc chỉ lưu id thuốc, đổi tên là sửa luôn đơn
  cũ). Đơn vị và mô tả sửa được bất cứ lúc nào.
- **Thuốc ngừng dùng** không còn trong gợi ý `GET /api/medical/bac-si/toi/thuoc`, và bác sĩ kê thuốc đó ở đơn mới nhận 409
  `THUOC_NGUNG_DUNG` (thông điệp có tên thuốc). Hồ sơ bệnh án đã có thuốc đó vẫn sửa / lưu lại được với dòng thuốc cũ, và
  bệnh nhân vẫn thấy thuốc trong kết quả khám cũ.

### Đánh giá lượt khám (bệnh nhân, bác sĩ, quản trị viên)

Danh sách UI chưa có form đánh giá: trang demo đặt form ở chi tiết lượt khám đã hoàn thành (`/patient/appointments`),
bảng đánh giá ẩn danh ở Dashboard của bác sĩ và bảng có tên bệnh nhân ở Doctor Management.

| API | Ai gọi | Ghi chú |
|---|---|---|
| `POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/danh-gia` | `BENH_NHAN` | `{ soSao: 1..5, nhanXet? (tối đa 1000 ký tự) }` → 201 `{ soSao, nhanXet, ngayTao, duocSuaDen }`. Mỗi lịch hẹn 1 đánh giá |
| `PUT /api/booking/lich-hen/cua-toi/{maPhieuKham}/danh-gia` | `BENH_NHAN` | Cùng thân, thay số sao và nhận xét, trong 7 ngày kể từ lúc gửi (`duocSuaDen`) |
| `GET /api/booking/lich-hen/cua-toi/{maPhieuKham}` (có từ trước) | `BENH_NHAN` | Thêm `duocDanhGia` (true: hiện form gửi đánh giá) và `danhGia` (đánh giá đã gửi, hoặc null) |
| `GET /api/booking/bac-si/toi/danh-gia/tong-quan` | `BAC_SI` | `{ diemTrungBinh, soDanhGia, phanBo: { "1": n, ..., "5": n } }`; `diemTrungBinh` null khi chưa có đánh giá. Ô "Average Rating" của Dashboard |
| `GET /api/booking/bac-si/toi/danh-gia?trang=&kichThuoc=` | `BAC_SI` | Đánh giá tôi nhận được, mới nhất trước: `soSao`, `nhanXet`, `ngayKham`, `ngayTao`. **Ẩn danh**: không có tên bệnh nhân, không có mã lịch hẹn |
| `GET /api/booking/quan-tri/danh-gia?idBacSi=&soSaoToiDa=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Mới nhất trước: `id`, `soSao`, `nhanXet`, `ngayTao`, `ngayCapNhat`, `maTraCuu`, `ngayKham`, `idHoSoBenhNhan`, `tenBenhNhan`, `idBacSi`, `tenBacSi`. `soSaoToiDa=2`: chỉ đánh giá 1-2 sao |
| `GET /api/catalog/bac-si`, `GET /api/catalog/bac-si/{id}` (có từ trước) | Công khai | Thêm `diemDanhGia` (1 chữ số thập phân, null khi chưa có) và `soDanhGia`; luôn hiển thị điểm kèm số lượt, vd `★ 4.5 (12)` |

- **Ai được đánh giá:** tài khoản bệnh nhân được xem kết quả của lượt khám đó (chủ tài khoản là người khám, hoặc tài khoản đã
  đặt lịch), và chỉ khi lịch hẹn `DA_HOAN_THANH`. Khách không có tài khoản không đánh giá được. Tài khoản chỉ thấy lịch
  vì là người giám hộ theo CCCD: 403.
- **Mã lỗi:** 409 `LICH_HEN_CHUA_KHAM_XONG` (lịch chưa khám xong / đã hủy), 409 `DA_DANH_GIA` (gửi lần 2, dùng `PUT` để
  sửa), 409 `HET_HAN_SUA_DANH_GIA` (quá 7 ngày), 404 (không thấy lịch hẹn, hoặc `PUT` khi chưa có đánh giá), 400 (số sao
  ngoài 1..5, nhận xét quá dài). Không có xoá đánh giá.
- **Nhận xét không công khai:** chỉ bác sĩ (ẩn danh) và quản trị viên (có tên bệnh nhân) đọc. Trang công khai chỉ có điểm
  trung bình và số lượt, hiện ngay từ đánh giá đầu tiên. Điểm = trung bình cộng mọi đánh giá của bác sĩ, tính khi đọc.
- Thời hạn sửa đổi bằng `RATING_EDIT_DAYS` (mặc định 7) của booking-service.

### Dashboard (bác sĩ, quản trị viên)

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/booking/bac-si/toi/tong-quan` | `BAC_SI` | Dashboard của bác sĩ, 1 lần gọi cho cả 4 ô: `{ lichHenHomNay, dangCho, daKham, diemDanhGia, soDanhGia, yeuCauChoXacNhan, tinhLuc }` |
| `GET /api/booking/quan-tri/tong-quan` | `QUAN_TRI_VIEN` | Dashboard của quản trị viên, 1 lần gọi cho cả 3 ô: `{ bacSiDangCongTac, benhNhanDaDangKy, luotDatTrongThang, tinhLuc }` |

Định nghĩa từng số (frontend không tự đếm lại từ các danh sách: danh sách có phân trang và bộ lọc riêng):

| Ô trên UI | Trường | Cách tính |
|---|---|---|
| Today's Appointments | `lichHenHomNay` | Lịch hẹn của bác sĩ có giờ khám hôm nay, **không tính** `DA_HUY`, `DA_HUY_DO_DOI_LICH`, `BI_TU_CHOI` |
| Waiting | `dangCho` | Trong số đó: `CHO_XAC_NHAN` hoặc `DA_XAC_NHAN` và không có `canDoiLich` (chưa khám) |
| Completed | `daKham` | Trong số đó: `DA_HOAN_THANH` |
| Average Rating | `diemDanhGia`, `soDanhGia` | Trung bình mọi đánh giá của bác sĩ, 1 chữ số thập phân; `null` và `0` khi chưa có. Hiển thị kèm số lượt |
| (nhãn Appointment Requests) | `yeuCauChoXacNhan` | Số lịch `CHO_XAC_NHAN` từ bây giờ trở đi của bác sĩ = `tongSoPhanTu` của danh sách yêu cầu |
| Active Doctors | `bacSiDangCongTac` | Bác sĩ `DANG_CONG_TAC` có tài khoản `DA_KICH_HOAT`: đúng tập bác sĩ của danh sách công khai |
| Registered Patients | `benhNhanDaDangKy` | Tài khoản vai trò `BENH_NHAN` đã kích hoạt. Khách chỉ đặt lịch (không có tài khoản) không tính |
| Bookings This Month | `luotDatTrongThang` | Lịch hẹn **được tạo** từ 00:00 ngày 1 tháng này, mọi trạng thái trừ `DA_HUY_DO_DOI_LICH` (1 lần đổi lịch chỉ tính 1) |

- `tinhLuc` là thời điểm backend tính các số; số được đếm trực tiếp mỗi lần gọi, không lưu sẵn.
- "System Uptime" trên Dashboard của quản trị viên **không có API** (đã thống nhất bỏ): frontend ẩn ô này.

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
| Thẻ bác sĩ | `GET /api/catalog/bac-si?idChuyenKhoa=` | `id`, `hoTen`, `anhDaiDien`, `hocVi`, `chucVu`, `soNamKinhNghiem`, `gioiThieuNgan`, `diemDanhGia` (null khi chưa có), `soDanhGia` |
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
| Appointment Requests: danh sách | `GET /api/booking/bac-si/toi/lich-hen/yeu-cau?trang=&kichThuoc=` | `{ noiDung[], trang, kichThuoc, tongSoPhanTu, tongSoTrang }`; mỗi dòng như danh sách trong ngày. Chỉ lịch `CHO_XAC_NHAN` của chính bác sĩ mà lượt khám chưa bắt đầu, giờ khám sớm nhất trước. `tongSoPhanTu` = số yêu cầu đang chờ |
| Appointment Requests: Accept | `POST /api/booking/bac-si/toi/lich-hen/{id}/xac-nhan` (không body) | 200, trả dòng lịch hẹn với `trangThai: DA_XAC_NHAN`. Đã xử lý rồi → 409 `LICH_HEN_KHONG_CHO_XAC_NHAN`; lượt khám đã bắt đầu → 409 `LICH_HEN_DA_QUA_GIO`; lịch của bác sĩ khác → 404 |
| Appointment Requests: Decline | `POST /api/booking/bac-si/toi/lich-hen/{id}/tu-choi` `{ lyDo }` | `lyDo` bắt buộc, tối đa 500 ký tự (thiếu → 400). 200, trả dòng lịch hẹn với `trangThai: BI_TU_CHOI`, `lyDoHuy`; lượt khám được mở lại cho người khác đặt. Lỗi như Accept. Lịch đã xác nhận không từ chối được |
| Consultation: nhập mã / quét QR | `GET /api/booking/bac-si/toi/lich-hen/tra-cuu?ma=` | `ma` = `maTraCuu`, `maPhieuKham`, hoặc **cả chuỗi link** máy quét QR trả về (`.../phieu-kham/<mã>`). Lịch của bác sĩ khác → 404 |
| Hồ sơ khám của bệnh nhân | `GET /api/booking/bac-si/toi/lich-hen/{id}/ho-so-kham` (bấm vào 1 dòng / số thứ tự), `GET /api/booking/bac-si/toi/lich-hen/tra-cuu/ho-so-kham?ma=` (gõ mã / quét QR, `ma` như trên) | Cùng 1 kết quả: `lichHen` (như dòng danh sách), `hoSoBenhNhan { id, cccd, hoTen, ngaySinh, tuoi, gioiTinh, soDienThoai, diaChi, soBaoHiemYTe, tienSuBenhLy, daLienKetTaiKhoan }` (CCCD không che), `lanKhamTruoc[] { idLichHen, maTraCuu, ngay, gioKhamDuKien, bacSi, tenChuyenKhoa, trangThai, lyDoKham, ketQua { chanDoan, ghiChu, ngayTaiKhamDeXuat } }` mới nhất trước, gồm cả lần khám với bác sĩ khác; `ketQua` null khi lần đó chưa có bệnh án. Lịch của bác sĩ khác → 404 |
| Đối chiếu giấy tờ | `PUT /api/booking/bac-si/toi/lich-hen/{id}/benh-nhan` `{ hoTen, ngaySinh, gioiTinh?, soDienThoai, diaChi?, soBaoHiemYTe?, cccd? }`, `POST .../lich-hen/{id}/da-doi-chieu` | Khi `lichHen.doiChieu.canDoiChieu`: bác sĩ sửa hồ sơ cho đúng giấy tờ (trả lại hồ sơ khám mới; `cccd` chỉ điền được khi hồ sơ chưa có số) rồi bấm "đã đối chiếu". Hai thao tác độc lập |
| Doctor Profile (chỉ xem) | `GET /api/users/me` | Hồ sơ giới thiệu công khai: `GET /api/catalog/bac-si/{id}` |
| Consultation: ghi kết quả khám | `POST /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an` `{ chanDoan, ghiChu?, ngayTaiKhamDeXuat?, donThuoc?: [{ tenThuoc, donVi?, lieuDung?, soLanMoiNgay?, soNgayDung?, ghiChuSuDung? }] }` | 201; lịch hẹn chuyển sang `DA_HOAN_THANH` cùng lúc. Chỉ với lịch hẹn của chính bác sĩ (khác → 404), còn `CHO_XAC_NHAN` / `DA_XAC_NHAN` (khác → 409 `LICH_HEN_KHONG_KHAM_DUOC`), và từ ngày khám trở đi (sớm hơn → 409 `CHUA_DEN_NGAY_KHAM`). `chanDoan` bắt buộc; `ngayTaiKhamDeXuat` phải sau hôm nay; đơn thuốc tối đa 30 dòng. Tên thuốc chưa có trong danh mục được tự thêm (chưa xác minh) |
| Consultation: xem / sửa kết quả đã ghi | `GET` / `PUT /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an` | `PUT` cùng body như `POST`, thay toàn bộ nội dung và đơn thuốc; lịch hẹn giữ `DA_HOAN_THANH`. Chưa có kết quả → 404. Kết quả cũng hiện ở hồ sơ khám (`ketQua`, và `lanKhamTruoc[].ketQua` kèm `donThuoc[]`) |
| Consultation: gợi ý tên thuốc | `GET /api/medical/bac-si/toi/thuoc?tuKhoa=` | Tối đa 20 dòng `{ id, tenThuoc, donVi, daXacMinh }`; từ khoá trống → `[]`. Thuốc quản trị viên đã cho ngừng dùng không được gợi ý; kê thuốc đó ở đơn mới: 409 `THUOC_NGUNG_DUNG` |
| Dashboard: 4 ô số liệu | `GET /api/booking/bac-si/toi/tong-quan` | Mục "Dashboard" ở đầu tài liệu. Lịch mới luôn ở `CHO_XAC_NHAN` cho tới khi bác sĩ xác nhận hoặc ghi kết quả khám |

## 4. Quản trị viên (vai trò `QUAN_TRI_VIEN`)

| Màn hình | API | Ghi chú |
|---|---|---|
| Schedule Management: xem (xếp / sửa / hủy ca và duyệt yêu cầu: bảng ở đầu tài liệu) | `GET /api/booking/quan-tri/lich-lam-viec?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=&idPhongKham=` | Tối đa 42 ngày, cùng dạng dòng ca như của bác sĩ, kèm `bacSi`, `tenChuyenKhoa` |
| Bệnh nhân của 1 ca | `GET /api/booking/quan-tri/lich-lam-viec/{id}/lich-hen` | |
| Tra lịch hẹn theo mã | `GET /api/booking/quan-tri/lich-hen/tra-cuu?ma=` | Mỗi lịch hẹn (cả ở danh sách của bác sĩ và của 1 ca) có `idHoSoBenhNhan`, `benhNhan` (dữ liệu của hồ sơ) và `doiChieu` `{ canDoiChieu, hoTenDaNhap, ngaySinhDaNhap, gioiTinhDaNhap, hoTenGiamHoDaNhap }` (những gì người đặt nhập). `canDoiChieu: true` → hiện cảnh báo "thông tin nhập khác hồ sơ, cần đối chiếu giấy tờ" |
| Đã đối chiếu giấy tờ | `POST /api/booking/quan-tri/lich-hen/{id}/da-doi-chieu` | Bỏ `canDoiChieu` của lịch hẹn; gọi lại vẫn 200 |
| Patient Management: xem / sửa hồ sơ | `GET /api/booking/quan-tri/ho-so-benh-nhan/{id}`, `PUT .../{id}` `{ hoTen, ngaySinh, gioiTinh?, soDienThoai, diaChi?, soBaoHiemYTe?, cccd? }` | `{id}` là `idHoSoBenhNhan` của lịch hẹn. Số CCCD không che. `cccd` chỉ điền được khi hồ sơ chưa có số: đổi số đã có → 400, số thuộc hồ sơ khác → 409 `XUNG_DOT_DU_LIEU`. Sửa hồ sơ không tự bỏ `canDoiChieu` của lịch hẹn |
| Doctor Management: hồ sơ giới thiệu | `GET /api/catalog/quan-tri/bac-si/{id}`, `PUT .../{id}/ho-so` | PUT thay toàn bộ: `hocVi`, `chucVu`, `soNamKinhNghiem`, `gioiThieuNgan`, `tieuSu`, `quaTrinhDaoTao[]`, `quaTrinhCongTac[]`, `linhVucKhamChua[]` |
| Doctor Management: ảnh giới thiệu | `POST .../{id}/anh` (multipart: `anh`, `loai`, `chuThich`), `PUT .../{id}/anh/{idAnh}`, `PUT .../{id}/anh/thu-tu` `{ idAnh: [] }`, `DELETE .../{id}/anh/{idAnh}` | Tối đa 12 ảnh / bác sĩ (409 `VUOT_SO_ANH_BAC_SI`), mỗi ảnh tối đa 4 MB (413), JPEG / PNG / WebP |
| Patient Management: hồ sơ chờ xác minh | `GET /api/booking/quan-tri/ho-so-benh-nhan/cho-xac-minh?trang=&kichThuoc=`, `POST .../{id}/duyet`, `POST .../{id}/tu-choi` `{ lyDo }` | `{id}` là id hồ sơ. Mỗi dòng có `hoSo`, `taiKhoan`, `khopHoTen`, `khopSoDienThoai` để quản trị viên đối chiếu. Tài khoản đang có hồ sơ chờ xác minh thì chưa xoá được: từ chối trước |
| Medical Catalog: chuyên khoa | `/api/catalog/chuyen-khoa` (CRUD) | Có từ trước |
| User account, quản lý tài khoản | `/api/users...` | Có từ trước (README) |
| Doctor Management: danh bạ, thêm, sửa, ngừng công tác / công tác lại | `/api/catalog/quan-tri/bac-si...` | Bảng "Doctor Management" ở đầu tài liệu |
| Patient Management: danh sách, tìm kiếm, lịch hẹn của 1 hồ sơ | `/api/booking/quan-tri/ho-so-benh-nhan...` | Bảng "Patient Management và Medical Catalog" ở đầu tài liệu |
| Medical Catalog: phòng khám, thuốc | `/api/catalog/quan-tri/phong-kham...`, `/api/medical/quan-tri/thuoc...` | Cùng bảng trên |
| Đánh giá lượt khám, ô "Average Rating" | `/api/booking/lich-hen/cua-toi/{maPhieuKham}/danh-gia`, `/api/booking/bac-si/toi/danh-gia...` | Bảng "Đánh giá lượt khám" ở đầu tài liệu |
| Dashboard: 3 ô số liệu | `GET /api/booking/quan-tri/tong-quan` | Mục "Dashboard" ở đầu tài liệu. Không có "System Uptime" |

## 5. Dữ liệu mẫu để thử

`SEED_DATA=true`: `admin@eclinic.local` / `Admin@123`; `bacsi01..12@eclinic.local` và `benhnhan01..04@eclinic.local` /
`Demo@123`. 12 bác sĩ mẫu có đủ hồ sơ giới thiệu và 3 ảnh mẫu (ảnh lấy từ `placehold.co`, cần internet). Trong dữ liệu mẫu,
2 bác sĩ cùng chuyên khoa làm lệch buổi nhau nên giờ gộp của "bác sĩ bất kỳ" luôn có `soBacSi: 1`.
