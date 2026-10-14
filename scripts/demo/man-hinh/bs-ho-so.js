// Màn hình UI: Doctor Profile (Doctor) — bác sĩ xem hồ sơ của mình, tự sửa phần được phép (có hiệu lực ngay)
// Đường dẫn:  /doctor/profile
// Vai trò:    BAC_SI
// API:
//   GET    /api/catalog/bac-si/toi/ho-so        mọi mục của hồ sơ (cả 2 ngôn ngữ) + phienBanHoSo + anh[]. Cần token dù
//                                                 GET /api/catalog/bac-si/** là công khai
//   PUT    /api/catalog/bac-si/toi/ho-so        { phienBan, gioiThieuNgan, gioiThieuNganEn, tieuSu, tieuSuEn, ngonNguKham[] (mã ISO 639),
//                                                 giaiThuong[], giaiThuongEn[], congTrinhNghienCuu[], congTrinhNghienCuuEn[] }: ghi đè các
//                                                 mục này, mục khác giữ nguyên. phienBan cũ -> 409 DU_LIEU_DA_THAY_DOI (quản trị viên vừa lưu)
//   POST   /api/catalog/bac-si/toi/anh          multipart: tệp "anh" (JPEG / PNG / WebP, tối đa 4 MB), "chuThich", "chuThichEn" -> 201,
//                                                 luôn là ảnh công việc
//   PUT    /api/catalog/bac-si/toi/anh/{idAnh}  { chuThich, chuThichEn }   (ảnh chứng chỉ: 403)
//   DELETE /api/catalog/bac-si/toi/anh/{idAnh}                              (ảnh chứng chỉ: 403)
// Học vị, chức vụ, giấy phép, đào tạo, công tác, lĩnh vực khám chữa, năm bắt đầu hành nghề, ảnh chứng chỉ: quản trị viên sửa ở
// /admin/doctors. Đổi số điện thoại, ảnh đại diện, mật khẩu: /account.
(() => {
    const TOI = '/api/catalog/bac-si/toi';
    const CAC_DANH_SACH = ['giaiThuong', 'congTrinhNghienCuu'];
    const CAC_O_CHU = ['gioiThieuNgan', 'gioiThieuNganEn', 'tieuSu', 'tieuSuEn'];
    let hoSo = null;   // HoSoBacSiQuanTriResponse đang hiện

    const dsY = (cacY) => (cacY && cacY.length ? cacY.join('; ') : null);

    async function taiHoSo(imLang = true) {
        const { status, json } = await lay(`${TOI}/ho-so`, imLang);
        if (!$('bs-ho-so-chi-doc')) return false;
        if (status !== 200) {
            $('bs-ho-so-chi-doc').replaceChildren(the('p', thongDiepLoi(json, t('Không tải được hồ sơ.', 'Could not load the profile.')), 'ghi-chu loi'));
            return false;
        }
        hienHoSo(json.duLieu);
        return true;
    }

    function hienHoSo(bs) {
        hoSo = bs;
        $('bs-ho-so-chi-doc').replaceChildren(anh(bs.anhDaiDien, t('Ảnh đại diện', 'Profile photo'), 'anh-dai-dien lon'), thongTin([
            [t('Họ tên', 'Full name'), bs.hoTen], ['Email', bs.email], [t('Số điện thoại', 'Phone number'), bs.soDienThoai],
            [t('Chuyên khoa', 'Specialty'), bs.tenChuyenKhoa], [t('Học vị', 'Academic degree'), bs.hocVi],
            [t('Chức vụ', 'Position'), theoNgonNgu(bs.chucVu, bs.chucVuEn)],
            [t('Số giấy phép hành nghề', 'Practice licence number'), bs.soGiayPhep],
            [t('Ngày cấp', 'Issue date'), bs.ngayCapGiayPhep ? ngayVn(bs.ngayCapGiayPhep) : null],
            [t('Nơi cấp', 'Issuer'), theoNgonNgu(bs.noiCapGiayPhep, bs.noiCapGiayPhepEn)],
            [t('Phạm vi hành nghề', 'Scope of practice'), theoNgonNgu(bs.phamViHanhNghe, bs.phamViHanhNgheEn)],
            [t('Năm bắt đầu hành nghề', 'Year started practising'), bs.namBatDauHanhNghe],
            [t('Số năm kinh nghiệm', 'Years of experience'), bs.soNamKinhNghiem],
            [t('Quá trình đào tạo', 'Education'), dsY(bs.quaTrinhDaoTao)], [t('Quá trình công tác', 'Work history'), dsY(bs.quaTrinhCongTac)],
            [t('Lĩnh vực khám chữa', 'Areas of treatment'), dsY(bs.linhVucKhamChua)],
            [t('Trạng thái', 'Status'), bs.trangThai],
        ]), ghiChu(t('Các mục trên do quản trị viên cập nhật (Quản lý bác sĩ).', 'The fields above are updated by an administrator (Doctor Management).')),
        lienKet(t('Đổi số điện thoại, ảnh đại diện, mật khẩu', 'Change phone number, profile photo, password'), '/account'));

        const f = $('form-bs-ho-so');
        for (const ten of CAC_O_CHU) f.elements[ten].value = bs[ten] ?? '';
        f.elements.ngonNguKham.value = (bs.ngonNguKham || []).join(', ');
        for (const ten of CAC_DANH_SACH) {
            f.elements[ten].value = (bs[ten] || []).join('\n');
            f.elements[ten + 'En'].value = (bs[ten + 'En'] || []).join('\n');
        }
        hienTenNgonNgu();
        hienAnh(bs.anh || []);
    }

    function hienTenNgonNgu() {
        const cacMa = $('form-bs-ho-so').elements.ngonNguKham.value.split(',').map((ma) => ma.trim()).filter(Boolean);
        $('bs-ten-ngon-ngu').textContent = cacMa.length ? dsNgonNgu(cacMa) : '';
    }

    function hienAnh(ds) {
        if (!ds.length) {
            $('bs-anh').replaceChildren(ghiChu(t('Chưa có ảnh giới thiệu nào.', 'No profile photos yet.')));
            return;
        }
        const luoi = the('div', null, 'luoi-anh');
        for (const a of ds) {
            const hinh = document.createElement('figure');
            const chuThich = theoNgonNgu(a.chuThich, a.chuThichEn);
            const congViec = a.loai === 'ANH_CONG_VIEC';
            hinh.append(anh(a.url, chuThich), the('figcaption', `${congViec ? t('Ảnh công việc', 'Work photo') : t('Chứng chỉ', 'Certificate')}${chuThich ? ': ' + chuThich : ''}`));
            if (congViec) {
                hinh.append(nut(t('Sửa chú thích', 'Edit caption'), () => suaAnh(a)), nut(t('Xoá ảnh', 'Delete photo'), () => xoaAnh(a), 'nguy-hiem nho'));
            } else {
                hinh.append(ghiChu(t('Quản trị viên quản lý', 'Managed by an administrator')));
            }
            luoi.append(hinh);
        }
        $('bs-anh').replaceChildren(luoi);
    }

    async function suaAnh(a) {
        const chuThich = prompt(t('Chú thích mới (tối đa 200 ký tự, để trống = bỏ chú thích):',
            'New caption (up to 200 characters, leave empty = remove the caption):'), a.chuThich || '');
        if (chuThich === null) return;
        const chuThichEn = prompt(t('Chú thích tiếng Anh (để trống = người xem tiếng Anh thấy chú thích tiếng Việt):',
            'English caption (leave empty = English viewers see the Vietnamese caption):'), a.chuThichEn || '');
        if (chuThichEn === null) return;
        const { status, json } = await goi(`${TOI}/anh/${a.id}`, { chuThich: chuThich.trim() || null, chuThichEn: chuThichEn.trim() || null }, 'PUT');
        if (status === 200) await taiHoSo();
        else bao('bs-anh-trang-thai', thongDiepLoi(json, t('Không sửa được ảnh.', 'Could not update the photo.')), 'loi');
    }

    async function xoaAnh(a) {
        if (!confirm(t('Xoá ảnh này?', 'Delete this photo?'))) return;
        const { status, json } = await goi(`${TOI}/anh/${a.id}`, undefined, 'DELETE');
        if (status === 200) await taiHoSo();
        else bao('bs-anh-trang-thai', thongDiepLoi(json, t('Không xoá được ảnh.', 'Could not delete the photo.')), 'loi');
    }

    dangKy({
        duongDan: '/doctor/profile',
        vaiTro: 'BAC_SI',
        tieuDe: cap('Hồ sơ bác sĩ', 'Doctor Profile'),
        async ve(khung) {
            hoSo = null;
            khuon(khung, `
                <section>
                    <h2>${t('Thông tin do quản trị viên quản lý', 'Details managed by an administrator')}</h2>
                    <div id="bs-ho-so-chi-doc">${t('Đang tải…', 'Loading…')}</div>
                </section>
                <section>
                    <h2>${t('Phần tôi tự sửa (hiện ngay trên trang công khai)', 'What I can edit (shown on the public page at once)')}</h2>
                    <form id="form-bs-ho-so">
                        <label>${t('Giới thiệu ngắn (hiện trên thẻ bác sĩ)', 'Short introduction (shown on the doctor card)')}</label><input name="gioiThieuNgan" maxlength="300">
                        <label>${t('Tiểu sử', 'Biography')}</label><textarea name="tieuSu" rows="4" maxlength="5000"></textarea>
                        <label>${t('Ngôn ngữ khám (mã ISO, cách nhau dấu phẩy, vd: vi, en, fr)', 'Consultation languages (ISO codes, comma separated, e.g. vi, en, fr)')}</label>
                        <input name="ngonNguKham" placeholder="vi, en"><p id="bs-ten-ngon-ngu" class="ghi-chu"></p>
                        <label>${t('Giải thưởng (mỗi dòng 1 ý, tối đa 20 ý)', 'Awards (one item per line, up to 20 items)')}</label><textarea name="giaiThuong" rows="3"></textarea>
                        <label>${t('Công trình nghiên cứu (mỗi dòng 1 ý)', 'Research (one item per line)')}</label><textarea name="congTrinhNghienCuu" rows="3"></textarea>
                        <h4>${t('English (bỏ trống mục nào thì người xem tiếng Anh thấy bản tiếng Việt của mục đó)',
                            'English (a field left empty shows its Vietnamese version to English viewers)')}</h4>
                        <label>${t('Giới thiệu ngắn (English)', 'Short introduction (English)')}</label><input name="gioiThieuNganEn" maxlength="300">
                        <label>${t('Tiểu sử (English)', 'Biography (English)')}</label><textarea name="tieuSuEn" rows="4" maxlength="5000"></textarea>
                        <label>${t('Giải thưởng (English, mỗi dòng 1 ý)', 'Awards (English, one item per line)')}</label><textarea name="giaiThuongEn" rows="3"></textarea>
                        <label>${t('Công trình nghiên cứu (English, mỗi dòng 1 ý)', 'Research (English, one item per line)')}</label><textarea name="congTrinhNghienCuuEn" rows="3"></textarea>
                        <button>${t('Lưu (ghi đè các mục trên, mục bỏ trống bị xoá)', 'Save (overwrites the fields above, empty fields are cleared)')}</button>
                    </form>
                    <p id="bs-ho-so-trang-thai" class="ghi-chu"></p>
                </section>
                <section>
                    <h2>${t('Ảnh giới thiệu', 'Profile photos')}</h2>
                    <p class="ghi-chu">${t('Tôi thêm / sửa / xoá được ảnh công việc; ảnh chứng chỉ do quản trị viên quản lý.',
                        'I can add, edit and delete work photos; certificate photos are managed by an administrator.')}</p>
                    <div id="bs-anh"></div>
                    <form id="form-bs-anh" class="hai-cot">
                        <div><label>${t('Tệp ảnh (JPEG / PNG / WebP, tối đa 4 MB)', 'Image file (JPEG / PNG / WebP, up to 4 MB)')}</label><input name="anh" type="file" accept="image/jpeg,image/png,image/webp" required></div>
                        <div><label>${t('Chú thích', 'Caption')}</label><input name="chuThich" maxlength="200"></div>
                        <div><label>${t('Chú thích (English)', 'Caption (English)')}</label><input name="chuThichEn" maxlength="200"></div>
                        <button>${t('Thêm ảnh công việc', 'Add work photo')}</button>
                    </form>
                    <p id="bs-anh-trang-thai" class="ghi-chu"></p>
                </section>`);

            $('form-bs-ho-so').elements.ngonNguKham.addEventListener('input', hienTenNgonNgu);
            khiGui('form-bs-ho-so', async (v) => {
                if (!hoSo) return;
                const than = { phienBan: hoSo.phienBanHoSo };
                for (const ten of CAC_O_CHU) than[ten] = v[ten].trim() || null;
                than.ngonNguKham = v.ngonNguKham.split(',').map((ma) => ma.trim()).filter(Boolean);
                for (const ten of [...CAC_DANH_SACH, ...CAC_DANH_SACH.map((muc) => muc + 'En')]) than[ten] = v[ten].split('\n').map((y) => y.trim()).filter(Boolean);
                const { status, json } = await goi(`${TOI}/ho-so`, than, 'PUT');
                if (status !== 200) {
                    const loi = thongDiepLoi(json, t('Không lưu được hồ sơ.', 'Could not save the profile.'));
                    // Quản trị viên vừa lưu hồ sơ: tải lại bản mới, nội dung đang nhập trên form bị thay
                    if (json?.maLoi === 'DU_LIEU_DA_THAY_DOI') {
                        await taiHoSo();
                        bao('bs-ho-so-trang-thai', loi + t(' Đã tải lại hồ sơ mới nhất, hãy nhập lại thay đổi.',
                            ' The latest profile has been reloaded; enter your changes again.'), 'loi');
                        return;
                    }
                    bao('bs-ho-so-trang-thai', loi, 'loi');
                    return;
                }
                hienHoSo(json.duLieu);
                bao('bs-ho-so-trang-thai', json.thongDiep || t('Đã cập nhật hồ sơ.', 'Profile updated.'), 'tot');
            });
            khiGui('form-bs-anh', async (v, form) => {
                const duLieu = new FormData();
                duLieu.append('anh', form.elements.anh.files[0]);
                if (v.chuThich.trim()) duLieu.append('chuThich', v.chuThich.trim());
                if (v.chuThichEn.trim()) duLieu.append('chuThichEn', v.chuThichEn.trim());
                const { status, json } = await goi(`${TOI}/anh`, duLieu);
                if (status !== 201 && status !== 200) {
                    bao('bs-anh-trang-thai', thongDiepLoi(json, t('Không thêm được ảnh.', 'Could not add the photo.')), 'loi');
                    return;
                }
                form.reset();
                bao('bs-anh-trang-thai', t('Đã thêm ảnh công việc.', 'Work photo added.'), 'tot');
                await taiHoSo();
            });
            await taiHoSo(false);
        },
    });
})();
