package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 1 dòng ca đã đọc được từ tệp Excel nhập lịch (mọi ô đúng dạng), chưa kiểm tra với DB. Không phải body của request:
 * {@code ExcelLichLamViecService} tạo ra từ tệp rồi chuyển cho {@code QuanLyCaService.nhapCa}.
 *
 * @param dong    số dòng trong Excel (dòng 1 là tiêu đề), để báo lỗi đúng dòng
 * @param maBacSi mã bác sĩ như trong tệp (BS0001); {@code idBacSi} suy ra từ mã
 */
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
