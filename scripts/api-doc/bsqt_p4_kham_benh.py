"""Tài liệu 2, giai đoạn 4: mục 7 (tra cứu lịch hẹn, hồ sơ khám, sửa thông tin bệnh nhân, đã đối chiếu; booking-service)
và mục 8 (hồ sơ bệnh án, lịch sử sửa, gợi ý thuốc và ICD-10; medical-service), 11 route phía bác sĩ, cùng 11 dòng của
bảng route ở mục 2. Chạy qua bsqt_build.py.

JSON mẫu lấy từ response thật do bsqt_lay_mau.py ghi lại (temp/api-doc/mau-bsqt), danh sách cắt còn 1-2 phần tử; id, tên
giữ như response thật, thời điểm đặt lịch và ghi hồ sơ chuẩn hoá về quanh giờ khám. Quy tắc nghiệp vụ đã có ở mục 3 thì
chỉ dẫn mục.
"""

import bsqt_p2_khung
import kit
from bsqt_p3_lich_hang_doi import BAC_SI, ID_LICH_HEN, LOI_KHONG_CO, NAM, NGAY, lich_hen, mh
from p3_dat_lich import dan, js, loi, ok, tham_so, tra

BS = "/api/booking/bac-si/toi"
MD = "/api/medical/bac-si/toi"
QUYEN = "Chỉ BAC_SI (cần access token)"
BOOKING, MEDICAL = "booking-service", "medical-service"
RONG_BODY = (2900, 900, 1000, 4838)  # tên trường lồng dài nhất: sinhHieu.huyetApTamTruong, donThuoc[].ghiChuSuDung

# ----- Dữ liệu mẫu (từ response thật) -----

GIO_KHUNG = {"gioBatDauKhung": f"{NGAY}T13:30:00", "gioKetThucKhung": f"{NGAY}T14:30:00"}


def lh(id_lh, ma, so_thu_tu, gio, benh_nhan, id_ho_so, ly_do, ngay_dat, **khac):
    """Lịch hẹn trong ca chiều 13:30-17:00 (ca 33) của bác sĩ mẫu."""
    return {**lich_hen(id_lh, ma, so_thu_tu, gio, benh_nhan, id_ho_so, ly_do, id_ca=33, ngay_dat=ngay_dat),
            **GIO_KHUNG, **khac}


BN_PHONG = {"id": 4, "hoTen": "Phạm Quốc Phong", "ngaySinh": "1976-01-30", "tuoi": 50, "gioiTinh": "NAM"}
ANH = {"hoTen": "Lê Minh Anh", "ngaySinh": "2023-05-20", "tuoi": 3, "gioiTinh": "NU"}
LH_A = lh(6, "1488", 22, "13:30:00", NAM, 2, "Ho, đau họng, sốt 3 ngày nay", "2026-10-15T07:48:12.530214")
LH_B = lh(7, "7136", 23, "13:40:00", {**{k: v for k, v in BN_PHONG.items() if k != "id"}, "soDienThoai": "0933100001"}, 4,
          "Tái khám huyết áp", "2026-10-15T09:21:40.118734")
LH_B["doiChieu"] = {**LH_B["doiChieu"], "canDoiChieu": True, "hoTenDaNhap": "Phạm Quốc Phonm"}
LH_C = lh(8, "8072", 24, "13:50:00", {**ANH, "soDienThoai": None}, 5, "Sốt, sổ mũi 2 ngày", "2026-10-15T10:05:27.64391",
          nguoiGiamHo={"hoTen": "Lê Thị Hoa", "quanHe": "ME", "soDienThoai": "0933100003"})
LH_C["doiChieu"] = {**LH_C["doiChieu"], "hoTenGiamHoDaNhap": "Lê Thị Hoa"}


def ho_so(id_hs, cccd, nguoi, so_dien_thoai, dia_chi, lien_ket):
    return {"id": id_hs, "cccd": cccd, **{k: nguoi[k] for k in ("hoTen", "ngaySinh", "tuoi", "gioiTinh")},
            "soDienThoai": so_dien_thoai, "diaChi": dia_chi, "soBaoHiemYTe": None, "tienSuBenhLy": None,
            "daLienKetTaiKhoan": lien_ket}


THUOC = {1: ("Paracetamol", "Paracetamol", "500mg", "viên"), 2: ("Hapacol 250", "Paracetamol", "250mg", "gói"),
         3: ("Amoxicillin", "Amoxicillin", "500mg", "viên"), 7: ("Cetirizin", "Cetirizin", "10mg", "viên")}
DONG_PARA = {"idThuoc": 1, "lieuDung": "500mg/lần", "soLanMoiNgay": 3, "soNgayDung": 5, "soLuong": 15,
             "ghiChuSuDung": "Uống sau ăn, khi sốt trên 38.5 độ"}
