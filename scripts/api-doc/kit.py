"""Công cụ sửa tài liệu API (.docx) cho frontend: mở gói docx, dựng các khối XML đúng định dạng tài liệu đang dùng,
đóng gói lại. Chỉ dùng thư viện chuẩn của Python.

Tài liệu không dùng style có tên cho từng khối: mỗi đoạn, ô bảng, chữ code đều mang định dạng trực tiếp. Các hàm dựng
khối dưới đây sinh ra đúng XML đó (lấy mẫu từ mục 5.1 của bản 1.0), nên nội dung thêm mới trông giống phần đã có.

Chữ trong các hàm nhận "markup" viết gọn: `chữ code` và **chữ đậm**; phần còn lại là chữ thường.

Dùng từ dòng lệnh:
    python scripts/api-doc/kit.py dump <tep.docx> [tep-ra.txt]
"""

import re
import sys
import zipfile
from xml.sax.saxutils import escape

TAI_LIEU = "eClinic_API_XacThuc_TaiKhoan.docx"
TEP_NOI_DUNG = "word/document.xml"
TEP_DANH_SO = "word/numbering.xml"

MAU_METHOD = {"GET": "2E7D32", "POST": "1565C0", "PUT": "E65100", "DELETE": "C62828"}
MAU_DONG_CHAN = "F6F8FA"
RONG_BANG = 9638
DANH_SACH_CHAM = 2  # numId của danh sách chấm tròn dùng trong LƯU Ý

_ARIAL = '<w:rFonts w:ascii="Arial" w:cs="Arial" w:eastAsia="Arial" w:hAnsi="Arial"/>'
_CONSOLAS = '<w:rFonts w:ascii="Consolas" w:cs="Consolas" w:eastAsia="Consolas" w:hAnsi="Consolas"/>'
_KHONG_DAM = '<w:b w:val="false"/><w:bCs w:val="false"/>'
_DAM = "<w:b/><w:bCs/>"
_NEN_CODE = '<w:shd w:fill="EAF0F7" w:color="auto" w:val="clear"/>'
_VIEN_O = ('<w:tcBorders><w:top w:val="single" w:color="C9D1DA" w:sz="4"/><w:left w:val="single" w:color="C9D1DA" '
           'w:sz="4"/><w:bottom w:val="single" w:color="C9D1DA" w:sz="4"/><w:right w:val="single" w:color="C9D1DA" '
           'w:sz="4"/></w:tcBorders>')
_LE_O = ('<w:tcMar><w:top w:type="dxa" w:w="60"/><w:left w:type="dxa" w:w="100"/><w:bottom w:type="dxa" w:w="60"/>'
         '<w:right w:type="dxa" w:w="100"/></w:tcMar>')
_VIEN_BANG = ('<w:tblBorders><w:top w:val="single" w:color="auto" w:sz="4"/><w:left w:val="single" w:color="auto" '
              'w:sz="4"/><w:bottom w:val="single" w:color="auto" w:sz="4"/><w:right w:val="single" w:color="auto" '
              'w:sz="4"/><w:insideH w:val="single" w:color="auto" w:sz="4"/><w:insideV w:val="single" w:color="auto" '
              'w:sz="4"/></w:tblBorders>')
_PPR_CODE = ('<w:keepLines/><w:pBdr><w:left w:val="single" w:color="1F3A5F" w:sz="18" w:space="6"/></w:pBdr>'
             '<w:shd w:fill="F3F5F8" w:color="auto" w:val="clear"/><w:spacing w:after="0" w:before="0" w:line="252"/>'
             '<w:ind w:left="140"/>')


def _co(sz):
    return f'<w:sz w:val="{sz}"/><w:szCs w:val="{sz}"/>'


def _nen(mau):
    return f'<w:shd w:fill="{mau}" w:color="auto" w:val="clear"/>'


def _run(rpr, chu):
    return f'<w:r><w:rPr>{rpr}</w:rPr><w:t xml:space="preserve">{escape(chu, {chr(34): "&quot;"})}</w:t></w:r>'


