"""Bản 2.3 của tài liệu API: phần phía bệnh nhân của DOANTOTNGH-11 (xác nhận ngay, bác sĩ hủy lịch, không đến, kết quả
khám đầy đủ).

- Lịch vừa đặt / vừa đổi là `DA_XAC_NHAN` ngay; không còn `CHO_XAC_NHAN` của lịch hẹn (3.7, 3.11, 4.21, mục 13-15).
- `BI_TU_CHOI` nay là bác sĩ hủy 1 lịch đã xác nhận, kèm lý do; có email và thông báo riêng (3.13, mục 18).
- Trạng thái mới `KHONG_DEN`: lịch chưa khám của ngày đã qua (3.11).
- Kết quả khám có lý do khám, sinh hiệu, khám lâm sàng, mã ICD-10, bệnh kèm theo; dòng đơn thuốc có `soLuong` (4.27).

`CHO_XAC_NHAN` của trạng thái tài khoản (mục 5-10, `TrangThaiTaiKhoan`) là enum khác, giữ nguyên. Không thêm route: hủy
lịch của bác sĩ, tìm mã ICD-10 và ghi bệnh án là route của bác sĩ, có ở docs/BAN-GIAO-FRONTEND-05-10.md. Giai đoạn 9 của
chuỗi dựng tài liệu; chạy qua build.py.
"""

import html
import re

import kit
import p2_quy_uoc
from p6_ngon_ngu import RONG_EMAIL, RONG_PHIEN_BAN
from p8_benh_an_ho_so_bac_si import _pham_vi_muc, sua_json_mau, them_truong

PHIEN_BAN = "2.3"
NGAY = "15/10/2026"
MUC_1 = "1. Giới thiệu và phạm vi"
MUC_2 = "2. Danh sách route"
MUC_311 = "3.11 Vòng đời lịch hẹn, hủy và đổi lịch"
MUC_313 = "3.13 Email và lời nhắc lịch hẹn"
MUC_427 = "4.27 KetQuaKhamResponse"

KHONG_HUY_DOI = ("409 `LICH_HEN_KHONG_HUY_DOI_DUOC` cả khi lịch đã là `KHONG_DEN` (3.11). Request và response không "
                 "đổi.")
LICH_MOI_XAC_NHAN = ("Lịch mới là `DA_XAC_NHAN` ngay, không chờ bác sĩ xác nhận lại (trước đây `CHO_XAC_NHAN`). "
                     + KHONG_HUY_DOI)
TRANG_THAI_DOI = ("`trangThai` không còn `CHO_XAC_NHAN`, có thêm `KHONG_DEN`; `SAP_TOI` chỉ gồm lịch `DA_XAC_NHAN` "
                  "chưa tới giờ; `BI_TU_CHOI` nay là bác sĩ hủy lịch, lý do ở `lyDoHuy` (3.11).")

# Mục có response hoặc ý nghĩa đổi ở bản này: (mục, tiêu đề mục chi tiết, điều đã đổi). Quyết định cột "Thay đổi".
# 18.3 và 19.2 chỉ đổi câu chữ trong JSON mẫu nên không đánh dấu (19.2 giữ "Mới · v2.1").
CAP_NHAT = (
    ("13.1", "Đặt lịch khám",
     "lịch vừa đặt là `DA_XAC_NHAN` ngay (trước đây `CHO_XAC_NHAN`, chờ bác sĩ xác nhận); email gửi đi là \"đặt lịch "
     "khám thành công\" (3.13). Request không đổi."),
    ("14.1", "Xem phiếu khám",
     "`trangThai` không còn `CHO_XAC_NHAN`, có thêm `KHONG_DEN`; `BI_TU_CHOI` nay là bác sĩ hủy lịch đã xác nhận, lý do "
     "ở `lyDoHuy` (3.11). Cấu trúc response không đổi."),
    ("14.3", "Hủy lịch bằng link phiếu khám", KHONG_HUY_DOI),
    ("14.4", "Đổi lịch bằng link phiếu khám", LICH_MOI_XAC_NHAN),
    ("15.1", "Danh sách lịch hẹn của tôi", TRANG_THAI_DOI + " Cấu trúc response không đổi."),
    ("15.2", "Lịch hẹn của tôi theo khoảng ngày", "`trangThai` đổi như ở 15.1. Cấu trúc response không đổi."),
    ("15.3", "Chi tiết lịch hẹn của tôi",
     "`ketQua` (`KetQuaKhamResponse`, 4.27) có thêm `lyDoKham`, `sinhHieu`, `khamLamSang`, `icdChinh`, "
     "`chanDoanKemTheo`; mỗi dòng `donThuoc` có thêm `soLuong`. `trangThai` đổi như ở 15.1."),
    ("15.4", "Hủy lịch hẹn của tôi", KHONG_HUY_DOI),
    ("15.5", "Đổi lịch hẹn của tôi", LICH_MOI_XAC_NHAN),
    ("16.1", "Lịch sử khám của tôi", "`ketQua` của mỗi lượt khám đổi như ở 15.3 (4.27)."),
    ("17.3", "Trang cá nhân của bệnh nhân",
     "`lanKhamGanDay[].ketQua` đổi như ở 15.3 (4.27); `trangThai` của các lịch đổi như ở 15.1."),
    ("18.1", "Danh sách thông báo của tôi",
     "bác sĩ không còn xác nhận / từ chối lịch: `LICH_HEN_DA_XAC_NHAN` không còn được tạo (chỉ còn ở thông báo cũ); "
     "`LICH_HEN_BI_TU_CHOI` nay báo bác sĩ hủy 1 lịch đã xác nhận, kèm lý do. Cấu trúc response không đổi."),
)

