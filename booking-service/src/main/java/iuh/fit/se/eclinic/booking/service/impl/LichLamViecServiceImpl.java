package iuh.fit.se.eclinic.booking.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.booking.repository.LichLamViecRepository;
import iuh.fit.se.eclinic.booking.service.LichLamViecService;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LichLamViecServiceImpl implements LichLamViecService {

    private final LichLamViecRepository lichLamViecRepository;

    @Override
    public LichLamViec layTheoId(Long id) {
        return lichLamViecRepository.findById(id).orElseThrow(() -> new LoiKhongTimThay("LichLamViec", id));
    }

    @Override
    public List<LichLamViec> timTheoBacSi(Long bacSiId, LocalDate tuNgay, LocalDate denNgay) {
        return lichLamViecRepository.findByBacSiIdAndNgayLamViecBetweenOrderByNgayLamViecAscGioBatDauAsc(bacSiId,
                tuNgay, denNgay);
    }

    @Override
    public void kiemTraKhongTrungLich(Long bacSiId, Long phongKhamId, LocalDate ngayLamViec, LocalTime gioBatDau,
            LocalTime gioKetThuc, Long excludeLichLamViecId) {
        if (!gioKetThuc.isAfter(gioBatDau)) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "Giờ kết thúc phải sau giờ bắt đầu");
        }
        if (lichLamViecRepository.existsTrungCaCuaBacSi(bacSiId, ngayLamViec, gioBatDau, gioKetThuc,
                excludeLichLamViecId)) {
            throw new LoiNghiepVu(MaLoi.TRUNG_LICH_LAM_VIEC, "Bác sĩ đã có ca làm việc trùng giờ");
        }
        if (lichLamViecRepository.existsTrungCaCuaPhongKham(phongKhamId, ngayLamViec, gioBatDau, gioKetThuc,
                excludeLichLamViecId)) {
            throw new LoiNghiepVu(MaLoi.TRUNG_LICH_LAM_VIEC, "Phòng khám đã được xếp ca trùng giờ");
        }
    }

}