def runs(markup, trong_bang=False):
    """Markup -> các run. Thân bài: chữ 10pt, code 9pt. Trong bảng: chữ 9pt, code 8pt."""
    co_chu, co_code = (18, 16) if trong_bang else (20, 18)
    ket_qua = []
    for phan in re.split(r"(`[^`]+`|\*\*[^*]+\*\*)", markup):
        if not phan:
            continue
        if phan.startswith("`"):
            ket_qua.append(_run(_CONSOLAS + _KHONG_DAM + '<w:color w:val="0B4F8A"/>' + _co(co_code) + _NEN_CODE,
                                phan[1:-1]))
        elif phan.startswith("**"):
            ket_qua.append(_run(_ARIAL + _DAM + _co(co_chu), phan[2:-2]))
        else:
            ket_qua.append(_run(_ARIAL + _KHONG_DAM + _co(co_chu), phan))
    return "".join(ket_qua)


def doan(markup):
    """Đoạn văn thân bài."""
    return f'<w:p><w:pPr><w:spacing w:after="100" w:before="0" w:line="276"/></w:pPr>{runs(markup)}</w:p>'


def khoang_trong(sau):
    """Đoạn rỗng tạo khoảng cách (60 sau bảng, 100 sau khối code, 200 cuối mỗi route)."""
    return f'<w:p><w:pPr><w:spacing w:after="{sau}" w:before="0"/></w:pPr></w:p>'


def nhan(chu):
    """Nhãn mục con viết hoa: REQUEST BODY (JSON), MÃ LỖI, LƯU Ý..."""
    rpr = _ARIAL + _DAM + '<w:color w:val="5F6B7A"/><w:spacing w:val="20"/>' + _co(16)
    return f'<w:p><w:pPr><w:keepNext/><w:spacing w:after="60" w:before="140"/></w:pPr>{_run(rpr, chu)}</w:p>'


def tieu_de(cap, chu, bookmark=None, id_bookmark=None):
    """Heading 1-3. Heading của route kèm bookmark để bảng route ở mục 2 nhảy tới."""
    than = f'<w:r><w:t xml:space="preserve">{escape(chu)}</w:t></w:r>'
    if bookmark:
        than = (f'<w:bookmarkStart w:name="{bookmark}" w:id="{id_bookmark}"/>{than}'
                f'<w:bookmarkEnd w:id="{id_bookmark}"/>')
    return f'<w:p><w:pPr><w:pStyle w:val="Heading{cap}"/></w:pPr>{than}</w:p>'


def quyen(chu):
    """Dòng "Quyền truy cập: ..." ngay dưới bảng method."""
    xam = '<w:color w:val="5F6B7A"/>' + _co(18)
    return ('<w:p><w:pPr><w:spacing w:after="80" w:before="0" w:line="276"/></w:pPr>'
            + _run(_ARIAL + _DAM + xam, "Quyền truy cập:") + _run(_ARIAL + _KHONG_DAM + xam, " " + chu) + "</w:p>")


def dong_code(dong, cuoi=False):
    """1 dòng của khối code; dòng cuối khối không giữ liền với đoạn sau."""
    giu = '<w:keepNext w:val="false"/>' if cuoi else "<w:keepNext/>"
    return f"<w:p><w:pPr>{giu}{_PPR_CODE}</w:pPr>{_run(_CONSOLAS + _co(17), dong)}</w:p>"


def khoi_code(noi_dung):
    """Khối JSON / curl: mỗi dòng 1 đoạn nền xám có vạch trái."""
    dong = noi_dung.strip("\n").split("\n")
    return "".join(dong_code(d, i == len(dong) - 1) for i, d in enumerate(dong)) + khoang_trong(100)


def cham(markup, num_id=DANH_SACH_CHAM):
    """1 dòng danh sách (chấm tròn ở LƯU Ý; truyền num_id của danh sách đánh số cho các luồng ở Phụ lục B)."""
    return ('<w:p><w:pPr><w:pStyle w:val="ListParagraph"/><w:numPr><w:ilvl w:val="0"/>'
            f'<w:numId w:val="{num_id}"/></w:numPr><w:spacing w:after="60" w:line="276"/></w:pPr>{runs(markup)}</w:p>')


