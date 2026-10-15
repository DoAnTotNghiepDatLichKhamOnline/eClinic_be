// Màn hình UI: Work Schedule (Doctor) — lịch làm việc theo ngày / tuần và nút "Request a shift change / leave"
// Đường dẫn:  /doctor/work-schedule
// Vai trò:    BAC_SI
// API:
//   GET /api/booking/bac-si/toi/lich-lam-viec?tuNgay=&denNgay=     các ca làm việc của tôi (kèm số lượt đã đặt / tổng số lượt)
//   GET /api/booking/bac-si/toi/lich-hen/lich?tuNgay=&denNgay=     lịch hẹn của tôi trong khoảng ngày (vẽ lên lịch)
// Bấm 1 lịch hẹn -> /doctor/consultation?id=<id lịch hẹn>; bấm 1 ca hoặc 1 ngày -> /doctor/consultation?ngay=&idLichLamViec=
//   POST /api/booking/bac-si/toi/yeu-cau-doi-lich                  { idLichLamViec, loaiYeuCau: XIN_NGHI | DOI_CA, lyDo,
//        ngayMongMuon?, gioBatDauMongMuon?, gioKetThucMongMuon?, idPhongKhamMongMuon? } -> 201. 3 trường ngày / giờ bắt buộc
//        với DOI_CA. Ca phải còn cách giờ bắt đầu ít nhất 24 giờ (409 QUA_HAN_GUI_YEU_CAU); mỗi ca 1 yêu cầu chờ duyệt
//        (409 CA_DA_CO_YEU_CAU_CHO_DUYET); ca đã hủy / đã bắt đầu -> 409 CA_KHONG_SUA_DUOC
//   GET  /api/booking/bac-si/toi/yeu-cau-doi-lich?trangThai=&trang=&kichThuoc=   yêu cầu tôi đã gửi, mới nhất trước
//        mỗi dòng: { id, loaiYeuCau, trangThai: CHO_DUYET | DA_DUYET | TU_CHOI | DA_RUT, lyDo, ca, ngayMongMuon, ...,
//        phongKhamMongMuon, ghiChuXuLy, ngayGui, ngayXuLy }
//   POST /api/booking/bac-si/toi/yeu-cau-doi-lich/{id}/rut          rút yêu cầu còn chờ duyệt -> DA_RUT
//   GET  /api/catalog/phong-kham?idChuyenKhoa=                      phòng khám đang hoạt động (chọn phòng mong muốn)
// Cần internet: thư viện lịch FullCalendar tải từ CDN.
(() => {
    function mauLichBacSi(lich) {
        if (KHONG_CON_KHAM.includes(lich.trangThai)) return MAU.daHuy;
        if (lich.trangThai === 'DA_HOAN_THANH') return MAU.daKham;
        return lich.doiChieu && lich.doiChieu.canDoiChieu ? MAU.doiChieu : MAU.cuaToi;
    }

    async function nguonCa(info, xong, hong) {
        const { status, json } = await lay(`/api/booking/bac-si/toi/lich-lam-viec?${khoangNgay(info)}`, true);
        if (status !== 200) {
            bao('bs-lich-trang-thai', thongDiepLoi(json, t('Không tải được lịch làm việc.', 'Could not load the work schedule.')), 'loi');
            hong(new Error('HTTP ' + status));
            return;
        }
        xong(json.duLieu.map((ca) => ({
            title: `${t('Ca', 'Shift')} ${ca.gioBatDau.slice(0, 5)}–${ca.gioKetThuc.slice(0, 5)} · ${ca.phongKham.tenPhong} · ${ca.soLuotDaDat}/${ca.tongSoLuot}`
                + (ca.trangThai === 'DA_HUY' ? t(' (đã hủy)', ' (cancelled)') : ''),
            start: `${ca.ngay}T${ca.gioBatDau}`,
            end: `${ca.ngay}T${ca.gioKetThuc}`,
            color: ca.trangThai === 'DA_HUY' ? MAU.daHuy : MAU.ca,
            display: 'block',
            extendedProps: { ca },
        })));
    }

    async function nguonLichHen(info, xong, hong) {
        const { status, json } = await lay(`/api/booking/bac-si/toi/lich-hen/lich?${khoangNgay(info)}`, true);
        if (status !== 200) {
            hong(new Error('HTTP ' + status));
            return;
        }
        xong(json.duLieu.map((lich) => ({
            title: `${t('STT', 'No.')} ${lich.soThuTu} · ${lich.benhNhan.hoTen}`,
            start: lich.gioKhamDuKien,
            color: mauLichBacSi(lich),
            extendedProps: { lich },
        })));
    }

    dangKy({
        duongDan: '/doctor/work-schedule',
        vaiTro: 'BAC_SI',
        tieuDe: cap('Lịch làm việc', 'Work Schedule'),
        async ve(khung) {
            khuon(khung, `
                <section>
                    <h2>${t('Lịch làm việc và lịch hẹn của tôi', 'My work schedule and appointments')}</h2>
                    <p class="chu-thich"><span><i style="background:#57606a"></i>${t('Ca làm việc (đã đặt / tổng số lượt)', 'Shift (booked / total visit slots)')}</span><span><i style="background:#1f6feb"></i>${t('Lịch hẹn chờ khám', 'Appointment waiting for the visit')}</span>
                        <span><i style="background:#1a7f37"></i>${t('Đã khám', 'Visit done')}</span><span><i style="background:#cf222e"></i>${t('Cần đối chiếu giấy tờ', 'Identity check needed')}</span><span><i style="background:#8c959f"></i>${t('Đã hủy', 'Cancelled')}</span></p>
                    <div id="lich-bs"></div>
                    <p id="bs-lich-trang-thai" class="ghi-chu">${t('Bấm vào 1 ngày hoặc 1 ca để xem danh sách bệnh nhân; bấm vào 1 lịch hẹn để mở hồ sơ khám.',
                        'Click a day or a shift to see the patient list; click an appointment to open the medical record.')}</p>
                </section>`);
            khuon(khung, `
                <section>
                    <h2>Request a shift change / leave</h2>
                    <p class="ghi-chu">${t(`Gửi trước giờ bắt đầu ca ít nhất 24 giờ. Quản trị viên duyệt thì ca bị hủy (xin nghỉ) hoặc được
                        chuyển sang ngày / giờ / phòng mong muốn (đổi ca); bệnh nhân đã đặt trong ca được báo để đổi lịch.`,
                        `Send it at least 24 hours before the shift starts. When the administrator approves, the shift is cancelled (leave) or
                        moved to the requested day / time / room (shift change); patients booked in the shift are told to reschedule.`)}</p>
                    <form id="form-yeu-cau-ca" class="hai-cot">
                        <div><label>${t('Ca của tôi (42 ngày tới)', 'My shifts (next 42 days)')}</label><select name="idLichLamViec" id="bs-yc-ca" required></select></div>
                        <div><label>${t('Loại', 'Type')}</label><select name="loaiYeuCau" id="bs-yc-loai"><option value="XIN_NGHI">${t('Xin nghỉ', 'Leave')}</option><option value="DOI_CA">${t('Đổi ca', 'Shift change')}</option></select></div>
                        <div class="ca-dong"><label>${t('Lý do', 'Reason')}</label><input name="lyDo" required maxlength="1000"></div>
                        <div class="bs-yc-doi an"><label>${t('Ngày mong muốn', 'Requested date')}</label><input name="ngayMongMuon" type="date"></div>
                        <div class="bs-yc-doi an"><label>${t('Giờ bắt đầu', 'Start time')}</label><input name="gioBatDauMongMuon" type="time"></div>
                        <div class="bs-yc-doi an"><label>${t('Giờ kết thúc', 'End time')}</label><input name="gioKetThucMongMuon" type="time"></div>
                        <div class="bs-yc-doi an"><label>${t('Phòng mong muốn', 'Requested room')}</label><select name="idPhongKhamMongMuon" id="bs-yc-phong"><option value="">${t('Giữ phòng hiện tại', 'Keep the current room')}</option></select></div>
                        <button>${t('Gửi yêu cầu', 'Send request')}</button>
                    </form>
                    <p id="bs-yc-trang-thai" class="ghi-chu"></p>
                    <h3>${t('Yêu cầu đã gửi', 'Sent requests')}</h3>
                    <div id="bs-yc-ds" class="bang"></div>
                </section>`);

            const YC = '/api/booking/bac-si/toi/yeu-cau-doi-lich';
            const moTaCa = (ca) => `${thuCuaNgay(ca.ngay)} ${ngayVn(ca.ngay)} ${ca.gioBatDau.slice(0, 5)}–${ca.gioKetThuc.slice(0, 5)} · ${tenPhong(ca.phongKham)}`;
            async function taiCaCuaToi() {
                const { status, json } = await lay(`/api/booking/bac-si/toi/lich-lam-viec?tuNgay=${congNgay(0)}&denNgay=${congNgay(41)}`, true);
                const o = $('bs-yc-ca');
                if (status !== 200 || !o) return;
                o.replaceChildren(...json.duLieu.filter((ca) => ca.trangThai === 'HOAT_DONG').map((ca) => {
                    const lua = the('option', `${moTaCa(ca)} · ${t('đã đặt', 'booked')} ${ca.soLuotDaDat}/${ca.tongSoLuot}`);
                    lua.value = ca.idLichLamViec;
                    return lua;
                }));
            }
            async function taiYeuCau() {
                const { status, json } = await lay(YC + '?kichThuoc=50', true);
                const ds = $('bs-yc-ds');
                if (!ds) return;
                if (status !== 200) {
                    ds.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được yêu cầu.', 'Could not load the requests.')), 'ghi-chu loi'));
                    return;
                }
                if (!json.duLieu.noiDung.length) {
                    ds.replaceChildren(ghiChu(t('Chưa gửi yêu cầu nào.', 'No requests sent yet.')));
                    return;
                }
                ds.replaceChildren(bang([t('Gửi lúc', 'Sent at'), t('Loại', 'Type'), t('Ca', 'Shift'), t('Mong muốn', 'Requested'), t('Lý do', 'Reason'),
                    t('Trạng thái', 'Status'), t('Ghi chú của quản trị viên', 'Administrator\'s note'), t('Thao tác', 'Actions')], json.duLieu.noiDung.map((yc) => dong([
                    ngayGioVn(yc.ngayGui), LOAI_YEU_CAU[yc.loaiYeuCau], moTaCa(yc.ca) + (yc.ca.trangThai === 'DA_HUY' ? t(' (đã hủy)', ' (cancelled)') : ''),
                    yc.ngayMongMuon ? `${ngayVn(yc.ngayMongMuon)} ${yc.gioBatDauMongMuon.slice(0, 5)}–${yc.gioKetThucMongMuon.slice(0, 5)}`
                        + (yc.phongKhamMongMuon ? ' · ' + tenPhong(yc.phongKhamMongMuon) : '') : null,
                    yc.lyDo, TRANG_THAI_YEU_CAU[yc.trangThai], yc.ghiChuXuLy,
                    yc.trangThai === 'CHO_DUYET' ? nut(t('Rút', 'Withdraw'), async () => {
                        const kq = await goi(`${YC}/${yc.id}/rut`);
                        bao('bs-yc-trang-thai', kq.status === 200 ? t('Đã rút yêu cầu.', 'Request withdrawn.')
                            : thongDiepLoi(kq.json, t('Không rút được.', 'Could not withdraw the request.')), kq.status === 200 ? 'tot' : 'loi');
                        taiYeuCau();
                    }, 'phu nho') : null,
                ]))));
            }
            $('bs-yc-loai').addEventListener('change', (e) => {
                document.querySelectorAll('.bs-yc-doi').forEach((o) => o.classList.toggle('an', e.target.value !== 'DOI_CA'));
            });
            khiGui('form-yeu-cau-ca', async (v, form) => {
                const than = thanForm(form, v.loaiYeuCau === 'DOI_CA'
                    ? ['idLichLamViec', 'loaiYeuCau', 'lyDo', 'ngayMongMuon', 'gioBatDauMongMuon', 'gioKetThucMongMuon', 'idPhongKhamMongMuon']
                    : ['idLichLamViec', 'loaiYeuCau', 'lyDo']);
                const { status, json } = await goi(YC, than);
                bao('bs-yc-trang-thai', status === 201 ? t('Đã gửi yêu cầu, chờ quản trị viên duyệt.', 'Request sent, waiting for the administrator to approve.')
                    : thongDiepLoi(json, t('Không gửi được.', 'Could not send the request.')), status === 201 ? 'tot' : 'loi');
                if (status === 201) taiYeuCau();
            });
            lay('/api/catalog/phong-kham', true).then(({ status, json }) => {
                if (status !== 200 || !$('bs-yc-phong')) return;
                for (const p of json.duLieu) {
                    const lua = the('option', `${tenPhong(p)} · ${p.tenChuyenKhoa}`);
                    lua.value = p.id;
                    $('bs-yc-phong').append(lua);
                }
            });
            taiCaCuaToi();
            taiYeuCau();

            const xemNgay = (ngay, idLichLamViec) => diToi(`/doctor/consultation?ngay=${ngay}` + (idLichLamViec ? `&idLichLamViec=${idLichLamViec}` : ''));
            const lich = await taoLich($('lich-bs'), [nguonCa, nguonLichHen], {
                // UI có 2 chế độ ngày / tuần; giữ thêm tháng để dễ thấy dữ liệu mẫu
                initialView: 'timeGridWeek',
                headerToolbar: { left: 'prev,next today', center: 'title', right: 'timeGridDay,timeGridWeek,dayGridMonth' },
                eventClick: (info) => {
                    const { ca, lich: lichHen } = info.event.extendedProps;
                    if (lichHen) diToi('/doctor/consultation?id=' + lichHen.id);
                    else if (ca) xemNgay(ca.ngay, ca.idLichLamViec);
                },
                dateClick: (info) => xemNgay(info.dateStr.slice(0, 10)),
            });
            return () => lich && lich.destroy();
        },
    });
})();
