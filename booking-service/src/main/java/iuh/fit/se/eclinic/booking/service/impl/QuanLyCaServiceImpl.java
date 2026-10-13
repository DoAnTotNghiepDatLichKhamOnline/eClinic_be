package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.config.LichLamViecProperties;
import iuh.fit.se.eclinic.booking.dto.request.SuaCaRequest;
import iuh.fit.se.eclinic.booking.dto.request.TaoCaRequest;
import iuh.fit.se.eclinic.booking.dto.response.CaLamViecResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaHuyCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaTaoCaResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaTaoCaResponse.NgayBoQua;
import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.mapper.LichLamViecMapper;
import iuh.fit.se.eclinic.booking.repository.BacSiChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.KhungGioKhamRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository;
import iuh.fit.se.eclinic.booking.repository.PhongKhamChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.QuanTriVienChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.YeuCauDoiLichRepository;
import iuh.fit.se.eclinic.booking.service.LuotKhamService;
import iuh.fit.se.eclinic.booking.service.LyDoHuyCa;
import iuh.fit.se.eclinic.booking.service.QuanLyCaService;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichLamViecService;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.entity.scheduling.KhungGioKham;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.enums.NgonNgu;
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

/**
 * READ_COMMITTED như đặt lịch: sau khi chờ được khoá phải thấy dữ liệu request trước vừa commit (ca vừa xếp, lịch hẹn
 * vừa đặt), không đọc lại snapshot cũ.
 * <p>
 * Thứ tự khoá cố định: dòng ca (khi sửa / hủy) -> dòng bác sĩ -> dòng phòng khám -> các lịch hẹn còn hiệu lực của ca
 * (theo id) -> các lượt khám của ca (theo giờ). "Lịch hẹn trước, lượt khám sau" giống bác sĩ từ chối và bệnh nhân hủy /
 * đổi. Đặt lịch chỉ khoá lượt khám rồi mới thêm lịch hẹn, nên sau khi khoá được các lượt phải đọc lại lịch hẹn của ca.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuanLyCaServiceImpl implements QuanLyCaService {

    private static final List<TrangThaiLichHen> CON_HIEU_LUC = List.of(TrangThaiLichHen.CHO_XAC_NHAN,
            TrangThaiLichHen.DA_XAC_NHAN);

    private final LichLamViecRepository lichLamViecRepository;
    private final KhungGioKhamRepository khungGioKhamRepository;
    private final LichHenRepository lichHenRepository;
    private final YeuCauDoiLichRepository yeuCauDoiLichRepository;
    private final BacSiChiDocRepository bacSiChiDocRepository;
    private final PhongKhamChiDocRepository phongKhamChiDocRepository;
    private final QuanTriVienChiDocRepository quanTriVienChiDocRepository;
    private final LuotKhamService luotKhamService;
    private final ThongBaoLichLamViecService thongBaoLichLamViecService;
    private final LichLamViecProperties lichLamViecProperties;
    private final LichLamViecMapper lichLamViecMapper;
    private final LichHenMapper lichHenMapper;
    private final MessageSource messageSource;

    /** Lịch hẹn còn hiệu lực và mọi lượt khám của 1 ca, đều đã khoá. */
    private record DaKhoa(List<LichHen> lichHen, List<KhungGioKham> luot) {
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public KetQuaTaoCaResponse tao(Long idTaiKhoanQuanTri, TaoCaRequest request) {
        QuanTriVien quanTri = layQuanTriVien(idTaiKhoanQuanTri);
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
            String lyDoTrung = lyDoTrungLich(bacSi.getId(), phongKham.getId(), ngay, request.gioBatDau(),
                    request.gioKetThuc(), null);
            if (lyDoTrung != null) {
                if (!lapLai) {
                    throw new LoiNghiepVu(MaLoi.TRUNG_LICH_LAM_VIEC, lyDoTrung);
                }
                boQua.add(new NgayBoQua(ngay,
                        messageSource.getMessage(lyDoTrung, null, lyDoTrung, NgonNgu.hienTai().locale())));
                continue;
            }
            daTao.add(luuCa(bacSi, phongKham, quanTri, ngay, request.gioBatDau(), request.gioKetThuc(),
                    request.soLuotToiDaMoiGio(), request.thoiLuongLuotPhut()));
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
        boolean coDoi = suaCaDaKhoa(ca, request.idPhongKham(), request.gioBatDau(), request.gioKetThuc(),
                request.soLuotToiDaMoiGio(), request.thoiLuongLuotPhut());
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
        int soLichHen = huyCaDaKhoa(ca, quanTri, lyDoHuy, null);
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
        return huyCaDaKhoa(ca, quanTri, LyDoHuyCa.nguoiDungGo(lyDo), null);
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
    public int huyCaDaKhoa(LichLamViec ca, QuanTriVien quanTri, LyDoHuyCa lyDo, Long idYeuCauDangDuyet) {
        DaKhoa daKhoa = khoaLichHenVaLuot(ca.getId());
        daKhoa.luot().forEach(luot -> luot.setTrangThai(TrangThaiKhungGio.DA_HUY));
        ca.setTrangThai(TrangThaiLichLamViec.DA_HUY);
        for (LichHen lichHen : daKhoa.lichHen()) {
            lichHen.setCanDoiLich(true);
            thongBaoLichLamViecService.lichHenCanDoi(lichHen, lyDo);
        }
        // Yêu cầu đang chờ duyệt của ca không còn gì để duyệt: đóng lại, bác sĩ được báo
        yeuCauDoiLichRepository.findByLichLamViecIdAndTrangThai(ca.getId(), TrangThaiYeuCau.CHO_DUYET)
                .filter(yeuCau -> !yeuCau.getId().equals(idYeuCauDangDuyet))
                .ifPresent(yeuCau -> {
                    yeuCau.setTrangThai(TrangThaiYeuCau.TU_CHOI);
                    yeuCau.setAdminXuLy(quanTri);
                    // Câu của hệ thống: lưu khoá + lý do quản trị viên gõ, lúc đọc mới thành câu (xem VanBanHeThong)
                    yeuCau.setGhiChuXuLy(VanBanHeThong.ghi("heThong.caBiQuanTriHuy", lyDo.chu()));
                    yeuCau.setNgayXuLy(LocalDateTime.now());
                    thongBaoLichLamViecService.yeuCauDongDoHuyCa(yeuCau, lyDo);
                });
        return daKhoa.lichHen().size();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean suaCaDaKhoa(LichLamViec ca, Long idPhongKham, LocalTime gioBatDau, LocalTime gioKetThuc,
            int soLuotToiDaMoiGio, int thoiLuongLuotPhut) {
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
        return true;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LichLamViec taoCaMoi(Long idBacSi, Long idPhongKham, QuanTriVien quanTri, LocalDate ngay,
            LocalTime gioBatDau, LocalTime gioKetThuc, int soLuotToiDaMoiGio, int thoiLuongLuotPhut) {
        BacSi bacSi = khoaBacSi(idBacSi, true);
        PhongKham phongKham = khoaPhongKham(idPhongKham, bacSi, true);
        kiemTraSucChua(gioBatDau, gioKetThuc, soLuotToiDaMoiGio, thoiLuongLuotPhut);
        kiemTraNgay(ngay, gioBatDau, LocalDate.now());
        kiemTraKhongTrungLich(idBacSi, idPhongKham, ngay, gioBatDau, gioKetThuc, null);
        LichLamViec ca = luuCa(bacSi, phongKham, quanTri, ngay, gioBatDau, gioKetThuc, soLuotToiDaMoiGio,
                thoiLuongLuotPhut);
        luotKhamService.danhLaiSoThuTu(idPhongKham, ngay);
        return ca;
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

    /**
     * Lý do ca [gioBatDau, gioKetThuc) trùng giờ với ca còn hoạt động khác của bác sĩ hoặc của phòng; null nếu không trùng.
     * Hỏi thẳng repository chứ không gọi LichLamViecService.kiemTraKhongTrungLich: exception ném qua 1 bean
     * {@code @Transactional} khác đánh dấu cả transaction là rollback-only, nên không "bỏ qua ngày trùng" được.
     *
     * @param idCaDangSua ca đang sửa (không tự trùng với chính nó); null khi xếp ca mới
     */
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
        if (lapLai.denNgay().isAfter(homNay.plusDays(lichLamViecProperties.soNgayXepTruocToiDa()))) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.caQuaXa", String.valueOf(lichLamViecProperties.soNgayXepTruocToiDa()));
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
        if (!ngay.atTime(gioBatDau).isAfter(LocalDateTime.now())) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.caTrongQuaKhu");
        }
        if (ngay.isAfter(homNay.plusDays(lichLamViecProperties.soNgayXepTruocToiDa()))) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.caQuaXa", String.valueOf(lichLamViecProperties.soNgayXepTruocToiDa()));
        }
    }

    private static void kiemTraSucChua(LocalTime gioBatDau, LocalTime gioKetThuc, int soLuotToiDaMoiGio,
            int thoiLuongLuotPhut) {
        if (!gioKetThuc.isAfter(gioBatDau)) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.gioKetThuc");
        }
        if (soLuotToiDaMoiGio * thoiLuongLuotPhut > ChiaCaLamViec.SO_PHUT_MOI_KHUNG) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE,
                    "loi.DU_LIEU_KHONG_HOP_LE.sucChuaMoiGio");
        }
        if (ChiaCaLamViec.gioBatDauCacLuot(gioBatDau, gioKetThuc, soLuotToiDaMoiGio, thoiLuongLuotPhut).isEmpty()) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.caQuaNgan");
        }
    }

    /** @param phaiDangCongTac false khi sửa ca đã có: chỉ cần khoá, không xét lại bác sĩ còn công tác hay không */
    private BacSi khoaBacSi(Long idBacSi, boolean phaiDangCongTac) {
        BacSi bacSi = bacSiChiDocRepository.khoaTheoId(idBacSi).orElseThrow(() -> new LoiKhongTimThay("BacSi", idBacSi));
        if (phaiDangCongTac && (bacSi.getTrangThai() != TrangThaiBacSi.DANG_CONG_TAC
                || bacSi.getTaiKhoan().getTrangThai() != TrangThaiTaiKhoan.DA_KICH_HOAT)) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE,
                    "loi.DU_LIEU_KHONG_HOP_LE.bacSiKhongHoatDong");
        }
        return bacSi;
    }

    /** @param kiemTra false khi ca giữ nguyên phòng: chỉ cần khoá */
    private PhongKham khoaPhongKham(Long idPhongKham, BacSi bacSi, boolean kiemTra) {
        PhongKham phongKham = phongKhamChiDocRepository.khoaTheoId(idPhongKham)
                .orElseThrow(() -> new LoiKhongTimThay("PhongKham", idPhongKham));
        if (!kiemTra) {
            return phongKham;
        }
        if (phongKham.getTrangThai() != TrangThaiPhongKham.HOAT_DONG) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.phongNgungHoatDong");
        }
        if (!phongKham.getChuyenKhoa().getId().equals(bacSi.getChuyenKhoa().getId())) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.phongKhacChuyenKhoa");
        }
        return phongKham;
    }

    private LichLamViec luuCa(BacSi bacSi, PhongKham phongKham, QuanTriVien quanTri, LocalDate ngay,
            LocalTime gioBatDau, LocalTime gioKetThuc, int soLuotToiDaMoiGio, int thoiLuongLuotPhut) {
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
        // Flush ngay: ca kế tiếp của cùng lần gọi (lặp lại) và request khác đang chờ khoá phải thấy ca này khi kiểm tra trùng
        lichLamViecRepository.saveAndFlush(ca);
        List<KhungGioKham> cacLuot = new ArrayList<>();
        for (LocalTime gio : gioBatDauCacLuot) {
            cacLuot.add(luotMoi(ca, ngay.atTime(gio), thoiLuongLuotPhut));
        }
        khungGioKhamRepository.saveAll(cacLuot);
        return ca;
    }

    private static KhungGioKham luotMoi(LichLamViec ca, LocalDateTime gioBatDau, int thoiLuongLuotPhut) {
        KhungGioKham luot = new KhungGioKham();
        luot.setLichLamViec(ca);
        luot.setGioBatDau(gioBatDau);
        luot.setGioKetThuc(gioBatDau.plusMinutes(thoiLuongLuotPhut));
        return luot;
    }

    /**
     * Khoá các lịch hẹn còn hiệu lực của ca rồi các lượt khám của ca. Một lượt đặt lịch đang giữ khoá lượt khám có thể
     * vừa commit thêm lịch hẹn trong lúc chờ, nên sau khi khoá được các lượt thì đọc (và khoá) lại lịch hẹn.
     */
    private DaKhoa khoaLichHenVaLuot(Long idLichLamViec) {
        List<Long> cacId = lichHenRepository.timIdTheoCaVaTrangThai(idLichLamViec, CON_HIEU_LUC);
        if (!cacId.isEmpty()) {
            lichHenRepository.khoaTheoId(cacId);
        }
        List<KhungGioKham> luot = khungGioKhamRepository.khoaCacLuotCuaCa(idLichLamViec);
        List<Long> cacIdSau = lichHenRepository.timIdTheoCaVaTrangThai(idLichLamViec, CON_HIEU_LUC);
        return new DaKhoa(cacIdSau.isEmpty() ? List.of() : lichHenRepository.khoaTheoId(cacIdSau), luot);
    }

    /**
     * Chỉnh các lượt khám theo giờ / sức chứa mới. Dòng lượt khám không bao giờ bị xoá (lịch hẹn cũ còn trỏ tới): lượt
     * không còn trong ca thành DA_HUY, lượt có lại thành CON_TRONG, lượt mới thì thêm dòng.
     */
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
