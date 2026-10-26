package iuh.fit.se.eclinic.medical.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.medical.repository.HoSoBenhAnRepository;
import iuh.fit.se.eclinic.medical.service.HoSoBenhAnService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoSoBenhAnServiceImpl implements HoSoBenhAnService {

    private final HoSoBenhAnRepository hoSoBenhAnRepository;

    @Override
    public HoSoBenhAn layTheoId(Long id) {
        return hoSoBenhAnRepository.findById(id).orElseThrow(() -> new LoiKhongTimThay("HoSoBenhAn", id));
    }

    @Override
    public Optional<HoSoBenhAn> timTheoLichHen(Long lichHenId) {
        return hoSoBenhAnRepository.findByLichHenId(lichHenId);
    }

    @Override
    public List<HoSoBenhAn> timTheoHoSoBenhNhan(Long hoSoBenhNhanId) {
        return hoSoBenhAnRepository.findByHoSoBenhNhanIdOrderByNgayTaoDesc(hoSoBenhNhanId);
    }

}
