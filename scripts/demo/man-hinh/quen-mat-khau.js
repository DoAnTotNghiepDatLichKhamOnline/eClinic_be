// Màn hình UI: Forgot Password (Authentication)
// Đường dẫn:  /forgot-password
// Vai trò:    công khai
// API:
//   POST /api/auth/forgot-password   { email } -> gửi email có liên kết /reset-password?token=...
(() => {
    dangKy({
        duongDan: '/forgot-password',
        tieuDe: 'Forgot Password',
        ve(khung) {
            khuon(khung, `
                <section>
                    <h2>Quên mật khẩu</h2>
                    <form id="form-quen">
                        <input name="email" type="email" placeholder="Email" required>
                        <button>Gửi liên kết đặt lại</button>
                    </form>
                    <p id="quen-trang-thai" class="ghi-chu"></p>
                    <p><a href="/login">Back to login</a></p>
                </section>`);
            khiGui('form-quen', async (v) => {
                const { status, json } = await goi('/api/auth/forgot-password', v);
                bao('quen-trang-thai', status === 200 ? ((json && json.thongDiep) || 'Đã gửi liên kết đặt lại mật khẩu.') : thongDiepLoi(json, 'Không gửi được.'),
                    status === 200 ? 'tot' : 'loi');
            });
        },
    });
})();
