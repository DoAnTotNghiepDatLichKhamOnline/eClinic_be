"""Gọi các route tài khoản và đặt lịch phía bệnh nhân trên 1 stack đang chạy (có dữ liệu mẫu) và lưu request + response thật
vào temp/api-doc/mau/NN-ten.json. JSON mẫu trong tài liệu lấy từ các tệp này.

Chỉ chạy với stack thử (docker compose -p eclinic-smoke, SEED_DATA=true): script tạo và hủy lịch hẹn.

    python scripts/api-doc/lay_mau.py [http://127.0.0.1:8080]
"""

import json
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import date, timedelta
from pathlib import Path

GOC = Path(__file__).resolve().parents[2]
THU_MUC = GOC / "temp/api-doc/mau"
API = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8080"
MA_GIA = "A" * 43
MAT_KHAU = "MatKhau@123"
dem = 0
cookie_cuoi = email_cuoi = None  # cookie eclinic_rt của lần gọi gần nhất có Set-Cookie; email của tài khoản vừa đăng ký


def goi(ten, method, duong_dan, body=None, token=None, mong=None, cookie=None, ngon_ngu=None, khach=None):
    """Gọi 1 route, lưu kết quả, trả (status, body đã parse hoặc None). mong: status phải nhận được.
    ngon_ngu: giá trị header Accept-Language (không truyền = không gửi header, tức tiếng Việt)."""
    global dem, cookie_cuoi
    dem += 1
    headers = {"Accept": "application/json"}
    du_lieu = None
    if body is not None:
        du_lieu = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = "Bearer " + token
    if cookie:
        headers["Cookie"] = "eclinic_rt=" + cookie
    if ngon_ngu:
        headers["Accept-Language"] = ngon_ngu
    if khach:
        headers["X-Chat-Khach"] = khach
    yeu_cau = urllib.request.Request(API + duong_dan, data=du_lieu, method=method, headers=headers)
    try:
        with urllib.request.urlopen(yeu_cau, timeout=40) as phan_hoi:
            status, tho, dau = phan_hoi.status, phan_hoi.read(), phan_hoi.headers
    except urllib.error.HTTPError as loi:
        status, tho, dau = loi.code, loi.read(), loi.headers
    phien = re.search(r"eclinic_rt=([^;]+)", dau.get("Set-Cookie") or "")
    cookie_cuoi = phien.group(1) if phien else cookie_cuoi
    la_json = "json" in (dau.get("Content-Type") or "")
    ket_qua = json.loads(tho) if la_json and tho else None
    ghi = {"method": method, "path": duong_dan, "acceptLanguage": ngon_ngu, "requestBody": body, "status": status,
           "headers": {k: dau.get(k) for k in ("Content-Type", "Content-Disposition") if dau.get(k)},
           "body": ket_qua if la_json else f"<{len(tho)} byte>"}
    THU_MUC.mkdir(parents=True, exist_ok=True)
    (THU_MUC / f"{dem:02d}-{ten}.json").write_text(json.dumps(ghi, ensure_ascii=False, indent=2), encoding="utf-8")
    ma_loi = (ket_qua or {}).get("maLoi", "")
    dat = mong is None or status == mong
    print(f"{'ok  ' if dat else 'SAI '} {dem:02d} {status} {ma_loi:32} {method} {duong_dan[:70]}")
    if not dat:
        print("     mong", mong, "nhận", json.dumps(ket_qua, ensure_ascii=False)[:300])
    return status, ket_qua


def xoa_gioi_han_ip(mau="dat-lich:ip:*"):
    """Xoá bộ đếm 20 lần / 10 phút / IP trên Redis của stack thử, để script gọi được nhiều hơn 20 lần đặt / đổi / hủy.
    mau: mẫu khoá khác (cap-lai:*, so-kham:sai:*) để chạy lại script trong cửa sổ đếm."""
    quet = subprocess.run(["docker", "exec", "eclinic-redis", "redis-cli", "--scan", "--pattern", mau],
                          capture_output=True, text=True, check=True).stdout.split()
    if quet:
        subprocess.run(["docker", "exec", "eclinic-redis", "redis-cli", "DEL", *quet], capture_output=True, check=True)


def sql(cau):
    """Chạy 1 câu SQL trên MySQL của stack thử (chỉ để dựng tình huống mà API không tạo được trong 1 lần chạy)."""
    subprocess.run(["docker", "exec", "eclinic-mysql", "sh", "-c",
                    'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "$0"', cau],
                   capture_output=True, text=True, check=True)


def doi_ngay_luot_kham(ma_tra_cuu, so_ngay):
    """Dời lượt khám của 1 lịch hẹn đi so_ngay ngày (âm = lùi). Bác sĩ chỉ ghi kết quả được cho lượt khám từ hôm nay
    trở về trước, và hạn hủy / đổi chỉ qua khi gần giờ khám: cả hai không chờ được trong 1 lần chạy."""
    sql("UPDATE khung_gio_kham k JOIN lich_hen l ON l.id_khung_gio = k.id_khung_gio "
        f"SET k.gio_bat_dau = k.gio_bat_dau + INTERVAL {so_ngay} DAY, "
        f"k.gio_ket_thuc = k.gio_ket_thuc + INTERVAL {so_ngay} DAY WHERE l.ma_tra_cuu = '{ma_tra_cuu}'")


def dang_nhap(ten, email, mat_khau):
    return goi(ten, "POST", "/api/auth/login", {"email": email, "matKhau": mat_khau}, mong=200)[1]["duLieu"][
        "accessToken"]


def tai_khoan_moi(ten, ho_ten, sdt, cccd=None):
    """Đăng ký 1 tài khoản bệnh nhân, xác thực bằng token trong log của identity-service (MAIL_MODE=console)."""
    global email_cuoi
    email = email_cuoi = f"{ten}.{int(time.time())}@example.com"
    goi(f"dang-ky-{ten}", "POST", "/api/auth/register",
        {"hoTen": ho_ten, "email": email, "matKhau": MAT_KHAU, "soDienThoai": sdt, "cccd": cccd}, mong=201)
    for _ in range(20):
        log = subprocess.run(["docker", "logs", "--tail", "200", "eclinic-identity-service"], capture_output=True,
                             text=True, encoding="utf-8", errors="replace")
        tim = re.findall(re.escape(email) + r".*?verify-email\?token=([\w-]+)", log.stdout + log.stderr)
        if tim:
            break
        time.sleep(1)
    assert tim, "Không thấy link xác thực trong log của identity-service"
    goi(f"xac-thuc-{ten}", "POST", "/api/auth/verify-email", {"token": tim[-1]}, mong=200)
    return dang_nhap(f"dang-nhap-{ten}", email, MAT_KHAU)


def nguoi(ho_ten, ngay_sinh, sdt, cccd=None, **khac):
    return {"hoTen": ho_ten, "ngaySinh": ngay_sinh, "gioiTinh": "NAM", "soDienThoai": sdt, "cccd": cccd, **khac}


