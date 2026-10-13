#!/usr/bin/env bash
# Smoke phase 6: dựng stack eclinic-smoke + Mailpit, chạy 3 chặng của temp/d9-p6-smoke.js, giữa các chặng tạo lại
# booking-service với cấu hình khác. Chạy từ thư mục gốc repo:  bash temp/d9-p6-run.sh
# Mọi biến đặt ở đây đè lên .env: identity ở chế độ console, notification-service gửi tới Mailpit, không có thư thật.
set -u
export DB_PORT=3316 REDIS_PORT=6389 MAIL_MODE=console CLOUDINARY_FOLDER=eclinic-smoke
export NOTIFICATION_MAIL_MODE=smtp MAIL_HOST=mailpit MAIL_PORT=1025 MAIL_SMTP_AUTH=false MAIL_SMTP_STARTTLS=false
export MAIL_USERNAME=smoke@eclinic.local MAIL_APP_PASSWORD=dummy NOTIFICATION_MAIL_INTERVAL=2s
export BOOKING_IP_LIMIT=200 BOOKING_REMINDER_INTERVAL=3s BOOKING_REMINDER_MIN_LEAD=3d BOOKING_REMINDER_BEFORE=20d,10d
export FOLLOWUP_REMINDER_DAYS=7
DICH_VU="mysql redis identity-service catalog-service booking-service medical-service notification-service api-gateway"

cho_khoe() {
  for _ in $(seq 1 80); do
    n=$(docker ps --filter name=eclinic- --filter health=healthy --format '{{.Names}}' | grep -vc mailpit)
    [ "$n" -ge 8 ] && return 0
    sleep 5
  done
  echo "stack không lên đủ 8 container"; docker ps --filter name=eclinic- --format '{{.Names}} {{.Status}}'; return 1
}
tao_lai_booking() {
  docker compose -p eclinic-smoke up -d --no-deps --force-recreate booking-service >/dev/null 2>&1
  cho_khoe
}

# "Ban đêm": khoảng giờ gửi không chứa giờ hiện tại
if [ "$(date +%H)" -lt 12 ]; then export BOOKING_REMINDER_FROM=13:00 BOOKING_REMINDER_TO=23:00; else export BOOKING_REMINDER_FROM=01:00 BOOKING_REMINDER_TO=11:00; fi
docker compose -p eclinic-smoke up -d --build $DICH_VU > temp/d9-p6-up.log 2>&1 || { tail -20 temp/d9-p6-up.log; exit 1; }
docker run -d --name eclinic-smoke-mailpit --network eclinic-smoke_default --network-alias mailpit -p 127.0.0.1:18025:8025 axllent/mailpit:latest >/dev/null
cho_khoe || exit 1
ket_qua=0
node temp/d9-p6-smoke.js 1 || ket_qua=1

export BOOKING_REMINDER_FROM=00:00 BOOKING_REMINDER_TO=23:59
tao_lai_booking && node temp/d9-p6-smoke.js 2 || ket_qua=1

export FOLLOWUP_REMINDER_DAYS=12
tao_lai_booking && node temp/d9-p6-smoke.js 3 || ket_qua=1
exit $ket_qua
