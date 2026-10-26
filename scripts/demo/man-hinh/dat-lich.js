// Màn hình UI: Appointment (Patient) — đặt lịch 4 bước của UI, ở đây tách thành 6 mục để thấy rõ từng lần gọi API;
//             phiếu khám vừa đặt (Appointment Ticket) hiện ngay dưới form
// Đường dẫn:  /appointment
// Vai trò:    công khai (khách đặt không cần đăng nhập); bệnh nhân đăng nhập thì lịch được lưu vào tài khoản
// API:
//   GET  /api/catalog/chuyen-khoa?kichThuoc=100                          1. chuyên khoa
//   GET  /api/catalog/bac-si?idChuyenKhoa=&kichThuoc=100                 2. bác sĩ của chuyên khoa (thẻ)
//   GET  /api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa=              2. ngày sớm nhất còn chỗ của từng bác sĩ
//   GET  /api/catalog/bac-si/{id}                                        2. chi tiết bác sĩ (hộp thoại): kèm giấy phép hành nghề
//                                                                          (soGiayPhep, ngayCapGiayPhep, noiCapGiayPhep, phamViHanhNghe),
//                                                                          ngonNguKham[] (mã ISO 639), giaiThuong[], congTrinhNghienCuu[]
//   GET  /api/booking/khung-gio/nhieu-ngay?idBacSi=&tuNgay=              3 + 4. bác sĩ cụ thể: các ca 7 ngày kèm khung giờ (tuNgay bỏ
//                                                                        trống = từ hôm nay; có khi mở từ thẻ của chatbot)
//   GET  /api/booking/khung-gio/ngay-con-cho?tuNgay=&denNgay=&idChuyenKhoa=   3. "bác sĩ bất kỳ": ngày còn chỗ trong 14 ngày
//   GET  /api/booking/khung-gio/gop?idChuyenKhoa=&ngay=                  4. "bác sĩ bất kỳ": khung giờ gộp các bác sĩ
//   POST /api/booking/lich-hen                                           6. đặt lịch (201) -> maPhieuKham, maTraCuu, soThuTu
//   GET  /api/booking/phieu-kham/{maPhieuKham}                           phiếu khám vừa đặt (+ /qr là ảnh mã QR)
// Chỉ khi đăng nhập bằng tài khoản bệnh nhân:
//   GET    /api/booking/thong-tin-dat-lich/cua-toi                       hồ sơ của tôi, người thân đã lưu, lần đặt gần nhất
//   DELETE /api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id}       bỏ 1 người thân đã lưu
//   GET  /api/booking/quy-tac-dat-lich                                   quy tắc đang dùng (quản trị viên sửa được): đặt trước tối
//                                                                        thiểu (phút) / tối đa (ngày), hạn hủy / đổi, số lần đổi,
//                                                                        số lịch còn hiệu lực của 1 người khám
//   GET  /api/booking/ngay-nghi?tuNgay=&denNgay=                         ngày bệnh viện nghỉ kèm lý do (ngày nghỉ không có ca nào)
//   GET  /api/catalog/bac-si?idChuyenKhoa=&ngay=&buoi=SANG|CHIEU|TOI     2. lọc bác sĩ còn lượt trống đặt được vào ngày / buổi đó
//                                                                        (buổi theo giờ bắt đầu lượt: sáng < 12h, chiều 12–17h,
//                                                                        tối từ 17h); ngoài khoảng đặt trước được -> trang rỗng
// Người khám dưới 18 tuổi (tính theo NGÀY KHÁM) phải có người giám hộ; trang chỉ hiện khối nhập, server mới là nơi quyết định.
// Mất link phiếu khám: /find-ticket (khách tự xin gửi lại qua email).
//
// Màn hình ngoài danh sách UI: Đặt lịch qua tổng đài (quản trị viên đặt thay người gọi điện / người đến quầy, kênh HOTLINE)
// Đường dẫn:  /admin/hotline-booking
// Vai trò:    QUAN_TRI_VIEN
// Cùng form và các bước như trên (không có hồ sơ / người thân của tài khoản), thêm ô ngôn ngữ của người gọi. Gửi:
//   POST /api/booking/quan-tri/lich-hen   { idLichLamViec | idChuyenKhoa, gioBatDauKhung, benhNhan, nguoiGiamHo?, lyDoKham,
//        ngonNgu: VI | EN } -> 201 { idLichHen, phieuKham } (phieuKham như kết quả đặt lịch, có maPhieuKham + linkPhieuKham: chỉ
//        phản hồi này trả mã phiếu cho nhân viên, để in phiếu / mở mã QR). Cùng quy tắc với khách đặt (giới hạn theo hồ sơ và SĐT,
//        CCCD, người giám hộ) trừ giới hạn theo IP. Có email thì phiếu được gửi tới đó theo ngonNgu.
(() => {
    const SO_NGAY_XEM = 14;
    const BAT_KY = 'bat-ky';
    const TRUONG_NGUOI_KHAM = ['hoTen', 'ngaySinh', 'gioiTinh', 'soDienThoai', 'cccd', 'email', 'soBaoHiemYTe', 'diaChi'];
    const TRUONG_GIAM_HO = { ghHoTen: 'hoTen', ghQuanHe: 'quanHe', ghSoDienThoai: 'soDienThoai', ghCccd: 'cccd', ghNgaySinh: 'ngaySinh' };

    let hoSoCuaToi = null;    // hồ sơ bệnh nhân của tài khoản (null = chưa có)
    let thongTinDat = null;   // thông tin điền sẵn của tài khoản: banThan, nguoiThan[], lanDatGanNhat
    let dsBacSi = [];         // bác sĩ của chuyên khoa đang chọn
    let bacSiChon = null;     // null = chưa chọn, BAT_KY, hoặc id bác sĩ (chuỗi)
    let caCuaBacSi = [];      // các ca 7 ngày tới của bác sĩ đang chọn (khung-gio/nhieu-ngay)
    let ngayChon = null;      // 'YYYY-MM-DD'
    let khungChon = null;     // { idLichLamViec | idChuyenKhoa, gioBatDauKhung, nhan }
    let thanChoGui = null;    // body của POST /api/booking/lich-hen đang chờ xác nhận
    let formDat = null;
    let tuNgayXem = null;     // { bacSi, ngay }: mở từ thẻ lịch khám của chatbot thì xem 7 ngày tính từ ngày của thẻ
    let hotline = false;      // true ở /admin/hotline-booking: quản trị viên đặt thay người gọi tổng đài

    // Hàm (không phải hằng) để chữ được chọn theo ngôn ngữ lúc vẽ màn hình
    const KHUNG = () => `
        <p id="dat-lich-phien" class="canh-bao"></p>
        <p id="dat-lich-quy-tac" class="ghi-chu"></p>
        <p id="dat-lich-ngay-nghi" class="canh-bao an"></p>
        <section>
            <h2>${t('1. Chuyên khoa', '1. Specialty')}</h2>
            <select id="chuyen-khoa"><option value="">${t('— Chọn chuyên khoa —', '— Select a specialty —')}</option></select>
        </section>
        <section>
            <h2>${t('2. Bác sĩ', '2. Doctor')}</h2>
            <div class="hai-cot">
                <div><label>${t('Chỉ bác sĩ còn lượt trống vào ngày (không bắt buộc)', 'Only doctors with a free place on (optional)')}</label><input id="loc-ngay" type="date"></div>
                <div><label>${t('Buổi', 'Session')}</label><select id="loc-buoi"><option value="">${t('(cả ngày)', '(whole day)')}</option>
                    <option value="SANG">${t('Sáng (trước 12h)', 'Morning (before 12:00)')}</option><option value="CHIEU">${t('Chiều (12h–17h)', 'Afternoon (12:00–17:00)')}</option>
                    <option value="TOI">${t('Tối (từ 17h)', 'Evening (from 17:00)')}</option></select></div>
            </div>
            <div id="ds-bac-si" class="luoi-the"><p class="ghi-chu">${chonChuyenKhoaTruoc()}</p></div>
        </section>
        <dialog id="hop-bac-si">
            <div id="bac-si-chi-tiet"></div>
            <div class="hang-nut">
                <button type="button" id="nut-chon-bac-si-nay">${t('Chọn bác sĩ này', 'Select this doctor')}</button>
                <button type="button" id="nut-dong-bac-si" class="phu">${t('Đóng', 'Close')}</button>
            </div>
        </dialog>
        <section>
            <h2>${t('3. Ngày khám', '3. Date')}</h2>
            <div id="ds-ngay"><p class="ghi-chu">${chonBacSiTruoc()}</p></div>
        </section>
        <section>
            <h2>${t('4. Giờ khám (khung 1 tiếng, hiện số chỗ còn lại)', '4. Time (1-hour time slots, showing places left)')}</h2>
            <div id="ds-ca"><p class="ghi-chu">${chonNgayTruoc()}</p></div>
            <p id="da-chon" class="ghi-chu">${chuaChonKhung()}</p>
        </section>
        <section>
            <h2>${t('5. Thông tin người khám', '5. Patient details')}</h2>
            <div id="chon-nguoi-kham" class="an">
                <label class="chon"><input type="radio" name="nguoiKham" value="banThan" checked>${t('Đặt cho bản thân', 'Book for myself')}</label>
                <label class="chon"><input type="radio" name="nguoiKham" value="nguoiThan">${t('Đặt cho người thân', 'Book for a relative')}</label>
                <p id="ghi-chu-nguoi-kham" class="ghi-chu"></p>
                <div id="khoi-nguoi-than-da-luu" class="an">
                    <label for="nguoi-than-da-luu">${t('Người thân đã lưu', 'Saved relatives')}</label>
                    <select id="nguoi-than-da-luu"><option value="">${nhapNguoiMoi()}</option></select>
                    <button type="button" id="nut-bo-nguoi-than" class="phu nho">${t('Bỏ người này khỏi danh sách đã lưu', 'Remove this person from the saved list')}</button>
                </div>
            </div>
            <form id="form-dat-lich">
                <div class="hai-cot">
                    <div><label>${t('Họ tên', 'Full name')}</label><input name="hoTen" required maxlength="150"></div>
                    <div><label>${t('Ngày sinh', 'Date of birth')}</label><input name="ngaySinh" type="date" required></div>
                    <div><label>${t('Giới tính', 'Gender')}</label>
                        <select name="gioiTinh" required>
                            <option value="">${t('— Chọn —', '— Select —')}</option>
                            <option value="NAM">${GIOI_TINH.NAM}</option>
                            <option value="NU">${GIOI_TINH.NU}</option>
                            <option value="KHAC">${GIOI_TINH.KHAC}</option>
                        </select></div>
                    <div><label>${t('Số điện thoại (10 số, bắt đầu bằng 0)', 'Phone number (10 digits, starting with 0)')}</label><input name="soDienThoai" required></div>
                    <div><label>${t('Số CCCD (12 số; dưới 18 tuổi chưa có thì để trống)', 'National ID (12 digits; leave blank if under 18 and not issued yet)')}</label><input name="cccd"></div>
                    <div><label>${t('Email (không bắt buộc)', 'Email (optional)')}</label><input name="email" type="email" maxlength="255"></div>
                    <div><label>${t('Số bảo hiểm y tế (không bắt buộc)', 'Health insurance number (optional)')}</label><input name="soBaoHiemYTe" maxlength="50"></div>
                    <div><label>${t('Địa chỉ (không bắt buộc)', 'Address (optional)')}</label><input name="diaChi" maxlength="500"></div>
                </div>
                <label>${t('Lý do khám', 'Reason for visit')}</label>
                <textarea name="lyDoKham" rows="2" maxlength="500" required></textarea>
                <fieldset id="khoi-giam-ho" class="an">
                    <legend>${t('Người giám hộ (bắt buộc khi người khám dưới 18 tuổi vào ngày khám)', 'Guardian (required when the patient is under 18 on the visit date)')}</legend>
                    <button type="button" id="nut-dien-giam-ho" class="phu nho an">${t('Điền từ hồ sơ của tôi', 'Fill in from my profile')}</button>
                    <div class="hai-cot">
                        <div><label>${t('Họ tên người giám hộ', 'Guardian full name')}</label><input name="ghHoTen" maxlength="150"></div>
                        <div><label>${t('Quan hệ với bệnh nhân', 'Relationship to the patient')}</label>
                            <select name="ghQuanHe">
                                <option value="">${t('— Chọn —', '— Select —')}</option>
                                <option value="CHA">${QUAN_HE.CHA}</option>
                                <option value="ME">${QUAN_HE.ME}</option>
                                <option value="NGUOI_GIAM_HO_HOP_PHAP">${QUAN_HE.NGUOI_GIAM_HO_HOP_PHAP}</option>
                                <option value="KHAC">${QUAN_HE.KHAC}</option>
                            </select></div>
                        <div><label>${t('Số điện thoại', 'Phone number')}</label><input name="ghSoDienThoai"></div>
                        <div><label>${t('Số CCCD (12 số)', 'National ID (12 digits)')}</label><input name="ghCccd"></div>
                        <div><label>${t('Ngày sinh (không bắt buộc; có thì phải từ đủ 18 tuổi)', 'Date of birth (optional; if given, must be 18 or older)')}</label><input name="ghNgaySinh" type="date"></div>
                    </div>
                </fieldset>
                <label id="khoi-luu-nguoi-than" class="chon an"><input type="checkbox" id="luu-nguoi-than" checked>${t('Lưu người này để lần sau điền sẵn',
                    'Save this person to prefill next time')}</label>
                <div id="khoi-ngon-ngu" class="an"><label>${t('Ngôn ngữ của người gọi (email phiếu khám, thông báo sau này)', 'Caller\'s language (ticket email, later notices)')}</label>
                    <select name="ngonNgu"><option value="VI">${t('Tiếng Việt', 'Vietnamese')}</option><option value="EN">${t('Tiếng Anh', 'English')}</option></select></div>
                <button id="nut-dat-lich">${t('Xem lại và đặt lịch', 'Review and book')}</button>
            </form>
            <p id="thong-bao-dat" class="ghi-chu"></p>
            <p id="link-tim-lai" class="ghi-chu"><a href="/find-ticket">${t('Mất link phiếu khám? Xin gửi lại qua email', 'Lost your ticket link? Get it emailed again')}</a></p>
        </section>
        <dialog id="hop-tom-tat">
            <h2>${t('6. Xác nhận đặt lịch', '6. Confirm the booking')}</h2>
            <dl id="tom-tat" class="tom-tat"></dl>
            <div class="hang-nut">
                <button type="button" id="nut-xac-nhan-dat">${t('Xác nhận đặt lịch', 'Confirm booking')}</button>
                <button type="button" id="nut-sua-lai" class="phu">${t('Sửa lại', 'Edit')}</button>
            </div>
        </dialog>
        <section id="khoi-ket-qua" class="an">
            <h2>${t('Appointment Ticket — phiếu khám vừa đặt', 'Appointment Ticket — the ticket just booked')}</h2>
            <p id="ket-qua-ghi-chu" class="ghi-chu"></p>
            <div id="ket-qua-phieu"></div>
            <button type="button" id="nut-in-phieu" class="phu an">${t('In phiếu khám', 'Print the ticket')}</button>
        </section>`;

    // Các câu dùng ở nhiều chỗ (khung ban đầu và khi vẽ lại từng mục)
    const chonChuyenKhoaTruoc = () => t('Chọn chuyên khoa trước.', 'Select a specialty first.');
    const chonBacSiTruoc = () => t('Chọn bác sĩ trước.', 'Select a doctor first.');
    const chonNgayTruoc = () => t('Chọn ngày khám trước.', 'Select a date first.');
    const chuaChonKhung = () => t('Chưa chọn khung giờ.', 'No time slot selected.');
    const nhapNguoiMoi = () => t('— Nhập người mới —', '— Enter a new person —');
    const chuHetCho = () => t('Hết chỗ', 'Full');

    const loi = (json, macDinh) => the('p', thongDiepLoi(json, macDinh), 'ghi-chu loi');

    // ===== 1. Chuyên khoa =====
    async function taiChuyenKhoa() {
        const { status, json } = await lay('/api/catalog/chuyen-khoa?kichThuoc=100', true);
        if (status !== 200) {
            $('ds-bac-si').replaceChildren(loi(json, t('Không tải được chuyên khoa.', 'Could not load specialties.')));
            return;
        }
        for (const ck of json.duLieu.noiDung) {
            const lua = the('option', ck.tenChuyenKhoa);
            lua.value = ck.id;
            $('chuyen-khoa').append(lua);
        }
    }

    // ===== 2. Thẻ bác sĩ, chi tiết bác sĩ =====
    // diemDanhGia (1 chữ số thập phân, null khi chưa có) luôn đi kèm soDanhGia; nhận xét của bệnh nhân không công khai
    const diemDanhGia = (bs) => (bs.diemDanhGia != null
        ? t(`★ ${bs.diemDanhGia.toFixed(1)} (${bs.soDanhGia} đánh giá)`, `★ ${bs.diemDanhGia.toFixed(1)} (${bs.soDanhGia} reviews)`)
        : t('Chưa có đánh giá', 'No reviews yet'));
    function theBacSi(bs, somNhat) {
        const khoi = the('div', undefined, 'the-bac-si');
        khoi.dataset.bacSi = bs.id;
        const dau = the('div', undefined, 'dau-the');
        dau.append(anh(bs.anhDaiDien, t('Ảnh ' + bs.hoTen, 'Photo of ' + bs.hoTen), 'anh-dai-dien'));
        const chu = document.createElement('div');
        chu.append(the('strong', tenBacSi(bs)));
        if (bs.chucVu) chu.append(the('div', bs.chucVu, 'ghi-chu'));
        if (bs.soNamKinhNghiem != null) chu.append(the('div', t(`${bs.soNamKinhNghiem} năm kinh nghiệm`, `${bs.soNamKinhNghiem} years of experience`), 'ghi-chu'));
        chu.append(the('div', diemDanhGia(bs), 'ghi-chu'));
        dau.append(chu);
        khoi.append(dau);
        if (bs.gioiThieuNgan) khoi.append(the('p', bs.gioiThieuNgan));
        khoi.append(the('p', somNhat
            ? t(`Ngày sớm nhất còn chỗ: ${thuCuaNgay(somNhat.ngay)} ${ngayVn(somNhat.ngay)} (còn ${somNhat.soChoConLai})`,
                `Earliest date with places: ${thuCuaNgay(somNhat.ngay)} ${ngayVn(somNhat.ngay)} (${somNhat.soChoConLai} left)`)
            : t('Chưa có ngày nào còn chỗ.', 'No date with places left yet.'), 'ghi-chu'));
        khoi.append(nut(t('Chọn', 'Select'), () => chonBacSi(String(bs.id))),
            nut(t('Xem chi tiết', 'View details'), () => xemBacSi(bs.id), 'nho phu'));
        return khoi;
    }

    async function taiBacSi() {
        const ds = $('ds-bac-si');
        dsBacSi = [];
        const idChuyenKhoa = $('chuyen-khoa').value;
        if (!idChuyenKhoa) {
            ds.replaceChildren(ghiChu(chonChuyenKhoaTruoc()));
            return;
        }
        // Lọc ngày / buổi (DOC-03): chỉ bác sĩ còn lượt trống đặt được lúc đó
        const loc = new URLSearchParams();
        if ($('loc-ngay').value) loc.set('ngay', $('loc-ngay').value);
        if ($('loc-buoi').value) loc.set('buoi', $('loc-buoi').value);
        const [bacSi, somNhat] = await Promise.all([
            lay(`/api/catalog/bac-si?idChuyenKhoa=${idChuyenKhoa}&kichThuoc=100${loc.size ? '&' + loc : ''}`),
            lay(`/api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa=${idChuyenKhoa}`, true),
        ]);
        if (bacSi.status !== 200) {
            ds.replaceChildren(loi(bacSi.json, t('Không tải được bác sĩ.', 'Could not load doctors.')));
            return;
        }
        dsBacSi = bacSi.json.duLieu.noiDung;
        const theoBacSi = new Map((somNhat.status === 200 ? somNhat.json.duLieu : []).map((n) => [n.idBacSi, n]));
        const batKy = the('div', undefined, 'the-bac-si');
        batKy.dataset.bacSi = BAT_KY;
        batKy.append(the('strong', t('Bác sĩ bất kỳ', 'Any doctor')),
            the('p', t('Phòng khám xếp bác sĩ còn nhiều chỗ nhất trong giờ bạn chọn.',
                'The clinic assigns the doctor with the most places left at the time you choose.'), 'ghi-chu'),
            nut(t('Chọn', 'Select'), () => chonBacSi(BAT_KY)));
        ds.replaceChildren(batKy, ...dsBacSi.map((bs) => theBacSi(bs, theoBacSi.get(bs.id))));
        if (loc.size && !dsBacSi.length) {
            ds.append(ghiChu(t('Không bác sĩ nào còn lượt trống đặt được vào lúc đã lọc (hoặc ngày đó ngoài khoảng đặt trước).',
                'No doctor has a bookable free place at the filtered time (or the date is outside the booking window).')));
        }
    }

    // Quy tắc đặt lịch và ngày bệnh viện nghỉ: chỉ để hiện, server vẫn là nơi kiểm tra
    async function taiQuyTacVaNgayNghi() {
        const [quyTac, ngayNghi] = await Promise.all([
            lay('/api/booking/quy-tac-dat-lich', true),
            lay(`/api/booking/ngay-nghi?tuNgay=${congNgay(0)}&denNgay=${congNgay(90)}`, true),
        ]);
        if (!$('dat-lich-quy-tac')) return;
        if (quyTac.status === 200) {
            const q = quyTac.json.duLieu;
            $('dat-lich-quy-tac').textContent = t(`Quy tắc: đặt trước ít nhất ${q.datTruocToiThieuPhut} phút và tối đa ${q.soNgayDatTruocToiDa} ngày; hủy / đổi lịch trước giờ khám ít nhất ${q.huyDoiTruocToiThieuPhut} phút, đổi tối đa ${q.soLanDoiLichToiDa} lần; mỗi người khám có tối đa ${q.soLichHieuLucToiDaMoiHoSo} lịch hẹn chưa khám.`,
                `Rules: book at least ${q.datTruocToiThieuPhut} minutes and at most ${q.soNgayDatTruocToiDa} days ahead; cancel or reschedule at least ${q.huyDoiTruocToiThieuPhut} minutes before the visit, at most ${q.soLanDoiLichToiDa} reschedules; each patient can hold at most ${q.soLichHieuLucToiDaMoiHoSo} upcoming appointments.`);
        }
        const cacNgay = ngayNghi.status === 200 ? ngayNghi.json.duLieu : [];
        $('dat-lich-ngay-nghi').classList.toggle('an', !cacNgay.length);
        $('dat-lich-ngay-nghi').textContent = t('Bệnh viện nghỉ (không có lịch khám): ', 'The hospital is closed (no appointments) on: ')
            + cacNgay.map((n) => `${thuCuaNgay(n.ngay)} ${ngayVn(n.ngay)}${n.lyDo ? ` (${n.lyDo})` : ''}`).join('; ');
    }

    function danhSach(tieuDe, cacDong) {
        if (!cacDong || !cacDong.length) return [];
        const ul = document.createElement('ul');
        for (const noiDung of cacDong) ul.append(the('li', noiDung));
        return [the('h3', tieuDe), ul];
    }

    async function xemBacSi(id) {
        const { status, json } = await lay('/api/catalog/bac-si/' + id);
        const khung = $('bac-si-chi-tiet');
        if (status !== 200) {
            khung.replaceChildren(loi(json, t('Không tải được bác sĩ.', 'Could not load the doctor.')));
        } else {
            const bs = json.duLieu;
            const dau = the('div', undefined, 'dau-the');
            dau.append(anh(bs.anhDaiDien, t('Ảnh ' + bs.hoTen, 'Photo of ' + bs.hoTen), 'anh-dai-dien lon'));
            const chu = document.createElement('div');
            chu.append(the('h2', tenBacSi(bs)), the('div', [bs.chucVu, bs.tenChuyenKhoa].filter(Boolean).join(' · '), 'ghi-chu'));
            if (bs.soNamKinhNghiem != null) chu.append(the('div', t(`${bs.soNamKinhNghiem} năm kinh nghiệm`, `${bs.soNamKinhNghiem} years of experience`), 'ghi-chu'));
            chu.append(the('div', diemDanhGia(bs), 'ghi-chu'));
            if (bs.ngonNguKham && bs.ngonNguKham.length) chu.append(the('div', t('Ngôn ngữ khám: ', 'Consultation languages: ') + dsNgonNgu(bs.ngonNguKham), 'ghi-chu'));
            if (bs.gioiThieuNgan) chu.append(the('p', bs.gioiThieuNgan));
            dau.append(chu);
            const phan = [dau];
            if (bs.tieuSu) phan.push(the('h3', t('Giới thiệu', 'About')), the('p', bs.tieuSu));
            phan.push(...danhSach(t('Khám và điều trị', 'Examination and treatment'), bs.linhVucKhamChua),
                ...danhSach(t('Quá trình công tác', 'Work experience'), bs.quaTrinhCongTac),
                ...danhSach(t('Quá trình đào tạo', 'Education and training'), bs.quaTrinhDaoTao),
                ...danhSach(t('Giải thưởng', 'Awards'), bs.giaiThuong),
                ...danhSach(t('Công trình nghiên cứu', 'Research'), bs.congTrinhNghienCuu));
            if (bs.soGiayPhep) {
                phan.push(the('h3', t('Giấy phép hành nghề', 'Practice licence')), thongTin([
                    [t('Số giấy phép', 'Licence number'), bs.soGiayPhep],
                    [t('Ngày cấp', 'Issue date'), bs.ngayCapGiayPhep ? ngayVn(bs.ngayCapGiayPhep) : null],
                    [t('Nơi cấp', 'Issuer'), bs.noiCapGiayPhep], [t('Phạm vi hành nghề', 'Scope of practice'), bs.phamViHanhNghe],
                ]));
            }
            if (bs.anh && bs.anh.length) {
                const luoi = the('div', undefined, 'luoi-anh');
                for (const a of bs.anh) {
                    const hinh = document.createElement('figure');
                    hinh.append(anh(a.url, a.chuThich), the('figcaption',
                        `${a.loai === 'CHUNG_CHI' ? t('Chứng chỉ', 'Certificate') : t('Ảnh công việc', 'Work photo')}${a.chuThich ? ': ' + a.chuThich : ''}`));
                    luoi.append(hinh);
                }
                phan.push(the('h3', t('Hình ảnh và chứng chỉ', 'Photos and certificates')), luoi);
            }
            khung.replaceChildren(...phan);
        }
        $('nut-chon-bac-si-nay').dataset.bacSi = id;
        $('nut-chon-bac-si-nay').classList.toggle('an', status !== 200);
        $('hop-bac-si').showModal();
    }

    async function chonBacSi(giaTri) {
        bacSiChon = giaTri;
        for (const khoi of $('ds-bac-si').querySelectorAll('.the-bac-si')) {
            khoi.classList.toggle('da-chon', khoi.dataset.bacSi === giaTri);
        }
        ngayChon = null;
        boChonKhung();
        $('ds-ca').replaceChildren(ghiChu(chonNgayTruoc()));
        await taiNgay();
    }

    // ===== 3. Ngày còn chỗ =====
    function boChonKhung() {
        khungChon = null;
        $('da-chon').textContent = chuaChonKhung();
    }

    // Bác sĩ cụ thể: 1 lần gọi nhieu-ngay (7 ngày) cho cả ngày lẫn giờ. Bác sĩ bất kỳ: ngay-con-cho (14 ngày), giờ lấy ở gop
    async function taiNgay(imLang = false) {
        const ds = $('ds-ngay');
        caCuaBacSi = [];
        if (!bacSiChon) {
            ds.replaceChildren(ghiChu(chonBacSiTruoc()));
            return;
        }
        let cacNgay;
        if (bacSiChon === BAT_KY) {
            const { status, json } = await lay(`/api/booking/khung-gio/ngay-con-cho?tuNgay=${congNgay(0)}`
                + `&denNgay=${congNgay(SO_NGAY_XEM - 1)}&idChuyenKhoa=${$('chuyen-khoa').value}`, imLang);
            if (status !== 200) {
                ds.replaceChildren(loi(json, t('Không tải được ngày khám.', 'Could not load dates.')));
                return;
            }
            cacNgay = json.duLieu;
        } else {
            const tuNgay = tuNgayXem && tuNgayXem.bacSi === bacSiChon ? `&tuNgay=${tuNgayXem.ngay}` : '';
            const { status, json } = await lay(`/api/booking/khung-gio/nhieu-ngay?idBacSi=${bacSiChon}${tuNgay}`, imLang);
            if (status !== 200 && tuNgay) {
                // Ngày lấy từ đường dẫn không còn hợp lệ (quá xa, đã qua): xem lại từ hôm nay
                tuNgayXem = null;
                return taiNgay(imLang);
            }
            if (status !== 200) {
                ds.replaceChildren(loi(json, t('Không tải được lịch của bác sĩ.', 'Could not load the schedule of the doctor.')));
                return;
            }
            caCuaBacSi = json.duLieu;
            const tong = new Map();
            for (const ca of caCuaBacSi) {
                tong.set(ca.ngay, (tong.get(ca.ngay) || 0) + ca.khungGio.reduce((s, kg) => s + kg.soChoConLai, 0));
            }
            cacNgay = [...tong].map(([ngay, soChoConLai]) => ({ ngay, soChoConLai }));
        }
        if (!cacNgay.length) {
            ds.replaceChildren(ghiChu(bacSiChon === BAT_KY
                ? t('Không có ca khám nào trong 14 ngày tới.', 'No shifts in the next 14 days.')
                : t('Bác sĩ không có ca khám nào trong 7 ngày đang xem.', 'The doctor has no shifts in the 7 days shown.')));
            return;
        }
        ds.replaceChildren(...cacNgay.map((n) => {
            const hetCho = n.soChoConLai === 0;
            const nutNgay = nut(`${thuCuaNgay(n.ngay)} ${n.ngay.slice(8, 10)}/${n.ngay.slice(5, 7)} · ${hetCho ? chuHetCho() : t(`còn ${n.soChoConLai}`, `${n.soChoConLai} left`)}`,
                () => chonNgay(n.ngay), 'o' + (n.ngay === ngayChon ? ' da-chon' : ''));
            nutNgay.dataset.ngay = n.ngay;
            nutNgay.disabled = hetCho;
            return nutNgay;
        }));
    }

    async function chonNgay(ngay) {
        ngayChon = ngay;
        boChonKhung();
        for (const nutNgay of $('ds-ngay').querySelectorAll('button')) {
            nutNgay.classList.toggle('da-chon', nutNgay.dataset.ngay === ngay);
        }
        capNhatKhoiGiamHo();
        await taiKhung();
    }

    // ===== 4. Giờ khám =====
    function nutKhung(ds, kg, nhanSoCho, khung) {
        const khoang = `${gio(kg.gioBatDau)}–${gio(kg.gioKetThuc)}`;
        const daChon = khungChon && khungChon.gioBatDauKhung === kg.gioBatDau
            && khungChon.idLichLamViec === khung.idLichLamViec && khungChon.idChuyenKhoa === khung.idChuyenKhoa;
        const nutGio = nut(`${khoang} · ${kg.hetCho ? chuHetCho() : nhanSoCho}`, () => {
            khungChon = { ...khung, gioBatDauKhung: kg.gioBatDau, nhan: `${khoang} ${ngayVn(ngayChon)}, ${khung.nhan}` };
            for (const nutKhac of ds.querySelectorAll('button')) nutKhac.classList.toggle('da-chon', nutKhac === nutGio);
            $('da-chon').textContent = t('Đã chọn: ', 'Selected: ') + khungChon.nhan;
        }, 'o' + (daChon ? ' da-chon' : ''));
        nutGio.disabled = kg.hetCho;
        if (khung.idLichLamViec) nutGio.dataset.khung = `${khung.idLichLamViec}|${kg.gioBatDau}`;
        return nutGio;
    }

    async function taiKhung(imLang = false) {
        const ds = $('ds-ca');
        if (!ngayChon || !bacSiChon) {
            ds.replaceChildren(ghiChu(chonNgayTruoc()));
            return;
        }
        if (bacSiChon === BAT_KY) {
            const idChuyenKhoa = Number($('chuyen-khoa').value);
            const { status, json } = await lay(`/api/booking/khung-gio/gop?idChuyenKhoa=${idChuyenKhoa}&ngay=${ngayChon}`, imLang);
            if (status !== 200) {
                ds.replaceChildren(loi(json, t('Không tải được giờ khám.', 'Could not load time slots.')));
                return;
            }
            if (!json.duLieu.length) {
                ds.replaceChildren(ghiChu(t('Ngày này không còn giờ khám nào đặt được.', 'No bookable time slots left on this date.')));
                return;
            }
            const khoi = the('div', undefined, 'ca');
            khoi.append(the('h3', t('Gộp các bác sĩ của chuyên khoa', 'All doctors of the specialty combined')));
            for (const kg of json.duLieu) {
                khoi.append(nutKhung(ds, kg, t(`còn ${kg.soChoConLai}/${kg.tongSoCho} · ${kg.soBacSi} bác sĩ`, `${kg.soChoConLai}/${kg.tongSoCho} left · ${kg.soBacSi} doctors`),
                    { idChuyenKhoa, nhan: t('bác sĩ do phòng khám xếp', 'doctor assigned by the clinic') }));
            }
            ds.replaceChildren(khoi);
            return;
        }
        const cacCa = caCuaBacSi.filter((ca) => ca.ngay === ngayChon);
        if (!cacCa.length) {
            ds.replaceChildren(ghiChu(t('Ngày này không còn ca khám nào đặt được.', 'No bookable shifts left on this date.')));
            return;
        }
        ds.replaceChildren(...cacCa.map((ca) => {
            const khoi = the('div', undefined, 'ca');
            const phong = tenPhong(ca.phongKham);
            khoi.append(the('h3', t(`${tenBacSi(ca.bacSi)} · ${phong} · ca ${ca.gioBatDau.slice(0, 5)}–${ca.gioKetThuc.slice(0, 5)}, mỗi lượt ${ca.thoiLuongLuotPhut} phút`,
                `${tenBacSi(ca.bacSi)} · ${phong} · shift ${ca.gioBatDau.slice(0, 5)}–${ca.gioKetThuc.slice(0, 5)}, ${ca.thoiLuongLuotPhut} min per visit`)));
            for (const kg of ca.khungGio) {
                khoi.append(nutKhung(ds, kg, t(`còn ${kg.soChoConLai}/${kg.tongSoCho}`, `${kg.soChoConLai}/${kg.tongSoCho} left`),
                    { idLichLamViec: ca.idLichLamViec, nhan: `${tenBacSi(ca.bacSi)}, ${phong}` }));
            }
            return khoi;
        }));
    }

    // Sau khi đặt (hoặc bị từ chối vì hết chỗ): tải lại số chỗ
    async function taiLaiLich() {
        await taiNgay(true);
        await taiKhung(true);
    }

    // ===== 5. Form người khám =====
    function capNhatKhoiGiamHo() {
        const ngaySinh = formDat.elements.ngaySinh.value;
        // Tuổi tính theo NGÀY KHÁM (chưa chọn ngày thì tạm tính theo hôm nay)
        const duoi18 = !!ngaySinh && tuoiVaoNgay(ngaySinh, ngayChon || congNgay(0)) < 18;
        $('khoi-giam-ho').classList.toggle('an', !duoi18);
    }

    function datChoBanThan() {
        return laBenhNhan() && document.querySelector('input[name=nguoiKham]:checked').value === 'banThan';
    }

    function dienNguoiKham(nguoi, giamHo) {
        for (const ten of TRUONG_NGUOI_KHAM) formDat.elements[ten].value = (nguoi && nguoi[ten]) || '';
        for (const [o, truong] of Object.entries(TRUONG_GIAM_HO)) formDat.elements[o].value = (giamHo && giamHo[truong]) || '';
        capNhatKhoiGiamHo();
    }

    function hienNguoiThanDaLuu() {
        const chon = $('nguoi-than-da-luu');
        const ds = (thongTinDat && thongTinDat.nguoiThan) || [];
        chon.replaceChildren(the('option', nhapNguoiMoi()));
        chon.firstChild.value = '';
        for (const nt of ds) {
            const lua = the('option', `${nt.hoTen}${nt.ngaySinh ? ' · ' + ngayVn(nt.ngaySinh) : ''}`);
            lua.value = nt.id;
            chon.append(lua);
        }
        const choNguoiThan = laBenhNhan() && !datChoBanThan();
        $('khoi-nguoi-than-da-luu').classList.toggle('an', !choNguoiThan || !ds.length);
        $('khoi-luu-nguoi-than').classList.toggle('an', !choNguoiThan);
    }

    // "Đặt cho bản thân": điền sẵn từ hồ sơ bệnh nhân của tài khoản; "người thân": chọn người đã lưu hoặc nhập mới
    function apDungNguoiKham() {
        if (!laBenhNhan()) return;
        hienNguoiThanDaLuu();
        const ghi = $('ghi-chu-nguoi-kham');
        if (!datChoBanThan()) {
            dienNguoiKham(null);
            ghi.textContent = t('Chọn người thân đã lưu hoặc nhập người mới. Trẻ dưới 18 tuổi: bạn là người giám hộ (bấm "Điền từ hồ sơ của tôi").',
                'Select a saved relative or enter a new person. For a child under 18 you are the guardian (press "Fill in from my profile").');
        } else if (hoSoCuaToi && hoSoCuaToi.cccd) {
            dienNguoiKham({ ...hoSoCuaToi, email: thongTinDat && thongTinDat.emailTaiKhoan });
            ghi.textContent = t('Đã điền từ hồ sơ bệnh nhân của bạn (họ tên, ngày sinh, CCCD phải khớp hồ sơ).',
                'Filled in from your patient profile (full name, date of birth and national ID must match the profile).');
        } else {
            dienNguoiKham(null);
            ghi.textContent = hoSoCuaToi
                ? t('Hồ sơ bệnh nhân của tài khoản đang chờ phòng khám xác minh: chưa đặt cho bản thân được, hãy đặt cho người thân hoặc liên hệ phòng khám.',
                    'The patient profile of this account is awaiting clinic verification: you cannot book for yourself yet, book for a relative or contact the clinic.')
                : t('Tài khoản chưa có hồ sơ bệnh nhân: lần đặt này sẽ tạo hồ sơ và gắn vào tài khoản.',
                    'This account has no patient profile yet: this booking will create one and attach it to the account.');
        }
    }

    // Thông tin điền sẵn của tài khoản. Lần đọc đầu tiên sau đăng nhập cũng là lúc server liên kết hồ sơ đã đặt như khách
    // theo CCCD khai khi đăng ký
    async function taiThongTin() {
        const { status, json } = await lay('/api/booking/thong-tin-dat-lich/cua-toi', true);
        if (status !== 200) return;
        thongTinDat = json.duLieu;
        hoSoCuaToi = thongTinDat.banThan;
        hienNguoiThanDaLuu();
    }

    async function chonLanDatGanNhat() {
        const lan = thongTinDat && thongTinDat.lanDatGanNhat;
        if (!lan || $('chuyen-khoa').value) return;
        $('chuyen-khoa').value = lan.idChuyenKhoa;
        if (!$('chuyen-khoa').value) return;
        await taiBacSi();
        if (dsBacSi.some((bs) => bs.id === lan.idBacSi)) await chonBacSi(String(lan.idBacSi));
    }

    function thanDatLich(v) {
        const than = {
            gioBatDauKhung: khungChon.gioBatDauKhung,
            benhNhan: {
                hoTen: v.hoTen, ngaySinh: v.ngaySinh || null, gioiTinh: v.gioiTinh || null,
                soDienThoai: v.soDienThoai, cccd: v.cccd || null, email: v.email || null,
                soBaoHiemYTe: v.soBaoHiemYTe || null, diaChi: v.diaChi || null,
            },
            lyDoKham: v.lyDoKham || null,
        };
        // Bác sĩ cụ thể gửi ca làm việc; "bác sĩ bất kỳ" gửi chuyên khoa và server chọn bác sĩ
        if (khungChon.idLichLamViec) than.idLichLamViec = khungChon.idLichLamViec;
        else than.idChuyenKhoa = khungChon.idChuyenKhoa;
        const nguoiGiamHo = {
            hoTen: v.ghHoTen, quanHe: v.ghQuanHe || null, soDienThoai: v.ghSoDienThoai, cccd: v.ghCccd,
            ngaySinh: v.ghNgaySinh || null,
        };
        // Khối giám hộ đang hiện và có nhập gì đó thì mới gửi; để trống hết thì server tự báo thiếu người giám hộ
        if (!$('khoi-giam-ho').classList.contains('an') && Object.values(nguoiGiamHo).some(Boolean)) {
            than.nguoiGiamHo = nguoiGiamHo;
        }
        if (datChoBanThan()) than.datChoBanThan = true;
        else if (laBenhNhan() && !$('luu-nguoi-than').checked) than.luuNguoiThan = false;
        if (hotline) than.ngonNgu = v.ngonNgu;
        return than;
    }

    // ===== 6. Tóm tắt rồi mới gửi =====
    function hienTomTat(than) {
        const dl = $('tom-tat');
        const bn = than.benhNhan;
        const them = (nhan, giaTri) => {
            if (giaTri) dl.append(the('dt', nhan), the('dd', giaTri));
        };
        dl.replaceChildren();
        them(t('Chuyên khoa', 'Specialty'), $('chuyen-khoa').selectedOptions[0].textContent);
        them(t('Giờ khám', 'Time'), khungChon.nhan);
        them(t('Người khám', 'Patient'), `${bn.hoTen} · ${bn.ngaySinh ? ngayVn(bn.ngaySinh) : '?'} · ${GIOI_TINH[bn.gioiTinh] || ''}`);
        them(t('Số điện thoại', 'Phone number'), bn.soDienThoai);
        them(t('CCCD', 'National ID'), bn.cccd);
        them('Email', bn.email);
        them(t('Số bảo hiểm y tế', 'Health insurance number'), bn.soBaoHiemYTe);
        them(t('Địa chỉ', 'Address'), bn.diaChi);
        if (than.nguoiGiamHo) {
            const gh = than.nguoiGiamHo;
            them(t('Người giám hộ', 'Guardian'), t(`${gh.hoTen} (${QUAN_HE[gh.quanHe] || '?'}) · ${gh.soDienThoai} · CCCD ${gh.cccd}`,
                `${gh.hoTen} (${QUAN_HE[gh.quanHe] || '?'}) · ${gh.soDienThoai} · national ID ${gh.cccd}`));
        }
        them(t('Lý do khám', 'Reason for visit'), than.lyDoKham);
        if (hotline) them(t('Ngôn ngữ của người gọi', 'Caller\'s language'), than.ngonNgu === 'EN' ? t('Tiếng Anh', 'English') : t('Tiếng Việt', 'Vietnamese'));
        if (laBenhNhan()) {
            them(t('Lưu vào tài khoản', 'Saved to the account'), than.datChoBanThan ? t('Đặt cho bản thân', 'Booked for myself')
                : than.luuNguoiThan === false ? t('Đặt cho người thân, không lưu người này', 'Booked for a relative, this person is not saved')
                    : t('Đặt cho người thân, lưu để lần sau điền sẵn', 'Booked for a relative, saved to prefill next time'));
        }
    }

    async function xacNhanDat() {
        const nutXacNhan = $('nut-xac-nhan-dat');
        nutXacNhan.disabled = true;
        const { status, json } = await goi(hotline ? '/api/booking/quan-tri/lich-hen' : '/api/booking/lich-hen', thanChoGui);
        nutXacNhan.disabled = false;
        $('hop-tom-tat').close();
        if (status !== 201) {
            bao('thong-bao-dat', thongDiepLoi(json, t('Đặt lịch thất bại.', 'Booking failed.')), 'loi');
            // Hết chỗ / khung không còn đặt được: tải lại để thấy số chỗ mới
            if (status === 409) await taiLaiLich();
            return;
        }
        // Tổng đài: { idLichHen, phieuKham }; bệnh nhân / khách: phiếu khám vừa đặt
        const kq = hotline ? json.duLieu.phieuKham : json.duLieu;
        $('nut-in-phieu').classList.toggle('an', !hotline);
        if (hotline) {
            formDat.reset();
            capNhatKhoiGiamHo();
        }
        bao('thong-bao-dat', t(`Đặt lịch thành công: mã ${kq.maTraCuu}, số thứ tự ${kq.soThuTu}, giờ khám dự kiến ${ngayGioVn(kq.gioKhamDuKien)}.`,
            `Booking successful: lookup code ${kq.maTraCuu}, queue number ${kq.soThuTu}, expected time ${ngayGioVn(kq.gioKhamDuKien)}.`), 'tot');
        $('khoi-ket-qua').classList.remove('an');
        $('ket-qua-ghi-chu').textContent = (kq.bacSiDoPhongKhamXep ? t(`Phòng khám đã xếp ${tenBacSi(kq.bacSi)}. `, `The clinic assigned ${tenBacSi(kq.bacSi)}. `) : '')
            + (hotline
                ? t(`Lịch hẹn #${json.duLieu.idLichHen} (kênh tổng đài). Đọc mã tra cứu cho người gọi, in phiếu hoặc cho quét mã QR: sau màn hình này nhân viên không xem lại được link phiếu khám (chỉ cấp lại sau khi xem CCCD).`,
                    `Appointment #${json.duLieu.idLichHen} (hotline channel). Read the lookup code to the caller, print the ticket or let them scan the QR code: after this screen staff cannot see the ticket link again (only reissue it after checking the national ID).`)
                : kq.luuVaoTaiKhoan
                    ? t('Lịch đã lưu vào tài khoản (xem ở Lịch hẹn của tôi).', 'The appointment is saved to your account (see My Appointments).')
                    : t('Khách đặt: lịch không nằm trong tài khoản nào, hãy lưu link / mã QR dưới đây để xem lại phiếu khám.',
                        'Guest booking: the appointment is not in any account, keep the link / QR code below to view the ticket again.'));
        const phieu = await taiPhieu(kq.maPhieuKham, true);
        if (phieu.status === 200) hienPhieu($('ket-qua-phieu'), phieu.json.duLieu);
        else $('ket-qua-phieu').replaceChildren(loi(phieu.json, t('Không tải được phiếu khám.', 'Could not load the appointment ticket.')));
        boChonKhung();
        await taiLaiLich();
        if (laBenhNhan()) await taiThongTin();   // hồ sơ vừa tạo khi "đặt cho bản thân" lần đầu, người thân vừa được lưu
        $('khoi-ket-qua').scrollIntoView({ behavior: 'smooth' });
    }

    // Mở từ thẻ gợi ý của chatbot (chung.js): /appointment?idChuyenKhoa=&idBacSi=&ngay=&idLichLamViec=&gioBatDauKhung=
    // ngay (ngày của ca trong thẻ): trang đặt lịch chỉ tải 7 ngày, nên với ca xa hơn phải tải từ ngày đó.
    // Chọn sẵn lần lượt chuyên khoa, bác sĩ, ngày, khung giờ; thiếu tham số nào thì dừng ở bước đó. Giá trị lạ hoặc đã cũ
    // (bác sĩ thôi nhận lịch, ca bị hủy, khung hết chỗ) được bỏ qua, không báo lỗi. Trả true khi đã chọn được chuyên khoa.
    async function chonTheoThamSo(thamSo) {
        const idBacSi = thamSo.get('idBacSi') || '';
        let idChuyenKhoa = thamSo.get('idChuyenKhoa') || '';
        if (!idChuyenKhoa && /^[0-9]+$/.test(idBacSi)) {
            // Thẻ lịch khám của chatbot có thể không kèm chuyên khoa: lấy từ bác sĩ
            const { status, json } = await lay('/api/catalog/bac-si/' + idBacSi, true);
            if (status === 200 && json.duLieu.idChuyenKhoa) idChuyenKhoa = String(json.duLieu.idChuyenKhoa);
        }
        if (!idChuyenKhoa || !formDat.isConnected) return false;
        $('chuyen-khoa').value = idChuyenKhoa;
        if (!$('chuyen-khoa').value) return false;
        await taiBacSi();
        if (!formDat.isConnected || !dsBacSi.some((bs) => String(bs.id) === idBacSi)) return true;
        const ngay = thamSo.get('ngay') || '';
        tuNgayXem = /^[0-9]{4}-[0-9]{2}-[0-9]{2}$/.test(ngay) && ngay > congNgay(0) ? { bacSi: idBacSi, ngay } : null;
        await chonBacSi(idBacSi);
        const ca = caCuaBacSi.find((c) => String(c.idLichLamViec) === thamSo.get('idLichLamViec'));
        if (!ca || !formDat.isConnected) return true;
        await chonNgay(ca.ngay);
        if (!formDat.isConnected) return true;
        const maKhung = `${ca.idLichLamViec}|${thamSo.get('gioBatDauKhung')}`;
        const nutGio = [...$('ds-ca').querySelectorAll('button')].find((n) => n.dataset.khung === maKhung);
        if (nutGio && !nutGio.disabled) nutGio.click();
        return true;
    }

    // Cùng 1 màn hình cho 2 đường dẫn: /appointment (bệnh nhân / khách) và /admin/hotline-booking (quản trị viên)
    async function veDatLich(khung, { thamSo, duongDan }) {
        hotline = duongDan === '/admin/hotline-booking';
        tuNgayXem = null;
        hoSoCuaToi = null;
        thongTinDat = null;
        dsBacSi = [];
        bacSiChon = null;
        caCuaBacSi = [];
        ngayChon = null;
        khungChon = null;
        thanChoGui = null;
        khuon(khung, KHUNG());
        formDat = $('form-dat-lich');

        $('dat-lich-phien').textContent = hotline
            ? t('Đặt thay người gọi tổng đài / người đến quầy: nhập đúng thông tin người khám như khách đặt. Lịch không gắn vào tài khoản nào.',
                'Booking for a caller or a walk-in: enter the patient\'s details as a guest would. The appointment is not attached to any account.')
            : !vaiTro()
            ? t('Chưa đăng nhập: đặt lịch như khách (không cần tài khoản). Đăng nhập bằng tài khoản bệnh nhân thì lịch được lưu vào tài khoản.',
                'Not signed in: booking as a guest (no account needed). Sign in with a patient account to save appointments to the account.')
            : laBenhNhan()
                ? t(`Đã đăng nhập: ${taiKhoan.hoTen}. Lịch đặt sẽ được lưu vào tài khoản.`,
                    `Signed in: ${taiKhoan.hoTen}. Appointments you book will be saved to the account.`)
                : t(`Đã đăng nhập bằng tài khoản ${VAI_TRO[vaiTro()]}: vai trò này không đặt lịch được (chỉ bệnh nhân hoặc khách).`,
                    `Signed in with an account of role ${VAI_TRO[vaiTro()]}: this role cannot book appointments (patients or guests only).`);
        for (const id of ['chon-nguoi-kham', 'nut-dien-giam-ho']) $(id).classList.toggle('an', !laBenhNhan());
        $('khoi-ngon-ngu').classList.toggle('an', !hotline);
        $('link-tim-lai').classList.toggle('an', hotline);
        $('nut-in-phieu').addEventListener('click', () => window.print());
        for (const id of ['loc-ngay', 'loc-buoi']) $(id).addEventListener('change', () => { if ($('chuyen-khoa').value) taiBacSi(); });
        taiQuyTacVaNgayNghi();

        $('chuyen-khoa').addEventListener('change', async () => {
            bacSiChon = null;
            ngayChon = null;
            boChonKhung();
            $('ds-ngay').replaceChildren(ghiChu(chonBacSiTruoc()));
            $('ds-ca').replaceChildren(ghiChu(chonNgayTruoc()));
            await taiBacSi();
        });
        $('nut-dong-bac-si').addEventListener('click', () => $('hop-bac-si').close());
        $('nut-chon-bac-si-nay').addEventListener('click', async (e) => {
            $('hop-bac-si').close();
            await chonBacSi(e.target.dataset.bacSi);
        });
        formDat.addEventListener('submit', (e) => {
            e.preventDefault();
            if (!khungChon) {
                bao('thong-bao-dat', t('Chưa chọn khung giờ (bước 4).', 'No time slot selected (step 4).'), 'loi');
                return;
            }
            thanChoGui = thanDatLich(giaTriForm(formDat));
            hienTomTat(thanChoGui);
            $('hop-tom-tat').showModal();
        });
        $('nut-sua-lai').addEventListener('click', () => $('hop-tom-tat').close());
        $('nut-xac-nhan-dat').addEventListener('click', xacNhanDat);
        formDat.elements.ngaySinh.addEventListener('change', capNhatKhoiGiamHo);
        for (const luaChon of document.querySelectorAll('input[name=nguoiKham]')) {
            luaChon.addEventListener('change', apDungNguoiKham);
        }
        $('nut-dien-giam-ho').addEventListener('click', () => {
            if (!hoSoCuaToi || !hoSoCuaToi.cccd) {
                bao('thong-bao-dat', t('Tài khoản chưa có hồ sơ bệnh nhân để điền: nhập tay, hoặc tạo hồ sơ ở Hồ sơ bệnh nhân của tôi.',
                    'This account has no patient profile to fill in from: type it in, or create a profile in My Patient Profile.'), 'loi');
                return;
            }
            formDat.elements.ghHoTen.value = hoSoCuaToi.hoTen || '';
            formDat.elements.ghSoDienThoai.value = hoSoCuaToi.soDienThoai || '';
            formDat.elements.ghCccd.value = hoSoCuaToi.cccd || '';
            formDat.elements.ghNgaySinh.value = hoSoCuaToi.ngaySinh || '';
        });
        $('nguoi-than-da-luu').addEventListener('change', (e) => {
            const nt = ((thongTinDat && thongTinDat.nguoiThan) || []).find((n) => String(n.id) === e.target.value);
            dienNguoiKham(nt || null, nt && nt.nguoiGiamHo);
        });
        $('nut-bo-nguoi-than').addEventListener('click', async () => {
            const id = $('nguoi-than-da-luu').value;
            if (!id) return;
            const { status, json } = await goi('/api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/' + id, undefined, 'DELETE');
            if (status !== 200) {
                bao('thong-bao-dat', thongDiepLoi(json, t('Không bỏ được người thân.', 'Could not remove the relative.')), 'loi');
                return;
            }
            dienNguoiKham(null);
            await taiThongTin();
        });

        await taiChuyenKhoa();
        if (!formDat.isConnected) return;
        const daChonTheoThamSo = await chonTheoThamSo(thamSo);
        if (laBenhNhan() && formDat.isConnected) {
            await taiThongTin();
            if (!formDat.isConnected) return;
            apDungNguoiKham();
            if (!daChonTheoThamSo) await chonLanDatGanNhat();
        }
    }

    dangKy({
        duongDan: '/appointment',
        tieuDe: cap('Đặt lịch khám', 'Appointment'),
        ve: veDatLich,
    });
    dangKy({
        duongDan: '/admin/hotline-booking',
        vaiTro: 'QUAN_TRI_VIEN',
        tieuDe: cap('Đặt lịch qua tổng đài', 'Hotline Booking'),
        ve: veDatLich,
    });
})();
