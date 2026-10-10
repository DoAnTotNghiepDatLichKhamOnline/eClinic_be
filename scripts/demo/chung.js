// Phần dùng chung của trang demo: gọi API, phiên đăng nhập, hàm dựng DOM, ngày giờ, bộ định tuyến và khung trang.
// Các file man-hinh/*.js dùng thẳng các hàm / hằng khai báo ở đây (script thường, không phải module).
//
// Quy ước:
// - Dữ liệu từ API luôn đưa vào trang bằng textContent (hàm the()), không dùng innerHTML. Hàm khuon() chỉ nhận chuỗi HTML
//   viết sẵn trong mã nguồn (khung form, tiêu đề), không bao giờ ghép dữ liệu từ API vào.
// - Access token chỉ nằm trong biến accessToken, không lưu localStorage; refresh token nằm trong cookie HttpOnly.

const API_URL = window.CAU_HINH.apiUrl;
const GOOGLE_CLIENT_ID = window.CAU_HINH.googleClientId;

const TRANG_THAI = {
    CHO_XAC_NHAN: 'Chờ xác nhận', DA_XAC_NHAN: 'Đã xác nhận', BI_TU_CHOI: 'Bị từ chối',
    DA_HOAN_THANH: 'Đã hoàn thành', DA_HUY: 'Đã hủy', DA_HUY_DO_DOI_LICH: 'Đã hủy do đổi lịch',
};
const DA_HUY = ['BI_TU_CHOI', 'DA_HUY', 'DA_HUY_DO_DOI_LICH'];
const CAN_DOI_LICH = 'Ca khám của lịch hẹn này đã bị hủy. Hãy đổi sang khung giờ khác (hoặc hủy lịch hẹn).';
const LOAI_YEU_CAU = { XIN_NGHI: 'Xin nghỉ', DOI_CA: 'Đổi ca' };
const TRANG_THAI_YEU_CAU = { CHO_DUYET: 'Chờ duyệt', DA_DUYET: 'Đã duyệt', TU_CHOI: 'Bị từ chối', DA_RUT: 'Đã rút' };
const GIOI_TINH = { NAM: 'Nam', NU: 'Nữ', KHAC: 'Khác' };
const QUAN_HE = { CHA: 'Cha', ME: 'Mẹ', NGUOI_GIAM_HO_HOP_PHAP: 'Người giám hộ hợp pháp', KHAC: 'Khác' };
const NGUOI_DAT = { TOI: 'Bạn đặt', KHACH: 'Đặt không đăng nhập', TAI_KHOAN_KHAC: 'Tài khoản khác đặt' };
const LIEN_KET = { CHUA_LIEN_KET: 'Chưa liên kết', CHO_XAC_MINH: 'Chờ phòng khám xác minh', DA_LIEN_KET: 'Đã liên kết' };
const VAI_TRO = { BENH_NHAN: 'Bệnh nhân', BAC_SI: 'Bác sĩ', QUAN_TRI_VIEN: 'Quản trị viên' };
const THU = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
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

function coGuiToken(duongDan, phuongThuc) {
    if (!accessToken || duongDan.startsWith('/api/auth/')) return false;
    return !(phuongThuc === 'GET' && GET_CONG_KHAI.some((dau) => duongDan.startsWith(dau)));
}

// body: object (gửi JSON), FormData (multipart, vd tải ảnh) hoặc undefined.
// imLang = true: không ghi vào ô "Kết quả gọi API gần nhất" (dùng khi trang tự tải lại dữ liệu)
async function goi(duongDan, body, phuongThuc = 'POST', imLang = false, duocThuLai = true) {
    const guiToken = coGuiToken(duongDan, phuongThuc);
    const tuyChon = { method: phuongThuc, credentials: 'include', headers: {} };
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
        json = { thongDiep: 'Không gọi được API: ' + String(e) };
    }
    if (guiToken && status === 401) {
        // Access token hết hạn: làm mới phiên 1 lần bằng cookie rồi gọi lại; không được thì coi như đã đăng xuất
        if (duocThuLai && await lamMoiPhien()) return goi(duongDan, body, phuongThuc, imLang, false);
        xoaPhien('Phiên đăng nhập đã hết hạn: hãy đăng nhập lại.');
    } else if (guiToken && status === 403 && json && json.maLoi === 'TAI_KHOAN_BI_VO_HIEU_HOA') {
        xoaPhien(json.thongDiep);
    }
    if (!imLang) {
        $('ket-qua').textContent = `${phuongThuc} ${duongDan} -> ${status}\n` + JSON.stringify(json, null, 2);
    }
    return { status, json };
}
const lay = (duongDan, imLang = false) => goi(duongDan, undefined, 'GET', imLang);

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
}

