// Màn hình UI: Schedule Management (Admin) — lịch làm việc toàn viện
// Đường dẫn:  /admin/schedule-management
// Vai trò:    QUAN_TRI_VIEN
// API:
//   GET  /api/booking/quan-tri/lich-lam-viec?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=&idPhongKham=   các ca trong khoảng ngày
//        (tối đa 42 ngày, kể cả ca đã huỷ) kèm tongSoLuot / soLuotDaDat / soLuotConTrong
//   GET  /api/booking/quan-tri/lich-lam-viec/{id}/lich-hen     lịch hẹn của 1 ca (mọi trạng thái) kèm thông tin bệnh nhân
//   GET  /api/booking/quan-tri/lich-hen/tra-cuu?ma=            tra 1 lịch hẹn theo mã tra cứu (ECL-…) hoặc mã phiếu khám
//   POST /api/booking/quan-tri/lich-hen/{id}/da-doi-chieu      bỏ đánh dấu "cần đối chiếu" sau khi đã đối chiếu giấy tờ
//   GET  /api/catalog/chuyen-khoa?kichThuoc=100, GET /api/catalog/bac-si?kichThuoc=100   chỉ để đổ 2 ô lọc
//   POST /api/booking/quan-tri/lich-lam-viec                   xếp ca { idBacSi, idPhongKham, ngay, gioBatDau, gioKetThuc,
//        soLuotToiDaMoiGio (N), thoiLuongLuotPhut (t), lapLai?: { cacThu: [1..7] (1 = thứ Hai), denNgay } } -> 201
//        { daTao: [ca], boQua: [{ ngay, lyDo }] }. N x t <= 60; phòng phải thuộc chuyên khoa của bác sĩ; trùng giờ với ca
//        khác của bác sĩ / phòng -> 409 TRUNG_LICH_LAM_VIEC (có lapLai thì ngày trùng nằm trong boQua)
//   PUT  /api/booking/quan-tri/lich-lam-viec/{id}              sửa { idPhongKham, gioBatDau, gioKetThuc, soLuotToiDaMoiGio,
//        thoiLuongLuotPhut } của ca chưa bắt đầu. Đổi phòng luôn được; đổi giờ / sức chứa làm mất lượt đã có người đặt
//        -> 409 CA_CON_LICH_HEN; ca đã bắt đầu / đã hủy -> 409 CA_KHONG_SUA_DUOC
//   POST /api/booking/quan-tri/lich-lam-viec/{id}/huy          { lyDo } -> { ca, soLichHenCanDoi }: lịch hẹn còn hiệu lực
//        của ca được giữ và đánh dấu canDoiLich, bệnh nhân được báo
//   GET  /api/booking/quan-tri/lich-lam-viec/can-doi-lich      lịch hẹn đang chờ bệnh nhân đổi lịch (TrangDuLieu)
//   GET  /api/booking/quan-tri/yeu-cau-doi-lich?trangThai=&tatCa=&trang=&kichThuoc=   yêu cầu đổi ca / xin nghỉ (mặc định
//        CHO_DUYET), mỗi dòng kèm ca và ca.soLuotDaDat
//   POST /api/booking/quan-tri/yeu-cau-doi-lich/{id}/duyet     { ghiChu? }: xin nghỉ -> hủy ca; đổi ca -> hủy ca cũ + xếp ca
//        mới (chỉ khác phòng thì đổi phòng tại chỗ); ca mới trùng giờ -> 409 TRUNG_LICH_LAM_VIEC, yêu cầu vẫn chờ duyệt
//        nhường ca -> ca và lịch hẹn chuyển sang bác sĩ nhận (lịch hẹn mang daDoiBacSi, bệnh nhân được báo + email); bác sĩ
//        nhận trùng giờ / vượt giờ làm tuần -> 409; nhường ca còn CHO_DONG_Y (chưa được đồng ý) -> 409 YEU_CAU_CHO_DONG_NGHIEP
//   POST /api/booking/quan-tri/yeu-cau-doi-lich/{id}/tu-choi   { ghiChu } bắt buộc
//   POST /api/booking/quan-tri/yeu-cau-doi-lich/nhom/{maNhom}/duyet     { ghiChu? } duyệt cả nhóm xin nghỉ nhiều ca ->
//        { daXuLy: [yêu cầu], boQua: [{ idYeuCau, lyDo }] } (ca đã bắt đầu bị bỏ qua); bác sĩ được báo 1 lần
//   POST /api/booking/quan-tri/yeu-cau-doi-lich/nhom/{maNhom}/tu-choi   { ghiChu } bắt buộc, từ chối cả nhóm
//   GET  /api/catalog/phong-kham                               phòng khám đang hoạt động (chọn khi xếp / sửa ca)
//   GET  /api/booking/quan-tri/lich-lam-viec/gio-lam-theo-tuan?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=   giờ làm theo tuần
//        thứ Hai - Chủ nhật (SCHED-05): { gioiHanPhut, bacSi: [{ bacSi, tenChuyenKhoa, cacTuan: [{ tuNgay, denNgay, soCa,
//        soPhut, soPhutConLai }] }] }. Xếp ca / sửa ca dài thêm / duyệt đổi ca làm tuần vượt giới hạn -> 409 VUOT_GIO_LAM_TUAN
//        (xếp lặp lại thì ngày vượt nằm trong boQua)
//   GET  /api/booking/quan-tri/lich-lam-viec/{id}/lich-su      lịch sử xếp / sửa / hủy của 1 ca: [{ hanhDong, nguon,
//        thoiDiem, quanTri, idYeuCau, lyDo, truoc, sau, cacTruongDoi }]
//   POST /api/booking/quan-tri/lich-lam-viec/sao-chep/xem-truoc   { tuNgay, denNgay (khoảng nguồn, tối đa 42 ngày), ngayDich
//        (sau denNgay, cùng thứ với tuNgay), soLan (1-12), idChuyenKhoa?, cacIdBacSi? } -> 200 { daLuu: false, soCaNguon,
//        taoDuoc: [{ idCaNguon, idCaMoi, bacSi, phongKham, ngay, gioBatDau, gioKetThuc, lyDo }], boQua: [... lyDo] }. Chạy
//        đúng như khi sao chép rồi hoàn tác; lần dán sau cách lần trước số tuần đủ phủ khoảng nguồn
//   POST /api/booking/quan-tri/lich-lam-viec/sao-chep          cùng body -> 201, daLuu: true, idCaMoi có giá trị; bản sao
//        vi phạm (ngày đã qua / quá xa, bác sĩ / phòng ngừng, trùng giờ, vượt giờ làm tuần) nằm trong boQua; không bản sao
//        nào tạo được -> 400 DU_LIEU_KHONG_HOP_LE
//   GET  /api/booking/quan-tri/lich-lam-viec/xuat-excel?tuNgay=&denNgay=&idChuyenKhoa=&idBacSi=&idPhongKham=   tệp .xlsx các
//        ca còn hoạt động (tối đa 42 ngày). Cột A-G: mã bác sĩ (BS0001), ngày, giờ bắt đầu, giờ kết thúc, mã phòng, N, t
//        (cùng bố cục với tệp nhập); cột sau G chỉ để xem
//   GET  /api/booking/quan-tri/lich-lam-viec/nhap-excel/mau    tệp mẫu: trang ca (chỉ tiêu đề), hướng dẫn, danh sách bác sĩ
//        và phòng (cột mã bác sĩ / mã phòng có danh sách chọn)
//   POST /api/booking/quan-tri/lich-lam-viec/nhap-excel/xem-truoc   multipart, phần "tep" (.xlsx, tối đa 2MB, 1000 dòng)
//        -> 200 { daLuu: false, soDong, hopLe: [{ dong, idCaMoi, bacSi, phongKham, ngay, gioBatDau, gioKetThuc }],
//        loi: [{ dong, cot (A-G, null = cả dòng), lyDo }] }. Không lưu gì
//   POST /api/booking/quan-tri/lich-lam-viec/nhap-excel        cùng phần "tep" -> 201, daLuu: true. Tất cả hoặc không: tệp
//        còn lỗi -> 400 DU_LIEU_KHONG_HOP_LE; tệp không phải .xlsx / rỗng / quá 1000 dòng -> 400; quá 2MB -> 413
//   GET  /api/booking/quan-tri/lich-lam-viec/pdf/bac-si/{idBacSi}?tuNgay=&denNgay=   bản in PDF lịch của 1 bác sĩ (tối đa 42
//        ngày, ca còn hoạt động; chữ theo Accept-Language), cùng tệp bác sĩ tự tải; không có bác sĩ -> 404
//   GET  /api/booking/quan-tri/lich-lam-viec/pdf/toan-vien?tuNgay=&denNgay=&idChuyenKhoa=   PDF lưới tuần toàn viện (A4
//        ngang): mỗi tuần thứ Hai - Chủ nhật chạm khoảng ngày là 1 trang, dòng = bác sĩ có ca, cột cuối = giờ trong tuần
//   POST /api/booking/quan-tri/lich-lam-viec/gui-lich           { tuNgay, denNgay (tối đa 42 ngày), idChuyenKhoa?,
//        cacIdBacSi? } -> 200 { soBacSi, soCa, cacBacSi: [{ bacSi, soCa, soPhut }] }: mỗi bác sĩ có ca còn hoạt động từ
//        hôm nay nhận 1 email (bảng các ca + link Lịch làm việc) và 1 thông báo; không có ca -> 400. Gửi lại = gửi thêm
(() => {
    const CA = '/api/booking/quan-tri/lich-lam-viec';
    const LICH_HEN = '/api/booking/quan-tri/lich-hen';

    // Bảng lịch hẹn (dùng cho cả "lịch hẹn của ca" và kết quả tra cứu theo mã)
    function bangLichHen(ds, taiLai) {
        return bang([t('STT', 'No.'), t('Giờ khám', 'Visit time'), t('Bệnh nhân', 'Patient'), t('Tuổi', 'Age'), t('SĐT', 'Phone'), t('Bác sĩ', 'Doctor'), t('Trạng thái', 'Status'), t('Mã tra cứu', 'Lookup code'), t('Thao tác', 'Actions')], ds.map((lich) => {
            const thaoTac = [lienKet(t('Hồ sơ bệnh nhân', 'Patient profile'), '/admin/patients?id=' + lich.idHoSoBenhNhan)];
            if (lich.doiChieu.canDoiChieu) {
                thaoTac.push(nut(t('Đã đối chiếu', 'Identity checked'), async () => {
                    const { status } = await goi(`${LICH_HEN}/${lich.id}/da-doi-chieu`);
                    if (status === 200) await taiLai();
                }));
            }
            return dong([
                lich.soThuTu, ngayGioVn(lich.gioKhamDuKien), lich.benhNhan.hoTen + (lich.doiChieu.canDoiChieu ? t(' ⚠ cần đối chiếu', ' ⚠ identity check needed') : ''),
                lich.benhNhan.tuoi, lich.benhNhan.soDienThoai, tenBacSi(lich.bacSi), TRANG_THAI[lich.trangThai] || lich.trangThai,
                lich.maTraCuu, thaoTac,
            ]);
        }));
    }

    async function xemLichHenCuaCa(id) {
        const { status, json } = await lay(`${CA}/${id}/lich-hen`);
        const khung = $('qt-lich-hen-ca');
        if (!khung) return;
        $('qt-khoi-lich-hen-ca').classList.remove('an');
        $('qt-tieu-de-ca').textContent = t(`Lịch hẹn của ca #${id}`, `Appointments of shift #${id}`);
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được lịch hẹn của ca.', 'Could not load the appointments of the shift.')), 'ghi-chu loi'));
        } else if (!json.duLieu.length) {
            khung.replaceChildren(ghiChu(t('Ca này chưa có lịch hẹn nào.', 'This shift has no appointments yet.')));
        } else {
            khung.replaceChildren(bangLichHen(json.duLieu, () => xemLichHenCuaCa(id)));
        }
        $('qt-khoi-lich-hen-ca').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    const YEU_CAU = '/api/booking/quan-tri/yeu-cau-doi-lich';
    const gioPhut = (g) => g.slice(0, 5);
    // 2880 -> "48h00"
    const soGio = (phut) => `${Math.floor(phut / 60)}h${String(phut % 60).padStart(2, '0')}`;
    const HANH_DONG_CA = { TAO: t('Xếp ca', 'Created'), SUA: t('Sửa', 'Edited'), HUY: t('Hủy', 'Cancelled') };
    const NGUON_THAY_DOI = {
        QUAN_TRI: t('Quản trị viên', 'Administrator'), DUYET_YEU_CAU: t('Duyệt yêu cầu', 'Approved request'),
        NGUNG_CONG_TAC: t('Bác sĩ ngừng công tác', 'Doctor deactivated'), DU_LIEU_MAU: t('Dữ liệu mẫu', 'Demo data'),
        DU_LIEU_CU: t('Trước khi có lịch sử', 'Before history tracking'), SAO_CHEP: t('Sao chép ca', 'Copied shifts'),
        NHAP_EXCEL: t('Nhập từ Excel', 'Excel import'),
    };
    const TEN_TRUONG = {
        bacSi: t('bác sĩ', 'doctor'), phongKham: t('phòng', 'room'), ngay: t('ngày', 'date'), gioBatDau: t('giờ bắt đầu', 'start'),
        gioKetThuc: t('giờ kết thúc', 'end'), soLuotToiDaMoiGio: 'N', thoiLuongLuotPhut: 't',
    };
    const giaTriTruong = (anh, truong) => ({
        bacSi: tenBacSi(anh.bacSi), phongKham: tenPhong(anh.phongKham), ngay: ngayVn(anh.ngay), gioBatDau: gioPhut(anh.gioBatDau),
        gioKetThuc: gioPhut(anh.gioKetThuc), soLuotToiDaMoiGio: anh.soLuotToiDaMoiGio, thoiLuongLuotPhut: anh.thoiLuongLuotPhut,
    })[truong];

    // Lịch sử xếp / sửa / hủy của 1 ca
    async function xemLichSuCa(id) {
        const { status, json } = await lay(`${CA}/${id}/lich-su`);
        const khung = $('qt-lich-su-ca');
        if (!khung) return;
        $('qt-khoi-lich-su-ca').classList.remove('an');
        $('qt-tieu-de-lich-su').textContent = t(`Lịch sử ca #${id}`, `History of shift #${id}`);
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được lịch sử ca.', 'Could not load the shift history.')), 'ghi-chu loi'));
        } else {
            khung.replaceChildren(bang([t('Lúc', 'At'), t('Thay đổi', 'Change'), t('Nguồn', 'Source'), t('Người làm', 'By'), t('Chi tiết', 'Details'), t('Lý do', 'Reason')],
                json.duLieu.map((ls) => dong([
                    ngayGioVn(ls.thoiDiem), HANH_DONG_CA[ls.hanhDong] || ls.hanhDong,
                    (NGUON_THAY_DOI[ls.nguon] || ls.nguon) + (ls.idYeuCau ? ` #${ls.idYeuCau}` : ''), ls.quanTri.hoTen,
                    ls.truoc
                        ? ls.cacTruongDoi.map((tr) => `${TEN_TRUONG[tr] || tr}: ${giaTriTruong(ls.truoc, tr)} → ${giaTriTruong(ls.sau, tr)}`).join('; ') || '—'
                        : `${tenBacSi(ls.sau.bacSi)}, ${moTaCa(ls.sau)}, N=${ls.sau.soLuotToiDaMoiGio}, t=${ls.sau.thoiLuongLuotPhut}`,
                    ls.lyDo,
                ]))));
        }
        $('qt-khoi-lich-su-ca').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    // Giờ làm theo tuần của các bác sĩ, theo bộ lọc của lịch toàn viện
    async function taiGioLam() {
        const khung = $('qt-gio-lam');
        if (!khung) return;
        const thamSo = new URLSearchParams(thanForm($('form-loc-ca'), ['tuNgay', 'denNgay', 'idChuyenKhoa', 'idBacSi']));
        const { status, json } = await lay(`${CA}/gio-lam-theo-tuan?${thamSo}`, true);
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được giờ làm.', 'Could not load the working hours.')), 'ghi-chu loi'));
            return;
        }
        const { gioiHanPhut, bacSi } = json.duLieu;
        if (!bacSi.length) {
            khung.replaceChildren(ghiChu(t('Không có bác sĩ nào khớp bộ lọc.', 'No doctors match the filter.')));
            return;
        }
        const cacTuan = bacSi[0].cacTuan;
        khung.replaceChildren(
            ghiChu(t(`Giới hạn ${soGio(gioiHanPhut)} mỗi tuần. ⚠ = vượt giới hạn.`, `Limit ${soGio(gioiHanPhut)} per week. ⚠ = over the limit.`)),
            bang([t('Bác sĩ', 'Doctor'), t('Chuyên khoa', 'Specialty'), ...cacTuan.map((tuan) => `${ngayVn(tuan.tuNgay)} – ${ngayVn(tuan.denNgay)}`)],
                bacSi.map((dongBs) => dong([tenBacSi(dongBs.bacSi), dongBs.tenChuyenKhoa, ...dongBs.cacTuan.map((tuan) =>
                    (tuan.soPhut > gioiHanPhut ? '⚠ ' : '') + t(`${soGio(tuan.soPhut)} (${tuan.soCa} ca)`, `${soGio(tuan.soPhut)} (${tuan.soCa} shifts)`))]))));
    }

    // Sao chép ca: body từ form (bác sĩ chọn nhiều, bỏ trống = tất cả)
    function thanSaoChep(form) {
        const than = thanForm(form, ['tuNgay', 'denNgay', 'ngayDich', 'soLan', 'idChuyenKhoa']);
        const cacIdBacSi = [...form.elements.cacIdBacSi.selectedOptions].map((o) => Number(o.value));
        if (cacIdBacSi.length) than.cacIdBacSi = cacIdBacSi;
        return than;
    }

    function veKetQuaSaoChep(dl) {
        const bangBanSao = (ds, laBoQua) => (ds.length
            ? bang([t('Ngày', 'Date'), t('Giờ', 'Time'), t('Bác sĩ', 'Doctor'), t('Phòng', 'Room'), laBoQua ? t('Lý do bỏ qua', 'Skip reason') : t('Ca mới', 'New shift')],
                ds.map((b) => dong([`${thuCuaNgay(b.ngay)} ${ngayVn(b.ngay)}`, `${gioPhut(b.gioBatDau)}–${gioPhut(b.gioKetThuc)}`, tenBacSi(b.bacSi), tenPhong(b.phongKham),
                    laBoQua ? b.lyDo : (b.idCaMoi ? '#' + b.idCaMoi : '—')])))
            : ghiChu(t('Không có.', 'None.')));
        $('qt-sc-ket-qua').replaceChildren(
            the('h3', dl.daLuu ? t(`Đã tạo (${dl.taoDuoc.length})`, `Created (${dl.taoDuoc.length})`) : t(`Sẽ tạo (${dl.taoDuoc.length})`, `Will be created (${dl.taoDuoc.length})`)),
            bangBanSao(dl.taoDuoc, false),
            the('h3', t(`Bỏ qua (${dl.boQua.length})`, `Skipped (${dl.boQua.length})`)),
            bangBanSao(dl.boQua, true));
    }

    function veKetQuaNhap(dl) {
        $('qt-nx-ket-qua').replaceChildren(
            the('h3', t(`Lỗi (${dl.loi.length})`, `Errors (${dl.loi.length})`)),
            dl.loi.length
                ? bang([t('Dòng', 'Row'), t('Cột', 'Column'), t('Lý do', 'Reason')], dl.loi.map((l) => dong([l.dong, l.cot || '—', l.lyDo])))
                : ghiChu(t('Không có.', 'None.')),
            the('h3', dl.daLuu ? t(`Đã tạo (${dl.hopLe.length})`, `Created (${dl.hopLe.length})`) : t(`Dòng hợp lệ (${dl.hopLe.length})`, `Valid rows (${dl.hopLe.length})`)),
            dl.hopLe.length
                ? bang([t('Dòng', 'Row'), t('Ngày', 'Date'), t('Giờ', 'Time'), t('Bác sĩ', 'Doctor'), t('Phòng', 'Room'), t('Ca mới', 'New shift')],
                    dl.hopLe.map((h) => dong([h.dong, `${thuCuaNgay(h.ngay)} ${ngayVn(h.ngay)}`, `${gioPhut(h.gioBatDau)}–${gioPhut(h.gioKetThuc)}`, tenBacSi(h.bacSi), tenPhong(h.phongKham),
                        h.idCaMoi ? '#' + h.idCaMoi : '—'])))
                : ghiChu(t('Không có.', 'None.')));
    }

    // Giờ làm trong tuần của bác sĩ đang chọn ở form xếp ca
    async function taiGioTuanCuaBacSi() {
        const form = $('form-tao-ca');
        if (!form || !form.elements.idBacSi.value || !form.elements.ngay.value) return;
        const ngay = form.elements.ngay.value;
        const { status, json } = await lay(`${CA}/gio-lam-theo-tuan?tuNgay=${ngay}&denNgay=${ngay}&idBacSi=${form.elements.idBacSi.value}`, true);
        const dongBs = status === 200 && json.duLieu.bacSi[0];
        if (!dongBs) {
            bao('qt-tao-gio-tuan', '');
            return;
        }
        const tuan = dongBs.cacTuan[0];
        bao('qt-tao-gio-tuan', t(`Tuần ${ngayVn(tuan.tuNgay)} – ${ngayVn(tuan.denNgay)}: đã xếp ${soGio(tuan.soPhut)} (${tuan.soCa} ca), còn ${soGio(tuan.soPhutConLai)} trong giới hạn ${soGio(json.duLieu.gioiHanPhut)}.`,
            `Week ${ngayVn(tuan.tuNgay)} – ${ngayVn(tuan.denNgay)}: ${soGio(tuan.soPhut)} scheduled (${tuan.soCa} shifts), ${soGio(tuan.soPhutConLai)} left of the ${soGio(json.duLieu.gioiHanPhut)} limit.`),
            tuan.soPhutConLai === 0 ? 'loi' : '');
    }
    const moTaCa = (ca) => `${thuCuaNgay(ca.ngay)} ${ngayVn(ca.ngay)} ${gioPhut(ca.gioBatDau)}–${gioPhut(ca.gioKetThuc)} · ${tenPhong(ca.phongKham)}`;
    function ketQua(id, kq, tot, hong) {
        const duoc = kq.status === 200 || kq.status === 201;
        bao(id, duoc ? tot : thongDiepLoi(kq.json, hong), duoc ? 'tot' : 'loi');
        return duoc;
    }

    // Đưa 1 ca vào form sửa
    function moSuaCa(ca) {
        const form = $('form-sua-ca');
        $('qt-khoi-sua-ca').classList.remove('an');
        $('qt-tieu-de-sua-ca').textContent = t(`Sửa ca #${ca.idLichLamViec}: ${tenBacSi(ca.bacSi)}, ${moTaCa(ca)} (đã đặt ${ca.soLuotDaDat}/${ca.tongSoLuot})`,
            `Edit shift #${ca.idLichLamViec}: ${tenBacSi(ca.bacSi)}, ${moTaCa(ca)} (booked ${ca.soLuotDaDat}/${ca.tongSoLuot})`);
        form.dataset.id = ca.idLichLamViec;
        form.elements.idPhongKham.value = ca.phongKham.id;
        form.elements.gioBatDau.value = gioPhut(ca.gioBatDau);
        form.elements.gioKetThuc.value = gioPhut(ca.gioKetThuc);
        form.elements.soLuotToiDaMoiGio.value = ca.soLuotToiDaMoiGio;
        form.elements.thoiLuongLuotPhut.value = ca.thoiLuongLuotPhut;
        bao('qt-sua-ca-trang-thai', '');
        $('qt-khoi-sua-ca').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    async function huyCa(ca) {
        const lyDo = prompt(t(`Hủy ca #${ca.idLichLamViec} (${moTaCa(ca)})? ${ca.soLuotDaDat} lượt đã có người đặt sẽ được đánh dấu cần đổi lịch.\nNhập lý do:`,
            `Cancel shift #${ca.idLichLamViec} (${moTaCa(ca)})? ${ca.soLuotDaDat} booked visit slots will be marked as needing a reschedule.\nEnter the reason:`));
        if (!lyDo || !lyDo.trim()) return;
        const kq = await goi(`${CA}/${ca.idLichLamViec}/huy`, { lyDo: lyDo.trim() });
        if (ketQua('qt-ca-trang-thai', kq, kq.json && kq.json.duLieu ? t(`Đã hủy ca. ${kq.json.duLieu.soLichHenCanDoi} lịch hẹn cần đổi lịch.`,
            `Shift cancelled. ${kq.json.duLieu.soLichHenCanDoi} appointments need a reschedule.`) : '', t('Không hủy được ca.', 'Could not cancel the shift.'))) {
            taiCa();
            taiCanDoi();
            taiYeuCau();
        }
    }

    async function taiYeuCau() {
        const khung = $('qt-ds-yeu-cau');
        if (!khung) return;
        const { status, json } = await lay(`${YEU_CAU}?kichThuoc=50&tatCa=${$('qt-yc-tat-ca').checked}`, true);
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được yêu cầu.', 'Could not load the requests.')), 'ghi-chu loi'));
            return;
        }
        if (!json.duLieu.noiDung.length) {
            khung.replaceChildren(ghiChu(t('Không có yêu cầu nào.', 'No requests.')));
            return;
        }
        const xuLy = async (yc, hanhDong, canGhiChu) => {
            const ghiChu = prompt(hanhDong === 'duyet'
                ? t(`Duyệt yêu cầu #${yc.id}? Ca đang có ${yc.ca.soLuotDaDat} lượt đã đặt.\nGhi chú (không bắt buộc):`,
                    `Approve request #${yc.id}? The shift has ${yc.ca.soLuotDaDat} booked visit slots.\nNote (optional):`)
                : t(`Từ chối yêu cầu #${yc.id}. Ghi chú (bắt buộc):`, `Reject request #${yc.id}. Note (required):`));
            if (ghiChu === null || (canGhiChu && !ghiChu.trim())) return;
            const kq = await goi(`${YEU_CAU}/${yc.id}/${hanhDong}`, { ghiChu: ghiChu.trim() || null });
            ketQua('qt-yc-trang-thai', kq, hanhDong === 'duyet' ? t('Đã duyệt, lịch làm việc đã đổi theo.', 'Approved, the work schedule was updated.') : t('Đã từ chối.', 'Rejected.'),
                t('Không xử lý được yêu cầu.', 'Could not process the request.'));
            taiYeuCau();
            taiCa();
            taiCanDoi();
        };
        // Cả nhóm xin nghỉ nhiều ca: 1 lần duyệt / từ chối; ca đã bắt đầu được liệt kê trong boQua
        const xuLyNhom = async (yc, hanhDong) => {
            const ghiChu = prompt(hanhDong === 'duyet'
                ? t('Duyệt cả nhóm xin nghỉ (hủy mọi ca còn chờ duyệt của nhóm)? Ghi chú (không bắt buộc):', 'Approve the whole leave group (cancels every pending shift in it)? Note (optional):')
                : t('Từ chối cả nhóm xin nghỉ. Ghi chú (bắt buộc):', 'Reject the whole leave group. Note (required):'));
            if (ghiChu === null || (hanhDong === 'tu-choi' && !ghiChu.trim())) return;
            const kq = await goi(`${YEU_CAU}/nhom/${yc.maNhom}/${hanhDong}`, { ghiChu: ghiChu.trim() || null });
            const dl = kq.json && kq.json.duLieu;
            ketQua('qt-yc-trang-thai', kq, dl ? t(`Đã xử lý ${dl.daXuLy.length} yêu cầu`, `${dl.daXuLy.length} request(s) handled`)
                + (dl.boQua.length ? t(`, bỏ qua ${dl.boQua.length} (ca đã bắt đầu)`, `, ${dl.boQua.length} skipped (shift already started)`) : '') + '.' : '',
                t('Không xử lý được nhóm.', 'Could not process the group.'));
            taiYeuCau();
            taiCa();
            taiCanDoi();
        };
        khung.replaceChildren(bang([t('Gửi lúc', 'Sent at'), t('Bác sĩ', 'Doctor'), t('Loại', 'Type'), t('Ca', 'Shift'), t('Đã đặt', 'Booked'), t('Mong muốn / người nhận', 'Requested / colleague'), t('Lý do', 'Reason'), t('Trạng thái', 'Status'), t('Ghi chú', 'Note'), t('Thao tác', 'Actions')], json.duLieu.noiDung.map((yc) => dong([
            ngayGioVn(yc.ngayGui), tenBacSi(yc.bacSi), LOAI_YEU_CAU[yc.loaiYeuCau] + (yc.maNhom ? t(' (nhóm ', ' (group ') + yc.maNhom.slice(0, 8) + ')' : ''),
            moTaCa(yc.ca) + (yc.ca.trangThai === 'DA_HUY' ? t(' (đã hủy)', ' (cancelled)') : ''), `${yc.ca.soLuotDaDat} / ${yc.ca.tongSoLuot}`,
            yc.bacSiNhan ? '→ ' + tenBacSi(yc.bacSiNhan)
                : yc.ngayMongMuon ? `${ngayVn(yc.ngayMongMuon)} ${gioPhut(yc.gioBatDauMongMuon)}–${gioPhut(yc.gioKetThucMongMuon)}`
                + (yc.phongKhamMongMuon ? ' · ' + tenPhong(yc.phongKhamMongMuon) : '') : null,
            yc.lyDo, TRANG_THAI_YEU_CAU[yc.trangThai], yc.ghiChuXuLy || yc.ghiChuBacSiNhan,
            yc.trangThai === 'CHO_DUYET' ? [nut(t('Duyệt', 'Approve'), () => xuLy(yc, 'duyet', false)), nut(t('Từ chối', 'Reject'), () => xuLy(yc, 'tu-choi', true), 'nguy-hiem nho'),
                yc.maNhom ? nut(t('Duyệt cả nhóm', 'Approve group'), () => xuLyNhom(yc, 'duyet')) : null,
                yc.maNhom ? nut(t('Từ chối cả nhóm', 'Reject group'), () => xuLyNhom(yc, 'tu-choi'), 'nguy-hiem nho') : null] : null,
        ]))));
    }

    async function taiCanDoi() {
        const khung = $('qt-ds-can-doi');
        if (!khung) return;
        const { status, json } = await lay(`${CA}/can-doi-lich?kichThuoc=50`, true);
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được danh sách.', 'Could not load the list.')), 'ghi-chu loi'));
            return;
        }
        khung.replaceChildren(json.duLieu.noiDung.length
            ? bangLichHen(json.duLieu.noiDung, taiCanDoi)
            : ghiChu(t('Không có lịch hẹn nào đang chờ đổi lịch.', 'No appointments are waiting for a reschedule.')));
    }

    async function taiCa() {
        taiGioLam();
        const thamSo = new URLSearchParams(thanForm($('form-loc-ca'), ['tuNgay', 'denNgay', 'idChuyenKhoa', 'idBacSi', 'idPhongKham']));
        const { status, json } = await lay(`${CA}?${thamSo}`);
        const khung = $('qt-ds-ca');
        if (!khung) return;
        if (status !== 200) {
            khung.replaceChildren(the('p', thongDiepLoi(json, t('Không tải được lịch làm việc.', 'Could not load the work schedule.')), 'ghi-chu loi'));
            return;
        }
        if (!json.duLieu.length) {
            khung.replaceChildren(ghiChu(t('Không có ca làm việc nào khớp bộ lọc.', 'No shifts match the filter.')));
            return;
        }
        khung.replaceChildren(bang([t('Ngày', 'Date'), t('Giờ', 'Time'), t('Bác sĩ', 'Doctor'), t('Chuyên khoa', 'Specialty'), t('Phòng', 'Room'), t('Đã đặt / tổng', 'Booked / total'), t('Còn trống', 'Free'), t('Trạng thái', 'Status'), t('Thao tác', 'Actions')], json.duLieu.map((ca) => dong([
            `${thuCuaNgay(ca.ngay)} ${ngayVn(ca.ngay)}`, `${ca.gioBatDau.slice(0, 5)}–${ca.gioKetThuc.slice(0, 5)}`, tenBacSi(ca.bacSi),
            ca.tenChuyenKhoa, tenPhong(ca.phongKham), `${ca.soLuotDaDat} / ${ca.tongSoLuot}`, ca.soLuotConTrong, ca.trangThai,
            [nut(t(`Lịch hẹn của ca #${ca.idLichLamViec}`, `Appointments of shift #${ca.idLichLamViec}`), () => xemLichHenCuaCa(ca.idLichLamViec)),
                nut(t('Lịch sử', 'History'), () => xemLichSuCa(ca.idLichLamViec), 'phu nho'),
                // Chỉ ca còn hoạt động và chưa bắt đầu mới sửa / hủy được (API trả 409 CA_KHONG_SUA_DUOC nếu không)
                ...(ca.trangThai === 'HOAT_DONG' ? [nut(t('Sửa', 'Edit'), () => moSuaCa(ca), 'phu nho'), nut(t('Hủy ca', 'Cancel shift'), () => huyCa(ca), 'nguy-hiem nho')] : [])],
        ]))));
    }

    async function doLuaChon(idO, duongDan, nhan) {
        const { status, json } = await lay(duongDan, true);
        if (status !== 200 || !$(idO)) return;
        for (const muc of json.duLieu.noiDung) {
            const lua = the('option', nhan(muc));
            lua.value = muc.id;
            $(idO).append(lua);
        }
    }

    dangKy({
        duongDan: '/admin/schedule-management',
        vaiTro: 'QUAN_TRI_VIEN',
        tieuDe: cap('Quản lý lịch làm việc', 'Schedule Management'),
        async ve(khung) {
            khuon(khung, `
                <section>
                    <h2>${t('Xếp ca làm việc', 'Schedule shifts')}</h2>
                    <form id="form-tao-ca" class="hai-cot">
                        <div><label>${t('Bác sĩ', 'Doctor')}</label><select name="idBacSi" id="qt-tao-bac-si" required></select></div>
                        <div><label>${t('Phòng khám (cùng chuyên khoa với bác sĩ)', 'Room (same specialty as the doctor)')}</label><select name="idPhongKham" id="qt-tao-phong" required></select></div>
                        <div><label>${t('Ngày', 'Date')}</label><input name="ngay" type="date" required></div>
                        <div><label>${t('Giờ bắt đầu', 'Start time')}</label><input name="gioBatDau" type="time" value="08:00" required></div>
                        <div><label>${t('Giờ kết thúc', 'End time')}</label><input name="gioKetThuc" type="time" value="11:30" required></div>
                        <div><label>${t('Số lượt mỗi giờ (N)', 'Visit slots per hour (N)')}</label><input name="soLuotToiDaMoiGio" type="number" min="1" max="60" value="6" required></div>
                        <div><label>${t('Số phút mỗi lượt (t), N x t ≤ 60', 'Minutes per visit slot (t), N x t ≤ 60')}</label><input name="thoiLuongLuotPhut" type="number" min="1" max="60" value="10" required></div>
                        <div class="ca-dong"><label>${t('Lặp lại vào các thứ (bỏ trống = chỉ 1 ca)', 'Repeat on weekdays (none checked = a single shift)')}</label>
                            <span id="qt-tao-thu"></span></div>
                        <div><label>${t('Lặp lại đến ngày', 'Repeat until')}</label><input name="denNgay" type="date"></div>
                        <button>${t('Xếp ca', 'Schedule shift')}</button>
                    </form>
                    <p id="qt-tao-gio-tuan" class="ghi-chu"></p>
                    <p id="qt-tao-ca-trang-thai" class="ghi-chu"></p>
                </section>
                <section>
                    <h2>${t('Sao chép ca sang khoảng khác', 'Copy shifts to another period')}</h2>
                    <p class="ghi-chu">${t('Các ca còn hoạt động của khoảng nguồn được dán từ ngày nhận (cùng thứ với ngày đầu khoảng nguồn), lặp lại theo số lần; lần sau cách lần trước số tuần đủ phủ khoảng nguồn. Ca không tạo được (trùng giờ, vượt giờ làm tuần, ngày đã qua / quá xa, bác sĩ / phòng ngừng hoạt động) bị bỏ qua kèm lý do. Xem trước rồi mới sao chép.',
                        'Active shifts of the source range are pasted from the target date (same weekday as the first source day), repeated the given number of times; each copy starts the number of whole weeks covering the source after the previous one. Copies that cannot be created (clash, over the weekly hours, past / too far ahead, inactive doctor / room) are skipped with the reason. Preview before copying.')}</p>
                    <form id="form-sao-chep" class="hai-cot">
                        <div><label>${t('Nguồn từ ngày', 'Source from')}</label><input name="tuNgay" type="date" required></div>
                        <div><label>${t('Nguồn đến ngày (tối đa 42 ngày)', 'Source to (up to 42 days)')}</label><input name="denNgay" type="date" required></div>
                        <div><label>${t('Dán từ ngày', 'Paste from')}</label><input name="ngayDich" type="date" required></div>
                        <div><label>${t('Số lần (1–12)', 'Number of copies (1–12)')}</label><input name="soLan" type="number" min="1" max="12" value="1" required></div>
                        <div><label>${t('Chuyên khoa', 'Specialty')}</label><select name="idChuyenKhoa" id="qt-sc-chuyen-khoa"><option value="">${t('Tất cả', 'All')}</option></select></div>
                        <div><label>${t('Bác sĩ (giữ Ctrl để chọn nhiều, bỏ trống = tất cả)', 'Doctors (hold Ctrl for several, none = all)')}</label><select name="cacIdBacSi" id="qt-sc-bac-si" multiple size="5"></select></div>
                        <button>${t('Xem trước', 'Preview')}</button>
                        <button type="button" id="qt-sc-luu" disabled>${t('Sao chép', 'Copy')}</button>
                    </form>
                    <p id="qt-sc-trang-thai" class="ghi-chu"></p>
                    <div id="qt-sc-ket-qua" class="bang"></div>
                </section>
                <section>
                    <h2>${t('Nhập lịch từ Excel', 'Import shifts from Excel')}</h2>
                    <p class="ghi-chu">${t('Mỗi dòng của trang đầu tiên là 1 ca: A mã bác sĩ (BS0001), B ngày, C giờ bắt đầu, D giờ kết thúc, E mã phòng, F N, G t. Tệp mẫu có hướng dẫn và danh sách bác sĩ / phòng; tệp xuất ở phần Lịch làm việc toàn viện cũng nhập lại được. Tất cả hoặc không: còn 1 dòng lỗi thì không dòng nào được lưu.',
                        'Each row of the first sheet is one shift: A doctor code (BS0001), B date, C start, D end, E room ID, F N, G t. The template has instructions and the doctor / room lists; a file exported from the clinic-wide schedule below can be imported again. All or nothing: if any row has an error, no row is saved.')}</p>
                    <form id="form-nhap-excel" class="hai-cot">
                        <div><label>${t('Tệp .xlsx (tối đa 2MB, 1000 dòng)', '.xlsx file (up to 2MB, 1000 rows)')}</label><input name="tep" type="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" required></div>
                        <button type="button" id="qt-nx-mau" class="phu">${t('Tải tệp mẫu', 'Download template')}</button>
                        <button>${t('Xem trước', 'Preview')}</button>
                        <button type="button" id="qt-nx-luu" disabled>${t('Nhập', 'Import')}</button>
                    </form>
                    <p id="qt-nx-trang-thai" class="ghi-chu"></p>
                    <div id="qt-nx-ket-qua" class="bang"></div>
                </section>
                <section>
                    <h2>${t('Lịch làm việc toàn viện', 'Clinic-wide work schedule')}</h2>
                    <form id="form-loc-ca" class="hai-cot">
                        <div><label>${t('Từ ngày', 'From date')}</label><input name="tuNgay" type="date" required></div>
                        <div><label>${t('Đến ngày (tối đa 42 ngày)', 'To date (up to 42 days)')}</label><input name="denNgay" type="date" required></div>
                        <div><label>${t('Chuyên khoa', 'Specialty')}</label><select name="idChuyenKhoa" id="qt-loc-chuyen-khoa"><option value="">${t('Tất cả', 'All')}</option></select></div>
                        <div><label>${t('Bác sĩ', 'Doctor')}</label><select name="idBacSi" id="qt-loc-bac-si"><option value="">${t('Tất cả', 'All')}</option></select></div>
                        <div><label>${t('Phòng khám (id)', 'Room (id)')}</label><input name="idPhongKham" type="number" min="1"></div>
                        <button>${t('Xem', 'View')}</button>
                        <button type="button" id="qt-xuat-excel" class="phu">${t('Xuất Excel (ca còn hoạt động)', 'Export to Excel (active shifts)')}</button>
                        <button type="button" id="qt-pdf-bac-si" class="phu">${t('In lịch bác sĩ (PDF)', 'Print doctor schedule (PDF)')}</button>
                        <button type="button" id="qt-pdf-toan-vien" class="phu">${t('In lịch toàn viện (PDF)', 'Print clinic schedule (PDF)')}</button>
                        <button type="button" id="qt-gui-lich" class="phu">${t('Gửi lịch cho bác sĩ', 'Send schedules to doctors')}</button>
                    </form>
                    <p id="qt-ca-trang-thai" class="ghi-chu"></p>
                    <div id="qt-gui-lich-ket-qua" class="bang"></div>
                    <div id="qt-ds-ca" class="bang"></div>
                    <h3>${t('Giờ làm theo tuần (thứ Hai – Chủ nhật)', 'Working hours per week (Monday – Sunday)')}</h3>
                    <div id="qt-gio-lam" class="bang"></div>
                </section>
                <section id="qt-khoi-lich-su-ca" class="an">
                    <h2 id="qt-tieu-de-lich-su">${t('Lịch sử ca', 'Shift history')}</h2>
                    <div id="qt-lich-su-ca" class="bang"></div>
                </section>
                <section id="qt-khoi-sua-ca" class="an">
                    <h2 id="qt-tieu-de-sua-ca">${t('Sửa ca', 'Edit shift')}</h2>
                    <p class="ghi-chu">${t('Đổi phòng luôn được (bệnh nhân đã đặt được báo). Đổi giờ / sức chứa chỉ được khi mọi lượt đã có người đặt còn nguyên.',
                        'The room can always be changed (patients who booked are notified). The time / capacity can only be changed when every booked visit slot stays intact.')}</p>
                    <form id="form-sua-ca" class="hai-cot">
                        <div><label>${t('Phòng khám', 'Room')}</label><select name="idPhongKham" id="qt-sua-phong" required></select></div>
                        <div><label>${t('Giờ bắt đầu', 'Start time')}</label><input name="gioBatDau" type="time" required></div>
                        <div><label>${t('Giờ kết thúc', 'End time')}</label><input name="gioKetThuc" type="time" required></div>
                        <div><label>${t('Số lượt mỗi giờ (N)', 'Visit slots per hour (N)')}</label><input name="soLuotToiDaMoiGio" type="number" min="1" max="60" required></div>
                        <div><label>${t('Số phút mỗi lượt (t)', 'Minutes per visit slot (t)')}</label><input name="thoiLuongLuotPhut" type="number" min="1" max="60" required></div>
                        <button>${t('Lưu ca', 'Save shift')}</button>
                    </form>
                    <p id="qt-sua-ca-trang-thai" class="ghi-chu"></p>
                </section>
                <section>
                    <h2>${t('Yêu cầu đổi ca / xin nghỉ / nhường ca của bác sĩ', 'Doctors\' shift-change / leave / handover requests')}</h2>
                    <p class="ghi-chu">${t('Nhường ca chỉ hiện ở đây sau khi đồng nghiệp đã đồng ý nhận.', 'A handover only appears here after the colleague has accepted it.')}</p>
                    <label><input type="checkbox" id="qt-yc-tat-ca"> ${t('Hiện cả yêu cầu đã xử lý', 'Also show handled requests')}</label>
                    <p id="qt-yc-trang-thai" class="ghi-chu"></p>
                    <div id="qt-ds-yeu-cau" class="bang"></div>
                </section>
                <section>
                    <h2>${t('Lịch hẹn cần đổi lịch (ca khám đã bị hủy)', 'Appointments that need a reschedule (shift was cancelled)')}</h2>
                    <p class="ghi-chu">${t('Bệnh nhân đã được báo trong ứng dụng; khách đặt không đăng nhập thì phòng khám cần gọi điện.',
                        'Patients were notified in the app; for guests who booked without signing in, the clinic needs to call them.')}</p>
                    <div id="qt-ds-can-doi" class="bang"></div>
                </section>
                <section id="qt-khoi-lich-hen-ca" class="an">
                    <h2 id="qt-tieu-de-ca">${t('Lịch hẹn của ca', 'Appointments of the shift')}</h2>
                    <div id="qt-lich-hen-ca" class="bang"></div>
                </section>
                <section>
                    <h2>${t('Tra lịch hẹn theo mã', 'Look up an appointment by code')}</h2>
                    <form id="form-tra-cuu" class="hai-cot">
                        <div><label>${t('Mã tra cứu (ECL-…) hoặc mã phiếu khám', 'Lookup code (ECL-…) or ticket code')}</label><input name="ma" required></div>
                        <button>${t('Tra cứu', 'Look up')}</button>
                    </form>
                    <div id="qt-tra-cuu" class="bang"></div>
                </section>`);
            const loc = $('form-loc-ca');
            loc.elements.tuNgay.value = congNgay(0);
            loc.elements.denNgay.value = congNgay(6);
            loc.addEventListener('submit', (e) => { e.preventDefault(); taiCa(); });
            khiGui('form-tra-cuu', async function traCuu(v) {
                const { status, json } = await lay(`${LICH_HEN}/tra-cuu?ma=${encodeURIComponent(v.ma.trim())}`);
                $('qt-tra-cuu').replaceChildren(status === 200
                    ? bangLichHen([json.duLieu], () => traCuu(v))
                    : the('p', thongDiepLoi(json, t('Không tìm thấy lịch hẹn.', 'Appointment not found.')), 'ghi-chu loi'));
            });
            // Xếp ca
            const taoCa = $('form-tao-ca');
            taoCa.elements.ngay.value = congNgay(1);
            [THU[1], THU[2], THU[3], THU[4], THU[5], THU[6], THU[0]].forEach((ten, i) => {
                const nhan = the('label', null, 'tren-dong');
                const o = the('input');
                o.type = 'checkbox';
                o.name = 'thu';
                o.value = i + 1;
                nhan.append(o, ' ' + ten + ' ');
                $('qt-tao-thu').append(nhan);
            });
            khiGui('form-tao-ca', async (v, form) => {
                const than = thanForm(form, ['idBacSi', 'idPhongKham', 'ngay', 'gioBatDau', 'gioKetThuc', 'soLuotToiDaMoiGio', 'thoiLuongLuotPhut']);
                const cacThu = [...form.querySelectorAll('input[name=thu]:checked')].map((o) => Number(o.value));
                if (cacThu.length || v.denNgay) than.lapLai = { cacThu, denNgay: v.denNgay || null };
                const kq = await goi(CA, than);
                const dl = kq.json && kq.json.duLieu;
                if (ketQua('qt-tao-ca-trang-thai', kq, dl ? t(`Đã xếp ${dl.daTao.length} ca.`, `Scheduled ${dl.daTao.length} shift(s).`)
                    + (dl.boQua.length ? t(' Bỏ qua: ', ' Skipped: ') + dl.boQua.map((b) => `${ngayVn(b.ngay)} (${b.lyDo})`).join('; ') : '') : '', t('Không xếp được ca.', 'Could not schedule the shift.'))) taiCa();
                taiGioTuanCuaBacSi();
            });
            taoCa.elements.idBacSi.addEventListener('change', taiGioTuanCuaBacSi);
            taoCa.elements.ngay.addEventListener('change', taiGioTuanCuaBacSi);
            // Sao chép ca: nút "Sao chép" chỉ bấm được sau khi xem trước đúng các giá trị đang có trong form
            const saoChep = $('form-sao-chep');
            saoChep.elements.tuNgay.value = congNgay(0);
            saoChep.elements.denNgay.value = congNgay(6);
            saoChep.elements.ngayDich.value = congNgay(7);
            let thanDaXemTruoc = null;
            const khoaNutSaoChep = () => { thanDaXemTruoc = null; $('qt-sc-luu').disabled = true; };
            saoChep.addEventListener('input', khoaNutSaoChep);
            saoChep.addEventListener('change', khoaNutSaoChep);
            khiGui('form-sao-chep', async (v, form) => {
                khoaNutSaoChep();
                const than = thanSaoChep(form);
                const kq = await goi(`${CA}/sao-chep/xem-truoc`, than);
                const dl = kq.json && kq.json.duLieu;
                if (!ketQua('qt-sc-trang-thai', kq, dl ? t(`Xem trước: ${dl.soCaNguon} ca nguồn.`, `Preview: ${dl.soCaNguon} source shift(s).`) : '', t('Không xem trước được.', 'Could not preview.'))) {
                    $('qt-sc-ket-qua').replaceChildren();
                    return;
                }
                veKetQuaSaoChep(dl);
                if (dl.taoDuoc.length) {
                    thanDaXemTruoc = JSON.stringify(than);
                    $('qt-sc-luu').disabled = false;
                }
            });
            $('qt-sc-luu').addEventListener('click', async () => {
                const than = thanSaoChep(saoChep);
                if (JSON.stringify(than) !== thanDaXemTruoc) return khoaNutSaoChep();
                khoaNutSaoChep();
                const kq = await goi(`${CA}/sao-chep`, than);
                const dl = kq.json && kq.json.duLieu;
                if (ketQua('qt-sc-trang-thai', kq, dl ? t(`Đã tạo ${dl.taoDuoc.length} ca, bỏ qua ${dl.boQua.length}.`, `Created ${dl.taoDuoc.length} shift(s), skipped ${dl.boQua.length}.`) : '', t('Không sao chép được.', 'Could not copy the shifts.'))) {
                    veKetQuaSaoChep(dl);
                    taiCa();
                }
            });
            // Xuất Excel theo bộ lọc đang có của lịch toàn viện
            $('qt-xuat-excel').addEventListener('click', async () => {
                const than = thanForm(loc, ['tuNgay', 'denNgay', 'idChuyenKhoa', 'idBacSi', 'idPhongKham']);
                const kq = await taiTep(`${CA}/xuat-excel?${new URLSearchParams(than)}`, `lich-lam-viec_${than.tuNgay}_${than.denNgay}.xlsx`);
                bao('qt-ca-trang-thai', kq.status === 200 ? t('Đã tải tệp Excel.', 'Excel file downloaded.') : thongDiepLoi(kq.json, t('Không xuất được.', 'Could not export.')), kq.status === 200 ? 'tot' : 'loi');
            });
            // In PDF theo bộ lọc đang có: lịch của bác sĩ đang chọn, hoặc lưới tuần toàn viện (lọc chuyên khoa)
            const baoTaiTep = (kq, loiMacDinh) => bao('qt-ca-trang-thai', kq.status === 200 ? t('Đã tải tệp PDF.', 'PDF downloaded.') : thongDiepLoi(kq.json, loiMacDinh), kq.status === 200 ? 'tot' : 'loi');
            $('qt-pdf-bac-si').addEventListener('click', async () => {
                const than = thanForm(loc, ['tuNgay', 'denNgay', 'idBacSi']);
                if (!than.idBacSi) return bao('qt-ca-trang-thai', t('Chọn 1 bác sĩ ở ô lọc để in lịch của bác sĩ đó.', 'Choose a doctor in the filter to print their schedule.'), 'loi');
                const kq = await taiTep(`${CA}/pdf/bac-si/${than.idBacSi}?tuNgay=${than.tuNgay}&denNgay=${than.denNgay}`, `lich-lam-viec_${than.tuNgay}_${than.denNgay}.pdf`);
                baoTaiTep(kq, t('Không in được.', 'Could not print.'));
            });
            $('qt-pdf-toan-vien').addEventListener('click', async () => {
                const than = thanForm(loc, ['tuNgay', 'denNgay', 'idChuyenKhoa']);
                const kq = await taiTep(`${CA}/pdf/toan-vien?${new URLSearchParams(than)}`, `lich-lam-viec-toan-vien_${than.tuNgay}_${than.denNgay}.pdf`);
                baoTaiTep(kq, t('Không in được.', 'Could not print.'));
            });
            // Gửi lịch: mỗi bác sĩ có ca (từ hôm nay) trong bộ lọc nhận 1 email + 1 thông báo; gửi lại = gửi thêm lần nữa
            $('qt-gui-lich').addEventListener('click', async () => {
                const than = thanForm(loc, ['tuNgay', 'denNgay', 'idChuyenKhoa']);
                if (loc.elements.idBacSi.value) than.cacIdBacSi = [Number(loc.elements.idBacSi.value)];
                if (!than.tuNgay || !than.denNgay) return bao('qt-ca-trang-thai', t('Chọn khoảng ngày trước.', 'Choose the date range first.'), 'loi');
                if (!confirm(t(`Gửi email lịch làm việc ${ngayVn(than.tuNgay)} – ${ngayVn(than.denNgay)} cho các bác sĩ trong bộ lọc?`,
                    `Email the work schedule ${ngayVn(than.tuNgay)} – ${ngayVn(than.denNgay)} to the doctors in the filter?`))) return;
                const nutGui = $('qt-gui-lich');
                nutGui.disabled = true;
                try {
                    const kq = await goi(`${CA}/gui-lich`, than);
                    const dl = kq.json && kq.json.duLieu;
                    if (ketQua('qt-ca-trang-thai', kq, dl ? t(`Đã gửi lịch cho ${dl.soBacSi} bác sĩ (${dl.soCa} ca).`, `Schedules sent to ${dl.soBacSi} doctor(s) (${dl.soCa} shifts).`) : '', t('Không gửi được.', 'Could not send.'))) {
                        $('qt-gui-lich-ket-qua').replaceChildren(bang([t('Bác sĩ', 'Doctor'), t('Số ca', 'Shifts'), t('Số giờ', 'Hours')],
                            dl.cacBacSi.map((b) => dong([tenBacSi(b.bacSi), b.soCa, soGio(b.soPhut)]))));
                    } else {
                        $('qt-gui-lich-ket-qua').replaceChildren();
                    }
                } finally {
                    nutGui.disabled = false;
                }
            });
            // Nhập Excel: nút "Nhập" chỉ bấm được sau khi xem trước đúng tệp đang chọn mà không còn lỗi
            const nhapExcel = $('form-nhap-excel');
            let tepDaXemTruoc = null;
            const khoaNutNhap = () => { tepDaXemTruoc = null; $('qt-nx-luu').disabled = true; };
            nhapExcel.elements.tep.addEventListener('change', khoaNutNhap);
            const guiTep = (duongDan) => {
                const du = new FormData();
                du.append('tep', nhapExcel.elements.tep.files[0]);
                return goi(duongDan, du);
            };
            $('qt-nx-mau').addEventListener('click', async () => {
                const kq = await taiTep(`${CA}/nhap-excel/mau`, 'mau-nhap-lich-lam-viec.xlsx');
                if (kq.status !== 200) bao('qt-nx-trang-thai', thongDiepLoi(kq.json, t('Không tải được tệp mẫu.', 'Could not download the template.')), 'loi');
            });
            nhapExcel.addEventListener('submit', async (e) => {
                e.preventDefault();
                khoaNutNhap();
                const tep = nhapExcel.elements.tep.files[0];
                const kq = await guiTep(`${CA}/nhap-excel/xem-truoc`);
                const dl = kq.json && kq.json.duLieu;
                if (!ketQua('qt-nx-trang-thai', kq, dl ? t(`Xem trước: ${dl.soDong} dòng, ${dl.hopLe.length} hợp lệ, ${dl.loi.length} lỗi.`, `Preview: ${dl.soDong} row(s), ${dl.hopLe.length} valid, ${dl.loi.length} error(s).`) : '', t('Không xem trước được.', 'Could not preview.'))) {
                    $('qt-nx-ket-qua').replaceChildren();
                    return;
                }
                veKetQuaNhap(dl);
                if (!dl.loi.length && dl.hopLe.length) {
                    tepDaXemTruoc = tep;
                    $('qt-nx-luu').disabled = false;
                }
            });
            $('qt-nx-luu').addEventListener('click', async () => {
                if (nhapExcel.elements.tep.files[0] !== tepDaXemTruoc) return khoaNutNhap();
                khoaNutNhap();
                const kq = await guiTep(`${CA}/nhap-excel`);
                const dl = kq.json && kq.json.duLieu;
                if (ketQua('qt-nx-trang-thai', kq, dl ? t(`Đã nhập ${dl.hopLe.length} ca.`, `Imported ${dl.hopLe.length} shift(s).`) : '', t('Không nhập được.', 'Could not import.'))) {
                    veKetQuaNhap(dl);
                    taiCa();
                }
            });
            khiGui('form-sua-ca', async (v, form) => {
                const kq = await goi(`${CA}/${form.dataset.id}`, thanForm(form, ['idPhongKham', 'gioBatDau', 'gioKetThuc', 'soLuotToiDaMoiGio', 'thoiLuongLuotPhut']), 'PUT');
                if (ketQua('qt-sua-ca-trang-thai', kq, t('Đã lưu ca.', 'Shift saved.'), t('Không sửa được ca.', 'Could not edit the shift.'))) taiCa();
                if (!$('qt-khoi-lich-su-ca').classList.contains('an')) xemLichSuCa(form.dataset.id);
            });
            lay('/api/catalog/phong-kham', true).then(({ status, json }) => {
                if (status !== 200) return;
                for (const idO of ['qt-tao-phong', 'qt-sua-phong']) {
                    if (!$(idO)) return;
                    for (const p of json.duLieu) {
                        const lua = the('option', `${tenPhong(p)} · ${p.tenChuyenKhoa}`);
                        lua.value = p.id;
                        $(idO).append(lua);
                    }
                }
            });
            doLuaChon('qt-tao-bac-si', '/api/catalog/bac-si?kichThuoc=100', (bs) => `${tenBacSi(bs)} · ${bs.tenChuyenKhoa}`).then(taiGioTuanCuaBacSi);
            $('qt-yc-tat-ca').addEventListener('change', taiYeuCau);
            taiYeuCau();
            taiCanDoi();
            doLuaChon('qt-loc-chuyen-khoa', '/api/catalog/chuyen-khoa?kichThuoc=100', (ck) => ck.tenChuyenKhoa);
            doLuaChon('qt-sc-chuyen-khoa', '/api/catalog/chuyen-khoa?kichThuoc=100', (ck) => ck.tenChuyenKhoa);
            doLuaChon('qt-sc-bac-si', '/api/catalog/bac-si?kichThuoc=100', (bs) => `${tenBacSi(bs)} · ${bs.tenChuyenKhoa}`);
            doLuaChon('qt-loc-bac-si', '/api/catalog/bac-si?kichThuoc=100', (bs) => `${tenBacSi(bs)} · ${bs.tenChuyenKhoa}`);
            await taiCa();
        },
    });
})();
