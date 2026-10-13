// Smoke phase 6 (DOANTOTNGH-9): báo và nhắc tái khám. 3 chặng, booking-service được tạo lại giữa các chặng (temp/d9-p6-run.sh):
//   1 "ban đêm"  : giờ gửi KHÔNG chứa giờ hiện tại, nhắc trước 7 ngày -> ghi các lượt khám có ngày tái khám, chưa báo gì
//   2 "ban ngày" : giờ gửi chứa giờ hiện tại -> báo 1 lần; đổi ngày, xoá ngày, hẹn lại
//   3 nhắc trước 12 ngày: ngày tái khám +10 đến hạn nhắc -> nhắc đúng 1 lần, bỏ người đã tự đặt lịch cùng chuyên khoa
// Lượt khám chỉ ghi được khi giờ khám từ hôm nay trở về trước: đặt vào ca ngày +15 rồi lùi giờ của lượt khám 16 ngày bằng SQL.
// Stack eclinic-smoke, DB mới, Mailpit ở 127.0.0.1:18025.   node temp/d9-p6-smoke.js <1|2|3>
const { execFileSync } = require('child_process');
const fs = require('fs');
const CHANG = Number(process.argv[2]);
const TEP = 'temp/d9-p6-state.json';
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
const ngaySau = (n) => { const d = new Date(Date.now() + n * 86400000); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; };
const ddmm = (iso) => iso.slice(8, 10) + '/' + iso.slice(5, 7) + '/' + iso.slice(0, 4);
const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
const LA_TAI_KHAM = /tái khám|Follow-up/;
const LA_NHAC_TAI_KHAM = /Nhắc tái khám|Follow-up reminder/;
const LA_DOI_NGAY = /thay đổi|changed/;
const EMAIL_BN = 'benhnhan01@eclinic.local';
const CHAN_DOAN = 'Viem hong smoke-p6', GHI_CHU = 'Ghi chu rieng p6', THUOC = 'Thuoc smoke p6';
const D3 = ngaySau(3), D10 = ngaySau(10), D12 = ngaySau(12);

async function mp(duongDan) {
  const res = await fetch(MAILPIT + duongDan, { signal: AbortSignal.timeout(10000) });
  try { return await res.json(); } catch { return {}; }
}
const tatCaThu = async () => (await mp('/api/v1/messages?limit=500')).messages || [];
/** Thư tái khám gửi tới 1 địa chỉ (bỏ trống = mọi địa chỉ), cũ nhất trước. */
const thuTaiKham = async (diaChi, loc = LA_TAI_KHAM) => (await tatCaThu()).filter((m) => loc.test(m.Subject) && (!diaChi || m.To.some((t) => t.Address === diaChi))).reverse();
const thuDay = async (m) => mp('/api/v1/message/' + m.ID);
async function choThu(diaChi, soThu, loc) {
  for (let i = 0; i < 45 && (await thuTaiKham(diaChi, loc)).length < soThu; i++) await cho(1000);
  return thuTaiKham(diaChi, loc);
}
/** Dòng nhac_tai_kham của 1 lượt khám: "2026-10-22:0", "null:0", hoặc "" khi chưa có dòng. */
const trangThai = (maTraCuu) => sql(`select ifnull((select concat(ifnull(n.ngay_da_bao, 'null'), ':', n.da_nhac_truoc) from nhac_tai_kham n join ho_so_benh_an b on b.id_ho_so_benh_an = n.id_ho_so_benh_an join lich_hen l on l.id_lich_hen = b.id_lich_hen where l.ma_tra_cuu = '${maTraCuu}'), '')`);
const trong = (cacMa) => cacMa.map((m) => `'${m}'`).join(',');
const phienBan = (cacMa) => sql(`select ifnull(group_concat(concat(ma_tra_cuu, '=', version, '/', trang_thai) order by ma_tra_cuu), '') from lich_hen where ma_tra_cuu in (${trong(cacMa)})`);
const benhAnCua = (cacMa) => sql(`select ifnull(group_concat(concat(l.ma_tra_cuu, '=', b.chan_doan, '/', ifnull(b.ngay_tai_kham_de_xuat, 'null')) order by l.ma_tra_cuu), '') from ho_so_benh_an b join lich_hen l on l.id_lich_hen = b.id_lich_hen where l.ma_tra_cuu in (${trong(cacMa)})`);
const moiTruong = (ten) => sh('exec', 'eclinic-booking-service', 'printenv', ten);
const dangNhap = async (email, matKhau) => (await goi('POST', '/api/auth/login', { body: { email, matKhau } })).json.duLieu;
const chuongCua = async (token, lang) => (await goi('GET', '/api/notification/thong-bao?kichThuoc=100', { token, lang })).json.duLieu.noiDung;
function kiemLog() {
  for (const dichVu of ['booking-service', 'notification-service']) {
    const loi = sh('logs', 'eclinic-' + dichVu).split('\n').filter((d) => / ERROR /.test(d));
    kiem(dichVu + ' không có dòng ERROR', loi.length === 0, loi.slice(0, 4).join('\n'));
  }
}
const benhAn = (ngay, chanDoan = CHAN_DOAN) => ({ chanDoan, ghiChu: GHI_CHU, ngayTaiKhamDeXuat: ngay, donThuoc: [{ tenThuoc: THUOC, donVi: 'vien', lieuDung: '500mg', soLanMoiNgay: 2, soNgayDung: 5 }] });

