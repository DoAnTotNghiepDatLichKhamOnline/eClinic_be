package iuh.fit.se.eclinic.booking.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.service.LichHenService;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LichHenServiceImpl implements LichHenService {

    private final LichHenRepository lichHenRepository;

    @Override
    public LichHen layTheoId(Long id) {
        return lichHenRepository.findById(id).orElseThrow(() -> new LoiKhongTimThay("LichHen", id));
    }

    @Override
    public List<LichHen> timTheoHoSoBenhNhan(Long hoSoBenhNhanId) {
        return lichHenRepository.findByHoSoBenhNhanIdOrderByNgayTaoDesc(hoSoBenhNhanId);
    }

    @Override
    public List<LichHen> timTheoBacSiVaTrangThai(Long bacSiId, TrangThaiLichHen trangThai) {
        return lichHenRepository.findByBacSiIdAndTrangThaiOrderByNgayTaoDesc(bacSiId, trangThai);
    }

}
