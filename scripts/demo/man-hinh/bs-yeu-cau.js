// Màn hình UI: Appointment Requests (Doctor) — danh sách yêu cầu đặt lịch với nút Accept / Decline / Open record
// Đường dẫn:  /doctor/appointment-requests   (ảnh chụp UI không có thanh địa chỉ; đường dẫn này do backend chọn, frontend xác nhận lại)
// Vai trò:    BAC_SI
// API:
//   GET  /api/booking/bac-si/toi/lich-hen/yeu-cau?trang=&kichThuoc=   lịch hẹn CHO_XAC_NHAN của tôi mà lượt khám chưa bắt đầu,
//        giờ khám sớm nhất trước (phân trang; tongSoPhanTu = số yêu cầu đang chờ)
//   POST /api/booking/bac-si/toi/lich-hen/{id}/xac-nhan               Accept: CHO_XAC_NHAN -> DA_XAC_NHAN
//   POST /api/booking/bac-si/toi/lich-hen/{id}/tu-choi  { lyDo }      Decline: CHO_XAC_NHAN -> BI_TU_CHOI, lý do bắt buộc (tối đa 500 ký tự),
//        lượt khám được mở lại cho người khác đặt; bệnh nhân đọc được lý do trên phiếu khám
//        Lỗi: 409 LICH_HEN_KHONG_CHO_XAC_NHAN (đã xử lý rồi), 409 LICH_HEN_DA_QUA_GIO (lượt khám đã bắt đầu), 404 (không phải lịch của tôi)
//   GET  /api/booking/bac-si/toi/lich-hen/lich?tuNgay=<hôm nay>&denNgay=<hôm nay + 41>   chỉ để hiện bảng "đã xác nhận, sắp tới"
// "Open record" -> /doctor/consultation?id=<id lịch hẹn>
(() => {
    const LICH_HEN = '/api/booking/bac-si/toi/lich-hen';
    const SO_NGAY = 42;
    let trang = 0, tongSoTrang = 0;

    // Tiêu đề cột dùng chung cho 2 bảng (gọi lúc vẽ để theo ngôn ngữ đang chọn)
    const cotBang = () => [t('Giờ khám dự kiến', 'Expected time'), t('STT', 'No.'), t('Bệnh nhân', 'Patient'), t('Tuổi', 'Age'),
        t('Lý do khám', 'Reason for visit'), t('Mã tra cứu', 'Lookup code'), t('Thao tác', 'Actions')];

    async function xuLy(lich, hanhDong) {
        let ketQua;
        if (hanhDong === 'xac-nhan') {
            ketQua = await goi(`${LICH_HEN}/${lich.id}/xac-nhan`);
        } else {
            const lyDo = $('yc-ly-do').value.trim();
            if (!lyDo) {
                bao('yc-trang-thai', t('Nhập lý do từ chối trước khi bấm Decline.', 'Enter the reason for declining before clicking Decline.'), 'loi');
                $('yc-ly-do').focus();
                return;
            }
            ketQua = await goi(`${LICH_HEN}/${lich.id}/tu-choi`, { lyDo });
        }
        const { status, json } = ketQua;
        bao('yc-trang-thai', status === 200 ? `${json.thongDiep}: ${lich.maTraCuu}` : thongDiepLoi(json, t('Không thực hiện được.', 'The action failed.')),
            status === 200 ? 'tot' : 'loi');
        if (status === 200 && hanhDong !== 'xac-nhan') $('yc-ly-do').value = '';
        // Lỗi 409 nghĩa là lịch hẹn đã đổi trạng thái ở nơi khác: tải lại để danh sách đúng
        if (status === 200 || status === 409) await Promise.all([taiYeuCau(true), taiDaXacNhan()]);
    }

    async function taiYeuCau(imLang = false) {
        const { status, json } = await lay(`${LICH_HEN}/yeu-cau?trang=${trang}&kichThuoc=10`, imLang);
        const khung = $('yc-ds');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được danh sách.', 'Could not load the list.')), 'ghi-chu loi'));
            return;
        }
        const duLieu = json.duLieu;
        // Trang cuối vừa hết dòng (đã xử lý hết): lùi về trang trước
        if (!duLieu.noiDung.length && trang > 0) {
            trang--;
            return taiYeuCau(imLang);
        }
        tongSoTrang = duLieu.tongSoTrang;
        $('yc-trang').textContent = t(`Trang ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} yêu cầu đang chờ`,
            `Page ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} pending requests`);
        if (!duLieu.noiDung.length) {
            khung.replaceChildren(ghiChu(t('Không có yêu cầu đặt lịch nào đang chờ.', 'No pending booking requests.')));
            return;
        }
        khung.replaceChildren(bang(cotBang(), duLieu.noiDung.map((lich) => dong([
            ngayGioVn(lich.gioKhamDuKien), lich.soThuTu, lich.benhNhan.hoTen + (lich.doiChieu.canDoiChieu ? t(' ⚠ cần đối chiếu', ' ⚠ identity check needed') : ''),
            lich.benhNhan.tuoi, lich.lyDoKham, lich.maTraCuu,
            [nut('Accept', () => xuLy(lich, 'xac-nhan')), nut('Decline', () => xuLy(lich, 'tu-choi'), 'nguy-hiem nho'),
                lienKet('Open record', '/doctor/consultation?id=' + lich.id)],
        ]))));
    }

    async function taiDaXacNhan() {
        const { status, json } = await lay(`${LICH_HEN}/lich?tuNgay=${congNgay(0)}&denNgay=${congNgay(SO_NGAY - 1)}`, true);
        const khung = $('yc-da-xac-nhan');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được danh sách.', 'Could not load the list.')), 'ghi-chu loi'));
            return;
        }
        const ds = json.duLieu.filter((lich) => lich.trangThai === 'DA_XAC_NHAN');
        if (!ds.length) {
            khung.replaceChildren(ghiChu(t('Chưa có lịch hẹn nào đã xác nhận.', 'No confirmed appointments yet.')));
            return;
        }
        khung.replaceChildren(bang(cotBang(), ds.map((lich) => dong([
            ngayGioVn(lich.gioKhamDuKien), lich.soThuTu, lich.benhNhan.hoTen, lich.benhNhan.tuoi, lich.lyDoKham, lich.maTraCuu,
            lienKet('Open record', '/doctor/consultation?id=' + lich.id),
        ]))));
    }

    dangKy({
        duongDan: '/doctor/appointment-requests',
        vaiTro: 'BAC_SI',
        tieuDe: cap('Yêu cầu đặt lịch', 'Appointment Requests'),
        async ve(khung) {
            trang = 0;
            khuon(khung, `
                <section>
                    <h2>${t('Yêu cầu đặt lịch đang chờ xác nhận', 'Booking requests pending confirmation')}</h2>
                    <p class="ghi-chu">${t('Chỉ xác nhận / từ chối được trước giờ khám. Lịch chưa kịp xác nhận vẫn khám được vào ngày khám.',
                        'Requests can only be confirmed or declined before the appointment time. An appointment not yet confirmed can still be seen on the day.')}</p>
                    <input id="yc-ly-do" placeholder="${t('Lý do từ chối (bắt buộc, nhập trước khi bấm Decline; bệnh nhân sẽ đọc được)',
                        'Reason for declining (required, enter it before clicking Decline; the patient will read it)')}" maxlength="500">
                    <div id="yc-ds" class="bang"></div>
                    <p id="yc-trang" class="ghi-chu"></p>
                    <button id="yc-truoc" class="phu nho">‹ ${t('Trang trước', 'Previous page')}</button>
                    <button id="yc-sau" class="phu nho">${t('Trang sau', 'Next page')} ›</button>
                    <p id="yc-trang-thai" class="ghi-chu"></p>
                </section>
                <section>
                    <h2>${t(`Đã xác nhận, trong ${SO_NGAY} ngày tới`, `Confirmed, in the next ${SO_NGAY} days`)}</h2>
                    <div id="yc-da-xac-nhan" class="bang"></div>
                </section>`);
            $('yc-truoc').addEventListener('click', () => {
                if (trang > 0) {
                    trang--;
                    taiYeuCau();
                }
            });
            $('yc-sau').addEventListener('click', () => {
                if (trang + 1 < tongSoTrang) {
                    trang++;
                    taiYeuCau();
                }
            });
            await Promise.all([taiYeuCau(true), taiDaXacNhan()]);
        },
    });
})();
