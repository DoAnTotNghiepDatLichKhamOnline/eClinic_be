"""Kiểm tra đường ghi của bộ kiểm tra hợp đồng API khi gộp bảng (DOANTOTNGH-19): chạy 1 chuỗi lệnh ghi cố định trên bản
dump V36 rồi ghi lại response và trạng thái DB, để so code cũ với code mới bằng so_sanh.py.

    PYTHONUTF8=1 /c/Users/buiho/AppData/Local/Python/bin/python scripts/db-gon/thu_ghi.py <ten> [--truoc] [--build]

chup.py chỉ gọi GET nên không thử được các luật chỉ chạy khi ghi. Script này khôi phục bản dump, chuẩn bị (CHUAN_BI, trên
schema V36 trước khi app lên), bật app (Flyway của code đang chạy nâng DB lên), chạy kich_ban() rồi ghi
temp/db-gon/<ten>/_ghi.json (cùng dạng tệp của chup.py):
  - mỗi bước: tên, status, thân response; mã phiếu khám / mã tra cứu sinh ra thay bằng <ma-N> theo thứ tự gặp, ngày giờ
    có phần lẻ giây (lúc ghi) thành <bay-gio>; id giữ nguyên vì cùng bản dump + cùng chuỗi lệnh ra cùng id,
  - trạng thái DB của phase: đọc từ bảng cũ nếu còn, không thì từ cột mới.
Phase 2: dùng lại người giám hộ (khai lần đầu, điền ngày sinh còn trống, khác tên, khác ngày sinh), khoá nhận diện sau khi
quản trị viên sửa hồ sơ trẻ chưa có CCCD, từ chối liên kết hồ sơ (dòng có sẵn trong dump và 1 lần từ chối mới).
Phase 3: ghi đủ 4 loại lịch sử (lịch hẹn: hủy / đổi lịch; ca: tạo / sửa / hủy; cấu hình; sửa bệnh án) rồi đọc lại qua API.
Phase 4: các dòng chờ gửi (thông báo chuông + email kèm .ics) mà các bước trên xếp vào hàng (job tắt nên chưa gửi), đọc
theo tên cột cũ từ bảng cũ nếu còn, không thì từ hang_cho_gui; mã sinh ra được che như trong response.
Phase 5: sửa bệnh án với mã ICD không có (400) và mã chữ thường (200); ảnh giới thiệu của bác sĩ 1 (quản trị viên tải 1
ảnh thật lên thư mục eclinic-smoke của kho ảnh rồi xoá trong cùng lần chạy, sửa chú thích, sắp xếp; bác sĩ sửa / xoá ảnh
làm việc, sửa ảnh chứng chỉ bị 403); url ảnh vừa tải được che như mã sinh ra.
--truoc / --build như chup.py. Chạy xong thì tắt stack.
"""

import json
import re
import struct
import sys
import urllib.error
import urllib.request
import uuid
import zlib

import chung
from chay_job import co_bang, dem_hang_cho, hang_cho_moi, mot, sql_json