// Làm mới bằng cookie refresh token: gọi khi mở trang và khi access token hết hạn
async function lamMoiPhien() {
    try {
        const res = await fetch(API_URL + '/api/auth/refresh-token', { method: 'POST', credentials: 'include' });
        const json = await res.json().catch(() => null);
        if (res.status !== 200 || !json || !json.duLieu) return false;
        nhanPhien(json);
        return true;
    } catch {
        return false;
    }
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
    baoChung('Đã đăng xuất.');
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
    tieuDe.forEach((t) => dau.append(the('th', t)));
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
                hong(new Error('Không tải được thư viện từ CDN: ' + url));
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
        el.replaceChildren(the('p', 'Không tải được thư viện lịch (FullCalendar) từ CDN: kiểm tra kết nối internet rồi tải lại trang.', 'ghi-chu loi'));
        return null;
    }
    // Trong lúc chờ thư viện người dùng đã sang màn hình khác
    if (!el.isConnected) return null;
    const lich = new FullCalendar.Calendar(el, {
        locale: 'vi',
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
// Kết quả khám: chẩn đoán, lời dặn, đơn thuốc (màn hình bệnh nhân và bác sĩ)
function ketQuaKham(ketQua) {
    const khung = the('div');
    khung.append(thongTin([['Chẩn đoán', ketQua.chanDoan], ['Lời dặn', ketQua.ghiChu],
        ['Ngày tái khám đề xuất', ketQua.ngayTaiKhamDeXuat ? ngayVn(ketQua.ngayTaiKhamDeXuat) : null]]));
    if (ketQua.donThuoc && ketQua.donThuoc.length) {
        khung.append(bang(['Thuốc', 'Liều dùng', 'Lần/ngày', 'Số ngày', 'Ghi chú'], ketQua.donThuoc.map((t) => dong([
            t.tenThuoc + (t.donVi ? ` (${t.donVi})` : ''), t.lieuDung, t.soLanMoiNgay, t.soNgayDung, t.ghiChuSuDung]))));
    } else {
        khung.append(ghiChu('Không kê đơn thuốc.'));
    }
    return khung;
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
    khung.append(the('h3', 'Hủy / đổi lịch'));
    // canDoiLich: ca khám đã bị hủy; hủy / đổi được tới giờ khám cũ và không tính vào số lần đổi lịch
    if (lich.canDoiLich) khung.append(the('p', CAN_DOI_LICH, 'canh-bao'));
    if (!lich.duocHuyDoi) {
        khung.append(ghiChu(['CHO_XAC_NHAN', 'DA_XAC_NHAN'].includes(lich.trangThai)
            ? `Không hủy / đổi được trên hệ thống (hạn chót: ${ngayGioVn(lich.hanHuyDoi)}, hoặc tài khoản này không phải người đặt / người khám). Vui lòng liên hệ phòng khám.`
            : 'Lịch hẹn này không còn hủy / đổi được.'));
        return khung;
    }
    khung.append(ghiChu(`Hủy / đổi được đến ${ngayGioVn(lich.hanHuyDoi)}. Đổi lịch = hủy lịch này và tạo lịch mới (chờ xác nhận, phiếu khám mới).`));
    const trangThai = ghiChu('');
    const oSdt = the('input');
    if (canSoDienThoai) {
        oSdt.placeholder = 'Số điện thoại đã dùng khi đặt lịch (bắt buộc)';
        oSdt.inputMode = 'numeric';
        oSdt.maxLength = 10;
        khung.append(oSdt);
    }
    const kem = (body) => (canSoDienThoai ? { ...body, soDienThoai: oSdt.value.trim() } : body);
    async function thucHien(hanhDong, body) {
        const { status, json } = await gui(hanhDong, kem(body));
        if (status !== 200 && status !== 201) {
            bao(trangThai, thongDiepLoi(json, 'Không thực hiện được.'), 'loi');
            return;
        }
        khiXong(hanhDong, json.duLieu);
    }

    const oLyDo = the('input');
    oLyDo.placeholder = 'Lý do hủy (không bắt buộc)';
    oLyDo.maxLength = 500;
    khung.append(oLyDo, nut('Hủy lịch hẹn', () => {
        if (confirm('Hủy lịch hẹn này?')) thucHien('huy', { lyDo: oLyDo.value.trim() || null });
    }, 'nguy-hiem nho'));

    const oNgay = the('input');
    oNgay.type = 'date';
    oNgay.min = congNgay(0);
    const dsKhung = the('div');
    khung.append(the('label', 'Đổi sang ngày khác / khung giờ khác (cùng bác sĩ)'), oNgay, dsKhung, trangThai);
    oNgay.addEventListener('change', async () => {
        dsKhung.replaceChildren();
        if (!oNgay.value) return;
        const { status, json } = await lay(`/api/booking/khung-gio?ngay=${oNgay.value}&idBacSi=${lich.bacSi.id}`, true);
        if (status !== 200) {
            bao(trangThai, thongDiepLoi(json, 'Không tải được khung giờ.'), 'loi');
            return;
        }
        const cacNut = [];
        for (const ca of json.duLieu) {
            for (const k of ca.khungGio) {
                const n = nut(`${gio(k.gioBatDau)}–${gio(k.gioKetThuc)} (còn ${k.soChoConLai})`, () => {
                    if (confirm(`Đổi lịch sang ${gio(k.gioBatDau)} ${ngayVn(k.gioBatDau)}?`)) {
                        thucHien('doi-lich', { idLichLamViec: ca.idLichLamViec, gioBatDauKhung: k.gioBatDau });
                    }
                }, 'phu nho');
                n.disabled = k.hetCho;
                cacNut.push(n);
            }
        }
        bao(trangThai, cacNut.length ? 'Chọn 1 khung giờ để đổi.' : 'Bác sĩ không có ca nhận đặt lịch trong ngày này.');
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
    them('Mã tra cứu', phieu.maTraCuu);
    them('Số thứ tự', phieu.soThuTu, 'so-thu-tu');
    them('Trạng thái', TRANG_THAI[phieu.trangThai] || phieu.trangThai);
    if (phieu.canDoiLich) them('Cần đổi lịch', CAN_DOI_LICH);
    if (phieu.lyDoHuy) them('Lý do từ chối / hủy', phieu.lyDoHuy);
    them('Ngày khám', `${thuCuaNgay(phieu.ngay)} ${ngayVn(phieu.ngay)}`);
    them('Giờ khám dự kiến', gio(phieu.gioKhamDuKien));
    them('Khung giờ', `${gio(phieu.gioBatDauKhung)} – ${gio(phieu.gioKetThucKhung)}`);
    them('Bác sĩ', `${tenBacSi(phieu.bacSi)} · ${phieu.tenChuyenKhoa}`);
    them('Phòng khám', tenPhong(phieu.phongKham));
    them('Bệnh nhân', `${bn.hoTen} · sinh năm ${bn.namSinh ?? '?'}${bn.gioiTinh ? ` · ${GIOI_TINH[bn.gioiTinh]}` : ''}`);
    if (bn.cccd) them('CCCD', bn.cccd);
    if (bn.soDienThoai) them('Số điện thoại', bn.soDienThoai);
    if (phieu.nguoiGiamHo) {
        const gh = phieu.nguoiGiamHo;
        them('Người giám hộ', `${gh.hoTen} (${QUAN_HE[gh.quanHe] || gh.quanHe}) · ${gh.soDienThoai}`);
    }
    if (phieu.canNguoiGiamHoDiCung) them('Lưu ý', 'Người giám hộ phải đi cùng bệnh nhân khi đến khám.');
    if (phieu.lyDoKham) them('Lý do khám', phieu.lyDoKham);
    them('Đặt lúc', ngayGioVn(phieu.ngayDat));
    them('Mã phiếu khám', phieu.maPhieuKham);
    them('Link phiếu khám', phieu.linkPhieuKham);

    const cotQr = document.createElement('div');
    const anhQr = document.createElement('img');
    anhQr.alt = 'Mã QR của link phiếu khám';
    anhQr.src = duongDanQr;
    cotQr.append(anhQr, the('br'), lienKet('Tải mã QR', duongDanQr + '?taiVe=true'), the('br'),
        lienKet('Mở trang phiếu khám', '/phieu-kham/' + encodeURIComponent(phieu.maPhieuKham), true));

    const hop = the('div', undefined, 'phieu');
    hop.append(dl, cotQr);
    khung.replaceChildren(hop);
}

// ===== Bộ định tuyến =====
// Mỗi file man-hinh/*.js gọi dangKy({ duongDan, vaiTro, tieuDe, ve }):
//   duongDan  chuỗi, mảng chuỗi hoặc RegExp (kết quả khớp đưa vào tham số khop)
//   vaiTro    bỏ trống = công khai; 'DANG_NHAP' = vai trò nào cũng được; hoặc 'BENH_NHAN' / 'BAC_SI' / 'QUAN_TRI_VIEN'
//   tieuDe    tên màn hình (tên trong danh sách UI)
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
        khung.replaceChildren(the('h1', 'Không có màn hình này'), lienKet('Về trang đầu', '/'));
        return;
    }
    const { mh, khop } = tim;
    document.title = `${mh.tieuDe} — eClinic demo`;
    if (mh.vaiTro && mh.vaiTro !== 'DANG_NHAP' && mh.vaiTro !== vaiTro()) {
        khung.replaceChildren(the('h1', mh.tieuDe),
            the('p', `Sai vai trò: màn hình này dành cho ${VAI_TRO[mh.vaiTro]}, bạn đang đăng nhập bằng tài khoản ${VAI_TRO[vaiTro()]}.`, 'canh-bao'),
            lienKet('Về trang của tôi', trangChu()));
        return;
    }
    khung.replaceChildren(the('h1', mh.tieuDe));
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
const THANH_BEN = {
    BAC_SI: [
        ['Work Schedule', '/doctor/work-schedule'],
        ['Appointment Requests', '/doctor/appointment-requests'],
        ['Consultation / EMR', '/doctor/consultation'],
        ['Doctor Profile', '/doctor/profile'],
    ],
    QUAN_TRI_VIEN: [
        ['Dashboard', '/admin/dashboard'],
        ['Schedule Management', '/admin/schedule-management'],
        ['Medical Catalog', '/admin/medical-catalog'],
        ['Doctor Management', '/admin/doctors'],
        ['Patient Management', '/admin/patients'],
        [null, 'Ngoài danh sách UI'],
        ['Accounts', '/admin/accounts'],
    ],
};
const NAV_CONG_KHAI = [
    ['Book Appointment', '/appointment'],
    ['My Appointments', '/patient/appointments'],
    ['My Patient Profile', '/patient/profile'],
];

function khuVuc(duongDan) {
    if (duongDan.startsWith('/doctor/')) return 'BAC_SI';
    if (duongDan.startsWith('/admin/')) return 'QUAN_TRI_VIEN';
    if (duongDan === '/account' && THANH_BEN[vaiTro()]) return vaiTro();
    return 'CONG_KHAI';
}

function mucDieuHuong([nhan, href], duongDan) {
    if (nhan === null) return the('div', href, 'nhom');
    const a = lienKet(nhan, href);
    if (href === duongDan) a.className = 'dang-xem';
    return a;
}

function veKhung(duongDan) {
    const khu = khuVuc(duongDan);
    const thanhBen = $('thanh-ben');
    if (khu === 'CONG_KHAI') {
        $('nav-tren').replaceChildren(...NAV_CONG_KHAI.map((muc) => mucDieuHuong(muc, duongDan)));
        thanhBen.classList.add('an');
    } else {
        // Thanh trên của bác sĩ / quản trị viên: nút về Dashboard (biểu tượng ngôi nhà trong UI)
        $('nav-tren').replaceChildren(mucDieuHuong(['Dashboard', TRANG_CHU[khu]], duongDan),
            the('span', khu === 'BAC_SI' ? 'Doctor workspace' : 'Admin workspace', 'ghi-chu'));
        thanhBen.replaceChildren(...THANH_BEN[khu].map((muc) => mucDieuHuong(muc, duongDan)));
        thanhBen.classList.remove('an');
    }

    const khuTaiKhoan = $('khu-tai-khoan');
    if (!vaiTro()) {
        khuTaiKhoan.replaceChildren(lienKet('Login', '/login'), lienKet('Register', '/register'));
        return;
    }
    const menu = the('details');
    const noiDung = the('div', null, 'menu-tai-khoan');
    noiDung.append(the('b', taiKhoan.hoTen), the('div', `${VAI_TRO[taiKhoan.vaiTro]} · ${taiKhoan.email}`, 'ghi-chu'),
        lienKet('Change password / tài khoản của tôi', '/account'), lienKet('Open workspace', trangChu()),
        nut('Log out', dangXuat, 'phu'));
    menu.append(the('summary', taiKhoan.hoTen), noiDung);
    // Chuông của UI chỉ vẽ ở khu bác sĩ; trang mẫu hiện cho mọi vai trò vì bệnh nhân cũng nhận thông báo (xác nhận / từ chối)
    khuTaiKhoan.replaceChildren(veChuong(), menu);
    capNhatChuong();
}

// ===== Chuông thông báo =====
//   GET  /api/notification/thong-bao?chuaDoc=&trang=&kichThuoc=   thông báo của tôi, mới nhất trước (TrangDuLieu)
//        mỗi dòng: { id, loai, noiDung, daDoc, ngayTao, idLichHen, maPhieuKham } — maPhieuKham chỉ có khi tôi là bệnh nhân
//   GET  /api/notification/thong-bao/so-chua-doc                  { soChuaDoc }: số trên chuông
//   POST /api/notification/thong-bao/{id}/da-doc                  đánh dấu 1 thông báo đã đọc (gọi lại vẫn được)
//   POST /api/notification/thong-bao/da-doc-tat-ca                { soDaDanhDau }
// Không có đẩy realtime: hỏi lại số chưa đọc mỗi 30 giây, bỏ qua khi tab đang ẩn, hỏi ngay khi tab hiện lại.
// loai: bác sĩ nhận LICH_HEN_MOI / LICH_HEN_DA_HUY / LICH_HEN_DA_DOI / CA_LAM_VIEC_THAY_DOI / KET_QUA_DUYET_DOI_LICH;
//       bệnh nhân nhận LICH_HEN_DA_XAC_NHAN / LICH_HEN_BI_TU_CHOI / LICH_HEN_CAN_DOI / LICH_HEN_DOI_PHONG;
//       quản trị viên nhận YEU_CAU_DOI_LICH_MOI. Thông báo về yêu cầu đổi ca / xin nghỉ có idYeuCau.
const CHU_KY_CHUONG = 30000;
let soChuaDoc = 0;

// Màn hình mở khi bấm 1 thông báo
function dichCuaThongBao(tb) {
    if (vaiTro() === 'BAC_SI') return tb.loai === 'LICH_HEN_MOI' || tb.loai === 'LICH_HEN_DA_DOI' ? '/doctor/appointment-requests' : '/doctor/work-schedule';
    // Quản trị viên: YEU_CAU_DOI_LICH_MOI (tb.idYeuCau) -> bảng yêu cầu ở Schedule Management
    if (vaiTro() === 'QUAN_TRI_VIEN') return '/admin/schedule-management';
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
    nhan.title = 'Thông báo';
    const danhSach = the('div', null, 'menu-tai-khoan ds-thong-bao');
    chuong.append(nhan, danhSach);
    chuong.addEventListener('toggle', () => { if (chuong.open) taiThongBao(danhSach); });
    setTimeout(veSoChuong, 0);
    return chuong;
}

async function taiThongBao(khung) {
    khung.replaceChildren(ghiChu('Đang tải...'));
    const { status, json } = await lay('/api/notification/thong-bao?kichThuoc=10', true);
    if (status !== 200) {
        khung.replaceChildren(the('p', thongDiepLoi(json, 'Không tải được thông báo.'), 'ghi-chu loi'));
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
    dau.append(the('b', `Thông báo (${json.duLieu.tongSoPhanTu})`), nut('Đánh dấu tất cả đã đọc', async () => {
        await goi('/api/notification/thong-bao/da-doc-tat-ca');
        await capNhatChuong();
        taiThongBao(khung);
    }, 'phu nho'));
    khung.replaceChildren(dau, ...(cacDong.length ? cacDong : [ghiChu('Chưa có thông báo nào.')]));
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