def main():
    for tep in THU_MUC.glob("*.json") if THU_MUC.exists() else ():
        tep.unlink()

    # --- 11. Chuyên khoa và bác sĩ ---
    _, ck = goi("chuyen-khoa-ds", "GET", "/api/catalog/chuyen-khoa?trang=0&kichThuoc=3", mong=200)
    goi("chuyen-khoa-tim", "GET", "/api/catalog/chuyen-khoa?tuKhoa=tim", mong=200)
    _, som = (None, None)
    id_ck = None
    for muc in goi("chuyen-khoa-tat-ca", "GET", "/api/catalog/chuyen-khoa?kichThuoc=100", mong=200)[1]["duLieu"]["noiDung"]:
        _, som = goi("ngay-som-nhat", "GET", f"/api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa={muc['id']}", mong=200)
        if len(som["duLieu"]) >= 2:
            id_ck = muc["id"]
            break
    assert id_ck, "Không chuyên khoa nào có từ 2 bác sĩ còn chỗ"
    goi("chuyen-khoa-chi-tiet", "GET", f"/api/catalog/chuyen-khoa/{id_ck}", mong=200)
    goi("bac-si-ds", "GET", f"/api/catalog/bac-si?idChuyenKhoa={id_ck}&kichThuoc=2", mong=200)
    id_bs = som["duLieu"][0]["idBacSi"]
    goi("bac-si-chi-tiet", "GET", f"/api/catalog/bac-si/{id_bs}", mong=200)
    goi("loi-bac-si-khong-co", "GET", "/api/catalog/bac-si/999999", mong=404)

    # --- 12. Khung giờ ---
    # Chọn ngày từ ngày mai để khung giờ không bị hạn "đặt trước 30 phút" cắt giữa chừng
    mai = (date.today() + timedelta(days=1)).isoformat()
    _, con_cho = goi("ngay-con-cho", "GET",
                     f"/api/booking/khung-gio/ngay-con-cho?idBacSi={id_bs}&tuNgay={mai}", mong=200)
    ngay = next(d["ngay"] for d in con_cho["duLieu"] if d["soChoConLai"] > 0)
    _, ca = goi("khung-gio-theo-ngay", "GET", f"/api/booking/khung-gio?ngay={ngay}&idBacSi={id_bs}", mong=200)
    goi("khung-gio-theo-chuyen-khoa", "GET", f"/api/booking/khung-gio?ngay={ngay}&idChuyenKhoa={id_ck}", mong=200)
    goi("khung-gio-nhieu-ngay", "GET", f"/api/booking/khung-gio/nhieu-ngay?idBacSi={id_bs}&tuNgay={ngay}", mong=200)
    _, gop = goi("khung-gio-gop", "GET", f"/api/booking/khung-gio/gop?idChuyenKhoa={id_ck}&ngay={ngay}", mong=200)
    goi("ngay-con-cho-chuyen-khoa", "GET", f"/api/booking/khung-gio/ngay-con-cho?idChuyenKhoa={id_ck}", mong=200)
    goi("khung-gio-ngay-da-qua", "GET", f"/api/booking/khung-gio?ngay=2020-01-01&idBacSi={id_bs}", mong=200)
    goi("khung-gio-bac-si-khong-co", "GET", f"/api/booking/khung-gio?ngay={ngay}&idBacSi=999999", mong=200)
    goi("loi-khung-gio-thieu-bo-loc", "GET", f"/api/booking/khung-gio?ngay={ngay}", mong=400)
    goi("loi-khung-gio-thieu-ngay", "GET", f"/api/booking/khung-gio?idBacSi={id_bs}", mong=400)
    goi("loi-khung-gio-sai-ngay", "GET", f"/api/booking/khung-gio?ngay=12-10-2026&idBacSi={id_bs}", mong=400)
    xa = (date.fromisoformat(ngay) + timedelta(days=7)).isoformat()
    goi("loi-nhieu-ngay-qua-7", "GET",
        f"/api/booking/khung-gio/nhieu-ngay?idBacSi={id_bs}&tuNgay={ngay}&denNgay={xa}", mong=400)
    goi("loi-token-sai-o-route-cong-khai", "GET", "/api/catalog/chuyen-khoa", token="khong.phai.jwt", mong=401)

    ca_kham = ca["duLieu"][0]
    id_ca = ca_kham["idLichLamViec"]
    khung = [k["gioBatDau"] for k in ca_kham["khungGio"] if not k["hetCho"]]
    assert len(khung) >= 3, "Ca khám cần ít nhất 3 khung giờ còn chỗ"

    # --- 13.1 Đặt lịch ---
    xoa_gioi_han_ip()
    lon = nguoi("Trần Văn Khách", "1990-03-15", "0912345601", "079190000101", email="khach@example.com",
                soBaoHiemYTe="DN4797912345601", diaChi="12 Nguyễn Văn Bảo, Gò Vấp, TP.HCM")
    dat_lon = {"idLichLamViec": id_ca, "gioBatDauKhung": khung[0], "benhNhan": lon,
               "lyDoKham": "Đau đầu, chóng mặt 3 ngày nay"}
    _, dat1 = goi("dat-lich-khach", "POST", "/api/booking/lich-hen", dat_lon, mong=201)
    goi("loi-dat-trung-gio", "POST", "/api/booking/lich-hen", dat_lon, mong=409)
    tre = nguoi("Trần Gia Bảo", "2018-06-01", "0912345601")
    giam_ho = {"hoTen": "Trần Văn Khách", "quanHe": "CHA", "soDienThoai": "0912345601", "cccd": "079190000101"}
    dat_tre = {"idChuyenKhoa": id_ck, "gioBatDauKhung": gop["duLieu"][0]["gioBatDau"], "benhNhan": tre,
               "nguoiGiamHo": giam_ho, "lyDoKham": "Sốt, ho"}
    _, dat2 = goi("dat-lich-khach-tre-em-bac-si-bat-ky", "POST", "/api/booking/lich-hen", dat_tre, mong=201)
    goi("loi-thieu-nguoi-giam-ho", "POST", "/api/booking/lich-hen", {**dat_tre, "nguoiGiamHo": None}, mong=400)
    goi("loi-giam-ho-trung-cccd", "POST", "/api/booking/lich-hen",
        {**dat_tre, "benhNhan": {**tre, "cccd": "079190000101"}}, mong=400)
    goi("loi-body-sai", "POST", "/api/booking/lich-hen",
        {"idLichLamViec": id_ca, "benhNhan": nguoi("", "2090-01-01", "123")}, mong=400)
    goi("loi-gui-ca-hai", "POST", "/api/booking/lich-hen", {**dat_lon, "idChuyenKhoa": id_ck}, mong=400)
    goi("loi-khong-gui-ca-nao", "POST", "/api/booking/lich-hen",
        {k: v for k, v in dat_lon.items() if k != "idLichLamViec"}, mong=400)
    goi("loi-nguoi-lon-thieu-cccd", "POST", "/api/booking/lich-hen",
        {**dat_lon, "benhNhan": {**lon, "cccd": ""}}, mong=400)
    goi("loi-gio-khong-phai-khung", "POST", "/api/booking/lich-hen",
        {**dat_lon, "gioBatDauKhung": khung[1][:14] + "07:00"}, mong=400)
    goi("loi-ca-khong-co", "POST", "/api/booking/lich-hen", {**dat_lon, "idLichLamViec": 999999}, mong=404)
    goi("loi-khung-gio-da-qua", "POST", "/api/booking/lich-hen",
        {**dat_lon, "idLichLamViec": None, "idChuyenKhoa": id_ck, "gioBatDauKhung": "2020-01-01T08:00:00"}, mong=409)

    # --- 14. Phiếu khám ---
    xoa_gioi_han_ip()
    ma1 = dat1["duLieu"]["maPhieuKham"]
    goi("phieu-kham", "GET", f"/api/booking/phieu-kham/{ma1}", mong=200)
    goi("phieu-kham-tre-em", "GET", f"/api/booking/phieu-kham/{dat2['duLieu']['maPhieuKham']}", mong=200)
    goi("qr", "GET", f"/api/booking/phieu-kham/{ma1}/qr", mong=200)
    goi("qr-tai-ve", "GET", f"/api/booking/phieu-kham/{ma1}/qr?kichThuoc=200&taiVe=true", mong=200)
    goi("loi-qr-kich-thuoc", "GET", f"/api/booking/phieu-kham/{ma1}/qr?kichThuoc=50", mong=400)
    goi("loi-phieu-khong-co", "GET", f"/api/booking/phieu-kham/{MA_GIA}", mong=404)
    goi("loi-phieu-sai-dang", "GET", "/api/booking/phieu-kham/123", mong=404)
    goi("loi-qr-khong-co", "GET", f"/api/booking/phieu-kham/{MA_GIA}/qr", mong=404)
    doi = {"idLichLamViec": id_ca, "gioBatDauKhung": khung[1], "soDienThoai": "0912345601"}
    goi("loi-doi-sai-sdt", "POST", f"/api/booking/phieu-kham/{ma1}/doi-lich", {**doi, "soDienThoai": "0999999999"},
        mong=403)
    goi("loi-doi-thieu-sdt", "POST", f"/api/booking/phieu-kham/{ma1}/doi-lich",
        {k: v for k, v in doi.items() if k != "soDienThoai"}, mong=400)
    goi("loi-doi-cung-khung", "POST", f"/api/booking/phieu-kham/{ma1}/doi-lich", {**doi, "gioBatDauKhung": khung[0]},
        mong=400)
    _, doi1 = goi("doi-lich-theo-phieu", "POST", f"/api/booking/phieu-kham/{ma1}/doi-lich", doi, mong=201)
    goi("phieu-kham-cu-sau-khi-doi", "GET", f"/api/booking/phieu-kham/{ma1}", mong=200)
    goi("loi-doi-lich-da-doi", "POST", f"/api/booking/phieu-kham/{ma1}/doi-lich", {**doi, "gioBatDauKhung": khung[2]},
        mong=409)
    ma2 = doi1["duLieu"]["maPhieuKham"]
    _, doi2 = goi("doi-lich-lan-2", "POST", f"/api/booking/phieu-kham/{ma2}/doi-lich",
                  {**doi, "gioBatDauKhung": khung[2]}, mong=201)
    ma3 = doi2["duLieu"]["maPhieuKham"]
    goi("loi-doi-lan-3", "POST", f"/api/booking/phieu-kham/{ma3}/doi-lich", doi, mong=409)
    goi("loi-huy-sai-sdt", "POST", f"/api/booking/phieu-kham/{ma3}/huy", {"soDienThoai": "0999999999"}, mong=403)
    goi("loi-huy-thieu-sdt", "POST", f"/api/booking/phieu-kham/{ma3}/huy", {"lyDo": "Bận"}, mong=400)
    goi("huy-theo-phieu", "POST", f"/api/booking/phieu-kham/{ma3}/huy",
        {"soDienThoai": "0912345601", "lyDo": "Bận việc đột xuất"}, mong=200)
    goi("loi-huy-lan-2", "POST", f"/api/booking/phieu-kham/{ma3}/huy", {"soDienThoai": "0912345601"}, mong=409)
    goi("loi-huy-phieu-khong-co", "POST", f"/api/booking/phieu-kham/{MA_GIA}/huy", {"soDienThoai": "0912345601"},
        mong=404)

    # --- 13. Bệnh nhân đã đăng nhập ---
    xoa_gioi_han_ip()
    _, dn = goi("dang-nhap", "POST", "/api/auth/login",
                {"email": "benhnhan01@eclinic.local", "matKhau": "Demo@123"}, mong=200)
    token = dn["duLieu"]["accessToken"]
    _, toi = goi("ho-so-tai-khoan", "GET", "/api/users/me", token=token, mong=200)
    _, tt0 = goi("thong-tin-dat-lich-truoc", "GET", "/api/booking/thong-tin-dat-lich/cua-toi", token=token, mong=200)
    ban_than = tt0["duLieu"]["banThan"]
    cccd_toi = (ban_than or {}).get("cccd") or "079190000201"
    toi_bn = nguoi(toi["duLieu"]["hoTen"], (ban_than or {}).get("ngaySinh") or "1992-08-20",
                   toi["duLieu"]["soDienThoai"], cccd_toi)
    goi("dat-lich-cho-ban-than", "POST", "/api/booking/lich-hen",
        {"idLichLamViec": id_ca, "gioBatDauKhung": khung[0], "benhNhan": toi_bn, "lyDoKham": "Khám định kỳ",
         "datChoBanThan": True}, token=token, mong=201)
    goi("loi-ban-than-sai-cccd", "POST", "/api/booking/lich-hen",
        {"idLichLamViec": id_ca, "gioBatDauKhung": khung[1], "benhNhan": {**toi_bn, "cccd": "079190000999"},
         "lyDoKham": "Khám định kỳ", "datChoBanThan": True}, token=token, mong=409)
    me = nguoi("Lê Thị Hoa", "1960-02-10", "0912345777", "079160000301", diaChi="5 Lê Lợi, Quận 1, TP.HCM")
    me["gioiTinh"] = "NU"
    goi("dat-lich-cho-nguoi-than", "POST", "/api/booking/lich-hen",
        {"idLichLamViec": id_ca, "gioBatDauKhung": khung[1], "benhNhan": me, "lyDoKham": "Đau khớp gối"},
        token=token, mong=201)
    con = nguoi("Nguyễn Gia Huy", "2015-09-09", toi["duLieu"]["soDienThoai"])
    goi("dat-lich-cho-con", "POST", "/api/booking/lich-hen",
        {"idLichLamViec": id_ca, "gioBatDauKhung": khung[2], "benhNhan": con, "lyDoKham": "Đau bụng",
         "nguoiGiamHo": {"hoTen": toi["duLieu"]["hoTen"], "quanHe": "CHA",
                         "soDienThoai": toi["duLieu"]["soDienThoai"], "cccd": cccd_toi}}, token=token, mong=201)
    _, tt = goi("thong-tin-dat-lich", "GET", "/api/booking/thong-tin-dat-lich/cua-toi", token=token, mong=200)
    nguoi_than = {n["hoTen"]: n for n in tt["duLieu"]["nguoiThan"]}
    id_me = nguoi_than["Lê Thị Hoa"]["id"]
    sua = {"hoTen": "Lê Thị Hoa", "ngaySinh": "1960-02-10", "gioiTinh": "NU", "soDienThoai": "0912345778",
           "email": "hoa@example.com", "soBaoHiemYTe": "HT3797900000301", "diaChi": "5 Lê Lợi, Quận 1, TP.HCM"}
    goi("sua-nguoi-than", "PUT", f"/api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id_me}", sua, token=token,
        mong=200)
    goi("loi-sua-nguoi-than-sai", "PUT", f"/api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id_me}",
        {**sua, "hoTen": ""}, token=token, mong=400)
    goi("loi-sua-nguoi-than-khong-co", "PUT", "/api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/999999", sua,
        token=token, mong=404)
    goi("xoa-nguoi-than", "DELETE", f"/api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id_me}", token=token,
        mong=200)
    goi("loi-xoa-nguoi-than-lan-2", "DELETE", f"/api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id_me}",
        token=token, mong=404)
    goi("loi-thong-tin-khong-token", "GET", "/api/booking/thong-tin-dat-lich/cua-toi", mong=401)

    # --- Vai trò khác ---
    _, ad = goi("dang-nhap-admin", "POST", "/api/auth/login",
                {"email": "admin@eclinic.local", "matKhau": "Admin@123"}, mong=200)
    token_ad = ad["duLieu"]["accessToken"]
    goi("loi-admin-dat-lich", "POST", "/api/booking/lich-hen", dat_lon, token=token_ad, mong=403)
    goi("loi-admin-thong-tin-dat-lich", "GET", "/api/booking/thong-tin-dat-lich/cua-toi", token=token_ad, mong=403)

    cua_toi(token, token_ad, toi_bn, id_ca, khung, ngay, ma1)
    tai_khoan_chua_co_ho_so(id_ca, khung)
    la_nguoi_giam_ho(token, cccd_toi, id_ca, khung[0])
    tai_khoan(token_ad)
    chatbot(token, token_ad, id_ck, id_bs)
    dat_lich_toan_vien(token, token_ad, toi_bn, id_ck, ngay, ma1, dat2["duLieu"]["maPhieuKham"], dat1["duLieu"]["maTraCuu"])

    # --- Khung giờ đủ lượt: đánh dấu mọi lượt còn trống của 1 khung là đã đặt rồi đặt vào khung đó ---
    xoa_gioi_han_ip()
    gio = khung[3].replace("T", " ")
    sql(f"UPDATE khung_gio_kham SET trang_thai = 'DA_DAT' WHERE id_lich_lam_viec = {id_ca} AND trang_thai = 'CON_TRONG' "
        f"AND gio_bat_dau >= '{gio}' AND gio_bat_dau < '{gio}' + INTERVAL 1 HOUR")
    goi("loi-khung-gio-du-luot", "POST", "/api/booking/lich-hen",
        {**dat_lon, "gioBatDauKhung": khung[3], "benhNhan": nguoi("Cao Văn Đủ", "1985-05-05", "0933000011",
                                                                 "079188000011")}, mong=409)

    # --- Giới hạn theo IP: gọi tới khi bị chặn ---
    xoa_gioi_han_ip()
    for lan in range(1, 23):
        status, _ = goi(f"gioi-han-ip-lan-{lan}", "POST", "/api/booking/lich-hen", {**dat_lon, "idLichLamViec": 999999})
        if status == 429:
            print("     bị chặn ở lần gọi thứ", lan)
            break
    xoa_gioi_han_ip()


