// Smoke phase 10: mọi tên trong `...` của tài liệu bàn giao (đường dẫn API, trường, mã lỗi, biến môi trường, file) phải
// có trong mã nguồn.   node temp/d9-p10-kiem-tai-lieu.js [file.md]
const fs = require('fs');
const path = require('path');
const GOC = path.join(__dirname, '..');
const TAI_LIEU = process.argv[2] || 'docs/BAN-GIAO-FRONTEND-NGON-NGU-EMAIL-CHATBOT.md';

function docCay(thuMuc, duoi, ketQua = []) {
  for (const muc of fs.readdirSync(thuMuc, { withFileTypes: true })) {
    if (['target', 'node_modules', '.git', 'test'].includes(muc.name)) continue;
    const duongDan = path.join(thuMuc, muc.name);
    if (muc.isDirectory()) docCay(duongDan, duoi, ketQua);
    else if (duoi.some((d) => muc.name.endsWith(d))) ketQua.push(fs.readFileSync(duongDan, 'utf8'));
  }
  return ketQua;
}
const cacModule = fs.readdirSync(GOC).filter((t) => fs.existsSync(path.join(GOC, t, 'src', 'main')));
const java = cacModule.flatMap((m) => docCay(path.join(GOC, m, 'src', 'main'), ['.java', '.yml', '.properties'])).join('\n');
const cauHinh = ['docker-compose.yml', '.env.example'].map((t) => fs.readFileSync(path.join(GOC, t), 'utf8')).join('\n');
const demo = docCay(path.join(GOC, 'scripts', 'demo'), ['.js', '.html']).join('\n');

const md = fs.readFileSync(path.join(GOC, TAI_LIEU), 'utf8');
const cacTen = new Set();
for (const m of md.matchAll(/`([^`\n]+)`/g)) cacTen.add(m[1]);

// Mọi đường dẫn @...Mapping ghép từ @RequestMapping của lớp + của hàm, {biến} thành {}
const cacApi = new Set();
for (const tep of cacModule.flatMap((m) => docCay(path.join(GOC, m, 'src', 'main', 'java'), ['Controller.java']))) {
  const goc = (tep.match(/@RequestMapping\("([^"]*)"\)/) || [, ''])[1];
  for (const m of tep.matchAll(/@(Get|Post|Put|Delete|Patch)Mapping(?:\(\s*(?:value\s*=\s*)?(?:"([^"]*)"|\{([^}]*)\})?[^)]*\))?/g)) {
    const cacDuoi = m[3] ? [...m[3].matchAll(/"([^"]*)"/g)].map((x) => x[1]) : [m[2] || ''];
    for (const duoi of cacDuoi) cacApi.add(`${m[1].toUpperCase()} ${(goc + duoi).replace(/\{[^}]*\}/g, '{}')}`);
  }
}
const coApi = (phuongThuc, duongDan) => {
  const sach = duongDan.split('?')[0].replace(/\{[^}]*\}|<[^>]*>/g, '{}').replace(/\/$/, '');
  return phuongThuc ? cacApi.has(`${phuongThuc} ${sach}`) : [...cacApi].some((a) => a.endsWith(' ' + sach));
};

const thieu = [];
let soKiem = 0;
const kiem = (ten, dung, loai) => { soKiem++; if (!dung) thieu.push(`${loai}: ${ten}`); };
for (const ten of cacTen) {
  let m;
  if ((m = ten.match(/^(GET|POST|PUT|DELETE)(?: \| (GET|POST|PUT|DELETE))? ?(?:\/ `?)?(\/api\/\S+|\.\.\.\S+)/))) {
    if (m[3].startsWith('...')) { kiem(ten, [...cacApi].some((a) => a.replace(/\{\}/g, '').includes(m[3].slice(3).replace(/\{[^}]*\}/g, ''))), 'API (đuôi)'); continue; }
    kiem(ten, coApi(m[1], m[3]), 'API');
    if (m[2]) kiem(ten, coApi(m[2], m[3]), 'API');
  } else if (/^\/api\//.test(ten)) {
    kiem(ten, coApi(null, ten), 'API');
  } else if (/^\.\.\.\//.test(ten) || /^(POST|DELETE|PUT|GET) \.\.\./.test(ten)) {
    const duoi = ten.replace(/^\S* ?\.\.\./, '').replace(/\{[^}]*\}/g, '{}');
    kiem(ten, [...cacApi].some((a) => a.endsWith(duoi)), 'API (đuôi)');
  } else if (/^[A-Z][A-Z0-9_]{2,}(\*)?$/.test(ten)) {
    // MA_LOI, ENUM hoặc BIEN_MOI_TRUONG (CHATBOT_* = tiền tố)
    const goc = ten.replace(/\*$/, '');
    kiem(ten, new RegExp(`\\b${goc}`).test(java) || cauHinh.includes(goc), 'hằng / biến môi trường');
  } else if (/^[a-z][A-Za-z0-9]*(\[\])?$/.test(ten)) {
    const goc = ten.replace('[]', '');
    kiem(ten, new RegExp(`\\b${goc}\\b`).test(java) || new RegExp(`\\b${goc}\\b`).test(demo), 'trường');
  } else if (/^(scripts|docs)\//.test(ten) || /\.(js|md)$/.test(ten)) {
    const tep = ten.replace(/^node /, '');
    kiem(ten, fs.existsSync(path.join(GOC, tep)) || fs.existsSync(path.join(GOC, 'docs', tep)), 'file');
  }
}
// Trường viết trong { a, b, c } hoặc a.b / a[].b
for (const ten of cacTen) {
  if (!/[{.]/.test(ten) || /^(GET|POST|PUT|DELETE|\/|node |scripts|docs|http|\.\.\.)/.test(ten) || /\.(js|md|yml|example)$/.test(ten)) continue;
  for (const m of ten.matchAll(/[a-z][A-Za-z0-9]*/g)) {
    if (['null', 'true', 'false', 'hoặc', 'tối', 'en'].includes(m[0])) continue;
    kiem(`${m[0]} (trong \`${ten.slice(0, 50)}\`)`, new RegExp(`\\b${m[0]}\\b`).test(java) || new RegExp(`\\b${m[0]}\\b`).test(demo), 'trường');
  }
}
console.log(`${TAI_LIEU}: ${cacTen.size} tên trong \`...\`, ${soKiem} lần đối chiếu, ${cacApi.size} API trong các controller`);
if (thieu.length) { console.log('KHÔNG THẤY trong mã nguồn:\n  ' + thieu.join('\n  ')); process.exit(1); }
console.log('Mọi tên đều có trong mã nguồn.');
