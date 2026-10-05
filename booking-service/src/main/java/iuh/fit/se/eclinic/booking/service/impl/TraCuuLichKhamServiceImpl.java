package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.config.DatLichProperties;
import iuh.fit.se.eclinic.booking.dto.response.CaKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.KhungGioGopResponse;
import iuh.fit.se.eclinic.booking.dto.response.KhungGioResponse;
import iuh.fit.se.eclinic.booking.dto.response.NgayConChoResponse;
import iuh.fit.se.eclinic.booking.dto.response.NgaySomNhatResponse;
import iuh.fit.se.eclinic.booking.mapper.CaKhamMapper;
import iuh.fit.se.eclinic.booking.repository.KhungGioKhamRepository;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository.SoChoCuaBacSiTheoNgay;
import iuh.fit.se.eclinic.booking.service.TraCuuLichKhamService;
import iuh.fit.se.eclinic.common.entity.scheduling.KhungGioKham;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.enums.TrangThaiKhungGio;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.util.ChiaCaLamViec;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TraCuuLichKhamServiceImpl implements TraCuuLichKhamService {

    private final LichLamViecRepository lichLamViecRepository;
    private final KhungGioKhamRepository khungGioKhamRepository;
    private final CaKhamMapper caKhamMapper;
    private final DatLichProperties datLichProperties;

    /** Số ngày tối đa của 1 lần xem khung giờ nhiều ngày. */
    private static final int SO_NGAY_TOI_DA_MOI_LAN = 7;

    /** Khung giờ gộp của chuyên khoa, cộng dồn khi duyệt khung của từng bác sĩ. */
    private static final class KhungGop {

        private LocalDateTime gioKetThuc;
        private int tongSoCho;
        private int soChoConLai;
        private final Set<Long> idBacSi = new HashSet<>();
    }

    /** Số chỗ của 1 khung 1 giờ, cộng dồn khi duyệt các lượt khám. */
    private static final class DemCho {

        private int tongSoCho;
        private int soChoConLai;
        private boolean conKipDat;
    }

    @Override
    public List<CaKhamResponse> timKhungGioTheoNgay(LocalDate ngay, Long idBacSi, Long idChuyenKhoa) {
        kiemTraBoLoc(idBacSi, idChuyenKhoa);
        LocalDateTime bayGio = LocalDateTime.now();
        LocalDate homNay = bayGio.toLocalDate();
        if (ngay.isBefore(homNay) || ngay.isAfter(ngayXaNhat(homNay))) {
            return List.of();
        }
        return chiaKhungCacCa(lichLamViecRepository.timCaDatLichDuoc(ngay, idBacSi, idChuyenKhoa), mocDatDuoc(bayGio));
    }

    @Override
    public List<CaKhamResponse> timKhungGioNhieuNgay(Long idBacSi, LocalDate tuNgay, LocalDate denNgay) {
        LocalDateTime bayGio = LocalDateTime.now();
        LocalDate homNay = bayGio.toLocalDate();
        LocalDate tu = tuNgay == null ? homNay : tuNgay;
        LocalDate den = denNgay == null ? tu.plusDays(SO_NGAY_TOI_DA_MOI_LAN - 1L) : denNgay;
        if (den.isBefore(tu)) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "denNgay phải từ tuNgay trở đi");
        }
        if (ChronoUnit.DAYS.between(tu, den) >= SO_NGAY_TOI_DA_MOI_LAN) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE,
                    "Mỗi lần chỉ xem được tối đa " + SO_NGAY_TOI_DA_MOI_LAN + " ngày");
        }
        // Cắt về khoảng còn đặt được, như timNgayConCho
        LocalDate ngayXaNhat = ngayXaNhat(homNay);
        if (tu.isBefore(homNay)) {
            tu = homNay;
        }
        if (den.isAfter(ngayXaNhat)) {
            den = ngayXaNhat;
        }
        if (den.isBefore(tu)) {
            return List.of();
        }
        return chiaKhungCacCa(lichLamViecRepository.timCaDatLichDuocTrongKhoang(tu, den, idBacSi),
                mocDatDuoc(bayGio));
    }

    @Override
    public List<KhungGioGopResponse> timKhungGioGop(Long idChuyenKhoa, LocalDate ngay) {
        // TreeMap: kết quả theo giờ bắt đầu
        Map<LocalDateTime, KhungGop> gopTheoGio = new TreeMap<>();
        for (CaKhamResponse ca : timKhungGioTheoNgay(ngay, null, idChuyenKhoa)) {
            for (KhungGioResponse khung : ca.khungGio()) {
                KhungGop gop = gopTheoGio.computeIfAbsent(khung.gioBatDau(), k -> new KhungGop());
                if (gop.gioKetThuc == null || khung.gioKetThuc().isAfter(gop.gioKetThuc)) {
                    gop.gioKetThuc = khung.gioKetThuc();
                }
                gop.tongSoCho += khung.tongSoCho();
                gop.soChoConLai += khung.soChoConLai();
                gop.idBacSi.add(ca.bacSi().id());
            }
        }
        List<KhungGioGopResponse> ketQua = new ArrayList<>();
        gopTheoGio.forEach((gioBatDau, gop) -> ketQua.add(new KhungGioGopResponse(gioBatDau, gop.gioKetThuc,
                gop.tongSoCho, gop.soChoConLai, gop.soChoConLai == 0, gop.idBacSi.size())));
        return ketQua;
    }

    /** Các ca kèm khung 1 giờ; 1 câu query lấy lượt khám của mọi ca. Ca không còn khung nào kịp đặt thì bỏ. */
    private List<CaKhamResponse> chiaKhungCacCa(List<LichLamViec> cacCa, LocalDateTime moc) {
        if (cacCa.isEmpty()) {
            return List.of();
        }
        List<Long> idCacCa = cacCa.stream().map(LichLamViec::getId).toList();
        Map<Long, List<KhungGioKham>> luotTheoCa = khungGioKhamRepository
                .findByLichLamViecIdInAndTrangThaiNotOrderByGioBatDauAsc(idCacCa, TrangThaiKhungGio.DA_HUY).stream()
                .collect(Collectors.groupingBy(luot -> luot.getLichLamViec().getId()));

        List<CaKhamResponse> ketQua = new ArrayList<>();
        for (LichLamViec ca : cacCa) {
            List<KhungGioResponse> khungGio = chiaKhung(ca, luotTheoCa.getOrDefault(ca.getId(), List.of()), moc);
            if (!khungGio.isEmpty()) {
                ketQua.add(caKhamMapper.toResponse(ca, khungGio));
            }
        }
        return ketQua;
    }

    @Override
    public List<NgayConChoResponse> timNgayConCho(LocalDate tuNgay, LocalDate denNgay, Long idBacSi,
            Long idChuyenKhoa) {
        kiemTraBoLoc(idBacSi, idChuyenKhoa);
        if (tuNgay != null && denNgay != null && denNgay.isBefore(tuNgay)) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "denNgay phải từ tuNgay trở đi");
        }
        LocalDateTime bayGio = LocalDateTime.now();
        LocalDate homNay = bayGio.toLocalDate();
        LocalDate ngayXaNhat = ngayXaNhat(homNay);
        LocalDate tu = tuNgay == null || tuNgay.isBefore(homNay) ? homNay : tuNgay;
        LocalDate den = denNgay == null || denNgay.isAfter(ngayXaNhat) ? ngayXaNhat : denNgay;
        if (den.isBefore(tu)) {
            return List.of();
        }
        return lichLamViecRepository.demChoTrongTheoNgay(tu, den, mocDatDuoc(bayGio), idBacSi, idChuyenKhoa).stream()
                .map(dong -> new NgayConChoResponse(dong.getNgay(), dong.getSoChoConLai()))
                .toList();
    }

    @Override
    public List<NgaySomNhatResponse> timNgaySomNhat(Long idChuyenKhoa) {
        LocalDateTime bayGio = LocalDateTime.now();
        LocalDate homNay = bayGio.toLocalDate();
        List<NgaySomNhatResponse> ketQua = new ArrayList<>();
        Long idBacSiTruoc = null;
        // Đã sắp theo bác sĩ rồi theo ngày: chỉ lấy dòng đầu tiên của mỗi bác sĩ
        for (SoChoCuaBacSiTheoNgay dong : lichLamViecRepository.demChoTrongTheoBacSiVaNgay(homNay, ngayXaNhat(homNay),
                mocDatDuoc(bayGio), idChuyenKhoa)) {
            if (!dong.getIdBacSi().equals(idBacSiTruoc)) {
                ketQua.add(new NgaySomNhatResponse(dong.getIdBacSi(), dong.getNgay(), dong.getSoChoConLai()));
                idBacSiTruoc = dong.getIdBacSi();
            }
        }
        return ketQua;
    }

    /**
     * Gom các lượt khám của ca (đã sắp theo giờ bắt đầu) thành khung 1 giờ. Lượt bắt đầu trước {@code moc} không
     * còn kịp đặt: vẫn tính vào tổng số chỗ của khung nhưng không tính là chỗ trống. Khung không còn lượt nào
     * kịp đặt thì bỏ.
     */
    private List<KhungGioResponse> chiaKhung(LichLamViec ca, List<KhungGioKham> cacLuot, LocalDateTime moc) {
        Map<LocalTime, DemCho> demTheoKhung = new LinkedHashMap<>();
        for (KhungGioKham luot : cacLuot) {
            LocalTime gioBatDauKhung = ChiaCaLamViec.gioBatDauKhung(ca.getGioBatDau(),
                    luot.getGioBatDau().toLocalTime());
            DemCho dem = demTheoKhung.computeIfAbsent(gioBatDauKhung, k -> new DemCho());
            dem.tongSoCho++;
            if (!luot.getGioBatDau().isBefore(moc)) {
                dem.conKipDat = true;
                if (luot.getTrangThai() == TrangThaiKhungGio.CON_TRONG) {
                    dem.soChoConLai++;
                }
            }
        }
        LocalDate ngay = ca.getNgayLamViec();
        List<KhungGioResponse> ketQua = new ArrayList<>();
        demTheoKhung.forEach((gioBatDauKhung, dem) -> {
            if (dem.conKipDat) {
                ketQua.add(new KhungGioResponse(ngay.atTime(gioBatDauKhung),
                        ngay.atTime(ChiaCaLamViec.gioKetThucKhung(gioBatDauKhung, ca.getGioKetThuc())),
                        dem.tongSoCho, dem.soChoConLai, dem.soChoConLai == 0));
            }
        });
        return ketQua;
    }

    private static void kiemTraBoLoc(Long idBacSi, Long idChuyenKhoa) {
        if (idBacSi == null && idChuyenKhoa == null) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "Cần truyền idBacSi hoặc idChuyenKhoa");
        }
    }

    /** Lượt khám bắt đầu từ thời điểm này trở đi mới còn kịp đặt. */
    private LocalDateTime mocDatDuoc(LocalDateTime bayGio) {
        return bayGio.plus(datLichProperties.datTruocToiThieu());
    }

    private LocalDate ngayXaNhat(LocalDate homNay) {
        return homNay.plusDays(datLichProperties.soNgayDatTruocToiDa());
    }

}
