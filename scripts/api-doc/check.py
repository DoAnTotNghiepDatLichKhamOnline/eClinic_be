"""Kiểm tra tài liệu API (.docx) sau khi sửa:
1. word/document.xml là XML hợp lệ.
2. Mọi liên kết nội bộ (w:anchor) trỏ tới 1 bookmark có thật; tên và id bookmark không trùng.
3. Mỗi dòng của bảng route ở mục 2 có 1 mục chi tiết cùng method + đường dẫn, và ngược lại.
4. Các route của identity / catalog / booking / notification / chatbot-service trong tài liệu khớp với @*Mapping của các controller phía
   người dùng của tài liệu (CONTROLLER_TRONG_TAI_LIEU); controller của bác sĩ, quản trị viên, nội bộ không tính.
5. Mỗi mô hình response từ 4.10 trở đi có đúng các trường của record Java cùng tên (kể cả record lồng).
6. JSON mẫu từ mục 11 trở đi: đọc được; mỗi object trong response mẫu có đúng các trường của 1 record response,
   mỗi object trong request mẫu chỉ dùng trường của 1 record request.
7. Mã lỗi: mỗi maLoi trong các bảng lỗi có trong MaLoi.java (hoặc là mã của api-gateway ở 3.4) với đúng HTTP status;
   mọi mã của MaLoi.java được nhắc trong thân bài có 1 dòng ở Phụ lục A; mỗi dòng Phụ lục A khớp HTTP status của
   MaLoi.java và thông điệp tiếng Việt ở thong-diep/*.properties (khoá loi.<MA_LOI>).
8. Cột "Thay đổi" của bảng route (từ bản 2.1): ô để trống, hoặc đúng dạng "Mới · v<x.y>" / "Cập nhật · v<x.y>" với
   phiên bản không lớn hơn phiên bản ở trang đầu; mục chi tiết của route được đánh dấu có đúng 1 dòng
   "Mới ở v<x.y>: ..." / "Thay đổi ở v<x.y>: ..." cùng phiên bản, và route không đánh dấu thì không có dòng đó.

    python scripts/api-doc/check.py [tep.docx]
"""

import json
import re
import sys
from http import HTTPStatus
from itertools import combinations
from pathlib import Path
from xml.dom import minidom

import kit

GOC = Path(__file__).resolve().parents[2]
GOI_JAVA = "{0}-service/src/main/java/iuh/fit/se/eclinic/{0}"
# (service, controller, các method được tính hoặc None = mọi method, các route được tính hoặc None = mọi route).
# Không có ở đây: controller nội bộ (NoiBo*), của bác sĩ và của quản trị viên.
CONTROLLER_TRONG_TAI_LIEU = (
    ("identity", "XacThucController", None, None),
    ("identity", "HoSoCaNhanController", None, None),
    ("identity", "AnhDaiDienController", None, None),
    ("identity", "DoiEmailController", None, None),
    ("identity", "PhienDangNhapController", None, None),
    ("identity", "QuanLyTaiKhoanController", None, None),
    ("catalog", "ChuyenKhoaController", ("GET",), None),  # thêm / sửa / xoá là của quản trị viên
    ("catalog", "BacSiController", None, None),
    ("booking", "KhungGioController", None, None),
    ("booking", "PhieuKhamController", None, None),
    ("booking", "ThongTinDatLichController", None, None),
    ("booking", "LichHenController", None, None),
    ("booking", "DanhGiaCuaToiController", None, None),
    ("booking", "LichSuKhamController", None, None),
    ("booking", "HoSoBenhNhanController", None, None),
    ("booking", "TrangCaNhanController", None, None),
    # Từ bản 2.5 (DOANTOTNGH-14): quy tắc đặt lịch, ngày nghỉ, sổ khám bệnh, khách xin cấp lại link (đều công khai)
    ("booking", "QuyTacDatLichController", None, None),
    ("booking", "NgayNghiController", None, None),
    ("booking", "SoKhamController", None, None),
    ("booking", "CapLaiLienKetController", None, None),
    ("notification", "ThongBaoController", None, None),
    ("chatbot", "ChatbotController", None, None),
)
TIEN_TO = {"identity": ("/api/auth", "/api/users"), "catalog": ("/api/catalog",), "booking": ("/api/booking",),
           "notification": ("/api/notification",), "chatbot": ("/api/chatbot",)}
