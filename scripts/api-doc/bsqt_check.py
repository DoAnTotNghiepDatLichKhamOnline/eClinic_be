"""Kiểm tra tài liệu API thứ 2 (lịch hẹn và khám bệnh phía bác sĩ và quản trị viên) sau khi dựng:
1. word/document.xml là XML hợp lệ.
2. Mọi liên kết nội bộ (w:anchor) trỏ tới 1 bookmark có thật và rơi đúng mục cùng đường dẫn; tên và id bookmark không
   trùng.
3. Mỗi dòng của bảng route ở mục 2 có 1 mục chi tiết cùng method + đường dẫn, và ngược lại.
4. Route trong tài liệu là route có thật của 5 controller trong phạm vi (CONTROLLER); route /api/booking, /api/medical
   ngoài 5 controller đó là lỗi. Khi DAY_DU = True (từ giai đoạn 5): mọi route của 5 controller phải có trong tài liệu.
5. Mỗi mô hình ở mục 4 (từ 4.1) có đúng các trường, đúng thứ tự, của record Java cùng tên trong dto/response của
   booking-service hoặc medical-service, kể cả record lồng (dòng "Ten — ..." ngay trước bảng).
6. Mọi chỗ dẫn "TK x.y" (TK 3.4, TK 5-10, TK Phụ lục A...) trỏ tới 1 mục có thật của TK (eClinic_API_XacThuc_TaiKhoan.docx).
7. Không lặp TK: không mô hình nào ở mục 4 trùng tên mô hình ở mục 4 của TK; không bảng record lồng nào là record TK đã
   có bảng, hoặc là bản giống hệt (cùng tên, cùng trường) của record lồng ở TK. Record lồng chỉ dẫn TK (GIONG_TK) phải
   có đúng các trường của record ở TK.
8. Mỗi JSON mẫu sau nhãn RESPONSE THÀNH CÔNG (từ mục 5) khớp cấu trúc `duLieu` của ít nhất 1 response 2xx thật của cùng
   route trong temp/api-doc/mau-bsqt (bsqt_lay_mau.py): đúng các trường, đúng thứ tự (so_mau.lech, chặt). Route chưa có
   response thật thì chỉ báo là chưa so, không tính lỗi; thư mục trống thì bỏ qua kiểm tra này.
9. Mã lỗi (thân bài = mọi phần ngoài Phụ lục A của tài liệu này): mỗi maLoi trong các bảng lỗi và mỗi "NNN MA_LOI" có trong
   MaLoi.java (hoặc là mã của api-gateway) với đúng HTTP status. Mọi mã của MaLoi.java được nhắc trong thân bài có 1
   dòng ở Phụ lục A, hoặc, nếu TK Phụ lục A đã có dòng của mã đó, được nêu tên ở Phụ lục A (dẫn TK, không lặp). Mỗi dòng
   Phụ lục A: mã có trong MaLoi.java, đúng HTTP status và thông điệp tiếng Việt, không phải mã TK Phụ lục A đã có, được
   nhắc trong thân bài, không lặp.

Các hàm của check.py (tài liệu thứ nhất) được dùng lại khi nhận tham số; phần dựa vào hằng số của tài liệu thứ nhất
viết lại ở đây. check.py không bị sửa.

    python scripts/api-doc/bsqt_check.py [tep.docx]
"""

import re
import sys
from xml.dom import minidom

import check
import kit
import so_mau

GOC = check.GOC
TAI_LIEU = "eClinic_API_LichHen_KhamBenh_BacSi_QuanTri.docx"
TAI_LIEU_TK = GOC / kit.TAI_LIEU
# Cả 5 controller nằm trọn trong phạm vi: 9 + 4 + 2 route của booking-service, 6 + 1 của medical-service
CONTROLLER = (
    ("booking", "LichCuaBacSiController"),
    ("booking", "HangDoiController"),
    ("booking", "QuanTriLichHenController"),
    ("medical", "KhamBenhController"),
    ("medical", "QuanTriBenhAnController"),
)
TIEN_TO = ("/api/booking/", "/api/medical/")
THU_MUC_RESPONSE = [GOC / check.GOI_JAVA.format(s) / "dto" / "response" for s in ("booking", "medical")]
DAY_DU = True  # tài liệu phải có đủ 23 route của 5 controller
# Record lồng của tài liệu này chỉ dẫn TK thay vì có bảng: (record cha, record lồng) ở đây = (record cha, record lồng) ở TK
GIONG_TK = (
    (("BenhAnResponse", "SinhHieu"), ("KetQuaKhamResponse", "SinhHieu")),
    (("BenhAnResponse", "DonThuoc"), ("KetQuaKhamResponse", "DonThuoc")),
)
THU_MUC_MAU = GOC / "temp/api-doc/mau-bsqt"
MAU_DAN_TK = r"\bTK ((?:Phụ lục [A-Z])|(?:[A-Z]\.\d+)|(?:\d+(?:\.\d+)?(?:-\d+(?:\.\d+)?)?))"