LICH_SU = (f"{PHIEN_BAN} — {NGAY}",
           "Lịch hẹn được xác nhận ngay khi đặt / đổi (bỏ `CHO_XAC_NHAN` của lịch hẹn); bác sĩ hủy lịch có lý do "
           "(`BI_TU_CHOI`, email và thông báo mới); trạng thái `KHONG_DEN` (3.11); kết quả khám có lý do khám, sinh "
           "hiệu, khám lâm sàng, mã ICD-10, bệnh kèm theo và số lượng thuốc (4.27).")

# --- Mô hình 4.27: (tên trường, kiểu, mô tả), chèn đúng thứ tự record Java ---
KQ_DAU = [
    ("lyDoKham", "string", "Lý do khám, triệu chứng do bác sĩ ghi lúc khám; `null` nếu không ghi. Có từ bản 2.3."),
    ("sinhHieu", "SinhHieu", "Sinh hiệu đo lúc khám; `null` nếu bác sĩ không đo chỉ số nào. Có từ bản 2.3."),
    ("khamLamSang", "string", "Kết quả khám lâm sàng; `null` nếu không ghi. Có từ bản 2.3."),
]
KQ_SAU_CHAN_DOAN = [
    ("icdChinh", "IcdChinh", "Mã ICD-10 của chẩn đoán chính; `null` nếu bác sĩ không chọn mã. `chanDoan` vẫn là chữ "
                             "bác sĩ gõ, hiện cả hai. Có từ bản 2.3."),
    ("chanDoanKemTheo", "string", "Bệnh kèm theo, chữ bác sĩ gõ (không có mã); `null` nếu không có. Có từ bản 2.3."),
]
SINH_HIEU = [
    ("mach", "number", "Mạch, lần/phút."),
    ("nhietDo", "number", "Nhiệt độ, °C, tối đa 1 chữ số thập phân, vd `37.5`."),
    ("huyetApTamThu", "number", "Huyết áp tâm thu, mmHg. Hai số huyết áp cùng có hoặc cùng `null`."),
    ("huyetApTamTruong", "number", "Huyết áp tâm trương, mmHg; luôn nhỏ hơn tâm thu."),
    ("nhipTho", "number", "Nhịp thở, lần/phút."),
    ("canNang", "number", "Cân nặng, kg, 2 chữ số thập phân, vd `54.50`."),
    ("chieuCao", "number", "Chiều cao, cm, 1 chữ số thập phân, vd `158.0`."),
]
ICD_CHINH = [
    ("ma", "string", "Mã ICD-10, vd `J02.9`."),
    ("ten", "string", "Tên bệnh theo `Accept-Language` (3.12): tiếng Việt hoặc tiếng Anh."),
    ("manTinh", "boolean", "`true`: bệnh thuộc danh mục bệnh mạn tính được kê đơn ngoại trú tới 90 ngày (Thông tư "
                           "26/2025/TT-BYT); chỉ để hiển thị."),
]
SO_LUONG = [("soLuong", "number", "Tổng số lượng cấp, theo `donVi`, vd 15 viên. Bắt buộc ở dòng ghi từ bản 2.3; `null` "
                                  "ở dòng ghi trước đó. Có từ bản 2.3.")]
SO_NGAY_DUNG = ("Số ngày dùng. Bắt buộc ở dòng ghi từ bản 2.3: tối đa 30 ngày, hoặc 90 ngày khi `icdChinh.manTinh` = "
                "`true`; `null` ở dòng ghi trước đó nếu bác sĩ không ghi.")

# --- JSON mẫu: giá trị theo kết quả khám lay_mau.py ghi (temp/api-doc/mau, bac-si-ghi-ket-qua) ---
KQ_MAU_DAU = {"lyDoKham": "Ho, sốt nhẹ 2 ngày nay",
              "sinhHieu": {"mach": 88, "nhietDo": 37.8, "huyetApTamThu": 118, "huyetApTamTruong": 76, "nhipTho": 18,
                           "canNang": 54.5, "chieuCao": 158.0},
              "khamLamSang": "Họng đỏ, amidan không sưng, phổi trong."}
KQ_MAU_SAU = {"icdChinh": {"ma": "J02.9", "ten": "Viêm họng cấp, không đặc hiệu", "manTinh": False},
              "chanDoanKemTheo": "Viêm mũi dị ứng"}
