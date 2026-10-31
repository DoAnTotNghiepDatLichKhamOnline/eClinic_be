"""Bước 2 của bộ kiểm tra hợp đồng API khi gộp bảng (DOANTOTNGH-19): chụp response của mọi route GET trên bản dump V36.

Khôi phục temp/db-gon/v36.sql.gz vào stack thử (Flyway của bản code đang chạy tự nâng DB lên), đăng nhập 3 vai trò
trong moc.json, gọi mọi route GET của bảng TUYEN bằng tiếng Việt và tiếng Anh, ghi vào temp/db-gon/<ten>/.

    PYTHONUTF8=1 /c/Users/buiho/AppData/Local/Python/bin/python scripts/db-gon/chup.py <ten> [--truoc] [--build]

--truoc: chạy bằng image đóng băng (compose.truoc.yml) thay vì image của stack thử; --build: build lại image stack thử
từ code hiện tại trước khi chạy (dùng sau khi sửa code). Chụp xong thì tắt stack.

Trước khi chạy, đối chiếu TUYEN với các tiêu đề "#### GET ..." của docs/api/*.md: route có trong tài liệu mà không có
trong TUYEN hay BO_QUA thì dừng.
"""

import hashlib
import json
import re
import sys
from datetime import date, timedelta
from itertools import product

import chung
from chung import GOC

