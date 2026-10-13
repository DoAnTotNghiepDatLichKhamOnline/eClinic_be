// Smoke phase 5 (DOANTOTNGH-9): nhắc lịch khám. 3 chặng, booking-service được tạo lại giữa các chặng (xem temp/d9-p5-run.sh):
//   1 "ban đêm"  : giờ gửi KHÔNG chứa giờ hiện tại; mốc 20d,10d; khoảng cách tối thiểu 3d -> chuẩn bị dữ liệu, chưa nhắc gì
//   2 "ban ngày" : giờ gửi chứa giờ hiện tại -> nhắc đúng các lịch đến hạn, mỗi mốc 1 lần
//   3 mốc 20d,16d: mốc mới đến hạn -> nhắc lần 2, không có lần 3
// Stack eclinic-smoke, DB mới, Mailpit ở 127.0.0.1:18025.   node temp/d9-p5-smoke.js <1|2|3>
const { execFileSync } = require('child_process');
const fs = require('fs');
const CHANG = Number(process.argv[2]);
const TEP = 'temp/d9-p5-state.json';
const GOC = 'http://127.0.0.1:8080';
const MAILPIT = 'http://127.0.0.1:18025';
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
const ngaySau = (n) => { const d = new Date(Date.now() + n * 86400000); return d.toISOString().slice(0, 10); };
const ddmm = (iso) => iso.slice(8, 10) + '/' + iso.slice(5, 7) + '/' + iso.slice(0, 4);
const gioNgay = (iso, lang) => `${iso.slice(11, 16)} ${lang === 'en' ? 'on' : 'ngày'} ${ddmm(iso)}`;
const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
const LA_NHAC = /Nhắc lịch|[Rr]eminder/;
const EMAIL_BN = 'benhnhan01@eclinic.local';
const LY_DO = 'Dau dau smoke-p5';

async function mp(duongDan) {
  const res = await fetch(MAILPIT + duongDan, { signal: AbortSignal.timeout(10000) });
  try { return await res.json(); } catch { return {}; }
}
const tatCaThu = async () => (await mp('/api/v1/messages?limit=500')).messages || [];
const thuNhac = async (diaChi) => (await tatCaThu()).filter((m) => LA_NHAC.test(m.Subject) && (!diaChi || m.To.some((t) => t.Address === diaChi)));
const thuDay = async (m) => mp('/api/v1/message/' + m.ID);
/** Các mốc đã ghi của 1 lịch hẹn, dạng "14400:1,28800:0". */
const moc = (maTraCuu) => sql(`select ifnull(group_concat(concat(n.so_phut_truoc, ':', n.da_gui) order by n.so_phut_truoc), '') from lich_hen l left join nhac_lich_hen n on n.id_lich_hen = l.id_lich_hen where l.ma_tra_cuu = '${maTraCuu}'`);
const phienBan = (cacMa) => sql(`select ifnull(group_concat(concat(ma_tra_cuu, '=', version) order by ma_tra_cuu), '') from lich_hen where ma_tra_cuu in (${cacMa.map((m) => `'${m}'`).join(',')})`);
const moiTruong = (ten) => sh('exec', 'eclinic-booking-service', 'printenv', ten);
const dangNhap = async (email, matKhau) => (await goi('POST', '/api/auth/login', { body: { email, matKhau } })).json.duLieu;
const chuongCua = async (token, lang) => (await goi('GET', '/api/notification/thong-bao?kichThuoc=100', { token, lang })).json.duLieu.noiDung;
function kiemLog() {
  const logBooking = sh('logs', 'eclinic-booking-service');
  const loi = logBooking.split('\n').filter((d) => / ERROR /.test(d));
  kiem('booking-service không có dòng ERROR', loi.length === 0, loi.slice(0, 4).join('\n'));
  return logBooking;
}

