"""Gọi các route của tài liệu API thứ 2 (lịch hẹn và khám bệnh phía bác sĩ và quản trị viên) trên 1 stack đang chạy (có
dữ liệu mẫu) và lưu request + response thật vào temp/api-doc/mau-bsqt/NN-ten.json. JSON mẫu trong tài liệu lấy từ các
tệp này; bsqt_check.py (kiểm tra 8) so mẫu với chúng.

Chỉ chạy với stack thử (docker compose -p eclinic-smoke, SEED_DATA=true): script đặt, hủy lịch hẹn, ghi hồ sơ bệnh án và
sửa dữ liệu bằng SQL (dời ca làm việc). Dùng lại hàm gọi API và SQL của lay_mau.py (tài liệu thứ nhất); kết quả ghi vào
thư mục riêng nên mẫu của tài liệu thứ nhất không bị đụng tới.

    python scripts/api-doc/bsqt_lay_mau.py [http://127.0.0.1:8080]
"""

import sys
from datetime import date, timedelta
from urllib.parse import quote

import lay_mau
from lay_mau import dang_nhap, goi, sql, xoa_gioi_han_ip

lay_mau.THU_MUC = lay_mau.GOC / "temp/api-doc/mau-bsqt"
MAT_KHAU_MAU = "Demo@123"
BS = "/api/booking/bac-si/toi"
SO_NGAY_DOI_CA_CU = 200  # ca hôm nay của bác sĩ được dời đi xa để ca mới dời về không trùng giờ


def bac_si(so):
    return dang_nhap(f"dang-nhap-bac-si-{so:02d}", f"bacsi{so:02d}@eclinic.local", MAT_KHAU_MAU)


def tu_dat_lich(so, id_ca, gio, ly_do):
    """Bệnh nhân mẫu benhnhanNN tự đặt lịch cho bản thân (TK 13.1); trả lịch hẹn vừa đặt."""
    token = dang_nhap(f"dang-nhap-benh-nhan-{so:02d}", f"benhnhan{so:02d}@eclinic.local", MAT_KHAU_MAU)
    _, toi = goi(f"tai-khoan-{so:02d}", "GET", "/api/users/me", token=token, mong=200)
    _, tt = goi(f"thong-tin-dat-lich-{so:02d}", "GET", "/api/booking/thong-tin-dat-lich/cua-toi", token=token, mong=200)
    ban_than = tt["duLieu"]["banThan"]
    benh_nhan = {"hoTen": toi["duLieu"]["hoTen"], "ngaySinh": ban_than["ngaySinh"], "gioiTinh": ban_than["gioiTinh"],
                 "soDienThoai": toi["duLieu"]["soDienThoai"], "cccd": ban_than.get("cccd")}
    _, da_dat = goi(f"dat-lich-{so:02d}", "POST", "/api/booking/lich-hen",
                    {"idLichLamViec": id_ca, "gioBatDauKhung": gio, "benhNhan": benh_nhan, "lyDoKham": ly_do,
                     "datChoBanThan": True}, token=token, mong=201)
    return token, da_dat["duLieu"]


def khung_con_cho(id_bac_si, ca):
    """Giờ bắt đầu các khung còn chỗ của 1 ca, theo route công khai TK 12.1."""
    _, kq = goi("khung-gio", "GET", f"/api/booking/khung-gio?ngay={ca['ngay']}&idBacSi={id_bac_si}", mong=200)
    for ca_kham in kq["duLieu"]:
        if ca_kham["idLichLamViec"] == ca["idLichLamViec"]:
            return [k["gioBatDau"] for k in ca_kham["khungGio"] if k["soChoConLai"] > 0]
    return []


def khach_dat_lich(ten, id_ca, gio, benh_nhan, ly_do, nguoi_giam_ho=None):
    """Khách (không token) đặt lịch theo TK 13.1; trả lịch hẹn vừa đặt."""
    body = {"idLichLamViec": id_ca, "gioBatDauKhung": gio, "benhNhan": benh_nhan, "lyDoKham": ly_do}
    if nguoi_giam_ho:
        body["nguoiGiamHo"] = nguoi_giam_ho
    return goi(f"khach-dat-{ten}", "POST", "/api/booking/lich-hen", body, mong=201)[1]["duLieu"]


def doi_ca_ve_hom_nay(id_bac_si, ca, don_ca_hom_nay=True):
    """Dời ca `ca` (và khung giờ của nó) về hôm nay; ca hôm nay sẵn có của bác sĩ dời đi SO_NGAY_DOI_CA_CU ngày (trừ khi
    don_ca_hom_nay = False: ca dời về không trùng giờ ca hôm nay). Hàng đợi nhóm lịch hẹn theo ca và chỉ lấy ca có ngày
    làm việc là hôm nay, nên phải dời cả ca chứ không chỉ khung giờ. Mã tra cứu (ECL-<ngày khám>-...) được viết lại theo
    ngày mới."""
    hom_nay = date.today()
    lui = (date.fromisoformat(ca["ngay"]) - hom_nay).days
    if don_ca_hom_nay:
        sql("UPDATE khung_gio_kham k JOIN lich_lam_viec c ON c.id_lich_lam_viec = k.id_lich_lam_viec "
            f"SET k.gio_bat_dau = k.gio_bat_dau + INTERVAL {SO_NGAY_DOI_CA_CU} DAY, "
            f"k.gio_ket_thuc = k.gio_ket_thuc + INTERVAL {SO_NGAY_DOI_CA_CU} DAY "
            f"WHERE c.id_bac_si = {id_bac_si} AND c.ngay_lam_viec = '{hom_nay}'")
        sql(f"UPDATE lich_lam_viec SET ngay_lam_viec = ngay_lam_viec + INTERVAL {SO_NGAY_DOI_CA_CU} DAY "
            f"WHERE id_bac_si = {id_bac_si} AND ngay_lam_viec = '{hom_nay}'")
    sql("UPDATE khung_gio_kham SET "
        f"gio_bat_dau = gio_bat_dau - INTERVAL {lui} DAY, gio_ket_thuc = gio_ket_thuc - INTERVAL {lui} DAY "
        f"WHERE id_lich_lam_viec = {ca['idLichLamViec']}")
    sql(f"UPDATE lich_lam_viec SET ngay_lam_viec = '{hom_nay}' WHERE id_lich_lam_viec = {ca['idLichLamViec']}")
    sql("UPDATE lich_hen l JOIN khung_gio_kham k ON k.id_khung_gio = l.id_khung_gio "
        "SET l.ma_tra_cuu = CONCAT('ECL-', DATE_FORMAT(k.gio_bat_dau, '%Y%m%d'), SUBSTRING(l.ma_tra_cuu, 13)) "
        f"WHERE k.id_lich_lam_viec = {ca['idLichLamViec']}")


