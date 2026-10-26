package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import iuh.fit.se.eclinic.common.enums.LoaiYeuCauDoiLich;
import iuh.fit.se.eclinic.common.enums.TrangThaiYeuCau;

/** 1 yêu cầu đổi ca / xin nghỉ / nhường ca, cho bác sĩ đã gửi, bác sĩ nhận ca và quản trị viên. */
public record YeuCauDoiLichResponse(Long id, LoaiYeuCauDoiLich loaiYeuCau, TrangThaiYeuCau trangThai, String lyDo,
        BacSiTomTatResponse bacSi, CaLamViecResponse ca, LocalDate ngayMongMuon, LocalTime gioBatDauMongMuon,
        LocalTime gioKetThucMongMuon, PhongKhamTomTatResponse phongKhamMongMuon, BacSiTomTatResponse bacSiNhan,
        String ghiChuBacSiNhan, LocalDateTime ngayBacSiNhanPhanHoi, String maNhom, String ghiChuXuLy,
        LocalDateTime ngayGui, LocalDateTime ngayXuLy) {
}
