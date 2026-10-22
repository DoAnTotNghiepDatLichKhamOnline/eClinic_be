"""Tài liệu 2, bản 1.2 (DOANTOTNGH-14): đặt lịch toàn viện. Thêm 7.6, 7.7 (bác sĩ tìm lịch hẹn theo CCCD, cấp lại liên
kết phiếu khám) và 9.4-9.9 (quản trị viên: danh sách lịch hẹn toàn viện, chi tiết và nhật ký, hủy, đổi lịch, đặt lịch qua
tổng đài, cấp lại liên kết), 31 route; quy ước 3.10, enum mới ở 3.9, mô hình 4.10-4.12; `kenhDat`, `chuyenKhoa` ở 4.2 và
`maSoKham`, `linkSoKham` ở 4.5, kể cả trong JSON mẫu đã có. Phụ lục A dựng lại vì thân bài nhắc thêm mã lỗi. Chạy qua
bsqt_build.py, sau mọi giai đoạn khác.

JSON mẫu lấy từ response thật do bsqt_lay_mau.py ghi lại (temp/api-doc/mau-bsqt, p7_quan_tri_lich_hen); phần lẻ của giây
cắt còn 6 chữ số như các mục trước.
"""

import base64
import hashlib
import html
import json
import re

import bsqt_p2_khung
import bsqt_p5_phu_luc
import kit
from bsqt_p2_khung import RONG_PHIEN_BAN
from bsqt_p2_mo_hinh import RONG_MO_HINH, mo_hinh
from bsqt_p2_quy_uoc import RONG_2
from bsqt_p3_lich_hang_doi import BAC_SI, BS, ID_LICH_HEN, LOI_KHONG_CO, lich_hen
from bsqt_p3_lich_hang_doi import QUYEN as QUYEN_BS
from bsqt_p5_quan_tri import QT
from bsqt_p5_quan_tri import QUYEN as QUYEN_QT
from bsqt_p6_lich_lam_viec_pdf import PHAM_VI as PHAM_VI_CU
from p3_dat_lich import TRANG, body, dan, js, loi, ok, tham_so, tra, trang
from p8_benh_an_ho_so_bac_si import _DONG_CODE
from p9_xac_nhan_ngay_benh_an_day_du import chen_doan_sau, sua_o_cuoi, viet_lai

PHIEN_BAN = "1.2 — 20/10/2026"
BOOKING = "booking-service"
MUC_1 = "1. Giới thiệu và phạm vi"
MUC_4 = "4. Mô hình dữ liệu (response)"
MUC_5 = "5. Lịch làm việc và lịch hẹn của bác sĩ"
MUC_8 = "8. Hồ sơ bệnh án"
MUC_9_CU = "9. Quản trị viên: tra cứu lịch hẹn, đối chiếu giấy tờ, lịch sử sửa hồ sơ bệnh án"
MUC_9 = ("9. Quản trị viên: lịch hẹn toàn viện, đặt lịch qua tổng đài, cấp lại liên kết, tra cứu, đối chiếu giấy tờ, "
         "lịch sử sửa hồ sơ bệnh án")
PHAM_VI = ("31 route của phần lịch hẹn và khám bệnh phía bác sĩ (22 route) và quản trị viên (9 route). "
           "booking-service 24 route (bác sĩ 16, quản trị viên 8): ca làm việc của bác sĩ và bản in PDF, lịch hẹn, tra "
           "cứu lịch hẹn theo mã hoặc CCCD, bác sĩ hủy lịch, hàng đợi khám, hồ sơ khám, sửa thông tin bệnh nhân, đối "
           "chiếu giấy tờ, cấp lại liên kết phiếu khám; lịch hẹn toàn viện của quản trị viên (danh sách, chi tiết và "
           "nhật ký, hủy, đổi lịch, đặt lịch qua tổng đài). medical-service 7 route (bác sĩ 6, quản trị viên 1): ghi, "
           "sửa, xem hồ sơ bệnh án, lịch sử sửa, gợi ý thuốc và mã ICD-10")
LICH_SU = (PHIEN_BAN, "Thêm 7.6, 7.7 (bác sĩ tìm lịch hẹn theo CCCD, cấp lại liên kết phiếu khám) và 9.4-9.9 (quản trị "
                      "viên: danh sách lịch hẹn toàn viện, chi tiết và nhật ký, hủy, đổi lịch thay bệnh nhân, đặt lịch qua "
                      "tổng đài, cấp lại liên kết), 31 route; quy ước 3.10, enum mới ở 3.9, mô hình 4.10-4.12. 4.2 có thêm "
                      "`kenhDat`, `chuyenKhoa`; 4.5 có thêm `maSoKham`, `linkSoKham`; trạng thái `QUAN_TRI_HUY`. Tài liệu "
                      "đi kèm TK nay là bản 2.5.")

# ----- Dữ liệu mẫu (từ response thật) -----

NOI_TQ = {"id": 1, "ten": "Nội tổng quát"}
CHUYEN_KHOA_CUA_BAC_SI = {BAC_SI["id"]: NOI_TQ}
LINK_PK = "http://localhost:5173/phieu-kham/"
LINK_SK = "http://localhost:5173/so-kham/"
NGAY_TD = "2026-11-02"
GOI = {"hoTen": "Phạm Văn Gọi", "ngaySinh": "1985-07-12", "tuoi": 41, "gioiTinh": "NAM", "soDienThoai": "0933200001"}
DEN = {"hoTen": "Lý Thị Đến", "ngaySinh": "1990-02-03", "tuoi": 36, "gioiTinh": "NU", "soDienThoai": "0933200002"}
CCCD_GOI = "079185000201"
LY_DO_DOI = "Bệnh nhân gọi tổng đài xin đổi sang giờ muộn hơn"
LY_DO_QT_HUY = "Bác sĩ đi công tác đột xuất, phòng khám đã gọi báo bệnh nhân"
QTV = {"loai": "QUAN_TRI_VIEN", "idTaiKhoan": 1, "hoTen": "Quản trị viên"}
MA_PK_25 = "ea6Wd-TjWHy5SDTLCm80363BU8Bhp9bEqDxSHtSTCz0"
MA_PK_27 = "H5vNPNPKnIXSycex5WqbQI4LhGoXFdbXSYH6Opb0Bqc"


def qt(lich, kenh="HOTLINE"):
    """Lịch hẹn như quản trị viên nhận: có bacSi, kenhDat, chuyenKhoa."""
    return {**lich, "bacSi": BAC_SI, "kenhDat": kenh, "chuyenKhoa": NOI_TQ}


def bs(lich):
    """Lịch hẹn như bác sĩ nhận: bacSi, chuyenKhoa = null."""
    return {**lich, "bacSi": None, "chuyenKhoa": None}


def td(id_lh, ma, so_thu_tu, gio, benh_nhan, id_ho_so, ly_do, ngay_dat, **khac):
    """Lịch hẹn đặt qua tổng đài vào ca 125 (2/11) của bác sĩ mẫu."""
    return qt(lich_hen(id_lh, ma, so_thu_tu, gio, benh_nhan, id_ho_so, ly_do, ngay=NGAY_TD, id_ca=125,
                       ngay_dat=ngay_dat, **khac))


LH_25 = td(25, "9677", 1, "08:00:00", GOI, 15, "Đau lưng 1 tuần nay", "2026-10-20T20:40:52.535343")
LH_26 = td(26, "1759", 7, "09:00:00", DEN, 16, "Khám tổng quát", "2026-10-20T20:40:52.580548")
LH_27 = td(27, "9141", 13, "10:00:00", GOI, 15, "Đau lưng 1 tuần nay", "2026-10-20T20:40:52.875822")
LH_26_HUY = {**LH_26, "trangThai": "QUAN_TRI_HUY", "lyDoHuy": LY_DO_QT_HUY}


def mat_xich(lich, la_lich_nay):
    return {"id": lich["id"], "maTraCuu": lich["maTraCuu"], "trangThai": lich["trangThai"],
            "gioKhamDuKien": lich["gioKhamDuKien"], "laLichNay": la_lich_nay}


