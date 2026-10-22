// Màn hình ngoài danh sách UI: Lịch hẹn toàn viện (ADM-04) — quản trị viên xem mọi lịch hẹn, hủy / đổi lịch thay bệnh nhân,
//             cấp lại liên kết cho người mất link (PAT-03)
// Đường dẫn:  /admin/appointments   (?id=<id lịch hẹn> mở thẳng chi tiết)
// Vai trò:    QUAN_TRI_VIEN
// API:
//   GET  /api/booking/quan-tri/lich-hen?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=&idPhongKham=&trangThai=&kenhDat=&canDoiLich=
//        &canDoiChieu=&tuKhoa=&trang=&kichThuoc=   lịch hẹn có giờ khám trong [tuNgay, denNgay] (mặc định từ hôm nay, 31 ngày; tối đa
//        366 ngày), sớm nhất trước. tuKhoa = mã ECL-... đầy đủ, CCCD 12 số (người khám / người giám hộ) hoặc 1 phần họ tên, SĐT,
//        email. Mỗi dòng như dòng lịch hẹn của bác sĩ, thêm kenhDat (KHACH | TAI_KHOAN | HOTLINE) và chuyenKhoa { id, ten }
//   GET  /api/booking/quan-tri/lich-hen/{id}   chi tiết: lichHen, emailLienHe, taiKhoanDat, ngonNgu, quanTriHuyDoiDuoc,
//        chuoiDoiLich[] { id, maTraCuu, trangThai, gioKhamDuKien, laLichNay }, lichSu[] { thoiDiem, maTraCuu, suKien,
//        nguoiThucHien { loai, idTaiKhoan, hoTen }, lyDo, maTraCuuLienQuan } (nhật ký có từ DOANTOTNGH-14, không có dữ liệu cũ)
//   POST /api/booking/quan-tri/lich-hen/{id}/huy        { lyDo } (bắt buộc) -> QUAN_TRI_HUY, trả chi tiết; bệnh nhân được báo + email
//   POST /api/booking/quan-tri/lich-hen/{id}/doi-lich   { idLichLamViec | idChuyenKhoa, gioBatDauKhung, lyDo } -> 201 chi tiết lịch
//        mới. Không theo hạn hủy / đổi và số lần đổi của bệnh nhân; khung giờ mới theo quy tắc đặt lịch. Hủy / đổi bị từ chối khi
//        đã quá giờ khám (409 LICH_HEN_DA_QUA_GIO, trừ lịch cần đổi vì ca bị hủy) hoặc không còn DA_XAC_NHAN (409)
//   POST /api/booking/quan-tri/lich-hen/{id}/cap-lai-lien-ket   { cccd, cachGui }   hộp thoại hopCapLaiLienKet() của chung.js
//   GET  /api/booking/khung-gio?ngay=&idBacSi=   khung giờ còn trống để đổi sang
//   GET  /api/catalog/chuyen-khoa?kichThuoc=100, /api/catalog/bac-si?kichThuoc=100, /api/catalog/phong-kham   các ô lọc
// Không phản hồi nào ở đây có mã / link phiếu khám (trừ cấp lại MAN_HINH). Đặt lịch thay người gọi: /admin/hotline-booking.
(() => {
    const LICH_HEN = '/api/booking/quan-tri/lich-hen';
    const LOC = ['tuNgay', 'denNgay', 'idChuyenKhoa', 'idBacSi', 'idPhongKham', 'trangThai', 'kenhDat', 'tuKhoa'];
    const SU_KIEN = nhan({
        DAT_LICH: cap('Đặt lịch', 'Booked'), DOI_LICH: cap('Đổi lịch', 'Rescheduled'), HUY: cap('Hủy', 'Cancelled'),
        CA_BI_HUY: cap('Ca khám bị hủy', 'Session cancelled'), DOI_BAC_SI: cap('Đổi bác sĩ (nhường ca)', 'Doctor changed (handover)'),
        CA_THAY_DOI: cap('Ca khám được sửa', 'Session edited'), CAP_LAI_LIEN_KET: cap('Cấp lại liên kết', 'Link reissued'),
    });
    const NGUOI_LAM = nhan({
        KHACH: cap('Khách', 'Guest'), TAI_KHOAN: cap('Tài khoản bệnh nhân', 'Patient account'), BAC_SI: cap('Bác sĩ', 'Doctor'),
        QUAN_TRI_VIEN: cap('Quản trị viên', 'Administrator'),
    });
    let trang = 0, tongSoTrang = 0;

    async function doLuaChon(idO, duongDan, chu, lay_ = (json) => json.duLieu.noiDung) {
        const { status, json } = await lay(duongDan, true);
        if (status !== 200 || !$(idO)) return;
        for (const muc of lay_(json)) {
            const lua = the('option', chu(muc));
            lua.value = muc.id;
            $(idO).append(lua);
        }
    }

    async function taiDanhSach(imLang = false) {
        const f = $('form-loc-lh');
        if (!f) return;
        const thamSo = new URLSearchParams({ trang, kichThuoc: 20, ...thanForm(f, LOC) });
        if (f.elements.canDoiLich.checked) thamSo.set('canDoiLich', 'true');
        if (f.elements.canDoiChieu.checked) thamSo.set('canDoiChieu', 'true');
        const { status, json } = await lay(`${LICH_HEN}?${thamSo}`, imLang);
        const khung = $('lh-ds');
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được danh sách lịch hẹn.', 'Could not load the appointments.')), 'ghi-chu loi'));
            return;
        }
        const duLieu = json.duLieu;
        tongSoTrang = duLieu.tongSoTrang;
        $('lh-trang').textContent = t(`Trang ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} lịch hẹn`,
            `Page ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} appointments`);
        if (!duLieu.noiDung.length) {
            khung.replaceChildren(ghiChu(t('Không có lịch hẹn nào khớp bộ lọc.', 'No appointments match the filter.')));
            return;
        }
        khung.replaceChildren(bang([t('Giờ khám', 'Visit time'), t('Mã tra cứu', 'Lookup code'), t('Bệnh nhân', 'Patient'), t('Bác sĩ', 'Doctor'),
            t('Phòng', 'Room'), t('Kênh đặt', 'Channel'), t('Trạng thái', 'Status'), t('Thao tác', 'Actions')], duLieu.noiDung.map((lh) => dong([
            ngayGioVn(lh.gioKhamDuKien), lh.maTraCuu,
            `${lh.benhNhan.hoTen}${lh.nguoiGiamHo ? t(` (giám hộ: ${lh.nguoiGiamHo.hoTen})`, ` (guardian: ${lh.nguoiGiamHo.hoTen})`) : ''}${lh.doiChieu.canDoiChieu ? t(' ⚠ cần đối chiếu', ' ⚠ identity check') : ''}`,
            `${tenBacSi(lh.bacSi)}${lh.chuyenKhoa ? ' · ' + lh.chuyenKhoa.ten : ''}`, tenPhong(lh.phongKham),
            KENH_DAT[lh.kenhDat] || lh.kenhDat,
            (TRANG_THAI[lh.trangThai] || lh.trangThai) + (lh.canDoiLich ? t(' · cần đổi lịch', ' · needs a reschedule') : ''),
            nut(t('Chi tiết', 'Details'), () => moChiTiet(lh.id)),
        ]))));
    }

    async function moChiTiet(id) {
        const { status, json } = await lay(`${LICH_HEN}/${id}`);
        const khung = $('lh-chi-tiet');
        if (!khung) return;
        $('lh-khoi-chi-tiet').classList.remove('an');
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không mở được lịch hẹn.', 'Could not open the appointment.')), 'ghi-chu loi'));
            return;
        }
        hienChiTiet(json.duLieu);
        $('lh-khoi-chi-tiet').scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function hienChiTiet(ct) {
        const lh = ct.lichHen;
        const khung = $('lh-chi-tiet');
        const phan = [thongTin([
            [t('Mã tra cứu', 'Lookup code'), lh.maTraCuu], [t('Trạng thái', 'Status'), TRANG_THAI[lh.trangThai] || lh.trangThai],
            [t('Cần đổi lịch', 'Needs a reschedule'), lh.canDoiLich ? CAN_DOI_LICH : null], [t('Lý do hủy', 'Cancellation reason'), lh.lyDoHuy],
            [t('Giờ khám dự kiến', 'Expected time'), ngayGioVn(lh.gioKhamDuKien)], [t('Số thứ tự', 'Queue number'), lh.soThuTu],
            [t('Bác sĩ', 'Doctor'), `${tenBacSi(lh.bacSi)}${lh.chuyenKhoa ? ' · ' + lh.chuyenKhoa.ten : ''}`], [t('Phòng khám', 'Room'), tenPhong(lh.phongKham)],
            [t('Bệnh nhân', 'Patient'), `${lh.benhNhan.hoTen}${lh.benhNhan.tuoi != null ? t(` · ${lh.benhNhan.tuoi} tuổi`, ` · age ${lh.benhNhan.tuoi}`) : ''}`],
            [t('SĐT liên hệ', 'Contact phone'), lh.benhNhan.soDienThoai],
            [t('Người giám hộ', 'Guardian'), lh.nguoiGiamHo ? `${lh.nguoiGiamHo.hoTen} (${QUAN_HE[lh.nguoiGiamHo.quanHe] || lh.nguoiGiamHo.quanHe}) · ${lh.nguoiGiamHo.soDienThoai}` : null],
            [t('Email liên hệ', 'Contact email'), ct.emailLienHe], [t('Ngôn ngữ', 'Language'), ct.ngonNgu],
            [t('Kênh đặt', 'Channel'), KENH_DAT[lh.kenhDat] || lh.kenhDat],
            [t('Tài khoản đặt', 'Booking account'), ct.taiKhoanDat ? `#${ct.taiKhoanDat.id} ${ct.taiKhoanDat.hoTen} · ${ct.taiKhoanDat.email}` : null],
            [t('Lý do khám', 'Reason for visit'), lh.lyDoKham], [t('Đặt lúc', 'Booked at'), ngayGioVn(lh.ngayDat)],
            [t('Id hồ sơ bệnh nhân', 'Patient profile id'), lh.idHoSoBenhNhan],
        ])];
        if (ct.chuoiDoiLich.length > 1) {
            phan.push(the('h3', t('Chuỗi đổi lịch', 'Reschedule chain')), bang([t('Mã tra cứu', 'Lookup code'), t('Giờ khám', 'Visit time'), t('Trạng thái', 'Status'), ''],
                ct.chuoiDoiLich.map((m) => dong([m.maTraCuu, ngayGioVn(m.gioKhamDuKien), TRANG_THAI[m.trangThai] || m.trangThai,
                    m.laLichNay ? t('(đang xem)', '(this one)') : nut(t('Mở', 'Open'), () => moChiTiet(m.id), 'phu nho')]))));
        }
        phan.push(the('h3', t('Nhật ký thao tác', 'Activity log')), ct.lichSu.length
            ? bang([t('Lúc', 'When'), t('Lịch hẹn', 'Appointment'), t('Thao tác', 'Action'), t('Người làm', 'By'), t('Lý do', 'Reason'), t('Lịch mới', 'New appointment')],
                ct.lichSu.map((sk) => dong([ngayGioVn(sk.thoiDiem), sk.maTraCuu, SU_KIEN[sk.suKien] || sk.suKien,
                    `${NGUOI_LAM[sk.nguoiThucHien.loai] || sk.nguoiThucHien.loai}${sk.nguoiThucHien.hoTen ? ': ' + sk.nguoiThucHien.hoTen : ''}`,
                    sk.lyDo, sk.maTraCuuLienQuan])))
            : ghiChu(t('Chưa có thao tác nào được ghi (nhật ký chỉ có từ khi có tính năng này).', 'No actions recorded (the log only covers actions since this feature was added).')));
        if (ct.quanTriHuyDoiDuoc) phan.push(khoiCanThiep(lh));
        else phan.push(ghiChu(t('Lịch hẹn này không còn hủy / đổi được (đã khám, đã hủy, hoặc đã quá giờ khám).',
            'This appointment can no longer be cancelled or rescheduled (completed, cancelled, or past its time).')));
        phan.push(hopCapLaiLienKet(`${LICH_HEN}/${lh.id}/cap-lai-lien-ket`, lh.maTraCuu));
        khung.replaceChildren(...phan);
    }

    // Hủy / đổi lịch thay bệnh nhân: lý do bắt buộc, không theo hạn của bệnh nhân
    function khoiCanThiep(lh) {
        const khoi = the('div', null, 'huy-doi');
        const trangThai = ghiChu('');
        const oLyDo = the('input');
        oLyDo.placeholder = t('Lý do (bắt buộc, bệnh nhân đọc được trên phiếu và email)', 'Reason (required, the patient sees it on the ticket and in the email)');
        oLyDo.maxLength = 500;
        const canLyDo = () => {
            if (oLyDo.value.trim()) return true;
            bao(trangThai, t('Nhập lý do trước.', 'Enter the reason first.'), 'loi');
            oLyDo.focus();
            return false;
        };
        const ketQua = (status, json, macDinh) => {
            if (status !== 200 && status !== 201) {
                bao(trangThai, thongDiepLoi(json, macDinh), 'loi');
                return;
            }
            hienChiTiet(json.duLieu);
            bao($('lh-trang-thai'), json.thongDiep, 'tot');
            taiDanhSach(true);
        };
        khoi.append(the('h3', t('Hủy / đổi lịch thay bệnh nhân', 'Cancel / reschedule for the patient')), oLyDo,
            nut(t('Hủy lịch hẹn', 'Cancel the appointment'), async () => {
                if (!canLyDo() || !confirm(t(`Hủy lịch ${lh.maTraCuu}?`, `Cancel ${lh.maTraCuu}?`))) return;
                const { status, json } = await goi(`${LICH_HEN}/${lh.id}/huy`, { lyDo: oLyDo.value.trim() });
                ketQua(status, json, t('Không hủy được.', 'Could not cancel.'));
            }, 'nguy-hiem nho'));

        // Đổi sang khung giờ khác: chọn ngày (+ bác sĩ, mặc định bác sĩ hiện tại) rồi bấm 1 khung còn chỗ
        const oNgay = the('input');
        oNgay.type = 'date';
        oNgay.min = congNgay(0);
        const oBacSi = the('select');
        const dsKhung = the('div');
        oBacSi.append(the('option', tenBacSi(lh.bacSi)));
        oBacSi.firstChild.value = lh.bacSi.id;
        lay(`/api/catalog/bac-si?kichThuoc=100${lh.chuyenKhoa ? '&idChuyenKhoa=' + lh.chuyenKhoa.id : ''}`, true).then(({ status, json }) => {
            if (status !== 200) return;
            for (const bs of json.duLieu.noiDung.filter((b) => b.id !== lh.bacSi.id)) {
                const lua = the('option', tenBacSi(bs));
                lua.value = bs.id;
                oBacSi.append(lua);
            }
        });
        const taiKhung = async () => {
            dsKhung.replaceChildren();
            if (!oNgay.value) return;
            const { status, json } = await lay(`/api/booking/khung-gio?ngay=${oNgay.value}&idBacSi=${oBacSi.value}`, true);
            if (status !== 200) {
                bao(trangThai, thongDiepLoi(json, t('Không tải được khung giờ.', 'Could not load the time slots.')), 'loi');
                return;
            }
            const cacNut = [];
            for (const ca of json.duLieu) {
                for (const k of ca.khungGio) {
                    const n = nut(`${gio(k.gioBatDau)}–${gio(k.gioKetThuc)} (${t(`còn ${k.soChoConLai}`, `${k.soChoConLai} left`)})`, async () => {
                        if (!canLyDo() || !confirm(t(`Đổi lịch sang ${gio(k.gioBatDau)} ${ngayVn(k.gioBatDau)}?`, `Move to ${gio(k.gioBatDau)} ${ngayVn(k.gioBatDau)}?`))) return;
                        const { status: st, json: kq } = await goi(`${LICH_HEN}/${lh.id}/doi-lich`,
                            { idLichLamViec: ca.idLichLamViec, gioBatDauKhung: k.gioBatDau, lyDo: oLyDo.value.trim() });
                        ketQua(st, kq, t('Không đổi được.', 'Could not reschedule.'));
                    }, 'phu nho');
                    n.disabled = k.hetCho;
                    cacNut.push(n);
                }
            }
            bao(trangThai, cacNut.length ? t('Chọn 1 khung giờ để đổi.', 'Choose a time slot to move to.')
                : t('Bác sĩ không có ca nhận đặt lịch trong ngày này.', 'The doctor has no bookable shift on this day.'));
            dsKhung.replaceChildren(...cacNut);
        };
        oNgay.addEventListener('change', taiKhung);
        oBacSi.addEventListener('change', taiKhung);
        khoi.append(the('label', t('Đổi sang ngày / bác sĩ / khung giờ khác', 'Move to another day / doctor / time slot')), oNgay, oBacSi, dsKhung, trangThai);
        return khoi;
    }

    dangKy({
        duongDan: '/admin/appointments',
        vaiTro: 'QUAN_TRI_VIEN',
        tieuDe: cap('Lịch hẹn toàn viện', 'All Appointments'),
        async ve(khung, { thamSo }) {
            trang = 0;
            khuon(khung, `
                <p class="ghi-chu">${t('Đặt lịch thay người gọi tổng đài / người đến quầy:', 'Book for a caller or a walk-in:')} <a href="/admin/hotline-booking">${t('Đặt lịch qua tổng đài', 'Hotline Booking')}</a></p>
                <section>
                    <h2>${t('Bộ lọc', 'Filters')}</h2>
                    <form id="form-loc-lh">
                        <div class="hai-cot">
                            <div><label>${t('Từ ngày (mặc định hôm nay)', 'From (default today)')}</label><input name="tuNgay" type="date"></div>
                            <div><label>${t('Đến ngày (mặc định +30 ngày, tối đa 366 ngày)', 'To (default +30 days, at most 366 days)')}</label><input name="denNgay" type="date"></div>
                            <div><label>${t('Chuyên khoa', 'Specialty')}</label><select name="idChuyenKhoa" id="lh-loc-ck"><option value="">${t('(tất cả)', '(all)')}</option></select></div>
                            <div><label>${t('Bác sĩ', 'Doctor')}</label><select name="idBacSi" id="lh-loc-bs"><option value="">${t('(tất cả)', '(all)')}</option></select></div>
                            <div><label>${t('Phòng khám', 'Room')}</label><select name="idPhongKham" id="lh-loc-phong"><option value="">${t('(tất cả)', '(all)')}</option></select></div>
                            <div><label>${t('Trạng thái', 'Status')}</label><select name="trangThai" id="lh-loc-tt"><option value="">${t('(tất cả)', '(all)')}</option></select></div>
                            <div><label>${t('Kênh đặt', 'Channel')}</label><select name="kenhDat" id="lh-loc-kenh"><option value="">${t('(tất cả)', '(all)')}</option></select></div>
                            <div><label>${t('Từ khoá: mã ECL-… đầy đủ, CCCD 12 số, hoặc 1 phần họ tên / SĐT / email', 'Keyword: full ECL-… code, 12-digit national ID, or part of a name / phone / email')}</label><input name="tuKhoa" maxlength="100"></div>
                        </div>
                        <label class="chon"><input type="checkbox" name="canDoiLich">${t('Chỉ lịch cần đổi (ca bị hủy)', 'Only appointments needing a reschedule (session cancelled)')}</label>
                        <label class="chon"><input type="checkbox" name="canDoiChieu">${t('Chỉ lịch cần đối chiếu giấy tờ', 'Only appointments needing an identity check')}</label>
                        <button class="phu">${t('Lọc', 'Filter')}</button>
                    </form>
                    <div id="lh-ds" class="bang"></div>
                    <p id="lh-trang" class="ghi-chu"></p>
                    <button id="lh-truoc" class="phu nho">${t('‹ Trang trước', '‹ Previous page')}</button>
                    <button id="lh-sau" class="phu nho">${t('Trang sau ›', 'Next page ›')}</button>
                    <p id="lh-trang-thai" class="ghi-chu"></p>
                </section>
                <section id="lh-khoi-chi-tiet" class="an">
                    <h2>${t('Chi tiết lịch hẹn', 'Appointment details')}</h2>
                    <div id="lh-chi-tiet"></div>
                </section>`);
            for (const [ma, chu] of Object.entries(TRANG_THAI)) {
                const lua = the('option', chu);
                lua.value = ma;
                $('lh-loc-tt').append(lua);
            }
            for (const [ma, chu] of Object.entries(KENH_DAT)) {
                const lua = the('option', chu);
                lua.value = ma;
                $('lh-loc-kenh').append(lua);
            }
            doLuaChon('lh-loc-ck', '/api/catalog/chuyen-khoa?kichThuoc=100', (ck) => ck.tenChuyenKhoa);
            doLuaChon('lh-loc-bs', '/api/catalog/bac-si?kichThuoc=100', (bs) => `${tenBacSi(bs)} · ${bs.tenChuyenKhoa}`);
            doLuaChon('lh-loc-phong', '/api/catalog/phong-kham', (p) => `${tenPhong(p)} · ${p.tenChuyenKhoa}`, (json) => json.duLieu);
            $('form-loc-lh').addEventListener('submit', (e) => {
                e.preventDefault();
                trang = 0;
                taiDanhSach();
            });
            $('lh-truoc').addEventListener('click', () => { if (trang > 0) { trang--; taiDanhSach(); } });
            $('lh-sau').addEventListener('click', () => { if (trang + 1 < tongSoTrang) { trang++; taiDanhSach(); } });
            await taiDanhSach(true);
            if (/^\d+$/.test(thamSo.get('id') || '')) await moChiTiet(thamSo.get('id'));
        },
    });
})();
