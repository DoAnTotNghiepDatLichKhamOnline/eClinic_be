// Smoke phase 10: chạy scripts/demo/server.js ở cổng riêng (15173), gọi mọi đường dẫn trong CAC_DUONG_DAN, 2 chuyển
// hướng, 1 trang phiếu khám, chung.js / chung.css và từng file màn hình.   node temp/d9-p10-may-chu-demo.js
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const GOC = path.join(__dirname, '..', 'scripts', 'demo');
const CONG = 15173;
const maServer = fs.readFileSync(path.join(GOC, 'server.js'), 'utf8');
const khoi = maServer.slice(maServer.indexOf('const CAC_DUONG_DAN'), maServer.indexOf(']);', maServer.indexOf('const CAC_DUONG_DAN')));
const cacDuongDan = [...khoi.matchAll(/'(\/[^']*)'/g)].map((m) => m[1]);
const cacManHinh = fs.readdirSync(path.join(GOC, 'man-hinh')).filter((t) => t.endsWith('.js')).sort();
let dat = 0, hong = 0;
const kiem = (ten, dung, chiTiet) => { if (dung) dat++; else { hong++; console.log('  HỎNG ' + ten + (chiTiet ? ': ' + chiTiet : '')); } };

const server = spawn(process.execPath, [path.join(GOC, 'server.js')], { env: { ...process.env, PORT: String(CONG), GOOGLE_CLIENT_ID: 'smoke' }, stdio: 'ignore' });
const lay = (duongDan) => fetch(`http://127.0.0.1:${CONG}${duongDan}`, { redirect: 'manual', signal: AbortSignal.timeout(5000) });

(async () => {
  for (let i = 0; i < 30; i++) {
    try { await lay('/_demo/chung.css'); break; } catch { await new Promise((x) => setTimeout(x, 200)); }
  }
  kiem('đọc được CAC_DUONG_DAN từ server.js', cacDuongDan.length >= 20, String(cacDuongDan.length));
  for (const duongDan of [...cacDuongDan, '/phieu-kham/ma-bat-ky']) {
    const res = await lay(duongDan);
    const html = await res.text();
    kiem(duongDan, res.status === 200 && html.includes('id="man-hinh"') && html.includes('id="tieu-de-ket-qua"') && html.includes('id="ghi-chu-trang"')
      && html.includes('/_demo/chung.js') && cacManHinh.every((t) => html.includes(`/_demo/man-hinh/${t}`)) && !html.includes('__MAN_HINH__')
      && !html.includes('__API_URL__'), `status ${res.status}`);
  }
  for (const [cu, moi] of [['/dat-lich', '/appointment'], ['/lich-hen', '/patient/appointments']]) {
    const res = await lay(cu);
    kiem(`${cu} -> 302 ${moi}`, res.status === 302 && res.headers.get('location') === moi, `${res.status} ${res.headers.get('location')}`);
  }
  for (const tep of ['chung.js', 'chung.css', ...cacManHinh.map((t) => 'man-hinh/' + t)]) {
    const res = await lay('/_demo/' + tep);
    const noiDung = await res.text();
    kiem('/_demo/' + tep, res.status === 200 && noiDung === fs.readFileSync(path.join(GOC, tep), 'utf8'), `status ${res.status}`);
  }
  const la = await lay('/khong-co-trang-nay');
  kiem('đường dẫn lạ không trả 200', la.status !== 200, String(la.status));
  console.log(`${cacDuongDan.length + 1} trang, 2 chuyển hướng, ${cacManHinh.length + 2} file tĩnh: ${dat} đạt, ${hong} hỏng`);
  server.kill();
  process.exit(hong ? 1 : 0);
})().catch((e) => { console.error(e); server.kill(); process.exit(1); });