DONG_AMOX = {"idThuoc": 3, "lieuDung": "500mg/lần", "soLanMoiNgay": 2, "soNgayDung": 7, "soLuong": 14,
             "ghiChuSuDung": "Uống sau ăn"}
DONG_CETI = {"idThuoc": 7, "lieuDung": "10mg/lần", "soLanMoiNgay": 1, "soNgayDung": 5, "soLuong": 5,
             "ghiChuSuDung": "Uống buổi tối"}


def don(dong):
    """1 dòng đơn thuốc như response trả (DonThuoc của TK 4.27)."""
    ten, hoat_chat, ham_luong, don_vi = THUOC[dong["idThuoc"]]
    return {"idThuoc": dong["idThuoc"], "tenThuoc": ten, "hoatChat": hoat_chat, "hamLuong": ham_luong,
            "duongDung": "UONG", "donVi": don_vi, **{k: v for k, v in dong.items() if k != "idThuoc"}}


def dong_luu(dong):
    """1 dòng đơn thuốc trong NoiDungBenhAn (4.7)."""
    ten, _, ham_luong, _ = THUOC[dong["idThuoc"]]
    return {"idThuoc": dong["idThuoc"], "tenThuoc": ten, "hamLuong": ham_luong,
            **{k: v for k, v in dong.items() if k != "idThuoc"}}


SINH_HIEU = {"mach": 92, "nhietDo": 38.2, "huyetApTamThu": 120, "huyetApTamTruong": 80, "nhipTho": 18, "canNang": 65.5,
             "chieuCao": 170}
J06 = {"ma": "J06.9", "ten": "Nhiễm trùng đường hô hấp trên cấp, không đặc hiệu", "manTinh": False}
J00 = {"ma": "J00", "ten": "Viêm mũi họng cấp [cảm thường]", "manTinh": False}
I10 = {"ma": "I10", "ten": "Tăng huyết áp vô căn (nguyên phát)", "manTinh": True}
GHI_A = {"lyDoKham": "Ho, đau họng, sốt 3 ngày nay", "sinhHieu": SINH_HIEU,
         "khamLamSang": "Họng đỏ, amidan sưng nhẹ, phổi không ran", "chanDoan": "Viêm họng cấp", "maIcdChinh": "J06.9",
         "chanDoanKemTheo": "Viêm mũi dị ứng",
         "ghiChu": "Uống nhiều nước, nghỉ ngơi; tái khám ngay nếu sốt cao trên 3 ngày", "ngayTaiKhamDeXuat": "2026-10-23",
         "donThuoc": [DONG_PARA, DONG_AMOX]}
SUA_A = {**GHI_A, "chanDoan": "Viêm mũi họng cấp", "maIcdChinh": "J00", "donThuoc": [DONG_PARA, DONG_CETI],
         "lyDoSua": "Test nhanh liên cầu âm tính: bỏ kháng sinh, thêm thuốc chống dị ứng", "phienBan": 1}
NGAY_TAO, NGAY_SUA, HAN_SUA = f"{NGAY}T13:52:08.311402", f"{NGAY}T14:05:33.27815", "2026-10-23T13:52:08.311402"


def benh_an(noi_dung, icd, ngay_tao, phien_ban, ngay_sua, han_sua):
    """BenhAnResponse (4.5) của lịch hẹn A."""
    return {"id": 2, "idLichHen": 6, "maTraCuu": LH_A["maTraCuu"], "trangThaiLichHen": "DA_HOAN_THANH",
            "lyDoKham": noi_dung["lyDoKham"], "sinhHieu": {**SINH_HIEU, "chieuCao": 170.0},
            "khamLamSang": noi_dung["khamLamSang"], "chanDoan": noi_dung["chanDoan"], "icdChinh": icd,
            "chanDoanKemTheo": noi_dung["chanDoanKemTheo"], "ghiChu": noi_dung["ghiChu"],
            "ngayTaiKhamDeXuat": noi_dung["ngayTaiKhamDeXuat"], "donThuoc": [don(d) for d in noi_dung["donThuoc"]],
            "ngayTao": ngay_tao, "phienBan": phien_ban, "ngaySuaCuoi": ngay_sua, "hanSua": han_sua}


def noi_dung(nd):
    """NoiDungBenhAn (4.7) của 1 body ghi / sửa."""
    return {"lyDoKham": nd["lyDoKham"], "sinhHieu": {**SINH_HIEU, "chieuCao": 170.0}, "khamLamSang": nd["khamLamSang"],
            "chanDoan": nd["chanDoan"], "maIcdChinh": nd["maIcdChinh"], "chanDoanKemTheo": nd["chanDoanKemTheo"],
            "ghiChu": nd["ghiChu"], "ngayTaiKhamDeXuat": nd["ngayTaiKhamDeXuat"],
            "donThuoc": [dong_luu(d) for d in nd["donThuoc"]]}


