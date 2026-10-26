// Màn hình ngoài danh sách UI: 2 trang mở từ liên kết trong email
// Đường dẫn:  /verify-email?token=...           kích hoạt tài khoản sau khi đăng ký
//             /confirm-email-change?token=...   xác nhận đổi email đăng nhập
// Vai trò:    công khai
// API:
//   POST /api/auth/verify-email           { token } : tự gọi khi mở trang
//   POST /api/auth/confirm-email-change   { token } : CHỈ gọi khi bấm nút (trình quét liên kết trong email chạy JavaScript
//                                                     không được phép xác nhận thay người dùng)
// Token lấy từ URL rồi xoá ngay khỏi thanh địa chỉ.
(() => {
    dangKy({
        duongDan: '/verify-email',
        tieuDe: cap('Kích hoạt tài khoản', 'Activate account'),
        async ve(khung, { thamSo, duongDan }) {
            const token = thamSo.get('token') || '';
            history.replaceState(null, '', duongDan);
            const trangThai = the('p', t('Đang kích hoạt…', 'Activating…'));
            khung.append(trangThai, lienKet('Login', '/login'));
            const { status, json } = await goi('/api/auth/verify-email', { token });
            trangThai.textContent = status === 200
                ? t('Kích hoạt thành công, bạn có thể đăng nhập.', 'Account activated, you can sign in now.')
                : thongDiepLoi(json, t('Kích hoạt thất bại.', 'Activation failed.'));
        },
    });

    dangKy({
        duongDan: '/confirm-email-change',
        tieuDe: cap('Xác nhận đổi email đăng nhập', 'Confirm sign-in email change'),
        ve(khung, { thamSo, duongDan }) {
            const token = thamSo.get('token') || '';
            history.replaceState(null, '', duongDan);
            const trangThai = the('p', t('Bấm nút để dùng địa chỉ email này làm email đăng nhập. Mọi thiết bị sẽ bị đăng xuất.',
                'Press the button to use this email address as your sign-in email. All devices will be signed out.'));
            const xacNhan = nut(t('Xác nhận đổi email', 'Confirm email change'), async () => {
                xacNhan.disabled = true;
                const { status, json } = await goi('/api/auth/confirm-email-change', { token });
                trangThai.textContent = status === 200
                    ? t('Đã đổi email đăng nhập. Hãy đăng nhập lại bằng email mới.', 'Sign-in email changed. Please sign in again with the new email.')
                    : thongDiepLoi(json, t('Xác nhận đổi email thất bại.', 'Could not confirm the email change.'));
                // Liên kết chỉ dùng được 1 lần: thành công hay 410/409 thì nút cũng không còn tác dụng
                xacNhan.classList.add('an');
            }, '');
            khung.append(trangThai, xacNhan, lienKet('Login', '/login'));
        },
    });
})();