CCCD_GIAM_HO = "079099000101"
CCCD_NGUOI_LON = "079099000202"
# Tài khoản trong dump từng bị từ chối liên kết: mượn mật khẩu của benhnhan01 (Demo@123) để đăng nhập được.
CHUAN_BI = """
UPDATE cau_hinh_he_thong SET so_lich_hieu_luc_toi_da_moi_ho_so = 100, so_lich_hieu_luc_toi_da_moi_sdt = 100;
UPDATE tai_khoan t
    JOIN (SELECT MIN(id_tai_khoan) AS id FROM lien_ket_ho_so_bi_tu_choi) r ON r.id = t.id_tai_khoan
    JOIN (SELECT mat_khau_hash FROM tai_khoan WHERE email = 'benhnhan01@eclinic.local') m
SET t.mat_khau_hash = m.mat_khau_hash;
UPDATE ho_so_benh_an SET ngay_tao = CURDATE() WHERE id_lich_hen = 20;
"""
# Hồ sơ bệnh án của lịch hẹn 20 (bác sĩ 1, đã sửa 1 lần trong dump): ngày ghi nhận dời về hôm nay để còn hạn sửa
ID_LICH_HEN_SUA_BENH_AN = 20
# Các cột của 1 dòng lịch sử, trừ id và ngay_tao: loại -> (bảng cũ, khoá cũ, cột cũ, cột trong lich_su_thay_doi)
LICH_SU = {
    "LICH_HEN": ("lich_su_lich_hen", "id_lich_su_lich_hen",
                 "id_lich_hen, su_kien, loai_nguoi_thuc_hien, id_tai_khoan_thuc_hien, id_lich_hen_lien_quan, ly_do",
                 "id_lich_hen, hanh_dong, loai_nguoi_thuc_hien, id_tai_khoan_thuc_hien, id_lich_hen_lien_quan, ly_do"),
    "CA_LAM_VIEC": ("lich_su_ca_lam_viec", "id_lich_su_ca",
                    "id_lich_lam_viec, hanh_dong, nguon, id_quan_tri_vien, id_yeu_cau, id_bac_si, id_phong_kham, "
                    "ngay_lam_viec, gio_bat_dau, gio_ket_thuc, so_luot_toi_da_moi_gio, thoi_luong_luot_phut, ly_do",
                    "id_lich_lam_viec, hanh_dong, nguon, id_quan_tri_vien, id_yeu_cau, id_bac_si, id_phong_kham, "
                    "ngay_lam_viec, gio_bat_dau, gio_ket_thuc, so_luot_toi_da_moi_gio, thoi_luong_luot_phut, ly_do"),
    "CAU_HINH": ("lich_su_cau_hinh", "id_lich_su_cau_hinh", "ten_truong, gia_tri_cu, gia_tri_moi, id_quan_tri_vien",
                 "ten_truong, gia_tri_truoc, gia_tri_sau, id_quan_tri_vien"),
    "BENH_AN": ("lich_su_sua_benh_an", "id_lich_su_sua",
                "id_ho_so_benh_an, phien_ban, id_bac_si, ly_do, noi_dung_truoc, noi_dung_sau",
                "id_ho_so_benh_an, phien_ban, id_bac_si, ly_do, gia_tri_truoc, gia_tri_sau"),
}
# Bác sĩ có ảnh mẫu trong dump (3 ảnh: 2 ảnh làm việc, 1 chứng chỉ), là bác sĩ của tài khoản bác sĩ trong moc.json
ID_BAC_SI_ANH = 1
# Ngày đầu tiên từ ngày mai (trong 80 ngày) bác sĩ 1 chưa có ca và bệnh viện không nghỉ, kèm phòng của bác sĩ 1
NGAY_TRONG = """
WITH RECURSIVE d(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM d WHERE n < 80)
SELECT (SELECT MIN(id_phong_kham) FROM lich_lam_viec WHERE id_bac_si = 1),
       DATE_FORMAT(CURDATE() + INTERVAL n DAY, '%Y-%m-%d')
FROM d
WHERE NOT EXISTS (SELECT 1 FROM lich_lam_viec c WHERE c.id_bac_si = 1 AND c.ngay_lam_viec = CURDATE() + INTERVAL n DAY)
  AND NOT EXISTS (SELECT 1 FROM ngay_nghi_benh_vien x WHERE x.ngay = CURDATE() + INTERVAL n DAY)
ORDER BY n LIMIT 1
"""
CAU_HINH = ["datTruocToiThieuPhut", "soNgayDatTruocToiDa", "soLichHieuLucToiDaMoiHoSo",
            "soLichHieuLucToiDaMoiSoDienThoai", "huyDoiTruocToiThieuPhut", "soLanDoiLichToiDa",
            "guiYeuCauTruocToiThieuGio", "soNgayXepTruocToiDa", "soGioToiDaMoiTuan", "soNgayDuocSuaDanhGia",
            "phienBan"]
# Khung 1 giờ còn trống của bác sĩ 1 từ ngày mai, mỗi khung 1 giờ bắt đầu khác nhau
KHUNG_TRONG = """
SELECT c.id_lich_lam_viec, DATE_FORMAT(MIN(k.gio_bat_dau), '%Y-%m-%dT%H:%i:%s')
FROM khung_gio_kham k JOIN lich_lam_viec c ON c.id_lich_lam_viec = k.id_lich_lam_viec
WHERE c.id_bac_si = 1 AND k.trang_thai = 'CON_TRONG' AND k.gio_bat_dau >= CURDATE() + INTERVAL 1 DAY
  AND MOD(TIMESTAMPDIFF(MINUTE, TIMESTAMP(c.ngay_lam_viec, c.gio_bat_dau), k.gio_bat_dau), 60) = 0
GROUP BY c.id_lich_lam_viec, k.gio_bat_dau
ORDER BY MIN(k.gio_bat_dau) LIMIT 12
"""
MA = re.compile(r"ECL-\d{8}-\d{4}")
BAY_GIO = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d+$")