def su_kien(thoi_diem, lich, loai, ly_do=None, lien_quan=None):
    return {"thoiDiem": f"2026-10-20T20:40:{thoi_diem}", "maTraCuu": lich["maTraCuu"], "suKien": loai,
            "nguoiThucHien": QTV, "lyDo": ly_do, "maTraCuuLienQuan": lien_quan}


# 9.5 (cũng là response của 9.7): lịch tổng đài đã cấp lại liên kết trên màn hình rồi đổi sang 10:00; đang xem lịch mới
CT_27 = {"lichHen": LH_27, "emailLienHe": None, "taiKhoanDat": None, "ngonNgu": "VI", "quanTriHuyDoiDuoc": True,
         "chuoiDoiLich": [mat_xich({**LH_25, "trangThai": "DA_HUY_DO_DOI_LICH"}, False), mat_xich(LH_27, True)],
         "lichSu": [su_kien("52.536372", LH_25, "DAT_LICH"),
                    su_kien("52.803011", LH_25, "CAP_LAI_LIEN_KET", "Cấp lại liên kết trên màn hình tại quầy."),
                    su_kien("52.878966", LH_25, "DOI_LICH", LY_DO_DOI, LH_27["maTraCuu"])]}
# 9.6: lịch tổng đài có email, ngôn ngữ email EN, đã cấp lại liên kết qua email rồi bị hủy
CT_26_HUY = {"lichHen": LH_26_HUY, "emailLienHe": "den@example.com", "taiKhoanDat": None, "ngonNgu": "EN",
             "quanTriHuyDoiDuoc": False, "chuoiDoiLich": [mat_xich(LH_26_HUY, True)],
             "lichSu": [su_kien("52.581696", LH_26, "DAT_LICH"),
                        su_kien("52.821721", LH_26, "CAP_LAI_LIEN_KET",
                                "Gửi lại liên kết qua email tới d***@example.com."),
                        su_kien("52.936436", LH_26, "HUY", LY_DO_QT_HUY)]}
DAT_TD = {"idLichLamViec": 125, "gioBatDauKhung": f"{NGAY_TD}T08:00:00",
          "benhNhan": {"hoTen": "Phạm Văn Gọi", "ngaySinh": "1985-07-12", "gioiTinh": "NAM",
                       "soDienThoai": "0933200001", "cccd": CCCD_GOI},
          "lyDoKham": "Đau lưng 1 tuần nay", "ngonNgu": "VI"}
TRA_TD = {"idLichHen": 25, "phieuKham": {
    "maPhieuKham": MA_PK_25, "maTraCuu": LH_25["maTraCuu"], "linkPhieuKham": LINK_PK + MA_PK_25, "soThuTu": 1,
    "ngay": NGAY_TD, "gioKhamDuKien": f"{NGAY_TD}T08:00:00", "gioBatDauKhung": f"{NGAY_TD}T08:00:00",
    "gioKetThucKhung": f"{NGAY_TD}T09:00:00", "trangThai": "DA_XAC_NHAN", "bacSi": BAC_SI,
    "phongKham": LH_25["phongKham"], "hoTenBenhNhan": "Phạm Văn Gọi", "hoTenNguoiGiamHo": None,
    "luuVaoTaiKhoan": False, "bacSiDoPhongKhamXep": False}}
CAP_LAI_MH = {"idLichHen": 25, "maTraCuu": LH_25["maTraCuu"], "cachGui": "MAN_HINH", "maPhieuKham": MA_PK_25,
              "lienKetPhieuKham": LINK_PK + MA_PK_25}
CAP_LAI_EMAIL = {"idLichHen": 26, "maTraCuu": LH_26["maTraCuu"], "cachGui": "EMAIL", "emailDaGui": "d***@example.com"}
CAP_LAI_BS = {"idLichHen": 27, "maTraCuu": LH_27["maTraCuu"], "cachGui": "MAN_HINH", "maPhieuKham": MA_PK_27,
              "lienKetPhieuKham": LINK_PK + MA_PK_27}

# ----- Bảng route ở mục 2 -----

ROUTE_BS = [
    ("7.6", "GET", f"{BS}/lich-hen/theo-cccd", "ep_bs_theo_cccd", "Tìm lịch hẹn của tôi theo CCCD"),
    ("7.7", "POST", f"{BS}/lich-hen/{{id}}/cap-lai-lien-ket", "ep_bs_cap_lai_lien_ket", "Cấp lại liên kết phiếu khám"),
]
ROUTE_QT = [
    ("9.4", "GET", QT, "ep_qt_lich_hen_toan_vien", "Lịch hẹn toàn viện, có lọc, phân trang"),
    ("9.5", "GET", f"{QT}/{{id}}", "ep_qt_lich_hen_chi_tiet", "Chi tiết lịch hẹn, chuỗi đổi lịch, nhật ký"),
    ("9.6", "POST", f"{QT}/{{id}}/huy", "ep_qt_huy_lich_hen", "Hủy lịch hẹn thay bệnh nhân, kèm lý do"),
    ("9.7", "POST", f"{QT}/{{id}}/doi-lich", "ep_qt_doi_lich_hen", "Đổi lịch hẹn thay bệnh nhân, kèm lý do"),
    ("9.8", "POST", QT, "ep_qt_dat_lich_tong_dai", "Đặt lịch qua tổng đài / tại quầy"),
    ("9.9", "POST", f"{QT}/{{id}}/cap-lai-lien-ket", "ep_qt_cap_lai_lien_ket", "Cấp lại liên kết phiếu khám"),
]
R = {muc: (method, duong, bm) for muc, method, duong, bm, _ in ROUTE_BS + ROUTE_QT}


def route(so, muc, ten, mo_ta, than):
    method, duong, bm = R[muc]
    return kit.route(muc, ten, method, duong, bm, next(so), QUYEN_BS if muc.startswith("7.") else QUYEN_QT, mo_ta,
                     than)


def mh(so, ten):
    return f"`{ten}` ({so})"


# ----- Mục 3 -----

ENUM = [
    ("KenhDatLich", "Lịch hẹn được đặt qua kênh nào: `KHACH` (khách không đăng nhập), `TAI_KHOAN` (bệnh nhân đã đăng "
                    "nhập, cho mình hoặc người thân), `HOTLINE` (quản trị viên đặt qua tổng đài / tại quầy, 9.8). Đổi lịch "
                    "giữ kênh của lịch cũ. Lịch hẹn có trước bản 1.2: có tài khoản đặt là `TAI_KHOAN`, còn lại `KHACH`."),
    ("SuKienLichHen", "1 dòng nhật ký lịch hẹn (3.10): `DAT_LICH`; `DOI_LICH` (dòng ở lịch cũ, `maTraCuuLienQuan` là "
                      "lịch mới); `HUY` (ai hủy: xem `trangThai` và người thực hiện); `CA_BI_HUY` (ca bị hủy, lịch chờ đổi); "
                      "`DOI_BAC_SI` (ca được nhường cho bác sĩ khác); `CA_THAY_DOI` (ca đổi phòng hoặc giờ); "
                      "`CAP_LAI_LIEN_KET` (cấp lại liên kết, lý do nêu cách gửi). Không có dòng cho \"đã khám\", \"không "
                      "đến\": trạng thái lịch hẹn đã cho biết."),
    ("LoaiNguoiThucHien", "Ai làm thao tác: `KHACH` (khách đặt lịch, hoặc người cầm link phiếu khám), `TAI_KHOAN` (tài "
                          "khoản bệnh nhân), `BAC_SI`, `QUAN_TRI_VIEN`."),
    ("CachCapLai", "`EMAIL`: gửi link tới email của lịch hẹn. `MAN_HINH`: trả mã và link để mở mã QR trên màn hình cho "
                   "người đang ở quầy (3.10)."),
]

