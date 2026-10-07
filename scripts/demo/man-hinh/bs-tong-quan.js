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
        tieuDe: 'Dashboard',
        async ve(khung) {
            const oSo = the('div', null, 'o-so');
            const danhSach = the('div', null, 'bang');
            const phanLich = the('section');
            phanLich.append(the('h2', `Today's Appointments — lịch hẹn hôm nay (${ngayVn(congNgay(0))})`), danhSach);
            const dsDanhGia = the('div', null, 'bang');
            const phanDanhGia = the('section');
            phanDanhGia.append(the('h2', 'Đánh giá của bệnh nhân (ẩn danh)'), dsDanhGia);
            const tinhLuc = ghiChu('');
            khung.append(oSo, tinhLuc, phanLich, phanDanhGia);

            const soLieu = await lay('/api/booking/bac-si/toi/tong-quan', true);
            const sl = soLieu.status === 200 ? soLieu.json.duLieu : null;
            for (const [nhan, giaTri] of [
                ["Today's Appointments", sl ? sl.lichHenHomNay : '—'],
                ['Waiting', sl ? sl.dangCho : '—'],
                ['Completed', sl ? sl.daKham : '—'],
                ['Average Rating', sl && sl.diemDanhGia != null ? `★ ${sl.diemDanhGia.toFixed(1)} (${sl.soDanhGia})` : '— (chưa có)'],
            ]) {
                const o = the('div');
                o.append(the('b', String(giaTri)), the('span', nhan, 'ghi-chu'));
                oSo.append(o);
            }
            tinhLuc.textContent = sl
                ? `Tính lúc ${ngayGioVn(sl.tinhLuc)} · ${sl.yeuCauChoXacNhan} yêu cầu đang chờ xác nhận (Appointment Requests)`
                : thongDiepLoi(soLieu.json, 'Không tải được số liệu.');

            const [tongQuan, danhGia] = await Promise.all([
                lay('/api/booking/bac-si/toi/danh-gia/tong-quan', true), lay('/api/booking/bac-si/toi/danh-gia?kichThuoc=10', true)]);
            const tq = tongQuan.status === 200 ? tongQuan.json.duLieu : null;
            if (danhGia.status !== 200) {
                dsDanhGia.replaceChildren(the('p', thongDiepLoi(danhGia.json, 'Không tải được đánh giá.'), 'ghi-chu loi'));
            } else if (!danhGia.json.duLieu.noiDung.length) {
                dsDanhGia.replaceChildren(ghiChu('No data yet — chưa có đánh giá nào.'));
            } else {
                dsDanhGia.replaceChildren(
                    ghiChu('Phân bố: ' + [5, 4, 3, 2, 1].map((sao) => `${sao}★ ${tq ? tq.phanBo[sao] : '?'}`).join(' · ')),
                    bang(['Số sao', 'Nhận xét', 'Ngày khám', 'Gửi lúc'], danhGia.json.duLieu.noiDung.map((dg) => dong([
                        '★'.repeat(dg.soSao) + ` (${dg.soSao})`, dg.nhanXet, ngayVn(dg.ngayKham), ngayGioVn(dg.ngayTao),
                    ]))));
            }

            const { status, json } = await lay('/api/booking/bac-si/toi/lich-hen?ngay=' + congNgay(0));
            if (status !== 200) {
                danhSach.replaceChildren(the('p', thongDiepLoi(json, 'Không tải được danh sách.'), 'ghi-chu loi'));
                return;
            }
            const ds = json.duLieu;
            if (!ds.length) {
                danhSach.replaceChildren(ghiChu('Hôm nay không có lịch hẹn nào.'));
                return;
            }
            danhSach.replaceChildren(bang(['Giờ', 'STT', 'Bệnh nhân', 'Tuổi', 'Lý do khám', 'Trạng thái', 'Thao tác'], ds.map((lich) => dong([
                gio(lich.gioKhamDuKien), lich.soThuTu, lich.benhNhan.hoTen + (lich.doiChieu.canDoiChieu ? ' ⚠ cần đối chiếu' : ''),
                lich.benhNhan.tuoi, lich.lyDoKham, TRANG_THAI[lich.trangThai] || lich.trangThai,
                lienKet('Mở hồ sơ khám', '/doctor/consultation?id=' + lich.id),
            ]))));
        },
    });
})();
