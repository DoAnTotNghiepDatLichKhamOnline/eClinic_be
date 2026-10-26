// Xuất 3 tài liệu API docs/api/*.md ra .docx ở thư mục gốc (bản sao để gửi / nộp; Markdown mới là bản gốc, sửa xong thì
// chạy lại, không sửa tay tệp .docx). Chạy từ thư mục gốc, cần Docker:
//   node scripts/xuat-docx-api.js                      xuất cả 3 tài liệu
//   node scripts/xuat-docx-api.js --nhom bac-si,quan-tri   chỉ các tài liệu này
// Thoát với mã 1 nếu có lỗi. Việc làm:
//   1. Chạy scripts/kiem-tra-tai-lieu-api.js --kiem-tra: phần chung lệch _CHUNG.md hoặc thiếu route thì không xuất.
//   2. Chuyển từng tài liệu bằng pandoc (ANH_PANDOC) trong Docker: đọc GitHub Markdown (id tiêu đề kiểu GitHub nên liên
//      kết nội bộ vẫn chạy), sắp theo bố cục tài liệu .docx cũ bằng scripts/docx/bo-cuc.lua (bìa, 1 giới thiệu,
//      2 danh sách route, 3 quy ước, 4 mô hình, 5… nhóm route, Phụ lục A mã lỗi / B luồng gọi mẫu / C chạy thử và tài
//      khoản mẫu; "mục N" đổi sang số mới; bảng mã lỗi đọc từ MaLoi.java, thong-diep/*.properties và gateway, truyền
//      qua biến môi trường MA_LOI_JSON), độ rộng cột theo
//      scripts/docx/do-rong-bang.lua, định dạng theo scripts/docx/mau-api.docx (dựng bằng scripts/docx/tao-mau-api.py).
//   3. Ghi vai trò của tài liệu vào đầu trang (thay VAI_TRO_TAI_LIEU trong word/header1.xml), ghi ra tệp tạm rồi đổi tên
//      đè tệp .docx. Tệp đang mở trong Word thì báo đóng tệp rồi chạy lại.
//   4. Kiểm tra tệp vừa xuất so với Markdown: mọi liên kết nội bộ trỏ tới 1 bookmark có thật và đủ số liên kết; route
//      đủ, đúng thứ tự, mỗi route có tiêu đề "N.k" và dải phương thức + đường dẫn; danh sách route ở mục 2 khớp từng
//      route; mỗi khối route có dòng "Quyền truy cập", đúng nhãn REQUEST… / RESPONSE THÀNH CÔNG / MÃ LỖI và cùng chữ
//      cái với khối Markdown (không mất, không lặp, không đảo chữ); bảng mã lỗi đúng dạng HTTP | maLoi và mọi maLoi có
//      trong code; Phụ lục A đúng các mã tài liệu nhắc, đúng thứ tự, HTTP và thông điệp của code; Phụ lục B đủ luồng,
//      Phụ lục C có bảng tài khoản; số mục liền nhau, cuối là Phụ lục A, B, C; đủ mô hình, đủ bảng; bìa, đầu trang, chân trang đúng; mọi "mục N" trỏ tới mục có thật; không lọt dấu CHUNG
//      hay Mục lục. Sai thì vẫn giữ tệp, in lỗi và thoát với mã 1.
// Tệp .docx ở thư mục gốc không vào git (/*.docx trong .gitignore).
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const zlib = require('zlib');

const GOC = path.resolve(__dirname, '..');
const ANH_PANDOC = 'pandoc/core:3.6.4';
const MAU = 'scripts/docx/mau-api.docx';
const BO_LOC = ['scripts/docx/bo-cuc.lua', 'scripts/docx/do-rong-bang.lua'];
const VAI_TRO = 'VAI_TRO_TAI_LIEU';
const TAI_LIEU = {
    'benh-nhan-khach': ['docs/api/API-BENH-NHAN-KHACH.md', 'eClinic_API_BenhNhan_Khach.docx'],
    'bac-si': ['docs/api/API-BAC-SI.md', 'eClinic_API_BacSi.docx'],
    'quan-tri': ['docs/api/API-QUAN-TRI.md', 'eClinic_API_QuanTri.docx'],
};

function chonNhom(args) {
    const i = args.indexOf('--nhom');
    if (i < 0) return Object.keys(TAI_LIEU);
    const nhom = (args[i + 1] || '').split(',').filter(Boolean);
    const sai = nhom.filter((n) => !TAI_LIEU[n]);
    if (!nhom.length || sai.length) {
        console.error(`--nhom phải là 1 hoặc nhiều nhóm trong: ${Object.keys(TAI_LIEU).join(', ')}`);
        process.exit(1);
    }
    return nhom;
}

