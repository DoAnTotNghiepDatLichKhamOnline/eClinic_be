package iuh.fit.se.eclinic.catalog.service.impl;

import java.time.Year;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.request.CapNhatHoSoCuaToiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.HoSoBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.mapper.BacSiMapper;
import iuh.fit.se.eclinic.catalog.repository.AnhBacSiRepository;
import iuh.fit.se.eclinic.catalog.repository.BacSiRepository;
import iuh.fit.se.eclinic.catalog.service.HoSoBacSiService;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
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
        BacSi bacSi = khoaDeSua(idBacSi, request.phienBan());
        kiemTraNamBatDauHanhNghe(request.namBatDauHanhNghe());
        bacSiMapper.capNhatHoSo(bacSi, request);
        bacSi.setPhienBanHoSo(bacSi.getPhienBanHoSo() + 1);
        log.info("Cập nhật hồ sơ giới thiệu của bác sĩ id={} (phiên bản {})", idBacSi, bacSi.getPhienBanHoSo());
        return layHoSo(idBacSi);
    }

    @Override
    @Transactional
    public HoSoBacSiQuanTriResponse capNhatHoSoCuaToi(Long idBacSi, CapNhatHoSoCuaToiRequest request) {
        BacSi bacSi = khoaDeSua(idBacSi, request.phienBan());
        bacSiMapper.capNhatHoSoCuaToi(bacSi, request);
        bacSi.setPhienBanHoSo(bacSi.getPhienBanHoSo() + 1);
        log.info("Bác sĩ id={} tự cập nhật hồ sơ giới thiệu (phiên bản {})", idBacSi, bacSi.getPhienBanHoSo());
        return layHoSo(idBacSi);
    }

    /** Năm bắt đầu hành nghề không được sau năm nay (giới hạn dưới nằm ở request và CHECK của bảng). */
    static void kiemTraNamBatDauHanhNghe(Integer nam) {
        if (nam != null && nam > Year.now().getValue()) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.namBatDauHanhNghe");
        }
    }

    /** Khoá dòng bác sĩ rồi so phiên bản: 2 người lưu cùng lúc thì người sau bị từ chối. */
    private BacSi khoaDeSua(Long idBacSi, int phienBan) {
        BacSi bacSi = bacSiRepository.findByIdForUpdate(idBacSi)
                .orElseThrow(() -> new LoiKhongTimThay("BacSi", idBacSi));
        if (phienBan != bacSi.getPhienBanHoSo()) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_DA_THAY_DOI);
        }
        return bacSi;
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
