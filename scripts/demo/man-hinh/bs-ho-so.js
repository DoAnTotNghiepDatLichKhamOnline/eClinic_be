// Màn hình UI: Doctor Profile (Doctor) — chỉ xem; hồ sơ bác sĩ do quản trị viên sửa ở /admin/doctors
// Đường dẫn:  /doctor/profile
// Vai trò:    BAC_SI
// API:
//   GET /api/users/me   hồ sơ cá nhân; phần bacSi: idChuyenKhoa, tenChuyenKhoa, hocVi, soGiayPhep, soNamKinhNghiem, tieuSu, trangThai
// Đổi số điện thoại, ảnh đại diện, mật khẩu: /account.
(() => {
    dangKy({
        duongDan: '/doctor/profile',
        vaiTro: 'BAC_SI',
        tieuDe: 'Doctor Profile',
        async ve(khung) {
            const phan = the('section');
            phan.append(ghiChu('Đang tải…'));
            khung.append(phan);
            const { status, json } = await lay('/api/users/me');
            if (status !== 200) {
                phan.replaceChildren(the('p', thongDiepLoi(json, 'Không tải được hồ sơ.'), 'ghi-chu loi'));
                return;
            }
            const hs = json.duLieu, bs = hs.bacSi || {};
            phan.replaceChildren(anh(hs.anhDaiDien, 'Ảnh đại diện', 'anh-dai-dien lon'), thongTin([
                ['Họ tên', hs.hoTen], ['Email', hs.email], ['Số điện thoại', hs.soDienThoai],
                ['Học vị', bs.hocVi], ['Chuyên khoa', bs.tenChuyenKhoa], ['Số giấy phép hành nghề', bs.soGiayPhep],
                ['Số năm kinh nghiệm', bs.soNamKinhNghiem], ['Tiểu sử', bs.tieuSu], ['Trạng thái', bs.trangThai],
            ]), ghiChu('Hồ sơ bác sĩ chỉ xem: quản trị viên sửa ở Doctor Management.'), lienKet('Đổi số điện thoại, ảnh đại diện, mật khẩu', '/account'));
        },
    });
})();
