"""Bước 1 của bộ kiểm tra hợp đồng API khi gộp bảng (DOANTOTNGH-19): dựng bộ dữ liệu đóng băng ở V36.

Dựng stack thử mới (dữ liệu mẫu), chạy 2 script lấy mẫu của tài liệu API (kết quả ghi vào temp/db-gon/kich-ban/, mẫu
của tài liệu không bị đụng), bổ sung các thao tác 2 script đó không làm, kiểm tra mọi bảng sắp gộp đều có dòng, chọn
mã mẫu cho các route có tham số đường dẫn (moc.json) rồi dump DB ra temp/db-gon/v36.sql.gz.

    PYTHONUTF8=1 /c/Users/buiho/AppData/Local/Python/bin/python scripts/db-gon/tao_du_lieu.py

Chỉ chạy ở commit trước khi gộp bảng: bản dump là điểm xuất phát của mọi lần so sánh sau này.
"""

import json
import subprocess
import sys
import time
from datetime import date

import chung
from chung import GOC, sql, sql_dong

sys.path.insert(0, str(GOC / "scripts/api-doc"))

BANG_PHAI_CO = ["danh_gia", "nhac_lich_hen", "nhac_tai_kham", "nguoi_giam_ho", "lien_ket_ho_so_bi_tu_choi",
                "lich_su_lich_hen", "lich_su_ca_lam_viec", "lich_su_cau_hinh", "lich_su_sua_benh_an",
                "su_kien_thong_bao", "email_gui", "icd10", "anh_bac_si", "nguoi_than_da_luu", "thong_bao",
                "ho_so_benh_an"]
# Lúc dựng dữ liệu: job nhắc lịch / nhắc tái khám chạy dày để kịp ghi nhac_lich_hen, nhac_tai_kham
JOB_NHANH = {"BOOKING_REMINDER_INTERVAL": "15s", "BOOKING_REMINDER_FROM": "00:00", "BOOKING_REMINDER_TO": "23:59"}


def mot(cau):
    dong = sql_dong(cau)
    return dong[0][0] if dong and dong[0] and dong[0][0] != "NULL" else None


def kiem(ten, status, mong=200):
    print(f"{'ok  ' if status == mong else 'SAI '} {status} {ten}")
    if status != mong:
        raise SystemExit(f"Bổ sung dữ liệu lỗi ở bước {ten}")


def json_cua(status_ct_tho):
    status, _, tho = status_ct_tho
    return status, (json.loads(tho) if tho else None)


def cho_seed():
    for _ in range(90):
        if int(mot("SELECT COUNT(*) FROM lich_lam_viec") or 0) > 0 and int(mot("SELECT COUNT(*) FROM anh_bac_si") or 0):
            return
        time.sleep(2)
    raise SystemExit("Dữ liệu mẫu chưa được tạo")


def chay_kich_ban():
    import lay_mau
    lay_mau.THU_MUC = chung.THU_MUC / "kich-ban/lay-mau"
    lay_mau.main()
    import bsqt_lay_mau
    lay_mau.THU_MUC = chung.THU_MUC / "kich-ban/bsqt"
    bsqt_lay_mau.main()