def route_trong_code():
    ket_qua = set()
    for service, ten in CONTROLLER:
        ma = (GOC / check.GOI_JAVA.format(service) / "controller" / f"{ten}.java").read_text(encoding="utf-8")
        goc = re.search(r'@RequestMapping\("([^"]+)"\)', ma).group(1)
        for method, duoi in re.findall(r'@(Get|Post|Put|Delete|Patch)Mapping(?:\((?:value\s*=\s*)?"([^"]*)")?', ma):
            ket_qua.add((method.upper(), goc + (duoi or "")))
    return ket_qua


def tep_record(ten, cac_thu_muc):
    """File .java của record cấp ngoài cùng tên `ten` trong các thư mục; None nếu không có."""
    for thu_muc in cac_thu_muc:
        tep = thu_muc / f"{ten}.java"
        if tep.exists():
            return tep
    return None


def cac_bang_mo_hinh(chu):
    """Đọc mục 4 của 1 bản dump: [(mục, mô hình, record, [trường])], record là tên mô hình hoặc tên record lồng."""
    dau = chu.index("[H1] 4. ")
    sau = re.search(r"^\[H1\] ", chu[dau + 1:], re.M)
    muc_4 = chu[dau:dau + 1 + sau.start()] if sau else chu[dau:]
    ket_qua = []
    for m in re.finditer(r"^\[H2\] (4\.\d+) (\w+)\n(.*?)(?=^\[H[12]\] |\Z)", muc_4, re.M | re.S):
        record, truong = m.group(2), None
        for dong in m.group(3).split("\n") + [""]:
            if dong.startswith("| Trường |"):
                truong = []
            elif dong.startswith("| ") and truong is not None:
                truong += [t.strip() for t in dong.split("|")[1].split(",")]
            else:
                if truong is not None:
                    ket_qua.append((m.group(1), m.group(2), record, truong))
                    truong = None
                nhan_con = re.match(r"(\w+) — ", dong)
                if nhan_con:
                    record = nhan_con.group(1)
    return ket_qua


def kiem_tra_mo_hinh(chu):
    """Check 5. Trả (số mô hình, danh sách lỗi)."""
    loi, mo_hinh = [], set()
    for muc, ten, record, truong in cac_bang_mo_hinh(chu):
        mo_hinh.add(muc)
        tep = tep_record(ten, THU_MUC_RESPONSE)
        if not tep:
            loi.append(f"Mô hình {muc} {ten}: không có record này trong dto/response của booking, medical")
            continue
        trong_code = check.truong_cua_record(tep.read_text(encoding="utf-8"), record)
        if trong_code is None:
            loi.append(f"Mô hình {muc} {ten}: không có record {record} trong {tep.name}")
        elif truong != trong_code:
            thieu = [t for t in trong_code if t not in truong]
            thua = [t for t in truong if t not in trong_code]
            loi.append(f"Mô hình {muc} {record} lệch record Java: thiếu {thieu}, thừa {thua}"
                       if thieu or thua else f"Mô hình {muc} {record}: sai thứ tự trường")
    if sorted(mo_hinh, key=lambda m: int(m.split(".")[1])) != [f"4.{i}" for i in range(1, len(mo_hinh) + 1)]:
        loi.append(f"Mô hình ở mục 4 không đánh số liền từ 4.1: {sorted(mo_hinh)}")
    return len(mo_hinh), loi


def kiem_tra_dan_tk(chu, chu_tk):
    """Check 6. Trả (số chỗ dẫn, danh sách lỗi)."""
    loi, so = [], 0
    dong_tk = chu_tk.split("\n")

    def co_muc(muc):
        if muc.startswith("Phụ lục"):
            dau = f"[H1] {muc}"
        elif re.fullmatch(r"\d+", muc):
            dau = f"[H1] {muc}. "
        else:
            return any(d.startswith((f"[H2] {muc} ", f"[H3] {muc}  ")) for d in dong_tk)
        return any(d.startswith(dau) for d in dong_tk)

    for dan in re.findall(MAU_DAN_TK, chu):
        so += 1
        for muc in dan.split("-") if not dan.startswith("Phụ lục") else [dan]:
            if not co_muc(muc):
                loi.append(f"Dẫn \"TK {dan}\" nhưng TK không có mục {muc}")
    return so, loi