def cua_toi(token, token_ad, toi_bn, id_ca, khung, ngay, ma_khach):
    """Mục 15-18: lịch hẹn của tôi, lịch sử khám, đánh giá, hồ sơ bệnh nhân, trang cá nhân, thông báo."""
    assert len(khung) >= 4, "Ca khám cần ít nhất 4 khung giờ còn chỗ"
    lh, hs = "/api/booking/lich-hen/cua-toi", "/api/booking/ho-so-benh-nhan/cua-toi"
    ls, tb = "/api/booking/lich-su-kham/cua-toi", "/api/notification/thong-bao"
    so_ngay = (date.fromisoformat(ngay) - date.today()).days
    xoa_gioi_han_ip()

    def dat(ten, gio, ly_do, mong=201):
        return goi(ten, "POST", "/api/booking/lich-hen",
                   {"idLichLamViec": id_ca, "gioBatDauKhung": gio, "benhNhan": toi_bn, "lyDoKham": ly_do,
                    "datChoBanThan": True}, token=token, mong=mong)[1]

    da_kham = dat("dat-lich-se-kham", khung[1], "Ho, sốt nhẹ 2 ngày nay")["duLieu"]
    bi_tu_choi = dat("dat-lich-se-bi-tu-choi", khung[2], "Tái khám")["duLieu"]
    dat("loi-qua-3-lich-dang-giu", khung[3], "Khám thêm", mong=409)
    _, ds = goi("lich-hen-cua-toi", "GET", lh, token=token, mong=200)
    theo_ly_do = {d["lyDoKham"]: d for d in ds["duLieu"]["noiDung"]}
    dinh_ky, cua_me, cua_con = theo_ly_do["Khám định kỳ"], theo_ly_do["Đau khớp gối"], theo_ly_do["Đau bụng"]

    # --- Bác sĩ của ca: hủy 1 lịch, ghi kết quả khám (các route này không nằm trong tài liệu) ---
    # Từ bản 2.3 lịch đặt xong là DA_XAC_NHAN ngay; bác sĩ không còn bước xác nhận / từ chối.
    token_bs = id_lich = None
    for so in range(1, 13):
        token_bs = dang_nhap("dang-nhap-bac-si", f"bacsi{so:02d}@eclinic.local", "Demo@123")
        status, tra = goi("bac-si-tra-cuu", "GET", f"/api/booking/bac-si/toi/lich-hen/tra-cuu?ma={da_kham['maTraCuu']}",
                          token=token_bs)
        if status == 200:
            id_lich = tra["duLieu"]["id"]
            break
    assert id_lich, "Không tìm thấy tài khoản bác sĩ của ca khám"

    def id_cua(ma_tra_cuu):
        return goi("bac-si-tra-cuu", "GET", f"/api/booking/bac-si/toi/lich-hen/tra-cuu?ma={ma_tra_cuu}",
                   token=token_bs, mong=200)[1]["duLieu"]["id"]

    goi("bac-si-huy", "POST", f"/api/booking/bac-si/toi/lich-hen/{id_cua(bi_tu_choi['maTraCuu'])}/huy",
        {"lyDo": "Bác sĩ có lịch hội chẩn đột xuất, mời bạn đặt khung giờ khác"}, token=token_bs, mong=200)

    def id_thuoc(tu_khoa, ham_luong):
        """Dòng đơn thuốc chỉ nhận id thuốc trong danh mục: lấy id từ gợi ý thuốc của bác sĩ (thuốc mẫu)."""
        _, goi_y = goi("bac-si-goi-y-thuoc", "GET", f"/api/medical/bac-si/toi/thuoc?tuKhoa={tu_khoa}", token=token_bs,
                       mong=200)
        return next(t["id"] for t in goi_y["duLieu"] if t["tenThuoc"] == tu_khoa and t["hamLuong"] == ham_luong)

    benh_an = {"lyDoKham": "Ho, sốt nhẹ 2 ngày nay",
               "sinhHieu": {"mach": 88, "nhietDo": 37.8, "huyetApTamThu": 118, "huyetApTamTruong": 76,
                            "nhipTho": 18, "canNang": 54.5, "chieuCao": 158},
               "khamLamSang": "Họng đỏ, amidan không sưng, phổi trong.",
               "chanDoan": "Viêm họng cấp", "maIcdChinh": "J02.9", "chanDoanKemTheo": "Viêm mũi dị ứng",
               "ghiChu": "Uống nhiều nước ấm, nghỉ ngơi; tái khám nếu sốt trên 38,5 độ.",
               "ngayTaiKhamDeXuat": (date.today() + timedelta(days=7)).isoformat(),
               "donThuoc": [{"idThuoc": id_thuoc("Paracetamol", "500mg"), "lieuDung": "1 viên / lần",
                             "soLanMoiNgay": 3, "soNgayDung": 5, "soLuong": 15,
                             "ghiChuSuDung": "Uống sau ăn, khi sốt hoặc đau"},
                            {"idThuoc": id_thuoc("Oresol", "4,22g"), "lieuDung": "1 gói pha 200 ml nước",
                             "soLanMoiNgay": 3, "soNgayDung": 3, "soLuong": 9, "ghiChuSuDung": None}]}
    goi("loi-bac-si-kham-truoc-ngay", "POST", f"/api/medical/bac-si/toi/lich-hen/{id_lich}/benh-an", benh_an,
        token=token_bs, mong=409)
    ma_kham, ma_dk = da_kham["maPhieuKham"], dinh_ky["maPhieuKham"]
    danh_gia = {"soSao": 5, "nhanXet": "Bác sĩ tư vấn kỹ, dễ hiểu."}
    goi("loi-danh-gia-chua-kham", "POST", f"{lh}/{ma_kham}/danh-gia", danh_gia, token=token, mong=409)
    doi_ngay_luot_kham(da_kham["maTraCuu"], -so_ngay)
    goi("bac-si-ghi-ket-qua", "POST", f"/api/medical/bac-si/toi/lich-hen/{id_lich}/benh-an", benh_an,
        token=token_bs, mong=201)

    # --- 15.1 - 15.3 ---
    goi("lich-hen-cua-toi-sau", "GET", lh, token=token, mong=200)
    goi("lich-hen-sap-toi", "GET", f"{lh}?loc=SAP_TOI&kichThuoc=2", token=token, mong=200)
    goi("lich-hen-lich-su", "GET", f"{lh}?loc=LICH_SU", token=token, mong=200)
    goi("lich-hen-ban-than", "GET", f"{lh}?cuaAi=BAN_THAN", token=token, mong=200)
    goi("lich-hen-nguoi-khac", "GET", f"{lh}?cuaAi=NGUOI_KHAC", token=token, mong=200)
    goi("loi-lich-hen-loc-sai", "GET", f"{lh}?loc=ABC", token=token, mong=400)
    goi("loi-lich-hen-kich-thuoc", "GET", f"{lh}?kichThuoc=101", token=token, mong=400)
    goi("loi-lich-hen-khong-token", "GET", lh, mong=401)
    goi("loi-lich-hen-admin", "GET", lh, token=token_ad, mong=403)
    goi("loi-lich-hen-bac-si", "GET", lh, token=token_bs, mong=403)
    hom_nay = date.today()

    def lich(ten, tu, den, them="", mong=200):
        return goi(ten, "GET", f"{lh}/lich?tuNgay={tu}&denNgay={den}{them}", token=token, mong=mong)

    lich("lich-thang", hom_nay, hom_nay + timedelta(days=6))
    lich("lich-thang-nguoi-khac", hom_nay, hom_nay + timedelta(days=6), "&cuaAi=NGUOI_KHAC")
    lich("lich-42-ngay", hom_nay, hom_nay + timedelta(days=41))
    lich("loi-lich-43-ngay", hom_nay, hom_nay + timedelta(days=42), mong=400)
    lich("loi-lich-nguoc", hom_nay + timedelta(days=1), hom_nay, mong=400)
    goi("loi-lich-thieu-ngay", "GET", f"{lh}/lich?tuNgay={hom_nay}", token=token, mong=400)
    goi("chi-tiet-da-xac-nhan", "GET", f"{lh}/{ma_dk}", token=token, mong=200)
    goi("chi-tiet-da-kham", "GET", f"{lh}/{ma_kham}", token=token, mong=200)
    goi("chi-tiet-bi-tu-choi", "GET", f"{lh}/{bi_tu_choi['maPhieuKham']}", token=token, mong=200)
    goi("chi-tiet-cua-con", "GET", f"{lh}/{cua_con['maPhieuKham']}", token=token, mong=200)
    goi("loi-chi-tiet-khong-co", "GET", f"{lh}/{MA_GIA}", token=token, mong=404)
    goi("loi-chi-tiet-cua-khach", "GET", f"{lh}/{ma_khach}", token=token, mong=404)

    # --- 16. Lịch sử khám và đánh giá ---
    goi("lich-su-kham", "GET", ls, token=token, mong=200)
    goi("lich-su-kham-nguoi-khac", "GET", f"{ls}?cuaAi=NGUOI_KHAC", token=token, mong=200)
    goi("loi-lich-su-kham-admin", "GET", ls, token=token_ad, mong=403)
    goi("loi-sua-danh-gia-chua-co", "PUT", f"{lh}/{ma_kham}/danh-gia", danh_gia, token=token, mong=404)
    goi("loi-danh-gia-6-sao", "POST", f"{lh}/{ma_kham}/danh-gia", {"soSao": 6}, token=token, mong=400)
    goi("loi-danh-gia-lich-chua-kham", "POST", f"{lh}/{ma_dk}/danh-gia", danh_gia, token=token, mong=409)
    goi("loi-danh-gia-khong-co", "POST", f"{lh}/{MA_GIA}/danh-gia", danh_gia, token=token, mong=404)
    goi("danh-gia", "POST", f"{lh}/{ma_kham}/danh-gia", danh_gia, token=token, mong=201)
    goi("loi-danh-gia-lan-2", "POST", f"{lh}/{ma_kham}/danh-gia", danh_gia, token=token, mong=409)
    goi("chi-tiet-da-danh-gia", "GET", f"{lh}/{ma_kham}", token=token, mong=200)
    goi("sua-danh-gia", "PUT", f"{lh}/{ma_kham}/danh-gia", {"soSao": 4, "nhanXet": ""}, token=token, mong=200)
    goi("ho-so-tai-khoan-bac-si", "GET", "/api/users/me", token=token_bs, mong=200)

    # --- 15.4, 15.5 ---
    goi("loi-huy-da-kham", "POST", f"{lh}/{ma_kham}/huy", token=token, mong=409)
    goi("loi-huy-khong-co", "POST", f"{lh}/{MA_GIA}/huy", token=token, mong=404)
    goi("loi-huy-cua-khach", "POST", f"{lh}/{ma_khach}/huy", token=token, mong=404)
    goi("huy-co-ly-do", "POST", f"{lh}/{cua_me['maPhieuKham']}/huy", {"lyDo": "Mẹ tôi đã khám ở nơi khác"},
        token=token, mong=200)
    goi("loi-huy-lan-2", "POST", f"{lh}/{cua_me['maPhieuKham']}/huy", token=token, mong=409)
    doi_ngay_luot_kham(cua_con["maTraCuu"], -so_ngay)
    goi("loi-huy-qua-han", "POST", f"{lh}/{cua_con['maPhieuKham']}/huy", token=token, mong=409)
    goi("loi-doi-qua-han", "POST", f"{lh}/{cua_con['maPhieuKham']}/doi-lich",
        {"idLichLamViec": id_ca, "gioBatDauKhung": khung[3]}, token=token, mong=409)
    doi_ngay_luot_kham(cua_con["maTraCuu"], so_ngay)
    goi("huy-khong-body", "POST", f"{lh}/{cua_con['maPhieuKham']}/huy", token=token, mong=200)
    goi("loi-doi-cung-khung", "POST", f"{lh}/{ma_dk}/doi-lich", {"idLichLamViec": id_ca, "gioBatDauKhung": khung[0]},
        token=token, mong=400)
    goi("loi-doi-thieu-gio", "POST", f"{lh}/{ma_dk}/doi-lich", {"idLichLamViec": id_ca}, token=token, mong=400)
    _, moi = goi("doi-lich-cua-toi", "POST", f"{lh}/{ma_dk}/doi-lich",
                 {"idLichLamViec": id_ca, "gioBatDauKhung": khung[3]}, token=token, mong=201)
    goi("loi-doi-lich-cu", "POST", f"{lh}/{ma_dk}/doi-lich", {"idLichLamViec": id_ca, "gioBatDauKhung": khung[2]},
        token=token, mong=409)
    goi("chi-tiet-lich-cu-sau-doi", "GET", f"{lh}/{ma_dk}", token=token, mong=200)
    goi("chi-tiet-lich-moi", "GET", f"{lh}/{moi['duLieu']['maPhieuKham']}", token=token, mong=200)

    # --- 17. Hồ sơ bệnh nhân, trang cá nhân ---
    _, ho_so = goi("ho-so-benh-nhan", "GET", hs, token=token, mong=200)
    cu = ho_so["duLieu"]
    sua = {"hoTen": cu["hoTen"], "ngaySinh": cu["ngaySinh"], "gioiTinh": cu["gioiTinh"],
           "soDienThoai": cu["soDienThoai"], "diaChi": cu["diaChi"], "soBaoHiemYTe": "DN4797900000001",
           "tienSuBenhLy": "Dị ứng penicillin"}
    goi("sua-ho-so-benh-nhan", "PUT", hs, sua, token=token, mong=200)
    goi("loi-ho-so-doi-cccd", "PUT", hs, {**sua, "cccd": "079000000999"}, token=token, mong=400)
    goi("loi-ho-so-sai", "PUT", hs, {**sua, "soDienThoai": "123", "hoTen": ""}, token=token, mong=400)
    goi("loi-ho-so-admin", "GET", hs, token=token_ad, mong=403)
    goi("trang-ca-nhan", "GET", "/api/booking/trang-ca-nhan/cua-toi", token=token, mong=200)
    goi("ho-so-tai-khoan-sau", "GET", "/api/users/me", token=token, mong=200)

    # --- 18. Thông báo (job gửi chạy mỗi 5 giây) ---
    for _ in range(15):
        if goi("so-chua-doc", "GET", f"{tb}/so-chua-doc", token=token, mong=200)[1]["duLieu"]["soChuaDoc"] >= 2:
            break
        time.sleep(2)
    _, ds_tb = goi("thong-bao", "GET", tb, token=token, mong=200)
    goi("thong-bao-en", "GET", tb, token=token, mong=200, ngon_ngu="en")
    goi("thong-bao-chua-doc", "GET", f"{tb}?chuaDoc=true&kichThuoc=1", token=token, mong=200)
    id_tb = ds_tb["duLieu"]["noiDung"][0]["id"]
    goi("thong-bao-da-doc", "POST", f"{tb}/{id_tb}/da-doc", token=token, mong=200)
    goi("thong-bao-da-doc-lan-2", "POST", f"{tb}/{id_tb}/da-doc", token=token, mong=200)
    goi("loi-thong-bao-khong-co", "POST", f"{tb}/999999/da-doc", token=token, mong=404)
    _, cua_bs = goi("thong-bao-bac-si", "GET", f"{tb}?kichThuoc=3", token=token_bs, mong=200)
    goi("loi-thong-bao-cua-nguoi-khac", "POST", f"{tb}/{cua_bs['duLieu']['noiDung'][0]['id']}/da-doc", token=token,
        mong=404)
    goi("so-chua-doc-truoc", "GET", f"{tb}/so-chua-doc", token=token, mong=200)
    goi("da-doc-tat-ca", "POST", f"{tb}/da-doc-tat-ca", token=token, mong=200)
    goi("so-chua-doc-sau", "GET", f"{tb}/so-chua-doc", token=token, mong=200)
    goi("da-doc-tat-ca-lan-2", "POST", f"{tb}/da-doc-tat-ca", token=token, mong=200)
    goi("loi-thong-bao-kich-thuoc", "GET", f"{tb}?kichThuoc=0", token=token, mong=400)
    goi("loi-thong-bao-khong-token", "GET", tb, mong=401)
    goi("thong-bao-admin", "GET", tb, token=token_ad, mong=200)

    # --- Hết hạn sửa đánh giá: lùi ngày gửi đánh giá quá 7 ngày ---
    sql("UPDATE lich_hen SET danh_gia_ngay_tao = danh_gia_ngay_tao - INTERVAL 8 DAY "
        f"WHERE ma_tra_cuu = '{da_kham['maTraCuu']}'")
    goi("loi-sua-danh-gia-het-han", "PUT", f"{lh}/{ma_kham}/danh-gia", danh_gia, token=token, mong=409)


