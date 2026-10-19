// Phần dùng chung của trang demo: gọi API, phiên đăng nhập, hàm dựng DOM, ngày giờ, bộ định tuyến và khung trang.
// Các file man-hinh/*.js dùng thẳng các hàm / hằng khai báo ở đây (script thường, không phải module).
//
// Quy ước:
// - Dữ liệu từ API luôn đưa vào trang bằng textContent (hàm the()), không dùng innerHTML. Hàm khuon() chỉ nhận chuỗi HTML
//   viết sẵn trong mã nguồn (khung form, tiêu đề), không bao giờ ghép dữ liệu từ API vào.
// - Access token chỉ nằm trong biến accessToken, không lưu localStorage; refresh token nằm trong cookie HttpOnly.
// - Ngôn ngữ (nút EN | VI ở thanh trên): mọi lời gọi API gửi header Accept-Language, backend trả thongDiep và nội dung
//   danh mục theo đó. Lựa chọn lưu ở localStorage của trình duyệt; khi đã đăng nhập còn lưu vào tài khoản
//   (PUT /api/users/me/language) để email / nhắc lịch dùng đúng ngôn ngữ.
// - Chữ của chính trang demo (nhãn, nút, gợi ý, câu báo lỗi phía trình duyệt) viết thành cặp ngay tại chỗ dùng:
//   t('Tiếng Việt', 'English'). Chữ khai báo 1 lần lúc tải file (hằng số, tieuDe của dangKy) dùng cap(vi, en) và được
//   đổi ra chữ lúc vẽ. Dữ liệu từ API KHÔNG đưa qua t(): nó đã theo ngôn ngữ của request, tên riêng giữ nguyên.
//   Kiểm tra chỗ sót: node scripts/kiem-tra-demo-ngon-ngu.js

const API_URL = window.CAU_HINH.apiUrl;
const GOOGLE_CLIENT_ID = window.CAU_HINH.googleClientId;

// Ngôn ngữ gửi cho backend: 'vi' hoặc 'en'. daChonNgonNgu = trình duyệt này đã bấm chọn (khác với "chưa chọn, mặc định vi")
const KHOA_NGON_NGU = 'eclinic-demo-ngon-ngu';
let daChonNgonNgu = false;
let ngonNgu = 'vi';
try {
    const daLuu = localStorage.getItem(KHOA_NGON_NGU);
    if (daLuu === 'vi' || daLuu === 'en') { ngonNgu = daLuu; daChonNgonNgu = true; }
} catch { /* localStorage bị chặn (cửa sổ ẩn danh...): dùng mặc định */ }
document.documentElement.lang = ngonNgu;

// ===== Chữ 2 ngôn ngữ của trang demo =====
// t(vi, en): chữ theo ngôn ngữ đang chọn, gọi lúc VẼ (đổi ngôn ngữ thì màn hình được vẽ lại nên chữ đổi theo)
const t = (vi, en) => (ngonNgu === 'en' ? en : vi);
// cap(vi, en): cặp chữ khai báo trước, chưa chọn ngôn ngữ; chuCua(x) đổi cặp ra chữ (chuỗi thường thì giữ nguyên)
class CapChu {
    constructor(vi, en) { this.vi = vi; this.en = en; }
    toString() { return t(this.vi, this.en); }
}
const cap = (vi, en) => new CapChu(vi, en);
const chuCua = (x) => (x instanceof CapChu ? x.toString() : x);
// Bảng nhãn (object hoặc mảng) có giá trị là cap(): đọc NHAN[ma] ra luôn chữ của ngôn ngữ đang chọn, nên nơi dùng viết
// như với bảng chuỗi thường (kể cả Object.entries / Object.keys / map)
const nhan = (bang) => new Proxy(bang, { get: (goc, khoa, noiNhan) => chuCua(Reflect.get(goc, khoa, noiNhan)) });

const TRANG_THAI = nhan({
    DA_XAC_NHAN: cap('Đã xác nhận', 'Confirmed'),
    BI_TU_CHOI: cap('Bác sĩ đã hủy', 'Cancelled by doctor'), DA_HOAN_THANH: cap('Đã hoàn thành', 'Completed'),
    DA_HUY: cap('Đã hủy', 'Cancelled'), DA_HUY_DO_DOI_LICH: cap('Đã hủy do đổi lịch', 'Cancelled (rescheduled)'),
    KHONG_DEN: cap('Không đến', 'No-show'),
});
const DA_HUY = ['BI_TU_CHOI', 'DA_HUY', 'DA_HUY_DO_DOI_LICH'];
// Lịch không còn khám: đã hủy (các kiểu) hoặc đã đóng là không đến (job đóng ngày khám, sang ngày hôm sau); tô cùng màu
const KHONG_CON_KHAM = [...DA_HUY, 'KHONG_DEN'];
// Hàng đợi khám của bác sĩ: buổi của ca (theo giờ bắt đầu) và trạng thái trong hàng đợi (DA_KHAM = đã ghi nhận kết quả khám)
const BUOI = nhan({ SANG: cap('Ca sáng', 'Morning shift'), CHIEU: cap('Ca chiều', 'Afternoon shift'), TOI: cap('Ca tối', 'Evening shift') });
const HANG_DOI = nhan({
    CHO_KHAM: cap('Chờ khám', 'Waiting'), DANG_KHAM: cap('Đang khám', 'In examination'),
    VANG_MAT: cap('Vắng mặt', 'Absent'), DA_KHAM: cap('Đã khám', 'Examined'),
});
// Dùng như chuỗi: the('p', CAN_DOI_LICH) và `${CAN_DOI_LICH}` đều ra chữ của ngôn ngữ đang chọn
const CAN_DOI_LICH = cap('Ca khám của lịch hẹn này đã bị hủy. Hãy đổi sang khung giờ khác (hoặc hủy lịch hẹn).',
    'The shift of this appointment was cancelled. Please move it to another time slot (or cancel the appointment).');
const DA_DOI_BAC_SI = cap('Ca khám đã chuyển sang bác sĩ khác (giờ, phòng không đổi). Bạn được đổi hoặc hủy lịch hẹn tới giờ khám, không tính vào số lần đổi.',
    'The session was handed to another doctor (same time and room). You may reschedule or cancel up to the appointment time; it does not count toward your reschedule limit.');
const LOAI_YEU_CAU = nhan({ XIN_NGHI: cap('Xin nghỉ', 'Leave'), DOI_CA: cap('Đổi ca', 'Shift change'), NHUONG_CA: cap('Nhường ca', 'Hand over') });
const TRANG_THAI_YEU_CAU = nhan({
    CHO_DONG_Y: cap('Chờ đồng nghiệp đồng ý', 'Waiting for the colleague'),
    CHO_DUYET: cap('Chờ duyệt', 'Pending'), DA_DUYET: cap('Đã duyệt', 'Approved'),
    TU_CHOI: cap('Bị từ chối', 'Rejected'), DA_RUT: cap('Đã rút', 'Withdrawn'),
    DONG_NGHIEP_TU_CHOI: cap('Đồng nghiệp không nhận', 'Declined by the colleague'),
});
const GIOI_TINH = nhan({ NAM: cap('Nam', 'Male'), NU: cap('Nữ', 'Female'), KHAC: cap('Khác', 'Other') });
const QUAN_HE = nhan({
    CHA: cap('Cha', 'Father'), ME: cap('Mẹ', 'Mother'),
    NGUOI_GIAM_HO_HOP_PHAP: cap('Người giám hộ hợp pháp', 'Legal guardian'), KHAC: cap('Khác', 'Other'),
});
const NGUOI_DAT = nhan({
    TOI: cap('Bạn đặt', 'Booked by you'), KHACH: cap('Đặt không đăng nhập', 'Booked without signing in'),
    TAI_KHOAN_KHAC: cap('Tài khoản khác đặt', 'Booked by another account'),
});
const LIEN_KET = nhan({
    CHUA_LIEN_KET: cap('Chưa liên kết', 'Not linked'), CHO_XAC_MINH: cap('Chờ phòng khám xác minh', 'Awaiting clinic verification'),
    DA_LIEN_KET: cap('Đã liên kết', 'Linked'),
});
const VAI_TRO = nhan({
    BENH_NHAN: cap('Bệnh nhân', 'Patient'), BAC_SI: cap('Bác sĩ', 'Doctor'), QUAN_TRI_VIEN: cap('Quản trị viên', 'Administrator'),
});
// Đường dùng của thuốc (enum DuongDungThuoc)
const DUONG_DUNG = nhan({
    UONG: cap('Uống', 'Oral'), NGAM_DUOI_LUOI: cap('Ngậm dưới lưỡi', 'Sublingual'), TIEM: cap('Tiêm', 'Injection'),
    BOI_NGOAI_DA: cap('Bôi / dùng ngoài da', 'Topical'), NHO_MAT: cap('Nhỏ mắt', 'Eye drops'), NHO_TAI: cap('Nhỏ tai', 'Ear drops'),
    NHO_MUI: cap('Nhỏ / xịt mũi', 'Nasal'), XIT_HIT: cap('Xịt / hít', 'Inhaled'), DAT: cap('Đặt', 'Rectal / vaginal'),
    KHAC: cap('Khác', 'Other'),
});
// "Paracetamol 500mg (Paracetamol) · Viên nén · Uống · viên" — thuốc trong gợi ý hoặc dòng đơn thuốc; trường null bị bỏ
function moTaThuoc(thuoc) {
    const ten = [thuoc.tenThuoc, thuoc.hamLuong].filter(Boolean).join(' ')
        + (thuoc.hoatChat && thuoc.hoatChat.toLowerCase() !== thuoc.tenThuoc.toLowerCase() ? ` (${thuoc.hoatChat})` : '');
    return [ten, thuoc.dangBaoChe, thuoc.duongDung ? DUONG_DUNG[thuoc.duongDung] : null, thuoc.donVi].filter(Boolean).join(' · ');
}
// Ngôn ngữ khám của bác sĩ: mã ISO 639 ("vi", "en") -> tên theo ngôn ngữ đang xem ("Tiếng Anh" / "English")
function tenNgonNgu(ma) {
    try {
        return new Intl.DisplayNames([ngonNgu], { type: 'language' }).of(ma) || ma;
    } catch {
        return ma;
    }
}
const dsNgonNgu = (cacMa) => (cacMa || []).map(tenNgonNgu).join(', ');
// Chủ nhật đứng đầu, như Date.getDay()
const THU = nhan([cap('CN', 'Sun'), cap('T2', 'Mon'), cap('T3', 'Tue'), cap('T4', 'Wed'), cap('T5', 'Thu'), cap('T6', 'Fri'),
    cap('T7', 'Sat')]);