function kiemThan(ten, thu, L, lang, ngay, tenA, lienKet) {
  const html = thu.HTML || '';
  const nhan = lang === 'en' ? ['Patient', 'Doctor', 'Recommended follow-up date', 'no appointment has been booked'] : ['Bệnh nhân', 'Bác sĩ', 'Ngày tái khám đề xuất', 'chưa có lịch hẹn nào được đặt'];
  const mong = [`<li>${nhan[0]}: <b>${esc(L.hoTen)}</b></li>`, `<li>${nhan[1]}: <b>${esc(tenA)}</b></li>`, `<li>${nhan[2]}: <b>${ddmm(ngay)}</b></li>`, `<a href="${esc(lienKet)}"`, nhan[3]];
  const thieu = mong.filter((m) => !html.includes(m));
  kiem(`${ten}: thân email [${lang}] đủ bệnh nhân, bác sĩ, ngày ${ddmm(ngay)}, link đặt lịch, câu "chưa có lịch hẹn"`, thieu.length === 0, 'thiếu: ' + J(thieu) + '\n' + html);
  kiem(`${ten}: không có chẩn đoán, ghi chú, tên thuốc, số điện thoại, CCCD`, html.length > 0 && ![CHAN_DOAN, GHI_CHU, THUOC, L.soDienThoai, L.cccd].some((x) => html.includes(x)), html);
}

