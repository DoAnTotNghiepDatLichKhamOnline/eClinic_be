"""Tài liệu 2, giai đoạn 3: mục 5 (lịch làm việc, lịch hẹn, bác sĩ hủy lịch) và mục 6 (hàng đợi khám), 8 route phía bác
sĩ của booking-service, cùng 8 dòng của bảng route ở mục 2. Chạy qua bsqt_build.py.

JSON mẫu lấy từ response thật do bsqt_lay_mau.py ghi lại (temp/api-doc/mau-bsqt), danh sách cắt còn 1-2 phần tử; id, tên
giữ như response thật, thời điểm đặt lịch chuẩn hoá về trước ngày khám. Quy tắc nghiệp vụ đã có ở mục 3 thì chỉ dẫn mục.
"""

import bsqt_p2_khung
import bsqt_p2_mo_hinh
import kit
from p3_dat_lich import dan, js, loi, ok, tham_so, tra

SO_MO_HINH = {m[0]: f"4.{i + 1}" for i, m in enumerate(bsqt_p2_mo_hinh.MO_HINH)}
BS = "/api/booking/bac-si/toi"
QUYEN = "Chỉ BAC_SI (cần access token)"
SERVICE = "booking-service"
NGAY = "2026-10-16"


def mh(ten):
    return f"`{ten}` ({SO_MO_HINH[ten]})"


# ----- Dữ liệu mẫu (từ response thật) -----

BAC_SI = {"id": 1, "hoTen": "Nguyễn Văn An", "hocVi": "TS.BS",
          "anhDaiDien": "https://placehold.co/400x400/0f766e/ffffff/png?text=BS+1"}
PHONG = {"id": 1, "tenPhong": "Phòng 101", "tang": "Tầng 1"}


def ca_lam_viec(id_ca, ngay, gio_bat_dau, gio_ket_thuc, da_dat, trang_thai="HOAT_DONG"):
    return {"idLichLamViec": id_ca, "ngay": ngay, "gioBatDau": gio_bat_dau, "gioKetThuc": gio_ket_thuc,
            "soLuotToiDaMoiGio": 6, "thoiLuongLuotPhut": 10, "trangThai": trang_thai, "bacSi": BAC_SI,
            "tenChuyenKhoa": "Nội tổng quát", "phongKham": PHONG, "tongSoLuot": 21, "soLuotDaDat": da_dat,
            "soLuotConTrong": 21 - da_dat}


def lich_hen(id_lh, ma, so_thu_tu, gio, benh_nhan, id_ho_so, ly_do, ngay=NGAY, id_ca=13, trang_thai="DA_XAC_NHAN",
             ly_do_huy=None, ngay_dat="2026-10-14T20:15:42.318204"):
    """LichHenTrongCaResponse như bác sĩ nhận: bacSi = null, người đặt tự khám nên doiChieu khớp hồ sơ."""
    return {"id": id_lh, "maTraCuu": f"ECL-{ngay.replace('-', '')}-{ma}", "idLichLamViec": id_ca,
            "trangThai": trang_thai, "soThuTu": so_thu_tu, "gioKhamDuKien": f"{ngay}T{gio}",
            "gioBatDauKhung": f"{ngay}T{gio[:2]}:00:00", "gioKetThucKhung": f"{ngay}T{int(gio[:2]) + 1:02d}:00:00",
            "bacSi": None, "phongKham": PHONG, "lyDoKham": ly_do, "ngayDat": ngay_dat, "idHoSoBenhNhan": id_ho_so,
            "benhNhan": benh_nhan, "nguoiGiamHo": None,
            "doiChieu": {"canDoiChieu": False, "hoTenDaNhap": benh_nhan["hoTen"],
                         "ngaySinhDaNhap": benh_nhan["ngaySinh"], "gioiTinhDaNhap": benh_nhan["gioiTinh"],
                         "hoTenGiamHoDaNhap": None},
            "lyDoHuy": ly_do_huy, "canDoiLich": False}


MAI = {"hoTen": "Nguyễn Thị Mai", "ngaySinh": "1995-03-12", "tuoi": 31, "gioiTinh": "NU", "soDienThoai": "0902000001"}
NAM = {"hoTen": "Trần Văn Nam", "ngaySinh": "1988-11-05", "tuoi": 37, "gioiTinh": "NAM", "soDienThoai": "0902000002"}
LH_1 = lich_hen(1, "6252", 1, "08:00:00", MAI, 1, "Ho, sốt nhẹ 2 ngày nay")
LH_2 = lich_hen(2, "1847", 2, "08:10:00", NAM, 2, "Đau đầu, chóng mặt")
LY_DO_HUY = "Bác sĩ có lịch hội chẩn đột xuất, mời bạn đặt khung giờ khác"
LH_HUY = lich_hen(5, "9558", 1, "08:00:00", MAI, 1, "Khám định kỳ", ngay="2026-10-19", id_ca=21,
                  trang_thai="BI_TU_CHOI", ly_do_huy=LY_DO_HUY)


