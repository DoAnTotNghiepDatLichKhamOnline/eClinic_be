package iuh.fit.fe.be_websatlichkham.identity.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.entity.identity.QuanTriVien;
import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.identity.repository.QuanTriVienRepository;
import iuh.fit.fe.be_websatlichkham.identity.service.QuanTriVienService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuanTriVienServiceImpl implements QuanTriVienService {

    private final QuanTriVienRepository quanTriVienRepository;

    @Override
    public QuanTriVien getById(Long id) {
        return quanTriVienRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("QuanTriVien", id));
    }

    @Override
    public Optional<QuanTriVien> findByTaiKhoanId(Long taiKhoanId) {
        return quanTriVienRepository.findByTaiKhoanId(taiKhoanId);
    }

}
