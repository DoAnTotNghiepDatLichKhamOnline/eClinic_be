"""Bản 2.2 của tài liệu API: phần phía bệnh nhân của DOANTOTNGH-10 (danh mục thuốc, sửa hồ sơ bệnh án, hồ sơ bác sĩ).

- Dòng đơn thuốc trong kết quả khám mang thuốc của danh mục: `idThuoc`, `hoatChat`, `hamLuong`, `duongDung` (4.27).
- Kết quả khám có `ngaySuaCuoi` ("đã chỉnh sửa lúc ..."); `phienBan` / `hanSua` chỉ có ở màn hình bác sĩ (4.27).
- Chi tiết bác sĩ có giấy phép hành nghề, ngôn ngữ khám, giải thưởng, công trình nghiên cứu (4.12).

Không thêm route: các route của bác sĩ / quản trị viên (kê đơn, hàng đợi, lịch sử sửa, bác sĩ tự sửa hồ sơ) nằm ngoài phạm
vi tài liệu này, có ở docs/BAN-GIAO-FRONTEND-05-10.md. Giai đoạn 8 của chuỗi dựng tài liệu; chạy qua build.py.
"""

import html
import json
import re

import kit
import p2_quy_uoc
from p6_ngon_ngu import RONG_PHIEN_BAN

PHIEN_BAN = "2.2"
NGAY = "14/10/2026"
MUC_1 = "1. Giới thiệu và phạm vi"
MUC_2 = "2. Danh sách route"
DONG_RONG = p2_quy_uoc.RONG_MO_HINH_MOI

# Mục có response đổi ở bản này: (mục, tiêu đề mục chi tiết, điều đã đổi). Quyết định cột "Thay đổi" của bảng route.
CAP_NHAT = (
    ("11.4", "Chi tiết bác sĩ",
     "`duLieu` (`BacSiChiTietResponse`, 4.12) có thêm giấy phép hành nghề (`soGiayPhep`, `ngayCapGiayPhep`, "
     "`noiCapGiayPhep`, `phamViHanhNghe`), `ngonNguKham`, `giaiThuong`, `congTrinhNghienCuu`; `soNamKinhNghiem` nay "
     "suy ra từ năm bắt đầu hành nghề. Ở v2.1: `chucVu`, `gioiThieuNgan`, `tenChuyenKhoa`, `tieuSu`, các danh sách "
     "và `anh[].chuThich` theo `Accept-Language` (3.12); `hoTen`, `hocVi` không dịch."),
    ("15.3", "Chi tiết lịch hẹn của tôi",
     "`ketQua` (`KetQuaKhamResponse`, 4.27) có thêm `ngaySuaCuoi`; mỗi dòng `donThuoc` có thêm `idThuoc`, "
     "`hoatChat`, `hamLuong`, `duongDung`."),
    ("16.1", "Lịch sử khám của tôi", "`ketQua` của mỗi lượt khám đổi như ở 15.3 (4.27)."),
    ("17.3", "Trang cá nhân của bệnh nhân", "`lanKhamGanDay[].ketQua` đổi như ở 15.3 (4.27)."),
)

LICH_SU = ("2.2 — " + NGAY,
           "Kết quả khám: dòng đơn thuốc theo danh mục thuốc (`idThuoc`, `hoatChat`, `hamLuong`, `duongDung`) và dấu "
           "\"đã chỉnh sửa\" `ngaySuaCuoi` (4.27); chi tiết bác sĩ có giấy phép hành nghề, ngôn ngữ khám, giải thưởng, "
           "công trình nghiên cứu (4.12); enum `DuongDungThuoc` (3.7).")