# (vai trò, đường dẫn). {ten} lấy từ moc.json (danh sách -> gọi với từng giá trị) hoặc từ NGAY bên dưới.
# Vai trò: "" = không token, bn = bệnh nhân, bs = bác sĩ, qt = quản trị viên.
TUYEN = [
    # Công khai
    ("", "/api/booking/khung-gio?ngay={tuNgay}&idBacSi={idBacSi}"),
    ("", "/api/booking/khung-gio?ngay={tuNgay}&idChuyenKhoa={idChuyenKhoa}"),
    ("", "/api/booking/khung-gio/gop?idChuyenKhoa={idChuyenKhoa}&ngay={tuNgay}"),
    ("", "/api/booking/khung-gio/ngay-con-cho?idBacSi={idBacSi}"),
    ("", "/api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa={idChuyenKhoa}"),
    ("", "/api/booking/khung-gio/nhieu-ngay?idBacSi={idBacSi}"),
    ("", "/api/booking/ngay-nghi"),
    ("", "/api/booking/phieu-kham/{maPhieuKham}"),
    ("", "/api/booking/phieu-kham/{maPhieuKham}/lich.ics"),
    ("", "/api/booking/phieu-kham/{maPhieuKham}/qr"),
    ("", "/api/booking/quy-tac-dat-lich"),
    ("", "/api/booking/so-kham/{maSoKham}"),
    ("", "/api/booking/so-kham/{maSoKham}/qr"),
    ("", "/api/catalog/bac-si?kichThuoc=100"),
    ("", "/api/catalog/bac-si/{idBacSi}"),
    ("", "/api/catalog/bac-si/{idBacSiCoAnh}"),
    ("", "/api/catalog/chuyen-khoa?kichThuoc=100"),
    ("", "/api/catalog/chuyen-khoa/{idChuyenKhoa}"),
    # Bệnh nhân
    ("bn", "/api/booking/ho-so-benh-nhan/cua-toi"),
    ("bn", "/api/booking/lich-hen/cua-toi?kichThuoc=100"),
    ("bn", "/api/booking/lich-hen/cua-toi/{maPhieuKhamBenhNhan}"),
    ("bn", "/api/booking/lich-hen/cua-toi/lich?tuNgay={tuNgay}&denNgay={denNgay}"),
    ("bn", "/api/booking/lich-su-kham/cua-toi?kichThuoc=100"),
    ("bn", "/api/booking/thong-tin-dat-lich/cua-toi"),
    ("bn", "/api/booking/trang-ca-nhan/cua-toi"),
    ("bn", "/api/chatbot/phien"),
    ("bn", "/api/chatbot/phien/{idPhienChat}/tin-nhan"),
    ("bn", "/api/notification/thong-bao?kichThuoc=100"),
    ("bn", "/api/notification/thong-bao/so-chua-doc"),
    ("bn", "/api/users/me"),
    ("bn", "/api/users/me/change-email"),
    ("bn", "/api/users/me/sessions"),
    # Bác sĩ
    ("bs", "/api/booking/bac-si/toi/danh-gia?kichThuoc=100"),
    ("bs", "/api/booking/bac-si/toi/danh-gia/tong-quan"),
    ("bs", "/api/booking/bac-si/toi/hang-doi?ngay={homNay}"),
    ("bs", "/api/booking/bac-si/toi/lich-hen?ngay={ngayBacSi}"),
    ("bs", "/api/booking/bac-si/toi/lich-hen/{idLichHenBacSi}/ho-so-kham"),
    ("bs", "/api/booking/bac-si/toi/lich-hen/lich?tuNgay={tuNgay}&denNgay={denNgay}"),
    ("bs", "/api/booking/bac-si/toi/lich-hen/theo-cccd?cccd={cccdBacSi}"),
    ("bs", "/api/booking/bac-si/toi/lich-hen/tra-cuu?ma={maTraCuuBacSi}"),
    ("bs", "/api/booking/bac-si/toi/lich-hen/tra-cuu/ho-so-kham?ma={maTraCuuBacSi}"),
    ("bs", "/api/booking/bac-si/toi/lich-lam-viec?tuNgay={tuNgay}&denNgay={denNgay}"),
    ("bs", "/api/booking/bac-si/toi/lich-lam-viec/pdf?tuNgay={tuNgay}&denNgay={denNgay}"),
    ("bs", "/api/booking/bac-si/toi/tong-quan"),
    ("bs", "/api/booking/bac-si/toi/yeu-cau-doi-lich?kichThuoc=100"),
    ("bs", "/api/booking/bac-si/toi/yeu-cau-nhuong-ca?kichThuoc=100"),
    ("bs", "/api/catalog/bac-si/toi/ho-so"),
    ("bs", "/api/catalog/phong-kham"),
    ("bs", "/api/medical/bac-si/toi/icd10?tuKhoa=J06"),
    ("bs", "/api/medical/bac-si/toi/icd10?tuKhoa=viêm"),
    ("bs", "/api/medical/bac-si/toi/lich-hen/{idLichHenBacSi}/benh-an"),
    ("bs", "/api/medical/bac-si/toi/lich-hen/{idLichHenBacSi}/benh-an/lich-su"),
    ("bs", "/api/medical/bac-si/toi/thuoc?tuKhoa=para"),
    ("bs", "/api/notification/thong-bao?kichThuoc=100"),
    # Quản trị viên
    ("qt", "/api/booking/quan-tri/cau-hinh"),
    ("qt", "/api/booking/quan-tri/cau-hinh/lich-su?kichThuoc=100"),
    ("qt", "/api/booking/quan-tri/danh-gia?kichThuoc=100"),
    ("qt", "/api/booking/quan-tri/ho-so-benh-nhan?kichThuoc=100"),
    ("qt", "/api/booking/quan-tri/ho-so-benh-nhan/{idHoSoBenhNhan}"),
    ("qt", "/api/booking/quan-tri/ho-so-benh-nhan/{idHoSoBenhNhan}/lich-hen?kichThuoc=100"),
    ("qt", "/api/booking/quan-tri/ho-so-benh-nhan/cho-xac-minh?kichThuoc=100"),
    ("qt", "/api/booking/quan-tri/lich-hen?tuNgay={tuNgay}&denNgay={denNgay}&kichThuoc=100"),
    ("qt", "/api/booking/quan-tri/lich-hen?kichThuoc=100"),
    ("qt", "/api/booking/quan-tri/lich-hen/{idLichHen}"),
    ("qt", "/api/booking/quan-tri/lich-hen/tra-cuu?ma={maTraCuu}"),
    ("qt", "/api/booking/quan-tri/lich-lam-viec?tuNgay={tuNgay}&denNgay={denNgay}"),
    ("qt", "/api/booking/quan-tri/lich-lam-viec/{idLichLamViec}/lich-hen"),
    ("qt", "/api/booking/quan-tri/lich-lam-viec/{idLichLamViec}/lich-su"),
    ("qt", "/api/booking/quan-tri/lich-lam-viec/can-doi-lich?kichThuoc=100"),
    ("qt", "/api/booking/quan-tri/lich-lam-viec/gio-lam-theo-tuan?tuNgay={tuNgay}&denNgay={denNgay}"),
    ("qt", "/api/booking/quan-tri/lich-lam-viec/pdf/bac-si/{idBacSi}?tuNgay={tuNgay}&denNgay={denNgay}"),
    ("qt", "/api/booking/quan-tri/lich-lam-viec/pdf/toan-vien?tuNgay={tuNgay}&denNgay={denNgay}"),
    ("qt", "/api/booking/quan-tri/lich-lam-viec/xuat-excel?tuNgay={tuNgay}&denNgay={denNgay}"),
    ("qt", "/api/booking/quan-tri/ngay-nghi"),
    ("qt", "/api/booking/quan-tri/tong-quan"),
    ("qt", "/api/booking/quan-tri/yeu-cau-doi-lich?tatCa=true&kichThuoc=100"),
    ("qt", "/api/catalog/quan-tri/bac-si?kichThuoc=100"),
    ("qt", "/api/catalog/quan-tri/bac-si/{idBacSi}"),
    ("qt", "/api/catalog/quan-tri/bac-si/{idBacSiCoAnh}"),
    ("qt", "/api/catalog/quan-tri/bac-si/{idBacSi}/anh-huong-ngung-cong-tac"),
    ("qt", "/api/catalog/quan-tri/chuyen-khoa?kichThuoc=100"),
    ("qt", "/api/catalog/quan-tri/phong-kham?kichThuoc=100"),
    ("qt", "/api/medical/quan-tri/lich-hen/{idLichHen}/benh-an/lich-su"),
    ("qt", "/api/medical/quan-tri/thuoc?kichThuoc=100"),
    ("qt", "/api/notification/thong-bao?kichThuoc=100"),
    ("qt", "/api/report/quan-tri/thong-ke"),
    ("qt", "/api/report/quan-tri/thong-ke/xuat-excel"),
    ("qt", "/api/users?kichThuoc=100"),
    ("qt", "/api/users/{idTaiKhoan}"),
]
# Route GET trong tài liệu không chụp, kèm lý do
BO_QUA = {
    "/api/booking/quan-tri/lich-lam-viec/nhap-excel/mau": "tệp mẫu tĩnh, không đọc bảng nào",
}
NGON_NGU = [None, "en"]
LA_TEP = re.compile(r"application/(pdf|vnd\.|octet-stream)|image/|text/calendar")
NGAY_GIO = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?$")
# Giá trị sinh ra trong lúc chụp (không nằm trong bản dump), che đi khi ghi: (mẫu đường dẫn route, khoá JSON)
CHE_THEO_ROUTE = [
    (re.compile(r"^/api/users/me/sessions"), {"id", "maPhien"}),
]


