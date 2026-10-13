// Smoke phase 9 (DOANTOTNGH-9): chatbot trả lời về lịch hẹn, lượt khám của CHÍNH bệnh nhân đang chat. Mô hình là mô hình
// GIẢ (temp/d9-p9-mo-hinh-gia.js): kiểm tra mọi thứ code làm (ai được tra cứu, đồng ý, dữ liệu gửi mô hình, thẻ goiY),
// KHÔNG kiểm tra được mô hình thật có nghe lời khi bị dụ không. Chạy bằng temp/d9-p9-run.sh trên DB mới.
//   1 mặc định: khách / bác sĩ, chưa đồng ý, đồng ý, 2 hàm "của tôi", tham số chỉ vào người khác, bệnh nhân khác, phiên
//     khách, đọc lại, rút đồng ý, lộ chỉ dẫn, booking-service tắt, dữ liệu gửi mô hình, log
//   2 CHATBOT_PERSONAL_DATA=false: bệnh nhân đã đồng ý cũng không có hàm "của tôi"
// node temp/d9-p9-smoke.js <1|2>
const { execFileSync } = require('child_process');
const fs = require('fs');
const CHANG = Number(process.argv[2]);
const TEP = 'temp/d9-p9-state.json';
const GOC = 'http://127.0.0.1:8080';
const GIA = 'http://127.0.0.1:18999';
let dat = 0, hong = 0;
function kiem(ten, dung, chiTiet) {
  if (dung) { dat++; console.log('  ok   ' + ten); } else { hong++; console.log('  HỎNG ' + ten + (chiTiet ? '\n       ' + String(chiTiet).slice(0, 1800) : '')); }
}
async function goi(phuongThuc, duongDan, { body, token, lang, khach } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = 'Bearer ' + token;
  if (lang) headers['Accept-Language'] = lang;
  if (khach) headers['X-Chat-Khach'] = khach;
  const res = await fetch(GOC + duongDan, { method: phuongThuc, headers, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(40000) });
  let json = null; try { json = await res.json(); } catch { /* không phải JSON */ }
  return { status: res.status, json: json || {} };
}
const sh = (...thamSo) => execFileSync('docker', thamSo, { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 }).trim();
const sql = (cau) => sh('exec', 'eclinic-mysql', 'sh', '-c', 'mysql -u$MYSQL_USER -p$MYSQL_PASSWORD $MYSQL_DATABASE -N -e "' + cau + '" 2>/dev/null');
const J = (x) => JSON.stringify(x);
const cho = (ms) => new Promise((xong) => setTimeout(xong, ms));
const ngaySau = (n) => { const d = new Date(Date.now() + n * 86400000); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; };
const dangNhap = async (email, matKhau) => (await goi('POST', '/api/auth/login', { body: { email, matKhau } })).json.duLieu;
// Mọi request mô hình giả đã nhận trong cả chặng, gom lại trước mỗi lần xoá
const tatCaBan = [];
const ghiCuaGia = async () => (await fetch(GIA + '/ghi', { signal: AbortSignal.timeout(10000) })).json();
const xoaGhi = async () => { tatCaBan.push(...await ghiCuaGia()); await fetch(GIA + '/ghi', { method: 'DELETE', signal: AbortSignal.timeout(10000) }); };
const moPhien = async (tuyChon) => (await goi('POST', '/api/chatbot/phien', tuyChon)).json.duLieu;
const gui = (P, noiDung, { token, lang = 'vi' } = {}) => goi('POST', `/api/chatbot/phien/${P.idPhien}/tin-nhan`, { khach: P.maKhach, token, lang, body: { noiDung } });
const dongY = (P, token, phuongThuc = 'POST') => goi(phuongThuc, `/api/chatbot/phien/${P.idPhien}/dong-y`, { token });
const tl = (r) => (r.json.duLieu || {}).traLoi || {};
const tenCongCu = (ban) => (ban.body.tools || []).map((t) => t.function.name);
const HAI = ['tim_bac_si', 'xem_lich_bac_si'];
const BON = [...HAI, 'lich_hen_cua_toi', 'lan_kham_cua_toi'];
const ketQuaTool = (ban) => ban.body.messages.filter((m) => m.role === 'tool').map((m) => m.content);
const logChatbot = () => execFileSync('docker', ['logs', 'eclinic-chatbot-service'], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, stdio: ['ignore', 'pipe', 'pipe'] });

