// Màn hình UI: Work Schedule (Doctor) — lịch làm việc theo ngày / tuần và nút "Request a shift change / leave"
// Đường dẫn:  /doctor/work-schedule
// Vai trò:    BAC_SI
// API:
//   GET /api/booking/bac-si/toi/lich-lam-viec?tuNgay=&denNgay=     các ca làm việc của tôi (kèm số lượt đã đặt / tổng số lượt)
//   GET /api/booking/bac-si/toi/lich-hen/lich?tuNgay=&denNgay=     lịch hẹn của tôi trong khoảng ngày (vẽ lên lịch)
// Bấm 1 lịch hẹn -> /doctor/consultation?id=<id lịch hẹn>; bấm 1 ca hoặc 1 ngày -> /doctor/consultation?ngay=&idLichLamViec=
//   POST /api/booking/bac-si/toi/yeu-cau-doi-lich                  { idLichLamViec, loaiYeuCau: XIN_NGHI | DOI_CA, lyDo,
//        ngayMongMuon?, gioBatDauMongMuon?, gioKetThucMongMuon?, idPhongKhamMongMuon? } -> 201. 3 trường ngày / giờ bắt buộc
//        với DOI_CA. Ca phải còn cách giờ bắt đầu ít nhất 24 giờ (409 QUA_HAN_GUI_YEU_CAU); mỗi ca 1 yêu cầu chờ duyệt
//        (409 CA_DA_CO_YEU_CAU_CHO_DUYET); ca đã hủy / đã bắt đầu -> 409 CA_KHONG_SUA_DUOC
//   GET  /api/booking/bac-si/toi/yeu-cau-doi-lich?trangThai=&trang=&kichThuoc=   yêu cầu tôi đã gửi, mới nhất trước
//        mỗi dòng: { id, loaiYeuCau, trangThai: CHO_DUYET | DA_DUYET | TU_CHOI | DA_RUT, lyDo, ca, ngayMongMuon, ...,
//        phongKhamMongMuon, ghiChuXuLy, ngayGui, ngayXuLy }
//   POST /api/booking/bac-si/toi/yeu-cau-doi-lich/{id}/rut          rút yêu cầu còn chờ duyệt -> DA_RUT
//   GET  /api/catalog/phong-kham?idChuyenKhoa=                      phòng khám đang hoạt động (chọn phòng mong muốn)
// Cần internet: thư viện lịch FullCalendar tải từ CDN.
(() => {
    function mauLichBacSi(lich) {
        if (DA_HUY.includes(lich.trangThai)) return MAU.daHuy;
        if (lich.trangThai === 'DA_HOAN_THANH') return MAU.daKham;
        return lich.doiChieu && lich.doiChieu.canDoiChieu ? MAU.doiChieu : MAU.cuaToi;
    }

    async function nguonCa(info, xong, hong) {
        const { status, json } = await lay(`/api/booking/bac-si/toi/lich-lam-viec?${khoangNgay(info)}`, true);
        if (status !== 200) {
            bao('bs-lich-trang-thai', thongDiepLoi(json, 'Không tải được lịch làm việc.'), 'loi');
            hong(new Error('HTTP ' + status));
            return;
        }
        xong(json.duLieu.map((ca) => ({
            title: `Ca ${ca.gioBatDau.slice(0, 5)}–${ca.gioKetThuc.slice(0, 5)} · ${ca.phongKham.tenPhong} · ${ca.soLuotDaDat}/${ca.tongSoLuot}`
                + (ca.trangThai === 'DA_HUY' ? ' (đã hủy)' : ''),
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
            title: `STT ${lich.soThuTu} · ${lich.benhNhan.hoTen}`,
            start: lich.gioKhamDuKien,
            color: mauLichBacSi(lich),
            extendedProps: { lich },
        })));
    }

    dangKy({
        duongDan: '/doctor/work-schedule',
        vaiTro: 'BAC_SI',
        tieuDe: 'Work Schedule',
        async ve(khung) {
            khuon(khung, `
                <section>
                    <h2>Lịch làm việc và lịch hẹn của tôi</h2>
                    <p class="chu-thich"><span><i style="background:#57606a"></i>Ca làm việc (đã đặt / tổng số lượt)</span><span><i style="background:#1f6feb"></i>Lịch hẹn chờ khám</span>
                        <span><i style="background:#1a7f37"></i>Đã khám</span><span><i style="background:#cf222e"></i>Cần đối chiếu giấy tờ</span><span><i style="background:#8c959f"></i>Đã hủy</span></p>
                    <div id="lich-bs"></div>
                    <p id="bs-lich-trang-thai" class="ghi-chu">Bấm vào 1 ngày hoặc 1 ca để xem danh sách bệnh nhân; bấm vào 1 lịch hẹn để mở hồ sơ khám.</p>
                </section>`);
            khuon(khung, `
                <section>
                    <h2>Request a shift change / leave</h2>
                    <p class="ghi-chu">Gửi trước giờ bắt đầu ca ít nhất 24 giờ. Quản trị viên duyệt thì ca bị hủy (xin nghỉ) hoặc được
                        chuyển sang ngày / giờ / phòng mong muốn (đổi ca); bệnh nhân đã đặt trong ca được báo để đổi lịch.</p>
                    <form id="form-yeu-cau-ca" class="hai-cot">
                        <div><label>Ca của tôi (42 ngày tới)</label><select name="idLichLamViec" id="bs-yc-ca" required></select></div>
                        <div><label>Loại</label><select name="loaiYeuCau" id="bs-yc-loai"><option value="XIN_NGHI">Xin nghỉ</option><option value="DOI_CA">Đổi ca</option></select></div>
                        <div class="ca-dong"><label>Lý do</label><input name="lyDo" required maxlength="1000"></div>
                        <div class="bs-yc-doi an"><label>Ngày mong muốn</label><input name="ngayMongMuon" type="date"></div>
                        <div class="bs-yc-doi an"><label>Giờ bắt đầu</label><input name="gioBatDauMongMuon" type="time"></div>
                        <div class="bs-yc-doi an"><label>Giờ kết thúc</label><input name="gioKetThucMongMuon" type="time"></div>
                        <div class="bs-yc-doi an"><label>Phòng mong muốn</label><select name="idPhongKhamMongMuon" id="bs-yc-phong"><option value="">Giữ phòng hiện tại</option></select></div>
                        <button>Gửi yêu cầu</button>
                    </form>
                    <p id="bs-yc-trang-thai" class="ghi-chu"></p>
                    <h3>Yêu cầu đã gửi</h3>
                    <div id="bs-yc-ds" class="bang"></div>
                </section>`);

            const YC = '/api/booking/bac-si/toi/yeu-cau-doi-lich';
            const moTaCa = (ca) => `${thuCuaNgay(ca.ngay)} ${ngayVn(ca.ngay)} ${ca.gioBatDau.slice(0, 5)}–${ca.gioKetThuc.slice(0, 5)} · ${tenPhong(ca.phongKham)}`;
            async function taiCaCuaToi() {
                const { status, json } = await lay(`/api/booking/bac-si/toi/lich-lam-viec?tuNgay=${congNgay(0)}&denNgay=${congNgay(41)}`, true);
                const o = $('bs-yc-ca');
                if (status !== 200 || !o) return;
                o.replaceChildren(...json.duLieu.filter((ca) => ca.trangThai === 'HOAT_DONG').map((ca) => {
                    const lua = the('option', `${moTaCa(ca)} · đã đặt ${ca.soLuotDaDat}/${ca.tongSoLuot}`);
                    lua.value = ca.idLichLamViec;
                    return lua;
                }));
            }
            async function taiYeuCau() {
                const { status, json } = await lay(YC + '?kichThuoc=50', true);
                const ds = $('bs-yc-ds');
                if (!ds) return;
                if (status !== 200) {
                    ds.replaceChildren(the('p', thongDiepLoi(json, 'Không tải được yêu cầu.'), 'ghi-chu loi'));
                    return;
                }
                if (!json.duLieu.noiDung.length) {
                    ds.replaceChildren(ghiChu('Chưa gửi yêu cầu nào.'));
                    return;
                }
                ds.replaceChildren(bang(['Gửi lúc', 'Loại', 'Ca', 'Mong muốn', 'Lý do', 'Trạng thái', 'Ghi chú của quản trị viên', 'Thao tác'], json.duLieu.noiDung.map((yc) => dong([
                    ngayGioVn(yc.ngayGui), LOAI_YEU_CAU[yc.loaiYeuCau], moTaCa(yc.ca) + (yc.ca.trangThai === 'DA_HUY' ? ' (đã hủy)' : ''),
                    yc.ngayMongMuon ? `${ngayVn(yc.ngayMongMuon)} ${yc.gioBatDauMongMuon.slice(0, 5)}–${yc.gioKetThucMongMuon.slice(0, 5)}`
                        + (yc.phongKhamMongMuon ? ' · ' + tenPhong(yc.phongKhamMongMuon) : '') : null,
                    yc.lyDo, TRANG_THAI_YEU_CAU[yc.trangThai], yc.ghiChuXuLy,
                    yc.trangThai === 'CHO_DUYET' ? nut('Rút', async () => {
                        const kq = await goi(`${YC}/${yc.id}/rut`);
                        bao('bs-yc-trang-thai', kq.status === 200 ? 'Đã rút yêu cầu.' : thongDiepLoi(kq.json, 'Không rút được.'), kq.status === 200 ? 'tot' : 'loi');
                        taiYeuCau();
                    }, 'phu nho') : null,
                ]))));
            }
            $('bs-yc-loai').addEventListener('change', (e) => {
                document.querySelectorAll('.bs-yc-doi').forEach((o) => o.classList.toggle('an', e.target.value !== 'DOI_CA'));
            });
            khiGui('form-yeu-cau-ca', async (v, form) => {
                const than = thanForm(form, v.loaiYeuCau === 'DOI_CA'
                    ? ['idLichLamViec', 'loaiYeuCau', 'lyDo', 'ngayMongMuon', 'gioBatDauMongMuon', 'gioKetThucMongMuon', 'idPhongKhamMongMuon']
                    : ['idLichLamViec', 'loaiYeuCau', 'lyDo']);
                const { status, json } = await goi(YC, than);
                bao('bs-yc-trang-thai', status === 201 ? 'Đã gửi yêu cầu, chờ quản trị viên duyệt.' : thongDiepLoi(json, 'Không gửi được.'), status === 201 ? 'tot' : 'loi');
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
