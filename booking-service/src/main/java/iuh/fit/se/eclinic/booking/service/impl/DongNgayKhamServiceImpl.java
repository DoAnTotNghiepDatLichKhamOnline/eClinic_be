package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.repository.LichHenRepository;
import iuh.fit.se.eclinic.booking.service.DongNgayKhamService;
import iuh.fit.se.eclinic.common.enums.TrangThaiHangDoi;
import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;
import lombok.RequiredArgsConstructor;

/** 1 lệnh UPDATE cho mọi lịch hẹn cần đóng, không gửi thông báo cho bệnh nhân. */
@Service
@RequiredArgsConstructor
public class DongNgayKhamServiceImpl implements DongNgayKhamService {

    private final LichHenRepository lichHenRepository;

    @Override
    @Transactional
    public int dongNgayDaQua() {
        return lichHenRepository.dongKhongDen(TrangThaiLichHen.DA_XAC_NHAN, TrangThaiLichHen.KHONG_DEN,
                TrangThaiHangDoi.DANG_KHAM, LocalDate.now().atStartOfDay());
    }

}