BA_GHI = benh_an(GHI_A, J06, f"{NGAY}T13:52:08.311402365", 1, None, "2026-10-23T13:52:08.311402365")
BA_SUA = benh_an(SUA_A, J00, NGAY_TAO, 2, NGAY_SUA, HAN_SUA)
KQ_A = {k: BA_SUA[k] for k in ("lyDoKham", "sinhHieu", "khamLamSang", "chanDoan", "icdChinh", "chanDoanKemTheo",
                               "ghiChu", "ngayTaiKhamDeXuat", "donThuoc", "ngaySuaCuoi", "phienBan", "hanSua")}
KQ_F2 = {"lyDoKham": "Đau đầu, chóng mặt", "sinhHieu": None, "khamLamSang": None, "chanDoan": "Đau đầu do căng thẳng",
         "icdChinh": {"ma": "G44.2", "ten": "Đau đầu dạng căng thẳng", "manTinh": False}, "chanDoanKemTheo": None,
         "ghiChu": None, "ngayTaiKhamDeXuat": None, "donThuoc": [], "ngaySuaCuoi": None, "phienBan": 1,
         "hanSua": "2026-10-23T08:24:31.902114"}


def lan_kham(id_lh, ma, gio, trang_thai, ly_do, ket_qua):
    return {"idLichHen": id_lh, "maTraCuu": f"ECL-{NGAY.replace('-', '')}-{ma}", "ngay": NGAY,
            "gioKhamDuKien": f"{NGAY}T{gio}", "bacSi": BAC_SI, "tenChuyenKhoa": "Nội tổng quát",
            "trangThai": trang_thai, "lyDoKham": ly_do, "ketQua": ket_qua}


HS_A = {"lichHen": {**LH_A, "trangThai": "DA_HOAN_THANH"},
        "hoSoBenhNhan": ho_so(2, "079000000002", NAM, "0902000002", "45 Lê Lợi, Bến Nghé, Quận 1, TP. Hồ Chí Minh",
                              True),
        "ketQua": KQ_A,
        "lanKhamTruoc": [lan_kham(2, "1847", "08:10:00", "DA_HOAN_THANH", "Đau đầu, chóng mặt", KQ_F2)]}
HS_B = {"lichHen": LH_B,
        "hoSoBenhNhan": ho_so(4, "079000000004", BN_PHONG, "0902000004",
                              "5 Phan Văn Trị, Phường 7, Bình Thạnh, TP. Hồ Chí Minh", True),
        "ketQua": None,
        "lanKhamTruoc": [lan_kham(4, "2839", "09:10:00", "DA_XAC_NHAN", "Đau dạ dày", None)]}
SUA_C = {"hoTen": "Lê Minh Anh", "ngaySinh": "2023-05-20", "gioiTinh": "NU", "soDienThoai": "0933100003",
         "cccd": "079323000777", "diaChi": "", "soBaoHiemYTe": ""}
HS_C = {"lichHen": LH_C, "hoSoBenhNhan": ho_so(5, "079323000777", ANH, "0933100003", None, False), "ketQua": None,
        "lanKhamTruoc": []}
LICH_SU = {"idLichHen": 6, "maTraCuu": LH_A["maTraCuu"], "phienBanHienTai": 2, "ngayGhiNhan": NGAY_TAO,
           "cacLanSua": [{"phienBan": 2, "ngaySua": f"{NGAY}T14:05:33.301266", "bacSi": {"id": 1, "hoTen": "Nguyễn Văn An"},
                          "lyDo": SUA_A["lyDoSua"], "cacTruongDoi": ["CHAN_DOAN", "DON_THUOC"],
                          "truoc": noi_dung(GHI_A), "sau": noi_dung(SUA_A)}]}
GOI_Y_THUOC = [{"id": 2, "tenThuoc": "Hapacol 250", "hoatChat": "Paracetamol", "hamLuong": "250mg",
                "dangBaoChe": "Thuốc bột sủi bọt", "duongDung": "UONG", "donVi": "gói"},
               {"id": 1, "tenThuoc": "Paracetamol", "hoatChat": "Paracetamol", "hamLuong": "500mg",
                "dangBaoChe": "Viên nén", "duongDung": "UONG", "donVi": "viên"}]
GOI_Y_ICD = [I10, {"ma": "I11.9", "ten": "Bệnh tim do tăng huyết áp, không có suy tim (sung huyết)", "manTinh": False}]

# ----- Bảng route ở mục 2 -----