const theLichHen = (l) => ({ maPhieuKham: l.maPhieuKham, maTraCuu: l.maTraCuu, linkPhieuKham: l.linkPhieuKham, trangThai: l.trangThai, ngay: l.ngay, gioBatDauKhung: l.gioBatDauKhung, gioKetThucKhung: l.gioKetThucKhung,
  hoTenBacSi: l.bacSi.hoTen, tenChuyenKhoa: l.tenChuyenKhoa, tenPhong: l.phongKham.tenPhong, tang: l.phongKham.tang, hoTenBenhNhan: l.hoTenBenhNhan, laBanThan: l.laBanThan, canDoiLich: l.canDoiLich });
const theLanKham = (k) => ({ maPhieuKham: k.lichHen.maPhieuKham, maTraCuu: k.lichHen.maTraCuu, ngay: k.lichHen.ngay, hoTenBacSi: k.lichHen.bacSi.hoTen, tenChuyenKhoa: k.lichHen.tenChuyenKhoa,
  hoTenBenhNhan: k.lichHen.hoTenBenhNhan, laBanThan: k.lichHen.laBanThan, ngayTaiKhamDeXuat: k.ketQua.ngayTaiKhamDeXuat });
const sapToiCua = async (token, lang) => (await goi('GET', '/api/booking/lich-hen/cua-toi?loc=SAP_TOI&kichThuoc=10', { token, lang })).json.duLieu.noiDung.map(theLichHen);
const daKhamCua = async (token, lang) => (await goi('GET', '/api/booking/lich-su-kham/cua-toi?kichThuoc=10', { token, lang })).json.duLieu.noiDung.map(theLanKham);

const LY_DO = 'LYDO-RIENG-P9', CHAN_DOAN = 'CHANDOAN-RIENG-P9', LOI_DAN = 'LOIDAN-RIENG-P9', THUOC = 'THUOC-RIENG-P9', NGUOI_THAN = 'Tran Van NGUOITHANRIENG';

