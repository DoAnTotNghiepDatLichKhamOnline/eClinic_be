// Smoke phase 1 (DOANTOTNGH-9): thông điệp 2 ngôn ngữ qua gateway của stack eclinic-smoke.
const { execSync } = require('child_process');
const GOC = 'http://127.0.0.1:8080';
const CO_DAU = /[àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđĐ]/;
let dat = 0, hong = 0;
function kiem(ten, dung, chiTiet) {
  if (dung) { dat++; console.log('  ok   ' + ten); } else { hong++; console.log('  HỎNG ' + ten + (chiTiet ? '\n       ' + chiTiet : '')); }
}
async function goi(phuongThuc, duongDan, { body, token, lang } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = 'Bearer ' + token;
  if (lang) headers['Accept-Language'] = lang;
  const res = await fetch(GOC + duongDan, { method: phuongThuc, headers, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(40000) });
  let json = null; try { json = await res.json(); } catch { /* không phải JSON */ }
  return { status: res.status, json };
}
const sh = (lenh) => execSync(lenh, { encoding: 'utf8' }).trim();
const sql = (cau) => require('child_process').execFileSync('docker', ['exec', 'eclinic-mysql', 'sh', '-c', 'mysql -u$MYSQL_USER -p$MYSQL_PASSWORD $MYSQL_DATABASE -N -e "' + cau + '" 2>/dev/null'], { encoding: 'utf8' }).trim();
const tiengAnh = (s) => typeof s === 'string' && s.length > 0 && !CO_DAU.test(s);
const tiengViet = (s) => typeof s === 'string' && CO_DAU.test(s);
const hauTo = Date.now().toString().slice(-7);

