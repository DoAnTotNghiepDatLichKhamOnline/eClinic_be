// Màn hình UI: Work Schedule (Doctor) — lịch làm việc theo ngày / tuần và nút "Request a shift change / leave"
// Đường dẫn:  /doctor/work-schedule
// Vai trò:    BAC_SI
// API:
//   GET /api/booking/bac-si/toi/lich-lam-viec?tuNgay=&denNgay=     các ca làm việc của tôi (kèm số lượt đã đặt / tổng số lượt)
//   GET /api/booking/bac-si/toi/lich-hen/lich?tuNgay=&denNgay=     lịch hẹn của tôi trong khoảng ngày (vẽ lên lịch)
// Bấm 1 lịch hẹn -> /doctor/consultation?id=<id lịch hẹn>; bấm 1 ca hoặc 1 ngày -> /doctor/consultation?ngay=&idLichLamViec=
//   POST /api/booking/bac-si/toi/yeu-cau-doi-lich                  { idLichLamViec, loaiYeuCau: XIN_NGHI | DOI_CA | NHUONG_CA,
//        lyDo, ngayMongMuon?, gioBatDauMongMuon?, gioKetThucMongMuon?, idPhongKhamMongMuon?, idBacSiNhan? } -> 201. 3 trường
//        ngày / giờ bắt buộc với DOI_CA; idBacSiNhan (bác sĩ khác, cùng chuyên khoa, đang công tác) bắt buộc với NHUONG_CA:
//        yêu cầu ở CHO_DONG_Y tới khi bác sĩ nhận đồng ý; bác sĩ nhận trùng giờ / vượt giờ làm tuần -> 409. Ca phải còn cách
//        giờ bắt đầu ít nhất 24 giờ (409 QUA_HAN_GUI_YEU_CAU); mỗi ca 1 yêu cầu còn chờ (409 CA_DA_CO_YEU_CAU_CHO_DUYET); ca
//        đã hủy / đã bắt đầu -> 409 CA_KHONG_SUA_DUOC
//   POST /api/booking/bac-si/toi/yeu-cau-doi-lich/xin-nghi-nhieu-ca  { cacIdLichLamViec (1-50), lyDo } -> 201 [yêu cầu]:
//        mỗi ca 1 yêu cầu XIN_NGHI chung maNhom; 1 ca không gửi được thì không lưu gì, lỗi nêu ca đó
//   GET  /api/booking/bac-si/toi/yeu-cau-doi-lich?trangThai=&trang=&kichThuoc=   yêu cầu tôi đã gửi, mới nhất trước
//        mỗi dòng: { id, loaiYeuCau, trangThai: CHO_DONG_Y | CHO_DUYET | DA_DUYET | TU_CHOI | DA_RUT | DONG_NGHIEP_TU_CHOI,
//        lyDo, bacSi, ca, ngayMongMuon, ..., phongKhamMongMuon, bacSiNhan, ghiChuBacSiNhan, ngayBacSiNhanPhanHoi, maNhom,
//        ghiChuXuLy, ngayGui, ngayXuLy }
//   POST /api/booking/bac-si/toi/yeu-cau-doi-lich/{id}/rut          rút yêu cầu còn chờ -> DA_RUT
//   POST /api/booking/bac-si/toi/yeu-cau-doi-lich/nhom/{maNhom}/rut  rút mọi yêu cầu còn chờ của nhóm xin nghỉ nhiều ca
//   GET  /api/booking/bac-si/toi/yeu-cau-nhuong-ca?trangThai=        yêu cầu nhường ca gửi cho tôi (CHO_DONG_Y = chờ tôi)
//   POST /api/booking/bac-si/toi/yeu-cau-nhuong-ca/{id}/dong-y       đồng ý nhận ca -> CHO_DUYET (chờ quản trị viên duyệt)
//   POST /api/booking/bac-si/toi/yeu-cau-nhuong-ca/{id}/tu-choi      { ghiChu? } -> DONG_NGHIEP_TU_CHOI
//   GET  /api/catalog/bac-si/toi/ho-so, /api/catalog/bac-si?idChuyenKhoa=   đồng nghiệp cùng chuyên khoa (chọn người nhận ca)
//   GET  /api/catalog/phong-kham?idChuyenKhoa=                      phòng khám đang hoạt động (chọn phòng mong muốn)
//   GET  /api/booking/bac-si/toi/lich-lam-viec/pdf?tuNgay=&denNgay=  bản in PDF lịch làm việc của tôi (tối đa 42 ngày, ca
//        còn hoạt động; chữ theo Accept-Language). Link trong email "gửi lịch" mở trang này kèm ?tuNgay=&denNgay=
// Cần internet: thư viện lịch FullCalendar tải từ CDN.
(() => {
    function mauLichBacSi(lich) {
        if (KHONG_CON_KHAM.includes(lich.trangThai)) return MAU.daHuy;
        if (lich.trangThai === 'DA_HOAN_THANH') return MAU.daKham;
        return lich.doiChieu && lich.doiChieu.canDoiChieu ? MAU.doiChieu : MAU.cuaToi;
    }

    async function nguonCa(info, xong, hong) {
        const { status, json } = await lay(`/api/booking/bac-si/toi/lich-lam-viec?${khoangNgay(info)}`, true);
        if (status !== 200) {
            bao('bs-lich-trang-thai', thongDiepLoi(json, t('Không tải được lịch làm việc.', 'Could not load the work schedule.')), 'loi');
            hong(new Error('HTTP ' + status));
            return;
        }
        xong(json.duLieu.map((ca) => ({
            title: `${t('Ca', 'Shift')} ${ca.gioBatDau.slice(0, 5)}–${ca.gioKetThuc.slice(0, 5)} · ${ca.phongKham.tenPhong} · ${ca.soLuotDaDat}/${ca.tongSoLuot}`
                + (ca.trangThai === 'DA_HUY' ? t(' (đã hủy)', ' (cancelled)') : ''),
            start: `${ca.ngay}T${ca.gioBatDau}`,
            end: `${ca.ngay}T${ca.gioKetThuc}`,
            color: ca.trangThai === 'DA_HUY' ? MAU.daHuy : MAU.ca,
            display: 'block',
            extendedProps: { ca },
        })));
    }

    async function nguonLichHen(info, xong, hong) {
        const { status, json } = await lay(`/api/booking/bac-si/toi/lich-hen/lich?${khoangNgay(info)}`, true);
        if (status !== 200) {
            hong(new Error('HTTP ' + status));
            return;
        }
        xong(json.duLieu.map((lich) => ({
            title: `${t('STT', 'No.')} ${lich.soThuTu} · ${lich.benhNhan.hoTen}`,
            start: lich.gioKhamDuKien,
            color: mauLichBacSi(lich),
            extendedProps: { lich },
        })));
    }

    dangKy({
        duongDan: '/doctor/work-schedule',
        vaiTro: 'BAC_SI',
        tieuDe: cap('Lịch làm việc', 'Work Schedule'),
        async ve(khung) {
            khuon(khung, `
                <section>
                    <h2>${t('Lịch làm việc và lịch hẹn của tôi', 'My work schedule and appointments')}</h2>
                    <p class="chu-thich"><span><i style="background:#57606a"></i>${t('Ca làm việc (đã đặt / tổng số lượt)', 'Shift (booked / total visit slots)')}</span><span><i style="background:#1f6feb"></i>${t('Lịch hẹn chờ khám', 'Appointment waiting for the visit')}</span>
                        <span><i style="background:#1a7f37"></i>${t('Đã khám', 'Visit done')}</span><span><i style="background:#cf222e"></i>${t('Cần đối chiếu giấy tờ', 'Identity check needed')}</span><span><i style="background:#8c959f"></i>${t('Đã hủy', 'Cancelled')}</span></p>
                    <div id="lich-bs"></div>
                    <form id="form-pdf-lich" class="hai-cot">
                        <div><label>${t('Từ ngày', 'From date')}</label><input name="tuNgay" type="date" required></div>
                        <div><label>${t('Đến ngày (tối đa 42 ngày)', 'To date (up to 42 days)')}</label><input name="denNgay" type="date" required></div>
                        <button>${t('Tải bản in (PDF)', 'Download printable copy (PDF)')}</button>
                    </form>
                    <p id="bs-lich-trang-thai" class="ghi-chu">${t('Bấm vào 1 ngày hoặc 1 ca để xem danh sách bệnh nhân; bấm vào 1 lịch hẹn để mở hồ sơ khám.',
                        'Click a day or a shift to see the patient list; click an appointment to open the medical record.')}</p>
                </section>`);
            khuon(khung, `
                <section>
                    <h2>Request a shift change / leave</h2>
                    <p class="ghi-chu">${t(`Gửi trước giờ bắt đầu ca ít nhất 24 giờ. Quản trị viên duyệt thì ca bị hủy (xin nghỉ) hoặc được
                        chuyển sang ngày / giờ / phòng mong muốn (đổi ca); bệnh nhân đã đặt trong ca được báo để đổi lịch.`,
                        `Send it at least 24 hours before the shift starts. When the administrator approves, the shift is cancelled (leave) or
                        moved to the requested day / time / room (shift change); patients booked in the shift are told to reschedule.`)}</p>
                    <form id="form-yeu-cau-ca" class="hai-cot">
                        <div><label>${t('Ca của tôi (42 ngày tới)', 'My shifts (next 42 days)')}</label><select name="idLichLamViec" id="bs-yc-ca" required></select></div>
                        <div><label>${t('Loại', 'Type')}</label><select name="loaiYeuCau" id="bs-yc-loai"><option value="XIN_NGHI">${t('Xin nghỉ', 'Leave')}</option><option value="DOI_CA">${t('Đổi ca', 'Shift change')}</option><option value="NHUONG_CA">${t('Nhường ca cho đồng nghiệp', 'Hand over to a colleague')}</option></select></div>
                        <div class="bs-yc-nhuong an"><label>${t('Đồng nghiệp nhận ca (cùng chuyên khoa)', 'Colleague taking the shift (same specialty)')}</label><select name="idBacSiNhan" id="bs-yc-nhan"></select></div>
                        <div class="ca-dong"><label>${t('Lý do', 'Reason')}</label><input name="lyDo" required maxlength="1000"></div>
                        <div class="bs-yc-doi an"><label>${t('Ngày mong muốn', 'Requested date')}</label><input name="ngayMongMuon" type="date"></div>
                        <div class="bs-yc-doi an"><label>${t('Giờ bắt đầu', 'Start time')}</label><input name="gioBatDauMongMuon" type="time"></div>
                        <div class="bs-yc-doi an"><label>${t('Giờ kết thúc', 'End time')}</label><input name="gioKetThucMongMuon" type="time"></div>
                        <div class="bs-yc-doi an"><label>${t('Phòng mong muốn', 'Requested room')}</label><select name="idPhongKhamMongMuon" id="bs-yc-phong"><option value="">${t('Giữ phòng hiện tại', 'Keep the current room')}</option></select></div>
                        <button>${t('Gửi yêu cầu', 'Send request')}</button>
                    </form>
                    <p id="bs-yc-trang-thai" class="ghi-chu"></p>
                    <h3>${t('Xin nghỉ nhiều ca', 'Leave for several shifts')}</h3>
                    <p class="ghi-chu">${t('Chọn các ca (vd cả 1 tuần): mỗi ca thành 1 yêu cầu xin nghỉ, quản trị viên duyệt từng ca hoặc cả nhóm.',
                        'Tick the shifts (e.g. a whole week): each becomes a leave request; the administrator approves them one by one or as a group.')}</p>
                    <form id="form-nghi-nhieu-ca">
                        <div id="bs-nghi-ca" class="ds-chon"></div>
                        <div><label>${t('Lý do', 'Reason')}</label><input name="lyDo" required maxlength="1000"></div>
                        <button>${t('Gửi xin nghỉ các ca đã chọn', 'Request leave for the ticked shifts')}</button>
                    </form>
                    <p id="bs-nghi-trang-thai" class="ghi-chu"></p>
                    <h3>${t('Yêu cầu đã gửi', 'Sent requests')}</h3>
                    <div id="bs-yc-ds" class="bang"></div>
                    <h3>${t('Yêu cầu nhường ca gửi tôi', 'Shifts colleagues want to hand me')}</h3>
                    <p id="bs-nhan-trang-thai" class="ghi-chu"></p>
                    <div id="bs-nhan-ds" class="bang"></div>
                </section>`);

            const YC = '/api/booking/bac-si/toi/yeu-cau-doi-lich';
            const moTaCa = (ca) => `${thuCuaNgay(ca.ngay)} ${ngayVn(ca.ngay)} ${ca.gioBatDau.slice(0, 5)}–${ca.gioKetThuc.slice(0, 5)} · ${tenPhong(ca.phongKham)}`;
            async function taiCaCuaToi() {
                const { status, json } = await lay(`/api/booking/bac-si/toi/lich-lam-viec?tuNgay=${congNgay(0)}&denNgay=${congNgay(41)}`, true);
                const o = $('bs-yc-ca');
                if (status !== 200 || !o) return;
                const conHoatDong = json.duLieu.filter((ca) => ca.trangThai === 'HOAT_DONG');
                o.replaceChildren(...conHoatDong.map((ca) => {
                    const lua = the('option', `${moTaCa(ca)} · ${t('đã đặt', 'booked')} ${ca.soLuotDaDat}/${ca.tongSoLuot}`);
                    lua.value = ca.idLichLamViec;
                    return lua;
                }));
                // Danh sách ô chọn cho "xin nghỉ nhiều ca"
                $('bs-nghi-ca').replaceChildren(...conHoatDong.map((ca) => {
                    const nhanO = the('label', null);
                    const o = the('input');
                    o.type = 'checkbox';
                    o.value = ca.idLichLamViec;
                    nhanO.append(o, ' ' + moTaCa(ca));
                    const dongCa = the('div');
                    dongCa.append(nhanO);
                    return dongCa;
                }));
            }
            // Đồng nghiệp cùng chuyên khoa (trừ tôi) để chọn người nhận ca
            async function taiDongNghiep() {
                const hoSo = await lay('/api/catalog/bac-si/toi/ho-so', true);
                if (hoSo.status !== 200 || !$('bs-yc-nhan')) return;
                const { status, json } = await lay(`/api/catalog/bac-si?idChuyenKhoa=${hoSo.json.duLieu.idChuyenKhoa}&kichThuoc=100`, true);
                if (status !== 200) return;
                $('bs-yc-nhan').replaceChildren(...json.duLieu.noiDung.filter((bs) => bs.id !== hoSo.json.duLieu.id).map((bs) => {
                    const lua = the('option', tenBacSi(bs));
                    lua.value = bs.id;
                    return lua;
                }));
            }
            const baoKetQua = (dich, kq, tot, loi) => bao(dich, kq.status === 200 || kq.status === 201 ? tot : thongDiepLoi(kq.json, loi),
                kq.status === 200 || kq.status === 201 ? 'tot' : 'loi');
            async function taiGuiChoToi() {
                const { status, json } = await lay('/api/booking/bac-si/toi/yeu-cau-nhuong-ca?kichThuoc=50', true);
                const ds = $('bs-nhan-ds');
                if (!ds || status !== 200) return;
                if (!json.duLieu.noiDung.length) {
                    ds.replaceChildren(ghiChu(t('Chưa có đồng nghiệp nào nhường ca cho bạn.', 'No colleague has offered you a shift.')));
                    return;
                }
                const phanHoi = async (yc, hanhDong) => {
                    let than;
                    if (hanhDong === 'tu-choi') {
                        const ghiChuNhan = prompt(t('Từ chối nhận ca. Ghi chú (không bắt buộc):', 'Decline the shift. Note (optional):'));
                        if (ghiChuNhan === null) return;
                        than = { ghiChu: ghiChuNhan.trim() || null };
                    }
                    const kq = await goi(`/api/booking/bac-si/toi/yeu-cau-nhuong-ca/${yc.id}/${hanhDong}`, than);
                    baoKetQua('bs-nhan-trang-thai', kq, hanhDong === 'dong-y' ? t('Đã đồng ý, chờ quản trị viên duyệt.', 'Accepted, waiting for the administrator.')
                        : t('Đã từ chối nhận ca.', 'Shift declined.'), t('Không xử lý được.', 'Could not process it.'));
                    taiGuiChoToi();
                };
                ds.replaceChildren(bang([t('Gửi lúc', 'Sent at'), t('Bác sĩ nhường', 'From'), t('Ca', 'Shift'), t('Lý do', 'Reason'), t('Trạng thái', 'Status'), t('Thao tác', 'Actions')],
                    json.duLieu.noiDung.map((yc) => dong([
                        ngayGioVn(yc.ngayGui), tenBacSi(yc.bacSi), moTaCa(yc.ca), yc.lyDo, TRANG_THAI_YEU_CAU[yc.trangThai],
                        yc.trangThai === 'CHO_DONG_Y' ? [nut(t('Đồng ý nhận', 'Accept'), () => phanHoi(yc, 'dong-y')), nut(t('Từ chối', 'Decline'), () => phanHoi(yc, 'tu-choi'), 'nguy-hiem nho')] : null,
                    ]))));
            }
            async function taiYeuCau() {
                const { status, json } = await lay(YC + '?kichThuoc=50', true);
                const ds = $('bs-yc-ds');
                if (!ds) return;
                if (status !== 200) {
                    ds.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được yêu cầu.', 'Could not load the requests.')), 'ghi-chu loi'));
                    return;
                }
                if (!json.duLieu.noiDung.length) {
                    ds.replaceChildren(ghiChu(t('Chưa gửi yêu cầu nào.', 'No requests sent yet.')));
                    return;
                }
                const dangCho = (yc) => yc.trangThai === 'CHO_DONG_Y' || yc.trangThai === 'CHO_DUYET';
                const rut = async (duongDan) => {
                    const kq = await goi(duongDan);
                    baoKetQua('bs-yc-trang-thai', kq, t('Đã rút yêu cầu.', 'Request withdrawn.'), t('Không rút được.', 'Could not withdraw the request.'));
                    taiYeuCau();
                };
                ds.replaceChildren(bang([t('Gửi lúc', 'Sent at'), t('Loại', 'Type'), t('Ca', 'Shift'), t('Mong muốn / người nhận', 'Requested / colleague'), t('Lý do', 'Reason'),
                    t('Trạng thái', 'Status'), t('Ghi chú', 'Note'), t('Thao tác', 'Actions')], json.duLieu.noiDung.map((yc) => dong([
                    ngayGioVn(yc.ngayGui), LOAI_YEU_CAU[yc.loaiYeuCau] + (yc.maNhom ? t(' (nhóm ', ' (group ') + yc.maNhom.slice(0, 8) + ')' : ''),
                    moTaCa(yc.ca) + (yc.ca.trangThai === 'DA_HUY' ? t(' (đã hủy)', ' (cancelled)') : ''),
                    yc.bacSiNhan ? tenBacSi(yc.bacSiNhan)
                        : yc.ngayMongMuon ? `${ngayVn(yc.ngayMongMuon)} ${yc.gioBatDauMongMuon.slice(0, 5)}–${yc.gioKetThucMongMuon.slice(0, 5)}`
                        + (yc.phongKhamMongMuon ? ' · ' + tenPhong(yc.phongKhamMongMuon) : '') : null,
                    yc.lyDo, TRANG_THAI_YEU_CAU[yc.trangThai], yc.ghiChuXuLy || yc.ghiChuBacSiNhan,
                    dangCho(yc) ? [nut(t('Rút', 'Withdraw'), () => rut(`${YC}/${yc.id}/rut`), 'phu nho'),
                        yc.maNhom ? nut(t('Rút cả nhóm', 'Withdraw the group'), () => rut(`${YC}/nhom/${yc.maNhom}/rut`), 'phu nho') : null] : null,
                ]))));
            }
            $('bs-yc-loai').addEventListener('change', (e) => {
                document.querySelectorAll('.bs-yc-doi').forEach((o) => o.classList.toggle('an', e.target.value !== 'DOI_CA'));
                document.querySelectorAll('.bs-yc-nhuong').forEach((o) => o.classList.toggle('an', e.target.value !== 'NHUONG_CA'));
            });
            khiGui('form-nghi-nhieu-ca', async (v) => {
                const cacId = [...document.querySelectorAll('#bs-nghi-ca input:checked')].map((o) => Number(o.value));
                if (!cacId.length) return bao('bs-nghi-trang-thai', t('Chọn ít nhất 1 ca.', 'Tick at least one shift.'), 'loi');
                const kq = await goi(`${YC}/xin-nghi-nhieu-ca`, { cacIdLichLamViec: cacId, lyDo: v.lyDo });
                baoKetQua('bs-nghi-trang-thai', kq, t(`Đã gửi xin nghỉ ${cacId.length} ca, chờ quản trị viên duyệt.`, `Leave requested for ${cacId.length} shift(s), waiting for approval.`),
                    t('Không gửi được.', 'Could not send the request.'));
                if (kq.status === 201) taiYeuCau();
            });
            // Bản in PDF: mặc định 4 tuần tới, hoặc khoảng ngày của link trong email gửi lịch (?tuNgay=&denNgay=)
            const formPdf = $('form-pdf-lich');
            const thamSoUrl = new URLSearchParams(location.search);
            formPdf.elements.tuNgay.value = thamSoUrl.get('tuNgay') || congNgay(0);
            formPdf.elements.denNgay.value = thamSoUrl.get('denNgay') || congNgay(27);
            khiGui('form-pdf-lich', async (v) => {
                const kq = await taiTep(`/api/booking/bac-si/toi/lich-lam-viec/pdf?tuNgay=${v.tuNgay}&denNgay=${v.denNgay}`, `lich-lam-viec_${v.tuNgay}_${v.denNgay}.pdf`);
                bao('bs-lich-trang-thai', kq.status === 200 ? t('Đã tải bản in.', 'Printable copy downloaded.') : thongDiepLoi(kq.json, t('Không tải được bản in.', 'Could not download the printable copy.')), kq.status === 200 ? 'tot' : 'loi');
            });
            khiGui('form-yeu-cau-ca', async (v, form) => {
                const than = thanForm(form, v.loaiYeuCau === 'DOI_CA'
                    ? ['idLichLamViec', 'loaiYeuCau', 'lyDo', 'ngayMongMuon', 'gioBatDauMongMuon', 'gioKetThucMongMuon', 'idPhongKhamMongMuon']
                    : v.loaiYeuCau === 'NHUONG_CA' ? ['idLichLamViec', 'loaiYeuCau', 'lyDo', 'idBacSiNhan'] : ['idLichLamViec', 'loaiYeuCau', 'lyDo']);
                const { status, json } = await goi(YC, than);
                bao('bs-yc-trang-thai', status === 201 ? (v.loaiYeuCau === 'NHUONG_CA'
                    ? t('Đã gửi, chờ đồng nghiệp đồng ý rồi quản trị viên duyệt.', 'Sent; waiting for the colleague to accept, then for the administrator.')
                    : t('Đã gửi yêu cầu, chờ quản trị viên duyệt.', 'Request sent, waiting for the administrator to approve.'))
                    : thongDiepLoi(json, t('Không gửi được.', 'Could not send the request.')), status === 201 ? 'tot' : 'loi');
                if (status === 201) taiYeuCau();
            });
            lay('/api/catalog/phong-kham', true).then(({ status, json }) => {
                if (status !== 200 || !$('bs-yc-phong')) return;
                for (const p of json.duLieu) {
                    const lua = the('option', `${tenPhong(p)} · ${p.tenChuyenKhoa}`);
                    lua.value = p.id;
                    $('bs-yc-phong').append(lua);
                }
            });
            taiCaCuaToi();
            taiYeuCau();
            taiDongNghiep();
            taiGuiChoToi();

            const xemNgay = (ngay, idLichLamViec) => diToi(`/doctor/consultation?ngay=${ngay}` + (idLichLamViec ? `&idLichLamViec=${idLichLamViec}` : ''));
            const lich = await taoLich($('lich-bs'), [nguonCa, nguonLichHen], {
                // UI có 2 chế độ ngày / tuần; giữ thêm tháng để dễ thấy dữ liệu mẫu
                initialView: 'timeGridWeek',
                headerToolbar: { left: 'prev,next today', center: 'title', right: 'timeGridDay,timeGridWeek,dayGridMonth' },
                eventClick: (info) => {
                    const { ca, lich: lichHen } = info.event.extendedProps;
                    if (lichHen) diToi('/doctor/consultation?id=' + lichHen.id);
                    else if (ca) xemNgay(ca.ngay, ca.idLichLamViec);
                },
                dateClick: (info) => xemNgay(info.dateStr.slice(0, 10)),
            });
            return () => lich && lich.destroy();
        },
    });
})();
