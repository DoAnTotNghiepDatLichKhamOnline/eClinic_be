// Smoke phase 7 (DOANTOTNGH-9): hội thoại chatbot và các rào chắn. 4 chặng, chatbot-service được tạo lại giữa các chặng
// (temp/d9-p7-run.sh). Mô hình là mô hình GIẢ (temp/d9-p7-mo-hinh-gia.js, 127.0.0.1:18999): kiểm tra mọi thứ code làm,
// KHÔNG kiểm tra được mô hình thật có nghe lời chỉ dẫn hay không.
//   1 giới hạn rộng : mở phiên, chủ phiên, nội dung gửi cho mô hình, ngoài phạm vi, cấp cứu, mô hình lỗi, kiểm tra dữ liệu, log
//   2 3 tin / cửa sổ, 4 phiên / giờ      3 2 tin / ngày, 5 lần gọi mô hình / ngày toàn hệ thống      4 không có khoá API
// node temp/d9-p7-smoke.js <1|2|3|4>
const { execFileSync } = require('child_process');
const CHANG = Number(process.argv[2]);
const GOC = 'http://127.0.0.1:8080';
const GIA = 'http://127.0.0.1:18999';
const KHOA_GIA = 'khoa-gia-smoke-p7';
let dat = 0, hong = 0;
function kiem(ten, dung, chiTiet) {
  if (dung) { dat++; console.log('  ok   ' + ten); } else { hong++; console.log('  HỎNG ' + ten + (chiTiet ? '\n       ' + String(chiTiet).slice(0, 1500) : '')); }
}
async function goi(phuongThuc, duongDan, { body, token, lang, khach, tho } = {}) {
  const headers = {};
  if (body !== undefined || tho !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = 'Bearer ' + token;
  if (lang) headers['Accept-Language'] = lang;
  if (khach) headers['X-Chat-Khach'] = khach;
  const batDau = Date.now();
  const res = await fetch(GOC + duongDan, { method: phuongThuc, headers, body: tho !== undefined ? tho : body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(40000) });
  let json = null; try { json = await res.json(); } catch { /* không phải JSON */ }
  return { status: res.status, json: json || {}, ms: Date.now() - batDau };
}
const sh = (...thamSo) => execFileSync('docker', thamSo, { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 }).trim();
const sql = (cau) => sh('exec', 'eclinic-mysql', 'sh', '-c', 'mysql -u$MYSQL_USER -p$MYSQL_PASSWORD $MYSQL_DATABASE -N -e "' + cau + '" 2>/dev/null');
const J = (x) => JSON.stringify(x);
const ngaySau = (n) => { const d = new Date(Date.now() + n * 86400000); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; };
const moiTruong = (ten) => sh('exec', 'eclinic-chatbot-service', 'printenv', ten);
const dangNhap = async (email, matKhau) => (await goi('POST', '/api/auth/login', { body: { email, matKhau } })).json.duLieu;
const ghiCuaGia = async () => (await fetch(GIA + '/ghi', { signal: AbortSignal.timeout(10000) })).json();
const xoaGhi = () => fetch(GIA + '/ghi', { method: 'DELETE', signal: AbortSignal.timeout(10000) });
const moPhien = (tuyChon) => goi('POST', '/api/chatbot/phien', tuyChon);
const gui = (idPhien, noiDung, tuyChon = {}) => goi('POST', `/api/chatbot/phien/${idPhien}/tin-nhan`, { ...tuyChon, body: { noiDung } });
const doc = (idPhien, tuyChon) => goi('GET', `/api/chatbot/phien/${idPhien}/tin-nhan`, tuyChon);
const soTin = (idPhien) => Number(sql(`select count(*) from tin_nhan_chat where id_phien_chat = ${idPhien}`));
const logChatbot = () => execFileSync('docker', ['logs', 'eclinic-chatbot-service'], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, stdio: ['ignore', 'pipe', 'pipe'] });
const VI_NGOAI = 'Xin lỗi, câu hỏi này nằm ngoài phạm vi hỗ trợ của tôi.';
const EN_NGOAI = 'Sorry, this question is out of scope for me.';
const la429 = (r, doan) => r.status === 429 && r.json.maLoi === 'CHAT_QUA_NHIEU' && (!doan || (r.json.thongDiep || '').includes(doan));
const laTraLoi = (r) => r.status === 200 && r.json.duLieu.traLoi.loai === 'TRA_LOI';

