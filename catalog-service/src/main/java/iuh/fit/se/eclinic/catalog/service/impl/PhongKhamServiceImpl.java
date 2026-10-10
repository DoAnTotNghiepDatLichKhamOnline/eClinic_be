package iuh.fit.se.eclinic.catalog.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.catalog.repository.PhongKhamRepository;
import iuh.fit.se.eclinic.catalog.service.PhongKhamService;
import iuh.fit.se.eclinic.catalog.dto.response.PhongKhamResponse;
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

    @Override
    public List<PhongKhamResponse> danhSachDangHoatDong(Long idChuyenKhoa) {
        // Trong transaction chỉ đọc của class: tên chuyên khoa (quan hệ lazy) được tải tại đây
        return (idChuyenKhoa == null ? layDangHoatDong() : layDangHoatDongTheoChuyenKhoa(idChuyenKhoa)).stream()
                .map(phong -> new PhongKhamResponse(phong.getId(), phong.getTenPhong(), phong.getTang(),
                        phong.getChuyenKhoa().getId(), phong.getChuyenKhoa().getTenChuyenKhoa()))
                .toList();
    }

}