QUY_TAC_3_10 = [
    ("Danh sách (9.4)", "Lịch hẹn của mọi bác sĩ có giờ khám trong `[tuNgay, denNgay]`, giờ khám sớm nhất trước, phân "
                        "trang (TK 3.3). Không gửi `tuNgay`: từ hôm nay; không gửi `denNgay`: `tuNgay` + 30 ngày. Khoảng "
                        "tối đa 366 ngày tính cả hai đầu. Các bộ lọc dùng cùng lúc được (và)."),
    ("`tuKhoa` của 9.4", "Mã tra cứu đầy đủ `ECL-<yyyyMMdd>-<số>` (không phân biệt hoa thường): đúng lịch hẹn đó. Đúng 12 "
                         "chữ số: CCCD của người khám hoặc của người giám hộ. Dạng khác: họ tên người khám, số điện thoại "
                         "hoặc email liên hệ chứa từ khoá. Khoảng trắng hai đầu được bỏ. Tìm theo mã hay CCCD vẫn chỉ trong "
                         "khoảng ngày đang xem; tra 1 mã bất kể ngày: 9.1."),
    ("Hủy (9.6), đổi lịch (9.7)", "Chỉ lịch `DA_XAC_NHAN` (khác: 409 `LICH_HEN_KHONG_HUY_DOI_DUOC`) chưa tới giờ bắt đầu "
                                  "khung giờ khám (đã tới: 409 `LICH_HEN_DA_QUA_GIO`); lịch đang chờ đổi vì ca bị hủy "
                                  "(`canDoiLich` = `true`) thì luôn được. Không theo hạn hủy / đổi và số lần đổi của bệnh "
                                  "nhân (TK 3.6). `quanTriHuyDoiDuoc` (4.10) cho biết trước. `lyDo` bắt buộc, tối đa 500 "
                                  "ký tự."),
    ("Sau khi hủy", "Lịch thành `QUAN_TRI_HUY`, `lyDoHuy` là lý do quản trị viên ghi (bệnh nhân đọc được trên phiếu khám), "
                    "lượt khám được mở lại cho người khác đặt. Bệnh nhân nhận thông báo `LICH_HEN_QUAN_TRI_HUY` và email "
                    "(TK 3.13); bác sĩ của lịch hẹn nhận thông báo."),
    ("Sau khi đổi lịch", "Lịch cũ thành `DA_HUY_DO_DOI_LICH`; lịch mới `DA_XAC_NHAN`, có `id`, mã tra cứu và mã phiếu khám "
                         "mới, giữ lý do khám, kênh đặt, email, ngôn ngữ email và cờ đối chiếu của lịch cũ. Khung giờ mới "
                         "theo đúng quy tắc đặt lịch (TK 13.1). `lyDo` chỉ ghi vào nhật ký, bệnh nhân không đọc được. Bệnh "
                         "nhân nhận thông báo `LICH_HEN_QUAN_TRI_DOI` và email theo ngôn ngữ của lịch cũ (TK 3.13); bác sĩ "
                         "nhận thông báo."),
    ("Đặt qua tổng đài (9.8)", "Cùng body và quy tắc với khách đặt (TK 13.1): khung giờ, CCCD, người giám hộ, giới hạn số "
                               "lịch theo người khám và theo số điện thoại (TK 3.6). Không giới hạn số lần gọi theo địa "
                               "chỉ IP. Lịch không gắn tài khoản nào (`taiKhoanDat` = `null`), `kenhDat` = `HOTLINE`, nhật "
                               "ký ghi quản trị viên đã đặt. Có `benhNhan.email` thì phiếu khám được gửi tới đó theo "
                               "`ngonNgu`."),
    ("Mã phiếu khám", "Chỉ response của 9.8 và của cấp lại liên kết trên màn hình (7.7, 9.9) có mã phiếu khám; danh sách, "
                      "chi tiết, tra cứu, hủy, đổi lịch không có (TK 3.10)."),
    ("Nhật ký (`lichSu`)", "Mỗi thao tác trên lịch hẹn: việc gì (`SuKienLichHen`), ai làm (`LoaiNguoiThucHien`), lúc nào, "
                           "lý do; gồm cả các lịch của chuỗi đổi lịch, cũ nhất trước. Chỉ có thao tác từ bản 1.2: lịch hẹn "
                           "cũ hơn có `lichSu` rỗng hoặc thiếu các bước đầu."),
    ("Cấp lại liên kết (7.7, 9.9)", "Cho người đã mất link phiếu khám, đến quầy hoặc gọi điện, xuất trình CCCD. `cccd` "
                                    "phải là CCCD của người khám hoặc của người giám hộ trên lịch hẹn (sai: 403 "
                                    "`CCCD_KHONG_KHOP`). Luôn cấp cho lịch hẹn hiện hành của chuỗi đổi lịch (chọn lịch cũ "
                                    "đã đổi thì được lịch mới nhất). Dùng lại mã cũ, không sinh mã mới; lịch ở trạng thái "
                                    "nào cũng được. Mỗi lần ghi 1 dòng `CAP_LAI_LIEN_KET` vào nhật ký."),
    ("`cachGui`", "`EMAIL`: gửi link phiếu khám (và link sổ khám bệnh nếu đã khám) tới email của lịch hẹn; response chỉ có "
                  "địa chỉ đã che. Lịch không có email (vd đặt qua tổng đài không nhập email): 409 `KHONG_CO_EMAIL_NHAN`, "
                  "dùng `MAN_HINH`. `MAN_HINH`: response có `maPhieuKham`, `lienKetPhieuKham` (và `maSoKham`, "
                  "`lienKetSoKham` nếu đã khám) để mở mã QR: TK 14.2, TK 20.5. Khách tự xin gửi lại link qua email: TK "
                  "20.6."),
    ("Tìm theo CCCD (7.6)", "Lịch hẹn của chính bác sĩ mà người khám hoặc người giám hộ có số CCCD này, mọi trạng thái, "
                            "giờ khám muộn nhất trước, tối đa 50. Quản trị viên tìm bằng `tuKhoa` của 9.4."),
]


def muc_3_10():
    return (kit.tieu_de(2, "3.10 Lịch hẹn toàn viện và cấp lại liên kết")
            + kit.doan("Quy tắc của các route quản trị viên quản lý lịch hẹn thay bệnh nhân (9.4-9.9) và của cấp lại liên "
                       "kết phiếu khám (7.6, 7.7, 9.9).")
            + kit.bang(RONG_2, ("Việc", "Quy tắc"), QUY_TAC_3_10) + kit.khoang_trong(60))


# ----- Mục 4 -----

