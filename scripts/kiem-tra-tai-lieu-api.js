// Đồng bộ và kiểm tra 3 tài liệu API cho frontend trong docs/api/ (bệnh nhân / khách, bác sĩ, quản trị viên). Chạy từ
// thư mục gốc:
//   node scripts/kiem-tra-tai-lieu-api.js                 chép phần chung rồi kiểm tra mọi nhóm
//   node scripts/kiem-tra-tai-lieu-api.js --kiem-tra      chỉ kiểm tra, không ghi file (phần chung lệch -> lỗi)
//   node scripts/kiem-tra-tai-lieu-api.js --nhom chung,bac-si   chỉ lỗi của các nhóm này làm thoát với mã 1
// Thoát với mã 1 nếu có lỗi. Việc làm:
//   1. Chép docs/api/_CHUNG.md (phần sau dòng HET-GHI-CHU) vào giữa 2 dấu CHUNG:BAT-DAU / CHUNG:KET-THUC của mỗi tài liệu.
//   2. Đọc mọi route công khai của các controller (bỏ /noi-bo/**): phương thức + đường dẫn, vai trò ở @PreAuthorize của
//      method (không có thì của class), và đường dẫn không cần đăng nhập ở app.bao-mat.duong-dan-cong-khai.
//   3. Xếp route vào nhóm: có vai trò -> tài liệu của từng vai trò; không vai trò mà công khai -> bệnh nhân / khách;
//      còn lại (cần đăng nhập, mọi vai trò) -> phần chung. Ngoại lệ ở NGOAI_LE.
//   4. Đọc tiêu đề route "#### PHƯƠNG_THỨC /đường/dẫn" của tài liệu (trong phần chung thì tính cho nhóm chung) và báo:
//      route còn thiếu, route không có trong code, route ghi 2 lần trong 1 tài liệu. Route ghi thêm ở tài liệu không bắt
//      buộc thì không sao.
const fs = require('fs');
const path = require('path');

const THU_MUC_TAI_LIEU = 'docs/api';
const NGUON_CHUNG = 'docs/api/_CHUNG.md';
const HET_GHI_CHU = '<!-- HET-GHI-CHU -->';
const BAT_DAU = '<!-- CHUNG:BAT-DAU -->';
const KET_THUC = '<!-- CHUNG:KET-THUC -->';
const TAI_LIEU = {
    'benh-nhan-khach': 'API-BENH-NHAN-KHACH.md',
    'bac-si': 'API-BAC-SI.md',
    'quan-tri': 'API-QUAN-TRI.md',
};
const NHOM = ['chung', ...Object.keys(TAI_LIEU)];
const NHOM_CUA_VAI_TRO = { BENH_NHAN: 'benh-nhan-khach', BAC_SI: 'bac-si', QUAN_TRI_VIEN: 'quan-tri' };
// Route xếp nhóm khác với quy tắc chung
const NGOAI_LE = [
    {
        khop: (r) => r.duongDan.startsWith('/api/auth/'),
        nhom: ['chung'],
        lyDo: 'đăng nhập, đăng ký, quên mật khẩu... là công khai nhưng mọi vai trò đều dùng',
    },
    {
        khop: (r) => r.duongDan.startsWith('/api/chatbot/'),
        nhom: ['benh-nhan-khach'],
        lyDo: 'chatbot dành cho bệnh nhân / khách (route xem phiên của tôi chỉ cần đăng nhập)',
    },
    {
        khop: (r) => r.phuongThuc === 'POST' && r.duongDan === '/api/booking/lich-hen',
        nhom: ['benh-nhan-khach'],
        lyDo: 'đặt lịch: khách hoặc bệnh nhân (vai trò kiểm tra trong code, không ở @PreAuthorize)',
    },
];
const PHUONG_THUC = { Get: 'GET', Post: 'POST', Put: 'PUT', Delete: 'DELETE', Patch: 'PATCH' };

const chiKiemTra = process.argv.includes('--kiem-tra');
const viTriNhom = process.argv.indexOf('--nhom');
const nhomChon = viTriNhom > 0 ? (process.argv[viTriNhom + 1] || '').split(',').filter(Boolean) : NHOM;
const loiChung = [];
const loiNhom = Object.fromEntries(NHOM.map((n) => [n, []]));

