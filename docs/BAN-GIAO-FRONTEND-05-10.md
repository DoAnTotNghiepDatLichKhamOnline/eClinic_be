# Bàn giao cho frontend — đối chiếu với "Danh sách UI hiện đang có 05_10_2026"

Tài liệu này đi theo từng màn hình trong file UI ngày 05/10: màn hình lấy dữ liệu từ API nào, frontend cần đổi gì cho khớp
backend. Mọi API gọi qua gateway `http://localhost:8080`; phản hồi luôn bọc trong
`{ thanhCong, thongDiep, duLieu }`, lỗi có `maLoi` và `chiTiet[]` (xem `docs/QUY-UOC-CODE.md` mục 4). Chi tiết từng trường
xem Swagger của từng service. Trang mẫu chạy được: `node scripts/demo/server.js` rồi mở
`http://localhost:5173` (không framework, không bước build). Trang mẫu đi theo đúng danh sách màn hình này: cùng đường dẫn,
cùng thanh bên của bác sĩ và quản trị viên. Mỗi màn hình là một file trong `scripts/demo/man-hinh/`, mở đầu bằng khối chú
thích ghi các API màn hình đó gọi. Lịch dùng FullCalendar 6 tải từ CDN.

**Bổ sung sau tài liệu này**: đổi ngôn ngữ EN / VI (header `Accept-Language`, các trường `…En` của danh mục), email lịch
hẹn và chatbot nằm ở [BAN-GIAO-FRONTEND-NGON-NGU-EMAIL-CHATBOT.md](BAN-GIAO-FRONTEND-NGON-NGU-EMAIL-CHATBOT.md). Mọi câu chữ
API trả về trong tài liệu này (`thongDiep`, tên chuyên khoa, phòng, chức vụ..., câu thông báo) nay theo ngôn ngữ của
request; hình dạng phản hồi không đổi. Trang mẫu có nút EN | VI và khung chat ở mọi màn hình.