async function chang1() {
  const hauTo = Date.now().toString().slice(-7);
  const admin = (await dangNhap(sh('exec', 'eclinic-identity-service', 'printenv', 'ADMIN_EMAIL'), sh('exec', 'eclinic-identity-service', 'printenv', 'ADMIN_PASSWORD'))).accessToken;
  const dnA = await dangNhap('bacsi01@eclinic.local', 'Demo@123');
  const bsA = dnA.accessToken, tenA = dnA.taiKhoan.hoTen;
  const bn = (await dangNhap(EMAIL_BN, 'Demo@123')).accessToken;
  let r;
  console.log('1.0 Chuẩn bị ("ban đêm": ' + moiTruong('BOOKING_REMINDER_FROM') + ' - ' + moiTruong('BOOKING_REMINDER_TO') + ', mốc ' + moiTruong('BOOKING_REMINDER_BEFORE') + ')');
  kiem('V20 đã chạy', sql("select count(*) from flyway_schema_history where version = '20' and success = 1") === '1');
  await fetch(MAILPIT + '/api/v1/messages', { method: 'DELETE' });
  const A = (await goi('GET', '/api/catalog/bac-si?kichThuoc=100')).json.duLieu.noiDung.find((b) => b.hoTen === tenA);
  const dsPhong = (await goi('GET', '/api/catalog/quan-tri/phong-kham?kichThuoc=100', { token: admin })).json.duLieu.noiDung;
  const P1 = dsPhong.find((p) => p.idChuyenKhoa === A.idChuyenKhoa);
  const CA = '/api/booking/quan-tri/lich-lam-viec';
  const taoCa = (ngay) => goi('POST', CA, { token: admin, body: { idBacSi: A.id, idPhongKham: P1.id, ngay, gioBatDau: '17:00', gioKetThuc: '21:00', soLuotToiDaMoiGio: 6, thoiLuongLuotPhut: 10 } });
  r = await taoCa(ngaySau(15));
  kiem('xếp ca ngày +15 (trong khoảng mốc 20d, ngoài mốc 10d)', r.status === 201, J(r.json));
  const C15 = r.json.duLieu.daTao[0];
  r = await taoCa(ngaySau(16));
  kiem('xếp ca ngày +16 (sẽ bị quản trị viên hủy)', r.status === 201, J(r.json));
  const C16 = r.json.duLieu.daTao[0];
  const caCua = async (ngay) => (await goi('GET', `/api/booking/khung-gio?ngay=${ngay}&idBacSi=${A.id}`)).json.duLieu || [];
  const khungCua = async (ngay, idCa) => (await caCua(ngay)).find((c) => c.idLichLamViec === idCa).khungGio;
  /** Ca có sẵn (dữ liệu mẫu) của bác sĩ A trong các ngày cho trước, còn khung đặt được. */
  async function caMau(tuNgay, denNgay) {
    for (let n = tuNgay; n <= denNgay; n++) {
      for (const ca of await caCua(ngaySau(n))) {
        const viTri = ca.khungGio.findIndex((k) => k.soChoConLai > 1);
        if (viTri >= 0) return { idLichLamViec: ca.idLichLamViec, gioBatDauKhung: ca.khungGio[viTri].gioBatDau, ngay: n };
      }
    }
    return null;
  }
  const K15 = await khungCua(ngaySau(15), C15.idLichLamViec), K16 = await khungCua(ngaySau(16), C16.idLichLamViec);
  const gan = await caMau(4, 9), satGio = await caMau(1, 2);
  kiem(`có ca mẫu trong 4-9 ngày tới (ngày +${gan && gan.ngay}) và trong 1-2 ngày tới (ngày +${satGio && satGio.ngay})`, !!gan && !!satGio, J([gan, satGio]));

  let soNguoi = 0;
  async function datLich(ten, cho_, { token, lang, email } = {}) {
    soNguoi++;
    const hoTen = 'Lê Thị Nhắc ' + ten;
    const so = soNguoi % 10, soDienThoai = '03' + hauTo + so, cccd = '0793' + hauTo + so;
    const body = { idLichLamViec: cho_.idLichLamViec, gioBatDauKhung: cho_.gioBatDauKhung, lyDoKham: LY_DO,
      benhNhan: { hoTen, ngaySinh: '1992-07-07', gioiTinh: 'NU', soDienThoai, cccd: cccd.slice(0, 12), email } };
    if (token) body.datChoBanThan = false;
    const kq = await goi('POST', '/api/booking/lich-hen', { token, lang, body });
    if (kq.status !== 201) { kiem('đặt lịch cho ' + hoTen, false, J(kq.json)); return {}; }
    const d = kq.json.duLieu;
    return { maTraCuu: d.maTraCuu, maPhieuKham: d.maPhieuKham, linkPhieuKham: d.linkPhieuKham, gioKhamDuKien: d.gioKhamDuKien, hoTen, soDienThoai, cccd: cccd.slice(0, 12), email: email || null, phong: d.phongKham };
  }
  const o15 = (viTri) => ({ idLichLamViec: C15.idLichLamViec, gioBatDauKhung: K15[viTri].gioBatDau });
  const g = (so) => `nhac${so}-${hauTo}@smoke.test`;
  const idCuaBacSi = async (maTraCuu) => {
    const ds = (await goi('GET', '/api/booking/bac-si/toi/lich-hen/yeu-cau?kichThuoc=100', { token: bsA })).json.duLieu.noiDung;
    return (ds.find((l) => l.maTraCuu === maTraCuu) || {}).id;
  };
  const xacNhan = async (L) => goi('POST', `/api/booking/bac-si/toi/lich-hen/${await idCuaBacSi(L.maTraCuu)}/xac-nhan`, { token: bsA });

  console.log('1.1 Đặt lịch và đưa về các trạng thái cần thử');
  r = await goi('PUT', '/api/users/me/language', { token: bn, body: { ngonNgu: 'EN' } });
  const X = {};
  X.x1 = await datLich('Một', o15(0), { email: g(1) });                              // khách VI, đã xác nhận, +15 ngày
  X.x2 = await datLich('Hai', gan, { token: bn, lang: 'vi' });                       // tài khoản EN, đã xác nhận, trong 10 ngày
  X.x3 = await datLich('Ba', gan, { email: g(3) });                                  // vừa đặt, trong 10 ngày (không lùi ngày tạo)
  X.x4 = await datLich('Bốn', o15(0), { token: bn, lang: 'vi', email: g(4) });       // sẽ hủy
  X.x5 = await datLich('Năm', o15(0), { email: g(5) });                              // bác sĩ từ chối
  X.x6 = await datLich('Sáu', o15(0), { email: g(6) });                              // sẽ đổi lịch (lịch cũ)
  X.x7 = await datLich('Bảy', { idLichLamViec: C16.idLichLamViec, gioBatDauKhung: K16[0].gioBatDau }, { email: g(7) }); // ca bị hủy
  X.x8 = await datLich('Tám', o15(0), { lang: 'en', email: g(8) });                  // khách EN, còn chờ xác nhận
  X.x9 = await datLich('Chín', o15(0), {});                                          // khách không email
  X.x10 = await datLich('Mười', satGio, { email: g(10) });                           // gần hơn khoảng cách tối thiểu
  kiem('đặt đủ 10 lịch hẹn', Object.values(X).every((l) => l.maTraCuu), J(X));
  r = await xacNhan(X.x1); kiem('bác sĩ xác nhận x1', r.status === 200, J(r.json));
  r = await xacNhan(X.x2); kiem('bác sĩ xác nhận x2', r.status === 200, J(r.json));
  r = await goi('POST', `/api/booking/lich-hen/cua-toi/${X.x4.maPhieuKham}/huy`, { token: bn, body: { lyDo: 'Ban viec' } });
  kiem('hủy x4', r.status === 200, J(r.json));
  r = await goi('POST', `/api/booking/bac-si/toi/lich-hen/${await idCuaBacSi(X.x5.maTraCuu)}/tu-choi`, { token: bsA, body: { lyDo: 'Bac si ban' } });
  kiem('bác sĩ từ chối x5', r.status === 200, J(r.json));
  r = await goi('POST', `/api/booking/phieu-kham/${X.x6.maPhieuKham}/doi-lich`, { body: { idLichLamViec: C15.idLichLamViec, gioBatDauKhung: K15[2].gioBatDau, soDienThoai: X.x6.soDienThoai } });
  kiem('đổi lịch x6 -> lịch mới', r.status === 200 || r.status === 201, J(r.json));
  X.x6moi = { ...X.x6, maTraCuu: r.json.duLieu.maTraCuu, maPhieuKham: r.json.duLieu.maPhieuKham };
  r = await goi('POST', `${CA}/${C16.idLichLamViec}/huy`, { token: admin, body: { lyDo: 'Bao tri' } });
  kiem('quản trị viên hủy ca ngày +16 -> x7 chờ đổi lịch', (r.status === 200 || r.status === 204) && sql(`select can_doi_lich from lich_hen where ma_tra_cuu = '${X.x7.maTraCuu}'`) === '1', J(r.json));
  // "Đặt từ lâu": lùi ngày tạo 40 ngày cho mọi lịch trừ x3 và lịch mới của x6
  const lui = ['x1', 'x2', 'x4', 'x5', 'x6', 'x7', 'x8', 'x9', 'x10'].map((k) => `'${X[k].maTraCuu}'`).join(',');
  sql(`update lich_hen set ngay_tao = ngay_tao - interval 40 day where ma_tra_cuu in (${lui})`);
  kiem('đã lùi ngày tạo 9 lịch hẹn', sql(`select count(*) from lich_hen where ngay_tao < now() - interval 30 day`) === '9');

  console.log('1.2 Ngoài giờ gửi: không nhắc gì');
  await cho(15000);
  kiem('chưa có dòng nhac_lich_hen nào', sql('select count(*) from nhac_lich_hen') === '0');
  kiem('chưa có thư nhắc nào', (await thuNhac()).length === 0);
  const thuXacNhan = (await tatCaThu()).length;
  kiem(`email xác nhận / đổi lịch (phase 4) vẫn đi ban đêm: ${thuXacNhan} thư`, thuXacNhan >= 9);
  kiemLog();
  const cacMa = Object.values(X).map((l) => l.maTraCuu);
  const P = dsPhong.find((p) => p.id === X.x2.phong.id) || P1;
  fs.writeFileSync(TEP, J({ hauTo, X, tenA, P1, P2: P, cacMa, phienBan: phienBan(cacMa), o15b: o15(1), C15: C15.idLichLamViec }));
}

