// Đo từng API sau các màn hình danh sách, báo cáo, tệp xuất (DOANTOTNGH-20); kiem-tra-tai.js chỉ có đường đi của bệnh nhân.
// Chạy trên stack đã có dữ liệu mẫu lớn (SEED_VOLUME=true), nâng GATEWAY_*RATE_LIMIT rất lớn như kiem-tra-tai.js.
// Từ thư mục gốc:
//   node scripts/load-test/do-man-hinh.js              đo mọi API, ghi ket-qua/man-hinh-<NHAN>.json
//   node scripts/load-test/do-man-hinh.js dong-thoi    5 tệp xuất cùng lúc, theo dõi bộ nhớ booking-service, report-service
// Biến môi trường: BASE_URL (mặc định http://127.0.0.1:8080), NHAN (mặc định truoc), MYSQL_CONTAINER, MAT_KHAU, ADMIN_EMAIL,
// ADMIN_MAT_KHAU (mặc định đọc từ container identity-service).
//
// Mỗi API đo 2 lượt. Lượt 1 tắt general_log: 1 lần chạy nóng, 5 lần đo. Lượt 2 bật general_log: đếm câu SELECT, EXPLAIN từng câu.
// Com_select không dùng được vì đếm cả job nền; general_log cho đúng các câu của API đang đo (bỏ các câu của job gửi thông báo).
const { execFile, execFileSync } = require('child_process');
const fs = require('fs');
const http = require('http');
const https = require('https');
const path = require('path');
const zlib = require('zlib');

const BASE_URL = process.env.BASE_URL || 'http://127.0.0.1:8080';
const NHAN = process.env.NHAN || 'truoc';
const MYSQL = process.env.MYSQL_CONTAINER || 'eclinic-mysql';
const MAT_KHAU = process.env.MAT_KHAU || 'Demo@123';
const ADMIN_EMAIL = process.env.ADMIN_EMAIL || 'admin@eclinic.local';
const THU_MUC_KET_QUA = path.join(__dirname, 'ket-qua');

// Ngân sách của phase 3, đặt trước khi đo (ms cho 1 người dùng)
const NGAN_SACH = { danhSach: 200, nang: 500, xuat: 3000 };
const CO_TOI_DA_GZIP = 100 * 1024;
const SO_LAN_DO = 5;
const NGUONG_SO_DONG = 1000;
// API không phân trang không so được 20 / 100: nhiều câu SELECT thế này là dấu hiệu N+1 (báo cáo cố định 15 câu)
const SO_SELECT_DANG_NGO = 15;
const BANG_TANG_MOI_NGAY = ['lich_lam_viec', 'khung_gio_kham', 'lich_hen', 'lich_su_thay_doi'];

const ngay = (lech) => new Date(Date.now() + lech * 864e5 + 7 * 36e5).toISOString().slice(0, 10);
const khoang = (tu, den) => `tuNgay=${ngay(tu)}&denNgay=${ngay(den)}`;

function sql(cau) {
    return execFileSync('docker', ['exec', '-i', MYSQL, 'sh', '-c',
        'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -uroot --default-character-set=utf8mb4 -N -B "$MYSQL_DATABASE"'],
    { input: cau, encoding: 'utf8', maxBuffer: 256 * 1024 * 1024, stdio: ['pipe', 'pipe', 'ignore'] }).trim();
}

function goi(duongDan, opts = {}) {
    const url = new URL(duongDan, BASE_URL);
    const noiDung = opts.body ? JSON.stringify(opts.body) : null;
    return new Promise((xong, loi) => {
        const batDau = process.hrtime.bigint();
        const req = (url.protocol === 'https:' ? https : http).request(url, {
            method: opts.method || 'GET',
            family: 4,
            headers: {
                'Accept-Encoding': 'gzip',
                'Accept-Language': 'vi',
                'Content-Type': 'application/json',
                ...(opts.token ? { Authorization: `Bearer ${opts.token}` } : {}),
            },
        }, (res) => {
            const phan = [];
            res.on('data', (d) => phan.push(d));
            res.on('end', () => {
                const ms = Number(process.hrtime.bigint() - batDau) / 1e6;
                const daNen = Buffer.concat(phan);
                const goc = res.headers['content-encoding'] === 'gzip' ? zlib.gunzipSync(daNen) : daNen;
                xong({ status: res.statusCode, ms, soByteGoc: goc.length, soByteGzip: daNen.length, goc,
                    json: (res.headers['content-type'] || '').includes('json') ? JSON.parse(goc.toString('utf8') || 'null') : null });
            });
        });
        req.setTimeout(120000, () => req.destroy(new Error(`Hết giờ chờ: ${duongDan}`)));
        req.on('error', loi);
        if (noiDung) req.write(noiDung);
        req.end();
    });
}

