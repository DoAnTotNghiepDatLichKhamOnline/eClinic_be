// Smoke phase 8 (DOANTOTNGH-9): chatbot gợi ý từ dữ liệu phòng khám. Mô hình là mô hình GIẢ (temp/d9-p8-mo-hinh-gia.js):
// kiểm tra mọi thứ code làm (tra cứu, goiY, vòng lặp, giới hạn), KHÔNG kiểm tra được mô hình thật chọn chuyên khoa có
// đúng không. Chạy bằng temp/d9-p8-run.sh trên DB mới.
//   1 giới hạn rộng: triệu chứng (quản trị, dữ liệu mẫu), chỉ dẫn hệ thống, tra cứu bác sĩ / lịch, goiY, tham số sai,
//     vòng lặp, giới hạn toàn hệ thống, lịch sử, ngoài phạm vi, cấp cứu, booking-service tắt, dữ liệu gửi mô hình, log
//   2 CHATBOT_TOTAL_TIMEOUT=5s: mô hình chậm qua nhiều lần gọi ra 503 chứ không ra 504
// node temp/d9-p8-smoke.js <1|2>
const { execFileSync } = require('child_process');
const CHANG = Number(process.argv[2]);
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
  const batDau = Date.now();
  const res = await fetch(GOC + duongDan, { method: phuongThuc, headers, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(40000) });
  let json = null; try { json = await res.json(); } catch { /* không phải JSON */ }
  return { status: res.status, json: json || {}, ms: Date.now() - batDau };
}
const sh = (...thamSo) => execFileSync('docker', thamSo, { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 }).trim();
const sql = (cau) => sh('exec', 'eclinic-mysql', 'sh', '-c', 'mysql -u$MYSQL_USER -p$MYSQL_PASSWORD $MYSQL_DATABASE -N -e "' + cau + '" 2>/dev/null');
const J = (x) => JSON.stringify(x);
const ngaySau = (n) => { const d = new Date(Date.now() + n * 86400000); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; };
const dangNhap = async (email, matKhau) => (await goi('POST', '/api/auth/login', { body: { email, matKhau } })).json.duLieu;
const ghiCuaGia = async () => (await fetch(GIA + '/ghi', { signal: AbortSignal.timeout(10000) })).json();
const xoaGhi = () => fetch(GIA + '/ghi', { method: 'DELETE', signal: AbortSignal.timeout(10000) });
const moPhien = async (tuyChon) => (await goi('POST', '/api/chatbot/phien', tuyChon)).json.duLieu;
const gui = (P, noiDung, lang = 'vi') => goi('POST', `/api/chatbot/phien/${P.idPhien}/tin-nhan`, { khach: P.maKhach, lang, body: { noiDung } });
const doc = (P) => goi('GET', `/api/chatbot/phien/${P.idPhien}/tin-nhan`, { khach: P.maKhach });
const soTin = (P) => Number(sql(`select count(*) from tin_nhan_chat where id_phien_chat = ${P.idPhien}`));
const logChatbot = () => execFileSync('docker', ['logs', 'eclinic-chatbot-service'], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, stdio: ['ignore', 'pipe', 'pipe'] });
const demToanHeThong = () => Number(sh('exec', 'eclinic-redis', 'redis-cli', 'get', 'chat:toan-he-thong') || 0);
const tl = (r) => (r.json.duLieu || {}).traLoi || {};
const cho = (ms) => new Promise((xong) => setTimeout(xong, ms));