def luot(trang_thai, goi_luc, lich):
    return {"trangThaiHangDoi": trang_thai, "goiLuc": goi_luc, "lichHen": lich}


def hang_doi(so_luong, id_dang_kham, id_tiep_theo, cac_luot, ngay=NGAY, id_ca_hien_tai=13,
             tinh_luc="2026-10-16T08:12:57.856327644"):
    ca = {"idLichLamViec": 13, "buoi": "SANG", "gioBatDau": "08:00:00", "gioKetThuc": "11:30:00", "phongKham": PHONG,
          "soLuong": dict(zip(("tong", "choKham", "dangKham", "vangMat", "daKham"), so_luong)),
          "idDangKham": id_dang_kham, "idTiepTheo": id_tiep_theo, "cacLuot": cac_luot}
    return {"ngay": ngay, "idCaHienTai": id_ca_hien_tai, "tinhLuc": tinh_luc, "cacCa": [ca] if cac_luot else []}


HANG_DOI_DAU = hang_doi((4, 4, 0, 0, 0), None, 1, [luot("CHO_KHAM", None, LH_1), luot("CHO_KHAM", None, LH_2)])
HANG_DOI_GOI = hang_doi((4, 3, 1, 0, 0), 1, 2, [luot("DANG_KHAM", "2026-10-16T08:03:41.205118", LH_1)],
                        tinh_luc="2026-10-16T08:03:41.21934")
HANG_DOI_CUOI = hang_doi(
    (4, 0, 1, 2, 1), 4, None,
    [luot("VANG_MAT", "2026-10-16T08:40:12.666192", LH_1),
     luot("DA_KHAM", "2026-10-16T08:22:05.223466", {**LH_2, "trangThai": "DA_HOAN_THANH"})],
    tinh_luc="2026-10-16T09:05:18.860107037")

# ----- Bảng route ở mục 2 -----

ROUTE = [
    ("5.1", "GET", f"{BS}/lich-lam-viec", "ep_bs_lich_lam_viec", "Ca làm việc của tôi theo khoảng ngày"),
    ("5.2", "GET", f"{BS}/lich-hen", "ep_bs_lich_hen", "Lịch hẹn của tôi trong 1 ngày, có lọc"),
    ("5.3", "GET", f"{BS}/lich-hen/lich", "ep_bs_lich_hen_lich", "Lịch hẹn của tôi theo khoảng ngày"),
    ("5.4", "POST", f"{BS}/lich-hen/{{id}}/huy", "ep_bs_huy", "Bác sĩ hủy lịch hẹn, kèm lý do"),
    ("6.1", "GET", f"{BS}/hang-doi", "ep_bs_hang_doi", "Hàng đợi khám của 1 ngày"),
    ("6.2", "POST", f"{BS}/hang-doi/goi-tiep", "ep_bs_goi_tiep", "Gọi bệnh nhân tiếp theo của ca"),
    ("6.3", "POST", f"{BS}/lich-hen/{{id}}/goi-kham", "ep_bs_goi_kham", "Gọi 1 bệnh nhân vào khám"),
    ("6.4", "POST", f"{BS}/lich-hen/{{id}}/vang-mat", "ep_bs_vang_mat", "Đánh dấu bệnh nhân vắng mặt"),
]
R = {muc: (method, duong, bm) for muc, method, duong, bm, _ in ROUTE}


def route(so, muc, ten, mo_ta, than):
    method, duong, bm = R[muc]
    return kit.route(muc, ten, method, duong, bm, next(so), QUYEN, mo_ta, than)


ID_LICH_HEN = ("id", "path", "number", "Id lịch hẹn (`id` của 4.2).")
KHOANG = [("tuNgay", "query", "date", "Bắt buộc. Dạng `2026-10-16`."),
          ("denNgay", "query", "date", "Bắt buộc, từ `tuNgay` trở đi, khoảng tối đa 42 ngày tính cả hai đầu (3.4).")]
LOI_KHOANG = (400, "DU_LIEU_KHONG_HOP_LE", "Thiếu `tuNgay` hoặc `denNgay`, ngày sai dạng, `denNgay` trước `tuNgay`, "
                                          "hoặc khoảng dài hơn 42 ngày (3.4).")
