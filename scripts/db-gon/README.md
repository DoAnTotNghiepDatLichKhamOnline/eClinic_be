# Kiểm tra hợp đồng API khi gộp bảng (DOANTOTNGH-19)

Gộp bảng chỉ đổi DB: route, request, response cho frontend phải giữ nguyên. Bộ script này chứng minh điều đó trên dữ liệu
thật: 1 bản dump V36 đóng băng, chụp mọi route GET bằng code cũ và code mới **trong cùng 1 ngày** (response phụ thuộc
ngày hôm nay), rồi so sánh.

Chạy từ thư mục gốc, Git Bash, Docker đang chạy. Tắt stack `eclinic` của máy trước (tên container trùng); script dừng và
báo nếu nó đang chạy. Mỗi lệnh tự tắt stack thử (`-p eclinicdbgon`, `down -v`) khi xong.

```bash
PY="/c/Users/buiho/AppData/Local/Python/bin/python"; export PYTHONUTF8=1

# 1 lần, ở commit trước khi gộp (0901c02): dựng dữ liệu -> temp/db-gon/v36.sql.gz + moc.json
$PY scripts/db-gon/tao_du_lieu.py
# image của lần dựng đó được giữ lại: docker tag eclinicdbgon-<service> eclinic-truoc/<service>:0901c02

# Mỗi lần so sánh (cùng 1 ngày):
$PY scripts/db-gon/chup.py truoc-p1 --truoc        # code cũ (image đóng băng, compose.truoc.yml)
$PY scripts/db-gon/chup.py sau-p1 --build          # code hiện tại, Flyway nâng bản dump lên V37+
$PY scripts/db-gon/so_sanh.py truoc-p1 sau-p1      # mã thoát 1 nếu có chỗ khác chưa được phép

# Job (nhắc lịch, nhắc tái khám, gửi thông báo) trên bản dump, cũng cùng 1 ngày, chạy liền nhau:
$PY scripts/db-gon/chay_job.py job-truoc-p1 --truoc
$PY scripts/db-gon/chay_job.py job-sau-p1          # sau lần --build ở trên thì không cần build lại
$PY scripts/db-gon/so_sanh.py job-truoc-p1 job-sau-p1

# Đường ghi (chuỗi lệnh ghi cố định trong hàm kich_ban của thu_ghi.py), cũng cùng 1 ngày:
$PY scripts/db-gon/thu_ghi.py ghi-truoc-p2 --truoc
$PY scripts/db-gon/thu_ghi.py ghi-sau-p2           # sau lần --build ở trên thì không cần build lại
$PY scripts/db-gon/so_sanh.py ghi-truoc-p2 ghi-sau-p2
```

- Kết quả chụp: `temp/db-gon/<ten>/`, mỗi response 1 tệp (route, vai trò, ngôn ngữ, status, thân đã chuẩn hoá). Tệp nhị
  phân (pdf, xlsx, ics, qr) chỉ ghi status + kiểu tệp.
- Chuẩn hoá: ngày giờ sau thời điểm dump thành `<sau-moc>`; khoá sinh ra lúc chụp (phiên đăng nhập) thành `<che>`. Lúc chụp
  các job chạy theo chu kỳ không chạy và dữ liệu mẫu không được bổ sung.
- `cho-phep.json`: chỗ khác được chấp nhận (route + đường dẫn JSON + lý do), ví dụ `id` chỉ đọc của dòng đã gộp. Thêm mục
  mới phải có lý do và được ghi trong Trail của phase.
- `chup.py` dừng nếu `docs/api/*.md` có route GET chưa nằm trong `TUYEN` hoặc `BO_QUA`.
