// Màn hình ngoài danh sách UI: quản trị tài khoản (UI chưa có màn hình riêng; "Doctor Management" và "Patient Management"
// của UI là màn hình khác: qt-bac-si.js, qt-benh-nhan.js)
// Đường dẫn:  /admin/accounts
// Vai trò:    QUAN_TRI_VIEN
// API:
//   GET    /api/users?tuKhoa=&vaiTro=&trangThai=&trang=&kichThuoc=   tìm tài khoản (phân trang)
//   GET    /api/users/{id}                                           xem 1 tài khoản
//   PUT    /api/users/{id}/status   { trangThai: VO_HIEU_HOA | DA_KICH_HOAT, lyDo? }
//   DELETE /api/users/{id}
// Tài khoản quản trị viên không vô hiệu hoá / xoá được qua API.
(() => {
    const TAI_KHOAN = '/api/users';
    let boLoc = {}, trang = 0, tongSoTrang = 0;

    function hangThongBao(thongBao) {
        const tr = the('tr');
        const td = the('td', thongBao);
        td.colSpan = 6;
        tr.append(td);
        return tr;
    }

    function hangTaiKhoan(tk) {
        const trangThai = tk.trangThai + (tk.lyDoVoHieuHoa ? ` (${tk.lyDoVoHieuHoa})` : '');
        const thaoTac = [nut(t('Xem', 'View'), () => lay(`${TAI_KHOAN}/${tk.id}`), 'phu nho')];
        // Tài khoản chưa xác thực email chỉ xoá được
        if (tk.vaiTro !== 'QUAN_TRI_VIEN') {
            if (tk.trangThai === 'DA_KICH_HOAT') {
                thaoTac.push(nut(t('Vô hiệu hoá', 'Deactivate'), () => doiTrangThai(tk.id, 'VO_HIEU_HOA'), 'nguy-hiem nho'));
            }
            if (tk.trangThai === 'VO_HIEU_HOA') {
                thaoTac.push(nut(t('Kích hoạt lại', 'Reactivate'), () => doiTrangThai(tk.id, 'DA_KICH_HOAT')));
            }
            thaoTac.push(nut(t('Xoá', 'Delete'), () => xoaTaiKhoan(tk), 'nguy-hiem nho'));
        }
        return dong([tk.id, tk.hoTen, tk.email, tk.vaiTro, trangThai, thaoTac]);
    }

    async function taiTaiKhoan(imLang = false) {
        const thamSo = new URLSearchParams({ trang, kichThuoc: 10 });
        for (const [ten, giaTriLoc] of Object.entries(boLoc)) {
            if (giaTriLoc) thamSo.set(ten, giaTriLoc);
        }
        const { status, json } = await lay(`${TAI_KHOAN}?${thamSo}`, imLang);
        const than = $('ds-tai-khoan');
        if (!than) return;
        if (status !== 200) {
            than.replaceChildren(hangThongBao(thongDiepLoi(json, t('Không tải được danh sách.', 'Could not load the list.'))));
            $('trang-tai-khoan').textContent = '';
            return;
        }
        const duLieu = json.duLieu;
        tongSoTrang = duLieu.tongSoTrang;
        than.replaceChildren(...(duLieu.noiDung.length ? duLieu.noiDung.map(hangTaiKhoan)
            : [hangThongBao(t('Không có tài khoản nào.', 'No accounts found.'))]));
        $('trang-tai-khoan').textContent = t(
            `Trang ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} tài khoản`,
            `Page ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} accounts`);
    }

    async function doiTrangThai(id, trangThai) {
        const body = { trangThai };
        if (trangThai === 'VO_HIEU_HOA') body.lyDo = $('ly-do').value;
        const { status } = await goi(`${TAI_KHOAN}/${id}/status`, body, 'PUT');
        if (status === 200) {
            $('ly-do').value = '';
            await taiTaiKhoan(true);
        }
    }

    async function xoaTaiKhoan(tk) {
        if (!confirm(t(`Xoá hẳn tài khoản ${tk.email} (id=${tk.id})? Không khôi phục được.`,
            `Permanently delete account ${tk.email} (id=${tk.id})? This cannot be undone.`))) return;
        const { status } = await goi(`${TAI_KHOAN}/${tk.id}`, undefined, 'DELETE');
        if (status === 200) await taiTaiKhoan(true);
    }

    dangKy({
        duongDan: '/admin/accounts',
        vaiTro: 'QUAN_TRI_VIEN',
        tieuDe: cap('Tài khoản', 'Accounts'),
        async ve(khung) {
            khuon(khung, `
                <section>
                    <h2>${t('Quản trị tài khoản', 'Account administration')}</h2>
                    <form id="form-tim-tai-khoan" class="hang-loc">
                        <input name="tuKhoa" placeholder="${t('Từ khoá: họ tên, email, số điện thoại', 'Keyword: full name, email, phone number')}">
                        <select name="vaiTro">
                            <option value="">${t('Mọi vai trò', 'All roles')}</option>
                            <option>BENH_NHAN</option>
                            <option>BAC_SI</option>
                            <option>QUAN_TRI_VIEN</option>
                        </select>
                        <select name="trangThai">
                            <option value="">${t('Mọi trạng thái', 'All statuses')}</option>
                            <option>DA_KICH_HOAT</option>
                            <option>CHO_XAC_NHAN</option>
                            <option>VO_HIEU_HOA</option>
                        </select>
                        <button>${t('Tìm', 'Search')}</button>
                    </form>
                    <input id="ly-do" placeholder="${t('Lý do vô hiệu hoá (nhập trước khi bấm Vô hiệu hoá)', 'Reason for deactivation (enter it before pressing Deactivate)')}" maxlength="500">
                    <div class="bang">
                        <table>
                            <thead><tr><th>${t('Id', 'ID')}</th><th>${t('Họ tên', 'Full name')}</th><th>Email</th><th>${t('Vai trò', 'Role')}</th><th>${t('Trạng thái', 'Status')}</th><th>${t('Thao tác', 'Actions')}</th></tr></thead>
                            <tbody id="ds-tai-khoan"></tbody>
                        </table>
                    </div>
                    <p id="trang-tai-khoan" class="ghi-chu"></p>
                    <button id="nut-trang-truoc" class="phu nho">${t('‹ Trang trước', '‹ Previous page')}</button>
                    <button id="nut-trang-sau" class="phu nho">${t('Trang sau ›', 'Next page ›')}</button>
                    <p class="ghi-chu">${t('Nút "Xem" và lỗi của các thao tác hiện ở ô "Kết quả gọi API gần nhất".',
                        'The View button and errors of the actions are shown in the latest API call result box.')}</p>
                </section>`);
            boLoc = {};
            trang = 0;
            khiGui('form-tim-tai-khoan', (v) => {
                boLoc = v;
                trang = 0;
                return taiTaiKhoan();
            });
            $('nut-trang-truoc').addEventListener('click', () => {
                if (trang > 0) {
                    trang--;
                    taiTaiKhoan();
                }
            });
            $('nut-trang-sau').addEventListener('click', () => {
                if (trang + 1 < tongSoTrang) {
                    trang++;
                    taiTaiKhoan();
                }
            });
            await taiTaiKhoan(true);
        },
    });
})();
