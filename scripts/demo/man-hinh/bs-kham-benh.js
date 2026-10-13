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

    const KHUNG = () => `
        <section>
            <h2>${t('Mở hồ sơ khám bằng mã hoặc QR', 'Open a medical record by code or QR')}</h2>
            <form id="form-ma" class="hai-cot">
                <div><label>${t('Mã tra cứu (ECL-…), mã phiếu khám, hoặc cả link phiếu khám', 'Lookup code (ECL-…), ticket code, or the whole appointment ticket link')}</label><input name="ma" required></div>
                <button>${t('Mở hồ sơ khám', 'Open medical record')}</button>
            </form>
            <div class="hai-cot">
                <button type="button" id="nut-quet" class="phu">${t('Quét QR bằng camera', 'Scan QR with the camera')}</button>
                <div><label>${t('Hoặc chọn ảnh mã QR', 'Or choose a QR code image')}</label><input type="file" id="anh-qr" accept="image/*"></div>
            </div>
            <video id="camera" class="an" playsinline muted></video>
            <p id="qr-trang-thai" class="ghi-chu"></p>
        </section>
        <section>
            <h2>${t('Danh sách bệnh nhân trong ngày', 'Patients of the day')}</h2>
            <form id="form-danh-sach" class="hai-cot">
                <div><label>${t('Ngày', 'Date')}</label><input name="ngay" type="date"></div>
                <div><label>${t('Ca (id ca làm việc, bỏ trống = cả ngày)', 'Shift (shift id, empty = whole day)')}</label><input name="idLichLamViec" type="number" min="1"></div>
                <div><label>${t('Số thứ tự', 'Queue number')}</label><input name="soThuTu" type="number" min="1"></div>
                <div><label>${t('Họ tên (không cần dấu)', 'Full name (diacritics not needed)')}</label><input name="tuKhoa"></div>
                <button>${t('Tìm', 'Search')}</button>
            </form>
            <div id="bs-danh-sach" class="bang"></div>
        </section>
        <section id="bs-khoi-ho-so" class="an">
            <h2>${t('Hồ sơ khám', 'Medical record')}</h2>
            <p id="bs-ho-so-trang-thai" class="ghi-chu"></p>
            <div id="bs-ho-so"></div>
            <div id="bs-khoi-sua">
            <h3>${t('Sửa hồ sơ bệnh nhân sau khi đối chiếu giấy tờ', 'Edit the patient profile after the identity check')}</h3>
            <form id="form-sua-benh-nhan">
                <div class="hai-cot">
                    <div><label>${t('Họ tên', 'Full name')}</label><input name="hoTen" required maxlength="150"></div>
                    <div><label>${t('Ngày sinh', 'Date of birth')}</label><input name="ngaySinh" type="date" required></div>
                    <div><label>${t('Giới tính', 'Gender')}</label><select name="gioiTinh"><option value="">${t('(chưa rõ)', '(unknown)')}</option><option value="NAM">${t('Nam', 'Male')}</option><option value="NU">${t('Nữ', 'Female')}</option><option value="KHAC">${t('Khác', 'Other')}</option></select></div>
                    <div><label>${t('Số điện thoại', 'Phone number')}</label><input name="soDienThoai" required pattern="0\\d{9}"></div>
                    <div><label>${t('Số CCCD (chỉ điền được khi hồ sơ chưa có)', 'National ID (can only be filled in when the profile has none)')}</label><input name="cccd" pattern="\\d{12}"></div>
                    <div><label>${t('Số thẻ bảo hiểm y tế', 'Health insurance number')}</label><input name="soBaoHiemYTe" maxlength="50"></div>
                </div>
                <label>${t('Địa chỉ', 'Address')}</label><input name="diaChi" maxlength="500">
                <div class="hai-cot">
                    <button type="button" id="nut-dien-da-nhap" class="phu">${t('Điền theo thông tin người đặt đã nhập', 'Fill in from what the booker entered')}</button>
                    <button>${t('Lưu hồ sơ bệnh nhân', 'Save patient profile')}</button>
                    <button type="button" id="nut-da-doi-chieu" class="phu">${t('Đã đối chiếu giấy tờ', 'Identity checked')}</button>
                </div>
            </form>
            <h3 id="tieu-de-ket-qua">${t('Ghi kết quả khám', 'Record the visit result')}</h3>
            <form id="form-ket-qua">
                <label>${t('Chẩn đoán', 'Diagnosis')}</label><textarea name="chanDoan" rows="2" required maxlength="5000"></textarea>
                <label>${t('Lời dặn', 'Doctor\'s advice')}</label><textarea name="ghiChu" rows="2" maxlength="5000"></textarea>
                <div class="hai-cot"><div><label>${t('Ngày tái khám đề xuất', 'Suggested follow-up date')}</label><input name="ngayTaiKhamDeXuat" type="date"></div></div>
                <label>${t('Đơn thuốc (tên thuốc · đơn vị · liều dùng · lần/ngày · số ngày · ghi chú)', 'Prescription (medicine name · unit · dosage · times/day · days · note)')}</label>
                <div id="don-thuoc"></div>
                <datalist id="goi-y-thuoc"></datalist>
                <div class="hai-cot"><button type="button" id="nut-them-thuoc" class="phu">${t('Thêm dòng thuốc', 'Add a medicine row')}</button><button id="nut-luu-ket-qua">${t('Lưu kết quả khám', 'Save visit result')}</button></div>
            </form>
            <p id="bs-ket-qua-trang-thai" class="ghi-chu"></p>
            <h3>${t('Các lần khám trước', 'Previous visits')}</h3>
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
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được danh sách.', 'Could not load the list.')), 'ghi-chu loi'));
            return;
        }
        if (!json.duLieu.length) {
            khung.replaceChildren(ghiChu(t('Không có lịch hẹn nào khớp.', 'No matching appointments.')));
            return;
        }
        khung.replaceChildren(bang([t('STT', 'No.'), t('Giờ', 'Time'), t('Bệnh nhân', 'Patient'), t('Tuổi', 'Age'), t('Lý do khám', 'Reason for visit'),
            t('Trạng thái', 'Status'), t('Mã tra cứu', 'Lookup code')], json.duLieu.map((lich) => {
            const hang = dong([lich.soThuTu, gio(lich.gioKhamDuKien), lich.benhNhan.hoTen + (lich.doiChieu.canDoiChieu ? t(' ⚠ cần đối chiếu', ' ⚠ identity check needed') : ''),
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
            bao('bs-ho-so-trang-thai', thongDiepLoi(json, t('Không mở được hồ sơ khám.', 'Could not open the medical record.')), 'loi');
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
            cacPhan.push(the('p', t('Thông tin người đặt nhập KHÁC hồ sơ bệnh nhân: đối chiếu giấy tờ, sửa hồ sơ nếu hồ sơ sai, rồi bấm "Đã đối chiếu giấy tờ".',
                'What the booker entered DIFFERS from the patient profile: check the identity papers, fix the profile if it is wrong, then press "Identity checked".'), 'canh-bao'));
        }
        cacPhan.push(the('h3', t('Lịch hẹn', 'Appointment')), thongTin([
            [t('Mã tra cứu', 'Lookup code'), lich.maTraCuu], [t('Số thứ tự', 'Queue number'), lich.soThuTu], [t('Giờ khám dự kiến', 'Expected visit time'), ngayGioVn(lich.gioKhamDuKien)],
            [t('Trạng thái', 'Status'), TRANG_THAI[lich.trangThai] || lich.trangThai], [t('Lý do từ chối / hủy', 'Reason for declining / cancelling'), lich.lyDoHuy], [t('Phòng khám', 'Room'), lich.phongKham ? lich.phongKham.tenPhong : null],
            [t('Lý do khám', 'Reason for visit'), lich.lyDoKham], [t('SĐT liên hệ', 'Contact phone'), lich.benhNhan.soDienThoai],
            [t('Người giám hộ', 'Guardian'), lich.nguoiGiamHo ? `${lich.nguoiGiamHo.hoTen} (${lich.nguoiGiamHo.soDienThoai})` : null],
        ]));
        cacPhan.push(the('h3', t('Hồ sơ bệnh nhân (đang lưu)', 'Patient profile (as stored)')), thongTin([
            [t('Họ tên', 'Full name'), bn.hoTen], [t('Số CCCD', 'National ID'), bn.cccd || t('(chưa có)', '(none)')],
            [t('Ngày sinh', 'Date of birth'), bn.ngaySinh ? t(`${ngayVn(bn.ngaySinh)} (${bn.tuoi} tuổi)`, `${ngayVn(bn.ngaySinh)} (age ${bn.tuoi})`) : null],
            [t('Giới tính', 'Gender'), GIOI_TINH[bn.gioiTinh]], [t('Số điện thoại', 'Phone number'), bn.soDienThoai], [t('Địa chỉ', 'Address'), bn.diaChi], [t('Số thẻ BHYT', 'Health insurance number'), bn.soBaoHiemYTe],
            [t('Tiền sử bệnh lý', 'Medical history'), bn.tienSuBenhLy],
            [t('Tài khoản', 'Account'), bn.daLienKetTaiKhoan ? t('Đã liên kết tài khoản bệnh nhân', 'Linked to a patient account') : t('Chưa liên kết tài khoản', 'Not linked to an account')],
        ]));
        if (dc.hoTenDaNhap) {
            cacPhan.push(the('h3', t('Người đặt đã nhập cho lượt khám này', 'What the booker entered for this visit')), thongTin([
                [t('Họ tên', 'Full name'), dc.hoTenDaNhap], [t('Ngày sinh', 'Date of birth'), dc.ngaySinhDaNhap ? ngayVn(dc.ngaySinhDaNhap) : null],
                [t('Giới tính', 'Gender'), GIOI_TINH[dc.gioiTinhDaNhap]], [t('Người giám hộ', 'Guardian'), dc.hoTenGiamHoDaNhap],
                [t('Cần đối chiếu', 'Identity check needed'), dc.canDoiChieu ? t('Có', 'Yes') : t('Không', 'No')],
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
        $('tieu-de-ket-qua').textContent = kq ? t('Kết quả khám đã ghi (sửa được)', 'Recorded visit result (editable)') : t('Ghi kết quả khám', 'Record the visit result');
        fk.classList.toggle('an', !kq && !khamDuoc);
        if (!kq && !khamDuoc) {
            bao('bs-ket-qua-trang-thai', t('Lịch hẹn đã hủy hoặc bị từ chối: không ghi kết quả khám được.',
                'The appointment was cancelled or declined: a visit result cannot be recorded.'));
        }
        fk.elements.chanDoan.value = kq ? kq.chanDoan : '';
        fk.elements.ghiChu.value = kq && kq.ghiChu ? kq.ghiChu : '';
        fk.elements.ngayTaiKhamDeXuat.value = kq && kq.ngayTaiKhamDeXuat ? kq.ngayTaiKhamDeXuat : '';
        $('don-thuoc').replaceChildren();
        (kq ? kq.donThuoc : []).forEach(themDongThuoc);
        $('nut-luu-ket-qua').textContent = kq ? t('Lưu thay đổi', 'Save changes')
            : t('Lưu kết quả khám (lịch hẹn chuyển sang đã hoàn thành)', 'Save visit result (the appointment becomes completed)');

        const truoc = hs.lanKhamTruoc.map((lan) => {
            const khoi = the('div', null, 'lan-kham');
            khoi.append(the('b', `${ngayGioVn(lan.gioKhamDuKien)} · ${t('BS', 'Dr.')} ${lan.bacSi.hoTen} (${lan.tenChuyenKhoa}) · ${TRANG_THAI[lan.trangThai] || lan.trangThai}`),
                the('p', lan.lyDoKham ? t('Lý do khám: ', 'Reason for visit: ') + lan.lyDoKham : null, 'ghi-chu'),
                lan.ketQua ? ketQuaKham(lan.ketQua) : ghiChu(t('Chưa có kết quả khám.', 'No visit result yet.')));
            return khoi;
        });
        $('bs-lan-kham-truoc').replaceChildren(...(truoc.length ? truoc
            : [ghiChu(t('Bệnh nhân chưa có lần khám nào trước lượt này.', 'The patient has no visits before this one.'))]));
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
        const muc = thuoc || {};
        const ten = o('tenThuoc', muc.tenThuoc, null, t('Tên thuốc', 'Medicine name'));
        ten.setAttribute('list', 'goi-y-thuoc');
        ten.addEventListener('input', () => goiYThuoc(ten.value));
        hang.append(ten, o('donVi', muc.donVi, null, t('Đơn vị', 'Unit')), o('lieuDung', muc.lieuDung, null, t('Liều dùng', 'Dosage')),
            o('soLanMoiNgay', muc.soLanMoiNgay, 'number', t('Lần/ngày', 'Times/day')),
            o('soNgayDung', muc.soNgayDung, 'number', t('Số ngày', 'Days')), o('ghiChuSuDung', muc.ghiChuSuDung, null, t('Ghi chú', 'Note')),
            nut(t('Xóa', 'Remove'), () => hang.remove()));
        $('don-thuoc').append(hang);
    }
    function goiYThuoc(tuKhoa) {
        clearTimeout(henGoiY);
        if (tuKhoa.trim().length < 2) return;
        henGoiY = setTimeout(async () => {
            const { status, json } = await lay('/api/medical/bac-si/toi/thuoc?tuKhoa=' + encodeURIComponent(tuKhoa.trim()), true);
            if (status !== 200 || !$('goi-y-thuoc')) return;
            $('goi-y-thuoc').replaceChildren(...json.duLieu.map((muc) => {
                const lc = document.createElement('option');
                lc.value = muc.tenThuoc;
                lc.label = (muc.donVi || '') + (muc.daXacMinh ? '' : t(' (chưa xác minh)', ' (not verified)'));
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
        bao('qr-trang-thai', t('Đang mở hồ sơ khám theo mã…', 'Opening the medical record by code…'));
        $('form-ma').elements.ma.value = ma;
        // Gửi nguyên chuỗi đọc được: API tự nhận mã tra cứu, mã phiếu khám hoặc cả link phiếu khám
        const duoc = await moHoSoKham('/api/booking/bac-si/toi/lich-hen/tra-cuu/ho-so-kham?ma=' + encodeURIComponent(ma));
        bao('qr-trang-thai', duoc ? t('Đã mở hồ sơ khám.', 'Medical record opened.')
            : t('Không tìm thấy lịch hẹn của bạn với mã này.', 'None of your appointments matches this code.'), duoc ? 'tot' : 'loi');
    }
    function tatCamera() {
        clearTimeout(henQuet);
        if (luongCamera) luongCamera.getTracks().forEach((luong) => luong.stop());
        luongCamera = null;
        if ($('camera')) {
            $('camera').classList.add('an');
            $('nut-quet').textContent = t('Quét QR bằng camera', 'Scan QR with the camera');
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
            bao('qr-trang-thai', t('Không mở được camera: ', 'Could not open the camera: ') + String(e), 'loi');
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
        $('nut-quet').textContent = t('Tắt camera', 'Turn off the camera');
        bao('qr-trang-thai', t('Đưa mã QR trên phiếu khám vào khung hình…', 'Hold the QR code of the appointment ticket in the frame…'));
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
        tieuDe: cap('Khám bệnh / bệnh án', 'Consultation / EMR'),
        async ve(khung, { thamSo }) {
            hoSoKham = null;
            khuon(khung, KHUNG());

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
                    else bao('qr-trang-thai', t('Không đọc được mã QR trong ảnh này.', 'Could not read a QR code in this image.'), 'loi');
                } catch (loi) {
                    bao('qr-trang-thai', t('Không đọc được ảnh: ', 'Could not read the image: ') + String(loi.message || loi), 'loi');
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
                    bao('bs-ho-so-trang-thai', thongDiepLoi(json, t('Không lưu được hồ sơ bệnh nhân.', 'Could not save the patient profile.')), 'loi');
                    return;
                }
                hienHoSoKham(json.duLieu);
                bao('bs-ho-so-trang-thai', t('Đã cập nhật hồ sơ bệnh nhân.', 'Patient profile updated.'), 'tot');
            });
            $('nut-da-doi-chieu').addEventListener('click', async () => {
                const id = hoSoKham.lichHen.id;
                const { status, json } = await goi(`/api/booking/bac-si/toi/lich-hen/${id}/da-doi-chieu`);
                if (status !== 200) {
                    bao('bs-ho-so-trang-thai', thongDiepLoi(json, t('Không ghi nhận được.', 'Could not record it.')), 'loi');
                    return;
                }
                await moHoSoKham(duongDanHoSo(id));
                bao('bs-ho-so-trang-thai', t('Đã ghi nhận đối chiếu giấy tờ.', 'Identity check recorded.'), 'tot');
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
                    bao('bs-ket-qua-trang-thai', thongDiepLoi(json, t('Không lưu được kết quả khám.', 'Could not save the visit result.')), 'loi');
                    return;
                }
                await moHoSoKham(duongDanHoSo(id));
                bao('bs-ket-qua-trang-thai', t('Đã lưu kết quả khám.', 'Visit result saved.'), 'tot');
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
