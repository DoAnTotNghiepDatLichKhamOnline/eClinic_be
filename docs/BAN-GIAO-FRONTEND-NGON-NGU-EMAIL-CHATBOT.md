# Bàn giao cho frontend — đổi ngôn ngữ EN / VI, email lịch hẹn, chatbot

Tài liệu này bổ sung cho [BAN-GIAO-FRONTEND-05-10.md](BAN-GIAO-FRONTEND-05-10.md): chỉ nói về 3 phần mới. Bản chạy được để
đối chiếu là trang demo (`node scripts/demo/server.js`, xem README mục "Email, đăng nhập Google và trang demo"): nút
**EN | VI** ở thanh trên và nút **Hỏi trợ lý** ở góc dưới phải có ở mọi màn hình. Mã nguồn tham khảo:
`scripts/demo/chung.js` (ngôn ngữ, khung chat), `scripts/demo/man-hinh/dat-lich.js` (form đặt lịch chọn sẵn từ thẻ chatbot).

Phần ngôn ngữ, email lịch hẹn và thông báo của **tài khoản và đặt lịch khám phía bệnh nhân** cũng đã vào tài liệu API
`eClinic_API_XacThuc_TaiKhoan.docx` bản 2.1 (mục 3.12, 3.13, route 6.4; mục 2 có cột "Thay đổi" ghi route mới / cập
nhật). Các route của chatbot cũng có ở đó (mục 19, mô hình 4.37 đến 4.41); cách trang demo vẽ khung chat và thẻ gợi
ý, và các màn hình quản trị, chỉ có ở tài liệu này.

Mọi đường dẫn API dưới đây đi qua gateway (`http://localhost:8080`). Phong bì phản hồi không đổi:
`{ thanhCong, maLoi, thongDiep, duLieu, chiTiet }`.

## 1. Ngôn ngữ

### 1.1 Quy tắc chung

| Việc | Cách làm |
|---|---|
| Chọn ngôn ngữ của 1 request | Header `Accept-Language: en` (hoặc `en-US`...) = tiếng Anh. Không gửi, hoặc ngôn ngữ khác = tiếng Việt như trước |
| Cái gì đổi theo header | `thongDiep` (lỗi, thành công), `chiTiet[].thongDiep` (lỗi từng trường), lỗi 401 / 403 và lỗi của gateway; nội dung danh mục (mục 1.3); câu thông báo trên chuông (mục 1.4); câu chào / câu cố định của chatbot |
| Cái gì KHÔNG đổi | `maLoi` (frontend rẽ nhánh theo mã này, không theo câu chữ), tên trường, giá trị enum, mã HTTP |
| Không bao giờ dịch | Họ tên người (bệnh nhân, bác sĩ, người giám hộ), học vị (`hocVi`), tên thuốc, mã tra cứu, và mọi chữ người dùng tự gõ: lý do khám, lý do hủy / từ chối, nhận xét đánh giá, chẩn đoán, lời dặn, ghi chú đơn thuốc, tin nhắn chat. Hiển thị nguyên văn ở cả 2 ngôn ngữ |
| Enum (`CHO_XAC_NHAN`, `NAM`, `XIN_NGHI`...) | API trả mã; nhãn hiển thị là việc của frontend. Bảng nhãn 2 ngôn ngữ của trang demo ở đầu `scripts/demo/chung.js` |

Frontend nên gửi `Accept-Language` ở **mọi** request (kể cả request công khai), và sau khi người dùng đổi ngôn ngữ thì tải
lại dữ liệu đang hiển thị: câu chữ từ API không tự đổi ở phía trình duyệt.

### 1.2 Ngôn ngữ của tài khoản

Tài khoản lưu ngôn ngữ ưa dùng `ngonNgu`: `VI` hoặc `EN`. Nó chỉ dùng cho những thứ gửi đi **khi không có request của
người dùng**: email và lời nhắc. Câu chữ của API vẫn theo header của từng request.