def p3_lich_va_hang_doi():
    """Mục 5 (lịch làm việc, lịch hẹn, bác sĩ hủy lịch) và mục 6 (hàng đợi khám)."""
    xoa_gioi_han_ip()
    hom_nay = date.today()
    tu, den = hom_nay.isoformat(), (hom_nay + timedelta(days=13)).isoformat()
    token_bs = bac_si(1)

    # --- Dựng: 4 lịch trong ca F (sẽ dời về hôm nay), 1 lịch trong ca G (để hủy) ---
    _, ca_ds = goi("bs-lich-lam-viec-dung", "GET", f"{BS}/lich-lam-viec?tuNgay={tu}&denNgay={den}", token=token_bs,
                   mong=200)
    id_bac_si = ca_ds["duLieu"][0]["bacSi"]["id"]
    ca_sau = [c for c in ca_ds["duLieu"] if c["ngay"] > tu and c["trangThai"] == "HOAT_DONG"]
    co_cho = [(c, khung_con_cho(id_bac_si, c)) for c in ca_sau]
    co_cho = [(c, k) for c, k in co_cho if len(k) >= 2]
    assert len(co_cho) >= 2, "Bác sĩ 01 cần ít nhất 2 ca sau hôm nay còn chỗ"
    (ca_f, khung_f), (ca_g, khung_g) = co_cho[0], co_cho[1]
    ly_do = ("Ho, sốt nhẹ 2 ngày nay", "Đau đầu, chóng mặt", "Tái khám huyết áp", "Đau dạ dày")
    lich_f = [tu_dat_lich(so, ca_f["idLichLamViec"], khung_f[0 if so <= 2 else 1], ly_do[so - 1])[1]
              for so in range(1, 5)]
    token_bn1, lich_g = tu_dat_lich(1, ca_g["idLichLamViec"], khung_g[0], "Khám định kỳ")
    doi_ca_ve_hom_nay(id_bac_si, ca_f)
    xoa_gioi_han_ip()

    def id_lich(lich):
        return goi("bs-tra-cuu", "GET", f"{BS}/lich-hen/tra-cuu?ma={lich['maPhieuKham']}", token=token_bs,
                   mong=200)[1]["duLieu"]["id"]

    so_1, so_2, so_3, so_4 = sorted((id_lich(l) for l in lich_f))
    id_g = id_lich(lich_g)
    ten_khong_dau = "mai"  # benhnhan01 Nguyễn Thị Mai

    # --- 5.1 ---
    goi("bs-lich-lam-viec", "GET", f"{BS}/lich-lam-viec?tuNgay={tu}&denNgay={den}", token=token_bs, mong=200)
    goi("loi-bs-lich-lam-viec-nguoc", "GET", f"{BS}/lich-lam-viec?tuNgay={den}&denNgay={tu}", token=token_bs,
        mong=400)
    goi("loi-bs-lich-lam-viec-43-ngay", "GET",
        f"{BS}/lich-lam-viec?tuNgay={tu}&denNgay={hom_nay + timedelta(days=42)}", token=token_bs, mong=400)
    goi("loi-bs-lich-lam-viec-thieu", "GET", f"{BS}/lich-lam-viec?tuNgay={tu}", token=token_bs, mong=400)

    # --- 5.2 ---
    goi("bs-lich-hen-hom-nay", "GET", f"{BS}/lich-hen", token=token_bs, mong=200)
    goi("bs-lich-hen-theo-ca", "GET", f"{BS}/lich-hen?idLichLamViec={ca_f['idLichLamViec']}", token=token_bs, mong=200)
    goi("bs-lich-hen-so-thu-tu", "GET", f"{BS}/lich-hen?soThuTu=2", token=token_bs, mong=200)
    goi("bs-lich-hen-tu-khoa", "GET", f"{BS}/lich-hen?tuKhoa={ten_khong_dau}", token=token_bs, mong=200)
    goi("bs-lich-hen-ngay-g", "GET", f"{BS}/lich-hen?ngay={ca_g['ngay']}", token=token_bs, mong=200)
    goi("loi-bs-lich-hen-ngay-sai", "GET", f"{BS}/lich-hen?ngay=16-10-2026", token=token_bs, mong=400)
    goi("loi-benh-nhan-goi-route-bac-si", "GET", f"{BS}/lich-hen", token=token_bn1, mong=403)

    # --- 5.3 ---
    goi("bs-lich-hen-khoang", "GET", f"{BS}/lich-hen/lich?tuNgay={tu}&denNgay={den}", token=token_bs, mong=200)
    goi("loi-bs-lich-hen-khoang-43-ngay", "GET",
        f"{BS}/lich-hen/lich?tuNgay={tu}&denNgay={hom_nay + timedelta(days=42)}", token=token_bs, mong=400)

    # --- 6.1 - 6.4 ---
    hd, ca_hom_nay = f"{BS}/hang-doi", ca_f["idLichLamViec"]
    token_bs2 = bac_si(2)
    _, ca_bs2 = goi("bs2-lich-lam-viec", "GET", f"{BS}/lich-lam-viec?tuNgay={tu}&denNgay={den}", token=token_bs2,
                    mong=200)
    goi("bs-hang-doi-hom-nay", "GET", hd, token=token_bs, mong=200)
    goi("bs-hang-doi-ngay-mai", "GET", f"{hd}?ngay={hom_nay + timedelta(days=1)}", token=token_bs, mong=200)
    goi("bs-goi-tiep", "POST", f"{hd}/goi-tiep?idLichLamViec={ca_hom_nay}", token=token_bs, mong=200)
    goi("loi-bs-goi-tiep-dang-kham", "POST", f"{hd}/goi-tiep?idLichLamViec={ca_hom_nay}", token=token_bs, mong=409)
    goi("bs-goi-kham-lai-nguoi-dang-kham", "POST", f"{BS}/lich-hen/{so_1}/goi-kham", token=token_bs, mong=200)
    goi("loi-bs-goi-kham-dang-kham-khac", "POST", f"{BS}/lich-hen/{so_2}/goi-kham", token=token_bs, mong=409)
    goi("loi-bs-goi-tiep-khong-phai-hom-nay", "POST", f"{hd}/goi-tiep?idLichLamViec={ca_g['idLichLamViec']}",
        token=token_bs, mong=409)
    goi("loi-bs-goi-tiep-ca-bac-si-khac", "POST", f"{hd}/goi-tiep?idLichLamViec={ca_bs2['duLieu'][0]['idLichLamViec']}",
        token=token_bs, mong=404)
    goi("loi-bs-goi-tiep-thieu-ca", "POST", f"{hd}/goi-tiep", token=token_bs, mong=400)
    goi("loi-bs-goi-kham-khong-phai-hom-nay", "POST", f"{BS}/lich-hen/{id_g}/goi-kham", token=token_bs, mong=409)
    goi("loi-bs-vang-mat-khong-phai-hom-nay", "POST", f"{BS}/lich-hen/{id_g}/vang-mat", token=token_bs, mong=409)
    goi("loi-bs-goi-kham-khong-co", "POST", f"{BS}/lich-hen/999999/goi-kham", token=token_bs, mong=404)
    goi("loi-bs-vang-mat-khong-co", "POST", f"{BS}/lich-hen/999999/vang-mat", token=token_bs, mong=404)
    goi("bs-vang-mat", "POST", f"{BS}/lich-hen/{so_1}/vang-mat", token=token_bs, mong=200)
    goi("bs-vang-mat-lan-2", "POST", f"{BS}/lich-hen/{so_1}/vang-mat", token=token_bs, mong=200)
    goi("bs-goi-kham", "POST", f"{BS}/lich-hen/{so_2}/goi-kham", token=token_bs, mong=200)
    # Dựng: ghi hồ sơ bệnh án cho lượt đang khám (route của mục 8, ở đây chỉ để lượt thành DA_KHAM)
    goi("bs-ghi-benh-an-dung", "POST", f"/api/medical/bac-si/toi/lich-hen/{so_2}/benh-an",
        {"lyDoKham": "Đau đầu, chóng mặt", "chanDoan": "Đau đầu do căng thẳng", "maIcdChinh": "G44.2"},
        token=token_bs, mong=201)
    goi("loi-bs-goi-kham-da-kham", "POST", f"{BS}/lich-hen/{so_2}/goi-kham", token=token_bs, mong=409)
    goi("loi-bs-vang-mat-da-kham", "POST", f"{BS}/lich-hen/{so_2}/vang-mat", token=token_bs, mong=409)
    goi("bs-goi-kham-lai-nguoi-vang-mat", "POST", f"{BS}/lich-hen/{so_1}/goi-kham", token=token_bs, mong=200)
    goi("bs-vang-mat-lai", "POST", f"{BS}/lich-hen/{so_1}/vang-mat", token=token_bs, mong=200)
    goi("bs-goi-tiep-2", "POST", f"{hd}/goi-tiep?idLichLamViec={ca_hom_nay}", token=token_bs, mong=200)
    goi("bs-vang-mat-3", "POST", f"{BS}/lich-hen/{so_3}/vang-mat", token=token_bs, mong=200)
    goi("bs-goi-tiep-3", "POST", f"{hd}/goi-tiep?idLichLamViec={ca_hom_nay}", token=token_bs, mong=200)
    goi("loi-bs-goi-tiep-het-hang", "POST", f"{hd}/goi-tiep?idLichLamViec={ca_hom_nay}", token=token_bs, mong=409)
    goi("bs-hang-doi-cuoi", "GET", hd, token=token_bs, mong=200)
    assert so_4  # lượt cuối đang DANG_KHAM trong hàng đợi cuối

    # --- 5.4 ---
    huy = f"{BS}/lich-hen/{id_g}/huy"
    goi("loi-bs-huy-thieu-ly-do", "POST", huy, {"lyDo": "  "}, token=token_bs, mong=400)
    goi("loi-bs-huy-ly-do-dai", "POST", huy, {"lyDo": "x" * 501}, token=token_bs, mong=400)
    goi("loi-bs-huy-khong-co", "POST", f"{BS}/lich-hen/999999/huy", {"lyDo": "Bận"}, token=token_bs, mong=404)
    goi("bs-huy", "POST", huy, {"lyDo": "Bác sĩ có lịch hội chẩn đột xuất, mời bạn đặt khung giờ khác"},
        token=token_bs, mong=200)
    goi("loi-bs-huy-lan-2", "POST", huy, {"lyDo": "Bận"}, token=token_bs, mong=409)
    goi("loi-bs-huy-da-kham", "POST", f"{BS}/lich-hen/{so_2}/huy", {"lyDo": "Bận"}, token=token_bs, mong=409)
    goi("bs-lich-hen-sau-huy", "GET", f"{BS}/lich-hen?ngay={ca_g['ngay']}", token=token_bs, mong=200)
    return {"token_bs": token_bs, "token_bs2": token_bs2, "id_bac_si": id_bac_si, "id_f2": so_2}


