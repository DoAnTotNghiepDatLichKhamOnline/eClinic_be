"""Bản 2.5 của tài liệu API: phần phía bệnh nhân / khách của DOANTOTNGH-14 (đặt lịch toàn viện).

- Mục 20 mới, 6 route công khai: quy tắc đặt lịch (20.1), ngày bệnh viện nghỉ (20.2), sổ khám bệnh bằng link riêng (20.3 -
  20.5) và khách xin cấp lại link phiếu khám (20.6); mô hình 4.42-4.45.
- Route 14.5: tệp lịch .ics của phiếu khám.
- 11.3 có thêm tham số `ngay` / `buoi`; kết quả khám có thêm `maSoKham` / `linkSoKham` (4.27); trạng thái `QUAN_TRI_HUY`
  và 2 loại thông báo mới khi quản trị viên hủy / đổi lịch; 3 email mới; quy tắc đặt lịch do quản trị viên sửa.

Route của quản trị viên và bác sĩ (lịch hẹn toàn viện, đặt lịch qua tổng đài, cấp lại link của nhân viên) ở tài liệu API
thứ 2; quy tắc, ngày nghỉ, báo cáo của quản trị viên ở docs/BAN-GIAO-FRONTEND-05-10.md. Giai đoạn 11 của chuỗi dựng tài
liệu; chạy qua build.py. JSON mẫu lấy từ response thật do lay_mau.py ghi lại (temp/api-doc/mau, hàm dat_lich_toan_vien),
tên người và mã chuẩn hoá như các mục trước.
"""

import re

import check
import kit
import p2_quy_uoc
import p5_phu_luc
from p3_dat_lich import BS_TOM_TAT, CONG_KHAI, PHONG, body, dan, js, loi, ok, tham_so, tra
from p6_ngon_ngu import RONG_EMAIL, RONG_PHIEN_BAN, rong_cua_bang
from p8_benh_an_ho_so_bac_si import _pham_vi_muc, sua_json_mau, them_truong
from p9_xac_nhan_ngay_benh_an_day_du import MUC_1, MUC_2, MUC_311, MUC_313, _chu, _DOAN, chen_doan_sau, viet_lai

PHIEN_BAN = "2.5"
NGAY = "20/10/2026"
PHU_LUC_A = "Phụ lục A. Bảng mã lỗi (maLoi)"
PHU_LUC_C = "Phụ lục C. Cấu hình ảnh hưởng tới API và tài khoản mẫu"
MUC_5 = "5. Xác thực (AUTH)"
MUC_15 = "15. Lịch hẹn của tôi"
MUC_20 = "20. Quy tắc đặt lịch, ngày nghỉ, sổ khám bệnh, lấy lại link"
MO_HINH_DAU = 42
MOI = f"**Mới ở v{PHIEN_BAN}:** route này có từ bản {PHIEN_BAN}."
LINK_SK = "http://localhost:5173/so-kham/"
MA_SO_KHAM = "WAIp8NP3oB3Und7FBOsQSVGf7xWe0k05_oQUAkuDotc"
MA_PHIEU = "YUJInIWAEVNA1g34Zn_gfUcAYBOiLWoF5xdxWKirYzY"
SK = "/api/booking/so-kham"

# (tên, mô tả, dòng, record lồng). Thứ tự ở đây là thứ tự đánh số 4.42, 4.43...
MO_HINH = [
    ("QuyTacDatLichResponse",
     "Quy tắc đặt lịch đang dùng (booking-service, 20.1). Quản trị viên sửa được bất cứ lúc nào; giá trị mới áp dụng "
     "ngay cho lần đặt / hủy / đổi tiếp theo.", [
         ("datTruocToiThieuPhut", "number", "Lượt khám chỉ đặt được khi còn cách giờ bắt đầu ít nhất chừng ấy phút."),
         ("soNgayDatTruocToiDa", "number", "Chỉ đặt được trong chừng ấy ngày tới, tính cả hôm nay."),
         ("huyDoiTruocToiThieuPhut", "number", "Hạn hủy / đổi lịch: giờ khám dự kiến trừ chừng ấy phút (`hanHuyDoi`, "
                                               "3.11)."),
         ("soLanDoiLichToiDa", "number", "Số lần đổi lịch tối đa của 1 lần đặt."),
         ("soLichHieuLucToiDaMoiHoSo", "number", "Số lịch sắp tới còn hiệu lực tối đa của 1 người khám."),
     ], ()),
    ("NgayNghiResponse", "1 ngày bệnh viện nghỉ (20.2).", [
        ("ngay", "string", "`yyyy-MM-dd`."),
        ("lyDo", "string", "Theo `Accept-Language` (3.12); thiếu bản tiếng Anh thì trả bản tiếng Việt."),
    ], ()),
    ("SoKhamTomTatResponse",
     "Phần tóm tắt của sổ khám bệnh: ai có link cũng xem được (20.3), nên họ tên đã che và không có kết quả khám.", [
         ("hoTenChe", "string", "Họ và tên đệm viết tắt, giữ tên, vd `N. T. Mai`."),
         ("namSinh", "number", "`null` nếu hồ sơ chưa có ngày sinh."),
         ("ngayKham", "string", "`yyyy-MM-dd`."),
         ("gioKham", "string", "Giờ của lượt khám, `HH:mm:ss`."),
         ("bacSi", "BacSiTomTatResponse", "4.14."),
         ("tenChuyenKhoa", "string", "Theo `Accept-Language`."),
         ("phongKham", "PhongKhamTomTatResponse", "4.15."),
     ], ()),
    ("SoKhamResponse",
     "Sổ khám bệnh đầy đủ (20.4): chỉ trả khi người xem nhập đúng 4 số cuối số điện thoại liên hệ của lượt khám.", [
         ("tomTat", "SoKhamTomTatResponse", "4.44."),
         ("benhNhan", "BenhNhan", "Bảng dưới."),
         ("hoTenNguoiGiamHo", "string", "`null` khi lượt khám không có người giám hộ."),
         ("ketQua", "KetQuaKhamResponse", "4.27; có `maSoKham` / `linkSoKham` của chính sổ khám này."),
     ], (
         ("BenhNhan", "người khám, đã che số CCCD.", [
             ("hoTen", "string", ""),
             ("ngaySinh", "string", "`yyyy-MM-dd`; `null` nếu chưa có."),
             ("gioiTinh", "enum GioiTinh", "`null` nếu chưa có."),
             ("cccdChe", "string", "Chỉ còn 3 số cuối (`*********123`); `null` khi người khám chưa có CCCD."),
         ]),
     )),
]
SO_MO_HINH = {m[0]: f"4.{i + MO_HINH_DAU}" for i, m in enumerate(MO_HINH)}

