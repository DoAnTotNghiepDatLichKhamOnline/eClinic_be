// Đo tốc độ của máy chủ thật từ máy của mình (DOANTOTNGH-25): mỗi API công khai đo nhiều lần, tách thời gian mạng và
// thời gian xử lý của hệ thống (header Server-Timing "gw;dur" do api-gateway ghi). Không cần cài gì ngoài Node.
// Từ thư mục gốc:
//   node scripts/load-test/kiem-tra-prod.js https://<frontend>.vercel.app,https://eclinic-api.duckdns.org
//   node scripts/load-test/kiem-tra-prod.js https://localhost:8443 --insecure      (stack giống máy thật, chứng chỉ tự cấp)
// Biến môi trường: BASE_URL (thay cho tham số), SO_LAN (số lần đo mỗi kiểu, mặc định 10).
//
// Mỗi API đo 2 kiểu:
//   lạnh: mỗi lần 1 kết nối mới (như lần đầu mở trang): DNS, TCP, TLS, TTFB (tới byte đầu tiên), tổng;
//   nóng: dùng lại kết nối keep-alive (như các lần bấm tiếp theo): TTFB, tổng.
// Cách đọc: "gw" là thời gian của gateway + service; "mạng+proxy" = TTFB - gw là mạng, Vercel, Caddy.
// Từ Việt Nam, TTFB nóng dưới khoảng 100 ms là nhanh. gw nhỏ mà TTFB lớn: chậm ở mạng / Vercel / Caddy, không phải ở code.
const http = require('http');
const https = require('https');

const thamSo = process.argv.slice(2);
const BO_QUA_CHUNG_CHI = thamSo.includes('--insecure');
const DANH_SACH_GOC = (thamSo.find((t) => !t.startsWith('--')) || process.env.BASE_URL || 'http://127.0.0.1:8080')
    .split(',').map((s) => s.trim().replace(/\/$/, '')).filter(Boolean);
const SO_LAN = Number(process.env.SO_LAN || 10);
const THOI_GIAN_CHO = 15000;

function goi(url, agent) {
    return new Promise((resolve) => {
        const u = new URL(url);
        const thuVien = u.protocol === 'https:' ? https : http;
        const t0 = process.hrtime.bigint();
        const ms = () => Number(process.hrtime.bigint() - t0) / 1e6;
        const moc = {};
        const req = thuVien.request(u, {
            agent, family: 4, rejectUnauthorized: !BO_QUA_CHUNG_CHI,
            headers: { 'Accept-Encoding': 'gzip', 'Accept-Language': 'vi' },
        }, (res) => {
            moc.ttfb = ms();
            let kichThuoc = 0;
            const phan = [];
            res.on('data', (c) => { kichThuoc += c.length; phan.push(c); });
            res.on('end', () => {
                const gw = /gw;dur=([\d.]+)/.exec(res.headers['server-timing'] || '');
                resolve({ ...moc, tong: ms(), ma: res.statusCode, gw: gw ? Number(gw[1]) : null, kichThuoc,
                    gzip: res.headers['content-encoding'] === 'gzip', body: Buffer.concat(phan) });
            });
        });
        req.on('socket', (s) => {
            s.on('lookup', () => { moc.dns = ms(); });
            s.on('connect', () => { moc.tcp = ms(); });
            s.on('secureConnect', () => { moc.tls = ms(); });
        });
        req.setTimeout(THOI_GIAN_CHO, () => req.destroy(new Error(`quá ${THOI_GIAN_CHO / 1000} giây`)));
        req.on('error', (e) => resolve({ loi: e.message }));
        req.end();
    });
}

function phanVi(mang, p) {
    const so = mang.filter((x) => x != null).sort((a, b) => a - b);
    if (!so.length) return null;
    return so[Math.min(so.length - 1, Math.floor(p * so.length))];
}

const f = (x) => (x == null ? '-' : x.toFixed(0)).padStart(6);

async function idBacSiDauTien(goc) {
    const kq = await goi(`${goc}/api/catalog/bac-si?kichThuoc=1`, false);
    if (kq.loi || kq.ma !== 200) return null;
    try {
        const zlib = require('zlib');
        const body = kq.gzip ? zlib.gunzipSync(kq.body) : kq.body;
        return JSON.parse(body.toString()).duLieu.noiDung[0].id;
    } catch (e) {
        return null;
    }
}

async function doMotGoc(goc) {
    console.log(`\n=== ${goc}  (${SO_LAN} lần mỗi kiểu, trung vị / p90, ms)`);
    const id = await idBacSiDauTien(goc);
    const duongDan = ['/api/catalog/chuyen-khoa', '/api/catalog/bac-si', id ? `/api/catalog/bac-si/${id}` : null,
        '/api/booking/quy-tac-dat-lich', '/api/booking/ngay-nghi'].filter(Boolean);
    console.log('API                              kiểu   mã   DNS   TCP   TLS  TTFB  p90TTFB  tổng    gw  mạng+proxy  byte gzip');
    let coLoi = false;
    for (const p of duongDan) {
        const nong = new (goc.startsWith('https') ? https : http).Agent({ keepAlive: true, maxSockets: 1 });
        for (const [kieu, agent] of [['lạnh', false], ['nóng', nong]]) {
            const kq = [];
            if (kieu === 'nóng') await goi(`${goc}${p}`, agent); // mở kết nối trước, không tính
            for (let i = 0; i < SO_LAN; i++) kq.push(await goi(`${goc}${p}`, agent));
            const loi = kq.find((k) => k.loi);
            if (loi) {
                coLoi = true;
                console.log(`${p.padEnd(32)} ${kieu}  LỖI: ${loi.loi}`);
                if (kq.every((k) => k.loi)) break;
                continue;
            }
            const lay = (k) => kq.map((x) => x[k]);
            const ttfb = phanVi(lay('ttfb'), 0.5);
            const gw = phanVi(lay('gw'), 0.5);
            const ma = [...new Set(lay('ma'))].join('/');
            console.log(`${p.padEnd(32)} ${kieu} ${String(ma).padStart(4)}${f(phanVi(lay('dns'), 0.5))}${f(phanVi(lay('tcp'), 0.5))}`
                + `${f(phanVi(lay('tls'), 0.5))}${f(ttfb)}${f(phanVi(lay('ttfb'), 0.9)).padStart(9)}${f(phanVi(lay('tong'), 0.5))}`
                + `${f(gw)}${f(gw == null ? null : ttfb - gw).padStart(12)}${String(kq[0].kichThuoc).padStart(6)} ${kq[0].gzip ? 'có' : 'không'}`);
        }
        nong.destroy();
    }
    return coLoi;
}

(async () => {
    let coLoi = false;
    for (const goc of DANH_SACH_GOC) coLoi = (await doMotGoc(goc)) || coLoi;
    process.exitCode = coLoi ? 1 : 0;
})();