async function taiKhoan() {
  const a = await dangNhap('benhnhan01@eclinic.local', 'Demo@123');
  const b = await dangNhap('bacsi01@eclinic.local', 'Demo@123');
  const q = await dangNhap(sh('exec', 'eclinic-identity-service', 'printenv', 'ADMIN_EMAIL'), sh('exec', 'eclinic-identity-service', 'printenv', 'ADMIN_PASSWORD'));
  return { A: a.accessToken, B: b.accessToken, Q: q.accessToken, tkA: a.taiKhoan };
}

async function chang1() {
  const { A, B, Q, tkA } = await taiKhoan();
  let r;
  console.log('1.0 Chuẩn bị');
  kiem('V22 đã chạy', sql("select count(*) from flyway_schema_history where version = '22' and success = 1") === '1');
  kiem('chatbot-service dùng mô hình giả', moiTruong('CHATBOT_BASE_URL').includes('mo-hinh-gia'), moiTruong('CHATBOT_BASE_URL'));
  await xoaGhi();

  console.log('1.1 Khách mở phiên, hỏi, đọc lại');
  const g1 = await moPhien({ lang: 'vi' });
  const P1 = g1.json.duLieu || {};
  kiem('khách mở phiên: 201, có idPhien và maKhach', g1.status === 201 && P1.idPhien > 0 && typeof P1.maKhach === 'string' && P1.maKhach.length >= 40, J(g1.json));
  const chao = (P1.tinNhan || [])[0] || {};
  kiem('câu chào [vi] của CHATBOT, loại LOI_CHAO, nói là AI và không thay bác sĩ', chao.nguoiGui === 'CHATBOT' && chao.loai === 'LOI_CHAO' && /AI/.test(chao.noiDung) && /không thay thế .*bác sĩ/.test(chao.noiDung), J(chao));
  const g2 = await moPhien({ lang: 'en' });
  const P2 = g2.json.duLieu || {};
  const chaoEn = (P2.tinNhan || [])[0] || {};
  kiem('câu chào [en] nói "AI assistant" và "does not replace ... doctor"', /AI assistant/.test(chaoEn.noiDung) && /does not replace .*doctor/.test(chaoEn.noiDung), J(chaoEn));
  const luu = sql(`select concat(ifnull(id_tai_khoan, 'null'), ':', length(ma_dinh_danh_khach), ':', ma_dinh_danh_khach) from phien_chat where id_phien_chat = ${P1.idPhien}`);
  kiem('DB chỉ lưu bản băm 64 ký tự của mã khách, không lưu mã gốc; phiên không gắn tài khoản', luu.startsWith('null:64:') && !luu.includes(P1.maKhach), luu);
  r = await gui(P1.idPhien, '  Lam sao dat lich?  ', { khach: P1.maKhach });
  kiem('gửi tin nhắn bằng mã khách: 200, nhận câu trả lời của mô hình giả, loại TRA_LOI; tin đã lưu loại CAU_HOI, đã cắt khoảng trắng',
    r.status === 200 && r.json.duLieu.traLoi.noiDung === 'TRA LOI GIA: Lam sao dat lich?' && r.json.duLieu.traLoi.loai === 'TRA_LOI' && r.json.duLieu.traLoi.nguoiGui === 'CHATBOT'
    && r.json.duLieu.tinNhan.loai === 'CAU_HOI' && r.json.duLieu.tinNhan.nguoiGui === 'NGUOI_DUNG' && r.json.duLieu.tinNhan.noiDung === 'Lam sao dat lich?', J(r.json));
  r = await doc(P1.idPhien, { khach: P1.maKhach });
  kiem('lịch sử: LOI_CHAO, CAU_HOI, TRA_LOI đúng thứ tự, có id và ngayTao', r.status === 200 && J(r.json.duLieu.map((t) => t.loai)) === J(['LOI_CHAO', 'CAU_HOI', 'TRA_LOI']) && r.json.duLieu.every((t) => t.id > 0 && t.ngayTao), J(r.json));

  console.log('1.2 Chủ phiên');
  const a1 = await moPhien({ token: A });
  const PA = a1.json.duLieu || {};
  kiem('tài khoản A mở phiên: 201, KHÔNG có maKhach', a1.status === 201 && PA.idPhien > 0 && PA.maKhach === undefined, J(a1.json));
  kiem('phiên của A gắn tài khoản, không có mã khách', sql(`select concat(id_tai_khoan, ':', ifnull(ma_dinh_danh_khach, 'null')) from phien_chat where id_phien_chat = ${PA.idPhien}`) === tkA.id + ':null');
  const b1 = await moPhien({ token: B });
  const PB = b1.json.duLieu || {};
  kiem('bác sĩ (vai trò khác) cũng mở được phiên', b1.status === 201 && PB.idPhien > 0, J(b1.json));
  await xoaGhi();
  const truocA = soTin(PA.idPhien), truocG = soTin(P1.idPhien);
  const sai = [
    ['phiên khách, không gửi mã', P1.idPhien, {}],
    ['phiên khách, mã sai', P1.idPhien, { khach: 'x'.repeat(43) }],
    ['phiên khách, mã của khách khác', P1.idPhien, { khach: P2.maKhach }],
    ['phiên khách, tài khoản A không có mã', P1.idPhien, { token: A }],
    ['phiên của A, tài khoản B', PA.idPhien, { token: B }],
    ['phiên của A, quản trị viên', PA.idPhien, { token: Q }],
    ['phiên của A, không đăng nhập', PA.idPhien, {}],
    ['phiên của A, khách cầm mã của phiên khác', PA.idPhien, { khach: P1.maKhach }],
    ['phiên không tồn tại', 99999999, { token: A, khach: P1.maKhach }],
  ];
  const thongDiep = new Set();
  for (const [ten, id, tuyChon] of sai) {
    const s = await gui(id, 'thu vao phien nguoi khac', tuyChon);
    const d = await doc(id, tuyChon);
    thongDiep.add(s.json.thongDiep); thongDiep.add(d.json.thongDiep);
    kiem(`${ten}: gửi và đọc đều 404 KHONG_TIM_THAY`, s.status === 404 && d.status === 404 && s.json.maLoi === 'KHONG_TIM_THAY' && d.json.maLoi === 'KHONG_TIM_THAY' && !d.json.duLieu, J(s.json) + J(d.json));
  }
  kiem('mọi trường hợp sai trả cùng 1 thông điệp', thongDiep.size === 1 && [...thongDiep][0] === 'Không tìm thấy phiên chat', J([...thongDiep]));
  kiem('các lần gửi bị từ chối: không lưu gì, mô hình không được gọi', soTin(PA.idPhien) === truocA && soTin(P1.idPhien) === truocG && (await ghiCuaGia()).length === 0);
  r = await gui(PA.idPhien, 'cau hoi cua A', { token: A });
  kiem('A gửi vào phiên của A: 200', laTraLoi(r), J(r.json));
  r = await gui(P1.idPhien, 'khach da dang nhap', { token: A, khach: P1.maKhach });
  kiem('người đã đăng nhập cầm đúng mã khách dùng tiếp được phiên khách', laTraLoi(r), J(r.json));
  r = await goi('GET', '/api/chatbot/phien');
  kiem('GET /api/chatbot/phien không JWT: 401', r.status === 401, J(r.json));
  r = await goi('GET', '/api/chatbot/phien', { token: A });
  const cuaA = sql(`select group_concat(id_phien_chat order by id_phien_chat desc) from phien_chat where id_tai_khoan = ${tkA.id}`);
  kiem('GET /api/chatbot/phien của A: chỉ các phiên của A, mới nhất trước', r.status === 200 && r.json.duLieu.map((p) => p.idPhien).join(',') === cuaA && !cuaA.split(',').includes(String(P1.idPhien)) && !cuaA.split(',').includes(String(PB.idPhien)), J(r.json) + ' / ' + cuaA);
  r = await goi('POST', '/api/chatbot/phien', { token: 'khong.phai.jwt' });
  kiem('JWT hỏng trên đường dẫn công khai: 401, không thành khách', r.status === 401, J(r.json));

  console.log('1.3 Nội dung gửi cho mô hình');
  const khac = (await moPhien({})).json.duLieu;
  await gui(khac.idPhien, 'BI MAT PHIEN KHAC', { khach: khac.maKhach });
  const g3 = (await moPhien({ lang: 'en' })).json.duLieu;
  for (let i = 1; i <= 7; i++) await gui(g3.idPhien, 'cau ' + i, { khach: g3.maKhach, lang: 'en' });
  let ban = (await ghiCuaGia()).pop();
  const tin = ban.body.messages || [];
  kiem('khoá API ở header Authorization: Bearer; model lấy từ cấu hình; có max_tokens và reasoning_effort',
    ban.authorization === 'Bearer ' + KHOA_GIA && ban.body.model === moiTruong('CHATBOT_MODEL') && ban.body.max_tokens > 0 && ban.body.reasoning_effort === 'low', J({ ...ban, body: { ...ban.body, messages: '...' } }));
  kiem('tin đầu là system: có luật dấu hiệu [NGOAI_PHAM_VI], cấm chẩn đoán, và "Reply in English" theo ngôn ngữ của request',
    tin[0].role === 'system' && tin[0].content.includes('reply with exactly [NGOAI_PHAM_VI]') && tin[0].content.includes('Never diagnose') && tin[0].content.includes('Reply in English') && !tin[0].content.includes('{{'), tin[0].content);
  const hoiThoai = tin.slice(1);
  kiem('sau system: nhiều nhất 10 tin lịch sử + câu hỏi mới, xen kẽ user / assistant, kết thúc bằng "cau 7"',
    hoiThoai.length <= 11 && hoiThoai.length >= 9 && hoiThoai.every((m, i) => m.role === (i % 2 === 0 ? 'user' : 'assistant')) && hoiThoai[hoiThoai.length - 1].content === 'cau 7', J(hoiThoai));
  kiem('lịch sử chỉ của phiên này: không có tin của phiên khác, không có câu chào', !J(tin.slice(1)).includes('BI MAT PHIEN KHAC') && !J(tin.slice(1)).includes('AI assistant of eClinic') && hoiThoai.filter((m) => m.role === 'user').every((m) => /^cau \d$/.test(m.content)), J(hoiThoai));
  await gui(PA.idPhien, 'cau thu hai cua A', { token: A, lang: 'vi' });
  ban = (await ghiCuaGia()).pop();
  const chuoi = J(ban.body);
  kiem('request của tài khoản A [vi]: "Reply in Vietnamese"; không có email, họ tên, số điện thoại, id tài khoản của A',
    ban.body.messages[0].content.includes('Reply in Vietnamese') && ![tkA.email, tkA.hoTen, tkA.soDienThoai].filter(Boolean).some((x) => chuoi.includes(x)) && !/"(user|id|idTaiKhoan|email)"\s*:/.test(chuoi) && J(Object.keys(ban.body).sort()) === J(['max_tokens', 'messages', 'model', 'reasoning_effort', 'tool_choice', 'tools']), chuoi);

  console.log('1.4 Ngoài phạm vi');
  const g4 = (await moPhien({})).json.duLieu;
  for (const [cau, lang, mong] of [['mua giay o dau #ngoai', 'vi', VI_NGOAI], ['write python for me #ngoai2', 'en', EN_NGOAI], ['gia vang hom nay #ngoai3', 'vi', VI_NGOAI]]) {
    r = await gui(g4.idPhien, cau, { khach: g4.maKhach, lang });
    kiem(`"${cau}" [${lang}]: câu từ chối cố định, loại NGOAI_PHAM_VI, không lộ dấu hiệu`, r.status === 200 && r.json.duLieu.traLoi.loai === 'NGOAI_PHAM_VI' && r.json.duLieu.traLoi.noiDung.startsWith(mong) && !/NGOAI_PHAM_VI\]|ngoai_pham_vi/.test(r.json.duLieu.traLoi.noiDung), J(r.json));
  }
  kiem('DB không có tin nào của chatbot chứa dấu hiệu', sql("select count(*) from tin_nhan_chat where nguoi_gui = 'CHATBOT' and (noi_dung like '%NGOAI\\_PHAM\\_VI%' or noi_dung like '%Toi khong giup duoc%')") === '0');
  r = await doc(g4.idPhien, { khach: g4.maKhach });
  kiem('đọc lại phiên: 3 câu từ chối giữ nguyên ngôn ngữ lúc trả lời (vi, en, vi)', J(r.json.duLieu.filter((t) => t.loai === 'NGOAI_PHAM_VI').map((t) => t.noiDung.startsWith(EN_NGOAI) ? 'en' : t.noiDung.startsWith(VI_NGOAI) ? 'vi' : '?')) === J(['vi', 'en', 'vi']), J(r.json));

  console.log('1.5 Cấp cứu');
  const g5 = (await moPhien({})).json.duLieu;
  await xoaGhi();
  const khanCap = [['Tôi bị đau ngực và khó thở', 'vi'], ['bo toi dau nguc, KHO THO qua', 'vi'], ['Em muốn tự tử', 'vi'], ['con toi dang bi co giat', 'vi'], ['Mẹ tôi bị méo miệng, yếu nửa người', 'vi'],
    ['My father has chest pain and is short of breath', 'en'], ['I want to kill myself', 'en'], ["She can't breathe", 'en'], ['I think he is having a stroke', 'en']];
  for (const [cau, lang] of khanCap) {
    r = await gui(g5.idPhien, cau, { khach: g5.maKhach, lang });
    const tl = (r.json.duLieu || {}).traLoi || {};
    kiem(`cấp cứu "${cau}" [${lang}]: câu cố định có 115, loại KHAN_CAP`, r.status === 200 && tl.loai === 'KHAN_CAP' && tl.noiDung.includes('115') && (lang === 'en' ? /^What you describe may be an emergency/ : /^Những dấu hiệu bạn mô tả/).test(tl.noiDung), J(r.json));
  }
  kiem('mô hình không nhận request nào cho các tin cấp cứu', (await ghiCuaGia()).length === 0);
  const thuong = ['Tôi bị đau đầu nhẹ', 'bác sĩ dặn cứ từ từ rồi sẽ khỏi', 'tôi muốn được tư vấn trước khi đặt lịch', 'I have a mild headache'];
  for (const cau of thuong) {
    r = await gui(g5.idPhien, cau, { khach: g5.maKhach });
    kiem(`không phải cấp cứu: "${cau}" đi tới mô hình`, laTraLoi(r), J(r.json));
  }
  const banGhi = await ghiCuaGia();
  kiem('mô hình nhận đúng 4 request; các tin cấp cứu và câu "gọi 115" không nằm trong lịch sử gửi đi', banGhi.length === 4 && !/dau nguc|đau ngực|kill myself|115/.test(J(banGhi.map((b) => b.body.messages.slice(1)))), J(banGhi.map((b) => b.body.messages.slice(1))));

  console.log('1.6 Mô hình lỗi (thời gian chờ ' + moiTruong('CHATBOT_TIMEOUT') + ')');
  const g6 = (await moPhien({})).json.duLieu;
  await gui(g6.idPhien, 'truoc khi loi', { khach: g6.maKhach });
  const truoc = soTin(g6.idPhien);
  for (const [cau, ten] of [['#cham doi lau', 'quá thời gian chờ'], ['#429', 'HTTP 429'], ['#500', 'HTTP 500'], ['#rong', 'trả lời rỗng']]) {
    r = await gui(g6.idPhien, cau, { khach: g6.maKhach, lang: 'en' });
    kiem(`mô hình ${ten}: 503 CHATBOT_KHONG_KHA_DUNG, thông điệp [en], trong ${r.ms} ms`, r.status === 503 && r.json.maLoi === 'CHATBOT_KHONG_KHA_DUNG' && /cannot answer right now/.test(r.json.thongDiep) && r.ms < 8000, J(r.json));
  }
  kiem('4 lần lỗi không lưu tin nhắn nào', soTin(g6.idPhien) === truoc, soTin(g6.idPhien) + ' / ' + truoc);
  r = await gui(g6.idPhien, 'sau khi loi', { khach: g6.maKhach });
  kiem('tin nhắn bình thường ngay sau đó vẫn được trả lời', laTraLoi(r) && soTin(g6.idPhien) === truoc + 2, J(r.json));

  console.log('1.7 Kiểm tra dữ liệu');
  for (const [ten, noiDung] of [['rỗng', ''], ['toàn khoảng trắng', '   \n '], ['1001 ký tự', 'a'.repeat(1001)]]) {
    r = await gui(g6.idPhien, noiDung, { khach: g6.maKhach });
    const en = await gui(g6.idPhien, noiDung, { khach: g6.maKhach, lang: 'en' });
    kiem(`tin nhắn ${ten}: 400, thông điệp theo ngôn ngữ`, r.status === 400 && en.status === 400 && /Tin nhắn/.test(J(r.json.chiTiet)) && /The message must/.test(J(en.json.chiTiet)), J(r.json) + J(en.json));
  }
  r = await goi('POST', `/api/chatbot/phien/${g6.idPhien}/tin-nhan`, { khach: g6.maKhach, body: {} });
  kiem('thiếu noiDung: 400', r.status === 400, J(r.json));
  r = await gui(g6.idPhien, 'ă'.repeat(1000), { khach: g6.maKhach });
  kiem('đúng 1000 ký tự (có dấu): 200', laTraLoi(r), J(r.json).slice(0, 300));
  r = await doc('abc', { khach: g6.maKhach });
  kiem('id phiên không phải số: 400, không phải 500', r.status === 400, J(r.json));

  console.log('1.8 Log của chatbot-service');
  const log = logChatbot();
  const dongLoi = log.split('\n').filter((d) => / ERROR /.test(d));
  kiem('không có dòng ERROR', dongLoi.length === 0, dongLoi.slice(0, 4).join('\n'));
  kiem('có cảnh báo cho các lỗi mô hình đã gây ra', /trả lỗi HTTP 429/.test(log) && /trả lỗi HTTP 500/.test(log) && /trả lời rỗng/.test(log) && /Không gọi được mô hình ngôn ngữ/.test(log));
  const lo = ['cau 7', 'BI MAT PHIEN KHAC', 'dau nguc', 'kill myself', 'Lam sao dat lich', 'truoc khi loi', KHOA_GIA, P1.maKhach, 'quota exceeded'].filter((x) => log.includes(x));
  kiem('log không chứa nội dung tin nhắn, khoá API, mã khách, body lỗi của mô hình', lo.length === 0, J(lo));

  console.log('1.9 Hồi quy: đặt lịch và chuông');
  const hauTo = Date.now().toString().slice(-6);
  const bacSi = (await goi('GET', '/api/catalog/bac-si?kichThuoc=100')).json.duLieu.noiDung;
  let cho = null;
  for (const b of bacSi) {
    for (let n = 2; n <= 14 && !cho; n++) {
      for (const ca of (await goi('GET', `/api/booking/khung-gio?ngay=${ngaySau(n)}&idBacSi=${b.id}`)).json.duLieu || []) {
        const k = ca.khungGio.find((x) => x.soChoConLai > 0);
        if (k) { cho = { idLichLamViec: ca.idLichLamViec, gioBatDauKhung: k.gioBatDau }; break; }
      }
    }
    if (cho) break;
  }
  r = cho ? await goi('POST', '/api/booking/lich-hen', { body: { ...cho, lyDoKham: 'Kham smoke-p7', benhNhan: { hoTen: 'Lê Thị Bảy', ngaySinh: '1992-07-07', gioiTinh: 'NU', soDienThoai: '0377' + hauTo, cccd: '079377' + hauTo } } }) : { status: 0, json: 'không tìm thấy khung giờ còn chỗ' };
  kiem('khách đặt lịch qua gateway (giới hạn theo IP dùng DiaChiIp đã chuyển sang common): 201', r.status === 201 && !!r.json.duLieu.maPhieuKham, J(r.json));
  r = await goi('GET', '/api/notification/thong-bao/so-chua-doc', { token: A });
  kiem('chuông của A: 200', r.status === 200, J(r.json));
}

