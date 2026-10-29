"""Phần dùng chung của bộ kiểm tra hợp đồng API khi gộp bảng (DOANTOTNGH-19): dựng / tắt stack thử, khôi phục bản dump,
chạy SQL, gọi API. Xem scripts/db-gon/README.md.

Stack thử là project compose `eclinicdbgon`. Tên container cố định (eclinic-mysql...) nên mỗi lúc chỉ chạy được 1 stack:
nếu stack `eclinic` của máy đang chạy thì dừng lại và báo, không tự tắt.
"""

import gzip
import json
import os
import shutil
import subprocess
import time
import urllib.error
import urllib.request
from pathlib import Path
from urllib.parse import quote

GOC = Path(__file__).resolve().parents[2]
THU_MUC = GOC / "temp/db-gon"
TEP_MOC = THU_MUC / "moc.json"
TEP_DUMP = THU_MUC / "v36.sql.gz"
API = "http://127.0.0.1:8080"
PROJECT = "eclinicdbgon"
DICH_VU = ["identity-service", "catalog-service", "booking-service", "medical-service", "notification-service",
           "chatbot-service", "report-service", "api-gateway"]
COMPOSE_TRUOC = GOC / "scripts/db-gon/compose.truoc.yml"

# Giá trị ghi đè .env cho mọi lần chạy: không gửi email thật, không đụng ảnh thật, không chiếm cổng 3306 của máy,
# tài khoản quản trị / mật khẩu mẫu cố định (các script lấy mẫu dùng các giá trị này), giới hạn gọi đủ rộng.
MOI_TRUONG_THU = {
    "DB_PORT": "3316", "REDIS_PORT": "6389", "PMA_PORT": "8091",
    "MAIL_MODE": "console", "NOTIFICATION_MAIL_MODE": "console",
    "CLOUDINARY_FOLDER": "eclinic-smoke",
    "ADMIN_EMAIL": "admin@eclinic.local", "ADMIN_PASSWORD": "Admin@123",
    "SEED_DATA": "true", "SEED_PASSWORD": "Demo@123",
    "GATEWAY_RATE_LIMIT": "100000", "GATEWAY_AUTH_RATE_LIMIT": "100000", "GATEWAY_CHATBOT_RATE_LIMIT": "100000",
}
# Lúc chụp: các job chạy theo chu kỳ không chạy (initialDelay = chu kỳ) và không bổ sung dữ liệu mẫu, để 2 lần chụp trên
# cùng bản dump ra cùng dữ liệu
JOB_TAT = {"BOOKING_NOTIFY_INTERVAL": "24h", "BOOKING_REMINDER_INTERVAL": "24h", "NOTIFICATION_MAIL_INTERVAL": "24h",
           "SEED_DATA": "false"}


def chay(lenh, **kw):
    kw.setdefault("check", True)
    return subprocess.run(lenh, **kw)


def compose(*doi_so, truoc=False, moi_truong=None, **kw):
    lenh = ["docker", "compose", "-p", PROJECT, "-f", str(GOC / "docker-compose.yml")]
    if truoc:
        lenh += ["-f", str(COMPOSE_TRUOC)]
    env = {**os.environ, **MOI_TRUONG_THU, **(moi_truong or {})}
    return chay(lenh + ["--profile", "app", *doi_so], cwd=GOC, env=env, **kw)


def kiem_tra_stack_may():
    """Dừng nếu container của project khác (stack `eclinic` của máy) đang giữ các tên container cố định."""
    ra = chay(["docker", "ps", "--format", '{{.Names}}|{{.Label "com.docker.compose.project"}}'],
              capture_output=True, text=True).stdout.split()
    khac = [d for d in ra if d.startswith("eclinic-") and not d.endswith("|" + PROJECT)]
    if khac:
        raise SystemExit("Stack khác đang chạy (" + ", ".join(khac) + "). Tắt nó trước rồi chạy lại.")


def tat():
    compose("down", "-v", "--remove-orphans")


