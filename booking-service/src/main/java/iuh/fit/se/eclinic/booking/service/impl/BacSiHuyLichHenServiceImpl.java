package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.dto.response.LichHenTrongCaResponse;
import iuh.fit.se.eclinic.booking.mapper.LichHenMapper;
import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.service.BacSiHuyLichHenService;
import iuh.fit.se.eclinic.booking.service.LichSuLichHenService;
import iuh.fit.se.eclinic.booking.service.LuotKhamService;
import iuh.fit.se.eclinic.booking.service.NguoiThucHien;
import iuh.fit.se.eclinic.booking.service.TaiKhoanService;
import iuh.fit.se.eclinic.booking.service.ThongBaoLichHenService;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.enums.SuKienLichHen;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BacSiHuyLichHenServiceImpl implements BacSiHuyLichHenService {

    private final TaiKhoanService taiKhoanService;
    private final LichHenRepository lichHenRepository;
    private final LuotKhamService luotKhamService;
    private final LichHenMapper lichHenMapper;
    private final ThongBaoLichHenService thongBaoLichHenService;
    private final LichSuLichHenService lichSuLichHenService;

    @Override
    @Transactional
    public LichHenTrongCaResponse huy(Long idTaiKhoanBacSi, Long idLichHen, String lyDo) {
        LichHen lichHen = khoaLichDaXacNhan(idTaiKhoanBacSi, idLichHen);
        lichHen.setTrangThai(TrangThaiLichHen.BI_TU_CHOI);
        lichHen.setLyDoHuy(lyDo.trim());
        // BI_TU_CHOI không còn chiếm khung giờ (TrangThaiLichHen#chiemKhungGio)
        luotKhamService.traLuot(lichHen);
        lichSuLichHenService.ghi(lichHen, SuKienLichHen.HUY, NguoiThucHien.bacSi(lichHen.getBacSi()),
                lichHen.getLyDoHuy(), null);
        thongBaoLichHenService.bacSiHuy(lichHen);
        log.info("Bác sĩ id={} hủy lịch hẹn id={}", lichHen.getBacSi().getId(), idLichHen);
        return toResponse(lichHen);
    }

    /** Khoá dòng lịch hẹn rồi mới kiểm tra, để lần bấm sau thấy lịch đã rời DA_XAC_NHAN. */
    private LichHen khoaLichDaXacNhan(Long idTaiKhoanBacSi, Long idLichHen) {
        Long idBacSi = taiKhoanService.layBacSiDangHoatDong(idTaiKhoanBacSi).getId();
        LichHen lichHen = lichHenRepository.findByIdForUpdate(idLichHen)
                .filter(timThay -> timThay.getBacSi().getId().equals(idBacSi))
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.lichHen"));
        if (lichHen.getTrangThai() != TrangThaiLichHen.DA_XAC_NHAN) {
            throw new LoiNghiepVu(MaLoi.LICH_HEN_KHONG_HUY_DOI_DUOC);
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
