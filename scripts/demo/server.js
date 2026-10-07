// Trang demo eClinic — CHỈ DÙNG KHI DEV, thay frontend để thử các luồng qua api-gateway. Cấu trúc theo đúng danh sách màn
// hình của frontend ("Danh sách UI hiện đang có 05_10_2026.docx"): cùng cổng, cùng đường dẫn, cùng thanh bên của từng vai trò.
//
// Chạy (cần gateway và các service đang chạy):  node scripts/demo/server.js
// rồi mở http://localhost:5173 bằng Chrome/Edge/Firefox (Safari không gửi cookie Secure qua http://localhost).
//
// Cổng 5173 trùng FRONTEND_URL mặc định nên liên kết trong email (/verify-email, /reset-password, /confirm-email-change) và
// link phiếu khám trong mã QR (/phieu-kham/<mã>) mở đúng trang này; cổng này cũng đã nằm trong CORS_ALLOWED_ORIGINS.
// GOOGLE_CLIENT_ID lấy từ biến môi trường; không có thì chỉ đọc ĐÚNG dòng GOOGLE_CLIENT_ID trong .env ở thư mục gốc
// (không đọc / in các biến khác). Client ID phải cho phép origin http://localhost:5173 (xem README).
//
// Bố cục thư mục:
//   index.html          khung trang (đầu trang, thanh bên, vùng màn hình, ô "Kết quả gọi API gần nhất")
//   chung.css, chung.js kiểu dáng và hàm dùng chung (gọi API, phiên đăng nhập, bộ định tuyến)
//   man-hinh/<tên>.js   mỗi màn hình của UI một file; file tự đăng ký đường dẫn của mình bằng dangKy({...})
// Mọi đường dẫn màn hình đều trả về cùng một khung trang; chung.js đọc location.pathname để chọn màn hình.
// Thêm màn hình mới: tạo file trong man-hinh/ (server tự chèn thẻ <script>) và thêm đường dẫn vào CAC_DUONG_DAN dưới đây.
const http = require('http');
const fs = require('fs');
const path = require('path');

const CONG = Number(process.env.PORT || 5173);
const API_URL = process.env.API_URL || 'http://localhost:8080';

// Đường dẫn của các màn hình (trùng với UI). Phải khớp với dangKy() trong man-hinh/*.js
const CAC_DUONG_DAN = new Set([
    '/',
    '/login', '/register', '/forgot-password', '/reset-password', '/verify-email', '/confirm-email-change',
    '/appointment', '/patient/appointments', '/patient/profile', '/account',
    '/doctor/dashboard', '/doctor/work-schedule', '/doctor/appointment-requests', '/doctor/consultation', '/doctor/profile',
    '/admin/dashboard', '/admin/schedule-management', '/admin/medical-catalog', '/admin/doctors', '/admin/patients',
    '/admin/accounts',
]);
// /phieu-kham/<đúng 1 đoạn>: mã phiếu khám do màn hình tự đọc từ URL
const PHIEU_KHAM = /^\/phieu-kham\/[^/]+$/;
// Đường dẫn của 3 trang demo cũ
const CHUYEN_HUONG = new Map([['/dat-lich', '/appointment'], ['/lich-hen', '/patient/appointments']]);

// Tệp tĩnh: danh sách cố định đọc từ thư mục lúc khởi động. Không ghép đường dẫn từ URL nên không đọc được tệp nào khác
const KIEU = { '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8' };
const cacManHinh = fs.readdirSync(path.join(__dirname, 'man-hinh')).filter((ten) => ten.endsWith('.js')).sort();
const TEP_TINH = new Map([
    ['/_demo/chung.css', path.join(__dirname, 'chung.css')],
    ['/_demo/chung.js', path.join(__dirname, 'chung.js')],
    ...cacManHinh.map((ten) => [`/_demo/man-hinh/${ten}`, path.join(__dirname, 'man-hinh', ten)]),
]);

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

// Đọc lại mỗi lần gọi: sửa file xong chỉ cần tải lại trang (thêm file màn hình mới thì phải chạy lại server)
function khungTrang() {
    return fs.readFileSync(path.join(__dirname, 'index.html'), 'utf8')
        .replace('"__API_URL__"', () => choVaoScript(API_URL))
        .replace('"__GOOGLE_CLIENT_ID__"', () => choVaoScript(googleClientId))
        .replace('<!--__MAN_HINH__-->', () => cacManHinh.map((ten) => `<script src="/_demo/man-hinh/${ten}"></script>`).join('\n'));
}

const DAU_CHUNG = {
    // Token (đặt lại mật khẩu, đổi email...) và mã phiếu khám nằm trên URL: không gửi qua header Referer, không cache trang
    'Referrer-Policy': 'no-referrer',
    'Cache-Control': 'no-store',
};

http.createServer((req, res) => {
    const duongDan = new URL(req.url, 'http://localhost').pathname;
    const traLoi = (ma, dau, noiDung) => {
        res.writeHead(ma, { ...DAU_CHUNG, ...dau });
        res.end(noiDung);
    };
    if (req.method !== 'GET') {
        traLoi(404, { 'Content-Type': 'text/plain; charset=utf-8' }, 'Không tìm thấy');
    } else if (CHUYEN_HUONG.has(duongDan)) {
        traLoi(302, { Location: CHUYEN_HUONG.get(duongDan) }, '');
    } else if (TEP_TINH.has(duongDan)) {
        const tep = TEP_TINH.get(duongDan);
        traLoi(200, { 'Content-Type': KIEU[path.extname(tep)] }, fs.readFileSync(tep));
    } else if (CAC_DUONG_DAN.has(duongDan) || PHIEU_KHAM.test(duongDan)) {
        traLoi(200, { 'Content-Type': 'text/html; charset=utf-8' }, khungTrang());
    } else {
        traLoi(404, { 'Content-Type': 'text/plain; charset=utf-8' }, 'Không tìm thấy');
    }
}).listen(CONG, () => {
    console.log(`Trang demo eClinic: http://localhost:${CONG}  (API: ${API_URL}, Google: `
        + `${googleClientId ? 'đã cấu hình' : 'chưa có GOOGLE_CLIENT_ID'}, ${cacManHinh.length} màn hình)`);
    console.log(`Bệnh nhân / khách: http://localhost:${CONG}/appointment`);
    console.log(`Bác sĩ:            http://localhost:${CONG}/doctor/dashboard`);
    console.log(`Quản trị viên:     http://localhost:${CONG}/admin/dashboard`);
});
