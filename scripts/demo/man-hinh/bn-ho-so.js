// Màn hình ngoài danh sách UI: trang cá nhân của bệnh nhân
// Đường dẫn:  /patient/profile
// Vai trò:    BENH_NHAN
// API:
//   GET    /api/booking/trang-ca-nhan/cua-toi                            hồ sơ, số lịch hẹn, người thân đã lưu, các lần khám gần đây
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
            bao('bn-ca-nhan-trang-thai', thongDiepLoi(json, 'Không tải được trang cá nhân.'), 'loi');
            return;
        }
        caNhan = json.duLieu;
        bao('bn-ca-nhan-trang-thai', 'Tài khoản: ' + caNhan.emailTaiKhoan);
        const so = caNhan.soLich;
        $('bn-so-lich').replaceChildren(thongTin([
            ['Sắp tới của tôi', String(so.sapToiCuaToi)], ['Sắp tới của người khác', String(so.sapToiCuaNguoiKhac)],
            ['Lịch sử (đã khám, đã hủy, đã qua giờ)', String(so.lichSu)], ['Đã khám, xem được kết quả', String(so.daKham)],
        ]));
        hienNguoiThan();
        const cacLan = caNhan.lanKhamGanDay.map((lan) => {
            const khoi = the('div', null, 'lan-kham');
            khoi.append(the('b', `${ngayGioVn(lan.lichHen.gioKhamDuKien)} · ${lan.lichHen.hoTenBenhNhan} · BS ${lan.lichHen.bacSi.hoTen} (${lan.lichHen.tenChuyenKhoa})`),
                ketQuaKham(lan.ketQua));
            return khoi;
        });
        $('bn-lan-kham').replaceChildren(...(cacLan.length ? cacLan : [ghiChu('Chưa có lần khám nào có kết quả.')]));
    }

    function hienNguoiThan() {
        const ds = caNhan.nguoiThan;
        if (!ds.length) {
            $('bn-nguoi-than').replaceChildren(ghiChu('Chưa lưu người thân nào (người thân được lưu khi bạn đặt lịch hộ).'));
            return;
        }
        $('bn-nguoi-than').replaceChildren(bang(['Họ tên', 'Ngày sinh', 'CCCD', 'Số điện thoại', 'Người giám hộ', ''], ds.map((nt) => dong([
            nt.hoTen, ngayVn(nt.ngaySinh), nt.cccd || '(chưa có)', nt.soDienThoai, nt.nguoiGiamHo ? nt.nguoiGiamHo.hoTen : '',
            [nut('Sửa', () => moSuaNguoiThan(nt)), nut('Bỏ', () => boNguoiThan(nt))],
        ]))));
    }

    function moSuaNguoiThan(nt) {
        nguoiThanDangSua = nt;
        const f = $('form-nguoi-than');
        for (const ten of TRUONG_NGUOI_THAN) f.elements[ten].value = nt[ten] || (ten === 'gioiTinh' ? 'NAM' : '');
        $('tieu-de-nguoi-than').textContent = `Sửa người thân: ${nt.hoTen}` + (nt.cccd ? ` (CCCD ${nt.cccd})` : '');
        bao('nguoi-than-trang-thai', '');
        f.classList.remove('an');
    }

    async function boNguoiThan(nt) {
        if (!confirm(`Bỏ "${nt.hoTen}" khỏi danh sách người thân đã lưu? Lịch đã đặt không bị ảnh hưởng.`)) return;
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
            ? 'Tài khoản chưa có hồ sơ bệnh nhân: nhập đủ thông tin (kể cả số CCCD) rồi bấm Lưu để tạo.'
            : hs.trangThaiLienKet === 'CHO_XAC_MINH'
                ? 'Số CCCD của bạn đã có hồ sơ bệnh nhân nhưng thông tin không khớp: hồ sơ đang chờ phòng khám xác minh, chưa xem / sửa được.'
                : `Trạng thái liên kết: ${LIEN_KET[hs.trangThaiLienKet] || hs.trangThaiLienKet}.`;
    }

    async function taiHoSo() {
        const { status, json } = await lay('/api/booking/ho-so-benh-nhan/cua-toi', true);
        if (!$('form-ho-so')) return;
        if (status === 200) hienHoSo(json.duLieu);
        else if (status === 404) hienHoSo(null);
        else $('ho-so-trang-thai').textContent = thongDiepLoi(json, 'Không tải được hồ sơ.');
    }

    dangKy({
        duongDan: '/patient/profile',
        vaiTro: 'BENH_NHAN',
        tieuDe: 'My Patient Profile',
        async ve(khung) {
            khuon(khung, `
                <section>
                    <h2>Hồ sơ bệnh nhân của tôi</h2>
                    <p id="ho-so-trang-thai" class="ghi-chu">Đang tải…</p>
                    <form id="form-ho-so">
                        <div class="hai-cot">
                            <div><label>Họ tên</label><input name="hoTen" required maxlength="150"></div>
                            <div><label>Ngày sinh</label><input name="ngaySinh" type="date" required></div>
                            <div><label>Giới tính</label>
                                <select name="gioiTinh">
                                    <option value="">(không chọn)</option>
                                    <option value="NAM">Nam</option>
                                    <option value="NU">Nữ</option>
                                    <option value="KHAC">Khác</option>
                                </select></div>
                            <div><label>Số điện thoại</label><input name="soDienThoai" required></div>
                            <div><label>Số CCCD (không đổi được sau khi đã có hồ sơ)</label><input name="cccd"></div>
                            <div><label>Số bảo hiểm y tế</label><input name="soBaoHiemYTe" maxlength="50"></div>
                        </div>
                        <label>Địa chỉ</label>
                        <input name="diaChi" maxlength="500">
                        <label>Tiền sử bệnh lý</label>
                        <textarea name="tienSuBenhLy" rows="2" maxlength="5000"></textarea>
                        <button>Lưu hồ sơ</button>
                    </form>
                    <p id="thong-bao-ho-so" class="ghi-chu"></p>
                </section>
                <section>
                    <h2>Trang cá nhân</h2>
                    <p id="bn-ca-nhan-trang-thai" class="ghi-chu"></p>
                    <h3>Số lịch hẹn</h3>
                    <div id="bn-so-lich"></div>
                    <h3>Người thân đã lưu</h3>
                    <div id="bn-nguoi-than" class="bang"></div>
                    <form id="form-nguoi-than" class="an">
                        <h3 id="tieu-de-nguoi-than">Sửa người thân</h3>
                        <div class="hai-cot">
                            <div><label>Họ tên</label><input name="hoTen" required maxlength="150"></div>
                            <div><label>Ngày sinh</label><input name="ngaySinh" type="date" required></div>
                            <div><label>Giới tính</label><select name="gioiTinh"><option value="NAM">Nam</option><option value="NU">Nữ</option><option value="KHAC">Khác</option></select></div>
                            <div><label>Số điện thoại</label><input name="soDienThoai" required pattern="0\\d{9}"></div>
                            <div><label>Email</label><input name="email" type="email"></div>
                            <div><label>Số thẻ bảo hiểm y tế</label><input name="soBaoHiemYTe" maxlength="50"></div>
                        </div>
                        <label>Địa chỉ</label><input name="diaChi" maxlength="500">
                        <p class="ghi-chu">Số CCCD không đổi được. Người giám hộ đã lưu (nếu có) được giữ nguyên. Chỉ sửa bản lưu của tài khoản, không sửa hồ sơ ở phòng khám.</p>
                        <div class="hai-cot"><button>Lưu</button><button type="button" id="nut-huy-nguoi-than" class="phu">Hủy</button></div>
                        <p id="nguoi-than-trang-thai" class="ghi-chu"></p>
                    </form>
                    <h3>Các lần khám gần đây</h3>
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
                    bao('thong-bao-ho-so', thongDiepLoi(json, 'Lưu hồ sơ thất bại.'), 'loi');
                    return;
                }
                hienHoSo(json.duLieu);
                bao('thong-bao-ho-so', json.thongDiep || 'Đã lưu hồ sơ.', 'tot');
            });
            $('nut-huy-nguoi-than').addEventListener('click', () => $('form-nguoi-than').classList.add('an'));
            $('form-nguoi-than').addEventListener('submit', async (e) => {
                e.preventDefault();
                const than = thanForm(e.target, TRUONG_NGUOI_THAN);
                if (nguoiThanDangSua.nguoiGiamHo) than.nguoiGiamHo = nguoiThanDangSua.nguoiGiamHo;
                const { status, json } = await goi('/api/booking/thong-tin-dat-lich/cua-toi/nguoi-than/' + nguoiThanDangSua.id, than, 'PUT');
                if (status !== 200) {
                    bao('nguoi-than-trang-thai', thongDiepLoi(json, 'Không lưu được.'), 'loi');
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
