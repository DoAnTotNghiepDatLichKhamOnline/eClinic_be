package iuh.fit.fe.be_websatlichkham.booking.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.booking.repository.LichHenRepository;
import iuh.fit.fe.be_websatlichkham.booking.service.LichHenService;
import iuh.fit.fe.be_websatlichkham.common.entity.booking.LichHen;
import iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiLichHen;
import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LichHenServiceImpl implements LichHenService {

    private final LichHenRepository lichHenRepository;

    @Override
    public LichHen getById(Long id) {
        return lichHenRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("LichHen", id));
    }

    @Override
    public List<LichHen> findByHoSoBenhNhan(Long hoSoBenhNhanId) {
        return lichHenRepository.findByHoSoBenhNhanIdOrderByNgayTaoDesc(hoSoBenhNhanId);
    }

    @Override
    public List<LichHen> findByBacSiAndTrangThai(Long bacSiId, TrangThaiLichHen trangThai) {
        return lichHenRepository.findByBacSiIdAndTrangThaiOrderByNgayTaoDesc(bacSiId, trangThai);
    }

}
