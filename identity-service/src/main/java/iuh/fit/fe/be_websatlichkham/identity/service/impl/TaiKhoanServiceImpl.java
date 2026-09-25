package iuh.fit.fe.be_websatlichkham.identity.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.entity.identity.TaiKhoan;
import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.identity.repository.TaiKhoanRepository;
import iuh.fit.fe.be_websatlichkham.identity.service.TaiKhoanService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaiKhoanServiceImpl implements TaiKhoanService {

    private final TaiKhoanRepository taiKhoanRepository;

    @Override
    public TaiKhoan getById(Long id) {
        return taiKhoanRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("TaiKhoan", id));
    }

    @Override
    public Optional<TaiKhoan> findByEmail(String email) {
        return taiKhoanRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return taiKhoanRepository.existsByEmail(email);
    }

}