def kiem_tra_khong_lap(chu, chu_tk):
    """Check 7. Trả danh sách lỗi."""
    loi = []
    thu_muc_tk = sorted(GOC.glob("*-service/src/main/java/iuh/fit/se/eclinic/*/dto/response"))
    bang_tk = cac_bang_mo_hinh(chu_tk)
    ten_tk = {ten for _, ten, _, _ in bang_tk}
    record_tk = {(tep_record(ten, thu_muc_tk), record) for _, ten, record, _ in bang_tk}
    long_tk = {(record, tuple(truong)) for _, ten, record, truong in bang_tk if record != ten}
    for muc, ten, record, truong in cac_bang_mo_hinh(chu):
        if record == ten:
            if ten in ten_tk:
                loi.append(f"Mô hình {muc} {ten} đã có ở mục 4 của TK: dẫn TK thay vì viết lại")
        elif (tep_record(ten, THU_MUC_RESPONSE), record) in record_tk:
            loi.append(f"Mô hình {muc}: bảng {record} là record TK đã có bảng: dẫn TK thay vì viết lại")
        elif (record, tuple(truong)) in long_tk:
            loi.append(f"Mô hình {muc}: bảng {record} giống hệt bảng {record} của TK: dẫn TK thay vì viết lại")
    for (cha, con), (cha_tk, con_tk) in GIONG_TK:
        o_day = check.truong_cua_record(tep_record(cha, THU_MUC_RESPONSE).read_text(encoding="utf-8"), con)
        o_tk = check.truong_cua_record(tep_record(cha_tk, thu_muc_tk).read_text(encoding="utf-8"), con_tk)
        if o_day != o_tk:
            loi.append(f"{cha}.{con} được ghi là giống {cha_tk}.{con_tk} của TK nhưng trường khác: {o_day} / {o_tk}")
    return loi


def mau_trong_tai_lieu(chu):
    """so_mau.mau_trong_tai_lieu đọc từ mục 5 tới "Phụ lục A"; tài liệu này chưa có phụ lục ở các giai đoạn đầu, nên cắt
    từ mục 5 tới phụ lục đầu tiên (hoặc cuối tài liệu) rồi thêm 1 dòng "Phụ lục A" làm điểm dừng."""
    dau = chu.index("[H1] 5. ")
    cuoi = chu.find("[H1] Phụ lục", dau)
    return so_mau.mau_trong_tai_lieu(chu[dau:cuoi if cuoi >= 0 else len(chu)] + "\n[H1] Phụ lục A")


def kiem_tra_mau(chu):
    """Check 8. Trả (số mẫu, số đã so, route chưa có response thật, danh sách lỗi)."""
    if not any(THU_MUC_MAU.glob("*.json")):
        return 0, 0, None, []
    so_mau.THU_MUC = THU_MUC_MAU
    cac_route = sorted(set(re.findall(rf"^\[H3\] .*\n\| ({check.METHOD}) \| (/\S+) \|$", chu, re.M)))
    that = so_mau.response_that(cac_route)
    cac_mau = mau_trong_tai_lieu(chu)
    loi, da_so = [], 0
    for muc, method, duong, mau in cac_mau:
        cac_body = that[(method, duong)]
        if not cac_body:
            continue
        da_so += 1
        cac_lech = [so_mau.lech(mau.get("duLieu"), b.get("duLieu"), True) for b in cac_body]
        if ("duLieu" in mau) != any("duLieu" in b for b in cac_body):
            loi.append(f"Mẫu {muc} {method} {duong}: mẫu {'có' if 'duLieu' in mau else 'không có'} duLieu, thật thì "
                       "ngược lại")
        elif all(cac_lech):
            loi.append(f"Mẫu {muc} {method} {duong}: {cac_lech[0]}")
    chua_co = [f"{m} {d}" for m, d in cac_route if not that[(m, d)]]
    return len(cac_mau), da_so, chua_co, loi


