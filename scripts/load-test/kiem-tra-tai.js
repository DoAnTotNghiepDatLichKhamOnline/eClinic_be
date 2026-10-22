// Kiểm tra tải (DOANTOTNGH-15): mô phỏng tới 1.000 người dùng đồng thời trên đường đi chính của hệ thống.
// Chạy k6 trong Docker, cùng mạng với stack eclinic-smoke (không qua cổng của máy, không đụng volume eclinic).
// Từ thư mục gốc, Git Bash:
//   export DB_PORT=3316 REDIS_PORT=6389 PMA_PORT=8091 CLOUDINARY_FOLDER=eclinic-smoke MAIL_MODE=console \
//          SEED_DATA=true BOOKING_IP_LIMIT=1000000 \
//          GATEWAY_RATE_LIMIT=1000000 GATEWAY_AUTH_RATE_LIMIT=1000000 GATEWAY_CHATBOT_RATE_LIMIT=1000000
//   (mọi request của k6 đến từ 1 IP: nâng các giới hạn theo IP, vẫn đếm trên Redis như khi chạy thật)
//   docker compose -p eclinic-smoke up -d --build mysql redis identity-service catalog-service booking-service \
//          notification-service api-gateway
//   MSYS_NO_PATHCONV=1 docker run --rm -i --network eclinic-smoke_default -v "$PWD/scripts/load-test:/scripts" \
//          -e BASE_URL=http://api-gateway:8080 -e NHAN=baseline grafana/k6:1.8.1 run /scripts/kiem-tra-tai.js
// Chạy thử nhanh: thêm -e VUS=10 -e HOLD=30s (bỏ giai đoạn tăng tải).
// Chạy dồn (tìm giới hạn): -e VUS=1000 -e HOLD=2m -e NGHI=0.5 (nghỉ trung bình 0,5 giây thay vì 3-8 giây).
// Kết quả: ket-qua/<NHAN>.json (không commit; tạo thư mục trước: mkdir -p scripts/load-test/ket-qua).
//
// Mỗi người dùng ảo lặp 1 hành trình chọn ngẫu nhiên theo tỉ lệ, nghỉ 3-8 giây giữa các bước như người thật:
//   55% xem bác sĩ, 20% xem khung giờ theo chuyên khoa, 15% bệnh nhân đã đăng nhập xem lịch hẹn,
//   5% đăng nhập, 5% khách đặt lịch (CCCD + số điện thoại ngẫu nhiên). Đặt lịch trả 409 khi khung giờ vừa hết chỗ:
//   tính là kết quả hợp lệ, không phải lỗi.
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://api-gateway:8080';
const NHAN = __ENV.NHAN || 'baseline';
const MAT_KHAU = __ENV.MAT_KHAU || 'Demo@123';
const BENH_NHAN = ['benhnhan01', 'benhnhan02', 'benhnhan03', 'benhnhan04'].map((t) => `${t}@eclinic.local`);
const NHOM = ['chuyen-khoa', 'bac-si', 'bac-si-chi-tiet', 'ngay-con-cho', 'khung-gio', 'ngay-som-nhat', 'khung-gio-gop',
    'quy-tac-dat-lich', 'ngay-nghi', 'lich-hen-cua-toi', 'users-me', 'dang-nhap', 'dat-lich'];

const datLichThanhCong = new Counter('dat_lich_201');
const datLichHetCho = new Counter('dat_lich_409');

const giaiDoan = __ENV.VUS
    ? [{ duration: '10s', target: Number(__ENV.VUS) }, { duration: __ENV.HOLD || '30s', target: Number(__ENV.VUS) },
        { duration: '5s', target: 0 }]
    : [{ duration: '1m', target: 200 }, { duration: '2m', target: 1000 }, { duration: __ENV.HOLD || '5m', target: 1000 },
        { duration: '1m', target: 0 }];

// Ngưỡng chỉ để ghi nhận (không dừng bài test). Các ngưỡng luôn đạt theo từng nhóm chỉ để summary có số liệu của nhóm.
const nguong = { http_req_failed: ['rate<0.01'], http_req_duration: ['p(95)<1000'] };
NHOM.forEach((n) => {
    nguong[`http_req_duration{name:${n}}`] = ['max>=0'];
    nguong[`http_reqs{name:${n}}`] = ['count>=0'];
    nguong[`http_req_failed{name:${n}}`] = ['rate>=0'];
});

export const options = {
    scenarios: { nguoi_dung: { executor: 'ramping-vus', startVUs: 0, stages: giaiDoan, gracefulRampDown: '30s' } },
    thresholds: nguong,
    discardResponseBodies: true,
    summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    setupTimeout: '120s',
};

const TIEU_DE = { 'Accept-Language': 'vi', 'Content-Type': 'application/json' };
const CHAP_NHAN_409 = http.expectedStatuses({ min: 200, max: 299 }, 409);

