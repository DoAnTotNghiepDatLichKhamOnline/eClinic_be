// Màn hình UI: Medical Catalog (Admin) — danh mục y tế
// Đường dẫn:  /admin/medical-catalog
// Vai trò:    QUAN_TRI_VIEN
// API (chuyên khoa):
//   GET    /api/catalog/chuyen-khoa?tuKhoa=&trang=&kichThuoc=   tìm theo tên (công khai, phân trang)
//   GET    /api/catalog/quan-tri/chuyen-khoa?tuKhoa=&trang=&kichThuoc=   id, tenChuyenKhoa, tenChuyenKhoaEn, moTa, moTaEn,
//                                          trieuChung, trieuChungEn (triệu chứng thường gặp: chatbot dùng để gợi ý chuyên khoa)
//                                          (chữ đã lưu của cả 2 ngôn ngữ; GET /api/catalog/chuyen-khoa chỉ trả 1 ngôn ngữ)
//   POST   /api/catalog/chuyen-khoa        { tenChuyenKhoa, tenChuyenKhoaEn?, moTa, moTaEn?, trieuChung?, trieuChungEn? } -> 201
//   PUT    /api/catalog/chuyen-khoa/{id}   { tenChuyenKhoa, tenChuyenKhoaEn?, moTa, moTaEn?, trieuChung?, trieuChungEn? }
//   DELETE /api/catalog/chuyen-khoa/{id}   chỉ xoá được khi không còn phòng khám / bác sĩ
// API (phòng khám):
//   GET  /api/catalog/quan-tri/phong-kham?tuKhoa=&idChuyenKhoa=&trangThai=&trang=&kichThuoc=   mọi phòng, kể cả phòng ngừng hoạt động:
//                                                         id, tenPhong, tenPhongEn, tang, tangEn, idChuyenKhoa, tenChuyenKhoa, trangThai, soCaSapToi
//   POST /api/catalog/quan-tri/phong-kham                 { tenPhong, tenPhongEn?, tang?, tangEn?, idChuyenKhoa } -> 201 (409 TEN_PHONG_DA_TON_TAI)
//   PUT  /api/catalog/quan-tri/phong-kham/{id}            cùng thân; đổi chuyên khoa khi phòng còn ca sắp tới: 409 PHONG_CON_CA_LAM_VIEC
//   POST /api/catalog/quan-tri/phong-kham/{id}/ngung-hoat-dong   409 PHONG_CON_CA_LAM_VIEC nếu còn ca sắp tới (chuyển ca sang phòng khác trước)
//   POST /api/catalog/quan-tri/phong-kham/{id}/hoat-dong-lai
// API (thuốc):
//   GET  /api/medical/quan-tri/thuoc?tuKhoa=&daXacMinh=&trangThai=&trang=&kichThuoc=   chưa xác minh trước, rồi theo tên, hàm lượng;
//                                                         tuKhoa khớp tên hoặc hoạt chất: id, tenThuoc, hoatChat, hamLuong, dangBaoChe,
//                                                         dangBaoCheEn, duongDung, donVi, donViEn, moTa, moTaEn, daXacMinh, trangThai,
//                                                         tenBacSiTao, soLanKe
//   POST /api/medical/quan-tri/thuoc                      { tenThuoc, hoatChat, hamLuong, dangBaoChe?, dangBaoCheEn?, duongDung?, donVi?,
//                                                         donViEn?, moTa?, moTaEn? } -> 201, đã xác minh (409 TEN_THUOC_DA_TON_TAI: trùng tên + hàm lượng)
//   PUT  /api/medical/quan-tri/thuoc/{id}                 cùng thân; thuốc đã được kê mà đổi tên / hoạt chất / hàm lượng / dạng bào chế /
//                                                         đường dùng: 409 THUOC_DA_DUOC_KE (trường đang trống thì điền được)
//   POST /api/medical/quan-tri/thuoc/{id}/xac-minh        xác minh thuốc bác sĩ tự thêm khi kê đơn trước đây (409 THUOC_THIEU_THONG_TIN nếu
//                                                         chưa có hoạt chất / hàm lượng)
//   POST /api/medical/quan-tri/thuoc/{id}/ngung-dung      không còn được gợi ý, không kê mới được (409 THUOC_NGUNG_DUNG phía bác sĩ)
//   POST /api/medical/quan-tri/thuoc/{id}/dung-lai
(() => {
    const CHUYEN_KHOA = '/api/catalog/chuyen-khoa';
    const CHUYEN_KHOA_QUAN_TRI = '/api/catalog/quan-tri/chuyen-khoa';
    const PHONG = '/api/catalog/quan-tri/phong-kham';
    const THUOC = '/api/medical/quan-tri/thuoc';
    const TRANG_THAI_PHONG = nhan({
        HOAT_DONG: cap('Active — đang hoạt động', 'Active'), NGUNG_HOAT_DONG: cap('Inactive — ngừng hoạt động', 'Inactive'),
    });
    const TRANG_THAI_THUOC = nhan({ DANG_DUNG: cap('Đang dùng', 'In use'), NGUNG_DUNG: cap('Ngừng dùng', 'Discontinued') });
    let dangSua = null;   // chuyên khoa đang sửa (null = thêm mới)
    let phongDangSua = null, thuocDangSua = null;
    let trangThuoc = 0, tongSoTrangThuoc = 0;

    // ----- Phòng khám -----
    function datFormPhong(p) {
        phongDangSua = p;
        const f = $('form-phong');
        f.elements.tenPhong.value = p ? p.tenPhong : '';
        f.elements.tang.value = p && p.tang ? p.tang : '';
        f.elements.tenPhongEn.value = p && p.tenPhongEn ? p.tenPhongEn : '';
        f.elements.tangEn.value = p && p.tangEn ? p.tangEn : '';
        if (p) f.elements.idChuyenKhoa.value = p.idChuyenKhoa;
        $('tieu-de-form-phong').textContent = p ? t(`Sửa phòng khám #${p.id}`, `Edit room #${p.id}`) : t('Thêm phòng khám', 'Add room');
        $('nut-huy-sua-phong').classList.toggle('an', !p);
    }

    async function taiPhong(imLang = false) {
        const thamSo = new URLSearchParams({ kichThuoc: 100, ...thanForm($('form-tim-phong'), ['tuKhoa', 'idChuyenKhoa', 'trangThai']) });
        const { status, json } = await lay(`${PHONG}?${thamSo}`, imLang);
        const khung = $('ds-phong');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được phòng khám.', 'Could not load the rooms.')), 'ghi-chu loi'));
            return;
        }
        const ds = json.duLieu.noiDung;
        if (!ds.length) {
            khung.replaceChildren(ghiChu(t('Chưa có phòng khám nào khớp.', 'No matching rooms.')));
            return;
        }
        khung.replaceChildren(bang(['Id', t('Tên phòng', 'Room name'), t('Tầng', 'Floor'), t('Chuyên khoa', 'Specialty'),
            t('Ca sắp tới', 'Upcoming shifts'), t('Trạng thái', 'Status'), t('Thao tác', 'Actions')], ds.map((p) => dong([
            p.id, theoNgonNgu(p.tenPhong, p.tenPhongEn), theoNgonNgu(p.tang, p.tangEn), p.tenChuyenKhoa, p.soCaSapToi, TRANG_THAI_PHONG[p.trangThai] || p.trangThai,
            [nut(t('Sửa', 'Edit'), () => datFormPhong(p)), p.trangThai === 'HOAT_DONG'
                ? nut(t('Ngừng hoạt động', 'Deactivate'), () => doiTrangThaiPhong(p, 'ngung-hoat-dong'), 'nguy-hiem nho')
                : nut(t('Hoạt động lại', 'Activate'), () => doiTrangThaiPhong(p, 'hoat-dong-lai'), 'phu nho')],
        ]))));
    }

    async function doiTrangThaiPhong(p, hanhDong) {
        const { status, json } = await goi(`${PHONG}/${p.id}/${hanhDong}`);
        bao('phong-trang-thai', status === 200 ? json.thongDiep : thongDiepLoi(json, t('Không thực hiện được.', 'The action failed.')), status === 200 ? 'tot' : 'loi');
        if (status === 200) await taiPhong(true);
    }

    // ----- Thuốc -----
    function datFormThuoc(thuoc) {
        thuocDangSua = thuoc;
        const f = $('form-thuoc');
        f.elements.tenThuoc.value = thuoc ? thuoc.tenThuoc : '';
        f.elements.hoatChat.value = thuoc && thuoc.hoatChat ? thuoc.hoatChat : '';
        f.elements.hamLuong.value = thuoc && thuoc.hamLuong ? thuoc.hamLuong : '';
        f.elements.dangBaoChe.value = thuoc && thuoc.dangBaoChe ? thuoc.dangBaoChe : '';
        f.elements.dangBaoCheEn.value = thuoc && thuoc.dangBaoCheEn ? thuoc.dangBaoCheEn : '';
        f.elements.duongDung.value = thuoc && thuoc.duongDung ? thuoc.duongDung : '';
        f.elements.donVi.value = thuoc && thuoc.donVi ? thuoc.donVi : '';
        f.elements.moTa.value = thuoc && thuoc.moTa ? thuoc.moTa : '';
        f.elements.donViEn.value = thuoc && thuoc.donViEn ? thuoc.donViEn : '';
        f.elements.moTaEn.value = thuoc && thuoc.moTaEn ? thuoc.moTaEn : '';
        $('tieu-de-form-thuoc').textContent = thuoc
            ? t(`Sửa thuốc #${thuoc.id}`, `Edit medicine #${thuoc.id}`) + (thuoc.soLanKe > 0
                ? t(` (đã kê ${thuoc.soLanKe} lần: tên chỉ sửa được cách viết hoa / khoảng trắng; hoạt chất, hàm lượng, dạng bào chế, đường dùng không đổi được, chỉ điền được ô còn trống)`,
                    ` (prescribed ${thuoc.soLanKe} times: only the capitalisation / spacing of the name can change; active ingredient, strength, dosage form and route are fixed, empty ones can be filled in)`) : '')
            : t('Thêm thuốc', 'Add medicine');
        $('nut-huy-sua-thuoc').classList.toggle('an', !thuoc);
    }

    async function taiThuoc(imLang = false) {
        const thamSo = new URLSearchParams({ trang: trangThuoc, kichThuoc: 20, ...thanForm($('form-tim-thuoc'), ['tuKhoa', 'daXacMinh', 'trangThai']) });
        const { status, json } = await lay(`${THUOC}?${thamSo}`, imLang);
        const khung = $('ds-thuoc');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được danh mục thuốc.', 'Could not load the medicine catalog.')), 'ghi-chu loi'));
            return;
        }
        const duLieu = json.duLieu;
        tongSoTrangThuoc = duLieu.tongSoTrang;
        $('trang-thuoc').textContent = t(`Trang ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} thuốc`,
            `Page ${duLieu.trang + 1} / ${Math.max(duLieu.tongSoTrang, 1)} · ${duLieu.tongSoPhanTu} medicines`);
        if (!duLieu.noiDung.length) {
            khung.replaceChildren(ghiChu(t('Chưa có thuốc nào khớp.', 'No matching medicines.')));
            return;
        }
        khung.replaceChildren(bang(['Id', t('Tên thuốc', 'Medicine name'), t('Hoạt chất', 'Active ingredient'), t('Hàm lượng', 'Strength'),
            t('Dạng bào chế', 'Dosage form'), t('Đường dùng', 'Route'), t('Đơn vị', 'Unit'), t('Mô tả', 'Description'), t('Xác minh', 'Verification'),
            t('Đã kê', 'Prescribed'), t('Trạng thái', 'Status'), t('Thao tác', 'Actions')], duLieu.noiDung.map((thuoc) => dong([
            thuoc.id, thuoc.tenThuoc, thuoc.hoatChat || '—', thuoc.hamLuong || '—', theoNgonNgu(thuoc.dangBaoChe, thuoc.dangBaoCheEn),
            thuoc.duongDung ? DUONG_DUNG[thuoc.duongDung] : '', theoNgonNgu(thuoc.donVi, thuoc.donViEn), theoNgonNgu(thuoc.moTa, thuoc.moTaEn),
            thuoc.daXacMinh ? t('Đã xác minh', 'Verified') : t('Chưa xác minh', 'Not verified')
                + (thuoc.tenBacSiTao ? t(` · bác sĩ ${thuoc.tenBacSiTao} thêm`, ` · added by Dr. ${thuoc.tenBacSiTao}`) : ''),
            thuoc.soLanKe, TRANG_THAI_THUOC[thuoc.trangThai] || thuoc.trangThai,
            [nut(t('Sửa', 'Edit'), () => datFormThuoc(thuoc)),
                ...(thuoc.daXacMinh ? [] : [nut(t('Xác minh', 'Verify'), () => doiThuoc(thuoc, 'xac-minh'))]),
                thuoc.trangThai === 'DANG_DUNG'
                    ? nut(t('Ngừng dùng', 'Discontinue'), () => doiThuoc(thuoc, 'ngung-dung'), 'nguy-hiem nho')
                    : nut(t('Dùng lại', 'Use again'), () => doiThuoc(thuoc, 'dung-lai'), 'phu nho')],
        ]))));
    }

    async function doiThuoc(thuoc, hanhDong) {
        const { status, json } = await goi(`${THUOC}/${thuoc.id}/${hanhDong}`);
        bao('thuoc-trang-thai', status === 200 ? json.thongDiep : thongDiepLoi(json, t('Không thực hiện được.', 'The action failed.')), status === 200 ? 'tot' : 'loi');
        if (status === 200) await taiThuoc(true);
    }

    async function napChuyenKhoaChoPhong() {
        const { status, json } = await lay(`${CHUYEN_KHOA}?kichThuoc=100`, true);
        if (status !== 200 || !$('form-phong')) return;
        for (const [o, rong] of [[$('form-phong').elements.idChuyenKhoa, null], [$('form-tim-phong').elements.idChuyenKhoa, t('(mọi chuyên khoa)', '(all specialties)')]]) {
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
        f.elements.tenChuyenKhoaEn.value = ck && ck.tenChuyenKhoaEn ? ck.tenChuyenKhoaEn : '';
        f.elements.moTaEn.value = ck && ck.moTaEn ? ck.moTaEn : '';
        f.elements.trieuChung.value = ck && ck.trieuChung ? ck.trieuChung : '';
        f.elements.trieuChungEn.value = ck && ck.trieuChungEn ? ck.trieuChungEn : '';
        $('tieu-de-form-chuyen-khoa').textContent = ck ? t(`Sửa chuyên khoa #${ck.id}`, `Edit specialty #${ck.id}`) : t('Thêm chuyên khoa', 'Add specialty');
        $('nut-huy-sua-chuyen-khoa').classList.toggle('an', !ck);
    }

    async function taiChuyenKhoa(imLang = false) {
        const tuKhoa = $('form-tim-chuyen-khoa').elements.tuKhoa.value.trim();
        const { status, json } = await lay(`${CHUYEN_KHOA_QUAN_TRI}?kichThuoc=100` + (tuKhoa ? `&tuKhoa=${encodeURIComponent(tuKhoa)}` : ''), imLang);
        const khung = $('ds-chuyen-khoa');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được chuyên khoa.', 'Could not load the specialties.')), 'ghi-chu loi'));
            return;
        }
        const ds = json.duLieu.noiDung;
        if (!ds.length) {
            khung.replaceChildren(ghiChu(t('Chưa có chuyên khoa nào khớp.', 'No matching specialties.')));
            return;
        }
        khung.replaceChildren(bang(['Id', t('Tên chuyên khoa', 'Specialty name'), t('Mô tả', 'Description'), t('Thao tác', 'Actions')], ds.map((ck) => dong([
            ck.id, theoNgonNgu(ck.tenChuyenKhoa, ck.tenChuyenKhoaEn), theoNgonNgu(ck.moTa, ck.moTaEn),
            [nut(t('Sửa', 'Edit'), () => datForm(ck)), nut(t('Xoá', 'Delete'), () => xoa(ck), 'nguy-hiem nho')],
        ]))));
    }

    async function xoa(ck) {
        if (!confirm(t(`Xoá chuyên khoa "${ck.tenChuyenKhoa}"?`, `Delete the specialty "${ck.tenChuyenKhoa}"?`))) return;
        const { status, json } = await goi(`${CHUYEN_KHOA}/${ck.id}`, undefined, 'DELETE');
        bao('chuyen-khoa-trang-thai', status === 200 ? t('Đã xoá chuyên khoa.', 'Specialty deleted.') : thongDiepLoi(json, t('Không xoá được.', 'Could not delete.')), status === 200 ? 'tot' : 'loi');
        if (status === 200) {
            if (dangSua && dangSua.id === ck.id) datForm(null);
            await taiChuyenKhoa(true);
        }
    }

    dangKy({
        duongDan: '/admin/medical-catalog',
        vaiTro: 'QUAN_TRI_VIEN',
        tieuDe: cap('Danh mục y tế', 'Medical Catalog'),
        async ve(khung) {
            khuon(khung, `
                <section>
                    <h2>${t('Chuyên khoa', 'Specialties')}</h2>
                    <form id="form-tim-chuyen-khoa" class="hai-cot">
                        <input name="tuKhoa" placeholder="${t('Tìm theo tên chuyên khoa (Việt hoặc Anh)', 'Search by specialty name (Vietnamese or English)')}">
                        <button class="phu">${t('Tìm', 'Search')}</button>
                    </form>
                    <div id="ds-chuyen-khoa" class="bang"></div>
                    <h3 id="tieu-de-form-chuyen-khoa">${t('Thêm chuyên khoa', 'Add specialty')}</h3>
                    <form id="form-chuyen-khoa">
                        <label>${t('Tên chuyên khoa', 'Specialty name')}</label><input name="tenChuyenKhoa" required maxlength="150">
                        <label>${t('Mô tả', 'Description')}</label><textarea name="moTa" rows="2" maxlength="2000"></textarea>
                        <label>${t('Tên chuyên khoa (English, bỏ trống = người xem tiếng Anh thấy tên tiếng Việt)', 'Specialty name (English, empty = English viewers see the Vietnamese name)')}</label><input name="tenChuyenKhoaEn" maxlength="150">
                        <label>${t('Mô tả (English)', 'Description (English)')}</label><textarea name="moTaEn" rows="2" maxlength="2000"></textarea>
                        <label>${t('Triệu chứng thường gặp (chatbot dựa vào đây để gợi ý chuyên khoa; không hiện công khai)', 'Common symptoms (the chatbot uses these to suggest a specialty; not shown publicly)')}</label><textarea name="trieuChung" rows="2" maxlength="2000"></textarea>
                        <label>${t('Triệu chứng thường gặp (English)', 'Common symptoms (English)')}</label><textarea name="trieuChungEn" rows="2" maxlength="2000"></textarea>
                        <div class="hai-cot"><button>${t('Lưu', 'Save')}</button><button type="button" id="nut-huy-sua-chuyen-khoa" class="phu an">${t('Hủy sửa', 'Cancel editing')}</button></div>
                    </form>
                    <p id="chuyen-khoa-trang-thai" class="ghi-chu"></p>
                </section>`);
            khuon(khung, `
                <section>
                    <h2>${t('Phòng khám', 'Rooms')}</h2>
                    <form id="form-tim-phong" class="hai-cot">
                        <input name="tuKhoa" placeholder="${t('Tìm theo tên phòng, tầng (Việt hoặc Anh)', 'Search by room name, floor (Vietnamese or English)')}">
                        <select name="idChuyenKhoa"><option value="">${t('(mọi chuyên khoa)', '(all specialties)')}</option></select>
                        <select name="trangThai"><option value="">${t('(mọi trạng thái)', '(all statuses)')}</option><option value="HOAT_DONG">${t('Đang hoạt động', 'Active')}</option><option value="NGUNG_HOAT_DONG">${t('Ngừng hoạt động', 'Inactive')}</option></select>
                        <button class="phu">${t('Tìm', 'Search')}</button>
                    </form>
                    <div id="ds-phong" class="bang"></div>
                    <p class="ghi-chu">${t('Phòng còn ca sắp tới thì chưa ngừng hoạt động và chưa đổi chuyên khoa được: chuyển các ca đó sang phòng khác ở Schedule Management trước.',
                        'A room with upcoming shifts cannot be deactivated or moved to another specialty yet: move those shifts to another room in Schedule Management first.')}</p>
                    <h3 id="tieu-de-form-phong">${t('Thêm phòng khám', 'Add room')}</h3>
                    <form id="form-phong">
                        <div class="hai-cot">
                            <div><label>${t('Tên phòng', 'Room name')}</label><input name="tenPhong" required maxlength="100"></div>
                            <div><label>${t('Tầng', 'Floor')}</label><input name="tang" maxlength="20"></div>
                            <div><label>${t('Tên phòng (English)', 'Room name (English)')}</label><input name="tenPhongEn" maxlength="100" placeholder="Room 203"></div>
                            <div><label>${t('Tầng (English)', 'Floor (English)')}</label><input name="tangEn" maxlength="20" placeholder="Floor 2"></div>
                            <div><label>${t('Chuyên khoa', 'Specialty')}</label><select name="idChuyenKhoa" required></select></div>
                        </div>
                        <div class="hai-cot"><button>${t('Lưu', 'Save')}</button><button type="button" id="nut-huy-sua-phong" class="phu an">${t('Hủy sửa', 'Cancel editing')}</button></div>
                    </form>
                    <p id="phong-trang-thai" class="ghi-chu"></p>
                </section>
                <section>
                    <h2>${t('Thuốc', 'Medicines')}</h2>
                    <form id="form-tim-thuoc" class="hai-cot">
                        <input name="tuKhoa" placeholder="${t('Tìm theo tên thuốc hoặc hoạt chất', 'Search by medicine name or active ingredient')}">
                        <select name="daXacMinh"><option value="">${t('(xác minh: tất cả)', '(verification: all)')}</option><option value="false">${t('Chưa xác minh', 'Not verified')}</option><option value="true">${t('Đã xác minh', 'Verified')}</option></select>
                        <select name="trangThai"><option value="">${t('(mọi trạng thái)', '(all statuses)')}</option><option value="DANG_DUNG">${TRANG_THAI_THUOC.DANG_DUNG}</option><option value="NGUNG_DUNG">${TRANG_THAI_THUOC.NGUNG_DUNG}</option></select>
                        <button class="phu">${t('Tìm', 'Search')}</button>
                    </form>
                    <div id="ds-thuoc" class="bang"></div>
                    <p id="trang-thuoc" class="ghi-chu"></p>
                    <button id="nut-thuoc-truoc" class="phu nho">${t('‹ Trang trước', '‹ Previous page')}</button>
                    <button id="nut-thuoc-sau" class="phu nho">${t('Trang sau ›', 'Next page ›')}</button>
                    <h3 id="tieu-de-form-thuoc">${t('Thêm thuốc', 'Add medicine')}</h3>
                    <form id="form-thuoc">
                        <div class="hai-cot">
                            <div><label>${t('Tên thuốc', 'Medicine name')}</label><input name="tenThuoc" required maxlength="255"></div>
                            <div><label>${t('Hoạt chất (tên chung quốc tế; phối hợp nối bằng " + ")', 'Active ingredient (generic name; join combinations with " + ")')}</label><input name="hoatChat" required maxlength="255"></div>
                            <div><label>${t('Hàm lượng (500mg, 250mg/5ml...)', 'Strength (500mg, 250mg/5ml...)')}</label><input name="hamLuong" required maxlength="100"></div>
                        </div>
                        <div class="hai-cot">
                            <div><label>${t('Dạng bào chế (viên nén, siro...)', 'Dosage form (Vietnamese)')}</label><input name="dangBaoChe" maxlength="50"></div>
                            <div><label>${t('Dạng bào chế (English: tablet, syrup...)', 'Dosage form (English: tablet, syrup...)')}</label><input name="dangBaoCheEn" maxlength="50"></div>
                            <div><label>${t('Đường dùng', 'Route')}</label><select name="duongDung"><option value="">${t('— chưa chọn —', '— not set —')}</option>${Object.keys(DUONG_DUNG).map((ma) => `<option value="${ma}">${DUONG_DUNG[ma]}</option>`).join('')}</select></div>
                        </div>
                        <div class="hai-cot">
                            <div><label>${t('Đơn vị (viên, ml, gói...)', 'Unit (Vietnamese)')}</label><input name="donVi" maxlength="30"></div>
                            <div><label>${t('Đơn vị (English: tablet, ml, sachet...)', 'Unit (English: tablet, ml, sachet...)')}</label><input name="donViEn" maxlength="30"></div>
                        </div>
                        <label>${t('Mô tả', 'Description')}</label><textarea name="moTa" rows="2" maxlength="2000"></textarea>
                        <label>${t('Mô tả (English)', 'Description (English)')}</label><textarea name="moTaEn" rows="2" maxlength="2000"></textarea>
                        <div class="hai-cot"><button>${t('Lưu', 'Save')}</button><button type="button" id="nut-huy-sua-thuoc" class="phu an">${t('Hủy sửa', 'Cancel editing')}</button></div>
                    </form>
                    <p id="thuoc-trang-thai" class="ghi-chu"></p>
                </section>`);

            phongDangSua = null;
            thuocDangSua = null;
            trangThuoc = 0;
            $('form-tim-phong').addEventListener('submit', (e) => { e.preventDefault(); taiPhong(); });
            $('nut-huy-sua-phong').addEventListener('click', () => datFormPhong(null));
            khiGui('form-phong', async (v) => {
                const than = {
                    tenPhong: v.tenPhong.trim(), tenPhongEn: v.tenPhongEn.trim() || null, tang: v.tang.trim() || null,
                    tangEn: v.tangEn.trim() || null, idChuyenKhoa: Number(v.idChuyenKhoa),
                };
                const { status, json } = phongDangSua
                    ? await goi(`${PHONG}/${phongDangSua.id}`, than, 'PUT')
                    : await goi(PHONG, than);
                if (status !== 200 && status !== 201) {
                    bao('phong-trang-thai', thongDiepLoi(json, t('Không lưu được phòng khám.', 'Could not save the room.')), 'loi');
                    return;
                }
                bao('phong-trang-thai', json.thongDiep || t('Đã lưu phòng khám.', 'Room saved.'), 'tot');
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
                const than = {
                    tenThuoc: v.tenThuoc.trim(), hoatChat: v.hoatChat.trim(), hamLuong: v.hamLuong.trim(),
                    dangBaoChe: v.dangBaoChe.trim() || null, dangBaoCheEn: v.dangBaoCheEn.trim() || null, duongDung: v.duongDung || null,
                    donVi: v.donVi.trim() || null, donViEn: v.donViEn.trim() || null,
                    moTa: v.moTa.trim() || null, moTaEn: v.moTaEn.trim() || null,
                };
                const { status, json } = thuocDangSua
                    ? await goi(`${THUOC}/${thuocDangSua.id}`, than, 'PUT')
                    : await goi(THUOC, than);
                if (status !== 200 && status !== 201) {
                    bao('thuoc-trang-thai', thongDiepLoi(json, t('Không lưu được thuốc.', 'Could not save the medicine.')), 'loi');
                    return;
                }
                bao('thuoc-trang-thai', json.thongDiep || t('Đã lưu thuốc.', 'Medicine saved.'), 'tot');
                datFormThuoc(null);
                await taiThuoc(true);
            });

            dangSua = null;
            $('form-tim-chuyen-khoa').addEventListener('submit', (e) => { e.preventDefault(); taiChuyenKhoa(); });
            $('nut-huy-sua-chuyen-khoa').addEventListener('click', () => datForm(null));
            khiGui('form-chuyen-khoa', async (v) => {
                const than = {
                    tenChuyenKhoa: v.tenChuyenKhoa.trim(), tenChuyenKhoaEn: v.tenChuyenKhoaEn.trim() || null,
                    moTa: v.moTa.trim() || null, moTaEn: v.moTaEn.trim() || null,
                    trieuChung: v.trieuChung.trim() || null, trieuChungEn: v.trieuChungEn.trim() || null,
                };
                const { status, json } = dangSua
                    ? await goi(`${CHUYEN_KHOA}/${dangSua.id}`, than, 'PUT')
                    : await goi(CHUYEN_KHOA, than);
                if (status !== 200 && status !== 201) {
                    bao('chuyen-khoa-trang-thai', thongDiepLoi(json, t('Không lưu được chuyên khoa.', 'Could not save the specialty.')), 'loi');
                    return;
                }
                bao('chuyen-khoa-trang-thai', json.thongDiep || t('Đã lưu chuyên khoa.', 'Specialty saved.'), 'tot');
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
