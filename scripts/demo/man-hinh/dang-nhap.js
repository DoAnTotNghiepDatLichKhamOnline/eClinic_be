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
                    else bao('dang-nhap-trang-thai', thongDiepLoi(json, 'Đăng nhập Google thất bại.'), 'loi');
                },
            });
            daKhoiTaoGoogle = true;
        }
        if ($('nut-google')) google.accounts.id.renderButton($('nut-google'), { theme: 'outline', size: 'large', text: 'signin_with' });
    }

    dangKy({
        duongDan: '/login',
        tieuDe: 'Login',
        ve(khung) {
            khuon(khung, `
                <section>
                    <h2>Đăng nhập</h2>
                    <p id="da-dang-nhap" class="ghi-chu"></p>
                    <form id="form-dang-nhap">
                        <input name="email" type="email" placeholder="Email (vd benhnhan01@ / bacsi01@ / admin@eclinic.local)" required autocomplete="username">
                        <input name="matKhau" type="password" placeholder="Mật khẩu (tài khoản mẫu: Demo@123)" required autocomplete="current-password">
                        <button>Đăng nhập</button>
                    </form>
                    <form id="form-mat-khau-lan-dau" class="an">
                        <p class="ghi-chu">Tài khoản đang dùng mật khẩu mặc định của phòng khám. Hãy đặt mật khẩu của riêng bạn để tiếp tục.</p>
                        <input name="matKhauMoi" type="password" placeholder="Mật khẩu mới (tối thiểu 6 ký tự, khác mật khẩu mặc định)" required minlength="6" autocomplete="new-password">
                        <input name="nhapLai" type="password" placeholder="Nhập lại mật khẩu mới" required autocomplete="new-password">
                        <button>Đặt mật khẩu và đăng nhập</button>
                    </form>
                    <div id="nut-google"></div>
                    <p id="google-chua-cau-hinh" class="ghi-chu an">Chưa có GOOGLE_CLIENT_ID nên không có nút Google.</p>
                    <p id="dang-nhap-trang-thai" class="ghi-chu"></p>
                    <p><a href="/forgot-password">Forgot password?</a> · <a href="/register">Register</a> ·
                        <a href="/appointment">Đặt lịch không cần đăng nhập</a></p>
                </section>
                <section>
                    <h2>Gửi lại liên kết kích hoạt (ngoài danh sách UI)</h2>
                    <form id="form-gui-lai">
                        <input name="email" type="email" placeholder="Email chưa kích hoạt" required>
                        <button class="phu">Gửi lại liên kết kích hoạt</button>
                    </form>
                    <p id="gui-lai-trang-thai" class="ghi-chu"></p>
                </section>`);
            if (vaiTro()) {
                $('da-dang-nhap').textContent = `Đang đăng nhập: ${taiKhoan.hoTen} <${taiKhoan.email}> (${VAI_TRO[taiKhoan.vaiTro]}). Đăng nhập lại để đổi tài khoản.`;
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
                    bao('dang-nhap-trang-thai', thongDiepLoi(json, 'Hãy đặt mật khẩu mới.'), 'loi');
                } else {
                    bao('dang-nhap-trang-thai', thongDiepLoi(json, 'Đăng nhập thất bại.'), 'loi');
                }
            });
            khiGui('form-mat-khau-lan-dau', async (v, form) => {
                if (!lanDau) return;
                if (v.matKhauMoi !== v.nhapLai) {
                    bao('dang-nhap-trang-thai', 'Hai lần nhập mật khẩu mới không giống nhau.', 'loi');
                    return;
                }
                const dat = await goi('/api/auth/first-password', { email: lanDau.email, matKhauHienTai: lanDau.matKhau, matKhauMoi: v.matKhauMoi });
                if (dat.status !== 200) {
                    bao('dang-nhap-trang-thai', thongDiepLoi(dat.json, 'Không đặt được mật khẩu.'), 'loi');
                    return;
                }
                const { status, json } = await goi('/api/auth/login', { email: lanDau.email, matKhau: v.matKhauMoi });
                lanDau = null;
                form.reset();
                form.classList.add('an');
                if (status === 200) await dangNhapXong(json);
                else bao('dang-nhap-trang-thai', 'Đã đặt mật khẩu. Hãy đăng nhập bằng mật khẩu mới.', 'tot');
            });
            khiGui('form-gui-lai', async (v) => {
                const { status, json } = await goi('/api/auth/resend-verification', v);
                bao('gui-lai-trang-thai', status === 200 ? ((json && json.thongDiep) || 'Đã gửi.') : thongDiepLoi(json, 'Không gửi được.'),
                    status === 200 ? 'tot' : 'loi');
            });
            veNutGoogle();
        },
    });
})();
