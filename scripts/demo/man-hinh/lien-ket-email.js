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
        tieuDe: 'Kích hoạt tài khoản',
        async ve(khung, { thamSo, duongDan }) {
            const token = thamSo.get('token') || '';
            history.replaceState(null, '', duongDan);
            const trangThai = the('p', 'Đang kích hoạt…');
            khung.append(trangThai, lienKet('Login', '/login'));
            const { status, json } = await goi('/api/auth/verify-email', { token });
            trangThai.textContent = status === 200 ? 'Kích hoạt thành công, bạn có thể đăng nhập.' : thongDiepLoi(json, 'Kích hoạt thất bại.');
        },
    });

    dangKy({
        duongDan: '/confirm-email-change',
        tieuDe: 'Xác nhận đổi email đăng nhập',
        ve(khung, { thamSo, duongDan }) {
            const token = thamSo.get('token') || '';
            history.replaceState(null, '', duongDan);
            const trangThai = the('p', 'Bấm nút để dùng địa chỉ email này làm email đăng nhập. Mọi thiết bị sẽ bị đăng xuất.');
            const xacNhan = nut('Xác nhận đổi email', async () => {
                xacNhan.disabled = true;
                const { status, json } = await goi('/api/auth/confirm-email-change', { token });
                trangThai.textContent = status === 200
                    ? 'Đã đổi email đăng nhập. Hãy đăng nhập lại bằng email mới.'
                    : thongDiepLoi(json, 'Xác nhận đổi email thất bại.');
                // Liên kết chỉ dùng được 1 lần: thành công hay 410/409 thì nút cũng không còn tác dụng
                xacNhan.classList.add('an');
            }, '');
            khung.append(trangThai, xacNhan, lienKet('Login', '/login'));
        },
    });
})();