SO_KHAM_TRUONG = (
    ("maSoKham", "string",
     "Mã sổ khám bệnh 43 ký tự, khác `maPhieuKham` (3.10). Có từ bản 2.5. **Chỉ có** ở kết quả khám trả cho bệnh nhân "
     "(15.3, 16.1, 17.3) và ở sổ khám (20.4); không có mặt trong JSON ở chỗ khác."),
    ("linkSoKham", "string", "`<FRONTEND_URL>/so-kham/<maSoKham>`: trang sổ khám bệnh của frontend (20.3); mã QR ở 20.5. "
                             "Có cùng lúc với `maSoKham`."),
)

# (mục, tiêu đề mục chi tiết, điều đã đổi): dòng "Thay đổi ở v2.5" và cột "Thay đổi" của bảng route.
KET_QUA_MOI = "`ketQua` (4.27) có thêm `maSoKham`, `linkSoKham`: link sổ khám bệnh (20.3)."
CAP_NHAT = (
    ("11.3", "Tìm bác sĩ", "tham số mới `ngay`, `buoi`: chỉ bác sĩ còn lượt đặt được vào ngày / buổi đó."),
    ("14.1", "Xem phiếu khám", "`trangThai` có thể là `QUAN_TRI_HUY` (phòng khám hủy, lý do ở `lyDoHuy`, 3.11). "
                               "Cấu trúc response không đổi."),
    ("15.1", "Danh sách lịch hẹn của tôi", "`trangThai` có thể là `QUAN_TRI_HUY` (3.11). Cấu trúc response không đổi."),
    ("15.3", "Chi tiết lịch hẹn của tôi", KET_QUA_MOI),
    ("16.1", "Lịch sử khám của tôi", KET_QUA_MOI),
    ("17.3", "Trang cá nhân của bệnh nhân", "`lanKhamGanDay[].ketQua` có thêm `maSoKham`, `linkSoKham` như ở 16.1."),
    ("18.1", "Danh sách thông báo của tôi", "loại mới `LICH_HEN_QUAN_TRI_HUY`, `LICH_HEN_QUAN_TRI_DOI` (mục 18). Cấu "
                                            "trúc response không đổi."),
)

LICH_SU = (f"{PHIEN_BAN} — {NGAY}",
           "Mục 20 (6 route công khai): quy tắc đặt lịch do quản trị viên sửa, ngày bệnh viện nghỉ, sổ khám bệnh bằng link "
           "riêng, khách xin cấp lại link phiếu khám; route 14.5 (tệp lịch .ics); tìm bác sĩ theo ngày / buổi còn chỗ "
           "(11.3); kết quả khám có `maSoKham`, `linkSoKham` (4.27); trạng thái `QUAN_TRI_HUY`, 2 loại thông báo và 3 "
           "email mới (3.11, 3.13).")

# ----- Dữ liệu mẫu (cấu trúc từ response thật) -----
TOM_TAT = {"hoTenChe": "N. T. Mai", "namSinh": 1995, "ngayKham": "2026-10-20", "gioKham": "09:10:00",
           "bacSi": BS_TOM_TAT, "tenChuyenKhoa": "Da liễu", "phongKham": PHONG}
KET_QUA = {
    "lyDoKham": "Ho, sốt nhẹ 2 ngày nay",
    "sinhHieu": {"mach": 88, "nhietDo": 37.8, "huyetApTamThu": 118, "huyetApTamTruong": 76, "nhipTho": 18,
                 "canNang": 54.5, "chieuCao": 158.0},
    "khamLamSang": "Họng đỏ, amidan không sưng, phổi trong.",
    "chanDoan": "Viêm họng cấp",
    "icdChinh": {"ma": "J02.9", "ten": "Viêm họng cấp, không đặc hiệu", "manTinh": False},
    "chanDoanKemTheo": "Viêm mũi dị ứng",
    "ghiChu": "Uống nhiều nước ấm, nghỉ ngơi; tái khám nếu sốt trên 38,5 độ.",
    "ngayTaiKhamDeXuat": "2026-10-27",
    "donThuoc": [{"idThuoc": 1, "tenThuoc": "Paracetamol", "hoatChat": "Paracetamol", "hamLuong": "500mg",
                  "duongDung": "UONG", "donVi": "viên", "lieuDung": "1 viên / lần", "soLanMoiNgay": 3, "soNgayDung": 5,
                  "soLuong": 15, "ghiChuSuDung": "Uống sau ăn, khi sốt hoặc đau"}],
    "ngaySuaCuoi": None,
    "maSoKham": MA_SO_KHAM,
    "linkSoKham": LINK_SK + MA_SO_KHAM,
}
CAU_CAP_LAI = ("Nếu mã tra cứu và số điện thoại khớp với 1 lịch hẹn có email, liên kết phiếu khám đã được gửi tới email "
               "đó. Không nhận được email: liên hệ quầy tiếp đón và mang theo CCCD.")
