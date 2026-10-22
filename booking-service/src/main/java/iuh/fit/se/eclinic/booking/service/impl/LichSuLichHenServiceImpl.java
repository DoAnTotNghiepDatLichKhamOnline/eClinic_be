package iuh.fit.se.eclinic.booking.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.repository.LichSuLichHenRepository;
import iuh.fit.se.eclinic.booking.service.LichSuLichHenService;
import iuh.fit.se.eclinic.booking.service.NguoiThucHien;
import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.booking.LichSuLichHen;
import iuh.fit.se.eclinic.common.enums.SuKienLichHen;
import lombok.RequiredArgsConstructor;

/** MANDATORY: gọi ngoài transaction là lỗi lập trình (nhật ký có thể được ghi mà lịch hẹn không đổi). */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class LichSuLichHenServiceImpl implements LichSuLichHenService {

    private final LichSuLichHenRepository lichSuLichHenRepository;

    @Override
    public void ghi(LichHen lichHen, SuKienLichHen suKien, NguoiThucHien nguoi, String lyDo, LichHen lienQuan) {
        lichSuLichHenRepository.save(LichSuLichHen.tao(lichHen, suKien, nguoi.loai(), nguoi.taiKhoan(),
                lyDo == null || lyDo.isBlank() ? null : lyDo.trim(), lienQuan));
    }

}