LOI_KHONG_CO = (404, "KHONG_TIM_THAY", "Lịch hẹn không có, hoặc là lịch hẹn của bác sĩ khác (3.3).")
LOI_NGAY_KHAM = (409, "KHONG_PHAI_NGAY_KHAM", "Lịch hẹn không khám hôm nay (3.5).")
LOI_KHAM_DUOC = (409, "LICH_HEN_KHONG_KHAM_DUOC", "Lịch hẹn không còn `DA_XAC_NHAN` (đã khám, đã hủy, không đến) hoặc "
                                                 "đang chờ đổi lịch (3.5).")
LOI_DANG_KHAM = (409, "DANG_KHAM_BENH_NHAN_KHAC", "Đang có bệnh nhân khác `DANG_KHAM` trong ngày; thông điệp nêu họ tên "
                                                 "và số thứ tự người đó (3.5).")
TRA_HANG_DOI = f"`duLieu`: {mh('HangDoiResponse')} của hôm nay, sau thao tác."
# 6.3, 6.4 trả cùng mô hình như 6.2: chỉ dẫn mẫu của 6.2, không in lại cả hàng đợi
TRA_NHU_6_2 = (kit.nhan("RESPONSE THÀNH CÔNG — 200 OK")
               + kit.doan(f"`duLieu`: {mh('HangDoiResponse')} của hôm nay, sau thao tác; mẫu ở 6.2."))


