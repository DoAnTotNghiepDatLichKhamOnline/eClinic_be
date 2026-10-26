"""Bản 2.1 của tài liệu API (tiếp): trợ lý AI (chatbot-service). Mục 19 với 6 route, mô hình 4.37-4.41, enum, giới hạn,
mã lỗi, luồng gọi và cấu hình của chatbot.

Giai đoạn 7 của chuỗi dựng tài liệu; chạy qua build.py. JSON mẫu lấy từ response thật do lay_mau.py ghi lại
(temp/api-doc/mau, các tệp *-chat-*) trên stack có mô hình giả; câu trả lời của mô hình trong mẫu được viết lại cho
giống câu thật, còn cấu trúc và các câu cố định của server (chào, cấp cứu, xin đồng ý) giữ nguyên.
"""

import re

import check
import kit
import p2_quy_uoc
import p5_phu_luc
from p3_dat_lich import CONG_KHAI, BENH_NHAN, body, dan, js, loi, ok, tham_so, tra
from p6_ngon_ngu import PHIEN_BAN, rong_cua_bang

PHU_LUC_A = "Phụ lục A. Bảng mã lỗi (maLoi)"
PHU_LUC_C = "Phụ lục C. Cấu hình ảnh hưởng tới API và tài khoản mẫu"
MUC_5 = "5. Xác thực (AUTH)"
MO_HINH_DAU = 37
P = "/api/chatbot/phien"
CAU_HINH_CHATBOT = check.GOC / "chatbot-service/src/main/resources/application.yml"
MOI = f"**Mới ở v{PHIEN_BAN}:** route này có từ bản {PHIEN_BAN}."
KHACH_HOAC_TK = "Công khai: khách hoặc mọi vai trò (access token không bắt buộc)"
CHU_PHIEN = "Chủ phiên: access token của tài khoản đã mở phiên, hoặc header `X-Chat-Khach` với phiên của khách"
DA_DANG_NHAP = "Mọi vai trò (cần access token)"