nhomChon.filter((n) => !NHOM.includes(n)).forEach((n) => loiChung.push(`--nhom: không có nhóm "${n}" (${NHOM.join(', ')})`));

// ---------- 1. Phần chung ----------
const nguon = fs.readFileSync(NGUON_CHUNG, 'utf8').replace(/\r\n/g, '\n');
const viTriHetGhiChu = nguon.indexOf(HET_GHI_CHU);
if (viTriHetGhiChu < 0) loiChung.push(`${NGUON_CHUNG}: không có dòng ${HET_GHI_CHU}`);
const phanChung = nguon.slice(viTriHetGhiChu + HET_GHI_CHU.length).trim();

for (const tenFile of Object.values(TAI_LIEU)) {
    const duongDan = path.join(THU_MUC_TAI_LIEU, tenFile);
    const goc = fs.readFileSync(duongDan, 'utf8');
    const xuongDong = goc.includes('\r\n') ? '\r\n' : '\n';
    const noiDung = goc.replace(/\r\n/g, '\n');
    const dau = noiDung.indexOf(BAT_DAU);
    const cuoi = noiDung.indexOf(KET_THUC);
    if (dau < 0 || cuoi < dau) {
        loiChung.push(`${duongDan}: thiếu cặp dấu ${BAT_DAU} ... ${KET_THUC}`);
        continue;
    }
    const moi = noiDung.slice(0, dau + BAT_DAU.length) + '\n' + phanChung + '\n' + noiDung.slice(cuoi);
    if (moi === noiDung) continue;
    if (chiKiemTra) {
        loiChung.push(`${duongDan}: phần chung khác ${NGUON_CHUNG} (chạy lại không có --kiem-tra để chép)`);
    } else {
        fs.writeFileSync(duongDan, moi.replace(/\n/g, xuongDong));
        console.log(`Đã chép phần chung vào ${duongDan}`);
    }
}

// ---------- 2. Route trong code ----------
/** Bỏ chú thích; trả về [mã, khung]: khung giống mã nhưng nội dung chuỗi thay bằng dấu cách (để tìm ngoặc cho đúng). */
function boChuThich(nguonJava) {
    let ma = '';
    let khung = '';
    for (let i = 0; i < nguonJava.length;) {
        const c = nguonJava[i];
        if (nguonJava.startsWith('//', i)) {
            const het = nguonJava.indexOf('\n', i);
            i = het < 0 ? nguonJava.length : het;
        } else if (nguonJava.startsWith('/*', i)) {
            const het = nguonJava.indexOf('*/', i + 2);
            i = het < 0 ? nguonJava.length : het + 2;
        } else if (nguonJava.startsWith('"""', i)) {
            const het = nguonJava.indexOf('"""', i + 3);
            const doan = nguonJava.slice(i, het + 3);
            ma += doan;
            khung += '"' + ' '.repeat(doan.length - 2) + '"';
            i = het + 3;
        } else if (c === '"' || c === "'") {
            let j = i + 1;
            while (j < nguonJava.length && nguonJava[j] !== c) j += nguonJava[j] === '\\' ? 2 : 1;
            const doan = nguonJava.slice(i, j + 1);
            ma += doan;
            khung += c + ' '.repeat(doan.length - 2) + c;
            i = j + 1;
        } else {
            ma += c;
            khung += c;
            i++;
        }
    }
    return [ma, khung];
}

/** Đối số của annotation bắt đầu ở viTri (ngay sau tên): chuỗi trong ngoặc, '' khi không có ngoặc, null khi hỏng. */
function doiSo(ma, khung, viTri) {
    let i = viTri;
    while (/\s/.test(khung[i])) i++;
    if (khung[i] !== '(') return { doiSo: '', het: viTri };
    let sau = 0;
    for (let j = i; j < khung.length; j++) {
        if (khung[j] === '(') sau++;
        else if (khung[j] === ')' && --sau === 0) return { doiSo: ma.slice(i + 1, j), khungDoiSo: khung.slice(i + 1, j), het: j + 1 };
    }
    return null;
}