| API | Ghi chú |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/google` (tài khoản mới) | `ngonNgu` của tài khoản = `Accept-Language` của chính request đăng ký. Không có trường nào trong body |
| `POST /api/auth/login`, `POST /api/auth/refresh-token`, `GET /api/users/me` | `taiKhoan.ngonNgu` (trường mới, đứng sau `trangThai`) |
| `PUT /api/users/me/language` `{ "ngonNgu": "EN" }` | Mọi tài khoản đã đăng nhập. Đổi ngôn ngữ ưa dùng; 400 khi giá trị không phải `VI` / `EN` |
| Tài khoản bác sĩ do quản trị viên tạo | Bắt đầu là `VI` cho tới khi bác sĩ tự đổi |

Cách trang demo làm (đề xuất cho frontend): lựa chọn của nút EN | VI lưu ở `localStorage`. Sau đăng nhập: nếu trình duyệt
này đã từng bấm chọn thì lựa chọn đó thắng và được ghi vào tài khoản bằng `PUT /api/users/me/language`; nếu chưa thì
dùng `taiKhoan.ngonNgu`. Mỗi lần bấm nút khi đang đăng nhập cũng gọi `PUT`.

**Khách (không đăng nhập)** không có tài khoản để lưu: email xác nhận và lời nhắc của 1 lịch khách đặt dùng ngôn ngữ của
request `POST /api/booking/lich-hen` đã tạo lịch đó.

### 1.3 Nội dung danh mục

API **ngoài** `/quan-tri/` giữ nguyên hình dạng cũ; cùng trường đó nay mang chữ của ngôn ngữ được yêu cầu, và rơi về tiếng
Việt từng trường một khi chưa có bản tiếng Anh. Áp dụng cho: chuyên khoa (`tenChuyenKhoa`, `moTa`), phòng khám
(`tenPhong`, `tang`), bác sĩ (`chucVu`, `gioiThieuNgan`, `tieuSu`, `quaTrinhDaoTao`, `quaTrinhCongTac`,
`linhVucKhamChua`), chú thích ảnh (`chuThich`), thuốc (`donVi`, `moTa`), và mọi nơi các tên này xuất hiện (phiếu khám,
lịch hẹn, khung giờ, lịch làm việc). Tìm kiếm theo từ khoá khớp cả 2 ngôn ngữ.

API quản lý danh mục trả và nhận **cả 2 bản**: trường cũ = tiếng Việt, trường `…En` = tiếng Anh (`null` khi chưa nhập).
Body cũ không có `…En` vẫn chạy.

| Màn hình quản trị | API | Trường tiếng Anh thêm vào (đều không bắt buộc) |
|---|---|---|
| Medical Catalog: chuyên khoa | `GET /api/catalog/quan-tri/chuyen-khoa` (mới, đọc cả 2 bản), `POST /api/catalog/chuyen-khoa`, `PUT /api/catalog/chuyen-khoa/{id}` | `tenChuyenKhoaEn`, `moTaEn`, và cặp `trieuChung` / `trieuChungEn` (triệu chứng thường gặp, tối đa 2000 ký tự: chatbot dùng để gợi ý chuyên khoa, không hiện ở API công khai) |
| Medical Catalog: phòng khám | `/api/catalog/quan-tri/phong-kham` | `tenPhongEn`, `tangEn` |
| Medical Catalog: thuốc | `/api/medical/quan-tri/thuoc` | `donViEn`, `moTaEn` (tên thuốc không có bản tiếng Anh) |
| Doctor Management: hồ sơ bác sĩ | `/api/catalog/quan-tri/bac-si` | `chucVuEn`, `gioiThieuNganEn`, `tieuSuEn`, `quaTrinhDaoTaoEn`, `quaTrinhCongTacEn`, `linhVucKhamChuaEn` |
| Doctor Management: ảnh giới thiệu | thêm / sửa ảnh | `chuThichEn` |

Tên tiếng Anh của chuyên khoa và phòng khám cũng không được trùng nhau (409 như tên tiếng Việt).

### 1.4 Chuông thông báo

`GET /api/notification/thong-bao`: `noiDung` nay được soạn **theo `Accept-Language` của request đọc** (đổi ngôn ngữ rồi tải
lại danh sách là câu đổi theo), không còn là câu tiếng Việt cố định. Tên người, mã tra cứu và lý do người dùng gõ nằm
trong câu vẫn giữ nguyên văn. Thông báo tạo trước khi có tính năng này chỉ có câu tiếng Việt.

2 loại (`loai`) mới của bệnh nhân:

| `loai` | Khi nào | Bấm vào thì mở |
|---|---|---|
| `NHAC_LICH_KHAM` | Trước giờ khám 3 ngày và 1 ngày (mục 2) | Chi tiết lịch hẹn / phiếu khám của `maPhieuKham` |
| `NHAC_TAI_KHAM` | Bác sĩ hẹn hoặc đổi ngày tái khám; và 7 ngày trước ngày đó | **Trang đặt lịch**. `idLichHen` / `maPhieuKham` của thông báo là của lượt khám ĐÃ XONG, chưa có lịch hẹn mới nào |

## 2. Email lịch hẹn

Frontend không gọi API nào để gửi email; phần này để biết người dùng nhận gì và liên kết trong email mở trang nào.

| Email | Khi nào gửi | Nút trong email mở |
|---|---|---|
| Đã nhận yêu cầu đặt lịch | Ngay sau `POST /api/booking/lich-hen` (khách hoặc tài khoản). Nói rõ lịch đang **chờ bác sĩ xác nhận** | Phiếu khám: `linkPhieuKham` |
| Đã đổi lịch | Sau khi bệnh nhân đổi lịch: 1 email cho lịch mới (mã mới) | Phiếu khám mới |
| Nhắc lịch khám | Trước giờ khám 3 ngày và 1 ngày, chỉ gửi trong 07:00 đến 21:00. Lịch chưa được xác nhận vẫn nhắc, với câu nói rõ điều đó. Lịch đã hủy, bị từ chối, đang chờ đổi (`canDoiLich`) không nhắc; đặt sát giờ thì bỏ mốc đã qua | Phiếu khám |
| Bác sĩ hẹn tái khám / đổi ngày tái khám | Trong vòng 5 phút sau khi bác sĩ ghi hoặc sửa `ngayTaiKhamDeXuat` (xoá ngày thì không gửi gì) | Trang đặt lịch |
| Nhắc tái khám | 7 ngày trước ngày tái khám; bỏ qua khi bệnh nhân đã có lịch hẹn sắp tới cùng chuyên khoa | Trang đặt lịch |

- **Gửi cho ai**: email liên hệ nhập lúc đặt lịch (`benhNhan.email`); không nhập thì email của tài khoản đã đặt. Khách không
  nhập email thì không nhận gì: form đặt lịch nên nói rõ điều này cạnh ô email.
- **Ngôn ngữ**: của tài khoản đã đặt (`ngonNgu`); với khách là ngôn ngữ của request đặt lịch.
- **Nội dung**: họ tên bệnh nhân, ngày giờ, bác sĩ, phòng và tầng, mã tra cứu, nút mở phiếu khám. Không có lý do khám, chẩn
  đoán, số điện thoại hay CCCD. Email tái khám nêu bệnh nhân, bác sĩ, ngày tái khám và nói rõ **chưa có lịch hẹn nào được
  đặt**.
- **Liên kết**: phiếu khám là `linkPhieuKham` như API trả (trang `/phieu-kham/<mã>` của frontend). Nút "đặt lịch tái khám"
  mở `FRONTEND_URL` + `FOLLOWUP_BOOKING_PATH` (mặc định `/dat-lich`; trang demo chuyển hướng sang `/appointment`).
- 9 email tài khoản có từ trước (kích hoạt, đặt lại mật khẩu, đổi email...) nay gửi theo `ngonNgu` của tài khoản; đường
  dẫn trong các email đó không đổi.

## 3. Chatbot

### 3.1 API

| API | Ai gọi | Ghi chú |
|---|---|---|
| `POST /api/chatbot/phien` | Khách, mọi tài khoản | 201 `{ idPhien, maKhach, thoiGianBatDau, tinNhan[] }`; `tinNhan` có sẵn câu chào. `maKhach` chỉ có khi **không gửi JWT** và chỉ trả 1 lần |
| `POST /api/chatbot/phien/{idPhien}/tin-nhan` `{ noiDung }` | Chủ phiên | 1 đến 1000 ký tự -> `{ tinNhan, traLoi }`. Không stream; thường vài giây, tới khoảng 25 giây khi có tra cứu: cần trạng thái "đang trả lời" và khoá nút gửi |
| `GET /api/chatbot/phien/{idPhien}/tin-nhan` | Chủ phiên | Cả hội thoại, cũ nhất trước (dùng khi tải lại trang) |
| `GET /api/chatbot/phien` | Tài khoản đã đăng nhập | `[{ idPhien, thoiGianBatDau, daDongYDuLieu }]`, mới nhất trước, tối đa 50 |
| `POST /api/chatbot/phien/{idPhien}/dong-y` | `BENH_NHAN`, phiên của chính tài khoản | Đồng ý cho chatbot đọc lịch hẹn của mình trong phiên này |
| `DELETE /api/chatbot/phien/{idPhien}/dong-y` | như trên | Rút lại đồng ý |

- **Chủ phiên**: phiên mở khi có JWT thuộc về tài khoản đó; phiên mở khi không có JWT thuộc về người cầm `maKhach`, gửi ở
  header **`X-Chat-Khach`** trong mọi request sau. Phiên không có và phiên của người khác cùng trả 404 `KHONG_TIM_THAY`.
- 1 tin nhắn: `{ id, nguoiGui, loai, noiDung, ngayTao, goiY }`. `nguoiGui`: `NGUOI_DUNG` / `CHATBOT`. `noiDung` là văn bản
  thường (không markdown, không HTML): hiển thị bằng text, giữ xuống dòng. Nội dung đã lưu không đổi theo ngôn ngữ khi đọc lại.

| `loai` | Ý nghĩa | Frontend làm gì |
|---|---|---|
| `CAU_HOI` | Tin của người dùng | Bong bóng bên phải |
| `LOI_CHAO` | Câu chào: nói rõ đây là AI, không thay bác sĩ | Hiện đầu hội thoại |
| `TRA_LOI` | Câu trả lời của mô hình, có thể kèm `goiY` | Hiện câu + vẽ thẻ từ `goiY` (mục 3.3) |
| `NGOAI_PHAM_VI` | Câu từ chối cố định (hỏi việc không liên quan) | Hiện như câu trả lời |
| `KHAN_CAP` | Tin có dấu hiệu cấp cứu: câu "gọi 115" cố định, không qua mô hình | Làm nổi bật (trang demo: nền đỏ nhạt) |
| `CAN_DONG_Y` | Xin bệnh nhân đồng ý trước khi đọc lịch hẹn của họ | Hiện câu + nút Đồng ý (mục 3.4) |

### 3.2 Phiên chat và đăng nhập

- Lưu `idPhien` (và `maKhach` nếu có) ở `sessionStorage` để tải lại trang vẫn còn hội thoại.
- **Khi tài khoản đang dùng trang thay đổi (đăng nhập, đăng xuất, đổi tài khoản) thì bỏ phiên cũ và mở phiên mới.** Phiên
  của khách không bao giờ tra cứu được lịch hẹn cá nhân, kể cả sau khi người đó đăng nhập, và phiên của tài khoản thì
  khách không đọc được.
- Mọi vai trò đều chat được; chỉ `BENH_NHAN` có phần "lịch hẹn của tôi".

### 3.3 Thẻ gợi ý `goiY`

`goiY` do **server dựng từ dữ liệu phòng khám**, không lấy từ câu chữ của mô hình: bác sĩ, phòng, giờ khám để đặt lịch chỉ
lấy từ đây, không lấy từ `noiDung`. `null` khi câu trả lời không tra cứu gì. Là ảnh chụp lúc trả lời (đọc lại phiên vẫn
như cũ), nên form đặt lịch phải đọc lại số chỗ.

| Trường | Hình dạng | Thẻ trên trang demo |
|---|---|---|
| `chuyenKhoa` | `{ id, ten }` hoặc `null` | Tên chuyên khoa + "Đặt lịch chuyên khoa này" |
| `bacSi[]` | `{ id, hoTen, hocVi, chucVu, anhDaiDien, ngaySomNhat, soChoConLai }`, tối đa 6; `ngaySomNhat` `null` = hết chỗ | Tên, chức vụ, ngày sớm nhất còn chỗ + "Đặt lịch với bác sĩ này" |
| `lichKham[]` | `{ idBacSi, hoTenBacSi, idLichLamViec, ngay, tenPhong, tang, khungGio: [{ gioBatDau, gioKetThuc, soChoConLai }] }`: các ca còn chỗ của 1 bác sĩ trong 7 ngày | Bác sĩ, ngày, phòng + mỗi khung giờ là 1 liên kết |
| `lichHenCuaToi[]` | `{ maPhieuKham, maTraCuu, linkPhieuKham, trangThai, ngay, gioBatDauKhung, gioKetThucKhung, hoTenBacSi, tenChuyenKhoa, tenPhong, tang, hoTenBenhNhan, laBanThan, canDoiLich }`, tối đa 10 | Ngày giờ, bác sĩ, trạng thái, mã tra cứu + "Mở phiếu khám" (`/phieu-kham/<maPhieuKham>`) |
| `lanKham[]` | `{ maPhieuKham, maTraCuu, ngay, hoTenBacSi, tenChuyenKhoa, hoTenBenhNhan, laBanThan, ngayTaiKhamDeXuat }`, tối đa 10 | Ngày khám, bác sĩ, ngày tái khám + liên kết sang lịch sử khám |

`lichHenCuaToi` / `lanKham`: `null` = câu trả lời không tra cứu phần đó; mảng rỗng = đã tra cứu, không có gì. `bacSi` /
`lichKham`: mảng rỗng = chưa tra cứu.

**Mở form đặt lịch từ thẻ.** Trang demo dùng đường dẫn
`/appointment?idChuyenKhoa=&idBacSi=&ngay=&idLichLamViec=&gioBatDauKhung=` (thiếu tham số nào thì form dừng ở bước đó):

1. `idChuyenKhoa` = `goiY.chuyenKhoa.id`. Thẻ lịch khám có thể không kèm chuyên khoa: lấy `idChuyenKhoa` từ
   `GET /api/catalog/bac-si/{idBacSi}`.
2. `idBacSi` -> chọn bác sĩ; `ngay` (ngày của ca) -> tải lịch bằng
   `GET /api/booking/khung-gio/nhieu-ngay?idBacSi=&tuNgay=<ngay>` vì chatbot có thể gợi ý ca xa hơn 7 ngày tới.
3. `idLichLamViec` + `gioBatDauKhung` (= `khungGio[].gioBatDau`) -> chọn sẵn khung giờ nếu ca còn trong kết quả và còn
   chỗ. Giá trị lạ hoặc đã cũ thì bỏ qua, không báo lỗi; người dùng chọn lại.

Đặt lịch vẫn là `POST /api/booking/lich-hen` với `idLichLamViec` + `gioBatDauKhung` như form thường: chatbot không đặt lịch.

### 3.4 Lịch hẹn của chính người hỏi (đồng ý theo phiên)

1. Bệnh nhân đã đăng nhập hỏi về lịch hẹn / lượt khám / ngày tái khám của mình. Lần đầu trong phiên, `traLoi.loai` là
   `CAN_DONG_Y`: câu cố định nói rõ cái gì sẽ được gửi cho dịch vụ AI bên ngoài. Chưa có gì được đọc.
2. Frontend hiện nút **Đồng ý** dưới câu đó. Bấm -> `POST .../dong-y` -> **gửi lại câu hỏi vừa rồi** bằng
   `POST .../tin-nhan` (server không tự trả lời lại). Câu hỏi nằm ngay trước tin `CAN_DONG_Y` trong hội thoại.
3. Câu trả lời sau đó có `goiY.lichHenCuaToi` / `goiY.lanKham`. Đồng ý có hiệu lực cho cả phiên đó.
4. Hiện chỗ **rút lại đồng ý** khi phiên đã đồng ý (`daDongYDuLieu` ở `GET /api/chatbot/phien`): `DELETE .../dong-y`. Sau
   đó hỏi lại sẽ nhận `CAN_DONG_Y`.

Chỉ ngày giờ, bác sĩ, chuyên khoa, phòng, tầng, trạng thái, "của mình hay đặt cho người khác" và ngày tái khám được gửi
cho mô hình. Họ tên, mã tra cứu, lý do khám, chẩn đoán, đơn thuốc không bao giờ được gửi: chúng chỉ có trên thẻ (do
server dựng) và ở các màn hình lịch hẹn / lịch sử khám. Không có cách nào hỏi về người khác. Khách, bác sĩ, quản trị viên
và phiên của khách không có phần này: chatbot trả lời như câu hỏi thường.

### 3.5 Lỗi và giới hạn

| Mã | `maLoi` | Khi nào | Frontend làm gì |
|---|---|---|---|
| 400 | `DU_LIEU_KHONG_HOP_LE` | Tin rỗng hoặc quá 1000 ký tự | Chặn ở ô nhập (`maxlength="1000"`) |
| 404 | `KHONG_TIM_THAY` | Phiên không có / không phải của người gọi | Bỏ phiên đã lưu, mở phiên mới ở lần gửi sau |
| 429 | `CHAT_QUA_NHIEU` | Mặc định 20 tin / 10 phút và 100 tin / ngày cho mỗi tài khoản hoặc mỗi IP của khách; 10 phiên mới / giờ; trần chung cả hệ thống | Hiện `thongDiep`, giữ nguyên chữ đang gõ |
| 503 | `CHATBOT_KHONG_KHA_DUNG` | Chưa cấu hình khoá mô hình, hoặc mô hình không trả lời kịp | Hiện `thongDiep`, **không có gì được lưu**: giữ chữ đang gõ để người dùng gửi lại |

Câu chào, câu từ chối, câu cấp cứu và câu xin đồng ý theo `Accept-Language` của request; mô hình trả lời theo ngôn ngữ đó
trừ khi người dùng viết rõ bằng ngôn ngữ kia. Cấu hình phía server (`CHATBOT_*`): xem `.env.example`. Chưa đặt
`CHATBOT_API_KEY` thì mở phiên vẫn được, gửi tin trả 503.

## 4. Câu hỏi cho đội frontend

1. **Đường dẫn trang đặt lịch thật** là gì? Email tái khám mở `FRONTEND_URL` + `FOLLOWUP_BOOKING_PATH` (đang là `/dat-lich`);
   báo lại để đặt biến này cho đúng. Trang đó có nhận tham số chọn sẵn như mục 3.3 không?
2. **Nút Đồng ý và chỗ rút lại đồng ý** của chatbot đặt ở đâu trong giao diện chat (trang demo: nút ngay dưới câu
   `CAN_DONG_Y`, liên kết rút lại ở chân khung chat)?
3. **Ô email ở form đặt lịch**: khách không nhập email thì không nhận email xác nhận và lời nhắc. Giữ không bắt buộc và
   ghi chú rõ, hay bắt buộc với khách?
4. **Thông báo `NHAC_TAI_KHAM`** nên mở trang đặt lịch trống, hay chọn sẵn bác sĩ của lượt khám cũ (đọc được từ
   `GET /api/booking/lich-hen/cua-toi/{maPhieuKham}`)?
5. **Ngày giờ ở giao diện tiếng Anh**: trang demo, email và câu thông báo giữ `dd/MM/yyyy` và giờ 24h ở cả 2 ngôn ngữ (trang demo chỉ đổi tên thứ).
   Frontend muốn định dạng khác thì tự định dạng từ giá trị ISO của API.