async function chang2() {
  const { A, B } = await taiKhoan();
  let r;
  console.log('2 Giới hạn: ' + moiTruong('CHATBOT_LIMIT_MESSAGES') + ' tin / ' + moiTruong('CHATBOT_LIMIT_WINDOW') + ', ' + moiTruong('CHATBOT_LIMIT_SESSIONS') + ' phiên / giờ');
  const g1 = (await moPhien({})).json.duLieu;
  const g2 = (await moPhien({})).json.duLieu;
  for (let i = 1; i <= 3; i++) { r = await gui(g1.idPhien, 'khach ' + i, { khach: g1.maKhach }); kiem(`khách, tin ${i}: 200`, laTraLoi(r), J(r.json)); }
  const truoc = soTin(g1.idPhien);
  r = await gui(g1.idPhien, 'khach 4', { khach: g1.maKhach });
  kiem('khách, tin 4: 429 CHAT_QUA_NHIEU [vi], không lưu', la429(r, 'quá nhanh') && soTin(g1.idPhien) === truoc, J(r.json));
  r = await gui(g2.idPhien, 'phien khac cung IP', { khach: g2.maKhach, lang: 'en' });
  kiem('phiên khách khác cùng IP: cũng 429 [en] (đếm theo IP, không theo mã khách)', la429(r, 'too fast'), J(r.json));
  r = await gui(g1.idPhien, 'toi bi dau nguc va kho tho', { khach: g1.maKhach });
  kiem('người đã vượt giới hạn gửi tin cấp cứu: vẫn 429 (giới hạn của người gửi đứng trước)', la429(r), J(r.json));
  const pa = (await moPhien({ token: A })).json.duLieu;
  const pb = (await moPhien({ token: B })).json.duLieu;
  for (let i = 1; i <= 3; i++) { r = await gui(pa.idPhien, 'A ' + i, { token: A }); kiem(`tài khoản A, tin ${i}: 200 dù IP của khách đã hết lượt`, laTraLoi(r), J(r.json)); }
  r = await gui(pa.idPhien, 'A 4', { token: A });
  kiem('tài khoản A, tin 4: 429', la429(r), J(r.json));
  r = await gui(pb.idPhien, 'B 1', { token: B });
  kiem('tài khoản B vẫn được trả lời', laTraLoi(r), J(r.json));
  r = await doc(pa.idPhien, { token: A });
  kiem('đọc lịch sử không bị giới hạn', r.status === 200 && r.json.duLieu.length === 7, J(r.json).slice(0, 300));
  r = await moPhien({}); kiem('khách mở phiên thứ 3: 201', r.status === 201, J(r.json));
  r = await moPhien({}); kiem('khách mở phiên thứ 4: 201', r.status === 201, J(r.json));
  const soPhien = sql('select count(*) from phien_chat');
  r = await moPhien({});
  kiem('khách mở phiên thứ 5: 429, thông điệp về số phiên, không tạo phiên', la429(r, 'quá nhiều phiên') && sql('select count(*) from phien_chat') === soPhien, J(r.json));
  r = await moPhien({ token: A });
  kiem('tài khoản A vẫn mở được phiên', r.status === 201, J(r.json));
}