def len_mysql():
    kiem_tra_stack_may()
    compose("up", "-d", "--wait", "mysql", "redis")
    # Healthcheck của MySQL đã qua trong lúc khởi tạo: chờ tới khi đăng nhập được bằng user ứng dụng
    for _ in range(60):
        if sql("SELECT 1", kiem_tra=False).returncode == 0:
            return
        time.sleep(2)
    raise SystemExit("MySQL không sẵn sàng")


def len_app(truoc=False, build=False, moi_truong=None):
    doi_so = ["up", "-d", "--wait"] + (["--build"] if build else ["--no-build"])
    compose(*doi_so, truoc=truoc, moi_truong=moi_truong)


def sql(cau, kiem_tra=True, root=False):
    """Chạy SQL trên MySQL của stack thử; trả CompletedProcess (stdout dạng tab, không tiêu đề)."""
    nguoi = 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot' if root else \
        'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER"'
    kq = chay(["docker", "exec", "-i", "eclinic-mysql", "sh", "-c",
               nguoi + ' -N --default-character-set=utf8mb4 "$MYSQL_DATABASE" 2>&1'],
              input=cau, capture_output=True, text=True, encoding="utf-8", check=False)
    if kiem_tra and kq.returncode != 0:
        raise SystemExit("SQL lỗi: " + kq.stdout[:500])
    return kq


def sql_dong(cau):
    return [dong.split("\t") for dong in sql(cau).stdout.splitlines() if dong]


def dump(dich):
    dich.parent.mkdir(parents=True, exist_ok=True)
    kq = chay(["docker", "exec", "eclinic-mysql", "sh", "-c",
               'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --triggers '
               '--default-character-set=utf8mb4 "$MYSQL_DATABASE"'], capture_output=True)
    with gzip.open(dich, "wb") as f:
        f.write(kq.stdout)


def khoi_phuc(nguon):
    """Dựng lại DB `eclinic` từ bản dump (chỉ MySQL + Redis đang chạy)."""
    sql('DROP DATABASE IF EXISTS eclinic; CREATE DATABASE eclinic CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; '
        "GRANT ALL ON eclinic.* TO 'eclinic'@'%';", root=True)
    with gzip.open(nguon, "rb") as f:
        noi_dung = f.read()
    kq = chay(["docker", "exec", "-i", "eclinic-mysql", "sh", "-c",
               'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 "$MYSQL_DATABASE"'],
              input=noi_dung, capture_output=True, check=False)
    if kq.returncode != 0:
        raise SystemExit("Nạp dump lỗi: " + kq.stderr.decode("utf-8", "replace")[:500])


def goi(method, duong_dan, body=None, token=None, ngon_ngu=None, timeout=60):
    """Gọi API; trả (status, content-type, bytes)."""
    headers = {"Accept": "application/json"}
    du_lieu = None
    if body is not None:
        du_lieu = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = "Bearer " + token
    if ngon_ngu:
        headers["Accept-Language"] = ngon_ngu
    yc = urllib.request.Request(API + quote(duong_dan, safe="/?=&%:,"), data=du_lieu, method=method, headers=headers)
    try:
        with urllib.request.urlopen(yc, timeout=timeout) as ph:
            return ph.status, ph.headers.get("Content-Type") or "", ph.read()
    except urllib.error.HTTPError as loi:
        return loi.code, loi.headers.get("Content-Type") or "", loi.read()


def dang_nhap(email, mat_khau):
    status, _, tho = goi("POST", "/api/auth/login", {"email": email, "matKhau": mat_khau})
    if status != 200:
        raise SystemExit(f"Đăng nhập {email} lỗi {status}: {tho[:300]!r}")
    return json.loads(tho)["duLieu"]["accessToken"]


def doc_moc():
    return json.loads(TEP_MOC.read_text(encoding="utf-8"))


def xoa_thu_muc(thu_muc):
    if thu_muc.exists():
        shutil.rmtree(thu_muc)
    thu_muc.mkdir(parents=True)
