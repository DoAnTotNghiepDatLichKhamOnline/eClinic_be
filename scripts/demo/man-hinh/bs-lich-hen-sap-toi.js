// Màn hình UI: Upcoming Appointments (Doctor) — thay màn hình Appointment Requests (DOANTOTNGH-11): lịch hẹn được xác nhận ngay
//             khi đặt nên bác sĩ không còn duyệt; màn hình liệt kê lịch hẹn sắp tới với nút Cancel / Open record
// Đường dẫn:  /doctor/upcoming-appointments   (đường dẫn do backend chọn, frontend xác nhận lại)
// Vai trò:    BAC_SI
// API:
//   GET  /api/booking/bac-si/toi/lich-hen/lich?tuNgay=<hôm nay>&denNgay=<hôm nay + 41>   lịch hẹn của tôi theo ngày; màn hình chỉ giữ
//        lịch DA_XAC_NHAN, chưa bị đánh dấu cần đổi lịch và lượt khám chưa bắt đầu (cùng tập với lichHenSapToi trên Dashboard)
//   POST /api/booking/bac-si/toi/lich-hen/{id}/huy  { lyDo }   Cancel: DA_XAC_NHAN -> BI_TU_CHOI, lý do bắt buộc (tối đa 500 ký tự),
//        lượt khám được mở lại cho người khác đặt; bệnh nhân được báo (trong ứng dụng + email liên hệ) và đọc được lý do trên phiếu khám
//        Lỗi: 409 LICH_HEN_KHONG_HUY_DOI_DUOC (đã khám / đã hủy), 409 LICH_HEN_CAN_DOI_LICH (ca đã bị hủy, chờ bệnh nhân đổi),
//             409 LICH_HEN_DA_QUA_GIO (lượt khám đã bắt đầu), 404 (không phải lịch của tôi)
// "Open record" -> /doctor/consultation?id=<id lịch hẹn>
(() => {
    const LICH_HEN = '/api/booking/bac-si/toi/lich-hen';
    const SO_NGAY = 42;

    const cotBang = () => [t('Giờ khám dự kiến', 'Expected time'), t('STT', 'No.'), t('Bệnh nhân', 'Patient'), t('Tuổi', 'Age'),
        t('Lý do khám', 'Reason for visit'), t('Mã tra cứu', 'Lookup code'), t('Thao tác', 'Actions')];

    async function huy(lich) {
        const lyDo = $('st-ly-do').value.trim();
        if (!lyDo) {
            bao('st-trang-thai', t('Nhập lý do hủy trước khi bấm Cancel.', 'Enter the reason before clicking Cancel.'), 'loi');
            $('st-ly-do').focus();
            return;
        }
        const { status, json } = await goi(`${LICH_HEN}/${lich.id}/huy`, { lyDo });
        bao('st-trang-thai', status === 200 ? `${json.thongDiep}: ${lich.maTraCuu}` : thongDiepLoi(json, t('Không thực hiện được.', 'The action failed.')),
            status === 200 ? 'tot' : 'loi');
        if (status === 200) $('st-ly-do').value = '';
        // Lỗi 409 nghĩa là lịch hẹn đã đổi trạng thái ở nơi khác: tải lại để danh sách đúng
        if (status === 200 || status === 409) await taiSapToi();
    }

    async function taiSapToi() {
        const { status, json } = await lay(`${LICH_HEN}/lich?tuNgay=${congNgay(0)}&denNgay=${congNgay(SO_NGAY - 1)}`, true);
        const khung = $('st-ds');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được danh sách.', 'Could not load the list.')), 'ghi-chu loi'));
            return;
        }
        const bayGio = new Date();
        const ds = json.duLieu.filter((lich) => lich.trangThai === 'DA_XAC_NHAN' && !lich.canDoiLich
            && new Date(lich.gioKhamDuKien) > bayGio);
        if (!ds.length) {
            khung.replaceChildren(ghiChu(t('Không có lịch hẹn sắp tới.', 'No upcoming appointments.')));
            return;
        }
        khung.replaceChildren(bang(cotBang(), ds.map((lich) => dong([
            ngayGioVn(lich.gioKhamDuKien), lich.soThuTu, lich.benhNhan.hoTen + (lich.doiChieu.canDoiChieu ? t(' ⚠ cần đối chiếu', ' ⚠ identity check needed') : ''),
            lich.benhNhan.tuoi, lich.lyDoKham, lich.maTraCuu,
            [nut('Cancel', () => huy(lich), 'nguy-hiem nho'), lienKet('Open record', '/doctor/consultation?id=' + lich.id)],
        ]))));
    }

    dangKy({
        duongDan: '/doctor/upcoming-appointments',
        vaiTro: 'BAC_SI',
        tieuDe: cap('Lịch hẹn sắp tới', 'Upcoming Appointments'),
        async ve(khung) {
            khuon(khung, `
                <section>
                    <h2>${t(`Lịch hẹn sắp tới, trong ${SO_NGAY} ngày`, `Upcoming appointments, next ${SO_NGAY} days`)}</h2>
                    <p class="ghi-chu">${t('Lịch hẹn được xác nhận ngay khi bệnh nhân đặt. Chỉ hủy khi thật cần (vd đặt nhầm chuyên khoa), trước giờ khám; nghỉ cả ca thì gửi yêu cầu xin nghỉ ở Lịch làm việc.',
                        'Appointments are confirmed as soon as they are booked. Cancel only when needed (e.g. wrong specialty), before the visit time; to skip a whole shift, send a leave request from Work Schedule.')}</p>
                    <input id="st-ly-do" placeholder="${t('Lý do hủy (bắt buộc, nhập trước khi bấm Cancel; bệnh nhân sẽ đọc được)',
                        'Reason for cancelling (required, enter it before clicking Cancel; the patient will read it)')}" maxlength="500">
                    <div id="st-ds" class="bang"></div>
                    <p id="st-trang-thai" class="ghi-chu"></p>
                </section>`);
            await taiSapToi();
        },
    });
})();
