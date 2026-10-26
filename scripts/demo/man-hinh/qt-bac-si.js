// Màn hình UI: Doctor Management (Admin) — danh bạ bác sĩ với "Add New Doctor" và "Edit"
// Đường dẫn:  /admin/doctors   (?id=<id bác sĩ> mở thẳng hồ sơ của 1 bác sĩ)
// Vai trò:    QUAN_TRI_VIEN
// API:
//   GET    /api/catalog/quan-tri/bac-si?tuKhoa=&idChuyenKhoa=&trangThai=&trang=&kichThuoc=   danh bạ (cả bác sĩ ngừng công tác):
//                                                                    maBacSi, hoTen, email, soDienThoai, tenChuyenKhoa, soLuotDaKham,
//                                                                    trangThai, trangThaiTaiKhoan, phaiDoiMatKhau
//   POST   /api/catalog/quan-tri/bac-si                              "Add New Doctor": { hoTen, email, soDienThoai?, idChuyenKhoa,
//                                                                    soGiayPhep?, hocVi?, chucVu?, namBatDauHanhNghe? } -> 201. Tài khoản
//                                                                    mang mật khẩu mặc định, bác sĩ đặt mật khẩu ở lần đăng nhập đầu
//   PUT    /api/catalog/quan-tri/bac-si/{id}                         { hoTen, soDienThoai?, idChuyenKhoa, soGiayPhep? } (email không sửa)
//   GET    /api/catalog/quan-tri/bac-si/{id}/anh-huong-ngung-cong-tac   { soCaSapToi, soLichHenBiAnhHuong }
//   POST   /api/catalog/quan-tri/bac-si/{id}/ngung-cong-tac          { lyDo }: hủy mọi ca sắp tới, vô hiệu hoá tài khoản.
//                                                                    503 DICH_VU_NOI_BO_LOI: gửi lại để chạy nốt
//   POST   /api/catalog/quan-tri/bac-si/{id}/cong-tac-lai
//   GET    /api/catalog/quan-tri/bac-si/{id}                         hồ sơ giới thiệu, kể cả bác sĩ không hiển thị công khai
//                                                                    (kèm phienBanHoSo; soNamKinhNghiem suy ra từ namBatDauHanhNghe)
//   PUT    /api/catalog/quan-tri/bac-si/{id}/ho-so                   ghi đè mọi trường (kèm bản tiếng Anh ...En, trừ hocVi, ngày cấp, năm, ngôn ngữ):
//                                                                    phienBan (bắt buộc, = phienBanHoSo đã tải; cũ -> 409 DU_LIEU_DA_THAY_DOI),
//                                                                    hocVi, chucVu, namBatDauHanhNghe, ngayCapGiayPhep, noiCapGiayPhep, phamViHanhNghe,
//                                                                    gioiThieuNgan, tieuSu, ngonNguKham[] (mã ISO 639), quaTrinhDaoTao[], quaTrinhCongTac[],
//                                                                    linhVucKhamChua[], giaiThuong[], congTrinhNghienCuu[]. Bác sĩ tự sửa được giới thiệu
//                                                                    ngắn, tiểu sử, ngôn ngữ, giải thưởng, nghiên cứu (/doctor/profile)
//   POST   /api/catalog/quan-tri/bac-si/{id}/anh                     multipart: tệp "anh" (JPEG / PNG / WebP, tối đa 4 MB),
//                                                                    "loai" = ANH_CONG_VIEC | CHUNG_CHI, "chuThich", "chuThichEn" -> 201
//   PUT    /api/catalog/quan-tri/bac-si/{id}/anh/{idAnh}             { loai, chuThich, chuThichEn }
//   DELETE /api/catalog/quan-tri/bac-si/{id}/anh/{idAnh}
//   GET    /api/booking/quan-tri/danh-gia?idBacSi=&soSaoToiDa=&trang=&kichThuoc=   đánh giá của bệnh nhân, mới nhất trước, kèm tên
//                                                                    bệnh nhân và mã tra cứu (bác sĩ thì xem ẩn danh ở Dashboard)
//   (PUT /api/catalog/quan-tri/bac-si/{id}/anh/thu-tu { idAnh: [...] } sắp xếp ảnh: có API, trang demo không dùng)
(() => {
    const QUAN_TRI = '/api/catalog/quan-tri/bac-si';
    const CAC_DANH_SACH = ['quaTrinhDaoTao', 'quaTrinhCongTac', 'linhVucKhamChua', 'giaiThuong', 'congTrinhNghienCuu'];
    const CAC_O_CHU = ['hocVi', 'chucVu', 'namBatDauHanhNghe', 'ngayCapGiayPhep', 'noiCapGiayPhep', 'phamViHanhNghe', 'gioiThieuNgan', 'tieuSu',
        'chucVuEn', 'noiCapGiayPhepEn', 'phamViHanhNgheEn', 'gioiThieuNganEn', 'tieuSuEn'];
    const LOAI_ANH = nhan({ ANH_CONG_VIEC: cap('Ảnh công việc', 'Work photo'), CHUNG_CHI: cap('Chứng chỉ', 'Certificate') });
    const TRANG_THAI_BAC_SI = nhan({
        DANG_CONG_TAC: cap('Active — đang công tác', 'Active'), NGUNG_CONG_TAC: cap('Inactive — ngừng công tác', 'Inactive'),
    });
    let bacSiDangSua = null;   // HoSoBacSiQuanTriResponse đang mở

    async function taiDanhSach(imLang = false) {
        const thamSo = new URLSearchParams({ kichThuoc: 100, ...thanForm($('form-tim-bac-si'), ['idChuyenKhoa', 'tuKhoa', 'trangThai']) });
        const { status, json } = await lay(`${QUAN_TRI}?${thamSo}`, imLang);
        const khung = $('ds-bac-si-qt');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được danh sách bác sĩ.', 'Could not load the doctor list.')), 'ghi-chu loi'));
            return;
        }
        const ds = json.duLieu.noiDung;
        if (!ds.length) {
            khung.replaceChildren(ghiChu(t('Không có bác sĩ nào khớp.', 'No matching doctors.')));
            return;
        }
        khung.replaceChildren(bang([t('Mã', 'Code'), t('Bác sĩ', 'Doctor'), t('Email / SĐT', 'Email / phone'), t('Chuyên khoa', 'Specialty'),
            t('Đã khám', 'Visits done'), t('Trạng thái', 'Status'), t('Thao tác', 'Actions')], ds.map((bs) => dong([
            bs.maBacSi, tenBacSi(bs), [bs.email, bs.soDienThoai].filter(Boolean).join(' · '), bs.tenChuyenKhoa, bs.soLuotDaKham,
            (TRANG_THAI_BAC_SI[bs.trangThai] || bs.trangThai) + (bs.phaiDoiMatKhau ? t(' · chưa đăng nhập lần nào', ' · never signed in') : ''),
            nut('Edit', () => moHoSo(bs.id)),
        ]))));
    }

    async function moHoSo(id) {
        const { status, json } = await lay(`${QUAN_TRI}/${id}`);
        if (!$('khoi-ho-so-bac-si')) return;
        $('khoi-ho-so-bac-si').classList.remove('an');
        if (status !== 200) {
            bacSiDangSua = null;
            $('bac-si-thong-tin').replaceChildren(the('p', thongDiepLoi(json, t('Không mở được hồ sơ bác sĩ.', 'Could not open the doctor profile.')), 'ghi-chu loi'));
            $('khoi-sua-bac-si').classList.add('an');
            return;
        }
        hienHoSo(json.duLieu);
        $('khoi-ho-so-bac-si').scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function hienHoSoMoi(bs) {
        $('khoi-ho-so-bac-si').classList.remove('an');
        hienHoSo(bs);
    }

    async function taiDanhGia(idBacSi) {
        const { status, json } = await lay(`/api/booking/quan-tri/danh-gia?idBacSi=${idBacSi}&kichThuoc=20`, true);
        const khung = $('ds-danh-gia-bac-si');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được đánh giá.', 'Could not load the reviews.')), 'ghi-chu loi'));
            return;
        }
        const duLieu = json.duLieu;
        if (!duLieu.noiDung.length) {
            khung.replaceChildren(ghiChu(t('Bác sĩ này chưa có đánh giá nào.', 'This doctor has no reviews yet.')));
            return;
        }
        khung.replaceChildren(ghiChu(t(`${duLieu.tongSoPhanTu} đánh giá, hiện ${duLieu.noiDung.length} đánh giá mới nhất.`,
            `${duLieu.tongSoPhanTu} reviews, showing the ${duLieu.noiDung.length} most recent.`)),
            bang([t('Số sao', 'Stars'), t('Nhận xét', 'Comment'), t('Bệnh nhân', 'Patient'), t('Lịch hẹn', 'Appointment'),
                t('Ngày khám', 'Visit date'), t('Gửi lúc', 'Submitted at')], duLieu.noiDung.map((dg) => dong([
                '★'.repeat(dg.soSao) + ` (${dg.soSao})`, dg.nhanXet, `#${dg.idHoSoBenhNhan} ${dg.tenBenhNhan}`, dg.maTraCuu, ngayVn(dg.ngayKham), ngayGioVn(dg.ngayTao),
            ]))));
    }

    function hienHoSo(bs) {
        bacSiDangSua = bs;
        taiDanhGia(bs.id);
        $('khoi-sua-bac-si').classList.remove('an');
        $('bac-si-thong-tin').replaceChildren(anh(bs.anhDaiDien, t('Ảnh đại diện', 'Profile photo'), 'anh-dai-dien lon'), thongTin([
            [t('Mã', 'Code'), bs.maBacSi], [t('Họ tên', 'Full name'), bs.hoTen], [t('Email đăng nhập', 'Sign-in email'), bs.email],
            [t('Số điện thoại', 'Phone number'), bs.soDienThoai],
            [t('Chuyên khoa', 'Specialty'), bs.tenChuyenKhoa], [t('Số giấy phép', 'License number'), bs.soGiayPhep],
            [t('Số năm kinh nghiệm', 'Years of experience'), bs.soNamKinhNghiem],
            [t('Phiên bản hồ sơ', 'Profile version'), bs.phienBanHoSo],
            [t('Trạng thái', 'Status'), TRANG_THAI_BAC_SI[bs.trangThai] || bs.trangThai],
            [t('Tài khoản', 'Account'), bs.trangThaiTaiKhoan + (bs.phaiDoiMatKhau
                ? t(' · còn mật khẩu mặc định, chưa đăng nhập lần nào', ' · still has the default password, never signed in') : '')],
        ]));
        const cb = $('form-co-ban-bac-si');
        cb.elements.hoTen.value = bs.hoTen || '';
        cb.elements.soDienThoai.value = bs.soDienThoai || '';
        cb.elements.idChuyenKhoa.value = bs.idChuyenKhoa;
        cb.elements.soGiayPhep.value = bs.soGiayPhep || '';
        hienTrangThai(bs);
        const f = $('form-ho-so-bac-si');
        for (const ten of CAC_O_CHU) f.elements[ten].value = bs[ten] ?? '';
        f.elements.ngonNguKham.value = (bs.ngonNguKham || []).join(', ');
        for (const ten of CAC_DANH_SACH) {
            f.elements[ten].value = (bs[ten] || []).join('\n');
            f.elements[ten + 'En'].value = (bs[ten + 'En'] || []).join('\n');
        }
        hienAnh(bs.anh || []);
    }

    // Khối "Activate / Deactivate": đang công tác -> xem trước ảnh hưởng rồi cho ngừng; đã ngừng -> cho công tác lại
    async function hienTrangThai(bs) {
        const khoi = $('bac-si-trang-thai');
        if (bs.trangThai === 'NGUNG_CONG_TAC') {
            khoi.replaceChildren(
                ghiChu(bs.trangThaiTaiKhoan === 'VO_HIEU_HOA'
                    ? t('Bác sĩ đã ngừng công tác: không hiện trong danh sách công khai, không đăng nhập được.',
                        'The doctor is inactive: not shown in the public list and cannot sign in.')
                    : t('Bác sĩ đã ngừng công tác nhưng tài khoản chưa bị vô hiệu hoá (lần trước chạy dở): bấm "Chạy nốt" để hoàn tất.',
                        'The doctor is inactive but the account is not deactivated yet (the last run stopped halfway): press "Finish deactivation" to complete it.')),
                nut(t('Activate — cho công tác lại', 'Activate'), () => doiTrangThai(`${QUAN_TRI}/${bs.id}/cong-tac-lai`, undefined)));
            if (bs.trangThaiTaiKhoan !== 'VO_HIEU_HOA') {
                khoi.append(' ', nut(t('Chạy nốt việc ngừng công tác', 'Finish deactivation'), () => ngungCongTac(bs), 'nguy-hiem nho'));
            }
            return;
        }
        const { status, json } = await lay(`${QUAN_TRI}/${bs.id}/anh-huong-ngung-cong-tac`, true);
        if (!bacSiDangSua || bacSiDangSua.id !== bs.id) return;
        const ah = status === 200 ? json.duLieu : null;
        khoi.replaceChildren(
            ghiChu(ah ? t(`Nếu cho ngừng công tác ngay: ${ah.soCaSapToi} ca sắp tới bị hủy, ${ah.soLichHenBiAnhHuong} lịch hẹn được giữ và đánh dấu "cần đổi lịch" (bệnh nhân được báo), bác sĩ không đăng nhập được nữa.`,
                `If deactivated now: ${ah.soCaSapToi} upcoming shifts are cancelled, ${ah.soLichHenBiAnhHuong} appointments are kept and `
                + 'marked "needs rescheduling" (patients are notified), and the doctor can no longer sign in.')
                : t('Không đọc được số ca / lịch hẹn bị ảnh hưởng.', 'Could not read the number of affected shifts / appointments.')),
            nut(t('Deactivate — cho ngừng công tác', 'Deactivate'), () => ngungCongTac(bs), 'nguy-hiem nho'));
    }

    async function ngungCongTac(bs) {
        const lyDo = prompt(t('Lý do ngừng công tác (bắt buộc; gửi cho bác sĩ và các bệnh nhân có lịch hẹn bị ảnh hưởng):',
            'Reason for deactivation (required; sent to the doctor and to patients with affected appointments):'), '');
        if (lyDo === null) return;
        await doiTrangThai(`${QUAN_TRI}/${bs.id}/ngung-cong-tac`, { lyDo: lyDo.trim() });
    }

    async function doiTrangThai(duongDan, than) {
        const { status, json } = await goi(duongDan, than);
        if (status !== 200) {
            // 503 DICH_VU_NOI_BO_LOI: bác sĩ đã ở NGUNG_CONG_TAC nhưng chưa xong; mở lại hồ sơ để thấy nút "Chạy nốt"
            bao('co-ban-bac-si-trang-thai', thongDiepLoi(json, t('Không đổi được trạng thái bác sĩ.', 'Could not change the doctor status.')), 'loi');
            if (bacSiDangSua) await moHoSo(bacSiDangSua.id);
            return;
        }
        const kq = json.duLieu;
        hienHoSo(kq.bacSi || kq);
        bao('co-ban-bac-si-trang-thai', (json.thongDiep || t('Đã đổi trạng thái.', 'Status changed.'))
            + (kq.bacSi ? t(` ${kq.soCaDaHuy} ca bị hủy, ${kq.soLichHenCanDoi} lịch hẹn cần đổi lịch.`,
                ` ${kq.soCaDaHuy} shifts cancelled, ${kq.soLichHenCanDoi} appointments need rescheduling.`) : ''), 'tot');
        taiDanhSach(true);
    }

    function hienAnh(ds) {
        if (!ds.length) {
            $('bac-si-anh').replaceChildren(ghiChu(t('Chưa có ảnh giới thiệu nào.', 'No profile photos yet.')));
            return;
        }
        const luoi = the('div', null, 'luoi-anh');
        for (const a of ds) {
            const hinh = document.createElement('figure');
            const chuThich = theoNgonNgu(a.chuThich, a.chuThichEn);
            hinh.append(anh(a.url, chuThich), the('figcaption', `${LOAI_ANH[a.loai] || a.loai}${chuThich ? ': ' + chuThich : ''}`),
                nut(t('Sửa chú thích', 'Edit caption'), () => suaAnh(a)), nut(t('Xoá ảnh', 'Delete photo'), () => xoaAnh(a), 'nguy-hiem nho'));
            luoi.append(hinh);
        }
        $('bac-si-anh').replaceChildren(luoi);
    }

    async function suaAnh(a) {
        const chuThich = prompt(t('Chú thích mới (tối đa 200 ký tự, để trống = bỏ chú thích):',
            'New caption (up to 200 characters, leave empty = remove the caption):'), a.chuThich || '');
        if (chuThich === null) return;
        const chuThichEn = prompt(t('Chú thích tiếng Anh (để trống = người xem tiếng Anh thấy chú thích tiếng Việt):',
            'English caption (leave empty = English viewers see the Vietnamese caption):'), a.chuThichEn || '');
        if (chuThichEn === null) return;
        const { status, json } = await goi(`${QUAN_TRI}/${bacSiDangSua.id}/anh/${a.id}`,
            { loai: a.loai, chuThich: chuThich.trim() || null, chuThichEn: chuThichEn.trim() || null }, 'PUT');
        if (status === 200) await moHoSo(bacSiDangSua.id);
        else bao('anh-bac-si-trang-thai', thongDiepLoi(json, t('Không sửa được ảnh.', 'Could not update the photo.')), 'loi');
    }

    async function xoaAnh(a) {
        if (!confirm(t('Xoá ảnh giới thiệu này?', 'Delete this profile photo?'))) return;
        const { status, json } = await goi(`${QUAN_TRI}/${bacSiDangSua.id}/anh/${a.id}`, undefined, 'DELETE');
        if (status === 200) await moHoSo(bacSiDangSua.id);
        else bao('anh-bac-si-trang-thai', thongDiepLoi(json, t('Không xoá được ảnh.', 'Could not delete the photo.')), 'loi');
    }

    dangKy({
        duongDan: '/admin/doctors',
        vaiTro: 'QUAN_TRI_VIEN',
        tieuDe: cap('Quản lý bác sĩ', 'Doctor Management'),
        async ve(khung, { thamSo }) {
            bacSiDangSua = null;
            khuon(khung, `
                <section>
                    <h2>${t('Doctor Directory — danh bạ bác sĩ', 'Doctor Directory')}</h2>
                    <form id="form-tim-bac-si" class="hai-cot">
                        <div><label>${t('Chuyên khoa', 'Specialty')}</label><select name="idChuyenKhoa" id="qt-bs-chuyen-khoa"><option value="">${t('Tất cả', 'All')}</option></select></div>
                        <div><label>${t('Trạng thái', 'Status')}</label><select name="trangThai"><option value="">${t('Tất cả', 'All')}</option>
                            <option value="DANG_CONG_TAC">${TRANG_THAI_BAC_SI.DANG_CONG_TAC}</option><option value="NGUNG_CONG_TAC">${TRANG_THAI_BAC_SI.NGUNG_CONG_TAC}</option></select></div>
                        <div><label>${t('Tên, email, SĐT hoặc số giấy phép', 'Name, email, phone or license number')}</label><input name="tuKhoa"></div>
                        <button class="phu">${t('Tìm', 'Search')}</button>
                    </form>
                    <div id="ds-bac-si-qt" class="bang"></div>
                </section>
                <section>
                    <h2>${t('Add New Doctor — thêm bác sĩ', 'Add New Doctor')}</h2>
                    <p class="ghi-chu">${t(`Tạo tài khoản đăng nhập và hồ sơ bác sĩ trong 1 bước. Tài khoản mang mật khẩu mặc định của phòng khám
                        (DOCTOR_DEFAULT_PASSWORD, mặc định Doctor@123); ở lần đăng nhập đầu bác sĩ phải đặt mật khẩu của mình.`,
                        `Creates the sign-in account and the doctor profile in one step. The account gets the clinic default password
                        (DOCTOR_DEFAULT_PASSWORD, default Doctor@123); at the first sign-in the doctor must set their own password.`)}</p>
                    <form id="form-them-bac-si">
                        <div class="hai-cot">
                            <div><label>${t('Họ tên *', 'Full name *')}</label><input name="hoTen" maxlength="150" required></div>
                            <div><label>${t('Email đăng nhập *', 'Sign-in email *')}</label><input name="email" type="email" maxlength="255" required></div>
                            <div><label>${t('Số điện thoại', 'Phone number')}</label><input name="soDienThoai" pattern="0[0-9]{9}" placeholder="${t('10 chữ số, bắt đầu bằng 0', '10 digits, starting with 0')}"></div>
                            <div><label>${t('Chuyên khoa *', 'Specialty *')}</label><select name="idChuyenKhoa" class="chon-chuyen-khoa" required><option value="">${t('— chọn —', '— select —')}</option></select></div>
                            <div><label>${t('Số giấy phép hành nghề', 'Practice license number')}</label><input name="soGiayPhep" maxlength="50"></div>
                            <div><label>${t('Học vị', 'Academic degree')}</label><input name="hocVi" maxlength="100" placeholder="${t('vd ThS.BS', 'e.g. ThS.BS')}"></div>
                            <div><label>${t('Chức vụ', 'Position')}</label><input name="chucVu" maxlength="150"></div>
                            <div><label>${t('Năm bắt đầu hành nghề', 'Year started practising')}</label><input name="namBatDauHanhNghe" type="number" min="1950" max="${new Date().getFullYear()}"></div>
                        </div>
                        <button>${t('Thêm bác sĩ', 'Add doctor')}</button>
                    </form>
                    <p id="them-bac-si-trang-thai" class="ghi-chu"></p>
                </section>
                <section id="khoi-ho-so-bac-si" class="an">
                    <h2>${t('Edit — bác sĩ đang chọn', 'Edit — selected doctor')}</h2>
                    <div id="bac-si-thong-tin"></div>
                    <div id="khoi-sua-bac-si">
                        <h3>${t('Thông tin cơ bản', 'Basic details')}</h3>
                        <form id="form-co-ban-bac-si">
                            <div class="hai-cot">
                                <div><label>${t('Họ tên *', 'Full name *')}</label><input name="hoTen" maxlength="150" required></div>
                                <div><label>${t('Số điện thoại (bỏ trống = xoá)', 'Phone number (empty = remove)')}</label><input name="soDienThoai" pattern="0[0-9]{9}"></div>
                                <div><label>${t('Chuyên khoa * (không đổi được khi còn ca sắp tới)', 'Specialty * (cannot change while there are upcoming shifts)')}</label><select name="idChuyenKhoa" class="chon-chuyen-khoa" required></select></div>
                                <div><label>${t('Số giấy phép (bỏ trống = xoá)', 'License number (empty = remove)')}</label><input name="soGiayPhep" maxlength="50"></div>
                            </div>
                            <button>${t('Lưu thông tin cơ bản', 'Save basic details')}</button>
                        </form>
                        <h3>Activate / Deactivate</h3>
                        <div id="bac-si-trang-thai"></div>
                        <p id="co-ban-bac-si-trang-thai" class="ghi-chu"></p>
                        <h3>${t('Hồ sơ giới thiệu', 'Public profile')}</h3>
                        <form id="form-ho-so-bac-si">
                            <div class="hai-cot">
                                <div><label>${t('Học vị', 'Academic degree')}</label><input name="hocVi" maxlength="100"></div>
                                <div><label>${t('Chức vụ', 'Position')}</label><input name="chucVu" maxlength="150"></div>
                                <div><label>${t('Năm bắt đầu hành nghề (số năm kinh nghiệm tính từ đây)', 'Year started practising (years of experience count from it)')}</label><input name="namBatDauHanhNghe" type="number" min="1950" max="${new Date().getFullYear()}"></div>
                                <div><label>${t('Ngày cấp giấy phép hành nghề', 'Licence issue date')}</label><input name="ngayCapGiayPhep" type="date"></div>
                                <div><label>${t('Nơi cấp giấy phép', 'Licence issuer')}</label><input name="noiCapGiayPhep" maxlength="200"></div>
                                <div><label>${t('Ngôn ngữ khám (mã ISO, cách nhau dấu phẩy: vi, en)', 'Consultation languages (ISO codes, comma separated: vi, en)')}</label><input name="ngonNguKham" placeholder="vi, en"></div>
                            </div>
                            <label>${t('Phạm vi hành nghề', 'Scope of practice')}</label><input name="phamViHanhNghe" maxlength="500">
                            <label>${t('Giới thiệu ngắn (hiện trên thẻ bác sĩ)', 'Short introduction (shown on the doctor card)')}</label><input name="gioiThieuNgan" maxlength="300">
                            <label>${t('Tiểu sử', 'Biography')}</label><textarea name="tieuSu" rows="3" maxlength="5000"></textarea>
                            <label>${t('Quá trình đào tạo (mỗi dòng 1 ý, tối đa 20 ý)', 'Education (one item per line, up to 20 items)')}</label><textarea name="quaTrinhDaoTao" rows="3"></textarea>
                            <label>${t('Quá trình công tác (mỗi dòng 1 ý)', 'Work history (one item per line)')}</label><textarea name="quaTrinhCongTac" rows="3"></textarea>
                            <label>${t('Lĩnh vực khám chữa (mỗi dòng 1 ý)', 'Areas of treatment (one item per line)')}</label><textarea name="linhVucKhamChua" rows="3"></textarea>
                            <label>${t('Giải thưởng (mỗi dòng 1 ý)', 'Awards (one item per line)')}</label><textarea name="giaiThuong" rows="2"></textarea>
                            <label>${t('Công trình nghiên cứu (mỗi dòng 1 ý)', 'Research (one item per line)')}</label><textarea name="congTrinhNghienCuu" rows="2"></textarea>
                            <h4>${t('English (bỏ trống mục nào thì người xem tiếng Anh thấy bản tiếng Việt của mục đó; học vị không dịch)',
                                'English (a field left empty shows its Vietnamese version to English viewers; the academic degree is not translated)')}</h4>
                            <label>${t('Chức vụ (English)', 'Position (English)')}</label><input name="chucVuEn" maxlength="150">
                            <label>${t('Nơi cấp giấy phép (English)', 'Licence issuer (English)')}</label><input name="noiCapGiayPhepEn" maxlength="200">
                            <label>${t('Phạm vi hành nghề (English)', 'Scope of practice (English)')}</label><input name="phamViHanhNgheEn" maxlength="500">
                            <label>${t('Giới thiệu ngắn (English)', 'Short introduction (English)')}</label><input name="gioiThieuNganEn" maxlength="300">
                            <label>${t('Tiểu sử (English)', 'Biography (English)')}</label><textarea name="tieuSuEn" rows="3" maxlength="5000"></textarea>
                            <label>${t('Quá trình đào tạo (English, mỗi dòng 1 ý)', 'Education (English, one item per line)')}</label><textarea name="quaTrinhDaoTaoEn" rows="3"></textarea>
                            <label>${t('Quá trình công tác (English, mỗi dòng 1 ý)', 'Work history (English, one item per line)')}</label><textarea name="quaTrinhCongTacEn" rows="3"></textarea>
                            <label>${t('Lĩnh vực khám chữa (English, mỗi dòng 1 ý)', 'Areas of treatment (English, one item per line)')}</label><textarea name="linhVucKhamChuaEn" rows="3"></textarea>
                            <label>${t('Giải thưởng (English, mỗi dòng 1 ý)', 'Awards (English, one item per line)')}</label><textarea name="giaiThuongEn" rows="2"></textarea>
                            <label>${t('Công trình nghiên cứu (English, mỗi dòng 1 ý)', 'Research (English, one item per line)')}</label><textarea name="congTrinhNghienCuuEn" rows="2"></textarea>
                            <button>${t('Lưu hồ sơ (ghi đè mọi trường, trường bỏ trống bị xoá trắng)', 'Save profile (overwrites every field, empty fields are cleared)')}</button>
                        </form>
                        <p id="ho-so-bac-si-trang-thai" class="ghi-chu"></p>
                        <h3>${t('Ảnh giới thiệu và chứng chỉ', 'Profile photos and certificates')}</h3>
                        <div id="bac-si-anh"></div>
                        <form id="form-anh-bac-si" class="hai-cot">
                            <div><label>${t('Tệp ảnh (JPEG / PNG / WebP, tối đa 4 MB)', 'Image file (JPEG / PNG / WebP, up to 4 MB)')}</label><input name="anh" type="file" accept="image/jpeg,image/png,image/webp" required></div>
                            <div><label>${t('Loại', 'Type')}</label><select name="loai"><option value="ANH_CONG_VIEC">${LOAI_ANH.ANH_CONG_VIEC}</option><option value="CHUNG_CHI">${LOAI_ANH.CHUNG_CHI}</option></select></div>
                            <div><label>${t('Chú thích', 'Caption')}</label><input name="chuThich" maxlength="200"></div>
                            <div><label>${t('Chú thích (English)', 'Caption (English)')}</label><input name="chuThichEn" maxlength="200"></div>
                            <button>${t('Thêm ảnh', 'Add photo')}</button>
                        </form>
                        <p id="anh-bac-si-trang-thai" class="ghi-chu"></p>
                    </div>
                    <h3>${t('Đánh giá của bệnh nhân', 'Patient reviews')}</h3>
                    <div id="ds-danh-gia-bac-si" class="bang"></div>
                </section>`);

            $('form-tim-bac-si').addEventListener('submit', (e) => { e.preventDefault(); taiDanhSach(); });
            khiGui('form-them-bac-si', async (v, form) => {
                const than = {
                    hoTen: v.hoTen.trim(), email: v.email.trim(), soDienThoai: v.soDienThoai.trim() || null, idChuyenKhoa: Number(v.idChuyenKhoa),
                    soGiayPhep: v.soGiayPhep.trim() || null, hocVi: v.hocVi.trim() || null, chucVu: v.chucVu.trim() || null,
                    namBatDauHanhNghe: v.namBatDauHanhNghe === '' ? null : Number(v.namBatDauHanhNghe),
                };
                const { status, json } = await goi(QUAN_TRI, than);
                if (status !== 201) {
                    bao('them-bac-si-trang-thai', thongDiepLoi(json, t('Không thêm được bác sĩ.', 'Could not add the doctor.')), 'loi');
                    return;
                }
                form.reset();
                bao('them-bac-si-trang-thai', t(`Đã thêm bác sĩ ${json.duLieu.maBacSi}. Bác sĩ đăng nhập bằng ${json.duLieu.email} và mật khẩu mặc định, rồi đặt mật khẩu của mình.`,
                    `Doctor ${json.duLieu.maBacSi} added. The doctor signs in with ${json.duLieu.email} and the default password, `
                    + 'then sets their own password.'), 'tot');
                await taiDanhSach(true);
                hienHoSoMoi(json.duLieu);
            });
            khiGui('form-co-ban-bac-si', async (v) => {
                const than = { hoTen: v.hoTen.trim(), soDienThoai: v.soDienThoai.trim() || null, idChuyenKhoa: Number(v.idChuyenKhoa), soGiayPhep: v.soGiayPhep.trim() || null };
                const { status, json } = await goi(`${QUAN_TRI}/${bacSiDangSua.id}`, than, 'PUT');
                if (status !== 200) {
                    bao('co-ban-bac-si-trang-thai', thongDiepLoi(json, t('Không lưu được thông tin bác sĩ.', 'Could not save the doctor details.')), 'loi');
                    return;
                }
                hienHoSo(json.duLieu);
                bao('co-ban-bac-si-trang-thai', json.thongDiep || t('Đã cập nhật thông tin bác sĩ.', 'Doctor details updated.'), 'tot');
                taiDanhSach(true);
            });
            khiGui('form-ho-so-bac-si', async (v) => {
                const than = { phienBan: bacSiDangSua.phienBanHoSo };
                for (const ten of CAC_O_CHU) than[ten] = v[ten].trim() || null;
                than.namBatDauHanhNghe = than.namBatDauHanhNghe === null ? null : Number(than.namBatDauHanhNghe);
                than.ngonNguKham = v.ngonNguKham.split(',').map((ma) => ma.trim()).filter(Boolean);
                for (const ten of [...CAC_DANH_SACH, ...CAC_DANH_SACH.map((muc) => muc + 'En')]) than[ten] = v[ten].split('\n').map((y) => y.trim()).filter(Boolean);
                const { status, json } = await goi(`${QUAN_TRI}/${bacSiDangSua.id}/ho-so`, than, 'PUT');
                if (status !== 200) {
                    const loi = thongDiepLoi(json, t('Không lưu được hồ sơ bác sĩ.', 'Could not save the doctor profile.'));
                    // Bác sĩ (hoặc quản trị viên khác) vừa lưu hồ sơ: tải lại bản mới, nội dung đang nhập trên form bị thay
                    if (json?.maLoi === 'DU_LIEU_DA_THAY_DOI') {
                        await moHoSo(bacSiDangSua.id);
                        bao('ho-so-bac-si-trang-thai', loi + t(' Đã tải lại hồ sơ mới nhất, hãy nhập lại thay đổi.',
                            ' The latest profile has been reloaded; enter your changes again.'), 'loi');
                        return;
                    }
                    bao('ho-so-bac-si-trang-thai', loi, 'loi');
                    return;
                }
                hienHoSo(json.duLieu);
                bao('ho-so-bac-si-trang-thai', json.thongDiep || t('Đã cập nhật hồ sơ bác sĩ.', 'Doctor profile updated.'), 'tot');
                taiDanhSach(true);
            });
            khiGui('form-anh-bac-si', async (v, form) => {
                // multipart: tệp "anh" cùng 2 tham số "loai", "chuThich" trong cùng FormData
                const duLieu = new FormData();
                duLieu.append('anh', form.elements.anh.files[0]);
                duLieu.append('loai', v.loai);
                if (v.chuThich.trim()) duLieu.append('chuThich', v.chuThich.trim());
                if (v.chuThichEn.trim()) duLieu.append('chuThichEn', v.chuThichEn.trim());
                const { status, json } = await goi(`${QUAN_TRI}/${bacSiDangSua.id}/anh`, duLieu);
                if (status !== 201 && status !== 200) {
                    bao('anh-bac-si-trang-thai', thongDiepLoi(json, t('Không thêm được ảnh.', 'Could not add the photo.')), 'loi');
                    return;
                }
                form.reset();
                bao('anh-bac-si-trang-thai', t('Đã thêm ảnh giới thiệu.', 'Profile photo added.'), 'tot');
                await moHoSo(bacSiDangSua.id);
            });

            const chuyenKhoa = await lay('/api/catalog/chuyen-khoa?kichThuoc=100', true);
            if (chuyenKhoa.status === 200 && $('qt-bs-chuyen-khoa')) {
                for (const o of [$('qt-bs-chuyen-khoa'), ...khung.querySelectorAll('.chon-chuyen-khoa')]) {
                    for (const ck of chuyenKhoa.json.duLieu.noiDung) {
                        const lua = the('option', ck.tenChuyenKhoa);
                        lua.value = ck.id;
                        o.append(lua);
                    }
                }
            }
            await taiDanhSach(true);
            if (/^\d+$/.test(thamSo.get('id') || '')) await moHoSo(thamSo.get('id'));
        },
    });
})();
