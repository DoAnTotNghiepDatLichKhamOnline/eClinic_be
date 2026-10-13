// Màn hình UI: Forgot Password (Authentication)
// Đường dẫn:  /forgot-password
// Vai trò:    công khai
// API:
//   POST /api/auth/forgot-password   { email } -> gửi email có liên kết /reset-password?token=...
(() => {
    dangKy({
        duongDan: '/forgot-password',
        tieuDe: cap('Quên mật khẩu', 'Forgot Password'),
        ve(khung) {
            khuon(khung, `
                <section>
                    <h2>${t('Quên mật khẩu', 'Forgot password')}</h2>
                    <form id="form-quen">
                        <input name="email" type="email" placeholder="Email" required>
                        <button>${t('Gửi liên kết đặt lại', 'Send the reset link')}</button>
                    </form>
                    <p id="quen-trang-thai" class="ghi-chu"></p>
                    <p><a href="/login">Back to login</a></p>
                </section>`);
            khiGui('form-quen', async (v) => {
                const { status, json } = await goi('/api/auth/forgot-password', v);
                bao('quen-trang-thai', status === 200 ? ((json && json.thongDiep) || t('Đã gửi liên kết đặt lại mật khẩu.', 'The password reset link has been sent.'))
                    : thongDiepLoi(json, t('Không gửi được.', 'Could not send.')),
                    status === 200 ? 'tot' : 'loi');
            });
        },
    });
})();