MUC_ROUTE_DAT_LICH = "[H1] 11. "
KHOA_TRANG = ["noiDung", "trang", "kichThuoc", "tongSoPhanTu", "tongSoTrang"]
METHOD = "GET|POST|PUT|DELETE|PATCH"
THU_MUC_RESPONSE = [GOC / d for d in (
    "catalog-service/src/main/java/iuh/fit/se/eclinic/catalog/dto/response",
    "booking-service/src/main/java/iuh/fit/se/eclinic/booking/dto/response",
    "notification-service/src/main/java/iuh/fit/se/eclinic/notification/dto/response",
    "chatbot-service/src/main/java/iuh/fit/se/eclinic/chatbot/dto/response")]
TEP_MA_LOI = GOC / "common/src/main/java/iuh/fit/se/eclinic/common/exception/MaLoi.java"
THU_MUC_THONG_DIEP = GOC / "common/src/main/resources/thong-diep"
MA_LOI_GATEWAY = {"DICH_VU_KHONG_KHA_DUNG": 503, "DICH_VU_PHAN_HOI_QUA_LAU": 504, "LOI_KET_NOI_DICH_VU": 502,
                  "YEU_CAU_QUA_LON": 413}  # api-gateway tự trả, không nằm trong MaLoi.java
PHU_LUC_A = "[H1] Phụ lục A"
MO_HINH_DAU_TIEN = 10  # 4.1-4.9 là mô hình của identity-service, viết gộp trường nên không so tự động


def ma_loi_trong_code():
    """{mã: (HTTP status, thông điệp tiếng Việt mặc định)} theo thứ tự khai báo trong MaLoi.java.
    HTTP status lấy từ MaLoi.java, thông điệp từ khoá loi.<MA_LOI> của các file tiếng Việt trong thong-diep/."""
    thong_diep = {}
    for tep in sorted(THU_MUC_THONG_DIEP.glob("*.properties")):
        if not tep.stem.endswith("_en"):
            thong_diep.update(re.findall(r"^loi\.([A-Z][A-Z_0-9]+)=(.*)$", tep.read_text(encoding="utf-8"), re.M))
    ket_qua = {}
    for ten, status in re.findall(r"^\s+([A-Z][A-Z_0-9]+)\(HttpStatus\.(\w+)\)", TEP_MA_LOI.read_text(encoding="utf-8"),
                                  re.M):
        assert ten in thong_diep, f"thong-diep/*.properties thiếu khoá loi.{ten}"
        ket_qua[ten] = (HTTPStatus[status].value, thong_diep[ten].strip().replace("''", "'"))
    return ket_qua


def kiem_tra_ma_loi(chu):
    """Trả (số mã trong các bảng lỗi, số dòng Phụ lục A, danh sách lỗi)."""
    loi, code = [], ma_loi_trong_code()
    than, phu_luc = chu[:chu.index(PHU_LUC_A)], chu[chu.index(PHU_LUC_A):chu.index("[H1] Phụ lục B")]
    trong_bang = set(re.findall(r"^\| (\d{3}) \| ([A-Z][A-Z_0-9]+) \|", than, re.M))
    nhac_kem_status = set(re.findall(r"\b(\d{3}) ([A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+)\b", than))
    for status, ma in sorted(trong_bang | nhac_kem_status):
        dung = code[ma][0] if ma in code else MA_LOI_GATEWAY.get(ma)
        if dung is None:
            if (status, ma) in trong_bang:
                loi.append(f"Mã lỗi {ma} trong bảng lỗi không có trong MaLoi.java")
        elif dung != int(status):
            loi.append(f"Mã lỗi {ma} ghi HTTP {status}, MaLoi.java là {dung}")
    dong_a = re.findall(r"^\| ([A-Z][A-Z_0-9]+) \| (\d{3}) \| (.*) \|$", phu_luc, re.M)
    co_o_a = [ma for ma, _, _ in dong_a]
    for ma in code:
        if re.search(rf"\b{ma}\b", than) and ma not in co_o_a:
            loi.append(f"Mã lỗi {ma} được nhắc trong tài liệu nhưng không có ở Phụ lục A")
    for ma, status, thong_diep in dong_a:
        if ma not in code:
            loi.append(f"Phụ lục A có mã {ma} không có trong MaLoi.java")
        elif (int(status), thong_diep) != code[ma]:
            loi.append(f"Phụ lục A, dòng {ma}: HTTP hoặc thông điệp khác MaLoi.java {code[ma]}")
    if len(co_o_a) != len(set(co_o_a)):
        loi.append("Phụ lục A có mã lỗi bị lặp")
    return len({ma for _, ma in trong_bang}), len(dong_a), loi