MO_HINH_MOI = [
    ("LichHenQuanTriChiTietResponse", "Chi tiết 1 lịch hẹn cho quản trị viên (9.5), cũng là response của hủy (9.6) và "
                                      "đổi lịch (9.7). Không có mã phiếu khám (3.10).", [
        ("lichHen", "LichHenTrongCaResponse", "4.2, `bacSi` và `chuyenKhoa` có giá trị."),
        ("emailLienHe", "string", "Email người đặt nhập; `null` nếu để trống."),
        ("taiKhoanDat", "TaiKhoanDat", "Tài khoản đã đặt; `null`: khách hoặc tổng đài đặt."),
        ("ngonNgu", "enum NgonNgu", "Ngôn ngữ email gửi cho người đặt (TK 3.12); `null` ở lịch hẹn tạo trước khi lưu "
                                    "ngôn ngữ."),
        ("quanTriHuyDoiDuoc", "boolean", "`true`: quản trị viên hủy / đổi được lịch này (3.10). Dùng để ẩn / hiện nút."),
        ("chuoiDoiLich", "MatXich[]", "Các lịch hẹn của cùng 1 chuỗi đổi lịch, gồm lịch này, cũ nhất trước; 1 phần tử "
                                      "nếu chưa đổi."),
        ("lichSu", "SuKien[]", "Nhật ký thao tác của cả chuỗi, cũ nhất trước (3.10)."),
    ], (
        ("TaiKhoanDat", "giá trị của `taiKhoanDat`.", [
            ("id", "number", "Id tài khoản."),
            ("hoTen", "string", ""),
            ("email", "string", "Email đăng nhập."),
        ]),
        ("MatXich", "phần tử của `chuoiDoiLich`.", [
            ("id", "number", "Id lịch hẹn; mở chi tiết bằng 9.5."),
            ("maTraCuu", "string", ""),
            ("trangThai", "enum TrangThaiLichHen", "TK 3.11."),
            ("gioKhamDuKien", "datetime", ""),
            ("laLichNay", "boolean", "`true` ở lịch hẹn đang xem."),
        ]),
        ("SuKien", "phần tử của `lichSu`.", [
            ("thoiDiem", "datetime", ""),
            ("maTraCuu", "string", "Lịch hẹn của thao tác (trong chuỗi)."),
            ("suKien", "enum SuKienLichHen", "3.9."),
            ("nguoiThucHien", "NguoiLam", ""),
            ("lyDo", "string", "Lý do người làm ghi (hủy, đổi lịch) giữ nguyên; câu của hệ thống (vd cấp lại liên kết) "
                               "theo `Accept-Language`. `null` nếu không có."),
            ("maTraCuuLienQuan", "string", "`DOI_LICH`: mã tra cứu của lịch mới; `null` ở các thao tác khác."),
        ]),
        ("NguoiLam", "giá trị của `nguoiThucHien`.", [
            ("loai", "enum LoaiNguoiThucHien", "3.9."),
            ("idTaiKhoan", "number", "`null` khi `loai` = `KHACH`."),
            ("hoTen", "string", "Họ tên của tài khoản đã làm; `null` khi `loai` = `KHACH`."),
        ]),
    ), ()),
    ("DatLichHotlineResponse", "Kết quả đặt lịch qua tổng đài (9.8): response duy nhất cho quản trị viên có mã phiếu "
                               "khám, và chỉ lúc vừa đặt.", [
        ("idLichHen", "number", "Id lịch hẹn, để mở chi tiết (9.5)."),
        ("phieuKham", "DatLichResponse", "TK 4.21: mã phiếu khám, mã tra cứu, link phiếu khám, số thứ tự, giờ khám dự "
                                         "kiến, bác sĩ, phòng khám."),
    ], (), ()),
    ("CapLaiLienKetResponse", "Kết quả cấp lại liên kết (7.7, 9.9), luôn của lịch hẹn hiện hành trong chuỗi đổi lịch. "
                              "Trường không có giá trị thì **không có mặt** trong JSON (khác TK 3.1).", [
        ("idLichHen", "number", "Lịch hẹn hiện hành; khác `{id}` đã gửi khi lịch đó đã được đổi."),
        ("maTraCuu", "string", ""),
        ("cachGui", "enum CachCapLai", "3.9."),
        ("emailDaGui", "string", "Chỉ khi `EMAIL`: địa chỉ đã che, vd `d***@example.com`."),
        ("maPhieuKham", "string", "Chỉ khi `MAN_HINH`: mở mã QR bằng TK 14.2."),
        ("lienKetPhieuKham", "string", "Chỉ khi `MAN_HINH`: link phiếu khám (TK 3.10)."),
        ("maSoKham", "string", "Chỉ khi `MAN_HINH` và lịch đã khám: mã QR ở TK 20.5."),
        ("lienKetSoKham", "string", "Như `maSoKham`: link sổ khám bệnh (TK 20.3)."),
    ], (), ()),
]
SO_MO_HINH_MOI = {ten: f"4.{i}" for i, (ten, _, _, _, _) in enumerate(MO_HINH_MOI, 10)}


# ----- JSON mẫu đã có: thêm trường mới -----

def _ma_so_kham(ma_tra_cuu):
    """Mã sổ khám bệnh 43 ký tự cho mẫu, cố định theo mã tra cứu (mã thật do máy chủ sinh ngẫu nhiên)."""
    return base64.urlsafe_b64encode(hashlib.sha256(ma_tra_cuu.encode()).digest()).decode().rstrip("=")


def _them_truong_moi(gia_tri):
    """Lịch hẹn (có canDoiLich, doiChieu) thêm kenhDat, chuyenKhoa ở cuối; hồ sơ bệnh án (có hanSua, trangThaiLichHen)
    thêm maSoKham, linkSoKham sau hanSua."""
    if isinstance(gia_tri, list):
        for v in gia_tri:
            _them_truong_moi(v)
        return
    if not isinstance(gia_tri, dict):
        return
    for v in gia_tri.values():
        _them_truong_moi(v)
    if "canDoiLich" in gia_tri and "doiChieu" in gia_tri and "kenhDat" not in gia_tri:
        benh_nhan = gia_tri["benhNhan"]
        khach = benh_nhan["hoTen"] == "Lê Minh Anh" or (benh_nhan["soDienThoai"] or "").startswith("09331")
        gia_tri["kenhDat"] = "KHACH" if khach else "TAI_KHOAN"
        gia_tri["chuyenKhoa"] = None if gia_tri["bacSi"] is None else CHUYEN_KHOA_CUA_BAC_SI[gia_tri["bacSi"]["id"]]
    if "hanSua" in gia_tri and "trangThaiLichHen" in gia_tri and "maSoKham" not in gia_tri:
        ma = _ma_so_kham(gia_tri["maTraCuu"])
        moi = {}
        for k, v in gia_tri.items():
            moi[k] = v
            if k == "hanSua":
                moi.update({"maSoKham": ma, "linkSoKham": LINK_SK + ma})
        gia_tri.clear()
        gia_tri.update(moi)


def sua_moi_mau(tl, dau_muc, cuoi_muc):
    """Như p8.sua_json_mau nhưng cho cả đoạn từ heading dau_muc tới heading cuoi_muc; trả số khối đã viết lại."""
    dau, cuoi = tl.vi_tri_tieu_de(dau_muc), tl.vi_tri_tieu_de(cuoi_muc)
    doan = tl.xml[dau:cuoi]
    so = 0
    for khoi in re.finditer(rf"(?:{_DONG_CODE.pattern})+", tl.xml[dau:cuoi]):
        chu = "\n".join(html.unescape(m.group(1)) for m in _DONG_CODE.finditer(khoi.group(0)))
        try:
            mau = json.loads(chu)
        except ValueError:
            continue
        if not (isinstance(mau, dict) and mau.get("thanhCong") is True and "duLieu" in mau):
            continue
        truoc = json.dumps(mau, ensure_ascii=False)
        _them_truong_moi(mau["duLieu"])
        if json.dumps(mau, ensure_ascii=False) == truoc:
            continue
        dong = json.dumps(mau, ensure_ascii=False, indent=2).split("\n")
        doan = doan.replace(khoi.group(0), "".join(kit.dong_code(d, i == len(dong) - 1) for i, d in enumerate(dong)),
                            1)
        so += 1
    tl.xml = tl.xml[:dau] + doan + tl.xml[cuoi:]
    return so


# ----- Mục 7, 9 -----

CCCD = ("cccd", "query", "string", "Bắt buộc, đúng 12 chữ số.")
BODY_CAP_LAI = [
    ("cccd", "string", "Có", "Đúng 12 chữ số: CCCD trên giấy tờ người đó xuất trình, của người khám hoặc của người giám "
                             "hộ ghi trên lịch hẹn."),
    ("cachGui", "enum CachCapLai", "Có", "`EMAIL` hoặc `MAN_HINH` (3.9, 3.10)."),
]
LOI_CAP_LAI = [
    (400, "DU_LIEU_KHONG_HOP_LE", "`cccd` không đúng 12 chữ số hoặc thiếu `cachGui` (`chiTiet` nêu trường)."),
    (403, "CCCD_KHONG_KHOP", "`cccd` không phải CCCD của người khám hay người giám hộ trên lịch hẹn."),
    (409, "KHONG_CO_EMAIL_NHAN", "`cachGui` = `EMAIL` mà lịch hẹn không có email: cấp lại bằng `MAN_HINH`."),
]
LUU_Y_MA = "Response `MAN_HINH` có mã phiếu khám: chỉ hiện mã QR cho người đang ở quầy, không lưu lại, không ghi log."
TRA_CHI_TIET = f"`duLieu`: {mh('4.10', 'LichHenQuanTriChiTietResponse')}"
LOI_KHONG_CO_QT = (404, "KHONG_TIM_THAY", "Lịch hẹn không có.")
LOI_HUY_DOI = [
    (409, "LICH_HEN_KHONG_HUY_DOI_DUOC", "Lịch hẹn không còn `DA_XAC_NHAN`: đã khám, đã hủy, đã đổi sang lịch khác, "
                                         "không đến (bấm lần 2 cũng vậy)."),
    (409, "LICH_HEN_DA_QUA_GIO", "Đã tới giờ bắt đầu khung giờ khám, trừ lịch đang chờ đổi vì ca bị hủy."),
]
LUU_Y_THONG_BAO = ("Thông báo và email cho bệnh nhân đi qua notification-service; service đó chưa chạy thì thông báo "
                   "được giữ lại và gửi khi service chạy, thao tác vẫn thành công (3.2).")