ICS = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//eClinic//Dat lich kham//VI
METHOD:PUBLISH
BEGIN:VEVENT
UID:ECL-20261021-1393@eclinic
SEQUENCE:0
DTSTART:20261021T011000Z
DTEND:20261021T012000Z
STATUS:CONFIRMED
SUMMARY:Khám bệnh - BS. Hoàng Minh Đức
LOCATION:eClinic - Phòng 103\\, Tầng 1
DESCRIPTION:Mã tra cứu: ECL-20261021-1393\\nSố thứ tự: 2\\n...
URL:http://localhost:5173/phieu-kham/...
BEGIN:VALARM
TRIGGER:-PT1H
...
END:VEVENT
END:VCALENDAR"""

MA_SK = ("maSoKham", "path", "string", "Mã sổ khám bệnh 43 ký tự (`maSoKham` ở 4.27, hoặc đọc từ link / QR).")
LOI_SK = (404, "KHONG_TIM_THAY", "Mã sổ khám sai dạng hoặc không tồn tại (cùng một thông điệp).")


def mh(ten):
    return f"`{ten}` ({SO_MO_HINH[ten]})"


def muc_14_5(so):
    return kit.route(
        "14.5", "Tệp lịch (.ics) của phiếu khám", "GET", "/api/booking/phieu-kham/{maPhieuKham}/lich.ics",
        "ep_phieu_kham_ics", next(so), CONG_KHAI,
        [MOI, "Tệp lịch chuẩn iCalendar của lịch hẹn, để người dùng thêm vào Google Calendar, Outlook hay lịch của điện "
              "thoại (nút \"Thêm vào lịch\"). Nội dung theo trạng thái **hiện tại**: giờ khám, bác sĩ, phòng, mã tra "
              "cứu, link phiếu khám, nhắc trước 1 giờ; không có họ tên, số điện thoại, CCCD. Chữ theo `Accept-Language`."],
        [tham_so([("maPhieuKham", "path", "string", "Mã phiếu khám 43 ký tự (3.10).")]),
         kit.nhan("RESPONSE THÀNH CÔNG — 200 OK"),
         dan("Body là tệp văn bản (không phải JSON). Header:"),
         kit.khoi_code("Content-Type: text/calendar;charset=UTF-8\n"
                       "Content-Disposition: attachment; filename=\"lich-kham-ECL-20261021-1393.ics\""),
         dan("Nội dung (rút gọn):"), kit.khoi_code(ICS),
         loi([(404, "KHONG_TIM_THAY", "Mã phiếu khám sai dạng hoặc không tồn tại (cùng một thông điệp).")]),
         kit.luu_y([
             "Cả chuỗi đổi lịch là **1 sự kiện**: `UID` là mã tra cứu của lần đặt đầu, `SEQUENCE` tăng theo mỗi lần đổi "
             "lịch / đổi trạng thái. Mở tệp mới hơn thì ứng dụng lịch dời hoặc hủy sự kiện đã thêm (Google Calendar có "
             "thể thêm bản thứ 2 thay vì cập nhật).",
             "Lịch đã hủy, đã đổi đi, bác sĩ / phòng khám đã hủy: sự kiện `STATUS:CANCELLED`; lịch cần đổi vì ca bị hủy: "
             "`TENTATIVE`. Phiếu cũ của 1 lần đổi chỉ cho tệp \"đã hủy\", không lộ lịch mới.",
             "Dùng thẻ `<a href=\".../lich.ics\">`; lỗi vẫn trả JSON (3.1). Các email đặt lịch, đổi lịch, đổi bác sĩ, cần "
             "đổi lịch, bác sĩ / phòng khám hủy và cấp lại link kèm sẵn tệp này (3.13).",
         ])])


def muc_20(so):
    gioi_thieu = [
        "Các route công khai thêm ở bản 2.5, do `booking-service` cung cấp. Không cần access token (gửi token hết hạn vẫn "
        "nhận 401, như 3.9).",
        "**Sổ khám bệnh** (EXAM-03 / EXAM-04): mỗi lượt khám đã có kết quả có 1 mã sổ khám ngẫu nhiên, khác mã phiếu khám. "
        "Link `linkSoKham` (= `<FRONTEND_URL>/so-kham/<maSoKham>`) có ở kết quả khám của bệnh nhân (15.3, 16.1, 17.3) và "
        "bác sĩ đưa cho bệnh nhân sau khi khám (link / mã QR). Frontend phải có trang ở đường dẫn `/so-kham/:maSoKham`: "
        "gọi 20.3 để hiện phần tóm tắt, rồi hỏi 4 số cuối số điện thoại cho 20.4.",
        "**Lấy lại link phiếu khám**: khách mất link gửi mã tra cứu và số điện thoại (20.6); link chỉ được gửi tới email "
        "của lịch hẹn. Lịch không có email (vd đặt qua tổng đài): người đó mang CCCD tới quầy, nhân viên cấp lại (route "
        "của nhân viên ở tài liệu API thứ 2).",
    ]
    return (kit.tieu_de(1, MUC_20) + "".join(kit.doan(d) for d in gioi_thieu)
            + kit.route(
                "20.1", "Quy tắc đặt lịch đang dùng", "GET", "/api/booking/quy-tac-dat-lich", "ep_quy_tac_dat_lich",
                next(so), CONG_KHAI,
                [MOI, "Các quy tắc bệnh nhân cần biết, theo cấu hình hiện hành do quản trị viên sửa: dùng để hiện \"đặt "
                      "trước tối đa N ngày\", \"hủy / đổi trước giờ khám X phút\" thay vì viết cứng con số ở frontend. "
                      "Không có tham số."],
                [tra("200 OK", f"`duLieu` là {mh('QuyTacDatLichResponse')}.",
                     ok({"datTruocToiThieuPhut": 30, "soNgayDatTruocToiDa": 30, "huyDoiTruocToiThieuPhut": 120,
                         "soLanDoiLichToiDa": 2, "soLichHieuLucToiDaMoiHoSo": 3})),
                 dan("Không có lỗi nghiệp vụ riêng, ngoài các lỗi chung ở mục 3.4."),
                 kit.luu_y(["Giá trị có thể đổi bất cứ lúc nào; đọc lại khi mở trang đặt lịch, không lưu lâu ở frontend.",
                            "Số lịch tối đa của 1 số điện thoại (409 `VUOT_GIOI_HAN_DAT_LICH`, 3.6) không có ở đây."])])
            + kit.route(
                "20.2", "Ngày bệnh viện nghỉ", "GET", "/api/booking/ngay-nghi", "ep_ngay_nghi", next(so), CONG_KHAI,
                [MOI, "Các ngày bệnh viện nghỉ (Tết, lễ, nghỉ đột xuất) trong khoảng, theo ngày, kèm lý do. Ngày nghỉ không "
                      "có ca khám nào nên không có khung giờ để đặt; dùng để làm mờ ngày và hiện lý do trên lịch đặt khám."],
                [tham_so([("tuNgay", "query", "date", "Không bắt buộc, `yyyy-MM-dd`; mặc định hôm nay."),
                          ("denNgay", "query", "date", "Không bắt buộc; mặc định `tuNgay` + 1 năm. Khoảng tối đa 366 "
                                                       "ngày.")]),
                 tra("200 OK", f"`duLieu` là mảng {mh('NgayNghiResponse')}, theo ngày; mảng rỗng khi không có ngày nghỉ.",
                     ok([{"ngay": "2026-11-19", "lyDo": "Nghỉ lễ Quốc khánh bù"}])),
                 dan("Cùng request với `Accept-Language: en`:"),
                 js(ok([{"ngay": "2026-11-19", "lyDo": "National Day holiday (in lieu)"}])),
                 loi([(400, "DU_LIEU_KHONG_HOP_LE", "`denNgay` trước `tuNgay`, khoảng quá 366 ngày, hoặc ngày sai dạng.")]),
                 kit.luu_y(["Một lịch hẹn đã đặt vào ngày sau đó bị khai báo nghỉ: ca bị hủy, lịch có `canDoiLich = true` "
                            "và bệnh nhân được báo + email (3.11, 3.13)."])])
            + kit.route(
                "20.3", "Tóm tắt sổ khám bệnh", "GET", f"{SK}/{{maSoKham}}", "ep_so_kham_tom_tat", next(so), CONG_KHAI,
                [MOI, "Phần ai có link cũng xem được: họ tên đã che, năm sinh, ngày giờ khám, bác sĩ, chuyên khoa, phòng. "
                      "**Không** có chẩn đoán, đơn thuốc hay lời dặn (20.4)."],
                [tham_so([MA_SK]),
                 tra("200 OK", f"`duLieu` là {mh('SoKhamTomTatResponse')}.", ok(TOM_TAT)),
                 loi([LOI_SK]),
                 kit.luu_y(["Trang sổ khám đọc `maSoKham` trên URL; không đưa mã vào log hay công cụ thống kê (như mã phiếu "
                            "khám, 3.10).", "Link không hết hạn."])])
            + kit.route(
                "20.4", "Xem sổ khám bệnh đầy đủ", "POST", f"{SK}/{{maSoKham}}/xem", "ep_so_kham_xem", next(so),
                CONG_KHAI,
                [MOI, "Kết quả khám đầy đủ (chẩn đoán, mã ICD-10, sinh hiệu, đơn thuốc, lời dặn, ngày tái khám) khi người "
                      "xem nhập đúng 4 số cuối số điện thoại liên hệ đã dùng lúc đặt lịch; lượt khám có người giám hộ thì "
                      "là số của người giám hộ. Chữ theo `Accept-Language`."],
                [tham_so([MA_SK]),
                 body([("bonSoCuoi", "string", "Có", "Đúng 4 chữ số.")]),
                 js({"bonSoCuoi": "0001"}),
                 tra("200 OK", f"`duLieu` là {mh('SoKhamResponse')}.",
                     ok({"tomTat": TOM_TAT, "benhNhan": {"hoTen": "Nguyễn Thị Mai", "ngaySinh": "1995-03-12",
                                                         "gioiTinh": "NU", "cccdChe": "*********001"},
                         "hoTenNguoiGiamHo": None, "ketQua": KET_QUA})),
                 loi([(400, "DU_LIEU_KHONG_HOP_LE", "`bonSoCuoi` thiếu hoặc không phải 4 chữ số (kèm `chiTiet`)."),
                      (403, "SO_DIEN_THOAI_KHONG_KHOP", "4 số không khớp số điện thoại liên hệ của lượt khám."),
                      LOI_SK,
                      (429, "GUI_LAI_QUA_NHANH", "Đã nhập sai 5 lần: sổ khám bị khoá xem 15 phút, kể cả khi nhập đúng; "
                                                 "`thongDiep` nêu thời gian chờ.")]),
                 kit.luu_y(["Không lưu kết quả ở trình duyệt: mỗi lần mở trang hỏi lại 4 số.",
                            "Số lần sai đếm theo sổ khám (không theo người xem). Đếm trên Redis; Redis lỗi thì không khoá."])])
            + kit.route(
                "20.5", "Ảnh mã QR của sổ khám bệnh", "GET", f"{SK}/{{maSoKham}}/qr", "ep_so_kham_qr", next(so),
                CONG_KHAI,
                [MOI, "Ảnh PNG (không phải JSON) của mã QR chứa `linkSoKham`, như 14.2 của phiếu khám."],
                [tham_so([MA_SK,
                          ("kichThuoc", "query", "int", "Không bắt buộc. Cạnh ảnh 100 đến 1000 pixel, mặc định 300."),
                          ("taiVe", "query", "boolean", "Không bắt buộc, mặc định false. true: thêm header để lưu thành "
                                                        "tệp.")]),
                 kit.nhan("RESPONSE THÀNH CÔNG — 200 OK"), dan("Body là dữ liệu ảnh. Header:"),
                 kit.khoi_code("Content-Type: image/png\n"
                               "Content-Disposition: attachment; filename=\"so-kham-qr.png\"    (chỉ khi taiVe=true)"),
                 loi([(400, "DU_LIEU_KHONG_HOP_LE", "`kichThuoc` ngoài 100..1000 hoặc tham số sai kiểu."), LOI_SK])])
            + kit.route(
                "20.6", "Xin gửi lại link phiếu khám", "POST", "/api/booking/cap-lai-lien-ket", "ep_cap_lai_lien_ket",
                next(so), CONG_KHAI,
                [MOI, "Khách mất link phiếu khám gửi mã tra cứu (in trên phiếu và trong email, 3.10) và **đủ** số điện "
                      "thoại liên hệ đã dùng lúc đặt (lịch có người giám hộ: số của người giám hộ). Nếu khớp và lịch hẹn có "
                      "email, link phiếu khám (và link sổ khám bệnh nếu đã khám) được gửi tới email đó; lịch đã đổi thì "
                      "gửi phiếu của lịch mới nhất. **Không bao giờ** trả link trên màn hình."],
                [body([("maTraCuu", "string", "Có", "Dạng `ECL-yyyyMMdd-nnnn` (chữ hoa / thường đều được)."),
                       ("soDienThoai", "string", "Có", "10 chữ số, bắt đầu bằng 0.")]),
                 js({"maTraCuu": "ECL-20261021-4385", "soDienThoai": "0912345601"}),
                 kit.nhan("RESPONSE THÀNH CÔNG — 200 OK"),
                 dan("Luôn cùng 1 response, dù mã có thật hay không, số điện thoại đúng hay sai, lịch có email hay "
                     "không:"),
                 js(ok(thong_diep=CAU_CAP_LAI)),
                 loi([(400, "DU_LIEU_KHONG_HOP_LE", "Mã tra cứu hoặc số điện thoại sai dạng (kèm `chiTiet`)."),
                      (429, "GUI_LAI_QUA_NHANH", "Quá 3 lần cho 1 mã tra cứu, hoặc quá 10 lần từ 1 địa chỉ IP trong 1 giờ "
                                                 "(đúng hay sai đều tính); `thongDiep` nêu số phút còn lại.")]),
                 kit.luu_y(["Hiện nguyên `thongDiep` cho người dùng; không suy ra \"mã đúng\" hay \"sai\" từ response.",
                            "Giới hạn đếm trên Redis; Redis lỗi thì bỏ qua giới hạn. Đổi được bằng cấu hình (Phụ lục C).",
                            "Email cấp lại kèm tệp lịch `.ics` (14.5)."])]))


def danh_dau_route(tl, muc):
    """Ô "Thay đổi" của 1 dòng bảng route -> "Cập nhật · v2.5"."""
    dau, cuoi = tl._bang_sau(tl.vi_tri_tieu_de(MUC_2))
    bang = tl.xml[dau:cuoi]
    cac_dong = [m for m in re.finditer(r"<w:tr>.*?</w:tr>", bang, re.S)
                if re.search(r"<w:t[^>]*>([^<]*)</w:t>", m.group(0)).group(1) == muc]
    assert len(cac_dong) == 1, f"Bảng route: cần đúng 1 dòng mục {muc}, thấy {len(cac_dong)}"
    dong = cac_dong[0].group(0)
    o_cuoi = dong.rindex("<w:tc>")
    chu = kit._run(kit._ARIAL + kit._DAM + f'<w:color w:val="{kit.MAU_THAY_DOI["Cập nhật"]}"/>' + kit._co(17),
                   f"Cập nhật · v{PHIEN_BAN}")
    bang = bang.replace(dong, dong[:o_cuoi] + kit._o(kit.RONG_ROUTE_THAY_DOI[5], chu) + "</w:tr>", 1)
    tl.xml = tl.xml[:dau] + kit.to_lai_nen(bang) + tl.xml[cuoi:]


def ghi_thay_doi(tl, muc, ten, markup):
    """Mỗi mục chỉ 1 dòng ghi thay đổi: dòng của bản trước được gộp vào sau "Ở v2.x:", giữ nguyên chữ của nó."""
    dau, cuoi = _pham_vi_muc(tl, f"{muc}  {ten}")
    cu = [m for m in _DOAN.finditer(tl.xml, dau, cuoi) if re.match(r"Thay đổi ở v2\.[1234]:", _chu(m.group(0)))]
    if not cu:
        tl.chen_sau_doan("Quyền truy cập:", kit.doan(f"**Thay đổi ở v{PHIEN_BAN}:** " + markup), tu=dau)
        return
    doan = cu[0].group(0)
    nhan = re.search(r"<w:r><w:rPr>(?:(?!</w:r>).)*>Thay đổi ở v(2\.[1234]):</w:t></w:r>", doan, re.S)
    moi = (doan[:doan.index("<w:r>")] + kit.runs(f"**Thay đổi ở v{PHIEN_BAN}:** {markup} **Ở v{nhan.group(1)}:**")
           + doan[nhan.end():])
    tl.xml = tl.xml[:cu[0].start()] + moi + tl.xml[cu[0].end():]


def _them_so_kham(d):
    """Chèn maSoKham / linkSoKham ngay sau ngaySuaCuoi ở mọi kết quả khám (dict có donThuoc) của mẫu."""
    if isinstance(d, list):
        for v in d:
            _them_so_kham(v)
        return
    if not isinstance(d, dict):
        return
    if "donThuoc" in d and "ngaySuaCuoi" in d and "maSoKham" not in d:
        moi = {}
        for k, v in d.items():
            moi[k] = v
            if k == "ngaySuaCuoi":
                moi["maSoKham"] = MA_SO_KHAM
                moi["linkSoKham"] = LINK_SK + MA_SO_KHAM
        d.clear()
        d.update(moi)
    for v in list(d.values()):
        _them_so_kham(v)


def sua(tl):
    so_booking = len(check.route_trong_code("booking"))

    # --- Trang đầu và mục 1 ---
    cu = ">2.4 — 19/10/2026</w:t>"
    vi_tri = tl.xml.index(cu)  # ô Phiên bản của trang đầu (đứng trước bảng lịch sử)
    tl.xml = tl.xml[:vi_tri] + f">{PHIEN_BAN} — {NGAY}</w:t>" + tl.xml[vi_tri + len(cu):]
    # Số route ở ô Phạm vi do giai đoạn trước đếm từ code (check.CONTROLLER_TRONG_TAI_LIEU, đã gồm 4 controller mới)
    assert f"booking-service {so_booking} route" in tl.xml, "Ô Phạm vi chưa có số route booking-service mới"
    tl.thay_mot("đánh giá; thông báo. Trợ lý AI", "đánh giá; thông báo; quy tắc đặt lịch, ngày nghỉ, sổ khám bệnh, lấy "
                                                 "lại link. Trợ lý AI")
    viet_lai(tl, MUC_1, "Không có trong tài liệu này:",
             "**Không có trong tài liệu này:** các route của bác sĩ (danh sách lịch hẹn, hủy lịch, hàng đợi khám, ca làm "
             "việc, ghi bệnh án) và của quản trị viên trên lịch hẹn (lịch hẹn toàn viện, đặt lịch qua tổng đài, cấp lại "
             "link): xem tài liệu API thứ 2 `eClinic_API_LichHen_KhamBenh_BacSi_QuanTri.docx`. Quy tắc đặt lịch, ngày "
             "nghỉ, lịch làm việc, báo cáo thống kê và danh mục của quản trị viên có ở `docs/BAN-GIAO-FRONTEND-05-10.md` "
             "trong mã nguồn backend.")
    tl.chen_sau_doan("Từ bản 2.4:", kit.doan(
        f"**Từ bản {PHIEN_BAN}:** quy tắc đặt lịch (đặt trước, hạn hủy / đổi, số lần đổi, số lịch) do quản trị viên sửa, "
        "đọc ở 20.1; ngày bệnh viện nghỉ (20.2); sổ khám bệnh có link và mã QR riêng, xem kết quả bằng 4 số cuối số điện "
        "thoại (20.3-20.5, `maSoKham` / `linkSoKham` ở 4.27); khách mất link xin gửi lại qua email (20.6); tệp lịch "
        ".ics (14.5); tìm bác sĩ còn chỗ theo ngày / buổi (11.3). Quản trị viên có thể hủy (`QUAN_TRI_HUY`) hoặc đổi "
        "lịch thay bệnh nhân (3.11, 3.13)."))
    tl.them_dong(MUC_1, "LỊCH SỬ PHIÊN BẢN", kit.dong_bang(RONG_PHIEN_BAN, LICH_SU))
    viet_lai(tl, MUC_1, "Bố cục:",
             "**Bố cục:** mục 2 là bảng tất cả route, bấm để nhảy tới phần chi tiết; mục 3 là quy ước chung (khung phản "
             "hồi, xác thực, lỗi chung, giới hạn, enum, quy ước của phần đặt lịch); mục 4 là mô hình dữ liệu của response; "
             "mục 5-10 là route tài khoản; mục 11-14 là xem chuyên khoa, bác sĩ, khung giờ, đặt lịch và phiếu khám (khách "
             "không đăng nhập dùng được); mục 15-18 là route của bệnh nhân đã đăng nhập (lịch hẹn của tôi, lịch sử khám và "
             "đánh giá, hồ sơ bệnh nhân, thông báo); mục 19 là trợ lý AI; mục 20 là quy tắc đặt lịch, ngày nghỉ, sổ khám "
             "bệnh và lấy lại link (công khai). Phụ lục A: bảng mã lỗi; Phụ lục B: luồng gọi mẫu; Phụ lục C: cấu hình và dữ "
             "liệu mẫu để thử.")

    # --- Mục 2: bảng route ---
    tl.them_dong(MUC_2, None, kit.dong_route("14.5", "GET", "/api/booking/phieu-kham/{maPhieuKham}/lich.ics",
                                             "ep_phieu_kham_ics", "Công khai", "Tệp lịch (.ics) của phiếu khám"),
                 sau_dong_chua="14.4")
    cac_route = (
        ("20.1", "GET", "/api/booking/quy-tac-dat-lich", "ep_quy_tac_dat_lich", "Công khai", "Quy tắc đặt lịch đang dùng"),
        ("20.2", "GET", "/api/booking/ngay-nghi", "ep_ngay_nghi", "Công khai", "Ngày bệnh viện nghỉ"),
        ("20.3", "GET", f"{SK}/{{maSoKham}}", "ep_so_kham_tom_tat", "Công khai", "Tóm tắt sổ khám bệnh"),
        ("20.4", "POST", f"{SK}/{{maSoKham}}/xem", "ep_so_kham_xem", "Công khai", "Xem sổ khám bệnh đầy đủ (4 số cuối "
                                                                                  "SĐT)"),
        ("20.5", "GET", f"{SK}/{{maSoKham}}/qr", "ep_so_kham_qr", "Công khai", "Ảnh mã QR của sổ khám bệnh"),
        ("20.6", "POST", "/api/booking/cap-lai-lien-ket", "ep_cap_lai_lien_ket", "Công khai", "Xin gửi lại link phiếu "
                                                                                              "khám qua email"),
    )
    for dong in cac_route:
        tl.them_dong(MUC_2, None, kit.dong_route(*dong))
    dau, cuoi = tl._bang_sau(tl.vi_tri_tieu_de(MUC_2))
    tl.xml = tl.xml[:dau] + kit.them_cot_thay_doi(
        tl.xml[dau:cuoi], {d[0]: ("Mới", PHIEN_BAN) for d in (("14.5",), *cac_route)}) + tl.xml[cuoi:]

    # --- 3.6 ---
    muc_36 = "3.6 Giới hạn và thời hạn"
    rong_36 = rong_cua_bang(tl, muc_36)
    tl.them_dong(muc_36, None, kit.dong_bang(rong_36, (
        "Đặt trước, số lịch sắp tới của 1 hồ sơ, hạn hủy / đổi, số lần đổi, hạn sửa đánh giá (các dòng trên)",
        "Là giá trị mặc định; từ bản 2.5 quản trị viên sửa được bất cứ lúc nào. Giá trị đang dùng: 20.1")))
    tl.them_dong(muc_36, None, kit.dong_bang(rong_36, (
        "Nhập sai 4 số cuối số điện thoại khi xem sổ khám bệnh (20.4)",
        "5 lần → sổ khám bị khoá xem 15 phút (429 `GUI_LAI_QUA_NHANH`)")))
    tl.them_dong(muc_36, None, kit.dong_bang(rong_36, (
        "Xin gửi lại link phiếu khám (20.6)",
        "3 lần / mã tra cứu và 10 lần / địa chỉ IP trong 1 giờ, đúng hay sai đều tính (429 `GUI_LAI_QUA_NHANH`)")))
    tl.them_dong(muc_36, None, kit.dong_bang(rong_36, ("Ngày bệnh viện nghỉ (20.2)", "Khoảng tối đa 366 ngày mỗi lần gọi")))

    # --- 3.7 ---
    muc_37 = "3.7 Giá trị enum"
    viet_lai(tl, muc_37, "DA_XAC_NHAN, BI_TU_CHOI",
             "`DA_XAC_NHAN`, `BI_TU_CHOI`, `DA_HOAN_THANH`, `DA_HUY`, `DA_HUY_DO_DOI_LICH`, `KHONG_DEN`, `QUAN_TRI_HUY` "
             "(phòng khám hủy, từ bản 2.5). Ý nghĩa từng giá trị ở 3.11.")
    viet_lai(tl, muc_37, "Gửi cho bệnh nhân: LICH_HEN_BI_TU_CHOI",
             "Gửi cho bệnh nhân: `LICH_HEN_BI_TU_CHOI` (bác sĩ hủy lịch đã xác nhận, kèm lý do; nghĩa này từ bản 2.3), "
             "`LICH_HEN_QUAN_TRI_HUY` (phòng khám hủy lịch, kèm lý do, từ bản 2.5), `LICH_HEN_QUAN_TRI_DOI` (phòng khám đổi "
             "lịch thay bệnh nhân, gắn với lịch mới, từ bản 2.5), `LICH_HEN_CAN_DOI` (ca khám bị hủy, cần đổi lịch), "
             "`LICH_HEN_DOI_PHONG` (giữ nguyên giờ, đổi phòng khám), `LICH_HEN_DOI_BAC_SI` (ca khám chuyển cho bác sĩ "
             "khác, giữ nguyên giờ và phòng, từ bản 2.4), `NHAC_LICH_KHAM` (nhắc trước giờ khám, từ bản 2.1), "
             "`NHAC_TAI_KHAM` (bác sĩ hẹn / đổi ngày tái khám và lời nhắc trước ngày đó, từ bản 2.1). "
             "`LICH_HEN_DA_XAC_NHAN` (bác sĩ xác nhận lịch) chỉ còn ở thông báo tạo trước bản 2.3. Chỉ gửi cho bác sĩ / "
             "quản trị viên: `LICH_HEN_MOI`, `LICH_HEN_DA_DOI`, `LICH_HEN_DA_HUY`, `KET_QUA_DUYET_DOI_LICH`, "
             "`CA_LAM_VIEC_THAY_DOI`, `YEU_CAU_DOI_LICH_MOI`, `YEU_CAU_NHUONG_CA`. Đã khai báo nhưng bản hiện tại chưa gửi: "
             "`HE_THONG`; frontend nên hiển thị được mọi giá trị bằng `noiDung`")
    tl.them_dong(muc_37, None, kit.dong_bang(rong_cua_bang(tl, muc_37), (
        "`BuoiKham`", "Tham số `buoi` của 11.3: `SANG` (lượt khám bắt đầu trước 12:00), `CHIEU` (12:00-17:00), `TOI` "
                      "(từ 17:00). Có từ bản 2.5")))

    # --- 3.9 ---
    chen_doan_sau(tl, "3.9 Khách và bệnh nhân đã đăng nhập", "GET /api/booking/phieu-kham/** và POST",
                  "`GET /api/booking/quy-tac-dat-lich`, `GET /api/booking/ngay-nghi`, `GET` / `POST "
                  "/api/booking/so-kham/**`, `POST /api/booking/cap-lai-lien-ket`: quy tắc đặt lịch, ngày nghỉ, sổ khám "
                  "bệnh và xin gửi lại link phiếu khám (mục 20, từ bản 2.5).")

    # --- 3.10 ---
    muc_310 = "3.10 Phiếu khám: mã, liên kết, che thông tin"
    tl.them_dong(muc_310, None, kit.dong_bang(rong_cua_bang(tl, muc_310), (
        "`maSoKham`, `linkSoKham`",
        "Mã sổ khám bệnh (43 ký tự, ngẫu nhiên, khác `maPhieuKham`) và link `<FRONTEND_URL>/so-kham/<maSoKham>` của 1 "
        "lượt khám đã có kết quả (20.3). Có ở kết quả khám của bệnh nhân (4.27). Biết link chỉ xem được phần tóm tắt đã "
        "che; kết quả khám cần thêm 4 số cuối số điện thoại liên hệ (20.4). Có từ bản 2.5.")), sau_dong_chua="linkPhieuKham")

    # --- 3.11 ---
    tl.them_dong(MUC_311, None, kit.dong_bang(rong_cua_bang(tl, MUC_311), (
        "`QUAN_TRI_HUY`",
        "Phòng khám (quản trị viên) hủy lịch, kèm lý do ở `lyDoHuy`; bệnh nhân nhận thông báo `LICH_HEN_QUAN_TRI_HUY` và "
        "email (3.13). Có từ bản 2.5.",
        "Không; đặt lịch mới.")), sau_dong_chua="BI_TU_CHOI")
    chen_doan_sau(tl, MUC_311, "canDoiLich = true:",
                  "**Phòng khám can thiệp** (từ bản 2.5): quản trị viên có thể hủy lịch (`QUAN_TRI_HUY`, có lý do) hoặc "
                  "đổi lịch thay bệnh nhân (lịch cũ `DA_HUY_DO_DOI_LICH`, lịch mới mang mã phiếu mới; bệnh nhân nhận "
                  "`LICH_HEN_QUAN_TRI_DOI` gắn với lịch mới và email \"đã đổi lịch\"), không theo hạn hủy / đổi và số lần "
                  "đổi của bệnh nhân. Khai báo **ngày bệnh viện nghỉ** cũng hủy ca của ngày đó: lịch hẹn giữ nguyên, có "
                  "`canDoiLich = true`, bệnh nhân (kể cả khách có email) nhận email \"ca khám bị hủy, cần đổi lịch\".")
    chen_doan_sau(tl, MUC_311, "Hạn hủy / đổi:",
                  "Các con số \"2 giờ\" (hạn hủy / đổi), \"2 lần\" (số lần đổi) ở mục này là giá trị mặc định: từ bản 2.5 "
                  "quản trị viên sửa được, đọc giá trị đang dùng ở 20.1. `hanHuyDoi` và `duocHuyDoi` luôn theo giá trị "
                  "đang dùng.")

    # --- 3.13: 3 dòng nối tiếp nhau sau "Đổi bác sĩ khám" ---
    sau = "Đổi bác sĩ khám"
    for dong in (
            ("Ca khám bị hủy, cần đổi lịch",
             "Mỗi lần ca khám bị hủy mà lịch hẹn vẫn giữ (`canDoiLich`, 3.11): phòng khám hủy ca, duyệt bác sĩ xin nghỉ / "
             "đổi ca, khai báo ngày bệnh viện nghỉ. Gửi cho mọi lịch hẹn có email, **kể cả khách**. Có từ bản 2.5.",
             "Phiếu khám, để đổi hoặc hủy."),
            ("Phòng khám hủy lịch",
             "Quản trị viên hủy lịch hẹn, kèm lý do (lịch thành `QUAN_TRI_HUY`), theo ngôn ngữ của lịch hẹn. Quản trị "
             "viên đổi lịch thay bệnh nhân thì gửi email \"đã đổi lịch\" cho lịch mới. Có từ bản 2.5.",
             "Phiếu khám; lý do hủy ghi trên phiếu."),
            ("Gửi lại link phiếu khám",
             "Khách xin gửi lại (20.6) hoặc nhân viên cấp lại sau khi xem CCCD: link phiếu khám của lịch hiện hành, và "
             "nút thứ 2 mở sổ khám bệnh khi lượt khám đã có kết quả. Có từ bản 2.5.",
             "Phiếu khám; sổ khám bệnh (20.3).")):
        tl.them_dong(MUC_313, None, kit.dong_bang(RONG_EMAIL, dong), sau_dong_chua=sau)
        sau = dong[0]
    chen_doan_sau(tl, MUC_313, "Liên kết:",
                  "**Tệp lịch** (từ bản 2.5): email đặt lịch, đã đổi lịch, đổi bác sĩ, ca bị hủy cần đổi lịch, bác sĩ / "
                  "phòng khám hủy và gửi lại link kèm tệp `lich-kham.ics` (như 14.5); mở tệp thì ứng dụng lịch thêm, dời "
                  "hoặc hủy cùng 1 sự kiện cho cả chuỗi đổi lịch. Email nhắc lịch, tái khám không kèm. Bệnh nhân tự hủy "
                  "thì không có email.")
    viet_lai(tl, MUC_313, "Thông báo trên chuông đi kèm:",
             "**Thông báo trên chuông đi kèm:** lời nhắc lịch khám sinh thông báo `NHAC_LICH_KHAM`, bác sĩ hủy lịch sinh "
             "`LICH_HEN_BI_TU_CHOI`, phòng khám hủy / đổi lịch sinh `LICH_HEN_QUAN_TRI_HUY` / `LICH_HEN_QUAN_TRI_DOI`, ca "
             "bị hủy sinh `LICH_HEN_CAN_DOI`, đổi bác sĩ khám sinh `LICH_HEN_DOI_BAC_SI`, việc hẹn / đổi ngày tái khám và "
             "lời nhắc tái khám sinh thông báo `NHAC_TAI_KHAM` cho tài khoản phía bệnh nhân (mục 18). Email \"đặt lịch "
             "khám thành công\", \"đã đổi lịch\" do bệnh nhân tự đổi và \"gửi lại link\" không sinh thông báo.")

    # --- Mô hình: 4.27 và 4.42-4.45 ---
    them_truong(tl, "4.27 KetQuaKhamResponse", SO_KHAM_TRUONG, "hanSua")
    tl.chen_truoc_doan(MUC_5, "".join(p2_quy_uoc.mo_hinh(i + MO_HINH_DAU, ten, mo_ta, dong, con)
                                      for i, (ten, mo_ta, dong, con) in enumerate(MO_HINH)))

    # --- 11.3 ---
    for ten, kieu, mo_ta, sau in (
            ("ngay", "date", "Không bắt buộc, `yyyy-MM-dd`. Chỉ bác sĩ còn ít nhất 1 lượt khám đặt được trong ngày này "
                             "(theo quy tắc đặt trước hiện hành, 20.1). Có từ bản 2.5.", "tuKhoa"),
            ("buoi", "enum BuoiKham", "Không bắt buộc: `SANG`, `CHIEU`, `TOI` (3.7). Chỉ bác sĩ còn lượt đặt được trong "
                                      "buổi đó; đi cùng `ngay` hoặc một mình (mọi ngày đặt được). Có từ bản 2.5.", "ngay")):
        tl.them_dong("11.3  Tìm bác sĩ", "THAM SỐ", kit.dong_bang(kit.RONG_THAM_SO, (f"`{ten}`", "query", kieu, mo_ta)),
                     sau_dong_chua=sau)
    viet_lai(tl, "11.3  Tìm bác sĩ", "trang < 0, kichThuoc ngoài 1..100",
             "`trang` < 0, `kichThuoc` ngoài 1..100, `buoi` không phải 1 trong 3 giá trị, hoặc tham số sai kiểu.")
    chen_doan_sau(tl, "11.3  Tìm bác sĩ", "idChuyenKhoa không tồn tại không phải là lỗi",
                  "`ngay` ngoài khoảng đặt lịch được (đã qua, hoặc quá số ngày đặt trước) cũng không phải là lỗi: trả "
                  "trang rỗng. Ngày bệnh viện nghỉ (20.2) không có bác sĩ nào.")

    # --- 18, B.11 ---
    viet_lai(tl, "18. Thông báo", "Bệnh nhân nhận 6 loại",
             "Bệnh nhân nhận 8 loại (`LoaiThongBao` ở 3.7): `LICH_HEN_BI_TU_CHOI` khi bác sĩ hủy lịch (kèm lý do); "
             "`LICH_HEN_QUAN_TRI_HUY` khi phòng khám hủy lịch (kèm lý do) và `LICH_HEN_QUAN_TRI_DOI` khi phòng khám đổi "
             "lịch thay bệnh nhân (`maPhieuKham` là của lịch mới), từ bản 2.5; `LICH_HEN_CAN_DOI` khi ca khám bị hủy và "
             "lịch cần đổi (3.11); `LICH_HEN_DOI_PHONG` khi ca khám đổi phòng; `LICH_HEN_DOI_BAC_SI` khi ca khám chuyển "
             "cho bác sĩ khác (3.11, từ bản 2.4); `NHAC_LICH_KHAM` trước giờ khám 3 ngày và 1 ngày; `NHAC_TAI_KHAM` khi "
             "bác sĩ hẹn hoặc đổi ngày tái khám và 7 ngày trước ngày đó (3.13). Thông báo cũ có thể còn loại "
             "`LICH_HEN_DA_XAC_NHAN` (bác sĩ xác nhận lịch, trước bản 2.3). Người nhận là tài khoản đã đặt lịch và tài "
             "khoản của hồ sơ người khám. Bệnh nhân không nhận thông báo khi tự đặt, tự hủy, tự đổi lịch hoặc khi bác sĩ "
             "ghi kết quả khám mà không hẹn tái khám.")
    viet_lai(tl, "B.11 Thông báo", "Bệnh nhân nhận thông báo khi bác sĩ",
             "Bệnh nhân nhận thông báo khi bác sĩ hủy lịch, khi phòng khám hủy hoặc đổi lịch thay mình, khi ca khám bị hủy "
             "(cần đổi lịch, kể cả do ngày bệnh viện nghỉ), khi đổi phòng khám, khi ca khám chuyển cho bác sĩ khác, khi "
             "tới mốc nhắc lịch khám, và khi bác sĩ hẹn tái khám hoặc tới mốc nhắc tái khám (3.13). Thao tác của chính "
             "bệnh nhân (đặt, đổi, hủy, đánh giá) không sinh thông báo. Thông báo về hủy lịch, hủy ca, đổi phòng và đổi "
             "bác sĩ xuất hiện sau sự kiện khoảng 5 giây; lời nhắc và thông báo tái khám theo chu kỳ nhắc (mặc định 5 "
             "phút, chỉ trong 07:00-21:00).")

    # --- 14.5 và mục 20 ---
    so = iter(range(tl.id_bookmark_moi(), 10 ** 6))
    tl.chen_truoc_doan(MUC_15, muc_14_5(so))
    tl.chen_truoc_doan(PHU_LUC_A, muc_20(so))

    # --- JSON mẫu: kết quả khám có link sổ khám ---
    for muc, ten in (("15.3", "Chi tiết lịch hẹn của tôi"), ("16.1", "Lịch sử khám của tôi"),
                     ("17.3", "Trang cá nhân của bệnh nhân")):
        sua_json_mau(tl, f"{muc}  {ten}", _them_so_kham)

    # --- Dòng "Thay đổi ở v2.5" và cột "Thay đổi" ---
    for muc, ten, markup in CAP_NHAT:
        ghi_thay_doi(tl, muc, ten, markup)
        danh_dau_route(tl, muc)

    # --- Phụ lục B ---
    so_ds = tl.danh_sach_so_moi()
    tl.chen_truoc_doan(PHU_LUC_C, kit.tieu_de(2, "B.14 Sổ khám bệnh và lấy lại link phiếu khám") + "".join(
        kit.cham(d, so_ds) for d in [
            "Sau khi khám, bệnh nhân có `linkSoKham` ở kết quả khám (16.1) hoặc nhận link / mã QR từ bác sĩ. Mở link: trang "
            "`/so-kham/:maSoKham` gọi `GET /api/booking/so-kham/{maSoKham}` (20.3) → hiện tóm tắt đã che.",
            "Người dùng nhập 4 số cuối số điện thoại liên hệ: `POST .../so-kham/{maSoKham}/xem` (20.4) → 200 hiện kết quả "
            "khám; 403 hiện `thongDiep` cho nhập lại; 429 thì khoá ô nhập và hiện thời gian chờ.",
            "Khách mất link phiếu khám: trang \"lấy lại link\" gửi `POST /api/booking/cap-lai-lien-ket` (20.6) với mã tra "
            "cứu và số điện thoại → luôn hiện nguyên `thongDiep`; link tới email của lịch hẹn.",
            "Lịch không có email: hướng dẫn người dùng mang CCCD tới quầy để nhân viên cấp lại (route của nhân viên ở "
            "tài liệu API thứ 2).",
        ]) + kit.khoang_trong(60))

    # --- Phụ lục C ---
    chen_doan_sau(tl, "Cấu hình đặt lịch (booking-service)", "Các con số ở mục 3.6",
                  "Từ bản 2.5, `BOOKING_MIN_LEAD`, `BOOKING_MAX_DAYS`, `BOOKING_MAX_ACTIVE_PER_PATIENT`, "
                  "`BOOKING_MAX_ACTIVE_PER_PHONE`, `BOOKING_CHANGE_LEAD`, `BOOKING_MAX_RESCHEDULES` và `RATING_EDIT_DAYS` "
                  "**chỉ là giá trị ban đầu** của bảng quy tắc trong cơ sở dữ liệu (lần đầu booking-service chạy); sau đó "
                  "quản trị viên sửa ở màn hình quy tắc và giá trị trong bảng thắng. Giá trị đang dùng: 20.1.")
    mac_dinh = p5_phu_luc.mac_dinh_cau_hinh()
    for bien, mo_ta in (("RELINK_CODE_LIMIT", "Số lần xin gửi lại link cho 1 mã tra cứu trong 1 cửa sổ (20.6)."),
                        ("RELINK_IP_LIMIT", "Số lần xin gửi lại link từ 1 địa chỉ IP trong 1 cửa sổ (20.6)."),
                        ("RELINK_WINDOW", "Độ dài cửa sổ đếm của 2 giới hạn trên.")):
        tl.them_dong("Cấu hình đặt lịch (booking-service)", None, kit.dong_bang(
            p5_phu_luc.RONG_CAU_HINH, (f"`{bien}`", f"`{mac_dinh[bien]}`", mo_ta)))
