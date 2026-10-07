// Màn hình UI: Doctor Management (Admin) — danh bạ bác sĩ với "Add New Doctor" và "Edit"
// Đường dẫn:  /admin/doctors   (?id=<id bác sĩ> mở thẳng hồ sơ của 1 bác sĩ)
// Vai trò:    QUAN_TRI_VIEN
// API:
//   GET    /api/catalog/quan-tri/bac-si?tuKhoa=&idChuyenKhoa=&trangThai=&trang=&kichThuoc=   danh bạ (cả bác sĩ ngừng công tác):
//                                                                    maBacSi, hoTen, email, soDienThoai, tenChuyenKhoa, soLuotDaKham,
//                                                                    trangThai, trangThaiTaiKhoan, phaiDoiMatKhau
//   POST   /api/catalog/quan-tri/bac-si                              "Add New Doctor": { hoTen, email, soDienThoai?, idChuyenKhoa,
//                                                                    soGiayPhep?, hocVi?, chucVu?, soNamKinhNghiem? } -> 201. Tài khoản
//                                                                    mang mật khẩu mặc định, bác sĩ đặt mật khẩu ở lần đăng nhập đầu
//   PUT    /api/catalog/quan-tri/bac-si/{id}                         { hoTen, soDienThoai?, idChuyenKhoa, soGiayPhep? } (email không sửa)
//   GET    /api/catalog/quan-tri/bac-si/{id}/anh-huong-ngung-cong-tac   { soCaSapToi, soLichHenBiAnhHuong }
//   POST   /api/catalog/quan-tri/bac-si/{id}/ngung-cong-tac          { lyDo }: hủy mọi ca sắp tới, vô hiệu hoá tài khoản.
//                                                                    503 DICH_VU_NOI_BO_LOI: gửi lại để chạy nốt
//   POST   /api/catalog/quan-tri/bac-si/{id}/cong-tac-lai
//   GET    /api/catalog/quan-tri/bac-si/{id}                         hồ sơ giới thiệu, kể cả bác sĩ không hiển thị công khai
//   PUT    /api/catalog/quan-tri/bac-si/{id}/ho-so                   ghi đè mọi trường: hocVi, chucVu, soNamKinhNghiem, gioiThieuNgan,
//                                                                    tieuSu, quaTrinhDaoTao[], quaTrinhCongTac[], linhVucKhamChua[]
//   POST   /api/catalog/quan-tri/bac-si/{id}/anh                     multipart: tệp "anh" (JPEG / PNG / WebP, tối đa 4 MB),
//                                                                    "loai" = ANH_CONG_VIEC | CHUNG_CHI, "chuThich" -> 201
//   PUT    /api/catalog/quan-tri/bac-si/{id}/anh/{idAnh}             { loai, chuThich }
//   DELETE /api/catalog/quan-tri/bac-si/{id}/anh/{idAnh}
//   GET    /api/booking/quan-tri/danh-gia?idBacSi=&soSaoToiDa=&trang=&kichThuoc=   đánh giá của bệnh nhân, mới nhất trước, kèm tên
//                                                                    bệnh nhân và mã tra cứu (bác sĩ thì xem ẩn danh ở Dashboard)
//   (PUT /api/catalog/quan-tri/bac-si/{id}/anh/thu-tu { idAnh: [...] } sắp xếp ảnh: có API, trang demo không dùng)
(() => {
    const QUAN_TRI = '/api/catalog/quan-tri/bac-si';
    const CAC_DANH_SACH = ['quaTrinhDaoTao', 'quaTrinhCongTac', 'linhVucKhamChua'];
    const LOAI_ANH = { ANH_CONG_VIEC: 'Ảnh công việc', CHUNG_CHI: 'Chứng chỉ' };
    const TRANG_THAI_BAC_SI = { DANG_CONG_TAC: 'Active — đang công tác', NGUNG_CONG_TAC: 'Inactive — ngừng công tác' };
    let bacSiDangSua = null;   // HoSoBacSiQuanTriResponse đang mở

    async function taiDanhSach(imLang = false) {
        const thamSo = new URLSearchParams({ kichThuoc: 100, ...thanForm($('form-tim-bac-si'), ['idChuyenKhoa', 'tuKhoa', 'trangThai']) });
        const { status, json } = await lay(`${QUAN_TRI}?${thamSo}`, imLang);
        const khung = $('ds-bac-si-qt');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, 'Không tải được danh sách bác sĩ.'), 'ghi-chu loi'));
            return;
        }
        const ds = json.duLieu.noiDung;
        if (!ds.length) {
            khung.replaceChildren(ghiChu('No data yet — không có bác sĩ nào khớp.'));
            return;
        }
        khung.replaceChildren(bang(['Mã', 'Bác sĩ', 'Email / SĐT', 'Chuyên khoa', 'Đã khám', 'Trạng thái', 'Thao tác'], ds.map((bs) => dong([
            bs.maBacSi, tenBacSi(bs), [bs.email, bs.soDienThoai].filter(Boolean).join(' · '), bs.tenChuyenKhoa, bs.soLuotDaKham,
            (TRANG_THAI_BAC_SI[bs.trangThai] || bs.trangThai) + (bs.phaiDoiMatKhau ? ' · chưa đăng nhập lần nào' : ''),
            nut('Edit', () => moHoSo(bs.id)),
        ]))));
    }

    async function moHoSo(id) {
        const { status, json } = await lay(`${QUAN_TRI}/${id}`);
        if (!$('khoi-ho-so-bac-si')) return;
        $('khoi-ho-so-bac-si').classList.remove('an');
        if (status !== 200) {
            bacSiDangSua = null;
            $('bac-si-thong-tin').replaceChildren(the('p', thongDiepLoi(json, 'Không mở được hồ sơ bác sĩ.'), 'ghi-chu loi'));
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
            khung.replaceChildren(the('p', thongDiepLoi(json, 'Không tải được đánh giá.'), 'ghi-chu loi'));
            return;
        }
        const duLieu = json.duLieu;
        if (!duLieu.noiDung.length) {
            khung.replaceChildren(ghiChu('No data yet — bác sĩ này chưa có đánh giá nào.'));
            return;
        }
        khung.replaceChildren(ghiChu(`${duLieu.tongSoPhanTu} đánh giá, hiện ${duLieu.noiDung.length} đánh giá mới nhất.`),
            bang(['Số sao', 'Nhận xét', 'Bệnh nhân', 'Lịch hẹn', 'Ngày khám', 'Gửi lúc'], duLieu.noiDung.map((dg) => dong([
                '★'.repeat(dg.soSao) + ` (${dg.soSao})`, dg.nhanXet, `#${dg.idHoSoBenhNhan} ${dg.tenBenhNhan}`, dg.maTraCuu, ngayVn(dg.ngayKham), ngayGioVn(dg.ngayTao),
            ]))));
    }

    function hienHoSo(bs) {
        bacSiDangSua = bs;
        taiDanhGia(bs.id);
        $('khoi-sua-bac-si').classList.remove('an');
        $('bac-si-thong-tin').replaceChildren(anh(bs.anhDaiDien, 'Ảnh đại diện', 'anh-dai-dien lon'), thongTin([
            ['Mã', bs.maBacSi], ['Họ tên', bs.hoTen], ['Email đăng nhập', bs.email], ['Số điện thoại', bs.soDienThoai],
            ['Chuyên khoa', bs.tenChuyenKhoa], ['Số giấy phép', bs.soGiayPhep], ['Trạng thái', TRANG_THAI_BAC_SI[bs.trangThai] || bs.trangThai],
            ['Tài khoản', bs.trangThaiTaiKhoan + (bs.phaiDoiMatKhau ? ' · còn mật khẩu mặc định, chưa đăng nhập lần nào' : '')],
        ]));
        const cb = $('form-co-ban-bac-si');
        cb.elements.hoTen.value = bs.hoTen || '';
        cb.elements.soDienThoai.value = bs.soDienThoai || '';
        cb.elements.idChuyenKhoa.value = bs.idChuyenKhoa;
        cb.elements.soGiayPhep.value = bs.soGiayPhep || '';
        hienTrangThai(bs);
        const f = $('form-ho-so-bac-si');
        for (const ten of ['hocVi', 'chucVu', 'soNamKinhNghiem', 'gioiThieuNgan', 'tieuSu']) f.elements[ten].value = bs[ten] ?? '';
        for (const ten of CAC_DANH_SACH) f.elements[ten].value = (bs[ten] || []).join('\n');
        hienAnh(bs.anh || []);
    }

    // Khối "Activate / Deactivate": đang công tác -> xem trước ảnh hưởng rồi cho ngừng; đã ngừng -> cho công tác lại
    async function hienTrangThai(bs) {
        const khoi = $('bac-si-trang-thai');
        if (bs.trangThai === 'NGUNG_CONG_TAC') {
            khoi.replaceChildren(
                ghiChu(bs.trangThaiTaiKhoan === 'VO_HIEU_HOA'
                    ? 'Bác sĩ đã ngừng công tác: không hiện trong danh sách công khai, không đăng nhập được.'
                    : 'Bác sĩ đã ngừng công tác nhưng tài khoản chưa bị vô hiệu hoá (lần trước chạy dở): bấm "Chạy nốt" để hoàn tất.'),
                nut('Activate — cho công tác lại', () => doiTrangThai(`${QUAN_TRI}/${bs.id}/cong-tac-lai`, undefined)));
            if (bs.trangThaiTaiKhoan !== 'VO_HIEU_HOA') {
                khoi.append(' ', nut('Chạy nốt việc ngừng công tác', () => ngungCongTac(bs), 'nguy-hiem nho'));
            }
            return;
        }
        const { status, json } = await lay(`${QUAN_TRI}/${bs.id}/anh-huong-ngung-cong-tac`, true);
        if (!bacSiDangSua || bacSiDangSua.id !== bs.id) return;
        const ah = status === 200 ? json.duLieu : null;
        khoi.replaceChildren(
            ghiChu(ah ? `Nếu cho ngừng công tác ngay: ${ah.soCaSapToi} ca sắp tới bị hủy, ${ah.soLichHenBiAnhHuong} lịch hẹn được giữ và `
                + 'đánh dấu "cần đổi lịch" (bệnh nhân được báo), bác sĩ không đăng nhập được nữa.' : 'Không đọc được số ca / lịch hẹn bị ảnh hưởng.'),
            nut('Deactivate — cho ngừng công tác', () => ngungCongTac(bs), 'nguy-hiem nho'));
    }

    async function ngungCongTac(bs) {
        const lyDo = prompt('Lý do ngừng công tác (bắt buộc; gửi cho bác sĩ và các bệnh nhân có lịch hẹn bị ảnh hưởng):', '');
        if (lyDo === null) return;
        await doiTrangThai(`${QUAN_TRI}/${bs.id}/ngung-cong-tac`, { lyDo: lyDo.trim() });
    }

    async function doiTrangThai(duongDan, than) {
        const { status, json } = await goi(duongDan, than);
        if (status !== 200) {
            // 503 DICH_VU_NOI_BO_LOI: bác sĩ đã ở NGUNG_CONG_TAC nhưng chưa xong; mở lại hồ sơ để thấy nút "Chạy nốt"
            bao('co-ban-bac-si-trang-thai', thongDiepLoi(json, 'Không đổi được trạng thái bác sĩ.'), 'loi');
            if (bacSiDangSua) await moHoSo(bacSiDangSua.id);
            return;
        }
        const kq = json.duLieu;
        hienHoSo(kq.bacSi || kq);
        bao('co-ban-bac-si-trang-thai', (json.thongDiep || 'Đã đổi trạng thái.')
            + (kq.bacSi ? ` ${kq.soCaDaHuy} ca bị hủy, ${kq.soLichHenCanDoi} lịch hẹn cần đổi lịch.` : ''), 'tot');
        taiDanhSach(true);
    }

    function hienAnh(ds) {
        if (!ds.length) {
            $('bac-si-anh').replaceChildren(ghiChu('Chưa có ảnh giới thiệu nào.'));
            return;
        }
        const luoi = the('div', null, 'luoi-anh');
        for (const a of ds) {
            const hinh = document.createElement('figure');
            hinh.append(anh(a.url, a.chuThich), the('figcaption', `${LOAI_ANH[a.loai] || a.loai}${a.chuThich ? ': ' + a.chuThich : ''}`),
                nut('Sửa chú thích', () => suaAnh(a)), nut('Xoá ảnh', () => xoaAnh(a), 'nguy-hiem nho'));
            luoi.append(hinh);
        }
        $('bac-si-anh').replaceChildren(luoi);
    }

    async function suaAnh(a) {
        const chuThich = prompt('Chú thích mới (tối đa 200 ký tự, để trống = bỏ chú thích):', a.chuThich || '');
        if (chuThich === null) return;
        const { status, json } = await goi(`${QUAN_TRI}/${bacSiDangSua.id}/anh/${a.id}`, { loai: a.loai, chuThich: chuThich.trim() || null }, 'PUT');
        if (status === 200) await moHoSo(bacSiDangSua.id);
        else bao('anh-bac-si-trang-thai', thongDiepLoi(json, 'Không sửa được ảnh.'), 'loi');
    }

    async function xoaAnh(a) {
        if (!confirm('Xoá ảnh giới thiệu này?')) return;
        const { status, json } = await goi(`${QUAN_TRI}/${bacSiDangSua.id}/anh/${a.id}`, undefined, 'DELETE');
        if (status === 200) await moHoSo(bacSiDangSua.id);
        else bao('anh-bac-si-trang-thai', thongDiepLoi(json, 'Không xoá được ảnh.'), 'loi');
    }

    dangKy({
        duongDan: '/admin/doctors',
        vaiTro: 'QUAN_TRI_VIEN',
        tieuDe: 'Doctor Management',
        async ve(khung, { thamSo }) {
            bacSiDangSua = null;
            khuon(khung, `
                <section>
                    <h2>Doctor Directory — danh bạ bác sĩ</h2>
                    <form id="form-tim-bac-si" class="hai-cot">
                        <div><label>Chuyên khoa</label><select name="idChuyenKhoa" id="qt-bs-chuyen-khoa"><option value="">Tất cả</option></select></div>
                        <div><label>Trạng thái</label><select name="trangThai"><option value="">Tất cả</option>
                            <option value="DANG_CONG_TAC">Active — đang công tác</option><option value="NGUNG_CONG_TAC">Inactive — ngừng công tác</option></select></div>
                        <div><label>Tên, email, SĐT hoặc số giấy phép</label><input name="tuKhoa"></div>
                        <button class="phu">Tìm</button>
                    </form>
                    <div id="ds-bac-si-qt" class="bang"></div>
                </section>
                <section>
                    <h2>Add New Doctor — thêm bác sĩ</h2>
                    <p class="ghi-chu">Tạo tài khoản đăng nhập và hồ sơ bác sĩ trong 1 bước. Tài khoản mang mật khẩu mặc định của phòng khám
                        (DOCTOR_DEFAULT_PASSWORD, mặc định Doctor@123); ở lần đăng nhập đầu bác sĩ phải đặt mật khẩu của mình.</p>
                    <form id="form-them-bac-si">
                        <div class="hai-cot">
                            <div><label>Họ tên *</label><input name="hoTen" maxlength="150" required></div>
                            <div><label>Email đăng nhập *</label><input name="email" type="email" maxlength="255" required></div>
                            <div><label>Số điện thoại</label><input name="soDienThoai" pattern="0[0-9]{9}" placeholder="10 chữ số, bắt đầu bằng 0"></div>
                            <div><label>Chuyên khoa *</label><select name="idChuyenKhoa" class="chon-chuyen-khoa" required><option value="">— chọn —</option></select></div>
                            <div><label>Số giấy phép hành nghề</label><input name="soGiayPhep" maxlength="50"></div>
                            <div><label>Học vị</label><input name="hocVi" maxlength="100" placeholder="vd ThS.BS"></div>
                            <div><label>Chức vụ</label><input name="chucVu" maxlength="150"></div>
                            <div><label>Số năm kinh nghiệm</label><input name="soNamKinhNghiem" type="number" min="0" max="80"></div>
                        </div>
                        <button>Thêm bác sĩ</button>
                    </form>
                    <p id="them-bac-si-trang-thai" class="ghi-chu"></p>
                </section>
                <section id="khoi-ho-so-bac-si" class="an">
                    <h2>Edit — bác sĩ đang chọn</h2>
                    <div id="bac-si-thong-tin"></div>
                    <div id="khoi-sua-bac-si">
                        <h3>Thông tin cơ bản</h3>
                        <form id="form-co-ban-bac-si">
                            <div class="hai-cot">
                                <div><label>Họ tên *</label><input name="hoTen" maxlength="150" required></div>
                                <div><label>Số điện thoại (bỏ trống = xoá)</label><input name="soDienThoai" pattern="0[0-9]{9}"></div>
                                <div><label>Chuyên khoa * (không đổi được khi còn ca sắp tới)</label><select name="idChuyenKhoa" class="chon-chuyen-khoa" required></select></div>
                                <div><label>Số giấy phép (bỏ trống = xoá)</label><input name="soGiayPhep" maxlength="50"></div>
                            </div>
                            <button>Lưu thông tin cơ bản</button>
                        </form>
                        <h3>Activate / Deactivate</h3>
                        <div id="bac-si-trang-thai"></div>
                        <p id="co-ban-bac-si-trang-thai" class="ghi-chu"></p>
                        <h3>Hồ sơ giới thiệu</h3>
                        <form id="form-ho-so-bac-si">
                            <div class="hai-cot">
                                <div><label>Học vị</label><input name="hocVi" maxlength="100"></div>
                                <div><label>Chức vụ</label><input name="chucVu" maxlength="150"></div>
                                <div><label>Số năm kinh nghiệm</label><input name="soNamKinhNghiem" type="number" min="0" max="80"></div>
                            </div>
                            <label>Giới thiệu ngắn (hiện trên thẻ bác sĩ)</label><input name="gioiThieuNgan" maxlength="300">
                            <label>Tiểu sử</label><textarea name="tieuSu" rows="3" maxlength="5000"></textarea>
                            <label>Quá trình đào tạo (mỗi dòng 1 ý, tối đa 20 ý)</label><textarea name="quaTrinhDaoTao" rows="3"></textarea>
                            <label>Quá trình công tác (mỗi dòng 1 ý)</label><textarea name="quaTrinhCongTac" rows="3"></textarea>
                            <label>Lĩnh vực khám chữa (mỗi dòng 1 ý)</label><textarea name="linhVucKhamChua" rows="3"></textarea>
                            <button>Lưu hồ sơ (ghi đè mọi trường, trường bỏ trống bị xoá trắng)</button>
                        </form>
                        <p id="ho-so-bac-si-trang-thai" class="ghi-chu"></p>
                        <h3>Ảnh giới thiệu và chứng chỉ</h3>
                        <div id="bac-si-anh"></div>
                        <form id="form-anh-bac-si" class="hai-cot">
                            <div><label>Tệp ảnh (JPEG / PNG / WebP, tối đa 4 MB)</label><input name="anh" type="file" accept="image/jpeg,image/png,image/webp" required></div>
                            <div><label>Loại</label><select name="loai"><option value="ANH_CONG_VIEC">Ảnh công việc</option><option value="CHUNG_CHI">Chứng chỉ</option></select></div>
                            <div><label>Chú thích</label><input name="chuThich" maxlength="200"></div>
                            <button>Thêm ảnh</button>
                        </form>
                        <p id="anh-bac-si-trang-thai" class="ghi-chu"></p>
                    </div>
                    <h3>Đánh giá của bệnh nhân</h3>
                    <div id="ds-danh-gia-bac-si" class="bang"></div>
                </section>`);

            $('form-tim-bac-si').addEventListener('submit', (e) => { e.preventDefault(); taiDanhSach(); });
            khiGui('form-them-bac-si', async (v, form) => {
                const than = {
                    hoTen: v.hoTen.trim(), email: v.email.trim(), soDienThoai: v.soDienThoai.trim() || null, idChuyenKhoa: Number(v.idChuyenKhoa),
                    soGiayPhep: v.soGiayPhep.trim() || null, hocVi: v.hocVi.trim() || null, chucVu: v.chucVu.trim() || null,
                    soNamKinhNghiem: v.soNamKinhNghiem === '' ? null : Number(v.soNamKinhNghiem),
                };
                const { status, json } = await goi(QUAN_TRI, than);
                if (status !== 201) {
                    bao('them-bac-si-trang-thai', thongDiepLoi(json, 'Không thêm được bác sĩ.'), 'loi');
                    return;
                }
                form.reset();
                bao('them-bac-si-trang-thai', `Đã thêm bác sĩ ${json.duLieu.maBacSi}. Bác sĩ đăng nhập bằng ${json.duLieu.email} và mật khẩu mặc định, `
                    + 'rồi đặt mật khẩu của mình.', 'tot');
                await taiDanhSach(true);
                hienHoSoMoi(json.duLieu);
            });
            khiGui('form-co-ban-bac-si', async (v) => {
                const than = { hoTen: v.hoTen.trim(), soDienThoai: v.soDienThoai.trim() || null, idChuyenKhoa: Number(v.idChuyenKhoa), soGiayPhep: v.soGiayPhep.trim() || null };
                const { status, json } = await goi(`${QUAN_TRI}/${bacSiDangSua.id}`, than, 'PUT');
                if (status !== 200) {
                    bao('co-ban-bac-si-trang-thai', thongDiepLoi(json, 'Không lưu được thông tin bác sĩ.'), 'loi');
                    return;
                }
                hienHoSo(json.duLieu);
                bao('co-ban-bac-si-trang-thai', json.thongDiep || 'Đã cập nhật thông tin bác sĩ.', 'tot');
                taiDanhSach(true);
            });
            khiGui('form-ho-so-bac-si', async (v) => {
                const than = {
                    hocVi: v.hocVi.trim() || null, chucVu: v.chucVu.trim() || null,
                    soNamKinhNghiem: v.soNamKinhNghiem === '' ? null : Number(v.soNamKinhNghiem),
                    gioiThieuNgan: v.gioiThieuNgan.trim() || null, tieuSu: v.tieuSu.trim() || null,
                };
                for (const ten of CAC_DANH_SACH) than[ten] = v[ten].split('\n').map((y) => y.trim()).filter(Boolean);
                const { status, json } = await goi(`${QUAN_TRI}/${bacSiDangSua.id}/ho-so`, than, 'PUT');
                if (status !== 200) {
                    bao('ho-so-bac-si-trang-thai', thongDiepLoi(json, 'Không lưu được hồ sơ bác sĩ.'), 'loi');
                    return;
                }
                hienHoSo(json.duLieu);
                bao('ho-so-bac-si-trang-thai', json.thongDiep || 'Đã cập nhật hồ sơ bác sĩ.', 'tot');
                taiDanhSach(true);
            });
            khiGui('form-anh-bac-si', async (v, form) => {
                // multipart: tệp "anh" cùng 2 tham số "loai", "chuThich" trong cùng FormData
                const duLieu = new FormData();
                duLieu.append('anh', form.elements.anh.files[0]);
                duLieu.append('loai', v.loai);
                if (v.chuThich.trim()) duLieu.append('chuThich', v.chuThich.trim());
                const { status, json } = await goi(`${QUAN_TRI}/${bacSiDangSua.id}/anh`, duLieu);
                if (status !== 201 && status !== 200) {
                    bao('anh-bac-si-trang-thai', thongDiepLoi(json, 'Không thêm được ảnh.'), 'loi');
                    return;
                }
                form.reset();
                bao('anh-bac-si-trang-thai', 'Đã thêm ảnh giới thiệu.', 'tot');
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