async function chang1() {
  const hauTo = Date.now().toString().slice(-6);
  const admin = (await dangNhap(sh('exec', 'eclinic-identity-service', 'printenv', 'ADMIN_EMAIL'), sh('exec', 'eclinic-identity-service', 'printenv', 'ADMIN_PASSWORD'))).accessToken;
  const dnA = await dangNhap('bacsi01@eclinic.local', 'Demo@123');
  const bsA = dnA.accessToken, tenA = dnA.taiKhoan.hoTen;
  const bn = (await dangNhap(EMAIL_BN, 'Demo@123')).accessToken;
  let r;
  console.log('1.0 Chuẩn bị ("ban đêm": ' + moiTruong('BOOKING_REMINDER_FROM') + ' - ' + moiTruong('BOOKING_REMINDER_TO') + ', nhắc trước ' + moiTruong('FOLLOWUP_REMINDER_DAYS') + ' ngày)');
  kiem('V21 đã chạy', sql("select count(*) from flyway_schema_history where version = '21' and success = 1") === '1');
  await fetch(MAILPIT + '/api/v1/messages', { method: 'DELETE' });
  await goi('PUT', '/api/users/me/language', { token: bn, body: { ngonNgu: 'VI' } });
  const dsBacSi = (await goi('GET', '/api/catalog/bac-si?kichThuoc=100')).json.duLieu.noiDung;
  const A = dsBacSi.find((b) => b.hoTen === tenA);
  const dsPhong = (await goi('GET', '/api/catalog/quan-tri/phong-kham?kichThuoc=100', { token: admin })).json.duLieu.noiDung;
  const P1 = dsPhong.find((p) => p.idChuyenKhoa === A.idChuyenKhoa);
  r = await goi('POST', '/api/booking/quan-tri/lich-lam-viec', { token: admin, body: { idBacSi: A.id, idPhongKham: P1.id, ngay: ngaySau(15), gioBatDau: '17:00', gioKetThuc: '21:00', soLuotToiDaMoiGio: 6, thoiLuongLuotPhut: 10 } });
  kiem('xếp ca ngày +15 cho bác sĩ A', r.status === 201, J(r.json));
  const C15 = r.json.duLieu.daTao[0];
  const caCua = async (ngay, idBacSi) => (await goi('GET', `/api/booking/khung-gio?ngay=${ngay}&idBacSi=${idBacSi}`)).json.duLieu || [];
  const K15 = (await caCua(ngaySau(15), A.id)).find((c) => c.idLichLamViec === C15.idLichLamViec).khungGio;
  // Ca có sẵn của 1 bác sĩ KHÁC chuyên khoa, còn chỗ, trong 1-14 ngày tới (cho lịch "đã tự đặt nhưng khác chuyên khoa")
  let khacKhoa = null;
  for (const b of dsBacSi.filter((x) => x.idChuyenKhoa !== A.idChuyenKhoa)) {
    for (let n = 2; n <= 14 && !khacKhoa; n++) {
      for (const ca of await caCua(ngaySau(n), b.id)) {
        const k = ca.khungGio.find((x) => x.soChoConLai > 1);
        if (k) { khacKhoa = { idLichLamViec: ca.idLichLamViec, gioBatDauKhung: k.gioBatDau }; break; }
      }
    }
    if (khacKhoa) break;
  }
  kiem('có ca mẫu của bác sĩ khác chuyên khoa', !!khacKhoa);

  let soNguoi = 0;
  async function datLich(ten, cho_, { token, lang, email, nguoi } = {}) {
    let N = nguoi;
    if (!N) {
      soNguoi++;
      const so = String(soNguoi).padStart(2, '0');
      N = { hoTen: 'Lê Thị Tái ' + ten, soDienThoai: '03' + hauTo + so, cccd: '0793' + hauTo + so };
    }
    const body = { idLichLamViec: cho_.idLichLamViec, gioBatDauKhung: cho_.gioBatDauKhung, lyDoKham: 'Kham smoke-p6',
      benhNhan: { hoTen: N.hoTen, ngaySinh: '1992-07-07', gioiTinh: 'NU', soDienThoai: N.soDienThoai, cccd: N.cccd, email } };
    if (token) body.datChoBanThan = false;
    const kq = await goi('POST', '/api/booking/lich-hen', { token, lang, body });
    if (kq.status !== 201) { kiem('đặt lịch cho ' + N.hoTen, false, J(kq.json)); return {}; }
    const d = kq.json.duLieu;
    return { ...N, maTraCuu: d.maTraCuu, maPhieuKham: d.maPhieuKham, email: email || null,
      id: sql(`select id_lich_hen from lich_hen where ma_tra_cuu = '${d.maTraCuu}'`) };
  }
  const o = (viTri) => ({ idLichLamViec: C15.idLichLamViec, gioBatDauKhung: K15[viTri].gioBatDau });
  const g = (so) => `taikham${so}-${hauTo}@smoke.test`;

  console.log('1.1 Đặt 11 lịch hẹn, lùi giờ khám về hôm qua, bác sĩ ghi kết quả khám');
  const V = {};
  V.v1 = await datLich('Một', o(0), { token: bn, lang: 'vi' });          // tài khoản (VI), ngày +10
  V.v2 = await datLich('Hai', o(0), { lang: 'en', email: g(2) });        // khách EN, ngày +10
  V.v3 = await datLich('Ba', o(0), {});                                  // khách không email, ngày +10
  V.v4 = await datLich('Bốn', o(0), { email: g(4) });                    // không có ngày tái khám
  V.v5 = await datLich('Năm', o(0), { email: g(5) });                    // ngày +3 (đã trong khoảng nhắc trước)
  V.v6 = await datLich('Sáu', o(0), { email: g(6) });                    // +10, xoá rồi hẹn lại
  V.v7 = await datLich('Bảy', o(1), { email: g(7) });                    // +10, sẽ tự đặt lịch cùng chuyên khoa
  V.v8 = await datLich('Tám', o(1), { email: g(8) });                    // +10, sẽ tự đặt lịch khác chuyên khoa
  V.v9 = await datLich('Chín', o(1), { email: g(9) });                   // +10 -> +12 -> +10 -> chỉ sửa chẩn đoán
  V.v10 = await datLich('Mười', o(1), { email: g(10) });                 // +10, xoá ngày
  V.v11 = await datLich('Mười Một', o(1), { email: g(11) });             // không ngày; ghi sẵn ngày + dòng đã báo như V21
  const R = await datLich('Nhắc Lịch', o(3), { email: g(12) });          // hồi quy phase 5: lịch hẹn còn hiệu lực
  kiem('đặt đủ 12 lịch hẹn', Object.values(V).every((l) => l.maTraCuu) && !!R.maTraCuu, J(V));
  const cacMa = Object.values(V).map((l) => l.maTraCuu);
  sql(`update khung_gio_kham k join lich_hen l on l.id_khung_gio = k.id_khung_gio set k.gio_bat_dau = k.gio_bat_dau - interval 16 day, k.gio_ket_thuc = k.gio_ket_thuc - interval 16 day where l.ma_tra_cuu in (${trong(cacMa)})`);
  sql(`update lich_hen set ngay_tao = ngay_tao - interval 40 day where ma_tra_cuu = '${R.maTraCuu}'`);
  const ngayCua = { v1: D10, v2: D10, v3: D10, v4: null, v5: D3, v6: D10, v7: D10, v8: D10, v9: D10, v10: D10, v11: null };
  const hongGhi = [];
  for (const [k, L] of Object.entries(V)) {
    r = await goi('POST', `/api/medical/bac-si/toi/lich-hen/${L.id}/benh-an`, { token: bsA, body: benhAn(ngayCua[k]) });
    if (r.status !== 201) hongGhi.push(k + ' ' + r.status + ' ' + J(r.json));
  }
  kiem('bác sĩ ghi kết quả 11 lượt khám', hongGhi.length === 0, hongGhi.join('\n'));
  sql(`start transaction; update ho_so_benh_an set ngay_tai_kham_de_xuat = '${D10}' where id_lich_hen = ${V.v11.id}; insert into nhac_tai_kham (id_ho_so_benh_an, ngay_da_bao, da_nhac_truoc) select id_ho_so_benh_an, ngay_tai_kham_de_xuat, 0 from ho_so_benh_an where id_lich_hen = ${V.v11.id}; commit;`);
  kiem('v11: ngày tái khám + dòng "đã báo" ghi sẵn bằng SQL (như V21 làm với dữ liệu cũ)', trangThai(V.v11.maTraCuu) === D10 + ':0', trangThai(V.v11.maTraCuu));

  console.log('1.2 Ngoài giờ gửi: chưa báo gì');
  await cho(14000);
  kiem('chưa có thư tái khám nào', (await thuTaiKham()).length === 0);
  kiem('nhac_tai_kham chỉ có dòng ghi sẵn của v11', sql('select count(*) from nhac_tai_kham') === '1');
  kiem('email xác nhận đặt lịch (phase 4) vẫn đi', (await tatCaThu()).filter((m) => m.To.some((t) => t.Address === R.email)).length === 1);
  kiemLog();
  fs.writeFileSync(TEP, J({ hauTo, V, R, tenA, cacMa, o3: o(3), khacKhoa, soLichHen: sql('select count(*) from lich_hen') }));
}

