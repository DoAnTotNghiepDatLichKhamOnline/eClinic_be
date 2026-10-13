// Màn hình UI: Patient Management (Admin)
// Đường dẫn:  /admin/patients   (?id=<id hồ sơ bệnh nhân> mở thẳng 1 hồ sơ; id lấy từ idHoSoBenhNhan của lịch hẹn)
// Vai trò:    QUAN_TRI_VIEN
// API:
//   GET  /api/booking/quan-tri/ho-so-benh-nhan?tuKhoa=&trangThaiLienKet=&trang=&kichThuoc=   danh sách hồ sơ, mới tạo trước:
//        id, hoTen, ngaySinh, gioiTinh, soDienThoai, cccdChe (chỉ còn 4 số cuối), coTaiKhoan, trangThaiLienKet, soLichHen, ngayTao.
//        tuKhoa so với họ tên / số điện thoại / số bảo hiểm y tế, hoặc đủ 12 số CCCD
//   GET  /api/booking/quan-tri/ho-so-benh-nhan/{id}/lich-hen?trang=&kichThuoc=   lịch hẹn của hồ sơ, mọi trạng thái, muộn nhất trước
//        (không có chẩn đoán / đơn thuốc)
//   GET  /api/booking/quan-tri/ho-so-benh-nhan/cho-xac-minh?trang=&kichThuoc=   hồ sơ đang chờ xác minh liên kết với tài khoản
//        (tài khoản khai đúng số CCCD nhưng họ tên / số điện thoại không khớp hồ sơ), cũ nhất trước
//   POST /api/booking/quan-tri/ho-so-benh-nhan/{id}/duyet        hồ sơ thuộc về tài khoản đang chờ
//   POST /api/booking/quan-tri/ho-so-benh-nhan/{id}/tu-choi      { lyDo? } : hồ sơ trở lại chưa liên kết
//   GET  /api/booking/quan-tri/ho-so-benh-nhan/{id}              xem 1 hồ sơ (số CCCD không che)
//   PUT  /api/booking/quan-tri/ho-so-benh-nhan/{id}              sửa sau khi đối chiếu giấy tờ: hoTen, ngaySinh, gioiTinh,
//        soDienThoai, cccd (chỉ điền được khi hồ sơ chưa có), diaChi, soBaoHiemYTe
(() => {
    const HO_SO = '/api/booking/quan-tri/ho-so-benh-nhan';
    const TRUONG = ['hoTen', 'ngaySinh', 'gioiTinh', 'soDienThoai', 'cccd', 'diaChi', 'soBaoHiemYTe'];
    let hoSoDangSua = null;
    let trang = 0, tongSoTrang = 0;
    let trangDs = 0, tongSoTrangDs = 0;
    let trangLichHen = 0, tongSoTrangLichHen = 0;

    async function taiDanhSach(imLang = false) {
        const thamSo = new URLSearchParams({ trang: trangDs, kichThuoc: 10, ...thanForm($('form-tim-ho-so'), ['tuKhoa', 'trangThaiLienKet']) });
        const { status, json } = await lay(`${HO_SO}?${thamSo}`, imLang);
        const khung = $('ds-ho-so');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được danh sách hồ sơ bệnh nhân.', 'Could not load the patient profile list.')), 'ghi-chu loi'));
            return;
        }
        const duLieu = json.duLieu;
        tongSoTrangDs = duLieu.tongSoTrang;
        $('trang-ho-so').textContent = t(`Trang ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} hồ sơ`,
            `Page ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} profiles`);
        if (!duLieu.noiDung.length) {
            khung.replaceChildren(ghiChu(t('Không có hồ sơ bệnh nhân nào khớp.', 'No matching patient profiles.')));
            return;
        }
        khung.replaceChildren(bang([t('Id', 'ID'), t('Họ tên', 'Full name'), t('Ngày sinh', 'Date of birth'), t('SĐT', 'Phone'), t('CCCD (đã che)', 'National ID (masked)'), t('Tài khoản', 'Account'), t('Số lịch hẹn', 'Appointments'), t('Ngày tạo', 'Created'), t('Thao tác', 'Actions')], duLieu.noiDung.map((hs) => dong([
            hs.id, hs.hoTen, hs.ngaySinh ? ngayVn(hs.ngaySinh) : '', hs.soDienThoai, hs.cccdChe,
            hs.coTaiKhoan ? (LIEN_KET[hs.trangThaiLienKet] || hs.trangThaiLienKet) : t('Khách (không có tài khoản)', 'Guest (no account)'),
            hs.soLichHen, hs.ngayTao ? ngayGioVn(hs.ngayTao) : '',
            nut(t('Mở hồ sơ', 'Open profile'), () => moHoSo(hs.id)),
        ]))));
    }

    async function taiLichHen(imLang = false) {
        const khung = $('ho-so-qt-lich-hen');
        if (!khung || !hoSoDangSua) return;
        const { status, json } = await lay(`${HO_SO}/${hoSoDangSua.id}/lich-hen?trang=${trangLichHen}&kichThuoc=10`, imLang);
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được lịch hẹn của hồ sơ.', 'Could not load the appointments of the profile.')), 'ghi-chu loi'));
            return;
        }
        const duLieu = json.duLieu;
        tongSoTrangLichHen = duLieu.tongSoTrang;
        $('trang-lich-hen-ho-so').textContent = t(`Trang ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} lịch hẹn`,
            `Page ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} appointments`);
        if (!duLieu.noiDung.length) {
            khung.replaceChildren(ghiChu(t('Hồ sơ này chưa có lịch hẹn nào.', 'This profile has no appointments yet.')));
            return;
        }
        khung.replaceChildren(bang([t('Mã tra cứu', 'Lookup code'), t('Giờ khám', 'Visit time'), t('Bác sĩ', 'Doctor'), t('Phòng', 'Room'), t('Lý do khám', 'Reason for visit'), t('Trạng thái', 'Status')], duLieu.noiDung.map((lh) => dong([
            lh.maTraCuu, ngayGioVn(lh.gioKhamDuKien), tenBacSi(lh.bacSi), lh.phongKham ? lh.phongKham.tenPhong : '', lh.lyDoKham,
            (TRANG_THAI[lh.trangThai] || lh.trangThai) + (lh.canDoiLich ? t(' · cần đổi lịch', ' · needs a reschedule') : '') + (lh.lyDoHuy ? ` · ${lh.lyDoHuy}` : ''),
        ]))));
    }

    async function taiChoXacMinh(imLang = false) {
        const { status, json } = await lay(`${HO_SO}/cho-xac-minh?trang=${trang}&kichThuoc=10`, imLang);
        const khung = $('ds-cho-xac-minh');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được danh sách.', 'Could not load the list.')), 'ghi-chu loi'));
            return;
        }
        const duLieu = json.duLieu;
        tongSoTrang = duLieu.tongSoTrang;
        $('trang-cho-xac-minh').textContent = t(`Trang ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} hồ sơ`,
            `Page ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} profiles`);
        if (!duLieu.noiDung.length) {
            khung.replaceChildren(ghiChu(t('Không có hồ sơ nào đang chờ xác minh.', 'No profiles are waiting for verification.')));
            return;
        }
        const khop = (dung) => (dung ? t('khớp', 'match') : t('KHÁC', 'DIFFERENT'));
        khung.replaceChildren(bang([t('Hồ sơ', 'Profile'), t('CCCD', 'National ID'), t('Số lịch hẹn', 'Appointments'), t('Tài khoản đang chờ', 'Waiting account'), t('Khớp', 'Match'), t('Thao tác', 'Actions')], duLieu.noiDung.map((muc) => {
            const hs = muc.hoSo, tk = muc.taiKhoan;
            return dong([
                `#${hs.id} ${hs.hoTen} · ${hs.ngaySinh ? ngayVn(hs.ngaySinh) : '?'} · ${hs.soDienThoai || ''}`, hs.cccd, hs.soLichHen,
                `#${tk.id} ${tk.hoTen} · ${tk.email} · ${tk.soDienThoai || ''}`,
                `${t('Họ tên', 'Full name')}: ${khop(muc.khopHoTen)} · ${t('SĐT', 'Phone')}: ${khop(muc.khopSoDienThoai)}`,
                [nut(t('Duyệt', 'Approve'), () => xuLy(hs.id, 'duyet')), nut(t('Từ chối', 'Reject'), () => xuLy(hs.id, 'tu-choi'), 'nguy-hiem nho'), nut(t('Mở hồ sơ', 'Open profile'), () => moHoSo(hs.id), 'phu nho')],
            ]);
        })));
    }

    async function xuLy(id, hanhDong) {
        const lyDo = $('ly-do-tu-choi').value.trim();
        const { status, json } = hanhDong === 'duyet'
            ? await goi(`${HO_SO}/${id}/duyet`)
            : await goi(`${HO_SO}/${id}/tu-choi`, lyDo ? { lyDo } : undefined);
        bao('cho-xac-minh-trang-thai', status === 200 ? json.thongDiep : thongDiepLoi(json, t('Không thực hiện được.', 'The action failed.')), status === 200 ? 'tot' : 'loi');
        if (status === 200) {
            $('ly-do-tu-choi').value = '';
            await taiChoXacMinh(true);
        }
    }

    async function moHoSo(id) {
        const { status, json } = await lay(`${HO_SO}/${id}`);
        if (!$('khoi-ho-so-qt')) return;
        $('khoi-ho-so-qt').classList.remove('an');
        if (status !== 200) {
            hoSoDangSua = null;
            $('ho-so-qt-thong-tin').replaceChildren(the('p', thongDiepLoi(json, t('Không mở được hồ sơ bệnh nhân.', 'Could not open the patient profile.')), 'ghi-chu loi'));
            $('form-sua-ho-so-qt').classList.add('an');
            return;
        }
        hienHoSo(json.duLieu);
        trangLichHen = 0;
        await taiLichHen(true);
        $('khoi-ho-so-qt').scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function hienHoSo(hs) {
        hoSoDangSua = hs;
        $('ho-so-qt-thong-tin').replaceChildren(thongTin([
            [t('Id hồ sơ', 'Profile ID'), hs.id], [t('Liên kết tài khoản', 'Account link'), LIEN_KET[hs.trangThaiLienKet] || hs.trangThaiLienKet],
            [t('Id tài khoản', 'Account ID'), hs.idTaiKhoan], [t('Ngày tạo', 'Created'), hs.ngayTao ? ngayGioVn(hs.ngayTao) : null],
        ]));
        const f = $('form-sua-ho-so-qt');
        f.classList.remove('an');
        for (const ten of TRUONG) f.elements[ten].value = hs[ten] || '';
        // Số CCCD chỉ điền được khi hồ sơ chưa có
        f.elements.cccd.disabled = !!hs.cccd;
    }

    dangKy({
        duongDan: '/admin/patients',
        vaiTro: 'QUAN_TRI_VIEN',
        tieuDe: cap('Quản lý bệnh nhân', 'Patient Management'),
        async ve(khung, { thamSo }) {
            hoSoDangSua = null;
            trang = 0;
            trangDs = 0;
            khuon(khung, `
                <section>
                    <h2>${t('Danh sách hồ sơ bệnh nhân', 'Patient profiles')}</h2>
                    <form id="form-tim-ho-so" class="hai-cot">
                        <div><label>${t('Từ khoá (họ tên, số điện thoại, số bảo hiểm y tế, hoặc đủ 12 số CCCD)', 'Keyword (full name, phone number, health insurance number, or all 12 digits of the national ID)')}</label><input name="tuKhoa" maxlength="100"></div>
                        <div><label>${t('Liên kết tài khoản', 'Account link')}</label><select name="trangThaiLienKet"><option value="">${t('(tất cả)', '(all)')}</option><option value="CHUA_LIEN_KET">${t('Chưa liên kết', 'Not linked')}</option><option value="CHO_XAC_MINH">${t('Chờ xác minh', 'Awaiting verification')}</option><option value="DA_LIEN_KET">${t('Đã liên kết', 'Linked')}</option></select></div>
                        <button class="phu">${t('Tìm', 'Search')}</button>
                    </form>
                    <div id="ds-ho-so" class="bang"></div>
                    <p id="trang-ho-so" class="ghi-chu"></p>
                    <button id="nut-hs-truoc" class="phu nho">${t('‹ Trang trước', '‹ Previous page')}</button>
                    <button id="nut-hs-sau" class="phu nho">${t('Trang sau ›', 'Next page ›')}</button>
                </section>
                <section>
                    <h2>${t('Hồ sơ chờ xác minh liên kết tài khoản', 'Profiles awaiting account link verification')}</h2>
                    <input id="ly-do-tu-choi" placeholder="${t('Lý do từ chối (không bắt buộc, nhập trước khi bấm Từ chối)', 'Rejection reason (optional, enter it before clicking Reject)')}" maxlength="500">
                    <div id="ds-cho-xac-minh" class="bang"></div>
                    <p id="trang-cho-xac-minh" class="ghi-chu"></p>
                    <button id="nut-cxm-truoc" class="phu nho">${t('‹ Trang trước', '‹ Previous page')}</button>
                    <button id="nut-cxm-sau" class="phu nho">${t('Trang sau ›', 'Next page ›')}</button>
                    <p id="cho-xac-minh-trang-thai" class="ghi-chu"></p>
                </section>
                <section>
                    <h2>${t('Mở hồ sơ bệnh nhân theo id', 'Open a patient profile by id')}</h2>
                    <form id="form-mo-ho-so-qt" class="hai-cot">
                        <div><label>${t('Id hồ sơ (idHoSoBenhNhan của lịch hẹn, xem ở Quản lý lịch làm việc)', 'Profile id (idHoSoBenhNhan of the appointment, see Schedule Management)')}</label><input name="id" type="number" min="1" required></div>
                        <button class="phu">${t('Mở hồ sơ', 'Open profile')}</button>
                    </form>
                </section>
                <section id="khoi-ho-so-qt" class="an">
                    <h2>${t('Hồ sơ bệnh nhân', 'Patient profile')}</h2>
                    <div id="ho-so-qt-thong-tin"></div>
                    <form id="form-sua-ho-so-qt">
                        <div class="hai-cot">
                            <div><label>${t('Họ tên', 'Full name')}</label><input name="hoTen" required maxlength="150"></div>
                            <div><label>${t('Ngày sinh', 'Date of birth')}</label><input name="ngaySinh" type="date" required></div>
                            <div><label>${t('Giới tính', 'Gender')}</label><select name="gioiTinh"><option value="">${t('(chưa rõ)', '(unknown)')}</option><option value="NAM">${GIOI_TINH.NAM}</option><option value="NU">${GIOI_TINH.NU}</option><option value="KHAC">${GIOI_TINH.KHAC}</option></select></div>
                            <div><label>${t('Số điện thoại', 'Phone number')}</label><input name="soDienThoai" required pattern="0\\d{9}"></div>
                            <div><label>${t('Số CCCD (chỉ điền được khi hồ sơ chưa có)', 'National ID number (can only be filled in when the profile has none)')}</label><input name="cccd" pattern="\\d{12}"></div>
                            <div><label>${t('Số bảo hiểm y tế', 'Health insurance number')}</label><input name="soBaoHiemYTe" maxlength="50"></div>
                        </div>
                        <label>${t('Địa chỉ', 'Address')}</label><input name="diaChi" maxlength="500">
                        <button>${t('Lưu hồ sơ bệnh nhân (sau khi đã đối chiếu giấy tờ)', 'Save patient profile (after the identity check)')}</button>
                    </form>
                    <p id="ho-so-qt-trang-thai" class="ghi-chu"></p>
                    <h3>${t('Lịch hẹn của hồ sơ', 'Appointments of the profile')}</h3>
                    <div id="ho-so-qt-lich-hen" class="bang"></div>
                    <p id="trang-lich-hen-ho-so" class="ghi-chu"></p>
                    <button id="nut-lh-truoc" class="phu nho">${t('‹ Trang trước', '‹ Previous page')}</button>
                    <button id="nut-lh-sau" class="phu nho">${t('Trang sau ›', 'Next page ›')}</button>
                </section>`);

            $('form-tim-ho-so').addEventListener('submit', (e) => {
                e.preventDefault();
                trangDs = 0;
                taiDanhSach();
            });
            $('nut-hs-truoc').addEventListener('click', () => {
                if (trangDs > 0) {
                    trangDs--;
                    taiDanhSach();
                }
            });
            $('nut-hs-sau').addEventListener('click', () => {
                if (trangDs + 1 < tongSoTrangDs) {
                    trangDs++;
                    taiDanhSach();
                }
            });
            $('nut-lh-truoc').addEventListener('click', () => {
                if (trangLichHen > 0) {
                    trangLichHen--;
                    taiLichHen();
                }
            });
            $('nut-lh-sau').addEventListener('click', () => {
                if (trangLichHen + 1 < tongSoTrangLichHen) {
                    trangLichHen++;
                    taiLichHen();
                }
            });

            $('nut-cxm-truoc').addEventListener('click', () => {
                if (trang > 0) {
                    trang--;
                    taiChoXacMinh();
                }
            });
            $('nut-cxm-sau').addEventListener('click', () => {
                if (trang + 1 < tongSoTrang) {
                    trang++;
                    taiChoXacMinh();
                }
            });
            khiGui('form-mo-ho-so-qt', (v) => moHoSo(v.id));
            $('form-sua-ho-so-qt').addEventListener('submit', async (e) => {
                e.preventDefault();
                const { status, json } = await goi(`${HO_SO}/${hoSoDangSua.id}`, thanForm(e.target, TRUONG), 'PUT');
                if (status !== 200) {
                    bao('ho-so-qt-trang-thai', thongDiepLoi(json, t('Không lưu được hồ sơ bệnh nhân.', 'Could not save the patient profile.')), 'loi');
                    return;
                }
                hienHoSo(json.duLieu);
                bao('ho-so-qt-trang-thai', json.thongDiep || t('Đã cập nhật hồ sơ bệnh nhân.', 'Patient profile updated.'), 'tot');
                await taiDanhSach(true);
            });

            await taiDanhSach(true);
            await taiChoXacMinh(true);
            if (/^\d+$/.test(thamSo.get('id') || '')) await moHoSo(thamSo.get('id'));
        },
    });
})();
