// Màn hình UI: Reset Password (Authentication)
// Đường dẫn:  /reset-password?token=...   (liên kết trong email "quên mật khẩu")
// Vai trò:    công khai
// API:
//   POST /api/auth/reset-password   { token, matKhauMoi }
// Token lấy từ URL rồi xoá ngay khỏi thanh địa chỉ (history.replaceState) để không nằm lại trong lịch sử trình duyệt.
(() => {
    dangKy({
        duongDan: '/reset-password',
        tieuDe: cap('Đặt lại mật khẩu', 'Reset Password'),
        ve(khung, { thamSo, duongDan }) {
            const token = thamSo.get('token') || '';
            history.replaceState(null, '', duongDan);
            khuon(khung, `
                <section>
                    <h2>${t('Đặt lại mật khẩu', 'Reset password')}</h2>
                    <form id="form-dat-lai">
                        <input type="password" name="matKhauMoi" placeholder="${t('Mật khẩu mới (tối thiểu 6 ký tự)', 'New password (at least 6 characters)')}" required minlength="6" autocomplete="new-password">
                        <input type="password" name="nhapLai" placeholder="${t('Nhập lại mật khẩu mới', 'Repeat the new password')}" required autocomplete="new-password">
                        <button>${t('Đặt lại mật khẩu', 'Reset password')}</button>
                    </form>
                    <p id="dat-lai-trang-thai" class="ghi-chu"></p>
                    <p><a href="/login">Back to login</a></p>
                </section>`);
            if (!token) bao('dat-lai-trang-thai', t('Thiếu token: mở màn hình này từ liên kết trong email.', 'Missing token: open this screen from the link in the email.'), 'loi');
            khiGui('form-dat-lai', async (v, form) => {
                if (v.matKhauMoi !== v.nhapLai) {
                    bao('dat-lai-trang-thai', t('Hai lần nhập mật khẩu không khớp.', 'The two passwords do not match.'), 'loi');
                    return;
                }
                const { status, json } = await goi('/api/auth/reset-password', { token, matKhauMoi: v.matKhauMoi });
                if (status === 200) {
                    form.reset();
                    bao('dat-lai-trang-thai', t('Đã đặt lại mật khẩu, bạn có thể đăng nhập.', 'Password reset, you can sign in now.'), 'tot');
                } else {
                    bao('dat-lai-trang-thai', thongDiepLoi(json, t('Đặt lại mật khẩu thất bại.', 'Password reset failed.')), 'loi');
                }
            });
        },
    });
})();
