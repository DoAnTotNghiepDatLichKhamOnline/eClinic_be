// Màn hình UI: mục "Change password" trong menu tài khoản; các phần còn lại nằm ngoài danh sách UI
// Đường dẫn:  /account
// Vai trò:    mọi tài khoản đã đăng nhập
// API (đều cần access token):
//   GET    /api/users/me                    hồ sơ cá nhân (kèm bacSi hoặc hoSoBenhNhan theo vai trò)
//   PUT    /api/users/me                    { hoTen, soDienThoai } (bác sĩ không đổi được họ tên)
//   POST   /api/users/me/avatar             multipart, phần tệp "anh" (JPEG / PNG / WebP, tối đa 2 MB)
//   DELETE /api/users/me/avatar
//   PUT    /api/users/me/change-password    { matKhauCu, matKhauMoi } : thiết bị khác bị đăng xuất
//   POST   /api/users/me/change-email       { emailMoi, matKhauHienTai } -> email có liên kết /confirm-email-change?token=...
//   GET    /api/users/me/change-email       yêu cầu đổi email đang chờ
//   DELETE /api/users/me/change-email       huỷ yêu cầu đang chờ
//   GET    /api/users/me/sessions           thiết bị đang đăng nhập
//   DELETE /api/users/me/sessions/{id}      đăng xuất 1 thiết bị
//   DELETE /api/users/me/sessions           đăng xuất mọi thiết bị khác
(() => {
    const HO_SO = '/api/users/me';
    const ANH = '/api/users/me/avatar';
    const DOI_EMAIL = '/api/users/me/change-email';
    const PHIEN = '/api/users/me/sessions';

    function hienHoSo(hs) {
        const anhDaiDien = $('anh-dai-dien');
        const cacDong = [
            `${hs.hoTen} <${hs.email}> (${hs.vaiTro}, id=${hs.id})`,
            `Số điện thoại: ${hs.soDienThoai || '(chưa có)'}`,
            `Có mật khẩu: ${hs.coMatKhau ? 'có' : 'không'} · Liên kết Google: ${hs.lienKetGoogle ? 'có' : 'không'}`,
        ];
        if (hs.bacSi) {
            cacDong.push(`Bác sĩ: ${hs.bacSi.hocVi || ''} · ${hs.bacSi.tenChuyenKhoa} · ${hs.bacSi.soNamKinhNghiem ?? '?'} năm kinh nghiệm`);
        }
        if (hs.hoSoBenhNhan) {
            cacDong.push(`Hồ sơ bệnh nhân (chỉ xem): ${hs.hoSoBenhNhan.hoTen}, sinh ${hs.hoSoBenhNhan.ngaySinh}, CCCD ${hs.hoSoBenhNhan.cccd}`);
        }
        $('ho-so').replaceChildren(...cacDong.map((noiDung) => the('li', noiDung)));
        const form = $('form-ho-so');
        form.elements.hoTen.value = hs.hoTen;
        form.elements.soDienThoai.value = hs.soDienThoai || '';
        if (/^https?:\/\//.test(hs.anhDaiDien || '')) {
            anhDaiDien.src = hs.anhDaiDien;
            anhDaiDien.classList.remove('an');
        } else {
            anhDaiDien.removeAttribute('src');
            anhDaiDien.classList.add('an');
        }
        $('anh-trang-thai').textContent = hs.anhDaiDien || 'Chưa có ảnh.';
    }

    async function taiHoSo(imLang = false) {
        const { status, json } = await lay(HO_SO, imLang);
        if (status === 200) hienHoSo(json.duLieu);
    }

    async function taiPhien(imLang = false) {
        const { status, json } = await lay(PHIEN, imLang);
        const danhSach = $('ds-phien');
        if (!danhSach) return;
        danhSach.replaceChildren();
        if (status !== 200) return;
        for (const phien of json.duLieu) {
            const li = the('li', `${phien.thietBi || '(không rõ thiết bị)'} · đăng nhập ${phien.dangNhapLuc} · hoạt động ${phien.hoatDongLuc}`
                + (phien.hienTai ? ' · THIẾT BỊ NÀY ' : ' '));
            if (!phien.hienTai) {
                li.append(nut('Đăng xuất', async () => {
                    const ketQua = await goi(`${PHIEN}/${encodeURIComponent(phien.id)}`, undefined, 'DELETE');
                    if (ketQua.status === 200) await taiPhien(true);
                }, 'phu nho'));
            }
            danhSach.append(li);
        }
    }

    dangKy({
        duongDan: '/account',
        vaiTro: 'DANG_NHAP',
        tieuDe: 'Tài khoản của tôi',
        async ve(khung) {
            khuon(khung, `
                <div class="luoi">
                <section>
                    <h2>Hồ sơ cá nhân</h2>
                    <ul id="ho-so" class="danh-sach-tron"><li>Đang tải…</li></ul>
                    <form id="form-ho-so">
                        <input name="hoTen" placeholder="Họ tên (bác sĩ không đổi được)" required maxlength="150">
                        <input name="soDienThoai" placeholder="Số điện thoại (bỏ trống = giữ số cũ)" pattern="0[0-9]{9}">
                        <button>Cập nhật hồ sơ</button>
                    </form>
                    <p id="ho-so-trang-thai" class="ghi-chu"></p>
                </section>
                <section>
                    <h2>Ảnh đại diện</h2>
                    <img id="anh-dai-dien" class="anh-dai-dien lon an" alt="Ảnh đại diện">
                    <p id="anh-trang-thai" class="ghi-chu">Chưa có ảnh.</p>
                    <form id="form-anh">
                        <input name="anh" type="file" accept="image/jpeg,image/png,image/webp" required>
                        <button>Tải ảnh lên (JPEG / PNG / WebP, tối đa 2 MB)</button>
                    </form>
                    <button id="nut-bo-anh" class="phu">Bỏ ảnh đại diện</button>
                </section>
                <section>
                    <h2>Change password — Đổi mật khẩu</h2>
                    <form id="form-doi-mat-khau">
                        <input name="matKhauCu" type="password" placeholder="Mật khẩu hiện tại" required autocomplete="current-password">
                        <input name="matKhauMoi" type="password" placeholder="Mật khẩu mới (tối thiểu 6 ký tự)" required minlength="6" autocomplete="new-password">
                        <input name="nhapLai" type="password" placeholder="Nhập lại mật khẩu mới" required autocomplete="new-password">
                        <button>Đổi mật khẩu</button>
                    </form>
                    <p id="mat-khau-trang-thai" class="ghi-chu">Các thiết bị khác bị đăng xuất, thiết bị này giữ nguyên.</p>
                </section>
                <section>
                    <h2>Đổi email đăng nhập</h2>
                    <form id="form-doi-email">
                        <input name="emailMoi" type="email" placeholder="Email mới" required>
                        <input name="matKhauHienTai" type="password" placeholder="Mật khẩu hiện tại" required autocomplete="current-password">
                        <button>Gửi liên kết xác nhận tới email mới</button>
                    </form>
                    <button id="nut-xem-doi-email" class="phu">Xem yêu cầu đang chờ</button>
                    <button id="nut-huy-doi-email" class="phu">Huỷ yêu cầu đang chờ</button>
                    <p id="doi-email-trang-thai" class="ghi-chu"></p>
                </section>
                <section>
                    <h2>Thiết bị đăng nhập</h2>
                    <ul id="ds-phien" class="danh-sach-tron"></ul>
                    <button id="nut-dang-xuat-khac" class="phu">Đăng xuất mọi thiết bị khác</button>
                </section>
                </div>`);

            khiGui('form-ho-so', async (v) => {
                // Bỏ trống số điện thoại = giữ số cũ (gửi chuỗi rỗng sẽ bị 400 vì sai định dạng)
                const { status, json } = await goi(HO_SO, { hoTen: v.hoTen, soDienThoai: v.soDienThoai || null }, 'PUT');
                if (status === 200) {
                    hienHoSo(json.duLieu);
                    bao('ho-so-trang-thai', 'Đã cập nhật hồ sơ.', 'tot');
                } else {
                    bao('ho-so-trang-thai', thongDiepLoi(json, 'Không cập nhật được.'), 'loi');
                }
            });
            khiGui('form-anh', async (v, form) => {
                const duLieu = new FormData();
                duLieu.append('anh', form.elements.anh.files[0]);
                const { status, json } = await goi(ANH, duLieu);
                if (status === 200) {
                    hienHoSo(json.duLieu);
                    form.reset();
                } else {
                    bao('anh-trang-thai', thongDiepLoi(json, 'Không tải ảnh lên được.'), 'loi');
                }
            });
            $('nut-bo-anh').addEventListener('click', async () => {
                const { status, json } = await goi(ANH, undefined, 'DELETE');
                if (status === 200) hienHoSo(json.duLieu);
            });
            khiGui('form-doi-mat-khau', async (v, form) => {
                if (v.matKhauMoi !== v.nhapLai) {
                    bao('mat-khau-trang-thai', 'Hai lần nhập mật khẩu mới không khớp.', 'loi');
                    return;
                }
                const { status, json } = await goi('/api/users/me/change-password', { matKhauCu: v.matKhauCu, matKhauMoi: v.matKhauMoi }, 'PUT');
                if (status === 200) {
                    form.reset();
                    bao('mat-khau-trang-thai', 'Đã đổi mật khẩu. Các thiết bị khác đã bị đăng xuất.', 'tot');
                } else {
                    bao('mat-khau-trang-thai', thongDiepLoi(json, 'Không đổi được mật khẩu.'), 'loi');
                }
            });
            khiGui('form-doi-email', async (v, form) => {
                const { status, json } = await goi(DOI_EMAIL, v);
                if (status === 200) form.reset();
                bao('doi-email-trang-thai', status === 200 ? ((json && json.thongDiep) || 'Đã gửi liên kết xác nhận.') : thongDiepLoi(json, 'Không gửi được.'),
                    status === 200 ? 'tot' : 'loi');
            });
            // Kết quả của 2 nút này xem ở ô "Kết quả gọi API gần nhất"
            $('nut-xem-doi-email').addEventListener('click', () => lay(DOI_EMAIL));
            $('nut-huy-doi-email').addEventListener('click', () => goi(DOI_EMAIL, undefined, 'DELETE'));
            $('nut-dang-xuat-khac').addEventListener('click', async () => {
                const { status } = await goi(PHIEN, undefined, 'DELETE');
                if (status === 200) await taiPhien(true);
            });

            await taiHoSo(true);
            await taiPhien(true);
        },
    });
})();
