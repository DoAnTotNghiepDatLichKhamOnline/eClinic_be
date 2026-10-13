// Smoke phase 2 (DOANTOTNGH-9): nội dung danh mục 2 ngôn ngữ qua gateway của stack eclinic-smoke.
const { execFileSync } = require('child_process');
const GOC = 'http://127.0.0.1:8080';
const CO_DAU = /[àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđĐ]/;
let dat = 0, hong = 0;
function kiem(ten, dung, chiTiet) {
  if (dung) { dat++; console.log('  ok   ' + ten); } else { hong++; console.log('  HỎNG ' + ten + (chiTiet ? '\n       ' + String(chiTiet).slice(0, 600) : '')); }
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
const sh = (...thamSo) => execFileSync('docker', thamSo, { encoding: 'utf8' }).trim();
const sql = (cau) => sh('exec', 'eclinic-mysql', 'sh', '-c', 'mysql -u$MYSQL_USER -p$MYSQL_PASSWORD $MYSQL_DATABASE -N -e "' + cau + '" 2>/dev/null');
const tiengAnh = (s) => typeof s === 'string' && s.length > 0 && !CO_DAU.test(s);
const tiengViet = (s) => typeof s === 'string' && CO_DAU.test(s);
const J = (x) => JSON.stringify(x);
const hauTo = Date.now().toString().slice(-7);
const CK = '/api/catalog/chuyen-khoa';
const CK_QT = '/api/catalog/quan-tri/chuyen-khoa';
const PHONG_QT = '/api/catalog/quan-tri/phong-kham';
const BS_QT = '/api/catalog/quan-tri/bac-si';
const cho = (ms) => new Promise((xong) => setTimeout(xong, ms));

(async () => {
  const adminEmail = sh('exec', 'eclinic-identity-service', 'printenv', 'ADMIN_EMAIL');
  const adminMatKhau = sh('exec', 'eclinic-identity-service', 'printenv', 'ADMIN_PASSWORD');
  const dangNhap = async (email, matKhau) => (await goi('POST', '/api/auth/login', { body: { email, matKhau } })).json.duLieu.accessToken;
  const admin = await dangNhap(adminEmail, adminMatKhau);
  const bacSi01 = await dangNhap('bacsi01@eclinic.local', 'Demo@123');
  const bn = await dangNhap('benhnhan01@eclinic.local', 'Demo@123');
  let r;

  console.log('1. Migration V17 và dữ liệu mẫu tiếng Anh');
  kiem('V17 đã chạy', sql("select count(*) from flyway_schema_history where version = '17' and success = 1") === '1');
  kiem('chuyên khoa mẫu có tên tiếng Anh trong DB', sql("select count(*) from chuyen_khoa where ten_chuyen_khoa_en = 'Cardiology' and mo_ta_en like 'Examination and follow-up%'") === '1');
  kiem('8 chuyên khoa, 8 phòng, 12 bác sĩ mẫu đều có bản tiếng Anh',
    sql('select concat((select count(*) from chuyen_khoa where ten_chuyen_khoa_en is not null and mo_ta_en is not null), 0x2f, (select count(*) from phong_kham where ten_phong_en is not null and tang_en is not null), 0x2f, (select count(*) from bac_si where chuc_vu_en is not null and gioi_thieu_ngan_en is not null and tieu_su_en is not null and qua_trinh_dao_tao_en is not null and qua_trinh_cong_tac_en is not null and linh_vuc_kham_chua_en is not null), 0x2f, (select count(*) from anh_bac_si where chu_thich_en is not null))') === '8/8/12/36',
    sql('select concat((select count(*) from chuyen_khoa where ten_chuyen_khoa_en is not null), 0x2f, (select count(*) from phong_kham where ten_phong_en is not null), 0x2f, (select count(*) from bac_si where gioi_thieu_ngan_en is not null), 0x2f, (select count(*) from anh_bac_si where chu_thich_en is not null))'));

  console.log('2. Danh sách chuyên khoa công khai');
  const vi = (await goi('GET', CK + '?kichThuoc=100')).json.duLieu.noiDung;
  const en = (await goi('GET', CK + '?kichThuoc=100', { lang: 'en' })).json.duLieu.noiDung;
  const fr = (await goi('GET', CK + '?kichThuoc=100', { lang: 'fr' })).json.duLieu.noiDung;
  kiem('không header: đúng 3 trường id, tenChuyenKhoa, moTa', vi.every((c) => Object.keys(c).join() === 'id,tenChuyenKhoa,moTa'), J(vi[0]));
  kiem('không header: tên, mô tả tiếng Việt như cũ', vi.some((c) => c.tenChuyenKhoa === 'Nội tổng quát' && c.moTa.startsWith('Khám và điều trị các bệnh nội khoa')) && vi.some((c) => c.tenChuyenKhoa === 'Tim mạch'), J(vi.map((c) => c.tenChuyenKhoa)));
  kiem('không header: thứ tự theo tên tiếng Việt', J(vi.map((c) => c.tenChuyenKhoa)) === J(['Da liễu', 'Mắt', 'Nhi khoa', 'Nội tổng quát', 'Răng Hàm Mặt', 'Sản phụ khoa', 'Tai Mũi Họng', 'Tim mạch']), J(vi.map((c) => c.tenChuyenKhoa)));
  kiem('en: tên và mô tả tiếng Anh -> ' + en.map((c) => c.tenChuyenKhoa).join(', '), en.length === 8 && en.every((c) => tiengAnh(c.tenChuyenKhoa) && tiengAnh(c.moTa)) && Object.keys(en[0]).join() === 'id,tenChuyenKhoa,moTa', J(en));
  const tenEn = en.map((c) => c.tenChuyenKhoa);
  kiem('en: sắp xếp theo tên tiếng Anh', J(tenEn) === J([...tenEn].sort((a, b) => a.localeCompare(b, 'en', { sensitivity: 'base' }))) && tenEn[0] === 'Cardiology', J(tenEn));
  kiem('en: cùng tập id với tiếng Việt', J(en.map((c) => c.id).sort()) === J(vi.map((c) => c.id).sort()));
  kiem('fr -> tiếng Việt', J(fr) === J(vi));
  const timMach = vi.find((c) => c.tenChuyenKhoa === 'Tim mạch');
  r = await goi('GET', CK + '/' + timMach.id, { lang: 'en' });
  kiem('en: chi tiết 1 chuyên khoa -> ' + r.json.duLieu.tenChuyenKhoa, r.json.duLieu.tenChuyenKhoa === 'Cardiology');

  console.log('3. Bản tiếng Anh trống thì lấy bản tiếng Việt');
  const tenVi = 'Thần kinh smoke ' + hauTo;
  r = await goi('POST', CK, { token: admin, body: { tenChuyenKhoa: tenVi, moTa: 'Mô tả tiếng Việt ' + hauTo } });
  kiem('thêm bằng body cũ (không có trường ...En) -> ' + r.status, r.status === 201 && r.json.duLieu.tenChuyenKhoa === tenVi && r.json.duLieu.tenChuyenKhoaEn === null && r.json.duLieu.moTaEn === null, J(r.json));
  const idMoi = r.json.duLieu.id;
  r = await goi('GET', CK + '/' + idMoi, { lang: 'en' });
  kiem('en đọc -> tên, mô tả tiếng Việt', r.json.duLieu.tenChuyenKhoa === tenVi && r.json.duLieu.moTa === 'Mô tả tiếng Việt ' + hauTo, J(r.json));
  const tenEnMoi = 'Neurology smoke ' + hauTo;
  r = await goi('PUT', CK + '/' + idMoi, { token: admin, body: { tenChuyenKhoa: tenVi, tenChuyenKhoaEn: '  ' + tenEnMoi + ' ', moTa: 'Mô tả tiếng Việt ' + hauTo, moTaEn: '' } });
  kiem('thêm tên tiếng Anh (cắt khoảng trắng, mô tả tiếng Anh rỗng -> null)', r.status === 200 && r.json.duLieu.tenChuyenKhoaEn === tenEnMoi && r.json.duLieu.moTaEn === null, J(r.json));
  r = await goi('GET', CK + '/' + idMoi, { lang: 'en' });
  kiem('en đọc -> tên tiếng Anh, mô tả vẫn tiếng Việt (lấy thay từng trường)', r.json.duLieu.tenChuyenKhoa === tenEnMoi && r.json.duLieu.moTa === 'Mô tả tiếng Việt ' + hauTo, J(r.json));
  r = await goi('GET', CK + '/' + idMoi);
  kiem('không header đọc -> không đổi', r.json.duLieu.tenChuyenKhoa === tenVi, J(r.json));

  console.log('4. Danh mục của quản trị viên trả cả 2 bản đã lưu');
  for (const lang of [undefined, 'en']) {
    r = await goi('GET', CK_QT + '?kichThuoc=100', { token: admin, lang });
    const dong = (r.json.duLieu ? r.json.duLieu.noiDung : []).find((c) => c.id === idMoi);
    kiem(`header ${lang || '(không)'}: tenChuyenKhoa = Việt, tenChuyenKhoaEn = Anh`, r.status === 200 && dong && dong.tenChuyenKhoa === tenVi && dong.tenChuyenKhoaEn === tenEnMoi && Object.keys(dong).join() === 'id,tenChuyenKhoa,tenChuyenKhoaEn,moTa,moTaEn,trieuChung,trieuChungEn', J(r.json).slice(0, 300));
  }
  r = await goi('GET', CK_QT, { token: bn });
  kiem('bệnh nhân gọi danh mục quản trị -> 403', r.status === 403, J(r.json));
  r = await goi('GET', CK_QT);
  kiem('không đăng nhập -> 401', r.status === 401, J(r.json));

  console.log('5. Tìm kiếm theo tên của cả 2 ngôn ngữ');
  for (const [tuKhoa, lang] of [['cardio', undefined], ['cardio', 'en'], ['tim mach', undefined], ['tim mach', 'en']]) {
    r = await goi('GET', CK + '?tuKhoa=' + encodeURIComponent(tuKhoa), { lang });
    const ds = r.json.duLieu ? r.json.duLieu.noiDung : [];
    kiem(`tuKhoa=${tuKhoa}, header ${lang || '(không)'} -> ${ds.map((c) => c.tenChuyenKhoa)}`, ds.length === 1 && ds[0].id === timMach.id, J(r.json));
  }
  r = await goi('GET', CK_QT + '?tuKhoa=neurology%20smoke%20' + hauTo, { token: admin });
  kiem('danh mục quản trị tìm theo tên tiếng Anh', r.json.duLieu.noiDung.length === 1 && r.json.duLieu.noiDung[0].id === idMoi, J(r.json));

  console.log('6. Tên tiếng Anh không trùng nhau');
  r = await goi('POST', CK, { token: admin, lang: 'en', body: { tenChuyenKhoa: 'Khoa khác ' + hauTo, tenChuyenKhoaEn: 'CARDIOLOGY' } });
  kiem('chuyên khoa trùng tên tiếng Anh, en -> ' + r.json.thongDiep, r.status === 409 && r.json.maLoi === 'TEN_CHUYEN_KHOA_DA_TON_TAI' && r.json.thongDiep === 'This English specialty name already exists', J(r.json));
  r = await goi('PUT', CK + '/' + idMoi, { token: admin, body: { tenChuyenKhoa: tenVi, tenChuyenKhoaEn: 'Cardiology' } });
  kiem('sửa thành tên tiếng Anh đã có, không header -> ' + r.json.thongDiep, r.status === 409 && r.json.maLoi === 'TEN_CHUYEN_KHOA_DA_TON_TAI' && r.json.thongDiep === 'Tên tiếng Anh của chuyên khoa đã tồn tại', J(r.json));
  r = await goi('PUT', CK + '/' + idMoi, { token: admin, body: { tenChuyenKhoa: tenVi, tenChuyenKhoaEn: tenEnMoi, moTa: 'Mô tả tiếng Việt ' + hauTo } });
  kiem('sửa giữ nguyên tên tiếng Anh của chính nó -> 200', r.status === 200, J(r.json));
  const phong = (ten, tenEn2) => ({ tenPhong: ten, tenPhongEn: tenEn2, tang: 'Tầng 9', idChuyenKhoa: idMoi });
  r = await goi('POST', PHONG_QT, { token: admin, lang: 'en', body: phong('Phòng trùng ' + hauTo, 'room 101') });
  kiem('phòng trùng tên tiếng Anh, en -> ' + r.json.thongDiep, r.status === 409 && r.json.maLoi === 'TEN_PHONG_DA_TON_TAI' && r.json.thongDiep === 'This English room name already exists', J(r.json));
  r = await goi('POST', PHONG_QT, { token: admin, body: phong('Phòng A ' + hauTo, null) });
  const phongA = r.json.duLieu;
  kiem('phòng không có tên tiếng Anh (1) -> 201, body cũ không có tangEn', r.status === 201 && phongA.tenPhongEn === null && phongA.tangEn === null, J(r.json));
  r = await goi('POST', PHONG_QT, { token: admin, body: phong('Phòng B ' + hauTo, '') });
  kiem('phòng không có tên tiếng Anh (2) -> 201', r.status === 201 && r.json.duLieu.tenPhongEn === null, J(r.json));
  r = await goi('PUT', PHONG_QT + '/' + phongA.id, { token: admin, lang: 'en', body: { tenPhong: phongA.tenPhong, tenPhongEn: 'Room A ' + hauTo, tang: 'Tầng 9', tangEn: 'Floor 9', idChuyenKhoa: idMoi } });
  kiem('sửa phòng: 2 bản đã lưu + tên chuyên khoa theo ngôn ngữ (en)', r.status === 200 && r.json.duLieu.tenPhong === phongA.tenPhong && r.json.duLieu.tenPhongEn === 'Room A ' + hauTo && r.json.duLieu.tang === 'Tầng 9' && r.json.duLieu.tangEn === 'Floor 9' && r.json.duLieu.tenChuyenKhoa === tenEnMoi, J(r.json));
  r = await goi('GET', PHONG_QT + '?tuKhoa=' + encodeURIComponent('room a ' + hauTo), { token: admin });
  kiem('danh mục phòng tìm theo tên tiếng Anh', r.json.duLieu.noiDung.length === 1 && r.json.duLieu.noiDung[0].id === phongA.id && r.json.duLieu.noiDung[0].tenChuyenKhoa === tenVi, J(r.json));
  r = await goi('GET', '/api/catalog/phong-kham?idChuyenKhoa=' + idMoi, { token: admin, lang: 'en' });
  const dsPhong = r.json.duLieu || [];
  kiem('danh sách phòng để xếp ca, en -> ' + dsPhong.map((p) => `${p.tenPhong} (${p.tang})`).join('; '), dsPhong.some((p) => p.tenPhong === 'Room A ' + hauTo && p.tang === 'Floor 9' && p.tenChuyenKhoa === tenEnMoi) && dsPhong.some((p) => p.tenPhong === 'Phòng B ' + hauTo && p.tang === 'Tầng 9'), J(r.json));

  console.log('3b. Xoá bản tiếng Anh thì lại lấy bản tiếng Việt');
  r = await goi('PUT', CK + '/' + idMoi, { token: admin, body: { tenChuyenKhoa: tenVi, tenChuyenKhoaEn: null, moTa: 'Mô tả tiếng Việt ' + hauTo, moTaEn: null } });
  r = await goi('GET', CK + '/' + idMoi, { lang: 'en' });
  kiem('gửi tên tiếng Anh null -> en đọc lại ra tên tiếng Việt', r.json.duLieu.tenChuyenKhoa === tenVi, J(r.json));

  console.log('7. Bác sĩ: danh sách và chi tiết công khai');
  const bsVi = (await goi('GET', '/api/catalog/bac-si?kichThuoc=100')).json.duLieu.noiDung;
  const bsEn = (await goi('GET', '/api/catalog/bac-si?kichThuoc=100', { lang: 'en' })).json.duLieu.noiDung;
  kiem('12 bác sĩ, cùng thứ tự; họ tên và học vị giống hệt', bsVi.length === 12 && J(bsVi.map((b) => [b.id, b.hoTen, b.hocVi, b.anhDaiDien])) === J(bsEn.map((b) => [b.id, b.hoTen, b.hocVi, b.anhDaiDien])));
  kiem('không header: chức vụ, giới thiệu, chuyên khoa tiếng Việt', bsVi.every((b) => tiengViet(b.chucVu + b.gioiThieuNgan + b.tenChuyenKhoa)), J(bsVi[0]));
  kiem('en: chức vụ, giới thiệu, chuyên khoa tiếng Anh', bsEn.every((b) => tiengAnh(b.chucVu) && tiengAnh(b.gioiThieuNgan) && tiengAnh(b.tenChuyenKhoa)), J(bsEn.filter((b) => !(tiengAnh(b.chucVu) && tiengAnh(b.gioiThieuNgan) && tiengAnh(b.tenChuyenKhoa)))));
  const idBs = bsVi[bsVi.length - 1].id;
  const ctVi = (await goi('GET', '/api/catalog/bac-si/' + idBs)).json.duLieu;
  const ctEn = (await goi('GET', '/api/catalog/bac-si/' + idBs, { lang: 'en' })).json.duLieu;
  const khoiChu = (b) => [b.chucVu, b.gioiThieuNgan, b.tieuSu, ...b.quaTrinhDaoTao, ...b.quaTrinhCongTac, ...b.linhVucKhamChua];
  kiem('chi tiết: hoTen, hocVi giống hệt; cùng tập trường', ctVi.hoTen === ctEn.hoTen && ctVi.hocVi === ctEn.hocVi && tiengViet(ctVi.hoTen) && Object.keys(ctVi).join() === Object.keys(ctEn).join(), J([ctVi.hoTen, ctEn.hoTen]));
  kiem('chi tiết không header: tiếng Việt', khoiChu(ctVi).every((s) => typeof s === 'string') && tiengViet(khoiChu(ctVi).join(' ')) && ctVi.quaTrinhDaoTao.length >= 2, J(ctVi));
  kiem('chi tiết en: tiểu sử, 3 danh sách tiếng Anh, cùng số ý', khoiChu(ctEn).every(tiengAnh) && ctEn.quaTrinhDaoTao.length === ctVi.quaTrinhDaoTao.length && ctEn.linhVucKhamChua.length === ctVi.linhVucKhamChua.length, J(khoiChu(ctEn).filter((s) => !tiengAnh(s))));
  // Chú thích ảnh 1 có họ tên bác sĩ (có dấu) nên không dùng tiengAnh() cho cả câu
  kiem('ảnh en: chú thích tiếng Anh, họ tên giữ nguyên -> ' + ctEn.anh.map((a) => a.chuThich).join(' | '), ctEn.anh.length === 3 && ctEn.anh[0].chuThich === `Dr. ${ctVi.hoTen} at the ${ctEn.tenChuyenKhoa} clinic` && ctEn.anh[1].chuThich === 'Consulting a patient' && ctEn.anh[2].chuThich.startsWith('Practising certificate no. ') && Object.keys(ctEn.anh[0]).join() === 'id,loai,url,chuThich', J(ctEn.anh));
  kiem('ảnh không header: chú thích tiếng Việt', ctVi.anh[0].chuThich.startsWith('Bác sĩ ') && ctVi.anh[1].chuThich === 'Tư vấn cho người bệnh', J(ctVi.anh));

  console.log('7b. Hồ sơ bác sĩ của quản trị viên');
  r = await goi('GET', BS_QT + '/' + idBs, { token: admin, lang: 'en' });
  const goc = r.json.duLieu;
  kiem('trả cả 2 bản (trường gốc = Việt, ...En = Anh), tên chuyên khoa theo en', r.status === 200 && goc.chucVu === ctVi.chucVu && goc.chucVuEn === ctEn.chucVu && goc.tieuSu === ctVi.tieuSu && goc.tieuSuEn === ctEn.tieuSu && J(goc.quaTrinhDaoTao) === J(ctVi.quaTrinhDaoTao) && J(goc.quaTrinhDaoTaoEn) === J(ctEn.quaTrinhDaoTao) && goc.anh[1].chuThich === 'Tư vấn cho người bệnh' && goc.anh[1].chuThichEn === 'Consulting a patient' && goc.tenChuyenKhoa === ctEn.tenChuyenKhoa, J(goc));
  const hoSo = (b) => ({ hocVi: b.hocVi, chucVu: b.chucVu, chucVuEn: b.chucVuEn, soNamKinhNghiem: b.soNamKinhNghiem, gioiThieuNgan: b.gioiThieuNgan, gioiThieuNganEn: b.gioiThieuNganEn, tieuSu: b.tieuSu, tieuSuEn: b.tieuSuEn,
    quaTrinhDaoTao: b.quaTrinhDaoTao, quaTrinhDaoTaoEn: b.quaTrinhDaoTaoEn, quaTrinhCongTac: b.quaTrinhCongTac, quaTrinhCongTacEn: b.quaTrinhCongTacEn, linhVucKhamChua: b.linhVucKhamChua, linhVucKhamChuaEn: b.linhVucKhamChuaEn });
  const chiMotTruong = { ...hoSo(goc), chucVuEn: 'Smoke Position ' + hauTo, gioiThieuNganEn: null, tieuSuEn: '  ', quaTrinhDaoTaoEn: [], quaTrinhCongTacEn: null };
  delete chiMotTruong.linhVucKhamChuaEn;
  r = await goi('PUT', BS_QT + '/' + idBs + '/ho-so', { token: admin, body: chiMotTruong });
  kiem('lưu chỉ 1 trường tiếng Anh -> 200, các bản tiếng Anh khác null / rỗng', r.status === 200 && r.json.duLieu.chucVuEn === 'Smoke Position ' + hauTo && r.json.duLieu.gioiThieuNganEn === null && r.json.duLieu.tieuSuEn === null && r.json.duLieu.quaTrinhDaoTaoEn.length === 0 && r.json.duLieu.linhVucKhamChuaEn.length === 0 && r.json.duLieu.chucVu === goc.chucVu, J(r.json));
  r = await goi('GET', '/api/catalog/bac-si/' + idBs, { lang: 'en' });
  kiem('chi tiết en: chức vụ tiếng Anh, các mục còn lại tiếng Việt', r.json.duLieu.chucVu === 'Smoke Position ' + hauTo && r.json.duLieu.gioiThieuNgan === ctVi.gioiThieuNgan && r.json.duLieu.tieuSu === ctVi.tieuSu && J(r.json.duLieu.quaTrinhDaoTao) === J(ctVi.quaTrinhDaoTao) && J(r.json.duLieu.linhVucKhamChua) === J(ctVi.linhVucKhamChua), J(r.json.duLieu));
  r = await goi('PUT', BS_QT + '/' + idBs + '/ho-so', { token: admin, body: hoSo(goc) });
  r = await goi('GET', '/api/catalog/bac-si/' + idBs, { lang: 'en' });
  kiem('ghi lại đúng dữ liệu form đọc ra -> chi tiết en như ban đầu', J(khoiChu(r.json.duLieu)) === J(khoiChu(ctEn)), J(r.json.duLieu));
  r = await goi('PUT', `${BS_QT}/${idBs}/anh/${goc.anh[1].id}`, { token: admin, body: { loai: goc.anh[1].loai, chuThich: goc.anh[1].chuThich, chuThichEn: 'Smoke caption ' + hauTo } });
  kiem('sửa chú thích tiếng Anh của ảnh', r.status === 200 && r.json.duLieu.chuThichEn === 'Smoke caption ' + hauTo && r.json.duLieu.chuThich === 'Tư vấn cho người bệnh', J(r.json));
  r = await goi('GET', '/api/catalog/bac-si/' + idBs, { lang: 'en' });
  kiem('chi tiết en thấy chú thích mới', r.json.duLieu.anh[1].chuThich === 'Smoke caption ' + hauTo, J(r.json.duLieu.anh));
  r = await goi('GET', BS_QT + '?kichThuoc=5', { token: admin, lang: 'en' });
  kiem('danh bạ bác sĩ của quản trị viên, en: tên chuyên khoa tiếng Anh', r.status === 200 && r.json.duLieu.noiDung.every((b) => tiengAnh(b.tenChuyenKhoa)), J(r.json.duLieu.noiDung.map((b) => b.tenChuyenKhoa)));
  r = await goi('PUT', BS_QT + '/' + idBs + '/ho-so', { token: admin, lang: 'en', body: { ...hoSo(goc), chucVuEn: 'x'.repeat(151) } });
  const ctLoi = (r.json.chiTiet || []).map((c) => `${c.truong}: ${c.thongDiep}`).join(' | ');
  kiem('chức vụ tiếng Anh quá 150 ký tự, en -> ' + ctLoi, r.status === 400 && ctLoi === 'chucVuEn: English position can be at most 150 characters', J(r.json));

  console.log('8. booking-service: khung giờ, đặt lịch, lịch hẹn, phiếu khám');
  r = await goi('GET', '/api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa=' + timMach.id);
  const som = r.json.duLieu[0];
  const caVi = (await goi('GET', `/api/booking/khung-gio?ngay=${som.ngay}&idBacSi=${som.idBacSi}`)).json.duLieu[0];
  const caEn = (await goi('GET', `/api/booking/khung-gio?ngay=${som.ngay}&idBacSi=${som.idBacSi}`, { lang: 'en' })).json.duLieu[0];
  kiem(`khung giờ không header: ${caVi.phongKham.tenPhong} (${caVi.phongKham.tang})`, caVi.phongKham.tenPhong === 'Phòng 105' && caVi.phongKham.tang === 'Tầng 1', J(caVi.phongKham));
  kiem(`khung giờ en: ${caEn.phongKham.tenPhong} (${caEn.phongKham.tang}); bác sĩ giống hệt`, caEn.phongKham.tenPhong === 'Room 105' && caEn.phongKham.tang === 'Floor 1' && J(caEn.bacSi) === J(caVi.bacSi) && tiengViet(caEn.bacSi.hoTen), J(caEn));
  const khung = caVi.khungGio.find((k) => !k.hetCho);
  const datLich = {
    idLichLamViec: caVi.idLichLamViec, gioBatDauKhung: khung.gioBatDau, lyDoKham: 'Đau ngực khi gắng sức (smoke phase 2)', datChoBanThan: false,
    benhNhan: { hoTen: 'Nguyễn Thị Người Thân', ngaySinh: '1990-05-05', gioiTinh: 'NU', soDienThoai: '03' + hauTo + '6', cccd: '0791' + hauTo + '2' },
  };
  r = await goi('POST', '/api/booking/lich-hen', { token: bn, body: datLich, lang: 'en' });
  kiem('đặt lịch, en -> ' + r.json.thongDiep + '; phòng ' + (r.json.duLieu && r.json.duLieu.phongKham.tenPhong), r.status === 201 && r.json.duLieu.phongKham.tenPhong === 'Room 105' && r.json.duLieu.hoTenBenhNhan === 'Nguyễn Thị Người Thân', J(r.json));
  const maPhieu = r.json.duLieu.maPhieuKham;
  const lichVi = (await goi('GET', '/api/booking/lich-hen/cua-toi/' + maPhieu, { token: bn })).json.duLieu.lichHen;
  const lichEn = (await goi('GET', '/api/booking/lich-hen/cua-toi/' + maPhieu, { token: bn, lang: 'en' })).json.duLieu.lichHen;
  kiem('lịch hẹn của tôi không header: ' + lichVi.tenChuyenKhoa + ', ' + lichVi.phongKham.tenPhong, lichVi.tenChuyenKhoa === 'Tim mạch' && lichVi.phongKham.tenPhong === 'Phòng 105', J(lichVi));
  kiem('lịch hẹn của tôi en: ' + lichEn.tenChuyenKhoa + ', ' + lichEn.phongKham.tenPhong, lichEn.tenChuyenKhoa === 'Cardiology' && lichEn.phongKham.tenPhong === 'Room 105' && lichEn.phongKham.tang === 'Floor 1', J(lichEn));
  kiem('họ tên bệnh nhân, bác sĩ và lý do khám giống hệt ở 2 ngôn ngữ', lichEn.hoTenBenhNhan === 'Nguyễn Thị Người Thân' && lichEn.hoTenBenhNhan === lichVi.hoTenBenhNhan && J(lichEn.bacSi) === J(lichVi.bacSi) && (lichEn.lyDoKham === undefined || lichEn.lyDoKham === lichVi.lyDoKham), J([lichVi, lichEn]));
  r = await goi('GET', '/api/booking/lich-hen/cua-toi?kichThuoc=50', { token: bn, lang: 'en' });
  const trongDs = (r.json.duLieu.noiDung || []).find((l) => l.maPhieuKham === maPhieu);
  kiem('danh sách lịch hẹn en', !!trongDs && trongDs.tenChuyenKhoa === 'Cardiology' && trongDs.phongKham.tenPhong === 'Room 105', J(r.json).slice(0, 400));
  const phieuVi = (await goi('GET', '/api/booking/phieu-kham/' + maPhieu)).json.duLieu;
  const phieuEn = (await goi('GET', '/api/booking/phieu-kham/' + maPhieu, { lang: 'en' })).json.duLieu;
  kiem('phiếu khám công khai en: ' + phieuEn.tenChuyenKhoa + ', ' + phieuEn.phongKham.tenPhong, phieuEn.tenChuyenKhoa === 'Cardiology' && phieuEn.phongKham.tenPhong === 'Room 105' && phieuVi.tenChuyenKhoa === 'Tim mạch' && phieuVi.phongKham.tenPhong === 'Phòng 105', J(phieuEn));
  kiem('phiếu khám: lý do khám và bác sĩ không đổi theo ngôn ngữ', phieuEn.lyDoKham === 'Đau ngực khi gắng sức (smoke phase 2)' && J(phieuEn.bacSi) === J(phieuVi.bacSi), J(phieuEn));
  r = await goi('GET', '/api/booking/thong-tin-dat-lich/cua-toi', { token: bn, lang: 'en' });
  kiem('thông tin đặt lịch (lần đặt gần nhất), en -> ' + J(r.json.duLieu && r.json.duLieu.lanDatGanNhat), r.status === 200 && (!r.json.duLieu.lanDatGanNhat || tiengAnh(r.json.duLieu.lanDatGanNhat.tenChuyenKhoa)), J(r.json));

  console.log('9. Hồ sơ của chính bác sĩ');
  r = await goi('GET', '/api/users/me', { token: bacSi01, lang: 'en' });
  const toiEn = r.json.duLieu;
  r = await goi('GET', '/api/users/me', { token: bacSi01 });
  kiem(`en: ${toiEn.bacSi.tenChuyenKhoa}; không header: ${r.json.duLieu.bacSi.tenChuyenKhoa}`, toiEn.bacSi.tenChuyenKhoa === 'General Internal Medicine' && tiengAnh(toiEn.bacSi.tieuSu) && r.json.duLieu.bacSi.tenChuyenKhoa === 'Nội tổng quát' && tiengViet(r.json.duLieu.bacSi.tieuSu) && toiEn.hoTen === r.json.duLieu.hoTen, J(toiEn));

  console.log('10. Thuốc: đơn vị theo ngôn ngữ, tên không dịch');
  const tenThuoc = 'Paracetamol smoke ' + hauTo;
  r = await goi('POST', '/api/medical/quan-tri/thuoc', { token: admin, body: { tenThuoc, donVi: 'viên', donViEn: 'tablet', moTa: 'Giảm đau, hạ sốt', moTaEn: 'Pain relief, fever reducer' } });
  kiem('thêm thuốc có bản tiếng Anh -> trả cả 2 bản', r.status === 201 && r.json.duLieu.donVi === 'viên' && r.json.duLieu.donViEn === 'tablet' && r.json.duLieu.moTa === 'Giảm đau, hạ sốt' && r.json.duLieu.moTaEn === 'Pain relief, fever reducer', J(r.json));
  r = await goi('POST', '/api/medical/quan-tri/thuoc', { token: admin, body: { tenThuoc: 'Vitamin C smoke ' + hauTo, donVi: 'gói' } });
  kiem('thêm thuốc bằng body cũ -> 201, bản tiếng Anh null', r.status === 201 && r.json.duLieu.donViEn === null && r.json.duLieu.moTaEn === null, J(r.json));
  const goiY = async (tuKhoa, lang) => ((await goi('GET', '/api/medical/bac-si/toi/thuoc?tuKhoa=' + encodeURIComponent(tuKhoa), { token: bacSi01, lang })).json.duLieu || [])[0] || {};
  const gVi = await goiY('paracetamol smoke ' + hauTo);
  const gEn = await goiY('paracetamol smoke ' + hauTo, 'en');
  kiem(`gợi ý cho bác sĩ: không header "${gVi.donVi}", en "${gEn.donVi}", tên giống hệt`, gVi.donVi === 'viên' && gEn.donVi === 'tablet' && gVi.tenThuoc === tenThuoc && gEn.tenThuoc === tenThuoc, J([gVi, gEn]));
  const g2 = await goiY('vitamin c smoke ' + hauTo, 'en');
  kiem('thuốc chưa có đơn vị tiếng Anh, en -> ' + g2.donVi, g2.donVi === 'gói', J(g2));
  r = await goi('GET', '/api/medical/quan-tri/thuoc?tuKhoa=' + encodeURIComponent('paracetamol smoke ' + hauTo), { token: admin, lang: 'en' });
  kiem('danh mục thuốc của quản trị viên, en: vẫn trả 2 bản đã lưu', r.json.duLieu.noiDung.length === 1 && r.json.duLieu.noiDung[0].donVi === 'viên' && r.json.duLieu.noiDung[0].donViEn === 'tablet', J(r.json));

  console.log('11. Thông báo (chuông) vẫn là câu tiếng Việt soạn sẵn (phase 3 chưa làm)');
  const bacSiCuaLich = sql(`select t.email from lich_hen l join bac_si b on b.id_bac_si = l.id_bac_si join tai_khoan t on t.id_tai_khoan = b.id_tai_khoan where l.ma_token_phieu_kham = '${maPhieu}'`);
  const tokenBacSi = await dangNhap(bacSiCuaLich, 'Demo@123');
  let thongBao = [];
  for (let lan = 0; lan < 20 && !thongBao.length; lan++) {
    r = await goi('GET', '/api/notification/thong-bao', { token: tokenBacSi, lang: 'en' });
    thongBao = r.json.duLieu ? (r.json.duLieu.noiDung || r.json.duLieu) : [];
    if (!thongBao.length) await cho(3000);
  }
  kiem('bác sĩ nhận thông báo đặt lịch, gọi bằng en -> ' + J(thongBao[0]).slice(0, 220), thongBao.length > 0 && tiengViet(J(thongBao[0])), J(r.json).slice(0, 400));

  console.log(`\n${dat} đạt, ${hong} hỏng`);
  process.exit(hong ? 1 : 0);
})().catch((e) => { console.error('Smoke dừng giữa chừng:', e); process.exit(2); });