# (tên, mô tả, dòng, record lồng). Thứ tự ở đây là thứ tự đánh số 4.37, 4.38...
MO_HINH = [
    ("PhienChatResponse",
     "Phiên chat vừa mở (chatbot-service). **Ngoại lệ của quy ước 3.1:** `maKhach` không có mặt trong JSON (không "
     "phải `null`) khi phiên được mở bằng access token.", [
         ("idPhien", "number", "Id phiên; dùng trên đường dẫn của các route còn lại."),
         ("maKhach", "string", "Mã bí mật của phiên do khách mở, 43 ký tự. Chỉ trả **1 lần** ở đây; gửi lại ở header "
                               "`X-Chat-Khach`. Không có khi request mở phiên có access token."),
         ("thoiGianBatDau", "datetime", ""),
         ("tinNhan", "TinNhanChatResponse[]", "Câu chào của chatbot (1 phần tử, `loai = LOI_CHAO`)."),
     ], ()),
    ("TinNhanChatResponse", "1 tin nhắn của phiên chat.", [
        ("id", "number", ""),
        ("nguoiGui", "enum NguoiGui", "`NGUOI_DUNG` hoặc `CHATBOT`."),
        ("loai", "enum LoaiTinNhanChat", "Xem 3.7 và mục 19."),
        ("noiDung", "string", "Văn bản thường, không markdown, không HTML: hiển thị bằng text và giữ xuống dòng. Nội "
                              "dung đã lưu không đổi theo ngôn ngữ khi đọc lại."),
        ("ngayTao", "datetime", ""),
        ("goiY", "GoiYResponse", "Dữ liệu để vẽ thẻ gợi ý dưới câu trả lời; `null` khi câu trả lời không tra cứu gì. "
                                 "Tin của người dùng luôn `null`."),
    ], ()),
    ("TraLoiResponse", "Kết quả của 1 lần gửi tin nhắn.", [
        ("tinNhan", "TinNhanChatResponse", "Tin của người dùng vừa được lưu."),
        ("traLoi", "TinNhanChatResponse", "Câu trả lời của chatbot."),
    ], ()),
    ("GoiYResponse",
     "Dữ liệu phòng khám đi kèm 1 câu trả lời, do **server dựng từ dữ liệu thật**, không lấy từ câu chữ của mô hình: "
     "bác sĩ, phòng, giờ khám để đặt lịch chỉ lấy từ đây, không lấy từ `noiDung`. Là ảnh chụp lúc trả lời (đọc lại "
     "phiên vẫn như cũ), nên form đặt lịch phải đọc lại số chỗ còn trống.", [
         ("chuyenKhoa", "ChuyenKhoa", "Chuyên khoa được gợi ý; `null` nếu không có."),
         ("bacSi", "BacSi[]", "Tối đa 6 bác sĩ của chuyên khoa; mảng rỗng khi câu trả lời không tra cứu bác sĩ."),
         ("lichKham", "LichKham[]", "Các ca còn chỗ của 1 bác sĩ trong 7 ngày; mảng rỗng khi không tra cứu lịch."),
         ("lichHenCuaToi", "LichHenCuaToi[]", "Lịch hẹn sắp tới của chính người chat, tối đa 10. `null`: câu trả lời "
                                              "không tra cứu phần này; mảng rỗng: đã tra cứu, không có lịch nào."),
         ("lanKham", "LanKham[]", "Các lượt đã khám gần nhất của chính người chat, tối đa 10. `null` / mảng rỗng như "
                                  "`lichHenCuaToi`."),
     ], (
         ("ChuyenKhoa", "chuyên khoa được gợi ý.", [
             ("id", "number", "Dùng làm `idChuyenKhoa` khi mở form đặt lịch."),
             ("ten", "string", "Theo ngôn ngữ của request gửi tin (3.12)."),
         ]),
         ("BacSi", "1 bác sĩ của chuyên khoa được gợi ý.", [
             ("id", "number", "Dùng làm `idBacSi`."),
             ("hoTen", "string", ""),
             ("hocVi", "string", ""),
             ("chucVu", "string", "`null` nếu chưa nhập."),
             ("anhDaiDien", "string", "`null` thì dùng ảnh mặc định."),
             ("ngaySomNhat", "string", "Ngày còn chỗ sớm nhất, `yyyy-MM-dd`; `null` khi bác sĩ không còn chỗ trong "
                                       "khoảng đặt lịch được."),
             ("soChoConLai", "number", "Số chỗ còn lại của ngày đó; `0` khi `ngaySomNhat` là `null`."),
         ]),
         ("LichKham", "1 ca làm việc còn chỗ. Đặt lịch bằng `idLichLamViec` + `khungGio[].gioBatDau` (13.1).", [
             ("idBacSi", "number", ""),
             ("hoTenBacSi", "string", ""),
             ("idLichLamViec", "number", "Id ca khám, như ở 12.1."),
             ("ngay", "string", "`yyyy-MM-dd`."),
             ("tenPhong", "string", ""),
             ("tang", "string", "`null` nếu phòng không ghi tầng."),
             ("khungGio", "KhungGio[]", "Các khung còn chỗ của ca."),
         ]),
         ("KhungGio", "1 khung giờ còn chỗ của ca.", [
             ("gioBatDau", "string", "Dạng ISO như API khung giờ (vd `2026-10-14T08:00:00`); dùng làm "
                                     "`gioBatDauKhung`."),
             ("gioKetThuc", "string", ""),
             ("soChoConLai", "number", ""),
         ]),
         ("LichHenCuaToi", "1 lịch hẹn sắp tới của người chat, như 1 dòng của 15.1 thu gọn. Họ tên và các mã chỉ có "
                           "ở đây; mô hình không nhận được.", [
             ("maPhieuKham", "string", "Mở chi tiết bằng 15.3."),
             ("maTraCuu", "string", ""),
             ("linkPhieuKham", "string", "Liên kết phiếu khám (3.10)."),
             ("trangThai", "string", "`CHO_XAC_NHAN` hoặc `DA_XAC_NHAN`."),
             ("ngay", "string", "`yyyy-MM-dd`."),
             ("gioBatDauKhung", "string", ""),
             ("gioKetThucKhung", "string", ""),
             ("hoTenBacSi", "string", ""),
             ("tenChuyenKhoa", "string", ""),
             ("tenPhong", "string", ""),
             ("tang", "string", ""),
             ("hoTenBenhNhan", "string", ""),
             ("laBanThan", "boolean", "`false`: lịch tài khoản đặt cho người khác."),
             ("canDoiLich", "boolean", "`true`: ca khám đã bị hủy, lịch đang chờ đổi hoặc hủy (3.11)."),
         ]),
         ("LanKham", "1 lượt đã khám của người chat. Chẩn đoán, lời dặn và đơn thuốc **không** có ở đây: mở lịch sử "
                     "khám (16.1) bằng `maPhieuKham`.", [
             ("maPhieuKham", "string", ""),
             ("maTraCuu", "string", ""),
             ("ngay", "string", "Ngày khám, `yyyy-MM-dd`."),
             ("hoTenBacSi", "string", ""),
             ("tenChuyenKhoa", "string", ""),
             ("hoTenBenhNhan", "string", ""),
             ("laBanThan", "boolean", ""),
             ("ngayTaiKhamDeXuat", "string", "`yyyy-MM-dd`; `null` khi bác sĩ không hẹn tái khám. Chỉ là gợi ý, không "
                                             "phải lịch hẹn."),
         ]),
     )),
    ("PhienChatTomTatResponse", "1 phiên chat của tài khoản đang đăng nhập.", [
        ("idPhien", "number", ""),
        ("thoiGianBatDau", "datetime", ""),
        ("daDongYDuLieu", "boolean", "`true`: phiên này đang có đồng ý cho chatbot đọc lịch hẹn của chính người chat "
                                     "(19.5)."),
    ], ()),
]
SO_MO_HINH = {m[0]: f"4.{i + MO_HINH_DAU}" for i, m in enumerate(MO_HINH)}

