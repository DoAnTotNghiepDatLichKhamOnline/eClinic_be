// Smoke phase 4 (DOANTOTNGH-9): email xác nhận đặt lịch / đổi lịch.
// Chạy trên stack eclinic-smoke, DB mới, identity MAIL_MODE=console, notification-service gửi SMTP tới Mailpit
// (container eclinic-smoke-mailpit, API ở 127.0.0.1:18025), NOTIFICATION_MAIL_RETRY_WAITS=5s,5s,5s,5s:
//   node temp/d9-p4-smoke.js
const { execFileSync } = require('child_process');
const GOC = 'http://127.0.0.1:8080';
const MAILPIT = 'http://127.0.0.1:18025';
const NOI_BO = 'http://127.0.0.1:8086/noi-bo/email';
const TEN_MAILPIT = 'eclinic-smoke-mailpit';
let dat = 0, hong = 0;
function kiem(ten, dung, chiTiet) {
  if (dung) { dat++; console.log('  ok   ' + ten); } else { hong++; console.log('  HỎNG ' + ten + (chiTiet ? '\n       ' + String(chiTiet).slice(0, 1500) : '')); }
}
async function goi(phuongThuc, duongDan, { body, token, lang } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = 'Bearer ' + token;
  if (lang) headers['Accept-Language'] = lang;
  const res = await fetch(GOC + duongDan, { method: phuongThuc, headers, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(40000) });
  let json = null; try { json = await res.json(); } catch { /* không phải JSON */ }
  return { status: res.status, json: json || {} };
}
const sh = (...thamSo) => execFileSync('docker', thamSo, { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 }).trim();
const sql = (cau) => sh('exec', 'eclinic-mysql', 'sh', '-c', 'mysql -u$MYSQL_USER -p$MYSQL_PASSWORD $MYSQL_DATABASE -N -e "' + cau + '" 2>/dev/null');
const J = (x) => JSON.stringify(x);
const cho = (ms) => new Promise((xong) => setTimeout(xong, ms));
const hauTo = Date.now().toString().slice(-7);
const ngaySau = (n) => { const d = new Date(Date.now() + n * 86400000); return d.toISOString().slice(0, 10); };
const ddmm = (iso) => iso.slice(8, 10) + '/' + iso.slice(5, 7) + '/' + iso.slice(0, 4);
const gioNgay = (iso, lang) => `${iso.slice(11, 16)} ${lang === 'en' ? 'on' : 'ngày'} ${ddmm(iso)}`;
const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');

// ----- Mailpit -----
async function mp(duongDan, phuongThuc = 'GET') {
  const res = await fetch(MAILPIT + duongDan, { method: phuongThuc, signal: AbortSignal.timeout(10000) });
  try { return await res.json(); } catch { return {}; }
}
async function thuToi(diaChi) {
  const ds = (await mp('/api/v1/messages?limit=200')).messages || [];
  return ds.filter((m) => (m.To || []).some((t) => t.Address === diaChi));
}
/** Chờ tới khi có đủ số thư gửi tới địa chỉ; trả các thư kèm HTML, cũ nhất trước. */
async function choThu(diaChi, soThu = 1, giay = 40) {
  let ds = [];
  for (let i = 0; i < giay; i++) {
    try { ds = await thuToi(diaChi); } catch { ds = []; }
    if (ds.length >= soThu) break;
    await cho(1000);
  }
  const day = [];
  for (const m of ds) day.push(await mp('/api/v1/message/' + m.ID));
  return day.sort((a, b) => new Date(a.Date) - new Date(b.Date));
}
const sqlCho = async (cau, dung, giay = 30) => {
  let kq = '';
  for (let i = 0; i < giay; i++) { kq = sql(cau); if (dung(kq)) break; await cho(1000); }
  return kq;
};

