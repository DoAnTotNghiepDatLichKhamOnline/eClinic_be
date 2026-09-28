package iuh.fit.se.eclinic.catalog.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.catalog.dto.request.ChuyenKhoaRequest;
import iuh.fit.se.eclinic.catalog.dto.response.ChuyenKhoaResponse;
import iuh.fit.se.eclinic.catalog.mapper.ChuyenKhoaMapper;
import iuh.fit.se.eclinic.catalog.repository.BacSiRepository;
import iuh.fit.se.eclinic.catalog.repository.ChuyenKhoaRepository;
import iuh.fit.se.eclinic.catalog.repository.PhongKhamRepository;
import iuh.fit.se.eclinic.catalog.service.ChuyenKhoaService;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChuyenKhoaServiceImpl implements ChuyenKhoaService {

    private final ChuyenKhoaRepository chuyenKhoaRepository;
    private final PhongKhamRepository phongKhamRepository;
    private final BacSiRepository bacSiRepository;
    private final ChuyenKhoaMapper chuyenKhoaMapper;

    @Override
    public TrangDuLieu<ChuyenKhoaResponse> timKiem(String tuKhoa, int trang, int kichThuoc) {
        Pageable pageable = PageRequest.of(trang, kichThuoc, Sort.by("tenChuyenKhoa"));
        Page<ChuyenKhoa> ketQua = tuKhoa == null || tuKhoa.isBlank()
                ? chuyenKhoaRepository.findAll(pageable)
                : chuyenKhoaRepository.findByTenChuyenKhoaContaining(tuKhoa.trim(), pageable);
        return TrangDuLieu.tu(ketQua.map(chuyenKhoaMapper::toResponse));
    }

    @Override
    public ChuyenKhoaResponse layTheoId(Long id) {
        return chuyenKhoaMapper.toResponse(timEntity(id));
    }

    @Override
    @Transactional
    public ChuyenKhoaResponse tao(ChuyenKhoaRequest request) {
        ChuyenKhoa chuyenKhoa = chuyenKhoaMapper.toEntity(request);
        if (chuyenKhoaRepository.existsByTenChuyenKhoa(chuyenKhoa.getTenChuyenKhoa())) {
            throw new LoiNghiepVu(MaLoi.TEN_CHUYEN_KHOA_DA_TON_TAI);
        }
        return chuyenKhoaMapper.toResponse(chuyenKhoaRepository.save(chuyenKhoa));
    }

    @Override
    @Transactional
    public ChuyenKhoaResponse capNhat(Long id, ChuyenKhoaRequest request) {
        ChuyenKhoa chuyenKhoa = timEntity(id);
        // Kiểm tra TRƯỚC khi sửa entity: entity đang được quản lý, query sau khi sửa sẽ làm Hibernate
        // flush UPDATE trước và lỗi unique key của DB bật ra trước khi kịp kiểm tra.
        if (chuyenKhoaRepository.existsByTenChuyenKhoaAndIdNot(request.tenChuyenKhoa().trim(), id)) {
            throw new LoiNghiepVu(MaLoi.TEN_CHUYEN_KHOA_DA_TON_TAI);
        }
        chuyenKhoaMapper.capNhat(chuyenKhoa, request);
        return chuyenKhoaMapper.toResponse(chuyenKhoa);
    }

    @Override
    @Transactional
    public void xoa(Long id) {
        ChuyenKhoa chuyenKhoa = timEntity(id);
        long soPhongKham = phongKhamRepository.countByChuyenKhoaId(id);
        long soBacSi = bacSiRepository.countByChuyenKhoaId(id);
        if (soPhongKham > 0 || soBacSi > 0) {
            throw new LoiNghiepVu(MaLoi.CHUYEN_KHOA_DANG_DUOC_SU_DUNG, "Chuyên khoa đang có " + soPhongKham
                    + " phòng khám và " + soBacSi + " bác sĩ, không thể xoá");
        }
        chuyenKhoaRepository.delete(chuyenKhoa);
    }

    private ChuyenKhoa timEntity(Long id) {
        return chuyenKhoaRepository.findById(id).orElseThrow(() -> new LoiKhongTimThay("ChuyenKhoa", id));
    }

}
