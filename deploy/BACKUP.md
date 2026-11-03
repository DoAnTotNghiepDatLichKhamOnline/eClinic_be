# Bản dự phòng trên laptop (Cloudflare Tunnel)

Khi VM Google Cloud gặp sự cố lúc demo: chạy toàn bộ backend trên laptop với dữ liệu mới nhất của VM, mở ra Internet
bằng Cloudflare Tunnel (không cần mở cổng), rồi trỏ frontend sang URL đó.

## 1. Cài trên máy mới (một lần)

1. Cài **Docker Desktop**. Máy cần khoảng **16 GB RAM** vì stack dùng khoảng 6 GB.
2. Clone repo và dùng đúng commit VM đang chạy (`main`). Flyway sẽ từ chối dữ liệu nếu migration khác nhau.
   ```powershell
   git clone https://github.com/DoAnTotNghiepDatLichKhamOnline/eClinic_be.git
   cd eClinic_be
   git checkout main
   ```
3. Lấy `.env` của VM, để `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS`, mail và Cloudinary giống hệt máy chủ:
   ```powershell
   scp -i $HOME\.ssh\eclinic_gcp ubuntu@<IP_VM>:~/eClinic_be/.env .env
   ```
   Thêm vào `.env`: `BACKUP_VM_HOST=<IP_VM>`. Nếu cổng 3306 trên laptop đã bị dùng, đặt thêm `DB_PORT=3316`.
4. *(Tuỳ chọn, cần tên miền trên Cloudflare)* **Named tunnel** cho URL cố định: vào Cloudflare **Zero Trust → Networks →
   Tunnels → Create a tunnel → Cloudflared**, chọn Docker và chép token (phần sau `--token`). Ở **Public Hostname**, đặt
   ví dụ `api-backup.<domain>` → Service `HTTP` `api-gateway:8080`. Ghi vào `.env`:
   ```
   CF_TUNNEL_TOKEN=<token>
   CF_TUNNEL_HOSTNAME=api-backup.<domain>
   ```
   Không có token thì script dùng **quick tunnel** `https://*.trycloudflare.com`: miễn phí, không cần tài khoản, nhưng URL
   đổi mỗi lần khởi động.

## 2. Hôm trước buổi demo

```powershell
powershell -File deploy\backup-up.ps1 -Pull
```

Lệnh này lấy bản dump mới từ VM vào `backups\` rồi build và chạy stack. Lần build đầu mất vài phút, nên làm trước.
Script in ra `Backup ready: <url>`. Mở `<url>/actuator/health` để kiểm tra, rồi tắt stack (lệnh ở mục 4).

## 3. Khi VM gặp sự cố

1. Chạy `powershell -File deploy\backup-up.ps1`. Lệnh này dùng dữ liệu đã có trên laptop.
   - VM còn SSH được: thêm `-Pull` để lấy dữ liệu mới nhất.
   - Dùng bản dump mới nhất trong `backups\`: thêm `-Restore`.
   - Chỉ định file dump: thêm `-Dump <file.sql.gz>`.
2. Trong repo frontend, sửa `vercel.json`: đổi `destination` của rule `/api/:path*` thành dòng script in ra, rồi push.
   Vercel tự deploy, khoảng 1 phút. Tên miền frontend không đổi, nên CORS, cookie và đăng nhập Google vẫn chạy.
   Với named tunnel, URL cố định nên có thể chuẩn bị sẵn commit này trên một nhánh.
3. Laptop phải cắm sạc và **không được sleep**. Nếu chạy lại script, dữ liệu vẫn giữ nguyên (không restore nếu không thêm
   tham số). Quick tunnel đổi URL khi tunnel khởi động lại (sau `-Pull`, `-Restore`, `-Dump` hoặc khi khởi động lại máy).
   Script luôn in URL hiện tại; nếu URL khác thì sửa lại `vercel.json`.

## 4. Quay lại VM và tắt bản dự phòng

Revert commit `vercel.json` ở frontend, rồi tắt stack (dữ liệu vẫn giữ trong volume):

```powershell
docker compose -p eclinicbackup -f docker-compose.yml -f docker-compose.backup.yml --profile app --profile quick --profile named down
```

**Lưu ý:** mọi request qua tunnel đến gateway với cùng một IP, nên chung giới hạn tần suất. Nếu nhiều người đăng nhập
cùng lúc, đặt `GATEWAY_AUTH_RATE_LIMIT=200` trong `.env` rồi chạy lại script.