class KichBan:
    def __init__(self):
        self.buoc, self.ma = [], {}
        self.khung = [tuple(d) for d in chung.sql_dong(KHUNG_TRONG)]
        if len(self.khung) < 8:
            raise SystemExit(f"Bản dump chỉ còn {len(self.khung)} khung giờ trống của bác sĩ 1")

    def che(self, gt):
        if isinstance(gt, dict):
            return {k: self.che(v) for k, v in gt.items()}
        if isinstance(gt, list):
            return [self.che(v) for v in gt]
        if isinstance(gt, str):
            if BAY_GIO.match(gt):
                return "<bay-gio>"
            for ma in sorted(self.ma, key=len, reverse=True):
                gt = gt.replace(ma, self.ma[ma])
            return MA.sub(lambda m: self.ma.setdefault(m.group(0), f"<ma-{len(self.ma) + 1}>"), gt)
        return gt

    def goi(self, ten, method, duong_dan, body=None, token=None):
        status, _, tho = chung.goi(method, duong_dan, body, token=token)
        than = json.loads(tho) if tho else None
        du_lieu = than.get("duLieu") if isinstance(than, dict) else None
        if isinstance(du_lieu, dict) and du_lieu.get("maPhieuKham"):
            self.ma.setdefault(du_lieu["maPhieuKham"], f"<ma-{len(self.ma) + 1}>")
        self.buoc.append({"buoc": ten, "status": status, "than": self.che(than)})
        print(f"  {status} {ten}")
        return du_lieu

    def tai_anh(self, ten, duong_dan, truong, token):
        """POST multipart 1 ảnh PNG nhỏ; url ảnh trong response được che (khác mỗi lần tải)."""
        ranh_gioi = uuid.uuid4().hex
        phan = [f'--{ranh_gioi}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode("utf-8")
                for k, v in truong.items()]
        phan.append(f'--{ranh_gioi}\r\nContent-Disposition: form-data; name="anh"; filename="thu.png"\r\n'
                    f'Content-Type: image/png\r\n\r\n'.encode("utf-8") + anh_png() + b"\r\n")
        yc = urllib.request.Request(chung.API + duong_dan, data=b"".join(phan) + f"--{ranh_gioi}--\r\n".encode(),
                                    method="POST", headers={"Authorization": "Bearer " + token, "Accept": "application/json",
                                                            "Content-Type": f"multipart/form-data; boundary={ranh_gioi}"})
        try:
            with urllib.request.urlopen(yc, timeout=120) as ph:
                status, tho = ph.status, ph.read()
        except urllib.error.HTTPError as loi:
            status, tho = loi.code, loi.read()
        than = json.loads(tho) if tho else None
        du_lieu = than.get("duLieu") if isinstance(than, dict) else None
        if isinstance(du_lieu, dict) and du_lieu.get("url"):
            self.ma.setdefault(du_lieu["url"], "<url-anh-moi>")
        self.buoc.append({"buoc": ten, "status": status, "than": self.che(than)})
        print(f"  {status} {ten}")
        return du_lieu

    def dat(self, ten, i, benh_nhan, giam_ho=None):
        id_ca, gio = self.khung[i]
        body = {"idLichLamViec": int(id_ca), "gioBatDauKhung": gio, "benhNhan": benh_nhan, "lyDoKham": "Khám tổng quát"}
        if giam_ho:
            body["nguoiGiamHo"] = giam_ho
        return self.goi(ten, "POST", "/api/booking/lich-hen", body)


def anh_png(canh=16):
    """Ảnh PNG 1 màu cạnh {canh} px."""
    def khoi(loai, du_lieu):
        return struct.pack(">I", len(du_lieu)) + loai + du_lieu + struct.pack(">I", zlib.crc32(loai + du_lieu))
    dong = b"".join(b"\x00" + b"\x0f\x76\x6e" * canh for _ in range(canh))
    return (b"\x89PNG\r\n\x1a\n" + khoi(b"IHDR", struct.pack(">IIBBBBB", canh, canh, 8, 2, 0, 0, 0))
            + khoi(b"IDAT", zlib.compress(dong)) + khoi(b"IEND", b""))


def tre(ho_ten="Lê Gia Bảo", ngay_sinh="2017-03-15"):
    return {"hoTen": ho_ten, "ngaySinh": ngay_sinh, "gioiTinh": "NAM", "soDienThoai": "0977000101"}


def giam_ho(ho_ten="Lê Văn Hùng", so_dien_thoai="0977000101", ngay_sinh=None):
    gh = {"hoTen": ho_ten, "quanHe": "CHA", "soDienThoai": so_dien_thoai, "cccd": CCCD_GIAM_HO}
    if ngay_sinh:
        gh["ngaySinh"] = ngay_sinh
    return gh


