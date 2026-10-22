package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.NgonNgu;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Quản trị viên đặt lịch thay người gọi tổng đài / người đến quầy: cùng form và quy tắc với khách đặt
 * ({@link DatLichRequest}), không có các lựa chọn của tài khoản bệnh nhân.
 *
 * @param ngonNgu ngôn ngữ người gọi chọn cho email phiếu khám; bỏ trống = VI
 */
public record QuanTriDatLichRequest(
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

        NgonNgu ngonNgu) {
}
