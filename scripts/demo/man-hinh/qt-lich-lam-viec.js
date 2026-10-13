// Màn hình UI: Schedule Management (Admin) — lịch làm việc toàn viện
// Đường dẫn:  /admin/schedule-management
// Vai trò:    QUAN_TRI_VIEN
// API:
//   GET  /api/booking/quan-tri/lich-lam-viec?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=&idPhongKham=   các ca trong khoảng ngày
//        (tối đa 42 ngày, kể cả ca đã huỷ) kèm tongSoLuot / soLuotDaDat / soLuotConTrong
//   GET  /api/booking/quan-tri/lich-lam-viec/{id}/lich-hen     lịch hẹn của 1 ca (mọi trạng thái) kèm thông tin bệnh nhân
//   GET  /api/booking/quan-tri/lich-hen/tra-cuu?ma=            tra 1 lịch hẹn theo mã tra cứu (ECL-…) hoặc mã phiếu khám
//   POST /api/booking/quan-tri/lich-hen/{id}/da-doi-chieu      bỏ đánh dấu "cần đối chiếu" sau khi đã đối chiếu giấy tờ
//   GET  /api/catalog/chuyen-khoa?kichThuoc=100, GET /api/catalog/bac-si?kichThuoc=100   chỉ để đổ 2 ô lọc
//   POST /api/booking/quan-tri/lich-lam-viec                   xếp ca { idBacSi, idPhongKham, ngay, gioBatDau, gioKetThuc,
//        soLuotToiDaMoiGio (N), thoiLuongLuotPhut (t), lapLai?: { cacThu: [1..7] (1 = thứ Hai), denNgay } } -> 201
//        { daTao: [ca], boQua: [{ ngay, lyDo }] }. N x t <= 60; phòng phải thuộc chuyên khoa của bác sĩ; trùng giờ với ca
//        khác của bác sĩ / phòng -> 409 TRUNG_LICH_LAM_VIEC (có lapLai thì ngày trùng nằm trong boQua)
//   PUT  /api/booking/quan-tri/lich-lam-viec/{id}              sửa { idPhongKham, gioBatDau, gioKetThuc, soLuotToiDaMoiGio,
//        thoiLuongLuotPhut } của ca chưa bắt đầu. Đổi phòng luôn được; đổi giờ / sức chứa làm mất lượt đã có người đặt
//        -> 409 CA_CON_LICH_HEN; ca đã bắt đầu / đã hủy -> 409 CA_KHONG_SUA_DUOC
//   POST /api/booking/quan-tri/lich-lam-viec/{id}/huy          { lyDo } -> { ca, soLichHenCanDoi }: lịch hẹn còn hiệu lực
//        của ca được giữ và đánh dấu canDoiLich, bệnh nhân được báo
//   GET  /api/booking/quan-tri/lich-lam-viec/can-doi-lich      lịch hẹn đang chờ bệnh nhân đổi lịch (TrangDuLieu)
//   GET  /api/booking/quan-tri/yeu-cau-doi-lich?trangThai=&tatCa=&trang=&kichThuoc=   yêu cầu đổi ca / xin nghỉ (mặc định
//        CHO_DUYET), mỗi dòng kèm ca và ca.soLuotDaDat
//   POST /api/booking/quan-tri/yeu-cau-doi-lich/{id}/duyet     { ghiChu? }: xin nghỉ -> hủy ca; đổi ca -> hủy ca cũ + xếp ca
//        mới (chỉ khác phòng thì đổi phòng tại chỗ); ca mới trùng giờ -> 409 TRUNG_LICH_LAM_VIEC, yêu cầu vẫn chờ duyệt
//   POST /api/booking/quan-tri/yeu-cau-doi-lich/{id}/tu-choi   { ghiChu } bắt buộc
//   GET  /api/catalog/phong-kham                               phòng khám đang hoạt động (chọn khi xếp / sửa ca)
(() => {
    const CA = '/api/booking/quan-tri/lich-lam-viec';
    const LICH_HEN = '/api/booking/quan-tri/lich-hen';

    // Bảng lịch hẹn (dùng cho cả "lịch hẹn của ca" và kết quả tra cứu theo mã)
    function bangLichHen(ds, taiLai) {
        return bang([t('STT', 'No.'), t('Giờ khám', 'Visit time'), t('Bệnh nhân', 'Patient'), t('Tuổi', 'Age'), t('SĐT', 'Phone'), t('Bác sĩ', 'Doctor'), t('Trạng thái', 'Status'), t('Mã tra cứu', 'Lookup code'), t('Thao tác', 'Actions')], ds.map((lich) => {
            const thaoTac = [lienKet(t('Hồ sơ bệnh nhân', 'Patient profile'), '/admin/patients?id=' + lich.idHoSoBenhNhan)];
            if (lich.doiChieu.canDoiChieu) {
                thaoTac.push(nut(t('Đã đối chiếu', 'Identity checked'), async () => {
                    const { status } = await goi(`${LICH_HEN}/${lich.id}/da-doi-chieu`);
                    if (status === 200) await taiLai();
                }));
            }
            return dong([
                lich.soThuTu, ngayGioVn(lich.gioKhamDuKien), lich.benhNhan.hoTen + (lich.doiChieu.canDoiChieu ? t(' ⚠ cần đối chiếu', ' ⚠ identity check needed') : ''),
                lich.benhNhan.tuoi, lich.benhNhan.soDienThoai, tenBacSi(lich.bacSi), TRANG_THAI[lich.trangThai] || lich.trangThai,
                lich.maTraCuu, thaoTac,
            ]);
        }));
    }

    async function xemLichHenCuaCa(id) {
        const { status, json } = await lay(`${CA}/${id}/lich-hen`);
        const khung = $('qt-lich-hen-ca');
        if (!khung) return;
        $('qt-khoi-lich-hen-ca').classList.remove('an');
        $('qt-tieu-de-ca').textContent = t(`Lịch hẹn của ca #${id}`, `Appointments of shift #${id}`);
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được lịch hẹn của ca.', 'Could not load the appointments of the shift.')), 'ghi-chu loi'));
        } else if (!json.duLieu.length) {
            khung.replaceChildren(ghiChu(t('Ca này chưa có lịch hẹn nào.', 'This shift has no appointments yet.')));
        } else {
            khung.replaceChildren(bangLichHen(json.duLieu, () => xemLichHenCuaCa(id)));
        }
        $('qt-khoi-lich-hen-ca').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    const YEU_CAU = '/api/booking/quan-tri/yeu-cau-doi-lich';
    const gioPhut = (g) => g.slice(0, 5);
    const moTaCa = (ca) => `${thuCuaNgay(ca.ngay)} ${ngayVn(ca.ngay)} ${gioPhut(ca.gioBatDau)}–${gioPhut(ca.gioKetThuc)} · ${tenPhong(ca.phongKham)}`;
    function ketQua(id, kq, tot, hong) {
        const duoc = kq.status === 200 || kq.status === 201;
        bao(id, duoc ? tot : thongDiepLoi(kq.json, hong), duoc ? 'tot' : 'loi');
        return duoc;
    }

    // Đưa 1 ca vào form sửa
    function moSuaCa(ca) {
        const form = $('form-sua-ca');
        $('qt-khoi-sua-ca').classList.remove('an');
        $('qt-tieu-de-sua-ca').textContent = t(`Sửa ca #${ca.idLichLamViec}: ${tenBacSi(ca.bacSi)}, ${moTaCa(ca)} (đã đặt ${ca.soLuotDaDat}/${ca.tongSoLuot})`,
            `Edit shift #${ca.idLichLamViec}: ${tenBacSi(ca.bacSi)}, ${moTaCa(ca)} (booked ${ca.soLuotDaDat}/${ca.tongSoLuot})`);
        form.dataset.id = ca.idLichLamViec;
        form.elements.idPhongKham.value = ca.phongKham.id;
        form.elements.gioBatDau.value = gioPhut(ca.gioBatDau);
        form.elements.gioKetThuc.value = gioPhut(ca.gioKetThuc);
        form.elements.soLuotToiDaMoiGio.value = ca.soLuotToiDaMoiGio;
        form.elements.thoiLuongLuotPhut.value = ca.thoiLuongLuotPhut;
        bao('qt-sua-ca-trang-thai', '');
        $('qt-khoi-sua-ca').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    async function huyCa(ca) {
        const lyDo = prompt(t(`Hủy ca #${ca.idLichLamViec} (${moTaCa(ca)})? ${ca.soLuotDaDat} lượt đã có người đặt sẽ được đánh dấu cần đổi lịch.\nNhập lý do:`,
            `Cancel shift #${ca.idLichLamViec} (${moTaCa(ca)})? ${ca.soLuotDaDat} booked visit slots will be marked as needing a reschedule.\nEnter the reason:`));
        if (!lyDo || !lyDo.trim()) return;
        const kq = await goi(`${CA}/${ca.idLichLamViec}/huy`, { lyDo: lyDo.trim() });
        if (ketQua('qt-ca-trang-thai', kq, kq.json && kq.json.duLieu ? t(`Đã hủy ca. ${kq.json.duLieu.soLichHenCanDoi} lịch hẹn cần đổi lịch.`,
            `Shift cancelled. ${kq.json.duLieu.soLichHenCanDoi} appointments need a reschedule.`) : '', t('Không hủy được ca.', 'Could not cancel the shift.'))) {
            taiCa();
            taiCanDoi();
            taiYeuCau();
        }
    }

    async function taiYeuCau() {
        const khung = $('qt-ds-yeu-cau');
        if (!khung) return;
        const { status, json } = await lay(`${YEU_CAU}?kichThuoc=50&tatCa=${$('qt-yc-tat-ca').checked}`, true);
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được yêu cầu.', 'Could not load the requests.')), 'ghi-chu loi'));
            return;
        }
        if (!json.duLieu.noiDung.length) {
            khung.replaceChildren(ghiChu(t('Không có yêu cầu nào.', 'No requests.')));
            return;
        }
        const xuLy = async (yc, hanhDong, canGhiChu) => {
            const ghiChu = prompt(hanhDong === 'duyet'
                ? t(`Duyệt yêu cầu #${yc.id}? Ca đang có ${yc.ca.soLuotDaDat} lượt đã đặt.\nGhi chú (không bắt buộc):`,
                    `Approve request #${yc.id}? The shift has ${yc.ca.soLuotDaDat} booked visit slots.\nNote (optional):`)
                : t(`Từ chối yêu cầu #${yc.id}. Ghi chú (bắt buộc):`, `Reject request #${yc.id}. Note (required):`));
            if (ghiChu === null || (canGhiChu && !ghiChu.trim())) return;
            const kq = await goi(`${YEU_CAU}/${yc.id}/${hanhDong}`, { ghiChu: ghiChu.trim() || null });
            ketQua('qt-yc-trang-thai', kq, hanhDong === 'duyet' ? t('Đã duyệt, lịch làm việc đã đổi theo.', 'Approved, the work schedule was updated.') : t('Đã từ chối.', 'Rejected.'),
                t('Không xử lý được yêu cầu.', 'Could not process the request.'));
            taiYeuCau();
            taiCa();
            taiCanDoi();
        };
        khung.replaceChildren(bang([t('Gửi lúc', 'Sent at'), t('Bác sĩ', 'Doctor'), t('Loại', 'Type'), t('Ca', 'Shift'), t('Đã đặt', 'Booked'), t('Mong muốn', 'Requested'), t('Lý do', 'Reason'), t('Trạng thái', 'Status'), t('Ghi chú', 'Note'), t('Thao tác', 'Actions')], json.duLieu.noiDung.map((yc) => dong([
            ngayGioVn(yc.ngayGui), tenBacSi(yc.ca.bacSi), LOAI_YEU_CAU[yc.loaiYeuCau],
            moTaCa(yc.ca) + (yc.ca.trangThai === 'DA_HUY' ? t(' (đã hủy)', ' (cancelled)') : ''), `${yc.ca.soLuotDaDat} / ${yc.ca.tongSoLuot}`,
            yc.ngayMongMuon ? `${ngayVn(yc.ngayMongMuon)} ${gioPhut(yc.gioBatDauMongMuon)}–${gioPhut(yc.gioKetThucMongMuon)}`
                + (yc.phongKhamMongMuon ? ' · ' + tenPhong(yc.phongKhamMongMuon) : '') : null,
            yc.lyDo, TRANG_THAI_YEU_CAU[yc.trangThai], yc.ghiChuXuLy,
            yc.trangThai === 'CHO_DUYET' ? [nut(t('Duyệt', 'Approve'), () => xuLy(yc, 'duyet', false)), nut(t('Từ chối', 'Reject'), () => xuLy(yc, 'tu-choi', true), 'nguy-hiem nho')] : null,
        ]))));
    }

    async function taiCanDoi() {
        const khung = $('qt-ds-can-doi');
        if (!khung) return;
        const { status, json } = await lay(`${CA}/can-doi-lich?kichThuoc=50`, true);
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được danh sách.', 'Could not load the list.')), 'ghi-chu loi'));
            return;
        }
        khung.replaceChildren(json.duLieu.noiDung.length
            ? bangLichHen(json.duLieu.noiDung, taiCanDoi)
            : ghiChu(t('Không có lịch hẹn nào đang chờ đổi lịch.', 'No appointments are waiting for a reschedule.')));
    }

    async function taiCa() {
        const thamSo = new URLSearchParams(thanForm($('form-loc-ca'), ['tuNgay', 'denNgay', 'idChuyenKhoa', 'idBacSi', 'idPhongKham']));
        const { status, json } = await lay(`${CA}?${thamSo}`);
        const khung = $('qt-ds-ca');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được lịch làm việc.', 'Could not load the work schedule.')), 'ghi-chu loi'));
            return;
        }
        if (!json.duLieu.length) {
            khung.replaceChildren(ghiChu(t('Không có ca làm việc nào khớp bộ lọc.', 'No shifts match the filter.')));
            return;
        }
        khung.replaceChildren(bang([t('Ngày', 'Date'), t('Giờ', 'Time'), t('Bác sĩ', 'Doctor'), t('Chuyên khoa', 'Specialty'), t('Phòng', 'Room'), t('Đã đặt / tổng', 'Booked / total'), t('Còn trống', 'Free'), t('Trạng thái', 'Status'), t('Thao tác', 'Actions')], json.duLieu.map((ca) => dong([
            `${thuCuaNgay(ca.ngay)} ${ngayVn(ca.ngay)}`, `${ca.gioBatDau.slice(0, 5)}–${ca.gioKetThuc.slice(0, 5)}`, tenBacSi(ca.bacSi),
            ca.tenChuyenKhoa, tenPhong(ca.phongKham), `${ca.soLuotDaDat} / ${ca.tongSoLuot}`, ca.soLuotConTrong, ca.trangThai,
            [nut(t(`Lịch hẹn của ca #${ca.idLichLamViec}`, `Appointments of shift #${ca.idLichLamViec}`), () => xemLichHenCuaCa(ca.idLichLamViec)),
                // Chỉ ca còn hoạt động và chưa bắt đầu mới sửa / hủy được (API trả 409 CA_KHONG_SUA_DUOC nếu không)
                ...(ca.trangThai === 'HOAT_DONG' ? [nut(t('Sửa', 'Edit'), () => moSuaCa(ca), 'phu nho'), nut(t('Hủy ca', 'Cancel shift'), () => huyCa(ca), 'nguy-hiem nho')] : [])],
        ]))));
    }

    async function doLuaChon(idO, duongDan, nhan) {
        const { status, json } = await lay(duongDan, true);
        if (status !== 200 || !$(idO)) return;
        for (const muc of json.duLieu.noiDung) {
            const lua = the('option', nhan(muc));
            lua.value = muc.id;
            $(idO).append(lua);
        }
    }

    dangKy({
        duongDan: '/admin/schedule-management',
        vaiTro: 'QUAN_TRI_VIEN',
        tieuDe: cap('Quản lý lịch làm việc', 'Schedule Management'),
        async ve(khung) {
            khuon(khung, `
                <section>
                    <h2>${t('Xếp ca làm việc', 'Schedule shifts')}</h2>
                    <form id="form-tao-ca" class="hai-cot">
                        <div><label>${t('Bác sĩ', 'Doctor')}</label><select name="idBacSi" id="qt-tao-bac-si" required></select></div>
                        <div><label>${t('Phòng khám (cùng chuyên khoa với bác sĩ)', 'Room (same specialty as the doctor)')}</label><select name="idPhongKham" id="qt-tao-phong" required></select></div>
                        <div><label>${t('Ngày', 'Date')}</label><input name="ngay" type="date" required></div>
                        <div><label>${t('Giờ bắt đầu', 'Start time')}</label><input name="gioBatDau" type="time" value="08:00" required></div>
                        <div><label>${t('Giờ kết thúc', 'End time')}</label><input name="gioKetThuc" type="time" value="11:30" required></div>
                        <div><label>${t('Số lượt mỗi giờ (N)', 'Visit slots per hour (N)')}</label><input name="soLuotToiDaMoiGio" type="number" min="1" max="60" value="6" required></div>
                        <div><label>${t('Số phút mỗi lượt (t), N x t ≤ 60', 'Minutes per visit slot (t), N x t ≤ 60')}</label><input name="thoiLuongLuotPhut" type="number" min="1" max="60" value="10" required></div>
                        <div class="ca-dong"><label>${t('Lặp lại vào các thứ (bỏ trống = chỉ 1 ca)', 'Repeat on weekdays (none checked = a single shift)')}</label>
                            <span id="qt-tao-thu"></span></div>
                        <div><label>${t('Lặp lại đến ngày', 'Repeat until')}</label><input name="denNgay" type="date"></div>
                        <button>${t('Xếp ca', 'Schedule shift')}</button>
                    </form>
                    <p id="qt-tao-ca-trang-thai" class="ghi-chu"></p>
                </section>
                <section>
                    <h2>${t('Lịch làm việc toàn viện', 'Clinic-wide work schedule')}</h2>
                    <form id="form-loc-ca" class="hai-cot">
                        <div><label>${t('Từ ngày', 'From date')}</label><input name="tuNgay" type="date" required></div>
                        <div><label>${t('Đến ngày (tối đa 42 ngày)', 'To date (up to 42 days)')}</label><input name="denNgay" type="date" required></div>
                        <div><label>${t('Chuyên khoa', 'Specialty')}</label><select name="idChuyenKhoa" id="qt-loc-chuyen-khoa"><option value="">${t('Tất cả', 'All')}</option></select></div>
                        <div><label>${t('Bác sĩ', 'Doctor')}</label><select name="idBacSi" id="qt-loc-bac-si"><option value="">${t('Tất cả', 'All')}</option></select></div>
                        <div><label>${t('Phòng khám (id)', 'Room (id)')}</label><input name="idPhongKham" type="number" min="1"></div>
                        <button>${t('Xem', 'View')}</button>
                    </form>
                    <p id="qt-ca-trang-thai" class="ghi-chu"></p>
                    <div id="qt-ds-ca" class="bang"></div>
                </section>
                <section id="qt-khoi-sua-ca" class="an">
                    <h2 id="qt-tieu-de-sua-ca">${t('Sửa ca', 'Edit shift')}</h2>
                    <p class="ghi-chu">${t('Đổi phòng luôn được (bệnh nhân đã đặt được báo). Đổi giờ / sức chứa chỉ được khi mọi lượt đã có người đặt còn nguyên.',
                        'The room can always be changed (patients who booked are notified). The time / capacity can only be changed when every booked visit slot stays intact.')}</p>
                    <form id="form-sua-ca" class="hai-cot">
                        <div><label>${t('Phòng khám', 'Room')}</label><select name="idPhongKham" id="qt-sua-phong" required></select></div>
                        <div><label>${t('Giờ bắt đầu', 'Start time')}</label><input name="gioBatDau" type="time" required></div>
                        <div><label>${t('Giờ kết thúc', 'End time')}</label><input name="gioKetThuc" type="time" required></div>
                        <div><label>${t('Số lượt mỗi giờ (N)', 'Visit slots per hour (N)')}</label><input name="soLuotToiDaMoiGio" type="number" min="1" max="60" required></div>
                        <div><label>${t('Số phút mỗi lượt (t)', 'Minutes per visit slot (t)')}</label><input name="thoiLuongLuotPhut" type="number" min="1" max="60" required></div>
                        <button>${t('Lưu ca', 'Save shift')}</button>
                    </form>
                    <p id="qt-sua-ca-trang-thai" class="ghi-chu"></p>
                </section>
                <section>
                    <h2>${t('Yêu cầu đổi ca / xin nghỉ của bác sĩ', 'Doctors\' shift-change / leave requests')}</h2>
                    <label><input type="checkbox" id="qt-yc-tat-ca"> ${t('Hiện cả yêu cầu đã xử lý', 'Also show handled requests')}</label>
                    <p id="qt-yc-trang-thai" class="ghi-chu"></p>
                    <div id="qt-ds-yeu-cau" class="bang"></div>
                </section>
                <section>
                    <h2>${t('Lịch hẹn cần đổi lịch (ca khám đã bị hủy)', 'Appointments that need a reschedule (shift was cancelled)')}</h2>
                    <p class="ghi-chu">${t('Bệnh nhân đã được báo trong ứng dụng; khách đặt không đăng nhập thì phòng khám cần gọi điện.',
                        'Patients were notified in the app; for guests who booked without signing in, the clinic needs to call them.')}</p>
                    <div id="qt-ds-can-doi" class="bang"></div>
                </section>
                <section id="qt-khoi-lich-hen-ca" class="an">
                    <h2 id="qt-tieu-de-ca">${t('Lịch hẹn của ca', 'Appointments of the shift')}</h2>
                    <div id="qt-lich-hen-ca" class="bang"></div>
                </section>
                <section>
                    <h2>${t('Tra lịch hẹn theo mã', 'Look up an appointment by code')}</h2>
                    <form id="form-tra-cuu" class="hai-cot">
                        <div><label>${t('Mã tra cứu (ECL-…) hoặc mã phiếu khám', 'Lookup code (ECL-…) or ticket code')}</label><input name="ma" required></div>
                        <button>${t('Tra cứu', 'Look up')}</button>
                    </form>
                    <div id="qt-tra-cuu" class="bang"></div>
                </section>`);
            const loc = $('form-loc-ca');
            loc.elements.tuNgay.value = congNgay(0);
            loc.elements.denNgay.value = congNgay(6);
            loc.addEventListener('submit', (e) => { e.preventDefault(); taiCa(); });
            khiGui('form-tra-cuu', async function traCuu(v) {
                const { status, json } = await lay(`${LICH_HEN}/tra-cuu?ma=${encodeURIComponent(v.ma.trim())}`);
                $('qt-tra-cuu').replaceChildren(status === 200
                    ? bangLichHen([json.duLieu], () => traCuu(v))
                    : the('p', thongDiepLoi(json, t('Không tìm thấy lịch hẹn.', 'Appointment not found.')), 'ghi-chu loi'));
            });
            // Xếp ca
            const taoCa = $('form-tao-ca');
            taoCa.elements.ngay.value = congNgay(1);
            [THU[1], THU[2], THU[3], THU[4], THU[5], THU[6], THU[0]].forEach((ten, i) => {
                const nhan = the('label', null, 'tren-dong');
                const o = the('input');
                o.type = 'checkbox';
                o.name = 'thu';
                o.value = i + 1;
                nhan.append(o, ' ' + ten + ' ');
                $('qt-tao-thu').append(nhan);
            });
            khiGui('form-tao-ca', async (v, form) => {
                const than = thanForm(form, ['idBacSi', 'idPhongKham', 'ngay', 'gioBatDau', 'gioKetThuc', 'soLuotToiDaMoiGio', 'thoiLuongLuotPhut']);
                const cacThu = [...form.querySelectorAll('input[name=thu]:checked')].map((o) => Number(o.value));
                if (cacThu.length || v.denNgay) than.lapLai = { cacThu, denNgay: v.denNgay || null };
                const kq = await goi(CA, than);
                const dl = kq.json && kq.json.duLieu;
                if (ketQua('qt-tao-ca-trang-thai', kq, dl ? t(`Đã xếp ${dl.daTao.length} ca.`, `Scheduled ${dl.daTao.length} shift(s).`)
                    + (dl.boQua.length ? t(' Bỏ qua: ', ' Skipped: ') + dl.boQua.map((b) => `${ngayVn(b.ngay)} (${b.lyDo})`).join('; ') : '') : '', t('Không xếp được ca.', 'Could not schedule the shift.'))) taiCa();
            });
            khiGui('form-sua-ca', async (v, form) => {
                const kq = await goi(`${CA}/${form.dataset.id}`, thanForm(form, ['idPhongKham', 'gioBatDau', 'gioKetThuc', 'soLuotToiDaMoiGio', 'thoiLuongLuotPhut']), 'PUT');
                if (ketQua('qt-sua-ca-trang-thai', kq, t('Đã lưu ca.', 'Shift saved.'), t('Không sửa được ca.', 'Could not edit the shift.'))) taiCa();
            });
            lay('/api/catalog/phong-kham', true).then(({ status, json }) => {
                if (status !== 200) return;
                for (const idO of ['qt-tao-phong', 'qt-sua-phong']) {
                    if (!$(idO)) return;
                    for (const p of json.duLieu) {
                        const lua = the('option', `${tenPhong(p)} · ${p.tenChuyenKhoa}`);
                        lua.value = p.id;
                        $(idO).append(lua);
                    }
                }
            });
            doLuaChon('qt-tao-bac-si', '/api/catalog/bac-si?kichThuoc=100', (bs) => `${tenBacSi(bs)} · ${bs.tenChuyenKhoa}`);
            $('qt-yc-tat-ca').addEventListener('change', taiYeuCau);
            taiYeuCau();
            taiCanDoi();
            doLuaChon('qt-loc-chuyen-khoa', '/api/catalog/chuyen-khoa?kichThuoc=100', (ck) => ck.tenChuyenKhoa);
            doLuaChon('qt-loc-bac-si', '/api/catalog/bac-si?kichThuoc=100', (bs) => `${tenBacSi(bs)} · ${bs.tenChuyenKhoa}`);
            await taiCa();
        },
    });
})();