def route_tai_lieu():
    tim = set()
    for tep in (GOC / "docs/api").glob("*.md"):
        tim |= set(re.findall(r"^#### GET (\S+)", tep.read_text(encoding="utf-8"), re.M))
    return tim


def kiem_tra_du_route():
    trong_bang = {re.sub(r"\{[^}]+}", "{}", d.split("?")[0]) for _, d in TUYEN}
    thieu = sorted(r for r in route_tai_lieu()
                   if re.sub(r"\{[^}]+}", "{}", r) not in trong_bang and r not in BO_QUA)
    if thieu:
        raise SystemExit("Route GET trong tài liệu chưa có trong TUYEN / BO_QUA:\n  " + "\n  ".join(thieu))


def gia_tri(moc):
    """Giá trị cho từng {ten}: luôn là danh sách."""
    ngay_dung = date.fromisoformat(moc["ngayDung"])
    ngay_bs = [d for d in moc["ngayBacSi"] if d]
    kq = {
        "homNay": [date.today().isoformat()],
        "tuNgay": [(ngay_dung - timedelta(days=7)).isoformat()],
        "denNgay": [(ngay_dung + timedelta(days=20)).isoformat()],
        "ngayBacSi": sorted(set(ngay_bs)),
    }
    for k, v in moc.items():
        if k not in kq:
            kq[k] = v if isinstance(v, list) else [v]
    return kq