ROUTE = [
    ("7.1", "GET", f"{BS}/lich-hen/tra-cuu", "ep_bs_tra_cuu", "Tra lịch hẹn của tôi theo mã / QR", BOOKING),
    ("7.2", "GET", f"{BS}/lich-hen/tra-cuu/ho-so-kham", "ep_bs_ho_so_kham_theo_ma", "Hồ sơ khám theo mã / QR",
     BOOKING),
    ("7.3", "GET", f"{BS}/lich-hen/{{id}}/ho-so-kham", "ep_bs_ho_so_kham", "Hồ sơ khám của 1 lịch hẹn", BOOKING),
    ("7.4", "PUT", f"{BS}/lich-hen/{{id}}/benh-nhan", "ep_bs_sua_benh_nhan", "Sửa thông tin bệnh nhân", BOOKING),
    ("7.5", "POST", f"{BS}/lich-hen/{{id}}/da-doi-chieu", "ep_bs_da_doi_chieu", "Đánh dấu đã đối chiếu giấy tờ",
     BOOKING),
    ("8.1", "POST", f"{MD}/lich-hen/{{idLichHen}}/benh-an", "ep_bs_ghi_benh_an", "Ghi hồ sơ bệnh án, đơn thuốc",
     MEDICAL),
    ("8.2", "PUT", f"{MD}/lich-hen/{{idLichHen}}/benh-an", "ep_bs_sua_benh_an", "Sửa hồ sơ bệnh án, kèm lý do",
     MEDICAL),
    ("8.3", "GET", f"{MD}/lich-hen/{{idLichHen}}/benh-an", "ep_bs_xem_benh_an", "Xem hồ sơ bệnh án", MEDICAL),
    ("8.4", "GET", f"{MD}/lich-hen/{{idLichHen}}/benh-an/lich-su", "ep_bs_lich_su_sua", "Lịch sử sửa hồ sơ bệnh án",
     MEDICAL),
    ("8.5", "GET", f"{MD}/thuoc", "ep_bs_goi_y_thuoc", "Gợi ý thuốc khi kê đơn", MEDICAL),
    ("8.6", "GET", f"{MD}/icd10", "ep_bs_goi_y_icd10", "Gợi ý mã ICD-10", MEDICAL),
]
R = {muc: (method, duong, bm) for muc, method, duong, bm, _, _ in ROUTE}


def route(so, muc, ten, mo_ta, than):
    method, duong, bm = R[muc]
    return kit.route(muc, ten, method, duong, bm, next(so), QUYEN, mo_ta, than)


def body(dong, rong=RONG_BODY):
    return (kit.nhan("REQUEST BODY (JSON)")
            + kit.bang(rong, ("Trường", "Kiểu", "Bắt buộc", "Ràng buộc / mô tả"),
                       [(f"`{t}`", k, b, m) for t, k, b, m in dong]) + kit.khoang_trong(60))


MA = ("ma", "query", "string", "Bắt buộc. Mã tra cứu `ECL-...`, mã phiếu khám hoặc nguyên link phiếu khám (3.3). Gửi "
                               "link thì mã hoá URL (`encodeURIComponent`).")
ID_LICH_HEN_MD = ("idLichHen", "path", "number", "Id lịch hẹn (`id` của 4.2; cùng id với `{id}` ở mục 7, 3.3).")
LOI_THIEU_MA = (400, "DU_LIEU_KHONG_HOP_LE", "Thiếu `ma`.")
LOI_MA = (404, "KHONG_TIM_THAY", "`ma` sai dạng, không có lịch hẹn nào, hoặc là lịch hẹn của bác sĩ khác (3.3).")
TU_KHOA = ("tuKhoa", "query", "string", "Không bắt buộc. Rỗng hoặc không gửi: `[]` (3.8).")
TRA_HO_SO = f"`duLieu`: {mh('HoSoKhamResponse')}."