async function dangNhap(email, matKhau) {
    const res = await goi('/api/auth/login', { method: 'POST', body: { email, matKhau } });
    if (res.status !== 200) throw new Error(`Đăng nhập ${email} lỗi ${res.status}`);
    return res.json.duLieu.accessToken;
}

async function chuanBi() {
    const matKhauAdmin = process.env.ADMIN_MAT_KHAU
        || execFileSync('docker', ['exec', 'eclinic-identity-service', 'printenv', 'ADMIN_PASSWORD'], { encoding: 'utf8' }).trim();
    const token = {
        admin: await dangNhap(ADMIN_EMAIL, matKhauAdmin),
        bacSi: await dangNhap('bacsi01@eclinic.local', MAT_KHAU),
        benhNhan: await dangNhap('benhnhan01@eclinic.local', MAT_KHAU),
    };
    const dauTien = async (duongDan, vai) => {
        const duLieu = (await goi(duongDan, { token: token[vai] })).json.duLieu;
        return (duLieu.noiDung || duLieu)[0];
    };
    const idBacSi = (await dauTien('/api/catalog/quan-tri/bac-si?kichThuoc=1', 'admin')).id;
    const idCa = (await dauTien(`/api/booking/quan-tri/lich-lam-viec?${khoang(0, 13)}`, 'admin')).idLichLamViec;
    const idHoSo = (await dauTien('/api/booking/quan-tri/ho-so-benh-nhan?kichThuoc=1', 'admin')).id;
    return { token, idBacSi, idCa, idHoSo };
}