# (biến môi trường, điều biến đó quyết định), mặc định đọc từ application.yml của chatbot-service
CAU_HINH = (
    ("CHATBOT_API_KEY", "Khoá của dịch vụ mô hình ngôn ngữ. Để trống: mở phiên vẫn được, gửi tin nhắn trả 503 "
                        "`CHATBOT_KHONG_KHA_DUNG`."),
    ("CHATBOT_LIMIT_MESSAGES", "Số tin nhắn của 1 tài khoản (hoặc 1 địa chỉ IP với khách) trong 1 cửa sổ (429 "
                               "`CHAT_QUA_NHIEU`)."),
    ("CHATBOT_LIMIT_WINDOW", "Độ dài cửa sổ đếm tin nhắn."),
    ("CHATBOT_LIMIT_DAILY", "Số tin nhắn của 1 tài khoản / IP trong 24 giờ."),
    ("CHATBOT_LIMIT_SESSIONS", "Số phiên chat mở mới của 1 tài khoản / IP trong 1 giờ."),
    ("CHATBOT_LIMIT_TOTAL_DAILY", "Số lần gọi mô hình của cả hệ thống trong 24 giờ; hết thì mọi người nhận 429."),
    ("CHATBOT_TOTAL_TIMEOUT", "Thời gian tối đa cho 1 câu trả lời (gồm cả các lần tra cứu); quá thì 503."),
    ("CHATBOT_PERSONAL_DATA", "`false`: chatbot không đọc lịch hẹn của người chat; hỏi về lịch của mình thì được trả "
                              "lời như câu hỏi thường, không có `CAN_DONG_Y`."),
)

# ----- Dữ liệu mẫu (cấu trúc từ response thật) -----
MA_KHACH = "_gQFf70quxP6ZtHzcBKoc4J1hE-WvzxeS6vLtWovA24"
MA_PHIEU = "mg_7qtCjg1ENrltrPIiDPUFj5PphkXKS8wr5ILWAQkA"
CAU_CHAO = ("Xin chào! Tôi là trợ lý ảo (AI) của phòng khám eClinic. Tôi có thể hướng dẫn bạn đặt lịch khám, tra cứu "
            "phiếu khám, đổi hoặc hủy lịch hẹn và trả lời các câu hỏi sức khỏe thông thường. Thông tin tôi đưa ra chỉ "
            "để tham khảo, không thay thế việc thăm khám và tư vấn của bác sĩ. Trường hợp khẩn cấp, hãy gọi 115.")
CAU_XIN_DONG_Y = (
    "Để trả lời câu hỏi này, tôi cần đọc lịch hẹn và các lượt khám của chính bạn. Nếu bạn đồng ý, các thông tin sau sẽ "
    "được gửi tới dịch vụ AI bên ngoài đang vận hành trợ lý này: ngày giờ khám, bác sĩ, chuyên khoa, phòng khám, trạng "
    "thái lịch hẹn và ngày tái khám bác sĩ đề xuất. Họ tên, mã lịch hẹn, lý do khám, chẩn đoán, lời dặn và đơn thuốc "
    "không được gửi đi. Hãy bấm Đồng ý rồi gửi lại câu hỏi; bạn có thể rút lại đồng ý bất cứ lúc nào. Nếu không đồng "
    "ý, bạn vẫn xem được các thông tin này ở mục Lịch hẹn và Lịch sử khám.")


def tin(id_tin, nguoi_gui, loai, noi_dung, ngay_tao, goi_y=None):
    return {"id": id_tin, "nguoiGui": nguoi_gui, "loai": loai, "noiDung": noi_dung, "ngayTao": ngay_tao, "goiY": goi_y}


def goi_y(chuyen_khoa=None, bac_si=(), lich_kham=(), lich_hen=None, lan_kham=None):
    return {"chuyenKhoa": chuyen_khoa, "bacSi": list(bac_si), "lichKham": list(lich_kham), "lichHenCuaToi": lich_hen,
            "lanKham": lan_kham}


CHAO = tin(1, "CHATBOT", "LOI_CHAO", CAU_CHAO, "2026-10-13T19:22:05.510574")
DA_LIEU = {"id": 3, "ten": "Da liễu"}
BS_DUC = {"id": 5, "hoTen": "Hoàng Minh Đức", "hocVi": "ThS.BS", "chucVu": "Trưởng khoa Da liễu",
          "anhDaiDien": "https://res.cloudinary.com/demo/image/upload/eclinic/bac-si/5.jpg", "ngaySomNhat": "2026-10-14",
          "soChoConLai": 14}
CA_KHAM = {"idBacSi": 5, "hoTenBacSi": "Hoàng Minh Đức", "idLichLamViec": 17, "ngay": "2026-10-14",
           "tenPhong": "Phòng 103", "tang": "Tầng 1", "khungGio": [
               {"gioBatDau": "2026-10-14T08:00:00", "gioKetThuc": "2026-10-14T09:00:00", "soChoConLai": 3},
               {"gioBatDau": "2026-10-14T09:00:00", "gioKetThuc": "2026-10-14T10:00:00", "soChoConLai": 4}]}
LICH_CUA_TOI = {"maPhieuKham": MA_PHIEU, "maTraCuu": "ECL-20261014-8439",
                "linkPhieuKham": "http://localhost:5173/phieu-kham/" + MA_PHIEU, "trangThai": "CHO_XAC_NHAN",
                "ngay": "2026-10-14", "gioBatDauKhung": "2026-10-14T08:00:00", "gioKetThucKhung": "2026-10-14T09:00:00",
                "hoTenBacSi": "Hoàng Minh Đức", "tenChuyenKhoa": "Da liễu", "tenPhong": "Phòng 103", "tang": "Tầng 1",
                "hoTenBenhNhan": "Nguyễn Thị Mai", "laBanThan": True, "canDoiLich": False}
