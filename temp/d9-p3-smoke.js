// Smoke phase 3 (DOANTOTNGH-9): thông báo theo mẫu 2 ngôn ngữ + email tài khoản theo ngôn ngữ của tài khoản.
// Chạy trên stack eclinic-smoke (MAIL_MODE=console), DB mới: node temp/d9-p3-smoke.js
const { execFileSync } = require('child_process');
const GOC = 'http://127.0.0.1:8080';
const CO_DAU = /[àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđĐ]/;
let dat = 0, hong = 0;
function kiem(ten, dung, chiTiet) {
  if (dung) { dat++; console.log('  ok   ' + ten); } else { hong++; console.log('  HỎNG ' + ten + (chiTiet ? '\n       ' + String(chiTiet).slice(0, 900) : '')); }
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
const hhmm = (isoGio) => isoGio.slice(11, 16);
const gioNgay = (iso, lang) => `${hhmm(iso)} ${lang === 'en' ? 'on' : 'ngày'} ${ddmm(iso)}`;
const CHUONG = '/api/notification/thong-bao';

async function chuong(token, lang) {
  const r = await goi('GET', CHUONG + '?kichThuoc=100', { token, lang });
  return r.json.duLieu ? r.json.duLieu.noiDung : [];
}
/** Chờ tới khi chuông có thông báo thoả điều kiện (job gửi 5 giây 1 lần); trả cặp { vi, en } của cùng 1 id. */
async function choThongBao(token, dieuKien) {
  for (let i = 0; i < 16; i++) {
    const vi = (await chuong(token)).find(dieuKien);
    if (vi) {
      const en = (await chuong(token, 'en')).find((t) => t.id === vi.id);
      return { vi, en: en || {} };
    }
    await cho(2000);
  }
  return { vi: {}, en: {} };
}
/** So cả 2 câu với câu mong đợi. */
function kiemCau(ten, cap, mongVi, mongEn) {
  kiem(`${ten} [vi] ${cap.vi.noiDung}`, cap.vi.noiDung === mongVi, 'mong: ' + mongVi);
  kiem(`${ten} [en] ${cap.en.noiDung}`, cap.en.noiDung === mongEn && cap.en.loai === cap.vi.loai, 'mong: ' + mongEn);
}

(async () => {
  const dangNhap = async (email, matKhau) => (await goi('POST', '/api/auth/login', { body: { email, matKhau } })).json.duLieu;
  const adminEmail = sh('exec', 'eclinic-identity-service', 'printenv', 'ADMIN_EMAIL');
  const admin = (await dangNhap(adminEmail, sh('exec', 'eclinic-identity-service', 'printenv', 'ADMIN_PASSWORD'))).accessToken;
  const dnA = await dangNhap('bacsi01@eclinic.local', 'Demo@123');
  const dnB = await dangNhap('bacsi02@eclinic.local', 'Demo@123');
  const dnBn = await dangNhap('benhnhan01@eclinic.local', 'Demo@123');
  const bsA = dnA.accessToken, bsB = dnB.accessToken, bn = dnBn.accessToken;
  const tenA = dnA.taiKhoan.hoTen, tenB = dnB.taiKhoan.hoTen;
  let r;

  console.log('0. Chuẩn bị');
  kiem('V18 đã chạy', sql("select count(*) from flyway_schema_history where version = '18' and success = 1") === '1');
  const dsBs = (await goi('GET', '/api/catalog/bac-si?kichThuoc=100')).json.duLieu.noiDung;
  const A = dsBs.find((b) => b.hoTen === tenA), B = dsBs.find((b) => b.hoTen === tenB);
  kiem(`2 bác sĩ cùng chuyên khoa: ${tenA} (#${A && A.id}), ${tenB} (#${B && B.id})`, A && B && A.idChuyenKhoa === B.idChuyenKhoa, J([A, B]));
  const dsPhong = (await goi('GET', '/api/catalog/quan-tri/phong-kham?kichThuoc=100', { token: admin })).json.duLieu.noiDung;
  const P1 = dsPhong.find((p) => p.idChuyenKhoa === A.idChuyenKhoa);
  const tenP2 = 'Phòng thử ' + hauTo, tenP2En = 'Smoke room ' + hauTo;
  r = await goi('POST', '/api/catalog/quan-tri/phong-kham', { token: admin, body: { tenPhong: tenP2, tenPhongEn: tenP2En, tang: 'Tầng 9', tangEn: 'Floor 9', idChuyenKhoa: A.idChuyenKhoa } });
  kiem('thêm phòng thứ 2 của chuyên khoa (có tên tiếng Anh)', r.status === 201, J(r.json));
  const P2 = r.json.duLieu;
  const tenP3 = 'Phòng chỉ Việt ' + hauTo;
  r = await goi('POST', '/api/catalog/quan-tri/phong-kham', { token: admin, body: { tenPhong: tenP3, idChuyenKhoa: A.idChuyenKhoa } });
  const P3 = r.json.duLieu;
  kiem('thêm phòng thứ 3 không có tên tiếng Anh, không có tầng', r.status === 201 && P3.tenPhongEn === null, J(r.json));

  const CA = '/api/booking/quan-tri/lich-lam-viec';
  const D1 = ngaySau(15), D2 = ngaySau(16), D2c = ngaySau(18);
  const taoCa = (idBacSi, idPhongKham, ngay, gioBatDau, gioKetThuc, lapLai) => goi('POST', CA, { token: admin, body: { idBacSi, idPhongKham, ngay, gioBatDau, gioKetThuc, soLuotToiDaMoiGio: 6, thoiLuongLuotPhut: 10, lapLai } });

  console.log('1. Xếp ca -> chuông của bác sĩ');
  r = await taoCa(A.id, P2.id, D1, '18:00', '20:00');
  kiem('xếp 1 ca cho bác sĩ A', r.status === 201 && r.json.duLieu.daTao.length === 1, J(r.json));
  const C1 = r.json.duLieu.daTao[0];
  let cap = await choThongBao(bsA, (t) => t.loai === 'CA_LAM_VIEC_THAY_DOI' && t.noiDung.includes(ddmm(D1)));
  kiemCau('ca mới', cap, `Bạn được xếp ca làm việc mới: 18:00-20:00 ngày ${ddmm(D1)}, phòng ${tenP2}.`, `You have a new shift: 18:00-20:00 on ${ddmm(D1)}, ${tenP2En}.`);
  kiem('dạng response của chuông không đổi', Object.keys(cap.vi).join() === 'id,loai,noiDung,daDoc,ngayTao,idLichHen,maPhieuKham,idYeuCau', Object.keys(cap.vi).join());

  r = await taoCa(A.id, P2.id, D2, '18:00', '19:00', { cacThu: [1, 2, 3, 4, 5, 6, 7], denNgay: D2c });
  const N = r.status === 201 ? r.json.duLieu.daTao : [];
  kiem(`xếp ca lặp lại -> ${N.length} ca`, N.length === 3, J(r.json));
  cap = await choThongBao(bsA, (t) => t.loai === 'CA_LAM_VIEC_THAY_DOI' && t.noiDung.includes('ca đầu'));
  kiemCau('nhiều ca mới', cap,
    `Bạn được xếp 3 ca làm việc mới, ca đầu 18:00-19:00 ngày ${ddmm(D2)}, ca cuối 18:00-19:00 ngày ${ddmm(D2c)}, phòng ${tenP2}.`,
    `You have 3 new shifts, the first 18:00-19:00 on ${ddmm(D2)}, the last 18:00-19:00 on ${ddmm(D2c)}, ${tenP2En}.`);
  r = await taoCa(B.id, P1.id, D1, '13:00', '15:00');
  kiem('xếp 1 ca cho bác sĩ B (phòng mẫu)', r.status === 201, J(r.json));
  const CB = r.json.duLieu.daTao[0];

  // ----- Đặt lịch -----
  let soNguoi = 0;
  async function datLich(idLichLamViec, idBacSi, ngay, viTriKhung = 0) {
    const ca = (await goi('GET', `/api/booking/khung-gio?ngay=${ngay}&idBacSi=${idBacSi}`)).json.duLieu.find((c) => c.idLichLamViec === idLichLamViec);
    soNguoi++;
    const hoTen = 'Nguyễn Thị Thử ' + ['Một', 'Hai', 'Ba', 'Bốn', 'Năm', 'Sáu', 'Bảy'][soNguoi - 1];
    const body = {
      idLichLamViec, gioBatDauKhung: ca.khungGio[viTriKhung].gioBatDau, lyDoKham: 'Khám tổng quát (smoke phase 3)', datChoBanThan: false,
      benhNhan: { hoTen, ngaySinh: '1990-05-05', gioiTinh: 'NU', soDienThoai: '03' + hauTo + soNguoi, cccd: '0791' + hauTo + soNguoi },
    };
    const kq = await goi('POST', '/api/booking/lich-hen', { token: bn, body });
    if (kq.status !== 201) { kiem('đặt lịch cho ' + hoTen, false, J(kq.json)); return {}; }
    return { ...kq.json.duLieu, hoTen };
  }
  const idLichHen = async (token, maTraCuu) => {
    const ds = (await goi('GET', '/api/booking/bac-si/toi/lich-hen/yeu-cau?kichThuoc=100', { token })).json.duLieu.noiDung;
    const dong = ds.find((l) => l.maTraCuu === maTraCuu);
    return dong ? dong.id : null;
  };

  console.log('2. Đặt lịch -> bác sĩ; xác nhận / từ chối -> bệnh nhân');
  const L1 = await datLich(C1.idLichLamViec, A.id, D1);
  cap = await choThongBao(bsA, (t) => t.loai === 'LICH_HEN_MOI' && t.noiDung.includes(L1.maTraCuu));
  kiemCau('lịch hẹn mới', cap, `Yêu cầu đặt lịch mới: bệnh nhân ${L1.hoTen}, ${gioNgay(L1.gioKhamDuKien)} (mã ${L1.maTraCuu}).`,
    `New booking request: patient ${L1.hoTen}, ${gioNgay(L1.gioKhamDuKien, 'en')} (code ${L1.maTraCuu}).`);
  kiem('bác sĩ: có idLichHen, không có maPhieuKham', cap.vi.idLichHen > 0 && cap.vi.maPhieuKham === null, J(cap.vi));
  r = await goi('POST', `/api/booking/bac-si/toi/lich-hen/${await idLichHen(bsA, L1.maTraCuu)}/xac-nhan`, { token: bsA });
  kiem('bác sĩ xác nhận L1', r.status === 200, J(r.json));
  cap = await choThongBao(bn, (t) => t.loai === 'LICH_HEN_DA_XAC_NHAN' && t.noiDung.includes(L1.maTraCuu));
  kiemCau('đã xác nhận', cap, `Bác sĩ ${tenA} đã xác nhận lịch khám của ${L1.hoTen} lúc ${gioNgay(L1.gioKhamDuKien)} (mã ${L1.maTraCuu}).`,
    `Dr. ${tenA} confirmed the appointment of ${L1.hoTen} at ${gioNgay(L1.gioKhamDuKien, 'en')} (code ${L1.maTraCuu}).`);
  kiem('bệnh nhân: có maPhieuKham', cap.vi.maPhieuKham === L1.maPhieuKham && cap.en.maPhieuKham === L1.maPhieuKham, J(cap.vi));

  const L2 = await datLich(C1.idLichLamViec, A.id, D1);
  const lyDoTuChoi = "Bác sĩ bận hội chẩn, bệnh nhân vui lòng chọn 'ca khác' {0}";
  r = await goi('POST', `/api/booking/bac-si/toi/lich-hen/${await idLichHen(bsA, L2.maTraCuu)}/tu-choi`, { token: bsA, body: { lyDo: lyDoTuChoi } });
  kiem('bác sĩ từ chối L2', r.status === 200, J(r.json));
  cap = await choThongBao(bn, (t) => t.loai === 'LICH_HEN_BI_TU_CHOI' && t.noiDung.includes(L2.maTraCuu));
  kiemCau('bị từ chối (lý do gõ có dấu nháy và {0}, giữ nguyên văn)', cap,
    `Bác sĩ ${tenA} đã từ chối lịch khám của ${L2.hoTen} lúc ${gioNgay(L2.gioKhamDuKien)} (mã ${L2.maTraCuu}). Lý do: ${lyDoTuChoi}`,
    `Dr. ${tenA} declined the appointment of ${L2.hoTen} at ${gioNgay(L2.gioKhamDuKien, 'en')} (code ${L2.maTraCuu}). Reason: ${lyDoTuChoi}`);

  console.log('3. Bệnh nhân hủy / đổi lịch -> bác sĩ');
  const L3 = await datLich(C1.idLichLamViec, A.id, D1);
  r = await goi('POST', `/api/booking/lich-hen/cua-toi/${L3.maPhieuKham}/huy`, { token: bn, body: { lyDo: 'Có việc đột xuất' } });
  kiem('hủy L3 có lý do', r.status === 200, J(r.json));
  cap = await choThongBao(bsA, (t) => t.loai === 'LICH_HEN_DA_HUY' && t.noiDung.includes(L3.maTraCuu));
  kiemCau('bệnh nhân hủy, có lý do', cap, `Bệnh nhân ${L3.hoTen} đã hủy lịch khám lúc ${gioNgay(L3.gioKhamDuKien)} (mã ${L3.maTraCuu}). Lý do: Có việc đột xuất`,
    `Patient ${L3.hoTen} cancelled the appointment at ${gioNgay(L3.gioKhamDuKien, 'en')} (code ${L3.maTraCuu}). Reason: Có việc đột xuất`);
  const L4 = await datLich(C1.idLichLamViec, A.id, D1);
  r = await goi('POST', `/api/booking/lich-hen/cua-toi/${L4.maPhieuKham}/huy`, { token: bn });
  kiem('hủy L4 không lý do', r.status === 200, J(r.json));
  cap = await choThongBao(bsA, (t) => t.loai === 'LICH_HEN_DA_HUY' && t.noiDung.includes(L4.maTraCuu));
  kiemCau('bệnh nhân hủy, không lý do', cap, `Bệnh nhân ${L4.hoTen} đã hủy lịch khám lúc ${gioNgay(L4.gioKhamDuKien)} (mã ${L4.maTraCuu}).`,
    `Patient ${L4.hoTen} cancelled the appointment at ${gioNgay(L4.gioKhamDuKien, 'en')} (code ${L4.maTraCuu}).`);

  const khungC1 = (await goi('GET', `/api/booking/khung-gio?ngay=${D1}&idBacSi=${A.id}`)).json.duLieu.find((c) => c.idLichLamViec === C1.idLichLamViec).khungGio;
  r = await goi('POST', `/api/booking/lich-hen/cua-toi/${L1.maPhieuKham}/doi-lich`, { token: bn, body: { idLichLamViec: C1.idLichLamViec, gioBatDauKhung: khungC1[1].gioBatDau } });
  kiem('đổi L1 sang khung giờ khác, cùng bác sĩ', r.status === 201, J(r.json));
  const L1b = { ...r.json.duLieu, hoTen: L1.hoTen };
  cap = await choThongBao(bsA, (t) => t.loai === 'LICH_HEN_DA_DOI' && t.noiDung.includes(L1b.maTraCuu));
  kiemCau('đổi lịch cùng bác sĩ', cap,
    `Bệnh nhân ${L1.hoTen} đã đổi lịch khám từ ${gioNgay(L1.gioKhamDuKien)} sang ${gioNgay(L1b.gioKhamDuKien)} (mã mới ${L1b.maTraCuu}), chờ bạn xác nhận.`,
    `Patient ${L1.hoTen} moved the appointment from ${gioNgay(L1.gioKhamDuKien, 'en')} to ${gioNgay(L1b.gioKhamDuKien, 'en')} (new code ${L1b.maTraCuu}); it is waiting for your confirmation.`);

  const L5 = await datLich(C1.idLichLamViec, A.id, D1);
  const khungCB = (await goi('GET', `/api/booking/khung-gio?ngay=${D1}&idBacSi=${B.id}`)).json.duLieu.find((c) => c.idLichLamViec === CB.idLichLamViec).khungGio;
  r = await goi('POST', `/api/booking/lich-hen/cua-toi/${L5.maPhieuKham}/doi-lich`, { token: bn, body: { idLichLamViec: CB.idLichLamViec, gioBatDauKhung: khungCB[0].gioBatDau } });
  kiem('đổi L5 sang bác sĩ B', r.status === 201, J(r.json));
  const L5b = { ...r.json.duLieu, hoTen: L5.hoTen };
  cap = await choThongBao(bsA, (t) => t.loai === 'LICH_HEN_DA_HUY' && t.noiDung.includes(L5.maTraCuu));
  kiemCau('đổi sang bác sĩ khác (bác sĩ cũ)', cap,
    `Bệnh nhân ${L5.hoTen} đã đổi lịch khám lúc ${gioNgay(L5.gioKhamDuKien)} (mã ${L5.maTraCuu}) sang bác sĩ khác; lịch hẹn này đã hủy.`,
    `Patient ${L5.hoTen} moved the appointment at ${gioNgay(L5.gioKhamDuKien, 'en')} (code ${L5.maTraCuu}) to another doctor; this appointment is cancelled.`);
  cap = await choThongBao(bsB, (t) => t.loai === 'LICH_HEN_MOI' && t.noiDung.includes(L5b.maTraCuu));
  kiemCau('đổi sang bác sĩ khác (bác sĩ mới)', cap, `Yêu cầu đặt lịch mới: bệnh nhân ${L5.hoTen}, ${gioNgay(L5b.gioKhamDuKien)} (mã ${L5b.maTraCuu}).`,
    `New booking request: patient ${L5.hoTen}, ${gioNgay(L5b.gioKhamDuKien, 'en')} (code ${L5b.maTraCuu}).`);

  console.log('4. Sửa ca: đổi phòng -> bác sĩ và bệnh nhân');
  const suaCa = (idPhongKham) => goi('PUT', `${CA}/${C1.idLichLamViec}`, { token: admin, body: { idPhongKham, gioBatDau: '18:00', gioKetThuc: '20:00', soLuotToiDaMoiGio: 6, thoiLuongLuotPhut: 10 } });
  r = await suaCa(P1.id);
  kiem('đổi ca C1 sang phòng mẫu (có tầng, có tiếng Anh)', r.status === 200, J(r.json));
  cap = await choThongBao(bsA, (t) => t.loai === 'CA_LAM_VIEC_THAY_DOI' && t.noiDung.includes('đã được sửa') && t.noiDung.includes(P1.tenPhong));
  kiemCau('ca đã sửa', cap, `Một ca làm việc của bạn đã được sửa, hiện là: 18:00-20:00 ngày ${ddmm(D1)}, phòng ${P1.tenPhong}.`,
    `One of your shifts was changed and is now: 18:00-20:00 on ${ddmm(D1)}, ${P1.tenPhongEn}.`);
  cap = await choThongBao(bn, (t) => t.loai === 'LICH_HEN_DOI_PHONG' && t.noiDung.includes(L1b.maTraCuu) && t.noiDung.includes(P1.tenPhong));
  kiemCau('lịch hẹn đổi phòng, có tầng', cap,
    `Lịch hẹn ${L1b.maTraCuu} (${gioNgay(L1b.gioKhamDuKien)}) đổi sang phòng ${P1.tenPhong} (${P1.tang}). Giờ khám không đổi.`,
    `Appointment ${L1b.maTraCuu} (${gioNgay(L1b.gioKhamDuKien, 'en')}) moved to ${P1.tenPhongEn} (${P1.tangEn}). The time is unchanged.`);
  r = await suaCa(P3.id);
  kiem('đổi ca C1 sang phòng không có tên tiếng Anh, không có tầng', r.status === 200, J(r.json));
  cap = await choThongBao(bn, (t) => t.loai === 'LICH_HEN_DOI_PHONG' && t.noiDung.includes(tenP3));
  kiemCau('đổi phòng, không tầng; tiếng Anh lấy tên tiếng Việt của phòng', cap,
    `Lịch hẹn ${L1b.maTraCuu} (${gioNgay(L1b.gioKhamDuKien)}) đổi sang phòng ${tenP3}. Giờ khám không đổi.`,
    `Appointment ${L1b.maTraCuu} (${gioNgay(L1b.gioKhamDuKien, 'en')}) moved to ${tenP3}. The time is unchanged.`);

  console.log('5. Yêu cầu xin nghỉ / đổi ca');
  const YC_BS = '/api/booking/bac-si/toi/yeu-cau-doi-lich', YC_QT = '/api/booking/quan-tri/yeu-cau-doi-lich';
  r = await goi('POST', YC_BS, { token: bsA, body: { idLichLamViec: N[0].idLichLamViec, loaiYeuCau: 'XIN_NGHI', lyDo: 'Việc gia đình' } });
  kiem('bác sĩ gửi xin nghỉ ca N1', r.status === 201, J(r.json));
  const yc1 = r.json.duLieu;
  const adminChuong = (loai, chuoi) => choThongBao(admin, (t) => t.loai === loai && t.idYeuCau === chuoi);
  cap = await adminChuong('YEU_CAU_DOI_LICH_MOI', yc1.id);
  kiemCau('yêu cầu mới (xin nghỉ) tới quản trị viên', cap, `Bác sĩ ${tenA} xin nghỉ ca 18:00-19:00 ngày ${ddmm(D2)}.`, `Dr. ${tenA} asks for leave on the shift 18:00-19:00 on ${ddmm(D2)}.`);
  r = await goi('POST', `${YC_QT}/${yc1.id}/tu-choi`, { token: admin, body: { ghiChu: 'Không có người thay' } });
  kiem('quản trị viên từ chối, có ghi chú', r.status === 200 && r.json.duLieu.ghiChuXuLy === 'Không có người thay', J(r.json));
  cap = await choThongBao(bsA, (t) => t.loai === 'KET_QUA_DUYET_DOI_LICH' && t.idYeuCau === yc1.id);
  kiemCau('kết quả: từ chối có ghi chú', cap, `Yêu cầu xin nghỉ ca 18:00-19:00 ngày ${ddmm(D2)} bị từ chối. Ghi chú: Không có người thay`,
    `Your leave request for the shift 18:00-19:00 on ${ddmm(D2)} was declined. Note: Không có người thay`);

  // Đổi ca chỉ đổi phòng (N3 sang phòng mẫu), duyệt không ghi chú
  r = await goi('POST', YC_BS, { token: bsA, body: { idLichLamViec: N[2].idLichLamViec, loaiYeuCau: 'DOI_CA', lyDo: 'Cần phòng có máy siêu âm', ngayMongMuon: D2c, gioBatDauMongMuon: '18:00', gioKetThucMongMuon: '19:00', idPhongKhamMongMuon: P1.id } });
  kiem('bác sĩ gửi đổi ca N3 (chỉ đổi phòng)', r.status === 201, J(r.json));
  const yc2 = r.json.duLieu;
  cap = await adminChuong('YEU_CAU_DOI_LICH_MOI', yc2.id);
  kiemCau('yêu cầu mới (đổi ca)', cap, `Bác sĩ ${tenA} xin đổi ca 18:00-19:00 ngày ${ddmm(D2c)}.`, `Dr. ${tenA} asks to change the shift 18:00-19:00 on ${ddmm(D2c)}.`);
  r = await goi('POST', `${YC_QT}/${yc2.id}/duyet`, { token: admin });
  kiem('duyệt đổi ca, không ghi chú', r.status === 200, J(r.json));
  cap = await choThongBao(bsA, (t) => t.loai === 'KET_QUA_DUYET_DOI_LICH' && t.idYeuCau === yc2.id);
  kiemCau('kết quả: duyệt đổi ca', cap, `Yêu cầu đổi ca 18:00-19:00 ngày ${ddmm(D2c)} đã được duyệt.`, `Your request to change the shift 18:00-19:00 on ${ddmm(D2c)} was approved.`);

  // Xin nghỉ ca C1 (đang có lịch hẹn L1b), duyệt có ghi chú -> bệnh nhân nhận lý do do hệ thống viết
  r = await goi('POST', YC_BS, { token: bsA, body: { idLichLamViec: C1.idLichLamViec, loaiYeuCau: 'XIN_NGHI', lyDo: 'Đi hội nghị' } });
  kiem('bác sĩ gửi xin nghỉ ca C1', r.status === 201, J(r.json));
  const yc3 = r.json.duLieu;
  r = await goi('POST', `${YC_QT}/${yc3.id}/duyet`, { token: admin, body: { ghiChu: 'Đã báo bệnh nhân' } });
  kiem('duyệt xin nghỉ, có ghi chú', r.status === 200, J(r.json));
  cap = await choThongBao(bsA, (t) => t.loai === 'KET_QUA_DUYET_DOI_LICH' && t.idYeuCau === yc3.id);
  kiemCau('kết quả: duyệt xin nghỉ có ghi chú', cap, `Yêu cầu xin nghỉ ca 18:00-20:00 ngày ${ddmm(D1)} đã được duyệt. Ghi chú: Đã báo bệnh nhân`,
    `Your leave request for the shift 18:00-20:00 on ${ddmm(D1)} was approved. Note: Đã báo bệnh nhân`);
  cap = await choThongBao(bn, (t) => t.loai === 'LICH_HEN_CAN_DOI' && t.noiDung.includes(L1b.maTraCuu));
  kiemCau('lịch hẹn cần đổi, lý do do hệ thống viết', cap,
    `Ca khám của lịch hẹn ${L1b.maTraCuu} (${gioNgay(L1b.gioKhamDuKien)}) đã bị hủy. Lý do: Bác sĩ nghỉ ca này. Vui lòng đổi sang khung giờ khác hoặc hủy lịch hẹn.`,
    `The session of appointment ${L1b.maTraCuu} (${gioNgay(L1b.gioKhamDuKien, 'en')}) was cancelled. Reason: The doctor is off for this shift. Please move to another time slot or cancel the appointment.`);

  console.log('6. Quản trị viên hủy ca đang có yêu cầu chờ duyệt');
  const L6 = await datLich(N[1].idLichLamViec, A.id, N[1].ngay);
  r = await goi('POST', YC_BS, { token: bsA, body: { idLichLamViec: N[1].idLichLamViec, loaiYeuCau: 'XIN_NGHI', lyDo: 'Khám sức khoẻ định kỳ' } });
  kiem('bác sĩ gửi xin nghỉ ca N2 (để chờ duyệt)', r.status === 201, J(r.json));
  const yc4 = r.json.duLieu;
  r = await goi('POST', `${CA}/${N[1].idLichLamViec}/huy`, { token: admin, body: { lyDo: '  Bảo trì thiết bị  ' } });
  kiem('quản trị viên hủy ca N2, lý do gõ không có dấu chấm', r.status === 200 && r.json.duLieu.soLichHenCanDoi === 1, J(r.json));
  cap = await choThongBao(bsA, (t) => t.loai === 'CA_LAM_VIEC_THAY_DOI' && t.noiDung.includes('đã bị hủy'));
  kiemCau('ca bị hủy', cap, `Ca làm việc 18:00-19:00 ngày ${ddmm(N[1].ngay)} của bạn đã bị hủy. Lý do: Bảo trì thiết bị.`,
    `Your shift 18:00-19:00 on ${ddmm(N[1].ngay)} was cancelled. Reason: Bảo trì thiết bị.`);
  cap = await choThongBao(bn, (t) => t.loai === 'LICH_HEN_CAN_DOI' && t.noiDung.includes(L6.maTraCuu));
  kiemCau('lịch hẹn cần đổi, lý do quản trị viên gõ', cap,
    `Ca khám của lịch hẹn ${L6.maTraCuu} (${gioNgay(L6.gioKhamDuKien)}) đã bị hủy. Lý do: Bảo trì thiết bị. Vui lòng đổi sang khung giờ khác hoặc hủy lịch hẹn.`,
    `The session of appointment ${L6.maTraCuu} (${gioNgay(L6.gioKhamDuKien, 'en')}) was cancelled. Reason: Bảo trì thiết bị. Please move to another time slot or cancel the appointment.`);
  cap = await choThongBao(bsA, (t) => t.loai === 'KET_QUA_DUYET_DOI_LICH' && t.idYeuCau === yc4.id);
  kiemCau('yêu cầu bị đóng do ca bị hủy', cap,
    `Yêu cầu xin nghỉ ca 18:00-19:00 ngày ${ddmm(N[1].ngay)} bị từ chối. Ghi chú: Ca làm việc đã bị quản trị viên hủy: Bảo trì thiết bị.`,
    `Your leave request for the shift 18:00-19:00 on ${ddmm(N[1].ngay)} was declined. Note: The shift was cancelled by an administrator: Bảo trì thiết bị.`);
  const ycCuaToi = async (lang) => (await goi('GET', YC_BS + '?kichThuoc=50', { token: bsA, lang })).json.duLieu.noiDung;
  const dsVi = await ycCuaToi(), dsEn = await ycCuaToi('en');
  const ghiChu = (ds, id) => (ds.find((y) => y.id === id) || {}).ghiChuXuLy;
  kiem('danh sách yêu cầu [vi]: ' + ghiChu(dsVi, yc4.id), ghiChu(dsVi, yc4.id) === 'Ca làm việc đã bị quản trị viên hủy: Bảo trì thiết bị.');
  kiem('danh sách yêu cầu [en]: ' + ghiChu(dsEn, yc4.id), ghiChu(dsEn, yc4.id) === 'The shift was cancelled by an administrator: Bảo trì thiết bị.');
  kiem('ghi chú quản trị viên gõ giữ nguyên ở cả 2 ngôn ngữ', ghiChu(dsVi, yc1.id) === 'Không có người thay' && ghiChu(dsEn, yc1.id) === 'Không có người thay', J([ghiChu(dsVi, yc1.id), ghiChu(dsEn, yc1.id)]));
  kiem('trong DB cột ghi_chu_xu_ly lưu khoá hệ thống', sql('select count(*) from yeu_cau_doi_lich where id_yeu_cau = ' + yc4.id + " and ghi_chu_xu_ly like '{heThong.caBiQuanTriHuy}%'") === '1');

  console.log('7. Dữ liệu cũ, DB, đếm chưa đọc');
  kiem('mọi thông báo sinh từ sự kiện đều có khoa_mau, tham_so, noi_dung', sql("select concat(count(*), 0x2f, sum(khoa_mau is not null and tham_so is not null and char_length(noi_dung) > 10)) from thong_bao where ma_nguon like 'booking:%'").split('/').every((s, i, a) => Number(s) > 20 && s === a[0]),
    sql("select concat(count(*), 0x2f, sum(khoa_mau is not null and tham_so is not null and char_length(noi_dung) > 10)) from thong_bao where ma_nguon like 'booking:%'"));
  kiem('sự kiện mới trong bảng chờ gửi không còn noi_dung, đều đã gửi', sql('select concat(sum(noi_dung is null), 0x2f, sum(khoa_mau is not null), 0x2f, sum(ngay_gui is not null), 0x2f, count(*)) from su_kien_thong_bao').split('/').every((s, i, a) => s === a[3]),
    sql('select concat(sum(noi_dung is null), 0x2f, sum(khoa_mau is not null), 0x2f, sum(ngay_gui is not null), 0x2f, count(*)) from su_kien_thong_bao'));
  const idBn = dnBn.taiKhoan.id;
  sql(`insert into thong_bao (id_tai_khoan, noi_dung, loai, da_doc) values (${idBn}, 'Legacy bell sentence ${hauTo}', 'HE_THONG', 0)`);
  sql(`insert into su_kien_thong_bao (loai, id_tai_khoan, noi_dung) values ('HE_THONG', ${idBn}, 'Legacy outbox sentence ${hauTo}')`);
  cap = await choThongBao(bn, (t) => t.noiDung === 'Legacy bell sentence ' + hauTo);
  kiem('thông báo cũ (không có mẫu) trả nguyên câu ở cả 2 ngôn ngữ', cap.vi.noiDung === 'Legacy bell sentence ' + hauTo && cap.en.noiDung === cap.vi.noiDung, J(cap));
  cap = await choThongBao(bn, (t) => t.noiDung === 'Legacy outbox sentence ' + hauTo);
  kiem('sự kiện cũ chỉ có noi_dung vẫn được gửi và hiện nguyên câu', cap.vi.noiDung === 'Legacy outbox sentence ' + hauTo && cap.en.noiDung === cap.vi.noiDung, J(cap));
  // Sự kiện có tham số hỏng: notification-service từ chối riêng dòng đó, các dòng khác vẫn đi
  sql(`insert into su_kien_thong_bao (loai, id_tai_khoan, khoa_mau, tham_so) values ('HE_THONG', ${idBn}, 'thongBao.lichHenMoi', 'khong-phai-json')`);
  sql(`insert into su_kien_thong_bao (loai, id_tai_khoan, noi_dung) values ('HE_THONG', ${idBn}, 'After bad row ${hauTo}')`);
  cap = await choThongBao(bn, (t) => t.noiDung === 'After bad row ' + hauTo);
  kiem('dòng hỏng không chặn dòng sau', cap.vi.noiDung === 'After bad row ' + hauTo, J(cap));
  await cho(12000);
  kiem('dòng tham số hỏng bị từ chối (so_lan_loi tăng), không tạo thông báo', Number(sql("select so_lan_loi from su_kien_thong_bao where tham_so = 'khong-phai-json'")) >= 1 && sql("select count(*) from thong_bao where tham_so = 'khong-phai-json'") === '0',
    sql("select concat(so_lan_loi, 0x2f, ifnull(ngay_gui, 0x2d)) from su_kien_thong_bao where tham_so = 'khong-phai-json'"));

  const dem = async () => (await goi('GET', CHUONG + '/so-chua-doc', { token: bn })).json.duLieu.soChuaDoc;
  const truoc = await dem();
  const mot = (await chuong(bn)).find((t) => !t.daDoc && t.loai === 'LICH_HEN_DA_XAC_NHAN');
  r = await goi('POST', `${CHUONG}/${mot.id}/da-doc`, { token: bn, lang: 'en' });
  kiem('đánh dấu đã đọc với en -> trả câu tiếng Anh, số chưa đọc giảm 1', r.status === 200 && r.json.duLieu.daDoc === true && r.json.duLieu.noiDung.startsWith('Dr. ') && (await dem()) === truoc - 1, J(r.json));
  r = await goi('GET', CHUONG + '?kichThuoc=100', { token: bn, lang: 'fr' });
  kiem('ngôn ngữ khác (fr) -> tiếng Việt', r.json.duLieu.noiDung.some((t) => t.noiDung.startsWith('Bác sĩ ')), J(r.json).slice(0, 300));
  const enHet = (await chuong(bsA, 'en')).filter((t) => CO_DAU.test(t.noiDung.replace(new RegExp([tenA, tenB, 'Nguyễn Thị Thử \\S+', tenP2, tenP3, 'Có việc đột xuất', 'Không có người thay', 'Đã báo bệnh nhân', 'Bảo trì thiết bị'].join('|'), 'g'), '')));
  kiem('chuông của bác sĩ đọc bằng en: ngoài họ tên, tên phòng chưa dịch và chữ người dùng gõ thì không còn tiếng Việt', enHet.length === 0, J(enHet.map((t) => t.noiDung)));

  console.log('8. Email tài khoản theo ngôn ngữ của tài khoản (log của identity-service, chế độ console)');
  const logEmail = (email) => sh('logs', '--since', '15m', 'eclinic-identity-service').split('\n').filter((d) => d.includes('DEV - ') && d.includes(email));
  const dangKy = (email, lang, so) => goi('POST', '/api/auth/register', { lang, body: { hoTen: 'Trần Thị Email ' + so, email, matKhau: 'Smoke@12345', soDienThoai: '09' + hauTo + so } });
  const emEn = `p3en${hauTo}@example.com`, emVi = `p3vi${hauTo}@example.com`;
  r = await dangKy(emEn, 'en', 1);
  kiem('đăng ký với Accept-Language: en -> ' + r.status, r.status === 201 || r.status === 200, J(r.json));
  r = await dangKy(emVi, undefined, 2);
  kiem('đăng ký không header -> ' + r.status, r.status === 201 || r.status === 200, J(r.json));
  await cho(3000);
  let dong = logEmail(emEn);
  kiem('email kích hoạt của tài khoản en: ' + (dong[0] || '').split('|').slice(1, 3).join('|'), dong.length === 1 && dong[0].includes('EN') && dong[0].includes('[eClinic] Activate your account'), dong.join('\n'));
  const dongVi = logEmail(emVi);
  kiem('email kích hoạt của tài khoản vi: ' + (dongVi[0] || '').split('|').slice(1, 3).join('|'), dongVi.length === 1 && dongVi[0].includes('VI') && dongVi[0].includes('[eClinic] Kích hoạt tài khoản'), dongVi.join('\n'));
  const token = ((dong[0] || '').match(/token=([A-Za-z0-9_\-.]+)/) || [])[1];
  r = await goi('POST', '/api/auth/verify-email', { body: { token } });
  kiem('kích hoạt tài khoản en bằng liên kết trong log', r.status === 200, J(r.json));
  r = await goi('POST', '/api/auth/forgot-password', { lang: 'vi', body: { email: emEn } });
  await cho(3000);
  dong = logEmail(emEn);
  kiem('quên mật khẩu gửi với header vi -> email vẫn tiếng Anh (theo tài khoản)', r.status === 200 && dong.some((d) => d.includes('[eClinic] Reset your password')), dong.join('\n'));
  const idEn = sql(`select id_tai_khoan from tai_khoan where email = '${emEn}'`);
  r = await goi('PUT', `/api/users/${idEn}/status`, { token: admin, body: { trangThai: 'VO_HIEU_HOA', lyDo: 'Kiểm thử phase 3' } });
  await cho(3000);
  dong = logEmail(emEn);
  kiem('quản trị viên (tiếng Việt) vô hiệu hoá -> email tiếng Anh', r.status === 200 && dong.some((d) => d.includes('[eClinic] Your account has been deactivated')), J(r.json) + '\n' + dong.join('\n'));
  r = await goi('PUT', `/api/users/${idEn}/status`, { token: admin, lang: 'en', body: { trangThai: 'DA_KICH_HOAT' } });
  await cho(3000);
  dong = logEmail(emEn);
  kiem('kích hoạt lại -> email tiếng Anh', r.status === 200 && dong.some((d) => d.includes('[eClinic] Your account has been reactivated')), J(r.json) + '\n' + dong.join('\n'));
  const loiEmail = sh('logs', '--since', '15m', 'eclinic-identity-service').split('\n').filter((d) => /ERROR/.test(d));
  kiem('identity-service không ghi ERROR nào khi dựng email', loiEmail.length === 0, loiEmail.slice(0, 5).join('\n'));
  const loiNoti = sh('logs', '--since', '15m', 'eclinic-notification-service').split('\n').filter((d) => /ERROR/.test(d));
  kiem('notification-service không ghi ERROR nào', loiNoti.length === 0, loiNoti.slice(0, 5).join('\n'));

  console.log(`\n${dat} đạt, ${hong} hỏng`);
  process.exit(hong ? 1 : 0);
})().catch((e) => { console.error(e); process.exit(1); });