SO_LUONG_MAU = {"Paracetamol": 15, "Oresol": 9}
THONG_BAO_HUY = ("đã từ chối lịch khám", "đã hủy lịch khám")
NHAC_LICH_MAU = ("Nhắc lịch khám: Nguyễn Thị Mai có lịch khám với bác sĩ Hoàng Minh Đức lúc 08:10 ngày 10/10/2026 tại "
                 "Phòng 103 (mã ECL-20261010-3552).")

_DOAN = re.compile(r"<w:p>(?:(?!</w:p>).)*</w:p>", re.S)


def _chu(doan):
    return html.unescape("".join(re.findall(r"<w:t[^>]*>([^<]*)</w:t>", doan)))


def _tim_doan(tl, muc, dau_chu):
    """(đầu, cuối) của đoạn duy nhất trong mục (kể cả trong bảng) có chữ bắt đầu bằng dau_chu."""
    dau, cuoi = _pham_vi_muc(tl, muc)
    cac = [m for m in _DOAN.finditer(tl.xml, dau, cuoi) if _chu(m.group(0)).startswith(dau_chu)]
    assert len(cac) == 1, f"{muc}: cần đúng 1 đoạn bắt đầu bằng {dau_chu!r}, thấy {len(cac)}"
    return cac[0].start(), cac[0].end()


def _trong_bang(tl, vi_tri):
    return tl.xml.rfind("<w:tc>", 0, vi_tri) > tl.xml.rfind("</w:tc>", 0, vi_tri)


def viet_lai(tl, muc, dau_chu, markup):
    """Viết lại chữ của 1 đoạn (hoặc ô bảng 1 đoạn), giữ định dạng đoạn; cỡ chữ theo thân bài hay bảng."""
    dau, cuoi = _tim_doan(tl, muc, dau_chu)
    cu = tl.xml[dau:cuoi]
    moi = cu[:cu.index("<w:r>")] + kit.runs(markup, _trong_bang(tl, dau)) + "</w:p>"
    tl.xml = tl.xml[:dau] + moi + tl.xml[cuoi:]


def chen_doan_sau(tl, muc, dau_chu, markup):
    """Thêm 1 đoạn cùng định dạng (vd 1 dòng LƯU Ý) ngay sau đoạn bắt đầu bằng dau_chu."""
    dau, cuoi = _tim_doan(tl, muc, dau_chu)
    cu = tl.xml[dau:cuoi]
    tl.xml = tl.xml[:cuoi] + cu[:cu.index("<w:r>")] + kit.runs(markup) + "</w:p>" + tl.xml[cuoi:]


def xoa_dong(tl, muc, o_dau):
    """Bỏ dòng có ô đầu đúng bằng o_dau của bảng đầu tiên trong mục, rồi tô lại nền xen kẽ."""
    dau, cuoi = tl._bang_sau(tl.vi_tri_tieu_de(muc))
    bang = tl.xml[dau:cuoi]
    cac = [m.group(0) for m in re.finditer(r"<w:tr>.*?</w:tr>", bang, re.S)
           if _chu(re.search(r"<w:p>.*?</w:p>", m.group(0), re.S).group(0)) == o_dau]
    assert len(cac) == 1, f"{muc}: cần đúng 1 dòng {o_dau!r}, thấy {len(cac)}"
    bang = bang.replace(cac[0], "", 1)
    tl.xml = tl.xml[:dau] + kit.to_lai_nen(bang) + tl.xml[cuoi:]


def sua_o_cuoi(tl, muc, ten_truong, markup):
    """Viết lại ô cuối (Mô tả) của dòng duy nhất trong mục có ô `ten_truong`."""
    dau, cuoi = _pham_vi_muc(tl, muc)
    khoa = f">{ten_truong}</w:t>"
    assert tl.xml.count(khoa, dau, cuoi) == 1, f"{muc}: cần đúng 1 dòng {ten_truong}"
    vi_tri = tl.xml.index(khoa, dau)
    dong_dau, dong_cuoi = tl.xml.rindex("<w:tr>", 0, vi_tri), tl.xml.index("</w:tr>", vi_tri)
    o = tl.xml.rindex("<w:tc>", dong_dau, dong_cuoi)
    p_dau = tl.xml.index("<w:p>", o)
    p_cuoi = tl.xml.index("</w:p>", p_dau)
    ppr = tl.xml[p_dau:p_cuoi]
    ppr = ppr[:ppr.index("</w:pPr>") + len("</w:pPr>")]
    tl.xml = tl.xml[:p_dau] + ppr + kit.runs(markup, True) + tl.xml[p_cuoi:]


