// Màn hình ngoài danh sách UI: Sổ khám bệnh (EXAM-03) — trang mở từ link / mã QR của sổ khám, khác link phiếu khám
// Đường dẫn:  /so-kham/<maSoKham>   (= app.so-kham.frontend-url + app.so-kham.duong-dan + mã; chính là nội dung mã QR)
// Vai trò:    công khai (ai có link đều xem được phần tóm tắt)
// API:
//   GET  /api/booking/so-kham/{maSoKham}         tóm tắt đã che: hoTenChe ("N. V. An"), namSinh, ngayKham, gioKham, bacSi,
//        tenChuyenKhoa, phongKham; không có kết quả khám. Mã sai / không có -> 404
//   POST /api/booking/so-kham/{maSoKham}/xem     { bonSoCuoi } = 4 số cuối SĐT liên hệ lúc đặt lịch (lượt khám có người giám hộ:
//        SĐT người giám hộ) -> { tomTat, benhNhan { hoTen, ngaySinh, gioiTinh, cccdChe }, hoTenNguoiGiamHo, ketQua } (ketQua như
//        kết quả khám ở lịch sử khám). Sai -> 403 SO_DIEN_THOAI_KHONG_KHOP; sai 5 lần -> 429 GUI_LAI_QUA_NHANH, khoá 15 phút kể cả
//        khi nhập đúng
//   GET  /api/booking/so-kham/{maSoKham}/qr      ảnh PNG mã QR của link (?kichThuoc=100..1000, ?taiVe=true để tải về)
(() => {
    dangKy({
        duongDan: /^\/so-kham\/([^/]+)$/,
        tieuDe: cap('Sổ khám bệnh', 'Visit Record'),
        async ve(khung, { khop }) {
            const goc = '/api/booking/so-kham/' + encodeURIComponent(khop[1]);
            const trangThai = the('p', t('Đang tải sổ khám bệnh…', 'Loading the visit record…'));
            const tomTat = the('div');
            const ketQua = the('div');
            khung.append(trangThai, tomTat, ketQua);

            const { status, json } = await lay(goc, true);
            if (status !== 200) {
                bao(trangThai, thongDiepLoi(json, t('Không mở được sổ khám bệnh.', 'Could not open the visit record.')), 'loi');
                return;
            }
            const tt = json.duLieu;
            trangThai.textContent = t('Phần tóm tắt: ai có link đều xem được. Kết quả khám cần 4 số cuối số điện thoại liên hệ lúc đặt lịch.',
                'Summary: anyone with the link can see it. The visit result needs the last 4 digits of the contact phone used when booking.');
            const cotQr = the('div');
            const duongDanQr = `${API_URL}${goc}/qr`;
            cotQr.append(anh(duongDanQr, t('Mã QR của sổ khám bệnh', 'QR code of the visit record')), the('br'),
                lienKet(t('Tải mã QR', 'Download QR code'), duongDanQr + '?taiVe=true'));
            const hop = the('div', null, 'phieu');
            hop.append(thongTin([
                [t('Bệnh nhân', 'Patient'), tt.hoTenChe], [t('Năm sinh', 'Year of birth'), tt.namSinh],
                [t('Ngày khám', 'Visit date'), `${thuCuaNgay(tt.ngayKham)} ${ngayVn(tt.ngayKham)} ${tt.gioKham ? tt.gioKham.slice(0, 5) : ''}`.trim()],
                [t('Bác sĩ', 'Doctor'), tenBacSi(tt.bacSi)], [t('Chuyên khoa', 'Specialty'), tt.tenChuyenKhoa],
                [t('Phòng khám', 'Room'), tenPhong(tt.phongKham)],
            ]), cotQr);
            tomTat.append(hop);

            const form = the('form', null, 'hai-cot');
            khuon(form, `
                <div><label>${t('4 số cuối số điện thoại liên hệ (người giám hộ, nếu bệnh nhân là trẻ em)', 'Last 4 digits of the contact phone (the guardian\'s, for a child)')}</label>
                    <input name="bonSoCuoi" required pattern="\\d{4}" maxlength="4" inputmode="numeric" autocomplete="off"></div>
                <button>${t('Xem kết quả khám', 'View the visit result')}</button>`);
            const baoForm = ghiChu('');
            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                const { status: st, json: kq } = await goi(`${goc}/xem`, giaTriForm(form));
                if (st !== 200) {
                    // 403: sai số; 429: sai quá nhiều lần, thongDiep nói còn bao lâu
                    bao(baoForm, thongDiepLoi(kq, t('Không xem được kết quả khám.', 'Could not view the visit result.')), 'loi');
                    return;
                }
                const sk = kq.duLieu, bn = sk.benhNhan;
                form.remove();
                baoForm.remove();
                ketQua.replaceChildren(the('h2', t('Kết quả khám', 'Visit result')), thongTin([
                    [t('Họ tên', 'Full name'), bn.hoTen], [t('Ngày sinh', 'Date of birth'), bn.ngaySinh ? ngayVn(bn.ngaySinh) : null],
                    [t('Giới tính', 'Gender'), bn.gioiTinh ? GIOI_TINH[bn.gioiTinh] : null], [t('CCCD', 'National ID'), bn.cccdChe],
                    [t('Người giám hộ', 'Guardian'), sk.hoTenNguoiGiamHo],
                ]), ketQuaKham({ ...sk.ketQua, maSoKham: null }));   // đang ở chính sổ khám: không vẽ lại link của nó
            });
            ketQua.append(form, baoForm);
        },
    });
})();