def _o(rong, noi_dung, nen=None, giua=False, doc="top", ppr=None):
    canh = '<w:jc w:val="center"/>' if giua else ""
    ppr = ppr or f'<w:spacing w:after="0" w:line="264"/>{canh}'
    dung = f'<w:vAlign w:val="{doc}"/>' if doc else ""
    return (f'<w:tc><w:tcPr><w:tcW w:type="dxa" w:w="{rong}"/>{_VIEN_O}{_nen(nen) if nen else ""}{_LE_O}{dung}'
            f'</w:tcPr><w:p><w:pPr>{ppr}</w:pPr>{noi_dung}</w:p></w:tc>')


def _dong(cac_o, tieu_de_bang=False):
    dau = "<w:tblHeader/>" if tieu_de_bang else ""
    return f'<w:tr><w:trPr><w:cantSplit/>{dau}</w:trPr>{"".join(cac_o)}</w:tr>'


def _mo_bang(rong):
    assert sum(rong) == RONG_BANG, f"Tổng độ rộng cột phải là {RONG_BANG}, đang là {sum(rong)}"
    luoi = "".join(f'<w:gridCol w:w="{r}"/>' for r in rong)
    return f'<w:tbl><w:tblPr><w:tblW w:type="dxa" w:w="{RONG_BANG}"/>{_VIEN_BANG}</w:tblPr><w:tblGrid>{luoi}</w:tblGrid>'


def dong_bang(rong, o, thu_tu=0, cot_giua=()):
    """1 dòng dữ liệu của bảng thường. o: markup từng ô. thu_tu: vị trí trong các dòng dữ liệu (dòng lẻ có nền)."""
    nen = MAU_DONG_CHAN if thu_tu % 2 else None
    return _dong([_o(r, runs(c, True), nen, giua=i in cot_giua) for i, (r, c) in enumerate(zip(rong, o))])


def bang(rong, tieu_de_cot, cac_dong, cot_giua=()):
    """Bảng có dòng tiêu đề xanh đậm; dòng dữ liệu xen kẽ nền."""
    rpr = _ARIAL + _DAM + '<w:color w:val="FFFFFF"/>' + _co(18)
    dau = _dong([_o(r, _run(rpr, c), "1F3A5F") for r, c in zip(rong, tieu_de_cot)], True)
    than = "".join(dong_bang(rong, d, i, cot_giua) for i, d in enumerate(cac_dong))
    return _mo_bang(rong) + dau + than + "</w:tbl>"


RONG_TRUONG = (1900, 1000, 1500, 5238)
RONG_THAM_SO = (1700, 1000, 1500, 5438)
RONG_LOI = (800, 3300, 5538)
RONG_MO_HINH = (2300, 2400, 4938)
RONG_ROUTE = (650, 950, 3700, 1388, 2950)
RONG_MA_LOI = (3400, 700, 5538)
RONG_ROUTE_THAY_DOI = (600, 850, 3700, 1320, 1718, 1450)  # bảng route từ bản 2.1: thêm cột "Thay đổi"
MAU_THAY_DOI = {"Mới": "2E7D32", "Cập nhật": "E65100"}


def bang_truong(cac_dong):
    """REQUEST BODY: (tên trường, kiểu, bắt buộc, mô tả). Tên trường tự thành chữ code."""
    return bang(RONG_TRUONG, ("Trường", "Kiểu", "Bắt buộc", "Ràng buộc / mô tả"),
                [(f"`{t}`", k, b, m) for t, k, b, m in cac_dong]) + khoang_trong(60)


def bang_tham_so(cac_dong):
    """THAM SỐ: (tên, vị trí path / query, kiểu, mô tả)."""
    return bang(RONG_THAM_SO, ("Tên", "Vị trí", "Kiểu", "Mô tả"), [(f"`{t}`", v, k, m) for t, v, k, m in cac_dong])


def bang_loi(cac_dong):
    """MÃ LỖI: (HTTP, maLoi, điều kiện)."""
    return bang(RONG_LOI, ("HTTP", "maLoi", "Điều kiện"), [(str(h), f"`{m}`", d) for h, m, d in cac_dong],
                cot_giua=(0,))


