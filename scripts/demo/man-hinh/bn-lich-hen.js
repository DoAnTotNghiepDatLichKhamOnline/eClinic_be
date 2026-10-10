// Màn hình ngoài danh sách UI: lịch hẹn của bệnh nhân (dạng lịch + danh sách phân trang), chi tiết và kết quả khám
// Đường dẫn:  /patient/appointments
// Vai trò:    BENH_NHAN
// API:
//   GET /api/booking/lich-hen/cua-toi/lich?tuNgay=&denNgay=&cuaAi=TAT_CA|BAN_THAN|NGUOI_KHAC   lịch hẹn trong khoảng ngày (vẽ lịch)
//   GET /api/booking/lich-hen/cua-toi?loc=TAT_CA|SAP_TOI|LICH_SU&trang=&kichThuoc=             danh sách phân trang
//   GET /api/booking/lich-hen/cua-toi/{maPhieuKham}                                            chi tiết + kết quả khám (nếu được xem)
// Kết quả khám chỉ trả về cho người khám hoặc người đã đặt lịch. Lịch bị bác sĩ từ chối (BI_TU_CHOI) có lyDoHuy, hiện ở phần chi tiết.
//   POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/huy       { lyDo? }  hủy (tôi đặt, hoặc tôi là người khám) -> phiếu khám DA_HUY
//   POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/doi-lich  { idLichLamViec | idChuyenKhoa, gioBatDauKhung } -> 201, lịch mới
//        (CHO_XAC_NHAN, mã phiếu khám mới); lịch cũ thành DA_HUY_DO_DOI_LICH. Tối đa 2 lần đổi cho 1 lần đặt.
//        Mỗi dòng có duocHuyDoi / hanHuyDoi (hạn chót = giờ khám trừ 2 giờ). Lỗi 409: LICH_HEN_KHONG_HUY_DOI_DUOC,
//        QUA_HAN_HUY_DOI_LICH, VUOT_SO_LAN_DOI_LICH, KHUNG_GIO_KHONG_CON_TRONG...; 403 nếu chỉ là người giám hộ theo CCCD
//        canDoiLich = true: ca khám đã bị hủy (quản trị viên hủy ca, hoặc duyệt bác sĩ xin nghỉ / đổi ca); lịch vẫn còn
//        hiệu lực, hủy / đổi được tới giờ khám cũ và không tính vào số lần đổi; lịch mới sau khi đổi không còn cờ này
//   GET  /api/booking/khung-gio?ngay=&idBacSi=                 khung giờ còn trống để đổi sang
// UI chưa có màn hình hủy / đổi lịch: trang mẫu đặt các nút ở phần chi tiết lịch hẹn.
//   POST /api/booking/lich-hen/cua-toi/{maPhieuKham}/danh-gia  { soSao 1..5, nhanXet? } -> 201: đánh giá lượt khám đã hoàn thành,
//        mỗi lịch hẹn 1 lần (409 LICH_HEN_CHUA_KHAM_XONG, DA_DANH_GIA); chỉ người khám hoặc người đã đặt lịch (403)
//   PUT  /api/booking/lich-hen/cua-toi/{maPhieuKham}/danh-gia  cùng thân: sửa trong 7 ngày kể từ lúc gửi (409 HET_HAN_SUA_DANH_GIA)
//        Chi tiết lịch hẹn trả duocDanhGia và danhGia { soSao, nhanXet, ngayTao, duocSuaDen }.
// UI chưa có form đánh giá: trang mẫu đặt form ở phần chi tiết của lượt khám đã hoàn thành.
// Cần internet: thư viện lịch FullCalendar tải từ CDN.
(() => {
    const KICH_THUOC_TRANG = 5;
    let lichBn = null;

    // Form đánh giá của 1 lượt khám đã hoàn thành: gửi mới (duocDanhGia) hoặc sửa đánh giá đã gửi khi còn hạn
    function khoiDanhGia(ct) {
        const dg = ct.danhGia;
        const khoi = the('div');
        khoi.append(the('h3', 'Đánh giá lượt khám'));
        if (dg) {
            khoi.append(thongTin([
                ['Đã đánh giá', '★'.repeat(dg.soSao) + '☆'.repeat(5 - dg.soSao) + ` (${dg.soSao}/5)`], ['Nhận xét', dg.nhanXet],
                ['Gửi lúc', ngayGioVn(dg.ngayTao)], ['Sửa được đến', ngayGioVn(dg.duocSuaDen)],
            ]));
        }
        const conHanSua = dg && new Date(dg.duocSuaDen) > new Date();
        if (!ct.duocDanhGia && !conHanSua) {
            if (dg) khoi.append(ghiChu('Đã quá thời hạn sửa đánh giá.'));
            return khoi;
        }
        const form = the('form');
        form.innerHTML = `
            <div class="hai-cot">
                <div><label>Số sao</label><select name="soSao" required>
                    <option value="5">★★★★★ (5) Rất hài lòng</option><option value="4">★★★★ (4) Hài lòng</option>
                    <option value="3">★★★ (3) Bình thường</option><option value="2">★★ (2) Chưa hài lòng</option>
                    <option value="1">★ (1) Rất không hài lòng</option></select></div>
                <div><label>Nhận xét (không bắt buộc; chỉ bác sĩ và phòng khám đọc, bác sĩ không thấy tên bạn)</label><input name="nhanXet" maxlength="1000"></div>
            </div>
            <button>${dg ? 'Sửa đánh giá' : 'Gửi đánh giá'}</button>`;
        if (dg) {
            form.elements.soSao.value = dg.soSao;
            form.elements.nhanXet.value = dg.nhanXet || '';
        }
        const trangThai = the('p', null, 'ghi-chu');
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const than = { soSao: Number(form.elements.soSao.value), nhanXet: form.elements.nhanXet.value.trim() || null };
            const { status, json } = await goi(`/api/booking/lich-hen/cua-toi/${encodeURIComponent(ct.lichHen.maPhieuKham)}/danh-gia`, than, dg ? 'PUT' : 'POST');
            if (status !== 200 && status !== 201) {
                bao(trangThai, thongDiepLoi(json, 'Không gửi được đánh giá.'), 'loi');
                return;
            }
            await xemChiTiet(ct.lichHen.maPhieuKham);
        });
        khoi.append(form, trangThai);
        return khoi;
    }
    let trangLich = 0, tongSoTrangLich = 0;

    function mauLichBenhNhan(lich) {
        if (DA_HUY.includes(lich.trangThai)) return MAU.daHuy;
        if (lich.trangThai === 'DA_HOAN_THANH') return MAU.daKham;
        return lich.laBanThan ? MAU.cuaToi : MAU.nguoiKhac;
    }

    async function nguonLich(info, xong, hong) {
        const { status, json } = await lay(`/api/booking/lich-hen/cua-toi/lich?${khoangNgay(info)}&cuaAi=${$('bn-cua-ai').value}`, true);
        if (status !== 200) {
            bao('bn-lich-trang-thai', thongDiepLoi(json, 'Không tải được lịch hẹn.'), 'loi');
            hong(new Error('HTTP ' + status));
            return;
        }
        bao('bn-lich-trang-thai', `${json.duLieu.length} lịch hẹn trong khoảng đang xem. Bấm vào 1 lịch hẹn để xem chi tiết.`);
        xong(json.duLieu.map((lich) => ({
            title: `${lich.hoTenBenhNhan} · BS ${lich.bacSi.hoTen}` + (lich.thongTinKhacHoSo ? ' ⚠' : ''),
            start: lich.gioKhamDuKien,
            color: mauLichBenhNhan(lich),
            extendedProps: { lich },
        })));
    }

    // Sau khi hủy / đổi: vẽ lại lịch và danh sách
    async function taiLaiSauKhiDoi() {
        if (lichBn) lichBn.refetchEvents();
        await taiDanhSach(true);
    }

    async function xemChiTiet(maPhieuKham) {
        const { status, json } = await lay('/api/booking/lich-hen/cua-toi/' + encodeURIComponent(maPhieuKham));
        const khung = $('bn-chi-tiet');
        $('bn-khoi-chi-tiet').classList.remove('an');
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, 'Không tải được lịch hẹn.'), 'ghi-chu loi'));
            return;
        }
        const ct = json.duLieu, lich = ct.lichHen;
        const cacPhan = [];
        if (lich.thongTinKhacHoSo) {
            cacPhan.push(the('p', 'Lịch này được đặt với họ tên / ngày sinh khác hồ sơ của bạn. Nếu không phải lịch của bạn, hãy liên hệ phòng khám.', 'canh-bao'));
        }
        cacPhan.push(thongTin([
            ['Mã tra cứu', lich.maTraCuu], ['Trạng thái', TRANG_THAI[lich.trangThai] || lich.trangThai],
            ['Cần đổi lịch', lich.canDoiLich ? CAN_DOI_LICH : null], ['Lý do từ chối / hủy', lich.lyDoHuy], ['Số thứ tự', lich.soThuTu],
            ['Giờ khám dự kiến', ngayGioVn(lich.gioKhamDuKien)], ['Bác sĩ', tenBacSi(lich.bacSi)],
            ['Chuyên khoa', lich.tenChuyenKhoa], ['Phòng khám', tenPhong(lich.phongKham)],
            ['Người khám', lich.hoTenBenhNhan + (lich.laBanThan ? ' (tôi)' : ' (người khác)')],
            ['Ngày sinh', ct.ngaySinhBenhNhan ? ngayVn(ct.ngaySinhBenhNhan) : null], ['Giới tính', GIOI_TINH[ct.gioiTinhBenhNhan]],
            ['Người giám hộ', lich.hoTenNguoiGiamHo], ['Ai đặt', NGUOI_DAT[lich.nguoiDat]],
            ['SĐT liên hệ', ct.soDienThoaiLienHe], ['Email liên hệ', ct.emailLienHe], ['Lý do khám', lich.lyDoKham],
            ['Ngày đặt', ngayGioVn(lich.ngayDat)],
        ]));
        cacPhan.push(the('p'), lienKet('Mở phiếu khám (mã QR)', '/phieu-kham/' + encodeURIComponent(lich.maPhieuKham), true));
        if (['CHO_XAC_NHAN', 'DA_XAC_NHAN'].includes(lich.trangThai)) {
            const goc = '/api/booking/lich-hen/cua-toi/' + encodeURIComponent(lich.maPhieuKham);
            cacPhan.push(khoiHuyDoi({
                lich,
                gui: (hanhDong, body) => goi(`${goc}/${hanhDong}`, body),
                async khiXong(hanhDong, duLieu) {
                    // Hủy: mở lại chính lịch này; đổi: mở lịch mới (mã phiếu khám mới)
                    await Promise.all([xemChiTiet(duLieu.maPhieuKham), taiLaiSauKhiDoi()]);
                    bao('bn-lich-trang-thai', hanhDong === 'huy' ? 'Đã hủy lịch hẹn.' : `Đã đổi lịch. Lịch mới: ${duLieu.maTraCuu}, chờ bác sĩ xác nhận.`, 'tot');
                },
            }));
        }
        if (ct.ketQua) {
            cacPhan.push(the('h3', 'Kết quả khám'), ketQuaKham(ct.ketQua));
        } else if (lich.trangThai === 'DA_HOAN_THANH') {
            cacPhan.push(ghiChu('Lượt khám đã hoàn thành nhưng tài khoản này không được xem kết quả (chỉ người khám hoặc người đã đặt lịch).'));
        }
        if (lich.trangThai === 'DA_HOAN_THANH' && (ct.duocDanhGia || ct.danhGia)) {
            cacPhan.push(khoiDanhGia(ct));
        }
        khung.replaceChildren(...cacPhan);
        $('bn-khoi-chi-tiet').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    function hangThongBao(noiDung, lop) {
        const tr = the('tr');
        const td = the('td', noiDung, lop);
        td.colSpan = 7;
        tr.append(td);
        return tr;
    }

    function hangLich(lich) {
        const nguoiKham = `${lich.hoTenBenhNhan} (${lich.laBanThan ? 'bản thân' : 'người thân'})`
            + (lich.hoTenNguoiGiamHo ? `, giám hộ: ${lich.hoTenNguoiGiamHo}` : '');
        return dong([
            ngayGioVn(lich.gioKhamDuKien), lich.soThuTu, `${tenBacSi(lich.bacSi)} · ${lich.tenChuyenKhoa}`,
            lich.phongKham.tenPhong, nguoiKham, TRANG_THAI[lich.trangThai] || lich.trangThai,
            nut(lich.maTraCuu || 'Xem', () => xemChiTiet(lich.maPhieuKham)),
        ]);
    }

    async function taiDanhSach(imLang = false) {
        const { status, json } = await lay(
            `/api/booking/lich-hen/cua-toi?loc=${$('loc-lich').value}&trang=${trangLich}&kichThuoc=${KICH_THUOC_TRANG}`, imLang);
        if (!$('ds-lich')) return;
        if (status !== 200) {
            $('ds-lich').replaceChildren(hangThongBao(thongDiepLoi(json, 'Không tải được lịch hẹn.'), 'loi'));
            $('trang-lich').textContent = '';
            return;
        }
        const duLieu = json.duLieu;
        tongSoTrangLich = duLieu.tongSoTrang;
        $('ds-lich').replaceChildren(...(duLieu.noiDung.length ? duLieu.noiDung.map(hangLich) : [hangThongBao('Không có lịch hẹn nào.')]));
        $('trang-lich').textContent = `Trang ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} lịch hẹn`;
    }

    dangKy({
        duongDan: '/patient/appointments',
        vaiTro: 'BENH_NHAN',
        tieuDe: 'My Appointments',
        async ve(khung, { thamSo }) {
            khuon(khung, `
                <section>
                    <h2>Lịch hẹn của tôi (dạng lịch)</h2>
                    <div class="hai-cot">
                        <div><label for="bn-cua-ai">Hiển thị</label>
                            <select id="bn-cua-ai">
                                <option value="TAT_CA">Tất cả</option>
                                <option value="BAN_THAN">Lịch của tôi</option>
                                <option value="NGUOI_KHAC">Lịch của người khác</option>
                            </select></div>
                        <p class="chu-thich"><span><i style="background:#1f6feb"></i>Của tôi</span><span><i style="background:#bf8700"></i>Của người khác</span>
                            <span><i style="background:#1a7f37"></i>Đã khám</span><span><i style="background:#8c959f"></i>Đã hủy / bị từ chối</span></p>
                    </div>
                    <div id="lich-bn"></div>
                    <p id="bn-lich-trang-thai" class="ghi-chu"></p>
                </section>
                <section id="bn-khoi-chi-tiet" class="an">
                    <h2>Chi tiết lịch hẹn</h2>
                    <div id="bn-chi-tiet"></div>
                </section>
                <section>
                    <h2>Danh sách lịch hẹn</h2>
                    <select id="loc-lich">
                        <option value="TAT_CA">Tất cả</option>
                        <option value="SAP_TOI">Sắp tới</option>
                        <option value="LICH_SU">Lịch sử</option>
                    </select>
                    <div class="bang">
                        <table>
                            <thead><tr><th>Giờ khám dự kiến</th><th>STT</th><th>Bác sĩ</th><th>Phòng</th><th>Người khám</th><th>Trạng thái</th><th>Chi tiết</th></tr></thead>
                            <tbody id="ds-lich"></tbody>
                        </table>
                    </div>
                    <p id="trang-lich" class="ghi-chu"></p>
                    <button id="nut-lich-truoc" class="phu nho">‹ Trang trước</button>
                    <button id="nut-lich-sau" class="phu nho">Trang sau ›</button>
                </section>`);
            trangLich = 0;
            $('bn-cua-ai').addEventListener('change', () => lichBn && lichBn.refetchEvents());
            $('loc-lich').addEventListener('change', () => {
                trangLich = 0;
                taiDanhSach();
            });
            $('nut-lich-truoc').addEventListener('click', () => {
                if (trangLich > 0) {
                    trangLich--;
                    taiDanhSach();
                }
            });
            $('nut-lich-sau').addEventListener('click', () => {
                if (trangLich + 1 < tongSoTrangLich) {
                    trangLich++;
                    taiDanhSach();
                }
            });
            taiDanhSach(true);
            lichBn = await taoLich($('lich-bn'), [nguonLich], {
                eventClick: (info) => xemChiTiet(info.event.extendedProps.lich.maPhieuKham),
            });
            // Mở từ chuông thông báo: /patient/appointments?ma=<mã phiếu khám> hiện luôn chi tiết lịch hẹn đó
            if (thamSo.get('ma')) xemChiTiet(thamSo.get('ma'));
            return () => {
                if (lichBn) lichBn.destroy();
                lichBn = null;
            };
        },
    });
})();
