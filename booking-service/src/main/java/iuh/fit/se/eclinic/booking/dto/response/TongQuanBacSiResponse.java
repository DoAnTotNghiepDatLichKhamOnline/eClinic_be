package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDateTime;

/** Số liệu màn hình Dashboard của bác sĩ đang đăng nhập. */
public record TongQuanBacSiResponse(
        long lichHenHomNay,
        long dangCho,
        long daKham,
        Double diemDanhGia,
        long soDanhGia,
        long lichHenSapToi,
        LocalDateTime tinhLuc) {
}
