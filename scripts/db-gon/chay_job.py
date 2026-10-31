"""Kiểm tra job của bộ kiểm tra hợp đồng API khi gộp bảng (DOANTOTNGH-19): chạy job nhắc lịch / nhắc tái khám / gửi thông
báo trên bản dump V36 rồi ghi lại những gì job đã làm, để so code cũ với code mới bằng so_sanh.py.

    PYTHONUTF8=1 /c/Users/buiho/AppData/Local/Python/bin/python scripts/db-gon/chay_job.py <ten> [--truoc] [--build]

chup.py chụp với job tắt nên không thử được job. Script này khôi phục bản dump (Flyway của code đang chạy nâng DB lên),
chuẩn bị việc đến hạn (CHUAN_BI), bật job với chu kỳ ngắn, nhắc cả ngày, thêm các mốc 6h / 12h / 20h / 7d / 14d (để lịch hẹn trong dump có mốc đến hạn mà chưa xử lý) và nhắc tái khám trước 30 ngày để job thật sự gửi trên dữ liệu
cũ, chờ tới khi không còn gì để gửi, rồi ghi temp/db-gon/<ten>/_job.json (cùng dạng tệp của chup.py):
  - các dòng thông báo / sự kiện chờ gửi / email job tạo ra (bỏ cột id, thời gian, số lần thử); 2 hàng chờ gửi đọc
    theo tên cột cũ, từ bảng cũ nếu còn, không thì từ hang_cho_gui (phase 4),
  - thong_bao.ma_nguon / email ma_nguon ('booking:<id>') không so giá trị (id sự kiện mới của code mới nằm sau các dòng
    email đã chép) mà đếm số dòng trỏ đúng 1 dòng sự kiện chờ gửi (maNguonKhop),
  - trạng thái nhắc của từng lịch hẹn và hồ sơ bệnh án, đọc từ bảng cũ nếu còn, không thì từ cột mới,
  - số thông báo nhắc theo loại.
--truoc / --build như chup.py. Chạy xong thì tắt stack.
"""

import json
import re
import subprocess
import sys
import time

import chung