async function chang1() {
  const hauTo = Date.now().toString().slice(-6);
  const admin = (await dangNhap(sh('exec', 'eclinic-identity-service', 'printenv', 'ADMIN_EMAIL'), sh('exec', 'eclinic-identity-service', 'printenv', 'ADMIN_PASSWORD'))).accessToken;
  const dnBs = await dangNhap('bacsi01@eclinic.local', 'Demo@123');
  const bs = dnBs.accessToken;
  const dnA = await dangNhap('benhnhan01@eclinic.local', 'Demo@123');
  const dnB = await dangNhap('benhnhan02@eclinic.local', 'Demo@123');
  const A = dnA.accessToken, B = dnB.accessToken;
  const hoSo = (email) => { const [cccd, sdt, ngaySinh, gioiTinh] = sql(`select h.cccd, h.so_dien_thoai, h.ngay_sinh, h.gioi_tinh from ho_so_benh_nhan h join tai_khoan t on t.id_tai_khoan = h.id_tai_khoan where t.email = '${email}'`).split('\t'); return { cccd, soDienThoai: sdt, ngaySinh, gioiTinh }; };
  const hsA = { hoTen: dnA.taiKhoan.hoTen, ...hoSo('benhnhan01@eclinic.local') };
  const hsB = { hoTen: dnB.taiKhoan.hoTen, ...hoSo('benhnhan02@eclinic.local') };
  let r;

  console.log('1.0 Chuẩn bị: A có 1 lịch cho mình, 1 lịch cho người thân, 1 lượt đã khám có ngày tái khám; B có 1 lịch');
  kiem('V24 đã chạy', sql("select count(*) from flyway_schema_history where version = '24' and success = 1") === '1');
  const dsBacSi = (await goi('GET', '/api/catalog/bac-si?kichThuoc=100')).json.duLieu.noiDung;
  const bacSiA = dsBacSi.find((b) => b.hoTen === dnBs.taiKhoan.hoTen);
  const phong = (await goi('GET', '/api/catalog/quan-tri/phong-kham?kichThuoc=100', { token: admin })).json.duLieu.noiDung.find((p) => p.idChuyenKhoa === bacSiA.idChuyenKhoa);
  r = await goi('POST', '/api/booking/quan-tri/lich-lam-viec', { token: admin, body: { idBacSi: bacSiA.id, idPhongKham: phong.id, ngay: ngaySau(15), gioBatDau: '17:00', gioKetThuc: '21:00', soLuotToiDaMoiGio: 6, thoiLuongLuotPhut: 10 } });
  kiem('xếp ca ngày +15 cho bác sĩ', r.status === 201, J(r.json));
  const ca = r.json.duLieu.daTao[0];
  const khung = ((await goi('GET', `/api/booking/khung-gio?ngay=${ngaySau(15)}&idBacSi=${bacSiA.id}`)).json.duLieu || []).find((c) => c.idLichLamViec === ca.idLichLamViec).khungGio;
  async function datLich(token, viTri, benhNhan, banThan) {
    const kq = await goi('POST', '/api/booking/lich-hen', { token, body: { idLichLamViec: ca.idLichLamViec, gioBatDauKhung: khung[viTri].gioBatDau, lyDoKham: LY_DO, benhNhan, datChoBanThan: banThan, luuNguoiThan: false } });
    if (kq.status !== 201) { kiem('đặt lịch', false, J(kq.json)); return {}; }
    return { ...kq.json.duLieu, id: sql(`select id_lich_hen from lich_hen where ma_tra_cuu = '${kq.json.duLieu.maTraCuu}'`) };
  }
  const nguoiThan = { hoTen: NGUOI_THAN, ngaySinh: '1960-02-02', gioiTinh: 'NAM', soDienThoai: '0377' + hauTo, cccd: '079377' + hauTo };
  const L1 = await datLich(A, 0, hsA, true);
  const L2 = await datLich(A, 1, nguoiThan, false);
  const L3 = await datLich(A, 2, hsA, true);
  const LB = await datLich(B, 0, hsB, true);
  kiem('đặt đủ 4 lịch hẹn', [L1, L2, L3, LB].every((l) => l.maTraCuu), J([L1, L2, L3, LB]));
  sql(`update khung_gio_kham k join lich_hen l on l.id_khung_gio = k.id_khung_gio set k.gio_bat_dau = k.gio_bat_dau - interval 16 day, k.gio_ket_thuc = k.gio_ket_thuc - interval 16 day where l.ma_tra_cuu = '${L3.maTraCuu}'`);
  r = await goi('POST', `/api/medical/bac-si/toi/lich-hen/${L3.id}/benh-an`, { token: bs, body: { chanDoan: CHAN_DOAN, ghiChu: LOI_DAN, ngayTaiKhamDeXuat: ngaySau(10), donThuoc: [{ tenThuoc: THUOC, donVi: 'vien', lieuDung: '500mg', soLanMoiNgay: 2, soNgayDung: 5 }] } });
  kiem('bác sĩ ghi kết quả lượt khám L3 kèm ngày tái khám +10', r.status === 201, J(r.json));
  const sapToiA = await sapToiCua(A, 'vi');
  const daKhamA = await daKhamCua(A, 'vi');
  kiem('màn hình của A: 2 lịch sắp tới (mình + người thân), 1 lượt đã khám có ngày tái khám', sapToiA.length === 2 && J(sapToiA.map((l) => l.maTraCuu).sort()) === J([L1.maTraCuu, L2.maTraCuu].sort()) && sapToiA.some((l) => !l.laBanThan && l.hoTenBenhNhan === NGUOI_THAN) && daKhamA.length === 1 && daKhamA[0].ngayTaiKhamDeXuat === ngaySau(10), J(sapToiA) + J(daKhamA));
  const camChu = [hsA.hoTen, NGUOI_THAN, hsA.soDienThoai, nguoiThan.soDienThoai, hsA.cccd, nguoiThan.cccd, 'ECL-', L1.maPhieuKham, L2.maPhieuKham, L3.maPhieuKham, LY_DO, CHAN_DOAN, LOI_DAN, THUOC, hsB.hoTen, LB.maPhieuKham];
  await xoaGhi(); tatCaBan.length = 0;

  console.log('1.1 Khách và tài khoản bác sĩ: không có hàm "của tôi"');
  const PK = await moPhien({});
  r = await gui(PK, '#lichtoi');
  let cacBan = await ghiCuaGia();
  kiem('khách: mô hình chỉ được đưa 2 hàm công khai; chỉ dẫn dùng đoạn "PERSONAL DATA", không nhắc tên 2 hàm kia', J(tenCongCu(cacBan[0])) === J(HAI) && cacBan[0].body.messages[0].content.includes('PERSONAL DATA\n') && !cacBan[0].body.messages[0].content.includes('lich_hen_cua_toi') && !cacBan[0].body.messages[0].content.includes('{{'), J(tenCongCu(cacBan[0])));
  kiem('khách: mô hình vẫn xin lich_hen_cua_toi -> kết quả báo lỗi, trả lời thường, không có thẻ', r.status === 200 && tl(r).loai === 'TRA_LOI' && tl(r).goiY == null && tl(r).noiDung.includes('"error"') && tl(r).noiDung.includes('Personal data is not available'), J(r.json));
  await xoaGhi();
  const PBS = await moPhien({ token: bs });
  r = await gui(PBS, '#lankham', { token: bs });
  cacBan = await ghiCuaGia();
  kiem('bác sĩ đã đăng nhập: 2 hàm công khai, xin lan_kham_cua_toi nhận kết quả báo lỗi, không có thẻ', J(tenCongCu(cacBan[0])) === J(HAI) && tl(r).loai === 'TRA_LOI' && tl(r).goiY == null && tl(r).noiDung.includes('Personal data is not available'), J(r.json));
  r = await dongY(PBS, bs);
  kiem('bác sĩ gọi dong-y: 403', r.status === 403, r.status + J(r.json));

  console.log('1.2 Bệnh nhân A chưa đồng ý');
  await xoaGhi();
  const PA = await moPhien({ token: A });
  await gui(PA, 'Xin chao', { token: A });
  r = await gui(PA, '#lichtoi', { token: A });
  cacBan = await ghiCuaGia();
  kiem('A: mô hình được đưa 4 hàm; 2 hàm "của tôi" không có tham số; chỉ dẫn có đoạn về dữ liệu của chính người dùng', J(tenCongCu(cacBan[0])) === J(BON) && cacBan[0].body.tools.slice(2).every((t) => J(t.function.parameters) === J({ type: 'object', properties: {} }) || J(t.function.parameters) === J({ properties: {}, type: 'object' })) && cacBan[0].body.messages[0].content.includes("THE USER'S OWN APPOINTMENTS AND VISITS") && !cacBan[0].body.messages[0].content.includes('PERSONAL DATA\n'), J(cacBan[0].body.tools.slice(2)));
  kiem('xin lich_hen_cua_toi khi chưa đồng ý: 200, loai CAN_DONG_Y, câu cố định tiếng Việt, không có thẻ', r.status === 200 && tl(r).loai === 'CAN_DONG_Y' && tl(r).noiDung.startsWith('Để trả lời câu hỏi này') && tl(r).noiDung.includes('không được gửi đi') && tl(r).goiY == null, J(r.json));
  kiem('dừng ngay sau lần gọi mô hình đầu: mô hình không nhận kết quả tra cứu nào', cacBan.length === 2 && cacBan.every((b) => ketQuaTool(b).length === 0), cacBan.length);
  r = await gui(PA, '#lankham', { token: A, lang: 'en' });
  kiem('[en] xin lan_kham_cua_toi: CAN_DONG_Y bằng tiếng Anh', tl(r).loai === 'CAN_DONG_Y' && tl(r).noiDung.startsWith('To answer this I need to read your own appointments'), J(r.json));
  r = await dongY(PA, null);
  kiem('dong-y không đăng nhập: 401', r.status === 401, r.status + J(r.json));
  r = await dongY(PA, B);
  kiem('B gọi dong-y trên phiên của A: 404, phiên của A vẫn chưa đồng ý', r.status === 404 && sql(`select count(*) from phien_chat where id_phien_chat = ${PA.idPhien} and dong_y_du_lieu_luc is null`) === '1', r.status + J(r.json));

  console.log('1.3 A đồng ý rồi hỏi lịch hẹn sắp tới');
  r = await dongY(PA, A);
  const dsPhien = (await goi('GET', '/api/chatbot/phien', { token: A })).json.duLieu;
  kiem('A gọi dong-y: 200; danh sách phiên báo daDongYDuLieu = true', r.status === 200 && r.json.thongDiep === 'Đã ghi nhận đồng ý, bạn hãy gửi lại câu hỏi' && dsPhien.find((p) => p.idPhien === PA.idPhien).daDongYDuLieu === true, J(r.json) + J(dsPhien));
  await xoaGhi();
  r = await gui(PA, '#lichtoi', { token: A });
  cacBan = await ghiCuaGia();
  const gA = tl(r).goiY || {};
  kiem('200 TRA_LOI; goiY.lichHenCuaToi khớp GET /lich-hen/cua-toi?loc=SAP_TOI của A (mã, giờ, bác sĩ, phòng, họ tên, của ai)', r.status === 200 && tl(r).loai === 'TRA_LOI' && J(gA.lichHenCuaToi) === J(sapToiA), J(gA.lichHenCuaToi) + '\n' + J(sapToiA));
  kiem('các phần khác của goiY: lanKham null (không tra cứu), bacSi / lichKham rỗng, chuyenKhoa null', gA.lanKham === null && J(gA.bacSi) === '[]' && J(gA.lichKham) === '[]' && gA.chuyenKhoa === null, J(gA));
  const kqLich = JSON.parse(ketQuaTool(cacBan[1])[0]);
  const KHOA_LICH = ['date', 'time', 'doctor', 'specialty', 'room', 'floor', 'status', 'forWhom'];
  kiem('kết quả cho mô hình: 2 lịch, mỗi lịch đúng 8 trường (ngày có thứ, giờ, bác sĩ, chuyên khoa, phòng, tầng, trạng thái, của ai)', kqLich.upcomingAppointments.length === 2 && kqLich.upcomingAppointments.every((l, i) => J(Object.keys(l)) === J(KHOA_LICH) && l.date.startsWith(sapToiA[i].ngay + ' (') && /^\d{2}:\d{2}-\d{2}:\d{2}$/.test(l.time) && l.doctor === sapToiA[i].hoTenBacSi && l.room === sapToiA[i].tenPhong && l.status === "waiting for the doctor's confirmation") && J(kqLich.upcomingAppointments.map((l) => l.forWhom).sort()) === J(['another person the user booked for', 'the user']), J(kqLich));
  kiem('các cặp CAN_DONG_Y cũ không được gửi lại cho mô hình', !J(cacBan[0].body.messages).includes('Để trả lời câu hỏi này') && !J(cacBan[0].body.messages).includes('To answer this I need') && cacBan[0].body.messages.some((m) => m.content === 'TRA LOI GIA: Xin chao'), J(cacBan[0].body.messages.map((m) => m.role)));
  r = await gui(PA, '#lichtoi', { token: A, lang: 'en' });
  kiem('[en] thẻ khớp màn hình tiếng Anh của A (phòng "Room ...", chuyên khoa tiếng Anh), họ tên giữ nguyên', J(tl(r).goiY.lichHenCuaToi) === J(await sapToiCua(A, 'en')) && /^Room /.test(tl(r).goiY.lichHenCuaToi[0].tenPhong) && tl(r).goiY.lichHenCuaToi.some((l) => l.hoTenBenhNhan === hsA.hoTen), J(tl(r).goiY.lichHenCuaToi));

  console.log('1.4 Các lượt đã khám và ngày tái khám');
  await xoaGhi();
  r = await gui(PA, '#lankham', { token: A });
  cacBan = await ghiCuaGia();
  kiem('goiY.lanKham khớp GET /lich-su-kham/cua-toi của A, có ngày tái khám +10; lichHenCuaToi null', J(tl(r).goiY.lanKham) === J(daKhamA) && tl(r).goiY.lanKham[0].ngayTaiKhamDeXuat === ngaySau(10) && tl(r).goiY.lichHenCuaToi === null, J(tl(r).goiY) + '\n' + J(daKhamA));
  const kqKham = JSON.parse(ketQuaTool(cacBan[1])[0]);
  kiem('kết quả cho mô hình: đúng 5 trường (ngày, bác sĩ, chuyên khoa, của ai, ngày tái khám có thứ) và lời nhắc "chưa có lịch hẹn"', kqKham.finishedVisits.length === 1 && J(Object.keys(kqKham.finishedVisits[0])) === J(['date', 'doctor', 'specialty', 'forWhom', 'followUpDateRecommended']) && kqKham.finishedVisits[0].followUpDateRecommended.startsWith(ngaySau(10) + ' (') && kqKham.note.includes('no appointment is booked'), J(kqKham));

  console.log('1.5 Mô hình viết tham số chỉ vào người khác');
  await xoaGhi();
  const idB = sql("select id_tai_khoan from tai_khoan where email = 'benhnhan02@eclinic.local'");
  r = await gui(PA, `#cuanguoikhac ${idB} ${hsB.soDienThoai} ${hsB.hoTen}`, { token: A });
  cacBan = await ghiCuaGia();
  kiem('mô hình đã gửi idTaiKhoan, soDienThoai, hoTen, maTraCuu của B trong tham số', cacBan.length === 2 && J(cacBan[1].body.messages.slice(-2)[0].tool_calls[0].function.arguments).includes(hsB.soDienThoai));
  kiem('kết quả vẫn là lịch của A, giống hệt lần gọi không tham số; thẻ là của A', J(JSON.parse(ketQuaTool(cacBan[1])[0])) === J(kqLich) && J(tl(r).goiY.lichHenCuaToi) === J(sapToiA), ketQuaTool(cacBan[1])[0]);
  kiem('không có gì của B trong câu trả lời và thẻ (mã tra cứu, mã phiếu khám)', !J(tl(r)).includes(LB.maTraCuu) && !J(tl(r)).includes(LB.maPhieuKham), J(tl(r)));

  console.log('1.6 Bệnh nhân B');
  await xoaGhi();
  const PB = await moPhien({ token: B });
  await dongY(PB, B);
  r = await gui(PB, '#lichtoi', { token: B });
  const sapToiB = await sapToiCua(B, 'vi');
  kiem('B đồng ý trong phiên của mình: thẻ chỉ có 1 lịch của B', sapToiB.length === 1 && sapToiB[0].maTraCuu === LB.maTraCuu && J(tl(r).goiY.lichHenCuaToi) === J(sapToiB), J(tl(r).goiY));
  r = await gui(PB, '#lankham', { token: B });
  kiem('B chưa khám lần nào: lanKham rỗng (không null), mô hình được báo là không có', J(tl(r).goiY.lanKham) === '[]' && tl(r).noiDung.includes('no finished visit'), J(r.json));
  r = await gui(PA, '#lichtoi', { token: B });
  kiem('JWT của B trên phiên của A: 404', r.status === 404, r.status + J(r.json));
  r = await goi('GET', `/api/chatbot/phien/${PA.idPhien}/tin-nhan`, { token: B });
  kiem('B đọc lại phiên của A: 404', r.status === 404, r.status);

  console.log('1.7 A đã đăng nhập nhưng dùng phiên của khách');
  await xoaGhi();
  const PK2 = await moPhien({});
  r = await gui(PK2, '#lichtoi', { token: A });
  cacBan = await ghiCuaGia();
  kiem('phiên khách + JWT của A: gửi được, nhưng chỉ 2 hàm công khai và không có thẻ', r.status === 200 && J(tenCongCu(cacBan[0])) === J(HAI) && tl(r).goiY == null && tl(r).noiDung.includes('Personal data is not available'), J(r.json));
  r = await goi('POST', `/api/chatbot/phien/${PK2.idPhien}/dong-y`, { token: A, khach: PK2.maKhach });
  kiem('dong-y trên phiên khách: 404', r.status === 404, r.status + J(r.json));

  console.log('1.8 Đọc lại phiên và hỏi tiếp');
  r = await goi('GET', `/api/chatbot/phien/${PA.idPhien}/tin-nhan`, { token: A });
  const coThe = r.json.duLieu.filter((t) => t.goiY && t.goiY.lichHenCuaToi);
  kiem('đọc lại phiên của A: các câu trả lời vẫn kèm đúng thẻ lịch hẹn; câu CAN_DONG_Y không có thẻ', coThe.length === 3 && J(coThe[0].goiY.lichHenCuaToi) === J(sapToiA) && r.json.duLieu.filter((t) => t.loai === 'CAN_DONG_Y').length === 2 && r.json.duLieu.filter((t) => t.loai === 'CAN_DONG_Y').every((t) => t.goiY == null), J(r.json.duLieu.map((t) => [t.loai, !!t.goiY])));
  await xoaGhi();
  r = await gui(PA, 'Cam on ban', { token: A });
  cacBan = await ghiCuaGia();
  const cu = cacBan[0].body.messages.filter((m) => m.role === 'assistant' && m.content.includes('[DATA SHOWN TO THE USER WITH THIS ANSWER]'));
  const ghiChu = cu.map((m) => m.content.split('[DATA SHOWN TO THE USER WITH THIS ANSWER]')[1]);
  kiem('câu hỏi tiếp theo: dòng ghi chú chỉ nói "đã hiện", không có ngày, tên, mã nào', cu.length >= 2 && ghiChu.every((g) => g === " the user's own upcoming appointments were shown as cards;" || g === " the user's own finished visits were shown as cards;"), J(ghiChu));
  kiem('không gửi lại lượt tra cứu cũ (không có role tool)', cacBan[0].body.messages.every((m) => m.role !== 'tool' && !m.tool_calls));

  console.log('1.9 Rút lại đồng ý');
  r = await dongY(PA, A, 'DELETE');
  kiem('DELETE dong-y: 200, cột đồng ý về NULL', r.status === 200 && sql(`select count(*) from phien_chat where id_phien_chat = ${PA.idPhien} and dong_y_du_lieu_luc is null`) === '1', J(r.json));
  await xoaGhi();
  r = await gui(PA, '#lichtoi', { token: A });
  cacBan = await ghiCuaGia();
  kiem('hỏi lại: CAN_DONG_Y, không đọc gì', tl(r).loai === 'CAN_DONG_Y' && tl(r).goiY == null && cacBan.length === 1 && ketQuaTool(cacBan[0]).length === 0, J(r.json));
  const guiLai = J(cacBan[0].body.messages);
  kiem('các câu trả lời cũ về lịch hẹn / lượt khám không còn được gửi cho mô hình; câu trả lời thường vẫn gửi', !guiLai.includes('upcomingAppointments') && !guiLai.includes('finishedVisits') && !guiLai.includes('were shown as cards') && guiLai.includes('TRA LOI GIA: Cam on ban'), guiLai.slice(-900));
  await dongY(PA, A);

  console.log('1.10 Mô hình chép lại chỉ dẫn');
  r = await gui(PA, '#lo', { token: A });
  kiem('câu trả lời chứa chỉ dẫn hệ thống bị thay bằng câu từ chối: NGOAI_PHAM_VI', tl(r).loai === 'NGOAI_PHAM_VI' && tl(r).noiDung.startsWith('Xin lỗi, câu hỏi này nằm ngoài phạm vi') && !tl(r).noiDung.includes('WHAT YOU HELP WITH'), J(r.json));
  kiem('chỉ dẫn không bị lưu vào tin nhắn nào', sql("select count(*) from tin_nhan_chat where noi_dung like '%WHAT YOU HELP WITH%'") === '0');

  console.log('1.11 Dữ liệu gửi cho mô hình và log');
  await xoaGhi();
  // Phần SERVER đưa cho mô hình: chỉ dẫn, kết quả tra cứu, các câu trả lời cũ gửi lại. Bỏ lời người dùng gõ và tham số do
  // chính mô hình viết (mục 1.5 cố ý gõ tên B và viết 1 mã ECL- giả vào đó).
  const daGui = J(tatCaBan.map((b) => b.body.messages.filter((m) => m.role !== 'user').map((m) => m.content)));
  const lot = camChu.filter((c) => daGui.includes(c));
  kiem(`mọi thứ mô hình nhận trong cả chặng (${tatCaBan.length} request): không có họ tên bệnh nhân / người thân, số điện thoại, CCCD, mã ECL-, mã phiếu khám, lý do khám, chẩn đoán, lời dặn, tên thuốc`, tatCaBan.length >= 20 && lot.length === 0, J(lot));
  kiem('mô hình không nhận JWT của ai (Authorization gửi mô hình là khoá của mô hình)', !J(tatCaBan).includes(A) && !J(tatCaBan).includes(B) && tatCaBan.every((b) => b.authorization === 'Bearer khoa-gia-smoke-p7'));
  let log = logChatbot();
  kiem('log chatbot-service: không có JWT, không có nội dung tin nhắn, không có dòng ERROR', !log.includes(A) && !log.includes(A.split('.')[2]) && !log.includes('Bearer ') && !log.includes('Cam on ban') && !/ ERROR /.test(log), (/.*ERROR.*/.exec(log) || [''])[0]);

  console.log('1.12 booking-service tắt');
  sh('stop', 'eclinic-booking-service');
  try {
    r = await gui(PA, '#lichtoi', { token: A });
    kiem('hỏi lịch hẹn khi booking-service tắt: vẫn 200 TRA_LOI, không có thẻ, mô hình được báo là tạm không xem được', r.status === 200 && tl(r).loai === 'TRA_LOI' && tl(r).goiY == null && tl(r).noiDung.includes('temporarily unavailable'), J(r.json).slice(0, 500));
  } finally {
    sh('start', 'eclinic-booking-service');
    for (let i = 0; i < 60; i++) {
      if ((await goi('GET', `/api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa=${bacSiA.idChuyenKhoa}`).catch(() => ({}))).status === 200) break;
      await cho(3000);
    }
  }
  r = await gui(PA, '#lichtoi', { token: A });
  kiem('booking-service chạy lại: thẻ lịch hẹn có trở lại', r.status === 200 && J((tl(r).goiY || {}).lichHenCuaToi) === J(sapToiA), J(r.json).slice(0, 300));
  log = logChatbot();
  kiem('log vẫn không có dòng ERROR và không có JWT', !/ ERROR /.test(log) && !log.includes(A.split('.')[2]), (/.*ERROR.*/.exec(log) || [''])[0]);
  fs.writeFileSync(TEP, J({ idPhien: PA.idPhien }));
}