# --- Mô hình: (tên trường, kiểu, mô tả), chèn đúng thứ tự record Java ---
BAC_SI_MOI = [
    ("giaiThuong", "string[]", "Giải thưởng, mỗi phần tử 1 dòng; mảng rỗng khi chưa có. Có từ bản 2.2."),
    ("congTrinhNghienCuu", "string[]", "Công trình nghiên cứu, mỗi phần tử 1 dòng; mảng rỗng khi chưa có. Có từ bản "
                                       "2.2."),
    ("ngonNguKham", "string[]", "Ngôn ngữ bác sĩ khám được: mã ISO 639 viết thường, vd `[\"vi\", \"en\"]`; mảng rỗng "
                                "khi chưa có. Hiện tên ngôn ngữ theo ngôn ngữ giao diện (vd `Intl.DisplayNames`). Có "
                                "từ bản 2.2."),
    ("soGiayPhep", "string", "Số giấy phép (chứng chỉ) hành nghề, công khai để người bệnh tra cứu; `null` nếu chưa "
                             "nhập. Có từ bản 2.2."),
    ("ngayCapGiayPhep", "date", "Ngày cấp giấy phép; `null` nếu chưa nhập. Có từ bản 2.2."),
    ("noiCapGiayPhep", "string", "Nơi cấp, theo `Accept-Language` (3.12); `null` nếu chưa nhập. Có từ bản 2.2."),
    ("phamViHanhNghe", "string", "Phạm vi hành nghề ghi trên giấy phép, theo `Accept-Language`; `null` nếu chưa nhập. "
                                 "Có từ bản 2.2."),
]
KET_QUA_MOI = [
    ("ngaySuaCuoi", "datetime", "Lần bác sĩ sửa kết quả gần nhất sau khi lưu; `null` = chưa sửa. Khác `null` thì hiện "
                                "\"Đã chỉnh sửa lúc <ngày giờ>\"; bệnh nhân không xem được lý do hay nội dung cũ. Có từ "
                                "bản 2.2."),
    ("phienBan", "number", "**Không có mặt** trong mọi response của tài liệu này (chỉ màn hình bác sĩ có)."),
    ("hanSua", "datetime", "**Không có mặt** trong mọi response của tài liệu này (chỉ màn hình bác sĩ có)."),
]
THUOC_DAU = ("idThuoc", "number", "Id thuốc trong danh mục do quản trị viên quản lý. Có từ bản 2.2.")
THUOC_SAU_TEN = [
    ("hoatChat", "string", "Hoạt chất; `null` ở thuốc cũ quản trị viên chưa bổ sung (khi đó hàm lượng và đường dùng "
                           "cũng `null`). Có từ bản 2.2."),
    ("hamLuong", "string", "Hàm lượng, vd `500mg`; hiện cạnh tên thuốc. Có từ bản 2.2."),
    ("duongDung", "enum DuongDungThuoc", "Đường dùng (3.7); `null` nếu chưa nhập. Có từ bản 2.2."),
]
DUONG_DUNG = ("`DuongDungThuoc`", "Đường dùng của thuốc: `UONG`, `NGAM_DUOI_LUOI`, `TIEM`, `BOI_NGOAI_DA`, `NHO_MAT`, "
              "`NHO_TAI`, `NHO_MUI`, `XIT_HIT`, `DAT` (đặt hậu môn / âm đạo), `KHAC`. Nhãn hiển thị do frontend đặt")

# --- JSON mẫu (giá trị lấy theo dữ liệu mẫu, cấu trúc theo response thật ở temp/api-doc/mau) ---
BAC_SI_MAU = {"giaiThuong": [], "congTrinhNghienCuu": [],
              "ngonNguKham": ["vi", "en"], "soGiayPhep": "CCHN-000005", "ngayCapGiayPhep": "2014-09-15",
              "noiCapGiayPhep": "Sở Y tế TP. Hồ Chí Minh",
              "phamViHanhNghe": "Khám bệnh, chữa bệnh chuyên khoa Da liễu"}
THUOC_MAU = {
    "Paracetamol 500mg": {"idThuoc": 1, "tenThuoc": "Paracetamol", "hoatChat": "Paracetamol", "hamLuong": "500mg",
                          "duongDung": "UONG", "donVi": "viên"},
    "Nước muối sinh lý 0,9%": {"idThuoc": 14, "tenThuoc": "Oresol",
                               "hoatChat": "Natri clorid + Kali clorid + Natri citrat + Glucose khan",
                               "hamLuong": "4,22g", "duongDung": "UONG", "donVi": "gói",
                               "lieuDung": "1 gói pha 200 ml nước", "soNgayDung": 3},
}
THU_TU_THUOC = ("idThuoc", "tenThuoc", "hoatChat", "hamLuong", "duongDung", "donVi", "lieuDung", "soLanMoiNgay",
                "soNgayDung", "ghiChuSuDung")

_DONG_CODE = re.compile(r'<w:p><w:pPr>(?:<w:keepNext w:val="false"/>|<w:keepNext/>)' + re.escape(kit._PPR_CODE)
                        + r'</w:pPr><w:r><w:rPr>[^<]*(?:<[^>]*/>)*</w:rPr><w:t xml:space="preserve">([^<]*)</w:t></w:r></w:p>')


def _pham_vi_muc(tl, tieu_de):
    """(đầu, cuối) của 1 mục chi tiết: từ heading tới heading kế tiếp."""
    dau = tl.vi_tri_tieu_de(tieu_de)
    cuoi = tl.xml.find('<w:pStyle w:val="Heading', dau + 50)
    return dau, (cuoi if cuoi >= 0 else len(tl.xml))


