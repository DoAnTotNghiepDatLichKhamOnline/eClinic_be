package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

/** 1 dòng ca đã đọc được từ tệp Excel nhập lịch (mọi ô đúng dạng), chưa kiểm tra với DB. */
public record DongCaNhap(
        int dong,
        Long idBacSi,
        String maBacSi,
        LocalDate ngay,
        LocalTime gioBatDau,
        LocalTime gioKetThuc,
        Long idPhongKham,
        int soLuotToiDaMoiGio,
        int thoiLuongLuotPhut) {
}
