// Màn hình UI: Dashboard (Doctor) — 4 ô số liệu và bảng lịch hẹn hôm nay
// Đường dẫn:  /doctor/dashboard
// Vai trò:    BAC_SI
// API:
//   GET /api/booking/bac-si/toi/tong-quan                 4 ô số liệu trong 1 lần gọi: lichHenHomNay (không tính đã hủy / bị từ chối),
//                                                         dangCho (chờ xác nhận + đã xác nhận, chưa khám), daKham, diemDanhGia + soDanhGia;
//                                                         thêm yeuCauChoXacNhan (số trên nhãn Appointment Requests) và tinhLuc
//   GET /api/booking/bac-si/toi/lich-hen?ngay=<hôm nay>   lịch hẹn của tôi trong ngày, theo giờ khám
//   GET /api/booking/bac-si/toi/danh-gia/tong-quan        { diemTrungBinh (null khi chưa có), soDanhGia, phanBo: {1..5} }
//   GET /api/booking/bac-si/toi/danh-gia?trang=&kichThuoc=   đánh giá tôi nhận được, mới nhất trước: soSao, nhanXet, ngayKham, ngayTao
//                                                          (ẩn danh: không có tên bệnh nhân)
(() => {
    dangKy({
        duongDan: '/doctor/dashboard',
        vaiTro: 'BAC_SI',
        tieuDe: cap('Tổng quan', 'Dashboard'),
        async ve(khung) {
            const oSo = the('div', null, 'o-so');
            const danhSach = the('div', null, 'bang');
            const phanLich = the('section');
            phanLich.append(the('h2', t(`Lịch hẹn hôm nay (${ngayVn(congNgay(0))})`, `Today's appointments (${ngayVn(congNgay(0))})`)), danhSach);
            const dsDanhGia = the('div', null, 'bang');
            const phanDanhGia = the('section');
            phanDanhGia.append(the('h2', t('Đánh giá của bệnh nhân (ẩn danh)', 'Patient reviews (anonymous)')), dsDanhGia);
            const tinhLuc = ghiChu('');
            khung.append(oSo, tinhLuc, phanLich, phanDanhGia);

            const soLieu = await lay('/api/booking/bac-si/toi/tong-quan', true);
            const sl = soLieu.status === 200 ? soLieu.json.duLieu : null;
            for (const [tenO, giaTri] of [
                ["Today's Appointments", sl ? sl.lichHenHomNay : '—'],
                ['Waiting', sl ? sl.dangCho : '—'],
                ['Completed', sl ? sl.daKham : '—'],
                ['Average Rating', sl && sl.diemDanhGia != null ? `★ ${sl.diemDanhGia.toFixed(1)} (${sl.soDanhGia})` : t('— (chưa có)', '— (none yet)')],
            ]) {
                const o = the('div');
                o.append(the('b', String(giaTri)), the('span', tenO, 'ghi-chu'));
                oSo.append(o);
            }
            tinhLuc.textContent = sl
                ? t(`Tính lúc ${ngayGioVn(sl.tinhLuc)} · ${sl.yeuCauChoXacNhan} yêu cầu đang chờ xác nhận (Yêu cầu đặt lịch)`,
                    `As of ${ngayGioVn(sl.tinhLuc)} · ${sl.yeuCauChoXacNhan} requests pending confirmation (Appointment Requests)`)
                : thongDiepLoi(soLieu.json, t('Không tải được số liệu.', 'Could not load the figures.'));

            const [tongQuan, danhGia] = await Promise.all([
                lay('/api/booking/bac-si/toi/danh-gia/tong-quan', true), lay('/api/booking/bac-si/toi/danh-gia?kichThuoc=10', true)]);
            const tq = tongQuan.status === 200 ? tongQuan.json.duLieu : null;
            if (danhGia.status !== 200) {
                dsDanhGia.replaceChildren(the('p', thongDiepLoi(danhGia.json, t('Không tải được đánh giá.', 'Could not load the reviews.')), 'ghi-chu loi'));
            } else if (!danhGia.json.duLieu.noiDung.length) {
                dsDanhGia.replaceChildren(ghiChu(t('Chưa có đánh giá nào.', 'No reviews yet.')));
            } else {
                dsDanhGia.replaceChildren(
                    ghiChu(t('Phân bố: ', 'Distribution: ') + [5, 4, 3, 2, 1].map((sao) => `${sao}★ ${tq ? tq.phanBo[sao] : '?'}`).join(' · ')),
                    bang([t('Số sao', 'Stars'), t('Nhận xét', 'Comment'), t('Ngày khám', 'Visit date'), t('Gửi lúc', 'Submitted at')], danhGia.json.duLieu.noiDung.map((dg) => dong([
                        '★'.repeat(dg.soSao) + ` (${dg.soSao})`, dg.nhanXet, ngayVn(dg.ngayKham), ngayGioVn(dg.ngayTao),
                    ]))));
            }

            const { status, json } = await lay('/api/booking/bac-si/toi/lich-hen?ngay=' + congNgay(0));
            if (status !== 200) {
                danhSach.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được danh sách.', 'Could not load the list.')), 'ghi-chu loi'));
                return;
            }
            const ds = json.duLieu;
            if (!ds.length) {
                danhSach.replaceChildren(ghiChu(t('Hôm nay không có lịch hẹn nào.', 'No appointments today.')));
                return;
            }
            danhSach.replaceChildren(bang([t('Giờ', 'Time'), t('STT', 'No.'), t('Bệnh nhân', 'Patient'), t('Tuổi', 'Age'), t('Lý do khám', 'Reason for visit'),
                t('Trạng thái', 'Status'), t('Thao tác', 'Actions')], ds.map((lich) => dong([
                gio(lich.gioKhamDuKien), lich.soThuTu, lich.benhNhan.hoTen + (lich.doiChieu.canDoiChieu ? t(' ⚠ cần đối chiếu', ' ⚠ identity check needed') : ''),
                lich.benhNhan.tuoi, lich.lyDoKham, TRANG_THAI[lich.trangThai] || lich.trangThai,
                lienKet(t('Mở hồ sơ khám', 'Open medical record'), '/doctor/consultation?id=' + lich.id),
            ]))));
        },
    });
})();