def danh_dau_route(tl, muc):
    """Ô "Thay đổi" của 1 dòng bảng route -> "Cập nhật · v2.3"."""
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
    """Mỗi mục chỉ 1 dòng ghi thay đổi. Mục đã có dòng của v2.1 / v2.2: 1 dòng gộp "Thay đổi ở v2.3: ... Ở v2.x: <dòng
    cũ>", giữ nguyên chữ và định dạng của dòng cũ. Chưa có: thêm dưới dòng Quyền truy cập."""
    dau, cuoi = _pham_vi_muc(tl, f"{muc}  {ten}")
    cu = [m for m in _DOAN.finditer(tl.xml, dau, cuoi) if re.match(r"Thay đổi ở v2\.[12]:", _chu(m.group(0)))]
    if not cu:
        tl.chen_sau_doan("Quyền truy cập:", kit.doan(f"**Thay đổi ở v{PHIEN_BAN}:** " + markup), tu=dau)
        return
    doan = cu[0].group(0)
    nhan = re.search(r"<w:r><w:rPr>(?:(?!</w:r>).)*>Thay đổi ở v(2\.[12]):</w:t></w:r>", doan, re.S)
    moi = (doan[:doan.index("<w:r>")] + kit.runs(f"**Thay đổi ở v{PHIEN_BAN}:** {markup} **Ở v{nhan.group(1)}:**")
           + doan[nhan.end():])
    tl.xml = tl.xml[:cu[0].start()] + moi + tl.xml[cu[0].end():]


# --- JSON mẫu ---

def _moi_dict(gia_tri, ham):
    """Gọi ham(d) cho mọi dict nằm trong gia_tri (kể cả chính nó)."""
    if isinstance(gia_tri, dict):
        ham(gia_tri)
        for v in list(gia_tri.values()):
            _moi_dict(v, ham)
    elif isinstance(gia_tri, list):
        for v in gia_tri:
            _moi_dict(v, ham)


def _xac_nhan_ngay(d):
    if d.get("trangThai") == "CHO_XAC_NHAN":
        d["trangThai"] = "DA_XAC_NHAN"
    if isinstance(d.get("noiDung"), str):
        d["noiDung"] = d["noiDung"].replace("Lịch đang chờ bác sĩ xác nhận.", "Lịch đã được xác nhận.")


def doi_trang_thai(du_lieu):
    _moi_dict(du_lieu, _xac_nhan_ngay)


def _ket_qua(d):
    kq = d.get("ketQua")
    if not isinstance(kq, dict):
        return
    moi = dict(KQ_MAU_DAU)
    for k, v in kq.items():
        moi[k] = v
        if k == "chanDoan":
            moi.update(KQ_MAU_SAU)
    for i, dong in enumerate(moi["donThuoc"]):
        thuoc = {}
        for k, v in dong.items():
            thuoc[k] = v
            if k == "soNgayDung":
                thuoc["soLuong"] = SO_LUONG_MAU[dong["tenThuoc"]]
        moi["donThuoc"][i] = thuoc
    d["ketQua"] = moi


def doi_ket_qua(du_lieu):
    _moi_dict(du_lieu, _xac_nhan_ngay)
    _moi_dict(du_lieu, _ket_qua)


def _thong_bao(d):
    if d.get("loai") == "LICH_HEN_BI_TU_CHOI":
        assert THONG_BAO_HUY[0] in d["noiDung"], d["noiDung"]
        d["noiDung"] = d["noiDung"].replace(*THONG_BAO_HUY)
    elif d.get("loai") == "LICH_HEN_DA_XAC_NHAN":
        d["loai"], d["noiDung"] = "NHAC_LICH_KHAM", NHAC_LICH_MAU


def doi_thong_bao(du_lieu):
    _moi_dict(du_lieu, _thong_bao)


