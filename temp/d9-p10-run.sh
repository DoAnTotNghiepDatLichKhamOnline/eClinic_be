#!/usr/bin/env bash
# Smoke phase 10: hồi quy toàn bộ task trên các stack eclinic-smoke MỚI (mỗi chặng 1 database mới), rồi chuỗi lời gọi
# của khung chat. Chạy từ thư mục gốc repo:  bash temp/d9-p10-run.sh   (khoảng 40 phút)
#   A. temp/d9-p9-run.sh  (phase 9 + chặng 1 của phase 8, 7)  rồi  temp/d9-p10-smoke.js trên cùng stack
#   B. temp/d9-p1-smoke.js    C. temp/d9-p2-smoke.js    D. temp/d9-p3-smoke.js    E. temp/d9-p6-run.sh (phase 4 đến 6)
# Kết quả từng chặng ghi vào temp/d9-p10-<chặng>.log; dòng cuối in bảng tổng. Mọi biến đặt ở đây đè lên .env: email ở
# chế độ console / Mailpit, mô hình giả, thư mục Cloudinary riêng.
set -u
ket_qua=0
tong=""

don() {
  docker rm -f eclinic-smoke-mohinh eclinic-smoke-mailpit >/dev/null 2>&1
  docker compose -p eclinic-smoke --profile app down -v >/dev/null 2>&1
}
ghi() {  # ghi <tên chặng> <mã thoát> <file log>
  local dong
  dong=$(grep -E "đạt, [0-9]+ hỏng" "$3" | tr '\n' ';')
  tong="$tong\n$1: exit=$2 | $dong"
  [ "$2" -eq 0 ] || ket_qua=1
}
cho_khoe_8() {
  for _ in $(seq 1 80); do
    n=$(docker ps --filter name=eclinic- --filter health=healthy --format '{{.Names}}' | wc -l)
    [ "$n" -ge 8 ] && return 0
    sleep 5
  done
  echo "stack không lên đủ 8 container"; docker ps --filter name=eclinic- --format '{{.Names}} {{.Status}}'; return 1
}
# Stack thường (không chatbot, không Mailpit) trên database mới rồi chạy 1 file smoke
chang_don() {  # chang_don <tên> <file smoke>
  don
  (
    export DB_PORT=3316 REDIS_PORT=6389 MAIL_MODE=console CLOUDINARY_FOLDER=eclinic-smoke BOOKING_IP_LIMIT=200
    docker compose -p eclinic-smoke up -d --build mysql redis identity-service catalog-service booking-service medical-service \
      notification-service api-gateway > "temp/d9-p10-$1-up.log" 2>&1 || { tail -20 "temp/d9-p10-$1-up.log"; exit 1; }
    cho_khoe_8 || exit 1
    node "$2"
  ) > "temp/d9-p10-$1.log" 2>&1
  ghi "$1" $? "temp/d9-p10-$1.log"
}

# A. Chatbot: phase 9 (kèm 8, 7) rồi khung chat của phase 10 trên cùng stack
don
bash temp/d9-p9-run.sh > temp/d9-p10-p9.log 2>&1
ghi "p9 (+p8, p7)" $? temp/d9-p10-p9.log
docker exec eclinic-redis sh -c "redis-cli --scan --pattern 'chat:*' | xargs -r redis-cli del" >/dev/null 2>&1
node temp/d9-p10-smoke.js > temp/d9-p10-chat.log 2>&1
ghi "p10 khung chat" $? temp/d9-p10-chat.log

chang_don p1 temp/d9-p1-smoke.js
chang_don p2 temp/d9-p2-smoke.js
chang_don p3 temp/d9-p3-smoke.js

# E. Email đặt lịch, nhắc lịch, nhắc tái khám (script tự dựng stack + Mailpit)
don
bash temp/d9-p6-run.sh > temp/d9-p10-p6.log 2>&1
ghi "p6 (p4 den p6)" $? temp/d9-p10-p6.log

don
echo -e "$tong"
echo "KET QUA CHUNG: $ket_qua"
exit $ket_qua