const MAU = { cuaToi: '#1f6feb', nguoiKhac: '#bf8700', daKham: '#1a7f37', daHuy: '#8c959f', ca: '#57606a', doiChieu: '#cf222e' };
// Trang đầu của từng vai trò sau khi đăng nhập
const TRANG_CHU = { BENH_NHAN: '/appointment', BAC_SI: '/doctor/dashboard', QUAN_TRI_VIEN: '/admin/dashboard' };

let accessToken = null;   // chỉ trong bộ nhớ
let taiKhoan = null;      // tài khoản đang đăng nhập (null = khách)

const $ = (id) => document.getElementById(id);
const vaiTro = () => (accessToken && taiKhoan ? taiKhoan.vaiTro : null);
const laBenhNhan = () => vaiTro() === 'BENH_NHAN';
const trangChu = () => TRANG_CHU[vaiTro()] || '/login';

// ===== Gọi API =====
// API công khai chỉ đọc: không gửi access token (token hết hạn sẽ làm cả API công khai trả 401)
const GET_CONG_KHAI = ['/api/booking/khung-gio', '/api/booking/phieu-kham', '/api/catalog/chuyen-khoa', '/api/catalog/bac-si'];

// Nằm dưới đường dẫn công khai nhưng cần đăng nhập (hồ sơ của bác sĩ đang đăng nhập)
const GET_CAN_TOKEN = ['/api/catalog/bac-si/toi/'];

function coGuiToken(duongDan, phuongThuc) {
    if (!accessToken || duongDan.startsWith('/api/auth/')) return false;
    if (GET_CAN_TOKEN.some((dau) => duongDan.startsWith(dau))) return true;
    return !(phuongThuc === 'GET' && GET_CONG_KHAI.some((dau) => duongDan.startsWith(dau)));
}

// body: object (gửi JSON), FormData (multipart, vd tải ảnh) hoặc undefined.
// imLang = true: không ghi vào ô "Kết quả gọi API gần nhất" (dùng khi trang tự tải lại dữ liệu)
// headerThem: header riêng của lời gọi (vd X-Chat-Khach của khung chat)
let daGoiApi = false;
async function goi(duongDan, body, phuongThuc = 'POST', imLang = false, duocThuLai = true, headerThem = null) {
    const guiToken = coGuiToken(duongDan, phuongThuc);
    const tuyChon = { method: phuongThuc, credentials: 'include', headers: { 'Accept-Language': ngonNgu, ...headerThem } };
    if (guiToken) tuyChon.headers['Authorization'] = 'Bearer ' + accessToken;
    if (body instanceof FormData) {
        // multipart: KHÔNG tự đặt Content-Type, trình duyệt tự thêm boundary
        tuyChon.body = body;
    } else if (body !== undefined) {
        tuyChon.headers['Content-Type'] = 'application/json';
        tuyChon.body = JSON.stringify(body);
    }
    let status = 0, json = null;
    try {
        const res = await fetch(API_URL + duongDan, tuyChon);
        status = res.status;
        json = await res.json().catch(() => null);
    } catch (e) {
        json = { thongDiep: t('Không gọi được API: ', 'Could not call the API: ') + String(e) };
    }
    if (guiToken && status === 401) {
        // Access token hết hạn: làm mới phiên 1 lần bằng cookie rồi gọi lại; không được thì coi như đã đăng xuất
        if (duocThuLai && await lamMoiPhien()) return goi(duongDan, body, phuongThuc, imLang, false, headerThem);
        xoaPhien(t('Phiên đăng nhập đã hết hạn: hãy đăng nhập lại.', 'Your session has expired: please sign in again.'));
    } else if (guiToken && status === 403 && json && json.maLoi === 'TAI_KHOAN_BI_VO_HIEU_HOA') {
        xoaPhien(json.thongDiep);
    }
    if (!imLang) {
        daGoiApi = true;
        $('ket-qua').textContent = `${phuongThuc} ${duongDan} -> ${status}\n` + JSON.stringify(json, null, 2);
    }
    return { status, json };
}
const lay = (duongDan, imLang = false) => goi(duongDan, undefined, 'GET', imLang);

// Tải 1 tệp (Excel, PDF...) bằng GET kèm token / ngôn ngữ như goi, rồi lưu qua 1 thẻ <a download>. Tên tệp lấy từ header
// Content-Disposition, không có thì dùng tenTep. Lỗi (không phải 2xx) trả { status, json } để nơi gọi hiện thongDiep.
async function taiTep(duongDan, tenTep, duocThuLai = true) {
    const guiToken = coGuiToken(duongDan, 'GET');
    const headers = { 'Accept-Language': ngonNgu };
    if (guiToken) headers['Authorization'] = 'Bearer ' + accessToken;
    let res;
    try {
        res = await fetch(API_URL + duongDan, { credentials: 'include', headers });
    } catch (e) {
        return { status: 0, json: { thongDiep: t('Không gọi được API: ', 'Could not call the API: ') + String(e) } };
    }
    if (guiToken && res.status === 401) {
        if (duocThuLai && await lamMoiPhien()) return taiTep(duongDan, tenTep, false);
        xoaPhien(t('Phiên đăng nhập đã hết hạn: hãy đăng nhập lại.', 'Your session has expired: please sign in again.'));
    }
    $('ket-qua').textContent = `GET ${duongDan} -> ${res.status}`;
    if (!res.ok) {
        const json = await res.json().catch(() => null);
        $('ket-qua').textContent += '\n' + JSON.stringify(json, null, 2);
        return { status: res.status, json };
    }
    const tenTrongHeader = /filename="?([^";]+)"?/.exec(res.headers.get('Content-Disposition') || '');
    const url = URL.createObjectURL(await res.blob());
    const a = document.createElement('a');
    a.href = url;
    a.download = tenTrongHeader ? tenTrongHeader[1] : tenTep;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    return { status: res.status, json: null };
}

// Thông điệp lỗi của API: thongDiep + từng trường sai (chiTiet)
function thongDiepLoi(json, macDinh) {
    if (!json) return macDinh;
    const chiTiet = (json.chiTiet || []).map((ct) => `• ${ct.truong}: ${ct.thongDiep}`);
    return [`${json.thongDiep || macDinh}${json.maLoi ? ` (${json.maLoi})` : ''}`, ...chiTiet].join('\n');
}

// ===== Phiên đăng nhập =====
function nhanPhien(json) {
    accessToken = json.duLieu.accessToken;
    taiKhoan = json.duLieu.taiKhoan;
    dongBoNgonNgu();
}

// ===== Ngôn ngữ =====
// Sau khi đăng nhập / làm mới phiên: trình duyệt đã bấm chọn thì lựa chọn đó thắng và được lưu vào tài khoản; chưa chọn
// thì lấy ngôn ngữ đã lưu của tài khoản (không ghi vào localStorage, để tài khoản vẫn là nơi quyết định).
function dongBoNgonNgu() {
    const cuaTaiKhoan = (taiKhoan.ngonNgu || 'VI').toLowerCase();
    if (cuaTaiKhoan === ngonNgu) return;
    if (daChonNgonNgu) {
        luuNgonNguVaoTaiKhoan();
    } else {
        ngonNgu = cuaTaiKhoan;
        document.documentElement.lang = ngonNgu;
    }
}

// Gọi thẳng fetch, không qua goi(): lỗi ở đây không được làm mới phiên rồi quay lại nhanPhien() (vòng lặp)
async function luuNgonNguVaoTaiKhoan() {
    if (!accessToken) return;
    const muonLuu = ngonNgu.toUpperCase();
    try {
        const res = await fetch(API_URL + '/api/users/me/language', {
            method: 'PUT', credentials: 'include',
            headers: { 'Authorization': 'Bearer ' + accessToken, 'Content-Type': 'application/json', 'Accept-Language': ngonNgu },
            body: JSON.stringify({ ngonNgu: muonLuu }),
        });
        if (res.ok && taiKhoan) taiKhoan.ngonNgu = muonLuu;
    } catch { /* không lưu được: lần đăng nhập sau thử lại */ }
}

async function doiNgonNgu(moi) {
    if (moi === ngonNgu && daChonNgonNgu) return;
    ngonNgu = moi;
    daChonNgonNgu = true;
    document.documentElement.lang = ngonNgu;
    try { localStorage.setItem(KHOA_NGON_NGU, ngonNgu); } catch { /* không lưu được thì chỉ giữ trong lần mở trang này */ }
    await luuNgonNguVaoTaiKhoan();
    await veTrang();   // vẽ lại màn hình đang mở: dữ liệu và thông điệp được tải lại theo ngôn ngữ mới
}

// Nội dung danh mục ở API quản trị (/quan-tri/) có cả 2 bản đã lưu: trường gốc là tiếng Việt, trường ...En là tiếng Anh.
// Bảng quản trị hiện bản của ngôn ngữ đang chọn, chưa có bản tiếng Anh thì hiện bản tiếng Việt (như API công khai).
function theoNgonNgu(tiengViet, tiengAnh) {
    return ngonNgu === 'en' && tiengAnh ? tiengAnh : tiengViet;
}

function nutNgonNgu() {
    const khung = the('span', null, 'ngon-ngu');
    for (const [ma, nhan] of [['en', 'EN'], ['vi', 'VI']]) {
        const n = nut(nhan, () => doiNgonNgu(ma), 'nho' + (ma === ngonNgu ? ' dang-chon' : ''));
        // Tên ngôn ngữ luôn viết bằng chính ngôn ngữ đó
        n.title = ma === 'en' ? 'English' : 'Ti\u1EBFng Vi\u1EC7t';
        n.setAttribute('aria-pressed', String(ma === ngonNgu));
        khung.append(n);
    }
    return khung;
}