**Thay đổi ngày 14/10/2026 (bệnh án, hàng đợi, danh mục thuốc, hồ sơ bác sĩ).** Bác sĩ chỉ kê thuốc có trong danh mục của
quản trị viên; sửa hồ sơ bệnh án cần lý do và có lịch sử sửa; trang chủ bác sĩ có hàng đợi khám theo ca; bác sĩ tự sửa 1
phần hồ sơ giới thiệu. Chi tiết ở 3 mục [Hàng đợi khám](#hàng-đợi-khám-bác-sĩ),
[Sửa hồ sơ bệnh án và lịch sử sửa](#sửa-hồ-sơ-bệnh-án-và-lịch-sử-sửa) và
[Hồ sơ bác sĩ: bác sĩ tự sửa](#hồ-sơ-bác-sĩ-bác-sĩ-tự-sửa). Những chỗ frontend **phải** đổi:

| Chỗ | Trước | Bây giờ | Frontend cần làm |
|---|---|---|---|
| Dòng đơn thuốc khi ghi / sửa kết quả khám (`POST` / `PUT /api/medical/bac-si/toi/lich-hen/{id}/benh-an`) | `donThuoc[] { tenThuoc, donVi?, ... }`; tên chưa có thì tự thêm vào danh mục (chưa xác minh) | `donThuoc[] { idThuoc, lieuDung?, soLanMoiNgay?, soNgayDung?, ghiChuSuDung? }`, `idThuoc` bắt buộc | Ô thuốc là ô chọn từ gợi ý, gửi `id` của gợi ý. Id không có → 404 `KHONG_TIM_THAY`; thuốc ngừng dùng → 409 `THUOC_NGUNG_DUNG`; chưa xác minh → 409 `THUOC_CHUA_XAC_MINH`; 1 thuốc 2 dòng → 400. Dòng thuốc đã có sẵn trong hồ sơ đang sửa thì giữ được dù thuốc đã ngừng dùng / chưa xác minh |
| Gợi ý thuốc `GET /api/medical/bac-si/toi/thuoc?tuKhoa=` | `{ id, tenThuoc, donVi, daXacMinh }`, tìm theo tên | `{ id, tenThuoc, hoatChat, hamLuong, dangBaoChe, duongDung, donVi }`, tìm theo tên **hoặc hoạt chất**; chỉ thuốc đang dùng và đã xác minh | Hiện tên + hàm lượng + dạng bào chế / đường dùng để phân biệt (vd Paracetamol 500mg và 250mg). Bỏ nhãn "chưa xác minh" |
| Mọi chỗ đọc dòng đơn thuốc (`BenhAnResponse.donThuoc[]`, `ketQua.donThuoc[]` ở hồ sơ khám, lịch sử khám, trang cá nhân) | `tenThuoc, donVi, lieuDung, ...` | thêm `idThuoc, hoatChat, hamLuong, duongDung` (null ở thuốc cũ quản trị viên chưa bổ sung) | Hiện hàm lượng cạnh tên thuốc. Form sửa nạp lại `idThuoc` của từng dòng |
| Thêm / sửa thuốc của quản trị viên (`POST` / `PUT /api/medical/quan-tri/thuoc...`) | `{ tenThuoc, donVi?, moTa? }`, trùng tên → 409 | thêm `hoatChat`, `hamLuong` **bắt buộc**, `dangBaoChe?`, `dangBaoCheEn?`, `duongDung?`; trùng **tên + hàm lượng** → 409 `TEN_THUOC_DA_TON_TAI` | Thêm các ô mới (đường dùng là ô chọn, giá trị ở mục "Patient Management và Medical Catalog"). Xác minh thuốc thiếu hoạt chất / hàm lượng → 409 `THUOC_THIEU_THONG_TIN` |
| Sửa kết quả khám `PUT .../lich-hen/{id}/benh-an` | Cùng body như `POST`, sửa bao nhiêu lần cũng được | Bắt buộc thêm `lyDoSua` (tối đa 500 ký tự) và `phienBan` (giá trị đang xem). Chỉ sửa được trong 7 ngày kể từ lần lưu đầu | Thêm ô lý do; gửi `phienBan` từ `ketQua` / `BenhAnResponse`. 409 `DU_LIEU_DA_THAY_DOI` → tải lại rồi sửa lại; 409 `HET_HAN_SUA_BENH_AN` hoặc đã quá `hanSua` → form chỉ xem |
| Ngày tái khám ở quá khứ | 400 lỗi theo trường (`chiTiet`) | 400 `DU_LIEU_KHONG_HOP_LE` từ service, không có `chiTiet` (khi sửa mà giữ nguyên ngày cũ thì vẫn lưu được) | Hiện `thongDiep` |
| `BenhAnResponse`, `ketQua` ở booking | | `BenhAnResponse` thêm `phienBan`, `ngaySuaCuoi`, `hanSua`. `ketQua` thêm `ngaySuaCuoi` (mọi chỗ); `phienBan`, `hanSua` **chỉ** có ở hồ sơ khám của bác sĩ (không có mặt trong JSON trả cho bệnh nhân) | Hiện "Đã chỉnh sửa lúc …" khi `ngaySuaCuoi` khác null |
| Thêm bác sĩ `POST /api/catalog/quan-tri/bac-si` | `soNamKinhNghiem?` | `namBatDauHanhNghe?` (năm, từ 1950, không sau năm nay) | Đổi ô "số năm kinh nghiệm" thành "năm bắt đầu hành nghề" |
| Hồ sơ giới thiệu của quản trị viên `PUT /api/catalog/quan-tri/bac-si/{id}/ho-so` | không có `phienBan`; `soNamKinhNghiem` | bắt buộc `phienBan` (= `phienBanHoSo` đã tải), `namBatDauHanhNghe` thay `soNamKinhNghiem`, thêm giấy phép, ngôn ngữ, giải thưởng, nghiên cứu | 409 `DU_LIEU_DA_THAY_DOI` → tải lại hồ sơ rồi lưu lại. Mọi phản hồi vẫn có `soNamKinhNghiem` (nay suy ra từ năm bắt đầu) |

**Thay đổi ngày 15/10/2026 (xác nhận ngay, bác sĩ hủy lịch, không đến, bệnh án đầy đủ).** Lịch hẹn được xác nhận ngay
khi đặt hoặc đổi: bác sĩ không còn duyệt, chỉ hủy được lịch trước giờ khám, kèm lý do. Lịch chưa khám của ngày đã qua thành
`KHONG_DEN`. Kết quả khám có thêm lý do khám, sinh hiệu, khám lâm sàng, mã ICD-10 và bệnh kèm theo; mỗi dòng thuốc bắt buộc
số ngày và số lượng. Phía bệnh nhân có ở tài liệu API bản 2.3 (`eClinic_API_XacThuc_TaiKhoan.docx`). Những chỗ frontend
**phải** đổi:

| Chỗ | Trước | Bây giờ | Frontend cần làm |
|---|---|---|---|
| `trangThai` của lịch hẹn (mọi màn hình) | Lịch mới `CHO_XAC_NHAN`, chờ bác sĩ xác nhận | Không còn giá trị này: đặt / đổi lịch xong là `DA_XAC_NHAN`. Thêm `KHONG_DEN`. `BI_TU_CHOI` nay là bác sĩ đã hủy lịch, lý do ở `lyDoHuy` | Bỏ nhãn "chờ xác nhận"; nhãn "Bác sĩ đã hủy" cho `BI_TU_CHOI` (hiện `lyDoHuy`), "Không đến" cho `KHONG_DEN` |
| Màn hình bác sĩ Appointment Requests | `GET /api/booking/bac-si/toi/lich-hen/yeu-cau`, `POST .../lich-hen/{id}/xac-nhan`, `POST /api/booking/bac-si/toi/lich-hen/{id}/tu-choi` `{ lyDo }` | 3 route này đã bỏ (404). Màn hình mới Upcoming Appointments (`/doctor/upcoming-appointments`): `GET /api/booking/bac-si/toi/lich-hen/lich?tuNgay=&denNgay=`, giữ lịch `DA_XAC_NHAN`, không `canDoiLich`, lượt khám chưa bắt đầu; nút Cancel gọi `POST /api/booking/bac-si/toi/lich-hen/{id}/huy` `{ lyDo }` | Đổi màn hình và đường dẫn; Cancel cần ô lý do (bắt buộc). Chi tiết và lỗi ở mục 3 |
| Dashboard bác sĩ `GET /api/booking/bac-si/toi/tong-quan` | `yeuCauChoXacNhan` (số lịch chờ xác nhận) | `lichHenSapToi`: số dòng của Upcoming Appointments. `dangCho` chỉ còn tính `DA_XAC_NHAN` | Đổi tên trường và nhãn ô |
| Bệnh nhân không đến | Lịch giữ nguyên trạng thái sau ngày khám | Từ 00:01 ngày hôm sau (và mỗi lần booking-service khởi động), lịch `DA_XAC_NHAN` của ngày đã qua mà chưa khám thành `KHONG_DEN`; trừ người đã được gọi vào khám (`DANG_KHAM`) và lịch `canDoiLich`. Hàng đợi của ngày đã qua hiện lịch này là `VANG_MAT` | Nhãn "Không đến"; không có nút hủy / đổi / đánh giá (`duocHuyDoi: false`). Trong ngày khám bác sĩ vẫn đánh dấu vắng mặt và gọi lại như cũ |
| Ghi / sửa kết quả khám `POST` / `PUT /api/medical/bac-si/toi/lich-hen/{id}/benh-an` | `{ chanDoan, ghiChu?, ngayTaiKhamDeXuat?, donThuoc?[] }`; dòng thuốc có `soNgayDung?` | Thêm `lyDoKham?`, `sinhHieu?`, `khamLamSang?`, `maIcdChinh?`, `chanDoanKemTheo?`. Mỗi dòng thuốc **bắt buộc** `soNgayDung` và `soLuong` | Thêm các ô; khoảng giá trị và luật ở mục 3, dòng "Consultation: ghi kết quả khám". Sửa 1 bệnh án cũ: dòng thuốc có `soLuong: null` phải được nhập số lượng trước khi lưu |
| Gợi ý mã ICD-10 | (chưa có) | `GET /api/medical/bac-si/toi/icd10?tuKhoa=` → tối đa 20 `{ ma, ten, manTinh }` | Ô mã ICD-10 cạnh ô chẩn đoán chính, có gợi ý; hiện "(mạn tính)" khi `manTinh` |
| Đọc kết quả khám (`BenhAnResponse`; `ketQua` ở hồ sơ khám, lịch sử khám, trang cá nhân, chi tiết lịch hẹn của bệnh nhân) | `chanDoan, ghiChu, ngayTaiKhamDeXuat, donThuoc[], ...` | Thêm `lyDoKham`, `sinhHieu { mach, nhietDo, huyetApTamThu, huyetApTamTruong, nhipTho, canNang, chieuCao }` (null khi không đo chỉ số nào), `khamLamSang`, `icdChinh { ma, ten, manTinh }` (null khi không chọn mã; `ten` theo `Accept-Language`), `chanDoanKemTheo`; dòng thuốc thêm `soLuong` (null ở dòng ghi trước 15/10) | Hiện thêm các trường; cột "Số lượng" ở đơn thuốc |
| Lỗi mới khi ghi / sửa kết quả khám | | 400 `DU_LIEU_KHONG_HOP_LE` có `thongDiep`, không có `chiTiet`: mã ICD-10 không có trong danh mục; huyết áp thiếu 1 số hoặc tâm trương không thấp hơn tâm thu; thuốc quá 30 ngày (90 khi mã chính mạn tính); trẻ dưới 72 tháng có kê thuốc mà thiếu cân nặng. Sinh hiệu ngoài khoảng, thiếu `soNgayDung` / `soLuong` → 400 có `chiTiet` theo trường | Hiện `thongDiep` / lỗi theo trường |
| Lịch sử sửa bệnh án | `cacTruongDoi`: `CHAN_DOAN`, `GHI_CHU`, `NGAY_TAI_KHAM`, `DON_THUOC` | Thêm `LY_DO_KHAM`, `SINH_HIEU`, `KHAM_LAM_SANG`, `CHAN_DOAN_KEM_THEO`; đổi mã ICD-10 tính là `CHAN_DOAN`. `truoc` / `sau` có thêm các trường mới | Nhãn cho 4 giá trị mới |
| Email và thông báo của bệnh nhân | Email "đã nhận yêu cầu đặt lịch"; thông báo `LICH_HEN_DA_XAC_NHAN` / `LICH_HEN_BI_TU_CHOI` khi bác sĩ xác nhận / từ chối | Email đặt lịch nói lịch đã được xác nhận. Bác sĩ hủy → thông báo `LICH_HEN_BI_TU_CHOI` (câu "đã hủy", có lý do) và email tới địa chỉ liên hệ. `LICH_HEN_DA_XAC_NHAN` chỉ còn ở thông báo cũ | Không đổi cách gọi |

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
| `/doctor/upcoming-appointments` | bác sĩ | Doctor: Upcoming Appointments (thay Appointment Requests từ 15/10) | `bs-lich-hen-sap-toi.js` |
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
| Doctor: Dashboard | `GET /api/booking/bac-si/toi/tong-quan`, `GET /api/booking/bac-si/toi/hang-doi?ngay=` (+ gọi khám / vắng mặt), `GET /api/booking/bac-si/toi/danh-gia` | Mục "Dashboard"; mục "Hàng đợi khám"; mục "Đánh giá lượt khám" |
| Doctor: Work Schedule | `GET /api/booking/bac-si/toi/lich-lam-viec`, `GET .../lich-hen/lich`, `POST /api/booking/bac-si/toi/yeu-cau-doi-lich` | Mục 3; bảng "Ca làm việc và yêu cầu đổi ca / xin nghỉ" |
| Doctor: Upcoming Appointments (thay Appointment Requests) | `GET /api/booking/bac-si/toi/lich-hen/lich`, `POST /api/booking/bac-si/toi/lich-hen/{id}/huy` | Mục 3 |
| Doctor: Consultation / EMR | `GET .../lich-hen/tra-cuu`, `GET .../{id}/ho-so-kham`, `POST / PUT /api/medical/bac-si/toi/lich-hen/{id}/benh-an`, `GET .../benh-an/lich-su`, `GET /api/medical/bac-si/toi/thuoc`, `GET /api/medical/bac-si/toi/icd10` | Mục 3; mục "Sửa hồ sơ bệnh án và lịch sử sửa" |
| Doctor: Doctor Profile, menu tài khoản | `GET` / `PUT /api/catalog/bac-si/toi/ho-so`, `/api/catalog/bac-si/toi/anh...`, `GET /api/users/me`, `/api/users/me/...` | Mục "Hồ sơ bác sĩ: bác sĩ tự sửa"; README "Tài khoản của tôi" |
| Chuông thông báo (Doctor, Patient, Admin) | `GET /api/notification/thong-bao`, `GET .../so-chua-doc`, `POST .../{id}/da-doc` | README "Thông báo trong ứng dụng" |
| Admin: Dashboard | `GET /api/booking/quan-tri/tong-quan` | Mục "Dashboard" |
| Admin: Schedule Management | `GET / POST / PUT /api/booking/quan-tri/lich-lam-viec...`, `/api/booking/quan-tri/yeu-cau-doi-lich...` | Mục 4; bảng "Ca làm việc và yêu cầu đổi ca / xin nghỉ" |
| Admin: Medical Catalog | `/api/catalog/chuyen-khoa`, `/api/catalog/quan-tri/phong-kham...`, `/api/medical/quan-tri/thuoc...` | Mục "Patient Management và Medical Catalog" |
| Admin: Doctor Management | `/api/catalog/quan-tri/bac-si...`, `GET /api/booking/quan-tri/danh-gia?idBacSi=` | Mục "Doctor Management"; mục 4 (hồ sơ, ảnh giới thiệu) |
| Admin: Patient Management | `/api/booking/quan-tri/ho-so-benh-nhan...`, `GET /api/medical/quan-tri/lich-hen/{id}/benh-an/lich-su` | Mục "Patient Management và Medical Catalog"; mục "Sửa hồ sơ bệnh án và lịch sử sửa"; mục 4 |
| (ngoài UI) quản trị tài khoản | `/api/users...` | README "Tài khoản của tôi và quản trị tài khoản" |

Mọi màn hình trong danh sách UI đều đã có API; riêng ô "System Uptime" của Admin Dashboard không được cung cấp.

**Vòng đời lịch hẹn:** đặt hoặc đổi lịch tạo lịch `DA_XAC_NHAN` ngay (phiếu khám dùng được ngay, không chờ bác sĩ). Bác sĩ
của lịch hẹn hủy được lịch trước giờ khám, kèm lý do (`BI_TU_CHOI`). Ghi kết quả khám chuyển lịch sang `DA_HOAN_THANH`. Lịch
chưa khám của ngày đã qua thành `KHONG_DEN` từ 00:01 ngày hôm sau, trừ người đã được gọi vào khám và lịch `canDoiLich`.
Phiếu khám (`GET /api/booking/phieu-kham/{ma}`), dòng "lịch hẹn của tôi" và dòng lịch hẹn của bác sĩ đều có `lyDoHuy`: lý
do bác sĩ hủy hoặc lý do bệnh nhân hủy, null ở các trạng thái khác — hiện cho bệnh nhân cạnh trạng thái.

**Bệnh nhân hủy / đổi lịch** (danh sách UI chưa có màn hình cho việc này; trang mẫu đặt nút ở chi tiết lịch hẹn và ở trang phiếu khám):

| Việc | API | Ghi chú |
|---|---|---|
| Hủy (đã đăng nhập) | `POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/huy` `{ lyDo? }` | 200, trả phiếu khám với `trangThai: DA_HUY`. Chỉ tài khoản đã đặt hoặc người khám; chỉ là người giám hộ theo CCCD → 403; lịch không thấy được → 404 |
| Đổi lịch (đã đăng nhập) | `POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/doi-lich` `{ idLichLamViec | idChuyenKhoa, gioBatDauKhung }` | 201, trả như đặt lịch: lịch **mới** `DA_XAC_NHAN` với `maPhieuKham` mới (không chờ bác sĩ xác nhận). Lịch cũ thành `DA_HUY_DO_DOI_LICH`. Không gửi lại thông tin người khám. Phải khác khung giờ đang giữ (→ 400) |
| Hủy / đổi (khách, không đăng nhập) | `POST /api/booking/phieu-kham/{maPhieuKham}/huy` `{ soDienThoai, lyDo? }`, `POST .../phieu-kham/{maPhieuKham}/doi-lich` `{ soDienThoai, ... }` | `soDienThoai` = đủ 10 số SĐT liên hệ đã nhập lúc đặt (phiếu chỉ hiện bản đã che). Sai → 403 `SO_DIEN_THOAI_KHONG_KHOP`; giới hạn số lần gọi theo IP |
| Ẩn / hiện nút | `duocHuyDoi`, `hanHuyDoi` trên phiếu khám và dòng "lịch hẹn của tôi" | `duocHuyDoi` = lịch `DA_XAC_NHAN`, chưa quá `hanHuyDoi` (giờ khám trừ 2 giờ) và, ở dòng "của tôi", tài khoản là người đặt / người khám. Quá hạn → 409 `QUA_HAN_HUY_DOI_LICH` (hiện "vui lòng liên hệ phòng khám") |
| Giới hạn | | Đổi tối đa 2 lần cho 1 lần đặt (→ 409 `VUOT_SO_LAN_DOI_LICH`), hủy thì không giới hạn; lịch đã khám / đã hủy / bác sĩ đã hủy / không đến → 409 `LICH_HEN_KHONG_HUY_DOI_DUOC`; khung mới hết chỗ → 409 `KHUNG_GIO_KHONG_CON_TRONG` và lịch cũ giữ nguyên |

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
`canDoiLich`. Khi `true`: lịch hẹn vẫn `DA_XAC_NHAN` nhưng ca đã hủy, bệnh nhân phải đổi sang khung giờ khác
hoặc hủy. Với lịch này `duocHuyDoi` đúng tới giờ khám cũ (`hanHuyDoi` = giờ khám, không trừ 2 giờ) và lần đổi không tính vào
giới hạn 2 lần; lịch mới sau khi đổi không còn cờ. Bác sĩ không thấy lịch này trong Upcoming Appointments và không hủy được
(409 `LICH_HEN_CAN_DOI_LICH`); lịch này cũng không bị chuyển thành `KHONG_DEN`.

**Số thứ tự khám** là thứ hạng của lượt khám trong các lượt của phòng trong ngày, nên khi phòng có thêm ca đứng trước, ca được
kéo dài hoặc ca đổi phòng thì số thứ tự của các lịch hẹn còn hiệu lực của phòng trong ngày đó được tính lại. Luôn hiển thị
`soThuTu` mới nhất từ API, không lưu lại ở frontend.

**Chuông thông báo (mọi vai trò đã đăng nhập).** UI mới vẽ chuông ở khu bác sĩ; bệnh nhân cũng có thông báo (bác sĩ hủy lịch,
nhắc lịch...), nên trang mẫu hiện chuông cho mọi tài khoản.

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/notification/thong-bao?chuaDoc=&trang=&kichThuoc=` | Mọi tài khoản đã đăng nhập | Thông báo của tôi, mới nhất trước, dạng `TrangDuLieu`. `chuaDoc=true` chỉ lấy thông báo chưa đọc; `kichThuoc` mặc định 20, tối đa 100. Mỗi dòng: `{ id, loai, noiDung, daDoc, ngayTao, idLichHen, maPhieuKham, idYeuCau }`. `noiDung` là câu đã soạn sẵn theo `Accept-Language` của request đọc (đổi ngôn ngữ rồi tải lại là câu đổi theo; tên người, mã và lý do người dùng gõ giữ nguyên văn; thông báo tạo trước khi có 2 ngôn ngữ chỉ có câu tiếng Việt), hiển thị nguyên văn. `maPhieuKham` chỉ có khi người đọc là bệnh nhân (dùng để mở chi tiết lịch hẹn); bác sĩ dùng `idLichHen` |
| `GET /api/notification/thong-bao/so-chua-doc` | Mọi tài khoản đã đăng nhập | `{ soChuaDoc }`: số trên biểu tượng chuông |
| `POST /api/notification/thong-bao/{id}/da-doc` | Mọi tài khoản đã đăng nhập | Đánh dấu 1 thông báo của tôi đã đọc, gọi lại nhiều lần vẫn được. Thông báo của tài khoản khác hoặc không có -> 404 `KHONG_TIM_THAY_THONG_BAO` |
| `POST /api/notification/thong-bao/da-doc-tat-ca` | Mọi tài khoản đã đăng nhập | `{ soDaDanhDau }` |

| Sự kiện | `loai` | Người nhận |
|---|---|---|
| Có lịch hẹn mới (kể cả khách đặt) | `LICH_HEN_MOI` | Bác sĩ của lịch hẹn |
| Bác sĩ hủy lịch đã xác nhận (câu thông báo có lý do) | `LICH_HEN_BI_TU_CHOI` | Tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân (mỗi tài khoản 1 thông báo); kèm email tới địa chỉ liên hệ của lịch hẹn |
| (đã bỏ từ 15/10) Bác sĩ xác nhận | `LICH_HEN_DA_XAC_NHAN` | Không còn được tạo; chỉ có ở thông báo cũ |
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
- Bấm 1 thông báo: gọi `da-doc` rồi chuyển màn hình. Trang mẫu: bác sĩ sang Upcoming Appointments (`LICH_HEN_MOI`,
  `LICH_HEN_DA_DOI`) hoặc Work Schedule; bệnh nhân sang chi tiết lịch hẹn theo `maPhieuKham`.
- Người làm thao tác không nhận thông báo về thao tác của chính mình; khách đặt không đăng nhập không có thông báo. Bệnh
  nhân nhận thêm `NHAC_LICH_KHAM` (nhắc trước giờ khám, có `maPhieuKham`) và `NHAC_TAI_KHAM` (bác sĩ hẹn / đổi ngày tái
  khám, và lời nhắc trước ngày đó 7 ngày). Với `NHAC_TAI_KHAM`, `idLichHen` / `maPhieuKham` là của lượt khám **đã xong**:
  chưa có lịch hẹn mới nào, bấm vào thì mở trang đặt lịch (có thể chọn sẵn bác sĩ của lượt khám đó). Email cùng nội dung
  có nút mở `FRONTEND_URL` + `FOLLOWUP_BOOKING_PATH` (mặc định `/dat-lich`). Chưa có xoá thông báo.

`/doctor/upcoming-appointments` là đường dẫn backend tự chọn (màn hình thay Appointment Requests từ 15/10, UI 05/10 chưa có):
frontend dùng đường dẫn khác thì báo lại để trang mẫu đổi theo.

### Doctor Management (quản trị viên) và lần đăng nhập đầu của bác sĩ mới

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/catalog/quan-tri/bac-si?tuKhoa=&idChuyenKhoa=&trangThai=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Danh bạ bác sĩ, bác sĩ mới thêm đứng trước, **cả bác sĩ ngừng công tác**. `tuKhoa` so với họ tên, email, số điện thoại, số giấy phép. Mỗi dòng: `id`, `maBacSi`, `hoTen`, `email`, `soDienThoai`, `anhDaiDien`, `hocVi`, `soGiayPhep`, `idChuyenKhoa`, `tenChuyenKhoa`, `soLuotDaKham`, `trangThai` (`DANG_CONG_TAC` / `NGUNG_CONG_TAC`), `trangThaiTaiKhoan`, `phaiDoiMatKhau` |
| `POST /api/catalog/quan-tri/bac-si` | `QUAN_TRI_VIEN` | "Add New Doctor": `{ hoTen, email, soDienThoai?, idChuyenKhoa, soGiayPhep?, hocVi?, chucVu?, namBatDauHanhNghe? }` → 201, trả hồ sơ quản trị của bác sĩ. Tạo tài khoản đăng nhập và hồ sơ bác sĩ trong 1 bước. `namBatDauHanhNghe` từ 1950, không sau năm nay (400); số năm kinh nghiệm suy ra từ đây |
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
| `GET /api/medical/quan-tri/thuoc?tuKhoa=&daXacMinh=&trangThai=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Medical Catalog: danh mục thuốc, chưa xác minh trước rồi theo tên: `id`, `tenThuoc`, `hoatChat`, `hamLuong`, `dangBaoChe`, `dangBaoCheEn`, `duongDung`, `donVi`, `donViEn`, `moTa`, `moTaEn`, `daXacMinh`, `trangThai` (`DANG_DUNG` / `NGUNG_DUNG`), `tenBacSiTao`, `soLanKe` |
| `POST /api/medical/quan-tri/thuoc`, `PUT .../{id}` | `QUAN_TRI_VIEN` | `{ tenThuoc, hoatChat, hamLuong, dangBaoChe?, dangBaoCheEn?, duongDung?, donVi?, donViEn?, moTa?, moTaEn? }`; thêm → 201, thuốc đã xác minh. `duongDung`: `UONG`, `NGAM_DUOI_LUOI`, `TIEM`, `BOI_NGOAI_DA`, `NHO_MAT`, `NHO_TAI`, `NHO_MUI`, `XIT_HIT`, `DAT` (đặt hậu môn / âm đạo), `KHAC` (nhãn do frontend đặt). Trùng **tên + hàm lượng** (không phân biệt hoa thường / khoảng trắng): 409 `TEN_THUOC_DA_TON_TAI` |
| `POST /api/medical/quan-tri/thuoc/{id}/xac-minh`, `POST .../{id}/ngung-dung`, `POST .../{id}/dung-lai` | `QUAN_TRI_VIEN` | Xác minh thuốc bác sĩ đã tự thêm trước đây (nay bác sĩ không thêm được nữa): thuốc thiếu hoạt chất hoặc hàm lượng → 409 `THUOC_THIEU_THONG_TIN`, bổ sung bằng `PUT` trước. Cho ngừng dùng / dùng lại. Gọi lại nhiều lần không sao |

- **Tìm hồ sơ bệnh nhân.** `tuKhoa` so với họ tên, số điện thoại, số bảo hiểm y tế (chứa từ khoá). Số CCCD chỉ tra được khi
  gõ **đủ 12 số** (khớp hoàn toàn); gõ 1 phần số CCCD không ra kết quả. Danh sách chỉ trả 4 số cuối của CCCD, số đầy đủ có
  ở `GET /api/booking/quan-tri/ho-so-benh-nhan/{id}`.
- **Phòng khám không xoá được**, chỉ ngừng hoạt động. Phòng còn ca sắp tới (`soCaSapToi > 0`) thì ngừng hoạt động và đổi
  chuyên khoa đều trả 409 `PHONG_CON_CA_LAM_VIEC` (thông điệp có số ca): chuyển các ca đó sang phòng khác bằng
  `PUT /api/booking/quan-tri/lich-lam-viec/{id}` trước. Tên phòng và tầng sửa được bất cứ lúc nào.
- **Bác sĩ chỉ kê thuốc trong danh mục này**, đang dùng và đã xác minh; bác sĩ không thêm được thuốc mới (thuốc chưa có
  thì quản trị viên thêm). Thuốc chưa xác minh là thuốc bác sĩ đã tự thêm trước thay đổi ngày 14/10.
- **Thuốc không xoá và không gộp được.** Thuốc đã có trong đơn thuốc (`soLanKe > 0`) không đổi được tên, hoạt chất, hàm
  lượng, dạng bào chế, đường dùng nữa (chỉ sửa được cách viết hoa / khoảng trắng của tên); đổi → 409 `THUOC_DA_DUOC_KE`
  (dòng đơn thuốc chỉ lưu id thuốc, đổi là sửa luôn đơn cũ). Đơn vị và mô tả sửa được bất cứ lúc nào.
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
| `GET /api/booking/bac-si/toi/tong-quan` | `BAC_SI` | Dashboard của bác sĩ, 1 lần gọi cho cả 4 ô: `{ lichHenHomNay, dangCho, daKham, diemDanhGia, soDanhGia, lichHenSapToi, tinhLuc }` |
| `GET /api/booking/quan-tri/tong-quan` | `QUAN_TRI_VIEN` | Dashboard của quản trị viên, 1 lần gọi cho cả 3 ô: `{ bacSiDangCongTac, benhNhanDaDangKy, luotDatTrongThang, tinhLuc }` |

Định nghĩa từng số (frontend không tự đếm lại từ các danh sách: danh sách có phân trang và bộ lọc riêng):

| Ô trên UI | Trường | Cách tính |
|---|---|---|
| Today's Appointments | `lichHenHomNay` | Lịch hẹn của bác sĩ có giờ khám hôm nay, **không tính** `DA_HUY`, `DA_HUY_DO_DOI_LICH`, `BI_TU_CHOI` |
| Waiting | `dangCho` | Trong số đó: `DA_XAC_NHAN` và không có `canDoiLich` (chưa khám) |
| Completed | `daKham` | Trong số đó: `DA_HOAN_THANH` |
| Average Rating | `diemDanhGia`, `soDanhGia` | Trung bình mọi đánh giá của bác sĩ, 1 chữ số thập phân; `null` và `0` khi chưa có. Hiển thị kèm số lượt |
| (nhãn Upcoming Appointments) | `lichHenSapToi` | Số lịch `DA_XAC_NHAN` của bác sĩ, không có `canDoiLich`, mà lượt khám bắt đầu sau `tinhLuc` = số dòng của màn hình Upcoming Appointments |
| Active Doctors | `bacSiDangCongTac` | Bác sĩ `DANG_CONG_TAC` có tài khoản `DA_KICH_HOAT`: đúng tập bác sĩ của danh sách công khai |
| Registered Patients | `benhNhanDaDangKy` | Tài khoản vai trò `BENH_NHAN` đã kích hoạt. Khách chỉ đặt lịch (không có tài khoản) không tính |
| Bookings This Month | `luotDatTrongThang` | Lịch hẹn **được tạo** từ 00:00 ngày 1 tháng này, mọi trạng thái trừ `DA_HUY_DO_DOI_LICH` (1 lần đổi lịch chỉ tính 1) |

- `tinhLuc` là thời điểm backend tính các số; số được đếm trực tiếp mỗi lần gọi, không lưu sẵn.
- "System Uptime" trên Dashboard của quản trị viên **không có API** (đã thống nhất bỏ): frontend ẩn ô này.

### Hàng đợi khám (bác sĩ)

Đặt ở đầu Dashboard của bác sĩ (trang mẫu `bs-tong-quan.js` mở thẳng vào hàng đợi hôm nay, bảng "lịch hẹn hôm nay" cũ
được thay bằng hàng đợi). `GET /api/booking/bac-si/toi/lich-hen?ngay=` vẫn còn, không đổi.

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/booking/bac-si/toi/hang-doi?ngay=` | `BAC_SI` | `ngay` bỏ trống = hôm nay. `{ ngay, idCaHienTai, tinhLuc, cacCa[] }`; mỗi ca đang hoạt động (giờ bắt đầu sớm nhất trước): `{ idLichLamViec, buoi: SANG \| CHIEU \| TOI, gioBatDau, gioKetThuc, phongKham, soLuong { tong, choKham, dangKham, vangMat, daKham }, idDangKham, idTiepTheo, cacLuot[] }`; mỗi lượt (theo số thứ tự): `{ trangThaiHangDoi, goiLuc, lichHen }`, `lichHen` như dòng danh sách trong ngày (mục 3) |
| `POST /api/booking/bac-si/toi/hang-doi/goi-tiep?idLichLamViec=` | `BAC_SI` | Gọi người `CHO_KHAM` có số thứ tự nhỏ nhất của ca (không gọi người vắng mặt) |
| `POST /api/booking/bac-si/toi/lich-hen/{id}/goi-kham` | `BAC_SI` | Gọi 1 người cụ thể: người đang chờ, hoặc gọi lại người vắng mặt; gọi lại người đang khám vẫn 200 |
| `POST /api/booking/bac-si/toi/lich-hen/{id}/vang-mat` | `BAC_SI` | Đánh dấu vắng mặt; gọi lại được sau. Đã vắng mặt vẫn 200 |

- Mỗi thao tác trả lại toàn bộ hàng đợi hôm nay (cùng dạng `GET`): vẽ lại màn hình từ phản hồi, không gọi thêm.
- `trangThaiHangDoi`: `CHO_KHAM` (chưa gọi), `DANG_KHAM` (đã gọi vào khám), `VANG_MAT`, `DA_KHAM` (đã ghi kết quả khám:
  ghi kết quả bằng `POST .../benh-an` là xong lượt, không có nút "khám xong" riêng). Trạng thái hàng đợi tách khỏi
  `trangThai` của lịch hẹn, không đổi `trangThai`.
- `buoi` suy ra từ giờ bắt đầu ca: trước 12:00 `SANG`, trước 17:00 `CHIEU`, còn lại `TOI`. `idCaHienTai` = ca nên mở sẵn:
  hôm nay là ca đang diễn ra, không có thì ca kế tiếp, hết ca thì ca cuối; ngày khác là ca đầu tiên; null khi không có ca.
- `idDangKham` / `idTiepTheo`: id lịch hẹn đang khám / sẽ được gọi tiếp của ca (`idTiepTheo` chỉ có ở hôm nay).
- Mỗi bác sĩ chỉ 1 người đang khám mỗi lúc: gọi người khác khi chưa ghi kết quả / chưa đánh dấu vắng mặt người đang khám
  → 409 `DANG_KHAM_BENH_NHAN_KHAC` (thông điệp có tên và số thứ tự người đang khám). Ca không còn ai chờ → 409
  `HANG_DOI_TRONG`. Thao tác ở ngày khác hôm nay → 409 `KHONG_PHAI_NGAY_KHAM` (ngày khác chỉ xem). Lịch hẹn đã hủy / đã
  khám → 409 `LICH_HEN_KHONG_KHAM_DUOC`. Lịch / ca của bác sĩ khác → 404.
- Hàng đợi của ngày đã qua (chỉ xem): lịch `KHONG_DEN` hiện với `trangThaiHangDoi: VANG_MAT`, lịch đã khám hiện `DA_KHAM`;
  không có nút gọi / vắng mặt.
- Không có đẩy realtime: gọi lại `GET hang-doi` khoảng 15 giây 1 lần (trang mẫu làm vậy), giữ tab ca đang chọn.

### Sửa hồ sơ bệnh án và lịch sử sửa

| API | Ai gọi | Ghi chú |
|---|---|---|
| `PUT /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an` | `BAC_SI` của lượt khám | Body như `POST` cộng `lyDoSua` (bắt buộc, tối đa 500 ký tự) và `phienBan` (bắt buộc: `phienBan` đang xem). 200 trả `BenhAnResponse` với `phienBan` mới |
| `GET /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an/lich-su` | `BAC_SI` | Bác sĩ của lượt khám, hoặc bác sĩ có lịch hẹn với cùng bệnh nhân từ giờ khám đó trở đi (thấy lượt khám này trong "lần khám trước" của hồ sơ khám); không thì 404 |
| `GET /api/medical/quan-tri/lich-hen/{idLichHen}/benh-an/lich-su` | `QUAN_TRI_VIEN` | Lịch sử của mọi lượt khám (trang mẫu: nút "Lịch sử sửa" ở lịch hẹn đã khám của Patient Management) |

- **Luật sửa.** Sửa được trong `RECORD_EDIT_DAYS` (mặc định 7) ngày **kể từ lần lưu đầu tiên** (`hanSua` trong phản hồi);
  sau đó hồ sơ chỉ xem → 409 `HET_HAN_SUA_BENH_AN`. Người khác đã sửa trước (gửi `phienBan` cũ) → 409
  `DU_LIEU_DA_THAY_DOI`: tải lại rồi sửa lại. Nội dung không khác bản đang lưu → 400 `DU_LIEU_KHONG_HOP_LE` (không ghi
  gì). 2 lần sửa cùng lúc chạy lần lượt, lần sau nhận 409.
- **Lịch sử:** `{ idLichHen, maTraCuu, phienBanHienTai, ngayGhiNhan, cacLanSua[] }`, mới nhất trước; mỗi lần sửa
  `{ phienBan, ngaySua, bacSi { id, hoTen }, lyDo, cacTruongDoi[], truoc, sau }`. `cacTruongDoi`: `LY_DO_KHAM`,
  `SINH_HIEU`, `KHAM_LAM_SANG`, `CHAN_DOAN` (cả khi chỉ đổi mã ICD-10), `CHAN_DOAN_KEM_THEO`, `GHI_CHU`, `NGAY_TAI_KHAM`,
  `DON_THUOC`; `truoc` / `sau` là toàn bộ nội dung `{ lyDoKham, sinhHieu, khamLamSang, chanDoan, maIcdChinh,
  chanDoanKemTheo, ghiChu, ngayTaiKhamDeXuat, donThuoc[] { idThuoc, tenThuoc, hamLuong, lieuDung, soLanMoiNgay,
  soNgayDung, soLuong, ghiChuSuDung } }` để so cũ / mới. Không sửa, không xoá
  được dòng lịch sử.
- **Bệnh nhân** chỉ thấy `ketQua.ngaySuaCuoi` ("Đã chỉnh sửa lúc …"), không thấy lý do, nội dung cũ, `phienBan`, `hanSua`.
- Việc **mở / xem** hồ sơ bệnh án không được ghi lại (chưa làm, đã thống nhất để sau).

### Hồ sơ bác sĩ: bác sĩ tự sửa

| Ai sửa | Mục |
|---|---|
| Bác sĩ tự sửa (có hiệu lực ngay, không chờ duyệt) | Ảnh đại diện, số điện thoại (`/api/users/me...`); giới thiệu ngắn, tiểu sử, ngôn ngữ khám, giải thưởng, công trình nghiên cứu (bản VI và EN); ảnh làm việc |
| Chỉ quản trị viên | Họ tên, email, chuyên khoa, số giấy phép, ngày cấp, nơi cấp, phạm vi hành nghề, học vị, chức vụ, năm bắt đầu hành nghề, quá trình đào tạo, quá trình công tác, lĩnh vực khám chữa, ảnh chứng chỉ, trạng thái. Quản trị viên sửa được cả các mục bác sĩ tự sửa |

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/catalog/bac-si/toi/ho-so` | `BAC_SI` | Hồ sơ của chính bác sĩ, cùng dạng `GET /api/catalog/quan-tri/bac-si/{id}` (đủ 2 ngôn ngữ, có `phienBanHoSo`, `namBatDauHanhNghe`, giấy phép, `anh[]` kèm `loai`). Hiện các mục quản trị viên quản lý dạng chỉ xem |
| `PUT /api/catalog/bac-si/toi/ho-so` | `BAC_SI` | `{ phienBan, gioiThieuNgan, gioiThieuNganEn, tieuSu, tieuSuEn, ngonNguKham[], giaiThuong[], giaiThuongEn[], congTrinhNghienCuu[], congTrinhNghienCuuEn[] }`, gửi đủ (bỏ trống = xoá trắng); trường khác gửi kèm bị bỏ qua. Trả hồ sơ mới (`phienBanHoSo` + 1) |
| `POST /api/catalog/bac-si/toi/anh` | `BAC_SI` | Multipart: `anh` (JPEG / PNG / WebP, tối đa 4 MB), `chuThich?`, `chuThichEn?` (tối đa 200 ký tự). Luôn là `ANH_CONG_VIEC`, thêm vào cuối. 201. Tối đa 12 ảnh / bác sĩ (409 `VUOT_SO_ANH_BAC_SI`) |
| `PUT /api/catalog/bac-si/toi/anh/{idAnh}` | `BAC_SI` | `{ chuThich?, chuThichEn? }`: chỉ đổi chú thích (bỏ trống = xoá). Không đổi thứ tự (quản trị viên sắp xếp) |
| `DELETE /api/catalog/bac-si/toi/anh/{idAnh}` | `BAC_SI` | Xoá ảnh làm việc của mình |

- **Ảnh chứng chỉ** (`loai = CHUNG_CHI`): bác sĩ sửa / xoá → 403 `KHONG_CO_QUYEN`; ảnh của bác sĩ khác → 404. Trang mẫu ghi
  "Quản trị viên quản lý" và ẩn nút.
- **Lưu đè.** Bác sĩ và quản trị viên cùng sửa được các mục trên: mỗi lần lưu gửi `phienBan` = `phienBanHoSo` đã tải; người
  kia đã lưu trước → 409 `DU_LIEU_DA_THAY_DOI`: tải lại hồ sơ rồi sửa lại. Thêm / sửa / xoá ảnh không cần `phienBan`.
- **Ngôn ngữ khám** là mã ISO 639 (2–3 chữ cái, vd `vi`, `en`, `fr`), tối đa 10, không phân biệt hoa thường, trùng thì gộp;
  API trả mảng mã viết thường. Frontend hiện tên ngôn ngữ theo ngôn ngữ giao diện (`Intl.DisplayNames`).
- **Token.** `GET /api/catalog/bac-si/**` là công khai nên frontend dễ bỏ token cho mọi GET dưới đó; riêng
  `/api/catalog/bac-si/toi/**` **phải** gửi token (không có → 401). Tài khoản bác sĩ bị vô hiệu hoá mà token còn hạn → 403.

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
| Thẻ bác sĩ | `GET /api/catalog/bac-si?idChuyenKhoa=` | `id`, `hoTen`, `anhDaiDien`, `hocVi`, `chucVu`, `soNamKinhNghiem` (suy ra từ năm bắt đầu hành nghề; null khi chưa có), `gioiThieuNgan`, `diemDanhGia` (null khi chưa có), `soDanhGia` |
| "Ngày sớm nhất còn chỗ" trên thẻ | `GET /api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa=` | `[{ idBacSi, ngay, soChoConLai }]`; ghép với thẻ theo `idBacSi`. Bác sĩ không còn chỗ thì không có dòng |
| Chi tiết bác sĩ (popup) | `GET /api/catalog/bac-si/{id}` | Thêm `tieuSu` (giới thiệu), `quaTrinhDaoTao[]`, `quaTrinhCongTac[]`, `linhVucKhamChua[]`, `giaiThuong[]`, `congTrinhNghienCuu[]` (mảng rỗng khi chưa có), `ngonNguKham[]` (mã ISO 639 viết thường, vd `["vi","en"]`: hiện tên ngôn ngữ, vd bằng `Intl.DisplayNames`), khối giấy phép hành nghề `soGiayPhep`, `ngayCapGiayPhep`, `noiCapGiayPhep`, `phamViHanhNghe` (công khai để người bệnh tra cứu; null khi chưa nhập), `anh[]` gồm `{ id, loai: ANH_CONG_VIEC \| CHUNG_CHI, url, chuThich }` theo đúng thứ tự hiển thị |
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
  khám xong, mới nhất trước. Mỗi dòng `{ lichHen, ketQua }`: `lichHen` như dòng "lịch hẹn của tôi", `ketQua { lyDoKham,
  sinhHieu { mach, nhietDo, huyetApTamThu, huyetApTamTruong, nhipTho, canNang, chieuCao }, khamLamSang, chanDoan,
  icdChinh { ma, ten, manTinh }, chanDoanKemTheo, ghiChu, ngayTaiKhamDeXuat, donThuoc[] { idThuoc, tenThuoc, hoatChat,
  hamLuong, duongDung, donVi, lieuDung, soLanMoiNgay, soNgayDung, soLuong, ghiChuSuDung }, ngaySuaCuoi }` (trường không
  có giá trị là `null`; `sinhHieu` null khi không đo chỉ số nào, `icdChinh` null khi bác sĩ không chọn mã; không kê đơn
  thì `donThuoc: []`). `ngayTaiKhamDeXuat` chỉ là gợi ý, không phải lịch hẹn. `ngaySuaCuoi` khác null: bác sĩ đã sửa kết quả
  sau khi lưu, hiện "Đã chỉnh sửa lúc <ngày giờ>"; bệnh nhân không thấy lý do hay nội dung cũ.
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
| Upcoming Appointments: danh sách | `GET /api/booking/bac-si/toi/lich-hen/lich?tuNgay=<hôm nay>&denNgay=<hôm nay + 41>` | Màn hình thay Appointment Requests (3 route cũ đã bỏ). Giữ các dòng `trangThai = DA_XAC_NHAN`, không `canDoiLich`, `gioKhamDuKien` sau bây giờ: đúng tập của `lichHenSapToi` trên Dashboard |
| Upcoming Appointments: Cancel | `POST /api/booking/bac-si/toi/lich-hen/{id}/huy` `{ lyDo }` | `lyDo` bắt buộc, tối đa 500 ký tự (thiếu → 400). 200, trả dòng lịch hẹn với `trangThai: BI_TU_CHOI`, `lyDoHuy`; lượt khám được mở lại cho người khác đặt; bệnh nhân nhận thông báo `LICH_HEN_BI_TU_CHOI` và email, đọc được lý do trên phiếu khám. Đã khám / đã hủy → 409 `LICH_HEN_KHONG_HUY_DOI_DUOC`; ca đã bị hủy (chờ bệnh nhân đổi) → 409 `LICH_HEN_CAN_DOI_LICH`; lượt khám đã bắt đầu → 409 `LICH_HEN_DA_QUA_GIO`; lịch của bác sĩ khác → 404 |
| Consultation: nhập mã / quét QR | `GET /api/booking/bac-si/toi/lich-hen/tra-cuu?ma=` | `ma` = `maTraCuu`, `maPhieuKham`, hoặc **cả chuỗi link** máy quét QR trả về (`.../phieu-kham/<mã>`). Lịch của bác sĩ khác → 404 |
| Hồ sơ khám của bệnh nhân | `GET /api/booking/bac-si/toi/lich-hen/{id}/ho-so-kham` (bấm vào 1 dòng / số thứ tự), `GET /api/booking/bac-si/toi/lich-hen/tra-cuu/ho-so-kham?ma=` (gõ mã / quét QR, `ma` như trên) | Cùng 1 kết quả: `lichHen` (như dòng danh sách), `hoSoBenhNhan { id, cccd, hoTen, ngaySinh, tuoi, gioiTinh, soDienThoai, diaChi, soBaoHiemYTe, tienSuBenhLy, daLienKetTaiKhoan }` (CCCD không che), `lanKhamTruoc[] { idLichHen, maTraCuu, ngay, gioKhamDuKien, bacSi, tenChuyenKhoa, trangThai, lyDoKham, ketQua }` (`ketQua` như ở lịch sử khám của bệnh nhân, mục 2) mới nhất trước, gồm cả lần khám với bác sĩ khác; `ketQua` null khi lần đó chưa có bệnh án. Ở hồ sơ khám (cả `ketQua` của lịch hẹn đang mở lẫn `lanKhamTruoc[].ketQua`) mỗi kết quả có thêm `phienBan`, `hanSua` (form sửa dùng `ketQua` của lịch hẹn đang mở). Lịch của bác sĩ khác → 404 |
| Đối chiếu giấy tờ | `PUT /api/booking/bac-si/toi/lich-hen/{id}/benh-nhan` `{ hoTen, ngaySinh, gioiTinh?, soDienThoai, diaChi?, soBaoHiemYTe?, cccd? }`, `POST .../lich-hen/{id}/da-doi-chieu` | Khi `lichHen.doiChieu.canDoiChieu`: bác sĩ sửa hồ sơ cho đúng giấy tờ (trả lại hồ sơ khám mới; `cccd` chỉ điền được khi hồ sơ chưa có số) rồi bấm "đã đối chiếu". Hai thao tác độc lập |
| Doctor Profile (xem và tự sửa 1 phần) | `GET` / `PUT /api/catalog/bac-si/toi/ho-so`, `POST /api/catalog/bac-si/toi/anh`, `PUT` / `DELETE .../toi/anh/{idAnh}`; tài khoản: `GET /api/users/me` | Mục "Hồ sơ bác sĩ: bác sĩ tự sửa". Trang công khai của bác sĩ: `GET /api/catalog/bac-si/{id}` |
| Consultation: ghi kết quả khám | `POST /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an` `{ lyDoKham?, sinhHieu?: { mach?, nhietDo?, huyetApTamThu?, huyetApTamTruong?, nhipTho?, canNang?, chieuCao? }, khamLamSang?, chanDoan, maIcdChinh?, chanDoanKemTheo?, ghiChu?, ngayTaiKhamDeXuat?, donThuoc?: [{ idThuoc, lieuDung?, soLanMoiNgay?, soNgayDung, soLuong, ghiChuSuDung? }] }` | 201; lịch hẹn chuyển sang `DA_HOAN_THANH` cùng lúc (hàng đợi hiện `DA_KHAM`). Chỉ với lịch hẹn của chính bác sĩ (khác → 404), còn `DA_XAC_NHAN` (khác → 409 `LICH_HEN_KHONG_KHAM_DUOC`), và từ ngày khám trở đi (sớm hơn → 409 `CHUA_DEN_NGAY_KHAM`). `chanDoan` bắt buộc, là chữ tự do kể cả khi có mã; `maIcdChinh` là `ma` lấy từ gợi ý ICD-10 (không có trong danh mục → 400); `chanDoanKemTheo` là chữ tự do, không có mã. `lyDoKham` (tối đa 2000 ký tự): màn hình khám điền sẵn lý do bệnh nhân ghi khi đặt. Sinh hiệu đều không bắt buộc: mạch 20-250 lần/phút, nhiệt độ 30-45 °C (1 chữ số thập phân), huyết áp tâm thu 50-300 và tâm trương 20-200 mmHg (gửi cả hai, tâm trương thấp hơn tâm thu), nhịp thở 5-80 lần/phút, cân nặng 0,3-400 kg (2 chữ số thập phân), chiều cao 20-250 cm (1 chữ số thập phân). Trẻ dưới 72 tháng tuổi (tính theo ngày khám) có kê thuốc thì bắt buộc cân nặng. Mỗi dòng thuốc: `soNgayDung` tối đa 30 ngày, 90 ngày khi mã ICD-10 chính là bệnh mạn tính (`manTinh`, Thông tư 26/2025); `soLuong` 1-9999 là tổng số lượng cấp theo đơn vị của thuốc (trang mẫu tự điền số lần/ngày × số ngày cho tới khi bác sĩ gõ vào ô). `ngayTaiKhamDeXuat` phải sau hôm nay; đơn thuốc tối đa 30 dòng, mỗi thuốc 1 dòng. `idThuoc` lấy từ gợi ý thuốc: bác sĩ không thêm thuốc mới (lỗi ở bảng "Thay đổi ngày 14/10/2026") |
| Consultation: xem / sửa kết quả đã ghi | `GET` / `PUT /api/medical/bac-si/toi/lich-hen/{idLichHen}/benh-an` | `PUT` như `POST` cộng `lyDoSua`, `phienBan` (bắt buộc), thay toàn bộ nội dung và đơn thuốc; lịch hẹn giữ `DA_HOAN_THANH`. Luật sửa và lịch sử sửa: mục "Sửa hồ sơ bệnh án và lịch sử sửa". Chưa có kết quả → 404. Kết quả cũng hiện ở hồ sơ khám (`ketQua`, và `lanKhamTruoc[].ketQua` kèm `donThuoc[]`) |
| Consultation: gợi ý mã ICD-10 | `GET /api/medical/bac-si/toi/icd10?tuKhoa=` | Tối đa 20 dòng `{ ma, ten, manTinh }` theo mã, khớp đầu mã (vd `J06`) hoặc một phần tên tiếng Việt (không xét dấu) / tiếng Anh; `ten` theo `Accept-Language`; từ khoá trống → `[]`. `manTinh: true` = bệnh thuộc Phụ lục VII Thông tư 26/2025: thuốc kê được tới 90 ngày. Danh mục 191 mã thường gặp ở phòng khám ngoại trú, chỉ đọc |
| Consultation: gợi ý thuốc | `GET /api/medical/bac-si/toi/thuoc?tuKhoa=` | Tối đa 20 dòng `{ id, tenThuoc, hoatChat, hamLuong, dangBaoChe, duongDung, donVi }`, khớp tên **hoặc hoạt chất**; từ khoá trống → `[]`. Chỉ thuốc đang dùng và đã xác minh; kê thuốc đã ngừng dùng ở đơn mới: 409 `THUOC_NGUNG_DUNG` |
| Dashboard: 4 ô số liệu | `GET /api/booking/bac-si/toi/tong-quan` | Mục "Dashboard" ở đầu tài liệu. Lịch mới là `DA_XAC_NHAN` ngay khi đặt; `lichHenSapToi` = số dòng của Upcoming Appointments |

## 4. Quản trị viên (vai trò `QUAN_TRI_VIEN`)

| Màn hình | API | Ghi chú |
|---|---|---|
| Schedule Management: xem (xếp / sửa / hủy ca và duyệt yêu cầu: bảng ở đầu tài liệu) | `GET /api/booking/quan-tri/lich-lam-viec?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=&idPhongKham=` | Tối đa 42 ngày, cùng dạng dòng ca như của bác sĩ, kèm `bacSi`, `tenChuyenKhoa` |
| Bệnh nhân của 1 ca | `GET /api/booking/quan-tri/lich-lam-viec/{id}/lich-hen` | |
| Tra lịch hẹn theo mã | `GET /api/booking/quan-tri/lich-hen/tra-cuu?ma=` | Mỗi lịch hẹn (cả ở danh sách của bác sĩ và của 1 ca) có `idHoSoBenhNhan`, `benhNhan` (dữ liệu của hồ sơ) và `doiChieu` `{ canDoiChieu, hoTenDaNhap, ngaySinhDaNhap, gioiTinhDaNhap, hoTenGiamHoDaNhap }` (những gì người đặt nhập). `canDoiChieu: true` → hiện cảnh báo "thông tin nhập khác hồ sơ, cần đối chiếu giấy tờ" |
| Đã đối chiếu giấy tờ | `POST /api/booking/quan-tri/lich-hen/{id}/da-doi-chieu` | Bỏ `canDoiChieu` của lịch hẹn; gọi lại vẫn 200 |
| Patient Management: xem / sửa hồ sơ | `GET /api/booking/quan-tri/ho-so-benh-nhan/{id}`, `PUT .../{id}` `{ hoTen, ngaySinh, gioiTinh?, soDienThoai, diaChi?, soBaoHiemYTe?, cccd? }` | `{id}` là `idHoSoBenhNhan` của lịch hẹn. Số CCCD không che. `cccd` chỉ điền được khi hồ sơ chưa có số: đổi số đã có → 400, số thuộc hồ sơ khác → 409 `XUNG_DOT_DU_LIEU`. Sửa hồ sơ không tự bỏ `canDoiChieu` của lịch hẹn |
| Doctor Management: hồ sơ giới thiệu | `GET /api/catalog/quan-tri/bac-si/{id}`, `PUT .../{id}/ho-so` | PUT thay toàn bộ (trường bỏ trống bị xoá trắng): `phienBan` (bắt buộc, = `phienBanHoSo` đã tải), `hocVi`, `chucVu(En)`, `namBatDauHanhNghe`, `ngayCapGiayPhep`, `noiCapGiayPhep(En)`, `phamViHanhNghe(En)`, `gioiThieuNgan(En)`, `tieuSu(En)`, `ngonNguKham[]`, `quaTrinhDaoTao[]`, `quaTrinhCongTac[]`, `linhVucKhamChua[]`, `giaiThuong[]`, `congTrinhNghienCuu[]` (mỗi danh sách có bản `...En`). Bác sĩ cũng tự sửa 1 phần hồ sơ này, nên lưu bằng phiên bản cũ → 409 `DU_LIEU_DA_THAY_DOI`: tải lại rồi lưu lại. GET có thêm `phienBanHoSo`, `namBatDauHanhNghe`, `soNamKinhNghiem` (suy ra) |
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

Từ 14/10/2026 dữ liệu mẫu có thêm 16 thuốc thường dùng đã xác minh (có hoạt chất, hàm lượng, dạng bào chế, đường dùng; vd
Paracetamol 500mg và Hapacol 250 cùng hoạt chất), và mỗi bác sĩ mẫu có giấy phép hành nghề (ngày cấp, nơi cấp, phạm vi),
năm bắt đầu hành nghề, ngôn ngữ khám (5 bác sĩ khám được tiếng Anh); bác sĩ 01, 03, 09 có giải thưởng / nghiên cứu. DB đã có dữ liệu mẫu
từ trước cũng được bổ sung ở lần khởi động tiếp theo với `SEED_DATA=true` (chỉ bác sĩ mẫu chưa có ngôn ngữ khám, thuốc
mẫu chưa có trong danh mục).

Từ 15/10/2026 mọi DB có danh mục 191 mã ICD-10 thường gặp (57 mã mạn tính theo Phụ lục VII Thông tư 26/2025), tạo bằng
migration V30 nên không phụ thuộc `SEED_DATA`. Muốn thấy `KHONG_DEN`: đặt 1 lịch, dời ngày khám của nó về hôm qua rồi khởi
động lại booking-service (hoặc chờ 00:01).