def bo_sung(token_ad):
    # Cấu hình: đổi 1 giá trị rồi đổi lại -> 2 dòng lịch sử
    _, ch = json_cua(chung.goi("GET", "/api/booking/quan-tri/cau-hinh", token=token_ad))
    gia_tri = {k: v for k, v in ch["duLieu"].items() if k not in ("ngaySua", "quanTriSua")}
    for lan, so_ngay in enumerate((gia_tri["soNgayDuocSuaDanhGia"] + 1, gia_tri["soNgayDuocSuaDanhGia"])):
        status, kq = json_cua(chung.goi("PUT", "/api/booking/quan-tri/cau-hinh",
                                        {**gia_tri, "soNgayDuocSuaDanhGia": so_ngay}, token=token_ad))
        kiem(f"sua-cau-hinh-{lan}", status)
        gia_tri["phienBan"] = kq["duLieu"]["phienBan"]

    # Ảnh bác sĩ: sửa chú thích 1 ảnh, đảo thứ tự ảnh của bác sĩ có nhiều ảnh nhất
    id_bs = mot("SELECT id_bac_si FROM anh_bac_si GROUP BY id_bac_si ORDER BY COUNT(*) DESC, id_bac_si LIMIT 1")
    cac_anh = [d[0] for d in sql_dong(f"SELECT id_anh_bac_si, loai FROM anh_bac_si WHERE id_bac_si = {id_bs} "
                                      "ORDER BY thu_tu")]
    loai = mot(f"SELECT loai FROM anh_bac_si WHERE id_anh_bac_si = {cac_anh[0]}")
    kiem("sua-anh", chung.goi("PUT", f"/api/catalog/quan-tri/bac-si/{id_bs}/anh/{cac_anh[0]}",
                              {"loai": loai, "chuThich": "Ảnh đã sửa chú thích", "chuThichEn": "Edited caption"},
                              token=token_ad)[0])
    kiem("dao-thu-tu-anh", chung.goi("PUT", f"/api/catalog/quan-tri/bac-si/{id_bs}/anh/thu-tu",
                                     {"idAnh": [int(i) for i in reversed(cac_anh)]}, token=token_ad)[0])

    # Ca làm việc: sửa 1 ca, hủy 1 ca (ca còn xa, chưa có lịch hẹn) -> lịch sử SUA / HUY
    ca = sql_dong("""
        SELECT c.id_lich_lam_viec, c.id_phong_kham, TIME_FORMAT(c.gio_bat_dau, '%H:%i'), TIME_FORMAT(c.gio_ket_thuc, '%H:%i'),
               c.so_luot_toi_da_moi_gio, c.thoi_luong_luot_phut
        FROM lich_lam_viec c
        WHERE c.trang_thai = 'HOAT_DONG' AND c.ngay_lam_viec >= CURDATE() + INTERVAL 4 DAY
          AND NOT EXISTS (SELECT 1 FROM khung_gio_kham k JOIN lich_hen l ON l.id_khung_gio = k.id_khung_gio
                          WHERE k.id_lich_lam_viec = c.id_lich_lam_viec)
        ORDER BY c.ngay_lam_viec, c.id_lich_lam_viec LIMIT 2""")
    (id_sua, phong, bat_dau, ket_thuc, n, t), (id_huy, *_) = ca
    n, t = int(n), int(t)
    n, t = (n - 1, t) if n > 1 else (n, max(5, t - 5))
    kiem("sua-ca", chung.goi("PUT", f"/api/booking/quan-tri/lich-lam-viec/{id_sua}",
                             {"idPhongKham": int(phong), "gioBatDau": bat_dau, "gioKetThuc": ket_thuc,
                              "soLuotToiDaMoiGio": n, "thoiLuongLuotPhut": t}, token=token_ad)[0])
    kiem("huy-ca", chung.goi("POST", f"/api/booking/quan-tri/lich-lam-viec/{id_huy}/huy",
                             {"lyDo": "Phòng khám bảo trì thiết bị"}, token=token_ad)[0])

    # Liên kết hồ sơ bị từ chối
    if int(mot("SELECT COUNT(*) FROM lien_ket_ho_so_bi_tu_choi")) == 0:
        _, cho = json_cua(chung.goi("GET", "/api/booking/quan-tri/ho-so-benh-nhan/cho-xac-minh", token=token_ad))
        noi_dung = cho["duLieu"]["noiDung"]
        if not noi_dung:
            ho_so = sql_dong("SELECT cccd, ho_ten FROM ho_so_benh_nhan WHERE id_tai_khoan IS NULL AND cccd IS NOT NULL "
                             "ORDER BY id_ho_so_benh_nhan LIMIT 1")[0]
            import lay_mau
            lay_mau.tai_khoan_moi("tu-choi-lien-ket", ho_so[1], "0977000123", ho_so[0])
            _, cho = json_cua(chung.goi("GET", "/api/booking/quan-tri/ho-so-benh-nhan/cho-xac-minh", token=token_ad))
            noi_dung = cho["duLieu"]["noiDung"]
        id_ho_so = noi_dung[0]["hoSo"]["id"]
        kiem("tu-choi-lien-ket", chung.goi("POST", f"/api/booking/quan-tri/ho-so-benh-nhan/{id_ho_so}/tu-choi",
                                           {"lyDo": "Thông tin không khớp giấy tờ"}, token=token_ad)[0])


def cho_job_nhac():
    for _ in range(40):
        if int(mot("SELECT COUNT(*) FROM nhac_lich_hen")) and int(mot("SELECT COUNT(*) FROM nhac_tai_kham")) \
                and int(mot("SELECT COUNT(*) FROM su_kien_thong_bao WHERE ngay_gui IS NULL")) == 0 \
                and int(mot("SELECT COUNT(*) FROM email_gui WHERE trang_thai = 'CHO_GUI'")) == 0:
            return
        time.sleep(5)
    print("CẢNH BÁO: job nhắc / gửi chưa xong sau 200 giây")