def bang_method(method, duong_dan):
    """Bảng 1 dòng ngay dưới heading của route: ô method có màu, ô đường dẫn nền xám."""
    trang = _run(_CONSOLAS + _DAM + '<w:color w:val="FFFFFF"/>' + _co(18), method)
    dd = _run(_CONSOLAS + _DAM + _co(18), duong_dan)
    rong = (1150, 8488)
    return (_mo_bang(rong) + _dong([_o(rong[0], trang, MAU_METHOD[method], giua=True, doc="center"),
                                    _o(rong[1], dd, "EEF1F5", doc="center")])
            + "</w:tbl>" + khoang_trong(60))


def dong_route(muc, method, duong_dan, bookmark, quyen_goi, chuc_nang):
    """1 dòng của bảng route ở mục 2; đường dẫn là liên kết tới bookmark của route."""
    chu = lambda c: _run(_ARIAL + _KHONG_DAM + _co(18), c)
    lien_ket = (f'<w:hyperlink w:history="1" w:anchor="{bookmark}">'
                + _run(_CONSOLAS + '<w:color w:val="1565C0"/>' + _co(17) + '<w:u w:val="single"/>', duong_dan)
                + "</w:hyperlink>")
    r = RONG_ROUTE
    return _dong([
        _o(r[0], chu(muc)),
        _o(r[1], _run(_CONSOLAS + _DAM + '<w:color w:val="FFFFFF"/>' + _co(18), method), MAU_METHOD[method], giua=True),
        _o(r[2], lien_ket, doc=None, ppr='<w:spacing w:after="0"/>'),
        _o(r[3], chu(quyen_goi)),
        _o(r[4], chu(chuc_nang)),
    ])


def them_cot_thay_doi(bang_xml, danh_dau):
    """Bảng route 5 cột -> 6 cột: co 5 cột cũ lại theo RONG_ROUTE_THAY_DOI và thêm cột "Thay đổi" ở cuối. Dòng đã có
    6 ô thì giữ nguyên, nên giai đoạn sau chỉ cần thêm dòng 5 ô (dong_route) rồi gọi lại hàm này.
    danh_dau: {mục: (loại, phiên bản)}, vd {"6.4": ("Mới", "2.1")}; mục không có trong đó thì ô để trống."""
    rong = RONG_ROUTE_THAY_DOI
    assert sum(rong) == RONG_BANG
    da_dung = set()

    def sua_dong(m):
        dong = m.group(0)
        cac_o = re.findall(r"<w:tcW [^>]*/>", dong)
        if len(cac_o) == 6:  # dòng đã có cột "Thay đổi" từ giai đoạn trước
            return dong
        assert len(cac_o) == 5, f"Dòng của bảng route phải có 5 hoặc 6 ô, đang có {len(cac_o)}"
        dem = iter(rong)
        dong = re.sub(r"<w:tcW [^>]*/>", lambda _: f'<w:tcW w:type="dxa" w:w="{next(dem)}"/>', dong)
        if "<w:tblHeader/>" in dong:
            o = _o(rong[5], _run(_ARIAL + _DAM + '<w:color w:val="FFFFFF"/>' + _co(18), "Thay đổi"), "1F3A5F")
        else:
            muc = re.search(r"<w:t[^>]*>([^<]*)</w:t>", dong).group(1)
            chu = ""
            if muc in danh_dau:
                loai, phien_ban = danh_dau[muc]
                da_dung.add(muc)
                chu = _run(_ARIAL + _DAM + f'<w:color w:val="{MAU_THAY_DOI[loai]}"/>' + _co(17),
                           f"{loai} · v{phien_ban}")
            o = _o(rong[5], chu)
        return dong.replace("</w:tr>", o + "</w:tr>")

    luoi = "<w:tblGrid>" + "".join(f'<w:gridCol w:w="{r}"/>' for r in rong) + "</w:tblGrid>"
    bang_xml = re.sub(r"<w:tblGrid>.*?</w:tblGrid>", luoi, bang_xml, count=1, flags=re.S)
    bang_xml = re.sub(r"<w:tr>.*?</w:tr>", sua_dong, bang_xml, flags=re.S)
    assert da_dung == set(danh_dau), f"Mục không có trong bảng route: {sorted(set(danh_dau) - da_dung)}"
    return to_lai_nen(bang_xml)