def kich_ban(kb, moc, bi_tu_choi):
    qt = chung.dang_nhap(moc["taiKhoan"]["quanTri"]["email"], moc["taiKhoan"]["quanTri"]["matKhau"])
    bs = chung.dang_nhap(moc["taiKhoan"]["bacSi"]["email"], moc["taiKhoan"]["bacSi"]["matKhau"])

    # Người giám hộ: khai lần đầu (chưa có ngày sinh) -> điền ngày sinh -> khác tên -> khác ngày sinh đã điền
    ma = [kb.dat("1-khai-lan-dau", 0, tre(), giam_ho())]
    ma.append(kb.dat("2-dien-ngay-sinh", 1, tre(), giam_ho(so_dien_thoai="0977000102", ngay_sinh="1985-05-05")))
    ma.append(kb.dat("3-khac-ten", 2, tre(), giam_ho(ho_ten="Lê Văn Tâm", ngay_sinh="1985-05-05")))
    ma.append(kb.dat("4-khac-ngay-sinh", 3, tre(), giam_ho(ngay_sinh="1986-06-06")))
    id_tre = mot("SELECT id_ho_so_benh_nhan FROM ho_so_benh_nhan WHERE ho_ten = 'Lê Gia Bảo'")
    # Sửa hồ sơ trẻ chưa có CCCD: khoá nhận diện tính lại từ CCCD người giám hộ đầu tiên, lần đặt sau về đúng hồ sơ
    kb.goi("5-sua-ho-so-tre", "PUT", f"/api/booking/quan-tri/ho-so-benh-nhan/{id_tre}",
           {"hoTen": "Lê Gia Bảo An", "ngaySinh": "2017-03-16", "gioiTinh": "NAM", "soDienThoai": "0977000101"},
           token=qt)
    ma.append(kb.dat("5-dat-sau-khi-sua", 4, tre("Lê Gia Bảo An", "2017-03-16"), giam_ho(ngay_sinh="1985-05-05")))
    kb.goi("5-lich-hen-cua-ho-so-tre", "GET", f"/api/booking/quan-tri/ho-so-benh-nhan/{id_tre}/lich-hen", token=qt)
    for i, d in enumerate(ma, 1):
        if d:
            kb.goi(f"6-phieu-kham-{i}", "GET", f"/api/booking/phieu-kham/{d['maPhieuKham']}")
    kb.goi("6-quan-tri-theo-cccd-giam-ho", "GET", f"/api/booking/quan-tri/lich-hen?tuKhoa={CCCD_GIAM_HO}", token=qt)
    kb.goi("6-quan-tri-theo-sdt-giam-ho", "GET", "/api/booking/quan-tri/lich-hen?tuKhoa=0977000101", token=qt)
    kb.goi("6-bac-si-theo-cccd-giam-ho", "GET", f"/api/booking/bac-si/toi/lich-hen/theo-cccd?cccd={CCCD_GIAM_HO}",
           token=bs)

    # Từ chối liên kết: dòng có sẵn trong dump, rồi 1 lần từ chối mới của cùng tài khoản với hồ sơ khác
    email, cccd_cu = bi_tu_choi
    tk = chung.dang_nhap(email, "Demo@123")

    def ho_so_cua_toi(ten, cccd):
        kb.goi(ten, "PUT", "/api/booking/ho-so-benh-nhan/cua-toi",
               {"hoTen": "Người Khác Hẳn", "ngaySinh": "1990-01-01", "gioiTinh": "NU", "soDienThoai": "0977000303",
                "cccd": cccd}, token=tk)

    nguoi_lon = kb.dat("7-dat-nguoi-lon", 5, {"hoTen": "Trần Thị Dung", "ngaySinh": "1980-02-02", "gioiTinh": "NU",
                                  "soDienThoai": "0977000202", "cccd": CCCD_NGUOI_LON})
    id_nguoi_lon = mot(f"SELECT id_ho_so_benh_nhan FROM ho_so_benh_nhan WHERE cccd = '{CCCD_NGUOI_LON}'")
    ho_so_cua_toi("7-gan-ho-so-da-tu-choi-trong-dump", cccd_cu)
    ho_so_cua_toi("8-gan-ho-so-moi-cho-xac-minh", CCCD_NGUOI_LON)
    kb.goi("8-tu-choi", "POST", f"/api/booking/quan-tri/ho-so-benh-nhan/{id_nguoi_lon}/tu-choi",
           {"lyDo": "Giấy tờ không khớp"}, token=qt)
    ho_so_cua_toi("8-gan-lai-ho-so-vua-tu-choi", CCCD_NGUOI_LON)
    ho_so_cua_toi("8-gan-lai-ho-so-tu-choi-trong-dump", cccd_cu)
    kb.goi("8-cho-xac-minh", "GET", "/api/booking/quan-tri/ho-so-benh-nhan/cho-xac-minh", token=qt)
    ghi_lich_su(kb, moc, qt, bs, ma[0], nguoi_lon)