def tai_khoan_chua_co_ho_so(id_ca, khung):
    """Mục 17 với tài khoản mới: chưa có hồ sơ, tạo hồ sơ, liên kết hồ sơ đã có từ lần đặt lịch như khách."""
    hs, lh = "/api/booking/ho-so-benh-nhan/cua-toi", "/api/booking/lich-hen/cua-toi"
    xoa_gioi_han_ip()

    def khach_dat(ten, ho_ten, sdt, cccd, gio):
        goi(f"khach-dat-{ten}", "POST", "/api/booking/lich-hen",
            {"idLichLamViec": id_ca, "gioBatDauKhung": gio, "benhNhan": nguoi(ho_ten, "1988-11-02", sdt, cccd),
             "lyDoKham": "Khám tổng quát"}, mong=201)

    def ho_so(ho_ten, sdt, cccd=None, ngay_sinh="1988-11-02"):
        return {"hoTen": ho_ten, "ngaySinh": ngay_sinh, "gioiTinh": "NAM", "soDienThoai": sdt, "cccd": cccd,
                "diaChi": "25 Quang Trung, Gò Vấp, TP.HCM", "soBaoHiemYTe": None, "tienSuBenhLy": None}

    # X: chưa có hồ sơ -> tạo mới
    x = tai_khoan_moi("x", "Đỗ Văn Xuân", "0933000001")
    goi("loi-ho-so-chua-co", "GET", hs, token=x, mong=404)
    goi("trang-ca-nhan-chua-co-ho-so", "GET", "/api/booking/trang-ca-nhan/cua-toi", token=x, mong=200)
    goi("thong-tin-dat-lich-chua-co-ho-so", "GET", "/api/booking/thong-tin-dat-lich/cua-toi", token=x, mong=200)
    goi("lich-hen-rong", "GET", lh, token=x, mong=200)
    goi("lich-su-kham-rong", "GET", "/api/booking/lich-su-kham/cua-toi", token=x, mong=200)
    goi("thong-bao-rong", "GET", "/api/notification/thong-bao", token=x, mong=200)
    goi("loi-tao-ho-so-thieu-cccd", "PUT", hs, ho_so("Đỗ Văn Xuân", "0933000001"), token=x, mong=400)
    goi("tao-ho-so", "PUT", hs, ho_so("Đỗ Văn Xuân", "0933000001", "079188000001"), token=x, mong=200)
    goi("ho-so-vua-tao", "GET", hs, token=x, mong=200)

    # Y: nhập CCCD của hồ sơ khách đã đặt, khớp họ tên + ngày sinh -> liên kết ngay
    khach_dat("y", "Phạm Văn Long", "0933000002", "079188000002", khung[0])
    y = tai_khoan_moi("y", "Phạm Văn Long", "0933000092")
    goi("lien-ket-ho-so-khop", "PUT", hs, ho_so("Phạm Văn Long", "0933000092", "079188000002"), token=y, mong=200)
    goi("lich-hen-sau-lien-ket", "GET", lh, token=y, mong=200)

    # W: khai CCCD lúc đăng ký, trùng họ tên + SĐT của hồ sơ khách -> tự liên kết ở lần đọc đầu tiên
    khach_dat("w", "Võ Minh Quân", "0933000003", "079188000003", khung[1])
    w = tai_khoan_moi("w", "Võ Minh Quân", "0933000003", "079188000003")
    goi("lich-hen-tu-lien-ket", "GET", lh, token=w, mong=200)
    goi("ho-so-tu-lien-ket", "GET", hs, token=w, mong=200)

    # Z: nhập CCCD của hồ sơ khách nhưng họ tên khác -> chờ xác minh
    khach_dat("z", "Lý Thanh Tùng", "0933000004", "079188000004", khung[2])
    z = tai_khoan_moi("z", "Lý Hải Nam", "0933000094")
    goi("lien-ket-ho-so-cho-xac-minh", "PUT", hs, ho_so("Lý Hải Nam", "0933000094", "079188000004", "1990-01-01"),
        token=z, mong=200)
    goi("loi-ho-so-cho-xac-minh", "PUT", hs, ho_so("Lý Hải Nam", "0933000094", "079188000004", "1990-01-01"),
        token=z, mong=409)
    goi("ho-so-cho-xac-minh", "GET", hs, token=z, mong=200)
    goi("trang-ca-nhan-cho-xac-minh", "GET", "/api/booking/trang-ca-nhan/cua-toi", token=z, mong=200)
    goi("lich-hen-cho-xac-minh", "GET", lh, token=z, mong=200)
    goi("loi-dat-ban-than-cho-xac-minh", "POST", "/api/booking/lich-hen",
        {"idLichLamViec": id_ca, "gioBatDauKhung": khung[3], "lyDoKham": "Khám tổng quát", "datChoBanThan": True,
         "benhNhan": nguoi("Lý Hải Nam", "1990-01-01", "0933000094", "079188000004")}, token=z, mong=409)

    # V: nhập CCCD của hồ sơ đã thuộc tài khoản khác
    v = tai_khoan_moi("v", "Trịnh Văn Vũ", "0933000005")
    goi("loi-cccd-da-co-ho-so", "PUT", hs, ho_so("Trịnh Văn Vũ", "0933000005", "079000000001"), token=v, mong=409)


