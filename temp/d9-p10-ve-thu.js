// Phase 10: draw every demo screen in Node with a permissive fake DOM, in both languages, to catch what `node --check`
// cannot: undefined identifiers, templates not called as functions, Vietnamese left in the English HTML.
// Not a browser test: API calls answer 500, so only each screen's frame and error branches run.
//   node temp/d9-p10-ve-thu.js
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const GOC = path.join(__dirname, '..', 'scripts', 'demo');
const CO_DAU = /[À-ÖØ-öø-ỹ]/;
const htmlDaChen = [];
const chuDaDat = [];

function phanTu() {
    const kho = {
        classList: { add() {}, remove() {}, toggle() {}, contains: () => false },
        dataset: {}, style: {}, value: '', files: [], length: 0, isConnected: true, checked: false, open: false,
        selectedOptions: [{ textContent: '' }],
    };
    return new Proxy(function () {}, {
        get(_, k) {
            if (k in kho) return kho[k];
            if (k === Symbol.iterator) return [][Symbol.iterator].bind([]);
            if (k === Symbol.toPrimitive) return () => '';
            if (k === 'then') return undefined;
            if (k === 'insertAdjacentHTML') return (viTri, html) => { htmlDaChen.push(String(html)); };
            if (k === 'elements') return new Proxy({}, { get: () => phanTu() });
            return phanTu();
        },
        set(_, k, v) {
            if (k === 'textContent' || k === 'placeholder' || k === 'title' || k === 'alt') chuDaDat.push(String(v));
            if (k === 'innerHTML') htmlDaChen.push(String(v));
            kho[k] = v;
            return true;
        },
        apply: () => phanTu(),
    });
}

const kho = new Map();
const luuTru = { getItem: (k) => (kho.has(k) ? kho.get(k) : null), setItem: (k, v) => kho.set(k, String(v)), removeItem: (k) => kho.delete(k) };
const document = phanTu();
const moiTruong = {
    console, URLSearchParams, setTimeout: () => 0, setInterval: () => 0, clearInterval() {}, clearTimeout() {}, FormData: class { entries() { return []; } },
    Node: class {}, document, localStorage: luuTru, sessionStorage: luuTru, navigator: phanTu(),
    location: { pathname: '/', search: '', origin: 'http://localhost:5173', href: 'http://localhost:5173/' },
    history: { pushState() {}, replaceState() {} }, confirm: () => false, alert() {}, prompt: () => null,
    fetch: async () => ({ status: 500, ok: false, json: async () => ({ thongDiep: 'loi gia', maLoi: 'LOI_GIA' }) }),
    encodeURIComponent, decodeURIComponent, JSON, Math, Date, Promise, String, Number, Object, Array, Map, Set, RegExp, Error, Symbol, Reflect, Proxy,
};
moiTruong.window = moiTruong;
moiTruong.window.CAU_HINH = { apiUrl: 'http://127.0.0.1:8080', googleClientId: '' };
moiTruong.window.scrollTo = () => {};
moiTruong.window.addEventListener = () => {};
vm.createContext(moiTruong);

const cacFile = ['chung.js', ...fs.readdirSync(path.join(GOC, 'man-hinh')).sort().map((t) => 'man-hinh/' + t)];
// const / let ở mức file không thành thuộc tính của ngữ cảnh: nối cả demo thành 1 script như trình duyệt dùng chung phạm vi
const ma = cacFile.map((ten) => fs.readFileSync(path.join(GOC, ten), 'utf8')).join('\n;\n')
    + '\n;globalThis.__demo = { cacManHinh, datNgonNgu: (m) => { ngonNgu = m; }, datPhien: (tk) => { accessToken = tk ? "x" : null; taiKhoan = tk; },'
    + ' veKhung, veChat, veNoiDungChat, theGoiY, hienPhieu, ketQuaKham, khoiHuyDoi, datPhienChat: (p) => { phienChat = p; }, TRANG_THAI, THU, CAN_DOI_LICH, t };';
vm.runInContext(ma, moiTruong, { filename: 'demo-gop.js' });
const demo = moiTruong.__demo;

const loi = [];
const choCdn = [];
const ghiLoi = (dau, e) => loi.push(`${dau}: ${e && e.stack ? e.stack.split('\n').slice(0, 2).join(' | ') : e}`);