def ghi_lich_su(kb, moc, qt, bs, lich_dau, nguoi_lon):
    """Phase 3: ghi mỗi loại lịch sử qua API rồi đọc lại."""
    # Lịch hẹn: khách hủy bằng link phiếu khám, đổi lịch sang khung khác
    if nguoi_lon:
        kb.goi("9-huy-theo-phieu", "POST", f"/api/booking/phieu-kham/{nguoi_lon['maPhieuKham']}/huy",
               {"lyDo": "Bận việc gia đình", "soDienThoai": "0977000202"})
    if lich_dau:
        id_ca, gio = kb.khung[6]
        kb.goi("9-doi-lich-theo-phieu", "POST", f"/api/booking/phieu-kham/{lich_dau['maPhieuKham']}/doi-lich",
               {"idLichLamViec": int(id_ca), "gioBatDauKhung": gio, "soDienThoai": "0977000101"})

    # Ca làm việc: tạo 1 ca vào ngày đầu tiên bác sĩ 1 chưa có ca, sửa rồi hủy ca của khung[7]
    phong, ngay = chung.sql_dong(NGAY_TRONG)[0]
    truoc_tao = mot("SELECT MAX(id_lich_lam_viec) FROM lich_lam_viec")
    kb.goi("10-tao-ca", "POST", "/api/booking/quan-tri/lich-lam-viec",
           {"idBacSi": 1, "idPhongKham": int(phong), "ngay": ngay, "gioBatDau": "08:00:00", "gioKetThuc": "11:00:00",
            "soLuotToiDaMoiGio": 4, "thoiLuongLuotPhut": 15}, token=qt)
    ca_moi = mot("SELECT MAX(id_lich_lam_viec) FROM lich_lam_viec")
    id_ca = int(kb.khung[7][0])
    phong, bat_dau, ket_thuc, so_luot, thoi_luong = chung.sql_dong(
        "SELECT id_phong_kham, TIME_FORMAT(gio_bat_dau, '%H:%i:%s'), TIME_FORMAT(gio_ket_thuc, '%H:%i:%s'), "
        f"so_luot_toi_da_moi_gio, thoi_luong_luot_phut FROM lich_lam_viec WHERE id_lich_lam_viec = {id_ca}")[0]
    kb.goi("10-sua-ca", "PUT", f"/api/booking/quan-tri/lich-lam-viec/{id_ca}",
           {"idPhongKham": int(phong), "gioBatDau": bat_dau, "gioKetThuc": ket_thuc,
            "soLuotToiDaMoiGio": min(int(so_luot) + 1, 60), "thoiLuongLuotPhut": int(thoi_luong)}, token=qt)
    kb.goi("10-huy-ca", "POST", f"/api/booking/quan-tri/lich-lam-viec/{id_ca}/huy",
           {"lyDo": "Bác sĩ bận đột xuất"}, token=qt)
    for id_xem in ([ca_moi] if ca_moi != truoc_tao else []) + [id_ca]:
        kb.goi(f"10-lich-su-ca-{id_xem}", "GET", f"/api/booking/quan-tri/lich-lam-viec/{id_xem}/lich-su", token=qt)

    # Cấu hình: lưu lại với 1 trường đổi
    cau_hinh = kb.goi("11-cau-hinh", "GET", "/api/booking/quan-tri/cau-hinh", token=qt)
    if cau_hinh:
        body = {k: cau_hinh[k] for k in CAU_HINH}
        body["soNgayDuocSuaDanhGia"] += 1
        # CHUAN_BI nâng 2 giới hạn lên 100 bằng SQL: đưa về trong khoảng API cho phép
        body["soLichHieuLucToiDaMoiHoSo"] = min(body["soLichHieuLucToiDaMoiHoSo"], 20)
        body["soLichHieuLucToiDaMoiSoDienThoai"] = min(body["soLichHieuLucToiDaMoiSoDienThoai"], 50)
        kb.goi("11-luu-cau-hinh", "PUT", "/api/booking/quan-tri/cau-hinh", body, token=qt)
    kb.goi("11-lich-su-cau-hinh", "GET", "/api/booking/quan-tri/cau-hinh/lich-su?kichThuoc=100", token=qt)

    # Sửa bệnh án: hồ sơ đã có 1 lần sửa trong dump, thêm 1 lần nữa
    duong_dan = f"/api/medical/bac-si/toi/lich-hen/{ID_LICH_HEN_SUA_BENH_AN}/benh-an"
    ba = kb.goi("12-benh-an", "GET", duong_dan, token=bs)
    if ba:
        don = [{k: d[k] for k in ("idThuoc", "lieuDung", "soLanMoiNgay", "soNgayDung", "soLuong", "ghiChuSuDung")}
               for d in ba.get("donThuoc") or []]

        def sua(ten, ma_icd, ghi_chu, ly_do, phien_ban):
            return kb.goi(ten, "PUT", duong_dan,
                          {"lyDoKham": ba["lyDoKham"], "sinhHieu": ba["sinhHieu"], "khamLamSang": ba["khamLamSang"],
                           "chanDoan": ba["chanDoan"], "maIcdChinh": ma_icd, "chanDoanKemTheo": ba["chanDoanKemTheo"],
                           "ghiChu": ghi_chu, "ngayTaiKhamDeXuat": ba["ngayTaiKhamDeXuat"], "donThuoc": don,
                           "lyDoSua": ly_do, "phienBan": phien_ban}, token=bs)

        ghi_chu = (ba["ghiChu"] or "") + " Hẹn tái khám nếu sốt."
        # Phase 5: mã ICD không có trong danh mục -> 400, hồ sơ không đổi
        sua("12-sua-benh-an-icd-sai", "ZZ9.9", ghi_chu, "Thử mã sai", ba["phienBan"])
        moi = sua("12-sua-benh-an", (ba.get("icdChinh") or {}).get("ma"), ghi_chu, "Bổ sung ghi chú", ba["phienBan"])
        # Phase 5: mã chữ thường được chuẩn hoá, response mang tên mã (I10 mạn tính)
        if moi:
            sua("12-sua-benh-an-icd-chu-thuong", "i10", ghi_chu, "Đổi mã ICD", moi["phienBan"])
    kb.goi("12-lich-su-bac-si", "GET", f"{duong_dan}/lich-su", token=bs)
    kb.goi("12-lich-su-quan-tri", "GET",
           f"/api/medical/quan-tri/lich-hen/{ID_LICH_HEN_SUA_BENH_AN}/benh-an/lich-su", token=qt)