function kiemTraTaiLieu() {
    try {
        execFileSync(process.execPath, ['scripts/kiem-tra-tai-lieu-api.js', '--kiem-tra'], { cwd: GOC, stdio: 'pipe' });
    } catch (e) {
        process.stderr.write(String(e.stdout || '') + String(e.stderr || ''));
        console.error('Tài liệu chưa đạt kiểm tra (xem trên). Chạy node scripts/kiem-tra-tai-lieu-api.js, sửa rồi xuất lại.');
        process.exit(1);
    }
}

// Vai trò của tài liệu: phần sau " — " của tiêu đề "#"
function vaiTro(md) {
    const h1 = md.split('\n').find((d) => d.startsWith('# ')) || '';
    return (h1.match(/ — (.+)$/) || [])[1];
}

// Tên HttpStatus của Spring → mã số (chỉ các tên MaLoi.java và gateway đang dùng; tên lạ thì dừng xuất)
const MA_HTTP = {
    BAD_REQUEST: 400, UNAUTHORIZED: 401, FORBIDDEN: 403, NOT_FOUND: 404, METHOD_NOT_ALLOWED: 405, CONFLICT: 409,
    GONE: 410, CONTENT_TOO_LARGE: 413, UNSUPPORTED_MEDIA_TYPE: 415, UNPROCESSABLE_ENTITY: 422, TOO_MANY_REQUESTS: 429,
    INTERNAL_SERVER_ERROR: 500, BAD_GATEWAY: 502, SERVICE_UNAVAILABLE: 503, GATEWAY_TIMEOUT: 504,
};
const TEP_MA_LOI = 'common/src/main/java/iuh/fit/se/eclinic/common/exception/MaLoi.java';
const THU_MUC_THONG_DIEP = 'common/src/main/resources/thong-diep';
const TEP_LOI_GATEWAY = 'api-gateway/src/main/java/iuh/fit/se/eclinic/gateway/exception/XuLyLoiGatewayHandler.java';
const THONG_DIEP_GATEWAY = 'api-gateway/src/main/resources/thong-diep/gateway.properties';

// Thông điệp loi.<MA>= (khoá không đuôi) của 1 tệp .properties; '' là dấu ' của MessageFormat
function thongDiepLoi(tep) {
    const kq = {};
    for (const m of fs.readFileSync(path.join(GOC, tep), 'utf8').matchAll(/^loi\.([A-Z][A-Z0-9_]*)=(.*)$/gm)) {
        kq[m[1]] = m[2].trim().replace(/''/g, "'");
    }
    return kq;
}

