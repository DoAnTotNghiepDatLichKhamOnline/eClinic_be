package iuh.fit.fe.be_websatlichkham.catalog.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.catalog.repository.BacSiRepository;
import iuh.fit.fe.be_websatlichkham.catalog.service.BacSiService;
import iuh.fit.fe.be_websatlichkham.common.entity.catalog.BacSi;
import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BacSiServiceImpl implements BacSiService {

    private final BacSiRepository bacSiRepository;

    @Override
    public BacSi getById(Long id) {
        return bacSiRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("BacSi", id));
    }

    @Override
    public Optional<BacSi> findByTaiKhoanId(Long taiKhoanId) {
        return bacSiRepository.findByTaiKhoanId(taiKhoanId);
    }

    @Override
    public List<BacSi> findByChuyenKhoa(Long chuyenKhoaId) {
        return bacSiRepository.findByChuyenKhoaId(chuyenKhoaId);
    }

}