const NGHI = __ENV.NGHI ? Number(__ENV.NGHI) : 0;

function nghi() {
    sleep(NGHI ? NGHI * (0.5 + Math.random()) : 3 + Math.random() * 5);
}

function ngauNhien(mang) {
    return mang[Math.floor(Math.random() * mang.length)];
}

function chuSo(soLuong) {
    let s = '';
    for (let i = 0; i < soLuong; i++) s += Math.floor(Math.random() * 10);
    return s;
}

function get(duongDan, ten, token, docBody) {
    const tieuDe = token ? { ...TIEU_DE, Authorization: `Bearer ${token}` } : TIEU_DE;
    const res = http.get(`${BASE_URL}${duongDan}`,
        { headers: tieuDe, tags: { name: ten }, responseType: docBody ? 'text' : 'none' });
    check(res, { [`${ten} 2xx`]: (r) => r.status >= 200 && r.status < 300 });
    return res;
}

function duLieu(res) {
    try {
        return res.status === 200 ? res.json('duLieu') : null;
    } catch (e) {
        return null;
    }
}

export function setup() {
    const token = BENH_NHAN.map((email) => {
        const res = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({ email, matKhau: MAT_KHAU }),
            { headers: TIEU_DE, responseType: 'text' });
        if (res.status !== 200) throw new Error(`Đăng nhập ${email} lỗi ${res.status}: ${res.body}`);
        return res.json('duLieu.accessToken');
    });
    const chuyenKhoa = duLieu(get('/api/catalog/chuyen-khoa?kichThuoc=100', 'setup', null, true)).noiDung.map((c) => c.id);
    const bacSi = duLieu(get('/api/catalog/bac-si?kichThuoc=100', 'setup', null, true)).noiDung
        .map((b) => ({ id: b.id, idChuyenKhoa: b.idChuyenKhoa }));
    if (!chuyenKhoa.length || !bacSi.length) throw new Error('Chưa có dữ liệu mẫu (SEED_DATA=true?)');
    return { token, chuyenKhoa, bacSi };
}

// Các ngày còn chỗ của 1 bác sĩ (hoặc chuyên khoa), dạng yyyy-MM-dd
function ngayConCho(thamSo) {
    const ds = duLieu(get(`/api/booking/khung-gio/ngay-con-cho?${thamSo}`, 'ngay-con-cho', null, true)) || [];
    return ds.filter((n) => n.soChoConLai > 0).map((n) => n.ngay);
}

function xemBacSi(dl) {
    get('/api/catalog/chuyen-khoa', 'chuyen-khoa');
    nghi();
    const idChuyenKhoa = ngauNhien(dl.chuyenKhoa);
    get(`/api/catalog/bac-si?idChuyenKhoa=${idChuyenKhoa}`, 'bac-si');
    nghi();
    const cungKhoa = dl.bacSi.filter((b) => b.idChuyenKhoa === idChuyenKhoa);
    const bacSi = ngauNhien(cungKhoa.length ? cungKhoa : dl.bacSi);
    get(`/api/catalog/bac-si/${bacSi.id}`, 'bac-si-chi-tiet');
    nghi();
    const ngay = ngayConCho(`idBacSi=${bacSi.id}`);
    nghi();
    if (ngay.length) get(`/api/booking/khung-gio?ngay=${ngauNhien(ngay)}&idBacSi=${bacSi.id}`, 'khung-gio');
}

function xemTheoChuyenKhoa(dl) {
    const idChuyenKhoa = ngauNhien(dl.chuyenKhoa);
    const somNhat = duLieu(get(`/api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa=${idChuyenKhoa}`, 'ngay-som-nhat', null,
        true)) || [];
    nghi();
    if (somNhat.length) {
        get(`/api/booking/khung-gio/gop?idChuyenKhoa=${idChuyenKhoa}&ngay=${ngauNhien(somNhat).ngay}`, 'khung-gio-gop');
        nghi();
    }
    get('/api/booking/quy-tac-dat-lich', 'quy-tac-dat-lich');
    nghi();
    get('/api/booking/ngay-nghi', 'ngay-nghi');
}

function benhNhanDaDangNhap(dl) {
    const token = ngauNhien(dl.token);
    get('/api/booking/lich-hen/cua-toi', 'lich-hen-cua-toi', token);
    nghi();
    get('/api/users/me', 'users-me', token);
}

function dangNhap() {
    const res = http.post(`${BASE_URL}/api/auth/login`,
        JSON.stringify({ email: ngauNhien(BENH_NHAN), matKhau: MAT_KHAU }),
        { headers: TIEU_DE, tags: { name: 'dang-nhap' } });
    check(res, { 'dang-nhap 200': (r) => r.status === 200 });
}