def kiem_tra_ma_loi(chu, chu_tk):
    """Check 9 (như check.kiem_tra_ma_loi, theo bố cục của tài liệu này). Trả (số mã trong bảng lỗi, số dòng Phụ lục A,
    số mã dẫn TK, danh sách lỗi)."""
    loi, code = [], check.ma_loi_trong_code()
    if "[H1] Phụ lục A" not in chu:
        return 0, 0, 0, ["Không có Phụ lục A"]
    dau_a = chu.index("[H1] Phụ lục A")
    sau_a = re.search(r"^\[H1\] ", chu[dau_a + 1:], re.M)
    cuoi_a = dau_a + 1 + sau_a.start() if sau_a else len(chu)
    # "Thân bài": mọi phần ngoài Phụ lục A, kể cả Phụ lục B, C (luồng gọi mẫu cũng nêu mã lỗi)
    than, phu_luc = chu[:dau_a] + chu[cuoi_a:], chu[dau_a:cuoi_a]
    trong_bang = set(re.findall(r"^\| (\d{3}) \| ([A-Z][A-Z_0-9]+) \|", than, re.M))
    nhac_kem_status = set(re.findall(r"\b(\d{3}) ([A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+)\b", than))
    for status, ma in sorted(trong_bang | nhac_kem_status):
        dung = code[ma][0] if ma in code else check.MA_LOI_GATEWAY.get(ma)
        if dung is None:
            if (status, ma) in trong_bang:
                loi.append(f"Mã lỗi {ma} trong bảng lỗi không có trong MaLoi.java")
        elif dung != int(status):
            loi.append(f"Mã lỗi {ma} ghi HTTP {status}, MaLoi.java là {dung}")
    a_tk = chu_tk[chu_tk.index("[H1] Phụ lục A"):chu_tk.index("[H1] Phụ lục B")]
    co_o_tk = set(re.findall(r"^\| ([A-Z][A-Z_0-9]+) \| \d{3} \|", a_tk, re.M))
    dong_a = re.findall(r"^\| ([A-Z][A-Z_0-9]+) \| (\d{3}) \| (.*) \|$", phu_luc, re.M)
    co_o_a = [ma for ma, _, _ in dong_a]
    nhac = [ma for ma in code if re.search(rf"\b{ma}\b", than)]
    for ma in nhac:
        if ma in co_o_tk:
            if not re.search(rf"\b{ma}\b", phu_luc):
                loi.append(f"Mã lỗi {ma} được nhắc trong tài liệu, TK Phụ lục A có, nhưng Phụ lục A không nêu tên")
        elif ma not in co_o_a:
            loi.append(f"Mã lỗi {ma} được nhắc trong tài liệu nhưng không có ở Phụ lục A")
    for ma, status, thong_diep in dong_a:
        if ma not in code:
            loi.append(f"Phụ lục A có mã {ma} không có trong MaLoi.java")
            continue
        if (int(status), thong_diep) != code[ma]:
            loi.append(f"Phụ lục A, dòng {ma}: HTTP hoặc thông điệp khác MaLoi.java {code[ma]}")
        if ma in co_o_tk:
            loi.append(f"Phụ lục A, dòng {ma}: TK Phụ lục A đã có, dẫn TK thay vì viết lại")
        if ma not in nhac:
            loi.append(f"Phụ lục A, dòng {ma}: thân bài không nhắc mã này")
    if len(co_o_a) != len(set(co_o_a)):
        loi.append("Phụ lục A có mã lỗi bị lặp")
    return len({ma for _, ma in trong_bang}), len(dong_a), len([m for m in nhac if m in co_o_tk]), loi


