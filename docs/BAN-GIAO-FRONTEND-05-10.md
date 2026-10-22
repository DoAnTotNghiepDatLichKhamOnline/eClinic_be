# Bàn giao cho frontend — đối chiếu với "Danh sách UI hiện đang có 05_10_2026"

> **Tài liệu cũ, chỉ để tra lịch sử thay đổi, không còn cập nhật.** Tài liệu API hiện hành: [api/](api/) — [bệnh nhân / khách](api/API-BENH-NHAN-KHACH.md), [bác sĩ](api/API-BAC-SI.md), [quản trị viên](api/API-QUAN-TRI.md).

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

**Thay đổi ngày 19/10/2026 (lịch làm việc).** Giới hạn 48 giờ / tuần cho mỗi bác sĩ, giờ làm theo tuần và lịch sử của
từng ca; sao chép ca sang khoảng khác; nhập / xuất Excel; bản in PDF và gửi lịch cho bác sĩ qua email; bác sĩ nhường ca
cho đồng nghiệp cùng chuyên khoa và xin nghỉ nhiều ca trong 1 lần. Route mới ở bảng
[Ca làm việc và yêu cầu đổi ca / xin nghỉ](#ca-làm-việc-và-yêu-cầu-đổi-ca--xin-nghỉ); phía bệnh nhân có ở tài liệu API bản
2.4 (`eClinic_API_XacThuc_TaiKhoan.docx`), route bản in PDF của bác sĩ ở tài liệu API thứ 2 bản 1.1
(`eClinic_API_LichHen_KhamBenh_BacSi_QuanTri.docx`, mục 5.5). Những chỗ frontend **phải** đổi:

| Chỗ | Trước | Bây giờ | Frontend cần làm |
|---|---|---|---|
| Dòng yêu cầu đổi ca / xin nghỉ (`YeuCauDoiLichResponse`, mọi danh sách yêu cầu) | `{ id, loaiYeuCau, trangThai, lyDo, ca, ngayMongMuon, gioBatDauMongMuon, gioKetThucMongMuon, phongKhamMongMuon, ghiChuXuLy, ngayGui, ngayXuLy }` | Thêm `bacSi` (bác sĩ gửi, sau `lyDo`), `bacSiNhan`, `ghiChuBacSiNhan`, `ngayBacSiNhanPhanHoi` (nhường ca), `maNhom` (xin nghỉ nhiều ca; null ở yêu cầu lẻ) trước `ghiChuXuLy` | Bảng của quản trị viên hiện người gửi → người nhận; gom các dòng cùng `maNhom` (nút duyệt / từ chối / rút cả nhóm) |
| `loaiYeuCau`, `trangThai` của yêu cầu | `XIN_NGHI`, `DOI_CA`; `CHO_DUYET`, `DA_DUYET`, `TU_CHOI`, `DA_RUT` | Thêm loại `NHUONG_CA`; thêm trạng thái `CHO_DONG_Y` (chờ đồng nghiệp đồng ý nhận) và `DONG_NGHIEP_TU_CHOI` | Nhãn cho 3 giá trị mới. Danh sách của quản trị viên mặc định `CHO_DUYET` nên không có yêu cầu `CHO_DONG_Y`; duyệt / từ chối yêu cầu đó → 409 `YEU_CAU_CHO_DONG_NGHIEP` |
| Gửi yêu cầu `POST /api/booking/bac-si/toi/yeu-cau-doi-lich` | `XIN_NGHI` / `DOI_CA` | Thêm `NHUONG_CA` kèm `idBacSiNhan`. 409 `CA_DA_CO_YEU_CAU_CHO_DUYET` nay tính cả yêu cầu nhường ca còn chờ đồng nghiệp (thông điệp "đang chờ xử lý") | Ô chọn đồng nghiệp khi chọn nhường ca (bảng ca làm việc bên dưới) |
| Xếp / sửa ca, duyệt đổi ca, duyệt nhường ca | Chỉ chặn trùng giờ | Thêm 409 `VUOT_GIO_LAM_TUAN`: tổng giờ của bác sĩ trong tuần thứ Hai - Chủ nhật vượt `SHIFT_MAX_WEEKLY_HOURS` (48); thông điệp nêu số giờ đã xếp, tuần, số giờ vượt. Xếp ca lặp lại: ngày vượt nằm trong `boQua[].lyDo` như ngày trùng | Hiện `thongDiep`; form xếp ca có thể hiện giờ đã xếp của tuần (`gio-lam-theo-tuan`) |
| Phiếu khám, dòng "lịch hẹn của tôi" | `canDoiLich` | Thêm `daDoiBacSi`: ca khám đã chuyển cho bác sĩ khác (`bacSi` là bác sĩ mới; giờ, phòng, mã không đổi). Như `canDoiLich`: `hanHuyDoi` = giờ khám và lần đổi không tính vào giới hạn 2 lần, nhưng lịch vẫn khám bình thường | Hiện "Đã đổi bác sĩ khám" cạnh tên bác sĩ |
| Chuông thông báo | | Loại mới `YEU_CAU_NHUONG_CA` (bác sĩ nhận ca: có yêu cầu mới, bị rút, được duyệt / từ chối, ca bị hủy; có `idYeuCau`) và `LICH_HEN_DOI_BAC_SI` (bệnh nhân của ca được nhường; có `idLichHen`, `maPhieuKham`) | `YEU_CAU_NHUONG_CA` mở Work Schedule; `LICH_HEN_DOI_BAC_SI` mở chi tiết lịch hẹn / phiếu khám |
| Tải tệp (Excel, PDF) | | Route tải về trả tệp (`Content-Disposition: attachment; filename=...`), lỗi vẫn là JSON. Gateway cho trình duyệt đọc header `Content-Disposition` (CORS) | Gọi bằng `fetch` kèm token, kiểm tra status rồi lưu `blob` với tên từ header |

**Thay đổi ngày 20/10/2026 (đặt lịch toàn viện).** Quy tắc đặt lịch do quản trị viên sửa; ngày bệnh viện nghỉ; quản trị
viên xem mọi lịch hẹn, hủy / đổi lịch thay bệnh nhân và đặt lịch qua tổng đài; sổ khám bệnh có link + mã QR riêng; cấp lại
link phiếu khám cho người đã mất; tệp lịch `.ics`; lọc bác sĩ theo ngày / buổi còn chỗ; báo cáo thống kê (report-service).
Route mới ở mục [Đặt lịch toàn viện](#đặt-lịch-toàn-viện-quy-tắc-ngày-nghỉ-lịch-hẹn-toàn-viện-tổng-đài-sổ-khám-cấp-lại-link-tệp-lịch-báo-cáo);
phía bệnh nhân / khách có ở tài liệu API bản 2.5 (`eClinic_API_XacThuc_TaiKhoan.docx`, mục 20), 8 route của quản trị viên
và bác sĩ trên lịch hẹn ở tài liệu API thứ 2 bản 1.2 (`eClinic_API_LichHen_KhamBenh_BacSi_QuanTri.docx`). Quy tắc, ngày
nghỉ và báo cáo chỉ có ở tài liệu này. Những chỗ frontend **phải** đổi:

| Chỗ | Trước | Bây giờ | Frontend cần làm |
|---|---|---|---|
| `trangThai` của lịch hẹn (phiếu khám, "lịch hẹn của tôi", dòng của bác sĩ / quản trị viên) | `DA_XAC_NHAN`, `BI_TU_CHOI`, `DA_HOAN_THANH`, `DA_HUY`, `DA_HUY_DO_DOI_LICH`, `KHONG_DEN` | Thêm `QUAN_TRI_HUY`: phòng khám (quản trị viên) đã hủy, lý do ở `lyDoHuy` | Nhãn "Phòng khám đã hủy" + lý do; tính là đã hủy (không hủy / đổi được nữa) |
| Dòng lịch hẹn của bác sĩ / quản trị viên (`LichHenTrongCaResponse`) | `... lyDoHuy, canDoiLich` | Thêm `kenhDat` (`KHACH` \| `TAI_KHOAN` \| `HOTLINE`) và `chuyenKhoa { id, ten }` (null ở danh sách của chính bác sĩ) | Cột / nhãn kênh đặt ở màn hình quản trị |
| Kết quả khám trả cho bệnh nhân (`ketQua` ở lịch sử khám, chi tiết lịch hẹn, trang cá nhân) và `BenhAnResponse` của bác sĩ | | Thêm `maSoKham`, `linkSoKham` (link = `FRONTEND_URL` + `/so-kham/` + mã). Không có ở phiếu khám, ở hồ sơ khám của bác sĩ (`ho-so-kham`) và ở mọi màn hình quản trị | Nút "Sổ khám bệnh" + mã QR (`GET /api/booking/so-kham/{ma}/qr`); trang `/so-kham/<mã>` của frontend (đường dẫn phải trùng `app.so-kham.duong-dan`) |
| Danh sách bác sĩ công khai `GET /api/catalog/bac-si` | `idChuyenKhoa`, `tuKhoa` | Thêm `ngay` (yyyy-MM-dd) và `buoi` (`SANG` \| `CHIEU` \| `TOI`): chỉ bác sĩ còn lượt đặt được lúc đó (buổi theo giờ bắt đầu lượt: sáng trước 12h, chiều 12h-17h, tối từ 17h), theo khoảng đặt trước của quy tắc hiện hành; ngày ngoài khoảng → trang rỗng; `buoi` sai → 400 | Ô lọc ngày / buổi ở bước chọn bác sĩ |
| Phiếu khám | | `GET /api/booking/phieu-kham/{ma}/lich.ics`: tệp lịch (`text/calendar`, `lich-kham-<ECL>.ics`) của trạng thái hiện tại; mã sai → 404 như phiếu | Nút "Thêm vào lịch" (link thường, không cần token) |
| Chuông thông báo của bệnh nhân | | Loại mới `LICH_HEN_QUAN_TRI_HUY` (quản trị viên hủy, có lý do) và `LICH_HEN_QUAN_TRI_DOI` (quản trị viên đổi lịch, gắn với lịch mới); có `idLichHen`, `maPhieuKham` | Mở chi tiết lịch hẹn / phiếu khám |
| Xếp / sao chép / nhập ca, duyệt đổi ca / nhường ca, bác sĩ gửi yêu cầu đổi ca | | Ngày bệnh viện nghỉ: 409 `NGAY_BENH_VIEN_NGHI` (thông điệp nêu ngày và lý do); xếp ca lặp lại, sao chép, nhập Excel thì ngày nghỉ vào `boQua[].lyDo` / lỗi theo dòng | Hiện `thongDiep` |
| Lỗi mới | | 403 `CCCD_KHONG_KHOP` (cấp lại link: CCCD xuất trình không phải của bệnh nhân / người giám hộ), 409 `KHONG_CO_EMAIL_NHAN` (cấp lại qua email mà lịch hẹn không có email), 409 `NGAY_BENH_VIEN_NGHI` | Hiện `thongDiep` |
| Quy tắc đặt lịch | Biến môi trường `BOOKING_*`, `SHIFT_REQUEST_LEAD`, `SHIFT_MAX_DAYS`, `SHIFT_MAX_WEEKLY_HOURS`, `RATING_EDIT_DAYS` | Chỉ là giá trị ban đầu của bảng `cau_hinh_he_thong` (lần đầu booking-service chạy); sau đó quản trị viên sửa ở `/api/booking/quan-tri/cau-hinh`, giá trị trong bảng thắng. Bệnh nhân đọc `GET /api/booking/quy-tac-dat-lich`. `BOOKING_IP_LIMIT` / `BOOKING_IP_WINDOW` vẫn là biến môi trường | Hiện quy tắc ở trang đặt lịch / phiếu khám từ API, không viết cứng "2 giờ", "30 ngày"... |

**Thay đổi ngày 21/10/2026 (giới hạn tần suất, nén gzip).** Gateway giới hạn số request của mỗi địa chỉ IP trong 1 phút
và nén response; một số dữ liệu công khai được cache trên Redis. Chi tiết và số liệu ở
[KIEN-TRUC-HIEU-NANG.md](KIEN-TRUC-HIEU-NANG.md). Những chỗ frontend **phải** đổi:

| Chỗ | Trước | Bây giờ | Frontend cần làm |
|---|---|---|---|
| Mọi API `/api/**` | | Quá giới hạn của 1 IP trong 1 phút → 429 `QUA_NHIEU_YEU_CAU`, `thongDiep` theo ngôn ngữ ("thử lại sau N giây"), header `Retry-After` = số giây (đọc được bằng JavaScript, đã mở qua CORS). Mặc định: 20 / phút cho POST đăng nhập, Google, đăng ký, quên mật khẩu, gửi lại email kích hoạt; 30 / phút cho POST chatbot; 300 / phút cho mọi request còn lại | Xử lý 429 ở 1 chỗ chung (interceptor): hiện `thongDiep`. **Không** tự gửi lại POST. GET được thử lại tối đa 1 lần sau `Retry-After` giây. Màn hình đăng nhập / đăng ký: khoá nút gửi và đếm ngược theo `Retry-After` |
| Các 429 cũ của từng chức năng (`GUI_LAI_QUA_NHANH`, `CHAT_QUA_NHIEU`...) | | Không đổi | Giữ cách xử lý hiện có; phân biệt bằng `maLoi` |
| Response JSON từ 2 KB | | Gateway nén gzip khi request có `Accept-Encoding: gzip` (trình duyệt luôn gửi) | Không cần làm gì: trình duyệt tự giải nén |
| Danh sách / chi tiết chuyên khoa, ngày bệnh viện nghỉ | | Đọc từ cache Redis; quản trị viên sửa thì cache bị xoá ngay sau khi lưu | Không cần làm gì: lần tải kế tiếp đã thấy dữ liệu mới |

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
| `/admin/appointments` | quản trị viên | (ngoài UI) lịch hẹn toàn viện: lọc, chi tiết + nhật ký, hủy / đổi lịch, cấp lại link | `qt-lich-hen.js` |
| `/admin/hotline-booking` | quản trị viên | (ngoài UI) đặt lịch qua tổng đài / tại quầy | `dat-lich.js` (cùng form với `/appointment`) |
| `/admin/booking-rules` | quản trị viên | (ngoài UI) quy tắc đặt lịch + lịch sử sửa | `qt-cau-hinh.js` |
| `/admin/closures` | quản trị viên | (ngoài UI) ngày bệnh viện nghỉ | `qt-ngay-nghi.js` |
| `/admin/reports` | quản trị viên | (ngoài UI) báo cáo thống kê, xuất Excel | `qt-bao-cao.js` |
| `/so-kham/<mã>` | công khai | (ngoài UI) sổ khám bệnh (link / QR của sổ khám) | `so-kham.js` |
| `/find-ticket` | công khai | (ngoài UI) khách mất link phiếu khám | `tim-lai-phieu.js` |

**Màn hình UI → API** (theo thứ tự của danh sách UI; cột cuối chỉ tới mục có chi tiết trong tài liệu này):

| Màn hình UI | API chính | Chi tiết |
|---|---|---|
| Login | `POST /api/auth/login`, `POST /api/auth/google`, `POST /api/auth/first-password` (bác sĩ mới) | README "Tài khoản và token"; mục "Doctor Management" (lần đăng nhập đầu) |
| Register, Forgot / Reset password | `POST /api/auth/register`, `/verify-email`, `/forgot-password`, `/reset-password` | README "Tài khoản và token" |
| Patient: Appointment | `GET /api/catalog/chuyen-khoa`, `GET /api/catalog/bac-si` (+ `ngay`, `buoi`), `GET /api/booking/khung-gio...`, `GET /api/booking/quy-tac-dat-lich`, `GET /api/booking/ngay-nghi`, `POST /api/booking/lich-hen` | Mục 2 "Appointment"; mục "Đặt lịch toàn viện" |
| Patient: Appointment Ticket | `GET /api/booking/phieu-kham/{ma}` (+ `/qr`, `/lich.ics`), `POST .../huy`, `POST .../doi-lich` | Mục 2 "Appointment Ticket"; bảng "Bệnh nhân hủy / đổi lịch" |
| (ngoài UI) lịch hẹn của tôi, kết quả khám, đánh giá | `GET /api/booking/lich-hen/cua-toi...`, `GET /api/booking/lich-su-kham/cua-toi`, `POST / PUT .../danh-gia` | Mục 2 "Lịch hẹn của tôi"; mục "Đánh giá lượt khám" |
| (ngoài UI) sổ khám bệnh, khách mất link | `GET /api/booking/so-kham/{ma}` (+ `POST .../xem`, `GET .../qr`), `POST /api/booking/cap-lai-lien-ket` | Mục "Đặt lịch toàn viện" |
| Doctor: Dashboard | `GET /api/booking/bac-si/toi/tong-quan`, `GET /api/booking/bac-si/toi/hang-doi?ngay=` (+ gọi khám / vắng mặt), `GET /api/booking/bac-si/toi/danh-gia` | Mục "Dashboard"; mục "Hàng đợi khám"; mục "Đánh giá lượt khám" |
| Doctor: Work Schedule | `GET /api/booking/bac-si/toi/lich-lam-viec` (+ `/pdf`), `GET .../lich-hen/lich`, `/api/booking/bac-si/toi/yeu-cau-doi-lich...` (gửi, xin nghỉ nhiều ca, rút), `/api/booking/bac-si/toi/yeu-cau-nhuong-ca...` (đồng nghiệp nhường ca cho tôi) | Mục 3; bảng "Ca làm việc và yêu cầu đổi ca / xin nghỉ" |
| Doctor: Upcoming Appointments (thay Appointment Requests) | `GET /api/booking/bac-si/toi/lich-hen/lich`, `POST /api/booking/bac-si/toi/lich-hen/{id}/huy`, `GET .../lich-hen/theo-cccd`, `POST .../lich-hen/{id}/cap-lai-lien-ket` | Mục 3; mục "Đặt lịch toàn viện" |
| Doctor: Consultation / EMR | `GET .../lich-hen/tra-cuu`, `GET .../{id}/ho-so-kham`, `GET` / `POST / PUT /api/medical/bac-si/toi/lich-hen/{id}/benh-an` (có `maSoKham`, `linkSoKham`), `GET .../benh-an/lich-su`, `GET /api/medical/bac-si/toi/thuoc`, `GET /api/medical/bac-si/toi/icd10` | Mục 3; mục "Sửa hồ sơ bệnh án và lịch sử sửa" |
| Doctor: Doctor Profile, menu tài khoản | `GET` / `PUT /api/catalog/bac-si/toi/ho-so`, `/api/catalog/bac-si/toi/anh...`, `GET /api/users/me`, `/api/users/me/...` | Mục "Hồ sơ bác sĩ: bác sĩ tự sửa"; README "Tài khoản của tôi" |
| Chuông thông báo (Doctor, Patient, Admin) | `GET /api/notification/thong-bao`, `GET .../so-chua-doc`, `POST .../{id}/da-doc` | README "Thông báo trong ứng dụng" |
| Admin: Dashboard | `GET /api/booking/quan-tri/tong-quan` | Mục "Dashboard" |
| Admin: Schedule Management | `/api/booking/quan-tri/lich-lam-viec...` (xếp / sửa / hủy, giờ làm theo tuần, lịch sử ca, sao chép, Excel, PDF, gửi lịch), `/api/booking/quan-tri/yeu-cau-doi-lich...` (duyệt / từ chối, cả nhóm) | Mục 4; bảng "Ca làm việc và yêu cầu đổi ca / xin nghỉ" |
| Admin: Medical Catalog | `/api/catalog/chuyen-khoa`, `/api/catalog/quan-tri/phong-kham...`, `/api/medical/quan-tri/thuoc...` | Mục "Patient Management và Medical Catalog" |
| Admin: Doctor Management | `/api/catalog/quan-tri/bac-si...`, `GET /api/booking/quan-tri/danh-gia?idBacSi=` | Mục "Doctor Management"; mục 4 (hồ sơ, ảnh giới thiệu) |
| Admin: Patient Management | `/api/booking/quan-tri/ho-so-benh-nhan...`, `GET /api/medical/quan-tri/lich-hen/{id}/benh-an/lich-su` | Mục "Patient Management và Medical Catalog"; mục "Sửa hồ sơ bệnh án và lịch sử sửa"; mục 4 |
| (ngoài UI) quản trị tài khoản | `/api/users...` | README "Tài khoản của tôi và quản trị tài khoản" |
| (ngoài UI) lịch hẹn toàn viện, đặt lịch qua tổng đài | `/api/booking/quan-tri/lich-hen...` (danh sách, chi tiết, hủy, đổi lịch, đặt lịch, cấp lại link) | Mục "Đặt lịch toàn viện"; tài liệu API thứ 2 bản 1.2 |
| (ngoài UI) quy tắc đặt lịch, ngày nghỉ, báo cáo | `/api/booking/quan-tri/cau-hinh...`, `/api/booking/quan-tri/ngay-nghi...`, `/api/report/quan-tri/thong-ke...` | Mục "Đặt lịch toàn viện" |

Mọi màn hình trong danh sách UI đều đã có API; riêng ô "System Uptime" của Admin Dashboard không được cung cấp.

**Vòng đời lịch hẹn:** đặt hoặc đổi lịch tạo lịch `DA_XAC_NHAN` ngay (phiếu khám dùng được ngay, không chờ bác sĩ). Bác sĩ
của lịch hẹn hủy được lịch trước giờ khám, kèm lý do (`BI_TU_CHOI`). Ghi kết quả khám chuyển lịch sang `DA_HOAN_THANH`. Lịch
chưa khám của ngày đã qua thành `KHONG_DEN` từ 00:01 ngày hôm sau, trừ người đã được gọi vào khám và lịch `canDoiLich`.
Phiếu khám (`GET /api/booking/phieu-kham/{ma}`), dòng "lịch hẹn của tôi" và dòng lịch hẹn của bác sĩ đều có `lyDoHuy`: lý
do bác sĩ hủy hoặc lý do bệnh nhân hủy, null ở các trạng thái khác — hiện cho bệnh nhân cạnh trạng thái. Từ 20/10 quản trị
viên cũng hủy được (`QUAN_TRI_HUY`, lý do bắt buộc, ở `lyDoHuy`) và đổi lịch thay bệnh nhân (lịch cũ `DA_HUY_DO_DOI_LICH`
như khi bệnh nhân đổi).

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
| `POST /api/booking/quan-tri/lich-lam-viec/{id}/huy` | `QUAN_TRI_VIEN` | `{ lyDo }` bắt buộc. Trả `{ ca, soLichHenCanDoi }`. Ca và mọi lượt khám thành `DA_HUY`; lịch hẹn còn hiệu lực của ca **giữ nguyên trạng thái** và có `canDoiLich = true`; bệnh nhân và bác sĩ được báo. Yêu cầu đang chờ của ca (`CHO_DUYET`, cả nhường ca còn `CHO_DONG_Y`) bị đóng (`TU_CHOI` kèm ghi chú); bác sĩ gửi và bác sĩ được nhường đều được báo |
| `GET /api/booking/quan-tri/lich-lam-viec/can-doi-lich?trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Lịch hẹn còn hiệu lực đang chờ bệnh nhân đổi lịch, giờ khám cũ sớm nhất trước (để phòng khám gọi điện cho khách đặt không đăng nhập) |
| `POST /api/booking/bac-si/toi/yeu-cau-doi-lich` | `BAC_SI` | `{ idLichLamViec, loaiYeuCau: XIN_NGHI \| DOI_CA \| NHUONG_CA, lyDo, ngayMongMuon?, gioBatDauMongMuon?, gioKetThucMongMuon?, idPhongKhamMongMuon?, idBacSiNhan? }` → 201. 3 trường ngày / giờ bắt buộc với `DOI_CA`. Chỉ ca còn hoạt động của chính bác sĩ (ca của người khác → 404), còn cách giờ bắt đầu ít nhất `SHIFT_REQUEST_LEAD` (24 giờ) → không thì 409 `QUA_HAN_GUI_YEU_CAU`. Mỗi ca 1 yêu cầu còn chờ (`CHO_DONG_Y` hoặc `CHO_DUYET`) → 409 `CA_DA_CO_YEU_CAU_CHO_DUYET`. **Nhường ca** (`NHUONG_CA`): `idBacSiNhan` là 1 bác sĩ khác, cùng chuyên khoa, đang công tác (không thì 400, thông điệp nêu điều này); bác sĩ nhận trùng giờ hoặc nhận thêm ca thì vượt giờ tuần → 409 `TRUNG_LICH_LAM_VIEC` / `VUOT_GIO_LAM_TUAN`. Yêu cầu thành `CHO_DONG_Y`, bác sĩ nhận được báo (`YEU_CAU_NHUONG_CA`); quản trị viên chỉ thấy và xử lý sau khi bác sĩ nhận đồng ý. Danh sách đồng nghiệp: `GET /api/catalog/bac-si?idChuyenKhoa=` (chuyên khoa lấy từ `GET /api/catalog/bac-si/toi/ho-so`) |
| `GET /api/booking/bac-si/toi/yeu-cau-doi-lich?trangThai=&trang=&kichThuoc=`, `POST .../yeu-cau-doi-lich/{id}/rut` | `BAC_SI` | Yêu cầu tôi đã gửi (mới nhất trước) và rút yêu cầu còn chờ, `CHO_DONG_Y` hoặc `CHO_DUYET` (`DA_RUT`; sau đó ca nhận được yêu cầu mới; rút nhường ca thì bác sĩ nhận được báo). Mỗi dòng: `{ id, loaiYeuCau, trangThai, lyDo, bacSi, ca, ngayMongMuon, gioBatDauMongMuon, gioKetThucMongMuon, phongKhamMongMuon, bacSiNhan, ghiChuBacSiNhan, ngayBacSiNhanPhanHoi, maNhom, ghiChuXuLy, ngayGui, ngayXuLy }`; `bacSi` là bác sĩ gửi, `ca` như 1 dòng lịch làm việc (có `soLuotDaDat`); `bacSiNhan`, `ghiChuBacSiNhan` (lý do đồng nghiệp không nhận), `ngayBacSiNhanPhanHoi` chỉ có ở nhường ca; `maNhom` chỉ có ở xin nghỉ nhiều ca |
| `GET /api/booking/quan-tri/yeu-cau-doi-lich?trangThai=&tatCa=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Yêu cầu của mọi bác sĩ, gửi sớm nhất trước; mặc định `CHO_DUYET` (nhường ca chỉ vào đây sau khi bác sĩ nhận đồng ý), `tatCa=true` lấy mọi trạng thái. Cùng dạng dòng như trên |
| `POST /api/booking/quan-tri/yeu-cau-doi-lich/{id}/duyet`, `POST .../{id}/tu-choi` | `QUAN_TRI_VIEN` | Body `{ ghiChu }`, bắt buộc khi từ chối. Duyệt `XIN_NGHI` = hủy ca. Duyệt `DOI_CA`: chỉ khác phòng thì đổi phòng tại chỗ; còn lại thì hủy ca cũ và xếp ca mới theo ngày / giờ / phòng mong muốn (giữ N, t). Duyệt `NHUONG_CA`: ca và mọi lịch hẹn còn hiệu lực của ca chuyển sang bác sĩ nhận (giờ, phòng, số thứ tự không đổi; lịch hẹn có `daDoiBacSi`, bệnh nhân nhận thông báo `LICH_HEN_DOI_BAC_SI` và email); lịch sử ca ghi `SUA` với `cacTruongDoi: ["bacSi"]`; cả 2 bác sĩ được báo. Ca mới / bác sĩ nhận trùng giờ hoặc vượt giờ tuần → 409 `TRUNG_LICH_LAM_VIEC` / `VUOT_GIO_LAM_TUAN`, yêu cầu vẫn chờ duyệt. Nhường ca còn `CHO_DONG_Y` → 409 `YEU_CAU_CHO_DONG_NGHIEP`. Đã xử lý / đã rút → 409 `YEU_CAU_DA_XU_LY` |
| `GET /api/booking/quan-tri/lich-lam-viec/gio-lam-theo-tuan?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=` | `QUAN_TRI_VIEN` | Giờ làm của từng bác sĩ theo tuần thứ Hai - Chủ nhật (khoảng tối đa 42 ngày, nới ra đủ tuần): `{ gioiHanPhut, bacSi: [{ bacSi, tenChuyenKhoa, cacTuan: [{ tuNgay, denNgay, soCa, soPhut, soPhutConLai }] }] }`, chỉ ca còn hoạt động. `soPhutConLai` âm = đã vượt. Form xếp ca gọi với `idBacSi` và tuần của ngày đã chọn để hiện giờ đã xếp |
| `GET /api/booking/quan-tri/lich-lam-viec/{id}/lich-su` | `QUAN_TRI_VIEN` | Lịch sử của 1 ca, cũ nhất trước: `[{ id, hanhDong, nguon, thoiDiem, quanTri { id, hoTen }, idYeuCau, lyDo, truoc, sau, cacTruongDoi }]`. `hanhDong`: `TAO`, `SUA`, `HUY`. `nguon`: `QUAN_TRI`, `DUYET_YEU_CAU` (có `idYeuCau`), `NGUNG_CONG_TAC`, `SAO_CHEP`, `NHAP_EXCEL`, `DU_LIEU_MAU`, `DU_LIEU_CU` (ca có từ trước khi có lịch sử: 1 dòng `TAO` tạo lúc nâng cấp). `truoc` / `sau` = `{ bacSi, phongKham, ngay, gioBatDau, gioKetThuc, soLuotToiDaMoiGio, thoiLuongLuotPhut }` (`truoc` null ở `TAO`); `cacTruongDoi` liệt kê tên trường đổi ở `SUA` |
| `POST /api/booking/quan-tri/lich-lam-viec/sao-chep/xem-truoc`, `POST /api/booking/quan-tri/lich-lam-viec/sao-chep` | `QUAN_TRI_VIEN` | Sao chép các ca còn hoạt động của khoảng nguồn sang khoảng khác: `{ tuNgay, denNgay, ngayDich, soLan, idChuyenKhoa?, cacIdBacSi? }`. Nguồn tối đa 42 ngày; `ngayDich` sau `denNgay` và cùng thứ với `tuNgay`; `soLan` 1-12, mỗi lần dời thêm số tuần đủ phủ khoảng nguồn; tối đa 1000 bản sao. Trả `{ daLuu, soCaNguon, taoDuoc: [ca], boQua: [ca] }`, mỗi `ca` = `{ idCaNguon, idCaMoi, bacSi, phongKham, ngay, gioBatDau, gioKetThuc, lyDo }`. Bản sao trùng giờ, vượt giờ tuần, ngày đã qua / quá `SHIFT_MAX_DAYS`, bác sĩ / phòng ngừng hoạt động thì vào `boQua` kèm `lyDo`. `xem-truoc` (200) chạy y hệt rồi hoàn tác (`daLuu: false`); `sao-chep` (201) lưu, mỗi bác sĩ 1 thông báo `CA_LAM_VIEC_THAY_DOI`, lịch sử `SAO_CHEP`. 400 khi: khác thứ, `ngayDich` không sau nguồn, nguồn không có ca, quá 1000 bản sao, hoặc lưu mà không bản sao nào tạo được |
| `GET /api/booking/quan-tri/lich-lam-viec/xuat-excel?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=&idPhongKham=`, `GET /api/booking/quan-tri/lich-lam-viec/nhap-excel/mau` | `QUAN_TRI_VIEN` | Tải `.xlsx`: ca còn hoạt động của khoảng (tối đa 42 ngày, cùng bộ lọc như lịch toàn viện), tên tệp `lich-lam-viec_<tuNgay>_<denNgay>.xlsx`; và tệp mẫu `mau-nhap-lich-lam-viec.xlsx` (trang ca, hướng dẫn, danh sách bác sĩ đang công tác và phòng đang hoạt động; cột mã bác sĩ / mã phòng có danh sách chọn). Cột A-G của cả hai: mã bác sĩ (`BS0001`), ngày, giờ bắt đầu, giờ kết thúc, mã phòng (id), N, t; cột sau G của tệp xuất chỉ để xem. Chữ theo `Accept-Language` |
| `POST /api/booking/quan-tri/lich-lam-viec/nhap-excel/xem-truoc`, `POST /api/booking/quan-tri/lich-lam-viec/nhap-excel` | `QUAN_TRI_VIEN` | Multipart, phần tệp tên `tep` (`.xlsx`, tối đa 2 MB → 413 `TEP_QUA_LON`; tối đa 1000 dòng ca). Trả `{ daLuu, soDong, hopLe: [{ dong, idCaMoi, bacSi, phongKham, ngay, gioBatDau, gioKetThuc }], loi: [{ dong, cot, lyDo }] }`. Ngày / giờ là ô ngày giờ của Excel hoặc chữ `yyyy-MM-dd` / `dd/MM/yyyy`, `HH:mm`. Lỗi theo dòng / cột: ô sai, mã bác sĩ / phòng không có hoặc ngừng hoạt động, phòng khác chuyên khoa, N x t, trùng giờ trong tệp hoặc với ca đã có, vượt giờ tuần, ngày đã qua / quá xa. `xem-truoc` (200) không lưu; `nhap-excel` (201) lưu tất cả hoặc không: còn lỗi → 400 (thông điệp nêu số lỗi), tệp hỏng / rỗng / quá 1000 dòng → 400. Mỗi bác sĩ 1 thông báo, lịch sử `NHAP_EXCEL`. Tệp xuất ra sửa rồi nhập lại được |
| `GET /api/booking/quan-tri/lich-lam-viec/pdf/bac-si/{idBacSi}?tuNgay=&denNgay=`, `GET /api/booking/quan-tri/lich-lam-viec/pdf/toan-vien?tuNgay=&denNgay=&idChuyenKhoa=` | `QUAN_TRI_VIEN` | Bản in PDF (khoảng tối đa 42 ngày, chỉ ca còn hoạt động, chữ theo `Accept-Language`): lịch 1 bác sĩ (A4 dọc, cùng tệp bác sĩ tự tải, tên `lich-lam-viec_<mã bác sĩ>_<tuNgay>_<denNgay>.pdf`); lưới toàn viện (A4 ngang, mỗi tuần thứ Hai - Chủ nhật 1 trang, bác sĩ theo chuyên khoa, cột cuối giờ làm trong tuần, tên `lich-lam-viec-toan-vien_<tuNgay>_<denNgay>.pdf`). Bác sĩ không có → 404 |
| `POST /api/booking/quan-tri/lich-lam-viec/gui-lich` | `QUAN_TRI_VIEN` | `{ tuNgay, denNgay, idChuyenKhoa?, cacIdBacSi? }` (khoảng tối đa 42 ngày, tối đa 200 bác sĩ). Mỗi bác sĩ có ca còn hoạt động từ hôm nay trong khoảng nhận 1 email liệt kê ca (tối đa 100 dòng, theo `ngonNgu` của tài khoản, nút mở `FRONTEND_URL` + `DOCTOR_SCHEDULE_PATH?tuNgay=&denNgay=`) và 1 thông báo `CA_LAM_VIEC_THAY_DOI`. Trả `{ soBacSi, soCa, cacBacSi: [{ bacSi, soCa, soPhut }] }`. Không có ca nào → 400. Gửi lại thì gửi thêm lần nữa |
| `GET /api/booking/bac-si/toi/lich-lam-viec/pdf?tuNgay=&denNgay=` | `BAC_SI` | Bản in PDF lịch của chính bác sĩ, như bản quản trị viên tải cho 1 bác sĩ (tài liệu API thứ 2, mục 5.5). Màn hình nhận `tuNgay` / `denNgay` từ đường dẫn của email gửi lịch thì điền sẵn 2 ngày này |
| `POST /api/booking/bac-si/toi/yeu-cau-doi-lich/xin-nghi-nhieu-ca` | `BAC_SI` | `{ cacIdLichLamViec: [id] (1-50), lyDo }` → 201, danh sách yêu cầu `XIN_NGHI` vừa tạo (mỗi ca 1 yêu cầu `CHO_DUYET`, chung `maNhom`), theo ngày giờ ca. Mỗi ca theo đúng luật của yêu cầu lẻ; 1 ca không gửi được thì không lưu gì và lỗi mang mã của ca đó, thông điệp "Ca <ngày giờ>: <lý do>". Quản trị viên nhận 1 thông báo cho cả nhóm |
| `POST /api/booking/bac-si/toi/yeu-cau-doi-lich/nhom/{maNhom}/rut` | `BAC_SI` | Rút mọi yêu cầu còn chờ của 1 nhóm của tôi; trả các yêu cầu vừa rút. Nhóm không có / của bác sĩ khác → 404; không còn yêu cầu chờ → 409 `YEU_CAU_DA_XU_LY` |
| `GET /api/booking/bac-si/toi/yeu-cau-nhuong-ca?trangThai=&trang=&kichThuoc=` | `BAC_SI` | Yêu cầu nhường ca gửi cho tôi, mới nhất trước (`trangThai=CHO_DONG_Y`: đang chờ tôi). Cùng dạng dòng như trên |
| `POST /api/booking/bac-si/toi/yeu-cau-nhuong-ca/{id}/dong-y`, `POST .../{id}/tu-choi` `{ ghiChu? }` | `BAC_SI` | Bác sĩ nhận đồng ý: yêu cầu thành `CHO_DUYET`, quản trị viên nhận `YEU_CAU_DOI_LICH_MOI`, bác sĩ gửi được báo; ca đã bắt đầu / đã hủy → 409 `CA_KHONG_SUA_DUOC`; tôi trùng giờ / vượt giờ tuần → 409 `TRUNG_LICH_LAM_VIEC` / `VUOT_GIO_LAM_TUAN`. Từ chối: `DONG_NGHIEP_TU_CHOI`, ghi chú (không bắt buộc) lưu ở `ghiChuBacSiNhan`, bác sĩ gửi được báo. Không phải yêu cầu gửi tôi → 404; không còn `CHO_DONG_Y` → 409 `YEU_CAU_DA_XU_LY` |
| `POST /api/booking/quan-tri/yeu-cau-doi-lich/nhom/{maNhom}/duyet`, `POST .../nhom/{maNhom}/tu-choi` | `QUAN_TRI_VIEN` | Body `{ ghiChu }`, bắt buộc khi từ chối. Xử lý mọi yêu cầu `CHO_DUYET` của nhóm, trả `{ daXuLy: [yêu cầu], boQua: [{ idYeuCau, lyDo }] }`; bác sĩ nhận 1 thông báo cho cả nhóm. Duyệt: ca đã bắt đầu thì bỏ qua (vào `boQua`); mọi ca đều bỏ qua → 409 `CA_KHONG_SUA_DUOC`. Nhóm không có → 404; không còn yêu cầu chờ → 409 `YEU_CAU_DA_XU_LY` |
| `GET /api/catalog/phong-kham?idChuyenKhoa=` | `QUAN_TRI_VIEN`, `BAC_SI` | Phòng khám đang hoạt động: `{ id, tenPhong, tang, idChuyenKhoa, tenChuyenKhoa }`. Chỉ đọc |

**Lịch hẹn của ca bị hủy (`canDoiLich`).** Phiếu khám, dòng "lịch hẹn của tôi" và dòng lịch hẹn của bác sĩ / quản trị viên có
`canDoiLich`. Khi `true`: lịch hẹn vẫn `DA_XAC_NHAN` nhưng ca đã hủy, bệnh nhân phải đổi sang khung giờ khác
hoặc hủy. Với lịch này `duocHuyDoi` đúng tới giờ khám cũ (`hanHuyDoi` = giờ khám, không trừ 2 giờ) và lần đổi không tính vào
giới hạn 2 lần; lịch mới sau khi đổi không còn cờ. Bác sĩ không thấy lịch này trong Upcoming Appointments và không hủy được
(409 `LICH_HEN_CAN_DOI_LICH`); lịch này cũng không bị chuyển thành `KHONG_DEN`.

**Lịch hẹn của ca được nhường (`daDoiBacSi`).** Phiếu khám và dòng "lịch hẹn của tôi" có `daDoiBacSi`. Khi `true`: quản
trị viên đã duyệt cho bác sĩ cũ nhường ca, `bacSi` là bác sĩ mới; giờ, phòng, số thứ tự và mã phiếu không đổi, lịch vẫn
`DA_XAC_NHAN` và khám bình thường (có trong Upcoming Appointments, hàng đợi của bác sĩ mới). Bệnh nhân không muốn khám với
bác sĩ mới thì hủy / đổi được tới giờ khám (`hanHuyDoi` = giờ khám) và lần đổi không tính vào giới hạn 2 lần; lịch mới sau
khi đổi không còn cờ.

**Giới hạn giờ làm trong tuần.** Tổng giờ các ca còn hoạt động của 1 bác sĩ trong 1 tuần thứ Hai - Chủ nhật không vượt
`SHIFT_MAX_WEEKLY_HOURS` (48; đúng 48 giờ vẫn được). Mọi cách tạo / đổi ca đều kiểm tra: xếp, sửa, duyệt đổi ca, duyệt và
đồng ý nhường ca (tính cho bác sĩ nhận), sao chép, nhập Excel.

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
| Ca khám được nhường cho bác sĩ khác (quản trị viên duyệt nhường ca) | `LICH_HEN_DOI_BAC_SI` | Như trên; kèm email "đổi bác sĩ khám" tới địa chỉ liên hệ của lịch hẹn |
| Quản trị viên xếp ca mới (1 thông báo cho 1 lần xếp), sửa hoặc hủy ca; sao chép, nhập Excel (1 thông báo cho mỗi bác sĩ); gửi lịch làm việc (kèm email) | `CA_LAM_VIEC_THAY_DOI` | Bác sĩ của ca |
| Bác sĩ gửi yêu cầu đổi ca / xin nghỉ (xin nghỉ nhiều ca: 1 thông báo cho cả nhóm); bác sĩ nhận đồng ý nhận ca được nhường | `YEU_CAU_DOI_LICH_MOI` (có `idYeuCau`) | Mọi quản trị viên đang hoạt động |
| Quản trị viên duyệt / từ chối yêu cầu (cả nhóm: 1 thông báo); bác sĩ nhận đồng ý / từ chối nhận ca; ca của yêu cầu bị quản trị viên hủy | `KET_QUA_DUYET_DOI_LICH` (có `idYeuCau`, câu thông báo có ghi chú) | Bác sĩ đã gửi |
| Bác sĩ khác nhường ca cho tôi; bác sĩ đó rút yêu cầu; quản trị viên duyệt / từ chối; ca bị hủy | `YEU_CAU_NHUONG_CA` (có `idYeuCau`) | Bác sĩ nhận ca |
| Quản trị viên hủy lịch hẹn (câu thông báo có lý do) | `LICH_HEN_QUAN_TRI_HUY` cho bệnh nhân, `LICH_HEN_DA_HUY` cho bác sĩ | Tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân; bác sĩ của lịch hẹn; kèm email `email.quanTriHuy` tới địa chỉ liên hệ |
| Quản trị viên đổi lịch thay bệnh nhân | `LICH_HEN_QUAN_TRI_DOI` (gắn với lịch mới) cho bệnh nhân; bác sĩ: `LICH_HEN_DA_DOI` (cùng bác sĩ) hoặc `LICH_HEN_DA_HUY` + `LICH_HEN_MOI` (đổi bác sĩ) | Như trên; kèm email đổi lịch theo ngôn ngữ của lịch cũ |
| Quản trị viên khai báo ngày bệnh viện nghỉ (hủy ca của ngày đó) | `LICH_HEN_CAN_DOI` cho bệnh nhân; 1 `CA_LAM_VIEC_THAY_DOI` cho mỗi bác sĩ (mọi ca bị hủy trong 1 câu) | Như "Ca khám bị hủy"; email `email.canDoiLich` tới địa chỉ liên hệ của mọi lịch hẹn, kể cả khách đặt |

- Không có đẩy realtime: gọi `so-chua-doc` khi vẽ trang và lặp lại khoảng 30 giây 1 lần; dừng khi tab bị ẩn
  (`document.hidden`), gọi ngay khi tab hiện lại. Thông báo xuất hiện chậm nhất khoảng 5 giây sau sự kiện, cộng chu kỳ hỏi.
- Bấm 1 thông báo: gọi `da-doc` rồi chuyển màn hình. Trang mẫu: bác sĩ sang Upcoming Appointments (`LICH_HEN_MOI`,
  `LICH_HEN_DA_DOI`) hoặc Work Schedule (mọi loại khác, kể cả `YEU_CAU_NHUONG_CA`); bệnh nhân sang chi tiết lịch hẹn theo
  `maPhieuKham` (kể cả `LICH_HEN_DOI_BAC_SI`).
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

### Đặt lịch toàn viện: quy tắc, ngày nghỉ, lịch hẹn toàn viện, tổng đài, sổ khám, cấp lại link, tệp lịch, báo cáo

Thêm ngày 20/10/2026. Màn hình mẫu: `/admin/appointments`, `/admin/hotline-booking`, `/admin/booking-rules`,
`/admin/closures`, `/admin/reports`, `/so-kham/<mã>`, `/find-ticket`; phần thêm vào `/appointment`, phiếu khám, lịch hẹn
của tôi, Upcoming Appointments và Consultation.

**Quy tắc đặt lịch (SCHED-05, quy tắc #13).** 10 quy tắc nằm ở 1 dòng của bảng `cau_hinh_he_thong`; lần đầu booking-service
chạy, dòng này lấy giá trị của biến môi trường (`BOOKING_*`, `SHIFT_REQUEST_LEAD`, `SHIFT_MAX_DAYS`, `SHIFT_MAX_WEEKLY_HOURS`,
`RATING_EDIT_DAYS`), sau đó giá trị trong bảng thắng (log WARN khi biến môi trường khác bảng). Giá trị mới áp dụng ngay cho
thao tác mới, không cần khởi động lại; lịch hẹn, ca, đánh giá đã có không đổi.

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/booking/quan-tri/cau-hinh` | `QUAN_TRI_VIEN` | `{ datTruocToiThieuPhut, soNgayDatTruocToiDa, soLichHieuLucToiDaMoiHoSo, soLichHieuLucToiDaMoiSoDienThoai, huyDoiTruocToiThieuPhut, soLanDoiLichToiDa, guiYeuCauTruocToiThieuGio, soNgayXepTruocToiDa, soGioToiDaMoiTuan, soNgayDuocSuaDanhGia, phienBan, ngaySua, quanTriSua { id, hoTen } }` |
| `PUT /api/booking/quan-tri/cau-hinh` | `QUAN_TRI_VIEN` | Cả 10 giá trị + `phienBan` vừa đọc. Khoảng cho phép: đặt trước 0-1440 phút; tối đa 1-180 ngày; mỗi hồ sơ 1-20 lịch; mỗi SĐT 1-50 lịch (không nhỏ hơn mỗi hồ sơ); hủy / đổi trước 0-10080 phút; đổi lịch 0-10 lần; gửi yêu cầu đổi ca trước 0-168 giờ; xếp ca trước 7-365 ngày; giờ làm tuần 1-168; sửa đánh giá 0-60 ngày. Sai → 400 có `chiTiet`; `phienBan` cũ (người khác vừa lưu) → 409 `DU_LIEU_DA_THAY_DOI`: tải lại rồi sửa lại. Không đổi gì thì phiên bản giữ nguyên |
| `GET /api/booking/quan-tri/cau-hinh/lich-su?trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Mới nhất trước, mỗi trường đổi giá trị 1 dòng `{ id, truong, giaTriCu, giaTriMoi, thoiDiem, quanTri }`; `truong` là tên trường ở trên |
| `GET /api/booking/quy-tac-dat-lich` | công khai | 5 quy tắc bệnh nhân cần biết: `{ datTruocToiThieuPhut, soNgayDatTruocToiDa, huyDoiTruocToiThieuPhut, soLanDoiLichToiDa, soLichHieuLucToiDaMoiHoSo }`. Dùng để hiện "đặt trước tối đa N ngày", "hủy / đổi trước giờ khám X phút" thay vì viết cứng |

`BOOKING_IP_LIMIT` / `BOOKING_IP_WINDOW`, nhắc lịch, đường dẫn frontend và `RECORD_EDIT_DAYS` vẫn là biến môi trường.

**Ngày bệnh viện nghỉ.** Mỗi ngày nghỉ 1 dòng, lý do tiếng Việt bắt buộc, tiếng Anh không bắt buộc (thiếu thì bản tiếng Anh
dùng câu tiếng Việt).

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/booking/ngay-nghi?tuNgay=&denNgay=` | công khai | `[{ ngay, lyDo }]` (lý do theo `Accept-Language`), mặc định từ hôm nay 1 năm, tối đa 366 ngày. Ngày nghỉ không có ca nào nên không có khung giờ để đặt; dùng để làm mờ ngày và hiện lý do trên lịch đặt khám |
| `GET /api/booking/quan-tri/ngay-nghi?tuNgay=&denNgay=` | `QUAN_TRI_VIEN` | `[{ id, ngay, lyDo, lyDoEn, quanTriTao { id, hoTen }, ngayTao }]`, cùng khoảng mặc định |
| `POST /api/booking/quan-tri/ngay-nghi/xem-truoc`, `POST /api/booking/quan-tri/ngay-nghi` | `QUAN_TRI_VIEN` | Body `{ tuNgay, denNgay, lyDo, lyDoEn? }`: từ ngày mai, tối đa 42 ngày. Trả `{ daLuu, ngayMoi[] (như dòng ở trên; `id` null ở xem trước), ngayDaNghi[] (ngày đã nghỉ từ trước, bỏ qua), caBiHuy[] { idLichLamViec, ngay, gioBatDau, gioKetThuc, bacSi, phongKham, soLichHenCanDoi }, soLichHenCanDoi }`. `xem-truoc` (200) chạy y hệt rồi hoàn tác, không báo ai; lưu (201) hủy các ca còn hoạt động của những ngày đó theo quy trình "cần đổi lịch" (`canDoiLich`, lịch sử ca nguồn `NGAY_NGHI`): bệnh nhân nhận `LICH_HEN_CAN_DOI` và email `email.canDoiLich` (cả khách đặt), mỗi bác sĩ 1 thông báo. Mọi ngày đều đã nghỉ → 409 `NGAY_BENH_VIEN_NGHI` |
| `DELETE /api/booking/quan-tri/ngay-nghi/{ngay}` | `QUAN_TRI_VIEN` | Bỏ 1 ngày nghỉ chưa tới (hôm nay / đã qua → 400). Ca đã hủy không được khôi phục; ngày đó xếp ca lại được |

Sau khi khai báo, mọi cách tạo ca vào ngày nghỉ bị từ chối: xếp ca (409 `NGAY_BENH_VIEN_NGHI`, xếp lặp lại thì ngày đó vào
`boQua`), sao chép và nhập Excel (vào `boQua` / lỗi theo dòng), duyệt đổi ca và bác sĩ gửi yêu cầu đổi ca sang ngày nghỉ (409).

**Lịch hẹn toàn viện (ADM-04) và đặt lịch qua tổng đài.** Phản hồi của các route quản trị không bao giờ có mã / link phiếu
khám, trừ phản hồi đặt lịch qua tổng đài và cấp lại link `MAN_HINH`. Nhật ký `lichSu` chỉ có thao tác từ ngày 20/10/2026
(không có dữ liệu cũ).

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/booking/quan-tri/lich-hen?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=&idPhongKham=&trangThai=&kenhDat=&canDoiLich=&canDoiChieu=&tuKhoa=&trang=&kichThuoc=` | `QUAN_TRI_VIEN` | Lịch hẹn có giờ khám trong khoảng (mặc định từ hôm nay 31 ngày, tối đa 366), sớm nhất trước, `TrangDuLieu` của dòng lịch hẹn (`LichHenTrongCaResponse`, có `kenhDat`, `chuyenKhoa`). `tuKhoa`: mã `ECL-...` đầy đủ, CCCD 12 số (người khám hoặc người giám hộ), hoặc 1 phần họ tên / SĐT / email. Chuyên khoa lọc theo chuyên khoa của bác sĩ |
| `GET /api/booking/quan-tri/lich-hen/{id}` | `QUAN_TRI_VIEN` | `{ lichHen, emailLienHe, taiKhoanDat { id, hoTen, email } (null với khách và tổng đài), ngonNgu, quanTriHuyDoiDuoc, chuoiDoiLich[] { id, maTraCuu, trangThai, gioKhamDuKien, laLichNay }, lichSu[] { thoiDiem, maTraCuu, suKien, nguoiThucHien { loai, idTaiKhoan, hoTen }, lyDo, maTraCuuLienQuan } }`. `suKien`: `DAT_LICH`, `DOI_LICH` (`maTraCuuLienQuan` = lịch mới), `HUY`, `CA_BI_HUY`, `DOI_BAC_SI`, `CA_THAY_DOI`, `CAP_LAI_LIEN_KET`; `loai`: `KHACH`, `TAI_KHOAN`, `BAC_SI`, `QUAN_TRI_VIEN`. Đã khám / không đến chỉ thấy ở `trangThai`. `quanTriHuyDoiDuoc` = được hủy / đổi lúc này (ẩn / hiện nút) |
| `POST /api/booking/quan-tri/lich-hen/{id}/huy` | `QUAN_TRI_VIEN` | `{ lyDo }` bắt buộc (tối đa 500). `DA_XAC_NHAN` → `QUAN_TRI_HUY`, lượt khám được mở lại; trả chi tiết như trên. Không theo hạn hủy của bệnh nhân. Đã quá giờ khám → 409 `LICH_HEN_DA_QUA_GIO` (trừ lịch `canDoiLich`); không còn `DA_XAC_NHAN` → 409 `LICH_HEN_KHONG_HUY_DOI_DUOC`. Bệnh nhân nhận `LICH_HEN_QUAN_TRI_HUY` + email `email.quanTriHuy` (ngôn ngữ của lịch hẹn), bác sĩ nhận `LICH_HEN_DA_HUY` |
| `POST /api/booking/quan-tri/lich-hen/{id}/doi-lich` | `QUAN_TRI_VIEN` | `{ idLichLamViec \| idChuyenKhoa, gioBatDauKhung, lyDo }` → 201, chi tiết của lịch **mới**. Không theo hạn đổi và số lần đổi của bệnh nhân; khung giờ mới theo quy tắc đặt lịch (còn chỗ, đặt trước, không trùng giờ...). Cùng điều kiện với hủy. Lịch cũ `DA_HUY_DO_DOI_LICH`; bệnh nhân nhận `LICH_HEN_QUAN_TRI_DOI` + email đổi lịch (ngôn ngữ của lịch cũ), bác sĩ được báo |
| `POST /api/booking/quan-tri/lich-hen` | `QUAN_TRI_VIEN` | Đặt lịch qua tổng đài / tại quầy: body như khách đặt (`idLichLamViec \| idChuyenKhoa, gioBatDauKhung, benhNhan, nguoiGiamHo?, lyDoKham`) cộng `ngonNgu` (`VI` mặc định / `EN`: ngôn ngữ của người gọi, dùng cho email và thông báo sau này). Cùng quy tắc với khách đặt (khung giờ, đặt trước, CCCD nhận và gắn cờ, người giám hộ, trùng giờ, giới hạn theo hồ sơ và SĐT), không giới hạn theo IP. 201 `{ idLichHen, phieuKham }` (`phieuKham` như kết quả đặt lịch, có `maPhieuKham`, `linkPhieuKham`): chỉ phản hồi này cho nhân viên thấy mã phiếu, để in phiếu / cho quét QR. Lịch không gắn tài khoản nào, `kenhDat = HOTLINE`, nhật ký ghi quản trị viên. Có email thì phiếu được gửi tới đó. Người gọi hủy / đổi bằng link phiếu + SĐT như khách |
| `POST /api/booking/quan-tri/lich-hen/{id}/cap-lai-lien-ket` | `QUAN_TRI_VIEN` | Cấp lại link (PAT-03), xem "Cấp lại link" bên dưới |

**Sổ khám bệnh (EXAM-03 / EXAM-04).** Mỗi lượt khám đã ghi kết quả có 1 mã sổ khám ngẫu nhiên, khác mã phiếu khám (quy tắc
#8). Link = `FRONTEND_URL` + `/so-kham/` + mã (`app.so-kham.duong-dan`); frontend phải có trang ở đúng đường dẫn đó vì mã QR
chứa link này. Link có ở `ketQua` trả cho bệnh nhân (lịch sử khám, chi tiết lịch hẹn, trang cá nhân) và ở `BenhAnResponse`
của bác sĩ (ghi / sửa / xem kết quả khám); không có ở phiếu khám, hồ sơ khám của bác sĩ và màn hình quản trị.

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/booking/so-kham/{maSoKham}` | công khai | Tóm tắt đã che, không có kết quả khám: `{ hoTenChe ("N. V. An"), namSinh, ngayKham, gioKham, bacSi, tenChuyenKhoa, phongKham }`. Mã sai → 404 |
| `POST /api/booking/so-kham/{maSoKham}/xem` | công khai | `{ bonSoCuoi }` = 4 số cuối SĐT liên hệ lúc đặt lịch (lượt khám có người giám hộ: SĐT người giám hộ). Đúng → `{ tomTat, benhNhan { hoTen, ngaySinh, gioiTinh, cccdChe }, hoTenNguoiGiamHo, ketQua }` (`ketQua` như ở lịch sử khám, theo `Accept-Language`). Sai → 403 `SO_DIEN_THOAI_KHONG_KHOP`; sai 5 lần → 429 `GUI_LAI_QUA_NHANH`, sổ khám bị khoá 15 phút kể cả khi nhập đúng (Redis lỗi thì bỏ qua giới hạn) |
| `GET /api/booking/so-kham/{maSoKham}/qr?kichThuoc=&taiVe=` | công khai | Ảnh PNG mã QR của link (100-1000 px, mặc định 300) |

**Cấp lại link phiếu khám / sổ khám (PAT-03).** Dùng lại mã cũ (không sinh mã mới), luôn cho lịch hẹn hiện hành của chuỗi đổi
lịch. Mỗi lần cấp lại ghi nhật ký `CAP_LAI_LIEN_KET` (ai làm, gửi cách nào).

| API | Ai gọi | Ghi chú |
|---|---|---|
| `POST /api/booking/quan-tri/lich-hen/{id}/cap-lai-lien-ket`, `POST /api/booking/bac-si/toi/lich-hen/{id}/cap-lai-lien-ket` | `QUAN_TRI_VIEN` (mọi lịch hẹn), `BAC_SI` (lịch của mình; khác → 404) | `{ cccd, cachGui: EMAIL \| MAN_HINH }`: `cccd` = số trên giấy tờ người đến xuất trình, của bệnh nhân hoặc người giám hộ trên lịch hẹn (sai → 403 `CCCD_KHONG_KHOP`). Trả `{ idLichHen, maTraCuu, cachGui, emailDaGui?, maPhieuKham?, lienKetPhieuKham?, maSoKham?, lienKetSoKham? }` (trường null bị bỏ). `EMAIL`: gửi `email.capLaiLienKet` tới email của lịch hẹn, `emailDaGui` đã che (`b***@domain`); không có email → 409 `KHONG_CO_EMAIL_NHAN`. `MAN_HINH`: trả mã + link (và sổ khám nếu đã khám) để mở QR bằng `/phieu-kham/{ma}/qr`, `/so-kham/{ma}/qr` — cách duy nhất cho lịch đặt qua tổng đài không có email. Nhân viên không bị giới hạn số lần |
| `GET /api/booking/bac-si/toi/lich-hen/theo-cccd?cccd=` | `BAC_SI` | Lịch hẹn của tôi (mọi trạng thái, tối đa 50, giờ khám muộn nhất trước) mà người khám hoặc người giám hộ có CCCD này: tìm lịch của người đến xin cấp lại. Quản trị viên tìm bằng `tuKhoa` của danh sách toàn viện |
| `POST /api/booking/cap-lai-lien-ket` | công khai | Khách tự xin: `{ maTraCuu (ECL-YYYYMMDD-NNNN), soDienThoai }` (đủ SĐT liên hệ lúc đặt). **Luôn 200 cùng 1 câu** dù mã có thật hay không, SĐT đúng hay sai, lịch có email hay không; link chỉ được gửi tới email của lịch hẹn, không bao giờ hiện trên màn hình. Mỗi mã 3 lần, mỗi IP 10 lần trong 1 giờ (`RELINK_CODE_LIMIT`, `RELINK_IP_LIMIT`, `RELINK_WINDOW`; đúng hay sai đều tính) → 429 `GUI_LAI_QUA_NHANH` kèm số phút còn lại. Giới hạn đã biết: nội dung phản hồi giống hệt nhau nhưng thời gian phản hồi không được làm đều |

**Tệp lịch `.ics` và lọc bác sĩ theo ngày / buổi.**

- `GET /api/booking/phieu-kham/{maPhieuKham}/lich.ics` (công khai): `text/calendar`, tệp `lich-kham-<ECL>.ics`, trạng thái
  hiện tại của lịch hẹn (giờ khám, bác sĩ, phòng, mã tra cứu, link phiếu; nhắc trước 1 giờ). Không có họ tên, SĐT, CCCD. Cả
  chuỗi đổi lịch là **1 sự kiện** (UID = mã tra cứu của lịch đầu chuỗi, SEQUENCE tăng theo mỗi lần đổi / đổi trạng thái), nên
  mở tệp mới hơn thì sự kiện được dời / hủy; phiếu cũ chỉ cho tệp "đã hủy", không lộ lịch mới. Mã sai → 404 như phiếu.
- Tệp `lich-kham.ics` đi kèm các email đặt lịch, đổi lịch, đổi bác sĩ, cần đổi lịch, bác sĩ hủy, quản trị viên hủy, cấp lại
  link; email nhắc lịch, tái khám, gửi lịch làm việc không kèm.
- Giới hạn đã biết: Google Calendar có thể thêm bản thứ 2 thay vì cập nhật sự kiện cũ; bệnh nhân tự hủy thì không có email
  (không có tệp "đã hủy" gửi tới).
- `GET /api/catalog/bac-si?idChuyenKhoa=&ngay=&buoi=`: `ngay` và / hoặc `buoi` (`SANG` trước 12h, `CHIEU` 12h-17h, `TOI` từ
  17h, theo giờ bắt đầu lượt khám) chỉ giữ bác sĩ còn ít nhất 1 lượt đặt được, theo khoảng đặt trước và thời gian đặt trước
  tối thiểu của quy tắc hiện hành. Ngày ngoài khoảng → trang rỗng; `buoi` sai → 400.

**Báo cáo thống kê (DASH-01..04, ADM-05).** report-service (cổng 8088, qua gateway `/api/report/**`), chỉ đọc DB chung, không
có bảng riêng.

| API | Ai gọi | Ghi chú |
|---|---|---|
| `GET /api/report/quan-tri/thong-ke?ky=&ngay=&tuNgay=&denNgay=&idChuyenKhoa=` | `QUAN_TRI_VIEN` | `ky`: `NGAY`, `TUAN` (thứ Hai - Chủ nhật), `THANG` (mặc định), `NAM` quanh `ngay` (mặc định hôm nay); `TUY_CHON` cần `tuNgay` + `denNgay`, tối đa 366 ngày (thiếu / sai → 400 `DU_LIEU_KHONG_HOP_LE`). `idChuyenKhoa` không có → 404. Trả `{ ky, tuNgay, denNgay, idChuyenKhoa, donViThoiGian, tongQuan, trangThai, kenhDat, thoiGianDatTruoc, theoChuyenKhoa[], theoBacSi[], theoThoiGian[], tinhLuc }` |
| `GET /api/report/quan-tri/thong-ke/xuat-excel` | `QUAN_TRI_VIEN` | Cùng tham số; tệp `bao-cao-thong-ke_<tuNgay>_<denNgay>.xlsx`, 5 trang (tổng quan, theo chuyên khoa, theo bác sĩ, theo thời gian, ghi chú cách tính), chữ theo `Accept-Language`. Lỗi vẫn là JSON |

Các phần của báo cáo:

- `tongQuan { bacSiDangCongTac, bacSiCoCa, taiKhoanBenhNhanMoi, benhNhanDaKham, luotKham, lichHen, luotDatMoi, tyLeHuy,
  tyLeKhongDen, luotDaDat, tongLuot, tyLeLapDay }`; `trangThai { chuaKham, daKham, khongDen, benhNhanHuy, bacSiHuy,
  quanTriHuy, canDoiLich }`; `kenhDat { khach, taiKhoan, hotline }`; `thoiGianDatTruoc { trungBinhGio, duoi1Ngay,
  tu1Den3Ngay, tu4Den7Ngay, tu8Den14Ngay, tren14Ngay }`.
- `theoChuyenKhoa[] { idChuyenKhoa, tenChuyenKhoa, chiSo }` (mọi chuyên khoa, kể cả số 0), `theoBacSi[] { idBacSi, maBacSi,
  hoTen, idChuyenKhoa, tenChuyenKhoa, chiSo }` (nhiều lịch hẹn nhất trước); `chiSo { lichHen, luotKham, benhNhanDaKham, daHuy,
  khongDen, tyLeHuy, tyLeKhongDen, luotDaDat, tongLuot, tyLeLapDay }`.
- `theoThoiGian[] { tuNgay, denNgay, lichHen, luotKham, daHuy, khongDen }`: mỗi điểm 1 ngày (kỳ ≤ 31 ngày), 1 tuần (≤ 184)
  hoặc 1 tháng (`donViThoiGian`), điểm đầu / cuối cắt theo kỳ, đủ mọi điểm kể cả số 0.

Cách tính (cũng ở trang "ghi chú" của tệp Excel):

- Lịch hẹn tính theo giờ khám trong kỳ, **không** tính lịch cũ của 1 lần đổi lịch (`DA_HUY_DO_DOI_LICH`); lọc chuyên khoa
  theo chuyên khoa của bác sĩ.
- Lượt khám = `DA_HOAN_THANH`; bệnh nhân đã khám = số hồ sơ bệnh nhân khác nhau trong các lượt đó (duy nhất theo CCCD).
- Tỷ lệ hủy = (bệnh nhân + bác sĩ + quản trị viên hủy) / lịch hẹn; tỷ lệ không đến = không đến / (đã khám + không đến); tỷ lệ
  lấp đầy = lượt đã đặt / tổng lượt của các ca còn hoạt động trong kỳ (ca bị hủy, kể cả do ngày nghỉ, không tính). Tỷ lệ là
  %, 1 chữ số thập phân, `null` khi mẫu số 0.
- Kênh đặt lấy từ `kenhDat`. Thời gian đặt trước = giờ bắt đầu lượt khám − lúc tạo lịch (lịch đổi tính từ lúc đổi).
- `luotDatMoi` = lịch hẹn được **tạo** trong kỳ, cùng quy tắc với ô "lượt đặt trong tháng" của Dashboard, nên báo cáo tháng
  này và Dashboard cho cùng số. `bacSiDangCongTac` là số hiện tại (như Dashboard), không theo kỳ.

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
| Quy tắc, ngày nghỉ (đầu trang) | `GET /api/booking/quy-tac-dat-lich`, `GET /api/booking/ngay-nghi` | Công khai. Hiện "đặt trước tối đa N ngày", hạn hủy / đổi; làm mờ ngày nghỉ kèm lý do (mục "Đặt lịch toàn viện") |
| Chuyên khoa | `GET /api/catalog/chuyen-khoa?kichThuoc=100` | Công khai |
| Thẻ bác sĩ | `GET /api/catalog/bac-si?idChuyenKhoa=` (+ `ngay`, `buoi` để chỉ lấy bác sĩ còn chỗ lúc đó) | `id`, `hoTen`, `anhDaiDien`, `hocVi`, `chucVu`, `soNamKinhNghiem` (suy ra từ năm bắt đầu hành nghề; null khi chưa có), `gioiThieuNgan`, `diemDanhGia` (null khi chưa có), `soDanhGia` |
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
  `LICH_HEN_TRUNG_GIO` (409), `VUOT_GIOI_HAN_DAT_LICH` (409: số lịch sắp tới tối đa của 1 hồ sơ / 1 số điện thoại theo quy
  tắc hiện hành, mặc định 3 / 5),
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
người giám hộ đã che thông tin, `canNguoiGiamHoDiCung`. Nút "Thêm vào lịch": `GET /api/booking/phieu-kham/{maPhieuKham}/lich.ics`
(công khai, mục "Đặt lịch toàn viện"). Mất link: trang "lấy lại link" gọi `POST /api/booking/cap-lai-lien-ket` (link chỉ
gửi tới email của lịch hẹn).

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
  hamLuong, duongDung, donVi, lieuDung, soLanMoiNgay, soNgayDung, soLuong, ghiChuSuDung }, ngaySuaCuoi, maSoKham,
  linkSoKham }` (`maSoKham` / `linkSoKham`: sổ khám bệnh của lượt khám, mục "Đặt lịch toàn viện"; trường không
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
| Work Schedule: tải bản in | `GET /api/booking/bac-si/toi/lich-lam-viec/pdf?tuNgay=&denNgay=` | Tệp PDF, tối đa 42 ngày (trang mẫu mặc định 4 tuần tới; mở từ email gửi lịch thì lấy `tuNgay` / `denNgay` trên đường dẫn). Bảng "Ca làm việc và yêu cầu đổi ca / xin nghỉ" |
| Work Schedule: yêu cầu đổi ca / xin nghỉ / nhường ca | `POST /api/booking/bac-si/toi/yeu-cau-doi-lich`, `POST .../xin-nghi-nhieu-ca`, `GET .../yeu-cau-doi-lich`, `POST .../{id}/rut`, `POST .../nhom/{maNhom}/rut` | Form gửi (chọn `NHUONG_CA` thì hiện ô chọn đồng nghiệp cùng chuyên khoa), form xin nghỉ nhiều ca (chọn các ca), danh sách "Yêu cầu đã gửi" (nút Rút, Rút cả nhóm). Bảng "Ca làm việc và yêu cầu đổi ca / xin nghỉ" |
| Work Schedule: yêu cầu nhường ca gửi tôi | `GET /api/booking/bac-si/toi/yeu-cau-nhuong-ca`, `POST .../{id}/dong-y`, `POST .../{id}/tu-choi` | Danh sách có nút "Đồng ý nhận" / "Từ chối" (ghi chú không bắt buộc) ở dòng `CHO_DONG_Y` |
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
| Consultation: link sổ khám bệnh | `BenhAnResponse` của `GET` / `POST` / `PUT /api/medical/bac-si/toi/lich-hen/{id}/benh-an` | Có `maSoKham`, `linkSoKham`: đưa link / mã QR (`GET /api/booking/so-kham/{ma}/qr`) cho bệnh nhân sau khi ghi kết quả. Hồ sơ khám (`ho-so-kham`) không có 2 trường này |
| Cấp lại link cho người mất (PAT-03) | `GET /api/booking/bac-si/toi/lich-hen/theo-cccd?cccd=`, `POST /api/booking/bac-si/toi/lich-hen/{id}/cap-lai-lien-ket` `{ cccd, cachGui }` | Tìm lịch của mình theo CCCD người đến xuất trình, rồi gửi lại link qua email của lịch hẹn hoặc mở mã QR trên màn hình (mục "Đặt lịch toàn viện"). Trang mẫu đặt ở Upcoming Appointments |

## 4. Quản trị viên (vai trò `QUAN_TRI_VIEN`)

| Màn hình | API | Ghi chú |
|---|---|---|
| Schedule Management: xem (xếp / sửa / hủy ca và duyệt yêu cầu: bảng ở đầu tài liệu) | `GET /api/booking/quan-tri/lich-lam-viec?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=&idPhongKham=` | Tối đa 42 ngày, cùng dạng dòng ca như của bác sĩ, kèm `bacSi`, `tenChuyenKhoa` |
| Schedule Management: giờ làm theo tuần, lịch sử ca | `GET .../lich-lam-viec/gio-lam-theo-tuan`, `GET .../lich-lam-viec/{id}/lich-su` | Bảng giờ làm của từng bác sĩ theo tuần (đánh dấu tuần vượt giới hạn); nút "Lịch sử" ở mỗi ca |
| Schedule Management: sao chép, Excel, bản in, gửi lịch | `POST .../sao-chep/xem-truoc`, `POST .../sao-chep`, `GET .../xuat-excel`, `GET .../nhap-excel/mau`, `POST .../nhap-excel/xem-truoc`, `POST .../nhap-excel`, `GET .../pdf/bac-si/{idBacSi}`, `GET .../pdf/toan-vien`, `POST .../gui-lich` | Sao chép và nhập Excel đều xem trước rồi mới lưu: nút lưu chỉ bật sau 1 lần xem trước thành công (nhập Excel: khi không còn lỗi) và tắt lại khi đổi ô nhập / chọn tệp khác. Gửi lịch nên hỏi xác nhận |
| Schedule Management: yêu cầu của bác sĩ | `GET /api/booking/quan-tri/yeu-cau-doi-lich`, `POST .../{id}/duyet`, `POST .../{id}/tu-choi`, `POST .../nhom/{maNhom}/duyet`, `POST .../nhom/{maNhom}/tu-choi` | Dòng nhường ca hiện "người gửi → `bacSiNhan`"; dòng có `maNhom` có thêm nút duyệt / từ chối cả nhóm, kết quả nêu số yêu cầu đã xử lý và các yêu cầu bỏ qua |
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
| (ngoài UI) Lịch hẹn toàn viện | `GET /api/booking/quan-tri/lich-hen`, `GET .../{id}`, `POST .../{id}/huy`, `POST .../{id}/doi-lich`, `POST .../{id}/cap-lai-lien-ket` | Lọc + phân trang, chi tiết có chuỗi đổi lịch và nhật ký, hủy / đổi lịch thay bệnh nhân (lý do bắt buộc), cấp lại link. Mục "Đặt lịch toàn viện" |
| (ngoài UI) Đặt lịch qua tổng đài | `POST /api/booking/quan-tri/lich-hen` | Form như khách đặt + ngôn ngữ của người gọi; in phiếu / cho quét QR từ phản hồi (lần duy nhất nhân viên thấy mã phiếu) |
| (ngoài UI) Quy tắc đặt lịch | `GET` / `PUT /api/booking/quan-tri/cau-hinh`, `GET .../cau-hinh/lich-su` | 10 quy tắc, `phienBan` chống ghi đè (409), lịch sử theo trường |
| (ngoài UI) Ngày bệnh viện nghỉ | `GET`, `POST .../xem-truoc`, `POST`, `DELETE .../{ngay}` của `/api/booking/quan-tri/ngay-nghi` | Xem trước các ca bị hủy rồi mới lưu |
| (ngoài UI) Báo cáo thống kê | `GET /api/report/quan-tri/thong-ke`, `GET .../xuat-excel` | Chọn kỳ + chuyên khoa; ô số liệu, biểu đồ theo chuyên khoa và theo thời gian, bảng bác sĩ, xuất Excel |

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

Từ 20/10/2026 (thử các màn hình "Đặt lịch toàn viện"):

- Dữ liệu mẫu có ca làm việc cho 14 ngày kể từ lần chạy `SEED_DATA` và chưa có lịch nào qua tổng đài: đăng nhập
  `admin@eclinic.local`, đặt 1 lịch ở `/admin/hotline-booking` (bỏ trống email để thấy cấp lại bằng `MAN_HINH`), rồi mở nó ở
  `/admin/appointments`.
- Báo cáo: chọn kỳ `THANG` hoặc `TUY_CHON` phủ những ngày đã đặt lịch thử để có số liệu; một năm chưa có lịch hẹn nào cho
  toàn số 0 và tỷ lệ `null`.
- Sổ khám bệnh: chỉ có sau khi bác sĩ ghi kết quả khám (ghi được từ ngày khám); bác sĩ mẫu `bacsi01` ghi rồi lấy
  `linkSoKham` ở Consultation, xem bằng 4 số cuối SĐT liên hệ của lịch hẹn.
- Ngày nghỉ: khai báo 1 ngày đã có ca mẫu để thấy bảng "ca sẽ bị hủy" ở bước xem trước.
