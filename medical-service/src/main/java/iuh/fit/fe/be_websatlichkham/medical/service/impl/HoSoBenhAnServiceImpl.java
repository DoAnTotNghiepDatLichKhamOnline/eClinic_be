package iuh.fit.fe.be_websatlichkham.medical.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.entity.medical.HoSoBenhAn;
import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.medical.repository.HoSoBenhAnRepository;
import iuh.fit.fe.be_websatlichkham.medical.service.HoSoBenhAnService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoSoBenhAnServiceImpl implements HoSoBenhAnService {

    private final HoSoBenhAnRepository hoSoBenhAnRepository;

    @Override
    public HoSoBenhAn getById(Long id) {
        return hoSoBenhAnRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("HoSoBenhAn", id));
    }

    @Override
    public Optional<HoSoBenhAn> findByLichHen(Long lichHenId) {
        return hoSoBenhAnRepository.findByLichHenId(lichHenId);
    }

    @Override
    public List<HoSoBenhAn> findByHoSoBenhNhan(Long hoSoBenhNhanId) {
        return hoSoBenhAnRepository.findByHoSoBenhNhanIdOrderByNgayTaoDesc(hoSoBenhNhanId);
    }

}
