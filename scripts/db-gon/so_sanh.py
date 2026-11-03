"""Bước 3 của bộ kiểm tra hợp đồng API khi gộp bảng (DOANTOTNGH-19): so 2 lần chụp của chup.py.

    PYTHONUTF8=1 /c/Users/buiho/AppData/Local/Python/bin/python scripts/db-gon/so_sanh.py <a> <b>

In từng chỗ khác nhau theo route + ngôn ngữ + đường dẫn JSON (dạng duLieu.noiDung.3.id). Thoát mã 1 nếu còn chỗ khác
không khớp mục nào của cho-phep.json: [{"route": "GET /api/...", "duongDan": "duLieu.noiDung.*.id", "lyDo": "..."}]
(route và duongDan là mẫu fnmatch; route so với mẫu đường dẫn trong TUYEN của chup.py).
"""

import json
import sys
from fnmatch import fnmatchcase
from pathlib import Path

import chung

TEP_CHO_PHEP = Path(__file__).with_name("cho-phep.json")


def khac_nhau(a, b, duong=""):
    if type(a) is not type(b):
        yield duong, a, b
    elif isinstance(a, dict):
        for k in sorted(set(a) | set(b)):
            con = f"{duong}.{k}" if duong else k
            if k not in a or k not in b:
                yield con, a.get(k, "<không có>"), b.get(k, "<không có>")
            else:
                yield from khac_nhau(a[k], b[k], con)
    elif isinstance(a, list):
        for i in range(max(len(a), len(b))):
            con = f"{duong}.{i}" if duong else str(i)
            if i >= len(a) or i >= len(b):
                yield con, a[i] if i < len(a) else "<không có>", b[i] if i < len(b) else "<không có>"
            else:
                yield from khac_nhau(a[i], b[i], con)
    elif a != b:
        yield duong, a, b


def rut_gon(gt):
    chu = json.dumps(gt, ensure_ascii=False)
    return chu if len(chu) <= 120 else chu[:117] + "..."


def main():
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    thu_muc_a, thu_muc_b = (chung.THU_MUC / t for t in sys.argv[1:])
    cho_phep = json.loads(TEP_CHO_PHEP.read_text(encoding="utf-8"))
    tep_a = {p.name for p in thu_muc_a.glob("*.json")}
    tep_b = {p.name for p in thu_muc_b.glob("*.json")}
    con_lai = da_cho_phep = 0
    for ten in sorted(tep_a ^ tep_b):
        print(f"CHỈ CÓ Ở {'a' if ten in tep_a else 'b'}: {ten}")
        con_lai += 1
    for ten in sorted(tep_a & tep_b):
        a = json.loads((thu_muc_a / ten).read_text(encoding="utf-8"))
        b = json.loads((thu_muc_b / ten).read_text(encoding="utf-8"))
        cac_cho = list(khac_nhau({"status": a["status"], "than": a["than"]},
                                 {"status": b["status"], "than": b["than"]}))
        for duong, gia_a, gia_b in cac_cho:
            khop = next((m for m in cho_phep if fnmatchcase(a["route"], m["route"])
                         and fnmatchcase(duong, m["duongDan"])), None)
            if khop:
                da_cho_phep += 1
                continue
            con_lai += 1
            print(f"{a['route']} [{a['ngonNgu']}] {a['duongDan']}\n    {duong}: {rut_gon(gia_a)} -> {rut_gon(gia_b)}")
    print(f"\n{len(tep_a & tep_b)} response chung; {con_lai} chỗ khác chưa được phép; {da_cho_phep} chỗ khác đã cho phép")
    sys.exit(1 if con_lai else 0)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