def muc_5(so):
    x = kit.tieu_de(1, "5. Lịch làm việc và lịch hẹn của bác sĩ")
    x += kit.doan(f"Các route của `booking-service` cho lịch của chính bác sĩ đang đăng nhập (3.3); service cần chạy: "
                  f"{SERVICE} (3.2). Màn hình lịch thường gọi 5.1 (các ca) và 5.3 (lịch hẹn) cho cùng khoảng ngày, rồi "
                  "5.2 khi bác sĩ chọn 1 ngày hoặc 1 ca. Bác sĩ làm được gì ở từng trạng thái lịch hẹn: 3.4.")
    x += route(so, "5.1", "Ca làm việc của tôi", "Các ca làm việc của bác sĩ trong khoảng ngày, kèm số lượt đã đặt, "
               "theo ngày rồi giờ bắt đầu.", [
                   tham_so(KHOANG),
                   tra("200 OK", f"`duLieu`: danh sách {mh('CaLamViecResponse')}.",
                       ok([ca_lam_viec(13, NGAY, "08:00:00", "11:30:00", 4),
                           ca_lam_viec(21, "2026-10-19", "08:00:00", "11:30:00", 1)])),
                   loi([LOI_KHOANG]),
                   kit.luu_y(["Có cả ca đã hủy (`trangThai` = `DA_HUY`, 3.9), vẫn kèm số lượt: lịch hẹn của ca đó chờ "
                              "bệnh nhân đổi lịch (3.4).",
                              "Ca không có lịch hẹn nào vẫn có trong kết quả, với `soLuotDaDat` = 0."])])
    x += route(so, "5.2", "Lịch hẹn của tôi trong 1 ngày", "Lịch hẹn của bác sĩ trong 1 ngày, mọi trạng thái, theo "
               "giờ khám dự kiến. Các bộ lọc dùng cùng lúc được (và).", [
                   tham_so([("ngay", "query", "date", "Không bắt buộc, mặc định hôm nay."),
                            ("idLichLamViec", "query", "number", "Chỉ lịch hẹn của ca này (`idLichLamViec` của 4.1)."),
                            ("soThuTu", "query", "number", "Chỉ lịch hẹn có số thứ tự này."),
                            ("tuKhoa", "query", "string", "Họ tên bệnh nhân chứa từ khoá, không phân biệt dấu và hoa "
                                                          "thường: `mai` khớp \"Nguyễn Thị Mai\".")]),
                   tra("200 OK", f"`duLieu`: danh sách {mh('LichHenTrongCaResponse')}, `bacSi` = `null`.",
                       ok([LH_1, LH_2])),
                   loi([(400, "DU_LIEU_KHONG_HOP_LE", "`ngay`, `idLichLamViec` hoặc `soThuTu` sai kiểu.")]),
                   kit.luu_y(["`tuKhoa` so với họ tên trong hồ sơ bệnh nhân và cả họ tên người đặt đã nhập lúc đặt "
                              "lịch (`doiChieu.hoTenDaNhap`).",
                              "Không có lịch hẹn nào khớp: `duLieu` = `[]`, không phải lỗi. `idLichLamViec` của ca khác "
                              "bác sĩ cũng cho `[]`."])])
    x += route(so, "5.3", "Lịch hẹn của tôi theo khoảng ngày", "Như 5.2 nhưng cho cả khoảng ngày, không có bộ lọc; dùng "
               "cho màn hình lịch tuần / tháng.", [
                   tham_so(KHOANG),
                   tra("200 OK", f"`duLieu`: danh sách {mh('LichHenTrongCaResponse')}, `bacSi` = `null`, theo giờ khám "
                                 "dự kiến.", ok([LH_1, {**LH_HUY, "trangThai": "DA_XAC_NHAN", "lyDoHuy": None}])),
                   loi([LOI_KHOANG])])
    x += route(so, "5.4", "Bác sĩ hủy lịch hẹn", "Hủy 1 lịch hẹn `DA_XAC_NHAN` của bác sĩ trước giờ khám, kèm lý do. "
               "Lịch thành `BI_TU_CHOI`, lượt khám được mở lại cho người khác đặt, bệnh nhân được báo. Quy tắc: 3.4.", [
                   tham_so([ID_LICH_HEN]),
                   kit.nhan("REQUEST BODY (JSON)"),
                   kit.bang(kit.RONG_TRUONG, ("Trường", "Kiểu", "Bắt buộc", "Ràng buộc / mô tả"),
                            [("`lyDo`", "string", "Có", "Không để trống, tối đa 500 ký tự. Bệnh nhân đọc được trên "
                                                       "phiếu khám (`lyDoHuy`).")]) + kit.khoang_trong(60),
                   dan("Ví dụ:"), js({"lyDo": LY_DO_HUY}),
                   tra("200 OK", f"`duLieu`: {mh('LichHenTrongCaResponse')} sau khi hủy: `trangThai` = `BI_TU_CHOI`, "
                                 "`lyDoHuy` là lý do vừa gửi.",
                       ok(LH_HUY, "Đã hủy lịch hẹn, bệnh nhân đã được báo")),
                   loi([(400, "DU_LIEU_KHONG_HOP_LE", "`lyDo` trống hoặc dài hơn 500 ký tự (`chiTiet` nêu trường "
                                                      "`lyDo`)."),
                        LOI_KHONG_CO,
                        (409, "LICH_HEN_KHONG_HUY_DOI_DUOC", "Lịch hẹn không còn `DA_XAC_NHAN`: đã khám, đã hủy, bác sĩ "
                                                             "đã hủy (bấm lần 2), không đến."),
                        (409, "LICH_HEN_CAN_DOI_LICH", "Lịch hẹn đang chờ bệnh nhân đổi lịch (`canDoiLich` = `true`)."),
                        (409, "LICH_HEN_DA_QUA_GIO", "Đã tới hoặc qua giờ bắt đầu khung giờ khám.")]),
                   kit.luu_y(["Thông báo và email cho bệnh nhân đi qua notification-service; service đó chưa chạy thì "
                              "thông báo được giữ lại và gửi khi service chạy, việc hủy vẫn thành công (3.2).",
                              "Lịch đã hủy vẫn có trong 5.2, 5.3 với `trangThai` = `BI_TU_CHOI`."])])
    return x


