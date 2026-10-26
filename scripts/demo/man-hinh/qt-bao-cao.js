// Màn hình ngoài danh sách UI: Báo cáo thống kê (DASH-01..04, ADM-05) — report-service, chỉ đọc
// Đường dẫn:  /admin/reports
// Vai trò:    QUAN_TRI_VIEN
// API:
//   GET /api/report/quan-tri/thong-ke?ky=NGAY|TUAN|THANG|NAM|TUY_CHON&ngay=&tuNgay=&denNgay=&idChuyenKhoa=
//       ky mặc định THANG quanh ngay (mặc định hôm nay); TUAN = thứ 2 – chủ nhật; TUY_CHON cần tuNgay + denNgay (tối đa 366 ngày,
//       400 DU_LIEU_KHONG_HOP_LE); idChuyenKhoa lạ -> 404. Trả: ky, tuNgay, denNgay, idChuyenKhoa, donViThoiGian (NGAY khi ≤ 31 ngày,
//       TUAN khi ≤ 184, THANG), tongQuan, trangThai, kenhDat, thoiGianDatTruoc, theoChuyenKhoa[], theoBacSi[], theoThoiGian[], tinhLuc
//       Lịch hẹn tính theo giờ khám, không tính lịch cũ của 1 lần đổi lịch. Tỷ lệ (%, 1 chữ số thập phân) là null khi mẫu số 0.
//   GET /api/report/quan-tri/thong-ke/xuat-excel   cùng tham số: tệp bao-cao-thong-ke_<tu>_<den>.xlsx (5 trang, theo ngôn ngữ)
//   GET /api/catalog/chuyen-khoa?kichThuoc=100     ô chọn chuyên khoa
// Biểu đồ vẽ bằng SVG ngay trong trang (không thư viện).
(() => {
    const BAO_CAO = '/api/report/quan-tri/thong-ke';
    const SVG = 'http://www.w3.org/2000/svg';
    const phanTram = (x) => (x === null || x === undefined ? '—' : `${x}%`);

    function svg(ten, thuocTinh, noiDung) {
        const el = document.createElementNS(SVG, ten);
        for (const [k, v] of Object.entries(thuocTinh || {})) el.setAttribute(k, v);
        if (noiDung !== undefined) el.textContent = noiDung;
        return el;
    }

    // Biểu đồ cột ngang: lượt khám và lịch hẹn của từng chuyên khoa
    function bieuDoCot(cacDong) {
        const cao = 24, rongNhan = 170, rongCot = 360;
        const lonNhat = Math.max(1, ...cacDong.map((d) => d.chiSo.lichHen));
        const hinh = svg('svg', { width: rongNhan + rongCot + 90, height: cacDong.length * cao + 10, role: 'img' });
        cacDong.forEach((d, i) => {
            const y = i * cao + 5;
            hinh.append(svg('text', { x: 0, y: y + 15, 'font-size': 12 }, d.tenChuyenKhoa),
                svg('rect', { x: rongNhan, y, width: (d.chiSo.lichHen / lonNhat) * rongCot, height: cao - 8, fill: '#d0d7de' }),
                svg('rect', { x: rongNhan, y, width: (d.chiSo.luotKham / lonNhat) * rongCot, height: cao - 8, fill: MAU.daKham }),
                svg('text', { x: rongNhan + (d.chiSo.lichHen / lonNhat) * rongCot + 6, y: y + 13, 'font-size': 12 }, `${d.chiSo.luotKham} / ${d.chiSo.lichHen}`));
        });
        return hinh;
    }

    // Đường theo thời gian: lịch hẹn (xanh), lượt khám (lục), đã hủy (xám)
    function bieuDoDuong(cacDiem) {
        const rong = 640, cao = 220, le = 36;
        const lonNhat = Math.max(1, ...cacDiem.map((d) => d.lichHen));
        const x = (i) => le + (cacDiem.length > 1 ? (i * (rong - 2 * le)) / (cacDiem.length - 1) : (rong - 2 * le) / 2);
        const y = (v) => cao - le - (v / lonNhat) * (cao - 2 * le);
        const hinh = svg('svg', { width: rong, height: cao, role: 'img' });
        hinh.append(svg('line', { x1: le, y1: cao - le, x2: rong - le, y2: cao - le, stroke: '#8c959f' }),
            svg('text', { x: 2, y: y(lonNhat) + 4, 'font-size': 11 }, String(lonNhat)), svg('text', { x: 2, y: cao - le + 4, 'font-size': 11 }, '0'));
        for (const [truong, mau] of [['lichHen', MAU.cuaToi], ['luotKham', MAU.daKham], ['daHuy', MAU.daHuy]]) {
            hinh.append(svg('polyline', { fill: 'none', stroke: mau, 'stroke-width': 2,
                points: cacDiem.map((d, i) => `${x(i)},${y(d[truong])}`).join(' ') }));
        }
        const buoc = Math.max(1, Math.ceil(cacDiem.length / 8));
        cacDiem.forEach((d, i) => {
            if (i % buoc === 0) hinh.append(svg('text', { x: x(i) - 14, y: cao - le + 16, 'font-size': 10 }, ngayVn(d.tuNgay).slice(0, 5)));
        });
        return hinh;
    }

    function oSo(cacO) {
        const khoi = the('div', null, 'o-so');
        for (const [nhanO, giaTri] of cacO) {
            const o = the('div');
            o.append(the('b', String(giaTri)), the('span', nhanO, 'ghi-chu'));
            khoi.append(o);
        }
        return khoi;
    }

    function thamSo() {
        const f = $('form-bao-cao');
        const ts = new URLSearchParams({ ky: f.elements.ky.value });
        if (f.elements.ky.value === 'TUY_CHON') {
            ts.set('tuNgay', f.elements.tuNgay.value);
            ts.set('denNgay', f.elements.denNgay.value);
        } else if (f.elements.ngay.value) {
            ts.set('ngay', f.elements.ngay.value);
        }
        if (f.elements.idChuyenKhoa.value) ts.set('idChuyenKhoa', f.elements.idChuyenKhoa.value);
        return ts;
    }

    async function taiBaoCao(imLang = false) {
        const { status, json } = await lay(`${BAO_CAO}?${thamSo()}`, imLang);
        const khung = $('bc-noi-dung');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không lập được báo cáo.', 'Could not build the report.')), 'ghi-chu loi'));
            return;
        }
        const bc = json.duLieu, tq = bc.tongQuan, tt = bc.trangThai, kd = bc.kenhDat, tg = bc.thoiGianDatTruoc;
        const donVi = { NGAY: t('ngày', 'day'), TUAN: t('tuần', 'week'), THANG: t('tháng', 'month') }[bc.donViThoiGian];
        khung.replaceChildren(
            ghiChu(t(`Kỳ ${ngayVn(bc.tuNgay)} – ${ngayVn(bc.denNgay)} · tính lúc ${ngayGioVn(bc.tinhLuc)}`,
                `Period ${ngayVn(bc.tuNgay)} – ${ngayVn(bc.denNgay)} · calculated at ${ngayGioVn(bc.tinhLuc)}`)),
            oSo([[t('Lịch hẹn', 'Appointments'), tq.lichHen], [t('Lượt khám', 'Visits'), tq.luotKham],
                [t('Bệnh nhân đã khám (duy nhất)', 'Unique patients seen'), tq.benhNhanDaKham], [t('Lượt đặt mới', 'New bookings'), tq.luotDatMoi],
                [t('Tỷ lệ hủy', 'Cancellation rate'), phanTram(tq.tyLeHuy)], [t('Tỷ lệ không đến', 'No-show rate'), phanTram(tq.tyLeKhongDen)],
                [t('Lấp đầy lượt khám', 'Slot utilisation'), `${phanTram(tq.tyLeLapDay)} (${tq.luotDaDat}/${tq.tongLuot})`],
                [t('Bác sĩ đang công tác / có ca', 'Active doctors / with shifts'), `${tq.bacSiDangCongTac} / ${tq.bacSiCoCa}`],
                [t('Tài khoản bệnh nhân mới', 'New patient accounts'), tq.taiKhoanBenhNhanMoi]]),
            the('h3', t('Trạng thái', 'Status')),
            thongTin([[t('Chưa khám', 'Not yet seen'), `${tt.chuaKham}${tt.canDoiLich ? t(` (${tt.canDoiLich} cần đổi lịch)`, ` (${tt.canDoiLich} need a reschedule)`) : ''}`],
                [t('Đã khám', 'Completed'), tt.daKham], [t('Không đến', 'No-show'), tt.khongDen], [t('Bệnh nhân hủy', 'Cancelled by patient'), tt.benhNhanHuy],
                [t('Bác sĩ hủy', 'Cancelled by doctor'), tt.bacSiHuy], [t('Phòng khám hủy', 'Cancelled by the clinic'), tt.quanTriHuy]]),
            the('h3', t('Kênh đặt và thời gian đặt trước', 'Booking channel and lead time')),
            thongTin([[KENH_DAT.KHACH, kd.khach], [KENH_DAT.TAI_KHOAN, kd.taiKhoan], [KENH_DAT.HOTLINE, kd.hotline],
                [t('Đặt trước trung bình', 'Average lead time'), tg.trungBinhGio === null ? '—' : t(`${tg.trungBinhGio} giờ`, `${tg.trungBinhGio} hours`)],
                [t('Dưới 1 ngày / 1–3 / 4–7 / 8–14 / trên 14 ngày', 'Under 1 day / 1–3 / 4–7 / 8–14 / over 14 days'),
                    `${tg.duoi1Ngay} / ${tg.tu1Den3Ngay} / ${tg.tu4Den7Ngay} / ${tg.tu8Den14Ngay} / ${tg.tren14Ngay}`]]),
            the('h3', t('Lượt khám / lịch hẹn theo chuyên khoa', 'Visits / appointments by specialty')),
            bieuDoCot(bc.theoChuyenKhoa),
            the('h3', t(`Theo thời gian (mỗi điểm 1 ${donVi})`, `Over time (one point per ${donVi})`)),
            ghiChu(t('Xanh: lịch hẹn · lục: lượt khám · xám: đã hủy', 'Blue: appointments · green: visits · grey: cancelled')),
            bieuDoDuong(bc.theoThoiGian),
            the('h3', t('Theo bác sĩ', 'By doctor')),
            bang([t('Bác sĩ', 'Doctor'), t('Chuyên khoa', 'Specialty'), t('Lịch hẹn', 'Appointments'), t('Lượt khám', 'Visits'), t('Bệnh nhân', 'Patients'),
                t('Đã hủy', 'Cancelled'), t('Không đến', 'No-show'), t('Tỷ lệ hủy', 'Cancellation rate'), t('Tỷ lệ không đến', 'No-show rate'), t('Lấp đầy', 'Utilisation')],
            bc.theoBacSi.map((d) => dong([`${d.hoTen} (${d.maBacSi})`, d.tenChuyenKhoa, d.chiSo.lichHen, d.chiSo.luotKham, d.chiSo.benhNhanDaKham,
                d.chiSo.daHuy, d.chiSo.khongDen, phanTram(d.chiSo.tyLeHuy), phanTram(d.chiSo.tyLeKhongDen),
                `${phanTram(d.chiSo.tyLeLapDay)} (${d.chiSo.luotDaDat}/${d.chiSo.tongLuot})`]))));
    }

    dangKy({
        duongDan: '/admin/reports',
        vaiTro: 'QUAN_TRI_VIEN',
        tieuDe: cap('Báo cáo thống kê', 'Reports'),
        async ve(khung) {
            khuon(khung, `
                <section>
                    <form id="form-bao-cao" class="hai-cot">
                        <div><label>${t('Kỳ', 'Period')}</label><select name="ky">
                            <option value="NGAY">${t('Ngày', 'Day')}</option><option value="TUAN">${t('Tuần (T2–CN)', 'Week (Mon–Sun)')}</option>
                            <option value="THANG" selected>${t('Tháng', 'Month')}</option><option value="NAM">${t('Năm', 'Year')}</option>
                            <option value="TUY_CHON">${t('Tùy chọn (tối đa 366 ngày)', 'Custom (at most 366 days)')}</option></select></div>
                        <div id="bc-o-ngay"><label>${t('Ngày trong kỳ (mặc định hôm nay)', 'A day in the period (default today)')}</label><input name="ngay" type="date"></div>
                        <div id="bc-o-tu" class="an"><label>${t('Từ ngày', 'From')}</label><input name="tuNgay" type="date"></div>
                        <div id="bc-o-den" class="an"><label>${t('Đến ngày', 'To')}</label><input name="denNgay" type="date"></div>
                        <div><label>${t('Chuyên khoa', 'Specialty')}</label><select name="idChuyenKhoa" id="bc-ck"><option value="">${t('(toàn viện)', '(whole hospital)')}</option></select></div>
                        <button>${t('Xem báo cáo', 'View report')}</button>
                        <button type="button" id="bc-excel" class="phu">${t('Xuất Excel', 'Export Excel')}</button>
                    </form>
                    <p id="bc-trang-thai" class="ghi-chu"></p>
                </section>
                <section id="bc-noi-dung"></section>`);
            const f = $('form-bao-cao');
            f.elements.ky.addEventListener('change', () => {
                const tuyChon = f.elements.ky.value === 'TUY_CHON';
                $('bc-o-ngay').classList.toggle('an', tuyChon);
                $('bc-o-tu').classList.toggle('an', !tuyChon);
                $('bc-o-den').classList.toggle('an', !tuyChon);
            });
            lay('/api/catalog/chuyen-khoa?kichThuoc=100', true).then(({ status, json }) => {
                if (status !== 200 || !$('bc-ck')) return;
                for (const ck of json.duLieu.noiDung) {
                    const lua = the('option', ck.tenChuyenKhoa);
                    lua.value = ck.id;
                    $('bc-ck').append(lua);
                }
            });
            f.addEventListener('submit', (e) => {
                e.preventDefault();
                taiBaoCao();
            });
            $('bc-excel').addEventListener('click', async () => {
                const { status, json } = await taiTep(`${BAO_CAO}/xuat-excel?${thamSo()}`, 'bao-cao-thong-ke.xlsx');
                bao('bc-trang-thai', status === 200 ? t('Đã tải tệp Excel.', 'Excel file downloaded.') : thongDiepLoi(json, t('Không xuất được.', 'Could not export.')),
                    status === 200 ? 'tot' : 'loi');
            });
            await taiBaoCao(true);
        },
    });
})();