TRA_LOI_GOI_Y = {
    "tinNhan": tin(7, "NGUOI_DUNG", "CAU_HOI", "Bác sĩ Đức tuần này còn lịch khám không?", "2026-10-13T19:22:06.120012"),
    "traLoi": tin(8, "CHATBOT", "TRA_LOI",
                  "Bác sĩ Hoàng Minh Đức (Da liễu) còn chỗ vào thứ Tư 14/10 tại Phòng 103, Tầng 1. Bạn có thể chọn "
                  "khung giờ ở thẻ bên dưới để đặt lịch.", "2026-10-13T19:22:06.121990",
                  goi_y(DA_LIEU, lich_kham=[CA_KHAM]))}
TRA_LOI_BAC_SI = {
    "tinNhan": tin(5, "NGUOI_DUNG", "CAU_HOI", "Tôi bị nổi mẩn đỏ ở tay, nên khám khoa nào?",
                   "2026-10-13T19:22:06.014489"),
    "traLoi": tin(6, "CHATBOT", "TRA_LOI",
                  "Nổi mẩn đỏ ở da thường được khám ở chuyên khoa Da liễu. Phòng khám có các bác sĩ dưới đây; bạn có "
                  "thể đặt lịch với bác sĩ còn chỗ sớm nhất. Thông tin này chỉ để tham khảo, bác sĩ sẽ thăm khám và "
                  "kết luận.", "2026-10-13T19:22:06.016091", goi_y(DA_LIEU, bac_si=[BS_DUC]))}
TRA_LOI_CAN_DONG_Y = {
    "tinNhan": tin(16, "NGUOI_DUNG", "CAU_HOI", "Lịch hẹn sắp tới của tôi là khi nào?", "2026-10-13T19:22:06.548403"),
    "traLoi": tin(17, "CHATBOT", "CAN_DONG_Y", CAU_XIN_DONG_Y, "2026-10-13T19:22:06.550008")}
TRA_LOI_LICH_CUA_TOI = {
    "tinNhan": tin(18, "NGUOI_DUNG", "CAU_HOI", "Lịch hẹn sắp tới của tôi là khi nào?", "2026-10-13T19:22:06.644862"),
    "traLoi": tin(19, "CHATBOT", "TRA_LOI",
                  "Bạn có 1 lịch hẹn sắp tới: thứ Tư 14/10, khung 08:00-09:00, với bác sĩ Hoàng Minh Đức (Da liễu) tại "
                  "Phòng 103, Tầng 1. Lịch đang chờ bác sĩ xác nhận.", "2026-10-13T19:22:06.646457",
                  goi_y(lich_hen=[LICH_CUA_TOI]))}

LOI_PHIEN = (404, "KHONG_TIM_THAY",
             "Phiên không tồn tại, hoặc không phải của người gọi: sai / thiếu `X-Chat-Khach` với phiên của khách, hay "
             "phiên của tài khoản khác (cùng một thông điệp).")
ID_PHIEN = ("idPhien", "path", "number", "Id phiên, từ 19.1.")


def mh(ten):
    return f"`{ten}` ({SO_MO_HINH[ten]})"


