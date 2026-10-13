// Kiểm tra chữ trên trang demo (scripts/demo) đã có đủ 2 ngôn ngữ: liệt kê mọi chuỗi trong mã nguồn còn chứa chữ tiếng
// Việt có dấu mà KHÔNG phải tham số đầu của t( hoặc cap( (xem chung.js). Chú thích được bỏ qua.
//
//   node scripts/kiem-tra-demo-ngon-ngu.js
//
// In "file:dòng: chuỗi" cho từng chỗ sót, mã thoát 1 khi có; không sót thì mã thoát 0.
// Giới hạn: chỉ nhận ra chữ có dấu. Từ tiếng Việt không dấu ("Nam", "Email", "CCCD") phải tự đọc lại.
// Chuỗi mẫu (`...`): từng đoạn chữ được xét riêng, biểu thức trong ${...} được xét như mã nguồn thường, nên
// `<label>${t('Họ tên', 'Full name')}</label>` là hợp lệ còn `<label>Họ tên</label>` thì không.
const fs = require('fs');
const path = require('path');

const GOC = path.join(__dirname, 'demo');
const CO_DAU = /[À-ÖØ-öø-ỹ]/;
const HAM_DICH = ['t', 'cap'];

// Tách mã JS thành các chuỗi kèm 2 "từ" đứng ngay trước (để biết chuỗi có là tham số đầu của t( / cap( không).
// Không phân biệt dấu chia với biểu thức chính quy: demo chỉ có vài regex ngắn, không chứa nháy hay chữ có dấu.
function quetJs(ma, dongDau, ketQua) {
    let i = 0, dong = dongDau;
    let truoc = ['', ''];   // 2 từ gần nhất, vd ['t', '(']
    const ghiTu = (tu) => { truoc = [truoc[1], tu]; };
    const laThamSoDau = () => truoc[1] === '(' && HAM_DICH.includes(truoc[0]);
    const ghiChuoi = (noiDung, dongChuoi, hopLe) => {
        if (CO_DAU.test(noiDung) && !hopLe) ketQua.push({ dong: dongChuoi, chuoi: noiDung });
    };
    while (i < ma.length) {
        const c = ma[i];
        if (c === '\n') { dong++; i++; continue; }
        if (/\s/.test(c)) { i++; continue; }
        if (c === '/' && ma[i + 1] === '/') {
            while (i < ma.length && ma[i] !== '\n') i++;
            continue;
        }
        if (c === '/' && ma[i + 1] === '*') {
            const het = ma.indexOf('*/', i + 2);
            const doan = ma.slice(i, het < 0 ? ma.length : het + 2);
            dong += doan.split('\n').length - 1;
            i += doan.length;
            continue;
        }
        if (c === '\'' || c === '"') {
            const hopLe = laThamSoDau();
            const dongChuoi = dong;
            let j = i + 1, noiDung = '';
            while (j < ma.length && ma[j] !== c && ma[j] !== '\n') {
                if (ma[j] === '\\') { noiDung += ma[j + 1] || ''; j += 2; continue; }
                noiDung += ma[j++];
            }
            ghiChuoi(noiDung, dongChuoi, hopLe);
            ghiTu('chuoi');
            i = j + 1;
            continue;
        }
        if (c === '`') {
            const hopLe = laThamSoDau();
            let j = i + 1, doan = '', dongDoan = dong;
            while (j < ma.length && ma[j] !== '`') {
                if (ma[j] === '\\') { doan += ma[j + 1] || ''; j += 2; continue; }
                if (ma[j] === '$' && ma[j + 1] === '{') {
                    // Biểu thức ${...}: tìm ngoặc đóng tương ứng rồi quét như mã thường
                    const batDau = j + 2;
                    const ketThuc = ngoacDong(ma, batDau);
                    quetJs(ma.slice(batDau, ketThuc), dong, ketQua);
                    dong += ma.slice(batDau, ketThuc).split('\n').length - 1;
                    j = ketThuc + 1;
                    continue;
                }
                if (ma[j] === '\n') {
                    ghiChuoi(doan.trim(), dongDoan, hopLe);
                    doan = '';
                    dong++;
                    dongDoan = dong;
                    j++;
                    continue;
                }
                doan += ma[j++];
            }
            ghiChuoi(doan.trim(), dongDoan, hopLe);
            ghiTu('chuoi');
            i = j + 1;
            continue;
        }
        const tu = ma.slice(i).match(/^[A-Za-z_$][\w$]*/);
        if (tu) { ghiTu(tu[0]); i += tu[0].length; continue; }
        ghiTu(c);
        i++;
    }
}

// Vị trí dấu } đóng biểu thức ${ bắt đầu ở batDau (bỏ qua chuỗi và chuỗi mẫu lồng bên trong)
function ngoacDong(ma, batDau) {
    let sau = 0, i = batDau;
    while (i < ma.length) {
        const c = ma[i];
        if (c === '\\') { i += 2; continue; }
        if (c === '\'' || c === '"') {
            i++;
            while (i < ma.length && ma[i] !== c) i += ma[i] === '\\' ? 2 : 1;
        } else if (c === '`') {
            i++;
            while (i < ma.length && ma[i] !== '`') {
                if (ma[i] === '\\') { i += 2; continue; }
                if (ma[i] === '$' && ma[i + 1] === '{') { i = ngoacDong(ma, i + 2) + 1; continue; }
                i++;
            }
        } else if (c === '{') {
            sau++;
        } else if (c === '}') {
            if (sau === 0) return i;
            sau--;
        }
        i++;
    }
    return ma.length;
}

// index.html: chữ nằm ngoài thẻ và ngoài <script> / chú thích phải do chung.js đặt (phần tử có id), không viết sẵn
function quetHtml(html, ketQua) {
    const sach = html.replace(/<!--[\s\S]*?-->|<script[\s\S]*?<\/script>|<[^>]*>/g, (doan) => doan.replace(/[^\n]/g, ' '));
    sach.split('\n').forEach((noiDung, chiSo) => {
        if (CO_DAU.test(noiDung)) ketQua.push({ dong: chiSo + 1, chuoi: noiDung.trim() });
    });
}

const cacFile = ['index.html', 'chung.js',
    ...fs.readdirSync(path.join(GOC, 'man-hinh')).filter((ten) => ten.endsWith('.js')).sort().map((ten) => 'man-hinh/' + ten)];
let soSot = 0;
for (const ten of cacFile) {
    const noiDung = fs.readFileSync(path.join(GOC, ten), 'utf8');
    const ketQua = [];
    if (ten.endsWith('.html')) quetHtml(noiDung, ketQua);
    else quetJs(noiDung, 1, ketQua);
    for (const { dong, chuoi } of ketQua) {
        console.log(`scripts/demo/${ten}:${dong}: ${chuoi.length > 90 ? chuoi.slice(0, 90) + '…' : chuoi}`);
    }
    soSot += ketQua.length;
}
console.log(soSot ? `\n${soSot} chuỗi chưa có bản tiếng Anh (bọc bằng t(vi, en) hoặc cap(vi, en)).`
    : `Đã kiểm tra ${cacFile.length} file: mọi chuỗi tiếng Việt có dấu đều nằm trong t() / cap().`);
process.exit(soSot ? 1 : 0);