async function chang2() {
  const S = JSON.parse(fs.readFileSync(TEP, 'utf8'));
  const A = (await dangNhap('benhnhan01@eclinic.local', 'Demo@123')).accessToken;
  const PA = { idPhien: S.idPhien };
  console.log('2.1 CHATBOT_PERSONAL_DATA=false (' + sh('exec', 'eclinic-chatbot-service', 'printenv', 'CHATBOT_PERSONAL_DATA') + ')');
  kiem('phiên của A vẫn đang ở trạng thái đã đồng ý', sql(`select count(*) from phien_chat where id_phien_chat = ${PA.idPhien} and dong_y_du_lieu_luc is not null`) === '1');
  await xoaGhi();
  const r = await gui(PA, '#lichtoi', { token: A });
  const cacBan = await ghiCuaGia();
  kiem('A đã đồng ý nhưng tính năng tắt: chỉ 2 hàm công khai, không có thẻ, kết quả báo lỗi', r.status === 200 && J(tenCongCu(cacBan[0])) === J(HAI) && tl(r).goiY == null && tl(r).noiDung.includes('Personal data is not available'), J(r.json));
  kiem('các câu trả lời cũ về lịch hẹn không được gửi lại cho mô hình', !J(cacBan[0].body.messages).includes('upcomingAppointments'), J(cacBan[0].body.messages).slice(-600));
}

[chang1, chang2][CHANG - 1]().catch((e) => { hong++; console.log('  HỎNG ngoại lệ: ' + (e.stack || e)); }).finally(() => {
  console.log(`\nChặng ${CHANG}: ${dat} đạt, ${hong} hỏng`);
  process.exit(hong ? 1 : 0);
});