def muc_19(so):
    gioi_thieu = [
        "Trợ lý AI của phòng khám: hướng dẫn dùng trang, trả lời câu hỏi sức khỏe thông thường, gợi ý chuyên khoa và "
        "bác sĩ còn chỗ từ dữ liệu của phòng khám, và (với bệnh nhân đã đăng nhập, sau khi đồng ý) trả lời về lịch hẹn "
        "và lượt khám của **chính người hỏi**. Chatbot không đặt, đổi hay hủy lịch: nó chỉ đưa thẻ gợi ý để frontend mở "
        "form đặt lịch. Do `chatbot-service` cung cấp.",
        "**Phiên và chủ phiên.** Mọi tin nhắn thuộc 1 phiên. Phiên mở khi request có access token thuộc về tài khoản "
        "đó; phiên mở khi không có token thuộc về người cầm `maKhach` (19.1), gửi ở header `X-Chat-Khach` trong mọi "
        "request sau. Phiên không tồn tại và phiên của người khác cùng trả 404 `KHONG_TIM_THAY`. Frontend lưu `idPhien` "
        "(và `maKhach`) ở `sessionStorage` để tải lại trang vẫn còn hội thoại (19.3).",
        "**Khi tài khoản đang dùng trang thay đổi (đăng nhập, đăng xuất, đổi tài khoản): bỏ phiên cũ và mở phiên "
        "mới.** Phiên của khách không bao giờ tra cứu được lịch hẹn cá nhân, kể cả sau khi người đó đăng nhập, và "
        "phiên của tài khoản thì khách không đọc được.",
        "**Loại tin nhắn** (`loai`, enum `LoaiTinNhanChat` ở 3.7):",
    ]
    loai = kit.bang((2000, 4000, 3638), ("loai", "Ý nghĩa", "Frontend làm gì"), [
        ("`CAU_HOI`", "Tin của người dùng.", "Hiện ở phía người dùng."),
        ("`LOI_CHAO`", "Câu chào khi mở phiên: nói rõ đây là AI, không thay bác sĩ.", "Hiện đầu hội thoại."),
        ("`TRA_LOI`", "Câu trả lời của mô hình, có thể kèm `goiY`.", "Hiện câu và vẽ thẻ từ `goiY`."),
        ("`NGOAI_PHAM_VI`", "Câu từ chối cố định khi câu hỏi không liên quan tới phòng khám hay sức khỏe.",
         "Hiện như câu trả lời."),
        ("`KHAN_CAP`", "Tin có dấu hiệu cấp cứu: câu \"gọi 115\" cố định, không qua mô hình.", "Làm nổi bật."),
        ("`CAN_DONG_Y`", "Xin bệnh nhân đồng ý trước khi đọc lịch hẹn của họ; chưa có gì được đọc.",
         "Hiện câu và nút Đồng ý (19.5)."),
    ]) + kit.khoang_trong(60)
    sau = [
        "**Mở form đặt lịch từ thẻ gợi ý.** Từ `goiY` (" + SO_MO_HINH["GoiYResponse"] + "): `chuyenKhoa.id` là "
        "`idChuyenKhoa`; `bacSi[].id` là `idBacSi`; với 1 phần tử của `lichKham`, dùng `idLichLamViec` và "
        "`khungGio[].gioBatDau` làm `gioBatDauKhung`. Thẻ lịch khám có thể không kèm `chuyenKhoa`: lấy chuyên khoa từ "
        "`GET /api/catalog/bac-si/{id}` (11.4). Chatbot có thể gợi ý ca xa hơn 7 ngày tới, nên tải lịch của bác sĩ từ "
        "ngày của ca bằng `GET /api/booking/khung-gio/nhieu-ngay` (12.3) với `tuNgay` = `lichKham[].ngay`. Giá trị đã "
        "cũ (ca hết chỗ, đã bị hủy) thì bỏ qua và để người dùng chọn lại. Đặt lịch vẫn là "
        "`POST /api/booking/lich-hen` (13.1).",
        "**Lịch hẹn của chính người hỏi.** Chỉ dành cho vai trò `BENH_NHAN`, trong phiên của chính tài khoản đó. Lần "
        "đầu hỏi về lịch hẹn, lượt khám hay ngày tái khám của mình trong 1 phiên, câu trả lời có `loai = CAN_DONG_Y`. "
        "Frontend hiện nút Đồng ý; bấm thì gọi 19.5 rồi **gửi lại câu hỏi vừa rồi** bằng 19.2 (server không tự trả "
        "lời lại). Đồng ý có hiệu lực cho cả phiên đó và rút lại được bằng 19.6. Khách, bác sĩ, quản trị viên và "
        "phiên của khách không có phần này: chatbot trả lời như câu hỏi thường.",
        "**Dữ liệu gửi cho dịch vụ AI bên ngoài.** Sau khi đồng ý, chỉ ngày giờ, bác sĩ, chuyên khoa, phòng, tầng, "
        "trạng thái, \"của mình hay đặt cho người khác\" và ngày tái khám được gửi cho mô hình. Họ tên, mã tra cứu, lý "
        "do khám, chẩn đoán, đơn thuốc không bao giờ được gửi: chúng chỉ có trên thẻ do server dựng (`goiY`) và ở các "
        "màn hình lịch hẹn, lịch sử khám. Không có cách nào hỏi về người khác.",
        "**Ngôn ngữ và thời gian chờ.** Câu chào, câu từ chối, câu cấp cứu và câu xin đồng ý theo `Accept-Language` "
        "của request (3.12); mô hình trả lời theo ngôn ngữ đó trừ khi người dùng viết rõ bằng ngôn ngữ kia. Gửi tin "
        "không stream: thường vài giây, tới khoảng 25 giây khi có tra cứu, nên cần trạng thái \"đang trả lời\" và "
        "khoá nút gửi. Giới hạn số tin nhắn ở 3.6.",
    ]
    return (kit.tieu_de(1, "19. Trợ lý AI (chatbot)") + "".join(kit.doan(d) for d in gioi_thieu) + loai
            + "".join(kit.doan(d) for d in sau)
            + kit.route(
                "19.1", "Mở phiên chat", "POST", P, "ep_chat_mo_phien", next(so), KHACH_HOAC_TK,
                [MOI, "Mở 1 phiên mới và trả câu chào. Không có body. Gửi kèm access token thì phiên thuộc về tài "
                      "khoản; không gửi thì phiên thuộc về khách và response có `maKhach`."],
                [tra("201 Created", f"`duLieu` là {mh('PhienChatResponse')}. Mẫu dưới là phiên do khách mở; phiên mở "
                                    "bằng access token không có trường `maKhach`.",
                     ok({"idPhien": 1, "maKhach": MA_KHACH, "thoiGianBatDau": "2026-10-13T19:22:05.446461",
                         "tinNhan": [CHAO]})),
                 loi([(401, "CHUA_DANG_NHAP", "Có gửi access token nhưng token sai hoặc hết hạn (3.9)."),
                      (429, "CHAT_QUA_NHIEU", "Quá 10 phiên mới trong 1 giờ từ cùng tài khoản, hoặc cùng địa chỉ IP với "
                                              "khách.")]),
                 kit.luu_y([
                     "`maKhach` chỉ được trả ở response này; mất thì không lấy lại được, mở phiên mới.",
                     "Mở phiên không cần cấu hình mô hình: khi chatbot đang tắt (thiếu `CHATBOT_API_KEY`) vẫn trả 201, "
                     "lần gửi tin đầu tiên mới trả 503.",
                 ])])
            + kit.route(
                "19.2", "Gửi tin nhắn", "POST", f"{P}/{{idPhien}}/tin-nhan", "ep_chat_gui", next(so), CHU_PHIEN,
                [MOI, "Lưu tin của người dùng và trả câu trả lời của chatbot trong cùng response."],
                [tham_so([ID_PHIEN, ("X-Chat-Khach", "header", "string", "`maKhach` của phiên; bắt buộc với phiên của "
                                                                         "khách, bỏ qua với phiên của tài khoản.")]),
                 body([("noiDung", "string", "Có", "Không rỗng, tối đa 1000 ký tự.")]),
                 js({"noiDung": "Tôi bị nổi mẩn đỏ ở tay, nên khám khoa nào?"}),
                 tra("200 OK", f"`duLieu` là {mh('TraLoiResponse')}. Câu trả lời kèm thẻ chuyên khoa và bác sĩ:",
                     ok(TRA_LOI_BAC_SI)),
                 dan("Câu trả lời kèm lịch khám còn chỗ của 1 bác sĩ:"), js(ok(TRA_LOI_GOI_Y)),
                 dan("Bệnh nhân hỏi về lịch hẹn của mình khi phiên chưa có đồng ý (`CAN_DONG_Y`):"),
                 js(ok(TRA_LOI_CAN_DONG_Y)),
                 dan("Cùng câu hỏi, sau khi đồng ý (19.5):"), js(ok(TRA_LOI_LICH_CUA_TOI)),
                 loi([(400, "DU_LIEU_KHONG_HOP_LE", "`noiDung` rỗng hoặc quá 1000 ký tự (kèm `chiTiet`)."),
                      LOI_PHIEN,
                      (429, "CHAT_QUA_NHIEU", "Quá 20 tin trong 10 phút hoặc 100 tin trong 24 giờ từ cùng tài khoản / "
                                              "địa chỉ IP, hoặc hệ thống đã hết lượt gọi mô hình trong ngày (3.6)."),
                      (503, "CHATBOT_KHONG_KHA_DUNG", "Chưa cấu hình khoá mô hình, hoặc mô hình lỗi / không trả lời "
                                                      "kịp.")]),
                 kit.luu_y([
                     "Khi nhận 429 hoặc 503, **không có gì được lưu**, kể cả tin của người dùng: giữ nguyên chữ đang "
                     "gõ, hiện `thongDiep` và để người dùng gửi lại.",
                     "Nhận 404: bỏ `idPhien` đã lưu và mở phiên mới ở lần gửi sau.",
                     "Nên chặn độ dài ngay ở ô nhập (`maxlength=\"1000\"`).",
                     "`KHAN_CAP` và `NGOAI_PHAM_VI` là câu cố định của server, trả về với mã 200 như câu trả lời thường.",
                     "Tài khoản không phải bệnh nhân hỏi về \"lịch hẹn của tôi\" nhận câu trả lời thường (`TRA_LOI`, "
                     "`goiY = null`), không có `CAN_DONG_Y`.",
                 ])])
            + kit.route(
                "19.3", "Lịch sử tin nhắn của phiên", "GET", f"{P}/{{idPhien}}/tin-nhan", "ep_chat_lich_su", next(so),
                CHU_PHIEN,
                [MOI, "Toàn bộ tin nhắn của 1 phiên, cũ nhất trước, không phân trang. Dùng khi tải lại trang."],
                [tham_so([ID_PHIEN, ("X-Chat-Khach", "header", "string", "Như 19.2.")]),
                 tra("200 OK", f"`duLieu` là mảng {mh('TinNhanChatResponse')}; `goiY` của từng câu trả lời được giữ "
                               "như lúc trả lời.",
                     ok([CHAO, TRA_LOI_BAC_SI["tinNhan"], TRA_LOI_BAC_SI["traLoi"]])),
                 loi([LOI_PHIEN])])
            + kit.route(
                "19.4", "Các phiên chat của tôi", "GET", P, "ep_chat_phien_cua_toi", next(so), DA_DANG_NHAP,
                [MOI, "Các phiên gần nhất của tài khoản đang đăng nhập, mới nhất trước, tối đa 50. Dùng để biết phiên "
                      "đang mở đã có đồng ý hay chưa (`daDongYDuLieu`) và hiện chỗ rút lại đồng ý."],
                [tra("200 OK", f"`duLieu` là mảng {mh('PhienChatTomTatResponse')}.",
                     ok([{"idPhien": 3, "thoiGianBatDau": "2026-10-13T19:22:06.512005", "daDongYDuLieu": True}])),
                 dan("Không có lỗi nghiệp vụ riêng, ngoài các lỗi chung ở mục 3.4. Phiên của khách không có trong danh "
                     "sách này.")])
            + kit.route(
                "19.5", "Đồng ý cho chatbot đọc lịch hẹn của tôi", "POST", f"{P}/{{idPhien}}/dong-y", "ep_chat_dong_y",
                next(so), BENH_NHAN,
                [MOI, "Ghi nhận đồng ý của bệnh nhân cho **phiên này**. Gọi sau khi nhận câu trả lời `CAN_DONG_Y`, "
                      "rồi gửi lại câu hỏi bằng 19.2. Không có body."],
                [tham_so([ID_PHIEN]),
                 kit.nhan("RESPONSE THÀNH CÔNG — 200 OK"),
                 js(ok(thong_diep="Đã ghi nhận đồng ý, bạn hãy gửi lại câu hỏi")),
                 loi([(403, "KHONG_CO_QUYEN", "Tài khoản không phải vai trò `BENH_NHAN`."),
                      (404, "KHONG_TIM_THAY", "Phiên không tồn tại, là phiên của khách, hoặc của tài khoản khác.")]),
                 kit.luu_y(["Đồng ý chỉ có hiệu lực trong phiên này; phiên mới phải xin lại.",
                            "Gọi lại khi phiên đã có đồng ý vẫn trả 200."])])
            + kit.route(
                "19.6", "Rút lại đồng ý", "DELETE", f"{P}/{{idPhien}}/dong-y", "ep_chat_rut_dong_y", next(so),
                BENH_NHAN,
                [MOI, "Rút lại đồng ý của phiên: chatbot thôi đọc lịch hẹn của bệnh nhân, và thôi gửi lại cho mô hình "
                      "các câu trả lời cũ về lịch hẹn. Hỏi lại về lịch của mình sẽ nhận `CAN_DONG_Y`."],
                [tham_so([ID_PHIEN]),
                 kit.nhan("RESPONSE THÀNH CÔNG — 200 OK"), js(ok(thong_diep="Đã rút lại đồng ý")),
                 loi([(403, "KHONG_CO_QUYEN", "Tài khoản không phải vai trò `BENH_NHAN`."),
                      (404, "KHONG_TIM_THAY", "Phiên không tồn tại, là phiên của khách, hoặc của tài khoản khác.")]),
                 kit.luu_y(["Các tin nhắn đã có trong phiên vẫn còn và vẫn đọc được bằng 19.3."])]))