function khachDatLich(dl) {
    const bacSi = ngauNhien(dl.bacSi);
    const ngay = ngayConCho(`idBacSi=${bacSi.id}`);
    if (!ngay.length) return;
    nghi();
    const ca = duLieu(get(`/api/booking/khung-gio?ngay=${ngauNhien(ngay)}&idBacSi=${bacSi.id}`, 'khung-gio', null,
        true)) || [];
    const lua = [];
    ca.forEach((c) => c.khungGio.filter((k) => !k.hetCho)
        .forEach((k) => lua.push({ idLichLamViec: c.idLichLamViec, gioBatDauKhung: k.gioBatDau })));
    if (!lua.length) return;
    nghi();
    const khung = ngauNhien(lua);
    const yeuCau = {
        ...khung,
        benhNhan: {
            hoTen: `Kiểm Tra Tải ${chuSo(4)}`,
            ngaySinh: '1990-01-01',
            gioiTinh: ngauNhien(['NAM', 'NU']),
            soDienThoai: `08${chuSo(8)}`,
            cccd: `099${chuSo(9)}`,
        },
        lyDoKham: 'Khám tổng quát (kiểm tra tải)',
    };
    const res = http.post(`${BASE_URL}/api/booking/lich-hen`, JSON.stringify(yeuCau),
        { headers: TIEU_DE, tags: { name: 'dat-lich' }, responseCallback: CHAP_NHAN_409 });
    check(res, { 'dat-lich 201/409': (r) => r.status === 201 || r.status === 409 });
    if (res.status === 201) datLichThanhCong.add(1);
    if (res.status === 409) datLichHetCho.add(1);
}

export default function (dl) {
    const r = Math.random();
    if (r < 0.55) xemBacSi(dl);
    else if (r < 0.75) xemTheoChuyenKhoa(dl);
    else if (r < 0.90) benhNhanDaDangNhap(dl);
    else if (r < 0.95) dangNhap();
    else khachDatLich(dl);
    nghi();
}

function so(giaTri, donVi) {
    return giaTri === undefined ? '-' : `${giaTri.toFixed(donVi === '%' ? 2 : 0)}${donVi}`;
}

// In bảng tóm tắt gọn (p95 theo từng nhóm) và ghi toàn bộ số liệu ra ket-qua/<NHAN>.json
export function handleSummary(data) {
    const m = data.metrics;
    const thoiGian = (k) => (m[k] ? m[k].values : {});
    const tong = thoiGian('http_req_duration');
    const dong = [
        `\n=== Kiểm tra tải: ${NHAN} ===`,
        `Request: ${m.http_reqs.values.count} (${so(m.http_reqs.values.rate, '')}/s), VU tối đa: ${m.vus_max.values.max}`,
        `Lỗi ngoài dự kiến: ${so(m.http_req_failed.values.rate * 100, '%')} (${m.http_req_failed.values.passes})`,
        `Thời gian phản hồi: p50 ${so(tong.med, 'ms')}, p95 ${so(tong['p(95)'], 'ms')}, p99 ${so(tong['p(99)'], 'ms')}, `
            + `max ${so(tong.max, 'ms')}`,
        `Đặt lịch: 201 = ${m.dat_lich_201 ? m.dat_lich_201.values.count : 0}, `
            + `409 = ${m.dat_lich_409 ? m.dat_lich_409.values.count : 0}`,
        `Check đạt: ${so(m.checks.values.rate * 100, '%')}`,
        '',
        'Nhóm                 số req   lỗi      p95       p99       max',
    ];
    NHOM.forEach((n) => {
        const k = `http_req_duration{name:${n}}`;
        const v = thoiGian(k);
        const soReq = thoiGian(`http_reqs{name:${n}}`).count || 0;
        if (!soReq) return dong.push(`${n.padEnd(20)} (không có request)`);
        const loi = thoiGian(`http_req_failed{name:${n}}`).rate * 100;
        dong.push(`${n.padEnd(20)} ${String(soReq).padStart(6)}   ${so(loi, '%').padEnd(8)} ${so(v['p(95)'], 'ms').padEnd(9)} `
            + `${so(v['p(99)'], 'ms').padEnd(9)} ${so(v.max, 'ms')}`);
    });
    dong.push('', `Ngưỡng: lỗi < 1% ${m.http_req_failed.thresholds['rate<0.01'].ok ? 'ĐẠT' : 'KHÔNG ĐẠT'}, `
        + `p95 < 1000ms ${m.http_req_duration.thresholds['p(95)<1000'].ok ? 'ĐẠT' : 'KHÔNG ĐẠT'}`, '');
    return { stdout: dong.join('\n'), [`/scripts/ket-qua/${NHAN}.json`]: JSON.stringify(data, null, 2) };
}