async function chang2() {
  const S = JSON.parse(fs.readFileSync(TEP, 'utf8'));
  const { V, R, tenA } = S;
  const bsA = (await dangNhap('bacsi01@eclinic.local', 'Demo@123')).accessToken;
  const bn = (await dangNhap(EMAIL_BN, 'Demo@123')).accessToken;
  const lienKet = moiTruong('FRONTEND_URL') + moiTruong('FOLLOWUP_BOOKING_PATH');
  let r, ds;
  console.log('2.0 "Ban ngày": ' + moiTruong('BOOKING_REMINDER_FROM') + ' - ' + moiTruong('BOOKING_REMINDER_TO') + ' -> báo các ngày tái khám đã ghi ban đêm');
  for (let i = 0; i < 45 && (await thuTaiKham()).length < 8; i++) await cho(1000);
  await cho(12000);
  const nhan = { v1: EMAIL_BN, v2: V.v2.email, v5: V.v5.email, v6: V.v6.email, v7: V.v7.email, v8: V.v8.email, v9: V.v9.email, v10: V.v10.email };
  const tatCa = await thuTaiKham();
  kiem('đúng 8 thư báo tái khám (v1 v2 v5 v6 v7 v8 v9 v10), không thư "đổi ngày" / "nhắc"', tatCa.length === 8 && !tatCa.some((m) => LA_DOI_NGAY.test(m.Subject) || LA_NHAC_TAI_KHAM.test(m.Subject)), J(tatCa.map((m) => [m.To[0].Address, m.Subject])));
  for (const [k, diaChi] of Object.entries(nhan)) {
    ds = await thuTaiKham(diaChi);
    if (ds.length !== 1) kiem(`${k}: 1 thư báo tới ${diaChi}`, false, J(ds.map((m) => m.Subject)));
  }
  kiem('v4 (không có ngày) và v11 (đã ghi sẵn là đã báo): không có thư', (await thuTaiKham(V.v4.email)).length === 0 && (await thuTaiKham(V.v11.email)).length === 0);
  ds = await thuTaiKham(EMAIL_BN);
  kiem('v1: tiêu đề tiếng Việt', ds.length === 1 && ds[0].Subject === '[eClinic] Bác sĩ hẹn tái khám', J(ds.map((m) => m.Subject)));
  if (ds.length) kiemThan('v1', await thuDay(ds[0]), V.v1, 'vi', D10, tenA, lienKet);
  ds = await thuTaiKham(V.v2.email);
  kiem('v2 (khách đặt bằng tiếng Anh): tiêu đề tiếng Anh', ds.length === 1 && ds[0].Subject === '[eClinic] Follow-up visit recommended', J(ds.map((m) => m.Subject)));
  if (ds.length) kiemThan('v2', await thuDay(ds[0]), V.v2, 'en', D10, tenA, lienKet);
  kiem('trạng thái: v1 đã báo +10 chưa nhắc', trangThai(V.v1.maTraCuu) === D10 + ':0', trangThai(V.v1.maTraCuu));
  kiem('trạng thái: v3 (khách không email) vẫn được ghi là đã báo', trangThai(V.v3.maTraCuu) === D10 + ':0', trangThai(V.v3.maTraCuu));
  kiem('trạng thái: v4 không có dòng', trangThai(V.v4.maTraCuu) === '');
  kiem('trạng thái: v5 (ngày +3, trong khoảng 7 ngày) coi như đã nhắc', trangThai(V.v5.maTraCuu) === D3 + ':1', trangThai(V.v5.maTraCuu));
  const chuongVi = (await chuongCua(bn, 'vi')).filter((t) => t.loai === 'NHAC_TAI_KHAM');
  const chuongEn = (await chuongCua(bn, 'en')).filter((t) => t.loai === 'NHAC_TAI_KHAM');
  kiem('chuông của tài khoản đặt v1: 1 thông báo NHAC_TAI_KHAM, mang mã phiếu khám của lượt khám', chuongVi.length === 1 && chuongVi[0].maPhieuKham === V.v1.maPhieuKham, J(chuongVi));
  kiem('  tiếng Việt: có bác sĩ, bệnh nhân, ngày', chuongVi.length === 1 && [tenA, V.v1.hoTen, ddmm(D10), 'tái khám', 'chưa có lịch hẹn'].every((x) => chuongVi[0].noiDung.includes(x)), J(chuongVi));
  kiem('  tiếng Anh: cùng thông báo đọc bằng tiếng Anh, họ tên giữ nguyên', chuongEn.length === 1 && [tenA, V.v1.hoTen, ddmm(D10), 'follow-up visit', 'no appointment has been booked'].every((x) => chuongEn[0].noiDung.includes(x)), J(chuongEn));
  kiem('không có lịch hẹn nào được tạo', sql('select count(*) from lich_hen') === S.soLichHen);

  console.log('2.1 Bác sĩ đổi ngày, đổi về ngày cũ, chỉ sửa chẩn đoán (v9)');
  const sua = (L, ngay, chanDoan) => goi('PUT', `/api/medical/bac-si/toi/lich-hen/${L.id}/benh-an`, { token: bsA, body: benhAn(ngay, chanDoan) });
  r = await sua(V.v9, D12);
  kiem('sửa ngày tái khám v9 -> +12', r.status === 200, J(r.json));
  ds = await choThu(V.v9.email, 2);
  kiem('v9: thư thứ 2 là "ngày tái khám đã thay đổi"', ds.length === 2 && ds[1].Subject === '[eClinic] Ngày tái khám đã thay đổi', J(ds.map((m) => m.Subject)));
  if (ds.length === 2) kiemThan('v9 đổi ngày', await thuDay(ds[1]), V.v9, 'vi', D12, tenA, lienKet);
  r = await sua(V.v9, D10);
  ds = await choThu(V.v9.email, 3);
  kiem('v9 đổi về ngày cũ +10: được báo lại (thư thứ 3, "đã thay đổi")', r.status === 200 && ds.length === 3 && LA_DOI_NGAY.test(ds[2].Subject), J(ds.map((m) => m.Subject)));
  if (ds.length === 3) kiem('  thư thứ 3 ghi ngày +10', ((await thuDay(ds[2])).HTML || '').includes(ddmm(D10)));
  r = await sua(V.v9, D10, CHAN_DOAN + ' sua');
  console.log('2.2 Bác sĩ xoá ngày (v10, v6), rồi hẹn lại (v6)');
  const r10 = await sua(V.v10, null), r6 = await sua(V.v6, null);
  kiem('sửa chẩn đoán v9, xoá ngày v10 và v6: 200', r.status === 200 && r10.status === 200 && r6.status === 200, J([r.json, r10.json, r6.json]));
  await cho(14000);
  kiem('v9 chỉ sửa chẩn đoán: không có thư mới', (await thuTaiKham(V.v9.email)).length === 3);
  kiem('v10, v6 xoá ngày: không có thư mới', (await thuTaiKham(V.v10.email)).length === 1 && (await thuTaiKham(V.v6.email)).length === 1);
  kiem('v10, v6: trạng thái đã quên ngày', trangThai(V.v10.maTraCuu) === 'null:0' && trangThai(V.v6.maTraCuu) === 'null:0', trangThai(V.v10.maTraCuu) + ' ' + trangThai(V.v6.maTraCuu));
  r = await sua(V.v6, D10);
  ds = await choThu(V.v6.email, 2);
  kiem('v6 hẹn lại đúng ngày cũ: báo như lần đầu (không phải "đổi ngày")', r.status === 200 && ds.length === 2 && ds[1].Subject === '[eClinic] Bác sĩ hẹn tái khám', J(ds.map((m) => m.Subject)));

  console.log('2.3 v7 tự đặt lịch cùng chuyên khoa, v8 đặt khác chuyên khoa; hồi quy phase 4 và 5');
  const datLai = (L, cho_) => goi('POST', '/api/booking/lich-hen', { body: { idLichLamViec: cho_.idLichLamViec, gioBatDauKhung: cho_.gioBatDauKhung, lyDoKham: 'Tai kham smoke-p6',
    benhNhan: { hoTen: L.hoTen, ngaySinh: '1992-07-07', gioiTinh: 'NU', soDienThoai: L.soDienThoai, cccd: L.cccd, email: L.email } } });
  r = await datLai(V.v7, S.o3);
  kiem('v7 đặt lịch mới với bác sĩ A', r.status === 201, J(r.json));
  const hoSo = (cccd) => sql(`select concat(count(distinct l.id_ho_so_benh_nhan), '/', count(*)) from lich_hen l join ho_so_benh_nhan h on h.id_ho_so_benh_nhan = l.id_ho_so_benh_nhan where h.cccd = '${cccd}'`);
  kiem('  cùng hồ sơ bệnh nhân với lượt khám cũ (1 hồ sơ, 2 lịch hẹn)', hoSo(V.v7.cccd) === '1/2', hoSo(V.v7.cccd));
  r = await datLai(V.v8, S.khacKhoa);
  kiem('v8 đặt lịch mới với bác sĩ khác chuyên khoa', r.status === 201 && hoSo(V.v8.cccd) === '1/2', J(r.json) + hoSo(V.v8.cccd));
  for (let i = 0; i < 30 && (await tatCaThu()).filter((m) => m.To.some((t) => t.Address === V.v7.email) && !LA_TAI_KHAM.test(m.Subject)).length < 2; i++) await cho(1000);
  kiem('phase 4: lịch mới của v7 có email xác nhận', (await tatCaThu()).filter((m) => m.To.some((t) => t.Address === V.v7.email) && !LA_TAI_KHAM.test(m.Subject)).length === 2);
  ds = (await tatCaThu()).filter((m) => m.To.some((t) => t.Address === R.email) && /Nhắc lịch khám/.test(m.Subject));
  kiem('phase 5: lịch hẹn còn hiệu lực (đặt từ lâu) vẫn được nhắc lịch khám 1 lần', ds.length === 1, J(ds.map((m) => m.Subject)));
  await cho(10000);
  const tong = (await thuTaiKham()).length;
  kiem('tổng thư tái khám tới giờ: 8 + 2 (v9) + 1 (v6) = 11, chưa có thư nhắc', tong === 11 && (await thuTaiKham(null, LA_NHAC_TAI_KHAM)).length === 0, String(tong));
  kiemLog();
  fs.writeFileSync(TEP, J({ ...S, phienBan: phienBan(S.cacMa), benhAn: benhAnCua(S.cacMa), soLichHen2: sql('select count(*) from lich_hen') }));
}

