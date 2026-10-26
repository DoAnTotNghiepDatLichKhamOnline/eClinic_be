// Màn hình ngoài danh sách UI: trang cá nhân của bệnh nhân
// Đường dẫn:  /patient/profile
// Vai trò:    BENH_NHAN
// API:
//   GET    /api/booking/trang-ca-nhan/cua-toi                            hồ sơ, số lịch hẹn, người thân đã lưu, các lần khám gần đây
//          (kết quả khám có maSoKham / linkSoKham: link sổ khám bệnh + mã QR, vẽ ở ketQuaKham() của chung.js)
//   GET    /api/booking/ho-so-benh-nhan/cua-toi                          hồ sơ bệnh nhân của tài khoản (404 = chưa có)
//   PUT    /api/booking/ho-so-benh-nhan/cua-toi                          tạo / sửa hồ sơ (số CCCD chỉ nhập được khi tạo)
//   PUT    /api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id}       sửa bản lưu của 1 người thân
//   DELETE /api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/{id}       bỏ 1 người thân đã lưu
// Hồ sơ ở trạng thái CHO_XAC_MINH (CCCD đã có hồ sơ nhưng thông tin không khớp) chưa xem / sửa được cho tới khi quản trị
// viên duyệt ở /admin/patients.
(() => {
    const TRUONG_HO_SO = ['hoTen', 'ngaySinh', 'gioiTinh', 'soDienThoai', 'cccd', 'soBaoHiemYTe', 'diaChi', 'tienSuBenhLy'];
    const TRUONG_NGUOI_THAN = ['hoTen', 'ngaySinh', 'gioiTinh', 'soDienThoai', 'email', 'soBaoHiemYTe', 'diaChi'];
    let caNhan = null;            // kết quả trang-ca-nhan/cua-toi
    let nguoiThanDangSua = null;

    async function taiCaNhan() {
        const { status, json } = await lay('/api/booking/trang-ca-nhan/cua-toi', true);
        if (!$('bn-so-lich')) return;
        if (status !== 200) {
            bao('bn-ca-nhan-trang-thai', thongDiepLoi(json, t('Không tải được trang cá nhân.', 'Could not load the personal page.')), 'loi');
            return;
        }
        caNhan = json.duLieu;
        bao('bn-ca-nhan-trang-thai', t('Tài khoản: ', 'Account: ') + caNhan.emailTaiKhoan);
        const so = caNhan.soLich;
        $('bn-so-lich').replaceChildren(thongTin([
            [t('Sắp tới của tôi', 'My upcoming'), String(so.sapToiCuaToi)],
            [t('Sắp tới của người khác', 'Upcoming for others'), String(so.sapToiCuaNguoiKhac)],
            [t('Lịch sử (đã khám, đã hủy, đã qua giờ)', 'History (completed, cancelled, past)'), String(so.lichSu)],
            [t('Đã khám, xem được kết quả', 'Completed, results available'), String(so.daKham)],
        ]));
        hienNguoiThan();
        const cacLan = caNhan.lanKhamGanDay.map((lan) => {
            const khoi = the('div', null, 'lan-kham');
            khoi.append(the('b', t(`${ngayGioVn(lan.lichHen.gioKhamDuKien)} · ${lan.lichHen.hoTenBenhNhan} · BS ${lan.lichHen.bacSi.hoTen} (${lan.lichHen.tenChuyenKhoa})`,
                `${ngayGioVn(lan.lichHen.gioKhamDuKien)} · ${lan.lichHen.hoTenBenhNhan} · Dr. ${lan.lichHen.bacSi.hoTen} (${lan.lichHen.tenChuyenKhoa})`)),
                ketQuaKham(lan.ketQua));
            return khoi;
        });
        $('bn-lan-kham').replaceChildren(...(cacLan.length ? cacLan : [ghiChu(t('Chưa có lần khám nào có kết quả.', 'No visits with results yet.'))]));
    }

    function hienNguoiThan() {
        const ds = caNhan.nguoiThan;
        if (!ds.length) {
            $('bn-nguoi-than').replaceChildren(ghiChu(t('Chưa lưu người thân nào (người thân được lưu khi bạn đặt lịch hộ).',
                'No saved relatives yet (a relative is saved when you book an appointment for them).')));
            return;
        }
        $('bn-nguoi-than').replaceChildren(bang([t('Họ tên', 'Full name'), t('Ngày sinh', 'Date of birth'), t('CCCD', 'National ID'),
            t('Số điện thoại', 'Phone number'), t('Người giám hộ', 'Guardian'), ''], ds.map((nt) => dong([
            nt.hoTen, ngayVn(nt.ngaySinh), nt.cccd || t('(chưa có)', '(none)'), nt.soDienThoai, nt.nguoiGiamHo ? nt.nguoiGiamHo.hoTen : '',
            [nut(t('Sửa', 'Edit'), () => moSuaNguoiThan(nt)), nut(t('Bỏ', 'Remove'), () => boNguoiThan(nt))],
        ]))));
    }

    function moSuaNguoiThan(nt) {
        nguoiThanDangSua = nt;
        const f = $('form-nguoi-than');
        for (const ten of TRUONG_NGUOI_THAN) f.elements[ten].value = nt[ten] || (ten === 'gioiTinh' ? 'NAM' : '');
        $('tieu-de-nguoi-than').textContent = t(`Sửa người thân: ${nt.hoTen}`, `Edit relative: ${nt.hoTen}`)
            + (nt.cccd ? t(` (CCCD ${nt.cccd})`, ` (national ID ${nt.cccd})`) : '');
        bao('nguoi-than-trang-thai', '');
        f.classList.remove('an');
    }

    async function boNguoiThan(nt) {
        if (!confirm(t(`Bỏ "${nt.hoTen}" khỏi danh sách người thân đã lưu? Lịch đã đặt không bị ảnh hưởng.`,
            `Remove "${nt.hoTen}" from your saved relatives? Booked appointments are not affected.`))) return;
        await goi('/api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/' + nt.id, undefined, 'DELETE');
        $('form-nguoi-than').classList.add('an');
        await taiCaNhan();
    }

    // ===== Hồ sơ bệnh nhân của tôi =====
    function hienHoSo(hs) {
        const form = $('form-ho-so');
        for (const ten of TRUONG_HO_SO) form.elements[ten].value = (hs && hs[ten]) || '';
        // Số CCCD chỉ nhập được khi tạo hồ sơ; đã có hồ sơ thì khoá
        form.elements.cccd.readOnly = !!(hs && hs.cccd);
        $('ho-so-trang-thai').textContent = !hs
            ? t('Tài khoản chưa có hồ sơ bệnh nhân: nhập đủ thông tin (kể cả số CCCD) rồi bấm Lưu để tạo.',
                'This account has no patient profile yet: fill in all the details (including the national ID number), then click Save to create it.')
            : hs.trangThaiLienKet === 'CHO_XAC_MINH'
                ? t('Số CCCD của bạn đã có hồ sơ bệnh nhân nhưng thông tin không khớp: hồ sơ đang chờ phòng khám xác minh, chưa xem / sửa được.',
                    'Your national ID number already has a patient profile but the details do not match: the profile is awaiting verification by the clinic and cannot be viewed or edited yet.')
                : t(`Trạng thái liên kết: ${LIEN_KET[hs.trangThaiLienKet] || hs.trangThaiLienKet}.`,
                    `Link status: ${LIEN_KET[hs.trangThaiLienKet] || hs.trangThaiLienKet}.`);
    }

    async function taiHoSo() {
        const { status, json } = await lay('/api/booking/ho-so-benh-nhan/cua-toi', true);
        if (!$('form-ho-so')) return;
        if (status === 200) hienHoSo(json.duLieu);
        else if (status === 404) hienHoSo(null);
        else $('ho-so-trang-thai').textContent = thongDiepLoi(json, t('Không tải được hồ sơ.', 'Could not load the profile.'));
    }

    dangKy({
        duongDan: '/patient/profile',
        vaiTro: 'BENH_NHAN',
        tieuDe: cap('Hồ sơ bệnh nhân của tôi', 'My Patient Profile'),
        async ve(khung) {
            khuon(khung, `
                <section>
                    <h2>${t('Hồ sơ bệnh nhân của tôi', 'My patient profile')}</h2>
                    <p id="ho-so-trang-thai" class="ghi-chu">${t('Đang tải…', 'Loading…')}</p>
                    <form id="form-ho-so">
                        <div class="hai-cot">
                            <div><label>${t('Họ tên', 'Full name')}</label><input name="hoTen" required maxlength="150"></div>
                            <div><label>${t('Ngày sinh', 'Date of birth')}</label><input name="ngaySinh" type="date" required></div>
                            <div><label>${t('Giới tính', 'Gender')}</label>
                                <select name="gioiTinh">
                                    <option value="">${t('(không chọn)', '(not selected)')}</option>
                                    <option value="NAM">${t('Nam', 'Male')}</option>
                                    <option value="NU">${t('Nữ', 'Female')}</option>
                                    <option value="KHAC">${t('Khác', 'Other')}</option>
                                </select></div>
                            <div><label>${t('Số điện thoại', 'Phone number')}</label><input name="soDienThoai" required></div>
                            <div><label>${t('Số CCCD (không đổi được sau khi đã có hồ sơ)', 'National ID number (cannot be changed once the profile exists)')}</label><input name="cccd"></div>
                            <div><label>${t('Số bảo hiểm y tế', 'Health insurance number')}</label><input name="soBaoHiemYTe" maxlength="50"></div>
                        </div>
                        <label>${t('Địa chỉ', 'Address')}</label>
                        <input name="diaChi" maxlength="500">
                        <label>${t('Tiền sử bệnh lý', 'Medical history')}</label>
                        <textarea name="tienSuBenhLy" rows="2" maxlength="5000"></textarea>
                        <button>${t('Lưu hồ sơ', 'Save profile')}</button>
                    </form>
                    <p id="thong-bao-ho-so" class="ghi-chu"></p>
                </section>
                <section>
                    <h2>${t('Trang cá nhân', 'Personal page')}</h2>
                    <p id="bn-ca-nhan-trang-thai" class="ghi-chu"></p>
                    <h3>${t('Số lịch hẹn', 'Appointment counts')}</h3>
                    <div id="bn-so-lich"></div>
                    <h3>${t('Người thân đã lưu', 'Saved relatives')}</h3>
                    <div id="bn-nguoi-than" class="bang"></div>
                    <form id="form-nguoi-than" class="an">
                        <h3 id="tieu-de-nguoi-than">${t('Sửa người thân', 'Edit relative')}</h3>
                        <div class="hai-cot">
                            <div><label>${t('Họ tên', 'Full name')}</label><input name="hoTen" required maxlength="150"></div>
                            <div><label>${t('Ngày sinh', 'Date of birth')}</label><input name="ngaySinh" type="date" required></div>
                            <div><label>${t('Giới tính', 'Gender')}</label><select name="gioiTinh"><option value="NAM">${t('Nam', 'Male')}</option><option value="NU">${t('Nữ', 'Female')}</option><option value="KHAC">${t('Khác', 'Other')}</option></select></div>
                            <div><label>${t('Số điện thoại', 'Phone number')}</label><input name="soDienThoai" required pattern="0\\d{9}"></div>
                            <div><label>Email</label><input name="email" type="email"></div>
                            <div><label>${t('Số thẻ bảo hiểm y tế', 'Health insurance number')}</label><input name="soBaoHiemYTe" maxlength="50"></div>
                        </div>
                        <label>${t('Địa chỉ', 'Address')}</label><input name="diaChi" maxlength="500">
                        <p class="ghi-chu">${t('Số CCCD không đổi được. Người giám hộ đã lưu (nếu có) được giữ nguyên. Chỉ sửa bản lưu của tài khoản, không sửa hồ sơ ở phòng khám.',
                            'The national ID number cannot be changed. The saved guardian (if any) is kept. This only edits the copy saved in your account, not the profile at the clinic.')}</p>
                        <div class="hai-cot"><button>${t('Lưu', 'Save')}</button><button type="button" id="nut-huy-nguoi-than" class="phu">${t('Hủy', 'Cancel')}</button></div>
                        <p id="nguoi-than-trang-thai" class="ghi-chu"></p>
                    </form>
                    <h3>${t('Các lần khám gần đây', 'Recent visits')}</h3>
                    <div id="bn-lan-kham"></div>
                </section>`);

            $('form-ho-so').addEventListener('submit', async (e) => {
                e.preventDefault();
                const v = giaTriForm(e.target);
                // Trường tuỳ chọn để trống thì gửi null (server xoá trắng trường đó)
                const than = {};
                for (const ten of TRUONG_HO_SO) than[ten] = v[ten] || null;
                const { status, json } = await goi('/api/booking/ho-so-benh-nhan/cua-toi', than, 'PUT');
                if (status !== 200) {
                    bao('thong-bao-ho-so', thongDiepLoi(json, t('Lưu hồ sơ thất bại.', 'Could not save the profile.')), 'loi');
                    return;
                }
                hienHoSo(json.duLieu);
                bao('thong-bao-ho-so', json.thongDiep || t('Đã lưu hồ sơ.', 'Profile saved.'), 'tot');
            });
            $('nut-huy-nguoi-than').addEventListener('click', () => $('form-nguoi-than').classList.add('an'));
            $('form-nguoi-than').addEventListener('submit', async (e) => {
                e.preventDefault();
                const than = thanForm(e.target, TRUONG_NGUOI_THAN);
                if (nguoiThanDangSua.nguoiGiamHo) than.nguoiGiamHo = nguoiThanDangSua.nguoiGiamHo;
                const { status, json } = await goi('/api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/' + nguoiThanDangSua.id, than, 'PUT');
                if (status !== 200) {
                    bao('nguoi-than-trang-thai', thongDiepLoi(json, t('Không lưu được.', 'Could not save.')), 'loi');
                    return;
                }
                e.target.classList.add('an');
                await taiCaNhan();
            });

            await taiHoSo();
            await taiCaNhan();
        },
    });
})();
