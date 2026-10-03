package iuh.fit.se.eclinic.catalog.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.catalog.repository.PhongKhamRepository;
import iuh.fit.se.eclinic.catalog.service.PhongKhamService;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiPhongKham;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhongKhamServiceImpl implements PhongKhamService {

    private final PhongKhamRepository phongKhamRepository;

    @Override
    public PhongKham layTheoId(Long id) {
        return phongKhamRepository.findById(id).orElseThrow(() -> new LoiKhongTimThay("PhongKham", id));
    }

    @Override
    public List<PhongKham> layDangHoatDong() {
        return phongKhamRepository.findByTrangThaiOrderByTenPhongAsc(TrangThaiPhongKham.HOAT_DONG);
    }

    @Override
    public List<PhongKham> layDangHoatDongTheoChuyenKhoa(Long chuyenKhoaId) {
        return phongKhamRepository.findByChuyenKhoaIdAndTrangThaiOrderByTenPhongAsc(chuyenKhoaId,
                TrangThaiPhongKham.HOAT_DONG);
    }

}
