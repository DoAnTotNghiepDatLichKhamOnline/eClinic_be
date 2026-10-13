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
        tieuDe: cap('Hồ sơ bác sĩ', 'Doctor Profile'),
        async ve(khung) {
            const phan = the('section');
            phan.append(ghiChu(t('Đang tải…', 'Loading…')));
            khung.append(phan);
            const { status, json } = await lay('/api/users/me');
            if (status !== 200) {
                phan.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được hồ sơ.', 'Could not load the profile.')), 'ghi-chu loi'));
                return;
            }
            const hs = json.duLieu, bs = hs.bacSi || {};
            phan.replaceChildren(anh(hs.anhDaiDien, t('Ảnh đại diện', 'Profile photo'), 'anh-dai-dien lon'), thongTin([
                [t('Họ tên', 'Full name'), hs.hoTen], ['Email', hs.email], [t('Số điện thoại', 'Phone number'), hs.soDienThoai],
                [t('Học vị', 'Academic degree'), bs.hocVi], [t('Chuyên khoa', 'Specialty'), bs.tenChuyenKhoa],
                [t('Số giấy phép hành nghề', 'Practice licence number'), bs.soGiayPhep],
                [t('Số năm kinh nghiệm', 'Years of experience'), bs.soNamKinhNghiem], [t('Tiểu sử', 'Biography'), bs.tieuSu],
                [t('Trạng thái', 'Status'), bs.trangThai],
            ]), ghiChu(t('Hồ sơ bác sĩ chỉ xem: quản trị viên sửa ở Quản lý bác sĩ.',
                'The doctor profile is read-only: an administrator edits it in Doctor Management.')),
            lienKet(t('Đổi số điện thoại, ảnh đại diện, mật khẩu', 'Change phone number, profile photo, password'), '/account'));
        },
    });
})();
