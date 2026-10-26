// Màn hình UI: Register (Authentication)
// Đường dẫn:  /register
// Vai trò:    công khai
// API:
//   POST /api/auth/register   { hoTen, email, matKhau, soDienThoai, cccd? } -> gửi email có liên kết /verify-email?token=...
// Số CCCD không bắt buộc: có thì lịch đã đặt như khách bằng CCCD đó được liên kết vào tài khoản ở lần đăng nhập đầu.
(() => {
    dangKy({
        duongDan: '/register',
        tieuDe: cap('Đăng ký', 'Register'),
        ve(khung) {
            khuon(khung, `
                <section>
                    <h2>${t('Đăng ký tài khoản bệnh nhân', 'Register a patient account')}</h2>
                    <form id="form-dang-ky">
                        <input name="hoTen" placeholder="${t('Họ tên', 'Full name')}" required>
                        <input name="email" type="email" placeholder="Email" required>
                        <input name="soDienThoai" placeholder="${t('Số điện thoại (10 số, bắt đầu bằng 0)', 'Phone number (10 digits, starting with 0)')}" required>
                        <input name="matKhau" type="password" placeholder="${t('Mật khẩu (tối thiểu 6 ký tự)', 'Password (at least 6 characters)')}" required autocomplete="new-password">
                        <input name="cccd" placeholder="${t('Số CCCD (12 số, không bắt buộc): liên kết lịch đã đặt như khách', 'National ID (12 digits, optional): links appointments booked as a guest')}">
                        <button>${t('Đăng ký', 'Register')}</button>
                    </form>
                    <p id="dang-ky-trang-thai" class="ghi-chu"></p>
                    <p>${t('Đã có tài khoản?', 'Already have an account?')} <a href="/login">Login</a></p>
                </section>`);
            khiGui('form-dang-ky', async (v, form) => {
                const { status, json } = await goi('/api/auth/register', v);
                if (status === 200 || status === 201) {
                    form.reset();
                    bao('dang-ky-trang-thai', (json && json.thongDiep)
                        || t('Đã đăng ký: mở liên kết kích hoạt trong email rồi đăng nhập.', 'Registered: open the activation link in the email, then sign in.'), 'tot');
                } else {
                    bao('dang-ky-trang-thai', thongDiepLoi(json, t('Đăng ký thất bại.', 'Registration failed.')), 'loi');
                }
            });
        },
    });
})();