async function chang3() {
  const { A, B, Q } = await taiKhoan();
  let r;
  console.log('3 Giới hạn: ' + moiTruong('CHATBOT_LIMIT_DAILY') + ' tin / ngày, ' + moiTruong('CHATBOT_LIMIT_TOTAL_DAILY') + ' lần gọi mô hình / ngày toàn hệ thống');
  const pa = (await moPhien({ token: A })).json.duLieu;
  const pb = (await moPhien({ token: B })).json.duLieu;
  const pq = (await moPhien({ token: Q })).json.duLieu;
  const g = (await moPhien({})).json.duLieu;
  for (let i = 1; i <= 2; i++) { r = await gui(pa.idPhien, 'A ' + i, { token: A }); kiem(`A, tin ${i}: 200`, laTraLoi(r), J(r.json)); }
  r = await gui(pa.idPhien, 'A 3', { token: A });
  kiem('A, tin 3: 429 với thông điệp "hết số tin nhắn của hôm nay"', la429(r, 'hôm nay') && r.json.thongDiep.includes('Bạn đã gửi hết'), J(r.json));
  for (let i = 1; i <= 2; i++) { r = await gui(pb.idPhien, 'B ' + i, { token: B }); kiem(`B, tin ${i}: 200`, laTraLoi(r), J(r.json)); }
  r = await gui(g.idPhien, 'khach 1', { khach: g.maKhach });
  kiem('khách, tin 1: 200 (lần gọi mô hình thứ 5 của hệ thống)', laTraLoi(r), J(r.json));
  r = await gui(g.idPhien, 'khach 2', { khach: g.maKhach, lang: 'en' });
  kiem('khách, tin 2: 429 vì hệ thống hết lượt [en]', la429(r, 'no answers left for today'), J(r.json));
  r = await gui(pq.idPhien, 'quan tri 1', { token: Q });
  kiem('tài khoản chưa gửi tin nào cũng 429 vì hệ thống hết lượt', la429(r, 'Trợ lý ảo đã hết lượt'), J(r.json));
  r = await gui(pq.idPhien, 'toi bi dau nguc va kho tho', { token: Q });
  kiem('tin cấp cứu vẫn được trả lời khi hệ thống hết lượt (không cần mô hình)', r.status === 200 && r.json.duLieu.traLoi.loai === 'KHAN_CAP', J(r.json));
}

