package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Quản trị viên đổi 1 lịch hẹn sang khung giờ khác thay bệnh nhân. */
public record QuanTriDoiLichRequest(
        Long idLichLamViec,

        Long idChuyenKhoa,

        @NotNull(message = "{kiemTra.khungGio.batBuoc}")
        LocalDateTime gioBatDauKhung,

        @NotBlank(message = "{kiemTra.lyDoDoiLich.batBuoc}")
        @Size(max = 500, message = "{kiemTra.lyDoDoiLich.toiDa}")
        String lyDo) {
}
