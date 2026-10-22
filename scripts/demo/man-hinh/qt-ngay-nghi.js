// Màn hình ngoài danh sách UI: Ngày nghỉ bệnh viện (Tết, lễ, nghỉ đột xuất)
// Đường dẫn:  /admin/closures
// Vai trò:    QUAN_TRI_VIEN
// API:
//   GET    /api/booking/quan-tri/ngay-nghi?tuNgay=&denNgay=   ngày nghỉ trong khoảng (mặc định từ hôm nay, 1 năm; tối đa 366 ngày):
//          id, ngay, lyDo, lyDoEn, quanTriTao { id, hoTen }, ngayTao
//   POST   /api/booking/quan-tri/ngay-nghi/xem-truoc   { tuNgay, denNgay, lyDo, lyDoEn? } chạy như khi lưu rồi hoàn tác, không báo ai:
//          { daLuu: false, ngayMoi[], ngayDaNghi[] (đã nghỉ từ trước, bỏ qua), caBiHuy[] { idLichLamViec, ngay, gioBatDau,
//          gioKetThuc, bacSi, phongKham, soLichHenCanDoi }, soLichHenCanDoi }
//   POST   /api/booking/quan-tri/ngay-nghi             cùng body -> 201, cùng dạng (daLuu: true). Từ ngày mai, tối đa 42 ngày; mọi
//          ngày đều đã nghỉ -> 409 NGAY_BENH_VIEN_NGHI. Ca còn hoạt động bị hủy: lịch hẹn giữ nguyên, đánh dấu cần đổi lịch,
//          bệnh nhân được báo + email (kèm link phiếu khám), mỗi bác sĩ được báo 1 lần
//   DELETE /api/booking/quan-tri/ngay-nghi/{ngay}      bỏ 1 ngày nghỉ chưa tới (hôm nay / đã qua -> 400); ca đã hủy KHÔNG khôi phục
// Sau khi khai báo: xếp / sao chép / nhập ca vào ngày nghỉ bị từ chối (409 NGAY_BENH_VIEN_NGHI hoặc lý do bỏ qua); bệnh nhân
// thấy ngày nghỉ ở GET /api/booking/ngay-nghi (trang đặt lịch).
(() => {
    const NGAY_NGHI = '/api/booking/quan-tri/ngay-nghi';
    let daXemTruoc = null;   // body của lần xem trước gần nhất: "Lưu" chỉ bật khi form vẫn đúng như vậy

    function thanKhaiBao() {
        const f = $('form-ngay-nghi');
        const soNgay = Number(f.elements.soNgay.value || 1);
        const tu = f.elements.tuNgay.value;
        if (!tu) return null;
        const [nam, thang, ngay] = tu.split('-').map(Number);
        const den = ngayIso(new Date(nam, thang - 1, ngay + soNgay - 1));
        const than = { tuNgay: tu, denNgay: den, lyDo: f.elements.lyDo.value.trim() };
        if (f.elements.lyDoEn.value.trim()) than.lyDoEn = f.elements.lyDoEn.value.trim();
        return than;
    }

    function hienKetQua(kq) {
        const phan = [the('p', kq.daLuu
            ? t(`Đã khai báo ${kq.ngayMoi.length} ngày nghỉ; đã hủy ${kq.caBiHuy.length} ca, ${kq.soLichHenCanDoi} lịch hẹn cần đổi (bệnh nhân đã được báo).`,
                `Declared ${kq.ngayMoi.length} closure day(s); cancelled ${kq.caBiHuy.length} shift(s), ${kq.soLichHenCanDoi} appointment(s) need a reschedule (patients notified).`)
            : t(`Xem trước: ${kq.ngayMoi.length} ngày mới, ${kq.caBiHuy.length} ca sẽ bị hủy, ${kq.soLichHenCanDoi} lịch hẹn sẽ phải đổi.`,
                `Preview: ${kq.ngayMoi.length} new day(s), ${kq.caBiHuy.length} shift(s) will be cancelled, ${kq.soLichHenCanDoi} appointment(s) will need a reschedule.`),
        kq.daLuu ? 'ghi-chu tot' : 'ghi-chu')];
        if (kq.ngayMoi.length) phan.push(ghiChu(t('Ngày mới: ', 'New days: ') + kq.ngayMoi.map((n) => `${thuCuaNgay(n.ngay)} ${ngayVn(n.ngay)}`).join(', ')));
        if (kq.ngayDaNghi.length) phan.push(ghiChu(t('Đã nghỉ từ trước (bỏ qua): ', 'Already closed (skipped): ') + kq.ngayDaNghi.map(ngayVn).join(', ')));
        if (kq.caBiHuy.length) {
            phan.push(bang([t('Ngày', 'Date'), t('Giờ', 'Hours'), t('Bác sĩ', 'Doctor'), t('Phòng', 'Room'), t('Lịch hẹn phải đổi', 'Appointments to move')],
                kq.caBiHuy.map((ca) => dong([`${thuCuaNgay(ca.ngay)} ${ngayVn(ca.ngay)}`, `${ca.gioBatDau.slice(0, 5)}–${ca.gioKetThuc.slice(0, 5)}`,
                    tenBacSi(ca.bacSi), tenPhong(ca.phongKham), ca.soLichHenCanDoi]))));
        }
        $('nn-ket-qua').replaceChildren(...phan);
    }

    async function taiDanhSach(imLang = false) {
        const { status, json } = await lay(NGAY_NGHI, imLang);
        const khung = $('nn-ds');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được ngày nghỉ.', 'Could not load the closure days.')), 'ghi-chu loi'));
            return;
        }
        const homNay = congNgay(0);
        khung.replaceChildren(json.duLieu.length
            ? bang([t('Ngày', 'Date'), t('Lý do', 'Reason'), t('Lý do (EN)', 'Reason (EN)'), t('Khai báo bởi', 'Declared by'), t('Thao tác', 'Actions')],
                json.duLieu.map((n) => dong([`${thuCuaNgay(n.ngay)} ${ngayVn(n.ngay)}`, n.lyDo, n.lyDoEn,
                    `${n.quanTriTao ? n.quanTriTao.hoTen : ''} · ${ngayGioVn(n.ngayTao)}`,
                    n.ngay > homNay ? nut(t('Bỏ ngày nghỉ', 'Remove'), () => xoa(n.ngay), 'nguy-hiem nho') : ''])))
            : ghiChu(t('Chưa có ngày nghỉ nào từ hôm nay.', 'No closure days from today on.')));
    }

    async function xoa(ngay) {
        if (!confirm(t(`Bỏ ngày nghỉ ${ngayVn(ngay)}? Ca đã hủy không được khôi phục; ngày đó xếp ca lại được.`,
            `Remove the closure on ${ngayVn(ngay)}? Cancelled shifts are not restored; shifts can be scheduled on that day again.`))) return;
        const { status, json } = await goi(`${NGAY_NGHI}/${ngay}`, undefined, 'DELETE');
        bao('nn-trang-thai', status === 200 ? json.thongDiep : thongDiepLoi(json, t('Không bỏ được.', 'Could not remove it.')), status === 200 ? 'tot' : 'loi');
        if (status === 200) await taiDanhSach(true);
    }

    dangKy({
        duongDan: '/admin/closures',
        vaiTro: 'QUAN_TRI_VIEN',
        tieuDe: cap('Ngày nghỉ bệnh viện', 'Hospital Closures'),
        async ve(khung) {
            daXemTruoc = null;
            khuon(khung, `
                <section>
                    <h2>${t('Khai báo ngày nghỉ', 'Declare closure days')}</h2>
                    <p class="ghi-chu">${t('Từ ngày mai trở đi, tối đa 42 ngày liền. Bấm "Xem trước" để thấy các ca sẽ bị hủy và số lịch hẹn phải đổi, rồi mới "Lưu".',
                        'From tomorrow on, at most 42 days in a row. Press "Preview" to see the shifts that will be cancelled and the appointments to move, then "Save".')}</p>
                    <form id="form-ngay-nghi">
                        <div class="hai-cot">
                            <div><label>${t('Từ ngày', 'From')}</label><input name="tuNgay" type="date" required></div>
                            <div><label>${t('Số ngày', 'Number of days')}</label><input name="soNgay" type="number" min="1" max="42" value="1" required></div>
                            <div><label>${t('Lý do (tiếng Việt, bắt buộc)', 'Reason (Vietnamese, required)')}</label><input name="lyDo" maxlength="200" required></div>
                            <div><label>${t('Lý do (tiếng Anh, không bắt buộc)', 'Reason (English, optional)')}</label><input name="lyDoEn" maxlength="200"></div>
                        </div>
                        <button>${t('Xem trước', 'Preview')}</button>
                        <button type="button" id="nn-luu" disabled>${t('Lưu (hủy các ca trên, báo bệnh nhân và bác sĩ)', 'Save (cancel the shifts above, notify patients and doctors)')}</button>
                    </form>
                    <p id="nn-trang-thai" class="ghi-chu"></p>
                    <div id="nn-ket-qua" class="bang"></div>
                </section>
                <section>
                    <h2>${t('Ngày nghỉ từ hôm nay', 'Closure days from today')}</h2>
                    <div id="nn-ds" class="bang"></div>
                </section>`);
            const f = $('form-ngay-nghi');
            f.elements.tuNgay.min = congNgay(1);
            f.addEventListener('input', () => {
                daXemTruoc = null;
                $('nn-luu').disabled = true;
            });
            f.addEventListener('submit', async (e) => {
                e.preventDefault();
                const than = thanKhaiBao();
                const { status, json } = await goi(`${NGAY_NGHI}/xem-truoc`, than);
                if (status !== 200) {
                    bao('nn-trang-thai', thongDiepLoi(json, t('Không xem trước được.', 'Could not preview.')), 'loi');
                    $('nn-ket-qua').replaceChildren();
                    return;
                }
                bao('nn-trang-thai', '');
                hienKetQua(json.duLieu);
                daXemTruoc = JSON.stringify(than);
                $('nn-luu').disabled = !json.duLieu.ngayMoi.length;
            });
            $('nn-luu').addEventListener('click', async () => {
                const than = thanKhaiBao();
                if (JSON.stringify(than) !== daXemTruoc) return;
                const { status, json } = await goi(NGAY_NGHI, than);
                $('nn-luu').disabled = true;
                daXemTruoc = null;
                if (status !== 201) {
                    bao('nn-trang-thai', thongDiepLoi(json, t('Không lưu được.', 'Could not save.')), 'loi');
                    return;
                }
                bao('nn-trang-thai', json.thongDiep, 'tot');
                hienKetQua(json.duLieu);
                await taiDanhSach(true);
            });
            await taiDanhSach(true);
        },
    });
})();
