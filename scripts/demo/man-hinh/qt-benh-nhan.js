// Màn hình UI: Patient Management (Admin)
// Đường dẫn:  /admin/patients   (?id=<id hồ sơ bệnh nhân> mở thẳng 1 hồ sơ; id lấy từ idHoSoBenhNhan của lịch hẹn)
// Vai trò:    QUAN_TRI_VIEN
// API:
//   GET  /api/booking/quan-tri/ho-so-benh-nhan?tuKhoa=&trangThaiLienKet=&trang=&kichThuoc=   danh sách hồ sơ, mới tạo trước:
//        id, hoTen, ngaySinh, gioiTinh, soDienThoai, cccdChe (chỉ còn 4 số cuối), coTaiKhoan, trangThaiLienKet, soLichHen, ngayTao.
//        tuKhoa so với họ tên / số điện thoại / số bảo hiểm y tế, hoặc đủ 12 số CCCD
//   GET  /api/booking/quan-tri/ho-so-benh-nhan/{id}/lich-hen?trang=&kichThuoc=   lịch hẹn của hồ sơ, mọi trạng thái, muộn nhất trước
//        (không có chẩn đoán / đơn thuốc)
//   GET  /api/booking/quan-tri/ho-so-benh-nhan/cho-xac-minh?trang=&kichThuoc=   hồ sơ đang chờ xác minh liên kết với tài khoản
//        (tài khoản khai đúng số CCCD nhưng họ tên / số điện thoại không khớp hồ sơ), cũ nhất trước
//   POST /api/booking/quan-tri/ho-so-benh-nhan/{id}/duyet        hồ sơ thuộc về tài khoản đang chờ
//   POST /api/booking/quan-tri/ho-so-benh-nhan/{id}/tu-choi      { lyDo? } : hồ sơ trở lại chưa liên kết
//   GET  /api/booking/quan-tri/ho-so-benh-nhan/{id}              xem 1 hồ sơ (số CCCD không che)
//   PUT  /api/booking/quan-tri/ho-so-benh-nhan/{id}              sửa sau khi đối chiếu giấy tờ: hoTen, ngaySinh, gioiTinh,
//        soDienThoai, cccd (chỉ điền được khi hồ sơ chưa có), diaChi, soBaoHiemYTe
(() => {
    const HO_SO = '/api/booking/quan-tri/ho-so-benh-nhan';
    const TRUONG = ['hoTen', 'ngaySinh', 'gioiTinh', 'soDienThoai', 'cccd', 'diaChi', 'soBaoHiemYTe'];
    let hoSoDangSua = null;
    let trang = 0, tongSoTrang = 0;
    let trangDs = 0, tongSoTrangDs = 0;
    let trangLichHen = 0, tongSoTrangLichHen = 0;

    async function taiDanhSach(imLang = false) {
        const thamSo = new URLSearchParams({ trang: trangDs, kichThuoc: 10, ...thanForm($('form-tim-ho-so'), ['tuKhoa', 'trangThaiLienKet']) });
        const { status, json } = await lay(`${HO_SO}?${thamSo}`, imLang);
        const khung = $('ds-ho-so');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, 'Không tải được danh sách hồ sơ bệnh nhân.'), 'ghi-chu loi'));
            return;
        }
        const duLieu = json.duLieu;
        tongSoTrangDs = duLieu.tongSoTrang;
        $('trang-ho-so').textContent = `Trang ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} hồ sơ`;
        if (!duLieu.noiDung.length) {
            khung.replaceChildren(ghiChu('No data yet — không có hồ sơ bệnh nhân nào khớp.'));
            return;
        }
        khung.replaceChildren(bang(['Id', 'Họ tên', 'Ngày sinh', 'SĐT', 'CCCD (đã che)', 'Tài khoản', 'Số lịch hẹn', 'Ngày tạo', 'Thao tác'], duLieu.noiDung.map((hs) => dong([
            hs.id, hs.hoTen, hs.ngaySinh ? ngayVn(hs.ngaySinh) : '', hs.soDienThoai, hs.cccdChe,
            hs.coTaiKhoan ? (LIEN_KET[hs.trangThaiLienKet] || hs.trangThaiLienKet) : 'Khách (không có tài khoản)',
            hs.soLichHen, hs.ngayTao ? ngayGioVn(hs.ngayTao) : '',
            nut('Mở hồ sơ', () => moHoSo(hs.id)),
        ]))));
    }

    async function taiLichHen(imLang = false) {
        const khung = $('ho-so-qt-lich-hen');
        if (!khung || !hoSoDangSua) return;
        const { status, json } = await lay(`${HO_SO}/${hoSoDangSua.id}/lich-hen?trang=${trangLichHen}&kichThuoc=10`, imLang);
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, 'Không tải được lịch hẹn của hồ sơ.'), 'ghi-chu loi'));
            return;
        }
        const duLieu = json.duLieu;
        tongSoTrangLichHen = duLieu.tongSoTrang;
        $('trang-lich-hen-ho-so').textContent = `Trang ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} lịch hẹn`;
        if (!duLieu.noiDung.length) {
            khung.replaceChildren(ghiChu('No data yet — hồ sơ này chưa có lịch hẹn nào.'));
            return;
        }
        khung.replaceChildren(bang(['Mã tra cứu', 'Giờ khám', 'Bác sĩ', 'Phòng', 'Lý do khám', 'Trạng thái'], duLieu.noiDung.map((lh) => dong([
            lh.maTraCuu, ngayGioVn(lh.gioKhamDuKien), tenBacSi(lh.bacSi), lh.phongKham ? lh.phongKham.tenPhong : '', lh.lyDoKham,
            (TRANG_THAI[lh.trangThai] || lh.trangThai) + (lh.canDoiLich ? ' · cần đổi lịch' : '') + (lh.lyDoHuy ? ` · ${lh.lyDoHuy}` : ''),
        ]))));
    }

    async function taiChoXacMinh(imLang = false) {
        const { status, json } = await lay(`${HO_SO}/cho-xac-minh?trang=${trang}&kichThuoc=10`, imLang);
        const khung = $('ds-cho-xac-minh');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, 'Không tải được danh sách.'), 'ghi-chu loi'));
            return;
        }
        const duLieu = json.duLieu;
        tongSoTrang = duLieu.tongSoTrang;
        $('trang-cho-xac-minh').textContent = `Trang ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} hồ sơ`;
        if (!duLieu.noiDung.length) {
            khung.replaceChildren(ghiChu('No data yet — không có hồ sơ nào đang chờ xác minh.'));
            return;
        }
        khung.replaceChildren(bang(['Hồ sơ', 'CCCD', 'Số lịch hẹn', 'Tài khoản đang chờ', 'Khớp', 'Thao tác'], duLieu.noiDung.map((muc) => {
            const hs = muc.hoSo, tk = muc.taiKhoan;
            return dong([
                `#${hs.id} ${hs.hoTen} · ${hs.ngaySinh ? ngayVn(hs.ngaySinh) : '?'} · ${hs.soDienThoai || ''}`, hs.cccd, hs.soLichHen,
                `#${tk.id} ${tk.hoTen} · ${tk.email} · ${tk.soDienThoai || ''}`,
                `Họ tên: ${muc.khopHoTen ? 'khớp' : 'KHÁC'} · SĐT: ${muc.khopSoDienThoai ? 'khớp' : 'KHÁC'}`,
                [nut('Duyệt', () => xuLy(hs.id, 'duyet')), nut('Từ chối', () => xuLy(hs.id, 'tu-choi'), 'nguy-hiem nho'), nut('Mở hồ sơ', () => moHoSo(hs.id), 'phu nho')],
            ]);
        })));
    }

    async function xuLy(id, hanhDong) {
        const lyDo = $('ly-do-tu-choi').value.trim();
        const { status, json } = hanhDong === 'duyet'
            ? await goi(`${HO_SO}/${id}/duyet`)
            : await goi(`${HO_SO}/${id}/tu-choi`, lyDo ? { lyDo } : undefined);
        bao('cho-xac-minh-trang-thai', status === 200 ? json.thongDiep : thongDiepLoi(json, 'Không thực hiện được.'), status === 200 ? 'tot' : 'loi');
        if (status === 200) {
            $('ly-do-tu-choi').value = '';
            await taiChoXacMinh(true);
        }
    }

    async function moHoSo(id) {
        const { status, json } = await lay(`${HO_SO}/${id}`);
        if (!$('khoi-ho-so-qt')) return;
        $('khoi-ho-so-qt').classList.remove('an');
        if (status !== 200) {
            hoSoDangSua = null;
            $('ho-so-qt-thong-tin').replaceChildren(the('p', thongDiepLoi(json, 'Không mở được hồ sơ bệnh nhân.'), 'ghi-chu loi'));
            $('form-sua-ho-so-qt').classList.add('an');
            return;
        }
        hienHoSo(json.duLieu);
        trangLichHen = 0;
        await taiLichHen(true);
        $('khoi-ho-so-qt').scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function hienHoSo(hs) {
        hoSoDangSua = hs;
        $('ho-so-qt-thong-tin').replaceChildren(thongTin([
            ['Id hồ sơ', hs.id], ['Liên kết tài khoản', LIEN_KET[hs.trangThaiLienKet] || hs.trangThaiLienKet],
            ['Id tài khoản', hs.idTaiKhoan], ['Ngày tạo', hs.ngayTao ? ngayGioVn(hs.ngayTao) : null],
        ]));
        const f = $('form-sua-ho-so-qt');
        f.classList.remove('an');
        for (const ten of TRUONG) f.elements[ten].value = hs[ten] || '';
        // Số CCCD chỉ điền được khi hồ sơ chưa có
        f.elements.cccd.disabled = !!hs.cccd;
    }

    dangKy({
        duongDan: '/admin/patients',
        vaiTro: 'QUAN_TRI_VIEN',
        tieuDe: 'Patient Management',
        async ve(khung, { thamSo }) {
            hoSoDangSua = null;
            trang = 0;
            trangDs = 0;
            khuon(khung, `
                <section>
                    <h2>Danh sách hồ sơ bệnh nhân</h2>
                    <form id="form-tim-ho-so" class="hai-cot">
                        <div><label>Từ khoá (họ tên, số điện thoại, số bảo hiểm y tế, hoặc đủ 12 số CCCD)</label><input name="tuKhoa" maxlength="100"></div>
                        <div><label>Liên kết tài khoản</label><select name="trangThaiLienKet"><option value="">(tất cả)</option><option value="CHUA_LIEN_KET">Chưa liên kết</option><option value="CHO_XAC_MINH">Chờ xác minh</option><option value="DA_LIEN_KET">Đã liên kết</option></select></div>
                        <button class="phu">Tìm</button>
                    </form>
                    <div id="ds-ho-so" class="bang"></div>
                    <p id="trang-ho-so" class="ghi-chu"></p>
                    <button id="nut-hs-truoc" class="phu nho">‹ Trang trước</button>
                    <button id="nut-hs-sau" class="phu nho">Trang sau ›</button>
                </section>
                <section>
                    <h2>Hồ sơ chờ xác minh liên kết tài khoản</h2>
                    <input id="ly-do-tu-choi" placeholder="Lý do từ chối (không bắt buộc, nhập trước khi bấm Từ chối)" maxlength="500">
                    <div id="ds-cho-xac-minh" class="bang"></div>
                    <p id="trang-cho-xac-minh" class="ghi-chu"></p>
                    <button id="nut-cxm-truoc" class="phu nho">‹ Trang trước</button>
                    <button id="nut-cxm-sau" class="phu nho">Trang sau ›</button>
                    <p id="cho-xac-minh-trang-thai" class="ghi-chu"></p>
                </section>
                <section>
                    <h2>Mở hồ sơ bệnh nhân theo id</h2>
                    <form id="form-mo-ho-so-qt" class="hai-cot">
                        <div><label>Id hồ sơ (idHoSoBenhNhan của lịch hẹn, xem ở Schedule Management)</label><input name="id" type="number" min="1" required></div>
                        <button class="phu">Mở hồ sơ</button>
                    </form>
                </section>
                <section id="khoi-ho-so-qt" class="an">
                    <h2>Hồ sơ bệnh nhân</h2>
                    <div id="ho-so-qt-thong-tin"></div>
                    <form id="form-sua-ho-so-qt">
                        <div class="hai-cot">
                            <div><label>Họ tên</label><input name="hoTen" required maxlength="150"></div>
                            <div><label>Ngày sinh</label><input name="ngaySinh" type="date" required></div>
                            <div><label>Giới tính</label><select name="gioiTinh"><option value="">(chưa rõ)</option><option value="NAM">Nam</option><option value="NU">Nữ</option><option value="KHAC">Khác</option></select></div>
                            <div><label>Số điện thoại</label><input name="soDienThoai" required pattern="0\\d{9}"></div>
                            <div><label>Số CCCD (chỉ điền được khi hồ sơ chưa có)</label><input name="cccd" pattern="\\d{12}"></div>
                            <div><label>Số bảo hiểm y tế</label><input name="soBaoHiemYTe" maxlength="50"></div>
                        </div>
                        <label>Địa chỉ</label><input name="diaChi" maxlength="500">
                        <button>Lưu hồ sơ bệnh nhân (sau khi đã đối chiếu giấy tờ)</button>
                    </form>
                    <p id="ho-so-qt-trang-thai" class="ghi-chu"></p>
                    <h3>Lịch hẹn của hồ sơ</h3>
                    <div id="ho-so-qt-lich-hen" class="bang"></div>
                    <p id="trang-lich-hen-ho-so" class="ghi-chu"></p>
                    <button id="nut-lh-truoc" class="phu nho">‹ Trang trước</button>
                    <button id="nut-lh-sau" class="phu nho">Trang sau ›</button>
                </section>`);

            $('form-tim-ho-so').addEventListener('submit', (e) => {
                e.preventDefault();
                trangDs = 0;
                taiDanhSach();
            });
            $('nut-hs-truoc').addEventListener('click', () => {
                if (trangDs > 0) {
                    trangDs--;
                    taiDanhSach();
                }
            });
            $('nut-hs-sau').addEventListener('click', () => {
                if (trangDs + 1 < tongSoTrangDs) {
                    trangDs++;
                    taiDanhSach();
                }
            });
            $('nut-lh-truoc').addEventListener('click', () => {
                if (trangLichHen > 0) {
                    trangLichHen--;
                    taiLichHen();
                }
            });
            $('nut-lh-sau').addEventListener('click', () => {
                if (trangLichHen + 1 < tongSoTrangLichHen) {
                    trangLichHen++;
                    taiLichHen();
                }
            });

            $('nut-cxm-truoc').addEventListener('click', () => {
                if (trang > 0) {
                    trang--;
                    taiChoXacMinh();
                }
            });
            $('nut-cxm-sau').addEventListener('click', () => {
                if (trang + 1 < tongSoTrang) {
                    trang++;
                    taiChoXacMinh();
                }
            });
            khiGui('form-mo-ho-so-qt', (v) => moHoSo(v.id));
            $('form-sua-ho-so-qt').addEventListener('submit', async (e) => {
                e.preventDefault();
                const { status, json } = await goi(`${HO_SO}/${hoSoDangSua.id}`, thanForm(e.target, TRUONG), 'PUT');
                if (status !== 200) {
                    bao('ho-so-qt-trang-thai', thongDiepLoi(json, 'Không lưu được hồ sơ bệnh nhân.'), 'loi');
                    return;
                }
                hienHoSo(json.duLieu);
                bao('ho-so-qt-trang-thai', json.thongDiep || 'Đã cập nhật hồ sơ bệnh nhân.', 'tot');
                await taiDanhSach(true);
            });

            await taiDanhSach(true);
            await taiChoXacMinh(true);
            if (/^\d+$/.test(thamSo.get('id') || '')) await moHoSo(thamSo.get('id'));
        },
    });
})();
