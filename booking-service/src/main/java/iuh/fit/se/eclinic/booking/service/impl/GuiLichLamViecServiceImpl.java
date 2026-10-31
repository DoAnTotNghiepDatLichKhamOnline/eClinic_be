package iuh.fit.se.eclinic.booking.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.request.GuiLichRequest;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaGuiLichResponse;
import iuh.fit.se.eclinic.booking.dto.response.KetQuaGuiLichResponse.DaGui;
import iuh.fit.se.eclinic.booking.mapper.CaKhamMapper;
import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository;
import iuh.fit.se.eclinic.booking.service.GuiLichLamViecService;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichLamViecService;
import iuh.fit.se.eclinic.booking.util.KhoangNgay;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichLamViec;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Chỉ ghi các dòng chờ gửi vào hàng chờ gửi (SuKienThongBao); GuiThongBaoJob chuyển sang notification-service. */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuiLichLamViecServiceImpl implements GuiLichLamViecService {

    private final LichLamViecRepository lichLamViecRepository;
    private final ThongBaoLichLamViecService thongBaoLichLamViecService;
    private final CaKhamMapper caKhamMapper;

    @Override
    @Transactional
    public KetQuaGuiLichResponse gui(Long idTaiKhoanQuanTri, GuiLichRequest request) {
        KhoangNgay.kiemTra(request.tuNgay(), request.denNgay());
        LocalDate homNay = LocalDate.now();
        // Ca đã qua không cần gửi
        LocalDate tuNgay = request.tuNgay().isBefore(homNay) ? homNay : request.tuNgay();
        Set<Long> chiBacSi = request.cacIdBacSi() == null ? Set.of() : new HashSet<>(request.cacIdBacSi());
        Map<Long, List<LichLamViec>> theoBacSi = new LinkedHashMap<>();
        if (!tuNgay.isAfter(request.denNgay())) {
            lichLamViecRepository.timTrongKhoang(tuNgay, request.denNgay(), request.idChuyenKhoa(), null, null).stream()
                    .filter(ca -> ca.getTrangThai() == TrangThaiLichLamViec.HOAT_DONG)
                    .filter(ca -> chiBacSi.isEmpty() || chiBacSi.contains(ca.getBacSi().getId()))
                    .forEach(ca -> theoBacSi.computeIfAbsent(ca.getBacSi().getId(), k -> new ArrayList<>()).add(ca));
        }
        if (theoBacSi.isEmpty()) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.guiLichKhongCoCa");
        }
        List<DaGui> daGui = new ArrayList<>();
        int soCa = 0;
        for (List<LichLamViec> cacCa : theoBacSi.values()) {
            thongBaoLichLamViecService.guiLich(cacCa.get(0).getBacSi(), tuNgay, request.denNgay(), cacCa);
            long soPhut = cacCa.stream()
                    .mapToLong(ca -> Duration.between(ca.getGioBatDau(), ca.getGioKetThuc()).toMinutes()).sum();
            daGui.add(new DaGui(caKhamMapper.toBacSiTomTat(cacCa.get(0).getBacSi()), cacCa.size(), soPhut));
            soCa += cacCa.size();
        }
        log.info("Quản trị viên id={} gửi lịch làm việc {} - {}: {} bác sĩ, {} ca", idTaiKhoanQuanTri, tuNgay,
                request.denNgay(), daGui.size(), soCa);
        return new KetQuaGuiLichResponse(daGui.size(), soCa, daGui);
    }

}