(async () => {
  const dangNhap = async (email, matKhau) => (await goi('POST', '/api/auth/login', { body: { email, matKhau } })).json.duLieu;
  const admin = (await dangNhap(sh('exec', 'eclinic-identity-service', 'printenv', 'ADMIN_EMAIL'), sh('exec', 'eclinic-identity-service', 'printenv', 'ADMIN_PASSWORD'))).accessToken;
  const dnA = await dangNhap('bacsi01@eclinic.local', 'Demo@123');
  const dnBn = await dangNhap('benhnhan01@eclinic.local', 'Demo@123');
  const bsA = dnA.accessToken, bn = dnBn.accessToken, tenA = dnA.taiKhoan.hoTen;
  const EMAIL_BN = 'benhnhan01@eclinic.local';
  let r;

  console.log('0. Chuẩn bị');
  kiem('V19 đã chạy', sql("select count(*) from flyway_schema_history where version = '19' and success = 1") === '1');
  kiem('identity ở chế độ console, notification ở chế độ smtp tới mailpit',
    sh('exec', 'eclinic-identity-service', 'printenv', 'MAIL_MODE') === 'console'
    && sh('exec', 'eclinic-notification-service', 'printenv', 'NOTIFICATION_MAIL_MODE') === 'smtp'
    && sh('exec', 'eclinic-notification-service', 'printenv', 'MAIL_HOST') === 'mailpit');
  await mp('/api/v1/messages', 'DELETE');
  const A = (await goi('GET', '/api/catalog/bac-si?kichThuoc=100')).json.duLieu.noiDung.find((b) => b.hoTen === tenA);
  const P1 = (await goi('GET', '/api/catalog/quan-tri/phong-kham?kichThuoc=100', { token: admin })).json.duLieu.noiDung.find((p) => p.idChuyenKhoa === A.idChuyenKhoa);
  const phong = (lang) => {
    const ten = lang === 'en' && P1.tenPhongEn ? P1.tenPhongEn : P1.tenPhong;
    const tang = lang === 'en' && P1.tangEn ? P1.tangEn : P1.tang;
    return `<b>${esc(ten)}</b>` + (tang ? ` (${esc(tang)})` : '');
  };
  const D1 = ngaySau(15);
  r = await goi('POST', '/api/booking/quan-tri/lich-lam-viec', { token: admin, body: { idBacSi: A.id, idPhongKham: P1.id, ngay: D1, gioBatDau: '17:00', gioKetThuc: '21:00', soLuotToiDaMoiGio: 6, thoiLuongLuotPhut: 10 } });
  kiem(`xếp 1 ca 17:00-21:00 cho ${tenA} ở ${P1.tenPhong} / ${P1.tenPhongEn} (${P1.tang} / ${P1.tangEn})`, r.status === 201, J(r.json));
  const C1 = r.json.duLieu.daTao[0];
  const khung = async () => (await goi('GET', `/api/booking/khung-gio?ngay=${D1}&idBacSi=${A.id}`)).json.duLieu.find((c) => c.idLichLamViec === C1.idLichLamViec).khungGio;
  const K = await khung();

  let soNguoi = 0;
  const LY_DO = 'Dau bung am i smoke-p4';
  async function datLich({ token, lang, email, viTriKhung = 0, hoTen }) {
    soNguoi++;
    const ten = hoTen || 'Trần Văn Thử ' + ['Một', 'Hai', 'Ba', 'Bốn', 'Năm', 'Sáu', 'Bảy', 'Tám', 'Chín', 'Mười'][soNguoi - 1];
    const soDienThoai = '03' + hauTo + soNguoi, cccd = '0792' + hauTo + soNguoi;
    const body = {
      idLichLamViec: C1.idLichLamViec, gioBatDauKhung: K[viTriKhung].gioBatDau, lyDoKham: LY_DO,
      benhNhan: { hoTen: ten, ngaySinh: '1991-06-06', gioiTinh: 'NAM', soDienThoai, cccd, email },
    };
    if (token) body.datChoBanThan = false;
    const kq = await goi('POST', '/api/booking/lich-hen', { token, lang, body });
    if (kq.status !== 201) { kiem('đặt lịch cho ' + ten, false, J(kq.json)); return {}; }
    return { ...kq.json.duLieu, hoTen: ten, soDienThoai, cccd };
  }
  /** Các phần chung của thân email theo ngôn ngữ. */
  function kiemThan(ten, thu, L, lang) {
    const html = thu.HTML || '';
    const nhan = lang === 'en' ? ['Patient', 'Time', 'Doctor', 'Room', 'Booking code'] : ['Bệnh nhân', 'Thời gian', 'Bác sĩ', 'Phòng khám', 'Mã tra cứu'];
    const mong = [
      `<li>${nhan[0]}: <b>${esc(L.hoTen)}</b></li>`,
      `<li>${nhan[1]}: <b>${gioNgay(L.gioKhamDuKien, lang)}</b></li>`,
      `<li>${nhan[2]}: <b>${esc(tenA)}</b></li>`,
      `<li>${nhan[3]}: ${phong(lang)}</li>`,
      `<li>${nhan[4]}: <b>${L.maTraCuu}</b></li>`,
      `<a href="${esc(L.linkPhieuKham)}"`,
      lang === 'en' ? '<p>Hello,</p>' : '<p>Xin chào,</p>',
      lang === 'en' ? 'waiting for the doctor to confirm' : 'chờ bác sĩ xác nhận',
    ];
    const thieu = mong.filter((m) => !html.includes(m));
    kiem(`${ten}: thân email [${lang}] đủ họ tên, giờ, bác sĩ, phòng + tầng, mã, link phiếu khám`, thieu.length === 0, 'thiếu: ' + J(thieu) + '\n' + html);
    kiem(`${ten}: không có lý do khám, số điện thoại, CCCD`, html.length > 0 && !html.includes(LY_DO) && !html.includes(L.soDienThoai) && !html.includes(L.cccd), html);
  }

  console.log('1. Khách đặt lịch, có email liên hệ, tiếng Anh');
  const g1 = `khach1-${hauTo}@smoke.test`;
  const L1 = await datLich({ lang: 'en', email: g1 });
  let thu = await choThu(g1);
  kiem('đúng 1 thư tới email liên hệ', thu.length === 1, J(thu.map((t) => t.Subject)));
  const T1 = thu[0] || {};
  kiem(`tiêu đề: ${T1.Subject}`, T1.Subject === `[eClinic] Booking request received (code ${L1.maTraCuu})`);
  kiem(`người gửi: ${T1.From && T1.From.Name} <${T1.From && T1.From.Address}>`, T1.From && T1.From.Name === 'eClinic');
  kiemThan('khách EN', T1, L1, 'en');
  kiem('lich_hen.ngon_ngu = EN', sql(`select ngon_ngu from lich_hen where ma_tra_cuu = '${L1.maTraCuu}'`) === 'EN');

  console.log('2. Khách đặt lịch, tiếng Việt');
  const g2 = `khach2-${hauTo}@smoke.test`;
  const L2 = await datLich({ email: g2 });
  thu = await choThu(g2);
  const T2 = thu[0] || {};
  kiem(`tiêu đề: ${T2.Subject}`, thu.length === 1 && T2.Subject === `[eClinic] Đã nhận yêu cầu đặt lịch khám (mã ${L2.maTraCuu})`);
  kiemThan('khách VI', T2, L2, 'vi');
  kiem('họ tên bác sĩ giống hệt nhau ở 2 email', (T1.HTML || '').includes(`<b>${esc(tenA)}</b>`) && (T2.HTML || '').includes(`<b>${esc(tenA)}</b>`));

  console.log('3. Tài khoản đặt lịch');
  r = await goi('PUT', '/api/users/me/language', { token: bn, body: { ngonNgu: 'EN' } });
  kiem('tài khoản bệnh nhân lưu ngôn ngữ EN', r.status === 200, J(r.json));
  const L3 = await datLich({ token: bn, lang: 'vi' });
  thu = await choThu(EMAIL_BN);
  const T3 = thu[0] || {};
  kiem(`không nhập email liên hệ -> gửi tới email tài khoản, theo ngôn ngữ đã lưu (request là vi): ${T3.Subject}`,
    thu.length === 1 && T3.Subject === `[eClinic] Booking request received (code ${L3.maTraCuu})`);
  kiemThan('tài khoản EN', T3, L3, 'en');
  const g4 = `lienhe4-${hauTo}@smoke.test`;
  const L4 = await datLich({ token: bn, lang: 'vi', email: g4 });
  thu = await choThu(g4);
  kiem('có email liên hệ -> gửi tới email liên hệ', thu.length === 1 && thu[0].Subject.includes(L4.maTraCuu), J(thu.map((t) => t.Subject)));
  kiem('email tài khoản không nhận thêm thư', (await thuToi(EMAIL_BN)).length === 1);

  console.log('4. Khách không nhập email');
  const L5 = await datLich({ lang: 'en' });
  kiem('đặt lịch vẫn thành công', !!L5.maTraCuu);
  await cho(9000);
  kiem('không có dòng email trong bảng chờ gửi', sql(`select count(*) from su_kien_thong_bao s join lich_hen l on l.id_lich_hen = s.id_lich_hen where l.ma_tra_cuu = '${L5.maTraCuu}' and s.kenh = 'EMAIL'`) === '0');
  kiem('không có dòng email_gui', sql(`select count(*) from email_gui e join lich_hen l on l.id_lich_hen = e.id_lich_hen where l.ma_tra_cuu = '${L5.maTraCuu}'`) === '0');
  const tongTruoc = (await mp('/api/v1/messages?limit=1')).total;
  kiem(`tổng số thư tới giờ: ${tongTruoc}`, tongTruoc === 4);

  console.log('5. Đổi lịch');
  r = await goi('POST', `/api/booking/lich-hen/cua-toi/${L3.maPhieuKham}/doi-lich`, { token: bn, lang: 'vi', body: { idLichLamViec: C1.idLichLamViec, gioBatDauKhung: K[2].gioBatDau } });
  kiem('tài khoản đổi lịch L3', r.status === 200 || r.status === 201, J(r.json));
  const L3m = { ...L3, ...r.json.duLieu };
  thu = await choThu(EMAIL_BN, 2);
  const T3m = thu[1] || {};
  kiem(`thư "đã đổi lịch": ${T3m.Subject}`, thu.length === 2 && T3m.Subject === `[eClinic] Appointment rescheduled (new code ${L3m.maTraCuu})`, J(thu.map((t) => t.Subject)));
  kiemThan('đổi lịch (tài khoản)', T3m, L3m, 'en');
  kiem('mã và link mới khác lịch cũ', L3m.maTraCuu !== L3.maTraCuu && L3m.linkPhieuKham !== L3.linkPhieuKham && !(T3m.HTML || '').includes(L3.linkPhieuKham));
  r = await goi('POST', `/api/booking/phieu-kham/${L2.maPhieuKham}/doi-lich`, { lang: 'en', body: { idLichLamViec: C1.idLichLamViec, gioBatDauKhung: K[2].gioBatDau, soDienThoai: L2.soDienThoai } });
  kiem('khách đổi lịch L2 bằng link phiếu khám', r.status === 200 || r.status === 201, J(r.json));
  const L2m = { ...L2, ...r.json.duLieu };
  thu = await choThu(g2, 2);
  const T2m = thu[1] || {};
  kiem(`khách: thư "đã đổi lịch" theo ngôn ngữ của request đổi lịch (en): ${T2m.Subject}`, thu.length === 2 && T2m.Subject === `[eClinic] Appointment rescheduled (new code ${L2m.maTraCuu})`, J(thu.map((t) => t.Subject)));
  kiemThan('đổi lịch (khách)', T2m, L2m, 'en');

  console.log('6. Chuông của bác sĩ không đổi');
  let chuong = [];
  for (let i = 0; i < 10; i++) {
    chuong = (await goi('GET', '/api/notification/thong-bao?kichThuoc=100', { token: bsA })).json.duLieu.noiDung;
    if (chuong.some((t) => t.noiDung.includes(L1.maTraCuu))) break;
    await cho(1500);
  }
  const tb = chuong.find((t) => t.loai === 'LICH_HEN_MOI' && t.noiDung.includes(L1.maTraCuu)) || {};
  kiem(`bác sĩ vẫn nhận "lịch hẹn mới": ${tb.noiDung}`, tb.noiDung === `Yêu cầu đặt lịch mới: bệnh nhân ${L1.hoTen}, ${gioNgay(L1.gioKhamDuKien)} (mã ${L1.maTraCuu}).`);
  kiem('bác sĩ nhận "đã đổi" của lịch L3', chuong.some((t) => t.loai === 'LICH_HEN_DA_DOI' && t.noiDung.includes(L3m.maTraCuu)));

  console.log('7. Máy chủ mail ngừng');
  sh('stop', TEN_MAILPIT);
  const g7 = `khach7-${hauTo}@smoke.test`;
  const L7 = await datLich({ email: g7, viTriKhung: 1 });
  const dk7 = `from email_gui e join lich_hen l on l.id_lich_hen = e.id_lich_hen where l.ma_tra_cuu = '${L7.maTraCuu}'`;
  let dong = await sqlCho(`select concat(e.trang_thai, ' ', e.so_lan_thu, ' ', ifnull(e.loi_cuoi, '-')) ${dk7}`, (k) => /^CHO_GUI [1-9]/.test(k));
  kiem(`email nằm chờ, đã ghi lần lỗi: ${dong}`, /^CHO_GUI [1-9]\d* Mail/.test(dong));
  sh('start', TEN_MAILPIT);
  thu = await choThu(g7, 1, 60);
  dong = await sqlCho(`select e.trang_thai ${dk7}`, (k) => k === 'DA_GUI', 10);
  kiem('máy chủ mail chạy lại -> thư tới đúng 1 lần', thu.length === 1 && thu[0].Subject.includes(L7.maTraCuu), J(thu.map((t) => t.Subject)));
  kiem(`dòng email_gui: ${dong}, có ngay_gui`, dong === 'DA_GUI' && sql(`select count(*) ${dk7} and e.ngay_gui is not null`) === '1');

  console.log('8. notification-service ngừng');
  sh('stop', 'eclinic-notification-service');
  const g8 = `khach8-${hauTo}@smoke.test`;
  const L8 = await datLich({ email: g8, viTriKhung: 1 });
  kiem('đặt lịch vẫn thành công khi notification-service ngừng', !!L8.maTraCuu);
  await cho(8000);
  const dk8 = `from su_kien_thong_bao s join lich_hen l on l.id_lich_hen = s.id_lich_hen where l.ma_tra_cuu = '${L8.maTraCuu}' and s.kenh = 'EMAIL'`;
  kiem('dòng email nằm chờ ở booking-service', sql(`select count(*) ${dk8} and s.ngay_gui is null`) === '1');
  sh('start', 'eclinic-notification-service');
  thu = await choThu(g8, 1, 150);
  await cho(8000);
  kiem('notification-service chạy lại -> đúng 1 thư', (await thuToi(g8)).length === 1 && thu.length === 1, J(thu.map((t) => t.Subject)));
  kiem('dòng ở booking-service đã đánh dấu gửi', sql(`select count(*) ${dk8} and s.ngay_gui is not null`) === '1');

  console.log('9. API nội bộ /noi-bo/email');
  const KHOA = sh('exec', 'eclinic-notification-service', 'printenv', 'INTERNAL_API_KEY');
  const tenHeader = /TEN_HEADER\s*=\s*"([^"]+)"/.exec(require('fs').readFileSync('common/src/main/java/iuh/fit/se/eclinic/common/security/KhoaNoiBo.java', 'utf8'))[1];
  const noiBo = async (danhSach, khoa = KHOA) => {
    const headers = { 'Content-Type': 'application/json' };
    if (khoa) headers[tenHeader] = khoa;
    const res = await fetch(NOI_BO, { method: 'POST', headers, body: JSON.stringify(danhSach), signal: AbortSignal.timeout(20000) });
    let json = {}; try { json = await res.json(); } catch { /* rỗng */ }
    return { status: res.status, json };
  };
  const g9 = `noibo9-${hauTo}@smoke.test`;
  const tenHtml = '<b>Xấu</b> & "Tốt"';
  const soNguon = Number(hauTo);
  const thamSo = (ten) => J([ten, { gn: D1 + 'T17:00' }, tenA, { vi: 'Phòng A', en: 'Room A' }, { vi: '' }, 'LH-SMOKE', 'http://localhost:5173/phieu-kham/abc?x=1&y=2']);
  const mau = { maNguon: 'smoke:' + soNguon, nguoiNhan: g9, ngonNgu: 'EN', khoaMau: 'email.datLich', thamSo: thamSo(tenHtml), idLichHen: null };
  r = await noiBo([mau]);
  kiem('nhận 1 email', r.status === 200 && r.json.duLieu.daTao === 1, J(r.json));
  r = await noiBo([mau]);
  kiem('gửi lại cùng maNguon -> bỏ qua', r.status === 200 && r.json.duLieu.daTao === 0 && r.json.duLieu.boQua === 1, J(r.json));
  kiem('chỉ 1 dòng email_gui', sql(`select count(*) from email_gui where ma_nguon = 'smoke:${soNguon}'`) === '1');
  thu = await choThu(g9);
  const T9 = thu[0] || {};
  kiem('giá trị có HTML được escape trong thân email', thu.length === 1 && (T9.HTML || '').includes(`<b>${esc(tenHtml)}</b>`) && !(T9.HTML || '').includes('<b>Xấu</b>'), T9.HTML);
  kiem('phòng không có tầng -> dòng không có ngoặc; link có & được escape trong href',
    (T9.HTML || '').includes('<li>Room: <b>Room A</b></li>') && (T9.HTML || '').includes('href="http://localhost:5173/phieu-kham/abc?x=1&amp;y=2"'), T9.HTML);
  await cho(6000);
  kiem('vẫn chỉ 1 thư sau vài lần job chạy', (await thuToi(g9)).length === 1);
  r = await noiBo([{ ...mau, maNguon: 'smoke:' + (soNguon + 1), khoaMau: 'email.khongCo' }]);
  kiem(`mẫu không có -> 400: ${r.json.thongDiep}`, r.status === 400, J(r.json));
  r = await noiBo([{ ...mau, maNguon: 'smoke:' + (soNguon + 2), khoaMau: 'thongBao.lichHenMoi' }]);
  kiem('mẫu của chuông không dùng làm email được -> 400', r.status === 400, J(r.json));
  r = await noiBo([{ ...mau, maNguon: 'smoke:' + (soNguon + 3), thamSo: '["a","b"]' }]);
  kiem('thiếu tham số -> 400', r.status === 400, J(r.json));
  r = await noiBo([{ ...mau, maNguon: 'smoke:' + (soNguon + 4), thamSo: '{hong' }]);
  kiem('tham số không đọc được -> 400', r.status === 400, J(r.json));
  r = await noiBo([{ ...mau, maNguon: 'smoke:' + (soNguon + 5), nguoiNhan: 'khong-phai-email' }]);
  kiem('địa chỉ sai dạng -> 400', r.status === 400, J(r.json));
  r = await noiBo([mau], null);
  kiem(`không có khoá nội bộ -> ${r.status}`, r.status === 401 || r.status === 403, J(r.json));
  r = await goi('POST', '/noi-bo/email', { body: [mau] });
  kiem(`không ra được qua gateway -> ${r.status}`, r.status === 404 || r.status === 401 || r.status === 403, J(r.json));
  kiem('không dòng nào được tạo từ các request bị từ chối', sql(`select count(*) from email_gui where ma_nguon like 'smoke:%'`) === '1');
  // Dòng hỏng trong bảng chờ gửi của booking-service: bị từ chối riêng, đếm lỗi, không chặn dòng sau
  sql(`insert into su_kien_thong_bao (kenh, email_nhan, ngon_ngu, khoa_mau, tham_so) values ('EMAIL', 'hong-${hauTo}@smoke.test', 'VI', 'email.khongCo', '[]')`);
  const g10 = `khach10-${hauTo}@smoke.test`;
  const L10 = await datLich({ email: g10, viTriKhung: 1 });
  thu = await choThu(g10);
  kiem('dòng sau dòng hỏng vẫn tới', thu.length === 1 && thu[0].Subject.includes(L10.maTraCuu));
  dong = await sqlCho(`select concat(so_lan_loi, ' ', ifnull(ngay_gui, 'null')) from su_kien_thong_bao where email_nhan = 'hong-${hauTo}@smoke.test'`, (k) => k.startsWith('3 '), 30);
  kiem(`dòng hỏng: so_lan_loi + ngay_gui = ${dong}`, dong === '3 null');

  console.log('10. Log');
  const logNoti = sh('logs', 'eclinic-notification-service');
  const dongLoi = logNoti.split('\n').filter((d) => / ERROR /.test(d));
  kiem('notification-service không có dòng ERROR', dongLoi.length === 0, dongLoi.slice(0, 5).join('\n'));
  const diaChiTrongLog = [g1, g2, g7, g8, g9, EMAIL_BN].filter((d) => logNoti.includes(d));
  kiem('log của notification-service không chứa địa chỉ người nhận', diaChiTrongLog.length === 0, J(diaChiTrongLog));
  kiem('log có dòng "chưa gửi được" và "đã gửi lại được" của bước 7', /Chưa gửi được email id=\d+ \(Mail/.test(logNoti) && logNoti.includes('Đã gửi lại được email'));
  const logBooking = sh('logs', 'eclinic-booking-service');
  kiem('booking-service ghi nhận dòng bị từ chối', /từ chối sự kiện thông báo id=\d+ \(HTTP 400\), lần 3\/3/.test(logBooking));
  kiem('identity-service không gửi thư nào ra ngoài (chế độ console)', sh('logs', 'eclinic-identity-service').includes('chế độ CONSOLE'));

  console.log('11. Thử hết số lần thì bỏ');
  sh('stop', TEN_MAILPIT);
  r = await noiBo([{ ...mau, maNguon: 'smoke:' + (soNguon + 9), nguoiNhan: `bo-${hauTo}@smoke.test`, thamSo: thamSo('Nguoi Bi Bo') }]);
  dong = await sqlCho(`select concat(trang_thai, ' ', so_lan_thu) from email_gui where ma_nguon = 'smoke:${soNguon + 9}'`, (k) => k.startsWith('THAT_BAI'), 60);
  kiem(`sau 4 lần chờ (5s) vẫn lỗi -> ${dong}`, dong === 'THAT_BAI 5');
  sh('start', TEN_MAILPIT);
  await cho(8000);
  kiem('email đã bỏ không được gửi lại khi máy chủ mail chạy lại', (await thuToi(`bo-${hauTo}@smoke.test`)).length === 0);
  kiem('log có đúng 1 dòng ERROR "Bỏ email"', sh('logs', 'eclinic-notification-service').split('\n').filter((d) => / ERROR /.test(d)).length === 1);

  r = await goi('PUT', '/api/users/me/language', { token: bn, body: { ngonNgu: 'VI' } });
  console.log(`\n${dat} đạt, ${hong} hỏng`);
  process.exit(hong ? 1 : 0);
})().catch((e) => { console.error(e); process.exit(2); });
