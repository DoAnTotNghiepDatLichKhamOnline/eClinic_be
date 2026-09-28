package iuh.fit.se.eclinic.booking.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.repository.HoSoBenhNhanRepository;
import iuh.fit.se.eclinic.booking.service.HoSoBenhNhanService;
import iuh.fit.se.eclinic.common.entity.booking.HoSoBenhNhan;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoSoBenhNhanServiceImpl implements HoSoBenhNhanService {

    private final HoSoBenhNhanRepository hoSoBenhNhanRepository;

    @Override
    public HoSoBenhNhan layTheoId(Long id) {
        return hoSoBenhNhanRepository.findById(id)
                .orElseThrow(() -> new LoiKhongTimThay("HoSoBenhNhan", id));
    }

    @Override
    public Optional<HoSoBenhNhan> timTheoTaiKhoanId(Long taiKhoanId) {
        return hoSoBenhNhanRepository.findByTaiKhoanId(taiKhoanId);
    }

    @Override
    public Optional<HoSoBenhNhan> timTheoCccd(String cccd) {
        return hoSoBenhNhanRepository.findByCccd(cccd);
    }

}