def sua_json_mau(tl, tieu_de, doi):
    """Viết lại mọi khối JSON response thành công trong mục: doi(du_lieu) sửa tại chỗ dict của `duLieu`."""
    dau, cuoi = _pham_vi_muc(tl, tieu_de)
    doan = tl.xml[dau:cuoi]
    so = 0
    for khoi in re.finditer(rf"(?:{_DONG_CODE.pattern})+", doan):
        chu = "\n".join(html.unescape(m.group(1)) for m in _DONG_CODE.finditer(khoi.group(0)))
        try:
            mau = json.loads(chu)
        except ValueError:
            continue
        if not (isinstance(mau, dict) and mau.get("thanhCong") is True and "duLieu" in mau):
            continue
        doi(mau["duLieu"])
        dong = json.dumps(mau, ensure_ascii=False, indent=2).split("\n")
        moi = "".join(kit.dong_code(d, i == len(dong) - 1) for i, d in enumerate(dong))
        doan = doan.replace(khoi.group(0), moi, 1)
        so += 1
    assert so, f"Mục {tieu_de}: không có khối JSON response thành công nào"
    tl.xml = tl.xml[:dau] + doan + tl.xml[cuoi:]


def _theo_thu_tu(cu, them, sau_khoa):
    """Dict mới: các khoá cũ, chèn `them` ngay sau khoá `sau_khoa`."""
    moi = {}
    for k, v in cu.items():
        moi[k] = v
        if k == sau_khoa:
            moi.update(them)
    return moi


def doi_bac_si(du_lieu):
    moi = _theo_thu_tu(du_lieu, BAC_SI_MAU, "linhVucKhamChua")
    du_lieu.clear()
    du_lieu.update(moi)


def doi_ket_qua(ket_qua):
    for i, dong in enumerate(ket_qua["donThuoc"]):
        dong = {**dong, **THUOC_MAU[dong["tenThuoc"]]}
        ket_qua["donThuoc"][i] = {k: dong[k] for k in THU_TU_THUOC}
    ket_qua["ngaySuaCuoi"] = None


def doi_moi_ket_qua(gia_tri):
    """Đổi mọi object `ketQua` (khác null) nằm trong gia_tri."""
    if isinstance(gia_tri, dict):
        for k, v in gia_tri.items():
            if k == "ketQua" and isinstance(v, dict):
                doi_ket_qua(v)
            else:
                doi_moi_ket_qua(v)
    elif isinstance(gia_tri, list):
        for v in gia_tri:
            doi_moi_ket_qua(v)


def them_truong(tl, mo_hinh, cac_dong, sau, nhan_bang=None):
    """Chèn lần lượt các dòng mô hình, dòng đầu ngay sau dòng có chữ `sau`."""
    for ten, kieu, mo_ta in cac_dong:
        tl.them_dong(mo_hinh, nhan_bang, kit.dong_bang(DONG_RONG, (f"`{ten}`", kieu, mo_ta)), sau_dong_chua=sau)
        sau = ten


def danh_dau_route(tl, muc, loai):
    """Ô "Thay đổi" của 1 dòng bảng route (đã 6 cột) -> "<loai> · v2.2"."""
    dau, cuoi = tl._bang_sau(tl.vi_tri_tieu_de(MUC_2))
    bang = tl.xml[dau:cuoi]
    cac_dong = [m for m in re.finditer(r"<w:tr>.*?</w:tr>", bang, re.S)
                if re.search(r"<w:t[^>]*>([^<]*)</w:t>", m.group(0)).group(1) == muc]
    assert len(cac_dong) == 1, f"Bảng route: cần đúng 1 dòng mục {muc}, thấy {len(cac_dong)}"
    dong = cac_dong[0].group(0)
    o_cuoi = dong.rindex("<w:tc>")
    rong = kit.RONG_ROUTE_THAY_DOI[5]
    chu = kit._run(kit._ARIAL + kit._DAM + f'<w:color w:val="{kit.MAU_THAY_DOI[loai]}"/>' + kit._co(17),
                   f"{loai} · v{PHIEN_BAN}")
    dong_moi = dong[:o_cuoi] + kit._o(rong, chu) + "</w:tr>"
    bang = bang.replace(dong, dong_moi, 1)
    tl.xml = tl.xml[:dau] + kit.to_lai_nen(bang) + tl.xml[cuoi:]