def p4_kham_benh(p3):
    """Mục 7 (tra cứu lịch hẹn, hồ sơ khám, sửa thông tin bệnh nhân, đã đối chiếu) và mục 8 (hồ sơ bệnh án, gợi ý thuốc và
    ICD-10). Đặt 3 lịch (A, B, C) vào 1 ca chiều sau hôm nay (H) rồi dời H về hôm nay, giữ nguyên ca sáng F của giai đoạn
    3; lịch D ở 1 ca sau H để thử "chưa đến ngày khám"."""
    xoa_gioi_han_ip()
    hom_nay = date.today()
    tu, den = hom_nay.isoformat(), (hom_nay + timedelta(days=13)).isoformat()
    token_bs, token_bs2, id_bac_si = p3["token_bs"], p3["token_bs2"], p3["id_bac_si"]
    MD = "/api/medical/bac-si/toi"

    # --- Dựng: ca H (chiều) và ca D sau hôm nay ---
    _, ca_ds = goi("bs-lich-lam-viec-dung-p4", "GET", f"{BS}/lich-lam-viec?tuNgay={tu}&denNgay={den}", token=token_bs,
                   mong=200)
    ca_sau = [c for c in ca_ds["duLieu"] if c["ngay"] > tu and c["trangThai"] == "HOAT_DONG"]
    ca_h = next(c for c in ca_sau if c["gioBatDau"] >= "12:00:00" and len(khung_con_cho(id_bac_si, c)) >= 1)
    ca_d = next(c for c in ca_sau if c["ngay"] > ca_h["ngay"])

    def gio_trong(ca):
        return khung_con_cho(id_bac_si, ca)[0]

    def cccd_cua(so):
        token = dang_nhap(f"dang-nhap-benh-nhan-{so:02d}-p4", f"benhnhan{so:02d}@eclinic.local", MAT_KHAU_MAU)
        _, tt = goi(f"thong-tin-dat-lich-{so:02d}-p4", "GET", "/api/booking/thong-tin-dat-lich/cua-toi", token=token,
                    mong=200)
        return tt["duLieu"]["banThan"]

    bn3, bn4 = cccd_cua(3), cccd_cua(4)
    _, lich_a = tu_dat_lich(2, ca_h["idLichLamViec"], gio_trong(ca_h), "Ho, đau họng, sốt 3 ngày nay")
    # B: khách đặt bằng CCCD của benhnhan04, họ tên nhập sai 1 chữ: lịch hẹn mang cờ cần đối chiếu (3.6)
    ten_sai = bn4["hoTen"][:-1] + ("m" if bn4["hoTen"][-1] != "m" else "n")
    lich_b = khach_dat_lich("b-can-doi-chieu", ca_h["idLichLamViec"], gio_trong(ca_h),
                            {"hoTen": ten_sai, "ngaySinh": bn4["ngaySinh"], "gioiTinh": bn4["gioiTinh"],
                             "soDienThoai": "0933100001", "cccd": bn4["cccd"]}, "Tái khám huyết áp")
    # C: trẻ dưới 72 tháng chưa có CCCD, có người giám hộ
    lich_c = khach_dat_lich("c-tre-em", ca_h["idLichLamViec"], gio_trong(ca_h),
                            {"hoTen": "Lê Minh Anh", "ngaySinh": "2023-05-20", "gioiTinh": "NU",
                             "soDienThoai": "0933100003", "cccd": None}, "Sốt, sổ mũi 2 ngày",
                            {"hoTen": "Lê Thị Hoa", "quanHe": "ME", "soDienThoai": "0933100003",
                             "cccd": "079190000555"})
    _, lich_d = tu_dat_lich(3, ca_d["idLichLamViec"], gio_trong(ca_d), "Đau dạ dày tái phát")
    doi_ca_ve_hom_nay(id_bac_si, ca_h, don_ca_hom_nay=False)
    xoa_gioi_han_ip()

    def tra_cuu(ten, ma, token=token_bs, mong=200):
        return goi(ten, "GET", f"{BS}/lich-hen/tra-cuu?ma={quote(ma, safe='')}", token=token, mong=mong)[1]

    lh_a = tra_cuu("bs-tra-cuu-ma-phieu", lich_a["maPhieuKham"])["duLieu"]
    id_a, ma_a = lh_a["id"], lh_a["maTraCuu"]
    id_b = tra_cuu("bs-tra-cuu-b", lich_b["maPhieuKham"])["duLieu"]["id"]
    id_c = tra_cuu("bs-tra-cuu-c", lich_c["maPhieuKham"])["duLieu"]["id"]
    id_d = tra_cuu("bs-tra-cuu-d", lich_d["maPhieuKham"])["duLieu"]["id"]

    # --- 7.1 ---
    tra_cuu("bs-tra-cuu-ma-tra-cuu", ma_a)
    tra_cuu("bs-tra-cuu-ma-tra-cuu-chu-thuong", ma_a.lower())
    tra_cuu("bs-tra-cuu-link", lich_a["linkPhieuKham"])
    tra_cuu("loi-bs-tra-cuu-ma-sai", "ABC-123", mong=404)
    tra_cuu("loi-bs2-tra-cuu-lich-bac-si-khac", ma_a, token=token_bs2, mong=404)
    goi("loi-bs-tra-cuu-thieu-ma", "GET", f"{BS}/lich-hen/tra-cuu", token=token_bs, mong=400)

    # --- 7.2, 7.3 ---
    goi("bs-ho-so-kham-theo-ma", "GET", f"{BS}/lich-hen/tra-cuu/ho-so-kham?ma={ma_a}", token=token_bs, mong=200)
    goi("loi-bs-ho-so-kham-ma-sai", "GET", f"{BS}/lich-hen/tra-cuu/ho-so-kham?ma=ECL-20261016-0000", token=token_bs,
        mong=404)
    goi("bs-ho-so-kham-can-doi-chieu", "GET", f"{BS}/lich-hen/{id_b}/ho-so-kham", token=token_bs, mong=200)
    goi("loi-bs-ho-so-kham-khong-co", "GET", f"{BS}/lich-hen/999999/ho-so-kham", token=token_bs, mong=404)

    # --- 7.4 ---
    _, hs_b = goi("bs-ho-so-kham-b", "GET", f"{BS}/lich-hen/{id_b}/ho-so-kham", token=token_bs, mong=200)
    b = hs_b["duLieu"]["hoSoBenhNhan"]
    sua_b = {"hoTen": b["hoTen"], "ngaySinh": b["ngaySinh"], "gioiTinh": b["gioiTinh"], "soDienThoai": b["soDienThoai"],
             "cccd": b["cccd"], "diaChi": "45 Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
             "soBaoHiemYTe": b["soBaoHiemYTe"]}
    goi("bs-sua-benh-nhan", "PUT", f"{BS}/lich-hen/{id_b}/benh-nhan", sua_b, token=token_bs, mong=200)
    sua_c = {"hoTen": "Lê Minh Anh", "ngaySinh": "2023-05-20", "gioiTinh": "NU", "soDienThoai": "0933100003",
             "cccd": bn3["cccd"], "diaChi": "", "soBaoHiemYTe": ""}
    goi("loi-bs-sua-benh-nhan-cccd-trung", "PUT", f"{BS}/lich-hen/{id_c}/benh-nhan", sua_c, token=token_bs, mong=409)
    goi("bs-sua-benh-nhan-dien-cccd", "PUT", f"{BS}/lich-hen/{id_c}/benh-nhan", {**sua_c, "cccd": "079323000777"},
        token=token_bs, mong=200)
    goi("loi-bs-sua-benh-nhan-doi-cccd", "PUT", f"{BS}/lich-hen/{id_c}/benh-nhan", {**sua_c, "cccd": "079323000778"},
        token=token_bs, mong=400)
    goi("loi-bs-sua-benh-nhan-thieu-ho-ten", "PUT", f"{BS}/lich-hen/{id_b}/benh-nhan", {**sua_b, "hoTen": " "},
        token=token_bs, mong=400)
    goi("loi-bs-sua-benh-nhan-khong-co", "PUT", f"{BS}/lich-hen/999999/benh-nhan", sua_b, token=token_bs, mong=404)

    # --- 7.5 ---
    goi("bs-da-doi-chieu", "POST", f"{BS}/lich-hen/{id_b}/da-doi-chieu", token=token_bs, mong=200)
    goi("bs-da-doi-chieu-lan-2", "POST", f"{BS}/lich-hen/{id_b}/da-doi-chieu", token=token_bs, mong=200)
    goi("loi-bs-da-doi-chieu-khong-co", "POST", f"{BS}/lich-hen/999999/da-doi-chieu", token=token_bs, mong=404)
    goi("bs-ho-so-kham-sau-doi-chieu", "GET", f"{BS}/lich-hen/{id_b}/ho-so-kham", token=token_bs, mong=200)

    # --- 8.5, 8.6 ---
    def tim(ten, loai, tu_khoa):
        return goi(ten, "GET", f"{MD}/{loai}?tuKhoa={quote(tu_khoa)}", token=token_bs, mong=200)[1]["duLieu"]

    id_thuoc = {t["tenThuoc"]: t["id"] for t in tim("bs-goi-y-thuoc", "thuoc", "para")}
    tim("bs-goi-y-thuoc-rong", "thuoc", "")
    for tu_khoa in ("amox", "cetiri", "amlo", "clora", "povidon"):
        id_thuoc.update({t["tenThuoc"]: t["id"] for t in tim(f"bs-goi-y-thuoc-dung-{tu_khoa}", "thuoc", tu_khoa)})
    tim("bs-goi-y-icd10-ma", "icd10", "J0")
    tim("bs-goi-y-icd10-ten", "icd10", "tăng huyết áp")
    tim("bs-goi-y-icd10-rong", "icd10", "")
    # Dựng: 1 thuốc ngừng dùng, 1 thuốc chưa xác minh (quản lý thuốc của quản trị viên ngoài phạm vi tài liệu)
    sql("UPDATE thuoc SET trang_thai = 'NGUNG_DUNG' WHERE ten_thuoc = 'Cloramphenicol'")
    sql("UPDATE thuoc SET da_xac_minh = FALSE WHERE ten_thuoc = 'Povidon iod'")
    tim("bs-goi-y-thuoc-sau-ngung-dung", "thuoc", "clora")

    # --- 8.1 ---
    def dong(ten, lieu, lan, ngay, so_luong, ghi_chu):
        return {"idThuoc": id_thuoc[ten], "lieuDung": lieu, "soLanMoiNgay": lan, "soNgayDung": ngay,
                "soLuong": so_luong, "ghiChuSuDung": ghi_chu}

    ghi_a = {"lyDoKham": "Ho, đau họng, sốt 3 ngày nay",
             "sinhHieu": {"mach": 92, "nhietDo": 38.2, "huyetApTamThu": 120, "huyetApTamTruong": 80, "nhipTho": 18,
                          "canNang": 65.5, "chieuCao": 170},
             "khamLamSang": "Họng đỏ, amidan sưng nhẹ, phổi không ran",
             "chanDoan": "Viêm họng cấp", "maIcdChinh": "J06.9", "chanDoanKemTheo": "Viêm mũi dị ứng",
             "ghiChu": "Uống nhiều nước, nghỉ ngơi; tái khám ngay nếu sốt cao trên 3 ngày",
             "ngayTaiKhamDeXuat": (hom_nay + timedelta(days=7)).isoformat(),
             "donThuoc": [dong("Paracetamol", "500mg/lần", 3, 5, 15, "Uống sau ăn, khi sốt trên 38.5 độ"),
                          dong("Amoxicillin", "500mg/lần", 2, 7, 14, "Uống sau ăn")]}
    ba = f"{MD}/lich-hen/{{}}/benh-an"
    goi("bs-ghi-benh-an", "POST", ba.format(id_a), ghi_a, token=token_bs, mong=201)
    goi("loi-bs-ghi-benh-an-lan-2", "POST", ba.format(id_a), ghi_a, token=token_bs, mong=409)
    goi("loi-bs-ghi-benh-an-chua-den-ngay", "POST", ba.format(id_d), ghi_a, token=token_bs, mong=409)
    goi("loi-bs-ghi-benh-an-khong-co", "POST", ba.format(999999), ghi_a, token=token_bs, mong=404)
    ghi_c = {"lyDoKham": "Sốt, sổ mũi 2 ngày", "sinhHieu": {"nhietDo": 38.0}, "chanDoan": "Viêm mũi họng cấp",
             "maIcdChinh": "J00", "donThuoc": [dong("Hapacol 250", "1 gói/lần", 3, 3, 9, "Hoà nước, uống khi sốt")]}
    goi("loi-bs-ghi-benh-an-tre-thieu-can-nang", "POST", ba.format(id_c), ghi_c, token=token_bs, mong=400)
    goi("bs-ghi-benh-an-tre-em", "POST", ba.format(id_c), {**ghi_c, "sinhHieu": {"nhietDo": 38.0, "canNang": 14.5}},
        token=token_bs, mong=201)
    # Các lỗi gửi vào B (chưa có hồ sơ bệnh án): không ghi nhầm gì
    ghi_b = {"lyDoKham": "Tái khám huyết áp", "sinhHieu": {"huyetApTamThu": 150, "huyetApTamTruong": 95},
             "chanDoan": "Tăng huyết áp", "maIcdChinh": "I10",
             "donThuoc": [dong("Amlodipin", "5mg/lần", 1, 60, 60, "Uống buổi sáng")]}
    loi_b = [("icd-sai", {"maIcdChinh": "X99.9"}),
             ("huyet-ap-thieu", {"sinhHieu": {"huyetApTamThu": 150}}),
             ("thuoc-trung", {"donThuoc": ghi_b["donThuoc"] * 2}),
             ("qua-30-ngay", {"maIcdChinh": "J06.9"}),
             ("tai-kham-hom-nay", {"ngayTaiKhamDeXuat": hom_nay.isoformat()}),
             ("thieu-chan-doan", {"chanDoan": " "}),
             ("ngoai-khoang", {"sinhHieu": {"mach": 300, "huyetApTamThu": 150, "huyetApTamTruong": 95}})]
    for ten, doi in loi_b:
        goi(f"loi-bs-ghi-benh-an-{ten}", "POST", ba.format(id_b), {**ghi_b, **doi}, token=token_bs, mong=400)
    goi("loi-bs-ghi-benh-an-thuoc-khong-co", "POST", ba.format(id_b),
        {**ghi_b, "donThuoc": [{**ghi_b["donThuoc"][0], "idThuoc": 999999}]}, token=token_bs, mong=404)
    goi("loi-bs-ghi-benh-an-thuoc-ngung-dung", "POST", ba.format(id_b),
        {**ghi_b, "donThuoc": [dong("Cloramphenicol", "1 giọt/lần", 3, 7, 1, "Nhỏ mắt")]}, token=token_bs, mong=409)
    goi("loi-bs-ghi-benh-an-thuoc-chua-xac-minh", "POST", ba.format(id_b),
        {**ghi_b, "donThuoc": [dong("Povidon iod", "Bôi ngoài da", 2, 7, 1, "Bôi vết thương")]}, token=token_bs,
        mong=409)
    goi("bs-ghi-benh-an-man-tinh", "POST", ba.format(id_b), ghi_b, token=token_bs, mong=201)

    # --- 8.3 ---
    goi("bs-xem-benh-an", "GET", ba.format(id_a), token=token_bs, mong=200)
    goi("loi-bs-xem-benh-an-chua-ghi", "GET", ba.format(id_d), token=token_bs, mong=404)

    # --- 8.2 ---
    sua_a = {**ghi_a, "chanDoan": "Viêm mũi họng cấp", "maIcdChinh": "J00",
             "donThuoc": [ghi_a["donThuoc"][0], dong("Cetirizin", "10mg/lần", 1, 5, 5, "Uống buổi tối")],
             "lyDoSua": "Test nhanh liên cầu âm tính: bỏ kháng sinh, thêm thuốc chống dị ứng", "phienBan": 1}
    goi("bs-sua-benh-an", "PUT", ba.format(id_a), sua_a, token=token_bs, mong=200)
    goi("loi-bs-sua-benh-an-phien-ban-cu", "PUT", ba.format(id_a), sua_a, token=token_bs, mong=409)
    goi("loi-bs-sua-benh-an-khong-doi", "PUT", ba.format(id_a), {**sua_a, "phienBan": 2}, token=token_bs, mong=400)
    goi("loi-bs-sua-benh-an-thieu-ly-do", "PUT", ba.format(id_a), {**sua_a, "lyDoSua": None, "phienBan": 2},
        token=token_bs, mong=400)
    sql(f"UPDATE ho_so_benh_an SET ngay_tao = ngay_tao - INTERVAL 8 DAY WHERE id_lich_hen = {id_c}")
    goi("loi-bs-sua-benh-an-het-han", "PUT", ba.format(id_c),
        {**ghi_c, "sinhHieu": {"nhietDo": 38.0, "canNang": 14.5}, "ghiChu": "Theo dõi nhiệt độ",
         "lyDoSua": "Thêm lời dặn", "phienBan": 1}, token=token_bs, mong=409)

    # --- 8.4 ---
    goi("bs-lich-su-sua", "GET", f"{ba.format(id_a)}/lich-su", token=token_bs, mong=200)
    goi("bs-lich-su-sua-chua-sua", "GET", f"{ba.format(p3['id_f2'])}/lich-su", token=token_bs, mong=200)
    goi("loi-bs2-lich-su-sua", "GET", f"{ba.format(id_a)}/lich-su", token=token_bs2, mong=404)
    goi("loi-bs-lich-su-sua-chua-ghi", "GET", f"{ba.format(id_d)}/lich-su", token=token_bs, mong=404)

    # --- 7.2, 7.3 sau khi sửa: kết quả khám phiên bản 2 ---
    goi("bs-ho-so-kham-sau-sua", "GET", f"{BS}/lich-hen/{id_a}/ho-so-kham", token=token_bs, mong=200)
    goi("bs-ho-so-kham-theo-ma-sau-sua", "GET", f"{BS}/lich-hen/tra-cuu/ho-so-kham?ma={ma_a}", token=token_bs,
        mong=200)
    return {"token_bs": token_bs, "id_bac_si": id_bac_si, "id_a": id_a, "ma_a": ma_a,
            "link_a": lich_a["linkPhieuKham"], "id_d": id_d, "id_f2": p3["id_f2"], "ca_d": ca_d}


