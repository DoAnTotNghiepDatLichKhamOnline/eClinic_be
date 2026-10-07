// Màn hình UI: Register (Authentication)
// Đường dẫn:  /register
// Vai trò:    công khai
// API:
//   POST /api/auth/register   { hoTen, email, matKhau, soDienThoai, cccd? } -> gửi email có liên kết /verify-email?token=...
// Số CCCD không bắt buộc: có thì lịch đã đặt như khách bằng CCCD đó được liên kết vào tài khoản ở lần đăng nhập đầu.
(() => {
    dangKy({
        duongDan: '/register',
        tieuDe: 'Register',
        ve(khung) {
            khuon(khung, `
                <section>
                    <h2>Đăng ký tài khoản bệnh nhân</h2>
                    <form id="form-dang-ky">
                        <input name="hoTen" placeholder="Họ tên" required>
                        <input name="email" type="email" placeholder="Email" required>
                        <input name="soDienThoai" placeholder="Số điện thoại (10 số, bắt đầu bằng 0)" required>
                        <input name="matKhau" type="password" placeholder="Mật khẩu (tối thiểu 6 ký tự)" required autocomplete="new-password">
                        <input name="cccd" placeholder="Số CCCD (12 số, không bắt buộc): liên kết lịch đã đặt như khách">
                        <button>Đăng ký</button>
                    </form>
                    <p id="dang-ky-trang-thai" class="ghi-chu"></p>
                    <p>Đã có tài khoản? <a href="/login">Login</a></p>
                </section>`);
            khiGui('form-dang-ky', async (v, form) => {
                const { status, json } = await goi('/api/auth/register', v);
                if (status === 200 || status === 201) {
                    form.reset();
                    bao('dang-ky-trang-thai', (json && json.thongDiep) || 'Đã đăng ký: mở liên kết kích hoạt trong email rồi đăng nhập.', 'tot');
                } else {
                    bao('dang-ky-trang-thai', thongDiepLoi(json, 'Đăng ký thất bại.'), 'loi');
                }
            });
        },
    });
})();
