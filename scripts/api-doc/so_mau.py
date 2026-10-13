"""So JSON mẫu của response thành công trong tài liệu với response thật đã lưu ở temp/api-doc/mau (lay_mau.py).

Chỉ so cấu trúc của `duLieu`, không so giá trị (mẫu trong tài liệu đã chuẩn hoá ngày, id, tên):
- Mục 11 trở đi: mỗi object phải có đúng các trường của response thật, đúng thứ tự.
- Mục 5-10: mẫu được lược trường đang `null` (quy ước 3.1), nên chỉ báo trường mẫu có mà response thật không có,
  và trường response thật có giá trị mà mẫu không có.
`null` ở một bên khớp với mọi kiểu ở bên kia. Mỗi mẫu chỉ cần khớp 1 response thật của cùng route.

    python scripts/api-doc/so_mau.py [tep.docx]
"""

import json
import re
import sys
from pathlib import Path

import check
import kit

GOC = Path(__file__).resolve().parents[2]
THU_MUC = GOC / "temp/api-doc/mau"
MUC_SO_CHAT = 11


def lech(mau, that, chat, duong="duLieu"):
    """Chỗ lệch đầu tiên giữa mẫu và response thật, None nếu khớp."""
    if mau is None or that is None:
        return None
    if isinstance(mau, dict) != isinstance(that, dict) or isinstance(mau, list) != isinstance(that, list):
        return f"{duong}: mẫu là {type(mau).__name__}, thật là {type(that).__name__}"
    if isinstance(mau, dict):
        if chat and list(mau) != list(that):
            return f"{duong}: mẫu {list(mau)} khác thật {list(that)}"
        thua = [k for k in mau if k not in that]
        thieu = [k for k in that if k not in mau and that[k] is not None]
        if thua or thieu:
            return f"{duong}: mẫu thừa {thua}, thiếu trường có giá trị {thieu}"
        for khoa in mau:
            loi = lech(mau[khoa], that[khoa], chat, f"{duong}.{khoa}")
            if loi:
                return loi
    elif isinstance(mau, list):
        for i, phan_tu in enumerate(mau):
            cac_loi = [lech(phan_tu, t, chat, f"{duong}[{i}]") for t in that]
            if not that or all(cac_loi):
                return cac_loi[0] if that else f"{duong}: response thật là mảng rỗng"
    elif isinstance(mau, bool) != isinstance(that, bool) or isinstance(mau, str) != isinstance(that, str):
        return f"{duong}: mẫu {mau!r} khác kiểu với thật {that!r}"
    return None


def mau_trong_tai_lieu(chu):
    """[(mục, method, đường dẫn mẫu, JSON)] của mọi khối JSON sau nhãn RESPONSE THÀNH CÔNG."""
    ket_qua, muc, route, la_response, khoi = [], "", None, False, None
    for dong in chu[chu.index("[H1] 5. "):chu.index("[H1] Phụ lục A")].split("\n"):
        if khoi is not None:
            khoi.append(dong)
            if dong == "}":
                ket_qua.append((muc, *route, json.loads("\n".join(khoi))))
                khoi = None
        elif dong.startswith("[H3] "):
            muc, route, la_response = dong[5:].split("  ")[0], None, False
        elif route is None and re.match(rf"\| ({check.METHOD}) \| (/\S+) \|$", dong):
            route = re.match(rf"\| ({check.METHOD}) \| (/\S+) \|$", dong).groups()
        elif dong.startswith("RESPONSE THÀNH CÔNG"):
            la_response = True
        elif dong.isupper() and not dong.startswith(("{", "}")):
            la_response = False
        elif dong == "{" and la_response and route:
            khoi = [dong]
    return ket_qua


def response_that(cac_route):
    """{(method, đường dẫn mẫu): [body]} của các lần gọi 2xx; mỗi lần gọi thuộc route có nhiều đoạn chữ cố định nhất."""
    ket_qua = {r: [] for r in cac_route}
    for tep in sorted(THU_MUC.glob("*.json")):
        ghi = json.loads(tep.read_text(encoding="utf-8"))
        if not 200 <= ghi["status"] < 300 or not isinstance(ghi["body"], dict):
            continue
        duong = ghi["path"].split("?")[0]
        khop = [r for r in cac_route if r[0] == ghi["method"]
                and re.fullmatch(re.sub(r"\{\w+\}", "[^/]+", r[1]), duong)]
        if khop:
            ket_qua[max(khop, key=lambda r: len(re.sub(r"\{\w+\}", "", r[1])))].append(ghi["body"])
    return ket_qua


def so(duong_dan):
    chu = kit.dump(duong_dan)
    cac_route = sorted(set(re.findall(rf"^\[H3\] .*\n\| ({check.METHOD}) \| (/\S+) \|$", chu, re.M)))
    that = response_that(cac_route)
    cac_mau = mau_trong_tai_lieu(chu)
    loi, da_so = [], 0
    for muc, method, duong, mau in cac_mau:
        cac_body = that[(method, duong)]
        if not cac_body:
            continue
        da_so += 1
        chat = int(muc.split(".")[0]) >= MUC_SO_CHAT
        cac_lech = [lech(mau.get("duLieu"), b.get("duLieu"), chat) for b in cac_body]
        if ("duLieu" in mau) != any("duLieu" in b for b in cac_body):
            loi.append(f"{muc} {method} {duong}: mẫu {'có' if 'duLieu' in mau else 'không có'} duLieu, thật thì ngược lại")
        elif all(cac_lech):
            loi.append(f"{muc} {method} {duong}: {cac_lech[0]}")
    khong_co = [r for r in cac_route if not that[r]]
    print(f"Mẫu response thành công: {len(cac_mau)} · đã so với response thật: {da_so} · lệch: {len(loi)}")
    print(f"Route không có response thật để so ({len(khong_co)}):", ", ".join(f"{m} {d}" for m, d in khong_co) or "không")
    co_mau = {(m, d) for _, m, d, _ in cac_mau}
    print("Route không có JSON mẫu thành công:", ", ".join(f"{m} {d}" for m, d in cac_route if (m, d) not in co_mau)
          or "không")
    return loi


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    cac_loi = so(sys.argv[1] if len(sys.argv) > 1 else str(GOC / kit.TAI_LIEU))
    for dong in cac_loi:
        print("LỆCH:", dong)
    sys.exit(1 if cac_loi else 0)
