package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDateTime;
import java.util.List;

import iuh.fit.se.eclinic.common.entity.scheduling.KhungGioKham;

public interface KhungGioKhamService {

    KhungGioKham layTheoId(Long id);

    List<KhungGioKham> timTheoLichLamViec(Long lichLamViecId);

    /** Các khung giờ còn trống của bác sĩ, bắt đầu trong khoảng [tu, den). */
    List<KhungGioKham> timKhungGioConTrongTheoBacSi(Long bacSiId, LocalDateTime tu, LocalDateTime den);

}