def mo_rong(duong_dan, gt):
    ten = re.findall(r"\{([^}]+)}", duong_dan)
    for bo in product(*(gt[t] for t in ten)):
        thuc = duong_dan
        for t, v in zip(ten, bo):
            thuc = thuc.replace("{" + t + "}", str(v), 1)
        yield thuc


def ten_tep(vai_tro, duong_dan, ngon_ngu):
    gon = re.sub(r"[^A-Za-z0-9]+", "-", duong_dan.removeprefix("/api/")).strip("-")[:90]
    bam = hashlib.sha1(f"{vai_tro}|{duong_dan}".encode()).hexdigest()[:8]
    return f"{vai_tro or 'cong-khai'}--{gon}--{bam}.{ngon_ngu or 'vi'}.json"


def chuan_hoa(gt, moc_dump, che):
    """Che giá trị sinh ra sau thời điểm dump (ngày giờ) và các khoá CHE_THEO_ROUTE."""
    if isinstance(gt, dict):
        return {k: "<che>" if k in che else chuan_hoa(v, moc_dump, che) for k, v in gt.items()}
    if isinstance(gt, list):
        return [chuan_hoa(v, moc_dump, che) for v in gt]
    if isinstance(gt, str) and NGAY_GIO.match(gt) and gt > moc_dump:
        return "<sau-moc>"
    return gt


def chup(ten, moc, thu_muc):
    token = {"": None}
    for vai_tro, khoa in (("bn", "benhNhan"), ("bs", "bacSi"), ("qt", "quanTri")):
        tk = moc["taiKhoan"][khoa]
        token[vai_tro] = chung.dang_nhap(tk["email"], tk["matKhau"])
    gt = gia_tri(moc)
    moc_dump = moc["thoiDiemDump"][:19]
    so, khong_200, da_goi = 0, [], set()
    for vai_tro, mau in TUYEN:
        che = set().union(*(k for m, k in CHE_THEO_ROUTE if m.match(mau)))
        for duong_dan in mo_rong(mau, gt):
            # 2 mẫu có thể ra cùng 1 đường dẫn (vd idBacSi = idBacSiCoAnh): chỉ gọi 1 lần
            if (vai_tro, duong_dan) in da_goi:
                continue
            da_goi.add((vai_tro, duong_dan))
            for ngon_ngu in NGON_NGU:
                status, kieu, tho = chung.goi("GET", duong_dan, token=token[vai_tro], ngon_ngu=ngon_ngu)
                if LA_TEP.search(kieu):
                    than = {"kieuTep": kieu.split(";")[0]}
                elif "json" in kieu and tho:
                    than = chuan_hoa(json.loads(tho), moc_dump, che)
                else:
                    than = tho.decode("utf-8", "replace")
                ghi = {"vaiTro": vai_tro, "route": f"GET {mau}", "duongDan": duong_dan, "ngonNgu": ngon_ngu or "vi",
                       "status": status, "than": than}
                (thu_muc / ten_tep(vai_tro, duong_dan, ngon_ngu)).write_text(
                    json.dumps(ghi, ensure_ascii=False, indent=1, sort_keys=True), encoding="utf-8")
                so += 1
                if status != 200 and ngon_ngu is None:
                    khong_200.append(f"{status} {vai_tro or '-'} {duong_dan}")
    print(f"Đã chụp {so} response vào {thu_muc}")
    if khong_200:
        print("Không phải 200:\n  " + "\n  ".join(khong_200))


def main():
    doi_so = sys.argv[1:]
    if not doi_so or doi_so[0].startswith("-"):
        raise SystemExit(__doc__)
    ten, truoc, build = doi_so[0], "--truoc" in doi_so, "--build" in doi_so
    kiem_tra_du_route()
    moc = chung.doc_moc()
    thu_muc = chung.THU_MUC / ten
    chung.xoa_thu_muc(thu_muc)
    chung.tat()
    try:
        chung.len_mysql()
        chung.khoi_phuc(chung.TEP_DUMP)
        chung.len_app(truoc=truoc, build=build, moi_truong=chung.JOB_TAT)
        chup(ten, moc, thu_muc)
    finally:
        chung.tat()


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
