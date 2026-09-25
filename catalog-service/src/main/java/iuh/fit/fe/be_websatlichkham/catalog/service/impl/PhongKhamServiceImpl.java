package iuh.fit.fe.be_websatlichkham.catalog.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.catalog.repository.PhongKhamRepository;
import iuh.fit.fe.be_websatlichkham.catalog.service.PhongKhamService;
import iuh.fit.fe.be_websatlichkham.common.entity.catalog.PhongKham;
import iuh.fit.fe.be_websatlichkham.common.enums.TrangThaiPhongKham;
import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhongKhamServiceImpl implements PhongKhamService {

    private final PhongKhamRepository phongKhamRepository;

    @Override
    public PhongKham getById(Long id) {
        return phongKhamRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("PhongKham", id));
    }

    @Override
    public List<PhongKham> findActive() {
        return phongKhamRepository.findByTrangThaiOrderByTenPhongAsc(TrangThaiPhongKham.HOAT_DONG);
    }

    @Override
    public List<PhongKham> findActiveByChuyenKhoa(Long chuyenKhoaId) {
        return phongKhamRepository.findByChuyenKhoaIdAndTrangThaiOrderByTenPhongAsc(chuyenKhoaId,
                TrangThaiPhongKham.HOAT_DONG);
    }

}