JOB_CHAY = {
    "SEED_DATA": "false",
    "BOOKING_NOTIFY_INTERVAL": "5s", "NOTIFICATION_MAIL_INTERVAL": "5s", "BOOKING_REMINDER_INTERVAL": "15s",
    "BOOKING_REMINDER_FROM": "00:00", "BOOKING_REMINDER_TO": "23:59",
    "BOOKING_REMINDER_BEFORE": "6h,12h,20h,1d,3d,7d,14d", "FOLLOWUP_REMINDER_DAYS": "30",
}
# Bảng ghi job tạo ra: (bảng, khoá chính, cột bỏ qua khi so)
BANG_MOI = [
    ("thong_bao", "id_thong_bao", {"ngay_tao", "da_doc", "ma_nguon"}),
]
# 2 hàng chờ gửi, gộp vào hang_cho_gui ở phase 4: bảng cũ -> (hang, khoá cũ, cột cũ trừ khoá, cột bỏ qua khi so)
HANG_CHO = {
    "su_kien_thong_bao": ("SU_KIEN", "id_su_kien",
                          ["kenh", "loai", "id_tai_khoan", "email_nhan", "ngon_ngu", "id_lich_hen", "id_yeu_cau",
                           "khoa_mau", "tham_so", "noi_dung", "so_lan_loi", "ngay_tao", "ngay_gui", "tep_lich"],
                          {"ngay_tao", "ngay_gui", "so_lan_loi"}),
    "email_gui": ("EMAIL_GUI", "id_email",
                  ["ma_nguon", "nguoi_nhan", "ngon_ngu", "khoa_mau", "tham_so", "id_lich_hen", "trang_thai",
                   "so_lan_thu", "lan_thu_ke_tiep", "loi_cuoi", "ngay_tao", "ngay_gui", "tep_lich"],
                  {"ma_nguon", "ngay_tao", "ngay_gui", "so_lan_thu", "lan_thu_ke_tiep", "loi_cuoi"}),
}
# Cột cũ -> cột trong hang_cho_gui khi khác tên
DOI_TEN = {"nguoi_nhan": "email_nhan", "so_lan_thu": "so_lan_loi"}
DTSTAMP = re.compile(r"DTSTAMP:\d{8}T\d{6}Z?")
# Chạy trên schema V36 của bản dump, trước khi app (và Flyway) lên, nên 2 lần chạy chuẩn bị giống hệt nhau. Không có
# bước này thì job không có gì để làm: lịch hẹn trong dump đều đặt khi đã ở trong khoảng của mốc, ngày tái khám đã báo.
#  - lùi ngày đặt mọi lịch hẹn 30 ngày: mốc chưa xử lý của lịch còn hiệu lực trong 3 ngày tới đều được nhắc
#  - hồ sơ bệnh án có dòng nhắc nhỏ nhất: hẹn tái khám sau 5 ngày, đã báo, chưa nhắc -> nhánh nhắc trước
#  - hồ sơ bệnh án có dòng nhắc lớn nhất (nếu khác): bác sĩ xoá ngày -> nhánh quên ngày đã báo
#  - hồ sơ bệnh án chưa có dòng nhắc: hẹn tái khám sau 3 + id ngày -> nhánh báo ngày mới
CHUAN_BI = """
UPDATE lich_hen SET ngay_tao = ngay_tao - INTERVAL 30 DAY;
UPDATE ho_so_benh_an SET ngay_tai_kham_de_xuat = CURDATE() + INTERVAL 5 DAY
WHERE id_ho_so_benh_an = (SELECT MIN(id_ho_so_benh_an) FROM nhac_tai_kham);
UPDATE nhac_tai_kham SET ngay_da_bao = CURDATE() + INTERVAL 5 DAY, da_nhac_truoc = FALSE
WHERE id_ho_so_benh_an = (SELECT * FROM (SELECT MIN(id_ho_so_benh_an) FROM nhac_tai_kham) t);
UPDATE ho_so_benh_an SET ngay_tai_kham_de_xuat = NULL
WHERE id_ho_so_benh_an = (SELECT MAX(id_ho_so_benh_an) FROM nhac_tai_kham)
  AND id_ho_so_benh_an <> (SELECT MIN(id_ho_so_benh_an) FROM nhac_tai_kham);
UPDATE ho_so_benh_an b SET b.ngay_tai_kham_de_xuat = CURDATE() + INTERVAL (3 + b.id_ho_so_benh_an) DAY
WHERE NOT EXISTS (SELECT 1 FROM nhac_tai_kham n WHERE n.id_ho_so_benh_an = b.id_ho_so_benh_an);
"""


def sql_json(cau):
    """Mỗi dòng kết quả là 1 giá trị JSON (cột đầu); --raw để chuỗi JSON không bị mysql thoát lần nữa."""
    kq = subprocess.run(["docker", "exec", "-i", "eclinic-mysql", "sh", "-c",
                         'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" -N --raw --default-character-set=utf8mb4 '
                         '"$MYSQL_DATABASE" 2>&1'],
                        input=cau, capture_output=True, text=True, encoding="utf-8", check=False)
    if kq.returncode != 0:
        raise SystemExit("SQL lỗi: " + kq.stdout[:500])
    return [json.loads(dong) for dong in kq.stdout.splitlines() if dong]


def mot(cau):
    return chung.sql_dong(cau)[0][0]


def co_bang(bang):
    return mot(f"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() "
               f"AND table_name = '{bang}'") != "0"


def cot(bang):
    return [d[0] for d in chung.sql_dong(f"SELECT column_name FROM information_schema.columns WHERE table_schema = "
                                         f"DATABASE() AND table_name = '{bang}' ORDER BY ordinal_position")]


def dong_moi(bang, khoa, bo_qua, tu_id):
    giu = [c for c in cot(bang) if c != khoa and c not in bo_qua]
    doi = ", ".join(f"'{c}', {c}" for c in giu)
    return sap_xep(che_dtstamp(sql_json(f"SELECT JSON_OBJECT({doi}) FROM {bang} WHERE {khoa} > {tu_id}")))