def tai_khoan(token_ad):
    """Mục 5-10: response thành công của các route tài khoản (để so cấu trúc với JSON mẫu trong tài liệu)."""
    me, doi_email = "/api/users/me", "/api/users/me/change-email"
    token = tai_khoan_moi("tk", "Nguyễn Văn An", "0933000010", "079188000010")
    email = email_cuoi
    token = goi("lam-moi-phien", "POST", "/api/auth/refresh-token", cookie=cookie_cuoi, mong=200)[1]["duLieu"][
        "accessToken"]
    _, toi = goi("ho-so-tai-khoan-moi", "GET", me, token=token, mong=200)
    goi("sua-ho-so-tai-khoan", "PUT", me, {"hoTen": "Nguyễn Văn An", "soDienThoai": "0933000012"}, token=token,
        mong=200)
    # 6.4 và quy ước ngôn ngữ (3.12): ngôn ngữ của tài khoản, thông điệp và nội dung danh mục theo Accept-Language
    goi("doi-ngon-ngu", "PUT", f"{me}/language", {"ngonNgu": "EN"}, token=token, mong=200)
    goi("doi-ngon-ngu-en", "PUT", f"{me}/language", {"ngonNgu": "EN"}, token=token, mong=200, ngon_ngu="en")
    goi("ho-so-sau-doi-ngon-ngu", "GET", me, token=token, mong=200)
    goi("loi-ngon-ngu-sai", "PUT", f"{me}/language", {"ngonNgu": "FR"}, token=token, mong=400)
    goi("loi-ngon-ngu-thieu", "PUT", f"{me}/language", {}, token=token, mong=400)
    goi("loi-ngon-ngu-thieu-en", "PUT", f"{me}/language", {}, token=token, mong=400, ngon_ngu="en-US")
    goi("loi-ngon-ngu-khong-token", "PUT", f"{me}/language", {"ngonNgu": "EN"}, mong=401)
    goi("loi-ngon-ngu-khong-token-en", "PUT", f"{me}/language", {"ngonNgu": "EN"}, mong=401, ngon_ngu="en")
    goi("doi-ngon-ngu-vi", "PUT", f"{me}/language", {"ngonNgu": "VI"}, token=token, mong=200)
    goi("chuyen-khoa-ds-en", "GET", "/api/catalog/chuyen-khoa?trang=0&kichThuoc=3", mong=200, ngon_ngu="en")
    goi("chuyen-khoa-tim-en", "GET", "/api/catalog/chuyen-khoa?tuKhoa=derma", mong=200, ngon_ngu="en")
    goi("chuyen-khoa-tim-en-doc-vi", "GET", "/api/catalog/chuyen-khoa?tuKhoa=derma", mong=200)
    goi("bac-si-ds-en", "GET", "/api/catalog/bac-si?kichThuoc=1", mong=200, ngon_ngu="en")
    goi("loi-bac-si-khong-co-en", "GET", "/api/catalog/bac-si/999999", mong=404, ngon_ngu="en")
    goi("loi-duong-dan-gateway-en", "GET", "/khong-co-duong-dan-nay", mong=404, ngon_ngu="en")
    goi("thiet-bi-dang-nhap", "GET", f"{me}/sessions", token=token, mong=200)
    goi("doi-email-chua-co", "GET", doi_email, token=token, mong=200)
    goi("doi-email", "POST", doi_email, {"emailMoi": "moi." + email, "matKhauHienTai": MAT_KHAU}, token=token,
        mong=200)
    goi("doi-email-dang-cho", "GET", doi_email, token=token, mong=200)
    goi("huy-doi-email", "DELETE", doi_email, token=token, mong=200)
    id_tk = toi["duLieu"]["id"]
    goi("quan-tri-ds-tai-khoan", "GET", "/api/users?vaiTro=BENH_NHAN&kichThuoc=2", token=token_ad, mong=200)
    goi("quan-tri-ds-bac-si", "GET", "/api/users?vaiTro=BAC_SI&kichThuoc=1", token=token_ad, mong=200)
    goi("quan-tri-chi-tiet", "GET", f"/api/users/{id_tk}", token=token_ad, mong=200)
    goi("quan-tri-vo-hieu-hoa", "PUT", f"/api/users/{id_tk}/status",
        {"trangThai": "VO_HIEU_HOA", "lyDo": "Vi phạm quy định sử dụng"}, token=token_ad, mong=200)
    goi("quan-tri-chi-tiet-vo-hieu-hoa", "GET", f"/api/users/{id_tk}", token=token_ad, mong=200)
    goi("quan-tri-kich-hoat-lai", "PUT", f"/api/users/{id_tk}/status", {"trangThai": "DA_KICH_HOAT"}, token=token_ad,
        mong=200)
    goi("dang-xuat", "POST", "/api/auth/logout", cookie=cookie_cuoi, mong=200)