/** Đường dẫn trong đối số của @XxxMapping: '' khi không ghi; null khi không đọc được (mảng nhiều đường dẫn...). */
function duongDanTrong(ds) {
    if (!ds || !ds.doiSo.trim()) return '';
    if (ds.khungDoiSo.includes('{')) return null;
    const dau = ds.doiSo.trim().match(/^"([^"]*)"/);
    if (dau) return dau[1];
    const ten = ds.doiSo.match(/\b(?:path|value)\s*=\s*"([^"]*)"/);
    if (ten) return ten[1];
    return /\b(?:path|value)\s*=/.test(ds.doiSo) ? null : '';
}

/** Vai trò trong @PreAuthorize: mảng, hoặc null khi không đọc được. */
function vaiTroTrong(ds) {
    const bieuThuc = (ds.doiSo.match(/^\s*"(.*)"\s*$/s) || [])[1];
    const khop = bieuThuc && bieuThuc.match(/^\s*has(?:Any)?Role\(([^)]*)\)\s*$/);
    if (!khop) return null;
    return khop[1].split(',').map((s) => s.trim().replace(/^'|'$/g, ''));
}

function noiDuongDan(a, b) {
    const ketQua = ('/' + a + '/' + b).replace(/\/+/g, '/').replace(/(.)\/$/, '$1');
    return ketQua;
}

function timFile(thuMuc, ketQua = []) {
    if (!fs.existsSync(thuMuc)) return ketQua;
    for (const muc of fs.readdirSync(thuMuc, { withFileTypes: true })) {
        const day = path.join(thuMuc, muc.name);
        if (muc.isDirectory()) timFile(day, ketQua);
        else if (muc.name.endsWith('Controller.java') && path.basename(thuMuc) === 'controller') ketQua.push(day);
    }
    return ketQua;
}

/** Mẫu đường dẫn kiểu Spring (*, **) -> RegExp. Biến đường dẫn của route được coi như 1 đoạn bất kỳ. */
function mauThanhRegex(mau) {
    const thoat = mau.split(/(\/\*\*|\*)/).map((p) => {
        if (p === '/**') return '(?:/.*)?';
        if (p === '*') return '[^/]*';
        return p.replace(/[.+?^${}()|[\]\\]/g, '\\$&');
    });
    return new RegExp('^' + thoat.join('') + '$');
}