def route(muc, ten, method, duong_dan, bookmark, id_bookmark, quyen_goi, mo_ta, than):
    """Khối đầy đủ của 1 route. mo_ta: 1 markup hoặc danh sách markup; than: các khối đã dựng (nhãn, bảng, code...)."""
    mo_ta = [mo_ta] if isinstance(mo_ta, str) else mo_ta
    return (tieu_de(3, f"{muc}  {ten}", bookmark, id_bookmark) + bang_method(method, duong_dan) + quyen(quyen_goi)
            + "".join(doan(m) for m in mo_ta) + "".join(than) + khoang_trong(200))


def luu_y(cac_dong):
    return nhan("LƯU Ý") + "".join(cham(d) for d in cac_dong)


class TaiLieu:
    """Gói docx đã mở: sửa self.xml (word/document.xml) rồi luu()."""

    def __init__(self, duong_dan=TAI_LIEU):
        with zipfile.ZipFile(duong_dan) as goi:
            self.tep = {t.filename: goi.read(t.filename) for t in goi.infolist() if not t.is_dir()}
        self.xml = self.tep[TEP_NOI_DUNG].decode("utf-8")

    def luu(self, duong_dan=TAI_LIEU):
        self.tep[TEP_NOI_DUNG] = self.xml.encode("utf-8")
        ten = sorted(self.tep, key=lambda t: (t != "[Content_Types].xml", t))
        with zipfile.ZipFile(duong_dan, "w", zipfile.ZIP_DEFLATED) as goi:
            for t in ten:
                goi.writestr(t, self.tep[t])

    # --- tìm và thay ---

    def thay_mot(self, cu, moi):
        """Thay đúng 1 chỗ; 0 hoặc nhiều hơn 1 chỗ khớp thì dừng (tránh sửa nhầm hoặc sửa hụt)."""
        so_lan = self.xml.count(cu)
        assert so_lan == 1, f"Cần đúng 1 chỗ khớp, thấy {so_lan}: {cu[:80]!r}"
        self.xml = self.xml.replace(cu, moi)

    def thay_chu(self, cu, moi):
        """Thay chữ nằm trọn trong 1 run (chữ thường, không markup)."""
        self.thay_mot(f">{escape(cu, {chr(34): '&quot;'})}</w:t>", f">{escape(moi, {chr(34): '&quot;'})}</w:t>")

    def _vi_tri_chu(self, chu, tu=0):
        khoa = f">{escape(chu, {chr(34): '&quot;'})}</w:t>"
        vi_tri = self.xml.find(khoa, tu)
        assert vi_tri >= 0, f"Không thấy chữ: {chu!r}"
        return vi_tri

    def _doan_chua(self, chu, tu=0):
        """(đầu, cuối) của đoạn <w:p> ngoài bảng chứa chữ này."""
        vi_tri = self._vi_tri_chu(chu, tu)
        dau = self.xml.rfind("<w:p>", 0, vi_tri)
        return dau, self.xml.index("</w:p>", vi_tri) + len("</w:p>")

    def chen_sau_doan(self, chu, moi, tu=0):
        _, cuoi = self._doan_chua(chu, tu)
        self.xml = self.xml[:cuoi] + moi + self.xml[cuoi:]

    def chen_truoc_doan(self, chu, moi, tu=0):
        dau, _ = self._doan_chua(chu, tu)
        self.xml = self.xml[:dau] + moi + self.xml[dau:]

    def thay_doan(self, chu, moi, tu=0):
        dau, cuoi = self._doan_chua(chu, tu)
        self.xml = self.xml[:dau] + moi + self.xml[cuoi:]

    def thay_noi_dung(self, chu, markup, trong_bang=False):
        """Viết lại chữ của đoạn (hoặc ô bảng 1 đoạn) có 1 run đúng bằng `chu`, giữ nguyên định dạng đoạn."""
        khoa = f">{escape(chu, {chr(34): '&quot;'})}</w:t>"
        assert self.xml.count(khoa) == 1, f"Cần đúng 1 chỗ khớp, thấy {self.xml.count(khoa)}: {chu[:80]!r}"
        dau, cuoi = self._doan_chua(chu)
        doan_cu = self.xml[dau:cuoi]
        self.xml = self.xml[:dau] + doan_cu[:doan_cu.index("<w:r>")] + runs(markup, trong_bang) + "</w:p>" + self.xml[cuoi:]

    def thay_dau_trang(self, cu, moi):
        """Thay dòng chữ ở đầu mỗi trang (word/header1.xml)."""
        tep = "word/header1.xml"
        noi_dung = self.tep[tep].decode("utf-8")
        assert noi_dung.count(f">{escape(cu)}<") == 1, f"Không thấy đầu trang: {cu!r}"
        self.tep[tep] = noi_dung.replace(f">{escape(cu)}<", f">{escape(moi)}<").encode("utf-8")

    def vi_tri_tieu_de(self, chu):
        """Vị trí bắt đầu đoạn heading có đúng chữ này."""
        return self._doan_chua(chu)[0]

    # --- bảng ---

    def _bang_sau(self, tu, nhan_bang=None):
        """(đầu, cuối) của bảng đầu tiên sau vị trí tu (sau nhãn nhan_bang nếu có)."""
        if nhan_bang:
            tu = self._vi_tri_chu(nhan_bang, tu)
        dau = self.xml.index("<w:tbl>", tu)
        return dau, self.xml.index("</w:tbl>", dau) + len("</w:tbl>")

    def them_dong(self, tieu_de_muc, nhan_bang, dong, sau_dong_chua=None):
        """Thêm dòng (XML của <w:tr>) vào bảng nằm sau nhãn nhan_bang trong mục tieu_de_muc: cuối bảng, hoặc ngay sau
        dòng chứa chữ sau_dong_chua. Sau đó tô lại nền xen kẽ cho cả bảng."""
        dau, cuoi = self._bang_sau(self.vi_tri_tieu_de(tieu_de_muc), nhan_bang)
        bang_cu = self.xml[dau:cuoi]
        if sau_dong_chua:
            khoa = f">{escape(sau_dong_chua, {chr(34): '&quot;'})}</w:t>"
            assert bang_cu.count(khoa) == 1, f"Cần đúng 1 dòng chứa {sau_dong_chua!r}, thấy {bang_cu.count(khoa)}"
            cho_chen = bang_cu.index("</w:tr>", bang_cu.index(khoa)) + len("</w:tr>")
        else:
            cho_chen = bang_cu.rindex("</w:tr>") + len("</w:tr>")
        self.xml = self.xml[:dau] + to_lai_nen(bang_cu[:cho_chen] + dong + bang_cu[cho_chen:]) + self.xml[cuoi:]

    # --- bookmark, danh sách đánh số ---

    def id_bookmark_moi(self):
        return max(int(i) for i in re.findall(r'<w:bookmarkStart [^>]*w:id="(\d+)"', self.xml)) + 1

    def danh_so_lai_bookmark(self):
        """Đánh lại id bookmark 1, 2, 3... theo thứ tự xuất hiện (bản 1.0 để mọi bookmark cùng id 1)."""
        dem = iter(range(1, 10 ** 6))
        dang_mo = []

        def thay(m):
            if m.group(1) == "Start":
                dang_mo.append(next(dem))
                return re.sub(r'w:id="\d+"', f'w:id="{dang_mo[-1]}"', m.group(0))
            return f'<w:bookmarkEnd w:id="{dang_mo.pop()}"/>'

        self.xml = re.sub(r"<w:bookmark(Start|End) [^>]*/>", thay, self.xml)

    def danh_sach_so_moi(self):
        """Thêm 1 danh sách đánh số 1. 2. 3. bắt đầu lại từ 1 (mỗi luồng ở Phụ lục B dùng 1 danh sách riêng)."""
        ds = self.tep[TEP_DANH_SO].decode("utf-8")
        mau = re.findall(r"<w:abstractNum .*?</w:abstractNum>", ds, re.S)[-1]
        assert '<w:numFmt w:val="decimal"/>' in mau, "abstractNum cuối không phải danh sách đánh số"
        so_moi = max(int(i) for i in re.findall(r'<w:num w:numId="(\d+)"', ds)) + 1
        cu = re.search(r'w:abstractNumId="(\d+)"', mau).group(1)
        dinh_nghia = mau.replace(f'w:abstractNumId="{cu}"', f'w:abstractNumId="{so_moi}"', 1)
        ds = ds.replace(mau, mau + dinh_nghia, 1)
        num = (f'<w:num w:numId="{so_moi}"><w:abstractNumId w:val="{so_moi}"/><w:lvlOverride w:ilvl="0">'
               '<w:startOverride w:val="1"/></w:lvlOverride></w:num>')
        ds = ds.replace("</w:numbering>", num + "</w:numbering>")
        self.tep[TEP_DANH_SO] = ds.encode("utf-8")
        return so_moi