def chatbot(token, token_ad, id_ck, id_bs):
    """Mục 19: chatbot. Cần chatbot-service chạy với mô hình giả temp/d9-p9-mo-hinh-gia.js (các câu #bacsi, #lich,
    #lichtoi, #lankham, #ngoai, #500 là luật của mô hình giả); không có chatbot-service thì bỏ qua cả phần này."""
    p = "/api/chatbot/phien"
    status, mo = goi("chat-mo-phien-khach", "POST", p)
    if status != 201:
        print("     bỏ qua phần chatbot: không mở được phiên")
        return
    id_k, ma_k = mo["duLieu"]["idPhien"], mo["duLieu"]["maKhach"]
    tn = f"{p}/{id_k}/tin-nhan"
    goi("chat-mo-phien-khach-en", "POST", p, mong=201, ngon_ngu="en")
    goi("chat-gui-khach", "POST", tn, {"noiDung": "Tôi bị nổi mẩn đỏ ở tay, nên khám khoa nào?"}, khach=ma_k, mong=200)
    goi("chat-gui-bac-si", "POST", tn, {"noiDung": f"#bacsi {id_ck}"}, khach=ma_k, mong=200)
    goi("chat-gui-lich", "POST", tn, {"noiDung": f"#lich {id_bs}"}, khach=ma_k, mong=200)
    goi("chat-khan-cap", "POST", tn, {"noiDung": "Tôi đang khó thở, đau ngực dữ dội"}, khach=ma_k, mong=200)
    goi("chat-khan-cap-en", "POST", tn, {"noiDung": "Tôi đang khó thở, đau ngực dữ dội"}, khach=ma_k, mong=200,
        ngon_ngu="en")
    goi("chat-ngoai-pham-vi", "POST", tn, {"noiDung": "#ngoai"}, khach=ma_k, mong=200)
    goi("chat-lich-su", "GET", tn, khach=ma_k, mong=200)
    goi("loi-chat-thieu-ma-khach", "GET", tn, mong=404)
    goi("loi-chat-sai-ma-khach", "POST", tn, {"noiDung": "xin chào"}, khach=MA_GIA, mong=404)
    goi("loi-chat-tin-rong", "POST", tn, {"noiDung": ""}, khach=ma_k, mong=400)
    goi("loi-chat-tin-dai", "POST", tn, {"noiDung": "a" * 1001}, khach=ma_k, mong=400)
    goi("loi-chat-phien-khong-co", "POST", f"{p}/999999/tin-nhan", {"noiDung": "xin chào"}, khach=ma_k, mong=404)
    goi("loi-chat-ds-khong-token", "GET", p, mong=401)
    goi("loi-chat-dong-y-khong-token", "POST", f"{p}/{id_k}/dong-y", khach=ma_k, mong=401)
    goi("loi-chat-mo-hinh-loi", "POST", tn, {"noiDung": "#500"}, khach=ma_k)

    _, mo_bn = goi("chat-mo-phien-benh-nhan", "POST", p, token=token, mong=201)
    id_bn = mo_bn["duLieu"]["idPhien"]
    tn_bn = f"{p}/{id_bn}/tin-nhan"
    goi("chat-can-dong-y", "POST", tn_bn, {"noiDung": "#lichtoi"}, token=token, mong=200)
    goi("chat-dong-y", "POST", f"{p}/{id_bn}/dong-y", token=token, mong=200)
    goi("chat-lich-hen-cua-toi", "POST", tn_bn, {"noiDung": "#lichtoi"}, token=token, mong=200)
    goi("chat-lan-kham", "POST", tn_bn, {"noiDung": "#lankham"}, token=token, mong=200)
    goi("chat-phien-cua-toi", "GET", p, token=token, mong=200)
    goi("chat-rut-dong-y", "DELETE", f"{p}/{id_bn}/dong-y", token=token, mong=200)
    goi("chat-can-dong-y-lai", "POST", tn_bn, {"noiDung": "#lichtoi"}, token=token, mong=200)
    goi("loi-chat-dong-y-phien-khach", "POST", f"{p}/{id_k}/dong-y", token=token, mong=404)
    _, mo_ad = goi("chat-mo-phien-admin", "POST", p, token=token_ad, mong=201)
    id_ad = mo_ad["duLieu"]["idPhien"]
    goi("loi-chat-dong-y-admin", "POST", f"{p}/{id_ad}/dong-y", token=token_ad, mong=403)
    goi("loi-chat-phien-nguoi-khac", "GET", f"{p}/{id_ad}/tin-nhan", token=token, mong=404)
    goi("chat-admin-hoi-lich", "POST", f"{p}/{id_ad}/tin-nhan", {"noiDung": "#lichtoi"}, token=token_ad, mong=200)


