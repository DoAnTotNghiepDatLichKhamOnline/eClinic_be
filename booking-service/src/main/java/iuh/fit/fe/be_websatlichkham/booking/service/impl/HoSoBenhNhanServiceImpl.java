package iuh.fit.fe.be_websatlichkham.booking.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.booking.repository.HoSoBenhNhanRepository;
import iuh.fit.fe.be_websatlichkham.booking.service.HoSoBenhNhanService;
import iuh.fit.fe.be_websatlichkham.common.entity.booking.HoSoBenhNhan;
import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoSoBenhNhanServiceImpl implements HoSoBenhNhanService {

    private final HoSoBenhNhanRepository hoSoBenhNhanRepository;

    @Override
    public HoSoBenhNhan getById(Long id) {
        return hoSoBenhNhanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HoSoBenhNhan", id));
    }

    @Override
    public Optional<HoSoBenhNhan> findByTaiKhoanId(Long taiKhoanId) {
        return hoSoBenhNhanRepository.findByTaiKhoanId(taiKhoanId);
    }

    @Override
    public Optional<HoSoBenhNhan> findByCccd(String cccd) {
        return hoSoBenhNhanRepository.findByCccd(cccd);
    }

}