def muc_7_6_7_7(so):
    x = route(so, "7.6", "Tìm lịch hẹn của tôi theo CCCD", "Lịch hẹn của bác sĩ mà người khám hoặc người giám hộ có số "
              "CCCD này, mọi trạng thái, giờ khám muộn nhất trước, tối đa 50. Dùng khi người đến quầy xin cấp lại liên kết "
              "phiếu khám (7.7) mà không còn mã.", [
                  tham_so([CCCD]),
                  tra("200 OK", f"`duLieu`: danh sách {mh('4.2', 'LichHenTrongCaResponse')}, `bacSi`, `chuyenKhoa` = "
                                "`null`. Ví dụ: người gọi tổng đài đã đổi lịch 1 lần (9.7).",
                      ok([bs(LH_27), bs({**LH_25, "trangThai": "DA_HUY_DO_DOI_LICH"})])),
                  loi([(400, "DU_LIEU_KHONG_HOP_LE", "Thiếu `cccd` hoặc không đúng 12 chữ số.")]),
                  kit.luu_y(["Không có lịch hẹn nào khớp: `duLieu` = `[]`, không phải lỗi.",
                             "Chỉ lịch hẹn của chính bác sĩ (3.3); lịch hẹn của người đó với bác sĩ khác không có ở đây."])])
    x += route(so, "7.7", "Cấp lại liên kết phiếu khám", "Như 9.9, cho lịch hẹn của chính bác sĩ: người đã mất link xuất "
               "trình CCCD, bác sĩ gửi lại link qua email hoặc mở mã QR trên màn hình. Quy tắc: 3.10.", [
                   tham_so([ID_LICH_HEN]),
                   body(BODY_CAP_LAI),
                   dan("Ví dụ:"), js({"cccd": CCCD_GOI, "cachGui": "MAN_HINH"}),
                   tra("200 OK", f"`duLieu`: {mh('4.12', 'CapLaiLienKetResponse')} của lịch hẹn hiện hành. Ví dụ: "
                                 "`MAN_HINH`, lịch chưa khám.", ok(CAP_LAI_BS, "Đã cấp lại liên kết phiếu khám")),
                   loi([LOI_CAP_LAI[0], LOI_CAP_LAI[1], LOI_KHONG_CO, LOI_CAP_LAI[2]]),
                   kit.luu_y([LUU_Y_MA])])
    return x