def dat_lich_toan_vien(token, token_ad, toi_bn, id_ck, ngay, ma_cu, ma_moi, ma_tra_cuu_khach):
    """Mục 20 và 14.5 (DOANTOTNGH-14): quy tắc đặt lịch, ngày nghỉ, sổ khám bệnh, khách xin cấp lại link, tệp lịch .ics,
    lọc bác sĩ theo ngày / buổi. ma_cu: phiếu đã đổi đi (tệp lịch "đã hủy"); ma_moi: 1 phiếu đang hiệu lực;
    ma_tra_cuu_khach: mã ECL của lần khách đặt đầu (chuỗi đã đổi rồi hủy: phản hồi vẫn như mọi trường hợp); sổ khám lấy
    từ lượt khám bác sĩ đã ghi ở cua_toi (SĐT liên hệ = SĐT của toi_bn)."""
    xoa_gioi_han_ip("cap-lai:*")
    xoa_gioi_han_ip("so-kham:sai:*")

    # --- 20.1 Quy tắc đặt lịch ---
    goi("quy-tac-dat-lich", "GET", "/api/booking/quy-tac-dat-lich", mong=200)

    # --- 20.2 Ngày nghỉ: khai báo 1 ngày ngoài 14 ngày ca mẫu (không ca nào bị hủy) bằng route của quản trị viên ---
    nghi = (date.today() + timedelta(days=30)).isoformat()
    goi("qt-khai-bao-ngay-nghi", "POST", "/api/booking/quan-tri/ngay-nghi",
        {"tuNgay": nghi, "denNgay": nghi, "lyDo": "Nghỉ lễ Quốc khánh bù", "lyDoEn": "National Day holiday (in lieu)"},
        token=token_ad)
    goi("ngay-nghi", "GET", "/api/booking/ngay-nghi", mong=200)
    goi("ngay-nghi-en", "GET", f"/api/booking/ngay-nghi?tuNgay={nghi}&denNgay={nghi}", mong=200, ngon_ngu="en")
    goi("ngay-nghi-rong", "GET", "/api/booking/ngay-nghi?tuNgay=2020-01-01&denNgay=2020-01-31", mong=200)
    goi("loi-ngay-nghi-nguoc", "GET", "/api/booking/ngay-nghi?tuNgay=2026-12-31&denNgay=2026-12-01", mong=400)
    goi("loi-ngay-nghi-qua-366", "GET", "/api/booking/ngay-nghi?tuNgay=2026-01-01&denNgay=2027-06-01", mong=400)

    # --- 11.3 Lọc bác sĩ theo ngày / buổi còn chỗ ---
    goi("bac-si-theo-ngay", "GET", f"/api/catalog/bac-si?idChuyenKhoa={id_ck}&ngay={ngay}", mong=200)
    goi("bac-si-theo-ngay-buoi", "GET", f"/api/catalog/bac-si?idChuyenKhoa={id_ck}&ngay={ngay}&buoi=SANG", mong=200)
    goi("bac-si-ngay-ngoai-khoang", "GET", f"/api/catalog/bac-si?ngay={(date.today() + timedelta(days=400)).isoformat()}",
        mong=200)
    goi("loi-bac-si-buoi-sai", "GET", f"/api/catalog/bac-si?buoi=DEM", mong=400)

    # --- 14.5 Tệp lịch .ics ---
    goi("lich-ics", "GET", f"/api/booking/phieu-kham/{ma_moi}/lich.ics", mong=200)
    goi("lich-ics-phieu-cu", "GET", f"/api/booking/phieu-kham/{ma_cu}/lich.ics", mong=200)
    goi("loi-lich-ics-khong-co", "GET", f"/api/booking/phieu-kham/{MA_GIA}/lich.ics", mong=404)

    # --- 20.3 - 20.5 Sổ khám bệnh ---
    _, ls = goi("lich-su-kham-so-kham", "GET", "/api/booking/lich-su-kham/cua-toi", token=token, mong=200)
    ma_sk = next(d["ketQua"]["maSoKham"] for d in ls["duLieu"]["noiDung"] if d["ketQua"] and d["ketQua"].get("maSoKham"))
    sk = f"/api/booking/so-kham/{ma_sk}"
    bon_so = toi_bn["soDienThoai"][-4:]
    sai = "0000" if bon_so != "0000" else "1111"
    goi("so-kham-tom-tat", "GET", sk, mong=200)
    goi("so-kham-tom-tat-en", "GET", sk, mong=200, ngon_ngu="en")
    goi("loi-so-kham-khong-co", "GET", f"/api/booking/so-kham/{MA_GIA}", mong=404)
    goi("loi-so-kham-thieu-so", "POST", f"{sk}/xem", {"bonSoCuoi": "12a"}, mong=400)
    goi("so-kham-xem", "POST", f"{sk}/xem", {"bonSoCuoi": bon_so}, mong=200)
    goi("so-kham-xem-en", "POST", f"{sk}/xem", {"bonSoCuoi": bon_so}, mong=200, ngon_ngu="en")
    goi("so-kham-qr", "GET", f"{sk}/qr", mong=200)
    goi("loi-so-kham-qr-kich-thuoc", "GET", f"{sk}/qr?kichThuoc=50", mong=400)
    for lan in range(1, 7):
        status, _ = goi(f"loi-so-kham-sai-lan-{lan}", "POST", f"{sk}/xem", {"bonSoCuoi": sai})
        if status == 429:
            break
    goi("loi-so-kham-dang-khoa", "POST", f"{sk}/xem", {"bonSoCuoi": bon_so}, mong=429)
    xoa_gioi_han_ip("so-kham:sai:*")

    # --- 20.6 Khách xin cấp lại link: mã tra cứu của lần đặt đầu (đã đổi), gửi cho lịch hiện hành của chuỗi ---
    cl = "/api/booking/cap-lai-lien-ket"
    goi("cap-lai-lien-ket", "POST", cl, {"maTraCuu": ma_tra_cuu_khach, "soDienThoai": "0912345601"}, mong=200)
    goi("cap-lai-lien-ket-sai-sdt", "POST", cl, {"maTraCuu": ma_tra_cuu_khach, "soDienThoai": "0999999999"}, mong=200)
    goi("cap-lai-lien-ket-ma-khong-co", "POST", cl, {"maTraCuu": "ECL-20200101-0000", "soDienThoai": "0912345601"},
        mong=200)
    goi("loi-cap-lai-sai-dang", "POST", cl, {"maTraCuu": "ABC", "soDienThoai": "123"}, mong=400)
    goi("cap-lai-lien-ket-lan-3", "POST", cl, {"maTraCuu": ma_tra_cuu_khach, "soDienThoai": "0912345601"}, mong=200)
    goi("loi-cap-lai-qua-so-lan", "POST", cl, {"maTraCuu": ma_tra_cuu_khach, "soDienThoai": "0912345601"}, mong=429)
    xoa_gioi_han_ip("cap-lai:*")