function kiemThanNhac(ten, thu, L, lang, phong, tenA) {
  const html = thu.HTML || '';
  const tenPhong = lang === 'en' && phong.tenPhongEn ? phong.tenPhongEn : phong.tenPhong;
  const nhan = lang === 'en' ? ['Patient', 'Time', 'Doctor', 'Room', 'Booking code'] : ['Bệnh nhân', 'Thời gian', 'Bác sĩ', 'Phòng khám', 'Mã tra cứu'];
  const mong = [`<li>${nhan[0]}: <b>${esc(L.hoTen)}</b></li>`, `<li>${nhan[1]}: <b>${gioNgay(L.gioKhamDuKien, lang)}</b></li>`, `<li>${nhan[2]}: <b>${esc(tenA)}</b></li>`,
    `<li>${nhan[3]}: <b>${esc(tenPhong)}</b>`, `<li>${nhan[4]}: <b>${L.maTraCuu}</b></li>`, `<a href="${esc(L.linkPhieuKham)}"`,
    lang === 'en' ? 'You have an upcoming appointment' : 'Bạn có lịch khám sắp tới'];
  const thieu = mong.filter((m) => !html.includes(m));
  kiem(`${ten}: thân email nhắc [${lang}] đủ họ tên, giờ, bác sĩ, phòng, mã, link phiếu khám`, thieu.length === 0, 'thiếu: ' + J(thieu) + '\n' + html);
  kiem(`${ten}: không có lý do khám, số điện thoại, CCCD`, html.length > 0 && !html.includes(LY_DO) && !html.includes(L.soDienThoai) && !html.includes(L.cccd), html);
}

