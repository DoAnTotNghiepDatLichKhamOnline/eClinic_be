package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Khai báo bệnh viện nghỉ các ngày [tuNgay, denNgay] (từ ngày mai, tối đa 42 ngày). */
public record KhaiBaoNgayNghiRequest(
        @NotNull(message = "{kiemTra.ngayNghi.tuNgay.batBuoc}") LocalDate tuNgay,
        @NotNull(message = "{kiemTra.ngayNghi.denNgay.batBuoc}") LocalDate denNgay,
        @NotBlank(message = "{kiemTra.ngayNghi.lyDo.batBuoc}")
        @Size(max = 200, message = "{kiemTra.ngayNghi.lyDo.toiDa}") String lyDo,
        @Size(max = 200, message = "{kiemTra.ngayNghi.lyDoEn.toiDa}") String lyDoEn) {
}