async function chang3() {
  const S = JSON.parse(fs.readFileSync(TEP, 'utf8'));
  const { V, tenA } = S;
  const bn = (await dangNhap(EMAIL_BN, 'Demo@123')).accessToken;
  const lienKet = moiTruong('FRONTEND_URL') + moiTruong('FOLLOWUP_BOOKING_PATH');
  let ds;
  console.log('3.0 Nhắc trước ' + moiTruong('FOLLOWUP_REMINDER_DAYS') + ' ngày: ngày tái khám +10 đến hạn nhắc');
  for (let i = 0; i < 45 && (await thuTaiKham(null, LA_NHAC_TAI_KHAM)).length < 6; i++) await cho(1000);
  await cho(14000);
  const nhac = await thuTaiKham(null, LA_NHAC_TAI_KHAM);
  const mong = [EMAIL_BN, V.v2.email, V.v6.email, V.v8.email, V.v9.email, V.v11.email].sort();
  kiem('đúng 6 thư nhắc: v1, v2, v6 (hẹn lại), v8 (lịch khác chuyên khoa), v9, v11 (dữ liệu ghi sẵn)', J(nhac.map((m) => m.To[0].Address).sort()) === J(mong), J(nhac.map((m) => [m.To[0].Address, m.Subject])));
  kiem('không nhắc v5 (hẹn trong khoảng), v7 (đã đặt lịch cùng chuyên khoa), v10 (đã xoá ngày), v4', [V.v5, V.v7, V.v10, V.v4].every((L) => !nhac.some((m) => m.To[0].Address === L.email)));
  ds = nhac.filter((m) => m.To[0].Address === V.v2.email);
  kiem('v2: thư nhắc tiếng Anh', ds.length === 1 && ds[0].Subject === '[eClinic] Follow-up reminder', J(ds.map((m) => m.Subject)));
  if (ds.length) kiemThan('v2 nhắc', await thuDay(ds[0]), V.v2, 'en', D10, tenA, lienKet);
  ds = nhac.filter((m) => m.To[0].Address === EMAIL_BN);
  if (ds.length) kiemThan('v1 nhắc', await thuDay(ds[0]), V.v1, 'vi', D10, tenA, lienKet);
  kiem('trạng thái: v1, v3, v7 đã xử lý lần nhắc; v10 vẫn là đã quên', [V.v1, V.v3, V.v7].every((L) => trangThai(L.maTraCuu) === D10 + ':1') && trangThai(V.v10.maTraCuu) === 'null:0',
    [V.v1, V.v3, V.v7, V.v10].map((L) => trangThai(L.maTraCuu)).join(' '));
  const chuong = (await chuongCua(bn, 'vi')).filter((t) => t.loai === 'NHAC_TAI_KHAM');
  kiem('chuông của tài khoản đặt v1: thêm 1 thông báo "Nhắc tái khám" (tổng 2)', chuong.length === 2 && chuong.filter((t) => t.noiDung.startsWith('Nhắc tái khám')).length === 1, J(chuong));
  await cho(12000);
  kiem('các lần chạy sau không gửi thêm: vẫn 6 thư nhắc, 17 thư tái khám', (await thuTaiKham(null, LA_NHAC_TAI_KHAM)).length === 6 && (await thuTaiKham()).length === 17, String((await thuTaiKham()).length));
  kiem('job không đổi lich_hen của các lượt khám (version, trạng thái)', phienBan(S.cacMa) === S.phienBan, phienBan(S.cacMa) + '\n' + S.phienBan);
  kiem('job không đổi ho_so_benh_an', benhAnCua(S.cacMa) === S.benhAn, benhAnCua(S.cacMa) + '\n' + S.benhAn);
  kiem('không có lịch hẹn nào do job tạo', sql('select count(*) from lich_hen') === S.soLichHen2);
  kiemLog();
  const dongJob = sh('logs', 'eclinic-booking-service').split('\n').filter((d) => /tái khám/.test(d));
  kiem(`log của job tái khám (${dongJob.length} dòng) không có họ tên, địa chỉ email`, dongJob.length > 0 && !dongJob.some((d) => /Lê Thị|@/.test(d)), dongJob.slice(0, 3).join('\n'));
}

(async () => {
  try {
    await [null, chang1, chang2, chang3][CHANG]();
  } catch (e) {
    hong++; console.log('  HỎNG (lỗi script) ' + (e && e.stack || e));
  }
  console.log(`Chặng ${CHANG}: ${dat} đạt, ${hong} hỏng`);
  process.exit(hong ? 1 : 0);
})();