def p5_quan_tri(p4):
    """Mục 9: quản trị viên tra cứu lịch hẹn theo mã, đánh dấu đã đối chiếu, xem lịch sử sửa hồ sơ bệnh án. Lịch E: khách
    đặt bằng CCCD của 1 bệnh nhân mẫu với họ tên nhập sai 1 chữ (mang cờ cần đối chiếu) vào ca D của giai đoạn 4."""
    xoa_gioi_han_ip()
    token_bs = p4["token_bs"]
    QT = "/api/booking/quan-tri/lich-hen"
    _, ad = goi("dang-nhap-admin", "POST", "/api/auth/login",
                {"email": "admin@eclinic.local", "matKhau": "Admin@123"}, mong=200)
    token_ad = ad["duLieu"]["accessToken"]

    def tra_cuu(ten, ma, token=token_ad, mong=200):
        return goi(ten, "GET", f"{QT}/tra-cuu?ma={quote(ma, safe='')}", token=token, mong=mong)[1]

    # --- 9.1 ---
    tra_cuu("qt-tra-cuu-ma-tra-cuu", p4["ma_a"])
    tra_cuu("qt-tra-cuu-ma-tra-cuu-chu-thuong", p4["ma_a"].lower())
    tra_cuu("qt-tra-cuu-link", p4["link_a"])
    tra_cuu("loi-qt-tra-cuu-ma-sai", "ABC-123", mong=404)
    goi("loi-qt-tra-cuu-thieu-ma", "GET", f"{QT}/tra-cuu", token=token_ad, mong=400)
    tra_cuu("loi-bs-tra-cuu-route-quan-tri", p4["ma_a"], token=token_bs, mong=403)

    # --- 9.2: lịch E cần đối chiếu ---
    gio = khung_con_cho(p4["id_bac_si"], p4["ca_d"])[0]
    lich_e = None
    for so in (1, 3, 2):
        token = dang_nhap(f"dang-nhap-benh-nhan-{so:02d}-p5", f"benhnhan{so:02d}@eclinic.local", MAT_KHAU_MAU)
        _, tt = goi(f"thong-tin-dat-lich-{so:02d}-p5", "GET", "/api/booking/thong-tin-dat-lich/cua-toi", token=token,
                    mong=200)
        bn = tt["duLieu"]["banThan"]
        ten_sai = bn["hoTen"][:-1] + ("m" if bn["hoTen"][-1] != "m" else "n")
        body = {"idLichLamViec": p4["ca_d"]["idLichLamViec"], "gioBatDauKhung": gio,
                "benhNhan": {"hoTen": ten_sai, "ngaySinh": bn["ngaySinh"], "gioiTinh": bn["gioiTinh"],
                             "soDienThoai": "0933100005", "cccd": bn["cccd"]}, "lyDoKham": "Khám sức khỏe định kỳ"}
        # 201, hoặc 409 khi bệnh nhân này đã giữ đủ lịch: thử bệnh nhân mẫu kế tiếp
        status, kq = goi(f"khach-dat-e-{so:02d}", "POST", "/api/booking/lich-hen", body)
        if status == 201:
            lich_e = kq["duLieu"]
            break
    assert lich_e, "Không bệnh nhân mẫu nào còn chỗ đặt lịch E"
    xoa_gioi_han_ip()
    id_e = tra_cuu("qt-tra-cuu-can-doi-chieu", lich_e["maPhieuKham"])["duLieu"]["id"]
    goi("qt-da-doi-chieu", "POST", f"{QT}/{id_e}/da-doi-chieu", token=token_ad, mong=200)
    goi("qt-da-doi-chieu-lan-2", "POST", f"{QT}/{id_e}/da-doi-chieu", token=token_ad, mong=200)
    tra_cuu("qt-tra-cuu-sau-doi-chieu", lich_e["maPhieuKham"])
    goi("loi-qt-da-doi-chieu-khong-co", "POST", f"{QT}/999999/da-doi-chieu", token=token_ad, mong=404)
    goi("loi-bs-da-doi-chieu-route-quan-tri", "POST", f"{QT}/{id_e}/da-doi-chieu", token=token_bs, mong=403)

    # --- 9.3 ---
    ls = "/api/medical/quan-tri/lich-hen/{}/benh-an/lich-su"
    goi("qt-lich-su-sua", "GET", ls.format(p4["id_a"]), token=token_ad, mong=200)
    goi("qt-lich-su-sua-chua-sua", "GET", ls.format(p4["id_f2"]), token=token_ad, mong=200)
    goi("loi-qt-lich-su-sua-chua-ghi", "GET", ls.format(p4["id_d"]), token=token_ad, mong=404)
    goi("loi-qt-lich-su-sua-khong-co", "GET", ls.format(999999), token=token_ad, mong=404)
    goi("loi-bs-lich-su-sua-route-quan-tri", "GET", ls.format(p4["id_a"]), token=token_bs, mong=403)