def sua(tl):
    so_route = len(check.route_trong_code("chatbot"))

    # --- Trang đầu và mục 1 ---
    tl.thay_mot("đánh giá; thông báo</w:t>", f"đánh giá; thông báo. Trợ lý AI (chatbot-service, {so_route} route)</w:t>")
    tl.thay_noi_dung(
        "Không có trong tài liệu này:",
        "**Không có trong tài liệu này:** các route đặt lịch của bác sĩ (danh sách lịch hẹn, xác nhận / từ chối, ca làm "
        "việc) và của quản trị viên (quản lý lịch hẹn, lịch làm việc, hồ sơ bệnh nhân, danh mục); API của "
        "`medical-service`, `report-service`.")
    tl.thay_noi_dung(
        f"Từ bản {PHIEN_BAN}:",
        f"**Từ bản {PHIEN_BAN}:** tài liệu có thêm quy ước ngôn ngữ EN / VI (3.12), email và lời nhắc lịch hẹn (3.13), "
        "route lưu ngôn ngữ của tài khoản (6.4) và phần trợ lý AI (mục 19, mô hình "
        f"4.{MO_HINH_DAU}-4.{MO_HINH_DAU + len(MO_HINH) - 1}). Cách trang demo dùng chatbot (khung chat, thẻ gợi ý, "
        "form đặt lịch chọn sẵn) có ở `docs/BAN-GIAO-FRONTEND-NGON-NGU-EMAIL-CHATBOT.md` trong mã nguồn backend.")

    # --- Mục 2 ---
    cac_route = (
        ("19.1", "POST", P, "ep_chat_mo_phien", "Công khai", "Mở phiên chat"),
        ("19.2", "POST", f"{P}/{{idPhien}}/tin-nhan", "ep_chat_gui", "Chủ phiên", "Gửi tin nhắn, nhận câu trả lời"),
        ("19.3", "GET", f"{P}/{{idPhien}}/tin-nhan", "ep_chat_lich_su", "Chủ phiên", "Lịch sử tin nhắn của phiên"),
        ("19.4", "GET", P, "ep_chat_phien_cua_toi", "Đã đăng nhập", "Các phiên chat của tôi"),
        ("19.5", "POST", f"{P}/{{idPhien}}/dong-y", "ep_chat_dong_y", "Bệnh nhân", "Đồng ý cho chatbot đọc lịch hẹn "
                                                                                 "của tôi"),
        ("19.6", "DELETE", f"{P}/{{idPhien}}/dong-y", "ep_chat_rut_dong_y", "Bệnh nhân", "Rút lại đồng ý"),
    )
    for dong in cac_route:
        tl.them_dong("2. Danh sách route", None, kit.dong_route(*dong))
    dau, cuoi = tl._bang_sau(tl.vi_tri_tieu_de("2. Danh sách route"))
    tl.xml = tl.xml[:dau] + kit.them_cot_thay_doi(tl.xml[dau:cuoi],
                                                  {d[0]: ("Mới", PHIEN_BAN) for d in cac_route}) + tl.xml[cuoi:]

    # --- 3.6, 3.7, 3.8 ---
    tl.them_dong("3.6 Giới hạn và thời hạn", None, kit.dong_bang(rong_cua_bang(tl, "3.6 Giới hạn và thời hạn"), (
        "Tin nhắn gửi cho chatbot (19.2), theo tài khoản, hoặc theo địa chỉ IP với khách",
        "20 tin / 10 phút và 100 tin / 24 giờ; 10 phiên mới / giờ (19.1); thêm 1 trần chung của cả hệ thống trong "
        "ngày → 429 `CHAT_QUA_NHIEU`. Đổi được bằng cấu hình (Phụ lục C)")))
    rong_enum = rong_cua_bang(tl, "3.7 Giá trị enum")
    tl.them_dong("3.7 Giá trị enum", None, kit.dong_bang(rong_enum, ("`NguoiGui`", "`NGUOI_DUNG`, `CHATBOT`")))
    tl.them_dong("3.7 Giá trị enum", None, kit.dong_bang(rong_enum, (
        "`LoaiTinNhanChat`", "`CAU_HOI`, `LOI_CHAO`, `TRA_LOI`, `NGOAI_PHAM_VI`, `KHAN_CAP`, `CAN_DONG_Y`. Ý nghĩa "
                             "từng giá trị ở mục 19.")))
    tl.them_dong("3.8 Service và tiền tố đường dẫn", None,
                 kit.dong_bang(rong_cua_bang(tl, "3.8 Service và tiền tố đường dẫn"), (
                     "`/api/chatbot/**`", "chatbot-service", "8087", "`http://localhost:8087/swagger-ui.html`")))

    # --- Mô hình 4.37-4.41 ---
    tl.chen_truoc_doan(MUC_5, "".join(p2_quy_uoc.mo_hinh(i + MO_HINH_DAU, ten, mo_ta, dong, con)
                                      for i, (ten, mo_ta, dong, con) in enumerate(MO_HINH)))

    # --- Mục 19 ---
    so = iter(range(tl.id_bookmark_moi(), 10 ** 6))
    tl.chen_truoc_doan(PHU_LUC_A, muc_19(so))

    # --- Phụ lục A: mã lỗi của chatbot ---
    code = check.ma_loi_trong_code()
    for ma in ("CHAT_QUA_NHIEU", "CHATBOT_KHONG_KHA_DUNG"):
        tl.them_dong(PHU_LUC_A, None, kit.dong_bang(kit.RONG_MA_LOI, (f"`{ma}`", str(code[ma][0]), code[ma][1]),
                                                    cot_giua=(1,)))

    # --- Phụ lục B ---
    so_ds = tl.danh_sach_so_moi()
    tl.chen_truoc_doan(PHU_LUC_C, kit.tieu_de(2, "B.13 Hỏi trợ lý AI") + "".join(kit.cham(d, so_ds) for d in [
        "Người dùng mở khung chat lần đầu: `POST /api/chatbot/phien` (19.1), kèm access token nếu đang đăng nhập → 201. "
        "Lưu `idPhien` (và `maKhach` nếu có) ở `sessionStorage`; hiện câu chào trong `tinNhan`.",
        "Gửi câu hỏi: `POST /api/chatbot/phien/{idPhien}/tin-nhan` (19.2), khách thì kèm header `X-Chat-Khach`. Khoá "
        "nút gửi và hiện \"đang trả lời\" tới khi có response; hiện `traLoi.noiDung` và vẽ thẻ từ `traLoi.goiY`.",
        "Người dùng bấm 1 khung giờ trên thẻ lịch khám: mở form đặt lịch đã chọn sẵn chuyên khoa, bác sĩ, ngày và "
        "khung giờ (mục 19), rồi đặt bằng `POST /api/booking/lich-hen` (13.1) như thường.",
        "Bệnh nhân hỏi về lịch hẹn của mình: nhận `loai = CAN_DONG_Y` → hiện nút Đồng ý → "
        "`POST .../dong-y` (19.5) → gửi lại đúng câu hỏi đó bằng 19.2 → câu trả lời có `goiY.lichHenCuaToi`.",
        "Tải lại trang: `GET /api/chatbot/phien/{idPhien}/tin-nhan` (19.3) để vẽ lại hội thoại; nhận 404 thì bỏ phiên "
        "đã lưu.",
        "Đăng nhập, đăng xuất hoặc đổi tài khoản: xoá `idPhien` / `maKhach` đã lưu và mở phiên mới ở lần mở khung chat "
        "sau.",
        "Nhận 429 `CHAT_QUA_NHIEU` hoặc 503 `CHATBOT_KHONG_KHA_DUNG`: hiện `thongDiep`, giữ chữ đang gõ để gửi lại.",
    ]) + kit.khoang_trong(60))

    # --- Phụ lục C ---
    mac_dinh = dict(re.findall(r"\$\{(\w+):([^}]*)\}", CAU_HINH_CHATBOT.read_text(encoding="utf-8")))
    tl.chen_truoc_doan("Tài khoản có sẵn", kit.tieu_de(2, "Cấu hình chatbot (chatbot-service)") + kit.doan(
        "Các giới hạn ở 3.6 và mục 19 là giá trị mặc định dưới đây. Frontend đừng tự đếm để chặn: cứ gọi API và xử lý "
        "mã lỗi trả về.")
        + kit.bang(p5_phu_luc.RONG_CAU_HINH, ("Biến môi trường", "Mặc định", "Quyết định"),
                   [(f"`{bien}`", f"`{mac_dinh[bien]}`" if mac_dinh[bien] else "(trống)", mo_ta)
                    for bien, mo_ta in CAU_HINH]) + kit.khoang_trong(60))
