# Dữ liệu test: ca làm cuối tuần + lịch hẹn

`them_ca_bac_si_cuoi_tuan.sql` thêm ca làm thứ 7 và Chủ nhật cho một bác sĩ (mặc định `bacsi01@eclinic.local`), mỗi ca đủ
10 lịch hẹn để test màn hình bác sĩ, hàng đợi khám và lịch làm việc.

**Đây không phải migration Flyway.** Không đặt vào `common/src/main/resources/db/migration`: migration chạy ở mọi môi
trường (kể cả DB trống của test CI, nơi không có `bacsi01`), không sửa được sau khi đã chạy, và sẽ đưa dữ liệu giả vào
DB thật. File chỉ chạy tay, khi cần.

## Nó làm gì

- Tạo 4 ca: thứ 7 và Chủ nhật, mỗi ngày ca sáng 08:00-11:30 và ca chiều 13:30-17:00. Ca đã có sẵn (ví dụ ca sáng thứ 7
  của dữ liệu mẫu) thì giữ nguyên, ca nào chồng giờ với ca khác của bác sĩ hoặc của phòng thì bỏ qua.
- Chia mỗi ca thành 21 lượt khám (6 lượt/giờ, 10 phút/lượt), giống `DuLieuMau`.
- Tạo 10 hồ sơ bệnh nhân test (CCCD `07999000xxxx`, khách chưa gắn tài khoản) và đặt đủ 10 lịch hẹn `DA_XAC_NHAN` cho mỗi ca
  (tính cả lịch đã có). `so_thu_tu` tính theo phòng và ngày như `DatLichServiceImpl`, mã tra cứu dạng `ECL-<ngày>-9xxx`.
- Chạy lại nhiều lần an toàn: ca đã đủ 10 lịch thì không thêm nữa.

## Chạy

Từ thư mục gốc repo. Container MySQL tên `eclinic-mysql`; mật khẩu và tên DB đọc từ biến môi trường trong container.

```bash
# (trên VM nên dump trước)
docker exec eclinic-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD exec mysqldump -uroot --single-transaction --routines --triggers "$MYSQL_DATABASE"' | gzip > ~/eclinic-truoc-khi-them-ca.sql.gz

docker exec -i eclinic-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD exec mysql -uroot --default-character-set=utf8mb4 -t "$MYSQL_DATABASE"' < scripts/du-lieu-test/them_ca_bac_si_cuoi_tuan.sql
```

Trên VM Google Cloud: SSH vào VM, đưa file lên (`scp` hoặc `git pull`), rồi chạy đúng lệnh trên. MySQL trên VM chỉ mở ở
`127.0.0.1`, nên dùng `docker exec`, không kết nối từ ngoài.

Kết quả in ra 3 bảng; bảng thứ hai phải có 4 ca, mỗi ca `so_lich_hen = 10`.

## Đổi tham số

Sửa các dòng `SET @...` ở đầu file trước khi chạy:

| Biến | Ý nghĩa |
|---|---|
| `@email_bac_si` | Bác sĩ cần thêm ca |
| `@ngay_thu_7`, `@ngay_chu_nhat` | 2 ngày cần thêm ca (định dạng `YYYY-MM-DD`) |
| `@so_lich_moi_ca` | Tổng số lịch hẹn mỗi ca |
| `@so_luot_moi_gio`, `@phut_moi_luot` | N và t, giữ giống `DuLieuMau` (6 và 10) |

## Lưu ý

- Lỗi `Column 'id_bac_si' cannot be null`: không có tài khoản `@email_bac_si` trong DB (thường do `SEED_DATA` tắt).
- Chạy bằng SQL không qua kiểm tra của app, trong đó có giới hạn giờ làm mỗi tuần (`VUOT_GIO_LAM_TUAN`). Tuần đã thêm ca
  có thể báo lỗi 409 khi đổi hoặc nhường ca qua giao diện.
- Lịch hẹn tạo bằng SQL không gửi email hay thông báo.
- Hoàn tác: bỏ comment đoạn "HOÀN TÁC" cuối file rồi chạy riêng. Lịch hẹn và hồ sơ test bị xoá, khung giờ trả về trống,
  ca làm việc giữ nguyên (huỷ ca trong giao diện quản trị).
