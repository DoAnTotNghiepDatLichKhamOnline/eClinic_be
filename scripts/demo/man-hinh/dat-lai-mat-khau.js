// Màn hình UI: Reset Password (Authentication)
// Đường dẫn:  /reset-password?token=...   (liên kết trong email "quên mật khẩu")
// Vai trò:    công khai
// API:
//   POST /api/auth/reset-password   { token, matKhauMoi }
// Token lấy từ URL rồi xoá ngay khỏi thanh địa chỉ (history.replaceState) để không nằm lại trong lịch sử trình duyệt.
(() => {
    dangKy({
        duongDan: '/reset-password',
        tieuDe: 'Reset Password',
        ve(khung, { thamSo, duongDan }) {
            const token = thamSo.get('token') || '';
            history.replaceState(null, '', duongDan);
            khuon(khung, `
                <section>
                    <h2>Đặt lại mật khẩu</h2>
                    <form id="form-dat-lai">
                        <input type="password" name="matKhauMoi" placeholder="Mật khẩu mới (tối thiểu 6 ký tự)" required minlength="6" autocomplete="new-password">
                        <input type="password" name="nhapLai" placeholder="Nhập lại mật khẩu mới" required autocomplete="new-password">
                        <button>Đặt lại mật khẩu</button>
                    </form>
                    <p id="dat-lai-trang-thai" class="ghi-chu"></p>
                    <p><a href="/login">Back to login</a></p>
                </section>`);
            if (!token) bao('dat-lai-trang-thai', 'Thiếu token: mở màn hình này từ liên kết trong email.', 'loi');
            khiGui('form-dat-lai', async (v, form) => {
                if (v.matKhauMoi !== v.nhapLai) {
                    bao('dat-lai-trang-thai', 'Hai lần nhập mật khẩu không khớp.', 'loi');
                    return;
                }
                const { status, json } = await goi('/api/auth/reset-password', { token, matKhauMoi: v.matKhauMoi });
                if (status === 200) {
                    form.reset();
                    bao('dat-lai-trang-thai', 'Đã đặt lại mật khẩu, bạn có thể đăng nhập.', 'tot');
                } else {
                    bao('dat-lai-trang-thai', thongDiepLoi(json, 'Đặt lại mật khẩu thất bại.'), 'loi');
                }
            });
        },
    });
})();
