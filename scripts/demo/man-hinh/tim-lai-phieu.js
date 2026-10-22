// Màn hình ngoài danh sách UI: khách mất link phiếu khám tự xin gửi lại (PAT-03)
// Đường dẫn:  /find-ticket
// Vai trò:    công khai
// API:
//   POST /api/booking/cap-lai-lien-ket   { maTraCuu (ECL-YYYYMMDD-NNNN, in trên phiếu / email), soDienThoai (đầy đủ SĐT liên hệ lúc
//        đặt; có người giám hộ: SĐT người giám hộ) } -> luôn 200 cùng 1 câu, dù mã có thật hay không, SĐT đúng hay sai, lịch hẹn
//        có email hay không. Link phiếu khám (và sổ khám bệnh nếu đã khám) chỉ được gửi tới email của lịch hẹn, không bao giờ
//        hiện trên màn hình; lịch đã đổi thì gửi phiếu của lịch mới nhất. Mỗi mã 3 lần, mỗi IP 10 lần trong 1 giờ (đúng hay sai
//        đều tính): vượt -> 429 GUI_LAI_QUA_NHANH kèm số phút còn lại. Lịch không có email: đến quầy / gọi tổng đài, nhân viên
//        cấp lại sau khi xem CCCD.
(() => {
    dangKy({
        duongDan: '/find-ticket',
        tieuDe: cap('Lấy lại link phiếu khám', 'Find my ticket link'),
        async ve(khung) {
            khuon(khung, `
                <section>
                    <p>${t('Nhập mã tra cứu (ECL-…, in trên phiếu khám và trong email đặt lịch) và đúng số điện thoại liên hệ đã dùng khi đặt. Nếu khớp, link phiếu khám được gửi lại tới email của lịch hẹn.',
                        'Enter the lookup code (ECL-…, printed on the ticket and in the booking email) and the exact contact phone used when booking. If they match, the ticket link is emailed again to the booking address.')}</p>
                    <form id="form-tim-lai" class="hai-cot">
                        <div><label>${t('Mã tra cứu', 'Lookup code')}</label><input name="maTraCuu" required placeholder="ECL-20261020-0001" maxlength="20"></div>
                        <div><label>${t('Số điện thoại liên hệ', 'Contact phone')}</label><input name="soDienThoai" required pattern="0\\d{9}" maxlength="10" inputmode="numeric"></div>
                        <button>${t('Gửi lại link', 'Send the link again')}</button>
                    </form>
                    <p id="tim-lai-trang-thai" class="ghi-chu"></p>
                    <p class="ghi-chu">${t('Lịch hẹn không có email (đặt qua tổng đài): mang CCCD đến quầy hoặc gọi tổng đài để nhân viên cấp lại.',
                        'A booking with no email (made by phone): bring your national ID to the front desk or call the hotline and staff will reissue it.')}</p>
                </section>`);
            khung.append(lienKet(t('Đặt lịch khám', 'Book an appointment'), '/appointment'));
            khiGui('form-tim-lai', async (v) => {
                const { status, json } = await goi('/api/booking/cap-lai-lien-ket', { maTraCuu: v.maTraCuu.trim(), soDienThoai: v.soDienThoai.trim() });
                bao('tim-lai-trang-thai', status === 200 ? json.thongDiep : thongDiepLoi(json, t('Không gửi được yêu cầu.', 'Could not send the request.')),
                    status === 200 ? 'tot' : 'loi');
            });
        },
    });
})();
