package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.service.LuotKhamService;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichHenService;
import iuh.fit.se.eclinic.booking.service.YeuCauLichHenService;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YeuCauLichHenServiceImpl implements YeuCauLichHenService {

    private final TaiKhoanService taiKhoanService;
    private final LichHenRepository lichHenRepository;
    private final LuotKhamService luotKhamService;
    private final LichHenMapper lichHenMapper;
    private final ThongBaoLichHenService thongBaoLichHenService;

    @Override
    public TrangDuLieu<LichHenTrongCaResponse> danhSach(Long idTaiKhoanBacSi, int trang, int kichThuoc) {
        BacSi bacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi);
        return TrangDuLieu.tu(lichHenRepository
                .timCuaBacSiTheoTrangThaiTu(bacSi.getId(), TrangThaiLichHen.CHO_XAC_NHAN, LocalDateTime.now(),
                        PageRequest.of(trang, kichThuoc))
                .map(lichHen -> lichHenMapper.toTrongCaResponse(lichHen, false)));
    }

    @Override
    @Transactional
    public LichHenTrongCaResponse xacNhan(Long idTaiKhoanBacSi, Long idLichHen) {
        LichHen lichHen = khoaLichChoXacNhan(idTaiKhoanBacSi, idLichHen);
        lichHen.setTrangThai(TrangThaiLichHen.DA_XAC_NHAN);
        thongBaoLichHenService.daXacNhan(lichHen);
        log.info("Bác sĩ id={} xác nhận lịch hẹn id={}", lichHen.getBacSi().getId(), idLichHen);
        return toResponse(lichHen);
    }

    @Override
    @Transactional
    public LichHenTrongCaResponse tuChoi(Long idTaiKhoanBacSi, Long idLichHen, String lyDo) {
        LichHen lichHen = khoaLichChoXacNhan(idTaiKhoanBacSi, idLichHen);
        lichHen.setTrangThai(TrangThaiLichHen.BI_TU_CHOI);
        lichHen.setLyDoHuy(lyDo.trim());
        // BI_TU_CHOI không còn chiếm khung giờ (TrangThaiLichHen#chiemKhungGio)
        luotKhamService.traLuot(lichHen);
        thongBaoLichHenService.biTuChoi(lichHen);
        log.info("Bác sĩ id={} từ chối lịch hẹn id={}", lichHen.getBacSi().getId(), idLichHen);
        return toResponse(lichHen);
    }

    /**
     * Khoá dòng lịch hẹn rồi mới kiểm tra: bấm 2 lần, hoặc xác nhận và từ chối cùng lúc, thì lần sau thấy lịch hẹn đã
     * rời CHO_XAC_NHAN. Lịch hẹn không có và lịch hẹn của bác sĩ khác trả cùng 1 lỗi.
     */
    private LichHen khoaLichChoXacNhan(Long idTaiKhoanBacSi, Long idLichHen) {
        Long idBacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi).getId();
        LichHen lichHen = lichHenRepository.findByIdForUpdate(idLichHen)
                .filter(timThay -> timThay.getBacSi().getId().equals(idBacSi))
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "Không tìm thấy lịch hẹn"));
        if (lichHen.getTrangThai() != TrangThaiLichHen.CHO_XAC_NHAN) {
            throw new LoiNghiepVu(MaLoi.LICH_HEN_KHONG_CHO_XAC_NHAN);
        }
        if (lichHen.isCanDoiLich()) {
            throw new LoiNghiepVu(MaLoi.LICH_HEN_CAN_DOI_LICH);
        }
        if (!lichHen.getKhungGio().getGioBatDau().isAfter(LocalDateTime.now())) {
            throw new LoiNghiepVu(MaLoi.LICH_HEN_DA_QUA_GIO);
        }
        return lichHen;
    }

    /** Tải lại kèm chi tiết cho mapper; cùng transaction nên vẫn là dòng vừa đổi trạng thái. */
    private LichHenTrongCaResponse toResponse(LichHen lichHen) {
        return lichHenMapper.toTrongCaResponse(
                lichHenRepository.timTheoIdKemChiTiet(lichHen.getId()).orElse(lichHen), false);
    }

}
