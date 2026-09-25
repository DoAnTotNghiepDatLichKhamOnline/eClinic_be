package iuh.fit.fe.be_websatlichkham.scheduling.service;

import java.time.LocalDateTime;
import java.util.List;

import iuh.fit.fe.be_websatlichkham.common.entity.scheduling.KhungGioKham;

public interface KhungGioKhamService {

    KhungGioKham getById(Long id);

    List<KhungGioKham> findByLichLamViec(Long lichLamViecId);

    /** Các khung giờ còn trống của bác sĩ, bắt đầu trong khoảng [tu, den). */
    List<KhungGioKham> findConTrongByBacSi(Long bacSiId, LocalDateTime tu, LocalDateTime den);

}
