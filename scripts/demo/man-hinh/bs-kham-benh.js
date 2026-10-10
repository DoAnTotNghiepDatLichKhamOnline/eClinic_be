// Màn hình UI: Consultation / EMR (Doctor) — nhập mã hoặc quét QR để mở hồ sơ khám, ghi kết quả khám và đơn thuốc
// Đường dẫn:  /doctor/consultation
//             ?id=<id lịch hẹn>              mở thẳng hồ sơ khám của 1 lịch hẹn
//             ?ngay=YYYY-MM-DD&idLichLamViec=   lọc sẵn danh sách bệnh nhân của 1 ngày / 1 ca
// Vai trò:    BAC_SI
// API:
//   GET  /api/booking/bac-si/toi/lich-hen?ngay=&idLichLamViec=&soThuTu=&tuKhoa=     danh sách bệnh nhân trong ngày
//   GET  /api/booking/bac-si/toi/lich-hen/tra-cuu/ho-so-kham?ma=                    mở hồ sơ khám theo mã tra cứu (ECL-…), mã phiếu
//                                                                                   khám, hoặc cả link phiếu khám đọc từ QR
//   GET  /api/booking/bac-si/toi/lich-hen/{id}/ho-so-kham                           hồ sơ khám: lịch hẹn, hồ sơ bệnh nhân, thông tin
//                                                                                   người đặt đã nhập, kết quả, các lần khám trước
//   PUT  /api/booking/bac-si/toi/lich-hen/{id}/benh-nhan                            sửa hồ sơ bệnh nhân sau khi đối chiếu giấy tờ
//   POST /api/booking/bac-si/toi/lich-hen/{id}/da-doi-chieu                         bỏ đánh dấu "cần đối chiếu"
//   POST /api/medical/bac-si/toi/lich-hen/{id}/benh-an                              ghi kết quả khám (lịch hẹn -> DA_HOAN_THANH)
//   PUT  /api/medical/bac-si/toi/lich-hen/{id}/benh-an                              sửa kết quả khám đã ghi
//   GET  /api/medical/bac-si/toi/thuoc?tuKhoa=                                      gợi ý tên thuốc khi kê đơn
// QR: dùng BarcodeDetector nếu trình duyệt có, không thì thư viện jsQR tải từ CDN (cần internet).
(() => {
    let hoSoKham = null;                   // hồ sơ khám đang mở
    let luongCamera = null, henQuet = null, henGoiY = null;

    const KHUNG = `
        <section>
            <h2>Mở hồ sơ khám bằng mã hoặc QR</h2>
            <form id="form-ma" class="hai-cot">
                <div><label>Mã tra cứu (ECL-…), mã phiếu khám, hoặc cả link phiếu khám</label><input name="ma" required></div>
                <button>Mở hồ sơ khám</button>
            </form>
            <div class="hai-cot">
                <button type="button" id="nut-quet" class="phu">Quét QR bằng camera</button>
                <div><label>Hoặc chọn ảnh mã QR</label><input type="file" id="anh-qr" accept="image/*"></div>
            </div>
            <video id="camera" class="an" playsinline muted></video>
            <p id="qr-trang-thai" class="ghi-chu"></p>
        </section>
        <section>
            <h2>Danh sách bệnh nhân trong ngày</h2>
            <form id="form-danh-sach" class="hai-cot">
                <div><label>Ngày</label><input name="ngay" type="date"></div>
                <div><label>Ca (id ca làm việc, bỏ trống = cả ngày)</label><input name="idLichLamViec" type="number" min="1"></div>
                <div><label>Số thứ tự</label><input name="soThuTu" type="number" min="1"></div>
                <div><label>Họ tên (không cần dấu)</label><input name="tuKhoa"></div>
                <button>Tìm</button>
            </form>
            <div id="bs-danh-sach" class="bang"></div>
        </section>
        <section id="bs-khoi-ho-so" class="an">
            <h2>Hồ sơ khám</h2>
            <p id="bs-ho-so-trang-thai" class="ghi-chu"></p>
            <div id="bs-ho-so"></div>
            <div id="bs-khoi-sua">
            <h3>Sửa hồ sơ bệnh nhân sau khi đối chiếu giấy tờ</h3>
            <form id="form-sua-benh-nhan">
                <div class="hai-cot">
                    <div><label>Họ tên</label><input name="hoTen" required maxlength="150"></div>
                    <div><label>Ngày sinh</label><input name="ngaySinh" type="date" required></div>
                    <div><label>Giới tính</label><select name="gioiTinh"><option value="">(chưa rõ)</option><option value="NAM">Nam</option><option value="NU">Nữ</option><option value="KHAC">Khác</option></select></div>
                    <div><label>Số điện thoại</label><input name="soDienThoai" required pattern="0\\d{9}"></div>
                    <div><label>Số CCCD (chỉ điền được khi hồ sơ chưa có)</label><input name="cccd" pattern="\\d{12}"></div>
                    <div><label>Số thẻ bảo hiểm y tế</label><input name="soBaoHiemYTe" maxlength="50"></div>
                </div>
                <label>Địa chỉ</label><input name="diaChi" maxlength="500">
                <div class="hai-cot">
                    <button type="button" id="nut-dien-da-nhap" class="phu">Điền theo thông tin người đặt đã nhập</button>
                    <button>Lưu hồ sơ bệnh nhân</button>
                    <button type="button" id="nut-da-doi-chieu" class="phu">Đã đối chiếu giấy tờ</button>
                </div>
            </form>
            <h3 id="tieu-de-ket-qua">Ghi kết quả khám</h3>
            <form id="form-ket-qua">
                <label>Chẩn đoán</label><textarea name="chanDoan" rows="2" required maxlength="5000"></textarea>
                <label>Lời dặn</label><textarea name="ghiChu" rows="2" maxlength="5000"></textarea>
                <div class="hai-cot"><div><label>Ngày tái khám đề xuất</label><input name="ngayTaiKhamDeXuat" type="date"></div></div>
                <label>Đơn thuốc (tên thuốc · đơn vị · liều dùng · lần/ngày · số ngày · ghi chú)</label>
                <div id="don-thuoc"></div>
                <datalist id="goi-y-thuoc"></datalist>
                <div class="hai-cot"><button type="button" id="nut-them-thuoc" class="phu">Thêm dòng thuốc</button><button id="nut-luu-ket-qua">Lưu kết quả khám</button></div>
            </form>
            <p id="bs-ket-qua-trang-thai" class="ghi-chu"></p>
            <h3>Các lần khám trước</h3>
            <div id="bs-lan-kham-truoc"></div>
            </div>
        </section>`;

    const duongDanHoSo = (id) => `/api/booking/bac-si/toi/lich-hen/${id}/ho-so-kham`;

    async function taiDanhSach() {
        const f = $('form-danh-sach');
        const thamSo = new URLSearchParams();
        for (const ten of ['ngay', 'idLichLamViec', 'soThuTu', 'tuKhoa']) {
            if (f.elements[ten].value.trim()) thamSo.set(ten, f.elements[ten].value.trim());
        }
        const { status, json } = await lay('/api/booking/bac-si/toi/lich-hen?' + thamSo.toString());
        const khung = $('bs-danh-sach');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, 'Không tải được danh sách.'), 'ghi-chu loi'));
            return;
        }
        if (!json.duLieu.length) {
            khung.replaceChildren(ghiChu('Không có lịch hẹn nào khớp.'));
            return;
        }
        khung.replaceChildren(bang(['STT', 'Giờ', 'Bệnh nhân', 'Tuổi', 'Lý do khám', 'Trạng thái', 'Mã tra cứu'], json.duLieu.map((lich) => {
            const hang = dong([lich.soThuTu, gio(lich.gioKhamDuKien), lich.benhNhan.hoTen + (lich.doiChieu.canDoiChieu ? ' ⚠ cần đối chiếu' : ''),
                lich.benhNhan.tuoi, lich.lyDoKham, TRANG_THAI[lich.trangThai] || lich.trangThai, lich.maTraCuu], 'bam');
            hang.addEventListener('click', () => moHoSoKham(duongDanHoSo(lich.id)));
            return hang;
        })));
    }

    async function moHoSoKham(duongDan) {
        const { status, json } = await lay(duongDan);
        if (!$('bs-khoi-ho-so')) return false;
        $('bs-khoi-ho-so').classList.remove('an');
        if (status !== 200) {
            hoSoKham = null;
            bao('bs-ho-so-trang-thai', thongDiepLoi(json, 'Không mở được hồ sơ khám.'), 'loi');
            $('bs-ho-so').replaceChildren();
            $('bs-khoi-sua').classList.add('an');
            return false;
        }
        hienHoSoKham(json.duLieu);
        $('bs-khoi-ho-so').scrollIntoView({ behavior: 'smooth', block: 'start' });
        return true;
    }

    function hienHoSoKham(hs) {
        hoSoKham = hs;
        const lich = hs.lichHen, bn = hs.hoSoBenhNhan, dc = lich.doiChieu;
        $('bs-khoi-sua').classList.remove('an');
        bao('bs-ho-so-trang-thai', '');
        bao('bs-ket-qua-trang-thai', '');
        const cacPhan = [];
        if (dc.canDoiChieu) {
            cacPhan.push(the('p', 'Thông tin người đặt nhập KHÁC hồ sơ bệnh nhân: đối chiếu giấy tờ, sửa hồ sơ nếu hồ sơ sai, rồi bấm "Đã đối chiếu giấy tờ".', 'canh-bao'));
        }
        cacPhan.push(the('h3', 'Lịch hẹn'), thongTin([
            ['Mã tra cứu', lich.maTraCuu], ['Số thứ tự', lich.soThuTu], ['Giờ khám dự kiến', ngayGioVn(lich.gioKhamDuKien)],
            ['Trạng thái', TRANG_THAI[lich.trangThai] || lich.trangThai], ['Lý do từ chối / hủy', lich.lyDoHuy], ['Phòng khám', lich.phongKham ? lich.phongKham.tenPhong : null],
            ['Lý do khám', lich.lyDoKham], ['SĐT liên hệ', lich.benhNhan.soDienThoai],
            ['Người giám hộ', lich.nguoiGiamHo ? `${lich.nguoiGiamHo.hoTen} (${lich.nguoiGiamHo.soDienThoai})` : null],
        ]));
        cacPhan.push(the('h3', 'Hồ sơ bệnh nhân (đang lưu)'), thongTin([
            ['Họ tên', bn.hoTen], ['Số CCCD', bn.cccd || '(chưa có)'], ['Ngày sinh', bn.ngaySinh ? `${ngayVn(bn.ngaySinh)} (${bn.tuoi} tuổi)` : null],
            ['Giới tính', GIOI_TINH[bn.gioiTinh]], ['Số điện thoại', bn.soDienThoai], ['Địa chỉ', bn.diaChi], ['Số thẻ BHYT', bn.soBaoHiemYTe],
            ['Tiền sử bệnh lý', bn.tienSuBenhLy], ['Tài khoản', bn.daLienKetTaiKhoan ? 'Đã liên kết tài khoản bệnh nhân' : 'Chưa liên kết tài khoản'],
        ]));
        if (dc.hoTenDaNhap) {
            cacPhan.push(the('h3', 'Người đặt đã nhập cho lượt khám này'), thongTin([
                ['Họ tên', dc.hoTenDaNhap], ['Ngày sinh', dc.ngaySinhDaNhap ? ngayVn(dc.ngaySinhDaNhap) : null],
                ['Giới tính', GIOI_TINH[dc.gioiTinhDaNhap]], ['Người giám hộ', dc.hoTenGiamHoDaNhap],
                ['Cần đối chiếu', dc.canDoiChieu ? 'Có' : 'Không'],
            ]));
        }
        $('bs-ho-so').replaceChildren(...cacPhan);

        const f = $('form-sua-benh-nhan');
        for (const ten of ['hoTen', 'ngaySinh', 'gioiTinh', 'soDienThoai', 'soBaoHiemYTe', 'diaChi']) f.elements[ten].value = bn[ten] || '';
        f.elements.cccd.value = bn.cccd || '';
        f.elements.cccd.disabled = !!bn.cccd;
        $('nut-dien-da-nhap').disabled = !dc.hoTenDaNhap;
        $('nut-da-doi-chieu').disabled = !dc.canDoiChieu;

        // Kết quả khám: chưa có thì ghi mới (POST), có rồi thì sửa (PUT)
        const kq = hs.ketQua;
        const fk = $('form-ket-qua');
        const khamDuoc = ['CHO_XAC_NHAN', 'DA_XAC_NHAN'].includes(lich.trangThai);
        $('tieu-de-ket-qua').textContent = kq ? 'Kết quả khám đã ghi (sửa được)' : 'Ghi kết quả khám';
        fk.classList.toggle('an', !kq && !khamDuoc);
        if (!kq && !khamDuoc) bao('bs-ket-qua-trang-thai', 'Lịch hẹn đã hủy hoặc bị từ chối: không ghi kết quả khám được.');
        fk.elements.chanDoan.value = kq ? kq.chanDoan : '';
        fk.elements.ghiChu.value = kq && kq.ghiChu ? kq.ghiChu : '';
        fk.elements.ngayTaiKhamDeXuat.value = kq && kq.ngayTaiKhamDeXuat ? kq.ngayTaiKhamDeXuat : '';
        $('don-thuoc').replaceChildren();
        (kq ? kq.donThuoc : []).forEach(themDongThuoc);
        $('nut-luu-ket-qua').textContent = kq ? 'Lưu thay đổi' : 'Lưu kết quả khám (lịch hẹn chuyển sang đã hoàn thành)';

        const truoc = hs.lanKhamTruoc.map((lan) => {
            const khoi = the('div', null, 'lan-kham');
            khoi.append(the('b', `${ngayGioVn(lan.gioKhamDuKien)} · BS ${lan.bacSi.hoTen} (${lan.tenChuyenKhoa}) · ${TRANG_THAI[lan.trangThai] || lan.trangThai}`),
                the('p', lan.lyDoKham ? 'Lý do khám: ' + lan.lyDoKham : null, 'ghi-chu'),
                lan.ketQua ? ketQuaKham(lan.ketQua) : ghiChu('Chưa có kết quả khám.'));
            return khoi;
        });
        $('bs-lan-kham-truoc').replaceChildren(...(truoc.length ? truoc : [ghiChu('Bệnh nhân chưa có lần khám nào trước lượt này.')]));
    }

    // ===== Đơn thuốc =====
    function themDongThuoc(thuoc) {
        const hang = the('div', null, 'dong-thuoc');
        const o = (ten, giaTri, kieu, goiY) => {
            const i = document.createElement('input');
            i.dataset.truong = ten;
            i.placeholder = goiY;
            if (kieu) { i.type = kieu; i.min = '1'; }
            i.value = giaTri === null || giaTri === undefined ? '' : giaTri;
            return i;
        };
        const t = thuoc || {};
        const ten = o('tenThuoc', t.tenThuoc, null, 'Tên thuốc');
        ten.setAttribute('list', 'goi-y-thuoc');
        ten.addEventListener('input', () => goiYThuoc(ten.value));
        hang.append(ten, o('donVi', t.donVi, null, 'Đơn vị'), o('lieuDung', t.lieuDung, null, 'Liều dùng'), o('soLanMoiNgay', t.soLanMoiNgay, 'number', 'Lần/ngày'),
            o('soNgayDung', t.soNgayDung, 'number', 'Số ngày'), o('ghiChuSuDung', t.ghiChuSuDung, null, 'Ghi chú'), nut('Xóa', () => hang.remove()));
        $('don-thuoc').append(hang);
    }
    function goiYThuoc(tuKhoa) {
        clearTimeout(henGoiY);
        if (tuKhoa.trim().length < 2) return;
        henGoiY = setTimeout(async () => {
            const { status, json } = await lay('/api/medical/bac-si/toi/thuoc?tuKhoa=' + encodeURIComponent(tuKhoa.trim()), true);
            if (status !== 200 || !$('goi-y-thuoc')) return;
            $('goi-y-thuoc').replaceChildren(...json.duLieu.map((t) => {
                const lc = document.createElement('option');
                lc.value = t.tenThuoc;
                lc.label = (t.donVi || '') + (t.daXacMinh ? '' : ' (chưa xác minh)');
                return lc;
            }));
        }, 250);
    }
    function docDonThuoc() {
        return [...$('don-thuoc').children].map((hang) => {
            const thuoc = {};
            for (const o of hang.querySelectorAll('input')) {
                const giaTri = o.value.trim();
                if (giaTri) thuoc[o.dataset.truong] = o.type === 'number' ? Number(giaTri) : giaTri;
            }
            return thuoc;
        }).filter((thuoc) => Object.keys(thuoc).length);
    }

    // ===== QR: camera, ảnh, hoặc gõ / dán mã =====
    // nguon: <video> hoặc ImageBitmap. Trả chuỗi trong mã QR, hoặc null nếu khung hình không có mã
    async function docQr(nguon, rong, cao) {
        if (!rong || !cao) return null;
        if ('BarcodeDetector' in window) {
            try {
                const ma = await new BarcodeDetector({ formats: ['qr_code'] }).detect(nguon);
                if (ma.length) return ma[0].rawValue;
                return null;
            } catch { /* trình duyệt có lớp nhưng không đọc được QR: dùng jsQR */ }
        }
        await napThuVien('https://cdn.jsdelivr.net/npm/jsqr@1.4.0/dist/jsQR.js');
        const khungVe = document.createElement('canvas');
        khungVe.width = rong;
        khungVe.height = cao;
        const ve = khungVe.getContext('2d', { willReadFrequently: true });
        ve.drawImage(nguon, 0, 0, rong, cao);
        const diemAnh = ve.getImageData(0, 0, rong, cao);
        const ketQua = window.jsQR(diemAnh.data, rong, cao);
        return ketQua ? ketQua.data : null;
    }
    async function moTheoMa(ma) {
        bao('qr-trang-thai', 'Đang mở hồ sơ khám theo mã…');
        $('form-ma').elements.ma.value = ma;
        // Gửi nguyên chuỗi đọc được: API tự nhận mã tra cứu, mã phiếu khám hoặc cả link phiếu khám
        const duoc = await moHoSoKham('/api/booking/bac-si/toi/lich-hen/tra-cuu/ho-so-kham?ma=' + encodeURIComponent(ma));
        bao('qr-trang-thai', duoc ? 'Đã mở hồ sơ khám.' : 'Không tìm thấy lịch hẹn của bạn với mã này.', duoc ? 'tot' : 'loi');
    }
    function tatCamera() {
        clearTimeout(henQuet);
        if (luongCamera) luongCamera.getTracks().forEach((t) => t.stop());
        luongCamera = null;
        if ($('camera')) {
            $('camera').classList.add('an');
            $('nut-quet').textContent = 'Quét QR bằng camera';
        }
    }
    async function batCamera() {
        if (luongCamera) {
            tatCamera();
            return;
        }
        try {
            luongCamera = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
        } catch (e) {
            bao('qr-trang-thai', 'Không mở được camera: ' + String(e), 'loi');
            return;
        }
        const video = $('camera');
        if (!video) {
            tatCamera();
            return;
        }
        video.srcObject = luongCamera;
        video.classList.remove('an');
        await video.play();
        $('nut-quet').textContent = 'Tắt camera';
        bao('qr-trang-thai', 'Đưa mã QR trên phiếu khám vào khung hình…');
        const quet = async () => {
            if (!luongCamera) return;
            let ma = null;
            try {
                ma = await docQr(video, video.videoWidth, video.videoHeight);
            } catch (e) {
                bao('qr-trang-thai', String(e.message || e), 'loi');
                tatCamera();
                return;
            }
            if (ma) {
                tatCamera();
                await moTheoMa(ma);
            } else {
                henQuet = setTimeout(quet, 300);
            }
        };
        quet();
    }

    dangKy({
        duongDan: '/doctor/consultation',
        vaiTro: 'BAC_SI',
        tieuDe: 'Consultation / EMR',
        async ve(khung, { thamSo }) {
            hoSoKham = null;
            khuon(khung, KHUNG);

            $('form-danh-sach').addEventListener('submit', (e) => { e.preventDefault(); taiDanhSach(); });
            $('form-ma').addEventListener('submit', (e) => { e.preventDefault(); moTheoMa(e.target.elements.ma.value.trim()); });
            $('nut-quet').addEventListener('click', batCamera);
            $('anh-qr').addEventListener('change', async (e) => {
                const tep = e.target.files[0];
                if (!tep) return;
                try {
                    const hinh = await createImageBitmap(tep);
                    const ma = await docQr(hinh, hinh.width, hinh.height);
                    if (ma) await moTheoMa(ma);
                    else bao('qr-trang-thai', 'Không đọc được mã QR trong ảnh này.', 'loi');
                } catch (loi) {
                    bao('qr-trang-thai', 'Không đọc được ảnh: ' + String(loi.message || loi), 'loi');
                }
                e.target.value = '';
            });

            $('nut-dien-da-nhap').addEventListener('click', () => {
                const dc = hoSoKham.lichHen.doiChieu, f = $('form-sua-benh-nhan');
                f.elements.hoTen.value = dc.hoTenDaNhap || f.elements.hoTen.value;
                f.elements.ngaySinh.value = dc.ngaySinhDaNhap || f.elements.ngaySinh.value;
                f.elements.gioiTinh.value = dc.gioiTinhDaNhap || f.elements.gioiTinh.value;
            });
            $('form-sua-benh-nhan').addEventListener('submit', async (e) => {
                e.preventDefault();
                const than = thanForm(e.target, ['hoTen', 'ngaySinh', 'gioiTinh', 'soDienThoai', 'cccd', 'soBaoHiemYTe', 'diaChi']);
                const { status, json } = await goi(`/api/booking/bac-si/toi/lich-hen/${hoSoKham.lichHen.id}/benh-nhan`, than, 'PUT');
                if (status !== 200) {
                    bao('bs-ho-so-trang-thai', thongDiepLoi(json, 'Không lưu được hồ sơ bệnh nhân.'), 'loi');
                    return;
                }
                hienHoSoKham(json.duLieu);
                bao('bs-ho-so-trang-thai', 'Đã cập nhật hồ sơ bệnh nhân.', 'tot');
            });
            $('nut-da-doi-chieu').addEventListener('click', async () => {
                const id = hoSoKham.lichHen.id;
                const { status, json } = await goi(`/api/booking/bac-si/toi/lich-hen/${id}/da-doi-chieu`);
                if (status !== 200) {
                    bao('bs-ho-so-trang-thai', thongDiepLoi(json, 'Không ghi nhận được.'), 'loi');
                    return;
                }
                await moHoSoKham(duongDanHoSo(id));
                bao('bs-ho-so-trang-thai', 'Đã ghi nhận đối chiếu giấy tờ.', 'tot');
                taiDanhSach();
            });
            $('nut-them-thuoc').addEventListener('click', () => themDongThuoc(null));
            $('form-ket-qua').addEventListener('submit', async (e) => {
                e.preventDefault();
                const id = hoSoKham.lichHen.id;
                const than = thanForm(e.target, ['chanDoan', 'ghiChu', 'ngayTaiKhamDeXuat']);
                than.donThuoc = docDonThuoc();
                const { status, json } = await goi(`/api/medical/bac-si/toi/lich-hen/${id}/benh-an`, than, hoSoKham.ketQua ? 'PUT' : 'POST');
                if (status !== 200 && status !== 201) {
                    bao('bs-ket-qua-trang-thai', thongDiepLoi(json, 'Không lưu được kết quả khám.'), 'loi');
                    return;
                }
                await moHoSoKham(duongDanHoSo(id));
                bao('bs-ket-qua-trang-thai', 'Đã lưu kết quả khám.', 'tot');
                taiDanhSach();
            });

            const f = $('form-danh-sach');
            f.elements.ngay.value = thamSo.get('ngay') || congNgay(0);
            f.elements.idLichLamViec.value = thamSo.get('idLichLamViec') || '';
            taiDanhSach();
            if (/^\d+$/.test(thamSo.get('id') || '')) await moHoSoKham(duongDanHoSo(thamSo.get('id')));
            return () => {
                tatCamera();
                clearTimeout(henGoiY);
            };
        },
    });
})();
