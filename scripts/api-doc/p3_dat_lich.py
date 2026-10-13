"""Mục 11-14 của tài liệu API: xem chuyên khoa, bác sĩ, khung giờ; đặt lịch; thông tin đặt lịch đã lưu; phiếu khám.

Giai đoạn 3 của chuỗi dựng tài liệu; chạy qua build.py. JSON mẫu lấy từ response thật do lay_mau.py ghi lại
(temp/api-doc/mau), danh sách cắt còn 1-2 phần tử.
"""

import json

import kit
import p2_quy_uoc

PHU_LUC_A = "Phụ lục A. Bảng mã lỗi (maLoi)"
SO_MO_HINH = {m[0]: f"4.{i + 10}" for i, m in enumerate(p2_quy_uoc.MO_HINH)}
RONG_TRUONG_DAI = (2400, 900, 1100, 5238)  # tên trường lồng (nguoiGiamHo.soDienThoai) dài hơn bảng request thường

CONG_KHAI = "Công khai (không cần access token)"
BENH_NHAN = "Chỉ BENH_NHAN (cần access token)"


def mh(ten):
    return f"`{ten}` ({SO_MO_HINH[ten]})"


def js(du_lieu):
    return kit.khoi_code(json.dumps(du_lieu, ensure_ascii=False, indent=2))


def ok(du_lieu=None, thong_diep=None):
    khung = {"thanhCong": True}
    if thong_diep:
        khung["thongDiep"] = thong_diep
    if du_lieu is not None:
        khung["duLieu"] = du_lieu
    return khung