function docCongKhai(module) {
    const file = path.join(module, 'src/main/resources/application.yml');
    if (!fs.existsSync(file)) return [];
    const dong = fs.readFileSync(file, 'utf8').split(/\r?\n/);
    const ketQua = [];
    const dau = dong.findIndex((d) => /^\s*duong-dan-cong-khai:\s*$/.test(d));
    if (dau < 0) return ketQua;
    const thut = dong[dau].search(/\S/);
    for (let i = dau + 1; i < dong.length; i++) {
        const d = dong[i];
        if (!d.trim() || d.trim().startsWith('#')) continue;
        if (d.search(/\S/) <= thut && !d.trim().startsWith('-')) break;
        const muc = d.match(/^\s*-\s*(.+?)\s*$/);
        if (!muc) break;
        const phan = muc[1].replace(/^["']|["']$/g, '').split(/\s+/);
        ketQua.push(phan.length === 2 ? { phuongThuc: phan[0].toUpperCase(), mau: mauThanhRegex(phan[1]) }
            : { phuongThuc: null, mau: mauThanhRegex(phan[0]) });
    }
    return ketQua;
}

const MODULE = fs.readdirSync('.', { withFileTypes: true })
    .filter((m) => m.isDirectory() && fs.existsSync(path.join(m.name, 'src/main/java')))
    .map((m) => m.name);
const routeCode = [];
let soNoiBo = 0;

for (const module of MODULE) {
    const congKhai = docCongKhai(module);
    for (const file of timFile(path.join(module, 'src/main/java'))) {
        const [ma, khung] = boChuThich(fs.readFileSync(file, 'utf8'));
        const tenFile = path.relative('.', file).replace(/\\/g, '/');
        const lop = khung.match(/\bclass\s+\w+[^{]*\{/);
        if (!lop) {
            loiChung.push(`${tenFile}: không thấy khai báo class`);
            continue;
        }
        const dauThan = lop.index + lop[0].length;
        // Annotation của class: từ đầu file tới "class"
        let duongDanLop = '';
        let vaiTroLop = null;
        const dauLop = khung.slice(0, lop.index);
        for (const a of dauLop.matchAll(/@(RequestMapping|PreAuthorize)\b/g)) {
            const ds = doiSo(ma, khung, a.index + a[0].length);
            if (a[1] === 'RequestMapping') {
                const p = duongDanTrong(ds);
                if (p === null) loiChung.push(`${tenFile}: không đọc được @RequestMapping của class`);
                else duongDanLop = p;
            } else {
                vaiTroLop = ds && vaiTroTrong(ds);
                if (!vaiTroLop) loiChung.push(`${tenFile}: không đọc được @PreAuthorize của class`);
            }
        }
        const than = khung.slice(dauThan);
        for (const a of than.matchAll(/@(Get|Post|Put|Delete|Patch|Request)Mapping\b/g)) {
            const viTri = dauThan + a.index;
            const dong = ma.slice(0, viTri).split('\n').length;
            if (a[1] === 'Request') {
                loiChung.push(`${tenFile}:${dong}: @RequestMapping trên method chưa được hỗ trợ`);
                continue;
            }
            const ds = doiSo(ma, khung, viTri + a[0].length);
            const p = duongDanTrong(ds);
            if (p === null) {
                loiChung.push(`${tenFile}:${dong}: không đọc được đường dẫn của @${a[1]}Mapping`);
                continue;
            }
            // Vùng annotation của method: từ hết thành phần trước (}, ;, {) tới thân method
            const truoc = Math.max(khung.lastIndexOf('}', viTri), khung.lastIndexOf(';', viTri), khung.lastIndexOf('{', viTri));
            const sau = khung.indexOf('{', viTri);
            const vung = khung.slice(truoc + 1, sau);
            if ((vung.match(/@(Get|Post|Put|Delete|Patch)Mapping\b/g) || []).length > 1) {
                loiChung.push(`${tenFile}:${dong}: 1 method có nhiều @XxxMapping`);
                continue;
            }
            let vaiTro = vaiTroLop;
            const pa = vung.match(/@PreAuthorize\b/);
            if (pa) {
                const dsPa = doiSo(ma, khung, truoc + 1 + pa.index + pa[0].length);
                vaiTro = dsPa && vaiTroTrong(dsPa);
                if (!vaiTro) {
                    loiChung.push(`${tenFile}:${dong}: không đọc được @PreAuthorize của method`);
                    continue;
                }
            }
            const duongDan = noiDuongDan(duongDanLop, p);
            if (duongDan.startsWith('/noi-bo/')) {
                soNoiBo++;
                continue;
            }
            const phuongThuc = PHUONG_THUC[a[1]];
            const mauSoKhop = duongDan.replace(/\{[^}]+\}/g, 'x');
            const laCongKhai = congKhai.some((c) => (!c.phuongThuc || c.phuongThuc === phuongThuc) && c.mau.test(mauSoKhop));
            routeCode.push({ phuongThuc, duongDan, vaiTro, laCongKhai, nguon: `${tenFile}:${dong}` });
        }
    }
}

function nhomCua(r) {
    const ngoaiLe = NGOAI_LE.find((n) => n.khop(r));
    if (ngoaiLe) return ngoaiLe.nhom;
    if (r.vaiTro) {
        const nhom = r.vaiTro.map((v) => NHOM_CUA_VAI_TRO[v]);
        if (nhom.includes(undefined)) loiChung.push(`${r.nguon}: vai trò lạ ${r.vaiTro.join(', ')}`);
        return nhom.filter(Boolean);
    }
    return r.laCongKhai ? ['benh-nhan-khach'] : ['chung'];
}

const khoa = (r) => `${r.phuongThuc} ${r.duongDan}`;
const maCode = new Map();
for (const r of routeCode) {
    if (maCode.has(khoa(r))) loiChung.push(`${r.nguon}: route trùng với ${maCode.get(khoa(r)).nguon}`);
    maCode.set(khoa(r), r);
}
const canCo = Object.fromEntries(NHOM.map((n) => [n, new Set()]));
for (const r of routeCode) nhomCua(r).forEach((n) => canCo[n].add(khoa(r)));

// ---------- 3. Route trong tài liệu ----------
const TIEU_DE = /^####\s+(GET|POST|PUT|DELETE|PATCH)\s+(\/\S+)\s*$/;
function docTieuDe(noiDung) {
    const ketQua = [];
    noiDung.replace(/\r\n/g, '\n').split('\n').forEach((d, i) => {
        const k = d.match(TIEU_DE);
        if (k) ketQua.push({ khoa: `${k[1]} ${k[2]}`, dong: i + 1 });
    });
    return ketQua;
}

const daGhi = Object.fromEntries(NHOM.map((n) => [n, []]));
const dongTruocPhanChung = nguon.slice(0, nguon.indexOf(phanChung)).split('\n').length - 1;
daGhi.chung = docTieuDe(phanChung).map((t) => ({ ...t, dong: t.dong + dongTruocPhanChung, file: NGUON_CHUNG }));
for (const [nhom, tenFile] of Object.entries(TAI_LIEU)) {
    const duongDan = path.join(THU_MUC_TAI_LIEU, tenFile).replace(/\\/g, '/');
    const noiDung = fs.readFileSync(duongDan, 'utf8').replace(/\r\n/g, '\n');
    const dau = noiDung.indexOf(BAT_DAU);
    const cuoi = noiDung.indexOf(KET_THUC);
    // Bỏ phần chung (đã tính ở nhóm chung) nhưng giữ số dòng
    const ngoaiChung = dau < 0 || cuoi < 0 ? noiDung
        : noiDung.slice(0, dau) + noiDung.slice(dau, cuoi).replace(/[^\n]/g, '') + noiDung.slice(cuoi);
    daGhi[nhom] = docTieuDe(ngoaiChung).map((t) => ({ ...t, file: duongDan }));
}

// ---------- Báo cáo ----------
console.log(`Route trong code: ${routeCode.length} (bỏ qua ${soNoiBo} route /noi-bo), ${MODULE.length} module`);
for (const nhom of NHOM) {
    const coGhi = new Set(daGhi[nhom].map((t) => t.khoa));
    const thieu = [...canCo[nhom]].filter((k) => !coGhi.has(k)).sort();
    const daCo = [...canCo[nhom]].length - thieu.length;
    const daThay = new Map();
    for (const t of daGhi[nhom]) {
        if (!maCode.has(t.khoa)) loiNhom[nhom].push(`${t.file}:${t.dong}: không có trong code: ${t.khoa}`);
        if (daThay.has(t.khoa)) loiNhom[nhom].push(`${t.file}:${t.dong}: ghi 2 lần (lần đầu dòng ${daThay.get(t.khoa)}): ${t.khoa}`);
        else daThay.set(t.khoa, t.dong);
    }
    thieu.forEach((k) => loiNhom[nhom].push(`thiếu: ${k}  (${maCode.get(k).nguon})`));
    const nhan = nhomChon.includes(nhom) ? '' : ' (không xét)';
    console.log(`\n[${nhom}]${nhan} đã ghi ${daCo}/${canCo[nhom].size} route bắt buộc, ${daGhi[nhom].length} tiêu đề route`);
    loiNhom[nhom].forEach((l) => console.log('  ' + l));
}

const soLoi = loiChung.length + nhomChon.reduce((tong, n) => tong + (loiNhom[n] ? loiNhom[n].length : 0), 0);
if (loiChung.length) {
    console.log('\nLỗi chung:');
    loiChung.forEach((l) => console.log('  ' + l));
}
console.log(soLoi ? `\nCó ${soLoi} lỗi.` : '\nĐạt.');
process.exit(soLoi ? 1 : 0);