async function chang2() {
  const S = JSON.parse(fs.readFileSync(TEP, 'utf8'));
  const { X, tenA } = S;
  console.log('2.0 "Ban ngày": ' + moiTruong('BOOKING_REMINDER_FROM') + ' - ' + moiTruong('BOOKING_REMINDER_TO'));
  for (let i = 0; i < 40 && (await thuNhac()).length < 3; i++) await cho(1000);
  await cho(12000);
  const nhac = await thuNhac();
  kiem(`đúng 3 thư nhắc (x1, x2, x8): ${nhac.map((m) => m.Subject).join(' | ')}`, nhac.length === 3);

  console.log('2.1 Khách, đã xác nhận, mốc 20 ngày');
  let ds = await thuNhac(X.x1.email);
  const T1 = ds[0] ? await thuDay(ds[0]) : {};
  kiem(`x1: 1 thư, tiêu đề ${T1.Subject}`, ds.length === 1 && T1.Subject === `[eClinic] Nhắc lịch khám (mã ${X.x1.maTraCuu})`);
  kiemThanNhac('x1', T1, X.x1, 'vi', S.P1, tenA);
  kiem(`x1: mốc đã ghi = ${moc(X.x1.maTraCuu)}`, moc(X.x1.maTraCuu) === '28800:1');

  console.log('2.2 Tài khoản (EN), trong khoảng mốc 10 ngày: chỉ nhắc mốc gần nhất');
  ds = await thuNhac(EMAIL_BN);
  const T2 = ds[0] ? await thuDay(ds[0]) : {};
  kiem(`x2: 1 thư tới email tài khoản, tiếng Anh: ${T2.Subject}`, ds.length === 1 && T2.Subject === `[eClinic] Appointment reminder (code ${X.x2.maTraCuu})`);
  kiemThanNhac('x2', T2, X.x2, 'en', S.P2, tenA);
  kiem(`x2: mốc 10 ngày đã nhắc, mốc 20 ngày ghi nhận không nhắc = ${moc(X.x2.maTraCuu)}`, moc(X.x2.maTraCuu) === '14400:1,28800:0');
  const bn = (await dangNhap(EMAIL_BN, 'Demo@123')).accessToken;
  let vi = [], en = [];
  for (let i = 0; i < 10; i++) { vi = (await chuongCua(bn)).filter((t) => t.loai === 'NHAC_LICH_KHAM'); if (vi.length) break; await cho(1500); }
  en = (await chuongCua(bn, 'en')).filter((t) => t.loai === 'NHAC_LICH_KHAM');
  kiem('chuông của tài khoản: đúng 1 thông báo NHAC_LICH_KHAM', vi.length === 1 && en.length === 1, J(vi));
  const mongVi = `Nhắc lịch khám: ${X.x2.hoTen} có lịch khám với bác sĩ ${tenA} lúc ${gioNgay(X.x2.gioKhamDuKien)} tại ${S.P2.tenPhong} (mã ${X.x2.maTraCuu}).`;
  const mongEn = `Appointment reminder: ${X.x2.hoTen} has an appointment with Dr. ${tenA} at ${gioNgay(X.x2.gioKhamDuKien, 'en')}, ${S.P2.tenPhongEn || S.P2.tenPhong} (code ${X.x2.maTraCuu}).`;
  kiem(`[vi] ${(vi[0] || {}).noiDung}`, (vi[0] || {}).noiDung === mongVi, 'mong: ' + mongVi);
  kiem(`[en] ${(en[0] || {}).noiDung}`, (en[0] || {}).noiDung === mongEn, 'mong: ' + mongEn);
  kiem('thông báo nhắc có maPhieuKham của lịch hẹn', (vi[0] || {}).maPhieuKham === X.x2.maPhieuKham);

  console.log('2.3 Không nhắc');
  kiem(`x3 vừa đặt trong khoảng của cả 2 mốc: ghi nhận, không nhắc = ${moc(X.x3.maTraCuu)}`, moc(X.x3.maTraCuu) === '14400:0,28800:0' && (await thuNhac(X.x3.email)).length === 0);
  for (const [k, ten] of [['x4', 'đã hủy'], ['x5', 'bị từ chối'], ['x6', 'lịch cũ sau khi đổi'], ['x7', 'chờ đổi vì ca bị hủy'], ['x10', 'gần hơn khoảng cách tối thiểu']]) {
    kiem(`${k} ${ten}: không thư nhắc, không ghi mốc`, moc(X[k].maTraCuu) === '' && (await thuNhac(X[k].email)).length === 0, moc(X[k].maTraCuu));
  }
  kiem(`x6 lịch mới (vừa tạo khi đổi): ghi nhận, không nhắc = ${moc(X.x6moi.maTraCuu)}`, moc(X.x6moi.maTraCuu) === '28800:0');

  console.log('2.4 Còn chờ bác sĩ xác nhận; khách không email');
  ds = await thuNhac(X.x8.email);
  const T8 = ds[0] ? await thuDay(ds[0]) : {};
  kiem(`x8: ${T8.Subject}`, ds.length === 1 && T8.Subject === `[eClinic] Reminder: appointment not confirmed yet (code ${X.x8.maTraCuu})`);
  kiem('x8: thân email nói rõ chưa được xác nhận', (T8.HTML || '').includes('<b>has not confirmed it yet</b>') && (T8.HTML || '').includes(`<b>${X.x8.maTraCuu}</b>`), T8.HTML);
  kiem(`x9 không email, không tài khoản: mốc được ghi (${moc(X.x9.maTraCuu)}), không có dòng chờ gửi nào`,
    moc(X.x9.maTraCuu) === '28800:1' && sql(`select count(*) from su_kien_thong_bao s join lich_hen l on l.id_lich_hen = s.id_lich_hen where l.ma_tra_cuu = '${X.x9.maTraCuu}' and s.khoa_mau like '%nhacLich%'`) === '0');

  console.log('2.5 Không nhắc lại; không đụng lịch hẹn');
  await cho(10000);
  kiem('sau nhiều lần job chạy vẫn đúng 3 thư nhắc', (await thuNhac()).length === 3);
  kiem('version của mọi lịch hẹn không đổi', phienBan(S.cacMa) === S.phienBan, phienBan(S.cacMa) + '\n' + S.phienBan);

  console.log('2.6 Phase 4 còn nguyên');
  const email = `nhac11-${S.hauTo}@smoke.test`;
  const r = await goi('POST', '/api/booking/lich-hen', { body: { ...S.o15b, lyDoKham: LY_DO, benhNhan: { hoTen: 'Lê Thị Nhắc Mười Một', ngaySinh: '1992-07-07', gioiTinh: 'NU', soDienThoai: '09' + S.hauTo + '1', cccd: '0794' + S.hauTo + '1', email } } });
  kiem('đặt lịch mới', r.status === 201, J(r.json));
  let thu = [];
  for (let i = 0; i < 30; i++) { thu = (await tatCaThu()).filter((m) => m.To.some((t) => t.Address === email)); if (thu.length) break; await cho(1000); }
  kiem(`email xác nhận vẫn tới: ${(thu[0] || {}).Subject}`, thu.length === 1 && thu[0].Subject === `[eClinic] Đã nhận yêu cầu đặt lịch khám (mã ${r.json.duLieu.maTraCuu})`);
  const bsA = (await dangNhap('bacsi01@eclinic.local', 'Demo@123')).accessToken;
  let coChuong = false;
  for (let i = 0; i < 10 && !coChuong; i++) { coChuong = (await chuongCua(bsA)).some((t) => t.loai === 'LICH_HEN_MOI' && t.noiDung.includes(r.json.duLieu.maTraCuu)); if (!coChuong) await cho(1500); }
  kiem('bác sĩ vẫn nhận "lịch hẹn mới"', coChuong);
  kiem('bác sĩ không nhận thông báo nhắc lịch', !(await chuongCua(bsA)).some((t) => t.loai === 'NHAC_LICH_KHAM'));

  console.log('2.7 Log');
  const log = kiemLog();
  const dongNhac = log.split('\n').filter((d) => /nhắc|Nhắc/.test(d));
  kiem(`log job: ${dongNhac.map((d) => d.replace(/^.*?: /, '')).join(' | ')}`, dongNhac.some((d) => /Đã nhắc \d+ lịch hẹn sắp tới/.test(d)));
  kiem('log không có họ tên bệnh nhân hay địa chỉ email', !log.includes('Nhắc Một') && !log.includes('@smoke.test') && !log.includes(EMAIL_BN));
  const loiNoti = sh('logs', 'eclinic-notification-service').split('\n').filter((d) => / ERROR /.test(d));
  kiem('notification-service không có dòng ERROR', loiNoti.length === 0, loiNoti.slice(0, 3).join('\n'));
}