def to_lai_nen(bang_xml):
    """Tô lại nền xen kẽ: dòng dữ liệu thứ 2, 4, 6... có nền xám nhạt; ô có màu riêng (ô method) giữ nguyên."""
    nen = _nen(MAU_DONG_CHAN)
    cac_dong = re.findall(r"<w:tr>.*?</w:tr>", bang_xml, re.S)
    dau = bang_xml[:bang_xml.index("<w:tr>")]
    moi, thu_tu = [], 0
    for dong in cac_dong:
        if "<w:tblHeader/>" in dong:
            moi.append(dong)
            continue
        dong = dong.replace(nen, "")
        if thu_tu % 2:
            # Ô chưa có nền: chèn nền ngay sau viền ô (thứ tự phần tử trong tcPr là cố định)
            dong = re.sub(r"(</w:tcBorders>)(?!<w:shd )", r"\1" + nen, dong)
        moi.append(dong)
        thu_tu += 1
    return dau + "".join(moi) + "</w:tbl>"


def dump(duong_dan):
    """Nội dung chữ của docx: heading có nhãn [H1]..[H3], dòng bảng dạng | ô | ô |. Dùng để so trước / sau khi sửa."""
    xml = TaiLieu(duong_dan).xml
    than = xml[xml.index("<w:body>"):]
    giai_ma = lambda s: (s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", '"').replace("&apos;", "'")
                         .replace("&amp;", "&"))
    ket_qua, trong_bang, dong, o = [], 0, [], []
    for m in re.finditer(r"<w:tbl>|</w:tbl>|</w:tr>|</w:tc>|<w:p>.*?</w:p>", than, re.S):
        t = m.group(0)
        if t == "<w:tbl>":
            trong_bang += 1
        elif t == "</w:tbl>":
            trong_bang -= 1
            ket_qua.append("")
        elif t == "</w:tc>":
            dong.append(" / ".join(o))
            o = []
        elif t == "</w:tr>":
            ket_qua.append("| " + " | ".join(dong) + " |")
            dong = []
        else:
            cap = re.search(r'<w:pStyle w:val="Heading(\d)"/>', t)
            chu = giai_ma("".join(re.findall(r"<w:t[^>]*>([^<]*)</w:t>", t)))
            if trong_bang:
                o.append(chu)
            else:
                ket_qua.append((f"[H{cap.group(1)}] " if cap else "") + chu)
    return "\n".join(ket_qua) + "\n"


if __name__ == "__main__":
    if len(sys.argv) >= 3 and sys.argv[1] == "dump":
        noi_dung = dump(sys.argv[2])
        if len(sys.argv) > 3:
            with open(sys.argv[3], "w", encoding="utf-8", newline="\n") as tep:
                tep.write(noi_dung)
        else:
            sys.stdout.reconfigure(encoding="utf-8")
            sys.stdout.write(noi_dung)
    else:
        print(__doc__)
