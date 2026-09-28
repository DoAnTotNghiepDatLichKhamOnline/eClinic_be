package iuh.fit.se.eclinic.identity.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.identity.TaiKhoan;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.identity.repository.TaiKhoanRepository;
import iuh.fit.se.eclinic.identity.service.TaiKhoanService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaiKhoanServiceImpl implements TaiKhoanService {

    private final TaiKhoanRepository taiKhoanRepository;

    @Override
    public TaiKhoan layTheoId(Long id) {
        return taiKhoanRepository.findById(id).orElseThrow(() -> new LoiKhongTimThay("TaiKhoan", id));
    }

    @Override
    public Optional<TaiKhoan> timTheoEmail(String email) {
        return taiKhoanRepository.findByEmail(email);
    }

    @Override
    public boolean tonTaiEmail(String email) {
        return taiKhoanRepository.existsByEmail(email);
    }

}