async function chang3() {
  const S = JSON.parse(fs.readFileSync(TEP, 'utf8'));
  const { X } = S;
  console.log('3.0 Mốc mới: ' + moiTruong('BOOKING_REMINDER_BEFORE'));
  for (let i = 0; i < 40 && (await thuNhac()).length < 5; i++) await cho(1000);
  await cho(10000);
  let ds = await thuNhac(X.x1.email);
  kiem(`x1: mốc thứ 2 đến hạn -> thư nhắc thứ 2 (tổng ${ds.length})`, ds.length === 2);
  kiem(`x1: mốc đã ghi = ${moc(X.x1.maTraCuu)}`, moc(X.x1.maTraCuu) === '23040:1,28800:1');
  kiem(`x2 đã được nhắc ở mốc gần hơn: mốc 16 ngày chỉ ghi nhận (${moc(X.x2.maTraCuu)}), vẫn 1 thư`, moc(X.x2.maTraCuu) === '14400:1,23040:0,28800:0' && (await thuNhac(EMAIL_BN)).length === 1);
  kiem(`x8: thư nhắc thứ 2 (${(await thuNhac(X.x8.email)).length})`, (await thuNhac(X.x8.email)).length === 2);
  kiem(`tổng số thư nhắc: ${(await thuNhac()).length}`, (await thuNhac()).length === 5);
  const bsA = (await dangNhap('bacsi01@eclinic.local', 'Demo@123')).accessToken;
  const yc = (await goi('GET', '/api/booking/bac-si/toi/lich-hen/yeu-cau?kichThuoc=100', { token: bsA })).json.duLieu.noiDung.find((l) => l.maTraCuu === X.x8.maTraCuu) || {};
  const r = await goi('POST', `/api/booking/bac-si/toi/lich-hen/${yc.id}/xac-nhan`, { token: bsA });
  kiem('bác sĩ xác nhận x8 sau khi đã nhắc', r.status === 200, J(r.json));
  await cho(12000);
  kiem('xác nhận xong không sinh thư nhắc nào nữa; không có lần nhắc thứ 3', (await thuNhac(X.x8.email)).length === 2 && (await thuNhac(X.x1.email)).length === 2 && (await thuNhac()).length === 5);
  kiem('mỗi (lịch hẹn, mốc) chỉ 1 dòng', sql('select count(*) from (select id_lich_hen, so_phut_truoc from nhac_lich_hen group by id_lich_hen, so_phut_truoc having count(*) > 1) t') === '0');
  kiemLog();
  const bn = (await dangNhap(EMAIL_BN, 'Demo@123')).accessToken;
  await goi('PUT', '/api/users/me/language', { token: bn, body: { ngonNgu: 'VI' } });
}

(async () => {
  if (CHANG === 1) await chang1(); else if (CHANG === 2) await chang2(); else if (CHANG === 3) await chang3(); else throw new Error('node temp/d9-p5-smoke.js <1|2|3>');
  console.log(`\nChặng ${CHANG}: ${dat} đạt, ${hong} hỏng`);
  process.exit(hong ? 1 : 0);
})().catch((e) => { console.error(e); process.exit(2); });