def dem_hang_cho():
    """Số dòng của mỗi hàng chờ gửi; gọi trước khi app lên (bảng cũ, schema V36)."""
    return {bang: int(mot(f"SELECT COUNT(*) FROM {bang}")) for bang in HANG_CHO}


def nguon_hang_cho(bang):
    """(FROM ... WHERE, khoá, hàm cột cũ -> biểu thức) của 1 hàng chờ gửi, đọc bảng cũ nếu còn."""
    hang, khoa, _, _ = HANG_CHO[bang]
    if co_bang(bang):
        return f"{bang} WHERE 1 = 1", khoa, lambda c: c
    return f"hang_cho_gui WHERE hang = '{hang}'", "id_hang_cho", lambda c: DOI_TEN.get(c, c)


def hang_cho_moi(bang, da_co, bo_qua=None):
    """Các dòng của 1 hàng chờ gửi có thêm trong lần chạy (bỏ da_co dòng đầu theo id), theo tên cột cũ."""
    _, _, cac_cot, bo_qua_mac_dinh = HANG_CHO[bang]
    bo_qua = bo_qua_mac_dinh if bo_qua is None else bo_qua
    tu, khoa, cot_moi = nguon_hang_cho(bang)
    doi = ", ".join(f"'{c}', {cot_moi(c)}" for c in cac_cot if c not in bo_qua)
    return che_dtstamp(sql_json(f"SELECT JSON_OBJECT({doi}) FROM {tu} ORDER BY {khoa} LIMIT {da_co[bang]}, 1000000"))


def con_cho_gui():
    if co_bang("su_kien_thong_bao"):
        return int(mot("SELECT COUNT(*) FROM su_kien_thong_bao WHERE ngay_gui IS NULL")) \
            + int(mot("SELECT COUNT(*) FROM email_gui WHERE trang_thai = 'CHO_GUI'"))
    return int(mot("SELECT COUNT(*) FROM hang_cho_gui WHERE (hang = 'SU_KIEN' AND ngay_gui IS NULL) "
                   "OR (hang = 'EMAIL_GUI' AND trang_thai = 'CHO_GUI')"))


def ma_nguon_khop(tu_id, da_co):
    """[số dòng, số dòng có ma_nguon 'booking:<id>' trỏ đúng 1 dòng SU_KIEN] của thông báo / email mới."""
    tu, khoa, _ = nguon_hang_cho("su_kien_thong_bao")
    id_su_kien = {d[0] for d in chung.sql_dong(f"SELECT {khoa} FROM {tu}")}
    thong_bao = [d[0] for d in chung.sql_dong(f"SELECT ma_nguon FROM thong_bao WHERE id_thong_bao > "
                                              f"{tu_id['thong_bao']} AND ma_nguon IS NOT NULL")]
    email = [d["ma_nguon"] for d in hang_cho_moi("email_gui", da_co, set())]

    def dem(cac_ma):
        return [len(cac_ma), sum(1 for m in cac_ma if m.startswith("booking:") and m[8:] in id_su_kien)]

    return {"thongBao": dem(thong_bao), "email": dem(email)}


def che_dtstamp(cac_dong):
    for d in cac_dong:
        for k, v in d.items():
            if isinstance(v, str):
                d[k] = DTSTAMP.sub("DTSTAMP:<che>", v)
    return cac_dong


def sap_xep(cac_dong):
    return sorted(cac_dong, key=lambda d: json.dumps(d, ensure_ascii=False, sort_keys=True))