/** nhom: danhSach | nang | xuat; trang: có kichThuoc thì đo ở 20 và 100. */
function danhSachApi({ idBacSi, idCa, idHoSo }) {
    const qt = '/api/booking/quan-tri';
    const bs = '/api/booking/bac-si/toi';
    const a = (ten, nhom, vai, url, trang = false) => ({ ten, nhom, vai, url, trang });
    return [
        a('chuyen-khoa', 'danhSach', null, '/api/catalog/chuyen-khoa', true),
        a('bac-si', 'danhSach', null, '/api/catalog/bac-si', true),
        a('bac-si-chi-tiet', 'danhSach', null, `/api/catalog/bac-si/${idBacSi}`),
        a('qt-bac-si', 'danhSach', 'admin', '/api/catalog/quan-tri/bac-si', true),
        a('qt-chuyen-khoa', 'danhSach', 'admin', '/api/catalog/quan-tri/chuyen-khoa', true),
        a('qt-phong-kham', 'danhSach', 'admin', '/api/catalog/quan-tri/phong-kham', true),
        a('phong-kham', 'danhSach', 'admin', '/api/catalog/phong-kham'),
        a('tai-khoan', 'danhSach', 'admin', '/api/users', true),
        a('tai-khoan-tim', 'danhSach', 'admin', '/api/users?tuKhoa=benhnhan', true),
        a('qt-lich-hen', 'danhSach', 'admin', `${qt}/lich-hen?${khoang(-180, 14)}`, true),
        a('qt-lich-hen-tim', 'danhSach', 'admin', `${qt}/lich-hen?${khoang(-180, 14)}&tuKhoa=Nguy`, true),
        a('qt-ho-so', 'danhSach', 'admin', `${qt}/ho-so-benh-nhan`, true),
        a('qt-ho-so-lich-hen', 'danhSach', 'admin', `${qt}/ho-so-benh-nhan/${idHoSo}/lich-hen`, true),
        a('qt-cho-xac-minh', 'danhSach', 'admin', `${qt}/ho-so-benh-nhan/cho-xac-minh`, true),
        a('qt-danh-gia', 'danhSach', 'admin', `${qt}/danh-gia`, true),
        a('qt-thuoc', 'danhSach', 'admin', '/api/medical/quan-tri/thuoc', true),
        a('qt-yeu-cau', 'danhSach', 'admin', `${qt}/yeu-cau-doi-lich`, true),
        a('qt-ngay-nghi', 'danhSach', 'admin', `${qt}/ngay-nghi?${khoang(-300, 65)}`),
        a('qt-cau-hinh-lich-su', 'danhSach', 'admin', `${qt}/cau-hinh/lich-su`, true),
        a('qt-can-doi-lich', 'danhSach', 'admin', `${qt}/lich-lam-viec/can-doi-lich`, true),
        a('qt-lich-cua-ca', 'danhSach', 'admin', `${qt}/lich-lam-viec/${idCa}/lich-hen`),
        a('qt-lich-su-ca', 'danhSach', 'admin', `${qt}/lich-lam-viec/${idCa}/lich-su`),
        a('bs-danh-gia', 'danhSach', 'bacSi', `${bs}/danh-gia`, true),
        a('bs-yeu-cau', 'danhSach', 'bacSi', `${bs}/yeu-cau-doi-lich`, true),
        a('bs-hang-doi', 'danhSach', 'bacSi', `${bs}/hang-doi`),
        a('bs-lich-lam-viec', 'danhSach', 'bacSi', `${bs}/lich-lam-viec?${khoang(-14, 20)}`),
        a('bs-lich-hen-khoang', 'danhSach', 'bacSi', `${bs}/lich-hen/lich?${khoang(-14, 20)}`),
        a('bn-lich-su-kham', 'danhSach', 'benhNhan', '/api/booking/lich-su-kham/cua-toi', true),
        a('bn-lich-hen', 'danhSach', 'benhNhan', '/api/booking/lich-hen/cua-toi'),
        a('bn-thong-bao', 'danhSach', 'benhNhan', '/api/notification/thong-bao', true),
        a('bn-chua-doc', 'danhSach', 'benhNhan', '/api/notification/thong-bao/so-chua-doc'),
        a('bn-phien-chat', 'danhSach', 'benhNhan', '/api/chatbot/phien'),
        a('bn-phien-dang-nhap', 'danhSach', 'benhNhan', '/api/users/me/sessions'),
        a('qt-tong-quan', 'nang', 'admin', `${qt}/tong-quan`),
        a('bs-tong-quan', 'nang', 'bacSi', `${bs}/tong-quan`),
        ...['NGAY', 'TUAN', 'THANG', 'NAM'].map((ky) => a(`bao-cao-${ky}`, 'nang', 'admin', `/api/report/quan-tri/thong-ke?ky=${ky}`)),
        a('bao-cao-366-ngay', 'nang', 'admin', `/api/report/quan-tri/thong-ke?ky=TUY_CHON&${khoang(-300, 65)}`),
        a('lich-toan-vien-14', 'nang', 'admin', `${qt}/lich-lam-viec?${khoang(0, 13)}`),
        a('lich-toan-vien-35', 'nang', 'admin', `${qt}/lich-lam-viec?${khoang(-14, 20)}`),
        a('lich-toan-vien-42', 'nang', 'admin', `${qt}/lich-lam-viec?${khoang(-14, 27)}`),
        a('gio-lam-theo-tuan', 'nang', 'admin', `${qt}/lich-lam-viec/gio-lam-theo-tuan?${khoang(-14, 20)}`),
        a('xuat-excel-lich', 'xuat', 'admin', `${qt}/lich-lam-viec/xuat-excel?${khoang(-14, 20)}`),
        a('pdf-toan-vien', 'xuat', 'admin', `${qt}/lich-lam-viec/pdf/toan-vien?${khoang(0, 13)}`),
        a('pdf-toan-vien-35', 'xuat', 'admin', `${qt}/lich-lam-viec/pdf/toan-vien?${khoang(-14, 20)}`),
        a('pdf-toan-vien-42', 'xuat', 'admin', `${qt}/lich-lam-viec/pdf/toan-vien?${khoang(-14, 27)}`),
        a('pdf-bac-si', 'xuat', 'admin', `${qt}/lich-lam-viec/pdf/bac-si/${idBacSi}?${khoang(-14, 20)}`),
        a('bs-pdf', 'xuat', 'bacSi', `${bs}/lich-lam-viec/pdf?${khoang(-14, 20)}`),
        a('mau-nhap-excel', 'xuat', 'admin', `${qt}/lich-lam-viec/nhap-excel/mau`),
        a('bao-cao-excel', 'xuat', 'admin', '/api/report/quan-tri/thong-ke/xuat-excel?ky=NAM'),
    ];
}

const voiKichThuoc = (url, kichThuoc) => `${url}${url.includes('?') ? '&' : '?'}kichThuoc=${kichThuoc}`;

