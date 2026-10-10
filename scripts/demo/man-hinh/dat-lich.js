// Màn hình UI: Appointment (Patient) — đặt lịch 4 bước của UI, ở đây tách thành 6 mục để thấy rõ từng lần gọi API;
//             phiếu khám vừa đặt (Appointment Ticket) hiện ngay dưới form
// Đường dẫn:  /appointment
// Vai trò:    công khai (khách đặt không cần đăng nhập); bệnh nhân đăng nhập thì lịch được lưu vào tài khoản
// API:
//   GET  /api/catalog/chuyen-khoa?kichThuoc=100                          1. chuyên khoa
//   GET  /api/catalog/bac-si?idChuyenKhoa=&kichThuoc=100                 2. bác sĩ của chuyên khoa (thẻ)
//   GET  /api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa=              2. ngày sớm nhất còn chỗ của từng bác sĩ
//   GET  /api/catalog/bac-si/{id}                                        2. chi tiết bác sĩ (hộp thoại)
//   GET  /api/booking/khung-gio/nhieu-ngay?idBacSi=                      3 + 4. bác sĩ cụ thể: các ca 7 ngày tới kèm khung giờ
//   GET  /api/booking/khung-gio/ngay-con-cho?tuNgay=&denNgay=&idChuyenKhoa=   3. "bác sĩ bất kỳ": ngày còn chỗ trong 14 ngày
//   GET  /api/booking/khung-gio/gop?idChuyenKhoa=&ngay=                  4. "bác sĩ bất kỳ": khung giờ gộp các bác sĩ
//   POST /api/booking/lich-hen                                           6. đặt lịch (201) -> maPhieuKham, maTraCuu, soThuTu
//   GET  /api/booking/phieu-kham/{maPhieuKham}                           phiếu khám vừa đặt (+ /qr là ảnh mã QR)
// Chỉ khi đăng nhập bằng tài khoản bệnh nhân:
//   GET    /api/booking/thong-tin-dat-lich/cua-toi                       hồ sơ của tôi, người thân đã lưu, lần đặt gần nhất
//   DELETE /api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id}       bỏ 1 người thân đã lưu
// Người khám dưới 18 tuổi (tính theo NGÀY KHÁM) phải có người giám hộ; trang chỉ hiện khối nhập, server mới là nơi quyết định.
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

    const KHUNG = `
        <p id="dat-lich-phien" class="canh-bao"></p>
        <section>
            <h2>1. Chuyên khoa</h2>
            <select id="chuyen-khoa"><option value="">— Chọn chuyên khoa —</option></select>
        </section>
        <section>
            <h2>2. Bác sĩ</h2>
            <div id="ds-bac-si" class="luoi-the"><p class="ghi-chu">Chọn chuyên khoa trước.</p></div>
        </section>
        <dialog id="hop-bac-si">
            <div id="bac-si-chi-tiet"></div>
            <div class="hang-nut">
                <button type="button" id="nut-chon-bac-si-nay">Chọn bác sĩ này</button>
                <button type="button" id="nut-dong-bac-si" class="phu">Đóng</button>
            </div>
        </dialog>
        <section>
            <h2>3. Ngày khám</h2>
            <div id="ds-ngay"><p class="ghi-chu">Chọn bác sĩ trước.</p></div>
        </section>
        <section>
            <h2>4. Giờ khám (khung 1 tiếng, hiện số chỗ còn lại)</h2>
            <div id="ds-ca"><p class="ghi-chu">Chọn ngày khám trước.</p></div>
            <p id="da-chon" class="ghi-chu">Chưa chọn khung giờ.</p>
        </section>
        <section>
            <h2>5. Thông tin người khám</h2>
            <div id="chon-nguoi-kham" class="an">
                <label class="chon"><input type="radio" name="nguoiKham" value="banThan" checked>Đặt cho bản thân</label>
                <label class="chon"><input type="radio" name="nguoiKham" value="nguoiThan">Đặt cho người thân</label>
                <p id="ghi-chu-nguoi-kham" class="ghi-chu"></p>
                <div id="khoi-nguoi-than-da-luu" class="an">
                    <label for="nguoi-than-da-luu">Người thân đã lưu</label>
                    <select id="nguoi-than-da-luu"><option value="">— Nhập người mới —</option></select>
                    <button type="button" id="nut-bo-nguoi-than" class="phu nho">Bỏ người này khỏi danh sách đã lưu</button>
                </div>
            </div>
            <form id="form-dat-lich">
                <div class="hai-cot">
                    <div><label>Họ tên</label><input name="hoTen" required maxlength="150"></div>
                    <div><label>Ngày sinh</label><input name="ngaySinh" type="date" required></div>
                    <div><label>Giới tính</label>
                        <select name="gioiTinh" required>
                            <option value="">— Chọn —</option>
                            <option value="NAM">Nam</option>
                            <option value="NU">Nữ</option>
                            <option value="KHAC">Khác</option>
                        </select></div>
                    <div><label>Số điện thoại (10 số, bắt đầu bằng 0)</label><input name="soDienThoai" required></div>
                    <div><label>Số CCCD (12 số; dưới 18 tuổi chưa có thì để trống)</label><input name="cccd"></div>
                    <div><label>Email (không bắt buộc)</label><input name="email" type="email" maxlength="255"></div>
                    <div><label>Số bảo hiểm y tế (không bắt buộc)</label><input name="soBaoHiemYTe" maxlength="50"></div>
                    <div><label>Địa chỉ (không bắt buộc)</label><input name="diaChi" maxlength="500"></div>
                </div>
                <label>Lý do khám</label>
                <textarea name="lyDoKham" rows="2" maxlength="500" required></textarea>
                <fieldset id="khoi-giam-ho" class="an">
                    <legend>Người giám hộ (bắt buộc khi người khám dưới 18 tuổi vào ngày khám)</legend>
                    <button type="button" id="nut-dien-giam-ho" class="phu nho an">Điền từ hồ sơ của tôi</button>
                    <div class="hai-cot">
                        <div><label>Họ tên người giám hộ</label><input name="ghHoTen" maxlength="150"></div>
                        <div><label>Quan hệ với bệnh nhân</label>
                            <select name="ghQuanHe">
                                <option value="">— Chọn —</option>
                                <option value="CHA">Cha</option>
                                <option value="ME">Mẹ</option>
                                <option value="NGUOI_GIAM_HO_HOP_PHAP">Người giám hộ hợp pháp</option>
                                <option value="KHAC">Khác</option>
                            </select></div>
                        <div><label>Số điện thoại</label><input name="ghSoDienThoai"></div>
                        <div><label>Số CCCD (12 số)</label><input name="ghCccd"></div>
                        <div><label>Ngày sinh (không bắt buộc; có thì phải từ đủ 18 tuổi)</label><input name="ghNgaySinh" type="date"></div>
                    </div>
                </fieldset>
                <label id="khoi-luu-nguoi-than" class="chon an"><input type="checkbox" id="luu-nguoi-than" checked>Lưu người này để lần sau
                    điền sẵn</label>
                <button id="nut-dat-lich">Xem lại và đặt lịch</button>
            </form>
            <p id="thong-bao-dat" class="ghi-chu"></p>
        </section>
        <dialog id="hop-tom-tat">
            <h2>6. Xác nhận đặt lịch</h2>
            <dl id="tom-tat" class="tom-tat"></dl>
            <div class="hang-nut">
                <button type="button" id="nut-xac-nhan-dat">Xác nhận đặt lịch</button>
                <button type="button" id="nut-sua-lai" class="phu">Sửa lại</button>
            </div>
        </dialog>
        <section id="khoi-ket-qua" class="an">
            <h2>Appointment Ticket — phiếu khám vừa đặt</h2>
            <p id="ket-qua-ghi-chu" class="ghi-chu"></p>
            <div id="ket-qua-phieu"></div>
        </section>`;

    const loi = (json, macDinh) => the('p', thongDiepLoi(json, macDinh), 'ghi-chu loi');

    // ===== 1. Chuyên khoa =====
    async function taiChuyenKhoa() {
        const { status, json } = await lay('/api/catalog/chuyen-khoa?kichThuoc=100', true);
        if (status !== 200) {
            $('ds-bac-si').replaceChildren(loi(json, 'Không tải được chuyên khoa.'));
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
    const diemDanhGia = (bs) => (bs.diemDanhGia != null ? `★ ${bs.diemDanhGia.toFixed(1)} (${bs.soDanhGia} đánh giá)` : 'Chưa có đánh giá');
    function theBacSi(bs, somNhat) {
        const khoi = the('div', undefined, 'the-bac-si');
        khoi.dataset.bacSi = bs.id;
        const dau = the('div', undefined, 'dau-the');
        dau.append(anh(bs.anhDaiDien, 'Ảnh ' + bs.hoTen, 'anh-dai-dien'));
        const chu = document.createElement('div');
        chu.append(the('strong', tenBacSi(bs)));
        if (bs.chucVu) chu.append(the('div', bs.chucVu, 'ghi-chu'));
        if (bs.soNamKinhNghiem != null) chu.append(the('div', `${bs.soNamKinhNghiem} năm kinh nghiệm`, 'ghi-chu'));
        chu.append(the('div', diemDanhGia(bs), 'ghi-chu'));
        dau.append(chu);
        khoi.append(dau);
        if (bs.gioiThieuNgan) khoi.append(the('p', bs.gioiThieuNgan));
        khoi.append(the('p', somNhat
            ? `Ngày sớm nhất còn chỗ: ${thuCuaNgay(somNhat.ngay)} ${ngayVn(somNhat.ngay)} (còn ${somNhat.soChoConLai})`
            : 'Chưa có ngày nào còn chỗ.', 'ghi-chu'));
        khoi.append(nut('Chọn', () => chonBacSi(String(bs.id))), nut('Xem chi tiết', () => xemBacSi(bs.id), 'nho phu'));
        return khoi;
    }

    async function taiBacSi() {
        const ds = $('ds-bac-si');
        dsBacSi = [];
        const idChuyenKhoa = $('chuyen-khoa').value;
        if (!idChuyenKhoa) {
            ds.replaceChildren(ghiChu('Chọn chuyên khoa trước.'));
            return;
        }
        const [bacSi, somNhat] = await Promise.all([
            lay(`/api/catalog/bac-si?idChuyenKhoa=${idChuyenKhoa}&kichThuoc=100`),
            lay(`/api/booking/khung-gio/ngay-som-nhat?idChuyenKhoa=${idChuyenKhoa}`, true),
        ]);
        if (bacSi.status !== 200) {
            ds.replaceChildren(loi(bacSi.json, 'Không tải được bác sĩ.'));
            return;
        }
        dsBacSi = bacSi.json.duLieu.noiDung;
        const theoBacSi = new Map((somNhat.status === 200 ? somNhat.json.duLieu : []).map((n) => [n.idBacSi, n]));
        const batKy = the('div', undefined, 'the-bac-si');
        batKy.dataset.bacSi = BAT_KY;
        batKy.append(the('strong', 'Bác sĩ bất kỳ'),
            the('p', 'Phòng khám xếp bác sĩ còn nhiều chỗ nhất trong giờ bạn chọn.', 'ghi-chu'),
            nut('Chọn', () => chonBacSi(BAT_KY)));
        ds.replaceChildren(batKy, ...dsBacSi.map((bs) => theBacSi(bs, theoBacSi.get(bs.id))));
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
            khung.replaceChildren(loi(json, 'Không tải được bác sĩ.'));
        } else {
            const bs = json.duLieu;
            const dau = the('div', undefined, 'dau-the');
            dau.append(anh(bs.anhDaiDien, 'Ảnh ' + bs.hoTen, 'anh-dai-dien lon'));
            const chu = document.createElement('div');
            chu.append(the('h2', tenBacSi(bs)), the('div', [bs.chucVu, bs.tenChuyenKhoa].filter(Boolean).join(' · '), 'ghi-chu'));
            if (bs.soNamKinhNghiem != null) chu.append(the('div', `${bs.soNamKinhNghiem} năm kinh nghiệm`, 'ghi-chu'));
            chu.append(the('div', diemDanhGia(bs), 'ghi-chu'));
            if (bs.gioiThieuNgan) chu.append(the('p', bs.gioiThieuNgan));
            dau.append(chu);
            const phan = [dau];
            if (bs.tieuSu) phan.push(the('h3', 'Giới thiệu'), the('p', bs.tieuSu));
            phan.push(...danhSach('Khám và điều trị', bs.linhVucKhamChua), ...danhSach('Quá trình công tác', bs.quaTrinhCongTac),
                ...danhSach('Quá trình đào tạo', bs.quaTrinhDaoTao));
            if (bs.anh && bs.anh.length) {
                const luoi = the('div', undefined, 'luoi-anh');
                for (const a of bs.anh) {
                    const hinh = document.createElement('figure');
                    hinh.append(anh(a.url, a.chuThich), the('figcaption',
                        `${a.loai === 'CHUNG_CHI' ? 'Chứng chỉ' : 'Ảnh công việc'}${a.chuThich ? ': ' + a.chuThich : ''}`));
                    luoi.append(hinh);
                }
                phan.push(the('h3', 'Hình ảnh và chứng chỉ'), luoi);
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
        $('ds-ca').replaceChildren(ghiChu('Chọn ngày khám trước.'));
        await taiNgay();
    }

    // ===== 3. Ngày còn chỗ =====
    function boChonKhung() {
        khungChon = null;
        $('da-chon').textContent = 'Chưa chọn khung giờ.';
    }

    // Bác sĩ cụ thể: 1 lần gọi nhieu-ngay (7 ngày) cho cả ngày lẫn giờ. Bác sĩ bất kỳ: ngay-con-cho (14 ngày), giờ lấy ở gop
    async function taiNgay(imLang = false) {
        const ds = $('ds-ngay');
        caCuaBacSi = [];
        if (!bacSiChon) {
            ds.replaceChildren(ghiChu('Chọn bác sĩ trước.'));
            return;
        }
        let cacNgay;
        if (bacSiChon === BAT_KY) {
            const { status, json } = await lay(`/api/booking/khung-gio/ngay-con-cho?tuNgay=${congNgay(0)}`
                + `&denNgay=${congNgay(SO_NGAY_XEM - 1)}&idChuyenKhoa=${$('chuyen-khoa').value}`, imLang);
            if (status !== 200) {
                ds.replaceChildren(loi(json, 'Không tải được ngày khám.'));
                return;
            }
            cacNgay = json.duLieu;
        } else {
            const { status, json } = await lay(`/api/booking/khung-gio/nhieu-ngay?idBacSi=${bacSiChon}`, imLang);
            if (status !== 200) {
                ds.replaceChildren(loi(json, 'Không tải được lịch của bác sĩ.'));
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
            ds.replaceChildren(ghiChu(bacSiChon === BAT_KY ? 'Không có ca khám nào trong 14 ngày tới.' : 'Bác sĩ không có ca khám nào trong 7 ngày tới.'));
            return;
        }
        ds.replaceChildren(...cacNgay.map((n) => {
            const hetCho = n.soChoConLai === 0;
            const nutNgay = nut(`${thuCuaNgay(n.ngay)} ${n.ngay.slice(8, 10)}/${n.ngay.slice(5, 7)} · ${hetCho ? 'Hết chỗ' : `còn ${n.soChoConLai}`}`,
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
        const nutGio = nut(`${khoang} · ${kg.hetCho ? 'Hết chỗ' : nhanSoCho}`, () => {
            khungChon = { ...khung, gioBatDauKhung: kg.gioBatDau, nhan: `${khoang} ${ngayVn(ngayChon)}, ${khung.nhan}` };
            for (const nutKhac of ds.querySelectorAll('button')) nutKhac.classList.toggle('da-chon', nutKhac === nutGio);
            $('da-chon').textContent = 'Đã chọn: ' + khungChon.nhan;
        }, 'o' + (daChon ? ' da-chon' : ''));
        nutGio.disabled = kg.hetCho;
        return nutGio;
    }

    async function taiKhung(imLang = false) {
        const ds = $('ds-ca');
        if (!ngayChon || !bacSiChon) {
            ds.replaceChildren(ghiChu('Chọn ngày khám trước.'));
            return;
        }
        if (bacSiChon === BAT_KY) {
            const idChuyenKhoa = Number($('chuyen-khoa').value);
            const { status, json } = await lay(`/api/booking/khung-gio/gop?idChuyenKhoa=${idChuyenKhoa}&ngay=${ngayChon}`, imLang);
            if (status !== 200) {
                ds.replaceChildren(loi(json, 'Không tải được giờ khám.'));
                return;
            }
            if (!json.duLieu.length) {
                ds.replaceChildren(ghiChu('Ngày này không còn giờ khám nào đặt được.'));
                return;
            }
            const khoi = the('div', undefined, 'ca');
            khoi.append(the('h3', 'Gộp các bác sĩ của chuyên khoa'));
            for (const kg of json.duLieu) {
                khoi.append(nutKhung(ds, kg, `còn ${kg.soChoConLai}/${kg.tongSoCho} · ${kg.soBacSi} bác sĩ`,
                    { idChuyenKhoa, nhan: 'bác sĩ do phòng khám xếp' }));
            }
            ds.replaceChildren(khoi);
            return;
        }
        const cacCa = caCuaBacSi.filter((ca) => ca.ngay === ngayChon);
        if (!cacCa.length) {
            ds.replaceChildren(ghiChu('Ngày này không còn ca khám nào đặt được.'));
            return;
        }
        ds.replaceChildren(...cacCa.map((ca) => {
            const khoi = the('div', undefined, 'ca');
            const phong = tenPhong(ca.phongKham);
            khoi.append(the('h3', `${tenBacSi(ca.bacSi)} · ${phong} · ca ${ca.gioBatDau.slice(0, 5)}–${ca.gioKetThuc.slice(0, 5)}, mỗi lượt ${ca.thoiLuongLuotPhut} phút`));
            for (const kg of ca.khungGio) {
                khoi.append(nutKhung(ds, kg, `còn ${kg.soChoConLai}/${kg.tongSoCho}`,
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
        chon.replaceChildren(the('option', '— Nhập người mới —'));
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
            ghi.textContent = 'Chọn người thân đã lưu hoặc nhập người mới. Trẻ dưới 18 tuổi: bạn là người giám hộ (bấm "Điền từ hồ sơ của tôi").';
        } else if (hoSoCuaToi && hoSoCuaToi.cccd) {
            dienNguoiKham({ ...hoSoCuaToi, email: thongTinDat && thongTinDat.emailTaiKhoan });
            ghi.textContent = 'Đã điền từ hồ sơ bệnh nhân của bạn (họ tên, ngày sinh, CCCD phải khớp hồ sơ).';
        } else {
            dienNguoiKham(null);
            ghi.textContent = hoSoCuaToi
                ? 'Hồ sơ bệnh nhân của tài khoản đang chờ phòng khám xác minh: chưa đặt cho bản thân được, hãy đặt cho người thân hoặc liên hệ phòng khám.'
                : 'Tài khoản chưa có hồ sơ bệnh nhân: lần đặt này sẽ tạo hồ sơ và gắn vào tài khoản.';
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
        them('Chuyên khoa', $('chuyen-khoa').selectedOptions[0].textContent);
        them('Giờ khám', khungChon.nhan);
        them('Người khám', `${bn.hoTen} · ${bn.ngaySinh ? ngayVn(bn.ngaySinh) : '?'} · ${GIOI_TINH[bn.gioiTinh] || ''}`);
        them('Số điện thoại', bn.soDienThoai);
        them('CCCD', bn.cccd);
        them('Email', bn.email);
        them('Số bảo hiểm y tế', bn.soBaoHiemYTe);
        them('Địa chỉ', bn.diaChi);
        if (than.nguoiGiamHo) {
            const gh = than.nguoiGiamHo;
            them('Người giám hộ', `${gh.hoTen} (${QUAN_HE[gh.quanHe] || '?'}) · ${gh.soDienThoai} · CCCD ${gh.cccd}`);
        }
        them('Lý do khám', than.lyDoKham);
        if (laBenhNhan()) {
            them('Lưu vào tài khoản', than.datChoBanThan ? 'Đặt cho bản thân'
                : than.luuNguoiThan === false ? 'Đặt cho người thân, không lưu người này' : 'Đặt cho người thân, lưu để lần sau điền sẵn');
        }
    }

    async function xacNhanDat() {
        const nutXacNhan = $('nut-xac-nhan-dat');
        nutXacNhan.disabled = true;
        const { status, json } = await goi('/api/booking/lich-hen', thanChoGui);
        nutXacNhan.disabled = false;
        $('hop-tom-tat').close();
        if (status !== 201) {
            bao('thong-bao-dat', thongDiepLoi(json, 'Đặt lịch thất bại.'), 'loi');
            // Hết chỗ / khung không còn đặt được: tải lại để thấy số chỗ mới
            if (status === 409) await taiLaiLich();
            return;
        }
        const kq = json.duLieu;
        bao('thong-bao-dat', `Đặt lịch thành công: mã ${kq.maTraCuu}, số thứ tự ${kq.soThuTu}, giờ khám dự kiến ${ngayGioVn(kq.gioKhamDuKien)}.`, 'tot');
        $('khoi-ket-qua').classList.remove('an');
        $('ket-qua-ghi-chu').textContent = (kq.bacSiDoPhongKhamXep ? `Phòng khám đã xếp ${tenBacSi(kq.bacSi)}. ` : '')
            + (kq.luuVaoTaiKhoan
                ? 'Lịch đã lưu vào tài khoản (xem ở My Appointments).'
                : 'Khách đặt: lịch không nằm trong tài khoản nào, hãy lưu link / mã QR dưới đây để xem lại phiếu khám.');
        const phieu = await taiPhieu(kq.maPhieuKham, true);
        if (phieu.status === 200) hienPhieu($('ket-qua-phieu'), phieu.json.duLieu);
        else $('ket-qua-phieu').replaceChildren(loi(phieu.json, 'Không tải được phiếu khám.'));
        boChonKhung();
        await taiLaiLich();
        if (laBenhNhan()) await taiThongTin();   // hồ sơ vừa tạo khi "đặt cho bản thân" lần đầu, người thân vừa được lưu
        $('khoi-ket-qua').scrollIntoView({ behavior: 'smooth' });
    }

    dangKy({
        duongDan: '/appointment',
        tieuDe: 'Appointment',
        async ve(khung) {
            hoSoCuaToi = null;
            thongTinDat = null;
            dsBacSi = [];
            bacSiChon = null;
            caCuaBacSi = [];
            ngayChon = null;
            khungChon = null;
            thanChoGui = null;
            khuon(khung, KHUNG);
            formDat = $('form-dat-lich');

            $('dat-lich-phien').textContent = !vaiTro()
                ? 'Chưa đăng nhập: đặt lịch như khách (không cần tài khoản). Đăng nhập bằng tài khoản bệnh nhân thì lịch được lưu vào tài khoản.'
                : laBenhNhan()
                    ? `Đã đăng nhập: ${taiKhoan.hoTen}. Lịch đặt sẽ được lưu vào tài khoản.`
                    : `Đã đăng nhập bằng tài khoản ${VAI_TRO[vaiTro()]}: vai trò này không đặt lịch được (chỉ bệnh nhân hoặc khách).`;
            for (const id of ['chon-nguoi-kham', 'nut-dien-giam-ho']) $(id).classList.toggle('an', !laBenhNhan());

            $('chuyen-khoa').addEventListener('change', async () => {
                bacSiChon = null;
                ngayChon = null;
                boChonKhung();
                $('ds-ngay').replaceChildren(ghiChu('Chọn bác sĩ trước.'));
                $('ds-ca').replaceChildren(ghiChu('Chọn ngày khám trước.'));
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
                    bao('thong-bao-dat', 'Chưa chọn khung giờ (bước 4).', 'loi');
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
                    bao('thong-bao-dat', 'Tài khoản chưa có hồ sơ bệnh nhân để điền: nhập tay, hoặc tạo hồ sơ ở My Patient Profile.', 'loi');
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
                    bao('thong-bao-dat', thongDiepLoi(json, 'Không bỏ được người thân.'), 'loi');
                    return;
                }
                dienNguoiKham(null);
                await taiThongTin();
            });

            await taiChuyenKhoa();
            if (laBenhNhan()) {
                await taiThongTin();
                if (!formDat.isConnected) return;
                apDungNguoiKham();
                await chonLanDatGanNhat();
            }
        },
    });
})();
