// Màn hình UI: Dashboard (Admin) — các ô số liệu toàn viện
// Đường dẫn:  /admin/dashboard
// Vai trò:    QUAN_TRI_VIEN
// API:
//   GET /api/booking/quan-tri/tong-quan   3 ô số liệu trong 1 lần gọi:
//         bacSiDangCongTac   bác sĩ đang công tác có tài khoản đã kích hoạt (đúng danh sách công khai)
//         benhNhanDaDangKy   tài khoản bệnh nhân đã kích hoạt (khách chỉ đặt lịch không tính)
//         luotDatTrongThang  lịch hẹn được tạo trong tháng này, mọi trạng thái; lịch cũ của 1 lần đổi lịch không tính
//         tinhLuc            thời điểm tính
// "System Uptime" của UI không được cung cấp (thoả thuận A2 của DOANTOTNGH-7).
(() => {
    dangKy({
        duongDan: '/admin/dashboard',
        vaiTro: 'QUAN_TRI_VIEN',
        tieuDe: cap('Tổng quan', 'Dashboard'),
        async ve(khung) {
            const oSo = the('div', null, 'o-so');
            const tinhLuc = ghiChu('');
            const loiTat = the('section');
            loiTat.append(the('h2', t('Các màn hình quản trị', 'Admin screens')), ...[
                [t('Quản lý lịch làm việc', 'Schedule Management'), '/admin/schedule-management'], [t('Danh mục y tế', 'Medical Catalog'), '/admin/medical-catalog'],
                [t('Quản lý bác sĩ', 'Doctor Management'), '/admin/doctors'], [t('Quản lý bệnh nhân', 'Patient Management'), '/admin/patients'],
                [t('Tài khoản', 'Accounts'), '/admin/accounts'],
                [t('Lịch hẹn toàn viện', 'All Appointments'), '/admin/appointments'], [t('Đặt lịch qua tổng đài', 'Hotline Booking'), '/admin/hotline-booking'],
                [t('Quy tắc đặt lịch', 'Booking Rules'), '/admin/booking-rules'], [t('Ngày nghỉ bệnh viện', 'Hospital Closures'), '/admin/closures'],
                [t('Báo cáo thống kê (theo kỳ, xuất Excel)', 'Reports (by period, Excel export)'), '/admin/reports'],
            ].map(([nhan, href]) => {
                const p = the('p');
                p.append(lienKet(nhan, href));
                return p;
            }));
            khung.append(oSo, tinhLuc, loiTat);

            const { status, json } = await lay('/api/booking/quan-tri/tong-quan', true);
            const sl = status === 200 ? json.duLieu : null;
            for (const [nhan, giaTri] of [
                ['Active Doctors', sl ? sl.bacSiDangCongTac : '—'],
                ['Registered Patients', sl ? sl.benhNhanDaDangKy : '—'],
                ['Bookings This Month', sl ? sl.luotDatTrongThang : '—'],
            ]) {
                const o = the('div');
                o.append(the('b', String(giaTri)), the('span', nhan, 'ghi-chu'));
                oSo.append(o);
            }
            tinhLuc.textContent = sl ? t(`Tính lúc ${ngayGioVn(sl.tinhLuc)}`, `Calculated at ${ngayGioVn(sl.tinhLuc)}`)
                : thongDiepLoi(json, t('Không tải được số liệu.', 'Could not load the figures.'));
        },
    });
})();
