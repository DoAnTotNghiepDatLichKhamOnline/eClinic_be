package iuh.fit.fe.be_websatlichkham.scheduling.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import iuh.fit.fe.be_websatlichkham.common.entity.scheduling.LichLamViec;

public interface LichLamViecService {

    LichLamViec getById(Long id);

    List<LichLamViec> findByBacSi(Long bacSiId, LocalDate tuNgay, LocalDate denNgay);

    /**
     * Ném BusinessException(SCHEDULE_OVERLAP) nếu ca [gioBatDau, gioKetThuc) trong ngày ngayLamViec
     * chồng giờ với ca khác (còn hoạt động) của cùng bác sĩ hoặc cùng phòng khám.
     *
     * @param excludeLichLamViecId id ca đang sửa, null khi tạo mới
     */
    void assertNoOverlap(Long bacSiId, Long phongKhamId, LocalDate ngayLamViec, LocalTime gioBatDau,
            LocalTime gioKetThuc, Long excludeLichLamViecId);

}
