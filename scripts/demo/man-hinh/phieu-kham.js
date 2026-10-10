// Màn hình UI: Appointment Ticket (Patient) — trong UI là hộp thoại sau khi đặt; đây là trang mở từ link / mã QR của phiếu
// Đường dẫn:  /phieu-kham/<maPhieuKham>   (= FRONTEND_URL + /phieu-kham/ + mã; chính là nội dung mã QR)
// Vai trò:    công khai (ai có link đều xem được)
// API:
//   GET /api/booking/phieu-kham/{maPhieuKham}        nội dung phiếu; lyDoHuy = lý do bác sĩ từ chối (khi trangThai là BI_TU_CHOI)
//   GET /api/booking/phieu-kham/{maPhieuKham}/qr     ảnh PNG mã QR (?taiVe=true để tải về)
//   POST /api/booking/phieu-kham/{maPhieuKham}/huy       { soDienThoai, lyDo? }   hủy bằng link phiếu khám + đúng SĐT liên hệ đã nhập
//        lúc đặt lịch (phiếu chỉ hiện SĐT đã che); trả phiếu khám DA_HUY
//   POST /api/booking/phieu-kham/{maPhieuKham}/doi-lich  { soDienThoai, idLichLamViec | idChuyenKhoa, gioBatDauKhung } -> 201,
//        lịch mới với mã phiếu khám mới. SĐT sai -> 403 SO_DIEN_THOAI_KHONG_KHOP; số lần gọi theo IP bị giới hạn
//        Phiếu có duocHuyDoi / hanHuyDoi để ẩn / hiện nút.
// Mã phiếu khám là base64url nên không cần giải mã URL; mã sai dạng thì API trả 404 kèm thông điệp.
(() => {
    dangKy({
        duongDan: /^\/phieu-kham\/([^/]+)$/,
        tieuDe: 'Appointment Ticket',
        async ve(khung, { khop }) {
            const trangThai = the('p', 'Đang tải phiếu khám…');
            const noiDung = the('div');
            khung.append(trangThai, noiDung, lienKet('Đặt lịch khám', '/appointment'));
            const { status, json } = await taiPhieu(khop[1], false);
            if (status === 200) {
                trangThai.textContent = 'Xuất trình phiếu này (hoặc mã QR) khi đến khám.';
                const hien = (phieu) => {
                    hienPhieu(noiDung, phieu);
                    const goc = '/api/booking/phieu-kham/' + encodeURIComponent(phieu.maPhieuKham);
                    noiDung.append(khoiHuyDoi({
                        lich: phieu,
                        canSoDienThoai: true,
                        gui: (hanhDong, body) => goi(`${goc}/${hanhDong}`, body),
                        khiXong(hanhDong, duLieu) {
                            if (hanhDong === 'huy') {
                                hien(duLieu);
                                bao(trangThai, 'Đã hủy lịch hẹn.', 'tot');
                            } else {
                                // Lịch mới có phiếu khám mới: chuyển sang trang của phiếu đó
                                diToi('/phieu-kham/' + encodeURIComponent(duLieu.maPhieuKham));
                            }
                        },
                    }));
                };
                hien(json.duLieu);
            } else {
                bao(trangThai, thongDiepLoi(json, 'Không tải được phiếu khám.'), 'loi');
            }
        },
    });
})();