def anh_bac_si(kb, moc):
    """Phase 5: ảnh giới thiệu của bác sĩ 1 qua mọi route ghi ảnh, rồi trang công khai."""
    qt = chung.dang_nhap(moc["taiKhoan"]["quanTri"]["email"], moc["taiKhoan"]["quanTri"]["matKhau"])
    bs = chung.dang_nhap(moc["taiKhoan"]["bacSi"]["email"], moc["taiKhoan"]["bacSi"]["matKhau"])
    goc = f"/api/catalog/quan-tri/bac-si/{ID_BAC_SI_ANH}"
    ho_so = kb.goi("14-ho-so-quan-tri", "GET", goc, token=qt) or {}
    anh = ho_so.get("anh") or []
    moi = kb.tai_anh("14-them-anh", f"{goc}/anh", {"loai": "ANH_CONG_VIEC", "chuThich": "Ảnh thử p5",
                                                    "chuThichEn": "Smoke photo p5"}, qt)
    cong_viec = [a["id"] for a in anh if a["loai"] == "ANH_CONG_VIEC"]
    chung_chi = [a["id"] for a in anh if a["loai"] == "CHUNG_CHI"]
    if cong_viec:
        kb.goi("14-sua-anh-quan-tri", "PUT", f"{goc}/anh/{cong_viec[-1]}",
               {"loai": "ANH_CONG_VIEC", "chuThich": "Phòng khám buổi sáng", "chuThichEn": "Morning clinic"}, token=qt)
    if moi:
        thu_tu = [a["id"] for a in anh] + [moi["id"]]
        kb.goi("14-sap-xep-anh", "PUT", f"{goc}/anh/thu-tu", {"idAnh": thu_tu[::-1]}, token=qt)
        kb.goi("14-xoa-anh-moi", "DELETE", f"{goc}/anh/{moi['id']}", token=qt)
    if cong_viec:
        kb.goi("14-bac-si-sua-anh", "PUT", f"/api/catalog/bac-si/toi/anh/{cong_viec[0]}",
               {"chuThich": "Khám cho người bệnh", "chuThichEn": "Seeing a patient"}, token=bs)
    if chung_chi:
        kb.goi("14-bac-si-sua-chung-chi", "PUT", f"/api/catalog/bac-si/toi/anh/{chung_chi[0]}",
               {"chuThich": "Không được sửa", "chuThichEn": "Not allowed"}, token=bs)
    if len(cong_viec) > 1:
        kb.goi("14-bac-si-xoa-anh", "DELETE", f"/api/catalog/bac-si/toi/anh/{cong_viec[-1]}", token=bs)
    kb.goi("14-ho-so-cua-toi", "GET", "/api/catalog/bac-si/toi/ho-so", token=bs)
    kb.goi("14-chi-tiet-cong-khai", "GET", f"/api/catalog/bac-si/{ID_BAC_SI_ANH}")