def muc_9_moi(so):
    x = route(so, "9.4", "Lịch hẹn toàn viện", "Lịch hẹn của mọi bác sĩ trong khoảng ngày, có bộ lọc và phân trang, giờ "
              "khám sớm nhất trước. Quy tắc: 3.10.", [
                  tham_so([("tuNgay", "query", "date", "Không bắt buộc, mặc định hôm nay."),
                           ("denNgay", "query", "date", "Không bắt buộc, mặc định `tuNgay` + 30 ngày; khoảng tối đa 366 "
                                                        "ngày tính cả hai đầu."),
                           ("idChuyenKhoa", "query", "number", "Chuyên khoa của bác sĩ."),
                           ("idBacSi", "query", "number", ""),
                           ("idPhongKham", "query", "number", ""),
                           ("trangThai", "query", "enum", "`TrangThaiLichHen` (TK 3.7)."),
                           ("kenhDat", "query", "enum", "`KenhDatLich` (3.9)."),
                           ("canDoiLich", "query", "boolean", "`true`: chỉ lịch đang chờ đổi vì ca bị hủy."),
                           ("canDoiChieu", "query", "boolean", "`true`: chỉ lịch cần đối chiếu giấy tờ (3.6)."),
                           ("tuKhoa", "query", "string", "Mã tra cứu, CCCD, hoặc 1 phần họ tên, số điện thoại, email "
                                                         "(3.10).")] + TRANG),
                  tra("200 OK", f"`duLieu`: trang (TK 3.3) của {mh('4.2', 'LichHenTrongCaResponse')}, `bacSi` và "
                                "`chuyenKhoa` có giá trị. Ví dụ: `?tuNgay=2026-10-20&denNgay=2026-11-02&idBacSi=1&kenhDat"
                                "=HOTLINE&trangThai=DA_XAC_NHAN`.", ok(trang([LH_25, LH_26], 2))),
                  loi([(400, "DU_LIEU_KHONG_HOP_LE", "`denNgay` trước `tuNgay`, khoảng dài hơn 366 ngày, `trang` < 0, "
                                                     "`kichThuoc` ngoài 1..100, hoặc tham số sai kiểu (vd `trangThai` "
                                                     "không có trong enum).")]),
                  kit.luu_y(["Không có lịch hẹn nào khớp: trang rỗng, không phải lỗi.",
                             "Tìm lịch cũ theo mã hay CCCD: mở rộng `tuNgay`, `denNgay`, hoặc tra mã bằng 9.1.",
                             "`id` của mỗi dòng dùng cho 9.5-9.7, 9.9."])])
    x += route(so, "9.5", "Chi tiết lịch hẹn và nhật ký", "1 lịch hẹn kèm email liên hệ, tài khoản đặt, ngôn ngữ email, "
               "chuỗi đổi lịch và nhật ký thao tác (ai, lúc nào, lý do) của cả chuỗi.", [
                   tham_so([ID_LICH_HEN]),
                   tra("200 OK", f"{TRA_CHI_TIET}. Ví dụ: lịch đặt qua tổng đài, đã cấp lại liên kết trên màn hình rồi "
                                 "đổi sang 10:00 (9.7); đang xem lịch mới.", ok(CT_27)),
                   loi([LOI_KHONG_CO_QT]),
                   kit.luu_y(["Dùng `quanTriHuyDoiDuoc` để ẩn / hiện nút hủy, đổi lịch (3.10).",
                              "Bấm 1 phần tử của `chuoiDoiLich` để mở chi tiết lịch đó bằng `id`.",
                              "Không có mã phiếu khám (3.10): cần đưa lại link cho bệnh nhân thì dùng 9.9."])])
    x += route(so, "9.6", "Hủy lịch hẹn thay bệnh nhân", "Hủy 1 lịch hẹn `DA_XAC_NHAN` kèm lý do, không theo hạn hủy của "
               "bệnh nhân. Lịch thành `QUAN_TRI_HUY`, lượt khám được mở lại, bệnh nhân và bác sĩ được báo. Quy tắc: 3.10.", [
                   tham_so([ID_LICH_HEN]),
                   body([("lyDo", "string", "Có", "Không để trống, tối đa 500 ký tự. Bệnh nhân đọc được trên phiếu khám "
                                                  "(`lyDoHuy`), trong thông báo và email.")]),
                   dan("Ví dụ:"), js({"lyDo": LY_DO_QT_HUY}),
                   tra("200 OK", f"{TRA_CHI_TIET} của lịch vừa hủy: `trangThai` = `QUAN_TRI_HUY`, `quanTriHuyDoiDuoc` = "
                                 "`false`, nhật ký có thêm `HUY`. Ví dụ: lịch đặt qua tổng đài có email, ngôn ngữ email "
                                 "EN.", ok(CT_26_HUY, "Đã hủy lịch hẹn, bệnh nhân đã được báo")),
                   loi([(400, "DU_LIEU_KHONG_HOP_LE", "`lyDo` trống hoặc dài hơn 500 ký tự (`chiTiet` nêu trường "
                                                      "`lyDo`)."), LOI_KHONG_CO_QT] + LOI_HUY_DOI),
                   kit.luu_y([LUU_Y_THONG_BAO])])
    x += route(so, "9.7", "Đổi lịch hẹn thay bệnh nhân", "Đổi 1 lịch hẹn `DA_XAC_NHAN` sang khung giờ khác kèm lý do, "
               "không theo hạn đổi và số lần đổi của bệnh nhân. Lịch cũ thành `DA_HUY_DO_DOI_LICH`, tạo lịch mới. Quy tắc: "
               "3.10.", [
                   tham_so([ID_LICH_HEN]),
                   body([("idLichLamViec", "number", "Một trong hai", "Khung giờ của 1 bác sĩ: id ca (TK 12.1, TK 12.3)."),
                         ("idChuyenKhoa", "number", "Một trong hai", "Bác sĩ bất kỳ của chuyên khoa (TK 12.4)."),
                         ("gioBatDauKhung", "datetime", "Có", "Giờ bắt đầu khung 1 giờ muốn đổi sang (TK 13.1)."),
                         ("lyDo", "string", "Có", "Không để trống, tối đa 500 ký tự. Chỉ ghi vào nhật ký (`lichSu`), "
                                                  "bệnh nhân không đọc được.")]),
                   dan("Ví dụ:"), js({"idLichLamViec": 125, "gioBatDauKhung": f"{NGAY_TD}T10:00:00", "lyDo": LY_DO_DOI}),
                   kit.nhan("RESPONSE THÀNH CÔNG — 201 Created"),
                   kit.doan(f"{TRA_CHI_TIET} của **lịch mới** (`DA_XAC_NHAN`, `id` và mã tra cứu mới); `chuoiDoiLich` có "
                            "lịch cũ `DA_HUY_DO_DOI_LICH`, nhật ký có thêm `DOI_LICH` với `maTraCuuLienQuan` là mã của "
                            "lịch mới. Mẫu ở 9.5 (đúng response của lần đổi này), `thongDiep` = \"Đã đổi lịch hẹn, bệnh "
                            "nhân đã được báo\"."),
                   loi([(400, "DU_LIEU_KHONG_HOP_LE", "`lyDo` trống hoặc quá dài, thiếu `gioBatDauKhung` (`chiTiet` nêu "
                                                      "trường); gửi cả hai hoặc không gửi cái nào trong `idLichLamViec`, "
                                                      "`idChuyenKhoa`; `gioBatDauKhung` không phải giờ bắt đầu 1 khung "
                                                      "(`thongDiep` nói rõ)."),
                        (404, "KHONG_TIM_THAY", "Lịch hẹn hoặc `idLichLamViec` không có.")] + LOI_HUY_DOI + [
                        (409, "KHUNG_GIO_KHONG_KHA_DUNG", "Khung giờ mới không còn nhận đặt (như TK 15.5)."),
                        (409, "KHUNG_GIO_KHONG_CON_TRONG", "Khung giờ mới đã đủ lượt khám."),
                        (409, "LICH_HEN_TRUNG_GIO", "Người khám đã có lịch hẹn khác còn hiệu lực trong khung giờ mới.")]),
                   kit.luu_y([LUU_Y_THONG_BAO,
                              "Lịch cũ không bị đổi khi gặp lỗi: chọn khung khác rồi gửi lại."])])
    x += route(so, "9.8", "Đặt lịch qua tổng đài", "Quản trị viên đặt lịch thay người gọi tổng đài hoặc người đến quầy, "
               "như khách đặt (TK 13.1). Lịch `DA_XAC_NHAN` ngay, `kenhDat` = `HOTLINE`. Quy tắc: 3.10.", [
                   body([("idLichLamViec", "number", "Một trong hai", "Chọn bác sĩ: id ca (TK 13.1)."),
                         ("idChuyenKhoa", "number", "Một trong hai", "Bác sĩ bất kỳ của chuyên khoa (TK 13.1)."),
                         ("gioBatDauKhung", "datetime", "Có", "Như TK 13.1."),
                         ("benhNhan", "object", "Có", "Người khám, đủ các trường của `benhNhan` ở TK 13.1. `benhNhan.email` "
                                                      "không bắt buộc: có thì phiếu khám được gửi tới đó."),
                         ("nguoiGiamHo", "object", "Dưới 18 tuổi", "Như TK 13.1."),
                         ("lyDoKham", "string", "Có", "Không rỗng, tối đa 500 ký tự."),
                         ("ngonNgu", "enum NgonNgu", "Không", "`VI` / `EN`: ngôn ngữ người gọi chọn cho email phiếu khám; "
                                                              "bỏ trống = `VI`.")]),
                   dan("Ví dụ (người gọi không có email):"), js(DAT_TD),
                   tra("201 Created", f"`duLieu`: {mh('4.11', 'DatLichHotlineResponse')}.",
                       ok(TRA_TD, "Đã đặt lịch qua tổng đài")),
                   loi([(400, "DU_LIEU_KHONG_HOP_LE", "Body sai quy tắc như TK 13.1 (`chiTiet` liệt kê từng trường)."),
                        (400, "THIEU_NGUOI_GIAM_HO", "Người khám dưới 18 tuổi mà không gửi `nguoiGiamHo`."),
                        (400, "NGUOI_GIAM_HO_KHONG_HOP_LE", "Như TK 13.1."),
                        (404, "KHONG_TIM_THAY", "`idLichLamViec` không có."),
                        (409, "KHUNG_GIO_KHONG_KHA_DUNG", "Như TK 13.1."),
                        (409, "KHUNG_GIO_KHONG_CON_TRONG", "Như TK 13.1."),
                        (409, "LICH_HEN_TRUNG_GIO", "Người khám đã có lịch hẹn còn hiệu lực trong khung giờ này."),
                        (409, "VUOT_GIOI_HAN_DAT_LICH", "Người khám hoặc số điện thoại liên hệ đã giữ đủ số lịch sắp tới "
                                                        "(TK 3.6).")]),
                   kit.luu_y(["Response là lần duy nhất quản trị viên thấy mã phiếu khám: in phiếu hoặc mở mã QR (TK 14.2) "
                              "ngay, đọc mã tra cứu cho người gọi. Sau đó chỉ lấy lại được bằng 9.9.",
                              "`ngonNgu` chỉ chọn ngôn ngữ email; câu chữ của response theo `Accept-Language`.",
                              "Người khám đã có hồ sơ (cùng CCCD) mà họ tên / ngày sinh khác thì lịch vẫn được đặt và "
                              "mang cờ cần đối chiếu (3.6)."])])
    x += route(so, "9.9", "Cấp lại liên kết phiếu khám", "Người đã mất link phiếu khám đến quầy hoặc gọi điện, xuất trình "
               "CCCD: quản trị viên gửi lại link qua email hoặc mở mã QR trên màn hình. Quy tắc: 3.10.", [
                   tham_so([ID_LICH_HEN]),
                   body(BODY_CAP_LAI),
                   dan("Ví dụ:"), js({"cccd": CCCD_GOI, "cachGui": "MAN_HINH"}),
                   tra("200 OK", f"`duLieu`: {mh('4.12', 'CapLaiLienKetResponse')} của lịch hẹn hiện hành. `MAN_HINH`, "
                                 "lịch chưa khám:", ok(CAP_LAI_MH, "Đã cấp lại liên kết phiếu khám")),
                   dan("`EMAIL` (lịch có email):"), js(ok(CAP_LAI_EMAIL, "Đã cấp lại liên kết phiếu khám")),
                   loi([LOI_CAP_LAI[0], LOI_CAP_LAI[1], LOI_KHONG_CO_QT, LOI_CAP_LAI[2]]),
                   kit.luu_y([LUU_Y_MA,
                              "Lịch đã khám: `MAN_HINH` có thêm `maSoKham`, `lienKetSoKham`; `EMAIL` gửi kèm link sổ khám "
                              "bệnh.",
                              "Mỗi lần cấp lại ghi 1 dòng `CAP_LAI_LIEN_KET` vào nhật ký (9.5)."])])
    return x