def muc_7(so):
    x = kit.tieu_de(1, "7. Tra cứu lịch hẹn và hồ sơ khám")
    x += kit.doan(f"Các route của `{BOOKING}` để bác sĩ mở hồ sơ khám của bệnh nhân (3.3); service cần chạy: {BOOKING} "
                  "(3.2). Bệnh nhân đến khám đưa phiếu khám: bác sĩ quét QR hoặc gõ mã, gọi 7.1 (chỉ lịch hẹn) hoặc 7.2 "
                  "(cả hồ sơ khám); từ danh sách lịch hẹn (mục 5) hoặc hàng đợi (mục 6) thì mở hồ sơ khám bằng 7.3. Lịch "
                  "hẹn cần đối chiếu giấy tờ: bác sĩ xem giấy tờ, sửa thông tin bệnh nhân nếu cần (7.4), rồi đánh dấu đã "
                  "đối chiếu (7.5). Quy tắc: 3.6.")
    x += route(so, "7.1", "Tra lịch hẹn theo mã", "Tìm 1 lịch hẹn của bác sĩ theo mã in trên phiếu khám, mã phiếu khám "
               "hoặc link phiếu khám đọc từ QR.", [
                   tham_so([MA]),
                   tra("200 OK", f"`duLieu`: {mh('LichHenTrongCaResponse')}, `bacSi` = `null`.", ok(LH_A)),
                   loi([LOI_THIEU_MA, LOI_MA]),
                   kit.luu_y(["`id` trong kết quả dùng cho các route khác của mục 5-8."])])
    x += route(so, "7.2", "Hồ sơ khám theo mã", "Như 7.3 nhưng tìm lịch hẹn theo `ma` như 7.1: quét QR xong mở ngay hồ "
               "sơ khám.", [
                   tham_so([MA]),
                   tra("200 OK", TRA_HO_SO + " Ví dụ: lượt khám đã ghi và đã sửa hồ sơ bệnh án (`ketQua.phienBan` = 2), "
                                             "bệnh nhân có 1 lần khám trước trong ngày.", ok(HS_A)),
                   loi([LOI_THIEU_MA, LOI_MA])])
    x += route(so, "7.3", "Hồ sơ khám của 1 lịch hẹn", "Lịch hẹn, hồ sơ bệnh nhân đầy đủ (CCCD, số điện thoại không "
               "che), kết quả khám của lịch hẹn này nếu đã ghi, và các lần khám trước của bệnh nhân với mọi bác sĩ.", [
                   tham_so([ID_LICH_HEN]),
                   tra("200 OK", TRA_HO_SO + " Ví dụ: lịch hẹn cần đối chiếu giấy tờ: họ tên đã nhập "
                                             "(`doiChieu.hoTenDaNhap`) khác hồ sơ.", ok(HS_B)),
                   loi([LOI_KHONG_CO]),
                   kit.luu_y(["`lichHen.doiChieu.canDoiChieu` = `true`: hiện cảnh báo, đặt `hoSoBenhNhan` cạnh "
                              "`doiChieu` (3.6).",
                              "`ketQua.phienBan`, `ketQua.hanSua` của lượt khám này dùng cho 8.2; với lần khám trước "
                              "chỉ để hiển thị (4.4). Lịch sử sửa của 1 lần khám trước: 8.4 với "
                              "`lanKhamTruoc[].idLichHen`."])])
    x += route(so, "7.4", "Sửa thông tin bệnh nhân", "Sửa hồ sơ bệnh nhân của 1 lịch hẹn của bác sĩ sau khi xem giấy "
               "tờ. Body gửi đủ mọi trường như đang hiển thị (3.6).", [
                   tham_so([ID_LICH_HEN]),
                   body([
                       ("hoTen", "string", "Có", "Không rỗng, tối đa 150 ký tự."),
                       ("ngaySinh", "date", "Có", "Không ở tương lai."),
                       ("gioiTinh", "enum", "Không", "`NAM`, `NU`, `KHAC`; `null` là xoá."),
                       ("soDienThoai", "string", "Có", "10 chữ số, bắt đầu bằng 0."),
                       ("cccd", "string", "Không", "Đúng 12 chữ số, hoặc trống. Chỉ điền được khi hồ sơ chưa có CCCD "
                                                   "(3.6)."),
                       ("diaChi", "string", "Không", "Tối đa 500 ký tự; trống là xoá."),
                       ("soBaoHiemYTe", "string", "Không", "Tối đa 50 ký tự; trống là xoá."),
                   ], kit.RONG_TRUONG),
                   dan("Ví dụ: điền CCCD cho trẻ đặt lịch khi chưa có, xoá trắng địa chỉ và số bảo hiểm:"), js(SUA_C),
                   tra("200 OK", TRA_HO_SO.rstrip(".") + " của lịch hẹn, sau khi sửa.",
                       ok(HS_C, "Đã cập nhật hồ sơ bệnh nhân")),
                   loi([(400, "DU_LIEU_KHONG_HOP_LE", "Body sai giới hạn (`chiTiet` nêu trường), hoặc gửi số CCCD khác "
                                                      "số hồ sơ đang có (3.6)."),
                        LOI_KHONG_CO,
                        (409, "XUNG_DOT_DU_LIEU", "Số CCCD đã thuộc hồ sơ bệnh nhân khác; hoặc trẻ chưa có CCCD được "
                                                  "sửa họ tên / ngày sinh trùng 1 hồ sơ trẻ khác có cùng CCCD người giám "
                                                  "hộ.")]),
                   kit.luu_y(["Sửa xong cờ cần đối chiếu không tự tắt: gọi 7.5.",
                              "Hồ sơ dùng chung cho mọi lịch hẹn của bệnh nhân: các lịch hẹn khác cũng thấy thông tin "
                              "mới."])])
    x += route(so, "7.5", "Đánh dấu đã đối chiếu", "Bác sĩ đã xem giấy tờ: `doiChieu.canDoiChieu` của lịch hẹn thành "
               "`false`.", [
                   tham_so([ID_LICH_HEN]),
                   kit.nhan("RESPONSE THÀNH CÔNG — 200 OK"),
                   dan("Không có `duLieu`:"), js(ok(None, "Đã ghi nhận đối chiếu thông tin bệnh nhân")),
                   loi([LOI_KHONG_CO]),
                   kit.luu_y(["Gọi lại khi đã `false` vẫn 200. Đọc lại cờ bằng 7.3."])])
    return x


