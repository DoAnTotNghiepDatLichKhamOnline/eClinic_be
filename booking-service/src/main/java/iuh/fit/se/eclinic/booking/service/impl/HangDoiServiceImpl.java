package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.response.HangDoiResponse;
import iuh.fit.se.eclinic.booking.dto.response.HangDoiResponse.CaHangDoi;
import iuh.fit.se.eclinic.booking.dto.response.HangDoiResponse.LuotHangDoi;
import iuh.fit.se.eclinic.booking.dto.response.HangDoiResponse.SoLuong;
import iuh.fit.se.eclinic.booking.mapper.CaKhamMapper;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.repository.BacSiChiDocRepository;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository;
import iuh.fit.se.eclinic.booking.service.HangDoiService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.enums.BuoiKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiHangDoi;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Thao tác hàng đợi khoá dòng bác sĩ trước, nên các lần bấm cùng lúc chạy lần lượt. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HangDoiServiceImpl implements HangDoiService {

    /** Lịch hẹn có mặt trong hàng đợi (DA_HOAN_THANH hiện là đã khám, KHONG_DEN của ngày đã qua hiện là vắng mặt). */
    private static final Set<TrangThaiLichHen> TRONG_HANG_DOI = EnumSet.of(TrangThaiLichHen.DA_XAC_NHAN,
            TrangThaiLichHen.DA_HOAN_THANH, TrangThaiLichHen.KHONG_DEN);
    /** Lịch hẹn còn khám được (như KhamBenhServiceImpl ở medical-service). */
    private static final Set<TrangThaiLichHen> KHAM_DUOC = EnumSet.of(TrangThaiLichHen.DA_XAC_NHAN);

    private final TaiKhoanService taiKhoanService;
    private final BacSiChiDocRepository bacSiChiDocRepository;
    private final LichLamViecRepository lichLamViecRepository;
    private final LichHenRepository lichHenRepository;
    private final LichHenMapper lichHenMapper;
    private final CaKhamMapper caKhamMapper;

    @Override
    public HangDoiResponse hangDoi(Long idTaiKhoanBacSi, LocalDate ngay) {
        BacSi bacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi);
        return docHangDoi(bacSi.getId(), ngay == null ? LocalDate.now() : ngay);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public HangDoiResponse goiTiep(Long idTaiKhoanBacSi, Long idLichLamViec) {
        BacSi bacSi = khoaBacSi(idTaiKhoanBacSi);
        LichLamViec ca = lichLamViecRepository.findById(idLichLamViec)
                .filter(timThay -> timThay.getBacSi().getId().equals(bacSi.getId())
                        && timThay.getTrangThai() == TrangThaiLichLamViec.HOAT_DONG)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.caLamViec"));
        if (!ca.getNgayLamViec().equals(LocalDate.now())) {
            throw new LoiNghiepVu(MaLoi.KHONG_PHAI_NGAY_KHAM);
        }
        Long idTiepTheo = lichHenRepository
                .timIdChoKhamCuaCa(idLichLamViec, TrangThaiHangDoi.CHO_KHAM, KHAM_DUOC, Limit.of(1))
                .stream().findFirst()
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.HANG_DOI_TRONG));
        goiVaoKham(bacSi, khoaLichHenHomNay(bacSi, idTiepTheo));
        return docHangDoi(bacSi.getId(), LocalDate.now());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public HangDoiResponse goi(Long idTaiKhoanBacSi, Long idLichHen) {
        BacSi bacSi = khoaBacSi(idTaiKhoanBacSi);
        goiVaoKham(bacSi, khoaLichHenHomNay(bacSi, idLichHen));
        return docHangDoi(bacSi.getId(), LocalDate.now());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public HangDoiResponse vangMat(Long idTaiKhoanBacSi, Long idLichHen) {
        BacSi bacSi = khoaBacSi(idTaiKhoanBacSi);
        LichHen lichHen = khoaLichHenHomNay(bacSi, idLichHen);
        if (lichHen.getTrangThaiHangDoi() != TrangThaiHangDoi.VANG_MAT) {
            lichHen.setTrangThaiHangDoi(TrangThaiHangDoi.VANG_MAT);
            log.info("Bác sĩ id={} đánh dấu vắng mặt lịch hẹn id={}", bacSi.getId(), idLichHen);
        }
        return docHangDoi(bacSi.getId(), LocalDate.now());
    }

    private BacSi khoaBacSi(Long idTaiKhoanBacSi) {
        Long idBacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi).getId();
        return bacSiChiDocRepository.khoaTheoId(idBacSi)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_CO_QUYEN));
    }

    /** Khoá lịch hẹn của bác sĩ, còn khám được và khám hôm nay. Không có / của bác sĩ khác trả cùng 1 lỗi. */
    private LichHen khoaLichHenHomNay(BacSi bacSi, Long idLichHen) {
        LichHen lichHen = lichHenRepository.findByIdForUpdate(idLichHen)
                .filter(timThay -> timThay.getBacSi().getId().equals(bacSi.getId()))
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.lichHen"));
        if (!KHAM_DUOC.contains(lichHen.getTrangThai()) || lichHen.isCanDoiLich()) {
            throw new LoiNghiepVu(MaLoi.LICH_HEN_KHONG_KHAM_DUOC);
        }
        if (!lichHen.getKhungGio().getGioBatDau().toLocalDate().equals(LocalDate.now())) {
            throw new LoiNghiepVu(MaLoi.KHONG_PHAI_NGAY_KHAM);
        }
        return lichHen;
    }

    /** Đang khám chính bệnh nhân này thì giữ nguyên (bấm lại không đổi giờ gọi). */
    private void goiVaoKham(BacSi bacSi, LichHen lichHen) {
        if (lichHen.getTrangThaiHangDoi() == TrangThaiHangDoi.DANG_KHAM) {
            return;
        }
        LocalDate homNay = LocalDate.now();
        lichHenRepository.timDangKhamCuaBacSi(bacSi.getId(), TrangThaiHangDoi.DANG_KHAM, KHAM_DUOC,
                homNay.atStartOfDay(), homNay.plusDays(1).atStartOfDay(), lichHen.getId())
                .stream().findFirst()
                .ifPresent(dangKham -> {
                    throw new LoiNghiepVu(MaLoi.DANG_KHAM_BENH_NHAN_KHAC, "loi.DANG_KHAM_BENH_NHAN_KHAC.ten",
                            dangKham.getHoSoBenhNhan().getHoTen(), dangKham.getSoThuTu());
                });
        lichHen.setTrangThaiHangDoi(TrangThaiHangDoi.DANG_KHAM);
        // Cắt về micro giây như cột DATETIME(6): phản hồi ngay sau khi gọi và các lần đọc sau trả cùng 1 giá trị
        lichHen.setGoiLuc(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        log.info("Bác sĩ id={} gọi lịch hẹn id={} vào khám", bacSi.getId(), lichHen.getId());
    }

    private HangDoiResponse docHangDoi(Long idBacSi, LocalDate ngay) {
        LocalDateTime bayGio = LocalDateTime.now();
        boolean homNay = ngay.equals(bayGio.toLocalDate());
        List<LichLamViec> cacCa = lichLamViecRepository
                .findByBacSiIdAndNgayLamViecBetweenOrderByNgayLamViecAscGioBatDauAsc(idBacSi, ngay, ngay).stream()
                .filter(ca -> ca.getTrangThai() == TrangThaiLichLamViec.HOAT_DONG)
                .toList();
        Map<Long, List<LichHen>> theoCa = lichHenRepository
                .timCuaBacSiTrongKhoang(idBacSi, ngay.atStartOfDay(), ngay.plusDays(1).atStartOfDay()).stream()
                .filter(lichHen -> TRONG_HANG_DOI.contains(lichHen.getTrangThai()) && !lichHen.isCanDoiLich())
                .sorted(Comparator.comparing(LichHen::getSoThuTu).thenComparing(LichHen::getId))
                .collect(Collectors.groupingBy(lichHen -> lichHen.getKhungGio().getLichLamViec().getId()));
        List<CaHangDoi> ketQua = cacCa.stream()
                .map(ca -> toCaHangDoi(ca, theoCa.getOrDefault(ca.getId(), List.of()), homNay))
                .toList();
        return new HangDoiResponse(ngay, caHienTai(cacCa, homNay, bayGio), bayGio, ketQua);
    }

    private CaHangDoi toCaHangDoi(LichLamViec ca, List<LichHen> cacLichHen, boolean homNay) {
        Map<TrangThaiHangDoi, Long> dem = cacLichHen.stream()
                .collect(Collectors.groupingBy(LichHen::hangDoiHienThi, Collectors.counting()));
        SoLuong soLuong = new SoLuong(cacLichHen.size(), demTheo(dem, TrangThaiHangDoi.CHO_KHAM),
                demTheo(dem, TrangThaiHangDoi.DANG_KHAM), demTheo(dem, TrangThaiHangDoi.VANG_MAT),
                demTheo(dem, TrangThaiHangDoi.DA_KHAM));
        Long idDangKham = idDauTien(cacLichHen, TrangThaiHangDoi.DANG_KHAM);
        Long idTiepTheo = homNay ? idDauTien(cacLichHen, TrangThaiHangDoi.CHO_KHAM) : null;
        List<LuotHangDoi> cacLuot = cacLichHen.stream()
                .map(lichHen -> new LuotHangDoi(lichHen.hangDoiHienThi(), lichHen.getGoiLuc(),
                        lichHenMapper.toTrongCaResponse(lichHen, false)))
                .toList();
        return new CaHangDoi(ca.getId(), BuoiKham.cua(ca.getGioBatDau()), ca.getGioBatDau(), ca.getGioKetThuc(),
                caKhamMapper.toPhongKhamTomTat(ca.getPhongKham()), soLuong, idDangKham, idTiepTheo, cacLuot);
    }

    private static int demTheo(Map<TrangThaiHangDoi, Long> dem, TrangThaiHangDoi trangThai) {
        return dem.getOrDefault(trangThai, 0L).intValue();
    }

    private static Long idDauTien(List<LichHen> cacLichHen, TrangThaiHangDoi trangThai) {
        return cacLichHen.stream()
                .filter(lichHen -> lichHen.hangDoiHienThi() == trangThai)
                .map(LichHen::getId)
                .findFirst()
                .orElse(null);
    }

    /** Hôm nay: ca đang diễn ra, không có thì ca kế tiếp, không có nữa thì ca cuối. Ngày khác: ca đầu tiên. */
    private static Long caHienTai(List<LichLamViec> cacCa, boolean homNay, LocalDateTime bayGio) {
        if (cacCa.isEmpty()) {
            return null;
        }
        if (!homNay) {
            return cacCa.get(0).getId();
        }
        return cacCa.stream()
                .filter(ca -> bayGio.toLocalTime().isBefore(ca.getGioKetThuc()))
                .findFirst()
                .orElse(cacCa.get(cacCa.size() - 1))
                .getId();
    }

}