def ghi_thay_doi(tl, muc, ten, markup):
    """Mỗi mục chỉ 1 dòng ghi thay đổi: thay dòng của bản trước nếu có, không thì thêm dưới dòng Quyền truy cập."""
    dau, cuoi = _pham_vi_muc(tl, f"{muc}  {ten}")
    dong = kit.doan(f"**Thay đổi ở v{PHIEN_BAN}:** " + markup)
    cu = tl.xml.find(">Thay đổi ở v2.1:</w:t>", dau, cuoi)
    if cu >= 0:
        tl.thay_doan("Thay đổi ở v2.1:", dong, tu=dau)
    else:
        tl.chen_sau_doan("Quyền truy cập:", dong, tu=dau)


def sua(tl):
    # --- Trang đầu và mục 1 ---
    vi_tri = tl.xml.index(">2.1 — 13/10/2026</w:t>")  # ô Phiên bản của trang đầu (đứng trước bảng lịch sử)
    tl.xml = tl.xml[:vi_tri] + f">{PHIEN_BAN} — {NGAY}</w:t>" + tl.xml[vi_tri + len(">2.1 — 13/10/2026</w:t>"):]
    tl.chen_sau_doan("Từ bản 2.1:", kit.doan(
        f"**Từ bản {PHIEN_BAN}:** kết quả khám có dấu \"đã chỉnh sửa\" và dòng đơn thuốc theo danh mục thuốc của "
        "quản trị viên (4.27); chi tiết bác sĩ có giấy phép hành nghề, ngôn ngữ khám, giải thưởng, nghiên cứu (4.12). "
        "Các route của bác sĩ và quản trị viên đi kèm (kê đơn, sửa hồ sơ bệnh án có lý do và lịch sử sửa, hàng đợi "
        "khám, bác sĩ tự sửa hồ sơ) có ở `docs/BAN-GIAO-FRONTEND-05-10.md` trong mã nguồn backend."))
    tl.them_dong(MUC_1, "LỊCH SỬ PHIÊN BẢN", kit.dong_bang(RONG_PHIEN_BAN, LICH_SU))

    # --- 3.7 ---
    tl.them_dong("3.7 Giá trị enum", None, kit.dong_bang(p2_quy_uoc.RONG_ENUM, DUONG_DUNG))

    # --- Mô hình 4.12, 4.27 ---
    them_truong(tl, "4.12 BacSiChiTietResponse", BAC_SI_MOI, "linhVucKhamChua")
    tl.thay_noi_dung(
        "Chi tiết công khai của 1 bác sĩ: mọi trường của ",
        "Chi tiết công khai của 1 bác sĩ: mọi trường của `BacSiResponse` và các trường dưới đây. Nhận xét của bệnh "
        "nhân không công khai. Giấy phép hành nghề (số, ngày cấp, nơi cấp, phạm vi) công khai để người bệnh tra cứu.")
    them_truong(tl, "4.27 KetQuaKhamResponse", KET_QUA_MOI, "donThuoc")
    tl.thay_noi_dung(
        "Kết quả 1 lượt khám do bác sĩ ghi.",
        "Kết quả 1 lượt khám do bác sĩ ghi. Bác sĩ sửa được kết quả trong 7 ngày kể từ lần lưu đầu (mỗi lần sửa cần "
        "lý do, được lưu ở lịch sử sửa mà bệnh nhân không xem được); bệnh nhân chỉ thấy `ngaySuaCuoi`. **Ngoại lệ của "
        "quy ước 3.1:** `phienBan` và `hanSua` không có mặt trong JSON (không phải `null`) ở mọi route của tài liệu "
        "này; frontend không dùng 2 trường đó.")
    tl.them_dong("4.27 KetQuaKhamResponse", " — phần tử của ",
                 kit.dong_bang(DONG_RONG, (f"`{THUOC_DAU[0]}`", THUOC_DAU[1], THUOC_DAU[2])), sau_dong_chua="Trường")
    them_truong(tl, "4.27 KetQuaKhamResponse", THUOC_SAU_TEN, "tenThuoc", nhan_bang=" — phần tử của ")

    # --- JSON mẫu ---
    sua_json_mau(tl, "11.4  Chi tiết bác sĩ", doi_bac_si)
    for muc, ten, _ in CAP_NHAT[1:]:
        sua_json_mau(tl, f"{muc}  {ten}", doi_moi_ket_qua)

    # --- Dòng "Thay đổi ở v2.2" và cột "Thay đổi" ---
    for muc, ten, markup in CAP_NHAT:
        ghi_thay_doi(tl, muc, ten, markup)
        danh_dau_route(tl, muc, "Cập nhật")
