package iuh.fit.se.eclinic.catalog.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.HoSoBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.mapper.BacSiMapper;
import iuh.fit.se.eclinic.catalog.repository.AnhBacSiRepository;
import iuh.fit.se.eclinic.catalog.repository.BacSiRepository;
import iuh.fit.se.eclinic.catalog.service.HoSoBacSiService;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoSoBacSiServiceImpl implements HoSoBacSiService {

    private final BacSiRepository bacSiRepository;
    private final AnhBacSiRepository anhBacSiRepository;
    private final BacSiMapper bacSiMapper;

    @Override
    public HoSoBacSiQuanTriResponse layHoSo(Long idBacSi) {
        return toResponse(timBacSi(idBacSi));
    }

    @Override
    @Transactional
    public HoSoBacSiQuanTriResponse capNhatHoSo(Long idBacSi, CapNhatHoSoBacSiRequest request) {
        BacSi bacSi = timBacSi(idBacSi);
        bacSiMapper.capNhatHoSo(bacSi, request);
        log.info("Cập nhật hồ sơ giới thiệu của bác sĩ id={}", idBacSi);
        return toResponse(bacSi);
    }

    private HoSoBacSiQuanTriResponse toResponse(BacSi bacSi) {
        return bacSiMapper.toQuanTriResponse(bacSi,
                anhBacSiRepository.findByBacSiIdOrderByThuTuAscIdAsc(bacSi.getId()));
    }

    private BacSi timBacSi(Long idBacSi) {
        return bacSiRepository.timKemTaiKhoanVaChuyenKhoa(idBacSi)
                .orElseThrow(() -> new LoiKhongTimThay("BacSi", idBacSi));
    }

}