def muc_8(so):
    x = kit.tieu_de(1, "8. Hồ sơ bệnh án")
    x += kit.doan(f"Các route của `{MEDICAL}` để bác sĩ ghi, sửa, xem hồ sơ bệnh án của lịch hẹn của mình (3.3); "
                  f"service cần chạy: {MEDICAL} (3.2). Màn hình khám gọi 8.5 và 8.6 khi bác sĩ gõ tên thuốc và chẩn "
                  "đoán, lưu bằng 8.1; mở lại bằng 8.3, sửa bằng 8.2 trong hạn sửa, xem các lần sửa bằng 8.4. Quy tắc "
                  "ghi, sửa, xem: 3.7; giới hạn của body: 3.8.")
    x += route(so, "8.1", "Ghi hồ sơ bệnh án", "Lưu kết quả khám (lý do khám, sinh hiệu, khám lâm sàng, chẩn đoán, lời "
               "dặn, ngày tái khám đề xuất) kèm đơn thuốc cho 1 lịch hẹn của bác sĩ. Lưu xong lịch hẹn thành "
               "`DA_HOAN_THANH`.", [
                   tham_so([ID_LICH_HEN_MD]),
                   body([
                       ("lyDoKham", "string", "Không", "Lý do khám, triệu chứng (3.7)."),
                       ("sinhHieu", "object", "Không", "Không gửi = không đo. Khoảng giá trị của từng chỉ số: 3.8."),
                       ("sinhHieu.mach", "number", "Không", "Lần/phút."),
                       ("sinhHieu.nhietDo", "number", "Không", "°C."),
                       ("sinhHieu.huyetApTamThu", "number", "Không", "mmHg. Gửi cùng `huyetApTamTruong`."),
                       ("sinhHieu.huyetApTamTruong", "number", "Không", "mmHg, nhỏ hơn tâm thu."),
                       ("sinhHieu.nhipTho", "number", "Không", "Lần/phút."),
                       ("sinhHieu.canNang", "number", "Trẻ dưới 72 tháng", "Kg. Bắt buộc khi kê thuốc cho trẻ dưới "
                                                                           "72 tháng tuổi."),
                       ("sinhHieu.chieuCao", "number", "Không", "Cm."),
                       ("khamLamSang", "string", "Không", "Kết quả khám lâm sàng."),
                       ("chanDoan", "string", "Có", "Chẩn đoán chính, chữ tự do."),
                       ("maIcdChinh", "string", "Không", "Mã ICD-10 của chẩn đoán chính (`ma` của 8.6)."),
                       ("chanDoanKemTheo", "string", "Không", "Bệnh kèm theo, chữ tự do."),
                       ("ghiChu", "string", "Không", "Lời dặn của bác sĩ."),
                       ("ngayTaiKhamDeXuat", "date", "Không", "Sau hôm nay; không tự tạo lịch hẹn (3.7)."),
                       ("donThuoc", "array", "Không", "Đơn thuốc; không gửi hoặc `[]` = không kê đơn."),
                       ("donThuoc[].idThuoc", "number", "Có", "`id` của 8.5."),
                       ("donThuoc[].lieuDung", "string", "Không", "Vd `500mg/lần`."),
                       ("donThuoc[].soLanMoiNgay", "number", "Không", "Số lần dùng mỗi ngày."),
                       ("donThuoc[].soNgayDung", "number", "Có", "Hạn mức 30 / 90 ngày theo `maIcdChinh`: 3.8."),
                       ("donThuoc[].soLuong", "number", "Có", "Tổng số lượng cấp, theo `donVi` của thuốc."),
                       ("donThuoc[].ghiChuSuDung", "string", "Không", "Vd `Uống sau ăn`."),
                   ]),
                   dan("Ví dụ:"), js(GHI_A),
                   tra("201 Created", f"`duLieu`: {mh('BenhAnResponse')} vừa ghi, `phienBan` = 1.",
                       ok(BA_GHI, "Đã ghi nhận kết quả khám")),
                   loi([(400, "DU_LIEU_KHONG_HOP_LE",
                         "Sai giới hạn của 3.8. Có `chiTiet` khi sai 1 trường (vd `chanDoan` trống, `sinhHieu.mach` "
                         "ngoài khoảng). Không có `chiTiet`, `thongDiep` nói rõ chỗ sai, khi: mã ICD-10 không có trong "
                         "danh mục, huyết áp thiếu 1 số hoặc tâm trương không nhỏ hơn tâm thu, trẻ dưới 72 tháng có kê "
                         "thuốc mà thiếu cân nặng, 1 thuốc có 2 dòng, số ngày dùng quá hạn mức (nêu tên thuốc), "
                         "`ngayTaiKhamDeXuat` không sau hôm nay."),
                        (404, "KHONG_TIM_THAY", "Lịch hẹn không có hoặc của bác sĩ khác (3.3); hoặc 1 `idThuoc` không "
                                                "có trong danh mục."),
                        (409, "LICH_HEN_KHONG_KHAM_DUOC", "Lịch hẹn không còn `DA_XAC_NHAN`: đã có hồ sơ bệnh án (bấm "
                                                          "lưu lần 2), đã hủy, không đến (3.7)."),
                        (409, "CHUA_DEN_NGAY_KHAM", "Ngày khám ở tương lai (3.7)."),
                        (409, "THUOC_NGUNG_DUNG", "1 thuốc trong đơn đã ngừng dùng; `thongDiep` nêu tên thuốc."),
                        (409, "THUOC_CHUA_XAC_MINH", "1 thuốc trong đơn chưa được quản trị viên xác minh; `thongDiep` "
                                                     "nêu tên thuốc.")]),
                   kit.luu_y(["Không cần gọi vào khám (6.2, 6.3) trước. Lưu xong, hàng đợi hiện lượt này là `DA_KHAM` "
                              "(3.5).",
                              "`ngayTao`, `hanSua` ở response của 8.1 (và `ngaySuaCuoi` ở 8.2) có thể tới 9 chữ số lẻ "
                              "của giây; 8.3 đọc lại trả tối đa 6 (TK 3.5). So như thời điểm, không so chuỗi.",
                              "Có `ngayTaiKhamDeXuat` thì bệnh nhân được nhắc tái khám qua email (TK 3.13)."])])
    x += route(so, "8.2", "Sửa hồ sơ bệnh án", "Thay toàn bộ nội dung hồ sơ bệnh án của 1 lịch hẹn, trong hạn sửa, kèm "
               "lý do và phiên bản đang xem. Mỗi lần sửa thêm 1 lần sửa vào lịch sử (8.4). Quy tắc: 3.7.", [
                   tham_so([ID_LICH_HEN_MD]),
                   kit.nhan("REQUEST BODY (JSON)"),
                   kit.doan("Mọi trường của 8.1 (trường bỏ trống là xoá trắng; `donThuoc` thay toàn bộ đơn cũ), thêm:"),
                   kit.bang(RONG_BODY, ("Trường", "Kiểu", "Bắt buộc", "Ràng buộc / mô tả"), [
                       ("`lyDoSua`", "string", "Có", "Lý do sửa, tối đa 500 ký tự; hiện trong lịch sử sửa."),
                       ("`phienBan`", "number", "Có", "`phienBan` của hồ sơ đang xem (8.3, hoặc `ketQua.phienBan` ở "
                                                       "7.2, 7.3)."),
                   ]) + kit.khoang_trong(60),
                   dan("Ví dụ: đổi chẩn đoán và 1 thuốc trong đơn của hồ sơ ở 8.1:"), js(SUA_A),
                   kit.nhan("RESPONSE THÀNH CÔNG — 200 OK"),
                   kit.doan(f"`duLieu`: {mh('BenhAnResponse')} sau khi sửa: `phienBan` tăng 1, `ngaySuaCuoi` là lúc "
                            "sửa; mẫu ở 8.3. `thongDiep`: \"Đã cập nhật hồ sơ bệnh án\"."),
                   loi([(400, "DU_LIEU_KHONG_HOP_LE", "Như 8.1; thêm: thiếu `lyDoSua` hoặc `phienBan`, `lyDoSua` dài "
                                                      "hơn 500 ký tự (có `chiTiet`); nội dung giống hệt bản đang lưu "
                                                      "(không có `chiTiet`)."),
                        (404, "KHONG_TIM_THAY", "Như 8.1; thêm: lịch hẹn chưa có hồ sơ bệnh án."),
                        (409, "HET_HAN_SUA_BENH_AN", "Đã qua `hanSua` (3.7)."),
                        (409, "DU_LIEU_DA_THAY_DOI", "`phienBan` khác phiên bản đang lưu: đã có lần sửa khác. Tải lại "
                                                     "bằng 8.3 rồi sửa lại."),
                        (409, "THUOC_NGUNG_DUNG", "Chỉ với thuốc thêm mới vào đơn; thuốc đã có trong đơn giữ lại được "
                                                  "(3.8)."),
                        (409, "THUOC_CHUA_XAC_MINH", "Như `THUOC_NGUNG_DUNG` ở trên.")]),
                   kit.luu_y(["Thứ tự kiểm tra: body (400), lịch hẹn và hồ sơ bệnh án (404), hạn sửa, phiên bản (409), "
                              "rồi nội dung như 8.1."])])
    x += route(so, "8.3", "Xem hồ sơ bệnh án", "Hồ sơ bệnh án của 1 lịch hẹn của bác sĩ, kèm `phienBan` và `hanSua` để "
               "sửa.", [
                   tham_so([ID_LICH_HEN_MD]),
                   tra("200 OK", f"`duLieu`: {mh('BenhAnResponse')}. Ví dụ: hồ sơ ở 8.1 sau lần sửa ở 8.2.",
                       ok(BA_SUA)),
                   loi([(404, "KHONG_TIM_THAY", "Lịch hẹn không có hoặc của bác sĩ khác (3.3), hoặc chưa có hồ sơ bệnh "
                                                "án; `thongDiep` phân biệt 2 trường hợp (\"Không tìm thấy lịch hẹn\" / "
                                                "\"Lịch hẹn này chưa có hồ sơ bệnh án\").")])])
    x += route(so, "8.4", "Lịch sử sửa hồ sơ bệnh án", "Các lần sửa hồ sơ bệnh án của 1 lượt khám, mới nhất trước: ai "
               "sửa, lúc nào, lý do, phần đã đổi, nội dung trước và sau.", [
                   tham_so([ID_LICH_HEN_MD]),
                   tra("200 OK", f"`duLieu`: {mh('LichSuSuaBenhAnResponse')}. Ví dụ: 1 lần sửa ở 8.2.", ok(LICH_SU)),
                   loi([(404, "KHONG_TIM_THAY", "Lượt khám không có, bác sĩ không được xem lượt đó (3.7), hoặc lượt "
                                                "chưa có hồ sơ bệnh án.")]),
                   kit.luu_y(["Xem được cả lần khám trước của bệnh nhân với bác sĩ khác, khi lần đó có trong "
                              "`lanKhamTruoc` của hồ sơ khám bác sĩ mở được (3.7).",
                              "Chưa sửa lần nào: `cacLanSua` = `[]`, `phienBanHienTai` = 1."])])
    x += route(so, "8.5", "Gợi ý thuốc", "Thuốc kê được (đang dùng, đã xác minh) có tên thuốc hoặc hoạt chất chứa từ "
               "khoá, tối đa 20, theo tên rồi hàm lượng (3.8).", [
                   tham_so([TU_KHOA]),
                   tra("200 OK", f"`duLieu`: danh sách {mh('ThuocGoiYResponse')}. Ví dụ: `tuKhoa=para`.",
                       ok(GOI_Y_THUOC)),
                   kit.luu_y(["Không có lỗi riêng (lỗi chung: TK 3.4). Không có thuốc khớp: `[]`.",
                              "Thuốc ngừng dùng hoặc chưa xác minh không hiện, nên không chọn được để kê mới."])])
    x += route(so, "8.6", "Gợi ý mã ICD-10", "Mã ICD-10 bắt đầu bằng từ khoá, hoặc có tên tiếng Việt / tiếng Anh chứa từ "
               "khoá, tối đa 20, theo mã (3.8).", [
                   tham_so([TU_KHOA]),
                   tra("200 OK", f"`duLieu`: danh sách {mh('Icd10Response')}. Ví dụ: `tuKhoa=tăng huyết áp`.",
                       ok(GOI_Y_ICD)),
                   kit.luu_y(["Không có lỗi riêng (lỗi chung: TK 3.4). Không có mã khớp: `[]`.",
                              "Gõ đầu mã (vd `J0`) cho các mã `J00`, `J01.9`...; `ten` theo `Accept-Language` (TK "
                              "3.12). Chọn mã có `manTinh` = `true` thì đơn được kê tới 90 ngày."])])
    return x


def sua(tl):
    for muc, method, duong, bm, chuc_nang, service in ROUTE:
        tl.them_dong(bsqt_p2_khung.MUC_2, None,
                     bsqt_p2_khung.dong_route(muc, method, duong, bm, "BAC_SI", chuc_nang, service))
    so = iter(range(tl.id_bookmark_moi(), 10 ** 6))
    bsqt_p2_khung.them_cuoi(tl, muc_7(so) + muc_8(so))