def sua(tl):
    # --- Trang đầu và mục 1 ---
    cu = ">2.2 — 14/10/2026</w:t>"
    vi_tri = tl.xml.index(cu)  # ô Phiên bản của trang đầu (đứng trước bảng lịch sử)
    tl.xml = tl.xml[:vi_tri] + f">{PHIEN_BAN} — {NGAY}</w:t>" + tl.xml[vi_tri + len(cu):]
    viet_lai(tl, MUC_1, "Không có trong tài liệu này:",
             "**Không có trong tài liệu này:** các route đặt lịch của bác sĩ (danh sách lịch hẹn, hủy lịch, hàng đợi "
             "khám, ca làm việc) và của quản trị viên (quản lý lịch hẹn, lịch làm việc, hồ sơ bệnh nhân, danh mục); API "
             "của `medical-service`, `report-service`.")
    tl.chen_sau_doan("Từ bản 2.2:", kit.doan(
        f"**Từ bản {PHIEN_BAN}:** lịch hẹn được xác nhận ngay khi đặt hoặc đổi, không còn chờ bác sĩ xác nhận "
        "(`CHO_XAC_NHAN`); bác sĩ hủy được lịch trước giờ khám, kèm lý do (`BI_TU_CHOI`); lịch không đến khám thành "
        "`KHONG_DEN` từ ngày hôm sau (3.11); kết quả khám có thêm lý do khám, sinh hiệu, khám lâm sàng, mã ICD-10, bệnh "
        "kèm theo và số lượng thuốc (4.27). Các route của bác sĩ đi kèm (hủy lịch, tìm mã ICD-10, ghi bệnh án) có ở "
        "`docs/BAN-GIAO-FRONTEND-05-10.md` trong mã nguồn backend."))
    tl.them_dong(MUC_1, "LỊCH SỬ PHIÊN BẢN", kit.dong_bang(RONG_PHIEN_BAN, LICH_SU))

    # --- 3.7 ---
    m37 = "3.7 Giá trị enum"
    viet_lai(tl, m37, "CHO_XAC_NHAN, DA_XAC_NHAN, BI_TU_CHOI",
             "`DA_XAC_NHAN`, `BI_TU_CHOI`, `DA_HOAN_THANH`, `DA_HUY`, `DA_HUY_DO_DOI_LICH`, `KHONG_DEN`. Ý nghĩa từng "
             "giá trị ở 3.11.")
    viet_lai(tl, m37, "Bộ lọc \"lịch hẹn của tôi\"",
             "Bộ lọc \"lịch hẹn của tôi\": `TAT_CA`, `SAP_TOI` (lịch `DA_XAC_NHAN` mà khung giờ chưa kết thúc), "
             "`LICH_SU` (mọi lịch còn lại: đã khám, đã hủy, bác sĩ đã hủy, không đến, đã qua giờ)")
    viet_lai(tl, m37, "Gửi cho bệnh nhân: LICH_HEN_DA_XAC_NHAN",
             "Gửi cho bệnh nhân: `LICH_HEN_BI_TU_CHOI` (bác sĩ hủy lịch đã xác nhận, kèm lý do; nghĩa này từ bản 2.3), "
             "`LICH_HEN_CAN_DOI` (ca khám bị hủy, cần đổi lịch), `LICH_HEN_DOI_PHONG` (giữ nguyên giờ, đổi phòng khám), "
             "`NHAC_LICH_KHAM` (nhắc trước giờ khám, từ bản 2.1), `NHAC_TAI_KHAM` (bác sĩ hẹn / đổi ngày tái khám và lời "
             "nhắc trước ngày đó, từ bản 2.1). `LICH_HEN_DA_XAC_NHAN` (bác sĩ xác nhận lịch) chỉ còn ở thông báo tạo "
             "trước bản 2.3. Chỉ gửi cho bác sĩ / quản trị viên: `LICH_HEN_MOI`, `LICH_HEN_DA_DOI`, `LICH_HEN_DA_HUY`, "
             "`KET_QUA_DUYET_DOI_LICH`, `CA_LAM_VIEC_THAY_DOI`, `YEU_CAU_DOI_LICH_MOI`. Đã khai báo nhưng bản hiện tại "
             "chưa gửi: `HE_THONG`; frontend nên hiển thị được mọi giá trị bằng `noiDung`")

    # --- 3.11 ---
    xoa_dong(tl, MUC_311, "CHO_XAC_NHAN")
    viet_lai(tl, MUC_311, "Bác sĩ đã xác nhận.",
             "Đặt hoặc đổi lịch thành công: lịch được xác nhận ngay, không chờ bác sĩ duyệt.")
    viet_lai(tl, MUC_311, "Bác sĩ từ chối; lý do ở",
             "Bác sĩ hủy lịch trước giờ khám; lý do ở `lyDoHuy`. Bệnh nhân nhận thông báo `LICH_HEN_BI_TU_CHOI` và email "
             "(3.13).")
    tl.them_dong(MUC_311, None, kit.dong_bang((2400, 4000, 3238), (
        "`KHONG_DEN`",
        "Bệnh nhân không đến: từ 00:01 ngày hôm sau, lịch `DA_XAC_NHAN` của ngày đã qua mà chưa khám thành `KHONG_DEN`. "
        "Lịch bệnh nhân đã được gọi vào khám, hoặc lịch đang chờ đổi vì ca khám bị hủy (`canDoiLich`), giữ nguyên. Có từ "
        "bản 2.3.",
        "Không; không hủy, đổi lịch hay đánh giá được.")), sau_dong_chua="DA_HUY_DO_DOI_LICH")
    viet_lai(tl, MUC_311, "Đổi lịch = hủy lịch cũ + tạo lịch mới",
             "**Đổi lịch = hủy lịch cũ + tạo lịch mới**: lịch cũ thành `DA_HUY_DO_DOI_LICH`, response là "
             "`DatLichResponse` của lịch mới (`DA_XAC_NHAN`) với `maPhieuKham` **mới**. Sau khi đổi, chuyển sang phiếu "
             "khám mới; link cũ vẫn mở được và hiện trạng thái đã đổi.")
    viet_lai(tl, MUC_311, "Lịch đã qua giờ",
             "**Lịch đã qua giờ**: trong ngày khám, lịch `DA_XAC_NHAN` mà khung giờ đã kết thúc vẫn giữ nguyên "
             "`trangThai` nhưng thuộc bộ lọc `LICH_SU`, không còn là `SAP_TOI`. Từ ngày hôm sau, lịch chưa khám thành "
             "`KHONG_DEN`, trừ lịch bệnh nhân đã được gọi vào khám hoặc lịch đang chờ đổi (`canDoiLich`).")

    # --- 3.12, 3.13 ---
    m312 = "3.12 Ngôn ngữ (EN / VI)"
    viet_lai(tl, m312, "Họ tên người (bệnh nhân",
             "Họ tên người (bệnh nhân, bác sĩ, người giám hộ), học vị `hocVi`, tên thuốc, mã tra cứu, và mọi chữ người "
             "dùng tự gõ: lý do khám, lý do hủy, nhận xét đánh giá, khám lâm sàng, chẩn đoán, bệnh kèm theo, lời dặn, "
             "ghi chú đơn thuốc. Hiển thị nguyên văn ở cả hai ngôn ngữ.")
    viet_lai(tl, m312, "API trả mã (",
             "API trả mã (`DA_XAC_NHAN`, `NAM`...); nhãn hiển thị của hai ngôn ngữ do frontend tự đặt.")
    viet_lai(tl, MUC_313, "Đã nhận yêu cầu đặt lịch", "Đặt lịch khám thành công")
    viet_lai(tl, MUC_313, "Ngay sau khi đặt lịch thành công (13.1)",
             "Ngay sau khi đặt lịch thành công (13.1), khách hay tài khoản đều có. Email báo lịch đã được xác nhận "
             "(trước bản 2.3: \"đã nhận yêu cầu\").")
    viet_lai(tl, MUC_313, "Trước giờ khám 3 ngày và 1 ngày",
             "Trước giờ khám 3 ngày và 1 ngày, chỉ gửi trong khoảng 07:00-21:00. Lịch đã hủy, bác sĩ đã hủy hoặc đang "
             "chờ đổi (`canDoiLich`) không nhắc; lịch đặt sát giờ thì bỏ mốc đã qua.")
    tl.them_dong(MUC_313, None, kit.dong_bang(RONG_EMAIL, (
        "Bác sĩ hủy lịch",
        "Ngay khi bác sĩ hủy 1 lịch đã xác nhận, trước giờ khám, kèm lý do (lịch thành `BI_TU_CHOI`). Có từ bản 2.3.",
        "Phiếu khám; lý do hủy ghi trên phiếu.")), sau_dong_chua="Đã đổi lịch")
    viet_lai(tl, MUC_313, "Thông báo trên chuông đi kèm:",
             "**Thông báo trên chuông đi kèm:** lời nhắc lịch khám sinh thông báo `NHAC_LICH_KHAM`, bác sĩ hủy lịch sinh "
             "`LICH_HEN_BI_TU_CHOI`, việc hẹn / đổi ngày tái khám và lời nhắc tái khám sinh thông báo `NHAC_TAI_KHAM` "
             "cho tài khoản phía bệnh nhân (mục 18). Email \"đặt lịch khám thành công\" và \"đã đổi lịch\" không sinh "
             "thông báo.")

    # --- Mô hình ---
    viet_lai(tl, "4.21 DatLichResponse", "Luôn là ",
             "Luôn là `DA_XAC_NHAN` ở lịch vừa đặt / vừa đổi: lịch được xác nhận ngay (từ bản 2.3).")
    viet_lai(tl, "4.22 PhieuKhamResponse", "Lý do bác sĩ từ chối (",
             "Lý do bác sĩ hủy (`BI_TU_CHOI`) hoặc lý do bệnh nhân hủy; `null` ở các trạng thái khác.")
    viet_lai(tl, "4.25 LichHenCuaToiResponse", "Lý do bác sĩ từ chối hoặc",
             "Lý do bác sĩ hủy (`BI_TU_CHOI`) hoặc lý do bệnh nhân hủy; `null` ở các trạng thái khác.")
    viet_lai(tl, "4.33 TrangCaNhanResponse", "Lịch đã khám, đã hủy, bị từ chối",
             "Lịch đã khám, đã hủy, bác sĩ đã hủy, không đến hoặc đã qua giờ, của cả hai bên.")
    viet_lai(tl, "4.40 GoiYResponse", "CHO_XAC_NHAN hoặc DA_XAC_NHAN", "`DA_XAC_NHAN` (lịch sắp tới).")

    sua_o_cuoi(tl, MUC_427, "soNgayDung", SO_NGAY_DUNG)
    them_truong(tl, MUC_427, SO_LUONG, "soNgayDung", nhan_bang=" — phần tử của ")
    them_truong(tl, MUC_427, KQ_SAU_CHAN_DOAN, "chanDoan")
    for ten, kieu, mo_ta in reversed(KQ_DAU):
        tl.them_dong(MUC_427, None, kit.dong_bang(p2_quy_uoc.RONG_MO_HINH_MOI, (f"`{ten}`", kieu, mo_ta)),
                     sau_dong_chua="Trường")
    dau, _ = _tim_doan(tl, MUC_427, "DonThuoc — ")
    con = (kit.doan("`SinhHieu` — giá trị của `sinhHieu`. Chỉ số bác sĩ không đo là `null`.")
           + p2_quy_uoc.bang_mo_hinh(SINH_HIEU)
           + kit.doan("`IcdChinh` — giá trị của `icdChinh`.") + p2_quy_uoc.bang_mo_hinh(ICD_CHINH))
    tl.xml = tl.xml[:dau] + con + tl.xml[dau:]

    # --- Đoạn văn của các mục route ---
    viet_lai(tl, "10.3  Vô hiệu hoá hoặc kích hoạt lại tài khoản", "Vô hiệu hoá bác sĩ còn lịch hẹn sắp tới (",
             "Vô hiệu hoá bác sĩ còn lịch hẹn sắp tới (`DA_XAC_NHAN`); phải chuyển hoặc huỷ lịch trước.")
    viet_lai(tl, "13.1  Đặt lịch khám", "Đặt 1 lịch hẹn cho 1 người khám",
             "Đặt 1 lịch hẹn cho 1 người khám vào 1 khung giờ. Hệ thống tự chọn lượt khám trống sớm nhất trong khung, "
             "cấp số thứ tự và trả mã phiếu khám. Lịch mới ở trạng thái `DA_XAC_NHAN` ngay, không chờ bác sĩ xác nhận.")
    viet_lai(tl, "14.1  Xem phiếu khám", "Trạng thái trên phiếu thay đổi theo thời gian",
             "Trạng thái trên phiếu thay đổi theo thời gian (bác sĩ hủy lịch, ghi kết quả khám, hệ thống đặt "
             "`KHONG_DEN`...): gọi lại route này mỗi lần mở trang, không lưu phiếu ở frontend.")
    chen_doan_sau(tl, "14.1  Xem phiếu khám", "Hiện nút hủy / đổi theo",
                  "`trangThai` = `BI_TU_CHOI`: hiện \"Bác sĩ đã hủy lịch\" kèm `lyDoHuy`. `KHONG_DEN`: hiện \"Không "
                  "đến\"; không còn nút hủy / đổi.")
    loi_409 = ("Lịch không còn `DA_XAC_NHAN`: đã khám xong, đã hủy, đã đổi sang lịch khác, bác sĩ đã hủy hoặc không đến "
               "(`KHONG_DEN`).")
    for muc in ("14.3  Hủy lịch bằng link phiếu khám", "14.4  Đổi lịch bằng link phiếu khám", "15.4  Hủy lịch hẹn của tôi",
                "15.5  Đổi lịch hẹn của tôi"):
        viet_lai(tl, muc, "Lịch không còn CHO_XAC_NHAN", loi_409)
    m151 = "15.1  Danh sách lịch hẹn của tôi"
    viet_lai(tl, m151, "Gọi lại mỗi lần mở màn hình",
             "Gọi lại mỗi lần mở màn hình: trạng thái đổi khi bác sĩ hủy lịch, ghi kết quả khám, hoặc khi lịch thành "
             "`KHONG_DEN`.")
    viet_lai(tl, m151, "Mặc định TAT_CA. SAP_TOI",
             "Mặc định `TAT_CA`. `SAP_TOI`: lịch `DA_XAC_NHAN` mà khung giờ chưa kết thúc; `LICH_SU`: mọi lịch còn lại "
             "(đã khám, đã hủy, bác sĩ đã hủy, không đến, đã đổi, đã qua giờ). Xem `LocLichHen` ở 3.7.")
    viet_lai(tl, m151, "lyDoHuy có giá trị ở lịch",
             "`lyDoHuy` có giá trị ở lịch `BI_TU_CHOI` (lý do bác sĩ hủy) và lịch `DA_HUY` có nhập lý do.")
    m155 = "15.5  Đổi lịch hẹn của tôi"
    viet_lai(tl, m155, "duLieu: DatLichResponse (4.21) của lịch mới",
             "`duLieu`: `DatLichResponse` (4.21) của lịch mới (`DA_XAC_NHAN`).")
    viet_lai(tl, m155, "Lịch đã được bác sĩ xác nhận mà đổi",
             "Lịch mới được xác nhận ngay như lúc đặt (`DA_XAC_NHAN`); không có bước chờ bác sĩ xác nhận lại.")
    viet_lai(tl, "18. Thông báo", "Bệnh nhân nhận 6 loại",
             "Bệnh nhân nhận 5 loại (`LoaiThongBao` ở 3.7): `LICH_HEN_BI_TU_CHOI` khi bác sĩ hủy lịch (kèm lý do); "
             "`LICH_HEN_CAN_DOI` khi ca khám bị hủy và lịch cần đổi (3.11); `LICH_HEN_DOI_PHONG` khi ca khám đổi phòng; "
             "`NHAC_LICH_KHAM` trước giờ khám 3 ngày và 1 ngày; `NHAC_TAI_KHAM` khi bác sĩ hẹn hoặc đổi ngày tái khám và "
             "7 ngày trước ngày đó (3.13). Thông báo cũ có thể còn loại `LICH_HEN_DA_XAC_NHAN` (bác sĩ xác nhận lịch, "
             "trước bản 2.3). Người nhận là tài khoản đã đặt lịch và tài khoản của hồ sơ người khám. Bệnh nhân không "
             "nhận thông báo khi tự đặt, tự hủy, tự đổi lịch hoặc khi bác sĩ ghi kết quả khám mà không hẹn tái khám.")
    viet_lai(tl, "18.1  Danh sách thông báo của tôi", "noiDung là câu đã soạn sẵn",
             "`noiDung` là câu đã soạn sẵn theo `Accept-Language` của request đọc: hiển thị nguyên văn, không tự ghép "
             "lại từ `loai`. Người dùng đổi ngôn ngữ thì tải lại danh sách để nhận câu ở ngôn ngữ mới. Họ tên, mã tra "
             "cứu và lý do do người dùng gõ nằm trong câu vẫn giữ nguyên văn (vd tiếng Anh: \"Dr. Hoàng Minh Đức "
             "cancelled the appointment of Nguyễn Thị Mai at 10:10 on 10/10/2026 (code ECL-20261010-6213). Reason: Bác "
             "sĩ có lịch hội chẩn đột xuất, mời bạn đặt khung giờ khác\"). Thông báo tạo trước bản 2.1 chỉ có câu tiếng "
             "Việt.")

    # --- Phụ lục B, C ---
    viet_lai(tl, "B.7 Khách đặt lịch và quản lý bằng phiếu khám", "POST /api/booking/lich-hen (13.1)",
             "`POST /api/booking/lich-hen` (13.1) với `idLichLamViec` (hoặc `idChuyenKhoa`), `gioBatDauKhung`, "
             "`benhNhan`, `lyDoKham`; người khám dưới 18 tuổi kèm `nguoiGiamHo` → 201, lịch `DA_XAC_NHAN` (đã xác nhận, "
             "không chờ bác sĩ). Lưu `maPhieuKham` và hiện liên kết phiếu khám (3.10): khách không có cách nào khác để mở "
             "lại.")
    m_b8 = "B.8 Bệnh nhân đã đăng nhập đặt, đổi và hủy lịch"
    viet_lai(tl, m_b8, "Bác sĩ xác nhận hoặc từ chối",
             "Bác sĩ hủy lịch (trước giờ khám, kèm lý do): lịch thành `BI_TU_CHOI`, bệnh nhân nhận thông báo (B.11) và "
             "email. Không đến khám: từ ngày hôm sau lịch thành `KHONG_DEN` (3.11).")
    viet_lai(tl, m_b8, "Đổi lịch: POST",
             "Đổi lịch: `POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/doi-lich` (15.5) → 201, lịch mới mang mã mới, "
             "vẫn `DA_XAC_NHAN`; lịch cũ thành `DA_HUY_DO_DOI_LICH`. Mỗi lần đặt đổi được 2 lần.")
    viet_lai(tl, "B.11 Thông báo", "Bệnh nhân nhận thông báo khi bác sĩ",
             "Bệnh nhân nhận thông báo khi bác sĩ hủy lịch, khi ca khám bị hủy (cần đổi lịch), khi đổi phòng khám, khi "
             "tới mốc nhắc lịch khám, và khi bác sĩ hẹn tái khám hoặc tới mốc nhắc tái khám (3.13). Thao tác của chính "
             "bệnh nhân (đặt, đổi, hủy, đánh giá) không sinh thông báo. Thông báo về bác sĩ hủy lịch, hủy ca và đổi phòng "
             "xuất hiện sau sự kiện khoảng 5 giây; lời nhắc và thông báo tái khám theo chu kỳ nhắc (mặc định 5 phút, chỉ "
             "trong 07:00-21:00).")
    viet_lai(tl, "Tài khoản có sẵn", "Chưa có lịch hẹn, kết quả khám",
             "Chưa có lịch hẹn, kết quả khám hay thông báo nào: lịch vừa đặt đã là `DA_XAC_NHAN`; muốn có lịch "
             "`DA_HOAN_THANH` thì đăng nhập tài khoản bác sĩ của ca khám để ghi kết quả.")

    # --- JSON mẫu ---
    for muc in ("13.1  Đặt lịch khám", "14.1  Xem phiếu khám", "14.4  Đổi lịch bằng link phiếu khám",
                "15.1  Danh sách lịch hẹn của tôi", "15.2  Lịch hẹn của tôi theo khoảng ngày", m155, "19.2  Gửi tin nhắn"):
        sua_json_mau(tl, muc, doi_trang_thai)
    for muc in ("15.3  Chi tiết lịch hẹn của tôi", "16.1  Lịch sử khám của tôi", "17.3  Trang cá nhân của bệnh nhân"):
        sua_json_mau(tl, muc, doi_ket_qua)
    for muc in ("18.1  Danh sách thông báo của tôi", "18.3  Đánh dấu 1 thông báo đã đọc"):
        sua_json_mau(tl, muc, doi_thong_bao)

    # --- Dòng "Thay đổi ở v2.3" và cột "Thay đổi" ---
    for muc, ten, markup in CAP_NHAT:
        ghi_thay_doi(tl, muc, ten, markup)
        danh_dau_route(tl, muc)
