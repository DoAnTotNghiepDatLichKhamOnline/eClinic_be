package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Đặt lịch khám. */
public record DatLichRequest(
        Long idLichLamViec,

        Long idChuyenKhoa,

        @NotNull(message = "{kiemTra.khungGio.batBuoc}")
        LocalDateTime gioBatDauKhung,

        @NotNull(message = "{kiemTra.benhNhan.batBuoc}")
        @Valid
        BenhNhanRequest benhNhan,

        @Valid
        NguoiGiamHoRequest nguoiGiamHo,

        @NotBlank(message = "{kiemTra.lyDoKham.batBuoc}")
        @Size(max = 500, message = "{kiemTra.lyDoKham.toiDa}")
        String lyDoKham,

        Boolean datChoBanThan,

        Boolean luuNguoiThan) {
}
