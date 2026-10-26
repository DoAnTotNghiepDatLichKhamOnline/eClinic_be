package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/** Đổi 1 lịch hẹn sang khung giờ khác (hủy lịch cũ và tạo lịch mới). */
public record DoiLichRequest(
        Long idLichLamViec,

        Long idChuyenKhoa,

        @NotNull(message = "{kiemTra.khungGio.batBuoc}")
        LocalDateTime gioBatDauKhung,

        String soDienThoai) {
}
