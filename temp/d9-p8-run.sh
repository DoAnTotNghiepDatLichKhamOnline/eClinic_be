#!/usr/bin/env bash
# Smoke phase 8: dựng stack eclinic-smoke + mô hình giả (temp/d9-p8-mo-hinh-gia.js, container Node trên cùng mạng), chạy
# 2 chặng của temp/d9-p8-smoke.js rồi chạy lại chặng 1 của smoke phase 7. Chạy từ thư mục gốc repo trên DB mới:
#   bash temp/d9-p8-run.sh
# Mọi biến đặt ở đây đè lên .env: email ở chế độ console, mô hình là mô hình giả, không có gì đi ra ngoài.
set -u
export DB_PORT=3316 REDIS_PORT=6389 MAIL_MODE=console CLOUDINARY_FOLDER=eclinic-smoke BOOKING_IP_LIMIT=200
export CHATBOT_BASE_URL=http://mo-hinh-gia:18999/v1 CHATBOT_API_KEY=khoa-gia-smoke-p7 CHATBOT_MODEL=mo-hinh-gia-1
export CHATBOT_REASONING_EFFORT=low CHATBOT_TIMEOUT=3s CHATBOT_LIMIT_WINDOW=10m
export CHATBOT_LIMIT_MESSAGES=500 CHATBOT_LIMIT_DAILY=1000 CHATBOT_LIMIT_SESSIONS=200 CHATBOT_LIMIT_TOTAL_DAILY=5000
DICH_VU="mysql redis identity-service catalog-service booking-service medical-service notification-service chatbot-service api-gateway"

cho_khoe() {
  for _ in $(seq 1 80); do
    n=$(docker ps --filter name=eclinic- --filter health=healthy --format '{{.Names}}' | wc -l)
    [ "$n" -ge 9 ] && return 0
    sleep 5
  done
  echo "stack không lên đủ 9 container"; docker ps --filter name=eclinic- --format '{{.Names}} {{.Status}}'; return 1
}
# Tạo lại chatbot-service với các biến đang export; xoá các khoá đếm chat:* trên Redis để chặng sau đếm lại từ 0
tao_lai() {
  docker exec eclinic-redis sh -c "redis-cli --scan --pattern 'chat:*' | xargs -r redis-cli del" >/dev/null
  docker compose -p eclinic-smoke up -d --no-deps --force-recreate chatbot-service >/dev/null 2>&1
  cho_khoe
}

docker compose -p eclinic-smoke up -d --build $DICH_VU > temp/d9-p8-up.log 2>&1 || { tail -20 temp/d9-p8-up.log; exit 1; }
MSYS_NO_PATHCONV=1 docker run -d --name eclinic-smoke-mohinh --network eclinic-smoke_default --network-alias mo-hinh-gia \
  -p 127.0.0.1:18999:18999 -v "$(pwd -W 2>/dev/null || pwd)/temp/d9-p8-mo-hinh-gia.js:/gia.js:ro" node:22-alpine node /gia.js >/dev/null
cho_khoe || exit 1
ket_qua=0
node temp/d9-p8-smoke.js 1 || ket_qua=1

export CHATBOT_TOTAL_TIMEOUT=5s
tao_lai && node temp/d9-p8-smoke.js 2 || ket_qua=1

unset CHATBOT_TOTAL_TIMEOUT
tao_lai && node temp/d9-p7-smoke.js 1 || ket_qua=1
exit $ket_qua