def trang(noi_dung, tong, kich_thuoc=20):
    return {"noiDung": noi_dung, "trang": 0, "kichThuoc": kich_thuoc, "tongSoPhanTu": tong,
            "tongSoTrang": -(-tong // kich_thuoc)}


def dan(markup):
    """Đoạn dẫn đứng ngay trên 1 khối JSON: giữ liền với khối đó khi sang trang."""
    return kit.doan(markup).replace("<w:pPr>", "<w:pPr><w:keepNext/>", 1)


def tra(ma, mo_ta, mau):
    """Phần response thành công: nhãn, dòng nói `duLieu` là mô hình nào, JSON mẫu."""
    return kit.nhan(f"RESPONSE THÀNH CÔNG — {ma}") + dan(mo_ta) + js(mau)


def tham_so(dong):
    return kit.nhan("THAM SỐ") + kit.bang_tham_so(dong)


def loi(dong):
    return kit.nhan("MÃ LỖI") + kit.bang_loi(dong)


def body(dong, rong=kit.RONG_TRUONG):
    return (kit.nhan("REQUEST BODY (JSON)")
            + kit.bang(rong, ("Trường", "Kiểu", "Bắt buộc", "Ràng buộc / mô tả"),
                       [(f"`{t}`", k, b, m) for t, k, b, m in dong]) + kit.khoang_trong(60))


TRANG = [("trang", "query", "int", "Mặc định 0, tối thiểu 0."),
         ("kichThuoc", "query", "int", "Mặc định 20, từ 1 đến 100.")]
MA_PHIEU = ("maPhieuKham", "path", "string", "Mã phiếu khám 43 ký tự (3.10).")
LOI_PHAN_TRANG = (400, "DU_LIEU_KHONG_HOP_LE", "`trang` < 0, `kichThuoc` ngoài 1..100, hoặc tham số sai kiểu.")

# ----- Dữ liệu mẫu (từ response thật) -----

BS_TOM_TAT = {"id": 5, "hoTen": "Hoàng Minh Đức", "hocVi": "ThS.BS",
              "anhDaiDien": "https://res.cloudinary.com/demo/image/upload/v1759450000/eclinic/avatar/17.jpg"}
PHONG = {"id": 3, "tenPhong": "Phòng 103", "tang": "Tầng 1"}
CHUYEN_KHOA = {"id": 3, "tenChuyenKhoa": "Da liễu",
               "moTa": "Khám và điều trị các bệnh về da, tóc, móng: mụn trứng cá, viêm da, dị ứng, nấm da."}
BAC_SI = {"id": 5, "hoTen": "Hoàng Minh Đức", "anhDaiDien": BS_TOM_TAT["anhDaiDien"], "hocVi": "ThS.BS",
          "chucVu": "Trưởng khoa Da liễu", "soNamKinhNghiem": 12,
          "gioiThieuNgan": "Điều trị mụn trứng cá, viêm da cơ địa, vảy nến và bệnh da dị ứng.",
          "idChuyenKhoa": 3, "tenChuyenKhoa": "Da liễu", "diemDanhGia": 4.7, "soDanhGia": 18}
BAC_SI_2 = {"id": 6, "hoTen": "Vũ Thị Giang", "anhDaiDien": None, "hocVi": "BS", "chucVu": "Bác sĩ điều trị",
            "soNamKinhNghiem": 5,
            "gioiThieuNgan": "Khám da liễu tổng quát, tư vấn chăm sóc da và điều trị nấm da.",
            "idChuyenKhoa": 3, "tenChuyenKhoa": "Da liễu", "diemDanhGia": None, "soDanhGia": 0}
BAC_SI_CHI_TIET = {
    **{k: v for k, v in BAC_SI.items() if k not in ("diemDanhGia", "soDanhGia")},
    "tieuSu": "Điều trị mụn trứng cá, viêm da cơ địa, vảy nến và các bệnh da dị ứng.",
    "quaTrinhDaoTao": ["2007 - 2013: Bác sĩ đa khoa, Đại học Y Dược TP. Hồ Chí Minh",
                       "2015 - 2017: Thạc sĩ Da liễu, Đại học Y Dược TP. Hồ Chí Minh"],
    "quaTrinhCongTac": ["2013 - 2020: Bác sĩ điều trị, bệnh viện chuyên khoa da liễu",
                        "2020 - nay: Trưởng khoa Da liễu, Phòng khám eClinic"],
    "linhVucKhamChua": ["Mụn trứng cá và sẹo mụn", "Viêm da cơ địa, chàm, mề đay"],
    "anh": [{"id": 13, "loai": "ANH_CONG_VIEC",
             "url": "https://res.cloudinary.com/demo/image/upload/v1759450000/eclinic/bac-si/5/13.jpg",
             "chuThich": "Bác sĩ Hoàng Minh Đức tại phòng khám Da liễu"}],
    "diemDanhGia": 4.7, "soDanhGia": 18}


def khung(ngay, gio, con_lai, tong=6):
    return {"gioBatDau": f"{ngay}T{gio:02d}:00:00", "gioKetThuc": f"{ngay}T{gio + 1:02d}:00:00", "tongSoCho": tong,
            "soChoConLai": con_lai, "hetCho": con_lai == 0}


def ca_kham(id_ca, ngay, cac_khung):
    return {"idLichLamViec": id_ca, "ngay": ngay, "gioBatDau": "08:00:00", "gioKetThuc": "11:30:00",
            "soLuotToiDaMoiGio": 6, "thoiLuongLuotPhut": 10, "bacSi": BS_TOM_TAT, "phongKham": PHONG,
            "khungGio": cac_khung}


MA_1 = "IRSiabCO4s-6vg1HCwLJDhga9Llnbs60nPaS29eUxtI"
MA_2 = "E46REPjAHCfZnIEpumdjp4GnWF_dz67qLxLgpRjJ0hE"
MA_3 = "B567sPX2cd-z72xSSI6UhqckE8QsAza7lmohF9A_GaI"
LINK = "http://localhost:5173/phieu-kham/"

NGUOI_LON = {"hoTen": "Trần Văn Khách", "ngaySinh": "1990-03-15", "gioiTinh": "NAM", "soDienThoai": "0912345601",
             "cccd": "079190000101", "email": "khach@example.com", "soBaoHiemYTe": "DN4797912345601",
             "diaChi": "12 Nguyễn Văn Bảo, Gò Vấp, TP.HCM"}
DAT_NGUOI_LON = {"idLichLamViec": 15, "gioBatDauKhung": "2026-10-10T08:00:00", "benhNhan": NGUOI_LON,
                 "lyDoKham": "Đau đầu, chóng mặt 3 ngày nay"}
DAT_TRE_EM = {"idChuyenKhoa": 3, "gioBatDauKhung": "2026-10-10T08:00:00",
              "benhNhan": {"hoTen": "Trần Gia Bảo", "ngaySinh": "2018-06-01", "gioiTinh": "NAM",
                           "soDienThoai": "0912345601"},
              "nguoiGiamHo": {"hoTen": "Trần Văn Khách", "quanHe": "CHA", "soDienThoai": "0912345601",
                              "cccd": "079190000101"},
              "lyDoKham": "Sốt, ho"}
DAT_BAN_THAN = {"idLichLamViec": 15, "gioBatDauKhung": "2026-10-10T09:00:00",
                "benhNhan": {"hoTen": "Nguyễn Thị Mai", "ngaySinh": "1995-03-12", "gioiTinh": "NU",
                             "soDienThoai": "0902000001", "cccd": "079000000001"},
                "lyDoKham": "Khám định kỳ", "datChoBanThan": True}
DA_DAT = {"maPhieuKham": MA_1, "maTraCuu": "ECL-20261010-0622", "linkPhieuKham": LINK + MA_1, "soThuTu": 1,
          "ngay": "2026-10-10", "gioKhamDuKien": "2026-10-10T08:00:00", "gioBatDauKhung": "2026-10-10T08:00:00",
          "gioKetThucKhung": "2026-10-10T09:00:00", "trangThai": "CHO_XAC_NHAN", "bacSi": BS_TOM_TAT,
          "phongKham": PHONG, "hoTenBenhNhan": "Trần Văn Khách", "hoTenNguoiGiamHo": None, "luuVaoTaiKhoan": False,
          "bacSiDoPhongKhamXep": False}
DA_DOI = {**DA_DAT, "maPhieuKham": MA_2, "maTraCuu": "ECL-20261010-0869", "linkPhieuKham": LINK + MA_2, "soThuTu": 7,
          "gioKhamDuKien": "2026-10-10T09:00:00", "gioBatDauKhung": "2026-10-10T09:00:00",
          "gioKetThucKhung": "2026-10-10T10:00:00"}
PHIEU = {"maPhieuKham": MA_1, "maTraCuu": "ECL-20261010-0622", "linkPhieuKham": LINK + MA_1,
         "trangThai": "CHO_XAC_NHAN", "soThuTu": 1, "ngay": "2026-10-10", "gioKhamDuKien": "2026-10-10T08:00:00",
         "gioBatDauKhung": "2026-10-10T08:00:00", "gioKetThucKhung": "2026-10-10T09:00:00", "bacSi": BS_TOM_TAT,
         "tenChuyenKhoa": "Da liễu", "phongKham": PHONG, "lyDoKham": "Đau đầu, chóng mặt 3 ngày nay",
         "ngayDat": "2026-10-09T17:02:27.576264",
         "benhNhan": {"hoTen": "Trần Văn Khách", "namSinh": 1990, "gioiTinh": "NAM", "cccd": "*********101",
                      "soDienThoai": "091****601"},
         "nguoiGiamHo": None, "canNguoiGiamHoDiCung": False, "lyDoHuy": None, "duocHuyDoi": True,
         "hanHuyDoi": "2026-10-10T06:00:00", "canDoiLich": False}
PHIEU_TRE_EM = {**PHIEU, "maPhieuKham": MA_3, "maTraCuu": "ECL-20261010-4353", "linkPhieuKham": LINK + MA_3,
                "soThuTu": 2, "gioKhamDuKien": "2026-10-10T08:10:00", "lyDoKham": "Sốt, ho",
                "ngayDat": "2026-10-09T17:02:27.672243",
                "benhNhan": {"hoTen": "Trần Gia Bảo", "namSinh": 2018, "gioiTinh": "NAM", "cccd": None,
                             "soDienThoai": None},
                "nguoiGiamHo": {"hoTen": "Trần Văn Khách", "quanHe": "CHA", "soDienThoai": "091****601"},
                "canNguoiGiamHoDiCung": True, "hanHuyDoi": "2026-10-10T06:10:00"}
PHIEU_DA_HUY = {**PHIEU, "trangThai": "DA_HUY", "lyDoHuy": "Bận việc đột xuất", "duocHuyDoi": False}

NGUOI_THAN_ME = {"id": 1, "hoTen": "Lê Thị Hoa", "ngaySinh": "1960-02-10", "gioiTinh": "NU", "cccd": "079160000301",
                 "soDienThoai": "0912345777", "email": None, "diaChi": "5 Lê Lợi, Quận 1, TP.HCM",
                 "soBaoHiemYTe": None, "nguoiGiamHo": None, "lanDungCuoi": "2026-10-09T17:02:29.853613"}
NGUOI_THAN_CON = {"id": 2, "hoTen": "Nguyễn Gia Huy", "ngaySinh": "2015-09-09", "gioiTinh": "NAM", "cccd": None,
                  "soDienThoai": "0902000001", "email": None, "diaChi": None, "soBaoHiemYTe": None,
                  "nguoiGiamHo": {"hoTen": "Nguyễn Thị Mai", "quanHe": "ME", "soDienThoai": "0902000001",
                                  "cccd": "079000000001", "ngaySinh": None},
                  "lanDungCuoi": "2026-10-09T17:02:29.92757"}
THONG_TIN_DAT_LICH = {
    "banThan": {"trangThaiLienKet": "DA_LIEN_KET", "cccd": "079000000001", "hoTen": "Nguyễn Thị Mai",
                "ngaySinh": "1995-03-12", "gioiTinh": "NU", "soDienThoai": "0902000001",
                "diaChi": "12 Nguyễn Văn Bảo, Phường 4, Gò Vấp, TP. Hồ Chí Minh", "soBaoHiemYTe": None,
                "tienSuBenhLy": None},
    "emailTaiKhoan": "benhnhan01@eclinic.local",
    "nguoiThan": [NGUOI_THAN_CON, NGUOI_THAN_ME],
    "lanDatGanNhat": {"idChuyenKhoa": 3, "tenChuyenKhoa": "Da liễu", "idBacSi": 5, "hoTenBacSi": "Hoàng Minh Đức"}}
SUA_NGUOI_THAN = {"hoTen": "Lê Thị Hoa", "ngaySinh": "1960-02-10", "gioiTinh": "NU", "soDienThoai": "0912345778",
                  "email": "hoa@example.com", "soBaoHiemYTe": "HT3797900000301",
                  "diaChi": "5 Lê Lợi, Quận 1, TP.HCM"}

# ----- Bảng request dùng lại -----

CHON_KHUNG = [
    ("idLichLamViec", "number", "Một trong hai",
     "Chọn bác sĩ cụ thể: id ca khám, lấy từ `CaKhamResponse.idLichLamViec` (12.1, 12.3)."),
    ("idChuyenKhoa", "number", "Một trong hai",
     "Bác sĩ bất kỳ của chuyên khoa: hệ thống xếp bác sĩ còn nhiều chỗ nhất ở khung giờ này (12.4)."),
    ("gioBatDauKhung", "datetime", "Có",
     "Giờ bắt đầu của khung 1 tiếng đã chọn, đúng bằng `gioBatDau` của khung giờ nhận từ mục 12."),
]
LOI_CHON_KHUNG = [
    (409, "KHUNG_GIO_KHONG_KHA_DUNG",
     "Khung giờ không còn nhận đặt: còn dưới 30 phút, ngoài 30 ngày tới, ca khám đã bị hủy, bác sĩ ngừng công tác, "
     "hoặc (với `idChuyenKhoa`) không bác sĩ nào của chuyên khoa có khung bắt đầu đúng giờ này."),
    (409, "KHUNG_GIO_KHONG_CON_TRONG", "Khung giờ đã đủ lượt khám. Tải lại khung giờ và mời chọn khung khác."),
    (409, "LICH_HEN_TRUNG_GIO", "Người khám đã có 1 lịch hẹn còn hiệu lực trong cùng khung giờ."),
    (409, "VUOT_GIOI_HAN_DAT_LICH",
     "Người khám đang giữ 3 lịch sắp tới, hoặc số điện thoại liên hệ đang giữ 5 lịch sắp tới (3.6)."),
    (429, "GUI_LAI_QUA_NHANH", "Quá 20 lần gọi đặt / đổi / hủy bằng phiếu khám từ cùng địa chỉ IP trong 10 phút."),
]
LOI_PHIEU = (404, "KHONG_TIM_THAY", "Mã phiếu khám sai dạng hoặc không tồn tại (cùng một thông điệp).")
LOI_SDT = [
    (400, "DU_LIEU_KHONG_HOP_LE",
     "Thiếu body, `soDienThoai` thiếu hoặc không phải 10 chữ số bắt đầu bằng 0 (không có `chiTiet`)."),
    (403, "SO_DIEN_THOAI_KHONG_KHOP", "Số điện thoại không trùng số liên hệ đã nhập lúc đặt lịch."),
]
LOI_HUY_DOI = [
    (409, "LICH_HEN_KHONG_HUY_DOI_DUOC",
     "Lịch không còn `CHO_XAC_NHAN` / `DA_XAC_NHAN`: đã khám xong, đã hủy, đã đổi sang lịch khác hoặc bị từ chối."),
    (409, "QUA_HAN_HUY_DOI_LICH", "Đã qua `hanHuyDoi` (2 giờ trước giờ khám dự kiến; xem 3.11)."),
]


def muc_11(tl, so):
    return (kit.tieu_de(1, "11. Chuyên khoa và bác sĩ") + kit.doan(
        "Các route đọc công khai của `catalog-service`, dùng cho bước chọn chuyên khoa, chọn bác sĩ của form đặt "
        "lịch và cho trang giới thiệu bác sĩ. Danh sách và chi tiết bác sĩ chỉ gồm bác sĩ **đang công tác** có tài "
        "khoản đã kích hoạt. Các route thêm / sửa / xoá trên cùng đường dẫn là của quản trị viên, không thuộc tài "
        "liệu này.")
        + kit.route(
            "11.1", "Danh sách chuyên khoa", "GET", "/api/catalog/chuyen-khoa", "ep_chuyen_khoa_ds", next(so),
            CONG_KHAI, "Phân trang, sắp theo tên chuyên khoa. Phòng khám có ít chuyên khoa: gọi 1 lần với "
            "`kichThuoc=100` để lấy hết cho ô chọn.",
            [tham_so([("tuKhoa", "query", "string", "Không bắt buộc. Tên chuyên khoa chứa chuỗi này.")] + TRANG),
             tra("200 OK", f"`duLieu`: trang (3.3) của {mh('ChuyenKhoaResponse')}.",
                 ok(trang([CHUYEN_KHOA, {"id": 7, "tenChuyenKhoa": "Mắt", "moTa": None}], 8))),
             loi([LOI_PHAN_TRANG])])
        + kit.route(
            "11.2", "Chi tiết chuyên khoa", "GET", "/api/catalog/chuyen-khoa/{id}", "ep_chuyen_khoa_chi_tiet",
            next(so), CONG_KHAI, "Một chuyên khoa theo id.",
            [tham_so([("id", "path", "number", "Id chuyên khoa.")]),
             tra("200 OK", f"`duLieu`: {mh('ChuyenKhoaResponse')}.", ok(CHUYEN_KHOA)),
             loi([(404, "KHONG_TIM_THAY", "Không có chuyên khoa với id này.")])])
        + kit.route(
            "11.3", "Tìm bác sĩ", "GET", "/api/catalog/bac-si", "ep_bac_si_ds", next(so), CONG_KHAI,
            "Danh sách bác sĩ theo chuyên khoa và / hoặc tên, phân trang, sắp theo họ tên. Dùng cho thẻ bác sĩ ở bước "
            "chọn bác sĩ; ngày còn chỗ sớm nhất của từng bác sĩ lấy ở 12.5.",
            [tham_so([("idChuyenKhoa", "query", "number", "Không bắt buộc. Chỉ lấy bác sĩ của chuyên khoa này."),
                      ("tuKhoa", "query", "string",
                       "Không bắt buộc. Họ tên bác sĩ chứa chuỗi này; ký tự `%` và `_` được hiểu đúng nghĩa đen.")]
                     + TRANG),
             tra("200 OK", f"`duLieu`: trang (3.3) của {mh('BacSiResponse')}.", ok(trang([BAC_SI, BAC_SI_2], 2))),
             loi([LOI_PHAN_TRANG]),
             kit.luu_y(["`idChuyenKhoa` không tồn tại không phải là lỗi: trả trang rỗng.",
                        "`diemDanhGia` là `null` khi bác sĩ chưa có đánh giá nào (`soDanhGia` = 0); `anhDaiDien` là "
                        "`null` khi bác sĩ chưa có ảnh."])])
        + kit.route(
            "11.4", "Chi tiết bác sĩ", "GET", "/api/catalog/bac-si/{id}", "ep_bac_si_chi_tiet", next(so), CONG_KHAI,
            "Hồ sơ giới thiệu đầy đủ của 1 bác sĩ: tiểu sử, quá trình đào tạo và công tác, lĩnh vực khám chữa, ảnh "
            "giới thiệu, điểm đánh giá.",
            [tham_so([("id", "path", "number", "Id bác sĩ (`BacSiResponse.id`).")]),
             tra("200 OK", f"`duLieu`: {mh('BacSiChiTietResponse')}.", ok(BAC_SI_CHI_TIET)),
             loi([(404, "KHONG_TIM_THAY",
                   "Không có bác sĩ với id này, hoặc bác sĩ đã ngừng công tác / tài khoản chưa kích hoạt.")])]))


def muc_12(tl, so):
    ngay = "2026-10-10"
    ca = ca_kham(15, ngay, [khung(ngay, 8, 4), khung(ngay, 9, 0), khung(ngay, 10, 6)])
    return (kit.tieu_de(1, "12. Khung giờ khám") + kit.doan(
        "Các route đọc công khai của `booking-service`. Mỗi **ca khám** (`CaKhamResponse`) là 1 buổi làm việc của 1 "
        "bác sĩ tại 1 phòng, chia thành các **khung giờ 1 tiếng**; bệnh nhân chọn khung giờ, hệ thống xếp lượt khám "
        "và số thứ tự trong khung đó.")
        + kit.doan(
            "Chỉ trả những gì còn đặt được: từ 30 phút tới và trong 30 ngày tới (3.6). Ngày đã qua, ngày ngoài 30 "
            "ngày, id bác sĩ / chuyên khoa không tồn tại đều cho **danh sách rỗng**, không phải lỗi. Thứ tự gọi thường "
            "dùng: 12.5 (ngày sớm nhất của từng bác sĩ) → 12.2 (tô lịch tháng) → 12.1 hoặc 12.3 (khung giờ của bác "
            "sĩ đã chọn), hoặc 12.4 khi bệnh nhân chọn \"bác sĩ bất kỳ\".")
        + kit.route(
            "12.1", "Khung giờ theo ngày", "GET", "/api/booking/khung-gio", "ep_khung_gio", next(so), CONG_KHAI,
            "Các ca khám của 1 ngày kèm khung giờ 1 tiếng và số chỗ còn lại, theo bác sĩ hoặc theo chuyên khoa (ca của mọi "
            "bác sĩ thuộc chuyên khoa, sắp theo họ tên bác sĩ rồi giờ bắt đầu ca).",
            [tham_so([("ngay", "query", "date", "Bắt buộc. Dạng `2026-10-10`."),
                      ("idBacSi", "query", "number", "Phải có ít nhất 1 trong 2: `idBacSi`, `idChuyenKhoa`."),
                      ("idChuyenKhoa", "query", "number", "Gửi cả hai thì chỉ `idBacSi` được dùng.")]),
             tra("200 OK", f"`duLieu`: danh sách {mh('CaKhamResponse')}.", ok([ca])),
             loi([(400, "DU_LIEU_KHONG_HOP_LE",
                   "Thiếu `ngay`, `ngay` sai dạng, hoặc không gửi cả `idBacSi` lẫn `idChuyenKhoa`.")]),
             kit.luu_y([
                 "Khung có `hetCho = true` vẫn được trả (để hiện mờ) chừng nào khung chưa qua hạn đặt; khung đã qua "
                 "hạn và ca không còn khung nào đặt được thì không có trong kết quả.",
                 "Khi đặt lịch gửi `idLichLamViec` của ca và `gioBatDau` của khung (13.1).",
                 "`soChoConLai` chỉ đúng tại lúc gọi: người khác có thể đặt mất chỗ, khi đó 13.1 trả 409 "
                 "`KHUNG_GIO_KHONG_CON_TRONG`."])])
        + kit.route(
            "12.2", "Các ngày có ca khám", "GET", "/api/booking/khung-gio/ngay-con-cho", "ep_ngay_con_cho", next(so),
            CONG_KHAI, "Mỗi ngày có ca khám trong khoảng, kèm tổng số chỗ còn lại của ngày. Dùng để tô màu lịch chọn "
            "ngày: ngày không có trong kết quả là không có ca, `soChoConLai = 0` là hết chỗ.",
            [tham_so([("tuNgay", "query", "date", "Không bắt buộc, mặc định hôm nay. Ngày trước hôm nay được tính "
                       "từ hôm nay."),
                      ("denNgay", "query", "date", "Không bắt buộc, mặc định và tối đa là hôm nay + 30 ngày."),
                      ("idBacSi", "query", "number", "Phải có ít nhất 1 trong 2: `idBacSi`, `idChuyenKhoa`."),
                      ("idChuyenKhoa", "query", "number", "Tính gộp mọi bác sĩ của chuyên khoa; bị bỏ qua khi có `idBacSi`.")]),
             tra("200 OK", f"`duLieu`: danh sách {mh('NgayConChoResponse')}, theo ngày tăng dần.",
                 ok([{"ngay": "2026-10-10", "soChoConLai": 21}, {"ngay": "2026-10-12", "soChoConLai": 0}])),
             loi([(400, "DU_LIEU_KHONG_HOP_LE",
                   "`denNgay` trước `tuNgay`, ngày sai dạng, hoặc không gửi cả `idBacSi` lẫn `idChuyenKhoa`.")])])
        + kit.route(
            "12.3", "Khung giờ nhiều ngày của một bác sĩ", "GET", "/api/booking/khung-gio/nhieu-ngay",
            "ep_khung_gio_nhieu_ngay", next(so), CONG_KHAI,
            "Như 12.1 nhưng cho tối đa 7 ngày liền của 1 bác sĩ trong 1 lần gọi (lưới \"tuần này\" trên trang bác sĩ).",
            [tham_so([("idBacSi", "query", "number", "Bắt buộc."),
                      ("tuNgay", "query", "date", "Không bắt buộc, mặc định hôm nay."),
                      ("denNgay", "query", "date", "Không bắt buộc, mặc định `tuNgay` + 6 ngày.")]),
             tra("200 OK", f"`duLieu`: danh sách {mh('CaKhamResponse')}; mỗi ca là 1 phần tử, ngày không có ca thì "
                 "không có phần tử nào.",
                 ok([ca_kham(15, "2026-10-10", [khung("2026-10-10", 8, 4)]),
                     ca_kham(25, "2026-10-12", [khung("2026-10-12", 8, 6)])])),
             loi([(400, "DU_LIEU_KHONG_HOP_LE",
                   "Thiếu `idBacSi`, `denNgay` trước `tuNgay`, hoặc khoảng dài hơn 7 ngày.")])])
        + kit.route(
            "12.4", "Khung giờ gộp của chuyên khoa", "GET", "/api/booking/khung-gio/gop", "ep_khung_gio_gop",
            next(so), CONG_KHAI,
            "Cho lựa chọn \"bác sĩ bất kỳ\": các khung giờ của mọi bác sĩ thuộc chuyên khoa trong 1 ngày, gộp theo giờ "
            "bắt đầu và cộng số chỗ.",
            [tham_so([("idChuyenKhoa", "query", "number", "Bắt buộc."),
                      ("ngay", "query", "date", "Bắt buộc.")]),
             tra("200 OK", f"`duLieu`: danh sách {mh('KhungGioGopResponse')}, theo giờ bắt đầu.",
                 ok([{**khung("2026-10-10", 8, 9, 12), "soBacSi": 2},
                     {**khung("2026-10-10", 9, 6), "soBacSi": 1}])),
             loi([(400, "DU_LIEU_KHONG_HOP_LE", "Thiếu `idChuyenKhoa` hoặc `ngay`, hoặc `ngay` sai dạng.")]),
             kit.luu_y(["Khi đặt lịch gửi `idChuyenKhoa` và `gioBatDau` của khung (không gửi `idLichLamViec`); bác sĩ "
                        "được xếp nằm trong response đặt lịch (`bacSiDoPhongKhamXep = true`)."])])
        + kit.route(
            "12.5", "Ngày còn chỗ sớm nhất của từng bác sĩ", "GET", "/api/booking/khung-gio/ngay-som-nhat",
            "ep_ngay_som_nhat", next(so), CONG_KHAI,
            "Với mỗi bác sĩ của chuyên khoa: ngày gần nhất còn chỗ và số chỗ còn lại của ngày đó. Ghép với danh sách "
            "bác sĩ (11.3) theo `idBacSi` để hiện \"Lịch sớm nhất: …\" trên thẻ bác sĩ.",
            [tham_so([("idChuyenKhoa", "query", "number", "Bắt buộc.")]),
             tra("200 OK", f"`duLieu`: danh sách {mh('NgaySomNhatResponse')}.",
                 ok([{"idBacSi": 5, "ngay": "2026-10-10", "soChoConLai": 21},
                     {"idBacSi": 6, "ngay": "2026-10-12", "soChoConLai": 21}])),
             loi([(400, "DU_LIEU_KHONG_HOP_LE", "Thiếu `idChuyenKhoa`.")]),
             kit.luu_y(["Bác sĩ không còn chỗ nào trong 30 ngày tới thì không có trong kết quả: hiện \"hết lịch\" "
                        "cho bác sĩ đó."])]))


def muc_13(tl, so):
    return (kit.tieu_de(1, "13. Đặt lịch khám") + kit.doan(
        "Route đặt lịch dùng chung cho khách và bệnh nhân đã đăng nhập (3.9), và các route thông tin đã lưu để điền "
        "sẵn form đặt lịch của bệnh nhân đã đăng nhập.")
        + kit.route(
            "13.1", "Đặt lịch khám", "POST", "/api/booking/lich-hen", "ep_dat_lich", next(so),
            "Công khai; có access token của BENH_NHAN thì lịch lưu vào tài khoản",
            ["Đặt 1 lịch hẹn cho 1 người khám vào 1 khung giờ. Hệ thống tự chọn lượt khám trống sớm nhất trong khung, "
             "cấp số thứ tự và trả mã phiếu khám. Lịch mới ở trạng thái `CHO_XAC_NHAN`.",
             "Cách xử lý theo token (khách, bệnh nhân, vai trò khác, token hết hạn) ở 3.9."],
            [body(CHON_KHUNG + [
                ("benhNhan", "object", "Có", "Người khám."),
                ("benhNhan.hoTen", "string", "Có", "Không rỗng, tối đa 150 ký tự."),
                ("benhNhan.ngaySinh", "date", "Có", "Không ở tương lai. Dùng để tính tuổi theo ngày khám."),
                ("benhNhan.gioiTinh", "enum", "Có", "`NAM`, `NU`, `KHAC`."),
                ("benhNhan.soDienThoai", "string", "Có", "10 chữ số, bắt đầu bằng 0."),
                ("benhNhan.cccd", "string", "Từ đủ 18 tuổi",
                 "Đúng 12 chữ số. Bắt buộc khi người khám từ đủ 18 tuổi tính theo ngày khám, và khi "
                 "`datChoBanThan = true`; trẻ chưa có CCCD thì bỏ trống."),
                ("benhNhan.email", "string", "Không", "Email liên hệ của lượt khám, tối đa 255 ký tự."),
                ("benhNhan.soBaoHiemYTe", "string", "Không", "Tối đa 50 ký tự."),
                ("benhNhan.diaChi", "string", "Không", "Tối đa 500 ký tự."),
                ("nguoiGiamHo", "object", "Dưới 18 tuổi",
                 "Bắt buộc khi người khám dưới 18 tuổi tính theo ngày khám; từ đủ 18 tuổi thì bị bỏ qua."),
                ("nguoiGiamHo.hoTen", "string", "Có", "Không rỗng, tối đa 150 ký tự."),
                ("nguoiGiamHo.quanHe", "enum", "Có", "`CHA`, `ME`, `NGUOI_GIAM_HO_HOP_PHAP`, `KHAC`."),
                ("nguoiGiamHo.soDienThoai", "string", "Có", "10 chữ số, bắt đầu bằng 0."),
                ("nguoiGiamHo.cccd", "string", "Có", "Đúng 12 chữ số, khác CCCD của người khám."),
                ("nguoiGiamHo.ngaySinh", "date", "Không", "Ở quá khứ; có gửi thì phải từ đủ 18 tuổi."),
                ("lyDoKham", "string", "Có", "Không rỗng, tối đa 500 ký tự."),
                ("datChoBanThan", "boolean", "Không",
                 "Chỉ có nghĩa khi có token bệnh nhân. `true`: người khám là chủ tài khoản, lịch dùng hồ sơ bệnh nhân "
                 "của tài khoản (chưa có thì tạo và gắn vào tài khoản). Bỏ trống / `false`: đặt cho người thân."),
                ("luuNguoiThan", "boolean", "Không",
                 "Chỉ có nghĩa khi bệnh nhân đã đăng nhập đặt cho người thân. Bỏ trống / `true`: lưu thông tin vừa "
                 "nhập vào người thân đã lưu (13.2); `false`: không lưu."),
            ], RONG_TRUONG_DAI),
             dan("Người lớn, chọn bác sĩ cụ thể:"), js(DAT_NGUOI_LON),
             dan("Trẻ em chưa có CCCD, bác sĩ bất kỳ của chuyên khoa:"), js(DAT_TRE_EM),
             dan("Bệnh nhân đã đăng nhập đặt cho bản thân (gửi kèm `Authorization: Bearer …`):"),
             js(DAT_BAN_THAN),
             tra("201 Created", f"`duLieu`: {mh('DatLichResponse')}.", ok(DA_DAT, "Đặt lịch thành công")),
             loi([
                 (400, "DU_LIEU_KHONG_HOP_LE",
                  "Body sai quy tắc: `chiTiet` liệt kê từng trường, tên trường lồng có dấu chấm (vd "
                  "`benhNhan.ngaySinh`). Không có `chiTiet` (đọc `thongDiep`) khi: gửi cả hai hoặc không gửi cái nào "
                  "trong `idLichLamViec`, `idChuyenKhoa`; `gioBatDauKhung` không phải giờ bắt đầu 1 khung của ca; "
                  "người khám từ đủ 18 tuổi thiếu CCCD; `datChoBanThan = true` mà thiếu CCCD."),
                 (400, "THIEU_NGUOI_GIAM_HO", "Người khám dưới 18 tuổi mà không gửi `nguoiGiamHo`."),
                 (400, "NGUOI_GIAM_HO_KHONG_HOP_LE",
                  "CCCD người giám hộ trùng CCCD người khám, hoặc người giám hộ chưa đủ 18 tuổi."),
                 (403, "KHONG_CO_QUYEN", "Token của bác sĩ hoặc quản trị viên (3.9)."),
                 (404, "KHONG_TIM_THAY", "`idLichLamViec` không tồn tại."),
             ] + LOI_CHON_KHUNG[:4] + [
                 (409, "CCCD_DA_CO_HO_SO",
                  "`datChoBanThan = true`, tài khoản chưa có hồ sơ bệnh nhân nhưng số CCCD này đã có hồ sơ (vd từng "
                  "đặt như khách). Đặt lại không chọn \"cho bản thân\", hoặc liên kết hồ sơ trước (3.9)."),
                 (409, "HO_SO_CHO_XAC_MINH",
                  "`datChoBanThan = true` khi hồ sơ của tài khoản còn `CHO_XAC_MINH`."),
                 (409, "THONG_TIN_BENH_NHAN_KHONG_KHOP",
                  "`datChoBanThan = true` nhưng CCCD gửi lên khác CCCD trong hồ sơ của tài khoản."),
             ] + LOI_CHON_KHUNG[4:]),
             kit.luu_y([
                 "Số điện thoại liên hệ của lịch hẹn là số của **người giám hộ** nếu có người giám hộ, ngược lại là "
                 "`benhNhan.soDienThoai`. Đây là số phải nhập khi hủy / đổi bằng link phiếu khám (14.3, 14.4) và là "
                 "số tính giới hạn 5 lịch.",
                 "Số CCCD là khóa của hồ sơ bệnh nhân. CCCD đã có hồ sơ mà họ tên / ngày sinh nhập khác hồ sơ thì "
                 "lịch **vẫn được đặt** (quầy tiếp đón sẽ đối chiếu); việc đặt lịch không sửa hồ sơ đã có. Phiếu khám "
                 "hiện đúng họ tên người đặt đã nhập.",
                 "Khách phải giữ `linkPhieuKham` (hoặc `maPhieuKham`): đó là cách duy nhất để xem lại, hủy, đổi lịch "
                 "khi không có tài khoản. Sau khi đặt, chuyển tới trang phiếu khám (14.1).",
                 "`gioKhamDuKien` là giờ của lượt khám được xếp trong khung (mỗi lượt `thoiLuongLuotPhut` phút); "
                 "`soThuTu` tính theo phòng khám trong ngày.",
                 "Giới hạn 20 lần / 10 phút / IP đếm cả lần gọi thất bại (404, 409...), không đếm lần bị 400 vì body "
                 "sai quy tắc có `chiTiet` và lần bị 403 vì vai trò.",
                 "Hai request cùng tạo hồ sơ cho 1 số CCCD mới có thể nhận 409 `XUNG_DOT_DU_LIEU` (3.4): gửi lại là "
                 "được. Nên khóa nút đặt lịch trong lúc chờ response để tránh bấm 2 lần.",
             ])])
        + kit.route(
            "13.2", "Thông tin điền sẵn form đặt lịch", "GET", "/api/booking/thong-tin-dat-lich/cua-toi",
            "ep_thong_tin_dat_lich", next(so), BENH_NHAN,
            "Gọi khi bệnh nhân đã đăng nhập mở form đặt lịch: hồ sơ bệnh nhân của tài khoản (cho lựa chọn \"đặt cho "
            "bản thân\"), các người thân đã lưu (cho \"đặt cho người thân\"), chuyên khoa và bác sĩ của lần đặt gần "
            "nhất.",
            [tra("200 OK", f"`duLieu`: {mh('ThongTinDatLichResponse')}.", ok(THONG_TIN_DAT_LICH)),
             kit.luu_y([
                 "`banThan = null`: tài khoản chưa có hồ sơ bệnh nhân; form \"cho bản thân\" điền họ tên, số điện "
                 "thoại từ `GET /api/users/me` và để bệnh nhân nhập phần còn lại (kể cả CCCD).",
                 "`banThan.trangThaiLienKet = CHO_XAC_MINH`: không đặt \"cho bản thân\" được (13.1 trả 409 "
                 "`HO_SO_CHO_XAC_MINH`); ẩn lựa chọn đó và hiện thông báo chờ xác minh.",
                 "`nguoiThan` sắp theo lần dùng gần nhất, tối đa 10 người. Người thân được lưu tự động sau mỗi lần "
                 "đặt cho người thân (trừ khi gửi `luuNguoiThan: false`), đặt lại cho cùng người thì bản lưu được cập "
                 "nhật.",
                 "`lanDatGanNhat = null` khi tài khoản chưa có lịch hẹn nào.",
                 "Route này cũng thử liên kết hồ sơ theo CCCD đã khai lúc đăng ký (3.9).",
             ])])
        + kit.route(
            "13.3", "Sửa người thân đã lưu", "PUT", "/api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id}",
            "ep_sua_nguoi_than", next(so), BENH_NHAN,
            "Sửa bản lưu của 1 người thân. Gửi đủ mọi trường (ghi đè toàn bộ): trường không bắt buộc gửi trống là xoá "
            "trắng trường đó.",
            [tham_so([("id", "path", "number", "`id` của người thân trong `nguoiThan` (13.2).")]),
             body([
                 ("hoTen", "string", "Có", "Không rỗng, tối đa 150 ký tự."),
                 ("ngaySinh", "date", "Có", "Không ở tương lai."),
                 ("gioiTinh", "enum", "Có", "`NAM`, `NU`, `KHAC`."),
                 ("soDienThoai", "string", "Có", "10 chữ số, bắt đầu bằng 0."),
                 ("email", "string", "Không", "Đúng định dạng, tối đa 255 ký tự."),
                 ("soBaoHiemYTe", "string", "Không", "Tối đa 50 ký tự."),
                 ("diaChi", "string", "Không", "Tối đa 500 ký tự."),
                 ("nguoiGiamHo", "object", "Không",
                  "Các trường như `nguoiGiamHo` ở 13.1. Không gửi hoặc `null`: xoá người giám hộ đã lưu."),
             ]),
             js(SUA_NGUOI_THAN),
             tra("200 OK", f"`duLieu`: {mh('NguoiThanDaLuuResponse')} sau khi sửa.",
                 ok({**NGUOI_THAN_ME, "soDienThoai": "0912345778", "email": "hoa@example.com",
                     "soBaoHiemYTe": "HT3797900000301"}, "Đã cập nhật thông tin người thân")),
             loi([(400, "DU_LIEU_KHONG_HOP_LE", "Body sai quy tắc; danh sách lỗi từng trường nằm trong `chiTiet`."),
                  (404, "KHONG_TIM_THAY", "Không có người thân với id này trong danh sách của tài khoản đang gọi.")]),
             kit.luu_y([
                 "Không có trường `cccd`: số CCCD của người thân đã lưu không đổi được. Nhập sai CCCD thì bỏ người "
                 "thân này (13.4) và đặt lịch lại với số đúng.",
                 "Chỉ sửa bản lưu dùng để điền sẵn form; hồ sơ bệnh nhân của người thân và các lịch hẹn đã đặt không "
                 "thay đổi.",
             ])])
        + kit.route(
            "13.4", "Bỏ người thân đã lưu", "DELETE", "/api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id}",
            "ep_xoa_nguoi_than", next(so), BENH_NHAN,
            "Bỏ 1 người thân khỏi danh sách đã lưu. Lịch hẹn đã đặt cho người này không bị ảnh hưởng và vẫn nằm trong "
            "\"lịch hẹn của tôi\".",
            [tham_so([("id", "path", "number", "`id` của người thân trong `nguoiThan` (13.2).")]),
             tra("200 OK", "Không có `duLieu`.", ok(thong_diep="Đã bỏ người thân khỏi danh sách đã lưu")),
             loi([(404, "KHONG_TIM_THAY",
                   "Không có người thân với id này trong danh sách của tài khoản đang gọi (kể cả đã bỏ rồi).")])]))


def muc_14(tl, so):
    return (kit.tieu_de(1, "14. Phiếu khám") + kit.doan(
        "Các route công khai tra theo `maPhieuKham` (3.10), dùng cho trang `/phieu-kham/:maPhieuKham` của frontend: "
        "khách và người nhận được link đều xem được phiếu, tải mã QR, và hủy / đổi lịch nếu nhập đúng số điện thoại "
        "liên hệ. Bệnh nhân đã đăng nhập hủy / đổi lịch của mình bằng các route `/cua-toi` (không cần số điện thoại).")
        + kit.route(
            "14.1", "Xem phiếu khám", "GET", "/api/booking/phieu-kham/{maPhieuKham}", "ep_phieu_kham", next(so),
            CONG_KHAI, "Phiếu khám của 1 lịch hẹn: số thứ tự, giờ khám dự kiến, bác sĩ, phòng, trạng thái, và có còn "
            "hủy / đổi được không. CCCD, số điện thoại và ngày sinh đã che (3.10).",
            [tham_so([MA_PHIEU]),
             tra("200 OK", f"`duLieu`: {mh('PhieuKhamResponse')}.", ok(PHIEU)),
             dan("Phiếu của trẻ em chưa có CCCD, có người giám hộ:"), js(ok(PHIEU_TRE_EM)),
             loi([LOI_PHIEU]),
             kit.luu_y([
                 "Trạng thái trên phiếu thay đổi theo thời gian (bác sĩ xác nhận, từ chối...): gọi lại route này mỗi "
                 "lần mở trang, không lưu phiếu ở frontend.",
                 "Hiện nút hủy / đổi theo `duocHuyDoi`; khi `canDoiLich = true` hiện lời nhắc đổi lịch (3.11).",
                 "Lịch có người giám hộ: `benhNhan.soDienThoai` là `null`, số liên hệ (đã che) nằm ở "
                 "`nguoiGiamHo.soDienThoai`. Trẻ chưa có CCCD: `benhNhan.cccd` là `null`.",
                 "`ngayDat` có phần lẻ của giây (vd `2026-10-09T17:02:27.576264`); dùng `new Date(...)` hoặc thư viện "
                 "ngày giờ để đọc, không cắt chuỗi theo độ dài cố định.",
                 "Phiếu của lịch đã đổi vẫn mở được và có `trangThai = DA_HUY_DO_DOI_LICH`; response không chứa mã "
                 "của phiếu mới.",
             ])])
        + kit.route(
            "14.2", "Ảnh mã QR của phiếu khám", "GET", "/api/booking/phieu-kham/{maPhieuKham}/qr", "ep_phieu_kham_qr",
            next(so), CONG_KHAI,
            "Trả **ảnh PNG** (không phải JSON) của mã QR chứa `linkPhieuKham`. Đặt thẳng URL này vào `src` của thẻ "
            "`<img>`.",
            [tham_so([MA_PHIEU,
                      ("kichThuoc", "query", "int", "Không bắt buộc. Cạnh ảnh tính bằng pixel, 100 đến 1000, mặc "
                       "định 300."),
                      ("taiVe", "query", "boolean", "Không bắt buộc, mặc định `false`. `true`: thêm header để trình "
                       "duyệt lưu ảnh thành tệp.")]),
             kit.nhan("RESPONSE THÀNH CÔNG — 200 OK"),
             dan("Body là dữ liệu ảnh. Header:"),
             kit.khoi_code('Content-Type: image/png\n'
                           'Content-Disposition: attachment; filename="phieu-kham-qr.png"    (chỉ khi taiVe=true)'),
             loi([(400, "DU_LIEU_KHONG_HOP_LE", "`kichThuoc` ngoài 100..1000 hoặc tham số sai kiểu."),
                  LOI_PHIEU]),
             kit.luu_y([
                 "Lỗi vẫn trả JSON theo khung phản hồi chung (3.1); thẻ `<img>` chỉ thấy ảnh không tải được, nên gọi "
                 "14.1 trước rồi mới hiện QR.",
                 "Nút \"Tải mã QR\": dùng thẻ `<a href=\".../qr?taiVe=true\">`.",
                 "Mã QR có ở mọi trạng thái của lịch hẹn, kể cả lịch đã hủy.",
             ])])
        + kit.route(
            "14.3", "Hủy lịch bằng link phiếu khám", "POST", "/api/booking/phieu-kham/{maPhieuKham}/huy",
            "ep_phieu_kham_huy", next(so), CONG_KHAI,
            "Hủy lịch hẹn khi không đăng nhập: phải gửi đúng số điện thoại liên hệ đã nhập lúc đặt. Quy tắc và hạn "
            "hủy ở 3.11.",
            [tham_so([MA_PHIEU]),
             body([("soDienThoai", "string", "Có",
                    "Số điện thoại liên hệ của lịch hẹn (số của người giám hộ nếu lịch có người giám hộ)."),
                   ("lyDo", "string", "Không", "Lý do hủy, tối đa 500 ký tự.")]),
             js({"soDienThoai": "0912345601", "lyDo": "Bận việc đột xuất"}),
             tra("200 OK", f"`duLieu`: {mh('PhieuKhamResponse')} sau khi hủy (`trangThai = DA_HUY`).",
                 ok(PHIEU_DA_HUY, "Đã hủy lịch hẹn")),
             loi([LOI_SDT[0], (400, "DU_LIEU_KHONG_HOP_LE", "`lyDo` dài hơn 500 ký tự (có `chiTiet`)."), LOI_SDT[1],
                  LOI_PHIEU] + LOI_HUY_DOI + LOI_CHON_KHUNG[4:]),
             kit.luu_y([
                 "Chỗ của lịch đã hủy được trả lại cho khung giờ ngay; không hoàn tác được, muốn khám thì đặt lịch "
                 "mới.",
                 "Mọi lần gọi qua được bước kiểm tra body đều tính vào giới hạn 20 lần / 10 phút / IP, kể cả lần sai "
                 "số điện thoại.",
             ])])
        + kit.route(
            "14.4", "Đổi lịch bằng link phiếu khám", "POST", "/api/booking/phieu-kham/{maPhieuKham}/doi-lich",
            "ep_phieu_kham_doi_lich", next(so), CONG_KHAI,
            "Đổi lịch hẹn sang khung giờ khác (cùng hoặc khác bác sĩ, chuyên khoa) khi không đăng nhập. Lịch cũ thành "
            "`DA_HUY_DO_DOI_LICH`, response là lịch mới với **mã phiếu khám mới** (3.11). Thông tin người khám, người "
            "giám hộ, lý do khám được giữ nguyên từ lịch cũ.",
            [tham_so([MA_PHIEU]),
             body(CHON_KHUNG + [("soDienThoai", "string", "Có", "Số điện thoại liên hệ của lịch hẹn, như 14.3.")]),
             js({"idLichLamViec": 15, "gioBatDauKhung": "2026-10-10T09:00:00", "soDienThoai": "0912345601"}),
             tra("201 Created", f"`duLieu`: {mh('DatLichResponse')} của lịch mới.", ok(DA_DOI, "Đổi lịch thành công")),
             loi([(400, "DU_LIEU_KHONG_HOP_LE",
                   "Thiếu `gioBatDauKhung` (có `chiTiet`). Không có `chiTiet`: `soDienThoai` thiếu / sai dạng; gửi cả "
                   "hai hoặc không gửi cái nào trong `idLichLamViec`, `idChuyenKhoa`; `gioBatDauKhung` không phải "
                   "khung của ca; chọn lại đúng khung giờ đang giữ."),
                  LOI_SDT[1],
                  (404, "KHONG_TIM_THAY", "Mã phiếu khám sai dạng hoặc không tồn tại; `idLichLamViec` không tồn tại.")]
                 + LOI_HUY_DOI
                 + [(409, "VUOT_SO_LAN_DOI_LICH", "Lịch này đã được đổi 2 lần; vẫn hủy được.")]
                 + LOI_CHON_KHUNG),
             kit.luu_y([
                 "Đổi không thành công (mọi mã lỗi ở trên) thì lịch cũ giữ nguyên, không mất chỗ.",
                 "Sau khi đổi, chuyển sang trang phiếu khám của `maPhieuKham` mới. Khách cần lưu link mới; link cũ "
                 "chỉ còn hiện trạng thái đã đổi.",
                 "Gọi tiếp route này trên mã cũ sẽ nhận 409 `LICH_HEN_KHONG_HUY_DOI_DUOC`.",
             ])]))


ROUTE = [
    ("11.1", "GET", "/api/catalog/chuyen-khoa", "ep_chuyen_khoa_ds", "Công khai", "Danh sách chuyên khoa"),
    ("11.2", "GET", "/api/catalog/chuyen-khoa/{id}", "ep_chuyen_khoa_chi_tiet", "Công khai", "Chi tiết chuyên khoa"),
    ("11.3", "GET", "/api/catalog/bac-si", "ep_bac_si_ds", "Công khai", "Tìm bác sĩ theo chuyên khoa, tên"),
    ("11.4", "GET", "/api/catalog/bac-si/{id}", "ep_bac_si_chi_tiet", "Công khai", "Chi tiết bác sĩ"),
    ("12.1", "GET", "/api/booking/khung-gio", "ep_khung_gio", "Công khai", "Khung giờ theo ngày"),
    ("12.2", "GET", "/api/booking/khung-gio/ngay-con-cho", "ep_ngay_con_cho", "Công khai", "Các ngày có ca khám"),
    ("12.3", "GET", "/api/booking/khung-gio/nhieu-ngay", "ep_khung_gio_nhieu_ngay", "Công khai",
     "Khung giờ nhiều ngày của một bác sĩ"),
    ("12.4", "GET", "/api/booking/khung-gio/gop", "ep_khung_gio_gop", "Công khai", "Khung giờ gộp của chuyên khoa"),
    ("12.5", "GET", "/api/booking/khung-gio/ngay-som-nhat", "ep_ngay_som_nhat", "Công khai",
     "Ngày còn chỗ sớm nhất của từng bác sĩ"),
    ("13.1", "POST", "/api/booking/lich-hen", "ep_dat_lich", "Công khai", "Đặt lịch khám (khách hoặc bệnh nhân)"),
    ("13.2", "GET", "/api/booking/thong-tin-dat-lich/cua-toi", "ep_thong_tin_dat_lich", "Bệnh nhân",
     "Thông tin điền sẵn form đặt lịch"),
    ("13.3", "PUT", "/api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id}", "ep_sua_nguoi_than", "Bệnh nhân",
     "Sửa người thân đã lưu"),
    ("13.4", "DELETE", "/api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id}", "ep_xoa_nguoi_than", "Bệnh nhân",
     "Bỏ người thân đã lưu"),
    ("14.1", "GET", "/api/booking/phieu-kham/{maPhieuKham}", "ep_phieu_kham", "Công khai", "Xem phiếu khám"),
    ("14.2", "GET", "/api/booking/phieu-kham/{maPhieuKham}/qr", "ep_phieu_kham_qr", "Công khai",
     "Ảnh mã QR của phiếu khám"),
    ("14.3", "POST", "/api/booking/phieu-kham/{maPhieuKham}/huy", "ep_phieu_kham_huy", "Công khai",
     "Hủy lịch bằng link phiếu khám"),
    ("14.4", "POST", "/api/booking/phieu-kham/{maPhieuKham}/doi-lich", "ep_phieu_kham_doi_lich", "Công khai",
     "Đổi lịch bằng link phiếu khám"),
]


def sua(tl):
    # 3.5: thời điểm do hệ thống ghi có phần lẻ của giây (thấy ở response thật)
    tl.thay_noi_dung("ISO-8601, múi giờ Asia/Ho_Chi_Minh, không kèm offset: ",
                     "ISO-8601, múi giờ Asia/Ho_Chi_Minh, không kèm offset: `2026-10-03T08:40:00`. Thời điểm do hệ "
                     "thống ghi (`ngayTao`, `ngayDat`, `lanDungCuoi`...) có thêm phần lẻ của giây, dài tới 6 chữ số "
                     "và bỏ các số 0 ở cuối: `2026-10-09T17:02:27.576264`. Ngày: `2001-05-20`.", trong_bang=True)
    for dong in ROUTE:
        tl.them_dong("2. Danh sách route", None, kit.dong_route(*dong))
    # Id bookmark tạm; build.py đánh số lại toàn bộ ở cuối chuỗi
    so = iter(range(tl.id_bookmark_moi(), 10 ** 6))
    tl.chen_truoc_doan(PHU_LUC_A, muc_11(tl, so) + muc_12(tl, so) + muc_13(tl, so) + muc_14(tl, so))