// Làm mới bằng cookie refresh token: gọi khi mở trang và khi access token hết hạn
async function lamMoiPhien() {
    try {
        const res = await fetch(API_URL + '/api/auth/refresh-token',
            { method: 'POST', credentials: 'include', headers: { 'Accept-Language': ngonNgu } });
        const json = await res.json().catch(() => null);
        if (res.status !== 200 || !json || !json.duLieu) return false;
        nhanPhien(json);
        return true;
    } catch {
        return false;
    }
}

// Chữ viết sẵn trong index.html (ngoài các màn hình): đặt lại mỗi lần vẽ khung để theo ngôn ngữ đang chọn
function veChuTinh() {
    $('tieu-de-ket-qua').textContent = t('Kết quả gọi API gần nhất', 'Last API result');
    if (!daGoiApi) $('ket-qua').textContent = t('(chưa gọi)', '(no call yet)');
    $('ghi-chu-trang').textContent = t(
        'Chỉ dùng khi dev (thay frontend). Refresh token nằm trong cookie HttpOnly eclinic_rt; access token chỉ giữ trong bộ nhớ trang. Dùng Chrome/Edge/Firefox.',
        'For development only (stands in for the frontend). The refresh token is in the HttpOnly cookie eclinic_rt; the access token is kept in page memory only. Use Chrome/Edge/Firefox.');
}

// Phiên mất giữa chừng: báo lý do rồi vẽ lại (màn hình cần đăng nhập sẽ chuyển về /login)
function xoaPhien(thongBao) {
    accessToken = null;
    taiKhoan = null;
    baoChung(thongBao);
    setTimeout(veTrang, 0);
}

// Sau POST /api/auth/login hoặc /api/auth/google thành công: về trang đang định mở (?tiep=) hoặc trang đầu của vai trò
function dangNhapXong(json) {
    nhanPhien(json);
    const tiep = new URLSearchParams(location.search).get('tiep') || '';
    const hopLe = tiep.startsWith('/') && !tiep.startsWith('//') && !tiep.startsWith('/login');
    return diToi(hopLe ? tiep : trangChu());
}

async function dangXuat() {
    await goi('/api/auth/logout');
    accessToken = null;
    taiKhoan = null;
    await diToi('/login');
    baoChung(t('Đã đăng xuất.', 'Signed out.'));
}