def trang_thai_nhac():
    if co_bang("nhac_lich_hen"):
        lich = sql_json("SELECT JSON_ARRAY(id_lich_hen, MIN(so_phut_truoc), "
                        "MIN(CASE WHEN da_gui THEN so_phut_truoc END)) FROM nhac_lich_hen GROUP BY id_lich_hen")
    else:
        lich = sql_json("SELECT JSON_ARRAY(id_lich_hen, nhac_moc_da_xu_ly, nhac_moc_da_gui) FROM lich_hen "
                        "WHERE nhac_moc_da_xu_ly IS NOT NULL")
    if co_bang("nhac_tai_kham"):
        tai_kham = sql_json("SELECT JSON_ARRAY(id_ho_so_benh_an, ngay_da_bao, CAST(da_nhac_truoc AS SIGNED)) "
                            "FROM nhac_tai_kham WHERE ngay_da_bao IS NOT NULL OR da_nhac_truoc")
    else:
        tai_kham = sql_json("SELECT JSON_ARRAY(id_ho_so_benh_an, tai_kham_ngay_da_bao, "
                            "CAST(tai_kham_da_nhac_truoc AS SIGNED)) FROM ho_so_benh_an "
                            "WHERE tai_kham_ngay_da_bao IS NOT NULL OR tai_kham_da_nhac_truoc")
    return {"lichHen": sorted(lich), "hoSoBenhAn": sorted(tai_kham)}


def cho_xong(tu_id, da_co):
    """Chờ tới khi không còn gì chờ gửi và số dòng mới đứng yên qua 3 lần đếm (ít nhất 60 giây, tối đa 5 phút)."""
    bat_dau, truoc, dung_yen = time.time(), None, 0
    while time.time() - bat_dau < 300:
        time.sleep(15)
        dem = tuple(int(mot(f"SELECT COUNT(*) FROM {bang} WHERE {khoa} > {tu_id[bang]}")) for bang, khoa, _ in BANG_MOI) \
            + tuple(len(hang_cho_moi(bang, da_co)) for bang in HANG_CHO)
        con_cho = con_cho_gui()
        dung_yen = dung_yen + 1 if dem == truoc and con_cho == 0 else 0
        truoc = dem
        print(f"  {int(time.time() - bat_dau):3d}s dòng mới {dem}, còn chờ gửi {con_cho}")
        if dung_yen >= 3 and time.time() - bat_dau >= 60:
            return
    print("CẢNH BÁO: job chưa xong sau 5 phút")


def main():
    doi_so = sys.argv[1:]
    if not doi_so or doi_so[0].startswith("-"):
        raise SystemExit(__doc__)
    ten, truoc, build = doi_so[0], "--truoc" in doi_so, "--build" in doi_so
    thu_muc = chung.THU_MUC / ten
    chung.xoa_thu_muc(thu_muc)
    chung.tat()
    try:
        chung.len_mysql()
        chung.khoi_phuc(chung.TEP_DUMP)
        chung.sql(CHUAN_BI)
        tu_id = {bang: int(mot(f"SELECT COALESCE(MAX({khoa}), 0) FROM {bang}")) for bang, khoa, _ in BANG_MOI}
        da_co = dem_hang_cho()
        chung.len_app(truoc=truoc, build=build, moi_truong=JOB_CHAY)
        cho_xong(tu_id, da_co)
        than = {bang: dong_moi(bang, khoa, bo_qua, tu_id[bang]) for bang, khoa, bo_qua in BANG_MOI}
        than.update({bang: sap_xep(hang_cho_moi(bang, da_co)) for bang in HANG_CHO})
        than["maNguonKhop"] = ma_nguon_khop(tu_id, da_co)
        than["trangThaiNhac"] = trang_thai_nhac()
        dem = {}
        for d in than["thong_bao"]:
            dem[d["loai"]] = dem.get(d["loai"], 0) + 1
        than["soThongBaoTheoLoai"] = dict(sorted(dem.items()))
        ghi = {"vaiTro": "job", "route": "JOB", "duongDan": "_job", "ngonNgu": "-", "status": 0, "than": than}
        (thu_muc / "_job.json").write_text(json.dumps(ghi, ensure_ascii=False, indent=1, sort_keys=True),
                                           encoding="utf-8")
        print("Thông báo job tạo theo loại:", than["soThongBaoTheoLoai"])
        print(f"Đã ghi {thu_muc / '_job.json'}")
    finally:
        chung.tat()


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