// Điều mong đợi, đọc qua gateway như trang đặt lịch đọc
async function bacSiMongDoi(ck, lang) {
  const ds = (await goi('GET', `/api/catalog/bac-si?idChuyenKhoa=${ck}&kichThuoc=100`, { lang })).json.duLieu.noiDung;
  const som = new Map((await goi('GET', `/api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa=${ck}`, { lang })).json.duLieu.map((n) => [n.idBacSi, n]));
  return ds.map((b) => ({ id: b.id, hoTen: b.hoTen, hocVi: b.hocVi, chucVu: b.chucVu, anhDaiDien: b.anhDaiDien, ngaySomNhat: som.has(b.id) ? som.get(b.id).ngay : null, soChoConLai: som.has(b.id) ? som.get(b.id).soChoConLai : 0 }));
}
async function lichMongDoi(bs, lang, tuNgay) {
  const ca = (await goi('GET', `/api/booking/khung-gio/nhieu-ngay?idBacSi=${bs}` + (tuNgay ? `&tuNgay=${tuNgay}` : ''), { lang })).json.duLieu;
  return ca.map((c) => ({ idBacSi: c.bacSi.id, hoTenBacSi: c.bacSi.hoTen, idLichLamViec: c.idLichLamViec, ngay: c.ngay, tenPhong: c.phongKham.tenPhong, tang: c.phongKham.tang,
    khungGio: c.khungGio.filter((k) => k.soChoConLai > 0).map((k) => ({ gioBatDau: k.gioBatDau, gioKetThuc: k.gioKetThuc, soChoConLai: k.soChoConLai })) })).filter((c) => c.khungGio.length);
}
const theoId = (ds) => [...ds].sort((a, b) => a.id - b.id);

