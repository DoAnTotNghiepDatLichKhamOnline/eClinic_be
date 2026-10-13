// Kiểm tra các file thông điệp 2 ngôn ngữ (docs/QUY-UOC-CODE.md mục 4). Chạy từ thư mục gốc:
//   node scripts/kiem-tra-thong-diep.js
// Thoát với mã 1 nếu có lỗi. Kiểm tra:
//   1. Mỗi cặp <ten>.properties / <ten>_en.properties có cùng tập khoá, không khoá nào lặp lại (kể cả giữa các file).
//   2. Mỗi mã trong MaLoi có khoá loi.<MA_LOI>.
//   3. Mọi khoá dùng trong mã nguồn Java ("loi....", "thanhCong....", "yeuCau....", "duLieu....", "thongBao....", "heThong....", "email....", "chatbot....",
//      {kiemTra....}) đều có.
//   4. Dòng có {0}, {1}: không có dấu nháy đơn lẻ (MessageFormat cần ''); dòng kiemTra.* không dùng {0}.
//   5. Không còn câu tiếng Việt viết thẳng trong new LoiNghiepVu(...), message = "...", PhanHoiApi.ok(...).
const fs = require('fs');
const path = require('path');

const THU_MUC = ['common/src/main/resources/thong-diep', 'api-gateway/src/main/resources/thong-diep'];
const MODULE = ['common', 'identity-service', 'catalog-service', 'booking-service', 'medical-service', 'notification-service',
    'chatbot-service'];
const CO_DAU = /[àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđĐ]/;
const loi = [];

function docFile(duongDan) {
    const khoa = new Map();
    fs.readFileSync(duongDan, 'utf8').split(/\r?\n/).forEach((dong, i) => {
        if (!dong.trim() || dong.trimStart().startsWith('#')) return;
        const viTri = dong.indexOf('=');
        if (viTri < 1) return loi.push(`${duongDan}:${i + 1}: dòng không có dạng khoá=giá trị`);
        const ten = dong.slice(0, viTri).trim();
        const giaTri = dong.slice(viTri + 1);
        if (khoa.has(ten)) loi.push(`${duongDan}:${i + 1}: khoá lặp lại ${ten}`);
        khoa.set(ten, giaTri);
        const coThamSoSo = /\{\d+\}/.test(giaTri);
        if (ten.startsWith('kiemTra.') || ten.startsWith('jakarta.')) {
            if (coThamSoSo) loi.push(`${duongDan}:${i + 1}: ${ten} dùng {0}; thông điệp kiểm tra chỉ dùng tham số có tên`);
        } else if (coThamSoSo && /(^|[^'])'([^']|$)/.test(giaTri)) {
            loi.push(`${duongDan}:${i + 1}: ${ten} có {0} nên dấu nháy đơn phải viết thành ''`);
        }
    });
    return khoa;
}

// 1. Các cặp file
const tatCa = new Map();   // khoá -> file tiếng Việt chứa nó (trong cùng 1 thư mục)
for (const thuMuc of THU_MUC) {
    const trongThuMuc = new Map();
    for (const ten of fs.readdirSync(thuMuc).filter((t) => t.endsWith('.properties') && !t.endsWith('_en.properties'))) {
        const vi = path.join(thuMuc, ten);
        const en = path.join(thuMuc, ten.replace('.properties', '_en.properties'));
        if (!fs.existsSync(en)) { loi.push(`Thiếu file ${en}`); continue; }
        const khoaVi = docFile(vi);
        const khoaEn = docFile(en);
        for (const k of khoaVi.keys()) {
            if (!khoaEn.has(k)) loi.push(`${en}: thiếu khoá ${k}`);
            if (trongThuMuc.has(k)) loi.push(`${vi}: khoá ${k} đã có ở ${trongThuMuc.get(k)}`);
            trongThuMuc.set(k, vi);
        }
        for (const k of khoaEn.keys()) if (!khoaVi.has(k)) loi.push(`${vi}: thiếu khoá ${k}`);
    }
    if (thuMuc.startsWith('common')) trongThuMuc.forEach((v, k) => tatCa.set(k, v));
}

// 2. MaLoi
const maLoi = fs.readFileSync('common/src/main/java/iuh/fit/se/eclinic/common/exception/MaLoi.java', 'utf8');
for (const m of maLoi.matchAll(/^\s{4}([A-Z][A-Z_0-9]+)\(HttpStatus\./gm)) {
    if (!tatCa.has('loi.' + m[1])) loi.push(`Mã lỗi ${m[1]} chưa có khoá loi.${m[1]} trong thong-diep/chung.properties`);
}

// 3 + 5. Mã nguồn Java
function* tepJava(thuMuc) {
    if (!fs.existsSync(thuMuc)) return;
    for (const e of fs.readdirSync(thuMuc, { withFileTypes: true })) {
        const p = path.join(thuMuc, e.name);
        if (e.isDirectory()) yield* tepJava(p); else if (p.endsWith('.java')) yield p;
    }
}
const KHOA_TRONG_MA = /"(?:\{((?:kiemTra)\.[A-Za-z0-9_.]+)\}|((?:loi|thanhCong|yeuCau|duLieu|thongBao|heThong|email|chatbot)\.[A-Za-z0-9_.]+))"/g;
const CAU_VIET_THANG = /(?:new LoiNghiepVu\([^;]*?|message\s*=\s*|PhanHoiApi\.ok\([^;]*?)"((?:[^"\\]|\\.)*)"/g;
for (const module of MODULE) {
    for (const tep of tepJava(path.join(module, 'src/main/java'))) {
        const nguon = fs.readFileSync(tep, 'utf8');
        const soDong = (viTri) => nguon.slice(0, viTri).split('\n').length;
        for (const m of nguon.matchAll(KHOA_TRONG_MA)) {
            const khoa = m[1] || m[2];
            if (khoa.endsWith('.')) continue;   // tiền tố để ghép ("loi." + name())
            if (!tatCa.has(khoa)) loi.push(`${tep}:${soDong(m.index)}: khoá ${khoa} không có trong thong-diep/`);
        }
        for (const m of nguon.matchAll(CAU_VIET_THANG)) {
            if (CO_DAU.test(m[1])) loi.push(`${tep}:${soDong(m.index)}: còn câu tiếng Việt viết thẳng: "${m[1]}"`);
        }
    }
}

if (loi.length) {
    console.error(loi.join('\n'));
    console.error(`\n${loi.length} lỗi.`);
    process.exit(1);
}
console.log(`Thông điệp: ${tatCa.size} khoá trong common, đủ 2 ngôn ngữ, mọi khoá dùng trong mã nguồn đều có.`);
