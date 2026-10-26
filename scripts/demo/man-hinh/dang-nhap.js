// Màn hình UI: Login (Authentication)
// Đường dẫn:  /login   (?tiep=<đường dẫn> : đăng nhập xong quay lại màn hình đó)
// Vai trò:    công khai
// API:
//   POST /api/auth/login                 { email, matKhau } -> accessToken + taiKhoan, cookie refresh token
//   POST /api/auth/first-password        { email, matKhauHienTai, matKhauMoi } : khi login trả 403 PHAI_DOI_MAT_KHAU (tài khoản
//                                        bác sĩ mới còn mang mật khẩu mặc định) -> đặt mật khẩu của mình rồi đăng nhập lại
//   POST /api/auth/google                { idToken } (Google Identity Services)
//   POST /api/auth/resend-verification   { email } : gửi lại liên kết kích hoạt (ngoài UI)
// Sau khi đăng nhập: BENH_NHAN -> /appointment, BAC_SI -> /doctor/dashboard, QUAN_TRI_VIEN -> /admin/dashboard.
(() => {
    let daKhoiTaoGoogle = false;

    // Đăng nhập Google: nhận ID token rồi gửi cho API
    async function veNutGoogle() {
        if (!GOOGLE_CLIENT_ID) {
            $('google-chua-cau-hinh').classList.remove('an');
            return;
        }
        try {
            await napThuVien('https://accounts.google.com/gsi/client');
        } catch (e) {
            bao('dang-nhap-trang-thai', String(e.message || e), 'loi');
            return;
        }
        if (!daKhoiTaoGoogle) {
            google.accounts.id.initialize({
                client_id: GOOGLE_CLIENT_ID,
                callback: async (phanHoi) => {
                    const { status, json } = await goi('/api/auth/google', { idToken: phanHoi.credential });
                    if (status === 200) await dangNhapXong(json);
                    else bao('dang-nhap-trang-thai', thongDiepLoi(json, t('Đăng nhập Google thất bại.', 'Google sign-in failed.')), 'loi');
                },
            });
            daKhoiTaoGoogle = true;
        }
        if ($('nut-google')) google.accounts.id.renderButton($('nut-google'), { theme: 'outline', size: 'large', text: 'signin_with' });
    }

    dangKy({
        duongDan: '/login',
        tieuDe: cap('Đăng nhập', 'Login'),
        ve(khung) {
            khuon(khung, `
                <section>
                    <h2>${t('Đăng nhập', 'Sign in')}</h2>
                    <p id="da-dang-nhap" class="ghi-chu"></p>
                    <form id="form-dang-nhap">
                        <input name="email" type="email" placeholder="${t('Email (vd benhnhan01@ / bacsi01@ / admin@eclinic.local)', 'Email (e.g. benhnhan01@ / bacsi01@ / admin@eclinic.local)')}" required autocomplete="username">
                        <input name="matKhau" type="password" placeholder="${t('Mật khẩu (tài khoản mẫu: Demo@123)', 'Password (sample accounts: Demo@123)')}" required autocomplete="current-password">
                        <button>${t('Đăng nhập', 'Sign in')}</button>
                    </form>
                    <form id="form-mat-khau-lan-dau" class="an">
                        <p class="ghi-chu">${t('Tài khoản đang dùng mật khẩu mặc định của phòng khám. Hãy đặt mật khẩu của riêng bạn để tiếp tục.',
                            'This account still uses the default password of the clinic. Set your own password to continue.')}</p>
                        <input name="matKhauMoi" type="password" placeholder="${t('Mật khẩu mới (tối thiểu 6 ký tự, khác mật khẩu mặc định)', 'New password (at least 6 characters, different from the default password)')}" required minlength="6" autocomplete="new-password">
                        <input name="nhapLai" type="password" placeholder="${t('Nhập lại mật khẩu mới', 'Repeat the new password')}" required autocomplete="new-password">
                        <button>${t('Đặt mật khẩu và đăng nhập', 'Set password and sign in')}</button>
                    </form>
                    <div id="nut-google"></div>
                    <p id="google-chua-cau-hinh" class="ghi-chu an">${t('Chưa có GOOGLE_CLIENT_ID nên không có nút Google.', 'GOOGLE_CLIENT_ID is not set, so there is no Google button.')}</p>
                    <p id="dang-nhap-trang-thai" class="ghi-chu"></p>
                    <p><a href="/forgot-password">Forgot password?</a> · <a href="/register">Register</a> ·
                        <a href="/appointment">${t('Đặt lịch không cần đăng nhập', 'Book an appointment without signing in')}</a></p>
                </section>
                <section>
                    <h2>${t('Gửi lại liên kết kích hoạt (ngoài danh sách UI)', 'Resend the activation link (not in the UI list)')}</h2>
                    <form id="form-gui-lai">
                        <input name="email" type="email" placeholder="${t('Email chưa kích hoạt', 'Email not yet activated')}" required>
                        <button class="phu">${t('Gửi lại liên kết kích hoạt', 'Resend the activation link')}</button>
                    </form>
                    <p id="gui-lai-trang-thai" class="ghi-chu"></p>
                </section>`);
            if (vaiTro()) {
                $('da-dang-nhap').textContent = t(`Đang đăng nhập: ${taiKhoan.hoTen} <${taiKhoan.email}> (${VAI_TRO[taiKhoan.vaiTro]}). Đăng nhập lại để đổi tài khoản.`,
                    `Signed in: ${taiKhoan.hoTen} <${taiKhoan.email}> (${VAI_TRO[taiKhoan.vaiTro]}). Sign in again to switch accounts.`);
            }
            let lanDau = null;   // { email, matKhau } vừa nhập, khi API báo PHAI_DOI_MAT_KHAU
            khiGui('form-dang-nhap', async (v) => {
                const { status, json } = await goi('/api/auth/login', v);
                lanDau = null;
                $('form-mat-khau-lan-dau').classList.add('an');
                if (status === 200) {
                    await dangNhapXong(json);
                } else if (json && json.maLoi === 'PHAI_DOI_MAT_KHAU') {
                    // Chưa có phiên nào được cấp: giữ email + mật khẩu mặc định vừa nhập để gọi first-password
                    lanDau = { email: v.email, matKhau: v.matKhau };
                    $('form-mat-khau-lan-dau').classList.remove('an');
                    bao('dang-nhap-trang-thai', thongDiepLoi(json, t('Hãy đặt mật khẩu mới.', 'Please set a new password.')), 'loi');
                } else {
                    bao('dang-nhap-trang-thai', thongDiepLoi(json, t('Đăng nhập thất bại.', 'Sign-in failed.')), 'loi');
                }
            });
            khiGui('form-mat-khau-lan-dau', async (v, form) => {
                if (!lanDau) return;
                if (v.matKhauMoi !== v.nhapLai) {
                    bao('dang-nhap-trang-thai', t('Hai lần nhập mật khẩu mới không giống nhau.', 'The two new passwords do not match.'), 'loi');
                    return;
                }
                const dat = await goi('/api/auth/first-password', { email: lanDau.email, matKhauHienTai: lanDau.matKhau, matKhauMoi: v.matKhauMoi });
                if (dat.status !== 200) {
                    bao('dang-nhap-trang-thai', thongDiepLoi(dat.json, t('Không đặt được mật khẩu.', 'Could not set the password.')), 'loi');
                    return;
                }
                const { status, json } = await goi('/api/auth/login', { email: lanDau.email, matKhau: v.matKhauMoi });
                lanDau = null;
                form.reset();
                form.classList.add('an');
                if (status === 200) await dangNhapXong(json);
                else bao('dang-nhap-trang-thai', t('Đã đặt mật khẩu. Hãy đăng nhập bằng mật khẩu mới.', 'Password set. Please sign in with the new password.'), 'tot');
            });
            khiGui('form-gui-lai', async (v) => {
                const { status, json } = await goi('/api/auth/resend-verification', v);
                bao('gui-lai-trang-thai', status === 200 ? ((json && json.thongDiep) || t('Đã gửi.', 'Sent.')) : thongDiepLoi(json, t('Không gửi được.', 'Could not send.')),
                    status === 200 ? 'tot' : 'loi');
            });
            veNutGoogle();
        },
    });
})();
