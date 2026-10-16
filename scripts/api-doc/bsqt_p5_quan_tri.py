"""Tài liệu 2, giai đoạn 5: mục 9 (quản trị viên: tra cứu lịch hẹn theo mã, đánh dấu đã đối chiếu giấy tờ, lịch sử sửa
hồ sơ bệnh án), 3 route, cùng 3 dòng của bảng route ở mục 2. Chạy qua bsqt_build.py.

JSON mẫu lấy từ response thật do bsqt_lay_mau.py ghi lại (temp/api-doc/mau-bsqt, p5_quan_tri); thời điểm đặt lịch chuẩn
hoá như các mục trước. Quy tắc nghiệp vụ đã có ở mục 3 thì chỉ dẫn mục.
"""

import bsqt_p2_khung
import kit
from bsqt_p3_lich_hang_doi import BAC_SI, ID_LICH_HEN, MAI, lich_hen, mh
from bsqt_p4_kham_benh import BOOKING, ID_LICH_HEN_MD, LICH_SU, LOI_THIEU_MA, MA, MEDICAL
from p3_dat_lich import dan, js, loi, ok, tham_so, tra

QT = "/api/booking/quan-tri/lich-hen"
QT_MD = "/api/medical/quan-tri/lich-hen"
QUYEN = "Chỉ QUAN_TRI_VIEN (cần access token)"

# ----- Dữ liệu mẫu (từ response thật) -----

# Lịch E: khách đặt bằng CCCD của bệnh nhân mẫu benhnhan01, họ tên nhập sai 1 chữ: mang cờ cần đối chiếu (3.6)
LH_E = {**lich_hen(10, "6717", 2, "08:10:00", {**MAI, "soDienThoai": "0933100005"}, 1, "Khám sức khỏe định kỳ",
                   ngay="2026-10-21", id_ca=45, ngay_dat="2026-10-15T16:42:05.27311"),
        "bacSi": BAC_SI}
LH_E["doiChieu"] = {**LH_E["doiChieu"], "canDoiChieu": True, "hoTenDaNhap": "Nguyễn Thị Mam"}

# ----- Bảng route ở mục 2 -----

ROUTE = [
    ("9.1", "GET", f"{QT}/tra-cuu", "ep_qt_tra_cuu", "Tra 1 lịch hẹn bất kỳ theo mã / QR", BOOKING),
    ("9.2", "POST", f"{QT}/{{id}}/da-doi-chieu", "ep_qt_da_doi_chieu", "Đánh dấu đã đối chiếu giấy tờ", BOOKING),
    ("9.3", "GET", f"{QT_MD}/{{idLichHen}}/benh-an/lich-su", "ep_qt_lich_su_sua", "Lịch sử sửa hồ sơ bệnh án",
     MEDICAL),
]
R = {muc: (method, duong, bm) for muc, method, duong, bm, _, _ in ROUTE}


def route(so, muc, ten, mo_ta, than):
    method, duong, bm = R[muc]
    return kit.route(muc, ten, method, duong, bm, next(so), QUYEN, mo_ta, than)


def muc_9(so):
    x = kit.tieu_de(1, "9. Quản trị viên: tra cứu lịch hẹn, đối chiếu giấy tờ, lịch sử sửa hồ sơ bệnh án")
    x += kit.doan(f"Các route của quản trị viên (3.3): thấy mọi lịch hẹn của mọi bác sĩ. Service cần chạy: {BOOKING} cho "
                  f"9.1, 9.2; {MEDICAL} cho 9.3 (3.2). Bệnh nhân mất phiếu hoặc khiếu nại: quản trị viên tra lịch hẹn "
                  "bằng mã (9.1); lịch hẹn cần đối chiếu giấy tờ thì đánh dấu đã đối chiếu sau khi xem giấy tờ (9.2); "
                  "lượt khám đã có hồ sơ bệnh án thì xem các lần bác sĩ sửa hồ sơ (9.3). Quản trị viên không sửa thông "
                  "tin bệnh nhân và không ghi, không sửa hồ sơ bệnh án.")
    x += route(so, "9.1", "Tra lịch hẹn theo mã", "Như 7.1 nhưng tìm trong mọi lịch hẹn, không chỉ lịch hẹn của 1 bác "
               "sĩ, và kết quả có `bacSi`.", [
                   tham_so([MA]),
                   tra("200 OK", f"`duLieu`: {mh('LichHenTrongCaResponse')}, `bacSi` có giá trị (TK 4.14). Ví dụ: lịch "
                                 "hẹn khách đặt, họ tên nhập khác hồ sơ nên cần đối chiếu (`doiChieu.canDoiChieu`).",
                       ok(LH_E)),
                   loi([LOI_THIEU_MA,
                        (404, "KHONG_TIM_THAY", "`ma` sai dạng hoặc không có lịch hẹn nào (3.3).")]),
                   kit.luu_y(["Tìm được lịch hẹn ở mọi trạng thái, kể cả đã hủy và lịch cũ sau khi đổi lịch.",
                              "`id` trong kết quả dùng cho 9.2 và 9.3."])])
    x += route(so, "9.2", "Đánh dấu đã đối chiếu", "Như 7.5, cho lịch hẹn của bất kỳ bác sĩ nào: "
               "`doiChieu.canDoiChieu` thành `false`.", [
                   tham_so([ID_LICH_HEN]),
                   kit.nhan("RESPONSE THÀNH CÔNG — 200 OK"),
                   dan("Không có `duLieu`:"), js(ok(None, "Đã ghi nhận đối chiếu thông tin bệnh nhân")),
                   loi([(404, "KHONG_TIM_THAY", "Lịch hẹn không có.")]),
                   kit.luu_y(["Lịch hẹn ở trạng thái nào cũng được. Gọi lại khi đã `false` vẫn 200. Đọc lại cờ bằng 9.1.",
                              "Sửa thông tin bệnh nhân là việc của bác sĩ (7.4); quản trị viên không có route này."])])
    x += route(so, "9.3", "Lịch sử sửa hồ sơ bệnh án", "Như 8.4, cho lượt khám của bất kỳ bác sĩ nào: các lần sửa hồ sơ "
               "bệnh án, mới nhất trước.", [
                   tham_so([ID_LICH_HEN_MD]),
                   tra("200 OK", f"`duLieu`: {mh('LichSuSuaBenhAnResponse')}. Ví dụ: lượt khám ở 8.4, 1 lần sửa.",
                       ok(LICH_SU)),
                   loi([(404, "KHONG_TIM_THAY", "Lịch hẹn không có, hoặc chưa có hồ sơ bệnh án; `thongDiep` phân biệt "
                                                "2 trường hợp.")]),
                   kit.luu_y(["Đã có hồ sơ bệnh án nhưng chưa sửa lần nào: 200, `cacLanSua` = `[]`, "
                              "`phienBanHienTai` = 1.",
                              "Nên hiện nút xem lịch sử ở lịch hẹn `DA_HOAN_THANH`: lịch hẹn khác chưa có hồ sơ bệnh án "
                              "nên trả 404."])])
    return x


def sua(tl):
    for muc, method, duong, bm, chuc_nang, service in ROUTE:
        tl.them_dong(bsqt_p2_khung.MUC_2, None,
                     bsqt_p2_khung.dong_route(muc, method, duong, bm, "QUAN_TRI_VIEN", chuc_nang, service))
    so = iter(range(tl.id_bookmark_moi(), 10 ** 6))
    bsqt_p2_khung.them_cuoi(tl, muc_9(so))