def kiem_tra_thay_doi(chu):
    """Trả ({mục: nhãn} của các route được đánh dấu ở bảng route, danh sách lỗi)."""
    loi, danh_dau = [], {}
    phien_ban = tuple(int(x) for x in re.search(r"^\| Phiên bản \| (\d+)\.(\d+) ", chu, re.M).groups())
    dau = chu.index("[H1] 2. Danh sách route")
    bang = chu[dau:chu.index("[H1] 3. ", dau)]
    for muc, o in re.findall(rf"^\| ([\d.]+) \| (?:{METHOD}) \| /\S+ \| [^|]* \| [^|]* \|(?: ([^|]*) \|)?$", bang, re.M):
        o = o.strip()
        if not o:
            continue
        m = re.fullmatch(r"(Mới|Cập nhật) · v(\d+)\.(\d+)", o)
        if not m:
            loi.append(f"Bảng route, mục {muc}: ô Thay đổi sai dạng: {o!r}")
        elif (int(m.group(2)), int(m.group(3))) > phien_ban:
            loi.append(f"Bảng route, mục {muc}: {o} lớn hơn phiên bản của tài liệu")
        else:
            danh_dau[muc] = o
    ghi_chu = {}
    for m in re.finditer(r"^\[H3\] ([\d.]+)  [^\n]*\n(.*?)(?=^\[H[123]\] |\Z)", chu, re.M | re.S):
        dong = re.findall(r"^(Mới|Thay đổi) ở v(\d+\.\d+): ", m.group(2), re.M)
        if len(dong) > 1:
            loi.append(f"Mục {m.group(1)} có hơn 1 dòng ghi thay đổi")
        elif dong:
            ghi_chu[m.group(1)] = f"{'Mới' if dong[0][0] == 'Mới' else 'Cập nhật'} · v{dong[0][1]}"
    for muc in sorted(set(danh_dau) | set(ghi_chu)):
        if danh_dau.get(muc) != ghi_chu.get(muc):
            loi.append(f"Mục {muc}: bảng route ghi {danh_dau.get(muc)!r}, mục chi tiết ghi {ghi_chu.get(muc)!r}")
    return danh_dau, loi


def route_trong_code(service):
    ket_qua = set()
    for cua, ten, cac_method, cac_route in CONTROLLER_TRONG_TAI_LIEU:
        if cua != service:
            continue
        ma = (GOC / GOI_JAVA.format(service) / "controller" / f"{ten}.java").read_text(encoding="utf-8")
        goc = re.search(r'@RequestMapping\("([^"]+)"\)', ma).group(1)
        for method, duoi in re.findall(r'@(Get|Post|Put|Delete|Patch)Mapping(?:\((?:value\s*=\s*)?"([^"]*)")?', ma):
            route = (method.upper(), goc + (duoi or ""))
            if (cac_method is None or route[0] in cac_method) and (cac_route is None or route in cac_route):
                ket_qua.add(route)
    return ket_qua