async function chang4() {
  let r;
  console.log('4 Không có khoá API');
  kiem('chatbot-service không có CHATBOT_API_KEY', moiTruong('CHATBOT_API_KEY') === '');
  await xoaGhi();
  const g = await moPhien({});
  kiem('mở phiên vẫn 201 kèm câu chào', g.status === 201 && g.json.duLieu.tinNhan[0].loai === 'LOI_CHAO', J(g.json));
  const P = g.json.duLieu;
  r = await gui(P.idPhien, 'xin chao', { khach: P.maKhach });
  kiem('gửi tin nhắn: 503 CHATBOT_KHONG_KHA_DUNG, không lưu, không gọi ra ngoài', r.status === 503 && r.json.maLoi === 'CHATBOT_KHONG_KHA_DUNG' && soTin(P.idPhien) === 1 && (await ghiCuaGia()).length === 0, J(r.json));
  r = await gui(P.idPhien, 'I cannot breathe', { khach: P.maKhach, lang: 'en' });
  kiem('tin cấp cứu vẫn được trả lời', r.status === 200 && r.json.duLieu.traLoi.loai === 'KHAN_CAP', J(r.json));
  const log = logChatbot();
  kiem('log khởi động cảnh báo chưa đặt CHATBOT_API_KEY; không có dòng ERROR', log.includes('Chưa đặt CHATBOT_API_KEY') && !/ ERROR /.test(log));
}

[chang1, chang2, chang3, chang4][CHANG - 1]().catch((e) => { hong++; console.log('  HỎNG ngoại lệ: ' + (e.stack || e)); }).finally(() => {
  console.log(`\nChặng ${CHANG}: ${dat} đạt, ${hong} hỏng`);
  process.exit(hong ? 1 : 0);
});