async function veMoi(ngonNgu) {
    demo.datNgonNgu(ngonNgu);
    for (const mh of demo.cacManHinh) {
        const ten = String([].concat(mh.duongDan)[0]);
        const vaiTro = mh.vaiTro && mh.vaiTro !== 'DANG_NHAP' ? mh.vaiTro : 'BENH_NHAN';
        demo.datPhien(mh.vaiTro ? { id: 1, hoTen: 'Nguyen Van A', email: 'a@b.c', vaiTro, ngonNgu: 'VI' } : null);
        const truoc = htmlDaChen.length, truocChu = chuDaDat.length;
        try {
            const tieuDe = String(mh.tieuDe);
            if (ngonNgu === 'en' && CO_DAU.test(tieuDe)) loi.push(`${ten} [en]: tieuDe còn tiếng Việt: ${tieuDe}`);
            const khop = Object.prototype.toString.call(mh.duongDan) === '[object RegExp]' ? ['/phieu-kham/abc', 'abc'] : null;
            // Màn hình chờ thư viện CDN (lịch, QR) không bao giờ xong ở đây: chờ tối đa 300 ms rồi đi tiếp
            let xongRoi = false;
            const donDep = await Promise.race([Promise.resolve(mh.ve(phanTu(), { thamSo: new URLSearchParams(''), khop, duongDan: ten })).finally(() => { xongRoi = true; }),
                new Promise((xong) => setTimeout(() => { if (!xongRoi) choCdn.push(ten); xong(null); }, 300))]);
            if (typeof donDep === 'function') donDep();
        } catch (e) {
            ghiLoi(`${ten} [${ngonNgu}]`, e);
        }
        const html = htmlDaChen.slice(truoc).join('\n');
        for (const [dau, mau] of [['undefined', /undefined/], ['[object', /\[object/], ['=>', /=>/], ['function', /function\s*\(/], ['${', /\$\{/]]) {
            if (mau.test(html)) loi.push(`${ten} [${ngonNgu}]: HTML chứa "${dau}"`);
        }
        if (ngonNgu === 'en') {
            const sot = html.split('\n').filter((d) => CO_DAU.test(d)).slice(0, 3);
            for (const d of sot) loi.push(`${ten} [en]: HTML còn tiếng Việt: ${d.trim().slice(0, 100)}`);
            const chuSot = chuDaDat.slice(truocChu).filter((c) => CO_DAU.test(c)).slice(0, 3);
            for (const c of chuSot) loi.push(`${ten} [en]: chữ còn tiếng Việt: ${c.slice(0, 100)}`);
        }
    }
}

// Khối dùng chung và khung chat với dữ liệu mẫu (đủ mọi loại thẻ)
function veKhoiChung(ngonNgu) {
    demo.datNgonNgu(ngonNgu);
    const truocChu = chuDaDat.length;
    try {
        for (const vaiTro of [null, 'BENH_NHAN', 'BAC_SI', 'QUAN_TRI_VIEN']) {
            demo.datPhien(vaiTro ? { id: 7, hoTen: 'Tran Thi B', email: 'b@c.d', vaiTro, ngonNgu: 'VI' } : null);
            demo.veKhung(vaiTro === 'BAC_SI' ? '/doctor/dashboard' : vaiTro === 'QUAN_TRI_VIEN' ? '/admin/dashboard' : '/appointment');
        }
        const goiY = {
            chuyenKhoa: { id: 1, ten: 'Cardiology' },
            bacSi: [{ id: 3, hoTen: 'Le Van C', hocVi: 'TS.BS', chucVu: 'Head', anhDaiDien: null, ngaySomNhat: '2026-10-20', soChoConLai: 4 },
                { id: 4, hoTen: 'Pham D', hocVi: null, chucVu: null, anhDaiDien: null, ngaySomNhat: null, soChoConLai: 0 }],
            lichKham: [{ idBacSi: 3, hoTenBacSi: 'TS.BS Le Van C', idLichLamViec: 9, ngay: '2026-10-20', tenPhong: 'Room 101', tang: 'Floor 1',
                khungGio: [{ gioBatDau: '2026-10-20T08:00:00', gioKetThuc: '2026-10-20T09:00:00', soChoConLai: 2 }] }],
            lichHenCuaToi: [{ maPhieuKham: 'tok', maTraCuu: 'EC-1', linkPhieuKham: 'http://x/phieu-kham/tok', trangThai: 'DA_XAC_NHAN', ngay: '2026-10-21',
                gioBatDauKhung: '2026-10-21T08:00:00', gioKetThucKhung: '2026-10-21T09:00:00', hoTenBacSi: 'Le Van C', tenChuyenKhoa: 'Cardiology',
                tenPhong: 'Room 101', tang: 'Floor 1', hoTenBenhNhan: 'Tran Thi B', laBanThan: false, canDoiLich: true }],
            lanKham: [{ maPhieuKham: 'tok2', maTraCuu: 'EC-2', ngay: '2026-09-01', hoTenBacSi: 'Le Van C', tenChuyenKhoa: 'Cardiology',
                hoTenBenhNhan: 'Tran Thi B', laBanThan: true, ngayTaiKhamDeXuat: '2026-11-01' },
            { maPhieuKham: 'tok3', maTraCuu: 'EC-3', ngay: '2026-08-01', hoTenBacSi: 'Le Van C', tenChuyenKhoa: 'Cardiology',
                hoTenBenhNhan: 'Tran Thi B', laBanThan: true, ngayTaiKhamDeXuat: null }],
        };
        const soThe = demo.theGoiY(goiY).length;
        if (soThe !== 7) loi.push(`theGoiY [${ngonNgu}]: ${soThe} thẻ, cần 7`);
        demo.datPhienChat({ idPhien: 1, maKhach: null, chu: '7', daDongY: true, tinNhan: [
            { id: 1, nguoiGui: 'CHATBOT', loai: 'LOI_CHAO', noiDung: 'hello', goiY: null },
            { id: 2, nguoiGui: 'NGUOI_DUNG', loai: 'CAU_HOI', noiDung: 'q', goiY: null },
            { id: 3, nguoiGui: 'CHATBOT', loai: 'TRA_LOI', noiDung: 'a', goiY },
            { id: 4, nguoiGui: 'CHATBOT', loai: 'KHAN_CAP', noiDung: '115', goiY: null },
            { id: 5, nguoiGui: 'CHATBOT', loai: 'CAN_DONG_Y', noiDung: 'consent', goiY: null }] });
        demo.veNoiDungChat();
        demo.datPhienChat(null);
        demo.ketQuaKham({ chanDoan: 'x', ghiChu: 'y', ngayTaiKhamDeXuat: '2026-11-01', donThuoc: [{ tenThuoc: 'A', donVi: 'vien', lieuDung: '1', soLanMoiNgay: 2, soNgayDung: 3, ghiChuSuDung: null }] });
        demo.ketQuaKham({ chanDoan: 'x', donThuoc: [] });
        const lich = { duocHuyDoi: true, hanHuyDoi: '2026-10-20T08:00:00', bacSi: { id: 3 }, trangThai: 'DA_XAC_NHAN', canDoiLich: true };
        demo.khoiHuyDoi({ lich, canSoDienThoai: true, gui: async () => ({ status: 500, json: null }), khiXong() {} });
        demo.khoiHuyDoi({ lich: { ...lich, duocHuyDoi: false }, canSoDienThoai: false, gui: async () => ({}), khiXong() {} });
        demo.hienPhieu(phanTu(), { maPhieuKham: 'tok', maTraCuu: 'EC-1', soThuTu: 3, trangThai: 'CHO_XAC_NHAN', canDoiLich: true, lyDoHuy: 'r', ngay: '2026-10-20',
            gioKhamDuKien: '2026-10-20T08:10:00', gioBatDauKhung: '2026-10-20T08:00:00', gioKetThucKhung: '2026-10-20T09:00:00',
            bacSi: { hocVi: 'BS', hoTen: 'Le Van C' }, tenChuyenKhoa: 'Cardiology', phongKham: { tenPhong: 'Room 101', tang: 'Floor 1' },
            benhNhan: { hoTen: 'Tran Thi B', namSinh: 2015, gioiTinh: 'NU', cccd: '0', soDienThoai: '0' },
            nguoiGiamHo: { hoTen: 'Tran E', quanHe: 'ME', soDienThoai: '0' }, canNguoiGiamHoDiCung: true, lyDoKham: 'ho', ngayDat: '2026-10-13T10:00:00',
            linkPhieuKham: 'http://x' });
    } catch (e) {
        ghiLoi(`khối chung [${ngonNgu}]`, e);
    }
    if (ngonNgu === 'en') {
        for (const c of chuDaDat.slice(truocChu).filter((x) => CO_DAU.test(x) && x !== 'Tiếng Việt').slice(0, 10)) loi.push(`khối chung [en]: chữ còn tiếng Việt: ${c.slice(0, 100)}`);
    }
}

(async () => {
    // Bảng nhãn: đọc theo ngôn ngữ, Object.entries / map vẫn chạy
    demo.datNgonNgu('en');
    const en = [demo.TRANG_THAI.DA_HUY, Object.entries(demo.TRANG_THAI).length, demo.THU[0], demo.THU.map((x) => x).length, String(demo.CAN_DOI_LICH).slice(0, 9)].join('|');
    demo.datNgonNgu('vi');
    const vi = [demo.TRANG_THAI.DA_HUY, Object.keys(demo.TRANG_THAI).length, demo.THU[0], `${demo.CAN_DOI_LICH}`.slice(0, 7)].join('|');
    if (en !== 'Cancelled|6|Sun|7|The shift') loi.push('bảng nhãn en: ' + en);
    if (vi !== 'Đã hủy|6|CN|Ca khám') loi.push('bảng nhãn vi: ' + vi);
    for (const ngonNgu of ['vi', 'en']) {
        await veMoi(ngonNgu);
        veKhoiChung(ngonNgu);
    }
    await new Promise((xong) => setImmediate(xong));
    console.log(`${demo.cacManHinh.length} màn hình x 2 ngôn ngữ, ${htmlDaChen.length} khung HTML, ${chuDaDat.length} chữ đã đặt; chờ quá 300 ms (thư viện CDN): ${[...new Set(choCdn)].join(', ') || 'không'}`);
    if (loi.length) {
        console.log(loi.join('\n'));
        console.log(`\n${loi.length} lỗi`);
        process.exit(1);
    }
    console.log('Không lỗi.');
})();