def cac_record(loai):
    """Danh sách trường của mọi record (kể cả record lồng) trong dto/<loai> của catalog, booking, notification."""
    ket_qua = []
    for service in ("catalog", "booking", "notification", "chatbot"):
        for tep in sorted((GOC / GOI_JAVA.format(service) / "dto" / loai).glob("*.java")):
            ma = tep.read_text(encoding="utf-8")
            for ten in re.findall(r"\brecord (\w+)\(", ma):
                truong = truong_cua_record(ma, ten)
                ket_qua.append(truong)
                # Trường @JsonInclude(NON_NULL) vắng hẳn khỏi JSON khi null: mẫu thiếu bất kỳ nhóm nào trong các trường
                # đó cũng khớp (vd kết quả khám của bệnh nhân có maSoKham / linkSoKham nhưng không có phienBan / hanSua)
                bo_duoc = sorted(truong_khong_null(ma, ten))
                for so in range(1, len(bo_duoc) + 1):
                    for nhom in combinations(bo_duoc, so):
                        ket_qua.append([t for t in truong if t not in nhom])
    return ket_qua


def truong_khong_null(ma, ten):
    """Tên các thành phần của `record ten(...)` có @JsonInclude(... NON_NULL): vắng khỏi JSON khi giá trị null."""
    m = re.search(rf"\brecord {ten}\(", ma)
    vi_tri, sau = m.end(), 1
    while sau:
        sau += {"(": 1, ")": -1}.get(ma[vi_tri], 0)
        vi_tri += 1
    than = re.sub(r"/\*.*?\*/|//[^\n]*", "", ma[m.end():vi_tri - 1], flags=re.S)
    return {re.sub(r"@\w+(\([^()]*\))?", "", phan).split()[-1]
            for phan in re.split(r",(?![^<]*>)(?![^()]*\))", than) if "NON_NULL" in phan}


def cac_object(gia_tri):
    """Mọi object JSON nằm trong gia_tri; khung phân trang (3.3) chỉ lấy các phần tử của noiDung."""
    if isinstance(gia_tri, list):
        for phan_tu in gia_tri:
            yield from cac_object(phan_tu)
    elif isinstance(gia_tri, dict):
        if list(gia_tri) == KHOA_TRANG:
            yield from cac_object(gia_tri["noiDung"])
            return
        yield gia_tri
        for con in gia_tri.values():
            yield from cac_object(con)


def kiem_tra_json_mau(chu):
    """Trả (số JSON mẫu đã kiểm, danh sách lỗi) cho các mục route đặt lịch."""
    loi, so = [], 0
    if MUC_ROUTE_DAT_LICH not in chu:
        return so, loi
    response, request = cac_record("response"), cac_record("request")
    muc, loai, khoi = "", None, None
    for dong in chu[chu.index(MUC_ROUTE_DAT_LICH):chu.index(PHU_LUC_A)].split("\n"):
        if khoi is not None:
            khoi.append(dong)
            if dong != "}":
                continue
            so += 1
            try:
                mau = json.loads("\n".join(khoi))
            except ValueError as ex:
                loi.append(f"JSON mẫu ở {muc} không đọc được: {ex}")
                mau = None
            khoi = None
            if loai == "response" and mau is not None:
                for obj in cac_object(mau.get("duLieu")):
                    if list(obj) not in response:
                        loi.append(f"Response mẫu ở {muc}: object {list(obj)} không khớp record response nào")
            elif loai == "request" and mau is not None:
                for obj in cac_object(mau):
                    if not any(set(obj) <= set(r) for r in request):
                        loi.append(f"Request mẫu ở {muc}: object {list(obj)} có trường không thuộc record request nào")
        elif dong.startswith("[H3] "):
            muc, loai = dong[5:].split("  ")[0], None
        elif dong.startswith("REQUEST BODY"):
            loai = "request"
        elif dong.startswith("RESPONSE THÀNH CÔNG"):
            loai = "response"
        elif dong == "{":
            khoi = [dong]
    return so, loi