const trungVi = (mang) => [...mang].sort((a, b) => a - b)[Math.floor(mang.length / 2)];

/** Đường dẫn khoá của JSON, để so sánh hình dạng response giữa 2 lần đo. */
function duongDanKhoa(gt, tien = '', ra = new Set()) {
    if (Array.isArray(gt)) gt.forEach((x) => duongDanKhoa(x, `${tien}[]`, ra));
    else if (gt && typeof gt === 'object') {
        Object.keys(gt).forEach((k) => {
            ra.add(`${tien}.${k}`);
            duongDanKhoa(gt[k], `${tien}.${k}`, ra);
        });
    }
    return ra;
}

async function doThoiGian(api, token) {
    const url = api.trang ? voiKichThuoc(api.url, 100) : api.url;
    const opts = { token: api.vai ? token[api.vai] : undefined };
    const dau = await goi(url, opts);
    const cacMs = [];
    let cuoi = dau;
    for (let i = 0; i < SO_LAN_DO; i++) {
        cuoi = await goi(url, opts);
        cacMs.push(cuoi.ms);
    }
    return {
        status: cuoi.status,
        msTrungVi: Math.round(trungVi(cacMs)),
        msMax: Math.round(Math.max(...cacMs)),
        soByteGoc: cuoi.soByteGoc,
        soByteGzip: cuoi.soByteGzip,
        khoa: cuoi.json ? [...duongDanKhoa(cuoi.json)].sort() : null,
    };
}

function cauSelectVuaChay() {
    const dong = sql(`SELECT HEX(argument) FROM mysql.general_log
        WHERE command_type IN ('Query', 'Execute') AND argument NOT LIKE '%general_log%';`);
    if (!dong) return [];
    return dong.split('\n').map((h) => Buffer.from(h, 'hex').toString('utf8').replace(/\s+/g, ' ').trim())
        .filter((c) => /^select/i.test(c) && !c.includes('@@') && !c.includes('hang_cho_gui') && !/^select 1\b/i.test(c));
}

/** Trả các dòng EXPLAIN đáng chú ý: quét toàn bảng, hoặc xét quá nhiều dòng trên bảng lớn dần mỗi ngày. */
function dongCanChuY(cau) {
    let ra;
    try {
        ra = sql(`EXPLAIN ${cau};`);
    } catch {
        return [{ loi: cau.slice(0, 200) }];
    }
    // EXPLAIN in bí danh của Hibernate (k1_0), không phải tên bảng: dò lại từ câu SQL
    const bangThat = {};
    for (const [, ten, biDanh] of cau.matchAll(/\b(?:from|join)\s+(\w+)\s+(?:as\s+)?(\w+)/gi)) bangThat[biDanh] = ten;
    return ra.split('\n').map((d) => d.split('\t')).filter((c) => c.length >= 11)
        .map(([, , bang, , kieu, , khoa, , , soDong, , thuThem]) => ({ bang: bangThat[bang] || bang, kieu, khoa,
            soDong: Number(soDong), thuThem }))
        .filter((d) => BANG_TANG_MOI_NGAY.includes(d.bang) && (d.kieu === 'ALL' || d.soDong > NGUONG_SO_DONG));
}

async function demSelect(url, opts) {
    sql('TRUNCATE TABLE mysql.general_log;');
    await goi(url, opts);
    await new Promise((r) => setTimeout(r, 80));
    return cauSelectVuaChay();
}

async function doCauLenh(api, token) {
    const opts = { token: api.vai ? token[api.vai] : undefined };
    const ket = {};
    if (api.trang) {
        ket.soSelect20 = (await demSelect(voiKichThuoc(api.url, 20), opts)).length;
        const cau100 = await demSelect(voiKichThuoc(api.url, 100), opts);
        ket.soSelect100 = cau100.length;
        ket.cac = cau100;
    } else {
        ket.cac = await demSelect(api.url, opts);
        ket.soSelect20 = ket.cac.length;
        ket.soSelect100 = ket.cac.length;
    }
    const daXet = new Set();
    ket.quet = [];
    ket.cac.forEach((c) => {
        if (!daXet.has(c)) {
            daXet.add(c);
            dongCanChuY(c).forEach((d) => ket.quet.push({ ...d, cau: c.slice(0, 300) }));
        }
    });
    delete ket.cac;
    return ket;
}

