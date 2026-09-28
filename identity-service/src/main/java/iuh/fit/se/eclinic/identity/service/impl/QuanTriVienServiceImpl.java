package iuh.fit.se.eclinic.identity.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.identity.repository.QuanTriVienRepository;
import iuh.fit.se.eclinic.identity.service.QuanTriVienService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuanTriVienServiceImpl implements QuanTriVienService {

    private final QuanTriVienRepository quanTriVienRepository;

    @Override
    public QuanTriVien layTheoId(Long id) {
        return quanTriVienRepository.findById(id).orElseThrow(() -> new LoiKhongTimThay("QuanTriVien", id));
    }

    @Override
    public Optional<QuanTriVien> timTheoTaiKhoanId(Long taiKhoanId) {
        return quanTriVienRepository.findByTaiKhoanId(taiKhoanId);
    }

}