def truong_cua_record(ma, ten):
    """Tên các thành phần của `record ten(...)` trong mã Java, theo thứ tự khai báo; None nếu không có record đó."""
    m = re.search(rf"\brecord {ten}\(", ma)
    if not m:
        return None
    vi_tri, sau = m.end(), 1
    while sau:
        sau += {"(": 1, ")": -1}.get(ma[vi_tri], 0)
        vi_tri += 1
    than = re.sub(r"/\*.*?\*/|//[^\n]*", "", ma[m.end():vi_tri - 1], flags=re.S)
    than = re.sub(r"@\w+(\([^()]*\))?", "", than)
    return [phan.split()[-1] for phan in re.split(r",(?![^<]*>)", than) if phan.strip()]


def kiem_tra_mo_hinh(chu):
    """So bảng của từng mô hình (và record lồng, có dòng nhãn "Ten — ..." ngay trước bảng) với record Java.
    Trả (số mô hình, danh sách lỗi)."""
    loi, so = [], 0
    muc_4 = chu[chu.index("[H1] 4. "):chu.index("[H1] 5. ")]
    for m in re.finditer(r"^\[H2\] 4\.(\d+) (\w+)\n(.*?)(?=^\[H2\] |\Z)", muc_4, re.M | re.S):
        if int(m.group(1)) < MO_HINH_DAU_TIEN:
            continue
        so += 1
        muc, ten = f"4.{m.group(1)}", m.group(2)
        tep = [d / f"{ten}.java" for d in THU_MUC_RESPONSE if (d / f"{ten}.java").exists()]
        if not tep:
            loi.append(f"Mô hình {muc} {ten}: không có record này trong mã nguồn")
            continue
        ma = tep[0].read_text(encoding="utf-8")
        record, truong = ten, None
        for dong in m.group(3).split("\n") + [""]:
            if dong.startswith("| Trường |"):
                truong = []
            elif dong.startswith("| ") and truong is not None:
                truong += [t.strip() for t in dong.split("|")[1].split(",")]
            else:
                if truong is not None:
                    trong_code = truong_cua_record(ma, record)
                    if trong_code is None:
                        loi.append(f"Mô hình {muc} {ten}: không có record {record} trong {tep[0].name}")
                    elif truong != trong_code:
                        thieu = [t for t in trong_code if t not in truong]
                        thua = [t for t in truong if t not in trong_code]
                        loi.append(f"Mô hình {muc} {record} lệch record Java: thiếu {thieu}, thừa {thua}"
                                   if thieu or thua else f"Mô hình {muc} {record}: sai thứ tự trường")
                    truong = None
                nhan_con = re.match(r"(\w+) — ", dong)
                if nhan_con:
                    record = nhan_con.group(1)
    return so, loi