(async () => {
  const adminEmail = sh('docker exec eclinic-identity-service printenv ADMIN_EMAIL');
  const adminMatKhau = sh('docker exec eclinic-identity-service printenv ADMIN_PASSWORD');

  console.log('3. Đăng nhập sai mật khẩu');
  const sai = { email: 'khong-co-' + hauTo + '@eclinic.local', matKhau: 'Sai@12345' };
  let r = await goi('POST', '/api/auth/login', { body: sai });
  kiem('không header -> câu tiếng Việt như cũ', r.status === 401 && r.json.thongDiep === 'Email hoặc mật khẩu không đúng', JSON.stringify(r.json));
  kiem('maLoi', r.json.maLoi === 'SAI_THONG_TIN_DANG_NHAP');
  r = await goi('POST', '/api/auth/login', { body: sai, lang: 'en' });
  kiem('en -> ' + r.json.thongDiep, r.status === 401 && r.json.maLoi === 'SAI_THONG_TIN_DANG_NHAP' && r.json.thongDiep === 'Incorrect email or password', JSON.stringify(r.json));
  r = await goi('POST', '/api/auth/login', { body: sai, lang: 'en-US,en;q=0.9,vi;q=0.8' });
  kiem('en-US,en;q=0.9 -> tiếng Anh', tiengAnh(r.json.thongDiep), r.json.thongDiep);
  r = await goi('POST', '/api/auth/login', { body: sai, lang: 'fr' });
  kiem('fr -> tiếng Việt', r.json.thongDiep === 'Email hoặc mật khẩu không đúng', r.json.thongDiep);

  console.log('4. Đăng ký với mật khẩu 3 ký tự');
  const dangKy = (email, matKhau, sdt) => ({ hoTen: 'Smoke Ngon Ngu', email, matKhau, soDienThoai: sdt });
  r = await goi('POST', '/api/auth/register', { body: dangKy('a' + hauTo + '@eclinic.local', '123', '09' + hauTo + '1'), lang: 'en' });
  let ct = (r.json.chiTiet || []).map((c) => c.thongDiep).join(' | ');
  kiem('en -> ' + ct, r.status === 400 && tiengAnh(ct) && ct.includes('6') && !ct.includes('{'), JSON.stringify(r.json));
  kiem('thongDiep chung tiếng Anh: ' + r.json.thongDiep, r.json.thongDiep === 'Invalid data');
  r = await goi('POST', '/api/auth/register', { body: dangKy('a' + hauTo + '@eclinic.local', '123', '09' + hauTo + '1') });
  ct = (r.json.chiTiet || []).map((c) => c.thongDiep).join(' | ');
  kiem('không header -> ' + ct, ct === 'Mật khẩu tối thiểu 6 ký tự', JSON.stringify(r.json));
  r = await goi('POST', '/api/auth/register', { body: { email: 'x', matKhau: '123456', soDienThoai: '1' }, lang: 'en' });
  ct = (r.json.chiTiet || []).map((c) => `${c.truong}: ${c.thongDiep}`).sort().join(' | ');
  kiem('en, nhiều trường -> ' + ct, r.status === 400 && tiengAnh(ct) && (r.json.chiTiet || []).length >= 3, JSON.stringify(r.json));

  console.log('5. Ràng buộc không ghi message (kichThuoc=0)');
  r = await goi('GET', '/api/catalog/chuyen-khoa?kichThuoc=0');
  ct = (r.json.chiTiet || []).map((c) => c.thongDiep).join(' | ');
  kiem('không header -> ' + ct, r.status === 400 && tiengViet(ct) && !ct.includes('{'), JSON.stringify(r.json));
  r = await goi('GET', '/api/catalog/chuyen-khoa?kichThuoc=0', { lang: 'en' });
  ct = (r.json.chiTiet || []).map((c) => c.thongDiep).join(' | ');
  kiem('en -> ' + ct, r.status === 400 && tiengAnh(ct) && !ct.includes('{'), JSON.stringify(r.json));
  r = await goi('GET', '/api/catalog/chuyen-khoa?trang=abc', { lang: 'en' });
  kiem('en, sai kiểu (có dấu nháy) -> ' + r.json.thongDiep, r.status === 400 && r.json.thongDiep === "Parameter 'trang' has the wrong data type", JSON.stringify(r.json));
  r = await goi('GET', '/api/catalog/chuyen-khoa?trang=abc');
  kiem('không header -> ' + r.json.thongDiep, r.json.thongDiep === "Tham số 'trang' sai kiểu dữ liệu", JSON.stringify(r.json));

  console.log('6. 401 từ filter bảo mật');
  r = await goi('GET', '/api/users/me', { lang: 'en' });
  kiem('en -> ' + r.json.thongDiep, r.status === 401 && r.json.maLoi === 'CHUA_DANG_NHAP' && tiengAnh(r.json.thongDiep), JSON.stringify(r.json));
  r = await goi('GET', '/api/users/me');
  kiem('không header -> ' + r.json.thongDiep, r.json.thongDiep === 'Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn', JSON.stringify(r.json));

  console.log('7. 404 của gateway');
  r = await goi('GET', '/khong-co', { lang: 'en' });
  kiem('en -> ' + r.json.thongDiep, r.status === 404 && r.json.maLoi === 'KHONG_TIM_THAY' && tiengAnh(r.json.thongDiep), JSON.stringify(r.json));
  r = await goi('GET', '/khong-co');
  kiem('không header -> ' + r.json.thongDiep, r.json.thongDiep === 'Không tìm thấy đường dẫn yêu cầu', JSON.stringify(r.json));

  console.log('11a. Đăng nhập trả ngonNgu');
  r = await goi('POST', '/api/auth/login', { body: { email: adminEmail, matKhau: adminMatKhau }, lang: 'en' });
  kiem('đăng nhập admin, en -> ' + r.json.thongDiep, r.status === 200 && r.json.thongDiep === 'Signed in', JSON.stringify(r.json).slice(0, 300));
  const admin = r.json.duLieu.accessToken;
  kiem('taiKhoan.ngonNgu = ' + r.json.duLieu.taiKhoan.ngonNgu, r.json.duLieu.taiKhoan.ngonNgu === 'VI');

  console.log('8. Thông điệp có tham số');
  r = await goi('GET', '/api/catalog/chuyen-khoa?kichThuoc=1');
  const idChuyenKhoa = r.json.duLieu.noiDung[0].id;
  r = await goi('DELETE', '/api/catalog/chuyen-khoa/' + idChuyenKhoa, { token: admin, lang: 'en' });
  kiem('xoá chuyên khoa đang dùng, en -> ' + r.json.thongDiep, r.status === 409 && tiengAnh(r.json.thongDiep) && /\d/.test(r.json.thongDiep) && !r.json.thongDiep.includes('{'), JSON.stringify(r.json));
  r = await goi('DELETE', '/api/catalog/chuyen-khoa/' + idChuyenKhoa, { token: admin });
  kiem('không header -> ' + r.json.thongDiep, r.status === 409 && /^Chuyên khoa đang có \d+ phòng khám và \d+ bác sĩ, không thể xoá$/.test(r.json.thongDiep), JSON.stringify(r.json));
  r = await goi('GET', '/api/catalog/chuyen-khoa/999999', { lang: 'en' });
  kiem('không tìm thấy, en -> ' + r.json.thongDiep, r.status === 404 && r.json.thongDiep === 'ChuyenKhoa not found (id = 999999)', JSON.stringify(r.json));
  r = await goi('GET', '/api/catalog/chuyen-khoa/999999');
  kiem('không header -> ' + r.json.thongDiep, r.json.thongDiep === 'ChuyenKhoa không tồn tại (id = 999999)', JSON.stringify(r.json));
  r = await goi('DELETE', '/api/users/me/sessions', { token: admin, lang: 'en' });
  kiem('thành công có tham số, en -> ' + r.json.thongDiep, r.status === 200 && /^Signed out \d+ other device\(s\)$/.test(r.json.thongDiep), JSON.stringify(r.json));
  r = await goi('DELETE', '/api/users/me/sessions', { token: admin });
  kiem('không header -> ' + r.json.thongDiep, /^Đã đăng xuất \d+ thiết bị khác$/.test(r.json.thongDiep), JSON.stringify(r.json));

  console.log('9. Thông điệp thành công');
  r = await goi('POST', '/api/catalog/chuyen-khoa', { token: admin, lang: 'en', body: { tenChuyenKhoa: 'Smoke EN ' + hauTo, moTa: 'x' } });
  kiem('thêm chuyên khoa, en -> ' + r.json.thongDiep, r.status === 201 || (r.status === 200 && r.json.thongDiep === 'Specialty added'), JSON.stringify(r.json));
  kiem('câu tiếng Anh', r.json.thongDiep === 'Specialty added', r.json.thongDiep);
  const idMoi1 = r.json.duLieu && r.json.duLieu.id;
  r = await goi('POST', '/api/catalog/chuyen-khoa', { token: admin, body: { tenChuyenKhoa: 'Smoke VI ' + hauTo, moTa: 'x' } });
  kiem('không header -> ' + r.json.thongDiep, r.json.thongDiep === 'Đã thêm chuyên khoa', JSON.stringify(r.json));
  const idMoi2 = r.json.duLieu && r.json.duLieu.id;
  r = await goi('POST', '/api/catalog/chuyen-khoa', { token: admin, lang: 'en', body: { tenChuyenKhoa: 'Smoke VI ' + hauTo } });
  kiem('tên trùng, en -> ' + r.json.thongDiep, r.status === 409 && r.json.thongDiep === 'This specialty name already exists', JSON.stringify(r.json));
  for (const id of [idMoi1, idMoi2]) if (id) await goi('DELETE', '/api/catalog/chuyen-khoa/' + id, { token: admin });

  console.log('10. Lỗi chuyển từ identity qua catalog');
  const bacSi = { hoTen: 'Smoke Bac Si', email: 'bacsi01@eclinic.local', soDienThoai: '08' + hauTo + '2', idChuyenKhoa, soGiayPhep: 'SMK-' + hauTo, hocVi: 'BS', soNamKinhNghiem: 1 };
  r = await goi('POST', '/api/catalog/quan-tri/bac-si', { token: admin, lang: 'en', body: bacSi });
  kiem('email đã có, en -> ' + r.json.thongDiep, r.status === 409 && r.json.maLoi === 'EMAIL_DA_TON_TAI' && tiengAnh(r.json.thongDiep), JSON.stringify(r.json));
  r = await goi('POST', '/api/catalog/quan-tri/bac-si', { token: admin, body: bacSi });
  kiem('không header -> ' + r.json.thongDiep, r.status === 409 && tiengViet(r.json.thongDiep), JSON.stringify(r.json));

  console.log('11b. Ngôn ngữ ưa dùng của tài khoản');
  const emailMoi = 'en' + hauTo + '@eclinic.local';
  r = await goi('POST', '/api/auth/register', { body: dangKy(emailMoi, 'Smoke@123', '07' + hauTo + '3'), lang: 'en' });
  kiem('đăng ký, en -> ' + r.json.thongDiep, r.status < 300 && tiengAnh(r.json.thongDiep) && r.json.duLieu.ngonNgu === 'EN', JSON.stringify(r.json));
  kiem('tai_khoan.ngon_ngu trong DB = EN', sql(`select ngon_ngu from tai_khoan where email='${emailMoi}'`) === 'EN');
  const emailVi = 'vi' + hauTo + '@eclinic.local';
  r = await goi('POST', '/api/auth/register', { body: dangKy(emailVi, 'Smoke@123', '07' + hauTo + '4') });
  kiem('đăng ký không header -> DB = VI', sql(`select ngon_ngu from tai_khoan where email='${emailVi}'`) === 'VI', JSON.stringify(r.json));
  r = await goi('PUT', '/api/users/me/language', { token: admin, lang: 'en', body: { ngonNgu: 'EN' } });
  kiem('PUT language EN -> ' + r.json.thongDiep, r.status === 200 && r.json.duLieu.ngonNgu === 'EN' && r.json.thongDiep === 'Language saved', JSON.stringify(r.json));
  r = await goi('GET', '/api/users/me', { token: admin });
  kiem('GET me -> ngonNgu EN', r.json.duLieu.ngonNgu === 'EN');
  r = await goi('PUT', '/api/users/me/language', { token: admin, body: { ngonNgu: 'FR' } });
  kiem('giá trị lạ -> 400', r.status === 400, JSON.stringify(r.json));
  r = await goi('PUT', '/api/users/me/language', { token: admin, body: {} });
  kiem('thiếu giá trị -> 400 ' + JSON.stringify(r.json.chiTiet), r.status === 400);
  r = await goi('PUT', '/api/users/me/language', { token: admin, body: { ngonNgu: 'VI' } });
  kiem('đặt lại VI', r.status === 200 && r.json.duLieu.ngonNgu === 'VI' && r.json.thongDiep === 'Đã lưu ngôn ngữ', JSON.stringify(r.json));

  console.log('12. Luồng cũ không gửi Accept-Language');
  r = await goi('POST', '/api/auth/login', { body: { email: 'benhnhan01@eclinic.local', matKhau: 'Demo@123' } });
  kiem('bệnh nhân đăng nhập -> ' + r.json.thongDiep, r.status === 200 && r.json.thongDiep === 'Đăng nhập thành công', JSON.stringify(r.json).slice(0, 200));
  const bn = r.json.duLieu.accessToken;
  r = await goi('GET', '/api/catalog/bac-si?kichThuoc=3');
  kiem('danh sách bác sĩ', r.status === 200 && r.json.duLieu.noiDung.length > 0);
  const idBacSi = r.json.duLieu.noiDung[0].id;
  r = await goi('GET', '/api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa=' + r.json.duLieu.noiDung[0].idChuyenKhoa);
  const som = (r.json.duLieu || []).find((x) => x.idBacSi === idBacSi) || (r.json.duLieu || [])[0];
  r = await goi('GET', `/api/booking/khung-gio?ngay=${som.ngay}&idBacSi=${som.idBacSi}`);
  const ca = r.json.duLieu[0];
  const khung = ca.khungGio.find((k) => !k.hetCho);
  kiem('có khung giờ trống', !!khung, JSON.stringify(ca).slice(0, 300));
  const datLich = {
    idLichLamViec: ca.idLichLamViec, gioBatDauKhung: khung.gioBatDau, lyDoKham: 'Smoke phase 1', datChoBanThan: false,
    benhNhan: { hoTen: 'Nguoi Than Smoke', ngaySinh: '1990-05-05', gioiTinh: 'NAM', soDienThoai: '03' + hauTo + '5', cccd: '0790' + hauTo + '1' },
  };
  r = await goi('POST', '/api/booking/lich-hen', { token: bn, body: datLich });
  kiem('đặt lịch -> ' + r.json.thongDiep, r.status < 300 && r.json.thongDiep === 'Đặt lịch thành công', JSON.stringify(r.json).slice(0, 400));
  const maPhieu = r.json.duLieu && r.json.duLieu.maPhieuKham;
  r = await goi('GET', '/api/booking/phieu-kham/' + maPhieu);
  kiem('mở phiếu khám', r.status === 200 && r.json.duLieu, JSON.stringify(r.json).slice(0, 200));
  r = await goi('POST', '/api/booking/lich-hen', { token: bn, body: datLich, lang: 'en' });
  kiem('đặt trùng giờ, en -> ' + r.json.thongDiep, r.status === 409 && tiengAnh(r.json.thongDiep), JSON.stringify(r.json));
  r = await goi('POST', '/api/booking/lich-hen', { token: bn, body: datLich });
  kiem('đặt trùng giờ, không header -> ' + r.json.thongDiep, r.status === 409 && tiengViet(r.json.thongDiep), JSON.stringify(r.json));

  console.log(`\n${dat} đạt, ${hong} hỏng`);
  process.exit(hong ? 1 : 0);
})().catch((e) => { console.error('Smoke dừng giữa chừng:', e); process.exit(2); });