// ===== Dựng DOM =====
function the(ten, noiDung, lop) {
    const el = document.createElement(ten);
    if (noiDung !== undefined && noiDung !== null) el.textContent = noiDung;
    if (lop) el.className = lop;
    return el;
}
// Chèn khung HTML viết sẵn trong mã nguồn (KHÔNG ghép dữ liệu từ API vào chuỗi này)
function khuon(khung, html) {
    khung.insertAdjacentHTML('beforeend', html);
}
const ghiChu = (noiDung) => the('p', noiDung, 'ghi-chu');
// Ghi thông báo vào 1 phần tử (id hoặc chính phần tử); lop: 'loi', 'tot' hoặc bỏ trống
function bao(dich, noiDung, lop) {
    const el = typeof dich === 'string' ? $(dich) : dich;
    if (!el) return;
    el.textContent = noiDung;
    el.className = 'ghi-chu' + (lop ? ' ' + lop : '');
}
// Thông báo trên đầu trang (hết phiên, đã đăng xuất); xoá khi chuyển màn hình
function baoChung(noiDung) {
    $('thong-bao-chung').textContent = noiDung || '';
    $('thong-bao-chung').classList.toggle('an', !noiDung);
}
// Danh sách "nhãn: giá trị"; bỏ qua dòng không có giá trị (API trả null cho trường trống)
function thongTin(cacDong) {
    const dl = the('dl', null, 'thong-tin');
    for (const [nhan, giaTri] of cacDong) {
        if (giaTri === null || giaTri === undefined || giaTri === '') continue;
        dl.append(the('dt', nhan), the('dd', String(giaTri)));
    }
    return dl;
}
function bang(tieuDe, cacDong) {
    const b = the('table');
    const dau = the('tr');
    tieuDe.forEach((cot) => dau.append(the('th', cot)));
    b.append(dau, ...cacDong);
    return b;
}
// 1 dòng bảng từ danh sách ô: chuỗi / số (đưa vào bằng textContent) hoặc phần tử DOM
function dong(cacO, lop) {
    const tr = the('tr', null, lop);
    for (const o of cacO) {
        const td = the('td');
        if (o instanceof Node) td.append(o);
        else if (Array.isArray(o)) td.append(...o);
        else if (o !== null && o !== undefined) td.textContent = o;
        tr.append(td);
    }
    return tr;
}
function nut(nhan, khiBam, lop = 'nho') {
    const n = the('button', nhan, lop);
    n.type = 'button';
    n.addEventListener('click', khiBam);
    return n;
}
function lienKet(nhan, href, tabMoi) {
    const a = the('a', nhan);
    a.href = href;
    if (tabMoi) a.target = '_blank';
    return a;
}
// Ảnh từ API: chỉ nhận URL http(s)
function anh(url, moTa, lop) {
    const el = document.createElement('img');
    if (lop) el.className = lop;
    el.alt = moTa || '';
    if (/^https?:\/\//.test(url || '')) el.src = url;
    return el;
}

// ===== Form =====
const giaTriForm = (form) => Object.fromEntries(new FormData(form).entries());
// Form -> body: bỏ trường trống để API hiểu là "không có"
function thanForm(form, cacTruong) {
    const than = {};
    for (const ten of cacTruong) {
        const o = form.elements[ten];
        if (!o.disabled && o.value.trim()) than[ten] = o.value.trim();
    }
    return than;
}
function khiGui(id, xuLy) {
    $(id).addEventListener('submit', async (e) => {
        e.preventDefault();
        await xuLy(giaTriForm(e.target), e.target);
    });
}

// ===== Ngày giờ: API trả 'YYYY-MM-DD', 'YYYY-MM-DDTHH:mm:ss', 'HH:mm:ss' theo giờ Việt Nam; trang chỉ cắt chuỗi =====
// Ngày viết dd/MM/yyyy và giờ 24h ở cả 2 ngôn ngữ (như email của hệ thống); chỉ tên thứ (THU) đổi theo ngôn ngữ
const gio = (ngayGio) => ngayGio.slice(11, 16);
const ngayVn = (ngay) => `${ngay.slice(8, 10)}/${ngay.slice(5, 7)}/${ngay.slice(0, 4)}`;
const ngayGioVn = (ngayGio) => `${gio(ngayGio)} ${ngayVn(ngayGio)}`;
function ngayIso(d) {
    const hai = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${hai(d.getMonth() + 1)}-${hai(d.getDate())}`;
}
function congNgay(soNgay) {
    const d = new Date();
    return ngayIso(new Date(d.getFullYear(), d.getMonth(), d.getDate() + soNgay));
}
function thuCuaNgay(ngay) {
    const [nam, thang, ngayTrongThang] = ngay.split('-').map(Number);
    return THU[new Date(nam, thang - 1, ngayTrongThang).getDay()];
}
function tuoiVaoNgay(ngaySinh, ngay) {
    const [namSinh, thangSinh, ngaySinhNhat] = ngaySinh.split('-').map(Number);
    const [nam, thang, ngayTrongThang] = ngay.split('-').map(Number);
    return nam - namSinh - (thang < thangSinh || (thang === thangSinh && ngayTrongThang < ngaySinhNhat) ? 1 : 0);
}
// Khoảng ngày lịch đang hiện: FullCalendar đưa [start, end), API nhận [tuNgay, denNgay]
function khoangNgay(info) {
    const cuoi = new Date(info.end.getTime());
    cuoi.setDate(cuoi.getDate() - 1);
    return `tuNgay=${ngayIso(info.start)}&denNgay=${ngayIso(cuoi)}`;
}

const tenBacSi = (bs) => `${bs.hocVi || ''} ${bs.hoTen}`.trim();
const tenPhong = (pk) => (pk ? `${pk.tenPhong}${pk.tang ? ` (${pk.tang})` : ''}` : null);

// ===== Thư viện ngoài (CDN, cần internet): chỉ màn hình nào dùng mới tải =====
const thuVienDangTai = new Map();
function napThuVien(url) {
    if (!thuVienDangTai.has(url)) {
        thuVienDangTai.set(url, new Promise((xong, hong) => {
            const s = document.createElement('script');
            s.src = url;
            s.onload = xong;
            s.onerror = () => {
                thuVienDangTai.delete(url);
                hong(new Error(t('Không tải được thư viện từ CDN: ', 'Could not load the library from the CDN: ') + url));
            };
            document.head.append(s);
        }));
    }
    return thuVienDangTai.get(url);
}

// Lịch vẽ bằng FullCalendar 6 (bản global: core + interaction + daygrid + timegrid + list). Trả null nếu không tải được
async function taoLich(el, nguon, tuyChon) {
    try {
        await napThuVien('https://cdn.jsdelivr.net/npm/fullcalendar@6.1.21/index.global.min.js');
        await napThuVien('https://cdn.jsdelivr.net/npm/@fullcalendar/core@6.1.21/locales/vi.global.min.js');
    } catch {
        el.replaceChildren(the('p', t('Không tải được thư viện lịch (FullCalendar) từ CDN: kiểm tra kết nối internet rồi tải lại trang.',
            'Could not load the calendar library (FullCalendar) from the CDN: check the internet connection and reload the page.'), 'ghi-chu loi'));
        return null;
    }
    // Trong lúc chờ thư viện người dùng đã sang màn hình khác
    if (!el.isConnected) return null;
    const lich = new FullCalendar.Calendar(el, {
        locale: ngonNgu,
        initialView: 'dayGridMonth',
        headerToolbar: { left: 'prev,next today', center: 'title', right: 'dayGridMonth,timeGridWeek,timeGridDay,listWeek' },
        height: 'auto',
        nowIndicator: true,
        dayMaxEvents: 4,
        slotMinTime: '06:00:00',
        slotMaxTime: '21:00:00',
        // Lượt khám không có giờ kết thúc trong API: vẽ mỗi lịch hẹn dài 15 phút ở lưới giờ
        defaultTimedEventDuration: '00:15',
        eventTimeFormat: { hour: '2-digit', minute: '2-digit', hour12: false },
        eventSources: nguon,
        ...tuyChon,
    });
    lich.render();
    return lich;
}

// ===== Khối dùng ở nhiều màn hình =====
// Sinh hiệu trên 1 dòng: "Mạch 80/phút · Nhiệt độ 37.5 °C · HA 120/80 mmHg · …"; null nếu chưa đo chỉ số nào
function sinhHieuChu(sh) {
    if (!sh) return null;
    const co = (giaTri) => giaTri !== null && giaTri !== undefined;
    return [co(sh.mach) ? t(`Mạch ${sh.mach}/phút`, `Pulse ${sh.mach}/min`) : null,
        co(sh.nhietDo) ? t(`Nhiệt độ ${sh.nhietDo} °C`, `Temperature ${sh.nhietDo} °C`) : null,
        co(sh.huyetApTamThu) ? t(`HA ${sh.huyetApTamThu}/${sh.huyetApTamTruong} mmHg`, `BP ${sh.huyetApTamThu}/${sh.huyetApTamTruong} mmHg`) : null,
        co(sh.nhipTho) ? t(`Nhịp thở ${sh.nhipTho}/phút`, `Breathing ${sh.nhipTho}/min`) : null,
        co(sh.canNang) ? t(`Cân nặng ${sh.canNang} kg`, `Weight ${sh.canNang} kg`) : null,
        co(sh.chieuCao) ? t(`Chiều cao ${sh.chieuCao} cm`, `Height ${sh.chieuCao} cm`) : null].filter(Boolean).join(' · ') || null;
}
// "J06.9 — Nhiễm trùng đường hô hấp trên cấp… (mạn tính)"
const icdChu = (icd) => icd ? `${icd.ma} — ${icd.ten}` + (icd.manTinh ? t(' (mạn tính)', ' (chronic)') : '') : null;
// Kết quả khám: lý do khám, sinh hiệu, khám lâm sàng, chẩn đoán, lời dặn, đơn thuốc (màn hình bệnh nhân và bác sĩ)
function ketQuaKham(ketQua) {
    const khung = the('div');
    if (ketQua.ngaySuaCuoi) khung.append(ghiChu(t(`Đã chỉnh sửa lúc ${ngayGioVn(ketQua.ngaySuaCuoi)}`, `Edited on ${ngayGioVn(ketQua.ngaySuaCuoi)}`)));
    khung.append(thongTin([[t('Lý do khám', 'Reason for visit'), ketQua.lyDoKham], [t('Sinh hiệu', 'Vital signs'), sinhHieuChu(ketQua.sinhHieu)],
        [t('Khám lâm sàng', 'Examination findings'), ketQua.khamLamSang],
        [t('Chẩn đoán', 'Diagnosis'), ketQua.chanDoan], [t('Mã ICD-10', 'ICD-10 code'), icdChu(ketQua.icdChinh)],
        [t('Chẩn đoán kèm theo', 'Secondary diagnoses'), ketQua.chanDoanKemTheo], [t('Lời dặn', 'Doctor\'s advice'), ketQua.ghiChu],
        [t('Ngày tái khám đề xuất', 'Suggested follow-up date'), ketQua.ngayTaiKhamDeXuat ? ngayVn(ketQua.ngayTaiKhamDeXuat) : null]]));
    if (ketQua.donThuoc && ketQua.donThuoc.length) {
        khung.append(bang([t('Thuốc', 'Medicine'), t('Liều dùng', 'Dosage'), t('Lần/ngày', 'Times/day'), t('Số ngày', 'Days'),
            t('Số lượng', 'Quantity'), t('Ghi chú', 'Note')], ketQua.donThuoc.map((thuoc) => dong([
            moTaThuoc(thuoc), thuoc.lieuDung, thuoc.soLanMoiNgay, thuoc.soNgayDung, thuoc.soLuong,
            thuoc.ghiChuSuDung]))));
    } else {
        khung.append(ghiChu(t('Không kê đơn thuốc.', 'No prescription.')));
    }
    return khung;
}

// Lịch sử sửa hồ sơ bệnh án (bác sĩ: GET /api/medical/bac-si/toi/lich-hen/{id}/benh-an/lich-su; quản trị viên:
// GET /api/medical/quan-tri/lich-hen/{id}/benh-an/lich-su). Mỗi lần sửa: ai, lúc nào, lý do, và chỉ phần đã đổi (trước -> sau).
const TRUONG_BENH_AN = nhan({
    LY_DO_KHAM: cap('Lý do khám', 'Reason for visit'), SINH_HIEU: cap('Sinh hiệu', 'Vital signs'),
    KHAM_LAM_SANG: cap('Khám lâm sàng', 'Examination findings'), CHAN_DOAN_KEM_THEO: cap('Chẩn đoán kèm theo', 'Secondary diagnoses'),
    CHAN_DOAN: cap('Chẩn đoán (kèm mã ICD-10)', 'Diagnosis (with ICD-10 code)'), GHI_CHU: cap('Lời dặn', 'Doctor\'s advice'),
    NGAY_TAI_KHAM: cap('Ngày tái khám đề xuất', 'Suggested follow-up date'), DON_THUOC: cap('Đơn thuốc', 'Prescription'),
});
function noiDungTruong(noiDung, truong) {
    // Lịch sử ghi trước khi có các trường mới không có các trường đó: hiện "—"
    if (truong === 'LY_DO_KHAM') return noiDung.lyDoKham || '—';
    if (truong === 'SINH_HIEU') return sinhHieuChu(noiDung.sinhHieu) || '—';
    if (truong === 'KHAM_LAM_SANG') return noiDung.khamLamSang || '—';
    if (truong === 'CHAN_DOAN_KEM_THEO') return noiDung.chanDoanKemTheo || '—';
    if (truong === 'CHAN_DOAN') return (noiDung.chanDoan || '—') + (noiDung.maIcdChinh ? ` [${noiDung.maIcdChinh}]` : '');
    if (truong === 'GHI_CHU') return noiDung.ghiChu || '—';
    if (truong === 'NGAY_TAI_KHAM') return noiDung.ngayTaiKhamDeXuat ? ngayVn(noiDung.ngayTaiKhamDeXuat) : '—';
    if (!noiDung.donThuoc || !noiDung.donThuoc.length) return ghiChu(t('Không kê đơn thuốc.', 'No prescription.'));
    return bang([t('Thuốc', 'Medicine'), t('Liều dùng', 'Dosage'), t('Lần/ngày', 'Times/day'), t('Số ngày', 'Days'), t('Số lượng', 'Quantity'), t('Ghi chú', 'Note')],
        noiDung.donThuoc.map((d) => dong([[d.tenThuoc, d.hamLuong].filter(Boolean).join(' '), d.lieuDung, d.soLanMoiNgay, d.soNgayDung, d.soLuong, d.ghiChuSuDung])));
}
function lichSuSuaBenhAn(duLieu) {
    const khung = the('div', null, 'lich-su-sua');
    khung.append(ghiChu(t(`Ghi nhận lúc ${ngayGioVn(duLieu.ngayGhiNhan)} · phiên bản hiện tại ${duLieu.phienBanHienTai}`,
        `Recorded on ${ngayGioVn(duLieu.ngayGhiNhan)} · current version ${duLieu.phienBanHienTai}`)));
    if (!duLieu.cacLanSua.length) {
        khung.append(ghiChu(t('Hồ sơ bệnh án chưa được sửa lần nào.', 'The medical record has never been edited.')));
        return khung;
    }
    for (const lan of duLieu.cacLanSua) {
        const khoi = the('div', null, 'lan-kham');
        khoi.append(the('b', t(`Phiên bản ${lan.phienBan} · ${ngayGioVn(lan.ngaySua)} · BS ${lan.bacSi.hoTen}`,
            `Version ${lan.phienBan} · ${ngayGioVn(lan.ngaySua)} · Dr. ${lan.bacSi.hoTen}`)),
            the('p', t('Lý do: ', 'Reason: ') + lan.lyDo, 'ghi-chu'),
            bang([t('Phần đã đổi', 'Changed'), t('Trước', 'Before'), t('Sau', 'After')],
                lan.cacTruongDoi.map((truong) => dong([TRUONG_BENH_AN[truong] || truong, noiDungTruong(lan.truoc, truong), noiDungTruong(lan.sau, truong)]))));
        khung.append(khoi);
    }
    return khung;
}
// Mở / đóng lịch sử sửa ngay dưới nút bấm: tải lại mỗi lần mở
async function batTatLichSuSua(nutBam, duongDan) {
    const daMo = nutBam.nextElementSibling && nutBam.nextElementSibling.classList.contains('lich-su-sua');
    if (daMo) {
        nutBam.nextElementSibling.remove();
        return;
    }
    const { status, json } = await lay(duongDan);
    nutBam.after(status === 200 ? lichSuSuaBenhAn(json.duLieu)
        : the('p', thongDiepLoi(json, t('Không tải được lịch sử sửa.', 'Could not load the edit history.')), 'ghi-chu loi lich-su-sua'));
}

// Hủy / đổi lịch hẹn (màn hình /patient/appointments và /phieu-kham/<mã>). UI chưa có màn hình cho việc này.
//   lich:            cần duocHuyDoi, hanHuyDoi, bacSi.id (dòng "lịch hẹn của tôi" hoặc phiếu khám)
//   canSoDienThoai:  true ở trang phiếu khám (không đăng nhập): phải nhập đúng SĐT đã dùng khi đặt lịch
//   gui(hanhDong, body): gọi API 'huy' hoặc 'doi-lich', trả { status, json }
//   khiXong(hanhDong, duLieu): 'huy' -> phiếu khám sau khi hủy; 'doi-lich' -> lịch mới (như kết quả đặt lịch)
// Khung giờ để đổi lấy từ GET /api/booking/khung-gio?ngay=&idBacSi= (cùng bác sĩ); API đổi lịch cũng nhận ca của bác sĩ
// khác hoặc idChuyenKhoa (bác sĩ bất kỳ) như API đặt lịch.
function khoiHuyDoi({ lich, canSoDienThoai, gui, khiXong }) {
    const khung = the('div', null, 'huy-doi');
    khung.append(the('h3', t('Hủy / đổi lịch', 'Cancel / reschedule')));
    // canDoiLich: ca khám đã bị hủy; hủy / đổi được tới giờ khám cũ và không tính vào số lần đổi lịch
    if (lich.canDoiLich) khung.append(the('p', CAN_DOI_LICH, 'canh-bao'));
    // daDoiBacSi: ca khám được nhường cho bác sĩ khác; hủy / đổi tự do tới giờ khám
    if (lich.daDoiBacSi) khung.append(the('p', DA_DOI_BAC_SI, 'canh-bao'));
    if (!lich.duocHuyDoi) {
        khung.append(ghiChu(lich.trangThai === 'DA_XAC_NHAN'
            ? t(`Không hủy / đổi được trên hệ thống (hạn chót: ${ngayGioVn(lich.hanHuyDoi)}, hoặc tài khoản này không phải người đặt / người khám). Vui lòng liên hệ phòng khám.`,
                `This appointment cannot be cancelled or rescheduled online (deadline: ${ngayGioVn(lich.hanHuyDoi)}, or this account is neither the booker nor the patient). Please contact the clinic.`)
            : t('Lịch hẹn này không còn hủy / đổi được.', 'This appointment can no longer be cancelled or rescheduled.')));
        return khung;
    }
    khung.append(ghiChu(t(`Hủy / đổi được đến ${ngayGioVn(lich.hanHuyDoi)}. Đổi lịch = hủy lịch này và tạo lịch mới (xác nhận ngay, phiếu khám mới).`,
        `Can be cancelled or rescheduled until ${ngayGioVn(lich.hanHuyDoi)}. Rescheduling cancels this appointment and creates a new one (confirmed at once, new ticket).`)));
    const trangThai = ghiChu('');
    const oSdt = the('input');
    if (canSoDienThoai) {
        oSdt.placeholder = t('Số điện thoại đã dùng khi đặt lịch (bắt buộc)', 'Phone number used for the booking (required)');
        oSdt.inputMode = 'numeric';
        oSdt.maxLength = 10;
        khung.append(oSdt);
    }
    const kem = (body) => (canSoDienThoai ? { ...body, soDienThoai: oSdt.value.trim() } : body);
    async function thucHien(hanhDong, body) {
        const { status, json } = await gui(hanhDong, kem(body));
        if (status !== 200 && status !== 201) {
            bao(trangThai, thongDiepLoi(json, t('Không thực hiện được.', 'The action failed.')), 'loi');
            return;
        }
        khiXong(hanhDong, json.duLieu);
    }

    const oLyDo = the('input');
    oLyDo.placeholder = t('Lý do hủy (không bắt buộc)', 'Reason for cancelling (optional)');
    oLyDo.maxLength = 500;
    khung.append(oLyDo, nut(t('Hủy lịch hẹn', 'Cancel appointment'), () => {
        if (confirm(t('Hủy lịch hẹn này?', 'Cancel this appointment?'))) thucHien('huy', { lyDo: oLyDo.value.trim() || null });
    }, 'nguy-hiem nho'));

    const oNgay = the('input');
    oNgay.type = 'date';
    oNgay.min = congNgay(0);
    const dsKhung = the('div');
    khung.append(the('label', t('Đổi sang ngày khác / khung giờ khác (cùng bác sĩ)', 'Move to another day / time slot (same doctor)')), oNgay, dsKhung, trangThai);
    oNgay.addEventListener('change', async () => {
        dsKhung.replaceChildren();
        if (!oNgay.value) return;
        const { status, json } = await lay(`/api/booking/khung-gio?ngay=${oNgay.value}&idBacSi=${lich.bacSi.id}`, true);
        if (status !== 200) {
            bao(trangThai, thongDiepLoi(json, t('Không tải được khung giờ.', 'Could not load the time slots.')), 'loi');
            return;
        }
        const cacNut = [];
        for (const ca of json.duLieu) {
            for (const k of ca.khungGio) {
                const n = nut(`${gio(k.gioBatDau)}–${gio(k.gioKetThuc)} (${t(`còn ${k.soChoConLai}`, `${k.soChoConLai} left`)})`, () => {
                    if (confirm(t(`Đổi lịch sang ${gio(k.gioBatDau)} ${ngayVn(k.gioBatDau)}?`,
                        `Reschedule to ${gio(k.gioBatDau)} ${ngayVn(k.gioBatDau)}?`))) {
                        thucHien('doi-lich', { idLichLamViec: ca.idLichLamViec, gioBatDauKhung: k.gioBatDau });
                    }
                }, 'phu nho');
                n.disabled = k.hetCho;
                cacNut.push(n);
            }
        }
        bao(trangThai, cacNut.length ? t('Chọn 1 khung giờ để đổi.', 'Choose a time slot to move to.')
            : t('Bác sĩ không có ca nhận đặt lịch trong ngày này.', 'The doctor has no bookable shift on this day.'));
        dsKhung.replaceChildren(...cacNut);
    });
    return khung;
}

// Phiếu khám + mã QR (màn hình /phieu-kham/<mã> và phiếu vừa đặt ở /appointment)
const taiPhieu = (ma, imLang) => lay('/api/booking/phieu-kham/' + encodeURIComponent(ma), imLang);
function hienPhieu(khung, phieu) {
    const duongDanQr = `${API_URL}/api/booking/phieu-kham/${encodeURIComponent(phieu.maPhieuKham)}/qr`;
    const dl = document.createElement('dl');
    const them = (nhan, giaTri, lop) => dl.append(the('dt', nhan), the('dd', giaTri, lop));
    const bn = phieu.benhNhan;
    them(t('Mã tra cứu', 'Lookup code'), phieu.maTraCuu);
    them(t('Số thứ tự', 'Queue number'), phieu.soThuTu, 'so-thu-tu');
    them(t('Trạng thái', 'Status'), TRANG_THAI[phieu.trangThai] || phieu.trangThai);
    if (phieu.canDoiLich) them(t('Cần đổi lịch', 'Needs rescheduling'), CAN_DOI_LICH);
    if (phieu.daDoiBacSi) them(t('Đã đổi bác sĩ', 'Doctor changed'), DA_DOI_BAC_SI);
    if (phieu.lyDoHuy) them(t('Lý do hủy', 'Cancellation reason'), phieu.lyDoHuy);
    them(t('Ngày khám', 'Visit date'), `${thuCuaNgay(phieu.ngay)} ${ngayVn(phieu.ngay)}`);
    them(t('Giờ khám dự kiến', 'Expected time'), gio(phieu.gioKhamDuKien));
    them(t('Khung giờ', 'Time slot'), `${gio(phieu.gioBatDauKhung)} – ${gio(phieu.gioKetThucKhung)}`);
    them(t('Bác sĩ', 'Doctor'), `${tenBacSi(phieu.bacSi)} · ${phieu.tenChuyenKhoa}`);
    them(t('Phòng khám', 'Room'), tenPhong(phieu.phongKham));
    them(t('Bệnh nhân', 'Patient'), `${bn.hoTen} · ${t('sinh năm', 'born')} ${bn.namSinh ?? '?'}${bn.gioiTinh ? ` · ${GIOI_TINH[bn.gioiTinh]}` : ''}`);
    if (bn.cccd) them(t('CCCD', 'National ID'), bn.cccd);
    if (bn.soDienThoai) them(t('Số điện thoại', 'Phone number'), bn.soDienThoai);
    if (phieu.nguoiGiamHo) {
        const gh = phieu.nguoiGiamHo;
        them(t('Người giám hộ', 'Guardian'), `${gh.hoTen} (${QUAN_HE[gh.quanHe] || gh.quanHe}) · ${gh.soDienThoai}`);
    }
    if (phieu.canNguoiGiamHoDiCung) them(t('Lưu ý', 'Note'), t('Người giám hộ phải đi cùng bệnh nhân khi đến khám.', 'The guardian must come with the patient to the visit.'));
    if (phieu.lyDoKham) them(t('Lý do khám', 'Reason for visit'), phieu.lyDoKham);
    them(t('Đặt lúc', 'Booked at'), ngayGioVn(phieu.ngayDat));
    them(t('Mã phiếu khám', 'Ticket code'), phieu.maPhieuKham);
    them(t('Link phiếu khám', 'Ticket link'), phieu.linkPhieuKham);

    const cotQr = document.createElement('div');
    const anhQr = document.createElement('img');
    anhQr.alt = t('Mã QR của link phiếu khám', 'QR code of the ticket link');
    anhQr.src = duongDanQr;
    cotQr.append(anhQr, the('br'), lienKet(t('Tải mã QR', 'Download QR code'), duongDanQr + '?taiVe=true'), the('br'),
        lienKet(t('Mở trang phiếu khám', 'Open the ticket page'), '/phieu-kham/' + encodeURIComponent(phieu.maPhieuKham), true));

    const hop = the('div', undefined, 'phieu');
    hop.append(dl, cotQr);
    khung.replaceChildren(hop);
}

// ===== Bộ định tuyến =====
// Mỗi file man-hinh/*.js gọi dangKy({ duongDan, vaiTro, tieuDe, ve }):
//   duongDan  chuỗi, mảng chuỗi hoặc RegExp (kết quả khớp đưa vào tham số khop)
//   vaiTro    bỏ trống = công khai; 'DANG_NHAP' = vai trò nào cũng được; hoặc 'BENH_NHAN' / 'BAC_SI' / 'QUAN_TRI_VIEN'
//   tieuDe    tên màn hình (tên trong danh sách UI): chuỗi, hoặc cap(vi, en) khi tên có 2 bản
//   ve(khung, { thamSo, khop, duongDan })  vẽ màn hình vào khung; có thể trả về hàm dọn dẹp (tắt camera, hủy lịch, hẹn giờ)
// Đường dẫn mới cũng phải thêm vào CAC_DUONG_DAN trong server.js.
const cacManHinh = [];
function dangKy(manHinh) {
    cacManHinh.push(manHinh);
}

function timManHinh(duongDan) {
    for (const mh of cacManHinh) {
        if (mh.duongDan instanceof RegExp) {
            const khop = duongDan.match(mh.duongDan);
            if (khop) return { mh, khop };
        } else if ([].concat(mh.duongDan).includes(duongDan)) {
            return { mh, khop: null };
        }
    }
    return null;
}

let donDep = null;   // hàm dọn dẹp của màn hình đang hiện
let luotVe = 0;      // tăng mỗi lần vẽ: màn hình vẽ chậm biết mình đã bị thay

async function veTrang() {
    const luot = ++luotVe;
    if (donDep) {
        try { donDep(); } catch { /* dọn dẹp lỗi không được chặn việc chuyển màn hình */ }
        donDep = null;
    }
    if (location.pathname === '/') history.replaceState(null, '', trangChu());
    const duongDan = location.pathname;
    const tim = timManHinh(duongDan);
    const khung = $('man-hinh');
    if (tim && tim.mh.vaiTro && !vaiTro()) {
        // Chưa đăng nhập: về /login, đăng nhập xong quay lại đúng màn hình này
        history.replaceState(null, '', '/login?tiep=' + encodeURIComponent(duongDan + location.search));
        return veTrang();
    }
    veKhung(duongDan);
    if (!tim) {
        document.title = 'eClinic demo';
        khung.replaceChildren(the('h1', t('Không có màn hình này', 'No such screen')), lienKet(t('Về trang đầu', 'Back to the start page'), '/'));
        return;
    }
    const { mh, khop } = tim;
    const tieuDe = chuCua(mh.tieuDe);
    document.title = `${tieuDe} — eClinic demo`;
    if (mh.vaiTro && mh.vaiTro !== 'DANG_NHAP' && mh.vaiTro !== vaiTro()) {
        khung.replaceChildren(the('h1', tieuDe),
            the('p', t(`Sai vai trò: màn hình này dành cho ${VAI_TRO[mh.vaiTro]}, bạn đang đăng nhập bằng tài khoản ${VAI_TRO[vaiTro()]}.`,
                `Wrong role: this screen is for the ${VAI_TRO[mh.vaiTro]} role, and you are signed in as ${VAI_TRO[vaiTro()]}.`), 'canh-bao'),
            lienKet(t('Về trang của tôi', 'Back to my page'), trangChu()));
        return;
    }
    khung.replaceChildren(the('h1', tieuDe));
    const ketQua = await mh.ve(khung, { thamSo: new URLSearchParams(location.search), khop, duongDan });
    if (typeof ketQua === 'function') {
        if (luot === luotVe) donDep = ketQua;
        else ketQua();
    }
}

function diToi(duongDan) {
    history.pushState(null, '', duongDan);
    baoChung('');
    window.scrollTo(0, 0);
    return veTrang();
}

// ===== Khung trang: thanh trên, thanh bên theo vai trò, menu tài khoản (theo đúng thứ tự trong UI) =====
// Bản tiếng Anh là tên màn hình trong danh sách UI
const THANH_BEN = {
    BAC_SI: [
        [cap('Lịch làm việc', 'Work Schedule'), '/doctor/work-schedule'],
        [cap('Lịch hẹn sắp tới', 'Upcoming Appointments'), '/doctor/upcoming-appointments'],
        [cap('Khám bệnh / bệnh án', 'Consultation / EMR'), '/doctor/consultation'],
        [cap('Hồ sơ bác sĩ', 'Doctor Profile'), '/doctor/profile'],
    ],
    QUAN_TRI_VIEN: [
        [cap('Tổng quan', 'Dashboard'), '/admin/dashboard'],
        [cap('Quản lý lịch làm việc', 'Schedule Management'), '/admin/schedule-management'],
        [cap('Danh mục y tế', 'Medical Catalog'), '/admin/medical-catalog'],
        [cap('Quản lý bác sĩ', 'Doctor Management'), '/admin/doctors'],
        [cap('Quản lý bệnh nhân', 'Patient Management'), '/admin/patients'],
        [null, cap('Ngoài danh sách UI', 'Not in the UI list')],
        [cap('Tài khoản', 'Accounts'), '/admin/accounts'],
    ],
};
const NAV_CONG_KHAI = [
    [cap('Đặt lịch khám', 'Book Appointment'), '/appointment'],
    [cap('Lịch hẹn của tôi', 'My Appointments'), '/patient/appointments'],
    [cap('Hồ sơ bệnh nhân của tôi', 'My Patient Profile'), '/patient/profile'],
];

function khuVuc(duongDan) {
    if (duongDan.startsWith('/doctor/')) return 'BAC_SI';
    if (duongDan.startsWith('/admin/')) return 'QUAN_TRI_VIEN';
    if (duongDan === '/account' && THANH_BEN[vaiTro()]) return vaiTro();
    return 'CONG_KHAI';
}

function mucDieuHuong([tenMuc, href], duongDan) {
    if (tenMuc === null) return the('div', chuCua(href), 'nhom');
    const a = lienKet(chuCua(tenMuc), href);
    if (href === duongDan) a.className = 'dang-xem';
    return a;
}

function veKhung(duongDan) {
    veChuTinh();
    veChat();
    const khu = khuVuc(duongDan);
    const thanhBen = $('thanh-ben');
    if (khu === 'CONG_KHAI') {
        $('nav-tren').replaceChildren(...NAV_CONG_KHAI.map((muc) => mucDieuHuong(muc, duongDan)));
        thanhBen.classList.add('an');
    } else {
        // Thanh trên của bác sĩ / quản trị viên: nút về Dashboard (biểu tượng ngôi nhà trong UI)
        $('nav-tren').replaceChildren(mucDieuHuong([t('Tổng quan', 'Dashboard'), TRANG_CHU[khu]], duongDan),
            the('span', khu === 'BAC_SI' ? t('Khu vực bác sĩ', 'Doctor workspace') : t('Khu vực quản trị', 'Admin workspace'), 'ghi-chu'));
        thanhBen.replaceChildren(...THANH_BEN[khu].map((muc) => mucDieuHuong(muc, duongDan)));
        thanhBen.classList.remove('an');
    }

    const khuTaiKhoan = $('khu-tai-khoan');
    if (!vaiTro()) {
        khuTaiKhoan.replaceChildren(nutNgonNgu(), lienKet(t('Đăng nhập', 'Login'), '/login'), lienKet(t('Đăng ký', 'Register'), '/register'));
        return;
    }
    const menu = the('details');
    const noiDung = the('div', null, 'menu-tai-khoan');
    noiDung.append(the('b', taiKhoan.hoTen), the('div', `${VAI_TRO[taiKhoan.vaiTro]} · ${taiKhoan.email}`, 'ghi-chu'),
        lienKet(t('Tài khoản của tôi / đổi mật khẩu', 'My account / change password'), '/account'),
        lienKet(t('Mở trang làm việc', 'Open workspace'), trangChu()), nut(t('Đăng xuất', 'Log out'), dangXuat, 'phu'));
    menu.append(the('summary', taiKhoan.hoTen), noiDung);
    // Chuông của UI chỉ vẽ ở khu bác sĩ; trang mẫu hiện cho mọi vai trò vì bệnh nhân cũng nhận thông báo (bác sĩ hủy, ca bị hủy, nhắc lịch)
    khuTaiKhoan.replaceChildren(nutNgonNgu(), veChuong(), menu);
    capNhatChuong();
}

// ===== Chuông thông báo =====
//   GET  /api/notification/thong-bao?chuaDoc=&trang=&kichThuoc=   thông báo của tôi, mới nhất trước (TrangDuLieu)
//        mỗi dòng: { id, loai, noiDung, daDoc, ngayTao, idLichHen, maPhieuKham } — maPhieuKham chỉ có khi tôi là bệnh nhân
//        noiDung theo Accept-Language của request (thông báo tạo trước V18 chỉ có câu tiếng Việt)
//   GET  /api/notification/thong-bao/so-chua-doc                  { soChuaDoc }: số trên chuông
//   POST /api/notification/thong-bao/{id}/da-doc                  đánh dấu 1 thông báo đã đọc (gọi lại vẫn được)
//   POST /api/notification/thong-bao/da-doc-tat-ca                { soDaDanhDau }
// Không có đẩy realtime: hỏi lại số chưa đọc mỗi 30 giây, bỏ qua khi tab đang ẩn, hỏi ngay khi tab hiện lại.
// loai: bác sĩ nhận LICH_HEN_MOI / LICH_HEN_DA_HUY / LICH_HEN_DA_DOI / CA_LAM_VIEC_THAY_DOI / KET_QUA_DUYET_DOI_LICH;
//       bệnh nhân nhận LICH_HEN_DA_XAC_NHAN / LICH_HEN_BI_TU_CHOI / LICH_HEN_CAN_DOI / LICH_HEN_DOI_PHONG /
//       NHAC_LICH_KHAM (nhắc trước giờ khám, có maPhieuKham) / NHAC_TAI_KHAM (bác sĩ hẹn, đổi ngày tái khám và lời
//       nhắc trước ngày đó; idLichHen / maPhieuKham là của lượt khám ĐÃ XONG, chưa có lịch hẹn mới: mở trang đặt lịch);
//       quản trị viên nhận YEU_CAU_DOI_LICH_MOI. Thông báo về yêu cầu đổi ca / xin nghỉ có idYeuCau.
const CHU_KY_CHUONG = 30000;
let soChuaDoc = 0;

// Màn hình mở khi bấm 1 thông báo
function dichCuaThongBao(tb) {
    if (vaiTro() === 'BAC_SI') return tb.loai === 'LICH_HEN_MOI' || tb.loai === 'LICH_HEN_DA_DOI' ? '/doctor/upcoming-appointments' : '/doctor/work-schedule';
    // Quản trị viên: YEU_CAU_DOI_LICH_MOI (tb.idYeuCau) -> bảng yêu cầu ở Schedule Management
    if (vaiTro() === 'QUAN_TRI_VIEN') return '/admin/schedule-management';
    if (vaiTro() === 'BENH_NHAN' && tb.loai === 'NHAC_TAI_KHAM') return '/appointment';
    if (vaiTro() === 'BENH_NHAN') return '/patient/appointments' + (tb.maPhieuKham ? '?ma=' + encodeURIComponent(tb.maPhieuKham) : '');
    return null;
}

function veSoChuong() {
    const nhan = $('chuong-so');
    if (nhan) nhan.textContent = soChuaDoc > 0 ? `🔔 ${soChuaDoc > 99 ? '99+' : soChuaDoc}` : '🔔';
}

function veChuong() {
    const chuong = the('details', null, 'chuong');
    const nhan = the('summary');
    nhan.id = 'chuong-so';
    nhan.title = t('Thông báo', 'Notifications');
    const danhSach = the('div', null, 'menu-tai-khoan ds-thong-bao');
    chuong.append(nhan, danhSach);
    chuong.addEventListener('toggle', () => { if (chuong.open) taiThongBao(danhSach); });
    setTimeout(veSoChuong, 0);
    return chuong;
}

async function taiThongBao(khung) {
    khung.replaceChildren(ghiChu(t('Đang tải...', 'Loading...')));
    const { status, json } = await lay('/api/notification/thong-bao?kichThuoc=10', true);
    if (status !== 200) {
        khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được thông báo.', 'Could not load notifications.')), 'ghi-chu loi'));
        return;
    }
    const cacDong = json.duLieu.noiDung.map((tb) => {
        const dong = the('div', null, tb.daDoc ? 'thong-bao' : 'thong-bao chua-doc');
        dong.append(the('div', tb.noiDung), the('div', ngayGioVn(tb.ngayTao), 'ghi-chu'));
        dong.addEventListener('click', async () => {
            if (!tb.daDoc) await goi(`/api/notification/thong-bao/${tb.id}/da-doc`, undefined, 'POST', true);
            const dich = dichCuaThongBao(tb);
            if (dich) await diToi(dich);
            else { await capNhatChuong(); taiThongBao(khung); }
        });
        return dong;
    });
    const dau = the('div', null, 'dau-thong-bao');
    dau.append(the('b', `${t('Thông báo', 'Notifications')} (${json.duLieu.tongSoPhanTu})`), nut(t('Đánh dấu tất cả đã đọc', 'Mark all as read'), async () => {
        await goi('/api/notification/thong-bao/da-doc-tat-ca');
        await capNhatChuong();
        taiThongBao(khung);
    }, 'phu nho'));
    khung.replaceChildren(dau, ...(cacDong.length ? cacDong : [ghiChu(t('Chưa có thông báo nào.', 'No notifications yet.'))]));
}

async function capNhatChuong() {
    if (!vaiTro() || document.hidden) return;
    const { status, json } = await lay('/api/notification/thong-bao/so-chua-doc', true);
    if (status === 200) {
        soChuaDoc = json.duLieu.soChuaDoc;
        veSoChuong();
    }
}
setInterval(capNhatChuong, CHU_KY_CHUONG);
document.addEventListener('visibilitychange', capNhatChuong);

// ===== Khung chat với chatbot (UC-CHAT-01): nút ở góc dưới phải, có ở mọi màn hình và mọi vai trò =====
//   POST   /api/chatbot/phien                       mở phiên (201): { idPhien, maKhach?, tinNhan: [câu chào] }
//          Chưa đăng nhập: maKhach chỉ trả 1 lần, gửi lại ở header X-Chat-Khach trong mọi request sau của phiên.
//          Đã đăng nhập: phiên thuộc về tài khoản (JWT), không có maKhach.
//   POST   /api/chatbot/phien/{id}/tin-nhan { noiDung }   -> { tinNhan, traLoi }; 429 gửi quá nhiều, 503 chatbot không trả lời
//          được (không lưu gì, gửi lại được): hiện thongDiep của server
//   GET    /api/chatbot/phien/{id}/tin-nhan         đọc lại hội thoại (sau khi tải lại trang)
//   POST | DELETE /api/chatbot/phien/{id}/dong-y    bệnh nhân đồng ý / rút đồng ý cho chatbot đọc lịch hẹn của chính mình
//   GET    /api/chatbot/phien                       phiên của tôi: lấy daDongYDuLieu của phiên đang mở
// traLoi.loai: TRA_LOI, NGOAI_PHAM_VI, KHAN_CAP (tô đỏ), CAN_DONG_Y (hiện nút Đồng ý; bấm thì gọi dong-y rồi GỬI LẠI câu
// hỏi vừa rồi). traLoi.goiY (server dựng, không phải chữ của mô hình): chuyenKhoa, bacSi[], lichKham[] -> thẻ mở
// /appointment?idChuyenKhoa=&idBacSi=&ngay=&idLichLamViec=&gioBatDauKhung= (dat-lich.js chọn sẵn); lichHenCuaToi[] -> phiếu khám; lanKham[] -> lịch hẹn của tôi.
// Phiên chat gắn với người mở nó: đăng nhập, đăng xuất hoặc đổi tài khoản thì mở phiên MỚI (phiên của khách không bao giờ
// tra cứu được lịch hẹn cá nhân). idPhien + maKhach giữ ở sessionStorage để tải lại trang vẫn còn hội thoại.
const KHOA_CHAT = 'eclinic-demo-chat';
let phienChat = null;      // { idPhien, maKhach, chu, tinNhan: [], daDongY }
let chatDangMo = false;
let chatDangGui = false;
let loiChat = '';

const chuChat = () => (vaiTro() ? String(taiKhoan.id) : 'khach');
const goiChat = (duoi, body, phuongThuc) => goi('/api/chatbot/phien' + duoi, body, phuongThuc, false, true,
    phienChat && phienChat.maKhach ? { 'X-Chat-Khach': phienChat.maKhach } : null);

function luuPhienChat() {
    try {
        if (phienChat) sessionStorage.setItem(KHOA_CHAT, JSON.stringify({ idPhien: phienChat.idPhien, maKhach: phienChat.maKhach, chu: phienChat.chu }));
        else sessionStorage.removeItem(KHOA_CHAT);
    } catch { /* sessionStorage bị chặn: phiên chỉ sống tới khi tải lại trang */ }
}
function docPhienChat() {
    try { return JSON.parse(sessionStorage.getItem(KHOA_CHAT)); } catch { return null; }
}

// Có phiên của đúng người đang dùng trang: dùng lại phiên đã lưu (đọc lại hội thoại) hoặc mở phiên mới
async function damBaoPhienChat() {
    const chu = chuChat();
    if (phienChat && phienChat.chu === chu) return true;
    phienChat = null;
    const daLuu = docPhienChat();
    if (daLuu && daLuu.chu === chu && daLuu.idPhien) {
        phienChat = { idPhien: daLuu.idPhien, maKhach: daLuu.maKhach || null, chu, tinNhan: [], daDongY: false };
        const { status, json } = await goiChat(`/${daLuu.idPhien}/tin-nhan`, undefined, 'GET');
        if (status === 200 && phienChat) {
            phienChat.tinNhan = json.duLieu;
            if (laBenhNhan()) {
                const cuaToi = await lay('/api/chatbot/phien', true);
                const dong = cuaToi.status === 200 ? cuaToi.json.duLieu.find((p) => p.idPhien === daLuu.idPhien) : null;
                if (phienChat) phienChat.daDongY = !!(dong && dong.daDongYDuLieu);
            }
            return !!phienChat;
        }
        phienChat = null;
    }
    const { status, json } = await goi('/api/chatbot/phien');
    if (status !== 201) {
        loiChat = thongDiepLoi(json, t('Không mở được phiên chat.', 'Could not open a chat session.'));
        return false;
    }
    phienChat = { idPhien: json.duLieu.idPhien, maKhach: json.duLieu.maKhach || null, chu, tinNhan: json.duLieu.tinNhan || [], daDongY: false };
    luuPhienChat();
    return true;
}

// Trả true khi tin đã được gửi và có câu trả lời (ô nhập được xoá); lỗi thì giữ nguyên chữ đang gõ để gửi lại
async function guiChat(noiDung) {
    if (chatDangGui || !noiDung) return false;
    chatDangGui = true;
    loiChat = '';
    veNoiDungChat();
    let xong = false;
    if (await damBaoPhienChat()) {
        const { status, json } = await goiChat(`/${phienChat.idPhien}/tin-nhan`, { noiDung });
        if (status === 200 && phienChat) {
            phienChat.tinNhan.push(json.duLieu.tinNhan, json.duLieu.traLoi);
            xong = true;
        } else {
            // 429 (gửi quá nhiều), 503 (chatbot không trả lời được), 400 (tin quá dài): câu của server
            loiChat = thongDiepLoi(json, t('Không gửi được tin nhắn.', 'Could not send the message.'));
            if (status === 404) {
                // Phiên không còn (hoặc không phải của người này): lần gửi sau mở phiên mới
                phienChat = null;
                luuPhienChat();
            }
        }
    }
    chatDangGui = false;
    veNoiDungChat();
    return xong;
}

// Câu hỏi đang chờ đồng ý: tin của người dùng đứng ngay trước câu trả lời CAN_DONG_Y cuối cùng
function cauHoiChoDongY() {
    const ds = phienChat ? phienChat.tinNhan : [];
    if (!ds.length || ds[ds.length - 1].loai !== 'CAN_DONG_Y') return null;
    for (let i = ds.length - 2; i >= 0; i--) {
        if (ds[i].nguoiGui === 'NGUOI_DUNG') return ds[i].noiDung;
    }
    return null;
}

async function datDongYChat(dongY) {
    if (!phienChat || chatDangGui) return;
    const cauHoi = cauHoiChoDongY();
    const { status, json } = await goiChat(`/${phienChat.idPhien}/dong-y`, undefined, dongY ? 'POST' : 'DELETE');
    if (!phienChat) return;
    if (status !== 200) {
        loiChat = thongDiepLoi(json, t('Không thực hiện được.', 'The action failed.'));
        veNoiDungChat();
        return;
    }
    phienChat.daDongY = dongY;
    loiChat = '';
    if (dongY && cauHoi) await guiChat(cauHoi);
    else veNoiDungChat();
}

function duongDanDatLich(thamSo) {
    const q = new URLSearchParams();
    for (const [ten, giaTri] of Object.entries(thamSo)) {
        if (giaTri !== null && giaTri !== undefined && giaTri !== '') q.set(ten, giaTri);
    }
    return '/appointment?' + q;
}

function theChat(...cacPhan) {
    const khoi = the('div', null, 'the-chat');
    khoi.append(...cacPhan.filter(Boolean));
    return khoi;
}

// Thẻ gợi ý dưới 1 câu trả lời. Tên bác sĩ, chuyên khoa, phòng là dữ liệu của API: chỉ ghép vào, không dịch
function theGoiY(goiY) {
    const cacThe = [];
    const idChuyenKhoa = goiY.chuyenKhoa ? goiY.chuyenKhoa.id : null;
    const gioCua = (giaTri) => (giaTri.length > 8 ? gio(giaTri) : giaTri.slice(0, 5));
    const conCho = (so) => t(`còn ${so}`, `${so} left`);
    if (goiY.chuyenKhoa) {
        cacThe.push(theChat(the('b', goiY.chuyenKhoa.ten),
            lienKet(t('Đặt lịch chuyên khoa này', 'Book in this specialty'), duongDanDatLich({ idChuyenKhoa }))));
    }
    for (const bs of goiY.bacSi || []) {
        cacThe.push(theChat(the('b', tenBacSi(bs)), bs.chucVu ? the('div', bs.chucVu, 'ghi-chu') : null,
            the('div', bs.ngaySomNhat
                ? `${t('Ngày sớm nhất còn chỗ', 'Earliest day with a place')}: ${thuCuaNgay(bs.ngaySomNhat)} ${ngayVn(bs.ngaySomNhat)} (${conCho(bs.soChoConLai)})`
                : t('Chưa có ngày nào còn chỗ.', 'No day with a free place yet.'), 'ghi-chu'),
            lienKet(t('Đặt lịch với bác sĩ này', 'Book with this doctor'), duongDanDatLich({ idChuyenKhoa, idBacSi: bs.id }))));
    }
    for (const lk of goiY.lichKham || []) {
        const phong = lk.tenPhong ? ` · ${lk.tenPhong}${lk.tang ? ` (${lk.tang})` : ''}` : '';
        const cacKhung = the('div', null, 'khung-gio-chat');
        for (const kg of lk.khungGio || []) {
            cacKhung.append(lienKet(`${gioCua(kg.gioBatDau)}–${gioCua(kg.gioKetThuc)} (${conCho(kg.soChoConLai)})`, duongDanDatLich({
                idChuyenKhoa, idBacSi: lk.idBacSi, ngay: lk.ngay, idLichLamViec: lk.idLichLamViec, gioBatDauKhung: kg.gioBatDau })));
        }
        cacThe.push(theChat(the('b', `${lk.hoTenBacSi} · ${thuCuaNgay(lk.ngay)} ${ngayVn(lk.ngay)}${phong}`),
            the('div', t('Bấm 1 khung giờ để mở form đặt lịch đã chọn sẵn:', 'Pick a time slot to open the booking form with it preselected:'), 'ghi-chu'),
            cacKhung));
    }
    for (const lich of goiY.lichHenCuaToi || []) {
        const phong = lich.tenPhong ? ` · ${lich.tenPhong}${lich.tang ? ` (${lich.tang})` : ''}` : '';
        cacThe.push(theChat(
            the('b', `${thuCuaNgay(lich.ngay)} ${ngayVn(lich.ngay)} ${gioCua(lich.gioBatDauKhung)}–${gioCua(lich.gioKetThucKhung)}`),
            the('div', `${lich.hoTenBacSi} · ${lich.tenChuyenKhoa}${phong}`),
            the('div', `${TRANG_THAI[lich.trangThai] || lich.trangThai} · ${t('Mã tra cứu', 'Lookup code')}: ${lich.maTraCuu}`
                + (lich.laBanThan ? '' : ` · ${t('Người khám', 'Patient')}: ${lich.hoTenBenhNhan}`), 'ghi-chu'),
            lich.canDoiLich ? the('div', CAN_DOI_LICH, 'ghi-chu loi') : null,
            lienKet(t('Mở phiếu khám', 'Open the ticket'), '/phieu-kham/' + encodeURIComponent(lich.maPhieuKham))));
    }
    for (const lan of goiY.lanKham || []) {
        cacThe.push(theChat(
            the('b', `${t('Đã khám', 'Visited')} ${thuCuaNgay(lan.ngay)} ${ngayVn(lan.ngay)}`),
            the('div', `${lan.hoTenBacSi} · ${lan.tenChuyenKhoa}` + (lan.laBanThan ? '' : ` · ${t('Người khám', 'Patient')}: ${lan.hoTenBenhNhan}`)),
            the('div', lan.ngayTaiKhamDeXuat
                ? `${t('Bác sĩ hẹn tái khám', 'Follow-up suggested by the doctor')}: ${ngayVn(lan.ngayTaiKhamDeXuat)}`
                : t('Bác sĩ không hẹn tái khám.', 'No follow-up was suggested.'), 'ghi-chu'),
            lienKet(t('Xem kết quả ở Lịch hẹn của tôi', 'See the result in My Appointments'),
                '/patient/appointments?ma=' + encodeURIComponent(lan.maPhieuKham))));
    }
    return cacThe;
}

function veNoiDungChat() {
    const khung = $('chat-tin-nhan');
    if (!khung) return;
    const ds = phienChat ? phienChat.tinNhan : [];
    const cacTin = ds.map((tn, chiSo) => {
        const khoi = the('div', null, 'tin-chat ' + (tn.nguoiGui === 'NGUOI_DUNG' ? 'cua-toi' : 'cua-bot')
            + (tn.loai === 'KHAN_CAP' ? ' khan-cap' : ''));
        khoi.append(the('div', tn.noiDung, 'noi-dung'));
        if (tn.goiY) khoi.append(...theGoiY(tn.goiY));
        if (tn.loai === 'CAN_DONG_Y' && chiSo === ds.length - 1 && !phienChat.daDongY) {
            khoi.append(nut(t('Đồng ý và hỏi lại', 'Agree and ask again'), () => datDongYChat(true)));
        }
        return khoi;
    });
    if (chatDangGui) cacTin.push(the('div', t('Đang trả lời...', 'Answering...'), 'ghi-chu'));
    khung.replaceChildren(...cacTin);
    khung.scrollTop = khung.scrollHeight;
    $('chat-loi').textContent = loiChat;
    $('chat-gui').disabled = chatDangGui;
    const chan = [the('span', t('Trợ lý AI chỉ hỗ trợ đặt lịch và thông tin phòng khám, không thay lời khuyên của bác sĩ. Cấp cứu: gọi 115.',
        'The AI assistant only helps with booking and clinic information; it does not replace a doctor\'s advice. Emergency: call 115.'))];
    if (phienChat && phienChat.daDongY) {
        chan.push(the('br'), the('span', t('Bạn đã cho chatbot đọc lịch hẹn của mình trong phiên này. ', 'You let the chatbot read your appointments in this session. ')),
            nut(t('Rút lại đồng ý', 'Withdraw consent'), () => datDongYChat(false), 'phu nho'));
    }
    $('chat-chan').replaceChildren(...chan);
}

async function moChat(mo) {
    chatDangMo = mo;
    $('khung-chat').classList.toggle('an', !mo);
    $('nut-chat').setAttribute('aria-expanded', String(mo));
    if (!mo) return;
    if (!phienChat || phienChat.chu !== chuChat()) {
        loiChat = '';
        await damBaoPhienChat();
    }
    veNoiDungChat();
    $('chat-o').focus();
}

// Gọi mỗi lần vẽ khung trang: dựng nút + khung chat (1 lần), đặt lại chữ theo ngôn ngữ, bỏ phiên của tài khoản cũ
function veChat() {
    const doiChu = !!phienChat && phienChat.chu !== chuChat();
    if (doiChu) {
        phienChat = null;
        loiChat = '';
    }
    if (!$('khung-chat')) {
        const khung = the('div', null, 'khung-chat an');
        khung.id = 'khung-chat';
        const dau = the('div', null, 'dau-chat');
        const tieuDe = the('b');
        tieuDe.id = 'chat-tieu-de';
        const dong = nut('×', () => moChat(false), 'phu nho');
        dong.id = 'chat-dong';
        dau.append(tieuDe, dong);
        const tinNhan = the('div', null, 'chat-tin-nhan');
        tinNhan.id = 'chat-tin-nhan';
        const loi = the('p', null, 'ghi-chu loi');
        loi.id = 'chat-loi';
        const form = the('form');
        const o = the('textarea');
        o.id = 'chat-o';
        o.rows = 2;
        o.maxLength = 1000;
        const gui = the('button');
        gui.id = 'chat-gui';
        form.append(o, gui);
        const guiDi = async () => {
            if (await guiChat(o.value.trim())) o.value = '';
        };
        form.addEventListener('submit', (e) => { e.preventDefault(); guiDi(); });
        o.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); guiDi(); }
        });
        const chan = the('div', null, 'ghi-chu');
        chan.id = 'chat-chan';
        khung.append(dau, tinNhan, loi, form, chan);
        const nutMo = nut('', () => moChat(!chatDangMo), 'nut-chat');
        nutMo.id = 'nut-chat';
        nutMo.setAttribute('aria-expanded', 'false');
        document.body.append(khung, nutMo);
    }
    $('nut-chat').textContent = t('💬 Hỏi trợ lý', '💬 Ask the assistant');
    $('chat-tieu-de').textContent = t('Trợ lý eClinic', 'eClinic assistant');
    $('chat-dong').title = t('Đóng', 'Close');
    $('chat-o').placeholder = t('Nhập câu hỏi (Enter để gửi, Shift+Enter xuống dòng)', 'Type your question (Enter to send, Shift+Enter for a new line)');
    $('chat-gui').textContent = t('Gửi', 'Send');
    // Khung đang mở mà tài khoản vừa đổi: mở luôn phiên mới của tài khoản này
    if (doiChu && chatDangMo) moChat(true);
    else veNoiDungChat();
}

// Bấm liên kết nội bộ: đổi màn hình không tải lại trang (liên kết mở tab mới và liên kết ra ngoài thì để trình duyệt xử lý)
document.addEventListener('click', (e) => {
    const a = e.target.closest ? e.target.closest('a[href]') : null;
    if (!a || a.target || e.defaultPrevented || e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey) return;
    const href = a.getAttribute('href');
    if (!href.startsWith('/') || href.startsWith('//')) return;
    e.preventDefault();
    diToi(href);
});
window.addEventListener('popstate', () => {
    baoChung('');
    veTrang();
});

// Gọi 1 lần ở cuối index.html, sau khi mọi màn hình đã đăng ký: lấy lại phiên bằng cookie rồi mới vẽ màn hình đầu tiên
async function khoiDong() {
    await lamMoiPhien();
    await veTrang();
}
