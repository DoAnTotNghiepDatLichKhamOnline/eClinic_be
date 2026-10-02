package iuh.fit.se.eclinic.catalog.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.catalog.dto.response.BacSiChiTietResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiResponse;
import iuh.fit.se.eclinic.catalog.mapper.BacSiMapper;
import iuh.fit.se.eclinic.catalog.repository.BacSiRepository;
import iuh.fit.se.eclinic.catalog.service.BacSiService;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BacSiServiceImpl implements BacSiService {

    private final BacSiRepository bacSiRepository;
    private final BacSiMapper bacSiMapper;

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

    @Override
    public TrangDuLieu<BacSiResponse> timKiem(Long idChuyenKhoa, String tuKhoa, int trang, int kichThuoc) {
        // Thứ tự sắp xếp nằm trong câu query (theo họ tên ở bảng tai_khoan)
        return TrangDuLieu.tu(bacSiRepository.timCongKhai(idChuyenKhoa, mauTen(tuKhoa), PageRequest.of(trang, kichThuoc))
                .map(bacSiMapper::toResponse));
    }

    @Override
    public BacSiChiTietResponse layChiTiet(Long id) {
        return bacSiMapper.toChiTietResponse(
                bacSiRepository.timCongKhaiTheoId(id).orElseThrow(() -> new LoiKhongTimThay("BacSi", id)));
    }

    /** Mẫu LIKE "chứa từ khoá"; %, _ trong từ khoá được escape để chỉ khớp đúng ký tự đó. Rỗng -> null. */
    private static String mauTen(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) {
            return null;
        }
        StringBuilder mau = new StringBuilder("%");
        for (char kyTu : tuKhoa.trim().toCharArray()) {
            if (kyTu == '%' || kyTu == '_' || kyTu == BacSiRepository.KY_TU_ESCAPE) {
                mau.append(BacSiRepository.KY_TU_ESCAPE);
            }
            mau.append(kyTu);
        }
        return mau.append('%').toString();
    }

}