async function chang1() {
  const q = await dangNhap(sh('exec', 'eclinic-identity-service', 'printenv', 'ADMIN_EMAIL'), sh('exec', 'eclinic-identity-service', 'printenv', 'ADMIN_PASSWORD'));
  const Q = q.accessToken;
  let r;
  console.log('1.0 Chuẩn bị');
  kiem('V23 đã chạy', sql("select count(*) from flyway_schema_history where version = '23' and success = 1") === '1');
  kiem('8 chuyên khoa mẫu đều có triệu chứng ở cả 2 ngôn ngữ', sql("select count(*) from chuyen_khoa where trieu_chung is not null and trieu_chung_en is not null and length(trieu_chung) > 20 and length(trieu_chung_en) > 20") === '8');
  await xoaGhi();

  console.log('1.1 Quản trị viên nhập triệu chứng');
  const dsQt = (await goi('GET', '/api/catalog/quan-tri/chuyen-khoa?kichThuoc=50', { token: Q })).json.duLieu.noiDung;
  kiem('danh sách quản trị trả trieuChung + trieuChungEn cho 8 chuyên khoa', dsQt.length === 8 && dsQt.every((c) => c.trieuChung && c.trieuChungEn), J(dsQt[0]));
  const noi = dsQt.find((c) => c.tenChuyenKhoaEn === 'General Internal Medicine');
  const tim = dsQt.find((c) => c.tenChuyenKhoaEn === 'Cardiology');
  const VI_MOI = 'ho, sốt, đau bụng, DAU-HIEU-RIENG-VI';
  const EN_MOI = 'cough, fever, stomach ache, DAU-HIEU-RIENG-EN';
  r = await goi('PUT', `/api/catalog/chuyen-khoa/${noi.id}`, { token: Q, body: { tenChuyenKhoa: noi.tenChuyenKhoa, tenChuyenKhoaEn: noi.tenChuyenKhoaEn, moTa: noi.moTa, moTaEn: noi.moTaEn, trieuChung: '  ' + VI_MOI + ' ', trieuChungEn: EN_MOI } });
  kiem('sửa chuyên khoa: lưu và trả lại triệu chứng 2 ngôn ngữ (đã cắt khoảng trắng)', r.status === 200 && r.json.duLieu.trieuChung === VI_MOI && r.json.duLieu.trieuChungEn === EN_MOI, J(r.json));
  r = await goi('PUT', `/api/catalog/chuyen-khoa/${noi.id}`, { token: Q, lang: 'en', body: { tenChuyenKhoa: noi.tenChuyenKhoa, tenChuyenKhoaEn: noi.tenChuyenKhoaEn, moTa: noi.moTa, moTaEn: noi.moTaEn, trieuChung: 'x'.repeat(2001), trieuChungEn: EN_MOI } });
  kiem('triệu chứng quá 2000 ký tự: 400 kèm thông điệp tiếng Anh', r.status === 400 && /Common symptoms can be at most 2000/.test(J(r.json)), J(r.json));
  r = await goi('PUT', `/api/catalog/chuyen-khoa/${tim.id}`, { token: Q, body: { tenChuyenKhoa: tim.tenChuyenKhoa, tenChuyenKhoaEn: tim.tenChuyenKhoaEn, moTa: tim.moTa, moTaEn: tim.moTaEn } });
  kiem('body cũ không có 2 trường mới vẫn lưu được (triệu chứng thành trống)', r.status === 200 && r.json.duLieu.trieuChung == null && r.json.duLieu.trieuChungEn == null, J(r.json));
  r = await goi('GET', '/api/catalog/chuyen-khoa?kichThuoc=50');
  kiem('danh sách chuyên khoa công khai giữ nguyên 3 trường, không có triệu chứng', r.status === 200 && r.json.duLieu.noiDung.every((c) => J(Object.keys(c).sort()) === J(['id', 'moTa', 'tenChuyenKhoa'])), J(r.json.duLieu.noiDung[0]));
  const congKhai = r.json.duLieu.noiDung;

  console.log('1.2 Chỉ dẫn hệ thống và công cụ gửi cho mô hình');
  const P = await moPhien({ lang: 'vi' });
  await xoaGhi();
  r = await gui(P, 'Toi bi ho CHUOI-RIENG-P8');
  let ban = (await ghiCuaGia())[0];
  let heThong = ban.body.messages[0].content;
  kiem('câu hỏi thường: 1 lần gọi mô hình, không có goiY', tl(r).loai === 'TRA_LOI' && tl(r).goiY == null && (await ghiCuaGia()).length === 1, J(r.json));
  kiem('request có 2 công cụ tim_bac_si, xem_lich_bac_si và tool_choice auto', J((ban.body.tools || []).map((t) => t.function.name)) === J(['tim_bac_si', 'xem_lich_bac_si']) && ban.body.tool_choice === 'auto' && ban.body.tools.every((t) => t.type === 'function' && t.function.parameters.type === 'object'), J(ban.body.tools));
  kiem('chỉ dẫn [vi] có đủ 8 chuyên khoa (id | tên tiếng Việt), triệu chứng vừa nhập, ngày hôm nay', congKhai.every((c) => heThong.includes(`- id ${c.id} | ${c.tenChuyenKhoa}`)) && heThong.includes('common symptoms: ' + VI_MOI) && heThong.includes('Today is ' + ngaySau(0)) && !heThong.includes('{{'), heThong.slice(1500, 3600));
  kiem('chuyên khoa không có triệu chứng: dòng của nó không có phần "common symptoms"', new RegExp(`- id ${tim.id} \\| [^\\n]*`).exec(heThong)[0].includes('treats:') && !new RegExp(`- id ${tim.id} \\| [^\\n]*`).exec(heThong)[0].includes('common symptoms'));
  await xoaGhi();
  await gui(P, 'I have a cough', 'en');
  heThong = (await ghiCuaGia())[0].body.messages[0].content;
  kiem('chỉ dẫn [en] có tên và triệu chứng tiếng Anh', heThong.includes(`- id ${noi.id} | General Internal Medicine`) && heThong.includes('common symptoms: ' + EN_MOI) && heThong.includes('Reply in English') && !heThong.includes(VI_MOI), heThong.slice(1500, 3600));

  console.log('1.3 Tra cứu bác sĩ của chuyên khoa');
  const P2 = await moPhien({ lang: 'vi' });
  await xoaGhi();
  const truoc = demToanHeThong();
  r = await gui(P2, `#bacsi ${noi.id}`);
  const g = tl(r).goiY || {};
  const mongDoi = await bacSiMongDoi(noi.id, 'vi');
  kiem('200 TRA_LOI kèm goiY.chuyenKhoa đúng id và tên tiếng Việt', r.status === 200 && tl(r).loai === 'TRA_LOI' && J(g.chuyenKhoa) === J({ id: noi.id, ten: noi.tenChuyenKhoa }), J(r.json));
  kiem('goiY.bacSi khớp danh sách công khai + ngày sớm nhất đọc qua gateway (id, tên, học vị, chức vụ, ngày, số chỗ)', mongDoi.length >= 2 && J(theoId(g.bacSi || [])) === J(theoId(mongDoi)), J(g.bacSi) + '\n' + J(mongDoi));
  kiem('bác sĩ còn chỗ sớm nhất đứng trước, không lịch đứng cuối; lichKham rỗng', (g.bacSi || []).every((b, i, ds) => i === 0 || (ds[i - 1].ngaySomNhat !== null && (b.ngaySomNhat === null || ds[i - 1].ngaySomNhat <= b.ngaySomNhat)) || (ds[i - 1].ngaySomNhat === null && b.ngaySomNhat === null)) && J(g.lichKham) === '[]', J(g));
  kiem('1 tin nhắn có tra cứu = 2 lần gọi mô hình, tính 2 vào giới hạn toàn hệ thống', (await ghiCuaGia()).length === 2 && demToanHeThong() - truoc === 2, `${truoc} -> ${demToanHeThong()}`);
  ban = (await ghiCuaGia())[1];
  const cuoi2 = ban.body.messages.slice(-2);
  kiem('lần gọi 2: gửi lại nguyên tin của mô hình (còn trường lạ extra_content) rồi 1 tin role tool đúng tool_call_id', cuoi2[0].role === 'assistant' && cuoi2[0].tool_calls[0].extra_content.google.thought_signature === 'chu-ky-gia-1' && cuoi2[0].tool_calls[0].id === 'goi-1' && cuoi2[1].role === 'tool' && cuoi2[1].tool_call_id === 'goi-1', J(cuoi2));
  const kq = JSON.parse(cuoi2[1].content);
  kiem('kết quả cho mô hình: chuyên khoa, bác sĩ kèm id, tên, ngày sớm nhất có thứ', kq.specialty.id === noi.id && kq.doctors.length === g.bacSi.length && kq.doctors.every((d, i) => d.id === g.bacSi[i].id && d.name === g.bacSi[i].hoTen) && (g.bacSi[0].ngaySomNhat === null || /^\d{4}-\d{2}-\d{2} \(.+\)$/.test(kq.doctors[0].earliestDayWithFreePlaces)), cuoi2[1].content);
  const P2en = await moPhien({ lang: 'en' });
  r = await gui(P2en, `#bacsi ${noi.id}`, 'en');
  kiem('[en] tên chuyên khoa và chức vụ bác sĩ theo tiếng Anh, tên người giữ nguyên', tl(r).goiY.chuyenKhoa.ten === 'General Internal Medicine' && J(theoId(tl(r).goiY.bacSi)) === J(theoId(await bacSiMongDoi(noi.id, 'en'))) && J(tl(r).goiY.bacSi.map((b) => b.hoTen).sort()) === J(g.bacSi.map((b) => b.hoTen).sort()), J(tl(r).goiY));

  console.log('1.4 Tra cứu lịch của bác sĩ');
  const bs = g.bacSi.find((b) => b.ngaySomNhat) || {};
  kiem('có bác sĩ mẫu còn chỗ để thử', bs.id > 0, J(g.bacSi));
  await xoaGhi();
  r = await gui(P2en, `#lich ${bs.id}`, 'en');
  let lich = await lichMongDoi(bs.id, 'en');
  kiem('[en] goiY.lichKham khớp /nhieu-ngay (ca, ngày, phòng, tầng, chỉ khung còn chỗ)', lich.length > 0 && J(tl(r).goiY.lichKham) === J(lich), J(tl(r).goiY.lichKham).slice(0, 700) + '\n' + J(lich).slice(0, 700));
  kiem('[en] phòng "Room ...", tầng "Floor ..."; goiY.chuyenKhoa lấy từ bác sĩ; bacSi rỗng', /^Room /.test(lich[0].tenPhong) && /^Floor /.test(lich[0].tang) && tl(r).goiY.chuyenKhoa.id === noi.id && J(tl(r).goiY.bacSi) === '[]', J(tl(r).goiY.chuyenKhoa));
  const kqLich = JSON.parse((await ghiCuaGia())[1].body.messages.slice(-1)[0].content);
  kiem('kết quả cho mô hình: bác sĩ, khoảng 7 ngày từ hôm nay, khung giờ dạng HH:mm-HH:mm (n free)', kqLich.doctor.id === bs.id && kqLich.from === ngaySau(0) && kqLich.to === ngaySau(6) && /^\d{2}:\d{2}-\d{2}:\d{2} \(\d+ free\)$/.test(kqLich.shiftsWithFreePlaces[0].freeTimeFrames[0]) && kqLich.shiftsWithFreePlaces[0].room === lich[0].tenPhong, J(kqLich).slice(0, 600));
  r = await gui(P2, `#lich ${bs.id} ${ngaySau(7)}`);
  lich = await lichMongDoi(bs.id, 'vi', ngaySau(7));
  kiem('[vi] có tuNgay: lịch của tuần sau khớp /nhieu-ngay?tuNgay=, phòng tiếng Việt', lich.length > 0 && J(tl(r).goiY.lichKham) === J(lich) && /^Phòng /.test(lich[0].tenPhong) && lich.every((c) => c.ngay >= ngaySau(7)), J(tl(r).goiY.lichKham).slice(0, 500));
  r = await gui(P2, `#lich ${bs.id} 2020-01-01`);
  kiem('tuNgay trong quá khứ: tính từ hôm nay', r.status === 200 && J(tl(r).goiY.lichKham) === J(await lichMongDoi(bs.id, 'vi')), J(r.json).slice(0, 400));
  await xoaGhi();
  r = await gui(P2, `#haibuoc ${noi.id} ${bs.id}`);
  let cacBan = await ghiCuaGia();
  kiem('2 lượt tra cứu nối nhau: goiY có cả bacSi và lichKham; 3 lần gọi, lần cuối tool_choice none', r.status === 200 && tl(r).goiY.bacSi.length === mongDoi.length && tl(r).goiY.lichKham.length > 0 && tl(r).goiY.chuyenKhoa.id === noi.id && cacBan.length === 3 && cacBan[0].body.tool_choice === 'auto' && cacBan[1].body.tool_choice === 'auto' && cacBan[2].body.tool_choice === 'none', J(cacBan.map((b) => b.body.tool_choice)) + J(r.json).slice(0, 300));
  await xoaGhi();
  r = await gui(P2, `#songsong ${noi.id} ${bs.id}`);
  cacBan = await ghiCuaGia();
  const cuoi3 = cacBan[1].body.messages.slice(-3);
  kiem('2 lời gọi trong 1 lượt: 2 lần gọi mô hình, 2 tin tool đúng id, goiY có cả 2 phần', cacBan.length === 2 && cuoi3[0].tool_calls.length === 2 && cuoi3[1].tool_call_id === 'goi-1' && cuoi3[2].tool_call_id === 'goi-2' && tl(r).goiY.bacSi.length > 0 && tl(r).goiY.lichKham.length > 0, J(cuoi3).slice(0, 600));

  console.log('1.5 Tham số sai, id không có thật, hàm lạ');
  for (const [cau, doan] of [[`#bacsi 999999`, 'SPECIALTIES list'], ['#bacsi abc', 'SPECIALTIES list'], ['#bacsi -1', 'SPECIALTIES list'], ['#lich 999999', 'No doctor with that id'], ['#lich abc', 'doctor id returned by'], [`#lich ${bs.id} 32-13-2026`, 'yyyy-MM-dd'], ['#la', 'Unknown function'], ['#thamsohong', 'not valid JSON']]) {
    r = await gui(P2, cau);
    kiem(`"${cau}": 200, không có goiY, mô hình nhận kết quả báo lỗi`, r.status === 200 && tl(r).loai === 'TRA_LOI' && tl(r).goiY == null && tl(r).noiDung.includes('"error"') && tl(r).noiDung.includes(doan), J(r.json).slice(0, 500));
  }

  console.log('1.6 Mô hình xin tra cứu mãi');
  const P3 = await moPhien({ lang: 'vi' });
  await xoaGhi();
  r = await gui(P3, '#mai');
  cacBan = await ghiCuaGia();
  kiem('503 CHATBOT_KHONG_KHA_DUNG sau đúng 3 lần gọi, không lưu gì', r.status === 503 && r.json.maLoi === 'CHATBOT_KHONG_KHA_DUNG' && cacBan.length === 3 && cacBan[2].body.tool_choice === 'none' && soTin(P3) === 1, J(r.json) + cacBan.length);

  console.log('1.7 Đọc lại phiên và hỏi tiếp');
  r = await doc(P2);
  const daLuu = r.json.duLieu.find((t) => t.goiY && t.goiY.bacSi.length && !t.goiY.lichKham.length);
  kiem('đọc lại phiên: tin trả lời đầu tiên vẫn kèm đúng goiY đã trả; tin của người dùng không có goiY', J(daLuu.goiY) === J(g) && r.json.duLieu.filter((t) => t.nguoiGui === 'NGUOI_DUNG').every((t) => t.goiY == null), J(daLuu));
  const P4 = await moPhien({ lang: 'vi' });
  await gui(P4, `#bacsi ${noi.id}`);
  await xoaGhi();
  r = await gui(P4, 'Thu bay thi sao?');
  ban = (await ghiCuaGia())[0];
  const cu = ban.body.messages.filter((m) => m.role === 'assistant');
  kiem('tin nhắn tiếp theo: lượt trả lời cũ gửi kèm dòng ghi chú có id chuyên khoa và id bác sĩ', cu.length === 1 && cu[0].content.includes('[DATA SHOWN TO THE USER WITH THIS ANSWER]') && cu[0].content.includes(`specialty id ${noi.id} `) && g.bacSi.every((b) => cu[0].content.includes(`id ${b.id} ${b.hoTen}`)), J(cu));
  kiem('không gửi lại lượt tra cứu cũ (không có role tool, không có tool_calls)', ban.body.messages.every((m) => m.role !== 'tool' && !m.tool_calls), J(ban.body.messages.map((m) => m.role)));
  r = await doc(P4);
  kiem('dòng ghi chú không bị lưu, không trả cho người dùng', r.json.duLieu.every((t) => !t.noiDung.includes('DATA SHOWN')) && sql(`select count(*) from tin_nhan_chat where noi_dung like '%DATA SHOWN%'`) === '0');

  console.log('1.8 Ngoài phạm vi sau khi tra cứu; cấp cứu');
  r = await gui(P4, `#ngoaisau ${noi.id}`);
  kiem('dấu hiệu ngoài phạm vi sau tra cứu: NGOAI_PHAM_VI, không có goiY, cột goi_y trống', tl(r).loai === 'NGOAI_PHAM_VI' && tl(r).goiY == null && sql(`select count(*) from tin_nhan_chat where id_tin_nhan = ${tl(r).id} and goi_y is null`) === '1', J(r.json));
  await xoaGhi();
  r = await gui(P4, 'toi bi dau nguc va kho tho');
  kiem('tin cấp cứu: KHAN_CAP, không gọi mô hình, không tra cứu', tl(r).loai === 'KHAN_CAP' && tl(r).goiY == null && (await ghiCuaGia()).length === 0, J(r.json));

  console.log('1.9 Dữ liệu gửi cho mô hình và log');
  const P5 = await moPhien({ lang: 'vi' });
  await xoaGhi();
  for (const c of congKhai) await gui(P5, `#bacsi ${c.id}`);
  await gui(P5, `#lich ${bs.id}`);
  const tatCa = J(await ghiCuaGia());
  kiem('mọi thứ mô hình nhận (8 chuyên khoa, 1 lịch): không có email, số điện thoại, số giấy phép, CCCD', tatCa.length > 5000 && !/@/.test(tatCa) && !/0\d{9}/.test(tatCa) && !/CCHN/.test(tatCa), (/.{60}(@|0\d{9}|CCHN).{60}/.exec(tatCa) || [''])[0]);
  const log = logChatbot();
  kiem('log chatbot-service không có nội dung tin nhắn, không có dòng ERROR', !log.includes('CHUOI-RIENG-P8') && !log.includes('DAU-HIEU-RIENG') && !/ ERROR /.test(log), (/.*ERROR.*/.exec(log) || [''])[0]);

  console.log('1.10 booking-service tắt');
  sh('stop', 'eclinic-booking-service');
  try {
    r = await gui(P5, `#bacsi ${noi.id}`);
    kiem('tra cứu bác sĩ khi booking-service tắt: vẫn 200, không có goiY, mô hình được báo là không tra cứu được', r.status === 200 && tl(r).loai === 'TRA_LOI' && tl(r).goiY == null && tl(r).noiDung.includes('temporarily unavailable'), J(r.json).slice(0, 500));
    r = await gui(P5, 'Lam sao dat lich?');
    kiem('câu hỏi không cần tra cứu vẫn trả lời bình thường', r.status === 200 && tl(r).noiDung === 'TRA LOI GIA: Lam sao dat lich?', J(r.json).slice(0, 300));
  } finally {
    sh('start', 'eclinic-booking-service');
    for (let i = 0; i < 60; i++) {
      if ((await goi('GET', `/api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa=${noi.id}`).catch(() => ({}))).status === 200) break;
      await cho(3000);
    }
  }
  r = await gui(P5, `#bacsi ${noi.id}`);
  kiem('booking-service chạy lại: tra cứu có goiY trở lại', r.status === 200 && (tl(r).goiY || {}).bacSi && tl(r).goiY.bacSi.length === mongDoi.length, J(r.json).slice(0, 300));
}

