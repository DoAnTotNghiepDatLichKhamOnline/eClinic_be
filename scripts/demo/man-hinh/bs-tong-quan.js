// Màn hình UI: Dashboard (Doctor) — hàng đợi khám hôm nay theo ca, 4 ô số liệu và đánh giá của bệnh nhân
// Đường dẫn:  /doctor/dashboard
// Vai trò:    BAC_SI
// API:
//   GET  /api/booking/bac-si/toi/hang-doi                 hàng đợi khám hôm nay theo ca: { ngay, idCaHienTai (ca mở sẵn), tinhLuc,
//                                                         cacCa: [{ idLichLamViec, buoi (SANG/CHIEU/TOI), gioBatDau, gioKetThuc, phongKham,
//                                                         soLuong {tong, choKham, dangKham, vangMat, daKham}, idDangKham, idTiepTheo,
//                                                         cacLuot: [{ trangThaiHangDoi, goiLuc, lichHen }] theo số thứ tự }] }; tự tải lại mỗi 15 giây
//   POST /api/booking/bac-si/toi/hang-doi/goi-tiep?idLichLamViec=   gọi bệnh nhân chờ khám có số thứ tự nhỏ nhất của ca
//   POST /api/booking/bac-si/toi/lich-hen/{id}/goi-kham   gọi 1 bệnh nhân (chờ khám, hoặc gọi lại người vắng mặt)
//   POST /api/booking/bac-si/toi/lich-hen/{id}/vang-mat   đánh dấu vắng mặt; ghi nhận kết quả khám (màn hình khám bệnh) là xong lượt
//                                                         3 thao tác trên trả lại hàng đợi hôm nay; 409 DANG_KHAM_BENH_NHAN_KHAC khi đang
//                                                         khám người khác, HANG_DOI_TRONG khi ca không còn ai chờ
//   GET  /api/booking/bac-si/toi/tong-quan                4 ô số liệu trong 1 lần gọi: lichHenHomNay (không tính đã hủy / bác sĩ đã hủy),
//                                                         dangCho (đã xác nhận, chưa khám), daKham, diemDanhGia + soDanhGia;
//                                                         thêm lichHenSapToi (số trên nhãn Upcoming Appointments) và tinhLuc
//   GET  /api/booking/bac-si/toi/danh-gia/tong-quan       { diemTrungBinh (null khi chưa có), soDanhGia, phanBo: {1..5} }
//   GET  /api/booking/bac-si/toi/danh-gia?trang=&kichThuoc=   đánh giá tôi nhận được, mới nhất trước: soSao, nhanXet, ngayKham, ngayTao
//                                                          (ẩn danh: không có tên bệnh nhân)
(() => {
    const GIAY_TAI_LAI = 15;

    dangKy({
        duongDan: '/doctor/dashboard',
        vaiTro: 'BAC_SI',
        tieuDe: cap('Tổng quan', 'Dashboard'),
        async ve(khung) {
            const cacTab = the('div');
            const khuDangKham = the('div');
            const khuGoiTiep = the('div');
            const baoHangDoi = ghiChu('');
            const dsHangDoi = the('div', null, 'bang');
            const capNhatLuc = ghiChu('');
            const phanHangDoi = the('section');
            phanHangDoi.append(the('h2', t(`Hàng đợi khám hôm nay (${ngayVn(congNgay(0))})`, `Today's examination queue (${ngayVn(congNgay(0))})`)),
                cacTab, khuDangKham, khuGoiTiep, baoHangDoi, dsHangDoi, capNhatLuc);
            const oSo = the('div', null, 'o-so');
            const dsDanhGia = the('div', null, 'bang');
            const phanDanhGia = the('section');
            phanDanhGia.append(the('h2', t('Đánh giá của bệnh nhân (ẩn danh)', 'Patient reviews (anonymous)')), dsDanhGia);
            const tinhLuc = ghiChu('');
            khung.append(phanHangDoi, oSo, tinhLuc, phanDanhGia);

            // ===== Hàng đợi khám =====
            let hangDoi = null;       // phản hồi gần nhất của GET hang-doi / thao tác
            let caDangChon = null;    // giữ tab đang xem qua các lần tự tải lại
            let dangThaoTac = false;  // đang chờ 1 thao tác: không tự tải lại đè lên

            const moHoSo = (lich) => lienKet(t('Mở hồ sơ khám', 'Open medical record'), '/doctor/consultation?id=' + lich.id);
            const tenLuot = (lich) => `${t('STT', 'No.')} ${lich.soThuTu} – ${lich.benhNhan.hoTen}`;

            async function thaoTac(duongDan) {
                if (dangThaoTac) return;
                dangThaoTac = true;
                const { status, json } = await goi(duongDan, undefined, 'POST');
                dangThaoTac = false;
                if (status === 200) {
                    bao(baoHangDoi, '');
                    hangDoi = json.duLieu;
                    veHangDoi();
                } else {
                    bao(baoHangDoi, thongDiepLoi(json, t('Không thực hiện được thao tác.', 'Could not complete the action.')), 'loi');
                    taiHangDoi();
                }
            }

            async function taiHangDoi() {
                const { status, json } = await lay('/api/booking/bac-si/toi/hang-doi', true);
                if (status !== 200) {
                    dsHangDoi.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được hàng đợi.', 'Could not load the queue.')), 'ghi-chu loi'));
                    return;
                }
                hangDoi = json.duLieu;
                veHangDoi();
            }

            function veHangDoi() {
                const cacCa = hangDoi.cacCa;
                capNhatLuc.textContent = t(`Cập nhật lúc ${gio(hangDoi.tinhLuc)} · tự làm mới mỗi ${GIAY_TAI_LAI} giây`,
                    `Updated at ${gio(hangDoi.tinhLuc)} · refreshes every ${GIAY_TAI_LAI} seconds`);
                if (!cacCa.length) {
                    cacTab.replaceChildren();
                    khuDangKham.replaceChildren();
                    khuGoiTiep.replaceChildren();
                    dsHangDoi.replaceChildren(ghiChu(t('Hôm nay bạn không có ca làm việc nào.', 'You have no shifts today.')));
                    return;
                }
                if (!cacCa.some((ca) => ca.idLichLamViec === caDangChon)) caDangChon = hangDoi.idCaHienTai ?? cacCa[0].idLichLamViec;
                const ca = cacCa.find((c) => c.idLichLamViec === caDangChon);

                cacTab.replaceChildren(...cacCa.map((c) => nut(
                    `${BUOI[c.buoi]} ${c.gioBatDau.slice(0, 5)}–${c.gioKetThuc.slice(0, 5)} · ${c.phongKham.tenPhong} · `
                    + t(`chờ ${c.soLuong.choKham} / ${c.soLuong.tong}`, `waiting ${c.soLuong.choKham} / ${c.soLuong.tong}`),
                    () => { caDangChon = c.idLichLamViec; veHangDoi(); },
                    'o' + (c.idLichLamViec === caDangChon ? ' da-chon' : ''))));

                // Người đang khám có thể thuộc ca khác tab đang xem: mỗi bác sĩ chỉ 1 người đang khám trong ngày
                const dangKham = cacCa.flatMap((c) => c.cacLuot).find((luot) => luot.trangThaiHangDoi === 'DANG_KHAM');
                if (dangKham) {
                    const lich = dangKham.lichHen;
                    const theDangKham = the('div', null, 'dang-kham');
                    theDangKham.append(the('b', `${HANG_DOI.DANG_KHAM}: ${tenLuot(lich)}`),
                        ghiChu([t(`Gọi lúc ${gio(dangKham.goiLuc)}`, `Called at ${gio(dangKham.goiLuc)}`), lich.benhNhan.tuoi != null ? t(`${lich.benhNhan.tuoi} tuổi`, `age ${lich.benhNhan.tuoi}`) : null,
                            lich.lyDoKham].filter(Boolean).join(' · ')),
                        moHoSo(lich), ' ', nut(HANG_DOI.VANG_MAT, () => thaoTac(`/api/booking/bac-si/toi/lich-hen/${lich.id}/vang-mat`), 'nho phu'));
                    khuDangKham.replaceChildren(theDangKham);
                } else {
                    khuDangKham.replaceChildren(ghiChu(t('Chưa có bệnh nhân nào đang khám.', 'No patient is in examination.')));
                }

                const tiepTheo = ca.cacLuot.find((luot) => luot.lichHen.id === ca.idTiepTheo);
                const nutTiep = nut(tiepTheo ? t(`Gọi bệnh nhân tiếp theo (${tenLuot(tiepTheo.lichHen)})`, `Call next patient (${tenLuot(tiepTheo.lichHen)})`)
                    : t('Gọi bệnh nhân tiếp theo', 'Call next patient'),
                () => thaoTac(`/api/booking/bac-si/toi/hang-doi/goi-tiep?idLichLamViec=${ca.idLichLamViec}`), '');
                nutTiep.disabled = !tiepTheo || !!dangKham;
                khuGoiTiep.replaceChildren(nutTiep);
                if (dangKham) khuGoiTiep.append(ghiChu(t('Ghi nhận kết quả khám hoặc đánh dấu vắng mặt người đang khám rồi mới gọi tiếp.',
                    'Record the visit or mark the current patient as absent before calling the next one.')));
                else if (!tiepTheo) khuGoiTiep.append(ghiChu(t('Ca này không còn bệnh nhân nào đang chờ.', 'No patient is waiting in this shift.')));

                if (!ca.cacLuot.length) {
                    dsHangDoi.replaceChildren(ghiChu(t('Ca này chưa có lịch hẹn nào.', 'This shift has no appointments yet.')));
                    return;
                }
                dsHangDoi.replaceChildren(bang([t('STT', 'No.'), t('Giờ', 'Time'), t('Bệnh nhân', 'Patient'), t('Tuổi', 'Age'), t('Lý do khám', 'Reason for visit'),
                    t('Hàng đợi', 'Queue'), t('Lịch hẹn', 'Appointment'), t('Gọi lúc', 'Called at'), t('Thao tác', 'Actions')], ca.cacLuot.map((luot) => {
                    const lich = luot.lichHen;
                    const thaoTacDong = [];
                    // Gọi / vắng chỉ với lịch còn khám được (lịch KHONG_DEN của ngày đã qua cũng hiện "vắng mặt")
                    const conKham = lich.trangThai === 'DA_XAC_NHAN';
                    if (conKham && (luot.trangThaiHangDoi === 'CHO_KHAM' || luot.trangThaiHangDoi === 'VANG_MAT')) {
                        const nutGoi = nut(luot.trangThaiHangDoi === 'VANG_MAT' ? t('Gọi lại', 'Call again') : t('Gọi', 'Call'),
                            () => thaoTac(`/api/booking/bac-si/toi/lich-hen/${lich.id}/goi-kham`));
                        nutGoi.disabled = !!dangKham;
                        thaoTacDong.push(nutGoi);
                    }
                    if (conKham && (luot.trangThaiHangDoi === 'CHO_KHAM' || luot.trangThaiHangDoi === 'DANG_KHAM')) {
                        thaoTacDong.push(nut(t('Vắng', 'Absent'), () => thaoTac(`/api/booking/bac-si/toi/lich-hen/${lich.id}/vang-mat`), 'nho phu'));
                    }
                    thaoTacDong.push(' ', moHoSo(lich));
                    return dong([
                        lich.soThuTu, gio(lich.gioKhamDuKien),
                        lich.benhNhan.hoTen + (lich.doiChieu.canDoiChieu ? t(' ⚠ cần đối chiếu', ' ⚠ identity check needed') : ''),
                        lich.benhNhan.tuoi, lich.lyDoKham, HANG_DOI[luot.trangThaiHangDoi] || luot.trangThaiHangDoi,
                        TRANG_THAI[lich.trangThai] || lich.trangThai, luot.goiLuc ? gio(luot.goiLuc) : '', thaoTacDong,
                    ], luot.trangThaiHangDoi === 'DANG_KHAM' ? 'dang-kham-dong' : luot.trangThaiHangDoi === 'DA_KHAM' ? 'da-kham-dong' : null);
                })));
            }

            await taiHangDoi();
            const henGio = setInterval(() => { if (!dangThaoTac) taiHangDoi(); }, GIAY_TAI_LAI * 1000);
            const donDep = () => clearInterval(henGio);

            // ===== Số liệu và đánh giá =====
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
                ? t(`Tính lúc ${ngayGioVn(sl.tinhLuc)} · ${sl.lichHenSapToi} lịch hẹn sắp tới (Lịch hẹn sắp tới)`,
                    `As of ${ngayGioVn(sl.tinhLuc)} · ${sl.lichHenSapToi} upcoming appointments (Upcoming Appointments)`)
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
            return donDep;
        },
    });
})();