def kiem_tra(duong_dan):
    loi = []
    tai_lieu = kit.TaiLieu(duong_dan)
    xml = tai_lieu.xml

    minidom.parseString(xml.encode("utf-8"))
    minidom.parseString(tai_lieu.tep[kit.TEP_DANH_SO])

    ten = re.findall(r'<w:bookmarkStart w:name="([^"]+)"', xml)
    ma = re.findall(r'<w:bookmarkStart [^>]*w:id="(\d+)"', xml)
    if len(ten) != len(set(ten)):
        loi.append("Tên bookmark bị trùng")
    if len(ma) != len(set(ma)):
        loi.append("Id bookmark bị trùng")
    for neo in re.findall(r'w:anchor="([^"]+)"', xml):
        if neo not in ten:
            loi.append(f"Liên kết trỏ tới bookmark không tồn tại: {neo}")
    # Liên kết ở bảng route phải rơi đúng mục có cùng đường dẫn (bảng method nằm ngay dưới heading mang bookmark)
    for neo, dich in re.findall(r'<w:hyperlink [^>]*w:anchor="([^"]+)">.*?<w:t[^>]*>([^<]*)</w:t>', xml, re.S):
        vi_tri = xml.find(f'<w:bookmarkStart w:name="{neo}"')
        if vi_tri < 0:
            continue
        bang_method = xml[vi_tri:xml.index("</w:tbl>", vi_tri)]
        if f">{dich}</w:t>" not in bang_method:
            loi.append(f"Liên kết {dich} nhảy tới mục không cùng đường dẫn (bookmark {neo})")
    for so in set(re.findall(r'<w:numId w:val="(\d+)"', xml)):
        if f'<w:num w:numId="{so}"'.encode() not in tai_lieu.tep[kit.TEP_DANH_SO]:
            loi.append(f"Danh sách dùng numId không có định nghĩa: {so}")

    chu = kit.dump(duong_dan)
    dau_muc_2 = chu.index("[H1] 2. Danh sách route")
    bang_route = set(re.findall(rf"^\| [\d.]+ \| ({METHOD}) \| (/\S+) \|", chu[dau_muc_2:], re.M))
    chi_tiet = set(re.findall(rf"^\[H3\] .*\n\| ({METHOD}) \| (/\S+) \|$", chu, re.M))
    for thieu in sorted(bang_route - chi_tiet):
        loi.append(f"Có trong bảng route nhưng không có mục chi tiết: {thieu[0]} {thieu[1]}")
    for thieu in sorted(chi_tiet - bang_route):
        loi.append(f"Có mục chi tiết nhưng không có trong bảng route: {thieu[0]} {thieu[1]}")

    dong_route = []
    for service, tien_to in TIEN_TO.items():
        code = route_trong_code(service)
        trong_tai_lieu = {r for r in chi_tiet if r[1].startswith(tien_to)}
        for thieu in sorted(code - trong_tai_lieu):
            loi.append(f"Route có trong code nhưng chưa có trong tài liệu: {thieu[0]} {thieu[1]}")
        for thua in sorted(trong_tai_lieu - code):
            loi.append(f"Route có trong tài liệu nhưng không có trong code: {thua[0]} {thua[1]}")
        dong_route.append(f"Route {service}-service: tài liệu {len(trong_tai_lieu & code)} / code {len(code)}")

    so_mo_hinh, loi_mo_hinh = kiem_tra_mo_hinh(chu)
    loi += loi_mo_hinh
    so_mau, loi_mau = kiem_tra_json_mau(chu)
    loi += loi_mau
    so_ma, so_dong_a, loi_ma = kiem_tra_ma_loi(chu)
    loi += loi_ma
    danh_dau, loi_thay_doi = kiem_tra_thay_doi(chu)
    loi += loi_thay_doi

    print(f"Bookmark: {len(ten)} · liên kết nội bộ: {len(re.findall('w:anchor=', xml))}")
    print(f"Bảng route: {len(bang_route)} · mục chi tiết: {len(chi_tiet)}")
    print("\n".join(dong_route))
    print(f"Mô hình từ 4.{MO_HINH_DAU_TIEN}: {so_mo_hinh} mô hình, {len(loi_mo_hinh)} lỗi")
    print(f"JSON mẫu từ mục 11: {so_mau} mẫu, {len(loi_mau)} lỗi")
    print(f"Mã lỗi: {so_ma} mã trong các bảng lỗi, {so_dong_a} dòng Phụ lục A, {len(loi_ma)} lỗi")
    moi = sorted((m for m, o in danh_dau.items() if o.startswith("Mới")), key=lambda m: [int(x) for x in m.split(".")])
    cap_nhat = sorted((m for m, o in danh_dau.items() if o.startswith("Cập")), key=lambda m: [int(x) for x in m.split(".")])
    print(f"Cột Thay đổi: mới {len(moi)} ({', '.join(moi) or '-'}) · cập nhật {len(cap_nhat)} ({', '.join(cap_nhat) or '-'})"
          f" · {len(loi_thay_doi)} lỗi")
    return loi


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    cac_loi = kiem_tra(sys.argv[1] if len(sys.argv) > 1 else str(GOC / kit.TAI_LIEU))
    for dong in cac_loi:
        print("LỖI:", dong)
    print("ĐẠT" if not cac_loi else f"KHÔNG ĐẠT ({len(cac_loi)} lỗi)")
    sys.exit(1 if cac_loi else 0)