def la_nguoi_giam_ho(token, cccd_toi, id_ca, gio):
    """Tài khoản chỉ là người giám hộ (theo CCCD) của lịch khách đặt: thấy lịch, không hủy / đổi / đánh giá được."""
    lh = "/api/booking/lich-hen/cua-toi"
    xoa_gioi_han_ip()
    _, dat = goi("khach-dat-cho-chau", "POST", "/api/booking/lich-hen",
                 {"idLichLamViec": id_ca, "gioBatDauKhung": gio, "lyDoKham": "Nổi mẩn đỏ ở tay",
                  "benhNhan": nguoi("Nguyễn Minh Khôi", "2016-04-20", "0933000006"),
                  "nguoiGiamHo": {"hoTen": "Nguyễn Thị Mai", "quanHe": "KHAC", "soDienThoai": "0933000006",
                                  "cccd": cccd_toi}}, mong=201)
    ma = dat["duLieu"]["maPhieuKham"]
    goi("lich-hen-la-nguoi-giam-ho", "GET", f"{lh}?cuaAi=NGUOI_KHAC", token=token, mong=200)
    goi("chi-tiet-la-nguoi-giam-ho", "GET", f"{lh}/{ma}", token=token, mong=200)
    goi("loi-huy-la-nguoi-giam-ho", "POST", f"{lh}/{ma}/huy", token=token, mong=403)
    goi("loi-doi-la-nguoi-giam-ho", "POST", f"{lh}/{ma}/doi-lich", {"idLichLamViec": id_ca, "gioBatDauKhung": gio},
        token=token, mong=403)
    goi("loi-danh-gia-la-nguoi-giam-ho", "POST", f"{lh}/{ma}/danh-gia", {"soSao": 5}, token=token, mong=403)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
