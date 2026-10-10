// Màn hình UI: Medical Catalog (Admin) — danh mục y tế
// Đường dẫn:  /admin/medical-catalog
// Vai trò:    QUAN_TRI_VIEN
// API (chuyên khoa):
//   GET    /api/catalog/chuyen-khoa?tuKhoa=&trang=&kichThuoc=   tìm theo tên (công khai, phân trang)
//   POST   /api/catalog/chuyen-khoa        { tenChuyenKhoa, moTa } -> 201
//   PUT    /api/catalog/chuyen-khoa/{id}   { tenChuyenKhoa, moTa }
//   DELETE /api/catalog/chuyen-khoa/{id}   chỉ xoá được khi không còn phòng khám / bác sĩ
// API (phòng khám):
//   GET  /api/catalog/quan-tri/phong-kham?tuKhoa=&idChuyenKhoa=&trangThai=&trang=&kichThuoc=   mọi phòng, kể cả phòng ngừng hoạt động:
//                                                         id, tenPhong, tang, idChuyenKhoa, tenChuyenKhoa, trangThai, soCaSapToi
//   POST /api/catalog/quan-tri/phong-kham                 { tenPhong, tang?, idChuyenKhoa } -> 201 (409 TEN_PHONG_DA_TON_TAI)
//   PUT  /api/catalog/quan-tri/phong-kham/{id}            cùng thân; đổi chuyên khoa khi phòng còn ca sắp tới: 409 PHONG_CON_CA_LAM_VIEC
//   POST /api/catalog/quan-tri/phong-kham/{id}/ngung-hoat-dong   409 PHONG_CON_CA_LAM_VIEC nếu còn ca sắp tới (chuyển ca sang phòng khác trước)
//   POST /api/catalog/quan-tri/phong-kham/{id}/hoat-dong-lai
// API (thuốc):
//   GET  /api/medical/quan-tri/thuoc?tuKhoa=&daXacMinh=&trangThai=&trang=&kichThuoc=   chưa xác minh trước, rồi theo tên:
//                                                         id, tenThuoc, donVi, moTa, daXacMinh, trangThai, tenBacSiTao, soLanKe
//   POST /api/medical/quan-tri/thuoc                      { tenThuoc, donVi?, moTa? } -> 201, đã xác minh (409 TEN_THUOC_DA_TON_TAI)
//   PUT  /api/medical/quan-tri/thuoc/{id}                 cùng thân; thuốc đã được kê mà đổi sang tên khác: 409 THUOC_DA_DUOC_KE
//   POST /api/medical/quan-tri/thuoc/{id}/xac-minh        xác minh thuốc bác sĩ tự thêm khi kê đơn
//   POST /api/medical/quan-tri/thuoc/{id}/ngung-dung      không còn được gợi ý, không kê mới được (409 THUOC_NGUNG_DUNG phía bác sĩ)
//   POST /api/medical/quan-tri/thuoc/{id}/dung-lai
(() => {
    const CHUYEN_KHOA = '/api/catalog/chuyen-khoa';
    const PHONG = '/api/catalog/quan-tri/phong-kham';
    const THUOC = '/api/medical/quan-tri/thuoc';
    const TRANG_THAI_PHONG = { HOAT_DONG: 'Active — đang hoạt động', NGUNG_HOAT_DONG: 'Inactive — ngừng hoạt động' };
    const TRANG_THAI_THUOC = { DANG_DUNG: 'Đang dùng', NGUNG_DUNG: 'Ngừng dùng' };
    let dangSua = null;   // chuyên khoa đang sửa (null = thêm mới)
    let phongDangSua = null, thuocDangSua = null;
    let trangThuoc = 0, tongSoTrangThuoc = 0;

    // ----- Phòng khám -----
    function datFormPhong(p) {
        phongDangSua = p;
        const f = $('form-phong');
        f.elements.tenPhong.value = p ? p.tenPhong : '';
        f.elements.tang.value = p && p.tang ? p.tang : '';
        if (p) f.elements.idChuyenKhoa.value = p.idChuyenKhoa;
        $('tieu-de-form-phong').textContent = p ? `Sửa phòng khám #${p.id}` : 'Thêm phòng khám';
        $('nut-huy-sua-phong').classList.toggle('an', !p);
    }

    async function taiPhong(imLang = false) {
        const thamSo = new URLSearchParams({ kichThuoc: 100, ...thanForm($('form-tim-phong'), ['tuKhoa', 'idChuyenKhoa', 'trangThai']) });
        const { status, json } = await lay(`${PHONG}?${thamSo}`, imLang);
        const khung = $('ds-phong');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, 'Không tải được phòng khám.'), 'ghi-chu loi'));
            return;
        }
        const ds = json.duLieu.noiDung;
        if (!ds.length) {
            khung.replaceChildren(ghiChu('No data yet — chưa có phòng khám nào khớp.'));
            return;
        }
        khung.replaceChildren(bang(['Id', 'Tên phòng', 'Tầng', 'Chuyên khoa', 'Ca sắp tới', 'Trạng thái', 'Thao tác'], ds.map((p) => dong([
            p.id, p.tenPhong, p.tang, p.tenChuyenKhoa, p.soCaSapToi, TRANG_THAI_PHONG[p.trangThai] || p.trangThai,
            [nut('Sửa', () => datFormPhong(p)), p.trangThai === 'HOAT_DONG'
                ? nut('Ngừng hoạt động', () => doiTrangThaiPhong(p, 'ngung-hoat-dong'), 'nguy-hiem nho')
                : nut('Hoạt động lại', () => doiTrangThaiPhong(p, 'hoat-dong-lai'), 'phu nho')],
        ]))));
    }

    async function doiTrangThaiPhong(p, hanhDong) {
        const { status, json } = await goi(`${PHONG}/${p.id}/${hanhDong}`);
        bao('phong-trang-thai', status === 200 ? json.thongDiep : thongDiepLoi(json, 'Không thực hiện được.'), status === 200 ? 'tot' : 'loi');
        if (status === 200) await taiPhong(true);
    }

    // ----- Thuốc -----
    function datFormThuoc(t) {
        thuocDangSua = t;
        const f = $('form-thuoc');
        f.elements.tenThuoc.value = t ? t.tenThuoc : '';
        f.elements.donVi.value = t && t.donVi ? t.donVi : '';
        f.elements.moTa.value = t && t.moTa ? t.moTa : '';
        $('tieu-de-form-thuoc').textContent = t
            ? `Sửa thuốc #${t.id}` + (t.soLanKe > 0 ? ` (đã kê ${t.soLanKe} lần: tên chỉ sửa được cách viết hoa / khoảng trắng)` : '')
            : 'Thêm thuốc';
        $('nut-huy-sua-thuoc').classList.toggle('an', !t);
    }

    async function taiThuoc(imLang = false) {
        const thamSo = new URLSearchParams({ trang: trangThuoc, kichThuoc: 20, ...thanForm($('form-tim-thuoc'), ['tuKhoa', 'daXacMinh', 'trangThai']) });
        const { status, json } = await lay(`${THUOC}?${thamSo}`, imLang);
        const khung = $('ds-thuoc');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, 'Không tải được danh mục thuốc.'), 'ghi-chu loi'));
            return;
        }
        const duLieu = json.duLieu;
        tongSoTrangThuoc = duLieu.tongSoTrang;
        $('trang-thuoc').textContent = `Trang ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} thuốc`;
        if (!duLieu.noiDung.length) {
            khung.replaceChildren(ghiChu('No data yet — chưa có thuốc nào khớp.'));
            return;
        }
        khung.replaceChildren(bang(['Id', 'Tên thuốc', 'Đơn vị', 'Mô tả', 'Xác minh', 'Đã kê', 'Trạng thái', 'Thao tác'], duLieu.noiDung.map((t) => dong([
            t.id, t.tenThuoc, t.donVi, t.moTa,
            t.daXacMinh ? 'Đã xác minh' : 'Chưa xác minh' + (t.tenBacSiTao ? ` · bác sĩ ${t.tenBacSiTao} thêm` : ''),
            t.soLanKe, TRANG_THAI_THUOC[t.trangThai] || t.trangThai,
            [nut('Sửa', () => datFormThuoc(t)),
                ...(t.daXacMinh ? [] : [nut('Xác minh', () => doiThuoc(t, 'xac-minh'))]),
                t.trangThai === 'DANG_DUNG'
                    ? nut('Ngừng dùng', () => doiThuoc(t, 'ngung-dung'), 'nguy-hiem nho')
                    : nut('Dùng lại', () => doiThuoc(t, 'dung-lai'), 'phu nho')],
        ]))));
    }

    async function doiThuoc(t, hanhDong) {
        const { status, json } = await goi(`${THUOC}/${t.id}/${hanhDong}`);
        bao('thuoc-trang-thai', status === 200 ? json.thongDiep : thongDiepLoi(json, 'Không thực hiện được.'), status === 200 ? 'tot' : 'loi');
        if (status === 200) await taiThuoc(true);
    }

    async function napChuyenKhoaChoPhong() {
        const { status, json } = await lay(`${CHUYEN_KHOA}?kichThuoc=100`, true);
        if (status !== 200 || !$('form-phong')) return;
        for (const [o, rong] of [[$('form-phong').elements.idChuyenKhoa, null], [$('form-tim-phong').elements.idChuyenKhoa, '(mọi chuyên khoa)']]) {
            const dangChon = o.value;
            o.replaceChildren();
            if (rong) o.append(new Option(rong, ''));
            for (const ck of json.duLieu.noiDung) o.append(new Option(ck.tenChuyenKhoa, ck.id));
            if (dangChon) o.value = dangChon;
        }
    }

    function datForm(ck) {
        dangSua = ck;
        const f = $('form-chuyen-khoa');
        f.elements.tenChuyenKhoa.value = ck ? ck.tenChuyenKhoa : '';
        f.elements.moTa.value = ck && ck.moTa ? ck.moTa : '';
        $('tieu-de-form-chuyen-khoa').textContent = ck ? `Sửa chuyên khoa #${ck.id}` : 'Thêm chuyên khoa';
        $('nut-huy-sua-chuyen-khoa').classList.toggle('an', !ck);
    }

    async function taiChuyenKhoa(imLang = false) {
        const tuKhoa = $('form-tim-chuyen-khoa').elements.tuKhoa.value.trim();
        const { status, json } = await lay(`${CHUYEN_KHOA}?kichThuoc=100` + (tuKhoa ? `&tuKhoa=${encodeURIComponent(tuKhoa)}` : ''), imLang);
        const khung = $('ds-chuyen-khoa');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, 'Không tải được chuyên khoa.'), 'ghi-chu loi'));
            return;
        }
        const ds = json.duLieu.noiDung;
        if (!ds.length) {
            khung.replaceChildren(ghiChu('No data yet — chưa có chuyên khoa nào khớp.'));
            return;
        }
        khung.replaceChildren(bang(['Id', 'Tên chuyên khoa', 'Mô tả', 'Thao tác'], ds.map((ck) => dong([
            ck.id, ck.tenChuyenKhoa, ck.moTa,
            [nut('Sửa', () => datForm(ck)), nut('Xoá', () => xoa(ck), 'nguy-hiem nho')],
        ]))));
    }

    async function xoa(ck) {
        if (!confirm(`Xoá chuyên khoa "${ck.tenChuyenKhoa}"?`)) return;
        const { status, json } = await goi(`${CHUYEN_KHOA}/${ck.id}`, undefined, 'DELETE');
        bao('chuyen-khoa-trang-thai', status === 200 ? 'Đã xoá chuyên khoa.' : thongDiepLoi(json, 'Không xoá được.'), status === 200 ? 'tot' : 'loi');
        if (status === 200) {
            if (dangSua && dangSua.id === ck.id) datForm(null);
            await taiChuyenKhoa(true);
        }
    }

    dangKy({
        duongDan: '/admin/medical-catalog',
        vaiTro: 'QUAN_TRI_VIEN',
        tieuDe: 'Medical Catalog',
        async ve(khung) {
            khuon(khung, `
                <section>
                    <h2>Chuyên khoa</h2>
                    <form id="form-tim-chuyen-khoa" class="hai-cot">
                        <input name="tuKhoa" placeholder="Tìm theo tên chuyên khoa">
                        <button class="phu">Tìm</button>
                    </form>
                    <div id="ds-chuyen-khoa" class="bang"></div>
                    <h3 id="tieu-de-form-chuyen-khoa">Thêm chuyên khoa</h3>
                    <form id="form-chuyen-khoa">
                        <label>Tên chuyên khoa</label><input name="tenChuyenKhoa" required maxlength="150">
                        <label>Mô tả</label><textarea name="moTa" rows="2" maxlength="2000"></textarea>
                        <div class="hai-cot"><button>Lưu</button><button type="button" id="nut-huy-sua-chuyen-khoa" class="phu an">Hủy sửa</button></div>
                    </form>
                    <p id="chuyen-khoa-trang-thai" class="ghi-chu"></p>
                </section>`);
            khuon(khung, `
                <section>
                    <h2>Phòng khám</h2>
                    <form id="form-tim-phong" class="hai-cot">
                        <input name="tuKhoa" placeholder="Tìm theo tên phòng, tầng">
                        <select name="idChuyenKhoa"><option value="">(mọi chuyên khoa)</option></select>
                        <select name="trangThai"><option value="">(mọi trạng thái)</option><option value="HOAT_DONG">Đang hoạt động</option><option value="NGUNG_HOAT_DONG">Ngừng hoạt động</option></select>
                        <button class="phu">Tìm</button>
                    </form>
                    <div id="ds-phong" class="bang"></div>
                    <p class="ghi-chu">Phòng còn ca sắp tới thì chưa ngừng hoạt động và chưa đổi chuyên khoa được: chuyển các ca đó sang phòng khác ở Schedule Management trước.</p>
                    <h3 id="tieu-de-form-phong">Thêm phòng khám</h3>
                    <form id="form-phong">
                        <div class="hai-cot">
                            <div><label>Tên phòng</label><input name="tenPhong" required maxlength="100"></div>
                            <div><label>Tầng</label><input name="tang" maxlength="20"></div>
                            <div><label>Chuyên khoa</label><select name="idChuyenKhoa" required></select></div>
                        </div>
                        <div class="hai-cot"><button>Lưu</button><button type="button" id="nut-huy-sua-phong" class="phu an">Hủy sửa</button></div>
                    </form>
                    <p id="phong-trang-thai" class="ghi-chu"></p>
                </section>
                <section>
                    <h2>Thuốc</h2>
                    <form id="form-tim-thuoc" class="hai-cot">
                        <input name="tuKhoa" placeholder="Tìm theo tên thuốc">
                        <select name="daXacMinh"><option value="">(xác minh: tất cả)</option><option value="false">Chưa xác minh</option><option value="true">Đã xác minh</option></select>
                        <select name="trangThai"><option value="">(mọi trạng thái)</option><option value="DANG_DUNG">Đang dùng</option><option value="NGUNG_DUNG">Ngừng dùng</option></select>
                        <button class="phu">Tìm</button>
                    </form>
                    <div id="ds-thuoc" class="bang"></div>
                    <p id="trang-thuoc" class="ghi-chu"></p>
                    <button id="nut-thuoc-truoc" class="phu nho">‹ Trang trước</button>
                    <button id="nut-thuoc-sau" class="phu nho">Trang sau ›</button>
                    <h3 id="tieu-de-form-thuoc">Thêm thuốc</h3>
                    <form id="form-thuoc">
                        <div class="hai-cot">
                            <div><label>Tên thuốc</label><input name="tenThuoc" required maxlength="255"></div>
                            <div><label>Đơn vị (viên, ml, gói...)</label><input name="donVi" maxlength="30"></div>
                        </div>
                        <label>Mô tả</label><textarea name="moTa" rows="2" maxlength="2000"></textarea>
                        <div class="hai-cot"><button>Lưu</button><button type="button" id="nut-huy-sua-thuoc" class="phu an">Hủy sửa</button></div>
                    </form>
                    <p id="thuoc-trang-thai" class="ghi-chu"></p>
                </section>`);

            phongDangSua = null;
            thuocDangSua = null;
            trangThuoc = 0;
            $('form-tim-phong').addEventListener('submit', (e) => { e.preventDefault(); taiPhong(); });
            $('nut-huy-sua-phong').addEventListener('click', () => datFormPhong(null));
            khiGui('form-phong', async (v) => {
                const than = { tenPhong: v.tenPhong.trim(), tang: v.tang.trim() || null, idChuyenKhoa: Number(v.idChuyenKhoa) };
                const { status, json } = phongDangSua
                    ? await goi(`${PHONG}/${phongDangSua.id}`, than, 'PUT')
                    : await goi(PHONG, than);
                if (status !== 200 && status !== 201) {
                    bao('phong-trang-thai', thongDiepLoi(json, 'Không lưu được phòng khám.'), 'loi');
                    return;
                }
                bao('phong-trang-thai', json.thongDiep || 'Đã lưu phòng khám.', 'tot');
                datFormPhong(null);
                await taiPhong(true);
            });
            $('form-tim-thuoc').addEventListener('submit', (e) => { e.preventDefault(); trangThuoc = 0; taiThuoc(); });
            $('nut-thuoc-truoc').addEventListener('click', () => {
                if (trangThuoc > 0) {
                    trangThuoc--;
                    taiThuoc();
                }
            });
            $('nut-thuoc-sau').addEventListener('click', () => {
                if (trangThuoc + 1 < tongSoTrangThuoc) {
                    trangThuoc++;
                    taiThuoc();
                }
            });
            $('nut-huy-sua-thuoc').addEventListener('click', () => datFormThuoc(null));
            khiGui('form-thuoc', async (v) => {
                const than = { tenThuoc: v.tenThuoc.trim(), donVi: v.donVi.trim() || null, moTa: v.moTa.trim() || null };
                const { status, json } = thuocDangSua
                    ? await goi(`${THUOC}/${thuocDangSua.id}`, than, 'PUT')
                    : await goi(THUOC, than);
                if (status !== 200 && status !== 201) {
                    bao('thuoc-trang-thai', thongDiepLoi(json, 'Không lưu được thuốc.'), 'loi');
                    return;
                }
                bao('thuoc-trang-thai', json.thongDiep || 'Đã lưu thuốc.', 'tot');
                datFormThuoc(null);
                await taiThuoc(true);
            });

            dangSua = null;
            $('form-tim-chuyen-khoa').addEventListener('submit', (e) => { e.preventDefault(); taiChuyenKhoa(); });
            $('nut-huy-sua-chuyen-khoa').addEventListener('click', () => datForm(null));
            khiGui('form-chuyen-khoa', async (v) => {
                const than = { tenChuyenKhoa: v.tenChuyenKhoa.trim(), moTa: v.moTa.trim() || null };
                const { status, json } = dangSua
                    ? await goi(`${CHUYEN_KHOA}/${dangSua.id}`, than, 'PUT')
                    : await goi(CHUYEN_KHOA, than);
                if (status !== 200 && status !== 201) {
                    bao('chuyen-khoa-trang-thai', thongDiepLoi(json, 'Không lưu được chuyên khoa.'), 'loi');
                    return;
                }
                bao('chuyen-khoa-trang-thai', json.thongDiep || 'Đã lưu chuyên khoa.', 'tot');
                datForm(null);
                await taiChuyenKhoa(true);
                await napChuyenKhoaChoPhong();
            });
            await taiChuyenKhoa(true);
            await napChuyenKhoaChoPhong();
            await taiPhong(true);
            await taiThuoc(true);
        },
    });
})();