// Mọi maLoi trong code theo thứ tự khai báo: MaLoi.java (HTTP status ở đó, thông điệp tiếng Việt ở thong-diep/*.properties
// không đuôi _en), rồi các mã gateway tự trả (XuLyLoiGatewayHandler + gateway.properties, {n} thành "…"). Mã có ở cả
// hai giữ dòng của MaLoi.java. Dùng cho Phụ lục A (bo-cuc.lua nhận qua biến MA_LOI_JSON) và cho phần kiểm tra.
function maLoiTuCode() {
    const loi = [];
    const http = (ten, ma) => MA_HTTP[ten] || loi.push(`${ma}: không biết mã số của HttpStatus.${ten}`);
    const thongDiep = {};
    for (const tep of fs.readdirSync(path.join(GOC, THU_MUC_THONG_DIEP)).sort()) {
        if (tep.endsWith('.properties') && !tep.endsWith('_en.properties')) {
            Object.assign(thongDiep, thongDiepLoi(`${THU_MUC_THONG_DIEP}/${tep}`));
        }
    }
    const ds = [];
    const maLoiJava = fs.readFileSync(path.join(GOC, TEP_MA_LOI), 'utf8');
    for (const m of maLoiJava.matchAll(/^\s+([A-Z][A-Z0-9_]*)\(HttpStatus\.(\w+)\)/gm)) {
        if (!thongDiep[m[1]]) loi.push(`${m[1]}: thiếu loi.${m[1]}= trong ${THU_MUC_THONG_DIEP}/*.properties`);
        ds.push({ ma: m[1], http: http(m[2], m[1]), thongDiep: thongDiep[m[1]] || '', gateway: false });
    }
    const daCo = new Set(ds.map((d) => d.ma));
    const tdGateway = thongDiepLoi(THONG_DIEP_GATEWAY);
    const handler = fs.readFileSync(path.join(GOC, TEP_LOI_GATEWAY), 'utf8');
    const cap = [...handler.matchAll(/HttpStatus\.(\w+)[\s\S]{0,200}?\bloi\("([A-Z][A-Z0-9_]*)"/g)];
    if (cap.length !== (handler.match(/\bloi\("/g) || []).length) loi.push(`${TEP_LOI_GATEWAY}: có loi("…") không đọc được HttpStatus`);
    for (const [, ten, ma] of cap) {
        if (daCo.has(ma)) continue;
        if (!tdGateway[ma]) loi.push(`${ma}: thiếu loi.${ma}= trong ${THONG_DIEP_GATEWAY}`);
        ds.push({ ma, http: http(ten, ma), thongDiep: (tdGateway[ma] || '').replace(/\{\d+\}/g, '…'), gateway: true });
        daCo.add(ma);
    }
    if (ds.length < 10) loi.push(`${TEP_MA_LOI}: chỉ đọc được ${ds.length} mã`);
    if (loi.length) {
        console.error(`Không đọc được bảng mã lỗi từ code:\n${loi.map((l) => '  - ' + l).join('\n')}`);
        process.exit(1);
    }
    return ds;
}

function xuat(nhom, maLoi) {
    const [nguon, ten] = TAI_LIEU[nhom];
    const tam = ten.replace(/\.docx$/, '.tmp.docx');
    const duongTam = path.join(GOC, tam);
    const vaiTroTaiLieu = vaiTro(fs.readFileSync(path.join(GOC, nguon), 'utf8').replace(/\r\n/g, '\n'));
    if (!vaiTroTaiLieu) {
        console.error(`${nguon}: tiêu đề "#" cần dạng "Tài liệu API eClinic — <vai trò>".`);
        process.exit(1);
    }
    try {
        execFileSync('docker', ['run', '--rm', '-v', `${GOC}:/data`, '-w', '/data', '-e', 'MA_LOI_JSON', ANH_PANDOC,
            '-f', 'gfm', '-t', 'docx', '--reference-doc', MAU, ...BO_LOC.flatMap((b) => ['--lua-filter', b]),
            '-M', 'lang=vi-VN', '-o', tam, nguon],
        { stdio: ['ignore', 'pipe', 'pipe'], env: { ...process.env, MA_LOI_JSON: JSON.stringify(maLoi) } });
    } catch (e) {
        fs.rmSync(duongTam, { force: true });
        if (e.code === 'ENOENT') {
            console.error('Không tìm thấy lệnh docker: cần cài Docker để chạy pandoc.');
        } else {
            process.stderr.write(String(e.stderr || e.message));
            console.error(`Không chuyển được ${nguon}: pandoc báo lỗi (xem trên; Docker có đang chạy không?).`);
        }
        process.exit(1);
    }
    const thoat = (t) => t.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    fs.writeFileSync(duongTam, thayTepTrongZip(fs.readFileSync(duongTam), 'word/header1.xml', (xml) => {
        if (!xml.includes(VAI_TRO)) {
            fs.rmSync(duongTam, { force: true });
            console.error(`${MAU} không có ${VAI_TRO} ở đầu trang: chạy lại scripts/docx/tao-mau-api.py.`);
            process.exit(1);
        }
        return xml.replace(VAI_TRO, thoat(vaiTroTaiLieu));
    }));
    try {
        fs.renameSync(duongTam, path.join(GOC, ten));
    } catch (e) {
        fs.rmSync(duongTam, { force: true });
        if (e.code === 'EBUSY' || e.code === 'EPERM' || e.code === 'EACCES') {
            console.error(`${ten} đang mở (Word / LibreOffice?). Đóng tệp rồi chạy lại.`);
            process.exit(1);
        }
        throw e;
    }
    console.log(`${nguon} -> ${ten} (${Math.round(fs.statSync(path.join(GOC, ten)).size / 1024)} KB)`);
    return kiemTraDocx(nguon, ten, vaiTroTaiLieu, maLoi);
}

// Các mục trong thư mục trung tâm của gói zip (.docx): vị trí bản ghi, tên, dữ liệu (còn nén) và cách nén
function mucTrongZip(zip) {
    let cuoi = zip.length - 22;
    while (cuoi >= 0 && zip.readUInt32LE(cuoi) !== 0x06054b50) cuoi--;
    if (cuoi < 0) throw new Error('không phải tệp zip');
    const muc = [];
    let o = zip.readUInt32LE(cuoi + 16);
    for (let i = zip.readUInt16LE(cuoi + 10); i > 0; i--) {
        const doDaiTen = zip.readUInt16LE(o + 28);
        const dauTep = zip.readUInt32LE(o + 42);
        const batDau = dauTep + 30 + zip.readUInt16LE(dauTep + 26) + zip.readUInt16LE(dauTep + 28);
        muc.push({
            o, ten: zip.toString('utf8', o + 46, o + 46 + doDaiTen), cachNen: zip.readUInt16LE(o + 10),
            du: zip.subarray(batDau, batDau + zip.readUInt32LE(o + 20)),
        });
        o += 46 + doDaiTen + zip.readUInt16LE(o + 30) + zip.readUInt16LE(o + 32);
    }
    return muc;
}

const giaiNen = (m) => (m.cachNen === 8 ? zlib.inflateRawSync(m.du) : m.du).toString('utf8');

// Đọc 1 tệp trong gói zip (deflate hoặc không nén)
function docTepTrongZip(zip, ten) {
    const m = mucTrongZip(zip).find((x) => x.ten === ten);
    if (!m) throw new Error(`không có ${ten}`);
    return giaiNen(m);
}

// Ghi lại gói zip với 1 tệp đã sửa bằng doi(xml). Bản ghi đầu tệp dựng lại từ thư mục trung tâm (không data descriptor,
// không extra), các tệp khác giữ nguyên dữ liệu nén.
function thayTepTrongZip(zip, ten, doi) {
    const phan = [];
    const trungTam = [];
    let viTri = 0;
    const muc = mucTrongZip(zip);
    if (!muc.some((m) => m.ten === ten)) throw new Error(`không có ${ten}`);
    for (const m of muc) {
        const tenTep = Buffer.from(m.ten, 'utf8');
        const cd = Buffer.from(zip.subarray(m.o, m.o + 46));
        let du = m.du;
        if (m.ten === ten) {
            const moi = Buffer.from(doi(giaiNen(m)), 'utf8');
            du = zlib.deflateRawSync(moi);
            cd.writeUInt16LE(8, 10);
            cd.writeUInt32LE(zlib.crc32(moi), 16);
            cd.writeUInt32LE(moi.length, 24);
        }
        cd.writeUInt16LE(cd.readUInt16LE(8) & ~0x8, 8);
        cd.writeUInt32LE(du.length, 20);
        cd.writeUInt16LE(tenTep.length, 28);
        cd.fill(0, 30, 34);
        cd.writeUInt32LE(viTri, 42);
        const dau = Buffer.alloc(30);
        dau.writeUInt32LE(0x04034b50, 0);
        cd.copy(dau, 4, 6, 30);
        dau.writeUInt16LE(0, 28);
        phan.push(dau, tenTep, du);
        trungTam.push(cd, tenTep);
        viTri += 30 + tenTep.length + du.length;
    }
    const thuMuc = Buffer.concat(trungTam);
    const ket = Buffer.alloc(22);
    ket.writeUInt32LE(0x06054b50, 0);
    ket.writeUInt16LE(muc.length, 8);
    ket.writeUInt16LE(muc.length, 10);
    ket.writeUInt32LE(thuMuc.length, 12);
    ket.writeUInt32LE(viTri, 16);
    return Buffer.concat([...phan, thuMuc, ket]);
}

const boThucThe = (t) => t.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&apos;/g, "'")
    .replace(/&amp;/g, '&');

const chuCua = (xml) => boThucThe([...xml.matchAll(/<w:t(?: [^>]*)?>([^<]*)<\/w:t>/g)].map((t) => t[1]).join(''))
    .replace(/\s+/g, ' ').trim();
const DAU_MUC = ['1. Giới thiệu và phạm vi', '2. Danh sách route', '3. Quy ước chung', '4. Mô hình dữ liệu'];
const PHU_LUC = ['Phụ lục A. Bảng mã lỗi (maLoi)', 'Phụ lục B. Luồng gọi mẫu', 'Phụ lục C. Chạy thử và tài khoản mẫu'];

function kiemTraDocx(nguon, ten, vaiTroTaiLieu, maLoi) {
    const md = fs.readFileSync(path.join(GOC, nguon), 'utf8').replace(/\r\n/g, '\n');
    let trongCode = false;
    const dongNgoaiCode = md.split('\n').filter((d) => {
        if (d.startsWith('```')) { trongCode = !trongCode; return false; }
        return !trongCode;
    });
    const tieuDeMd = dongNgoaiCode.filter((d) => d.startsWith('#### ')).map((d) => d.slice(5).replace(/[`*]/g, '').trim());
    const routeMd = tieuDeMd.filter((t) => /^(GET|POST|PUT|PATCH|DELETE) /.test(t));
    const moHinhMd = tieuDeMd.length - routeMd.length + dongNgoaiCode.filter((d) => /^\*\*`\w+`\*\*/.test(d)).length;
    const bangTongQuan = dongNgoaiCode.filter((d) => /^\| Phương thức \| Đường dẫn \| Quyền \| Mục đích \|/.test(d)).length;
    let bangMd = dongNgoaiCode.filter((d) => /^\|[ :|-]*-[ :|-]*\|\s*$/.test(d)).length - bangTongQuan + 2;
    // Khối Markdown của từng route: các dòng sau "#### METHOD /đường/dẫn" tới tiêu đề kế tiếp (kể cả trong khối code)
    const khoiMd = [];
    trongCode = false;
    for (const d of md.split('\n')) {
        if (d.startsWith('```')) trongCode = !trongCode;
        else if (!trongCode && /^#{1,4} /.test(d)) {
            if (/^#### (GET|POST|PUT|PATCH|DELETE) \//.test(d)) khoiMd.push([]);
            else khoiMd.push(null);
            continue;
        }
        if (khoiMd.length && khoiMd[khoiMd.length - 1]) khoiMd[khoiMd.length - 1].push(d);
    }
    const khoiRouteMd = khoiMd.filter(Boolean).map((dong) => dong.join('\n'));
    const mucLuc = (md.match(/^## Mục lục\n[\s\S]*?(?=^## )/m) || [''])[0];
    const lienKetMd = (md.match(/\]\(#[^)]*\)/g) || []).length - (mucLuc.match(/\]\(#[^)]*\)/g) || []).length
        + routeMd.length;

    const zip = fs.readFileSync(path.join(GOC, ten));
    const xml = docTepTrongZip(zip, 'word/document.xml');
    const bookmark = new Set([...xml.matchAll(/<w:bookmarkStart [^>]*w:name="([^"]*)"/g)].map((m) => m[1]));
    const lienKet = [...xml.matchAll(/w:anchor="([^"]*)"/g)].map((m) => m[1]);
    // Mỗi đoạn kèm bookmark đặt ngay trước nó (pandoc đặt bookmark của tiêu đề trước <w:p>); <w:p /> là đoạn rỗng
    let het = 0;
    const doan = [...xml.matchAll(/<w:p\b[^>]*?(?:\/>|>([\s\S]*?)<\/w:p>)/g)].map((m) => {
        const truoc = xml.slice(het, m.index);
        const than = m[1] || '';
        het = m.index + m[0].length;
        return {
            viTri: m.index,
            style: (than.match(/<w:pStyle w:val="([^"]+)"/) || [])[1],
            chu: chuCua(than),
            moc: ([...truoc.matchAll(/<w:bookmarkStart [^>]*w:name="([^"]*)"/g)].pop() || [])[1],
        };
    });
    const theoStyle = (s) => doan.filter((d) => d.style === s);
    const bang = (xml.match(/<w:tbl>/g) || []).length;

    const loi = [];
    const lienKetHong = [...new Set(lienKet.filter((a) => !bookmark.has(a)))];
    if (lienKetHong.length) loi.push(`liên kết không có đích: ${lienKetHong.join(', ')}`);
    if (lienKet.length !== lienKetMd) loi.push(`${lienKet.length} liên kết, cần ${lienKetMd}`);

    // Route: dải ThanhRoute theo thứ tự Markdown, ngay sau tiêu đề "N.k <tên>"
    const route = [];
    doan.forEach((d, i) => {
        if (d.style === 'ThanhRoute') route.push({ chu: d.chu, tieuDe: doan[i - 1], viTri: i });
    });
    const lech = routeMd.findIndex((t, i) => !route[i] || t !== route[i].chu);
    if (lech >= 0 || route.length !== routeMd.length) {
        const i = lech >= 0 ? lech : routeMd.length;
        loi.push(`route lệch từ số ${i + 1}: Markdown "${routeMd[i]}", .docx "${route[i] && route[i].chu}"`);
    }
    route.forEach((r) => {
        if (!r.tieuDe || r.tieuDe.style !== 'Heading2' || !/^\d+\.\d+ \S/.test(r.tieuDe.chu)) {
            loi.push(`route ${r.chu} không đứng sau tiêu đề "N.k <tên>"`);
        }
    });

    // Danh sách route: bảng đầu tiên sau tiêu đề "2. Danh sách route", mỗi dòng 1 route theo thứ tự
    const h2DanhSach = doan.find((d) => d.style === 'Heading1' && d.chu === DAU_MUC[1]);
    const bangDs = h2DanhSach && (xml.slice(h2DanhSach.viTri).match(/<w:tbl>[\s\S]*?<\/w:tbl>/) || [])[0];
    const dongDs = bangDs ? [...bangDs.matchAll(/<w:tr\b[\s\S]*?<\/w:tr>/g)].slice(1).map((m) => ({
        muc: chuCua((m[0].match(/<w:tc>[\s\S]*?<\/w:tc>/) || [''])[0]),
        dich: (m[0].match(/w:anchor="([^"]*)"/) || [])[1],
    })) : [];
    if (dongDs.length !== route.length) loi.push(`danh sách route có ${dongDs.length} dòng, cần ${route.length}`);
    dongDs.forEach((d, i) => {
        const r = route[i] && route[i].tieuDe;
        if (r && (d.muc !== r.chu.split(' ')[0] || d.dich !== r.moc)) {
            loi.push(`danh sách route dòng ${i + 1} (${d.muc}, #${d.dich}) không khớp tiêu đề "${r.chu}" (#${r.moc})`);
        }
    });

    // Khối route: Quyền truy cập, nhãn REQUEST… / RESPONSE THÀNH CÔNG / MÃ LỖI, và cùng chữ cái với Markdown theo đúng
    // thứ tự (chỉ so chữ cái nên số "mục N" đã đổi không ảnh hưởng; bỏ nhãn của 2 bên và dòng tiêu đề bảng mã lỗi)
    const chuCai = (s) => (s.match(/\p{L}/gu) || []).join('');
    let soNhanLoi = 0;
    route.forEach((r, i) => {
        const md1 = khoiRouteMd[i];
        if (md1 === undefined || r.chu !== routeMd[i]) return;
        const khoi = [];
        for (let j = r.viTri + 1; j < doan.length && !/^Heading[12]$/.test(doan[j].style || ''); j++) khoi.push(doan[j]);
        if (!khoi[0] || khoi[0].style !== 'QuyenTruyCap' || !khoi[0].chu.startsWith('Quyền truy cập: ')) {
            loi.push(`route ${r.chu}: thiếu dòng "Quyền truy cập: …" ngay dưới dải route`);
        }
        const nhan = khoi.filter((d) => d.style === 'NhanKhoi').map((d) => d.chu);
        const dem = (re) => nhan.filter((n) => re.test(n)).length;
        const coLoi = /^\*\*Lỗi:\*\*/m.test(md1) ? 1 : 0;
        soNhanLoi += dem(/^MÃ LỖI$/);
        if (dem(/^RESPONSE THÀNH CÔNG — (200 OK|201 Created)$/) !== 1 || dem(/^REQUEST/) > 1
            || dem(/^MÃ LỖI$/) !== coLoi || nhan.some((n) => !/^(REQUEST|RESPONSE THÀNH CÔNG — |MÃ LỖI$)/.test(n))) {
            loi.push(`route ${r.chu}: nhãn khối ${JSON.stringify(nhan)}`);
        }
        const chuMd = chuCai(md1.replace(/\*\*Response:\*\*\s*HTTP \d{3}[,.]/g, '')
            .replace(/\*\*(Quyền|Request|Response|Lỗi):\*\*/g, '').replace(/<!--[\s\S]*?-->/g, '')
            .replace(/\]\([^)]*\)/g, ']').replace(/^```.*$/gm, ''));
        const chuDocx = chuCai(khoi.filter((d) => d.style !== 'NhanKhoi')
            .map((d) => (d.style === 'QuyenTruyCap' ? d.chu.replace(/^Quyền truy cập:/, '') : d.chu)).join(' '))
            .split('HTTPmaLoiĐiềukiện').join('');
        if (chuMd !== chuDocx) {
            let k = 0;
            while (chuMd[k] === chuDocx[k]) k++;
            loi.push(`route ${r.chu}: chữ lệch Markdown từ "${chuMd.slice(k, k + 40)}", .docx "${chuDocx.slice(k, k + 40)}"`);
        }
    });

    // Bảng mã lỗi: HTTP | maLoi | Điều kiện, mỗi dòng 1 mã HTTP 3 chữ số và 1 maLoi
    let soBangLoi = 0;
    for (const b of xml.match(/<w:tbl>[\s\S]*?<\/w:tbl>/g) || []) {
        const dong = [...b.matchAll(/<w:tr\b[\s\S]*?<\/w:tr>/g)]
            .map((d) => [...d[0].matchAll(/<w:tc>[\s\S]*?<\/w:tc>/g)].map((o) => chuCua(o[0])));
        if (!dong[0] || dong[0].join('|') !== 'HTTP|maLoi|Điều kiện') continue;
        soBangLoi++;
        dong.slice(1).forEach((o) => {
            if (!/^\d{3}$/.test(o[0]) || !/^[A-Z][A-Z0-9_]+$/.test(o[1])) loi.push(`dòng bảng mã lỗi "${o.join(' | ')}"`);
            else if (!maLoi.some((m) => m.ma === o[1])) loi.push(`maLoi không có trong code: ${o[1]} (${o.join(' | ')})`);
        });
    }
    bangMd += soBangLoi;

    // Số mục: 4 mục đầu cố định, rồi liền nhau; tiêu đề cấp 2 là "N.k" liền nhau trong mục N. Cuối tài liệu là đúng 3 phụ
    // lục A, B, C; chỉ Phụ lục B có tiêu đề cấp 2 ("B.k", mỗi luồng 1 tiêu đề)
    const soMuc = new Set();
    let h1 = 0;
    let h2 = 0;
    let moHinh = 0;
    let phuLuc = 0;
    const tieuDeB = [];
    for (const d of doan) {
        if (d.style === 'Heading1' && (phuLuc || d.chu.startsWith('Phụ lục '))) {
            phuLuc++;
            h2 = 0;
            const can = PHU_LUC[phuLuc - 1];
            if (d.chu !== can) loi.push(`phụ lục thứ ${phuLuc} là "${d.chu}", cần "${can}"`);
        } else if (d.style === 'Heading1') {
            h1++;
            h2 = 0;
            const can = DAU_MUC[h1 - 1] || `${h1}. `;
            if (!(h1 <= DAU_MUC.length ? d.chu === can : d.chu.startsWith(can) && d.chu.length > can.length)) {
                loi.push(`tiêu đề cấp 1 thứ ${h1} là "${d.chu}", cần "${can}…"`);
            }
            soMuc.add(String(h1));
        } else if (d.style === 'Heading2' && phuLuc) {
            h2++;
            if (phuLuc !== 2 || !d.chu.startsWith(`B.${h2} `)) loi.push(`tiêu đề cấp 2 "${d.chu}" trong phụ lục thứ ${phuLuc}`);
            tieuDeB.push(d.chu.replace(/^B\.\d+ /, ''));
        } else if (d.style === 'Heading2') {
            h2++;
            if (!d.chu.startsWith(`${h1}.${h2} `)) loi.push(`tiêu đề cấp 2 "${d.chu}", cần "${h1}.${h2} …"`);
            soMuc.add(`${h1}.${h2}`);
            if (h1 === 4) moHinh++;
        }
        if (/^Heading\d/.test(d.style) && d.chu === 'Mục lục') loi.push('còn tiêu đề Mục lục');
    }
    if (phuLuc !== PHU_LUC.length) loi.push(`${phuLuc} phụ lục, cần ${PHU_LUC.length}`);
    const tieuDePhuLuc = (i) => doan.find((d) => d.style === 'Heading1' && d.chu === PHU_LUC[i]);
    const bangSau = (d) => d && (xml.slice(d.viTri).match(/<w:tbl>[\s\S]*?<\/w:tbl>/) || [])[0];
    const oCuaBang = (b) => [...b.matchAll(/<w:tr\b[\s\S]*?<\/w:tr>/g)]
        .map((d) => [...d[0].matchAll(/<w:tc>[\s\S]*?<\/w:tc>/g)].map((o) => chuCua(o[0])));

    // Phụ lục A: đúng các mã trong code mà Markdown nhắc (chữ code ngoài khối code), theo thứ tự code, đúng HTTP và thông
    // điệp. Mọi maLoi trong bảng mã lỗi của route phải có trong code.
    const theoMa = new Map(maLoi.map((m) => [m.ma, m]));
    const nhacMd = new Set(dongNgoaiCode.flatMap((d) => [...d.matchAll(/`([A-Z][A-Z0-9_]*)`/g)].map((m) => m[1])));
    const canA = maLoi.filter((m) => nhacMd.has(m.ma));
    const bangA = bangSau(tieuDePhuLuc(0));
    const dongA = bangA ? oCuaBang(bangA) : [];
    if (!dongA[0] || dongA[0].join('|') !== 'maLoi|HTTP|Thông điệp mặc định (tiếng Việt)') {
        loi.push('Phụ lục A không có bảng maLoi | HTTP | Thông điệp mặc định (tiếng Việt)');
    } else {
        const coA = dongA.slice(1);
        const thieu = canA.filter((m) => !coA.some((o) => o[0] === m.ma)).map((m) => m.ma);
        const thua = coA.filter((o) => !canA.some((m) => m.ma === o[0])).map((o) => o[0]);
        if (thieu.length) loi.push(`Phụ lục A thiếu mã ${thieu.join(', ')}`);
        if (thua.length) loi.push(`Phụ lục A thừa mã ${thua.join(', ')}`);
        coA.forEach((o, i) => {
            const m = theoMa.get(o[0]);
            if (!m) return;
            if (o[1] !== String(m.http) || o[2] !== m.thongDiep.replace(/\s+/g, ' ').trim()) {
                loi.push(`Phụ lục A dòng ${o[0]}: "${o[1]} | ${o[2]}", code "${m.http} | ${m.thongDiep}"`);
            }
            if (!thieu.length && !thua.length && canA[i].ma !== o[0]) loi.push(`Phụ lục A dòng ${i + 1} (${o[0]}) sai thứ tự code`);
        });
    }

    // Phụ lục B: tiêu đề B.k là chữ đậm đầu của từng luồng trong các danh sách "**… luồng chính**" của Markdown (bỏ ":")
    const luongMd = [];
    let trongLuong = false;
    for (const d of dongNgoaiCode) {
        if (/^\*\*.*luồng chính\*\*$/.test(d)) trongLuong = true;
        else if (/^(#|\*\*)/.test(d)) trongLuong = false;
        else if (trongLuong && /^\d+\. \*\*/.test(d)) luongMd.push(d.match(/^\d+\. \*\*(.+?)\*\*/)[1].replace(/:$/, ''));
    }
    if (tieuDeB.join('|') !== luongMd.join('|')) {
        loi.push(`Phụ lục B: tiêu đề ${JSON.stringify(tieuDeB)}, Markdown có các luồng ${JSON.stringify(luongMd)}`);
    }
    const dauB = tieuDePhuLuc(1);
    if (doan.some((d) => d.chu.includes('luồng chính') && (!dauB || d.viTri < dauB.viTri))) {
        loi.push('còn nhãn "… luồng chính" ngoài Phụ lục B');
    }

    // Phụ lục C: có bảng tài khoản mẫu; nhãn chạy thử không còn ở mục 3.1
    const dauC = tieuDePhuLuc(2);
    const bangC = bangSau(dauC);
    if (!bangC || !/^Tài khoản\|Mật khẩu/.test((oCuaBang(bangC)[0] || []).join('|'))) loi.push('Phụ lục C không có bảng Tài khoản | Mật khẩu');
    if (doan.some((d) => d.chu.includes('Chạy thử và tài khoản mẫu') && d.style !== 'Heading1')) {
        loi.push('còn nhãn "Chạy thử và tài khoản mẫu" ngoài tiêu đề Phụ lục C');
    }
    bangMd += 1;
    if (moHinh !== moHinhMd) loi.push(`${moHinh} mô hình ở mục 4, Markdown có ${moHinhMd}`);
    if (bang !== bangMd) loi.push(`${bang} bảng, cần ${bangMd}`);

    // Bìa, đầu trang, chân trang
    const tieuDeTaiLieu = theoStyle('Title').map((d) => d.chu);
    const phuDe = theoStyle('Subtitle').map((d) => d.chu);
    if (tieuDeTaiLieu.join('|') !== 'Tài liệu API') loi.push(`Title ${JSON.stringify(tieuDeTaiLieu)}, cần "Tài liệu API"`);
    if (phuDe.join('|') !== vaiTroTaiLieu) loi.push(`Subtitle ${JSON.stringify(phuDe)}, cần "${vaiTroTaiLieu}"`);
    const dauTrang = chuCua(docTepTrongZip(zip, 'word/header1.xml'));
    if (dauTrang !== `eClinic — Tài liệu API: ${vaiTroTaiLieu}`) loi.push(`đầu trang "${dauTrang}"`);
    if (!docTepTrongZip(zip, 'word/footer1.xml').includes('NUMPAGES')) loi.push('chân trang không có tổng số trang');

    // "mục N" / "mục N–M" phải trỏ tới mục có thật
    const sai = new Set();
    for (const d of doan) {
        for (const m of d.chu.matchAll(/[Mm]ục (\d+(?:\.\d+)?)(?:–(\d+(?:\.\d+)?))?/g)) {
            [m[1], m[2]].filter(Boolean).forEach((so) => { if (!soMuc.has(so)) sai.add(so); });
        }
    }
    if (sai.size) loi.push(`"mục" trỏ tới mục không có: ${[...sai].join(', ')}`);
    if (doan.some((d) => d.chu.includes('CHUNG:'))) loi.push('lọt dấu CHUNG: vào tài liệu');

    const tomTat = `liên kết ${lienKet.length}/${lienKetMd}, route ${route.length}/${routeMd.length}, mô hình ${moHinh}, `
        + `bảng ${bang}/${bangMd}, mã lỗi ${soBangLoi}/${soNhanLoi} bảng, phụ lục A ${Math.max(dongA.length - 1, 0)} mã, `
        + `phụ lục B ${tieuDeB.length} luồng`;
    if (!loi.length) {
        console.log(`  ${tomTat}: đạt`);
        return true;
    }
    console.error(`  ${tomTat}: ${loi.length} lỗi\n${loi.map((l) => '  - ' + l).join('\n')}`);
    return false;
}

const nhom = chonNhom(process.argv.slice(2));
kiemTraTaiLieu();
const maLoi = maLoiTuCode();
const dat = nhom.map((n) => xuat(n, maLoi)).every(Boolean);
if (!dat) process.exit(1);
