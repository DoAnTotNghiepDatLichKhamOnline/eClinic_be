package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;

public interface LichLamViecService {

    LichLamViec layTheoId(Long id);

    List<LichLamViec> timTheoBacSi(Long bacSiId, LocalDate tuNgay, LocalDate denNgay);

    /**
     * Ném LoiNghiepVu(TRUNG_LICH_LAM_VIEC) nếu ca [gioBatDau, gioKetThuc) trong ngày ngayLamViec
     * chồng giờ với ca khác (còn hoạt động) của cùng bác sĩ hoặc cùng phòng khám.
     *
     * @param excludeLichLamViecId id ca đang sửa, null khi tạo mới
     */
    void kiemTraKhongTrungLich(Long bacSiId, Long phongKhamId, LocalDate ngayLamViec, LocalTime gioBatDau,
            LocalTime gioKetThuc, Long excludeLichLamViecId);

}