def anh_bac_si_db():
    """Ảnh của bác sĩ 1 theo thứ tự hiển thị, trừ ngay_tao: từ bảng cũ nếu còn, không thì từ JSON trên bac_si."""
    if co_bang("anh_bac_si"):
        return sql_json("SELECT JSON_ARRAY(id_anh_bac_si, loai, url, ma_luu_tru, chu_thich, chu_thich_en, thu_tu) "
                        f"FROM anh_bac_si WHERE id_bac_si = {ID_BAC_SI_ANH} ORDER BY thu_tu, id_anh_bac_si")
    return sql_json("SELECT JSON_ARRAY(j.id, j.loai, j.url, j.ma_luu_tru, j.chu_thich, j.chu_thich_en, j.thu_tu) "
                    "FROM bac_si b, JSON_TABLE(b.anh_gioi_thieu, '$[*]' COLUMNS (id BIGINT PATH '$.id', "
                    "loai VARCHAR(20) PATH '$.loai', url VARCHAR(500) PATH '$.url', "
                    "ma_luu_tru VARCHAR(150) PATH '$.maLuuTru', chu_thich VARCHAR(200) PATH '$.chuThich', "
                    "chu_thich_en VARCHAR(200) PATH '$.chuThichEn', thu_tu INT PATH '$.thuTu')) j "
                    f"WHERE b.id_bac_si = {ID_BAC_SI_ANH} ORDER BY j.thu_tu, j.id")


def doc_lich_su(kb, qt, tu_id_lich_hen):
    """Phase 3: chi tiết quản trị (có dòng thời gian lịch sử) của mọi lịch hẹn mới."""
    for (id_lh,) in chung.sql_dong(f"SELECT id_lich_hen FROM lich_hen WHERE id_lich_hen > {tu_id_lich_hen} "
                                   "ORDER BY id_lich_hen"):
        kb.goi(f"13-chi-tiet-quan-tri-{id_lh}", "GET", f"/api/booking/quan-tri/lich-hen/{id_lh}", token=qt)


def dem_lich_su():
    """Số dòng mỗi loại lịch sử trước khi app lên (bảng cũ, schema V36)."""
    return {loai: int(mot(f"SELECT COUNT(*) FROM {bang}")) for loai, (bang, *_) in LICH_SU.items()}


def lich_su_moi(da_co):
    """Các dòng lịch sử ghi trong lần chạy (bỏ da_co dòng đầu của mỗi loại theo id), trừ id và ngay_tao."""
    ket_qua = {}
    for loai, (bang, khoa, cot_cu, cot_moi) in LICH_SU.items():
        if co_bang(bang):
            cau = f"SELECT JSON_ARRAY({cot_cu}) FROM {bang} ORDER BY {khoa} LIMIT {da_co[loai]}, 1000000"
        else:
            cau = (f"SELECT JSON_ARRAY({cot_moi}) FROM lich_su_thay_doi WHERE loai = '{loai}' "
                   f"ORDER BY id_lich_su LIMIT {da_co[loai]}, 1000000")
        ket_qua[loai] = sql_json(cau)
    return ket_qua