async function doTatCa() {
    const dl = await chuanBi();
    const cacApi = danhSachApi(dl);
    sql("SET GLOBAL general_log = 'OFF';");
    const ketQua = [];
    for (const api of cacApi) {
        ketQua.push({ ten: api.ten, nhom: api.nhom, url: api.url, ...(await doThoiGian(api, dl.token)) });
    }
    sql("SET GLOBAL log_output = 'TABLE'; SET GLOBAL general_log = 'ON';");
    try {
        for (let i = 0; i < cacApi.length; i++) {
            Object.assign(ketQua[i], await doCauLenh(cacApi[i], dl.token));
        }
    } finally {
        sql("SET GLOBAL general_log = 'OFF'; TRUNCATE TABLE mysql.general_log;");
    }

    const vuot = [];
    console.log(['API', 'status', 'ms', 'max', 'gốc B', 'gzip B', 'SELECT 20/100', 'quét'].join('\t'));
    ketQua.forEach((k) => {
        const loi = [];
        if (k.status >= 400) loi.push(`status ${k.status}`);
        if (k.msTrungVi > NGAN_SACH[k.nhom]) loi.push(`${k.msTrungVi} ms > ${NGAN_SACH[k.nhom]}`);
        if (k.khoa && k.soByteGzip > CO_TOI_DA_GZIP) loi.push(`${k.soByteGzip} B gzip`);
        if (Math.abs(k.soSelect100 - k.soSelect20) > 1) loi.push(`SELECT ${k.soSelect20} -> ${k.soSelect100}`);
        if (k.soSelect100 > SO_SELECT_DANG_NGO) loi.push(`${k.soSelect100} SELECT`);
        if (k.quet.length) loi.push(`quét ${k.quet.map((d) => `${d.bang}:${d.kieu}:${d.soDong}`).join(',')}`);
        if (loi.length) vuot.push(`${k.ten}: ${loi.join('; ')}`);
        console.log([k.ten, k.status, k.msTrungVi, k.msMax, k.soByteGoc, k.soByteGzip, `${k.soSelect20}/${k.soSelect100}`,
            k.quet.length].join('\t'));
    });
    console.log(`\nVượt ngân sách / cần xem (${vuot.length}):`);
    vuot.forEach((v) => console.log(`  ${v}`));

    fs.mkdirSync(THU_MUC_KET_QUA, { recursive: true });
    fs.writeFileSync(path.join(THU_MUC_KET_QUA, `man-hinh-${NHAN}.json`), JSON.stringify(ketQua, null, 2));
}

function boNhoMiB(tuongDoi) {
    const [so, donVi] = [parseFloat(tuongDoi), tuongDoi.replace(/[\d.\s]/g, '')];
    return donVi.startsWith('Gi') ? so * 1024 : so;
}

async function dongThoi() {
    const dl = await chuanBi();
    const xuat = danhSachApi(dl).filter((a) => ['xuat-excel-lich', 'pdf-toan-vien-35', 'bao-cao-excel'].includes(a.ten));
    const dinh = {};
    let dangChay = true;
    const lay = () => new Promise((xong) => {
        execFile('docker', ['stats', '--no-stream', '--format', '{{.Name}} {{.MemUsage}}', 'eclinic-booking-service',
            'eclinic-report-service'], (loi, ra) => {
            if (!loi) {
                ra.trim().split('\n').forEach((d) => {
                    const [ten, dung] = d.split(' ');
                    dinh[ten] = Math.max(dinh[ten] || 0, boNhoMiB(dung));
                });
            }
            xong();
        });
    });
    const theoDoi = (async () => {
        while (dangChay) await lay();
    })();
    for (const api of xuat) {
        const batDau = Date.now();
        const cac = await Promise.all(Array.from({ length: 5 }, () => goi(api.url, { token: dl.token[api.vai] })));
        console.log(`${api.ten}: 5 cùng lúc ${Date.now() - batDau} ms, status ${[...new Set(cac.map((c) => c.status))].join(',')}`);
    }
    dangChay = false;
    await theoDoi;
    console.log('Bộ nhớ đỉnh (MiB):', dinh);
    ['eclinic-booking-service', 'eclinic-report-service'].forEach((ten) => console.log(ten, 'RestartCount',
        execFileSync('docker', ['inspect', '-f', '{{.RestartCount}}', ten], { encoding: 'utf8' }).trim()));
}

(process.argv[2] === 'dong-thoi' ? dongThoi() : doTatCa()).catch((e) => {
    console.error(e);
    process.exit(1);
});
