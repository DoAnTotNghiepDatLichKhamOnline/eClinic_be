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
//   POST /api/medical/bac-si/toi/lich-hen/{id}/benh-an                              ghi kết quả khám (lịch hẹn -> DA_HOAN_THANH):
//                                                                                   lyDoKham, sinhHieu {mach, nhietDo, huyetApTamThu,
//                                                                                   huyetApTamTruong, nhipTho, canNang, chieuCao},
//                                                                                   khamLamSang, chanDoan, maIcdChinh, chanDoanKemTheo,
//                                                                                   ghiChu, ngayTaiKhamDeXuat, donThuoc[] (mỗi dòng bắt
//                                                                                   buộc soNgayDung, soLuong)
//   PUT  /api/medical/bac-si/toi/lich-hen/{id}/benh-an                              sửa kết quả khám đã ghi: kèm lyDoSua và phienBan
//                                                                                   đang xem; chỉ sửa được trước ketQua.hanSua
//   GET  /api/medical/bac-si/toi/lich-hen/{id}/benh-an/lich-su                      lịch sử sửa của 1 lượt khám (của mình, hoặc lần
//                                                                                   khám trước của bệnh nhân đang mở)
//   GET  /api/medical/bac-si/toi/thuoc?tuKhoa=                                      gợi ý thuốc kê được theo tên hoặc hoạt chất:
//                                                                                   id, tenThuoc, hoatChat, hamLuong, dangBaoChe, duongDung, donVi
//   GET  /api/medical/bac-si/toi/icd10?tuKhoa=                                      gợi ý mã ICD-10 theo đầu mã hoặc tên: ma, ten, manTinh
// Quy tắc của server (Thông tư 26/2025): mỗi thuốc tối đa 30 ngày, 90 khi mã ICD chẩn đoán chính mạn tính; trẻ dưới 72
// tháng có kê thuốc thì phải có cân nặng; huyết áp gửi cả 2 số, tâm trương thấp hơn tâm thu.
// Đơn thuốc chỉ chọn thuốc trong danh mục: mỗi dòng gửi idThuoc (thuốc đã có trong đơn đang sửa giữ nguyên id cũ).
// QR: dùng BarcodeDetector nếu trình duyệt có, không thì thư viện jsQR tải từ CDN (cần internet).
(() => {
    let hoSoKham = null;                   // hồ sơ khám đang mở
    let luongCamera = null, henQuet = null, henGoiY = null;
    const goiYTheoChu = new Map();         // chữ hiện trong ô thuốc -> thuốc gợi ý (để lấy id)
    const icdTheoMa = new Map();           // mã ICD-10 đã gợi ý -> {ma, ten, manTinh}
    let henGoiYIcd = null;
    const SINH_HIEU = ['mach', 'nhietDo', 'huyetApTamThu', 'huyetApTamTruong', 'nhipTho', 'canNang', 'chieuCao'];

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
                <label>${t('Lý do khám, triệu chứng', 'Reason for visit, symptoms')}</label><textarea name="lyDoKham" rows="2" maxlength="2000"></textarea>
                <label>${t('Sinh hiệu (không bắt buộc; trẻ dưới 72 tháng có kê thuốc thì phải có cân nặng)', 'Vital signs (optional; a child under 72 months who gets medicine needs a weight)')}</label>
                <div class="hai-cot">
                    <div><label>${t('Mạch (lần/phút)', 'Pulse (/min)')}</label><input name="mach" type="number" min="20" max="250"></div>
                    <div><label>${t('Nhiệt độ (°C)', 'Temperature (°C)')}</label><input name="nhietDo" type="number" min="30" max="45" step="0.1"></div>
                    <div><label>${t('Huyết áp tâm thu (mmHg)', 'Systolic BP (mmHg)')}</label><input name="huyetApTamThu" type="number" min="50" max="300"></div>
                    <div><label>${t('Huyết áp tâm trương (mmHg)', 'Diastolic BP (mmHg)')}</label><input name="huyetApTamTruong" type="number" min="20" max="200"></div>
                    <div><label>${t('Nhịp thở (lần/phút)', 'Breathing rate (/min)')}</label><input name="nhipTho" type="number" min="5" max="80"></div>
                    <div><label>${t('Cân nặng (kg)', 'Weight (kg)')}</label><input name="canNang" type="number" min="0.3" max="400" step="0.01"></div>
                    <div><label>${t('Chiều cao (cm)', 'Height (cm)')}</label><input name="chieuCao" type="number" min="20" max="250" step="0.1"></div>
                </div>
                <label>${t('Khám lâm sàng', 'Examination findings')}</label><textarea name="khamLamSang" rows="2" maxlength="5000"></textarea>
                <label>${t('Chẩn đoán chính', 'Main diagnosis')}</label><textarea name="chanDoan" rows="2" required maxlength="5000"></textarea>
                <div class="hai-cot">
                    <div><label>${t('Mã ICD-10 (không bắt buộc) — gõ mã hoặc tên bệnh', 'ICD-10 code (optional) — type a code or disease name')}</label><input name="maIcdChinh" list="goi-y-icd" maxlength="10" autocomplete="off"></div>
                    <p id="ten-icd" class="ghi-chu"></p>
                </div>
                <datalist id="goi-y-icd"></datalist>
                <label>${t('Chẩn đoán kèm theo', 'Secondary diagnoses')}</label><textarea name="chanDoanKemTheo" rows="2" maxlength="2000"></textarea>
                <label>${t('Lời dặn', 'Doctor\'s advice')}</label><textarea name="ghiChu" rows="2" maxlength="5000"></textarea>
                <div class="hai-cot"><div><label>${t('Ngày tái khám đề xuất', 'Suggested follow-up date')}</label><input name="ngayTaiKhamDeXuat" type="date"></div></div>
                <label>${t('Đơn thuốc (thuốc trong danh mục · liều dùng · lần/ngày · số ngày* · tổng số lượng* · ghi chú) — gõ tên thuốc hoặc hoạt chất rồi chọn trong danh sách. Tối đa 30 ngày, 90 ngày khi mã ICD chẩn đoán chính là bệnh mạn tính', 'Prescription (catalog medicine · dosage · times/day · days* · total quantity* · note) — type a medicine name or active ingredient, then pick from the list. At most 30 days, or 90 when the main ICD code is a chronic disease')}</label>
                <div id="don-thuoc"></div>
                <datalist id="goi-y-thuoc"></datalist>
                <div id="khoi-ly-do-sua" class="an"><label>${t('Lý do sửa (bắt buộc, lưu vào lịch sử sửa)', 'Reason for the change (required, kept in the edit history)')}</label><textarea name="lyDoSua" rows="2" maxlength="500"></textarea></div>
                <div class="hai-cot"><button type="button" id="nut-them-thuoc" class="phu">${t('Thêm dòng thuốc', 'Add a medicine row')}</button><button id="nut-luu-ket-qua">${t('Lưu kết quả khám', 'Save visit result')}</button></div>
            </form>
            <p id="bs-ket-qua-trang-thai" class="ghi-chu"></p>
            <div id="bs-lich-su-sua"></div>
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
            [t('Trạng thái', 'Status'), TRANG_THAI[lich.trangThai] || lich.trangThai], [t('Lý do hủy', 'Cancellation reason'), lich.lyDoHuy], [t('Phòng khám', 'Room'), lich.phongKham ? lich.phongKham.tenPhong : null],
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
        const khamDuoc = lich.trangThai === 'DA_XAC_NHAN';
        // Quá hạn sửa: chỉ xem (server cũng chặn, 409 HET_HAN_SUA_BENH_AN)
        const daKhoa = !!kq && new Date(kq.hanSua) < new Date();
        $('tieu-de-ket-qua').textContent = !kq ? t('Ghi kết quả khám', 'Record the visit result')
            : daKhoa ? t('Kết quả khám đã ghi (đã khóa, chỉ xem)', 'Recorded visit result (locked, read-only)')
                : t('Kết quả khám đã ghi (sửa được)', 'Recorded visit result (editable)');
        fk.classList.toggle('an', !kq && !khamDuoc);
        if (!kq && !khamDuoc) {
            bao('bs-ket-qua-trang-thai', t('Lịch hẹn đã hủy (bởi bệnh nhân hoặc bác sĩ): không ghi kết quả khám được.',
                'The appointment was cancelled (by the patient or the doctor): a visit result cannot be recorded.'));
        }
        // Lần ghi đầu: điền sẵn lý do khám bệnh nhân ghi khi đặt lịch
        fk.elements.lyDoKham.value = kq ? kq.lyDoKham || '' : lich.lyDoKham || '';
        for (const ten of SINH_HIEU) {
            const giaTri = kq && kq.sinhHieu ? kq.sinhHieu[ten] : null;
            fk.elements[ten].value = giaTri === null || giaTri === undefined ? '' : giaTri;
        }
        fk.elements.khamLamSang.value = kq && kq.khamLamSang ? kq.khamLamSang : '';
        fk.elements.chanDoan.value = kq ? kq.chanDoan : '';
        if (kq && kq.icdChinh) icdTheoMa.set(kq.icdChinh.ma, kq.icdChinh);
        fk.elements.maIcdChinh.value = kq && kq.icdChinh ? kq.icdChinh.ma : '';
        hienTenIcd();
        fk.elements.chanDoanKemTheo.value = kq && kq.chanDoanKemTheo ? kq.chanDoanKemTheo : '';
        fk.elements.ghiChu.value = kq && kq.ghiChu ? kq.ghiChu : '';
        fk.elements.ngayTaiKhamDeXuat.value = kq && kq.ngayTaiKhamDeXuat ? kq.ngayTaiKhamDeXuat : '';
        $('don-thuoc').replaceChildren();
        (kq ? kq.donThuoc : []).forEach(themDongThuoc);
        $('nut-luu-ket-qua').textContent = kq ? t('Lưu thay đổi', 'Save changes')
            : t('Lưu kết quả khám (lịch hẹn chuyển sang đã hoàn thành)', 'Save visit result (the appointment becomes completed)');
        $('khoi-ly-do-sua').classList.toggle('an', !kq || daKhoa);
        fk.elements.lyDoSua.value = '';
        fk.elements.lyDoSua.required = !!kq && !daKhoa;
        for (const el of fk.elements) el.disabled = daKhoa;
        if (daKhoa) {
            bao('bs-ket-qua-trang-thai', t(`Đã khóa từ ${ngayGioVn(kq.hanSua)}: hồ sơ bệnh án chỉ còn xem được.`,
                `Locked since ${ngayGioVn(kq.hanSua)}: the medical record is read-only.`));
        } else if (kq) {
            bao('bs-ket-qua-trang-thai', t(`Sửa được đến ${ngayGioVn(kq.hanSua)}. Mỗi lần sửa cần ghi lý do và được lưu vào lịch sử sửa.`,
                `Editable until ${ngayGioVn(kq.hanSua)}. Each change needs a reason and is kept in the edit history.`));
        }
        const lichSu = (idLichHen) => `/api/medical/bac-si/toi/lich-hen/${idLichHen}/benh-an/lich-su`;
        $('bs-lich-su-sua').replaceChildren(...(kq ? [nut(t('Lịch sử sửa', 'Edit history'), (e) => batTatLichSuSua(e.currentTarget, lichSu(lich.id)))] : []));

        const truoc = hs.lanKhamTruoc.map((lan) => {
            const khoi = the('div', null, 'lan-kham');
            khoi.append(the('b', `${ngayGioVn(lan.gioKhamDuKien)} · ${t('BS', 'Dr.')} ${lan.bacSi.hoTen} (${lan.tenChuyenKhoa}) · ${TRANG_THAI[lan.trangThai] || lan.trangThai}`),
                the('p', lan.lyDoKham ? t('Lý do khám: ', 'Reason for visit: ') + lan.lyDoKham : null, 'ghi-chu'),
                lan.ketQua ? ketQuaKham(lan.ketQua) : ghiChu(t('Chưa có kết quả khám.', 'No visit result yet.')));
            if (lan.ketQua && lan.ketQua.ngaySuaCuoi) khoi.append(nut(t('Lịch sử sửa', 'Edit history'), (e) => batTatLichSuSua(e.currentTarget, lichSu(lan.idLichHen))));
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
        // Ô chọn thuốc: không gửi chữ, chỉ gửi id của thuốc đã chọn (hang.dataset.idThuoc)
        const ten = o('chonThuoc', thuoc ? moTaThuoc(muc) : '', null, t('Tên thuốc hoặc hoạt chất', 'Medicine name or active ingredient'));
        ten.className = 'chon-thuoc';
        if (thuoc) hang.dataset.idThuoc = muc.idThuoc;
        ten.setAttribute('list', 'goi-y-thuoc');
        ten.addEventListener('input', () => {
            const daChon = goiYTheoChu.get(ten.value);
            if (daChon) hang.dataset.idThuoc = daChon.id;
            else { delete hang.dataset.idThuoc; goiYThuoc(ten.value); }
            ten.classList.toggle('chua-chon', !hang.dataset.idThuoc && ten.value.trim() !== '');
        });
        const soLan = o('soLanMoiNgay', muc.soLanMoiNgay, 'number', t('Lần/ngày', 'Times/day'));
        const soNgay = o('soNgayDung', muc.soNgayDung, 'number', t('Số ngày *', 'Days *'));
        soNgay.max = '90';
        const soLuong = o('soLuong', muc.soLuong, 'number', t('Tổng số lượng *', 'Total quantity *'));
        soLuong.max = '9999';
        // Gợi ý số lượng = lần/ngày × số ngày, tới khi bác sĩ tự sửa ô số lượng (dòng cũ chưa có số lượng cũng được gợi ý)
        let tuDien = !muc.soLuong;
        soLuong.addEventListener('input', () => { tuDien = false; });
        const goiYSoLuong = () => {
            if (tuDien && Number(soLan.value) > 0 && Number(soNgay.value) > 0) soLuong.value = Number(soLan.value) * Number(soNgay.value);
        };
        soLan.addEventListener('input', goiYSoLuong);
        soNgay.addEventListener('input', goiYSoLuong);
        goiYSoLuong();
        hang.append(ten, o('lieuDung', muc.lieuDung, null, t('Liều dùng', 'Dosage')), soLan, soNgay, soLuong,
            o('ghiChuSuDung', muc.ghiChuSuDung, null, t('Ghi chú', 'Note')),
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
                lc.value = moTaThuoc(muc);
                goiYTheoChu.set(lc.value, muc);
                return lc;
            }));
        }, 250);
    }
    // ===== Mã ICD-10 của chẩn đoán chính =====
    function goiYIcd(tuKhoa) {
        clearTimeout(henGoiYIcd);
        if (tuKhoa.trim().length < 2) return;
        henGoiYIcd = setTimeout(async () => {
            const { status, json } = await lay('/api/medical/bac-si/toi/icd10?tuKhoa=' + encodeURIComponent(tuKhoa.trim()), true);
            if (status !== 200 || !$('goi-y-icd')) return;
            $('goi-y-icd').replaceChildren(...json.duLieu.map((muc) => {
                icdTheoMa.set(muc.ma, muc);
                const lc = document.createElement('option');
                lc.value = muc.ma;
                lc.label = muc.ten + (muc.manTinh ? t(' (mạn tính)', ' (chronic)') : '');
                return lc;
            }));
        }, 250);
    }
    // Tên của mã đang nhập, nếu mã đó đã có trong gợi ý (server vẫn kiểm tra mã có trong danh mục)
    function hienTenIcd() {
        const ma = $('form-ket-qua').elements.maIcdChinh.value.trim().toUpperCase();
        const muc = icdTheoMa.get(ma);
        $('ten-icd').textContent = !ma ? '' : muc ? icdChu(muc) + (muc.manTinh ? t(' — kê được tối đa 90 ngày', ' — up to 90 days allowed') : '') : '';
    }

    // Trả null nếu có dòng đã nhập nhưng chưa chọn thuốc trong danh sách gợi ý; dòng để trống hết thì bỏ qua
    function docDonThuoc() {
        const cacDong = [];
        for (const hang of $('don-thuoc').children) {
            const thuoc = {};
            for (const o of hang.querySelectorAll('input')) {
                const giaTri = o.value.trim();
                if (giaTri && o.dataset.truong !== 'chonThuoc') thuoc[o.dataset.truong] = o.type === 'number' ? Number(giaTri) : giaTri;
            }
            const coChu = hang.querySelector('.chon-thuoc').value.trim() !== '';
            if (!hang.dataset.idThuoc) {
                if (coChu || Object.keys(thuoc).length) return null;
                continue;
            }
            cacDong.push({ idThuoc: Number(hang.dataset.idThuoc), ...thuoc });
        }
        return cacDong;
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
            $('form-ket-qua').elements.maIcdChinh.addEventListener('input', (e) => {
                goiYIcd(e.target.value);
                hienTenIcd();
            });
            $('form-ket-qua').addEventListener('submit', async (e) => {
                e.preventDefault();
                const id = hoSoKham.lichHen.id;
                const than = thanForm(e.target, ['lyDoKham', 'khamLamSang', 'chanDoan', 'maIcdChinh', 'chanDoanKemTheo', 'ghiChu', 'ngayTaiKhamDeXuat']);
                const sinhHieu = {};
                for (const ten of SINH_HIEU) {
                    const giaTri = e.target.elements[ten].value.trim();
                    if (giaTri) sinhHieu[ten] = Number(giaTri);
                }
                if (Object.keys(sinhHieu).length) than.sinhHieu = sinhHieu;
                than.donThuoc = docDonThuoc();
                if (!than.donThuoc) {
                    bao('bs-ket-qua-trang-thai', t('Có dòng thuốc chưa chọn thuốc trong danh mục: gõ tên rồi chọn trong danh sách gợi ý, hoặc xóa dòng đó.',
                        'A prescription row has no catalog medicine: type a name and pick it from the suggestions, or remove the row.'), 'loi');
                    return;
                }
                if (hoSoKham.ketQua) {
                    than.lyDoSua = e.target.elements.lyDoSua.value;
                    than.phienBan = hoSoKham.ketQua.phienBan;
                }
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
                clearTimeout(henGoiYIcd);
            };
        },
    });
})();