def trang_thai_db(tu_id_lich_hen, da_co):
    if co_bang("nguoi_giam_ho"):
        giam_ho_moi = sql_json(
            "SELECT JSON_ARRAY(l.id_lich_hen, l.id_ho_so_benh_nhan, g.ho_ten, g.quan_he, g.so_dien_thoai, g.cccd, "
            f"g.ngay_sinh, l.can_doi_chieu) FROM lich_hen l LEFT JOIN nguoi_giam_ho g "
            f"ON g.id_nguoi_giam_ho = l.id_nguoi_giam_ho WHERE l.id_lich_hen > {tu_id_lich_hen}")
    else:
        giam_ho_moi = sql_json(
            "SELECT JSON_ARRAY(id_lich_hen, id_ho_so_benh_nhan, giam_ho_ho_ten, giam_ho_quan_he, "
            f"giam_ho_so_dien_thoai, giam_ho_cccd, giam_ho_ngay_sinh, can_doi_chieu) FROM lich_hen "
            f"WHERE id_lich_hen > {tu_id_lich_hen}")
    if co_bang("lien_ket_ho_so_bi_tu_choi"):
        tu_choi = sql_json("SELECT JSON_ARRAY(id_tai_khoan, id_ho_so_benh_nhan, ly_do) FROM lien_ket_ho_so_bi_tu_choi")
    else:
        tu_choi = sql_json("SELECT JSON_ARRAY(j.id_tai_khoan, h.id_ho_so_benh_nhan, j.ly_do) FROM ho_so_benh_nhan h, "
                           "JSON_TABLE(h.tu_choi_lien_ket, '$[*]' COLUMNS (id_tai_khoan BIGINT PATH '$.idTaiKhoan', "
                           "ly_do VARCHAR(500) PATH '$.lyDo')) j")
    ho_so_moi = sql_json("SELECT JSON_ARRAY(id_ho_so_benh_nhan, cccd, ho_ten, ngay_sinh, khoa_nhan_dien IS NOT NULL, "
                         "id_tai_khoan, trang_thai_lien_ket) FROM ho_so_benh_nhan "
                         f"WHERE ho_ten LIKE 'Lê Gia Bảo%' OR cccd = '{CCCD_NGUOI_LON}'")
    return {"giamHoCuaLichHenMoi": sorted(giam_ho_moi), "tuChoiLienKet": sorted(tu_choi),
            "hoSoMoi": sorted(ho_so_moi), "lichSuMoi": lich_su_moi(da_co)}


def main():
    doi_so = sys.argv[1:]
    if not doi_so or doi_so[0].startswith("-"):
        raise SystemExit(__doc__)
    ten, truoc, build = doi_so[0], "--truoc" in doi_so, "--build" in doi_so
    thu_muc = chung.THU_MUC / ten
    chung.xoa_thu_muc(thu_muc)
    moc = chung.doc_moc()
    chung.tat()
    try:
        chung.len_mysql()
        chung.khoi_phuc(chung.TEP_DUMP)
        chung.sql(CHUAN_BI)
        kb = KichBan()
        # Đọc trên schema V36 (bảng cũ còn): tài khoản bị từ chối đầu tiên và CCCD hồ sơ nó bị từ chối
        bi_tu_choi = tuple(chung.sql_dong(
            "SELECT t.email, h.cccd FROM lien_ket_ho_so_bi_tu_choi r JOIN tai_khoan t ON t.id_tai_khoan = r.id_tai_khoan "
            "JOIN ho_so_benh_nhan h ON h.id_ho_so_benh_nhan = r.id_ho_so_benh_nhan "
            "ORDER BY r.id_tai_khoan, r.id_lien_ket_bi_tu_choi LIMIT 1")[0])
        tu_id_lich_hen = int(mot("SELECT MAX(id_lich_hen) FROM lich_hen"))
        da_co = dem_lich_su()
        da_co_hang_cho = dem_hang_cho()
        chung.len_app(truoc=truoc, build=build, moi_truong=chung.JOB_TAT)
        kich_ban(kb, moc, bi_tu_choi)
        doc_lich_su(kb, chung.dang_nhap(moc["taiKhoan"]["quanTri"]["email"], moc["taiKhoan"]["quanTri"]["matKhau"]),
                    tu_id_lich_hen)
        anh_bac_si(kb, moc)
        # Theo tên bước (tên không trùng) để cho-phep.json chỉ đúng bước, không theo số thứ tự
        buoc = {b["buoc"]: {"status": b["status"], "than": b["than"]} for b in kb.buoc}
        assert len(buoc) == len(kb.buoc), "tên bước bị trùng"
        than = {"buoc": buoc, "db": trang_thai_db(tu_id_lich_hen, da_co)}
        than["db"]["hangChoMoi"] = {bang: kb.che(hang_cho_moi(bang, da_co_hang_cho, {"ngay_tao", "ngay_gui"}))
                                    for bang in ("su_kien_thong_bao", "email_gui")}
        than["db"]["anhBacSi1"] = anh_bac_si_db()
        ghi = {"vaiTro": "ghi", "route": "GHI", "duongDan": "_ghi", "ngonNgu": "-", "status": 0, "than": than}
        (thu_muc / "_ghi.json").write_text(json.dumps(ghi, ensure_ascii=False, indent=1, sort_keys=True),
                                           encoding="utf-8")
        print(f"Đã ghi {thu_muc / '_ghi.json'}")
    finally:
        chung.tat()


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