def muc_6(so):
    x = kit.tieu_de(1, "6. Hàng đợi khám")
    x += kit.doan(f"Hàng đợi khám trong ngày của bác sĩ, theo ca; service cần chạy: {SERVICE} (3.2). Trạng thái, điều "
                  "kiện gọi và việc bấm lại: 3.5. Màn hình hàng đợi gọi 6.1 khi mở và định kỳ; bác sĩ bấm 6.2 (gọi người "
                  "tiếp theo) hoặc 6.3 (gọi đúng 1 người), khám, rồi ghi hồ sơ bệnh án (mục 8) để lượt thành "
                  "`DA_KHAM`, hoặc 6.4 nếu bệnh nhân không có mặt.")
    x += kit.doan("Các route thao tác (6.2-6.4) trả lại toàn bộ hàng đợi của hôm nay sau thao tác: vẽ lại màn hình từ "
                  "response.")
    x += route(so, "6.1", "Hàng đợi khám", "Hàng đợi 1 ngày của bác sĩ, nhóm theo ca đang hoạt động, mỗi ca kèm số "
               "lượng theo trạng thái, người đang khám và người được gọi tiếp.", [
                   tham_so([("ngay", "query", "date", "Không bắt buộc, mặc định hôm nay. Ngày khác chỉ để xem.")]),
                   tra("200 OK", f"`duLieu`: {mh('HangDoiResponse')}. Ví dụ: đầu ca, chưa gọi ai.", ok(HANG_DOI_DAU)),
                   dan("Cuối buổi: 1 người vắng mặt, 1 người đã khám (2 lượt sau lược bớt):"), js(ok(HANG_DOI_CUOI)),
                   loi([(400, "DU_LIEU_KHONG_HOP_LE", "`ngay` sai dạng.")]),
                   kit.luu_y(["Ngày không có ca: `cacCa` = `[]`, `idCaHienTai` = `null`. Ngày khác hôm nay: "
                              "`idTiepTheo` luôn `null`.",
                              "`tinhLuc` có tới 9 chữ số lẻ của giây (nano giây), khác các thời điểm khác (tối đa 6, "
                              "TK 3.5); chỉ dùng để hiện \"cập nhật lúc\".",
                              "Ca đã hủy không có trong hàng đợi; lịch hẹn đang chờ đổi lịch cũng không."])])
    x += route(so, "6.2", "Gọi bệnh nhân tiếp theo", "Gọi lượt `CHO_KHAM` có số thứ tự nhỏ nhất của 1 ca hôm nay: "
               "lượt đó thành `DANG_KHAM`, `goiLuc` là lúc gọi.", [
                   tham_so([("idLichLamViec", "query", "number", "Bắt buộc. Id ca (`idLichLamViec` của `CaHangDoi`, "
                                                                 "4.3).")]),
                   tra("200 OK", TRA_HANG_DOI + " Ví dụ: vừa gọi số 1 (các lượt sau lược bớt).", ok(HANG_DOI_GOI)),
                   loi([(400, "DU_LIEU_KHONG_HOP_LE", "Thiếu `idLichLamViec` hoặc sai kiểu."),
                        (404, "KHONG_TIM_THAY", "Ca không có, đã hủy, hoặc là ca của bác sĩ khác."),
                        (409, "KHONG_PHAI_NGAY_KHAM", "Ca không phải ca của hôm nay."),
                        (409, "HANG_DOI_TRONG", "Ca không còn lượt `CHO_KHAM` (người vắng mặt không được gọi lại tự "
                                                "động: dùng 6.3)."),
                        LOI_DANG_KHAM])])
    x += route(so, "6.3", "Gọi 1 bệnh nhân vào khám", "Gọi đúng 1 lịch hẹn hôm nay vào khám: người đang chờ, hoặc gọi "
               "lại người đã đánh dấu vắng mặt.", [
                   tham_so([ID_LICH_HEN]),
                   TRA_NHU_6_2,
                   loi([LOI_KHONG_CO, LOI_NGAY_KHAM, LOI_KHAM_DUOC, LOI_DANG_KHAM]),
                   kit.luu_y(["Gọi lại người đang `DANG_KHAM` vẫn 200 và không đổi `goiLuc`."])])
    x += route(so, "6.4", "Đánh dấu vắng mặt", "Đánh dấu 1 lịch hẹn hôm nay là vắng mặt (`VANG_MAT`); người đang khám "
               "cũng đánh dấu được, khi đó bác sĩ gọi được người khác. Gọi lại người vắng mặt bằng 6.3.", [
                   tham_so([ID_LICH_HEN]),
                   TRA_NHU_6_2,
                   loi([LOI_KHONG_CO, LOI_NGAY_KHAM, LOI_KHAM_DUOC]),
                   kit.luu_y(["Đánh dấu lại người đã `VANG_MAT` vẫn 200, không đổi gì.",
                              "`VANG_MAT` chỉ là trạng thái hàng đợi: lịch hẹn vẫn `DA_XAC_NHAN` trong ngày."])])
    return x


def sua(tl):
    for muc, method, duong, bm, chuc_nang in ROUTE:
        tl.them_dong(bsqt_p2_khung.MUC_2, None,
                     bsqt_p2_khung.dong_route(muc, method, duong, bm, "BAC_SI", chuc_nang, SERVICE))
    # Id bookmark tạm; bsqt_build.py đánh số lại toàn bộ ở cuối chuỗi. Đây là các bookmark đầu tiên của tài liệu nên
    # kit.TaiLieu.id_bookmark_moi (cần ít nhất 1 bookmark sẵn có) không dùng được
    so = iter(range(1, 10 ** 6))
    bsqt_p2_khung.them_cuoi(tl, muc_5(so) + muc_6(so))