def kiem_tra_du_bang():
    thieu = []
    for bang in BANG_PHAI_CO:
        so = int(mot(f"SELECT COUNT(*) FROM {bang}"))
        print(f"  {bang:28} {so}")
        if so == 0:
            thieu.append(bang)
    for cau, ten in (("SELECT COUNT(*) FROM su_kien_thong_bao WHERE ngay_gui IS NOT NULL", "su_kien_thong_bao đã gửi"),
                     ("SELECT COUNT(*) FROM email_gui WHERE trang_thai = 'DA_GUI'", "email_gui đã gửi"),
                     ("SELECT COUNT(*) FROM lich_su_ca_lam_viec WHERE nguon <> 'DU_LIEU_MAU'", "lich_su_ca ngoài seed")):
        so = int(mot(cau))
        print(f"  {ten:28} {so}")
        if so == 0:
            thieu.append(ten)
    if thieu:
        raise SystemExit("Bảng chưa có dữ liệu: " + ", ".join(thieu))


def chon_moc():
    """Mã mẫu cho tham số đường dẫn / query của các route GET, chọn theo SQL trên dữ liệu vừa dựng."""
    id_bn = mot("SELECT id_tai_khoan FROM tai_khoan WHERE email = 'benhnhan01@eclinic.local'")
    id_bs = mot("""SELECT b.id_bac_si FROM bac_si b ORDER BY
                     (SELECT COUNT(*) FROM ho_so_benh_an h WHERE h.id_bac_si = b.id_bac_si) DESC,
                     (SELECT COUNT(*) FROM danh_gia d WHERE d.id_bac_si = b.id_bac_si) DESC, b.id_bac_si LIMIT 1""")
    lh = "SELECT l.id_lich_hen FROM lich_hen l "

    def ids(cau, gioi_han=1):
        return [int(d[0]) for d in sql_dong(f"{cau} LIMIT {gioi_han}")]

    lich_hen = {
        "giam_ho": ids(lh + "WHERE l.id_nguoi_giam_ho IS NOT NULL ORDER BY l.id_lich_hen"),
        "danh_gia": ids("SELECT id_lich_hen FROM danh_gia ORDER BY id_lich_hen"),
        "benh_an_da_sua": ids("SELECT b.id_lich_hen FROM ho_so_benh_an b WHERE EXISTS (SELECT 1 FROM "
                              "lich_su_sua_benh_an s WHERE s.id_ho_so_benh_an = b.id_ho_so_benh_an) ORDER BY 1"),
        "nhieu_lich_su": ids("SELECT id_lich_hen FROM lich_su_lich_hen GROUP BY id_lich_hen "
                             "ORDER BY COUNT(*) DESC, id_lich_hen"),
        "da_nhac": ids("SELECT id_lich_hen FROM nhac_lich_hen ORDER BY id_lich_hen"),
    }
    lich_hen_bs = {
        "benh_an": ids(f"SELECT b.id_lich_hen FROM ho_so_benh_an b WHERE b.id_bac_si = {id_bs} ORDER BY "
                       "(SELECT COUNT(*) FROM lich_su_sua_benh_an s WHERE s.id_ho_so_benh_an = b.id_ho_so_benh_an) "
                       "DESC, b.id_lich_hen", 2),
        "giam_ho": ids(lh + f"WHERE l.id_bac_si = {id_bs} AND l.id_nguoi_giam_ho IS NOT NULL ORDER BY l.id_lich_hen"),
    }
    phieu_bn = [d[0] for d in sql_dong(
        "SELECT l.ma_token_phieu_kham FROM lich_hen l JOIN ho_so_benh_nhan h USING (id_ho_so_benh_nhan) "
        f"WHERE h.id_tai_khoan = {id_bn} OR l.id_tai_khoan_dat = {id_bn} ORDER BY "
        "EXISTS (SELECT 1 FROM danh_gia d WHERE d.id_lich_hen = l.id_lich_hen) DESC, "
        "l.id_nguoi_giam_ho IS NOT NULL DESC, l.id_lich_hen LIMIT 3")]
    phieu_cong_khai = [d[0] for d in sql_dong(
        "SELECT ma_token_phieu_kham FROM lich_hen WHERE id_lich_hen IN (" +
        ",".join(str(i) for i in lich_hen["giam_ho"] + lich_hen["nhieu_lich_su"]) + ")")]
    ngay_bs = sql_dong(f"SELECT MIN(DATE(k.gio_bat_dau)), MAX(DATE(k.gio_bat_dau)) FROM lich_hen l "
                       f"JOIN khung_gio_kham k USING (id_khung_gio) WHERE l.id_bac_si = {id_bs}")[0]
    ngay_tat_ca = sql_dong("SELECT MIN(DATE(k.gio_bat_dau)), MAX(DATE(k.gio_bat_dau)) FROM lich_hen l "
                           "JOIN khung_gio_kham k USING (id_khung_gio)")[0]
    ca = ids("SELECT id_lich_lam_viec FROM lich_su_ca_lam_viec GROUP BY id_lich_lam_viec "
             "ORDER BY SUM(nguon <> 'DU_LIEU_MAU') DESC, COUNT(*) DESC, id_lich_lam_viec", 3)
    ho_so = ids("SELECT id_ho_so_benh_nhan FROM ho_so_benh_nhan h ORDER BY "
                "EXISTS (SELECT 1 FROM nguoi_giam_ho g WHERE g.id_ho_so_benh_nhan = h.id_ho_so_benh_nhan) DESC, "
                "EXISTS (SELECT 1 FROM lien_ket_ho_so_bi_tu_choi r WHERE r.id_ho_so_benh_nhan = h.id_ho_so_benh_nhan) "
                "DESC, id_ho_so_benh_nhan", 3)
    ho_so += ids("SELECT id_ho_so_benh_nhan FROM lien_ket_ho_so_bi_tu_choi ORDER BY 1")
    return {
        "taiKhoan": {
            "quanTri": {"email": "admin@eclinic.local", "matKhau": "Admin@123"},
            "bacSi": {"email": mot(f"SELECT t.email FROM bac_si b JOIN tai_khoan t USING (id_tai_khoan) "
                                   f"WHERE b.id_bac_si = {id_bs}"), "matKhau": "Demo@123"},
            "benhNhan": {"email": "benhnhan01@eclinic.local", "matKhau": "Demo@123"},
        },
        "idBacSi": int(id_bs),
        "idBacSiCoAnh": int(mot("SELECT id_bac_si FROM anh_bac_si GROUP BY id_bac_si ORDER BY COUNT(*) DESC, "
                                "id_bac_si LIMIT 1")),
        "idChuyenKhoa": int(mot(f"SELECT id_chuyen_khoa FROM bac_si WHERE id_bac_si = {id_bs}")),
        "idTaiKhoan": [int(id_bn), int(mot(f"SELECT id_tai_khoan FROM bac_si WHERE id_bac_si = {id_bs}"))],
        "idLichHen": sorted({i for v in lich_hen.values() for i in v}),
        "idLichHenBacSi": sorted({i for v in lich_hen_bs.values() for i in v}),
        "maPhieuKhamBenhNhan": phieu_bn,
        "maPhieuKham": sorted(set(phieu_cong_khai)),
        "maSoKham": [d[0] for d in sql_dong(
            "SELECT b.ma_token_so_kham FROM ho_so_benh_an b WHERE b.id_lich_hen IN (" +
            ",".join(str(i) for i in lich_hen["benh_an_da_sua"] + lich_hen_bs["benh_an"]) + ")")],
        "maTraCuuBacSi": mot(f"SELECT l.ma_tra_cuu FROM lich_hen l WHERE l.id_bac_si = {id_bs} ORDER BY "
                             "EXISTS (SELECT 1 FROM ho_so_benh_an b WHERE b.id_lich_hen = l.id_lich_hen) DESC LIMIT 1"),
        "maTraCuu": mot("SELECT ma_tra_cuu FROM lich_hen WHERE id_lich_hen = " + str(lich_hen["nhieu_lich_su"][0])),
        "cccdBacSi": mot(f"SELECT h.cccd FROM lich_hen l JOIN ho_so_benh_nhan h USING (id_ho_so_benh_nhan) "
                         f"WHERE l.id_bac_si = {id_bs} AND h.cccd IS NOT NULL ORDER BY l.id_lich_hen LIMIT 1"),
        "ngayBacSi": ngay_bs,
        "ngayTatCa": ngay_tat_ca,
        "idLichLamViec": ca,
        "idHoSoBenhNhan": sorted(set(ho_so)),
        "idPhienChat": ids(f"SELECT id_phien_chat FROM phien_chat WHERE id_tai_khoan = {id_bn} ORDER BY 1"),
    }


def main():
    chung.tat()
    chung.len_mysql()
    chung.len_app(moi_truong=JOB_NHANH)
    cho_seed()
    chay_kich_ban()
    token_ad = chung.dang_nhap("admin@eclinic.local", "Admin@123")
    bo_sung(token_ad)
    cho_job_nhac()
    print("Số dòng các bảng sắp gộp:")
    kiem_tra_du_bang()
    moc = chon_moc()
    moc["commit"] = subprocess.run(["git", "rev-parse", "--short", "HEAD"], capture_output=True, text=True,
                                   cwd=GOC).stdout.strip()
    moc["ngayDung"] = date.today().isoformat()
    moc["thoiDiemDump"] = mot("SELECT DATE_FORMAT(NOW(6), '%Y-%m-%dT%H:%i:%s.%f')")
    chung.dump(chung.TEP_DUMP)
    chung.TEP_MOC.write_text(json.dumps(moc, ensure_ascii=False, indent=2), encoding="utf-8")
    print("Đã ghi", chung.TEP_DUMP, "và", chung.TEP_MOC)
    chung.tat()


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