# ----- Phụ lục B, C -----

LUONG = (
    ("B.4 Quản trị viên đặt, đổi, hủy lịch thay bệnh nhân", [
        "Người gọi tổng đài: tìm khung giờ như TK B.7 (TK 12.1, TK 12.4) rồi `POST /api/booking/quan-tri/lich-hen` (9.8) "
        "→ 201; đọc mã tra cứu cho người gọi, in phiếu hoặc mở mã QR bằng `phieuKham.maPhieuKham` (TK 14.2). Có email "
        "thì phiếu khám được gửi tới đó.",
        "Tìm lịch: `GET /api/booking/quan-tri/lich-hen?tuNgay=&denNgay=` (9.4), lọc theo bác sĩ, trạng thái, kênh đặt, "
        "hoặc `tuKhoa` là mã tra cứu, CCCD, họ tên, số điện thoại; bấm 1 dòng → `GET .../{id}` (9.5): email, tài khoản "
        "đặt, chuỗi đổi lịch, nhật ký.",
        "`quanTriHuyDoiDuoc` = `true`: đổi lịch `POST .../{id}/doi-lich` (9.7) với khung giờ mới và lý do → 201, chi tiết "
        "lịch mới; hoặc hủy `POST .../{id}/huy` (9.6) với lý do → 200, `QUAN_TRI_HUY`. Bệnh nhân nhận thông báo và email "
        "(TK 3.13).",
    ]),
    ("B.5 Người mất link phiếu khám đến quầy", [
        "Người đó xuất trình CCCD. Quản trị viên tìm bằng `GET /api/booking/quan-tri/lich-hen?tuKhoa=<CCCD>` (9.4); bác sĩ "
        "tìm trong lịch hẹn của mình bằng `GET /api/booking/bac-si/toi/lich-hen/theo-cccd?cccd=` (7.6).",
        "Lịch có email: `POST .../{id}/cap-lai-lien-ket` (9.9 hoặc 7.7) với `cccd` và `cachGui` = `EMAIL` → link được gửi "
        "tới email, response có địa chỉ đã che. Không có email (409 `KHONG_CO_EMAIL_NHAN`) hoặc người đó đang ở quầy: "
        "`cachGui` = `MAN_HINH` → mở mã QR bằng `maPhieuKham` (TK 14.2) cho người đó quét.",
        "CCCD không khớp người khám hay người giám hộ: 403 `CCCD_KHONG_KHOP`, không cấp. Khách tự lấy lại link qua email "
        "mà không cần đến quầy: TK 20.6.",
    ]),
)


