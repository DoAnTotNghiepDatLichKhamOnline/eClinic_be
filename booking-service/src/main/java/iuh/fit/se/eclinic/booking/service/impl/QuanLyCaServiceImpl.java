package iuh.fit.se.eclinic.booking.service.impl;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import iuh.fit.se.eclinic.booking.dto.request.DongCaNhap;
import iuh.fit.se.eclinic.booking.dto.request.SaoChepCaRequest;
import iuh.fit.se.eclinic.booking.dto.request.SuaCaRequest;
import iuh.fit.se.eclinic.booking.dto.request.TaoCaRequest;
import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaHuyCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaNhapExcelResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaNhapExcelResponse.DongHopLe;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaNhapExcelResponse.LoiDong;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaSaoChepResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaSaoChepResponse.CaSaoChep;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaTaoCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaTaoCaResponse.NgayBoQua;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.mapper.CaKhamMapper;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.mapper.LichLamViecMapper;
import iuh.fit.se.eclinic.booking.repository.BacSiChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.CauHinhHeThongRepository;
import iuh.fit.se.eclinic.booking.repository.KhungGioKhamRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository;
import iuh.fit.se.eclinic.booking.repository.LichSuCaLamViecRepository;
import iuh.fit.se.eclinic.booking.repository.NgayNghiBenhVienRepository;
import iuh.fit.se.eclinic.booking.repository.PhongKhamChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.QuanTriVienChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.YeuCauDoiLichRepository;
import iuh.fit.se.eclinic.booking.service.CauHinhService;
import iuh.fit.se.eclinic.booking.service.LichSuLichHenService;
import iuh.fit.se.eclinic.booking.service.LuotKhamService;
import iuh.fit.se.eclinic.booking.service.LyDoHuyCa;
import iuh.fit.se.eclinic.booking.service.NguoiThucHien;
import iuh.fit.se.eclinic.booking.service.NguonThayDoi;
import iuh.fit.se.eclinic.booking.service.QuanLyCaService;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichLamViecService;
import iuh.fit.se.eclinic.booking.util.KhoangNgay;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.CauHinhHeThong;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.entity.scheduling.KhungGioKham;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.entity.scheduling.LichSuCaLamViec;
import iuh.fit.se.eclinic.common.enums.HanhDongCa;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
import iuh.fit.se.eclinic.common.enums.SuKienLichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiBacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec;
import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import iuh.fit.se.eclinic.common.enums.TrangThaiYeuCau;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.thongbao.VanBanHeThong;
import iuh.fit.se.eclinic.common.util.ChiaCaLamViec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** READ_COMMITTED như đặt lịch: chờ được khoá xong phải thấy dữ liệu request trước vừa commit. */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuanLyCaServiceImpl implements QuanLyCaService {

    private static final List<TrangThaiLichHen> CON_HIEU_LUC = List.of(TrangThaiLichHen.DA_XAC_NHAN);

    private final LichLamViecRepository lichLamViecRepository;
    private final KhungGioKhamRepository khungGioKhamRepository;
    private final LichHenRepository lichHenRepository;
    private final YeuCauDoiLichRepository yeuCauDoiLichRepository;
    private final LichSuCaLamViecRepository lichSuCaLamViecRepository;
    private final CauHinhHeThongRepository cauHinhHeThongRepository;
    /** Đọc thẳng repository: NgayNghiService gọi service này, gọi ngược lại thành vòng phụ thuộc. */
    private final NgayNghiBenhVienRepository ngayNghiBenhVienRepository;
    private final BacSiChiDocRepository bacSiChiDocRepository;
    private final PhongKhamChiDocRepository phongKhamChiDocRepository;
    private final QuanTriVienChiDocRepository quanTriVienChiDocRepository;
    private final LuotKhamService luotKhamService;
    private final ThongBaoLichLamViecService thongBaoLichLamViecService;
    private final LichSuLichHenService lichSuLichHenService;
    private final CauHinhService cauHinhService;
    private final LichLamViecMapper lichLamViecMapper;
    private final LichHenMapper lichHenMapper;
    private final CaKhamMapper caKhamMapper;
    private final MessageSource messageSource;

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Số bản sao tối đa của 1 lần sao chép (số ca nguồn x số lần): giữ 1 transaction ở cỡ vừa phải. */
    private static final int SO_BAN_SAO_TOI_DA = 1000;

    /** Lịch hẹn còn hiệu lực và mọi lượt khám của 1 ca, đều đã khoá. */
    private record DaKhoa(List<LichHen> lichHen, List<KhungGioKham> luot) {
    }

    /** 1 quy tắc xếp ca bị vi phạm: thành lỗi hoặc thành lý do bỏ qua 1 ngày khi xếp lặp lại. */
    private record ViPham(MaLoi maLoi, String khoa, String... thamSo) {

        LoiNghiepVu loi() {
            return new LoiNghiepVu(maLoi, khoa, (Object[]) thamSo);
        }

        String cau(MessageSource messageSource) {
            return messageSource.getMessage(khoa, thamSo, khoa, NgonNgu.hienTai().locale());
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public KetQuaTaoCaResponse tao(Long idTaiKhoanQuanTri, TaoCaRequest request) {
        QuanTriVien quanTri = layQuanTriVien(idTaiKhoanQuanTri);
        giuKhoaXepCa();
        BacSi bacSi = khoaBacSi(request.idBacSi(), true);
        PhongKham phongKham = khoaPhongKham(request.idPhongKham(), bacSi, true);
        kiemTraSucChua(request.gioBatDau(), request.gioKetThuc(), request.soLuotToiDaMoiGio(),
                request.thoiLuongLuotPhut());
        LocalDate homNay = LocalDate.now();
        kiemTraNgay(request.ngay(), request.gioBatDau(), homNay);
        boolean lapLai = request.lapLai() != null;

        List<LichLamViec> daTao = new ArrayList<>();
        List<NgayBoQua> boQua = new ArrayList<>();
        for (LocalDate ngay : cacNgay(request, homNay)) {
            ViPham viPham = viPhamNgayNghi(ngay);
            if (viPham == null) {
                String lyDoTrung = lyDoTrungLich(bacSi.getId(), phongKham.getId(), ngay, request.gioBatDau(),
                        request.gioKetThuc(), null);
                // Ca vừa tạo ở vòng trước đã flush nên được cộng vào giờ làm của tuần
                viPham = lyDoTrung != null ? new ViPham(MaLoi.TRUNG_LICH_LAM_VIEC, lyDoTrung)
                        : viPhamGioLamTuan(bacSi.getId(), ngay, request.gioBatDau(), request.gioKetThuc(), null, 0);
            }
            if (viPham != null) {
                if (!lapLai) {
                    throw viPham.loi();
                }
                boQua.add(new NgayBoQua(ngay, viPham.cau(messageSource)));
                continue;
            }
            daTao.add(luuCa(bacSi, phongKham, quanTri, ngay, request.gioBatDau(), request.gioKetThuc(),
                    request.soLuotToiDaMoiGio(), request.thoiLuongLuotPhut(), NguonThayDoi.QUAN_TRI));
        }
        if (daTao.isEmpty()) {
            throw new LoiNghiepVu(MaLoi.TRUNG_LICH_LAM_VIEC, "loi.TRUNG_LICH_LAM_VIEC.moiNgay");
        }
        for (LichLamViec ca : daTao) {
            luotKhamService.danhLaiSoThuTu(phongKham.getId(), ca.getNgayLamViec());
        }
        thongBaoLichLamViecService.caMoi(bacSi, daTao, phongKham);
        log.info("Quản trị viên id={} xếp {} ca cho bác sĩ id={} (bỏ qua {} ngày trùng)", quanTri.getId(),
                daTao.size(), bacSi.getId(), boQua.size());
        return new KetQuaTaoCaResponse(toResponses(daTao), boQua);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CaLamViecResponse sua(Long idTaiKhoanQuanTri, Long idLichLamViec, SuaCaRequest request) {
        QuanTriVien quanTri = layQuanTriVien(idTaiKhoanQuanTri);
        LichLamViec ca = khoaCaConSuaDuoc(idLichLamViec);
        boolean coDoi = suaCaDaKhoa(ca, quanTri, request.idPhongKham(), request.gioBatDau(), request.gioKetThuc(),
                request.soLuotToiDaMoiGio(), request.thoiLuongLuotPhut(), NguonThayDoi.QUAN_TRI);
        if (coDoi) {
            thongBaoLichLamViecService.caDaSua(ca);
            log.info("Quản trị viên id={} sửa ca id={}", quanTri.getId(), idLichLamViec);
        }
        return toResponse(ca);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public KetQuaHuyCaResponse huy(Long idTaiKhoanQuanTri, Long idLichLamViec, String lyDo) {
        QuanTriVien quanTri = layQuanTriVien(idTaiKhoanQuanTri);
        LichLamViec ca = khoaCaConSuaDuoc(idLichLamViec);
        LyDoHuyCa lyDoHuy = LyDoHuyCa.nguoiDungGo(lyDo);
        int soLichHen = huyCaDaKhoa(ca, quanTri, lyDoHuy, NguonThayDoi.QUAN_TRI);
        thongBaoLichLamViecService.caBiHuy(ca, lyDoHuy);
        log.info("Quản trị viên id={} hủy ca id={}, {} lịch hẹn cần đổi lịch", quanTri.getId(), idLichLamViec,
                soLichHen);
        return new KetQuaHuyCaResponse(toResponse(ca), soLichHen);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int huyKhongBaoBacSi(Long idTaiKhoanQuanTri, Long idLichLamViec, String lyDo) {
        QuanTriVien quanTri = layQuanTriVien(idTaiKhoanQuanTri);
        LichLamViec ca = khoaCaConSuaDuoc(idLichLamViec);
        return huyCaDaKhoa(ca, quanTri, LyDoHuyCa.nguoiDungGo(lyDo), NguonThayDoi.NGUNG_CONG_TAC);
    }

    @Override
    @Transactional(readOnly = true)
    public TrangDuLieu<LichHenTrongCaResponse> lichHenCanDoi(int trang, int kichThuoc) {
        return TrangDuLieu.tu(lichHenRepository.timCanDoiLich(CON_HIEU_LUC, PageRequest.of(trang, kichThuoc))
                .map(lichHen -> lichHenMapper.toTrongCaResponse(lichHen, true)));
    }

    @Override
    @Transactional(readOnly = true)
    public QuanTriVien layQuanTriVien(Long idTaiKhoan) {
        return quanTriVienChiDocRepository.findByTaiKhoanId(idTaiKhoan)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_CO_QUYEN));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LichLamViec khoaCaConSuaDuoc(Long idLichLamViec) {
        LichLamViec ca = lichLamViecRepository.findByIdForUpdate(idLichLamViec)
                .orElseThrow(() -> new LoiKhongTimThay("LichLamViec", idLichLamViec));
        if (ca.getTrangThai() != TrangThaiLichLamViec.HOAT_DONG
                || !ca.getNgayLamViec().atTime(ca.getGioBatDau()).isAfter(LocalDateTime.now())) {
            throw new LoiNghiepVu(MaLoi.CA_KHONG_SUA_DUOC);
        }
        return ca;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int huyCaDaKhoa(LichLamViec ca, QuanTriVien quanTri, LyDoHuyCa lyDo, NguonThayDoi nguon) {
        Long idYeuCauDangDuyet = nguon.idYeuCau();
        DaKhoa daKhoa = khoaLichHenVaLuot(ca.getId());
        daKhoa.luot().forEach(luot -> luot.setTrangThai(TrangThaiKhungGio.DA_HUY));
        ca.setTrangThai(TrangThaiLichLamViec.DA_HUY);
        // Cùng 1 câu cho lịch sử ca và nhật ký của từng lịch hẹn
        String lyDoLuu = lyDo.khoa() == null ? lyDo.chu() : VanBanHeThong.ghi(lyDo.khoa(), null);
        ghiLichSu(ca, HanhDongCa.HUY, nguon, quanTri, lyDoLuu);
        NguoiThucHien nguoi = NguoiThucHien.quanTri(quanTri);
        for (LichHen lichHen : daKhoa.lichHen()) {
            lichHen.setCanDoiLich(true);
            lichSuLichHenService.ghi(lichHen, SuKienLichHen.CA_BI_HUY, nguoi, lyDoLuu, null);
            thongBaoLichLamViecService.lichHenCanDoi(lichHen, lyDo);
        }
        // Yêu cầu đang chờ duyệt của ca không còn gì để duyệt: đóng lại, bác sĩ được báo
        yeuCauDoiLichRepository.findByLichLamViecIdAndTrangThaiIn(ca.getId(), TrangThaiYeuCau.DANG_CHO)
                .filter(yeuCau -> !yeuCau.getId().equals(idYeuCauDangDuyet))
                .ifPresent(yeuCau -> {
                    yeuCau.setTrangThai(TrangThaiYeuCau.TU_CHOI);
                    yeuCau.setAdminXuLy(quanTri);
                    // Câu của hệ thống: lưu khoá và lý do quản trị viên gõ, lúc đọc mới thành câu
                    yeuCau.setGhiChuXuLy(VanBanHeThong.ghi("heThong.caBiQuanTriHuy", lyDo.chu()));
                    yeuCau.setNgayXuLy(LocalDateTime.now());
                    thongBaoLichLamViecService.yeuCauDongDoHuyCa(yeuCau, lyDo);
                });
        return daKhoa.lichHen().size();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean suaCaDaKhoa(LichLamViec ca, QuanTriVien quanTri, Long idPhongKham, LocalTime gioBatDau,
            LocalTime gioKetThuc, int soLuotToiDaMoiGio, int thoiLuongLuotPhut, NguonThayDoi nguon) {
        PhongKham phongCu = ca.getPhongKham();
        boolean doiPhong = !phongCu.getId().equals(idPhongKham);
        boolean doiGio = !ca.getGioBatDau().equals(gioBatDau) || !ca.getGioKetThuc().equals(gioKetThuc)
                || ca.getSoLuotToiDaMoiGio() != soLuotToiDaMoiGio || ca.getThoiLuongLuotPhut() != thoiLuongLuotPhut;
        if (!doiPhong && !doiGio) {
            return false;
        }
        LocalDate ngay = ca.getNgayLamViec();
        BacSi bacSi = khoaBacSi(ca.getBacSi().getId(), false);
        // Phòng không đổi thì không xét lại trạng thái phòng (ca đang chạy trong phòng đó)
        PhongKham phongMoi = khoaPhongKham(idPhongKham, bacSi, doiPhong);
        kiemTraSucChua(gioBatDau, gioKetThuc, soLuotToiDaMoiGio, thoiLuongLuotPhut);
        if (!ngay.atTime(gioBatDau).isAfter(LocalDateTime.now())) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.caTrongQuaKhu");
        }
        kiemTraKhongTrungLich(bacSi.getId(), phongMoi.getId(), ngay, gioBatDau, gioKetThuc, ca.getId());
        if (doiGio) {
            // Chỉ chặn khi ca dài thêm: sửa giữ nguyên / rút ngắn giờ luôn được, kể cả ở tuần đã vượt giới hạn từ trước
            nemNeuCo(viPhamGioLamTuan(bacSi.getId(), ngay, gioBatDau, gioKetThuc, ca.getId(),
                    soPhut(ca.getGioBatDau(), ca.getGioKetThuc())));
        }

        DaKhoa daKhoa = khoaLichHenVaLuot(ca.getId());
        if (doiGio) {
            chinhLuot(ca, daKhoa, gioBatDau, gioKetThuc, soLuotToiDaMoiGio, thoiLuongLuotPhut);
            ca.setGioBatDau(gioBatDau);
            ca.setGioKetThuc(gioKetThuc);
            ca.setSoLuotToiDaMoiGio(soLuotToiDaMoiGio);
            ca.setThoiLuongLuotPhut(thoiLuongLuotPhut);
        }
        if (doiPhong) {
            ca.setPhongKham(phongMoi);
            for (LichHen lichHen : daKhoa.lichHen()) {
                lichHen.setPhongKham(phongMoi);
                thongBaoLichLamViecService.lichHenDoiPhong(lichHen);
            }
            lichLamViecRepository.flush();
            luotKhamService.danhLaiSoThuTu(phongCu.getId(), ngay);
        }
        luotKhamService.danhLaiSoThuTu(phongMoi.getId(), ngay);
        ghiLichSu(ca, HanhDongCa.SUA, nguon, quanTri, null);
        NguoiThucHien nguoi = NguoiThucHien.quanTri(quanTri);
        daKhoa.lichHen().forEach(lichHen -> lichSuLichHenService.ghi(lichHen, SuKienLichHen.CA_THAY_DOI, nguoi));
        return true;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LichLamViec taoCaMoi(Long idBacSi, Long idPhongKham, QuanTriVien quanTri, LocalDate ngay,
            LocalTime gioBatDau, LocalTime gioKetThuc, int soLuotToiDaMoiGio, int thoiLuongLuotPhut,
            NguonThayDoi nguon) {
        BacSi bacSi = khoaBacSi(idBacSi, true);
        PhongKham phongKham = khoaPhongKham(idPhongKham, bacSi, true);
        kiemTraSucChua(gioBatDau, gioKetThuc, soLuotToiDaMoiGio, thoiLuongLuotPhut);
        kiemTraNgay(ngay, gioBatDau, LocalDate.now());
        nemNeuCo(viPhamNgayNghi(ngay));
        kiemTraKhongTrungLich(idBacSi, idPhongKham, ngay, gioBatDau, gioKetThuc, null);
        nemNeuCo(viPhamGioLamTuan(idBacSi, ngay, gioBatDau, gioKetThuc, null, 0));
        LichLamViec ca = luuCa(bacSi, phongKham, quanTri, ngay, gioBatDau, gioKetThuc, soLuotToiDaMoiGio,
                thoiLuongLuotPhut, nguon);
        luotKhamService.danhLaiSoThuTu(idPhongKham, ngay);
        return ca;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int doiBacSiCaDaKhoa(LichLamViec ca, QuanTriVien quanTri, Long idBacSiMoi, NguonThayDoi nguon) {
        BacSi bacSiCu = ca.getBacSi();
        BacSi bacSiMoi = khoaBacSi(idBacSiMoi, true);
        // Phòng đang chạy ca này nên không xét lại trạng thái phòng, chỉ xét chuyên khoa của bác sĩ mới
        if (!ca.getPhongKham().getChuyenKhoa().getId().equals(bacSiMoi.getChuyenKhoa().getId())) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.phongKhacChuyenKhoa");
        }
        // Đã khoá dòng bác sĩ mới: giờ làm trong tuần của bác sĩ đó không bị request khác đổi giữa chừng
        kiemTraNhanCa(bacSiMoi.getId(), ca);
        DaKhoa daKhoa = khoaLichHenVaLuot(ca.getId());
        ca.setBacSi(bacSiMoi);
        NguoiThucHien nguoi = NguoiThucHien.quanTri(quanTri);
        String lyDo = VanBanHeThong.ghi("heThong.lichHenDoiBacSi", bacSiCu.getTaiKhoan().getHoTen());
        for (LichHen lichHen : daKhoa.lichHen()) {
            lichHen.setBacSi(bacSiMoi);
            lichHen.setDoiBacSi(true);
            lichSuLichHenService.ghi(lichHen, SuKienLichHen.DOI_BAC_SI, nguoi, lyDo, null);
            thongBaoLichLamViecService.lichHenDoiBacSi(lichHen, bacSiCu);
        }
        ghiLichSu(ca, HanhDongCa.SUA, nguon, quanTri, null);
        return daKhoa.lichHen().size();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void kiemTraNhanCa(Long idBacSiNhan, LichLamViec ca) {
        LocalDate ngay = ca.getNgayLamViec();
        if (lichLamViecRepository.existsTrungCaCuaBacSi(idBacSiNhan, ngay, ca.getGioBatDau(), ca.getGioKetThuc(),
                ca.getId())) {
            throw new LoiNghiepVu(MaLoi.TRUNG_LICH_LAM_VIEC, "loi.TRUNG_LICH_LAM_VIEC.bacSi");
        }
        // Ca còn thuộc bác sĩ cũ nên không nằm trong giờ đã xếp của bác sĩ nhận: tính như xếp thêm 1 ca mới
        nemNeuCo(viPhamGioLamTuan(idBacSiNhan, ngay, ca.getGioBatDau(), ca.getGioKetThuc(), null, 0));
    }

    /** 1 bản sao của 1 ca nguồn: {@code ca} là ca vừa tạo (null nếu bỏ qua), {@code lyDo} là lý do bỏ qua. */
    private record BanSao(LichLamViec nguon, BacSi bacSi, PhongKham phongKham, LocalDate ngay, LichLamViec ca,
            String lyDo) {
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public KetQuaSaoChepResponse saoChep(Long idTaiKhoanQuanTri, SaoChepCaRequest request, boolean luu) {
        QuanTriVien quanTri = layQuanTriVien(idTaiKhoanQuanTri);
        giuKhoaXepCa();
        LocalDate tuNgay = request.tuNgay();
        KhoangNgay.kiemTra(tuNgay, request.denNgay());
        if (!request.ngayDich().isAfter(request.denNgay())) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.saoChepNgayDich");
        }
        if (request.ngayDich().getDayOfWeek() != tuNgay.getDayOfWeek()) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.saoChepCungThu");
        }
        Set<Long> chiBacSi = request.cacIdBacSi() == null ? Set.of() : Set.copyOf(request.cacIdBacSi());
        List<LichLamViec> cacCaNguon = lichLamViecRepository
                .timCaNguonSaoChep(tuNgay, request.denNgay(), request.idChuyenKhoa()).stream()
                .filter(ca -> chiBacSi.isEmpty() || chiBacSi.contains(ca.getBacSi().getId()))
                .toList();
        if (cacCaNguon.isEmpty()) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.saoChepKhongCoCa");
        }
        if ((long) cacCaNguon.size() * request.soLan() > SO_BAN_SAO_TOI_DA) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.saoChepQuaNhieu",
                    String.valueOf(SO_BAN_SAO_TOI_DA));
        }

        // Khoá mọi bác sĩ rồi mọi phòng theo id tăng dần, cùng thứ tự với các đường ghi khác nên không deadlock
        Map<Long, BacSi> bacSiTheoId = new HashMap<>();
        cacCaNguon.stream().map(ca -> ca.getBacSi().getId()).distinct().sorted()
                .forEach(id -> bacSiTheoId.put(id, khoaBacSi(id, false)));
        Map<Long, PhongKham> phongTheoId = new HashMap<>();
        cacCaNguon.stream().map(ca -> ca.getPhongKham().getId()).distinct().sorted()
                .forEach(id -> phongTheoId.put(id, khoaPhongKham(id, null, false)));

        LocalDate homNay = LocalDate.now();
        long lech = ChronoUnit.DAYS.between(tuNgay, request.ngayDich());
        // Lần dán sau cách lần trước số tuần đủ phủ khoảng nguồn, nên thứ trong tuần được giữ nguyên
        long buoc = 7 * ((ChronoUnit.DAYS.between(tuNgay, request.denNgay()) + 7) / 7);
        List<BanSao> taoDuoc = new ArrayList<>();
        List<BanSao> boQua = new ArrayList<>();
        for (int lan = 0; lan < request.soLan(); lan++) {
            for (LichLamViec nguon : cacCaNguon) {
                LocalDate ngay = nguon.getNgayLamViec().plusDays(lech + lan * buoc);
                BacSi bacSi = bacSiTheoId.get(nguon.getBacSi().getId());
                PhongKham phongKham = phongTheoId.get(nguon.getPhongKham().getId());
                // Ca đã lưu ở vòng trước đã flush nên được tính khi xét trùng giờ và giờ làm trong tuần
                ViPham viPham = viPhamBanSao(nguon, bacSi, phongKham, ngay, homNay);
                if (viPham != null) {
                    boQua.add(new BanSao(nguon, bacSi, phongKham, ngay, null, viPham.cau(messageSource)));
                    continue;
                }
                LichLamViec ca = luuCa(bacSi, phongKham, quanTri, ngay, nguon.getGioBatDau(), nguon.getGioKetThuc(),
                        nguon.getSoLuotToiDaMoiGio(), nguon.getThoiLuongLuotPhut(), NguonThayDoi.SAO_CHEP);
                taoDuoc.add(new BanSao(nguon, bacSi, phongKham, ngay, ca, null));
            }
        }

        if (luu) {
            if (taoDuoc.isEmpty()) {
                throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.saoChepKhongTaoDuoc");
            }
            taoDuoc.stream().map(banSao -> Map.entry(banSao.phongKham().getId(), banSao.ngay())).distinct()
                    .forEach(phongNgay -> luotKhamService.danhLaiSoThuTu(phongNgay.getKey(), phongNgay.getValue()));
            Map<BacSi, List<LichLamViec>> theoBacSi = new LinkedHashMap<>();
            taoDuoc.forEach(banSao -> theoBacSi.computeIfAbsent(banSao.bacSi(), bs -> new ArrayList<>()).add(banSao.ca()));
            theoBacSi.forEach((bacSi, cacCa) -> thongBaoLichLamViecService.caMoiHangLoat(bacSi, cacCa.stream()
                    .sorted(Comparator.comparing(LichLamViec::getNgayLamViec).thenComparing(LichLamViec::getGioBatDau))
                    .toList()));
            log.info("Quản trị viên id={} sao chép {} ca nguồn x {} lần: tạo {} ca, bỏ qua {}", quanTri.getId(),
                    cacCaNguon.size(), request.soLan(), taoDuoc.size(), boQua.size());
        } else {
            // Xem trước: chạy y như khi lưu rồi rollback, nên kết quả xem trước theo đúng các quy tắc của lần lưu
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return new KetQuaSaoChepResponse(luu, cacCaNguon.size(), toCaSaoChep(taoDuoc, luu), toCaSaoChep(boQua, false));
    }

    /** Lý do không tạo được bản sao của ca nguồn vào ngày {@code ngay}; null nếu tạo được. Không ném (xem ViPham). */
    private ViPham viPhamBanSao(LichLamViec nguon, BacSi bacSi, PhongKham phongKham, LocalDate ngay,
            LocalDate homNay) {
        ViPham viPham = viPhamNgay(ngay, nguon.getGioBatDau(), homNay);
        if (viPham == null) {
            viPham = viPhamNgayNghi(ngay);
        }
        if (viPham == null) {
            viPham = viPhamBacSi(bacSi);
        }
        if (viPham == null) {
            viPham = viPhamPhong(phongKham, bacSi);
        }
        if (viPham == null) {
            String lyDoTrung = lyDoTrungLich(bacSi.getId(), phongKham.getId(), ngay, nguon.getGioBatDau(),
                    nguon.getGioKetThuc(), null);
            viPham = lyDoTrung == null ? null : new ViPham(MaLoi.TRUNG_LICH_LAM_VIEC, lyDoTrung);
        }
        if (viPham == null) {
            viPham = viPhamGioLamTuan(bacSi.getId(), ngay, nguon.getGioBatDau(), nguon.getGioKetThuc(), null, 0);
        }
        return viPham;
    }

    /** 1 dòng nhập đã lưu (trong transaction): ca vừa tạo cùng bác sĩ, phòng đã khoá. */
    private record DongDaLuu(DongCaNhap dong, BacSi bacSi, PhongKham phongKham, LichLamViec ca) {
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public KetQuaNhapExcelResponse nhapCa(Long idTaiKhoanQuanTri, List<DongCaNhap> cacDong, boolean luu) {
        QuanTriVien quanTri = layQuanTriVien(idTaiKhoanQuanTri);
        giuKhoaXepCa();
        // Thứ tự khoá như sao chép; mã hoặc id không có trong DB thành lỗi dòng
        Map<Long, BacSi> bacSiTheoId = new HashMap<>();
        cacDong.stream().map(DongCaNhap::idBacSi).distinct().sorted()
                .forEach(id -> bacSiChiDocRepository.khoaTheoId(id).ifPresent(bacSi -> bacSiTheoId.put(id, bacSi)));
        Map<Long, PhongKham> phongTheoId = new HashMap<>();
        cacDong.stream().map(DongCaNhap::idPhongKham).distinct().sorted()
                .forEach(id -> phongKhamChiDocRepository.khoaTheoId(id).ifPresent(phong -> phongTheoId.put(id, phong)));

        LocalDate homNay = LocalDate.now();
        List<DongDaLuu> daLuu = new ArrayList<>();
        List<LoiDong> loi = new ArrayList<>();
        for (DongCaNhap dong : cacDong) {
            BacSi bacSi = bacSiTheoId.get(dong.idBacSi());
            PhongKham phongKham = phongTheoId.get(dong.idPhongKham());
            if (bacSi == null) {
                loi.add(new LoiDong(dong.dong(), "A", new ViPham(MaLoi.DU_LIEU_KHONG_HOP_LE, "nhapExcel.khongCoBacSi",
                        dong.maBacSi()).cau(messageSource)));
            }
            if (phongKham == null) {
                loi.add(new LoiDong(dong.dong(), "E", new ViPham(MaLoi.DU_LIEU_KHONG_HOP_LE, "nhapExcel.khongCoPhong",
                        String.valueOf(dong.idPhongKham())).cau(messageSource)));
            }
            if (bacSi == null || phongKham == null) {
                continue;
            }
            // Dòng đã lưu ở vòng trước đã flush nên được tính khi xét trùng giờ và giờ làm trong tuần
            LoiDong loiDong = loiDongNhap(dong, bacSi, phongKham, homNay);
            if (loiDong != null) {
                loi.add(loiDong);
                continue;
            }
            LichLamViec ca = luuCa(bacSi, phongKham, quanTri, dong.ngay(), dong.gioBatDau(), dong.gioKetThuc(),
                    dong.soLuotToiDaMoiGio(), dong.thoiLuongLuotPhut(), NguonThayDoi.NHAP_EXCEL);
            daLuu.add(new DongDaLuu(dong, bacSi, phongKham, ca));
        }

        boolean coLuu = luu && loi.isEmpty();
        if (luu && !coLuu) {
            // Tất cả hoặc không: ném ra để rollback cả các dòng vừa lưu
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.nhapExcelCoLoi",
                    String.valueOf(loi.size()));
        }
        if (coLuu) {
            daLuu.stream().map(dong -> Map.entry(dong.phongKham().getId(), dong.dong().ngay())).distinct()
                    .forEach(phongNgay -> luotKhamService.danhLaiSoThuTu(phongNgay.getKey(), phongNgay.getValue()));
            Map<BacSi, List<LichLamViec>> theoBacSi = new LinkedHashMap<>();
            daLuu.forEach(dong -> theoBacSi.computeIfAbsent(dong.bacSi(), bs -> new ArrayList<>()).add(dong.ca()));
            theoBacSi.forEach((bacSi, cacCa) -> thongBaoLichLamViecService.caMoiHangLoat(bacSi, cacCa.stream()
                    .sorted(Comparator.comparing(LichLamViec::getNgayLamViec).thenComparing(LichLamViec::getGioBatDau))
                    .toList()));
            log.info("Quản trị viên id={} nhập {} ca từ Excel cho {} bác sĩ", quanTri.getId(), daLuu.size(),
                    theoBacSi.size());
        } else {
            // Xem trước, hoặc tệp còn lỗi: chạy y như khi lưu rồi rollback
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        List<DongHopLe> hopLe = daLuu.stream()
                .map(dong -> new DongHopLe(dong.dong().dong(), coLuu ? dong.ca().getId() : null,
                        caKhamMapper.toBacSiTomTat(dong.bacSi()), caKhamMapper.toPhongKhamTomTat(dong.phongKham()),
                        dong.dong().ngay(), dong.dong().gioBatDau(), dong.dong().gioKetThuc()))
                .toList();
        return new KetQuaNhapExcelResponse(coLuu, cacDong.size(), hopLe, loi);
    }

    /** Lỗi đầu tiên của 1 dòng nhập (bác sĩ, phòng đã khoá); null nếu lưu được. */
    private LoiDong loiDongNhap(DongCaNhap dong, BacSi bacSi, PhongKham phongKham, LocalDate homNay) {
        String cot = null;
        ViPham viPham = viPhamSucChua(dong.gioBatDau(), dong.gioKetThuc(), dong.soLuotToiDaMoiGio(),
                dong.thoiLuongLuotPhut());
        if (viPham == null) {
            viPham = viPhamNgay(dong.ngay(), dong.gioBatDau(), homNay);
            cot = "B";
        }
        if (viPham == null) {
            viPham = viPhamNgayNghi(dong.ngay());
        }
        if (viPham == null) {
            viPham = viPhamBacSi(bacSi);
            cot = "A";
        }
        if (viPham == null) {
            viPham = viPhamPhong(phongKham, bacSi);
            cot = "E";
        }
        if (viPham == null) {
            String lyDoTrung = lyDoTrungLich(bacSi.getId(), phongKham.getId(), dong.ngay(), dong.gioBatDau(),
                    dong.gioKetThuc(), null);
            viPham = lyDoTrung == null ? null : new ViPham(MaLoi.TRUNG_LICH_LAM_VIEC, lyDoTrung);
            cot = null;
        }
        if (viPham == null) {
            viPham = viPhamGioLamTuan(bacSi.getId(), dong.ngay(), dong.gioBatDau(), dong.gioKetThuc(), null, 0);
        }
        return viPham == null ? null : new LoiDong(dong.dong(), cot, viPham.cau(messageSource));
    }

    /** @param coIdCaMoi false khi xem trước: id ca vừa tạo sẽ mất khi rollback */
    private List<CaSaoChep> toCaSaoChep(List<BanSao> cacBanSao, boolean coIdCaMoi) {
        return cacBanSao.stream()
                .map(banSao -> new CaSaoChep(banSao.nguon().getId(),
                        coIdCaMoi && banSao.ca() != null ? banSao.ca().getId() : null,
                        caKhamMapper.toBacSiTomTat(banSao.bacSi()), caKhamMapper.toPhongKhamTomTat(banSao.phongKham()),
                        banSao.ngay(), banSao.nguon().getGioBatDau(), banSao.nguon().getGioKetThuc(), banSao.lyDo()))
                .sorted(Comparator.comparing(CaSaoChep::ngay).thenComparing(CaSaoChep::gioBatDau)
                        .thenComparing(caSaoChep -> caSaoChep.bacSi().hoTen()))
                .toList();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CaLamViecResponse toResponse(LichLamViec ca) {
        return toResponses(List.of(ca)).get(0);
    }

    private List<CaLamViecResponse> toResponses(List<LichLamViec> cacCa) {
        khungGioKhamRepository.flush();
        Map<Long, LichLamViecRepository.SoLuotCuaCa> soLuot = lichLamViecRepository
                .demLuotTheoCa(cacCa.stream().map(LichLamViec::getId).toList()).stream()
                .collect(Collectors.toMap(LichLamViecRepository.SoLuotCuaCa::getIdLichLamViec, dong -> dong));
        return cacCa.stream().map(ca -> lichLamViecMapper.toResponse(ca, soLuot.get(ca.getId()))).toList();
    }

    /** Lý do ca trùng giờ với ca còn hoạt động của bác sĩ hoặc của phòng; null nếu không trùng. */
    /** @return khoá thông điệp của lý do trùng (thong-diep/booking*.properties); null nếu không trùng */
    private String lyDoTrungLich(Long idBacSi, Long idPhongKham, LocalDate ngay, LocalTime gioBatDau,
            LocalTime gioKetThuc, Long idCaDangSua) {
        if (lichLamViecRepository.existsTrungCaCuaBacSi(idBacSi, ngay, gioBatDau, gioKetThuc, idCaDangSua)) {
            return "loi.TRUNG_LICH_LAM_VIEC.bacSi";
        }
        if (lichLamViecRepository.existsTrungCaCuaPhongKham(idPhongKham, ngay, gioBatDau, gioKetThuc, idCaDangSua)) {
            return "loi.TRUNG_LICH_LAM_VIEC.phongKham";
        }
        return null;
    }

    private void kiemTraKhongTrungLich(Long idBacSi, Long idPhongKham, LocalDate ngay, LocalTime gioBatDau,
            LocalTime gioKetThuc, Long idCaDangSua) {
        String lyDoTrung = lyDoTrungLich(idBacSi, idPhongKham, ngay, gioBatDau, gioKetThuc, idCaDangSua);
        if (lyDoTrung != null) {
            throw new LoiNghiepVu(MaLoi.TRUNG_LICH_LAM_VIEC, lyDoTrung);
        }
    }

    /** Ngày của ca đầu, rồi các ngày sau đó tới {@code denNgay} rơi vào các thứ đã chọn. */
    private List<LocalDate> cacNgay(TaoCaRequest request, LocalDate homNay) {
        List<LocalDate> cacNgay = new ArrayList<>();
        cacNgay.add(request.ngay());
        TaoCaRequest.LapLai lapLai = request.lapLai();
        if (lapLai == null) {
            return cacNgay;
        }
        if (lapLai.denNgay().isBefore(request.ngay())) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.lapLaiDenNgay");
        }
        int soNgayXepTruocToiDa = cauHinhService.hienTai().soNgayXepTruocToiDa();
        if (lapLai.denNgay().isAfter(homNay.plusDays(soNgayXepTruocToiDa))) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.caQuaXa", String.valueOf(soNgayXepTruocToiDa));
        }
        Set<Integer> cacThu = new HashSet<>(lapLai.cacThu());
        for (LocalDate ngay = request.ngay().plusDays(1); !ngay.isAfter(lapLai.denNgay()); ngay = ngay.plusDays(1)) {
            if (cacThu.contains(ngay.getDayOfWeek().getValue())) {
                cacNgay.add(ngay);
            }
        }
        return cacNgay;
    }

    private void kiemTraNgay(LocalDate ngay, LocalTime gioBatDau, LocalDate homNay) {
        nemNeuCo(viPhamNgay(ngay, gioBatDau, homNay));
    }

    /** Ca bắt đầu trong quá khứ hoặc ngày vượt quá số ngày được xếp trước; null nếu không vi phạm. */
    private ViPham viPhamNgay(LocalDate ngay, LocalTime gioBatDau, LocalDate homNay) {
        if (!ngay.atTime(gioBatDau).isAfter(LocalDateTime.now())) {
            return new ViPham(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.caTrongQuaKhu");
        }
        int soNgayXepTruocToiDa = cauHinhService.hienTai().soNgayXepTruocToiDa();
        if (ngay.isAfter(homNay.plusDays(soNgayXepTruocToiDa))) {
            return new ViPham(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.caQuaXa",
                    String.valueOf(soNgayXepTruocToiDa));
        }
        return null;
    }

    /** Ngày bệnh viện nghỉ, kèm ngày và lý do theo ngôn ngữ của request; null nếu bệnh viện làm việc. */
    private ViPham viPhamNgayNghi(LocalDate ngay) {
        return ngayNghiBenhVienRepository.findByNgay(ngay)
                .map(ngayNghi -> new ViPham(MaLoi.NGAY_BENH_VIEN_NGHI, "loi.NGAY_BENH_VIEN_NGHI.ngay",
                        NGAY.format(ngay), ngayNghi.lyDo(NgonNgu.hienTai())))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public void kiemTraNgayKhongNghi(LocalDate ngay) {
        nemNeuCo(viPhamNgayNghi(ngay));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void giuKhoaXepCa() {
        // Dòng cấu hình phải có rồi mới khoá được: hienTai() thêm dòng (transaction riêng) nếu chưa có
        cauHinhService.hienTai();
        cauHinhHeThongRepository.khoaChiaSe(CauHinhHeThong.ID)
                .orElseThrow(() -> new IllegalStateException("Không có dòng cau_hinh_he_thong"));
    }

    private static void nemNeuCo(ViPham viPham) {
        if (viPham != null) {
            throw viPham.loi();
        }
    }

    private static void kiemTraSucChua(LocalTime gioBatDau, LocalTime gioKetThuc, int soLuotToiDaMoiGio,
            int thoiLuongLuotPhut) {
        nemNeuCo(viPhamSucChua(gioBatDau, gioKetThuc, soLuotToiDaMoiGio, thoiLuongLuotPhut));
    }

    /** Giờ kết thúc không sau giờ bắt đầu, N x t quá 60 phút hoặc ca không đủ dài cho 1 lượt; null nếu hợp lệ. */
    private static ViPham viPhamSucChua(LocalTime gioBatDau, LocalTime gioKetThuc, int soLuotToiDaMoiGio,
            int thoiLuongLuotPhut) {
        if (!gioKetThuc.isAfter(gioBatDau)) {
            return new ViPham(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.gioKetThuc");
        }
        if (soLuotToiDaMoiGio * thoiLuongLuotPhut > ChiaCaLamViec.SO_PHUT_MOI_KHUNG) {
            return new ViPham(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.sucChuaMoiGio");
        }
        if (ChiaCaLamViec.gioBatDauCacLuot(gioBatDau, gioKetThuc, soLuotToiDaMoiGio, thoiLuongLuotPhut).isEmpty()) {
            return new ViPham(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.caQuaNgan");
        }
        return null;
    }

    /** @param phaiDangCongTac false khi sửa ca đã có: chỉ cần khoá, không xét lại bác sĩ còn công tác hay không */
    private BacSi khoaBacSi(Long idBacSi, boolean phaiDangCongTac) {
        BacSi bacSi = bacSiChiDocRepository.khoaTheoId(idBacSi).orElseThrow(() -> new LoiKhongTimThay("BacSi", idBacSi));
        if (phaiDangCongTac) {
            nemNeuCo(viPhamBacSi(bacSi));
        }
        return bacSi;
    }

    /** Bác sĩ đã ngừng công tác hoặc tài khoản chưa hoạt động; null nếu còn xếp ca được. */
    private static ViPham viPhamBacSi(BacSi bacSi) {
        if (bacSi.getTrangThai() != TrangThaiBacSi.DANG_CONG_TAC
                || bacSi.getTaiKhoan().getTrangThai() != TrangThaiTaiKhoan.DA_KICH_HOAT) {
            return new ViPham(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.bacSiKhongHoatDong");
        }
        return null;
    }

    /** @param kiemTra false khi ca giữ nguyên phòng: chỉ cần khoá */
    private PhongKham khoaPhongKham(Long idPhongKham, BacSi bacSi, boolean kiemTra) {
        PhongKham phongKham = phongKhamChiDocRepository.khoaTheoId(idPhongKham)
                .orElseThrow(() -> new LoiKhongTimThay("PhongKham", idPhongKham));
        if (kiemTra) {
            nemNeuCo(viPhamPhong(phongKham, bacSi));
        }
        return phongKham;
    }

    /** Phòng đã ngừng hoạt động hoặc không thuộc chuyên khoa của bác sĩ; null nếu xếp ca vào được. */
    private static ViPham viPhamPhong(PhongKham phongKham, BacSi bacSi) {
        if (phongKham.getTrangThai() != TrangThaiPhongKham.HOAT_DONG) {
            return new ViPham(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.phongNgungHoatDong");
        }
        if (!phongKham.getChuyenKhoa().getId().equals(bacSi.getChuyenKhoa().getId())) {
            return new ViPham(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.phongKhacChuyenKhoa");
        }
        return null;
    }

    private LichLamViec luuCa(BacSi bacSi, PhongKham phongKham, QuanTriVien quanTri, LocalDate ngay,
            LocalTime gioBatDau, LocalTime gioKetThuc, int soLuotToiDaMoiGio, int thoiLuongLuotPhut,
            NguonThayDoi nguon) {
        List<LocalTime> gioBatDauCacLuot = ChiaCaLamViec.gioBatDauCacLuot(gioBatDau, gioKetThuc, soLuotToiDaMoiGio,
                thoiLuongLuotPhut);
        LichLamViec ca = new LichLamViec();
        ca.setBacSi(bacSi);
        ca.setPhongKham(phongKham);
        ca.setAdminTao(quanTri);
        ca.setNgayLamViec(ngay);
        ca.setGioBatDau(gioBatDau);
        ca.setGioKetThuc(gioKetThuc);
        ca.setSoBenhNhanToiDa(gioBatDauCacLuot.size());
        ca.setSoLuotToiDaMoiGio(soLuotToiDaMoiGio);
        ca.setThoiLuongLuotPhut(thoiLuongLuotPhut);
        // Flush ngay để ca kế tiếp và request đang chờ khoá thấy ca này khi kiểm tra trùng
        lichLamViecRepository.saveAndFlush(ca);
        List<KhungGioKham> cacLuot = new ArrayList<>();
        for (LocalTime gio : gioBatDauCacLuot) {
            cacLuot.add(luotMoi(ca, ngay.atTime(gio), thoiLuongLuotPhut));
        }
        khungGioKhamRepository.saveAll(cacLuot);
        ghiLichSu(ca, HanhDongCa.TAO, nguon, quanTri, null);
        return ca;
    }

    /** Thêm 1 dòng lịch sử mang ảnh hiện tại của ca (gọi SAU khi đã đổi ca). */
    private void ghiLichSu(LichLamViec ca, HanhDongCa hanhDong, NguonThayDoi nguon, QuanTriVien quanTri,
            String lyDo) {
        LichSuCaLamViec dong = LichSuCaLamViec.anhCua(ca, hanhDong, nguon.nguon(), quanTri);
        dong.setIdYeuCau(nguon.idYeuCau());
        dong.setLyDo(lyDo);
        lichSuCaLamViecRepository.save(dong);
    }

    /** Vi phạm giới hạn giờ làm trong tuần nếu bác sĩ nhận thêm ca này; null nếu không vi phạm. */
    private ViPham viPhamGioLamTuan(Long idBacSi, LocalDate ngay, LocalTime gioBatDau, LocalTime gioKetThuc,
            Long idCaDangSua, long soPhutCu) {
        long soPhutCa = soPhut(gioBatDau, gioKetThuc);
        if (soPhutCa <= soPhutCu) {
            return null;
        }
        LocalDate dauTuan = ngay.with(DayOfWeek.MONDAY);
        LocalDate cuoiTuan = dauTuan.plusDays(6);
        long daXep = lichLamViecRepository.timGioCaHoatDong(dauTuan, cuoiTuan, idBacSi, null, idCaDangSua).stream()
                .mapToLong(gio -> soPhut(gio.getGioBatDau(), gio.getGioKetThuc()))
                .sum();
        long gioiHan = cauHinhService.hienTai().soPhutToiDaMoiTuan();
        long tong = daXep + soPhutCa;
        if (tong <= gioiHan) {
            return null;
        }
        return new ViPham(MaLoi.VUOT_GIO_LAM_TUAN, "loi.VUOT_GIO_LAM_TUAN.chiTiet", NGAY.format(dauTuan),
                NGAY.format(cuoiTuan), thoiLuong(daXep), thoiLuong(tong), thoiLuong(tong - gioiHan),
                thoiLuong(gioiHan));
    }

    private static long soPhut(LocalTime gioBatDau, LocalTime gioKetThuc) {
        return Duration.between(gioBatDau, gioKetThuc).toMinutes();
    }

    /** "47 giờ 30 phút" / "47 h 30 min" theo ngôn ngữ của request (thông điệp lỗi được soạn cùng ngôn ngữ đó). */
    private String thoiLuong(long phut) {
        String khoa = phut % 60 == 0 ? "thoiLuong.gio" : "thoiLuong.gioPhut";
        return messageSource.getMessage(khoa, new Object[] {String.valueOf(phut / 60), String.valueOf(phut % 60)},
                NgonNgu.hienTai().locale());
    }

    private static KhungGioKham luotMoi(LichLamViec ca, LocalDateTime gioBatDau, int thoiLuongLuotPhut) {
        KhungGioKham luot = new KhungGioKham();
        luot.setLichLamViec(ca);
        luot.setGioBatDau(gioBatDau);
        luot.setGioKetThuc(gioBatDau.plusMinutes(thoiLuongLuotPhut));
        return luot;
    }

    /** Khoá các lịch hẹn còn hiệu lực của ca rồi các lượt khám của ca. */
    private DaKhoa khoaLichHenVaLuot(Long idLichLamViec) {
        List<Long> cacId = lichHenRepository.timIdTheoCaVaTrangThai(idLichLamViec, CON_HIEU_LUC);
        if (!cacId.isEmpty()) {
            lichHenRepository.khoaTheoId(cacId);
        }
        List<KhungGioKham> luot = khungGioKhamRepository.khoaCacLuotCuaCa(idLichLamViec);
        List<Long> cacIdSau = lichHenRepository.timIdTheoCaVaTrangThai(idLichLamViec, CON_HIEU_LUC);
        return new DaKhoa(cacIdSau.isEmpty() ? List.of() : lichHenRepository.khoaTheoId(cacIdSau), luot);
    }

    /** Chỉnh các lượt khám theo giờ / sức chứa mới. */
    private void chinhLuot(LichLamViec ca, DaKhoa daKhoa, LocalTime gioBatDau, LocalTime gioKetThuc,
            int soLuotToiDaMoiGio, int thoiLuongLuotPhut) {
        LocalDate ngay = ca.getNgayLamViec();
        Set<LocalDateTime> gioMoi = new HashSet<>();
        for (LocalTime gio : ChiaCaLamViec.gioBatDauCacLuot(gioBatDau, gioKetThuc, soLuotToiDaMoiGio,
                thoiLuongLuotPhut)) {
            gioMoi.add(ngay.atTime(gio));
        }
        Set<Long> luotCoLichHen = daKhoa.lichHen().stream().map(lichHen -> lichHen.getKhungGio().getId())
                .collect(Collectors.toSet());
        Map<LocalDateTime, KhungGioKham> luotTheoGio = new HashMap<>();
        long soLuotBiMat = 0;
        for (KhungGioKham luot : daKhoa.luot()) {
            luotTheoGio.put(luot.getGioBatDau(), luot);
            if (luotCoLichHen.contains(luot.getId()) && (!gioMoi.contains(luot.getGioBatDau())
                    || !luot.getGioKetThuc().equals(luot.getGioBatDau().plusMinutes(thoiLuongLuotPhut)))) {
                soLuotBiMat++;
            }
        }
        if (soLuotBiMat > 0) {
            throw new LoiNghiepVu(MaLoi.CA_CON_LICH_HEN, "loi.CA_CON_LICH_HEN.soLuot", String.valueOf(soLuotBiMat));
        }
        List<KhungGioKham> luotThem = new ArrayList<>();
        for (LocalDateTime gio : gioMoi) {
            KhungGioKham luot = luotTheoGio.get(gio);
            if (luot == null) {
                luotThem.add(luotMoi(ca, gio, thoiLuongLuotPhut));
            } else if (!luotCoLichHen.contains(luot.getId())) {
                luot.setGioKetThuc(gio.plusMinutes(thoiLuongLuotPhut));
                luot.setTrangThai(TrangThaiKhungGio.CON_TRONG);
            }
        }
        for (KhungGioKham luot : daKhoa.luot()) {
            if (!gioMoi.contains(luot.getGioBatDau())) {
                luot.setTrangThai(TrangThaiKhungGio.DA_HUY);
            }
        }
        khungGioKhamRepository.saveAll(luotThem);
        ca.setSoBenhNhanToiDa(gioMoi.size());
    }

}
