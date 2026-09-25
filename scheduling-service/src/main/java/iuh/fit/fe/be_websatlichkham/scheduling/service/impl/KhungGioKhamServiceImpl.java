package iuh.fit.fe.be_websatlichkham.scheduling.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.entity.scheduling.KhungGioKham;
import iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiKhungGio;
import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.scheduling.repository.KhungGioKhamRepository;
import iuh.fit.fe.be_websatlichkham.scheduling.service.KhungGioKhamService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KhungGioKhamServiceImpl implements KhungGioKhamService {

    private final KhungGioKhamRepository khungGioKhamRepository;

    @Override
    public KhungGioKham getById(Long id) {
        return khungGioKhamRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("KhungGioKham", id));
    }

    @Override
    public List<KhungGioKham> findByLichLamViec(Long lichLamViecId) {
        return khungGioKhamRepository.findByLichLamViecIdOrderByGioBatDauAsc(lichLamViecId);
    }

    @Override
    public List<KhungGioKham> findConTrongByBacSi(Long bacSiId, LocalDateTime tu, LocalDateTime den) {
        return khungGioKhamRepository.findByBacSiAndTrangThaiInRange(bacSiId, TrangThaiKhungGio.CON_TRONG, tu, den);
    }

}