def kiem_tra(duong_dan):
    loi = []
    tai_lieu = kit.TaiLieu(duong_dan)
    xml = tai_lieu.xml

    # 1
    minidom.parseString(xml.encode("utf-8"))
    minidom.parseString(tai_lieu.tep[kit.TEP_DANH_SO])

    # 2 (như check.kiem_tra)
    ten = re.findall(r'<w:bookmarkStart w:name="([^"]+)"', xml)
    ma = re.findall(r'<w:bookmarkStart [^>]*w:id="(\d+)"', xml)
    if len(ten) != len(set(ten)):
        loi.append("Tên bookmark bị trùng")
    if len(ma) != len(set(ma)):
        loi.append("Id bookmark bị trùng")
    for neo in re.findall(r'w:anchor="([^"]+)"', xml):
        if neo not in ten:
            loi.append(f"Liên kết trỏ tới bookmark không tồn tại: {neo}")
    for neo, dich in re.findall(r'<w:hyperlink [^>]*w:anchor="([^"]+)">.*?<w:t[^>]*>([^<]*)</w:t>', xml, re.S):
        vi_tri = xml.find(f'<w:bookmarkStart w:name="{neo}"')
        if vi_tri >= 0 and f">{dich}</w:t>" not in xml[vi_tri:xml.index("</w:tbl>", vi_tri)]:
            loi.append(f"Liên kết {dich} nhảy tới mục không cùng đường dẫn (bookmark {neo})")
    for so in set(re.findall(r'<w:numId w:val="(\d+)"', xml)):
        if f'<w:num w:numId="{so}"'.encode() not in tai_lieu.tep[kit.TEP_DANH_SO]:
            loi.append(f"Danh sách dùng numId không có định nghĩa: {so}")

    # 3
    chu = kit.dump(duong_dan)
    dau_muc_2 = chu.index("[H1] 2. Danh sách route")
    muc_2 = chu[dau_muc_2:chu.index("[H1] 3. ", dau_muc_2)]
    bang_route = set(re.findall(rf"^\| [\d.]+ \| ({check.METHOD}) \| (/\S+) \|", muc_2, re.M))
    chi_tiet = set(re.findall(rf"^\[H3\] .*\n\| ({check.METHOD}) \| (/\S+) \|$", chu, re.M))
    for thieu in sorted(bang_route - chi_tiet):
        loi.append(f"Có trong bảng route nhưng không có mục chi tiết: {thieu[0]} {thieu[1]}")
    for thieu in sorted(chi_tiet - bang_route):
        loi.append(f"Có mục chi tiết nhưng không có trong bảng route: {thieu[0]} {thieu[1]}")

    # 4
    code = route_trong_code()
    trong_tai_lieu = {r for r in chi_tiet | bang_route if r[1].startswith(TIEN_TO)}
    for thua in sorted((chi_tiet | bang_route) - code):
        loi.append(f"Route có trong tài liệu nhưng không thuộc 5 controller trong phạm vi: {thua[0]} {thua[1]}")
    if DAY_DU:
        for thieu in sorted(code - trong_tai_lieu):
            loi.append(f"Route có trong code nhưng chưa có trong tài liệu: {thieu[0]} {thieu[1]}")

    # 5
    so_mo_hinh, loi_mo_hinh = kiem_tra_mo_hinh(chu)
    loi += loi_mo_hinh

    # 6, 7
    chu_tk = kit.dump(TAI_LIEU_TK)
    so_dan, loi_dan = kiem_tra_dan_tk(chu, chu_tk)
    loi += loi_dan
    loi_lap = kiem_tra_khong_lap(chu, chu_tk)
    loi += loi_lap

    # 8
    so_mau_tl, so_da_so, chua_co, loi_mau = kiem_tra_mau(chu)
    loi += loi_mau

    # 9
    so_ma, so_dong_a, so_dan_a, loi_ma = kiem_tra_ma_loi(chu, chu_tk)
    loi += loi_ma

    print(f"Bookmark: {len(ten)} · liên kết nội bộ: {len(re.findall('w:anchor=', xml))}")
    print(f"Bảng route: {len(bang_route)} · mục chi tiết: {len(chi_tiet)}")
    print(f"Route trong phạm vi: tài liệu {len(trong_tai_lieu & code)} / code {len(code)}"
          + ("" if DAY_DU else " (chưa bắt buộc đủ)"))
    print(f"Mô hình từ 4.1: {so_mo_hinh} mô hình, {len(loi_mo_hinh)} lỗi")
    print(f"Dẫn TK: {so_dan} chỗ, {len(loi_dan)} lỗi · không lặp TK: {len(loi_lap)} lỗi")
    if chua_co is None:
        print(f"JSON mẫu: chưa có response thật ({THU_MUC_MAU.relative_to(GOC)} trống), chưa so")
    else:
        print(f"JSON mẫu: {so_mau_tl} · đã so với response thật: {so_da_so} · lệch: {len(loi_mau)} · route chưa có "
              f"response thật: {', '.join(chua_co) or 'không'}")
    print(f"Mã lỗi: {so_ma} mã trong các bảng lỗi, Phụ lục A {so_dong_a} dòng + {so_dan_a} mã dẫn TK, {len(loi_ma)} lỗi")
    return loi


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    cac_loi = kiem_tra(sys.argv[1] if len(sys.argv) > 1 else str(GOC / TAI_LIEU))
    for dong in cac_loi:
        print("LỖI:", dong)
    print("ĐẠT" if not cac_loi else f"KHÔNG ĐẠT ({len(cac_loi)} lỗi)")
    sys.exit(1 if cac_loi else 0)
