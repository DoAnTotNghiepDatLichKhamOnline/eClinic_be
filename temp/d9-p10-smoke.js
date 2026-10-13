// Smoke phase 10 (DOANTOTNGH-9): đúng chuỗi lời gọi API mà khung chat của trang demo (scripts/demo/chung.js) thực hiện,
// qua gateway của stack eclinic-smoke với mô hình GIẢ (temp/d9-p9-mo-hinh-gia.js). Chạy sau temp/d9-p9-run.sh trên cùng
// stack (temp/d9-p10-run.sh làm việc đó):  node temp/d9-p10-smoke.js
// Không mở trình duyệt: phần vẽ thẻ và form đặt lịch chọn sẵn nằm ở file kiểm tra trên màn hình.
const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const GOC = 'http://127.0.0.1:8080';
let dat = 0, hong = 0;
function kiem(ten, dung, chiTiet) {
  if (dung) { dat++; console.log('  ok   ' + ten); } else { hong++; console.log('  HỎNG ' + ten + (chiTiet ? '\n       ' + String(chiTiet).slice(0, 1500) : '')); }
}
async function goi(phuongThuc, duongDan, { body, token, lang = 'en', khach } = {}) {
  const headers = { 'Accept-Language': lang };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = 'Bearer ' + token;
  if (khach) headers['X-Chat-Khach'] = khach;
  const res = await fetch(GOC + duongDan, { method: phuongThuc, headers, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(40000) });
  let json = null; try { json = await res.json(); } catch { /* không phải JSON */ }
  return { status: res.status, json: json || {} };
}
const sh = (...thamSo) => execFileSync('docker', thamSo, { encoding: 'utf8' }).trim();
const J = (x) => JSON.stringify(x);
const ngaySau = (n) => { const d = new Date(Date.now() + n * 86400000); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; };
const CO_DAU = /[À-ÖØ-öø-ỹ]/;

// Hàm dựng đường dẫn đặt lịch THẬT của khung chat, lấy thẳng từ chung.js (không chép lại)
function hamCuaDemo() {
  const ma = fs.readFileSync(path.join(__dirname, '..', 'scripts', 'demo', 'chung.js'), 'utf8');
  const dau = ma.indexOf('function duongDanDatLich(');
  const cuoi = ma.indexOf('\nfunction theChat(');
  if (dau < 0 || cuoi < 0) throw new Error('không thấy duongDanDatLich trong chung.js');
  const nguCanh = { URLSearchParams };
  vm.createContext(nguCanh);
  vm.runInContext(ma.slice(dau, cuoi) + '\n;globalThis.duongDanDatLich = duongDanDatLich;', nguCanh);
  return nguCanh.duongDanDatLich;
}