def sua(tl):
    # --- Trang đầu, mục 1 ---
    cu = ">1.1 — 19/10/2026</w:t>"
    vi_tri = tl.xml.index(cu)  # ô Phiên bản của trang đầu (đứng trước bảng lịch sử)
    tl.xml = tl.xml[:vi_tri] + f">{PHIEN_BAN}</w:t>" + tl.xml[vi_tri + len(cu):]
    tl.thay_noi_dung(PHAM_VI_CU, PHAM_VI, trong_bang=True)
    assert tl.xml.count("bản 2.4, gọi tắt là") == 2, "Cần đúng 2 chỗ nêu bản của TK"
    tl.xml = tl.xml.replace("bản 2.4, gọi tắt là", "bản 2.5, gọi tắt là")
    tl.them_dong(MUC_1, "LỊCH SỬ PHIÊN BẢN", kit.dong_bang(RONG_PHIEN_BAN, LICH_SU))
    viet_lai(tl, MUC_1, "Bác sĩ:",
             "**Bác sĩ:** xem ca làm việc của mình; lịch hẹn theo ngày và theo khoảng ngày; tra cứu lịch hẹn bằng mã trên "
             "phiếu khám, mã QR hoặc CCCD; hủy lịch kèm lý do; hàng đợi khám (gọi bệnh nhân, đánh dấu vắng mặt); hồ sơ "
             "khám (thông tin bệnh nhân và các lần khám trước); sửa thông tin hành chính của bệnh nhân; đối chiếu giấy tờ; "
             "cấp lại liên kết phiếu khám cho người mất link; ghi, sửa, xem hồ sơ bệnh án và lịch sử sửa; gợi ý thuốc và "
             "mã ICD-10.")
    viet_lai(tl, MUC_1, "Quản trị viên:",
             "**Quản trị viên:** danh sách lịch hẹn toàn viện có bộ lọc; chi tiết lịch hẹn kèm chuỗi đổi lịch và nhật ký "
             "thao tác; hủy, đổi lịch thay bệnh nhân kèm lý do; đặt lịch qua tổng đài / tại quầy; cấp lại liên kết phiếu "
             "khám; tra cứu lịch hẹn bằng mã; đánh dấu đã đối chiếu giấy tờ; xem lịch sử sửa hồ sơ bệnh án.")
    viet_lai(tl, MUC_1, "Báo cáo, thống kê",
             "Báo cáo, thống kê (report-service); quy tắc đặt lịch và ngày bệnh viện nghỉ do quản trị viên sửa (bệnh nhân "
             "đọc ở TK 20.1, TK 20.2). Các route này có ở docs/BAN-GIAO-FRONTEND-05-10.md.")

    # --- Mục 2 ---
    for muc, method, duong, bm, chuc_nang in ROUTE_BS:
        tl.them_dong(bsqt_p2_khung.MUC_2, None,
                     bsqt_p2_khung.dong_route(muc, method, duong, bm, "BAC_SI", chuc_nang, BOOKING),
                     sau_dong_chua="7.6" if muc == "7.7" else "7.5")
    truoc = "9.3"
    for muc, method, duong, bm, chuc_nang in ROUTE_QT:
        tl.them_dong(bsqt_p2_khung.MUC_2, None,
                     bsqt_p2_khung.dong_route(muc, method, duong, bm, "QUAN_TRI_VIEN", chuc_nang, BOOKING),
                     sau_dong_chua=truoc)
        truoc = muc

    # --- Mục 3 ---
    viet_lai(tl, "3.1 Quy ước dùng chung với TK", "Email cho bệnh nhân khi",
             "Email cho bệnh nhân khi bác sĩ hủy lịch, khi quản trị viên hủy / đổi lịch, khi cấp lại liên kết, khi bác sĩ "
             "hẹn tái khám; email phiếu khám khi đặt qua tổng đài")
    viet_lai(tl, "3.1 Quy ước dùng chung với TK", "Không route nào trong tài liệu này phân trang",
             "Chỉ danh sách lịch hẹn toàn viện (9.4) phân trang: `trang`, `kichThuoc`, kết quả là trang (TK 3.3). Các route "
             "khác trả mảng đầy đủ, `[]` khi không có phần tử nào.")
    viet_lai(tl, "3.2 Service cần chạy", "Ca làm việc, lịch hẹn, tra cứu",
             "Ca làm việc, lịch hẹn, tra cứu, hủy lịch, hàng đợi, hồ sơ khám, đối chiếu giấy tờ, cấp lại liên kết, lịch hẹn "
             "toàn viện, đặt lịch qua tổng đài (`/api/booking/**`)")
    viet_lai(tl, "3.2 Service cần chạy", "Hủy lịch còn gửi thông báo",
             "Hủy, đổi lịch, đặt qua tổng đài và cấp lại liên kết qua email còn gửi thông báo / email: dòng cuối.")
    viet_lai(tl, "3.2 Service cần chạy", "Thông báo và email cho bệnh nhân khi bác sĩ hủy lịch",
             "Thông báo và email cho bệnh nhân khi bác sĩ hoặc quản trị viên hủy lịch, khi quản trị viên đổi lịch; email "
             "phiếu khám khi đặt qua tổng đài, email cấp lại liên kết; email hẹn tái khám sau khi ghi / sửa "
             "`ngayTaiKhamDeXuat`")
    viet_lai(tl, "3.4 Lịch hẹn phía bác sĩ", "BI_TU_CHOI, DA_HUY",
             "`BI_TU_CHOI`, `DA_HUY`, `QUAN_TRI_HUY`, `DA_HUY_DO_DOI_LICH`, `KHONG_DEN`")
    viet_lai(tl, "3.4 Lịch hẹn phía bác sĩ", "Chỉ xem.",
             "Chỉ xem. `lyDoHuy` có ở `BI_TU_CHOI` (lý do bác sĩ ghi), ở `QUAN_TRI_HUY` (lý do quản trị viên ghi, 3.10) và "
             "ở `DA_HUY` khi bệnh nhân có ghi lý do.")
    viet_lai(tl, "3.4 Lịch hẹn phía bác sĩ", "Mở hồ sơ khám, sửa thông tin hành chính",
             "Mở hồ sơ khám, sửa thông tin hành chính của bệnh nhân, đánh dấu đã đối chiếu giấy tờ (3.6); cấp lại liên kết "
             "phiếu khám (3.10).")
    for ten, gia_tri in ENUM:
        tl.them_dong("3.9 Enum mới", None, kit.dong_bang(RONG_2, (ten, gia_tri)))
    tl.chen_truoc_doan(MUC_4, muc_3_10())

    # --- Mục 4 ---
    viet_lai(tl, "4.2 LichHenTrongCaResponse", "1 lịch hẹn trong danh sách",
             "1 lịch hẹn trong danh sách lịch hẹn của bác sĩ, trong hàng đợi, trong kết quả tra cứu theo mã hoặc CCCD và "
             "trong danh sách, chi tiết của quản trị viên. Chỉ trả cho bác sĩ của lịch hẹn và quản trị viên: số điện thoại "
             "không che (khác phiếu khám công khai, TK 3.10). Không có mã phiếu khám, không có CCCD.")
    sua_o_cuoi(tl, "4.2 LichHenTrongCaResponse", "lyDoHuy",
               "Lý do bác sĩ hủy (`BI_TU_CHOI`), quản trị viên hủy (`QUAN_TRI_HUY`) hoặc bệnh nhân hủy (`DA_HUY`); `null` "
               "ở các trạng thái khác.")
    for ten, kieu, mo_ta, sau in (
            ("kenhDat", "enum KenhDatLich", "Kênh đặt lịch (3.9). Từ bản 1.2.", "canDoiLich"),
            ("chuyenKhoa", "ChuyenKhoa", "`{ id, ten }`: chuyên khoa của bác sĩ, `ten` theo `Accept-Language`. `null` khi "
                                         "`bacSi` = `null` (route của bác sĩ). Từ bản 1.2.", "kenhDat")):
        tl.them_dong("4.2 LichHenTrongCaResponse", None,
                     kit.dong_bang(RONG_MO_HINH, (f"`{ten}`", kieu, mo_ta)), sau_dong_chua=sau)
    for ten, kieu, mo_ta, sau in (
            ("maSoKham", "string", "Mã sổ khám bệnh của lượt khám, khác mã phiếu khám. Từ bản 1.2: bác sĩ đưa link hoặc mã "
                                   "QR (TK 20.5) cho bệnh nhân.", "hanSua"),
            ("linkSoKham", "string", "Link trang sổ khám bệnh (TK 20.3), cũng là nội dung mã QR; bệnh nhân xem đầy đủ bằng "
                                     "4 số cuối số điện thoại liên hệ (TK 20.4).", "maSoKham")):
        tl.them_dong("4.5 BenhAnResponse", None, kit.dong_bang(RONG_MO_HINH, (f"`{ten}`", kieu, mo_ta)),
                     sau_dong_chua=sau)
    tl.chen_truoc_doan(MUC_5, "".join(mo_hinh(int(SO_MO_HINH_MOI[ten][2:]), ten, mo_ta, dong, con, ghi_chu)
                                      for ten, mo_ta, dong, con, ghi_chu in MO_HINH_MOI))

    # --- JSON mẫu đã có ---
    so_khoi = sua_moi_mau(tl, MUC_5, bsqt_p5_phu_luc.PHU_LUC_A)
    assert so_khoi >= 10, f"Chỉ viết lại {so_khoi} khối JSON mẫu"

    # --- Mục 7, 9 ---
    so = iter(range(tl.id_bookmark_moi(), 10 ** 6))
    chen_doan_sau(tl, "7. Tra cứu lịch hẹn và hồ sơ khám", "Các route của booking-service để bác sĩ",
                  "Người mất link phiếu khám đến quầy: bác sĩ tìm lịch hẹn của mình theo CCCD (7.6) rồi cấp lại liên kết "
                  "(7.7). Quy tắc: 3.10.")
    tl.chen_truoc_doan(MUC_8, muc_7_6_7_7(so))
    tl.thay_chu(MUC_9_CU, MUC_9)
    viet_lai(tl, MUC_9, "Các route của quản trị viên (3.3)",
             f"Các route của quản trị viên (3.3): thấy mọi lịch hẹn của mọi bác sĩ. Service cần chạy: {BOOKING} cho 9.1, "
             f"9.2, 9.4-9.9; medical-service cho 9.3 (3.2). Màn hình lịch hẹn toàn viện: danh sách có lọc (9.4), chi tiết "
             "và nhật ký (9.5), hủy hoặc đổi lịch thay bệnh nhân (9.6, 9.7); người gọi tổng đài hoặc đến quầy: đặt lịch "
             "thay (9.8); người mất link phiếu khám: cấp lại liên kết (9.9). Quy tắc: 3.10. Bệnh nhân mất phiếu hoặc khiếu "
             "nại: tra lịch hẹn bằng mã (9.1); lịch hẹn cần đối chiếu giấy tờ thì đánh dấu đã đối chiếu sau khi xem giấy "
             "tờ (9.2); lượt khám đã có hồ sơ bệnh án thì xem các lần bác sĩ sửa hồ sơ (9.3). Quản trị viên không sửa "
             "thông tin bệnh nhân và không ghi, không sửa hồ sơ bệnh án.")
    tl.chen_truoc_doan(bsqt_p5_phu_luc.PHU_LUC_A, muc_9_moi(so))

    # --- Phụ lục B, C ---
    viet_lai(tl, bsqt_p5_phu_luc.PHU_LUC_B, "Thứ tự gọi API cho các màn hình chính",
             "Thứ tự gọi API cho các màn hình chính. `...` thay cho tiền tố của đường dẫn đầy đủ gần nhất phía trước "
             "(`/api/booking/bac-si/toi`, `/api/medical/bac-si/toi` hoặc `/api/booking/quan-tri/lich-hen`); route đầy đủ "
             "ở mục 2.")
    x = ""
    for tieu_de, cac_buoc in LUONG:
        so_ds = tl.danh_sach_so_moi()
        x += kit.tieu_de(2, tieu_de) + "".join(kit.cham(b, so_ds) for b in cac_buoc) + kit.khoang_trong(60)
    tl.chen_truoc_doan(bsqt_p5_phu_luc.PHU_LUC_C, x)
    chen_doan_sau(tl, bsqt_p5_phu_luc.PHU_LUC_C, "Chưa có lịch hẹn, hồ sơ bệnh án:",
                  "**Tổng đài và cấp lại liên kết:** đặt qua tổng đài (9.8) 1 lịch không nhập email và 1 lịch có email, "
                  "rồi cấp lại bằng CCCD vừa nhập (9.9, hoặc 7.7 bằng tài khoản bác sĩ của lịch): lịch không email với "
                  "`EMAIL` trả 409 `KHONG_CO_EMAIL_NHAN`. Chạy với `MAIL_MODE=console` thì email được ghi ra log thay vì "
                  "gửi thật (README.md).")

    # --- Phụ lục A: dựng lại vì thân bài nhắc thêm mã lỗi ---
    dau = tl.vi_tri_tieu_de(bsqt_p5_phu_luc.PHU_LUC_A)
    cuoi = tl.vi_tri_tieu_de(bsqt_p5_phu_luc.PHU_LUC_B)
    tl.xml = tl.xml[:dau] + tl.xml[cuoi:]
    tl.xml = tl.xml[:dau] + bsqt_p5_phu_luc.phu_luc_a(tl, "") + tl.xml[dau:]

