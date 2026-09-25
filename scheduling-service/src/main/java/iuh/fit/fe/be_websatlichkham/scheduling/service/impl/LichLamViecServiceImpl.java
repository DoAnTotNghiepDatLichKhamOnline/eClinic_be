package iuh.fit.fe.be_websatlichkham.scheduling.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.entity.scheduling.LichLamViec;
import iuh.fit.fe.be_websatlichkham.common.exception.BusinessException;
import iuh.fit.fe.be_websatlichkham.common.exception.ErrorCode;
import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.scheduling.repository.LichLamViecRepository;
import iuh.fit.fe.be_websatlichkham.scheduling.service.LichLamViecService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LichLamViecServiceImpl implements LichLamViecService {

    private final LichLamViecRepository lichLamViecRepository;

    @Override
    public LichLamViec getById(Long id) {
        return lichLamViecRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("LichLamViec", id));
    }

    @Override
    public List<LichLamViec> findByBacSi(Long bacSiId, LocalDate tuNgay, LocalDate denNgay) {
        return lichLamViecRepository.findByBacSiIdAndNgayLamViecBetweenOrderByNgayLamViecAscGioBatDauAsc(bacSiId,
                tuNgay, denNgay);
    }

    @Override
    public void assertNoOverlap(Long bacSiId, Long phongKhamId, LocalDate ngayLamViec, LocalTime gioBatDau,
            LocalTime gioKetThuc, Long excludeLichLamViecId) {
        if (!gioKetThuc.isAfter(gioBatDau)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "Giờ kết thúc phải sau giờ bắt đầu");
        }
        if (lichLamViecRepository.existsTrungCaCuaBacSi(bacSiId, ngayLamViec, gioBatDau, gioKetThuc,
                excludeLichLamViecId)) {
            throw new BusinessException(ErrorCode.SCHEDULE_OVERLAP, "Bác sĩ đã có ca làm việc trùng giờ");
        }
        if (lichLamViecRepository.existsTrungCaCuaPhongKham(phongKhamId, ngayLamViec, gioBatDau, gioKetThuc,
                excludeLichLamViecId)) {
            throw new BusinessException(ErrorCode.SCHEDULE_OVERLAP, "Phòng khám đã được xếp ca trùng giờ");
        }
    }

}
