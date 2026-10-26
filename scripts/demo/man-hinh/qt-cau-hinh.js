// Màn hình ngoài danh sách UI: Quy tắc đặt lịch (SCHED-05, quy tắc #13) — quản trị viên sửa 10 quy tắc đặt lịch / xếp ca
// Đường dẫn:  /admin/booking-rules
// Vai trò:    QUAN_TRI_VIEN
// API:
//   GET /api/booking/quan-tri/cau-hinh            10 quy tắc đang dùng + phienBan, ngaySua, quanTriSua { id, hoTen }
//   PUT /api/booking/quan-tri/cau-hinh            cả 10 quy tắc + phienBan (giá trị vừa đọc). 400 khi ngoài khoảng cho phép
//        hoặc số lịch mỗi SĐT nhỏ hơn số lịch mỗi hồ sơ; 409 DU_LIEU_DA_THAY_DOI khi người khác vừa lưu (tải lại rồi sửa lại).
//        Áp dụng ngay cho thao tác MỚI (lịch hẹn, ca, đánh giá đã có không đổi); không đổi gì thì phiên bản giữ nguyên
//   GET /api/booking/quan-tri/cau-hinh/lich-su?trang=&kichThuoc=   mỗi trường đổi giá trị 1 dòng: truong, giaTriCu, giaTriMoi,
//        thoiDiem, quanTri, mới nhất trước
// Biến môi trường BOOKING_* / SHIFT_* / RATING_EDIT_DAYS chỉ là giá trị ban đầu của bảng cau_hinh_he_thong (lần chạy đầu).
(() => {
    const CAU_HINH = '/api/booking/quan-tri/cau-hinh';
    // [tên trường, nhãn, đơn vị, nhỏ nhất, lớn nhất]: khoảng cho phép đúng như CapNhatCauHinhRequest
    const TRUONG = () => [
        ['datTruocToiThieuPhut', t('Đặt trước giờ khám ít nhất', 'Book at least this long before the visit'), t('phút', 'minutes'), 0, 1440],
        ['soNgayDatTruocToiDa', t('Đặt trước tối đa', 'Book at most this far ahead'), t('ngày', 'days'), 1, 180],
        ['soLichHieuLucToiDaMoiHoSo', t('Số lịch chưa khám tối đa của 1 người khám', 'Upcoming appointments per patient, at most'), t('lịch', 'appointments'), 1, 20],
        ['soLichHieuLucToiDaMoiSoDienThoai', t('Số lịch chưa khám tối đa của 1 số điện thoại', 'Upcoming appointments per phone number, at most'), t('lịch', 'appointments'), 1, 50],
        ['huyDoiTruocToiThieuPhut', t('Bệnh nhân hủy / đổi lịch trước giờ khám ít nhất', 'Patients cancel / reschedule at least this long before'), t('phút', 'minutes'), 0, 10080],
        ['soLanDoiLichToiDa', t('Số lần đổi lịch tối đa của 1 lần đặt', 'Reschedules per booking, at most'), t('lần', 'times'), 0, 10],
        ['guiYeuCauTruocToiThieuGio', t('Bác sĩ gửi yêu cầu đổi ca / xin nghỉ trước ca ít nhất', 'Doctors send shift-change / leave requests at least'), t('giờ', 'hours'), 0, 168],
        ['soNgayXepTruocToiDa', t('Xếp ca trước tối đa', 'Schedule shifts at most this far ahead'), t('ngày', 'days'), 7, 365],
        ['soGioToiDaMoiTuan', t('Giờ làm tối đa của 1 bác sĩ mỗi tuần', 'Working hours per doctor per week, at most'), t('giờ', 'hours'), 1, 168],
        ['soNgayDuocSuaDanhGia', t('Bệnh nhân sửa đánh giá trong', 'Patients may edit a review within'), t('ngày', 'days'), 0, 60],
    ];
    let phienBan = null;
    let trangLs = 0, tongSoTrangLs = 0;

    function hien(ch) {
        phienBan = ch.phienBan;
        const f = $('form-cau-hinh');
        for (const [ten] of TRUONG()) f.elements[ten].value = ch[ten];
        bao('ch-phien-ban', t(`Phiên bản ${ch.phienBan}`, `Version ${ch.phienBan}`)
            + (ch.ngaySua ? t(` · sửa lần cuối ${ngayGioVn(ch.ngaySua)}`, ` · last changed ${ngayGioVn(ch.ngaySua)}`) : '')
            + (ch.quanTriSua ? t(` bởi ${ch.quanTriSua.hoTen}`, ` by ${ch.quanTriSua.hoTen}`) : ''));
    }

    async function taiCauHinh(imLang = false) {
        const { status, json } = await lay(CAU_HINH, imLang);
        if (!$('form-cau-hinh')) return;
        if (status !== 200) {
            bao('ch-trang-thai', thongDiepLoi(json, t('Không tải được quy tắc.', 'Could not load the rules.')), 'loi');
            return;
        }
        hien(json.duLieu);
    }

    async function taiLichSu(imLang = false) {
        const { status, json } = await lay(`${CAU_HINH}/lich-su?trang=${trangLs}&kichThuoc=20`, imLang);
        const khung = $('ch-lich-su');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được lịch sử.', 'Could not load the history.')), 'ghi-chu loi'));
            return;
        }
        const duLieu = json.duLieu;
        tongSoTrangLs = duLieu.tongSoTrang;
        $('ch-trang-ls').textContent = t(`Trang ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} lần đổi`,
            `Page ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} changes`);
        const nhanTruong = new Map(TRUONG().map(([ten, nhanTruong_]) => [ten, nhanTruong_]));
        khung.replaceChildren(duLieu.noiDung.length
            ? bang([t('Lúc', 'When'), t('Quy tắc', 'Rule'), t('Cũ', 'Old'), t('Mới', 'New'), t('Người sửa', 'Changed by')], duLieu.noiDung.map((ls) => dong([
                ngayGioVn(ls.thoiDiem), nhanTruong.get(ls.truong) || ls.truong, ls.giaTriCu, ls.giaTriMoi, ls.quanTri ? ls.quanTri.hoTen : '',
            ])))
            : ghiChu(t('Chưa sửa lần nào: đang dùng giá trị ban đầu từ biến môi trường.', 'Never changed: the initial values from the environment are in use.')));
    }

    dangKy({
        duongDan: '/admin/booking-rules',
        vaiTro: 'QUAN_TRI_VIEN',
        tieuDe: cap('Quy tắc đặt lịch', 'Booking Rules'),
        async ve(khung) {
            trangLs = 0;
            khuon(khung, `
                <section>
                    <p class="ghi-chu">${t('Giá trị mới áp dụng ngay cho lần đặt / đổi lịch / xếp ca / đánh giá tiếp theo; lịch hẹn và ca đã có không đổi. Bệnh nhân đọc được 5 quy tắc đặt lịch ở GET /api/booking/quy-tac-dat-lich.',
                        'New values apply at once to the next booking, reschedule, shift or review; existing appointments and shifts do not change. Patients can read the 5 booking rules at GET /api/booking/quy-tac-dat-lich.')}</p>
                    <form id="form-cau-hinh"><div class="hai-cot" id="ch-truong"></div>
                        <button>${t('Lưu quy tắc', 'Save the rules')}</button>
                        <button type="button" id="ch-tai-lai" class="phu">${t('Tải lại', 'Reload')}</button>
                    </form>
                    <p id="ch-phien-ban" class="ghi-chu"></p>
                    <p id="ch-trang-thai" class="ghi-chu"></p>
                </section>
                <section>
                    <h2>${t('Lịch sử sửa', 'Change history')}</h2>
                    <div id="ch-lich-su" class="bang"></div>
                    <p id="ch-trang-ls" class="ghi-chu"></p>
                    <button id="ch-ls-truoc" class="phu nho">${t('‹ Trang trước', '‹ Previous page')}</button>
                    <button id="ch-ls-sau" class="phu nho">${t('Trang sau ›', 'Next page ›')}</button>
                </section>`);
            for (const [ten, nhanTruong, donVi, nhoNhat, lonNhat] of TRUONG()) {
                const o = the('div');
                const nhanO = the('label', `${nhanTruong} (${donVi}; ${nhoNhat}–${lonNhat})`);
                const input = the('input');
                Object.assign(input, { name: ten, type: 'number', min: nhoNhat, max: lonNhat, step: 1, required: true });
                o.append(nhanO, input);
                $('ch-truong').append(o);
            }
            $('ch-tai-lai').addEventListener('click', () => taiCauHinh());
            $('ch-ls-truoc').addEventListener('click', () => { if (trangLs > 0) { trangLs--; taiLichSu(); } });
            $('ch-ls-sau').addEventListener('click', () => { if (trangLs + 1 < tongSoTrangLs) { trangLs++; taiLichSu(); } });
            khiGui('form-cau-hinh', async (v) => {
                const than = { phienBan };
                for (const [ten] of TRUONG()) than[ten] = Number(v[ten]);
                const { status, json } = await goi(CAU_HINH, than, 'PUT');
                if (status === 409) {
                    bao('ch-trang-thai', thongDiepLoi(json, '') + t(' — bấm "Tải lại" để xem giá trị mới nhất rồi sửa lại.',
                        ' — press "Reload" to see the latest values, then edit again.'), 'loi');
                    return;
                }
                if (status !== 200) {
                    bao('ch-trang-thai', thongDiepLoi(json, t('Không lưu được.', 'Could not save.')), 'loi');
                    return;
                }
                hien(json.duLieu);
                bao('ch-trang-thai', json.thongDiep, 'tot');
                trangLs = 0;
                await taiLichSu(true);
            });
            await taiCauHinh(true);
            await taiLichSu(true);
        },
    });
})();