def p7_quan_tri_lich_hen(p4):
    """Mục 9.4-9.9 và 7.7-7.8 (DOANTOTNGH-14): quản trị viên xem lịch hẹn toàn viện, chi tiết + nhật ký, đặt lịch qua
    tổng đài, hủy / đổi lịch thay bệnh nhân, cấp lại liên kết; bác sĩ tìm lịch theo CCCD và cấp lại liên kết. Dựng: 2
    lịch tổng đài (không email / có email) vào 1 ca sau hôm nay còn chỗ của bác sĩ 01."""
    xoa_gioi_han_ip()
    QT = "/api/booking/quan-tri/lich-hen"
    token_bs = p4["token_bs"]
    token_bs2 = bac_si(2)
    _, ad = goi("dang-nhap-admin-p7", "POST", "/api/auth/login",
                {"email": "admin@eclinic.local", "matKhau": "Admin@123"}, mong=200)
    token_ad = ad["duLieu"]["accessToken"]
    hom_nay = date.today()
    tu, den = hom_nay.isoformat(), (hom_nay + timedelta(days=13)).isoformat()

    # --- Dựng: ca còn ít nhất 3 khung trống của bác sĩ 01 (từ ca muộn nhất: các giai đoạn trước dùng ca sớm) ---
    _, ca_ds = goi("bs-lich-lam-viec-p7", "GET", f"{BS}/lich-lam-viec?tuNgay={tu}&denNgay={den}", token=token_bs,
                   mong=200)
    ca_sau = [c for c in ca_ds["duLieu"] if c["ngay"] > tu and c["trangThai"] == "HOAT_DONG"]
    ca_t, khung_t = next((c, k) for c, k in ((c, khung_con_cho(p4["id_bac_si"], c)) for c in reversed(ca_sau))
                         if len(k) >= 3)

    # --- 9.5 Đặt lịch qua tổng đài ---
    goi_dien = {"hoTen": "Phạm Văn Gọi", "ngaySinh": "1985-07-12", "gioiTinh": "NAM", "soDienThoai": "0933200001",
                "cccd": "079185000201"}
    dat_t = {"idLichLamViec": ca_t["idLichLamViec"], "gioBatDauKhung": khung_t[0], "benhNhan": goi_dien,
             "lyDoKham": "Đau lưng 1 tuần nay", "ngonNgu": "VI"}
    _, hl1 = goi("qt-dat-lich-tong-dai", "POST", QT, dat_t, token=token_ad, mong=201)
    co_email = {"hoTen": "Lý Thị Đến", "ngaySinh": "1990-02-03", "gioiTinh": "NU", "soDienThoai": "0933200002",
                "cccd": "079190000202", "email": "den@example.com"}
    _, hl2 = goi("qt-dat-lich-tong-dai-co-email", "POST", QT,
                 {**dat_t, "gioBatDauKhung": khung_t[1], "benhNhan": co_email, "lyDoKham": "Khám tổng quát",
                  "ngonNgu": "EN"}, token=token_ad, mong=201)
    goi("loi-qt-dat-lich-thieu-ly-do", "POST", QT, {**dat_t, "lyDoKham": " "}, token=token_ad, mong=400)
    goi("loi-qt-dat-lich-trung-gio", "POST", QT, dat_t, token=token_ad, mong=409)
    goi("loi-bs-dat-lich-tong-dai", "POST", QT, dat_t, token=token_bs, mong=403)
    id1, id2 = hl1["duLieu"]["idLichHen"], hl2["duLieu"]["idLichHen"]

    # --- 9.4 Danh sách toàn viện ---
    goi("qt-lich-hen-ds", "GET", f"{QT}?kichThuoc=3", token=token_ad, mong=200)
    goi("qt-lich-hen-ds-loc", "GET", f"{QT}?tuNgay={tu}&denNgay={den}&idBacSi={p4['id_bac_si']}&kenhDat=HOTLINE"
                                     f"&trangThai=DA_XAC_NHAN", token=token_ad, mong=200)
    goi("qt-lich-hen-ds-tu-khoa-cccd", "GET", f"{QT}?tuKhoa=079185000201", token=token_ad, mong=200)
    goi("qt-lich-hen-ds-tu-khoa-ma", "GET", f"{QT}?tuKhoa={hl1['duLieu']['phieuKham']['maTraCuu']}", token=token_ad,
        mong=200)
    goi("loi-qt-lich-hen-ds-qua-366", "GET", f"{QT}?tuNgay=2026-01-01&denNgay=2027-06-01", token=token_ad, mong=400)
    goi("loi-qt-lich-hen-ds-trang-thai-sai", "GET", f"{QT}?trangThai=ABC", token=token_ad, mong=400)
    goi("loi-bs-lich-hen-ds-quan-tri", "GET", QT, token=token_bs, mong=403)

    # --- 9.6 Chi tiết ---
    goi("qt-lich-hen-chi-tiet", "GET", f"{QT}/{id1}", token=token_ad, mong=200)
    goi("loi-qt-lich-hen-chi-tiet-khong-co", "GET", f"{QT}/999999", token=token_ad, mong=404)

    # --- 9.9 Cấp lại liên kết ---
    cl = f"{QT}/{{}}/cap-lai-lien-ket"
    goi("loi-qt-cap-lai-sai-cccd", "POST", cl.format(id1), {"cccd": "079000000000", "cachGui": "EMAIL"}, token=token_ad,
        mong=403)
    goi("loi-qt-cap-lai-khong-co-email", "POST", cl.format(id1), {"cccd": "079185000201", "cachGui": "EMAIL"},
        token=token_ad, mong=409)
    goi("qt-cap-lai-man-hinh", "POST", cl.format(id1), {"cccd": "079185000201", "cachGui": "MAN_HINH"}, token=token_ad,
        mong=200)
    goi("qt-cap-lai-email", "POST", cl.format(id2), {"cccd": "079190000202", "cachGui": "EMAIL"}, token=token_ad,
        mong=200)
    goi("loi-qt-cap-lai-cccd-sai-dang", "POST", cl.format(id1), {"cccd": "123", "cachGui": "EMAIL"}, token=token_ad,
        mong=400)
    goi("loi-qt-cap-lai-khong-co", "POST", cl.format(999999), {"cccd": "079185000201", "cachGui": "EMAIL"},
        token=token_ad, mong=404)

    # --- 9.8 Đổi lịch thay bệnh nhân ---
    dl = f"{QT}/{{}}/doi-lich"
    doi = {"idLichLamViec": ca_t["idLichLamViec"], "gioBatDauKhung": khung_t[2],
           "lyDo": "Bệnh nhân gọi tổng đài xin đổi sang giờ muộn hơn"}
    goi("loi-qt-doi-lich-thieu-ly-do", "POST", dl.format(id1), {**doi, "lyDo": ""}, token=token_ad, mong=400)
    _, moi = goi("qt-doi-lich", "POST", dl.format(id1), doi, token=token_ad, mong=201)
    goi("loi-qt-doi-lich-da-doi", "POST", dl.format(id1), doi, token=token_ad, mong=409)
    id_moi = moi["duLieu"]["lichHen"]["id"]
    goi("qt-lich-hen-chi-tiet-sau-doi", "GET", f"{QT}/{id_moi}", token=token_ad, mong=200)

    # --- 9.7 Hủy thay bệnh nhân ---
    huy = f"{QT}/{{}}/huy"
    goi("loi-qt-huy-thieu-ly-do", "POST", huy.format(id2), {"lyDo": "  "}, token=token_ad, mong=400)
    goi("qt-huy", "POST", huy.format(id2), {"lyDo": "Bác sĩ đi công tác đột xuất, phòng khám đã gọi báo bệnh nhân"},
        token=token_ad, mong=200)
    goi("loi-qt-huy-lan-2", "POST", huy.format(id2), {"lyDo": "Bận"}, token=token_ad, mong=409)
    goi("loi-qt-huy-khong-co", "POST", huy.format(999999), {"lyDo": "Bận"}, token=token_ad, mong=404)

    # --- 7.7, 7.8 Bác sĩ: tìm theo CCCD, cấp lại liên kết ---
    goi("bs-lich-hen-theo-cccd", "GET", f"{BS}/lich-hen/theo-cccd?cccd=079185000201", token=token_bs, mong=200)
    goi("bs-lich-hen-theo-cccd-rong", "GET", f"{BS}/lich-hen/theo-cccd?cccd=079000000000", token=token_bs, mong=200)
    goi("loi-bs-lich-hen-theo-cccd-sai-dang", "GET", f"{BS}/lich-hen/theo-cccd?cccd=12", token=token_bs, mong=400)
    bcl = f"{BS}/lich-hen/{{}}/cap-lai-lien-ket"
    goi("bs-cap-lai-man-hinh", "POST", bcl.format(id_moi), {"cccd": "079185000201", "cachGui": "MAN_HINH"},
        token=token_bs, mong=200)
    goi("loi-bs-cap-lai-sai-cccd", "POST", bcl.format(id_moi), {"cccd": "079000000000", "cachGui": "MAN_HINH"},
        token=token_bs, mong=403)
    goi("loi-bs2-cap-lai-lich-nguoi-khac", "POST", bcl.format(id_moi), {"cccd": "079185000201", "cachGui": "MAN_HINH"},
        token=token_bs2, mong=404)
    goi("bs-lich-hen-sau-p7", "GET", f"{BS}/lich-hen?ngay={ca_t['ngay']}", token=token_bs, mong=200)


def main():
    for tep in lay_mau.THU_MUC.glob("*.json") if lay_mau.THU_MUC.exists() else ():
        tep.unlink()
    p4 = p4_kham_benh(p3_lich_va_hang_doi())
    p5_quan_tri(p4)
    p7_quan_tri_lich_hen(p4)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
