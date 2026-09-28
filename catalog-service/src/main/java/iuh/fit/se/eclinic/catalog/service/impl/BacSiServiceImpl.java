package iuh.fit.se.eclinic.catalog.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.catalog.repository.BacSiRepository;
import iuh.fit.se.eclinic.catalog.service.BacSiService;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BacSiServiceImpl implements BacSiService {

    private final BacSiRepository bacSiRepository;

    @Override
    public BacSi layTheoId(Long id) {
        return bacSiRepository.findById(id).orElseThrow(() -> new LoiKhongTimThay("BacSi", id));
    }

    @Override
    public Optional<BacSi> timTheoTaiKhoanId(Long taiKhoanId) {
        return bacSiRepository.findByTaiKhoanId(taiKhoanId);
    }

    @Override
    public List<BacSi> timTheoChuyenKhoa(Long chuyenKhoaId) {
        return bacSiRepository.findByChuyenKhoaId(chuyenKhoaId);
    }

}