(async () => {
  const duongDanDatLich = hamCuaDemo();
  const tinNhan = (P, tuyChon) => goi('GET', `/api/chatbot/phien/${P.idPhien}/tin-nhan`, tuyChon);
  const gui = (P, noiDung, tuyChon = {}) => goi('POST', `/api/chatbot/phien/${P.idPhien}/tin-nhan`, { ...tuyChon, khach: P.maKhach, body: { noiDung } });
  let r;

  console.log('1. Khách: mở phiên, gửi, đọc lại bằng mã bí mật');
  r = await goi('POST', '/api/chatbot/phien');
  const K = r.json.duLieu || {};
  kiem('mở phiên 201, có maKhach và câu chào', r.status === 201 && !!K.maKhach && K.tinNhan.length === 1 && K.tinNhan[0].loai === 'LOI_CHAO', J(r));
  kiem('câu chào bằng tiếng Anh (Accept-Language: en)', !CO_DAU.test(K.tinNhan[0].noiDung), K.tinNhan[0].noiDung);
  const dsBacSi = (await goi('GET', '/api/catalog/bac-si?kichThuoc=100')).json.duLieu.noiDung;
  const bacSi = dsBacSi.find((b) => b.email === 'bacsi01@eclinic.local') || dsBacSi[0];
  // Bác sĩ có ca còn chỗ: lấy từ chính API công khai mà trang đặt lịch dùng
  let coLich = null;
  for (const b of dsBacSi) {
    for (const tu of [ngaySau(0), ngaySau(7), ngaySau(14)]) {
      const ca = (await goi('GET', `/api/booking/khung-gio/nhieu-ngay?idBacSi=${b.id}&tuNgay=${tu}`)).json.duLieu || [];
      if (ca.some((c) => c.khungGio.some((k) => !k.hetCho))) { coLich = { bacSi: b, tu }; break; }
    }
    if (coLich) break;
  }
  kiem('có bác sĩ còn ca đặt được trong 21 ngày tới (dữ liệu của smoke phase 9)', !!coLich, 'không bác sĩ nào còn ca');
  if (!coLich) { console.log(`\n${dat} đạt, ${hong + 1} hỏng`); process.exit(1); }

  r = await gui(K, `#bacsi ${coLich.bacSi.idChuyenKhoa}`);
  const goiYBacSi = ((r.json.duLieu || {}).traLoi || {}).goiY || {};
  kiem('"#bacsi": TRA_LOI kèm goiY.chuyenKhoa và goiY.bacSi', r.status === 200 && r.json.duLieu.traLoi.loai === 'TRA_LOI'
    && goiYBacSi.chuyenKhoa && goiYBacSi.chuyenKhoa.id === coLich.bacSi.idChuyenKhoa && goiYBacSi.bacSi.length > 0, J(r.json).slice(0, 900));
  const theBacSi = (goiYBacSi.bacSi || [])[0] || {};
  const lienKetBacSi = duongDanDatLich({ idChuyenKhoa: goiYBacSi.chuyenKhoa && goiYBacSi.chuyenKhoa.id, idBacSi: theBacSi.id });
  const q1 = new URLSearchParams(lienKetBacSi.split('?')[1]);
  r = await goi('GET', `/api/catalog/bac-si?idChuyenKhoa=${q1.get('idChuyenKhoa')}&kichThuoc=100`);
  kiem('thẻ bác sĩ -> ' + lienKetBacSi + ': bác sĩ thuộc chuyên khoa đó ở API trang đặt lịch dùng',
    lienKetBacSi.startsWith('/appointment?') && r.status === 200 && r.json.duLieu.noiDung.some((b) => String(b.id) === q1.get('idBacSi')), lienKetBacSi);

  r = await gui(K, `#lich ${coLich.bacSi.id} ${coLich.tu}`);
  const lichKham = (((r.json.duLieu || {}).traLoi || {}).goiY || {}).lichKham || [];
  kiem('"#lich": goiY.lichKham có ca và khung giờ', r.status === 200 && lichKham.length > 0 && lichKham[0].khungGio.length > 0, J(r.json).slice(0, 900));
  const lk = lichKham[0] || { khungGio: [{}] };
  const kg = lk.khungGio[0];
  // Thẻ lịch khám không có goiY.chuyenKhoa (chưa tra cứu chuyên khoa trong câu này): đường dẫn không có idChuyenKhoa
  const lienKetKhung = duongDanDatLich({ idChuyenKhoa: null, idBacSi: lk.idBacSi, ngay: lk.ngay, idLichLamViec: lk.idLichLamViec, gioBatDauKhung: kg.gioBatDau });
  const q2 = new URLSearchParams(lienKetKhung.split('?')[1]);
  kiem('đường dẫn của thẻ khung giờ có đủ idBacSi, ngay, idLichLamViec, gioBatDauKhung; không có tham số rỗng',
    ['idBacSi', 'ngay', 'idLichLamViec', 'gioBatDauKhung'].every((k) => q2.get(k)) && !q2.has('idChuyenKhoa'), lienKetKhung);
  r = await goi('GET', `/api/booking/khung-gio?ngay=${q2.get('ngay')}&idBacSi=${q2.get('idBacSi')}`);
  const caKhop = (r.json.duLieu || []).find((c) => String(c.idLichLamViec) === q2.get('idLichLamViec'));
  kiem('GET /api/booking/khung-gio vẫn trả đúng ca và khung giờ đó, còn chỗ',
    r.status === 200 && !!caKhop && caKhop.khungGio.some((k) => k.gioBatDau === q2.get('gioBatDauKhung') && !k.hetCho), J(r.json).slice(0, 600));
  // 2 lời gọi dat-lich.js làm khi mở đường dẫn này: chuyên khoa lấy từ bác sĩ, lịch 7 ngày tính từ ngày của thẻ
  r = await goi('GET', '/api/catalog/bac-si/' + q2.get('idBacSi'));
  kiem('GET /api/catalog/bac-si/{id} cho idChuyenKhoa (form tự chọn chuyên khoa)', r.status === 200 && !!r.json.duLieu.idChuyenKhoa, J(r.json).slice(0, 300));
  r = await goi('GET', `/api/booking/khung-gio/nhieu-ngay?idBacSi=${q2.get('idBacSi')}&tuNgay=${q2.get('ngay')}`);
  kiem('GET khung-gio/nhieu-ngay?tuNgay=<ngày của thẻ> có ca đó (form chọn sẵn ngày và khung giờ)',
    r.status === 200 && r.json.duLieu.some((c) => String(c.idLichLamViec) === q2.get('idLichLamViec') && c.ngay === q2.get('ngay')
      && c.khungGio.some((k) => k.gioBatDau === q2.get('gioBatDauKhung'))), J(r.json).slice(0, 600));
  r = await goi('GET', `/api/booking/khung-gio/nhieu-ngay?idBacSi=${q2.get('idBacSi')}&tuNgay=${ngaySau(400)}`);
  kiem('ngày quá xa trong đường dẫn: API trả ' + r.status + ' (form bỏ qua và xem lại từ hôm nay)', r.status === 200 || r.status === 400, J(r));

  r = await tinNhan(K, { khach: K.maKhach });
  kiem('đọc lại bằng maKhach: 5 tin, thẻ vẫn còn trong tin đã lưu', r.status === 200 && r.json.duLieu.length === 5
    && r.json.duLieu[4].goiY && r.json.duLieu[4].goiY.lichKham.length === lichKham.length, J(r.json).slice(0, 500));
  r = await tinNhan(K);
  kiem('đọc lại không có maKhach -> 404', r.status === 404, J(r));

  console.log('2. Lỗi của chatbot: khung chat hiện thongDiep của server');
  r = await gui(K, '#500');
  kiem('mô hình lỗi -> 503 CHATBOT_KHONG_KHA_DUNG, thongDiep tiếng Anh', r.status === 503 && r.json.maLoi === 'CHATBOT_KHONG_KHA_DUNG'
    && !!r.json.thongDiep && !CO_DAU.test(r.json.thongDiep), J(r));
  r = await gui(K, '#500', { lang: 'vi' });
  kiem('cùng lỗi với Accept-Language: vi -> thongDiep tiếng Việt', r.status === 503 && CO_DAU.test(r.json.thongDiep || ''), J(r));
  r = await gui(K, 'x'.repeat(1001));
  kiem('tin quá 1000 ký tự -> 400 có thongDiep', r.status === 400 && !!r.json.thongDiep, J(r));
  r = await tinNhan(K, { khach: K.maKhach });
  kiem('3 lần lỗi không lưu gì: vẫn 5 tin', r.json.duLieu.length === 5, r.json.duLieu.length);

  console.log('3. Bệnh nhân: đăng nhập thì mở phiên MỚI, xin đồng ý, đồng ý, gửi lại, thẻ');
  const dn = (await goi('POST', '/api/auth/login', { body: { email: 'benhnhan01@eclinic.local', matKhau: 'Demo@123' } })).json.duLieu;
  const A = dn.accessToken;
  r = await goi('POST', '/api/chatbot/phien', { token: A });
  const P = r.json.duLieu || {};
  kiem('phiên của tài khoản: 201, không có maKhach, id khác phiên khách', r.status === 201 && !P.maKhach && P.idPhien !== K.idPhien, J(r));
  r = await gui(P, '#lichtoi', { token: A });
  let tl = (r.json.duLieu || {}).traLoi || {};
  kiem('hỏi lịch của mình lần đầu -> CAN_DONG_Y, không có goiY', r.status === 200 && tl.loai === 'CAN_DONG_Y' && !tl.goiY, J(r.json).slice(0, 500));
  const cauHoiDaLuu = ((r.json.duLieu || {}).tinNhan || {}).noiDung;
  r = await tinNhan(P, { token: A });
  const ds = r.json.duLieu || [];
  kiem('đọc lại: tin cuối là CAN_DONG_Y, ngay trước là câu hỏi của người dùng (khung chat lấy câu này để gửi lại)',
    ds.length === 3 && ds[2].loai === 'CAN_DONG_Y' && ds[1].nguoiGui === 'NGUOI_DUNG' && ds[1].noiDung === '#lichtoi' && cauHoiDaLuu === '#lichtoi', J(ds).slice(0, 600));
  const trangThaiDongY = async () => ((await goi('GET', '/api/chatbot/phien', { token: A })).json.duLieu || []).find((p) => p.idPhien === P.idPhien);
  kiem('GET /api/chatbot/phien: daDongYDuLieu = false', (await trangThaiDongY()).daDongYDuLieu === false);
  r = await goi('POST', `/api/chatbot/phien/${P.idPhien}/dong-y`, { token: A });
  kiem('POST dong-y -> 200 có thongDiep', r.status === 200 && !!r.json.thongDiep, J(r));
  kiem('GET /api/chatbot/phien: daDongYDuLieu = true', (await trangThaiDongY()).daDongYDuLieu === true);
  r = await gui(P, ds[1] ? ds[1].noiDung : '#lichtoi', { token: A });
  tl = (r.json.duLieu || {}).traLoi || {};
  const cuaToi = (tl.goiY || {}).lichHenCuaToi;
  kiem('gửi lại câu hỏi -> TRA_LOI, goiY.lichHenCuaToi là mảng', r.status === 200 && tl.loai === 'TRA_LOI' && Array.isArray(cuaToi), J(r.json).slice(0, 700));
  const sapToi = ((await goi('GET', '/api/booking/lich-hen/cua-toi?loc=SAP_TOI&kichThuoc=10', { token: A })).json.duLieu || {}).noiDung || [];
  kiem(`số thẻ lịch hẹn = số lịch sắp tới của tài khoản (${sapToi.length})`, (cuaToi || []).length === sapToi.length, J(cuaToi));
  if ((cuaToi || []).length) {
    r = await goi('GET', '/api/booking/phieu-kham/' + encodeURIComponent(cuaToi[0].maPhieuKham));
    kiem('thẻ lịch hẹn -> /phieu-kham/<maPhieuKham>: API phiếu khám trả 200 đúng mã tra cứu', r.status === 200 && r.json.duLieu.maTraCuu === cuaToi[0].maTraCuu, J(r).slice(0, 400));
    kiem('giờ trên thẻ là dạng trang cắt được (ISO hoặc HH:mm)', /^(\d{4}-\d{2}-\d{2}T)?\d{2}:\d{2}/.test(cuaToi[0].gioBatDauKhung), cuaToi[0].gioBatDauKhung);
  }
  r = await gui(P, '#lankham', { token: A });
  const lanKham = (((r.json.duLieu || {}).traLoi || {}).goiY || {}).lanKham;
  kiem('"#lankham" -> goiY.lanKham là mảng', r.status === 200 && Array.isArray(lanKham), J(r.json).slice(0, 600));
  if ((lanKham || []).length) {
    r = await goi('GET', '/api/booking/lich-hen/cua-toi/' + encodeURIComponent(lanKham[0].maPhieuKham), { token: A });
    kiem('thẻ lượt khám -> /patient/appointments?ma=<maPhieuKham>: API chi tiết lịch hẹn của tôi trả 200', r.status === 200, J(r).slice(0, 400));
  }

  console.log('4. Rút đồng ý; phiên của người khác');
  r = await goi('DELETE', `/api/chatbot/phien/${P.idPhien}/dong-y`, { token: A });
  kiem('DELETE dong-y -> 200 có thongDiep', r.status === 200 && !!r.json.thongDiep, J(r));
  r = await gui(P, '#lichtoi', { token: A });
  kiem('hỏi lại sau khi rút -> CAN_DONG_Y', ((r.json.duLieu || {}).traLoi || {}).loai === 'CAN_DONG_Y', J(r.json).slice(0, 400));
  r = await goi('POST', `/api/chatbot/phien/${K.idPhien}/dong-y`, { token: A });
  kiem('đồng ý trên phiên khách -> 404 (vì vậy khung chat mở phiên mới sau khi đăng nhập)', r.status === 404, J(r));
  r = await tinNhan(P);
  kiem('khách đọc phiên của tài khoản -> 404 (khung chat bỏ phiên, lần sau mở phiên mới)', r.status === 404, J(r));
  const B = (await goi('POST', '/api/auth/login', { body: { email: 'benhnhan02@eclinic.local', matKhau: 'Demo@123' } })).json.duLieu.accessToken;
  r = await gui(P, 'xin chao', { token: B });
  kiem('tài khoản khác gửi vào phiên của A -> 404', r.status === 404, J(r));

  console.log('5. Vai trò khác vẫn chat được (khung chat có ở mọi màn hình)');
  const bs = (await goi('POST', '/api/auth/login', { body: { email: 'bacsi01@eclinic.local', matKhau: 'Demo@123' } })).json.duLieu.accessToken;
  r = await goi('POST', '/api/chatbot/phien', { token: bs, lang: 'vi' });
  kiem('bác sĩ mở phiên 201, câu chào tiếng Việt', r.status === 201 && CO_DAU.test(r.json.duLieu.tinNhan[0].noiDung), J(r).slice(0, 300));
  r = await goi('GET', '/api/chatbot/phien', { token: bs });
  kiem('bác sĩ đọc danh sách phiên 200', r.status === 200 && r.json.duLieu.length >= 1, J(r).slice(0, 300));

  console.log(`\n${dat} đạt, ${hong} hỏng`);
  process.exit(hong ? 1 : 0);
})().catch((e) => { console.error(e); process.exit(1); });