async function chang2() {
  const P = await moPhien({ lang: 'vi' });
  await xoaGhi();
  console.log('2.1 Tổng thời gian của 1 tin nhắn (CHATBOT_TOTAL_TIMEOUT=5s, mỗi lần gọi mô hình mất 2 giây)');
  const r = await gui(P, '#cham2');
  const soLan = (await ghiCuaGia()).length;
  kiem('503 CHATBOT_KHONG_KHA_DUNG (không phải 504 của gateway), trong vòng 8 giây, không lưu gì', r.status === 503 && r.json.maLoi === 'CHATBOT_KHONG_KHA_DUNG' && r.ms < 8000 && soTin(P) === 1, `${r.status} ${J(r.json)} ${r.ms}ms, ${soLan} lần gọi`);
  kiem('dừng vì hết thời gian hoặc hết số lần gọi (2 - 3 lần gọi mô hình)', soLan >= 2 && soLan <= 3, soLan);
  kiem('log ghi lý do, không có dòng ERROR', /hết thời gian cho 1 tin nhắn|vẫn xin tra cứu|Không gọi được mô hình/.test(logChatbot()) && !/ ERROR /.test(logChatbot()));
}

[chang1, chang2][CHANG - 1]().catch((e) => { hong++; console.log('  HỎNG ngoại lệ: ' + (e.stack || e)); }).finally(() => {
  console.log(`\nChặng ${CHANG}: ${dat} đạt, ${hong} hỏng`);
  process.exit(hong ? 1 : 0);
});
