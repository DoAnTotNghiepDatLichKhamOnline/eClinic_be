// Trang demo xác thực và tài khoản — CHỈ DÙNG KHI DEV, thay frontend để thử toàn bộ luồng qua api-gateway:
// đăng ký, liên kết kích hoạt / đặt lại mật khẩu / xác nhận đổi email trong email, đăng nhập (mật khẩu, Google),
// cookie refresh token, đổi email, hồ sơ cá nhân, ảnh đại diện, đổi mật khẩu, thiết bị đăng nhập, quản trị tài khoản.
//
// Chạy (cần gateway + identity-service đang chạy):  node scripts/demo-xac-thuc/server.js
// rồi mở http://localhost:5173 bằng Chrome/Edge/Firefox (Safari không gửi cookie Secure qua http://localhost).
//
// Cổng 5173 trùng FRONTEND_URL mặc định nên liên kết trong email mở đúng trang này, và đã nằm trong CORS_ALLOWED_ORIGINS.
// GOOGLE_CLIENT_ID lấy từ biến môi trường; không có thì chỉ đọc ĐÚNG dòng GOOGLE_CLIENT_ID trong .env ở thư mục gốc
// (không đọc / in các biến khác). Client ID phải cho phép origin http://localhost:5173 (xem README).
//
// Server này cũng phục vụ trang demo đặt lịch khám (scripts/demo-dat-lich/index.html, cần thêm catalog-service và
// booking-service): /dat-lich và /phieu-kham/<mã phiếu khám>. Phải chung cổng 5173 vì link phiếu khám trong mã QR là
// FRONTEND_URL/phieu-kham/<mã>, và cookie refresh token dùng chung nên đăng nhập ở trang này thì trang kia cũng có phiên.
//
// Và trang demo lịch hẹn (scripts/demo-lich-hen/index.html, cần thêm medical-service và internet cho thư viện lịch):
// /lich-hen. Bệnh nhân: lịch hẹn dạng lịch, trang cá nhân, lịch sử khám. Bác sĩ: lịch làm việc, danh sách bệnh nhân, hồ sơ
// khám, quét QR, ghi kết quả khám.
const http = require('http');
const fs = require('fs');
const path = require('path');

const CONG = Number(process.env.PORT || 5173);
const API_URL = process.env.API_URL || 'http://localhost:8080';
const CAC_TRANG = new Set(['/', '/verify-email', '/reset-password', '/confirm-email-change']);
// Trang đặt lịch: /dat-lich, và /phieu-kham/<đúng 1 đoạn> (mã phiếu khám do trang tự đọc từ URL)
const TRANG_DAT_LICH = /^\/(dat-lich|phieu-kham\/[^/]+)$/;

function layGoogleClientId() {
    if (process.env.GOOGLE_CLIENT_ID) {
        return process.env.GOOGLE_CLIENT_ID.trim();
    }
    try {
        const env = fs.readFileSync(path.join(__dirname, '..', '..', '.env'), 'utf8');
        const dong = env.match(/^\s*GOOGLE_CLIENT_ID\s*=\s*(.*)$/m);
        return dong ? dong[1].trim().replace(/^["']|["']$/g, '') : '';
    } catch {
        return '';
    }
}

// Giá trị chèn vào <script>: JSON + thoát "<" để không đóng thẻ script
const choVaoScript = (giaTri) => JSON.stringify(giaTri).replace(/</g, '\\u003c');

const googleClientId = layGoogleClientId();
const html = fs.readFileSync(path.join(__dirname, 'index.html'), 'utf8')
    .replace('"__API_URL__"', choVaoScript(API_URL))
    .replace('"__GOOGLE_CLIENT_ID__"', choVaoScript(googleClientId));
const htmlDatLich = fs.readFileSync(path.join(__dirname, '..', 'demo-dat-lich', 'index.html'), 'utf8')
    .replace('"__API_URL__"', choVaoScript(API_URL));
const htmlLichHen = fs.readFileSync(path.join(__dirname, '..', 'demo-lich-hen', 'index.html'), 'utf8')
    .replace('"__API_URL__"', choVaoScript(API_URL));

http.createServer((req, res) => {
    const duongDan = new URL(req.url, 'http://localhost').pathname;
    const trang = CAC_TRANG.has(duongDan) ? html
        : TRANG_DAT_LICH.test(duongDan) ? htmlDatLich
        : duongDan === '/lich-hen' ? htmlLichHen
        : null;
    if (req.method !== 'GET' || !trang) {
        res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end('Không tìm thấy');
        return;
    }
    res.writeHead(200, {
        'Content-Type': 'text/html; charset=utf-8',
        // Token (đặt lại mật khẩu, đổi email...) và mã phiếu khám nằm trên URL: không gửi qua header Referer, không cache trang
        'Referrer-Policy': 'no-referrer',
        'Cache-Control': 'no-store',
    });
    res.end(trang);
}).listen(CONG, () => {
    console.log(`Trang demo xác thực: http://localhost:${CONG}  (API: ${API_URL}, Google: `
        + `${googleClientId ? 'đã cấu hình' : 'chưa có GOOGLE_CLIENT_ID'})`);
    console.log(`Trang demo đặt lịch: http://localhost:${CONG}/dat-lich`);
    console.log(`Trang demo lịch hẹn: http://localhost:${CONG}/lich-hen`);
});
